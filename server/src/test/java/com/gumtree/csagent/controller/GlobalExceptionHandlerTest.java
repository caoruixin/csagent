package com.gumtree.csagent.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * P1-A (2026-07-25) — a malformed request body is a <em>client</em> error and
 * must be answered with 4xx.
 *
 * <p>Observed defect: {@code curl -X POST /v1/chat/sessions -d '{"first_name":'}
 * returned {@code {"error":"JSON parse error: Unexpected end-of-input…",
 * "status":500}}. {@code HttpMessageNotReadableException} had no dedicated
 * handler, so it fell into {@link GlobalExceptionHandler#handleException} —
 * which reports 500 and logs at ERROR as "Unhandled exception". That
 * mis-classifies routine bad input as a server fault in both the API contract
 * and the log stream.
 *
 * <p>These tests exercise the advice through the real Spring MVC dispatch path
 * (standalone MockMvc + {@code setControllerAdvice}) so the assertion covers
 * handler <em>selection</em>, not just the handler body: a unit call to the
 * method would pass even if the advice never won over the {@code Exception}
 * catch-all.
 */
class GlobalExceptionHandlerTest {

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new ProbeController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("truncated JSON body -> 400, not 500")
    void truncatedJsonBody_returns400() throws Exception {
        mvc.perform(post("/probe/echo")
                        .contentType(MediaType.APPLICATION_JSON)
                        // The exact payload from the reproduction.
                        .content("{\"first_name\":"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Malformed request body"));
    }

    @Test
    @DisplayName("malformed body response leaks no parser internals or payload fragment")
    void malformedBody_doesNotLeakParserInternals() throws Exception {
        MvcResult result = mvc.perform(post("/probe/echo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"leak@example.com\", oops"))
                .andExpect(status().isBadRequest())
                .andReturn();

        String body = result.getResponse().getContentAsString();
        assertFalse(body.contains("JSON parse error"),
                "response must not echo the Jackson diagnostic: " + body);
        assertFalse(body.contains("com.fasterxml"),
                "response must not echo parser class names: " + body);
        assertFalse(body.contains("leak@example.com"),
                "response must not echo a fragment of the caller's payload: " + body);
    }

    @Test
    @DisplayName("wrong JSON shape for the target type -> 400")
    void wrongJsonShape_returns400() throws Exception {
        mvc.perform(post("/probe/echo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[1, 2, 3]"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("@Valid violation -> 400 naming the offending field")
    void validationFailure_returns400() throws Exception {
        mvc.perform(post("/probe/validated")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"topicSubject\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error")
                        .value(org.hamcrest.Matchers.containsString("topicSubject")));
    }

    @Test
    @DisplayName("un-convertible path variable -> 400")
    void typeMismatch_returns400() throws Exception {
        mvc.perform(get("/probe/number/not-a-number"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("IllegalArgumentException still maps to 400 with its own message")
    void illegalArgument_returns400_unchanged() throws Exception {
        mvc.perform(get("/probe/illegal-argument"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("session not found"));
    }

    @Test
    @DisplayName("a genuine server fault still maps to 500 (catch-all intact)")
    void serverFault_still500() throws Exception {
        mvc.perform(get("/probe/server-fault"))
                .andExpect(status().is5xxServerError())
                .andExpect(jsonPath("$.status").value(500));
    }

    // ------------------------------------------------------------------
    //  Probe controller — stands in for ChatController's request shapes.
    // ------------------------------------------------------------------

    @RestController
    static class ProbeController {

        @PostMapping("/probe/echo")
        Map<String, String> echo(@RequestBody Map<String, String> body) {
            return body;
        }

        @PostMapping("/probe/validated")
        String validated(@Valid @RequestBody ValidatedForm form) {
            return form.getTopicSubject();
        }

        @GetMapping("/probe/number/{n}")
        int number(@PathVariable("n") int n) {
            return n;
        }

        @GetMapping("/probe/illegal-argument")
        String illegalArgument() {
            throw new IllegalArgumentException("session not found");
        }

        @GetMapping("/probe/server-fault")
        String serverFault() {
            throw new IllegalStateException("database connection pool exhausted");
        }
    }

    static class ValidatedForm {
        @NotBlank(message = "must not be blank")
        private String topicSubject;

        public String getTopicSubject() {
            return topicSubject;
        }

        public void setTopicSubject(String topicSubject) {
            this.topicSubject = topicSubject;
        }
    }
}
