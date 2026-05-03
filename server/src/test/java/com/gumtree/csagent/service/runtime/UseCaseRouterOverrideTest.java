package com.gumtree.csagent.service.runtime;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Codex 2026-05-03 round 3: phase2 §2.11.4 + fixed_script_library_v1 §9.1
 * require Description-based UC overrides for handover-only Topic Subjects
 * (Delivery + scam → UC-J, Delivery + refund → UC-I, Ratings Reviews +
 * tech failure → UC-K, etc.). The router used to fall straight through to
 * OUT_OF_SCOPE_*, mis-routing cs_036 (Delivery + "Refund delivery") to
 * terminal hard-OOS. These tests pin the override precedence.
 */
class UseCaseRouterOverrideTest {

    @Test
    void delivery_refund_routesToPaymentDispute() {
        Optional<String> uc = UseCaseRouter.matchHandoverOnlyOverride(
                "Delivery", "I want a refund for the delivery");
        assertTrue(uc.isPresent());
        assertEquals("UC-I", uc.get());
    }

    @Test
    void delivery_scam_routesToTrustAndSafety() {
        Optional<String> uc = UseCaseRouter.matchHandoverOnlyOverride(
                "Delivery", "The seller scammed me out of £40");
        assertTrue(uc.isPresent());
        assertEquals("UC-J", uc.get());
    }

    @Test
    void delivery_plainCourierIssue_keepsTerminalOos() {
        Optional<String> uc = UseCaseRouter.matchHandoverOnlyOverride(
                "Delivery", "My parcel is late and the courier is not responding");
        assertFalse(uc.isPresent());
    }

    @Test
    void ratingsReviews_techFailure_routesToTechIntake() {
        Optional<String> uc = UseCaseRouter.matchHandoverOnlyOverride(
                "Ratings Reviews", "I can't leave a review, the page errors out");
        assertTrue(uc.isPresent());
        assertEquals("UC-K", uc.get());
    }

    @Test
    void ratingsReviews_fraud_routesToTrustAndSafety() {
        Optional<String> uc = UseCaseRouter.matchHandoverOnlyOverride(
                "Ratings Reviews", "Buyer left a fraudulent review threatening me");
        assertTrue(uc.isPresent());
        assertEquals("UC-J", uc.get());
    }

    @Test
    void proContract_hasNoOverride_keepsTerminalOos() {
        Optional<String> uc = UseCaseRouter.matchHandoverOnlyOverride(
                "Pro Contract", "I want a refund on my pro subscription");
        assertFalse(uc.isPresent());
    }

    @Test
    void accountManagerSupport_hasNoOverride_keepsTerminalOos() {
        Optional<String> uc = UseCaseRouter.matchHandoverOnlyOverride(
                "Account Manager Support", "There is a scam in my dashboard");
        assertFalse(uc.isPresent());
    }

    @Test
    void emptyDescription_returnsEmpty() {
        Optional<String> uc = UseCaseRouter.matchHandoverOnlyOverride("Delivery", "");
        assertFalse(uc.isPresent());
    }

    @Test
    void nullInputsReturnEmpty() {
        assertFalse(UseCaseRouter.matchHandoverOnlyOverride(null, "scam").isPresent());
        assertFalse(UseCaseRouter.matchHandoverOnlyOverride("Delivery", null).isPresent());
    }
}
