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
 * Sprint 43 (S-Eval-2, NEW Milestone M3-Eval sub-sprint 2) coverage for the
 * NEW {@code critical_steps} block on Skill YAMLs.
 *
 * <p>Verifies:
 * <ul>
 *   <li>All 6 production Skill YAMLs load with {@code criticalSteps} = empty
 *       list (parity with the M2 envelope; S-Eval-3 populates the content).</li>
 *   <li>A synthetic Skill YAML carrying a valid {@code critical_steps:} block
 *       parses to a {@link CriticalStep} list with all 5 fields preserved.</li>
 *   <li>Each of the 6 schema-violation negative cases (missing id / missing
 *       desc / missing trace_check / empty mandatory_for / unknown UC in
 *       mandatory_for / missing severity / invalid severity / duplicate id)
 *       fails {@link SkillLoader#validate(Skill, String)} fail-fast with a
 *       clear error message.</li>
 *   <li>The {@link CriticalStep.Severity} enum is the only allowed range;
 *       the loader rejects any value outside {@code MANDATORY | ADVISORY}.</li>
 * </ul>
 */
class SkillCriticalStepsLoadingTest {

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

    private static String baseYaml(String criticalStepsBlock) {
        // Minimal valid Skill envelope plus a configurable critical_steps block.
        // We point at UC-A (covered by UseCaseRegistryService) and DISCOVER
        // phase to keep the rest of the schema satisfied.
        return ""
                + "name: synthetic_skill_for_critical_steps_test\n"
                + "description: Synthetic Skill for Sprint 43 (S-Eval-2) critical_steps loader tests.\n"
                + "applicable_phases:\n"
                + "  - DISCOVER\n"
                + "applicable_use_cases:\n"
                + "  - '*'\n"
                + "tools_required:\n"
                + "  - search_knowledge\n"
                + "required_context_keys: []\n"
                + "max_tool_steps: 3\n"
                + "allow_interim_message: false\n"
                + "valid_terminal_outcomes:\n"
                + "  - FINAL_ANSWER\n"
                + "objective: synthetic\n"
                + "procedure: synthetic procedure body for testing\n"
                + "grounding_instruction: synthetic grounding\n"
                + "escalation_policy: synthetic escalation\n"
                + "guardrails: []\n"
                + "state_inheritance:\n"
                + "  inherit: []\n"
                + "  reset: []\n"
                + "  soft_signal_via_projection: []\n"
                + criticalStepsBlock;
    }

    @Test
    void loadAll_productionSkills_haveEmptyCriticalSteps_atSEval2_close() {
        List<Skill> skills = loader.loadAll();
        assertEquals(6, skills.size(),
                "6 production Skill YAMLs (M2 close) — Sprint 43 (S-Eval-2) does not change the file count");

        for (Skill s : skills) {
            assertNotNull(s.criticalSteps(), "criticalSteps() must never be null after compact-ctor normalization");
            assertTrue(s.criticalSteps().isEmpty(),
                    "S-Eval-2 ships NO critical_steps content on any of the 6 Skills; populated in S-Eval-3. "
                            + "Skill=" + s.name() + " has " + s.criticalSteps().size() + " critical_steps");
        }
    }

    @Test
    void parse_validCriticalStepsBlock_parsesAllFields() {
        String block = ""
                + "critical_steps:\n"
                + "  - id: search_before_answer\n"
                + "    desc: Retrieve a knowledge article before answering a UC-A visibility question.\n"
                + "    trace_check: accumulated_tool_results.search_knowledge\n"
                + "    mandatory_for:\n"
                + "      - UC-A\n"
                + "    severity: mandatory\n"
                + "  - id: resolve_after_search\n"
                + "    desc: Resolve the chosen article before drafting the customer-facing reply.\n"
                + "    trace_check: tool_event_seq(search_knowledge) < tool_event_seq(resolve_article)\n"
                + "    mandatory_for:\n"
                + "      - UC-A\n"
                + "    severity: advisory\n";
        Skill skill = loader.parseAndValidate(yaml(baseYaml(block)), "synthetic_valid.yaml");

        assertEquals(2, skill.criticalSteps().size());

        CriticalStep s0 = skill.criticalSteps().get(0);
        assertEquals("search_before_answer", s0.id());
        assertEquals("Retrieve a knowledge article before answering a UC-A visibility question.",
                s0.desc());
        assertEquals("accumulated_tool_results.search_knowledge", s0.traceCheck());
        assertEquals(List.of("UC-A"), s0.mandatoryFor());
        assertEquals(CriticalStep.Severity.MANDATORY, s0.severity());

        CriticalStep s1 = skill.criticalSteps().get(1);
        assertEquals("resolve_after_search", s1.id());
        assertEquals(CriticalStep.Severity.ADVISORY, s1.severity());
        assertEquals("tool_event_seq(search_knowledge) < tool_event_seq(resolve_article)",
                s1.traceCheck());
    }

    @Test
    void parse_emptyCriticalStepsBlock_isAccepted() {
        // Explicit empty list at the YAML level should round-trip to an
        // empty list on the record (parity with the absent-block case).
        Skill skill = loader.parseAndValidate(
                yaml(baseYaml("critical_steps: []\n")), "synthetic_empty.yaml");
        assertTrue(skill.criticalSteps().isEmpty());
    }

    @Test
    void parse_absentCriticalStepsBlock_defaultsToEmptyList() {
        Skill skill = loader.parseAndValidate(yaml(baseYaml("")), "synthetic_absent.yaml");
        assertTrue(skill.criticalSteps().isEmpty());
    }

    // -----------------------------------------------------------------
    // Negative cases — fail-fast schema validation per allowlist precedent.
    // -----------------------------------------------------------------

    @Test
    void validate_criticalStep_missingId_throws() {
        String block = ""
                + "critical_steps:\n"
                + "  - desc: missing id\n"
                + "    trace_check: accumulated_tool_results.search_knowledge\n"
                + "    mandatory_for: [UC-A]\n"
                + "    severity: mandatory\n";
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> loader.parseAndValidate(yaml(baseYaml(block)), "missing_id.yaml"));
        assertTrue(ex.getMessage().contains("critical_steps[].id"),
                "expected error to name 'critical_steps[].id' but got: " + ex.getMessage());
    }

    @Test
    void validate_criticalStep_blankDesc_throws() {
        String block = ""
                + "critical_steps:\n"
                + "  - id: s1\n"
                + "    desc: ''\n"
                + "    trace_check: accumulated_tool_results.search_knowledge\n"
                + "    mandatory_for: [UC-A]\n"
                + "    severity: mandatory\n";
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> loader.parseAndValidate(yaml(baseYaml(block)), "blank_desc.yaml"));
        assertTrue(ex.getMessage().contains("desc"),
                "expected error to mention 'desc' but got: " + ex.getMessage());
    }

    @Test
    void validate_criticalStep_missingTraceCheck_throws() {
        String block = ""
                + "critical_steps:\n"
                + "  - id: s1\n"
                + "    desc: ok\n"
                + "    mandatory_for: [UC-A]\n"
                + "    severity: mandatory\n";
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> loader.parseAndValidate(yaml(baseYaml(block)), "missing_trace_check.yaml"));
        assertTrue(ex.getMessage().contains("trace_check"),
                "expected error to mention 'trace_check' but got: " + ex.getMessage());
    }

    @Test
    void validate_criticalStep_emptyMandatoryFor_throws() {
        String block = ""
                + "critical_steps:\n"
                + "  - id: s1\n"
                + "    desc: ok\n"
                + "    trace_check: accumulated_tool_results.search_knowledge\n"
                + "    mandatory_for: []\n"
                + "    severity: mandatory\n";
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> loader.parseAndValidate(yaml(baseYaml(block)), "empty_mandatory_for.yaml"));
        assertTrue(ex.getMessage().contains("mandatory_for"),
                "expected error to mention 'mandatory_for' but got: " + ex.getMessage());
    }

    @Test
    void validate_criticalStep_unknownUcInMandatoryFor_throws() {
        String block = ""
                + "critical_steps:\n"
                + "  - id: s1\n"
                + "    desc: ok\n"
                + "    trace_check: accumulated_tool_results.search_knowledge\n"
                + "    mandatory_for: [UC-DOES-NOT-EXIST]\n"
                + "    severity: mandatory\n";
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> loader.parseAndValidate(yaml(baseYaml(block)), "unknown_uc.yaml"));
        assertTrue(ex.getMessage().contains("UC-DOES-NOT-EXIST")
                        || ex.getMessage().contains("not registered"),
                "expected error to mention the unknown UC or 'not registered' but got: "
                        + ex.getMessage());
    }

    @Test
    void validate_criticalStep_invalidSeverity_throws() {
        // The Severity enum's fromYaml() raises IllegalArgumentException for
        // values outside MANDATORY/ADVISORY, which Jackson wraps into a
        // deserialization failure that the loader rethrows as
        // IllegalStateException ("failed to parse YAML").
        String block = ""
                + "critical_steps:\n"
                + "  - id: s1\n"
                + "    desc: ok\n"
                + "    trace_check: accumulated_tool_results.search_knowledge\n"
                + "    mandatory_for: [UC-A]\n"
                + "    severity: SEVERE\n";
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> loader.parseAndValidate(yaml(baseYaml(block)), "invalid_severity.yaml"));
        String msg = ex.getMessage();
        assertTrue(msg.contains("severity") || msg.contains("SEVERE")
                        || msg.contains("failed to parse YAML"),
                "expected error related to severity but got: " + msg);
    }

    @Test
    void validate_criticalStep_missingSeverity_throws() {
        String block = ""
                + "critical_steps:\n"
                + "  - id: s1\n"
                + "    desc: ok\n"
                + "    trace_check: accumulated_tool_results.search_knowledge\n"
                + "    mandatory_for: [UC-A]\n";
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> loader.parseAndValidate(yaml(baseYaml(block)), "missing_severity.yaml"));
        assertTrue(ex.getMessage().contains("severity"),
                "expected error to mention 'severity' but got: " + ex.getMessage());
    }

    @Test
    void validate_criticalStep_duplicateId_throws() {
        String block = ""
                + "critical_steps:\n"
                + "  - id: dup_id\n"
                + "    desc: first\n"
                + "    trace_check: accumulated_tool_results.search_knowledge\n"
                + "    mandatory_for: [UC-A]\n"
                + "    severity: mandatory\n"
                + "  - id: dup_id\n"
                + "    desc: second\n"
                + "    trace_check: accumulated_tool_results.classify_use_case\n"
                + "    mandatory_for: [UC-A]\n"
                + "    severity: advisory\n";
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> loader.parseAndValidate(yaml(baseYaml(block)), "duplicate_id.yaml"));
        assertTrue(ex.getMessage().contains("duplicate")
                        && ex.getMessage().contains("dup_id"),
                "expected error to mention 'duplicate' and 'dup_id' but got: "
                        + ex.getMessage());
    }

    @Test
    void severity_enum_acceptsLowerCaseYaml() {
        // Sanity: YAML uses lower-case ('mandatory'/'advisory') by Skill-YAML
        // convention; the fromYaml() upper-cases before enum lookup.
        String block = ""
                + "critical_steps:\n"
                + "  - id: s1\n"
                + "    desc: ok\n"
                + "    trace_check: accumulated_tool_results.search_knowledge\n"
                + "    mandatory_for: [UC-A]\n"
                + "    severity: mandatory\n"
                + "  - id: s2\n"
                + "    desc: ok2\n"
                + "    trace_check: accumulated_tool_results.classify_use_case\n"
                + "    mandatory_for: [UC-A]\n"
                + "    severity: advisory\n";
        Skill skill = loader.parseAndValidate(yaml(baseYaml(block)), "lowercase_severity.yaml");
        assertEquals(CriticalStep.Severity.MANDATORY, skill.criticalSteps().get(0).severity());
        assertEquals(CriticalStep.Severity.ADVISORY, skill.criticalSteps().get(1).severity());
    }

    @Test
    void backwardCompat_secondaryConstructor_defaultsCriticalStepsToEmptyList() {
        // The Sprint 43 secondary constructor lets pre-S-Eval-2 call sites
        // (test fixtures, ad-hoc construction) skip the new arg and get an
        // empty list as default. Verifies the bundle does NOT need to touch
        // 12+ existing test ctor sites.
        Skill skill = new Skill(
                "legacy_construction",
                "Skill built without the new critical_steps arg",
                List.of("DISCOVER"),
                List.of("*"),
                List.of("search_knowledge"),
                List.of(),
                3,
                false,
                List.of("FINAL_ANSWER"),
                "obj",
                "proc",
                "grounding",
                "escalation",
                List.of(),
                StateInheritance.EMPTY);
        assertNotNull(skill.criticalSteps());
        assertTrue(skill.criticalSteps().isEmpty());
        // Sanity — other existing fields still round-trip through the
        // secondary ctor.
        assertEquals("legacy_construction", skill.name());
        assertFalse(skill.allowInterimMessage());
    }
}
