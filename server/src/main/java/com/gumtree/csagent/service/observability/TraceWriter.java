package com.gumtree.csagent.service.observability;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gumtree.csagent.model.BotTurn;
import com.gumtree.csagent.model.BotTurnLlmCall;
import com.gumtree.csagent.model.LlmCallRecord;
import com.gumtree.csagent.repository.BotTurnLlmCallRepository;
import com.gumtree.csagent.repository.BotTurnRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class TraceWriter {

    private final BotTurnRepository botTurnRepository;
    private final BotTurnLlmCallRepository botTurnLlmCallRepository;
    private final ObjectMapper objectMapper;

    public TraceWriter(BotTurnRepository botTurnRepository,
                       BotTurnLlmCallRepository botTurnLlmCallRepository,
                       ObjectMapper objectMapper) {
        this.botTurnRepository = botTurnRepository;
        this.botTurnLlmCallRepository = botTurnLlmCallRepository;
        this.objectMapper = objectMapper;
    }

    public void recordTurn(String sessionId, int turnIndex, String userMessage,
                           String projectedContext, String llmRawResponse,
                           List<Map<String, Object>> toolCalls, String botResponse,
                           String[] sourceIds, String phaseBefore, String phaseAfter,
                           String activeUseCase, int latencyMs) {

        String toolCallsJson = serializeToJson(toolCalls, "tool calls");

        BotTurn turn = BotTurn.builder()
                .turnId(UUID.randomUUID().toString())
                .sessionId(sessionId)
                .turnIndex(turnIndex)
                .userMessage(userMessage)
                .projectedContext(projectedContext)
                .llmRawResponse(llmRawResponse)
                .toolCalls(toolCallsJson)
                .botResponse(botResponse)
                .sourceIds(sourceIds)
                .phaseBefore(phaseBefore)
                .phaseAfter(phaseAfter)
                .activeUseCase(activeUseCase)
                .latencyMs(latencyMs)
                .createdAt(OffsetDateTime.now())
                .build();

        botTurnRepository.save(turn);
        log.debug("Turn recorded: sessionId={}, turnIndex={}, latencyMs={}",
                sessionId, turnIndex, latencyMs);
    }

    /**
     * Sprint 51 / M5 S2 — persist the per-step LLM invocation records
     * accumulated by {@code AgentRunLoopImpl}. One row in
     * {@code bot_turn_llm_calls} per record, keyed by the already-persisted
     * {@code BotTurn.turnId}. Observation-only: callers (typically
     * {@code ControlKernel.recordRunResult}) invoke this AFTER the owning
     * {@link BotTurn} has been saved so the FK resolves.
     *
     * <p>Tolerant — never throws on serialization or persistence failure
     * (records observability, not core state). Logs at WARN and skips the
     * offending record so a single bad payload does not lose the rest of
     * the run's per-step trace.
     */
    public void recordLlmCalls(String botTurnId, List<LlmCallRecord> records) {
        if (botTurnId == null || records == null || records.isEmpty()) {
            return;
        }
        for (LlmCallRecord r : records) {
            try {
                String toolCallsJson = r.toolCalls() == null || r.toolCalls().isEmpty()
                        ? null
                        : serializeToJson(r.toolCalls(), "per-step tool_calls");
                BotTurnLlmCall row = BotTurnLlmCall.builder()
                        .botTurnId(botTurnId)
                        .stepIndex(r.stepIndex())
                        .callType(r.callType())
                        .model(r.model())
                        .latencyMs((int) r.latencyMs())
                        .llmRawResponse(r.llmRawResponse())
                        .projectedContext(r.projectedContext())
                        .toolCalls(toolCallsJson)
                        .createdAt(OffsetDateTime.now())
                        .build();
                botTurnLlmCallRepository.save(row);
            } catch (Exception ex) {
                log.warn("Failed to persist per-step llm call (botTurnId={}, step={}): {}",
                        botTurnId, r.stepIndex(), ex.getMessage());
            }
        }
        log.debug("Per-step llm calls recorded: botTurnId={}, count={}", botTurnId, records.size());
    }

    private String serializeToJson(Object value, String fieldName) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize {}: {}", fieldName, e.getMessage());
            return null;
        }
    }
}
