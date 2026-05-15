package com.gumtree.csagent.model;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Mission briefing produced by {@code PhaseEvaluator.plan(...)} and consumed by
 * {@code AgentRunLoop.run(...)}. Per Phase 3 §3.3.3 (v9 — D16).
 *
 * <p>The full plan is projected into the LLM context every iteration of the loop
 * (except {@link #maxToolSteps()}, which is enforced server-side and not shown
 * to the model). {@code allowedTools} is enforced by
 * {@code ToolDispatcher.validateAgainstPlan(...)} as a hard whitelist.
 *
 * <p>Records do not auto-generate a builder, so a hand-rolled {@link Builder} is
 * provided for ergonomic construction matching the existing codebase style.
 */
public record PhasePlan(
        String phase,
        String useCase,
        String objective,
        List<String> allowedTools,
        Set<String> requiredContextKeys,
        int maxToolSteps,
        boolean allowInterimMessage,
        Set<TerminalOutcome> validTerminalOutcomes,
        String systemInstruction,
        String groundingInstruction,
        String escalationPolicy
) {

    /**
     * Compact constructor: defensively-copies collection-valued components and
     * substitutes empty collections for nulls so consumers never need to
     * null-check. {@code maxToolSteps} must be {@code >= 0}.
     */
    public PhasePlan {
        if (maxToolSteps < 0) {
            throw new IllegalArgumentException("maxToolSteps must be >= 0, got " + maxToolSteps);
        }
        allowedTools = allowedTools == null
                ? List.of()
                : List.copyOf(allowedTools);
        requiredContextKeys = requiredContextKeys == null
                ? Set.of()
                : Set.copyOf(requiredContextKeys);
        validTerminalOutcomes = validTerminalOutcomes == null
                ? Set.of()
                : Collections.unmodifiableSet(new LinkedHashSet<>(validTerminalOutcomes));
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String phase;
        private String useCase;
        private String objective;
        private List<String> allowedTools;
        private Set<String> requiredContextKeys;
        private int maxToolSteps;
        private boolean allowInterimMessage;
        private Set<TerminalOutcome> validTerminalOutcomes;
        private String systemInstruction;
        private String groundingInstruction;
        private String escalationPolicy;

        private Builder() {}

        public Builder phase(String phase) {
            this.phase = phase;
            return this;
        }

        public Builder useCase(String useCase) {
            this.useCase = useCase;
            return this;
        }

        public Builder objective(String objective) {
            this.objective = objective;
            return this;
        }

        public Builder allowedTools(List<String> allowedTools) {
            this.allowedTools = allowedTools;
            return this;
        }

        public Builder requiredContextKeys(Set<String> requiredContextKeys) {
            this.requiredContextKeys = requiredContextKeys;
            return this;
        }

        public Builder maxToolSteps(int maxToolSteps) {
            this.maxToolSteps = maxToolSteps;
            return this;
        }

        public Builder allowInterimMessage(boolean allowInterimMessage) {
            this.allowInterimMessage = allowInterimMessage;
            return this;
        }

        public Builder validTerminalOutcomes(Set<TerminalOutcome> validTerminalOutcomes) {
            this.validTerminalOutcomes = validTerminalOutcomes;
            return this;
        }

        public Builder systemInstruction(String systemInstruction) {
            this.systemInstruction = systemInstruction;
            return this;
        }

        public Builder groundingInstruction(String groundingInstruction) {
            this.groundingInstruction = groundingInstruction;
            return this;
        }

        public Builder escalationPolicy(String escalationPolicy) {
            this.escalationPolicy = escalationPolicy;
            return this;
        }

        public PhasePlan build() {
            Objects.requireNonNull(phase, "phase is required");
            return new PhasePlan(
                    phase,
                    useCase,
                    objective,
                    allowedTools,
                    requiredContextKeys,
                    maxToolSteps,
                    allowInterimMessage,
                    validTerminalOutcomes,
                    systemInstruction,
                    groundingInstruction,
                    escalationPolicy
            );
        }
    }
}
