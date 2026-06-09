package com.gumtree.csagent.service.runtime.skill;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Holds all loaded Skills + exposes {@code select(phase, useCase)} per Sprint
 * 37 freeze §3 decision (b).
 *
 * <p>Selection semantics per design doc §3.2:
 * <ol>
 *   <li>Exact match: a Skill whose {@code applicable_phases} contains the
 *       phase AND whose {@code applicable_use_cases} contains the active UC
 *       (literal).</li>
 *   <li>Wildcard UC match: a Skill whose {@code applicable_phases} contains
 *       the phase AND whose {@code applicable_use_cases == ["*"]}.</li>
 *   <li>Fallback: {@link Optional#empty()}; caller falls back to the legacy
 *       {@code PhaseEvaluator.plan} branch.</li>
 * </ol>
 *
 * <p>The null-on-miss / fallback-to-legacy semantics supports the Sprint 38 →
 * Sprint 39 incremental migration window: Sprint 38 ships 4 of 6 phase Skills
 * (DISCOVER + CONFIRM + ESCALATE + CLOSE); the 2 RESOLVE phases stay on the
 * legacy Java-string branch until Sprint 39 ships their Skills.
 *
 * <p>Collision invariant per design doc §3.1: two Skills MAY share a phase
 * but MUST NOT collide on {@code (phase, useCase)} after wildcard expansion.
 * Sprint 38's 4 Skills each name a distinct phase so collision is impossible
 * by construction; Sprint 39 RESOLVE Skills are scoped by UC path so they
 * partition the RESOLVE phase without overlap.
 */
@Slf4j
@Component
public class SkillRegistry {

    private final List<Skill> skills;
    private final Map<String, Skill> wildcardByPhase;
    private final Map<String, Map<String, Skill>> exactByPhaseUc;

    public SkillRegistry(SkillLoader loader) {
        Objects.requireNonNull(loader, "SkillLoader is required");
        this.skills = List.copyOf(loader.loadAll());
        this.wildcardByPhase = new HashMap<>();
        this.exactByPhaseUc = new HashMap<>();
        indexSkills();
        log.info("SkillRegistry: indexed {} Skill(s) ({} wildcard-by-phase, {} exact-phase-uc)",
                skills.size(),
                wildcardByPhase.size(),
                exactByPhaseUc.values().stream().mapToInt(Map::size).sum());
    }

    /**
     * Select the Skill for the given {@code (phase, useCase)} tuple per
     * design doc §3.2. Returns {@link Optional#empty()} if neither exact-match
     * nor wildcard-match applies; caller falls through to the legacy
     * PhaseEvaluator branch.
     */
    public Optional<Skill> select(String phase, String useCase) {
        if (phase == null) {
            return Optional.empty();
        }

        Map<String, Skill> exact = exactByPhaseUc.get(phase);
        if (exact != null && useCase != null) {
            Skill literal = exact.get(useCase);
            if (literal != null) {
                return Optional.of(literal);
            }
        }

        Skill wildcard = wildcardByPhase.get(phase);
        if (wildcard != null) {
            return Optional.of(wildcard);
        }

        return Optional.empty();
    }

    /** All Skills in registration order. Diagnostic / admin tooling use only. */
    public List<Skill> allSkills() {
        return skills;
    }

    /** Skills whose {@code applicable_phases} contains the given phase. */
    public List<Skill> skillsForPhase(String phase) {
        return skills.stream()
                .filter(s -> s.applicablePhases().contains(phase))
                .collect(Collectors.toUnmodifiableList());
    }

    private void indexSkills() {
        for (Skill skill : skills) {
            for (String phase : skill.applicablePhases()) {
                if (skill.applicableUseCases().contains(Skill.UC_WILDCARD)) {
                    Skill prior = wildcardByPhase.putIfAbsent(phase, skill);
                    if (prior != null) {
                        throw new IllegalStateException(
                                "SkillRegistry: collision on wildcard for phase=" + phase
                                        + " between '" + prior.name() + "' and '" + skill.name() + "'");
                    }
                } else {
                    Map<String, Skill> ucMap = exactByPhaseUc
                            .computeIfAbsent(phase, p -> new HashMap<>());
                    for (String uc : skill.applicableUseCases()) {
                        Skill prior = ucMap.putIfAbsent(uc, skill);
                        if (prior != null) {
                            throw new IllegalStateException(
                                    "SkillRegistry: collision on (phase=" + phase + ", uc=" + uc
                                            + ") between '" + prior.name() + "' and '" + skill.name() + "'");
                        }
                    }
                }
            }
        }
    }
}
