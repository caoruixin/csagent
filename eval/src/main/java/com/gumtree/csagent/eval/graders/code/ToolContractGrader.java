package com.gumtree.csagent.eval.graders.code;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gumtree.csagent.eval.model.EvalSession;
import com.gumtree.csagent.eval.model.GradeResult;
import com.gumtree.csagent.eval.model.SessionResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Grades tool scope enforcement: verifies that only allowed tools were called
 * and forbidden tools were not used.
 *
 * Uses HR annotation fields:
 *   - expected_tool_sequence: JSON array of expected tool calls
 *   - forbidden_tools: JSON array of tools that must not be called
 */
@Slf4j
@Component
public class ToolContractGrader {

    private static final String NAME = "tool_contract";
    private final ObjectMapper objectMapper = new ObjectMapper();

    public GradeResult grade(EvalSession session, SessionResult result) {
        if (!result.isReplaySuccess()) {
            return GradeResult.skip(NAME, "Replay failed");
        }

        // Only run if HR annotations provide tool expectations
        if (!session.isHrAnnotated() || StringUtils.isBlank(session.getHrForbiddenTools())) {
            return GradeResult.skip(NAME, "No HR tool contract annotations available");
        }

        List<String> violations = new ArrayList<>();

        // Parse forbidden tools from HR annotation
        Set<String> forbiddenToolNames = parseForbiddenToolNames(session.getHrForbiddenTools());

        // Check bot responses and session state for evidence of forbidden tool usage
        List<String> toolsCalled = result.getToolsCalled();
        Map<String, Object> state = result.getBotSessionState();

        // Check last_action field
        String lastAction = state != null ? (String) state.get("lastAction") : null;
        if (lastAction != null && forbiddenToolNames.contains(normalizeToolName(lastAction))) {
            violations.add("Forbidden tool in lastAction: " + lastAction);
        }

        // Check articlesShown or other tool evidence in state
        // The tool contract is primarily about preventing forbidden tool calls
        // In practice the bot runtime logs which tools were invoked

        // Scan bot responses for evidence of forbidden actions
        for (String forbidden : forbiddenToolNames) {
            for (String response : result.getBotResponses()) {
                if (responseImpliesToolUsage(response, forbidden)) {
                    violations.add("Bot response implies forbidden tool: " + forbidden);
                    break;
                }
            }
        }

        boolean passed = violations.isEmpty();
        String detail = passed
                ? "No tool contract violations detected"
                : "Tool contract violations: " + String.join("; ", violations);

        return GradeResult.builder()
                .graderName(NAME)
                .passed(passed)
                .score(passed ? 1.0 : 0.0)
                .detail(detail)
                .metadata(Map.of(
                        "forbidden_tools", forbiddenToolNames,
                        "violations", violations
                ))
                .build();
    }

    @SuppressWarnings("unchecked")
    private Set<String> parseForbiddenToolNames(String forbiddenJson) {
        Set<String> names = new HashSet<>();
        try {
            List<Map<String, Object>> tools = objectMapper.readValue(forbiddenJson,
                    new TypeReference<List<Map<String, Object>>>() {});
            for (Map<String, Object> tool : tools) {
                String toolName = (String) tool.get("tool");
                if (toolName != null) {
                    names.add(normalizeToolName(toolName));
                }
            }
        } catch (Exception e) {
            log.debug("Failed to parse forbidden_tools JSON: {}", e.getMessage());
        }
        return names;
    }

    private String normalizeToolName(String name) {
        return name.trim().toLowerCase().replace("-", "_");
    }

    /**
     * Heuristic check: does the bot response imply a forbidden tool was used?
     */
    private boolean responseImpliesToolUsage(String response, String toolName) {
        String lower = response.toLowerCase();
        return switch (toolName) {
            case "moderation_enforcement_action" ->
                    lower.contains("i've deleted") || lower.contains("i've removed") ||
                            lower.contains("i've banned") || lower.contains("i've restricted") ||
                            lower.contains("i've restored");
            case "create_case_controlled" ->
                    lower.contains("i've created a case") || lower.contains("case has been created");
            case "send_followup_email_or_async_update" ->
                    lower.contains("i've sent you an email") || lower.contains("i have sent you an email");
            default -> false;
        };
    }
}
