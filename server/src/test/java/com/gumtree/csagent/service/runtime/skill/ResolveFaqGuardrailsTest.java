package com.gumtree.csagent.service.runtime.skill;

import com.gumtree.csagent.model.BotSession;
import com.gumtree.csagent.model.PhasePlan;
import com.gumtree.csagent.model.ToolCall;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Sprint 39 integration regression for the 3 RESOLVE-FAQ Skill guardrails
 * declared on {@code resolve_faq_grounded_answer.yaml} per Sprint 37 freeze
 * §6.2.6 + §8.2.1 (Sprint 6 §G2 migration) + §8.2.3 (Sprint 11 §M1
 * migration) + §8.2.4 (NEW S1 must_cite_source per M2 §6 #4 verbatim
 * authorization).
 *
 * <p>Asserts target / neighbor / negative coverage for each guardrail type
 * via the unified {@link SkillGuardrailDispatcher} entry points. Skill
 * scope is implicit via {@code applicable_use_cases:
 * [UC-A, UC-B, UC-C, UC-D, UC-E, UC-F, UC-FP]} on the FAQ Skill.
 */
class ResolveFaqGuardrailsTest {

    // R5 — a real Salesforce-style article_id from the KB corpus
    // (data/knowledge/knowledge_base_articles.json). 18 chars, ka-prefixed.
    private static final String REAL_ARTICLE_ID = "ka41r000000LIEEAA4";

    private SkillGuardrailDispatcher dispatcher;

    @BeforeEach
    void setUp() {
        dispatcher = SkillTestFixtures.productionDispatcher();
    }

    private PhasePlan faqPlan(String uc) {
        return PhasePlan.builder().phase("RESOLVE").useCase(uc)
                .objective("x")
                .allowedTools(List.of("search_knowledge", "resolve_article",
                        "record_outcome", "request_handover"))
                .maxToolSteps(4).build();
    }

    private BotSession sess(String uc, String phase) {
        BotSession s = new BotSession();
        s.setSessionId("rfgt-" + uc);
        s.setCurrentPhase(phase);
        s.setActiveUseCase(uc);
        return s;
    }

    private ToolCall handover(String reason) {
        return ToolCall.builder().name("request_handover")
                .arguments(Map.of("escalation_reason", reason)).build();
    }

    private Map<String, Object> viableHits() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("faq_miss", false);
        m.put("hits", List.of(Map.of("source_id", "kb-001", "score", 0.92)));
        return m;
    }

    private DispatchContext ctx(PhasePlan plan, BotSession s, Map<String, Object> acc,
                                 String userMsg) {
        return new DispatchContext(plan, s, acc, userMsg, Optional.ofNullable(userMsg));
    }

    // ---------- faq_miss_handover_requires_resolve_attempt ----------

    @Test
    void s1FaqMiss_target_rejects_when_searchHits_andNoResolve_acrossAllFaqUcs() {
        for (String uc : List.of("UC-A", "UC-B", "UC-C", "UC-D", "UC-E", "UC-F", "UC-FP")) {
            Map<String, Object> acc = new LinkedHashMap<>();
            acc.put("search_knowledge", viableHits());
            Optional<RejectVerdict> verdict = dispatcher.checkBeforeDispatch(
                    faqPlan(uc),
                    handover("faq_miss_threshold_exceeded"),
                    ctx(faqPlan(uc), sess(uc, "RESOLVE"), acc, null));
            assertTrue(verdict.isPresent(), "FAQ guardrail must fire for " + uc);
            assertEquals(SkillGuardrailDispatcher.FAQ_MISS_REJECT_REASON,
                    verdict.get().predicateName(), "wrong predicate label for " + uc);
        }
    }

    @Test
    void s1FaqMiss_neighbor_allows_when_resolveArticleAttempted() {
        Map<String, Object> acc = new LinkedHashMap<>();
        acc.put("search_knowledge", viableHits());
        acc.put("resolve_article", Map.of("article", "x", "source_id", "kb-001"));
        Optional<RejectVerdict> verdict = dispatcher.checkBeforeDispatch(
                faqPlan("UC-A"),
                handover("faq_miss_threshold_exceeded"),
                ctx(faqPlan("UC-A"), sess("UC-A", "RESOLVE"), acc, null));
        assertFalse(verdict.isPresent());
    }

    @Test
    void s1FaqMiss_negative_doesNotFire_for_userRequested() {
        Map<String, Object> acc = new LinkedHashMap<>();
        acc.put("search_knowledge", viableHits());
        assertFalse(dispatcher.checkBeforeDispatch(faqPlan("UC-A"),
                handover("user_requested"),
                ctx(faqPlan("UC-A"), sess("UC-A", "RESOLVE"), acc, null)).isPresent());
    }

    // ---------- premature_resolve_outcome_guard ----------

    @Test
    void prematureResolve_target_rejects_resolveInResolvePhase() {
        Optional<RejectVerdict> verdict = dispatcher.checkBeforeOutcomePersist(
                faqPlan("UC-A"), "resolve",
                ctx(faqPlan("UC-A"), sess("UC-A", "RESOLVE"), Map.of(),
                        "Your ad is active."));
        assertTrue(verdict.isPresent());
        assertEquals(SkillGuardrailDispatcher.PROGRESSIVE_RESOLVE_REJECT_REASON,
                verdict.get().predicateName());
    }

    @Test
    void prematureResolve_neighbor_allows_resolveInClosePhase() {
        // CLOSE phase passes ResolveDispositionEvaluator's guard. But the
        // 3rd guardrail must_cite_source still fires on this Skill if the
        // user_message has no acceptable citation token (R5 structural shape).
        Optional<RejectVerdict> verdict = dispatcher.checkBeforeOutcomePersist(
                faqPlan("UC-A"), "resolve",
                ctx(faqPlan("UC-A"), sess("UC-A", "CLOSE"), Map.of(),
                        "Your ad is active [source_id: " + REAL_ARTICLE_ID + "]."));
        assertFalse(verdict.isPresent(),
                "CLOSE phase with citation present must pass all 3 guardrails");
    }

    @Test
    void prematureResolve_negative_doesNotFire_for_escalate() {
        // escalate outcome class — neither premature_resolve nor must_cite_source fires.
        assertFalse(dispatcher.checkBeforeOutcomePersist(
                faqPlan("UC-A"), "escalate",
                ctx(faqPlan("UC-A"), sess("UC-A", "RESOLVE"), Map.of(),
                        "Connecting to a specialist.")).isPresent());
    }

    // ---------- must_cite_source (NEW S1 per M2 §6 #4 verbatim) ----------

    @Test
    void mustCiteSource_target_rejects_recordOutcomeResolve_withoutSourceId() {
        // Use CONFIRM phase to pass premature_resolve_outcome_guard; then
        // must_cite_source fires because user_message has no source_id token.
        Optional<RejectVerdict> verdict = dispatcher.checkBeforeOutcomePersist(
                faqPlan("UC-A"), "resolve",
                ctx(faqPlan("UC-A"), sess("UC-A", "CONFIRM"), Map.of(),
                        "Your ad is active for 30 days."));
        assertTrue(verdict.isPresent());
        assertEquals(SkillGuardrailDispatcher.S1_CITATION_PRESENCE_REQUIRED,
                verdict.get().predicateName());
    }

    @Test
    void mustCiteSource_neighbor_passes_withSourceIdInUserMessage() {
        // R5 — a structural article_id token (display_citation fallback) passes.
        Optional<RejectVerdict> verdict = dispatcher.checkBeforeOutcomePersist(
                faqPlan("UC-A"), "resolve",
                ctx(faqPlan("UC-A"), sess("UC-A", "CONFIRM"), Map.of(),
                        "Your ad is active for 30 days [source_id: " + REAL_ARTICLE_ID + "]."));
        assertFalse(verdict.isPresent());
    }

    @Test
    void mustCiteSource_negative_doesNotFire_for_classEscalate() {
        // Bounded per M2 §6 #4: S1 ONLY fires on class=resolve. Other outcome
        // classes pass through regardless of citation presence.
        assertFalse(dispatcher.checkBeforeOutcomePersist(
                faqPlan("UC-A"), "escalate",
                ctx(faqPlan("UC-A"), sess("UC-A", "CONFIRM"), Map.of(),
                        "Connecting you to a human, no citation here.")).isPresent());
    }

    @Test
    void mustCiteSource_negative_doesNotFire_for_classAbandon() {
        // class=abandon is OUT OF S1 SCOPE per M2 §6 #4 verbatim authorization.
        assertFalse(dispatcher.checkBeforeOutcomePersist(
                faqPlan("UC-A"), "abandon",
                ctx(faqPlan("UC-A"), sess("UC-A", "CONFIRM"), Map.of(),
                        "Conversation closed, no citation here.")).isPresent());
    }

    @Test
    void mustCiteSource_negative_doesNotFire_outside_resolveFaqSkill_scope() {
        // INTAKE-path UCs route to a different Skill (without must_cite_source).
        PhasePlan intakePlan = PhasePlan.builder().phase("RESOLVE").useCase("UC-K")
                .objective("intake")
                .allowedTools(List.of("request_handover")).maxToolSteps(3).build();
        // INTAKE Skill's premature_resolve is NOT declared either, so the
        // dispatcher returns empty regardless of outcome class / message.
        assertFalse(dispatcher.checkBeforeOutcomePersist(
                intakePlan, "resolve",
                ctx(intakePlan, sess("UC-K", "RESOLVE"), Map.of(),
                        "Your ad is being escalated.")).isPresent());
    }
}
