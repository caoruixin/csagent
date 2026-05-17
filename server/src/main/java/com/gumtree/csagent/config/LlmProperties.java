package com.gumtree.csagent.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "llm")
public class LlmProperties {

    private KimiProperties kimi = new KimiProperties();
    private DeepSeekProperties deepseek = new DeepSeekProperties();
    private DashScopeProperties dashscope = new DashScopeProperties();

    @Data
    public static class KimiProperties {
        private String apiKey;
        private String baseUrl;
        private String model;
        private boolean thinkingEnabled = false;
    }

    @Data
    public static class DeepSeekProperties {
        private String apiKey;
        private String baseUrl;
        private String model;
        private boolean thinkingEnabled = false;
    }

    @Data
    public static class DashScopeProperties {
        private String apiKey;
        private String baseUrl;
        private String embeddingModel;
        private int embeddingDimension = 768;
    }
}
