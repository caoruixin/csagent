package com.gumtree.csagent.service.runtime;

import com.gumtree.csagent.config.LlmProperties;
import com.gumtree.csagent.model.ChatMessage;
import com.gumtree.csagent.model.LlmRequest;
import com.gumtree.csagent.model.LlmResponse;
import com.gumtree.csagent.service.llm.LlmClient;
import com.gumtree.csagent.service.observability.LlmCallLogger;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Wraps LlmClient with context-aware request building, system prompt loading,
 * and graceful error handling (returns safe escalation response on LLM failure).
 */
@Slf4j
@Service
public class LlmInvocationService {

    private static final String SYSTEM_PROMPT_PATH = "prompts/system_prompt.txt";
    private static final String ROUTING_PROMPT_PATH = "prompts/routing_prompt.txt";

    private static final LlmResponse SAFE_ESCALATION_RESPONSE = LlmResponse.builder()
            .content("{\"user_message\":\"I apologize, but I'm experiencing a technical issue. " +
                     "Let me connect you with a human agent who can assist you.\"," +
                     "\"reasoning\":\"LLM invocation failure\"," +
                     "\"tool_calls\":[{\"name\":\"request_handover\"," +
                     "\"arguments\":{\"escalation_reason\":\"system_failure\"}}]}")
            .finishReason("error_fallback")
            .latencyMs(0)
            .build();

    private final LlmClient llmClient;
    private final LlmCallLogger llmCallLogger;
    private final String modelName;

    private String systemPromptTemplate;
    private String routingPromptTemplate;

    public LlmInvocationService(LlmClient llmClient, LlmCallLogger llmCallLogger,
                                 LlmProperties llmProperties) {
        this.llmClient = llmClient;
        this.llmCallLogger = llmCallLogger;
        this.modelName = llmProperties.getKimi().getModel();
    }

    @PostConstruct
    public void init() {
        systemPromptTemplate = loadPromptTemplate(SYSTEM_PROMPT_PATH);
        routingPromptTemplate = loadPromptTemplate(ROUTING_PROMPT_PATH);
        log.info("LLM prompts loaded: system_prompt={}chars, routing_prompt={}chars",
                systemPromptTemplate.length(), routingPromptTemplate.length());
    }

    /**
     * Invoke the LLM for a standard chat turn.
     *
     * @param projectedContext the JSON context string built by ContextProjectionBuilder
     * @param userMessage      the current user message
     * @param sessionId        session identifier for call logging
     * @param turnIndex        turn index for call logging
     * @return LlmResponse, or a safe escalation response on failure
     */
    public LlmResponse invokeChat(String projectedContext, String userMessage,
                                   String sessionId, int turnIndex) {
        long start = System.currentTimeMillis();
        String requestSummary = truncate(userMessage, 200);
        try {
            String systemPrompt = systemPromptTemplate + "\n\nCurrent context:\n" + projectedContext;

            LlmRequest request = LlmRequest.builder()
                    .systemPrompt(systemPrompt)
                    .messages(List.of(
                            ChatMessage.builder().role("user").content(userMessage).build()
                    ))
                    .temperature(0.3)
                    .maxTokens(1024)
                    .responseFormat("json_object")
                    .build();

            LlmResponse response = llmClient.chat(request);
            int elapsed = (int) (System.currentTimeMillis() - start);
            log.info("LLM [chat] response: latency={}ms, tokens={}/{}",
                    response.getLatencyMs(), response.getPromptTokens(), response.getCompletionTokens());

            llmCallLogger.log(sessionId, turnIndex, "chat", modelName,
                    response.getPromptTokens(), response.getCompletionTokens(),
                    elapsed, requestSummary, truncate(response.getContent(), 500));

            return response;

        } catch (Exception e) {
            int elapsed = (int) (System.currentTimeMillis() - start);
            log.error("LLM invocation failed, returning safe escalation: {}", e.getMessage(), e);
            llmCallLogger.logFailure(sessionId, turnIndex, "chat", modelName,
                    elapsed, requestSummary, e.getMessage());
            return SAFE_ESCALATION_RESPONSE;
        }
    }

    /**
     * Invoke the LLM for use case routing/classification.
     *
     * @param ucCandidates  formatted string describing candidate use cases
     * @param topicSubject  the customer's form topic
     * @param description   the customer's description
     * @param sessionId     session identifier for call logging
     * @return LlmResponse with classification result
     */
    public LlmResponse invokeRouting(String ucCandidates, String topicSubject,
                                      String description, String sessionId) {
        long start = System.currentTimeMillis();
        String requestSummary = truncate("topic=" + topicSubject + " desc=" + description, 200);
        try {
            String prompt = routingPromptTemplate
                    .replace("{uc_candidates}", ucCandidates)
                    .replace("{topic_subject}", topicSubject != null ? topicSubject : "")
                    .replace("{description}", description != null ? description : "");

            LlmRequest request = LlmRequest.builder()
                    .systemPrompt("You are a customer inquiry classifier. Respond only in JSON format.")
                    .messages(List.of(
                            ChatMessage.builder().role("user").content(prompt).build()
                    ))
                    .temperature(0.1)
                    .maxTokens(256)
                    .responseFormat("json_object")
                    .build();

            LlmResponse response = llmClient.chat(request);
            int elapsed = (int) (System.currentTimeMillis() - start);
            log.info("LLM [routing] response: latency={}ms", response.getLatencyMs());

            llmCallLogger.log(sessionId, null, "routing", modelName,
                    response.getPromptTokens(), response.getCompletionTokens(),
                    elapsed, requestSummary, truncate(response.getContent(), 500));

            return response;

        } catch (Exception e) {
            int elapsed = (int) (System.currentTimeMillis() - start);
            log.error("LLM routing invocation failed: {}", e.getMessage(), e);
            llmCallLogger.logFailure(sessionId, null, "routing", modelName,
                    elapsed, requestSummary, e.getMessage());
            return SAFE_ESCALATION_RESPONSE;
        }
    }

    public String getSystemPromptTemplate() {
        return systemPromptTemplate;
    }

    private String truncate(String text, int maxLen) {
        if (text == null) return null;
        return text.length() <= maxLen ? text : text.substring(0, maxLen);
    }

    private String loadPromptTemplate(String path) {
        try {
            ClassPathResource resource = new ClassPathResource(path);
            return new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.error("Failed to load prompt template from {}: {}", path, e.getMessage());
            throw new RuntimeException("Failed to load prompt template: " + path, e);
        }
    }
}
