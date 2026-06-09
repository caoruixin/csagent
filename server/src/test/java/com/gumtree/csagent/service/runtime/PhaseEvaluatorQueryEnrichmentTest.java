package com.gumtree.csagent.service.runtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gumtree.csagent.model.BotSession;
import com.gumtree.csagent.model.BotTurn;
import com.gumtree.csagent.service.guardrails.ScriptLibraryService;
import com.gumtree.csagent.service.knowledge.KnowledgeSearchService;
import com.gumtree.csagent.service.observability.EventEmitter;
import com.gumtree.csagent.service.tools.CreateCaseControlledTool;
import com.gumtree.csagent.service.tools.ToolDispatcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for PhaseEvaluator.enrichQueryWithFormContext() (D14.2).
 * Uses reflection to test the private method directly, since testing
 * indirectly via the full resolve flow would require too many mocks.
 */
@ExtendWith(MockitoExtension.class)
class PhaseEvaluatorQueryEnrichmentTest {

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
    private Method enrichMethod;

    @BeforeEach
    void setUp() throws Exception {
        phaseEvaluator = new PhaseEvaluator(
                useCaseRegistry, knowledgeSearchService, scriptLibrary,
                llmInvocation, contextProjection, actionParser,
                new ObjectMapper(), createCaseTool, eventEmitter, toolDispatcher,
                com.gumtree.csagent.service.runtime.skill.SkillTestFixtures.productionRegistry(), null);

        // Access the private enrichQueryWithFormContext method via reflection
        enrichMethod = PhaseEvaluator.class.getDeclaredMethod(
                "enrichQueryWithFormContext", String.class, BotSession.class, List.class);
        enrichMethod.setAccessible(true);
    }

    private String invokeEnrich(String userMessage, BotSession session, List<BotTurn> history)
            throws Exception {
        return (String) enrichMethod.invoke(phaseEvaluator, userMessage, session, history);
    }

    // --- C2: Short message + form description -> enriched query ---

    @Test
    void enrichQuery_shortMessage_withFormDescription_shouldEnrich() throws Exception {
        BotSession session = buildSession("{\"description\":\"My ad was removed unfairly and I need help restoring it\"}");
        List<BotTurn> history = Collections.emptyList(); // turnIndex = 0, early turn

        String result = invokeEnrich("Pls help", session, history);

        assertTrue(result.contains("Pls help"), "Should contain original message");
        assertTrue(result.contains("Context:"), "Should contain enrichment separator");
        assertTrue(result.contains("My ad was removed"), "Should contain form description");
    }

    @Test
    void enrichQuery_shortMessage_laterTurn_shouldEnrich() throws Exception {
        // Short message (< 20 chars) should trigger enrichment regardless of turn index
        BotSession session = buildSession("{\"description\":\"Payment issue details\"}");
        List<BotTurn> history = buildHistory(5); // turnIndex = 5, not early turn

        String result = invokeEnrich("help me", session, history);

        assertTrue(result.contains("help me"), "Should contain original message");
        assertTrue(result.contains("Context:"), "Short message should always trigger enrichment");
    }

    @Test
    void enrichQuery_earlyTurn_longMessage_shouldEnrich() throws Exception {
        // turnIndex <= 1, message >= 20 chars: should still enrich because it's an early turn
        BotSession session = buildSession("{\"description\":\"My ad was removed\"}");
        List<BotTurn> history = buildHistory(1); // turnIndex = 1, early turn

        String longMessage = "I have a question about my account settings and how they work";

        String result = invokeEnrich(longMessage, session, history);

        assertTrue(result.contains(longMessage), "Should contain original message");
        assertTrue(result.contains("Context:"), "Early turn should trigger enrichment even with long message");
    }

    // --- C2: Long message + turn > 1 -> original query (no enrichment) ---

    @Test
    void enrichQuery_longMessage_laterTurn_shouldNotEnrich() throws Exception {
        BotSession session = buildSession("{\"description\":\"Some description\"}");
        List<BotTurn> history = buildHistory(3); // turnIndex = 3, not early turn

        String longMessage = "I would like to know about my account billing details";

        String result = invokeEnrich(longMessage, session, history);

        assertEquals(longMessage, result, "Long message on later turn should not be enriched");
        assertFalse(result.contains("Context:"), "Should not contain enrichment");
    }

    @Test
    void enrichQuery_exactly20Chars_laterTurn_shouldNotEnrich() throws Exception {
        BotSession session = buildSession("{\"description\":\"Some description\"}");
        List<BotTurn> history = buildHistory(2); // turnIndex = 2

        // Exactly 20 chars: "12345678901234567890"
        String msg = "12345678901234567890";
        assertEquals(20, msg.length());

        String result = invokeEnrich(msg, session, history);

        assertEquals(msg, result, "Exactly 20-char message on turn > 1 should not be enriched");
    }

    // --- C2: Null/blank form context -> original query ---

    @Test
    void enrichQuery_nullFormContext_shouldReturnOriginal() throws Exception {
        BotSession session = buildSession(null);
        List<BotTurn> history = Collections.emptyList();

        String result = invokeEnrich("help", session, history);

        assertEquals("help", result, "Null formContext should return original");
    }

    @Test
    void enrichQuery_blankFormContext_shouldReturnOriginal() throws Exception {
        BotSession session = buildSession("   ");
        List<BotTurn> history = Collections.emptyList();

        String result = invokeEnrich("help", session, history);

        assertEquals("help", result, "Blank formContext should return original");
    }

    @Test
    void enrichQuery_emptyFormContext_shouldReturnOriginal() throws Exception {
        BotSession session = buildSession("");
        List<BotTurn> history = Collections.emptyList();

        String result = invokeEnrich("help", session, history);

        assertEquals("help", result, "Empty formContext should return original");
    }

    // --- C2: Form context without description field -> original query ---

    @Test
    void enrichQuery_formContextWithoutDescription_shouldReturnOriginal() throws Exception {
        BotSession session = buildSession("{\"email\":\"test@test.com\",\"name\":\"John\"}");
        List<BotTurn> history = Collections.emptyList();

        String result = invokeEnrich("help", session, history);

        assertEquals("help", result, "FormContext without description should return original");
    }

    @Test
    void enrichQuery_formContextWithNullDescription_shouldReturnOriginal() throws Exception {
        BotSession session = buildSession("{\"description\":null}");
        List<BotTurn> history = Collections.emptyList();

        String result = invokeEnrich("help", session, history);

        assertEquals("help", result, "Null description field should return original");
    }

    @Test
    void enrichQuery_formContextWithEmptyDescription_shouldReturnOriginal() throws Exception {
        BotSession session = buildSession("{\"description\":\"\"}");
        List<BotTurn> history = Collections.emptyList();

        String result = invokeEnrich("help", session, history);

        assertEquals("help", result, "Empty description should return original");
    }

    @Test
    void enrichQuery_formContextWithBlankDescription_shouldReturnOriginal() throws Exception {
        BotSession session = buildSession("{\"description\":\"   \"}");
        List<BotTurn> history = Collections.emptyList();

        String result = invokeEnrich("help", session, history);

        assertEquals("help", result, "Blank description should return original");
    }

    // --- Edge cases ---

    @Test
    void enrichQuery_malformedJson_shouldReturnOriginal() throws Exception {
        BotSession session = buildSession("{bad json}}");
        List<BotTurn> history = Collections.emptyList();

        String result = invokeEnrich("help", session, history);

        assertEquals("help", result, "Malformed JSON should return original gracefully");
    }

    @Test
    void enrichQuery_longDescription_shouldTruncateTo200Chars() throws Exception {
        String longDesc = "A".repeat(300);
        BotSession session = buildSession("{\"description\":\"" + longDesc + "\"}");
        List<BotTurn> history = Collections.emptyList();

        String result = invokeEnrich("help", session, history);

        // The enriched query should contain the original + separator + truncated description
        assertTrue(result.contains("help"), "Should contain original message");
        assertTrue(result.contains("Context:"), "Should be enriched");
        // Check that the description portion is truncated
        // "help | Context: " is ~17 chars, plus 200 chars of description = ~217 total
        String contextPart = result.substring(result.indexOf("Context: ") + "Context: ".length());
        assertEquals(200, contextPart.length(),
                "Description should be truncated to 200 characters");
    }

    @Test
    void enrichQuery_nullHistory_shouldTreatAsTurnZero() throws Exception {
        BotSession session = buildSession("{\"description\":\"Some issue description\"}");

        // Null history should be treated as turn 0 (early turn), triggering enrichment
        String result = invokeEnrich("help", session, null);

        assertTrue(result.contains("Context:"),
                "Null history should be treated as turn 0 (early turn) and trigger enrichment");
    }

    private BotSession buildSession(String formContext) {
        return BotSession.builder()
                .sessionId("test-session-enrich")
                .activeUseCase("UC-A")
                .handlingState("BOT_HANDLING")
                .currentPhase("RESOLVE")
                .totalBotTurns(1)
                .clarificationCount(0)
                .faqMissCount(0)
                .repeatedActionCount(0)
                .formContext(formContext)
                .build();
    }

    private List<BotTurn> buildHistory(int turnCount) {
        List<BotTurn> history = new ArrayList<>();
        for (int i = 0; i < turnCount; i++) {
            history.add(BotTurn.builder()
                    .turnId("turn-" + i)
                    .sessionId("test-session-enrich")
                    .turnIndex(i)
                    .userMessage("message " + i)
                    .botResponse("response " + i)
                    .build());
        }
        return history;
    }
}
