package com.gumtree.csagent.service.runtime;

import com.gumtree.csagent.model.AgentRunResult;
import com.gumtree.csagent.model.BotSession;
import com.gumtree.csagent.model.LlmResponse;
import com.gumtree.csagent.model.ParsedAction;
import com.gumtree.csagent.model.PhasePlan;
import com.gumtree.csagent.model.TerminalOutcome;
import com.gumtree.csagent.model.ToolCall;
import com.gumtree.csagent.service.tools.ToolDispatcher;
import com.gumtree.csagent.service.tools.ToolResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link AgentRunLoopImpl} (D16.B). Verifies the loop body
 * faithfully implements the Phase 4 §D16.B.2 pseudocode and produces a
 * well-formed {@link AgentRunResult} for each terminal outcome.
 */
@ExtendWith(MockitoExtension.class)
class AgentRunLoopImplTest {

    @Mock private LlmInvocationService llmInvocation;
    @Mock private ToolDispatcher toolDispatcher;
    @Mock private ContextProjectionBuilder contextProjectionBuilder;
    @Mock private ActionParser actionParser;

    private AgentRunLoopImpl loop;

    @BeforeEach
    void setUp() {
        loop = new AgentRunLoopImpl(llmInvocation, toolDispatcher,
                contextProjectionBuilder, actionParser, new com.fasterxml.jackson.databind.ObjectMapper());
    }

    private PhasePlan plan() {
        return PhasePlan.builder()
                .phase("RESOLVE")
                .useCase("UC-A")
                .objective("test")
                .allowedTools(List.of("get_customer_context", "search_knowledge", "request_handover"))
                .maxToolSteps(3)
                .validTerminalOutcomes(Set.of(TerminalOutcome.FINAL_ANSWER, TerminalOutcome.ESCALATE))
                .build();
    }

    private BotSession session() {
        BotSession s = new BotSession();
        s.setSessionId("sess-1");
        s.setCurrentPhase("RESOLVE");
        s.setActiveUseCase("UC-A");
        s.setTotalBotTurns(0);
        return s;
    }

    @Test
    void singleIteration_finalAnswer() {
        when(contextProjectionBuilder.build(any(), any(), any(PhasePlan.class), anyString(), any()))
                .thenReturn("{\"x\":1}");
        when(llmInvocation.invokeChat(anyString(), anyString(), anyString(), anyInt()))
                .thenReturn(LlmResponse.builder().content("{}").build());
        when(actionParser.parse(any())).thenReturn(ParsedAction.builder()
                .toolCalls(List.of())
                .userMessage("Here is the answer.")
                .build());

        AgentRunResult result = loop.run(plan(), session(), "user q", List.of());

        assertEquals(TerminalOutcome.FINAL_ANSWER, result.terminalOutcome());
        assertEquals("Here is the answer.", result.finalUserMessage());
        assertEquals(1, result.llmEvents().size());
        assertEquals(0, result.toolEvents().size());
        verify(toolDispatcher, never()).dispatch(any(), any(), any());
    }

    @Test
    void multiIteration_dispatchesToolThenReturnsFinalAnswer() {
        when(contextProjectionBuilder.build(any(), any(), any(PhasePlan.class), anyString(), any()))
                .thenReturn("{}");
        // First LLM call: requests get_customer_context
        // Second LLM call: returns final answer
        when(llmInvocation.invokeChat(anyString(), anyString(), anyString(), anyInt()))
                .thenReturn(LlmResponse.builder().content("call").build())
                .thenReturn(LlmResponse.builder().content("final").build());
        when(actionParser.parse("call")).thenReturn(ParsedAction.builder()
                .toolCalls(List.of(ToolCall.builder()
                        .name("get_customer_context")
                        .arguments(Map.of("ad_id", "AD-1002"))
                        .build()))
                .userMessage("")
                .build());
        when(actionParser.parse("final")).thenReturn(ParsedAction.builder()
                .toolCalls(List.of())
                .userMessage("Your ad AD-1002 has been removed.")
                .build());

        when(toolDispatcher.validateAgainstPlan(any(PhasePlan.class), eq("get_customer_context")))
                .thenReturn(ToolResult.ok(null));
        when(toolDispatcher.dispatch(eq("get_customer_context"), any(), any()))
                .thenReturn(ToolResult.ok(Map.of("ad_id", "AD-1002", "status", "removed")));

        AgentRunResult result = loop.run(plan(), session(), "AD-1002", List.of());

        assertEquals(TerminalOutcome.FINAL_ANSWER, result.terminalOutcome());
        assertEquals("Your ad AD-1002 has been removed.", result.finalUserMessage());
        assertEquals(2, result.llmEvents().size());
        assertEquals(1, result.toolEvents().size());
        // Sequence indices should be monotonic across both event types
        assertEquals(0, result.llmEvents().get(0).sequenceIndex());
        assertEquals(1, result.toolEvents().get(0).sequenceIndex());
        assertEquals(2, result.llmEvents().get(1).sequenceIndex());
        assertTrue(result.toolEvents().get(0).success());
        assertEquals("get_customer_context", result.toolEvents().get(0).toolName());
    }

    @Test
    void toolNotInPlan_isRejectedAndLoopContinues() {
        when(contextProjectionBuilder.build(any(), any(), any(PhasePlan.class), anyString(), any()))
                .thenReturn("{}");
        when(llmInvocation.invokeChat(anyString(), anyString(), anyString(), anyInt()))
                .thenReturn(LlmResponse.builder().content("disallowed").build())
                .thenReturn(LlmResponse.builder().content("final").build());
        when(actionParser.parse("disallowed")).thenReturn(ParsedAction.builder()
                .toolCalls(List.of(ToolCall.builder()
                        .name("create_case_controlled")
                        .arguments(Map.of())
                        .build()))
                .userMessage("")
                .build());
        when(actionParser.parse("final")).thenReturn(ParsedAction.builder()
                .toolCalls(List.of())
                .userMessage("Sorry, I can't do that. Here's an answer instead.")
                .build());

        when(toolDispatcher.validateAgainstPlan(any(PhasePlan.class), eq("create_case_controlled")))
                .thenReturn(ToolResult.error("tool_not_in_plan: create_case_controlled not in [...]"));

        AgentRunResult result = loop.run(plan(), session(), "msg", List.of());

        assertEquals(TerminalOutcome.FINAL_ANSWER, result.terminalOutcome());
        assertEquals(1, result.toolEvents().size());
        assertFalse(result.toolEvents().get(0).success());
        assertTrue(result.toolEvents().get(0).errorMessage().contains("tool_not_in_plan"));
        verify(toolDispatcher, never()).dispatch(any(), any(), any());
    }

    @Test
    void maxSteps_returnsMaxStepsOutcome() {
        when(contextProjectionBuilder.build(any(), any(), any(PhasePlan.class), anyString(), any()))
                .thenReturn("{}");
        // Every LLM call requests a tool. Loop should hit maxToolSteps.
        when(llmInvocation.invokeChat(anyString(), anyString(), anyString(), anyInt()))
                .thenReturn(LlmResponse.builder().content("call").build());
        when(actionParser.parse(any())).thenReturn(ParsedAction.builder()
                .toolCalls(List.of(ToolCall.builder()
                        .name("get_customer_context")
                        .arguments(Map.of())
                        .build()))
                .userMessage("")
                .build());
        when(toolDispatcher.validateAgainstPlan(any(PhasePlan.class), anyString()))
                .thenReturn(ToolResult.ok(null));
        when(toolDispatcher.dispatch(anyString(), any(), any()))
                .thenReturn(ToolResult.ok(Map.of("k", "v")));

        AgentRunResult result = loop.run(plan(), session(), "msg", List.of());

        assertEquals(TerminalOutcome.MAX_STEPS, result.terminalOutcome());
        assertEquals(3, result.llmEvents().size());
        assertEquals(3, result.toolEvents().size());
    }

    @Test
    void llmException_returnsError() {
        when(contextProjectionBuilder.build(any(), any(), any(PhasePlan.class), anyString(), any()))
                .thenReturn("{}");
        when(llmInvocation.invokeChat(anyString(), anyString(), anyString(), anyInt()))
                .thenThrow(new RuntimeException("connection refused"));

        AgentRunResult result = loop.run(plan(), session(), "msg", List.of());

        assertEquals(TerminalOutcome.ERROR, result.terminalOutcome());
        assertTrue(result.escalationReason().orElse("").contains("llm_invocation_failed"));
    }

    @Test
    void handoverRequested_shortCircuitsToEscalate() {
        when(contextProjectionBuilder.build(any(), any(), any(PhasePlan.class), anyString(), any()))
                .thenReturn("{}");
        when(llmInvocation.invokeChat(anyString(), anyString(), anyString(), anyInt()))
                .thenReturn(LlmResponse.builder().content("handover").build());
        when(actionParser.parse(any())).thenReturn(ParsedAction.builder()
                .toolCalls(List.of(ToolCall.builder()
                        .name("request_handover")
                        .arguments(Map.of("escalation_reason", "user_distress"))
                        .build()))
                .userMessage("Connecting you now.")
                .build());
        when(toolDispatcher.validateAgainstPlan(any(PhasePlan.class), eq("request_handover")))
                .thenReturn(ToolResult.ok(null));
        when(toolDispatcher.dispatch(eq("request_handover"), any(), any()))
                .thenReturn(ToolResult.ok(Map.of("handover_status", "queued")));

        AgentRunResult result = loop.run(plan(), session(), "msg", List.of());

        assertEquals(TerminalOutcome.ESCALATE, result.terminalOutcome());
        assertEquals("user_distress", result.escalationReason().orElse(null));
        assertEquals(1, result.toolEvents().size());
        assertEquals(1, result.llmEvents().size());
    }

    @Test
    void parseReturnsNull_treatsAsFinalAnswer() {
        when(contextProjectionBuilder.build(any(), any(), any(PhasePlan.class), anyString(), any()))
                .thenReturn("{}");
        when(llmInvocation.invokeChat(anyString(), anyString(), anyString(), anyInt()))
                .thenReturn(LlmResponse.builder().content("garbage").build());
        when(actionParser.parse(any())).thenReturn(null);

        AgentRunResult result = loop.run(plan(), session(), "msg", List.of());

        assertEquals(TerminalOutcome.FINAL_ANSWER, result.terminalOutcome());
        assertEquals("garbage", result.finalUserMessage());
    }

    @Test
    void nullPlan_returnsError() {
        AgentRunResult result = loop.run(null, session(), "msg", List.of());
        assertEquals(TerminalOutcome.ERROR, result.terminalOutcome());
    }

    @Test
    void accumulatedToolResults_passedToNextProjection() {
        when(contextProjectionBuilder.build(any(), any(), any(PhasePlan.class), anyString(), any()))
                .thenReturn("{}");
        when(llmInvocation.invokeChat(anyString(), anyString(), anyString(), anyInt()))
                .thenReturn(LlmResponse.builder().content("c1").build())
                .thenReturn(LlmResponse.builder().content("c2").build());
        when(actionParser.parse("c1")).thenReturn(ParsedAction.builder()
                .toolCalls(List.of(ToolCall.builder()
                        .name("get_customer_context").arguments(Map.of()).build()))
                .userMessage("").build());
        when(actionParser.parse("c2")).thenReturn(ParsedAction.builder()
                .toolCalls(List.of()).userMessage("done").build());
        when(toolDispatcher.validateAgainstPlan(any(PhasePlan.class), anyString()))
                .thenReturn(ToolResult.ok(null));
        when(toolDispatcher.dispatch(anyString(), any(), any()))
                .thenReturn(ToolResult.ok(Map.of("k", "v")));

        AgentRunResult result = loop.run(plan(), session(), "msg", List.of());
        assertEquals(TerminalOutcome.FINAL_ANSWER, result.terminalOutcome());

        // Verify second call to projection builder received the accumulated result
        org.mockito.ArgumentCaptor<Map<String, Object>> captor =
                org.mockito.ArgumentCaptor.forClass(Map.class);
        verify(contextProjectionBuilder, times(2))
                .build(any(), any(), any(PhasePlan.class), anyString(), captor.capture());
        // Second invocation's accumulated map should contain the tool result
        Map<String, Object> secondMap = captor.getAllValues().get(1);
        assertNotNull(secondMap);
        assertTrue(secondMap.containsKey("get_customer_context"));
    }
}
