package com.gumtree.csagent.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gumtree.csagent.config.AgentRunLoopProperties;
import com.gumtree.csagent.model.AgentRunResult;
import com.gumtree.csagent.model.BotSession;
import com.gumtree.csagent.model.DriftResult;
import com.gumtree.csagent.model.PhasePlan;
import com.gumtree.csagent.model.PhaseTransitionDecision;
import com.gumtree.csagent.model.ResolveDisposition;
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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Sprint 084 / S-Auto-29 — CLOSE-arm containment grounding gate (pure-infra
 * runtime trace-contract fix; §3.2 layer {@code infra}).
 *
 * <p>Pins the binding contract for the {@link ControlKernel} agent-loop CLOSE
 * arm after the {@code phase=CLOSE} resolved-stamp was routed through
 * {@link ControlKernel#isResolvedSuccessTerminal}. Before this sprint the arm
 * UNCONDITIONALLY defaulted {@code containment_outcome="resolved"} on ANY
 * LLM-emitted {@code next_phase=CLOSE} where containment was null — crediting a
 * resolved success even when the loop delivered no grounded answer (a
 * DISCOVER-stalled clarifier close, a hallucinated "user confirmed" close, or a
 * simulator drop-out close). The gate now stamps {@code "resolved"} ONLY when
 * containment is null AND {@code isResolvedSuccessTerminal} holds (FINAL_ANSWER +
 * READY_TO_CONFIRM / ANSWERED_SUBTASK + non-empty {@code articlesShown}).
 *
 * <p>These tests drive the real {@code ControlKernel.processMessage} agent-loop
 * branch with the {@link PhaseEvaluator} and {@link AgentRunLoop} mocked so the
 * CLOSE transition, the terminal {@link AgentRunResult}, and the per-turn
 * grounding state are controlled precisely. The CLOSE arm only inspects
 * {@code phaseAfter == "CLOSE"} plus the session's containment / disposition /
 * grounding slots and the run result's terminal outcome, so this is the unit of
 * behaviour under test.
 *
 * <p>The two UNCONDITIONAL CLOSE side-effects — {@code handlingState="CLOSED"}
 * and {@code emitSessionClosed} — must still fire regardless of the containment
 * outcome (the session did close); only the {@code "resolved"} credit is gated.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AgentRunLoopCloseContainmentGroundingGateIntegrationTest {

    private static final String SESSION_ID = "sess-close-gate";
    private static final String[] GROUNDED = {"ka44J000000gKxqQAE", "ka4P200000005MHIAY"};

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

    private ControlKernel controlKernel;

    @BeforeEach
    void setUp() {
        AgentRunLoopProperties props = new AgentRunLoopProperties();
        props.setEnabledPhases(List.of(
                "RESOLVE_FAQ", "RESOLVE_INTAKE",
                "DISCOVER", "CONFIRM", "CLOSE", "ESCALATE"));

        controlKernel = new ControlKernel(
                turnRepository, eventRepository, budgetChecker, driftDetector,
                phaseEvaluator, controlPolicy, objectMapper, createCaseTool,
                eventEmitter, contextProjectionBuilder, props, agentRunLoop,
                new EscalationReasonResolver());

        // Common pipeline stubs: no budget escalation, no drift, no history,
        // benign projection. CLOSE is a legal transition (mocked policy).
        when(budgetChecker.checkBudgets(any())).thenReturn(Optional.empty());
        when(driftDetector.detect(any(), anyString())).thenReturn(
                DriftResult.builder().type(DriftResult.DriftType.NONE).build());
        when(turnRepository.findBySessionIdOrderByTurnIndex(anyString()))
                .thenReturn(List.of());
        when(controlPolicy.isValidTransition(anyString(), eq("CLOSE"))).thenReturn(true);
        when(contextProjectionBuilder.build(any(), any(), any(), anyString(), any(), any()))
                .thenReturn("{}");
    }

    private AgentRunResult finalAnswer() {
        return AgentRunResult.finalAnswer(
                "Thanks, glad I could help.", List.of(), List.of());
    }

    /**
     * Drive a single turn through the agent-loop branch into an LLM-emitted
     * CLOSE transition with the given pre-turn session state and terminal
     * run result. Returns the mutated session for assertions.
     */
    private BotSession driveToClose(String phaseBefore, String activeUseCase,
                                    AgentRunResult runResult,
                                    String priorContainment,
                                    ResolveDisposition disposition,
                                    String[] articlesShown,
                                    String userMessage) {
        BotSession s = new BotSession();
        s.setSessionId(SESSION_ID);
        s.setCurrentPhase(phaseBefore);
        s.setActiveUseCase(activeUseCase);
        s.setTotalBotTurns(1);
        if (priorContainment != null) {
            s.setContainmentOutcome(priorContainment);
        }
        if (disposition != null) {
            s.setResolveDisposition(disposition.name());
        }
        if (articlesShown != null) {
            s.setArticlesShown(articlesShown);
        }

        when(phaseEvaluator.plan(any(), anyString(), any()))
                .thenReturn(PhasePlan.builder().phase(phaseBefore).useCase(activeUseCase).build());
        when(agentRunLoop.run(any(), any(), anyString(), any())).thenReturn(runResult);
        when(phaseEvaluator.interpretRunResult(any(), any(), any()))
                .thenReturn(new PhaseTransitionDecision(
                        "CLOSE", "Thanks, glad I could help.", null, "user_satisfied"));

        controlKernel.processMessage(s, userMessage);
        return s;
    }

    // ── Test 1: DISCOVER-stalled close, no grounding → NOT resolved ──────────

    @Test
    void discoverStalledClose_noGrounding_noUc_leavesContainmentNull() {
        // DISCOVER → DISCOVER → DISCOVER → CLOSE: the LLM emits next_phase=CLOSE
        // after a clarifier stall — 0 tool calls, no UC committed, no grounded
        // answer. The pre-gate default would have credited this as "resolved";
        // the grounding gate must leave containment null (FINAL_ANSWER text but
        // no disposition / no articlesShown -> isResolvedSuccessTerminal false).
        BotSession s = driveToClose(
                "DISCOVER", null, finalAnswer(),
                null, null, null,
                "ok, never mind then");

        assertEquals("CLOSE", s.getCurrentPhase());
        assertNull(s.getContainmentOutcome(),
                "an ungrounded LLM-emitted CLOSE must NOT be credited as resolved");
        assertEquals("CLOSED", s.getHandlingState(),
                "handlingState=CLOSED is an unconditional CLOSE side-effect");
        verify(eventEmitter, times(1))
                .emitSessionClosed(eq(SESSION_ID), nullable(String.class));
    }

    // ── Test 2: grounded goal_achieved one-shot → resolved (anti-误杀) ────────

    @Test
    void groundedGoalAchievedOneShot_close_stampsResolved() {
        // The legitimate one-shot: a substantive grounded FINAL_ANSWER
        // (ANSWERED_SUBTASK + non-empty articlesShown) that the LLM closes.
        // This case was resolved via the CLOSE arm before the change and MUST
        // stay resolved after gating — the gate's anti-误杀 invariant.
        BotSession s = driveToClose(
                "CONFIRM", "UC-A", finalAnswer(),
                null, ResolveDisposition.ANSWERED_SUBTASK, GROUNDED,
                "thanks, that helped");

        assertEquals("CLOSE", s.getCurrentPhase());
        assertEquals("resolved", s.getContainmentOutcome(),
                "a grounded goal_achieved one-shot close stays resolved");
        assertEquals("CLOSED", s.getHandlingState());
        verify(eventEmitter, times(1))
                .emitSessionClosed(eq(SESSION_ID), eq("resolved"));
    }

    // ── Test 3: FINAL_ANSWER but empty grounding → NOT resolved ──────────────

    @Test
    void finalAnswer_emptyArticlesShown_close_leavesContainmentNull() {
        // FINAL_ANSWER with a resolve disposition but EMPTY articlesShown is an
        // ungrounded answer: isResolvedSuccessTerminal requires non-empty
        // grounding, so the gate must not stamp resolved.
        BotSession s = driveToClose(
                "CONFIRM", "UC-A", finalAnswer(),
                null, ResolveDisposition.ANSWERED_SUBTASK, new String[0],
                "ok thanks");

        assertEquals("CLOSE", s.getCurrentPhase());
        assertNull(s.getContainmentOutcome(),
                "an ungrounded FINAL_ANSWER (empty articlesShown) is not a resolved terminal");
        assertEquals("CLOSED", s.getHandlingState());
        verify(eventEmitter, times(1))
                .emitSessionClosed(eq(SESSION_ID), nullable(String.class));
    }

    // ── Test 4: escalation stamped earlier → CLOSE preserves "escalated" ─────

    @Test
    void priorEscalated_close_isNotOverwritten() {
        // Escalation stamped "escalated" on an earlier turn (non-null
        // containment). A later CLOSE transition must not overwrite it — the
        // null-guard short-circuits before the grounding gate even runs.
        // (A neutral message is used so THIS turn does not itself escalate;
        // the escalation is the earlier-turn pre-set containment.)
        BotSession s = driveToClose(
                "CONFIRM", "UC-A", finalAnswer(),
                "escalated", null, null,
                "ok, thanks for letting me know");

        assertEquals("CLOSE", s.getCurrentPhase());
        assertEquals("escalated", s.getContainmentOutcome(),
                "a pre-existing 'escalated' containment must never be overwritten on CLOSE");
        verify(eventEmitter, times(1))
                .emitSessionClosed(eq(SESSION_ID), eq("escalated"));
    }

    // ── Test 5: prior "resolved" → CLOSE does not regress it ─────────────────

    @Test
    void priorResolved_close_isNotRegressed() {
        // An earlier turn legitimately stamped "resolved". This turn's CLOSE
        // transition must preserve it (non-null guard holds) — the gate never
        // downgrades a prior resolved on a plain CLOSE.
        BotSession s = driveToClose(
                "CONFIRM", "UC-A", finalAnswer(),
                "resolved", ResolveDisposition.READY_TO_CONFIRM, GROUNDED,
                "great, thanks");

        assertEquals("CLOSE", s.getCurrentPhase());
        assertEquals("resolved", s.getContainmentOutcome(),
                "a prior 'resolved' stamp is preserved on a later CLOSE");
        assertEquals("CLOSED", s.getHandlingState());
        verify(eventEmitter, times(1))
                .emitSessionClosed(eq(SESSION_ID), eq("resolved"));
    }
}
