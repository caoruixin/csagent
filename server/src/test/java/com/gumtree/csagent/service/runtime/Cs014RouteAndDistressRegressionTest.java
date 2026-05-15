package com.gumtree.csagent.service.runtime;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Sprint 2 follow-up — cs_interactive_014 case-level route / loop
 * regression. Pins the deterministic decisions the smoke CaseSpec
 * actually exercises with cs_014's verbatim form context and seed
 * messages.
 *
 * <p>Why this exists: cs_014's smoke spec previously declared
 * {@code expected.escalation_trigger=user_distress}, but none of the
 * scripted user inputs (form description + the two seed messages)
 * contain a distress signal that
 * {@link EscalationReasonResolver#detectDistressSignal(String)}
 * recognises. The spec also implies a soft Replies/Messaging FAQ
 * resolution path. After Sprint-2 follow-up the CaseSpec was
 * corrected to {@code faq_miss_threshold_exceeded} (the truthful
 * outcome the bot reaches when the FAQ corpus has no admin-
 * mediated email-revert article). This test pins the deterministic
 * decisions so:
 *
 * <ul>
 *   <li>A future change that widens the distress detector cannot
 *       silently flip cs_014's persisted reason to
 *       {@code user_distress} without an authored utterance change.</li>
 *   <li>The B2 routing bias and the strong-prior topic alias cannot
 *       drift away from UC-C for cs_014's Replies/Messaging shape.</li>
 *   <li>Sprint 2's cs_002 distress contract is preserved by
 *       contrast: cs_002's verbatim seed "this is ridiculous"-shaped
 *       messages still fire the detector, while cs_014's calmer
 *       phrasing does not.</li>
 * </ul>
 */
class Cs014RouteAndDistressRegressionTest {

    private final EscalationReasonResolver resolver = new EscalationReasonResolver();

    /** Verbatim from {@code cs_interactive_014.yaml} form_context. */
    private static final String CS014_TOPIC = "Replies & Messaging";
    private static final String CS014_DESCRIPTION =
            "Hi. I am getting no response from sellers when I contact them. "
                    + "My account has 2 email addresses and I'm wondering if it's "
                    + "causing some issues in sending or receiving messages.";

    /** Verbatim from {@code cs_interactive_014.yaml} persona.seed_messages. */
    private static final String CS014_SEED_1 =
            "How come an administrator can't atleast make the contact email "
                    + "match the main account email?";
    private static final String CS014_SEED_2 =
            "I'd be happy enough if the contact email address was reverted to "
                    + "the original account address";

    // ------------------------------------------------------------
    // Distress detector — cs_014 utterances must NOT fire it.
    // ------------------------------------------------------------

    @Test
    void cs014_formDescription_doesNotTriggerDistress() {
        // Calm informational description — no shouting, no frustration phrase,
        // no rhetorical complaint pattern.
        assertFalse(resolver.detectDistressSignal(CS014_DESCRIPTION),
                "cs_014 form description must not trigger user_distress; "
                        + "any future widening that flips this would change "
                        + "the persisted escalation_reason from "
                        + "faq_miss_threshold_exceeded to user_distress and "
                        + "break the corrected smoke CaseSpec.");
    }

    @Test
    void cs014_seedMessage1_doesNotTriggerDistress() {
        // Rhetorical mild complaint ("How come ...?"). NOT in DISTRESS_PATTERNS.
        assertFalse(resolver.detectDistressSignal(CS014_SEED_1),
                "Seed message 1 must not trigger user_distress.");
    }

    @Test
    void cs014_seedMessage2_doesNotTriggerDistress() {
        // Cooperative concession ("I'd be happy enough if ..."). Polite tone.
        assertFalse(resolver.detectDistressSignal(CS014_SEED_2),
                "Seed message 2 must not trigger user_distress.");
    }

    @Test
    void cs014_seedMessages_areNotAllCapsShouts() {
        // Sanity: the persona is `frustration_level: high` but the actual
        // text is mixed-case.
        assertFalse(EscalationReasonResolver.isAllCapsShout(CS014_SEED_1));
        assertFalse(EscalationReasonResolver.isAllCapsShout(CS014_SEED_2));
        assertFalse(EscalationReasonResolver.isAllCapsShout(CS014_DESCRIPTION));
    }

    // ------------------------------------------------------------
    // Sprint 2 cross-case contrast: cs_002 utterances DO fire the
    // detector. Without this guard, a regression that breaks
    // cs_002's distress contract would pass cs_014's tests.
    // ------------------------------------------------------------

    @Test
    void cs002_seedMessage_stillTriggersDistress_byContrast() {
        // Verbatim from cs_interactive_002.yaml seed_messages[0]. cs_002
        // MUST stay distress so the Sprint B1 contract is still met
        // even after the cs_014 CaseSpec correction. (cs_002's other
        // seeds use only sub-shout uppercase emphasis like "I HAVE" —
        // 5 letters, below the 8-letter ALL-CAPS threshold — so this
        // seed is the canonical distress trigger.)
        assertTrue(resolver.detectDistressSignal(
                "How long do I have to wait to sort this out?"),
                "cs_002 seed 1 must still trigger user_distress.");
    }

    // ------------------------------------------------------------
    // Routing — alias + strong-prior + B2 bias all converge on UC-C.
    // ------------------------------------------------------------

    @Test
    void cs014_topicAliasNormalizes_repliesAmpersandToRepliesOr() {
        // The alias must resolve so the strong-prior table fires for
        // cs_014; without normalization the router falls to UNKNOWN_TOPIC
        // and the LLM chooses any UC.
        assertEquals("Replies or Messaging",
                UseCaseRouter.normalizeTopicSubject(CS014_TOPIC));
    }

    @Test
    void cs014_b2BiasResolvesToUcC() {
        // "no response from sellers" matches MESSAGING_BIAS_PATTERN. Even
        // if the strong-prior path is bypassed in the future, B2 still
        // pins UC-C — preventing a regression to UC-B / UC-F via the
        // LLM router.
        Optional<String> bias = UseCaseRouter.matchAccountMessagingBias(
                CS014_TOPIC, CS014_DESCRIPTION);
        assertTrue(bias.isPresent(), "B2 bias must match cs_014 description.");
        assertEquals("UC-C", bias.get(),
                "cs_014 must route to UC-C via B2 messaging bias; any "
                        + "drift to UC-A / UC-D / UC-B here breaks the "
                        + "Sprint 2 stability contract.");
    }

    @Test
    void cs014_b2BiasAlsoMatchesAfterAliasNormalization() {
        // After alias normalisation the topic becomes "Replies or
        // Messaging" — B2 must continue to match (not just before
        // normalisation). Pins both spellings so future cleanup of
        // the alias map doesn't accidentally drop B2 coverage.
        Optional<String> bias = UseCaseRouter.matchAccountMessagingBias(
                UseCaseRouter.normalizeTopicSubject(CS014_TOPIC),
                CS014_DESCRIPTION);
        assertTrue(bias.isPresent());
        assertEquals("UC-C", bias.get());
    }

    @Test
    void cs014_doesNotTripUcKTechnicalRegressionOverride() {
        // cs_014's description has no in-app regression signal. UC-K
        // override must NOT fire — that would override the UC-C route
        // and stamp a wrong intake_complete_for_uc_k reason.
        assertFalse(UseCaseRouter.matchUcKTechnicalRegression(
                UseCaseRouter.normalizeTopicSubject(CS014_TOPIC),
                CS014_DESCRIPTION),
                "cs_014 must not trip the UC-K technical-regression "
                        + "override; that route is reserved for cs_066-shape "
                        + "feature-disappeared cases.");
    }

    // ------------------------------------------------------------
    // Reason canonicalization — the corrected smoke CaseSpec value
    // must round-trip through the resolver unchanged.
    // ------------------------------------------------------------

    @Test
    void cs014_correctedReason_isCanonical() {
        // The CaseSpec now expects faq_miss_threshold_exceeded — must be
        // a canonical EscalationReason enum value so the L1 trace
        // contract enum gate accepts it.
        assertEquals("faq_miss_threshold_exceeded",
                resolver.canonicalize("faq_miss_threshold_exceeded"));
    }

    @Test
    void cs014_oldReason_isCanonicalButNotApplicable() {
        // user_distress is still a canonical value (don't break Sprint
        // B1's cs_002 path). This test only documents that the smoke
        // CaseSpec correction is a SPEC change, not an enum change.
        assertEquals("user_distress",
                resolver.canonicalize("user_distress"));
    }
}
