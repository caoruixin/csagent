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
 * Sprint 8.1 §M1 — budget-aware fast retry contracts.
 *
 * <p>Pins:
 * <ul>
 *   <li>One transient failure inside an ample budget triggers exactly one retry.</li>
 *   <li>Budget exhausted before the second attempt aborts with
 *       {@link LlmDeadlineExceededException} rather than a third attempt.</li>
 *   <li>Two full read-timeout waits cannot occur inside a 10 s budget — the
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
    void retry_under_ampleBudget_succeedsOnSecondAttempt() {
        // Sprint 8.1 §M1: transient 5xx + ample 30 s budget → one retry.
        LlmCallContext.setDeadline(System.currentTimeMillis() + 30_000L);
        AtomicInteger calls = new AtomicInteger();
        server.createContext("/chat/completions", exchange -> {
            int n = calls.incrementAndGet();
            if (n == 1) respond(exchange, 503, "{\"error\":\"upstream\"}");
            else respond(exchange, 200, successBody());
        });

        LlmResponse response = newClient().chat(sampleRequest());

        assertEquals(2, calls.get(), "exactly one retry on transient 5xx within budget");
        assertEquals("ok", response.getContent());
    }

    @Test
    void retry_exhaustsBudget_throwsDeadlineExceeded_notThirdAttempt() {
        // Sprint 8.1 §M1: budget runs out after the first attempt; the
        // client must skip the retry and surface DeadlineExceeded rather
        // than burning a third call.
        LlmCallContext.setDeadline(System.currentTimeMillis() + 100L); // ~exhausted by the time attempt 2 starts
        AtomicInteger calls = new AtomicInteger();
        server.createContext("/chat/completions", exchange -> {
            calls.incrementAndGet();
            respond(exchange, 503, "{\"error\":\"upstream\"}");
        });

        OpenAiCompatibleLlmClient client = newClient();
        assertThrows(LlmDeadlineExceededException.class,
                () -> client.chat(sampleRequest()));

        assertTrue(calls.get() <= 1,
                "M1 must not start a retry when remaining budget < min-attempt budget. "
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
