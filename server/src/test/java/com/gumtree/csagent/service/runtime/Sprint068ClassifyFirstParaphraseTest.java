package com.gumtree.csagent.service.runtime;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gumtree.csagent.model.BotSession;
import com.gumtree.csagent.model.PhasePlan;
import com.gumtree.csagent.model.TerminalOutcome;
import com.gumtree.csagent.service.runtime.skill.CriticalStep;
import com.gumtree.csagent.service.runtime.skill.Skill;
import com.gumtree.csagent.service.runtime.skill.SkillLoader;
import com.gumtree.csagent.service.runtime.skill.SkillRegistry;
import com.gumtree.csagent.service.tools.ToolPolicyEnforcer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/**
 * Sprint 068 (S-Auto-13, M-Auto-3) coverage for the two skill/projection-layer
 * step-waster fixes:
 *
 * <ul>
 *   <li><b>A2 — DISCOVER classify-first.</b> {@code discover_triage}'s
 *       procedure now commits the use case FIRST instead of instructing a
 *       pre-classification {@code search_knowledge} (which the tool policy
 *       rejects while {@code active_use_case=none}). The
 *       {@code faq-uc-search-before-commit} critical step is reconciled to
 *       {@code faq-uc-classify-first} so its trace_check no longer rewards
 *       search-before-classify.</li>
 *   <li><b>A3 — RESOLVE paraphrase discipline.</b> The
 *       {@code resolve_faq_grounded_answer} grounding instruction discourages
 *       re-searching after a viable hit, and {@link ContextProjectionBuilder}
 *       echoes a soft {@code search_reuse_instruction} when a prior
 *       {@code search_knowledge} already landed a viable hit
 *       ({@code faq_miss=false}) this turn.</li>
 * </ul>
 *
 * <p>These are projection/rendering/wiring assertions. The behaviour-change
 * evidence (GATING_RACE / PARAPHRASE_STORM reduction) is the real-LLM
 * bad_cases rerun recorded in the sub-sprint handoff, per the §5 eval
 * evidence gate (mocked tests cover wiring only).
 */
@ExtendWith(MockitoExtension.class)
class Sprint068ClassifyFirstParaphraseTest {

    private ContextProjectionBuilder builder;
    private ObjectMapper objectMapper;

    @Mock
    private UseCaseRegistryService useCaseRegistry;

    @Mock
    private ControlPolicyService controlPolicy;

    @Mock
    private ToolPolicyEnforcer toolPolicyEnforcer;

    @Mock
    private SkillRegistry skillRegistry;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        builder = new ContextProjectionBuilder(objectMapper, useCaseRegistry,
                controlPolicy, toolPolicyEnforcer, skillRegistry);
        builder.initToolSchemas();
    }

    // ---- A2: discover_triage skill reconciliation (real YAML) ------------

    @Test
    void a2_discoverTriage_procedureIsClassifyFirst() {
        Skill discover = loadProductionSkill("discover_triage");
        String proc = discover.procedure();
        assertTrue(proc.contains("classify FIRST"),
                "A2: DISCOVER procedure must instruct classify-first");
        assertTrue(proc.contains("Do NOT call `search_knowledge` before `classify_use_case` in DISCOVER"),
                "A2: DISCOVER procedure must forbid pre-classification search in DISCOVER");
        assertFalse(proc.contains("call `search_knowledge` with the user's question as the query, then call `classify_use_case`"),
                "A2: the old search-before-classify instruction must be gone");
    }

    @Test
    void a2_discoverTriage_criticalStepReconciledToClassifyFirst() {
        Skill discover = loadProductionSkill("discover_triage");
        assertEquals(3, discover.criticalSteps().size(),
                "A2: discover_triage keeps 3 critical steps (count invariant preserved)");

        CriticalStep classifyFirst = discover.criticalSteps().stream()
                .filter(s -> "faq-uc-classify-first".equals(s.id()))
                .findFirst()
                .orElse(null);
        assertNotNull(classifyFirst,
                "A2: reconciled critical step id must be faq-uc-classify-first");
        assertEquals("accumulated_tool_results.classify_use_case", classifyFirst.traceCheck(),
                "A2: trace_check must reward the classify commit, not search-before-classify");

        assertFalse(discover.criticalSteps().stream()
                        .anyMatch(s -> s.traceCheck() != null
                                && s.traceCheck().contains("tool_event_seq(search_knowledge) < tool_event_seq(classify_use_case)")),
                "A2: no critical step may still reward search-before-classify in DISCOVER");
    }

    // ---- A3: resolve_faq skill grounding discipline (real YAML) ----------

    @Test
    void a3_resolveFaq_groundingDiscouragesReSearchAfterViableHit() {
        Skill resolveFaq = loadProductionSkill("resolve_faq_grounded_answer");
        String grounding = resolveFaq.groundingInstruction();
        assertTrue(grounding.contains("do NOT re-search this turn"),
                "A3: grounding must discourage re-search after a viable hit");
        assertTrue(grounding.contains("faq_miss=false"),
                "A3: grounding must key the discipline on faq_miss=false");
        // P1 search re-issue storm (2026-07-25) — INVERTED. This assertion
        // used to require the clause "...or your new query is materially
        // different from what you already searched". Measurement retired it:
        //
        //  (a) it was FALSE about this runtime. The A3 gate
        //      (AgentRunLoopImpl, `lastSearchKnowledgeViableHit != null`) and
        //      the cross-turn gate suppress a re-search after a viable hit
        //      WITHOUT EVER COMPARING QUERY CONTENT. A "materially different"
        //      query is suppressed exactly like a paraphrase, so the skill was
        //      advertising an exemption the runtime does not implement.
        //  (b) the model quoted it back while storming. Real E2E session
        //      f62ad6ce-0a0f-4d88-ac05-8d3dc77914c3 turn 1: ten searches, one
        //      real retrieval, nine suppressed, every step reasoning "a new
        //      search with a materially different query is warranted".
        //
        // The faq_miss=true carve-out below is KEPT and pinned: that one is
        // true (a non-viable result clears the gate's tracker and a fresh
        // search really does run).
        assertFalse(grounding.contains("materially different"),
                "A3: the re-phrase exemption is retired — it described an exemption the "
                        + "A3 / cross-turn gates never implemented (they never compare query "
                        + "content) and the model cited it verbatim while issuing ten searches "
                        + "in one turn");
        assertTrue(grounding.contains("faq_miss=true"),
                "A3: the TRUE carve-out must survive — a non-viable prior result does warrant "
                        + "a fresh search, and the runtime really does allow it");
    }

    // ---- A3: ContextProjectionBuilder search-reuse echo ------------------

    @Test
    void a3_projectionEcho_emittedAfterViableHit() throws Exception {
        stubForBuild();
        Map<String, Object> accumulated = Map.of(
                "search_knowledge", Map.of(
                        "faq_miss", false,
                        "hits", List.of(Map.of("source_id", "KB-1", "title", "Visibility"))));

        JsonNode root = buildResolveProjection(accumulated);
        assertTrue(root.has("search_reuse_instruction"),
                "A3: echo must fire when a prior search_knowledge returned a viable hit");
        assertTrue(root.path("prior_search_knowledge_viable_hit").asBoolean(false),
                "A3: prior_search_knowledge_viable_hit flag must be true on a viable hit");
        assertTrue(root.get("search_reuse_instruction").asText().contains("Do NOT call search_knowledge again"),
                "A3: echo text must instruct the LLM not to re-search");
    }

    @Test
    void a3_projectionEcho_suppressedOnFaqMiss() throws Exception {
        stubForBuild();
        Map<String, Object> accumulated = Map.of(
                "search_knowledge", Map.of("faq_miss", true, "hits", List.of()));

        JsonNode root = buildResolveProjection(accumulated);
        assertFalse(root.has("search_reuse_instruction"),
                "A3: a faq_miss=true result must NOT suppress a fresh search — no anti-re-search echo");
    }

    @Test
    void a3_projectionEcho_absentWhenNoPriorSearch() throws Exception {
        stubForBuild();
        Map<String, Object> accumulated = Map.of(
                "get_customer_context", Map.of("name", "Sam"));

        JsonNode root = buildResolveProjection(accumulated);
        assertFalse(root.has("search_reuse_instruction"),
                "A3: echo must be absent when no search_knowledge result is accumulated");
    }

    // ---- helpers ---------------------------------------------------------

    private Skill loadProductionSkill(String name) {
        UseCaseRegistryService reg = new UseCaseRegistryService();
        reg.init();
        return new SkillLoader(reg).loadAll().stream()
                .filter(s -> name.equals(s.name()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("production skill not found: " + name));
    }

    private void stubForBuild() {
        when(controlPolicy.getMaxBotTurnsFaq()).thenReturn(6);
        when(controlPolicy.getMaxClarificationRounds()).thenReturn(3);
        when(controlPolicy.getMaxFaqMiss()).thenReturn(2);
        when(skillRegistry.select("RESOLVE", "UC-A")).thenReturn(Optional.empty());
    }

    private JsonNode buildResolveProjection(Map<String, Object> accumulated) throws Exception {
        BotSession session = BotSession.builder()
                .sessionId("sprint068-test")
                .activeUseCase("UC-A")
                .currentPhase("RESOLVE")
                .handlingState("BOT_HANDLING")
                .totalBotTurns(1)
                .clarificationCount(0)
                .faqMissCount(0)
                .repeatedActionCount(0)
                .build();
        PhasePlan plan = PhasePlan.builder()
                .phase("RESOLVE")
                .useCase("UC-A")
                .objective("answer the visibility question")
                .allowedTools(List.of("search_knowledge", "resolve_article"))
                .maxToolSteps(6)
                .systemInstruction("resolve UC-A")
                .validTerminalOutcomes(Set.of(
                        TerminalOutcome.FINAL_ANSWER, TerminalOutcome.ESCALATE))
                .build();
        String projection = builder.build(session, List.of(), plan, "where is my advert?", accumulated);
        return objectMapper.readTree(projection);
    }
}
