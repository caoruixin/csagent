package com.gumtree.csagent.service.tools;

import com.gumtree.csagent.model.BotSession;
import com.gumtree.csagent.service.GumtreeApiService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for GetCustomerContextTool (D11.1 - Auto-trigger get_customer_context).
 * Verifies account lookup, listing lookup, moderation review conditional fetch,
 * and PII sanitization.
 */
@ExtendWith(MockitoExtension.class)
class GetCustomerContextToolTest {

    private GetCustomerContextTool tool;

    @Mock
    private GumtreeApiService gumtreeApiService;

    @BeforeEach
    void setUp() {
        tool = new GetCustomerContextTool(gumtreeApiService);
    }

    @Test
    void getName_shouldReturnGetCustomerContext() {
        assertEquals("get_customer_context", tool.getName());
    }

    // --- Email-based account lookup ---

    @Test
    void execute_withEmail_shouldReturnAccountData() {
        Map<String, Object> account = Map.of(
                "email", "jane@example.com",
                "account_status", "ACTIVE",
                "account_type", "personal",
                "has_verified_email", true);

        when(gumtreeApiService.getAccountByEmail("jane@example.com")).thenReturn(account);

        BotSession session = buildSession();
        Map<String, Object> params = Map.of("email", "jane@example.com");
        ToolResult result = tool.execute(session, params);

        assertTrue(result.isSuccess());
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) result.getData();
        assertTrue(data.containsKey("account"));
    }

    @Test
    void execute_withEmail_shouldSanitizeAccountPii() {
        Map<String, Object> account = new LinkedHashMap<>();
        account.put("email", "jane@example.com");
        account.put("phone", "+44123456789");
        account.put("full_name", "Jane Doe");
        account.put("account_status", "ACTIVE");
        account.put("account_type", "personal");

        when(gumtreeApiService.getAccountByEmail("jane@example.com")).thenReturn(account);

        BotSession session = buildSession();
        Map<String, Object> params = Map.of("email", "jane@example.com");
        ToolResult result = tool.execute(session, params);

        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) result.getData();
        @SuppressWarnings("unchecked")
        Map<String, Object> sanitizedAccount = (Map<String, Object>) data.get("account");

        assertFalse(sanitizedAccount.containsKey("email"), "PII: email should be stripped");
        assertFalse(sanitizedAccount.containsKey("phone"), "PII: phone should be stripped");
        assertFalse(sanitizedAccount.containsKey("full_name"), "PII: full_name should be stripped");
        assertEquals("ACTIVE", sanitizedAccount.get("account_status"), "Safe field should remain");
    }

    // --- Ad ID-based listing lookup ---

    @Test
    void execute_withAdId_shouldReturnListingData() {
        Map<String, Object> listing = Map.of(
                "ad_id", "AD-1001",
                "title", "iPhone 15 Pro",
                "status", "LIVE",
                "price", 899.0);

        when(gumtreeApiService.getListingByAdId("AD-1001")).thenReturn(listing);

        BotSession session = buildSession();
        Map<String, Object> params = Map.of("ad_id", "AD-1001");
        ToolResult result = tool.execute(session, params);

        assertTrue(result.isSuccess());
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) result.getData();
        assertTrue(data.containsKey("listing"));
    }

    @Test
    void execute_withAdId_listingRemoved_shouldFetchModerationReview() {
        Map<String, Object> listing = new LinkedHashMap<>();
        listing.put("ad_id", "AD-1002");
        listing.put("status", "removed");

        Map<String, Object> modReview = Map.of(
                "ad_id", "AD-1002",
                "removal_reason", "policy_violation");

        when(gumtreeApiService.getListingByAdId("AD-1002")).thenReturn(listing);
        when(gumtreeApiService.getModerationReview("AD-1002")).thenReturn(modReview);

        BotSession session = buildSession();
        Map<String, Object> params = Map.of("ad_id", "AD-1002");
        ToolResult result = tool.execute(session, params);

        assertTrue(result.isSuccess());
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) result.getData();
        assertTrue(data.containsKey("moderation_review"), "Removed listing should fetch moderation review");
    }

    @Test
    void execute_withAdId_listingLive_shouldNotFetchModerationReview() {
        Map<String, Object> listing = new LinkedHashMap<>();
        listing.put("ad_id", "AD-1001");
        listing.put("status", "LIVE");

        when(gumtreeApiService.getListingByAdId("AD-1001")).thenReturn(listing);

        BotSession session = buildSession();
        Map<String, Object> params = Map.of("ad_id", "AD-1001");
        ToolResult result = tool.execute(session, params);

        assertTrue(result.isSuccess());
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) result.getData();
        assertFalse(data.containsKey("moderation_review"), "LIVE listing should NOT fetch moderation review");
        verify(gumtreeApiService, never()).getModerationReview(anyString());
    }

    // --- Both email and ad_id ---

    @Test
    void execute_withBothEmailAndAdId_shouldReturnBothAccountAndListing() {
        Map<String, Object> account = Map.of("account_status", "ACTIVE");
        Map<String, Object> listing = Map.of("ad_id", "AD-1001", "status", "LIVE");

        when(gumtreeApiService.getAccountByEmail("jane@example.com")).thenReturn(account);
        when(gumtreeApiService.getListingByAdId("AD-1001")).thenReturn(listing);

        BotSession session = buildSession();
        Map<String, Object> params = Map.of("email", "jane@example.com", "ad_id", "AD-1001");
        ToolResult result = tool.execute(session, params);

        assertTrue(result.isSuccess());
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) result.getData();
        assertTrue(data.containsKey("account"));
        assertTrue(data.containsKey("listing"));
    }

    // --- No parameters ---

    @Test
    void execute_noEmailNoAdId_shouldReturnError() {
        BotSession session = buildSession();
        Map<String, Object> params = Map.of();
        ToolResult result = tool.execute(session, params);

        assertFalse(result.isSuccess());
    }

    @Test
    void execute_blankEmail_shouldReturnError() {
        BotSession session = buildSession();
        Map<String, Object> params = Map.of("email", "   ");
        ToolResult result = tool.execute(session, params);

        assertFalse(result.isSuccess());
    }

    @Test
    void execute_nullEmail_noAdId_shouldReturnError() {
        BotSession session = buildSession();
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("email", null);
        ToolResult result = tool.execute(session, params);

        assertFalse(result.isSuccess());
    }

    private BotSession buildSession() {
        return BotSession.builder()
                .sessionId("test-session-ctx")
                .handlingState("BOT_HANDLING")
                .currentPhase("INIT")
                .totalBotTurns(0)
                .clarificationCount(0)
                .faqMissCount(0)
                .repeatedActionCount(0)
                .build();
    }
}
