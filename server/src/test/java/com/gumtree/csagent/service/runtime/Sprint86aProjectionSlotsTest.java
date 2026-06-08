package com.gumtree.csagent.service.runtime;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gumtree.csagent.model.BotSession;
import com.gumtree.csagent.service.tools.ToolPolicyEnforcer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * Sprint 86a / S-Auto-31 (M-Auto-7 S-Y1 Part A) — regression coverage for the
 * two data-derived projection slots that the CS4 autoloop pilot (S-Y2) will
 * reference. Layer {@code prompt_projection}; no skill-procedure text change.
 *
 * <ul>
 *   <li><b>{@code moderation_reason_available}</b> — presence-only boolean
 *       inside the {@code discover_disambiguation_signals} node. True iff
 *       {@code session.moderationContext} is present + non-blank. HARD FENCE:
 *       the raw moderation-review text / reason-code is NEVER emitted (anti-leak
 *       bar).</li>
 *   <li><b>{@code candidate_use_cases_named}</b> — NEW additive slot emitting
 *       {@code {id,name}} objects with names from the registry, null-safe on an
 *       unknown id. The existing bare-ID {@code candidate_use_cases} slot is
 *       UNCHANGED (backward-compat bar).</li>
 * </ul>
 *
 * <p>The gates default to TRUE (emit) here because {@code skillRegistry} is
 * {@code null} in this unit fixture — same defensive default the sibling
 * {@link DiscoverDisambiguationSignalsProjectionTest} and
 * {@link Sprint7CandidateUseCasesProjectionTest} rely on.
 */
@ExtendWith(MockitoExtension.class)
class Sprint86aProjectionSlotsTest {

    @Mock private UseCaseRegistryService useCaseRegistry;
    @Mock private ControlPolicyService controlPolicy;
    @Mock private ToolPolicyEnforcer toolPolicyEnforcer;

    private ObjectMapper objectMapper;
    private ContextProjectionBuilder projectionBuilder;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        projectionBuilder = new ContextProjectionBuilder(
                objectMapper, useCaseRegistry, controlPolicy, toolPolicyEnforcer, null);
        projectionBuilder.initToolSchemas();

        lenient().when(controlPolicy.getMaxBotTurnsFaq()).thenReturn(6);
        lenient().when(controlPolicy.getMaxClarificationRounds()).thenReturn(3);
        lenient().when(controlPolicy.getMaxFaqMiss()).thenReturn(2);
    }

    // -------------------------------------------------------------------
    // A1 — moderation_reason_available (presence-only boolean)
    // -------------------------------------------------------------------

    @Test
    void moderationReasonAvailable_true_whenModerationContextPresent() throws Exception {
        BotSession session = baseSession();
        session.setModerationContext("{\"reason\":\"PROHIBITED_ITEM\"}");

        JsonNode slot = readDisambiguationSlot(session, "why was my ad removed?");

        assertTrue(slot.has("moderation_reason_available"),
                "moderation_reason_available sub-field must be present.");
        assertTrue(slot.get("moderation_reason_available").asBoolean(),
                "A present, non-blank moderationContext must set the boolean true.");
    }

    @Test
    void moderationReasonAvailable_false_whenNull() throws Exception {
        BotSession session = baseSession();
        session.setModerationContext(null);

        JsonNode slot = readDisambiguationSlot(session, "where is my ad?");

        assertTrue(slot.has("moderation_reason_available"));
        assertFalse(slot.get("moderation_reason_available").asBoolean(),
                "Null moderationContext must set the boolean false.");
    }

    @Test
    void moderationReasonAvailable_false_whenBlank() throws Exception {
        BotSession session = baseSession();
        session.setModerationContext("   ");

        JsonNode slot = readDisambiguationSlot(session, "where is my ad?");

        assertFalse(slot.get("moderation_reason_available").asBoolean(),
                "Blank moderationContext must set the boolean false.");
    }

    @Test
    void antiLeak_rawModerationTextNeverEmitted() throws Exception {
        // The moderationContext carries a recognizable reason-code + free-text
        // note. The projection must surface ONLY the boolean — never the text.
        BotSession session = baseSession();
        session.setModerationContext(
                "{\"reason_code\":\"WEAPONS_PROHIBITED\","
                        + "\"reviewer_note\":\"SENSITIVE-MOD-NOTE-XYZ\"}");

        String projection = projectionBuilder.buildProjection(
                session, List.of(), null, "why was my ad removed?");

        assertTrue(projection.contains("\"moderation_reason_available\":true"),
                "The presence boolean must be projected.");
        assertFalse(projection.contains("SENSITIVE-MOD-NOTE-XYZ"),
                "Raw moderation reviewer note must NEVER appear in the projection.");
        assertFalse(projection.contains("WEAPONS_PROHIBITED"),
                "Raw moderation reason-code value must NEVER appear in the projection.");
        assertFalse(projection.contains("reviewer_note"),
                "No moderation-review field name should leak into the projection.");
    }

    // -------------------------------------------------------------------
    // A2 — candidate_use_cases_named ({id,name}, null-safe)
    // -------------------------------------------------------------------

    @Test
    void candidateUseCasesNamed_emitsIdAndName_fromRegistry() throws Exception {
        when(useCaseRegistry.getUseCase("UC-A")).thenReturn(ucDef("UC-A", "Ad Status & Visibility"));
        when(useCaseRegistry.getUseCase("UC-FP")).thenReturn(ucDef("UC-FP", "Correct Deletion Explanation"));

        BotSession session = baseSession();
        session.setCandidateUseCases(new String[]{"UC-A", "UC-FP"});

        JsonNode named = readNamedSlot(session, "what happened to my ad?");

        assertEquals(2, named.size(), "Two candidates in → two named entries out.");
        assertEquals("UC-A", named.get(0).get("id").asText());
        assertEquals("Ad Status & Visibility", named.get(0).get("name").asText());
        assertEquals("UC-FP", named.get(1).get("id").asText());
        assertEquals("Correct Deletion Explanation", named.get(1).get("name").asText());
    }

    @Test
    void candidateUseCasesNamed_nullSafeOnUnknownId_fallsBackToId() throws Exception {
        when(useCaseRegistry.getUseCase("UC-A")).thenReturn(ucDef("UC-A", "Ad Status & Visibility"));
        when(useCaseRegistry.getUseCase("UC-ZZZ")).thenReturn(null);

        BotSession session = baseSession();
        session.setCandidateUseCases(new String[]{"UC-A", "UC-ZZZ"});

        JsonNode named = readNamedSlot(session, "hi");

        assertEquals(2, named.size());
        assertEquals("Ad Status & Visibility", named.get(0).get("name").asText());
        assertEquals("UC-ZZZ", named.get(1).get("id").asText());
        assertEquals("UC-ZZZ", named.get(1).get("name").asText(),
                "An unknown id must fall back to the id as the name (null-safe).");
    }

    @Test
    void candidateUseCasesNamed_skipsNullAndBlankIds() throws Exception {
        lenient().when(useCaseRegistry.getUseCase("UC-A")).thenReturn(ucDef("UC-A", "Ad Status & Visibility"));

        BotSession session = baseSession();
        session.setCandidateUseCases(new String[]{"UC-A", null, "  "});

        JsonNode named = readNamedSlot(session, "hi");

        assertEquals(1, named.size(), "Null / blank candidate ids must be skipped.");
        assertEquals("UC-A", named.get(0).get("id").asText());
    }

    @Test
    void candidateUseCasesNamed_emptyArrayWhenNoCandidates() throws Exception {
        BotSession session = baseSession();
        session.setCandidateUseCases(null);

        JsonNode root = parseProjection(session, "hi");
        assertTrue(root.has("candidate_use_cases_named"),
                "Slot must be present (empty array) even when no candidates exist.");
        assertTrue(root.get("candidate_use_cases_named").isArray());
        assertEquals(0, root.get("candidate_use_cases_named").size());
    }

    // -------------------------------------------------------------------
    // Backward-compat — existing candidate_use_cases shape UNCHANGED
    // -------------------------------------------------------------------

    @Test
    void candidateUseCases_backwardCompat_stillBareStringArray() throws Exception {
        when(useCaseRegistry.getUseCase("UC-A")).thenReturn(ucDef("UC-A", "Ad Status & Visibility"));
        when(useCaseRegistry.getUseCase("UC-FP")).thenReturn(ucDef("UC-FP", "Correct Deletion Explanation"));

        BotSession session = baseSession();
        session.setCandidateUseCases(new String[]{"UC-A", "UC-FP"});

        JsonNode root = parseProjection(session, "what happened to my ad?");

        JsonNode bare = root.get("candidate_use_cases");
        assertNotNull(bare, "Existing candidate_use_cases slot must still be emitted.");
        assertTrue(bare.isArray());
        assertEquals(2, bare.size());
        // Every element is a bare string id — NOT an object. Shape unchanged.
        assertTrue(bare.get(0).isTextual(), "candidate_use_cases must remain a string array.");
        assertEquals("UC-A", bare.get(0).asText());
        assertEquals("UC-FP", bare.get(1).asText());
    }

    // -------------------------------------------------------------------
    // Regression — existing discover_disambiguation_signals fields untouched
    // -------------------------------------------------------------------

    @Test
    void existingDisambiguationFields_unchanged_alongsideNewBoolean() throws Exception {
        when(useCaseRegistry.getCandidateUcsForTopic("Ad Support"))
                .thenReturn(List.of("UC-A", "UC-B", "UC-FP", "UC-H"));

        BotSession session = baseSession();
        session.setFormTopicSubject("Ad Support");
        session.setListingContext("{\"status\":\"REMOVED\",\"ad_id\":\"AD-8001\"}");
        session.setModerationContext("{\"reason\":\"PROHIBITED_ITEM\"}");

        JsonNode slot = readDisambiguationSlot(session, "why was my ad removed?");

        // Pre-existing three sub-fields keep their values + types.
        assertEquals("REMOVED", slot.get("ad_status_observed").asText());
        assertTrue(slot.get("topic_subject_carries_multiple_candidate_ucs").asBoolean());
        assertTrue(slot.get("candidate_ucs_for_topic").isArray());
        assertEquals(4, slot.get("candidate_ucs_for_topic").size());
        // New additive boolean coexists.
        assertTrue(slot.get("moderation_reason_available").asBoolean());
    }

    // --- Helpers ------------------------------------------------------

    private static UseCaseRegistryService.UseCaseDefinition ucDef(String id, String name) {
        return new UseCaseRegistryService.UseCaseDefinition(
                id, name, List.of("Ad Support"), "LOW", true, "FAQ");
    }

    private BotSession baseSession() {
        return BotSession.builder()
                .sessionId("sprint86a-projection-slots-test")
                .currentPhase("DISCOVER")
                .handlingState("BOT_HANDLING")
                .totalBotTurns(0)
                .clarificationCount(0)
                .faqMissCount(0)
                .repeatedActionCount(0)
                .build();
    }

    private JsonNode parseProjection(BotSession session, String userMessage) throws Exception {
        String projection = projectionBuilder.buildProjection(
                session, List.of(), null, userMessage);
        return objectMapper.readTree(projection);
    }

    private JsonNode readDisambiguationSlot(BotSession session, String userMessage) throws Exception {
        JsonNode root = parseProjection(session, userMessage);
        assertTrue(root.has("discover_disambiguation_signals"),
                "Projection must include discover_disambiguation_signals key.");
        JsonNode slot = root.get("discover_disambiguation_signals");
        assertNotNull(slot);
        assertTrue(slot.isObject());
        return slot;
    }

    private JsonNode readNamedSlot(BotSession session, String userMessage) throws Exception {
        JsonNode root = parseProjection(session, userMessage);
        assertTrue(root.has("candidate_use_cases_named"),
                "Projection must include candidate_use_cases_named key.");
        JsonNode named = root.get("candidate_use_cases_named");
        assertNotNull(named);
        assertTrue(named.isArray(), "candidate_use_cases_named must be a JSON array.");
        return named;
    }
}
