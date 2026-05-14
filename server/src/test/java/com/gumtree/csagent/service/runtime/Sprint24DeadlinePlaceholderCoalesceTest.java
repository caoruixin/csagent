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
import java.util.Locale;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Sprint 24 Track A — cross-turn DEADLINE_EXCEEDED placeholder coalesce.
 *
 * <p>Asserts the behaviour shape (event-shape trigger; NOT a CaseSpec list)
 * required by {@code docs/sprint_objective.md} §3 / §8.1:
 *
 * <ul>
 *   <li>1st {@code DEADLINE_EXCEEDED} outcome → existing placeholder text.</li>
 *   <li>2nd consecutive {@code DEADLINE_EXCEEDED} outcome → user-facing
 *       message is NOT the placeholder (inequality assert) AND contains a
 *       next-step intent token (handover / retry / alternative / specialist
 *       / connect / try again).</li>
 *   <li>Reset: between two {@code DEADLINE_EXCEEDED} outcomes, a non-deadline
 *       outcome ({@link TerminalOutcome#CLARIFICATION_NEEDED}) resets the
 *       counter and a subsequent deadline emits the placeholder again.</li>
 * </ul>
 *
 * <p>The honest-next-step content assertion is intentionally a property
 * check, not a literal string match, per the §7 anti-hardcode gate
 * (no encoding of visible-eval phrases or trace-specific wording in a
 * runtime regression test).
 */
@ExtendWith(MockitoExtension.class)
class Sprint24DeadlinePlaceholderCoalesceTest {

    private static final String PLACEHOLDER =
            "Sorry, I'm a bit slow right now. Please try sending that again in a moment.";

    private static final List<String> NEXT_STEP_INTENT_TOKENS = List.of(
            "handover",
            "specialist",
            "connect",
            "retry",
            "try again",
            "alternative",
            "alternatively");

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
                new ObjectMapper(), createCaseTool, eventEmitter, toolDispatcher);
    }

    private PhasePlan resolveFaqPlan() {
        return PhasePlan.builder()
                .phase("RESOLVE")
                .useCase("UC-A")
                .objective("test")
                .allowedTools(List.of())
                .requiredContextKeys(Set.of())
                .maxToolSteps(2)
                .allowInterimMessage(false)
                .validTerminalOutcomes(Set.of(
                        TerminalOutcome.FINAL_ANSWER,
                        TerminalOutcome.CLARIFICATION_NEEDED,
                        TerminalOutcome.DEADLINE_EXCEEDED,
                        TerminalOutcome.ESCALATE))
                .systemInstruction("test")
                .groundingInstruction("test")
                .escalationPolicy("test")
                .build();
    }

    private BotSession newSession() {
        BotSession s = new BotSession();
        s.setSessionId("sess-sprint24");
        s.setCurrentPhase("RESOLVE");
        s.setActiveUseCase("UC-A");
        s.setRuntimeErrorCount(0);
        s.setConsecutiveDeadlineCount(0);
        return s;
    }

    private AgentRunResult deadlineOutcome() {
        return AgentRunResult.deadlineExceeded(null, List.of(), List.of(), null);
    }

    private AgentRunResult clarificationOutcome() {
        return AgentRunResult.clarification(
                "Could you tell me a bit more?", List.of(), List.of(), null, null);
    }

    private boolean containsNextStepIntent(String text) {
        if (text == null) {
            return false;
        }
        String lower = text.toLowerCase(Locale.ENGLISH);
        return NEXT_STEP_INTENT_TOKENS.stream().anyMatch(lower::contains);
    }

    /**
     * 1st consecutive DEADLINE_EXCEEDED outcome → existing placeholder text;
     * transition tag is {@code agent_deadline_exceeded}; session stays in
     * the current phase (no escalation, no auto-handover); the per-session
     * counter advances to 1.
     */
    @Test
    void firstDeadlineExceeded_emitsExistingPlaceholder() {
        BotSession session = newSession();
        PhasePlan plan = resolveFaqPlan();

        PhaseTransitionDecision decision =
                evaluator.interpretRunResult(plan, deadlineOutcome(), session);

        assertEquals(PLACEHOLDER, decision.responseText(),
                "First consecutive DEADLINE_EXCEEDED must emit the existing placeholder text byte-for-byte.");
        assertEquals("RESOLVE", decision.nextPhase(),
                "DEADLINE_EXCEEDED must stay in the current phase (no escalation).");
        assertEquals("agent_deadline_exceeded", decision.transitionReason(),
                "Transition tag must remain agent_deadline_exceeded for both branches.");
        assertEquals(1, session.getConsecutiveDeadlineCount(),
                "Counter must advance to 1 on the first deadline outcome.");
    }

    /**
     * 2nd consecutive DEADLINE_EXCEEDED outcome → user-facing message is
     * NOT the placeholder AND carries a next-step intent. Transition tag
     * remains {@code agent_deadline_exceeded}; bot does NOT auto-call
     * request_handover (no escalation reason on the decision).
     */
    @Test
    void secondConsecutiveDeadline_emitsDistinctHonestNextStepMessage() {
        BotSession session = newSession();
        PhasePlan plan = resolveFaqPlan();

        evaluator.interpretRunResult(plan, deadlineOutcome(), session);
        PhaseTransitionDecision second =
                evaluator.interpretRunResult(plan, deadlineOutcome(), session);

        assertNotEquals(PLACEHOLDER, second.responseText(),
                "Second consecutive DEADLINE_EXCEEDED must NOT repeat the existing placeholder text.");
        assertFalse(second.responseText() == null || second.responseText().isBlank(),
                "Second consecutive DEADLINE_EXCEEDED must emit a non-empty honest next-step message.");
        assertTrue(containsNextStepIntent(second.responseText()),
                "Second consecutive DEADLINE_EXCEEDED message must offer an actionable next step "
                        + "(handover / retry / alternative / equivalent). Got: "
                        + second.responseText());
        assertEquals("RESOLVE", second.nextPhase(),
                "Distinct-message branch must stay in the current phase (no escalation, no auto-handover).");
        assertEquals("agent_deadline_exceeded", second.transitionReason(),
                "Transition tag must remain agent_deadline_exceeded on the distinct-message branch.");
        assertEquals(null, second.escalationReason(),
                "Distinct-message branch must NOT auto-call request_handover (no escalation reason).");
        assertEquals(2, session.getConsecutiveDeadlineCount(),
                "Counter must advance to 2 on the second consecutive deadline.");
    }

    /**
     * Reset assertion: a non-deadline outcome between two
     * DEADLINE_EXCEEDED outcomes re-zeros the counter; the subsequent
     * deadline outcome emits the existing placeholder again, not the
     * honest next-step message.
     */
    @Test
    void nonDeadlineOutcomeBetweenDeadlines_resetsCounterAndPlaceholderFiresAgain() {
        BotSession session = newSession();
        PhasePlan plan = resolveFaqPlan();

        // 1st deadline → placeholder + counter=1.
        PhaseTransitionDecision first =
                evaluator.interpretRunResult(plan, deadlineOutcome(), session);
        assertEquals(PLACEHOLDER, first.responseText());
        assertEquals(1, session.getConsecutiveDeadlineCount());

        // Non-deadline outcome resets the counter to 0.
        PhaseTransitionDecision clarification =
                evaluator.interpretRunResult(plan, clarificationOutcome(), session);
        assertEquals("clarification_asked", clarification.transitionReason(),
                "CLARIFICATION_NEEDED branch must fire (reset hook precondition).");
        assertEquals(0, session.getConsecutiveDeadlineCount(),
                "Non-deadline outcome must reset the consecutive-deadline counter to 0.");

        // Subsequent deadline → existing placeholder again, NOT the
        // honest next-step message.
        PhaseTransitionDecision third =
                evaluator.interpretRunResult(plan, deadlineOutcome(), session);
        assertEquals(PLACEHOLDER, third.responseText(),
                "After a non-deadline outcome reset, the next DEADLINE_EXCEEDED must "
                        + "emit the existing placeholder again (not the honest next-step message).");
        assertEquals(1, session.getConsecutiveDeadlineCount(),
                "Counter must restart from 1 after the reset.");
    }
}
