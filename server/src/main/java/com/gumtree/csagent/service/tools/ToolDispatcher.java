package com.gumtree.csagent.service.tools;

import com.gumtree.csagent.config.MockProperties;
import com.gumtree.csagent.model.BotSession;
import com.gumtree.csagent.model.PhasePlan;
import com.gumtree.csagent.service.guardrails.ProgressPlaceholderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Central dispatcher that routes tool calls through policy enforcement
 * before delegating to the appropriate Tool implementation.
 */
@Slf4j
@Component
public class ToolDispatcher {

    /**
     * Canonical 23-value {@code escalation_reason} enum — must mirror
     * {@code PhaseEvaluator.CANONICAL_ESCALATION_REASONS} and the enum surfaced
     * by {@code ContextProjectionBuilder.buildRequestHandoverArgsSchema()}.
     *
     * <p>Phase 2 #16 belt-and-suspenders: even though the system_prompt now
     * carries an explicit decision tree, a sloppy LLM emission must never reach
     * the trace as a {@code CONTRACT_VIOLATION:enum_violation}. Any non-canonical
     * value emitted on a {@code request_handover} call is logged WARN and
     * coerced to {@code "service_degraded"} (the catch-all for
     * infrastructure/agent fallbacks).
     */
    private static final Set<String> CANONICAL_ESCALATION_REASONS = Set.of(
            "user_requested",
            "faq_miss_threshold_exceeded",
            "clarification_budget_exhausted",
            "intake_complete_for_uc_g",
            "intake_complete_for_uc_h",
            "intake_complete_for_uc_i",
            "intake_complete_for_uc_j",
            "intake_complete_for_uc_k",
            "incomplete_intake",
            "payment_dispute_detected",
            "appeal_requires_human",
            "user_distress",
            "imminent_harm",
            "incorrect_deletion_appeal",
            "trust_safety_required",
            "account_compliance",
            "gdpr_intake",
            "identity_verification_required",
            "out_of_scope",
            "service_degraded",
            "turn_budget_exhausted",
            "tool_scope_blocked",
            "runtime_error_threshold");

    private final ToolPolicyEnforcer policyEnforcer;
    private final ProgressPlaceholderService placeholderService;
    private final MockProperties mockProperties;
    private final Map<String, Tool> toolRegistry = new LinkedHashMap<>();

    public ToolDispatcher(ToolPolicyEnforcer policyEnforcer, List<Tool> tools,
                          ProgressPlaceholderService placeholderService,
                          MockProperties mockProperties) {
        this.policyEnforcer = policyEnforcer;
        this.placeholderService = placeholderService;
        this.mockProperties = mockProperties;
        for (Tool tool : tools) {
            toolRegistry.put(tool.getName(), tool);
            log.debug("Registered tool: {}", tool.getName());
        }
        log.info("ToolDispatcher initialized with {} tools: {}", toolRegistry.size(), toolRegistry.keySet());
    }

    /**
     * Dispatch a tool call: check policy, then execute.
     *
     * @param toolName   snake_case tool name
     * @param session    current bot session
     * @param parameters tool-specific parameters
     * @return tool result (success or error)
     */
    public ToolResult dispatch(String toolName, BotSession session, Map<String, Object> parameters) {
        log.info("Dispatching tool '{}' for session '{}'", toolName, session.getSessionId());

        // 1. Lookup tool
        Tool tool = toolRegistry.get(toolName);
        if (tool == null) {
            log.error("Unknown tool '{}' requested for session '{}'", toolName, session.getSessionId());
            return ToolResult.error("Unknown tool: " + toolName);
        }

        // 2. Check policy
        String activeUseCase = session.getActiveUseCase();
        if (!policyEnforcer.isToolAllowed(toolName, activeUseCase)) {
            policyEnforcer.emitBlockedEvent(session.getSessionId(), toolName, activeUseCase);
            return ToolResult.error(String.format(
                    "Tool '%s' is not allowed for use case '%s'", toolName,
                    activeUseCase != null ? activeUseCase : "none"));
        }

        // 2.5. Belt-and-suspenders canonicalization for request_handover.
        // Phase 2 #16: even though the prompt now carries an explicit decision
        // tree and the tool schema declares the canonical 23-value enum, a
        // sloppy LLM emission (or the ActionParser parse-failure fallback,
        // which historically emitted "system_failure") must never reach the
        // trace as a CV. Coerce non-canonical reasons to "service_degraded"
        // with a WARN log so we can see prompt-side regressions in telemetry
        // without breaking L1. We rebuild a mutable copy because callers may
        // pass an immutable Map.of(...) (see ActionParser.buildFallback()).
        if ("request_handover".equals(toolName) && parameters != null) {
            Object reasonRaw = parameters.get("escalation_reason");
            if (reasonRaw instanceof String reasonStr
                    && !CANONICAL_ESCALATION_REASONS.contains(reasonStr)) {
                log.warn("Session {}: LLM emitted non-canonical escalation_reason '{}' on request_handover; "
                                + "coercing to 'service_degraded'",
                        session.getSessionId(), reasonStr);
                Map<String, Object> mutable = new LinkedHashMap<>(parameters);
                mutable.put("escalation_reason", "service_degraded");
                parameters = mutable;
            }
        }

        // 3. Execute with latency tracking and optional artificial delay
        try {
            long toolStart = System.currentTimeMillis();

            // Optional artificial delay for demo/testing placeholder behavior
            int artificialDelay = mockProperties.getToolLatencyMs();
            if (artificialDelay > 0) {
                try {
                    Thread.sleep(artificialDelay);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }

            ToolResult result = tool.execute(session, parameters);
            long toolLatency = System.currentTimeMillis() - toolStart;

            // Track latency on result
            result.setLatencyMs(toolLatency);

            // Log if placeholder threshold exceeded
            if (toolLatency > 1500) {
                String placeholder = placeholderService.getPlaceholder();
                log.info("Tool '{}' took {}ms (>1500ms threshold). Placeholder: '{}'",
                        toolName, toolLatency, placeholder);
                result.setPlaceholderSent(true);
                result.setPlaceholderMessage(placeholder);
            }

            log.info("Tool '{}' completed: success={}, latency={}ms", toolName, result.isSuccess(), toolLatency);
            return result;
        } catch (Exception e) {
            log.error("Tool '{}' failed with exception: {}", toolName, e.getMessage(), e);
            return ToolResult.error("Tool execution failed: " + e.getMessage());
        }
    }

    /**
     * Validate a tool name against a {@link PhasePlan}'s allowed-tools whitelist.
     *
     * <p>Per Phase 3 §3.3.3 (v9 — D16), the {@code AgentRunLoop} calls this
     * before every dispatch so a phase plan can hard-restrict which tools the
     * LLM is permitted to invoke (e.g. INTAKE phases must not call
     * {@code search_knowledge}).
     *
     * <p>Semantics:
     * <ul>
     *   <li>{@code plan == null} → ok (legacy path; no plan-side validation)</li>
     *   <li>{@code plan.allowedTools() == null} → ok (plan does not constrain tools)</li>
     *   <li>{@code plan.allowedTools().contains(toolName)} → ok</li>
     *   <li>otherwise → error with reason {@code tool_not_in_plan: <tool> not in <list>}</li>
     * </ul>
     *
     * <p>This is a pre-dispatch check; the existing per-UC
     * {@link ToolPolicyEnforcer} check in {@link #dispatch} still runs.
     */
    public ToolResult validateAgainstPlan(PhasePlan plan, String toolName) {
        if (plan == null) {
            return ToolResult.ok(null);
        }
        if (plan.allowedTools() == null) {
            return ToolResult.ok(null);
        }
        if (plan.allowedTools().contains(toolName)) {
            return ToolResult.ok(null);
        }
        return ToolResult.error("tool_not_in_plan: " + toolName
                + " not in " + plan.allowedTools());
    }

    /**
     * Get the list of AGENT_VISIBLE tools allowed for a use case.
     */
    public List<String> getVisibleToolsForUc(String activeUseCase) {
        return policyEnforcer.getVisibleToolsForUc(activeUseCase);
    }

    /**
     * Check if a tool exists in the registry.
     */
    public boolean hasToolNamed(String toolName) {
        return toolRegistry.containsKey(toolName);
    }
}
