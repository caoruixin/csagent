package com.gumtree.csagent.service.observability;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gumtree.csagent.model.BotTurn;
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
    private final ObjectMapper objectMapper;

    public TraceWriter(BotTurnRepository botTurnRepository, ObjectMapper objectMapper) {
        this.botTurnRepository = botTurnRepository;
        this.objectMapper = objectMapper;
    }

    public void recordTurn(String sessionId, int turnIndex, String userMessage,
                           String projectedContext, String llmRawResponse,
                           String actionSelected, Map<String, Object> actionParameters,
                           List<Map<String, Object>> toolCalls, String botResponse,
                           String[] sourceIds, String phaseBefore, String phaseAfter,
                           String activeUseCase, int latencyMs) {

        String actionParamsJson = serializeToJson(actionParameters, "action parameters");
        String toolCallsJson = serializeToJson(toolCalls, "tool calls");

        BotTurn turn = BotTurn.builder()
                .turnId(UUID.randomUUID().toString())
                .sessionId(sessionId)
                .turnIndex(turnIndex)
                .userMessage(userMessage)
                .projectedContext(projectedContext)
                .llmRawResponse(llmRawResponse)
                .actionSelected(actionSelected)
                .actionParameters(actionParamsJson)
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
        log.debug("Turn recorded: sessionId={}, turnIndex={}, action={}, latencyMs={}",
                sessionId, turnIndex, actionSelected, latencyMs);
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
