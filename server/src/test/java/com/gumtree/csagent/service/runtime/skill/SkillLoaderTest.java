package com.gumtree.csagent.service.runtime.skill;

import com.gumtree.csagent.service.runtime.UseCaseRegistryService;
import org.junit.jupiter.api.BeforeAll;
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
 *
 * <p>Sprint 38 fix iteration #1 (Codex Blocking Finding 1) adds 5 negative
 * cases for the schema-validation gaps in design doc §2.2 (missing
 * {@code tools_required}, unknown tool name, explicit unknown UC, unknown
 * guardrail type) plus a positive case confirming the UC-registry check
 * accepts a registered UC.
 */
class SkillLoaderTest {

    private static UseCaseRegistryService useCaseRegistry;
    private static SkillLoader loader;

    @BeforeAll
    static void initLoader() {
        useCaseRegistry = new UseCaseRegistryService();
        useCaseRegistry.init();
        loader = new SkillLoader(useCaseRegistry);
    }

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

    /**
     * Sprint 38 fix iteration #1 sub-gap #1a: omitting the {@code tools_required}
     * key entirely from YAML must fail-fast. The {@link Skill} record's compact
     * constructor leaves a null {@code toolsRequired} as null so the loader's
     * null-check is reachable per design doc §2.2.
     */
    @Test
    void parseAndValidate_missingToolsRequiredField_failsFast() {
        String body = """
                name: x
                description: x
                applicable_phases: [DISCOVER]
                applicable_use_cases: ['*']
                procedure: x
                """;
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> loader.parseAndValidate(yaml(body), "x.yaml"));
        assertTrue(ex.getMessage().contains("tools_required"),
                "expected tools_required schema error, got: " + ex.getMessage());
    }

    /**
     * Sprint 38 fix iteration #1 sub-gap #1b: {@code tools_required} entries
     * must be canonical tool names per design doc §2.2.
     */
    @Test
    void parseAndValidate_unknownToolName_failsFast() {
        String body = """
                name: x
                description: x
                applicable_phases: [DISCOVER]
                applicable_use_cases: ['*']
                tools_required: [some_nonexistent_tool]
                procedure: x
                """;
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> loader.parseAndValidate(yaml(body), "x.yaml"));
        assertTrue(ex.getMessage().contains("some_nonexistent_tool"),
                "expected unknown-tool schema error, got: " + ex.getMessage());
    }

    /**
     * Sprint 38 fix iteration #1 sub-gap #1c: explicit (non-wildcard)
     * {@code applicable_use_cases} entries must reference UCs registered in
     * {@link UseCaseRegistryService} per design doc §2.2.
     */
    @Test
    void parseAndValidate_explicitUnknownUseCase_failsFast() {
        String body = """
                name: x
                description: x
                applicable_phases: [RESOLVE]
                applicable_use_cases: ['UC-NONEXISTENT']
                tools_required: [search_knowledge]
                procedure: x
                """;
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> loader.parseAndValidate(yaml(body), "x.yaml"));
        assertTrue(ex.getMessage().contains("UC-NONEXISTENT"),
                "expected unknown-UC schema error, got: " + ex.getMessage());
    }

    /**
     * Sprint 38 fix iteration #1 sub-gap #1c (positive): explicit non-wildcard
     * UC that IS registered in {@link UseCaseRegistryService} must pass.
     */
    @Test
    void parseAndValidate_explicitKnownUseCase_passes() {
        String body = """
                name: x
                description: x
                applicable_phases: [RESOLVE]
                applicable_use_cases: ['UC-A']
                tools_required: [search_knowledge]
                procedure: x
                """;
        Skill skill = loader.parseAndValidate(yaml(body), "x.yaml");
        assertNotNull(skill);
        assertEquals(List.of("UC-A"), skill.applicableUseCases());
    }

    /**
     * Sprint 38 fix iteration #1 sub-gap #1d: guardrail {@code type} entries
     * must match a known dispatcher predicate type per design doc §2.2 / §5.2.
     */
    @Test
    void parseAndValidate_unknownGuardrailType_failsFast() {
        String body = """
                name: x
                description: x
                applicable_phases: [RESOLVE]
                applicable_use_cases: ['UC-A']
                tools_required: [record_outcome]
                procedure: x
                guardrails:
                  - type: nonexistent_predicate
                    on_fail: reject_with_hint
                    parameters: {}
                """;
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> loader.parseAndValidate(yaml(body), "x.yaml"));
        assertTrue(ex.getMessage().contains("nonexistent_predicate"),
                "expected unknown-guardrail-type schema error, got: " + ex.getMessage());
    }
}
