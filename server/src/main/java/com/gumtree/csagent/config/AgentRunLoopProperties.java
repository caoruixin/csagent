package com.gumtree.csagent.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Feature-flag configuration for the D16 agent run loop.
 *
 * <p>{@code enabledPhases} is the rollout switch: each entry is a
 * phase-or-phase/UC token (e.g. {@code RESOLVE_FAQ}, {@code RESOLVE_INTAKE},
 * {@code DISCOVER}, {@code CONFIRM}, {@code CLOSE}, {@code ESCALATE}). When a
 * given phase token is present, {@code ControlKernel} routes that phase
 * through {@code PhaseEvaluator.plan() -> AgentRunLoop.run() ->
 * PhaseEvaluator.interpretRunResult() -> recordRunResult()}; otherwise the
 * legacy path is used.
 *
 * <p>D16.A default: empty list — legacy path everywhere.
 */
@Data
@ConfigurationProperties(prefix = "agent.run-loop")
public class AgentRunLoopProperties {

    /** Phase tokens that should be routed through {@code AgentRunLoop}. */
    private List<String> enabledPhases = new ArrayList<>();

    /**
     * Default upper bound on tool-use iterations within a single agent run.
     * Used by {@code PhaseEvaluator.plan(...)} when a phase plan does not
     * specify its own {@code maxToolSteps}.
     */
    private int maxToolStepsDefault = 4;
}
