package com.gumtree.csagent.service.guardrails;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ForbiddenPhraseDetector.
 * Validates all forbidden phrase categories, sanitization, and edge cases.
 */
class ForbiddenPhraseDetectorTest {

    private ForbiddenPhraseDetector detector;

    @BeforeEach
    void setUp() {
        detector = new ForbiddenPhraseDetector();
    }

    // --- Detection tests ---

    @Test
    void check_identityImpersonation_shouldDetect() {
        List<ForbiddenPhraseMatch> matches = detector.check("Hello, my name is John and I will help you.");

        assertFalse(matches.isEmpty());
        assertEquals("identity_impersonation", matches.get(0).getCategory());
    }

    @Test
    void check_falseAction_shouldDetect() {
        List<ForbiddenPhraseMatch> matches = detector.check("I've fixed your ad and it should be visible now.");

        assertFalse(matches.isEmpty());
        assertEquals("false_action", matches.get(0).getCategory());
    }

    @Test
    void check_falsePromise_shouldDetect() {
        List<ForbiddenPhraseMatch> matches = detector.check("I'll process your refund right away.");

        assertFalse(matches.isEmpty());
        assertEquals("false_promise", matches.get(0).getCategory());
    }

    @Test
    void check_fakeEmpathy_shouldDetect() {
        List<ForbiddenPhraseMatch> matches = detector.check("I understand how you feel about this.");

        assertFalse(matches.isEmpty());
        assertEquals("fake_empathy", matches.get(0).getCategory());
    }

    @Test
    void check_aiDisclosure_shouldDetect() {
        List<ForbiddenPhraseMatch> matches = detector.check("As an AI language model, I cannot help.");

        assertFalse(matches.isEmpty());
        assertEquals("ai_disclosure", matches.get(0).getCategory());
    }

    @Test
    void check_dataPromise_shouldDetect() {
        List<ForbiddenPhraseMatch> matches = detector.check("I have deleted your data as requested.");

        assertFalse(matches.isEmpty());
        assertEquals("data_promise", matches.get(0).getCategory());
    }

    @Test
    void check_authorityClaim_shouldDetect() {
        List<ForbiddenPhraseMatch> matches = detector.check("I'm a manager and I can override this.");

        assertFalse(matches.isEmpty());
        assertEquals("authority_claim", matches.get(0).getCategory());
    }

    @Test
    void check_legalAdvice_shouldDetect() {
        List<ForbiddenPhraseMatch> matches = detector.check("You should take legal action against them.");

        assertFalse(matches.isEmpty());
        assertEquals("legal_advice", matches.get(0).getCategory());
    }

    @Test
    void check_cleanMessage_shouldReturnEmpty() {
        List<ForbiddenPhraseMatch> matches = detector.check("I'm the Gumtree Support Assistant. How can I help?");

        assertTrue(matches.isEmpty());
    }

    @Test
    void check_nullInput_shouldReturnEmpty() {
        List<ForbiddenPhraseMatch> matches = detector.check(null);

        assertTrue(matches.isEmpty());
    }

    @Test
    void check_emptyInput_shouldReturnEmpty() {
        List<ForbiddenPhraseMatch> matches = detector.check("");

        assertTrue(matches.isEmpty());
    }

    // --- hasForbiddenPhrases tests ---

    @Test
    void hasForbiddenPhrases_withForbidden_shouldReturnTrue() {
        assertTrue(detector.hasForbiddenPhrases("I've removed the issue."));
    }

    @Test
    void hasForbiddenPhrases_withClean_shouldReturnFalse() {
        assertFalse(detector.hasForbiddenPhrases("Let me help you with that."));
    }

    @Test
    void hasForbiddenPhrases_null_shouldReturnFalse() {
        assertFalse(detector.hasForbiddenPhrases(null));
    }

    // --- Sanitization tests ---

    @Test
    void sanitize_identityImpersonation_shouldReplace() {
        String result = detector.sanitize("Hello, my name is John, how can I help?");

        assertTrue(result.contains("Gumtree Support Assistant"));
        assertFalse(result.contains("Hello, my name is"));
    }

    @Test
    void sanitize_falseAction_shouldReplace() {
        String result = detector.sanitize("I've fixed the problem.");

        assertTrue(result.contains("specialist team"));
    }

    @Test
    void sanitize_cleanMessage_shouldReturnUnchanged() {
        String input = "Here is the information you requested.";
        String result = detector.sanitize(input);

        assertEquals(input, result);
    }

    @Test
    void sanitize_null_shouldReturnNull() {
        assertNull(detector.sanitize(null));
    }

    @Test
    void sanitize_empty_shouldReturnEmpty() {
        assertEquals("", detector.sanitize(""));
    }
}
