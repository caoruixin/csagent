package com.gumtree.csagent.service.guardrails;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Loads script templates from scripts/templates.yaml at startup and provides
 * template lookup and variable substitution for the CS Agent bot.
 *
 * Templates are referenced by category (e.g. "opening", "idle_close") and
 * optionally by sub-key for nested templates (e.g. "hold_placeholder" / "short").
 */
@Slf4j
@Service
public class ScriptLibraryService {

    private static final String TEMPLATES_PATH = "scripts/templates.yaml";
    private static final String OOS_PREFIX = "oos_";

    /** category -> JsonNode (either has "pattern" directly or nested sub-keys) */
    private final Map<String, JsonNode> templates = new LinkedHashMap<>();

    /** OOS category key -> category key in templates map (e.g. "DELIVERY" -> "oos_delivery") */
    private static final Map<String, String> OOS_CATEGORY_MAP = Map.of(
            "DELIVERY", "oos_delivery",
            "PRO_CONTRACT", "oos_pro_contract",
            "ACCOUNT_MANAGER", "oos_account_manager",
            "RATINGS_REVIEWS", "oos_ratings_reviews"
    );

    @PostConstruct
    public void init() {
        try {
            ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
            InputStream is = new ClassPathResource(TEMPLATES_PATH).getInputStream();
            JsonNode root = yamlMapper.readTree(is);

            JsonNode templatesNode = root.get("templates");
            if (templatesNode != null) {
                Iterator<Map.Entry<String, JsonNode>> fields = templatesNode.fields();
                while (fields.hasNext()) {
                    Map.Entry<String, JsonNode> entry = fields.next();
                    templates.put(entry.getKey(), entry.getValue());
                }
            }

            log.info("Loaded {} script templates from {}", templates.size(), TEMPLATES_PATH);

        } catch (Exception e) {
            log.error("Failed to load {}: {}", TEMPLATES_PATH, e.getMessage(), e);
            throw new RuntimeException("Failed to load " + TEMPLATES_PATH, e);
        }
    }

    /**
     * Get the template text for a top-level category.
     * If the category has a direct "pattern" field, returns it.
     * If the category has nested sub-keys, returns the raw JSON text representation.
     *
     * @param category the template category (e.g. "opening", "idle_close")
     * @return the template pattern text, or null if not found
     */
    public String getTemplate(String category) {
        JsonNode node = templates.get(category);
        if (node == null) {
            log.warn("Template category not found: {}", category);
            return null;
        }
        JsonNode pattern = node.get("pattern");
        if (pattern != null) {
            return pattern.asText();
        }
        // For categories with sub-keys but no direct pattern, return null
        // Caller should use getTemplate(category, subKey) instead
        log.debug("Template '{}' has no direct pattern; use getTemplate(category, subKey)", category);
        return null;
    }

    /**
     * Get the template text for a nested sub-key within a category.
     *
     * @param category the template category (e.g. "hold_placeholder", "identifier_request")
     * @param subKey the sub-key within the category (e.g. "short", "email")
     * @return the template pattern text, or null if not found
     */
    public String getTemplate(String category, String subKey) {
        JsonNode node = templates.get(category);
        if (node == null) {
            log.warn("Template category not found: {}", category);
            return null;
        }
        JsonNode subNode = node.get(subKey);
        if (subNode == null) {
            log.warn("Template sub-key not found: {}/{}", category, subKey);
            return null;
        }
        JsonNode pattern = subNode.get("pattern");
        if (pattern != null) {
            return pattern.asText();
        }
        // Sub-key itself is a direct text value
        return subNode.asText();
    }

    /**
     * Render a template by substituting {variable_name} placeholders with provided values.
     *
     * @param category the template category
     * @param variables map of variable names to their values (without braces)
     * @return the rendered text, or null if template not found
     */
    public String renderTemplate(String category, Map<String, String> variables) {
        String template = getTemplate(category);
        if (template == null) {
            return null;
        }
        return substituteVariables(template, variables);
    }

    /**
     * Render a nested template by substituting {variable_name} placeholders with provided values.
     *
     * @param category the template category
     * @param subKey the sub-key within the category
     * @param variables map of variable names to their values (without braces)
     * @return the rendered text, or null if template not found
     */
    public String renderTemplate(String category, String subKey, Map<String, String> variables) {
        String template = getTemplate(category, subKey);
        if (template == null) {
            return null;
        }
        return substituteVariables(template, variables);
    }

    /**
     * Get the appropriate out-of-scope template based on the OOS category.
     * Accepts category keys like "DELIVERY", "PRO_CONTRACT", etc.
     * Falls back to "oos_generic" if the category is not recognized.
     *
     * @param oosCategory the out-of-scope category (e.g. "DELIVERY", "PRO_CONTRACT")
     * @return the template text for the OOS category
     */
    public String getOosTemplate(String oosCategory) {
        String templateKey = OOS_CATEGORY_MAP.get(oosCategory);
        if (templateKey != null) {
            String result = getTemplate(templateKey);
            if (result != null) {
                return result;
            }
        }

        // Also try direct template key lookup (e.g. "oos_delivery" passed directly)
        if (oosCategory != null && oosCategory.startsWith(OOS_PREFIX)) {
            String result = getTemplate(oosCategory);
            if (result != null) {
                return result;
            }
        }

        // Fall back to generic OOS template
        log.info("Using generic OOS template for category: {}", oosCategory);
        return getTemplate("oos_generic");
    }

    private String substituteVariables(String template, Map<String, String> variables) {
        if (variables == null || variables.isEmpty()) {
            return template;
        }
        String result = template;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            result = result.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return result;
    }
}
