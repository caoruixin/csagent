package com.gumtree.csagent.service.runtime.skill;

import com.gumtree.csagent.model.PhasePlan;
import com.gumtree.csagent.model.ToolCall;
import com.gumtree.csagent.service.runtime.IntakeFieldsRegistry;
import com.gumtree.csagent.service.runtime.ResolveDispositionEvaluator;
import com.gumtree.csagent.service.tools.RecordOutcomeTool;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Sprint 39 — unified Skill terminal-predicate dispatcher per Sprint 37 freeze
 * decision (h) §9. Replaces the scattered {@code shouldRejectXxx} static
 * methods previously in {@code AgentRunLoopImpl} (Sprint 6 §G2 / Sprint 7 §I2 /
 * Sprint 11 §M1) with declarative {@code guardrails[]} declarations enforced
 * uniformly across the four canonical predicate types.
 *
 * <h2>Public surface (design doc §9.1)</h2>
 *
 * <p>Two entry points, mirroring the two existing dispatch surfaces in
 * {@code AgentRunLoopImpl}:
 *
 * <ul>
 *   <li>{@link #checkBeforeDispatch} — invoked BEFORE a tool call is
 *       dispatched. Fires guardrails of type
 *       {@code faq_miss_handover_requires_resolve_attempt} and
 *       {@code intake_complete_required} (both target {@code request_handover}).</li>
 *   <li>{@link #checkBeforeOutcomePersist} — invoked BEFORE
 *       {@code record_outcome} outcome persistence. Fires guardrails of type
 *       {@code premature_resolve_outcome_guard} (Sprint 11 §M1) and
 *       {@code must_cite_source} (NEW S1 per M2 §6 #4 verbatim authorization).</li>
 * </ul>
 *
 * <p>Each method exists in two forms: the primary form takes a {@link Skill}
 * directly per design doc §9.1 (test-friendly); the convenience overload
 * takes a {@link PhasePlan} and looks up the active Skill via the injected
 * {@link SkillRegistry}.
 *
 * <h2>Composition</h2>
 *
 * <p>Walks the active Skill's {@code guardrails[]} in declaration order with
 * <strong>short-circuit on first reject</strong> per design doc §9.2. Each
 * per-type handler is registry-driven (no per-UC-pair branch logic) and
 * returns an {@link Optional}; the dispatcher returns the first non-empty
 * verdict it encounters.
 *
 * <h2>Bounded scope (NOT a generic rule engine)</h2>
 *
 * <p>The four typed predicate types are pinned to the {@code SkillLoader}
 * {@code VALID_GUARDRAIL_TYPES} allowlist (Sprint 38 fix iteration #1). Any
 * future predicate type addition requires extending BOTH the allowlist AND a
 * new handler in this class. The dispatcher is intentionally NOT a generic
 * rule engine accepting arbitrary external predicate definitions per design
 * doc §9.1 + Sprint 39 contract §6 #5.
 *
 * <h2>S1 {@code must_cite_source} authorization scope (M2 §6 #4 verbatim)</h2>
 *
 * <p>The {@code must_cite_source} handler fires ONLY on
 * {@code record_outcome(class=resolve)} within the {@code RESOLVE_FAQ} scope
 * and checks only {@code source_id} citation presence in the bot's
 * user-facing message. It does NOT judge correctness, relevance, or content
 * quality. Expansion beyond this scope is OUT OF SCOPE for Sprint 39.
 */
@Component
public class SkillGuardrailDispatcher {

    // --- guardrail type tokens (mirror SkillLoader.VALID_GUARDRAIL_TYPES) ---
    static final String TYPE_FAQ_MISS_HANDOVER_REQUIRES_RESOLVE_ATTEMPT =
            "faq_miss_handover_requires_resolve_attempt";
    static final String TYPE_INTAKE_COMPLETE_REQUIRED = "intake_complete_required";
    static final String TYPE_PREMATURE_RESOLVE_OUTCOME_GUARD = "premature_resolve_outcome_guard";
    static final String TYPE_MUST_CITE_SOURCE = "must_cite_source";

    // --- predicate names / reject reason labels (preserved from Sprint 6/7/11) ---
    /** Sprint 6 §G2 — preserved from {@code AgentRunLoopImpl.S1_GUARD_REJECT_REASON}. */
    public static final String FAQ_MISS_REJECT_REASON =
            "s1_resolve_required_before_faq_miss_handover";
    /** Sprint 7 §I2 — preserved from {@code AgentRunLoopImpl.INTAKE_COMPLETE_GUARD_REJECT_REASON}. */
    public static final String INTAKE_INCOMPLETE_REJECT_REASON =
            "intake_required_fields_missing_for_intake_complete";
    /** Sprint 11 §M1 — preserved from {@code AgentRunLoopImpl.PROGRESSIVE_RESOLVE_GUARD_REJECT_REASON}. */
    public static final String PROGRESSIVE_RESOLVE_REJECT_REASON =
            "progressive_resolve_record_outcome_premature";
    /** Sprint 39 NEW S1 — per design doc §9.4. */
    public static final String S1_CITATION_PRESENCE_REQUIRED =
            "s1_citation_presence_required";

    private static final String HANDOVER_TOOL = "request_handover";
    private static final String SEARCH_TOOL = "search_knowledge";
    private static final String RESOLVE_TOOL = "resolve_article";
    private static final String FAQ_MISS_REASON = "faq_miss_threshold_exceeded";

    private final SkillRegistry skillRegistry;
    private final ObjectMapper objectMapper;

    public SkillGuardrailDispatcher(SkillRegistry skillRegistry, ObjectMapper objectMapper) {
        this.skillRegistry = Objects.requireNonNull(skillRegistry, "skillRegistry");
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
    }

    // ------------------------------------------------------------------
    // checkBeforeDispatch — fires on tool-call dispatch (request_handover)
    // ------------------------------------------------------------------

    /**
     * Convenience overload that resolves the active Skill from the registry
     * via {@code (plan.phase(), plan.useCase())} before dispatching.
     *
     * <p>Returns an empty Optional when {@code plan} is null OR no Skill is
     * registered for the (phase, useCase) tuple (e.g., RESOLVE on an unknown
     * UC). This matches the legacy behaviour of the {@code shouldRejectXxx}
     * methods which returned {@code false} on unmapped plans.
     */
    public Optional<RejectVerdict> checkBeforeDispatch(PhasePlan plan,
                                                        ToolCall call,
                                                        DispatchContext context) {
        if (plan == null) return Optional.empty();
        Optional<Skill> active = skillRegistry.select(plan.phase(), plan.useCase());
        if (active.isEmpty()) return Optional.empty();
        return checkBeforeDispatch(active.get(), call, context);
    }

    /**
     * Primary form per design doc §9.1. Walks {@code activeSkill.guardrails()}
     * in declaration order; short-circuits on the first reject per §9.2.
     * Only guardrails that fire on tool-call dispatch are evaluated here
     * (others — premature_resolve_outcome_guard / must_cite_source — flow
     * through {@link #checkBeforeOutcomePersist}).
     */
    public Optional<RejectVerdict> checkBeforeDispatch(Skill activeSkill,
                                                        ToolCall call,
                                                        DispatchContext context) {
        if (activeSkill == null || call == null) return Optional.empty();
        for (Guardrail g : activeSkill.guardrails()) {
            Optional<RejectVerdict> verdict = switch (g.type()) {
                case TYPE_FAQ_MISS_HANDOVER_REQUIRES_RESOLVE_ATTEMPT ->
                        handleFaqMissHandoverRequiresResolveAttempt(activeSkill, g, call, context);
                case TYPE_INTAKE_COMPLETE_REQUIRED ->
                        handleIntakeCompleteRequired(activeSkill, g, call, context);
                // The two outcome-persistence types are intentionally
                // not invoked on tool-call dispatch — they fire via
                // checkBeforeOutcomePersist instead per design doc §9.3.
                case TYPE_PREMATURE_RESOLVE_OUTCOME_GUARD,
                     TYPE_MUST_CITE_SOURCE -> Optional.empty();
                default -> Optional.empty();
            };
            if (verdict.isPresent()) {
                return verdict;
            }
        }
        return Optional.empty();
    }

    // ------------------------------------------------------------------
    // checkBeforeOutcomePersist — fires on record_outcome dispatch
    // ------------------------------------------------------------------

    /**
     * Convenience overload that resolves the active Skill from the registry.
     *
     * @see #checkBeforeDispatch(PhasePlan, ToolCall, DispatchContext)
     */
    public Optional<RejectVerdict> checkBeforeOutcomePersist(PhasePlan plan,
                                                              String outcomeClass,
                                                              DispatchContext context) {
        if (plan == null) return Optional.empty();
        Optional<Skill> active = skillRegistry.select(plan.phase(), plan.useCase());
        if (active.isEmpty()) return Optional.empty();
        return checkBeforeOutcomePersist(active.get(), outcomeClass, context);
    }

    /**
     * Primary form per design doc §9.1. Walks {@code activeSkill.guardrails()}
     * in declaration order; short-circuits on the first reject per §9.2.
     */
    public Optional<RejectVerdict> checkBeforeOutcomePersist(Skill activeSkill,
                                                              String outcomeClass,
                                                              DispatchContext context) {
        if (activeSkill == null) return Optional.empty();
        for (Guardrail g : activeSkill.guardrails()) {
            Optional<RejectVerdict> verdict = switch (g.type()) {
                case TYPE_PREMATURE_RESOLVE_OUTCOME_GUARD ->
                        handlePrematureResolveOutcomeGuard(activeSkill, g, outcomeClass, context);
                case TYPE_MUST_CITE_SOURCE ->
                        handleMustCiteSource(activeSkill, g, outcomeClass, context);
                // Tool-call dispatch types are no-ops on outcome persist.
                case TYPE_FAQ_MISS_HANDOVER_REQUIRES_RESOLVE_ATTEMPT,
                     TYPE_INTAKE_COMPLETE_REQUIRED -> Optional.empty();
                default -> Optional.empty();
            };
            if (verdict.isPresent()) {
                return verdict;
            }
        }
        return Optional.empty();
    }

    // ------------------------------------------------------------------
    // Per-type handlers
    // ------------------------------------------------------------------

    /**
     * Sprint 6 §G2 migration (design doc §8.2.1). Refuses
     * {@code request_handover(faq_miss_threshold_exceeded)} when
     * {@code search_knowledge} already returned a viable hit and
     * {@code resolve_article} has not yet been attempted. The Skill's
     * {@code applicable_use_cases: [UC-A..UC-FP]} encodes the FAQ-path UC
     * scope; this handler does NOT re-check the UC set.
     */
    private Optional<RejectVerdict> handleFaqMissHandoverRequiresResolveAttempt(
            Skill skill, Guardrail g, ToolCall call, DispatchContext ctx) {
        if (!HANDOVER_TOOL.equals(safeToolName(call))) return Optional.empty();
        Map<String, Object> args = call.getArguments();
        if (args == null) return Optional.empty();
        Object reasonObj = args.get("escalation_reason");
        if (!(reasonObj instanceof String reason) || !FAQ_MISS_REASON.equals(reason)) {
            return Optional.empty();
        }
        Map<String, Object> accumulated = ctx.accumulatedToolResults();
        // Already resolved? Allow through (a non-error resolve_article entry
        // counts as a prior attempt).
        Object resolveData = accumulated.get(RESOLVE_TOOL);
        if (resolveData != null
                && !(resolveData instanceof Map<?, ?> rm && rm.containsKey("error"))) {
            return Optional.empty();
        }
        // Search not yet run, or search errored, or faq_miss=true, or no
        // hits? Allow the handover through (no viable evidence to leverage).
        Object searchData = accumulated.get(SEARCH_TOOL);
        if (!(searchData instanceof Map<?, ?> searchMap)) return Optional.empty();
        if (searchMap.containsKey("error")) return Optional.empty();
        Object faqMiss = searchMap.get("faq_miss");
        if (Boolean.TRUE.equals(faqMiss)) return Optional.empty();
        Object hits = searchMap.get("hits");
        if (!(hits instanceof List<?> hitList) || hitList.isEmpty()) return Optional.empty();

        Map<String, Object> trace = new LinkedHashMap<>();
        trace.put("predicate_name", FAQ_MISS_REJECT_REASON);
        trace.put("decision_outcome", "rejected");
        trace.put("skill_name", skill.name());
        trace.put("reject_reason_label", FAQ_MISS_REJECT_REASON);
        trace.put("escalation_reason_requested", reason);
        trace.put("search_knowledge_present", true);
        trace.put("resolve_article_attempted", false);
        return Optional.of(new RejectVerdict(
                FAQ_MISS_REJECT_REASON,
                "Call resolve_article for the top search_knowledge hit before "
                        + "escalating with faq_miss_threshold_exceeded.",
                trace));
    }

    /**
     * Sprint 7 §I2 migration (design doc §8.2.2). Refuses
     * {@code request_handover(intake_complete_for_uc_X)} when the required
     * intake fields for the active UC are not all collected. Delegates
     * required-fields enforcement to {@link IntakeFieldsRegistry}.
     */
    private Optional<RejectVerdict> handleIntakeCompleteRequired(
            Skill skill, Guardrail g, ToolCall call, DispatchContext ctx) {
        if (!HANDOVER_TOOL.equals(safeToolName(call))) return Optional.empty();
        Map<String, Object> args = call.getArguments();
        if (args == null) return Optional.empty();
        Object reasonObj = args.get("escalation_reason");
        if (!(reasonObj instanceof String reason)) return Optional.empty();
        // Pattern match: intake_complete_for_uc_* (the canonical Sprint 7
        // §I2 trigger reason). The pattern is declared in the guardrail
        // parameters; we honor the prefix form (equivalent to the legacy
        // startsWith check that Sprint 7 §I2 used).
        if (!reason.startsWith("intake_complete_for_uc_")) return Optional.empty();
        PhasePlan plan = ctx.plan();
        if (plan == null) return Optional.empty();
        String activeUc = plan.useCase();
        if (!IntakeFieldsRegistry.isIntakeUseCase(activeUc)) return Optional.empty();
        Map<String, String> collected = parseSessionIntakeFields(ctx);
        if (IntakeFieldsRegistry.intakeComplete(activeUc, collected)) {
            return Optional.empty();
        }
        List<String> missing = IntakeFieldsRegistry.fieldsRemaining(activeUc, collected);
        Map<String, Object> trace = new LinkedHashMap<>();
        trace.put("predicate_name", INTAKE_INCOMPLETE_REJECT_REASON);
        trace.put("decision_outcome", "rejected");
        trace.put("skill_name", skill.name());
        trace.put("reject_reason_label", INTAKE_INCOMPLETE_REJECT_REASON);
        trace.put("escalation_reason_requested", reason);
        trace.put("missing_fields", missing);
        return Optional.of(new RejectVerdict(
                INTAKE_INCOMPLETE_REJECT_REASON,
                "Ask the user for the missing intake fields above, then call "
                        + "request_handover with arguments.intake_fields populated.",
                trace));
    }

    /**
     * Sprint 11 §M1 migration (design doc §8.2.3). Delegates to the
     * <strong>untouched</strong> {@link ResolveDispositionEvaluator}
     * frozen surface per {@code runtime_freeze_and_risk_policy.md} §1.1 #3.
     * Only the invocation site moves from {@code AgentRunLoopImpl} to this
     * dispatcher; the predicate logic itself is unchanged.
     */
    private Optional<RejectVerdict> handlePrematureResolveOutcomeGuard(
            Skill skill, Guardrail g, String outcomeClass, DispatchContext ctx) {
        PhasePlan plan = ctx.plan();
        String currentPhase = ctx.session() == null ? null : ctx.session().getCurrentPhase();
        if (!ResolveDispositionEvaluator.shouldRejectPrematureResolveOutcome(
                plan, currentPhase, outcomeClass)) {
            return Optional.empty();
        }
        Map<String, Object> trace = new LinkedHashMap<>();
        trace.put("predicate_name", PROGRESSIVE_RESOLVE_REJECT_REASON);
        trace.put("decision_outcome", "rejected");
        trace.put("skill_name", skill.name());
        trace.put("reject_reason_label", PROGRESSIVE_RESOLVE_REJECT_REASON);
        trace.put("outcome_class_requested", outcomeClass);
        trace.put("current_phase", currentPhase);
        return Optional.of(new RejectVerdict(
                PROGRESSIVE_RESOLVE_REJECT_REASON,
                "Stay in RESOLVE and wait for the user to confirm the answer or "
                        + "supply more detail before recording a resolve outcome.",
                trace));
    }

    /**
     * Sprint 39 NEW S1 (design doc §8.2.4). Refuses
     * {@code record_outcome(class=resolve)} inside the {@code RESOLVE_FAQ}
     * scope when the bot's user-facing message text does not include a
     * {@code source_id} citation. The Skill's
     * {@code applicable_use_cases: [UC-A..UC-FP]} encodes the RESOLVE_FAQ
     * scope; this handler does NOT re-check the UC set. The handler does
     * NOT fire on {@code class=escalate} or {@code class=abandon}, and it
     * does NOT judge correctness, relevance, or content quality of the
     * citation per M2 §6 #4 verbatim authorization.
     */
    private Optional<RejectVerdict> handleMustCiteSource(
            Skill skill, Guardrail g, String outcomeClass, DispatchContext ctx) {
        if (outcomeClass == null) return Optional.empty();
        String normalized = outcomeClass.trim().toLowerCase(Locale.ROOT);
        // RecordOutcomeTool.normalizeOutcomeClass resolves canonical
        // "resolve" + legacy "RESOLVED" alias; mirror that here.
        if (!"resolve".equals(normalized) && !"resolved".equals(normalized)) {
            return Optional.empty();
        }
        // Configured outcome_class parameter must agree (Sprint 39 contract
        // §2.5 D-n: parameters.outcome_class=resolve). If the YAML declared
        // a different outcome_class, the handler bows out.
        Object configuredOutcome = g.parameters().get("outcome_class");
        if (configuredOutcome != null
                && !configuredOutcome.toString().equalsIgnoreCase("resolve")) {
            return Optional.empty();
        }
        String citeToken = "source_id";
        Object citeField = g.parameters().get("cite_token_field");
        if (citeField instanceof String s && !s.isBlank()) {
            citeToken = s.trim();
        }
        String userMessage = ctx.parsedUserMessage().orElse(null);
        if (userMessage == null || userMessage.isBlank()) {
            userMessage = ctx.lastLlmRawResponse();
        }
        if (userMessage != null && userMessage.contains(citeToken)) {
            return Optional.empty();
        }
        Map<String, Object> trace = new LinkedHashMap<>();
        trace.put("predicate_name", S1_CITATION_PRESENCE_REQUIRED);
        trace.put("decision_outcome", "rejected");
        trace.put("skill_name", skill.name());
        trace.put("reject_reason_label", S1_CITATION_PRESENCE_REQUIRED);
        trace.put("outcome_class_requested", outcomeClass);
        trace.put("cite_token_field", citeToken);
        trace.put("user_message_present", userMessage != null && !userMessage.isBlank());
        return Optional.of(new RejectVerdict(
                S1_CITATION_PRESENCE_REQUIRED,
                "The user-facing message must include a " + citeToken + " citation from a "
                        + "successful resolve_article call before record_outcome with "
                        + "class=resolve can persist. Cite the relevant article in your "
                        + "user_message.",
                trace));
    }

    // ------------------------------------------------------------------
    // helpers
    // ------------------------------------------------------------------

    private static String safeToolName(ToolCall call) {
        return call == null ? null : call.getName();
    }

    private Map<String, String> parseSessionIntakeFields(DispatchContext ctx) {
        if (ctx == null || ctx.session() == null) return Map.of();
        try {
            return IntakeFieldsRegistry.parseCollectedFields(
                    objectMapper, ctx.session().getIntakeFields());
        } catch (Exception ex) {
            return Map.of();
        }
    }

    /**
     * Hook used by the unit-test suite to read the canonical outcome-class
     * normalization that {@link RecordOutcomeTool} performs at the kernel.
     * Mirrors the existing alias mapping (canonical {@code resolve} + legacy
     * {@code RESOLVED}); kept here so the dispatcher does not need a runtime
     * dependency on {@code RecordOutcomeTool} just for the normalization.
     */
    static String normalizeOutcomeClass(String raw) {
        if (raw == null) return null;
        String n = raw.trim().toLowerCase(Locale.ROOT);
        return "resolved".equals(n) ? "resolve" : n;
    }
}
