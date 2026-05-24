package com.gumtree.csagent.service.runtime;

import com.gumtree.csagent.config.LlmProperties;
import com.gumtree.csagent.model.ChatMessage;
import com.gumtree.csagent.model.LlmRequest;
import com.gumtree.csagent.model.LlmResponse;
import com.gumtree.csagent.service.llm.LlmClient;
import com.gumtree.csagent.service.llm.LlmDeadlineExceededException;
import com.gumtree.csagent.service.llm.LlmUnavailableException;
import com.gumtree.csagent.service.observability.LlmCallLogger;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Wraps LlmClient with context-aware request building, system prompt loading,
 * and graceful error handling (returns safe escalation response on LLM failure).
 */
@Slf4j
@Service
public class LlmInvocationService {

    private static final String SYSTEM_PROMPT_PATH = "prompts/system_prompt.txt";
    private static final String ROUTING_PROMPT_PATH = "prompts/routing_prompt.txt";

    /**
     * Sprint 8.1 §M2 — synthetic safe-escalation marker visible in
     * {@link LlmResponse#getFinishReason()}. The K0 missing-UC fallback
     * gate detects this so that a SAFE_ESCALATION_RESPONSE is never
     * counted as real LLM reasoning evidence (see
     * {@code ControlKernel.agentRunResultHasEvidence}).
     */
    public static final String SYNTHETIC_SAFE_ESCALATION_FINISH_REASON = "error_fallback";

    private static final LlmResponse SAFE_ESCALATION_RESPONSE = LlmResponse.builder()
            .content("{\"user_message\":\"I apologize, but I'm experiencing a technical issue. " +
                     "Let me connect you with a human agent who can assist you.\"," +
                     "\"reasoning\":\"LLM invocation failure\"," +
                     "\"tool_calls\":[{\"name\":\"request_handover\"," +
                     "\"arguments\":{\"escalation_reason\":\"system_failure\"}}]}")
            .finishReason(SYNTHETIC_SAFE_ESCALATION_FINISH_REASON)
            .latencyMs(0)
            .build();

    private final LlmClient llmClient;
    private final LlmCallLogger llmCallLogger;
    private final String modelName;

    private String systemPromptTemplate;
    private String routingPromptTemplate;

    public LlmInvocationService(LlmClient llmClient, LlmCallLogger llmCallLogger,
                                 LlmProperties llmProperties) {
        this.llmClient = llmClient;
        this.llmCallLogger = llmCallLogger;
        // Sprint 8.1 follow-up #2 (2026-05-06): primary is now deepseek (see
        // LlmClientConfig). Telemetry tag for the modelName surface points at
        // the primary's model so trace metadata reflects the model that ran
        // most often. Fallback model leaks into per-call client logs already.
        this.modelName = llmProperties.getDeepseek().getModel();
    }

    @PostConstruct
    public void init() {
        systemPromptTemplate = loadPromptTemplate(SYSTEM_PROMPT_PATH);
        routingPromptTemplate = loadPromptTemplate(ROUTING_PROMPT_PATH);
        log.info("LLM prompts loaded: system_prompt={}chars, routing_prompt={}chars",
                systemPromptTemplate.length(), routingPromptTemplate.length());
    }

    /**
     * Sprint 51 / M5 S2 — the active model name used for {@code chat} calls.
     * Stamped on per-step {@link com.gumtree.csagent.model.LlmCallRecord} so
     * the admin trace can show which model produced each invocation. The
     * value is the primary provider's configured model (see constructor).
     */
    public String getModelName() {
        return modelName;
    }

    /**
     * Invoke the LLM for a standard chat turn.
     *
     * <p>Sprint §C1: the underlying {@code OpenAiCompatibleLlmClient} retries
     * once on transient failure and {@code FallbackLlmClient} engages a
     * different provider on transient failure. If both fail, this method
     * surfaces a {@link #SAFE_ESCALATION_RESPONSE} so the bot loop terminates
     * gracefully rather than tearing down the session. The failure is logged
     * with provider / class / message for traceability — no secrets.
     *
     * @param projectedContext the JSON context string built by ContextProjectionBuilder
     * @param userMessage      the current user message
     * @param sessionId        session identifier for call logging
     * @param turnIndex        turn index for call logging
     * @return LlmResponse, or a safe escalation response on failure
     */
    public LlmResponse invokeChat(String projectedContext, String userMessage,
                                   String sessionId, int turnIndex) {
        long start = System.currentTimeMillis();
        String requestSummary = truncate(userMessage, 200);
        try {
            String systemPrompt = systemPromptTemplate + "\n\nCurrent context:\n" + projectedContext;

            LlmRequest request = LlmRequest.builder()
                    .systemPrompt(systemPrompt)
                    .messages(List.of(
                            ChatMessage.builder().role("user").content(userMessage).build()
                    ))
                    .temperature(0.3)
                    .maxTokens(1024)
                    .responseFormat("json_object")
                    .build();

            LlmResponse response = llmClient.chat(request);
            int elapsed = (int) (System.currentTimeMillis() - start);
            log.info("LLM [chat] response: latency={}ms, tokens={}/{}",
                    response.getLatencyMs(), response.getPromptTokens(), response.getCompletionTokens());

            llmCallLogger.log(sessionId, turnIndex, "chat", modelName,
                    response.getPromptTokens(), response.getCompletionTokens(),
                    elapsed, requestSummary, truncate(response.getContent(), 500));

            return response;

        } catch (LlmDeadlineExceededException e) {
            // Sprint 8.1 §M2: deadline exhaustion MUST surface honestly.
            // Previously this was caught and converted to a synthetic
            // SAFE_ESCALATION_RESPONSE, which let K0 stamp a fake UC on
            // no-real-LLM-work timeout paths. Now we propagate so
            // AgentRunLoop returns TerminalOutcome.DEADLINE_EXCEEDED.
            int elapsed = (int) (System.currentTimeMillis() - start);
            log.warn("LLM [chat:deadline-exceeded] session={} turn={} elapsed_ms={} — propagating, "
                            + "no synthetic safe-escalation",
                    sessionId, turnIndex, elapsed);
            llmCallLogger.logFailure(sessionId, turnIndex, "chat", modelName,
                    elapsed, requestSummary, "[llm_deadline_exceeded] " + e.getMessage());
            throw e;
        } catch (Exception e) {
            int elapsed = (int) (System.currentTimeMillis() - start);
            // Sprint §C1: classify the failure tag for traceability so eval
            // post-hoc analysis can distinguish auth misconfig from transport
            // flakes without parsing free-form messages. The tag never
            // includes secrets.
            String failureTag = classifyFailure(e);
            log.error("LLM [chat:exception] session={} turn={} failure_tag={} cause={}: {}",
                    sessionId, turnIndex, failureTag, e.getClass().getSimpleName(), e.getMessage(), e);
            llmCallLogger.logFailure(sessionId, turnIndex, "chat", modelName,
                    elapsed, requestSummary, "[" + failureTag + "] " + e.getMessage());
            // Sprint 8.1 §M2: infrastructure-class failures (transport / 5xx /
            // 429 / 401 / 403) are propagated as LlmUnavailableException so
            // AgentRunLoop returns TerminalOutcome.LLM_UNAVAILABLE rather
            // than synthesising a safe escalation that the K0 gate would
            // mistake for real LLM evidence. Parse / processing errors keep
            // returning the SAFE_ESCALATION_RESPONSE — those are real LLM
            // calls that returned a malformed payload, not infra outages —
            // and the synthetic marker (finish_reason=error_fallback) is
            // detected by the K0 evidence gate.
            if (isInfraFailure(failureTag)) {
                throw new LlmUnavailableException(
                        "LLM provider chain failed (failure_class=" + failureTag + ")",
                        failureTag, e);
            }
            return SAFE_ESCALATION_RESPONSE;
        }
    }

    /**
     * Sprint 8.1 §M2 — classification of failure tags that indicate an
     * infrastructure-class failure (worth surfacing as
     * {@link LlmUnavailableException}). Parse / unknown errors return
     * {@code false} so the legacy synthetic-safe-escalation path is kept
     * for malformed-but-real LLM responses.
     */
    static boolean isInfraFailure(String failureTag) {
        if (failureTag == null) return false;
        return failureTag.equals("llm_timeout")
                || failureTag.equals("llm_connect_failed")
                || failureTag.equals("llm_transport_error")
                || failureTag.equals("llm_rate_limited")
                || failureTag.equals("llm_server_error")
                || failureTag.equals("llm_auth_error")
                || failureTag.startsWith("llm_http_error_");
    }

    /**
     * Sprint §C1 / Sprint 8.1 §M2: classify an exception thrown out of the
     * underlying LLM stack into a small set of trace-friendly tags. Keeps
     * trace metadata stable across providers (Kimi vs DeepSeek wrap
     * exceptions slightly differently). M2 broadens the message-based
     * fallbacks so generic {@code RuntimeException}s carrying transport
     * shape ({@code "connection refused"}, {@code "connection reset"},
     * {@code "i/o error"}) classify as infra failures rather than the
     * catch-all {@code llm_unknown_error}.
     */
    static String classifyFailure(Throwable t) {
        Throwable cur = t;
        for (int depth = 0; cur != null && depth < 8; depth++, cur = cur.getCause()) {
            if (cur instanceof org.springframework.web.client.HttpStatusCodeException hse) {
                int code = hse.getStatusCode().value();
                if (code == 401 || code == 403) return "llm_auth_error";
                if (code == 429) return "llm_rate_limited";
                if (code >= 500 && code <= 599) return "llm_server_error";
                return "llm_http_error_" + code;
            }
            if (cur instanceof java.net.SocketTimeoutException) return "llm_timeout";
            if (cur instanceof java.net.ConnectException) return "llm_connect_failed";
            if (cur instanceof org.springframework.web.client.ResourceAccessException) return "llm_transport_error";
            String msg = cur.getMessage() == null ? "" : cur.getMessage().toLowerCase();
            if (msg.contains("timeout") || msg.contains("timed out")) return "llm_timeout";
            if (msg.contains("rate limit") || msg.contains("429")) return "llm_rate_limited";
            if (msg.contains("connection refused") || msg.contains("connection reset")
                    || msg.contains("connect failed")) return "llm_connect_failed";
            if (msg.contains("i/o error") || msg.contains("ioexception")
                    || msg.contains("broken pipe")) return "llm_transport_error";
        }
        return "llm_unknown_error";
    }

    /**
     * Invoke the LLM for use case routing/classification.
     *
     * @param ucCandidates  formatted string describing candidate use cases
     * @param topicSubject  the customer's form topic
     * @param description   the customer's description
     * @param sessionId     session identifier for call logging
     * @return LlmResponse with classification result
     */
    public LlmResponse invokeRouting(String ucCandidates, String topicSubject,
                                      String description, String sessionId) {
        return invokeRouting(ucCandidates, topicSubject, description, null, sessionId);
    }

    /**
     * Sprint 7 §I1 — overload that carries a routing-context cue (e.g.
     * `customer_context.moderation_status`) so the UC-FP vs UC-A tiebreaker
     * for short ad-rejection forms can fire deterministically. The cue is
     * substituted into the {@code {routing_context}} placeholder in
     * {@code routing_prompt.txt}; when {@code routingContext} is null/blank
     * the placeholder is replaced with a stable "unknown" string so the
     * routing prompt remains well-formed.
     */
    public LlmResponse invokeRouting(String ucCandidates, String topicSubject,
                                      String description, String routingContext,
                                      String sessionId) {
        long start = System.currentTimeMillis();
        String requestSummary = truncate("topic=" + topicSubject + " desc=" + description, 200);
        try {
            String routingContextValue = (routingContext == null || routingContext.isBlank())
                    ? "moderation_status: unknown (no per-account moderation signal available at routing time)"
                    : routingContext;
            String prompt = routingPromptTemplate
                    .replace("{uc_candidates}", ucCandidates)
                    .replace("{topic_subject}", topicSubject != null ? topicSubject : "")
                    .replace("{description}", description != null ? description : "")
                    .replace("{routing_context}", routingContextValue);

            LlmRequest request = LlmRequest.builder()
                    .systemPrompt("You are a customer inquiry classifier. Respond only in JSON format.")
                    .messages(List.of(
                            ChatMessage.builder().role("user").content(prompt).build()
                    ))
                    .temperature(0.1)
                    .maxTokens(256)
                    .responseFormat("json_object")
                    .build();

            LlmResponse response = llmClient.chat(request);
            int elapsed = (int) (System.currentTimeMillis() - start);
            log.info("LLM [routing] response: latency={}ms", response.getLatencyMs());

            llmCallLogger.log(sessionId, null, "routing", modelName,
                    response.getPromptTokens(), response.getCompletionTokens(),
                    elapsed, requestSummary, truncate(response.getContent(), 500));

            return response;

        } catch (Exception e) {
            int elapsed = (int) (System.currentTimeMillis() - start);
            log.error("LLM routing invocation failed: {}", e.getMessage(), e);
            llmCallLogger.logFailure(sessionId, null, "routing", modelName,
                    elapsed, requestSummary, e.getMessage());
            return SAFE_ESCALATION_RESPONSE;
        }
    }

    public String getSystemPromptTemplate() {
        return systemPromptTemplate;
    }

    private String truncate(String text, int maxLen) {
        if (text == null) return null;
        return text.length() <= maxLen ? text : text.substring(0, maxLen);
    }

    private String loadPromptTemplate(String path) {
        try {
            ClassPathResource resource = new ClassPathResource(path);
            return new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.error("Failed to load prompt template from {}: {}", path, e.getMessage());
            throw new RuntimeException("Failed to load prompt template: " + path, e);
        }
    }
}
