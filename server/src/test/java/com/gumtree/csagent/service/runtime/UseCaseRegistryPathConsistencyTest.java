package com.gumtree.csagent.service.runtime;

import com.gumtree.csagent.model.PhasePlan;
import com.gumtree.csagent.service.runtime.skill.Skill;
import com.gumtree.csagent.service.runtime.skill.SkillLoader;
import com.gumtree.csagent.service.runtime.skill.SkillRegistry;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * WS-3 / A3 (2026-07-25) — drift guard for the use-case {@code path}
 * classification.
 *
 * <p>Before WS-3, four independent literal {@code INTAKE_UCS} sets existed on
 * the server side ({@code PhaseEvaluator}, {@code ControlKernel},
 * {@code ResolveDispositionEvaluator}) plus two more on the eval side, and they
 * had already drifted apart over UC-K. The single declaration is now
 * {@code path} in {@code config/use-case-registry.yaml}; where a class cannot
 * inject {@link UseCaseRegistryService} (static utilities, and
 * {@code ControlKernel}'s many direct-construction test fixtures) it mirrors
 * the registry, and this test fails the build if a mirror diverges.
 *
 * <p>It also pins the two structural consequences of the classification:
 * every knowledge-capable UC must resolve to a Skill that can actually call
 * {@code search_knowledge}, and every intake-collecting UC must resolve to a
 * Skill whose guardrails include {@code intake_complete_required}.
 */
class UseCaseRegistryPathConsistencyTest {

    private static UseCaseRegistryService registry;
    private static SkillRegistry skillRegistry;

    @BeforeAll
    static void init() {
        registry = new UseCaseRegistryService();
        registry.init();
        skillRegistry = new SkillRegistry(new SkillLoader(registry));
    }

    private static Set<String> ucsWithPath(String path) {
        Set<String> out = new TreeSet<>();
        for (Map.Entry<String, UseCaseRegistryService.UseCaseDefinition> e
                : registry.getAllUseCases().entrySet()) {
            if (path.equals(e.getValue().path())) {
                out.add(e.getKey());
            }
        }
        return out;
    }

    @Test
    void registry_declaresOnlyKnownPathValues() {
        Set<String> allowed = Set.of(
                UseCaseRegistryService.PATH_FAQ,
                UseCaseRegistryService.PATH_INTAKE,
                UseCaseRegistryService.PATH_PARTIAL);
        for (Map.Entry<String, UseCaseRegistryService.UseCaseDefinition> e
                : registry.getAllUseCases().entrySet()) {
            assertTrue(allowed.contains(e.getValue().path()),
                    "use-case-registry.yaml " + e.getKey() + " declares path='"
                            + e.getValue().path() + "'; allowed values are " + allowed
                            + ". Adding a new path value requires updating "
                            + "UseCaseRegistryService's three derived predicates.");
        }
    }

    @Test
    void registry_ucK_isPartialPath() {
        // A3: phase2_domain_realization_spec.md:493 declares UC-K
        // `allow_bot_resolution: partial`. The registry now agrees.
        assertEquals(UseCaseRegistryService.PATH_PARTIAL, registry.pathOf("UC-K"),
                "UC-K must stay on the PARTIAL path — it is the only UC that may both "
                        + "retrieve knowledge and collect intake fields.");
        assertTrue(registry.isKnowledgeCapablePath("UC-K"));
        assertTrue(registry.collectsIntakeFields("UC-K"));
        assertFalse(registry.isIntakeOnlyPath("UC-K"));
    }

    @Test
    void derivedPredicates_agreeWithDeclaredPaths() {
        Set<String> faq = ucsWithPath(UseCaseRegistryService.PATH_FAQ);
        Set<String> intake = ucsWithPath(UseCaseRegistryService.PATH_INTAKE);
        Set<String> partial = ucsWithPath(UseCaseRegistryService.PATH_PARTIAL);

        Set<String> knowledgeCapable = new TreeSet<>(faq);
        knowledgeCapable.addAll(partial);
        Set<String> collectsIntake = new TreeSet<>(intake);
        collectsIntake.addAll(partial);

        for (String uc : registry.getAllUseCases().keySet()) {
            assertEquals(intake.contains(uc), registry.isIntakeOnlyPath(uc),
                    "isIntakeOnlyPath disagrees with the declared path for " + uc);
            assertEquals(knowledgeCapable.contains(uc), registry.isKnowledgeCapablePath(uc),
                    "isKnowledgeCapablePath disagrees with the declared path for " + uc);
            assertEquals(collectsIntake.contains(uc), registry.collectsIntakeFields(uc),
                    "collectsIntakeFields disagrees with the declared path for " + uc);
        }

        // Unknown UCs fall to the conservative side: no retrieval, no intake,
        // and not classified as intake-only either.
        assertFalse(registry.isKnowledgeCapablePath("UC-ZZ"));
        assertFalse(registry.collectsIntakeFields("UC-ZZ"));
        assertFalse(registry.isIntakeOnlyPath("UC-ZZ"));
        assertFalse(registry.isKnowledgeCapablePath(null));
    }

    @Test
    void everyKnowledgeCapableUc_resolvesToASkillThatCanSearch() {
        for (String uc : registry.getAllUseCases().keySet()) {
            if (!registry.isKnowledgeCapablePath(uc)) {
                continue;
            }
            Skill skill = skillRegistry.select("RESOLVE", uc).orElseThrow(
                    () -> new AssertionError("No RESOLVE Skill for knowledge-capable UC " + uc));
            assertTrue(skill.toolsRequired().contains("search_knowledge"),
                    "UC " + uc + " declares a knowledge-capable registry path but its RESOLVE "
                            + "Skill '" + skill.name() + "' cannot call search_knowledge. "
                            + "This is the exact A3 defect: a UC that is allowed to answer but "
                            + "not allowed to look anything up.");
            assertTrue(skill.toolsRequired().contains("resolve_article"),
                    "UC " + uc + " must also be able to resolve_article to earn a citation.");
        }
    }

    @Test
    void everyIntakeCollectingUc_resolvesToASkillWithTheIntakeGuardrail() {
        for (String uc : registry.getAllUseCases().keySet()) {
            if (!registry.collectsIntakeFields(uc)) {
                continue;
            }
            Skill skill = skillRegistry.select("RESOLVE", uc).orElseThrow(
                    () -> new AssertionError("No RESOLVE Skill for intake-collecting UC " + uc));
            Set<String> types = new LinkedHashSet<>();
            skill.guardrails().forEach(g -> types.add(g.type()));
            assertTrue(types.contains("intake_complete_required"),
                    "UC " + uc + " collects intake fields but its RESOLVE Skill '"
                            + skill.name() + "' does not carry the intake_complete_required "
                            + "guardrail; an incomplete intake could reach a human queue.");
        }
    }

    @Test
    void resolveDispositionEvaluator_intakeOnlySet_mirrorsTheRegistry() {
        // isResolveFaqPlan() is true exactly for the non-intake-only UCs.
        for (String uc : registry.getAllUseCases().keySet()) {
            PhasePlan plan = PhasePlan.builder()
                    .phase("RESOLVE")
                    .useCase(uc)
                    .build();
            assertEquals(!registry.isIntakeOnlyPath(uc),
                    ResolveDispositionEvaluator.isResolveFaqPlan(plan),
                    "ResolveDispositionEvaluator's mirrored intake-only set has drifted from "
                            + "use-case-registry.yaml for " + uc);
        }
    }

    @Test
    void everyUc_isExactlyOneOfKnowledgeCapableOrIntakeOnly() {
        // ControlKernel.computeRouteKey picks RESOLVE_FAQ for knowledge-capable
        // UCs and RESOLVE_INTAKE for intake-only ones. A UC that is neither (or
        // both) would fall out of the AgentRunLoop feature flag into the legacy
        // executor silently, so pin the partition here.
        for (String uc : registry.getAllUseCases().keySet()) {
            boolean knowledge = registry.isKnowledgeCapablePath(uc);
            boolean intakeOnly = registry.isIntakeOnlyPath(uc);
            assertTrue(knowledge ^ intakeOnly,
                    "UC " + uc + " must be exactly one of knowledge-capable / intake-only so "
                            + "ControlKernel.computeRouteKey can pick a single route key.");
        }
    }
}
