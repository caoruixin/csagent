package com.gumtree.csagent.service.runtime;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gumtree.csagent.model.AgentRunResult;
import com.gumtree.csagent.model.BotSession;
import com.gumtree.csagent.model.LlmResponse;
import com.gumtree.csagent.model.ParsedAction;
import com.gumtree.csagent.model.PhasePlan;
import com.gumtree.csagent.model.TerminalOutcome;
import com.gumtree.csagent.model.ToolCall;
import com.gumtree.csagent.model.ToolEvent;
import com.gumtree.csagent.service.tools.ToolDispatcher;
import com.gumtree.csagent.service.tools.ToolPolicyEnforcer;
import com.gumtree.csagent.service.tools.ToolResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * P1 search re-issue storm (2026-07-25) — a turn in which the LLM keeps
 * re-issuing {@code search_knowledge} must (a) never buy extra loop
 * iterations, and (b) be given the facts it needs to stop.
 *
 * <h2>What was measured</h2>
 *
 * Two real E2E failures of
 * {@code test_06_three_turn_dialogue_progresses_without_repeating_question},
 * both ending {@code request_handover(turn_budget_exhausted)} on a read-only
 * UC-A status conversation that already held viable KB hits:
 *
 * <ul>
 *   <li>{@code 81a08aeb-…} turn 3 — FOUR byte-identical searches, all
 *       cross-turn-suppressed, all charged.</li>
 *   <li>{@code f62ad6ce-…} turn 1 — TEN searches, only two byte-identical;
 *       the first landed a viable hit so the within-turn A3 gate suppressed
 *       the other NINE.</li>
 * </ul>
 *
 * <h2>The withdrawn first attempt (regression guard below)</h2>
 *
 * The first fix refunded "no-progress" steps (steps where every call was
 * backstop-served) and raised the ceiling to {@code maxToolSteps + 4}. On
 * {@code f62ad6ce-…} that is exactly what happened — {@code 10 = 6 + 4} — and
 * the turn escalated anyway. Because a viable hit makes A3 suppress
 * <em>every</em> later search in the turn, essentially the whole turn became
 * uncharged and ran to the ceiling. The refund bought four extra LLM calls and
 * four extra chances to re-phrase, and changed no outcome. It is withdrawn:
 * the loop bound is {@code step < maxToolSteps}, and
 * {@link #suppressedRepeatStorm_neverExceedsMaxToolStepsLlmCalls()} guards
 * against re-introducing it.
 *
 * <h2>The actual fix</h2>
 *
 * The LLM could not see its own prior queries: {@code already_called} carries
 * an opaque {@code arguments_hash}, and the old
 * {@code search_reuse_instruction} granted an exemption ("...or your new query
 * is materially different from what you already searched") whose precondition
 * it therefore had no data to evaluate — and which it quoted back verbatim
 * while storming. {@code search_attempts_this_turn} /
 * {@code search_attempts_summary} now report, deterministically, every query
 * issued this turn, whether the tool actually ran, and what came back.
 *
 * <p><strong>Deliberately NOT done:</strong> no cap on how many searches a
 * turn may contain, and no rule that N searches is a failure. Repeated
 * retrieval is not provably wrong behaviour, and hard-failing it is the
 * documented PARAPHRASE_STORM 误杀 trap. The runtime already stops the
 * redundant <em>retrieval</em> (the KB was queried once in both sessions);
 * what was missing was the LLM's ability to know that.
 */
@ExtendWith(MockitoExtension.class)
class AgentRunLoopIdempotentRepeatBudgetTest {

    @Mock private LlmInvocationService llmInvocation;
    @Mock private ToolDispatcher toolDispatcher;
    @Mock private ActionParser actionParser;
    @Mock private UseCaseRegistryService useCaseRegistry;
    @Mock private ControlPolicyService controlPolicy;
    @Mock private ToolPolicyEnforcer toolPolicyEnforcer;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private AgentRunLoop agentRunLoop;

    @BeforeEach
    void setUp() {
        ContextProjectionBuilder projectionBuilder = new ContextProjectionBuilder(
                objectMapper, useCaseRegistry, controlPolicy, toolPolicyEnforcer, null);
        agentRunLoop = new AgentRunLoopImpl(
                llmInvocation, toolDispatcher, projectionBuilder, actionParser, objectMapper);

        lenient().when(controlPolicy.getMaxBotTurnsFaq()).thenReturn(6);
        lenient().when(controlPolicy.getMaxClarificationRounds()).thenReturn(3);
        lenient().when(controlPolicy.getMaxFaqMiss()).thenReturn(2);

        UseCaseRegistryService.UseCaseDefinition uca = new UseCaseRegistryService.UseCaseDefinition(
                "UC-A", "Ad Status & Visibility", List.of("Ad Support"), "LOW", true, "FAQ");
        lenient().when(useCaseRegistry.getUseCase("UC-A")).thenReturn(uca);
        lenient().when(toolPolicyEnforcer.getVisibleToolsForUc(anyString())).thenReturn(List.of(
                "search_knowledge", "resolve_article", "get_customer_context",
                "request_handover", "record_outcome"));
    }

    // ── fixtures ───────────────────────────────────────────────────────

    private static PhasePlan resolvePlan(int maxToolSteps) {
        return PhasePlan.builder()
                .phase("RESOLVE")
                .useCase("UC-A")
                .objective("answer")
                .allowedTools(List.of("search_knowledge", "resolve_article",
                        "request_handover", "record_outcome"))
                .maxToolSteps(maxToolSteps)
                .systemInstruction("RESOLVE")
                .validTerminalOutcomes(Set.of(TerminalOutcome.FINAL_ANSWER,
                        TerminalOutcome.ESCALATE, TerminalOutcome.MAX_STEPS))
                .build();
    }

    private static BotSession session(String id) {
        return BotSession.builder()
                .sessionId(id)
                .activeUseCase("UC-A")
                .currentPhase("RESOLVE")
                .handlingState("BOT_HANDLING")
                .totalBotTurns(3)
                .clarificationCount(0)
                .faqMissCount(0)
                .repeatedActionCount(0)
                .crossTurnSearchAllowedSinceHit(0)
                .build();
    }

    private static Map<String, Object> queryArgs(String query) {
        Map<String, Object> a = new LinkedHashMap<>();
        a.put("query", query);
        a.put("uc_tags", List.of("UC-A"));
        return a;
    }

    private static Map<String, Object> viableHitData() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("faq_miss", false);
        m.put("hits", List.of(Map.of("source_id", "ka4P2-faq", "title", "Where Is My Ad?")));
        return m;
    }

    private static ToolCall search(String query) {
        return ToolCall.builder().name("search_knowledge").arguments(queryArgs(query)).build();
    }

    /**
     * Stub the LLM to emit one search per step for the supplied queries, then
     * a no-tool-call final answer. Passing the SAME string twice reproduces a
     * byte-identical repeat; the near-miss strings reproduce the paraphrase
     * shape observed on {@code f62ad6ce-…}.
     */
    private void stubSearchStorm(List<String> queries) {
        List<LlmResponse> responses = new ArrayList<>();
        for (int i = 0; i < queries.size(); i++) {
            LlmResponse raw = LlmResponse.builder().content("{\"step\":" + i + "}").build();
            responses.add(raw);
            lenient().when(actionParser.parse(raw.getContent())).thenReturn(ParsedAction.builder()
                    .toolCalls(List.of(search(queries.get(i))))
                    .userMessage("")
                    .reasoning("the prior search was about something else")
                    .build());
        }
        LlmResponse finalRaw = LlmResponse.builder().content("{\"final\":true}").build();
        lenient().when(actionParser.parse(finalRaw.getContent())).thenReturn(ParsedAction.builder()
                .toolCalls(List.of())
                .userMessage("Your ad AD-2002 is LIVE. Here are tips to improve visibility.")
                .reasoning("answer from the evidence already retrieved")
                .build());

        var stub = when(llmInvocation.invokeChat(anyString(), anyString(), anyString(), anyInt()))
                .thenReturn(responses.get(0));
        for (int i = 1; i < responses.size(); i++) {
            stub = stub.thenReturn(responses.get(i));
        }
        stub.thenReturn(finalRaw);
    }

    /** The paraphrase shape actually observed on session f62ad6ce-…. */
    private static List<String> observedParaphraseStorm() {
        return List.of(
                "ad visibility low visibility how to improve",
                "ad visibility low views",
                "improve ad visibility Gumtree",
                "ad visibility tips Gumtree",
                "ad visibility tips improve",
                "ad visibility tips Gumtree",     // byte-identical to #4
                "ad visibility low visibility",
                "improve ad visibility Gumtree",  // byte-identical to #3
                "improve ad visibility",
                "ad visibility tips");
    }

    // ── (1) REGRESSION GUARD: no extra iterations, ever ───────────────

    @Test
    void suppressedRepeatStorm_neverExceedsMaxToolStepsLlmCalls() {
        // Guards the withdrawn no-progress refund. On session f62ad6ce-… the
        // refund let this exact shape run 10 = maxToolSteps(6) + grace(4)
        // iterations and escalate anyway. The loop must make at most
        // maxToolSteps LLM calls no matter how many calls are suppressed.
        stubSearchStorm(observedParaphraseStorm());
        when(toolDispatcher.validateAgainstPlan(any(), eq("search_knowledge")))
                .thenReturn(ToolResult.ok(null));
        when(toolDispatcher.dispatch(eq("search_knowledge"), any(), any()))
                .thenReturn(ToolResult.ok(viableHitData()));

        AgentRunResult result = agentRunLoop.run(
                resolvePlan(6), session("storm-bound"), "how do I improve visibility?",
                List.of());

        assertEquals(6, result.llmEvents().size(),
                "the loop must make exactly maxToolSteps(6) LLM calls; a suppressed call "
                        + "must never buy an extra iteration");
        assertEquals(TerminalOutcome.MAX_STEPS, result.terminalOutcome());
        // The knowledge base is queried once; the A3 gate suppresses the rest.
        // That part was already correct and must stay correct.
        verify(toolDispatcher, times(1)).dispatch(eq("search_knowledge"), any(), any());
        assertEquals(5, result.toolEvents().stream()
                        .filter(ToolEvent::paraphraseSuppressed).count(),
                "every search after the first viable hit is served by the within-turn gate; "
                        + "events=" + result.toolEvents());
    }

    // ── (2) THE FIX: the LLM can read back its own queries ─────────────

    @Test
    void searchAttemptsSlot_reportsEveryQueryVerbatimWithHonestExecutionState()
            throws Exception {
        stubSearchStorm(observedParaphraseStorm());
        when(toolDispatcher.validateAgainstPlan(any(), eq("search_knowledge")))
                .thenReturn(ToolResult.ok(null));
        when(toolDispatcher.dispatch(eq("search_knowledge"), any(), any()))
                .thenReturn(ToolResult.ok(viableHitData()));

        agentRunLoop.run(resolvePlan(6), session("storm-facts"),
                "how do I improve visibility?", List.of());

        ArgumentCaptor<String> projections = ArgumentCaptor.forClass(String.class);
        verify(llmInvocation, times(6))
                .invokeChat(projections.capture(), anyString(), anyString(), anyInt());

        // The LAST projection the LLM saw must let it answer "have I already
        // searched this?" — the question it kept getting wrong.
        JsonNode last = objectMapper.readTree(projections.getAllValues().get(5));
        JsonNode attempts = last.get("search_attempts_this_turn");
        assertNotNull(attempts, "the slot must always be present");
        assertTrue(attempts.isArray() && attempts.size() == 5,
                "all five prior searches must be listed; got " + attempts);

        // Verbatim queries — not hashes. This is the whole point.
        List<String> listed = new ArrayList<>();
        attempts.forEach(n -> listed.add(n.path("query").asText()));
        assertEquals(observedParaphraseStorm().subList(0, 5), listed,
                "queries must be reported verbatim and in order; got " + listed);

        // Honest execution state: one real retrieval, four served.
        assertTrue(attempts.get(0).path("executed").asBoolean(false),
                "the first search really ran");
        assertFalse(attempts.get(0).has("suppression"),
                "a real dispatch carries no suppression label");
        for (int i = 1; i < 5; i++) {
            assertFalse(attempts.get(i).path("executed").asBoolean(true),
                    "search #" + i + " was served, not executed: " + attempts.get(i));
            assertEquals("within_turn_faq_hit", attempts.get(i).path("suppression").asText());
            assertEquals("step 0", attempts.get(i).path("served_from").asText());
        }
        // Result state travels with each attempt so "did it help?" is answerable.
        assertFalse(attempts.get(1).path("faq_miss").asBoolean(true));
        assertEquals(1, attempts.get(1).path("hit_count").asInt(-1));
        assertEquals("ka4P2-faq", attempts.get(1).path("top_source_ids").get(0).asText());

        JsonNode summary = last.get("search_attempts_summary");
        assertNotNull(summary);
        assertEquals(5, summary.path("attempts").asInt(-1));
        assertEquals(1, summary.path("knowledge_base_queries_executed").asInt(-1),
                "the knowledge base was queried exactly once");
        assertEquals(4, summary.path("served_without_executing").asInt(-1));
        assertEquals(5, summary.path("distinct_query_strings").asInt(-1),
                "plain set cardinality of the verbatim strings — no similarity judgement");
    }

    // ── (3) the licence clause the model quoted back is gone ───────────

    @Test
    void searchReuseInstruction_noLongerGrantsTheMateriallyDifferentExemption()
            throws Exception {
        stubSearchStorm(observedParaphraseStorm());
        when(toolDispatcher.validateAgainstPlan(any(), eq("search_knowledge")))
                .thenReturn(ToolResult.ok(null));
        when(toolDispatcher.dispatch(eq("search_knowledge"), any(), any()))
                .thenReturn(ToolResult.ok(viableHitData()));

        agentRunLoop.run(resolvePlan(6), session("storm-instr"),
                "how do I improve visibility?", List.of());

        ArgumentCaptor<String> projections = ArgumentCaptor.forClass(String.class);
        verify(llmInvocation, times(6))
                .invokeChat(projections.capture(), anyString(), anyString(), anyInt());
        JsonNode last = objectMapper.readTree(projections.getAllValues().get(5));
        String instruction = last.path("search_reuse_instruction").asText("");

        assertFalse(instruction.isEmpty(),
                "the slot must still render once a viable hit stands");
        assertFalse(instruction.contains("materially different"),
                "the exemption clause the model quoted verbatim while issuing ten searches "
                        + "must be gone; got: " + instruction);
        assertTrue(instruction.contains("search_attempts_this_turn"),
                "the instruction must point at the data the model needs to check itself");
        assertTrue(instruction.contains("in any wording"),
                "it must state the mechanical fact that re-phrasing does not re-retrieve");
    }

    // ── (4) NEGATIVE CONTROL: budget accounting is untouched ───────────

    @Test
    void stepsThatReallyDispatch_stillConsumeTheBudget_exactlyAsBefore() {
        LlmResponse r0 = LlmResponse.builder().content("{\"s\":0}").build();
        LlmResponse r1 = LlmResponse.builder().content("{\"s\":1}").build();
        LlmResponse r2 = LlmResponse.builder().content("{\"s\":2}").build();
        when(llmInvocation.invokeChat(anyString(), anyString(), anyString(), anyInt()))
                .thenReturn(r0).thenReturn(r1).thenReturn(r2);
        lenient().when(actionParser.parse(r0.getContent())).thenReturn(ParsedAction.builder()
                .toolCalls(List.of(ToolCall.builder().name("resolve_article")
                        .arguments(Map.of("source_id", "kb-1")).build()))
                .userMessage("").reasoning("fetch one").build());
        lenient().when(actionParser.parse(r1.getContent())).thenReturn(ParsedAction.builder()
                .toolCalls(List.of(ToolCall.builder().name("resolve_article")
                        .arguments(Map.of("source_id", "kb-2")).build()))
                .userMessage("").reasoning("fetch another").build());
        lenient().when(actionParser.parse(r2.getContent())).thenReturn(ParsedAction.builder()
                .toolCalls(List.of(ToolCall.builder().name("resolve_article")
                        .arguments(Map.of("source_id", "kb-3")).build()))
                .userMessage("").reasoning("fetch a third").build());
        when(toolDispatcher.validateAgainstPlan(any(), eq("resolve_article")))
                .thenReturn(ToolResult.ok(null));
        when(toolDispatcher.dispatch(eq("resolve_article"), any(), any()))
                .thenReturn(ToolResult.ok(Map.of("source_id", "kb-1")))
                .thenReturn(ToolResult.ok(Map.of("source_id", "kb-2")));

        AgentRunResult result = agentRunLoop.run(
                resolvePlan(2), session("neg-work"), "tell me more", List.of());

        assertEquals(TerminalOutcome.MAX_STEPS, result.terminalOutcome());
        verify(llmInvocation, times(2))
                .invokeChat(anyString(), anyString(), anyString(), anyInt());
        verify(toolDispatcher, times(2)).dispatch(eq("resolve_article"), any(), any());
    }

    // ── (5) NEGATIVE CONTROL: the storm is NOT hard-failed ─────────────

    @Test
    void repeatedSearches_areNeverRejectedOrCapped_antiWrongKill() {
        // Anti-误杀. Repeated retrieval is not provably wrong behaviour, and
        // the documented PARAPHRASE_STORM precedent forbids turning it into a
        // hard failure. Every re-issued search must still produce a SUCCESSFUL
        // event carrying the viable hits — never a rejection, never an
        // `error` key (SkillGuardrailDispatcher reads that key and would let a
        // premature handover through).
        stubSearchStorm(observedParaphraseStorm());
        when(toolDispatcher.validateAgainstPlan(any(), eq("search_knowledge")))
                .thenReturn(ToolResult.ok(null));
        when(toolDispatcher.dispatch(eq("search_knowledge"), any(), any()))
                .thenReturn(ToolResult.ok(viableHitData()));

        AgentRunResult result = agentRunLoop.run(
                resolvePlan(6), session("anti-wrongkill"), "how do I improve visibility?",
                List.of());

        for (ToolEvent e : result.toolEvents()) {
            if (!"search_knowledge".equals(e.toolName())) continue;
            assertTrue(e.success(),
                    "a re-issued search must never be turned into a failure: " + e);
            assertTrue(e.resultData() instanceof Map,
                    "the viable hits must still be served: " + e);
            Map<?, ?> data = (Map<?, ?>) e.resultData();
            assertFalse(data.containsKey("error"),
                    "no error key may be injected — the FAQ guardrails read it: " + e);
            assertTrue(data.get("hits") instanceof List<?> hits && !hits.isEmpty(),
                    "grounding evidence must survive: " + e);
        }
    }

    // ── (6) the cross-turn production shape still suppresses ───────────

    @Test
    void crossTurnStandingHit_stillSuppresses_andIsReportedInTheSlot() throws Exception {
        BotSession s = session("xt-facts");
        s.setCrossTurnFaqHitUseCase("UC-A");
        s.setCrossTurnFaqHitPayload(objectMapper.writeValueAsString(viableHitData()));
        s.setCrossTurnSearchAllowedSinceHit(1);
        s.setDriftType(null);
        s.setPreviousActiveUseCase("UC-A");

        stubSearchStorm(List.of("improve ad visibility tips", "improve ad visibility tips"));
        lenient().when(toolDispatcher.validateAgainstPlan(any(), eq("search_knowledge")))
                .thenReturn(ToolResult.ok(null));

        AgentRunResult result = agentRunLoop.run(
                resolvePlan(6), s, "what can I do to improve visibility?", List.of());

        verify(toolDispatcher, times(0)).dispatch(eq("search_knowledge"), any(), any());
        assertTrue(result.toolEvents().stream().anyMatch(ToolEvent::crossTurnParaphraseSuppressed),
                "CROSS_TURN_SUPPRESSION_BUDGET behaviour must be byte-unchanged; events="
                        + result.toolEvents());
        assertEquals(TerminalOutcome.FINAL_ANSWER, result.terminalOutcome(),
                "two suppressed searches inside a 6-step budget still leave room to answer");

        ArgumentCaptor<String> projections = ArgumentCaptor.forClass(String.class);
        verify(llmInvocation, org.mockito.Mockito.atLeast(2))
                .invokeChat(projections.capture(), anyString(), anyString(), anyInt());
        List<String> all = projections.getAllValues();
        JsonNode attempts = objectMapper.readTree(all.get(all.size() - 1))
                .get("search_attempts_this_turn");
        assertTrue(attempts.size() >= 1, "the cross-turn attempt must be listed: " + attempts);
        assertEquals("cross_turn_standing_hit", attempts.get(0).path("suppression").asText());
        assertFalse(attempts.get(0).path("executed").asBoolean(true));
    }

    // ── (7) per-call diagnostic: shape safety ──────────────────────────

    @Test
    void withRepeatSuppressedNotice_preservesPayloadAndIsSideEffectFree() {
        Map<String, Object> original = viableHitData();
        Object annotated = AgentRunLoopImpl.withRepeatSuppressedNotice(
                original, "a1_identity_cache", "step 0", 2);

        assertNotNull(annotated);
        assertTrue(annotated instanceof Map);
        @SuppressWarnings("unchecked")
        Map<String, Object> map = (Map<String, Object>) annotated;
        assertEquals(false, map.get("faq_miss"), "faq_miss must survive verbatim");
        assertEquals(original.get("hits"), map.get("hits"), "hits must survive verbatim");
        assertFalse(map.containsKey("error"),
                "the notice must never inject an `error` key — SkillGuardrailDispatcher reads it "
                        + "and would let a premature handover through");
        assertFalse(original.containsKey("repeat_suppressed"),
                "the served/cached payload must not be mutated (the trace + cache reuse it)");
        assertEquals("raw", AgentRunLoopImpl.withRepeatSuppressedNotice(
                "raw", "a1_identity_cache", "step 0", 1));
    }

    @Test
    void countBackstopServe_isPureCardinalityPerIdentity() {
        Map<String, Integer> counts = new LinkedHashMap<>();
        assertEquals(1, AgentRunLoopImpl.countBackstopServe(counts, "search_knowledge|h1"));
        assertEquals(2, AgentRunLoopImpl.countBackstopServe(counts, "search_knowledge|h1"));
        assertEquals(3, AgentRunLoopImpl.countBackstopServe(counts, "search_knowledge|h1"));
        assertEquals(1, AgentRunLoopImpl.countBackstopServe(counts, "search_knowledge|h2"),
                "a different call identity counts independently — which is exactly why this "
                        + "counter alone could not see a paraphrase storm (it reads 1 forever)");
    }
}
