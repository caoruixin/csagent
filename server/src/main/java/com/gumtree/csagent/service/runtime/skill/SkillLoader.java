package com.gumtree.csagent.service.runtime.skill;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.gumtree.csagent.model.TerminalOutcome;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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
            "prior_use_case_carry"
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

    private static final String SKILLS_CLASSPATH_PATTERN = "classpath:/skills/*.yaml";

    private final ObjectMapper yamlMapper;

    public SkillLoader() {
        this.yamlMapper = new ObjectMapper(new YAMLFactory());
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
        // Skills with no tool calls), but the field itself must be present
        // (List.of() after compact constructor normalization is acceptable).
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
            if (!VALID_ON_FAIL_MODES.contains(g.onFail())) {
                throw schema(filename, "guardrail type=" + g.type() + " has invalid on_fail '"
                        + g.onFail() + "'; valid modes are " + VALID_ON_FAIL_MODES);
            }
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
