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
import org.springframework.web.client.HttpStatusCodeException;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Sprint §C1: verify the bounded retry / no-retry classification for the
 * bot-side LLM call. Uses an embedded {@link HttpServer} so we exercise the
 * real {@code RestTemplate} path without hitting the network.
 *
 * <p>Tested:
 * <ul>
 *   <li>Transient HTTP 429 → one retry, success on second attempt.</li>
 *   <li>Transient HTTP 503 → one retry, success on second attempt.</li>
 *   <li>Two consecutive 5xx → exhausts retry, throws after exactly 2 attempts.</li>
 *   <li>HTTP 401 (auth) → NO retry, throws after exactly 1 attempt.</li>
 *   <li>HTTP 400 (bad request) → NO retry, throws after exactly 1 attempt.</li>
 * </ul>
 *
 * <p>Cross-provider fallback is handled at {@link FallbackLlmClient} (one
 * layer up); this test focuses solely on the inner-provider retry.
 */
class OpenAiCompatibleLlmClientRetryTest {

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
        if (server != null) {
            server.stop(0);
        }
    }

    private OpenAiCompatibleLlmClient newClient() {
        return new OpenAiCompatibleLlmClient(
                "kimi", "sk-test-key", "http://127.0.0.1:" + port, "kimi-k2.6", false, objectMapper);
    }

    private LlmRequest sampleRequest() {
        return LlmRequest.builder()
                .systemPrompt("test")
                .messages(List.of(ChatMessage.builder().role("user").content("hello").build()))
                .temperature(0.0)
                .maxTokens(100)
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
    void chat_429ThenSuccess_retriesOnceAndReturnsResponse() {
        AtomicInteger calls = new AtomicInteger();
        server.createContext("/chat/completions", exchange -> {
            int n = calls.incrementAndGet();
            if (n == 1) {
                respond(exchange, 429, "{\"error\":\"rate_limit_exceeded\"}");
            } else {
                respond(exchange, 200, successBody());
            }
        });

        LlmResponse response = newClient().chat(sampleRequest());

        assertEquals(2, calls.get(),
                "429 must trigger exactly one retry — total of 2 attempts.");
        assertEquals("ok", response.getContent());
    }

    @Test
    void chat_503ThenSuccess_retriesOnceAndReturnsResponse() {
        AtomicInteger calls = new AtomicInteger();
        server.createContext("/chat/completions", exchange -> {
            int n = calls.incrementAndGet();
            if (n == 1) {
                respond(exchange, 503, "{\"error\":\"upstream_unavailable\"}");
            } else {
                respond(exchange, 200, successBody());
            }
        });

        LlmResponse response = newClient().chat(sampleRequest());

        assertEquals(2, calls.get(),
                "5xx must trigger exactly one retry — total of 2 attempts.");
        assertEquals("ok", response.getContent());
    }

    @Test
    void chat_twoConsecutive5xx_exhaustsRetryAndThrowsAfter2Attempts() {
        AtomicInteger calls = new AtomicInteger();
        server.createContext("/chat/completions", exchange -> {
            calls.incrementAndGet();
            respond(exchange, 502, "{\"error\":\"bad_gateway\"}");
        });

        OpenAiCompatibleLlmClient client = newClient();
        LlmRequest req = sampleRequest();
        Exception ex = assertThrows(Exception.class, () -> client.chat(req));

        assertEquals(2, calls.get(),
                "Bounded retry must cap at exactly 2 attempts.");
        assertTrue(ex instanceof HttpStatusCodeException
                        || (ex.getCause() != null && ex.getCause() instanceof HttpStatusCodeException),
                "After exhaustion the original HttpStatusCodeException (or its wrapper) must propagate.");
    }

    @Test
    void chat_401_noRetry_throwsImmediately() {
        AtomicInteger calls = new AtomicInteger();
        server.createContext("/chat/completions", exchange -> {
            calls.incrementAndGet();
            respond(exchange, 401, "{\"error\":\"invalid_api_key\"}");
        });

        OpenAiCompatibleLlmClient client = newClient();
        LlmRequest req = sampleRequest();
        Exception ex = assertThrows(Exception.class, () -> client.chat(req));

        assertEquals(1, calls.get(),
                "401 (auth) must NOT trigger retry — exactly 1 attempt.");
        assertTrue(ex instanceof HttpStatusCodeException
                        || (ex.getCause() != null && ex.getCause() instanceof HttpStatusCodeException),
                "401 must propagate as HttpStatusCodeException so FallbackLlmClient can classify it as non-transient.");
    }

    @Test
    void chat_400_noRetry_throwsImmediately() {
        AtomicInteger calls = new AtomicInteger();
        server.createContext("/chat/completions", exchange -> {
            calls.incrementAndGet();
            respond(exchange, 400, "{\"error\":\"bad_request\"}");
        });

        OpenAiCompatibleLlmClient client = newClient();
        LlmRequest req = sampleRequest();
        assertThrows(Exception.class, () -> client.chat(req));

        assertEquals(1, calls.get(),
                "400 (bad request — not a tool-scope violation but a malformed deterministic request) "
                        + "must NOT trigger retry.");
    }

    @Test
    void chat_blankApiKey_failsFastWithoutHittingNetwork() {
        AtomicInteger calls = new AtomicInteger();
        server.createContext("/chat/completions", exchange -> {
            calls.incrementAndGet();
            respond(exchange, 200, successBody());
        });

        OpenAiCompatibleLlmClient client = new OpenAiCompatibleLlmClient(
                "kimi", "", "http://127.0.0.1:" + port, "kimi-k2.6", false, objectMapper);
        LlmRequest req = sampleRequest();

        Exception ex = assertThrows(Exception.class, () -> client.chat(req));
        // Either IllegalStateException (preferred) or wrapped — but the
        // important thing is no network call.
        assertTrue(ex instanceof IllegalStateException
                        || (ex.getCause() != null && ex.getCause() instanceof IllegalStateException),
                "Blank api-key must fail fast with IllegalStateException (or wrapper). Got: "
                        + ex.getClass().getName() + ": " + ex.getMessage());
        assertEquals(0, calls.get(),
                "Blank api-key must NOT result in any HTTP call.");
    }
}
