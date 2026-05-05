package com.gumtree.csagent.service.runtime;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Sprint 7 §I2 — required-intake-fields registry for UC-G/H/I/J/K.
 *
 * <p>Anchors the {@code intake_state} projection (so the LLM can see which
 * fields are still missing) and the {@code AgentRunLoopImpl} runtime guard
 * (which refuses {@code request_handover(intake_complete_for_uc_X)} when the
 * required field set has not actually been collected). Field names mirror
 * {@code docs/customer_service_tool_spec_v0_2.yaml} §6
 * {@code per_uc_required_fields} for UC-H/J/K and adopt narrow,
 * documented-elsewhere defaults for UC-G/I.
 *
 * <p>This class is intentionally a static registry, not a Spring bean, so it
 * can be referenced from both {@link ContextProjectionBuilder} (projection)
 * and {@link AgentRunLoopImpl} (runtime guard) without dragging
 * Spring-context coupling into the run loop. The contract is:
 * <ul>
 *   <li>{@link #requiredFieldsFor(String)} — canonical ordered required-field
 *       list for an intake UC, or empty for non-intake UCs.</li>
 *   <li>{@link #parseCollectedFields(ObjectMapper, String)} — decode the
 *       {@code session.intakeFields} JSONB blob into a name → string-value
 *       map. Tolerant: returns an empty map on null / blank / parse failure
 *       so the projection never fails closed.</li>
 *   <li>{@link #fieldsRemaining(String, Map)} — required - collected, in
 *       the canonical order.</li>
 *   <li>{@link #intakeComplete(String, Map)} — true iff every required
 *       field is present with a non-empty value.</li>
 * </ul>
 */
public final class IntakeFieldsRegistry {

    private IntakeFieldsRegistry() {
        // static-only registry
    }

    /**
     * Canonical required intake fields per UC. The list ordering matches the
     * canonical spec ordering so the projection's {@code fields_remaining}
     * surface is stable across turns.
     */
    private static final Map<String, List<String>> REQUIRED_FIELDS_BY_UC = Map.of(
            // UC-G — GDPR / data-deletion intake. Narrow defaults: the
            // requestor's registered email + the kind of data action they
            // want (deletion / portability / access).
            "UC-G", List.of("registered_email", "data_request_type"),
            // UC-H — Ad Removal Appeal (canonical from tool spec §6).
            "UC-H", List.of("ad_id_or_listing_url", "registered_email", "stated_reason_or_context"),
            // UC-I — Refund / Payment Dispute. Narrow defaults: transaction
            // reference + dispute reason.
            "UC-I", List.of("transaction_reference", "dispute_reason"),
            // UC-J — Trust & Safety Report (canonical from tool spec §6).
            "UC-J", List.of("report_target", "report_type", "description"),
            // UC-K — Technical Issue Intake (canonical from tool spec §6).
            "UC-K", List.of("platform", "repro_steps_or_error_message")
    );

    /**
     * Synonym aliases for canonical field names. Lets the LLM persist a
     * collected value under a slightly different label without losing
     * coverage (the projection still reports the canonical name as
     * collected). All keys are lower-case.
     */
    private static final Map<String, String> FIELD_ALIASES;

    static {
        Map<String, String> aliases = new LinkedHashMap<>();
        // UC-K
        aliases.put("repro_steps", "repro_steps_or_error_message");
        aliases.put("error_message", "repro_steps_or_error_message");
        aliases.put("steps_to_reproduce", "repro_steps_or_error_message");
        aliases.put("reproduction_steps", "repro_steps_or_error_message");
        aliases.put("os", "platform");
        aliases.put("device", "platform");
        // UC-H
        aliases.put("ad_id", "ad_id_or_listing_url");
        aliases.put("listing_url", "ad_id_or_listing_url");
        aliases.put("email", "registered_email");
        aliases.put("appeal_reason", "stated_reason_or_context");
        aliases.put("reason", "stated_reason_or_context");
        // UC-J
        aliases.put("target", "report_target");
        aliases.put("report_about", "report_target");
        aliases.put("type", "report_type");
        aliases.put("issue_type", "report_type");
        // UC-I
        aliases.put("transaction_id", "transaction_reference");
        aliases.put("payment_reference", "transaction_reference");
        aliases.put("dispute_description", "dispute_reason");
        // UC-G
        aliases.put("request_type", "data_request_type");
        aliases.put("data_action", "data_request_type");
        FIELD_ALIASES = Collections.unmodifiableMap(aliases);
    }

    /**
     * @return true iff {@code uc} has a required-intake-fields entry.
     */
    public static boolean isIntakeUseCase(String uc) {
        return uc != null && REQUIRED_FIELDS_BY_UC.containsKey(uc);
    }

    /**
     * @return the canonical ordered required-fields list for {@code uc}, or
     *         an empty list when {@code uc} is null / non-intake.
     */
    public static List<String> requiredFieldsFor(String uc) {
        if (uc == null) return List.of();
        return REQUIRED_FIELDS_BY_UC.getOrDefault(uc, List.of());
    }

    /**
     * Resolve a possibly-aliased field name to its canonical form. Returns
     * {@code name} unchanged when no alias matches.
     */
    public static String canonicalFieldName(String name) {
        if (name == null) return null;
        String key = name.trim().toLowerCase(Locale.ENGLISH);
        return FIELD_ALIASES.getOrDefault(key, key);
    }

    /**
     * Decode the {@code session.intakeFields} JSONB blob into a
     * {@code canonical-name → value} map. Values are normalised to
     * non-empty strings; null / blank values are dropped. Tolerant —
     * returns an empty map on null / blank / parse failure.
     */
    public static Map<String, String> parseCollectedFields(ObjectMapper objectMapper, String json) {
        Map<String, String> out = new LinkedHashMap<>();
        if (objectMapper == null || json == null || json.isBlank()) {
            return out;
        }
        try {
            JsonNode root = objectMapper.readTree(json);
            if (root == null || !root.isObject()) {
                return out;
            }
            root.fields().forEachRemaining(entry -> {
                JsonNode value = entry.getValue();
                if (value == null || value.isNull()) return;
                String text = value.isValueNode() ? value.asText("") : value.toString();
                if (text == null || text.isBlank()) return;
                out.put(canonicalFieldName(entry.getKey()), text);
            });
        } catch (Exception ex) {
            return new LinkedHashMap<>();
        }
        return out;
    }

    /**
     * Returns the ordered list of required fields not yet present in
     * {@code collected} (preserving canonical ordering). Empty when intake
     * is complete or when {@code uc} is non-intake.
     */
    public static List<String> fieldsRemaining(String uc, Map<String, String> collected) {
        List<String> required = requiredFieldsFor(uc);
        if (required.isEmpty()) return List.of();
        Set<String> have = collected == null ? Set.of() : collected.keySet();
        java.util.ArrayList<String> remaining = new java.util.ArrayList<>();
        for (String field : required) {
            if (!have.contains(field)) {
                remaining.add(field);
            }
        }
        return remaining;
    }

    /**
     * @return true iff every required field for {@code uc} has a non-empty
     *         value in {@code collected}.
     */
    public static boolean intakeComplete(String uc, Map<String, String> collected) {
        List<String> required = requiredFieldsFor(uc);
        if (required.isEmpty()) return false;
        if (collected == null) return false;
        for (String field : required) {
            String value = collected.get(field);
            if (value == null || value.isBlank()) return false;
        }
        return true;
    }

    /**
     * Merge a payload of newly-supplied intake fields into an existing
     * collected map, normalising the field names to canonical form.
     * Returns a new map; the inputs are not mutated.
     */
    public static Map<String, String> mergeFields(Map<String, String> existing,
                                                   Map<String, ?> incoming) {
        Map<String, String> merged = new LinkedHashMap<>();
        if (existing != null) merged.putAll(existing);
        if (incoming != null) {
            for (Map.Entry<String, ?> entry : incoming.entrySet()) {
                Object raw = entry.getValue();
                if (raw == null) continue;
                String text = raw instanceof CharSequence cs ? cs.toString() : raw.toString();
                if (text == null || text.isBlank()) continue;
                merged.put(canonicalFieldName(entry.getKey()), text);
            }
        }
        return merged;
    }

    /**
     * @return all canonical UC ids tracked by this registry.
     */
    public static Set<String> trackedUseCases() {
        return Collections.unmodifiableSet(new LinkedHashSet<>(REQUIRED_FIELDS_BY_UC.keySet()));
    }
}
