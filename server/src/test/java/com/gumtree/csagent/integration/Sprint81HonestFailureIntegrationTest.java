package com.gumtree.csagent.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gumtree.csagent.config.AgentRunLoopProperties;
import com.gumtree.csagent.model.AgentRunResult;
import com.gumtree.csagent.model.BotSession;
import com.gumtree.csagent.model.DriftResult;
import com.gumtree.csagent.model.PhasePlan;
import com.gumtree.csagent.model.PhaseTransitionDecision;
import com.gumtree.csagent.model.TerminalOutcome;
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

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Sprint 8.1 §M2 — pin the honest-failure contract on the localhost
 * ad-visibility shape that exposed the original bug.
 *
 * <p>Form: {@code topic_subject="Ad Support"}, {@code ad_id="123"},
 * {@code description="where is my ad?"}. Current message:
 * {@code "hi, why I can't see my ad"}.
 *
 * <p>Pre-Sprint-8.1 behaviour: simulated LLM timeout produced a
 * {@code DISCOVER -> ESCALATE} transition with {@code active_use_case=UC-A}
 * stamped by K0 (the regex saw "ad"). Post-Sprint-8.1: the deadline
 * propagates as {@code TerminalOutcome.DEADLINE_EXCEEDED}, the kernel
 * returns a non-escalating slow-message {@code KernelResult}, and the
 * UC slot stays {@code null} so the trace surfaces the real failure.
 */
@ExtendWith(MockitoExtension.class)
class Sprint81HonestFailureIntegrationTest {

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
    private final EscalationReasonResolver escalationResolver = new EscalationReasonResolver();

    private ControlKernel controlKernel;

    @BeforeEach
    void setUp() {
        AgentRunLoopProperties props = new AgentRunLoopProperties();
        props.setEnabledPhases(List.of(
                "RESOLVE_FAQ", "RESOLVE_INTAKE", "DISCOVER", "CONFIRM", "CLOSE", "ESCALATE"));
        controlKernel = new ControlKernel(
                turnRepository, eventRepository, budgetChecker, driftDetector,
                phaseEvaluator, controlPolicy, objectMapper, createCaseTool,
                eventEmitter, contextProjectionBuilder, props, agentRunLoop,
                escalationResolver);
    }

    private BotSession adVisibilityFormSession() {
        BotSession s = new BotSession();
        s.setSessionId("sess-localhost-ad");
        s.setCurrentPhase("DISCOVER");
        s.setActiveUseCase(null);
        s.setTotalBotTurns(0);
        s.setFormTopicSubject("Ad Support");
        s.setFormContext("{\"first_name\":\"joh\",\"email\":\"joh@e.com\","
                + "\"topic_subject\":\"Ad Support\",\"ad_id\":\"123\","
                + "\"description\":\"where is my ad?\"}");
        return s;
    }

    private void stubKernelChain(AgentRunResult runResult, PhaseTransitionDecision decision) {
        when(budgetChecker.checkBudgets(any())).thenReturn(Optional.empty());
        when(driftDetector.detect(any(), anyString())).thenReturn(
                DriftResult.builder().type(DriftResult.DriftType.NONE).build());
        when(turnRepository.findBySessionIdOrderByTurnIndex(anyString()))
                .thenReturn(List.of());
        PhasePlan plan = PhasePlan.builder()
                .phase("DISCOVER").useCase(null)
                .objective("Identify the user's use case")
                .allowedTools(List.of("search_knowledge", "classify_use_case"))
                .maxToolSteps(2)
                .systemInstruction("M2 stub plan")
                .validTerminalOutcomes(Set.of(
                        TerminalOutcome.ESCALATE,
                        TerminalOutcome.FINAL_ANSWER,
                        TerminalOutcome.CLARIFICATION_NEEDED,
                        TerminalOutcome.DEADLINE_EXCEEDED,
                        TerminalOutcome.LLM_UNAVAILABLE,
                        TerminalOutcome.MAX_STEPS,
                        TerminalOutcome.ERROR))
                .build();
        when(phaseEvaluator.plan(any(), anyString(), any())).thenReturn(plan);
        when(agentRunLoop.run(any(), any(), anyString(), any())).thenReturn(runResult);
        when(phaseEvaluator.interpretRunResult(any(), eq(runResult), any()))
                .thenReturn(decision);
    }

    @Test
    void localhostAdVisibility_deadlineExceeded_doesNotStampUcA_returnsSlowMessage() {
        // ── Arrange: simulated LLM deadline before any real LLM
        // completion. The AgentRunLoop preserves whatever events ran
        // earlier (none here) and returns DEADLINE_EXCEEDED.
        AgentRunResult deadlineResult = AgentRunResult.deadlineExceeded(
                "llm_deadline_exceeded: budget exhausted",
                List.of(),
                List.of(),
                "{\"phase\":\"DISCOVER\"}");
        // PhaseEvaluator maps DEADLINE_EXCEEDED to a STAY-IN-DISCOVER
        // decision with the slow-message text. Mirrored here so the
        // mock chain matches the production interpretRunResult contract.
        PhaseTransitionDecision deadlineDecision = new PhaseTransitionDecision(
                "DISCOVER",
                "Sorry, I'm a bit slow right now. Please try sending that again in a moment.",
                null,
                "agent_deadline_exceeded");
        BotSession session = adVisibilityFormSession();
        stubKernelChain(deadlineResult, deadlineDecision);

        // ── Act ──────────────────────────────────────────────────
        ControlKernel.KernelResult kernel =
                controlKernel.processMessage(session, "hi, why I can't see my ad");

        // ── Assert: M2 contract holds ────────────────────────────
        assertNull(session.getActiveUseCase(),
                "Sprint 8.1 §M2: deadline exhaustion must NOT stamp a "
                        + "synthetic UC-A from the regex. The trace must "
                        + "surface the real timeout, not a fake business "
                        + "escalation.");
        assertNull(session.getEscalationReason(),
                "No business escalation reason should be set on a deadline path.");
        assertEquals("DISCOVER", session.getCurrentPhase(),
                "Phase must stay in DISCOVER (no escalation).");
        assertFalse(kernel.shouldEndChat(),
                "Slow-message turn must keep the chat box open so the "
                        + "user can retry on their next message.");
        assertEquals(
                "Sorry, I'm a bit slow right now. Please try sending that again in a moment.",
                kernel.responseText());
        verify(eventEmitter, never()).emitEscalationRequested(anyString(), org.mockito.ArgumentMatchers.anyInt(), anyString());
    }

    @Test
    void localhostAdVisibility_llmUnavailable_doesNotStampUc_returnsSlowMessage() {
        AgentRunResult unavailableResult = AgentRunResult.llmUnavailable(
                "llm_unavailable: failure_class=llm_server_error",
                List.of(),
                List.of(),
                "{\"phase\":\"DISCOVER\"}");
        PhaseTransitionDecision unavailableDecision = new PhaseTransitionDecision(
                "DISCOVER",
                "Sorry, I'm having trouble reaching the assistant right now. "
                        + "Please try again in a moment.",
                null,
                "agent_llm_unavailable");
        BotSession session = adVisibilityFormSession();
        stubKernelChain(unavailableResult, unavailableDecision);

        ControlKernel.KernelResult kernel =
                controlKernel.processMessage(session, "hi, why I can't see my ad");

        assertNull(session.getActiveUseCase(),
                "Sprint 8.1 §M2: llm-unavailable must not stamp a "
                        + "synthetic UC.");
        assertEquals("DISCOVER", session.getCurrentPhase());
        assertFalse(kernel.shouldEndChat());
    }
}
