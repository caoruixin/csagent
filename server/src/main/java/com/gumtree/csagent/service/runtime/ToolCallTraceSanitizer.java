package com.gumtree.csagent.service.runtime;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Sprint 9 §O2 — bounded, sanitized tool-result projection for
 * {@code bot_turns.tool_calls}. Produces:
 *
 * <ul>
 *   <li>{@code result_data}: a JSON-serialisable object suitable for the
 *       Trace UI Result panel. Tool-aware: large objects (e.g.
 *       {@code resolve_article}) are reduced to safe summary fields
 *       ({@code source_id}, {@code title}, {@code source_url}, short
 *       excerpt) instead of the full article body. Always size-bounded
 *       so a runaway tool cannot smuggle a multi-megabyte payload into
 *       the {@code bot_turns} table.</li>
 *   <li>{@code result_summary}: a one-line human-readable status string
 *       suitable for inline display when {@code result_data} is too
 *       structured to skim.</li>
 *   <li>{@code error_message}: a sanitized projection of the underlying
 *       tool error string, suitable for the
 *       {@code bot_turns.tool_calls.error_message} column.</li>
 * </ul>
 *
 * <p>Sprint 9.1 redaction rules (kept local to this class so we don't
 * grow a broad PII framework):
 * <ul>
 *   <li>Sensitive map keys ({@code password}, {@code passwd}, {@code token},
 *       {@code access_token}, {@code refresh_token}, {@code secret},
 *       {@code api_key}, {@code apikey}, {@code authorization},
 *       {@code auth_header}, {@code bearer}, {@code credential},
 *       {@code credentials}) — values replaced with
 *       {@code [REDACTED_SECRET]} regardless of contents.</li>
 *   <li>Sensitive-shaped values:
 *     <ul>
 *       <li>email addresses → {@code [REDACTED_EMAIL]}</li>
 *       <li>phone-like sequences (10+ digits with optional separators)
 *           → {@code [REDACTED_PHONE]}</li>
 *       <li>UK postcodes → {@code [REDACTED_POSTCODE]}</li>
 *       <li>{@code Bearer ...} authorization headers
 *           → {@code [REDACTED_BEARER]}</li>
 *       <li>API-key-like prefixes (e.g. {@code sk_abcd...},
 *           {@code api_xxx...}) → {@code [REDACTED_API_KEY]}</li>
 *       <li>long random credential-like strings (32+ chars of
 *           {@code [A-Za-z0-9_\-]}) → {@code [REDACTED_TOKEN]}</li>
 *     </ul>
 *   </li>
 *   <li>Strings are truncated; maps and lists are bounded by size and
 *       depth so a runaway tool cannot smuggle a multi-megabyte payload
 *       through.</li>
 * </ul>
 */
final class ToolCallTraceSanitizer {

    private ToolCallTraceSanitizer() {}

    /** Maximum characters per leaf string in {@code result_data}. */
    private static final int MAX_STRING_LEN = 240;

    /** Maximum entries kept on a list/array. */
    private static final int MAX_LIST_LEN = 5;

    /** Maximum keys kept on a map. */
    private static final int MAX_MAP_KEYS = 12;

    /** Maximum {@code result_summary} length. */
    private static final int MAX_SUMMARY_LEN = 240;

    /** Maximum length of a sanitized {@code error_message}. */
    private static final int MAX_ERROR_LEN = 240;

    private static final Pattern EMAIL = Pattern.compile(
            "[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}");

    /**
     * {@code Authorization: Bearer xxxxx} headers and similar inline
     * bearer tokens. Match before the long-token rule so the
     * {@code Bearer} prefix gets its own redaction marker.
     */
    private static final Pattern BEARER = Pattern.compile(
            "(?i)\\bbearer\\s+[A-Za-z0-9_\\-.~+/]+=*");

    /**
     * Common API-key prefixes ({@code sk_...}, {@code pk_...},
     * {@code api_...}, {@code key_...}, {@code tok_...}, {@code rk_...}
     * with at least 8 trailing characters). Match before the long-token
     * rule so the prefix is preserved in the marker.
     */
    private static final Pattern API_KEY_PREFIX = Pattern.compile(
            "(?i)\\b(?:sk|pk|rk|api|key|tok)[_\\-][A-Za-z0-9_\\-]{8,}\\b");

    /**
     * UK postcode forms — {@code SW1A 1AA}, {@code EC1A 1BB},
     * {@code M1 1AA}, {@code GIR 0AA}. Word-bounded.
     */
    private static final Pattern UK_POSTCODE = Pattern.compile(
            "(?i)\\b(?:gir\\s?0aa|[a-z]{1,2}\\d{1,2}[a-z]?\\s?\\d[a-z]{2})\\b");

    /**
     * Phone-like sequences. A {@code +} prefix is optional; the body is
     * 10+ digits possibly broken by spaces, dashes, dots, or parentheses,
     * and must end on a digit. Picks up {@code +44 20 7946 0958},
     * {@code (555) 555-5555}, {@code 020 7946 0958}, {@code 07911 123456}.
     * Conservative on the lower bound to avoid eating short numeric
     * fields like ad ids ({@code AD-1001}) or scores.
     */
    private static final Pattern PHONE_LIKE = Pattern.compile(
            "\\+?\\d(?:[\\d\\s().\\-]{8,18})\\d");

    /**
     * Long random credential-like strings. 32+ word-ish characters with
     * no spaces — JWT fragments, hex digests, base64-ish secrets. URL
     * components rarely exceed 32 unbroken characters once you account
     * for {@code /}, {@code .}, {@code ?}, {@code &} so the false
     * positive rate is acceptable here.
     */
    private static final Pattern LONG_TOKEN = Pattern.compile(
            "\\b[A-Za-z0-9_\\-]{32,}\\b");

    /**
     * Map keys whose value should be replaced with
     * {@code [REDACTED_SECRET]} regardless of the value's content. All
     * comparisons are case-insensitive after collapsing common
     * separators ({@code -}, {@code ' '}) to {@code _} so e.g.
     * {@code Auth-Header}, {@code auth header}, and
     * {@code authHeader} all hit the same rule.
     */
    private static final Set<String> SENSITIVE_KEYS = Set.of(
            "password",
            "passwd",
            "token",
            "access_token",
            "refresh_token",
            "id_token",
            "secret",
            "client_secret",
            "api_key",
            "apikey",
            "x_api_key",
            "authorization",
            "auth_header",
            "bearer",
            "credential",
            "credentials"
    );

    /**
     * Build a bounded, sanitized projection of {@code resultData} for the
     * trace. {@code null}-safe; returns {@code null} when nothing useful
     * can be surfaced (the caller skips the {@code result_data} field in
     * that case so old rows keep rendering safely).
     */
    static Object sanitizeResultData(String toolName, Object resultData) {
        if (resultData == null) return null;
        if (resultData instanceof Map<?, ?> map) {
            return sanitizeForTool(toolName, map);
        }
        return sanitizeAny(resultData, 0);
    }

    /**
     * Sprint 9.1 — sanitize the verbatim tool error message before it
     * is persisted to {@code bot_turns.tool_calls.error_message} or
     * surfaced through {@link #summarize}. Applies the same redaction
     * rules used for {@code result_data} string leaves and bounds the
     * length so a runaway exception cannot dump a stack trace into the
     * trace column.
     */
    static String sanitizeErrorMessage(String errorMessage) {
        if (errorMessage == null) return null;
        String redacted = redactPii(errorMessage);
        if (redacted.length() <= MAX_ERROR_LEN) return redacted;
        return redacted.substring(0, MAX_ERROR_LEN - 1) + "…";
    }

    /**
     * Build a one-line summary for the tool call. Failed calls carry
     * the error surface; successful calls carry a tool-specific
     * one-liner (e.g. {@code "1 hit"} for {@code search_knowledge}).
     *
     * <p>Sprint 9.1: failed-tool summaries route the error string
     * through {@link #sanitizeErrorMessage} so the same redaction rules
     * apply whether the trace surface is the {@code error_message}
     * column or the inline {@code result_summary} field.
     */
    static String summarize(String toolName, boolean success,
                            Object resultData, String errorMessage) {
        if (!success) {
            String safe = sanitizeErrorMessage(errorMessage);
            if (safe == null) safe = "tool_failed";
            return truncateSummary("error: " + safe);
        }
        if (resultData == null) return "ok";
        if (resultData instanceof Map<?, ?> rawMap) {
            @SuppressWarnings("unchecked")
            Map<String, Object> m = (Map<String, Object>) rawMap;
            switch (toolName == null ? "" : toolName) {
                case "search_knowledge": {
                    Object hits = m.get("hits");
                    int n = (hits instanceof List<?> l) ? l.size() : 0;
                    Object faqMiss = m.get("faq_miss");
                    return truncateSummary(n + " hit" + (n == 1 ? "" : "s")
                            + (Boolean.TRUE.equals(faqMiss) ? " (faq_miss)" : ""));
                }
                case "resolve_article": {
                    Object sid = firstNonNull(m.get("source_id"), m.get("article_id"));
                    Object title = m.get("title");
                    StringBuilder sb = new StringBuilder("resolved");
                    if (sid != null) sb.append(' ').append(sid);
                    if (title != null) sb.append(": ").append(redactPii(String.valueOf(title)));
                    return truncateSummary(sb.toString());
                }
                case "record_outcome": {
                    Object oc = firstNonNull(m.get("outcome_class"), m.get("outcome"));
                    return truncateSummary("outcome=" + oc);
                }
                case "request_handover": {
                    Object reason = m.get("escalation_reason");
                    Object xfer = m.get("transfer_result");
                    StringBuilder sb = new StringBuilder("handover");
                    if (reason != null) sb.append(' ').append(reason);
                    if (xfer != null) sb.append(" -> ").append(xfer);
                    return truncateSummary(sb.toString());
                }
                case "classify_use_case": {
                    Object uc = m.get("use_case_id");
                    Object conf = m.get("confidence");
                    return truncateSummary("uc=" + uc
                            + (conf != null ? " conf=" + conf : ""));
                }
                case "get_customer_context": {
                    Object found = m.get("found");
                    return truncateSummary("customer_context found=" + found);
                }
                default:
                    return truncateSummary("ok");
            }
        }
        return truncateSummary("ok");
    }

    private static Object firstNonNull(Object a, Object b) {
        return a != null ? a : b;
    }

    /**
     * Tool-aware sanitization of a Map result.
     * For {@code resolve_article}, the full article body is dropped in
     * favour of the safe summary fields. All other tools share the
     * generic bounded recursive sanitizer.
     */
    private static Object sanitizeForTool(String toolName, Map<?, ?> map) {
        if ("resolve_article".equals(toolName)) {
            Map<String, Object> safe = new LinkedHashMap<>();
            putIfPresent(safe, map, "source_id");
            putIfPresent(safe, map, "article_id");
            putIfPresent(safe, map, "title");
            putIfPresent(safe, map, "source_url");
            // Description / summary are public help-centre content; truncate
            // to keep the trace row under control even when an article body
            // is several KB.
            Object summary = map.get("summary");
            Object description = map.get("description");
            String excerpt = (summary instanceof String s && !s.isBlank())
                    ? s
                    : (description instanceof String d ? d : null);
            if (excerpt != null) {
                safe.put("excerpt", truncateString(excerpt));
            }
            return safe;
        }
        return sanitizeAny(map, 0);
    }

    private static void putIfPresent(Map<String, Object> dst, Map<?, ?> src, String key) {
        Object v = src.get(key);
        if (v == null) return;
        if (v instanceof String s) {
            dst.put(key, truncateString(s));
        } else {
            dst.put(key, v);
        }
    }

    /** Recursive bounded sanitizer for arbitrary tool result shapes. */
    private static Object sanitizeAny(Object v, int depth) {
        if (v == null) return null;
        if (depth > 4) return "[truncated:depth]";
        if (v instanceof String s) {
            return truncateString(s);
        }
        if (v instanceof Number || v instanceof Boolean) {
            return v;
        }
        if (v instanceof Map<?, ?> m) {
            Map<String, Object> out = new LinkedHashMap<>();
            int n = 0;
            for (Map.Entry<?, ?> e : m.entrySet()) {
                if (n++ >= MAX_MAP_KEYS) {
                    out.put("__truncated__", "map size=" + m.size());
                    break;
                }
                String k = String.valueOf(e.getKey());
                if (isSensitiveKey(k)) {
                    // Sprint 9.1: redact the whole value when the key
                    // names a credential / token / authorization slot.
                    out.put(k, "[REDACTED_SECRET]");
                } else {
                    out.put(k, sanitizeAny(e.getValue(), depth + 1));
                }
            }
            return out;
        }
        if (v instanceof List<?> l) {
            int size = l.size();
            int cap = Math.min(size, MAX_LIST_LEN);
            java.util.List<Object> out = new java.util.ArrayList<>(cap + (size > cap ? 1 : 0));
            for (int i = 0; i < cap; i++) {
                out.add(sanitizeAny(l.get(i), depth + 1));
            }
            if (size > cap) {
                out.add("[truncated:" + (size - cap) + " more]");
            }
            return out;
        }
        if (v.getClass().isArray()) {
            // Convert primitive / object arrays to a bounded list shape.
            if (v instanceof Object[] arr) {
                int size = arr.length;
                int cap = Math.min(size, MAX_LIST_LEN);
                java.util.List<Object> out = new java.util.ArrayList<>(cap + (size > cap ? 1 : 0));
                for (int i = 0; i < cap; i++) {
                    out.add(sanitizeAny(arr[i], depth + 1));
                }
                if (size > cap) {
                    out.add("[truncated:" + (size - cap) + " more]");
                }
                return out;
            }
            return "[array]";
        }
        return truncateString(v.toString());
    }

    /**
     * True when the map key names a credential / token / authorization
     * slot. Comparison is case-insensitive after collapsing common
     * separators ({@code -}, {@code ' '}) into {@code _}.
     */
    private static boolean isSensitiveKey(String key) {
        if (key == null || key.isBlank()) return false;
        String norm = key.toLowerCase(Locale.ENGLISH)
                .replace('-', '_')
                .replace(' ', '_');
        return SENSITIVE_KEYS.contains(norm);
    }

    private static String truncateString(String s) {
        if (s == null) return null;
        String redacted = redactPii(s);
        if (redacted.length() <= MAX_STRING_LEN) return redacted;
        return redacted.substring(0, MAX_STRING_LEN - 1) + "…";
    }

    private static String truncateSummary(String s) {
        if (s == null) return null;
        if (s.length() <= MAX_SUMMARY_LEN) return s;
        return s.substring(0, MAX_SUMMARY_LEN - 1) + "…";
    }

    /**
     * Apply every redaction pattern in priority order. Order matters:
     * <ol>
     *   <li>{@link #EMAIL} — specific @-bearing shape.</li>
     *   <li>{@link #BEARER} / {@link #API_KEY_PREFIX} — keep their
     *       prefix-specific markers before the generic
     *       {@link #LONG_TOKEN} sweep eats the same characters.</li>
     *   <li>{@link #LONG_TOKEN} — runs BEFORE {@link #PHONE_LIKE} so a
     *       long hex / base64 credential like a 64-char SHA-256 digest
     *       gets a single {@code [REDACTED_TOKEN]} marker instead of
     *       being shredded by the digit-block phone matcher.</li>
     *   <li>{@link #PHONE_LIKE} — digit-only sequences left over after
     *       the long-token sweep.</li>
     *   <li>{@link #UK_POSTCODE} — last so we don't accidentally label
     *       a number block as a postcode.</li>
     * </ol>
     */
    private static String redactPii(String s) {
        if (s == null) return null;
        String out = s;
        out = EMAIL.matcher(out).replaceAll("[REDACTED_EMAIL]");
        out = BEARER.matcher(out).replaceAll("[REDACTED_BEARER]");
        out = API_KEY_PREFIX.matcher(out).replaceAll("[REDACTED_API_KEY]");
        out = LONG_TOKEN.matcher(out).replaceAll("[REDACTED_TOKEN]");
        out = PHONE_LIKE.matcher(out).replaceAll("[REDACTED_PHONE]");
        out = UK_POSTCODE.matcher(out).replaceAll("[REDACTED_POSTCODE]");
        return out;
    }
}
