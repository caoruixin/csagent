package com.gumtree.csagent.service.tools;

import com.gumtree.csagent.model.BotSession;
import com.gumtree.csagent.service.SalesforceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Builds the handover payload and delegates to SalesforceService.requestHandover().
 * AGENT_VISIBLE — allowed for ALL use cases.
 * Handover payload follows Phase 3 section 3.6.2 schema.
 */
@Slf4j
@Component
public class RequestHandoverTool implements Tool {

    private static final String HANDOVER_SCHEMA_VERSION = "1.0";

    private final SalesforceService salesforceService;

    public RequestHandoverTool(SalesforceService salesforceService) {
        this.salesforceService = salesforceService;
    }

    @Override
    public String getName() {
        return "request_handover";
    }

    @Override
    public ToolResult execute(BotSession session, Map<String, Object> parameters) {
        String escalationReason = (String) parameters.get("escalation_reason");
        if (escalationReason == null || escalationReason.isBlank()) {
            return ToolResult.error("Parameter 'escalation_reason' is required");
        }

        String summary = (String) parameters.get("summary");
        if (summary == null || summary.isBlank()) {
            return ToolResult.error("Parameter 'summary' is required for handover");
        }

        // Build the handover payload per Phase 3 section 3.6.2
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("version", HANDOVER_SCHEMA_VERSION);
        payload.put("session_id", session.getSessionId());
        payload.put("primary_use_case", session.getActiveUseCase());
        payload.put("candidate_use_cases", session.getCandidateUseCases());
        payload.put("current_status", session.getHandlingState());
        payload.put("summary", summary);
        payload.put("intent_confidence", session.getIntentConfidence());
        payload.put("clarification_count", session.getClarificationCount());
        payload.put("faq_miss_count", session.getFaqMissCount());
        payload.put("articles_shown", session.getArticlesShown());
        payload.put("escalation_reason", escalationReason);
        payload.put("case_id", session.getCaseId());
        payload.put("identifiers_collected", buildIdentifiersCollected(session));
        payload.put("intake_fields", session.getIntakeFields());
        payload.put("total_bot_turns", session.getTotalBotTurns());
        payload.put("form_topic_subject", session.getFormTopicSubject());
        payload.put("topic_uc_mismatch", parameters.get("topic_uc_mismatch"));
        payload.put("prompt_version", session.getPromptVersion());
        payload.put("model_version", session.getModelVersion());

        String transferResult = salesforceService.requestHandover(session.getSessionId(), payload);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("transfer_result", transferResult);
        data.put("session_id", session.getSessionId());
        data.put("escalation_reason", escalationReason);

        log.info("Handover requested: session='{}', reason='{}', result='{}'",
                session.getSessionId(), escalationReason, transferResult);

        return ToolResult.ok(data);
    }

    private Map<String, Object> buildIdentifiersCollected(BotSession session) {
        Map<String, Object> identifiers = new LinkedHashMap<>();
        if (session.getCustomerContext() != null) {
            identifiers.put("has_customer_context", true);
        }
        if (session.getListingContext() != null) {
            identifiers.put("has_listing_context", true);
        }
        if (session.getModerationContext() != null) {
            identifiers.put("has_moderation_context", true);
        }
        return identifiers;
    }
}
