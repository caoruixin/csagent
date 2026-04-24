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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

@Service
public class OpenAiCompatibleLlmClient implements LlmClient {

    private static final Logger log = LoggerFactory.getLogger(OpenAiCompatibleLlmClient.class);

    private final LlmProperties llmProperties;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public OpenAiCompatibleLlmClient(LlmProperties llmProperties, ObjectMapper objectMapper) {
        this.llmProperties = llmProperties;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);   // 5 seconds connect timeout
        factory.setReadTimeout(30000);      // 30 seconds read timeout (LLM can be slow)
        this.restTemplate = new RestTemplate(factory);
        this.objectMapper = objectMapper;
    }

    @Override
    public LlmResponse chat(LlmRequest request) {
        // Try up to 2 times (initial + 1 retry) for transient failures
        Exception lastException = null;
        for (int attempt = 0; attempt < 2; attempt++) {
            try {
                return doChat(request);
            } catch (RestClientException e) {
                lastException = e;
                if (attempt == 0) {
                    log.warn("LLM API call failed on attempt 1, retrying in 500ms: {}", e.getMessage());
                    try { Thread.sleep(500); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); break; }
                }
            }
        }
        log.error("LLM API call failed after 2 attempts", lastException);
        throw new RuntimeException("LLM API call failed after retry", lastException);
    }

    private LlmResponse doChat(LlmRequest request) {
        long startTime = System.currentTimeMillis();

        LlmProperties.DashScopeProperties config = llmProperties.getDashscope();
        String url = config.getBaseUrl() + "/chat/completions";
        String apiKey = config.getApiKey();
        String model = config.getChatModel();

        // Fall back to Kimi if DashScope API key is not set
        if (apiKey == null || apiKey.isBlank()) {
            LlmProperties.KimiProperties kimiConfig = llmProperties.getKimi();
            url = kimiConfig.getBaseUrl() + "/chat/completions";
            apiKey = kimiConfig.getApiKey();
            model = kimiConfig.getModel();
        }

        try {
            ObjectNode body = buildRequestBody(request, model);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            HttpEntity<String> entity = new HttpEntity<>(objectMapper.writeValueAsString(body), headers);
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

            long latencyMs = System.currentTimeMillis() - startTime;
            return parseResponse(response.getBody(), latencyMs);

        } catch (RestClientException e) {
            throw e; // Let retry loop handle RestClientException
        } catch (Exception e) {
            log.error("Error processing LLM request: {}", e.getMessage(), e);
            throw new RuntimeException("Error processing LLM request", e);
        }
    }

    private ObjectNode buildRequestBody(LlmRequest request, String model) {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", model);
        body.put("temperature", request.getTemperature());
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
