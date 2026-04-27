package com.gumtree.csagent.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "llm")
public class LlmProperties {

    private KimiProperties kimi = new KimiProperties();
    private DashScopeProperties dashscope = new DashScopeProperties();

    @Data
    public static class KimiProperties {
        private String apiKey;
        private String baseUrl = "https://api.moonshot.ai/v1";
        private String model = "kimi-k2.6";
    }

    @Data
    public static class DashScopeProperties {
        private String apiKey;
        private String baseUrl = "https://dashscope.aliyuncs.com/compatible-mode/v1";
        private String chatModel = "qwen-plus";
        private String embeddingModel = "text-embedding-v3";
        private int embeddingDimension = 768;
    }
}
