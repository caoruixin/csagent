package com.gumtree.csagent.service.tools;

import com.gumtree.csagent.model.BotSession;

import java.util.Map;

/**
 * Contract for all tool implementations.
 * Each tool is a Spring @Component discovered via List<Tool> injection.
 */
public interface Tool {

    /**
     * Unique snake_case tool name matching tool-policy.yaml keys.
     */
    String getName();

    /**
     * Execute the tool with the given session context and parameters.
     */
    ToolResult execute(BotSession session, Map<String, Object> parameters);
}
