package com.gumtree.csagent.service.runtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gumtree.csagent.model.*;
import com.gumtree.csagent.repository.BotEventRepository;
import com.gumtree.csagent.repository.BotSessionRepository;
import com.gumtree.csagent.repository.BotTurnRepository;
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
    private final BotTurnRepository botTurnRepository;
    private final FormContextIngestionService formIngestion;
    private final UseCaseRouter useCaseRouter;
    private final ControlKernel controlKernel;
    private final ControlPolicyService controlPolicy;
    private final UseCaseRegistryService useCaseRegistry;
    private final ObjectMapper objectMapper;
    private final HandoverPayloadAssembler handoverPayloadAssembler;
    private final EscalationReasonResolver escalationResolver;

    public SessionManager(BotSessionRepository sessionRepository,
                          BotEventRepository eventRepository,
                          SessionOutcomeRepository outcomeRepository,
                          MockHandoverLogRepository handoverLogRepository,
                          BotTurnRepository botTurnRepository,
                          FormContextIngestionService formIngestion,
                          UseCaseRouter useCaseRouter,
                          ControlKernel controlKernel,
                          ControlPolicyService controlPolicy,
                          UseCaseRegistryService useCaseRegistry,
                          ObjectMapper objectMapper,
                          HandoverPayloadAssembler handoverPayloadAssembler,
                          EscalationReasonResolver escalationResolver) {
        this.sessionRepository = sessionRepository;
        this.eventRepository = eventRepository;
        this.outcomeRepository = outcomeRepository;
        this.handoverLogRepository = handoverLogRepository;
        this.botTurnRepository = botTurnRepository;
        this.formIngestion = formIngestion;
        this.useCaseRouter = useCaseRouter;
        this.controlKernel = controlKernel;
        this.controlPolicy = controlPolicy;
        this.useCaseRegistry = useCaseRegistry;
        this.objectMapper = objectMapper;
        this.handoverPayloadAssembler = handoverPayloadAssembler;
        this.escalationResolver = escalationResolver;
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
        // NOTE: articlesShown and candidateUseCases are initialised to empty arrays
        // here because the schema declares both columns NOT NULL with DEFAULT '{}',
        // and at least one code path (FAQ auto-search at the ROUTED branch below)
        // calls sessionRepository.save(session) BEFORE the defensive null-coalesce
        // block further down. Hibernate maps a null Java field to SQL NULL (the
        // column DEFAULT does not apply on INSERT when a value is explicitly
        // provided), which violates the NOT NULL constraint and pollutes the
        // transaction with rollback-only state. Initialising in the builder
        // ensures every save path is safe.
        BotSession session = BotSession.builder()
                .sessionId(sessionId)
                .trafficVariant("bot_v1")
                .handlingState("BOT_HANDLING")
                .currentPhase("INIT")
                .totalBotTurns(0)
                .clarificationCount(0)
                .faqMissCount(0)
                .repeatedActionCount(0)
                .runtimeErrorCount(0)
                .consecutiveDeadlineCount(0)
                .articlesShown(new String[0])
                .candidateUseCases(new String[0])
                .promptVersion("v1.0.0")
                .modelVersion("local")
                .projectionVersion("v1.0.0")
                .createdAt(now)
                .updatedAt(now)
                .build();

        // Step 1: Ingest form context
        formIngestion.ingest(session, firstName, email, topicSubject, adId, description);

        // Step 2: Route to use case via deterministic rules only.
        // Sprint 8.1 §M0: the create-session path must never block on an LLM
        // call. {@link UseCaseRouter#routeNonBlocking} runs the deterministic
        // stages (handover-only / strong-prior / UC-K regression / B2 bias)
        // and falls back to AMBIGUOUS for weak-prior topics that would
        // otherwise need LLM disambiguation. The user lands in DISCOVER and
        // the next message exercises classify_use_case inside the per-turn
        // deadline budget.
        RoutingResult routingResult;
        try {
            routingResult = useCaseRouter.routeNonBlocking(session, topicSubject, description);
        } catch (Exception e) {
            log.error("Session {}: routing failed, defaulting to DISCOVER with clarifying question: {}",
                    sessionId, e.getMessage(), e);
            routingResult = RoutingResult.ambiguous(java.util.List.of());
        }

        // Step 3: Generate greeting and handle routing outcome
        String greeting;
        boolean shouldEndChat = false;

        switch (routingResult.outcome()) {
            case ROUTED -> {
                // Sprint 8 §D1: session creation must NEVER block on an LLM call.
                // The previous "auto-search at create-time" branch ran a full
                // ControlKernel.processMessage chain (routing → knowledge search
                // → rerank fan-out → answer) synchronously, which routinely
                // exceeded the user-facing wait budget. The user now lands in
                // the chat box immediately with a static greeting; their first
                // message exercises the normal processMessage path.
                session.setCurrentPhase("DISCOVER");
                if (session.getActiveUseCase() != null) {
                    // UC already identified → skip DISCOVER and start in RESOLVE
                    // for the user's first message.
                    session.setCurrentPhase("RESOLVE");
                }
                greeting = buildGreeting(firstName, topicSubject, session.getActiveUseCase());
            }
            case OUT_OF_SCOPE -> {
                // Step 3a: differentiate soft OOS (UNKNOWN_TOPIC — no UC candidates
                // matched the topic) from hard OOS (handover-only topic explicitly
                // mapped to e.g. OUT_OF_SCOPE_DELIVERY). Soft OOS gets one
                // clarifying turn in DISCOVER before any escalation; hard OOS
                // escalates immediately as in Step 2.5b.
                if ("UNKNOWN_TOPIC".equals(routingResult.outOfScopeReason())) {
                    session.setCurrentPhase("DISCOVER");
                    log.info("Session {}: soft OOS (UNKNOWN_TOPIC) -> DISCOVER for one clarifying turn",
                            session.getSessionId());
                    greeting = buildAmbiguousGreeting(firstName, topicSubject);
                } else {
                    // Hard OOS: handover-only topic — immediately escalate.
                    // escalation_reason MUST be canonical (request_handover tool enum,
                    // eval_interactive case_spec/schema.py:43-66). Routing detail is
                    // logged for debugging but not stuffed into the enum value.
                    session.setCurrentPhase("ESCALATE");
                    session.setHandlingState("QUEUE_TO_HUMAN");
                    session.setContainmentOutcome("escalated");
                    session.setEscalationReason("out_of_scope");
                    log.info("Session {}: OUT_OF_SCOPE routing detail={}",
                            session.getSessionId(), routingResult.outOfScopeReason());
                    greeting = buildOutOfScopeGreeting(firstName, topicSubject);
                    shouldEndChat = true;
                }
            }
            case AMBIGUOUS -> {
                // Sprint 31 — Option β: preserve the intake-time alternate-UC
                // snapshot the router considered plausible for this
                // topic-subject family. Until Sprint 31 this list was
                // discarded; the slot is now persisted onto the session and
                // surfaced as the per-turn `alternate_candidate_use_cases`
                // projection soft signal. The LLM owns whether to act on it;
                // the runtime does NOT branch on the value.
                List<String> ambiguousCandidates = routingResult.ambiguousCandidates();
                session.setIntakeAmbiguousCandidates(
                        ambiguousCandidates == null
                                ? null
                                : ambiguousCandidates.toArray(new String[0]));
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

        // For hard OOS, persist a synthetic bot_turns row carrying the
        // request_handover tool_call so the eval trace has L1 evidence of
        // escalation compliance even though no LLM turn ever ran. See phase0
        // §0.3 (form data == bot's "turn 0") and phase3 §3.3.3 (single-layer
        // tool-use contract). Must happen AFTER sessionRepository.save so the
        // bot_turns FK constraint is satisfied. Wrapped in try/catch so trace
        // insertion never fails session creation.
        // Step 3a: skip this for soft OOS (UNKNOWN_TOPIC) — those route to
        // DISCOVER for a clarifying turn, not to ESCALATE.
        boolean isHardOos = routingResult.outcome() == RoutingResult.RoutingOutcome.OUT_OF_SCOPE
                && !"UNKNOWN_TOPIC".equals(routingResult.outOfScopeReason());
        if (isHardOos) {
            try {
                String turn0UserMessage = (description != null && !description.isBlank())
                        ? description : topicSubject;
                List<Map<String, Object>> toolCallsList = new ArrayList<>();
                toolCallsList.add(controlKernel.synthesizeHandoverToolCall(session.getEscalationReason()));
                String toolCallsJson = objectMapper.writeValueAsString(toolCallsList);

                BotTurn oosTurn = BotTurn.builder()
                        .turnId(UUID.randomUUID().toString())
                        .sessionId(session.getSessionId())
                        .turnIndex(0)
                        .userMessage(turn0UserMessage)
                        .botResponse(greeting)
                        .phaseBefore("INIT")
                        .phaseAfter("ESCALATE")
                        .activeUseCase(null)
                        .latencyMs(0)
                        .toolCalls(toolCallsJson)
                        .createdAt(OffsetDateTime.now())
                        .build();
                botTurnRepository.save(oosTurn);
                session.setTotalBotTurns(session.getTotalBotTurns() + 1);
                session.setUpdatedAt(OffsetDateTime.now());
                sessionRepository.save(session);
            } catch (Exception e) {
                log.warn("Session {}: failed to persist synthetic OOS handover turn: {}",
                        session.getSessionId(), e.getMessage());
            }
        }

        // Emit session_started event
        emitEvent(session, "SESSION_STARTED", 0,
                String.format("{\"topic_subject\":\"%s\",\"routing_outcome\":\"%s\"}",
                        topicSubject, routingResult.outcome()));

        if (routingResult.outcome() == RoutingResult.RoutingOutcome.ROUTED) {
            emitEvent(session, "USE_CASE_INFERRED", 0,
                    String.format("{\"use_case\":\"%s\",\"confidence\":%s}",
                            routingResult.activeUseCase(), routingResult.confidence()));
        }

        // If hard OOS, also record the handover. Soft OOS (UNKNOWN_TOPIC) is
        // routed to DISCOVER for one clarifying turn (Step 3a) and so must NOT
        // record a handover at session-creation time.
        if (isHardOos) {
            emitEvent(session, "OUT_OF_SCOPE_HANDOVER", 0,
                    String.format("{\"reason\":\"%s\"}", routingResult.outOfScopeReason()));
            recordOutcome(session);
            recordHandover(session);
        }

        // Sprint 8 §D1: the previous "auto-search escalated mid-create" branch
        // is removed along with auto-search itself. Session creation no longer
        // produces an ESCALATE outcome on the ROUTED path — only the hard-OOS
        // branch above can escalate at create-time, and that branch already
        // records its own outcome / handover.

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
            // Sprint 3.1 P1: when session-create's auto-search has already
            // escalated the session with a lower-priority reason
            // (faq_miss_threshold_exceeded / turn_budget_exhausted) and the
            // user's NEXT message carries a higher-priority semantic signal
            // (user_distress, user_requested), we still need to honour the
            // sprint-3 cs_002 contract: the canonical session reason, the
            // persisted request_handover tool-call argument, and the
            // handover payload all become the higher-priority semantic
            // reason. Without this rewrite, ControlKernel.processMessage
            // step 2.4 never sees the incoming distress turn (we early-
            // return below) and the cs_002 transcript reports the distress
            // utterance while every persisted surface still says
            // ``faq_miss_threshold_exceeded``.
            //
            // Contract preserved: the user-facing transfer message and
            // shouldEndChat=true behaviour are unchanged. We do NOT create
            // a second handover row — the existing MockHandoverLog is
            // updated in-place via its log_id.
            reconcileEscalationReasonOnAlreadyEscalatedSession(session, userMessage);
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

    /**
     * Sprint 3.1 P1 — reconcile {@code session.escalationReason} when an
     * already-escalated session receives a user message containing a
     * higher-priority semantic signal.
     *
     * <p>Specifically:
     * <ul>
     *   <li>{@code user_requested} (priority 1) wins over everything except
     *       {@code imminent_harm}.</li>
     *   <li>{@code user_distress} (priority 2) wins over budget reasons
     *       ({@code faq_miss_threshold_exceeded} priority 41,
     *       {@code turn_budget_exhausted} priority 42,
     *       {@code clarification_budget_exhausted} priority 40).</li>
     * </ul>
     *
     * <p>The rewrite is performed in-place across three surfaces so
     * the L1 {@code escalation_reason_consistency} contract holds:
     * <ol>
     *   <li>{@code session.escalationReason} via the resolver.</li>
     *   <li>Every existing {@code bot_turns.tool_calls}
     *       {@code request_handover} entry's
     *       {@code arguments.escalation_reason} via
     *       {@link ControlKernel#normalizeHandoverArgsToSessionReason}.</li>
     *   <li>The existing {@code mock_handover_log.handover_payload}
     *       (re-assembled and saved through the same {@code log_id}; no
     *       second handover row is created).</li>
     * </ol>
     *
     * <p>Returns {@code true} when the canonical session reason was
     * actually upgraded.
     */
    boolean reconcileEscalationReasonOnAlreadyEscalatedSession(
            BotSession session, String userMessage) {
        String candidate = null;
        if (escalationResolver.detectExplicitUserEscalation(userMessage)) {
            candidate = "user_requested";
        } else if (escalationResolver.detectDistressSignal(userMessage)) {
            candidate = "user_distress";
        }
        if (candidate == null) {
            return false;
        }
        String existing = session.getEscalationReason();
        String resolved = escalationResolver.resolve(existing, candidate);
        if (resolved == null || resolved.equals(existing)) {
            return false;
        }
        log.info("Session {}: reconciling escalation_reason on already-escalated session: {} -> {} (signal={})",
                session.getSessionId(), existing, resolved, candidate);
        session.setEscalationReason(resolved);
        session.setUpdatedAt(OffsetDateTime.now());
        sessionRepository.save(session);

        // Surface 2: normalise every persisted ``request_handover`` tool
        // call argument so the persisted trace agrees with the new
        // canonical reason. Re-uses the §B0 helper on ControlKernel.
        try {
            List<BotTurn> turns = botTurnRepository.findBySessionIdOrderByTurnIndex(session.getSessionId());
            for (BotTurn turn : turns) {
                String json = turn.getToolCalls();
                if (json == null || json.isBlank()) {
                    continue;
                }
                try {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> toolCallsList = objectMapper.readValue(json, List.class);
                    if (controlKernel.normalizeHandoverArgsToSessionReason(session, toolCallsList)) {
                        turn.setToolCalls(objectMapper.writeValueAsString(toolCallsList));
                        botTurnRepository.save(turn);
                    }
                } catch (Exception ex) {
                    log.warn("Session {}: turn {} tool_calls JSON unparseable, skipping normalize: {}",
                            session.getSessionId(), turn.getTurnIndex(), ex.getMessage());
                }
            }
        } catch (Exception ex) {
            log.warn("Session {}: failed to normalize bot_turns tool_calls after reconciling reason: {}",
                    session.getSessionId(), ex.getMessage());
        }

        // Surface 3: rewrite the existing handover payload in-place so the
        // handover log carries the new canonical reason. We update via the
        // existing log_id (no second handover row).
        try {
            List<MockHandoverLog> logs = handoverLogRepository.findBySessionId(session.getSessionId());
            for (MockHandoverLog hLog : logs) {
                Map<String, Object> payload = handoverPayloadAssembler.assemble(session);
                payload.put("handling_duration_seconds", calculateDuration(session));
                payload.put("topic_uc_mismatch", checkMismatch(session));
                hLog.setHandoverPayload(objectMapper.writeValueAsString(payload));
                handoverLogRepository.save(hLog);
            }
        } catch (Exception ex) {
            log.warn("Session {}: failed to rewrite handover payload after reconciling reason: {}",
                    session.getSessionId(), ex.getMessage());
        }
        return true;
    }

    private void recordOutcome(BotSession session) {
        try {
            String outcome = session.getContainmentOutcome() != null
                    ? session.getContainmentOutcome() : "escalated";

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

    /**
     * Sprint 16 §H0 — known-unspecced-surface marker (no behaviour
     * change in Sprint 16). This method is the second independently-wired
     * local handover writer on the LLM-driven path; the first is
     * {@code RequestHandoverTool} →
     * {@code SalesforceService.requestHandover(...)}. The future
     * invariant frozen by {@code docs/handover_orchestrator_design.md}
     * §3.3 / §6 is: for each {@code session_id}, at most one
     * transmitted / {@code offline_logged} handover decision may
     * exist; the future {@code HandoverOrchestrator} is the single
     * owner and must be idempotent by {@code session_id}. This method
     * MUST eventually delegate to the orchestrator (or be removed in
     * favour of the orchestrator firing once at the kernel boundary).
     * See {@code Sprint16HandoverDualPathReproTest} for the
     * characterization repro and {@code docs/release_gate.md} §1.1
     * for the cutover blocker.
     */
    private void recordHandover(BotSession session) {
        try {
            // Sprint §A1: ensure the session reason persists as a canonical
            // 23-value enum even when older code paths stamped a literal
            // (``user_requested_escalation`` etc.). The deterministic
            // {@link EscalationReasonResolver} maps non-canonical values to
            // ``service_degraded`` rather than letting them slip into the
            // payload and trip the L1 enum gate.
            String canonicalReason = escalationResolver.canonicalize(session.getEscalationReason());
            if (canonicalReason != null && !canonicalReason.equals(session.getEscalationReason())) {
                session.setEscalationReason(canonicalReason);
            }

            // Sprint §A3: hand the payload assembly off to the dedicated
            // service. The assembler builds an issue-specific summary,
            // identifiers, source/status checks, and partial-answer-or-blocker
            // line deterministically from session state — independent of
            // whatever (if any) summary the LLM produced.
            Map<String, Object> payload = handoverPayloadAssembler.assemble(session);
            payload.put("handling_duration_seconds", calculateDuration(session));
            payload.put("topic_uc_mismatch", checkMismatch(session));

            String payloadJson = objectMapper.writeValueAsString(payload);

            // Build customer message (escalation summary shown to user)
            String customerMessage = buildCustomerMessage(session);

            // Build conversation transcript from bot turns
            String transcriptJson = buildTranscriptJson(session.getSessionId());

            MockHandoverLog handoverLog = MockHandoverLog.builder()
                    .logId(UUID.randomUUID().toString())
                    .sessionId(session.getSessionId())
                    .handoverPayload(payloadJson)
                    .customerMessage(customerMessage)
                    .transcript(transcriptJson)
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
     * Build the customer-facing escalation message.
     */
    private String buildCustomerMessage(BotSession session) {
        String reason = session.getEscalationReason();
        if (reason != null && reason.startsWith("out_of_scope")) {
            return "Your inquiry requires assistance from a specialist. You have been connected to a human agent.";
        }
        return "This conversation has been transferred to a human agent who can better assist you.";
    }

    /**
     * Build a JSON array of transcript entries from bot turns for the session.
     */
    private String buildTranscriptJson(String sessionId) {
        try {
            List<BotTurn> turns = botTurnRepository.findBySessionIdOrderByTurnIndex(sessionId);
            List<Map<String, Object>> transcript = new ArrayList<>();
            for (BotTurn turn : turns) {
                if (turn.getUserMessage() != null && !turn.getUserMessage().isBlank()) {
                    Map<String, Object> userEntry = new LinkedHashMap<>();
                    userEntry.put("role", "user");
                    userEntry.put("message", turn.getUserMessage());
                    userEntry.put("turn_index", turn.getTurnIndex());
                    transcript.add(userEntry);
                }
                if (turn.getBotResponse() != null && !turn.getBotResponse().isBlank()) {
                    Map<String, Object> botEntry = new LinkedHashMap<>();
                    botEntry.put("role", "bot");
                    botEntry.put("message", turn.getBotResponse());
                    botEntry.put("turn_index", turn.getTurnIndex());
                    transcript.add(botEntry);
                }
            }
            return objectMapper.writeValueAsString(transcript);
        } catch (Exception e) {
            log.warn("Session {}: failed to build transcript: {}", sessionId, e.getMessage());
            return "[]";
        }
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
