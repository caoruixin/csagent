package com.gumtree.csagent.service;

import java.util.Map;

/**
 * Interface for Gumtree platform API (accounts, listings, moderation).
 * Implemented by MockGumtreeApiService in local profile.
 */
public interface GumtreeApiService {

    Map<String, Object> getAccountByEmail(String email);

    Map<String, Object> getListingByAdId(String adId);

    Map<String, Object> getModerationReview(String adId);

    Map<String, Object> getMessageModerationHistory(String conversationId);
}
