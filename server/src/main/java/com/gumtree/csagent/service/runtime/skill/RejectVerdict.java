package com.gumtree.csagent.service.runtime.skill;

import java.util.Map;

/**
 * Verdict returned by {@link SkillGuardrailDispatcher} when a Skill guardrail
 * rejects a tool call or an outcome persistence per Sprint 37 freeze §9.1.
 *
 * <p>Carries the canonical {@code predicateName} (label persisted to
 * {@code accumulated_tool_results.<tool>.error}), the LLM-visible {@code hint}
 * the next iteration sees, and a {@code trace} map of the predicate-input data
 * surface for post-hoc trace review (Sprint 6/7/11 trace shape preserved per
 * design doc §9.4 + {@code skill_name} added at M2).
 *
 * <p>Constructed only by per-predicate handlers in {@link SkillGuardrailDispatcher};
 * not part of the LLM-visible projection schema.
 */
public record RejectVerdict(
        String predicateName,
        String hint,
        Map<String, Object> trace
) {
    public RejectVerdict {
        trace = trace == null ? Map.of() : Map.copyOf(trace);
    }
}
