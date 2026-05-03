package com.gumtree.csagent.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gumtree.csagent.service.llm.FallbackLlmClient;
import com.gumtree.csagent.service.llm.LlmClient;
import com.gumtree.csagent.service.llm.OpenAiCompatibleLlmClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Wires the chat-completion LLM bean graph: primary Kimi 2.6 + fallback DeepSeek v4 pro.
 * Per phase3 §3.8.5 / §3.9.1, fallback engages only on transient primary failures
 * (5xx / 429 / network errors); non-transient errors (auth, 4xx, parse) propagate.
 */
@Slf4j
@Configuration
public class LlmClientConfig {

    /** Kimi 2.6 — primary chat-completion provider. */
    @Bean(name = "kimiLlmClient")
    public OpenAiCompatibleLlmClient kimiLlmClient(LlmProperties props, ObjectMapper objectMapper) {
        LlmProperties.KimiProperties k = props.getKimi();
        return new OpenAiCompatibleLlmClient("kimi", k.getApiKey(), k.getBaseUrl(), k.getModel(), objectMapper);
    }

    /** DeepSeek v4 pro — fallback chat-completion provider. */
    @Bean(name = "deepseekLlmClient")
    public OpenAiCompatibleLlmClient deepseekLlmClient(LlmProperties props, ObjectMapper objectMapper) {
        LlmProperties.DeepSeekProperties d = props.getDeepseek();
        return new OpenAiCompatibleLlmClient("deepseek", d.getApiKey(), d.getBaseUrl(), d.getModel(), objectMapper);
    }

    /**
     * The single {@link LlmClient} bean injected everywhere downstream
     * ({@code LlmInvocationService}, etc.) is the fallback wrapper.
     */
    @Bean
    @Primary
    public LlmClient llmClient(OpenAiCompatibleLlmClient kimiLlmClient,
                               OpenAiCompatibleLlmClient deepseekLlmClient,
                               LlmProperties props) {
        LlmProperties.KimiProperties k = props.getKimi();
        LlmProperties.DeepSeekProperties d = props.getDeepseek();
        log.info("LLM provider lineup: primary=kimi[model={}, base={}], fallback=deepseek[model={}, base={}]",
                k.getModel(), k.getBaseUrl(), d.getModel(), d.getBaseUrl());
        return new FallbackLlmClient(kimiLlmClient, deepseekLlmClient, "kimi", "deepseek");
    }
}
