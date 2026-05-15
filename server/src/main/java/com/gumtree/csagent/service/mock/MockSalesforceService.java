package com.gumtree.csagent.service.mock;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gumtree.csagent.config.MockProperties;
import com.gumtree.csagent.model.MockCase;
import com.gumtree.csagent.model.MockHandoverLog;
import com.gumtree.csagent.repository.MockCaseRepository;
import com.gumtree.csagent.repository.MockHandoverLogRepository;
import com.gumtree.csagent.service.SalesforceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@Profile("local")
public class MockSalesforceService implements SalesforceService {

    private final MockCaseRepository caseRepository;
    private final MockHandoverLogRepository handoverLogRepository;
    private final MockProperties mockProperties;
    private final ObjectMapper objectMapper;

    public MockSalesforceService(MockCaseRepository caseRepository,
                                 MockHandoverLogRepository handoverLogRepository,
                                 MockProperties mockProperties,
                                 ObjectMapper objectMapper) {
        this.caseRepository = caseRepository;
        this.handoverLogRepository = handoverLogRepository;
        this.mockProperties = mockProperties;
        this.objectMapper = objectMapper;
    }

    public String createCase(String sessionId, String useCaseId, String subject,
                             String description, String email, String adId,
                             String queueName, Map<String, Object> intakeFields) {
        String caseId = "CASE-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        OffsetDateTime now = OffsetDateTime.now();

        String intakeFieldsJson = null;
        if (intakeFields != null && !intakeFields.isEmpty()) {
            try {
                intakeFieldsJson = objectMapper.writeValueAsString(intakeFields);
            } catch (JsonProcessingException e) {
                log.warn("Failed to serialize intake fields: {}", e.getMessage());
            }
        }

        MockCase mockCase = MockCase.builder()
                .caseId(caseId)
                .sessionId(sessionId)
                .useCaseId(useCaseId)
                .subject(subject)
                .description(description)
                .contactEmail(email)
                .adId(adId)
                .status("New")
                .queueName(queueName)
                .intakeFields(intakeFieldsJson)
                .createdAt(now)
                .updatedAt(now)
                .build();

        caseRepository.save(mockCase);
        log.info("Mock case created: caseId={}, useCaseId={}, sessionId={}", caseId, useCaseId, sessionId);
        return caseId;
    }

    public String requestHandover(String sessionId, Map<String, Object> handoverPayload) {
        return requestHandover(sessionId, handoverPayload, null, null);
    }

    public String requestHandover(String sessionId, Map<String, Object> handoverPayload,
                                  String customerMessage, String transcript) {
        String logId = "LOG-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String transferResult = mockProperties.isBusinessHours() ? "transferred" : "offline_logged";

        String payloadJson;
        try {
            payloadJson = objectMapper.writeValueAsString(handoverPayload);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize handover payload: {}", e.getMessage());
            payloadJson = "{}";
        }

        MockHandoverLog logEntry = MockHandoverLog.builder()
                .logId(logId)
                .sessionId(sessionId)
                .handoverPayload(payloadJson)
                .customerMessage(customerMessage)
                .transcript(transcript)
                .transferResult(transferResult)
                .createdAt(OffsetDateTime.now())
                .build();

        handoverLogRepository.save(logEntry);
        log.info("Mock handover logged: logId={}, sessionId={}, result={}", logId, sessionId, transferResult);
        return transferResult;
    }

    public List<MockCase> findCasesBySessionId(String sessionId) {
        return caseRepository.findBySessionId(sessionId);
    }

    public List<MockCase> findAllCases() {
        return caseRepository.findAll();
    }

    public List<MockHandoverLog> findAllHandoverLogs() {
        return handoverLogRepository.findAll();
    }
}
