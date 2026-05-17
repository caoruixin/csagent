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
 * Wires the chat-completion LLM bean graph.
 *
 * <p>DeepSeek is the primary chat-completion provider; Kimi is the fallback.
 * Moonshot "thinking" mode for each provider is controlled solely by
 * {@code llm.*.thinking-enabled} / {@code *_THINKING_ENABLED} (see {@link LlmProperties}) —
 * not by model-name heuristics in the client.
 *
 * <p>Per phase3 §3.8.5 / §3.9.1, fallback engages only on transient primary
 * failures (5xx / 429 / network errors); non-transient errors (auth, 4xx,
 * parse) propagate.
 */
@Slf4j
@Configuration
public class LlmClientConfig {

    /**
     * Kimi — FALLBACK chat-completion provider (model and thinking from {@code llm.kimi} / env).
     */
    @Bean(name = "kimiLlmClient")
    public OpenAiCompatibleLlmClient kimiLlmClient(LlmProperties props, ObjectMapper objectMapper) {
        LlmProperties.KimiProperties k = props.getKimi();
        return new OpenAiCompatibleLlmClient("kimi", k.getApiKey(), k.getBaseUrl(),
                k.getModel(), k.isThinkingEnabled(), objectMapper);
    }

    /**
     * DeepSeek — PRIMARY chat-completion provider (model and thinking from {@code llm.deepseek} / env).
     */
    @Bean(name = "deepseekLlmClient")
    public OpenAiCompatibleLlmClient deepseekLlmClient(LlmProperties props, ObjectMapper objectMapper) {
        LlmProperties.DeepSeekProperties d = props.getDeepseek();
        return new OpenAiCompatibleLlmClient("deepseek", d.getApiKey(), d.getBaseUrl(),
                d.getModel(), d.isThinkingEnabled(), objectMapper);
    }

    /**
     * The single {@link LlmClient} bean injected everywhere downstream
     * ({@code LlmInvocationService}, etc.) is the fallback wrapper.
     *
     * <p>Sprint §C0: runs {@link LlmConfigValidator#validateOrThrow} at bean
     * creation so an invalid endpoint / credential pair fails the startup
     * with a clear diagnostic instead of silently surfacing as a 401 inside
     * the bot loop and getting swallowed by {@code SAFE_ESCALATION_RESPONSE}.
     * Secrets are never logged; the lineup describe string only reports key
     * presence ({@code present|placeholder|blank}).
     */
    @Bean
    @Primary
    public LlmClient llmClient(OpenAiCompatibleLlmClient kimiLlmClient,
                               OpenAiCompatibleLlmClient deepseekLlmClient,
                               LlmProperties props) {
        LlmConfigValidator.validateOrThrow(props);
        log.info("LLM provider lineup: {}", LlmConfigValidator.describe(props));
        // Sprint 8.1 follow-up #2: deepseek is primary, kimi is fallback.
        return new FallbackLlmClient(deepseekLlmClient, kimiLlmClient, "deepseek", "kimi");
    }
}
