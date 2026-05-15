package com.gumtree.csagent.service.mock;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gumtree.csagent.service.GumtreeApiService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@Profile("local")
public class MockGumtreeApiService implements GumtreeApiService {

    private final ObjectMapper objectMapper;

    private final Map<String, Map<String, Object>> accounts = new HashMap<>();
    private final Map<String, Map<String, Object>> listings = new HashMap<>();
    private final Map<String, Map<String, Object>> moderationReviews = new HashMap<>();
    private final Map<String, Map<String, Object>> messageModeration = new HashMap<>();

    public MockGumtreeApiService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void loadFixtures() {
        loadDirectory("mock/accounts", accounts);
        loadDirectory("mock/listings", listings);
        loadDirectory("mock/moderation_reviews", moderationReviews);
        loadDirectory("mock/message_moderation", messageModeration);
        log.info("Mock Gumtree API fixtures loaded: accounts={}, listings={}, moderation_reviews={}, message_moderation={}",
                accounts.size(), listings.size(), moderationReviews.size(), messageModeration.size());
    }

    private void loadDirectory(String path, Map<String, Map<String, Object>> target) {
        try {
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources("classpath:" + path + "/*.json");
            for (Resource resource : resources) {
                String filename = resource.getFilename();
                if (filename == null) continue;
                String key = filename.replace(".json", "");
                try (InputStream is = resource.getInputStream()) {
                    Map<String, Object> data = objectMapper.readValue(is, new TypeReference<>() {});
                    target.put(key, data);
                }
            }
        } catch (IOException e) {
            log.warn("Failed to load mock fixtures from {}: {}", path, e.getMessage());
        }
    }

    public Map<String, Object> getAccountByEmail(String email) {
        for (Map<String, Object> account : accounts.values()) {
            if (email != null && email.equals(account.get("email"))) {
                return account;
            }
        }
        log.debug("No account match for email={}, returning default active_user", email);
        return accounts.getOrDefault("active_user", Map.of());
    }

    public Map<String, Object> getListingByAdId(String adId) {
        for (Map<String, Object> listing : listings.values()) {
            if (adId != null && adId.equals(listing.get("ad_id"))) {
                return listing;
            }
        }
        log.debug("No listing match for adId={}, returning default live_ad", adId);
        return listings.getOrDefault("live_ad", Map.of());
    }

    public Map<String, Object> getModerationReview(String adId) {
        for (Map<String, Object> review : moderationReviews.values()) {
            if (adId != null && adId.equals(review.get("ad_id"))) {
                return review;
            }
        }
        log.debug("No moderation review for adId={}, returning first available", adId);
        return moderationReviews.values().stream().findFirst().orElse(Map.of());
    }

    public Map<String, Object> getMessageModerationHistory(String conversationId) {
        for (Map<String, Object> moderation : messageModeration.values()) {
            if (conversationId != null && conversationId.equals(moderation.get("conversation_id"))) {
                return moderation;
            }
        }
        log.debug("No message moderation for conversationId={}, returning default clean", conversationId);
        return messageModeration.getOrDefault("clean", Map.of());
    }

    /**
     * Returns all loaded fixtures for a given type, used by the demo inspection endpoint.
     */
    public Map<String, Map<String, Object>> getFixturesByType(String type) {
        return switch (type) {
            case "accounts" -> accounts;
            case "listings" -> listings;
            case "moderation_reviews" -> moderationReviews;
            case "message_moderation" -> messageModeration;
            default -> Map.of();
        };
    }
}
