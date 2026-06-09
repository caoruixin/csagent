package com.gumtree.csagent.service.runtime.skill;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Sprint 38 unit coverage for {@link Skill} per Sprint 37 freeze §2.1
 * (decision (a) — Skill data model).
 *
 * <p>Verifies record field accessors, compact-constructor defensive normalization
 * (null lists → {@link List#of()}; null {@link StateInheritance} →
 * {@link StateInheritance#EMPTY}), and the {@link Skill#appliesTo(String, String)}
 * selection semantics per design doc §3.2.
 */
class SkillTest {

    @Test
    void skill_constructedWithAllFields_exposesViaAccessors() {
        Guardrail g = new Guardrail("must_cite_source", "reject_with_hint",
                Map.of("outcome_class", "resolve"));
        StateInheritance si = new StateInheritance(
                List.of("customer_context"),
                List.of(),
                List.of("alternate_candidate_use_cases"));

        Skill skill = new Skill(
                "discover_triage",
                "DISCOVER Skill",
                List.of("DISCOVER"),
                List.of("*"),
                List.of("search_knowledge", "classify_use_case"),
                List.of("form_context"),
                3,
                false,
                List.of("FINAL_ANSWER"),
                "Identify UC",
                "Procedure body",
                "Grounding",
                "Escalation",
                List.of(g),
                si);

        assertEquals("discover_triage", skill.name());
        assertEquals("DISCOVER Skill", skill.description());
        assertEquals(List.of("DISCOVER"), skill.applicablePhases());
        assertEquals(List.of("*"), skill.applicableUseCases());
        assertEquals(List.of("search_knowledge", "classify_use_case"), skill.toolsRequired());
        assertEquals(List.of("form_context"), skill.requiredContextKeys());
        assertEquals(3, skill.maxToolSteps());
        assertFalse(skill.allowInterimMessage());
        assertEquals(List.of("FINAL_ANSWER"), skill.validTerminalOutcomes());
        assertEquals("Identify UC", skill.objective());
        assertEquals("Procedure body", skill.procedure());
        assertEquals("Grounding", skill.groundingInstruction());
        assertEquals("Escalation", skill.escalationPolicy());
        assertEquals(List.of(g), skill.guardrails());
        assertSame(si, skill.stateInheritance());
    }

    @Test
    void skill_compactConstructor_normalizesNullListsToEmpty() {
        Skill skill = new Skill(
                "x", "x", null, null, null, null, null, false,
                null, null, "x", null, null, null, null);

        assertEquals(List.of(), skill.applicablePhases());
        assertEquals(List.of(), skill.applicableUseCases());
        // toolsRequired is intentionally NOT normalized: per Sprint 38 fix
        // iteration #1 (Codex Blocking Finding 1 sub-gap #1a), the compact
        // constructor leaves a null toolsRequired as null so SkillLoader can
        // fail-fast on a missing tools_required key per design doc §2.2.
        assertNull(skill.toolsRequired());
        assertEquals(List.of(), skill.requiredContextKeys());
        assertEquals(List.of(), skill.validTerminalOutcomes());
        assertEquals(List.of(), skill.guardrails());
        assertNotNull(skill.stateInheritance());
        assertEquals(StateInheritance.EMPTY, skill.stateInheritance());
    }

    @Test
    void skill_compactConstructor_emptyToolsRequiredIsPreserved() {
        // Explicit empty list (tools_required: [] in YAML) is acceptable per
        // design doc §2.1 (Skills with no tool calls). Distinct from null.
        Skill skill = new Skill(
                "x", "x", List.of("DISCOVER"), List.of("*"),
                List.of(), List.of(), null, false,
                List.of(), null, "x", null, null, null, null);
        assertNotNull(skill.toolsRequired());
        assertTrue(skill.toolsRequired().isEmpty());
    }

    @Test
    void appliesTo_exactPhaseAndExactUseCase_isTrue() {
        Skill skill = new Skill("s", "s",
                List.of("RESOLVE"), List.of("UC-A", "UC-B"),
                List.of(), List.of(), null, false,
                List.of(), null, "x", null, null, null, null);
        assertTrue(skill.appliesTo("RESOLVE", "UC-A"));
        assertTrue(skill.appliesTo("RESOLVE", "UC-B"));
    }

    @Test
    void appliesTo_exactPhaseWithWildcardUcs_isTrueForAnyUc() {
        Skill skill = new Skill("s", "s",
                List.of("DISCOVER"), List.of("*"),
                List.of(), List.of(), null, false,
                List.of(), null, "x", null, null, null, null);
        assertTrue(skill.appliesTo("DISCOVER", "UC-A"));
        assertTrue(skill.appliesTo("DISCOVER", "UC-Z"));
        assertTrue(skill.appliesTo("DISCOVER", null));
    }

    @Test
    void appliesTo_phaseMismatch_isFalse() {
        Skill skill = new Skill("s", "s",
                List.of("CONFIRM"), List.of("*"),
                List.of(), List.of(), null, false,
                List.of(), null, "x", null, null, null, null);
        assertFalse(skill.appliesTo("RESOLVE", "UC-A"));
        assertFalse(skill.appliesTo("DISCOVER", "UC-A"));
    }

    @Test
    void appliesTo_ucMismatchAndNoWildcard_isFalse() {
        Skill skill = new Skill("s", "s",
                List.of("RESOLVE"), List.of("UC-A"),
                List.of(), List.of(), null, false,
                List.of(), null, "x", null, null, null, null);
        assertFalse(skill.appliesTo("RESOLVE", "UC-X"));
    }

    @Test
    void appliesTo_nullPhase_isFalse() {
        Skill skill = new Skill("s", "s",
                List.of("DISCOVER"), List.of("*"),
                List.of(), List.of(), null, false,
                List.of(), null, "x", null, null, null, null);
        assertFalse(skill.appliesTo(null, "UC-A"));
    }

    @Test
    void appliesTo_nullUseCaseWithoutWildcard_isFalse() {
        Skill skill = new Skill("s", "s",
                List.of("RESOLVE"), List.of("UC-A"),
                List.of(), List.of(), null, false,
                List.of(), null, "x", null, null, null, null);
        assertFalse(skill.appliesTo("RESOLVE", null));
    }
}
