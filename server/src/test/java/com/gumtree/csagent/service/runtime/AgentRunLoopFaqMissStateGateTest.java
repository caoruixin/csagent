package com.gumtree.csagent.service.runtime;

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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
 * Sprint 069 / S-Auto-13b (M-Auto-3) — negative controls + coexistence
 * coverage for the A3 deterministic backstop: the {@code faq_miss}-state-
 * aware same-turn {@code search_knowledge} re-search suppression gate.
 *
 * <p>S-Auto-13 shipped the soft layer ({@code search_reuse_instruction}
 * projection echo + the paraphrase-discipline {@code grounding_instruction}
 * line in {@code resolve_faq_grounded_answer.yaml}). The signal fires but
 * {@code deepseek-v4-flash} ignores it (PARAPHRASE_STORM stayed 16/16/7 vs
 * the §11 ≤3 target). This sub-sprint adds the deterministic substrate
 * the LLM cannot route around, BENEATH the soft layer (which stays as the
 * soft-signal-first measure).
 *
 * <p>Covered cases:
 *
 * <ul>
 *   <li>(a) FIRST {@code search_knowledge} of a turn is NOT suppressed;</li>
 *   <li>(b) a same-turn re-search after the most-recent prior search
 *       returned {@code faq_miss=true} IS dispatched (the gate ALLOWS
 *       legitimate retries after a no-viable-hit result);</li>
 *   <li>(c) a fresh {@code AgentRunLoop.run(...)} (a new outer turn) does
 *       NOT share the gate state — the tracker is per-run;</li>
 *   <li>(d) a suppressed re-search reuses the cached payload at zero
 *       latency, carries the {@code paraphrase_suppressed=true} +
 *       {@code faqHitAtStep} annotation, and does not re-execute the
 *       tool (no step / budget charged);</li>
 *   <li>(e) A1 (byte-identical) and A3 (paraphrase) coexist — a byte-
 *       identical repeat is caught by A1 (annotation {@code deduplicated});
 *       a paraphrase is caught by A3 (annotation {@code paraphrase_suppressed});
 *       a given event carries one annotation or the other, never both;</li>
 *   <li>(f) a same-turn re-search after the most-recent prior search
 *       FAILED (external failure) IS dispatched — failures reset the
 *       tracker so a legitimate retry still reaches the tool.</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class AgentRunLoopFaqMissStateGateTest {

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
        lenient().when(toolPolicyEnforcer.getVisibleToolsForUc("UC-A")).thenReturn(List.of(
                "search_knowledge", "resolve_article", "get_customer_context",
                "request_handover", "record_outcome"));
    }

    private static PhasePlan resolvePlan() {
        return PhasePlan.builder()
                .phase("RESOLVE")
                .useCase("UC-A")
                .objective("answer")
                .allowedTools(List.of("search_knowledge", "resolve_article",
                        "request_handover", "record_outcome"))
                .maxToolSteps(6)
                .systemInstruction("RESOLVE")
                .validTerminalOutcomes(Set.of(TerminalOutcome.FINAL_ANSWER,
                        TerminalOutcome.ESCALATE))
                .build();
    }

    private static BotSession session(String id) {
        return BotSession.builder()
                .sessionId(id)
                .activeUseCase("UC-A")
                .currentPhase("RESOLVE")
                .handlingState("BOT_HANDLING")
                .totalBotTurns(0)
                .clarificationCount(0)
                .faqMissCount(0)
                .repeatedActionCount(0)
                .build();
    }

    private static Map<String, Object> argsA() {
        Map<String, Object> a = new LinkedHashMap<>();
        a.put("query", "why can't I see my advert");
        a.put("uc_tags", List.of("UC-A"));
        return a;
    }

    private static Map<String, Object> argsB() {
        Map<String, Object> a = new LinkedHashMap<>();
        // Different wording, same intent — a paraphrase, NOT a byte-identical
        // repeat (different canonical args hash). A1 cannot catch this; the
        // A3 gate (faq_miss-state-keyed) is exactly the substrate that does.
        a.put("query", "advert visibility on my account");
        a.put("uc_tags", List.of("UC-A"));
        return a;
    }

    private static Map<String, Object> viableHitData() {
        return Map.of(
                "faq_miss", false,
                "hits", List.of(Map.of("source_id", "ka4P2-faq", "title", "Ad visibility")));
    }

    private static Map<String, Object> faqMissData() {
        return Map.of("faq_miss", true, "hits", List.of());
    }

    // ── (a) first search_knowledge of a turn is NOT suppressed ─────────
    @Test
    void firstSearchKnowledge_isNotSuppressed() {
        ToolCall call = ToolCall.builder().name("search_knowledge").arguments(argsA()).build();

        LlmResponse rawA = LlmResponse.builder().content("{\"step\":0}").build();
        LlmResponse rawB = LlmResponse.builder().content("{\"step\":1}").build();
        when(llmInvocation.invokeChat(anyString(), anyString(), anyString(), anyInt()))
                .thenReturn(rawA).thenReturn(rawB);
        when(actionParser.parse(rawA.getContent())).thenReturn(ParsedAction.builder()
                .toolCalls(List.of(call)).userMessage("").reasoning("look up").build());
        when(actionParser.parse(rawB.getContent())).thenReturn(ParsedAction.builder()
                .toolCalls(List.of()).userMessage("Here is what I found.").reasoning("done").build());
        when(toolDispatcher.validateAgainstPlan(any(), eq("search_knowledge")))
                .thenReturn(ToolResult.ok(null));
        when(toolDispatcher.dispatch(eq("search_knowledge"), any(), any()))
                .thenReturn(ToolResult.ok(viableHitData()));

        AgentRunResult result = agentRunLoop.run(resolvePlan(), session("sgate-a"),
                "why can't I see my advert", List.of());

        verify(toolDispatcher, times(1)).dispatch(eq("search_knowledge"), any(), any());
        assertEquals(TerminalOutcome.FINAL_ANSWER, result.terminalOutcome());
        boolean anySuppressed = result.toolEvents().stream()
                .anyMatch(ToolEvent::paraphraseSuppressed);
        assertFalse(anySuppressed,
                "a single dispatch must not be flagged paraphrase_suppressed; got "
                        + result.toolEvents());
    }

    // ── (b) re-search after faq_miss=true IS dispatched (gate ALLOWS) ───
    @Test
    void reSearchAfterFaqMissTrue_isDispatched() {
        ToolCall call0 = ToolCall.builder().name("search_knowledge").arguments(argsA()).build();
        ToolCall call1 = ToolCall.builder().name("search_knowledge").arguments(argsB()).build();

        LlmResponse rawA = LlmResponse.builder().content("{\"step\":0}").build();
        LlmResponse rawB = LlmResponse.builder().content("{\"step\":1}").build();
        LlmResponse rawC = LlmResponse.builder().content("{\"step\":2}").build();
        when(llmInvocation.invokeChat(anyString(), anyString(), anyString(), anyInt()))
                .thenReturn(rawA).thenReturn(rawB).thenReturn(rawC);
        when(actionParser.parse(rawA.getContent())).thenReturn(ParsedAction.builder()
                .toolCalls(List.of(call0)).userMessage("").reasoning("first try").build());
        when(actionParser.parse(rawB.getContent())).thenReturn(ParsedAction.builder()
                .toolCalls(List.of(call1)).userMessage("").reasoning("retry, different wording").build());
        when(actionParser.parse(rawC.getContent())).thenReturn(ParsedAction.builder()
                .toolCalls(List.of()).userMessage("Here is what I found.").reasoning("done").build());

        when(toolDispatcher.validateAgainstPlan(any(), eq("search_knowledge")))
                .thenReturn(ToolResult.ok(null));
        // First search returns faq_miss=true (no viable hit); the second
        // call has a DIFFERENT query (paraphrase) and must STILL dispatch
        // because the prior result was not a viable hit.
        when(toolDispatcher.dispatch(eq("search_knowledge"), any(), any()))
                .thenReturn(ToolResult.ok(faqMissData()))
                .thenReturn(ToolResult.ok(viableHitData()));

        AgentRunResult result = agentRunLoop.run(resolvePlan(), session("sgate-b"),
                "why can't I see my advert", List.of());

        verify(toolDispatcher, times(2)).dispatch(eq("search_knowledge"), any(), any());
        boolean anySuppressed = result.toolEvents().stream()
                .anyMatch(ToolEvent::paraphraseSuppressed);
        assertFalse(anySuppressed,
                "a re-search after the prior search returned faq_miss=true must dispatch, "
                        + "not be suppressed; got " + result.toolEvents());
    }

    // ── (c) a fresh AgentRunLoop.run() does NOT share gate state ───────
    @Test
    void crossRun_gateStateIsPerRun() {
        // Run 1: single search_knowledge with viable hit, then final answer.
        ToolCall callR1 = ToolCall.builder().name("search_knowledge").arguments(argsA()).build();
        LlmResponse r1a = LlmResponse.builder().content("{\"step\":0\"}").build();
        LlmResponse r1b = LlmResponse.builder().content("{\"step\":1\"}").build();
        // Run 2: a different search_knowledge query (paraphrase of run 1) —
        // because gate state does NOT persist across runs, this must
        // dispatch even though run 1 ended with a viable hit.
        ToolCall callR2 = ToolCall.builder().name("search_knowledge").arguments(argsB()).build();
        LlmResponse r2a = LlmResponse.builder().content("{\"step\":0\"}").build();
        LlmResponse r2b = LlmResponse.builder().content("{\"step\":1\"}").build();

        when(llmInvocation.invokeChat(anyString(), anyString(), anyString(), anyInt()))
                .thenReturn(r1a).thenReturn(r1b)   // run 1
                .thenReturn(r2a).thenReturn(r2b);  // run 2
        when(actionParser.parse(r1a.getContent())).thenReturn(ParsedAction.builder()
                .toolCalls(List.of(callR1)).userMessage("").reasoning("first turn").build());
        when(actionParser.parse(r1b.getContent())).thenReturn(ParsedAction.builder()
                .toolCalls(List.of()).userMessage("Answer 1.").reasoning("done 1").build());
        when(actionParser.parse(r2a.getContent())).thenReturn(ParsedAction.builder()
                .toolCalls(List.of(callR2)).userMessage("").reasoning("second turn").build());
        when(actionParser.parse(r2b.getContent())).thenReturn(ParsedAction.builder()
                .toolCalls(List.of()).userMessage("Answer 2.").reasoning("done 2").build());

        when(toolDispatcher.validateAgainstPlan(any(), eq("search_knowledge")))
                .thenReturn(ToolResult.ok(null));
        when(toolDispatcher.dispatch(eq("search_knowledge"), any(), any()))
                .thenReturn(ToolResult.ok(viableHitData()))   // run 1 dispatch
                .thenReturn(ToolResult.ok(viableHitData()));  // run 2 dispatch

        AgentRunResult r1Result = agentRunLoop.run(resolvePlan(), session("sgate-c-r1"),
                "why can't I see my advert", List.of());
        AgentRunResult r2Result = agentRunLoop.run(resolvePlan(), session("sgate-c-r2"),
                "advert visibility on my account", List.of());

        // Both runs must dispatch their search_knowledge (2 dispatches total).
        // If the gate state leaked across runs, run 2 would have suppressed
        // and the dispatcher would have been called only once.
        verify(toolDispatcher, times(2)).dispatch(eq("search_knowledge"), any(), any());
        boolean anySuppressedR1 = r1Result.toolEvents().stream()
                .anyMatch(ToolEvent::paraphraseSuppressed);
        boolean anySuppressedR2 = r2Result.toolEvents().stream()
                .anyMatch(ToolEvent::paraphraseSuppressed);
        assertFalse(anySuppressedR1, "run 1 must have no suppression");
        assertFalse(anySuppressedR2, "run 2 must have no suppression (gate is per-run)");
    }

    // ── (d) suppressed event carries annotation + prior hit + zero latency ─
    @Test
    void paraphraseAfterViableHit_isSuppressed_carriesAnnotationAndPriorHit() {
        ToolCall call0 = ToolCall.builder().name("search_knowledge").arguments(argsA()).build();
        ToolCall call1 = ToolCall.builder().name("search_knowledge").arguments(argsB()).build();

        LlmResponse rawA = LlmResponse.builder().content("{\"step\":0\"}").build();
        LlmResponse rawB = LlmResponse.builder().content("{\"step\":1\"}").build();
        LlmResponse rawC = LlmResponse.builder().content("{\"step\":2\"}").build();
        when(llmInvocation.invokeChat(anyString(), anyString(), anyString(), anyInt()))
                .thenReturn(rawA).thenReturn(rawB).thenReturn(rawC);
        when(actionParser.parse(rawA.getContent())).thenReturn(ParsedAction.builder()
                .toolCalls(List.of(call0)).userMessage("").reasoning("first").build());
        when(actionParser.parse(rawB.getContent())).thenReturn(ParsedAction.builder()
                .toolCalls(List.of(call1)).userMessage("").reasoning("paraphrase").build());
        when(actionParser.parse(rawC.getContent())).thenReturn(ParsedAction.builder()
                .toolCalls(List.of()).userMessage("Answer.").reasoning("done").build());

        Map<String, Object> hitData = viableHitData();
        when(toolDispatcher.validateAgainstPlan(any(), eq("search_knowledge")))
                .thenReturn(ToolResult.ok(null));
        when(toolDispatcher.dispatch(eq("search_knowledge"), any(), any()))
                .thenReturn(ToolResult.ok(hitData));

        AgentRunResult result = agentRunLoop.run(resolvePlan(), session("sgate-d"),
                "why can't I see my advert", List.of());

        // Only ONE real dispatch — the paraphrase is suppressed.
        verify(toolDispatcher, times(1)).dispatch(eq("search_knowledge"), any(), any());
        assertEquals(TerminalOutcome.FINAL_ANSWER, result.terminalOutcome());

        List<ToolEvent> events = result.toolEvents();
        assertNotNull(events);
        long searchEvents = events.stream()
                .filter(e -> "search_knowledge".equals(e.toolName())).count();
        assertEquals(2, searchEvents,
                "both LLM-emitted search_knowledge calls must produce a ToolEvent "
                        + "(one real dispatch + one paraphrase-suppressed annotation); got "
                        + events);

        ToolEvent realEvent = events.stream()
                .filter(e -> "search_knowledge".equals(e.toolName())
                        && !e.paraphraseSuppressed())
                .findFirst().orElseThrow();
        ToolEvent suppressedEvent = events.stream()
                .filter(e -> "search_knowledge".equals(e.toolName())
                        && e.paraphraseSuppressed())
                .findFirst().orElseThrow();
        assertEquals(0, realEvent.stepIndex(), "the real dispatch landed at step 0");
        assertTrue(realEvent.success(), "the real dispatch succeeded with a viable hit");

        // Annotation shape:
        assertEquals(1, suppressedEvent.stepIndex(), "the suppressed re-search was at step 1");
        assertTrue(suppressedEvent.paraphraseSuppressed(),
                "the suppressed event must carry paraphraseSuppressed=true");
        assertEquals(0, suppressedEvent.faqHitAtStep(),
                "the suppressed event must reference the step-0 prior viable hit");
        assertTrue(suppressedEvent.success(),
                "a suppressed event mirrors the cached success result");
        assertEquals(0L, suppressedEvent.latencyMs(),
                "a suppressed event records zero latency (tool not re-executed)");
        assertEquals(hitData, suppressedEvent.resultData(),
                "the suppressed event must carry the prior viable-hit payload");
        // Distinctness from A1: a suppressed event is NOT marked deduplicated.
        assertFalse(suppressedEvent.deduplicated(),
                "paraphrase_suppressed events must NOT also be flagged deduplicated "
                        + "(distinct annotation; A1 catches byte-identical, A3 catches paraphrase)");
        assertEquals(-1, suppressedEvent.originalAtStep(),
                "originalAtStep is -1 on a paraphrase_suppressed event (the A1 step index "
                        + "is not used by the A3 gate)");
    }

    // ── (e) A1 and A3 coexist — byte-identical → A1, paraphrase → A3 ────
    @Test
    void a1AndA3Coexist_byteIdenticalDeduped_paraphraseSuppressed() {
        // Step 0: dispatch (viable hit). Step 1: byte-identical repeat
        // (A1 catches). Step 2: paraphrase (A3 catches). Step 3: final.
        ToolCall step0 = ToolCall.builder().name("search_knowledge").arguments(argsA()).build();
        ToolCall step1 = ToolCall.builder().name("search_knowledge").arguments(argsA()).build();
        ToolCall step2 = ToolCall.builder().name("search_knowledge").arguments(argsB()).build();

        LlmResponse rawA = LlmResponse.builder().content("{\"step\":0\"}").build();
        LlmResponse rawB = LlmResponse.builder().content("{\"step\":1\"}").build();
        LlmResponse rawC = LlmResponse.builder().content("{\"step\":2\"}").build();
        LlmResponse rawD = LlmResponse.builder().content("{\"step\":3\"}").build();
        when(llmInvocation.invokeChat(anyString(), anyString(), anyString(), anyInt()))
                .thenReturn(rawA).thenReturn(rawB).thenReturn(rawC).thenReturn(rawD);
        when(actionParser.parse(rawA.getContent())).thenReturn(ParsedAction.builder()
                .toolCalls(List.of(step0)).userMessage("").reasoning("first").build());
        when(actionParser.parse(rawB.getContent())).thenReturn(ParsedAction.builder()
                .toolCalls(List.of(step1)).userMessage("").reasoning("identical retry").build());
        when(actionParser.parse(rawC.getContent())).thenReturn(ParsedAction.builder()
                .toolCalls(List.of(step2)).userMessage("").reasoning("paraphrase").build());
        when(actionParser.parse(rawD.getContent())).thenReturn(ParsedAction.builder()
                .toolCalls(List.of()).userMessage("Answer.").reasoning("done").build());

        Map<String, Object> hitData = viableHitData();
        when(toolDispatcher.validateAgainstPlan(any(), eq("search_knowledge")))
                .thenReturn(ToolResult.ok(null));
        when(toolDispatcher.dispatch(eq("search_knowledge"), any(), any()))
                .thenReturn(ToolResult.ok(hitData));

        AgentRunResult result = agentRunLoop.run(resolvePlan(), session("sgate-e"),
                "why can't I see my advert", List.of());

        // Only ONE real dispatch — A1 + A3 together suppress the next two.
        verify(toolDispatcher, times(1)).dispatch(eq("search_knowledge"), any(), any());

        List<ToolEvent> events = result.toolEvents();
        long searchEvents = events.stream()
                .filter(e -> "search_knowledge".equals(e.toolName())).count();
        assertEquals(3, searchEvents,
                "three LLM-emitted search_knowledge calls produce three ToolEvents "
                        + "(one real + one A1 dedup + one A3 suppression); got " + events);

        ToolEvent dedupEvent = events.stream()
                .filter(e -> "search_knowledge".equals(e.toolName()) && e.deduplicated())
                .findFirst().orElseThrow();
        ToolEvent suppressedEvent = events.stream()
                .filter(e -> "search_knowledge".equals(e.toolName()) && e.paraphraseSuppressed())
                .findFirst().orElseThrow();

        // A1 caught step 1 (byte-identical to step 0):
        assertEquals(1, dedupEvent.stepIndex());
        assertEquals(0, dedupEvent.originalAtStep());
        assertFalse(dedupEvent.paraphraseSuppressed(),
                "A1's deduplicated event must NOT also be flagged paraphrase_suppressed");

        // A3 caught step 2 (paraphrase of step 0; different args hash so A1
        // doesn't match):
        assertEquals(2, suppressedEvent.stepIndex());
        assertEquals(0, suppressedEvent.faqHitAtStep());
        assertFalse(suppressedEvent.deduplicated(),
                "A3's paraphrase_suppressed event must NOT also be flagged deduplicated");
    }

    // ── (f) re-search after dispatch FAILURE IS dispatched ─────────────
    @Test
    void reSearchAfterDispatchFailure_isDispatched() {
        ToolCall call0 = ToolCall.builder().name("search_knowledge").arguments(argsA()).build();
        ToolCall call1 = ToolCall.builder().name("search_knowledge").arguments(argsB()).build();

        LlmResponse rawA = LlmResponse.builder().content("{\"step\":0\"}").build();
        LlmResponse rawB = LlmResponse.builder().content("{\"step\":1\"}").build();
        LlmResponse rawC = LlmResponse.builder().content("{\"step\":2\"}").build();
        when(llmInvocation.invokeChat(anyString(), anyString(), anyString(), anyInt()))
                .thenReturn(rawA).thenReturn(rawB).thenReturn(rawC);
        when(actionParser.parse(rawA.getContent())).thenReturn(ParsedAction.builder()
                .toolCalls(List.of(call0)).userMessage("").reasoning("first").build());
        when(actionParser.parse(rawB.getContent())).thenReturn(ParsedAction.builder()
                .toolCalls(List.of(call1)).userMessage("").reasoning("retry").build());
        when(actionParser.parse(rawC.getContent())).thenReturn(ParsedAction.builder()
                .toolCalls(List.of()).userMessage("Answer.").reasoning("done").build());

        when(toolDispatcher.validateAgainstPlan(any(), eq("search_knowledge")))
                .thenReturn(ToolResult.ok(null));
        // Step-0 dispatch fails (success=false); the tracker resets to
        // null and the step-1 paraphrase must re-dispatch.
        when(toolDispatcher.dispatch(eq("search_knowledge"), any(), any()))
                .thenReturn(ToolResult.error("backend timeout"))
                .thenReturn(ToolResult.ok(viableHitData()));

        AgentRunResult result = agentRunLoop.run(resolvePlan(), session("sgate-f"),
                "why can't I see my advert", List.of());

        verify(toolDispatcher, times(2)).dispatch(eq("search_knowledge"), any(), any());
        boolean anySuppressed = result.toolEvents().stream()
                .anyMatch(ToolEvent::paraphraseSuppressed);
        assertFalse(anySuppressed,
                "a same-args re-search after an external failure must re-dispatch, "
                        + "not be suppressed; got " + result.toolEvents());
    }
}
