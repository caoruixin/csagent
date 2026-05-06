package com.gumtree.csagent.service.runtime;

import com.fasterxml.jackson.databind.ObjectMapper;
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
 * Sprint 8.1 §M3 — DISCOVER successful classification phase boundary.
 *
 * <p>The trace symptom: with DISCOVER {@code allowedTools=
 * {search_knowledge, classify_use_case}} and {@code maxToolSteps=2}, a
 * happy-path turn would call {@code search_knowledge} then
 * {@code classify_use_case}, commit {@code activeUseCase=UC-A}, run out of
 * loop steps, return {@link TerminalOutcome#MAX_STEPS}, and let
 * {@link PhaseEvaluator#interpretRunResult} mis-map that to ESCALATE /
 * {@code faq_miss_threshold_exceeded} — even though the bot actually
 * succeeded. This pin verifies the new behaviour: a successful
 * {@code classify_use_case} on a DISCOVER plan returns
 * {@link TerminalOutcome#USE_CASE_IDENTIFIED} immediately so
 * {@link ControlKernel} can perform a same-turn replan into RESOLVE.
 */
@ExtendWith(MockitoExtension.class)
class Sprint81DiscoverPhaseBoundaryTest {

    @Mock private LlmInvocationService llmInvocation;
    @Mock private ToolDispatcher toolDispatcher;
    @Mock private ContextProjectionBuilder contextProjectionBuilder;
    @Mock private ActionParser actionParser;

    private AgentRunLoopImpl loop;

    @BeforeEach
    void setUp() {
        loop = new AgentRunLoopImpl(llmInvocation, toolDispatcher,
                contextProjectionBuilder, actionParser, new ObjectMapper());
    }

    private PhasePlan discoverPlan() {
        return PhasePlan.builder()
                .phase("DISCOVER")
                .useCase(null) // no UC committed yet
                .objective("identify use case")
                .allowedTools(List.of("search_knowledge", "classify_use_case"))
                .maxToolSteps(2)
                .validTerminalOutcomes(Set.of(
                        TerminalOutcome.CLARIFICATION_NEEDED,
                        TerminalOutcome.FINAL_ANSWER,
                        TerminalOutcome.ESCALATE,
                        TerminalOutcome.USE_CASE_IDENTIFIED))
                .build();
    }

    private BotSession discoverSession() {
        BotSession s = new BotSession();
        s.setSessionId("sess-m3");
        s.setCurrentPhase("DISCOVER");
        s.setActiveUseCase(null);
        s.setTotalBotTurns(0);
        return s;
    }

    @Test
    void classifyUseCaseSuccess_returnsUseCaseIdentified_notMaxSteps() {
        // The trace shape from the bug: LLM calls search_knowledge first
        // (step 0), then classify_use_case (step 1) which commits UC-A on
        // the session. Without §M3 the loop would now call the LLM a
        // third time, run out of steps (maxToolSteps=2), and return
        // MAX_STEPS — which the legacy mapper turns into ESCALATE.
        when(contextProjectionBuilder.build(any(), any(), any(PhasePlan.class), anyString(), any()))
                .thenReturn("{}");
        when(llmInvocation.invokeChat(anyString(), anyString(), anyString(), anyInt()))
                .thenReturn(LlmResponse.builder().content("step0").build())
                .thenReturn(LlmResponse.builder().content("step1").build());
        when(actionParser.parse("step0")).thenReturn(ParsedAction.builder()
                .toolCalls(List.of(ToolCall.builder()
                        .name("search_knowledge")
                        .arguments(Map.of("query", "where is my ad"))
                        .build()))
                .userMessage("")
                .build());
        when(actionParser.parse("step1")).thenReturn(ParsedAction.builder()
                .toolCalls(List.of(ToolCall.builder()
                        .name("classify_use_case")
                        .arguments(Map.of("use_case_id", "UC-A", "confidence", 0.9))
                        .build()))
                .userMessage("")
                .build());
        when(toolDispatcher.validateAgainstPlan(any(PhasePlan.class), anyString()))
                .thenReturn(ToolResult.ok(null));
        // search_knowledge returns hits
        when(toolDispatcher.dispatch(eq("search_knowledge"), any(), any()))
                .thenReturn(ToolResult.ok(Map.of("hits", List.of(Map.of("source_id", "src-1")))));
        // classify_use_case sets activeUseCase as a side effect AND returns success
        BotSession session = discoverSession();
        when(toolDispatcher.dispatch(eq("classify_use_case"), eq(session), any()))
                .thenAnswer(inv -> {
                    session.setActiveUseCase("UC-A");
                    return ToolResult.ok(Map.of("committed", true, "use_case_id", "UC-A"));
                });

        AgentRunResult result = loop.run(discoverPlan(), session, "where is my advert", List.of());

        assertEquals(TerminalOutcome.USE_CASE_IDENTIFIED, result.terminalOutcome(),
                "DISCOVER + successful classify_use_case must return USE_CASE_IDENTIFIED, not MAX_STEPS / ESCALATE");
        assertEquals("UC-A", session.getActiveUseCase());
        assertEquals(2, result.toolEvents().size(),
                "Both DISCOVER tool events (search_knowledge, classify_use_case) must be preserved");
        assertTrue(result.escalationReason().isEmpty(),
                "USE_CASE_IDENTIFIED is not an escalation");
    }

    @Test
    void classifyUseCaseFailure_doesNotReturnUseCaseIdentified() {
        // When classify_use_case dispatch fails (e.g. invalid UC, strong-prior
        // refusal that left activeUseCase null), the loop must NOT take the
        // §M3 phase boundary — there is nothing to resolve.
        when(contextProjectionBuilder.build(any(), any(), any(PhasePlan.class), anyString(), any()))
                .thenReturn("{}");
        when(llmInvocation.invokeChat(anyString(), anyString(), anyString(), anyInt()))
                .thenReturn(LlmResponse.builder().content("step0").build())
                .thenReturn(LlmResponse.builder().content("step1").build());
        when(actionParser.parse(any())).thenReturn(ParsedAction.builder()
                .toolCalls(List.of(ToolCall.builder()
                        .name("classify_use_case")
                        .arguments(Map.of("use_case_id", "UC-X"))
                        .build()))
                .userMessage("")
                .build());
        when(toolDispatcher.validateAgainstPlan(any(PhasePlan.class), anyString()))
                .thenReturn(ToolResult.ok(null));
        // Tool returns failure; activeUseCase stays null
        when(toolDispatcher.dispatch(eq("classify_use_case"), any(), any()))
                .thenReturn(ToolResult.error("invalid_use_case_id"));

        BotSession session = discoverSession();
        AgentRunResult result = loop.run(discoverPlan(), session, "msg", List.of());

        assertNotEquals(TerminalOutcome.USE_CASE_IDENTIFIED, result.terminalOutcome());
        assertNull(session.getActiveUseCase());
    }

    @Test
    void classifyUseCaseInResolve_isNotPhaseBoundary() {
        // §M3 fires only when phase=DISCOVER. A successful classify in
        // RESOLVE is unusual but must not trigger the boundary.
        PhasePlan resolvePlan = PhasePlan.builder()
                .phase("RESOLVE")
                .useCase("UC-A")
                .objective("answer")
                .allowedTools(List.of("classify_use_case", "search_knowledge"))
                .maxToolSteps(3)
                .validTerminalOutcomes(Set.of(TerminalOutcome.FINAL_ANSWER))
                .build();
        when(contextProjectionBuilder.build(any(), any(), any(PhasePlan.class), anyString(), any()))
                .thenReturn("{}");
        when(llmInvocation.invokeChat(anyString(), anyString(), anyString(), anyInt()))
                .thenReturn(LlmResponse.builder().content("c1").build())
                .thenReturn(LlmResponse.builder().content("c2").build());
        when(actionParser.parse("c1")).thenReturn(ParsedAction.builder()
                .toolCalls(List.of(ToolCall.builder()
                        .name("classify_use_case")
                        .arguments(Map.of("use_case_id", "UC-A"))
                        .build()))
                .userMessage("")
                .build());
        when(actionParser.parse("c2")).thenReturn(ParsedAction.builder()
                .toolCalls(List.of())
                .userMessage("Done.")
                .build());
        when(toolDispatcher.validateAgainstPlan(any(PhasePlan.class), anyString()))
                .thenReturn(ToolResult.ok(null));
        BotSession session = discoverSession();
        session.setCurrentPhase("RESOLVE");
        session.setActiveUseCase("UC-A");
        when(toolDispatcher.dispatch(eq("classify_use_case"), eq(session), any()))
                .thenReturn(ToolResult.ok(Map.of("committed", true, "use_case_id", "UC-A")));

        AgentRunResult result = loop.run(resolvePlan, session, "msg", List.of());

        assertEquals(TerminalOutcome.FINAL_ANSWER, result.terminalOutcome(),
                "classify_use_case success in RESOLVE must NOT short-circuit to USE_CASE_IDENTIFIED");
    }
}
