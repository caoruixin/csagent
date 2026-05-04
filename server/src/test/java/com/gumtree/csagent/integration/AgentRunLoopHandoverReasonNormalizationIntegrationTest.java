package com.gumtree.csagent.integration;

import com.fasterxml.jackson.databind.JsonNode;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Sprint 2026-05-04 §B0: persisted {@code request_handover} arguments
 * must agree with the resolved canonical {@code session.escalationReason}.
 *
 * <p>The {@link AgentRunLoop} dispatches {@code request_handover} with the
 * arguments the LLM emitted, and {@link ControlKernel#recordRunResult} used
 * to persist them verbatim. That let a non-canonical literal
 * ({@code user_requested_escalation}) or a lower-priority reason
 * ({@code faq_miss_threshold_exceeded} on top of an already-set
 * {@code user_distress}) leak into the trace and disagree with both the
 * session and the handover payload, re-opening the
 * {@code L1:escalation_reason_consistency} failure mode for the LLM path.
 *
 * <p>This test pins the rewrite: regardless of what the LLM emits, the
 * persisted {@code tool_calls.request_handover.arguments.escalation_reason}
 * matches the resolver-driven {@code session.escalationReason}.
 */
@ExtendWith(MockitoExtension.class)
class AgentRunLoopHandoverReasonNormalizationIntegrationTest {

    @Mock private BotTurnRepository turnRepository;
    @Mock private BotEventRepository eventRepository;
    @Mock private BudgetChecker budgetChecker;
    @Mock private DriftDetector driftDetector;
    @Mock private ControlPolicyService controlPolicy;
    @Mock private CreateCaseControlledTool createCaseTool;
    @Mock private EventEmitter eventEmitter;
    @Mock private ContextProjectionBuilder contextProjectionBuilder;
    @Mock private com.gumtree.csagent.service.runtime.LlmInvocationService llmInvocation;
    @Mock private ToolDispatcher toolDispatcher;
    @Mock private ActionParser actionParser;
    @Mock private com.gumtree.csagent.service.runtime.UseCaseRegistryService useCaseRegistry;
    @Mock private com.gumtree.csagent.service.knowledge.KnowledgeSearchService knowledgeSearchService;
    @Mock private com.gumtree.csagent.service.guardrails.ScriptLibraryService scriptLibrary;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final EscalationReasonResolver escalationResolver = new EscalationReasonResolver();

    private ControlKernel controlKernel;

    @BeforeEach
    void setUp() {
        PhaseEvaluator phaseEvaluator = new PhaseEvaluator(
                useCaseRegistry, knowledgeSearchService, scriptLibrary,
                llmInvocation, contextProjectionBuilder, actionParser,
                objectMapper, createCaseTool, eventEmitter, toolDispatcher);

        AgentRunLoop agentRunLoop = new AgentRunLoopImpl(
                llmInvocation, toolDispatcher, contextProjectionBuilder, actionParser);

        AgentRunLoopProperties props = new AgentRunLoopProperties();
        props.setEnabledPhases(List.of("RESOLVE_FAQ", "RESOLVE_INTAKE"));

        controlKernel = new ControlKernel(
                turnRepository, eventRepository, budgetChecker, driftDetector,
                phaseEvaluator, controlPolicy, objectMapper, createCaseTool,
                eventEmitter, contextProjectionBuilder, props, agentRunLoop,
                escalationResolver);
    }

    private BotSession buildFaqSession() {
        BotSession session = new BotSession();
        session.setSessionId("sess-handover-norm-1");
        session.setCurrentPhase("RESOLVE");
        session.setActiveUseCase("UC-C");
        session.setTotalBotTurns(0);
        session.setFormContext(
                "{\"email\":\"alice@example.com\","
                        + "\"description\":\"I am not receiving notifications\"}");
        return session;
    }

    private void mockCommonStubs() {
        when(budgetChecker.checkBudgets(any())).thenReturn(Optional.empty());
        when(driftDetector.detect(any(), anyString())).thenReturn(
                DriftResult.builder().type(DriftResult.DriftType.NONE).build());
        when(turnRepository.findBySessionIdOrderByTurnIndex(anyString()))
                .thenReturn(List.of());
        when(useCaseRegistry.getUseCase("UC-C")).thenReturn(
                new com.gumtree.csagent.service.runtime.UseCaseRegistryService.UseCaseDefinition(
                        "UC-C", "Messages & Replies",
                        List.of("Replies or Messaging"), "LOW", true, "FAQ"));
        when(contextProjectionBuilder.build(any(), any(), any(), anyString(), any()))
                .thenReturn("{\"phase\":\"RESOLVE\",\"use_case\":\"UC-C\"}");
        when(controlPolicy.isValidTransition("RESOLVE", "ESCALATE")).thenReturn(true);
    }

    @Test
    void llmEmitsNonCanonicalReason_persistedTraceIsCanonical() throws Exception {
        // ── Arrange: session already has user_distress on it (Sprint §B1
        // detector path). The LLM then emits a request_handover with the
        // legacy literal "user_requested_escalation". The resolver leaves
        // the session reason as user_distress (priority 2 < 1 only for
        // the strictly higher-priority "user_requested"; legacy literal
        // canonicalises to "user_requested" which would actually upgrade —
        // so to demonstrate the rewrite we use a lower-priority literal
        // that maps to faq_miss_threshold_exceeded equivalent: a raw
        // "service_degraded" literal).
        BotSession session = buildFaqSession();
        session.setEscalationReason("user_distress"); // Sprint §B1 prerequisite
        mockCommonStubs();

        LlmResponse turnResp = LlmResponse.builder()
                .content("{\"user_message\":\"Connecting you to a specialist.\","
                        + "\"tool_calls\":[{\"name\":\"request_handover\","
                        + "\"arguments\":{\"escalation_reason\":\"faq_miss_threshold_exceeded\"}}]}")
                .promptTokens(50).completionTokens(20).build();
        when(llmInvocation.invokeChat(anyString(), anyString(), anyString(), anyInt()))
                .thenReturn(turnResp);
        when(actionParser.parse(turnResp.getContent())).thenReturn(ParsedAction.builder()
                .toolCalls(List.of(ToolCall.builder()
                        .name("request_handover")
                        .arguments(Map.of("escalation_reason", "faq_miss_threshold_exceeded"))
                        .build()))
                .userMessage("Connecting you to a specialist.")
                .build());

        when(toolDispatcher.validateAgainstPlan(any(), eq("request_handover")))
                .thenReturn(ToolResult.ok(null));
        when(toolDispatcher.dispatch(eq("request_handover"), any(), any()))
                .thenReturn(ToolResult.ok(Map.of("status", "queued")));

        // ── Act ──────────────────────────────────────────────────
        controlKernel.processMessage(session, "yes please continue");

        // ── Assert: session reason preserved ─────────────────────
        // Resolver keeps user_distress (priority 2) over
        // faq_miss_threshold_exceeded (priority 41) — Sprint §B1 contract.
        assertEquals("user_distress", session.getEscalationReason(),
                "Session reason must remain user_distress; the resolver's "
                        + "precedence table forbids a budget reason from "
                        + "overwriting user_distress.");

        // ── Assert: persisted tool_call rewritten ────────────────
        ArgumentCaptor<BotTurn> turnCaptor = ArgumentCaptor.forClass(BotTurn.class);
        verify(turnRepository, times(1)).save(turnCaptor.capture());
        BotTurn savedTurn = turnCaptor.getValue();
        assertNotNull(savedTurn.getToolCalls(),
                "Tool calls JSONB must be persisted on escalation");

        JsonNode toolCalls = objectMapper.readTree(savedTurn.getToolCalls());
        assertTrue(toolCalls.isArray() && toolCalls.size() >= 1);
        JsonNode handover = null;
        for (JsonNode entry : toolCalls) {
            if ("request_handover".equals(entry.path("tool_name").asText())) {
                handover = entry;
                break;
            }
        }
        assertNotNull(handover, "request_handover entry must be present in trace");
        assertEquals("user_distress",
                handover.path("arguments").path("escalation_reason").asText(),
                "Persisted request_handover.arguments.escalation_reason must "
                        + "match resolved session reason (user_distress), not "
                        + "the lower-priority value the LLM emitted.");
    }

    @Test
    void llmEmitsLegacyLiteral_persistedTraceCanonicalises() throws Exception {
        // The LLM emits the legacy literal "user_requested_escalation".
        // The resolver canonicalises that to "user_requested". After
        // rewriting, the persisted tool_call must show the canonical
        // form, and session state must agree.
        BotSession session = buildFaqSession();
        // No prior reason — the legacy literal will be the only signal,
        // so canonicalisation drives the final value.
        mockCommonStubs();

        LlmResponse turnResp = LlmResponse.builder()
                .content("{\"user_message\":\"Connecting you to a human.\","
                        + "\"tool_calls\":[{\"name\":\"request_handover\","
                        + "\"arguments\":{\"escalation_reason\":\"user_requested_escalation\"}}]}")
                .promptTokens(50).completionTokens(20).build();
        when(llmInvocation.invokeChat(anyString(), anyString(), anyString(), anyInt()))
                .thenReturn(turnResp);
        when(actionParser.parse(turnResp.getContent())).thenReturn(ParsedAction.builder()
                .toolCalls(List.of(ToolCall.builder()
                        .name("request_handover")
                        .arguments(Map.of("escalation_reason", "user_requested_escalation"))
                        .build()))
                .userMessage("Connecting you to a human.")
                .build());
        when(toolDispatcher.validateAgainstPlan(any(), eq("request_handover")))
                .thenReturn(ToolResult.ok(null));
        when(toolDispatcher.dispatch(eq("request_handover"), any(), any()))
                .thenReturn(ToolResult.ok(Map.of("status", "queued")));

        controlKernel.processMessage(session, "please escalate");

        assertEquals("user_requested", session.getEscalationReason(),
                "Session reason must canonicalise to user_requested.");

        ArgumentCaptor<BotTurn> turnCaptor = ArgumentCaptor.forClass(BotTurn.class);
        verify(turnRepository, times(1)).save(turnCaptor.capture());
        BotTurn savedTurn = turnCaptor.getValue();
        JsonNode toolCalls = objectMapper.readTree(savedTurn.getToolCalls());
        JsonNode handover = null;
        for (JsonNode entry : toolCalls) {
            if ("request_handover".equals(entry.path("tool_name").asText())) {
                handover = entry;
                break;
            }
        }
        assertNotNull(handover);
        assertEquals("user_requested",
                handover.path("arguments").path("escalation_reason").asText(),
                "Persisted tool_call must carry canonical user_requested, "
                        + "not the legacy literal user_requested_escalation.");
    }
}
