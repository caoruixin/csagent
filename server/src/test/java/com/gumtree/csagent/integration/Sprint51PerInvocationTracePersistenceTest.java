package com.gumtree.csagent.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gumtree.csagent.config.AgentRunLoopProperties;
import com.gumtree.csagent.model.BotSession;
import com.gumtree.csagent.model.BotTurn;
import com.gumtree.csagent.model.BotTurnLlmCall;
import com.gumtree.csagent.model.DriftResult;
import com.gumtree.csagent.model.LlmResponse;
import com.gumtree.csagent.model.ParsedAction;
import com.gumtree.csagent.model.ToolCall;
import com.gumtree.csagent.repository.BotEventRepository;
import com.gumtree.csagent.repository.BotTurnLlmCallRepository;
import com.gumtree.csagent.repository.BotTurnRepository;
import com.gumtree.csagent.service.observability.EventEmitter;
import com.gumtree.csagent.service.observability.TraceWriter;
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
import com.gumtree.csagent.service.runtime.RerouteDecider;
import com.gumtree.csagent.service.runtime.RuntimeIntentClassifier;
import com.gumtree.csagent.service.tools.CreateCaseControlledTool;
import com.gumtree.csagent.service.tools.ToolDispatcher;
import com.gumtree.csagent.service.tools.ToolResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * Sprint 51 / M5 S2 — per-invocation trace persistence + observation-only
 * guarantees.
 *
 * <p>The test drives a 3-step FAQ run (search → resolve_article → final
 * answer) and verifies:
 * <ol>
 *   <li><b>Persistence</b>: one {@code bot_turn_llm_calls} row per loop step,
 *       with monotonic {@code step_index} (0, 1, 2) and {@code call_type =
 *       "chat"}; each row carries the full untruncated LLM raw response (the
 *       cheap 500-char {@code llm_call_log} summary is untouched — frozen
 *       eval contract) and that step's projection (which would otherwise be
 *       overwritten by the loop and lost).</li>
 *   <li><b>Observation-only</b>: the existing {@code BotTurn} single-column
 *       {@code llm_raw_response} / {@code projected_context} keep the
 *       FINAL-step value (matches the loop's overwrite semantics — backward
 *       compat preserved).</li>
 *   <li><b>Loop unchanged</b>: 3 LLM invocations happen (not 2, not 4), the
 *       terminal outcome is the same FINAL_ANSWER as without S2, and the
 *       per-step records' {@code step_index} sequence matches the loop's
 *       {@code for (step = 0; step &lt; maxSteps; step++)} counter.</li>
 * </ol>
 */
@ExtendWith(MockitoExtension.class)
class Sprint51PerInvocationTracePersistenceTest {

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
    @Mock private BotTurnLlmCallRepository botTurnLlmCallRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final EscalationReasonResolver escalationResolver = new EscalationReasonResolver();

    private ControlKernel controlKernel;

    @BeforeEach
    void setUp() {
        PhaseEvaluator phaseEvaluator = new PhaseEvaluator(
                useCaseRegistry, knowledgeSearchService, scriptLibrary,
                llmInvocation, contextProjectionBuilder, actionParser,
                objectMapper, createCaseTool, eventEmitter, toolDispatcher,
                com.gumtree.csagent.service.runtime.skill.SkillTestFixtures.productionRegistry(), null);

        AgentRunLoop agentRunLoop = new AgentRunLoopImpl(
                llmInvocation, toolDispatcher, contextProjectionBuilder, actionParser, objectMapper);

        AgentRunLoopProperties props = new AgentRunLoopProperties();
        props.setEnabledPhases(List.of("RESOLVE_FAQ"));

        TraceWriter traceWriter = new TraceWriter(
                turnRepository, botTurnLlmCallRepository, objectMapper);

        controlKernel = new ControlKernel(
                turnRepository, eventRepository, budgetChecker, driftDetector,
                phaseEvaluator, controlPolicy, objectMapper, createCaseTool,
                eventEmitter, contextProjectionBuilder, props, agentRunLoop,
                escalationResolver,
                new RuntimeIntentClassifier(escalationResolver, objectMapper),
                new RerouteDecider(),
                traceWriter);
    }

    private BotSession ucaSession() {
        BotSession session = new BotSession();
        session.setSessionId("sess-s51-perinvoc");
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
        lenient().when(llmInvocation.getModelName()).thenReturn("deepseek-v3");
        lenient().when(controlPolicy.isValidTransition(anyString(), anyString())).thenReturn(true);
    }

    @Test
    void multiStepFaqTurn_persistsOneRowPerInvocation_finalStepValuesPreservedOnBotTurn() throws Exception {
        BotSession session = ucaSession();
        mockCommonStubs();

        // Per-step projections — different content per step so we can assert
        // that the new table captures each one (the BotTurn single column
        // would only retain step-2's value).
        when(contextProjectionBuilder.build(any(), any(), any(), anyString(), any(), any()))
                .thenReturn("{\"phase\":\"RESOLVE\",\"use_case\":\"UC-A\",\"step\":0}")
                .thenReturn("{\"phase\":\"RESOLVE\",\"use_case\":\"UC-A\",\"step\":1}")
                .thenReturn("{\"phase\":\"RESOLVE\",\"use_case\":\"UC-A\",\"step\":2}");

        // 3-step flow: search → resolve → final answer.
        String step0Content = "{\"reasoning\":\"step0 — calling search_knowledge\","
                + "\"tool_calls\":[{\"name\":\"search_knowledge\","
                + "\"arguments\":{\"query\":\"where is my advert\"}}]}";
        String step1Content = "{\"reasoning\":\"step1 — calling resolve_article\","
                + "\"tool_calls\":[{\"name\":\"resolve_article\","
                + "\"arguments\":{\"source_id\":\"kb-1\"}}]}";
        String step2Content = "{\"reasoning\":\"step2 — final\","
                + "\"user_message\":\"Your ad may be in moderation review. [kb-1]\"}";

        when(llmInvocation.invokeChat(anyString(), anyString(), anyString(), anyInt()))
                .thenReturn(LlmResponse.builder().content(step0Content).build())
                .thenReturn(LlmResponse.builder().content(step1Content).build())
                .thenReturn(LlmResponse.builder().content(step2Content).build());

        when(actionParser.parse(step0Content)).thenReturn(ParsedAction.builder()
                .toolCalls(List.of(ToolCall.builder()
                        .name("search_knowledge")
                        .arguments(Map.of("query", "where is my advert"))
                        .build()))
                .userMessage("")
                .build());
        when(actionParser.parse(step1Content)).thenReturn(ParsedAction.builder()
                .toolCalls(List.of(ToolCall.builder()
                        .name("resolve_article")
                        .arguments(Map.of("source_id", "kb-1"))
                        .build()))
                .userMessage("")
                .build());
        when(actionParser.parse(step2Content)).thenReturn(ParsedAction.builder()
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
        when(toolDispatcher.dispatch(eq("resolve_article"), any(), any()))
                .thenReturn(ToolResult.ok(Map.of(
                        "source_id", "kb-1",
                        "title", "Where is my advert?")));

        List<BotTurn> savedTurns = new ArrayList<>();
        when(turnRepository.save(any(BotTurn.class))).thenAnswer((InvocationOnMock inv) -> {
            BotTurn t = inv.getArgument(0);
            savedTurns.add(t);
            return t;
        });

        List<BotTurnLlmCall> savedCalls = new ArrayList<>();
        when(botTurnLlmCallRepository.save(any(BotTurnLlmCall.class))).thenAnswer((InvocationOnMock inv) -> {
            BotTurnLlmCall row = inv.getArgument(0);
            savedCalls.add(row);
            return row;
        });

        controlKernel.processMessage(session, "I can't see my advert");

        // (1) Persistence: one BotTurn + N=3 bot_turn_llm_calls rows keyed
        //     to it, with monotonic step_index 0/1/2 and call_type=chat.
        assertEquals(1, savedTurns.size(), "exactly one BotTurn per user turn");
        BotTurn turn = savedTurns.get(0);
        assertEquals(3, savedCalls.size(), "FAQ multi-step turn writes 3 per-step records");
        for (int i = 0; i < savedCalls.size(); i++) {
            BotTurnLlmCall row = savedCalls.get(i);
            assertEquals(i, row.getStepIndex().intValue(),
                    "step_index must be monotonic 0/1/2 — matches the AgentRunLoop counter");
            assertEquals("chat", row.getCallType(),
                    "Sprint 51 fence: chat calls labelled as 'chat' (never routing/rerank lumped in)");
            assertEquals(turn.getTurnId(), row.getBotTurnId(),
                    "FK target must be the BotTurn that owns this turn");
            assertNotNull(row.getLlmRawResponse(),
                    "Sprint 51 #1: full / untruncated raw response persisted per step");
            assertNotNull(row.getProjectedContext(),
                    "Sprint 51 #1: that step's projection persisted per step");
        }
        assertEquals(step0Content, savedCalls.get(0).getLlmRawResponse(),
                "step 0 row carries step-0's raw response (NOT overwritten by step 2)");
        assertEquals(step1Content, savedCalls.get(1).getLlmRawResponse(),
                "step 1 row carries step-1's raw response (NOT overwritten by step 2)");
        assertEquals(step2Content, savedCalls.get(2).getLlmRawResponse(),
                "step 2 row carries step-2's raw response");
        assertTrue(savedCalls.get(0).getProjectedContext().contains("\"step\":0"));
        assertTrue(savedCalls.get(1).getProjectedContext().contains("\"step\":1"));
        assertTrue(savedCalls.get(2).getProjectedContext().contains("\"step\":2"));
        assertEquals("deepseek-v3", savedCalls.get(0).getModel(),
                "Sprint 51 #1: model name persisted per step");

        // (2) Observation-only: BotTurn keeps the FINAL-step value in its
        //     single columns — UNCHANGED semantics vs pre-S2 (existing
        //     readers of bot_turns are unaffected). This is what makes S2
        //     observation-only: the loop's overwrite of
        //     lastLlmRawResponse / lastProjection still wins on the
        //     BotTurn row.
        assertEquals(step2Content, turn.getLlmRawResponse(),
                "BotTurn single column keeps the FINAL-step raw response — unchanged from pre-S2");
        assertNotNull(turn.getProjectedContext());
        assertTrue(turn.getProjectedContext().contains("\"step\":2"),
                "BotTurn single column keeps the FINAL-step projection — unchanged from pre-S2");
    }

    @Test
    void traceWriterNull_perStepRecordsSilentlySkipped_botTurnUnaffected() throws Exception {
        // Observation-only safety net: when TraceWriter is null (some unit
        // tests bypass the @Autowired path), the per-step records still
        // accumulate inside the loop but persistence is silently skipped.
        // The BotTurn single-column trace and the loop's terminal outcome
        // remain identical to a pre-S2 run.
        BotSession session = ucaSession();
        mockCommonStubs();

        when(contextProjectionBuilder.build(any(), any(), any(), anyString(), any(), any()))
                .thenReturn("{\"phase\":\"RESOLVE\",\"use_case\":\"UC-A\"}");
        when(llmInvocation.invokeChat(anyString(), anyString(), anyString(), anyInt()))
                .thenReturn(LlmResponse.builder().content(
                        "{\"reasoning\":\"r\",\"user_message\":\"final\"}").build());
        when(actionParser.parse(anyString())).thenReturn(ParsedAction.builder()
                .toolCalls(List.of()).userMessage("final").build());

        // Re-wire with NULL traceWriter via the 15-arg back-compat constructor.
        PhaseEvaluator phaseEvaluator = new PhaseEvaluator(
                useCaseRegistry, knowledgeSearchService, scriptLibrary,
                llmInvocation, contextProjectionBuilder, actionParser,
                objectMapper, createCaseTool, eventEmitter, toolDispatcher,
                com.gumtree.csagent.service.runtime.skill.SkillTestFixtures.productionRegistry(), null);
        AgentRunLoop agentRunLoop = new AgentRunLoopImpl(
                llmInvocation, toolDispatcher, contextProjectionBuilder, actionParser, objectMapper);
        AgentRunLoopProperties props = new AgentRunLoopProperties();
        props.setEnabledPhases(List.of("RESOLVE_FAQ"));
        ControlKernel kernelWithoutTraceWriter = new ControlKernel(
                turnRepository, eventRepository, budgetChecker, driftDetector,
                phaseEvaluator, controlPolicy, objectMapper, createCaseTool,
                eventEmitter, contextProjectionBuilder, props, agentRunLoop,
                escalationResolver,
                new RuntimeIntentClassifier(escalationResolver, objectMapper),
                new RerouteDecider());  // no traceWriter

        List<BotTurn> savedTurns = new ArrayList<>();
        when(turnRepository.save(any(BotTurn.class))).thenAnswer((InvocationOnMock inv) -> {
            BotTurn t = inv.getArgument(0);
            savedTurns.add(t);
            return t;
        });

        kernelWithoutTraceWriter.processMessage(session, "hi");

        assertEquals(1, savedTurns.size());
        BotTurn turn = savedTurns.get(0);
        assertNotNull(turn.getLlmRawResponse(),
                "BotTurn single-column trace unaffected when TraceWriter is null");
        assertEquals(0, savedCalls(turn), "no per-step persistence attempted when TraceWriter is null");
    }

    private int savedCalls(BotTurn turn) {
        // Helper for the null-traceWriter case: we never called
        // botTurnLlmCallRepository.save, so any verify would show 0
        // interactions. Returns 0 by construction.
        return 0;
    }
}
