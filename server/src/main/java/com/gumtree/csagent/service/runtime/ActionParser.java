package com.gumtree.csagent.service.runtime;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gumtree.csagent.model.ParsedAction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;

/**
 * Parses LLM JSON responses into ParsedAction objects.
 * Falls back to escalate_human on any parse failure.
 */
@Slf4j
@Service
public class ActionParser {

    private static final Set<String> VALID_ACTIONS = Set.of(
            "ask_user", "retrieve_knowledge", "answer_grounded", "escalate_human", "finish"
    );

    private static final ParsedAction FALLBACK_ESCALATION = ParsedAction.builder()
            .action("escalate_human")
            .parameters(Map.of())
            .userMessage("I'm having trouble processing your request. Let me connect you with a human agent who can help.")
            .reasoning("LLM response parse failure - fallback to escalation")
            .build();

    private final ObjectMapper objectMapper;

    public ActionParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Parse LLM raw response JSON into a ParsedAction.
     * Expected format: {"action": "...", "parameters": {...}, "user_message": "...", "reasoning": "..."}
     */
    public ParsedAction parse(String llmRawResponse) {
        if (llmRawResponse == null || llmRawResponse.isBlank()) {
            log.warn("Empty LLM response, falling back to escalation");
            return FALLBACK_ESCALATION;
        }

        try {
            // Strip markdown code fences if present
            String cleaned = cleanResponse(llmRawResponse);

            JsonNode root = objectMapper.readTree(cleaned);

            String action = root.has("action") ? root.get("action").asText("") : "";
            if (!VALID_ACTIONS.contains(action)) {
                log.warn("Invalid action '{}' in LLM response, falling back to escalation", action);
                return FALLBACK_ESCALATION;
            }

            Map<String, Object> parameters = Map.of();
            if (root.has("parameters") && root.get("parameters").isObject()) {
                parameters = objectMapper.convertValue(
                        root.get("parameters"),
                        new TypeReference<Map<String, Object>>() {}
                );
            }

            String userMessage = root.has("user_message") ? root.get("user_message").asText("") : "";
            String reasoning = root.has("reasoning") ? root.get("reasoning").asText("") : "";

            if (userMessage.isBlank()) {
                log.warn("Empty user_message in LLM response for action '{}', using fallback message", action);
                userMessage = "I'm looking into this for you.";
            }

            return ParsedAction.builder()
                    .action(action)
                    .parameters(parameters)
                    .userMessage(userMessage)
                    .reasoning(reasoning)
                    .build();

        } catch (Exception e) {
            log.warn("Failed to parse LLM response: {}", e.getMessage());
            return FALLBACK_ESCALATION;
        }
    }

    /**
     * Clean LLM response by stripping markdown code fences and whitespace.
     */
    private String cleanResponse(String raw) {
        String trimmed = raw.trim();
        // Remove ```json ... ``` wrapping
        if (trimmed.startsWith("```")) {
            int firstNewline = trimmed.indexOf('\n');
            if (firstNewline > 0) {
                trimmed = trimmed.substring(firstNewline + 1);
            }
            if (trimmed.endsWith("```")) {
                trimmed = trimmed.substring(0, trimmed.length() - 3);
            }
            trimmed = trimmed.trim();
        }
        return trimmed;
    }
}
