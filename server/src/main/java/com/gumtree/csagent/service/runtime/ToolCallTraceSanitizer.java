package com.gumtree.csagent.service.runtime;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Sprint 9 §O2 — bounded tool-result projection for {@code bot_turns.tool_calls}.
 * Produces:
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
 *   <li>{@code error_message}: a length-bounded copy of the underlying
 *       tool or guardrail error string for
 *       {@code bot_turns.tool_calls.error_message}.</li>
 * </ul>
 *
 * <p>Demo / admin-trace policy: persist diagnostic text verbatim (guardrail
 * predicate names, Salesforce {@code source_id}s, tool errors). Do not run
 * broad value-shape PII regexes here — those produced false positives on
 * {@code progressive_resolve_record_outcome_premature} and KB article ids.
 * User-message email redaction lives in {@link ContextProjectionBuilder}
 * only. Values under credential-like map keys are still replaced with
 * {@code [REDACTED_SECRET]} regardless of contents.
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
     * Bound the verbatim tool or guardrail error message before it is
     * persisted to {@code bot_turns.tool_calls.error_message} or surfaced
     * through {@link #summarize}.
     */
    static String sanitizeErrorMessage(String errorMessage) {
        if (errorMessage == null) return null;
        return truncateToMax(errorMessage, MAX_ERROR_LEN);
    }

    /**
     * Build a one-line summary for the tool call. Failed calls carry
     * the error surface; successful calls carry a tool-specific
     * one-liner (e.g. {@code "1 hit"} for {@code search_knowledge}).
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
                    if (title != null) sb.append(": ").append(title);
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

    private static boolean isSensitiveKey(String key) {
        if (key == null || key.isBlank()) return false;
        String norm = key.toLowerCase(Locale.ENGLISH)
                .replace('-', '_')
                .replace(' ', '_');
        return SENSITIVE_KEYS.contains(norm);
    }

    private static String truncateString(String s) {
        return truncateToMax(s, MAX_STRING_LEN);
    }

    private static String truncateSummary(String s) {
        return truncateToMax(s, MAX_SUMMARY_LEN);
    }

    private static String truncateToMax(String s, int maxLen) {
        if (s == null) return null;
        if (s.length() <= maxLen) return s;
        return s.substring(0, maxLen - 1) + "…";
    }
}
