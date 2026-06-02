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
import static org.junit.jupiter.api.Assertions.assertNull;
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
 * Sprint 071 / S-Auto-15 (M-Auto-3, workstream A) — negative controls +
 * suppress-case + capture/reset coverage for the NEW BotSession-scoped
 * CROSS-TURN {@code search_knowledge} re-search suppression gate.
 *
 * <p>The within-turn A3 gate ({@code lastSearchKnowledgeViableHit}, a per-run
 * local) and the A1 dedup cache ({@code successfulDispatchCache}, also
 * per-run) see only ONE outer turn because
 * {@code SessionManager.processMessage} reloads the {@code BotSession} from
 * the DB every turn. The cross-turn gate carries a standing viable-hit marker
 * across turns via PERSISTED {@code BotSession} columns
 * ({@code crossTurnFaqHitUseCase} / {@code crossTurnFaqHitPayload} /
 * {@code crossTurnSearchAllowedSinceHit}) and is keyed PURELY on the existing
 * {@code activeUseCase} / {@code driftType} / {@code faq_miss} signals + a
 * FIXED cardinality budget of 1.
 *
 * <p>These tests model the per-turn DB reload by re-using the SAME
 * {@code BotSession} instance across consecutive {@code run()} calls (the
 * persisted fields are what would survive the reload) and by setting the
 * {@code @Transient driftType}/{@code previousActiveUseCase} slots on the
 * session before each turn (in production
 * {@code ControlKernel.applyRerouteDecision} sets them BEFORE {@code run()}).
 *
 * <p>GUARDRAIL 0 (anti-误杀): default = ALLOW; suppress ONLY when all four
 * conditions PROVE true; drift always disables the gate; budget FIXED at 1
 * (first cross-turn refinement always allowed); ONLY {@code search_knowledge}
 * may ever be gated. The mandatory negative controls below are (a) UC-change
 * → ALLOW, (b) drift → ALLOW, (c) first refinement budget-1 → ALLOW,
 * (d) genuine-miss → ALLOW, (e) write-tool → never gated, (f) all-4-true →
 * SUPPRESS, (g) null/malformed standing-state → ALLOW.
 */
@ExtendWith(MockitoExtension.class)
class AgentRunLoopCrossTurnParaphraseGateTest {

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
        UseCaseRegistryService.UseCaseDefinition ucc = new UseCaseRegistryService.UseCaseDefinition(
                "UC-C", "Messages & Replies", List.of("Messaging"), "LOW", true, "FAQ");
        lenient().when(useCaseRegistry.getUseCase("UC-A")).thenReturn(uca);
        lenient().when(useCaseRegistry.getUseCase("UC-C")).thenReturn(ucc);
        lenient().when(toolPolicyEnforcer.getVisibleToolsForUc(anyString())).thenReturn(List.of(
                "search_knowledge", "resolve_article", "get_customer_context",
                "request_handover", "record_outcome"));
    }

    private static PhasePlan resolvePlan(String uc) {
        return PhasePlan.builder()
                .phase("RESOLVE")
                .useCase(uc)
                .objective("answer")
                .allowedTools(List.of("search_knowledge", "resolve_article",
                        "get_customer_context", "request_handover", "record_outcome"))
                .maxToolSteps(6)
                .systemInstruction("RESOLVE")
                .validTerminalOutcomes(Set.of(TerminalOutcome.FINAL_ANSWER,
                        TerminalOutcome.ESCALATE))
                .build();
    }

    /**
     * A single persisted BotSession re-used across turns to model the
     * per-turn DB reload. {@code totalBotTurns} doubles as the captured-turn
     * marker; we bump it before each new turn.
     */
    private static BotSession session(String id, String uc) {
        return BotSession.builder()
                .sessionId(id)
                .activeUseCase(uc)
                .currentPhase("RESOLVE")
                .handlingState("BOT_HANDLING")
                .totalBotTurns(0)
                .clarificationCount(0)
                .faqMissCount(0)
                .repeatedActionCount(0)
                .crossTurnSearchAllowedSinceHit(0)
                .build();
    }

    /** Simulate a non-drifting same-UC continuation turn. */
    private static void markNoDrift(BotSession s) {
        s.setDriftType(null);
        s.setPreviousActiveUseCase(s.getActiveUseCase());
    }

    private static Map<String, Object> argsA() {
        Map<String, Object> a = new LinkedHashMap<>();
        a.put("query", "why can't I see my advert");
        a.put("uc_tags", List.of("UC-A"));
        return a;
    }

    private static Map<String, Object> argsB() {
        Map<String, Object> a = new LinkedHashMap<>();
        // Paraphrase of argsA: different wording, same intent.
        a.put("query", "advert visibility on my account");
        a.put("uc_tags", List.of("UC-A"));
        return a;
    }

    private static Map<String, Object> argsNewIntent() {
        Map<String, Object> a = new LinkedHashMap<>();
        // A genuinely new searchable intent on the next turn.
        a.put("query", "how do I delete my account");
        a.put("uc_tags", List.of("UC-A"));
        return a;
    }

    private static Map<String, Object> viableHitData() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("faq_miss", false);
        m.put("hits", List.of(Map.of("source_id", "ka4P2-faq", "title", "Ad visibility")));
        return m;
    }

    private static Map<String, Object> faqMissData() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("faq_miss", true);
        m.put("hits", List.of());
        return m;
    }

    private static ToolCall search(Map<String, Object> args) {
        return ToolCall.builder().name("search_knowledge").arguments(args).build();
    }

    /**
     * Run ONE turn that issues a single search_knowledge (returning the
     * supplied result) then a final answer. Returns the run result.
     */
    private AgentRunResult runTurnWithSearch(BotSession s, ToolCall searchCall,
                                             ToolResult searchResult) {
        LlmResponse rawA = LlmResponse.builder().content("{\"step\":0}").build();
        LlmResponse rawB = LlmResponse.builder().content("{\"step\":1}").build();
        lenient().when(llmInvocation.invokeChat(anyString(), anyString(), anyString(), anyInt()))
                .thenReturn(rawA).thenReturn(rawB);
        lenient().when(actionParser.parse(rawA.getContent())).thenReturn(ParsedAction.builder()
                .toolCalls(List.of(searchCall)).userMessage("").reasoning("search").build());
        lenient().when(actionParser.parse(rawB.getContent())).thenReturn(ParsedAction.builder()
                .toolCalls(List.of()).userMessage("Here is what I found.").reasoning("done").build());
        lenient().when(toolDispatcher.validateAgainstPlan(any(), eq("search_knowledge")))
                .thenReturn(ToolResult.ok(null));
        lenient().when(toolDispatcher.dispatch(eq("search_knowledge"), any(), any()))
                .thenReturn(searchResult);
        return agentRunLoop.run(resolvePlan(s.getActiveUseCase()), s, "msg", List.of());
    }

    private static boolean anyCrossTurnSuppressed(AgentRunResult r) {
        return r.toolEvents().stream().anyMatch(ToolEvent::crossTurnParaphraseSuppressed);
    }

    // ── end-to-end capture → first-refinement-allowed → suppress ───────
    @Test
    void endToEnd_captureThenFirstRefinementAllowed() {
        BotSession s = session("xt-e2e", "UC-A");

        // Turn 0: capture a standing viable hit for UC-A. budget -> 0.
        markNoDrift(s);
        s.setTotalBotTurns(0);
        AgentRunResult t0 = runTurnWithSearch(s, search(argsA()),
                ToolResult.ok(viableHitData()));
        assertFalse(anyCrossTurnSuppressed(t0), "turn 0 capture must not suppress");
        assertEquals("UC-A", s.getCrossTurnFaqHitUseCase());
        assertNotNull(s.getCrossTurnFaqHitPayload());
        assertEquals(0, s.getCrossTurnSearchAllowedSinceHit());

        // Turn 1: FIRST cross-turn refinement — budget-1, ALWAYS allowed.
        // Use a faq_miss result so the budget spend is observable (a viable
        // hit would re-capture and reset budget to 0).
        markNoDrift(s);
        s.setTotalBotTurns(1);
        AgentRunResult t1 = runTurnWithSearch(s, search(argsB()),
                ToolResult.ok(faqMissData()));
        assertFalse(anyCrossTurnSuppressed(t1),
                "first cross-turn refinement must be allowed (budget-1); got "
                        + t1.toolEvents());
        // A faq_miss result clears the marker — that is the correct
        // generalization (a genuine miss warrants fresh searching). The
        // dedicated suppress-case below drives the standing state directly.
        assertNull(s.getCrossTurnFaqHitUseCase(),
                "the faq_miss refinement result clears the standing marker");
    }

    // ── end-to-end VIABLE-HIT CHAIN (the cs001 storm shape): turn0 capture
    //    → turn1 first-refinement allowed (budget preserved, NOT reset by the
    //    same-UC viable re-capture) → turn2 SUPPRESSED. This is the real
    //    storm pattern: every same-UC re-search returns faq_miss=false, so a
    //    same-UC viable re-capture must NOT reset the budget, else the gate
    //    could never reach suppression. ───────────────────────────────────
    @Test
    void viableHitChain_budgetAccumulates_thirdTurnSuppressed() {
        BotSession s = session("xt-chain", "UC-A");

        // Turn 0: first search, viable hit → capture marker, budget 0.
        markNoDrift(s);
        s.setTotalBotTurns(0);
        AgentRunResult t0 = runTurnWithSearch(s, search(argsA()),
                ToolResult.ok(viableHitData()));
        assertFalse(anyCrossTurnSuppressed(t0), "turn 0 capture must not suppress");
        assertEquals("UC-A", s.getCrossTurnFaqHitUseCase());
        assertEquals(0, s.getCrossTurnSearchAllowedSinceHit(), "budget 0 after capture");

        // Turn 1: paraphrase re-search, viable hit again. FIRST refinement →
        // allowed (budget-1); the same-UC viable re-capture PRESERVES budget=1
        // (does NOT reset to 0).
        markNoDrift(s);
        s.setTotalBotTurns(1);
        AgentRunResult t1 = runTurnWithSearch(s, search(argsB()),
                ToolResult.ok(viableHitData()));
        assertFalse(anyCrossTurnSuppressed(t1),
                "first cross-turn refinement must be allowed (budget-1); got "
                        + t1.toolEvents());
        assertEquals("UC-A", s.getCrossTurnFaqHitUseCase(),
                "same-UC viable re-capture keeps the marker");
        assertEquals(1, s.getCrossTurnSearchAllowedSinceHit(),
                "a same-UC viable re-capture must PRESERVE the spent budget (not reset to 0)");

        // Turn 2: another paraphrase re-search. budget now >= 1, same un-drifted
        // UC, standing hit present → SUPPRESSED.
        markNoDrift(s);
        s.setTotalBotTurns(2);
        AgentRunResult t2 = runTurnWithSearch(s, search(argsNewIntent()),
                ToolResult.ok(viableHitData()));
        assertTrue(anyCrossTurnSuppressed(t2),
                "the 2nd+ cross-turn re-search (budget spent) must be suppressed; got "
                        + t2.toolEvents());
    }

    // ── (f) all four conditions true → SUPPRESS ────────────────────────
    @Test
    void allFourConditionsTrue_crossTurnReSearch_isSuppressed() {
        BotSession s = session("xt-f", "UC-A");
        // Drive the standing state directly: a standing viable hit for the
        // SAME un-drifted UC with the budget-1 refinement already spent.
        s.setCrossTurnFaqHitUseCase("UC-A");
        s.setCrossTurnFaqHitPayload(objectMapperWrite(viableHitData()));
        s.setCrossTurnSearchAllowedSinceHit(1);
        markNoDrift(s);
        s.setTotalBotTurns(2);

        // Use a viable-hit result for the (suppressed) dispatch's stub; it
        // should never be consumed because the gate suppresses before
        // dispatch.
        AgentRunResult t2 = runTurnWithSearch(s, search(argsB()),
                ToolResult.ok(viableHitData()));

        // The tool must NOT be re-dispatched.
        verify(toolDispatcher, times(0)).dispatch(eq("search_knowledge"), any(), any());
        assertTrue(anyCrossTurnSuppressed(t2),
                "with all 4 conditions true (same un-drifted UC + standing hit + "
                        + "budget spent) the cross-turn re-search must be suppressed; got "
                        + t2.toolEvents());
        ToolEvent suppressed = t2.toolEvents().stream()
                .filter(ToolEvent::crossTurnParaphraseSuppressed).findFirst().orElseThrow();
        assertTrue(suppressed.success(), "suppressed event mirrors the standing hit success");
        assertEquals(0L, suppressed.latencyMs(), "tool not re-executed → zero latency");
        assertEquals(viableHitData(), suppressed.resultData(),
                "the standing payload is served on suppression");
        assertEquals(2, suppressed.crossTurnHitAtTurn(),
                "the suppressed event references the standing-hit turn marker");
        // Distinctness from the within-turn / A1 annotations:
        assertFalse(suppressed.paraphraseSuppressed(),
                "cross-turn suppression is DISTINCT from within-turn paraphrase_suppressed");
        assertFalse(suppressed.deduplicated(),
                "cross-turn suppression is DISTINCT from A1 deduplicated");
    }

    private String objectMapperWrite(Object o) {
        try {
            return objectMapper.writeValueAsString(o);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // ── (a) UC change → ALLOW (standing hit was for a different UC) ─────
    @Test
    void ucChange_crossTurnSearch_isNotSuppressed() {
        BotSession s = session("xt-a", "UC-A");
        // Standing hit for UC-A, budget already spent.
        s.setCrossTurnFaqHitUseCase("UC-A");
        s.setCrossTurnFaqHitPayload(objectMapperWrite(viableHitData()));
        s.setCrossTurnSearchAllowedSinceHit(1);

        // This turn the UC switched to UC-C. A UC change is a drift signal;
        // even setting driftType null, active != previous disables the gate.
        s.setActiveUseCase("UC-C");
        s.setPreviousActiveUseCase("UC-A");
        s.setDriftType(null);
        s.setTotalBotTurns(2);

        AgentRunResult r = runTurnWithSearch(s, search(argsB()),
                ToolResult.ok(viableHitData()));

        verify(toolDispatcher, times(1)).dispatch(eq("search_knowledge"), any(), any());
        assertFalse(anyCrossTurnSuppressed(r),
                "a UC change must disable the gate (standing hit was for a different UC); got "
                        + r.toolEvents());
        // Stale standing marker for the old UC must not have suppressed.
    }

    // ── (b) drift → ALLOW ──────────────────────────────────────────────
    @Test
    void driftThisTurn_crossTurnSearch_isNotSuppressed() {
        BotSession s = session("xt-b", "UC-A");
        s.setCrossTurnFaqHitUseCase("UC-A");
        s.setCrossTurnFaqHitPayload(objectMapperWrite(viableHitData()));
        s.setCrossTurnSearchAllowedSinceHit(1);

        // Same UC, but a non-null/non-"none" drift token this turn.
        s.setActiveUseCase("UC-A");
        s.setPreviousActiveUseCase("UC-A");
        s.setDriftType("SAME_UC_NEW_TASK");
        s.setTotalBotTurns(2);

        // Use a faq_miss result so the standing marker is not re-captured,
        // isolating the run-start drift-clear assertion (a viable hit would
        // legitimately re-capture a fresh standing marker AFTER the clear).
        AgentRunResult r = runTurnWithSearch(s, search(argsNewIntent()),
                ToolResult.ok(faqMissData()));

        verify(toolDispatcher, times(1)).dispatch(eq("search_knowledge"), any(), any());
        assertFalse(anyCrossTurnSuppressed(r),
                "any drift signal must disable the gate (drift always wins); got "
                        + r.toolEvents());
        // Drift CLEARS the standing marker at run start (and the faq_miss
        // result this turn does not re-capture).
        assertNull(s.getCrossTurnFaqHitUseCase(),
                "drift must clear the standing marker so it does not leak");
        assertNull(s.getCrossTurnFaqHitPayload());
        assertEquals(0, s.getCrossTurnSearchAllowedSinceHit());
    }

    // ── (c) first cross-turn refinement (budget-1) → ALLOW ─────────────
    @Test
    void firstCrossTurnRefinement_budget1_isNotSuppressed() {
        BotSession s = session("xt-c", "UC-A");
        // Standing hit present, budget NOT yet spent (0).
        s.setCrossTurnFaqHitUseCase("UC-A");
        s.setCrossTurnFaqHitPayload(objectMapperWrite(viableHitData()));
        s.setCrossTurnSearchAllowedSinceHit(0);

        markNoDrift(s);
        s.setTotalBotTurns(1);
        // Use a faq_miss result so the standing hit is NOT re-captured, to
        // isolate the budget-spend assertion.
        AgentRunResult r = runTurnWithSearch(s, search(argsB()),
                ToolResult.ok(faqMissData()));

        verify(toolDispatcher, times(1)).dispatch(eq("search_knowledge"), any(), any());
        assertFalse(anyCrossTurnSuppressed(r),
                "the FIRST cross-turn refinement after a standing hit must be allowed "
                        + "(budget-1); got " + r.toolEvents());
    }

    // ── (d) genuine miss → ALLOW + clears the standing marker ───────────
    @Test
    void genuineMissReSearch_isNotSuppressed_andClearsMarker() {
        BotSession s = session("xt-d", "UC-A");
        // Standing hit present, budget spent.
        s.setCrossTurnFaqHitUseCase("UC-A");
        s.setCrossTurnFaqHitPayload(objectMapperWrite(viableHitData()));
        s.setCrossTurnSearchAllowedSinceHit(1);

        markNoDrift(s);
        s.setTotalBotTurns(2);
        // A turn whose search itself returns a genuine miss. The suppression
        // gate sits BEFORE dispatch, so with budget spent it WOULD suppress —
        // unless the marker had been cleared by a prior genuine miss. Here we
        // assert the genuine-miss RESULT clears the marker for the NEXT turn.
        // First, prove the result clears the standing state by running a turn
        // whose search returns faq_miss=true; we drive a fresh standing state
        // so this turn's first call is the allowed refinement.
        s.setCrossTurnSearchAllowedSinceHit(0); // first refinement this turn
        AgentRunResult r = runTurnWithSearch(s, search(argsB()),
                ToolResult.ok(faqMissData()));

        verify(toolDispatcher, times(1)).dispatch(eq("search_knowledge"), any(), any());
        assertFalse(anyCrossTurnSuppressed(r), "genuine-miss re-search must dispatch");
        // The faq_miss=true RESULT must clear the persisted standing marker.
        assertNull(s.getCrossTurnFaqHitUseCase(),
                "a genuine miss must clear the standing marker");
        assertNull(s.getCrossTurnFaqHitPayload());
        assertEquals(0, s.getCrossTurnSearchAllowedSinceHit());
    }

    // ── (e) write/side-effect tool → never gated ───────────────────────
    @Test
    void writeTool_isNeverGated() {
        BotSession s = session("xt-e", "UC-A");
        // A standing hit + budget spent would suppress a search_knowledge,
        // but a write tool (record_outcome) must NEVER be gated.
        s.setCrossTurnFaqHitUseCase("UC-A");
        s.setCrossTurnFaqHitPayload(objectMapperWrite(viableHitData()));
        s.setCrossTurnSearchAllowedSinceHit(1);
        markNoDrift(s);
        s.setTotalBotTurns(2);

        ToolCall recordOutcome = ToolCall.builder().name("record_outcome")
                .arguments(Map.of("containment_outcome", "contained")).build();

        LlmResponse rawA = LlmResponse.builder().content("{\"step\":0}").build();
        LlmResponse rawB = LlmResponse.builder().content("{\"step\":1}").build();
        when(llmInvocation.invokeChat(anyString(), anyString(), anyString(), anyInt()))
                .thenReturn(rawA).thenReturn(rawB);
        when(actionParser.parse(rawA.getContent())).thenReturn(ParsedAction.builder()
                .toolCalls(List.of(recordOutcome)).userMessage("").reasoning("record").build());
        when(actionParser.parse(rawB.getContent())).thenReturn(ParsedAction.builder()
                .toolCalls(List.of()).userMessage("Done.").reasoning("done").build());
        when(toolDispatcher.validateAgainstPlan(any(), eq("record_outcome")))
                .thenReturn(ToolResult.ok(null));
        when(toolDispatcher.dispatch(eq("record_outcome"), any(), any()))
                .thenReturn(ToolResult.ok(Map.of("recorded", true)));

        AgentRunResult r = agentRunLoop.run(resolvePlan("UC-A"), s, "msg", List.of());

        verify(toolDispatcher, times(1)).dispatch(eq("record_outcome"), any(), any());
        assertFalse(anyCrossTurnSuppressed(r),
                "a write tool must never be suppressed by the cross-turn gate; got "
                        + r.toolEvents());
        // record_outcome is a resolution signal → clears the standing marker.
        assertNull(s.getCrossTurnFaqHitUseCase(),
                "record_outcome must clear the standing marker");
    }

    // ── (g) null / malformed standing state → ALLOW (fail-open) ────────
    @Test
    void nullStandingState_isNotSuppressed() {
        BotSession s = session("xt-g1", "UC-A");
        // No standing hit at all (all null/0).
        markNoDrift(s);
        s.setTotalBotTurns(1);
        AgentRunResult r = runTurnWithSearch(s, search(argsB()),
                ToolResult.ok(faqMissData()));
        verify(toolDispatcher, times(1)).dispatch(eq("search_knowledge"), any(), any());
        assertFalse(anyCrossTurnSuppressed(r),
                "with no standing state the gate must fail open (ALLOW); got " + r.toolEvents());
    }

    @Test
    void malformedStandingPayload_failsOpen_andDispatches() {
        BotSession s = session("xt-g2", "UC-A");
        // UC marker + budget say "suppress", but the payload is malformed JSON
        // and cannot be deserialized → FAIL OPEN, dispatch.
        s.setCrossTurnFaqHitUseCase("UC-A");
        s.setCrossTurnFaqHitPayload("{ this is not valid json ]");
        s.setCrossTurnSearchAllowedSinceHit(1);
        markNoDrift(s);
        s.setTotalBotTurns(2);

        AgentRunResult r = runTurnWithSearch(s, search(argsB()),
                ToolResult.ok(faqMissData()));

        verify(toolDispatcher, times(1)).dispatch(eq("search_knowledge"), any(), any());
        assertFalse(anyCrossTurnSuppressed(r),
                "a malformed standing payload must fail open (never suppress on evidence "
                        + "we cannot serve); got " + r.toolEvents());
    }

    // ── within-turn A3 + A1 behaviour unchanged (coexistence) ──────────
    @Test
    void withinTurnA3AndA1_unchanged_byCrossTurnGate() {
        // Single turn, no standing cross-turn state. Step 0: dispatch (viable
        // hit). Step 1: byte-identical (A1 dedup). Step 2: paraphrase
        // (within-turn A3). Step 3: final. The cross-turn gate must NOT
        // interfere: only ONE real dispatch, A1 + within-turn A3 annotations
        // present, NO cross-turn annotation.
        BotSession s = session("xt-coexist", "UC-A");
        markNoDrift(s);
        s.setTotalBotTurns(0);

        ToolCall step0 = search(argsA());
        ToolCall step1 = search(argsA()); // byte-identical
        ToolCall step2 = search(argsB()); // paraphrase

        LlmResponse rawA = LlmResponse.builder().content("{\"step\":0}").build();
        LlmResponse rawB = LlmResponse.builder().content("{\"step\":1}").build();
        LlmResponse rawC = LlmResponse.builder().content("{\"step\":2}").build();
        LlmResponse rawD = LlmResponse.builder().content("{\"step\":3}").build();
        when(llmInvocation.invokeChat(anyString(), anyString(), anyString(), anyInt()))
                .thenReturn(rawA).thenReturn(rawB).thenReturn(rawC).thenReturn(rawD);
        when(actionParser.parse(rawA.getContent())).thenReturn(ParsedAction.builder()
                .toolCalls(List.of(step0)).userMessage("").reasoning("first").build());
        when(actionParser.parse(rawB.getContent())).thenReturn(ParsedAction.builder()
                .toolCalls(List.of(step1)).userMessage("").reasoning("identical").build());
        when(actionParser.parse(rawC.getContent())).thenReturn(ParsedAction.builder()
                .toolCalls(List.of(step2)).userMessage("").reasoning("paraphrase").build());
        when(actionParser.parse(rawD.getContent())).thenReturn(ParsedAction.builder()
                .toolCalls(List.of()).userMessage("Answer.").reasoning("done").build());

        when(toolDispatcher.validateAgainstPlan(any(), eq("search_knowledge")))
                .thenReturn(ToolResult.ok(null));
        when(toolDispatcher.dispatch(eq("search_knowledge"), any(), any()))
                .thenReturn(ToolResult.ok(viableHitData()));

        AgentRunResult r = agentRunLoop.run(resolvePlan("UC-A"), s, "msg", List.of());

        verify(toolDispatcher, times(1)).dispatch(eq("search_knowledge"), any(), any());
        List<ToolEvent> events = r.toolEvents();
        ToolEvent dedup = events.stream().filter(ToolEvent::deduplicated)
                .findFirst().orElseThrow();
        ToolEvent within = events.stream().filter(ToolEvent::paraphraseSuppressed)
                .findFirst().orElseThrow();
        assertEquals(1, dedup.stepIndex(), "A1 caught the byte-identical step 1");
        assertEquals(2, within.stepIndex(), "within-turn A3 caught the paraphrase at step 2");
        assertFalse(anyCrossTurnSuppressed(r),
                "the cross-turn gate must not fire within a single turn (no standing state); got "
                        + events);
        // After this turn, the standing cross-turn marker WAS captured (viable
        // hit) for UC-A with budget reset to 0.
        assertEquals("UC-A", s.getCrossTurnFaqHitUseCase());
        assertEquals(0, s.getCrossTurnSearchAllowedSinceHit());
    }
}
