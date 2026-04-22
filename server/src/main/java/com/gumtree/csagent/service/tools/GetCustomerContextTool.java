package com.gumtree.csagent.service.tools;

import com.gumtree.csagent.model.BotSession;
import com.gumtree.csagent.service.GumtreeApiService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Composite tool that fetches account, listing, and moderation data for a customer.
 * AGENT_VISIBLE — returns a safe summary (no raw PII).
 */
@Slf4j
@Component
public class GetCustomerContextTool implements Tool {

    private final GumtreeApiService gumtreeApiService;

    public GetCustomerContextTool(GumtreeApiService gumtreeApiService) {
        this.gumtreeApiService = gumtreeApiService;
    }

    @Override
    public String getName() {
        return "get_customer_context";
    }

    @Override
    public ToolResult execute(BotSession session, Map<String, Object> parameters) {
        Map<String, Object> data = new LinkedHashMap<>();

        // Get account by email from session or parameters
        String email = (String) parameters.get("email");
        if (email != null && !email.isBlank()) {
            Map<String, Object> account = gumtreeApiService.getAccountByEmail(email);
            data.put("account", sanitizeAccount(account));
        }

        // Get listing if ad_id is present
        String adId = (String) parameters.get("ad_id");
        if (adId != null && !adId.isBlank()) {
            Map<String, Object> listing = gumtreeApiService.getListingByAdId(adId);
            data.put("listing", sanitizeListing(listing));

            // Also get moderation review if the listing status indicates removal
            String listingStatus = listing != null ? (String) listing.get("status") : null;
            if ("removed".equalsIgnoreCase(listingStatus) || "moderated".equalsIgnoreCase(listingStatus)) {
                Map<String, Object> modReview = gumtreeApiService.getModerationReview(adId);
                data.put("moderation_review", modReview);
            }
        }

        if (data.isEmpty()) {
            return ToolResult.error("At least 'email' or 'ad_id' parameter is required");
        }

        return ToolResult.ok(data);
    }

    /**
     * Strip raw PII from account data, keeping only safe fields.
     */
    private Map<String, Object> sanitizeAccount(Map<String, Object> account) {
        if (account == null || account.isEmpty()) {
            return Map.of();
        }
        Map<String, Object> safe = new LinkedHashMap<>();
        safe.put("account_status", account.get("account_status"));
        safe.put("account_type", account.get("account_type"));
        safe.put("creation_date", account.get("creation_date"));
        safe.put("active_ads_count", account.get("active_ads_count"));
        safe.put("total_ads_count", account.get("total_ads_count"));
        safe.put("has_verified_email", account.get("has_verified_email"));
        safe.put("has_verified_phone", account.get("has_verified_phone"));
        // Intentionally omit: email, phone, full_name, address, etc.
        return safe;
    }

    /**
     * Strip raw PII from listing data.
     */
    private Map<String, Object> sanitizeListing(Map<String, Object> listing) {
        if (listing == null || listing.isEmpty()) {
            return Map.of();
        }
        Map<String, Object> safe = new LinkedHashMap<>();
        safe.put("ad_id", listing.get("ad_id"));
        safe.put("title", listing.get("title"));
        safe.put("category", listing.get("category"));
        safe.put("status", listing.get("status"));
        safe.put("price", listing.get("price"));
        safe.put("location", listing.get("location"));
        safe.put("posted_date", listing.get("posted_date"));
        safe.put("is_featured", listing.get("is_featured"));
        return safe;
    }
}
