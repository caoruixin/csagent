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

    /**
     * Sprint 8.2 §M0b — when the loop hits {@code maxToolSteps} after several
     * successful LLM responses, the {@code lastLlmRawResponse} from the
     * terminating iteration must survive into the {@link AgentRunResult} so
     * {@code bot_turns.llm_raw_response} is not silently nulled out.
     */
    @Test
    void maxSteps_preservesLastLlmRawResponseWhenSupplied() {
        LlmCallEvent llm = new LlmCallEvent(0, 0, "kimi", 10, 5, 30L, "ok");
        String rawResponse = "{\"user_message\":\"Let me check that for you.\"}";

        AgentRunResult result = AgentRunResult.maxSteps(
                List.of(llm), List.of(), "{\"projection\":1}", rawResponse);

        assertEquals(TerminalOutcome.MAX_STEPS, result.terminalOutcome(),
                "MAX_STEPS terminal outcome must be unchanged");
        assertEquals(rawResponse, result.lastLlmRawResponse(),
                "MAX_STEPS must preserve lastLlmRawResponse so the trace UI can render the real LLM content");
        assertEquals("{\"projection\":1}", result.lastProjection());
    }

    @Test
    void maxSteps_legacyOverloadStillReturnsNullRawResponse() {
        LlmCallEvent llm = new LlmCallEvent(0, 0, "kimi", 1, 1, 1L, null);

        AgentRunResult legacy = AgentRunResult.maxSteps(List.of(llm), List.of());
        AgentRunResult legacyWithProjection = AgentRunResult.maxSteps(
                List.of(llm), List.of(), "{\"projection\":1}");

        assertNull(legacy.lastLlmRawResponse(),
                "callers that did not pass a raw response continue to see null (pre-§M0b shape)");
        assertNull(legacyWithProjection.lastLlmRawResponse(),
                "the 3-arg projection-only overload must remain back-compatible");
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
