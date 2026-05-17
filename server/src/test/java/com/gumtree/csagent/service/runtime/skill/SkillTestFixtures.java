package com.gumtree.csagent.service.runtime.skill;

/**
 * Test fixtures for the Sprint 38 Skill Registry abstraction.
 *
 * <p>Exposes a {@code productionRegistry()} helper that constructs a real
 * {@link SkillRegistry} from the 4 production Skill YAML files in
 * {@code server/src/main/resources/skills/}. Used by existing PhaseEvaluator
 * tests so the migrated phases (DISCOVER / CONFIRM / CLOSE / ESCALATE) keep
 * the same observational behaviour they had pre-migration without each test
 * needing to know about the loader internals.
 */
public final class SkillTestFixtures {

    private SkillTestFixtures() {}

    /** Real SkillRegistry built from the on-classpath production Skill YAMLs. */
    public static SkillRegistry productionRegistry() {
        return new SkillRegistry(new SkillLoader());
    }
}
