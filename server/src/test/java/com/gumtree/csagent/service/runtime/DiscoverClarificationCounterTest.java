package com.gumtree.csagent.service.runtime;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Sprint 078 / S-Auto-23 / M-Auto-6 Sub-sprint A — R2.a #3 DISCOVER free-text
 * clarification counter predicate.
 *
 * <p>Exercises {@link AgentRunLoopImpl#isDiscoverFreeTextClarification} — the
 * structural cardinality predicate that gates the
 * {@code session.clarificationCount} increment on the live AgentRunLoopImpl
 * path. ALL four structural criteria must hold; zero content / similarity
 * matching. The predicate takes only the BOT's outgoing reply, so a customer
 * message can never be misclassified (field-confusion regression guard).
 */
class DiscoverClarificationCounterTest {

    // --- #3 positive ---

    @Test
    void discoverNoToolNoCommitWithBotReply_increments() {
        assertTrue(AgentRunLoopImpl.isDiscoverFreeTextClarification(
                "DISCOVER", false, false, "Can you confirm the ad ID?"));
    }

    @Test
    void discoverPhaseLabelIsCaseInsensitive() {
        assertTrue(AgentRunLoopImpl.isDiscoverFreeTextClarification(
                "discover", false, false, "Which listing do you mean?"));
    }

    @Test
    void nonQuestionFreeTextStillCounts_structuralNotContentBased() {
        // The predicate is cardinality-only — it does NOT require a "?"-shaped
        // clarification. Any non-empty DISCOVER free-text reply with no tool
        // call + no UC commit counts.
        assertTrue(AgentRunLoopImpl.isDiscoverFreeTextClarification(
                "DISCOVER", false, false, "Tell me more about the problem."));
    }

    // --- #3 negative (5) ---

    @Test
    void discoverWithToolCall_doesNotIncrement() {
        assertFalse(AgentRunLoopImpl.isDiscoverFreeTextClarification(
                "DISCOVER", true, false, "Searching the knowledge base."));
    }

    @Test
    void discoverThatCommitsUseCase_doesNotIncrement() {
        assertFalse(AgentRunLoopImpl.isDiscoverFreeTextClarification(
                "DISCOVER", false, true, "I can help with that."));
    }

    @Test
    void discoverWithEmptyBotReply_doesNotIncrement() {
        assertFalse(AgentRunLoopImpl.isDiscoverFreeTextClarification(
                "DISCOVER", false, false, ""));
        assertFalse(AgentRunLoopImpl.isDiscoverFreeTextClarification(
                "DISCOVER", false, false, "   "));
        assertFalse(AgentRunLoopImpl.isDiscoverFreeTextClarification(
                "DISCOVER", false, false, null));
    }

    @Test
    void nonDiscoverPhase_doesNotIncrement_regardlessOfReply() {
        for (String phase : new String[] {"RESOLVE", "INTAKE", "CONFIRM", "CLOSE", "INIT"}) {
            assertFalse(AgentRunLoopImpl.isDiscoverFreeTextClarification(
                            phase, false, false, "Can you confirm the ad ID?"),
                    "phase " + phase + " must not increment the clarification counter");
        }
    }

    @Test
    void customerMessageFieldConfusion_doesNotIncrement() {
        // Field-confusion regression guard: the predicate takes the BOT reply
        // only. When the bot reply is empty (the customer's incoming
        // user_message is a different variable the predicate never sees), the
        // counter must NOT increment — a customer turn can never be mistaken
        // for a bot clarification.
        assertFalse(AgentRunLoopImpl.isDiscoverFreeTextClarification(
                "DISCOVER", false, false, ""));
    }
}
