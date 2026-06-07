package com.gumtree.csagent.service.runtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gumtree.csagent.model.ParsedAction;
import com.gumtree.csagent.model.ToolCall;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link ActionParser} under the OpenAI-style tool-use contract:
 * {@code {user_message, reasoning, tool_calls:[{name, arguments}]}}.
 *
 * <p>Task #10: synthetic legacy {@code action} / {@code parameters} fields were
 * removed from {@link ParsedAction}; assertions now target {@code toolCalls},
 * {@code userMessage}, and {@code reasoning} only.
 */
class ActionParserTest {

    private ActionParser parser;

    @BeforeEach
    void setUp() {
        parser = new ActionParser(new ObjectMapper());
    }

    // ---------- valid tool_calls: knowledge retrieval ----------

    @Test
    void parse_searchKnowledgeToolCall_shouldReturnToolCall() {
        String json = """
                {"user_message":"Let me check that for you.",
                 "reasoning":"need KB lookup",
                 "tool_calls":[{"name":"search_knowledge","arguments":{"query":"how to post ad"}}]}""";

        ParsedAction result = parser.parse(json);

        List<ToolCall> tcs = result.getToolCalls();
        assertEquals(1, tcs.size());
        assertEquals("search_knowledge", tcs.get(0).getName());
        assertEquals("how to post ad", tcs.get(0).getArguments().get("query"));
        assertEquals("Let me check that for you.", result.getUserMessage());
        assertEquals("need KB lookup", result.getReasoning());
    }

    @Test
    void parse_resolveArticleToolCall_shouldReturnToolCall() {
        String json = """
                {"user_message":"",
                 "reasoning":"opening selected article",
                 "tool_calls":[{"name":"resolve_article","arguments":{"article_id":"FAQ-42"}}]}""";

        ParsedAction result = parser.parse(json);

        assertEquals(1, result.getToolCalls().size());
        assertEquals("resolve_article", result.getToolCalls().get(0).getName());
        assertEquals("FAQ-42", result.getToolCalls().get(0).getArguments().get("article_id"));
    }

    // ---------- valid tool_calls: handover ----------

    @Test
    void parse_requestHandoverToolCall_shouldReturnToolCall() {
        String json = """
                {"user_message":"Connecting you to a human agent now.",
                 "reasoning":"user requested escalation",
                 "tool_calls":[{"name":"request_handover","arguments":{"escalation_reason":"user_explicit_request"}}]}""";

        ParsedAction result = parser.parse(json);

        assertEquals(1, result.getToolCalls().size());
        assertEquals("request_handover", result.getToolCalls().get(0).getName());
        assertEquals("user_explicit_request",
                result.getToolCalls().get(0).getArguments().get("escalation_reason"));
        assertEquals("Connecting you to a human agent now.", result.getUserMessage());
    }

    // ---------- valid tool_calls: record_outcome (finish) ----------

    @Test
    void parse_recordOutcomeOnly_shouldReturnToolCall() {
        String json = """
                {"user_message":"Glad I could help! Have a great day.",
                 "reasoning":"user confirmed resolved",
                 "tool_calls":[{"name":"record_outcome","arguments":{"outcome_class":"resolved_by_bot"}}]}""";

        ParsedAction result = parser.parse(json);

        assertEquals(1, result.getToolCalls().size());
        assertEquals("record_outcome", result.getToolCalls().get(0).getName());
        assertEquals("resolved_by_bot",
                result.getToolCalls().get(0).getArguments().get("outcome_class"));
    }

    // ---------- empty tool_calls: clarification ----------

    @Test
    void parse_emptyToolCallsWithClarifyingQuestion_shouldReturnEmptyToolCalls() {
        String json = """
                {"user_message":"Could you tell me the ad ID so I can look it up?",
                 "reasoning":"need slot",
                 "tool_calls":[]}""";

        ParsedAction result = parser.parse(json);

        assertNotNull(result.getToolCalls());
        assertTrue(result.getToolCalls().isEmpty(), "tool_calls should be empty");
        assertEquals("Could you tell me the ad ID so I can look it up?", result.getUserMessage());
    }

    @Test
    void parse_emptyToolCallsWithQuestionMark_shouldReturnEmptyToolCalls() {
        String json = """
                {"user_message":"Which ad are you referring to?",
                 "reasoning":"clarify scope",
                 "tool_calls":[]}""";

        ParsedAction result = parser.parse(json);

        assertTrue(result.getToolCalls().isEmpty());
        assertEquals("Which ad are you referring to?", result.getUserMessage());
    }

    // ---------- empty tool_calls: grounded answer ----------

    @Test
    void parse_emptyToolCallsWithAnswer_shouldReturnEmptyToolCalls() {
        String json = """
                {"user_message":"To post an ad, sign in, click 'Post Ad', and follow the steps.",
                 "reasoning":"answering from prior retrieval context",
                 "tool_calls":[]}""";

        ParsedAction result = parser.parse(json);

        assertNotNull(result.getToolCalls());
        assertTrue(result.getToolCalls().isEmpty());
        assertEquals("To post an ad, sign in, click 'Post Ad', and follow the steps.", result.getUserMessage());
    }

    // ---------- markdown stripping ----------

    @Test
    void parse_markdownWrappedJson_shouldStripAndParse() {
        String json = """
                ```json
                {"user_message":"Hello!","reasoning":"greet","tool_calls":[]}
                ```""";

        ParsedAction result = parser.parse(json);

        assertEquals("Hello!", result.getUserMessage());
        assertTrue(result.getToolCalls().isEmpty());
    }

    // ---------- fallback paths ----------

    @Test
    void parse_nullInput_shouldReturnFallbackHandover() {
        ParsedAction result = parser.parse(null);

        assertEquals(1, result.getToolCalls().size());
        assertEquals("request_handover", result.getToolCalls().get(0).getName());
        assertNotNull(result.getUserMessage());
        assertFalse(result.getUserMessage().isEmpty());
    }

    @Test
    void parse_emptyInput_shouldReturnFallbackHandover() {
        ParsedAction result = parser.parse("");

        assertEquals("request_handover", result.getToolCalls().get(0).getName());
    }

    @Test
    void parse_blankInput_shouldReturnFallbackHandover() {
        ParsedAction result = parser.parse("   ");

        assertEquals("request_handover", result.getToolCalls().get(0).getName());
    }

    @Test
    void parse_invalidJson_shouldReturnFallbackHandover() {
        ParsedAction result = parser.parse("this is not json");

        assertEquals(1, result.getToolCalls().size());
        assertEquals("request_handover", result.getToolCalls().get(0).getName());
        assertEquals("system_failure",
                result.getToolCalls().get(0).getArguments().get("escalation_reason"));
    }

    // ---------- field tolerance ----------

    @Test
    void parse_missingToolCallsField_shouldDefaultToEmpty() {
        String json = """
                {"user_message":"What ad are you referring to?","reasoning":"clarify"}""";

        ParsedAction result = parser.parse(json);

        assertNotNull(result.getToolCalls());
        assertTrue(result.getToolCalls().isEmpty());
        assertEquals("What ad are you referring to?", result.getUserMessage());
    }

    @Test
    void parse_emptyUserMessageAndEmptyToolCalls_shouldUseFallbackMessage() {
        String json = """
                {"user_message":"","reasoning":"silent","tool_calls":[]}""";

        ParsedAction result = parser.parse(json);

        assertFalse(result.getUserMessage().isEmpty(),
                "Should use fallback message when both tool_calls and user_message are empty");
    }

    // ---------- S-Auto-30: synthesised-null-turn provenance flag ----------

    @Test
    void parse_emptyUserMessageAndEmptyToolCalls_marksUserMessageSynthesised() {
        // Test #1: a true null turn (blank user_message + no tool_calls) →
        // ActionParser substitutes the placeholder AND flags it as
        // runtime-synthesised, so the loop can exclude it from the R2.a
        // clarification counter.
        String json = """
                {"user_message":"","reasoning":"silent","tool_calls":[]}""";

        ParsedAction result = parser.parse(json);

        assertEquals("I'm looking into this for you.", result.getUserMessage(),
                "null turn must substitute the runtime placeholder");
        assertTrue(result.isUserMessageSynthesised(),
                "null-turn placeholder must be flagged userMessageSynthesised=true");
    }

    @Test
    void parse_blankUserMessageWhitespace_alsoMarksSynthesised() {
        // user_message that is whitespace-only is also blank → synthesised.
        String json = """
                {"user_message":"   ","reasoning":"x","tool_calls":[]}""";

        ParsedAction result = parser.parse(json);

        assertEquals("I'm looking into this for you.", result.getUserMessage());
        assertTrue(result.isUserMessageSynthesised());
    }

    @Test
    void parse_genuineClarification_isNotSynthesised() {
        // Negative control: an LLM-authored clarification keeps the flag false,
        // so it still counts toward the clarification budget.
        String json = """
                {"user_message":"Which ad are you referring to?",
                 "reasoning":"clarify scope","tool_calls":[]}""";

        ParsedAction result = parser.parse(json);

        assertFalse(result.isUserMessageSynthesised(),
                "an LLM-authored reply must NOT be flagged synthesised");
    }

    @Test
    void parse_toolCallResponse_isNotSynthesised() {
        // Negative control: a normal tool-call response is never synthesised.
        String json = """
                {"user_message":"",
                 "reasoning":"need KB lookup",
                 "tool_calls":[{"name":"search_knowledge","arguments":{"query":"x"}}]}""";

        ParsedAction result = parser.parse(json);

        assertFalse(result.isUserMessageSynthesised());
    }

    @Test
    void parse_parseFailureHandoverFallback_isNotSynthesised() {
        // Negative control: the parse-failure handover apology (buildFallback)
        // is a DIFFERENT case and must leave the flag false — only the
        // null-turn placeholder is synthesised.
        ParsedAction result = parser.parse("this is not json");

        assertEquals("request_handover", result.getToolCalls().get(0).getName());
        assertFalse(result.isUserMessageSynthesised(),
                "parse-failure handover apology must NOT be flagged synthesised");
    }

    @Test
    void parse_toolCallWithoutArguments_shouldDefaultToEmptyMap() {
        String json = """
                {"user_message":"","reasoning":"x",
                 "tool_calls":[{"name":"request_handover"}]}""";

        ParsedAction result = parser.parse(json);

        assertEquals(1, result.getToolCalls().size());
        assertEquals("request_handover", result.getToolCalls().get(0).getName());
        assertNotNull(result.getToolCalls().get(0).getArguments());
        assertTrue(result.getToolCalls().get(0).getArguments().isEmpty());
    }
}
