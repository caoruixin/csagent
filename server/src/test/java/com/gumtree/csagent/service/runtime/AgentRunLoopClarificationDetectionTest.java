package com.gumtree.csagent.service.runtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gumtree.csagent.model.AgentRunResult;
import com.gumtree.csagent.model.BotSession;
import com.gumtree.csagent.model.LlmResponse;
import com.gumtree.csagent.model.PhasePlan;
import com.gumtree.csagent.model.TerminalOutcome;
import com.gumtree.csagent.service.tools.ToolDispatcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Sprint 8.1 follow-up regression — pin the AgentRunLoop's clarification
 * detection so a no-tool-calls LLM message that ends with {@code '?'} or
 * carries a clarifying phrase emits
 * {@link TerminalOutcome#CLARIFICATION_NEEDED} (stay in phase) rather
 * than {@link TerminalOutcome#FINAL_ANSWER} (which the FAQ-path mapper
 * promotes to RESOLVE → CONFIRM and the next turn to CONFIRM → CLOSE).
 *
 * <p>Anchors the live trace b3ee7bcb-e7bc-... where the bot kept asking
 * "Can you confirm the ad ID?" and the system ended the chat after two
 * such turns even though the user was still answering.
 */
@ExtendWith(MockitoExtension.class)
class AgentRunLoopClarificationDetectionTest {

    @Mock private LlmInvocationService llmInvocation;
    @Mock private ToolDispatcher toolDispatcher;
    @Mock private ContextProjectionBuilder contextProjectionBuilder;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ActionParser actionParser = new ActionParser(objectMapper);

    private AgentRunLoopImpl loop;

    @BeforeEach
    void setUp() {
        loop = new AgentRunLoopImpl(llmInvocation, toolDispatcher,
                contextProjectionBuilder, actionParser, objectMapper);
    }

    private static PhasePlan resolvePlan() {
        return PhasePlan.builder()
                .phase("RESOLVE")
                .useCase("UC-B")
                .objective("Resolve")
                .allowedTools(List.of("search_knowledge", "request_handover"))
                .maxToolSteps(2)
                .systemInstruction("test")
                .validTerminalOutcomes(Set.of(
                        TerminalOutcome.FINAL_ANSWER,
                        TerminalOutcome.CLARIFICATION_NEEDED,
                        TerminalOutcome.ESCALATE))
                .build();
    }

    private void stubLlmReturns(String userMsg) {
        String json = "{\"user_message\":\"" + userMsg + "\",\"reasoning\":\"x\",\"tool_calls\":[]}";
        LlmResponse resp = LlmResponse.builder()
                .content(json).finishReason("stop").latencyMs(100L).build();
        when(contextProjectionBuilder.build(any(), any(), any(), anyString(), any(), any()))
                .thenReturn("{}");
        when(llmInvocation.invokeChat(anyString(), anyString(), anyString(), anyInt()))
                .thenReturn(resp);
    }

    @Test
    void questionMarkSuffix_returnsClarificationNeeded_notFinalAnswer() {
        stubLlmReturns("Can you confirm the ad ID you're referring to?");
        BotSession session = new BotSession();
        session.setSessionId("sess-clarify-1");
        session.setCurrentPhase("RESOLVE");
        session.setActiveUseCase("UC-B");

        AgentRunResult result = loop.run(resolvePlan(), session, "1111", List.of());

        assertEquals(TerminalOutcome.CLARIFICATION_NEEDED, result.terminalOutcome(),
                "Sprint 8.1 follow-up: '?'-suffix message must surface as "
                        + "CLARIFICATION_NEEDED so the FAQ-path mapper does NOT "
                        + "transition RESOLVE → CONFIRM mid-conversation.");
        assertEquals("Can you confirm the ad ID you're referring to?",
                result.finalUserMessage());
    }

    @Test
    void clarifyingPhraseInBody_returnsClarificationNeeded() {
        stubLlmReturns("Could you provide the ad ID in the format AD-XXXX.");
        BotSession session = new BotSession();
        session.setSessionId("sess-clarify-2");
        session.setCurrentPhase("RESOLVE");
        session.setActiveUseCase("UC-B");

        AgentRunResult result = loop.run(resolvePlan(), session, "what's the format", List.of());

        assertEquals(TerminalOutcome.CLARIFICATION_NEEDED, result.terminalOutcome(),
                "Even without a literal '?', a 'could you' phrase signals the "
                        + "bot is still gathering info; do not chain to CONFIRM/CLOSE.");
    }

    @Test
    void infoPlusQuestion_stillReturnsClarification() {
        // Live trace b3ee7bcb turn 4: LLM gives info AND asks a question.
        // Question shape wins — the user is still mid-conversation.
        stubLlmReturns("The correct ad ID format is AD-1001. Can you provide the ad ID in this format?");
        BotSession session = new BotSession();
        session.setSessionId("sess-clarify-3");
        session.setCurrentPhase("CONFIRM");
        session.setActiveUseCase("UC-B");

        AgentRunResult result = loop.run(resolvePlan(), session, "what's the correct id format", List.of());

        assertEquals(TerminalOutcome.CLARIFICATION_NEEDED, result.terminalOutcome(),
                "Info-plus-question must NOT close the chat — the user has "
                        + "another turn to answer the question.");
    }

    @Test
    void declarativeStatement_stillReturnsFinalAnswer() {
        stubLlmReturns("Your ad is now live and visible to other users.");
        BotSession session = new BotSession();
        session.setSessionId("sess-final-1");
        session.setCurrentPhase("RESOLVE");
        session.setActiveUseCase("UC-B");

        AgentRunResult result = loop.run(resolvePlan(), session, "is my ad up", List.of());

        assertEquals(TerminalOutcome.FINAL_ANSWER, result.terminalOutcome(),
                "A declarative answer with no clarifying signal must still "
                        + "be FINAL_ANSWER so the RESOLVE → CONFIRM transition "
                        + "fires for genuinely satisfied flows.");
    }

    @Test
    void isClarificationMessage_unitChecks() {
        assertTrue(AgentRunLoopImpl.isClarificationMessage("Can you confirm the ad ID?"));
        assertTrue(AgentRunLoopImpl.isClarificationMessage("Could you tell me which ad."));
        assertTrue(AgentRunLoopImpl.isClarificationMessage("What is your ad ID"));
        assertFalse(AgentRunLoopImpl.isClarificationMessage("Your ad is live."));
        assertFalse(AgentRunLoopImpl.isClarificationMessage(""));
        assertFalse(AgentRunLoopImpl.isClarificationMessage(null));
    }
}
