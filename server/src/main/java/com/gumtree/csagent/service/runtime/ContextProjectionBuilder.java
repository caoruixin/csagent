package com.gumtree.csagent.service.runtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.gumtree.csagent.model.BotSession;
import com.gumtree.csagent.model.BotTurn;
import com.gumtree.csagent.model.KnowledgeHit;
import com.gumtree.csagent.model.PhasePlan;
import com.gumtree.csagent.model.TerminalOutcome;
import com.gumtree.csagent.service.tools.ToolPolicyEnforcer;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Builds the projected context JSON for LLM invocation.
 * Combines session state, form context, conversation history, knowledge results,
 * and current phase into a single context document.
 */
@Slf4j
@Service
public class ContextProjectionBuilder {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}"
    );

    private final ObjectMapper objectMapper;
    private final UseCaseRegistryService useCaseRegistry;
    private final ControlPolicyService controlPolicy;
    private final ToolPolicyEnforcer toolPolicyEnforcer;

    /**
     * Static schema map for agent_visible tools, populated in {@link #initToolSchemas()}.
     * Each entry contains an ObjectNode with {name, description, arguments_schema}
     * conforming to the OpenAI-style tool schema contract (see phase0_normative_freeze §0.6).
     */
    private final Map<String, ObjectNode> toolSchemas = new LinkedHashMap<>();

    public ContextProjectionBuilder(ObjectMapper objectMapper,
                                     UseCaseRegistryService useCaseRegistry,
                                     ControlPolicyService controlPolicy,
                                     ToolPolicyEnforcer toolPolicyEnforcer) {
        this.objectMapper = objectMapper;
        this.useCaseRegistry = useCaseRegistry;
        this.controlPolicy = controlPolicy;
        this.toolPolicyEnforcer = toolPolicyEnforcer;
    }

    @PostConstruct
    void initToolSchemas() {
        toolSchemas.put("search_knowledge", buildToolSchema(
                "search_knowledge",
                "Search the knowledge base for FAQ articles relevant to the user's question. "
                        + "Returns a list of articles with source_ids.",
                buildSearchKnowledgeArgsSchema()));

        toolSchemas.put("resolve_article", buildToolSchema(
                "resolve_article",
                "Fetch the full content of a specific knowledge article by source_id.",
                buildSingleStringFieldSchema("source_id", true)));

        toolSchemas.put("get_customer_context", buildToolSchema(
                "get_customer_context",
                "Look up the customer's account / ad / moderation context. "
                        + "Auto-triggered at INIT when email is in form_context.",
                buildGetCustomerContextArgsSchema()));

        toolSchemas.put("request_handover", buildToolSchema(
                "request_handover",
                "Escalate the session to a human agent. Use when the user explicitly requests human help, "
                        + "when the issue requires human action, or after intake is complete for UC-G/H/I/J/K.",
                buildRequestHandoverArgsSchema()));

        toolSchemas.put("record_outcome", buildToolSchema(
                "record_outcome",
                "Record the final outcome of the session (resolved / escalated / abandoned).",
                buildRecordOutcomeArgsSchema()));

        // 2026-05-02 — Fix 3c: classify_use_case tool exposed during DISCOVER
        // so the LLM can commit a UC once intent is clear (closes
        // CONTRACT_VIOLATION:active_use_case missing_after_turns).
        toolSchemas.put("classify_use_case", buildToolSchema(
                "classify_use_case",
                "Commit a use-case classification once you have identified the user's intent. "
                        + "Sets session.active_use_case + session.intent_confidence and unlocks the "
                        + "DISCOVER -> RESOLVE transition. Use confidence >= 0.7 when the intent is "
                        + "clear, >= 0.5 with explicit topic + at least one supporting detail; below "
                        + "0.5, ask another clarifying question instead of calling this tool.",
                buildClassifyUseCaseArgsSchema()));

        log.info("Initialized {} tool schemas for context projection", toolSchemas.size());
    }

    private ObjectNode buildToolSchema(String name, String description, ObjectNode argumentsSchema) {
        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("name", name);
        schema.put("description", description);
        schema.set("arguments_schema", argumentsSchema);
        return schema;
    }

    private ObjectNode buildSearchKnowledgeArgsSchema() {
        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "object");
        ObjectNode props = objectMapper.createObjectNode();
        ObjectNode queryProp = objectMapper.createObjectNode();
        queryProp.put("type", "string");
        props.set("query", queryProp);
        ObjectNode ucTagsProp = objectMapper.createObjectNode();
        ucTagsProp.put("type", "array");
        ObjectNode ucTagsItems = objectMapper.createObjectNode();
        ucTagsItems.put("type", "string");
        ucTagsProp.set("items", ucTagsItems);
        props.set("uc_tags", ucTagsProp);
        schema.set("properties", props);
        ArrayNode required = objectMapper.createArrayNode();
        required.add("query");
        schema.set("required", required);
        return schema;
    }

    private ObjectNode buildSingleStringFieldSchema(String fieldName, boolean isRequired) {
        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "object");
        ObjectNode props = objectMapper.createObjectNode();
        ObjectNode prop = objectMapper.createObjectNode();
        prop.put("type", "string");
        props.set(fieldName, prop);
        schema.set("properties", props);
        if (isRequired) {
            ArrayNode required = objectMapper.createArrayNode();
            required.add(fieldName);
            schema.set("required", required);
        }
        return schema;
    }

    private ObjectNode buildGetCustomerContextArgsSchema() {
        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "object");
        ObjectNode props = objectMapper.createObjectNode();
        ObjectNode emailProp = objectMapper.createObjectNode();
        emailProp.put("type", "string");
        props.set("email", emailProp);
        ObjectNode adIdProp = objectMapper.createObjectNode();
        adIdProp.put("type", "string");
        props.set("ad_id", adIdProp);
        schema.set("properties", props);
        return schema;
    }

    private ObjectNode buildRequestHandoverArgsSchema() {
        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "object");
        ObjectNode props = objectMapper.createObjectNode();
        ObjectNode reasonProp = objectMapper.createObjectNode();
        reasonProp.put("type", "string");
        ArrayNode enumValues = objectMapper.createArrayNode();
        // Canonical 23-value escalation_reason enum. Mirrors
        // eval_interactive/eval_interactive/case_spec/schema.py:43-66
        // (EscalationTrigger Literal). Keep these two lists in lockstep.
        // Grouped by source: user-driven, FAQ/clarification budget, intake-
        // complete (UC-G..K), policy/safety, system/guardrail.
        // User-driven
        enumValues.add("user_requested");
        enumValues.add("user_distress");
        // FAQ / clarification budget
        enumValues.add("faq_miss_threshold_exceeded");
        enumValues.add("clarification_budget_exhausted");
        enumValues.add("incomplete_intake");
        // Intake-complete (per UC)
        enumValues.add("intake_complete_for_uc_g");
        enumValues.add("intake_complete_for_uc_h");
        enumValues.add("intake_complete_for_uc_i");
        enumValues.add("intake_complete_for_uc_j");
        enumValues.add("intake_complete_for_uc_k");
        // Policy / safety / compliance
        enumValues.add("payment_dispute_detected");
        enumValues.add("appeal_requires_human");
        enumValues.add("imminent_harm");
        enumValues.add("incorrect_deletion_appeal");
        enumValues.add("trust_safety_required");
        enumValues.add("account_compliance");
        enumValues.add("gdpr_intake");
        enumValues.add("identity_verification_required");
        // System / guardrail / scope
        enumValues.add("out_of_scope");
        enumValues.add("service_degraded");
        enumValues.add("turn_budget_exhausted");
        enumValues.add("tool_scope_blocked");
        enumValues.add("runtime_error_threshold");
        reasonProp.set("enum", enumValues);
        props.set("escalation_reason", reasonProp);
        schema.set("properties", props);
        ArrayNode required = objectMapper.createArrayNode();
        required.add("escalation_reason");
        schema.set("required", required);
        return schema;
    }

    /**
     * 2026-05-02 — Fix 3c: schema for the {@code classify_use_case} tool.
     * Mirrors {@link #buildRequestHandoverArgsSchema()} structurally; the
     * {@code use_case_id} enum lists the V1 UC IDs.
     */
    private ObjectNode buildClassifyUseCaseArgsSchema() {
        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "object");
        ObjectNode props = objectMapper.createObjectNode();

        ObjectNode useCaseProp = objectMapper.createObjectNode();
        useCaseProp.put("type", "string");
        ArrayNode ucEnum = objectMapper.createArrayNode();
        // Canonical V1 use-case ID set; mirrors UseCaseRegistryService entries.
        ucEnum.add("UC-A");
        ucEnum.add("UC-B");
        ucEnum.add("UC-C");
        ucEnum.add("UC-D");
        ucEnum.add("UC-E");
        ucEnum.add("UC-F");
        ucEnum.add("UC-FP");
        ucEnum.add("UC-G");
        ucEnum.add("UC-H");
        ucEnum.add("UC-I");
        ucEnum.add("UC-J");
        ucEnum.add("UC-K");
        useCaseProp.set("enum", ucEnum);
        props.set("use_case_id", useCaseProp);

        ObjectNode confidenceProp = objectMapper.createObjectNode();
        confidenceProp.put("type", "number");
        confidenceProp.put("minimum", 0);
        confidenceProp.put("maximum", 1);
        props.set("confidence", confidenceProp);

        ObjectNode reasoningProp = objectMapper.createObjectNode();
        reasoningProp.put("type", "string");
        props.set("reasoning", reasoningProp);

        schema.set("properties", props);
        ArrayNode required = objectMapper.createArrayNode();
        required.add("use_case_id");
        required.add("confidence");
        schema.set("required", required);
        return schema;
    }

    private ObjectNode buildRecordOutcomeArgsSchema() {
        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "object");
        ObjectNode props = objectMapper.createObjectNode();
        ObjectNode outcomeProp = objectMapper.createObjectNode();
        outcomeProp.put("type", "string");
        ArrayNode enumValues = objectMapper.createArrayNode();
        enumValues.add("resolve");
        enumValues.add("escalate");
        enumValues.add("abandon");
        outcomeProp.set("enum", enumValues);
        props.set("outcome_class", outcomeProp);
        schema.set("properties", props);
        ArrayNode required = objectMapper.createArrayNode();
        required.add("outcome_class");
        schema.set("required", required);
        return schema;
    }

    /**
     * Build a projected context JSON string for the LLM.
     *
     * @param session          current bot session
     * @param conversationHistory previous turns in the session
     * @param knowledgeHits    retrieved knowledge articles (may be null)
     * @param userMessage      the current user message
     * @return JSON string representing the full projected context
     */
    public String buildProjection(BotSession session,
                                   List<BotTurn> conversationHistory,
                                   List<KnowledgeHit> knowledgeHits,
                                   String userMessage) {
        try {
            ObjectNode projection = objectMapper.createObjectNode();

            // Session state
            ObjectNode sessionNode = objectMapper.createObjectNode();
            sessionNode.put("session_id", session.getSessionId());
            sessionNode.put("current_phase", session.getCurrentPhase());
            sessionNode.put("active_use_case", session.getActiveUseCase());
            sessionNode.put("total_bot_turns", session.getTotalBotTurns());
            sessionNode.put("clarification_count", session.getClarificationCount());
            sessionNode.put("faq_miss_count", session.getFaqMissCount());
            sessionNode.put("handling_state", session.getHandlingState());
            projection.set("session", sessionNode);

            // Task summary
            String activeUc = session.getActiveUseCase();
            String taskSummary = buildTaskSummary(session);
            projection.put("task_summary", taskSummary);

            // Risk flags from UC registry
            ArrayNode riskFlagsNode = objectMapper.createArrayNode();
            if (activeUc != null) {
                UseCaseRegistryService.UseCaseDefinition ucDef = useCaseRegistry.getUseCase(activeUc);
                if (ucDef != null && ucDef.riskLevel() != null) {
                    riskFlagsNode.add(ucDef.riskLevel());
                }
            }
            projection.set("risk_flags", riskFlagsNode);

            // Sprint 7 §I2 — intake_state projection. Surfaces required /
            // collected / remaining fields and intake_complete for INTAKE-path
            // UCs (UC-G/H/I/J/K) so the LLM can ask only for missing fields
            // and avoid stamping intake_complete_for_uc_X prematurely. Other
            // UCs (FAQ-path, DISCOVER, etc.) skip this slot entirely.
            if (IntakeFieldsRegistry.isIntakeUseCase(activeUc)) {
                ObjectNode intakeStateNode = objectMapper.createObjectNode();
                List<String> required = IntakeFieldsRegistry.requiredFieldsFor(activeUc);
                Map<String, String> collected = IntakeFieldsRegistry.parseCollectedFields(
                        objectMapper, session.getIntakeFields());
                List<String> remaining = IntakeFieldsRegistry.fieldsRemaining(activeUc, collected);

                ArrayNode requiredNode = objectMapper.createArrayNode();
                for (String f : required) requiredNode.add(f);
                intakeStateNode.set("required_fields", requiredNode);

                ObjectNode collectedNode = objectMapper.createObjectNode();
                for (Map.Entry<String, String> entry : collected.entrySet()) {
                    if (required.contains(entry.getKey())) {
                        collectedNode.put(entry.getKey(), entry.getValue());
                    }
                }
                intakeStateNode.set("fields_collected", collectedNode);

                ArrayNode remainingNode = objectMapper.createArrayNode();
                for (String f : remaining) remainingNode.add(f);
                intakeStateNode.set("fields_remaining", remainingNode);

                intakeStateNode.put("intake_complete",
                        IntakeFieldsRegistry.intakeComplete(activeUc, collected));

                projection.set("intake_state", intakeStateNode);
            }

            // Sprint 7 §I0 — C5 candidate_use_cases projection. Surfaces the
            // routing-derived candidate UC list so the DISCOVER cue can act
            // on it deterministically. Empty array signals "no candidates yet"
            // (UNKNOWN topic + empty description), which the DISCOVER
            // systemInstruction reads as the trigger to gather evidence
            // before escalating with faq_miss_threshold_exceeded.
            ArrayNode candidateUcsNode = objectMapper.createArrayNode();
            if (session.getCandidateUseCases() != null) {
                for (String uc : session.getCandidateUseCases()) {
                    if (uc != null && !uc.isBlank()) {
                        candidateUcsNode.add(uc);
                    }
                }
            }
            projection.set("candidate_use_cases", candidateUcsNode);

            // Budget state
            ObjectNode budgetNode = objectMapper.createObjectNode();
            budgetNode.put("total_bot_turns", session.getTotalBotTurns());
            budgetNode.put("max_bot_turns", getMaxBotTurns(session));
            budgetNode.put("clarification_count", session.getClarificationCount());
            budgetNode.put("max_clarification", controlPolicy.getMaxClarificationRounds());
            budgetNode.put("faq_miss_count", session.getFaqMissCount());
            budgetNode.put("max_faq_miss", controlPolicy.getMaxFaqMiss());
            projection.set("budget_state", budgetNode);

            // Tool schemas (visible tools for current UC) — full per-tool schema objects.
            // ToolPolicyEnforcer drives WHICH tools appear; the static schema map provides the
            // {name, description, arguments_schema} payload. Per phase0 §0.6, this is the sole
            // tool-discovery channel for the LLM (single-layer tool-use; no per-phase action list).
            ArrayNode toolSchemasNode = objectMapper.createArrayNode();
            if (activeUc != null) {
                for (String toolName : toolPolicyEnforcer.getVisibleToolsForUc(activeUc)) {
                    ObjectNode schema = toolSchemas.get(toolName);
                    if (schema != null) {
                        toolSchemasNode.add(schema.deepCopy());
                    } else {
                        log.warn("No static tool schema registered for visible tool '{}' (UC={})",
                                toolName, activeUc);
                    }
                }
            }
            projection.set("tool_schemas", toolSchemasNode);

            // Form context (pre-chat form data)
            if (session.getFormContext() != null && !session.getFormContext().isBlank()) {
                projection.set("form_context", objectMapper.readTree(session.getFormContext()));
            }

            // Customer context
            if (session.getCustomerContext() != null && !session.getCustomerContext().isBlank()) {
                projection.set("customer_context", objectMapper.readTree(session.getCustomerContext()));
            }

            // Listing context
            if (session.getListingContext() != null && !session.getListingContext().isBlank()) {
                projection.set("listing_context", objectMapper.readTree(session.getListingContext()));
            }

            // Conversation history (last 10 turns max to limit context window)
            ArrayNode historyNode = objectMapper.createArrayNode();
            int startIdx = Math.max(0, conversationHistory.size() - 10);
            for (int i = startIdx; i < conversationHistory.size(); i++) {
                BotTurn turn = conversationHistory.get(i);
                ObjectNode turnNode = objectMapper.createObjectNode();
                turnNode.put("turn_index", turn.getTurnIndex());
                turnNode.put("user_message", redactPii(turn.getUserMessage()));
                turnNode.put("bot_response", turn.getBotResponse());
                if (turn.getToolCalls() != null && !turn.getToolCalls().isBlank()) {
                    try {
                        turnNode.set("tool_calls", objectMapper.readTree(turn.getToolCalls()));
                    } catch (Exception ex) {
                        turnNode.put("tool_calls", turn.getToolCalls());
                    }
                }
                historyNode.add(turnNode);
            }
            projection.set("conversation_history", historyNode);

            // Knowledge hits (if any)
            if (knowledgeHits != null && !knowledgeHits.isEmpty()) {
                ArrayNode hitsNode = objectMapper.createArrayNode();
                for (KnowledgeHit hit : knowledgeHits) {
                    ObjectNode hitNode = objectMapper.createObjectNode();
                    hitNode.put("source_id", hit.getSourceId());
                    hitNode.put("title", hit.getTitle());
                    hitNode.put("snippet", hit.getSnippet());
                    hitNode.put("score", hit.getScore());
                    hitsNode.add(hitNode);
                }
                projection.set("knowledge_hits", hitsNode);

                // Phase-aware instruction: tell the LLM knowledge is pre-searched (tool-use phrasing)
                projection.put("knowledge_instruction",
                        "Knowledge results have already been retrieved and are provided above in 'knowledge_hits'. "
                        + "Do NOT call search_knowledge again. "
                        + "To answer the user, return a non-empty user_message that grounds in the provided knowledge "
                        + "(cite source_ids from knowledge_hits). "
                        + "To ask a clarifying question, return a non-empty user_message ending with '?'. "
                        + "To escalate, include a tool_call to request_handover with an appropriate escalation_reason.");
            }

            // Current user message
            projection.put("current_user_message", redactPii(userMessage));

            return objectMapper.writeValueAsString(projection);

        } catch (Exception e) {
            log.error("Failed to build context projection: {}", e.getMessage(), e);
            return "{}";
        }
    }

    /**
     * D16 PhasePlan-aware projection. Builds a base projection via the legacy
     * {@link #buildProjection(BotSession, List, List, String)} path (with no
     * pre-loaded knowledge hits — the loop fetches knowledge via tools, and
     * results land in {@code accumulatedToolResults}), then injects two
     * additional fields the {@code AgentRunLoop} requires:
     *
     * <ul>
     *   <li>{@code phase_plan} — the full {@link PhasePlan} except
     *       {@code maxToolSteps} (which is enforced server-side and not
     *       shown to the model). Includes objective, allowed_tools (names),
     *       grounding_instruction, system_instruction,
     *       valid_terminal_outcomes, escalation_policy.</li>
     *   <li>{@code accumulated_tool_results} — a map of {@code tool_name ->
     *       result_data} so the LLM can see what previous loop iterations
     *       have already discovered.</li>
     * </ul>
     *
     * <p>Per Phase 4 §D16.B.3, the legacy {@link #buildProjection} method is
     * preserved for non-loop callers.
     *
     * @param session                  current bot session
     * @param history                  prior turns in the session
     * @param plan                     the phase plan from {@code PhaseEvaluator.plan()}
     * @param userMessage              the current user message
     * @param accumulatedToolResults   map of tool_name -> result.data accumulated
     *                                 across prior loop iterations (may be {@code null})
     * @return JSON string representing the full plan-aware projection
     */
    public String build(BotSession session,
                        List<BotTurn> history,
                        PhasePlan plan,
                        String userMessage,
                        Map<String, Object> accumulatedToolResults) {
        // Delegate to legacy path for the bulk of the projection. We pass
        // null knowledgeHits because in the run-loop world, knowledge results
        // arrive via accumulatedToolResults (under search_knowledge), not as
        // a pre-loaded slot.
        String baseJson = buildProjection(session, history, null, userMessage);

        if (plan == null && (accumulatedToolResults == null || accumulatedToolResults.isEmpty())) {
            return baseJson;
        }

        try {
            ObjectNode projection = (ObjectNode) objectMapper.readTree(baseJson);

            // Inject the PhasePlan (excluding maxToolSteps).
            if (plan != null) {
                ObjectNode planNode = objectMapper.createObjectNode();
                planNode.put("phase", plan.phase());
                if (plan.useCase() != null) planNode.put("use_case", plan.useCase());
                if (plan.objective() != null) planNode.put("objective", plan.objective());

                ArrayNode allowedToolsNode = objectMapper.createArrayNode();
                if (plan.allowedTools() != null) {
                    for (String t : plan.allowedTools()) allowedToolsNode.add(t);
                }
                planNode.set("allowed_tools", allowedToolsNode);

                // Sprint 8.1 §M3 (2026-05-07): when a PhasePlan is present
                // the projected `tool_schemas` array MUST be filtered to
                // {@code plan.allowedTools}. Previously the per-UC
                // tool-policy enforcer dictated which schemas were
                // projected, and the plan only ENRICHED that list — so a
                // DISCOVER plan whose allowedTools is
                // {{search_knowledge, classify_use_case}} would still have
                // every UC-specific policy tool (resolve_article,
                // record_outcome, request_handover, get_customer_context)
                // projected to the LLM. The {@link
                // com.gumtree.csagent.service.tools.ToolDispatcher#validateAgainstPlan}
                // whitelist would later reject those calls, so the LLM
                // could legitimately produce a plan-rejected handover even
                // though the projection invited it. Replacing the array
                // with the plan-allowed schemas keeps DISCOVER focused on
                // the two tools it can actually dispatch.
                //
                // Schemas for tools in `allowed_tools` that don't have a
                // registered static schema are skipped silently (logged at
                // WARN); the LLM still sees the tool name in
                // `phase_plan.allowed_tools`.
                if (plan.allowedTools() != null) {
                    ArrayNode filteredToolSchemas = objectMapper.createArrayNode();
                    java.util.Set<String> seen = new java.util.HashSet<>();
                    for (String toolName : plan.allowedTools()) {
                        if (toolName == null || toolName.isBlank() || !seen.add(toolName)) {
                            continue;
                        }
                        ObjectNode schema = toolSchemas.get(toolName);
                        if (schema != null) {
                            filteredToolSchemas.add(schema.deepCopy());
                        } else {
                            log.warn(
                                    "Sprint 8.1 §M3: no static schema registered for "
                                            + "phase_plan.allowed_tools entry '{}' (phase={}, uc={})",
                                    toolName, plan.phase(), plan.useCase());
                        }
                    }
                    projection.set("tool_schemas", filteredToolSchemas);
                }

                if (plan.groundingInstruction() != null) {
                    planNode.put("grounding_instruction", plan.groundingInstruction());
                }
                if (plan.systemInstruction() != null) {
                    planNode.put("system_instruction", plan.systemInstruction());
                }
                if (plan.escalationPolicy() != null) {
                    planNode.put("escalation_policy", plan.escalationPolicy());
                }

                ArrayNode terminalOutcomesNode = objectMapper.createArrayNode();
                if (plan.validTerminalOutcomes() != null) {
                    for (TerminalOutcome to : plan.validTerminalOutcomes()) {
                        terminalOutcomesNode.add(to.name());
                    }
                }
                planNode.set("valid_terminal_outcomes", terminalOutcomesNode);

                projection.set("phase_plan", planNode);
            }

            // Inject accumulated tool results (last-write-wins per tool name).
            if (accumulatedToolResults != null && !accumulatedToolResults.isEmpty()) {
                ObjectNode toolResultsNode = objectMapper.createObjectNode();
                for (Map.Entry<String, Object> e : accumulatedToolResults.entrySet()) {
                    if (e.getValue() == null) {
                        toolResultsNode.putNull(e.getKey());
                    } else {
                        toolResultsNode.set(e.getKey(), objectMapper.valueToTree(e.getValue()));
                    }
                }
                projection.set("accumulated_tool_results", toolResultsNode);
            }

            return objectMapper.writeValueAsString(projection);
        } catch (Exception ex) {
            log.warn("Failed to inject phase_plan / accumulated_tool_results into projection: {}",
                    ex.getMessage());
            return baseJson;
        }
    }

    /**
     * Basic PII redaction: replace email addresses with placeholder.
     * Full PII handling is deferred to DM6.
     */
    private String redactPii(String text) {
        if (text == null) {
            return null;
        }
        return EMAIL_PATTERN.matcher(text).replaceAll("[REDACTED_EMAIL]");
    }

    /**
     * Build a human-readable task summary from session state.
     */
    private String buildTaskSummary(BotSession session) {
        StringBuilder sb = new StringBuilder();
        String topicSubject = session.getFormTopicSubject();
        String activeUc = session.getActiveUseCase();
        String phase = session.getCurrentPhase();

        if (topicSubject != null) {
            sb.append("User inquiry about ").append(topicSubject).append(". ");
        }
        if (activeUc != null) {
            UseCaseRegistryService.UseCaseDefinition ucDef = useCaseRegistry.getUseCase(activeUc);
            String ucName = ucDef != null ? ucDef.name() : activeUc;
            sb.append(ucName).append(" detected");
            if (session.getIntentConfidence() != null) {
                sb.append(" with ").append(session.getIntentConfidence()).append(" confidence");
            }
            sb.append(". ");
        }
        if (phase != null) {
            sb.append("Current phase: ").append(phase).append(".");
        }
        return sb.toString().trim();
    }

    /**
     * Get the appropriate max bot turns based on UC type.
     */
    private int getMaxBotTurns(BotSession session) {
        String activeUc = session.getActiveUseCase();
        if (activeUc != null) {
            UseCaseRegistryService.UseCaseDefinition ucDef = useCaseRegistry.getUseCase(activeUc);
            if (ucDef != null && "INTAKE".equals(ucDef.path())) {
                return controlPolicy.getMaxBotTurnsIntake();
            }
        }
        return controlPolicy.getMaxBotTurnsFaq();
    }
}
