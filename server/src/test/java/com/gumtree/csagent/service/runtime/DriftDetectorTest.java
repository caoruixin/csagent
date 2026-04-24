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
