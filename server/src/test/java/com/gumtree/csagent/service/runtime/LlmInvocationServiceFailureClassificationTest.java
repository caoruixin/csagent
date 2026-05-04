package com.gumtree.csagent.service.runtime;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Sprint §C1: ensure the trace classifier on
 * {@link LlmInvocationService#classifyFailure} produces stable tags so the
 * eval analysis can distinguish auth-config flakes from transport flakes
 * without parsing free-form messages.
 */
class LlmInvocationServiceFailureClassificationTest {

    @Test
    void classify_401_isLlmAuthError() {
        assertEquals("llm_auth_error",
                LlmInvocationService.classifyFailure(
                        HttpClientErrorException.create(HttpStatus.UNAUTHORIZED, "Unauthorized",
                                org.springframework.http.HttpHeaders.EMPTY, new byte[0], null)));
    }

    @Test
    void classify_403_isLlmAuthError() {
        assertEquals("llm_auth_error",
                LlmInvocationService.classifyFailure(
                        HttpClientErrorException.create(HttpStatus.FORBIDDEN, "Forbidden",
                                org.springframework.http.HttpHeaders.EMPTY, new byte[0], null)));
    }

    @Test
    void classify_429_isLlmRateLimited() {
        assertEquals("llm_rate_limited",
                LlmInvocationService.classifyFailure(
                        HttpClientErrorException.create(HttpStatus.TOO_MANY_REQUESTS, "Too Many",
                                org.springframework.http.HttpHeaders.EMPTY, new byte[0], null)));
    }

    @Test
    void classify_503_isLlmServerError() {
        assertEquals("llm_server_error",
                LlmInvocationService.classifyFailure(
                        HttpServerErrorException.create(HttpStatus.SERVICE_UNAVAILABLE, "503",
                                org.springframework.http.HttpHeaders.EMPTY, new byte[0], null)));
    }

    @Test
    void classify_socketTimeout_isLlmTimeout() {
        assertEquals("llm_timeout",
                LlmInvocationService.classifyFailure(new SocketTimeoutException("read timed out")));
    }

    @Test
    void classify_connectFailure_isLlmConnectFailed() {
        assertEquals("llm_connect_failed",
                LlmInvocationService.classifyFailure(new ConnectException("Connection refused")));
    }

    @Test
    void classify_resourceAccessException_isTransportError() {
        assertEquals("llm_transport_error",
                LlmInvocationService.classifyFailure(new ResourceAccessException("read failed",
                        new IOException("read"))));
    }

    @Test
    void classify_wrappedRuntimeException_walksCauseChain() {
        RuntimeException wrapped = new RuntimeException("LLM API call failed after retry",
                new SocketTimeoutException("read timed out"));
        assertEquals("llm_timeout", LlmInvocationService.classifyFailure(wrapped));
    }

    @Test
    void classify_unknown_isLlmUnknownError() {
        assertEquals("llm_unknown_error",
                LlmInvocationService.classifyFailure(new IllegalStateException("weird")));
    }
}
