package com.gumtree.csagent.controller;

import com.gumtree.csagent.model.BotSession;
import com.gumtree.csagent.model.ChatSessionResponse;
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

        ChatSessionResponse response = sessionManager.createSession(
                firstName, email, topicSubject, adId, description);

        return ResponseEntity.ok(response);
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

        try {
            ChatSessionResponse response = sessionManager.processMessage(sessionId, message);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
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
