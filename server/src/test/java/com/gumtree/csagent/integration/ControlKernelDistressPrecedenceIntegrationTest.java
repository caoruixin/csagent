package com.gumtree.csagent.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gumtree.csagent.config.AgentRunLoopProperties;
import com.gumtree.csagent.model.BotSession;
import com.gumtree.csagent.model.DriftResult;
import com.gumtree.csagent.repository.BotEventRepository;
import com.gumtree.csagent.repository.BotTurnRepository;
import com.gumtree.csagent.service.observability.EventEmitter;
import com.gumtree.csagent.service.runtime.AgentRunLoop;
import com.gumtree.csagent.service.runtime.BudgetChecker;
import com.gumtree.csagent.service.runtime.ContextProjectionBuilder;
import com.gumtree.csagent.service.runtime.ControlKernel;
import com.gumtree.csagent.service.runtime.ControlPolicyService;
import com.gumtree.csagent.service.runtime.DriftDetector;
import com.gumtree.csagent.service.runtime.EscalationReasonResolver;
import com.gumtree.csagent.service.runtime.PhaseEvaluator;
import com.gumtree.csagent.service.tools.CreateCaseControlledTool;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Sprint 2026-05-04 §B1: distress / frustration detector. The
 * {@link ControlKernel} stamps {@code user_distress} via the
 * resolver BEFORE the budget check fires, so the resolver's
 * precedence table guarantees:
 *
 * <ul>
 *   <li>{@code user_distress} (priority 2) beats
 *       {@code faq_miss_threshold_exceeded} (priority 41).</li>
 *   <li>{@code user_distress} beats {@code turn_budget_exhausted}
 *       (priority 42) and {@code clarification_budget_exhausted}
 *       (priority 40).</li>
 *   <li>An explicit "speak to a human" / callback request still
 *       outranks distress because {@code user_requested} is
 *       priority 1 (cs_interactive_029 contract).</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ControlKernelDistressPrecedenceIntegrationTest {

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

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final EscalationReasonResolver resolver = new EscalationReasonResolver();

    private ControlKernel controlKernel;

    @BeforeEach
    void setUp() {
        AgentRunLoopProperties props = new AgentRunLoopProperties();
        props.setEnabledPhases(List.of()); // legacy path only
        controlKernel = new ControlKernel(
                turnRepository, eventRepository, budgetChecker, driftDetector,
                phaseEvaluator, controlPolicy, objectMapper, createCaseTool,
                eventEmitter, contextProjectionBuilder, props, agentRunLoop, resolver);
    }

    private BotSession session() {
        BotSession s = new BotSession();
        s.setSessionId("sess-distress-1");
        s.setCurrentPhase("RESOLVE");
        s.setActiveUseCase("UC-C");
        s.setTotalBotTurns(0);
        return s;
    }

    @Test
    void distressBeatsFaqMissThresholdExceeded() {
        // Arrange — distress detector fires on the user message, then
        // the FAQ-miss budget exhausts on the same turn. The resolver
        // must keep user_distress; the budget reason must lose.
        BotSession s = session();
        when(driftDetector.detect(any(), anyString())).thenReturn(
                DriftResult.builder().type(DriftResult.DriftType.NONE).build());
        when(turnRepository.findBySessionIdOrderByTurnIndex(anyString()))
                .thenReturn(List.of());
        when(budgetChecker.checkBudgets(any())).thenReturn(Optional.of("max-faq-miss"));
        when(controlPolicy.isValidTransition("RESOLVE", "ESCALATE")).thenReturn(true);

        // Act
        controlKernel.processMessage(s,
                "this is ridiculous, you are not helping at all");

        // Assert
        assertEquals("user_distress", s.getEscalationReason(),
                "B1 contract: user_distress must persist through the "
                        + "max-faq-miss budget close-out.");
    }

    @Test
    void distressBeatsTurnBudgetExhausted() {
        BotSession s = session();
        when(driftDetector.detect(any(), anyString())).thenReturn(
                DriftResult.builder().type(DriftResult.DriftType.NONE).build());
        when(turnRepository.findBySessionIdOrderByTurnIndex(anyString()))
                .thenReturn(List.of());
        when(budgetChecker.checkBudgets(any())).thenReturn(Optional.of("max-total-bot-turns"));
        when(controlPolicy.isValidTransition("RESOLVE", "ESCALATE")).thenReturn(true);

        controlKernel.processMessage(s,
                "I HAVE ALREADY FOLLOWED YOUR SO CALLED PROCESS");

        assertEquals("user_distress", s.getEscalationReason(),
                "B1 contract: user_distress must persist through the "
                        + "turn_budget_exhausted close-out.");
    }

    @Test
    void distressBeatsClarificationBudgetExhausted() {
        BotSession s = session();
        when(driftDetector.detect(any(), anyString())).thenReturn(
                DriftResult.builder().type(DriftResult.DriftType.NONE).build());
        when(turnRepository.findBySessionIdOrderByTurnIndex(anyString()))
                .thenReturn(List.of());
        when(budgetChecker.checkBudgets(any())).thenReturn(Optional.of("max-clarification-rounds"));
        when(controlPolicy.isValidTransition("RESOLVE", "ESCALATE")).thenReturn(true);

        controlKernel.processMessage(s,
                "How long do I have to wait? this is a joke");

        assertEquals("user_distress", s.getEscalationReason());
    }

    @Test
    void userRequestedStillBeatsDistress() {
        // cs_interactive_029 contract — when both signals fire on the
        // same user message, an explicit human / callback request
        // must win because user_requested has priority 1.
        BotSession s = session();
        when(turnRepository.findBySessionIdOrderByTurnIndex(anyString()))
                .thenReturn(List.of());
        when(controlPolicy.isValidTransition("RESOLVE", "ESCALATE")).thenReturn(true);

        // Distress + explicit callback in the same message.
        controlKernel.processMessage(s,
                "this is ridiculous, please call me back NOW");

        assertEquals("user_requested", s.getEscalationReason(),
                "B1/cs_029 contract: an explicit callback request "
                        + "outranks the distress stamp on the same turn.");
    }

    @Test
    void noDistress_normalQuestion_keepsReasonNull() {
        BotSession s = session();
        when(driftDetector.detect(any(), anyString())).thenReturn(
                DriftResult.builder().type(DriftResult.DriftType.NONE).build());
        when(turnRepository.findBySessionIdOrderByTurnIndex(anyString()))
                .thenReturn(List.of());
        when(budgetChecker.checkBudgets(any())).thenReturn(Optional.empty());

        // No budget tripping; calm question — distress must NOT fire.
        // Mock phase evaluator to a no-op response so the kernel just
        // records and returns.
        when(phaseEvaluator.evaluate(any(), anyString(), any())).thenReturn(
                new PhaseEvaluator.PhaseResult(
                        null,   // nextPhase
                        null,   // action
                        null,   // llmResponse
                        null,   // knowledgeHits
                        null,   // transitionReason
                        false,  // shouldClose
                        false,  // shouldEscalate
                        null,   // escalationReason
                        "ok"    // responseText
                ));

        controlKernel.processMessage(s, "thanks, that worked");

        assertEquals(null, s.getEscalationReason(),
                "Calm message must not stamp a distress reason.");
    }
}
