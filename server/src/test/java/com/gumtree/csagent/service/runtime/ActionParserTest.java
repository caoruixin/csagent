package com.gumtree.csagent.service.runtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gumtree.csagent.model.ParsedAction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ActionParser.
 * Validates JSON parsing, markdown stripping, and fallback behavior.
 */
class ActionParserTest {

    private ActionParser parser;

    @BeforeEach
    void setUp() {
        parser = new ActionParser(new ObjectMapper());
    }

    @Test
    void parse_validAskUserJson_shouldReturnParsedAction() {
        String json = """
                {"action":"ask_user","parameters":{},"user_message":"How can I help?","reasoning":"test"}""";

        ParsedAction result = parser.parse(json);

        assertEquals("ask_user", result.getAction());
        assertEquals("How can I help?", result.getUserMessage());
        assertEquals("test", result.getReasoning());
        assertNotNull(result.getParameters());
    }

    @Test
    void parse_validEscalateHumanJson_shouldReturnParsedAction() {
        String json = """
                {"action":"escalate_human","parameters":{},"user_message":"Connecting you.","reasoning":"needs help"}""";

        ParsedAction result = parser.parse(json);

        assertEquals("escalate_human", result.getAction());
        assertEquals("Connecting you.", result.getUserMessage());
    }

    @Test
    void parse_validAnswerGroundedJson_shouldReturnParsedAction() {
        String json = """
                {"action":"answer_grounded","parameters":{"source":"FAQ-1"},"user_message":"Here is the answer.","reasoning":"found in KB"}""";

        ParsedAction result = parser.parse(json);

        assertEquals("answer_grounded", result.getAction());
        assertTrue(result.getParameters().containsKey("source"));
    }

    @Test
    void parse_nullInput_shouldReturnFallbackEscalation() {
        ParsedAction result = parser.parse(null);

        assertEquals("escalate_human", result.getAction());
        assertNotNull(result.getUserMessage());
        assertFalse(result.getUserMessage().isEmpty());
    }

    @Test
    void parse_emptyInput_shouldReturnFallbackEscalation() {
        ParsedAction result = parser.parse("");

        assertEquals("escalate_human", result.getAction());
    }

    @Test
    void parse_blankInput_shouldReturnFallbackEscalation() {
        ParsedAction result = parser.parse("   ");

        assertEquals("escalate_human", result.getAction());
    }

    @Test
    void parse_invalidJson_shouldReturnFallbackEscalation() {
        ParsedAction result = parser.parse("this is not json");

        assertEquals("escalate_human", result.getAction());
    }

    @Test
    void parse_invalidAction_shouldReturnFallbackEscalation() {
        String json = """
                {"action":"invalid_action","parameters":{},"user_message":"test","reasoning":"test"}""";

        ParsedAction result = parser.parse(json);

        assertEquals("escalate_human", result.getAction());
    }

    @Test
    void parse_markdownWrappedJson_shouldStripAndParse() {
        String json = """
                ```json
                {"action":"ask_user","parameters":{},"user_message":"Hello!","reasoning":"test"}
                ```""";

        ParsedAction result = parser.parse(json);

        assertEquals("ask_user", result.getAction());
        assertEquals("Hello!", result.getUserMessage());
    }

    @Test
    void parse_emptyUserMessage_shouldUseFallbackMessage() {
        String json = """
                {"action":"ask_user","parameters":{},"user_message":"","reasoning":"test"}""";

        ParsedAction result = parser.parse(json);

        assertEquals("ask_user", result.getAction());
        assertFalse(result.getUserMessage().isEmpty(),
                "Should use fallback message when user_message is empty");
    }

    @Test
    void parse_missingUserMessageField_shouldUseFallbackMessage() {
        String json = """
                {"action":"ask_user","parameters":{},"reasoning":"test"}""";

        ParsedAction result = parser.parse(json);

        assertEquals("ask_user", result.getAction());
        assertFalse(result.getUserMessage().isEmpty());
    }

    @Test
    void parse_missingParametersField_shouldDefaultToEmptyMap() {
        String json = """
                {"action":"finish","user_message":"Done.","reasoning":"test"}""";

        ParsedAction result = parser.parse(json);

        assertEquals("finish", result.getAction());
        assertNotNull(result.getParameters());
    }

    @Test
    void parse_retrieveKnowledgeAction_shouldBeValid() {
        String json = """
                {"action":"retrieve_knowledge","parameters":{"query":"how to post ad"},"user_message":"Let me check.","reasoning":"searching"}""";

        ParsedAction result = parser.parse(json);

        assertEquals("retrieve_knowledge", result.getAction());
    }

    @Test
    void parse_finishAction_shouldBeValid() {
        String json = """
                {"action":"finish","parameters":{},"user_message":"Goodbye!","reasoning":"user confirmed"}""";

        ParsedAction result = parser.parse(json);

        assertEquals("finish", result.getAction());
    }
}
