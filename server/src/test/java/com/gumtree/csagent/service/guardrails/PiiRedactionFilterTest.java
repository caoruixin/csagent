package com.gumtree.csagent.service.guardrails;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for PiiRedactionFilter.
 * Validates all PII types are redacted correctly across different layers.
 */
class PiiRedactionFilterTest {

    private PiiRedactionFilter filter;

    @BeforeEach
    void setUp() {
        filter = new PiiRedactionFilter();
    }

    // --- Full redaction ---

    @Test
    void redact_emailAddress_shouldRedact() {
        String result = filter.redact("Contact me at john@example.com please.");

        assertTrue(result.contains("[REDACTED_EMAIL]"));
        assertFalse(result.contains("john@example.com"));
    }

    @Test
    void redact_ukPhone_shouldRedact() {
        String result = filter.redact("Call me at 07911 123456.");

        assertTrue(result.contains("[REDACTED_PHONE]"));
    }

    @Test
    void redact_ukPostcode_shouldRedact() {
        String result = filter.redact("I live near SW1A 1AA.");

        assertTrue(result.contains("[REDACTED_POSTCODE]"));
    }

    @Test
    void redact_adId_shouldRedact() {
        String result = filter.redact("My ad ID is 1234567890.");

        assertTrue(result.contains("[REDACTED_AD_ID]"));
    }

    @Test
    void redact_multiplePiiTypes_shouldRedactAll() {
        String result = filter.redact(
                "Email: user@test.com, phone: 07911 123456, postcode: EC2A 4NE, ad: 1234567890");

        assertTrue(result.contains("[REDACTED_EMAIL]"));
        assertTrue(result.contains("[REDACTED_PHONE]"));
        assertTrue(result.contains("[REDACTED_POSTCODE]"));
        assertTrue(result.contains("[REDACTED_AD_ID]"));
    }

    @Test
    void redact_null_shouldReturnNull() {
        assertNull(filter.redact(null));
    }

    @Test
    void redact_empty_shouldReturnEmpty() {
        assertEquals("", filter.redact(""));
    }

    @Test
    void redact_noPii_shouldReturnUnchanged() {
        String input = "My ad is not showing in search results.";
        assertEquals(input, filter.redact(input));
    }

    // --- Log-safe redaction ---

    @Test
    void redactForLogs_email_shouldRedact() {
        String result = filter.redactForLogs("Email is john@test.com");

        assertTrue(result.contains("[REDACTED_EMAIL]"));
    }

    @Test
    void redactForLogs_adId_shouldNotRedact() {
        String result = filter.redactForLogs("Ad ID is 1234567890");

        // Ad IDs are NOT redacted in logs
        assertTrue(result.contains("1234567890"),
                "Ad IDs should not be redacted in log-safe mode");
    }

    @Test
    void redactForLogs_null_shouldReturnNull() {
        assertNull(filter.redactForLogs(null));
    }

    // --- Projection-safe redaction ---

    @Test
    void redactForProjection_email_shouldRedact() {
        String result = filter.redactForProjection("Email is john@test.com");

        assertTrue(result.contains("[REDACTED_EMAIL]"));
    }

    @Test
    void redactForProjection_phone_shouldRedact() {
        String result = filter.redactForProjection("Phone is 07911 123456");

        assertTrue(result.contains("[REDACTED_PHONE]"));
    }

    @Test
    void redactForProjection_postcode_shouldNotRedact() {
        String result = filter.redactForProjection("Postcode is SW1A 1AA");

        // Postcodes are not redacted in projection mode
        assertFalse(result.contains("[REDACTED_POSTCODE]"),
                "Postcodes should not be redacted in projection mode");
    }

    @Test
    void redactForProjection_adId_shouldNotRedact() {
        String result = filter.redactForProjection("Ad ID is 1234567890");

        // Ad IDs are not redacted in projection mode
        assertTrue(result.contains("1234567890"),
                "Ad IDs should not be redacted in projection mode");
    }

    @Test
    void redactForProjection_null_shouldReturnNull() {
        assertNull(filter.redactForProjection(null));
    }
}
