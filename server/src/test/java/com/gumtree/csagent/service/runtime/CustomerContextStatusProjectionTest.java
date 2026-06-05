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

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;

/**
 * Sprint 078 / S-Auto-23 / M-Auto-6 Sub-sprint A — R4.a #6
 * {@code customer_context_status} enum slot.
 *
 * <p>Anti-误杀 invariant #10: the priority order is LOAD-BEARING —
 * {@code missing_email} / {@code missing_ad_id} / {@code lookup_failed} /
 * {@code lookup_skipped} take precedence over {@code loaded} so a populated
 * customer context never masks an absent ad_id premise (the c7 scenario).
 * Anti-误杀 invariant #8: the enum derives ENTIRELY from runtime state — zero
 * keyword matching of user-message content.
 */
@ExtendWith(MockitoExtension.class)
class CustomerContextStatusProjectionTest {

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

    // --- #6 positive (5): one per enum value (pure helper) ---

    @Test
    void missingEmail() {
        assertEquals("missing_email", ContextProjectionBuilder.computeCustomerContextStatus(
                null, "ad-123", ListingLookupState.SKIPPED));
        assertEquals("missing_email", ContextProjectionBuilder.computeCustomerContextStatus(
                "  ", "ad-123", ListingLookupState.SKIPPED));
    }

    @Test
    void missingAdId() {
        assertEquals("missing_ad_id", ContextProjectionBuilder.computeCustomerContextStatus(
                "a@b.com", null, ListingLookupState.SKIPPED));
        assertEquals("missing_ad_id", ContextProjectionBuilder.computeCustomerContextStatus(
                "a@b.com", "", ListingLookupState.SKIPPED));
    }

    @Test
    void lookupFailed() {
        assertEquals("lookup_failed", ContextProjectionBuilder.computeCustomerContextStatus(
                "a@b.com", "ad-123", ListingLookupState.FAILED));
    }

    @Test
    void lookupSkipped() {
        assertEquals("lookup_skipped", ContextProjectionBuilder.computeCustomerContextStatus(
                "a@b.com", "ad-123", ListingLookupState.SKIPPED));
    }

    @Test
    void loaded() {
        assertEquals("loaded", ContextProjectionBuilder.computeCustomerContextStatus(
                "a@b.com", "ad-123", ListingLookupState.OK));
    }

    // --- #6 priority-order (4, CRITICAL) ---

    @Test
    void priority_loadedCustomerContextWithNullAdId_missingAdIdWins() {
        // c7 scenario — get_customer_context loaded (lookup OK) BUT no ad_id.
        // missing_ad_id MUST win, else R4's purpose is defeated.
        assertEquals("missing_ad_id", ContextProjectionBuilder.computeCustomerContextStatus(
                "a@b.com", null, ListingLookupState.OK));
    }

    @Test
    void priority_loadedCustomerContextWithNullEmail_missingEmailWins() {
        assertEquals("missing_email", ContextProjectionBuilder.computeCustomerContextStatus(
                null, "ad-123", ListingLookupState.OK));
    }

    @Test
    void priority_lookupFailedOverridesLoaded() {
        assertEquals("lookup_failed", ContextProjectionBuilder.computeCustomerContextStatus(
                "a@b.com", "ad-123", ListingLookupState.FAILED));
    }

    @Test
    void priority_noKeywordLeakage_userMessagePhraseDoesNotShiftEnum() throws Exception {
        // (d) "my ad" / "my listing" in the user message WITHOUT a runtime
        // state change must NOT shift the enum — derivation is structural only.
        String neutral = projectionStatus("UC-A", "RESOLVE",
                "{\"email\":\"a@b.com\"}", null, "hello");
        String withAdPhrase = projectionStatus("UC-A", "RESOLVE",
                "{\"email\":\"a@b.com\"}", null,
                "why is my ad / my listing offline, my advert disappeared");
        assertEquals("missing_ad_id", neutral);
        assertEquals(neutral, withAdPhrase,
                "user-message keywords must NOT change customer_context_status");
    }

    // --- #6 negative (2) ---

    @Test
    void enumIndependentOfActiveUc() throws Exception {
        // (a) Same runtime state, different active UC -> same enum.
        String formNoAd = "{\"email\":\"a@b.com\"}";
        String ucA = projectionStatus("UC-A", "RESOLVE", formNoAd, null, "hi");
        String ucJ = projectionStatus("UC-J", "INTAKE", formNoAd, null, "hi");
        String ucD = projectionStatus("UC-D", "RESOLVE", formNoAd, null, "hi");
        assertEquals("missing_ad_id", ucA);
        assertEquals(ucA, ucJ);
        assertEquals(ucA, ucD);
    }

    @Test
    void enumIndependentOfOtherProjectionFields() throws Exception {
        // (b) Toggling phase (which toggles the budgets.clarification field)
        // must not change the enum for identical form/tool state.
        String formNoAd = "{\"email\":\"a@b.com\"}";
        String discover = projectionStatus("UC-A", "DISCOVER", formNoAd, null, "hi");
        String resolve = projectionStatus("UC-A", "RESOLVE", formNoAd, null, "hi");
        assertEquals(discover, resolve);
    }

    @Test
    void withAdIdAndLookupOk_isLoaded() throws Exception {
        // Positive integration: ad_id present + a successful lookup_listing_or_ad
        // event -> loaded, and NOT missing_ad_id (with-ad_id negative gate).
        ToolEvent lookupOk = ToolEvent.of(0,
                ToolCall.builder().name("lookup_listing_or_ad")
                        .arguments(Map.of("ad_id", "ad-123")).build(),
                ToolResult.ok(Map.of("found", true, "listing", Map.of("id", "ad-123"))),
                10L);
        String status = projectionStatus("UC-A", "RESOLVE",
                "{\"email\":\"a@b.com\",\"ad_id\":\"ad-123\"}", List.of(lookupOk), "hi");
        assertEquals("loaded", status);
    }

    // --- helpers ---

    private String projectionStatus(String uc, String phase, String formContext,
                                    List<ToolEvent> toolEvents, String userMessage) throws Exception {
        BotSession session = BotSession.builder()
                .sessionId("sprint078-r4-cc-test")
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
        String json = builder.build(session, List.of(), plan, userMessage,
                null, toolEvents == null ? List.of() : toolEvents);
        JsonNode root = objectMapper.readTree(json);
        return root.path("customer_context_status").asText(null);
    }
}
