package com.gumtree.csagent.service.runtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gumtree.csagent.model.AgentMessage;
import com.gumtree.csagent.model.AgentRunResult;
import com.gumtree.csagent.model.BotSession;
import com.gumtree.csagent.model.LlmCallEvent;
import com.gumtree.csagent.model.LlmResponse;
import com.gumtree.csagent.model.ParsedAction;
import com.gumtree.csagent.model.PhasePlan;
import com.gumtree.csagent.model.PhaseTransitionDecision;
import com.gumtree.csagent.model.TerminalOutcome;
import com.gumtree.csagent.model.ToolCall;
import com.gumtree.csagent.model.ToolEvent;
import com.gumtree.csagent.service.guardrails.ScriptLibraryService;
import com.gumtree.csagent.service.knowledge.KnowledgeSearchService;
import com.gumtree.csagent.service.observability.EventEmitter;
import com.gumtree.csagent.service.tools.CreateCaseControlledTool;
import com.gumtree.csagent.service.tools.ToolDispatcher;
import com.gumtree.csagent.service.tools.ToolResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Sprint 9 §O1 — terminal-state honesty. Failed terminal-tool dispatches
 * (record_outcome, request_handover) must not silently advance the
 * session phase as though they had succeeded.
 *
 * <ul>
 *   <li>Failed {@code record_outcome} on a FAQ-path RESOLVE plan must not
 *       trigger RESOLVE → CONFIRM via {@link PhaseEvaluator#interpretRunResult}.</li>
 *   <li>Successful {@code record_outcome} still triggers the canonical
 *       RESOLVE → CONFIRM transition.</li>
 *   <li>Failed {@code request_handover} dispatch must not be treated as a
 *       successful terminal escalation by the {@link AgentRunLoopImpl}; the
 *       loop must continue (or hit MAX_STEPS) instead of returning
 *       {@link AgentRunResult#escalate}.</li>
 *   <li>Successful {@code request_handover} dispatch still short-circuits
 *       to ESCALATE.</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class Sprint9TerminalToolHonestyTest {

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
    private AgentRunLoopImpl loop;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        evaluator = new PhaseEvaluator(
                useCaseRegistry, knowledgeSearchService, scriptLibrary,
                llmInvocation, contextProjection, actionParser,
                objectMapper, createCaseTool, eventEmitter, toolDispatcher);
        loop = new AgentRunLoopImpl(llmInvocation, toolDispatcher, contextProjection,
                actionParser, objectMapper);
    }

    private static PhasePlan resolveFaqPlan() {
        return PhasePlan.builder()
                .phase("RESOLVE")
                .useCase("UC-A")
                .objective("test")
                .allowedTools(List.of("search_knowledge", "resolve_article",
                        "record_outcome", "request_handover"))
                .requiredContextKeys(Set.of())
                .maxToolSteps(3)
                .allowInterimMessage(false)
                .validTerminalOutcomes(Set.of(
                        TerminalOutcome.FINAL_ANSWER,
                        TerminalOutcome.ESCALATE,
                        TerminalOutcome.MAX_STEPS))
                .systemInstruction("test")
                .groundingInstruction("test")
                .escalationPolicy("test")
                .build();
    }

    private static BotSession session() {
        BotSession s = new BotSession();
        s.setSessionId("sess-sprint9");
        s.setCurrentPhase("RESOLVE");
        s.setActiveUseCase("UC-A");
        s.setTotalBotTurns(1);
        return s;
    }

    @Test
    void interpretRunResult_failedRecordOutcome_doesNotTransitionResolveToConfirm() {
        // Build a FINAL_ANSWER result that contains a failed record_outcome
        // tool event in toolEvents. PhaseEvaluator must keep the session in
        // RESOLVE (not advance to CONFIRM as if the outcome had been
        // recorded) so the LLM can retry on the next user turn.
        ToolEvent failedRecord = new ToolEvent(0, 0, "record_outcome",
                Map.of("outcome_class", "resolve"),
                false, null, "DB write failed", 5L);
        AgentRunResult result = new AgentRunResult(
                List.of(AgentMessage.finalMessage("All sorted!")),
                List.of(failedRecord),
                List.<LlmCallEvent>of(),
                TerminalOutcome.FINAL_ANSWER,
                "All sorted!",
                Optional.empty(),
                "{}",
                "raw");

        PhaseTransitionDecision decision =
                evaluator.interpretRunResult(resolveFaqPlan(), result, session());

        assertEquals("RESOLVE", decision.nextPhase(),
                "Sprint 9 §O1: failed record_outcome must NOT advance RESOLVE → CONFIRM");
        assertNotEquals("answer_provided", decision.transitionReason(),
                "transition reason must reflect the retry, not pretend the answer was confirmed");
    }

    @Test
    void interpretRunResult_successfulRecordOutcome_stillTransitionsToConfirm() {
        ToolEvent successRecord = new ToolEvent(0, 0, "record_outcome",
                Map.of("outcome_class", "resolve"),
                true,
                Map.of("outcome_class", "resolve", "outcome", "RESOLVED"),
                null, 5L);
        AgentRunResult result = new AgentRunResult(
                List.of(AgentMessage.finalMessage("Glad I could help!")),
                List.of(successRecord),
                List.<LlmCallEvent>of(),
                TerminalOutcome.FINAL_ANSWER,
                "Glad I could help!",
                Optional.empty(),
                "{}",
                "raw");

        PhaseTransitionDecision decision =
                evaluator.interpretRunResult(resolveFaqPlan(), result, session());

        assertEquals("CONFIRM", decision.nextPhase(),
                "successful record_outcome still drives the canonical RESOLVE → CONFIRM transition");
        assertEquals("answer_provided", decision.transitionReason());
    }

    @Test
    void agentRunLoop_failedRequestHandover_doesNotShortCircuitToEscalate() {
        PhasePlan plan = PhasePlan.builder()
                .phase("RESOLVE")
                .useCase("UC-A")
                .objective("test")
                .allowedTools(List.of("request_handover"))
                .requiredContextKeys(Set.of())
                .maxToolSteps(2)
                .allowInterimMessage(false)
                .validTerminalOutcomes(Set.of(
                        TerminalOutcome.ESCALATE,
                        TerminalOutcome.MAX_STEPS,
                        TerminalOutcome.FINAL_ANSWER))
                .systemInstruction("test")
                .groundingInstruction("test")
                .escalationPolicy("test")
                .build();
        BotSession s = session();

        when(contextProjection.build(any(), any(), any(PhasePlan.class), anyString(), any()))
                .thenReturn("{}");
        when(llmInvocation.invokeChat(anyString(), anyString(), anyString(), anyInt()))
                .thenReturn(LlmResponse.builder().content("step0").build())
                .thenReturn(LlmResponse.builder().content("step1").build());

        ParsedAction handoverCall = ParsedAction.builder()
                .toolCalls(List.of(ToolCall.builder()
                        .name("request_handover")
                        .arguments(Map.of("escalation_reason", "user_requested"))
                        .build()))
                .userMessage("")
                .build();
        when(actionParser.parse("step0")).thenReturn(handoverCall);
        when(actionParser.parse("step1")).thenReturn(handoverCall);

        when(toolDispatcher.validateAgainstPlan(eq(plan), eq("request_handover")))
                .thenReturn(ToolResult.ok(null));
        // Both dispatches FAIL — the loop must NOT short-circuit to
        // AgentRunResult.escalate just because the LLM tried to call
        // request_handover. Instead, it should run out of steps.
        when(toolDispatcher.dispatch(eq("request_handover"), eq(s), any()))
                .thenReturn(ToolResult.error("salesforce_handover_failed: 503"));

        AgentRunResult result = loop.run(plan, s, "please escalate", List.of());

        assertEquals(TerminalOutcome.MAX_STEPS, result.terminalOutcome(),
                "Sprint 9 §O1: a failed request_handover dispatch must NOT be treated as "
                        + "a successful terminal escalation; the loop should hit MAX_STEPS instead.");
        assertFalse(result.escalationReason().isPresent(),
                "no synthetic escalation reason should be stamped when handover dispatches failed");
        // The error must surface in the trace (toolEvents) so the LLM sees it.
        assertTrue(result.toolEvents().stream().anyMatch(te ->
                        "request_handover".equals(te.toolName())
                                && !te.success()
                                && te.errorMessage() != null
                                && te.errorMessage().contains("salesforce_handover_failed")),
                "failed handover must be recorded as a non-successful ToolEvent so the trace "
                        + "and accumulated_tool_results can surface the error to the LLM");
    }

    @Test
    void agentRunLoop_successfulRequestHandover_stillEscalates() {
        PhasePlan plan = PhasePlan.builder()
                .phase("RESOLVE")
                .useCase("UC-A")
                .objective("test")
                .allowedTools(List.of("request_handover"))
                .requiredContextKeys(Set.of())
                .maxToolSteps(2)
                .allowInterimMessage(false)
                .validTerminalOutcomes(Set.of(
                        TerminalOutcome.ESCALATE,
                        TerminalOutcome.MAX_STEPS,
                        TerminalOutcome.FINAL_ANSWER))
                .systemInstruction("test")
                .groundingInstruction("test")
                .escalationPolicy("test")
                .build();
        BotSession s = session();

        when(contextProjection.build(any(), any(), any(PhasePlan.class), anyString(), any()))
                .thenReturn("{}");
        when(llmInvocation.invokeChat(anyString(), anyString(), anyString(), anyInt()))
                .thenReturn(LlmResponse.builder().content("step0").build());
        when(actionParser.parse("step0")).thenReturn(ParsedAction.builder()
                .toolCalls(List.of(ToolCall.builder()
                        .name("request_handover")
                        .arguments(Map.of("escalation_reason", "user_requested"))
                        .build()))
                .userMessage("")
                .build());
        when(toolDispatcher.validateAgainstPlan(eq(plan), eq("request_handover")))
                .thenReturn(ToolResult.ok(null));
        when(toolDispatcher.dispatch(eq("request_handover"), eq(s), any()))
                .thenReturn(ToolResult.ok(Map.of("transfer_result", "queued")));

        AgentRunResult result = loop.run(plan, s, "please escalate", List.of());

        assertEquals(TerminalOutcome.ESCALATE, result.terminalOutcome(),
                "successful request_handover must still short-circuit the loop to ESCALATE");
        assertTrue(result.escalationReason().isPresent());
        assertEquals("user_requested", result.escalationReason().get());
    }
}
