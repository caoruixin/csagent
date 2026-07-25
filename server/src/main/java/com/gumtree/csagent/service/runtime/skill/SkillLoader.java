package com.gumtree.csagent.service.runtime.skill;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.gumtree.csagent.model.TerminalOutcome;
import com.gumtree.csagent.service.runtime.UseCaseRegistryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Loads Skill YAML files from {@code server/src/main/resources/skills/} and
 * fail-fast validates each against the schema in design doc §2.2.
 *
 * <p>Per Sprint 37 freeze §3.3: invoked at Spring bootstrap via the
 * {@link SkillRegistry} constructor; throws on any validation failure so
 * Spring fails to start. M2 does NOT implement Skill hot-reload — Sprint 39+
 * Skill content changes require a Spring reload.
 */
@Slf4j
@Component
public class SkillLoader {

    /**
     * Phase enum subset accepted in {@code applicable_phases}. Matches the
     * runtime phase machine (DISCOVER / RESOLVE / CONFIRM / CLOSE / ESCALATE
     * per {@code PhaseEvaluator.evaluate} switch lines 355-360). The design
     * doc §2.1 mentions {@code RESOLVE_FAQ} / {@code RESOLVE_INTAKE} /
     * {@code TERMINAL} as architectural intent for M3+ rename; Sprint 38
     * keeps the runtime enum unchanged per OQ-7.1 default.
     */
    private static final Set<String> VALID_PHASES = Set.of(
            "DISCOVER",
            "RESOLVE",
            "CONFIRM",
            "CLOSE",
            "ESCALATE"
    );

    /**
     * Allowed state keys in {@code state_inheritance.inherit} /
     * {@code state_inheritance.reset} per design doc §10.2.
     */
    private static final Set<String> VALID_STATE_KEYS = Set.of(
            "customer_context",
            "accumulated_tool_results",
            "intake_fields_partial"
    );

    /**
     * Allowed projection slot names in
     * {@code state_inheritance.soft_signal_via_projection} per design doc
     * §10.2. {@code prior_use_case_carry} is the NEW M2 Sprint 41 slot
     * (declared here for forward-compat with Sprint 41).
     */
    private static final Set<String> VALID_PROJECTION_SLOTS = Set.of(
            "alternate_candidate_use_cases",
            "discover_disambiguation_signals",
            "prior_use_case_carry",
            // Sprint 103 / WS-6-A — the argument domain of `propose_reroute`
            // (use-case id + registry name, active UC excluded). Declared by
            // the Skills that carry the tool so the ids it accepts are not
            // opaque to the LLM.
            "reroute_target_use_cases"
    );

    /**
     * Allowed {@code on_fail} modes for guardrails per design doc §5.2.
     * {@code observe_only} is explicitly excluded per OLD Sprint 36 D2 §3.2
     * verbatim (observations belong in projection, not guardrails).
     */
    private static final Set<String> VALID_ON_FAIL_MODES = Set.of(
            "reject_with_hint",
            "downgrade_reason"
    );

    /**
     * Canonical tool names — mirrors the {@code getName()} return value of
     * every {@code Tool} implementation under
     * {@code server/src/main/java/com/gumtree/csagent/service/tools/}.
     *
     * <p>Sprint 38 fix iteration #1 (Codex Blocking Finding 1 sub-gap #1b):
     * design doc §2.2 requires the loader to reject {@code tools_required}
     * entries that are not in the canonical tool-name set. Keeping the set
     * mirrored here (rather than constructor-injecting the {@code Tool} list
     * from Spring) keeps {@link SkillLoader} test-friendly and avoids a bean
     * graph ordering concern (the {@code Tool} beans depend on a number of
     * other beans). If a new tool is added to the runtime, this set must be
     * updated in the same commit — surfaced as an OQ at Sprint 38 fix close.
     */
    private static final Set<String> VALID_TOOL_NAMES = Set.of(
            "search_knowledge",
            "resolve_article",
            "classify_use_case",
            // Sprint 103 / WS-6-A — mid-session re-route, declared by the
            // RESOLVE / CONFIRM Skills (DISCOVER keeps `classify_use_case`).
            "propose_reroute",
            "record_outcome",
            "request_handover",
            // Sprint 080 / R7 — no-side-effect intake-field accumulation tool.
            "update_intake_fields",
            "create_case_controlled",
            "get_customer_context",
            "lookup_listing_or_ad",
            "lookup_customer_account",
            "get_moderation_review_context",
            "get_message_moderation_context"
    );

    /**
     * Canonical guardrail predicate types per Sprint 37 freeze §5.2 + §8.2.
     * Sprint 38 fix iteration #1 (Codex Blocking Finding 1 sub-gap #1d): design
     * doc §2.2 requires the loader to reject {@code guardrails[].type} entries
     * that are not a known predicate type. Sprint 38's 4 simpler phase Skills
     * all ship {@code guardrails: []}; this whitelist becomes load-bearing for
     * Sprint 39's first concrete guardrails on the RESOLVE Skills.
     */
    private static final Set<String> VALID_GUARDRAIL_TYPES = Set.of(
            "faq_miss_handover_requires_resolve_attempt",
            "intake_complete_required",
            "premature_resolve_outcome_guard",
            "must_cite_source"
    );

    /**
     * Allowed {@code severity} values on a {@code critical_steps[].severity}
     * entry per Sprint 43 (S-Eval-2). {@code MANDATORY} steps are Tier-2
     * gate-contributing; {@code ADVISORY} steps are recorded but never flip
     * {@code case_passed}. Mirrors the
     * {@code severity = critical | advisory} contract that S-Eval-1
     * introduced on {@code HardCheckResult} / {@code OutcomeCheckResult}.
     */
    private static final Set<String> VALID_CRITICAL_STEP_SEVERITIES = Set.of(
            "MANDATORY",
            "ADVISORY"
    );

    private static final String SKILLS_CLASSPATH_PATTERN = "classpath:/skills/*.yaml";

    private final ObjectMapper yamlMapper;
    private final UseCaseRegistryService useCaseRegistry;

    /**
     * Production constructor — Spring injects the shared
     * {@link UseCaseRegistryService} so the loader can validate explicit
     * (non-wildcard) {@code applicable_use_cases} entries against the registered
     * UC set per design doc §2.2 (Sprint 38 fix iteration #1, Codex Blocking
     * Finding 1 sub-gap #1c).
     */
    public SkillLoader(UseCaseRegistryService useCaseRegistry) {
        this.yamlMapper = new ObjectMapper(new YAMLFactory());
        this.useCaseRegistry = Objects.requireNonNull(useCaseRegistry,
                "UseCaseRegistryService is required for SkillLoader UC-registry validation "
                        + "(design doc §2.2; Sprint 38 fix iteration #1).");
    }

    /**
     * Load all {@code skills/*.yaml} files from the classpath. Validates each
     * Skill against the schema and the inter-Skill collision invariant per
     * design doc §3.1. Throws {@link IllegalStateException} on any failure.
     */
    public List<Skill> loadAll() {
        PathMatchingResourcePatternResolver resolver =
                new PathMatchingResourcePatternResolver(getClass().getClassLoader());
        Resource[] resources;
        try {
            resources = resolver.getResources(SKILLS_CLASSPATH_PATTERN);
        } catch (Exception e) {
            throw new IllegalStateException(
                    "SkillLoader: failed to enumerate " + SKILLS_CLASSPATH_PATTERN, e);
        }

        if (resources.length == 0) {
            log.warn("SkillLoader: no Skill YAML files found under {}", SKILLS_CLASSPATH_PATTERN);
            return List.of();
        }

        List<Skill> loaded = new ArrayList<>(resources.length);
        for (Resource resource : resources) {
            String filename = resource.getFilename() != null ? resource.getFilename() : "(unknown)";
            Skill skill = parseAndValidate(resource, filename);
            loaded.add(skill);
        }

        log.info("SkillLoader: loaded {} Skill(s) from {}", loaded.size(), SKILLS_CLASSPATH_PATTERN);
        return loaded;
    }

    /**
     * Parse a single Skill YAML from an explicit input stream + filename.
     * Useful for tests that build resources programmatically. Performs the
     * same schema validation as {@link #loadAll()}.
     */
    public Skill parseAndValidate(InputStream in, String filename) {
        Skill skill;
        try {
            skill = yamlMapper.readValue(in, Skill.class);
        } catch (Exception e) {
            throw new IllegalStateException(
                    "SkillLoader: failed to parse YAML from " + filename + ": " + e.getMessage(), e);
        }
        validate(skill, filename);
        return skill;
    }

    private Skill parseAndValidate(Resource resource, String filename) {
        try (InputStream in = resource.getInputStream()) {
            return parseAndValidate(in, filename);
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException(
                    "SkillLoader: failed to read Skill resource " + filename + ": " + e.getMessage(), e);
        }
    }

    void validate(Skill skill, String filename) {
        require(skill.name(), "name", filename);
        require(skill.description(), "description", filename);
        requireNonEmpty(skill.applicablePhases(), "applicable_phases", filename);
        requireNonEmpty(skill.applicableUseCases(), "applicable_use_cases", filename);
        // tools_required may be empty per design doc §2.1 (e.g., CONFIRM-like
        // Skills with no tool calls), but the field itself must be present.
        // Per Sprint 38 fix iteration #1 (sub-gap #1a), the Skill record's
        // compact constructor does NOT normalize null toolsRequired, so a
        // YAML file omitting the key reaches this check as null.
        if (skill.toolsRequired() == null) {
            throw schema(filename, "tools_required is required (may be empty list)");
        }
        require(skill.procedure(), "procedure", filename);

        for (String phase : skill.applicablePhases()) {
            if (!VALID_PHASES.contains(phase)) {
                throw schema(filename, "applicable_phases contains unknown phase '" + phase
                        + "'; valid phases are " + VALID_PHASES);
            }
        }

        boolean hasWildcard = skill.applicableUseCases().contains(Skill.UC_WILDCARD);
        if (hasWildcard && skill.applicableUseCases().size() > 1) {
            throw schema(filename, "applicable_use_cases mixes '*' wildcard with explicit UCs");
        }
        // Sprint 38 fix iteration #1 sub-gap #1c: explicit (non-wildcard)
        // applicable_use_cases entries must reference UCs registered in
        // UseCaseRegistryService per design doc §2.2.
        if (!hasWildcard) {
            for (String uc : skill.applicableUseCases()) {
                if (!useCaseRegistry.isKnownUseCase(uc)) {
                    throw schema(filename, "applicable_use_cases contains unknown UC '" + uc
                            + "'; not registered in UseCaseRegistryService");
                }
            }
        }

        // Sprint 38 fix iteration #1 sub-gap #1b: tools_required entries must
        // be canonical tool names per design doc §2.2.
        for (String tool : skill.toolsRequired()) {
            if (!VALID_TOOL_NAMES.contains(tool)) {
                throw schema(filename, "tools_required contains unknown tool '" + tool
                        + "'; valid tools are " + VALID_TOOL_NAMES);
            }
        }

        if (skill.validTerminalOutcomes() != null) {
            Set<String> validOutcomes = Arrays.stream(TerminalOutcome.values())
                    .map(Enum::name)
                    .collect(Collectors.toSet());
            for (String outcome : skill.validTerminalOutcomes()) {
                if (!validOutcomes.contains(outcome)) {
                    throw schema(filename, "valid_terminal_outcomes contains unknown outcome '"
                            + outcome + "'; valid outcomes are " + validOutcomes);
                }
            }
        }

        StateInheritance si = skill.stateInheritance();
        for (String key : si.inherit()) {
            if (!VALID_STATE_KEYS.contains(key)) {
                throw schema(filename, "state_inheritance.inherit contains unknown key '" + key
                        + "'; valid keys are " + VALID_STATE_KEYS);
            }
        }
        for (String key : si.reset()) {
            if (!VALID_STATE_KEYS.contains(key)) {
                throw schema(filename, "state_inheritance.reset contains unknown key '" + key
                        + "'; valid keys are " + VALID_STATE_KEYS);
            }
        }
        for (String slot : si.softSignalViaProjection()) {
            if (!VALID_PROJECTION_SLOTS.contains(slot)) {
                throw schema(filename, "state_inheritance.soft_signal_via_projection contains "
                        + "unknown slot '" + slot + "'; valid slots are " + VALID_PROJECTION_SLOTS);
            }
        }

        for (Guardrail g : skill.guardrails()) {
            // Sprint 38 fix iteration #1 sub-gap #1d: guardrail type must match
            // a known dispatcher predicate type per design doc §2.2 / §5.2.
            if (!VALID_GUARDRAIL_TYPES.contains(g.type())) {
                throw schema(filename, "guardrail type '" + g.type()
                        + "' is not a known predicate type; valid types are "
                        + VALID_GUARDRAIL_TYPES);
            }
            if (!VALID_ON_FAIL_MODES.contains(g.onFail())) {
                throw schema(filename, "guardrail type=" + g.type() + " has invalid on_fail '"
                        + g.onFail() + "'; valid modes are " + VALID_ON_FAIL_MODES);
            }
        }

        // Sprint 43 (S-Eval-2): critical_steps[] schema validation. Empty list
        // is acceptable (the default on every Skill until S-Eval-3 populates).
        // Each step must carry all five fields; mandatory_for UCs must be
        // registered; severity must be MANDATORY|ADVISORY. The traceCheck
        // string is preserved verbatim — DSL syntax validation is owned by
        // the Python SkillProcedureExtractor (eval-side; see
        // eval_interactive/eval_interactive/scoring/skill_procedure_check.py).
        Set<String> seenStepIds = new java.util.HashSet<>();
        for (CriticalStep step : skill.criticalSteps()) {
            requireStepField(step.id(), "critical_steps[].id", filename);
            if (!seenStepIds.add(step.id())) {
                throw schema(filename, "critical_steps contains duplicate id '"
                        + step.id() + "'; step ids must be unique within a Skill");
            }
            requireStepField(step.desc(), "critical_steps[id=" + step.id() + "].desc", filename);
            requireStepField(step.traceCheck(),
                    "critical_steps[id=" + step.id() + "].trace_check", filename);
            if (step.mandatoryFor().isEmpty()) {
                throw schema(filename, "critical_steps[id=" + step.id()
                        + "].mandatory_for is required (non-empty UC list)");
            }
            for (String uc : step.mandatoryFor()) {
                if (uc == null || uc.isBlank()) {
                    throw schema(filename, "critical_steps[id=" + step.id()
                            + "].mandatory_for contains a null/blank UC entry");
                }
                if (!useCaseRegistry.isKnownUseCase(uc)) {
                    throw schema(filename, "critical_steps[id=" + step.id()
                            + "].mandatory_for contains unknown UC '" + uc
                            + "'; not registered in UseCaseRegistryService");
                }
            }
            if (step.severity() == null) {
                throw schema(filename, "critical_steps[id=" + step.id()
                        + "].severity is required; valid values are "
                        + VALID_CRITICAL_STEP_SEVERITIES);
            }
            if (!VALID_CRITICAL_STEP_SEVERITIES.contains(step.severity().name())) {
                throw schema(filename, "critical_steps[id=" + step.id()
                        + "].severity '" + step.severity().name()
                        + "' is invalid; valid values are "
                        + VALID_CRITICAL_STEP_SEVERITIES);
            }
        }
    }

    private static void requireStepField(String value, String fieldName, String filename) {
        if (value == null || value.isBlank()) {
            throw schema(filename, "required field '" + fieldName + "' is missing or blank");
        }
    }

    private static void require(String value, String fieldName, String filename) {
        if (value == null || value.isBlank()) {
            throw schema(filename, "required field '" + fieldName + "' is missing or blank");
        }
    }

    private static void requireNonEmpty(List<?> value, String fieldName, String filename) {
        if (value == null || value.isEmpty()) {
            throw schema(filename, "required field '" + fieldName + "' is missing or empty");
        }
    }

    private static IllegalStateException schema(String filename, String detail) {
        return new IllegalStateException("SkillLoader: schema validation failed for "
                + filename + " — " + detail);
    }
}
