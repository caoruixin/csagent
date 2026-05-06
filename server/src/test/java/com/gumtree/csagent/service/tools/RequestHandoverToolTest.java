package com.gumtree.csagent.service.tools;

import com.gumtree.csagent.model.BotSession;
import com.gumtree.csagent.service.SalesforceService;
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
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Sprint 9 §O0 — terminal tool contract alignment for {@code request_handover}.
 *
 * <p>The projected schema now exposes {@code summary} as a recommended
 * (non-required) field. The tool derives a safe fallback when missing
 * so e.g. {@code request_handover(escalation_reason=tool_scope_blocked)}
 * does not fail solely because the LLM omitted {@code summary}. A
 * supplied summary is always preserved verbatim; an invalid (missing /
 * blank) {@code escalation_reason} still fails.
 */
@ExtendWith(MockitoExtension.class)
class RequestHandoverToolTest {

    @Mock private SalesforceService salesforceService;

    private RequestHandoverTool tool;

    @BeforeEach
    void setUp() {
        tool = new RequestHandoverTool(salesforceService);
    }

    @Test
    void execute_withReasonOnly_derivesSafeFallbackSummaryAndSucceeds() {
        when(salesforceService.requestHandover(anyString(), anyMap())).thenReturn("queued");

        ToolResult result = tool.execute(buildSession(),
                Map.of("escalation_reason", "tool_scope_blocked"));

        assertTrue(result.isSuccess(),
                "Sprint 9 §O0: request_handover(tool_scope_blocked) must NOT fail solely "
                        + "because summary is missing; the tool derives a safe fallback.");

        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) result.getData();
        Object summary = data.get("summary");
        assertNotNull(summary, "result_data must include the derived fallback summary");
        assertTrue(((String) summary).length() > 0,
                "derived fallback summary must not be blank");
        assertEquals("fallback", data.get("summary_source"),
                "result data should mark the summary as runtime-derived");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> payloadCaptor = ArgumentCaptor.forClass(Map.class);
        verify(salesforceService, times(1)).requestHandover(eq("sess-handover-test"), payloadCaptor.capture());
        Map<String, Object> payload = payloadCaptor.getValue();
        assertNotNull(payload.get("summary"),
                "Salesforce handover payload must include the fallback summary");
        assertEquals("tool_scope_blocked", payload.get("escalation_reason"));
    }

    @Test
    void execute_withProvidedSummary_preservesItVerbatim() {
        when(salesforceService.requestHandover(anyString(), anyMap())).thenReturn("queued");

        Map<String, Object> args = new LinkedHashMap<>();
        args.put("escalation_reason", "user_requested");
        args.put("summary", "User explicitly asked for a human agent for help with payments.");

        ToolResult result = tool.execute(buildSession(), args);
        assertTrue(result.isSuccess());

        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) result.getData();
        assertEquals("User explicitly asked for a human agent for help with payments.",
                data.get("summary"),
                "supplied summary must be preserved verbatim — not rewritten by fallback derivation");
        assertFalse(data.containsKey("summary_source"),
                "summary_source marker must NOT be present when the LLM supplied summary itself");
    }

    @Test
    void execute_missingEscalationReason_stillFails() {
        ToolResult result = tool.execute(buildSession(), Map.of());

        assertFalse(result.isSuccess(),
                "an invalid (missing) escalation_reason must still fail — the contract on "
                        + "the canonical reason field is unchanged.");
        assertTrue(result.getErrorMessage().contains("escalation_reason"));
        verify(salesforceService, never()).requestHandover(anyString(), anyMap());
    }

    @Test
    void execute_blankEscalationReason_stillFails() {
        ToolResult result = tool.execute(buildSession(),
                Map.of("escalation_reason", "   "));

        assertFalse(result.isSuccess());
        verify(salesforceService, never()).requestHandover(anyString(), anyMap());
    }

    @Test
    void deriveFallbackSummary_combinesUcTopicAndReason() {
        BotSession s = buildSession();
        s.setFormTopicSubject("Ad Support");

        String summary = RequestHandoverTool.deriveFallbackSummary(
                s,
                Map.of("current_user_message", "could you give me the link of advert?"),
                "tool_scope_blocked");

        assertNotNull(summary);
        assertTrue(summary.contains("UC-A"),
                "fallback summary must include the active UC when present");
        assertTrue(summary.contains("Ad Support"),
                "fallback summary must include the form topic when present");
        assertTrue(summary.contains("tool_scope_blocked"),
                "fallback summary must include the canonical escalation_reason");
        assertTrue(summary.contains("could you give me the link of advert?"),
                "fallback summary should surface the current user message");
    }

    private static BotSession buildSession() {
        BotSession session = new BotSession();
        session.setSessionId("sess-handover-test");
        session.setActiveUseCase("UC-A");
        session.setTotalBotTurns(3);
        return session;
    }
}
