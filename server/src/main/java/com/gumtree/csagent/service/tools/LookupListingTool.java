package com.gumtree.csagent.service.tools;

import com.gumtree.csagent.model.BotSession;
import com.gumtree.csagent.service.GumtreeApiService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Runtime-only tool that looks up a listing/ad by its ad ID.
 * Delegates to GumtreeApiService.getListingByAdId().
 */
@Slf4j
@Component
public class LookupListingTool implements Tool {

    private final GumtreeApiService gumtreeApiService;

    public LookupListingTool(GumtreeApiService gumtreeApiService) {
        this.gumtreeApiService = gumtreeApiService;
    }

    @Override
    public String getName() {
        return "lookup_listing_or_ad";
    }

    @Override
    public ToolResult execute(BotSession session, Map<String, Object> parameters) {
        String adId = (String) parameters.get("ad_id");
        if (adId == null || adId.isBlank()) {
            return ToolResult.error("Parameter 'ad_id' is required");
        }

        Map<String, Object> listing = gumtreeApiService.getListingByAdId(adId);

        if (listing == null || listing.isEmpty()) {
            log.info("No listing found for ad_id in session '{}'", session.getSessionId());
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("found", false);
            return ToolResult.ok(data);
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("found", true);
        data.put("listing", listing);

        return ToolResult.ok(data);
    }
}
