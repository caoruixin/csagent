package com.gumtree.csagent.service.runtime.skill;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

/**
 * Skill guardrail declaration per Sprint 37 freeze §5.2 (declarative DSL).
 *
 * <p>A guardrail is a typed object naming a Runtime-floor enforcement predicate
 * the dispatcher composes at tool-call / outcome-emission time. Sprint 38 ships
 * the schema but the 4 simpler phase Skills all declare {@code guardrails: []}
 * (empty) per design doc §6.2.1-§6.2.4; concrete guardrail types
 * ({@code must_cite_source}, {@code faq_miss_handover_requires_resolve_attempt},
 * {@code premature_resolve_outcome_guard}, {@code intake_complete_required})
 * are populated in Sprint 39 alongside the unified
 * {@code SkillGuardrailDispatcher}.
 *
 * <p>Schema:
 * <pre>
 * type: must_cite_source            # known predicate type
 * on_fail: reject_with_hint         # reject_with_hint | downgrade_reason
 * parameters:
 *   outcome_class: resolve
 * </pre>
 *
 * <p>Per design doc §5.2: {@code on_fail} excludes {@code observe_only} (OLD
 * Sprint 36 D2 §3.2 verbatim — observations belong in projection / trace, not
 * in guardrails).
 */
public record Guardrail(
        String type,
        String onFail,
        Map<String, Object> parameters
) {

    public Guardrail {
        if (type == null || type.isBlank()) {
            throw new IllegalArgumentException("Guardrail.type is required");
        }
        if (onFail == null || onFail.isBlank()) {
            throw new IllegalArgumentException("Guardrail.on_fail is required");
        }
        parameters = parameters == null ? Map.of() : Map.copyOf(parameters);
    }

    @JsonCreator
    public static Guardrail fromYaml(
            @JsonProperty("type") String type,
            @JsonProperty("on_fail") String onFail,
            @JsonProperty("parameters") Map<String, Object> parameters
    ) {
        return new Guardrail(type, onFail, parameters);
    }
}
