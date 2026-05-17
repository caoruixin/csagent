package com.gumtree.csagent.service.runtime.skill;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Sprint 38 unit coverage for {@link SkillLoader} per Sprint 37 freeze §3.3
 * (loader semantics) + §2.2 (schema validation contract).
 *
 * <p>{@link #loadAll_productionSkills_loadsFourSkills()} verifies the 4
 * Sprint 38 production YAML files under {@code src/main/resources/skills/}
 * parse cleanly. The remaining tests build minimal in-memory YAML streams
 * and verify fail-fast behaviour for each schema violation.
 */
class SkillLoaderTest {

    private final SkillLoader loader = new SkillLoader();

    private static InputStream yaml(String body) {
        return new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void loadAll_productionSkills_loadsFourSkills() {
        List<Skill> skills = loader.loadAll();
        assertEquals(4, skills.size(),
                "Sprint 38 ships 4 Skill YAMLs (DISCOVER + CONFIRM + ESCALATE + TERMINAL/CLOSE)");

        List<String> names = skills.stream().map(Skill::name).toList();
        assertTrue(names.contains("discover_triage"));
        assertTrue(names.contains("confirm"));
        assertTrue(names.contains("escalate"));
        assertTrue(names.contains("terminal"));

        Skill discover = skills.stream()
                .filter(s -> "discover_triage".equals(s.name())).findFirst().orElseThrow();
        assertEquals(List.of("DISCOVER"), discover.applicablePhases());
        assertEquals(List.of("*"), discover.applicableUseCases());
        assertEquals(List.of("search_knowledge", "classify_use_case"), discover.toolsRequired());
        assertEquals(List.of("alternate_candidate_use_cases", "discover_disambiguation_signals"),
                discover.stateInheritance().softSignalViaProjection());
        assertTrue(discover.guardrails().isEmpty(),
                "Sprint 38 DISCOVER ships with guardrails: [] per design doc §6.2.1");

        Skill terminal = skills.stream()
                .filter(s -> "terminal".equals(s.name())).findFirst().orElseThrow();
        assertEquals(List.of("CLOSE"), terminal.applicablePhases(),
                "OQ-7.1 default: keep CLOSE phase enum; file name terminal.yaml carries the M3+ intent");
    }

    @Test
    void parseAndValidate_minimalValidSkill_succeeds() {
        String body = """
                name: x
                description: x
                applicable_phases: [DISCOVER]
                applicable_use_cases: ['*']
                tools_required: []
                procedure: do thing
                """;
        Skill skill = loader.parseAndValidate(yaml(body), "x.yaml");
        assertNotNull(skill);
        assertEquals("x", skill.name());
        assertFalse(skill.allowInterimMessage());
    }

    @Test
    void parseAndValidate_missingName_failsFast() {
        String body = """
                description: x
                applicable_phases: [DISCOVER]
                applicable_use_cases: ['*']
                tools_required: []
                procedure: x
                """;
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> loader.parseAndValidate(yaml(body), "x.yaml"));
        assertTrue(ex.getMessage().contains("name"));
    }

    @Test
    void parseAndValidate_blankName_failsFast() {
        String body = """
                name: "   "
                description: x
                applicable_phases: [DISCOVER]
                applicable_use_cases: ['*']
                tools_required: []
                procedure: x
                """;
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> loader.parseAndValidate(yaml(body), "x.yaml"));
        assertTrue(ex.getMessage().contains("name"));
    }

    @Test
    void parseAndValidate_missingProcedure_failsFast() {
        String body = """
                name: x
                description: x
                applicable_phases: [DISCOVER]
                applicable_use_cases: ['*']
                tools_required: []
                """;
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> loader.parseAndValidate(yaml(body), "x.yaml"));
        assertTrue(ex.getMessage().contains("procedure"));
    }

    @Test
    void parseAndValidate_unknownPhase_failsFast() {
        String body = """
                name: x
                description: x
                applicable_phases: [UNKNOWN_PHASE]
                applicable_use_cases: ['*']
                tools_required: []
                procedure: x
                """;
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> loader.parseAndValidate(yaml(body), "x.yaml"));
        assertTrue(ex.getMessage().contains("UNKNOWN_PHASE"));
    }

    @Test
    void parseAndValidate_emptyApplicableUseCases_failsFast() {
        String body = """
                name: x
                description: x
                applicable_phases: [DISCOVER]
                applicable_use_cases: []
                tools_required: []
                procedure: x
                """;
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> loader.parseAndValidate(yaml(body), "x.yaml"));
        assertTrue(ex.getMessage().contains("applicable_use_cases"));
    }

    @Test
    void parseAndValidate_wildcardMixedWithExplicitUcs_failsFast() {
        String body = """
                name: x
                description: x
                applicable_phases: [RESOLVE]
                applicable_use_cases: ['*', 'UC-A']
                tools_required: []
                procedure: x
                """;
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> loader.parseAndValidate(yaml(body), "x.yaml"));
        assertTrue(ex.getMessage().contains("wildcard"));
    }

    @Test
    void parseAndValidate_unknownTerminalOutcome_failsFast() {
        String body = """
                name: x
                description: x
                applicable_phases: [DISCOVER]
                applicable_use_cases: ['*']
                tools_required: []
                procedure: x
                valid_terminal_outcomes: [NOT_A_REAL_OUTCOME]
                """;
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> loader.parseAndValidate(yaml(body), "x.yaml"));
        assertTrue(ex.getMessage().contains("NOT_A_REAL_OUTCOME"));
    }

    @Test
    void parseAndValidate_unknownStateInheritanceKey_failsFast() {
        String body = """
                name: x
                description: x
                applicable_phases: [DISCOVER]
                applicable_use_cases: ['*']
                tools_required: []
                procedure: x
                state_inheritance:
                  inherit: [not_a_real_key]
                  reset: []
                  soft_signal_via_projection: []
                """;
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> loader.parseAndValidate(yaml(body), "x.yaml"));
        assertTrue(ex.getMessage().contains("not_a_real_key"));
    }

    @Test
    void parseAndValidate_unknownProjectionSlot_failsFast() {
        String body = """
                name: x
                description: x
                applicable_phases: [DISCOVER]
                applicable_use_cases: ['*']
                tools_required: []
                procedure: x
                state_inheritance:
                  inherit: []
                  reset: []
                  soft_signal_via_projection: [not_a_real_slot]
                """;
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> loader.parseAndValidate(yaml(body), "x.yaml"));
        assertTrue(ex.getMessage().contains("not_a_real_slot"));
    }

    @Test
    void parseAndValidate_unknownOnFailMode_failsFast() {
        String body = """
                name: x
                description: x
                applicable_phases: [RESOLVE]
                applicable_use_cases: ['UC-A']
                tools_required: [record_outcome]
                procedure: x
                guardrails:
                  - type: must_cite_source
                    on_fail: observe_only
                    parameters: {}
                """;
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> loader.parseAndValidate(yaml(body), "x.yaml"));
        assertTrue(ex.getMessage().contains("observe_only"));
    }

    @Test
    void parseAndValidate_malformedYaml_failsFast() {
        String body = "::: this is not yaml :::";
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> loader.parseAndValidate(yaml(body), "x.yaml"));
        assertTrue(ex.getMessage().contains("x.yaml"));
    }
}
