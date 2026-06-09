package com.gumtree.csagent.service.llm;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gumtree.csagent.model.ChatMessage;
import com.gumtree.csagent.model.LlmRequest;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Sprint 8.1 closure follow-up (2026-05-07) — P1-1.
 *
 * <p>Pins the wall-clock remaining-budget guard at the fallback boundary in
 * {@link FallbackLlmClient}. Codex review observed that a fast transient
 * primary failure with less than one full attempt's worth of remaining
 * wall-clock could still engage the fallback and block on a 12 s read
 * timeout — pushing the user-facing deadline (30 s) past its budget.
 *
 * <p>Required behaviour: when the primary fails transiently and
 * {@link LlmCallContext#remainingMillis()} is below the full-attempt floor
 * (~3 s connect + 12 s read + buffer), {@link FallbackLlmClient} MUST skip
 * the fallback and surface {@link LlmDeadlineExceededException} with
 * {@code retry_decision=fallback_skipped_insufficient_budget} telemetry.
 */
class Sprint81FallbackBudgetGuardTest {

    private HttpServer primaryServer;
    private HttpServer fallbackServer;
    private int primaryPort;
    private int fallbackPort;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void startServers() throws IOException {
        primaryServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        primaryPort = primaryServer.getAddress().getPort();
        primaryServer.start();
        fallbackServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        fallbackPort = fallbackServer.getAddress().getPort();
        fallbackServer.start();
    }

    @AfterEach
    void stopServers() {
        if (primaryServer != null) primaryServer.stop(0);
        if (fallbackServer != null) fallbackServer.stop(0);
        LlmCallContext.clear();
    }

    private FallbackLlmClient newChain() {
        OpenAiCompatibleLlmClient primary = new OpenAiCompatibleLlmClient(
                "deepseek", "sk-test", "http://127.0.0.1:" + primaryPort,
                "deepseek-v4-flash", false, objectMapper);
        OpenAiCompatibleLlmClient fallback = new OpenAiCompatibleLlmClient(
                "kimi", "sk-test", "http://127.0.0.1:" + fallbackPort,
                "kimi-k2.6", false, objectMapper);
        return new FallbackLlmClient(primary, fallback, "deepseek", "kimi");
    }

    private LlmRequest sampleRequest() {
        return LlmRequest.builder()
                .systemPrompt("test")
                .messages(List.of(ChatMessage.builder().role("user").content("hi").build()))
                .temperature(0.0)
                .maxTokens(64)
                .build();
    }

    private static String successBody() {
        return "{\"choices\":[{\"message\":{\"content\":\"ok\"},\"finish_reason\":\"stop\"}],"
                + "\"usage\":{\"prompt_tokens\":1,\"completion_tokens\":1}}";
    }

    private void respond(HttpExchange exchange, int code, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    @Test
    void primaryFailsFastTransient_remainingBudgetBelowFloor_skipsFallback() {
        // ── Arrange ───────────────────────────────────────────────────
        // Deadline 5 s — well below the fallback's full-attempt floor of
        // ~15.2 s (3 s connect + 12 s read + 200 ms buffer). Primary fails
        // fast with a transient 503; under the previous code the fallback
        // would still engage and could block on its own 12 s read timeout,
        // blowing past the 30 s user-facing deadline. With the §P1-1 guard
        // in place the fallback MUST be skipped.
        LlmCallContext.setDeadline(System.currentTimeMillis() + 5_000L);
        AtomicInteger primaryCalls = new AtomicInteger();
        AtomicInteger fallbackCalls = new AtomicInteger();
        primaryServer.createContext("/chat/completions", exchange -> {
            primaryCalls.incrementAndGet();
            // Fast transient failure: respond immediately with 503 so the
            // remaining wall-clock stays comfortably > 0 but < the floor.
            respond(exchange, 503, "{\"error\":\"upstream\"}");
        });
        fallbackServer.createContext("/chat/completions", exchange -> {
            fallbackCalls.incrementAndGet();
            respond(exchange, 200, successBody());
        });

        FallbackLlmClient chain = newChain();

        // ── Act ───────────────────────────────────────────────────────
        Throwable thrown = assertThrows(Throwable.class, () -> chain.chat(sampleRequest()));

        // ── Assert ────────────────────────────────────────────────────
        // The fallback must not have been engaged — saving us from a 12 s
        // read-timeout wait that the deadline cannot afford.
        assertEquals(0, fallbackCalls.get(),
                "Fallback MUST be skipped when remaining wall-clock < full-attempt floor; "
                        + "got fallbackCalls=" + fallbackCalls.get());
        assertEquals(1, primaryCalls.get(),
                "Primary should have been attempted exactly once before the budget guard fires; "
                        + "got primaryCalls=" + primaryCalls.get());
        // Final classification: deadline_exceeded (LlmDeadlineExceededException
        // or RuntimeException with deadline-exceeded cause). Honest-failure
        // contract from M2: do NOT mask as a synthetic safe escalation.
        Throwable cur = thrown;
        boolean sawDeadlineException = false;
        for (int depth = 0; cur != null && depth < 6; depth++, cur = cur.getCause()) {
            if (cur instanceof LlmDeadlineExceededException) {
                sawDeadlineException = true;
                break;
            }
        }
        assertTrue(sawDeadlineException,
                "Final classification must be LlmDeadlineExceededException (or have one in its "
                        + "cause chain). Got: " + thrown);
    }

    @Test
    void primaryFailsTransient_ampleRemainingBudget_engagesFallback() {
        // Sanity counter-test: with sufficient remaining wall-clock, the
        // fallback DOES engage on a transient primary 503. Pins that the
        // §P1-1 guard does not regress the legitimate fallback path.
        LlmCallContext.setDeadline(System.currentTimeMillis() + 30_000L);
        AtomicInteger primaryCalls = new AtomicInteger();
        AtomicInteger fallbackCalls = new AtomicInteger();
        primaryServer.createContext("/chat/completions", exchange -> {
            primaryCalls.incrementAndGet();
            respond(exchange, 503, "{\"error\":\"upstream\"}");
        });
        fallbackServer.createContext("/chat/completions", exchange -> {
            fallbackCalls.incrementAndGet();
            respond(exchange, 200, successBody());
        });

        FallbackLlmClient chain = newChain();
        com.gumtree.csagent.model.LlmResponse response = chain.chat(sampleRequest());

        assertEquals("ok", response.getContent());
        assertEquals(1, primaryCalls.get(),
                "Primary attempted exactly once before yielding to fallback");
        assertEquals(1, fallbackCalls.get(),
                "Fallback engaged once because remaining wall-clock can fit one full attempt");
    }
}
