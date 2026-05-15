package com.gumtree.csagent.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Represents a single OpenAI-style tool call requested by the LLM.
 * Part of the new tool-use response contract: {user_message, reasoning, tool_calls: [{name, arguments}]}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ToolCall {

    private String name;
    private Map<String, Object> arguments;
}
