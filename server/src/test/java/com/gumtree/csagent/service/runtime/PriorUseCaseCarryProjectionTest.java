package com.gumtree.csagent.service.runtime;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gumtree.csagent.model.BotSession;
import com.gumtree.csagent.model.BotTurn;
import com.gumtree.csagent.service.runtime.skill.SkillTestFixtures;
import com.gumtree.csagent.service.tools.ToolPolicyEnforcer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Sprint 41 unit tests for the NEW {@code prior_use_case_carry} projection
 * slot per Sprint 37 freeze decision (i) §10.4 + Sprint 41 D-d.
 *
 * <p>Covers:
 * <ul>
 *   <li>Slot shape (prior_active_use_case, prior_skill_name,
 *       prior_citations, ages_out_after_turns).</li>
 *   <li>Citation cap = {@code PRIOR_USE_CASE_CARRY_CITATION_CAP}.</li>
 *   <li>Aging window = {@code PRIOR_USE_CASE_CARRY_AGING_TURNS}; slot
 *       drops to null when current turn is more than that many turns
 *       past the most recent UC switch.</li>
 *   <li>Null on no prior switch (single-UC session OR fresh session).</li>
 *   <li>No per-UC variation: UC-A switch produces the same shape as
 *       UC-B switch.</li>
 *   <li>Resolution of prior_skill_name via SkillRegistry.</li>
 * </ul>
 */
class PriorUseCaseCarryProjectionTest {

    private ContextProjectionBuilder builder;
    private ObjectMapper objectMapper;
    private UseCaseRegistryService useCaseRegistry;
    private ControlPolicyService controlPolicy;
    private ToolPolicyEnforcer toolPolicyEnforcer;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        useCaseRegistry = new UseCaseRegistryService();
        useCaseRegistry.init();
        controlPolicy = Mockito.mock(ControlPolicyService.class);
        toolPolicyEnforcer = Mockito.mock(ToolPolicyEnforcer.class);
        Mockito.when(controlPolicy.getMaxBotTurnsFaq()).thenReturn(6);
        Mockito.when(controlPolicy.getMaxBotTurnsIntake()).thenReturn(8);

        builder = new ContextProjectionBuilder(objectMapper, useCaseRegistry,
                controlPolicy, toolPolicyEnforcer,
                SkillTestFixtures.productionRegistry());
        builder.initToolSchemas();
    }

    // -------------------- helpers --------------------

    private BotSession session(String activeUc) {
        BotSession s = new BotSession();
        s.setSessionId("sess-pucc");
        s.setCurrentPhase("RESOLVE");
        s.setActiveUseCase(activeUc);
        s.setTotalBotTurns(0);
        s.setClarificationCount(0);
        s.setFaqMissCount(0);
        s.setHandlingState("BOT_HANDLING");
        return s;
    }

    private BotTurn turn(int idx, String activeUc, String phaseAfter, String... sourceIds) {
        BotTurn t = new BotTurn();
        t.setTurnId("t-" + idx);
        t.setSessionId("sess-pucc");
        t.setTurnIndex(idx);
        t.setUserMessage("user msg " + idx);
        t.setBotResponse("bot msg " + idx);
        t.setActiveUseCase(activeUc);
        t.setPhaseBefore(phaseAfter);
        t.setPhaseAfter(phaseAfter);
        t.setSourceIds(sourceIds.length == 0 ? null : sourceIds);
        return t;
    }

    private JsonNode buildAndExtractSlot(BotSession s, List<BotTurn> history) throws Exception {
        String json = builder.buildProjection(s, history, null, "next message");
        return objectMapper.readTree(json).get("prior_use_case_carry");
    }

    // -------------------- positive cases --------------------

    @Test
    void slot_populatedOnSimpleUcSwitch_shape() throws Exception {
        BotSession s = session("UC-G");  // current
        List<BotTurn> history = List.of(
                turn(0, "UC-A", "RESOLVE", "ka-001"),
                turn(1, "UC-A", "RESOLVE", "ka-002")
        );
        JsonNode slot = buildAndExtractSlot(s, history);
        assertNotNull(slot);
        assertFalse(slot.isNull());
        assertEquals("UC-A", slot.get("prior_active_use_case").asText());
        assertNotNull(slot.get("prior_skill_name"));
        // RESOLVE phase + UC-A → resolve_faq_grounded_answer Skill.
        assertEquals("resolve_faq_grounded_answer",
                slot.get("prior_skill_name").asText());
        assertEquals(4, slot.get("ages_out_after_turns").asInt());
        JsonNode citations = slot.get("prior_citations");
        assertTrue(citations.isArray());
        assertEquals(2, citations.size());
    }

    @Test
    void slot_citationsCappedAtThree() throws Exception {
        BotSession s = session("UC-G");
        List<BotTurn> history = new ArrayList<>();
        // Five UC-A turns, each with 1 citation.
        for (int i = 0; i < 5; i++) {
            history.add(turn(i, "UC-A", "RESOLVE", "ka-00" + i));
        }
        JsonNode slot = buildAndExtractSlot(s, history);
        assertEquals(3, slot.get("prior_citations").size());
        // Cap takes the most-recent first (descending turn index).
        assertEquals("ka-004", slot.get("prior_citations").get(0).get("source_id").asText());
    }

    @Test
    void slot_citationsRespectCapAcrossMultiCiteTurn() throws Exception {
        BotSession s = session("UC-G");
        List<BotTurn> history = List.of(
                turn(0, "UC-A", "RESOLVE", "ka-001", "ka-002", "ka-003", "ka-004")
        );
        JsonNode slot = buildAndExtractSlot(s, history);
        // Cap stops at 3 even within a single turn's sourceIds array.
        assertEquals(3, slot.get("prior_citations").size());
    }

    @Test
    void slot_citationEntriesContainSourceIdFromUcAndTurnIndex() throws Exception {
        BotSession s = session("UC-G");
        List<BotTurn> history = List.of(
                turn(0, "UC-A", "RESOLVE", "ka-001")
        );
        JsonNode slot = buildAndExtractSlot(s, history);
        JsonNode cite = slot.get("prior_citations").get(0);
        assertEquals("ka-001", cite.get("source_id").asText());
        assertEquals("UC-A", cite.get("from_use_case").asText());
        assertEquals(0, cite.get("turn_index").asInt());
    }

    @Test
    void slot_skillNameResolvedForResolveIntakePriorPath() throws Exception {
        BotSession s = session("UC-A");
        List<BotTurn> history = List.of(
                turn(0, "UC-G", "RESOLVE")
        );
        JsonNode slot = buildAndExtractSlot(s, history);
        assertEquals("UC-G", slot.get("prior_active_use_case").asText());
        // RESOLVE + UC-G → resolve_intake_collect_and_handover Skill.
        assertEquals("resolve_intake_collect_and_handover",
                slot.get("prior_skill_name").asText());
    }

    @Test
    void slot_skillNameNullWhenRegistryMisses() throws Exception {
        BotSession s = session("UC-A");
        // Unknown phase "FOO" - registry select misses, prior_skill_name=null.
        List<BotTurn> history = List.of(
                turn(0, "UC-G", "FOO")
        );
        JsonNode slot = buildAndExtractSlot(s, history);
        assertTrue(slot.get("prior_skill_name").isNull());
    }

    // -------------------- aging window --------------------

    @Test
    void slot_dropsToNullWhenSwitchIsExactlyAgingWindowPlusOneTurnsAgo() throws Exception {
        BotSession s = session("UC-G");
        // priorTurnIdx=0, lastTurnIdx=4, currentTurnIdx=5 → diff=5 > 4 → drop.
        List<BotTurn> history = List.of(
                turn(0, "UC-A", "RESOLVE", "ka-001"),
                turn(1, "UC-G", "RESOLVE"),
                turn(2, "UC-G", "RESOLVE"),
                turn(3, "UC-G", "RESOLVE"),
                turn(4, "UC-G", "RESOLVE")
        );
        JsonNode slot = buildAndExtractSlot(s, history);
        assertTrue(slot.isNull());
    }

    @Test
    void slot_survivesAtBoundaryWhenSwitchIsExactlyAgingWindowTurnsAgo() throws Exception {
        BotSession s = session("UC-G");
        // priorTurnIdx=0, lastTurnIdx=3, currentTurnIdx=4 → diff=4 ≤ 4 → keep.
        List<BotTurn> history = List.of(
                turn(0, "UC-A", "RESOLVE", "ka-001"),
                turn(1, "UC-G", "RESOLVE"),
                turn(2, "UC-G", "RESOLVE"),
                turn(3, "UC-G", "RESOLVE")
        );
        JsonNode slot = buildAndExtractSlot(s, history);
        assertFalse(slot.isNull());
        assertEquals("UC-A", slot.get("prior_active_use_case").asText());
    }

    // -------------------- null / no-switch cases --------------------

    @Test
    void slot_nullOnEmptyHistory() throws Exception {
        BotSession s = session("UC-A");
        JsonNode slot = buildAndExtractSlot(s, List.of());
        assertTrue(slot.isNull());
    }

    @Test
    void slot_nullOnSingleUcSession() throws Exception {
        BotSession s = session("UC-A");
        List<BotTurn> history = List.of(
                turn(0, "UC-A", "RESOLVE", "ka-001"),
                turn(1, "UC-A", "RESOLVE", "ka-002")
        );
        JsonNode slot = buildAndExtractSlot(s, history);
        assertTrue(slot.isNull());
    }

    @Test
    void slot_nullWhenCurrentActiveUcIsNullDiscoverPreClassification() throws Exception {
        BotSession s = session(null);  // DISCOVER before classify_use_case
        List<BotTurn> history = List.of(
                turn(0, "UC-A", "RESOLVE", "ka-001")
        );
        JsonNode slot = buildAndExtractSlot(s, history);
        assertTrue(slot.isNull());
    }

    @Test
    void slot_ignoresTurnsWithNullActiveUc() throws Exception {
        // First DISCOVER turn had no committed UC; only the prior UC switch
        // chain (UC-A then current UC-G) is meaningful.
        BotSession s = session("UC-G");
        List<BotTurn> history = List.of(
                turn(0, null, "DISCOVER"),
                turn(1, "UC-A", "RESOLVE", "ka-007")
        );
        JsonNode slot = buildAndExtractSlot(s, history);
        assertFalse(slot.isNull());
        assertEquals("UC-A", slot.get("prior_active_use_case").asText());
    }

    // -------------------- no per-UC variation --------------------

    @Test
    void slot_shapeIsIdenticalAcrossDifferentSwitchPairs() throws Exception {
        BotSession s1 = session("UC-G");
        BotSession s2 = session("UC-H");
        s2.setSessionId("sess-pucc-2");
        List<BotTurn> h1 = List.of(turn(0, "UC-A", "RESOLVE", "ka-001"));
        List<BotTurn> h2 = List.of(turn(0, "UC-B", "RESOLVE", "ka-001"));

        JsonNode slot1 = buildAndExtractSlot(s1, h1);
        JsonNode slot2 = buildAndExtractSlot(s2, h2);

        // Both must have the same field names (shape invariance per §1.7).
        List<String> n1 = new ArrayList<>();
        slot1.fieldNames().forEachRemaining(n1::add);
        List<String> n2 = new ArrayList<>();
        slot2.fieldNames().forEachRemaining(n2::add);
        assertEquals(n1, n2);
        assertEquals(slot1.get("ages_out_after_turns").asInt(),
                slot2.get("ages_out_after_turns").asInt());
    }
}
