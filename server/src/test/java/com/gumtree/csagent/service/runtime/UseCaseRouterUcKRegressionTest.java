package com.gumtree.csagent.service.runtime;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Sprint 2026-05-04 §A2: cs_interactive_066-style technical regression
 * cases must route to UC-K. Generic FAQ phrasing must stay on UC-E.
 *
 * <p>The override is a deterministic pre-LLM step inside
 * {@link UseCaseRouter#route} — it is exposed via the package-private
 * helper {@link UseCaseRouter#matchUcKTechnicalRegression(String, String)}
 * so it can be unit-tested without spinning up the full Spring context
 * (mirrors the style of {@link UseCaseRouterOverrideTest}).
 */
class UseCaseRouterUcKRegressionTest {

    // ---------------- regression descriptions → UC-K ----------------

    @Test
    void cs066_phoneOptionMissing_routesToUcK() {
        // Verbatim from cs_interactive_066's form description.
        assertTrue(UseCaseRouter.matchUcKTechnicalRegression(
                "Account Support",
                "Why am I not getting the option to add my phone number as a "
                        + "point of contact when listening an item any more"));
    }

    @Test
    void disappearedOption_routesToUcK() {
        assertTrue(UseCaseRouter.matchUcKTechnicalRegression(
                "Ad Support",
                "The phone-number contact option disappeared from my listing"));
    }

    @Test
    void greyedOutButton_routesToUcK() {
        assertTrue(UseCaseRouter.matchUcKTechnicalRegression(
                "Technical Support",
                "The submit button is greyed out and I can't tap it"));
    }

    @Test
    void usedToWorkOnAndroid_routesToUcK() {
        assertTrue(UseCaseRouter.matchUcKTechnicalRegression(
                "Technical Support",
                "It used to work on Android but the form no longer loads"));
    }

    @Test
    void errorWhenEnabling_routesToUcK() {
        assertTrue(UseCaseRouter.matchUcKTechnicalRegression(
                "Account Support",
                "I get an error when enabling phone contact in the app"));
    }

    @Test
    void appCrashesOnIos_routesToUcK() {
        assertTrue(UseCaseRouter.matchUcKTechnicalRegression(
                "Technical Support",
                "The app crashes on iOS whenever I open the listing flow"));
    }

    @Test
    void cantSeeContactOption_routesToUcK() {
        assertTrue(UseCaseRouter.matchUcKTechnicalRegression(
                "Ad Support",
                "I can't see the contact option that used to be on the listing"));
    }

    // ---------------- generic FAQ → NOT UC-K ----------------

    @Test
    void howDoContactOptionsWork_doesNotRouteToUcK() {
        assertFalse(UseCaseRouter.matchUcKTechnicalRegression(
                "Technical Support",
                "How do contact options work? Can buyers call me?"));
    }

    @Test
    void canBuyersCallMe_doesNotRouteToUcK() {
        assertFalse(UseCaseRouter.matchUcKTechnicalRegression(
                "Technical Support",
                "Can buyers call me directly when they reply to my ad?"));
    }

    @Test
    void whereDoISetContactPreferences_doesNotRouteToUcK() {
        assertFalse(UseCaseRouter.matchUcKTechnicalRegression(
                "Account Support",
                "Where do I set my contact preferences?"));
    }

    @Test
    void plainPaymentFaq_doesNotRouteToUcK() {
        // Should fall through to the normal LLM router and reach UC-F.
        assertFalse(UseCaseRouter.matchUcKTechnicalRegression(
                "Payments",
                "How do I receive payment when I sell an item?"));
    }

    @Test
    void cs095_emailSyncIssue_doesNotRouteToUcK() {
        // Verbatim from cs_interactive_095. The user describes an account /
        // email sync issue, not an in-app feature regression. Must NOT
        // short-circuit to UC-K — the LLM router resolves this to UC-A or
        // UC-D so the bot can answer the FAQ.
        assertFalse(UseCaseRouter.matchUcKTechnicalRegression(
                "Account Support",
                "I think I have the wrong email address on my app account "
                        + "so am not getting messages to the app and it's "
                        + "telling me I have no adverts.. I cant see where "
                        + "it can be changed, can you help"));
    }

    @Test
    void notGettingNotifications_withoutUiNoun_doesNotRouteToUcK() {
        // Sanity check: "not getting messages / notifications / emails" is
        // a UC-C / UC-D complaint, not a UI regression. Must not flip to
        // UC-K.
        assertFalse(UseCaseRouter.matchUcKTechnicalRegression(
                "Replies or Messaging",
                "I am not getting notifications when buyers reply to my ad"));
    }

    // ---------------- topic eligibility ----------------

    @Test
    void deliveryTopic_isOutsideUcKEligibleTopics() {
        // Delivery is handover-only and uses its own override table; the
        // UC-K helper should not fire even if the description contains
        // regression keywords.
        assertFalse(UseCaseRouter.matchUcKTechnicalRegression(
                "Delivery",
                "the courier app crashes and delivery option disappeared"));
    }

    @Test
    void proContractTopic_doesNotFireUcKOverride() {
        assertFalse(UseCaseRouter.matchUcKTechnicalRegression(
                "Pro Contract",
                "the dashboard error keeps blocking me"));
    }

    // ---------------- defensive cases ----------------

    @Test
    void blankDescription_returnsFalse() {
        assertFalse(UseCaseRouter.matchUcKTechnicalRegression("Account Support", ""));
        assertFalse(UseCaseRouter.matchUcKTechnicalRegression("Account Support", "   "));
    }

    @Test
    void nullInputs_returnFalse() {
        assertFalse(UseCaseRouter.matchUcKTechnicalRegression(null, "missing button"));
        assertFalse(UseCaseRouter.matchUcKTechnicalRegression("Account Support", null));
    }
}
