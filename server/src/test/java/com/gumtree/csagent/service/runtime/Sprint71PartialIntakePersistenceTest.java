package com.gumtree.csagent.service.runtime;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gumtree.csagent.model.BotSession;
import com.gumtree.csagent.model.PhasePlan;
import com.gumtree.csagent.model.ToolCall;
import com.gumtree.csagent.service.runtime.skill.DispatchContext;
import com.gumtree.csagent.service.runtime.skill.RejectVerdict;
import com.gumtree.csagent.service.runtime.skill.SkillGuardrailDispatcher;
import com.gumtree.csagent.service.runtime.skill.SkillTestFixtures;
import com.gumtree.csagent.service.tools.ToolPolicyEnforcer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * Sprint 7.1 §J0 — focused regression for partial intake-field persistence
 * across normal clarification turns. Anchored on cs_interactive_066 (UC-K —
 * canonical required fields {@code [platform, repro_steps_or_error_message]}).
 *
 * <p>Prior to Sprint 7.1, {@code session.intakeFields} was only ever written
 * when the LLM emitted {@code request_handover.arguments.intake_fields}. A
 * normal intake clarification turn therefore left every required field
 * "remaining" in the next {@code intake_state} projection, so the bot kept
 * asking for fields the user had already supplied (the cs066 stall shape).
 *
 * <p>This test exercises:
 * <ul>
 *   <li>{@link IntakeFieldExtractor} extraction heuristics for UC-K
 *       (platform from the user reply, repro_steps from the form
 *       description).</li>
 *   <li>{@link AgentRunLoopImpl#mergePartialIntakeFromContext} writing
 *       extracted fields back to {@code session.intakeFields}.</li>
 *   <li>The next {@link ContextProjectionBuilder#buildProjection}
 *       projection moves the supplied field from {@code fields_remaining}
 *       to {@code fields_collected}.</li>
 *   <li>Multi-turn cs066 flow: form-context-seed + user-reply both land
 *       and {@code intake_complete} flips true.</li>
 *   <li>Negative: FAQ-path UCs are unaffected (no projection slot, no
 *       merge).</li>
 *   <li>Guard: an {@code intake_complete_for_uc_k} handover is still
 *       rejected when the extractor cannot infer a required field.</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class Sprint71PartialIntakePersistenceTest {

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

    // -------- IntakeFieldExtractor unit contracts --------

    @Test
    void extractor_capturesPlatformFromUserReply() {
        Map<String, String> out = IntakeFieldExtractor.extractFromTurn(
                "UC-K", "I'm using Chrome on Windows 11", null, objectMapper);
        assertEquals("I'm using Chrome on Windows 11", out.get("platform"));
    }

    @Test
    void extractor_capturesPlatformFromShortAppReply() {
        Map<String, String> out = IntakeFieldExtractor.extractFromTurn(
                "UC-K", "the app", null, objectMapper);
        assertEquals("the app", out.get("platform"));
    }

    @Test
    void extractor_capturesReproStepsFromCs066FormDescription() {
        String formContext = "{\"description\":\""
                + "Why am I not getting the option to add my phone number as a "
                + "point of contact when listening an item any more\"}";
        Map<String, String> out = IntakeFieldExtractor.extractFromTurn(
                "UC-K", "thanks", formContext, objectMapper);
        assertNotNull(out.get("repro_steps_or_error_message"),
                "cs066 form description must seed repro_steps_or_error_message");
        assertTrue(out.get("repro_steps_or_error_message")
                        .toLowerCase().contains("phone number"),
                "extracted repro_steps must reference the regression text");
    }

    @Test
    void extractor_doesNotCapturePlatformWhenUserSaysOnlyShortFiller() {
        // "thanks" / "ok" do not contain a platform token — must NOT be
        // mistaken for a platform reply.
        assertNull(IntakeFieldExtractor.extractFromTurn(
                "UC-K", "ok thanks", null, objectMapper).get("platform"));
        assertNull(IntakeFieldExtractor.extractFromTurn(
                "UC-K", "yes please help", null, objectMapper).get("platform"));
    }

    @Test
    void extractor_doesNotCaptureReproStepsForShortFiller() {
        // "thanks" alone is not a regression description.
        assertNull(IntakeFieldExtractor.extractFromTurn(
                "UC-K", "thanks", null, objectMapper).get("repro_steps_or_error_message"));
    }

    @Test
    void extractor_isNoOpForNonUcK() {
        // UC-H, UC-J, UC-G, UC-I are not yet handled by the extractor —
        // fields still arrive via request_handover.arguments.intake_fields.
        assertTrue(IntakeFieldExtractor.extractFromTurn(
                "UC-H", "AD-9 / spam flag", null, objectMapper).isEmpty());
        assertTrue(IntakeFieldExtractor.extractFromTurn(
                "UC-A", "any FAQ-path text", null, objectMapper).isEmpty());
    }

    @Test
    void extractor_doesNotOverwriteExistingFields() {
        Map<String, String> existing = new HashMap<>();
        existing.put("platform", "Android 14");
        Map<String, String> merged = IntakeFieldExtractor.mergeForUc(
                "UC-K", existing,
                "switching to Chrome on Windows now",
                "{\"description\":\"option missing any more\"}",
                objectMapper);
        // existing platform value preserved; repro_steps newly added from form.
        assertEquals("Android 14", merged.get("platform"));
        assertNotNull(merged.get("repro_steps_or_error_message"));
    }

    // -------- AgentRunLoopImpl.mergePartialIntakeFromContext --------

    @Test
    void mergePartialIntake_seedsReproStepsFromCs066FormDescription() {
        BotSession session = ucKIntakeSession();
        session.setFormContext("{\"first_name\":\"Stephen\","
                + "\"description\":\"Why am I not getting the option to add my "
                + "phone number as a point of contact when listening an item any more\"}");
        // No prior intake fields.

        runLoop.mergePartialIntakeFromContext(session, intakePlan("UC-K"),
                "I'm using Chrome on Windows 11");

        Map<String, String> collected = IntakeFieldsRegistry.parseCollectedFields(
                objectMapper, session.getIntakeFields());
        // Both fields land: platform from user reply, repro_steps from form.
        assertNotNull(collected.get("platform"),
                "platform must be persisted from user reply");
        assertNotNull(collected.get("repro_steps_or_error_message"),
                "repro_steps must be seeded from cs066 form description");
        assertTrue(IntakeFieldsRegistry.intakeComplete("UC-K", collected),
                "UC-K intake should be complete after seed + reply");
    }

    @Test
    void mergePartialIntake_partialFlow_onlyPlatformLands_whenFormDescriptionIsBlank() {
        BotSession session = ucKIntakeSession();
        session.setFormContext("{\"first_name\":\"Stephen\",\"description\":\"\"}");

        runLoop.mergePartialIntakeFromContext(session, intakePlan("UC-K"),
                "I'm on the Android app");

        Map<String, String> collected = IntakeFieldsRegistry.parseCollectedFields(
                objectMapper, session.getIntakeFields());
        assertNotNull(collected.get("platform"));
        assertNull(collected.get("repro_steps_or_error_message"),
                "repro_steps must NOT be inferred when form description is blank "
                        + "and user reply is just a platform answer");
        assertFalse(IntakeFieldsRegistry.intakeComplete("UC-K", collected));
    }

    @Test
    void mergePartialIntake_isNoOpForFaqUc() {
        BotSession session = BotSession.builder()
                .sessionId("test-faq")
                .activeUseCase("UC-A")
                .currentPhase("RESOLVE")
                .handlingState("BOT_HANDLING")
                .totalBotTurns(0).clarificationCount(0)
                .faqMissCount(0).repeatedActionCount(0)
                .formContext("{\"description\":\"my ad isn't visible any more\"}")
                .build();
        PhasePlan faqPlan = PhasePlan.builder()
                .phase("RESOLVE").useCase("UC-A").objective("faq")
                .allowedTools(List.of("search_knowledge", "resolve_article",
                        "request_handover", "record_outcome"))
                .maxToolSteps(4).build();

        runLoop.mergePartialIntakeFromContext(session, faqPlan, "I'm on Chrome");
        assertNull(session.getIntakeFields(),
                "FAQ-path UCs must never receive partial intake-field persistence");
    }

    @Test
    void mergePartialIntake_isNoOpWhenNoFieldsExtracted() {
        BotSession session = ucKIntakeSession();
        session.setFormContext("{\"description\":\"\"}");
        runLoop.mergePartialIntakeFromContext(session, intakePlan("UC-K"), "ok thanks");
        assertNull(session.getIntakeFields(),
                "filler reply with no platform / regression text must not write");
    }

    // -------- Multi-turn cs066 projection contract --------

    @Test
    void cs066MultiTurn_partialFlow_projectionShowsOnlyMissingField() throws Exception {
        BotSession session = ucKIntakeSession();
        session.setFormContext("{\"first_name\":\"Stephen\","
                + "\"description\":\"Why am I not getting the option to add my "
                + "phone number as a point of contact when listening an item any more\"}");
        wireUcKRegistry();

        // Turn 1 — empty user reply (form description seed only). The
        // form description matches the UC-K regression marker, so
        // repro_steps lands; platform stays remaining.
        runLoop.mergePartialIntakeFromContext(session, intakePlan("UC-K"), "Should be customer@example.com");

        String t1Projection = builder.buildProjection(session, List.of(), null,
                "Should be customer@example.com");
        JsonNode t1 = objectMapper.readTree(t1Projection).get("intake_state");
        assertTrue(t1.get("fields_collected").has("repro_steps_or_error_message"),
                "Turn 1 projection: repro_steps must be collected from form description");
        assertEquals(1, t1.get("fields_remaining").size(),
                "Turn 1 projection: only platform should remain");
        assertEquals("platform", t1.get("fields_remaining").get(0).asText());
        assertFalse(t1.get("intake_complete").asBoolean());

        // Turn 2 — user supplies platform.
        runLoop.mergePartialIntakeFromContext(session, intakePlan("UC-K"),
                "I'm using Chrome on Windows 11");

        String t2Projection = builder.buildProjection(session, List.of(), null,
                "I'm using Chrome on Windows 11");
        JsonNode t2 = objectMapper.readTree(t2Projection).get("intake_state");
        assertEquals(2, t2.get("fields_collected").size(),
                "Turn 2 projection: both required fields must be collected");
        assertEquals(0, t2.get("fields_remaining").size());
        assertTrue(t2.get("intake_complete").asBoolean());
    }

    @Test
    void cs066MultiTurn_handoverGuard_acceptsAfterBothFieldsCollected() {
        // Sanity: once partial-intake persistence + the existing inline
        // hook have populated both fields, the intake-complete handover
        // is no longer rejected.
        BotSession session = ucKIntakeSession();
        session.setFormContext("{\"description\":\"option not appearing any more\"}");
        runLoop.mergePartialIntakeFromContext(session, intakePlan("UC-K"),
                "Chrome on Mac");

        ToolCall completeCall = new ToolCall("request_handover",
                Map.of("escalation_reason", "intake_complete_for_uc_k"));
        assertFalse(dispatcherRejectsIntakeIncomplete(
                intakePlan("UC-K"), completeCall, session),
                "guard must allow the handover once both UC-K fields are present");
    }

    @Test
    void cs066MultiTurn_handoverGuard_rejectsWhenExtractorCannotInferField() {
        // If the user reply does NOT contain a platform token AND the form
        // description is silent, the extractor leaves platform missing —
        // and the existing intake-complete guard MUST still reject a
        // premature intake_complete_for_uc_k handover. Without this
        // negative test the J0 fix could mask the I2 guard.
        BotSession session = ucKIntakeSession();
        session.setFormContext("{\"description\":\"\"}");
        runLoop.mergePartialIntakeFromContext(session, intakePlan("UC-K"), "ok thanks");

        Map<String, String> collected = IntakeFieldsRegistry.parseCollectedFields(
                objectMapper, session.getIntakeFields());
        assertTrue(collected.isEmpty(),
                "no fields should have been extracted from filler reply + blank form");

        ToolCall prematureCall = new ToolCall("request_handover",
                Map.of("escalation_reason", "intake_complete_for_uc_k"));
        assertTrue(dispatcherRejectsIntakeIncomplete(
                intakePlan("UC-K"), prematureCall, session),
                "intake-complete guard MUST still reject handover when extractor "
                        + "cannot infer required fields");
    }

    // -------- helpers --------

    private BotSession ucKIntakeSession() {
        return BotSession.builder()
                .sessionId("test-cs066")
                .activeUseCase("UC-K")
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

    private void wireUcKRegistry() {
        UseCaseRegistryService.UseCaseDefinition ucDef =
                new UseCaseRegistryService.UseCaseDefinition(
                        "UC-K", "Technical Issue Intake",
                        List.of("Technical Support"), "MEDIUM", false, "INTAKE");
        when(useCaseRegistry.getUseCase("UC-K")).thenReturn(ucDef);
        when(toolPolicyEnforcer.getVisibleToolsForUc("UC-K"))
                .thenReturn(List.of("request_handover"));
    }

    /**
     * Sprint 39 — invoke the unified SkillGuardrailDispatcher per the
     * Sprint 37 freeze §8.2.2 migration of Sprint 7 §I2
     * shouldRejectIncompleteIntakeHandover. M1 functional surface
     * preserved bit-for-bit (this file is the M1 protection regression
     * test per Sprint 39 §6 fence #28).
     */
    private boolean dispatcherRejectsIntakeIncomplete(PhasePlan plan,
                                                      ToolCall call,
                                                      BotSession session) {
        SkillGuardrailDispatcher dispatcher = SkillTestFixtures.productionDispatcher();
        DispatchContext ctx = new DispatchContext(
                plan, session, Map.of(), null, Optional.empty());
        Optional<RejectVerdict> verdict = dispatcher.checkBeforeDispatch(plan, call, ctx);
        if (verdict.isEmpty()) return false;
        assertEquals(SkillGuardrailDispatcher.INTAKE_INCOMPLETE_REJECT_REASON,
                verdict.get().predicateName(),
                "intake_complete_required guardrail must use the canonical Sprint 7 §I2 "
                        + "reject-reason label");
        return true;
    }
}
