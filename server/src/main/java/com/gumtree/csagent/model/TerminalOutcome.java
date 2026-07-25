package com.gumtree.csagent.model;

/**
 * Terminal outcome of a single {@code AgentRunLoop.run(...)} invocation.
 *
 * <p>Per Phase 3 §3.3.3 (Agent Run Loop Architecture, v9 — D16). The loop is
 * bounded by {@link PhasePlan#maxToolSteps()}; on each iteration the loop ends
 * when the LLM produces a final user_message or runs out of steps.
 *
 * <ul>
 *   <li>{@link #FINAL_ANSWER} — LLM produced a customer-facing answer with no further tool calls.</li>
 *   <li>{@link #CLARIFICATION_NEEDED} — LLM produced a clarifying question (no tool calls).</li>
 *   <li>{@link #ESCALATE} — LLM (or runtime) chose to hand over to a human agent.</li>
 *   <li>{@link #MAX_STEPS} — Loop hit {@code plan.maxToolSteps} without terminating; treated as escalate-on-budget.</li>
 *   <li>{@link #ERROR} — Unrecoverable failure inside the loop (parser, dispatcher, etc.).</li>
 *   <li>{@link #DEADLINE_EXCEEDED} — Sprint 8.1 §M2: per-turn LLM wall-clock
 *       budget was exhausted before any attempt could complete. The loop
 *       returns this so upstream layers can render an honest slow/give-up
 *       response without stamping a synthetic UC fallback.</li>
 *   <li>{@link #LLM_UNAVAILABLE} — Sprint 8.1 §M2: the LLM provider chain
 *       (primary + fallback) failed for an infrastructure reason that would
 *       not be cured by the remaining budget (transport / 5xx / 429 /
 *       401 / 403 after retry exhaustion). Same downstream treatment as
 *       {@link #DEADLINE_EXCEEDED}: K0 fallback does NOT fire so the
 *       failure is not masked as a normal business escalation.</li>
 *   <li>{@link #USE_CASE_IDENTIFIED} — Sprint 8.1 §M3: DISCOVER plan
 *       deterministic phase boundary. The LLM successfully called
 *       {@code classify_use_case} and the tool committed
 *       {@code session.activeUseCase}. Returned immediately by the loop so
 *       {@code ControlKernel} can perform a bounded same-turn replan into
 *       RESOLVE for the newly committed UC instead of continuing the
 *       DISCOVER plan to {@code maxToolSteps} (which would otherwise be
 *       mis-mapped to ESCALATE / {@code faq_miss_threshold_exceeded}).</li>
 *   <li>{@link #USE_CASE_REROUTED} — Sprint 103 / WS-6-A: the mid-session
 *       analogue of {@link #USE_CASE_IDENTIFIED}. The LLM called
 *       {@code propose_reroute} from a RESOLVE or CONFIRM plan because the
 *       customer raised a different need, and the runtime honoured the
 *       proposal by moving {@code session.activeUseCase} to the target UC.
 *       Returned immediately so {@code ControlKernel} can replan the same
 *       turn into the target UC's RESOLVE Skill; continuing the old UC's
 *       plan would leave the loop dispatching against a plan whose
 *       {@code allowedTools} and guardrails belong to a UC the session has
 *       left. Distinct from {@link #USE_CASE_IDENTIFIED} so the trace can
 *       tell an intake-time commit apart from a mid-session re-route.</li>
 * </ul>
 */
public enum TerminalOutcome {
    FINAL_ANSWER,
    CLARIFICATION_NEEDED,
    ESCALATE,
    MAX_STEPS,
    ERROR,
    DEADLINE_EXCEEDED,
    LLM_UNAVAILABLE,
    USE_CASE_IDENTIFIED,
    USE_CASE_REROUTED
}
