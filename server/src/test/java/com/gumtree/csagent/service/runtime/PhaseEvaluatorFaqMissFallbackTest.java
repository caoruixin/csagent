package com.gumtree.csagent.service.runtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gumtree.csagent.model.*;
import com.gumtree.csagent.service.guardrails.ScriptLibraryService;
import com.gumtree.csagent.service.knowledge.KnowledgeSearchService;
import com.gumtree.csagent.service.observability.EventEmitter;
import com.gumtree.csagent.service.tools.CreateCaseControlledTool;
import com.gumtree.csagent.service.tools.ToolDispatcher;
import com.gumtree.csagent.service.tools.ToolResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for PhaseEvaluator.resolveFaq() FAQ miss fallback behaviour (D14.7).
 *
 * When FAQ miss occurs but form_context.description exists (> 10 chars),
 * the bot invokes the main LLM with a modified projection containing a
 * faq_miss_instruction that tells the LLM to use the form description
 * to help the user.
 */
@ExtendWith(MockitoExtension.class)
class PhaseEvaluatorFaqMissFallbackTest {

    @Mock private UseCaseRegistryService useCaseRegistry;
    @Mock private KnowledgeSearchService knowledgeSearchService;
    @Mock private ScriptLibraryService scriptLibrary;
    @Mock private LlmInvocationService llmInvocation;
    @Mock private ContextProjectionBuilder contextProjection;
    @Mock private ActionParser actionParser;
    @Mock private CreateCaseControlledTool createCaseTool;
    @Mock private EventEmitter eventEmitter;
    @Mock private ToolDispatcher toolDispatcher;

    private PhaseEvaluator phaseEvaluator;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        phaseEvaluator = new PhaseEvaluator(
                useCaseRegistry, knowledgeSearchService, scriptLibrary,
                llmInvocation, contextProjection, actionParser,
                objectMapper, createCaseTool, eventEmitter, toolDispatcher);
    }

    /**
     * FAQ miss + form description present (> 10 chars) -> LLM invoked with
     * faq_miss_instruction containing the form description.
     */
    @Test
    void resolveFaq_faqMissWithFormDescription_shouldInvokeLlmWithInstruction() {
        BotSession session = buildFaqSession("UC-A",
                "{\"description\":\"My ad was removed unfairly and I need help restoring it\",\"email\":\"test@test.com\"}");

        stubUcDefinition("UC-A", "Account FAQ", "FAQ");
        stubFaqMissToolResult();

        // Stub the projection builder to return a valid JSON projection
        String projectionJson = "{\"phase\":\"RESOLVE\",\"use_case\":\"UC-A\"}";
        when(contextProjection.buildProjection(eq(session), anyList(), isNull(), anyString()))
                .thenReturn(projectionJson);

        // Stub the LLM response for the faq_miss_instruction call
        LlmResponse llmResponse = LlmResponse.builder()
                .content("{\"action\":\"answer_grounded\",\"user_message\":\"Based on what you described, here is how to appeal...\"}")
                .finishReason("stop")
                .build();
        when(llmInvocation.invokeChat(anyString(), anyString(), anyString(), anyInt()))
                .thenReturn(llmResponse);

        ParsedAction parsedAction = ParsedAction.builder()
                .action("answer_grounded")
                .userMessage("Based on what you described, here is how to appeal...")
                .reasoning("Used form context")
                .build();
        when(actionParser.parse(anyString())).thenReturn(parsedAction);

        // Act
        PhaseEvaluator.PhaseResult result = phaseEvaluator.evaluate(session, "help me", List.of());

        // Assert: LLM was invoked with a projection that contains faq_miss_instruction
        ArgumentCaptor<String> projectionCaptor = ArgumentCaptor.forClass(String.class);
        verify(llmInvocation).invokeChat(projectionCaptor.capture(), anyString(), anyString(), anyInt());

        String capturedProjection = projectionCaptor.getValue();
        assertTrue(capturedProjection.contains("faq_miss_instruction"),
                "Modified projection should contain faq_miss_instruction. Got: " + capturedProjection);
        assertTrue(capturedProjection.contains("My ad was removed unfairly"),
                "faq_miss_instruction should contain the form description. Got: " + capturedProjection);

        // Assert: response text comes from the LLM
        assertEquals("Based on what you described, here is how to appeal...", result.responseText());
    }

    /**
     * FAQ miss + no form description -> hardcoded "describe more" response,
     * no LLM invocation for fallback.
     */
    @Test
    void resolveFaq_faqMissWithoutFormDescription_shouldReturnHardcodedResponse() {
        BotSession session = buildFaqSession("UC-A", "{\"email\":\"test@test.com\"}");

        stubUcDefinition("UC-A", "Account FAQ", "FAQ");
        stubFaqMissToolResult();

        // Act
        PhaseEvaluator.PhaseResult result = phaseEvaluator.evaluate(session, "help me", List.of());

        // Assert: hardcoded fallback message
        assertNotNull(result.responseText());
        assertTrue(result.responseText().contains("Could you describe your issue"),
                "No form description should produce hardcoded ask_user. Got: " + result.responseText());

        // LLM should NOT be invoked for the faq_miss_instruction path
        // (it may be invoked for enrichment, but not for fallback)
        verify(llmInvocation, never()).invokeChat(anyString(), anyString(), anyString(), anyInt());
    }

    /**
     * FAQ miss + null formContext -> hardcoded response.
     */
    @Test
    void resolveFaq_faqMissWithNullFormContext_shouldReturnHardcodedResponse() {
        BotSession session = buildFaqSession("UC-A", null);

        stubUcDefinition("UC-A", "Account FAQ", "FAQ");
        stubFaqMissToolResult();

        PhaseEvaluator.PhaseResult result = phaseEvaluator.evaluate(session, "help me", List.of());

        assertNotNull(result.responseText());
        assertTrue(result.responseText().contains("Could you describe your issue"),
                "Null formContext should produce hardcoded response. Got: " + result.responseText());
    }

    /**
     * FAQ miss + form description <= 10 chars -> hardcoded response.
     */
    @Test
    void resolveFaq_faqMissWithShortFormDescription_shouldReturnHardcodedResponse() {
        BotSession session = buildFaqSession("UC-A", "{\"description\":\"short\"}");

        stubUcDefinition("UC-A", "Account FAQ", "FAQ");
        stubFaqMissToolResult();

        PhaseEvaluator.PhaseResult result = phaseEvaluator.evaluate(session, "help me", List.of());

        assertNotNull(result.responseText());
        assertTrue(result.responseText().contains("Could you describe your issue"),
                "Short form description (<=10) should produce hardcoded response. Got: " + result.responseText());
    }

    /**
     * FAQ miss + form description present + LLM throws -> falls back to
     * hardcoded "describe more" response.
     */
    @Test
    void resolveFaq_faqMissWithFormDescriptionLlmThrows_shouldFallbackToHardcoded() {
        BotSession session = buildFaqSession("UC-A",
                "{\"description\":\"I cannot log into my account after resetting my password\"}");

        stubUcDefinition("UC-A", "Account FAQ", "FAQ");
        stubFaqMissToolResult();

        String projectionJson = "{\"phase\":\"RESOLVE\",\"use_case\":\"UC-A\"}";
        when(contextProjection.buildProjection(eq(session), anyList(), isNull(), anyString()))
                .thenReturn(projectionJson);

        // LLM throws when invoked with the faq_miss_instruction
        when(llmInvocation.invokeChat(anyString(), anyString(), anyString(), anyInt()))
                .thenThrow(new RuntimeException("LLM service unavailable"));

        // Act
        PhaseEvaluator.PhaseResult result = phaseEvaluator.evaluate(session, "help me", List.of());

        // Assert: hardcoded fallback (not crash)
        assertNotNull(result.responseText());
        assertTrue(result.responseText().contains("Could you describe your issue"),
                "LLM failure should fall back to hardcoded response. Got: " + result.responseText());
    }

    /**
     * Verify faqMissCount is incremented REGARDLESS of which path is taken
     * (hardcoded response vs LLM fallback). Budget enforcement depends on this.
     */
    @Test
    void resolveFaq_faqMiss_shouldIncrementFaqMissCount_withFormDescription() {
        BotSession session = buildFaqSession("UC-A",
                "{\"description\":\"I need help with my account recovery process\"}");
        assertEquals(0, session.getFaqMissCount(), "Starting faqMissCount should be 0");

        stubUcDefinition("UC-A", "Account FAQ", "FAQ");
        stubFaqMissToolResult();

        String projectionJson = "{\"phase\":\"RESOLVE\",\"use_case\":\"UC-A\"}";
        when(contextProjection.buildProjection(eq(session), anyList(), isNull(), anyString()))
                .thenReturn(projectionJson);

        LlmResponse llmResponse = LlmResponse.builder()
                .content("{\"action\":\"ask_user\",\"user_message\":\"Let me help you with that.\"}")
                .build();
        when(llmInvocation.invokeChat(anyString(), anyString(), anyString(), anyInt()))
                .thenReturn(llmResponse);
        when(actionParser.parse(anyString())).thenReturn(
                ParsedAction.builder().action("ask_user").userMessage("Let me help.").build());

        phaseEvaluator.evaluate(session, "help me", List.of());

        assertEquals(1, session.getFaqMissCount(),
                "faqMissCount must be incremented even when LLM fallback path is taken");
    }

    /**
     * Verify faqMissCount is incremented when no form description (hardcoded path).
     */
    @Test
    void resolveFaq_faqMiss_shouldIncrementFaqMissCount_withoutFormDescription() {
        BotSession session = buildFaqSession("UC-A", null);
        assertEquals(0, session.getFaqMissCount(), "Starting faqMissCount should be 0");

        stubUcDefinition("UC-A", "Account FAQ", "FAQ");
        stubFaqMissToolResult();

        phaseEvaluator.evaluate(session, "help me", List.of());

        assertEquals(1, session.getFaqMissCount(),
                "faqMissCount must be incremented in the hardcoded response path");
    }

    /**
     * When faqMissCount reaches the limit (>= 2), should escalate regardless
     * of whether form description is available.
     */
    @Test
    void resolveFaq_faqMissExceedsLimit_shouldEscalate() {
        BotSession session = buildFaqSession("UC-A",
                "{\"description\":\"I have a detailed description of my problem here\"}");
        session.setFaqMissCount(1); // Already 1 miss, this will be the 2nd

        stubUcDefinition("UC-A", "Account FAQ", "FAQ");
        stubFaqMissToolResult();

        PhaseEvaluator.PhaseResult result = phaseEvaluator.evaluate(session, "help me", List.of());

        // Assert: escalation, not LLM fallback
        assertTrue(result.shouldEscalate(),
                "Second FAQ miss should trigger escalation");
        assertEquals(2, session.getFaqMissCount(),
                "faqMissCount should be 2 after second miss");
        assertTrue(result.responseText().contains("connect you with a specialist"),
                "Escalation message should mention specialist. Got: " + result.responseText());
    }

    // --- Helper: Build event logging test (C3) ---

    /**
     * Verify that RETRIEVAL_EXECUTED event records the enriched query, not raw userMessage.
     * This tests D14.8: emitRetrievalExecuted should pass enrichedQuery.
     */
    @Test
    void resolveFaq_retrievalExecutedEvent_shouldUseEnrichedQuery() {
        BotSession session = buildFaqSession("UC-A",
                "{\"description\":\"My ad was flagged and I want to know why\"}");

        stubUcDefinition("UC-A", "Account FAQ", "FAQ");

        // Stub a successful (non-miss) search result
        ToolResult toolResult = ToolResult.ok(Map.of(
                "faq_miss", false,
                "retrieval_miss", false,
                "answer_miss", false,
                "hits", List.of(Map.of(
                        "source_id", "KB-001",
                        "title", "Ad Flagging FAQ",
                        "snippet", "Articles about ad flagging",
                        "score", 0.9
                ))
        ));
        when(toolDispatcher.dispatch(eq("search_knowledge"), eq(session), anyMap()))
                .thenReturn(toolResult);

        String projectionJson = "{\"phase\":\"RESOLVE\",\"use_case\":\"UC-A\"}";
        when(contextProjection.buildProjection(eq(session), anyList(), anyList(), anyString()))
                .thenReturn(projectionJson);

        LlmResponse llmResponse = LlmResponse.builder()
                .content("{\"action\":\"answer_grounded\",\"user_message\":\"Here is the answer.\"}")
                .build();
        when(llmInvocation.invokeChat(anyString(), anyString(), anyString(), anyInt()))
                .thenReturn(llmResponse);

        ParsedAction parsedAction = ParsedAction.builder()
                .action("answer_grounded")
                .userMessage("Here is the answer.")
                .build();
        when(actionParser.parse(anyString())).thenReturn(parsedAction);

        // Act: "pls" is a short message (< 20 chars) on turn 0, so enrichment should activate
        phaseEvaluator.evaluate(session, "pls", List.of());

        // Assert: emitRetrievalExecuted was called with the enriched query
        ArgumentCaptor<String> queryCaptor = ArgumentCaptor.forClass(String.class);
        verify(eventEmitter).emitRetrievalExecuted(
                eq(session.getSessionId()),
                anyInt(),
                queryCaptor.capture(),
                eq(false),
                eq(1)
        );

        String capturedQuery = queryCaptor.getValue();
        assertTrue(capturedQuery.contains("pls"),
                "Enriched query should still contain original message. Got: " + capturedQuery);
        assertTrue(capturedQuery.contains("Context:"),
                "Enriched query should contain Context separator (enrichment active). Got: " + capturedQuery);
        assertTrue(capturedQuery.contains("My ad was flagged"),
                "Enriched query should contain form description. Got: " + capturedQuery);
    }

    /**
     * When there is no form description and message is long, the query passed to
     * emitRetrievalExecuted should be the raw userMessage (no enrichment).
     */
    @Test
    void resolveFaq_retrievalExecutedEvent_noEnrichment_shouldPassRawMessage() {
        BotSession session = buildFaqSession("UC-A", null);

        stubUcDefinition("UC-A", "Account FAQ", "FAQ");

        ToolResult toolResult = ToolResult.ok(Map.of(
                "faq_miss", false,
                "retrieval_miss", false,
                "answer_miss", false,
                "hits", List.of(Map.of(
                        "source_id", "KB-002",
                        "title", "FAQ article",
                        "snippet", "Some content",
                        "score", 0.85
                ))
        ));
        when(toolDispatcher.dispatch(eq("search_knowledge"), eq(session), anyMap()))
                .thenReturn(toolResult);

        String projectionJson = "{\"phase\":\"RESOLVE\",\"use_case\":\"UC-A\"}";
        when(contextProjection.buildProjection(eq(session), anyList(), anyList(), anyString()))
                .thenReturn(projectionJson);

        LlmResponse llmResponse = LlmResponse.builder()
                .content("{\"action\":\"answer_grounded\",\"user_message\":\"Answer\"}")
                .build();
        when(llmInvocation.invokeChat(anyString(), anyString(), anyString(), anyInt()))
                .thenReturn(llmResponse);
        when(actionParser.parse(anyString())).thenReturn(
                ParsedAction.builder().action("answer_grounded").userMessage("Answer").build());

        String rawMessage = "How can I reset my password and recover my account?";
        // History has 3 turns (turnIndex = 3, not early) and message is long (>= 20 chars)
        List<BotTurn> history = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            history.add(BotTurn.builder().turnId("t-" + i).sessionId(session.getSessionId())
                    .turnIndex(i).userMessage("msg " + i).botResponse("resp " + i).build());
        }

        phaseEvaluator.evaluate(session, rawMessage, history);

        ArgumentCaptor<String> queryCaptor = ArgumentCaptor.forClass(String.class);
        verify(eventEmitter).emitRetrievalExecuted(
                eq(session.getSessionId()), anyInt(), queryCaptor.capture(), eq(false), eq(1));

        assertEquals(rawMessage, queryCaptor.getValue(),
                "Without enrichment, event should log the raw userMessage");
    }

    // --- Helper methods ---

    private BotSession buildFaqSession(String ucId, String formContext) {
        return BotSession.builder()
                .sessionId("test-faq-miss-session")
                .activeUseCase(ucId)
                .handlingState("BOT_HANDLING")
                .currentPhase("RESOLVE")
                .totalBotTurns(1)
                .clarificationCount(0)
                .faqMissCount(0)
                .repeatedActionCount(0)
                .formContext(formContext)
                .build();
    }

    private void stubUcDefinition(String ucId, String name, String path) {
        UseCaseRegistryService.UseCaseDefinition ucDef =
                new UseCaseRegistryService.UseCaseDefinition(
                        ucId, name, List.of(), "low", true, path);
        when(useCaseRegistry.getUseCase(ucId)).thenReturn(ucDef);
    }

    private void stubFaqMissToolResult() {
        ToolResult toolResult = ToolResult.ok(Map.of(
                "faq_miss", true,
                "retrieval_miss", false,
                "answer_miss", false,
                "hits", List.of()
        ));
        when(toolDispatcher.dispatch(eq("search_knowledge"), any(BotSession.class), anyMap()))
                .thenReturn(toolResult);
    }
}
