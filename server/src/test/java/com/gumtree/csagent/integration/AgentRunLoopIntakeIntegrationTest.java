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
 * D16.C end-to-end integration test for the INTAKE migration.
 *
 * <p>Simulates a UC-H (Ad Removal Appeal) intake flow through the
 * {@link AgentRunLoop} with feature flag {@code RESOLVE_INTAKE} enabled:
 *
 * <ol>
 *   <li>Turn 1 — user says "I want to appeal". The (mocked) LLM returns a
 *       clarification ask (no tool calls). The loop returns FINAL_ANSWER, but
 *       {@code PhaseEvaluator.interpretRunResult} maps it to RESOLVE
 *       (clarification) for INTAKE plans.</li>
 *   <li>Turn 2 — user provides ad and reason. The LLM returns
 *       tool_calls=[create_case_controlled, request_handover]. Both tools are
 *       dispatched; loop terminates with ESCALATE; phase transitions to
 *       ESCALATE.</li>
 * </ol>
 *
 * <p>Key contract checks:
 * <ul>
 *   <li>{@code search_knowledge} is NEVER dispatched in either turn (the INTAKE
 *       plan does not include it in {@code allowedTools}, and even if the LLM
 *       attempted it, {@code ToolDispatcher.validateAgainstPlan} would reject
 *       it).</li>
 *   <li>{@code create_case_controlled} is dispatched on the escalation turn.</li>
 *   <li>{@code request_handover} is dispatched on the escalation turn.</li>
 *   <li>The persisted {@code bot_turns.tool_calls} JSONB contains both tools
 *       on the escalation turn.</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class AgentRunLoopIntakeIntegrationTest {

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
        // Real PhaseEvaluator (so .plan() is exercised end-to-end)
        phaseEvaluator = new PhaseEvaluator(
                useCaseRegistry, knowledgeSearchService, scriptLibrary,
                llmInvocation, contextProjectionBuilder, actionParser,
                objectMapper, createCaseTool, eventEmitter, toolDispatcher);

        // Real AgentRunLoop
        agentRunLoop = new AgentRunLoopImpl(
                llmInvocation, toolDispatcher, contextProjectionBuilder, actionParser, objectMapper);

        // Feature flag: enable RESOLVE_INTAKE
        AgentRunLoopProperties props = new AgentRunLoopProperties();
        props.setEnabledPhases(List.of("RESOLVE_FAQ", "RESOLVE_INTAKE"));

        controlKernel = new ControlKernel(
                turnRepository, eventRepository, budgetChecker, driftDetector,
                phaseEvaluator, controlPolicy, objectMapper, createCaseTool,
                eventEmitter, contextProjectionBuilder, props, agentRunLoop,
                new EscalationReasonResolver());
    }

    private BotSession buildIntakeSession() {
        BotSession session = new BotSession();
        session.setSessionId("sess-uc-h-1");
        session.setCurrentPhase("RESOLVE");
        session.setActiveUseCase("UC-H");
        session.setTotalBotTurns(0);
        session.setFormContext(
                "{\"email\":\"alice@example.com\","
                        + "\"description\":\"my ad was removed unfairly\"}");
        return session;
    }

    private void mockCommonStubs() {
        when(budgetChecker.checkBudgets(any())).thenReturn(Optional.empty());
        when(driftDetector.detect(any(), anyString())).thenReturn(
                DriftResult.builder().type(DriftResult.DriftType.NONE).build());
        when(turnRepository.findBySessionIdOrderByTurnIndex(anyString()))
                .thenReturn(List.of());
        when(useCaseRegistry.getUseCase("UC-H")).thenReturn(
                new com.gumtree.csagent.service.runtime.UseCaseRegistryService.UseCaseDefinition(
                        "UC-H", "Ad Removal Appeal",
                        List.of("Ad Support"), "MEDIUM", false, "INTAKE"));
        when(contextProjectionBuilder.build(any(), any(), any(), anyString(), any()))
                .thenReturn("{\"phase\":\"RESOLVE\",\"use_case\":\"UC-H\"}");
    }

    @Test
    void ucH_endToEnd_collectsIntakeAndEscalatesWithCase() {
        // ── Arrange ─────────────────────────────────────────────────
        BotSession session = buildIntakeSession();
        mockCommonStubs();

        // INTAKE FINAL_ANSWER stays in RESOLVE (clarification); no transition needed.
        // Second turn: RESOLVE -> ESCALATE.
        when(controlPolicy.isValidTransition("RESOLVE", "ESCALATE")).thenReturn(true);

        // Turn 1: clarification (no tool calls)
        LlmResponse turn1Resp = LlmResponse.builder()
                .content("{\"user_message\":\"Which ad was removed and what's the reason given?\","
                        + "\"reasoning\":\"need details\",\"tool_calls\":[]}")
                .promptTokens(100).completionTokens(20).build();

        // Turn 2: tool_calls = [create_case_controlled, request_handover].
        // Sprint 7 §I2 — request_handover(intake_complete_for_uc_X) must
        // carry the collected intake_fields so the runtime intake-complete
        // guard accepts it (otherwise the guard downgrades the call).
        LlmResponse turn2Resp = LlmResponse.builder()
                .content("{\"user_message\":\"Thanks. I'm passing this to the Ad Support team.\","
                        + "\"reasoning\":\"intake complete; escalate with case\","
                        + "\"tool_calls\":["
                        + "{\"name\":\"create_case_controlled\","
                        + "\"arguments\":{\"subject\":\"Ad Support - Appeal\",\"description\":\"ad AD-9 removed\"}},"
                        + "{\"name\":\"request_handover\","
                        + "\"arguments\":{\"escalation_reason\":\"intake_complete_for_uc_h\","
                        + "\"intake_fields\":{\"ad_id_or_listing_url\":\"AD-9\","
                        + "\"registered_email\":\"alice@example.com\","
                        + "\"stated_reason_or_context\":\"ad AD-9 removed; reason: spam flag\"}}}"
                        + "]}")
                .promptTokens(180).completionTokens(40).build();

        when(llmInvocation.invokeChat(anyString(), anyString(), anyString(), anyInt()))
                .thenReturn(turn1Resp)
                .thenReturn(turn2Resp);

        when(actionParser.parse(turn1Resp.getContent())).thenReturn(ParsedAction.builder()
                .toolCalls(List.of())
                .userMessage("Which ad was removed and what's the reason given?")
                .reasoning("need details")
                .build());
        when(actionParser.parse(turn2Resp.getContent())).thenReturn(ParsedAction.builder()
                .toolCalls(List.of(
                        ToolCall.builder()
                                .name("create_case_controlled")
                                .arguments(Map.of(
                                        "subject", "Ad Support - Appeal",
                                        "description", "ad AD-9 removed"))
                                .build(),
                        ToolCall.builder()
                                .name("request_handover")
                                .arguments(Map.of(
                                        "escalation_reason", "intake_complete_for_uc_h",
                                        "intake_fields", Map.of(
                                                "ad_id_or_listing_url", "AD-9",
                                                "registered_email", "alice@example.com",
                                                "stated_reason_or_context",
                                                "ad AD-9 removed; reason: spam flag")))
                                .build()))
                .userMessage("Thanks. I'm passing this to the Ad Support team.")
                .reasoning("intake complete; escalate with case")
                .build());

        // ToolDispatcher: validate + dispatch for the two intake tools
        when(toolDispatcher.validateAgainstPlan(any(), eq("create_case_controlled")))
                .thenReturn(ToolResult.ok(null));
        when(toolDispatcher.validateAgainstPlan(any(), eq("request_handover")))
                .thenReturn(ToolResult.ok(null));
        when(toolDispatcher.dispatch(eq("create_case_controlled"), any(), any()))
                .thenReturn(ToolResult.ok(Map.of(
                        "case_id", "CASE-7777",
                        "status", "created")));
        when(toolDispatcher.dispatch(eq("request_handover"), any(), any()))
                .thenReturn(ToolResult.ok(Map.of("status", "queued")));

        // ── Act: Turn 1 ─────────────────────────────────────────────
        ControlKernel.KernelResult turn1 =
                controlKernel.processMessage(session, "I want to appeal");

        // ── Assert: Turn 1 ──────────────────────────────────────────
        // Stays in RESOLVE; clarification is sent verbatim.
        assertEquals("RESOLVE", session.getCurrentPhase(),
                "INTAKE clarification turn must keep session in RESOLVE");
        assertNotNull(turn1.responseText());
        assertTrue(turn1.responseText().contains("Which ad"),
                "Turn 1 response should be the clarification question");
        assertFalse(turn1.shouldEndChat(),
                "Clarification turn must NOT end the chat");

        // search_knowledge must NEVER have been dispatched.
        verify(toolDispatcher, never()).dispatch(eq("search_knowledge"), any(), any());

        // ── Act: Turn 2 ─────────────────────────────────────────────
        ControlKernel.KernelResult turn2 =
                controlKernel.processMessage(session, "ad AD-9 was removed; reason: spam flag");

        // ── Assert: Turn 2 ──────────────────────────────────────────
        // Phase transitioned to ESCALATE
        assertEquals("ESCALATE", session.getCurrentPhase(),
                "After tool_calls=[create_case_controlled, request_handover], session must escalate");
        assertTrue(turn2.shouldEndChat(),
                "Escalation must end the chat");

        // search_knowledge must STILL never have been dispatched
        verify(toolDispatcher, never()).dispatch(eq("search_knowledge"), any(), any());
        // create_case_controlled WAS dispatched
        verify(toolDispatcher, times(1))
                .dispatch(eq("create_case_controlled"), any(), any());
        // request_handover WAS dispatched
        verify(toolDispatcher, times(1))
                .dispatch(eq("request_handover"), any(), any());

        // The persisted BotTurn JSONB contains both tools.
        ArgumentCaptor<BotTurn> turnCaptor = ArgumentCaptor.forClass(BotTurn.class);
        verify(turnRepository, times(2)).save(turnCaptor.capture());
        List<BotTurn> savedTurns = turnCaptor.getAllValues();
        // Latest turn is the escalation turn
        BotTurn escalationTurn = savedTurns.get(savedTurns.size() - 1);
        assertNotNull(escalationTurn.getToolCalls());
        assertTrue(escalationTurn.getToolCalls().contains("create_case_controlled"),
                "Escalation turn tool_calls JSONB must include create_case_controlled: "
                        + escalationTurn.getToolCalls());
        assertTrue(escalationTurn.getToolCalls().contains("request_handover"),
                "Escalation turn tool_calls JSONB must include request_handover: "
                        + escalationTurn.getToolCalls());

        // Escalation reason carried through
        assertEquals("intake_complete_for_uc_h", session.getEscalationReason());
        assertEquals("QUEUE_TO_HUMAN", session.getHandlingState());
    }

    @Test
    void ucH_searchKnowledgeAttempt_isRejectedByValidateAgainstPlan() {
        // ── Arrange ─────────────────────────────────────────────────
        // Verify the INTAKE plan rejects search_knowledge if the LLM (mis)attempts it.
        BotSession session = buildIntakeSession();
        mockCommonStubs();

        when(controlPolicy.isValidTransition("RESOLVE", "ESCALATE")).thenReturn(true);

        // Turn 1: LLM attempts search_knowledge (which is NOT in INTAKE plan.allowedTools)
        // followed by request_handover. The first should be rejected; the second proceeds.
        // Sprint 7 §I2: include intake_fields so the intake-complete guard
        // accepts the handover; this test exercises the tool-whitelist
        // rejection, not the intake-complete guard.
        LlmResponse turn1Resp = LlmResponse.builder()
                .content("{\"user_message\":\"Let me check.\","
                        + "\"tool_calls\":["
                        + "{\"name\":\"search_knowledge\",\"arguments\":{\"query\":\"appeal\"}},"
                        + "{\"name\":\"request_handover\","
                        + "\"arguments\":{\"escalation_reason\":\"intake_complete_for_uc_h\","
                        + "\"intake_fields\":{\"ad_id_or_listing_url\":\"AD-9\","
                        + "\"registered_email\":\"alice@example.com\","
                        + "\"stated_reason_or_context\":\"appeal\"}}}"
                        + "]}")
                .promptTokens(100).completionTokens(20).build();

        when(llmInvocation.invokeChat(anyString(), anyString(), anyString(), anyInt()))
                .thenReturn(turn1Resp);

        when(actionParser.parse(turn1Resp.getContent())).thenReturn(ParsedAction.builder()
                .toolCalls(List.of(
                        ToolCall.builder()
                                .name("search_knowledge")
                                .arguments(Map.of("query", "appeal"))
                                .build(),
                        ToolCall.builder()
                                .name("request_handover")
                                .arguments(Map.of(
                                        "escalation_reason", "intake_complete_for_uc_h",
                                        "intake_fields", Map.of(
                                                "ad_id_or_listing_url", "AD-9",
                                                "registered_email", "alice@example.com",
                                                "stated_reason_or_context", "appeal")))
                                .build()))
                .userMessage("Let me check.")
                .build());

        // search_knowledge: validateAgainstPlan returns error => loop must NOT dispatch
        when(toolDispatcher.validateAgainstPlan(any(), eq("search_knowledge")))
                .thenReturn(ToolResult.error("tool_not_in_plan: search_knowledge"));
        // request_handover: validateAgainstPlan succeeds and dispatch returns ok
        when(toolDispatcher.validateAgainstPlan(any(), eq("request_handover")))
                .thenReturn(ToolResult.ok(null));
        when(toolDispatcher.dispatch(eq("request_handover"), any(), any()))
                .thenReturn(ToolResult.ok(Map.of("status", "queued")));

        // ── Act ─────────────────────────────────────────────────────
        ControlKernel.KernelResult result =
                controlKernel.processMessage(session, "I want to appeal");

        // ── Assert ──────────────────────────────────────────────────
        // search_knowledge was REJECTED — never dispatched
        verify(toolDispatcher, never()).dispatch(eq("search_knowledge"), any(), any());
        // request_handover was dispatched (since handover short-circuits the loop,
        // we expect exactly one invocation here)
        verify(toolDispatcher, times(1))
                .dispatch(eq("request_handover"), any(), any());

        // Session escalated
        assertEquals("ESCALATE", session.getCurrentPhase());
        assertTrue(result.shouldEndChat());

        // Persisted turn captures the rejection in tool_calls JSONB.
        ArgumentCaptor<BotTurn> turnCaptor = ArgumentCaptor.forClass(BotTurn.class);
        verify(turnRepository).save(turnCaptor.capture());
        BotTurn savedTurn = turnCaptor.getValue();
        assertNotNull(savedTurn.getToolCalls());
        assertTrue(savedTurn.getToolCalls().contains("search_knowledge"),
                "Rejected tool should still appear in tool_calls JSONB for trace replay: "
                        + savedTurn.getToolCalls());
        assertTrue(savedTurn.getToolCalls().contains("tool_not_in_plan"),
                "Rejection reason should be recorded: " + savedTurn.getToolCalls());
    }
}
