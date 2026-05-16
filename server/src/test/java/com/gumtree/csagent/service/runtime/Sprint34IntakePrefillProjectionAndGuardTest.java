package com.gumtree.csagent.service.runtime;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gumtree.csagent.model.BotSession;
import com.gumtree.csagent.model.PhasePlan;
import com.gumtree.csagent.model.ToolCall;
import com.gumtree.csagent.service.tools.ToolPolicyEnforcer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * Sprint 34 — projection-flip + intake-complete-guard regression for the
 * UC-G / UC-H / UC-I / UC-J form-context prefill extension. Mirrors the
 * {@link Sprint71PartialIntakePersistenceTest} projection-flip + guard
 * shape and is the per-UC complement to that file (which stays the UC-K
 * regression guard at HEAD).
 *
 * <p>Contract under test, per UC: after
 * {@link AgentRunLoopImpl#mergePartialIntakeFromContext} has run on a
 * session whose {@code formContext} carries the source-of-truth fields,
 * the next call to
 * {@link ContextProjectionBuilder#buildProjection} (or {@code build})
 * shows the prefilled canonical names under
 * {@code intake_state.fields_collected}, drops them from
 * {@code fields_remaining}, and — when intake is otherwise complete —
 * flips {@code intake_state.intake_complete} to {@code true} +
 * {@link AgentRunLoopImpl#shouldRejectIncompleteIntakeHandover} returns
 * {@code false}.
 */
@ExtendWith(MockitoExtension.class)
class Sprint34IntakePrefillProjectionAndGuardTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private ContextProjectionBuilder builder;
    private AgentRunLoopImpl runLoop;

    @Mock private UseCaseRegistryService useCaseRegistry;
    @Mock private ControlPolicyService controlPolicy;
    @Mock private ToolPolicyEnforcer toolPolicyEnforcer;

    @BeforeEach
    void setUp() {
        builder = new ContextProjectionBuilder(objectMapper, useCaseRegistry,
                controlPolicy, toolPolicyEnforcer);
        builder.initToolSchemas();
        runLoop = new AgentRunLoopImpl(null, null, builder, null, objectMapper);
        lenient().when(controlPolicy.getMaxBotTurnsFaq()).thenReturn(6);
        lenient().when(controlPolicy.getMaxBotTurnsIntake()).thenReturn(8);
        lenient().when(controlPolicy.getMaxClarificationRounds()).thenReturn(3);
        lenient().when(controlPolicy.getMaxFaqMiss()).thenReturn(2);
    }

    // -------- UC-H projection flip --------

    @Test
    void ucH_projectionShowsAdIdAndEmailCollected_afterFormContextMerge() throws Exception {
        BotSession session = intakeSession("UC-H");
        session.setFormContext("{\"first_name\":\"Alice\","
                + "\"email\":\"alice@example.com\","
                + "\"topic_subject\":\"Ad Support\","
                + "\"ad_id\":\"AD-2001\","
                + "\"description\":\"can't see my ad\"}");
        wireRegistry("UC-H", "Ad Removal Appeal", "Ad Support", "INTAKE");

        runLoop.mergePartialIntakeFromContext(session, intakePlan("UC-H"),
                "I want to appeal");

        // Persisted form: canonical names land in session.intakeFields.
        Map<String, String> collected = IntakeFieldsRegistry.parseCollectedFields(
                objectMapper, session.getIntakeFields());
        assertEquals("AD-2001", collected.get("ad_id_or_listing_url"));
        assertEquals("alice@example.com", collected.get("registered_email"));
        assertNull(collected.get("stated_reason_or_context"),
                "stated_reason_or_context has no form_context source — must remain unseeded");

        // Projection form: fields_collected reflects two of three; one remains.
        String projection = builder.buildProjection(session, List.of(), null,
                "I want to appeal");
        JsonNode intake = objectMapper.readTree(projection).get("intake_state");
        assertNotNull(intake, "projection must always carry intake_state slot for UC-H");
        assertTrue(intake.get("fields_collected").has("ad_id_or_listing_url"));
        assertTrue(intake.get("fields_collected").has("registered_email"));
        assertEquals(1, intake.get("fields_remaining").size(),
                "exactly one required field must remain (stated_reason_or_context)");
        assertEquals("stated_reason_or_context",
                intake.get("fields_remaining").get(0).asText());
        assertFalse(intake.get("intake_complete").asBoolean(),
                "intake must remain incomplete until stated_reason_or_context is supplied");
    }

    @Test
    void ucH_intakeCompleteGuard_flipsTrue_afterStatedReasonAlsoSupplied() {
        // Sanity end-to-end: after the extractor seeds ad_id +
        // registered_email AND the LLM supplies stated_reason_or_context
        // via the inline intake_fields hook, the existing intake-complete
        // guard allows the handover.
        BotSession session = intakeSession("UC-H");
        session.setFormContext("{\"email\":\"alice@example.com\",\"ad_id\":\"AD-2001\"}");
        runLoop.mergePartialIntakeFromContext(session, intakePlan("UC-H"),
                "I want to appeal");

        // Simulate the LLM persisting the third field through the inline
        // path (request_handover.arguments.intake_fields).
        ToolCall inlineCall = new ToolCall("request_handover", Map.of(
                "escalation_reason", "intake_complete_for_uc_h",
                "intake_fields", Map.of("stated_reason_or_context",
                        "I believe the removal was a mistake.")));
        runLoop.persistInlineIntakeFields(session, inlineCall);

        Map<String, String> collected = IntakeFieldsRegistry.parseCollectedFields(
                objectMapper, session.getIntakeFields());
        assertTrue(IntakeFieldsRegistry.intakeComplete("UC-H", collected),
                "intake_complete(UC-H) must flip true once all three required fields are present");
        assertFalse(AgentRunLoopImpl.shouldRejectIncompleteIntakeHandover(
                intakePlan("UC-H"), inlineCall, session),
                "guard must allow the handover once stated_reason has also been supplied");
    }

    @Test
    void ucH_intakeCompleteGuard_stillRejects_whenStatedReasonStillMissing() {
        // Negative-control on the §1.7 anti-lie principle: even after
        // Sprint 34's extractor seeds ad_id + email, the
        // intake_complete_for_uc_h handover MUST still be rejected if
        // stated_reason_or_context has not actually been collected. This
        // pins the Alice closure-criterion (c) escape path: bot can
        // gracefully escalate via reason=user_requested or
        // reason=ambiguous_intent, but lying about intake_complete is
        // still blocked.
        BotSession session = intakeSession("UC-H");
        session.setFormContext("{\"email\":\"alice@example.com\",\"ad_id\":\"AD-2001\"}");
        runLoop.mergePartialIntakeFromContext(session, intakePlan("UC-H"),
                "I don't know why my ad was removed");

        ToolCall prematureCall = new ToolCall("request_handover",
                Map.of("escalation_reason", "intake_complete_for_uc_h"));
        assertTrue(AgentRunLoopImpl.shouldRejectIncompleteIntakeHandover(
                intakePlan("UC-H"), prematureCall, session),
                "guard must still reject intake_complete_for_uc_h when "
                        + "stated_reason_or_context is unsupplied");
    }

    @Test
    void ucH_intakeCompleteGuard_acceptsUserRequestedEscape() {
        BotSession session = intakeSession("UC-H");
        session.setFormContext("{\"email\":\"alice@example.com\",\"ad_id\":\"AD-2001\"}");
        runLoop.mergePartialIntakeFromContext(session, intakePlan("UC-H"),
                "I just want a human");

        ToolCall escape = new ToolCall("request_handover",
                Map.of("escalation_reason", "user_requested"));
        assertFalse(AgentRunLoopImpl.shouldRejectIncompleteIntakeHandover(
                intakePlan("UC-H"), escape, session),
                "user_requested escape path must pass through regardless of "
                        + "intake completeness — Alice closure-criterion (c) shape");
    }

    // -------- UC-G projection flip --------

    @Test
    void ucG_projectionShowsRegisteredEmailCollected_afterFormContextMerge() throws Exception {
        BotSession session = intakeSession("UC-G");
        session.setFormContext("{\"first_name\":\"Dave\","
                + "\"email\":\"dave@example.com\","
                + "\"topic_subject\":\"Account & Privacy\","
                + "\"description\":\"delete my account\"}");
        wireRegistry("UC-G", "GDPR / Data Action", "Account & Privacy", "INTAKE");

        runLoop.mergePartialIntakeFromContext(session, intakePlan("UC-G"),
                "delete please");

        Map<String, String> collected = IntakeFieldsRegistry.parseCollectedFields(
                objectMapper, session.getIntakeFields());
        assertEquals("dave@example.com", collected.get("registered_email"));

        String projection = builder.buildProjection(session, List.of(), null,
                "delete please");
        JsonNode intake = objectMapper.readTree(projection).get("intake_state");
        assertTrue(intake.get("fields_collected").has("registered_email"));
        assertEquals(1, intake.get("fields_remaining").size());
        assertEquals("data_request_type", intake.get("fields_remaining").get(0).asText());
        assertFalse(intake.get("intake_complete").asBoolean());
    }

    @Test
    void ucG_intakeCompleteGuard_flipsTrue_afterDataRequestTypeAlsoSupplied() {
        BotSession session = intakeSession("UC-G");
        session.setFormContext("{\"email\":\"dave@example.com\"}");
        runLoop.mergePartialIntakeFromContext(session, intakePlan("UC-G"),
                "delete my account");

        ToolCall inlineCall = new ToolCall("request_handover", Map.of(
                "escalation_reason", "intake_complete_for_uc_g",
                "intake_fields", Map.of("data_request_type", "deletion")));
        runLoop.persistInlineIntakeFields(session, inlineCall);

        Map<String, String> collected = IntakeFieldsRegistry.parseCollectedFields(
                objectMapper, session.getIntakeFields());
        assertTrue(IntakeFieldsRegistry.intakeComplete("UC-G", collected));
        assertFalse(AgentRunLoopImpl.shouldRejectIncompleteIntakeHandover(
                intakePlan("UC-G"), inlineCall, session));
    }

    // -------- UC-J projection flip --------

    @Test
    void ucJ_projectionShowsDescriptionCollected_afterFormContextMerge() throws Exception {
        BotSession session = intakeSession("UC-J");
        session.setFormContext("{\"first_name\":\"Frank\","
                + "\"email\":\"frank@example.com\","
                + "\"topic_subject\":\"Trust & Safety\","
                + "\"description\":\"user XYZ is posting spam ads repeatedly\"}");
        wireRegistry("UC-J", "Trust & Safety Report", "Trust & Safety", "INTAKE");

        runLoop.mergePartialIntakeFromContext(session, intakePlan("UC-J"),
                "please review");

        Map<String, String> collected = IntakeFieldsRegistry.parseCollectedFields(
                objectMapper, session.getIntakeFields());
        assertEquals("user XYZ is posting spam ads repeatedly", collected.get("description"));

        String projection = builder.buildProjection(session, List.of(), null,
                "please review");
        JsonNode intake = objectMapper.readTree(projection).get("intake_state");
        assertTrue(intake.get("fields_collected").has("description"));
        assertEquals(2, intake.get("fields_remaining").size(),
                "report_target + report_type still missing for UC-J");
        assertFalse(intake.get("intake_complete").asBoolean());
    }

    @Test
    void ucJ_intakeCompleteGuard_flipsTrue_afterReportTargetAndTypeAlsoSupplied() {
        BotSession session = intakeSession("UC-J");
        session.setFormContext("{\"description\":\"user XYZ is posting spam\"}");
        runLoop.mergePartialIntakeFromContext(session, intakePlan("UC-J"),
                "please review");

        ToolCall inlineCall = new ToolCall("request_handover", Map.of(
                "escalation_reason", "intake_complete_for_uc_j",
                "intake_fields", Map.of(
                        "report_target", "user XYZ",
                        "report_type", "spam")));
        runLoop.persistInlineIntakeFields(session, inlineCall);

        Map<String, String> collected = IntakeFieldsRegistry.parseCollectedFields(
                objectMapper, session.getIntakeFields());
        assertTrue(IntakeFieldsRegistry.intakeComplete("UC-J", collected));
        assertFalse(AgentRunLoopImpl.shouldRejectIncompleteIntakeHandover(
                intakePlan("UC-J"), inlineCall, session));
    }

    // -------- UC-I projection — deliberate no-op --------

    @Test
    void ucI_projection_isUnchangedAfterMerge_perDeliberateNoOp() throws Exception {
        BotSession session = intakeSession("UC-I");
        session.setFormContext("{\"first_name\":\"Ivan\","
                + "\"email\":\"ivan@example.com\","
                + "\"topic_subject\":\"Payments\","
                + "\"description\":\"my refund didn't arrive\"}");
        wireRegistry("UC-I", "Refund / Payment Dispute", "Payments", "INTAKE");

        runLoop.mergePartialIntakeFromContext(session, intakePlan("UC-I"),
                "refund");

        assertNull(session.getIntakeFields(),
                "UC-I extractor is a deliberate no-op — session.intakeFields must "
                        + "remain unwritten");

        String projection = builder.buildProjection(session, List.of(), null,
                "refund");
        JsonNode intake = objectMapper.readTree(projection).get("intake_state");
        assertEquals(0, intake.get("fields_collected").size(),
                "UC-I no-op: fields_collected stays empty");
        assertEquals(2, intake.get("fields_remaining").size(),
                "UC-I no-op: both required fields still remain");
        assertFalse(intake.get("intake_complete").asBoolean());
    }

    @Test
    void ucI_intakeCompleteGuard_stillEnforced_perDeliberateNoOp() {
        // Sanity: UC-I no-op does not weaken the §1.7 anti-lie principle.
        // A premature intake_complete_for_uc_i is still rejected.
        BotSession session = intakeSession("UC-I");
        session.setFormContext("{\"email\":\"ivan@example.com\"}");
        runLoop.mergePartialIntakeFromContext(session, intakePlan("UC-I"),
                "refund");

        ToolCall prematureCall = new ToolCall("request_handover",
                Map.of("escalation_reason", "intake_complete_for_uc_i"));
        assertTrue(AgentRunLoopImpl.shouldRejectIncompleteIntakeHandover(
                intakePlan("UC-I"), prematureCall, session),
                "intake-complete guard must still reject UC-I handover when "
                        + "no required fields are collected");
    }

    // -------- UC-K regression guard --------

    @Test
    void ucK_projectionStillFires_perSprint71Contract() throws Exception {
        BotSession session = intakeSession("UC-K");
        session.setFormContext("{\"first_name\":\"Stephen\","
                + "\"description\":\"Why am I not getting the option to add my "
                + "phone number as a point of contact when listening an item any more\"}");
        wireRegistry("UC-K", "Technical Issue Intake", "Technical Support", "INTAKE");

        runLoop.mergePartialIntakeFromContext(session, intakePlan("UC-K"),
                "I'm using Chrome on Windows 11");

        Map<String, String> collected = IntakeFieldsRegistry.parseCollectedFields(
                objectMapper, session.getIntakeFields());
        // Sprint 34 must NOT regress UC-K extraction: both fields land.
        assertNotNull(collected.get("platform"));
        assertNotNull(collected.get("repro_steps_or_error_message"));
        assertTrue(IntakeFieldsRegistry.intakeComplete("UC-K", collected));

        String projection = builder.buildProjection(session, List.of(), null,
                "I'm using Chrome on Windows 11");
        JsonNode intake = objectMapper.readTree(projection).get("intake_state");
        assertEquals(2, intake.get("fields_collected").size());
        assertEquals(0, intake.get("fields_remaining").size());
        assertTrue(intake.get("intake_complete").asBoolean());
    }

    // -------- FAQ-path UCs — negative control --------

    @Test
    void faqPathUcs_doNotReceivePartialIntakeMerge() {
        // The mergePartialIntakeFromContext early-exits on
        // !IntakeFieldsRegistry.isIntakeUseCase(uc); Sprint 34 must not
        // erode this gate. UC-A FAQ-path session: form_context with all
        // fields, but no intake-fields write must occur.
        BotSession session = BotSession.builder()
                .sessionId("uc-a-negative-control")
                .activeUseCase("UC-A")
                .currentPhase("RESOLVE")
                .handlingState("BOT_HANDLING")
                .totalBotTurns(0).clarificationCount(0)
                .faqMissCount(0).repeatedActionCount(0)
                .formContext("{\"email\":\"alice@example.com\","
                        + "\"ad_id\":\"AD-7777\","
                        + "\"description\":\"can't see my ad\"}")
                .build();
        PhasePlan faqPlan = PhasePlan.builder()
                .phase("RESOLVE").useCase("UC-A").objective("faq")
                .allowedTools(List.of("search_knowledge", "resolve_article",
                        "request_handover", "record_outcome"))
                .maxToolSteps(4).build();

        runLoop.mergePartialIntakeFromContext(session, faqPlan,
                "where is my ad?");

        assertNull(session.getIntakeFields(),
                "FAQ-path UC-A must never receive partial intake-field persistence");
    }

    // -------- helpers --------

    private BotSession intakeSession(String uc) {
        return BotSession.builder()
                .sessionId("sprint34-prefill-" + uc)
                .activeUseCase(uc)
                .currentPhase("RESOLVE")
                .handlingState("BOT_HANDLING")
                .totalBotTurns(0).clarificationCount(0)
                .faqMissCount(0).repeatedActionCount(0)
                .build();
    }

    private PhasePlan intakePlan(String uc) {
        return PhasePlan.builder()
                .phase("RESOLVE")
                .useCase(uc)
                .objective("intake")
                .allowedTools(List.of("request_handover"))
                .maxToolSteps(3)
                .build();
    }

    private void wireRegistry(String uc, String name, String topic, String phaseShape) {
        UseCaseRegistryService.UseCaseDefinition def =
                new UseCaseRegistryService.UseCaseDefinition(
                        uc, name, List.of(topic), "MEDIUM", false, phaseShape);
        when(useCaseRegistry.getUseCase(uc)).thenReturn(def);
        when(toolPolicyEnforcer.getVisibleToolsForUc(uc))
                .thenReturn(List.of("request_handover"));
    }
}
