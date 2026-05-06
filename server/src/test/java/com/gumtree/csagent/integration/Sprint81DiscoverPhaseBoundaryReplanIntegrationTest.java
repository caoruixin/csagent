package com.gumtree.csagent.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gumtree.csagent.config.AgentRunLoopProperties;
import com.gumtree.csagent.model.AgentRunResult;
import com.gumtree.csagent.model.BotSession;
import com.gumtree.csagent.model.DriftResult;
import com.gumtree.csagent.model.LlmCallEvent;
import com.gumtree.csagent.model.PhasePlan;
import com.gumtree.csagent.model.PhaseTransitionDecision;
import com.gumtree.csagent.model.TerminalOutcome;
import com.gumtree.csagent.model.ToolEvent;
import com.gumtree.csagent.repository.BotEventRepository;
import com.gumtree.csagent.repository.BotTurnRepository;
import com.gumtree.csagent.service.llm.LlmCallContext;
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Sprint 8.1 §M3 — DISCOVER successful classification phase-boundary
 * replan, end-to-end through {@link ControlKernel}.
 *
 * <p>Pinpoints the trace shape:
 * <ol>
 *   <li>DISCOVER plan, allowedTools = {{search_knowledge, classify_use_case}}.</li>
 *   <li>LLM calls search_knowledge → hits returned.</li>
 *   <li>LLM calls classify_use_case → activeUseCase=UC-A committed.</li>
 *   <li>AgentRunLoop returns USE_CASE_IDENTIFIED (no MAX_STEPS escalation).</li>
 *   <li>ControlKernel applies the deterministic DISCOVER → RESOLVE
 *       transition and runs the AgentRunLoop a second time with a fresh
 *       RESOLVE plan, sharing the same wall-clock / attempt budget.</li>
 *   <li>The second run produces the FAQ answer; the persisted bot turn
 *       carries the combined tool events from BOTH phases.</li>
 * </ol>
 *
 * <p>Negative branch:
 * <ul>
 *   <li>When the remaining budget cannot fit a RESOLVE attempt, the kernel
 *       skips the replan, leaves the session in RESOLVE, and returns a
 *       non-terminal "looking into it" response — the next user turn
 *       re-runs RESOLVE fresh.</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class Sprint81DiscoverPhaseBoundaryReplanIntegrationTest {

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

    @AfterEach
    void clearDeadline() {
        LlmCallContext.clear();
    }

    private BotSession adSupportSession() {
        BotSession s = new BotSession();
        s.setSessionId("sess-m3-replan");
        s.setCurrentPhase("DISCOVER");
        s.setActiveUseCase(null);
        s.setTotalBotTurns(0);
        s.setFormTopicSubject("Ad Support");
        s.setFormContext("{\"first_name\":\"hh\",\"email\":\"hh@hh.com\","
                + "\"topic_subject\":\"Ad Support\",\"ad_id\":\"123\","
                + "\"description\":\"where is my ad?\"}");
        return s;
    }

    private PhasePlan discoverPlan() {
        return PhasePlan.builder()
                .phase("DISCOVER").useCase(null)
                .objective("Identify the use case")
                .allowedTools(List.of("search_knowledge", "classify_use_case"))
                .maxToolSteps(2)
                .systemInstruction("DISCOVER")
                .validTerminalOutcomes(Set.of(
                        TerminalOutcome.USE_CASE_IDENTIFIED,
                        TerminalOutcome.FINAL_ANSWER,
                        TerminalOutcome.CLARIFICATION_NEEDED,
                        TerminalOutcome.ESCALATE))
                .build();
    }

    private PhasePlan resolvePlan(String useCase) {
        return PhasePlan.builder()
                .phase("RESOLVE").useCase(useCase)
                .objective("Resolve the user's question for " + useCase)
                .allowedTools(List.of("search_knowledge", "resolve_article", "request_handover"))
                .maxToolSteps(3)
                .systemInstruction("RESOLVE")
                .validTerminalOutcomes(Set.of(TerminalOutcome.FINAL_ANSWER, TerminalOutcome.ESCALATE))
                .build();
    }

    @Test
    void discoverClassifySuccess_triggersSameTurnReplan_intoResolve() {
        // ── Arrange ──────────────────────────────────────────────
        BotSession session = adSupportSession();
        when(budgetChecker.checkBudgets(any())).thenReturn(Optional.empty());
        when(driftDetector.detect(any(), anyString())).thenReturn(
                DriftResult.builder().type(DriftResult.DriftType.NONE).build());
        when(turnRepository.findBySessionIdOrderByTurnIndex(anyString()))
                .thenReturn(List.of());

        // First plan: DISCOVER. Second plan (after the same-turn
        // transition flips currentPhase to RESOLVE): RESOLVE for UC-A.
        PhasePlan discover = discoverPlan();
        PhasePlan resolve = resolvePlan("UC-A");
        when(phaseEvaluator.plan(any(), anyString(), any()))
                .thenReturn(discover)
                .thenReturn(resolve);
        when(controlPolicy.isValidTransition(eq("DISCOVER"), eq("RESOLVE"))).thenReturn(true);
        when(controlPolicy.isValidTransition(eq("RESOLVE"), eq("CONFIRM"))).thenReturn(true);

        // First AgentRunLoop run: classify success → USE_CASE_IDENTIFIED.
        // Side-effect: classify_use_case sets session.activeUseCase=UC-A.
        AgentRunResult discoverResult = AgentRunResult.useCaseIdentified(
                "UC-A",
                List.of(new LlmCallEvent(0, 0, "deepseek", 100, 50, 1500L,
                        "{\"finish_reason\":\"stop\"}")),
                List.of(
                        new ToolEvent(1, 0, "search_knowledge", null, true,
                                java.util.Map.of("hits", List.of()), null, 100L),
                        new ToolEvent(2, 1, "classify_use_case", null, true,
                                java.util.Map.of("committed", true, "use_case_id", "UC-A"), null, 50L)),
                "{\"phase\":\"DISCOVER\"}",
                "{}");
        // Second AgentRunLoop run: RESOLVE produces a final answer.
        AgentRunResult resolveResult = AgentRunResult.finalAnswer(
                "Here is how ads become visible…",
                List.of(new LlmCallEvent(0, 0, "deepseek", 200, 80, 1700L,
                        "{\"finish_reason\":\"stop\"}")),
                List.of(new ToolEvent(1, 0, "resolve_article", null, true,
                        java.util.Map.of("source_id", "kb-1"), null, 80L)),
                "{\"phase\":\"RESOLVE\"}",
                "{\"user_message\":\"Here is how…\"}");

        when(agentRunLoop.run(eq(discover), any(), anyString(), any()))
                .thenAnswer(inv -> {
                    // Simulate classify_use_case side-effect.
                    BotSession sess = inv.getArgument(1);
                    sess.setActiveUseCase("UC-A");
                    sess.setIntentConfidence(new java.math.BigDecimal("0.92"));
                    return discoverResult;
                });
        when(agentRunLoop.run(eq(resolve), any(), anyString(), any()))
                .thenReturn(resolveResult);

        // PhaseEvaluator: only the second interpretRunResult (on the
        // merged result) is consumed by the kernel. We mirror the
        // production mapping for the merged FINAL_ANSWER → CONFIRM
        // (RESOLVE/FAQ) decision.
        PhaseTransitionDecision finalDecision = new PhaseTransitionDecision(
                "CONFIRM", "Here is how ads become visible…", null, "answer_provided");
        when(phaseEvaluator.interpretRunResult(eq(resolve), any(AgentRunResult.class), any()))
                .thenReturn(finalDecision);

        // ── Act ──────────────────────────────────────────────────
        ControlKernel.KernelResult kernel =
                controlKernel.processMessage(session, "where is my advert? I can't see it");

        // ── Assert ───────────────────────────────────────────────
        assertEquals("UC-A", session.getActiveUseCase(),
                "DISCOVER classify_use_case must commit UC-A on the session");
        assertNull(session.getEscalationReason(),
                "Successful classification must not trigger any escalation");
        assertEquals("Here is how ads become visible…", kernel.responseText(),
                "User-facing reply comes from the RESOLVE replan, not the DISCOVER transitional placeholder");
        assertFalse(kernel.shouldEndChat(),
                "After RESOLVE FINAL_ANSWER → CONFIRM, the chat should stay open until CLOSE");
        // Both DISCOVER and RESOLVE plans were fetched.
        verify(phaseEvaluator, times(2)).plan(any(), anyString(), any());
        // AgentRunLoop ran twice — once on DISCOVER, once on RESOLVE.
        verify(agentRunLoop, times(1)).run(eq(discover), any(), anyString(), any());
        verify(agentRunLoop, times(1)).run(eq(resolve), any(), anyString(), any());
        // The persisted bot turn captures both phases' tool calls. The
        // ControlKernel.recordRunResult consumes the merged AgentRunResult,
        // whose toolEvents = DISCOVER's 2 tool events + RESOLVE's 1 = 3.
        org.mockito.ArgumentCaptor<com.gumtree.csagent.model.BotTurn> turnCaptor =
                org.mockito.ArgumentCaptor.forClass(com.gumtree.csagent.model.BotTurn.class);
        verify(turnRepository).save(turnCaptor.capture());
        com.gumtree.csagent.model.BotTurn savedTurn = turnCaptor.getValue();
        assertNotNull(savedTurn.getToolCalls(),
                "Persisted bot turn must include the combined tool_calls trace");
        assertTrue(savedTurn.getToolCalls().contains("search_knowledge"),
                "Persisted tool_calls must include the DISCOVER search_knowledge event");
        assertTrue(savedTurn.getToolCalls().contains("classify_use_case"),
                "Persisted tool_calls must include the DISCOVER classify_use_case event");
        assertTrue(savedTurn.getToolCalls().contains("resolve_article"),
                "Persisted tool_calls must include the RESOLVE resolve_article event");
        // Escalation never fired.
        verify(eventEmitter, never())
                .emitEscalationRequested(anyString(), org.mockito.ArgumentMatchers.anyInt(), anyString());
    }

    @Test
    void discoverClassifySuccess_deadlineActive_attemptBudgetDrained_stillReplans() {
        // Sprint 8.1 closure follow-up (2026-05-07) — P1-2 production
        // shape. With a 30 s user-facing deadline in effect, two successful
        // DISCOVER LLM calls (search_knowledge + classify_use_case) used to
        // drain the shared HTTP-attempt budget to 0 — leaving
        // canAttempt()==false. The kernel then refused to run the same-turn
        // RESOLVE replan even though wall-clock budget remained, and the
        // user got a transitional "looking into it" placeholder instead of
        // the real FAQ answer. Fix: per-invocation reset at
        // {@link FallbackLlmClient#chat} entry + drop the kernel's
        // canAttempt() precondition. Now the wall-clock alone gates the
        // same-turn replan.
        LlmCallContext.setDeadline(System.currentTimeMillis() + 30_000L);

        BotSession session = adSupportSession();
        when(budgetChecker.checkBudgets(any())).thenReturn(Optional.empty());
        when(driftDetector.detect(any(), anyString())).thenReturn(
                DriftResult.builder().type(DriftResult.DriftType.NONE).build());
        when(turnRepository.findBySessionIdOrderByTurnIndex(anyString()))
                .thenReturn(List.of());

        PhasePlan discover = discoverPlan();
        PhasePlan resolve = resolvePlan("UC-A");
        when(phaseEvaluator.plan(any(), anyString(), any()))
                .thenReturn(discover)
                .thenReturn(resolve);
        when(controlPolicy.isValidTransition(eq("DISCOVER"), eq("RESOLVE"))).thenReturn(true);
        when(controlPolicy.isValidTransition(eq("RESOLVE"), eq("CONFIRM"))).thenReturn(true);

        AgentRunResult discoverResult = AgentRunResult.useCaseIdentified(
                "UC-A",
                List.of(
                        // Two successful DISCOVER LLM calls: search_knowledge + classify_use_case.
                        new LlmCallEvent(0, 0, "deepseek", 100, 50, 1500L,
                                "{\"finish_reason\":\"stop\"}"),
                        new LlmCallEvent(1, 1, "deepseek", 100, 50, 1500L,
                                "{\"finish_reason\":\"stop\"}")),
                List.of(
                        new ToolEvent(1, 0, "search_knowledge", null, true,
                                java.util.Map.of("hits", List.of()), null, 100L),
                        new ToolEvent(2, 1, "classify_use_case", null, true,
                                java.util.Map.of("committed", true, "use_case_id", "UC-A"),
                                null, 50L)),
                "{\"phase\":\"DISCOVER\"}",
                "{}");
        AgentRunResult resolveResult = AgentRunResult.finalAnswer(
                "Here is how ads become visible…",
                List.of(new LlmCallEvent(0, 0, "deepseek", 200, 80, 1700L,
                        "{\"finish_reason\":\"stop\"}")),
                List.of(new ToolEvent(1, 0, "resolve_article", null, true,
                        java.util.Map.of("source_id", "kb-1"), null, 80L)),
                "{\"phase\":\"RESOLVE\"}",
                "{\"user_message\":\"Here is how…\"}");

        when(agentRunLoop.run(eq(discover), any(), anyString(), any()))
                .thenAnswer(inv -> {
                    // Simulate the production behaviour: each successful LLM
                    // call inside the AgentRunLoop drains one slot from the
                    // shared per-invocation HTTP-attempt budget. The §M3
                    // replan must still proceed because (a) wall-clock has
                    // ample time left and (b) FallbackLlmClient.chat() on
                    // the next invocation re-arms the per-invocation budget.
                    LlmCallContext.consumeAttempt();
                    LlmCallContext.consumeAttempt();
                    BotSession sess = inv.getArgument(1);
                    sess.setActiveUseCase("UC-A");
                    sess.setIntentConfidence(new java.math.BigDecimal("0.92"));
                    return discoverResult;
                });
        when(agentRunLoop.run(eq(resolve), any(), anyString(), any()))
                .thenReturn(resolveResult);

        PhaseTransitionDecision finalDecision = new PhaseTransitionDecision(
                "CONFIRM", "Here is how ads become visible…", null, "answer_provided");
        when(phaseEvaluator.interpretRunResult(eq(resolve), any(AgentRunResult.class), any()))
                .thenReturn(finalDecision);

        // ── Act ──────────────────────────────────────────────────
        ControlKernel.KernelResult kernel =
                controlKernel.processMessage(session, "where is my advert? I can't see it");

        // ── Assert ───────────────────────────────────────────────
        assertEquals("UC-A", session.getActiveUseCase(),
                "DISCOVER classify_use_case must commit UC-A on the session");
        assertNull(session.getEscalationReason(),
                "Per-invocation budget reset must not push the run into ESCALATE");
        assertEquals("Here is how ads become visible…", kernel.responseText(),
                "Same-turn RESOLVE replan must run and produce the FAQ answer "
                        + "even when two prior successful DISCOVER LLM calls drained the "
                        + "per-invocation HTTP-attempt counter — only wall-clock budget "
                        + "should gate the replan");
        assertFalse(kernel.shouldEndChat(),
                "After RESOLVE FINAL_ANSWER → CONFIRM, the chat should stay open until CLOSE");
        verify(phaseEvaluator, times(2)).plan(any(), anyString(), any());
        verify(agentRunLoop, times(1)).run(eq(discover), any(), anyString(), any());
        verify(agentRunLoop, times(1)).run(eq(resolve), any(), anyString(), any());
        verify(eventEmitter, never())
                .emitEscalationRequested(anyString(), org.mockito.ArgumentMatchers.anyInt(), anyString());
    }

    @Test
    void discoverClassifySuccess_insufficientBudget_skipsReplan_staysInResolve() {
        // ── Arrange: a deadline that cannot fit a second RESOLVE
        // attempt. The kernel must skip the replan, leave the session
        // in RESOLVE, and return a non-terminal "looking into it"
        // response — no escalation.
        LlmCallContext.setDeadline(System.currentTimeMillis() + 1_500L); // < MIN_RESOLVE_REPLAN_BUDGET_MS

        BotSession session = adSupportSession();
        when(budgetChecker.checkBudgets(any())).thenReturn(Optional.empty());
        when(driftDetector.detect(any(), anyString())).thenReturn(
                DriftResult.builder().type(DriftResult.DriftType.NONE).build());
        when(turnRepository.findBySessionIdOrderByTurnIndex(anyString()))
                .thenReturn(List.of());

        PhasePlan discover = discoverPlan();
        when(phaseEvaluator.plan(any(), anyString(), any())).thenReturn(discover);
        when(controlPolicy.isValidTransition(eq("DISCOVER"), eq("RESOLVE"))).thenReturn(true);

        AgentRunResult discoverResult = AgentRunResult.useCaseIdentified(
                "UC-A",
                List.of(new LlmCallEvent(0, 0, "deepseek", 100, 50, 1500L,
                        "{\"finish_reason\":\"stop\"}")),
                List.of(new ToolEvent(1, 0, "classify_use_case", null, true,
                        java.util.Map.of("committed", true, "use_case_id", "UC-A"), null, 50L)),
                "{\"phase\":\"DISCOVER\"}",
                "{}");
        when(agentRunLoop.run(any(), any(), anyString(), any()))
                .thenAnswer(inv -> {
                    BotSession sess = inv.getArgument(1);
                    sess.setActiveUseCase("UC-A");
                    return discoverResult;
                });

        // Kernel will call interpretRunResult on the original
        // USE_CASE_IDENTIFIED result (replan was skipped). Mirror the
        // production mapping: RESOLVE / uc_identified / "looking into
        // this" placeholder.
        PhaseTransitionDecision transitional = new PhaseTransitionDecision(
                "RESOLVE", "I'm looking into this for you.", null, "uc_identified");
        when(phaseEvaluator.interpretRunResult(eq(discover), any(AgentRunResult.class), any()))
                .thenReturn(transitional);

        // ── Act ──────────────────────────────────────────────────
        ControlKernel.KernelResult kernel =
                controlKernel.processMessage(session, "where is my advert?");

        // ── Assert ───────────────────────────────────────────────
        assertEquals("UC-A", session.getActiveUseCase(),
                "Classify still committed UC-A — only the same-turn replan is skipped");
        assertEquals("RESOLVE", session.getCurrentPhase(),
                "DISCOVER → RESOLVE transition still applied even when replan is skipped");
        assertEquals("I'm looking into this for you.", kernel.responseText(),
                "Insufficient-budget path returns the transitional 'looking into it' response");
        assertFalse(kernel.shouldEndChat(),
                "Insufficient-budget path must NOT end the chat — user retries on next turn");
        assertNull(session.getEscalationReason(),
                "No business escalation reason is stamped");
        // AgentRunLoop ran once (DISCOVER); the RESOLVE replan was skipped on budget.
        verify(agentRunLoop, times(1)).run(any(), any(), anyString(), any());
    }
}
