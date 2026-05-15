package com.gumtree.csagent.service.runtime;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for FormContextIngestionService.sanitize() method.
 * Tests XSS prevention via HTML sanitization.
 */
class FormContextIngestionServiceTest {

    @Test
    void sanitize_normalText_shouldReturnUnchanged() {
        assertEquals("hello world", FormContextIngestionService.sanitize("hello world"));
    }

    @Test
    void sanitize_scriptTag_shouldStrip() {
        String result = FormContextIngestionService.sanitize("<script>alert(1)</script>");

        assertFalse(result.contains("<script>"));
        assertFalse(result.contains("</script>"));
    }

    @Test
    void sanitize_imgOnError_shouldStrip() {
        String result = FormContextIngestionService.sanitize("<img onerror=alert(1) src=x>");

        assertFalse(result.contains("<img"));
        assertFalse(result.contains("onerror"));
    }

    @Test
    void sanitize_htmlTags_shouldStrip() {
        String result = FormContextIngestionService.sanitize("<b>bold</b> <i>italic</i>");

        assertFalse(result.contains("<b>"));
        assertFalse(result.contains("<i>"));
        assertTrue(result.contains("bold"));
        assertTrue(result.contains("italic"));
    }

    @Test
    void sanitize_null_shouldReturnEmpty() {
        assertEquals("", FormContextIngestionService.sanitize(null));
    }

    @Test
    void sanitize_empty_shouldReturnEmpty() {
        assertEquals("", FormContextIngestionService.sanitize(""));
    }

    @Test
    void sanitize_iframeTag_shouldStrip() {
        String result = FormContextIngestionService.sanitize("<iframe src='evil.com'></iframe>");

        assertFalse(result.contains("<iframe"));
    }

    @Test
    void sanitize_eventHandler_shouldStrip() {
        String result = FormContextIngestionService.sanitize("<div onclick=\"alert(1)\">click</div>");

        assertFalse(result.contains("onclick"));
        assertTrue(result.contains("click"));
    }

    @Test
    void sanitize_whitespace_shouldTrim() {
        assertEquals("hello", FormContextIngestionService.sanitize("  hello  "));
    }
}
