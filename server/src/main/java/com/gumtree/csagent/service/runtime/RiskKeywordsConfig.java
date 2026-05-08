package com.gumtree.csagent.service.runtime;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Sprint 15 §M0 — config-backed source of truth for the DriftDetector's
 * escalation regex and hard-shift keyword groups.
 *
 * <p>Loads {@code config/risk-keywords.yaml} at startup. The contract
 * matches the previously hardcoded {@code DriftDetector} constants
 * exactly: same keyword groups, same case-insensitive substring matching
 * semantics, same target use cases, same in-order precedence (first
 * match wins).
 *
 * <p>Validation rules (fail fast on invalid config):
 * <ul>
 *   <li>file exists on classpath at {@value #DEFAULT_RESOURCE_PATH}
 *   <li>top-level {@code escalation-pattern} is non-blank and compiles
 *       as a {@link Pattern}
 *   <li>top-level {@code hard-shift-groups} is a non-empty list
 *   <li>each group has a non-blank {@code target-use-case}
 *   <li>each group has a non-empty {@code keywords} list
 *   <li>no blank keyword strings
 *   <li>no duplicate keyword strings across all groups (since first-
 *       match-wins makes downstream matches unreachable)
 * </ul>
 *
 * <p>Sprint 15 explicitly does NOT change drift semantics, escalation
 * reasons, risk levels, or introduce auto-handover for Level 1 / Level
 * 2 risk signals.
 */
@Slf4j
@Service
public class RiskKeywordsConfig {

    static final String DEFAULT_RESOURCE_PATH = "config/risk-keywords.yaml";

    private Pattern escalationPattern;
    private List<HardShiftGroup> hardShiftGroups = List.of();
    private int version;

    @PostConstruct
    public void init() {
        try (InputStream is = new ClassPathResource(DEFAULT_RESOURCE_PATH).getInputStream()) {
            loadFromStream(is);
            log.info("Loaded risk-keywords config v{}: {} hard-shift groups, escalation pattern compiled",
                    version, hardShiftGroups.size());
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Failed to load " + DEFAULT_RESOURCE_PATH + ": " + e.getMessage(), e);
        }
    }

    /**
     * Load from an explicit input stream. Used by parity / validation
     * tests so the production loader and tests share the same code path.
     */
    public void loadFromStream(InputStream stream) throws IOException {
        ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
        JsonNode root = yamlMapper.readTree(stream);
        if (root == null || root.isMissingNode() || root.isNull()) {
            throw new IllegalStateException("risk-keywords config is empty");
        }

        JsonNode versionNode = root.get("version");
        this.version = versionNode == null ? 0 : versionNode.asInt(0);

        JsonNode patternNode = root.get("escalation-pattern");
        if (patternNode == null || patternNode.isNull() || patternNode.asText().isBlank()) {
            throw new IllegalStateException(
                    "risk-keywords: 'escalation-pattern' is required and must be non-blank");
        }
        try {
            this.escalationPattern = Pattern.compile(patternNode.asText());
        } catch (PatternSyntaxException pse) {
            throw new IllegalStateException(
                    "risk-keywords: 'escalation-pattern' is not a valid regex: " + pse.getMessage(), pse);
        }

        JsonNode groupsNode = root.get("hard-shift-groups");
        if (groupsNode == null || !groupsNode.isArray() || groupsNode.size() == 0) {
            throw new IllegalStateException(
                    "risk-keywords: 'hard-shift-groups' is required and must be a non-empty list");
        }

        List<HardShiftGroup> parsed = new ArrayList<>(groupsNode.size());
        Set<String> seenKeywords = new HashSet<>();
        for (int i = 0; i < groupsNode.size(); i++) {
            JsonNode g = groupsNode.get(i);
            JsonNode targetNode = g.get("target-use-case");
            if (targetNode == null || targetNode.isNull() || targetNode.asText().isBlank()) {
                throw new IllegalStateException(
                        "risk-keywords: group #" + i + " missing 'target-use-case'");
            }
            String target = targetNode.asText();

            JsonNode keywordsNode = g.get("keywords");
            if (keywordsNode == null || !keywordsNode.isArray() || keywordsNode.size() == 0) {
                throw new IllegalStateException(
                        "risk-keywords: group #" + i + " ('" + target
                                + "') must declare a non-empty 'keywords' list");
            }

            Set<String> kws = new LinkedHashSet<>();
            for (int k = 0; k < keywordsNode.size(); k++) {
                String kw = keywordsNode.get(k).asText();
                if (kw == null || kw.isBlank()) {
                    throw new IllegalStateException(
                            "risk-keywords: group #" + i + " ('" + target
                                    + "') contains a blank keyword at index " + k);
                }
                if (!seenKeywords.add(kw)) {
                    throw new IllegalStateException(
                            "risk-keywords: duplicate keyword '" + kw
                                    + "' (later occurrences are unreachable due to first-match-wins)");
                }
                kws.add(kw);
            }
            parsed.add(new HardShiftGroup(target, List.copyOf(kws)));
        }
        this.hardShiftGroups = Collections.unmodifiableList(parsed);
    }

    public Pattern getEscalationPattern() {
        return escalationPattern;
    }

    public List<HardShiftGroup> getHardShiftGroups() {
        return hardShiftGroups;
    }

    public int getVersion() {
        return version;
    }

    /**
     * Test-only factory: build a config instance from an explicit
     * stream (e.g. the production classpath resource) without going
     * through Spring.
     */
    public static RiskKeywordsConfig loadFrom(InputStream stream) throws IOException {
        RiskKeywordsConfig config = new RiskKeywordsConfig();
        config.loadFromStream(stream);
        return config;
    }

    /**
     * Test-only factory: load from the bundled default classpath resource.
     */
    public static RiskKeywordsConfig loadDefault() throws IOException {
        try (InputStream is = new ClassPathResource(DEFAULT_RESOURCE_PATH).getInputStream()) {
            return loadFrom(is);
        }
    }

    public record HardShiftGroup(String targetUseCase, List<String> keywords) {}
}
