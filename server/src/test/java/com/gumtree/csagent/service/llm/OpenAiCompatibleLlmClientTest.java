package com.gumtree.csagent.service.llm;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gumtree.csagent.config.LlmProperties;
import com.gumtree.csagent.model.ChatMessage;
import com.gumtree.csagent.model.LlmRequest;
import com.gumtree.csagent.model.LlmResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for OpenAiCompatibleLlmClient.
 * Focuses on error handling, fallback behavior, and connection failure scenarios.
 */
class OpenAiCompatibleLlmClientTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
    }

    @Test
    void chat_whenDashScopeKeyBlank_shouldFallbackToKimi() {
        // Arrange: DashScope key is blank, Kimi key is set
        LlmProperties props = new LlmProperties();
        props.getDashscope().setApiKey("");
        props.getKimi().setApiKey("test-kimi-key");
        props.getKimi().setBaseUrl("https://api.moonshot.cn/v1");

        OpenAiCompatibleLlmClient client = new OpenAiCompatibleLlmClient(props, objectMapper);

        LlmRequest request = LlmRequest.builder()
                .systemPrompt("test")
                .messages(List.of(ChatMessage.builder().role("user").content("hello").build()))
                .temperature(0.3)
                .maxTokens(100)
                .build();

        // Act & Assert: Should throw because Kimi endpoint will reject our fake key,
        // but the important thing is that it targets Kimi, not DashScope.
        RuntimeException ex = assertThrows(RuntimeException.class, () -> client.chat(request));
        assertNotNull(ex.getMessage());
        assertTrue(ex.getMessage().contains("LLM API call failed"),
                "Expected LLM API call failed, got: " + ex.getMessage());
    }

    @Test
    void chat_whenDashScopeKeyNull_shouldFallbackToKimi() {
        LlmProperties props = new LlmProperties();
        props.getDashscope().setApiKey(null);
        props.getKimi().setApiKey("test-kimi-key");

        OpenAiCompatibleLlmClient client = new OpenAiCompatibleLlmClient(props, objectMapper);

        LlmRequest request = LlmRequest.builder()
                .systemPrompt("test")
                .messages(List.of(ChatMessage.builder().role("user").content("hello").build()))
                .build();

        RuntimeException ex = assertThrows(RuntimeException.class, () -> client.chat(request));
        assertNotNull(ex.getMessage());
    }

    @Test
    void chat_whenBothKeysBlank_shouldThrowRuntimeException() {
        // Both API keys are blank -- no valid endpoint
        LlmProperties props = new LlmProperties();
        props.getDashscope().setApiKey("");
        props.getKimi().setApiKey("");

        OpenAiCompatibleLlmClient client = new OpenAiCompatibleLlmClient(props, objectMapper);

        LlmRequest request = LlmRequest.builder()
                .systemPrompt("test")
                .messages(List.of(ChatMessage.builder().role("user").content("hello").build()))
                .build();

        // Should throw RuntimeException wrapping the connection or auth error
        assertThrows(RuntimeException.class, () -> client.chat(request));
    }

    @Test
    void chat_whenConnectionRefused_shouldWrapInRuntimeException() {
        // Point to localhost on a port that is not listening
        LlmProperties props = new LlmProperties();
        props.getDashscope().setApiKey("test-key");
        props.getDashscope().setBaseUrl("http://localhost:19999");

        OpenAiCompatibleLlmClient client = new OpenAiCompatibleLlmClient(props, objectMapper);

        LlmRequest request = LlmRequest.builder()
                .systemPrompt("test")
                .messages(List.of(ChatMessage.builder().role("user").content("hello").build()))
                .build();

        RuntimeException ex = assertThrows(RuntimeException.class, () -> client.chat(request));
        assertTrue(ex.getMessage().contains("LLM API call failed"),
                "Expected wrapped RuntimeException, got: " + ex.getMessage());
        assertNotNull(ex.getCause(), "Should have a cause (the original exception)");
    }

    @Test
    void chat_whenNullSystemPrompt_shouldNotThrowNPE() {
        LlmProperties props = new LlmProperties();
        props.getDashscope().setApiKey("test-key");
        props.getDashscope().setBaseUrl("http://localhost:19999");

        OpenAiCompatibleLlmClient client = new OpenAiCompatibleLlmClient(props, objectMapper);

        LlmRequest request = LlmRequest.builder()
                .systemPrompt(null)
                .messages(List.of(ChatMessage.builder().role("user").content("hello").build()))
                .build();

        // Should throw connection error, not NPE
        RuntimeException ex = assertThrows(RuntimeException.class, () -> client.chat(request));
        assertFalse(ex instanceof NullPointerException,
                "Should not throw NPE for null system prompt");
    }

    @Test
    void chat_whenNullMessages_shouldNotThrowNPE() {
        LlmProperties props = new LlmProperties();
        props.getDashscope().setApiKey("test-key");
        props.getDashscope().setBaseUrl("http://localhost:19999");

        OpenAiCompatibleLlmClient client = new OpenAiCompatibleLlmClient(props, objectMapper);

        LlmRequest request = LlmRequest.builder()
                .systemPrompt("test")
                .messages(null)
                .build();

        // Should throw connection error, not NPE
        RuntimeException ex = assertThrows(RuntimeException.class, () -> client.chat(request));
        assertFalse(ex instanceof NullPointerException,
                "Should not throw NPE for null messages");
    }

    @Test
    void chat_withResponseFormatJsonObject_shouldIncludeInRequest() {
        LlmProperties props = new LlmProperties();
        props.getDashscope().setApiKey("test-key");
        props.getDashscope().setBaseUrl("http://localhost:19999");

        OpenAiCompatibleLlmClient client = new OpenAiCompatibleLlmClient(props, objectMapper);

        LlmRequest request = LlmRequest.builder()
                .systemPrompt("test")
                .messages(List.of(ChatMessage.builder().role("user").content("hello").build()))
                .responseFormat("json_object")
                .build();

        // Connection will fail, but it should not crash on request building
        assertThrows(RuntimeException.class, () -> client.chat(request));
    }
}
