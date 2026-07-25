package com.gumtree.csagent.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleBadRequest(IllegalArgumentException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("error", ex.getMessage());
        body.put("status", 400);
        return ResponseEntity.badRequest().body(body);
    }

    /**
     * Malformed / truncated / wrong-typed request body.
     *
     * <p>Spring raises {@link HttpMessageNotReadableException} when Jackson
     * cannot deserialize the request body at all (e.g. a truncated
     * {@code {"first_name":} payload). This is a <em>client</em> error: the
     * server behaved correctly by refusing it. Before this handler existed the
     * exception fell through to {@link #handleException} and the caller got a
     * {@code 500} plus an "Unhandled exception" ERROR log line, which made a
     * routine bad request look like a server defect in both the API contract
     * and the log stream.
     *
     * <p>The response body reuses the same {@code {error, status}} shape as
     * every other handler here, but carries a stable, generic message rather
     * than the raw Jackson diagnostic — the underlying message embeds parser
     * internals and a fragment of the caller's payload, neither of which
     * belongs in an API response. The detail is preserved at WARN in the log.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleUnreadableBody(HttpMessageNotReadableException ex) {
        log.warn("Malformed request body rejected with 400: {}", ex.getMessage());
        return badRequest("Malformed request body");
    }

    /**
     * Bean-validation failure on an {@code @Valid @RequestBody} argument.
     * Same rationale as {@link #handleUnreadableBody}: a client-side input
     * error must not be reported as a server fault. Field errors are
     * summarised into the message so the caller can fix the request; no
     * stack trace or internal type name is exposed.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationFailure(MethodArgumentNotValidException ex) {
        StringBuilder detail = new StringBuilder("Request validation failed");
        if (ex.getBindingResult().hasFieldErrors()) {
            detail.append(": ");
            boolean first = true;
            for (org.springframework.validation.FieldError fe : ex.getBindingResult().getFieldErrors()) {
                if (!first) {
                    detail.append("; ");
                }
                detail.append(fe.getField()).append(' ')
                        .append(fe.getDefaultMessage() == null ? "is invalid" : fe.getDefaultMessage());
                first = false;
            }
        }
        log.warn("Request validation failed, rejected with 400: {}", detail);
        return badRequest(detail.toString());
    }

    /**
     * Missing required query / form parameter — a client error, per
     * {@link #handleUnreadableBody}.
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Map<String, Object>> handleMissingParameter(
            MissingServletRequestParameterException ex) {
        log.warn("Missing request parameter, rejected with 400: {}", ex.getMessage());
        return badRequest("Missing required parameter: " + ex.getParameterName());
    }

    /**
     * Un-convertible path variable / request parameter (e.g. {@code abc} for an
     * {@code int} path segment) — a client error, per
     * {@link #handleUnreadableBody}.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex) {
        log.warn("Un-convertible request parameter, rejected with 400: {}", ex.getMessage());
        return badRequest("Invalid value for parameter: " + ex.getName());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleException(Exception ex) {
        log.error("Unhandled exception", ex);
        Map<String, Object> body = new HashMap<>();
        body.put("error", ex.getMessage());
        body.put("status", 500);
        return ResponseEntity.status(500).body(body);
    }

    private static ResponseEntity<Map<String, Object>> badRequest(String message) {
        Map<String, Object> body = new HashMap<>();
        body.put("error", message);
        body.put("status", 400);
        return ResponseEntity.badRequest().body(body);
    }
}
