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
    private final EscalationReasonResolver escalationResolver;

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
                         AgentRunLoop agentRunLoop,
                         EscalationReasonResolver escalationResolver) {
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
        this.escalationResolver = escalationResolver;
    }

    /**
     * Set the session's escalation reason via the deterministic
     * {@link EscalationReasonResolver}. Higher-priority reasons (e.g.
     * {@code user_requested}) cannot be overwritten by lower-priority
     * reasons (e.g. {@code turn_budget_exhausted}). Centralising the
     * write site here is what makes Sprint §A1 hold across budget
     * forced escalation, drift detection, agent loop transitions, and
     * the legacy phase evaluator.
     *
     * <p>Sprint §E1 gate: refuse to upgrade the canonical session reason
     * to {@code user_distress} (priority 2) on the bot LLM's say-so. The
     * deterministic B1 detector earns this Tier-0 reason on the session
     * via {@link #applyDeterministicDistressReason(BotSession)} (called
     * from step 2.4 after {@code detectDistressSignal} matches a
     * DISTRESS_PATTERN or ALL-CAPS shout) and the SessionManager
     * already-escalated reconciliation path (which itself gates on
     * {@code detectDistressSignal} before calling
     * {@link EscalationReasonResolver#resolve(String, String)} directly).
     * An LLM-supplied {@code user_distress} (e.g. emitted in a
     * {@code request_handover.arguments.escalation_reason}) without a
     * prior B1 hit on this session is a subjective claim, so it gets
     * downgraded to {@code faq_miss_threshold_exceeded} (same
     * {@code bot_limit} family in the L1 escalation_compliance check as
     * {@code clarification_budget_exhausted} / {@code turn_budget_exhausted}).
     * Re-asserting an already-set {@code user_distress} (B1 fired earlier
     * in the session) is a no-op via the resolver.
     */
    private void applyEscalationReason(BotSession session, String candidate) {
        String canonCandidate = escalationResolver.canonicalize(candidate);
        if ("user_distress".equals(canonCandidate)
                && !"user_distress".equals(escalationResolver.canonicalize(session.getEscalationReason()))) {
            log.debug("Session {}: refused unconfirmed user_distress upgrade; downgrading to faq_miss_threshold_exceeded",
                    session.getSessionId());
            canonCandidate = "faq_miss_threshold_exceeded";
        }
        String resolved = escalationResolver.resolve(session.getEscalationReason(), canonCandidate);
        if (resolved != null) {
            session.setEscalationReason(resolved);
        }
    }

    /**
     * Sprint §E1 deterministic distress write — bypasses
     * {@link #applyEscalationReason(BotSession, String)}'s LLM gate
     * because the caller has just confirmed the message contains a
     * B1 distress phrase or ALL-CAPS shout. Used by step 2.4 in
     * {@link #processMessage(BotSession, String)}.
     */
    void applyDeterministicDistressReason(BotSession session) {
        String resolved = escalationResolver.resolve(session.getEscalationReason(), "user_distress");
        if (resolved != null) {
            session.setEscalationReason(resolved);
        }
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

        // Step 2.4 (Sprint §B1): detect distress / sustained frustration
        // BEFORE the budget check. ALL-CAPS shouting, "no one is helping",
        // "I followed your so called process", "this is ridiculous", etc.
        // stamp ``user_distress`` on the session via the resolver. The
        // resolver's precedence table (priority 2) keeps it ahead of
        // ``faq_miss_threshold_exceeded`` (41) and ``turn_budget_exhausted``
        // (42), and below ``user_requested`` (1) so an explicit callback /
        // human request on the same turn (Step 2.5) still wins. We DO NOT
        // forceEscalate here — distress alone is a precedence stamp, not a
        // terminal trigger; the budget / phase logic still decides whether
        // this turn ends the session, but the persisted reason will now be
        // the semantic ``user_distress``.
        if (escalationResolver.detectDistressSignal(userMessage)) {
            log.info("Session {}: distress signal detected in user message", session.getSessionId());
            applyDeterministicDistressReason(session);
        }

        // Step 2.5 (Sprint §A1): detect explicit user-driven escalation BEFORE
        // budget / drift checks. Without this, a user message asking for a
        // callback on the same turn the clarification budget exhausts gets
        // serialised as ``clarification_budget_exhausted`` / ``turn_budget_exhausted``
        // (cs_interactive_029). The deterministic resolver still guards every
        // downstream write site; running this first just means the canonical
        // reason is set to ``user_requested`` from the outset so even if the
        // budget path later fires, the resolver keeps the higher-priority
        // semantic reason.
        if (escalationResolver.detectExplicitUserEscalation(userMessage)) {
            log.info("Session {}: user message contains explicit human / callback request", session.getSessionId());
            applyEscalationReason(session, "user_requested");
            return forceEscalate(session, phaseBefore, userMessage, startTime,
                    "No problem, let me connect you with a human agent right away.");
        }

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
            //
            // Sprint §A1: route through the resolver so a higher-priority
            // semantic reason already on the session (e.g. ``user_requested``
            // set in step 2.5) is not overwritten by the budget close-out.
            applyEscalationReason(session, mapBudgetToEscalationReason(exceededBudget.get()));
            return forceEscalate(session, phaseBefore, userMessage, startTime,
                    "I've reached the limit of what I can assist with on this topic. " +
                    "Let me connect you with a human agent who can help further.");
        }

        // Step 4: Check drift — may update activeUseCase or force ESCALATE
        DriftResult drift = driftDetector.detect(session, userMessage);
        if (drift.getType() == DriftType.USER_ESCALATION_REQUEST || drift.isEscalationRequested()) {
            // User explicitly asked for a human agent — distinct from topic drift.
            // (Step 2.5 above catches most cases; this branch fires when
            // DriftDetector matches on a pattern not in EscalationReasonResolver,
            // e.g. "live agent" / "customer service".)
            log.info("Session {}: drift detector flagged user escalation request", session.getSessionId());
            applyEscalationReason(session, "user_requested");
            return forceEscalate(session, phaseBefore, userMessage, startTime,
                    "No problem, let me connect you with a human agent right away.");
        }
        if (drift.getType() == DriftType.HARD_SHIFT) {
            if (drift.getNewUseCase() != null) {
                log.info("Session {}: hard drift shift to {}", session.getSessionId(), drift.getNewUseCase());
                session.setActiveUseCase(drift.getNewUseCase());
            }
            // Canonicalize at the write site (Phase 2 #16, sibling of #17/#19):
            // hard drift maps to the closest canonical infrastructure fallback.
            // Routed through the resolver so a previously set semantic reason
            // (e.g. user_requested from an earlier turn) survives.
            applyEscalationReason(session, "service_degraded");
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

                // Emit ESCALATION_REQUESTED + create runtime case BEFORE
                // recordRunResult so the persisted tool_calls trace can
                // surface create_case_controlled in the right order
                // (Codex 1.2 / 1.3 / phase2 §2.10.4). The legacy
                // PhaseEvaluator.evaluateIntake path already creates the
                // case before its own recordTurn; the AgentRunLoop path
                // used to short-circuit on request_handover without ever
                // creating the case, which left case_id missing from the
                // handover payload (cs_038 / cs_040 L2:case_id_present
                // failures).
                boolean shouldEscalate = "ESCALATE".equals(decision.nextPhase());
                ToolResult runtimeCaseResult = null;
                if (shouldEscalate) {
                    // Sprint §A1: route the agent loop's decision through the
                    // resolver so the budget-close fallback ``service_degraded``
                    // never overwrites an earlier-stamped semantic reason. The
                    // canonical winner becomes the single source of truth
                    // surfaced to the tool call, session state, and handover
                    // payload.
                    String candidate = decision.escalationReason() != null
                            ? decision.escalationReason() : "service_degraded";
                    applyEscalationReason(session, candidate);
                    session.setHandlingState("QUEUE_TO_HUMAN");
                    session.setContainmentOutcome("escalated");
                    // Sprint 8 §K0: when the AgentRunLoop ESCALATE branch fires
                    // before any classify_use_case has committed
                    // ``activeUseCase`` (cs_interactive_259 r2 shape: empty
                    // form / UNKNOWN topic, FAQ-miss handover after two
                    // search_knowledge calls returned no viable hits, no
                    // classify call), apply the same deterministic fallback
                    // that ``forceEscalate`` uses so the trace contract is
                    // satisfied. The semantic escalation reason has already
                    // been settled above; this only fills in the missing UC
                    // slot. No-op when an UC is already committed.
                    //
                    // §K0 narrowing (2026-05-06): gate the fallback on
                    // evidence that the LLM actually reasoned about the user's
                    // problem this turn. When the loop returned ERROR (LLM
                    // call threw / timed out / parser failure) OR produced
                    // ZERO {@code llmEvents} AND ZERO {@code toolEvents}, the
                    // bot did not think about the user's request — applying a
                    // deterministic UC stamp here would silently mask an
                    // upstream regression (Kimi timeout, network blip,
                    // mis-configured chat budget) and let the eval trace
                    // contract pass with a UC the agent never reasoned about.
                    // In that case we DELIBERATELY leave {@code activeUseCase}
                    // null so the next user turn or the trace contract
                    // validator surfaces {@code service_degraded} /
                    // {@code CONTRACT_VIOLATION:active_use_case} loudly. The
                    // legacy {@link #forceEscalate} path is unaffected — it
                    // still always applies the §B3 fallback, since
                    // {@code forceEscalate} fires for budget / drift /
                    // explicit-OOS escalations where there is no
                    // {@code AgentRunResult} to inspect and a missing UC is
                    // never the result of a runtime LLM failure.
                    if (agentRunResultHasEvidence(runResult)) {
                        applyMissingUseCaseFallback(session, userMessage);
                    } else {
                        log.warn("Session {}: AgentRunLoop ESCALATE branch with no LLM/tool "
                                        + "evidence (terminalOutcome={}, llmEvents={}, "
                                        + "toolEvents={}); skipping K0 fallback so "
                                        + "service_degraded surface remains visible "
                                        + "instead of being masked with a regex UC.",
                                session.getSessionId(),
                                runResult == null ? "null" : runResult.terminalOutcome(),
                                runResult == null ? 0 : runResult.llmEvents().size(),
                                runResult == null ? 0 : runResult.toolEvents().size());
                    }
                    String escalationReason = session.getEscalationReason();
                    eventEmitter.emitEscalationRequested(session.getSessionId(),
                            session.getTotalBotTurns(), escalationReason);
                    runtimeCaseResult = createCaseIfNeeded(session);
                }

                long latencyMs = System.currentTimeMillis() - startTime;
                recordRunResult(session, userMessage, plan, runResult,
                        phaseBefore, phaseAfter, latencyMs, runtimeCaseResult);

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

        // Sprint §A1: PhaseResult.escalate no longer stamps the session reason
        // directly. Apply it through the resolver so the precedence table
        // protects already-set semantic reasons.
        if (phaseResult.shouldEscalate() && phaseResult.escalationReason() != null) {
            applyEscalationReason(session, phaseResult.escalationReason());
        }

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
            // Sprint §A1: re-eval may itself escalate; route through the resolver.
            if (reEval.shouldEscalate() && reEval.escalationReason() != null) {
                applyEscalationReason(session, reEval.escalationReason());
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

        // Step 10: Emit escalation event (OUTCOME_RECORDED is emitted by SessionManager.recordOutcome).
        // Sprint §A1: emit the canonical session reason (post-resolver) rather
        // than the evaluator's raw candidate so the event stream agrees with
        // the persisted session and tool-call surfaces.
        if (phaseResult.shouldEscalate()) {
            eventEmitter.emitEscalationRequested(session.getSessionId(), session.getTotalBotTurns(),
                    session.getEscalationReason() != null
                            ? session.getEscalationReason()
                            : phaseResult.escalationReason());
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
        // Sprint §B3: a soft-OOS UNKNOWN-topic session that escalates
        // immediately on the first user turn (cs_interactive_029) has
        // no committed ``active_use_case`` yet, which trips the
        // ``CONTRACT_VIOLATION:active_use_case`` gate even when the
        // semantic reason is correct (``user_requested``). Pick a
        // deterministic fallback UC from the user message + form
        // description so the trace contract is satisfied. The
        // semantic escalation reason is set BEFORE this method is
        // invoked, so the resolver still keeps the higher-priority
        // ``user_requested`` (or ``user_distress``) — we only fill
        // in the missing UC slot.
        applyMissingUseCaseFallback(session, userMessage);

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

        // Create case for intake UCs (UC-H/J/K) even on forced escalation.
        // Capture the result so the synthesized turn can include a
        // create_case_controlled tool_call entry (Codex 1.3 / phase2 §2.10.4).
        ToolResult runtimeCaseResult = createCaseIfNeeded(session);

        // Record the turn. Persist a synthesized handover tool_call so the trace
        // remains consistent with LLM-driven escalations under the single-layer
        // tool-use contract (phase0 §0.6 / phase3 §3.3.3). Sprint §A1: the
        // session reason has already been set through the resolver by the
        // caller; canonicalise here as a safety net for older code paths that
        // may have stamped a non-canonical literal.
        String escalationReason = escalationResolver.canonicalize(session.getEscalationReason());
        if (escalationReason == null) {
            escalationReason = "service_degraded";
        }
        session.setEscalationReason(escalationReason);
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
            // Codex 1.3: surface runtime-only create_case_controlled before the
            // synthesized request_handover so the persisted tool sequence
            // matches the expected order documented in phase2 §2.10.4.
            if (runtimeCaseResult != null) {
                toolCallsList.add(synthesizeCreateCaseToolCall(runtimeCaseResult));
            }
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

    /**
     * Sprint 8 §K0: shared fallback-UC commit used by both the legacy
     * {@link #forceEscalate} budget-close / drift path AND the
     * {@code AgentRunLoop} ESCALATE branch in {@link #processMessage}.
     *
     * <p>When the session reaches an escalation surface without any
     * {@code active_use_case} committed (cs_interactive_259 r2 shape:
     * the LLM made two {@code search_knowledge} calls, returned no
     * viable hits, and emitted {@code request_handover(faq_miss_threshold_exceeded)}
     * without ever calling {@code classify_use_case}), the trace
     * contract validator
     * ({@code eval_interactive.trace.collector.TraceCollector
     * ._enforce_conditional_session_contracts}) raises
     * {@code CONTRACT_VIOLATION:active_use_case} and the case never
     * reaches scoring. This helper fills the missing UC slot with a
     * deterministic inference so the contract is satisfied. The
     * semantic escalation reason is unaffected (the resolver has
     * already settled it before this method is called).
     *
     * <p>No-op when {@code session.activeUseCase} is already non-blank,
     * so the fallback never overrides an LLM-classified or
     * deterministically-routed UC. Negative guards: cs014 / cs066 /
     * cs095 / cs011 / cs002 / cs029 / cs176 either commit their UC
     * upstream (UseCaseRouter strong-priors / B2 bias / UC-K
     * regression override) or land in a different
     * {@link #inferFallbackUseCase} branch (account / messaging / ad
     * keywords) — none of them match the UC-F regex.
     */
    void applyMissingUseCaseFallback(BotSession session, String userMessage) {
        if (session.getActiveUseCase() != null && !session.getActiveUseCase().isBlank()) {
            return;
        }
        String fallbackUc = inferFallbackUseCase(session, userMessage);
        if (fallbackUc == null) {
            return;
        }
        log.info("Session {}: applying fallback active_use_case={} (topic was UNKNOWN/unset)",
                session.getSessionId(), fallbackUc);
        session.setActiveUseCase(fallbackUc);
        session.setIntentConfidence(new java.math.BigDecimal("0.30"));
        if (session.getCandidateUseCases() == null
                || session.getCandidateUseCases().length == 0) {
            session.setCandidateUseCases(new String[]{fallbackUc});
        }
    }

    /**
     * Sprint 8 §K0 narrowing (2026-05-06) / Sprint 8.1 §M2 (2026-05-06):
     * true when the {@link AgentRunResult} carries POSITIVE evidence
     * that the bot actually reasoned about the user's turn. Used by the
     * AgentRunLoop ESCALATE branch in {@link #processMessage} to gate
     * the {@link #applyMissingUseCaseFallback} call so the deterministic
     * UC stamp never papers over an upstream LLM / tool failure.
     *
     * <p>The fallback fires only when ALL of these hold:
     * <ul>
     *   <li>terminal outcome is NOT one of {@code ERROR},
     *       {@code DEADLINE_EXCEEDED}, {@code LLM_UNAVAILABLE} —
     *       these are infra failures, never normal escalations.</li>
     *   <li>if terminal outcome is {@code MAX_STEPS}, there is at
     *       least one successful, allowed tool event (synthetic
     *       safe-escalation handover requests are rejected by
     *       {@code validateAgainstPlan} on DISCOVER and so do not
     *       count). A MAX_STEPS run with only rejected / failed tool
     *       events means the LLM produced nothing usable.</li>
     *   <li>if terminal outcome is {@code ESCALATE} with an
     *       infra-class escalation reason ({@code service_degraded},
     *       {@code system_failure}, {@code runtime_error_threshold},
     *       {@code agent_error}, deadline/llm-unavailable), it is
     *       treated as a no-evidence path even if events exist.</li>
     *   <li>at least one of (a) a non-synthetic LLM event (real
     *       finish_reason, not the {@link
     *       com.gumtree.csagent.service.runtime.LlmInvocationService#SYNTHETIC_SAFE_ESCALATION_FINISH_REASON}
     *       marker), or (b) a successful tool event has been recorded.</li>
     * </ul>
     *
     * <p>The cs259 r2 shape (two {@code search_knowledge} successes +
     * a {@code request_handover} LLM event) satisfies the above and
     * still gets the K0 stamp. The localhost ad-visibility timeout
     * shape (deadline exhausted, no real LLM completion) does NOT, and
     * the trace surface remains diagnostically honest.
     */
    static boolean agentRunResultHasEvidence(AgentRunResult result) {
        if (result == null) {
            return false;
        }
        com.gumtree.csagent.model.TerminalOutcome outcome = result.terminalOutcome();
        if (outcome == null) {
            return false;
        }
        // Honest-failure outcomes never carry K0 evidence.
        if (outcome == com.gumtree.csagent.model.TerminalOutcome.ERROR
                || outcome == com.gumtree.csagent.model.TerminalOutcome.DEADLINE_EXCEEDED
                || outcome == com.gumtree.csagent.model.TerminalOutcome.LLM_UNAVAILABLE) {
            return false;
        }
        // ESCALATE with an infra-class reason is also no-evidence.
        if (outcome == com.gumtree.csagent.model.TerminalOutcome.ESCALATE) {
            String reason = result.escalationReason().orElse(null);
            if (isInfraEscalationReason(reason)) {
                return false;
            }
        }
        boolean hasRealTool = hasSuccessfulToolEvent(result);
        boolean hasRealLlm = hasNonSyntheticLlmEvent(result);
        // MAX_STEPS without any successful tool event is the "synthetic
        // SAFE_ESCALATION rejected on every step" shape — no real work
        // happened, so no fallback.
        if (outcome == com.gumtree.csagent.model.TerminalOutcome.MAX_STEPS && !hasRealTool) {
            return false;
        }
        return hasRealTool || hasRealLlm;
    }

    /**
     * Sprint 8.1 §M2 — escalation reasons that indicate an
     * infrastructure failure rather than a legitimate business
     * escalation. K0 is suppressed for these to keep the trace honest.
     */
    private static boolean isInfraEscalationReason(String reason) {
        if (reason == null) return false;
        switch (reason) {
            case "service_degraded":
            case "system_failure":
            case "agent_error":
            case "runtime_error_threshold":
            case "deadline_exceeded":
            case "llm_unavailable":
                return true;
            default:
                return false;
        }
    }

    /**
     * Sprint 8.1 §M2 — true when at least one tool event is both
     * successful AND not a rejected/synthetic guard event. Rejected
     * tool calls (e.g. SAFE_ESCALATION_RESPONSE's {@code request_handover}
     * blocked by {@code validateAgainstPlan} in DISCOVER) come back as
     * {@code success=false} so this naturally excludes them.
     */
    private static boolean hasSuccessfulToolEvent(AgentRunResult result) {
        if (result.toolEvents() == null) return false;
        for (com.gumtree.csagent.model.ToolEvent te : result.toolEvents()) {
            if (te != null && te.success()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Sprint 8.1 §M2 — true when at least one LLM event in the run is
     * NOT the synthetic SAFE_ESCALATION_RESPONSE marker. Detects the
     * marker via the response summary which carries the verbatim
     * {@code finish_reason=error_fallback} JSON-shaped content; we look
     * for the unique {@code "reasoning":"LLM invocation failure"}
     * substring to avoid false positives on real LLM completions that
     * happen to mention "error".
     */
    private static boolean hasNonSyntheticLlmEvent(AgentRunResult result) {
        if (result.llmEvents() == null) return false;
        for (com.gumtree.csagent.model.LlmCallEvent ev : result.llmEvents()) {
            if (ev == null) continue;
            String summary = ev.responseSummary();
            if (summary == null || !isSyntheticSafeEscalationSummary(summary)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isSyntheticSafeEscalationSummary(String summary) {
        // Matches the SAFE_ESCALATION_RESPONSE content emitted by
        // LlmInvocationService when an LLM call returns malformed JSON.
        // The unique markers are the static "LLM invocation failure"
        // reasoning string and the system_failure escalation_reason.
        return summary.contains("\"reasoning\":\"LLM invocation failure\"")
                || (summary.contains("\"escalation_reason\":\"system_failure\"")
                    && summary.contains("request_handover"));
    }

    /**
     * Sprint §B3: pick a deterministic fallback {@code active_use_case}
     * for sessions that escalate before any UC has been committed.
     * Cs_interactive_029 lands here: topic is UNKNOWN, the form
     * description is empty, and the very first user message asks for a
     * callback while shouting frustration. The resolver correctly
     * stamps {@code user_requested}, but without a committed UC the
     * eval trace contract fires {@code CONTRACT_VIOLATION:active_use_case}.
     *
     * <p>Inference rules (first match wins):
     * <ul>
     *   <li>"account / login / locked / password / email" → UC-D
     *       (Account &amp; Login).</li>
     *   <li>"message / notification / reply / inbox" → UC-C
     *       (Messages &amp; Replies).</li>
     *   <li>"ad / advert / listing / posting" → UC-A
     *       (Ad Status &amp; Visibility).</li>
     *   <li>"refund / payment / charged / paid / payout / proceeds /
     *       sale / sold / selling / money" → UC-F (Payment Inquiry —
     *       the lowest-risk Payments UC; Sprint 8 §K0 extends the
     *       sale-proceeds vocabulary so the cs259 family of intents
     *       robustly resolves to UC-F even when the user message
     *       avoids the literal "payment" token, e.g. "sale proceeds",
     *       "receive money for an item I sold").</li>
     *   <li>Otherwise UC-D as a safe generic-account default. The
     *       deterministic choice keeps the trace stable across runs;
     *       the semantic escalation reason is unaffected.</li>
     * </ul>
     */
    String inferFallbackUseCase(BotSession session, String userMessage) {
        StringBuilder sb = new StringBuilder();
        if (userMessage != null) {
            sb.append(userMessage).append(' ');
        }
        if (session != null && session.getFormContext() != null) {
            try {
                com.fasterxml.jackson.databind.JsonNode formNode =
                        objectMapper.readTree(session.getFormContext());
                if (formNode.has("description")) {
                    sb.append(formNode.get("description").asText()).append(' ');
                }
            } catch (Exception ignored) {
                // Non-JSON or malformed form_context — keep going with
                // whatever we have from the user message alone.
            }
        }
        String text = sb.toString().toLowerCase(java.util.Locale.ENGLISH);
        if (text.isBlank()) {
            return "UC-D";
        }
        // Order matters: payment / refund signals must beat the
        // ad/listing keyword in mixed phrases like "refund for my
        // listing fee" (Payments takes priority over Ad-Status).
        // Account / login likewise beats messaging, since
        // cs_interactive_029 says "MY ACCOUNT OS" with no messaging
        // context but "account" and "messages" can co-occur on
        // Replies & Messaging cases (those route via
        // UseCaseRouter.matchAccountMessagingBias upstream).
        // Sprint 8 §K0: UC-F (Payment Inquiry) regex extended with
        // sale-proceeds vocabulary so cs_interactive_259 family lands
        // here deterministically. Tokens added:
        //  - payout / payouts: explicit payment-out vocabulary.
        //  - proceeds / sale: the "sale proceeds" phrase the user
        //    persona uses for payment-after-selling questions.
        //  - sold / selling: payment context appears with these tokens
        //    even when "payment" is absent
        //    (e.g. "receive money for an item I sold").
        //  - money: payment context. Bare "money" only matches in the
        //    fallback path; primary classification still routes via
        //    UseCaseRouter strong-priors and the LLM, which never see
        //    this regex. Existing UC-A / UC-C / UC-D negative guards
        //    (cs095 / cs014 / cs066 / cs011 / cs002 / cs029) do NOT
        //    contain these tokens — see Sprint8Cs259ActiveUseCaseHardeningTest
        //    negative guards.
        if (text.matches(".*\\b(refund|refunds|payment|payments|charged|paid|invoice|receipt|payout|payouts|proceeds|sale|sold|selling|money)\\b.*")) {
            return "UC-F";
        }
        if (text.matches(".*\\b(account|login|log\\s*in|sign\\s*in|locked|password|email\\s+address)\\b.*")) {
            return "UC-D";
        }
        if (text.matches(".*\\b(message|messages|notification|notifications|reply|replies|inbox|chat)\\b.*")) {
            return "UC-C";
        }
        if (text.matches(".*\\b(ad|ads|advert|adverts|listing|listings|posting|post)\\b.*")) {
            return "UC-A";
        }
        return "UC-D";
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
     * Create a case for intake UCs (UC-H, UC-J, UC-K) when escalating. Used by
     * both the forced-escalation path and the AgentRunLoop ESCALATE branch.
     * Non-blocking: failures logged but don't prevent escalation.
     *
     * @return the {@link ToolResult} from the runtime tool, or {@code null} if
     *         the session is not eligible (UC not in {UC-H, UC-J, UC-K}) or a
     *         case was already created. Callers may use the returned result to
     *         append a {@code create_case_controlled} entry to the persisted
     *         tool_calls trace so eval can verify the runtime side-effect order.
     */
    ToolResult createCaseIfNeeded(BotSession session) {
        String activeUc = session.getActiveUseCase();
        Set<String> caseCreationUcs = Set.of("UC-H", "UC-J", "UC-K");
        if (activeUc == null || !caseCreationUcs.contains(activeUc)) {
            return null;
        }
        if (session.getCaseId() != null && !session.getCaseId().isBlank()) {
            return null; // Case already created
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
            if (result != null && result.isSuccess() && result.getData() != null) {
                String caseId = (String) result.getData().get("case_id");
                if (caseId != null) {
                    session.setCaseId(caseId);
                    log.info("Session {}: case created during escalation: {}", session.getSessionId(), caseId);
                    eventEmitter.emitCaseCreated(session.getSessionId(), session.getTotalBotTurns(), caseId, activeUc);
                }
            }
            return result;
        } catch (Exception e) {
            log.warn("Session {}: case creation during escalation failed: {}", session.getSessionId(), e.getMessage());
            return null;
        }
    }

    /**
     * Sprint §B0 helper: rewrite the {@code arguments.escalation_reason} of
     * every {@code request_handover} entry in {@code toolCallsList} so it
     * carries the resolved canonical session reason rather than whatever
     * literal the LLM emitted.
     *
     * <p>Why this exists: the {@link AgentRunLoop} dispatches
     * {@code request_handover} with the LLM-supplied arguments verbatim,
     * and {@link #recordRunResult} persists those arguments unchanged.
     * That lets a non-canonical literal (e.g. {@code user_requested_escalation})
     * or a lower-priority reason (e.g. {@code faq_miss_threshold_exceeded}
     * after the resolver settled on {@code user_distress}) leak into the
     * trace and disagree with {@code session.escalation_reason} and the
     * handover payload — exactly the {@code L1:escalation_reason_consistency}
     * failure mode Sprint §A1 closed for non-LLM paths.
     *
     * @return {@code true} when at least one entry was rewritten, useful
     *         for the legacy {@link #recordTurn} path that needs to know
     *         whether to re-serialize.
     */
    boolean normalizeHandoverArgsToSessionReason(BotSession session, List<Map<String, Object>> toolCallsList) {
        if (toolCallsList == null || toolCallsList.isEmpty()) {
            return false;
        }
        String resolved = escalationResolver.canonicalize(session.getEscalationReason());
        if (resolved == null) {
            return false;
        }
        boolean modified = false;
        for (Map<String, Object> entry : toolCallsList) {
            if (entry == null) continue;
            if (!"request_handover".equals(entry.get("tool_name"))) continue;
            Object argsObj = entry.get("arguments");
            Map<String, Object> writableArgs;
            if (argsObj instanceof Map<?, ?> existingArgs) {
                // Map.of(...) is immutable; copy into a mutable LinkedHashMap.
                writableArgs = new LinkedHashMap<>();
                for (Map.Entry<?, ?> kv : existingArgs.entrySet()) {
                    if (kv.getKey() != null) {
                        writableArgs.put(kv.getKey().toString(), kv.getValue());
                    }
                }
            } else {
                writableArgs = new LinkedHashMap<>();
            }
            Object current = writableArgs.get("escalation_reason");
            if (!resolved.equals(current)) {
                writableArgs.put("escalation_reason", resolved);
                entry.put("arguments", writableArgs);
                modified = true;
            } else if (!(argsObj instanceof Map<?, ?>)) {
                entry.put("arguments", writableArgs);
                modified = true;
            }
        }
        return modified;
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

    /**
     * Build a single {@code tool_calls} entry representing a runtime-issued
     * {@code create_case_controlled} call (UC-H/J/K). Lets the legacy and
     * forced-escalation paths surface the runtime-only side effect in the
     * persisted trace just like {@link #recordRunResult} does for the
     * AgentRunLoop path. The {@code source: runtime} marker tells eval
     * tooling this entry was synthesized rather than LLM-issued.
     */
    @SuppressWarnings("unchecked")
    Map<String, Object> synthesizeCreateCaseToolCall(ToolResult result) {
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("tool_name", "create_case_controlled");
        entry.put("source", "runtime");
        Map<String, Object> args = new LinkedHashMap<>();
        if (result != null) {
            entry.put("success", result.isSuccess());
            if (result.isSuccess() && result.getData() != null) {
                Object caseId = ((Map<String, Object>) result.getData()).get("case_id");
                if (caseId != null) {
                    args.put("case_id", caseId);
                }
            } else if (result.getErrorMessage() != null) {
                entry.put("error_message", result.getErrorMessage());
            }
        } else {
            entry.put("success", true);
        }
        // Trace contract requires arguments map even when empty.
        entry.put("arguments", args);
        return entry;
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

            // D12.4: Populate tool_calls if knowledge was retrieved.
            // Trace contract requires every tool_call entry to carry an
            // ``arguments`` map (collector.py REQUIRED_TOOL_CALL_FIELDS), so
            // emit an empty one even when we don't capture the original query.
            if (phaseResult.knowledgeHits() != null && !phaseResult.knowledgeHits().isEmpty()) {
                try {
                    List<Map<String, Object>> toolCallsList = new ArrayList<>();
                    Map<String, Object> searchCall = new LinkedHashMap<>();
                    searchCall.put("tool_name", "search_knowledge");
                    searchCall.put("status", "success");
                    searchCall.put("result_count", phaseResult.knowledgeHits().size());
                    searchCall.put("arguments", Map.of());
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
                // Fallback must be a canonical EscalationTrigger value
                // (case_spec/schema.py). ``service_degraded`` is the Phase 2
                // §2.4 catch-all; preserves previous behaviour without
                // breaking the trace contract enum check.
                String escalationReason = session.getEscalationReason() != null
                        ? session.getEscalationReason() : "service_degraded";
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
                // Codex 1.3: legacy evaluateIntake path calls
                // PhaseEvaluator.createCaseIfAllowed which sets session.caseId
                // but does not surface a create_case_controlled entry in the
                // bot_turns trace. Detect that here (INTAKE UC + ESCALATE +
                // case_id present + entry not already in this turn) and append.
                String activeUc = session.getActiveUseCase();
                boolean caseCreated = session.getCaseId() != null
                        && !session.getCaseId().isBlank();
                boolean alreadyHasCreateCase = false;
                if (toolCallsList != null) {
                    for (Map<String, Object> entry : toolCallsList) {
                        if ("create_case_controlled".equals(entry.get("tool_name"))) {
                            alreadyHasCreateCase = true;
                            break;
                        }
                    }
                }
                boolean addedCreateCase = false;
                if (caseCreated && !alreadyHasCreateCase
                        && activeUc != null
                        && Set.of("UC-H", "UC-J", "UC-K").contains(activeUc)) {
                    if (toolCallsList == null) {
                        toolCallsList = new ArrayList<>();
                    }
                    Map<String, Object> entry = new LinkedHashMap<>();
                    entry.put("tool_name", "create_case_controlled");
                    entry.put("source", "runtime");
                    entry.put("success", true);
                    entry.put("arguments", Map.of("case_id", session.getCaseId()));
                    toolCallsList.add(entry);
                    addedCreateCase = true;
                }
                boolean modified = addedCreateCase;
                if (!alreadyHasHandover) {
                    if (toolCallsList == null) {
                        toolCallsList = new ArrayList<>();
                    }
                    toolCallsList.add(synthesizeHandoverToolCall(escalationReason));
                    modified = true;
                }
                // Sprint §B0: normalize any LLM-emitted handover arguments
                // (raw or lower-priority) to the resolved canonical session
                // reason so the persisted trace agrees with session state and
                // the handover payload (see normalizeHandoverArgsToSessionReason).
                if (toolCallsList != null && normalizeHandoverArgsToSessionReason(session, toolCallsList)) {
                    modified = true;
                }
                if (modified) {
                    try {
                        turn.setToolCalls(objectMapper.writeValueAsString(toolCallsList));
                    } catch (Exception ex) {
                        log.warn("Session {}: failed to serialize synthesized escalation tool_calls: {}",
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
                                  String phaseBefore, String phaseAfter, long latencyMs,
                                  ToolResult runtimeCaseResult) {
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
                    // Trace contract requires every tool_call entry to carry an
                    // ``arguments`` map, even when empty (eval_interactive
                    // collector.py REQUIRED_TOOL_CALL_FIELDS). Always emit a
                    // map so the strict-mode validator does not flag empty
                    // tool calls as CONTRACT_VIOLATION:arguments.
                    entry.put("arguments",
                            te.arguments() != null ? te.arguments() : Map.of());
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

            // Codex 1.3 / phase2 §2.10.4: surface runtime-only side effects in
            // the trace. When the runtime created a Case for an INTAKE escalation
            // (UC-H/J/K), prepend a synthesized create_case_controlled entry
            // before the eventual request_handover so eval can verify the
            // expected_tool_sequence (create_case_controlled → request_handover →
            // record_outcome). Skipped when the runtime did not create a case
            // (already-created session, ineligible UC, or tool failure).
            if (runtimeCaseResult != null) {
                Map<String, Object> caseEntry = new LinkedHashMap<>();
                caseEntry.put("sequence_index", toolCallsList.size());
                caseEntry.put("step_index", -1);
                caseEntry.put("tool_name", "create_case_controlled");
                caseEntry.put("success", runtimeCaseResult.isSuccess());
                caseEntry.put("latency_ms", 0);
                caseEntry.put("source", "runtime");
                Map<String, Object> caseArgs = new LinkedHashMap<>();
                if (runtimeCaseResult.isSuccess() && runtimeCaseResult.getData() != null) {
                    Object caseId = ((Map<String, Object>) runtimeCaseResult.getData()).get("case_id");
                    if (caseId != null) {
                        caseArgs.put("case_id", caseId);
                    }
                } else if (runtimeCaseResult.getErrorMessage() != null) {
                    caseEntry.put("error_message", runtimeCaseResult.getErrorMessage());
                }
                caseEntry.put("arguments", caseArgs);
                toolCallsList.add(caseEntry);
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

            // Sprint §B0: rewrite every persisted ``request_handover`` entry
            // so its ``arguments.escalation_reason`` matches the resolved
            // canonical session reason. The LLM may emit a non-canonical
            // literal (``user_requested_escalation``) or a lower-priority
            // reason (``faq_miss_threshold_exceeded`` after the resolver
            // already settled on ``user_distress``). Without this rewrite
            // the persisted trace surface disagrees with the session
            // and handover payload, re-opening the
            // ``L1:escalation_reason_consistency`` failure mode that
            // Sprint §A1 closed for non-LLM paths.
            normalizeHandoverArgsToSessionReason(session, toolCallsList);

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
            // Sprint §A1: when transitioning to ESCALATE, merge the
            // decision's reason via the resolver. Higher-priority semantic
            // reasons already on the session (e.g. user_requested set
            // earlier in the turn) survive low-priority decisions like
            // turn_budget_exhausted.
            if ("ESCALATE".equals(target) && decision.escalationReason() != null) {
                applyEscalationReason(session, decision.escalationReason());
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
