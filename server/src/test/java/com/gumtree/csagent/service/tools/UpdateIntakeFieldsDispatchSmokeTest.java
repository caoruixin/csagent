package com.gumtree.csagent.service.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gumtree.csagent.config.MockProperties;
import com.gumtree.csagent.model.BotSession;
import com.gumtree.csagent.repository.BotEventRepository;
import com.gumtree.csagent.service.guardrails.ProgressPlaceholderService;
import com.gumtree.csagent.service.runtime.IntakeFieldsRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Sprint 080 / S-Auto-25 / M-Auto-6 Sub-sprint C-1 (R7) — #7 dispatch-wiring
 * smoke for {@code update_intake_fields}, exercised end-to-end through the REAL
 * {@link ToolDispatcher} + REAL {@link ToolPolicyEnforcer} (which loads the
 * production {@code config/tool-policy.yaml}). This is the reproducible,
 * CI-gated equivalent of the live-backend POST: it proves the full runtime
 * dispatch path (registry lookup → {@code isToolAllowed} → {@code tool.execute}
 * → persist) without depending on a running server or a real-LLM tool emission.
 *
 * <p>Per §5.7, this is dispatch-WIRING evidence only; outcome evidence is the
 * M-Auto-6 milestone-shared real-LLM re-bless.
 */
@ExtendWith(MockitoExtension.class)
class UpdateIntakeFieldsDispatchSmokeTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private BotEventRepository botEventRepository;
    @Mock
    private ProgressPlaceholderService placeholderService;
    @Mock
    private MockProperties mockProperties;

    private ToolDispatcher dispatcher;

    @BeforeEach
    void setUp() {
        // Real enforcer loading the production tool-policy.yaml from classpath.
        ToolPolicyEnforcer enforcer = new ToolPolicyEnforcer(botEventRepository);
        enforcer.init();
        dispatcher = new ToolDispatcher(
                enforcer,
                List.of(new UpdateIntakeFieldsTool(objectMapper)),
                placeholderService,
                mockProperties);
    }

    private BotSession intakeSession(String uc, String intakeFieldsJson) {
        BotSession s = new BotSession();
        s.setSessionId("uif-smoke-" + uc);
        s.setCurrentPhase("RESOLVE");
        s.setActiveUseCase(uc);
        s.setIntakeFields(intakeFieldsJson);
        return s;
    }

    @Test
    void dispatchesForIntakeUc_persistsAndReturnsStructuralResult() {
        BotSession session = intakeSession("UC-J", null);

        // Request shape carried by POST /v1/chat/sessions/{id}/messages tool call:
        //   { "name": "update_intake_fields", "arguments": { "fields": {...} } }
        ToolResult result = dispatcher.dispatch(
                "update_intake_fields", session,
                Map.of("fields", Map.of("report_type", "scam")));

        // Response body shape: { status: ok, fields_merged: 1, fields_persisted: [report_type] }
        assertTrue(result.isSuccess(), "update_intake_fields must dispatch successfully for UC-J");
        assertEquals("ok", result.getData().get("status"));
        assertEquals(1, result.getData().get("fields_merged"));
        assertEquals(List.of("report_type"), result.getData().get("fields_persisted"));
        assertEquals("scam",
                IntakeFieldsRegistry.parseCollectedFields(objectMapper, session.getIntakeFields())
                        .get("report_type"));
    }

    @Test
    void policyScoped_blockedForNonIntakeUc() {
        // tool-policy.yaml scopes update_intake_fields to UC-G/H/I/J/K, so a
        // RESOLVE-FAQ UC (UC-A) is policy-blocked at the dispatcher.
        BotSession session = intakeSession("UC-A", null);

        ToolResult result = dispatcher.dispatch(
                "update_intake_fields", session,
                Map.of("fields", Map.of("report_type", "scam")));

        assertFalse(result.isSuccess(), "update_intake_fields must be policy-blocked for UC-A");
        assertTrue(IntakeFieldsRegistry.parseCollectedFields(objectMapper, session.getIntakeFields())
                .isEmpty());
    }
}
