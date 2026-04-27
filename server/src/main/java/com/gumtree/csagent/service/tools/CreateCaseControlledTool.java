package com.gumtree.csagent.service.tools;

import com.gumtree.csagent.model.BotSession;
import com.gumtree.csagent.service.SalesforceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Runtime-only, HIGH RISK tool that creates a Salesforce case.
 * Restricted to UC-H, UC-J, UC-K with required fields validated per UC.
 */
@Slf4j
@Component
public class CreateCaseControlledTool implements Tool {

    private static final Set<String> ALLOWED_UC = Set.of("UC-H", "UC-J", "UC-K");

    /**
     * Required intake fields per use case.
     * UC-H: ad removal appeal — needs description
     * UC-J: report a safety issue — needs subject, description
     * UC-K: account access issues — needs email, description
     */
    private static final Map<String, Set<String>> REQUIRED_FIELDS_BY_UC = Map.of(
            "UC-H", Set.of("description"),
            "UC-J", Set.of("subject", "description"),
            "UC-K", Set.of("email", "description")
    );

    private static final Map<String, String> QUEUE_BY_UC = Map.of(
            "UC-H", "Ad_Support_Queue",
            "UC-J", "Safety_Queue",
            "UC-K", "Account_Support_Queue"
    );

    private final SalesforceService salesforceService;

    public CreateCaseControlledTool(SalesforceService salesforceService) {
        this.salesforceService = salesforceService;
    }

    @Override
    public String getName() {
        return "create_case_controlled";
    }

    @Override
    @SuppressWarnings("unchecked")
    public ToolResult execute(BotSession session, Map<String, Object> parameters) {
        String activeUc = session.getActiveUseCase();
        if (activeUc == null || !ALLOWED_UC.contains(activeUc)) {
            return ToolResult.error("create_case_controlled is only allowed for UC-H, UC-J, UC-K. " +
                    "Current UC: " + activeUc);
        }

        // Validate required fields for this UC
        Set<String> requiredFields = REQUIRED_FIELDS_BY_UC.getOrDefault(activeUc, Set.of());
        List<String> missingFields = new ArrayList<>();
        for (String field : requiredFields) {
            Object value = parameters.get(field);
            if (value == null || (value instanceof String && ((String) value).isBlank())) {
                missingFields.add(field);
            }
        }
        if (!missingFields.isEmpty()) {
            return ToolResult.error("Missing required fields for " + activeUc + ": " + missingFields);
        }

        String subject = (String) parameters.getOrDefault("subject",
                activeUc + " case for session " + session.getSessionId());
        String description = (String) parameters.get("description");
        String email = (String) parameters.get("email");
        String adId = (String) parameters.get("ad_id");
        String queueName = QUEUE_BY_UC.getOrDefault(activeUc, "General_Queue");

        Map<String, Object> intakeFields = (Map<String, Object>) parameters.get("intake_fields");

        String caseId = salesforceService.createCase(
                session.getSessionId(),
                activeUc,
                subject,
                description,
                email,
                adId,
                queueName,
                intakeFields
        );

        log.info("Case created: caseId='{}', UC='{}', session='{}', queue='{}'",
                caseId, activeUc, session.getSessionId(), queueName);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("case_id", caseId);
        data.put("use_case_id", activeUc);
        data.put("queue_name", queueName);
        data.put("session_id", session.getSessionId());

        return ToolResult.ok(data);
    }
}
