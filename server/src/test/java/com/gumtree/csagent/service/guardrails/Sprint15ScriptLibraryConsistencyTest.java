package com.gumtree.csagent.service.guardrails;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Map.entry;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Sprint 15 §M1 — docs ↔ YAML consistency check for the fixed script
 * library.
 *
 * <p>Acceptance condition (Sprint 15): runtime templates and approved
 * docs cannot silently drift. This test fails the build if:
 * <ul>
 *   <li>{@code library_version} is missing from
 *       {@code scripts/templates.yaml}.
 *   <li>The YAML version does not match the latest entry of the
 *       {@code ## 11. Version} table in
 *       {@code docs/fixed_script_library_v1.md}.
 *   <li>Any required template ID listed in
 *       {@link #REQUIRED_TEMPLATE_VARIABLES} is missing from the YAML.
 *   <li>A required template's declared {@code variables:} list drifts
 *       from the doc's variable contract.
 * </ul>
 *
 * <p>Sprint 15 §M1 explicitly does NOT rewrite script copy, alter tone
 * / escalation wording, or change forbidden-phrase rules. It only
 * pins metadata + variable contract.
 */
class Sprint15ScriptLibraryConsistencyTest {

    private static final String TEMPLATES_RESOURCE = "scripts/templates.yaml";
    private static final String DOC_RELATIVE_PATH = "docs/fixed_script_library_v1.md";

    /**
     * Required template ID → variable list contract.
     *
     * <p>The list is the canonical "must exist on the runtime side"
     * subset of templates referenced by docs/fixed_script_library_v1.md
     * §1–§9. A missing key means the runtime cannot render the
     * approved phrase. A drifted variable list means docs and runtime
     * disagree on the placeholder contract.
     *
     * <p>Templates not declared here may still exist (e.g. nested
     * sub-keys); the goal is a stable docs/YAML contract surface, not
     * an exhaustive YAML mirror.
     */
    private static final Map<String, List<String>> REQUIRED_TEMPLATE_VARIABLES = Map.ofEntries(
            // §1 Shared
            entry("opening", List.of("first_name")),
            entry("hold_placeholder", List.of()),                // nested sub-keys (short/medium/long)
            entry("identifier_request", List.of()),              // nested sub-keys (email/ad_id)
            entry("resolution_check", List.of()),                // nested sub-keys (ask/yes/no)
            entry("escalation_business_hours", List.of()),
            entry("escalation_off_hours", List.of("SLA_HOURS")),
            entry("idle_warning", List.of()),
            entry("idle_close", List.of()),
            entry("closing", List.of()),
            // §2 UC-FP
            entry("fp_empathy", List.of()),
            entry("fp_specific_reason", List.of("SPECIFIC_REASON", "POLICY_URL")),
            entry("fp_no_reason_available", List.of("SLA_HOURS")),
            entry("fp_repost_guidance", List.of("POLICY_URL")),
            entry("fp_appeal_redirect", List.of()),
            entry("fp_complaint_redirect", List.of("COMPLAINT_EMAIL")),
            // §3 UC-H
            entry("h_empathy", List.of()),
            entry("h_intake_prompt", List.of()),
            entry("h_intake_complete_case_created", List.of("CASE_NUMBER", "TEAM_NAME", "SLA_HOURS")),
            entry("h_account_blocked_escalate", List.of("SLA_HOURS")),
            entry("h_frustration_ack", List.of()),
            entry("h_cannot_reinstate", List.of()),
            // §4 UC-G
            entry("g_process_explanation", List.of("SLA_HOURS")),
            entry("g_intake_prompt", List.of()),
            entry("g_identity_verification_required", List.of()),
            entry("g_timeline", List.of()),
            entry("g_alternative_submission", List.of("PRIVACY_EMAIL")),
            // §5 UC-I
            entry("i_disclaimer", List.of()),
            entry("i_intake_prompt", List.of()),
            entry("i_refund_policy_link", List.of()),
            entry("i_escalation", List.of("SLA_HOURS")),
            // §6 UC-J
            entry("j_acknowledgment", List.of()),
            entry("j_intake_prompt", List.of()),
            entry("j_police_guidance", List.of()),
            entry("j_action_taken", List.of()),
            entry("j_safety_links", List.of()),
            entry("j_law_enforcement_info", List.of("LAW_ENFORCEMENT_EMAIL")),
            entry("j_imminent_harm", List.of()),
            // §7 UC-K
            entry("k_basic_troubleshoot", List.of()),
            entry("k_platform_check", List.of()),
            entry("k_known_issue", List.of()),
            entry("k_escalation", List.of("SLA_HOURS")),
            entry("k_screenshot_request", List.of("SUPPORT_EMAIL")),
            // §9 OOS
            entry("oos_delivery", List.of()),
            entry("oos_pro_contract", List.of()),
            entry("oos_account_manager", List.of()),
            entry("oos_ratings_reviews", List.of()),
            entry("oos_generic", List.of())
    );

    /** Pattern matching the version table rows: "| v1.1 | 2026-04-21 | ... |". */
    private static final Pattern DOC_VERSION_ROW = Pattern.compile(
            "^\\|\\s*(v\\d+(?:\\.\\d+)*)\\s*\\|\\s*(\\d{4}-\\d{2}-\\d{2})\\s*\\|");

    @Test
    void yaml_libraryVersionPin_isPresent() throws IOException {
        JsonNode root = readTemplatesYaml();
        JsonNode versionNode = root.get("library_version");
        assertNotNull(versionNode, "library_version is required in scripts/templates.yaml");
        assertFalse(versionNode.asText("").isBlank(),
                "library_version must be non-blank");
        assertNotNull(root.get("library_version_date"),
                "library_version_date is required in scripts/templates.yaml");
    }

    @Test
    void yaml_libraryVersion_matchesLatestDocVersion() throws IOException {
        JsonNode root = readTemplatesYaml();
        String yamlVersion = root.get("library_version").asText();
        String yamlDate = root.get("library_version_date").asText();

        DocVersion latest = readLatestDocVersion();

        assertEquals(latest.version(), yamlVersion,
                "templates.yaml library_version must match the latest entry in "
                        + DOC_RELATIVE_PATH + " §11 Version table");
        assertEquals(latest.date(), yamlDate,
                "templates.yaml library_version_date must match the latest entry in "
                        + DOC_RELATIVE_PATH + " §11 Version table");
    }

    @Test
    void yaml_requiredTemplateIds_allPresent() throws IOException {
        JsonNode templatesNode = readTemplatesYaml().get("templates");
        assertNotNull(templatesNode, "templates root key must exist");

        Set<String> missing = new LinkedHashSet<>();
        for (String id : REQUIRED_TEMPLATE_VARIABLES.keySet()) {
            if (!templatesNode.has(id)) {
                missing.add(id);
            }
        }
        if (!missing.isEmpty()) {
            fail("Required template IDs missing from templates.yaml: " + missing
                    + " (declared in " + DOC_RELATIVE_PATH + " §1–§9)");
        }
    }

    @Test
    void yaml_requiredTemplateVariables_matchDocContract() throws IOException {
        JsonNode templatesNode = readTemplatesYaml().get("templates");
        assertNotNull(templatesNode, "templates root key must exist");

        StringBuilder errors = new StringBuilder();
        for (Map.Entry<String, List<String>> e : REQUIRED_TEMPLATE_VARIABLES.entrySet()) {
            String id = e.getKey();
            List<String> expected = e.getValue();
            JsonNode node = templatesNode.get(id);
            if (node == null) {
                // Missing-id is reported by the previous test; skip.
                continue;
            }
            JsonNode varsNode = node.get("variables");
            List<String> actual = readStringArray(varsNode);

            if (!expected.equals(actual)) {
                errors.append("\n  - ").append(id)
                        .append(": expected variables ").append(expected)
                        .append(", actual ").append(actual);
            }
        }
        if (errors.length() > 0) {
            fail("Variable contract drift between docs/fixed_script_library_v1.md and templates.yaml:"
                    + errors);
        }
    }

    @Test
    void yaml_topLevelTemplateNames_haveNoStrayDuplicates() throws IOException {
        JsonNode templatesNode = readTemplatesYaml().get("templates");
        assertNotNull(templatesNode, "templates root key must exist");
        Set<String> seen = new LinkedHashSet<>();
        Iterator<Map.Entry<String, JsonNode>> it = templatesNode.fields();
        while (it.hasNext()) {
            String key = it.next().getKey();
            assertTrue(seen.add(key),
                    "duplicate template id in templates.yaml: " + key);
        }
    }

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    private JsonNode readTemplatesYaml() throws IOException {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        try (InputStream is = new ClassPathResource(TEMPLATES_RESOURCE).getInputStream()) {
            return mapper.readTree(is);
        }
    }

    private List<String> readStringArray(JsonNode node) {
        if (node == null || !node.isArray()) {
            return List.of();
        }
        List<String> out = new java.util.ArrayList<>(node.size());
        node.forEach(n -> out.add(n.asText()));
        return out;
    }

    /**
     * Walk up from the working directory to find the project root that
     * contains {@link #DOC_RELATIVE_PATH}, then read the latest version
     * row from the {@code ## 11. Version} table.
     */
    private DocVersion readLatestDocVersion() throws IOException {
        Path docPath = locateDoc();
        List<String> lines = Files.readAllLines(docPath);

        DocVersion latest = null;
        for (String line : lines) {
            Matcher m = DOC_VERSION_ROW.matcher(line);
            if (m.find()) {
                // The doc's last v1.x row is authoritative.
                latest = new DocVersion(m.group(1), m.group(2));
            }
        }
        if (latest == null) {
            fail("could not find a version row 'v<n>[.<n>] | YYYY-MM-DD' in "
                    + docPath);
        }
        return latest;
    }

    private Path locateDoc() {
        Path cwd = Paths.get("").toAbsolutePath();
        for (Path dir = cwd; dir != null; dir = dir.getParent()) {
            Path candidate = dir.resolve(DOC_RELATIVE_PATH);
            if (Files.exists(candidate)) {
                return candidate;
            }
        }
        fail("could not locate " + DOC_RELATIVE_PATH + " from working dir " + cwd);
        return null; // unreachable
    }

    private record DocVersion(String version, String date) {}
}
