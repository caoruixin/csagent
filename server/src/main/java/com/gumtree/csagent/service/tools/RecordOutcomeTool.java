package com.gumtree.csagent.service.tools;

import com.gumtree.csagent.model.BotSession;
import com.gumtree.csagent.model.SessionOutcome;
import com.gumtree.csagent.repository.SessionOutcomeRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Records the final session outcome (resolve / escalate / abandon).
 * AGENT_VISIBLE — allowed for ALL use cases.
 *
 * <p>Sprint 9 §O0 — terminal tool contract alignment. The projected schema
 * advertises {@code outcome_class} with lowercase enum values
 * {@code resolve | escalate | abandon}; this tool now accepts the canonical
 * shape directly. {@code outcome} (legacy) and uppercase values
 * ({@code RESOLVED / ESCALATED / ABANDONED}) are still accepted as
 * back-compat aliases and normalised internally to the canonical persisted
 * uppercase form so {@link SessionOutcome} rows stay consistent.
 */
@Slf4j
@Component
public class RecordOutcomeTool implements Tool {

    private final SessionOutcomeRepository sessionOutcomeRepository;

    public RecordOutcomeTool(SessionOutcomeRepository sessionOutcomeRepository) {
        this.sessionOutcomeRepository = sessionOutcomeRepository;
    }

    @Override
    public String getName() {
        return "record_outcome";
    }

    @Override
    public ToolResult execute(BotSession session, Map<String, Object> parameters) {
        // Sprint 9 §O0: accept the canonical schema-advertised arg name first,
        // fall back to the legacy {@code outcome} alias if absent.
        String raw = readStringParam(parameters, "outcome_class");
        if (raw == null) {
            raw = readStringParam(parameters, "outcome");
        }
        if (raw == null) {
            return ToolResult.error(
                    "Parameter 'outcome_class' is required (one of: resolve, escalate, abandon)");
        }

        String normalizedClass = normalizeOutcomeClass(raw);
        if (normalizedClass == null) {
            return ToolResult.error(
                    "Parameter 'outcome_class' must be one of: resolve, escalate, abandon "
                            + "(legacy values RESOLVED/ESCALATED/ABANDONED also accepted)");
        }
        // Persisted outcome stays in the legacy uppercase form so the
        // session_outcomes table and downstream eval do not have to re-map.
        String persistedOutcome = persistedFormFor(normalizedClass);

        String escalationReason = readStringParam(parameters, "escalation_reason");
        if ("escalate".equals(normalizedClass)
                && (escalationReason == null || escalationReason.isBlank())) {
            return ToolResult.error(
                    "Parameter 'escalation_reason' is required when outcome_class is 'escalate'");
        }

        SessionOutcome sessionOutcome = SessionOutcome.builder()
                .sessionId(session.getSessionId())
                .outcome(persistedOutcome)
                .useCaseId(session.getActiveUseCase())
                .escalationReason(escalationReason)
                .articlesShown(session.getArticlesShown())
                .totalTurns(session.getTotalBotTurns())
                .createdAt(OffsetDateTime.now())
                .build();

        sessionOutcomeRepository.save(sessionOutcome);

        log.info("Outcome recorded: session='{}', outcome_class='{}', persisted='{}', useCase='{}'",
                session.getSessionId(), normalizedClass, persistedOutcome,
                session.getActiveUseCase());

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("session_id", session.getSessionId());
        data.put("outcome_class", normalizedClass);
        data.put("outcome", persistedOutcome);
        data.put("use_case_id", session.getActiveUseCase());

        return ToolResult.ok(data);
    }

    private static String readStringParam(Map<String, Object> parameters, String name) {
        if (parameters == null) return null;
        Object raw = parameters.get(name);
        if (!(raw instanceof String s)) return null;
        return s.isBlank() ? null : s;
    }

    /**
     * Normalise an inbound {@code outcome_class}/{@code outcome} string to the
     * canonical lowercase form ({@code resolve | escalate | abandon}). Returns
     * {@code null} if the value is not in the accepted set. Both the canonical
     * lowercase forms and the legacy uppercase forms ({@code RESOLVED /
     * ESCALATED / ABANDONED}) are accepted.
     */
    private static String normalizeOutcomeClass(String raw) {
        if (raw == null) return null;
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) return null;
        String lower = trimmed.toLowerCase(Locale.ENGLISH);
        return switch (lower) {
            case "resolve", "resolved" -> "resolve";
            case "escalate", "escalated" -> "escalate";
            case "abandon", "abandoned" -> "abandon";
            default -> null;
        };
    }

    /** Translate canonical lowercase or legacy uppercase into persisted uppercase. */
    private static String persistedFormFor(String value) {
        return switch (value.toLowerCase(Locale.ENGLISH)) {
            case "resolve", "resolved" -> "RESOLVED";
            case "escalate", "escalated" -> "ESCALATED";
            case "abandon", "abandoned" -> "ABANDONED";
            default -> value;
        };
    }
}
