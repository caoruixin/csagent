package com.gumtree.csagent.service.runtime.skill;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Per-Skill state-bus posture for UC switch, per Sprint 37 freeze §10.2.
 *
 * <p>Names what session state the Skill {@code inherit}s, {@code reset}s, or
 * sees as {@code soft_signal_via_projection} from the prior Skill.
 *
 * <p>Sprint 38 populates this field on each Skill YAML (schema-defined). It
 * is NOT yet enforced at session-state-bus boundary — Sprint 41 ships
 * {@code SkillStateBus} which reads these declarations and mediates
 * cross-Skill state visibility per design doc §10.3.
 *
 * <p>Allowed state keys (validated by {@link SkillLoader} per design doc
 * §10.2):
 * <ul>
 *   <li>{@code customer_context}</li>
 *   <li>{@code accumulated_tool_results}</li>
 *   <li>{@code intake_fields_partial}</li>
 * </ul>
 *
 * <p>Allowed projection slot names for {@code soft_signal_via_projection}:
 * <ul>
 *   <li>{@code alternate_candidate_use_cases}</li>
 *   <li>{@code discover_disambiguation_signals}</li>
 *   <li>{@code prior_use_case_carry} (NEW M2 Sprint 41 slot)</li>
 * </ul>
 */
public record StateInheritance(
        List<String> inherit,
        List<String> reset,
        List<String> softSignalViaProjection
) {

    public static final StateInheritance EMPTY =
            new StateInheritance(List.of(), List.of(), List.of());

    public StateInheritance {
        inherit = inherit == null ? List.of() : List.copyOf(inherit);
        reset = reset == null ? List.of() : List.copyOf(reset);
        softSignalViaProjection = softSignalViaProjection == null
                ? List.of()
                : List.copyOf(softSignalViaProjection);
    }

    @JsonCreator
    public static StateInheritance fromYaml(
            @JsonProperty("inherit") List<String> inherit,
            @JsonProperty("reset") List<String> reset,
            @JsonProperty("soft_signal_via_projection") List<String> softSignalViaProjection
    ) {
        return new StateInheritance(inherit, reset, softSignalViaProjection);
    }
}
