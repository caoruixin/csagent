package com.gumtree.csagent.service.llm;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gumtree.csagent.model.ChatMessage;
import com.gumtree.csagent.model.LlmRequest;
import com.gumtree.csagent.model.LlmResponse;
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
 * Sprint 8.1 §M1 follow-up (2026-05-06) — production fallback-chain attempt cap.
 *
 * <p>The previous focused suite exercised the inner
 * {@link OpenAiCompatibleLlmClient} retry loop in isolation. Codex review
 * surfaced that the production chain
 * ({@link com.gumtree.csagent.config.LlmClientConfig#llmClient}) wraps the
 * inner client in {@link FallbackLlmClient}, and BOTH layers had their own
 * 2-attempt budget. With a 12 s read timeout and a 30 s wall-clock budget,
 * two primary timeouts plus a fallback timeout could blow past the
 * user-facing deadline (the trace for session
 * {@code f1555995-ff1...} turn 3 measured ~41 s). This test pins the new
 * shared HTTP-attempt budget on {@link LlmCallContext}: with a deadline in
 * effect, total HTTP calls across primary + fallback are hard-capped at
 * {@link LlmCallContext#DEFAULT_GLOBAL_ATTEMPT_BUDGET} (= 2).
 *
 * <p>Without a deadline (legacy / batch / unit-test paths) the budget is
 * unset and per-client retry semantics stand — covered by
 * {@link OpenAiCompatibleLlmClientRetryTest} and {@link Sprint81BudgetedRetryTest}.
 */
class Sprint81GlobalAttemptBudgetTest {

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
                "deepseek-v4-flash", objectMapper);
        OpenAiCompatibleLlmClient fallback = new OpenAiCompatibleLlmClient(
                "kimi", "sk-test", "http://127.0.0.1:" + fallbackPort,
                "kimi-k2.6", objectMapper);
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
    void productionChain_primary503AndFallback503_capsAtTwoTotalAttempts() {
        // Production scenario: deadline set (user-facing 30 s). Both
        // primary and fallback return 503 on every call. Without the
        // global cap, the chain would burn FOUR HTTP calls (primary x2 +
        // fallback x2). With the §M1 follow-up shared budget, total HTTP
        // calls across both providers must be ≤ 2.
        LlmCallContext.setDeadline(System.currentTimeMillis() + 30_000L);
        AtomicInteger primaryCalls = new AtomicInteger();
        AtomicInteger fallbackCalls = new AtomicInteger();
        primaryServer.createContext("/chat/completions", exchange -> {
            primaryCalls.incrementAndGet();
            respond(exchange, 503, "{\"error\":\"upstream\"}");
        });
        fallbackServer.createContext("/chat/completions", exchange -> {
            fallbackCalls.incrementAndGet();
            respond(exchange, 503, "{\"error\":\"upstream\"}");
        });

        FallbackLlmClient chain = newChain();
        assertThrows(Exception.class, () -> chain.chat(sampleRequest()));

        int total = primaryCalls.get() + fallbackCalls.get();
        assertTrue(total <= 2,
                "Global HTTP-attempt budget must cap total provider-chain "
                        + "calls at 2. Got primary=" + primaryCalls.get()
                        + ", fallback=" + fallbackCalls.get() + ", total=" + total);
        assertTrue(primaryCalls.get() >= 1,
                "Primary should have been called at least once; got "
                        + primaryCalls.get());
    }

    @Test
    void productionChain_primary503ThenFallbackSuccess_consumesExactlyTwoAttempts() {
        // §M1 follow-up: with the budget set, a single primary transient
        // failure followed by a fallback success spends exactly the
        // 2-attempt budget — and returns the fallback's response. This
        // pins that the cap does NOT prevent legitimate fallback recovery.
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

        LlmResponse response = newChain().chat(sampleRequest());

        assertEquals("ok", response.getContent());
        // Primary may consume 1 attempt before fallback engages. Fallback
        // consumes the 2nd. Inner primary retry must NOT also fire (that
        // would exceed the global budget before fallback gets its turn).
        assertEquals(1, primaryCalls.get(),
                "Primary must use exactly one HTTP call before yielding to "
                        + "fallback under the global budget; got "
                        + primaryCalls.get());
        assertEquals(1, fallbackCalls.get(),
                "Fallback must succeed on its first attempt; got "
                        + fallbackCalls.get());
    }

    @Test
    void productionChain_primary401_neverEngagesFallback() {
        // Auth errors are non-transient: FallbackLlmClient must NOT engage
        // the fallback (and the global budget should not be a factor — the
        // failure mode is independent of attempt count).
        LlmCallContext.setDeadline(System.currentTimeMillis() + 30_000L);
        AtomicInteger primaryCalls = new AtomicInteger();
        AtomicInteger fallbackCalls = new AtomicInteger();
        primaryServer.createContext("/chat/completions", exchange -> {
            primaryCalls.incrementAndGet();
            respond(exchange, 401, "{\"error\":\"invalid_api_key\"}");
        });
        fallbackServer.createContext("/chat/completions", exchange -> {
            fallbackCalls.incrementAndGet();
            respond(exchange, 200, successBody());
        });

        FallbackLlmClient chain = newChain();
        assertThrows(Exception.class, () -> chain.chat(sampleRequest()));

        assertEquals(1, primaryCalls.get(), "401 must surface after exactly 1 primary call");
        assertEquals(0, fallbackCalls.get(),
                "Fallback must NOT engage on a non-transient 401 from the primary");
    }

    @Test
    void noDeadline_primaryAndFallbackEachKeepTheirOwnRetryBudget() {
        // Legacy / batch / eval path: no deadline set →
        // {@link LlmCallContext#remainingAttempts()} returns
        // {@link Integer#MAX_VALUE} and the per-client 2-attempt loop
        // retains its existing behaviour. Pins backward compatibility for
        // non-user-facing callers.
        AtomicInteger primaryCalls = new AtomicInteger();
        AtomicInteger fallbackCalls = new AtomicInteger();
        primaryServer.createContext("/chat/completions", exchange -> {
            primaryCalls.incrementAndGet();
            respond(exchange, 503, "{\"error\":\"upstream\"}");
        });
        fallbackServer.createContext("/chat/completions", exchange -> {
            int n = fallbackCalls.incrementAndGet();
            if (n == 1) respond(exchange, 503, "{\"error\":\"upstream\"}");
            else respond(exchange, 200, successBody());
        });

        LlmResponse response = newChain().chat(sampleRequest());

        assertEquals("ok", response.getContent());
        assertEquals(2, primaryCalls.get(),
                "Without a deadline, primary keeps its 2-attempt retry budget.");
        assertEquals(2, fallbackCalls.get(),
                "Without a deadline, fallback also keeps its 2-attempt retry budget.");
    }
}
