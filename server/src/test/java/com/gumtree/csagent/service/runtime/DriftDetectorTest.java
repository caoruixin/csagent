package com.gumtree.csagent.service.runtime;

import com.gumtree.csagent.model.BotSession;
import com.gumtree.csagent.model.DriftResult;
import com.gumtree.csagent.model.DriftResult.DriftType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for DriftDetector.
 * Validates escalation detection, hard shift keywords, and edge cases.
 */
class DriftDetectorTest {

    private DriftDetector detector;

    @BeforeEach
    void setUp() {
        detector = new DriftDetector();
    }

    // --- Escalation detection ---

    @ParameterizedTest
    @ValueSource(strings = {
            "I want to talk to a real person",
            "Talk to an agent",
            "transfer me",
            "speak with a human",
            "connect me with a live agent",
            "I need to speak to someone",
            "let me talk to customer service"
    })
    void detect_escalationPhrases_shouldReturnUserEscalationRequest(String message) {
        BotSession session = buildSession("UC-A");

        DriftResult result = detector.detect(session, message);

        assertEquals(DriftType.USER_ESCALATION_REQUEST, result.getType());
        assertTrue(result.isEscalationRequested());
        assertNull(result.getNewUseCase(), "Escalation should not change UC");
    }

    /**
     * WS-3 / D2 (2026-07-25) — negative control for the frustration posture.
     *
     * <p>These messages COMPLAIN ABOUT customer service; they do not ASK for a
     * human. Before D2 the escalation pattern ended with the bare noun phrases
     * {@code customer service} / {@code live support}, so every one of these
     * matched and {@code ControlKernel.processMessage} step 4 handed the
     * session straight to a human. That was the only executable implementation
     * of the BRD's "customer indicates frustration => escalate" rule, and it
     * fired on tone rather than on a request.
     *
     * <p>Post-D2 frustration reaches the LLM as the advisory
     * {@code user_sentiment_signal} projection slot instead; the LLM decides
     * whether the underlying ask is still solvable. See
     * {@code phase0_normative_freeze.md} §0.6 deviation 2026-07-25.
     */
    @ParameterizedTest
    @ValueSource(strings = {
            "your customer service is useless",
            "this is the worst customer service I have ever had",
            "honestly the customer service here is a joke",
            "I have been waiting days, terrible live support"
    })
    void detect_frustrationAboutSupport_isNotAnEscalationRequest(String message) {
        BotSession session = buildSession("UC-A");

        DriftResult result = detector.detect(session, message);

        assertEquals(DriftType.NONE, result.getType(),
                "Complaining about customer service is frustration, not a request for a "
                        + "human; D2 forbids treating it as a deterministic handover trigger");
        assertFalse(result.isEscalationRequested());
    }

    /** Genuine requests that name the same nouns MUST still escalate. */
    @ParameterizedTest
    @ValueSource(strings = {
            "please connect me to customer service",
            "put me through to live support",
            "I want to speak to customer service",
            "get me customer service now"
    })
    void detect_requestsNamingCustomerService_stillEscalate(String message) {
        BotSession session = buildSession("UC-A");

        DriftResult result = detector.detect(session, message);

        assertEquals(DriftType.USER_ESCALATION_REQUEST, result.getType());
        assertTrue(result.isEscalationRequested());
    }

    // --- Hard shift detection ---

    @Test
    void detect_scamKeyword_whenDifferentUc_shouldReturnHardShiftToUCJ() {
        BotSession session = buildSession("UC-A");

        DriftResult result = detector.detect(session, "I think I got scammed by this seller");

        assertEquals(DriftType.HARD_SHIFT, result.getType());
        assertEquals("UC-J", result.getNewUseCase());
    }

    @Test
    void detect_scamKeyword_whenAlreadyUCJ_shouldReturnNone() {
        BotSession session = buildSession("UC-J");

        DriftResult result = detector.detect(session, "I was scammed");

        assertEquals(DriftType.NONE, result.getType());
    }

    @Test
    void detect_deleteMyDataKeyword_shouldReturnHardShiftToUCG() {
        BotSession session = buildSession("UC-A");

        DriftResult result = detector.detect(session, "I want to delete my data");

        assertEquals(DriftType.HARD_SHIFT, result.getType());
        assertEquals("UC-G", result.getNewUseCase());
    }

    @Test
    void detect_refundKeyword_shouldReturnHardShiftToUCI() {
        BotSession session = buildSession("UC-A");

        DriftResult result = detector.detect(session, "I want a refund for this");

        assertEquals(DriftType.HARD_SHIFT, result.getType());
        assertEquals("UC-I", result.getNewUseCase());
    }

    @Test
    void detect_adRemovedKeyword_shouldReturnHardShiftToUCH() {
        BotSession session = buildSession("UC-A");

        DriftResult result = detector.detect(session, "why was my ad removed");

        assertEquals(DriftType.HARD_SHIFT, result.getType());
        assertEquals("UC-H", result.getNewUseCase());
    }

    @Test
    void detect_unsafeKeyword_shouldReturnHardShiftToUCJ() {
        BotSession session = buildSession("UC-A");

        DriftResult result = detector.detect(session, "this person is being threatening");

        assertEquals(DriftType.HARD_SHIFT, result.getType());
        assertEquals("UC-J", result.getNewUseCase());
    }

    // --- No drift scenarios ---

    @Test
    void detect_normalMessage_shouldReturnNone() {
        BotSession session = buildSession("UC-A");

        DriftResult result = detector.detect(session, "my ad is not showing up");

        assertEquals(DriftType.NONE, result.getType());
    }

    @Test
    void detect_nullMessage_shouldReturnNone() {
        BotSession session = buildSession("UC-A");

        DriftResult result = detector.detect(session, null);

        assertEquals(DriftType.NONE, result.getType());
    }

    @Test
    void detect_emptyMessage_shouldReturnNone() {
        BotSession session = buildSession("UC-A");

        DriftResult result = detector.detect(session, "");

        assertEquals(DriftType.NONE, result.getType());
    }

    @Test
    void detect_blankMessage_shouldReturnNone() {
        BotSession session = buildSession("UC-A");

        DriftResult result = detector.detect(session, "   ");

        assertEquals(DriftType.NONE, result.getType());
    }

    @Test
    void detect_escalationHasPriorityOverHardShift() {
        // "talk to an agent" should be detected as escalation even with hard-shift keywords
        BotSession session = buildSession("UC-A");

        DriftResult result = detector.detect(session, "talk to an agent about this scam");

        // Escalation check runs first
        assertEquals(DriftType.USER_ESCALATION_REQUEST, result.getType());
    }

    private BotSession buildSession(String activeUseCase) {
        return BotSession.builder()
                .sessionId("test-session-1")
                .activeUseCase(activeUseCase)
                .build();
    }
}
