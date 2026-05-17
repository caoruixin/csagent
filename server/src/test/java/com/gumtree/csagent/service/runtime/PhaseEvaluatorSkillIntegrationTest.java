package com.gumtree.csagent.service.runtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gumtree.csagent.model.BotSession;
import com.gumtree.csagent.model.PhasePlan;
import com.gumtree.csagent.model.TerminalOutcome;
import com.gumtree.csagent.service.guardrails.ScriptLibraryService;
import com.gumtree.csagent.service.knowledge.KnowledgeSearchService;
import com.gumtree.csagent.service.observability.EventEmitter;
import com.gumtree.csagent.service.runtime.skill.SkillTestFixtures;
import com.gumtree.csagent.service.tools.CreateCaseControlledTool;
import com.gumtree.csagent.service.tools.ToolDispatcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Sprint 38 behavioural-equivalence integration test per Sprint 37 freeze
 * §6.4 (decision (e) §6.2.1-§6.2.4).
 *
 * <p>For each of the 4 phases migrated in Sprint 38 (DISCOVER + CONFIRM +
 * ESCALATE + CLOSE), this test pins the post-migration PhasePlan composed by
 * the SkillRegistry path to the GOLDEN pre-migration PhasePlan that the
 * legacy hardcoded Java-string branches in {@code PhaseEvaluator.java:397-542}
 * produced at HEAD {@code 51c327c} (Sprint 37 close).
 *
 * <p>The golden strings below are bit-for-bit equivalent to what the legacy
 * branches concatenated, modulo whitespace normalization the Java string
 * concatenation already performed. Future Skill content changes (Sprint 39+)
 * MAY change these expectations; until then, the Sprint 38 migration's
 * primary acceptance gate (per Sprint 38 contract §9) is "Skill-composed
 * PhasePlan is field-by-field equal to pre-migration golden for representative
 * UCs in each of the 4 phases".
 */
@ExtendWith(MockitoExtension.class)
class PhaseEvaluatorSkillIntegrationTest {

    @Mock private UseCaseRegistryService useCaseRegistry;
    @Mock private KnowledgeSearchService knowledgeSearchService;
    @Mock private ScriptLibraryService scriptLibrary;
    @Mock private LlmInvocationService llmInvocation;
    @Mock private ContextProjectionBuilder contextProjection;
    @Mock private ActionParser actionParser;
    @Mock private CreateCaseControlledTool createCaseTool;
    @Mock private EventEmitter eventEmitter;
    @Mock private ToolDispatcher toolDispatcher;

    private PhaseEvaluator evaluator;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        evaluator = new PhaseEvaluator(
                useCaseRegistry, knowledgeSearchService, scriptLibrary,
                llmInvocation, contextProjection, actionParser,
                objectMapper, createCaseTool, eventEmitter, toolDispatcher,
                SkillTestFixtures.productionRegistry());
    }

    private BotSession session(String phase, String uc) {
        BotSession s = new BotSession();
        s.setSessionId("sess-equiv");
        s.setCurrentPhase(phase);
        s.setActiveUseCase(uc);
        return s;
    }

    // -------------------- golden strings (pre-migration PhaseEvaluator branches at HEAD 51c327c) --------------------

    private static final String DISCOVER_OBJECTIVE =
            "Identify the user's use case by asking clarifying questions or "
                    + "interpreting their message, then commit it via classify_use_case";

    private static final String DISCOVER_SYSTEM_INSTRUCTION =
            "You are in the DISCOVER phase. Your goal is to identify which Use Case "
                    + "applies to the customer. Look at the form context, candidate use cases, "
                    + "and conversation history. When the user's intent is clear (or you can "
                    + "infer it with a supporting detail), call classify_use_case with the "
                    + "matching use_case_id and a confidence in [0,1]. Otherwise ask one clear "
                    + "clarifying question, or escalate if the user's request is out of scope. "
                    + "Sprint 7 §I0 weak-candidate cue: when "
                    + "`candidate_use_cases` is empty or weak AND the form context is empty / "
                    + "UNKNOWN topic AND the current user message is clearly FAQ-shaped "
                    + "(\"how do I X\", \"can I Y\", \"what items are allowed\") OR is "
                    + "payment / sale-proceeds-shaped (\"how do I receive payment\", "
                    + "\"how do I get paid when I sell\", \"how does payout work\"), do NOT "
                    + "request_handover with `faq_miss_threshold_exceeded` after a single user "
                    + "turn. Instead, gather enough evidence to classify toward the right "
                    + "FAQ-path UC: call `search_knowledge` with the user's question as the "
                    + "query, then call `classify_use_case` with the most plausible UC "
                    + "(payment / sale-proceeds questions point to UC-F; how-to-post and "
                    + "general advertising questions to UC-B; messaging to UC-C; account / "
                    + "login to UC-D). Once classified, RESOLVE will run the grounded resolve "
                    + "sequence. "
                    + "Sprint 33 ad-status disambiguation cue (read alongside "
                    + "`candidate_use_cases` and the `discover_disambiguation_signals` "
                    + "projection): when the projection shows the user's listing is in a "
                    + "not-visible state (`ad_status_observed` is one of REMOVED / SUSPENDED "
                    + "/ EXPIRED) AND `topic_subject_carries_multiple_candidate_ucs` is true, "
                    + "the user's literal request is the disambiguation signal — not the "
                    + "listing's database row. A user asking why the ad is gone, what "
                    + "happened to it, or where it went is asking to UNDERSTAND the "
                    + "situation (FAQ-resolvable, classify toward the visibility / ad-status "
                    + "explanation UC). A user asking to appeal, contest, or reverse the "
                    + "removal is asking to ACT (the appeal UC, an intake path). If the "
                    + "user's request is ambiguous between understanding and acting, ask "
                    + "ONE focused clarifying question this turn before committing "
                    + "classify_use_case (for example: \"Do you want to know the reason it "
                    + "was removed, or do you want to appeal the removal?\"). Do not commit "
                    + "an intake-path UC purely on `ad_status_observed` alone; the user's "
                    + "stated need is the disambiguation signal.";

    private static final String DISCOVER_GROUNDING_INSTRUCTION =
            "Do not commit to detailed answers in DISCOVER. Your job is to determine the "
                    + "use case category (call classify_use_case), then RESOLVE will produce the "
                    + "actual resolution.";

    private static final String DISCOVER_ESCALATION_POLICY =
            "Escalate if user explicitly requests human help, if request is clearly out of scope, "
                    + "or if you cannot disambiguate after one clarification.";

    private static final String CONFIRM_OBJECTIVE =
            "Determine whether the user is satisfied with the prior answer";

    private static final String CONFIRM_SYSTEM_INSTRUCTION =
            "You are in the CONFIRM phase. Interpret whether the user is satisfied with "
                    + "the prior answer. If satisfied (e.g., 'thanks', 'that helps', 'yes'), "
                    + "call record_outcome with outcome='RESOLVED'. If not satisfied (e.g., "
                    + "'no', 'still not working', 'I need more help'), call request_handover "
                    + "with reason 'user_dissatisfied' OR transition back to RESOLVE if "
                    + "appropriate.";

    private static final String CONFIRM_GROUNDING_INSTRUCTION =
            "Read the user's response carefully. Sentiment matters more than literal words. "
                    + "When unclear, ask a single yes/no clarification.";

    private static final String CONFIRM_ESCALATION_POLICY =
            "Escalate if user clearly expresses dissatisfaction or requests a human.";

    private static final String CLOSE_OBJECTIVE =
            "Send a polite closing message and record the final outcome";

    private static final String CLOSE_SYSTEM_INSTRUCTION =
            "You are in the CLOSE phase. Thank the user and confirm the outcome. "
                    + "Call record_outcome with the appropriate outcome if not already recorded.";

    private static final String CLOSE_GROUNDING_INSTRUCTION =
            "Keep the closing message brief, warm, and final. Do not introduce new topics.";

    private static final String CLOSE_ESCALATION_POLICY =
            "Do not escalate from CLOSE. The session is ending.";

    private static final String ESCALATE_OBJECTIVE =
            "Complete the handover to a human agent and inform the customer";

    private static final String ESCALATE_SYSTEM_INSTRUCTION =
            "You are in the ESCALATE phase. Send a clear handover message and ensure "
                    + "request_handover has been called with an appropriate escalation_reason.";

    private static final String ESCALATE_GROUNDING_INSTRUCTION =
            "Tell the user a human agent will assist them. Provide expected SLA if known. "
                    + "Do not promise specific outcomes.";

    private static final String ESCALATE_ESCALATION_POLICY =
            "Already in ESCALATE — finalize the handover.";

    // -------------------- DISCOVER --------------------

    @Test
    void discover_nullActiveUc_composesGoldenPhasePlan() {
        PhasePlan plan = evaluator.plan(session("DISCOVER", null), "hi", List.of());
        assertGoldenDiscover(plan, null);
    }

    @Test
    void discover_anyActiveUc_composesGoldenPhasePlan() {
        // DISCOVER Skill applicable_use_cases: ["*"] → composes for any UC.
        PhasePlan plan = evaluator.plan(session("DISCOVER", "UC-A"), "hi", List.of());
        assertGoldenDiscover(plan, "UC-A");
    }

    @Test
    void discover_intakeUc_stillComposesGoldenPhasePlan() {
        PhasePlan plan = evaluator.plan(session("DISCOVER", "UC-K"), "hi", List.of());
        assertGoldenDiscover(plan, "UC-K");
    }

    private void assertGoldenDiscover(PhasePlan plan, String expectedUc) {
        assertNotNull(plan);
        assertEquals("DISCOVER", plan.phase());
        assertEquals(expectedUc, plan.useCase());
        assertEquals(DISCOVER_OBJECTIVE, plan.objective());
        assertEquals(List.of("search_knowledge", "classify_use_case"), plan.allowedTools());
        assertEquals(Set.of("form_context", "candidate_use_cases"), plan.requiredContextKeys());
        assertEquals(2, plan.maxToolSteps());
        assertEquals(false, plan.allowInterimMessage());
        assertEquals(Set.of(TerminalOutcome.CLARIFICATION_NEEDED,
                        TerminalOutcome.FINAL_ANSWER,
                        TerminalOutcome.ESCALATE),
                plan.validTerminalOutcomes());
        assertEquals(DISCOVER_SYSTEM_INSTRUCTION, plan.systemInstruction());
        assertEquals(DISCOVER_GROUNDING_INSTRUCTION, plan.groundingInstruction());
        assertEquals(DISCOVER_ESCALATION_POLICY, plan.escalationPolicy());
    }

    // -------------------- CONFIRM --------------------

    @Test
    void confirm_anyActiveUc_composesGoldenPhasePlan() {
        PhasePlan plan = evaluator.plan(session("CONFIRM", "UC-A"), "thanks", List.of());
        assertGoldenConfirm(plan, "UC-A");
    }

    @Test
    void confirm_intakeUc_composesGoldenPhasePlan() {
        PhasePlan plan = evaluator.plan(session("CONFIRM", "UC-G"), "ok", List.of());
        assertGoldenConfirm(plan, "UC-G");
    }

    @Test
    void confirm_nullUc_composesGoldenPhasePlan() {
        PhasePlan plan = evaluator.plan(session("CONFIRM", null), "ok", List.of());
        assertGoldenConfirm(plan, null);
    }

    private void assertGoldenConfirm(PhasePlan plan, String expectedUc) {
        assertNotNull(plan);
        assertEquals("CONFIRM", plan.phase());
        assertEquals(expectedUc, plan.useCase());
        assertEquals(CONFIRM_OBJECTIVE, plan.objective());
        assertEquals(List.of("record_outcome", "request_handover"), plan.allowedTools());
        assertEquals(Set.of("form_context", "conversation_history"), plan.requiredContextKeys());
        assertEquals(2, plan.maxToolSteps());
        assertEquals(false, plan.allowInterimMessage());
        assertEquals(Set.of(TerminalOutcome.FINAL_ANSWER,
                        TerminalOutcome.CLARIFICATION_NEEDED,
                        TerminalOutcome.ESCALATE),
                plan.validTerminalOutcomes());
        assertEquals(CONFIRM_SYSTEM_INSTRUCTION, plan.systemInstruction());
        assertEquals(CONFIRM_GROUNDING_INSTRUCTION, plan.groundingInstruction());
        assertEquals(CONFIRM_ESCALATION_POLICY, plan.escalationPolicy());
    }

    // -------------------- CLOSE --------------------

    @Test
    void close_anyActiveUc_composesGoldenPhasePlan() {
        PhasePlan plan = evaluator.plan(session("CLOSE", "UC-A"), "bye", List.of());
        assertGoldenClose(plan, "UC-A");
    }

    @Test
    void close_intakeUc_composesGoldenPhasePlan() {
        PhasePlan plan = evaluator.plan(session("CLOSE", "UC-K"), "bye", List.of());
        assertGoldenClose(plan, "UC-K");
    }

    @Test
    void close_nullUc_composesGoldenPhasePlan() {
        PhasePlan plan = evaluator.plan(session("CLOSE", null), "bye", List.of());
        assertGoldenClose(plan, null);
    }

    private void assertGoldenClose(PhasePlan plan, String expectedUc) {
        assertNotNull(plan);
        assertEquals("CLOSE", plan.phase());
        assertEquals(expectedUc, plan.useCase());
        assertEquals(CLOSE_OBJECTIVE, plan.objective());
        assertEquals(List.of("record_outcome"), plan.allowedTools());
        assertEquals(Set.of("form_context"), plan.requiredContextKeys());
        assertEquals(2, plan.maxToolSteps());
        assertEquals(false, plan.allowInterimMessage());
        assertEquals(Set.of(TerminalOutcome.FINAL_ANSWER), plan.validTerminalOutcomes());
        assertEquals(CLOSE_SYSTEM_INSTRUCTION, plan.systemInstruction());
        assertEquals(CLOSE_GROUNDING_INSTRUCTION, plan.groundingInstruction());
        assertEquals(CLOSE_ESCALATION_POLICY, plan.escalationPolicy());
    }

    // -------------------- ESCALATE --------------------

    @Test
    void escalate_anyActiveUc_composesGoldenPhasePlan() {
        PhasePlan plan = evaluator.plan(session("ESCALATE", "UC-A"), "human please", List.of());
        assertGoldenEscalate(plan, "UC-A");
    }

    @Test
    void escalate_intakeUc_composesGoldenPhasePlan() {
        PhasePlan plan = evaluator.plan(session("ESCALATE", "UC-H"), "human please", List.of());
        assertGoldenEscalate(plan, "UC-H");
    }

    @Test
    void escalate_nullUc_composesGoldenPhasePlan() {
        PhasePlan plan = evaluator.plan(session("ESCALATE", null), "human please", List.of());
        assertGoldenEscalate(plan, null);
    }

    private void assertGoldenEscalate(PhasePlan plan, String expectedUc) {
        assertNotNull(plan);
        assertEquals("ESCALATE", plan.phase());
        assertEquals(expectedUc, plan.useCase());
        assertEquals(ESCALATE_OBJECTIVE, plan.objective());
        assertEquals(List.of("request_handover", "record_outcome"), plan.allowedTools());
        assertEquals(Set.of("form_context", "customer_context"), plan.requiredContextKeys());
        assertEquals(2, plan.maxToolSteps());
        assertEquals(false, plan.allowInterimMessage());
        assertEquals(Set.of(TerminalOutcome.ESCALATE, TerminalOutcome.FINAL_ANSWER),
                plan.validTerminalOutcomes());
        assertEquals(ESCALATE_SYSTEM_INSTRUCTION, plan.systemInstruction());
        assertEquals(ESCALATE_GROUNDING_INSTRUCTION, plan.groundingInstruction());
        assertEquals(ESCALATE_ESCALATION_POLICY, plan.escalationPolicy());
    }

    // -------------------- legacy phases preserved unchanged (Sprint 39 scope) --------------------

    @Test
    void resolve_intakeUc_stillFollowsLegacyBranchUntilSprint39() {
        // Sprint 38 does NOT migrate RESOLVE_INTAKE; the legacy branch at
        // PhaseEvaluator.java still runs. SkillRegistry.select(RESOLVE, UC-G)
        // returns Optional.empty() because no RESOLVE Skill is loaded yet.
        org.mockito.Mockito.when(useCaseRegistry.getUseCase("UC-G")).thenReturn(
                new UseCaseRegistryService.UseCaseDefinition(
                        "UC-G", "GDPR Intake",
                        List.of("Privacy"), "HIGH", false, "INTAKE"));
        PhasePlan plan = evaluator.plan(session("RESOLVE", "UC-G"), "delete my data", List.of());
        assertNotNull(plan);
        assertEquals("RESOLVE", plan.phase());
        assertEquals("UC-G", plan.useCase());
        assertEquals(List.of("request_handover"), plan.allowedTools());
        // Sprint 39 will migrate this; for now the legacy branch handles it.
    }
}
