package com.gumtree.csagent.service.runtime;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gumtree.csagent.model.BotSession;
import com.gumtree.csagent.model.PhasePlan;
import com.gumtree.csagent.model.TerminalOutcome;
import com.gumtree.csagent.service.runtime.skill.Skill;
import com.gumtree.csagent.service.runtime.skill.SkillLoader;
import com.gumtree.csagent.service.runtime.skill.SkillRegistry;
import com.gumtree.csagent.service.runtime.skill.StateInheritance;
import com.gumtree.csagent.service.tools.ToolPolicyEnforcer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Sprint 52 / M5 S3 — verifies the post-C2-#4 Skill-registry-driven
 * {@code tool_schemas} projection surface in
 * {@link ContextProjectionBuilder#buildProjection(BotSession, java.util.List,
 * java.util.List, String)}.
 *
 * <p>Contract (per {@code docs/diagnostics/m5-s3-projection-consumption-map.md}
 * §3.K + §4 row #4):
 * <ol>
 *   <li>When {@link SkillRegistry#select(String, String)} matches a Skill
 *       for the session's {@code (currentPhase, activeUseCase)} tuple, the
 *       base {@code tool_schemas} comes from {@link Skill#toolsRequired()}
 *       — single source of truth shared with {@code PhasePlan.allowedTools()}.
 *   </li>
 *   <li>When no Skill maps the tuple (legacy / unmapped phase), the base
 *       falls back to the pre-M2 UC-driven palette via
 *       {@link ToolPolicyEnforcer#getVisibleToolsForUc(String)}.</li>
 *   <li>When the active UC is null AND no wildcard Skill matches the phase,
 *       the projection emits an empty {@code tool_schemas} array (no tools
 *       to project).</li>
 *   <li>The run-loop plan-aware {@code build(...)} path's
 *       {@code PhasePlan.allowedTools()} overwrite at the
 *       {@code Sprint 8.1 §M3} site is preserved as defense-in-depth.</li>
 * </ol>
 *
 * <p>§7 boundary: registry/Skill-driven only; no per-UC if-else added to
 * the projection. The Skill's {@code toolsRequired} is registry data
 * (loaded from {@code server/src/main/resources/skills/*.yaml}); the
 * UC-driven fallback is a single defensive call that mirrors pre-M2
 * behaviour for any tuple the registry does not cover.
 */
@ExtendWith(MockitoExtension.class)
class Sprint52ProjectionSkillDrivenTest {

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

    private void stubBudgetForBuildProjection() {
        when(controlPolicy.getMaxBotTurnsFaq()).thenReturn(6);
        when(controlPolicy.getMaxClarificationRounds()).thenReturn(3);
        when(controlPolicy.getMaxFaqMiss()).thenReturn(2);
    }

    @Test
    void buildProjection_skillMatched_usesSkillToolsRequired_notUcPalette() throws Exception {
        stubBudgetForBuildProjection();
        BotSession session = buildSession("UC-A", "RESOLVE");
        UseCaseRegistryService.UseCaseDefinition ucDef = new UseCaseRegistryService.UseCaseDefinition(
                "UC-A", "Ad Status & Visibility", List.of("Ad Support"), "LOW", true, "FAQ");
        when(useCaseRegistry.getUseCase("UC-A")).thenReturn(ucDef);

        Skill faqSkill = stubFaqSkill();
        when(skillRegistry.select("RESOLVE", "UC-A")).thenReturn(Optional.of(faqSkill));

        String projection = builder.buildProjection(session, List.of(), null, "where is my ad?");
        JsonNode root = objectMapper.readTree(projection);
        JsonNode tools = root.get("tool_schemas");

        assertNotNull(tools, "tool_schemas must be present");
        assertTrue(tools.isArray());
        Set<String> names = collectNames(tools);
        assertEquals(
                Set.of("get_customer_context", "search_knowledge",
                        "resolve_article", "record_outcome", "request_handover"),
                names,
                "buildProjection MUST source tool_schemas from the resolved Skill's "
                        + "toolsRequired (M2-correct), not from "
                        + "toolPolicyEnforcer.getVisibleToolsForUc — the Skill is the "
                        + "single source of truth shared with PhasePlan.allowedTools().");
        // The UC-driven fallback must NOT have been consulted when a Skill matched.
        verify(toolPolicyEnforcer, never()).getVisibleToolsForUc(anyString());
    }

    @Test
    void buildProjection_skillUnmatched_fallsBackToUcDrivenPalette() throws Exception {
        stubBudgetForBuildProjection();
        BotSession session = buildSession("UC-Z", "RESOLVE");
        UseCaseRegistryService.UseCaseDefinition ucDef = new UseCaseRegistryService.UseCaseDefinition(
                "UC-Z", "Hypothetical Unmapped UC", List.of("misc"), "LOW", true, "FAQ");
        when(useCaseRegistry.getUseCase("UC-Z")).thenReturn(ucDef);

        when(skillRegistry.select("RESOLVE", "UC-Z")).thenReturn(Optional.empty());
        when(toolPolicyEnforcer.getVisibleToolsForUc("UC-Z"))
                .thenReturn(List.of("search_knowledge", "request_handover"));

        String projection = builder.buildProjection(session, List.of(), null, "test");
        JsonNode root = objectMapper.readTree(projection);
        Set<String> names = collectNames(root.get("tool_schemas"));

        assertEquals(Set.of("search_knowledge", "request_handover"), names,
                "When no Skill maps the (phase, UC) tuple, the projection MUST fall back "
                        + "to the pre-M2 UC-driven palette via ToolPolicyEnforcer.");
    }

    @Test
    void buildProjection_nullActiveUc_noWildcardSkill_emitsEmptyToolSchemas() throws Exception {
        stubBudgetForBuildProjection();
        BotSession session = buildSession(null, "DISCOVER");
        when(skillRegistry.select("DISCOVER", null)).thenReturn(Optional.empty());

        String projection = builder.buildProjection(session, List.of(), null, "hello");
        JsonNode root = objectMapper.readTree(projection);
        JsonNode tools = root.get("tool_schemas");

        assertNotNull(tools);
        assertTrue(tools.isArray());
        assertEquals(0, tools.size(),
                "Null activeUc + no wildcard Skill match MUST yield an empty tool_schemas "
                        + "array — preserves pre-S3 behaviour for unclassified DISCOVER turns.");
        verify(toolPolicyEnforcer, never()).getVisibleToolsForUc(anyString());
    }

    @Test
    void buildProjection_nullActiveUc_wildcardSkillMatched_usesSkillTools() throws Exception {
        // DISCOVER phase carries a wildcard Skill (discover_triage in real YAML)
        // that applies for (DISCOVER, *) regardless of UC. The S3 helper must
        // pick it up even when activeUc is null (pre-classification turns).
        stubBudgetForBuildProjection();
        BotSession session = buildSession(null, "DISCOVER");
        Skill discoverSkill = stubDiscoverSkill();
        when(skillRegistry.select("DISCOVER", null)).thenReturn(Optional.of(discoverSkill));

        String projection = builder.buildProjection(session, List.of(), null, "hello");
        JsonNode root = objectMapper.readTree(projection);
        Set<String> names = collectNames(root.get("tool_schemas"));

        assertEquals(Set.of("search_knowledge", "classify_use_case"), names,
                "Wildcard Skill match for (DISCOVER, null) MUST source tool_schemas from "
                        + "the Skill's toolsRequired even with null activeUc.");
    }

    @Test
    void planAwareBuild_skillMatched_skillToolsEqualPlanAllowedTools_overwriteIsNoOpForNames()
            throws Exception {
        // When the resolved Skill's toolsRequired matches the plan's allowedTools
        // (the M2-correct steady state), the base buildProjection emits the
        // Skill-filtered set and the build(...) :allowedTools overwrite writes
        // the same set. Defense-in-depth holds; observable end-state is the
        // Skill-driven list.
        stubBudgetForBuildProjection();
        BotSession session = buildSession("UC-A", "DISCOVER");
        UseCaseRegistryService.UseCaseDefinition ucDef = new UseCaseRegistryService.UseCaseDefinition(
                "UC-A", "Ad Status & Visibility", List.of("Ad Support"), "LOW", true, "FAQ");
        when(useCaseRegistry.getUseCase("UC-A")).thenReturn(ucDef);

        Skill discoverSkill = stubDiscoverSkill();
        when(skillRegistry.select("DISCOVER", "UC-A")).thenReturn(Optional.of(discoverSkill));

        PhasePlan discoverPlan = PhasePlan.builder()
                .phase("DISCOVER")
                .useCase("UC-A")
                .objective("identify use case")
                .allowedTools(List.of("search_knowledge", "classify_use_case"))
                .maxToolSteps(2)
                .systemInstruction("DISCOVER")
                .validTerminalOutcomes(Set.of(
                        TerminalOutcome.USE_CASE_IDENTIFIED,
                        TerminalOutcome.FINAL_ANSWER,
                        TerminalOutcome.ESCALATE))
                .build();

        String projection = builder.build(session, List.of(), discoverPlan, "where is my ad?", null);
        JsonNode root = objectMapper.readTree(projection);
        Set<String> names = collectNames(root.get("tool_schemas"));

        assertEquals(Set.of("search_knowledge", "classify_use_case"), names,
                "build(...) end-state tool_schemas MUST equal the Skill-driven set when "
                        + "PhasePlan.allowedTools matches Skill.toolsRequired (the steady "
                        + "state after S3 C2 #4).");
    }

    @Test
    void buildProjection_faqSkillDoesNotDeclareModerationContext_projectionDoesNotEmitIt()
            throws Exception {
        // C2 #3 guard: after stripping the stale moderation_context declaration
        // from resolve_faq_grounded_answer.yaml, the projection must not emit a
        // moderation_context slot for FAQ RESOLVE turns. The projection never
        // emitted the slot before C2 #3 either — this test guards against a
        // regression that would emit it under the wrong assumption.
        stubBudgetForBuildProjection();
        BotSession session = buildSession("UC-A", "RESOLVE");
        UseCaseRegistryService.UseCaseDefinition ucDef = new UseCaseRegistryService.UseCaseDefinition(
                "UC-A", "Ad Status & Visibility", List.of("Ad Support"), "LOW", true, "FAQ");
        when(useCaseRegistry.getUseCase("UC-A")).thenReturn(ucDef);

        Skill faqSkill = stubFaqSkill();
        when(skillRegistry.select("RESOLVE", "UC-A")).thenReturn(Optional.of(faqSkill));

        String projection = builder.buildProjection(session, List.of(), null, "test");
        JsonNode root = objectMapper.readTree(projection);

        assertFalse(root.has("moderation_context"),
                "Projection MUST NOT emit moderation_context — the slot has no producer "
                        + "and (post-C2 #3) no Skill declaration.");
    }

    @Test
    void productionFaqSkill_doesNotDeclareModerationContext_postC2No3() {
        // Loads the real resolve_faq_grounded_answer.yaml from the classpath
        // (post-C2 #3 strip). Guards against re-introduction of the stale
        // moderation_context declaration that had no producer, no consumer,
        // and was a M2-era artifact with no behaviour. See
        // docs/diagnostics/m5-s3-projection-consumption-map.md §3.R.
        UseCaseRegistryService ucRegistry = new UseCaseRegistryService();
        ucRegistry.init();
        SkillLoader loader = new SkillLoader(ucRegistry);
        List<Skill> all = loader.loadAll();
        Skill faq = all.stream()
                .filter(s -> "resolve_faq_grounded_answer".equals(s.name()))
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "resolve_faq_grounded_answer Skill must load from classpath"));

        assertFalse(faq.requiredContextKeys().contains("moderation_context"),
                "resolve_faq_grounded_answer.required_context_keys MUST NOT contain "
                        + "moderation_context — the declaration had no producer and no "
                        + "consumer; re-adding it would re-open the M2-era contract gap.");
        // Defensive: the three keys that DO have producers should still be declared.
        assertTrue(faq.requiredContextKeys().contains("form_context"));
        assertTrue(faq.requiredContextKeys().contains("customer_context"));
        assertTrue(faq.requiredContextKeys().contains("listing_context"));
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private static BotSession buildSession(String activeUc, String phase) {
        return BotSession.builder()
                .sessionId("test-session")
                .activeUseCase(activeUc)
                .currentPhase(phase)
                .totalBotTurns(0)
                .clarificationCount(0)
                .faqMissCount(0)
                .handlingState("RUNNING")
                .build();
    }

    private static Skill stubFaqSkill() {
        return new Skill(
                "resolve_faq_grounded_answer",
                "RESOLVE-FAQ phase Skill stub",
                List.of("RESOLVE"),
                List.of("UC-A", "UC-B", "UC-C", "UC-D", "UC-E", "UC-F", "UC-FP"),
                List.of("get_customer_context", "search_knowledge",
                        "resolve_article", "record_outcome", "request_handover"),
                List.of("form_context", "customer_context", "listing_context"),
                4,
                false,
                List.of("FINAL_ANSWER", "CLARIFICATION_NEEDED", "ESCALATE"),
                "objective stub",
                "procedure stub",
                "grounding stub",
                "escalation stub",
                List.of(),
                StateInheritance.EMPTY,
                List.of()
        );
    }

    private static Skill stubDiscoverSkill() {
        return new Skill(
                "discover_triage",
                "DISCOVER phase Skill stub",
                List.of("DISCOVER"),
                List.of(Skill.UC_WILDCARD),
                List.of("search_knowledge", "classify_use_case"),
                List.of("form_context", "candidate_use_cases"),
                2,
                false,
                List.of("USE_CASE_IDENTIFIED", "FINAL_ANSWER", "ESCALATE"),
                "objective stub",
                "procedure stub",
                "grounding stub",
                "escalation stub",
                List.of(),
                StateInheritance.EMPTY,
                List.of()
        );
    }

    private static Set<String> collectNames(JsonNode tools) {
        Set<String> names = new HashSet<>();
        for (JsonNode tool : tools) {
            names.add(tool.get("name").asText());
        }
        return names;
    }
}
