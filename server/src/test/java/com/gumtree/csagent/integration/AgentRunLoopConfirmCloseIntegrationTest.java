package com.gumtree.csagent.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gumtree.csagent.config.AgentRunLoopProperties;
import com.gumtree.csagent.model.BotSession;
import com.gumtree.csagent.model.DriftResult;
import com.gumtree.csagent.model.LlmResponse;
import com.gumtree.csagent.model.ParsedAction;
import com.gumtree.csagent.model.ToolCall;
import com.gumtree.csagent.repository.BotEventRepository;
import com.gumtree.csagent.repository.BotTurnRepository;
import com.gumtree.csagent.service.observability.EventEmitter;
import com.gumtree.csagent.service.runtime.ActionParser;
import com.gumtree.csagent.service.runtime.AgentRunLoop;
import com.gumtree.csagent.service.runtime.AgentRunLoopImpl;
import com.gumtree.csagent.service.runtime.BudgetChecker;
import com.gumtree.csagent.service.runtime.ContextProjectionBuilder;
import com.gumtree.csagent.service.runtime.ControlKernel;
import com.gumtree.csagent.service.runtime.ControlPolicyService;
import com.gumtree.csagent.service.runtime.DriftDetector;
import com.gumtree.csagent.service.runtime.EscalationReasonResolver;
import com.gumtree.csagent.service.runtime.LlmInvocationService;
import com.gumtree.csagent.service.runtime.PhaseEvaluator;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * D16.D end-to-end integration test: simulates a satisfied-user CONFIRM turn
 * driven through {@link AgentRunLoop}.
 *
 * <p>Flow:
 * <ol>
 *   <li>Session starts in CONFIRM phase with active UC=UC-A.</li>
 *   <li>User types "thanks, that helped".</li>
 *   <li>The mocked LLM response includes a {@code record_outcome(RESOLVED)}
 *       tool call followed by a brief friendly user_message.</li>
 *   <li>The loop dispatches {@code record_outcome}, then re-invokes the LLM
 *       which returns a clean closing message with no further tool calls.</li>
 *   <li>{@code PhaseEvaluator.interpretRunResult} maps CONFIRM + FINAL_ANSWER
 *       to CLOSE with reason {@code user_satisfied}.</li>
 *   <li>{@link ControlKernel} applies the transition; session phase becomes
 *       CLOSE; handlingState becomes CLOSED; SESSION_CLOSED is emitted.</li>
 * </ol>
 */
@ExtendWith(MockitoExtension.class)
class AgentRunLoopConfirmCloseIntegrationTest {

    @Mock private BotTurnRepository turnRepository;
    @Mock private BotEventRepository eventRepository;
    @Mock private BudgetChecker budgetChecker;
    @Mock private DriftDetector driftDetector;
    @Mock private ControlPolicyService controlPolicy;
    @Mock private CreateCaseControlledTool createCaseTool;
    @Mock private EventEmitter eventEmitter;
    @Mock private ContextProjectionBuilder contextProjectionBuilder;
    @Mock private LlmInvocationService llmInvocation;
    @Mock private ToolDispatcher toolDispatcher;
    @Mock private ActionParser actionParser;
    @Mock private com.gumtree.csagent.service.runtime.UseCaseRegistryService useCaseRegistry;
    @Mock private com.gumtree.csagent.service.knowledge.KnowledgeSearchService knowledgeSearchService;
    @Mock private com.gumtree.csagent.service.guardrails.ScriptLibraryService scriptLibrary;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private ControlKernel controlKernel;

    @BeforeEach
    void setUp() {
        PhaseEvaluator phaseEvaluator = new PhaseEvaluator(
                useCaseRegistry, knowledgeSearchService, scriptLibrary,
                llmInvocation, contextProjectionBuilder, actionParser,
                objectMapper, createCaseTool, eventEmitter, toolDispatcher);

        AgentRunLoop agentRunLoop = new AgentRunLoopImpl(
                llmInvocation, toolDispatcher, contextProjectionBuilder, actionParser, objectMapper);

        AgentRunLoopProperties props = new AgentRunLoopProperties();
        props.setEnabledPhases(List.of(
                "RESOLVE_FAQ", "RESOLVE_INTAKE",
                "DISCOVER", "CONFIRM", "CLOSE", "ESCALATE"));

        controlKernel = new ControlKernel(
                turnRepository, eventRepository, budgetChecker, driftDetector,
                phaseEvaluator, controlPolicy, objectMapper, createCaseTool,
                eventEmitter, contextProjectionBuilder, props, agentRunLoop,
                new EscalationReasonResolver());
    }

    @Test
    void confirmFinalAnswer_userSatisfied_transitionsToCloseAndRecordsOutcome() {
        // ── Arrange ─────────────────────────────────────────────────
        BotSession session = new BotSession();
        session.setSessionId("sess-confirm-1");
        session.setCurrentPhase("CONFIRM");
        session.setActiveUseCase("UC-A");
        session.setTotalBotTurns(1);
        session.setFormContext("{\"email\":\"alice@example.com\"}");

        when(budgetChecker.checkBudgets(any())).thenReturn(Optional.empty());
        when(driftDetector.detect(any(), anyString())).thenReturn(
                DriftResult.builder().type(DriftResult.DriftType.NONE).build());
        when(turnRepository.findBySessionIdOrderByTurnIndex(anyString()))
                .thenReturn(List.of());
        when(controlPolicy.isValidTransition("CONFIRM", "CLOSE")).thenReturn(true);

        when(contextProjectionBuilder.build(any(), any(), any(), anyString(), any()))
                .thenReturn("{\"phase\":\"CONFIRM\",\"use_case\":\"UC-A\"}");

        // Step 1: LLM calls record_outcome(RESOLVED).
        LlmResponse callRecord = LlmResponse.builder()
                .content("{\"user_message\":\"\",\"reasoning\":\"user is satisfied\","
                        + "\"tool_calls\":[{\"name\":\"record_outcome\","
                        + "\"arguments\":{\"outcome\":\"RESOLVED\"}}]}")
                .promptTokens(80).completionTokens(15).build();
        // Step 2: After tool result, LLM returns a friendly close message (no tool calls).
        LlmResponse closingMsg = LlmResponse.builder()
                .content("{\"user_message\":\"Glad I could help! Have a great day.\","
                        + "\"reasoning\":\"closing\",\"tool_calls\":[]}")
                .promptTokens(120).completionTokens(20).build();

        when(llmInvocation.invokeChat(anyString(), anyString(), anyString(), anyInt()))
                .thenReturn(callRecord)
                .thenReturn(closingMsg);

        when(actionParser.parse(callRecord.getContent())).thenReturn(ParsedAction.builder()
                .toolCalls(List.of(ToolCall.builder()
                        .name("record_outcome")
                        .arguments(Map.of("outcome", "RESOLVED"))
                        .build()))
                .userMessage("")
                .reasoning("user is satisfied")
                .build());
        when(actionParser.parse(closingMsg.getContent())).thenReturn(ParsedAction.builder()
                .toolCalls(List.of())
                .userMessage("Glad I could help! Have a great day.")
                .reasoning("closing")
                .build());

        when(toolDispatcher.validateAgainstPlan(any(), eq("record_outcome")))
                .thenReturn(ToolResult.ok(null));
        when(toolDispatcher.dispatch(eq("record_outcome"), any(), any()))
                .thenReturn(ToolResult.ok(Map.of(
                        "session_id", "sess-confirm-1",
                        "outcome", "RESOLVED",
                        "use_case_id", "UC-A")));

        // ── Act ─────────────────────────────────────────────────────
        ControlKernel.KernelResult result =
                controlKernel.processMessage(session, "thanks, that helped");

        // ── Assert ──────────────────────────────────────────────────
        // 1. Phase transitioned CONFIRM → CLOSE
        assertEquals("CLOSE", session.getCurrentPhase(),
                "CONFIRM FINAL_ANSWER (after record_outcome) should transition to CLOSE");

        // 2. Closing reply was returned to the user
        assertNotNull(result.responseText());
        assertTrue(result.responseText().toLowerCase().contains("glad"),
                "Bot response should be the closing message: " + result.responseText());

        // 3. record_outcome was actually dispatched
        verify(toolDispatcher, times(1))
                .dispatch(eq("record_outcome"), any(), any());

        // 4. Session-closed observability state set on the session
        assertEquals("CLOSED", session.getHandlingState(),
                "CLOSE phase should set handlingState=CLOSED");
        assertEquals("resolved", session.getContainmentOutcome(),
                "CLOSE phase should default containmentOutcome to 'resolved'");

        // 5. SESSION_CLOSED event was emitted by ControlKernel
        verify(eventEmitter).emitSessionClosed(eq("sess-confirm-1"), eq("resolved"));

        // 6. shouldEndChat is true (CLOSE ends the chat)
        assertTrue(result.shouldEndChat(),
                "CLOSE phase result should end the chat");
    }
}
