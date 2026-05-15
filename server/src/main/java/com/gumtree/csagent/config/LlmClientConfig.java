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
 * <p>Sprint 8.1 follow-up #2 (2026-05-06): primary / fallback ORDER SWAPPED.
 * DeepSeek v4 flash is now the primary chat-completion provider (per user
 * direction: kimi-k2.6 hits intermittent {@code engine_overloaded_error} and
 * downgrading to a smaller moonshot-v1-* tier surfaced new instruction-
 * following gaps). Kimi (kimi-k2.6) stays as the fallback so we still have a
 * "smarter" backup on rare primary 5xx / 429 / network blips.
 *
 * <p>Per phase3 §3.8.5 / §3.9.1, fallback engages only on transient primary
 * failures (5xx / 429 / network errors); non-transient errors (auth, 4xx,
 * parse) propagate.
 */
@Slf4j
@Configuration
public class LlmClientConfig {

    /**
     * Kimi (model from {@code KIMI_MODEL}, default kimi-k2.6) — FALLBACK
     * chat-completion provider after Sprint 8.1 follow-up #2.
     */
    @Bean(name = "kimiLlmClient")
    public OpenAiCompatibleLlmClient kimiLlmClient(LlmProperties props, ObjectMapper objectMapper) {
        LlmProperties.KimiProperties k = props.getKimi();
        return new OpenAiCompatibleLlmClient("kimi", k.getApiKey(), k.getBaseUrl(), k.getModel(), objectMapper);
    }

    /**
     * DeepSeek (model from {@code DEEPSEEK_MODEL}, default deepseek-v4-flash) —
     * PRIMARY chat-completion provider after Sprint 8.1 follow-up #2.
     */
    @Bean(name = "deepseekLlmClient")
    public OpenAiCompatibleLlmClient deepseekLlmClient(LlmProperties props, ObjectMapper objectMapper) {
        LlmProperties.DeepSeekProperties d = props.getDeepseek();
        return new OpenAiCompatibleLlmClient("deepseek", d.getApiKey(), d.getBaseUrl(), d.getModel(), objectMapper);
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
