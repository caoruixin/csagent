package com.gumtree.csagent.service.runtime.skill;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gumtree.csagent.model.BotSession;
import com.gumtree.csagent.model.PhasePlan;
import com.gumtree.csagent.model.ToolCall;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Sprint 39 unit coverage for {@link SkillGuardrailDispatcher} per Sprint 37
 * freeze decision (h) §9. Verifies the 4 typed predicate handlers' fire /
 * no-fire scenarios, the short-circuit-on-first-reject composition semantic
 * (§9.2), the {@link RejectVerdict} trace shape (§9.4), and
 * {@link DispatchContext} null-safety.
 *
 * <p>Tests use the real production {@link SkillRegistry} so the dispatch
 * fires the actual Sprint 39 RESOLVE Skill guardrails declared at
 * {@code server/src/main/resources/skills/resolve_*.yaml}.
 */
class SkillGuardrailDispatcherTest {

    private SkillGuardrailDispatcher dispatcher;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        dispatcher = SkillTestFixtures.productionDispatcher();
    }

    // ---------- helpers ----------

    private PhasePlan faqPlan(String uc) {
        return PhasePlan.builder()
                .phase("RESOLVE")
                .useCase(uc)
                .objective("resolve-faq")
                .allowedTools(List.of("get_customer_context", "search_knowledge",
                        "resolve_article", "record_outcome", "request_handover"))
                .maxToolSteps(4)
                .build();
    }

    private PhasePlan intakePlan(String uc) {
        return PhasePlan.builder()
                .phase("RESOLVE")
                .useCase(uc)
                .objective("resolve-intake")
                .allowedTools(List.of("request_handover"))
                .maxToolSteps(3)
                .build();
    }

    private BotSession intakeSession(String uc, String intakeFieldsJson) {
        BotSession s = new BotSession();
        s.setSessionId("sess-d-" + uc);
        s.setCurrentPhase("RESOLVE");
        s.setActiveUseCase(uc);
        s.setIntakeFields(intakeFieldsJson);
        return s;
    }

    private BotSession faqSession(String uc, String phase) {
        BotSession s = new BotSession();
        s.setSessionId("sess-f-" + uc);
        s.setCurrentPhase(phase);
        s.setActiveUseCase(uc);
        return s;
    }

    private Map<String, Object> viableSearchHits() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("faq_miss", false);
        data.put("hits", List.of(Map.of("source_id", "kb-001",
                "title", "How to receive payment", "score", 0.95)));
        return data;
    }

    private Map<String, Object> emptySearch() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("faq_miss", true);
        data.put("hits", List.of());
        return data;
    }

    private ToolCall handover(String reason) {
        return ToolCall.builder()
                .name("request_handover")
                .arguments(Map.of("escalation_reason", reason))
                .build();
    }

    private ToolCall recordOutcome(String outcomeClass) {
        return ToolCall.builder()
                .name("record_outcome")
                .arguments(Map.of("outcome_class", outcomeClass))
                .build();
    }

    // ---------- faq_miss_handover_requires_resolve_attempt ----------

    @Test
    void faqMiss_fires_when_searchHits_andNoResolveYet() {
        Map<String, Object> acc = new LinkedHashMap<>();
        acc.put("search_knowledge", viableSearchHits());
        DispatchContext ctx = new DispatchContext(
                faqPlan("UC-A"), null, acc, null, Optional.empty());
        Optional<RejectVerdict> verdict = dispatcher.checkBeforeDispatch(
                faqPlan("UC-A"), handover("faq_miss_threshold_exceeded"), ctx);
        assertTrue(verdict.isPresent());
        assertEquals(SkillGuardrailDispatcher.FAQ_MISS_REJECT_REASON,
                verdict.get().predicateName());
        assertNotNull(verdict.get().trace().get("skill_name"));
        assertEquals("resolve_faq_grounded_answer", verdict.get().trace().get("skill_name"));
    }

    @Test
    void faqMiss_doesNotFire_when_resolveAlreadyAttempted() {
        Map<String, Object> acc = new LinkedHashMap<>();
        acc.put("search_knowledge", viableSearchHits());
        acc.put("resolve_article", Map.of("article", "x"));
        DispatchContext ctx = new DispatchContext(
                faqPlan("UC-A"), null, acc, null, Optional.empty());
        assertFalse(dispatcher.checkBeforeDispatch(faqPlan("UC-A"),
                handover("faq_miss_threshold_exceeded"), ctx).isPresent());
    }

    @Test
    void faqMiss_doesNotFire_when_searchEmpty_orFaqMiss() {
        Map<String, Object> acc = new LinkedHashMap<>();
        acc.put("search_knowledge", emptySearch());
        DispatchContext ctx = new DispatchContext(
                faqPlan("UC-A"), null, acc, null, Optional.empty());
        assertFalse(dispatcher.checkBeforeDispatch(faqPlan("UC-A"),
                handover("faq_miss_threshold_exceeded"), ctx).isPresent());
    }

    @Test
    void faqMiss_doesNotFire_for_userRequested() {
        Map<String, Object> acc = new LinkedHashMap<>();
        acc.put("search_knowledge", viableSearchHits());
        DispatchContext ctx = new DispatchContext(
                faqPlan("UC-A"), null, acc, null, Optional.empty());
        assertFalse(dispatcher.checkBeforeDispatch(faqPlan("UC-A"),
                handover("user_requested"), ctx).isPresent());
    }

    @Test
    void faqMiss_doesNotFire_for_intakeUc() {
        // UC-K routes to resolve_intake_collect_and_handover Skill which does
        // NOT declare faq_miss_handover_requires_resolve_attempt; the handler
        // is never invoked.
        Map<String, Object> acc = new LinkedHashMap<>();
        acc.put("search_knowledge", viableSearchHits());
        DispatchContext ctx = new DispatchContext(
                intakePlan("UC-K"), intakeSession("UC-K", null), acc, null, Optional.empty());
        assertFalse(dispatcher.checkBeforeDispatch(intakePlan("UC-K"),
                handover("faq_miss_threshold_exceeded"), ctx).isPresent());
    }

    @Test
    void faqMiss_doesNotFire_outside_resolve_phase() {
        PhasePlan discoverPlan = PhasePlan.builder().phase("DISCOVER").useCase(null)
                .objective("x").allowedTools(List.of("classify_use_case")).maxToolSteps(2).build();
        Map<String, Object> acc = new LinkedHashMap<>();
        acc.put("search_knowledge", viableSearchHits());
        DispatchContext ctx = new DispatchContext(
                discoverPlan, null, acc, null, Optional.empty());
        assertFalse(dispatcher.checkBeforeDispatch(discoverPlan,
                handover("faq_miss_threshold_exceeded"), ctx).isPresent());
    }

    // ---------- intake_complete_required ----------

    @Test
    void intakeComplete_fires_when_fieldsMissing() {
        BotSession s = intakeSession("UC-K", null);
        DispatchContext ctx = new DispatchContext(
                intakePlan("UC-K"), s, Map.of(), null, Optional.empty());
        Optional<RejectVerdict> verdict = dispatcher.checkBeforeDispatch(
                intakePlan("UC-K"),
                handover("intake_complete_for_uc_k"), ctx);
        assertTrue(verdict.isPresent());
        assertEquals(SkillGuardrailDispatcher.INTAKE_INCOMPLETE_REJECT_REASON,
                verdict.get().predicateName());
        assertNotNull(verdict.get().trace().get("missing_fields"));
        assertEquals("resolve_intake_collect_and_handover",
                verdict.get().trace().get("skill_name"));
    }

    @Test
    void intakeComplete_doesNotFire_when_allFieldsPresent() {
        BotSession s = intakeSession("UC-K",
                "{\"platform\":\"Chrome on Mac\",\"repro_steps_or_error_message\":\"option missing\"}");
        DispatchContext ctx = new DispatchContext(
                intakePlan("UC-K"), s, Map.of(), null, Optional.empty());
        assertFalse(dispatcher.checkBeforeDispatch(intakePlan("UC-K"),
                handover("intake_complete_for_uc_k"), ctx).isPresent());
    }

    @Test
    void intakeComplete_doesNotFire_for_userRequested() {
        BotSession s = intakeSession("UC-K", null);
        DispatchContext ctx = new DispatchContext(
                intakePlan("UC-K"), s, Map.of(), null, Optional.empty());
        assertFalse(dispatcher.checkBeforeDispatch(intakePlan("UC-K"),
                handover("user_requested"), ctx).isPresent());
    }

    @Test
    void intakeComplete_doesNotFire_for_faqUc() {
        // UC-A routes to resolve_faq_grounded_answer Skill which does NOT
        // declare intake_complete_required.
        BotSession s = faqSession("UC-A", "RESOLVE");
        DispatchContext ctx = new DispatchContext(
                faqPlan("UC-A"), s, Map.of(), null, Optional.empty());
        assertFalse(dispatcher.checkBeforeDispatch(faqPlan("UC-A"),
                handover("intake_complete_for_uc_k"), ctx).isPresent());
    }

    // ---------- premature_resolve_outcome_guard ----------

    @Test
    void prematureResolve_fires_when_resolveBeforeConfirm() {
        BotSession s = faqSession("UC-A", "RESOLVE");
        DispatchContext ctx = new DispatchContext(
                faqPlan("UC-A"), s, Map.of(), null, Optional.empty());
        Optional<RejectVerdict> verdict = dispatcher.checkBeforeOutcomePersist(
                faqPlan("UC-A"), "resolve", ctx);
        assertTrue(verdict.isPresent());
        assertEquals(SkillGuardrailDispatcher.PROGRESSIVE_RESOLVE_REJECT_REASON,
                verdict.get().predicateName());
    }

    @Test
    void prematureResolve_doesNotFire_inConfirmPhase() {
        BotSession s = faqSession("UC-A", "CONFIRM");
        DispatchContext ctx = new DispatchContext(
                faqPlan("UC-A"), s, Map.of(), null, Optional.empty());
        // CONFIRM phase passes through ResolveDispositionEvaluator's guard;
        // BUT the must_cite_source guardrail (3rd on the FAQ Skill) still
        // fires because no source_id is in the user message.
        Optional<RejectVerdict> verdict = dispatcher.checkBeforeOutcomePersist(
                faqPlan("UC-A"), "resolve", ctx);
        // Verify it's NOT the premature_resolve label (the next guardrail
        // must_cite_source is what's firing here).
        if (verdict.isPresent()) {
            assertEquals(SkillGuardrailDispatcher.S1_CITATION_PRESENCE_REQUIRED,
                    verdict.get().predicateName(),
                    "premature_resolve_outcome_guard must pass through in CONFIRM phase; "
                            + "the must_cite_source guardrail downstream takes over.");
        }
    }

    @Test
    void prematureResolve_doesNotFire_for_escalateOutcome() {
        BotSession s = faqSession("UC-A", "RESOLVE");
        DispatchContext ctx = new DispatchContext(
                faqPlan("UC-A"), s, Map.of(), null, Optional.empty());
        // escalate outcome doesn't trigger premature-resolve OR must_cite_source.
        assertFalse(dispatcher.checkBeforeOutcomePersist(
                faqPlan("UC-A"), "escalate", ctx).isPresent());
    }

    // ---------- must_cite_source (NEW S1) ----------

    @Test
    void mustCiteSource_fires_when_classResolve_andNoSourceId() {
        // CONFIRM phase to bypass premature_resolve_outcome_guard.
        BotSession s = faqSession("UC-A", "CONFIRM");
        DispatchContext ctx = new DispatchContext(
                faqPlan("UC-A"), s, Map.of(),
                "Your ad is active for 30 days.",
                Optional.of("Your ad is active for 30 days."));
        Optional<RejectVerdict> verdict = dispatcher.checkBeforeOutcomePersist(
                faqPlan("UC-A"), "resolve", ctx);
        assertTrue(verdict.isPresent());
        assertEquals(SkillGuardrailDispatcher.S1_CITATION_PRESENCE_REQUIRED,
                verdict.get().predicateName());
    }

    // R5 — a real Salesforce-style article_id from the KB corpus
    // (data/knowledge/knowledge_base_articles.json). 18 chars, ka-prefixed.
    private static final String REAL_ARTICLE_ID = "ka41r000000LIEEAA4";

    @Test
    void mustCiteSource_doesNotFire_when_articleIdShapePresent() {
        // R5 — replaces the legacy literal "source_id" substring check. A
        // structural article_id token (the display_citation fallback) passes.
        BotSession s = faqSession("UC-A", "CONFIRM");
        String msg = "Your ad is active for 30 days [source_id: " + REAL_ARTICLE_ID + "].";
        DispatchContext ctx = new DispatchContext(
                faqPlan("UC-A"), s, Map.of(), msg, Optional.of(msg));
        assertFalse(dispatcher.checkBeforeOutcomePersist(
                faqPlan("UC-A"), "resolve", ctx).isPresent());
    }

    @Test
    void mustCiteSource_doesNotFire_when_httpsUrlPresent() {
        // R5 Positive A — the source_url citation shape (display_citation when
        // the article has a non-blank source_url).
        BotSession s = faqSession("UC-A", "CONFIRM");
        String msg = "You can manage this here: https://help.gumtree.com/article/123";
        DispatchContext ctx = new DispatchContext(
                faqPlan("UC-A"), s, Map.of(), msg, Optional.of(msg));
        assertFalse(dispatcher.checkBeforeOutcomePersist(
                faqPlan("UC-A"), "resolve", ctx).isPresent());
    }

    @Test
    void mustCiteSource_doesNotFire_when_httpUrlPresent() {
        // R5 Positive B — http:// (not just https://) also counts.
        BotSession s = faqSession("UC-A", "CONFIRM");
        String msg = "See http://help.gumtree.com/x for details.";
        DispatchContext ctx = new DispatchContext(
                faqPlan("UC-A"), s, Map.of(), msg, Optional.of(msg));
        assertFalse(dispatcher.checkBeforeOutcomePersist(
                faqPlan("UC-A"), "resolve", ctx).isPresent());
    }

    @Test
    void mustCiteSource_fires_when_emptyUserMessage() {
        // R5 Negative A — empty reply still rejects (grounding floor preserved).
        BotSession s = faqSession("UC-A", "CONFIRM");
        DispatchContext ctx = new DispatchContext(
                faqPlan("UC-A"), s, Map.of(), "", Optional.of(""));
        Optional<RejectVerdict> verdict = dispatcher.checkBeforeOutcomePersist(
                faqPlan("UC-A"), "resolve", ctx);
        assertTrue(verdict.isPresent());
        assertEquals(SkillGuardrailDispatcher.S1_CITATION_PRESENCE_REQUIRED,
                verdict.get().predicateName());
    }

    @Test
    void mustCiteSource_fires_when_nullUserMessage() {
        // R5 Negative B — null reply (both parsed + raw) still rejects.
        BotSession s = faqSession("UC-A", "CONFIRM");
        DispatchContext ctx = new DispatchContext(
                faqPlan("UC-A"), s, Map.of(), null, Optional.empty());
        Optional<RejectVerdict> verdict = dispatcher.checkBeforeOutcomePersist(
                faqPlan("UC-A"), "resolve", ctx);
        assertTrue(verdict.isPresent());
        assertEquals(SkillGuardrailDispatcher.S1_CITATION_PRESENCE_REQUIRED,
                verdict.get().predicateName());
    }

    @Test
    void mustCiteSource_fires_when_literalSourceIdWordButNoStructuralToken() {
        // R5 Negative D — the OLD literal-substring code PASSED this (the
        // string "source_id" appears); the structural-shape code REJECTS it
        // because there is no URL and no article_id-shape token. This pins the
        // literal -> shape semantics shift.
        BotSession s = faqSession("UC-A", "CONFIRM");
        String msg = "I'll cite the source_id for you.";
        DispatchContext ctx = new DispatchContext(
                faqPlan("UC-A"), s, Map.of(), msg, Optional.of(msg));
        Optional<RejectVerdict> verdict = dispatcher.checkBeforeOutcomePersist(
                faqPlan("UC-A"), "resolve", ctx);
        assertTrue(verdict.isPresent());
        assertEquals(SkillGuardrailDispatcher.S1_CITATION_PRESENCE_REQUIRED,
                verdict.get().predicateName());
    }

    @Test
    void mustCiteSource_bowsOut_when_yamlOutcomeClassDisagrees() {
        // R5 Negative F — a must_cite_source guardrail whose configured
        // parameters.outcome_class != resolve bows out even on a resolve
        // record_outcome (Sprint 39 §2.5 D-n contract preserved).
        Skill skill = new Skill(
                "x", "x",
                List.of("RESOLVE"),
                List.of("UC-A"),
                List.of("record_outcome"),
                List.of("form_context"),
                3, false,
                List.of("FINAL_ANSWER"),
                "obj", "proc", "g", "e",
                List.of(new Guardrail("must_cite_source", "reject_with_hint",
                        Map.of("outcome_class", "escalate"))),
                StateInheritance.EMPTY);
        DispatchContext ctx = new DispatchContext(
                faqPlan("UC-A"), null, Map.of(),
                "No citation here.", Optional.of("No citation here."));
        assertFalse(dispatcher.checkBeforeOutcomePersist(skill, "resolve", ctx).isPresent());
    }

    @Test
    void mustCiteSource_doesNotFire_for_classEscalate() {
        // M2 §6 #4 verbatim authorization: S1 fires ONLY on class=resolve.
        BotSession s = faqSession("UC-A", "RESOLVE");
        DispatchContext ctx = new DispatchContext(
                faqPlan("UC-A"), s, Map.of(),
                "Connecting you to a specialist.",
                Optional.of("Connecting you to a specialist."));
        assertFalse(dispatcher.checkBeforeOutcomePersist(
                faqPlan("UC-A"), "escalate", ctx).isPresent());
    }

    @Test
    void mustCiteSource_doesNotFire_for_classAbandon() {
        // M2 §6 #4 verbatim authorization: S1 fires ONLY on class=resolve.
        BotSession s = faqSession("UC-A", "RESOLVE");
        DispatchContext ctx = new DispatchContext(
                faqPlan("UC-A"), s, Map.of(),
                "Conversation closed without resolution.",
                Optional.of("Conversation closed without resolution."));
        assertFalse(dispatcher.checkBeforeOutcomePersist(
                faqPlan("UC-A"), "abandon", ctx).isPresent());
    }

    @Test
    void mustCiteSource_doesNotFire_outside_resolve_faq_scope() {
        // UC-K (INTAKE Skill) does not declare must_cite_source; the
        // dispatcher does not invoke the handler.
        BotSession s = intakeSession("UC-K",
                "{\"platform\":\"Mac\",\"repro_steps_or_error_message\":\"button gone\"}");
        DispatchContext ctx = new DispatchContext(
                intakePlan("UC-K"), s, Map.of(),
                "Connecting you to Technical Support.",
                Optional.of("Connecting you to Technical Support."));
        assertFalse(dispatcher.checkBeforeOutcomePersist(
                intakePlan("UC-K"), "resolve", ctx).isPresent());
    }

    @Test
    void mustCiteSource_handlesRESOLVEDLegacyAlias() {
        // RecordOutcomeTool.normalizeOutcomeClass resolves canonical
        // "resolve" + legacy "RESOLVED" alias; the dispatcher mirrors this.
        BotSession s = faqSession("UC-A", "CONFIRM");
        DispatchContext ctx = new DispatchContext(
                faqPlan("UC-A"), s, Map.of(),
                "Your ad is active.", Optional.of("Your ad is active."));
        Optional<RejectVerdict> verdict = dispatcher.checkBeforeOutcomePersist(
                faqPlan("UC-A"), "RESOLVED", ctx);
        assertTrue(verdict.isPresent());
        assertEquals(SkillGuardrailDispatcher.S1_CITATION_PRESENCE_REQUIRED,
                verdict.get().predicateName());
    }

    // ---------- short-circuit + composition ----------

    @Test
    void shortCircuit_firstGuardrailRejectWinsOnFaqResolveOutcome() {
        // RESOLVE_FAQ × resolve outcome: 1st on-outcome guardrail
        // (premature_resolve_outcome_guard) fires in RESOLVE phase; the
        // 2nd (must_cite_source) is NOT evaluated.
        BotSession s = faqSession("UC-A", "RESOLVE");
        DispatchContext ctx = new DispatchContext(
                faqPlan("UC-A"), s, Map.of(),
                "Your ad is active.", Optional.of("Your ad is active."));
        Optional<RejectVerdict> verdict = dispatcher.checkBeforeOutcomePersist(
                faqPlan("UC-A"), "resolve", ctx);
        assertTrue(verdict.isPresent());
        assertEquals(SkillGuardrailDispatcher.PROGRESSIVE_RESOLVE_REJECT_REASON,
                verdict.get().predicateName(),
                "Short-circuit on first reject: premature_resolve_outcome_guard "
                        + "fires before must_cite_source has a chance to evaluate.");
    }

    // ---------- DispatchContext null-safety ----------

    @Test
    void dispatch_returnsEmpty_whenPlanIsNull() {
        DispatchContext ctx = new DispatchContext(
                null, null, Map.of(), null, Optional.empty());
        assertFalse(dispatcher.checkBeforeDispatch((PhasePlan) null,
                handover("user_requested"), ctx).isPresent());
        assertFalse(dispatcher.checkBeforeOutcomePersist((PhasePlan) null,
                "resolve", ctx).isPresent());
    }

    @Test
    void dispatchContext_normalizesNullAccumulated() {
        // null accumulatedToolResults → empty map (DispatchContext compact ctor).
        DispatchContext ctx = new DispatchContext(
                faqPlan("UC-A"), null, null, null, null);
        assertNotNull(ctx.accumulatedToolResults());
        assertEquals(0, ctx.accumulatedToolResults().size());
        assertEquals(Optional.empty(), ctx.parsedUserMessage());
    }

    @Test
    void rejectVerdict_traceMapIsImmutableCopy() {
        // Trace map copy guard: the dispatcher must not return a verdict whose
        // trace map can be mutated by the caller.
        Map<String, Object> acc = new LinkedHashMap<>();
        acc.put("search_knowledge", viableSearchHits());
        DispatchContext ctx = new DispatchContext(
                faqPlan("UC-A"), null, acc, null, Optional.empty());
        Optional<RejectVerdict> verdict = dispatcher.checkBeforeDispatch(faqPlan("UC-A"),
                handover("faq_miss_threshold_exceeded"), ctx);
        assertTrue(verdict.isPresent());
        // Map.copyOf returns an immutable map.
        try {
            verdict.get().trace().put("x", "y");
            assertTrue(false, "Expected UnsupportedOperationException on trace mutation");
        } catch (UnsupportedOperationException ignored) {
            // expected
        }
    }

    @Test
    void unknownGuardrailType_treatedAsNoOp_byDispatcher() {
        // Synthetic Skill with an in-allowlist guardrail type used in an
        // unexpected position: dispatcher walks all guardrails but
        // type-mismatched ones (e.g., on-outcome type for tool-call dispatch)
        // are no-ops. Verifies the dispatcher does not throw or short-circuit
        // on these cases.
        Skill skill = new Skill(
                "x", "x",
                List.of("RESOLVE"),
                List.of("UC-A"),
                List.of("request_handover"),
                List.of("form_context"),
                3, false,
                List.of("ESCALATE"),
                "obj", "proc", "g", "e",
                List.of(new Guardrail("must_cite_source", "reject_with_hint",
                        Map.of("outcome_class", "resolve"))),
                StateInheritance.EMPTY);
        DispatchContext ctx = new DispatchContext(
                faqPlan("UC-A"), null, Map.of(), null, Optional.empty());
        // checkBeforeDispatch on a tool call: must_cite_source is an outcome
        // guardrail, so the handler short-circuits (no-op) and returns empty.
        assertFalse(dispatcher.checkBeforeDispatch(skill,
                handover("faq_miss_threshold_exceeded"), ctx).isPresent());
    }
}
