package com.gumtree.csagent.service.runtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gumtree.csagent.model.AgentRunResult;
import com.gumtree.csagent.model.BotSession;
import com.gumtree.csagent.model.PhasePlan;
import com.gumtree.csagent.model.TerminalOutcome;
import com.gumtree.csagent.model.ToolEvent;
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
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

/**
 * Direct unit coverage for
 * {@link PhaseEvaluator#resolveMaxStepsReason(PhasePlan, AgentRunResult, BotSession)}.
 *
 * <p>Sprint 070 / S-Auto-14 (B1) made step 3 evidence-aware: a MAX_STEPS exit
 * is attributed to {@code faq_miss_threshold_exceeded} only when the
 * MOST-RECENT {@code search_knowledge} result was a genuine miss
 * ({@code faq_miss=true}). A search that returned a viable hit
 * ({@code faq_miss=false}) — or no {@code search_knowledge} at all, or a
 * null/malformed result map — falls through to the {@code turn_budget_exhausted}
 * catch-all: the loop ran out of budget with (potentially) a usable answer in
 * hand, which is not a knowledge miss. Before B1, ANY {@code search_knowledge}
 * presence attributed to {@code faq_miss_threshold_exceeded}, mis-labelling
 * viable-hit exhaustions. These tests pin the corrected contract and its
 * precedence:
 *
 * <ol>
 *   <li>INTAKE plan → {@code incomplete_intake}</li>
 *   <li>Session has clarifications → {@code clarification_budget_exhausted}</li>
 *   <li>Most-recent search_knowledge {@code faq_miss=true} → {@code
 *       faq_miss_threshold_exceeded}</li>
 *   <li>Otherwise (viable last hit / no search / null data) → {@code
 *       turn_budget_exhausted}</li>
 * </ol>
 */
@ExtendWith(MockitoExtension.class)
class PhaseEvaluatorMaxStepsResolverTest {

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

    @BeforeEach
    void setUp() {
        evaluator = new PhaseEvaluator(
                useCaseRegistry, knowledgeSearchService, scriptLibrary,
                llmInvocation, contextProjection, actionParser,
                new ObjectMapper(), createCaseTool, eventEmitter, toolDispatcher,
                SkillTestFixtures.productionRegistry(), null);
    }

    private PhasePlan plan(String phase, String useCase) {
        return PhasePlan.builder()
                .phase(phase)
                .useCase(useCase)
                .objective("test")
                .allowedTools(List.of())
                .requiredContextKeys(Set.of())
                .maxToolSteps(2)
                .allowInterimMessage(false)
                .validTerminalOutcomes(Set.of(TerminalOutcome.FINAL_ANSWER, TerminalOutcome.ESCALATE))
                .systemInstruction("test")
                .groundingInstruction("test")
                .escalationPolicy("test")
                .build();
    }

    private BotSession sessionWithClarifications(int count) {
        BotSession s = new BotSession();
        s.setSessionId("sess");
        s.setClarificationCount(count);
        return s;
    }

    /** A search_knowledge event whose dispatched result carries the faq_miss flag. */
    private ToolEvent searchEvent(int step, boolean faqMiss) {
        return new ToolEvent(step, step, "search_knowledge",
                Map.of(), true, Map.of("faq_miss", faqMiss), null, 5);
    }

    /** A search_knowledge event with a null result map (defensive / malformed). */
    private ToolEvent searchEventNullData(int step) {
        return new ToolEvent(step, step, "search_knowledge",
                Map.of(), true, null, null, 5);
    }

    private AgentRunResult maxStepsWith(ToolEvent... events) {
        return AgentRunResult.maxSteps(List.of(), List.of(events), null);
    }

    private AgentRunResult resultEmpty() {
        return AgentRunResult.maxSteps(List.of(), List.of(), null);
    }

    // ---- Step 3 (B1): evidence-aware FAQ attribution -----------------------

    @Test
    void mostRecentFaqMissFalse_viableHit_fallsThroughToTurnBudget() {
        // The B1 fix: a MAX_STEPS exit whose last search_knowledge returned a
        // viable hit (faq_miss=false) ran out of budget WITH a usable answer —
        // turn_budget_exhausted, not a knowledge miss.
        String reason = evaluator.resolveMaxStepsReason(
                plan("RESOLVE", "UC-C"),
                maxStepsWith(searchEvent(0, false)),
                sessionWithClarifications(0));
        assertEquals("turn_budget_exhausted", reason);
    }

    @Test
    void mostRecentFaqMissTrue_genuineMiss_returnsFaqMiss() {
        // Negative control: a genuine miss (last search faq_miss=true) is still
        // correctly attributed to faq_miss_threshold_exceeded.
        String reason = evaluator.resolveMaxStepsReason(
                plan("RESOLVE", "UC-B"),
                maxStepsWith(searchEvent(0, true)),
                sessionWithClarifications(0));
        assertEquals("faq_miss_threshold_exceeded", reason);
    }

    @Test
    void mixedTurn_earlyMissThenViableHit_mostRecentWins_turnBudget() {
        // Most-recent wins: an early miss followed by a viable hit means the
        // loop ended with a usable answer → turn_budget_exhausted.
        String reason = evaluator.resolveMaxStepsReason(
                plan("RESOLVE", "UC-A"),
                maxStepsWith(searchEvent(0, true), searchEvent(1, false)),
                sessionWithClarifications(0));
        assertEquals("turn_budget_exhausted", reason);
    }

    @Test
    void mixedTurn_earlyHitThenMiss_mostRecentWins_faqMiss() {
        // Most-recent wins, other direction (the cs014 shape): an early viable
        // hit followed by repeated misses ends on a genuine miss →
        // faq_miss_threshold_exceeded.
        String reason = evaluator.resolveMaxStepsReason(
                plan("RESOLVE", "UC-C"),
                maxStepsWith(searchEvent(0, false), searchEvent(1, true)),
                sessionWithClarifications(0));
        assertEquals("faq_miss_threshold_exceeded", reason);
    }

    @Test
    void searchKnowledge_nullResultData_guardedToTurnBudget() {
        // Defensive: a null/malformed result map carries no positive miss
        // evidence, so it falls through to the catch-all rather than NPEing.
        String reason = evaluator.resolveMaxStepsReason(
                plan("RESOLVE", "UC-A"),
                maxStepsWith(searchEventNullData(0)),
                sessionWithClarifications(0));
        assertEquals("turn_budget_exhausted", reason);
    }

    @Test
    void noSearchKnowledge_fallsBackToTurnBudgetExhausted() {
        String reason = evaluator.resolveMaxStepsReason(
                plan("RESOLVE", "UC-A"),
                resultEmpty(),
                sessionWithClarifications(0));
        assertEquals("turn_budget_exhausted", reason);
    }

    // ---- Precedence: step 1 (intake) and step 2 (clarification) ------------

    @Test
    void intakePlan_returnsIncompleteIntake_evenWhenSearchedKnowledge() {
        // Intake-ONLY plans short-circuit at step 1 and are never attributed to
        // faq_miss; a search event (even a genuine miss) is shadowed.
        //
        // WS-3 / A3: step 1 now reads the UC's registry `path` instead of a
        // literal UC set, so the mocked registry must supply UC-J's definition.
        when(useCaseRegistry.getUseCase("UC-J")).thenReturn(
                new UseCaseRegistryService.UseCaseDefinition(
                        "UC-J", "Trust & Safety Report",
                        List.of("Report a Safety Issue"), "CRITICAL", false, "INTAKE"));
        String reason = evaluator.resolveMaxStepsReason(
                plan("RESOLVE", "UC-J"),
                maxStepsWith(searchEvent(0, true)),
                sessionWithClarifications(1));
        assertEquals("incomplete_intake", reason);
    }

    @Test
    void partialPathPlan_isNotShortCircuitedToIncompleteIntake() {
        // WS-3 / A3 negative control: UC-K's registry path is PARTIAL, so a
        // MAX_STEPS exit is attributed on the evidence (a genuine faq_miss
        // here), not stamped `incomplete_intake` on the strength of the UC id.
        when(useCaseRegistry.getUseCase("UC-K")).thenReturn(
                new UseCaseRegistryService.UseCaseDefinition(
                        "UC-K", "Technical Issue Intake",
                        List.of("Technical Support"), "MEDIUM", true, "PARTIAL"));
        String reason = evaluator.resolveMaxStepsReason(
                plan("RESOLVE", "UC-K"),
                maxStepsWith(searchEvent(0, true)),
                sessionWithClarifications(0));
        assertEquals("faq_miss_threshold_exceeded", reason);
    }

    @Test
    void clarificationCheckedBeforeSearch_mixedLoop() {
        // Step 2 precedence: a mixed search + clarify loop attributes to
        // clarification_budget_exhausted, regardless of the last faq_miss.
        String reason = evaluator.resolveMaxStepsReason(
                plan("RESOLVE", "UC-A"),
                maxStepsWith(searchEvent(0, false)),
                sessionWithClarifications(2));
        assertEquals("clarification_budget_exhausted", reason);
    }

    // ---- Null-safety on plan / session -------------------------------------

    @Test
    void nullPlan_treatedAsNonIntake_genuineMissStillFaqMiss() {
        // A null plan must not short-circuit at step 1; it falls through to the
        // faq_miss check, and a genuine miss returns faq_miss_threshold_exceeded.
        String reason = evaluator.resolveMaxStepsReason(
                null,
                maxStepsWith(searchEvent(0, true)),
                sessionWithClarifications(0));
        assertEquals("faq_miss_threshold_exceeded", reason);
    }

    @Test
    void nullSession_skipsClarificationCheck() {
        String reason = evaluator.resolveMaxStepsReason(
                plan("RESOLVE", "UC-A"),
                resultEmpty(),
                null);
        assertEquals("turn_budget_exhausted", reason);
    }
}
