package com.gumtree.csagent.service.runtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gumtree.csagent.model.AgentRunResult;
import com.gumtree.csagent.model.BotSession;
import com.gumtree.csagent.model.PhasePlan;
import com.gumtree.csagent.model.PhaseTransitionDecision;
import com.gumtree.csagent.model.TerminalOutcome;
import com.gumtree.csagent.service.guardrails.ScriptLibraryService;
import com.gumtree.csagent.service.knowledge.KnowledgeSearchService;
import com.gumtree.csagent.service.observability.EventEmitter;
import com.gumtree.csagent.service.tools.CreateCaseControlledTool;
import com.gumtree.csagent.service.tools.ToolDispatcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Sprint 8.1 §M3 — {@link PhaseEvaluator#interpretRunResult} must map
 * {@link TerminalOutcome#USE_CASE_IDENTIFIED} on a DISCOVER plan to a
 * non-escalating RESOLVE transition with reason {@code uc_identified}.
 * No synthetic {@code request_handover} is produced.
 */
@ExtendWith(MockitoExtension.class)
class Sprint81PhaseEvaluatorUseCaseIdentifiedTest {

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
                com.gumtree.csagent.service.runtime.skill.SkillTestFixtures.productionRegistry());
    }

    private PhasePlan discoverPlan() {
        return PhasePlan.builder()
                .phase("DISCOVER")
                .useCase(null)
                .objective("identify use case")
                .allowedTools(List.of("search_knowledge", "classify_use_case"))
                .maxToolSteps(2)
                .validTerminalOutcomes(Set.of(
                        TerminalOutcome.CLARIFICATION_NEEDED,
                        TerminalOutcome.FINAL_ANSWER,
                        TerminalOutcome.ESCALATE,
                        TerminalOutcome.USE_CASE_IDENTIFIED))
                .systemInstruction("test")
                .groundingInstruction("test")
                .escalationPolicy("test")
                .build();
    }

    @Test
    void useCaseIdentified_mapsToResolveWithUcIdentifiedReason() {
        BotSession session = new BotSession();
        session.setSessionId("sess");
        session.setCurrentPhase("DISCOVER");
        session.setActiveUseCase("UC-A");
        session.setTotalBotTurns(1);

        AgentRunResult result = AgentRunResult.useCaseIdentified(
                "UC-A", List.of(), List.of(), "{}", null);

        PhaseTransitionDecision decision = evaluator.interpretRunResult(
                discoverPlan(), result, session);

        assertNotNull(decision);
        assertEquals("RESOLVE", decision.nextPhase(),
                "USE_CASE_IDENTIFIED on DISCOVER must transition to RESOLVE");
        assertEquals("uc_identified", decision.transitionReason(),
                "Transition reason must be uc_identified");
        assertNull(decision.escalationReason(),
                "USE_CASE_IDENTIFIED is not an escalation; no reason should be set");
        assertNotNull(decision.responseText(),
                "A transitional placeholder response must be present");
    }

    @Test
    void useCaseIdentified_doesNotResetRuntimeErrorCount_below_threshold() {
        // Sanity: arbitrary side-effect check — runtime error counter still
        // resets on non-ERROR outcomes (as before). Pin nothing about the
        // synthetic request_handover behaviour; the §M3 path explicitly
        // produces no escalation reason.
        BotSession session = new BotSession();
        session.setSessionId("sess");
        session.setCurrentPhase("DISCOVER");
        session.setActiveUseCase("UC-A");
        session.setRuntimeErrorCount(2);

        AgentRunResult result = AgentRunResult.useCaseIdentified(
                "UC-A", List.of(), List.of(), "{}", null);

        evaluator.interpretRunResult(discoverPlan(), result, session);

        assertEquals(0, session.getRuntimeErrorCount(),
                "Non-ERROR outcomes (including USE_CASE_IDENTIFIED) reset the runtime error counter");
    }
}
