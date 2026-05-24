package com.gumtree.csagent.service.runtime;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.gumtree.csagent.model.BotSession;
import com.gumtree.csagent.model.BotTurn;
import com.gumtree.csagent.model.KnowledgeHit;
import com.gumtree.csagent.model.PhasePlan;
import com.gumtree.csagent.model.TerminalOutcome;
import com.gumtree.csagent.model.ToolEvent;
import com.gumtree.csagent.service.runtime.skill.Skill;
import com.gumtree.csagent.service.runtime.skill.SkillRegistry;
import com.gumtree.csagent.service.tools.ToolPolicyEnforcer;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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

    /**
     * Sprint 41 — aging window (in turns) for the {@code prior_use_case_carry}
     * projection slot per design doc §10.4 + OLD Sprint 36 OQ 7.7 default
     * carried forward. The slot is null when the most recent UC switch is
     * more than {@value} turns ago. Single integer constant: NO per-UC
     * variation per §1.7.
     */
    static final int PRIOR_USE_CASE_CARRY_AGING_TURNS = 4;

    /**
     * Sprint 41 — cap on the number of citations surfaced in the
     * {@code prior_use_case_carry} slot per design doc §10.4 + OLD Sprint 36
     * OQ 7.7 default carried forward. Single integer constant: NO per-UC
     * variation per §1.7.
     */
    static final int PRIOR_USE_CASE_CARRY_CITATION_CAP = 3;

    private final ObjectMapper objectMapper;
    private final UseCaseRegistryService useCaseRegistry;
    private final ControlPolicyService controlPolicy;
    private final ToolPolicyEnforcer toolPolicyEnforcer;
    private final SkillRegistry skillRegistry;

    /**
     * Sprint 20 Track B — stable JSON canonicaliser for the
     * {@code already_called.arguments_hash} projection slot. Configured to
     * sort map entries by key so two ToolCalls with identical content but
     * different field-emission order hash identically. Reused per build()
     * call; instance-shared.
     */
    private final ObjectMapper argumentsHashMapper = new ObjectMapper()
            .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);

    /**
     * Static schema map for agent_visible tools, populated in {@link #initToolSchemas()}.
     * Each entry contains an ObjectNode with {name, description, arguments_schema}
     * conforming to the OpenAI-style tool schema contract (see phase0_normative_freeze §0.6).
     */
    private final Map<String, ObjectNode> toolSchemas = new LinkedHashMap<>();

    public ContextProjectionBuilder(ObjectMapper objectMapper,
                                     UseCaseRegistryService useCaseRegistry,
                                     ControlPolicyService controlPolicy,
                                     ToolPolicyEnforcer toolPolicyEnforcer,
                                     SkillRegistry skillRegistry) {
        this.objectMapper = objectMapper;
        this.useCaseRegistry = useCaseRegistry;
        this.controlPolicy = controlPolicy;
        this.toolPolicyEnforcer = toolPolicyEnforcer;
        this.skillRegistry = skillRegistry;
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
        // Sprint 9 §O0: expose `summary` as a recommended field on the
        // handover schema so the LLM is nudged to provide a one-line
        // synopsis. Tool implementation derives a safe fallback when
        // missing, so this field is NOT marked required (the runtime
        // already has enough session state to synthesise a usable
        // summary on its own — required-but-fallback would have meant
        // tool_scope_blocked handovers fail solely on a missing field).
        ObjectNode summaryProp = objectMapper.createObjectNode();
        summaryProp.put("type", "string");
        summaryProp.put("description",
                "Optional one-line summary of the issue for the human "
                        + "agent. If omitted, the runtime derives a safe "
                        + "fallback summary from session state.");
        props.set("summary", summaryProp);
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

            // Sprint 31 — Option β alternate_candidate_use_cases projection
            // slot. Soft signal carrying the UCs the intake router considered
            // plausible for the session's topic-subject family when
            // RoutingResult.AMBIGUOUS fired at session creation, minus the
            // currently active UC. Empty array signals either (a) the intake
            // routed deterministically to a single UC (no alternates
            // considered) or (b) the active UC is the only surviving
            // candidate after filtering. The slot is always present for
            // projection-shape stability (§N0 nullable-field convention).
            // The runtime does NOT branch on this value; the LLM owns
            // whether to act on it.
            ArrayNode alternateCandidateUcsNode = objectMapper.createArrayNode();
            String activeUcForAlternate = session.getActiveUseCase();
            if (session.getIntakeAmbiguousCandidates() != null) {
                for (String uc : session.getIntakeAmbiguousCandidates()) {
                    if (uc != null && !uc.isBlank() && !uc.equals(activeUcForAlternate)) {
                        alternateCandidateUcsNode.add(uc);
                    }
                }
            }
            projection.set("alternate_candidate_use_cases", alternateCandidateUcsNode);

            // Sprint 33 — discover_disambiguation_signals projection slot.
            // Soft signal carrying observable evidence that the user's
            // DISCOVER session is in a state where multiple UCs are
            // plausibly responsive: the listing is in a not-visible
            // state (REMOVED / SUSPENDED / EXPIRED) AND the form
            // topic_subject maps to more than one candidate UC in
            // {@link UseCaseRegistryService#getCandidateUcsForTopic}.
            // The slot is observable evidence the LLM MAY use to inform
            // classify_use_case (e.g. to ask one clarifying question
            // before committing); the runtime does NOT enforce or branch
            // on the slot value. The slot is always present for
            // projection-shape stability (§N0 nullable-field convention);
            // empty / null sub-fields are the common case (most sessions
            // are unambiguous).
            projection.set("discover_disambiguation_signals",
                    buildDiscoverDisambiguationSignalsNode(session));

            // Sprint 41 — prior_use_case_carry projection slot per Sprint 37
            // freeze decision (i) §10.4. Surfaces continuity state as soft
            // signal when the session has experienced a prior UC switch
            // within the aging window (default 4 turns). The slot carries
            // (a) the prior active UC, (b) the prior Skill name (resolved
            // via SkillRegistry.select), (c) up to 3 most recent citation
            // source_ids from the prior UC's turns, and (d) the aging
            // window constant for LLM diagnostic visibility. The slot is
            // null when no prior UC switch has occurred OR when the
            // aging-out window has elapsed. Construction is REGISTRY-DRIVEN:
            // single aging constant + single citation cap; NO per-UC
            // variation in shape per §1.7. LLM-owned read per §1.3: the
            // LLM decides whether to surface continuity, ask, or ignore.
            JsonNode priorUseCaseCarryNode =
                    buildPriorUseCaseCarryNode(session, conversationHistory);
            projection.set("prior_use_case_carry", priorUseCaseCarryNode);

            // Sprint 10 §L2 — minimal projected issue-state. Surfaces the
            // runtime reroute outcome (previous_active_use_case, drift_type,
            // current_task_type, primary_entity, issue_status_summary) so
            // the LLM, trace UI, and eval contract validators can see the
            // runtime's reroute reasoning. Populated by
            // {@code ControlKernel.applyRerouteDecision} via transient
            // {@link BotSession} slots; absent / null values are emitted
            // as JSON {@code null} to keep the projection shape stable.
            String previousUc = session.getPreviousActiveUseCase();
            if (previousUc != null && !previousUc.isBlank()) {
                projection.put("previous_active_use_case", previousUc);
            } else {
                projection.putNull("previous_active_use_case");
            }
            String driftType = session.getDriftType();
            if (driftType != null && !driftType.isBlank()) {
                projection.put("drift_type", driftType);
            } else {
                projection.putNull("drift_type");
            }
            String taskType = session.getCurrentTaskType();
            if (taskType != null && !taskType.isBlank()) {
                projection.put("current_task_type", taskType);
            } else {
                projection.putNull("current_task_type");
            }
            String entityType = session.getPrimaryEntityType();
            String entityValue = session.getPrimaryEntityValue();
            if ((entityType != null && !entityType.isBlank())
                    || (entityValue != null && !entityValue.isBlank())) {
                ObjectNode primaryEntityNode = objectMapper.createObjectNode();
                primaryEntityNode.put("entity_type", entityType);
                if ("listing".equals(entityType) && entityValue != null && !entityValue.isBlank()) {
                    primaryEntityNode.put("ad_id", entityValue);
                } else if (entityValue != null && !entityValue.isBlank()) {
                    primaryEntityNode.put("entity_value", entityValue);
                }
                projection.set("primary_entity", primaryEntityNode);
            } else {
                projection.putNull("primary_entity");
            }
            String issueStatus = session.getIssueStatusSummary();
            if (issueStatus != null && !issueStatus.isBlank()) {
                projection.put("issue_status_summary", issueStatus);
            } else {
                // Default to "open" so downstream readers see a stable
                // string rather than absent / null. Closed sessions
                // would be in CLOSE phase and not invoke this builder
                // for further turns.
                projection.put("issue_status_summary", "open");
            }

            // Sprint 11 §M0 — task_status surfaces the progressive
            // resolve checkpoint (in_progress / asked_for_slot /
            // answered_subtask / ready_to_confirm / escalate). Default
            // is in_progress so the projection shape is stable across
            // turns even before the first ResolveDisposition has fired.
            String taskStatus = session.getTaskStatus();
            if (taskStatus != null && !taskStatus.isBlank()) {
                projection.put("task_status", taskStatus);
            } else {
                projection.put("task_status", "in_progress");
            }

            // Sprint 11 §M0 — optional pointer to where the
            // primary_entity originated (form_context.ad_id vs
            // user_message.ad_id). null when no entity is in scope.
            String lastEntityRef = session.getLastEntityContextRef();
            if (lastEntityRef != null && !lastEntityRef.isBlank()) {
                projection.put("last_entity_context_ref", lastEntityRef);
            } else {
                projection.putNull("last_entity_context_ref");
            }

            // Sprint 12 §N0 — runtime alignment observability. Each
            // turn's projection / persisted bot_turns row carries the
            // canonical reroute + progressive-resolve signals so a
            // reviewer reading any single turn can audit
            // (a) why the bot stayed in current UC, (b) why it
            // soft-shifted, (c) why it risk-shifted, (d) why it stayed
            // RESOLVE instead of CONFIRM, (e) why record_outcome(resolve)
            // was allowed or rejected. All additions are
            // backward-compatible JSON fields; existing keys are
            // preserved verbatim above.
            putNullableString(projection, "predicted_use_case",
                    session.getPredictedUseCase());
            putNullableString(projection, "intent_relation",
                    session.getIntentRelation());
            putNullableString(projection, "reroute_action",
                    session.getRerouteAction());
            putNullableString(projection, "phase_transition_reason",
                    session.getPhaseTransitionReason());
            putNullableString(projection, "resolve_disposition",
                    session.getResolveDisposition());
            putNullableString(projection, "record_outcome_guard_result",
                    session.getRecordOutcomeGuardResult());

            // Terminal evidence: deterministic facts about whether a
            // record_outcome dispatch landed during the most recent run
            // loop. Always emitted with stable shape so the trace UI /
            // contract validators see a predictable JSON object.
            ObjectNode terminalEvidence = objectMapper.createObjectNode();
            Boolean attempted = session.getRecordOutcomeAttempted();
            Boolean succeeded = session.getRecordOutcomeSucceeded();
            if (attempted != null) {
                terminalEvidence.put("record_outcome_attempted", attempted);
            } else {
                terminalEvidence.putNull("record_outcome_attempted");
            }
            if (succeeded != null) {
                terminalEvidence.put("record_outcome_succeeded", succeeded);
            } else {
                terminalEvidence.putNull("record_outcome_succeeded");
            }
            terminalEvidence.put("record_outcome_success",
                    Boolean.TRUE.equals(succeeded));
            projection.set("terminal_evidence", terminalEvidence);

            // drift_history / task_history — reconstructed from the
            // conversation_history's projected_context entries so a
            // reviewer reading a single turn sees the same drift /
            // task trajectory the runtime saw across turns. Cheap: capped
            // at the same last-10-turns window already used for
            // conversation_history above.
            ArrayNode driftHistoryNode = objectMapper.createArrayNode();
            ArrayNode taskHistoryNode = objectMapper.createArrayNode();
            buildDriftAndTaskHistory(conversationHistory, driftHistoryNode,
                    taskHistoryNode);
            projection.set("drift_history", driftHistoryNode);
            projection.set("task_history", taskHistoryNode);

            // Budget state
            ObjectNode budgetNode = objectMapper.createObjectNode();
            budgetNode.put("total_bot_turns", session.getTotalBotTurns());
            budgetNode.put("max_bot_turns", getMaxBotTurns(session));
            budgetNode.put("clarification_count", session.getClarificationCount());
            budgetNode.put("max_clarification", controlPolicy.getMaxClarificationRounds());
            budgetNode.put("faq_miss_count", session.getFaqMissCount());
            budgetNode.put("max_faq_miss", controlPolicy.getMaxFaqMiss());
            projection.set("budget_state", budgetNode);

            // Tool schemas — Skill-registry-driven (M2-correct). When the
            // active (phase, UC) maps to a Skill, the Skill's
            // {@code tools_required} is the projection surface (single source
            // of truth shared with {@code PhasePlan.allowedTools()} and the
            // ToolDispatcher whitelist). When no Skill maps the tuple
            // (e.g. RESOLVE with no active UC, or a future phase with no
            // Skill yet), fall back to the pre-M2 UC-driven palette via
            // {@code ToolPolicyEnforcer} so legacy / unmapped cases keep
            // their previous behaviour. The run-loop {@code build(...)}
            // path's {@code :allowedTools} overwrite remains as defense-
            // in-depth; for mapped Skills it now writes the same set.
            ArrayNode toolSchemasNode = objectMapper.createArrayNode();
            for (String toolName : resolveProjectedToolNames(session, activeUc)) {
                ObjectNode schema = toolSchemas.get(toolName);
                if (schema != null) {
                    toolSchemasNode.add(schema.deepCopy());
                } else {
                    log.warn("No static tool schema registered for projected tool '{}' (phase={}, uc={})",
                            toolName, session.getCurrentPhase(), activeUc);
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
        return build(session, history, plan, userMessage, accumulatedToolResults,
                Collections.emptyList());
    }

    /**
     * Sprint 20 Track B overload — same as
     * {@link #build(BotSession, List, PhasePlan, String, Map)} but also emits
     * an {@code already_called} projection slot derived from
     * {@code priorToolEvents}.
     *
     * <p>The slot is an array of {@code {tool, arguments_hash, at_step}}
     * entries describing every successful tool dispatch that has already
     * landed in the current {@code AgentRunLoop.run(...)} invocation. The
     * slot is observability-only: the LLM owns whether to re-emit. The
     * runtime does <strong>not</strong> short-circuit dispatch on this slot
     * (an idempotent-read short-circuit is a separate concern tracked under
     * {@code R-idempotent-read-tool-short-circuit}; see Sprint 19 §4.2 and
     * Sprint 20 objective §"Defer (do not bundle in Track B)").
     *
     * <p>Behaviour:
     * <ul>
     *   <li>Empty array {@code []} when {@code priorToolEvents} is null,
     *       empty, or contains only unsuccessful events. The slot is always
     *       present in the projection for shape stability (§N0
     *       nullable-field convention).</li>
     *   <li>For each {@link ToolEvent} where {@code success == true}, the
     *       slot carries a single entry. Failed-by-plan rejections and
     *       intake-guard rejections are excluded because they were never
     *       dispatched — they were not "already called".</li>
     *   <li>Argument hash is computed by canonical-JSON-serialising the
     *       event's {@code arguments} map (lexicographic key order) and
     *       taking the first 16 hex chars of the SHA-256. Two events with
     *       the same tool + same arguments hash to the same value
     *       regardless of insertion order.</li>
     * </ul>
     *
     * @param session                  current bot session
     * @param history                  prior turns in the session
     * @param plan                     the phase plan from PhaseEvaluator.plan()
     * @param userMessage              the current user message
     * @param accumulatedToolResults   map of tool_name -> result.data accumulated
     *                                 across prior loop iterations (may be null)
     * @param priorToolEvents          ordered list of {@link ToolEvent}s that
     *                                 have already landed in the current
     *                                 {@code AgentRunLoop.run(...)} invocation,
     *                                 across all prior steps (may be null or empty)
     * @return JSON string representing the full plan-aware projection plus
     *         the {@code already_called} slot
     */
    public String build(BotSession session,
                        List<BotTurn> history,
                        PhasePlan plan,
                        String userMessage,
                        Map<String, Object> accumulatedToolResults,
                        List<ToolEvent> priorToolEvents) {
        // Delegate to legacy path for the bulk of the projection. We pass
        // null knowledgeHits because in the run-loop world, knowledge results
        // arrive via accumulatedToolResults (under search_knowledge), not as
        // a pre-loaded slot.
        String baseJson = buildProjection(session, history, null, userMessage);

        try {
            ObjectNode projection = (ObjectNode) objectMapper.readTree(baseJson);

            // Track B always emits an already_called array (possibly empty)
            // for projection shape stability. Done first so the slot is
            // present even when there is no PhasePlan / accumulated results.
            projection.set("already_called", buildAlreadyCalledNode(priorToolEvents));

            if (plan == null && (accumulatedToolResults == null || accumulatedToolResults.isEmpty())) {
                return objectMapper.writeValueAsString(projection);
            }

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

                // Sprint 43 (S-Eval-2, NEW Milestone M3-Eval): render the
                // active Skill's `critical_steps[].desc` slice as an LLM-
                // visible projection slot, immediately after the procedure
                // narrative (which is folded into `system_instruction` via
                // PhaseEvaluator.plan() — see PhaseEvaluator.java:459).
                // Empty list → no `critical_steps` key (parity with M2
                // envelope behaviour; existing prompt-composition golden
                // tests stay green). Each rendered entry carries both
                // `id` and `desc` so S-Eval-3 authors can cross-reference
                // steps from one `desc` to another by stable id.
                // Per contract §8 (S-Eval-2 stanza): no semantic hardcode.
                // Rendering surfaces the soft narrative to the LLM (§1.3
                // LLM-owned: LLM owns whether to act on it).
                if (skillRegistry != null && plan.useCase() != null) {
                    skillRegistry.select(plan.phase(), plan.useCase()).ifPresent(skill -> {
                        if (!skill.criticalSteps().isEmpty()) {
                            ArrayNode stepsNode = objectMapper.createArrayNode();
                            for (var step : skill.criticalSteps()) {
                                ObjectNode stepNode = objectMapper.createObjectNode();
                                stepNode.put("id", step.id());
                                stepNode.put("desc", step.desc());
                                stepsNode.add(stepNode);
                            }
                            planNode.set("critical_steps", stepsNode);
                        }
                    });
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
     * Sprint 33 — build the {@code discover_disambiguation_signals}
     * projection slot. Returns an {@link ObjectNode} carrying three
     * fields, always present (null / false / empty when no signal
     * fires) for projection-shape stability:
     *
     * <ul>
     *   <li>{@code ad_status_observed} — the listing's status string
     *       when {@code session.listingContext.status} is one of
     *       {@code REMOVED / SUSPENDED / EXPIRED} (states where the
     *       listing is not visible to the user); {@code null}
     *       otherwise.</li>
     *   <li>{@code topic_subject_carries_multiple_candidate_ucs} —
     *       {@code true} iff the form's {@code topic_subject} maps
     *       to more than one UC in
     *       {@link UseCaseRegistryService#getCandidateUcsForTopic}.</li>
     *   <li>{@code candidate_ucs_for_topic} — the candidate UC list
     *       for that topic, or empty when the topic is null or
     *       single-candidate.</li>
     * </ul>
     *
     * <p>The slot is observable evidence the LLM may consume to inform
     * DISCOVER classification; the runtime does NOT branch on the
     * slot. A future Java decision-path branch on this slot would be
     * a §1.7 forbidden hardcode and would fail the companion
     * {@code AgentRunLoopDiscoverDisambiguationNonEnforcementIntegrationTest}
     * parameterised invariance bars.
     */
    private ObjectNode buildDiscoverDisambiguationSignalsNode(BotSession session) {
        ObjectNode node = objectMapper.createObjectNode();

        String adStatus = extractListingStatus(session);
        if (adStatus != null
                && ("REMOVED".equals(adStatus)
                        || "SUSPENDED".equals(adStatus)
                        || "EXPIRED".equals(adStatus))) {
            node.put("ad_status_observed", adStatus);
        } else {
            node.putNull("ad_status_observed");
        }

        ArrayNode candidatesNode = objectMapper.createArrayNode();
        String topic = session.getFormTopicSubject();
        boolean multiCandidate = false;
        if (topic != null && !topic.isBlank()) {
            List<String> candidates = useCaseRegistry.getCandidateUcsForTopic(topic);
            if (candidates != null && candidates.size() > 1) {
                multiCandidate = true;
                for (String uc : candidates) {
                    if (uc != null && !uc.isBlank()) {
                        candidatesNode.add(uc);
                    }
                }
            }
        }
        node.put("topic_subject_carries_multiple_candidate_ucs", multiCandidate);
        node.set("candidate_ucs_for_topic", candidatesNode);

        return node;
    }

    /**
     * Sprint 33 — read {@code session.listingContext.status} from the
     * jsonb string when present. Returns {@code null} when the
     * listing context is absent, unparseable, or has no
     * {@code status} text field.
     */
    private String extractListingStatus(BotSession session) {
        String raw = session.getListingContext();
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            com.fasterxml.jackson.databind.JsonNode tree = objectMapper.readTree(raw);
            if (tree == null || !tree.isObject()) {
                return null;
            }
            com.fasterxml.jackson.databind.JsonNode statusNode = tree.get("status");
            if (statusNode == null || !statusNode.isTextual()) {
                return null;
            }
            String text = statusNode.asText();
            return (text == null || text.isBlank()) ? null : text;
        } catch (Exception ex) {
            log.debug("Sprint 33: listing_context parse failed for session {}: {}",
                    session.getSessionId(), ex.getMessage());
            return null;
        }
    }

    /**
     * Sprint 41 — build the {@code prior_use_case_carry} projection slot
     * per design doc §10.4. The slot surfaces continuity state when the
     * session has experienced a prior UC switch within the aging window
     * (default {@link #PRIOR_USE_CASE_CARRY_AGING_TURNS} turns). It carries:
     *
     * <ul>
     *   <li>{@code prior_active_use_case} — the UC immediately preceding
     *       the current active UC.</li>
     *   <li>{@code prior_skill_name} — the Skill resolved via
     *       {@link SkillRegistry#select(String, String)} for the prior
     *       turn's {@code phase_after} + prior UC.</li>
     *   <li>{@code prior_citations} — up to
     *       {@link #PRIOR_USE_CASE_CARRY_CITATION_CAP} most recent
     *       citation source_ids from turns where the active UC matched
     *       the prior UC.</li>
     *   <li>{@code ages_out_after_turns} — the single integer aging
     *       window constant; diagnostic visibility for the LLM.</li>
     * </ul>
     *
     * <p>Returns {@link NullNode#getInstance()} when (a) the conversation
     * history is empty or absent, (b) the current active UC is null
     * (DISCOVER pre-classification), (c) no prior turn carries a different
     * active UC (single-UC session), OR (d) the most recent UC switch is
     * more than {@link #PRIOR_USE_CASE_CARRY_AGING_TURNS} turns ago
     * (aged out).
     *
     * <p>§1.7 boundary: construction is REGISTRY-DRIVEN. The aging window
     * is a single integer constant; the citation cap is a single integer
     * constant; the {@code prior_skill_name} is resolved via the registry
     * lookup. NO per-UC variation in slot shape.
     *
     * <p>§1.3 boundary: the slot is a soft signal. The LLM reads the slot
     * value on the next turn and judges whether to surface continuity,
     * ask, or ignore. The runtime does NOT enforce action on the slot.
     */
    private JsonNode buildPriorUseCaseCarryNode(BotSession session,
                                                 List<BotTurn> conversationHistory) {
        if (session == null) {
            return NullNode.getInstance();
        }
        String currentActiveUc = session.getActiveUseCase();
        if (currentActiveUc == null || currentActiveUc.isBlank()) {
            return NullNode.getInstance();
        }
        if (conversationHistory == null || conversationHistory.isEmpty()) {
            return NullNode.getInstance();
        }

        // Walk history in reverse to find the most recent turn whose
        // active_use_case is non-null AND differs from the current active
        // UC. That turn marks the prior UC switch boundary.
        BotTurn priorSwitchTurn = null;
        for (int i = conversationHistory.size() - 1; i >= 0; i--) {
            BotTurn t = conversationHistory.get(i);
            if (t == null) continue;
            String tUc = t.getActiveUseCase();
            if (tUc != null && !tUc.isBlank() && !tUc.equals(currentActiveUc)) {
                priorSwitchTurn = t;
                break;
            }
        }
        if (priorSwitchTurn == null) {
            return NullNode.getInstance();
        }

        // Aging check: if the most recent UC switch is more than the
        // configured aging window away from the current (in-flight) turn,
        // drop the slot. The "current turn index" is the index this turn
        // will receive once persisted = (last persisted turn_index + 1).
        Integer priorTurnIdx = priorSwitchTurn.getTurnIndex();
        BotTurn lastTurn = conversationHistory.get(conversationHistory.size() - 1);
        int currentTurnIdx = (lastTurn != null && lastTurn.getTurnIndex() != null)
                ? lastTurn.getTurnIndex() + 1
                : conversationHistory.size();
        if (priorTurnIdx == null) {
            return NullNode.getInstance();
        }
        if (currentTurnIdx - priorTurnIdx > PRIOR_USE_CASE_CARRY_AGING_TURNS) {
            return NullNode.getInstance();
        }

        String priorUc = priorSwitchTurn.getActiveUseCase();

        // Resolve prior_skill_name via SkillRegistry.select on the prior
        // turn's phase_after + prior UC. Null when registry select misses
        // (e.g., legacy turn without a Skill mapping).
        String priorSkillName = null;
        if (skillRegistry != null) {
            String priorPhase = priorSwitchTurn.getPhaseAfter();
            if (priorPhase != null && !priorPhase.isBlank()) {
                priorSkillName = skillRegistry.select(priorPhase, priorUc)
                        .map(Skill::name)
                        .orElse(null);
            }
        }

        // Collect citation source_ids from turns where active_use_case
        // matched the prior UC, cap to the most recent
        // PRIOR_USE_CASE_CARRY_CITATION_CAP. Walk in reverse to gather
        // most-recent first.
        ArrayNode citationsNode = objectMapper.createArrayNode();
        int collected = 0;
        for (int i = conversationHistory.size() - 1; i >= 0
                && collected < PRIOR_USE_CASE_CARRY_CITATION_CAP; i--) {
            BotTurn t = conversationHistory.get(i);
            if (t == null) continue;
            String tUc = t.getActiveUseCase();
            if (tUc == null || !tUc.equals(priorUc)) continue;
            String[] sids = t.getSourceIds();
            if (sids == null) continue;
            for (String sid : sids) {
                if (sid == null || sid.isBlank()) continue;
                ObjectNode cite = objectMapper.createObjectNode();
                cite.put("source_id", sid);
                cite.put("from_use_case", priorUc);
                if (t.getTurnIndex() != null) {
                    cite.put("turn_index", t.getTurnIndex());
                } else {
                    cite.putNull("turn_index");
                }
                citationsNode.add(cite);
                collected++;
                if (collected >= PRIOR_USE_CASE_CARRY_CITATION_CAP) break;
            }
        }

        ObjectNode node = objectMapper.createObjectNode();
        node.set("prior_citations", citationsNode);
        node.put("prior_active_use_case", priorUc);
        if (priorSkillName != null) {
            node.put("prior_skill_name", priorSkillName);
        } else {
            node.putNull("prior_skill_name");
        }
        node.put("ages_out_after_turns", PRIOR_USE_CASE_CARRY_AGING_TURNS);
        return node;
    }

    /**
     * Sprint 20 Track B — build the {@code already_called} projection
     * slot from a list of prior {@link ToolEvent}s within the current
     * {@code AgentRunLoop.run(...)} invocation. Returns an
     * {@link ArrayNode} of {@code {tool, arguments_hash, at_step}}
     * entries, in the same order the events were dispatched.
     *
     * <p>Only events with {@code success == true} produce an entry; plan-
     * rejected and intake-guard-rejected events were not dispatched and
     * are excluded. {@code null} or empty input produces an empty array
     * (kept on the projection for shape stability, per the §N0 nullable-
     * field convention).
     *
     * <p>If argument-hashing fails for a specific event (very unusual —
     * the Jackson canonical-JSON serialiser would have to throw, or the
     * SHA-256 algorithm would have to be unavailable), the entry uses the
     * sentinel {@code "hash_error"} so the slot remains parseable.
     */
    private ArrayNode buildAlreadyCalledNode(List<ToolEvent> priorToolEvents) {
        ArrayNode alreadyCalled = objectMapper.createArrayNode();
        if (priorToolEvents == null || priorToolEvents.isEmpty()) {
            return alreadyCalled;
        }
        for (ToolEvent evt : priorToolEvents) {
            if (evt == null || !evt.success()) {
                continue;
            }
            ObjectNode entry = objectMapper.createObjectNode();
            entry.put("tool", evt.toolName());
            entry.put("arguments_hash", canonicalArgumentsHash(evt.arguments()));
            entry.put("at_step", evt.stepIndex());
            alreadyCalled.add(entry);
        }
        return alreadyCalled;
    }

    /**
     * Sprint 20 Track B — canonical, order-insensitive 16-hex-char
     * SHA-256 of a tool-call {@code arguments} map. The map is serialised
     * via a Jackson {@link ObjectMapper} configured with
     * {@link SerializationFeature#ORDER_MAP_ENTRIES_BY_KEYS}, then SHA-256
     * is applied to the UTF-8 bytes and the result is truncated to the
     * first 16 hex chars for projection compactness.
     *
     * <p>Two ToolCalls with identical content but differently-ordered map
     * keys produce the same hash. The truncation length (16 hex chars =
     * 64 bits) is comfortably collision-free for the per-run scale of a
     * single {@code AgentRunLoop.run(...)} invocation (bounded by
     * {@code plan.maxToolSteps()}).
     */
    String canonicalArgumentsHash(Map<String, Object> arguments) {
        try {
            String canonical = argumentsHashMapper.writeValueAsString(
                    arguments == null ? Collections.emptyMap() : arguments);
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(canonical.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(16);
            for (int i = 0; i < 8 && i < digest.length; i++) {
                sb.append(String.format("%02x", digest[i] & 0xff));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException | RuntimeException ex) {
            log.warn("Sprint 20 Track B: arguments_hash computation failed for tool args: {}",
                    ex.getMessage());
            return "hash_error";
        } catch (Exception ex) {
            log.warn("Sprint 20 Track B: arguments_hash serialisation failed for tool args: {}",
                    ex.getMessage());
            return "hash_error";
        }
    }

    /**
     * Sprint 12 §N0 — write a nullable string field to the projection so
     * the JSON shape is stable across turns (absent values become JSON
     * {@code null} rather than missing keys). Keeps the trace contract
     * predictable for downstream readers.
     */
    private static void putNullableString(ObjectNode node, String key, String value) {
        if (value == null || value.isBlank()) {
            node.putNull(key);
        } else {
            node.put(key, value);
        }
    }

    /**
     * Sprint 12 §N0 — reconstruct {@code drift_history} and
     * {@code task_history} arrays from prior turns' persisted projection
     * JSON. Each entry captures one prior turn's
     * {@code drift_type / intent_relation / reroute_action} and
     * {@code current_task_type / task_status / resolve_disposition}
     * trajectory so a reviewer reading any single turn can audit the
     * sequence of decisions that led here.
     *
     * <p>The window matches the conversation-history window emitted
     * above (last 10 turns). Skips silently when a prior turn did not
     * persist a projected_context (e.g. legacy rows from before
     * Sprint 10). Uses {@code turn_index} as the sort key when present.
     */
    private void buildDriftAndTaskHistory(List<BotTurn> conversationHistory,
                                          ArrayNode driftHistoryNode,
                                          ArrayNode taskHistoryNode) {
        if (conversationHistory == null || conversationHistory.isEmpty()) {
            return;
        }
        int startIdx = Math.max(0, conversationHistory.size() - 10);
        for (int i = startIdx; i < conversationHistory.size(); i++) {
            BotTurn turn = conversationHistory.get(i);
            if (turn == null) continue;
            String projected = turn.getProjectedContext();
            if (projected == null || projected.isBlank()) continue;
            try {
                com.fasterxml.jackson.databind.JsonNode root = objectMapper.readTree(projected);
                if (root == null || !root.isObject()) continue;

                ObjectNode driftEntry = objectMapper.createObjectNode();
                driftEntry.put("turn_index", turn.getTurnIndex());
                copyTextField(root, driftEntry, "drift_type");
                copyTextField(root, driftEntry, "intent_relation");
                copyTextField(root, driftEntry, "reroute_action");
                copyTextField(root, driftEntry, "predicted_use_case");
                copyTextField(root, driftEntry, "active_use_case");
                copyTextField(root, driftEntry, "previous_active_use_case");
                copyTextField(root, driftEntry, "phase_transition_reason");
                driftHistoryNode.add(driftEntry);

                ObjectNode taskEntry = objectMapper.createObjectNode();
                taskEntry.put("turn_index", turn.getTurnIndex());
                copyTextField(root, taskEntry, "current_task_type");
                copyTextField(root, taskEntry, "task_status");
                copyTextField(root, taskEntry, "resolve_disposition");
                copyTextField(root, taskEntry, "record_outcome_guard_result");
                copyTextField(root, taskEntry, "issue_status_summary");
                taskHistoryNode.add(taskEntry);
            } catch (Exception ex) {
                log.debug("drift/task history skip for turn_index={}: {}",
                        turn.getTurnIndex(), ex.getMessage());
            }
        }
    }

    private static void copyTextField(com.fasterxml.jackson.databind.JsonNode src,
                                      ObjectNode dst, String key) {
        com.fasterxml.jackson.databind.JsonNode v = src.get(key);
        if (v == null || v.isNull()) {
            dst.putNull(key);
        } else if (v.isTextual()) {
            dst.put(key, v.asText());
        } else {
            dst.put(key, v.toString());
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
     * Resolve the projected {@code tool_schemas} surface for a session.
     *
     * <p>Registry/Skill-driven (M2-correct): when {@link SkillRegistry#select}
     * matches a Skill for the session's {@code (currentPhase, activeUseCase)}
     * tuple, the Skill's {@code toolsRequired} list is the projection
     * surface — the same single source of truth that
     * {@code PhaseEvaluator.plan(...)} composes into
     * {@code PhasePlan.allowedTools()} and that
     * {@code ToolDispatcher.validateAgainstPlan} enforces. When no Skill
     * maps the tuple (legacy / unmapped phase, or RESOLVE with no committed
     * UC yet), fall back to the pre-M2 UC-driven palette via
     * {@code ToolPolicyEnforcer.getVisibleToolsForUc(activeUseCase)} so the
     * legacy fallback path keeps its previous behaviour.
     *
     * <p>§1.7 boundary: no per-UC if-else, no keyword/regex/enum branch.
     * The path is registry/Skill-driven; the UC-driven fallback is a
     * single defensive call that mirrors pre-M2 behaviour for any tuple
     * the registry does not cover. The Skill's {@code toolsRequired} list
     * is itself registry data (loaded from
     * {@code server/src/main/resources/skills/*.yaml}).
     */
    private List<String> resolveProjectedToolNames(BotSession session, String activeUseCase) {
        if (session == null) {
            return Collections.emptyList();
        }
        String phase = session.getCurrentPhase();
        if (skillRegistry != null) {
            Optional<Skill> selected = skillRegistry.select(phase, activeUseCase);
            if (selected.isPresent()) {
                List<String> toolsRequired = selected.get().toolsRequired();
                return toolsRequired == null ? Collections.emptyList() : toolsRequired;
            }
        }
        if (activeUseCase == null) {
            return Collections.emptyList();
        }
        return toolPolicyEnforcer.getVisibleToolsForUc(activeUseCase);
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
