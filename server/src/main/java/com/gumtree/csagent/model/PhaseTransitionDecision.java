package com.gumtree.csagent.model;

/**
 * Decision returned by {@code PhaseEvaluator.interpretRunResult(...)} after
 * an {@link AgentRunResult} comes back from the loop. Tells
 * {@code ControlKernel} which phase to move to next, what to send back to
 * the user, and (if escalating) why.
 *
 * <p>For D16.A this type is defined but {@code interpretRunResult} is not yet
 * called; real construction lands in D16.B.
 */
public record PhaseTransitionDecision(
        String nextPhase,
        String responseText,
        String escalationReason,
        String transitionReason
) {}
