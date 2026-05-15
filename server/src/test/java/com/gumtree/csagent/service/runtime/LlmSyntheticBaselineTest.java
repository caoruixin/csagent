package com.gumtree.csagent.service.runtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gumtree.csagent.model.ChatMessage;
import com.gumtree.csagent.model.LlmRequest;
import com.gumtree.csagent.model.LlmResponse;
import com.gumtree.csagent.service.llm.OpenAiCompatibleLlmClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Sprint 25 — synthetic LLM-only baseline. Measures one LLM round-trip in
 * isolation from tool dispatch, persistence, and context projection.
 *
 * <p>Opt-in: requires {@code RUN_LLM_BASELINE=true} and a live
 * {@code DEEPSEEK_API_KEY}. Skipped under the normal {@code mvn test} run so
 * CI does not spend on outbound LLM calls. Reproducer:
 *
 * <pre>
 *   RUN_LLM_BASELINE=true mvn -pl server -Dtest=LlmSyntheticBaselineTest test
 * </pre>
 *
 * <p>What this measures: the HTTP round-trip + LLM compute for a single
 * {@code chat} call against the production provider lineup (DeepSeek primary
 * by default; overridable via env). What it does NOT measure: context
 * projection cost, {@code LlmCallLogger} write, tool dispatch, retry loop,
 * fallback engagement. The point is to subtract everything except the LLM
 * call so the per-LLM-call latency observed in {@code llm_call_log} can be
 * compared against a measurement that contains only what the LLM and the
 * network are actually doing.
 */
class LlmSyntheticBaselineTest {

    private static final String FIXED_USER_MESSAGE =
            "Respond with the JSON object {\"ok\": true} and nothing else.";
    private static final String FIXED_SYSTEM_PROMPT =
            "You are a measurement endpoint. Reply only with the requested JSON.";

    @Test
    @EnabledIfEnvironmentVariable(named = "RUN_LLM_BASELINE", matches = "true")
    void measureLlmRoundTripBaseline() {
        String apiKey = required("DEEPSEEK_API_KEY");
        String baseUrl = envOr("DEEPSEEK_BASE_URL", "https://api.deepseek.com/v1");
        String model = envOr("DEEPSEEK_MODEL", "deepseek-v4-flash");
        int n = parseIntEnv("LLM_BASELINE_N", 30);
        int warmup = parseIntEnv("LLM_BASELINE_WARMUP", 2);

        OpenAiCompatibleLlmClient client = new OpenAiCompatibleLlmClient(
                "deepseek", apiKey, baseUrl, model, new ObjectMapper());

        LlmRequest request = LlmRequest.builder()
                .systemPrompt(FIXED_SYSTEM_PROMPT)
                .messages(List.of(
                        ChatMessage.builder().role("user").content(FIXED_USER_MESSAGE).build()))
                .temperature(0.3)
                .maxTokens(1024)
                .responseFormat("json_object")
                .build();

        for (int i = 0; i < warmup; i++) {
            try {
                client.chat(request);
            } catch (Exception ignored) {
                // warm-up failures don't count; the measured loop reports them
            }
        }

        List<Long> samples = new ArrayList<>(n);
        int failures = 0;
        for (int i = 0; i < n; i++) {
            long start = System.currentTimeMillis();
            try {
                LlmResponse response = client.chat(request);
                long elapsed = System.currentTimeMillis() - start;
                samples.add(elapsed);
                System.out.printf("[SYNTH] sample=%02d/%02d elapsed_ms=%d client_reported_ms=%d "
                                + "prompt_tokens=%d completion_tokens=%d finish=%s%n",
                        i + 1, n, elapsed, response.getLatencyMs(),
                        response.getPromptTokens(), response.getCompletionTokens(),
                        response.getFinishReason());
            } catch (Exception e) {
                long elapsed = System.currentTimeMillis() - start;
                failures++;
                System.out.printf("[SYNTH] sample=%02d/%02d FAILED elapsed_ms=%d cause=%s: %s%n",
                        i + 1, n, elapsed, e.getClass().getSimpleName(), e.getMessage());
            }
        }

        System.out.printf("[SYNTH-BASELINE-CONFIG] model=%s base_url=%s n_target=%d warmup=%d%n",
                model, baseUrl, n, warmup);
        System.out.printf("[SYNTH-BASELINE-RESULT] n_success=%d n_failure=%d%n",
                samples.size(), failures);
        if (!samples.isEmpty()) {
            Collections.sort(samples);
            long min = samples.get(0);
            long max = samples.get(samples.size() - 1);
            long p50 = percentile(samples, 0.50);
            long p95 = percentile(samples, 0.95);
            long p99 = percentile(samples, 0.99);
            double mean = samples.stream().mapToLong(Long::longValue).average().orElse(0);
            System.out.printf("[SYNTH-BASELINE-RESULT] min_ms=%d p50_ms=%d p95_ms=%d p99_ms=%d "
                            + "max_ms=%d mean_ms=%.1f%n",
                    min, p50, p95, p99, max, mean);
        }

        assertTrue(samples.size() >= 1,
                "Synthetic baseline produced zero successful samples; check creds / network.");
    }

    private static long percentile(List<Long> sortedSamples, double q) {
        if (sortedSamples.isEmpty()) return -1;
        int idx = (int) Math.ceil(q * sortedSamples.size()) - 1;
        idx = Math.max(0, Math.min(idx, sortedSamples.size() - 1));
        return sortedSamples.get(idx);
    }

    private static String required(String key) {
        String v = System.getenv(key);
        if (v == null || v.isBlank()) {
            throw new IllegalStateException(
                    "Env var " + key + " is required for LlmSyntheticBaselineTest.");
        }
        return v;
    }

    private static String envOr(String key, String dflt) {
        String v = System.getenv(key);
        return (v == null || v.isBlank()) ? dflt : v;
    }

    private static int parseIntEnv(String key, int dflt) {
        String v = System.getenv(key);
        if (v == null || v.isBlank()) return dflt;
        try {
            return Integer.parseInt(v);
        } catch (NumberFormatException e) {
            return dflt;
        }
    }
}
