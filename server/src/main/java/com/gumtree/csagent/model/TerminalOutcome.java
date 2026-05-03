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
 * </ul>
 */
public enum TerminalOutcome {
    FINAL_ANSWER,
    CLARIFICATION_NEEDED,
    ESCALATE,
    MAX_STEPS,
    ERROR
}
