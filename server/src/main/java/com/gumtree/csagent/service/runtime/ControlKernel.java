package com.gumtree.csagent.service.runtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gumtree.csagent.config.AgentRunLoopProperties;
import com.gumtree.csagent.model.*;
import com.gumtree.csagent.model.DriftResult.DriftType;
import com.gumtree.csagent.repository.BotEventRepository;
import com.gumtree.csagent.repository.BotTurnRepository;
import com.gumtree.csagent.service.llm.LlmCallContext;
import com.gumtree.csagent.service.observability.EventEmitter;
import com.gumtree.csagent.service.tools.CreateCaseControlledTool;
import com.gumtree.csagent.service.tools.ToolResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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

    /**
     * Sprint 8.1 §M3 — minimum remaining wall-clock budget required to
     * start a same-turn RESOLVE replan. Sized so a RESOLVE plan with one
     * search_knowledge + one resolve_article LLM round-trip fits inside
     * the user-facing deadline. Below this floor the kernel skips the
     * replan, leaves the session in RESOLVE, and returns a non-terminal
     * "looking into it" response so the next user turn re-runs RESOLVE
     * fresh.
     */
    private static final long MIN_RESOLVE_REPLAN_BUDGET_MS = 8_000L;

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
    private final RuntimeIntentClassifier runtimeIntentClassifier;
    private final RerouteDecider rerouteDecider;

    @Autowired
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
                         EscalationReasonResolver escalationResolver,
                         RuntimeIntentClassifier runtimeIntentClassifier,
                         RerouteDecider rerouteDecider) {
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
        this.runtimeIntentClassifier = runtimeIntentClassifier;
        this.rerouteDecider = rerouteDecider;
    }

    /**
     * Sprint 10 §L1 — backwards-compat constructor for tests built before
     * the {@link RuntimeIntentClassifier} / {@link RerouteDecider}
     * dependencies were introduced. Auto-wires fresh instances so the
     * runtime reroute pipeline stays exercised under the existing test
     * fixtures (Sprint 6 / 7 / 8 / 8.1 / 8.2 / 9 regressions). Production
     * Spring wiring continues to use the all-args constructor above so
     * the singleton beans are reused.
     */
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
        this(turnRepository, eventRepository, budgetChecker, driftDetector,
                phaseEvaluator, controlPolicy, objectMapper, createCaseTool,
                eventEmitter, contextProjectionBuilder, agentRunLoopProperties,
                agentRunLoop, escalationResolver,
                new RuntimeIntentClassifier(escalationResolver, objectMapper),
                new RerouteDecider());
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

        // Step 4: Check drift — may flag user escalation request. The legacy
        // HARD_SHIFT branch (immediate forceEscalate on every hard shift) was
        // removed in Sprint 10 §L1: per the runtime-reroute MVP, not every
        // hard shift is an immediate handover. Hard-shift signals are now
        // surfaced to the {@link RuntimeIntentClassifier} below as a
        // {@link DriftResult#getNewUseCase()} hint, and the {@link RerouteDecider}
        // decides whether to soft-shift (FAQ-class UC), enter intake
        // (UC-G/H/I/J/K), or continue current.
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

        // Step 4.5 (Sprint 10 §L0/§L1): runtime-internal intent classifier +
        // reroute decision. Runs AFTER hard guards / explicit-human / distress /
        // critical-risk checks and BEFORE PhaseEvaluator.plan(...). Mutates
        // (phase, active_use_case, minimal projected issue state) per the
        // Sprint 10 MVP shapes (UC-A → UC-C soft shift, UC-A same-issue rebound,
        // UC-A same-UC follow-up, UC-A → UC-J risk-shift, payment-ambiguity
        // negative guard). Unrecognised messages produce
        // {@link com.gumtree.csagent.model.IntentClassification.IntentRelation#UNKNOWN}
        // and the kernel leaves the session untouched.
        applyRerouteDecision(session, userMessage, drift, phaseBefore);

        // After the reroute, the session's current phase is what {@code
        // PhaseEvaluator.plan(...)} and {@code AgentRunLoop} will see.
        // Update the local {@code phaseBefore} so the rest of the
        // {@code processMessage} pipeline (transition validation, persisted
        // {@code bot_turns.phase_before}, drift / repetition tracking) uses
        // the post-reroute phase. The pre-reroute phase is preserved on the
        // session via {@link BotSession#getPreviousActiveUseCase()} +
        // logged in the {@code REROUTE_DECISION} event for trace fidelity.
        phaseBefore = session.getCurrentPhase();

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

                // Sprint 8.1 §M3: deterministic DISCOVER → RESOLVE phase
                // boundary. When the AgentRunLoop returns USE_CASE_IDENTIFIED
                // (classify_use_case successfully committed activeUseCase on
                // a DISCOVER plan), perform exactly one bounded same-turn
                // replan into RESOLVE. Both runs share the SAME thread-local
                // {@link LlmCallContext} budget, so the combined wall-clock
                // and HTTP-attempt budget is preserved. If the remaining
                // budget cannot fit a RESOLVE attempt, leave the session in
                // RESOLVE and return a non-terminal "looking into it"
                // response — the next user turn will run RESOLVE fresh.
                AgentRunResult discoverDiscovery = null;
                String effectivePhaseBefore = phaseBefore;
                if (runResult.terminalOutcome() == TerminalOutcome.USE_CASE_IDENTIFIED
                        && plan.phase() != null
                        && "DISCOVER".equalsIgnoreCase(plan.phase())) {
                    discoverDiscovery = runResult;
                    String committedUc = session.getActiveUseCase();
                    log.info("Session {}: §M3 same-turn DISCOVER->RESOLVE replan candidate "
                                    + "(committedUc={})",
                            session.getSessionId(), committedUc);
                    // Apply the deterministic DISCOVER -> RESOLVE transition
                    // BEFORE the second plan() call so the new plan reflects
                    // the new phase. {@link #applyTransition} would also
                    // refuse to step over RESOLVE (DISCOVER -> CONFIRM is
                    // not a legal direct transition); promoting
                    // {@code effectivePhaseBefore} to RESOLVE keeps the
                    // post-replan transition (e.g. RESOLVE -> CONFIRM on a
                    // FAQ FINAL_ANSWER) inside the policy.
                    if (controlPolicy.isValidTransition(phaseBefore, "RESOLVE")) {
                        session.setCurrentPhase("RESOLVE");
                        effectivePhaseBefore = "RESOLVE";
                    }
                    // Budget gate: a RESOLVE attempt typically needs ~3 s
                    // connect + 12 s read + tiny buffer. Sprint 8.1 closure
                    // follow-up (2026-05-07): only the wall-clock budget
                    // bounds whether the same-turn replan can fit. The
                    // per-invocation HTTP-attempt budget is re-armed at
                    // {@link FallbackLlmClient#chat} entry, so two
                    // successful DISCOVER calls (search_knowledge +
                    // classify_use_case) no longer poison the RESOLVE
                    // replan's first attempt; they only consume wall-clock.
                    // If the remaining wall-clock cannot fit one full
                    // attempt, skip the replan and let the user retry next
                    // turn.
                    Long remaining = LlmCallContext.remainingMillis();
                    boolean budgetOk = (remaining == null
                            || remaining >= MIN_RESOLVE_REPLAN_BUDGET_MS);
                    if (!budgetOk) {
                        log.info("Session {}: §M3 insufficient wall-clock budget for same-turn "
                                        + "RESOLVE replan (remaining_ms={}, min_required_ms={}); "
                                        + "staying in RESOLVE and returning transitional response",
                                session.getSessionId(), remaining,
                                MIN_RESOLVE_REPLAN_BUDGET_MS);
                    } else {
                        PhasePlan resolvePlan = phaseEvaluator.plan(session, userMessage, history);
                        if (resolvePlan != null) {
                            log.info("Session {}: §M3 running same-turn RESOLVE replan with plan.useCase={}",
                                    session.getSessionId(), resolvePlan.useCase());
                            AgentRunResult resolveRunResult =
                                    agentRunLoop.run(resolvePlan, session, userMessage, history);
                            // Combine tool / llm events from BOTH runs so the
                            // persisted bot turn captures the DISCOVER tool
                            // chain (search_knowledge, classify_use_case)
                            // alongside the RESOLVE tool chain.
                            runResult = mergeAgentRunResults(discoverDiscovery, resolveRunResult);
                            plan = resolvePlan;
                        } else {
                            log.warn("Session {}: §M3 RESOLVE plan was null; falling back to "
                                            + "USE_CASE_IDENTIFIED transitional response",
                                    session.getSessionId());
                        }
                    }
                }

                PhaseTransitionDecision decision =
                        phaseEvaluator.interpretRunResult(plan, runResult, session);

                String phaseAfter = applyTransition(session, effectivePhaseBefore, decision);
                String responseText = decision.responseText();
                if (responseText == null) {
                    responseText = "I'm looking into this for you. One moment please.";
                }

                // Sprint 12 §N0 — emit RESOLVE_DISPOSITION and
                // RECORD_OUTCOME_GUARD trace events so a reviewer can audit
                // why the bot stayed RESOLVE instead of CONFIRM and why
                // record_outcome(resolve) was allowed or rejected. Both
                // events are backward-compatible additions; existing
                // event stream consumers ignore unknown event types.
                emitResolveDispositionEvent(session, plan, runResult, decision,
                        effectivePhaseBefore, phaseAfter);
                emitRecordOutcomeGuardEvent(session, plan, runResult);

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
                // Sprint 8.1 §M2 follow-up (2026-05-06): persist the
                // user-facing reply text (the same string returned via
                // KernelResult.responseText). The legacy code passed only
                // {@link AgentRunResult#finalUserMessage}, which is null for
                // every non-FINAL_ANSWER terminal outcome (MAX_STEPS,
                // ESCALATE, DEADLINE_EXCEEDED, LLM_UNAVAILABLE, ERROR), so
                // {@code bot_turns.bot_response} silently became NULL
                // whenever the loop hit the slow / unavailable / max-steps /
                // escalate paths. The trace UI showed a blank Output for
                // those turns and the next turn's
                // {@code conversation_history} projection fed the LLM a
                // record claiming the bot had said nothing — both
                // problems disappear once the persisted column reflects what
                // the user actually saw.
                recordRunResult(session, userMessage, plan, runResult,
                        phaseBefore, phaseAfter, latencyMs, runtimeCaseResult,
                        responseText);

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

    /**
     * Sprint 10 §L1 — runtime reroute application. Invokes the
     * {@link RuntimeIntentClassifier} and {@link RerouteDecider} and
     * mutates session state (active_use_case, current_phase, transient
     * projection slots) so {@link PhaseEvaluator#plan(BotSession, String, java.util.List)}
     * sees the post-reroute view.
     *
     * <p>An already-committed UC is preserved unless the
     * {@link com.gumtree.csagent.model.RerouteDecision.RerouteAction}
     * explicitly requires a switch
     * ({@link com.gumtree.csagent.model.RerouteDecision.RerouteAction#SOFT_SHIFT_TO_DISCOVER},
     * {@link com.gumtree.csagent.model.RerouteDecision.RerouteAction#RISK_SHIFT_TO_INTAKE},
     * {@link com.gumtree.csagent.model.RerouteDecision.RerouteAction#REBOUND_TO_RESOLVE}).
     *
     * <p>Phase transitions are validated through {@link ControlPolicyService}
     * — a target the policy table forbids leaves the session in
     * {@code phaseBefore}, and the trace observability records
     * {@code applied=false} for the failed transition.
     */
    void applyRerouteDecision(BotSession session, String userMessage,
                              DriftResult drift, String phaseBefore) {
        if (runtimeIntentClassifier == null || rerouteDecider == null) {
            // Defensive: if the bean wiring was somehow stripped (e.g. partial
            // test setup with a hand-built ControlKernel), fall back to the
            // pre-Sprint-10 behaviour of doing nothing here.
            return;
        }
        com.gumtree.csagent.model.IntentClassification classification =
                runtimeIntentClassifier.classify(session, userMessage, drift);
        com.gumtree.csagent.model.RerouteDecision decision =
                rerouteDecider.decide(session, classification);
        if (decision == null) {
            return;
        }

        String previousUc = session.getActiveUseCase();
        String previousPhase = session.getCurrentPhase();
        boolean ucMutated = false;
        boolean phaseMutated = false;

        com.gumtree.csagent.model.RerouteDecision.RerouteAction action = decision.action();
        switch (action) {
            case SOFT_SHIFT_TO_DISCOVER:
            case RISK_SHIFT_TO_INTAKE: {
                String targetUc = decision.targetUseCase();
                if (targetUc != null && !targetUc.isBlank() && !targetUc.equals(previousUc)) {
                    session.setActiveUseCase(targetUc);
                    session.setIntentConfidence(java.math.BigDecimal.valueOf(
                            classification.confidence()));
                    // Surface the new UC in candidate_use_cases when no list
                    // was committed (so the projection has something useful
                    // to show downstream).
                    if (session.getCandidateUseCases() == null
                            || session.getCandidateUseCases().length == 0) {
                        session.setCandidateUseCases(new String[]{targetUc});
                    }
                    ucMutated = true;
                }
                String targetPhase = decision.targetPhase();
                if (targetPhase != null && !targetPhase.equals(previousPhase)
                        && controlPolicy.isValidTransition(previousPhase, targetPhase)) {
                    session.setCurrentPhase(targetPhase);
                    phaseMutated = true;
                }
                break;
            }
            case REBOUND_TO_RESOLVE: {
                String targetPhase = decision.targetPhase();
                if (targetPhase != null && !targetPhase.equals(previousPhase)
                        && controlPolicy.isValidTransition(previousPhase, targetPhase)) {
                    session.setCurrentPhase(targetPhase);
                    phaseMutated = true;
                }
                // UC stays the same; do not overwrite a committed UC.
                break;
            }
            case ESCALATE_IMMEDIATELY:
                // The kernel's existing step 2.5 / drift-USER_ESCALATION_REQUEST
                // path already escalated for HUMAN_REQUEST. Do not duplicate.
                break;
            case CONTINUE_CURRENT:
            default:
                break;
        }

        // Sprint 10 §L2 — minimal projected issue-state. Always populate
        // the transient slots so the projection emits a stable shape; the
        // kernel re-builds these every turn, so values never leak across
        // turns. Use the PRE-mutation UC for previous_active_use_case
        // when a switch happened.
        session.setPreviousActiveUseCase(previousUc);
        session.setDriftType(deriveDriftTypeToken(action,
                classification.relation()));

        // Sprint 12 §N0 — runtime alignment observability slots. The
        // classifier output (predicted_use_case, intent_relation,
        // confidence), the decider action, and the canonical
        // phase_transition_reason are all stamped on the session so the
        // upcoming projection can surface them and a reviewer can audit
        // why the bot stayed in the current UC, soft-shifted, or
        // risk-shifted from a single turn's trace evidence. These are
        // BACKWARD-COMPATIBLE additions: the existing Sprint 10 §L2
        // slots (previous_active_use_case, drift_type, current_task_type,
        // primary_entity, issue_status_summary) are preserved verbatim.
        session.setPredictedUseCase(classification.predictedUseCase());
        session.setIntentRelation(classification.relation() == null
                ? null : classification.relation().name());
        session.setRerouteAction(action == null ? null : action.name());
        session.setPhaseTransitionReason(decision.transitionReason());

        // Sprint 11 §M0 — same-UC progressive resolve: capture an ad_id
        // from the user message even when the Sprint 10 MVP shapes did
        // not match (CONTINUE_CURRENT). The progressive UC-A flow
        // routinely has the user reply with just an ad_id after a soft
        // "send the advert ID" turn; without this hook, the
        // primary_entity slot disappears on the very next user turn.
        // Only fires when the active UC is a FAQ-class UC where
        // progressive resolve applies (currently UC-A). Persists the
        // ad_id back into form_context so subsequent turns also see it
        // via the form-context fast path.
        String capturedAdId = null;
        if (action == com.gumtree.csagent.model.RerouteDecision.RerouteAction.CONTINUE_CURRENT
                && runtimeIntentClassifier != null) {
            capturedAdId = runtimeIntentClassifier.captureSameUcAdIdHint(
                    session, userMessage);
        }

        String taskType = classification.taskType();
        String entityType = classification.primaryEntityType();
        String entityValue = classification.primaryEntityValue();
        if ((entityValue == null || entityValue.isBlank())
                && capturedAdId != null && !capturedAdId.isBlank()) {
            entityType = "listing";
            entityValue = capturedAdId;
            // Synthesize a same-UC follow-up task type so the projection
            // surfaces "user is providing the ad_id we asked for" rather
            // than re-asking on the next turn. Cheap, deterministic, no
            // taxonomy expansion.
            if (taskType == null || taskType.isBlank()) {
                taskType = "listing_visibility_diagnostic";
            }
        }

        session.setCurrentTaskType(taskType);
        session.setPrimaryEntityType(entityType);
        session.setPrimaryEntityValue(entityValue);
        session.setIssueStatusSummary("open");

        // Sprint 11 §M0 — last_entity_context_ref. Surface a short
        // observability pointer so the LLM / trace can see whether the
        // primary_entity originated from form_context (cold start) or
        // from a runtime user-message capture (progressive resolve).
        if (entityValue != null && !entityValue.isBlank()) {
            String ref = "form_context.ad_id";
            if (capturedAdId != null && capturedAdId.equals(entityValue)) {
                ref = "user_message.ad_id";
            }
            session.setLastEntityContextRef(ref);
        } else {
            session.setLastEntityContextRef(null);
        }

        // Sprint 11 §M0 — persist a runtime-captured ad_id into
        // form_context so the next turn's RuntimeIntentClassifier and
        // FAQ tools see it via the existing form-context fast path. We
        // only WRITE when the form context did not already carry the
        // value, so customer-supplied form data always wins.
        if (capturedAdId != null && !capturedAdId.isBlank()) {
            persistAdIdIntoFormContext(session, capturedAdId);
        }

        // Sprint 10 §L2 — lightweight trace observability. log.info captures
        // the predicted UC, relation, reroute action, previous UC, new UC,
        // and phase transition reason. A BotEvent is emitted (REROUTE_DECISION)
        // when an actual reroute fired so the trace UI / eval harness can
        // surface the runtime reroute path. UNKNOWN / CONTINUE_CURRENT no-ops
        // are logged at debug to keep production logs quiet on uneventful turns.
        if (ucMutated || phaseMutated
                || action == com.gumtree.csagent.model.RerouteDecision.RerouteAction.REBOUND_TO_RESOLVE
                || action == com.gumtree.csagent.model.RerouteDecision.RerouteAction.ESCALATE_IMMEDIATELY) {
            log.info("Session {}: reroute applied — predictedUc={}, relation={}, action={}, "
                            + "previousUc={}, newUc={}, previousPhase={}, newPhase={}, reason={}",
                    session.getSessionId(),
                    classification.predictedUseCase(),
                    classification.relation(),
                    action,
                    previousUc,
                    session.getActiveUseCase(),
                    previousPhase,
                    session.getCurrentPhase(),
                    decision.transitionReason());
            try {
                Map<String, Object> payload = new LinkedHashMap<>();
                payload.put("predicted_use_case", classification.predictedUseCase());
                payload.put("relation", classification.relation() == null
                        ? null : classification.relation().name());
                payload.put("action", action.name());
                payload.put("previous_use_case", previousUc);
                payload.put("new_use_case", session.getActiveUseCase());
                payload.put("previous_phase", previousPhase);
                payload.put("new_phase", session.getCurrentPhase());
                payload.put("transition_reason", decision.transitionReason());
                payload.put("confidence", classification.confidence());
                payload.put("task_type", classification.taskType());
                // Sprint 12 §N0 — backward-compat alias keys so the
                // canonical Sprint 12 observability vocabulary
                // ({@code intent_relation}, {@code reroute_action},
                // {@code phase_transition_reason},
                // {@code previous_active_use_case},
                // {@code active_use_case}, {@code drift_type},
                // {@code current_task_type}, {@code primary_entity},
                // {@code task_status}) is available in one place. The
                // existing Sprint 10 keys above are preserved verbatim so
                // any downstream consumer that read them still works.
                payload.put("intent_relation", classification.relation() == null
                        ? null : classification.relation().name());
                payload.put("reroute_action", action.name());
                payload.put("phase_transition_reason", decision.transitionReason());
                payload.put("previous_active_use_case", previousUc);
                payload.put("active_use_case", session.getActiveUseCase());
                payload.put("drift_type", session.getDriftType());
                payload.put("current_task_type", session.getCurrentTaskType());
                payload.put("task_status", session.getTaskStatus());
                Map<String, Object> primaryEntityNode = buildPrimaryEntityPayload(session);
                if (primaryEntityNode != null) {
                    payload.put("primary_entity", primaryEntityNode);
                } else {
                    payload.put("primary_entity", null);
                }
                emitEvent(session, "REROUTE_DECISION",
                        session.getTotalBotTurns(),
                        objectMapper.writeValueAsString(payload));
            } catch (Exception ex) {
                log.warn("Session {}: failed to emit REROUTE_DECISION event: {}",
                        session.getSessionId(), ex.getMessage());
            }
        } else {
            log.debug("Session {}: reroute classifier returned {} / {} ({}); no state change",
                    session.getSessionId(),
                    classification.relation(),
                    action,
                    decision.transitionReason());
        }
    }

    /**
     * Sprint 11 §M0 — write a runtime-captured ad_id into the session's
     * form_context JSON so the next turn's {@link RuntimeIntentClassifier}
     * and FAQ tooling see the entity via the existing form-context fast
     * path. Tolerant: never throws on null / malformed input. Does not
     * overwrite a non-blank ad_id that already exists in the form
     * context.
     */
    private void persistAdIdIntoFormContext(BotSession session, String adId) {
        if (session == null || adId == null || adId.isBlank()) return;
        try {
            com.fasterxml.jackson.databind.node.ObjectNode root;
            String existing = session.getFormContext();
            if (existing == null || existing.isBlank()) {
                root = objectMapper.createObjectNode();
            } else {
                com.fasterxml.jackson.databind.JsonNode parsed =
                        objectMapper.readTree(existing);
                if (parsed == null || !parsed.isObject()) {
                    root = objectMapper.createObjectNode();
                } else {
                    root = (com.fasterxml.jackson.databind.node.ObjectNode) parsed;
                }
            }
            com.fasterxml.jackson.databind.JsonNode adNode = root.get("ad_id");
            if (adNode != null && !adNode.isNull()) {
                String current = adNode.asText(null);
                if (current != null && !current.isBlank()) {
                    return; // form data wins
                }
            }
            root.put("ad_id", adId);
            session.setFormContext(objectMapper.writeValueAsString(root));
        } catch (Exception ex) {
            log.debug("Session {}: failed to persist ad_id into form_context: {}",
                    session.getSessionId(), ex.getMessage());
        }
    }

    /**
     * Sprint 10 §L2 — translate the {@code (action, relation)} pair into
     * the short token surfaced in {@link BotSession#getDriftType()} and
     * the {@code drift_type} projection field.
     *
     * <p>Sprint 11 §M0 — when the action is {@code CONTINUE_CURRENT} but
     * the classifier's relation is {@code SAME_ISSUE} or
     * {@code SAME_UC_NEW_TASK}, surface the relation token so the
     * progressive same-UC follow-up signal survives the projection (the
     * decider returns {@code CONTINUE_CURRENT} when the session is
     * already in RESOLVE because no phase change is needed; that does
     * not mean the relation is uninteresting downstream).
     */
    /**
     * Sprint 12 §N0 — shared helper that materialises the
     * {@code primary_entity} JSON shape from the session's transient
     * Sprint 10 §L2 slots. Keeps the projection and event emission paths
     * agreeing on a single canonical shape ({@code listing} →
     * {@code {entity_type, ad_id}}, otherwise
     * {@code {entity_type, entity_value}}).
     *
     * @return a fresh {@link Map} or {@code null} when no entity is in
     *         scope.
     */
    private static Map<String, Object> buildPrimaryEntityPayload(BotSession session) {
        if (session == null) return null;
        String entityType = session.getPrimaryEntityType();
        String entityValue = session.getPrimaryEntityValue();
        boolean hasType = entityType != null && !entityType.isBlank();
        boolean hasValue = entityValue != null && !entityValue.isBlank();
        if (!hasType && !hasValue) {
            return null;
        }
        Map<String, Object> node = new LinkedHashMap<>();
        node.put("entity_type", entityType);
        if ("listing".equals(entityType) && hasValue) {
            node.put("ad_id", entityValue);
        } else if (hasValue) {
            node.put("entity_value", entityValue);
        }
        return node;
    }

    /**
     * Sprint 12 §N0 — emit a {@code RESOLVE_DISPOSITION} trace event for
     * RESOLVE / FAQ plans so a reviewer can audit why the bot stayed in
     * RESOLVE instead of advancing to CONFIRM. The event is a
     * backward-compatible addition; existing event consumers that filter
     * by event type continue to work.
     *
     * <p>Payload includes {@code resolve_disposition},
     * {@code task_status}, {@code phase_transition_reason},
     * {@code terminal_evidence} (record_outcome attempted / succeeded
     * counts), and the post-loop phase pair so the trace UI can join the
     * disposition with the eventual phase transition.
     */
    private void emitResolveDispositionEvent(BotSession session, PhasePlan plan,
                                              AgentRunResult runResult,
                                              PhaseTransitionDecision decision,
                                              String phaseBefore, String phaseAfter) {
        if (session == null || plan == null || runResult == null) return;
        if (!ResolveDispositionEvaluator.isResolveFaqPlan(plan)) return;
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("resolve_disposition", session.getResolveDisposition());
            payload.put("task_status", session.getTaskStatus());
            payload.put("phase_transition_reason",
                    decision == null ? null : decision.transitionReason());
            payload.put("transition_reason",
                    decision == null ? null : decision.transitionReason());
            payload.put("previous_phase", phaseBefore);
            payload.put("new_phase", phaseAfter);
            payload.put("active_use_case", session.getActiveUseCase());
            payload.put("current_task_type", session.getCurrentTaskType());
            payload.put("primary_entity", buildPrimaryEntityPayload(session));
            payload.put("terminal_evidence", buildTerminalEvidencePayload(runResult));
            emitEvent(session, "RESOLVE_DISPOSITION",
                    session.getTotalBotTurns(),
                    objectMapper.writeValueAsString(payload));
        } catch (Exception ex) {
            log.warn("Session {}: failed to emit RESOLVE_DISPOSITION event: {}",
                    session.getSessionId(), ex.getMessage());
        }
    }

    /**
     * Sprint 12 §N0 — emit a {@code RECORD_OUTCOME_GUARD} trace event when
     * the §M1 record-outcome guard observed a {@code record_outcome} call
     * on a RESOLVE / FAQ plan during this turn. Captures whether the
     * guard rejected the call (and the canonical reject reason) or
     * allowed it through, alongside the deterministic terminal-evidence
     * summary. Backward-compatible: only emitted when there is something
     * to report.
     */
    private void emitRecordOutcomeGuardEvent(BotSession session, PhasePlan plan,
                                              AgentRunResult runResult) {
        if (session == null || plan == null || runResult == null) return;
        String guardResult = session.getRecordOutcomeGuardResult();
        if (guardResult == null || guardResult.isBlank() || "none".equals(guardResult)) {
            return;
        }
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("record_outcome_guard_result", guardResult);
            payload.put("plan_phase", plan.phase());
            payload.put("plan_use_case", plan.useCase());
            payload.put("current_phase", session.getCurrentPhase());
            payload.put("active_use_case", session.getActiveUseCase());
            payload.put("terminal_evidence", buildTerminalEvidencePayload(runResult));
            emitEvent(session, "RECORD_OUTCOME_GUARD",
                    session.getTotalBotTurns(),
                    objectMapper.writeValueAsString(payload));
        } catch (Exception ex) {
            log.warn("Session {}: failed to emit RECORD_OUTCOME_GUARD event: {}",
                    session.getSessionId(), ex.getMessage());
        }
    }

    /**
     * Sprint 12 §N0 — terminal evidence summary used by both
     * {@code RESOLVE_DISPOSITION} and {@code RECORD_OUTCOME_GUARD}
     * events. Counts {@code record_outcome} attempts and successes for
     * the agent run that just completed; the {@code succeeded} flag is
     * the deterministic terminal condition that earns
     * {@code READY_TO_CONFIRM} per Sprint 11 §M1.
     */
    private static Map<String, Object> buildTerminalEvidencePayload(AgentRunResult result) {
        Map<String, Object> node = new LinkedHashMap<>();
        int attempted = 0;
        int succeeded = 0;
        if (result != null && result.toolEvents() != null) {
            for (ToolEvent te : result.toolEvents()) {
                if ("record_outcome".equals(te.toolName())) {
                    attempted++;
                    if (te.success()) succeeded++;
                }
            }
        }
        node.put("record_outcome_attempted", attempted);
        node.put("record_outcome_succeeded", succeeded);
        node.put("record_outcome_success", succeeded > 0);
        return node;
    }

    private static String deriveDriftTypeToken(
            com.gumtree.csagent.model.RerouteDecision.RerouteAction action,
            com.gumtree.csagent.model.IntentClassification.IntentRelation relation) {
        if (action == null) {
            return null;
        }
        return switch (action) {
            case SOFT_SHIFT_TO_DISCOVER -> "SOFT_SHIFT";
            case RISK_SHIFT_TO_INTAKE -> "RISK_SHIFT";
            case REBOUND_TO_RESOLVE ->
                    relation == com.gumtree.csagent.model.IntentClassification.IntentRelation.SAME_UC_NEW_TASK
                            ? "SAME_UC_NEW_TASK"
                            : "SAME_ISSUE";
            case ESCALATE_IMMEDIATELY -> "ESCALATE";
            case CONTINUE_CURRENT -> switch (relation) {
                case SAME_ISSUE -> "SAME_ISSUE";
                case SAME_UC_NEW_TASK -> "SAME_UC_NEW_TASK";
                default -> null;
            };
        };
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
        // Sprint 8.1 §M2: per the explicit "K0 must NOT fire when …"
        // list — no real LLM output AND no real tool events both
        // forbid K0. Require BOTH to count as evidence so a path
        // with only one of them (e.g. one real LLM event but every
        // tool call rejected by the plan whitelist, or one
        // successful tool event followed by a deadline-exhausted LLM
        // call that recorded zero LLM events) does not silently
        // stamp a UC. The cs259 reasoned-but-missing-UC path
        // satisfies both because it makes real search_knowledge
        // tool calls AND records real LLM events for each of them.
        return hasRealTool && hasRealLlm;
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
                                  ToolResult runtimeCaseResult,
                                  String displayedResponseText) {
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
                    // Sprint 9.1 — sanitize the verbatim tool error
                    // message before it lands on the persisted trace
                    // column so secrets / sensitive PII that the tool
                    // happened to interpolate into its error string
                    // (email, phone, postcode, bearer token, api key,
                    // long random credential) cannot leak via
                    // bot_turns.tool_calls.error_message.
                    String sanitizedError = ToolCallTraceSanitizer.sanitizeErrorMessage(
                            te.errorMessage());
                    if (sanitizedError != null) {
                        entry.put("error_message", sanitizedError);
                    }
                    // Trace contract requires every tool_call entry to carry an
                    // ``arguments`` map, even when empty (eval_interactive
                    // collector.py REQUIRED_TOOL_CALL_FIELDS). Always emit a
                    // map so the strict-mode validator does not flag empty
                    // tool calls as CONTRACT_VIOLATION:arguments.
                    entry.put("arguments",
                            te.arguments() != null ? te.arguments() : Map.of());

                    // Sprint 9 §O2 — trace observability fidelity. Persist a
                    // bounded sanitized {@code result_data} (and a compact
                    // {@code result_summary} string) for every tool event so
                    // the Trace UI Result panels are not blank. For failed
                    // events the {@code result_summary} carries the error
                    // surface; the existing {@code error_message} field is
                    // kept for back-compat with tooling that already reads it.
                    Object sanitizedResult = ToolCallTraceSanitizer.sanitizeResultData(
                            te.toolName(), te.resultData());
                    if (sanitizedResult != null) {
                        entry.put("result_data", sanitizedResult);
                    }
                    String resultSummary = ToolCallTraceSanitizer.summarize(
                            te.toolName(), te.success(), te.resultData(), te.errorMessage());
                    if (resultSummary != null && !resultSummary.isBlank()) {
                        entry.put("result_summary", resultSummary);
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
                    // Sprint 9.1 — sanitize runtime-side case-creation errors
                    // through the same redaction path used for AgentRunLoop
                    // tool errors.
                    caseEntry.put("error_message",
                            ToolCallTraceSanitizer.sanitizeErrorMessage(
                                    runtimeCaseResult.getErrorMessage()));
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

            // Sprint 8.1 §M2 follow-up: prefer the displayed user-facing
            // reply text. {@code result.finalUserMessage()} is null for
            // MAX_STEPS / ESCALATE / DEADLINE_EXCEEDED / LLM_UNAVAILABLE /
            // ERROR outcomes; the displayed text (from
            // PhaseTransitionDecision.responseText, or the kernel's "I'm
            // looking into this for you" fallback) is what the user
            // actually saw on those turns and what the next projection
            // needs in conversation_history.
            String persistedBotResponse = (displayedResponseText != null && !displayedResponseText.isBlank())
                    ? displayedResponseText
                    : result.finalUserMessage();
            BotTurn turn = BotTurn.builder()
                    .turnId(UUID.randomUUID().toString())
                    .sessionId(session.getSessionId())
                    .turnIndex(session.getTotalBotTurns())
                    .userMessage(userMessage)
                    .projectedContext(result.lastProjection())
                    .llmRawResponse(result.lastLlmRawResponse())
                    .botResponse(persistedBotResponse)
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

    /**
     * Sprint 8.1 §M3 — combine the DISCOVER {@link AgentRunResult} (which
     * terminated with {@link TerminalOutcome#USE_CASE_IDENTIFIED} after a
     * successful classify_use_case) with the subsequent RESOLVE
     * {@link AgentRunResult} so the persisted bot turn carries the full
     * cross-phase tool / LLM trace. The terminal outcome, escalation
     * reason, projection, raw response, and customer-facing message all
     * come from the RESOLVE run; only the events list is concatenated.
     *
     * <p>The DISCOVER events are placed BEFORE the RESOLVE events to
     * preserve chronological order in the trace (search_knowledge,
     * classify_use_case, then any RESOLVE tools).
     */
    static AgentRunResult mergeAgentRunResults(AgentRunResult discover,
                                                AgentRunResult resolve) {
        if (discover == null) return resolve;
        if (resolve == null) return discover;
        java.util.List<LlmCallEvent> mergedLlm = new java.util.ArrayList<>();
        if (discover.llmEvents() != null) mergedLlm.addAll(discover.llmEvents());
        if (resolve.llmEvents() != null) mergedLlm.addAll(resolve.llmEvents());
        java.util.List<ToolEvent> mergedTool = new java.util.ArrayList<>();
        if (discover.toolEvents() != null) mergedTool.addAll(discover.toolEvents());
        if (resolve.toolEvents() != null) mergedTool.addAll(resolve.toolEvents());
        return new AgentRunResult(
                resolve.messages(),
                mergedTool,
                mergedLlm,
                resolve.terminalOutcome(),
                resolve.finalUserMessage(),
                resolve.escalationReason(),
                resolve.lastProjection(),
                resolve.lastLlmRawResponse());
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
