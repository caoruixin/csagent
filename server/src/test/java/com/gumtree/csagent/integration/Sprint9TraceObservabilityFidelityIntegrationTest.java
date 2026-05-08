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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;
import org.mockito.invocation.InvocationOnMock;

/**
 * Sprint 9 §O2 — trace observability fidelity. The
 * {@code bot_turns.tool_calls} JSONB column must surface a bounded,
 * sanitized {@code result_data} (and a one-line {@code result_summary})
 * for every tool event so the Trace UI Result panels are not blank
 * when a real tool dispatch occurred.
 *
 * <p>This integration test pins:
 * <ul>
 *   <li>For a successful {@code search_knowledge → resolve_article} flow,
 *       both entries persist a non-empty {@code result_summary} and
 *       {@code result_data}; for {@code resolve_article} the article body
 *       is reduced to safe summary fields (no full description).</li>
 *   <li>For a failing {@code resolve_article} dispatch, the persisted
 *       entry carries {@code success=false}, the underlying
 *       {@code error_message}, and a {@code result_summary} that surfaces
 *       the error so the Trace UI Result panel is not blank.</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class Sprint9TraceObservabilityFidelityIntegrationTest {

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
                llmInvocation, toolDispatcher, contextProjectionBuilder, actionParser, objectMapper);

        AgentRunLoopProperties props = new AgentRunLoopProperties();
        props.setEnabledPhases(List.of("RESOLVE_FAQ"));

        controlKernel = new ControlKernel(
                turnRepository, eventRepository, budgetChecker, driftDetector,
                phaseEvaluator, controlPolicy, objectMapper, createCaseTool,
                eventEmitter, contextProjectionBuilder, props, agentRunLoop,
                escalationResolver);
    }

    private BotSession ucaSession() {
        BotSession session = new BotSession();
        session.setSessionId("sess-trace-fidelity");
        session.setCurrentPhase("RESOLVE");
        session.setActiveUseCase("UC-A");
        session.setTotalBotTurns(0);
        session.setFormTopicSubject("Ad Support");
        session.setFormContext("{\"description\":\"I can't see my advert\"}");
        return session;
    }

    private void mockCommonStubs() {
        when(budgetChecker.checkBudgets(any())).thenReturn(Optional.empty());
        when(driftDetector.detect(any(), anyString())).thenReturn(
                DriftResult.builder().type(DriftResult.DriftType.NONE).build());
        when(turnRepository.findBySessionIdOrderByTurnIndex(anyString()))
                .thenReturn(List.of());
        when(useCaseRegistry.getUseCase("UC-A")).thenReturn(
                new com.gumtree.csagent.service.runtime.UseCaseRegistryService.UseCaseDefinition(
                        "UC-A", "Ad Support",
                        List.of("Ad Support"), "LOW", true, "FAQ"));
        when(contextProjectionBuilder.build(any(), any(), any(), anyString(), any()))
                .thenReturn("{\"phase\":\"RESOLVE\",\"use_case\":\"UC-A\"}");
        // Sprint 11.1 — RESOLVE/FAQ FINAL_ANSWER without successful
        // record_outcome now stays in RESOLVE rather than transitioning
        // to CONFIRM, so isValidTransition may not be consulted for
        // every successful flow under the stricter contract. Keep the
        // permissive stub but mark it lenient.
        lenient().when(controlPolicy.isValidTransition(anyString(), anyString())).thenReturn(true);
    }

    @Test
    void successfulSearchAndResolveArticle_persistResultDataAndSummary() throws Exception {
        BotSession session = ucaSession();
        mockCommonStubs();

        // LLM step 0 → search_knowledge
        // LLM step 1 → resolve_article(source_id)
        // LLM step 2 → final answer
        when(llmInvocation.invokeChat(anyString(), anyString(), anyString(), anyInt()))
                .thenReturn(LlmResponse.builder().content("step0").build())
                .thenReturn(LlmResponse.builder().content("step1").build())
                .thenReturn(LlmResponse.builder().content("step2").build());

        when(actionParser.parse("step0")).thenReturn(ParsedAction.builder()
                .toolCalls(List.of(ToolCall.builder()
                        .name("search_knowledge")
                        .arguments(Map.of("query", "where is my advert"))
                        .build()))
                .userMessage("")
                .build());
        when(actionParser.parse("step1")).thenReturn(ParsedAction.builder()
                .toolCalls(List.of(ToolCall.builder()
                        .name("resolve_article")
                        .arguments(Map.of("source_id", "kb-1"))
                        .build()))
                .userMessage("")
                .build());
        when(actionParser.parse("step2")).thenReturn(ParsedAction.builder()
                .toolCalls(List.of())
                .userMessage("Your ad may be in moderation review. [kb-1]")
                .build());

        when(toolDispatcher.validateAgainstPlan(any(), eq("search_knowledge")))
                .thenReturn(ToolResult.ok(null));
        when(toolDispatcher.validateAgainstPlan(any(), eq("resolve_article")))
                .thenReturn(ToolResult.ok(null));

        Map<String, Object> hits = new LinkedHashMap<>();
        hits.put("faq_miss", false);
        hits.put("hits", List.of(Map.of(
                "source_id", "kb-1",
                "title", "Where is my advert?",
                "snippet", "Ads can be hidden during moderation",
                "score", 0.91)));
        when(toolDispatcher.dispatch(eq("search_knowledge"), any(), any()))
                .thenReturn(ToolResult.ok(hits));

        // resolve_article returns the FULL article shape (with description body).
        // Sprint 9 §O2 requires the persisted result_data for resolve_article
        // to be reduced to safe summary fields, NOT the full body.
        StringBuilder big = new StringBuilder();
        for (int i = 0; i < 4_000; i++) big.append('y');
        Map<String, Object> articlePayload = new LinkedHashMap<>();
        articlePayload.put("source_id", "kb-1");
        articlePayload.put("article_id", "kb-1");
        articlePayload.put("title", "Where is my advert?");
        articlePayload.put("source_url", "https://help.example/faq/where-is-my-ad");
        articlePayload.put("description", big.toString());
        when(toolDispatcher.dispatch(eq("resolve_article"), any(), any()))
                .thenReturn(ToolResult.ok(articlePayload));

        List<BotTurn> savedTurns = new ArrayList<>();
        when(turnRepository.save(any(BotTurn.class))).thenAnswer((InvocationOnMock inv) -> {
            BotTurn t = inv.getArgument(0);
            savedTurns.add(t);
            return t;
        });

        controlKernel.processMessage(session, "I can't see my advert");

        assertEquals(1, savedTurns.size());
        BotTurn savedTurn = savedTurns.get(0);
        JsonNode toolCalls = objectMapper.readTree(savedTurn.getToolCalls());
        assertTrue(toolCalls.isArray() && toolCalls.size() >= 2);

        JsonNode searchEntry = findEntry(toolCalls, "search_knowledge");
        JsonNode resolveEntry = findEntry(toolCalls, "resolve_article");
        assertNotNull(searchEntry);
        assertNotNull(resolveEntry);

        // search_knowledge: result_summary mentions hit count; result_data
        // present (bounded).
        assertTrue(searchEntry.path("success").asBoolean());
        assertTrue(searchEntry.has("result_summary"),
                "Sprint 9 §O2: every successful tool entry must carry a result_summary");
        assertTrue(searchEntry.path("result_summary").asText().contains("1 hit"));
        assertTrue(searchEntry.has("result_data"),
                "Sprint 9 §O2: every successful tool entry must carry a result_data field");

        // resolve_article: result_data reduced to safe fields, NO raw description.
        assertTrue(resolveEntry.has("result_data"));
        JsonNode resolveData = resolveEntry.path("result_data");
        assertEquals("kb-1", resolveData.path("source_id").asText());
        assertEquals("Where is my advert?", resolveData.path("title").asText());
        assertFalse(resolveData.has("description"),
                "resolve_article result_data must drop the raw description body");
        // Excerpt may be present but must be size-bounded.
        if (resolveData.has("excerpt")) {
            String excerpt = resolveData.path("excerpt").asText();
            assertTrue(excerpt.length() < 300,
                    "excerpt must be size-bounded; got length=" + excerpt.length());
        }
    }

    @Test
    void failedToolDispatch_persistsErrorMessageAndSummary() throws Exception {
        BotSession session = ucaSession();
        mockCommonStubs();

        // The LLM emits resolve_article twice; both fail.
        when(llmInvocation.invokeChat(anyString(), anyString(), anyString(), anyInt()))
                .thenReturn(LlmResponse.builder().content("step0").build())
                .thenReturn(LlmResponse.builder().content("step1").build())
                .thenReturn(LlmResponse.builder().content("step2").build())
                .thenReturn(LlmResponse.builder().content("step3").build());
        ParsedAction resolveCall = ParsedAction.builder()
                .toolCalls(List.of(ToolCall.builder()
                        .name("resolve_article")
                        .arguments(Map.of("source_id", "kb-missing"))
                        .build()))
                .userMessage("")
                .build();
        when(actionParser.parse(anyString())).thenReturn(resolveCall);

        when(toolDispatcher.validateAgainstPlan(any(), eq("resolve_article")))
                .thenReturn(ToolResult.ok(null));
        when(toolDispatcher.dispatch(eq("resolve_article"), any(), any()))
                .thenReturn(ToolResult.error("Article not found: kb-missing"));

        List<BotTurn> savedTurns = new ArrayList<>();
        when(turnRepository.save(any(BotTurn.class))).thenAnswer((InvocationOnMock inv) -> {
            BotTurn t = inv.getArgument(0);
            savedTurns.add(t);
            return t;
        });

        controlKernel.processMessage(session, "show me my advert");

        assertEquals(1, savedTurns.size());
        BotTurn savedTurn = savedTurns.get(0);
        JsonNode toolCalls = objectMapper.readTree(savedTurn.getToolCalls());
        // At least one resolve_article entry must appear with success=false.
        boolean foundFailedEntry = false;
        for (JsonNode entry : toolCalls) {
            if ("resolve_article".equals(entry.path("tool_name").asText())
                    && !entry.path("success").asBoolean()) {
                foundFailedEntry = true;
                assertEquals("Article not found: kb-missing",
                        entry.path("error_message").asText(),
                        "Sprint 9 §O2: failed tool entry must persist the underlying error_message");
                String summary = entry.path("result_summary").asText();
                assertTrue(summary.startsWith("error:"),
                        "Sprint 9 §O2: failed tool result_summary must surface the error so the "
                                + "Trace UI Result panel is not blank (was: " + summary + ")");
                assertTrue(summary.contains("Article not found"),
                        "failed tool result_summary should include the underlying error text");
            }
        }
        assertTrue(foundFailedEntry,
                "expected at least one failed resolve_article entry in the persisted trace");
    }

    @Test
    void failedToolDispatch_sanitizesEmailAndTokenInPersistedErrorMessage() throws Exception {
        // Sprint 9.1 closure — even when the underlying tool error string
        // contains email / phone / bearer-token / long-credential surfaces,
        // bot_turns.tool_calls.error_message and the inline result_summary
        // must redact those before persistence so secrets / sensitive PII
        // cannot leak via the trace.
        BotSession session = ucaSession();
        mockCommonStubs();

        when(llmInvocation.invokeChat(anyString(), anyString(), anyString(), anyInt()))
                .thenReturn(LlmResponse.builder().content("step0").build())
                .thenReturn(LlmResponse.builder().content("step1").build())
                .thenReturn(LlmResponse.builder().content("step2").build())
                .thenReturn(LlmResponse.builder().content("step3").build());
        ParsedAction handoverCall = ParsedAction.builder()
                .toolCalls(List.of(ToolCall.builder()
                        .name("request_handover")
                        .arguments(Map.of("escalation_reason", "user_requested"))
                        .build()))
                .userMessage("")
                .build();
        when(actionParser.parse(anyString())).thenReturn(handoverCall);

        when(toolDispatcher.validateAgainstPlan(any(), eq("request_handover")))
                .thenReturn(ToolResult.ok(null));
        // Salesforce-style failure that interpolates user PII + a bearer token
        // into the error string.
        String dirtyError =
                "salesforce_handover_failed: 401 for alice@example.com phone +44 20 7946 0958"
                        + " token Bearer abcdef0123456789ABCDEFGHabcdef01";
        when(toolDispatcher.dispatch(eq("request_handover"), any(), any()))
                .thenReturn(ToolResult.error(dirtyError));

        List<BotTurn> savedTurns = new ArrayList<>();
        when(turnRepository.save(any(BotTurn.class))).thenAnswer((InvocationOnMock inv) -> {
            BotTurn t = inv.getArgument(0);
            savedTurns.add(t);
            return t;
        });

        controlKernel.processMessage(session, "please escalate me");

        assertEquals(1, savedTurns.size());
        BotTurn savedTurn = savedTurns.get(0);
        JsonNode toolCalls = objectMapper.readTree(savedTurn.getToolCalls());

        boolean checkedAtLeastOne = false;
        for (JsonNode entry : toolCalls) {
            if (!"request_handover".equals(entry.path("tool_name").asText())) continue;
            if (entry.path("success").asBoolean()) continue;
            checkedAtLeastOne = true;

            String persistedErr = entry.path("error_message").asText();
            assertFalse(persistedErr.contains("alice@example.com"),
                    "Sprint 9.1: persisted error_message must redact email (was: " + persistedErr + ")");
            assertFalse(persistedErr.contains("20 7946 0958"),
                    "Sprint 9.1: persisted error_message must redact phone digits (was: "
                            + persistedErr + ")");
            assertFalse(persistedErr.contains("abcdef0123456789ABCDEFGHabcdef01"),
                    "Sprint 9.1: persisted error_message must redact long bearer token (was: "
                            + persistedErr + ")");
            assertTrue(persistedErr.contains("[REDACTED_EMAIL]"));
            assertTrue(persistedErr.contains("[REDACTED_BEARER]"));

            String summary = entry.path("result_summary").asText();
            assertFalse(summary.contains("alice@example.com"));
            assertFalse(summary.contains("abcdef0123456789ABCDEFGHabcdef01"));
            assertTrue(summary.startsWith("error:"),
                    "result_summary must still surface the failure prefix");
        }
        assertTrue(checkedAtLeastOne,
                "expected at least one failed request_handover entry to inspect");
    }

    private static JsonNode findEntry(JsonNode toolCalls, String name) {
        for (JsonNode entry : toolCalls) {
            if (name.equals(entry.path("tool_name").asText())) return entry;
        }
        return null;
    }
}
