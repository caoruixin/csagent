package com.gumtree.csagent.service.runtime;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Sprint 9 §O2 — bounded sanitized projection unit tests. Pin the
 * size cap, PII redaction, and tool-aware {@code resolve_article}
 * summary fields so the {@code bot_turns.tool_calls} trace stays
 * non-empty without leaking large or sensitive payloads.
 */
class ToolCallTraceSanitizerTest {

    @Test
    void sanitize_resolveArticle_dropsLargeBodyKeepsSummaryFields() {
        StringBuilder big = new StringBuilder();
        for (int i = 0; i < 5_000; i++) big.append('x');

        Map<String, Object> articlePayload = new LinkedHashMap<>();
        articlePayload.put("source_id", "kb-001");
        articlePayload.put("article_id", "kb-001");
        articlePayload.put("title", "Where is my advert?");
        articlePayload.put("source_url", "https://help.example/faq/where-is-my-ad");
        articlePayload.put("description", big.toString());
        articlePayload.put("uc_tags", new String[]{"UC-A", "UC-B"});
        articlePayload.put("is_published", true);

        Object sanitized = ToolCallTraceSanitizer.sanitizeResultData(
                "resolve_article", articlePayload);
        assertNotNull(sanitized);
        @SuppressWarnings("unchecked")
        Map<String, Object> safe = (Map<String, Object>) sanitized;

        assertEquals("kb-001", safe.get("source_id"));
        assertEquals("Where is my advert?", safe.get("title"));
        assertEquals("https://help.example/faq/where-is-my-ad", safe.get("source_url"));
        assertFalse(safe.containsKey("description"),
                "raw description should be dropped — replaced by truncated 'excerpt'");
        Object excerpt = safe.get("excerpt");
        assertNotNull(excerpt);
        assertTrue(((String) excerpt).length() < 300,
                "excerpt must be size-bounded so the trace row cannot grow unbounded");
    }

    @Test
    void sanitize_searchKnowledge_truncatesHits() {
        Map<String, Object> hit1 = Map.of("source_id", "kb-1", "title", "h1", "score", 0.9);
        Map<String, Object> hit2 = Map.of("source_id", "kb-2", "title", "h2", "score", 0.8);
        Map<String, Object> hit3 = Map.of("source_id", "kb-3", "title", "h3", "score", 0.7);
        Map<String, Object> hit4 = Map.of("source_id", "kb-4", "title", "h4", "score", 0.6);
        Map<String, Object> hit5 = Map.of("source_id", "kb-5", "title", "h5", "score", 0.5);
        Map<String, Object> hit6 = Map.of("source_id", "kb-6", "title", "h6", "score", 0.4);
        Map<String, Object> hit7 = Map.of("source_id", "kb-7", "title", "h7", "score", 0.3);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("faq_miss", false);
        result.put("hits", List.of(hit1, hit2, hit3, hit4, hit5, hit6, hit7));

        Object sanitized = ToolCallTraceSanitizer.sanitizeResultData("search_knowledge", result);
        assertNotNull(sanitized);
        @SuppressWarnings("unchecked")
        Map<String, Object> safe = (Map<String, Object>) sanitized;
        @SuppressWarnings("unchecked")
        List<Object> hits = (List<Object>) safe.get("hits");
        assertTrue(hits.size() <= 6, "hits list must be size-capped");
    }

    @Test
    void sanitize_redactsEmailFromStringFields() {
        Map<String, Object> payload = Map.of(
                "title", "Contact alice@example.com for details",
                "source_id", "kb-pii");

        Object sanitized = ToolCallTraceSanitizer.sanitizeResultData("resolve_article", payload);
        @SuppressWarnings("unchecked")
        Map<String, Object> safe = (Map<String, Object>) sanitized;
        assertTrue(((String) safe.get("title")).contains("[REDACTED_EMAIL]"));
        assertFalse(((String) safe.get("title")).contains("alice@example.com"));
    }

    @Test
    void sanitize_nullResult_returnsNull() {
        assertNull(ToolCallTraceSanitizer.sanitizeResultData("any_tool", null));
    }

    @Test
    void summarize_failedTool_includesErrorMessage() {
        String summary = ToolCallTraceSanitizer.summarize(
                "resolve_article", false, null, "Article not found: kb-missing");
        assertNotNull(summary);
        assertTrue(summary.startsWith("error:"));
        assertTrue(summary.contains("Article not found: kb-missing"));
    }

    @Test
    void summarize_searchKnowledge_includesHitCountAndFaqMissFlag() {
        Map<String, Object> hits = Map.of(
                "faq_miss", true,
                "hits", List.of(Map.of("source_id", "kb-1")));
        String summary = ToolCallTraceSanitizer.summarize("search_knowledge", true, hits, null);
        assertTrue(summary.contains("1 hit"));
        assertTrue(summary.contains("faq_miss"));
    }

    @Test
    void summarize_recordOutcome_includesOutcomeClass() {
        Map<String, Object> data = Map.of("outcome_class", "resolve", "outcome", "RESOLVED");
        String summary = ToolCallTraceSanitizer.summarize("record_outcome", true, data, null);
        assertEquals("outcome=resolve", summary);
    }

    @Test
    void summarize_requestHandover_includesReason() {
        Map<String, Object> data = Map.of(
                "escalation_reason", "user_requested", "transfer_result", "queued");
        String summary = ToolCallTraceSanitizer.summarize("request_handover", true, data, null);
        assertTrue(summary.contains("user_requested"));
        assertTrue(summary.contains("queued"));
    }

    // ── Sprint 9.1 — sanitization closure fix ──────────────────────────

    @Test
    void sanitizeErrorMessage_redactsEmailPhoneAndToken() {
        String raw = "Salesforce 503: failed for alice@example.com phone +44 20 7946 0958 "
                + "auth Bearer abcdef0123456789ABCDEFGHabcdef01";
        String safe = ToolCallTraceSanitizer.sanitizeErrorMessage(raw);
        assertNotNull(safe);
        assertFalse(safe.contains("alice@example.com"),
                "email must be redacted in error_message (was: " + safe + ")");
        assertFalse(safe.contains("20 7946 0958"),
                "phone digits must be redacted in error_message (was: " + safe + ")");
        assertFalse(safe.contains("abcdef0123456789ABCDEFGHabcdef01"),
                "long bearer token must be redacted in error_message (was: " + safe + ")");
        assertTrue(safe.contains("[REDACTED_EMAIL]"));
        assertTrue(safe.contains("[REDACTED_PHONE]"));
        // The Bearer header should hit the BEARER pattern, not LONG_TOKEN.
        assertTrue(safe.contains("[REDACTED_BEARER]"),
                "Bearer pattern must surface as a bearer-specific marker (was: " + safe + ")");
    }

    @Test
    void summarize_failedTool_redactsErrorContents() {
        String raw = "downstream rejected token=ZjY1OWY3MGVjN2E4NDI2OTk5OWY1MDE5OWM2NjY2ZGY "
                + "for user alice@example.com";
        String summary = ToolCallTraceSanitizer.summarize(
                "request_handover", false, null, raw);
        assertTrue(summary.startsWith("error:"));
        assertFalse(summary.contains("alice@example.com"),
                "failed-tool result_summary must redact email (was: " + summary + ")");
        assertFalse(summary.contains("ZjY1OWY3MGVjN2E4NDI2OTk5OWY1MDE5OWM2NjY2ZGY"),
                "failed-tool result_summary must redact long random tokens (was: " + summary + ")");
        assertTrue(summary.contains("[REDACTED_EMAIL]"));
    }

    @Test
    void sanitizeResultData_redactsErrorFieldFromGenericMap() {
        // Some tool wraps its failure as { "error": "...", ... }. The
        // generic sanitizer must scrub the embedded sensitive surface
        // even though the entry isn't surfaced via error_message.
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("error",
                "salesforce 401: Authorization Bearer abcdef0123456789ABCDEFGHabcdef0123 "
                        + "rejected for user@example.com (postcode SW1A 1AA)");
        Object sanitized = ToolCallTraceSanitizer.sanitizeResultData("request_handover", payload);
        @SuppressWarnings("unchecked")
        Map<String, Object> safe = (Map<String, Object>) sanitized;
        String err = (String) safe.get("error");
        assertNotNull(err);
        assertFalse(err.contains("user@example.com"));
        assertFalse(err.contains("abcdef0123456789ABCDEFGHabcdef0123"));
        assertFalse(err.contains("SW1A 1AA"));
        assertTrue(err.contains("[REDACTED_EMAIL]"));
        assertTrue(err.contains("[REDACTED_BEARER]"));
        assertTrue(err.contains("[REDACTED_POSTCODE]"));
    }

    @Test
    void sanitizeResultData_redactsSensitiveKeyValuesEvenIfBenignContent() {
        // Sprint 9.1: the value under a sensitive key is replaced with
        // [REDACTED_SECRET] regardless of contents — even strings that
        // would not match any value pattern.
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("password", "hunter2");           // short, benign-looking
        payload.put("token", "shorttoken");           // too short for LONG_TOKEN
        payload.put("secret", "abc");                 // 3 chars
        payload.put("api_key", "k1");                 // tiny
        payload.put("authorization", "anything-here");
        // Casing / separator variations must hit the same rule.
        payload.put("Auth-Header", "x-y-z");
        payload.put("Refresh_Token", "q");
        // Benign keys must NOT be redacted by key alone.
        payload.put("source_id", "kb-1");

        Object sanitized = ToolCallTraceSanitizer.sanitizeResultData("anything", payload);
        @SuppressWarnings("unchecked")
        Map<String, Object> safe = (Map<String, Object>) sanitized;

        assertEquals("[REDACTED_SECRET]", safe.get("password"));
        assertEquals("[REDACTED_SECRET]", safe.get("token"));
        assertEquals("[REDACTED_SECRET]", safe.get("secret"));
        assertEquals("[REDACTED_SECRET]", safe.get("api_key"));
        assertEquals("[REDACTED_SECRET]", safe.get("authorization"));
        assertEquals("[REDACTED_SECRET]", safe.get("Auth-Header"));
        assertEquals("[REDACTED_SECRET]", safe.get("Refresh_Token"));
        // source_id remains as-is — benign key, benign value.
        assertEquals("kb-1", safe.get("source_id"));
    }

    @Test
    void sanitizeResultData_redactsSensitiveValuesUnderBenignKeys() {
        // Sprint 9.1: values that LOOK sensitive get redacted even when
        // the surrounding key is innocuous (e.g. tool dumped a stray
        // Authorization header into a "note" field).
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("note", "raw header: Bearer abcdef0123456789ABCDEFGHabcdef01");
        payload.put("contact", "alice@example.com or 020 7946 0958");
        payload.put("next_step", "check postcode SW1A 1AA");
        payload.put("hash", "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef");
        payload.put("api_session_token_value", "sk_abcd12345678efgh");

        Object sanitized = ToolCallTraceSanitizer.sanitizeResultData("anything", payload);
        @SuppressWarnings("unchecked")
        Map<String, Object> safe = (Map<String, Object>) sanitized;

        assertTrue(((String) safe.get("note")).contains("[REDACTED_BEARER]"));
        assertFalse(((String) safe.get("note")).contains("abcdef0123456789ABCDEFGHabcdef01"));

        String contact = (String) safe.get("contact");
        assertTrue(contact.contains("[REDACTED_EMAIL]"));
        assertFalse(contact.contains("alice@example.com"));
        assertTrue(contact.contains("[REDACTED_PHONE]"));

        assertTrue(((String) safe.get("next_step")).contains("[REDACTED_POSTCODE]"));
        assertFalse(((String) safe.get("next_step")).contains("SW1A 1AA"));

        // 64-char hex must be marked as a long token even under a benign key.
        assertEquals("[REDACTED_TOKEN]", safe.get("hash"));

        // sk_-prefixed api keys hit the API_KEY_PREFIX pattern.
        assertTrue(((String) safe.get("api_session_token_value")).contains("[REDACTED_API_KEY]"));
    }

    @Test
    void resolveArticle_safeSummaryShape_isPreservedAfterRedactionRules() {
        // Regression: hardening must not reshape the resolve_article
        // summary — source_id / article_id / title / source_url /
        // excerpt are still the canonical surface fields.
        Map<String, Object> articlePayload = new LinkedHashMap<>();
        articlePayload.put("source_id", "kb-001");
        articlePayload.put("article_id", "kb-001");
        articlePayload.put("title", "Where is my advert?");
        articlePayload.put("source_url", "https://help.example/faq/where-is-my-ad");
        articlePayload.put("description", "Common reasons your ad isn't visible.");
        articlePayload.put("uc_tags", new String[]{"UC-A"});

        Object sanitized = ToolCallTraceSanitizer.sanitizeResultData(
                "resolve_article", articlePayload);
        @SuppressWarnings("unchecked")
        Map<String, Object> safe = (Map<String, Object>) sanitized;

        assertEquals("kb-001", safe.get("source_id"));
        assertEquals("kb-001", safe.get("article_id"));
        assertEquals("Where is my advert?", safe.get("title"));
        assertEquals("https://help.example/faq/where-is-my-ad", safe.get("source_url"));
        assertNotNull(safe.get("excerpt"));
        assertFalse(safe.containsKey("description"));
    }
}
