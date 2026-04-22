package com.gumtree.csagent.service.tools;

import com.gumtree.csagent.model.BotSession;
import com.gumtree.csagent.model.SessionOutcome;
import com.gumtree.csagent.repository.SessionOutcomeRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Records the final session outcome (RESOLVED / ESCALATED / ABANDONED).
 * AGENT_VISIBLE — allowed for ALL use cases.
 */
@Slf4j
@Component
public class RecordOutcomeTool implements Tool {

    private static final Set<String> VALID_OUTCOMES = Set.of("RESOLVED", "ESCALATED", "ABANDONED");

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
        String outcome = (String) parameters.get("outcome");
        if (outcome == null || !VALID_OUTCOMES.contains(outcome.toUpperCase())) {
            return ToolResult.error("Parameter 'outcome' must be one of: RESOLVED, ESCALATED, ABANDONED");
        }
        outcome = outcome.toUpperCase();

        String escalationReason = (String) parameters.get("escalation_reason");
        if ("ESCALATED".equals(outcome) && (escalationReason == null || escalationReason.isBlank())) {
            return ToolResult.error("Parameter 'escalation_reason' is required when outcome is ESCALATED");
        }

        SessionOutcome sessionOutcome = SessionOutcome.builder()
                .sessionId(session.getSessionId())
                .outcome(outcome)
                .useCaseId(session.getActiveUseCase())
                .escalationReason(escalationReason)
                .articlesShown(session.getArticlesShown())
                .totalTurns(session.getTotalBotTurns())
                .createdAt(OffsetDateTime.now())
                .build();

        sessionOutcomeRepository.save(sessionOutcome);

        log.info("Outcome recorded: session='{}', outcome='{}', useCase='{}'",
                session.getSessionId(), outcome, session.getActiveUseCase());

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("session_id", session.getSessionId());
        data.put("outcome", outcome);
        data.put("use_case_id", session.getActiveUseCase());

        return ToolResult.ok(data);
    }
}
