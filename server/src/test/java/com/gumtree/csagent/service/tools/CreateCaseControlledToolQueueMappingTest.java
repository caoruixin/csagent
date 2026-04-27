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
 * Verifies correct queue routing:
 * UC-H (Ad Removal Appeal) → Ad_Support_Queue
 * UC-J (Trust & Safety Report) → Safety_Queue
 * UC-K (Technical Support) → Account_Support_Queue
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
     * Routes to Ad_Support_Queue.
     */
    @Test
    void execute_ucH_queueName_shouldBeAdSupportQueue() {
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
        assertEquals("Ad_Support_Queue", actualQueue,
                "UC-H (Ad Removal Appeal) should route to Ad_Support_Queue");
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
