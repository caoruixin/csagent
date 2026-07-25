package com.gumtree.csagent.service.runtime;

import com.gumtree.csagent.model.AgentRunResult;
import com.gumtree.csagent.model.PhasePlan;
import com.gumtree.csagent.model.ResolveDisposition;
import com.gumtree.csagent.model.TerminalOutcome;
import com.gumtree.csagent.model.ToolEvent;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Sprint 11 §M1 — translate an {@link AgentRunResult} on a RESOLVE/FAQ
 * plan into a {@link ResolveDisposition} so the kernel does not collapse
 * every grounded answer into {@code RESOLVE → CONFIRM → record_outcome}
 * on the same turn.
 *
 * <p>The evaluator is deterministic and tolerant: nullable / blank
 * inputs default to {@link ResolveDisposition#CONTINUE_RESOLVE}.
 *
 * <p>Decision order (RESOLVE / FAQ plan only):
 * <ol>
 *   <li>{@code TerminalOutcome.ESCALATE} → {@link ResolveDisposition#ESCALATE}.</li>
 *   <li>{@code TerminalOutcome.CLARIFICATION_NEEDED} → {@link ResolveDisposition#ASKED_FOR_SLOT}.</li>
 *   <li>{@code TerminalOutcome.FINAL_ANSWER} —
 *     <ul>
 *       <li>If the bot text is a clarifying question or a soft next-step
 *           shape ("send the advert ID", "if you can share", "share the
 *           ad id"), the bot ASKED for a slot — return
 *           {@link ResolveDisposition#ASKED_FOR_SLOT}.</li>
 *       <li>If deterministic terminal evidence exists — a successful
 *           {@code record_outcome} call on this run — return
 *           {@link ResolveDisposition#READY_TO_CONFIRM}.</li>
 *       <li>Otherwise (a non-question / non-slot-request grounded
 *           answer such as "Your advert is active for 30 days from
 *           posting"), the bot answered a same-UC subtask but has not
 *           earned a hard CONFIRM — return
 *           {@link ResolveDisposition#ANSWERED_SUBTASK} so the session
 *           stays in RESOLVE.</li>
 *     </ul>
 *   </li>
 *   <li>Anything else (MAX_STEPS, ERROR, USE_CASE_IDENTIFIED, etc.) →
 *       {@link ResolveDisposition#CONTINUE_RESOLVE}.</li>
 * </ol>
 *
 * <p>Sprint 11.1 closure — the previous iteration defaulted any
 * non-slot FINAL_ANSWER to {@link ResolveDisposition#READY_TO_CONFIRM},
 * which let UC-A same-UC follow-up answers (listing expiry / status,
 * messaging diagnostics) collapse RESOLVE → CONFIRM without a
 * deterministic terminal marker. The current contract requires
 * deterministic terminal evidence — successful {@code record_outcome}
 * dispatch on this turn — before READY_TO_CONFIRM is returned.
 */
public final class ResolveDispositionEvaluator {

    /**
     * Soft next-step / asked-for-slot phrases used in FAQ-grounded bot
     * answers when the bot wants the user to share an entity (e.g.
     * {@code ad_id}) but is not yet ready to confirm resolution.
     * Matched case-insensitively.
     */
    private static final Pattern SOFT_NEXT_STEP_PATTERN = Pattern.compile(
            "\\b("
                    + "send\\s+(?:me\\s+)?(?:the\\s+|your\\s+)?(?:ad(?:vert)?[-\\s]?id|advert\\s+id|ad\\s+id|listing\\s+id)"
                    + "|share\\s+(?:the\\s+|your\\s+)?(?:ad(?:vert)?[-\\s]?id|advert\\s+id|ad\\s+id|listing\\s+id)"
                    + "|provide\\s+(?:the\\s+|your\\s+)?(?:ad(?:vert)?[-\\s]?id|advert\\s+id|ad\\s+id|listing\\s+id)"
                    + "|let\\s+me\\s+know\\s+(?:the\\s+|your\\s+)?(?:ad(?:vert)?[-\\s]?id|advert\\s+id|ad\\s+id|listing\\s+id)"
                    + "|if\\s+you\\s+(?:can|could|want)\\s+(?:me\\s+to\\s+)?(?:check|look|share)"
                    + "|would\\s+you\\s+like\\s+me\\s+to"
                    + "|do\\s+you\\s+want\\s+me\\s+to\\s+check"
                    + "|let\\s+me\\s+know\\s+if"
                    + ")\\b",
            Pattern.CASE_INSENSITIVE);

    /** Question shapes treated as clarification / slot-request follow-ups. */
    private static final Pattern CLARIFYING_QUESTION_PATTERN = Pattern.compile(
            "\\b("
                    + "could\\s+you|can\\s+you\\s+tell|what\\s+is|which\\s+|do\\s+you\\s+have"
                    + "|please\\s+share|please\\s+send|please\\s+provide"
                    + ")\\b",
            Pattern.CASE_INSENSITIVE);

    private ResolveDispositionEvaluator() {
        // static-only helper
    }

    /**
     * Pin the disposition for a RESOLVE / FAQ plan turn.
     *
     * <p>The evaluator only fires on RESOLVE plans for FAQ-class UCs.
     * INTAKE plans, DISCOVER, CONFIRM, and CLOSE keep their existing
     * mappings; passing them in returns {@link ResolveDisposition#CONTINUE_RESOLVE}
     * defensively.
     */
    public static ResolveDisposition evaluate(PhasePlan plan, AgentRunResult result) {
        if (result == null) {
            return ResolveDisposition.CONTINUE_RESOLVE;
        }
        if (!isResolveFaqPlan(plan)) {
            return ResolveDisposition.CONTINUE_RESOLVE;
        }
        TerminalOutcome outcome = result.terminalOutcome();
        if (outcome == null) {
            return ResolveDisposition.CONTINUE_RESOLVE;
        }
        switch (outcome) {
            case ESCALATE:
                return ResolveDisposition.ESCALATE;
            case CLARIFICATION_NEEDED:
                return ResolveDisposition.ASKED_FOR_SLOT;
            case FINAL_ANSWER: {
                String text = safeLower(result.finalUserMessage());
                boolean asksForSlot = text != null
                        && (text.trim().endsWith("?")
                                || CLARIFYING_QUESTION_PATTERN.matcher(text).find()
                                || SOFT_NEXT_STEP_PATTERN.matcher(text).find());
                if (asksForSlot) {
                    return ResolveDisposition.ASKED_FOR_SLOT;
                }
                // Sprint 11.1 — require deterministic terminal evidence
                // before READY_TO_CONFIRM. A non-question, non-slot
                // FAQ-grounded final answer (e.g. "Your advert is active
                // for 30 days from posting.") is a same-UC subtask
                // answer, NOT a hard close: it does not by itself prove
                // the user accepted the answer or that the session
                // outcome was recorded. Only a successful
                // {@code record_outcome} dispatch on this run earns the
                // RESOLVE → CONFIRM transition. The Sprint 9 §O1
                // record-outcome failure-retry path is upstream of this
                // evaluator and continues to keep failed-record_outcome
                // turns in RESOLVE via
                // {@code PhaseEvaluator#recordOutcomeAttemptedAndFailed}.
                if (recordOutcomeSucceededThisRun(result)) {
                    return ResolveDisposition.READY_TO_CONFIRM;
                }
                return ResolveDisposition.ANSWERED_SUBTASK;
            }
            default:
                return ResolveDisposition.CONTINUE_RESOLVE;
        }
    }

    /**
     * Sprint 11 §M1 — record-outcome guard. Returns true when the LLM
     * asks to {@code record_outcome(outcome_class=resolve)} during
     * RESOLVE / FAQ but the deterministic terminal condition is not
     * satisfied:
     *
     * <ul>
     *   <li>The current phase is not CONFIRM or CLOSE; AND</li>
     *   <li>This run has not yet emitted any tool that earns the
     *       outcome (no successful {@code record_outcome} prior to this
     *       call) — i.e. this is a "single factual answer" attempt.</li>
     * </ul>
     *
     * <p>The bot is still allowed to call {@code record_outcome} with
     * other outcome classes (escalate / abandon) and on CONFIRM / CLOSE
     * plans. Other resolve-outcome paths are not intercepted.
     */
    public static boolean shouldRejectPrematureResolveOutcome(PhasePlan plan,
                                                              String currentPhase,
                                                              String outcomeClassArg) {
        if (plan == null) return false;
        if (!isResolveFaqPlan(plan)) return false;
        if (outcomeClassArg == null) return false;
        String normalized = outcomeClassArg.trim().toLowerCase(Locale.ROOT);
        if (!"resolve".equals(normalized) && !"resolved".equals(normalized)) {
            return false;
        }
        // Allow CONFIRM / CLOSE phases (the planner already validated the
        // user accepted the answer or the close phase fired).
        if ("CONFIRM".equalsIgnoreCase(currentPhase)
                || "CLOSE".equalsIgnoreCase(currentPhase)) {
            return false;
        }
        return true;
    }

    static boolean isResolveFaqPlan(PhasePlan plan) {
        if (plan == null) return false;
        if (!"RESOLVE".equalsIgnoreCase(plan.phase())) return false;
        String uc = plan.useCase();
        if (uc == null || uc.isBlank()) return false;
        // Intake-ONLY UCs route through their own flow; they never produce
        // FAQ-grounded final answers in the same way.
        return !INTAKE_ONLY_UCS.contains(uc);
    }

    /**
     * Intake-only UCs (registry {@code path: INTAKE}).
     *
     * <p>WS-3 / A3 (2026-07-25): UC-K removed. Its registry path is now
     * {@code PARTIAL}, so a UC-K RESOLVE turn CAN produce a grounded final
     * answer and must therefore get a real {@link ResolveDisposition} — with
     * UC-K still listed here, every UC-K answer fell through to
     * {@code CONTINUE_RESOLVE}, the session could never reach CONFIRM, and
     * {@code record_outcome} could never land. That is the second half of the
     * "architecturally cannot self-resolve" defect A3 names.
     *
     * <p>The authoritative declaration is {@code path} in
     * {@code config/use-case-registry.yaml}. This class is a static utility on
     * the {@code runtime_freeze_and_risk_policy.md} §1.1 #3 frozen surface and
     * has no Spring context to inject the registry from, so it mirrors the
     * registry rather than reading it; {@code UseCaseRegistryPathConsistencyTest}
     * fails if the two ever disagree.
     */
    private static final java.util.Set<String> INTAKE_ONLY_UCS = java.util.Set.of(
            "UC-G", "UC-H", "UC-I", "UC-J");

    private static boolean recordOutcomeSucceededThisRun(AgentRunResult result) {
        if (result == null || result.toolEvents() == null) return false;
        for (ToolEvent te : result.toolEvents()) {
            if ("record_outcome".equals(te.toolName()) && te.success()) {
                return true;
            }
        }
        return false;
    }

    private static String safeLower(String s) {
        return s == null ? null : s.toLowerCase(Locale.ROOT);
    }
}
