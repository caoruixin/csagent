package com.gumtree.csagent.service.tools;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gumtree.csagent.model.BotEvent;
import com.gumtree.csagent.model.BotSession;
import com.gumtree.csagent.model.enums.EventType;
import com.gumtree.csagent.repository.BotEventRepository;
import com.gumtree.csagent.repository.BotSessionRepository;
import com.gumtree.csagent.service.runtime.UseCaseRegistryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Lets the LLM commit a use-case classification during the DISCOVER phase.
 * AGENT_VISIBLE — exposed to the LLM whenever {@code activeUseCase == null}
 * (per tool-policy.yaml: {@code allowed-ucs: [ALL]}); restricted to the
 * DISCOVER plan via {@code PhaseEvaluator.plan()} {@code allowedTools}.
 *
 * <p>Added 2026-05-02 (Phase 2 Fix 3c) to close the soft-OOS DISCOVER gap.
 * Without this tool, soft-OOS DISCOVER sessions could only ask clarifying
 * questions until the budget exhausted, producing
 * {@code CONTRACT_VIOLATION:active_use_case missing_after_turns} on
 * cs_interactive_001 / 002 / 014 / 029 / 259.
 *
 * <p>Tool effect:
 * <ol>
 *   <li>Validate {@code use_case_id} via {@link UseCaseRegistryService#isKnownUseCase}.</li>
 *   <li>Validate {@code confidence} ∈ [0, 1].</li>
 *   <li>Set {@code session.activeUseCase} + {@code session.intentConfidence}.</li>
 *   <li>Save session via {@link BotSessionRepository}.</li>
 *   <li>Emit a {@link EventType#CLASSIFICATION_COMMITTED} {@code BotEvent}.</li>
 *   <li>Return {@code {committed: true, use_case_id, confidence}}.</li>
 * </ol>
 *
 * <p>The AgentRunLoop continues after this tool call — the LLM can call
 * {@code search_knowledge} or emit a final user_message, and
 * {@code PhaseEvaluator.interpretRunResult} transitions DISCOVER → RESOLVE on
 * FINAL_ANSWER once {@code activeUseCase != null}.
 */
@Slf4j
@Component
public class ClassifyUseCaseTool implements Tool {

    private final UseCaseRegistryService useCaseRegistry;
    private final BotSessionRepository botSessionRepository;
    private final BotEventRepository botEventRepository;
    private final ObjectMapper objectMapper;

    public ClassifyUseCaseTool(UseCaseRegistryService useCaseRegistry,
                               BotSessionRepository botSessionRepository,
                               BotEventRepository botEventRepository,
                               ObjectMapper objectMapper) {
        this.useCaseRegistry = useCaseRegistry;
        this.botSessionRepository = botSessionRepository;
        this.botEventRepository = botEventRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public String getName() {
        return "classify_use_case";
    }

    @Override
    public ToolResult execute(BotSession session, Map<String, Object> parameters) {
        if (parameters == null) {
            return ToolResult.error("Parameters are required: use_case_id, confidence");
        }

        String useCaseId = asString(parameters.get("use_case_id"));
        if (useCaseId == null || useCaseId.isBlank()) {
            return ToolResult.error("Parameter 'use_case_id' is required");
        }
        if (!useCaseRegistry.isKnownUseCase(useCaseId)) {
            return ToolResult.error("unknown_use_case: " + useCaseId);
        }

        Double confidence = asDouble(parameters.get("confidence"));
        if (confidence == null) {
            return ToolResult.error("Parameter 'confidence' is required and must be numeric");
        }
        if (confidence < 0.0 || confidence > 1.0) {
            return ToolResult.error("invalid_confidence: must be in [0, 1]; got " + confidence);
        }

        String reasoning = asString(parameters.get("reasoning"));

        // Apply to session.
        session.setActiveUseCase(useCaseId);
        // Intent confidence column is precision=4 scale=2 — clamp to 2 decimals.
        BigDecimal storedConfidence = BigDecimal.valueOf(confidence)
                .setScale(2, RoundingMode.HALF_UP);
        session.setIntentConfidence(storedConfidence);
        session.setUpdatedAt(OffsetDateTime.now());
        botSessionRepository.save(session);

        // Emit CLASSIFICATION_COMMITTED event.
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("use_case_id", useCaseId);
            payload.put("confidence", storedConfidence);
            if (reasoning != null && !reasoning.isBlank()) {
                payload.put("reasoning", reasoning);
            }
            BotEvent event = BotEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .sessionId(session.getSessionId())
                    .eventType(EventType.CLASSIFICATION_COMMITTED.name())
                    .turnIndex(session.getTotalBotTurns())
                    .payload(objectMapper.writeValueAsString(payload))
                    .createdAt(OffsetDateTime.now())
                    .build();
            botEventRepository.save(event);
        } catch (JsonProcessingException ex) {
            log.warn("classify_use_case: failed to serialize CLASSIFICATION_COMMITTED payload: {}",
                    ex.getMessage());
        }

        log.info("classify_use_case committed: session='{}', uc='{}', confidence={}",
                session.getSessionId(), useCaseId, storedConfidence);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("committed", true);
        data.put("use_case_id", useCaseId);
        data.put("confidence", storedConfidence);
        return ToolResult.ok(data);
    }

    private static String asString(Object value) {
        return value == null ? null : value.toString();
    }

    private static Double asDouble(Object value) {
        if (value == null) return null;
        if (value instanceof Number n) return n.doubleValue();
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
