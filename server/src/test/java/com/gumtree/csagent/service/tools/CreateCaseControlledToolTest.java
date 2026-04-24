package com.gumtree.csagent.service.tools;

import com.gumtree.csagent.model.BotSession;
import com.gumtree.csagent.service.SalesforceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for CreateCaseControlledTool (D11.3 - create_case_controlled Integration).
 * Verifies UC access control, required field validation, and case creation.
 */
@ExtendWith(MockitoExtension.class)
class CreateCaseControlledToolTest {

    private CreateCaseControlledTool tool;

    @Mock
    private SalesforceService salesforceService;

    @BeforeEach
    void setUp() {
        tool = new CreateCaseControlledTool(salesforceService);
    }

    @Test
    void getName_shouldReturnCreateCaseControlled() {
        assertEquals("create_case_controlled", tool.getName());
    }

    // --- UC access control ---

    @ParameterizedTest
    @ValueSource(strings = {"UC-H", "UC-J", "UC-K"})
    void execute_allowedUcs_shouldCreateCase(String ucId) {
        BotSession session = buildSession(ucId);
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("subject", "Test case");
        params.put("description", "Test description");
        params.put("email", "test@example.com");

        when(salesforceService.createCase(anyString(), eq(ucId), anyString(),
                anyString(), anyString(), any(), anyString(), any()))
                .thenReturn("CASE-12345");

        ToolResult result = tool.execute(session, params);

        assertTrue(result.isSuccess(), "Should succeed for allowed UC: " + ucId);
        assertNotNull(result.getData());
    }

    @ParameterizedTest
    @ValueSource(strings = {"UC-A", "UC-B", "UC-C", "UC-D", "UC-E", "UC-F", "UC-FP", "UC-G", "UC-I"})
    void execute_disallowedUcs_shouldReturnError(String ucId) {
        BotSession session = buildSession(ucId);
        Map<String, Object> params = Map.of("description", "Test");

        ToolResult result = tool.execute(session, params);

        assertFalse(result.isSuccess(), "Should fail for disallowed UC: " + ucId);
    }

    @Test
    void execute_nullActiveUc_shouldReturnError() {
        BotSession session = buildSession(null);
        Map<String, Object> params = Map.of("description", "Test");

        ToolResult result = tool.execute(session, params);

        assertFalse(result.isSuccess());
    }

    // --- D11.3: UC-I specifically excluded ---

    @Test
    void execute_ucI_shouldReturnError_noCaseCreated() {
        BotSession session = buildSession("UC-I");
        Map<String, Object> params = Map.of(
                "subject", "Refund request",
                "description", "I want my money back");

        ToolResult result = tool.execute(session, params);

        assertFalse(result.isSuccess(), "UC-I should NOT be allowed to create cases");
        verify(salesforceService, never()).createCase(any(), any(), any(), any(), any(), any(), any(), any());
    }

    // --- Required field validation ---

    @Test
    void execute_ucH_missingDescription_shouldReturnError() {
        BotSession session = buildSession("UC-H");
        Map<String, Object> params = Map.of("subject", "Appeal");

        ToolResult result = tool.execute(session, params);

        assertFalse(result.isSuccess(), "UC-H requires description");
    }

    @Test
    void execute_ucH_withDescription_shouldSucceed() {
        BotSession session = buildSession("UC-H");
        Map<String, Object> params = Map.of("description", "My ad was removed unfairly");

        when(salesforceService.createCase(anyString(), eq("UC-H"), anyString(),
                anyString(), any(), any(), eq("Safety_Queue"), any()))
                .thenReturn("CASE-H-001");

        ToolResult result = tool.execute(session, params);

        assertTrue(result.isSuccess());
    }

    @Test
    void execute_ucJ_missingSubject_shouldReturnError() {
        BotSession session = buildSession("UC-J");
        Map<String, Object> params = Map.of("description", "Scam report");
        // UC-J requires both subject AND description

        ToolResult result = tool.execute(session, params);

        assertFalse(result.isSuccess(), "UC-J requires subject");
    }

    @Test
    void execute_ucJ_withSubjectAndDescription_shouldSucceed() {
        BotSession session = buildSession("UC-J");
        Map<String, Object> params = Map.of(
                "subject", "Safety report",
                "description", "User threatened me");

        when(salesforceService.createCase(anyString(), eq("UC-J"), anyString(),
                anyString(), any(), any(), eq("Commercial_Queue"), any()))
                .thenReturn("CASE-J-001");

        ToolResult result = tool.execute(session, params);

        assertTrue(result.isSuccess());
    }

    @Test
    void execute_ucK_missingEmail_shouldReturnError() {
        BotSession session = buildSession("UC-K");
        Map<String, Object> params = Map.of("description", "Cannot log in");
        // UC-K requires both email AND description

        ToolResult result = tool.execute(session, params);

        assertFalse(result.isSuccess(), "UC-K requires email");
    }

    @Test
    void execute_ucK_withEmailAndDescription_shouldSucceed() {
        BotSession session = buildSession("UC-K");
        Map<String, Object> params = Map.of(
                "email", "user@example.com",
                "description", "Cannot access account");

        when(salesforceService.createCase(anyString(), eq("UC-K"), anyString(),
                anyString(), anyString(), any(), eq("Account_Support_Queue"), any()))
                .thenReturn("CASE-K-001");

        ToolResult result = tool.execute(session, params);

        assertTrue(result.isSuccess());
    }

    // --- Case ID in result ---

    @Test
    void execute_success_shouldReturnCaseIdInData() {
        BotSession session = buildSession("UC-H");
        Map<String, Object> params = Map.of("description", "Appeal my ad removal");

        when(salesforceService.createCase(anyString(), eq("UC-H"), anyString(),
                anyString(), any(), any(), anyString(), any()))
                .thenReturn("CASE-ABC123");

        ToolResult result = tool.execute(session, params);

        assertTrue(result.isSuccess());
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) result.getData();
        assertEquals("CASE-ABC123", data.get("case_id"));
        assertEquals("UC-H", data.get("use_case_id"));
        assertEquals("Safety_Queue", data.get("queue_name"));
        assertEquals("test-session-1", data.get("session_id"));
    }

    // --- Queue mapping ---

    @Test
    void execute_ucH_shouldUseSafetyQueue() {
        BotSession session = buildSession("UC-H");
        Map<String, Object> params = Map.of("description", "Test");

        when(salesforceService.createCase(anyString(), eq("UC-H"), anyString(),
                anyString(), any(), any(), eq("Safety_Queue"), any()))
                .thenReturn("CASE-001");

        tool.execute(session, params);

        verify(salesforceService).createCase(anyString(), eq("UC-H"), anyString(),
                anyString(), any(), any(), eq("Safety_Queue"), any());
    }

    @Test
    void execute_ucJ_shouldUseCommercialQueue() {
        BotSession session = buildSession("UC-J");
        Map<String, Object> params = Map.of("subject", "Safety report", "description", "Test");

        when(salesforceService.createCase(anyString(), eq("UC-J"), anyString(),
                anyString(), any(), any(), eq("Commercial_Queue"), any()))
                .thenReturn("CASE-001");

        tool.execute(session, params);

        verify(salesforceService).createCase(anyString(), eq("UC-J"), anyString(),
                anyString(), any(), any(), eq("Commercial_Queue"), any());
    }

    @Test
    void execute_ucK_shouldUseAccountSupportQueue() {
        BotSession session = buildSession("UC-K");
        Map<String, Object> params = Map.of("email", "user@test.com", "description", "Test");

        when(salesforceService.createCase(anyString(), eq("UC-K"), anyString(),
                anyString(), anyString(), any(), eq("Account_Support_Queue"), any()))
                .thenReturn("CASE-001");

        tool.execute(session, params);

        verify(salesforceService).createCase(anyString(), eq("UC-K"), anyString(),
                anyString(), anyString(), any(), eq("Account_Support_Queue"), any());
    }

    // --- Blank field validation ---

    @Test
    void execute_blankDescription_shouldReturnError() {
        BotSession session = buildSession("UC-H");
        Map<String, Object> params = Map.of("description", "   ");

        ToolResult result = tool.execute(session, params);

        assertFalse(result.isSuccess(), "Blank description should be treated as missing");
    }

    private BotSession buildSession(String activeUseCase) {
        return BotSession.builder()
                .sessionId("test-session-1")
                .activeUseCase(activeUseCase)
                .handlingState("BOT_HANDLING")
                .currentPhase("RESOLVE")
                .totalBotTurns(0)
                .clarificationCount(0)
                .faqMissCount(0)
                .repeatedActionCount(0)
                .build();
    }
}
