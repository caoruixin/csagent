package com.gumtree.csagent.service.tools;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gumtree.csagent.model.BotEvent;
import com.gumtree.csagent.model.BotSession;
import com.gumtree.csagent.model.enums.EventType;
import com.gumtree.csagent.repository.BotEventRepository;
import com.gumtree.csagent.repository.BotSessionRepository;
import com.gumtree.csagent.service.runtime.UseCaseRegistryService;
import com.gumtree.csagent.service.runtime.UseCaseRouter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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

        // Sprint §C2: Replies/Messaging strong-prior carry-forward.
        //
        // The bot-turn AgentRunLoop has historically rotated cs_interactive_014's
        // active_use_case to UC-B / UC-F / UC-H even though the form-context
        // strong-prior table maps "Replies or Messaging" -> UC-C. The drift
        // happens here: the LLM emits classify_use_case with a different UC,
        // and this tool blindly overwrites session.activeUseCase.
        //
        // Policy: when the session already has an active_use_case that matches
        // the deterministic strong-prior / B2 phrase-bias UC for the current
        // form context, refuse to overwrite it with a different UC. The
        // upstream {@link com.gumtree.csagent.service.runtime.DriftDetector}
        // is the legitimate path for hard-shifts (UC-G GDPR, UC-I refund,
        // UC-J safety, UC-H ad-removal) — those flip session.activeUseCase
        // BEFORE the bot loop runs, so by the time the LLM gets here the
        // strong-prior basis no longer matches and this guard releases.
        //
        // Idempotent re-classification (LLM picks the same UC the strong
        // prior already chose) is allowed and continues to emit the
        // CLASSIFICATION_COMMITTED event — that's still the LLM
        // committing to the UC.
        if (shouldPreserveStrongPrior(session, useCaseId)) {
            String preservedUc = session.getActiveUseCase();
            log.info("classify_use_case: preserved strong-prior active_use_case='{}' on session='{}' "
                    + "(LLM proposed '{}' but form-context strong prior is '{}'); "
                    + "no hard-shift drift signal observed",
                    preservedUc, session.getSessionId(), useCaseId, preservedUc);
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("committed", false);
            data.put("preserved_use_case_id", preservedUc);
            data.put("rejected_use_case_id", useCaseId);
            data.put("reason", "strong_prior_carry_forward");
            return ToolResult.ok(data);
        }

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

    /**
     * Sprint §C2 strong-prior carry-forward policy. Returns true iff the LLM's
     * proposed UC should be REFUSED because the session's current active UC
     * came from a deterministic high-confidence form-context route
     * (strong-prior topic or B2 phrase bias) and the proposed UC is different.
     *
     * <p>Hard-shift exits via {@link com.gumtree.csagent.service.runtime.DriftDetector}:
     * GDPR / refund / scam / ad-removal keywords flip {@code session.activeUseCase}
     * BEFORE the bot loop runs, so by the time this tool is reached the
     * deterministic re-derivation no longer matches the (now changed) active
     * UC, and this guard releases — the LLM is free to commit the hard-shift UC.
     */
    boolean shouldPreserveStrongPrior(BotSession session, String proposedUcId) {
        if (session == null) return false;
        String activeUc = session.getActiveUseCase();
        if (activeUc == null || activeUc.isBlank()) return false;
        if (proposedUcId == null) return false;
        if (proposedUcId.equals(activeUc)) return false; // idempotent — let it through

        String topicSubject = session.getFormTopicSubject();
        String description = extractFormDescription(session);
        Optional<String> derived = UseCaseRouter.deriveStrongPriorUc(
                topicSubject, description, useCaseRegistry);
        if (derived.isEmpty()) return false;
        // Only fire when the active UC matches what the deterministic router
        // would have set — i.e. the strong-prior signal is still load-bearing.
        // If DriftDetector already flipped activeUseCase to a hard-shift UC
        // (UC-G/UC-I/UC-J/UC-H), the derivation no longer matches and this
        // policy releases.
        return derived.get().equals(activeUc);
    }

    /**
     * Pull {@code description} out of the persisted form context JSON. Returns
     * empty string when form context is absent or malformed — the strong-prior
     * derivation will then fall back to topic-only logic via the registry.
     */
    String extractFormDescription(BotSession session) {
        if (session == null) return "";
        String formContextJson = session.getFormContext();
        if (formContextJson == null || formContextJson.isBlank()) return "";
        try {
            JsonNode root = objectMapper.readTree(formContextJson);
            JsonNode desc = root.get("description");
            return desc == null ? "" : desc.asText("");
        } catch (Exception ex) {
            log.debug("classify_use_case: failed to parse formContext for description: {}", ex.getMessage());
            return "";
        }
    }

    /**
     * Sprint §C2: UCs that escape the carry-forward policy regardless of the
     * strong-prior signal. Currently empty — the {@link com.gumtree.csagent.service.runtime.DriftDetector}
     * pre-empts hard-shift cases by mutating {@code activeUseCase} BEFORE the
     * bot loop, so this set is reserved for any future hard-shift signal that
     * may need to override the carry-forward without going through the drift
     * detector first.
     */
    static final Set<String> HARD_SHIFT_BYPASS_UCS = Set.of();

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
