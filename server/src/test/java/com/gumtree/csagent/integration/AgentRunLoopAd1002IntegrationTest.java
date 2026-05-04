package com.gumtree.csagent.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gumtree.csagent.config.AgentRunLoopProperties;
import com.gumtree.csagent.model.BotSession;
import com.gumtree.csagent.model.BotTurn;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * D16.B end-to-end integration test (acceptance criterion).
 *
 * <p>Simulates the AD-1002 scenario:
 * <ol>
 *   <li>Session is in {@code RESOLVE} phase with active UC=UC-A and form_context
 *       containing email + ad_id.</li>
 *   <li>User types "AD-1002".</li>
 *   <li>The LLM (mocked) requests {@code get_customer_context} on the first
 *       loop iteration.</li>
 *   <li>The {@link AgentRunLoop} dispatches the tool, accumulates the result,
 *       and re-invokes the LLM.</li>
 *   <li>The second LLM response contains the grounded final answer.</li>
 *   <li>{@link ControlKernel#processMessage} returns that answer in a single
 *       HTTP-equivalent call.</li>
 *   <li>A {@link BotTurn} is persisted with the grounded response and tool_calls
 *       JSONB containing one {@code get_customer_context} entry.</li>
 * </ol>
 *
 * <p>This test uses real {@link AgentRunLoopImpl} and the real {@link PhaseEvaluator}
 * RESOLVE/FAQ planning branch; only LLM, projection, parser, and persistence
 * are mocked. It is the closest possible representation of the in-process flow
 * without requiring Postgres, pgvector, and a live LLM.
 */
@ExtendWith(MockitoExtension.class)
class AgentRunLoopAd1002IntegrationTest {

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
    private PhaseEvaluator phaseEvaluator;
    private AgentRunLoop agentRunLoop;

    @BeforeEach
    void setUp() {
        // Real PhaseEvaluator (so .plan() is exercised)
        phaseEvaluator = new PhaseEvaluator(
                useCaseRegistry, knowledgeSearchService, scriptLibrary,
                llmInvocation, contextProjectionBuilder, actionParser,
                objectMapper, createCaseTool, eventEmitter, toolDispatcher);

        // Real AgentRunLoop
        agentRunLoop = new AgentRunLoopImpl(
                llmInvocation, toolDispatcher, contextProjectionBuilder, actionParser);

        // Feature flag: enable RESOLVE_FAQ
        AgentRunLoopProperties props = new AgentRunLoopProperties();
        props.setEnabledPhases(List.of("RESOLVE_FAQ"));

        controlKernel = new ControlKernel(
                turnRepository, eventRepository, budgetChecker, driftDetector,
                phaseEvaluator, controlPolicy, objectMapper, createCaseTool,
                eventEmitter, contextProjectionBuilder, props, agentRunLoop,
                new EscalationReasonResolver());
    }

    @Test
    void ad1002_endToEnd_returnsGroundedAnswerInSingleHttpResponse() {
        // ── Arrange ─────────────────────────────────────────────────
        BotSession session = new BotSession();
        session.setSessionId("sess-ad1002");
        session.setCurrentPhase("RESOLVE");
        session.setActiveUseCase("UC-A");
        session.setTotalBotTurns(0);
        session.setFormContext(
                "{\"email\":\"alice@example.com\",\"ad_id\":\"AD-1002\","
                        + "\"description\":\"My ad got removed\"}");

        when(budgetChecker.checkBudgets(any())).thenReturn(Optional.empty());
        when(driftDetector.detect(any(), anyString())).thenReturn(
                DriftResult.builder().type(DriftResult.DriftType.NONE).build());
        when(turnRepository.findBySessionIdOrderByTurnIndex(anyString()))
                .thenReturn(List.of());
        when(useCaseRegistry.getUseCase("UC-A")).thenReturn(
                new com.gumtree.csagent.service.runtime.UseCaseRegistryService.UseCaseDefinition(
                        "UC-A", "Ad Status & Visibility",
                        List.of("Ad Support"), "LOW", true, "FAQ"));
        when(controlPolicy.isValidTransition("RESOLVE", "CONFIRM")).thenReturn(true);

        // Projection builder — return any non-null JSON; loop just passes it through
        when(contextProjectionBuilder.build(any(), any(), any(), anyString(), any()))
                .thenReturn("{\"phase\":\"RESOLVE\",\"use_case\":\"UC-A\"}");

        // First LLM call requests get_customer_context.
        // Second LLM call returns the grounded final answer.
        LlmResponse llmCall1 = LlmResponse.builder()
                .content("{\"user_message\":\"\",\"reasoning\":\"need account context\","
                        + "\"tool_calls\":[{\"name\":\"get_customer_context\","
                        + "\"arguments\":{\"ad_id\":\"AD-1002\",\"email\":\"alice@example.com\"}}]}")
                .promptTokens(100).completionTokens(20).build();
        LlmResponse llmCall2 = LlmResponse.builder()
                .content("{\"user_message\":\"Your ad AD-1002 was removed because it violated "
                        + "our category guidelines. You can edit and repost it.\","
                        + "\"reasoning\":\"grounded in moderation_review\","
                        + "\"tool_calls\":[]}")
                .promptTokens(180).completionTokens(40).build();
        when(llmInvocation.invokeChat(anyString(), anyString(), anyString(), anyInt()))
                .thenReturn(llmCall1)
                .thenReturn(llmCall2);

        // ActionParser delegates to mock for both responses
        when(actionParser.parse(llmCall1.getContent())).thenReturn(ParsedAction.builder()
                .toolCalls(List.of(ToolCall.builder()
                        .name("get_customer_context")
                        .arguments(Map.of("ad_id", "AD-1002", "email", "alice@example.com"))
                        .build()))
                .userMessage("")
                .reasoning("need account context")
                .build());
        when(actionParser.parse(llmCall2.getContent())).thenReturn(ParsedAction.builder()
                .toolCalls(List.of())
                .userMessage("Your ad AD-1002 was removed because it violated "
                        + "our category guidelines. You can edit and repost it.")
                .reasoning("grounded in moderation_review")
                .build());

        // ToolDispatcher: validate then execute
        when(toolDispatcher.validateAgainstPlan(any(), eq("get_customer_context")))
                .thenReturn(ToolResult.ok(null));
        when(toolDispatcher.dispatch(eq("get_customer_context"), any(), any()))
                .thenReturn(ToolResult.ok(Map.of(
                        "ad_id", "AD-1002",
                        "status", "removed",
                        "moderation_review", Map.of("reason", "category_violation"))));

        // ── Act ─────────────────────────────────────────────────────
        ControlKernel.KernelResult result = controlKernel.processMessage(session, "AD-1002");

        // ── Assert ──────────────────────────────────────────────────

        // 1. The bot returned the GROUNDED answer (not the placeholder).
        assertNotNull(result.responseText());
        assertTrue(result.responseText().contains("AD-1002"),
                "Bot response should reference the ad: " + result.responseText());
        assertTrue(result.responseText().contains("removed"),
                "Bot response should reference the moderation outcome: " + result.responseText());
        assertFalse(result.responseText().contains("looking into this"),
                "Bot must not return the placeholder; should be grounded: " + result.responseText());

        // 2. The LLM was invoked TWICE in this single turn (loop iterated).
        verify(llmInvocation, times(2))
                .invokeChat(anyString(), anyString(), anyString(), anyInt());

        // 3. get_customer_context was actually executed (not just parsed).
        verify(toolDispatcher, times(1))
                .dispatch(eq("get_customer_context"), any(), any());

        // 4. A BotTurn was persisted with tool_calls JSONB containing
        //    one get_customer_context entry, and bot_response is the grounded answer.
        ArgumentCaptor<BotTurn> turnCaptor = ArgumentCaptor.forClass(BotTurn.class);
        verify(turnRepository).save(turnCaptor.capture());
        BotTurn savedTurn = turnCaptor.getValue();
        assertEquals(result.responseText(), savedTurn.getBotResponse());
        assertNotNull(savedTurn.getToolCalls(),
                "Saved turn must record the tool_calls performed by the loop");
        assertTrue(savedTurn.getToolCalls().contains("get_customer_context"),
                "tool_calls JSONB must contain the get_customer_context invocation: "
                        + savedTurn.getToolCalls());

        // 5. Phase transitioned RESOLVE -> CONFIRM (FINAL_ANSWER outcome)
        assertEquals("CONFIRM", session.getCurrentPhase());

        // 6. Single HTTP-equivalent response: shouldEndChat is false (not escalation)
        assertFalse(result.shouldEndChat());
    }
}
