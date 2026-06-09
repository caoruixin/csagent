package com.gumtree.csagent.service.runtime.skill;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gumtree.csagent.service.runtime.UseCaseRegistryService;

/**
 * Test fixtures for the Sprint 38/39 Skill Registry abstraction.
 *
 * <p>Exposes:
 * <ul>
 *   <li>{@link #productionRegistry()} — a real {@link SkillRegistry} built from
 *       the 6 production Skill YAML files in
 *       {@code server/src/main/resources/skills/} (Sprint 38: discover /
 *       confirm / escalate / terminal; Sprint 39: resolve_faq_grounded_answer /
 *       resolve_intake_collect_and_handover).</li>
 *   <li>{@link #productionDispatcher()} — Sprint 39 {@link SkillGuardrailDispatcher}
 *       wired to the production SkillRegistry; used by AgentRunLoop integration
 *       tests that exercise the migrated Sprint 6/7/11 predicate semantics +
 *       the NEW S1 must_cite_source + S2 intake_complete_required predicates.</li>
 * </ul>
 *
 * <p>Used by existing PhaseEvaluator + AgentRunLoop tests so the migrated
 * phases / predicates keep the same observational behaviour they had
 * pre-migration without each test needing to know about the loader internals.
 */
public final class SkillTestFixtures {

    private SkillTestFixtures() {}

    /** Real SkillRegistry built from the on-classpath production Skill YAMLs. */
    public static SkillRegistry productionRegistry() {
        return new SkillRegistry(new SkillLoader(initializedUseCaseRegistry()));
    }

    /**
     * Sprint 39 — real {@link SkillGuardrailDispatcher} wired to a real
     * production SkillRegistry. Used by AgentRunLoop integration tests that
     * exercise the migrated Sprint 6/7/11 predicate semantics under the
     * unified dispatch surface. Constructs a fresh ObjectMapper since the
     * dispatcher uses it only for {@code session.intakeFields} JSONB parsing
     * (cheap; no need to share a mapper across tests).
     */
    public static SkillGuardrailDispatcher productionDispatcher() {
        return new SkillGuardrailDispatcher(productionRegistry(), new ObjectMapper());
    }

    /**
     * Sprint 39 — convenience overload that lets a test share its existing
     * {@link SkillRegistry} with the dispatcher (avoids loading the YAMLs
     * twice).
     */
    public static SkillGuardrailDispatcher productionDispatcher(SkillRegistry registry) {
        return new SkillGuardrailDispatcher(registry, new ObjectMapper());
    }

    /**
     * Construct + initialize a {@link UseCaseRegistryService} from the
     * classpath {@code config/use-case-registry.yaml}. Required by
     * {@link SkillLoader} after Sprint 38 fix iteration #1 sub-gap #1c
     * (explicit-UC registry check).
     */
    public static UseCaseRegistryService initializedUseCaseRegistry() {
        UseCaseRegistryService registry = new UseCaseRegistryService();
        registry.init();
        return registry;
    }
}
