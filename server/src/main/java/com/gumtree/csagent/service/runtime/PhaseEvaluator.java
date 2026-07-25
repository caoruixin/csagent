package com.gumtree.csagent.service.runtime;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gumtree.csagent.model.*;
import com.gumtree.csagent.service.guardrails.ScriptLibraryService;
import com.gumtree.csagent.service.knowledge.KnowledgeSearchService;
import com.gumtree.csagent.service.observability.EventEmitter;
import com.gumtree.csagent.service.runtime.skill.Skill;
import com.gumtree.csagent.service.runtime.skill.SkillGuardrailDispatcher;
import com.gumtree.csagent.service.runtime.skill.SkillRegistry;
import com.gumtree.csagent.service.runtime.skill.SkillStateBus;
import com.gumtree.csagent.service.tools.CreateCaseControlledTool;
import com.gumtree.csagent.service.tools.ToolDispatcher;
import com.gumtree.csagent.service.tools.ToolResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Per-phase logic evaluator. Determines what action to take based on
 * the current session phase and state.
 *
 * <p>Task #10: control-flow decisions are now derived from {@link ParsedAction#getToolCalls()}
 * and {@link ParsedAction#getUserMessage()} (single-layer tool-use contract — see phase0 §0.6
 * and phase3 §3.3.3). The legacy 5-action switch has been removed.
 */
@Slf4j
@Service
public class PhaseEvaluator {

    // WS-3 / A3 (2026-07-25): the literal
    // `INTAKE_UCS = {UC-G, UC-H, UC-I, UC-J, UC-K}` constant that used to live
    // here is removed. "Is this UC intake-only?" is now answered by
    // `isIntakeOnlyUseCase(String)` below, which reads the `path` field of
    // `config/use-case-registry.yaml` through the injected registry (the
    // service's own `isIntakeOnlyPath` predicate is the same rule, exposed for
    // callers that hold the registry directly). The duplicated constant was the reason
    // UC-K stayed intake-only after the domain spec had declared it `partial`:
    // four Java sets and two eval-side Python sets each had to be edited in
    // lockstep, and they had already drifted apart over UC-K.

    /**
     * Canonical 24-value escalation_reason enum (mirrors
     * {@code eval_interactive/eval_interactive/case_spec/schema.py}).
     * Anything emitted by this evaluator that is NOT in this set must be
     * mapped to {@code "service_degraded"} via {@link #canonicalize(String)}
     * to keep the L1 trace contract green.
     *
     * <p>Sprint 096 / S-Auto-44 (M-Auto-9 WP1): added
     * {@code "agent_unable_to_resolve"} — the lowest-priority semantic
     * reason the LLM selects for a bot-initiated, in-scope,
     * exhausted-resolution, unresolved handover. It is never auto-stamped
     * by the runtime (this evaluator does not emit it; unknown values
     * still coerce to {@code service_degraded}).
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
            "runtime_error_threshold",
            "agent_unable_to_resolve"
    );

    /**
     * Map a possibly-non-canonical escalation reason to a canonical value.
     * Returns {@code reason} when it is already canonical, otherwise
     * maps the well-known legacy literals onto their canonical
     * equivalents (kept in lockstep with
     * {@link EscalationReasonResolver#canonicalize(String)} so the LLM
     * path and the resolver path always pick the same value), and
     * falls back to {@code "service_degraded"} for anything else.
     */
    private String canonicalize(String reason) {
        if (reason != null && CANONICAL_ESCALATION_REASONS.contains(reason)) {
            return reason;
        }
        if (reason != null) {
            String lower = reason.trim().toLowerCase(Locale.ENGLISH);
            // Sprint §B0: keep the legacy LLM literal -> canonical mapping
            // identical to EscalationReasonResolver so tool-call, session,
            // and handover payload converge on the same enum value.
            if ("user_requested_escalation".equals(lower)
                    || "user_request".equals(lower)
                    || "human_requested".equals(lower)
                    || "callback_requested".equals(lower)) {
                return "user_requested";
            }
        }
        return "service_degraded";
    }

    /** Map from intake UC ID to its script template prefix. */
    private static final Map<String, String> INTAKE_TEMPLATE_PREFIX = Map.of(
            "UC-G", "g",
            "UC-H", "h",
            "UC-I", "i",
            "UC-J", "j",
            "UC-K", "k"
    );

    /** Opening template keys per intake UC (first available is used). */
    private static final Map<String, List<String>> INTAKE_OPENING_TEMPLATES = Map.of(
            "UC-G", List.of("g_process_explanation"),
            "UC-H", List.of("h_empathy"),
            "UC-I", List.of("i_disclaimer"),
            "UC-J", List.of("j_acknowledgment"),
            "UC-K", List.of("k_basic_troubleshoot")
    );

    /** Map from intake UC ID to the human-readable team name for template variable substitution. */
    private static final Map<String, String> UC_TEAM_NAME = Map.of(
            "UC-G", "Data Protection",
            "UC-H", "Ad Support",
            "UC-I", "Payments",
            "UC-J", "Trust & Safety",
            "UC-K", "Technical Support"
    );

    /** Default SLA hours used in template variable substitution. */
    private static final String DEFAULT_SLA_HOURS = "24-48";

    /**
     * Map from intake UC ID to its canonical request_handover.escalation_reason
     * enum value (per docs/customer_service_tool_spec_v0_2.yaml lines 433-457).
     * Intake-only UCs (UC-G/H/I/J/K) emit one of these once intake is complete;
     * non-intake UCs do not flow through this path.
     */
    private static final Map<String, String> INTAKE_ESCALATION_TRIGGER = Map.of(
            "UC-G", "intake_complete_for_uc_g",
            "UC-H", "intake_complete_for_uc_h",
            "UC-I", "intake_complete_for_uc_i",
            "UC-J", "intake_complete_for_uc_j",
            "UC-K", "intake_complete_for_uc_k"
    );

    /**
     * Resolve the canonical escalation_reason enum for an intake-complete
     * handover. Falls back to the bare {@code intake_complete} legacy literal
     * if the active UC is not in the intake set, but in practice this branch
     * is only reached by {@code resolveIntake} which is gated on UC-G/H/I/J/K.
     */
    private String intakeCompleteTrigger(String activeUc) {
        return INTAKE_ESCALATION_TRIGGER.getOrDefault(activeUc, "intake_complete");
    }

    /**
     * WS-3 / A3 (2026-07-25) — "is this UC intake-ONLY?", read from the
     * registry's {@code path} field rather than from a literal UC set.
     *
     * <p>{@code path: INTAKE} means the UC may never retrieve knowledge and
     * never terminates on a customer-facing final answer. {@code path: FAQ}
     * and the new {@code path: PARTIAL} both may. Resolving through
     * {@link UseCaseRegistryService#getUseCase(String)} (rather than the
     * service's own predicate) keeps this consistent with every other registry
     * read in this class. An unknown UC is not treated as intake-only — the
     * callers' fall-through paths are the safe ones.
     */
    private boolean isIntakeOnlyUseCase(String ucId) {
        UseCaseRegistryService.UseCaseDefinition def = useCaseRegistry.getUseCase(ucId);
        return def != null && UseCaseRegistryService.PATH_INTAKE.equals(def.path());
    }

    /**
     * Pick the canonical escalation_reason for a {@link TerminalOutcome#MAX_STEPS}
     * exit. See the call site for rationale (Codex 1.9). Falls back to
     * {@code turn_budget_exhausted} when nothing more specific applies.
     *
     * <p>Heuristics, in priority order (Codex 2026-05-03 round 3 — clarification
     * count is checked before knowledge search so a mixed search + clarify
     * loop attributes to the user-feedback-driven signal that actually stalled
     * the conversation, instead of the first FAQ attempt):
     * <ol>
     *   <li>Plan is INTAKE (UC-G/H/I/J/K) → {@code incomplete_intake} (the loop
     *       exhausted without collecting all required fields).</li>
     *   <li>Session has at least one logged clarification turn → {@code
     *       clarification_budget_exhausted} (the agent kept asking instead of
     *       converging).</li>
     *   <li>The most-recent {@code search_knowledge} result was a genuine miss
     *       ({@code faq_miss=true}) → {@code faq_miss_threshold_exceeded} (only
     *       reachable when the agent did not also stall on clarification).
     *       Sprint 070 / S-Auto-14 (B1) made this step evidence-aware: a
     *       search that returned a viable hit ({@code faq_miss=false}) no
     *       longer attributes here — the loop ran out of budget with a usable
     *       answer in hand, which is the catch-all below, not a knowledge
     *       miss. A null / malformed result map also falls through (no
     *       positive miss evidence).</li>
     *   <li>Otherwise → {@code turn_budget_exhausted} (catch-all per Phase 2
     *       §2.4 — also covers a viable-hit exhaustion and a turn with no
     *       {@code search_knowledge} call).</li>
     * </ol>
     */
    String resolveMaxStepsReason(PhasePlan plan,
                                 AgentRunResult result,
                                 BotSession session) {
        // WS-3 / A3: intake-ONLY plans attribute to incomplete_intake. A
        // PARTIAL-path plan (UC-K) that exhausted its steps may have been
        // retrieving rather than collecting, so it falls through to the
        // evidence-aware FAQ / turn-budget attribution below instead of being
        // stamped with an intake reason it may not have earned.
        if (plan != null && plan.useCase() != null
                && isIntakeOnlyUseCase(plan.useCase())) {
            return "incomplete_intake";
        }
        if (session != null && session.getClarificationCount() != null
                && session.getClarificationCount() > 0) {
            return "clarification_budget_exhausted";
        }
        // Step 3 (Sprint 070 / S-Auto-14, B1) — evidence-aware FAQ attribution.
        // Attribute to faq_miss_threshold_exceeded ONLY when the MOST-RECENT
        // search_knowledge result was a genuine miss (faq_miss=true). When the
        // last search returned a viable hit (faq_miss=false) the loop ran out
        // of budget WITH a usable answer, which is the turn_budget_exhausted
        // catch-all below, not a knowledge miss. The faq_miss flag is read off
        // the dispatched result map (the same flag the S-Auto-13b A3 gate
        // reads); a null / malformed map falls through (no positive miss).
        ToolEvent lastSearch = null;
        if (result != null && result.toolEvents() != null) {
            for (ToolEvent te : result.toolEvents()) {
                if ("search_knowledge".equals(te.toolName())) {
                    lastSearch = te;
                }
            }
        }
        if (lastSearch != null && lastSearch.resultData() instanceof Map<?, ?> data
                && Boolean.TRUE.equals(data.get("faq_miss"))) {
            return "faq_miss_threshold_exceeded";
        }
        return "turn_budget_exhausted";
    }

    // Sprint 39 — the legacy buildIntakeSystemInstruction(uc, ucDef) helper
    // is removed; its content is now externalized in
    // server/src/main/resources/skills/resolve_intake_collect_and_handover.yaml
    // (procedure field) and the per-UC substitutions are performed by
    // substitutePlaceholders(...) below per Sprint 37 freeze §6.2.5.
    // DEFAULT_SLA_HOURS is a hardcoded "24-48" in the Skill YAML procedure;
    // it is intentionally NOT a substitution placeholder because no per-UC
    // variation exists.

    private final UseCaseRegistryService useCaseRegistry;
    private final KnowledgeSearchService knowledgeSearchService;
    private final ScriptLibraryService scriptLibrary;
    private final LlmInvocationService llmInvocation;
    private final ContextProjectionBuilder contextProjection;
    private final ActionParser actionParser;
    private final ObjectMapper objectMapper;
    private final CreateCaseControlledTool createCaseTool;
    private final EventEmitter eventEmitter;
    private final ToolDispatcher toolDispatcher;
    private final SkillRegistry skillRegistry;
    private final SkillStateBus skillStateBus;

    public PhaseEvaluator(UseCaseRegistryService useCaseRegistry,
                          KnowledgeSearchService knowledgeSearchService,
                          ScriptLibraryService scriptLibrary,
                          LlmInvocationService llmInvocation,
                          ContextProjectionBuilder contextProjection,
                          ActionParser actionParser,
                          ObjectMapper objectMapper,
                          CreateCaseControlledTool createCaseTool,
                          EventEmitter eventEmitter,
                          ToolDispatcher toolDispatcher,
                          SkillRegistry skillRegistry,
                          SkillStateBus skillStateBus) {
        this.useCaseRegistry = useCaseRegistry;
        this.knowledgeSearchService = knowledgeSearchService;
        this.scriptLibrary = scriptLibrary;
        this.llmInvocation = llmInvocation;
        this.contextProjection = contextProjection;
        this.actionParser = actionParser;
        this.objectMapper = objectMapper;
        this.createCaseTool = createCaseTool;
        this.eventEmitter = eventEmitter;
        this.toolDispatcher = toolDispatcher;
        this.skillRegistry = skillRegistry;
        this.skillStateBus = skillStateBus;
    }

    // ---------------- tool_calls / user_message helpers ----------------

    /** True when the parsed response contains a tool_call with the given name. */
    static boolean hasToolCall(ParsedAction action, String toolName) {
        return action != null
                && action.getToolCalls() != null
                && action.getToolCalls().stream().anyMatch(tc -> toolName.equals(tc.getName()));
    }

    /** True when the only tool_call in the response is {@code record_outcome}. */
    static boolean hasOnlyRecordOutcome(ParsedAction action) {
        return action != null
                && action.getToolCalls() != null
                && action.getToolCalls().size() == 1
                && "record_outcome".equals(action.getToolCalls().get(0).getName());
    }

    /** True when the LLM requested a knowledge fetch (search_knowledge or resolve_article). */
    static boolean hasKnowledgeFetch(ParsedAction action) {
        return hasToolCall(action, "search_knowledge") || hasToolCall(action, "resolve_article");
    }

    /** True when the LLM requested a handover. */
    static boolean hasHandover(ParsedAction action) {
        return hasToolCall(action, "request_handover");
    }

    /**
     * Heuristic clarification detection: empty tool_calls + non-empty user_message
     * that ends with '?' or contains a clarifying phrase.
     * Used for clarification budget tracking and to derive a repetition key for
     * loop detection.
     */
    static boolean isClarificationTurn(ParsedAction action) {
        if (action == null) return false;
        boolean noTools = action.getToolCalls() == null || action.getToolCalls().isEmpty();
        if (!noTools) return false;
        String msg = action.getUserMessage();
        if (msg == null || msg.isBlank()) return false;
        return msg.trim().endsWith("?") || containsClarifyingPhrase(msg);
    }

    private static boolean containsClarifyingPhrase(String msg) {
        String lower = msg.toLowerCase(Locale.ROOT);
        return lower.contains("could you")
                || lower.contains("can you tell")
                || lower.contains("what is")
                || lower.contains("which")
                || lower.contains("do you have");
    }

    /**
     * True when the LLM produced a direct answer: empty tool_calls + non-empty
     * user_message that is not a clarifying question.
     */
    static boolean isDirectAnswerTurn(ParsedAction action) {
        if (action == null) return false;
        boolean noTools = action.getToolCalls() == null || action.getToolCalls().isEmpty();
        if (!noTools) return false;
        String msg = action.getUserMessage();
        if (msg == null || msg.isBlank()) return false;
        return !isClarificationTurn(action);
    }

    /**
     * Evaluate the current phase and produce a PhaseResult with the action to take
     * and the next phase to transition to.
     */
    public PhaseResult evaluate(BotSession session, String userMessage,
                                 List<BotTurn> conversationHistory) {
        String phase = session.getCurrentPhase();
        log.debug("Session {}: evaluating phase {}", session.getSessionId(), phase);

        return switch (phase) {
            case "DISCOVER" -> evaluateDiscover(session, userMessage, conversationHistory);
            case "RESOLVE" -> evaluateResolve(session, userMessage, conversationHistory);
            case "CONFIRM" -> evaluateConfirm(session, userMessage, conversationHistory);
            case "CLOSE" -> evaluateClose(session);
            case "ESCALATE" -> evaluateEscalate(session, conversationHistory);
            default -> {
                log.warn("Session {}: unexpected phase '{}'", session.getSessionId(), phase);
                yield PhaseResult.escalate(session,
                        "I'm having trouble processing your request. Let me connect you with a human agent.",
                        "unexpected_phase");
            }
        };
    }

    /**
     * D16 planner entry point. Returns a {@link PhasePlan} describing the
     * mission for the current phase, or {@code null} to signal the caller
     * (typically {@code ControlKernel}) to fall back to the legacy
     * {@link #evaluate} path.
     *
     * <p>D16.B added the RESOLVE/FAQ branch, D16.C added INTAKE, and D16.D
     * adds DISCOVER / CONFIRM / CLOSE / ESCALATE. After D16.D this method
     * returns a non-null plan for every (phase, UC) combination it
     * encounters; a {@code null} return value signals the legacy fallback
     * for unexpected inputs only.
     *
     * @param session     the current bot session
     * @param userMessage the inbound user message
     * @param history     prior turns in this session
     * @return a phase plan, or {@code null} to use the legacy path
     */
    public PhasePlan plan(BotSession session, String userMessage, List<BotTurn> history) {
        if (session == null) return null;
        String phase = session.getCurrentPhase();
        String activeUc = session.getActiveUseCase();

        // Sprint 39 (NEW M2): SkillRegistry-driven composition for ALL 6
        // phases per Sprint 37 freeze decisions (e §6.2.5 + §6.2.6) and
        // (c §4.1). The 4 simpler Sprint 38 Skills (DISCOVER + CONFIRM +
        // CLOSE + ESCALATE) carry guardrails: [] and no template
        // placeholders; the 2 Sprint 39 RESOLVE Skills (FAQ + INTAKE) carry
        // guardrails enforced by SkillGuardrailDispatcher and template
        // placeholders substituted by composeSkillPhasePlan(...).
        if (skillRegistry != null) {
            Optional<Skill> selected = skillRegistry.select(phase, activeUc);
            if (selected.isPresent()) {
                Skill newSkill = selected.get();
                // Sprint 41 — SkillStateBus integration per design doc §10.3.
                // Detect a Skill switch and invoke the bus once at the boundary:
                //   (a) prior turn's active_use_case is non-null AND differs
                //       from the current session.activeUseCase, AND
                //   (b) the resolved Skill for the prior (phase_after, prior_uc)
                //       differs by name from newSkill.
                // Detection rides on existing M1 surfaces (BotTurn.activeUseCase
                // + BotTurn.phaseAfter); no new classifier introduced per
                // M2 §6 #5 fence.
                maybeApplyStateBusOnSwitch(history, session, newSkill);
                return composeSkillPhasePlan(newSkill, phase, activeUc);
            }
        }
        // Unknown phase OR unmapped (phase, useCase) tuple — fall back to
        // null (the kernel handles null plans defensively). Pre-Sprint-39
        // this fall-through carried the legacy RESOLVE-INTAKE and
        // RESOLVE-FAQ branches; both are now externalized to
        // server/src/main/resources/skills/resolve_*.yaml.
        return null;
    }

    /**
     * Sprint 41 — invoke {@link SkillStateBus#applyOnSkillSwitch} at the
     * Skill-switch boundary per design doc §10.3. Skill-switch is detected
     * when (a) the prior persisted turn carries a non-null
     * {@code active_use_case} that differs from the current session's
     * active UC, AND (b) the Skill resolved for the prior
     * {@code (phase_after, prior_uc)} differs by name from {@code newSkill}.
     *
     * <p>Detection rides on existing M1 surfaces (BotTurn.activeUseCase
     * + BotTurn.phaseAfter); no new classifier introduced per M2 §6 #5
     * fence. Per §1.7: this method has NO per-UC-pair branch — the bus
     * itself applies the new Skill's {@code state_inheritance} declaration
     * verbatim.
     *
     * <p>Defensive: no-op when {@code skillStateBus} is null (test
     * harness fallback), when {@code history} is empty, when the prior
     * turn has no committed {@code active_use_case}, OR when the prior
     * Skill resolves to the same Skill as {@code newSkill}.
     */
    private void maybeApplyStateBusOnSwitch(List<BotTurn> history,
                                             BotSession session,
                                             Skill newSkill) {
        if (skillStateBus == null || history == null || history.isEmpty()
                || session == null || newSkill == null) {
            return;
        }
        BotTurn priorTurn = history.get(history.size() - 1);
        if (priorTurn == null) return;
        String priorUc = priorTurn.getActiveUseCase();
        String activeUc = session.getActiveUseCase();
        if (priorUc == null || priorUc.isBlank()) return;
        if (Objects.equals(priorUc, activeUc)) return;
        String priorPhase = priorTurn.getPhaseAfter();
        if (priorPhase == null || priorPhase.isBlank()) return;
        Skill priorSkill = skillRegistry.select(priorPhase, priorUc).orElse(null);
        if (priorSkill == null) return;
        if (Objects.equals(priorSkill.name(), newSkill.name())) return;
        skillStateBus.applyOnSkillSwitch(priorSkill, newSkill, session);
    }

    /**
     * Compose a {@link Skill} with session state into a {@link PhasePlan} per
     * Sprint 37 freeze §4.1 decision (c). The output is observationally
     * identical to the pre-migration hardcoded per-phase branch for the
     * representative UCs covered by the Skill (verified by
     * {@code PhaseEvaluatorSkillIntegrationTest} for Sprint 38's 4 simpler
     * phases and {@code PhaseEvaluatorResolveSkillIntegrationTest} for
     * Sprint 39's 2 RESOLVE phases).
     *
     * <p>Sprint 39: applies template substitution per design doc §6.2.5 /
     * §6.2.6 to the RESOLVE Skills' text fields. Each placeholder is a
     * SINGLE registry / map lookup keyed by the active UC — there is NO
     * per-UC-pair branch logic in the substitution body per §1.7 + M2 §6 #1.
     * Skills that do not contain a placeholder (Sprint 38's 4 simpler phase
     * Skills) pass their text fields through unchanged.
     */
    private PhasePlan composeSkillPhasePlan(Skill skill, String phase, String activeUc) {
        Set<TerminalOutcome> outcomes = skill.validTerminalOutcomes().stream()
                .map(TerminalOutcome::valueOf)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        int maxToolSteps = skill.maxToolSteps() != null ? skill.maxToolSteps() : 2;

        return PhasePlan.builder()
                .phase(phase)
                .useCase(activeUc) // may be null in DISCOVER while still discovering
                .objective(substitutePlaceholders(skill.objective(), activeUc))
                .allowedTools(skill.toolsRequired())
                .requiredContextKeys(new LinkedHashSet<>(skill.requiredContextKeys()))
                .maxToolSteps(maxToolSteps)
                .allowInterimMessage(skill.allowInterimMessage())
                .validTerminalOutcomes(outcomes)
                .systemInstruction(substitutePlaceholders(skill.procedure(), activeUc))
                .groundingInstruction(substitutePlaceholders(skill.groundingInstruction(), activeUc))
                .escalationPolicy(substitutePlaceholders(skill.escalationPolicy(), activeUc))
                .build();
    }

    /**
     * Substitute Sprint 39 RESOLVE-Skill template placeholders per Sprint 37
     * freeze §6.2.5 / §6.2.6. Six placeholders, each filled by a SINGLE
     * registry / map lookup keyed by the active UC; no per-UC-pair branch
     * logic per §1.7 + M2 §6 #1. Skills that do not contain a placeholder
     * pass their text through unchanged.
     *
     * <ul>
     *   <li>{@code {uc_name}} — display name from {@link UseCaseRegistryService}.</li>
     *   <li>{@code {uc_id}} — the active UC itself.</li>
     *   <li>{@code {team_name}} — {@link #UC_TEAM_NAME} map (defaults to
     *       {@code "specialist"}).</li>
     *   <li>{@code {intake_complete_trigger}} — canonical
     *       {@code intake_complete_for_uc_X} via {@link #intakeCompleteTrigger(String)}.</li>
     *   <li>{@code {case_creation_note}} — case-creation appendix for UC-H/J/K
     *       (M1 {@code ControlKernel.createCaseIfNeeded} runtime side effect),
     *       empty for UC-G/I and non-intake UCs.</li>
     *   <li>{@code {intake_required_fields}} —
     *       {@link IntakeFieldsRegistry#requiredFieldsFor(String)} list rendered
     *       via Java {@code List.toString()}. Sixth placeholder (beyond the
     *       five named in Sprint 39 contract §2.2 D-c) preserves byte-for-byte
     *       equivalence with the legacy
     *       {@code buildIntakeSystemInstruction(...)} output; flagged in
     *       handoff §7 OQ for deliver-agent + human review at Sprint 39 close.</li>
     * </ul>
     */
    private String substitutePlaceholders(String text, String activeUc) {
        if (text == null) return null;
        if (activeUc == null || activeUc.isBlank()) return text;

        String ucName = activeUc;
        try {
            UseCaseRegistryService.UseCaseDefinition ucDef = useCaseRegistry.getUseCase(activeUc);
            if (ucDef != null && ucDef.name() != null) {
                ucName = ucDef.name();
            }
        } catch (Exception ex) {
            // Defensive: fall back to the raw UC id if the registry rejects.
        }
        String teamName = UC_TEAM_NAME.getOrDefault(activeUc, "specialist");
        String trigger = intakeCompleteTrigger(activeUc);
        boolean needsCase = Set.of("UC-H", "UC-J", "UC-K").contains(activeUc);
        String caseNote = needsCase
                ? "A tracking case will be created automatically by the runtime when you escalate; "
                        + "you do not need to call any case-creation tool yourself."
                : "";
        String requiredFields = IntakeFieldsRegistry.requiredFieldsFor(activeUc).toString();

        return text
                .replace("{uc_name}", ucName)
                .replace("{uc_id}", activeUc)
                .replace("{team_name}", teamName)
                .replace("{intake_complete_trigger}", trigger)
                .replace("{case_creation_note}", caseNote)
                .replace("{intake_required_fields}", requiredFields);
    }

    /**
     * D16 post-loop interpreter. Given a {@link PhasePlan} and the
     * {@link AgentRunResult} produced by the {@code AgentRunLoop}, decides
     * what phase transition (if any) to apply and what response text to send
     * back to the user.
     *
     * <p>D16.D extends the mapping to be phase-aware so DISCOVER, CONFIRM,
     * CLOSE, and ESCALATE produce the right downstream transitions instead
     * of always assuming RESOLVE→CONFIRM.
     */
    public PhaseTransitionDecision interpretRunResult(PhasePlan plan,
                                                       AgentRunResult result,
                                                       BotSession session) {
        if (result == null) {
            return new PhaseTransitionDecision("ESCALATE",
                    "I'm experiencing a technical issue. Let me connect you with a specialist.",
                    "service_degraded", "agent_error");
        }
        TerminalOutcome outcome = result.terminalOutcome();
        if (outcome == null) {
            return new PhaseTransitionDecision("ESCALATE",
                    "I'm experiencing a technical issue. Let me connect you with a specialist.",
                    "service_degraded", "agent_error");
        }
        // Step 3b: reset the per-session runtime error counter on any non-ERROR
        // outcome so the retry threshold (≥2) only applies to *consecutive*
        // errors within the same phase, not errors interleaved with success.
        if (session != null && outcome != TerminalOutcome.ERROR
                && session.getRuntimeErrorCount() != null
                && session.getRuntimeErrorCount() > 0) {
            session.setRuntimeErrorCount(0);
        }
        // Sprint 24 Track A: same shape, applied to the cross-turn slow-LLM
        // placeholder loop. The DEADLINE_EXCEEDED branch's distinct
        // honest-next-step message fires only on the *second* consecutive
        // DEADLINE_EXCEEDED outcome; any non-deadline outcome (FINAL_ANSWER /
        // CLARIFICATION_NEEDED / etc.) resets the counter so a later isolated
        // deadline still gets the existing placeholder rather than the
        // honest-next-step text.
        if (session != null && outcome != TerminalOutcome.DEADLINE_EXCEEDED
                && session.getConsecutiveDeadlineCount() != null
                && session.getConsecutiveDeadlineCount() > 0) {
            session.setConsecutiveDeadlineCount(0);
        }
        // For INTAKE plans, FINAL_ANSWER means the LLM produced a no-tool-call
        // user_message — which in intake mode is a clarification question, not
        // a customer-facing final answer. Keep the session in RESOLVE so the
        // user can supply the missing details. INTAKE plans terminate via
        // ESCALATE only.
        // WS-3 / A3: intake-ONLY. A PARTIAL-path plan (UC-K) may legitimately
        // terminate on a FINAL_ANSWER — that is the whole point of the path —
        // so it must not be forced back into RESOLVE-as-clarification.
        boolean isIntakePlan = plan != null && plan.useCase() != null
                && isIntakeOnlyUseCase(plan.useCase());

        String fromPhase = plan == null ? null : plan.phase();

        // Sprint 093 / S-Auto-39 — structural precondition for the
        // RESOLVE→CONFIRM promotion: a grounded FINAL_ANSWER was delivered on
        // a prior turn that stayed in RESOLVE. The kernel derives this from
        // the persisted turn history (via priorGroundedResolveAnswerDelivered)
        // and stashes it on the session transient before this call; no
        // user-message content is inspected.
        boolean priorGroundedResolveAnswer = session != null
                && Boolean.TRUE.equals(session.getPriorGroundedResolveAnswer());

        switch (outcome) {
            case FINAL_ANSWER:
                return mapFinalAnswer(plan, result, session, fromPhase, isIntakePlan,
                        priorGroundedResolveAnswer);
            case CLARIFICATION_NEEDED:
                // Stay in current phase for clarification (RESOLVE for legacy callers
                // when plan is null).
                String stayPhase = fromPhase != null ? fromPhase : "RESOLVE";
                return new PhaseTransitionDecision(stayPhase,
                        result.finalUserMessage(),
                        null, "clarification_asked");
            case ESCALATE: {
                String msg = result.finalUserMessage() != null
                        ? result.finalUserMessage()
                        : "Let me connect you with a specialist.";
                String reason = canonicalize(
                        result.escalationReason().orElse("service_degraded"));
                return new PhaseTransitionDecision("ESCALATE", msg, reason, "agent_escalated");
            }
            case MAX_STEPS: {
                // Codex 1.9: ``turn_budget_exhausted`` masks the more specific
                // semantic reason. When the agent loop ran out of steps it is
                // almost always one of:
                //   - FAQ plans where retrieval did not return a usable answer
                //     after multiple search_knowledge attempts → faq_miss_threshold_exceeded
                //   - DISCOVER / RESOLVE plans where the agent kept asking the
                //     user clarifying questions but never converged → clarification_budget_exhausted
                //   - INTAKE plans where required fields could not be collected
                //     → incomplete_intake
                // Fall back to ``turn_budget_exhausted`` only when none of the
                // above apply.
                String maxStepsReason = resolveMaxStepsReason(plan, result, session);
                return new PhaseTransitionDecision("ESCALATE",
                        "I'm having difficulty resolving this. Let me connect you with a specialist.",
                        maxStepsReason,
                        "max_steps_exceeded");
            }
            case ERROR: {
                // Step 3b: tolerate transient runtime errors. Only escalate on
                // the *second* consecutive ERROR within the same phase; the
                // first one stays in the current phase with a brief retry
                // message so the next user turn re-runs the agent loop. The
                // counter is reset above on any non-ERROR outcome.
                int errCount = (session != null && session.getRuntimeErrorCount() != null)
                        ? session.getRuntimeErrorCount() : 0;
                errCount += 1;
                if (session != null) {
                    session.setRuntimeErrorCount(errCount);
                }
                if (errCount < 2) {
                    String retryPhase = fromPhase != null ? fromPhase : "RESOLVE";
                    String retryMsg = result.finalUserMessage() != null
                            ? result.finalUserMessage()
                            : "Let me try that again.";
                    return new PhaseTransitionDecision(retryPhase, retryMsg, null,
                            "agent_error_retry");
                }
                return new PhaseTransitionDecision("ESCALATE",
                        "I'm experiencing repeated technical issues. Let me connect you with a specialist.",
                        "runtime_error_threshold",
                        "agent_error");
            }
            case DEADLINE_EXCEEDED: {
                // Sprint 8.1 §M2: an honest slow response. We do NOT escalate
                // — escalation here would be a fake business handover that
                // hides the real infra failure. The session stays in the
                // current phase with shouldEndChat=false so the user can
                // retry on their next message. The K0 fallback gate detects
                // this terminal outcome too and refuses to stamp a synthetic
                // UC.
                //
                // Sprint 24 Track A: cross-turn placeholder coalesce. Two
                // consecutive DEADLINE_EXCEEDED outcomes used to emit
                // byte-identical placeholder text on every deadline turn,
                // which reads as a stuck loop and which the runtime
                // loop-detector flags. Mirror the V12 runtime_error_count
                // shape: increment a per-session counter (reset above on any
                // non-deadline outcome); on the first consecutive deadline
                // emit the existing placeholder; on the second emit a
                // distinct honest next-step message naming the slowness and
                // offering an actionable next step. Trigger is the
                // event-shape (count of consecutive DEADLINE_EXCEEDED
                // outcomes), NOT anything in the user's content. The
                // transition tag remains agent_deadline_exceeded for both
                // branches; the bot does NOT auto-call request_handover.
                int deadlineCount = (session != null
                        && session.getConsecutiveDeadlineCount() != null)
                        ? session.getConsecutiveDeadlineCount() : 0;
                deadlineCount += 1;
                if (session != null) {
                    session.setConsecutiveDeadlineCount(deadlineCount);
                }
                String slowStayPhase = fromPhase != null ? fromPhase : "DISCOVER";
                String deadlineMsg = (deadlineCount < 2)
                        ? "Sorry, I'm a bit slow right now. Please try sending that again in a moment."
                        : "I'm still having trouble responding in time. "
                                + "If you'd like, I can connect you with a specialist, "
                                + "or you can try again in a few minutes.";
                return new PhaseTransitionDecision(slowStayPhase, deadlineMsg, null,
                        "agent_deadline_exceeded");
            }
            case LLM_UNAVAILABLE: {
                // Sprint 8.1 §M2: an honest unavailable response. Same
                // posture as DEADLINE_EXCEEDED above (no fake handover, stay
                // in current phase). Sprint 24 Track A is scoped to
                // DEADLINE_EXCEEDED only; LLM_UNAVAILABLE is intentionally
                // left unchanged (a possible future coalesce here belongs in
                // its own R-item).
                String slowStayPhase = fromPhase != null ? fromPhase : "DISCOVER";
                String userMsg = "Sorry, I'm having trouble reaching the assistant right now. "
                        + "Please try again in a moment.";
                return new PhaseTransitionDecision(slowStayPhase, userMsg, null,
                        "agent_llm_unavailable");
            }
            case USE_CASE_IDENTIFIED: {
                // Sprint 8.1 §M3: deterministic DISCOVER → RESOLVE phase
                // boundary. The AgentRunLoop returned this outcome
                // immediately after a successful classify_use_case call
                // committed session.activeUseCase. There is no escalation
                // reason and no synthetic request_handover; the same-turn
                // RESOLVE replan in {@link ControlKernel} owns the actual
                // user-facing reply. We surface a transitional placeholder
                // here purely as a defensive default — ControlKernel
                // discards this text once the RESOLVE replan runs.
                return new PhaseTransitionDecision("RESOLVE",
                        "I'm looking into this for you.",
                        null, "uc_identified");
            }
            case USE_CASE_REROUTED: {
                // Sprint 103 / WS-6-A: mid-session re-route. `propose_reroute`
                // moved session.activeUseCase and the loop returned
                // immediately. RESOLVE is the landing phase — reachable from
                // RESOLVE (no transition) and from CONFIRM (an edge
                // control-policy.yaml already declares), so no new edge is
                // introduced. Same shape as USE_CASE_IDENTIFIED above: the
                // text here is a defensive placeholder that ControlKernel
                // discards once the same-turn replan produces the real reply.
                return new PhaseTransitionDecision("RESOLVE",
                        "Let me look into that for you.",
                        null, "uc_rerouted");
            }
            default:
                throw new IllegalStateException("Unknown terminal outcome: " + outcome);
        }
    }

    /**
     * Sprint 9 §O1 — terminal-state honesty: true when the agent run
     * attempted at least one {@code record_outcome} dispatch and EVERY
     * such attempt failed (no successful one). Used by
     * {@link #mapFinalAnswer} to keep the session in RESOLVE so the LLM
     * can retry on the next user turn. Returns false when no
     * {@code record_outcome} attempt was made (the LLM may have answered
     * directly without persisting an outcome — that path is unchanged).
     */
    static boolean recordOutcomeAttemptedAndFailed(AgentRunResult result) {
        if (result == null || result.toolEvents() == null) return false;
        boolean attempted = false;
        boolean anySuccess = false;
        for (ToolEvent te : result.toolEvents()) {
            if ("record_outcome".equals(te.toolName())) {
                attempted = true;
                if (te.success()) {
                    anySuccess = true;
                    break;
                }
            }
        }
        return attempted && !anySuccess;
    }

    /**
     * Sprint 093 / S-Auto-39 — true when the agent run attempted
     * {@code record_outcome}, none succeeded, AND every failed attempt was
     * rejected by the premature-resolve guard
     * ({@code progressive_resolve_record_outcome_premature}). This isolates a
     * STRUCTURAL guard rejection — which can never succeed while the phase is
     * RESOLVE, so retrying in RESOLVE loops forever — from a GENUINE /
     * transient {@code record_outcome} failure (e.g. a persistence error),
     * which the Sprint 9 §O1 {@code record_outcome_failed_retry} path must
     * still keep in RESOLVE. Returns false when no {@code record_outcome} was
     * attempted, when one succeeded, or when any failure carried a
     * non-premature error message.
     */
    static boolean recordOutcomeRejectedOnlyByPrematureGuard(AgentRunResult result) {
        if (result == null || result.toolEvents() == null) return false;
        boolean attempted = false;
        for (ToolEvent te : result.toolEvents()) {
            if (!"record_outcome".equals(te.toolName())) continue;
            attempted = true;
            if (te.success()) return false;
            if (!SkillGuardrailDispatcher.PROGRESSIVE_RESOLVE_REJECT_REASON
                    .equals(te.errorMessage())) {
                return false;
            }
        }
        return attempted;
    }

    /**
     * Sprint 093 / S-Auto-39 — true when a grounded FINAL_ANSWER was delivered
     * on a PRIOR turn that stayed in RESOLVE. Structural signal derived purely
     * from the durable turn record: a persisted {@link BotTurn} with
     * {@code phase_after == RESOLVE} and non-empty {@code source_ids} is a
     * grounded RESOLVE answer that did not (yet) advance the phase. The
     * {@code phase_after == RESOLVE} filter excludes DISCOVER turns that merely
     * ran {@code search_knowledge} while clarifying (those persist
     * {@code phase_after == DISCOVER}), so prior RETRIEVAL alone does not
     * satisfy the precondition — only a prior delivered grounded ANSWER does.
     * No user-message content is inspected.
     */
    static boolean priorGroundedResolveAnswerDelivered(List<BotTurn> history) {
        if (history == null) return false;
        for (BotTurn t : history) {
            if (t == null) continue;
            if (!"RESOLVE".equalsIgnoreCase(t.getPhaseAfter())) continue;
            String[] sids = t.getSourceIds();
            if (sids != null && sids.length > 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * Phase-aware mapping for {@link TerminalOutcome#FINAL_ANSWER}. RESOLVE/FAQ
     * → CONFIRM, RESOLVE/INTAKE → stay in RESOLVE (clarification), DISCOVER →
     * RESOLVE if a UC has been committed (otherwise stay in DISCOVER), CONFIRM
     * → CLOSE (user satisfied), CLOSE → CLOSE, ESCALATE → ESCALATE.
     */
    private PhaseTransitionDecision mapFinalAnswer(PhasePlan plan,
                                                    AgentRunResult result,
                                                    BotSession session,
                                                    String fromPhase,
                                                    boolean isIntakePlan,
                                                    boolean priorGroundedResolveAnswer) {
        // Legacy callers (plan == null) fall back to the pre-D16.D contract:
        // RESOLVE → CONFIRM with answer_provided.
        if (fromPhase == null) {
            return new PhaseTransitionDecision("CONFIRM",
                    result.finalUserMessage(),
                    null, "answer_provided");
        }

        switch (fromPhase) {
            case "DISCOVER": {
                String activeUc = session == null ? null : session.getActiveUseCase();
                if (activeUc != null && !activeUc.isBlank()) {
                    return new PhaseTransitionDecision("RESOLVE",
                            result.finalUserMessage(),
                            null, "uc_identified");
                }
                // No UC yet — stay in DISCOVER as clarification.
                return new PhaseTransitionDecision("DISCOVER",
                        result.finalUserMessage(),
                        null, "clarification_asked");
            }
            case "RESOLVE": {
                if (isIntakePlan) {
                    return new PhaseTransitionDecision("RESOLVE",
                            result.finalUserMessage(),
                            null, "clarification_asked");
                }
                // Sprint 9 §O1 — terminal-state honesty. Do not advance
                // RESOLVE → CONFIRM when the only record_outcome
                // dispatch attempted in this run failed. CONFIRM signals
                // "the user accepted the answer and the outcome was
                // recorded"; staying in RESOLVE lets the LLM retry the
                // record_outcome call within the next user turn rather
                // than silently marking the session as resolved while
                // the session_outcomes row was never written.
                //
                // Sprint 093 / S-Auto-39 — break the RESOLVE→CONFIRM
                // deadlock. A record_outcome(resolve) rejected ONLY by the
                // premature-resolve guard is NOT a transient failure:
                // retrying in RESOLVE hits the same guard forever
                // (READY_TO_CONFIRM requires a SUCCESSFUL record_outcome,
                // which the guard forbids outside CONFIRM/CLOSE). When a
                // grounded answer was ALREADY delivered on a prior RESOLVE
                // turn (priorGroundedResolveAnswer — i.e. a subsequent user
                // turn has now occurred), exclude that case from the retry
                // gate and let the structural promotion below route the bot
                // into CONFIRM, where record_outcome is already permitted.
                // First-grounded-answer turns (no prior grounding) and
                // GENUINE record_outcome failures keep the unchanged retry
                // behaviour. The premature guard itself is PRESERVED.
                boolean prematureGuardOnlyRejection =
                        recordOutcomeRejectedOnlyByPrematureGuard(result);
                if (recordOutcomeAttemptedAndFailed(result)
                        && !(prematureGuardOnlyRejection && priorGroundedResolveAnswer)) {
                    return new PhaseTransitionDecision("RESOLVE",
                            result.finalUserMessage(),
                            null, "record_outcome_failed_retry");
                }
                // Sprint 11 §M1 — ResolveDisposition guard. A single
                // grounded answer that asks for a slot or delivers a
                // soft next step ("here is how to find your ad; send the
                // advert ID if you want me to check it") must remain in
                // RESOLVE. Only a deterministic terminal condition
                // (successful record_outcome dispatch on this turn)
                // earns the RESOLVE → CONFIRM transition. The bot's
                // own clarifying-question heuristics already gate
                // CLARIFICATION_NEEDED upstream; this branch handles the
                // FINAL_ANSWER case where the bot text shape implies a
                // follow-up is expected.
                com.gumtree.csagent.model.ResolveDisposition disposition =
                        ResolveDispositionEvaluator.evaluate(plan, result);
                if (session != null) {
                    session.setTaskStatus(disposition.toTaskStatusToken());
                    // Sprint 12 §N0 — stamp the canonical
                    // resolve_disposition token so the projection / trace
                    // events can surface the disposition independently of
                    // task_status (which is the Sprint 11 §M0 alias).
                    session.setResolveDisposition(disposition.name());
                }
                PhaseTransitionDecision dispositionDecision = switch (disposition) {
                    case ASKED_FOR_SLOT,
                         CONTINUE_RESOLVE -> new PhaseTransitionDecision("RESOLVE",
                                result.finalUserMessage(),
                                null, "progressive_resolve_stay");
                    // Sprint 093 / S-Auto-39 — PRIMARY structural repair. A
                    // confident grounded non-slot answer (ANSWERED_SUBTASK)
                    // delivered on a SUBSEQUENT user turn after a grounded
                    // FINAL_ANSWER already formed on a prior RESOLVE turn is
                    // confirmable: enter CONFIRM so the CONFIRM Skill can
                    // record the outcome (record_outcome is permitted there).
                    // The trigger is purely structural (prior grounded
                    // RESOLVE answer + this turn's disposition) — no
                    // user-message content heuristic — and the premature
                    // guard is untouched: record_outcome still cannot land
                    // until phase == CONFIRM. The FIRST grounded answer turn
                    // (no prior grounding) stays in RESOLVE so the original
                    // premature-collapse protection holds.
                    case ANSWERED_SUBTASK -> priorGroundedResolveAnswer
                            ? new PhaseTransitionDecision("CONFIRM",
                                result.finalUserMessage(),
                                null, "progressive_resolve_confirmable")
                            : new PhaseTransitionDecision("RESOLVE",
                                result.finalUserMessage(),
                                null, "progressive_resolve_stay");
                    case ESCALATE -> new PhaseTransitionDecision("ESCALATE",
                                result.finalUserMessage(),
                                "service_degraded", "agent_escalated");
                    case READY_TO_CONFIRM -> new PhaseTransitionDecision("CONFIRM",
                                result.finalUserMessage(),
                                null, "answer_provided");
                };
                if (session != null) {
                    session.setPhaseTransitionReason(dispositionDecision.transitionReason());
                }
                return dispositionDecision;
            }
            case "CONFIRM":
                // FINAL_ANSWER from CONFIRM means the user is satisfied; the LLM
                // should already have called record_outcome.
                return new PhaseTransitionDecision("CLOSE",
                        result.finalUserMessage(),
                        null, "user_satisfied");
            case "CLOSE":
                return new PhaseTransitionDecision("CLOSE",
                        result.finalUserMessage(),
                        null, "session_closed");
            case "ESCALATE":
                return new PhaseTransitionDecision("ESCALATE",
                        result.finalUserMessage(),
                        null, "handover_completed");
            default:
                return new PhaseTransitionDecision(fromPhase,
                        result.finalUserMessage(),
                        null, "answer_provided");
        }
    }

    private PhaseResult evaluateDiscover(BotSession session, String userMessage,
                                          List<BotTurn> conversationHistory) {
        String activeUc = session.getActiveUseCase();

        // If UC already identified, move to RESOLVE
        if (activeUc != null && !activeUc.isBlank()) {
            UseCaseRegistryService.UseCaseDefinition ucDef = useCaseRegistry.getUseCase(activeUc);
            if (ucDef != null) {
                log.info("Session {}: UC {} identified, transitioning to RESOLVE", session.getSessionId(), activeUc);
                return PhaseResult.transition(session, "RESOLVE", null, "use_case_identified");
            }
        }

        // UC not yet identified — use LLM to ask clarifying question
        String projection = contextProjection.buildProjection(session, conversationHistory, null, userMessage);
        LlmResponse llmResponse = llmInvocation.invokeChat(projection, userMessage,
                    session.getSessionId(), session.getTotalBotTurns());
        ParsedAction action = actionParser.parse(llmResponse.getContent());

        // Track clarification: empty tool_calls + clarifying user_message
        if (isClarificationTurn(action)) {
            session.setClarificationCount(session.getClarificationCount() + 1);
            eventEmitter.emitClarificationAsked(session.getSessionId(), session.getTotalBotTurns(),
                    session.getClarificationCount());
        }

        // If LLM determined escalation (e.g. safe fallback during API failure), escalate immediately
        if (hasHandover(action)) {
            return PhaseResult.escalate(session, action.getUserMessage(),
                "llm_determined_escalation");
        }

        return PhaseResult.respond(session, action, llmResponse, null);
    }

    private PhaseResult evaluateResolve(BotSession session, String userMessage,
                                         List<BotTurn> conversationHistory) {
        String activeUc = session.getActiveUseCase();
        UseCaseRegistryService.UseCaseDefinition ucDef = useCaseRegistry.getUseCase(activeUc);

        if (ucDef == null) {
            return PhaseResult.escalate(session,
                    "I'm having trouble identifying how to help you. Let me connect you with a human agent.",
                    "unknown_use_case");
        }

        // Intake-ONLY UCs (registry `path: INTAKE` — UC-G/H/I/J) must NEVER
        // invoke knowledge search; they use fixed script templates +
        // clarifying questions for intake field collection.
        //
        // WS-3 / A3 (2026-07-25): the second disjunct here used to be a
        // literal UC set containing UC-K, which overrode the registry and
        // forced every technical-support question down the no-retrieval path.
        // The registry `path` is now the sole classifier: PARTIAL-path UCs
        // reach resolveFaq and may consult the knowledge surface.
        if (isIntakeOnlyUseCase(activeUc)) {
            return resolveIntake(session, userMessage, conversationHistory, ucDef);
        }

        // FAQ- and PARTIAL-path UCs proceed with knowledge search.
        return resolveFaq(session, userMessage, conversationHistory, ucDef);
    }

    private PhaseResult resolveFaq(BotSession session, String userMessage,
                                    List<BotTurn> conversationHistory,
                                    UseCaseRegistryService.UseCaseDefinition ucDef) {
        // Step 1: Search knowledge base via ToolDispatcher for policy enforcement
        List<String> ucTags = List.of(ucDef.ucId());
        KnowledgeSearchResult searchResult;

        String enrichedQuery = enrichQueryWithFormContext(userMessage, session, conversationHistory);
        ToolResult toolResult = toolDispatcher.dispatch("search_knowledge", session,
                Map.of("query", enrichedQuery, "uc_tags", ucTags));

        if (!toolResult.isSuccess()) {
            log.warn("Session {}: search_knowledge blocked or failed: {}",
                    session.getSessionId(), toolResult.getErrorMessage());
            return PhaseResult.escalate(session,
                    "I need to connect you with a specialist who can help with this.",
                    "tool_scope_blocked");
        }

        searchResult = reconstructSearchResult(toolResult.getData());

        // Emit RETRIEVAL_EXECUTED event
        eventEmitter.emitRetrievalExecuted(session.getSessionId(), session.getTotalBotTurns(),
                enrichedQuery, searchResult.isFaqMiss(),
                searchResult.getHits() != null ? searchResult.getHits().size() : 0);

        // Step 2: Check for FAQ miss
        if (searchResult.isFaqMiss()) {
            session.setFaqMissCount(session.getFaqMissCount() + 1);
            log.info("Session {}: FAQ miss #{}", session.getSessionId(), session.getFaqMissCount());

            // If we've hit the FAQ miss limit, escalate
            // (BudgetChecker will catch this on next turn, but be proactive)
            if (session.getFaqMissCount() >= 2) {
                return PhaseResult.escalate(session,
                        "I wasn't able to find a clear answer to your question. Let me connect you with a specialist who can help.",
                        "faq_miss_exceeded");
            }

            // Check if form context has a substantive description we can use instead of asking user to repeat
            String formDescription = extractFormDescription(session);
            if (formDescription != null && formDescription.length() > 10) {
                try {
                    log.info("Session {}: FAQ miss but form description available, invoking LLM with form context",
                            session.getSessionId());
                    String projection = contextProjection.buildProjection(
                            session, conversationHistory, null, userMessage);
                    // Inject faq_miss_instruction into the projection (tool-use phrasing)
                    com.fasterxml.jackson.databind.node.ObjectNode projNode =
                            (com.fasterxml.jackson.databind.node.ObjectNode) objectMapper.readTree(projection);
                    projNode.put("faq_miss_instruction",
                            "Knowledge search did not find matching articles for this query. " +
                            "However, the user described their issue in the pre-chat form: '" + formDescription + "'. " +
                            "Based on this context and your understanding of the topic, provide a helpful response. " +
                            "Return a non-empty user_message with general guidance, " +
                            "or a clarifying question (ending with '?'), " +
                            "or include a request_handover tool_call if you cannot help.");
                    String modifiedProjection = objectMapper.writeValueAsString(projNode);

                    LlmResponse llmResponse = llmInvocation.invokeChat(modifiedProjection, userMessage,
                            session.getSessionId(), session.getTotalBotTurns());
                    ParsedAction action = actionParser.parse(llmResponse.getContent());

                    return PhaseResult.respond(session, action, llmResponse, null);
                } catch (Exception e) {
                    log.warn("Session {}: LLM fallback on FAQ miss failed, using hardcoded response: {}",
                            session.getSessionId(), e.getMessage());
                    // Fall through to hardcoded response below
                }
            }

            // Ask user to rephrase (no form context available or LLM fallback failed)
            return PhaseResult.respond(session,
                    ParsedAction.builder()
                            .toolCalls(List.of())
                            .userMessage("I couldn't find a specific answer to that. Could you describe your issue in a bit more detail?")
                            .reasoning("FAQ miss - asking for clarification")
                            .build(),
                    null, null);
        }

        // Step 3: Knowledge found — use LLM to generate grounded answer
        String projection = contextProjection.buildProjection(
                session, conversationHistory, searchResult.getHits(), userMessage);
        LlmResponse llmResponse = llmInvocation.invokeChat(projection, userMessage,
                    session.getSessionId(), session.getTotalBotTurns());
        ParsedAction action = actionParser.parse(llmResponse.getContent());

        // Track articles shown
        if (searchResult.getHits() != null && !searchResult.getHits().isEmpty()) {
            String[] sourceIds = searchResult.getHits().stream()
                    .map(KnowledgeHit::getSourceId)
                    .toArray(String[]::new);
            // Merge with existing articles shown
            String[] existing = session.getArticlesShown();
            if (existing != null) {
                String[] merged = new String[existing.length + sourceIds.length];
                System.arraycopy(existing, 0, merged, 0, existing.length);
                System.arraycopy(sourceIds, 0, merged, existing.length, sourceIds.length);
                session.setArticlesShown(merged);
            } else {
                session.setArticlesShown(sourceIds);
            }

            // Emit ARTICLE_SHOWN event for each hit
            for (KnowledgeHit hit : searchResult.getHits()) {
                eventEmitter.emitArticleShown(session.getSessionId(), session.getTotalBotTurns(),
                        hit.getSourceId(), hit.getTitle());
            }
        }

        // If the LLM produced a direct grounded answer or a record_outcome tool_call,
        // transition to CONFIRM. (Both signal "answer delivered" to the customer.)
        if (isDirectAnswerTurn(action) || hasOnlyRecordOutcome(action)) {
            return PhaseResult.transitionWithResponse(session, "CONFIRM", action, llmResponse, "answer_provided", searchResult.getHits());
        }

        // Defense-in-depth: if LLM included `search_knowledge` in tool_calls despite
        // pre-loaded knowledge, re-invoke with override. This adds latency (double LLM
        // call) but prevents the user from seeing an intermediate "Let me check..."
        // message with no follow-up.
        if (hasToolCall(action, "search_knowledge")) {
            log.warn("Session {}: LLM called search_knowledge despite pre-loaded knowledge. "
                    + "Re-invoking with explicit grounding override.", session.getSessionId());

            String retryProjection = contextProjection.buildProjection(
                    session, conversationHistory, searchResult.getHits(), userMessage);
            String groundingOverride = "IMPORTANT: Knowledge articles have already been retrieved "
                    + "and are included in this context under 'knowledge_hits'. "
                    + "Do NOT call search_knowledge again. "
                    + "Compose a helpful user_message grounded in the provided knowledge snippets "
                    + "and cite source_ids from knowledge_hits.";
            LlmResponse retryResponse = llmInvocation.invokeChat(
                    retryProjection, groundingOverride + "\n\nUser question: " + userMessage,
                    session.getSessionId(), session.getTotalBotTurns());
            ParsedAction retryAction = actionParser.parse(retryResponse.getContent());

            if (isDirectAnswerTurn(retryAction) || hasOnlyRecordOutcome(retryAction)) {
                return PhaseResult.transitionWithResponse(session, "CONFIRM", retryAction, retryResponse, "answer_provided_retry", searchResult.getHits());
            }
            // If still not a direct answer, use whatever message the LLM produced
            return PhaseResult.respond(session, retryAction, retryResponse, searchResult.getHits());
        }

        // If LLM determined escalation (e.g. safe fallback during API failure), escalate immediately
        if (hasHandover(action)) {
            return PhaseResult.escalate(session, action.getUserMessage(),
                "llm_determined_escalation");
        }

        return PhaseResult.respond(session, action, llmResponse, searchResult.getHits());
    }

    private PhaseResult resolveIntake(BotSession session, String userMessage,
                                       List<BotTurn> conversationHistory,
                                       UseCaseRegistryService.UseCaseDefinition ucDef) {
        // INTAKE path: use fixed script templates for intake field collection,
        // then escalate. Intake UCs never self-resolve and never call search_knowledge.
        String activeUc = ucDef.ucId();
        String prefix = INTAKE_TEMPLATE_PREFIX.getOrDefault(activeUc, activeUc.toLowerCase().replace("uc-", ""));
        Map<String, String> vars = buildIntakeVariables(session, activeUc);

        // On the first intake turn, send the opening + intake prompt template.
        // Guard: use lastAction — if it's already set, the opening template was already sent.
        // (conversationHistory may be empty because BotTurns are persisted after response)
        if (session.getLastAction() == null) {
            String empathy = null;
            List<String> openingKeys = INTAKE_OPENING_TEMPLATES.getOrDefault(activeUc, List.of());
            for (String key : openingKeys) {
                empathy = scriptLibrary.renderTemplate(key, vars);
                if (empathy != null) break;
            }
            String intakePrompt = scriptLibrary.renderTemplate(prefix + "_intake_prompt", vars);

            String message;
            if (empathy != null && intakePrompt != null) {
                message = empathy + "\n\n" + intakePrompt;
            } else if (intakePrompt != null) {
                message = intakePrompt;
            } else {
                // Fallback: use LLM to generate an intake prompt
                message = "I'd like to help with this. Could you provide some more details so I can pass this to the right team?";
            }

            return PhaseResult.respond(session,
                    ParsedAction.builder()
                            .toolCalls(List.of())
                            .userMessage(message)
                            .reasoning("Intake UC " + activeUc + ": presenting intake template")
                            .build(),
                    null, null);
        }

        // Subsequent turns: use LLM to decide if intake is complete or more info needed.
        // Prepend an intake-specific instruction so the LLM knows NOT to search knowledge
        // and instead focuses on collecting remaining fields or escalating.
        String intakeInstruction = "You are collecting information for a " + UC_TEAM_NAME.getOrDefault(activeUc, "support")
                + " case (use case " + activeUc + "). "
                + "Either ask the user a clarifying question (return a non-empty user_message ending with '?') "
                + "to collect remaining required details, "
                + "OR call request_handover with an appropriate escalation_reason "
                + "when you have enough information to hand over to the team. "
                + "Do NOT call search_knowledge — this is an intake flow, not a FAQ flow. "
                + "Acknowledge what the user provided, then ask for anything still missing or escalate.";

        String projection = contextProjection.buildProjection(session, conversationHistory, null, userMessage);
        LlmResponse llmResponse = llmInvocation.invokeChat(
                projection, intakeInstruction + "\n\nUser message: " + userMessage,
                session.getSessionId(), session.getTotalBotTurns());
        ParsedAction action = actionParser.parse(llmResponse.getContent());

        // If the LLM says escalate (handover) or finish (record_outcome), escalate with intake-complete template
        if (hasHandover(action) || hasOnlyRecordOutcome(action)) {
            // Create case for UC-H/J/K before escalation
            createCaseIfAllowed(session, activeUc);

            // Rebuild vars after case creation (CASE_NUMBER may have changed)
            vars = buildIntakeVariables(session, activeUc);

            String completeTemplate = scriptLibrary.renderTemplate(prefix + "_escalation", vars);
            if (completeTemplate == null) {
                completeTemplate = scriptLibrary.renderTemplate(prefix + "_intake_complete_case_created", vars);
            }
            String msg = completeTemplate != null ? completeTemplate : action.getUserMessage();
            return PhaseResult.escalate(session, msg, intakeCompleteTrigger(activeUc));
        }

        // Defense-in-depth: if LLM called search_knowledge in intake mode OR produced a
        // grounded answer (direct answer turn), it should instead acknowledge what the
        // user gave and ask for remaining intake fields. Re-invoke once with override.
        if (hasToolCall(action, "search_knowledge") || isDirectAnswerTurn(action)) {
            String observed = hasToolCall(action, "search_knowledge")
                    ? "search_knowledge tool_call"
                    : "direct answer (no tool_call)";
            log.warn("Session {}: LLM produced '{}' in intake mode for {}. "
                    + "Re-invoking to collect intake fields.", session.getSessionId(), observed, activeUc);

            String retryInstruction = "IMPORTANT: This is an INTAKE case for " + UC_TEAM_NAME.getOrDefault(activeUc, "support")
                    + ". You CANNOT call search_knowledge or provide direct FAQ-style answers. "
                    + "The user just said: \"" + userMessage + "\". "
                    + "Acknowledge what they provided, then either: "
                    + "(1) ask a clarifying question (non-empty user_message ending with '?') to collect any remaining details needed, or "
                    + "(2) call request_handover with an appropriate escalation_reason if you have enough info to pass to the team.";
            LlmResponse retryResponse = llmInvocation.invokeChat(projection, retryInstruction,
                    session.getSessionId(), session.getTotalBotTurns());
            ParsedAction retryAction = actionParser.parse(retryResponse.getContent());

            if (hasHandover(retryAction) || hasOnlyRecordOutcome(retryAction)) {
                // Create case for UC-H/J/K before escalation
                createCaseIfAllowed(session, activeUc);

                // Rebuild vars after case creation
                vars = buildIntakeVariables(session, activeUc);

                String tmpl = scriptLibrary.renderTemplate(prefix + "_escalation", vars);
                String msg = tmpl != null ? tmpl : retryAction.getUserMessage();
                return PhaseResult.escalate(session, msg, intakeCompleteTrigger(activeUc));
            }
            return PhaseResult.respond(session, retryAction, retryResponse, null);
        }

        // (Unreachable in normal flow — handover & outcome covered above; clarifying
        // question falls through here and is returned via respond.)
        return PhaseResult.respond(session, action, llmResponse, null);
    }

    /**
     * Reconstruct a {@link KnowledgeSearchResult} from the map returned by
     * {@link com.gumtree.csagent.service.tools.SearchKnowledgeTool}.
     * The tool serialises the result as a flat map with keys: faq_miss, retrieval_miss, answer_miss, hits.
     */
    @SuppressWarnings("unchecked")
    private KnowledgeSearchResult reconstructSearchResult(Map<String, Object> data) {
        boolean faqMiss = Boolean.TRUE.equals(data.get("faq_miss"));
        boolean retrievalMiss = Boolean.TRUE.equals(data.get("retrieval_miss"));
        boolean answerMiss = Boolean.TRUE.equals(data.get("answer_miss"));

        List<KnowledgeHit> hits = new ArrayList<>();
        Object rawHits = data.get("hits");
        if (rawHits instanceof List<?> hitList) {
            for (Object item : hitList) {
                if (item instanceof Map<?, ?> hitMap) {
                    Map<String, Object> m = (Map<String, Object>) hitMap;
                    hits.add(KnowledgeHit.builder()
                            .sourceId((String) m.get("source_id"))
                            .title((String) m.get("title"))
                            .snippet((String) m.get("snippet"))
                            .canonicalUrl((String) m.get("canonical_url"))
                            .score(m.get("score") instanceof Number n ? n.doubleValue() : 0.0)
                            .build());
                }
            }
        }

        return KnowledgeSearchResult.builder()
                .faqMiss(faqMiss)
                .retrievalMiss(retrievalMiss)
                .answerMiss(answerMiss)
                .hits(hits)
                .build();
    }

    /**
     * Build a variable map for template substitution from the session context.
     * Resolves {SLA_HOURS}, {first_name}, {CASE_NUMBER}, and {TEAM_NAME}.
     */
    private Map<String, String> buildIntakeVariables(BotSession session, String activeUc) {
        Map<String, String> vars = new LinkedHashMap<>();
        vars.put("SLA_HOURS", DEFAULT_SLA_HOURS);
        vars.put("TEAM_NAME", UC_TEAM_NAME.getOrDefault(activeUc, "Support"));

        // Extract first_name from formContext JSON
        String firstName = extractFormField(session, "first_name");
        vars.put("first_name", firstName != null && !firstName.isBlank() ? firstName : "there");

        // Use case ID as the case number, or generate a placeholder
        String caseNumber = session.getCaseId();
        if (caseNumber == null || caseNumber.isBlank()) {
            caseNumber = "CS-" + session.getSessionId().substring(0, 8).toUpperCase();
        }
        vars.put("CASE_NUMBER", caseNumber);

        return vars;
    }

    /**
     * Extract the description field from formContext.
     */
    private String extractFormDescription(BotSession session) {
        return extractFormField(session, "description");
    }

    /**
     * Extract a field value from the session's formContext JSON.
     */
    private String extractFormField(BotSession session, String fieldName) {
        if (session.getFormContext() == null || session.getFormContext().isBlank()) {
            return null;
        }
        try {
            JsonNode formNode = objectMapper.readTree(session.getFormContext());
            JsonNode field = formNode.get(fieldName);
            return field != null ? field.asText(null) : null;
        } catch (Exception e) {
            log.warn("Session {}: failed to extract '{}' from formContext: {}",
                    session.getSessionId(), fieldName, e.getMessage());
            return null;
        }
    }

    /**
     * Enriches a vague/short user message with form description context.
     * Activates when userMessage is short (&lt; 20 chars) OR it's an early turn (index 0 or 1).
     * This prevents knowledge search from failing when users send vague chat messages
     * like "Pls help" while having provided detailed descriptions in the contact form.
     */
    private String enrichQueryWithFormContext(String userMessage, BotSession session,
                                              List<BotTurn> conversationHistory) {
        if (session.getFormContext() == null || session.getFormContext().isBlank()) {
            return userMessage;
        }

        try {
            JsonNode formNode = objectMapper.readTree(session.getFormContext());
            JsonNode descNode = formNode.get("description");
            if (descNode == null || descNode.asText("").isBlank()) {
                return userMessage;
            }

            String description = descNode.asText("");
            int turnIndex = conversationHistory != null ? conversationHistory.size() : 0;

            // Only enrich when the user message is short or it's an early turn
            if (userMessage.length() >= 20 && turnIndex > 1) {
                return userMessage;
            }

            // Truncate description to 200 chars if longer
            if (description.length() > 200) {
                description = description.substring(0, 200);
            }

            String enrichedQuery = userMessage + " | Context: " + description;
            log.debug("Session {}: enriched search query with form description (turnIndex={}, msgLen={})",
                    session.getSessionId(), turnIndex, userMessage.length());
            return enrichedQuery;
        } catch (Exception e) {
            log.warn("Session {}: failed to enrich query with form context: {}",
                    session.getSessionId(), e.getMessage());
            return userMessage;
        }
    }

    /**
     * Create a case via CreateCaseControlledTool for intake UCs that require it (UC-H, UC-J, UC-K).
     * Stores the case_id in the session. Non-blocking: failures are logged but do not prevent escalation.
     */
    private void createCaseIfAllowed(BotSession session, String activeUc) {
        Set<String> caseCreationUcs = Set.of("UC-H", "UC-J", "UC-K");
        if (!caseCreationUcs.contains(activeUc)) {
            return;
        }

        try {
            Map<String, Object> params = new LinkedHashMap<>();

            // Build case fields from session context
            String email = extractFormField(session, "email");
            String description = extractFormField(session, "description");
            String adId = extractFormField(session, "ad_id");

            // Subject per UC
            String subject = switch (activeUc) {
                case "UC-H" -> "Ad Support - Appeal";
                case "UC-J" -> "Report a Safety Issue";
                case "UC-K" -> "Technical Support Request";
                default -> activeUc + " case";
            };

            params.put("subject", subject);
            if (description != null) params.put("description", description);
            if (email != null) params.put("email", email);
            if (adId != null) params.put("ad_id", adId);

            ToolResult result = createCaseTool.execute(session, params);

            if (result.isSuccess() && result.getData() != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> data = (Map<String, Object>) result.getData();
                String caseId = (String) data.get("case_id");
                if (caseId != null) {
                    session.setCaseId(caseId);
                    log.info("Session {}: case created for {}: caseId={}",
                            session.getSessionId(), activeUc, caseId);

                    // Emit CASE_CREATED event
                    eventEmitter.emitCaseCreated(session.getSessionId(),
                            session.getTotalBotTurns(), caseId, activeUc);
                }
            } else {
                log.warn("Session {}: create_case_controlled failed for {}",
                        session.getSessionId(), activeUc);
            }
        } catch (Exception e) {
            log.warn("Session {}: case creation failed (non-blocking) for {}: {}",
                    session.getSessionId(), activeUc, e.getMessage());
        }
    }

    private PhaseResult evaluateConfirm(BotSession session, String userMessage,
                                         List<BotTurn> conversationHistory) {
        String lower = userMessage.toLowerCase(Locale.ENGLISH).trim();

        // Check for positive confirmation
        if (isPositiveConfirmation(lower)) {
            log.info("Session {}: user confirmed resolution", session.getSessionId());
            return PhaseResult.transition(session, "CLOSE", null, "user_confirmed");
        }

        // Check for negative / "no, that didn't help"
        if (isNegativeConfirmation(lower)) {
            log.info("Session {}: user rejected resolution, retrying RESOLVE", session.getSessionId());
            return PhaseResult.transition(session, "RESOLVE", null, "user_rejected");
        }

        // Check for escalation request
        if (isEscalationRequest(lower)) {
            return PhaseResult.escalate(session,
                    "No problem, let me connect you with a human agent.",
                    "user_requested_escalation");
        }

        // Otherwise, use LLM to interpret the response
        String projection = contextProjection.buildProjection(session, conversationHistory, null, userMessage);
        LlmResponse llmResponse = llmInvocation.invokeChat(projection, userMessage,
                    session.getSessionId(), session.getTotalBotTurns());
        ParsedAction action = actionParser.parse(llmResponse.getContent());

        if (hasOnlyRecordOutcome(action)) {
            return PhaseResult.transition(session, "CLOSE", null, "llm_determined_close");
        }
        if (hasHandover(action)) {
            return PhaseResult.escalate(session, action.getUserMessage(), "llm_determined_escalation");
        }

        return PhaseResult.respond(session, action, llmResponse, null);
    }

    private PhaseResult evaluateClose(BotSession session) {
        session.setHandlingState("CLOSED");
        session.setContainmentOutcome("resolved");

        // Emit SESSION_CLOSED event (OUTCOME_RECORDED is emitted by SessionManager.recordOutcome)
        eventEmitter.emitSessionClosed(session.getSessionId(), "resolved");

        return PhaseResult.close(session,
                "I'm glad I could help! If you have any other questions, feel free to start a new chat. Have a great day!");
    }

    private PhaseResult evaluateEscalate(BotSession session, List<BotTurn> conversationHistory) {
        session.setHandlingState("QUEUE_TO_HUMAN");
        session.setContainmentOutcome("escalated");

        String message = "I'm connecting you with a human agent who can assist you further. " +
                          "A summary of our conversation will be provided to them. Please hold on.";

        return PhaseResult.escalate(session, message, session.getEscalationReason());
    }

    private boolean isPositiveConfirmation(String msg) {
        return msg.matches("(?i)(yes|yeah|yep|yup|sure|that helps|that worked|thanks|thank you|" +
                "great|perfect|awesome|got it|understood|makes sense|helpful|resolved|all good).*");
    }

    private boolean isNegativeConfirmation(String msg) {
        return msg.matches("(?i)(no|nope|nah|didn't help|not helpful|doesn't answer|wrong|incorrect|" +
                "that's not right|not what i asked|try again|different question).*");
    }

    private boolean isEscalationRequest(String msg) {
        return msg.matches("(?i).*(talk to (an?\\s+)?agent|human agent|real person|transfer me|" +
                "speak (to|with) (a\\s+)?(human|person|agent)|connect me|live agent).*");
    }

    /**
     * Result of phase evaluation.
     */
    public record PhaseResult(
            String nextPhase,
            ParsedAction action,
            LlmResponse llmResponse,
            List<KnowledgeHit> knowledgeHits,
            String transitionReason,
            boolean shouldClose,
            boolean shouldEscalate,
            String escalationReason,
            String responseText
    ) {
        static PhaseResult respond(BotSession session, ParsedAction action,
                                    LlmResponse llmResponse, List<KnowledgeHit> knowledgeHits) {
            return new PhaseResult(null, action, llmResponse, knowledgeHits,
                    null, false, false, null, action.getUserMessage());
        }

        static PhaseResult transition(BotSession session, String nextPhase,
                                       LlmResponse llmResponse, String reason) {
            return new PhaseResult(nextPhase, null, llmResponse, null,
                    reason, false, false, null, null);
        }

        static PhaseResult transitionWithResponse(BotSession session, String nextPhase,
                                                    ParsedAction action, LlmResponse llmResponse,
                                                    String reason) {
            return new PhaseResult(nextPhase, action, llmResponse, null,
                    reason, false, false, null, action.getUserMessage());
        }

        static PhaseResult transitionWithResponse(BotSession session, String nextPhase,
                                                    ParsedAction action, LlmResponse llmResponse,
                                                    String reason, List<KnowledgeHit> knowledgeHits) {
            return new PhaseResult(nextPhase, action, llmResponse, knowledgeHits,
                    reason, false, false, null, action.getUserMessage());
        }

        static PhaseResult escalate(BotSession session, String message, String reason) {
            // Sprint §A1: do NOT directly stamp session.escalationReason here.
            // ControlKernel funnels every reason through EscalationReasonResolver
            // so a higher-priority semantic reason (e.g. user_requested set on
            // an earlier turn) cannot be overwritten by a lower-priority
            // evaluator-side reason. The reason still travels back to the
            // caller via PhaseResult.escalationReason().
            session.setHandlingState("QUEUE_TO_HUMAN");
            session.setContainmentOutcome("escalated");
            return new PhaseResult("ESCALATE", null, null, null,
                    reason, false, true, reason, message);
        }

        static PhaseResult close(BotSession session, String message) {
            return new PhaseResult("CLOSE", null, null, null,
                    "session_closed", true, false, null, message);
        }
    }
}
