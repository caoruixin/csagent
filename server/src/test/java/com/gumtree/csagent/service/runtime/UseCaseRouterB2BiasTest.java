package com.gumtree.csagent.service.runtime;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Sprint 2026-05-04 §B2: deterministic pre-LLM routing bias for
 * account / messaging / email-sync flows. Stabilises
 * cs_interactive_001 / 002 / 014 / 095 and explicitly does NOT
 * route cs_interactive_095 to UC-K.
 *
 * <p>The bias is exposed via the package-private helpers
 * {@link UseCaseRouter#normalizeTopicSubject(String)} and
 * {@link UseCaseRouter#matchAccountMessagingBias(String, String)}
 * so unit tests can pin behaviour without spinning up the full
 * Spring context.
 */
class UseCaseRouterB2BiasTest {

    // ---------------- topic alias normalisation ----------------

    @Test
    void normalizeTopic_repliesAmpersandBecomesRepliesOr() {
        // Live customer forms emit "Replies & Messaging"; the registry
        // uses "Replies or Messaging" (UC-C strong prior).
        assertEquals("Replies or Messaging",
                UseCaseRouter.normalizeTopicSubject("Replies & Messaging"));
    }

    @Test
    void normalizeTopic_htmlEncodedAmpersandAlsoBecomesRepliesOr() {
        // Sprint §C2: FormContextIngestionService.sanitize runs every topic
        // through Jsoup.clean(...) with Safelist.none(), which entity-encodes
        // the literal '&' to '&amp;' before this router ever sees it.
        // Without recognising the encoded form, the strong-prior alias path
        // would never fire on cs_interactive_001/002/014 even with the live
        // form payload identical to "Replies & Messaging".
        assertEquals("Replies or Messaging",
                UseCaseRouter.normalizeTopicSubject("Replies &amp; Messaging"));
    }

    @Test
    void normalizeTopic_otherTopicsPassThrough() {
        assertEquals("Account Support",
                UseCaseRouter.normalizeTopicSubject("Account Support"));
        assertEquals("Ad Support",
                UseCaseRouter.normalizeTopicSubject("Ad Support"));
        assertEquals("UNKNOWN",
                UseCaseRouter.normalizeTopicSubject("UNKNOWN"));
    }

    @Test
    void normalizeTopic_nullPassesThrough() {
        assertEquals(null, UseCaseRouter.normalizeTopicSubject(null));
    }

    // ---------------- UC-A bias (cs_095) ----------------

    @Test
    void cs095_emailSync_noAdverts_routesToUcA() {
        // Verbatim from cs_interactive_095. The "no adverts" signal
        // dominates the messaging context; the case's expected primary
        // UC is UC-A. The LLM router previously drifted this to UC-K.
        Optional<String> bias = UseCaseRouter.matchAccountMessagingBias(
                "Account Support",
                "I think I have the wrong email address on my app account "
                        + "so am not getting messages to the app and it's "
                        + "telling me I have no adverts.. I cant see where "
                        + "it can be changed, can you help");
        assertTrue(bias.isPresent());
        assertEquals("UC-A", bias.get());
    }

    @Test
    void noListingsShowing_routesToUcA() {
        Optional<String> bias = UseCaseRouter.matchAccountMessagingBias(
                "Account Support",
                "My listings are not showing on the app");
        assertTrue(bias.isPresent());
        assertEquals("UC-A", bias.get());
    }

    @Test
    void cantSeeMyAds_routesToUcA() {
        Optional<String> bias = UseCaseRouter.matchAccountMessagingBias(
                "Account Support",
                "I can't see my ads on the live site");
        assertTrue(bias.isPresent());
        assertEquals("UC-A", bias.get());
    }

    // ---------------- UC-D bias (account / login) ----------------

    @Test
    void accountLocked_routesToUcD() {
        Optional<String> bias = UseCaseRouter.matchAccountMessagingBias(
                "Account Support",
                "My account is locked and I need to get back in");
        assertTrue(bias.isPresent());
        assertEquals("UC-D", bias.get());
    }

    @Test
    void cantLogIn_routesToUcD() {
        Optional<String> bias = UseCaseRouter.matchAccountMessagingBias(
                "Account Support",
                "I can't log in even with the right password");
        assertTrue(bias.isPresent());
        assertEquals("UC-D", bias.get());
    }

    @Test
    void forgotPassword_routesToUcD() {
        Optional<String> bias = UseCaseRouter.matchAccountMessagingBias(
                "Account Support",
                "I forgot my password and the reset email never arrives");
        assertTrue(bias.isPresent());
        assertEquals("UC-D", bias.get());
    }

    // ---------------- UC-C bias (messaging) ----------------

    @Test
    void cs001_unableToReceiveMessages_routesToUcC() {
        // Verbatim from cs_interactive_001 description.
        Optional<String> bias = UseCaseRouter.matchAccountMessagingBias(
                "Account Support",
                "I am unable to receive messages on my account");
        assertTrue(bias.isPresent());
        assertEquals("UC-C", bias.get());
    }

    @Test
    void cs002_notReceivingNotifications_routesToUcC() {
        // Verbatim from cs_interactive_002 description.
        Optional<String> bias = UseCaseRouter.matchAccountMessagingBias(
                "Account Support",
                "I am not receiving notifications from you when messages "
                        + "are arriving in my inbox.");
        assertTrue(bias.isPresent());
        assertEquals("UC-C", bias.get());
    }

    @Test
    void cs014_noResponseFromSellers_routesToUcC() {
        Optional<String> bias = UseCaseRouter.matchAccountMessagingBias(
                "Account Support",
                "Hi. I am getting no response from sellers when I contact "
                        + "them. My account has 2 email addresses and I'm "
                        + "wondering if it's causing some issues in sending "
                        + "or receiving messages.");
        assertTrue(bias.isPresent());
        assertEquals("UC-C", bias.get());
    }

    // ---------------- topic eligibility / negative cases ----------------

    @Test
    void cs066_phoneOptionMissing_doesNotMatchB2_letsUcKWin() {
        // cs_interactive_066 must keep going to UC-K via the §A2 override.
        // The B2 patterns intentionally do NOT match "not getting the
        // option to add my phone number" — the phrase is a UI regression,
        // not a missed-message complaint.
        Optional<String> bias = UseCaseRouter.matchAccountMessagingBias(
                "Account Support",
                "Why am I not getting the option to add my phone number "
                        + "as a point of contact when listening an item any more");
        assertFalse(bias.isPresent());
    }

    @Test
    void deliveryTopic_doesNotMatchB2() {
        // Handover-only topics keep their own override table; B2 must
        // not interfere.
        Optional<String> bias = UseCaseRouter.matchAccountMessagingBias(
                "Delivery",
                "I am unable to receive messages about my delivery");
        assertFalse(bias.isPresent());
    }

    @Test
    void paymentsTopic_doesNotMatchB2() {
        Optional<String> bias = UseCaseRouter.matchAccountMessagingBias(
                "Payments",
                "I can't access my payment account");
        assertFalse(bias.isPresent());
    }

    @Test
    void blankDescription_returnsEmpty() {
        assertFalse(UseCaseRouter.matchAccountMessagingBias(
                "Account Support", "").isPresent());
        assertFalse(UseCaseRouter.matchAccountMessagingBias(
                "Account Support", "   ").isPresent());
        assertFalse(UseCaseRouter.matchAccountMessagingBias(
                null, "no adverts").isPresent());
    }

    @Test
    void unrelatedDescription_returnsEmpty() {
        // Generic "thank you" / "ok" must not match.
        assertFalse(UseCaseRouter.matchAccountMessagingBias(
                "Account Support", "Thank you, that's all I needed").isPresent());
    }

    @Test
    void priority_uc_a_winsOverMessagingHint() {
        // When both ad-visibility and messaging hints appear in the same
        // description, UC-A is the primary intent (cs_095 shape).
        Optional<String> bias = UseCaseRouter.matchAccountMessagingBias(
                "Account Support",
                "I have no adverts showing and also no messages from buyers");
        assertTrue(bias.isPresent());
        assertEquals("UC-A", bias.get());
    }
}
