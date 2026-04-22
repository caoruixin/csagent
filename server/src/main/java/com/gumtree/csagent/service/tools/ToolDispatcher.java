package com.gumtree.csagent.service.tools;

import com.gumtree.csagent.model.BotSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Central dispatcher that routes tool calls through policy enforcement
 * before delegating to the appropriate Tool implementation.
 */
@Slf4j
@Component
public class ToolDispatcher {

    private final ToolPolicyEnforcer policyEnforcer;
    private final Map<String, Tool> toolRegistry = new LinkedHashMap<>();

    public ToolDispatcher(ToolPolicyEnforcer policyEnforcer, List<Tool> tools) {
        this.policyEnforcer = policyEnforcer;
        for (Tool tool : tools) {
            toolRegistry.put(tool.getName(), tool);
            log.debug("Registered tool: {}", tool.getName());
        }
        log.info("ToolDispatcher initialized with {} tools: {}", toolRegistry.size(), toolRegistry.keySet());
    }

    /**
     * Dispatch a tool call: check policy, then execute.
     *
     * @param toolName   snake_case tool name
     * @param session    current bot session
     * @param parameters tool-specific parameters
     * @return tool result (success or error)
     */
    public ToolResult dispatch(String toolName, BotSession session, Map<String, Object> parameters) {
        log.info("Dispatching tool '{}' for session '{}'", toolName, session.getSessionId());

        // 1. Lookup tool
        Tool tool = toolRegistry.get(toolName);
        if (tool == null) {
            log.error("Unknown tool '{}' requested for session '{}'", toolName, session.getSessionId());
            return ToolResult.error("Unknown tool: " + toolName);
        }

        // 2. Check policy
        String activeUseCase = session.getActiveUseCase();
        if (!policyEnforcer.isToolAllowed(toolName, activeUseCase)) {
            policyEnforcer.emitBlockedEvent(session.getSessionId(), toolName, activeUseCase);
            return ToolResult.error(String.format(
                    "Tool '%s' is not allowed for use case '%s'", toolName,
                    activeUseCase != null ? activeUseCase : "none"));
        }

        // 3. Execute
        try {
            ToolResult result = tool.execute(session, parameters);
            log.info("Tool '{}' completed: success={}", toolName, result.isSuccess());
            return result;
        } catch (Exception e) {
            log.error("Tool '{}' failed with exception: {}", toolName, e.getMessage(), e);
            return ToolResult.error("Tool execution failed: " + e.getMessage());
        }
    }

    /**
     * Get the list of AGENT_VISIBLE tools allowed for a use case.
     */
    public List<String> getVisibleToolsForUc(String activeUseCase) {
        return policyEnforcer.getVisibleToolsForUc(activeUseCase);
    }

    /**
     * Check if a tool exists in the registry.
     */
    public boolean hasToolNamed(String toolName) {
        return toolRegistry.containsKey(toolName);
    }
}
