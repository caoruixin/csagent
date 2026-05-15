package com.gumtree.csagent.model;

/**
 * Sprint 10 §L1 — decision produced by the runtime reroute logic before
 * {@code PhaseEvaluator.plan(...)}. The kernel applies this to mutate
 * (phase, active_use_case, minimal projected issue state) BEFORE planning
 * runs and the agent loop is invoked.
 *
 * <p>Decoupled from {@link IntentClassification} so the kernel can apply
 * existing precedence (distress / explicit-human / budget) without the
 * classifier needing to know about every kernel-internal guard.
 *
 * @param action            the reroute action to apply.
 * @param targetUseCase     UC the kernel should switch to (null when
 *                          {@code action == CONTINUE_CURRENT}).
 * @param targetPhase       phase the kernel should land in after the
 *                          reroute (null = leave phase untouched).
 * @param transitionReason  short token recorded in the trace observability
 *                          payload (e.g. {@code soft_shift_to_uc_c},
 *                          {@code same_issue_rebound},
 *                          {@code risk_shift_to_uc_j}).
 * @param classification    underlying {@link IntentClassification} that
 *                          produced this decision; carried through so
 *                          downstream observability (events, trace,
 *                          projection) can surface predicted UC + relation
 *                          without re-classifying.
 */
public record RerouteDecision(
        RerouteAction action,
        String targetUseCase,
        String targetPhase,
        String transitionReason,
        IntentClassification classification
) {

    /** Sprint 10 §L1 — runtime action to apply before planner execution. */
    public enum RerouteAction {
        /** Leave phase / UC untouched; classifier did not decide. */
        CONTINUE_CURRENT,
        /** Switch active UC and land in DISCOVER (or RESOLVE when safe). */
        SOFT_SHIFT_TO_DISCOVER,
        /** Switch active UC and land in RESOLVE (intake-path UC). */
        RISK_SHIFT_TO_INTAKE,
        /** Stay on current UC, return to RESOLVE (CONFIRM rebound on same issue / same-UC follow-up). */
        REBOUND_TO_RESOLVE,
        /** Escalate immediately; existing user_requested / user_distress precedence applies. */
        ESCALATE_IMMEDIATELY
    }

    public static RerouteDecision continueCurrent(IntentClassification classification) {
        return new RerouteDecision(RerouteAction.CONTINUE_CURRENT, null, null,
                "no_reroute", classification);
    }
}
