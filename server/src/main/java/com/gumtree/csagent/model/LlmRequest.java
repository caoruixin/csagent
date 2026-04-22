package com.gumtree.csagent.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LlmRequest {

    private String systemPrompt;
    private List<ChatMessage> messages;
    @Builder.Default
    private double temperature = 0.3;
    @Builder.Default
    private int maxTokens = 2048;
    private String responseFormat;
}
