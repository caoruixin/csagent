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
 * size cap and tool-aware {@code resolve_article} summary fields so the
 * {@code bot_turns.tool_calls} trace stays non-empty without leaking large
 * payloads. Demo policy: diagnostic strings (guardrail predicates, KB
 * source ids) pass through verbatim; only credential-like map keys are
 * redacted.
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
    void sanitizeResultData_preservesSalesforceSourceIdInSearchHits() {
        String sourceId = "ka4P2000000060bIAA";
        Map<String, Object> hit = Map.of(
                "source_id", sourceId,
                "title", "I Can't Find My Ad",
                "score", 2.5);
        Map<String, Object> result = Map.of("hits", List.of(hit), "faq_miss", true);

        Object sanitized = ToolCallTraceSanitizer.sanitizeResultData("search_knowledge", result);
        @SuppressWarnings("unchecked")
        Map<String, Object> safe = (Map<String, Object>) sanitized;
        @SuppressWarnings("unchecked")
        Map<String, Object> firstHit = (Map<String, Object>) ((List<?>) safe.get("hits")).get(0);
        assertEquals(sourceId, firstHit.get("source_id"));
    }

    @Test
    void sanitize_preservesEmailInStringFields() {
        Map<String, Object> payload = Map.of(
                "title", "Contact alice@example.com for details",
                "source_id", "kb-pii");

        Object sanitized = ToolCallTraceSanitizer.sanitizeResultData("resolve_article", payload);
        @SuppressWarnings("unchecked")
        Map<String, Object> safe = (Map<String, Object>) sanitized;
        assertTrue(((String) safe.get("title")).contains("alice@example.com"));
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

    @Test
    void sanitizeErrorMessage_preservesGuardrailPredicate() {
        String predicate = "progressive_resolve_record_outcome_premature";
        assertEquals(predicate, ToolCallTraceSanitizer.sanitizeErrorMessage(predicate));
    }

    @Test
    void summarize_failedTool_passesErrorVerbatim() {
        String raw = "downstream rejected token=ZjY1OWY3MGVjN2E4NDI2OTk5OWY1MDE5OWM2NjY2ZGY "
                + "for user alice@example.com";
        String summary = ToolCallTraceSanitizer.summarize(
                "request_handover", false, null, raw);
        assertTrue(summary.startsWith("error:"));
        assertTrue(summary.contains("alice@example.com"),
                "failed-tool result_summary must pass through diagnostic text (was: " + summary + ")");
        assertTrue(summary.contains("ZjY1OWY3MGVjN2E4NDI2OTk5OWY1MDE5OWM2NjY2ZGY"));
    }

    @Test
    void sanitizeResultData_preservesErrorFieldFromGenericMap() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("error",
                "salesforce 401: Authorization Bearer abcdef0123456789ABCDEFGHabcdef0123 "
                        + "rejected for user@example.com (postcode SW1A 1AA)");

        Object sanitized = ToolCallTraceSanitizer.sanitizeResultData("request_handover", payload);
        @SuppressWarnings("unchecked")
        Map<String, Object> safe = (Map<String, Object>) sanitized;
        String err = (String) safe.get("error");
        assertNotNull(err);
        assertTrue(err.contains("user@example.com"));
        assertTrue(err.contains("SW1A 1AA"));
        assertTrue(err.contains("Bearer abcdef0123456789ABCDEFGHabcdef0123"));
    }

    @Test
    void sanitizeResultData_redactsSensitiveKeyValuesEvenIfBenignContent() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("password", "hunter2");
        payload.put("token", "shorttoken");
        payload.put("secret", "abc");
        payload.put("api_key", "k1");
        payload.put("authorization", "anything-here");
        payload.put("Auth-Header", "x-y-z");
        payload.put("Refresh_Token", "q");
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
        assertEquals("kb-1", safe.get("source_id"));
    }

    @Test
    void resolveArticle_safeSummaryShape_isPreservedAfterRedactionRules() {
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
