package com.gumtree.csagent.service.runtime;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gumtree.csagent.model.ParsedAction;
import com.gumtree.csagent.model.ToolCall;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Parses LLM JSON responses (OpenAI-style tool-use shape) into ParsedAction objects.
 *
 * <p>Expected response shape (Task #8 contract, see phase3 §3.3.3):
 * <pre>{@code
 * {
 *   "user_message": "...",     // string, may be empty
 *   "reasoning":    "...",     // string, internal
 *   "tool_calls":   [          // array, may be empty
 *     {"name": "search_knowledge", "arguments": {"query": "..."}}
 *   ]
 * }
 * }</pre>
 *
 * <p>Falls back to a {@code request_handover} tool call on any parse failure.
 *
 * <p>Tool-name validity is NOT enforced here — that is the responsibility of
 * {@code ToolPolicyEnforcer} downstream.
 *
 * <p>Task #10: synthetic legacy-action derivation removed; this parser now only
 * fills {@code toolCalls}, {@code userMessage}, {@code reasoning}.
 */
@Slf4j
@Service
public class ActionParser {

    private static final String FALLBACK_USER_MESSAGE =
            "I'm having trouble processing your request. Let me connect you with a human agent who can help.";

    private final ObjectMapper objectMapper;

    public ActionParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Parse LLM raw response JSON into a ParsedAction.
     */
    public ParsedAction parse(String llmRawResponse) {
        if (llmRawResponse == null || llmRawResponse.isBlank()) {
            log.warn("Empty LLM response, falling back to request_handover");
            return buildFallback();
        }

        try {
            String cleaned = cleanResponse(llmRawResponse);
            JsonNode root = objectMapper.readTree(cleaned);

            String userMessage = root.has("user_message") ? root.get("user_message").asText("") : "";
            String reasoning = root.has("reasoning") ? root.get("reasoning").asText("") : "";

            List<ToolCall> toolCalls = parseToolCalls(root);

            // Default fallback user_message when both tool_calls and user_message are empty
            if (toolCalls.isEmpty() && userMessage.isBlank()) {
                log.warn("Both tool_calls and user_message empty in LLM response, using fallback message");
                userMessage = "I'm looking into this for you.";
            }

            return ParsedAction.builder()
                    .toolCalls(toolCalls)
                    .userMessage(userMessage)
                    .reasoning(reasoning)
                    .build();

        } catch (Exception e) {
            log.warn("Failed to parse LLM response: {}", e.getMessage());
            return buildFallback();
        }
    }

    /**
     * Parse the {@code tool_calls} array out of the response root, tolerating
     * missing/null fields and individual malformed entries.
     */
    private List<ToolCall> parseToolCalls(JsonNode root) {
        List<ToolCall> result = new ArrayList<>();
        if (!root.has("tool_calls") || !root.get("tool_calls").isArray()) {
            return result;
        }
        for (Iterator<JsonNode> it = root.get("tool_calls").elements(); it.hasNext(); ) {
            JsonNode tc = it.next();
            if (!tc.isObject()) continue;
            String name = tc.has("name") ? tc.get("name").asText("") : "";
            if (name.isBlank()) {
                log.warn("Skipping tool_call with missing/blank name: {}", tc);
                continue;
            }
            Map<String, Object> arguments = Map.of();
            if (tc.has("arguments") && tc.get("arguments").isObject()) {
                arguments = objectMapper.convertValue(
                        tc.get("arguments"),
                        new TypeReference<Map<String, Object>>() {}
                );
            }
            result.add(ToolCall.builder().name(name).arguments(arguments).build());
        }
        return result;
    }

    /**
     * Fallback returned on any parse failure: a single {@code request_handover}
     * tool call with a customer-facing apology.
     */
    private ParsedAction buildFallback() {
        ToolCall handover = ToolCall.builder()
                .name("request_handover")
                .arguments(Map.of("escalation_reason", "system_failure"))
                .build();
        return ParsedAction.builder()
                .toolCalls(List.of(handover))
                .userMessage(FALLBACK_USER_MESSAGE)
                .reasoning("LLM response parse failure - fallback to handover")
                .build();
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
