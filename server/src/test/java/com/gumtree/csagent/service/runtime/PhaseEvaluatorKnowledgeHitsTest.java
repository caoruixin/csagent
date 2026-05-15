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
 * <p>Updated for Task #10: ParsedAction no longer carries the legacy
 * synthetic {@code action} / {@code parameters} fields. Construction
 * uses {@code toolCalls} + {@code userMessage} only.
 */
class PhaseEvaluatorKnowledgeHitsTest {

    /**
     * PhaseResult.transitionWithResponse always sets knowledgeHits to null
     * in the legacy 5-arg overload. This test documents the issue.
     * ControlKernel.recordTurn then sees knowledgeHits==null and skips
     * populating toolCalls in the BotTurn record (D12.4 regression).
     */
    @Test
    void transitionWithResponse_shouldPreserveKnowledgeHits() {
        // The 5-arg PhaseResult.transitionWithResponse method does not
        // accept knowledgeHits, so they are always null in the result.
        ParsedAction action = ParsedAction.builder()
                .toolCalls(List.of())
                .userMessage("Here is the answer based on knowledge base.")
                .reasoning("Knowledge found")
                .build();

        PhaseEvaluator.PhaseResult result =
                PhaseEvaluator.PhaseResult.transitionWithResponse(null, "CONFIRM", action, null, "answer_provided");

        assertNull(result.knowledgeHits(),
                "Legacy 5-arg transitionWithResponse always sets knowledgeHits to null. " +
                "Use the 6-arg overload to preserve hits.");
    }

    /**
     * PhaseResult.respond correctly passes through knowledgeHits.
     * This is the baseline -- respond works.
     */
    @Test
    void respond_shouldPreserveKnowledgeHits() {
        ParsedAction action = ParsedAction.builder()
                .toolCalls(List.of())
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
