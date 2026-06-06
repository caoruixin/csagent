package com.gumtree.csagent.service.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gumtree.csagent.model.BotSession;
import com.gumtree.csagent.model.PhasePlan;
import com.gumtree.csagent.model.ToolCall;
import com.gumtree.csagent.service.runtime.IntakeFieldsRegistry;
import com.gumtree.csagent.service.runtime.skill.DispatchContext;
import com.gumtree.csagent.service.runtime.skill.RejectVerdict;
import com.gumtree.csagent.service.runtime.skill.SkillGuardrailDispatcher;
import com.gumtree.csagent.service.runtime.skill.SkillTestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Sprint 080 / S-Auto-25 / M-Auto-6 Sub-sprint C-1 (R7) — unit + anti-误杀
 * coverage for the no-side-effect {@code update_intake_fields} tool.
 *
 * <p>The validator non-bypass tests use the REAL production
 * {@link SkillGuardrailDispatcher} (via
 * {@link SkillTestFixtures#productionDispatcher()}) so they exercise the same
 * {@code intake_complete_required} guardrail the run loop uses: the tool and
 * the validator share {@code session.intakeFields}, so the tool can only make
 * intake accumulation VISIBLE to the validator — it can never make an
 * incomplete intake pass.
 */
class UpdateIntakeFieldsToolTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private UpdateIntakeFieldsTool tool;
    private SkillGuardrailDispatcher dispatcher;

    @BeforeEach
    void setUp() {
        tool = new UpdateIntakeFieldsTool(objectMapper);
        dispatcher = SkillTestFixtures.productionDispatcher();
    }

    // -------------------- helpers --------------------

    private BotSession intakeSession(String uc, String intakeFieldsJson) {
        BotSession s = new BotSession();
        s.setSessionId("uif-" + uc);
        s.setCurrentPhase("RESOLVE");
        s.setActiveUseCase(uc);
        s.setIntakeFields(intakeFieldsJson);
        return s;
    }

    private Map<String, Object> fieldsParam(Object fieldsValue) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("fields", fieldsValue);
        return p;
    }

    private Map<String, String> collected(BotSession s) {
        return IntakeFieldsRegistry.parseCollectedFields(objectMapper, s.getIntakeFields());
    }

    private PhasePlan intakePlan(String uc) {
        return PhasePlan.builder().phase("RESOLVE").useCase(uc)
                .objective("intake")
                .allowedTools(List.of("request_handover", "update_intake_fields"))
                .maxToolSteps(3).build();
    }

    private ToolCall handover(String reason) {
        return ToolCall.builder().name("request_handover")
                .arguments(Map.of("escalation_reason", reason)).build();
    }

    private DispatchContext ctx(PhasePlan plan, BotSession s) {
        return new DispatchContext(plan, s, Map.of(), null, Optional.empty());
    }

    @SuppressWarnings("unchecked")
    private List<String> persistedFields(ToolResult result) {
        return (List<String>) result.getData().get("fields_persisted");
    }

    // -------------------- R7 positive --------------------

    @Test
    void persistsSingleField_forUcJ() {
        BotSession session = intakeSession("UC-J", null);

        ToolResult result = tool.execute(session, fieldsParam(Map.of("report_type", "scam")));

        assertTrue(result.isSuccess());
        assertEquals("ok", result.getData().get("status"));
        assertEquals(1, result.getData().get("fields_merged"));
        assertEquals(List.of("report_type"), persistedFields(result));
        assertEquals("scam", collected(session).get("report_type"));
    }

    @Test
    void multiCallAccumulation_unionsFields() {
        // c14 shape: the bot collects one field per turn via separate calls.
        BotSession session = intakeSession("UC-J", null);

        tool.execute(session, fieldsParam(Map.of("report_target", "AD-7")));
        tool.execute(session, fieldsParam(Map.of("report_type", "scam")));

        Map<String, String> collected = collected(session);
        assertEquals("AD-7", collected.get("report_target"));
        assertEquals("scam", collected.get("report_type"));
    }

    @Test
    void aliasFieldName_canonicalisedOnPersist() {
        // "type" is an alias for the canonical report_type (IntakeFieldsRegistry).
        BotSession session = intakeSession("UC-J", null);

        ToolResult result = tool.execute(session, fieldsParam(Map.of("type", "scam")));

        assertTrue(result.isSuccess());
        assertEquals("scam", collected(session).get("report_type"));
        assertEquals(List.of("report_type"), persistedFields(result));
    }

    // -------------------- R7 anti-误杀 #1: validator NON-bypass --------------------

    @Test
    void validatorStillRejects_afterPartialUpdate() {
        // UC-J requires report_target + report_type + description. Persist only
        // ONE via update_intake_fields, then attempt a complete-handover. The
        // real intake_complete_required guardrail MUST still reject — R7 does
        // not bypass the validator.
        BotSession session = intakeSession("UC-J", null);
        tool.execute(session, fieldsParam(Map.of("report_type", "scam")));

        Optional<RejectVerdict> verdict = dispatcher.checkBeforeDispatch(
                intakePlan("UC-J"), handover("intake_complete_for_uc_j"),
                ctx(intakePlan("UC-J"), session));

        assertTrue(verdict.isPresent(),
                "handover with partial intake must still be rejected after update_intake_fields");
        assertEquals(SkillGuardrailDispatcher.INTAKE_INCOMPLETE_REJECT_REASON,
                verdict.get().predicateName());
    }

    // -------------------- R7 anti-误杀 #2: full stash -> handover passes --------------------

    @Test
    void validatorPasses_afterFullStashViaTool() {
        // Persist ALL required UC-J fields via update_intake_fields, then the
        // complete-handover passes the validator — proving the tool's persist
        // semantics are equivalent to the request_handover intake_fields path.
        BotSession session = intakeSession("UC-J", null);
        Map<String, Object> all = new LinkedHashMap<>();
        all.put("report_target", "AD-7");
        all.put("report_type", "scam");
        all.put("description", "seller is sending phishing links");
        tool.execute(session, fieldsParam(all));

        Optional<RejectVerdict> verdict = dispatcher.checkBeforeDispatch(
                intakePlan("UC-J"), handover("intake_complete_for_uc_j"),
                ctx(intakePlan("UC-J"), session));

        assertFalse(verdict.isPresent(),
                "handover must pass once all required fields are stashed via the tool");
        assertTrue(IntakeFieldsRegistry.intakeComplete("UC-J", collected(session)));
    }

    // -------------------- R7 anti-误杀 #3: empty fields rejected --------------------

    @Test
    void emptyFields_rejectedAtDispatch_sessionUnchanged() {
        BotSession session = intakeSession("UC-J", "{\"report_type\":\"scam\"}");
        String before = session.getIntakeFields();

        ToolResult result = tool.execute(session, fieldsParam(Map.of()));

        assertFalse(result.isSuccess());
        assertNotNull(result.getErrorMessage());
        assertEquals(before, session.getIntakeFields(), "session.intakeFields must be unchanged");
    }

    // -------------------- R7 anti-误杀 #4: wrong type rejected --------------------

    @Test
    void wrongTypeFields_rejected_sessionUnchanged() {
        BotSession session = intakeSession("UC-J", "{\"report_type\":\"scam\"}");
        String before = session.getIntakeFields();

        ToolResult result = tool.execute(session, fieldsParam("not-a-map"));

        assertFalse(result.isSuccess());
        assertEquals(before, session.getIntakeFields());
    }

    // -------------------- R7 anti-误杀 #5: no auto-derivation --------------------

    @Test
    void noAutoDerivation_onlyActsOnFieldsArg() {
        // The tool MUST NOT derive intake fields from any other channel
        // (user_message, accumulated_tool_results, form_context, ...). Given a
        // parameters map WITHOUT a `fields` key — even one carrying message-like
        // content — the tool rejects and persists nothing.
        BotSession session = intakeSession("UC-J", null);
        Map<String, Object> noFields = new LinkedHashMap<>();
        noFields.put("user_message", "I want to report a scammer, target is AD-7, type scam");
        noFields.put("accumulated_tool_results", Map.of("search_knowledge", Map.of("faq_miss", true)));

        ToolResult result = tool.execute(session, noFields);

        assertFalse(result.isSuccess());
        assertTrue(collected(session).isEmpty(),
                "no field may be derived from non-`fields` channels");
    }

    @Test
    void nullParameters_rejected() {
        BotSession session = intakeSession("UC-J", null);
        ToolResult result = tool.execute(session, null);
        assertFalse(result.isSuccess());
        assertTrue(collected(session).isEmpty());
    }

    // -------------------- content normalisation edge --------------------

    @Test
    void blankValueOnly_succeedsWithZeroMerged_sessionUnchanged() {
        // Structurally non-empty map, but the only value is blank -> the merge
        // drops it; the tool reports 0 merged and leaves the session untouched.
        BotSession session = intakeSession("UC-J", "{\"report_type\":\"scam\"}");
        String before = session.getIntakeFields();
        Map<String, Object> blank = new LinkedHashMap<>();
        blank.put("description", "");

        ToolResult result = tool.execute(session, fieldsParam(blank));

        assertTrue(result.isSuccess());
        assertEquals(0, result.getData().get("fields_merged"));
        assertEquals(before, session.getIntakeFields());
    }
}
