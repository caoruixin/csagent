package com.gumtree.csagent.service;

import com.gumtree.csagent.model.MockCase;
import com.gumtree.csagent.model.MockHandoverLog;

import java.util.List;
import java.util.Map;

/**
 * Interface for Salesforce integration (case creation, handover).
 * Implemented by MockSalesforceService in local profile.
 */
public interface SalesforceService {

    String createCase(String sessionId, String useCaseId, String subject,
                      String description, String email, String adId,
                      String queueName, Map<String, Object> intakeFields);

    String requestHandover(String sessionId, Map<String, Object> handoverPayload);

    List<MockCase> findCasesBySessionId(String sessionId);
}
