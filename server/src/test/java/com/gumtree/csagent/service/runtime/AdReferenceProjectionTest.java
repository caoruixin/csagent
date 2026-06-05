package com.gumtree.csagent.service.runtime;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gumtree.csagent.model.BotSession;
import com.gumtree.csagent.model.PhasePlan;
import com.gumtree.csagent.model.TerminalOutcome;
import com.gumtree.csagent.model.ToolCall;
import com.gumtree.csagent.model.ToolEvent;
import com.gumtree.csagent.service.runtime.ContextProjectionBuilder.ListingLookupState;
import com.gumtree.csagent.service.tools.ToolPolicyEnforcer;
import com.gumtree.csagent.service.tools.ToolResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;

/**
 * Sprint 078 / S-Auto-23 / M-Auto-6 Sub-sprint A — R4.a #7 {@code ad_reference}
 * structured fields.
 *
 * <p>Anti-误杀 invariant #11: {@code listing_lookup} MUST distinguish
 * {@code missing} (ran-but-no-result) from {@code skipped} (never-ran) —
 * collapsing them removes load-bearing premise signal. Block content is
 * runtime ground-truth only (no reason text, no LLM-generated string), and the
 * block is emitted on every live turn regardless of active UC. R4 adds a new
 * field; existing projection fields are unchanged in shape.
 */
@ExtendWith(MockitoExtension.class)
class AdReferenceProjectionTest {

    private ContextProjectionBuilder builder;
    private ObjectMapper objectMapper;

    @Mock
    private UseCaseRegistryService useCaseRegistry;
    @Mock
    private ControlPolicyService controlPolicy;
    @Mock
    private ToolPolicyEnforcer toolPolicyEnforcer;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        builder = new ContextProjectionBuilder(objectMapper, useCaseRegistry,
                controlPolicy, toolPolicyEnforcer, null);
        builder.initToolSchemas();
        lenient().when(controlPolicy.getMaxBotTurnsFaq()).thenReturn(6);
        lenient().when(controlPolicy.getMaxClarificationRounds()).thenReturn(2);
        lenient().when(controlPolicy.getMaxFaqMiss()).thenReturn(2);
        lenient().when(toolPolicyEnforcer.getVisibleToolsForUc(anyString()))
                .thenReturn(List.of("request_handover", "lookup_listing_or_ad"));
    }

    // --- #7 positive (4): state derivation + token ---

    @Test
    void deriveState_ok() {
        assertEquals(ListingLookupState.OK,
                ContextProjectionBuilder.deriveListingLookupState(true, true, true));
        assertEquals("ok", ContextProjectionBuilder.listingLookupToken(ListingLookupState.OK));
    }

    @Test
    void deriveState_missing_ranButNoResult() {
        assertEquals(ListingLookupState.MISSING,
                ContextProjectionBuilder.deriveListingLookupState(true, true, false));
        assertEquals("missing", ContextProjectionBuilder.listingLookupToken(ListingLookupState.MISSING));
    }

    @Test
    void deriveState_failed() {
        assertEquals(ListingLookupState.FAILED,
                ContextProjectionBuilder.deriveListingLookupState(true, false, false));
        assertEquals("failed", ContextProjectionBuilder.listingLookupToken(ListingLookupState.FAILED));
    }

    @Test
    void deriveState_skipped_neverRan() {
        assertEquals(ListingLookupState.SKIPPED,
                ContextProjectionBuilder.deriveListingLookupState(false, false, false));
        assertEquals("skipped", ContextProjectionBuilder.listingLookupToken(ListingLookupState.SKIPPED));
    }

    @Test
    void missingIsDistinctFromSkipped() {
        // The load-bearing distinction: ran-but-no-result vs never-ran.
        ListingLookupState missing =
                ContextProjectionBuilder.deriveListingLookupState(true, true, false);
        ListingLookupState skipped =
                ContextProjectionBuilder.deriveListingLookupState(false, false, false);
        assertNotEquals(missing, skipped);
        assertNotEquals(ContextProjectionBuilder.listingLookupToken(missing),
                ContextProjectionBuilder.listingLookupToken(skipped));
    }

    // --- #7 positive: form_ad_id literal/null + listing_lookup wiring via build() ---

    @Test
    void adReference_okWhenLookupResolvesListing() throws Exception {
        JsonNode adRef = adReference("UC-A", "{\"email\":\"a@b.com\",\"ad_id\":\"ad-9\"}",
                List.of(lookupEvent(ToolResult.ok(Map.of("found", true,
                        "listing", Map.of("id", "ad-9"))))));
        assertEquals("ad-9", adRef.get("form_ad_id").asText());
        assertEquals("ok", adRef.get("listing_lookup").asText());
    }

    @Test
    void adReference_missingWhenLookupRanButNoListing() throws Exception {
        JsonNode adRef = adReference("UC-A", "{\"email\":\"a@b.com\",\"ad_id\":\"ad-9\"}",
                List.of(lookupEvent(ToolResult.ok(Map.of("found", false)))));
        assertEquals("missing", adRef.get("listing_lookup").asText());
    }

    @Test
    void adReference_failedWhenLookupErrored() throws Exception {
        JsonNode adRef = adReference("UC-A", "{\"email\":\"a@b.com\",\"ad_id\":\"ad-9\"}",
                List.of(lookupEvent(ToolResult.error("transport boom"))));
        assertEquals("failed", adRef.get("listing_lookup").asText());
    }

    @Test
    void adReference_skippedWhenNeverRan_andNullAdId() throws Exception {
        // c7 surface: no ad_id, lookup never ran.
        JsonNode adRef = adReference("UC-A", "{\"email\":\"a@b.com\"}", List.of());
        assertTrue(adRef.get("form_ad_id").isNull(),
                "form_ad_id must be JSON null when ad_id is absent");
        assertEquals("skipped", adRef.get("listing_lookup").asText());
    }

    // --- #7 negative (2) ---

    @Test
    void adReference_emittedRegardlessOfActiveUc() throws Exception {
        // UC-A with ad_id, UC-J without ad_id, UC-D — all emit ad_reference.
        assertTrue(hasAdReference("UC-A", "{\"email\":\"a@b.com\",\"ad_id\":\"ad-1\"}"));
        assertTrue(hasAdReference("UC-J", "{\"email\":\"a@b.com\"}"));
        assertTrue(hasAdReference("UC-D", "{\"email\":\"a@b.com\"}"));
    }

    @Test
    void adReference_containsOnlyStructuralKeys_noReasonText() throws Exception {
        JsonNode adRef = adReference("UC-A", "{\"email\":\"a@b.com\",\"ad_id\":\"ad-9\"}", List.of());
        List<String> keys = new ArrayList<>();
        adRef.fieldNames().forEachRemaining(keys::add);
        assertEquals(Set.of("form_ad_id", "listing_lookup"), Set.copyOf(keys),
                "ad_reference must carry ONLY structural fields — no reason text / LLM string");
        assertTrue(Set.of("ok", "missing", "failed", "skipped")
                        .contains(adRef.get("listing_lookup").asText()),
                "listing_lookup must be one of the four structural tokens");
    }

    // --- Anti-误杀: downstream consumers unchanged in shape ---

    @Test
    void downstreamProjectionFieldsUnchangedInShape() throws Exception {
        JsonNode root = buildRoot("UC-J", "INTAKE", "{\"email\":\"a@b.com\"}",
                Map.of("search_knowledge", Map.of("hits", List.of("ka1"))), List.of());
        // R4 adds new fields; existing fields keep their shape.
        assertTrue(root.has("ad_reference"), "ad_reference must be present");
        assertTrue(root.has("customer_context_status"));
        assertTrue(root.has("form_context"), "form_context must still be projected");
        assertEquals("a@b.com", root.path("form_context").path("email").asText());
        assertTrue(root.has("accumulated_tool_results"),
                "accumulated_tool_results must still be projected unchanged");
        assertTrue(root.path("accumulated_tool_results").has("search_knowledge"));
        assertTrue(root.has("intake_state"), "intake_state must still be projected for intake UC");
        assertFalse(root.path("form_context").has("ad_reference"),
                "ad_reference must be a top-level field, not injected into form_context");
    }

    // --- helpers ---

    private ToolEvent lookupEvent(ToolResult result) {
        return ToolEvent.of(0, ToolCall.builder().name("lookup_listing_or_ad")
                .arguments(Map.of("ad_id", "ad-9")).build(), result, 5L);
    }

    private JsonNode adReference(String uc, String formContext, List<ToolEvent> events) throws Exception {
        return buildRoot(uc, "RESOLVE", formContext, null, events).get("ad_reference");
    }

    private boolean hasAdReference(String uc, String formContext) throws Exception {
        return buildRoot(uc, "RESOLVE", formContext, null, List.of()).has("ad_reference");
    }

    private JsonNode buildRoot(String uc, String phase, String formContext,
                               Map<String, Object> accumulated, List<ToolEvent> events) throws Exception {
        BotSession session = BotSession.builder()
                .sessionId("sprint078-r4-ad-test")
                .activeUseCase(uc)
                .currentPhase(phase)
                .handlingState("BOT_HANDLING")
                .totalBotTurns(0)
                .clarificationCount(0)
                .faqMissCount(0)
                .repeatedActionCount(0)
                .formContext(formContext)
                .build();
        PhasePlan plan = PhasePlan.builder()
                .phase(phase)
                .useCase(uc)
                .objective("o")
                .allowedTools(List.of("request_handover", "lookup_listing_or_ad"))
                .maxToolSteps(3)
                .systemInstruction(phase)
                .validTerminalOutcomes(Set.of(TerminalOutcome.ESCALATE,
                        TerminalOutcome.FINAL_ANSWER))
                .build();
        String json = builder.build(session, List.of(), plan, "hello", accumulated,
                events == null ? List.of() : events);
        return objectMapper.readTree(json);
    }
}
