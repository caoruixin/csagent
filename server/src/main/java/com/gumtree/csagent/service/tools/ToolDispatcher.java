package com.gumtree.csagent.service.tools;

import com.gumtree.csagent.config.MockProperties;
import com.gumtree.csagent.model.BotSession;
import com.gumtree.csagent.service.guardrails.ProgressPlaceholderService;
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
    private final ProgressPlaceholderService placeholderService;
    private final MockProperties mockProperties;
    private final Map<String, Tool> toolRegistry = new LinkedHashMap<>();

    public ToolDispatcher(ToolPolicyEnforcer policyEnforcer, List<Tool> tools,
                          ProgressPlaceholderService placeholderService,
                          MockProperties mockProperties) {
        this.policyEnforcer = policyEnforcer;
        this.placeholderService = placeholderService;
        this.mockProperties = mockProperties;
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

        // 3. Execute with latency tracking and optional artificial delay
        try {
            long toolStart = System.currentTimeMillis();

            // Optional artificial delay for demo/testing placeholder behavior
            int artificialDelay = mockProperties.getToolLatencyMs();
            if (artificialDelay > 0) {
                try {
                    Thread.sleep(artificialDelay);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }

            ToolResult result = tool.execute(session, parameters);
            long toolLatency = System.currentTimeMillis() - toolStart;

            // Track latency on result
            result.setLatencyMs(toolLatency);

            // Log if placeholder threshold exceeded
            if (toolLatency > 1500) {
                String placeholder = placeholderService.getPlaceholder();
                log.info("Tool '{}' took {}ms (>1500ms threshold). Placeholder: '{}'",
                        toolName, toolLatency, placeholder);
                result.setPlaceholderSent(true);
                result.setPlaceholderMessage(placeholder);
            }

            log.info("Tool '{}' completed: success={}, latency={}ms", toolName, result.isSuccess(), toolLatency);
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
