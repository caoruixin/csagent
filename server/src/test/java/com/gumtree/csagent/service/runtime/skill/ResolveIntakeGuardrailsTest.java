package com.gumtree.csagent.service.runtime.skill;

import com.gumtree.csagent.model.BotSession;
import com.gumtree.csagent.model.PhasePlan;
import com.gumtree.csagent.model.ToolCall;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Sprint 39 integration regression for the {@code intake_complete_required}
 * Skill guardrail declared on
 * {@code resolve_intake_collect_and_handover.yaml} per Sprint 37 freeze
 * §6.2.5 + §8.2.2 (S2 = the existing Sprint 7 §I2 predicate migrated to
 * declarative form; same semantic, new invocation surface).
 *
 * <p>Asserts target / neighbor / negative coverage across the 5 INTAKE-path
 * UCs (UC-G/H/I/J/K) via the unified {@link SkillGuardrailDispatcher}.
 */
class ResolveIntakeGuardrailsTest {

    private SkillGuardrailDispatcher dispatcher;

    @BeforeEach
    void setUp() {
        dispatcher = SkillTestFixtures.productionDispatcher();
    }

    private PhasePlan intakePlan(String uc) {
        return PhasePlan.builder().phase("RESOLVE").useCase(uc)
                .objective("intake")
                .allowedTools(List.of("request_handover"))
                .maxToolSteps(3).build();
    }

    private BotSession intakeSession(String uc, String intakeFieldsJson) {
        BotSession s = new BotSession();
        s.setSessionId("rit-" + uc);
        s.setCurrentPhase("RESOLVE");
        s.setActiveUseCase(uc);
        s.setIntakeFields(intakeFieldsJson);
        return s;
    }

    private ToolCall handover(String reason) {
        return ToolCall.builder().name("request_handover")
                .arguments(Map.of("escalation_reason", reason)).build();
    }

    private DispatchContext ctx(PhasePlan plan, BotSession s) {
        return new DispatchContext(plan, s, Map.of(), null, Optional.empty());
    }

    // ---------- target: intake_complete rejected when fields missing ----------

    @Test
    void target_rejectsHandover_for_ucG_whenAllFieldsMissing() {
        Optional<RejectVerdict> verdict = dispatcher.checkBeforeDispatch(
                intakePlan("UC-G"), handover("intake_complete_for_uc_g"),
                ctx(intakePlan("UC-G"), intakeSession("UC-G", null)));
        assertTrue(verdict.isPresent());
        assertEquals(SkillGuardrailDispatcher.INTAKE_INCOMPLETE_REJECT_REASON,
                verdict.get().predicateName());
        assertEquals("resolve_intake_collect_and_handover",
                verdict.get().trace().get("skill_name"));
    }

    @Test
    void target_rejectsHandover_for_ucH_whenStatedReasonStillMissing() {
        BotSession s = intakeSession("UC-H",
                "{\"ad_id_or_listing_url\":\"AD-2001\","
                        + "\"registered_email\":\"alice@example.com\"}");
        Optional<RejectVerdict> verdict = dispatcher.checkBeforeDispatch(
                intakePlan("UC-H"), handover("intake_complete_for_uc_h"),
                ctx(intakePlan("UC-H"), s));
        assertTrue(verdict.isPresent(),
                "guard must still reject UC-H when stated_reason_or_context is missing");
    }

    @Test
    void target_rejectsHandover_for_ucI_whenAllFieldsMissing() {
        Optional<RejectVerdict> verdict = dispatcher.checkBeforeDispatch(
                intakePlan("UC-I"), handover("intake_complete_for_uc_i"),
                ctx(intakePlan("UC-I"), intakeSession("UC-I", null)));
        assertTrue(verdict.isPresent());
    }

    @Test
    void target_rejectsHandover_for_ucJ_whenAllFieldsMissing() {
        Optional<RejectVerdict> verdict = dispatcher.checkBeforeDispatch(
                intakePlan("UC-J"), handover("intake_complete_for_uc_j"),
                ctx(intakePlan("UC-J"), intakeSession("UC-J", null)));
        assertTrue(verdict.isPresent());
    }

    @Test
    void target_rejectsHandover_for_ucK_whenAllFieldsMissing() {
        Optional<RejectVerdict> verdict = dispatcher.checkBeforeDispatch(
                intakePlan("UC-K"), handover("intake_complete_for_uc_k"),
                ctx(intakePlan("UC-K"), intakeSession("UC-K", null)));
        assertTrue(verdict.isPresent());
    }

    // ---------- neighbor (positive): handover allowed when fields complete ----------

    @Test
    void neighbor_allowsHandover_for_ucK_whenBothFieldsPresent() {
        BotSession s = intakeSession("UC-K",
                "{\"platform\":\"Chrome on Mac\","
                        + "\"repro_steps_or_error_message\":\"button missing\"}");
        assertFalse(dispatcher.checkBeforeDispatch(intakePlan("UC-K"),
                handover("intake_complete_for_uc_k"), ctx(intakePlan("UC-K"), s)).isPresent());
    }

    @Test
    void neighbor_allowsHandover_for_ucH_whenAllThreeFieldsPresent() {
        BotSession s = intakeSession("UC-H",
                "{\"ad_id_or_listing_url\":\"AD-2001\","
                        + "\"registered_email\":\"alice@example.com\","
                        + "\"stated_reason_or_context\":\"a removal mistake\"}");
        assertFalse(dispatcher.checkBeforeDispatch(intakePlan("UC-H"),
                handover("intake_complete_for_uc_h"), ctx(intakePlan("UC-H"), s)).isPresent());
    }

    // ---------- negative: non-matching reasons pass through ----------

    @Test
    void negative_userRequestedPassesThrough_regardlessOfFields() {
        assertFalse(dispatcher.checkBeforeDispatch(intakePlan("UC-K"),
                handover("user_requested"),
                ctx(intakePlan("UC-K"), intakeSession("UC-K", null))).isPresent(),
                "user_requested handover must always pass — Sprint 6 §G1 / Alice "
                        + "closure-criterion (c) preserved");
    }

    @Test
    void negative_incompleteIntakePassesThrough_regardlessOfFields() {
        // The "incomplete_intake" reason is the canonical fallback for the
        // bot to honestly escalate when intake cannot be completed — it MUST
        // pass through the guardrail.
        assertFalse(dispatcher.checkBeforeDispatch(intakePlan("UC-K"),
                handover("incomplete_intake"),
                ctx(intakePlan("UC-K"), intakeSession("UC-K", null))).isPresent());
    }

    @Test
    void negative_doesNotFire_for_faqPathUc_whenIntakeReason_passedIn() {
        // Skill scope: UC-A routes to resolve_faq_grounded_answer which does
        // NOT declare intake_complete_required; the dispatcher does not fire
        // the handler regardless of the request_handover reason.
        PhasePlan faqPlan = PhasePlan.builder().phase("RESOLVE").useCase("UC-A")
                .objective("x")
                .allowedTools(List.of("search_knowledge", "request_handover"))
                .maxToolSteps(4).build();
        BotSession s = new BotSession();
        s.setSessionId("rit-faq");
        s.setCurrentPhase("RESOLVE");
        s.setActiveUseCase("UC-A");
        // The dispatcher walks the FAQ Skill's guardrails; intake_complete_required
        // is not in that list, so no rejection on this dimension.
        // (faq_miss_handover_requires_resolve_attempt also doesn't fire because
        // the reason is "intake_complete_for_uc_k", not "faq_miss_threshold_exceeded".)
        assertFalse(dispatcher.checkBeforeDispatch(faqPlan,
                handover("intake_complete_for_uc_k"), ctx(faqPlan, s)).isPresent());
    }

    @Test
    void negative_doesNotFire_for_unrelatedReason() {
        // request_handover with "out_of_scope" doesn't match the intake_complete_*
        // glob pattern → handler returns empty.
        assertFalse(dispatcher.checkBeforeDispatch(intakePlan("UC-K"),
                handover("out_of_scope"),
                ctx(intakePlan("UC-K"), intakeSession("UC-K", null))).isPresent());
    }
}
