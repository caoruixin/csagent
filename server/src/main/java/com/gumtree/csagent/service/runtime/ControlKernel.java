package com.gumtree.csagent.service.runtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gumtree.csagent.model.*;
import com.gumtree.csagent.model.DriftResult.DriftType;
import com.gumtree.csagent.repository.BotEventRepository;
import com.gumtree.csagent.repository.BotTurnRepository;
import com.gumtree.csagent.service.observability.EventEmitter;
import com.gumtree.csagent.service.tools.CreateCaseControlledTool;
import com.gumtree.csagent.service.tools.ToolResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.*;

/**
 * Main orchestration engine for processing user messages.
 * Implements the 10-step control loop: increment turns, build projection,
 * check budgets, detect drift, evaluate phase, invoke LLM, parse action,
 * record turn, transition phase, return response.
 */
@Slf4j
@Service
public class ControlKernel {

    private final BotTurnRepository turnRepository;
    private final BotEventRepository eventRepository;
    private final BudgetChecker budgetChecker;
    private final DriftDetector driftDetector;
    private final PhaseEvaluator phaseEvaluator;
    private final ControlPolicyService controlPolicy;
    private final ObjectMapper objectMapper;
    private final CreateCaseControlledTool createCaseTool;
    private final EventEmitter eventEmitter;
    private final ContextProjectionBuilder contextProjectionBuilder;

    public ControlKernel(BotTurnRepository turnRepository,
                         BotEventRepository eventRepository,
                         BudgetChecker budgetChecker,
                         DriftDetector driftDetector,
                         PhaseEvaluator phaseEvaluator,
                         ControlPolicyService controlPolicy,
                         ObjectMapper objectMapper,
                         CreateCaseControlledTool createCaseTool,
                         EventEmitter eventEmitter,
                         ContextProjectionBuilder contextProjectionBuilder) {
        this.turnRepository = turnRepository;
        this.eventRepository = eventRepository;
        this.budgetChecker = budgetChecker;
        this.driftDetector = driftDetector;
        this.phaseEvaluator = phaseEvaluator;
        this.controlPolicy = controlPolicy;
        this.objectMapper = objectMapper;
        this.createCaseTool = createCaseTool;
        this.eventEmitter = eventEmitter;
        this.contextProjectionBuilder = contextProjectionBuilder;
    }

    /**
     * Process a user message within an existing session.
     *
     * @param session      the current bot session (will be modified in place)
     * @param userMessage  the user's message
     * @return the bot's response text
     */
    public KernelResult processMessage(BotSession session, String userMessage) {
        long startTime = System.currentTimeMillis();
        String phaseBefore = session.getCurrentPhase();

        // Step 1: Increment totalBotTurns
        session.setTotalBotTurns(session.getTotalBotTurns() + 1);
        log.info("Session {}: processing turn #{}, phase={}, uc={}",
                session.getSessionId(), session.getTotalBotTurns(),
                phaseBefore, session.getActiveUseCase());

        // Step 2: Context projection is built inside PhaseEvaluator as needed

        // Step 3: Check budgets — if exceeded, force ESCALATE
        Optional<String> exceededBudget = budgetChecker.checkBudgets(session);
        if (exceededBudget.isPresent()) {
            log.info("Session {}: budget '{}' exceeded, forcing ESCALATE",
                    session.getSessionId(), exceededBudget.get());
            session.setEscalationReason("budget_exceeded:" + exceededBudget.get());
            return forceEscalate(session, phaseBefore, userMessage, startTime,
                    "I've reached the limit of what I can assist with on this topic. " +
                    "Let me connect you with a human agent who can help further.");
        }

        // Step 4: Check drift — may update activeUseCase or force ESCALATE
        DriftResult drift = driftDetector.detect(session, userMessage);
        if (drift.getType() == DriftType.USER_ESCALATION_REQUEST || drift.isEscalationRequested()) {
            // User explicitly asked for a human agent — distinct from topic drift
            log.info("Session {}: user explicitly requested human agent", session.getSessionId());
            session.setEscalationReason("user_requested_escalation");
            return forceEscalate(session, phaseBefore, userMessage, startTime,
                    "No problem, let me connect you with a human agent right away.");
        }
        if (drift.getType() == DriftType.HARD_SHIFT) {
            if (drift.getNewUseCase() != null) {
                log.info("Session {}: hard drift shift to {}", session.getSessionId(), drift.getNewUseCase());
                session.setActiveUseCase(drift.getNewUseCase());
            }
            session.setEscalationReason("drift_hard_shift");
            return forceEscalate(session, phaseBefore, userMessage, startTime,
                    "I can see your concern has changed. Let me connect you with a specialist who can best assist you.");
        }

        // Step 5-6: Evaluate current phase (includes LLM invocation when needed)
        List<BotTurn> history = turnRepository.findBySessionIdOrderByTurnIndex(session.getSessionId());
        PhaseEvaluator.PhaseResult phaseResult = phaseEvaluator.evaluate(session, userMessage, history);

        // Step 7: Track repeated actions
        if (phaseResult.action() != null) {
            trackRepeatedAction(session, phaseResult.action().getAction());
        }

        // Step 8: Transition phase if needed
        String phaseAfter = phaseBefore;
        if (phaseResult.nextPhase() != null) {
            String targetPhase = phaseResult.nextPhase();
            if (controlPolicy.isValidTransition(phaseBefore, targetPhase)) {
                session.setCurrentPhase(targetPhase);
                phaseAfter = targetPhase;
                log.info("Session {}: phase transition {} -> {} (reason: {})",
                        session.getSessionId(), phaseBefore, phaseAfter, phaseResult.transitionReason());
            } else {
                log.warn("Session {}: invalid transition {} -> {}, staying in {}",
                        session.getSessionId(), phaseBefore, targetPhase, phaseBefore);
            }
        }

        // If we transitioned to RESOLVE or DISCOVER and have no response yet, re-evaluate
        String responseText = phaseResult.responseText();
        if (responseText == null && phaseResult.nextPhase() != null) {
            // Re-evaluate in the new phase
            PhaseEvaluator.PhaseResult reEval = phaseEvaluator.evaluate(session, userMessage, history);
            responseText = reEval.responseText();
            if (reEval.action() != null) {
                trackRepeatedAction(session, reEval.action().getAction());
            }
            if (reEval.nextPhase() != null && controlPolicy.isValidTransition(phaseAfter, reEval.nextPhase())) {
                session.setCurrentPhase(reEval.nextPhase());
                phaseAfter = reEval.nextPhase();
            }
            // Use the re-evaluation result for the turn record
            if (reEval.action() != null) {
                phaseResult = reEval;
            }
        }

        // Fallback response
        if (responseText == null) {
            responseText = "I'm looking into this for you. One moment please.";
        }

        // Step 9: Record turn
        long latencyMs = System.currentTimeMillis() - startTime;
        recordTurn(session, userMessage, phaseResult, phaseBefore, phaseAfter, latencyMs);

        // Step 10: Emit events (escalation events only — CLOSE events
        // are already emitted by PhaseEvaluator.evaluateClose())
        if (phaseResult.shouldEscalate()) {
            eventEmitter.emitEscalationRequested(session.getSessionId(), session.getTotalBotTurns(),
                    phaseResult.escalationReason());
            eventEmitter.emitOutcomeRecorded(session.getSessionId(), "ESCALATED", session.getActiveUseCase());
        }

        boolean shouldEndChat = "CLOSE".equals(phaseAfter) || "ESCALATE".equals(phaseAfter);
        session.setUpdatedAt(OffsetDateTime.now());

        return new KernelResult(responseText, shouldEndChat, latencyMs);
    }

    private KernelResult forceEscalate(BotSession session, String phaseBefore,
                                        String userMessage, long startTime, String message) {
        // Transition to ESCALATE
        if (controlPolicy.isValidTransition(phaseBefore, "ESCALATE")) {
            session.setCurrentPhase("ESCALATE");
        } else if (controlPolicy.isValidTransition(phaseBefore, "CLOSE")) {
            // Some phases can only go to CLOSE
            session.setCurrentPhase("CLOSE");
        }
        session.setHandlingState("QUEUE_TO_HUMAN");
        session.setContainmentOutcome("ESCALATED");

        long latencyMs = System.currentTimeMillis() - startTime;

        // Create case for intake UCs (UC-H/J/K) even on forced escalation
        createCaseIfNeeded(session);

        // Record the turn
        BotTurn turn = BotTurn.builder()
                .turnId(UUID.randomUUID().toString())
                .sessionId(session.getSessionId())
                .turnIndex(session.getTotalBotTurns())
                .userMessage(userMessage)
                .actionSelected("escalate_human")
                .botResponse(message)
                .phaseBefore(phaseBefore)
                .phaseAfter(session.getCurrentPhase())
                .activeUseCase(session.getActiveUseCase())
                .latencyMs((int) latencyMs)
                .createdAt(OffsetDateTime.now())
                .build();
        turnRepository.save(turn);

        emitEvent(session, "ESCALATION_REQUESTED", session.getTotalBotTurns(),
                String.format("{\"reason\":\"%s\"}", session.getEscalationReason()));

        session.setUpdatedAt(OffsetDateTime.now());
        return new KernelResult(message, true, latencyMs);
    }

    private void trackRepeatedAction(BotSession session, String action) {
        if (action != null && action.equals(session.getLastAction())) {
            session.setRepeatedActionCount(session.getRepeatedActionCount() + 1);
        } else {
            session.setRepeatedActionCount(0);
        }
        session.setLastAction(action);
    }

    /**
     * Create a case for intake UCs (UC-H, UC-J, UC-K) during forced escalation.
     * Non-blocking: failures logged but don't prevent escalation.
     */
    private void createCaseIfNeeded(BotSession session) {
        String activeUc = session.getActiveUseCase();
        Set<String> caseCreationUcs = Set.of("UC-H", "UC-J", "UC-K");
        if (activeUc == null || !caseCreationUcs.contains(activeUc)) {
            return;
        }
        if (session.getCaseId() != null && !session.getCaseId().isBlank()) {
            return; // Case already created
        }

        try {
            Map<String, Object> params = new LinkedHashMap<>();
            String subject = switch (activeUc) {
                case "UC-H" -> "Ad Support - Appeal";
                case "UC-J" -> "Report a Safety Issue";
                case "UC-K" -> "Technical Support Request";
                default -> activeUc + " case";
            };
            params.put("subject", subject);

            // Extract fields from form context
            if (session.getFormContext() != null) {
                try {
                    com.fasterxml.jackson.databind.JsonNode formNode = objectMapper.readTree(session.getFormContext());
                    if (formNode.has("description")) params.put("description", formNode.get("description").asText());
                    if (formNode.has("email")) params.put("email", formNode.get("email").asText());
                    if (formNode.has("ad_id")) params.put("ad_id", formNode.get("ad_id").asText());
                } catch (Exception e) {
                    params.put("description", "Escalated session " + session.getSessionId());
                }
            } else {
                params.put("description", "Escalated session " + session.getSessionId());
            }

            ToolResult result = createCaseTool.execute(session, params);
            if (result.isSuccess() && result.getData() != null) {
                String caseId = (String) result.getData().get("case_id");
                if (caseId != null) {
                    session.setCaseId(caseId);
                    log.info("Session {}: case created during forced escalation: {}", session.getSessionId(), caseId);
                    eventEmitter.emitCaseCreated(session.getSessionId(), session.getTotalBotTurns(), caseId, activeUc);
                }
            }
        } catch (Exception e) {
            log.warn("Session {}: case creation during forced escalation failed: {}", session.getSessionId(), e.getMessage());
        }
    }

    private void recordTurn(BotSession session, String userMessage,
                             PhaseEvaluator.PhaseResult phaseResult,
                             String phaseBefore, String phaseAfter, long latencyMs) {
        try {
            String actionSelected = null;
            String actionParams = null;
            String llmRawResponse = null;
            String[] sourceIds = null;

            if (phaseResult.action() != null) {
                actionSelected = phaseResult.action().getAction();
                if (phaseResult.action().getParameters() != null && !phaseResult.action().getParameters().isEmpty()) {
                    actionParams = objectMapper.writeValueAsString(phaseResult.action().getParameters());
                }
            }
            if (phaseResult.llmResponse() != null) {
                llmRawResponse = phaseResult.llmResponse().getContent();
            }
            if (phaseResult.knowledgeHits() != null) {
                sourceIds = phaseResult.knowledgeHits().stream()
                        .map(KnowledgeHit::getSourceId)
                        .toArray(String[]::new);
            }

            // Build projected context for trace persistence
            List<BotTurn> historyForProjection = turnRepository.findBySessionIdOrderByTurnIndex(session.getSessionId());
            List<KnowledgeHit> knowledgeHits = phaseResult.knowledgeHits();
            String projectedContext = contextProjectionBuilder.buildProjection(
                    session, historyForProjection, knowledgeHits, userMessage);

            BotTurn turn = BotTurn.builder()
                    .turnId(UUID.randomUUID().toString())
                    .sessionId(session.getSessionId())
                    .turnIndex(session.getTotalBotTurns())
                    .userMessage(userMessage)
                    .projectedContext(projectedContext)
                    .llmRawResponse(llmRawResponse)
                    .actionSelected(actionSelected)
                    .actionParameters(actionParams)
                    .botResponse(phaseResult.responseText())
                    .sourceIds(sourceIds)
                    .phaseBefore(phaseBefore)
                    .phaseAfter(phaseAfter)
                    .activeUseCase(session.getActiveUseCase())
                    .latencyMs((int) latencyMs)
                    .createdAt(OffsetDateTime.now())
                    .build();

            // D12.4: Populate tool_calls if knowledge was retrieved
            if (phaseResult.knowledgeHits() != null && !phaseResult.knowledgeHits().isEmpty()) {
                try {
                    List<Map<String, Object>> toolCallsList = new ArrayList<>();
                    Map<String, Object> searchCall = new LinkedHashMap<>();
                    searchCall.put("tool_name", "search_knowledge");
                    searchCall.put("status", "success");
                    searchCall.put("result_count", phaseResult.knowledgeHits().size());
                    toolCallsList.add(searchCall);
                    turn.setToolCalls(objectMapper.writeValueAsString(toolCallsList));
                } catch (Exception ex) {
                    log.warn("Failed to serialize tool_calls: {}", ex.getMessage());
                }
            }

            turnRepository.save(turn);

        } catch (Exception e) {
            log.error("Session {}: failed to record turn: {}", session.getSessionId(), e.getMessage(), e);
        }
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

    /**
     * Result from the control kernel processing.
     */
    public record KernelResult(String responseText, boolean shouldEndChat, long latencyMs) {}
}
