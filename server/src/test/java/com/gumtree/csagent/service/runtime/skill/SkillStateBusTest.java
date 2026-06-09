package com.gumtree.csagent.service.runtime.skill;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gumtree.csagent.model.BotSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Sprint 41 unit tests for {@link SkillStateBus} per Sprint 37 freeze
 * decision (i) §10.3-§10.5 + Sprint 41 D-f.
 *
 * <p>Covers:
 * <ul>
 *   <li>{@code inherit} dimensions: customer_context + accumulated_tool_results
 *       pass through unchanged; intake_fields_partial applies the
 *       registry-driven intersection rule (§10.3 + OLD Sprint 36 D5 §6.1.C).</li>
 *   <li>{@code reset} dimensions: customer_context cleared on session;
 *       intake_fields_partial cleared on session.</li>
 *   <li>{@code soft_signal_via_projection} dimensions: no-op at bus level
 *       (the projection builder emits slots unconditionally per §N0).</li>
 *   <li>No-op safety: same-Skill turn, null inputs, missing
 *       state_inheritance, EMPTY declaration.</li>
 *   <li>{@link SkillStateBus#inheritanceFor(Skill)} diagnostic accessor.</li>
 * </ul>
 */
class SkillStateBusTest {

    private SkillStateBus bus;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        bus = new SkillStateBus(objectMapper);
    }

    // -------------------- helpers --------------------

    private Skill skillWith(String name, StateInheritance si, List<String> applicableUseCases) {
        return new Skill(
                name, "test " + name,
                List.of("RESOLVE"),
                applicableUseCases,
                List.of(),
                List.of(),
                2,
                false,
                List.of("FINAL_ANSWER"),
                "obj", "proc", "ground", "esc",
                List.of(),
                si);
    }

    private Skill discoverSkill() {
        return skillWith("discover_triage",
                new StateInheritance(
                        List.of("customer_context", "accumulated_tool_results"),
                        List.of("intake_fields_partial"),
                        List.of("alternate_candidate_use_cases", "discover_disambiguation_signals")),
                List.of("*"));
    }

    private Skill confirmSkill() {
        return skillWith("confirm",
                new StateInheritance(
                        List.of("customer_context", "accumulated_tool_results", "intake_fields_partial"),
                        List.of(),
                        List.of()),
                List.of("*"));
    }

    private Skill resolveFaqSkill() {
        return skillWith("resolve_faq_grounded_answer",
                new StateInheritance(
                        List.of("customer_context", "accumulated_tool_results"),
                        List.of("intake_fields_partial"),
                        List.of("prior_use_case_carry")),
                List.of("UC-A", "UC-B", "UC-C", "UC-D", "UC-E", "UC-F", "UC-FP"));
    }

    private Skill resolveIntakeSkill() {
        return skillWith("resolve_intake_collect_and_handover",
                new StateInheritance(
                        List.of("customer_context", "accumulated_tool_results", "intake_fields_partial"),
                        List.of(),
                        List.of("prior_use_case_carry")),
                List.of("UC-G", "UC-H", "UC-I", "UC-J", "UC-K"));
    }

    private BotSession sessionWith(String activeUc, String customerContextJson,
                                    String intakeFieldsJson) {
        BotSession s = new BotSession();
        s.setSessionId("sess-bus-test");
        s.setActiveUseCase(activeUc);
        s.setCustomerContext(customerContextJson);
        s.setIntakeFields(intakeFieldsJson);
        return s;
    }

    private String json(Map<String, String> kv) throws Exception {
        return objectMapper.writeValueAsString(kv);
    }

    // -------------------- reset dimension --------------------

    @Test
    void resetIntakeFields_clearsSessionField() {
        BotSession s = sessionWith("UC-A", "{\"email\":\"a@b.com\"}", "{\"ad_id_or_listing_url\":\"123\"}");
        bus.applyOnSkillSwitch(resolveIntakeSkill(), resolveFaqSkill(), s);
        assertNull(s.getIntakeFields());
    }

    @Test
    void resetIntakeFields_handlesAlreadyNull() {
        BotSession s = sessionWith("UC-A", null, null);
        bus.applyOnSkillSwitch(resolveIntakeSkill(), resolveFaqSkill(), s);
        assertNull(s.getIntakeFields());
    }

    @Test
    void resetCustomerContext_isHonoredWhenDeclared() {
        Skill custReset = skillWith("custom_reset",
                new StateInheritance(List.of(), List.of("customer_context"), List.of()),
                List.of("*"));
        BotSession s = sessionWith("UC-A", "{\"x\":1}", null);
        bus.applyOnSkillSwitch(discoverSkill(), custReset, s);
        assertNull(s.getCustomerContext());
    }

    // -------------------- inherit dimension (intake_fields_partial intersection) --------------------

    @Test
    void inheritIntakeFields_preservesRequiredFieldsForNewUc() throws Exception {
        // resolve_intake_collect_and_handover inherits intake_fields_partial.
        // Switching INTO it with active UC = UC-H, the bus must preserve
        // fields whose canonical name is in IntakeFieldsRegistry.requiredFieldsFor("UC-H").
        // UC-H required: ad_id_or_listing_url, registered_email, stated_reason_or_context.
        Map<String, String> existing = Map.of(
                "ad_id_or_listing_url", "ad-7",
                "registered_email", "a@b.com",
                "stated_reason_or_context", "policy"
        );
        BotSession s = sessionWith("UC-H", null, json(existing));
        bus.applyOnSkillSwitch(discoverSkill(), resolveIntakeSkill(), s);
        assertNotNull(s.getIntakeFields());
        Map<String, Object> survived = objectMapper.readValue(
                s.getIntakeFields(), Map.class);
        assertEquals("ad-7", survived.get("ad_id_or_listing_url"));
        assertEquals("a@b.com", survived.get("registered_email"));
        assertEquals("policy", survived.get("stated_reason_or_context"));
    }

    @Test
    void inheritIntakeFields_dropsNonRequiredFieldsForNewUc() throws Exception {
        // UC-K required: platform, repro_steps_or_error_message.
        // Pre-existing fields include UC-H's ad_id_or_listing_url (not in
        // UC-K's required set) — must be dropped by the intersection.
        Map<String, String> existing = Map.of(
                "platform", "ios",
                "ad_id_or_listing_url", "ad-3"
        );
        BotSession s = sessionWith("UC-K", null, json(existing));
        bus.applyOnSkillSwitch(discoverSkill(), resolveIntakeSkill(), s);
        Map<String, Object> survived = objectMapper.readValue(
                s.getIntakeFields(), Map.class);
        assertEquals("ios", survived.get("platform"));
        assertFalse(survived.containsKey("ad_id_or_listing_url"));
    }

    @Test
    void inheritIntakeFields_dropsAllWhenNewUcIsNotIntake() throws Exception {
        // UC-A is FAQ-path: requiredFieldsFor returns []. Even though the
        // Skill declares inherit:[intake_fields_partial], every field drops.
        Map<String, String> existing = Map.of("ad_id_or_listing_url", "ad-7");
        // hypothetical Skill: inherits intake_fields_partial AND is bound to UC-A
        Skill ucaInheritIntake = skillWith("custom_uc_a_inheritor",
                new StateInheritance(List.of("intake_fields_partial"), List.of(), List.of()),
                List.of("UC-A"));
        BotSession s = sessionWith("UC-A", null, json(existing));
        bus.applyOnSkillSwitch(discoverSkill(), ucaInheritIntake, s);
        assertNull(s.getIntakeFields());
    }

    @Test
    void inheritIntakeFields_canonicalisesAliasNames() throws Exception {
        // The registry canonicalises "ad_id" -> "ad_id_or_listing_url".
        // Even when the JSON keys are the alias form, the intersection
        // must still match the canonical required-field name and preserve.
        Map<String, String> existing = Map.of("ad_id", "ad-9");
        BotSession s = sessionWith("UC-H", null, json(existing));
        bus.applyOnSkillSwitch(discoverSkill(), resolveIntakeSkill(), s);
        assertNotNull(s.getIntakeFields());
        Map<String, Object> survived = objectMapper.readValue(
                s.getIntakeFields(), Map.class);
        assertEquals("ad-9", survived.get("ad_id_or_listing_url"));
    }

    @Test
    void inheritCustomerContext_passesThrough() {
        BotSession s = sessionWith("UC-A", "{\"name\":\"alice\"}", null);
        bus.applyOnSkillSwitch(discoverSkill(), resolveFaqSkill(), s);
        assertEquals("{\"name\":\"alice\"}", s.getCustomerContext());
    }

    @Test
    void inheritAccumulatedToolResults_isNoOpOnSession() {
        // accumulated_tool_results has no persistent session field.
        // The bus' inherit handling for it is a documented no-op; the
        // session's other persistent fields remain unchanged.
        BotSession s = sessionWith("UC-A", "{\"x\":1}", null);
        bus.applyOnSkillSwitch(discoverSkill(), resolveFaqSkill(), s);
        assertEquals("{\"x\":1}", s.getCustomerContext());
        // intake_fields_partial is reset by resolveFaqSkill — that's
        // documented in the resolveFaq reset list, not by accumulated_tool_results.
        assertNull(s.getIntakeFields());
    }

    // -------------------- soft_signal_via_projection dimension --------------------

    @Test
    void softSignalProjection_isNoOpOnSession() {
        // resolve_faq_grounded_answer declares soft_signal_via_projection:
        // [prior_use_case_carry]. The bus is a no-op for this dimension
        // (the projection builder emits slots unconditionally).
        BotSession s = sessionWith("UC-A", "{\"x\":1}", null);
        s.setIntakeAmbiguousCandidates(new String[]{"UC-B"});
        bus.applyOnSkillSwitch(discoverSkill(), resolveFaqSkill(), s);
        // Soft-signal handling did not mutate any session field.
        assertEquals("{\"x\":1}", s.getCustomerContext());
        assertNotNull(s.getIntakeAmbiguousCandidates());
        assertEquals("UC-B", s.getIntakeAmbiguousCandidates()[0]);
    }

    // -------------------- no-op safety --------------------

    @Test
    void sameSkill_isNoOp() {
        Skill s1 = resolveFaqSkill();
        BotSession sess = sessionWith("UC-A", "{\"x\":1}",
                "{\"ad_id_or_listing_url\":\"keep-me\"}");
        bus.applyOnSkillSwitch(s1, s1, sess);
        // Same-name no-op: intake_fields_partial NOT cleared (despite
        // resolveFaq's reset list) because the bus short-circuits before
        // applying anything.
        assertEquals("{\"ad_id_or_listing_url\":\"keep-me\"}", sess.getIntakeFields());
        assertEquals("{\"x\":1}", sess.getCustomerContext());
    }

    @Test
    void nullNewSkill_isNoOp() {
        BotSession s = sessionWith("UC-A", "{\"x\":1}", "{}");
        bus.applyOnSkillSwitch(discoverSkill(), null, s);
        assertEquals("{\"x\":1}", s.getCustomerContext());
    }

    @Test
    void nullSession_isNoOp() {
        // No exception thrown; trivially passes.
        bus.applyOnSkillSwitch(discoverSkill(), resolveFaqSkill(), null);
    }

    @Test
    void nullPriorSkill_appliesNewSkillInheritance() {
        // A fresh session with no prior Skill should still apply the new
        // Skill's state_inheritance (in particular, reset dimensions).
        BotSession s = sessionWith("UC-A", null, "{\"ad_id\":\"x\"}");
        bus.applyOnSkillSwitch(null, resolveFaqSkill(), s);
        assertNull(s.getIntakeFields());
    }

    @Test
    void emptyInheritance_isNoOp() {
        Skill emptyDecl = skillWith("custom_empty",
                StateInheritance.EMPTY, List.of("*"));
        BotSession s = sessionWith("UC-A", "{\"a\":1}", "{\"b\":2}");
        bus.applyOnSkillSwitch(discoverSkill(), emptyDecl, s);
        assertEquals("{\"a\":1}", s.getCustomerContext());
        assertEquals("{\"b\":2}", s.getIntakeFields());
    }

    // -------------------- diagnostic accessor --------------------

    @Test
    void inheritanceFor_returnsSkillDeclaration() {
        StateInheritance si = bus.inheritanceFor(resolveIntakeSkill());
        assertEquals(List.of("customer_context", "accumulated_tool_results", "intake_fields_partial"),
                si.inherit());
        assertTrue(si.reset().isEmpty());
        assertEquals(List.of("prior_use_case_carry"), si.softSignalViaProjection());
    }

    @Test
    void inheritanceFor_handlesNullSkill() {
        assertSame(StateInheritance.EMPTY, bus.inheritanceFor(null));
    }
}
