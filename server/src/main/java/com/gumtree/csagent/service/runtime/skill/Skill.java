package com.gumtree.csagent.service.runtime.skill;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Skill — first-class externalized capability per Sprint 37 freeze (NEW M2)
 * {@code docs/proposals/skill_registry_design.md} §2.1 decision (a).
 *
 * <p>A Skill is a YAML/JSON file under
 * {@code server/src/main/resources/skills/} deserialized into this record by
 * {@link SkillLoader}. The {@link SkillRegistry} indexes Skills by
 * {@code (phase, useCase)} and {@code PhaseEvaluator.plan(...)} composes the
 * selected Skill with session state into a {@code PhasePlan} per decision (c)
 * §4.1.
 *
 * <p>Field-level boundary per design doc §5 decision (d):
 * <ul>
 *   <li>{@link #procedure()} / {@link #groundingInstruction()} /
 *       {@link #escalationPolicy()} are LLM-soft teaching surfaced via
 *       {@code PhasePlan.systemInstruction / groundingInstruction /
 *       escalationPolicy}. LLM owns response strategy and customer-facing
 *       wording per Constitution §1.3.</li>
 *   <li>{@link #toolsRequired()} is the hard tool whitelist composed into
 *       {@code PhasePlan.allowedTools}, enforced by
 *       {@code ToolDispatcher.validateAgainstPlan} per Constitution §1.4.</li>
 *   <li>{@link #guardrails()} declare Runtime-floor enforcement points
 *       (Sprint 39 populates; Sprint 38's 4 simpler phase Skills all declare
 *       {@code guardrails: []}).</li>
 *   <li>{@link #stateInheritance()} declares per-Skill state-bus posture for
 *       UC switch (Sprint 38 populates per design doc §6.2.1-§6.2.4 templates;
 *       Sprint 41 enforces at {@code SkillStateBus} boundary).</li>
 * </ul>
 *
 * <p>Per design doc §2.4 / §2.5 / §1.7 boundary check: this data model itself
 * encodes no per-UC-branch if-else. {@link #applicableUseCases()} is
 * registry-scoping ("when does this Skill apply?"), NOT a per-UC semantic
 * decision branch.
 */
public record Skill(
        String name,
        String description,
        List<String> applicablePhases,
        List<String> applicableUseCases,
        List<String> toolsRequired,
        List<String> requiredContextKeys,
        Integer maxToolSteps,
        boolean allowInterimMessage,
        List<String> validTerminalOutcomes,
        String objective,
        String procedure,
        String groundingInstruction,
        String escalationPolicy,
        List<Guardrail> guardrails,
        StateInheritance stateInheritance
) {

    /** Wildcard sentinel for {@link #applicableUseCases()} per design doc §3.2. */
    public static final String UC_WILDCARD = "*";

    public Skill {
        applicablePhases = applicablePhases == null ? List.of() : List.copyOf(applicablePhases);
        applicableUseCases = applicableUseCases == null ? List.of() : List.copyOf(applicableUseCases);
        toolsRequired = toolsRequired == null ? List.of() : List.copyOf(toolsRequired);
        requiredContextKeys = requiredContextKeys == null ? List.of() : List.copyOf(requiredContextKeys);
        validTerminalOutcomes = validTerminalOutcomes == null ? List.of() : List.copyOf(validTerminalOutcomes);
        guardrails = guardrails == null ? List.of() : List.copyOf(guardrails);
        stateInheritance = stateInheritance == null ? StateInheritance.EMPTY : stateInheritance;
    }

    @JsonCreator
    public static Skill fromYaml(
            @JsonProperty("name") String name,
            @JsonProperty("description") String description,
            @JsonProperty("applicable_phases") List<String> applicablePhases,
            @JsonProperty("applicable_use_cases") List<String> applicableUseCases,
            @JsonProperty("tools_required") List<String> toolsRequired,
            @JsonProperty("required_context_keys") List<String> requiredContextKeys,
            @JsonProperty("max_tool_steps") Integer maxToolSteps,
            @JsonProperty("allow_interim_message") Boolean allowInterimMessage,
            @JsonProperty("valid_terminal_outcomes") List<String> validTerminalOutcomes,
            @JsonProperty("objective") String objective,
            @JsonProperty("procedure") String procedure,
            @JsonProperty("grounding_instruction") String groundingInstruction,
            @JsonProperty("escalation_policy") String escalationPolicy,
            @JsonProperty("guardrails") List<Guardrail> guardrails,
            @JsonProperty("state_inheritance") StateInheritance stateInheritance
    ) {
        return new Skill(
                name,
                description,
                applicablePhases,
                applicableUseCases,
                toolsRequired,
                requiredContextKeys,
                maxToolSteps,
                allowInterimMessage != null && allowInterimMessage,
                validTerminalOutcomes,
                objective,
                procedure,
                groundingInstruction,
                escalationPolicy,
                guardrails,
                stateInheritance
        );
    }

    /**
     * True if this Skill applies to the given {@code (phase, useCase)} tuple
     * per the selection semantics in design doc §3.2: exact phase match AND
     * (exact UC match OR {@code ["*"]} wildcard).
     */
    public boolean appliesTo(String phase, String useCase) {
        if (phase == null || !applicablePhases.contains(phase)) {
            return false;
        }
        if (useCase != null && applicableUseCases.contains(useCase)) {
            return true;
        }
        return applicableUseCases.contains(UC_WILDCARD);
    }
}
