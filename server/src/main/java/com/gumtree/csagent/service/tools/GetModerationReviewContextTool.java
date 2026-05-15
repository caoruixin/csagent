package com.gumtree.csagent.service.tools;

import com.gumtree.csagent.model.BotSession;
import com.gumtree.csagent.service.GumtreeApiService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Runtime-only tool that fetches moderation review context for a listing.
 * Maps internal reason codes to customer-facing explanations.
 * Allowed for UC-A, UC-FP only.
 */
@Slf4j
@Component
public class GetModerationReviewContextTool implements Tool {

    /**
     * Map of internal moderation reason codes to customer-facing explanations.
     */
    private static final Map<String, String> REASON_CODE_EXPLANATIONS = Map.ofEntries(
            Map.entry("PROHIBITED_ITEM", "Your ad was removed because it contained a prohibited item that is not allowed on Gumtree."),
            Map.entry("DUPLICATE_AD", "Your ad was removed because it appears to be a duplicate of another listing."),
            Map.entry("MISLEADING_CONTENT", "Your ad was removed because it contained misleading or inaccurate information."),
            Map.entry("PROHIBITED_CATEGORY", "Your ad was removed because it was posted in a category that is not permitted."),
            Map.entry("SPAM", "Your ad was removed because it was identified as spam."),
            Map.entry("SCAM_SUSPECT", "Your ad was removed because it displayed characteristics associated with fraudulent listings."),
            Map.entry("IMAGE_VIOLATION", "Your ad was removed because the images did not comply with our image policy."),
            Map.entry("PRICING_ISSUE", "Your ad was removed because the pricing appeared incorrect or misleading."),
            Map.entry("CONTACT_INFO_IN_AD", "Your ad was removed because it contained contact information in the description or images."),
            Map.entry("OTHER", "Your ad was removed for a policy violation. Please contact support for more details.")
    );

    private final GumtreeApiService gumtreeApiService;

    public GetModerationReviewContextTool(GumtreeApiService gumtreeApiService) {
        this.gumtreeApiService = gumtreeApiService;
    }

    @Override
    public String getName() {
        return "get_moderation_review_context";
    }

    @Override
    public ToolResult execute(BotSession session, Map<String, Object> parameters) {
        String adId = (String) parameters.get("ad_id");
        if (adId == null || adId.isBlank()) {
            return ToolResult.error("Parameter 'ad_id' is required");
        }

        Map<String, Object> review = gumtreeApiService.getModerationReview(adId);

        if (review == null || review.isEmpty()) {
            log.info("No moderation review found for ad_id='{}' in session '{}'",
                    adId, session.getSessionId());
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("found", false);
            return ToolResult.ok(data);
        }

        // Map internal reason code to customer-facing explanation
        Map<String, Object> data = new LinkedHashMap<>(review);
        data.put("found", true);

        String reasonCode = (String) review.get("reason_code");
        if (reasonCode != null) {
            String explanation = REASON_CODE_EXPLANATIONS.getOrDefault(
                    reasonCode, REASON_CODE_EXPLANATIONS.get("OTHER"));
            data.put("customer_facing_explanation", explanation);
        }

        return ToolResult.ok(data);
    }
}
