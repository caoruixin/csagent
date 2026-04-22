package com.gumtree.csagent.service.guardrails;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

/**
 * Four-layer PII redaction filter for protecting personally identifiable
 * information in logs, events, and projected context.
 *
 * Patterns:
 * - Email addresses
 * - UK phone numbers
 * - UK postcodes
 * - Ad IDs (10+ digit numbers, only in log context)
 */
@Slf4j
@Service
public class PiiRedactionFilter {

    // All patterns compiled as static constants for performance

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("[\\w.+\\-]+@[\\w\\-]+\\.[\\w.\\-]+");

    private static final Pattern UK_PHONE_PATTERN =
            Pattern.compile("(\\+?44|0)\\s*\\d[\\d\\s\\-]{8,12}");

    private static final Pattern UK_POSTCODE_PATTERN =
            Pattern.compile("(?i)[A-Z]{1,2}\\d[A-Z\\d]?\\s*\\d[A-Z]{2}");

    private static final Pattern AD_ID_PATTERN =
            Pattern.compile("\\b\\d{10,}\\b");

    private static final String REDACTED_EMAIL = "[REDACTED_EMAIL]";
    private static final String REDACTED_PHONE = "[REDACTED_PHONE]";
    private static final String REDACTED_POSTCODE = "[REDACTED_POSTCODE]";
    private static final String REDACTED_AD_ID = "[REDACTED_AD_ID]";

    /**
     * Full redaction: email + phone + postcode + ad_id.
     * Use for general-purpose redaction where all PII should be removed.
     *
     * @param text the input text
     * @return text with all PII types redacted
     */
    public String redact(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        String result = EMAIL_PATTERN.matcher(text).replaceAll(REDACTED_EMAIL);
        result = UK_PHONE_PATTERN.matcher(result).replaceAll(REDACTED_PHONE);
        result = UK_POSTCODE_PATTERN.matcher(result).replaceAll(REDACTED_POSTCODE);
        result = AD_ID_PATTERN.matcher(result).replaceAll(REDACTED_AD_ID);
        return result;
    }

    /**
     * Log-safe redaction: email + phone + postcode.
     * Ad IDs are NOT redacted because they are not PII,
     * but this method ensures no personal contact info leaks into logs.
     *
     * @param text the input text
     * @return text with email, phone, and postcode redacted
     */
    public String redactForLogs(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        String result = EMAIL_PATTERN.matcher(text).replaceAll(REDACTED_EMAIL);
        result = UK_PHONE_PATTERN.matcher(result).replaceAll(REDACTED_PHONE);
        result = UK_POSTCODE_PATTERN.matcher(result).replaceAll(REDACTED_POSTCODE);
        return result;
    }

    /**
     * Projection-safe redaction: email + phone only.
     * Keeps ad_id (needed for session context / tool calls) and postcode.
     * Used when projecting context to the LLM where ad_id is operationally required.
     *
     * @param text the input text
     * @return text with email and phone redacted
     */
    public String redactForProjection(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        String result = EMAIL_PATTERN.matcher(text).replaceAll(REDACTED_EMAIL);
        result = UK_PHONE_PATTERN.matcher(result).replaceAll(REDACTED_PHONE);
        return result;
    }
}
