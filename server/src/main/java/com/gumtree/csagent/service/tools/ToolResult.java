package com.gumtree.csagent.service.tools;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * DTO returned by every tool execution.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ToolResult {

    private boolean success;
    private Map<String, Object> data;
    private String errorMessage;

    public static ToolResult ok(Map<String, Object> data) {
        return ToolResult.builder().success(true).data(data).build();
    }

    public static ToolResult error(String message) {
        return ToolResult.builder().success(false).errorMessage(message).build();
    }
}
