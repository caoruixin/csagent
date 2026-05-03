package com.gumtree.csagent.service.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.gumtree.csagent.config.LlmProperties;
import com.gumtree.csagent.model.ChatMessage;
import com.gumtree.csagent.model.LlmRequest;
import com.gumtree.csagent.model.LlmResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;

/**
 * Generic OpenAI-compatible chat-completion client. The same class wires either
 * Kimi (primary) or DeepSeek (fallback); the active provider is chosen by the
 * Spring config that constructs the bean (see {@link com.gumtree.csagent.config.LlmClientConfig}).
 */
public class OpenAiCompatibleLlmClient implements LlmClient {

    private static final Logger log = LoggerFactory.getLogger(OpenAiCompatibleLlmClient.class);

    private final String providerLabel;
    private final String apiKey;
    private final String baseUrl;
    private final String model;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public OpenAiCompatibleLlmClient(String providerLabel, String apiKey, String baseUrl,
                                     String model, ObjectMapper objectMapper) {
        this.providerLabel = providerLabel;
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
        this.model = model;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);   // 5 seconds connect timeout
        factory.setReadTimeout(30000);      // 30 seconds read timeout (LLM can be slow)
        this.restTemplate = new RestTemplate(factory);
        this.objectMapper = objectMapper;
    }

    /**
     * @deprecated Legacy constructor preserved for test-source backward compatibility only.
     *             Production wiring uses the explicit (providerLabel, apiKey, baseUrl, model, objectMapper)
     *             constructor via {@code LlmClientConfig}. This shim picks Kimi if its key is set,
     *             else DashScope — matching the pre-Step-4 behaviour of the deleted in-method fallback.
     */
    @Deprecated
    public OpenAiCompatibleLlmClient(LlmProperties llmProperties, ObjectMapper objectMapper) {
        this(
            (llmProperties.getKimi().getApiKey() != null && !llmProperties.getKimi().getApiKey().isBlank())
                ? "kimi" : "dashscope",
            (llmProperties.getKimi().getApiKey() != null && !llmProperties.getKimi().getApiKey().isBlank())
                ? llmProperties.getKimi().getApiKey() : llmProperties.getDashscope().getApiKey(),
            (llmProperties.getKimi().getApiKey() != null && !llmProperties.getKimi().getApiKey().isBlank())
                ? llmProperties.getKimi().getBaseUrl() : llmProperties.getDashscope().getBaseUrl(),
            (llmProperties.getKimi().getApiKey() != null && !llmProperties.getKimi().getApiKey().isBlank())
                ? llmProperties.getKimi().getModel() : llmProperties.getDashscope().getChatModel(),
            objectMapper
        );
    }

    @Override
    public LlmResponse chat(LlmRequest request) {
        // Try up to 2 times (initial + 1 retry) for transient failures within the same provider.
        // Cross-provider fallback is handled by FallbackLlmClient one layer up.
        Exception lastException = null;
        for (int attempt = 0; attempt < 2; attempt++) {
            try {
                return doChat(request);
            } catch (RestClientException e) {
                lastException = e;
                if (attempt == 0) {
                    log.warn("LLM API call failed on attempt 1 (provider={}), retrying in 500ms: {}",
                            providerLabel, e.getMessage());
                    try { Thread.sleep(500); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); break; }
                }
            }
        }
        log.error("LLM API call failed after 2 attempts (provider={})", providerLabel, lastException);
        throw new RuntimeException("LLM API call failed after retry (provider=" + providerLabel + ")", lastException);
    }

    private LlmResponse doChat(LlmRequest request) {
        long startTime = System.currentTimeMillis();
        String url = baseUrl + "/chat/completions";

        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("LLM provider " + providerLabel + " has no api-key configured");
        }

        log.info("LLM request: provider={}, model={}, url={}", providerLabel, model, url);

        try {
            ObjectNode body = buildRequestBody(request, model);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            HttpEntity<String> entity = new HttpEntity<>(objectMapper.writeValueAsString(body), headers);
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

            long latencyMs = System.currentTimeMillis() - startTime;
            LlmResponse llmResponse = parseResponse(response.getBody(), latencyMs);
            log.info("LLM response: provider={}, model={}, latency={}ms, tokens={}/{}",
                    providerLabel, model, latencyMs, llmResponse.getPromptTokens(), llmResponse.getCompletionTokens());
            return llmResponse;

        } catch (HttpStatusCodeException e) {
            HttpStatusCode status = e.getStatusCode();
            log.error("LLM HTTP error (provider={}, model={}): status={}, body={}",
                    providerLabel, model, status.value(), e.getResponseBodyAsString());
            throw e; // Surface to caller; FallbackLlmClient inspects status for transient classification.
        } catch (RestClientException e) {
            throw e; // Let retry loop handle other RestClientException (timeouts, connect failures).
        } catch (Exception e) {
            log.error("Error processing LLM request (provider={}): {}", providerLabel, e.getMessage(), e);
            throw new RuntimeException("Error processing LLM request (provider=" + providerLabel + ")", e);
        }
    }

    private ObjectNode buildRequestBody(LlmRequest request, String model) {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", model);

        boolean isKimiK2 = model != null && model.startsWith("kimi-k2");
        if (isKimiK2) {
            // K2.6/K2.5 enforce fixed temperature values; skip it and let the API use its default.
            // Disable thinking for structured output scenarios (JSON actions, routing, reranking).
            ObjectNode thinking = objectMapper.createObjectNode();
            thinking.put("type", "disabled");
            body.set("thinking", thinking);
        } else {
            body.put("temperature", request.getTemperature());
        }

        body.put("max_tokens", request.getMaxTokens());

        if (request.getResponseFormat() != null && !request.getResponseFormat().isBlank()) {
            ObjectNode responseFormat = objectMapper.createObjectNode();
            responseFormat.put("type", request.getResponseFormat());
            body.set("response_format", responseFormat);
        }

        ArrayNode messages = objectMapper.createArrayNode();

        // Add system prompt as first message
        if (request.getSystemPrompt() != null && !request.getSystemPrompt().isBlank()) {
            ObjectNode systemMsg = objectMapper.createObjectNode();
            systemMsg.put("role", "system");
            systemMsg.put("content", request.getSystemPrompt());
            messages.add(systemMsg);
        }

        // Add conversation messages
        if (request.getMessages() != null) {
            for (ChatMessage msg : request.getMessages()) {
                ObjectNode msgNode = objectMapper.createObjectNode();
                msgNode.put("role", msg.getRole());
                msgNode.put("content", msg.getContent());
                messages.add(msgNode);
            }
        }

        body.set("messages", messages);
        return body;
    }

    private LlmResponse parseResponse(String responseBody, long latencyMs) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode choices = root.get("choices");
            String content = "";
            String finishReason = "";

            if (choices != null && choices.isArray() && !choices.isEmpty()) {
                JsonNode firstChoice = choices.get(0);
                JsonNode message = firstChoice.get("message");
                if (message != null && message.has("content")) {
                    content = message.get("content").asText("");
                }
                if (firstChoice.has("finish_reason")) {
                    finishReason = firstChoice.get("finish_reason").asText("");
                }
            }

            int promptTokens = 0;
            int completionTokens = 0;
            JsonNode usage = root.get("usage");
            if (usage != null) {
                promptTokens = usage.path("prompt_tokens").asInt(0);
                completionTokens = usage.path("completion_tokens").asInt(0);
            }

            return LlmResponse.builder()
                    .content(content)
                    .finishReason(finishReason)
                    .promptTokens(promptTokens)
                    .completionTokens(completionTokens)
                    .latencyMs(latencyMs)
                    .build();

        } catch (Exception e) {
            log.error("Failed to parse LLM response: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to parse LLM response", e);
        }
    }
}
