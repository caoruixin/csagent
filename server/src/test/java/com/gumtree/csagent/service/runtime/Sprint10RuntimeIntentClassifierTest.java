package com.gumtree.csagent.service.runtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gumtree.csagent.model.BotSession;
import com.gumtree.csagent.model.DriftResult;
import com.gumtree.csagent.model.IntentClassification;
import com.gumtree.csagent.model.IntentClassification.IntentRelation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Sprint 10 §L0 — pin the runtime intent classifier on the Sprint 10
 * MVP shapes. Each test exercises one of the six required cases listed
 * in {@code docs/sprint_objective.md} (UC-A → UC-C soft shift, UC-A
 * same issue, UC-A same UC follow-up, UC-A → UC-J risk shift, explicit
 * human request, payment ambiguity negative guard).
 *
 * <p>The classifier is runtime-internal: it never participates in the
 * agent-visible tool surface. The tests here pin the deterministic
 * regex shapes plus the {@link EscalationReasonResolver} integration
 * (distress / explicit-human precedence).
 */
class Sprint10RuntimeIntentClassifierTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final EscalationReasonResolver resolver = new EscalationReasonResolver();
    private final RuntimeIntentClassifier classifier =
            new RuntimeIntentClassifier(resolver, objectMapper);

    private BotSession sessionInPhase(String phase, String activeUc) {
        BotSession s = new BotSession();
        s.setSessionId("sess-l0");
        s.setCurrentPhase(phase);
        s.setActiveUseCase(activeUc);
        s.setFormTopicSubject("Ad Support");
        s.setFormContext("{\"ad_id\":\"AD-1001\",\"description\":\"my ad disappeared\"}");
        return s;
    }

    // ─────────────────────────────────────────────────────────────
    // Sprint 10 MVP shapes (positive)
    // ─────────────────────────────────────────────────────────────

    @Test
    void ucA_confirm_haventGotReplies_softShiftToUcC() {
        BotSession s = sessionInPhase("CONFIRM", "UC-A");
        IntentClassification c = classifier.classify(s,
                "I haven't got replies", DriftResult.builder().type(DriftResult.DriftType.NONE).build());
        assertEquals(IntentRelation.NEW_LOW_RISK_UC, c.relation(),
                "UC-A + 'I haven't got replies' must produce a soft shift relation.");
        assertEquals("UC-C", c.predictedUseCase(),
                "Predicted UC for replies-empty intent must be UC-C.");
    }

    @Test
    void ucA_confirm_stillCantSeeMyAd_sameIssue() {
        BotSession s = sessionInPhase("CONFIRM", "UC-A");
        IntentClassification c = classifier.classify(s,
                "I still can't see my ad", DriftResult.builder().type(DriftResult.DriftType.NONE).build());
        assertEquals(IntentRelation.SAME_ISSUE, c.relation());
        assertEquals("UC-A", c.predictedUseCase());
        assertEquals("listing", c.primaryEntityType(),
                "Primary entity type must be 'listing' when ad_id is in the form context.");
        assertEquals("AD-1001", c.primaryEntityValue(),
                "Primary entity value must be sourced from form_context.ad_id.");
    }

    @Test
    void ucA_confirm_howLongIsItActiveFor_sameUcNewTask() {
        BotSession s = sessionInPhase("CONFIRM", "UC-A");
        IntentClassification c = classifier.classify(s,
                "how long is it active for?", DriftResult.builder().type(DriftResult.DriftType.NONE).build());
        assertEquals(IntentRelation.SAME_UC_NEW_TASK, c.relation());
        assertEquals("UC-A", c.predictedUseCase());
    }

    @Test
    void ucA_iWasScammed_riskShiftToUcJ() {
        BotSession s = sessionInPhase("CONFIRM", "UC-A");
        IntentClassification c = classifier.classify(s,
                "I was scammed", DriftResult.builder().type(DriftResult.DriftType.NONE).build());
        assertEquals(IntentRelation.NEW_HIGH_RISK_UC, c.relation(),
                "Fraud / scam vocabulary must surface as a NEW_HIGH_RISK_UC reroute candidate.");
        assertEquals("UC-J", c.predictedUseCase());
    }

    @Test
    void ucA_iWantAHuman_humanRequest() {
        BotSession s = sessionInPhase("RESOLVE", "UC-A");
        IntentClassification c = classifier.classify(s,
                "I want a human", DriftResult.builder().type(DriftResult.DriftType.NONE).build());
        assertEquals(IntentRelation.HUMAN_REQUEST, c.relation(),
                "Explicit human-help phrasing must be classified as HUMAN_REQUEST so callers " +
                        "can record observability even though the kernel's existing step 2.5 " +
                        "actually performs the escalate.");
    }

    // ─────────────────────────────────────────────────────────────
    // Sprint 10 MVP — payment-ambiguity negative guard
    // ─────────────────────────────────────────────────────────────

    @Test
    void ucA_paidForTopAd_butNotShowing_doesNotBlindlyRouteToUcI() {
        BotSession s = sessionInPhase("CONFIRM", "UC-A");
        IntentClassification c = classifier.classify(s,
                "I paid for Top Ad but it's not showing",
                DriftResult.builder().type(DriftResult.DriftType.NONE).build());
        // Spec: "I paid for Top Ad but it's not showing" must NOT blindly
        // route to UC-I. The classifier keeps UC-A as the predicted UC and
        // the kernel does not switch UC.
        assertNotEquals("UC-I", c.predictedUseCase(),
                "Payment-ambiguity ad-visibility shape must not produce UC-I.");
        assertEquals("UC-A", c.predictedUseCase(),
                "Payment-ambiguity ad-visibility shape must keep UC-A as the predicted UC.");
        assertEquals(IntentRelation.SAME_ISSUE, c.relation(),
                "When the active UC is already UC-A, the relation must be SAME_ISSUE.");
        assertEquals("listing_visibility_paid_promotion", c.taskType(),
                "Task type must record the paid-promotion variant for trace observability.");
    }

    // ─────────────────────────────────────────────────────────────
    // UNKNOWN fallback
    // ─────────────────────────────────────────────────────────────

    @Test
    void blankMessage_returnsUnknown() {
        BotSession s = sessionInPhase("RESOLVE", "UC-A");
        IntentClassification c = classifier.classify(s, "  ",
                DriftResult.builder().type(DriftResult.DriftType.NONE).build());
        assertEquals(IntentRelation.UNKNOWN, c.relation());
        assertNull(c.predictedUseCase());
    }

    @Test
    void unrelatedMessage_returnsUnknown() {
        BotSession s = sessionInPhase("RESOLVE", "UC-A");
        IntentClassification c = classifier.classify(s,
                "okay", DriftResult.builder().type(DriftResult.DriftType.NONE).build());
        assertEquals(IntentRelation.UNKNOWN, c.relation());
    }

    // ─────────────────────────────────────────────────────────────
    // Pattern-level smoke (visible-for-test helpers)
    // ─────────────────────────────────────────────────────────────

    @Test
    void uc_c_replies_pattern_smoke() {
        assertTrue(RuntimeIntentClassifier.matchesUcCNewReplies("I haven't got replies"));
        assertTrue(RuntimeIntentClassifier.matchesUcCNewReplies("no replies"));
        assertTrue(RuntimeIntentClassifier.matchesUcCNewReplies("not getting any replies"));
        assertFalse(RuntimeIntentClassifier.matchesUcCNewReplies("I love replies"));
    }

    @Test
    void uc_a_same_issue_pattern_smoke() {
        assertTrue(RuntimeIntentClassifier.matchesUcASameIssue("I still can't see my ad"));
        assertTrue(RuntimeIntentClassifier.matchesUcASameIssue("my advert is still missing"));
        assertFalse(RuntimeIntentClassifier.matchesUcASameIssue("can't see the menu"));
    }

    @Test
    void uc_a_followup_pattern_smoke() {
        assertTrue(RuntimeIntentClassifier.matchesUcAFollowup(
                "how long is it active for?"));
        assertTrue(RuntimeIntentClassifier.matchesUcAFollowup(
                "how long does my ad last"));
        assertFalse(RuntimeIntentClassifier.matchesUcAFollowup("how do I post an ad"));
    }

    @Test
    void uc_j_risk_shift_pattern_smoke() {
        assertTrue(RuntimeIntentClassifier.matchesUcJRiskShift("I was scammed"));
        assertTrue(RuntimeIntentClassifier.matchesUcJRiskShift("this is a scam"));
        assertTrue(RuntimeIntentClassifier.matchesUcJRiskShift("the seller scammed me"));
        assertFalse(RuntimeIntentClassifier.matchesUcJRiskShift("how does scammer prevention work"));
    }

    @Test
    void payment_ambiguity_pattern_smoke() {
        assertTrue(RuntimeIntentClassifier.matchesPaymentAmbiguityAdVisibility(
                "I paid for Top Ad but it's not showing"));
        assertTrue(RuntimeIntentClassifier.matchesPaymentAmbiguityAdVisibility(
                "I paid for promotion but it isn't showing"));
        assertFalse(RuntimeIntentClassifier.matchesPaymentAmbiguityAdVisibility(
                "I want a refund"));
    }
}
