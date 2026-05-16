package com.gumtree.csagent.service.runtime;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Sprint 7.1 §J0 — narrow per-UC extractor that reads a clarification turn
 * and the form context, and returns the canonical intake field values that
 * can be persisted into {@code session.intakeFields} so the next
 * {@code intake_state} projection reflects what the user has already
 * supplied.
 *
 * <p>This sits beside {@link IntakeFieldsRegistry}: the registry owns the
 * canonical required-field set + alias map, while this extractor owns the
 * per-UC heuristics that turn user-typed text into canonical-name → value
 * pairs. The split keeps the registry pure (no NLP) and the extractor
 * narrowly scoped to the UCs anchored by the targeted regression set.
 *
 * <p>UC scope:
 * <ul>
 *   <li>UC-K (Technical Issue Intake) — {@code platform} +
 *       {@code repro_steps_or_error_message}, anchored on cs066.
 *       User-text mining (via {@link #PLATFORM_TOKEN_PATTERN} +
 *       {@link #REGRESSION_MARKER_PATTERN}).</li>
 *   <li>UC-G / UC-H / UC-I / UC-J (Sprint 34) — pure FORM-CONTEXT
 *       field-mapping plumbing. For each canonical required field that
 *       has a form_context source-of-truth in
 *       {@link FormContextIngestionService} (first_name / email /
 *       topic_subject / description / optional ad_id), seed the
 *       canonical name onto {@code session.intakeFields} so the next
 *       projection's {@code intake_state.fields_collected} reflects
 *       what the form already supplied. No regex on user-typed text;
 *       the LLM still owns capture of any inline user-supplied
 *       intake_fields via
 *       {@code AgentRunLoopImpl.persistInlineIntakeFields}.</li>
 * </ul>
 *
 * <p>UC-I is wired as a deliberate no-op today: the canonical required
 * fields ({@code transaction_reference}, {@code dispute_reason}) have no
 * corresponding form_context source-of-truth in
 * {@link FormContextIngestionService}. The branch exists for symmetry
 * with the other intake UCs so a future sprint that adds a
 * transaction-reference form field can extend the branch without
 * re-touching the dispatch structure.
 */
public final class IntakeFieldExtractor {

    private IntakeFieldExtractor() {
        // static-only helper
    }

    /**
     * Tokens that, when present in the user's text, identify a platform
     * answer for UC-K. Matched case-insensitively against word boundaries.
     */
    private static final List<String> PLATFORM_TOKENS = List.of(
            "chrome", "firefox", "safari", "edge", "opera", "brave",
            "android", "ios", "iphone", "ipad",
            "windows", "macos", "mac os", "mac", "linux",
            "desktop", "mobile", "tablet",
            "web", "browser",
            "app");

    private static final Pattern PLATFORM_TOKEN_PATTERN = Pattern.compile(
            "\\b(" + String.join("|", PLATFORM_TOKENS) + ")\\b",
            Pattern.CASE_INSENSITIVE);

    /**
     * Markers that suggest the text is describing a regression / error /
     * missing-feature scenario. cs066 form description matches "not" +
     * "any more"; other targeted UC-K shapes match "doesn't", "broken",
     * "error", etc.
     */
    private static final Pattern REGRESSION_MARKER_PATTERN = Pattern.compile(
            "\\b(not|n[''’]t|won[''’]t|doesn[''’]t|isn[''’]t|can[''’]t|cannot|"
                    + "missing|disappeared|disappear|removed|gone|lost|broken|"
                    + "error|fail|failure|failed|crash|crashes|crashed|"
                    + "no longer|any\\s*more|anymore|"
                    + "doesn[''’]t\\s+work|doesn[''’]t\\s+show|stopped\\s+working)\\b",
            Pattern.CASE_INSENSITIVE);

    /** Minimum length for a piece of text to qualify as repro_steps. */
    private static final int MIN_REPRO_LENGTH = 20;

    /**
     * Inspect the current user message and form context for canonical
     * intake field values for {@code uc}. Returned map only contains
     * canonical keys whose values were positively identified — callers
     * merge into the existing collected map via
     * {@link IntakeFieldsRegistry#mergeFields}.
     *
     * <p>Tolerant: never throws on null / blank / unparseable inputs.
     */
    public static Map<String, String> extractFromTurn(String uc,
                                                     String userMessage,
                                                     String formContextJson,
                                                     ObjectMapper objectMapper) {
        Map<String, String> extracted = new LinkedHashMap<>();
        if (uc == null) {
            return extracted;
        }

        if ("UC-K".equals(uc)) {
            extractUcKFields(extracted, userMessage,
                    extractFormDescription(formContextJson, objectMapper));
        } else if ("UC-H".equals(uc)) {
            extractUcHFields(extracted, formContextJson, objectMapper);
        } else if ("UC-G".equals(uc)) {
            extractUcGFields(extracted, formContextJson, objectMapper);
        } else if ("UC-J".equals(uc)) {
            extractUcJFields(extracted, formContextJson, objectMapper);
        } else if ("UC-I".equals(uc)) {
            extractUcIFields(extracted, formContextJson, objectMapper);
        }
        return extracted;
    }

    private static void extractUcKFields(Map<String, String> out,
                                         String userMessage,
                                         String formDescription) {
        // platform — capture from the user's reply first (their direct
        // answer to the platform clarification question), then fall back
        // to the form description if the user already disclosed it there.
        String platform = capturePlatform(userMessage);
        if (platform == null) {
            platform = capturePlatform(formDescription);
        }
        if (platform != null) {
            out.put("platform", platform);
        }

        // repro_steps_or_error_message — seed from the form description
        // when it already describes the regression (cs066 anchor: "Why am
        // I not getting the option ... any more"). Only fall back to the
        // user message when the form description is silent AND the user
        // typed something substantive. This avoids capturing the platform
        // reply ("I'm using Chrome on Windows") as repro steps.
        String repro = captureRegressionText(formDescription);
        if (repro == null) {
            repro = captureRegressionText(userMessage);
        }
        if (repro != null) {
            out.put("repro_steps_or_error_message", repro);
        }
    }

    /**
     * Sprint 34 — UC-H (Ad Removal Appeal) form-context field mapping.
     * Reads {@code form_context.ad_id} → canonical
     * {@code ad_id_or_listing_url} and {@code form_context.email} →
     * canonical {@code registered_email} per the alias entries already
     * defined in {@link IntakeFieldsRegistry#FIELD_ALIASES} (lines
     * 87-89). The third required field
     * ({@code stated_reason_or_context}) has no form_context source —
     * the LLM owns capturing it from the conversation via
     * {@code request_handover.arguments.intake_fields}.
     */
    private static void extractUcHFields(Map<String, String> out,
                                         String formContextJson,
                                         ObjectMapper objectMapper) {
        String adId = extractFormContextField(formContextJson, "ad_id", objectMapper);
        if (adId != null) {
            out.put("ad_id_or_listing_url", adId);
        }
        String email = extractFormContextField(formContextJson, "email", objectMapper);
        if (email != null) {
            out.put("registered_email", email);
        }
    }

    /**
     * Sprint 34 — UC-G (GDPR / data-action intake) form-context field
     * mapping. Reads {@code form_context.email} → canonical
     * {@code registered_email}. The {@code data_request_type} field has
     * no form_context source — it surfaces from the user-driven
     * conversation via the LLM's inline intake_fields path.
     */
    private static void extractUcGFields(Map<String, String> out,
                                         String formContextJson,
                                         ObjectMapper objectMapper) {
        String email = extractFormContextField(formContextJson, "email", objectMapper);
        if (email != null) {
            out.put("registered_email", email);
        }
    }

    /**
     * Sprint 34 — UC-J (Trust & Safety report intake) form-context field
     * mapping. Reads {@code form_context.description} → canonical
     * {@code description} (canonical name and form-context key
     * coincide). The other required fields ({@code report_target} +
     * {@code report_type}) have no form_context source.
     */
    private static void extractUcJFields(Map<String, String> out,
                                         String formContextJson,
                                         ObjectMapper objectMapper) {
        String description = extractFormContextField(formContextJson, "description", objectMapper);
        if (description != null) {
            out.put("description", description);
        }
    }

    /**
     * Sprint 34 — UC-I (Refund / payment-dispute intake) form-context
     * field mapping. Deliberate no-op today: the canonical required
     * fields ({@code transaction_reference}, {@code dispute_reason})
     * have no source-of-truth in
     * {@link FormContextIngestionService}'s form_context shape
     * (first_name / email / topic_subject / description / optional
     * ad_id). The branch exists for structural symmetry with the other
     * intake UCs — a future sprint that adds a transaction-reference
     * form field can extend this method without re-touching the
     * dispatch in {@link #extractFromTurn}. Surfaced as Sprint 34 OQ in
     * the handoff.
     */
    private static void extractUcIFields(Map<String, String> out,
                                         String formContextJson,
                                         ObjectMapper objectMapper) {
        // No-op: no form_context source for transaction_reference /
        // dispute_reason. Reads form_context only to keep the method
        // signature uniform with the other UC helpers; result is unused.
        if (formContextJson == null) {
            return;
        }
        // Intentionally empty body — see Javadoc.
    }

    /**
     * If {@code text} mentions a known platform token, return the trimmed
     * text as the platform value (the user's full reply is the most
     * faithful representation of "what platform are you on?"). Returns
     * {@code null} when no platform token is found.
     */
    static String capturePlatform(String text) {
        if (text == null || text.isBlank()) return null;
        if (!PLATFORM_TOKEN_PATTERN.matcher(text).find()) return null;
        String trimmed = text.trim();
        // Cap absurdly long replies so the persisted intake_fields blob
        // stays bounded; the projection still surfaces the field as
        // collected, which is what the LLM needs to skip the question.
        if (trimmed.length() > 240) {
            trimmed = trimmed.substring(0, 240) + "…";
        }
        return trimmed;
    }

    /**
     * If {@code text} contains regression / error markers AND is long
     * enough to be a useful description, return the trimmed text. Returns
     * {@code null} otherwise.
     */
    static String captureRegressionText(String text) {
        if (text == null) return null;
        String trimmed = text.trim();
        if (trimmed.length() < MIN_REPRO_LENGTH) return null;
        if (!REGRESSION_MARKER_PATTERN.matcher(trimmed).find()) return null;
        if (trimmed.length() > 480) {
            trimmed = trimmed.substring(0, 480) + "…";
        }
        return trimmed;
    }

    /**
     * Pull {@code description} out of {@code session.formContext} JSONB.
     * Returns {@code null} on missing / blank / parse failure.
     */
    static String extractFormDescription(String formContextJson, ObjectMapper objectMapper) {
        if (formContextJson == null || formContextJson.isBlank() || objectMapper == null) {
            return null;
        }
        try {
            JsonNode root = objectMapper.readTree(formContextJson);
            if (root == null || !root.isObject()) return null;
            JsonNode descNode = root.get("description");
            if (descNode == null || descNode.isNull()) return null;
            String text = descNode.isValueNode() ? descNode.asText("") : descNode.toString();
            return (text == null || text.isBlank()) ? null : text;
        } catch (Exception ex) {
            return null;
        }
    }

    /**
     * Sprint 34 — generalised form_context field reader. Pulls
     * {@code fieldName} out of {@code session.formContext} JSONB and
     * returns the trimmed string value, or {@code null} on missing /
     * blank / parse failure. Mirrors {@link #extractFormDescription}
     * but accepts an arbitrary field name (e.g. {@code "ad_id"},
     * {@code "email"}) so the per-UC helpers can read form-context
     * source-of-truth fields without each duplicating the parse +
     * null-handling pattern. Tolerant — never throws on malformed JSON.
     */
    static String extractFormContextField(String formContextJson,
                                          String fieldName,
                                          ObjectMapper objectMapper) {
        if (formContextJson == null || formContextJson.isBlank()
                || objectMapper == null || fieldName == null || fieldName.isBlank()) {
            return null;
        }
        try {
            JsonNode root = objectMapper.readTree(formContextJson);
            if (root == null || !root.isObject()) return null;
            JsonNode node = root.get(fieldName);
            if (node == null || node.isNull()) return null;
            String text = node.isValueNode() ? node.asText("") : node.toString();
            return (text == null || text.isBlank()) ? null : text;
        } catch (Exception ex) {
            return null;
        }
    }

    /**
     * Convenience helper used by the run loop: extract canonical fields
     * for {@code uc} from {@code userMessage} + {@code session.formContext},
     * merge them into the existing collected map, and return the result.
     * Returns the input {@code existing} unchanged when nothing new is
     * extracted (caller can compare references / equality to skip a write).
     */
    public static Map<String, String> mergeForUc(String uc,
                                                 Map<String, String> existing,
                                                 String userMessage,
                                                 String formContextJson,
                                                 ObjectMapper objectMapper) {
        Map<String, String> incoming = extractFromTurn(uc, userMessage, formContextJson, objectMapper);
        if (incoming.isEmpty()) {
            return existing;
        }
        // Filter out fields that are already present so we never overwrite
        // a value the LLM persisted via inline intake_fields.
        Map<String, String> additions = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : incoming.entrySet()) {
            String canonical = IntakeFieldsRegistry.canonicalFieldName(entry.getKey());
            if (existing != null && existing.containsKey(canonical)) continue;
            additions.put(canonical, entry.getValue());
        }
        if (additions.isEmpty()) {
            return existing;
        }
        return IntakeFieldsRegistry.mergeFields(existing, additions);
    }

    /**
     * @return true iff this extractor has per-UC heuristics or
     *         form-context plumbing for {@code uc}. Useful for tests
     *         and for the run loop's "is this worth invoking?" check.
     *         UC-I returns {@code true} despite the helper being a
     *         no-op today (per the Javadoc on
     *         {@link #extractUcIFields}) so the dispatch surface
     *         covers every {@link IntakeFieldsRegistry} intake UC.
     */
    public static boolean handlesUc(String uc) {
        return "UC-G".equals(uc) || "UC-H".equals(uc)
                || "UC-I".equals(uc) || "UC-J".equals(uc)
                || "UC-K".equals(uc);
    }

    @SuppressWarnings("unused") // kept for symmetry with existing registry helpers
    private static String lower(String s) {
        return s == null ? null : s.toLowerCase(Locale.ENGLISH);
    }
}
