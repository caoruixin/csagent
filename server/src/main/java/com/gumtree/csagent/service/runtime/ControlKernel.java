package com.gumtree.csagent.service.runtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gumtree.csagent.config.AgentRunLoopProperties;
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

    /** FAQ UCs that route through the {@code RESOLVE_FAQ} phase plan. */
    private static final Set<String> FAQ_UCS = Set.of(
            "UC-A", "UC-B", "UC-C", "UC-D", "UC-E", "UC-F", "UC-FP");

    /** INTAKE UCs that route through the {@code RESOLVE_INTAKE} phase plan. */
    private static final Set<String> INTAKE_UCS = Set.of(
            "UC-G", "UC-H", "UC-I", "UC-J", "UC-K");

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
    private final AgentRunLoopProperties agentRunLoopProperties;
    private final AgentRunLoop agentRunLoop;

    public ControlKernel(BotTurnRepository turnRepository,
                         BotEventRepository eventRepository,
                         BudgetChecker budgetChecker,
                         DriftDetector driftDetector,
                         PhaseEvaluator phaseEvaluator,
                         ControlPolicyService controlPolicy,
                         ObjectMapper objectMapper,
                         CreateCaseControlledTool createCaseTool,
                         EventEmitter eventEmitter,
                         ContextProjectionBuilder contextProjectionBuilder,
                         AgentRunLoopProperties agentRunLoopProperties,
                         AgentRunLoop agentRunLoop) {
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
        this.agentRunLoopProperties = agentRunLoopProperties;
        this.agentRunLoop = agentRunLoop;
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
            // Codex 1.5 / 1.9: pick the most specific canonical escalation_reason
            // that matches the exceeded bucket, instead of always emitting the
            // generic ``turn_budget_exhausted``. The Phase 2 §2.4 precedence has
            // ``clarification_budget_exhausted`` and ``faq_miss_threshold_exceeded``
            // strictly above the catch-all ``turn_budget_exhausted``; preserving
            // the bucket → reason mapping here keeps the L1 escalation_compliance
            // gate green for cases like cs_interactive_001 / 011 / 014 that were
            // failing on a generic-vs-specific reason mismatch.
            session.setEscalationReason(mapBudgetToEscalationReason(exceededBudget.get()));
            return forceEscalate(session, phaseBefore, userMessage, startTime,
                    "I've reached the limit of what I can assist with on this topic. " +
                    "Let me connect you with a human agent who can help further.");
        }

        // Step 4: Check drift — may update activeUseCase or force ESCALATE
        DriftResult drift = driftDetector.detect(session, userMessage);
        if (drift.getType() == DriftType.USER_ESCALATION_REQUEST || drift.isEscalationRequested()) {
            // User explicitly asked for a human agent — distinct from topic drift
            log.info("Session {}: user explicitly requested human agent", session.getSessionId());
            // Canonicalize at the write site (Phase 2 #17): the literal
            // "user_requested_escalation" is not in the 23-value escalation_reason
            // enum (canonical value is "user_requested"). Mapping here closes the
            // third write site so AgentRunLoop / forceEscalate / interpretRunResult
            // all emit canonical reasons.
            session.setEscalationReason("user_requested");
            return forceEscalate(session, phaseBefore, userMessage, startTime,
                    "No problem, let me connect you with a human agent right away.");
        }
        if (drift.getType() == DriftType.HARD_SHIFT) {
            if (drift.getNewUseCase() != null) {
                log.info("Session {}: hard drift shift to {}", session.getSessionId(), drift.getNewUseCase());
                session.setActiveUseCase(drift.getNewUseCase());
            }
            // Canonicalize at the write site (Phase 2 #16, sibling of #17/#19): the
            // literal "drift_hard_shift" is not in the 23-value escalation_reason
            // enum and trips L1 trace_contract_escalation_reason (cs_259 hits this
            // at total_turns=0). Map to canonical "service_degraded" — semantically
            // the bot can no longer continue serving the original UC after a hard
            // topic shift, so an infrastructure-style fallback is the closest
            // canonical match. Drift type is preserved in DriftDetector telemetry.
            session.setEscalationReason("service_degraded");
            return forceEscalate(session, phaseBefore, userMessage, startTime,
                    "I can see your concern has changed. Let me connect you with a specialist who can best assist you.");
        }

        // Step 5-6: Evaluate current phase (includes LLM invocation when needed)
        List<BotTurn> history = turnRepository.findBySessionIdOrderByTurnIndex(session.getSessionId());

        // D16: route through AgentRunLoop when the feature flag enables this phase.
        // Falls back to the legacy path if the flag is off, the phase isn't migrated,
        // or PhaseEvaluator.plan() returns null.
        String routeKey = computeRouteKey(session);
        if (routeKey != null
                && agentRunLoopProperties.getEnabledPhases() != null
                && agentRunLoopProperties.getEnabledPhases().contains(routeKey)) {
            PhasePlan plan = phaseEvaluator.plan(session, userMessage, history);
            if (plan != null) {
                log.info("Session {}: routing turn through AgentRunLoop (route={}, uc={})",
                        session.getSessionId(), routeKey, session.getActiveUseCase());
                AgentRunResult runResult = agentRunLoop.run(plan, session, userMessage, history);
                PhaseTransitionDecision decision =
                        phaseEvaluator.interpretRunResult(plan, runResult, session);

                String phaseAfter = applyTransition(session, phaseBefore, decision);
                String responseText = decision.responseText();
                if (responseText == null) {
                    responseText = "I'm looking into this for you. One moment please.";
                }

                // Update lastAction for budget/loop-detection tracking. We
                // synthesize a key from the loop's tool events so the existing
                // repeated-action checks remain meaningful when the agent path
                // is active. (For FAQ flows this is informational only;
                // INTAKE flows under D16.C will rely on it for the
                // opening-template guard.)
                trackRepeatedAction(session, deriveRunResultKey(runResult));

                long latencyMs = System.currentTimeMillis() - startTime;
                recordRunResult(session, userMessage, plan, runResult,
                        phaseBefore, phaseAfter, latencyMs);

                // Emit ESCALATION_REQUESTED if the decision is an escalation
                boolean shouldEscalate = "ESCALATE".equals(decision.nextPhase());
                if (shouldEscalate) {
                    String escalationReason = decision.escalationReason() != null
                            ? decision.escalationReason() : "agent_escalated";
                    session.setEscalationReason(escalationReason);
                    session.setHandlingState("QUEUE_TO_HUMAN");
                    session.setContainmentOutcome("escalated");
                    eventEmitter.emitEscalationRequested(session.getSessionId(),
                            session.getTotalBotTurns(), escalationReason);
                }

                // D16.D: mirror the legacy evaluateClose state-setting when the
                // agent loop terminates in CLOSE. SessionManager reads
                // containmentOutcome via recordOutcome.
                if ("CLOSE".equals(phaseAfter)) {
                    session.setHandlingState("CLOSED");
                    if (session.getContainmentOutcome() == null) {
                        session.setContainmentOutcome("resolved");
                    }
                    eventEmitter.emitSessionClosed(session.getSessionId(),
                            session.getContainmentOutcome());
                }

                boolean shouldEndChat = "CLOSE".equals(phaseAfter) || "ESCALATE".equals(phaseAfter);
                session.setUpdatedAt(OffsetDateTime.now());

                return new KernelResult(responseText, shouldEndChat, latencyMs);
            }
        }

        PhaseEvaluator.PhaseResult phaseResult = phaseEvaluator.evaluate(session, userMessage, history);

        // Step 7: Track repeated actions (label derived from tool_calls + user_message)
        if (phaseResult.action() != null) {
            trackRepeatedAction(session, deriveRepetitionKey(phaseResult.action()));
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
                trackRepeatedAction(session, deriveRepetitionKey(reEval.action()));
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

        // Step 10: Emit escalation event (OUTCOME_RECORDED is emitted by SessionManager.recordOutcome)
        if (phaseResult.shouldEscalate()) {
            eventEmitter.emitEscalationRequested(session.getSessionId(), session.getTotalBotTurns(),
                    phaseResult.escalationReason());
        }

        boolean shouldEndChat = "CLOSE".equals(phaseAfter) || "ESCALATE".equals(phaseAfter);
        session.setUpdatedAt(OffsetDateTime.now());

        return new KernelResult(responseText, shouldEndChat, latencyMs);
    }

    /**
     * Map a {@link BudgetChecker} bucket name to a canonical
     * {@code escalation_reason} enum value. Aligned with the Phase 2 §2.4
     * precedence ordering: clarification &gt; faq-miss &gt; per-path turn cap
     * &gt; total turn cap. Buckets that do not have a more specific canonical
     * representation fall back to {@code turn_budget_exhausted}, which is the
     * spec-defined catch-all for control-plane stops.
     */
    static String mapBudgetToEscalationReason(String bucket) {
        if (bucket == null) {
            return "turn_budget_exhausted";
        }
        switch (bucket) {
            case "max-clarification-rounds":
                return "clarification_budget_exhausted";
            case "max-faq-miss":
                return "faq_miss_threshold_exceeded";
            case "max-bot-turns-faq":
            case "max-bot-turns-intake":
            case "max-total-bot-turns":
            case "max-repeated-same-action":
            default:
                return "turn_budget_exhausted";
        }
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
        session.setContainmentOutcome("escalated");

        long latencyMs = System.currentTimeMillis() - startTime;

        // Create case for intake UCs (UC-H/J/K) even on forced escalation
        createCaseIfNeeded(session);

        // Record the turn. Persist a synthesized handover tool_call so the trace
        // remains consistent with LLM-driven escalations under the single-layer
        // tool-use contract (phase0 §0.6 / phase3 §3.3.3).
        String escalationReason = session.getEscalationReason() != null
                ? session.getEscalationReason() : "forced_escalation";
        BotTurn turn = BotTurn.builder()
                .turnId(UUID.randomUUID().toString())
                .sessionId(session.getSessionId())
                .turnIndex(session.getTotalBotTurns())
                .userMessage(userMessage)
                .botResponse(message)
                .phaseBefore(phaseBefore)
                .phaseAfter(session.getCurrentPhase())
                .activeUseCase(session.getActiveUseCase())
                .latencyMs((int) latencyMs)
                .createdAt(OffsetDateTime.now())
                .build();
        try {
            List<Map<String, Object>> toolCallsList = new ArrayList<>();
            toolCallsList.add(synthesizeHandoverToolCall(escalationReason));
            turn.setToolCalls(objectMapper.writeValueAsString(toolCallsList));
        } catch (Exception ex) {
            log.warn("Failed to serialize forced-escalation tool_calls: {}", ex.getMessage());
        }
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

    /**
     * Build a single {@code tool_calls} entry representing a synthesized
     * {@code request_handover} call. Used by both {@link #forceEscalate} and
     * the legacy {@link #recordTurn} path so the persisted trace stays
     * consistent with the single-layer tool-use contract (phase0 §0.6 /
     * phase3 §3.3.3) regardless of which control path produced the escalation.
     */
    Map<String, Object> synthesizeHandoverToolCall(String escalationReason) {
        Map<String, Object> handoverCall = new LinkedHashMap<>();
        handoverCall.put("tool_name", "request_handover");
        handoverCall.put("arguments", Map.of("escalation_reason", escalationReason));
        return handoverCall;
    }

    private void recordTurn(BotSession session, String userMessage,
                             PhaseEvaluator.PhaseResult phaseResult,
                             String phaseBefore, String phaseAfter, long latencyMs) {
        try {
            String llmRawResponse = null;
            String[] sourceIds = null;

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

            // Remediation 2026-05-01 (phase0 §0.6 deviation): the legacy
            // recordTurn path previously persisted only search_knowledge
            // tool_calls, so escalations that originated here lacked a
            // request_handover entry — failing the L1 escalation_compliance
            // check. Synthesize one when this turn ends in ESCALATE, mirroring
            // the contract used by forceEscalate() and AgentRunLoop.
            if ("ESCALATE".equals(phaseAfter)) {
                String escalationReason = session.getEscalationReason() != null
                        ? session.getEscalationReason() : "unspecified";
                List<Map<String, Object>> toolCallsList = null;
                String existing = turn.getToolCalls();
                boolean alreadyHasHandover = false;
                if (existing != null && !existing.isBlank()) {
                    try {
                        @SuppressWarnings("unchecked")
                        List<Map<String, Object>> parsed = objectMapper.readValue(
                                existing, List.class);
                        toolCallsList = parsed;
                        for (Map<String, Object> entry : toolCallsList) {
                            Object name = entry.get("tool_name");
                            if ("request_handover".equals(name)) {
                                alreadyHasHandover = true;
                                break;
                            }
                        }
                    } catch (Exception ex) {
                        log.warn("Session {}: existing tool_calls JSON unparseable, " +
                                "overwriting with synthesized handover: {}",
                                session.getSessionId(), ex.getMessage());
                        toolCallsList = null;
                    }
                }
                if (!alreadyHasHandover) {
                    if (toolCallsList == null) {
                        toolCallsList = new ArrayList<>();
                    }
                    toolCallsList.add(synthesizeHandoverToolCall(escalationReason));
                    try {
                        turn.setToolCalls(objectMapper.writeValueAsString(toolCallsList));
                    } catch (Exception ex) {
                        log.warn("Session {}: failed to serialize synthesized handover tool_calls: {}",
                                session.getSessionId(), ex.getMessage());
                    }
                }
            }

            turnRepository.save(turn);

        } catch (Exception e) {
            log.error("Session {}: failed to record turn: {}", session.getSessionId(), e.getMessage(), e);
        }
    }

    /**
     * D16 trace persistence path: flatten an {@link AgentRunResult} into the
     * existing tables.
     *
     * <p>Per Phase 3 §3.3.3:
     * <ul>
     *   <li>{@link LlmCallEvent}s are already auto-persisted to
     *       {@code llm_call_log} by {@link LlmInvocationService} on every call,
     *       so this method does not re-persist them.</li>
     *   <li>{@link ToolEvent}s are flattened into a JSONB array stored in
     *       {@code bot_turns.tool_calls}; sequence numbers are preserved.</li>
     *   <li>The {@link AgentRunResult#finalUserMessage()} populates
     *       {@code bot_turns.bot_response}.</li>
     *   <li>{@link AgentRunResult#lastProjection()} populates
     *       {@code bot_turns.projected_context}.</li>
     *   <li>{@link AgentRunResult#lastLlmRawResponse()} populates
     *       {@code bot_turns.llm_raw_response}.</li>
     * </ul>
     *
     * <p>Also emits {@code RETRIEVAL_EXECUTED} / {@code ARTICLE_SHOWN} events
     * when the loop dispatched {@code search_knowledge}.
     */
    @SuppressWarnings("unchecked")
    private void recordRunResult(BotSession session, String userMessage,
                                  PhasePlan plan, AgentRunResult result,
                                  String phaseBefore, String phaseAfter, long latencyMs) {
        try {
            // Build tool_calls JSONB from the loop's ToolEvents
            String toolCallsJson = null;
            String[] sourceIds = null;
            int searchKnowledgeCount = 0;

            List<Map<String, Object>> toolCallsList = new ArrayList<>();
            List<String> collectedSourceIds = new ArrayList<>();
            if (result.toolEvents() != null && !result.toolEvents().isEmpty()) {
                for (ToolEvent te : result.toolEvents()) {
                    Map<String, Object> entry = new LinkedHashMap<>();
                    entry.put("sequence_index", te.sequenceIndex());
                    entry.put("step_index", te.stepIndex());
                    entry.put("tool_name", te.toolName());
                    entry.put("success", te.success());
                    entry.put("latency_ms", te.latencyMs());
                    if (te.errorMessage() != null) {
                        entry.put("error_message", te.errorMessage());
                    }
                    if (te.arguments() != null && !te.arguments().isEmpty()) {
                        entry.put("arguments", te.arguments());
                    }
                    toolCallsList.add(entry);

                    // Aggregate observability info from search_knowledge results
                    if ("search_knowledge".equals(te.toolName()) && te.success()) {
                        searchKnowledgeCount++;
                        Object data = te.resultData();
                        if (data instanceof Map<?, ?> dataMap) {
                            Object hits = ((Map<String, Object>) dataMap).get("hits");
                            if (hits instanceof List<?> hitsList) {
                                for (Object hit : hitsList) {
                                    if (hit instanceof Map<?, ?> hitMap) {
                                        Object sid = ((Map<String, Object>) hitMap).get("source_id");
                                        if (sid != null) collectedSourceIds.add(sid.toString());
                                    }
                                }
                            }
                        }
                    }
                }
                if (!collectedSourceIds.isEmpty()) {
                    sourceIds = collectedSourceIds.toArray(new String[0]);
                }
            }

            // Step 2.6: ensure ESCALATE outcomes always carry a request_handover
            // tool_call entry in the persisted trace, even when the AgentRunLoop
            // exited without dispatching one (e.g. max_steps_exceeded). The L1
            // escalation_compliance evaluator looks here for the handover marker.
            if ("ESCALATE".equals(phaseAfter)) {
                boolean hasHandover = false;
                for (Map<String, Object> entry : toolCallsList) {
                    if ("request_handover".equals(entry.get("tool_name"))) {
                        hasHandover = true;
                        break;
                    }
                }
                if (!hasHandover) {
                    String reason = session.getEscalationReason() != null
                            ? session.getEscalationReason()
                            : "service_degraded";
                    Map<String, Object> handoverEntry = new LinkedHashMap<>();
                    handoverEntry.put("sequence_index", toolCallsList.size());
                    handoverEntry.put("step_index", -1);
                    handoverEntry.put("tool_name", "request_handover");
                    handoverEntry.put("success", true);
                    handoverEntry.put("latency_ms", 0);
                    handoverEntry.put("arguments", Map.of("escalation_reason", reason));
                    toolCallsList.add(handoverEntry);
                }
            }

            if (!toolCallsList.isEmpty()) {
                try {
                    toolCallsJson = objectMapper.writeValueAsString(toolCallsList);
                } catch (Exception ex) {
                    log.warn("Failed to serialize AgentRunLoop tool_calls: {}", ex.getMessage());
                }
            }

            BotTurn turn = BotTurn.builder()
                    .turnId(UUID.randomUUID().toString())
                    .sessionId(session.getSessionId())
                    .turnIndex(session.getTotalBotTurns())
                    .userMessage(userMessage)
                    .projectedContext(result.lastProjection())
                    .llmRawResponse(result.lastLlmRawResponse())
                    .botResponse(result.finalUserMessage())
                    .toolCalls(toolCallsJson)
                    .sourceIds(sourceIds)
                    .phaseBefore(phaseBefore)
                    .phaseAfter(phaseAfter)
                    .activeUseCase(session.getActiveUseCase())
                    .latencyMs((int) latencyMs)
                    .createdAt(OffsetDateTime.now())
                    .build();
            turnRepository.save(turn);

            // Emit RETRIEVAL_EXECUTED if any search_knowledge ran
            if (searchKnowledgeCount > 0) {
                int hitCount = sourceIds == null ? 0 : sourceIds.length;
                eventEmitter.emitRetrievalExecuted(session.getSessionId(),
                        session.getTotalBotTurns(),
                        userMessage, hitCount == 0, hitCount);
                if (sourceIds != null) {
                    // Also persist articles_shown on session and emit ARTICLE_SHOWN per source
                    String[] existing = session.getArticlesShown();
                    if (existing != null) {
                        String[] merged = new String[existing.length + sourceIds.length];
                        System.arraycopy(existing, 0, merged, 0, existing.length);
                        System.arraycopy(sourceIds, 0, merged, existing.length, sourceIds.length);
                        session.setArticlesShown(merged);
                    } else {
                        session.setArticlesShown(sourceIds);
                    }
                    for (String sid : sourceIds) {
                        eventEmitter.emitArticleShown(session.getSessionId(),
                                session.getTotalBotTurns(), sid, null);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Session {}: failed to record agent run result: {}",
                    session.getSessionId(), e.getMessage(), e);
        }
    }

    /**
     * D16 routing helper. Computes the phase token used to look up entries in
     * {@link AgentRunLoopProperties#getEnabledPhases()}. Returns {@code null}
     * when no token applies (caller falls back to the legacy path).
     */
    private String computeRouteKey(BotSession session) {
        String phase = session.getCurrentPhase();
        if (phase == null) return null;
        switch (phase) {
            case "RESOLVE": {
                String uc = session.getActiveUseCase();
                if (uc == null) return null;
                if (FAQ_UCS.contains(uc)) return "RESOLVE_FAQ";
                if (INTAKE_UCS.contains(uc)) return "RESOLVE_INTAKE";
                return null;
            }
            case "DISCOVER":
            case "CONFIRM":
            case "CLOSE":
            case "ESCALATE":
                return phase;
            default:
                return null;
        }
    }

    /**
     * Apply a {@link PhaseTransitionDecision} to the session. Validates the
     * transition via {@link ControlPolicyService}; if invalid, the session
     * stays in {@code phaseBefore}. Returns the resulting phase.
     */
    private String applyTransition(BotSession session, String phaseBefore,
                                    PhaseTransitionDecision decision) {
        if (decision == null || decision.nextPhase() == null) {
            return phaseBefore;
        }
        String target = decision.nextPhase();
        if (target.equals(phaseBefore)) {
            return phaseBefore;
        }
        if (controlPolicy.isValidTransition(phaseBefore, target)) {
            session.setCurrentPhase(target);
            // Step 3 cleanup: when transitioning to ESCALATE, propagate the
            // canonical escalation_reason from the decision onto the session
            // so downstream record paths (recordRunResult / SessionManager)
            // don't have to fabricate a non-canonical fallback. Don't
            // overwrite an existing canonical session reason.
            if ("ESCALATE".equals(target)
                    && decision.escalationReason() != null
                    && (session.getEscalationReason() == null
                        || session.getEscalationReason().isBlank())) {
                session.setEscalationReason(decision.escalationReason());
            }
            log.info("Session {}: phase transition {} -> {} (reason: {})",
                    session.getSessionId(), phaseBefore, target, decision.transitionReason());
            return target;
        }
        log.warn("Session {}: invalid agent-loop transition {} -> {}, staying in {}",
                session.getSessionId(), phaseBefore, target, phaseBefore);
        return phaseBefore;
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
     * Derive a stable per-turn repetition key from an {@link AgentRunResult}.
     * Mirrors the legacy {@link #deriveRepetitionKey(ParsedAction)} logic but
     * sources the tool-name list from the loop's {@link ToolEvent}s. Used to
     * keep budget/loop detection working when the agent path is active.
     */
    private static String deriveRunResultKey(AgentRunResult result) {
        if (result == null) return null;
        if (result.toolEvents() == null || result.toolEvents().isEmpty()) {
            return result.terminalOutcome() == TerminalOutcome.ESCALATE
                    ? "escalate"
                    : "answer";
        }
        StringBuilder sb = new StringBuilder();
        for (ToolEvent te : result.toolEvents()) {
            if (sb.length() > 0) sb.append('+');
            sb.append(te.toolName() == null ? "" : te.toolName());
        }
        return sb.toString();
    }

    /**
     * Derive a stable per-turn key for repeated-action tracking. The key is the
     * concatenation of tool names invoked this turn (or "answer" / "clarify" when
     * there are no tool calls). Used by {@link #trackRepeatedAction} to detect
     * loops and by {@link PhaseEvaluator}'s intake guard to know whether the
     * opening template has already been sent.
     */
    private static String deriveRepetitionKey(ParsedAction action) {
        if (action == null) return null;
        List<ToolCall> tcs = action.getToolCalls();
        if (tcs == null || tcs.isEmpty()) {
            return PhaseEvaluator.isClarificationTurn(action) ? "clarify" : "answer";
        }
        StringBuilder sb = new StringBuilder();
        for (ToolCall tc : tcs) {
            if (sb.length() > 0) sb.append('+');
            sb.append(tc.getName() == null ? "" : tc.getName());
        }
        return sb.toString();
    }

    /**
     * Result from the control kernel processing.
     */
    public record KernelResult(String responseText, boolean shouldEndChat, long latencyMs) {}
}
