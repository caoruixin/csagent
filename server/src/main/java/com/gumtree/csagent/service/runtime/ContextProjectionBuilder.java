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

    /** Canonical DISCOVER phase label (mirrors AgentRunLoopImpl.DISCOVER_PHASE). */
    private static final String DISCOVER_PHASE = "DISCOVER";

    /** R4.a — explicit listing-verification tool name (LookupListingTool). */
    private static final String LOOKUP_LISTING_TOOL = "lookup_listing_or_ad";

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

        // Sprint 103 / WS-6-A — mid-session re-route. Projected on the RESOLVE
        // / CONFIRM Skills that declare it in `tools_required`; DISCOVER keeps
        // `classify_use_case`. The description states what the tool does and
        // what the runtime will do with the result — it does not tell the LLM
        // when to use it, which is the §1.3 judgement.
        toolSchemas.put("propose_reroute", buildToolSchema(
                "propose_reroute",
                "Move this conversation to a different use case when the customer has raised a "
                        + "different need from the one the session is currently working on. The "
                        + "runtime moves session.active_use_case to the target and replans the "
                        + "same turn into that use case's RESOLVE skill, so the tools and "
                        + "procedure you get next are the target use case's. The proposal is "
                        + "declined (honoured=false, with a reason, not an error) if the target "
                        + "is already active or no skill serves it; you may then pick a "
                        + "different target or continue. See `reroute_target_use_cases` in the "
                        + "projection for the ids this accepts and what each one means.",
                buildProposeRerouteArgsSchema()));

        // Sprint 080 / R7 — no-side-effect intake-field accumulation tool. The
        // schema is registered here; whether it is projected to the LLM on a
        // given turn is gated by `plan.allowedTools()` (the intake Skill's
        // `tools_required`), exactly like every other tool schema.
        toolSchemas.put("update_intake_fields", buildToolSchema(
                "update_intake_fields",
                "Persist partial intake fields collected from the user so they "
                        + "survive across turns. Use this when you have identified one "
                        + "or more intake fields the user has provided but you do not "
                        + "yet have a complete set to call request_handover. Does NOT "
                        + "trigger handover. See `required_intake_fields_for_active_uc` "
                        + "in the projection for the active UC's required-fields list.",
                buildUpdateIntakeFieldsArgsSchema()));

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

    /**
     * Sprint 080 / R7 — {@code update_intake_fields} arguments schema. A single
     * required {@code fields} property typed as a free-form
     * {@code string -> string} object (mirrors the R1.a {@code intake_fields}
     * slot). No per-UC field enumeration: which fields belong to which UC is
     * surfaced separately via {@code required_intake_fields_for_active_uc}.
     */
    private ObjectNode buildUpdateIntakeFieldsArgsSchema() {
        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "object");
        ObjectNode props = objectMapper.createObjectNode();
        ObjectNode fieldsProp = objectMapper.createObjectNode();
        fieldsProp.put("type", "object");
        ObjectNode additional = objectMapper.createObjectNode();
        additional.put("type", "string");
        fieldsProp.set("additionalProperties", additional);
        props.set("fields", fieldsProp);
        schema.set("properties", props);
        ArrayNode required = objectMapper.createArrayNode();
        required.add("fields");
        schema.set("required", required);
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
        reasonProp.put("description",
                "Pick the canonical reason per the system-prompt decision tree. "
                        + "Reserve `user_requested` for an ACTUAL user request for a "
                        + "human. For a handover YOU initiate on an in-scope issue you "
                        + "could not resolve (you tried / exhausted your grounded "
                        + "resolution, the issue is unresolved, the user did not ask "
                        + "for a human, and no higher-priority reason applies), use "
                        + "`agent_unable_to_resolve` — not `user_requested`, "
                        + "`service_degraded`, `out_of_scope`, or a budget reason. "
                        + "`agent_unable_to_resolve` is the lowest-priority reason and "
                        + "is never a generic catch-all.");
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
        // Canonical 24-value escalation_reason enum. Mirrors
        // eval_interactive/eval_interactive/case_spec/schema.py
        // (EscalationTrigger Literal). Keep these two lists in lockstep.
        // Grouped by source: user-driven, FAQ/clarification budget, intake-
        // complete (UC-G..K), policy/safety, system/guardrail, semantic
        // last-resort.
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
        // Semantic last-resort (Sprint 096 / S-Auto-44, M-Auto-9 WP1):
        // bot-initiated, in-scope, exhausted-resolution, unresolved handover.
        // Lowest-priority reason; LLM-selected only, never auto-stamped.
        enumValues.add("agent_unable_to_resolve");
        reasonProp.set("enum", enumValues);
        props.set("escalation_reason", reasonProp);
        // R1.a #1 — declare the intake_fields slot the validator
        // (SkillGuardrailDispatcher) + AgentRunLoopImpl.persistInlineIntakeFields
        // already expect. Free-form string->string map; intentionally NOT a
        // per-UC property matrix (§1.7) — the per-active-UC required key list is
        // surfaced separately via the required_intake_fields_for_active_uc
        // projection field. OPTIONAL (absent from the required[] array) so
        // non-intake UCs are unaffected; only intake UCs (UC-G/H/I/J/K) need it.
        ObjectNode intakeFieldsProp = objectMapper.createObjectNode();
        intakeFieldsProp.put("type", "object");
        intakeFieldsProp.put("description",
                "Required for intake UCs (UC-G / UC-H / UC-I / UC-J / UC-K) — "
                        + "see the `required_intake_fields_for_active_uc` "
                        + "projection field for the active UC's required-fields "
                        + "list. Free-form string-to-string map of the collected "
                        + "intake field values for this handover.");
        props.set("intake_fields", intakeFieldsProp);
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

    /**
     * Sprint 103 / WS-6-A — schema for the {@code propose_reroute} tool. The
     * {@code target_use_case} enum is built from
     * {@link UseCaseRegistryService#getAllUseCases()} rather than a literal
     * list, so registering a use case in {@code use-case-registry.yaml} is the
     * only place the accepted id set is declared.
     */
    private ObjectNode buildProposeRerouteArgsSchema() {
        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "object");
        ObjectNode props = objectMapper.createObjectNode();

        ObjectNode targetProp = objectMapper.createObjectNode();
        targetProp.put("type", "string");
        ArrayNode ucEnum = objectMapper.createArrayNode();
        useCaseRegistry.getAllUseCases().keySet().stream().sorted().forEach(ucEnum::add);
        targetProp.set("enum", ucEnum);
        props.set("target_use_case", targetProp);

        ObjectNode reasoningProp = objectMapper.createObjectNode();
        reasoningProp.put("type", "string");
        props.set("reasoning", reasoningProp);

        schema.set("properties", props);
        ArrayNode required = objectMapper.createArrayNode();
        required.add("target_use_case");
        required.add("reasoning");
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

            // WS-3 / D2 (2026-07-25) — frustration as a projected SOFT SIGNAL.
            //
            // The BRD's "the bot must escalate after ... customer indicates
            // frustration" rule is revised to "de-escalate and keep solving
            // first" (phase0_normative_freeze.md §0.6, deviation 2026-07-25).
            // The implementation of that revision is this slot: the runtime's
            // deterministic detector still runs, but its output is handed to
            // the LLM as an observation instead of being spent on a control
            // decision. Whether a frustrated turn should be handed over
            // depends on what the customer is actually asking for, which is a
            // semantic judgement the Constitution assigns to the LLM (§1.3);
            // encoding it as a keyword-driven trigger is what §1.5 / §1.7
            // forbid. The deterministic floor that remains is unchanged and
            // lives elsewhere: an explicit request for a human (ControlKernel
            // step 2.5 / DriftDetector) and the fraud / safety / GDPR /
            // payment hard shifts in risk-keywords.yaml.
            //
            // The slot is OMITTED entirely when no signal fires, so the common
            // path is byte-identical to the pre-WS-3 projection.
            if (EscalationReasonResolver.hasDistressSignal(userMessage)) {
                ObjectNode sentimentNode = objectMapper.createObjectNode();
                sentimentNode.put("frustration_detected", true);
                sentimentNode.put("binding", "advisory");
                sentimentNode.put("note",
                        "The customer sounds frustrated on this turn. This is an "
                                + "observation, not an instruction: frustration on its own is "
                                + "NOT a reason to hand over. Acknowledge it and keep solving "
                                + "if the underlying ask is still something you can explain or "
                                + "look up. You own this judgement.");
                projection.set("user_sentiment_signal", sentimentNode);
            }

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

            // R1.a #2 — per-active-UC required-intake-fields hint. Gives the LLM
            // a structured per-turn list of EXACTLY which field keys to populate
            // in the next request_handover.arguments.intake_fields call, sourced
            // from the SAME IntakeFieldsRegistry the validator uses (single
            // source of truth — no per-UC matrix replicated here, §1.7). Present
            // ONLY for intake UCs; for null / non-intake UCs the field is OMITTED
            // entirely (distinguish "not applicable" from "no fields required" —
            // never an empty list).
            if (IntakeFieldsRegistry.isIntakeUseCase(activeUc)) {
                ArrayNode requiredForUc = objectMapper.createArrayNode();
                for (String f : IntakeFieldsRegistry.requiredFieldsFor(activeUc)) {
                    requiredForUc.add(f);
                }
                projection.set("required_intake_fields_for_active_uc", requiredForUc);
            }

            // Sprint 7 §I0 — C5 candidate_use_cases projection. Surfaces the
            // routing-derived candidate UC list so the DISCOVER cue can act
            // on it deterministically. Empty array signals "no candidates yet"
            // (UNKNOWN topic + empty description), which the DISCOVER
            // systemInstruction reads as the trigger to gather evidence
            // before escalating with faq_miss_threshold_exceeded.
            //
            // Sprint 53 / M5 S4 — Skill-declared context-key gating
            // (#2). Per the Phase-A audit
            // (`docs/diagnostics/m5-s4-skill-declaration-audit.md` §3.E),
            // only `discover_triage` declares `candidate_use_cases` in
            // `required_context_keys` AND only DISCOVER actually reads
            // the slot (post-DISCOVER Skills work on a committed UC).
            // Gating reads `Skill.requiredContextKeys()`; for unmapped
            // (phase, UC) tuples the helper defaults to TRUE (emit) so
            // legacy/unmapped sessions keep pre-S4 behaviour. §1.7
            // boundary: no per-UC if-else; the gate is registry-data-
            // driven (the YAML declaration).
            if (skillRequiresContextKey(session, activeUc, "candidate_use_cases")) {
                ArrayNode candidateUcsNode = objectMapper.createArrayNode();
                if (session.getCandidateUseCases() != null) {
                    for (String uc : session.getCandidateUseCases()) {
                        if (uc != null && !uc.isBlank()) {
                            candidateUcsNode.add(uc);
                        }
                    }
                }
                projection.set("candidate_use_cases", candidateUcsNode);
            }

            // Sprint 86a / S-Auto-31 — candidate_use_cases_named projection
            // slot. BACKWARD-COMPATIBLE ADDITIVE companion to the bare-ID
            // candidate_use_cases slot above (which is UNCHANGED). The LLM
            // otherwise sees only opaque UC ids (["UC-A","UC-FP",...]) and
            // must recall from training what each id means; surfacing the
            // registry name alongside each id grounds the DISCOVER
            // classify_use_case choice (addresses UC-FP invisibility +
            // UC-G hallucination). Each entry is {id, name}; name falls back
            // to the id when the registry has no definition (unknown id).
            // Gated by its OWN required_context_keys declaration via the same
            // registry-data-driven helper as candidate_use_cases; for
            // unmapped (phase, UC) tuples the helper defaults to TRUE (emit).
            // §1.7 boundary: registry-driven lookup; no per-UC if-else, no
            // keyword/regex/enum branch, and the runtime does NOT branch on
            // the slot value.
            if (skillRequiresContextKey(session, activeUc, "candidate_use_cases_named")) {
                ArrayNode namedUcsNode = objectMapper.createArrayNode();
                if (session.getCandidateUseCases() != null) {
                    for (String uc : session.getCandidateUseCases()) {
                        if (uc == null || uc.isBlank()) {
                            continue;
                        }
                        ObjectNode entry = objectMapper.createObjectNode();
                        entry.put("id", uc);
                        UseCaseRegistryService.UseCaseDefinition def =
                                useCaseRegistry.getUseCase(uc);
                        entry.put("name", def != null ? def.name() : uc);
                        namedUcsNode.add(entry);
                    }
                }
                projection.set("candidate_use_cases_named", namedUcsNode);
            }

            // Sprint 31 — Option β alternate_candidate_use_cases projection
            // slot. Soft signal carrying the UCs the intake router considered
            // plausible for the session's topic-subject family when
            // RoutingResult.AMBIGUOUS fired at session creation, minus the
            // currently active UC. Empty array signals either (a) the intake
            // routed deterministically to a single UC (no alternates
            // considered) or (b) the active UC is the only surviving
            // candidate after filtering. The runtime does NOT branch on
            // this value; the LLM owns whether to act on it.
            //
            // Sprint 53 / M5 S4 — `soft_signal_via_projection` gating
            // (#5). Per the Phase-A audit
            // (`docs/diagnostics/m5-s4-skill-declaration-audit.md` §3.G),
            // only `discover_triage` declares this slot in
            // `state_inheritance.soft_signal_via_projection` AND only
            // DISCOVER procedure references it. Gating reads
            // `Skill.stateInheritance().softSignalViaProjection()`; for
            // unmapped (phase, UC) tuples the helper defaults to TRUE
            // (emit) — defensive pre-S4 behaviour preservation. The slot
            // is null/empty for most sessions even when emitted (only
            // populated when intake-router fired AMBIGUOUS), so the
            // shape-change risk is minimal. §1.7 boundary: registry/
            // Skill-driven.
            if (skillDeclaresSoftSignal(session, activeUc, "alternate_candidate_use_cases")) {
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
            }

            // Sprint 103 / WS-6-A — reroute_target_use_cases projection slot.
            // The argument domain of `propose_reroute`: every use case in the
            // registry as {id, name}, minus the one already active.
            //
            // Why this slot and not `alternate_candidate_use_cases` alone: that
            // slot is populated ONLY from `session.intakeAmbiguousCandidates`,
            // which `SessionManager` writes only on the `RoutingResult.AMBIGUOUS`
            // branch at session creation. A session that routed deterministically
            // — every strong-prior topic, every B2 phrase-bias hit — has it null,
            // so the slot emits `[]` on exactly the sessions where the intake
            // router was confident and the customer later changed the subject.
            // The slot's own documentation says as much ("mid-session shifts the
            // intake router did not anticipate may not appear in the slot").
            // Without this catalogue the LLM sees `propose_reroute`'s enum of
            // opaque ids with nothing saying what they mean, because
            // `candidate_use_cases_named` is gated to DISCOVER.
            //
            // This is fact, not persuasion: it states what the tool accepts and
            // what each id is called. It does not say when to re-route — that is
            // the §1.3 judgement. Construction is registry-driven; no per-UC
            // branch, no keyword, and the runtime does not read the slot back.
            if (skillDeclaresSoftSignal(session, activeUc, "reroute_target_use_cases")) {
                ArrayNode rerouteTargetsNode = objectMapper.createArrayNode();
                String activeUcForReroute = session.getActiveUseCase();
                useCaseRegistry.getAllUseCases().entrySet().stream()
                        .filter(e -> !e.getKey().equals(activeUcForReroute))
                        .sorted(Map.Entry.comparingByKey())
                        .forEach(e -> {
                            ObjectNode entry = objectMapper.createObjectNode();
                            entry.put("id", e.getKey());
                            entry.put("name", e.getValue() != null && e.getValue().name() != null
                                    ? e.getValue().name()
                                    : e.getKey());
                            rerouteTargetsNode.add(entry);
                        });
                projection.set("reroute_target_use_cases", rerouteTargetsNode);
            }

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
            // on the slot value.
            //
            // Sprint 53 / M5 S4 — `soft_signal_via_projection` gating
            // (#5). Per the Phase-A audit
            // (`docs/diagnostics/m5-s4-skill-declaration-audit.md` §3.H),
            // only `discover_triage` declares + reads. Same pattern as
            // `alternate_candidate_use_cases` above.
            if (skillDeclaresSoftSignal(session, activeUc, "discover_disambiguation_signals")) {
                projection.set("discover_disambiguation_signals",
                        buildDiscoverDisambiguationSignalsNode(session));
            }

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
            //
            // Sprint 53 / M5 S4 — `soft_signal_via_projection` gating
            // (#5). Per the Phase-A audit
            // (`docs/diagnostics/m5-s4-skill-declaration-audit.md` §3.I),
            // `resolve_faq_grounded_answer` + `resolve_intake_collect_and_handover`
            // declare + need this slot (UC-A↔UC-C / cross-UC continuity is
            // a RESOLVE-Skill concern); other Skills have
            // `previous_active_use_case` as a separate slot for drift
            // context and don't need the carry's prior_skill_name +
            // citation list. Defensive default = emit for unmapped
            // tuples.
            if (skillDeclaresSoftSignal(session, activeUc, "prior_use_case_carry")) {
                JsonNode priorUseCaseCarryNode =
                        buildPriorUseCaseCarryNode(session, conversationHistory);
                projection.set("prior_use_case_carry", priorUseCaseCarryNode);
            }

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

            // R2.a #4 — DISCOVER clarification budget soft signal. Surfaces the
            // now-live clarification counter (R2.a #3 wires the increment on the
            // AgentRunLoopImpl path) as observable state so the LLM can sequence
            // DISCOVER turns BEFORE the hard cap fires. Cardinality only — the
            // LLM still owns next-action (§1.3); no semantic hardcode. Emitted
            // only in DISCOVER (the only phase where clarification rounds are
            // counted); absent in every other phase.
            if (DISCOVER_PHASE.equalsIgnoreCase(session.getCurrentPhase())) {
                ObjectNode budgetsNode = objectMapper.createObjectNode();
                ObjectNode clarificationBudget = objectMapper.createObjectNode();
                clarificationBudget.put("used", session.getClarificationCount());
                clarificationBudget.put("max", controlPolicy.getMaxClarificationRounds());
                budgetsNode.set("clarification", clarificationBudget);
                projection.set("budgets", budgetsNode);
            }

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

            // P1 paraphrase-storm fix (2026-07-25) — the LLM's own
            // search_knowledge attempts THIS TURN, verbatim. `already_called`
            // above carries only an opaque `arguments_hash`, so the LLM could
            // not read back the queries it had already issued; the question
            // "is my new query materially different from what I already
            // searched?" was literally unanswerable from the projection, and
            // the model answered it optimistically every time (measured:
            // session f62ad6ce-… turn 1, ten searches, nine suppressed, all
            // reasoning "the prior search was about X, not Y"). This slot is
            // deterministic STATE, not an instruction: it reports what was
            // asked, whether the tool actually ran, and what came back.
            // §1.3 keeps the decision with the LLM; §1.4/§3.2-Q3 make it the
            // Runtime's job to surface the state that decision needs.
            projection.set("search_attempts_this_turn",
                    buildSearchAttemptsNode(priorToolEvents));
            projection.set("search_attempts_summary",
                    buildSearchAttemptsSummaryNode(priorToolEvents));

            // R4.a #6 + #7 — ad-context premise projection. Surfaces already-
            // observed runtime state (form_context.email/ad_id presence + the
            // lookup_listing_or_ad tool result) as a structured enum + struct,
            // so the LLM has observable premise state without being told what to
            // say (§1.3). Emitted on EVERY live-path turn (shape stability;
            // before the early-return below) — NOT a per-UC matrix. ZERO content
            // matching of user messages; derived entirely from runtime state.
            addAdContextPremiseProjection(projection, session, priorToolEvents);

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

                // Sprint 068 (S-Auto-13, A3) — paraphrase-storm echo. This is
                // the run-loop-path analogue of the legacy `knowledge_instruction`
                // snippet in buildProjection (which only fires when knowledgeHits
                // is pre-loaded — never the case in the loop, where knowledge
                // arrives via accumulated_tool_results). When a prior
                // search_knowledge has already landed a viable hit
                // (faq_miss=false) this turn, surface a soft anti-re-search
                // signal so the LLM drafts from the existing hits via
                // resolve_article instead of re-issuing a paraphrased search.
                // The LLM owns whether to re-search (§1.3 / §1.5 soft-signal-
                // first); the runtime does NOT block a re-search dispatch on
                // this slot. A fresh search remains warranted when the prior
                // result was faq_miss=true or the new query is materially
                // different.
                JsonNode priorSearch = toolResultsNode.get("search_knowledge");
                if (priorSearch != null && priorSearch.has("faq_miss")
                        && !priorSearch.path("faq_miss").asBoolean(true)) {
                    projection.put("prior_search_knowledge_viable_hit", true);
                    // P1 paraphrase-storm fix (2026-07-25) — REWRITTEN.
                    //
                    // The previous wording ended "...a fresh search_knowledge is
                    // only warranted if the prior result was faq_miss=true or
                    // your new query is materially different from what you
                    // already searched." That clause was measured to be the
                    // licence the model cited, verbatim, while issuing ten
                    // searches in one turn (session f62ad6ce-… step-by-step
                    // reasoning: "the prior search was about ad status, not
                    // visibility tips. A new search with a materially different
                    // query is warranted"). It granted an exemption whose
                    // precondition the model had no data to evaluate, because
                    // its own prior queries were not in the projection.
                    //
                    // The replacement removes the exemption clause and states
                    // the MECHANICAL CONSEQUENCE instead: while a viable hit
                    // stands, the runtime serves it back rather than retrieving
                    // again, and the attempt still costs a tool step. That is a
                    // true statement about how this runtime behaves (the A1 /
                    // A3 / cross-turn backstops), not an instruction and not a
                    // rule — the LLM still owns whether to search (§1.3). It is
                    // paired with search_attempts_this_turn, which is where the
                    // model can now actually check what it already asked.
                    projection.put("search_reuse_instruction",
                            "A prior search_knowledge in this turn already returned a viable hit "
                            + "(faq_miss=false); the hits are in "
                            + "accumulated_tool_results.search_knowledge.hits. Do NOT call "
                            + "search_knowledge again this turn — draft your grounded "
                            + "customer-facing answer via resolve_article from those existing "
                            + "hits (cite the source_id). Every query you have already issued "
                            + "this turn is listed verbatim in search_attempts_this_turn, with "
                            + "whether it actually ran and what it returned (counts in "
                            + "search_attempts_summary); check it before assuming you have not "
                            + "covered this ground. While that viable hit stands, re-issuing "
                            + "search_knowledge — in any wording — does NOT retrieve anything "
                            + "new: the runtime serves the same result back without querying the "
                            + "knowledge base, and the attempt still consumes one of your "
                            + "remaining tool steps.");
                }
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

        // Sprint 86a / S-Auto-31 — moderation_reason_available. PRESENCE-ONLY
        // boolean: true iff a moderation review is on file for the session
        // (session.moderationContext present + non-blank). Lets a UC-FP
        // removal-explanation procedure prefer the moderation-grounded answer
        // when a reason is on record. HARD FENCE (PII / grounding boundary):
        // ONLY the boolean is projected — the raw moderation-review text /
        // reason-code value is NEVER parsed or emitted here.
        String moderationContext = session.getModerationContext();
        boolean moderationReasonAvailable =
                moderationContext != null && !moderationContext.isBlank();
        node.put("moderation_reason_available", moderationReasonAvailable);

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

    /** The one tool this slot describes. Read-only retrieval. */
    private static final String SEARCH_KNOWLEDGE_TOOL = "search_knowledge";

    /**
     * P1 paraphrase-storm fix (2026-07-25) — build the
     * {@code search_attempts_this_turn} slot: every {@code search_knowledge}
     * call already issued in the current {@code AgentRunLoop.run(...)}, in
     * order, with the query VERBATIM and the honest execution outcome.
     *
     * <p>Per entry: {@code at_step}, {@code query}, {@code executed}
     * (false when one of the three idempotency backstops served it without
     * running the tool), {@code served_from} (which prior step / turn's result
     * was replayed), {@code suppression} (which backstop), and the result
     * state actually handed back ({@code faq_miss}, {@code hit_count},
     * {@code top_source_ids}).
     *
     * <p><strong>Why this is not another falsified soft signal.</strong> The
     * A3 / cross-turn soft layers told the model what to do and were ignored;
     * so was the per-call {@code repeat_suppressed.hint} added earlier the
     * same day (measured ignored on session {@code f62ad6ce-…}). This slot
     * tells it nothing — it reports facts the runtime already holds and had
     * been withholding. The model's own step-by-step reasoning on the failing
     * turns ("the prior search was about ad status, not visibility tips") was
     * a *factual claim about its own history* that it had no way to check.
     * Making a claim checkable is a projection duty (§1.4), not persuasion.
     *
     * <p>Zero content matching: no keyword list, no similarity metric, no
     * per-UC branching, no judgement about whether two queries "mean the
     * same". The entries are transcribed from {@link ToolEvent}s.
     *
     * <p>Empty array when there are no search events (shape stability, §N0).
     * Tolerant of malformed/absent result payloads — a field that cannot be
     * read is simply omitted rather than guessed.
     */
    private ArrayNode buildSearchAttemptsNode(List<ToolEvent> priorToolEvents) {
        ArrayNode attempts = objectMapper.createArrayNode();
        if (priorToolEvents == null || priorToolEvents.isEmpty()) {
            return attempts;
        }
        for (ToolEvent evt : priorToolEvents) {
            if (evt == null || !SEARCH_KNOWLEDGE_TOOL.equals(evt.toolName())) {
                continue;
            }
            ObjectNode entry = objectMapper.createObjectNode();
            entry.put("at_step", evt.stepIndex());
            Object q = evt.arguments() == null ? null : evt.arguments().get("query");
            if (q != null) {
                entry.put("query", String.valueOf(q));
            }
            if (!evt.success()) {
                // Rejected by the plan whitelist, or the dispatch failed.
                entry.put("executed", false);
                entry.put("outcome", "failed");
                if (evt.errorMessage() != null) {
                    entry.put("error", evt.errorMessage());
                }
                attempts.add(entry);
                continue;
            }
            boolean suppressed = evt.deduplicated()
                    || evt.paraphraseSuppressed()
                    || evt.crossTurnParaphraseSuppressed();
            entry.put("executed", !suppressed);
            if (suppressed) {
                if (evt.deduplicated()) {
                    entry.put("suppression", "a1_identity_cache");
                    entry.put("served_from", "step " + evt.originalAtStep());
                } else if (evt.paraphraseSuppressed()) {
                    entry.put("suppression", "within_turn_faq_hit");
                    entry.put("served_from", "step " + evt.faqHitAtStep());
                } else {
                    entry.put("suppression", "cross_turn_standing_hit");
                    entry.put("served_from", "turn " + evt.crossTurnHitAtTurn());
                }
            }
            if (evt.resultData() instanceof Map<?, ?> data) {
                Object faqMiss = data.get("faq_miss");
                if (faqMiss instanceof Boolean fm) {
                    entry.put("faq_miss", fm);
                }
                Object hits = data.get("hits");
                if (hits instanceof List<?> hitList) {
                    entry.put("hit_count", hitList.size());
                    ArrayNode ids = objectMapper.createArrayNode();
                    for (Object hit : hitList) {
                        if (hit instanceof Map<?, ?> hitMap) {
                            Object sid = hitMap.get("source_id");
                            if (sid != null) {
                                ids.add(String.valueOf(sid));
                            }
                        }
                    }
                    entry.set("top_source_ids", ids);
                }
            }
            attempts.add(entry);
        }
        return attempts;
    }

    /**
     * P1 paraphrase-storm fix (2026-07-25) — the counting companion to
     * {@link #buildSearchAttemptsNode}: how many search attempts were made
     * this turn, how many actually reached the knowledge base, and how many
     * were served from an existing result without running.
     *
     * <p>{@code distinct_queries} counts distinct verbatim query strings —
     * a plain set cardinality, NOT a similarity or paraphrase judgement. It is
     * reported so the LLM can see the shape of its own behaviour ("9 attempts,
     * 8 distinct strings, 1 real retrieval") without the runtime ruling on
     * whether any two of them mean the same thing. Deciding that remains
     * §1.3 territory.
     */
    private ObjectNode buildSearchAttemptsSummaryNode(List<ToolEvent> priorToolEvents) {
        ObjectNode summary = objectMapper.createObjectNode();
        int attempts = 0;
        int executed = 0;
        int servedWithoutExecuting = 0;
        java.util.Set<String> distinct = new java.util.LinkedHashSet<>();
        if (priorToolEvents != null) {
            for (ToolEvent evt : priorToolEvents) {
                if (evt == null || !SEARCH_KNOWLEDGE_TOOL.equals(evt.toolName())) {
                    continue;
                }
                attempts++;
                Object q = evt.arguments() == null ? null : evt.arguments().get("query");
                if (q != null) {
                    distinct.add(String.valueOf(q));
                }
                if (!evt.success()) {
                    continue;
                }
                if (evt.deduplicated() || evt.paraphraseSuppressed()
                        || evt.crossTurnParaphraseSuppressed()) {
                    servedWithoutExecuting++;
                } else {
                    executed++;
                }
            }
        }
        summary.put("attempts", attempts);
        summary.put("knowledge_base_queries_executed", executed);
        summary.put("served_without_executing", servedWithoutExecuting);
        summary.put("distinct_query_strings", distinct.size());
        return summary;
    }

    // ------------------------------------------------------------------
    // R4.a — ad-context premise projection (#6 customer_context_status +
    // #7 ad_reference). STRUCTURAL ONLY: derived from runtime form_context
    // fields + the lookup_listing_or_ad tool result. NEVER from user-message
    // content / NLP / keyword matching. NO reason text emitted.
    // ------------------------------------------------------------------

    /**
     * Resolved state of the explicit {@code lookup_listing_or_ad} listing
     * verification step. {@code MISSING} (ran but no listing found) is kept
     * DISTINCT from {@code SKIPPED} (never ran) — collapsing them removes the
     * load-bearing premise signal R4 surfaces (anti-误杀 invariant #11).
     */
    enum ListingLookupState { OK, MISSING, FAILED, SKIPPED }

    /**
     * Pure mapping from the three runtime facts about the listing lookup to a
     * {@link ListingLookupState}. Structural only.
     */
    static ListingLookupState deriveListingLookupState(boolean triggered,
                                                       boolean succeeded,
                                                       boolean listingResolved) {
        if (!triggered) {
            return ListingLookupState.SKIPPED;
        }
        if (!succeeded) {
            return ListingLookupState.FAILED;
        }
        return listingResolved ? ListingLookupState.OK : ListingLookupState.MISSING;
    }

    /**
     * Compute the {@code customer_context_status} enum value. Priority order is
     * LOAD-BEARING (anti-误杀 invariant #10): {@code missing_email} /
     * {@code missing_ad_id} / {@code lookup_failed} / {@code lookup_skipped}
     * take precedence over {@code loaded} so a populated customer context never
     * masks an absent ad_id premise (the c7 scenario). FIRST match wins.
     */
    static String computeCustomerContextStatus(String email, String adId,
                                               ListingLookupState lookup) {
        if (email == null || email.isBlank()) {
            return "missing_email";
        }
        if (adId == null || adId.isBlank()) {
            return "missing_ad_id";
        }
        if (lookup == ListingLookupState.FAILED) {
            return "lookup_failed";
        }
        if (lookup == ListingLookupState.SKIPPED) {
            return "lookup_skipped";
        }
        // email + ad_id present, lookup ran (OK or ran-but-no-result MISSING):
        // the premise is verifiable. The ad-specific nuance (OK vs MISSING)
        // is carried by ad_reference.listing_lookup (#7).
        return "loaded";
    }

    /** Lower-case token for the {@code ad_reference.listing_lookup} field. */
    static String listingLookupToken(ListingLookupState state) {
        switch (state) {
            case OK:
                return "ok";
            case MISSING:
                return "missing";
            case FAILED:
                return "failed";
            case SKIPPED:
            default:
                return "skipped";
        }
    }

    /**
     * Emit the R4 {@code customer_context_status} enum (#6) and the
     * {@code ad_reference} struct (#7) onto the live-path projection. Reads
     * {@code form_context.email} / {@code form_context.ad_id} and the most
     * recent {@code lookup_listing_or_ad} tool event from the current run's
     * {@code priorToolEvents}. Block content is ground-truth runtime state
     * only — no LLM-generated string, no reason text.
     */
    private void addAdContextPremiseProjection(ObjectNode projection,
                                               BotSession session,
                                               List<ToolEvent> priorToolEvents) {
        String email = formField(session, "email");
        String adId = formField(session, "ad_id");

        ToolEvent lookupEvent = lastToolEvent(priorToolEvents, LOOKUP_LISTING_TOOL);
        boolean triggered = lookupEvent != null;
        boolean succeeded = triggered && lookupEvent.success();
        boolean listingResolved = succeeded && toolResultResolvedListing(lookupEvent);
        ListingLookupState lookupState =
                deriveListingLookupState(triggered, succeeded, listingResolved);

        projection.put("customer_context_status",
                computeCustomerContextStatus(email, adId, lookupState));

        ObjectNode adReference = objectMapper.createObjectNode();
        if (adId == null || adId.isBlank()) {
            adReference.putNull("form_ad_id");
        } else {
            adReference.put("form_ad_id", adId);
        }
        adReference.put("listing_lookup", listingLookupToken(lookupState));
        projection.set("ad_reference", adReference);
    }

    /**
     * Read a single string field from {@code session.formContext} (the parsed
     * pre-chat form JSON). Returns null on absent / blank / parse failure —
     * the projection never fails closed on a malformed form blob.
     */
    private String formField(BotSession session, String field) {
        if (session == null || session.getFormContext() == null
                || session.getFormContext().isBlank()) {
            return null;
        }
        try {
            JsonNode form = objectMapper.readTree(session.getFormContext());
            JsonNode value = form == null ? null : form.get(field);
            if (value == null || value.isNull()) {
                return null;
            }
            String text = value.isValueNode() ? value.asText("") : value.toString();
            return (text == null || text.isBlank()) ? null : text;
        } catch (Exception ex) {
            return null;
        }
    }

    /** Most recent successful-or-failed event for {@code toolName}, or null. */
    private static ToolEvent lastToolEvent(List<ToolEvent> events, String toolName) {
        if (events == null || events.isEmpty()) {
            return null;
        }
        ToolEvent found = null;
        for (ToolEvent evt : events) {
            if (evt != null && toolName.equals(evt.toolName())) {
                found = evt;
            }
        }
        return found;
    }

    /**
     * True iff the {@code lookup_listing_or_ad} result resolved a listing.
     * {@link com.gumtree.csagent.service.tools.LookupListingTool} returns
     * {@code {found:true, listing:{...}}} on a hit and {@code {found:false}}
     * when no listing exists.
     */
    private static boolean toolResultResolvedListing(ToolEvent event) {
        if (event == null || !event.success() || event.resultData() == null) {
            return false;
        }
        Object data = event.resultData();
        if (data instanceof Map<?, ?> map) {
            return Boolean.TRUE.equals(map.get("found"));
        }
        return false;
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
     * Sprint 53 / M5 S4 — Skill-declared context-key gating helper (C2 #2).
     *
     * <p>Returns {@code true} iff the Skill that
     * {@link SkillRegistry#select} maps to the session's
     * {@code (currentPhase, activeUseCase)} tuple declares {@code key} in
     * its {@code required_context_keys} list. Defaults to {@code true}
     * (emit) when (a) the registry is unavailable, (b) the session has
     * no current phase, OR (c) no Skill maps the tuple — defensive
     * pre-S4 behaviour preservation for legacy / unmapped sessions.
     *
     * <p>Per the Phase-A audit
     * ({@code docs/diagnostics/m5-s4-skill-declaration-audit.md} §4.A),
     * only {@code candidate_use_cases} is routed to this gate in S4
     * (the four other context slots stay unconditional / data-gated /
     * registry-gated per the audit's §4.B / §4.C disposition).
     *
     * <p>§1.7 boundary: no per-UC if-else, no keyword/regex/enum branch.
     * The gate reads registry data (the Skill YAML declaration); the
     * defensive fallback is a single uniform {@code true}.
     */
    private boolean skillRequiresContextKey(BotSession session, String activeUseCase, String key) {
        if (skillRegistry == null || session == null) {
            return true;
        }
        String phase = session.getCurrentPhase();
        if (phase == null) {
            return true;
        }
        Optional<Skill> selected = skillRegistry.select(phase, activeUseCase);
        if (selected.isEmpty()) {
            return true;
        }
        List<String> declared = selected.get().requiredContextKeys();
        return declared != null && declared.contains(key);
    }

    /**
     * Sprint 53 / M5 S4 — Skill-declared soft-signal gating helper (C2 #5).
     *
     * <p>Returns {@code true} iff the Skill that
     * {@link SkillRegistry#select} maps to the session's
     * {@code (currentPhase, activeUseCase)} tuple declares {@code slot}
     * in its {@code state_inheritance.soft_signal_via_projection} list.
     * Defaults to {@code true} (emit) when (a) the registry is
     * unavailable, (b) the session has no current phase, OR (c) no
     * Skill maps the tuple — same defensive pre-S4 default as
     * {@link #skillRequiresContextKey} for legacy/unmapped sessions.
     *
     * <p>Per the Phase-A audit
     * ({@code docs/diagnostics/m5-s4-skill-declaration-audit.md} §3.G /
     * §3.H / §3.I), the three soft-signal slots
     * ({@code alternate_candidate_use_cases},
     * {@code discover_disambiguation_signals},
     * {@code prior_use_case_carry}) all route to this gate; their DECL
     * coverage matches their NEED column exactly (no dropped signal).
     *
     * <p>§1.7 boundary: registry/Skill-driven; the soft signal stays
     * LLM-owned per §1.3 (the gate decides whether the slot is
     * projected; the LLM decides whether to act on it).
     */
    private boolean skillDeclaresSoftSignal(BotSession session, String activeUseCase, String slot) {
        if (skillRegistry == null || session == null) {
            return true;
        }
        String phase = session.getCurrentPhase();
        if (phase == null) {
            return true;
        }
        Optional<Skill> selected = skillRegistry.select(phase, activeUseCase);
        if (selected.isEmpty()) {
            return true;
        }
        return selected.get().stateInheritance().softSignalViaProjection().contains(slot);
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
