package com.gumtree.csagent.service.tools;

import com.gumtree.csagent.model.BotSession;
import com.gumtree.csagent.service.SalesforceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Regression tests for CreateCaseControlledTool queue mapping.
 * Catches the D11+D12 finding where UC-J (Trust & Safety) routes to
 * Commercial_Queue instead of Safety_Queue, and UC-H (Ad Appeal) routes
 * to Safety_Queue instead of Ad_Support_Queue.
 *
 * EXPECTED FAILURES: These tests document the current incorrect mapping
 * and will pass once the queue mapping is corrected.
 */
@ExtendWith(MockitoExtension.class)
class CreateCaseControlledToolQueueMappingTest {

    private CreateCaseControlledTool tool;

    @Mock
    private SalesforceService salesforceService;

    @BeforeEach
    void setUp() {
        tool = new CreateCaseControlledTool(salesforceService);
    }

    /**
     * UC-J is "Trust & Safety Report" (scam, fraud, harassment).
     * It should route to a Safety/Trust queue, NOT Commercial_Queue.
     *
     * Current behavior: UC-J -> Commercial_Queue (INCORRECT)
     * Expected behavior: UC-J -> Safety_Queue or Trust_Safety_Queue
     */
    @Test
    void execute_ucJ_queueName_shouldNotBeCommercialQueue() {
        BotSession session = buildSession("UC-J");
        Map<String, Object> params = Map.of(
                "subject", "Scam report",
                "description", "User was scammed out of money");

        when(salesforceService.createCase(anyString(), eq("UC-J"), anyString(),
                anyString(), any(), any(), anyString(), any()))
                .thenReturn("CASE-J-001");

        tool.execute(session, params);

        ArgumentCaptor<String> queueCaptor = ArgumentCaptor.forClass(String.class);
        verify(salesforceService).createCase(anyString(), eq("UC-J"), anyString(),
                anyString(), any(), any(), queueCaptor.capture(), any());

        String actualQueue = queueCaptor.getValue();
        // UC-J is Trust & Safety -- Commercial_Queue is semantically wrong
        assertNotEquals("Commercial_Queue", actualQueue,
                "UC-J (Trust & Safety Report) should NOT route to Commercial_Queue. " +
                "Expected a safety-related queue name.");
    }

    /**
     * UC-H is "Ad Removal Appeal" (ad moderation disputes).
     * Routing to Safety_Queue is debatable but may cause confusion
     * with UC-J safety reports in the same queue.
     */
    @Test
    void execute_ucH_queueName_shouldBeAdSupportRelated() {
        BotSession session = buildSession("UC-H");
        Map<String, Object> params = Map.of(
                "description", "My ad was removed unfairly");

        when(salesforceService.createCase(anyString(), eq("UC-H"), anyString(),
                anyString(), any(), any(), anyString(), any()))
                .thenReturn("CASE-H-001");

        tool.execute(session, params);

        ArgumentCaptor<String> queueCaptor = ArgumentCaptor.forClass(String.class);
        verify(salesforceService).createCase(anyString(), eq("UC-H"), anyString(),
                anyString(), any(), any(), queueCaptor.capture(), any());

        String actualQueue = queueCaptor.getValue();
        // Document the current value for visibility
        assertEquals("Safety_Queue", actualQueue,
                "UC-H currently maps to Safety_Queue -- verify this is the intended queue " +
                "for ad moderation appeals (not to be confused with UC-J safety reports)");
    }

    private BotSession buildSession(String activeUseCase) {
        return BotSession.builder()
                .sessionId("test-session-queue-mapping")
                .activeUseCase(activeUseCase)
                .handlingState("BOT_HANDLING")
                .currentPhase("RESOLVE")
                .totalBotTurns(1)
                .clarificationCount(0)
                .faqMissCount(0)
                .repeatedActionCount(0)
                .build();
    }
}
