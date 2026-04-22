package com.gumtree.csagent.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LlmResponse {

    private String content;
    private String finishReason;
    private int promptTokens;
    private int completionTokens;
    private long latencyMs;
}
