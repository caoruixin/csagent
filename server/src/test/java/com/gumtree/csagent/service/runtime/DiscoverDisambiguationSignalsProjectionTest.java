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
 * Sprint 33 — regression coverage for the
 * {@code discover_disambiguation_signals} projection slot.
 *
 * <p>Three behaviour bars pinned at the {@link ContextProjectionBuilder}
 * unit layer:
 *
 * <ul>
 *   <li><b>Signal fires on REMOVED+multi-candidate topic.</b>
 *       A listing in REMOVED state combined with a form
 *       {@code topic_subject} whose registry candidate list has
 *       more than one entry populates {@code ad_status_observed}
 *       and sets {@code topic_subject_carries_multiple_candidate_ucs}
 *       to {@code true} with the registry's candidate list.</li>
 *   <li><b>Signal does not fire when premises are missing.</b>
 *       LIVE listings, single-candidate topics, or absent
 *       {@code listing_context} produce null / false / empty
 *       sub-fields. Slot is still present (shape stability).</li>
 *   <li><b>Schema stability.</b> The slot key is present in every
 *       projection — never omitted — and the three sub-fields
 *       have stable types regardless of input.</li>
 * </ul>
 *
 * <p>The non-enforcement bar (the runtime does NOT branch on the
 * slot's value) is covered by the companion integration test
 * {@code AgentRunLoopDiscoverDisambiguationNonEnforcementIntegrationTest},
 * mirroring the Sprint 31 split between
 * {@link IntakeAmbiguousCandidatesProjectionTest} and the
 * {@code AgentRunLoopAlternateCandidateUseCasesNonEnforcementIntegrationTest}.
 */
@ExtendWith(MockitoExtension.class)
class DiscoverDisambiguationSignalsProjectionTest {

    @Mock private UseCaseRegistryService useCaseRegistry;
    @Mock private ControlPolicyService controlPolicy;
    @Mock private ToolPolicyEnforcer toolPolicyEnforcer;

    private ObjectMapper objectMapper;
    private ContextProjectionBuilder projectionBuilder;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        projectionBuilder = new ContextProjectionBuilder(
                objectMapper, useCaseRegistry, controlPolicy, toolPolicyEnforcer);
        projectionBuilder.initToolSchemas();

        lenient().when(controlPolicy.getMaxBotTurnsFaq()).thenReturn(6);
        lenient().when(controlPolicy.getMaxClarificationRounds()).thenReturn(3);
        lenient().when(controlPolicy.getMaxFaqMiss()).thenReturn(2);
    }

    // -------------------------------------------------------------------
    // Bar 1 — Signal fires on REMOVED listing + multi-candidate topic
    // -------------------------------------------------------------------

    @Test
    void removedListing_withMultiCandidateTopic_populatesSlot() throws Exception {
        when(useCaseRegistry.getCandidateUcsForTopic("Ad Support"))
                .thenReturn(List.of("UC-A", "UC-B", "UC-FP", "UC-H"));

        BotSession session = baseSession();
        session.setFormTopicSubject("Ad Support");
        session.setListingContext("{\"status\":\"REMOVED\",\"ad_id\":\"AD-2001\"}");

        JsonNode slot = readSlot(session, "where is my advert?");

        assertEquals("REMOVED", slot.get("ad_status_observed").asText(),
                "REMOVED listing must populate ad_status_observed.");
        assertTrue(slot.get("topic_subject_carries_multiple_candidate_ucs").asBoolean(),
                "Ad Support (4 candidate UCs) must set the multi-candidate flag.");
        JsonNode candidates = slot.get("candidate_ucs_for_topic");
        assertTrue(candidates.isArray() && candidates.size() == 4,
                "candidate_ucs_for_topic must mirror the registry list; got " + candidates);
        assertEquals("UC-A", candidates.get(0).asText());
        assertEquals("UC-H", candidates.get(3).asText());
    }

    @Test
    void suspendedListing_withMultiCandidateTopic_populatesAdStatus() throws Exception {
        when(useCaseRegistry.getCandidateUcsForTopic("Ad Support"))
                .thenReturn(List.of("UC-A", "UC-B", "UC-FP", "UC-H"));

        BotSession session = baseSession();
        session.setFormTopicSubject("Ad Support");
        session.setListingContext("{\"status\":\"SUSPENDED\",\"ad_id\":\"AD-3001\"}");

        JsonNode slot = readSlot(session, "why can't I see my ad");

        assertEquals("SUSPENDED", slot.get("ad_status_observed").asText(),
                "SUSPENDED listing must populate ad_status_observed.");
        assertTrue(slot.get("topic_subject_carries_multiple_candidate_ucs").asBoolean());
    }

    @Test
    void expiredListing_withMultiCandidateTopic_populatesAdStatus() throws Exception {
        when(useCaseRegistry.getCandidateUcsForTopic("Ad Support"))
                .thenReturn(List.of("UC-A", "UC-B", "UC-FP", "UC-H"));

        BotSession session = baseSession();
        session.setFormTopicSubject("Ad Support");
        session.setListingContext("{\"status\":\"EXPIRED\",\"ad_id\":\"AD-4001\"}");

        JsonNode slot = readSlot(session, "what happened to my ad");

        assertEquals("EXPIRED", slot.get("ad_status_observed").asText(),
                "EXPIRED listing must populate ad_status_observed.");
    }

    // -------------------------------------------------------------------
    // Bar 2 — Signal does not fire when premises are missing
    // -------------------------------------------------------------------

    @Test
    void liveListing_doesNotPopulateAdStatus() throws Exception {
        when(useCaseRegistry.getCandidateUcsForTopic("Ad Support"))
                .thenReturn(List.of("UC-A", "UC-B", "UC-FP", "UC-H"));

        BotSession session = baseSession();
        session.setFormTopicSubject("Ad Support");
        session.setListingContext("{\"status\":\"LIVE\",\"ad_id\":\"AD-9001\"}");

        JsonNode slot = readSlot(session, "is my ad still up");

        assertTrue(slot.get("ad_status_observed").isNull(),
                "LIVE listing must leave ad_status_observed null.");
        // Multi-candidate flag MUST still reflect the topic, independent
        // of listing status — the two sub-signals are computed in
        // parallel from independent observables.
        assertTrue(slot.get("topic_subject_carries_multiple_candidate_ucs").asBoolean(),
                "Topic multi-candidate flag is independent of ad status.");
    }

    @Test
    void singleCandidateTopic_clearsMultiCandidateFlag() throws Exception {
        // "Replies or Messaging" is strong-prior UC-C only; the registry
        // returns a single-candidate list for it.
        when(useCaseRegistry.getCandidateUcsForTopic("Replies or Messaging"))
                .thenReturn(List.of("UC-C"));

        BotSession session = baseSession();
        session.setFormTopicSubject("Replies or Messaging");
        session.setListingContext("{\"status\":\"REMOVED\",\"ad_id\":\"AD-5001\"}");

        JsonNode slot = readSlot(session, "my replies aren't coming through");

        // REMOVED listing still populates ad_status_observed.
        assertEquals("REMOVED", slot.get("ad_status_observed").asText());
        // But single-candidate topic clears the multi-candidate flag.
        assertFalse(slot.get("topic_subject_carries_multiple_candidate_ucs").asBoolean(),
                "Single-candidate topic must clear the multi-candidate flag.");
        assertEquals(0, slot.get("candidate_ucs_for_topic").size(),
                "Single-candidate topic must produce an empty candidate list.");
    }

    @Test
    void nullListingContext_leavesAdStatusNull() throws Exception {
        when(useCaseRegistry.getCandidateUcsForTopic("Ad Support"))
                .thenReturn(List.of("UC-A", "UC-B", "UC-FP", "UC-H"));

        BotSession session = baseSession();
        session.setFormTopicSubject("Ad Support");
        session.setListingContext(null);

        JsonNode slot = readSlot(session, "I have a general question about ads");

        assertTrue(slot.get("ad_status_observed").isNull(),
                "Null listing_context must leave ad_status_observed null.");
        // Multi-candidate flag still fires on topic alone.
        assertTrue(slot.get("topic_subject_carries_multiple_candidate_ucs").asBoolean());
    }

    @Test
    void nullTopicSubject_clearsMultiCandidateFlag() throws Exception {
        BotSession session = baseSession();
        session.setFormTopicSubject(null);
        session.setListingContext("{\"status\":\"REMOVED\",\"ad_id\":\"AD-6001\"}");

        JsonNode slot = readSlot(session, "hi");

        assertEquals("REMOVED", slot.get("ad_status_observed").asText());
        assertFalse(slot.get("topic_subject_carries_multiple_candidate_ucs").asBoolean(),
                "Null topic must clear the multi-candidate flag.");
        assertEquals(0, slot.get("candidate_ucs_for_topic").size(),
                "Null topic must produce an empty candidate list.");
    }

    // -------------------------------------------------------------------
    // Bar 3 — Schema stability across input shapes
    // -------------------------------------------------------------------

    @Test
    void schemaStability_slotPresentOnEveryProjection() throws Exception {
        lenient().when(useCaseRegistry.getCandidateUcsForTopic("Ad Support"))
                .thenReturn(List.of("UC-A", "UC-B", "UC-FP", "UC-H"));
        lenient().when(useCaseRegistry.getCandidateUcsForTopic("Replies or Messaging"))
                .thenReturn(List.of("UC-C"));

        for (String[] tuple : new String[][] {
                {null, null},
                {"Ad Support", null},
                {null, "{\"status\":\"REMOVED\"}"},
                {"Ad Support", "{\"status\":\"LIVE\"}"},
                {"Ad Support", "{\"status\":\"REMOVED\"}"},
                {"Replies or Messaging", "{\"status\":\"REMOVED\"}"}
        }) {
            BotSession session = baseSession();
            session.setFormTopicSubject(tuple[0]);
            session.setListingContext(tuple[1]);

            JsonNode root = parseProjection(session, "any message");
            assertTrue(root.has("discover_disambiguation_signals"),
                    "discover_disambiguation_signals key must be present for tuple ("
                            + tuple[0] + ", " + tuple[1] + ").");
            JsonNode slot = root.get("discover_disambiguation_signals");
            assertTrue(slot.isObject(),
                    "discover_disambiguation_signals must always be a JSON object.");
            assertTrue(slot.has("ad_status_observed"),
                    "ad_status_observed sub-field must always be present.");
            assertTrue(slot.has("topic_subject_carries_multiple_candidate_ucs"),
                    "topic_subject_carries_multiple_candidate_ucs sub-field must always be present.");
            assertTrue(slot.has("candidate_ucs_for_topic"),
                    "candidate_ucs_for_topic sub-field must always be present.");
            assertTrue(slot.get("candidate_ucs_for_topic").isArray(),
                    "candidate_ucs_for_topic must always be a JSON array.");
        }
    }

    @Test
    void listingContextWithoutStatus_leavesAdStatusNull() throws Exception {
        when(useCaseRegistry.getCandidateUcsForTopic("Ad Support"))
                .thenReturn(List.of("UC-A", "UC-B", "UC-FP", "UC-H"));

        BotSession session = baseSession();
        session.setFormTopicSubject("Ad Support");
        session.setListingContext("{\"ad_id\":\"AD-7001\"}");

        JsonNode slot = readSlot(session, "hi");
        assertTrue(slot.get("ad_status_observed").isNull(),
                "listing_context with no status field must leave ad_status_observed null.");
    }

    // --- Helpers ------------------------------------------------------

    private BotSession baseSession() {
        return BotSession.builder()
                .sessionId("sprint33-discover-disambig-test")
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

    private JsonNode readSlot(BotSession session, String userMessage) throws Exception {
        JsonNode root = parseProjection(session, userMessage);
        assertTrue(root.has("discover_disambiguation_signals"),
                "Projection must include discover_disambiguation_signals key.");
        JsonNode slot = root.get("discover_disambiguation_signals");
        assertNotNull(slot);
        assertTrue(slot.isObject(), "discover_disambiguation_signals must be a JSON object.");
        return slot;
    }
}
