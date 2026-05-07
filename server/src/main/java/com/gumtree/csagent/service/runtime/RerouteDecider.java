package com.gumtree.csagent.service.runtime;

import com.gumtree.csagent.model.BotSession;
import com.gumtree.csagent.model.IntentClassification;
import com.gumtree.csagent.model.IntentClassification.IntentRelation;
import com.gumtree.csagent.model.RerouteDecision;
import com.gumtree.csagent.model.RerouteDecision.RerouteAction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Sprint 10 §L1 — translate an {@link IntentClassification} + current
 * (phase, active_use_case) into a {@link RerouteDecision} the kernel
 * applies BEFORE {@code PhaseEvaluator.plan(...)}.
 *
 * <p>Decision matrix (Sprint 10 spec):
 * <ul>
 *   <li>{@code CONFIRM + NEW_LOW_RISK_UC} → switch UC + RESOLVE
 *       (deterministic UC produced by classifier; jump straight into
 *       the new UC's RESOLVE plan instead of re-clarifying in DISCOVER).</li>
 *   <li>{@code RESOLVE + NEW_LOW_RISK_UC} → switch UC; stay in RESOLVE
 *       (the {@code RESOLVE → DISCOVER} transition is not in the
 *       allowed-transitions table and the spec forbids broadening it).</li>
 *   <li>{@code CONFIRM + SAME_ISSUE} or {@code SAME_UC_NEW_TASK} →
 *       CONFIRM → RESOLVE rebound on the current UC.</li>
 *   <li>{@code RESOLVE + SAME_ISSUE / SAME_UC_NEW_TASK} → no-op (already
 *       in RESOLVE; the planner re-runs RESOLVE this turn).</li>
 *   <li>{@code NEW_HIGH_RISK_UC} → switch UC + RESOLVE so the
 *       intake plan ({@code RESOLVE_INTAKE}) runs instead of generic
 *       FAQ. Existing intake plan + {@code createCaseIfNeeded} +
 *       {@code request_handover(intake_complete_for_uc_*)} pipeline
 *       handles handover.</li>
 *   <li>{@code HUMAN_REQUEST / CRITICAL_ESCALATION} → CONTINUE_CURRENT
 *       (the kernel's existing step 2.4 / 2.5 already escalated; the
 *       classifier never reaches the decider on those turns, but we
 *       keep this branch defensively for callers that bypass the
 *       kernel's early guards).</li>
 *   <li>{@code UNKNOWN} → CONTINUE_CURRENT (no state change).</li>
 * </ul>
 *
 * <p>An already-committed UC is never overwritten unless the
 * {@link RerouteAction} explicitly requires it (i.e. SOFT_SHIFT, RISK_SHIFT,
 * or REBOUND_TO_RESOLVE on the same UC).
 */
@Slf4j
@Service
public class RerouteDecider {

    public RerouteDecision decide(BotSession session, IntentClassification classification) {
        if (session == null || classification == null) {
            return RerouteDecision.continueCurrent(IntentClassification.unknown());
        }
        IntentRelation relation = classification.relation();
        if (relation == null) {
            return RerouteDecision.continueCurrent(classification);
        }
        String activeUc = session.getActiveUseCase();
        String phase = session.getCurrentPhase();
        String predicted = classification.predictedUseCase();

        switch (relation) {
            case HUMAN_REQUEST:
                // Kernel step 2.5 already handled this; we still surface the
                // relation for telemetry but do not mutate state ourselves.
                return new RerouteDecision(RerouteAction.ESCALATE_IMMEDIATELY,
                        activeUc, "ESCALATE", "user_requested_via_classifier", classification);

            case CRITICAL_ESCALATION:
                // Distress is a precedence STAMP (priority 2), not a terminal
                // trigger by itself — kernel step 2.4 already applied it.
                // We surface ESCALATE_IMMEDIATELY only if the message is also
                // an explicit human request, but that is the HUMAN_REQUEST
                // branch above. So for pure distress we continue current.
                return RerouteDecision.continueCurrent(classification);

            case NEW_HIGH_RISK_UC:
                if (predicted == null || predicted.isBlank()) {
                    return RerouteDecision.continueCurrent(classification);
                }
                // Risk shift: switch UC and land in RESOLVE so the intake
                // plan runs. UC may already be the predicted target (drift
                // detector ran twice / re-assertion) — still allow the
                // re-route so phase normalises to RESOLVE.
                return new RerouteDecision(RerouteAction.RISK_SHIFT_TO_INTAKE,
                        predicted, "RESOLVE",
                        "risk_shift_to_" + predicted.toLowerCase().replace('-', '_'),
                        classification);

            case NEW_LOW_RISK_UC:
                if (predicted == null || predicted.isBlank()
                        || predicted.equals(activeUc)) {
                    // Predicted matches the active UC — a SAME_ISSUE in
                    // disguise. Rebound to RESOLVE if we were in CONFIRM.
                    if ("CONFIRM".equals(phase)) {
                        return new RerouteDecision(RerouteAction.REBOUND_TO_RESOLVE,
                                activeUc, "RESOLVE",
                                "same_uc_resolve_rebound", classification);
                    }
                    return RerouteDecision.continueCurrent(classification);
                }
                // Genuine soft shift — switch UC and route to RESOLVE.
                String softTarget;
                if ("CONFIRM".equals(phase)) {
                    softTarget = "RESOLVE";
                } else if ("RESOLVE".equals(phase)) {
                    // The legal RESOLVE→{CONFIRM,ESCALATE} table forbids
                    // RESOLVE→DISCOVER; stay in RESOLVE and let the
                    // planner re-run for the new UC.
                    softTarget = "RESOLVE";
                } else if ("DISCOVER".equals(phase)) {
                    softTarget = "DISCOVER";
                } else {
                    softTarget = phase; // unknown phase — leave as-is
                }
                return new RerouteDecision(RerouteAction.SOFT_SHIFT_TO_DISCOVER,
                        predicted, softTarget,
                        "soft_shift_to_" + predicted.toLowerCase().replace('-', '_'),
                        classification);

            case SAME_ISSUE:
            case SAME_UC_NEW_TASK:
                if ("CONFIRM".equals(phase)) {
                    return new RerouteDecision(RerouteAction.REBOUND_TO_RESOLVE,
                            activeUc, "RESOLVE",
                            relation == IntentRelation.SAME_ISSUE
                                    ? "same_issue_resolve_rebound"
                                    : "same_uc_followup_resolve_rebound",
                            classification);
                }
                // Already in RESOLVE / DISCOVER — let planner run as today.
                return RerouteDecision.continueCurrent(classification);

            case UNKNOWN:
            default:
                return RerouteDecision.continueCurrent(classification);
        }
    }
}
