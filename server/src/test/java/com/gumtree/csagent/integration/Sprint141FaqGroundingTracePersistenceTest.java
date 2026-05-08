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
import org.mockito.invocation.InvocationOnMock;
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

/**
 * Sprint 14.1 §closure — FAQ grounding observability must be durably
 * persisted on the saved {@link BotTurn} so a save/reload trace surface
 * (equivalently: a Mockito capture of the {@code BotTurn} handed to
 * {@code BotTurnRepository.save(...)}) can read the §L1 lineage and
 * §L2 diagnostics. Sprint 14 stamped them onto {@code @Transient}
 * {@code BotSession} fields only, which the Codex review identified
 * as the blocking persistence gap.
 *
 * <p>This test pins the durable trace surface
 * ({@code bot_turns.projected_context.faq_grounding}) and exercises the
 * five blocker-required cases:
 *
 * <ol>
 *   <li>retrieved-only evidence does not imply cited evidence;</li>
 *   <li>resolved-only evidence does not imply cited evidence;</li>
 *   <li>cited-by-source-id is visible;</li>
 *   <li>cited-by-canonical-url is visible (and maps back to a source_id);</li>
 *   <li>missing citation is visible AND non-blocking (the bot reply still
 *       persists; no rewrite, re-loop, or escalation was triggered).</li>
 * </ol>
 *
 * <p>The test deliberately avoids any DB layer — it uses the Mockito
 * {@link BotTurnRepository#save(Object)} interception that
 * {@code Sprint9TraceObservabilityFidelityIntegrationTest} also uses.
 * The persisted {@code projected_context} JSON is the exact value JPA
 * would write to {@code bot_turns.projected_context} (a {@code jsonb}
 * column).
 */
@ExtendWith(MockitoExtension.class)
class Sprint141FaqGroundingTracePersistenceTest {

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
        session.setSessionId("sess-faq-grounding-trace");
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
        // Lenient on getUseCase so the runtime reroute classifier (which
        // may probe other UC tokens during PhaseEvaluator.evaluate) does
        // not trip the strict-stubbing check; the test only asserts
        // observability fields on the persisted turn, not routing.
        lenient().when(useCaseRegistry.getUseCase(anyString())).thenReturn(
                new com.gumtree.csagent.service.runtime.UseCaseRegistryService.UseCaseDefinition(
                        "UC-A", "Ad Support",
                        List.of("Ad Support"), "LOW", true, "FAQ"));
        when(contextProjectionBuilder.build(any(), any(), any(), anyString(), any()))
                .thenReturn("{\"phase\":\"RESOLVE\",\"use_case\":\"UC-A\"}");
        lenient().when(controlPolicy.isValidTransition(anyString(), anyString())).thenReturn(true);
    }

    private List<BotTurn> captureSaves() {
        List<BotTurn> savedTurns = new ArrayList<>();
        when(turnRepository.save(any(BotTurn.class))).thenAnswer((InvocationOnMock inv) -> {
            BotTurn t = inv.getArgument(0);
            savedTurns.add(t);
            return t;
        });
        return savedTurns;
    }

    private static Map<String, Object> hit(String sourceId, String title, String canonicalUrl) {
        Map<String, Object> h = new LinkedHashMap<>();
        h.put("source_id", sourceId);
        h.put("title", title);
        h.put("snippet", "snippet body");
        h.put("score", 0.91);
        if (canonicalUrl != null) {
            h.put("canonical_url", canonicalUrl);
        }
        return h;
    }

    private static Map<String, Object> articlePayload(String sourceId, String title, String url) {
        Map<String, Object> a = new LinkedHashMap<>();
        a.put("source_id", sourceId);
        a.put("article_id", sourceId);
        a.put("title", title);
        a.put("description", "Body of " + sourceId);
        if (url != null) {
            a.put("source_url", url);
            a.put("canonical_url", url);
        }
        a.put("is_published", true);
        a.put("safe_to_show", true);
        return a;
    }

    private JsonNode readFaqGrounding(BotTurn savedTurn) throws Exception {
        assertNotNull(savedTurn.getProjectedContext(),
                "Sprint 14.1 closure: bot_turns.projected_context must be persisted (not null)");
        JsonNode projected = objectMapper.readTree(savedTurn.getProjectedContext());
        JsonNode faq = projected.get("faq_grounding");
        assertNotNull(faq,
                "Sprint 14.1 closure: bot_turns.projected_context.faq_grounding must be visible after save");
        return faq;
    }

    /**
     * Case 1 — search_knowledge succeeds, resolve_article never runs, the
     * bot answers without quoting the candidate. The saved turn must
     * surface {@code retrieved_source_ids} and a non-cited grounding state
     * so a reviewer can spot a retrieved-only turn from the trace.
     */
    @Test
    void retrievedOnly_persistsRetrievedSourceIds_doesNotImplyCited() throws Exception {
        BotSession session = ucaSession();
        mockCommonStubs();

        when(llmInvocation.invokeChat(anyString(), anyString(), anyString(), anyInt()))
                .thenReturn(LlmResponse.builder().content("step0").build())
                .thenReturn(LlmResponse.builder().content("step1").build());

        when(actionParser.parse("step0")).thenReturn(ParsedAction.builder()
                .toolCalls(List.of(ToolCall.builder()
                        .name("search_knowledge")
                        .arguments(Map.of("query", "where is my advert"))
                        .build()))
                .userMessage("")
                .build());
        when(actionParser.parse("step1")).thenReturn(ParsedAction.builder()
                .toolCalls(List.of())
                .userMessage("Hi, I'm looking into this for you right now.")
                .build());

        when(toolDispatcher.validateAgainstPlan(any(), eq("search_knowledge")))
                .thenReturn(ToolResult.ok(null));

        Map<String, Object> hits = new LinkedHashMap<>();
        hits.put("faq_miss", false);
        hits.put("hits", List.of(hit("ka4P2000000021pIAA",
                "Where is my advert",
                "https://help.gumtree.com/where-is-my-ad")));
        when(toolDispatcher.dispatch(eq("search_knowledge"), any(), any()))
                .thenReturn(ToolResult.ok(hits));

        List<BotTurn> savedTurns = captureSaves();
        controlKernel.processMessage(session, "I can't see my advert");

        assertEquals(1, savedTurns.size());
        JsonNode faq = readFaqGrounding(savedTurns.get(0));

        assertTrue(faq.has("retrieved_source_ids"));
        assertEquals(1, faq.get("retrieved_source_ids").size());
        assertEquals("ka4P2000000021pIAA",
                faq.get("retrieved_source_ids").get(0).asText());
        assertEquals(0, faq.get("resolved_source_ids").size());
        assertEquals(0, faq.get("cited_source_ids").size(),
                "Sprint 14.1 closure: retrieved evidence must not imply cited evidence in the persisted trace");
        assertFalse(faq.get("citation_present").asBoolean());
        assertTrue(faq.get("retrieved_but_unresolved").asBoolean());
    }

    /**
     * Case 2 — search + resolve both succeed; the bot paraphrases without
     * quoting the article. The persisted trace must show
     * {@code resolved_source_ids} and {@code resolved_but_uncited=true}.
     */
    @Test
    void resolvedOnly_persistsResolvedSourceIds_doesNotImplyCited() throws Exception {
        BotSession session = ucaSession();
        mockCommonStubs();

        when(llmInvocation.invokeChat(anyString(), anyString(), anyString(), anyInt()))
                .thenReturn(LlmResponse.builder().content("step0").build())
                .thenReturn(LlmResponse.builder().content("step1").build())
                .thenReturn(LlmResponse.builder().content("step2").build());

        when(actionParser.parse("step0")).thenReturn(ParsedAction.builder()
                .toolCalls(List.of(ToolCall.builder()
                        .name("search_knowledge")
                        .arguments(Map.of("query", "buyer safety guidance"))
                        .build()))
                .userMessage("")
                .build());
        when(actionParser.parse("step1")).thenReturn(ParsedAction.builder()
                .toolCalls(List.of(ToolCall.builder()
                        .name("resolve_article")
                        .arguments(Map.of("source_id", "ka4P200000001AbIAI"))
                        .build()))
                .userMessage("")
                .build());
        when(actionParser.parse("step2")).thenReturn(ParsedAction.builder()
                .toolCalls(List.of())
                .userMessage("If something seems too good to be true, please double-check before paying.")
                .build());

        when(toolDispatcher.validateAgainstPlan(any(), eq("search_knowledge")))
                .thenReturn(ToolResult.ok(null));
        when(toolDispatcher.validateAgainstPlan(any(), eq("resolve_article")))
                .thenReturn(ToolResult.ok(null));

        Map<String, Object> hits = new LinkedHashMap<>();
        hits.put("faq_miss", false);
        hits.put("hits", List.of(hit("ka4P200000001AbIAI",
                "Property Scams Awareness",
                "https://help.gumtree.com/scams")));
        when(toolDispatcher.dispatch(eq("search_knowledge"), any(), any()))
                .thenReturn(ToolResult.ok(hits));
        when(toolDispatcher.dispatch(eq("resolve_article"), any(), any()))
                .thenReturn(ToolResult.ok(articlePayload("ka4P200000001AbIAI",
                        "Property Scams Awareness",
                        "https://help.gumtree.com/scams")));

        List<BotTurn> savedTurns = captureSaves();
        controlKernel.processMessage(session, "Can you tell me about buyer safety guidance?");

        assertEquals(1, savedTurns.size());
        JsonNode faq = readFaqGrounding(savedTurns.get(0));

        assertEquals(1, faq.get("retrieved_source_ids").size());
        assertEquals(1, faq.get("resolved_source_ids").size());
        assertEquals("ka4P200000001AbIAI",
                faq.get("resolved_source_ids").get(0).asText());
        assertEquals(0, faq.get("cited_source_ids").size(),
                "Sprint 14.1 closure: resolved evidence must not imply cited evidence in the persisted trace");
        assertFalse(faq.get("citation_present").asBoolean());
        assertTrue(faq.get("resolved_but_uncited").asBoolean());
        assertFalse(faq.get("retrieved_but_unresolved").asBoolean());
    }

    /**
     * Case 3 — bot reply quotes a Salesforce KA source_id verbatim. The
     * persisted trace must list the cited id and report
     * {@code citation_present=true}, {@code citation_match=true}.
     */
    @Test
    void citedBySourceId_isVisibleInPersistedTrace() throws Exception {
        BotSession session = ucaSession();
        mockCommonStubs();

        when(llmInvocation.invokeChat(anyString(), anyString(), anyString(), anyInt()))
                .thenReturn(LlmResponse.builder().content("step0").build())
                .thenReturn(LlmResponse.builder().content("step1").build())
                .thenReturn(LlmResponse.builder().content("step2").build());

        when(actionParser.parse("step0")).thenReturn(ParsedAction.builder()
                .toolCalls(List.of(ToolCall.builder()
                        .name("search_knowledge")
                        .arguments(Map.of("query", "advert"))
                        .build()))
                .userMessage("")
                .build());
        when(actionParser.parse("step1")).thenReturn(ParsedAction.builder()
                .toolCalls(List.of(ToolCall.builder()
                        .name("resolve_article")
                        .arguments(Map.of("source_id", "ka4P2000000021pIAA"))
                        .build()))
                .userMessage("")
                .build());
        when(actionParser.parse("step2")).thenReturn(ParsedAction.builder()
                .toolCalls(List.of())
                .userMessage("Per article ka4P2000000021pIAA, ads can be hidden during moderation review.")
                .build());

        when(toolDispatcher.validateAgainstPlan(any(), eq("search_knowledge")))
                .thenReturn(ToolResult.ok(null));
        when(toolDispatcher.validateAgainstPlan(any(), eq("resolve_article")))
                .thenReturn(ToolResult.ok(null));

        Map<String, Object> hits = new LinkedHashMap<>();
        hits.put("faq_miss", false);
        hits.put("hits", List.of(hit("ka4P2000000021pIAA",
                "Where is my advert",
                "https://help.gumtree.com/where-is-my-ad")));
        when(toolDispatcher.dispatch(eq("search_knowledge"), any(), any()))
                .thenReturn(ToolResult.ok(hits));
        when(toolDispatcher.dispatch(eq("resolve_article"), any(), any()))
                .thenReturn(ToolResult.ok(articlePayload("ka4P2000000021pIAA",
                        "Where is my advert",
                        "https://help.gumtree.com/where-is-my-ad")));

        List<BotTurn> savedTurns = captureSaves();
        controlKernel.processMessage(session, "where is my advert");

        assertEquals(1, savedTurns.size());
        JsonNode faq = readFaqGrounding(savedTurns.get(0));

        assertEquals(1, faq.get("cited_source_ids").size());
        assertEquals("ka4P2000000021pIAA",
                faq.get("cited_source_ids").get(0).asText());
        assertTrue(faq.get("citation_present").asBoolean());
        assertTrue(faq.get("citation_match").asBoolean());
        assertFalse(faq.get("citation_drift").asBoolean());
        assertFalse(faq.get("resolved_but_uncited").asBoolean());
    }

    /**
     * Case 4 — bot reply quotes the article's canonical URL (no source_id
     * mention). The persisted trace must map the URL back to the candidate
     * source_id under {@code cited_source_ids} and leave
     * {@code cited_canonical_urls} empty.
     */
    @Test
    void citedByCanonicalUrl_isVisibleInPersistedTrace() throws Exception {
        BotSession session = ucaSession();
        mockCommonStubs();

        when(llmInvocation.invokeChat(anyString(), anyString(), anyString(), anyInt()))
                .thenReturn(LlmResponse.builder().content("step0").build())
                .thenReturn(LlmResponse.builder().content("step1").build())
                .thenReturn(LlmResponse.builder().content("step2").build());

        when(actionParser.parse("step0")).thenReturn(ParsedAction.builder()
                .toolCalls(List.of(ToolCall.builder()
                        .name("search_knowledge")
                        .arguments(Map.of("query", "advert"))
                        .build()))
                .userMessage("")
                .build());
        when(actionParser.parse("step1")).thenReturn(ParsedAction.builder()
                .toolCalls(List.of(ToolCall.builder()
                        .name("resolve_article")
                        .arguments(Map.of("source_id", "ka4P2000000021pIAA"))
                        .build()))
                .userMessage("")
                .build());
        when(actionParser.parse("step2")).thenReturn(ParsedAction.builder()
                .toolCalls(List.of())
                .userMessage("More details here: https://help.gumtree.com/where-is-my-ad")
                .build());

        when(toolDispatcher.validateAgainstPlan(any(), eq("search_knowledge")))
                .thenReturn(ToolResult.ok(null));
        when(toolDispatcher.validateAgainstPlan(any(), eq("resolve_article")))
                .thenReturn(ToolResult.ok(null));

        Map<String, Object> hits = new LinkedHashMap<>();
        hits.put("faq_miss", false);
        hits.put("hits", List.of(hit("ka4P2000000021pIAA",
                "Where is my advert",
                "https://help.gumtree.com/where-is-my-ad")));
        when(toolDispatcher.dispatch(eq("search_knowledge"), any(), any()))
                .thenReturn(ToolResult.ok(hits));
        when(toolDispatcher.dispatch(eq("resolve_article"), any(), any()))
                .thenReturn(ToolResult.ok(articlePayload("ka4P2000000021pIAA",
                        "Where is my advert",
                        "https://help.gumtree.com/where-is-my-ad")));

        List<BotTurn> savedTurns = captureSaves();
        controlKernel.processMessage(session, "where is my advert");

        assertEquals(1, savedTurns.size());
        JsonNode faq = readFaqGrounding(savedTurns.get(0));

        assertEquals(1, faq.get("cited_source_ids").size(),
                "Sprint 14.1 closure: a canonical URL citation must map back to a cited source_id");
        assertEquals("ka4P2000000021pIAA",
                faq.get("cited_source_ids").get(0).asText());
        assertEquals(0, faq.get("cited_canonical_urls").size(),
                "URL that matched a candidate canonical_url must NOT also appear under cited_canonical_urls");
        assertTrue(faq.get("citation_present").asBoolean());
        assertTrue(faq.get("citation_match").asBoolean());
        assertFalse(faq.get("citation_drift").asBoolean());
    }

    /**
     * Case 5 — search + resolve succeed but the bot replies with a
     * clarification that contains no citation whatsoever. The persisted
     * trace must surface the missing-citation state AND the bot reply
     * must still flow through unchanged (non-blocking guarantee — Sprint
     * 14.1 closure does NOT introduce a hard citation gate).
     */
    @Test
    void missingCitation_isVisibleAndNonBlocking() throws Exception {
        BotSession session = ucaSession();
        mockCommonStubs();

        when(llmInvocation.invokeChat(anyString(), anyString(), anyString(), anyInt()))
                .thenReturn(LlmResponse.builder().content("step0").build())
                .thenReturn(LlmResponse.builder().content("step1").build())
                .thenReturn(LlmResponse.builder().content("step2").build());

        when(actionParser.parse("step0")).thenReturn(ParsedAction.builder()
                .toolCalls(List.of(ToolCall.builder()
                        .name("search_knowledge")
                        .arguments(Map.of("query", "advert"))
                        .build()))
                .userMessage("")
                .build());
        when(actionParser.parse("step1")).thenReturn(ParsedAction.builder()
                .toolCalls(List.of(ToolCall.builder()
                        .name("resolve_article")
                        .arguments(Map.of("source_id", "ka4P2000000021pIAA"))
                        .build()))
                .userMessage("")
                .build());
        // Clarification reply — no source_id, no URL, no distinctive title.
        String clarification = "I understand. Could you share the ad ID so I can take a closer look?";
        when(actionParser.parse("step2")).thenReturn(ParsedAction.builder()
                .toolCalls(List.of())
                .userMessage(clarification)
                .build());

        when(toolDispatcher.validateAgainstPlan(any(), eq("search_knowledge")))
                .thenReturn(ToolResult.ok(null));
        when(toolDispatcher.validateAgainstPlan(any(), eq("resolve_article")))
                .thenReturn(ToolResult.ok(null));

        Map<String, Object> hits = new LinkedHashMap<>();
        hits.put("faq_miss", false);
        hits.put("hits", List.of(hit("ka4P2000000021pIAA",
                "Where is my advert",
                "https://help.gumtree.com/where-is-my-ad")));
        when(toolDispatcher.dispatch(eq("search_knowledge"), any(), any()))
                .thenReturn(ToolResult.ok(hits));
        when(toolDispatcher.dispatch(eq("resolve_article"), any(), any()))
                .thenReturn(ToolResult.ok(articlePayload("ka4P2000000021pIAA",
                        "Where is my advert",
                        "https://help.gumtree.com/where-is-my-ad")));

        List<BotTurn> savedTurns = captureSaves();
        ControlKernel.KernelResult result = controlKernel.processMessage(session, "I can't find my ad");

        assertEquals(1, savedTurns.size());
        BotTurn savedTurn = savedTurns.get(0);
        JsonNode faq = readFaqGrounding(savedTurn);

        // Non-blocking — the bot reply still landed verbatim on the
        // saved turn AND on the kernel's response.
        assertEquals(clarification, savedTurn.getBotResponse());
        assertEquals(clarification, result.responseText());
        assertFalse(result.shouldEndChat(),
                "Sprint 14.1 closure: missing citation must NOT terminate the session");

        // Observable — the missing-citation diagnostic is durable on
        // bot_turns.projected_context.faq_grounding.
        assertFalse(faq.get("citation_present").asBoolean(),
                "missing citation must surface as citation_present=false in the persisted trace");
        assertEquals(0, faq.get("cited_source_ids").size());
        assertEquals(0, faq.get("cited_canonical_urls").size());
        assertTrue(faq.get("resolved_but_uncited").asBoolean(),
                "resolved-but-uncited must surface as a soft diagnostic in the persisted trace");
        assertNotNull(faq.get("faq_grounding_state"));
    }
}
