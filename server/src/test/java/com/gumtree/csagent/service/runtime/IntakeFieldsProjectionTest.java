package com.gumtree.csagent.service.runtime;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gumtree.csagent.model.BotSession;
import com.gumtree.csagent.model.PhasePlan;
import com.gumtree.csagent.model.TerminalOutcome;
import com.gumtree.csagent.service.tools.ToolPolicyEnforcer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;

/**
 * Sprint 078 / S-Auto-23 / M-Auto-6 Sub-sprint A — R1.a #1 (request_handover
 * schema declares {@code intake_fields}) + #2 (per-active-UC
 * {@code required_intake_fields_for_active_uc} projection).
 *
 * <p>Anti-误杀: the schema field is universal-optional (absent from
 * {@code required[]}, so the validator is never forced and non-intake UCs are
 * unaffected); only the projected REQUIREMENT is per-UC, and it is iterated
 * from {@link IntakeFieldsRegistry} (single source of truth — no per-UC matrix
 * replicated in the projection builder).
 */
@ExtendWith(MockitoExtension.class)
class IntakeFieldsProjectionTest {

    private ContextProjectionBuilder builder;
    private ObjectMapper objectMapper;

    @Mock
    private UseCaseRegistryService useCaseRegistry;
    @Mock
    private ControlPolicyService controlPolicy;
    @Mock
    private ToolPolicyEnforcer toolPolicyEnforcer;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        builder = new ContextProjectionBuilder(objectMapper, useCaseRegistry,
                controlPolicy, toolPolicyEnforcer, null);
        builder.initToolSchemas();

        lenient().when(controlPolicy.getMaxBotTurnsFaq()).thenReturn(6);
        lenient().when(controlPolicy.getMaxClarificationRounds()).thenReturn(2);
        lenient().when(controlPolicy.getMaxFaqMiss()).thenReturn(2);
        lenient().when(toolPolicyEnforcer.getVisibleToolsForUc(anyString()))
                .thenReturn(List.of("request_handover", "record_outcome"));
    }

    // --- #1: schema declares intake_fields as an OPTIONAL object property ---

    @Test
    void requestHandoverSchema_declaresOptionalIntakeFieldsObject() throws Exception {
        JsonNode argsSchema = requestHandoverArgsSchema("UC-J", "INTAKE");

        JsonNode props = argsSchema.get("properties");
        assertTrue(props.has("intake_fields"),
                "request_handover schema must declare an intake_fields property");
        assertEquals("object", props.get("intake_fields").get("type").asText(),
                "intake_fields must be a free-form object (string->string map), not a per-UC matrix");
        assertFalse(props.get("intake_fields").has("properties"),
                "intake_fields must NOT enumerate per-UC properties (that would be a per-UC matrix)");

        // Optional: escalation_reason stays the ONLY required field, so the
        // validator is never forced and non-intake UCs are unaffected.
        JsonNode required = argsSchema.get("required");
        assertTrue(required.isArray());
        boolean hasEscalationReason = false;
        boolean hasIntakeFields = false;
        for (JsonNode r : required) {
            if ("escalation_reason".equals(r.asText())) hasEscalationReason = true;
            if ("intake_fields".equals(r.asText())) hasIntakeFields = true;
        }
        assertTrue(hasEscalationReason, "escalation_reason must remain required");
        assertFalse(hasIntakeFields, "intake_fields must be OPTIONAL (not in required[])");
    }

    // --- #2 positive: each intake UC projects its registry required-fields list ---

    @Test
    void intakeUcG_projectsRegistryRequiredFields() throws Exception {
        assertRequiredFields("UC-G", List.of("registered_email", "data_request_type"));
    }

    @Test
    void intakeUcH_projectsRegistryRequiredFields() throws Exception {
        assertRequiredFields("UC-H",
                List.of("ad_id_or_listing_url", "registered_email", "stated_reason_or_context"));
    }

    @Test
    void intakeUcI_projectsRegistryRequiredFields() throws Exception {
        assertRequiredFields("UC-I", List.of("transaction_reference", "dispute_reason"));
    }

    @Test
    void intakeUcJ_projectsRegistryRequiredFields() throws Exception {
        assertRequiredFields("UC-J", List.of("report_target", "report_type", "description"));
    }

    @Test
    void intakeUcK_projectsRegistryRequiredFields() throws Exception {
        assertRequiredFields("UC-K", List.of("platform", "repro_steps_or_error_message"));
    }

    @Test
    void projectedRequiredFields_matchRegistrySource() throws Exception {
        // Single-source-of-truth guard: the projection equals the registry
        // output verbatim (no replicated per-UC list to drift).
        for (String uc : List.of("UC-G", "UC-H", "UC-I", "UC-J", "UC-K")) {
            assertRequiredFields(uc, IntakeFieldsRegistry.requiredFieldsFor(uc));
        }
    }

    // --- #2 negative: non-intake UCs OMIT the field (never an empty list) ---

    @Test
    void nonIntakeUcs_omitRequiredIntakeFieldsField() throws Exception {
        for (String uc : List.of("UC-A", "UC-B", "UC-D", "UC-F", "UC-FP")) {
            JsonNode projection = buildProjectionFor(uc, "RESOLVE", "{\"ad_id\":\"123\"}");
            assertFalse(projection.has("required_intake_fields_for_active_uc"),
                    "non-intake UC " + uc + " must OMIT required_intake_fields_for_active_uc "
                            + "(not emit an empty list)");
        }
    }

    @Test
    void nullActiveUc_omitsRequiredIntakeFieldsField() throws Exception {
        JsonNode projection = buildProjectionFor(null, "DISCOVER", "{}");
        assertFalse(projection.has("required_intake_fields_for_active_uc"),
                "null active UC must omit required_intake_fields_for_active_uc");
    }

    // --- helpers ---

    private void assertRequiredFields(String uc, List<String> expected) throws Exception {
        JsonNode projection = buildProjectionFor(uc, "INTAKE", "{\"email\":\"a@b.com\"}");
        assertTrue(projection.has("required_intake_fields_for_active_uc"),
                "intake UC " + uc + " must project required_intake_fields_for_active_uc");
        JsonNode arr = projection.get("required_intake_fields_for_active_uc");
        assertTrue(arr.isArray());
        assertEquals(expected.size(), arr.size(),
                "required-fields count mismatch for " + uc);
        for (int i = 0; i < expected.size(); i++) {
            assertEquals(expected.get(i), arr.get(i).asText(),
                    "required-field[" + i + "] mismatch for " + uc + " (ordering must match registry)");
        }
    }

    private JsonNode buildProjectionFor(String uc, String phase, String formContext) throws Exception {
        BotSession session = BotSession.builder()
                .sessionId("sprint078-r1-test")
                .activeUseCase(uc)
                .currentPhase(phase)
                .handlingState("BOT_HANDLING")
                .totalBotTurns(0)
                .clarificationCount(0)
                .faqMissCount(0)
                .repeatedActionCount(0)
                .formContext(formContext)
                .build();
        PhasePlan plan = PhasePlan.builder()
                .phase(phase)
                .useCase(uc)
                .objective("intake")
                .allowedTools(List.of("request_handover", "record_outcome"))
                .maxToolSteps(3)
                .systemInstruction(phase)
                .validTerminalOutcomes(Set.of(TerminalOutcome.ESCALATE))
                .build();
        String json = builder.build(session, List.of(), plan, "hello", null, List.of());
        return objectMapper.readTree(json);
    }

    private JsonNode requestHandoverArgsSchema(String uc, String phase) throws Exception {
        JsonNode projection = buildProjectionFor(uc, phase, "{\"email\":\"a@b.com\"}");
        JsonNode toolSchemas = projection.get("tool_schemas");
        assertNotNull(toolSchemas, "tool_schemas must be present");
        for (JsonNode schema : toolSchemas) {
            if ("request_handover".equals(schema.path("name").asText())) {
                return schema.get("arguments_schema");
            }
        }
        throw new AssertionError("request_handover schema not found in tool_schemas");
    }
}
