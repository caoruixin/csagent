package com.gumtree.csagent.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link LlmCallEvent} (D16.A scaffolding).
 */
class LlmCallEventTest {

    @Test
    void of_buildsFromLlmResponse() {
        LlmResponse response = LlmResponse.builder()
                .content("{\"user_message\":\"hi\"}")
                .promptTokens(120)
                .completionTokens(45)
                .latencyMs(200L)
                .build();

        LlmCallEvent event = LlmCallEvent.of(0, response, 250L);

        assertEquals(0, event.stepIndex());
        assertEquals(0, event.sequenceIndex());
        assertEquals(120, event.promptTokens());
        assertEquals(45, event.completionTokens());
        // Loop-measured latency takes precedence over the LlmResponse latency.
        assertEquals(250L, event.latencyMs());
        assertEquals("{\"user_message\":\"hi\"}", event.responseSummary());
    }

    @Test
    void of_truncatesLongResponseSummary() {
        StringBuilder big = new StringBuilder();
        for (int i = 0; i < 1_000; i++) {
            big.append('x');
        }
        LlmResponse response = LlmResponse.builder().content(big.toString()).build();

        LlmCallEvent event = LlmCallEvent.of(0, response, 0L);

        assertEquals(500, event.responseSummary().length());
    }

    @Test
    void of_handlesNullResponse() {
        LlmCallEvent event = LlmCallEvent.of(3, null, 50L);
        assertEquals(3, event.stepIndex());
        assertEquals(50L, event.latencyMs());
        assertNull(event.responseSummary());
    }

    @Test
    void records_haveValueEquality() {
        LlmCallEvent a = new LlmCallEvent(0, 0, "kimi", 100, 50, 200L, "x");
        LlmCallEvent b = new LlmCallEvent(0, 0, "kimi", 100, 50, 200L, "x");
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }
}
