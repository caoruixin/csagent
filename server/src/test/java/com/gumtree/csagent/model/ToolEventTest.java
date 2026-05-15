package com.gumtree.csagent.model;

import com.gumtree.csagent.service.tools.ToolResult;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link ToolEvent} static factories (D16.A scaffolding).
 */
class ToolEventTest {

    @Test
    void of_buildsFromSuccessfulResult() {
        ToolCall call = ToolCall.builder()
                .name("search_knowledge")
                .arguments(Map.of("query", "ad blocked"))
                .build();
        ToolResult result = ToolResult.ok(Map.of("hits", 3));

        ToolEvent event = ToolEvent.of(2, call, result, 175L);

        assertEquals(2, event.stepIndex());
        assertEquals(2, event.sequenceIndex());
        assertEquals("search_knowledge", event.toolName());
        assertEquals(Map.of("query", "ad blocked"), event.arguments());
        assertTrue(event.success());
        assertEquals(Map.of("hits", 3), event.resultData());
        assertNull(event.errorMessage());
        assertEquals(175L, event.latencyMs());
    }

    @Test
    void of_buildsFromErrorResult() {
        ToolCall call = ToolCall.builder().name("search_knowledge").arguments(Map.of()).build();
        ToolResult result = ToolResult.error("backend timeout");

        ToolEvent event = ToolEvent.of(0, call, result, 5_000L);

        assertFalse(event.success());
        assertEquals("backend timeout", event.errorMessage());
        assertNull(event.resultData());
        assertEquals(5_000L, event.latencyMs());
    }

    @Test
    void rejected_setsZeroLatencyAndCarriesReason() {
        ToolCall call = ToolCall.builder().name("search_knowledge").arguments(Map.of()).build();

        ToolEvent event = ToolEvent.rejected(1, call,
                "tool_not_in_plan: search_knowledge not in [request_handover]");

        assertEquals(1, event.stepIndex());
        assertFalse(event.success());
        assertTrue(event.errorMessage().startsWith("tool_not_in_plan"));
        assertEquals(0L, event.latencyMs());
    }

    @Test
    void compactConstructor_substitutesEmptyForNullArguments() {
        ToolEvent event = new ToolEvent(0, 0, "x", null, true, null, null, 0L);
        assertNotNull(event.arguments());
        assertTrue(event.arguments().isEmpty());
    }

    @Test
    void rejected_handlesNullCall() {
        ToolEvent event = ToolEvent.rejected(0, null, "reason");
        assertNull(event.toolName());
        assertNotNull(event.arguments());
        assertTrue(event.arguments().isEmpty());
    }
}
