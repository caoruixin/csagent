package com.gumtree.csagent.service.runtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gumtree.csagent.config.AgentRunLoopProperties;
import com.gumtree.csagent.model.BotSession;
import com.gumtree.csagent.repository.BotEventRepository;
import com.gumtree.csagent.repository.BotTurnRepository;
import com.gumtree.csagent.service.observability.EventEmitter;
import com.gumtree.csagent.service.tools.CreateCaseControlledTool;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Sprint 8 §K0: cs_interactive_259 active-use-case contract hardening.
 *
 * <p>Sprint 7 §I0 added the {@code candidate_use_cases} projection and
 * the DISCOVER cue that pushes the LLM toward
 * {@code search_knowledge → classify_use_case(UC-F)} for the cs259
 * empty-form payment / sale-proceeds shape. Sprint 7's clean smoke
 * confirmed the §I0 effect (UC-F now committed in r1, vs Sprint 6 r1
 * UC-J / r2 UC-E drift). However, the post-Sprint-7 clean smoke r2
 * (`eval_interactive/results/20260505-225708/results.json`) showed a
 * CONTRACT_VIOLATION:active_use_case on cs259: the LLM called
 * {@code search_knowledge} twice (no viable hits), then emitted
 * {@code request_handover(faq_miss_threshold_exceeded)} WITHOUT ever
 * calling {@code classify_use_case}. The session reached the
 * {@code AgentRunLoop} ESCALATE branch in
 * {@link ControlKernel#processMessage} with
 * {@code activeUseCase=null}; that branch (unlike
 * {@link ControlKernel#forceEscalate}) had no fallback-UC commit, so
 * the trace contract validator raised the violation and the case
 * never reached scoring.
 *
 * <p>The Sprint 8 §K0 fix:
 * <ol>
 *   <li>Extracts the existing {@code forceEscalate} fallback into
 *       {@link ControlKernel#applyMissingUseCaseFallback} so both the
 *       legacy budget-close path AND the AgentRunLoop ESCALATE branch
 *       share the same deterministic UC commit.</li>
 *   <li>Calls the helper from the AgentRunLoop ESCALATE branch in
 *       {@link ControlKernel#processMessage} before
 *       {@code createCaseIfNeeded} runs, so the persisted
 *       {@code request_handover} entry carries a non-null
 *       {@code activeUseCase} on the session.</li>
 *   <li>Extends the UC-F regex in
 *       {@link ControlKernel#inferFallbackUseCase} with sale-proceeds
 *       vocabulary ({@code payout / payouts / proceeds / sale / sold /
 *       selling / money}) so cs259-shape user messages that avoid
 *       the literal "payment" token (e.g. "sale proceeds", "receive
 *       money for an item I sold") still resolve to UC-F.</li>
 * </ol>
 *
 * <p>This test class pins the contract surface: the fallback fires
 * for the cs259 family of intents, never overrides an
 * already-committed UC, and the negative guards for cs014 / cs066 /
 * cs095 / cs011 / cs002 / cs029 are preserved because none of those
 * personas use the extended UC-F vocabulary.
 */
@ExtendWith(MockitoExtension.class)
class Sprint8Cs259ActiveUseCaseHardeningTest {

    @Mock private BotTurnRepository turnRepository;
    @Mock private BotEventRepository eventRepository;
    @Mock private BudgetChecker budgetChecker;
    @Mock private DriftDetector driftDetector;
    @Mock private PhaseEvaluator phaseEvaluator;
    @Mock private ControlPolicyService controlPolicy;
    @Mock private CreateCaseControlledTool createCaseTool;
    @Mock private EventEmitter eventEmitter;
    @Mock private ContextProjectionBuilder contextProjectionBuilder;
    @Mock private AgentRunLoop agentRunLoop;

    private ControlKernel kernel() {
        return new ControlKernel(
                turnRepository, eventRepository, budgetChecker, driftDetector,
                phaseEvaluator, controlPolicy, new ObjectMapper(), createCaseTool,
                eventEmitter, contextProjectionBuilder, new AgentRunLoopProperties(),
                agentRunLoop, new EscalationReasonResolver());
    }

    private BotSession unknownTopicSession() {
        BotSession s = new BotSession();
        s.setSessionId("sess-k0");
        s.setFormTopicSubject("UNKNOWN");
        s.setFormContext("{\"description\":\"\",\"topic_subject\":\"UNKNOWN\"}");
        return s;
    }

    // ─────────────────────────────────────────────────────────────
    // Positive: cs259 family resolves to UC-F via inferFallbackUseCase
    // ─────────────────────────────────────────────────────────────

    @Test
    void cs259_verbatimSeed_resolvesToUcF() {
        // cs_interactive_259.yaml seed_messages[0] verbatim.
        BotSession s = unknownTopicSession();
        assertEquals("UC-F",
                kernel().inferFallbackUseCase(s, "How do I receive the payment when I sell an item"));
    }

    @Test
    void cs259_paidAfterSelling_resolvesToUcF() {
        BotSession s = unknownTopicSession();
        assertEquals("UC-F",
                kernel().inferFallbackUseCase(s, "how do I get paid after selling"));
    }

    @Test
    void cs259_paymentAfterSelling_resolvesToUcF() {
        BotSession s = unknownTopicSession();
        assertEquals("UC-F",
                kernel().inferFallbackUseCase(s, "payment after selling"));
    }

    @Test
    void cs259_receiveMoneyForSoldItem_resolvesToUcF() {
        // The "money + sold" variant has neither "payment" nor "paid".
        // Sprint 8 §K0 regex extension is what makes this resolve to UC-F.
        BotSession s = unknownTopicSession();
        assertEquals("UC-F",
                kernel().inferFallbackUseCase(s, "receive money for an item I sold"));
    }

    @Test
    void cs259_saleProceeds_resolvesToUcF() {
        // The "sale proceeds" phrase has neither "payment" nor "paid".
        BotSession s = unknownTopicSession();
        assertEquals("UC-F",
                kernel().inferFallbackUseCase(s, "sale proceeds"));
    }

    @Test
    void cs259_payoutKeyword_resolvesToUcF() {
        BotSession s = unknownTopicSession();
        assertEquals("UC-F",
                kernel().inferFallbackUseCase(s, "when do I get my payout?"));
    }

    // ─────────────────────────────────────────────────────────────
    // applyMissingUseCaseFallback — wrapper helper
    // ─────────────────────────────────────────────────────────────

    @Test
    void applyMissingUseCaseFallback_blankUc_commitsFallback() {
        BotSession s = unknownTopicSession();
        s.setActiveUseCase(null);
        kernel().applyMissingUseCaseFallback(s,
                "How do I receive the payment when I sell an item");
        assertEquals("UC-F", s.getActiveUseCase(),
                "Blank UC must be filled with the inferred fallback UC.");
        assertEquals(0, new java.math.BigDecimal("0.30").compareTo(s.getIntentConfidence()),
                "Fallback path must stamp intentConfidence=0.30 (uncertain).");
        assertArrayEquals(new String[]{"UC-F"}, s.getCandidateUseCases(),
                "candidate_use_cases must be seeded when the fallback fires "
                        + "into an empty list, so the projection has the "
                        + "single-candidate signal for downstream phases.");
    }

    @Test
    void applyMissingUseCaseFallback_emptyStringUc_commitsFallback() {
        // Empty string is treated the same as null per the helper's
        // ``isBlank`` check.
        BotSession s = unknownTopicSession();
        s.setActiveUseCase("");
        kernel().applyMissingUseCaseFallback(s, "how do I get paid after selling");
        assertEquals("UC-F", s.getActiveUseCase());
    }

    @Test
    void applyMissingUseCaseFallback_committedUc_isNotOverwritten() {
        // Negative guard: a UC already committed by the LLM (or by
        // UseCaseRouter strong-priors) must NEVER be overwritten by
        // this fallback. Even if the user message would otherwise map
        // to UC-F, the existing UC wins.
        BotSession s = unknownTopicSession();
        s.setActiveUseCase("UC-C");
        s.setIntentConfidence(new java.math.BigDecimal("0.85"));
        kernel().applyMissingUseCaseFallback(s,
                "How do I receive payment when I sell an item");
        assertEquals("UC-C", s.getActiveUseCase(),
                "applyMissingUseCaseFallback must be a no-op when an UC is "
                        + "already committed.");
        assertEquals(0, new java.math.BigDecimal("0.85").compareTo(s.getIntentConfidence()),
                "intentConfidence must not be downgraded by the fallback.");
    }

    @Test
    void applyMissingUseCaseFallback_preservesExistingCandidateUseCases() {
        // When candidate_use_cases is already populated (e.g. by
        // UseCaseRouter LLM classification narrowing), the fallback
        // must not overwrite the array — only fill it when empty.
        BotSession s = unknownTopicSession();
        s.setActiveUseCase(null);
        s.setCandidateUseCases(new String[]{"UC-F", "UC-K"});
        kernel().applyMissingUseCaseFallback(s,
                "How do I receive payment when I sell an item");
        assertEquals("UC-F", s.getActiveUseCase());
        assertArrayEquals(new String[]{"UC-F", "UC-K"}, s.getCandidateUseCases(),
                "Existing candidate_use_cases must be preserved when the "
                        + "fallback commits a UC that is already in the "
                        + "candidate set.");
    }

    @Test
    void applyMissingUseCaseFallback_blankInputs_writesSafeDefault() {
        // No user message + empty form description → safe default
        // UC-D so the trace contract is still satisfied.
        BotSession s = unknownTopicSession();
        s.setActiveUseCase(null);
        kernel().applyMissingUseCaseFallback(s, "");
        assertEquals("UC-D", s.getActiveUseCase(),
                "Empty user message + empty form description must produce "
                        + "the safe generic-account default UC-D, not null.");
    }

    // ─────────────────────────────────────────────────────────────
    // Negative guards — preserve cs014 / cs066 / cs095 / cs011 / cs002 / cs029 / cs176
    // ─────────────────────────────────────────────────────────────

    @Test
    void cs014_messagingMessage_isNotUcF() {
        // cs014 persona: "I'm not receiving messages from buyers".
        // Must not match UC-F vocabulary; must remain UC-C
        // (messaging keyword wins) so the cs014 negative guard holds.
        BotSession s = unknownTopicSession();
        String actual = kernel().inferFallbackUseCase(s,
                "I am not receiving messages from buyers");
        assertNotEquals("UC-F", actual,
                "cs014 messaging persona must not be re-routed to UC-F.");
        assertEquals("UC-C", actual);
    }

    @Test
    void cs066_phoneNumberRegression_isNotUcF() {
        // cs066 persona: "Why am I not getting the option to add my
        // phone number as a point of contact when listing an item any
        // more". The phrase contains "listing" (UC-A keyword) and the
        // pre-LLM UC-K technical-regression override fires upstream;
        // either way, the fallback must NOT pick UC-F.
        BotSession s = unknownTopicSession();
        String actual = kernel().inferFallbackUseCase(s,
                "Why am I not getting the option to add my phone number "
                        + "as a point of contact when listing an item any more");
        assertNotEquals("UC-F", actual,
                "cs066 technical-regression persona must not be re-routed "
                        + "to UC-F by the fallback.");
    }

    @Test
    void cs095_adVisibility_isNotUcF() {
        // cs095 persona: "I have no adverts on my account, can't see
        // my live ads". The pre-LLM B2 messaging/account/email-sync
        // bias commits UC-A upstream, so the fallback never fires for
        // cs095 in production. The K0 contract here is the
        // belt-and-suspenders guarantee that the regex does not
        // re-route this message to UC-F. (The pure-regex result is
        // UC-D, because the "account" keyword matches UC-D before the
        // "ad/advert" keyword matches UC-A — that ordering is part of
        // the §B3 inference rules and predates Sprint 8.)
        BotSession s = unknownTopicSession();
        String actual = kernel().inferFallbackUseCase(s,
                "I have no adverts on my account, can't see my live ads");
        assertNotEquals("UC-F", actual,
                "cs095 ad-visibility persona must not be re-routed to UC-F "
                        + "by the Sprint 8 §K0 sale-proceeds extension.");
    }

    @Test
    void cs095_pureAdVisibility_resolvesToUcA_viaInferenceOrder() {
        // When the user message is purely about ads with no account
        // keyword, the regex correctly picks UC-A (the §B3 inference
        // ordering check; defends against an accidental swap that
        // would route ad-visibility persona to UC-F via "sale"/"sold").
        BotSession s = unknownTopicSession();
        assertEquals("UC-A",
                kernel().inferFallbackUseCase(s, "my advert is missing from search"));
    }

    @Test
    void cs011_accountAccess_isNotUcF() {
        BotSession s = unknownTopicSession();
        String actual = kernel().inferFallbackUseCase(s, "I cannot access my account");
        assertNotEquals("UC-F", actual);
        assertEquals("UC-D", actual);
    }

    @Test
    void cs002_messagingDistress_isNotUcF() {
        BotSession s = unknownTopicSession();
        String actual = kernel().inferFallbackUseCase(s,
                "your no helping at all I haven't been able to send messages for days");
        assertNotEquals("UC-F", actual);
        assertEquals("UC-C", actual);
    }

    @Test
    void cs029_accountShout_isNotUcF() {
        // cs_interactive_029 verbatim ALL-CAPS shout: must remain UC-D.
        BotSession s = unknownTopicSession();
        String actual = kernel().inferFallbackUseCase(s, "HI MY ACCOUNT OS");
        assertNotEquals("UC-F", actual);
        assertEquals("UC-D", actual);
    }

    @Test
    void cs176_explicitHumanHelp_isNotUcF() {
        // cs176 persona: "What about giving a phone number to talk to
        // someone". The deterministic
        // EscalationReasonResolver.detectExplicitUserEscalation hits
        // first and stamps user_requested upstream; the
        // forceEscalate fallback then commits a UC. The phrase has no
        // payment/sale tokens — must not pick UC-F.
        BotSession s = unknownTopicSession();
        String actual = kernel().inferFallbackUseCase(s,
                "What about giving a phone number to talk to someone");
        assertNotEquals("UC-F", actual,
                "cs176 explicit-human-help persona must not be re-routed "
                        + "to UC-F by the K0 fallback.");
    }

    // ─────────────────────────────────────────────────────────────
    // Helper invariants
    // ─────────────────────────────────────────────────────────────

    @Test
    void applyMissingUseCaseFallback_intakeUcCandidate_passesThroughCreateCaseGate() {
        // When the fallback infers an intake UC (UC-K via the technical
        // regression keywords baked into the form description, or
        // similar), the helper writes the UC but does NOT itself create
        // a case — that is the caller's responsibility. We verify the
        // helper does not invoke createCaseTool. This pins the
        // separation of concerns: the helper only fills the slot; the
        // caller decides whether createCaseIfNeeded fires.
        BotSession s = unknownTopicSession();
        s.setFormContext("{\"description\":\"my account locked, password reset broken\"}");
        s.setActiveUseCase(null);
        kernel().applyMissingUseCaseFallback(s, "");
        assertEquals("UC-D", s.getActiveUseCase(),
                "Form description with account/login keywords picks UC-D.");
        // No interaction with createCaseTool from this helper.
        org.mockito.Mockito.verifyNoInteractions(createCaseTool);
    }

    @Test
    void applyMissingUseCaseFallback_unknownInferenceResult_isHonoured() {
        // If inferFallbackUseCase ever returned null (it cannot today
        // — the default branch returns UC-D — but the helper's null
        // check guards a future change), the helper would leave the
        // session UC unchanged. Document the contract: a null result
        // produces no commit.
        BotSession s = unknownTopicSession();
        s.setActiveUseCase(null);
        // Drive a blank inference result indirectly by not touching
        // anything; default returns UC-D.
        kernel().applyMissingUseCaseFallback(s, "");
        assertNotEquals(null, s.getActiveUseCase(),
                "Default branch is UC-D, not null.");
        assertNull(s.getCaseId(),
                "applyMissingUseCaseFallback must never create a runtime "
                        + "case directly.");
    }
}
