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
 *
 * <p>Sprint 9 §O0 — terminal tool contract alignment. The projected schema
 * now exposes {@code summary} as a recommended field, but a missing
 * {@code summary} no longer fails the tool: {@link #deriveFallbackSummary}
 * synthesises a safe summary from the session, the LLM-supplied
 * {@code current_user_message}, and the canonical {@code escalation_reason}
 * so cases like {@code request_handover(tool_scope_blocked)} can complete.
 * The 23-value canonical {@code escalation_reason} enum and the
 * {@code EscalationReasonResolver} precedence remain unchanged.
 *
 * <p>Sprint 16 §H0 — known-unspecced-surface marker (no behaviour
 * change in Sprint 16). On the LLM-driven path this tool's
 * {@link SalesforceService#requestHandover(String, java.util.Map)}
 * call is one of two independently-wired local handover writers (the
 * other is {@code SessionManager.recordHandover}). The future
 * invariant frozen by {@code docs/handover_orchestrator_design.md}
 * §3.3 / §6 is: for each {@code session_id}, at most one transmitted
 * / {@code offline_logged} handover decision may exist; the future
 * {@code HandoverOrchestrator} is the single owner and must be
 * idempotent by {@code session_id}. This tool MUST eventually
 * delegate to the orchestrator instead of calling
 * {@link SalesforceService} directly. See
 * {@code Sprint16HandoverDualPathReproTest} for the characterization
 * repro and {@code docs/release_gate.md} §1.1 for the cutover blocker.
 */
@Slf4j
@Component
public class RequestHandoverTool implements Tool {

    private static final String HANDOVER_SCHEMA_VERSION = "1.0";
    private static final int FALLBACK_SUMMARY_MAX_CHARS = 280;

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
        String escalationReason = readStringParam(parameters, "escalation_reason");
        if (escalationReason == null) {
            return ToolResult.error("Parameter 'escalation_reason' is required");
        }

        String summary = readStringParam(parameters, "summary");
        boolean summaryDerived = false;
        if (summary == null) {
            summary = deriveFallbackSummary(session, parameters, escalationReason);
            summaryDerived = true;
        }

        // Build the handover payload per Phase 3 section 3.6.2
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("version", HANDOVER_SCHEMA_VERSION);
        payload.put("session_id", session.getSessionId());
        payload.put("primary_use_case", session.getActiveUseCase());
        payload.put("candidate_use_cases", session.getCandidateUseCases());
        payload.put("current_status", session.getHandlingState());
        payload.put("summary", summary);
        if (summaryDerived) {
            payload.put("summary_source", "fallback");
        }
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
        payload.put("topic_uc_mismatch", parameters == null ? null : parameters.get("topic_uc_mismatch"));
        payload.put("prompt_version", session.getPromptVersion());
        payload.put("model_version", session.getModelVersion());

        String transferResult = salesforceService.requestHandover(session.getSessionId(), payload);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("transfer_result", transferResult);
        data.put("session_id", session.getSessionId());
        data.put("escalation_reason", escalationReason);
        data.put("summary", summary);
        if (summaryDerived) {
            data.put("summary_source", "fallback");
        }

        log.info("Handover requested: session='{}', reason='{}', result='{}', summary_source='{}'",
                session.getSessionId(), escalationReason, transferResult,
                summaryDerived ? "fallback" : "provided");

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

    /**
     * Sprint 9 §O0 — derive a safe handover summary when the LLM did not
     * supply one. Combines (in priority order) the most informative
     * non-blank source available — the LLM's {@code current_user_message},
     * the form description, the session's topic + UC, and the canonical
     * escalation reason. The result is bounded so it cannot smuggle large
     * PII payloads into the trace.
     */
    static String deriveFallbackSummary(BotSession session,
                                        Map<String, Object> parameters,
                                        String escalationReason) {
        String userMessage = parameters == null
                ? null
                : readStringParam(parameters, "current_user_message");

        String topic = session == null ? null : session.getFormTopicSubject();
        String uc = session == null ? null : session.getActiveUseCase();

        StringBuilder sb = new StringBuilder();
        if (uc != null && !uc.isBlank()) {
            sb.append("UC=").append(uc);
        }
        if (topic != null && !topic.isBlank()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append("topic=").append(topic);
        }
        if (escalationReason != null && !escalationReason.isBlank()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append("reason=").append(escalationReason);
        }
        if (userMessage != null && !userMessage.isBlank()) {
            if (sb.length() > 0) sb.append(" — ");
            sb.append(userMessage.trim());
        }
        if (sb.length() == 0) {
            sb.append("Handover requested with no summary supplied.");
        }
        String summary = sb.toString();
        if (summary.length() > FALLBACK_SUMMARY_MAX_CHARS) {
            summary = summary.substring(0, FALLBACK_SUMMARY_MAX_CHARS - 1) + "…";
        }
        return summary;
    }

    private static String readStringParam(Map<String, Object> parameters, String name) {
        if (parameters == null) return null;
        Object raw = parameters.get(name);
        if (!(raw instanceof String s)) return null;
        return s.isBlank() ? null : s;
    }
}
