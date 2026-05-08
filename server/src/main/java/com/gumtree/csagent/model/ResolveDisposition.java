package com.gumtree.csagent.model;

/**
 * Sprint 11 §M1 — runtime checkpoint for same-UC progressive resolve.
 *
 * <p>Pinned by {@code ResolveDispositionEvaluator}, applied by
 * {@code PhaseEvaluator.mapFinalAnswer} (RESOLVE/FAQ branch) and the
 * {@code AgentRunLoopImpl} record-outcome guard. The disposition prevents
 * a single factual answer from collapsing into {@code RESOLVE → CONFIRM →
 * record_outcome(resolve)} on the same turn — same-UC follow-up subtasks,
 * slot requests, and soft next-step answers must remain in RESOLVE.
 *
 * <ul>
 *   <li>{@link #CONTINUE_RESOLVE} — bot is still working the issue
 *       (e.g. searching, planning the next step). Stay in RESOLVE.</li>
 *   <li>{@link #ASKED_FOR_SLOT} — bot asked the user for a missing slot
 *       such as {@code ad_id} or a clarification question. Stay in
 *       RESOLVE so the user reply re-enters the same plan.</li>
 *   <li>{@link #ANSWERED_SUBTASK} — bot delivered a grounded answer or a
 *       soft next step (e.g. "Here is how to find your ad; send the
 *       advert ID if you want me to check it"). Stay in RESOLVE; the
 *       LLM did not yet earn a hard CONFIRM.</li>
 *   <li>{@link #READY_TO_CONFIRM} — deterministic terminal condition
 *       satisfied: the user explicitly accepted the answer, the close
 *       phase was reached, or {@code record_outcome(resolve)} was earned
 *       through a successful CONFIRM round. Transition RESOLVE → CONFIRM.</li>
 *   <li>{@link #ESCALATE} — escalate immediately
 *       (existing precedence applies; user_requested / user_distress).</li>
 * </ul>
 */
public enum ResolveDisposition {
    CONTINUE_RESOLVE,
    ASKED_FOR_SLOT,
    ANSWERED_SUBTASK,
    READY_TO_CONFIRM,
    ESCALATE;

    /**
     * Map this disposition to the canonical {@code task_status} token
     * surfaced in the projection / observability slot.
     */
    public String toTaskStatusToken() {
        return switch (this) {
            case CONTINUE_RESOLVE -> "in_progress";
            case ASKED_FOR_SLOT -> "asked_for_slot";
            case ANSWERED_SUBTASK -> "answered_subtask";
            case READY_TO_CONFIRM -> "ready_to_confirm";
            case ESCALATE -> "escalate";
        };
    }
}
