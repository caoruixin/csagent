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
 * <p>Initial UC scope:
 * <ul>
 *   <li>UC-K (Technical Issue Intake) — {@code platform} +
 *       {@code repro_steps_or_error_message}, anchored on cs066.</li>
 * </ul>
 *
 * <p>Other intake UCs (UC-G/H/I/J) currently rely on the LLM to provide
 * fields via {@code request_handover.arguments.intake_fields}, which the
 * existing {@code AgentRunLoopImpl.persistInlineIntakeFields} hook
 * persists. They can be added here later if a targeted blocker shows the
 * partial-intake gap on those UCs too.
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
     * @return the lowercase canonical UC IDs for which this extractor has
     *         per-UC heuristics. Useful for tests and for the run loop's
     *         "is this worth invoking?" check.
     */
    public static boolean handlesUc(String uc) {
        return "UC-K".equals(uc);
    }

    @SuppressWarnings("unused") // kept for symmetry with existing registry helpers
    private static String lower(String s) {
        return s == null ? null : s.toLowerCase(Locale.ENGLISH);
    }
}
