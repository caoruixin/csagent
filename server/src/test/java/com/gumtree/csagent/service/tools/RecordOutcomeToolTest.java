package com.gumtree.csagent.service.tools;

import com.gumtree.csagent.model.BotSession;
import com.gumtree.csagent.model.SessionOutcome;
import com.gumtree.csagent.repository.SessionOutcomeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Sprint 9 §O0 — terminal tool contract alignment for {@code record_outcome}.
 *
 * <p>The projected schema advertises {@code outcome_class} with lowercase
 * enum values {@code resolve | escalate | abandon}. Legacy callers /
 * tests still pass {@code outcome=RESOLVED}; both forms must work and
 * normalise to the canonical persisted outcome enum.
 */
@ExtendWith(MockitoExtension.class)
class RecordOutcomeToolTest {

    @Mock private SessionOutcomeRepository sessionOutcomeRepository;

    private RecordOutcomeTool tool;

    @BeforeEach
    void setUp() {
        tool = new RecordOutcomeTool(sessionOutcomeRepository);
    }

    @Test
    void getName_returnsRecordOutcome() {
        assertEquals("record_outcome", tool.getName());
    }

    @Test
    void execute_withCanonicalOutcomeClassResolve_persistsRow() {
        ToolResult result = tool.execute(buildSession(), Map.of("outcome_class", "resolve"));

        assertTrue(result.isSuccess(), "outcome_class=resolve must be accepted");
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) result.getData();
        assertEquals("resolve", data.get("outcome_class"),
                "result data should expose the normalized canonical outcome_class");
        assertEquals("RESOLVED", data.get("outcome"),
                "result data should also expose the persisted uppercase form for legacy readers");

        ArgumentCaptor<SessionOutcome> captor = ArgumentCaptor.forClass(SessionOutcome.class);
        verify(sessionOutcomeRepository, times(1)).save(captor.capture());
        SessionOutcome saved = captor.getValue();
        assertEquals("RESOLVED", saved.getOutcome(),
                "session_outcomes.outcome must persist in the canonical uppercase form");
        assertEquals("UC-A", saved.getUseCaseId());
    }

    @Test
    void execute_withLegacyOutcomeUppercaseResolved_persistsRow() {
        ToolResult result = tool.execute(buildSession(), Map.of("outcome", "RESOLVED"));

        assertTrue(result.isSuccess(),
                "legacy outcome=RESOLVED form must continue to be accepted");
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) result.getData();
        assertEquals("resolve", data.get("outcome_class"),
                "legacy uppercase outcome must normalize back to lowercase outcome_class");
        verify(sessionOutcomeRepository, times(1)).save(org.mockito.ArgumentMatchers.any(SessionOutcome.class));
    }

    @Test
    void execute_lowercaseAndUppercaseValuesNormalizeIdentically() {
        ToolResult lower = tool.execute(buildSession(), Map.of("outcome_class", "abandon"));
        ToolResult upper = tool.execute(buildSession(), Map.of("outcome_class", "ABANDONED"));

        assertTrue(lower.isSuccess());
        assertTrue(upper.isSuccess());
        @SuppressWarnings("unchecked")
        Map<String, Object> ld = (Map<String, Object>) lower.getData();
        @SuppressWarnings("unchecked")
        Map<String, Object> ud = (Map<String, Object>) upper.getData();
        assertEquals("abandon", ld.get("outcome_class"));
        assertEquals("abandon", ud.get("outcome_class"));
        assertEquals("ABANDONED", ld.get("outcome"));
        assertEquals("ABANDONED", ud.get("outcome"));
    }

    @Test
    void execute_missingParameter_errorNamesOutcomeClass() {
        ToolResult result = tool.execute(buildSession(), Map.of());

        assertFalse(result.isSuccess());
        String err = result.getErrorMessage();
        assertNotNull(err);
        assertTrue(err.contains("outcome_class"),
                "missing-parameter error must name the canonical 'outcome_class' (was: " + err + ")");
        verify(sessionOutcomeRepository, never()).save(org.mockito.ArgumentMatchers.any(SessionOutcome.class));
    }

    @Test
    void execute_unknownOutcomeClass_returnsError() {
        ToolResult result = tool.execute(buildSession(), Map.of("outcome_class", "frobnicate"));

        assertFalse(result.isSuccess());
        assertNotNull(result.getErrorMessage());
        verify(sessionOutcomeRepository, never()).save(org.mockito.ArgumentMatchers.any(SessionOutcome.class));
    }

    @Test
    void execute_escalateMissingEscalationReason_returnsError() {
        ToolResult result = tool.execute(buildSession(), Map.of("outcome_class", "escalate"));

        assertFalse(result.isSuccess());
        assertTrue(result.getErrorMessage().contains("escalation_reason"));
        verify(sessionOutcomeRepository, never()).save(org.mockito.ArgumentMatchers.any(SessionOutcome.class));
    }

    @Test
    void execute_escalateWithReason_persistsEscalatedOutcomeAndReason() {
        Map<String, Object> args = new LinkedHashMap<>();
        args.put("outcome_class", "escalate");
        args.put("escalation_reason", "user_requested");

        ToolResult result = tool.execute(buildSession(), args);
        assertTrue(result.isSuccess());

        ArgumentCaptor<SessionOutcome> captor = ArgumentCaptor.forClass(SessionOutcome.class);
        verify(sessionOutcomeRepository, times(1)).save(captor.capture());
        SessionOutcome saved = captor.getValue();
        assertEquals("ESCALATED", saved.getOutcome());
        assertEquals("user_requested", saved.getEscalationReason());
    }

    private static BotSession buildSession() {
        BotSession session = new BotSession();
        session.setSessionId("sess-record-outcome-test");
        session.setActiveUseCase("UC-A");
        session.setTotalBotTurns(3);
        return session;
    }
}
