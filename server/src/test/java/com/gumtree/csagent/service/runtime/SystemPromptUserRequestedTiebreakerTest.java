package com.gumtree.csagent.service.runtime;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Sprint 6 §G1 — golden prompt regression check for the corrected C1
 * fix targeting cs_interactive_176.
 *
 * <p>The active-UC tiebreaker paragraph in
 * {@code prompts/system_prompt.txt} must:
 *
 * <ol>
 *   <li>State that explicit human-help requests preserve / produce
 *       {@code escalation_reason=user_requested} (priority 1).</li>
 *   <li>Explicitly forbid substituting
 *       {@code faq_miss_threshold_exceeded},
 *       {@code intake_complete_for_uc_k},
 *       {@code service_degraded}, or
 *       {@code payment_dispute_detected} for {@code user_requested}.</li>
 *   <li>Keep the genuine Tier-2 escape hatch intact so chargeback /
 *       GDPR / appeal / identity / safety flows are NOT suppressed.</li>
 * </ol>
 *
 * <p>This is a focused content snapshot — it pins the post-Sprint-6
 * §G1 prompt shape against accidental rewrite or removal. It does
 * NOT pin every byte of the prompt; only the load-bearing phrasing
 * for the C1 acceptance contract.
 */
class SystemPromptUserRequestedTiebreakerTest {

    private static String loadSystemPrompt() throws IOException {
        try (InputStream is = SystemPromptUserRequestedTiebreakerTest.class
                .getClassLoader()
                .getResourceAsStream("prompts/system_prompt.txt")) {
            assertNotNull(is, "prompts/system_prompt.txt not found on the test classpath");
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Test
    void systemPrompt_marksActiveUcTiebreakerExplicitly() throws IOException {
        String prompt = loadSystemPrompt();
        assertTrue(prompt.contains("ACTIVE-UC TIEBREAKER"),
                "ACTIVE-UC TIEBREAKER section must be present in the system prompt");
        assertTrue(prompt.contains("Sprint 6"),
                "ACTIVE-UC TIEBREAKER must be tagged with the Sprint 6 anchor");
    }

    @Test
    void systemPrompt_namesTheExplicitHumanHelpCues() throws IOException {
        String prompt = loadSystemPrompt();
        // The cs_176 persona uses "give me a phone number" / "talk to someone".
        // The prompt must enumerate enough cues for the LLM to recognise the
        // pattern even when the user phrasing varies.
        assertTrue(prompt.contains("give me a phone number"),
                "explicit-human-help cue 'give me a phone number' must be in the prompt");
        assertTrue(prompt.contains("talk to"),
                "explicit-human-help cue 'talk to' must be in the prompt");
        assertTrue(prompt.contains("speak to"),
                "explicit-human-help cue 'speak to' must be in the prompt");
        assertTrue(prompt.contains("manager") || prompt.contains("transfer me"),
                "explicit-human-help cue 'manager' or 'transfer me' must be in the prompt");
    }

    @Test
    void systemPrompt_requiresUserRequestedOverPaymentLanguage() throws IOException {
        String prompt = loadSystemPrompt();
        // The corrected C1 contract: explicit human-help wins over
        // payment-keyword phrasing in the same conversation.
        assertTrue(prompt.contains("user_requested"),
                "prompt must reference user_requested");
        assertTrue(
                prompt.contains("regardless of payment-keyword phrasing")
                        || prompt.contains("regardless of payment"),
                "prompt must explicitly steer the LLM toward user_requested regardless of payment phrasing");
    }

    @Test
    void systemPrompt_forbidsForbiddenSubstitutesForUserRequested() throws IOException {
        String prompt = loadSystemPrompt();
        // The four forbidden substitutes per Sprint 5.1 codex correction
        // (none of these are family-match against user_requested).
        assertTrue(prompt.contains("faq_miss_threshold_exceeded"),
                "prompt must mention faq_miss_threshold_exceeded so it can forbid it as a substitute");
        assertTrue(prompt.contains("intake_complete_for_uc_k"),
                "prompt must mention intake_complete_for_uc_k so it can forbid it as a substitute");
        assertTrue(prompt.contains("service_degraded"),
                "prompt must mention service_degraded so it can forbid it as a substitute");
        assertTrue(prompt.contains("payment_dispute_detected"),
                "prompt must mention payment_dispute_detected so it can forbid it as a substitute");
        assertTrue(
                prompt.contains("Do NOT substitute")
                        || prompt.contains("are NOT family-match"),
                "prompt must explicitly forbid substituting other reasons for user_requested");
    }

    @Test
    void systemPrompt_preservesGenuineTier2EscapeHatch() throws IOException {
        String prompt = loadSystemPrompt();
        // The G1 fix must not suppress real chargeback / GDPR / identity /
        // appeal / safety cases. Verify the canonical Tier-2 reasons are
        // still wired up in the prompt for genuine flows.
        assertTrue(prompt.contains("chargeback"),
                "genuine chargeback flow must remain in the prompt");
        assertTrue(prompt.contains("appeal"),
                "genuine appeal flow must remain in the prompt");
        assertTrue(prompt.contains("gdpr_intake") || prompt.contains("GDPR"),
                "genuine GDPR flow must remain in the prompt");
        assertTrue(prompt.contains("identity_verification_required") || prompt.contains("identity verification"),
                "genuine identity-verification flow must remain in the prompt");
        assertTrue(prompt.contains("trust_safety_required") || prompt.contains("imminent_harm"),
                "genuine trust & safety / imminent-harm flow must remain in the prompt");
    }

    @Test
    void systemPrompt_doesNotBlanketSuppressPaymentDispute() throws IOException {
        String prompt = loadSystemPrompt();
        // Defensive: the prompt must NOT say "never pick payment_dispute_detected"
        // — it must reserve payment_dispute_detected for genuine chargeback
        // cases (the "give me my £50 back" UC-E persona is different from a
        // real chargeback).
        assertFalse(prompt.contains("never pick payment_dispute_detected"),
                "prompt must NOT blanket-forbid payment_dispute_detected");
        assertFalse(prompt.contains("never pick `payment_dispute_detected`"),
                "prompt must NOT blanket-forbid payment_dispute_detected");
    }
}
