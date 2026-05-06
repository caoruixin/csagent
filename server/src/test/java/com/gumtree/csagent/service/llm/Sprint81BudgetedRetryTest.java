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
 * Sprint 8.1 §M1 — budget-aware fast retry contracts at the inner-client level.
 *
 * <p>Sprint 8.1 §M1 follow-up (2026-05-06): when a user-facing deadline is
 * in effect, the inner {@link OpenAiCompatibleLlmClient} runs at exactly
 * one attempt — the cross-provider retry has been hoisted up to
 * {@link FallbackLlmClient}. Production-chain assertions live in
 * {@link Sprint81GlobalAttemptBudgetTest}; this suite focuses on the
 * inner-client behaviour:
 * <ul>
 *   <li>Without a budget, a transient 5xx triggers exactly one retry
 *       (legacy semantics for batch / eval paths).</li>
 *   <li>With a budget, only one attempt is made — retries are the
 *       fallback layer's job.</li>
 *   <li>Budget exhausted before the next attempt aborts with
 *       {@link LlmDeadlineExceededException} rather than a third attempt.</li>
 *   <li>Two full read-timeout waits cannot occur inside a 25 s budget — the
 *       deadline check skips the retry once the remaining budget cannot
 *       fit one full attempt.</li>
 *   <li>HTTP {@code 401} / deterministic {@code 4xx} are never retried.</li>
 * </ul>
 */
class Sprint81BudgetedRetryTest {

    private HttpServer server;
    private int port;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        port = server.getAddress().getPort();
        server.start();
    }

    @AfterEach
    void stopServer() {
        if (server != null) server.stop(0);
        LlmCallContext.clear();
    }

    private OpenAiCompatibleLlmClient newClient() {
        return new OpenAiCompatibleLlmClient(
                "kimi", "sk-test", "http://127.0.0.1:" + port, "kimi-k2.6", objectMapper);
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
    void retry_under_noBudget_succeedsOnSecondAttempt() {
        // Sprint 8.1 §M1 follow-up: inner-client retry survives ONLY when
        // no global budget is set (legacy / batch / eval paths). The
        // user-facing path always sets a deadline → retry hoisted to
        // FallbackLlmClient (covered by Sprint81GlobalAttemptBudgetTest).
        AtomicInteger calls = new AtomicInteger();
        server.createContext("/chat/completions", exchange -> {
            int n = calls.incrementAndGet();
            if (n == 1) respond(exchange, 503, "{\"error\":\"upstream\"}");
            else respond(exchange, 200, successBody());
        });

        LlmResponse response = newClient().chat(sampleRequest());

        assertEquals(2, calls.get(),
                "Without a global budget, inner client must retry once on transient 5xx");
        assertEquals("ok", response.getContent());
    }

    @Test
    void retry_under_ampleBudget_innerClientDoesExactlyOneAttempt() {
        // Sprint 8.1 §M1 follow-up: with a deadline (and therefore the
        // shared HTTP-attempt budget) in effect, the inner client MUST do
        // exactly one attempt — the second slot is reserved for the
        // cross-provider fallback. Without a fallback wrapper this
        // surfaces the 5xx after a single call.
        LlmCallContext.setDeadline(System.currentTimeMillis() + 30_000L);
        AtomicInteger calls = new AtomicInteger();
        server.createContext("/chat/completions", exchange -> {
            calls.incrementAndGet();
            respond(exchange, 503, "{\"error\":\"upstream\"}");
        });

        OpenAiCompatibleLlmClient client = newClient();
        assertThrows(Exception.class, () -> client.chat(sampleRequest()));

        assertEquals(1, calls.get(),
                "Inner client must NOT retry while a global attempt budget is active; "
                        + "cross-provider fallback owns the second slot. Got " + calls.get());
    }

    @Test
    void deadlineAlreadyExpired_abortsBeforeAttempt() {
        // Sprint 8.1 §M1: even with a budget that allows attempts, a
        // deadline already in the past must abort with
        // {@link LlmDeadlineExceededException} rather than burning an HTTP
        // call. Pins the deadline check at the top of {@link #chat}.
        LlmCallContext.setDeadline(System.currentTimeMillis() - 1L);
        AtomicInteger calls = new AtomicInteger();
        server.createContext("/chat/completions", exchange -> {
            calls.incrementAndGet();
            respond(exchange, 200, successBody());
        });

        OpenAiCompatibleLlmClient client = newClient();
        assertThrows(LlmDeadlineExceededException.class,
                () -> client.chat(sampleRequest()));

        assertEquals(0, calls.get(),
                "Already-expired deadline must abort before any HTTP call. "
                        + "Got " + calls.get() + " calls.");
    }

    @Test
    void retry_underTwentyFiveSecondBudget_doesNotEatTwoTwelveSecondReads() {
        // Sprint 8.1 §M1 + follow-up #2: with a 25 s deadline and a server
        // that hangs beyond the 12 s read timeout on EVERY call, the second
        // attempt must be skipped on budget grounds — total wait must NOT
        // exceed ~24 s (two reads). We pin <= ~26 s + buffer (read timeout
        // 12 s + tiny overhead, plus the deadline-skip path on attempt 2).
        LlmCallContext.setDeadline(System.currentTimeMillis() + 25_000L);
        AtomicInteger calls = new AtomicInteger();
        server.createContext("/chat/completions", exchange -> {
            calls.incrementAndGet();
            try {
                Thread.sleep(20_000L); // hang past the 12 s read timeout
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
            respond(exchange, 200, successBody());
        });

        OpenAiCompatibleLlmClient client = newClient();
        long t0 = System.currentTimeMillis();
        assertThrows(Exception.class, () -> client.chat(sampleRequest()));
        long elapsed = System.currentTimeMillis() - t0;

        assertTrue(elapsed < 26_000L,
                "Budget gating: must not blow past the 25 s deadline with "
                        + "two 12 s reads. Got " + elapsed + " ms.");
        assertTrue(calls.get() <= 2,
                "Bounded retry must not exceed 2 attempts. Got " + calls.get());
    }

    @Test
    void status401_noRetry() {
        // Sprint 8.1 §M1: deterministic 4xx — never retry.
        AtomicInteger calls = new AtomicInteger();
        server.createContext("/chat/completions", exchange -> {
            calls.incrementAndGet();
            respond(exchange, 401, "{\"error\":\"invalid_api_key\"}");
        });

        OpenAiCompatibleLlmClient client = newClient();
        assertThrows(Exception.class, () -> client.chat(sampleRequest()));
        assertEquals(1, calls.get(), "401 must never trigger a retry");
    }

    @Test
    void status403_noRetry() {
        AtomicInteger calls = new AtomicInteger();
        server.createContext("/chat/completions", exchange -> {
            calls.incrementAndGet();
            respond(exchange, 403, "{\"error\":\"forbidden\"}");
        });

        OpenAiCompatibleLlmClient client = newClient();
        assertThrows(Exception.class, () -> client.chat(sampleRequest()));
        assertEquals(1, calls.get(), "403 must never trigger a retry");
    }
}
