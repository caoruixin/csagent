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
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Direct unit coverage for
 * {@link PhaseEvaluator#resolveMaxStepsReason(PhasePlan, AgentRunResult, BotSession)}.
 * Codex 2026-05-03 round 3 flagged the helper as untested and the previous
 * heuristic order (search_knowledge before clarification) misattributed mixed
 * search + clarify loops. These tests pin the corrected precedence:
 *
 * <ol>
 *   <li>INTAKE plan → {@code incomplete_intake}</li>
 *   <li>Session has clarifications → {@code clarification_budget_exhausted}</li>
 *   <li>Loop ran search_knowledge → {@code faq_miss_threshold_exceeded}</li>
 *   <li>Otherwise → {@code turn_budget_exhausted}</li>
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

    private AgentRunResult resultWithSearchKnowledge() {
        ToolEvent te = new ToolEvent(0, 0, "search_knowledge",
                java.util.Map.of(), true, null, null, 5);
        return AgentRunResult.maxSteps(List.of(), List.of(te), null);
    }

    private AgentRunResult resultEmpty() {
        return AgentRunResult.maxSteps(List.of(), List.of(), null);
    }

    @Test
    void intakePlan_returnsIncompleteIntake_evenWhenSearchedKnowledge() {
        // INTAKE plans should never be attributed to faq_miss; they are
        // structurally not allowed to call search_knowledge, so a search
        // event in this branch is a defensive observation only.
        String reason = evaluator.resolveMaxStepsReason(
                plan("RESOLVE", "UC-J"),
                resultWithSearchKnowledge(),
                sessionWithClarifications(1));
        assertEquals("incomplete_intake", reason);
    }

    @Test
    void clarificationCheckedBeforeSearch_mixedLoop() {
        // Codex round 3: a mixed search + clarify loop must attribute to
        // clarification_budget_exhausted, not faq_miss_threshold_exceeded.
        String reason = evaluator.resolveMaxStepsReason(
                plan("RESOLVE", "UC-A"),
                resultWithSearchKnowledge(),
                sessionWithClarifications(2));
        assertEquals("clarification_budget_exhausted", reason);
    }

    @Test
    void searchKnowledgeOnly_noClarifications_returnsFaqMiss() {
        String reason = evaluator.resolveMaxStepsReason(
                plan("RESOLVE", "UC-B"),
                resultWithSearchKnowledge(),
                sessionWithClarifications(0));
        assertEquals("faq_miss_threshold_exceeded", reason);
    }

    @Test
    void noEvidence_fallsBackToTurnBudgetExhausted() {
        String reason = evaluator.resolveMaxStepsReason(
                plan("RESOLVE", "UC-A"),
                resultEmpty(),
                sessionWithClarifications(0));
        assertEquals("turn_budget_exhausted", reason);
    }

    @Test
    void nullPlan_treatedAsNonIntake() {
        String reason = evaluator.resolveMaxStepsReason(
                null,
                resultWithSearchKnowledge(),
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
