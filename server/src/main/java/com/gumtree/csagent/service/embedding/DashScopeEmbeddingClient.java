package com.gumtree.csagent.service.embedding;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.gumtree.csagent.config.LlmProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

@Service
@Profile("local")
public class DashScopeEmbeddingClient implements EmbeddingClient {

    private static final Logger log = LoggerFactory.getLogger(DashScopeEmbeddingClient.class);

    private final LlmProperties llmProperties;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public DashScopeEmbeddingClient(LlmProperties llmProperties, ObjectMapper objectMapper) {
        this.llmProperties = llmProperties;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);   // 5 seconds connect timeout
        factory.setReadTimeout(10000);      // 10 seconds read timeout (embeddings are fast)
        this.restTemplate = new RestTemplate(factory);
        this.objectMapper = objectMapper;
    }

    @Override
    public float[] embed(String text) {
        List<float[]> results = embedBatch(List.of(text));
        return results.isEmpty() ? new float[0] : results.get(0);
    }

    @Override
    public List<float[]> embedBatch(List<String> texts) {
        LlmProperties.DashScopeProperties config = llmProperties.getDashscope();
        String url = config.getBaseUrl() + "/embeddings";

        try {
            ObjectNode body = objectMapper.createObjectNode();
            body.put("model", config.getEmbeddingModel());

            ArrayNode inputArray = objectMapper.createArrayNode();
            for (String text : texts) {
                inputArray.add(text);
            }
            body.set("input", inputArray);

            ObjectNode dimensions = objectMapper.createObjectNode();
            body.put("dimensions", config.getEmbeddingDimension());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(config.getApiKey());

            HttpEntity<String> entity = new HttpEntity<>(objectMapper.writeValueAsString(body), headers);
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

            return parseEmbeddingResponse(response.getBody());

        } catch (Exception e) {
            log.error("Embedding API call failed: {}", e.getMessage(), e);
            throw new RuntimeException("Embedding API call failed", e);
        }
    }

    private List<float[]> parseEmbeddingResponse(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode data = root.get("data");
            List<float[]> embeddings = new ArrayList<>();

            if (data != null && data.isArray()) {
                for (JsonNode item : data) {
                    JsonNode embeddingNode = item.get("embedding");
                    if (embeddingNode != null && embeddingNode.isArray()) {
                        float[] vector = new float[embeddingNode.size()];
                        for (int i = 0; i < embeddingNode.size(); i++) {
                            vector[i] = (float) embeddingNode.get(i).asDouble();
                        }
                        embeddings.add(vector);
                    }
                }
            }

            return embeddings;

        } catch (Exception e) {
            log.error("Failed to parse embedding response: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to parse embedding response", e);
        }
    }
}
