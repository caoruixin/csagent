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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
 * Sprint 103 / WS-6-A — the loop-side half of the mid-session re-route
 * boundary: an honoured {@code propose_reroute} must end the run immediately
 * with {@link TerminalOutcome#USE_CASE_REROUTED} so {@code ControlKernel} can
 * replan into the target UC's Skill, and a DECLINED proposal must not.
 *
 * <p>Why the distinction matters: the plan being executed belongs to the UC the
 * session just left. Continuing it would dispatch that UC's tool whitelist and
 * guardrails while {@code ToolDispatcher.dispatch} policy-checks against the
 * new UC — the split-brain that ruled out re-using {@code classify_use_case}
 * here (see {@code ProposeRerouteTool} class javadoc).
 *
 * <p>Per iteration_governance §5.7 this is wiring evidence, not behaviour
 * evidence: the mock decides what the LLM "chose".
 */
@ExtendWith(MockitoExtension.class)
class AgentRunLoopRerouteBoundaryTest {

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

    @Test
    void honouredReroute_endsRunWithUseCaseRerouted() {
        BotSession session = session("UC-D");
        stubLlmProposingReroute();
        when(toolDispatcher.validateAgainstPlan(any(PhasePlan.class), eq("propose_reroute")))
                .thenReturn(ToolResult.ok(null));
        // The tool has already moved the session by the time it returns —
        // mirror that here so the loop sees the post-move state it will see in
        // production.
        when(toolDispatcher.dispatch(eq("propose_reroute"), any(), any()))
                .thenAnswer(inv -> {
                    session.setPreviousActiveUseCase("UC-D");
                    session.setActiveUseCase("UC-H");
                    return ToolResult.ok(rerouteResult(true, null));
                });

        AgentRunResult result = loop.run(plan("RESOLVE", "UC-D"), session, "and also…", List.of());

        assertEquals(TerminalOutcome.USE_CASE_REROUTED, result.terminalOutcome());
        assertEquals("UC-H", result.finalUserMessage(),
                "the target UC rides in finalUserMessage as a marker, exactly as "
                        + "USE_CASE_IDENTIFIED does; it is not customer-facing");
        assertEquals(1, result.llmEvents().size(),
                "the run must stop at the re-route, not spend the rest of the old UC's budget");
    }

    @Test
    void honouredRerouteFromConfirm_endsRunWithUseCaseRerouted() {
        // CONFIRM is where "yes, but also…" lands. Same boundary, different
        // entry phase; the landing phase is RESOLVE in both cases.
        BotSession session = session("UC-D");
        session.setCurrentPhase("CONFIRM");
        stubLlmProposingReroute();
        when(toolDispatcher.validateAgainstPlan(any(PhasePlan.class), eq("propose_reroute")))
                .thenReturn(ToolResult.ok(null));
        when(toolDispatcher.dispatch(eq("propose_reroute"), any(), any()))
                .thenAnswer(inv -> {
                    session.setActiveUseCase("UC-H");
                    return ToolResult.ok(rerouteResult(true, null));
                });

        AgentRunResult result = loop.run(plan("CONFIRM", "UC-D"), session, "thanks, but also…",
                List.of());

        assertEquals(TerminalOutcome.USE_CASE_REROUTED, result.terminalOutcome());
        assertEquals("UC-H", result.finalUserMessage(),
                "the target UC rides in finalUserMessage as a marker, exactly as "
                        + "USE_CASE_IDENTIFIED does; it is not customer-facing");
    }

    @Test
    void declinedReroute_doesNotEndRun() {
        BotSession session = session("UC-D");
        when(contextProjectionBuilder.build(any(), any(), any(PhasePlan.class), anyString(), any(), any()))
                .thenReturn("{}");
        when(llmInvocation.invokeChat(anyString(), anyString(), anyString(), anyInt()))
                .thenReturn(LlmResponse.builder().content("{}").build());
        when(actionParser.parse(any()))
                .thenReturn(ParsedAction.builder()
                        .toolCalls(List.of(ToolCall.builder()
                                .name("propose_reroute")
                                .arguments(Map.of("target_use_case", "UC-D",
                                        "reasoning", "same one really"))
                                .build()))
                        .userMessage("")
                        .build())
                .thenReturn(ParsedAction.builder()
                        .toolCalls(List.of())
                        .userMessage("Right — staying with your login issue then.")
                        .build());
        when(toolDispatcher.validateAgainstPlan(any(PhasePlan.class), eq("propose_reroute")))
                .thenReturn(ToolResult.ok(null));
        when(toolDispatcher.dispatch(eq("propose_reroute"), any(), any()))
                .thenReturn(ToolResult.ok(rerouteResult(false, "already_active")));

        AgentRunResult result = loop.run(plan("RESOLVE", "UC-D"), session, "msg", List.of());

        assertNotEquals(TerminalOutcome.USE_CASE_REROUTED, result.terminalOutcome());
        assertEquals(TerminalOutcome.FINAL_ANSWER, result.terminalOutcome());
        assertEquals("Right — staying with your login issue then.", result.finalUserMessage());
        assertEquals("UC-D", session.getActiveUseCase());
    }

    @Test
    void isRerouteHonoured_readsTheFlagNotTheSuccessBit() {
        assertFalse(AgentRunLoopImpl.isRerouteHonoured(null));
        assertFalse(AgentRunLoopImpl.isRerouteHonoured(ToolResult.ok(null)));
        assertFalse(AgentRunLoopImpl.isRerouteHonoured(
                ToolResult.ok(rerouteResult(false, "no_skill_for_target_use_case"))));
        assertTrue(AgentRunLoopImpl.isRerouteHonoured(
                ToolResult.ok(rerouteResult(true, null))));
    }

    private void stubLlmProposingReroute() {
        when(contextProjectionBuilder.build(any(), any(), any(PhasePlan.class), anyString(), any(), any()))
                .thenReturn("{}");
        when(llmInvocation.invokeChat(anyString(), anyString(), anyString(), anyInt()))
                .thenReturn(LlmResponse.builder().content("{}").build());
        when(actionParser.parse(any())).thenReturn(ParsedAction.builder()
                .toolCalls(List.of(ToolCall.builder()
                        .name("propose_reroute")
                        .arguments(Map.of("target_use_case", "UC-H",
                                "reasoning", "customer now asking to appeal a removal"))
                        .build()))
                .userMessage("")
                .build());
    }

    private static Map<String, Object> rerouteResult(boolean honoured, String reason) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("honoured", honoured);
        data.put("target_use_case", honoured ? "UC-H" : "UC-D");
        if (reason != null) {
            data.put("reason", reason);
        }
        return data;
    }

    private static PhasePlan plan(String phase, String uc) {
        return PhasePlan.builder()
                .phase(phase)
                .useCase(uc)
                .objective("test")
                .allowedTools(List.of("search_knowledge", "record_outcome",
                        "request_handover", "propose_reroute"))
                .maxToolSteps(4)
                .validTerminalOutcomes(Set.of(TerminalOutcome.FINAL_ANSWER,
                        TerminalOutcome.ESCALATE, TerminalOutcome.USE_CASE_REROUTED))
                .build();
    }

    private static BotSession session(String uc) {
        BotSession s = new BotSession();
        s.setSessionId("sess-reroute-loop");
        s.setCurrentPhase("RESOLVE");
        s.setActiveUseCase(uc);
        s.setTotalBotTurns(2);
        return s;
    }
}
