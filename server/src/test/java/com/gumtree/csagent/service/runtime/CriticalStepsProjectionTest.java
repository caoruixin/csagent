package com.gumtree.csagent.service.runtime;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gumtree.csagent.model.BotSession;
import com.gumtree.csagent.model.PhasePlan;
import com.gumtree.csagent.model.TerminalOutcome;
import com.gumtree.csagent.service.runtime.skill.CriticalStep;
import com.gumtree.csagent.service.runtime.skill.Guardrail;
import com.gumtree.csagent.service.runtime.skill.Skill;
import com.gumtree.csagent.service.runtime.skill.SkillRegistry;
import com.gumtree.csagent.service.runtime.skill.StateInheritance;
import com.gumtree.csagent.service.tools.ToolPolicyEnforcer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/**
 * Sprint 43 (S-Eval-2, NEW Milestone M3-Eval sub-sprint 2) coverage for the
 * NEW {@code phase_plan.critical_steps} projection slot rendered by
 * {@link ContextProjectionBuilder} when the active Skill (resolved via
 * {@link SkillRegistry#select(String, String)}) carries a populated
 * {@code criticalSteps} list.
 *
 * <p>Empty {@code criticalSteps[]} → NO {@code critical_steps} key in
 * projection output (parity preservation; existing M2 prompt-composition
 * golden tests stay green). Populated → list-of-objects with stable
 * {@code id} + {@code desc} verbatim from the Skill YAML; rendered as
 * sibling immediately after {@code system_instruction} inside
 * {@code phase_plan}.
 *
 * <p>This test mocks {@link SkillRegistry} via Mockito so the synthetic
 * Skill+criticalSteps can be exercised without touching
 * {@code SkillRegistry.select(...)} (M2 §6 hard fence #5 preserved).
 */
@ExtendWith(MockitoExtension.class)
class CriticalStepsProjectionTest {

    private ContextProjectionBuilder builder;
    private ObjectMapper objectMapper;

    @Mock
    private UseCaseRegistryService useCaseRegistry;

    @Mock
    private ControlPolicyService controlPolicy;

    @Mock
    private ToolPolicyEnforcer toolPolicyEnforcer;

    @Mock
    private SkillRegistry skillRegistry;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        builder = new ContextProjectionBuilder(objectMapper, useCaseRegistry,
                controlPolicy, toolPolicyEnforcer, skillRegistry);
        builder.initToolSchemas();
    }

    private BotSession resolveSessionUcA() {
        return BotSession.builder()
                .sessionId("test-session-critical-steps")
                .activeUseCase("UC-A")
                .currentPhase("RESOLVE")
                .handlingState("BOT_HANDLING")
                .totalBotTurns(0)
                .clarificationCount(0)
                .faqMissCount(0)
                .repeatedActionCount(0)
                .build();
    }

    private PhasePlan resolveUcAPlan() {
        return PhasePlan.builder()
                .phase("RESOLVE")
                .useCase("UC-A")
                .objective("answer the visibility question")
                .allowedTools(List.of("search_knowledge", "resolve_article"))
                .maxToolSteps(3)
                .systemInstruction("You are answering a UC-A visibility question. Use search_knowledge first.")
                .validTerminalOutcomes(Set.of(
                        TerminalOutcome.FINAL_ANSWER, TerminalOutcome.ESCALATE))
                .build();
    }

    private Skill skillWithSteps(List<CriticalStep> steps) {
        return new Skill(
                "resolve_faq_grounded_answer",
                "RESOLVE-FAQ Skill — synthetic for tests",
                List.of("RESOLVE"),
                List.of("UC-A"),
                List.of("search_knowledge", "resolve_article"),
                List.of("customer_context"),
                3,
                false,
                List.of("FINAL_ANSWER", "ESCALATE"),
                "objective",
                "procedure body",
                "grounding",
                "escalation",
                List.<Guardrail>of(),
                StateInheritance.EMPTY,
                steps);
    }

    private void stubCommonMocks() {
        when(controlPolicy.getMaxBotTurnsFaq()).thenReturn(6);
        when(controlPolicy.getMaxClarificationRounds()).thenReturn(3);
        when(controlPolicy.getMaxFaqMiss()).thenReturn(2);
    }

    // -----------------------------------------------------------------
    // Parity preservation — empty critical_steps[] → no projection key.
    // -----------------------------------------------------------------

    @Test
    void projection_emptyCriticalSteps_omitsKeyEntirely() throws Exception {
        stubCommonMocks();
        Skill empty = skillWithSteps(List.of());
        when(skillRegistry.select("RESOLVE", "UC-A")).thenReturn(Optional.of(empty));

        String projection = builder.build(
                resolveSessionUcA(), List.of(), resolveUcAPlan(), "where is my advert?", null);
        JsonNode root = objectMapper.readTree(projection);
        JsonNode phasePlan = root.get("phase_plan");
        assertNotNull(phasePlan, "phase_plan must always be present when a plan is supplied");
        assertFalse(phasePlan.has("critical_steps"),
                "Empty critical_steps[] must NOT add a critical_steps key to phase_plan (parity invariant). "
                        + "Got phase_plan keys: " + phasePlan.fieldNames());
    }

    @Test
    void projection_skillRegistryMisses_omitsCriticalStepsKey() throws Exception {
        // No registered Skill for (RESOLVE, UC-A) → registry returns empty().
        // The projection wiring must guard with Optional.ifPresent and skip
        // critical_steps entirely (parity with the empty-Skill case above).
        stubCommonMocks();
        when(skillRegistry.select("RESOLVE", "UC-A")).thenReturn(Optional.empty());

        String projection = builder.build(
                resolveSessionUcA(), List.of(), resolveUcAPlan(), "where is my advert?", null);
        JsonNode root = objectMapper.readTree(projection);
        JsonNode phasePlan = root.get("phase_plan");
        assertNotNull(phasePlan);
        assertFalse(phasePlan.has("critical_steps"));
    }

    // -----------------------------------------------------------------
    // Populated case — list-of-objects rendering with id + desc verbatim.
    // -----------------------------------------------------------------

    @Test
    void projection_populatedCriticalSteps_rendersAsListOfObjects() throws Exception {
        stubCommonMocks();
        Skill populated = skillWithSteps(List.of(
                new CriticalStep(
                        "search_before_answer",
                        "Retrieve a knowledge article before answering a UC-A visibility question.",
                        "accumulated_tool_results.search_knowledge",
                        List.of("UC-A"),
                        CriticalStep.Severity.MANDATORY),
                new CriticalStep(
                        "resolve_after_search",
                        "Resolve the chosen article before drafting the customer-facing reply.",
                        "tool_event_seq(search_knowledge) < tool_event_seq(resolve_article)",
                        List.of("UC-A"),
                        CriticalStep.Severity.ADVISORY)
        ));
        when(skillRegistry.select("RESOLVE", "UC-A")).thenReturn(Optional.of(populated));

        String projection = builder.build(
                resolveSessionUcA(), List.of(), resolveUcAPlan(), "where is my advert?", null);
        JsonNode root = objectMapper.readTree(projection);
        JsonNode phasePlan = root.get("phase_plan");
        assertNotNull(phasePlan);
        JsonNode steps = phasePlan.get("critical_steps");
        assertNotNull(steps, "critical_steps key must be present when the Skill carries populated steps");
        assertTrue(steps.isArray());
        assertEquals(2, steps.size());

        JsonNode s0 = steps.get(0);
        assertEquals("search_before_answer", s0.get("id").asText());
        // desc rendered VERBATIM — the LLM sees the soft narrative as
        // written. No template substitution; no per-UC if-else; no
        // trace_check leaked into the projection (eval-side only).
        assertEquals(
                "Retrieve a knowledge article before answering a UC-A visibility question.",
                s0.get("desc").asText());
        assertFalse(s0.has("trace_check"),
                "trace_check is eval-side only — MUST NOT leak into the LLM-visible projection");
        assertFalse(s0.has("severity"),
                "severity is eval-side only — MUST NOT leak into the LLM-visible projection");
        assertFalse(s0.has("mandatory_for"),
                "mandatory_for is eval-side only — MUST NOT leak into the LLM-visible projection");

        JsonNode s1 = steps.get(1);
        assertEquals("resolve_after_search", s1.get("id").asText());
        assertEquals(
                "Resolve the chosen article before drafting the customer-facing reply.",
                s1.get("desc").asText());
    }

    @Test
    void projection_criticalStepsRenderedAfterSystemInstruction_within_phasePlan() throws Exception {
        // The contract specifies "immediately after the existing procedure
        // field" (which folds into system_instruction at PhaseEvaluator.java
        // line 459). Verify the field ordering by walking the JSON node's
        // child names: critical_steps must appear AFTER system_instruction
        // and BEFORE escalation_policy.
        stubCommonMocks();
        Skill populated = skillWithSteps(List.of(
                new CriticalStep(
                        "search_before_answer",
                        "Retrieve a knowledge article before answering.",
                        "accumulated_tool_results.search_knowledge",
                        List.of("UC-A"),
                        CriticalStep.Severity.MANDATORY)
        ));
        when(skillRegistry.select("RESOLVE", "UC-A")).thenReturn(Optional.of(populated));

        PhasePlan plan = PhasePlan.builder()
                .phase("RESOLVE")
                .useCase("UC-A")
                .objective("answer")
                .allowedTools(List.of("search_knowledge", "resolve_article"))
                .maxToolSteps(3)
                .systemInstruction("DO X")
                .escalationPolicy("escalate on N misses")
                .validTerminalOutcomes(Set.of(
                        TerminalOutcome.FINAL_ANSWER, TerminalOutcome.ESCALATE))
                .build();

        String projection = builder.build(
                resolveSessionUcA(), List.of(), plan, "user msg", null);
        JsonNode root = objectMapper.readTree(projection);
        JsonNode phasePlan = root.get("phase_plan");

        // Walk the JsonNode field iteration order.
        List<String> order = new java.util.ArrayList<>();
        phasePlan.fieldNames().forEachRemaining(order::add);
        int sysIdx = order.indexOf("system_instruction");
        int csIdx = order.indexOf("critical_steps");
        int escIdx = order.indexOf("escalation_policy");
        assertTrue(sysIdx >= 0 && csIdx >= 0 && escIdx >= 0,
                "system_instruction, critical_steps, and escalation_policy must all be present. "
                        + "Got: " + order);
        assertTrue(sysIdx < csIdx,
                "critical_steps must appear AFTER system_instruction (which carries the "
                        + "procedure narrative; see PhaseEvaluator.java:459). Got order: " + order);
        assertTrue(csIdx < escIdx,
                "critical_steps must appear BEFORE escalation_policy (sibling ordering). "
                        + "Got order: " + order);
    }

    @Test
    void projection_descVerbatim_noTemplateSubstitution() throws Exception {
        // Sprint 43 ships NO content; but the wiring's contract is that
        // desc is rendered VERBATIM, with no placeholder substitution that
        // could accidentally introduce a hardcode. Verify by using a desc
        // containing characters that could be misinterpreted as a template.
        stupCommonStubsForVerbatimTest();
        Skill populated = skillWithSteps(List.of(
                new CriticalStep(
                        "verbatim_check",
                        "Use {placeholder} verbatim — no substitution should occur here.",
                        "accumulated_tool_results.search_knowledge",
                        List.of("UC-A"),
                        CriticalStep.Severity.MANDATORY)
        ));
        when(skillRegistry.select("RESOLVE", "UC-A")).thenReturn(Optional.of(populated));

        String projection = builder.build(
                resolveSessionUcA(), List.of(), resolveUcAPlan(), "user msg", null);
        JsonNode root = objectMapper.readTree(projection);
        JsonNode steps = root.get("phase_plan").get("critical_steps");
        assertEquals(
                "Use {placeholder} verbatim — no substitution should occur here.",
                steps.get(0).get("desc").asText());
    }

    private void stupCommonStubsForVerbatimTest() {
        stubCommonMocks();
    }
}
