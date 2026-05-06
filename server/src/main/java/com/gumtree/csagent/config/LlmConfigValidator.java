package com.gumtree.csagent.config;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * Sprint §C0: fail-fast diagnostic for LLM endpoint / credential configuration.
 *
 * <p>The bot-side Kimi LLM auth is sensitive to a small set of misconfigurations
 * that historically appeared as ambiguous eval flakes (silent 401 swallowed by
 * {@code LlmInvocationService.SAFE_ESCALATION_RESPONSE} → cs001/cs002/cs014
 * looking like generic timeouts). This validator surfaces those at startup
 * so the operator can fix them before running an eval rather than tracing
 * them back from a smoke-result divergence.
 *
 * <p>What is checked (per provider — Kimi primary, DeepSeek fallback):
 * <ul>
 *   <li>API key is present and non-blank.</li>
 *   <li>API key does not look like an unresolved placeholder
 *       ({@code ${KIMI_API_KEY}}, {@code your-api-key-here}, etc.).</li>
 *   <li>Base URL is non-blank and parses as an http(s) URL.</li>
 *   <li>Model name is non-blank.</li>
 * </ul>
 *
 * <p>Output classification:
 * <ul>
 *   <li>{@link Severity#FATAL} — primary provider has a hard misconfiguration
 *       (no key, placeholder key, malformed URL). The validator returns the
 *       diagnostic so callers can throw or log+continue depending on context.</li>
 *   <li>{@link Severity#WARN} — fallback provider has the same issues, or the
 *       primary base URL is the legacy {@code https://api.moonshot.cn/v1}
 *       endpoint, which historically returned 401 in some shells once the
 *       Sprint 7.1 credential rotation moved primary traffic to
 *       {@code https://api.moonshot.ai/v1}.</li>
 * </ul>
 *
 * <p>Secrets are never logged. The validator only logs whether a key is
 * present / placeholder / blank, never the key content.
 */
@Slf4j
public final class LlmConfigValidator {

    /**
     * Working endpoint for this repo's Kimi K2.6 provisioning (post Sprint
     * 7.1 credential rotation; matches {@code .env.local} and
     * {@code application-local.yml} defaults).
     */
    public static final String KIMI_WORKING_ENDPOINT = "https://api.moonshot.ai/v1";

    /**
     * Legacy endpoint that historically held the previous Kimi provisioning.
     * Kept as a constant so the auth-error hint in
     * {@code OpenAiCompatibleLlmClient} can still suggest it as a fallback if
     * the working endpoint starts returning 401 in a given shell.
     */
    public static final String KIMI_LEGACY_ENDPOINT = "https://api.moonshot.cn/v1";

    private LlmConfigValidator() {}

    public enum Severity { OK, WARN, FATAL }

    /** A single config diagnostic. */
    public record Diagnostic(Severity severity, String provider, String code, String message) {
        public boolean isFatal() { return severity == Severity.FATAL; }
        public boolean isWarn() { return severity == Severity.WARN; }
    }

    /** Run the full validation across known providers. */
    public static List<Diagnostic> validate(LlmProperties props) {
        List<Diagnostic> out = new ArrayList<>();
        if (props == null) {
            out.add(new Diagnostic(Severity.FATAL, "llm", "missing_properties",
                    "LlmProperties bean is null"));
            return out;
        }
        validateProvider(out, "kimi", true,
                props.getKimi().getApiKey(), props.getKimi().getBaseUrl(), props.getKimi().getModel());
        validateProvider(out, "deepseek", false,
                props.getDeepseek().getApiKey(), props.getDeepseek().getBaseUrl(), props.getDeepseek().getModel());
        return out;
    }

    /**
     * Throw {@link IllegalStateException} on FATAL diagnostics; log WARN entries.
     * The thrown message lists every fatal finding so the operator can fix all
     * issues in one pass.
     */
    public static void validateOrThrow(LlmProperties props) {
        List<Diagnostic> diagnostics = validate(props);
        for (Diagnostic d : diagnostics) {
            String msg = String.format("LLM config %s [%s/%s]: %s",
                    d.severity(), d.provider(), d.code(), d.message());
            switch (d.severity()) {
                case FATAL -> log.error(msg);
                case WARN -> log.warn(msg);
                case OK -> log.info(msg);
            }
        }
        List<String> fatals = diagnostics.stream()
                .filter(Diagnostic::isFatal)
                .map(d -> d.provider() + "/" + d.code() + ": " + d.message())
                .toList();
        if (!fatals.isEmpty()) {
            throw new IllegalStateException(
                    "LLM configuration is invalid; refusing to start. Issues: " + fatals);
        }
    }

    private static void validateProvider(List<Diagnostic> out,
                                          String provider, boolean isPrimary,
                                          String apiKey, String baseUrl, String model) {
        Severity missingSeverity = isPrimary ? Severity.FATAL : Severity.WARN;

        // API key presence
        if (apiKey == null || apiKey.isBlank()) {
            out.add(new Diagnostic(missingSeverity, provider, "api_key_blank",
                    "api-key is empty or unset"));
        } else if (looksLikePlaceholder(apiKey)) {
            out.add(new Diagnostic(missingSeverity, provider, "api_key_placeholder",
                    "api-key looks like an unresolved placeholder; check .env.local / env vars"));
        }

        // Base URL shape
        if (baseUrl == null || baseUrl.isBlank()) {
            out.add(new Diagnostic(missingSeverity, provider, "base_url_blank",
                    "base-url is empty or unset"));
        } else if (looksLikePlaceholder(baseUrl)) {
            out.add(new Diagnostic(missingSeverity, provider, "base_url_placeholder",
                    "base-url looks like an unresolved placeholder; check .env.local / env vars"));
        } else if (!isHttpUrl(baseUrl)) {
            out.add(new Diagnostic(missingSeverity, provider, "base_url_malformed",
                    "base-url is not a valid http(s) URL: " + baseUrl));
        } else if ("kimi".equals(provider) && KIMI_LEGACY_ENDPOINT.equals(baseUrl.trim())) {
            // Sprint §C0 (post Sprint 7.1 credential rotation): the legacy
            // .cn endpoint still works for some keys but the working
            // endpoint for this repo's current Kimi K2.6 provisioning is
            // the .ai host. Flag it so the operator knows to switch if 401s
            // appear.
            out.add(new Diagnostic(Severity.WARN, provider, "base_url_legacy_warning",
                    "kimi base-url is the legacy '" + KIMI_LEGACY_ENDPOINT + "'. "
                    + "Current Kimi K2.6 provisioning targets '"
                    + KIMI_WORKING_ENDPOINT + "'; switch if chat calls return 401."));
        }

        // Model presence
        if (model == null || model.isBlank()) {
            out.add(new Diagnostic(missingSeverity, provider, "model_blank",
                    "model name is empty or unset"));
        }
    }

    /**
     * Return true if the value looks like an unresolved env-var placeholder or
     * a docs-style stub. Used so `KIMI_API_KEY=${KIMI_API_KEY}` (Spring env
     * resolution miss) can be flagged before it surfaces as a 401.
     */
    static boolean looksLikePlaceholder(String value) {
        if (value == null) return false;
        String v = value.trim();
        if (v.startsWith("${") && v.endsWith("}")) return true;
        if (v.equals("changeme") || v.equals("your-api-key-here")
                || v.equals("placeholder") || v.equals("YOUR_API_KEY")) return true;
        return false;
    }

    static boolean isHttpUrl(String url) {
        if (url == null) return false;
        String trimmed = url.trim();
        if (trimmed.isEmpty()) return false;
        try {
            java.net.URI uri = java.net.URI.create(trimmed);
            String scheme = uri.getScheme();
            return scheme != null
                    && (scheme.equals("http") || scheme.equals("https"))
                    && uri.getHost() != null && !uri.getHost().isBlank();
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    /**
     * Build a human-readable summary of the active LLM lineup that does NOT
     * include secrets. Safe to log at startup.
     */
    public static String describe(LlmProperties props) {
        if (props == null) return "(null)";
        return String.format(
                "primary=kimi[model=%s, base=%s, key=%s], fallback=deepseek[model=%s, base=%s, key=%s]",
                nullToDash(props.getKimi().getModel()),
                nullToDash(props.getKimi().getBaseUrl()),
                describeKeyState(props.getKimi().getApiKey()),
                nullToDash(props.getDeepseek().getModel()),
                nullToDash(props.getDeepseek().getBaseUrl()),
                describeKeyState(props.getDeepseek().getApiKey()));
    }

    private static String nullToDash(String s) { return s == null ? "-" : s; }

    /**
     * Describe a key's state without leaking it. We only emit:
     * {@code present|placeholder|blank}.
     */
    public static String describeKeyState(String key) {
        if (key == null || key.isBlank()) return "blank";
        if (looksLikePlaceholder(key)) return "placeholder";
        return "present";
    }
}
