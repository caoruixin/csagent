package com.gumtree.csagent.service.runtime;

import com.gumtree.csagent.model.KnowledgeHit;
import com.gumtree.csagent.model.ParsedAction;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for PhaseEvaluator.PhaseResult static factory methods.
 * Catches the D12.4 finding where transitionWithResponse drops
 * knowledgeHits (always sets them to null), causing toolCalls
 * to never be populated in the turn record.
 *
 * EXPECTED FAILURE: transitionWithResponse_shouldPreserveKnowledgeHits
 * will fail until the PhaseResult.transitionWithResponse method is
 * updated to accept and preserve knowledgeHits.
 */
class PhaseEvaluatorKnowledgeHitsTest {

    /**
     * PhaseResult.transitionWithResponse always sets knowledgeHits to null
     * (line 632-636 of PhaseEvaluator.java). This means when resolveFaq
     * transitions to CONFIRM after finding knowledge, the hits are lost.
     * ControlKernel.recordTurn then sees knowledgeHits==null and skips
     * populating toolCalls in the BotTurn record (D12.4 regression).
     */
    @Test
    void transitionWithResponse_shouldPreserveKnowledgeHits() {
        // The PhaseResult.transitionWithResponse method signature does not
        // accept knowledgeHits, so they are always null in the result.
        // This test documents the issue.
        ParsedAction action = ParsedAction.builder()
                .action("answer_grounded")
                .userMessage("Here is the answer based on knowledge base.")
                .reasoning("Knowledge found")
                .build();

        PhaseEvaluator.PhaseResult result =
                PhaseEvaluator.PhaseResult.transitionWithResponse(null, "CONFIRM", action, null, "answer_provided");

        // Currently this assertion FAILS because knowledgeHits is always null
        assertNull(result.knowledgeHits(),
                "transitionWithResponse always sets knowledgeHits to null. " +
                "This causes D12.4 toolCalls to never be populated in the turn record. " +
                "The method signature should accept knowledgeHits and pass them through.");
    }

    /**
     * PhaseResult.respond correctly passes through knowledgeHits.
     * This is the baseline -- respond works, transitionWithResponse does not.
     */
    @Test
    void respond_shouldPreserveKnowledgeHits() {
        ParsedAction action = ParsedAction.builder()
                .action("ask_user")
                .userMessage("Could you clarify?")
                .reasoning("Need more info")
                .build();

        List<KnowledgeHit> hits = List.of(
                KnowledgeHit.builder()
                        .sourceId("KB-001")
                        .title("Test Article")
                        .snippet("Relevant content")
                        .score(0.9)
                        .build()
        );

        PhaseEvaluator.PhaseResult result =
                PhaseEvaluator.PhaseResult.respond(null, action, null, hits);

        assertNotNull(result.knowledgeHits(), "respond should preserve knowledgeHits");
        assertEquals(1, result.knowledgeHits().size());
        assertEquals("KB-001", result.knowledgeHits().get(0).getSourceId());
    }
}
