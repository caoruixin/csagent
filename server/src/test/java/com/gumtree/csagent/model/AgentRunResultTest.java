package com.gumtree.csagent.model;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link AgentRunResult} static factories and contract
 * (D16.A scaffolding).
 */
class AgentRunResultTest {

    @Test
    void finalAnswer_setsTerminalOutcomeAndUserMessage() {
        LlmCallEvent llm = new LlmCallEvent(0, 0, "kimi", 100, 50, 250L, "ok");
        ToolEvent tool = ToolEvent.rejected(0, null, "n/a");

        AgentRunResult result = AgentRunResult.finalAnswer(
                "Here is your answer.",
                List.of(llm),
                List.of(tool));

        assertEquals(TerminalOutcome.FINAL_ANSWER, result.terminalOutcome());
        assertEquals("Here is your answer.", result.finalUserMessage());
        assertEquals(1, result.messages().size());
        assertEquals(AgentMessage.Type.FINAL, result.messages().get(0).type());
        assertEquals(1, result.llmEvents().size());
        assertEquals(1, result.toolEvents().size());
        assertTrue(result.escalationReason().isEmpty());
    }

    @Test
    void maxSteps_setsTerminalOutcomeAndPreservesEvents() {
        LlmCallEvent llm = new LlmCallEvent(0, 0, "kimi", 1, 1, 1L, null);

        AgentRunResult result = AgentRunResult.maxSteps(List.of(llm), List.of());

        assertEquals(TerminalOutcome.MAX_STEPS, result.terminalOutcome());
        assertNull(result.finalUserMessage());
        assertEquals(1, result.llmEvents().size());
        assertTrue(result.toolEvents().isEmpty());
        assertTrue(result.escalationReason().isEmpty());
    }

    @Test
    void escalate_carriesReason() {
        AgentRunResult result = AgentRunResult.escalate(
                "user_distress", List.of(), List.of());

        assertEquals(TerminalOutcome.ESCALATE, result.terminalOutcome());
        assertEquals(Optional.of("user_distress"), result.escalationReason());
        assertNull(result.finalUserMessage());
    }

    @Test
    void error_setsErrorOutcome() {
        AgentRunResult result = AgentRunResult.error("LLM parse failure");

        assertEquals(TerminalOutcome.ERROR, result.terminalOutcome());
        assertEquals(Optional.of("LLM parse failure"), result.escalationReason());
    }

    @Test
    void compactConstructor_substitutesEmptyForNullCollections() {
        AgentRunResult result = new AgentRunResult(
                null, null, null, TerminalOutcome.FINAL_ANSWER, "msg", null,
                null, null);

        assertNotNull(result.messages());
        assertNotNull(result.toolEvents());
        assertNotNull(result.llmEvents());
        assertNotNull(result.escalationReason());
        assertTrue(result.escalationReason().isEmpty());
    }

    @Test
    void collections_areImmutable() {
        AgentRunResult result = AgentRunResult.finalAnswer("x", List.of(), List.of());

        assertThrows(UnsupportedOperationException.class,
                () -> result.messages().add(AgentMessage.ack("y")));
    }
}
