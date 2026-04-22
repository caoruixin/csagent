package com.gumtree.csagent.service.observability;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gumtree.csagent.model.BotEvent;
import com.gumtree.csagent.model.enums.EventType;
import com.gumtree.csagent.repository.BotEventRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class EventEmitter {

    private final BotEventRepository botEventRepository;
    private final ObjectMapper objectMapper;

    public EventEmitter(BotEventRepository botEventRepository, ObjectMapper objectMapper) {
        this.botEventRepository = botEventRepository;
        this.objectMapper = objectMapper;
    }

    public void emit(String sessionId, EventType eventType, Integer turnIndex, Map<String, Object> payload) {
        String payloadJson = null;
        if (payload != null && !payload.isEmpty()) {
            try {
                payloadJson = objectMapper.writeValueAsString(payload);
            } catch (JsonProcessingException e) {
                log.warn("Failed to serialize event payload for event_type={}: {}", eventType, e.getMessage());
                payloadJson = "{}";
            }
        }

        BotEvent event = BotEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .sessionId(sessionId)
                .eventType(eventType.name())
                .turnIndex(turnIndex)
                .payload(payloadJson)
                .createdAt(OffsetDateTime.now())
                .build();

        botEventRepository.save(event);
        log.debug("Event emitted: sessionId={}, type={}, turnIndex={}", sessionId, eventType, turnIndex);
    }

    public void emitSessionStarted(String sessionId, Map<String, Object> formContext) {
        Map<String, Object> payload = new LinkedHashMap<>();
        if (formContext != null) {
            payload.put("formContext", formContext);
        }
        emit(sessionId, EventType.SESSION_STARTED, null, payload);
    }

    public void emitUseCaseInferred(String sessionId, int turnIndex, String useCase, double confidence) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("useCase", useCase);
        payload.put("confidence", confidence);
        emit(sessionId, EventType.USE_CASE_INFERRED, turnIndex, payload);
    }

    public void emitRetrievalExecuted(String sessionId, int turnIndex, String query, boolean faqMiss, int resultCount) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("query", query);
        payload.put("faqMiss", faqMiss);
        payload.put("resultCount", resultCount);
        emit(sessionId, EventType.RETRIEVAL_EXECUTED, turnIndex, payload);
    }

    public void emitArticleShown(String sessionId, int turnIndex, String articleId, String title) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("articleId", articleId);
        payload.put("title", title);
        emit(sessionId, EventType.ARTICLE_SHOWN, turnIndex, payload);
    }

    public void emitClarificationAsked(String sessionId, int turnIndex, int clarificationCount) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("clarificationCount", clarificationCount);
        emit(sessionId, EventType.CLARIFICATION_ASKED, turnIndex, payload);
    }

    public void emitEscalationRequested(String sessionId, int turnIndex, String reason) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("reason", reason);
        emit(sessionId, EventType.ESCALATION_REQUESTED, turnIndex, payload);
    }

    public void emitCaseCreated(String sessionId, int turnIndex, String caseId, String useCase) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("caseId", caseId);
        payload.put("useCase", useCase);
        emit(sessionId, EventType.CASE_CREATED, turnIndex, payload);
    }

    public void emitOutcomeRecorded(String sessionId, String outcome, String useCase) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("outcome", outcome);
        payload.put("useCase", useCase);
        emit(sessionId, EventType.OUTCOME_RECORDED, null, payload);
    }

    public void emitSessionClosed(String sessionId, String outcome) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("outcome", outcome);
        emit(sessionId, EventType.SESSION_CLOSED, null, payload);
    }

    public void emitToolScopeBlocked(String sessionId, int turnIndex, String toolName, String useCase) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("toolName", toolName);
        payload.put("useCase", useCase);
        emit(sessionId, EventType.TOOL_SCOPE_BLOCKED, turnIndex, payload);
    }

    public void emitGuardrailViolation(String sessionId, int turnIndex, String category, String matchedText) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("category", category);
        payload.put("matchedText", matchedText);
        emit(sessionId, EventType.GUARDRAIL_VIOLATION, turnIndex, payload);
    }

    public void emitOutOfScopeHandover(String sessionId, int turnIndex, String oosCategory) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("oosCategory", oosCategory);
        emit(sessionId, EventType.OUT_OF_SCOPE_HANDOVER, turnIndex, payload);
    }
}
