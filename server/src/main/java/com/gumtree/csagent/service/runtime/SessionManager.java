package com.gumtree.csagent.service.runtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gumtree.csagent.model.*;
import com.gumtree.csagent.repository.BotEventRepository;
import com.gumtree.csagent.repository.BotSessionRepository;
import com.gumtree.csagent.repository.MockHandoverLogRepository;
import com.gumtree.csagent.repository.SessionOutcomeRepository;
import com.gumtree.csagent.service.runtime.UseCaseRouter.RoutingResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.*;

/**
 * Session lifecycle manager: creates sessions, processes messages,
 * handles escalation handover, and records outcomes.
 */
@Slf4j
@Service
public class SessionManager {

    private final BotSessionRepository sessionRepository;
    private final BotEventRepository eventRepository;
    private final SessionOutcomeRepository outcomeRepository;
    private final MockHandoverLogRepository handoverLogRepository;
    private final FormContextIngestionService formIngestion;
    private final UseCaseRouter useCaseRouter;
    private final ControlKernel controlKernel;
    private final ControlPolicyService controlPolicy;
    private final UseCaseRegistryService useCaseRegistry;
    private final ObjectMapper objectMapper;

    public SessionManager(BotSessionRepository sessionRepository,
                          BotEventRepository eventRepository,
                          SessionOutcomeRepository outcomeRepository,
                          MockHandoverLogRepository handoverLogRepository,
                          FormContextIngestionService formIngestion,
                          UseCaseRouter useCaseRouter,
                          ControlKernel controlKernel,
                          ControlPolicyService controlPolicy,
                          UseCaseRegistryService useCaseRegistry,
                          ObjectMapper objectMapper) {
        this.sessionRepository = sessionRepository;
        this.eventRepository = eventRepository;
        this.outcomeRepository = outcomeRepository;
        this.handoverLogRepository = handoverLogRepository;
        this.formIngestion = formIngestion;
        this.useCaseRouter = useCaseRouter;
        this.controlKernel = controlKernel;
        this.controlPolicy = controlPolicy;
        this.useCaseRegistry = useCaseRegistry;
        this.objectMapper = objectMapper;
    }

    /**
     * Create a new chat session from the pre-chat form data.
     * Runs form ingestion, UC routing, transitions INIT -> DISCOVER,
     * and returns the session with an initial greeting.
     */
    @Transactional
    public ChatSessionResponse createSession(String firstName, String email,
                                              String topicSubject, String adId,
                                              String description) {
        // Sanitize all user-provided form fields upfront (XSS prevention)
        firstName = FormContextIngestionService.sanitize(firstName);
        email = FormContextIngestionService.sanitize(email);
        topicSubject = FormContextIngestionService.sanitize(topicSubject);
        adId = FormContextIngestionService.sanitize(adId);
        description = FormContextIngestionService.sanitize(description);

        String sessionId = UUID.randomUUID().toString();
        OffsetDateTime now = OffsetDateTime.now();

        // Create the session
        BotSession session = BotSession.builder()
                .sessionId(sessionId)
                .trafficVariant("bot_v1")
                .handlingState("BOT_HANDLING")
                .currentPhase("INIT")
                .totalBotTurns(0)
                .clarificationCount(0)
                .faqMissCount(0)
                .repeatedActionCount(0)
                .promptVersion("v1.0.0")
                .modelVersion("local")
                .projectionVersion("v1.0.0")
                .createdAt(now)
                .updatedAt(now)
                .build();

        // Step 1: Ingest form context
        formIngestion.ingest(session, firstName, email, topicSubject, adId, description);

        // Step 2: Route to use case (with graceful fallback)
        RoutingResult routingResult;
        try {
            routingResult = useCaseRouter.route(session, topicSubject, description);
        } catch (Exception e) {
            log.error("Session {}: routing failed, defaulting to DISCOVER with ask_user: {}",
                    sessionId, e.getMessage(), e);
            routingResult = RoutingResult.ambiguous(java.util.List.of());
        }

        // Step 3: Generate greeting and handle routing outcome
        String greeting;
        boolean shouldEndChat = false;

        switch (routingResult.outcome()) {
            case ROUTED -> {
                // Transition INIT -> DISCOVER
                session.setCurrentPhase("DISCOVER");
                // Since we have a UC, immediately transition to RESOLVE
                // (DISCOVER phase checks if UC is identified and moves to RESOLVE)
                if (session.getActiveUseCase() != null) {
                    session.setCurrentPhase("RESOLVE");
                }
                greeting = buildGreeting(firstName, topicSubject, session.getActiveUseCase());
            }
            case OUT_OF_SCOPE -> {
                // Handover-only or out of scope — immediately escalate
                session.setCurrentPhase("ESCALATE");
                session.setHandlingState("QUEUE_TO_HUMAN");
                session.setContainmentOutcome("ESCALATED");
                session.setEscalationReason("out_of_scope:" + routingResult.outOfScopeReason());
                greeting = buildOutOfScopeGreeting(firstName, topicSubject);
                shouldEndChat = true;
            }
            case AMBIGUOUS -> {
                // Move to DISCOVER to disambiguate
                session.setCurrentPhase("DISCOVER");
                greeting = buildAmbiguousGreeting(firstName, topicSubject);
            }
            default -> {
                session.setCurrentPhase("DISCOVER");
                greeting = buildGreeting(firstName, topicSubject, null);
            }
        }

        // Ensure array fields are never null to prevent persistence issues
        if (session.getCandidateUseCases() == null) {
            session.setCandidateUseCases(new String[0]);
        }
        if (session.getArticlesShown() == null) {
            session.setArticlesShown(new String[0]);
        }

        // Save session
        session.setUpdatedAt(OffsetDateTime.now());
        sessionRepository.save(session);

        // Emit session_started event
        emitEvent(session, "SESSION_STARTED", 0,
                String.format("{\"topic_subject\":\"%s\",\"routing_outcome\":\"%s\"}",
                        topicSubject, routingResult.outcome()));

        if (routingResult.outcome() == RoutingResult.RoutingOutcome.ROUTED) {
            emitEvent(session, "USE_CASE_INFERRED", 0,
                    String.format("{\"use_case\":\"%s\",\"confidence\":%s}",
                            routingResult.activeUseCase(), routingResult.confidence()));
        }

        // If OOS, also record the handover
        if (routingResult.outcome() == RoutingResult.RoutingOutcome.OUT_OF_SCOPE) {
            emitEvent(session, "OUT_OF_SCOPE_HANDOVER", 0,
                    String.format("{\"reason\":\"%s\"}", routingResult.outOfScopeReason()));
            recordOutcome(session);
            recordHandover(session);
        }

        log.info("Session {} created: phase={}, uc={}, routing={}",
                sessionId, session.getCurrentPhase(), session.getActiveUseCase(), routingResult.outcome());

        return ChatSessionResponse.builder()
                .sessionId(sessionId)
                .replyText(greeting)
                .intent(session.getActiveUseCase())
                .shouldEndChat(shouldEndChat)
                .additionalData(Map.of())
                .build();
    }

    /**
     * Process a user message in an existing session.
     */
    @Transactional
    public ChatSessionResponse processMessage(String sessionId, String userMessage) {
        BotSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found: " + sessionId));

        // Check if session is already closed or escalated
        if ("CLOSED".equals(session.getHandlingState()) || "CLOSE".equals(session.getCurrentPhase())) {
            return ChatSessionResponse.builder()
                    .sessionId(sessionId)
                    .replyText("This session has ended. Please start a new chat if you need further assistance.")
                    .intent(session.getActiveUseCase())
                    .shouldEndChat(true)
                    .additionalData(Map.of())
                    .build();
        }
        if ("ESCALATE".equals(session.getCurrentPhase())
                || "QUEUE_TO_HUMAN".equals(session.getHandlingState())
                || "HUMAN_HANDLING".equals(session.getHandlingState())) {
            return ChatSessionResponse.builder()
                    .sessionId(sessionId)
                    .replyText("This conversation has been transferred to a human agent. Please wait for them to respond.")
                    .intent(session.getActiveUseCase())
                    .shouldEndChat(true)
                    .additionalData(Map.of())
                    .build();
        }

        // Process through control kernel
        ControlKernel.KernelResult result = controlKernel.processMessage(session, userMessage);

        // If the session is ending, record outcome and handle escalation
        if (result.shouldEndChat()) {
            if ("ESCALATE".equals(session.getCurrentPhase()) || "QUEUE_TO_HUMAN".equals(session.getHandlingState())) {
                recordOutcome(session);
                recordHandover(session);
            } else if ("CLOSE".equals(session.getCurrentPhase())) {
                session.setClosedAt(OffsetDateTime.now());
                recordOutcome(session);
            }
        }

        // Save session state
        session.setUpdatedAt(OffsetDateTime.now());
        sessionRepository.save(session);

        return ChatSessionResponse.builder()
                .sessionId(sessionId)
                .replyText(result.responseText())
                .intent(session.getActiveUseCase())
                .shouldEndChat(result.shouldEndChat())
                .additionalData(Map.of("latency_ms", result.latencyMs()))
                .build();
    }

    /**
     * Get the current session state.
     */
    public BotSession getSession(String sessionId) {
        return sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found: " + sessionId));
    }

    // --- Greeting builders ---

    private String buildGreeting(String firstName, String topicSubject, String activeUc) {
        String name = (firstName != null && !firstName.isBlank()) ? firstName : "there";
        String topicSummary = buildTopicSummary(topicSubject, activeUc);
        return String.format("Hi %s! I'm here to help with your inquiry about %s. Let me look into this for you.", name, topicSummary);
    }

    private String buildOutOfScopeGreeting(String firstName, String topicSubject) {
        String name = (firstName != null && !firstName.isBlank()) ? firstName : "there";
        return String.format("Hi %s! Thank you for reaching out about %s. " +
                "This type of inquiry requires assistance from a specialist. " +
                "I'm connecting you with a human agent now.",
                name, topicSubject != null ? topicSubject : "your inquiry");
    }

    private String buildAmbiguousGreeting(String firstName, String topicSubject) {
        String name = (firstName != null && !firstName.isBlank()) ? firstName : "there";
        return String.format("Hi %s! Thank you for reaching out. " +
                "I'd like to help you with your inquiry. Could you tell me a bit more about what you need help with?",
                name);
    }

    private String buildTopicSummary(String topicSubject, String activeUc) {
        if (activeUc != null) {
            UseCaseRegistryService.UseCaseDefinition ucDef = useCaseRegistry.getUseCase(activeUc);
            if (ucDef != null) {
                return ucDef.name().toLowerCase();
            }
        }
        if (topicSubject != null && !topicSubject.isBlank()) {
            return topicSubject.toLowerCase();
        }
        return "your inquiry";
    }

    // --- Outcome and handover recording ---

    private void recordOutcome(BotSession session) {
        try {
            String outcome = session.getContainmentOutcome() != null
                    ? session.getContainmentOutcome() : "ESCALATED";

            SessionOutcome sessionOutcome = SessionOutcome.builder()
                    .sessionId(session.getSessionId())
                    .outcome(outcome)
                    .useCaseId(session.getActiveUseCase())
                    .escalationReason(session.getEscalationReason())
                    .articlesShown(session.getArticlesShown())
                    .totalTurns(session.getTotalBotTurns())
                    .createdAt(OffsetDateTime.now())
                    .build();
            outcomeRepository.save(sessionOutcome);

            emitEvent(session, "OUTCOME_RECORDED", session.getTotalBotTurns(),
                    String.format("{\"outcome\":\"%s\"}", outcome));

        } catch (Exception e) {
            log.error("Session {}: failed to record outcome: {}", session.getSessionId(), e.getMessage(), e);
        }
    }

    private void recordHandover(BotSession session) {
        try {
            String status = session.getEscalationReason() != null
                    && session.getEscalationReason().startsWith("intake_complete")
                    ? "intake_complete" : "escalation_triggered";

            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("version", "1.0");
            payload.put("session_id", session.getSessionId());
            payload.put("bot_session_id", session.getSfBotSessionId());
            payload.put("primary_use_case", session.getActiveUseCase());
            payload.put("candidate_use_cases",
                    session.getCandidateUseCases() != null
                            ? Arrays.asList(session.getCandidateUseCases()) : List.of());
            payload.put("current_status", status);
            payload.put("summary", buildSummary(session));
            payload.put("intent_confidence", session.getIntentConfidence());
            payload.put("clarification_count", session.getClarificationCount());
            payload.put("faq_miss_count", session.getFaqMissCount());
            payload.put("articles_shown",
                    session.getArticlesShown() != null
                            ? Arrays.asList(session.getArticlesShown()) : List.of());
            payload.put("escalation_reason", session.getEscalationReason());
            payload.put("transcript_ref", "bot_session:" + session.getSessionId());
            payload.put("case_id", session.getCaseId());
            payload.put("identifiers_collected", buildIdentifiers(session));
            payload.put("intake_fields",
                    session.getIntakeFields() != null ? session.getIntakeFields() : "{}");
            payload.put("total_bot_turns", session.getTotalBotTurns());
            payload.put("handling_duration_seconds", calculateDuration(session));
            payload.put("form_topic_subject", session.getFormTopicSubject());
            payload.put("topic_uc_mismatch", checkMismatch(session));
            payload.put("prompt_version", session.getPromptVersion());
            payload.put("model_version", session.getModelVersion());

            String payloadJson = objectMapper.writeValueAsString(payload);

            MockHandoverLog handoverLog = MockHandoverLog.builder()
                    .logId(UUID.randomUUID().toString())
                    .sessionId(session.getSessionId())
                    .handoverPayload(payloadJson)
                    .transferResult("mock_transfer")
                    .createdAt(OffsetDateTime.now())
                    .build();
            handoverLogRepository.save(handoverLog);

            log.info("Session {}: handover recorded with {} fields", session.getSessionId(), payload.size());

        } catch (Exception e) {
            log.error("Session {}: failed to record handover: {}", session.getSessionId(), e.getMessage(), e);
        }
    }

    /**
     * Build a human-readable summary from session state for the handover payload.
     */
    private String buildSummary(BotSession session) {
        StringBuilder sb = new StringBuilder();
        if (session.getActiveUseCase() != null) {
            UseCaseRegistryService.UseCaseDefinition ucDef = useCaseRegistry.getUseCase(session.getActiveUseCase());
            sb.append("Use case: ").append(ucDef != null ? ucDef.name() : session.getActiveUseCase()).append(". ");
        }
        // Include form description if available
        try {
            if (session.getFormContext() != null) {
                var formNode = objectMapper.readTree(session.getFormContext());
                String desc = formNode.has("description") ? formNode.get("description").asText("") : "";
                if (!desc.isBlank()) {
                    sb.append("Customer issue: ").append(desc).append(". ");
                }
            }
        } catch (Exception ignored) { /* best effort */ }
        if (session.getArticlesShown() != null && session.getArticlesShown().length > 0) {
            sb.append("Articles shown: ").append(String.join(", ", session.getArticlesShown())).append(". ");
        }
        if (session.getEscalationReason() != null) {
            sb.append("Escalation reason: ").append(session.getEscalationReason()).append(".");
        }
        return sb.toString().trim();
    }

    /**
     * Extract identifiers (email, ad_id) from the formContext JSON.
     */
    private Map<String, String> buildIdentifiers(BotSession session) {
        Map<String, String> identifiers = new LinkedHashMap<>();
        try {
            if (session.getFormContext() != null) {
                var formNode = objectMapper.readTree(session.getFormContext());
                if (formNode.has("email") && !formNode.get("email").asText("").isBlank()) {
                    identifiers.put("email", formNode.get("email").asText());
                }
                if (formNode.has("ad_id") && !formNode.get("ad_id").asText("").isBlank()) {
                    identifiers.put("ad_id", formNode.get("ad_id").asText());
                }
            }
        } catch (Exception ignored) { /* best effort */ }
        return identifiers;
    }

    /**
     * Calculate the session duration in seconds between createdAt and now.
     */
    private long calculateDuration(BotSession session) {
        if (session.getCreatedAt() == null) {
            return 0L;
        }
        return Duration.between(session.getCreatedAt(), OffsetDateTime.now()).getSeconds();
    }

    /**
     * Check if the form topic subject's expected UC differs from the active use case.
     * Returns true if there is a mismatch.
     */
    private boolean checkMismatch(BotSession session) {
        if (session.getFormTopicSubject() == null || session.getActiveUseCase() == null) {
            return false;
        }
        Optional<String> strongPrior = useCaseRegistry.getStrongPriorTopic(session.getFormTopicSubject());
        if (strongPrior.isPresent()) {
            return !strongPrior.get().equals(session.getActiveUseCase());
        }
        // For weak prior topics, check if the UC is in the candidate set for that topic
        List<String> candidates = useCaseRegistry.getCandidateUcsForTopic(session.getFormTopicSubject());
        if (!candidates.isEmpty()) {
            return !candidates.contains(session.getActiveUseCase());
        }
        return false;
    }

    private void emitEvent(BotSession session, String eventType, int turnIndex, String payload) {
        try {
            BotEvent event = BotEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .sessionId(session.getSessionId())
                    .eventType(eventType)
                    .turnIndex(turnIndex)
                    .payload(payload)
                    .createdAt(OffsetDateTime.now())
                    .build();
            eventRepository.save(event);
        } catch (Exception e) {
            log.error("Session {}: failed to emit event {}: {}", session.getSessionId(), eventType, e.getMessage());
        }
    }
}
