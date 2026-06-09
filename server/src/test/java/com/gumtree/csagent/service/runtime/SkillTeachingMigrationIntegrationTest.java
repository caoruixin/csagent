package com.gumtree.csagent.service.runtime;

import com.gumtree.csagent.service.runtime.skill.Skill;
import com.gumtree.csagent.service.runtime.skill.SkillRegistry;
import com.gumtree.csagent.service.runtime.skill.SkillTestFixtures;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Sprint 40 behavioural-equivalence integration test per Sprint 37 freeze
 * §7.8 (decision (f) §7.1-§7.4).
 *
 * <p>Verifies the teaching extraction from {@code system_prompt.txt} into
 * {@code discover_triage.yaml} {@code procedure} is observable from a Skill
 * envelope projection equivalently to the prior monolithic
 * {@code system_prompt.txt}: the LLM continues to receive the same teaching
 * content post-migration, just sourced from the Skill envelope (the DISCOVER
 * Skill's {@code procedure} → {@code systemInstruction}) rather than the
 * orchestration shell.
 *
 * <p>The test surface is Java-deterministic on prompt composition: classpath
 * read of the post-Sprint-40 {@code system_prompt.txt} + classpath read of
 * the post-Sprint-40 {@code discover_triage.yaml} via the production
 * {@link SkillRegistry}. Per
 * {@code feedback_mocked_llm_cannot_prove_prompt_causal_change.md} this is a
 * Java-composition assertion, NOT an LLM-behaviour claim.
 *
 * <p>Sprint 40 contract §2.5: 5-10 tests covering (a) shell shrinkage, (b)
 * Sprint 23 + decision-tree preservation, (c) envelope-mechanics paragraph
 * present, (d) Sprint 31 paragraph removed from shell + present in DISCOVER
 * Skill, (e) Sprint 33 SLOT description removed from shell + present in
 * DISCOVER Skill, (f) DISCOVER phase guidance bulk replaced by shell pointer
 * + content present in DISCOVER Skill.
 */
class SkillTeachingMigrationIntegrationTest {

    private static final String SYSTEM_PROMPT_PATH = "/prompts/system_prompt.txt";

    private final SkillRegistry registry = SkillTestFixtures.productionRegistry();

    // -------------------- helpers --------------------

    private String readSystemPrompt() {
        try (InputStream in = getClass().getResourceAsStream(SYSTEM_PROMPT_PATH)) {
            assertNotNull(in, "system_prompt.txt must be present on classpath");
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("failed to read " + SYSTEM_PROMPT_PATH, e);
        }
    }

    private Skill discoverSkill() {
        Optional<Skill> selected = registry.select("DISCOVER", "UC-A");
        assertTrue(selected.isPresent(), "DISCOVER Skill must be loadable from production registry");
        return selected.get();
    }

    // -------------------- (a) orchestration-shell shrinkage --------------------

    /**
     * Sprint 40 D-a/D-b/D-c: post-migration system_prompt.txt is materially
     * smaller than pre-migration (101 lines). Conservative gate: < 90 lines.
     * Observed post-Sprint-40: ~80 lines.
     */
    @Test
    void systemPrompt_postSprint40_lineCountIsMateriallyReduced() {
        String shell = readSystemPrompt();
        long lineCount = shell.lines().count();
        assertTrue(lineCount < 90,
                "post-Sprint-40 system_prompt.txt expected < 90 lines (was 101 pre-Sprint-40); got "
                        + lineCount);
    }

    // -------------------- (b) preservation: Sprint 23 already_called --------------------

    /**
     * Sprint 40 §6 #1 + design doc §7.2.1: Sprint 23 already_called teaching
     * stays in the orchestration shell (cross-Skill envelope-mechanics
     * teaching).
     */
    @Test
    void systemPrompt_postSprint40_preservesSprint23AlreadyCalledTeaching() {
        String shell = readSystemPrompt();
        assertTrue(shell.contains("`already_called` projection slot"),
                "Sprint 23 already_called header must remain in shell");
        assertTrue(shell.contains("not to repeat work the runtime has already performed"),
                "Sprint 23 already_called principle line must remain in shell");
    }

    // -------------------- (b) preservation: request_handover decision tree --------------------

    /**
     * Sprint 40 §6 #2 + design doc §7.3: the 23-value canonical
     * escalation_reason decision tree at lines 54-101 (pre-Sprint-40) stays
     * in the shell — cross-Skill canonical enum.
     */
    @Test
    void systemPrompt_postSprint40_preservesRequestHandoverDecisionTree() {
        String shell = readSystemPrompt();
        assertTrue(shell.contains("`request_handover` — choosing the canonical `escalation_reason`"),
                "request_handover decision-tree header must remain in shell");
        assertTrue(shell.contains("ACTIVE-UC TIEBREAKER"),
                "ACTIVE-UC TIEBREAKER block must remain in shell");
        assertTrue(shell.contains("INTAKE COMPLETION (UC-G/H/I/J/K only"),
                "INTAKE COMPLETION block must remain in shell");
        assertTrue(shell.contains("`service_degraded`"),
                "tail enum value service_degraded must remain in shell");
    }

    // -------------------- (c) addition: envelope-mechanics paragraph --------------------

    /**
     * Sprint 40 D-d + design doc §7.3 table row: ADD orchestration-shell
     * paragraph teaching the LLM about Skill envelope mechanics (cross-Skill
     * universal).
     */
    @Test
    void systemPrompt_postSprint40_addsEnvelopeMechanicsParagraph() {
        String shell = readSystemPrompt();
        assertTrue(shell.contains("Skill envelope"),
                "envelope-mechanics paragraph must reference Skill envelope");
        assertTrue(shell.contains("tool whitelist"),
                "envelope-mechanics paragraph must reference tool whitelist");
        assertTrue(shell.contains("Runtime-floor invariants"),
                "envelope-mechanics paragraph must reference Runtime-floor invariants");
        assertTrue(shell.contains("Phase-specific guidance lives in the Skill `procedure`"),
                "envelope-mechanics paragraph must contain the DISCOVER pointer line");
    }

    // -------------------- (d) migration: Sprint 31 alternate_candidate_use_cases --------------------

    /**
     * Sprint 40 D-a + design doc §7.2.2: REMOVE Sprint 31
     * alternate_candidate_use_cases header / lead-in from shell.
     */
    @Test
    void systemPrompt_postSprint40_removesSprint31AlternateCandidateHeader() {
        String shell = readSystemPrompt();
        assertFalse(shell.contains("Alternate candidate use cases (the `alternate_candidate_use_cases` projection slot):"),
                "Sprint 31 alternate_candidate_use_cases header line must be removed from shell");
        assertFalse(shell.contains("This is intake-time evidence; mid-session shifts the intake router did not anticipate"),
                "Sprint 31 trailing principle line must be removed from shell");
    }

    /**
     * Sprint 40 D-a + design doc §7.2.2: ADD Sprint 31 teaching content into
     * discover_triage.yaml procedure (extension).
     */
    @Test
    void discoverSkill_postSprint40_carriesMigratedSprint31Teaching() {
        Skill skill = discoverSkill();
        String procedure = skill.procedure();
        assertTrue(procedure.contains("Alternate candidate use cases (the `alternate_candidate_use_cases` projection slot)"),
                "DISCOVER Skill procedure must carry migrated Sprint 31 paragraph header");
        assertTrue(procedure.contains("the UCs the user's intake message was ambiguous between"),
                "DISCOVER Skill procedure must carry migrated Sprint 31 principle");
    }

    // -------------------- (e) migration: Sprint 33 discover_disambiguation_signals SLOT description --------------------

    /**
     * Sprint 40 D-b + design doc §7.2.3: REMOVE Sprint 33 SLOT description
     * header / sub-field descriptions from shell.
     */
    @Test
    void systemPrompt_postSprint40_removesSprint33DisambiguationSignalsSlotDescription() {
        String shell = readSystemPrompt();
        assertFalse(shell.contains("DISCOVER disambiguation signals (the `discover_disambiguation_signals` projection slot, introduced Sprint 33):"),
                "Sprint 33 SLOT description header must be removed from shell");
        assertFalse(shell.contains("`topic_subject_carries_multiple_candidate_ucs` — true when the form's topic_subject"),
                "Sprint 33 SLOT sub-field description must be removed from shell");
    }

    /**
     * Sprint 40 D-b + design doc §7.2.3 + Sprint 33 cue/slot split closure:
     * ADD Sprint 33 SLOT description into discover_triage.yaml procedure
     * alongside the Sprint 33 cue body migrated in Sprint 38 (per decision
     * (e) §6.2.1).
     */
    @Test
    void discoverSkill_postSprint40_carriesMigratedSprint33SlotDescription() {
        Skill skill = discoverSkill();
        String procedure = skill.procedure();
        assertTrue(procedure.contains("DISCOVER disambiguation signals (the `discover_disambiguation_signals` projection slot, introduced Sprint 33)"),
                "DISCOVER Skill procedure must carry migrated Sprint 33 SLOT description header");
        assertTrue(procedure.contains("`candidate_ucs_for_topic` — the candidate UC list for that topic"),
                "DISCOVER Skill procedure must carry migrated Sprint 33 sub-field description");
        // Sprint 38 already migrated the CUE BODY per design doc §6.2.1; verify it's still present alongside.
        assertTrue(procedure.contains("Sprint 33 ad-status disambiguation cue"),
                "DISCOVER Skill procedure must still carry the Sprint 33 cue body migrated in Sprint 38");
    }

    // -------------------- (f) migration: DISCOVER phase guidance bulk --------------------

    /**
     * Sprint 40 D-c + design doc §7.3: REMOVE DISCOVER phase guidance bulk
     * (pre-Sprint-40 lines 45-52) from shell; the post-migration shell
     * carries only the one-line Skill-pointer in the envelope-mechanics
     * paragraph.
     */
    @Test
    void systemPrompt_postSprint40_removesDiscoverPhaseGuidanceBulk() {
        String shell = readSystemPrompt();
        assertFalse(shell.contains("DISCOVER phase guidance (when `phase_plan.phase == \"DISCOVER\"`):"),
                "DISCOVER phase guidance bulk header must be removed from shell");
        assertFalse(shell.contains("Your job is to identify which use case applies to the customer, not to answer their question yet."),
                "DISCOVER phase guidance lead-in must be removed from shell");
        assertFalse(shell.contains("Only escalate from DISCOVER if the user explicitly requests a human"),
                "DISCOVER phase guidance escalate line must be removed from shell");
    }

    /**
     * Sprint 40 D-c + design doc §7.3: ADD migrated DISCOVER phase guidance
     * principles into discover_triage.yaml procedure.
     */
    @Test
    void discoverSkill_postSprint40_carriesMigratedDiscoverPhaseGuidance() {
        Skill skill = discoverSkill();
        String procedure = skill.procedure();
        assertTrue(procedure.contains("DISCOVER phase guidance refinements"),
                "DISCOVER Skill procedure must carry migrated phase-guidance refinements section");
        assertTrue(procedure.contains("Confidence guidance: >= 0.7"),
                "DISCOVER Skill procedure must carry migrated confidence guidance");
        assertTrue(procedure.contains("ask one focused clarifying question (`user_message` ending with `?`)"),
                "DISCOVER Skill procedure must carry migrated < 0.5 clarification rule");
    }

    // -------------------- composition observability --------------------

    /**
     * Sprint 40 §2.5 contract gate: the projected LLM input for a DISCOVER
     * turn contains BOTH the orchestration-shell content AND the
     * discover_triage Skill envelope — observable to the LLM via the
     * Sprint 38 SkillRegistry-driven composeSkillPhasePlan(...) path.
     */
    @Test
    void llmInputForDiscoverTurn_containsBothShellAndDiscoverSkillEnvelope() {
        String shell = readSystemPrompt();
        Skill discover = discoverSkill();

        // Shell carries Sprint 23 + envelope mechanics + request_handover decision tree.
        assertTrue(shell.contains("`already_called` projection slot"));
        assertTrue(shell.contains("Skill envelope"));
        assertTrue(shell.contains("`request_handover` — choosing the canonical `escalation_reason`"));

        // DISCOVER Skill envelope (procedure → systemInstruction) carries Sprint 31 + Sprint 33 + DISCOVER guidance.
        String procedure = discover.procedure();
        assertEquals("discover_triage", discover.name());
        assertTrue(procedure.contains("alternate_candidate_use_cases"));
        assertTrue(procedure.contains("discover_disambiguation_signals"));
        assertTrue(procedure.contains("Confidence guidance"));

        // The two surfaces together carry the same teaching the LLM observed pre-Sprint-40 from monolithic system_prompt.txt.
    }
}
