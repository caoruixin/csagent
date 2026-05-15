package com.gumtree.csagent.service.observability;

import com.gumtree.csagent.model.LlmCallLog;
import com.gumtree.csagent.repository.LlmCallLogRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;

/**
 * Records every LLM call (routing, chat, rerank, retry) into the llm_call_log table.
 * Logging failures are swallowed — they must NEVER break the main flow.
 */
@Slf4j
@Service
public class LlmCallLogger {

    private final LlmCallLogRepository repository;

    public LlmCallLogger(LlmCallLogRepository repository) {
        this.repository = repository;
    }

    /**
     * Log a successful LLM call.
     *
     * @param sessionId        session identifier
     * @param turnIndex        turn index (nullable for routing calls)
     * @param callType         one of: "routing", "chat", "rerank"
     * @param model            the model name used
     * @param promptTokens     prompt token count (may be 0 if unavailable)
     * @param completionTokens completion token count (may be 0 if unavailable)
     * @param latencyMs        call latency in milliseconds
     * @param requestSummary   truncated request text (nullable)
     * @param responseSummary  truncated response text (nullable)
     */
    public void log(String sessionId, Integer turnIndex, String callType,
                    String model, int promptTokens, int completionTokens,
                    int latencyMs, String requestSummary, String responseSummary) {
        try {
            LlmCallLog entry = LlmCallLog.builder()
                    .sessionId(sessionId)
                    .turnIndex(turnIndex)
                    .callType(callType)
                    .model(model)
                    .promptTokens(promptTokens)
                    .completionTokens(completionTokens)
                    .latencyMs(latencyMs)
                    .requestSummary(truncate(requestSummary, 500))
                    .responseSummary(truncate(responseSummary, 500))
                    .success(true)
                    .createdAt(OffsetDateTime.now())
                    .build();
            repository.save(entry);
        } catch (Exception e) {
            log.warn("Failed to log LLM call [{}] for session {}: {}", callType, sessionId, e.getMessage());
        }
    }

    /**
     * Log a failed LLM call.
     *
     * @param sessionId      session identifier
     * @param turnIndex      turn index (nullable for routing calls)
     * @param callType       one of: "routing", "chat", "rerank"
     * @param model          the model name used
     * @param latencyMs      call latency in milliseconds
     * @param requestSummary truncated request text (nullable)
     * @param errorMessage   the error message
     */
    public void logFailure(String sessionId, Integer turnIndex, String callType,
                           String model, int latencyMs, String requestSummary,
                           String errorMessage) {
        try {
            LlmCallLog entry = LlmCallLog.builder()
                    .sessionId(sessionId)
                    .turnIndex(turnIndex)
                    .callType(callType)
                    .model(model)
                    .promptTokens(0)
                    .completionTokens(0)
                    .latencyMs(latencyMs)
                    .requestSummary(truncate(requestSummary, 500))
                    .success(false)
                    .errorMessage(truncate(errorMessage, 1000))
                    .createdAt(OffsetDateTime.now())
                    .build();
            repository.save(entry);
        } catch (Exception e) {
            log.warn("Failed to log LLM call failure [{}] for session {}: {}", callType, sessionId, e.getMessage());
        }
    }

    private String truncate(String text, int maxLen) {
        if (text == null) return null;
        return text.length() <= maxLen ? text : text.substring(0, maxLen);
    }
}
