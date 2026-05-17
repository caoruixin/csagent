package com.gumtree.csagent.service.runtime.skill;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Sprint 38 unit coverage for {@link SkillRegistry} per Sprint 37 freeze
 * §3.2 selection semantics (decision (b)).
 *
 * <p>Each test constructs an in-memory {@link SkillLoader} stub that returns
 * a hand-built {@link Skill} list, so the registry's selection logic is
 * exercised independently of the on-classpath YAML production fixtures.
 */
class SkillRegistryTest {

    private static SkillLoader loaderReturning(List<Skill> skills) {
        return new SkillLoader() {
            @Override
            public List<Skill> loadAll() {
                return skills;
            }
        };
    }

    private static Skill skill(String name, List<String> phases, List<String> ucs) {
        return new Skill(name, name, phases, ucs,
                List.of(), List.of(), null, false,
                List.of(), null, "x", null, null, null, null);
    }

    @Test
    void select_emptyRegistry_returnsEmpty() {
        SkillRegistry registry = new SkillRegistry(loaderReturning(List.of()));
        assertEquals(Optional.empty(), registry.select("DISCOVER", "UC-A"));
    }

    @Test
    void select_exactPhaseExactUc_returnsSkill() {
        Skill discover = skill("discover_triage", List.of("DISCOVER"), List.of("UC-A"));
        SkillRegistry registry = new SkillRegistry(loaderReturning(List.of(discover)));
        Optional<Skill> selected = registry.select("DISCOVER", "UC-A");
        assertTrue(selected.isPresent());
        assertSame(discover, selected.get());
    }

    @Test
    void select_exactPhaseWithWildcardUcs_returnsSkill_forAnyUc() {
        Skill discover = skill("discover_triage", List.of("DISCOVER"), List.of("*"));
        SkillRegistry registry = new SkillRegistry(loaderReturning(List.of(discover)));

        assertTrue(registry.select("DISCOVER", "UC-A").isPresent());
        assertTrue(registry.select("DISCOVER", "UC-K").isPresent());
        assertTrue(registry.select("DISCOVER", null).isPresent());
        assertSame(discover, registry.select("DISCOVER", "UC-Z").get());
    }

    @Test
    void select_exactMatchPreferredOverWildcard() {
        Skill wild = skill("wild", List.of("RESOLVE"), List.of("*"));
        Skill exact = skill("exact", List.of("RESOLVE"), List.of("UC-A"));
        SkillRegistry registry = new SkillRegistry(loaderReturning(List.of(wild, exact)));

        Optional<Skill> selected = registry.select("RESOLVE", "UC-A");
        assertTrue(selected.isPresent());
        assertEquals("exact", selected.get().name());

        Optional<Skill> wildcardFallback = registry.select("RESOLVE", "UC-B");
        assertTrue(wildcardFallback.isPresent());
        assertEquals("wild", wildcardFallback.get().name());
    }

    @Test
    void select_phaseMiss_returnsEmpty() {
        Skill discover = skill("discover_triage", List.of("DISCOVER"), List.of("*"));
        SkillRegistry registry = new SkillRegistry(loaderReturning(List.of(discover)));
        assertEquals(Optional.empty(), registry.select("CONFIRM", "UC-A"));
        assertEquals(Optional.empty(), registry.select("RESOLVE", "UC-A"));
    }

    @Test
    void select_nullPhase_returnsEmpty() {
        Skill discover = skill("discover_triage", List.of("DISCOVER"), List.of("*"));
        SkillRegistry registry = new SkillRegistry(loaderReturning(List.of(discover)));
        assertEquals(Optional.empty(), registry.select(null, "UC-A"));
    }

    @Test
    void allSkills_returnsRegistrationOrder() {
        Skill a = skill("a", List.of("DISCOVER"), List.of("*"));
        Skill b = skill("b", List.of("CONFIRM"), List.of("*"));
        SkillRegistry registry = new SkillRegistry(loaderReturning(List.of(a, b)));
        assertEquals(List.of(a, b), registry.allSkills());
    }

    @Test
    void skillsForPhase_filtersByApplicablePhases() {
        Skill discover = skill("d", List.of("DISCOVER"), List.of("*"));
        Skill confirm = skill("c", List.of("CONFIRM"), List.of("*"));
        Skill multiphase = skill("m", List.of("DISCOVER", "CONFIRM"), List.of("UC-A"));
        SkillRegistry registry = new SkillRegistry(loaderReturning(
                List.of(discover, confirm, multiphase)));

        assertEquals(List.of(discover, multiphase), registry.skillsForPhase("DISCOVER"));
        assertEquals(List.of(confirm, multiphase), registry.skillsForPhase("CONFIRM"));
        assertTrue(registry.skillsForPhase("RESOLVE").isEmpty());
    }

    @Test
    void registry_wildcardCollisionWithinSamePhase_failsFast() {
        Skill a = skill("a", List.of("DISCOVER"), List.of("*"));
        Skill b = skill("b", List.of("DISCOVER"), List.of("*"));
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> new SkillRegistry(loaderReturning(List.of(a, b))));
        assertTrue(ex.getMessage().contains("collision"));
    }

    @Test
    void registry_exactUcCollisionWithinSamePhase_failsFast() {
        Skill a = skill("a", List.of("RESOLVE"), List.of("UC-A"));
        Skill b = skill("b", List.of("RESOLVE"), List.of("UC-A"));
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> new SkillRegistry(loaderReturning(List.of(a, b))));
        assertTrue(ex.getMessage().contains("collision"));
    }

    @Test
    void registry_disjointUcsWithinSamePhase_coexist() {
        Skill faq = skill("faq", List.of("RESOLVE"), List.of("UC-A", "UC-B"));
        Skill intake = skill("intake", List.of("RESOLVE"), List.of("UC-G", "UC-H"));
        SkillRegistry registry = new SkillRegistry(loaderReturning(List.of(faq, intake)));

        assertEquals("faq", registry.select("RESOLVE", "UC-A").get().name());
        assertEquals("faq", registry.select("RESOLVE", "UC-B").get().name());
        assertEquals("intake", registry.select("RESOLVE", "UC-G").get().name());
        assertEquals("intake", registry.select("RESOLVE", "UC-H").get().name());
        assertFalse(registry.select("RESOLVE", "UC-Z").isPresent());
    }
}
