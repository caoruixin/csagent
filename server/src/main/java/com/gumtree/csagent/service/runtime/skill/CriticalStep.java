package com.gumtree.csagent.service.runtime.skill;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Locale;

/**
 * Critical step declaration on a {@link Skill} per Sprint 43 (S-Eval-2, NEW
 * Milestone M3-Eval sub-sprint 2). One step names a load-bearing piece of
 * per-Skill procedure that the eval-side Tier-2 {@code skill_procedure_followship}
 * gate verifies against a session trace.
 *
 * <p>Field semantics:
 * <ul>
 *   <li>{@code id} — stable identifier, unique within a Skill. Used by the
 *       extractor when reporting per-step PASS/FAIL/N/A and by S-Eval-3
 *       authors to cross-reference steps from a {@code desc} narrative.</li>
 *   <li>{@code desc} — LLM-visible procedural narrative (soft signal). Rendered
 *       inside the {@code phase_plan.skill.critical_steps} projection slot
 *       immediately after {@code procedure} by
 *       {@link com.gumtree.csagent.service.runtime.ContextProjectionBuilder}.
 *       Single source of truth: the same string feeds both the LLM (as
 *       guidance) and the eval rubric (as the contract the
 *       {@code trace_check} verifies). Per the proposal §5 decision 5; per
 *       Constitution §1.3 the LLM owns whether to act on it.</li>
 *   <li>{@code traceCheck} — eval-side DSL expression (a string, preserved
 *       verbatim). The Python {@code SkillProcedureExtractor} parses and
 *       evaluates this against the session trace; its grammar is the only
 *       structural defence against §1.7 hardcodes on the eval side. The Java
 *       loader DOES NOT validate DSL syntax — Python owns evaluation.</li>
 *   <li>{@code mandatoryFor} — list of UC ids for which this step is
 *       gate-contributing. A case whose {@code active_use_case} is in this
 *       list and whose extractor outcome is FAIL flips
 *       {@code case_passed}. UCs outside this list see the step as N/A.</li>
 *   <li>{@code severity} — {@link Severity#MANDATORY} steps gate the case;
 *       {@link Severity#ADVISORY} steps are recorded but never flip
 *       {@code case_passed}. The enum mirrors the
 *       {@code severity = critical | advisory} contract that S-Eval-1
 *       (Sprint 42, D-2.5) introduced on
 *       {@code HardCheckResult} / {@code OutcomeCheckResult}.</li>
 * </ul>
 *
 * <p>Sprint 43 ships the schema only. No Skill YAML carries a populated
 * {@code critical_steps:} block; that lands in S-Eval-3 (per-sub-sprint
 * Codex review per {@code iteration_governance.md} §4.3 trigger #2).
 */
public record CriticalStep(
        String id,
        String desc,
        String traceCheck,
        List<String> mandatoryFor,
        Severity severity
) {

    /**
     * Gate posture per step. {@link #MANDATORY} steps are Tier-2 gate-
     * contributing; {@link #ADVISORY} steps are recorded but do not flip
     * {@code case_passed}.
     */
    public enum Severity {
        MANDATORY,
        ADVISORY;

        @JsonCreator
        public static Severity fromYaml(String raw) {
            if (raw == null) {
                return null;
            }
            return Severity.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        }
    }

    public CriticalStep {
        mandatoryFor = mandatoryFor == null ? List.of() : List.copyOf(mandatoryFor);
    }

    @JsonCreator
    public static CriticalStep fromYaml(
            @JsonProperty("id") String id,
            @JsonProperty("desc") String desc,
            @JsonProperty("trace_check") String traceCheck,
            @JsonProperty("mandatory_for") List<String> mandatoryFor,
            @JsonProperty("severity") Severity severity
    ) {
        return new CriticalStep(id, desc, traceCheck, mandatoryFor, severity);
    }
}
