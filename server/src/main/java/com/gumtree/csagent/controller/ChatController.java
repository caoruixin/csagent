package com.gumtree.csagent.controller;

import com.gumtree.csagent.model.BotSession;
import com.gumtree.csagent.model.ChatSessionResponse;
import com.gumtree.csagent.service.llm.LlmCallContext;
import com.gumtree.csagent.service.llm.LlmDeadlineExceededException;
import com.gumtree.csagent.service.llm.LlmUnavailableException;
import com.gumtree.csagent.service.runtime.SessionManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Chat API controller for session creation, message processing, and session retrieval.
 */
@Slf4j
@RestController
@RequestMapping("/v1/chat")
public class ChatController {

    /**
     * Sprint 8 §C: user-facing wall-clock budget for any single HTTP request
     * that may invoke an LLM. The frontend axios timeout is 30s; we keep the
     * server budget well under that so the user always gets a graceful
     * "we gave up" response rather than the raw axios timeout error string.
     */
    private static final long USER_FACING_LLM_DEADLINE_MS = 10_000L;

    private final SessionManager sessionManager;

    public ChatController(SessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    /**
     * Create a new chat session from pre-chat form data.
     *
     * POST /v1/chat/sessions
     * Body: { "first_name": "...", "email": "...", "topic_subject": "...", "ad_id": "...", "description": "..." }
     */
    @PostMapping("/sessions")
    public ResponseEntity<ChatSessionResponse> createSession(@RequestBody Map<String, String> body) {
        String firstName = body.get("first_name");
        String email = body.get("email");
        String topicSubject = body.get("topic_subject");
        String adId = body.get("ad_id");
        String description = body.get("description");

        if (topicSubject == null || topicSubject.isBlank()) {
            return ResponseEntity.badRequest().body(
                    ChatSessionResponse.builder()
                            .replyText("topic_subject is required")
                            .shouldEndChat(true)
                            .build()
            );
        }

        log.info("Creating session: firstName={}, topicSubject={}", firstName, topicSubject);

        LlmCallContext.setDeadline(System.currentTimeMillis() + USER_FACING_LLM_DEADLINE_MS);
        try {
            ChatSessionResponse response = sessionManager.createSession(
                    firstName, email, topicSubject, adId, description);
            return ResponseEntity.ok(response);
        } finally {
            LlmCallContext.clear();
        }
    }

    /**
     * Process a user message in an existing session.
     *
     * POST /v1/chat/sessions/{id}/messages
     * Body: { "message": "..." }
     */
    @PostMapping("/sessions/{id}/messages")
    public ResponseEntity<ChatSessionResponse> processMessage(
            @PathVariable("id") String sessionId,
            @RequestBody Map<String, String> body) {

        String message = body.get("message");
        if (message == null || message.isBlank()) {
            return ResponseEntity.badRequest().body(
                    ChatSessionResponse.builder()
                            .sessionId(sessionId)
                            .replyText("message is required")
                            .shouldEndChat(false)
                            .build()
            );
        }

        log.info("Processing message for session {}: {}",
                sessionId, message.length() > 100 ? message.substring(0, 100) + "..." : message);

        LlmCallContext.setDeadline(System.currentTimeMillis() + USER_FACING_LLM_DEADLINE_MS);
        try {
            ChatSessionResponse response = sessionManager.processMessage(sessionId, message);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (LlmDeadlineExceededException e) {
            // Fail-fast give-up: respond gracefully rather than a 500 with the
            // raw exception. Keeps the chat box open so the user can retry.
            // Reaches this branch only when the deadline propagates past the
            // AgentRunLoop catch (legacy PhaseEvaluator path); the AgentRunLoop
            // path returns TerminalOutcome.DEADLINE_EXCEEDED and is handled
            // gracefully by the kernel before getting here.
            log.warn("Session {}: LLM deadline exceeded — returning graceful give-up response", sessionId);
            return ResponseEntity.ok(ChatSessionResponse.builder()
                    .sessionId(sessionId)
                    .replyText("Sorry, I'm a bit slow right now. Please try sending that again in a moment.")
                    .shouldEndChat(false)
                    .additionalData(Map.of("llm_deadline_exceeded", true))
                    .build());
        } catch (LlmUnavailableException e) {
            // Sprint 8.1 §M2: same defensive shape for the LLM-unavailable
            // surface. The AgentRunLoop normally converts this to
            // TerminalOutcome.LLM_UNAVAILABLE and returns gracefully; this
            // catch only fires if the legacy PhaseEvaluator path leaks the
            // exception. The user-facing message stays honest about the infra
            // failure rather than rendering a fake business escalation.
            log.warn("Session {}: LLM unavailable (failure_class={}) — returning graceful give-up response",
                    sessionId, e.getFailureClass());
            return ResponseEntity.ok(ChatSessionResponse.builder()
                    .sessionId(sessionId)
                    .replyText("Sorry, I'm having trouble reaching the assistant right now. "
                            + "Please try again in a moment.")
                    .shouldEndChat(false)
                    .additionalData(Map.of("llm_unavailable", true,
                            "failure_class", e.getFailureClass()))
                    .build());
        } finally {
            LlmCallContext.clear();
        }
    }

    /**
     * Get the current session state.
     *
     * GET /v1/chat/sessions/{id}
     */
    @GetMapping("/sessions/{id}")
    public ResponseEntity<BotSession> getSession(@PathVariable("id") String sessionId) {
        try {
            BotSession session = sessionManager.getSession(sessionId);
            return ResponseEntity.ok(session);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
