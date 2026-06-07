package com.gumtree.csagent.service.runtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gumtree.csagent.model.AgentRunResult;
import com.gumtree.csagent.model.BotSession;
import com.gumtree.csagent.model.BotTurn;
import com.gumtree.csagent.model.LlmCallEvent;
import com.gumtree.csagent.model.LlmCallRecord;
import com.gumtree.csagent.model.LlmResponse;
import com.gumtree.csagent.model.ParsedAction;
import com.gumtree.csagent.model.PhasePlan;
import com.gumtree.csagent.model.ToolCall;
import com.gumtree.csagent.model.ToolEvent;
import com.gumtree.csagent.service.llm.LlmDeadlineExceededException;
import com.gumtree.csagent.service.llm.LlmUnavailableException;
import com.gumtree.csagent.service.runtime.skill.DispatchContext;
import com.gumtree.csagent.service.runtime.skill.RejectVerdict;
import com.gumtree.csagent.service.runtime.skill.SkillGuardrailDispatcher;
import com.gumtree.csagent.service.tools.ToolDispatcher;
import com.gumtree.csagent.service.tools.ToolResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Default {@link AgentRunLoop} implementation. Per Phase 3 §3.3.3 and Phase 4
 * §D16.B.2, the loop:
 *
 * <ol>
 *   <li>Builds a plan-aware projection (PhasePlan + accumulated tool results)</li>
 *   <li>Invokes the LLM</li>
 *   <li>Records an {@link LlmCallEvent} with a globally-monotonic sequence index</li>
 *   <li>Parses the response for {@code tool_calls} / {@code user_message}</li>
 *   <li>If no tool calls: returns {@link AgentRunResult#finalAnswer}</li>
 *   <li>If tool calls: validates each against {@link PhasePlan#allowedTools()},
 *       dispatches via {@link ToolDispatcher}, accumulates results, and loops</li>
 * </ol>
 *
 * <p>Special cases:
 * <ul>
 *   <li>{@code request_handover} short-circuits to {@link AgentRunResult#escalate}</li>
 *   <li>parser failure: treat raw response content as final user message</li>
 *   <li>LLM exception: caught upstream by {@link LlmInvocationService} which
 *       returns a safe-escalation response, so the parser sees a handover and
 *       the loop terminates normally. As an additional safety net we still
 *       catch any leaked exception and return {@link AgentRunResult#error}.</li>
 *   <li>Tool dispatch exception: caught here and recorded as a failed
 *       {@link ToolEvent}; loop continues so the LLM can react.</li>
 *   <li>Loop hits {@link PhasePlan#maxToolSteps()} without termination:
 *       returns {@link AgentRunResult#maxSteps}</li>
 * </ul>
 *
 * <p>Sequence numbers are monotonic across {@link LlmCallEvent} and
 * {@link ToolEvent} so trace UIs can replay the loop in order.
 */
@Slf4j
@Service
public class AgentRunLoopImpl implements AgentRunLoop {

    private static final String HANDOVER_TOOL = "request_handover";
    private static final String RECORD_OUTCOME_TOOL = "record_outcome";

    /**
     * Sprint 071 / S-Auto-15 (workstream A) — the ONLY tool the cross-turn
     * gate may ever consider. Read-only retrieval; write / side-effect tools
     * are structurally excluded from the gate (GUARDRAIL 0). The existing A1
     * / A3 within-turn blocks keep their inline {@code "search_knowledge"}
     * literals byte-untouched; this constant is used only by the NEW
     * cross-turn code so the read-only-only invariant is explicit.
     */
    private static final String SEARCH_KNOWLEDGE_TOOL = "search_knowledge";

    /**
     * Sprint 071 / S-Auto-15 (workstream A) — the FIXED cross-turn
     * suppression budget. The first cross-turn {@code search_knowledge} for a
     * UC after a standing viable hit is ALWAYS allowed (the legitimate
     * refinement); only the 2nd+ is eligible for suppression. FIXED at 1 by
     * GUARDRAIL 0 — lowering it re-introduces 误杀 and is forbidden.
     */
    private static final int CROSS_TURN_SUPPRESSION_BUDGET = 1;

    /**
     * Sprint 8.1 §M3 — DISCOVER classification phase boundary. When the
     * LLM successfully calls this tool inside a DISCOVER plan and
     * {@link com.gumtree.csagent.service.tools.ClassifyUseCaseTool}
     * commits a non-blank {@code activeUseCase} on the session, the loop
     * returns {@link com.gumtree.csagent.model.TerminalOutcome#USE_CASE_IDENTIFIED}
     * immediately rather than continuing to {@code maxToolSteps} (which
     * the legacy mapper would mis-map to ESCALATE).
     */
    private static final String CLASSIFY_USE_CASE_TOOL = "classify_use_case";
    private static final String DISCOVER_PHASE = "DISCOVER";

    // Sprint 39 (NEW M2) — Sprint 6 §G2 + Sprint 7 §I2 + Sprint 11 §M1
    // reject-reason labels + FAQ_PATH_UCS / FAQ_MISS_REASON / SEARCH_TOOL /
    // RESOLVE_TOOL constants previously inlined here are now owned by
    // SkillGuardrailDispatcher (per Sprint 37 freeze §9 unified dispatcher).
    // The canonical reject-reason labels remain byte-for-byte equivalent and
    // are surfaced via RejectVerdict.predicateName() at the dispatch sites.

    private final LlmInvocationService llmInvocation;
    private final ToolDispatcher toolDispatcher;
    private final ContextProjectionBuilder contextProjectionBuilder;
    private final ActionParser actionParser;
    private final ObjectMapper objectMapper;
    /**
     * Sprint 39 (NEW M2) — unified Skill terminal-predicate dispatcher per
     * Sprint 37 freeze §9. Replaces the 3 {@code shouldRejectXxx} static
     * predicates previously inlined here. Nullable for tests that don't
     * exercise guardrail enforcement (the dispatch sites null-check before
     * invoking).
     */
    private final SkillGuardrailDispatcher skillGuardrailDispatcher;

    /**
     * Spring DI constructor — Spring autowires the {@link SkillGuardrailDispatcher}
     * shipped in Sprint 39 so all four canonical guardrail types fire at the
     * three migrated dispatch sites.
     */
    @Autowired
    public AgentRunLoopImpl(LlmInvocationService llmInvocation,
                            ToolDispatcher toolDispatcher,
                            ContextProjectionBuilder contextProjectionBuilder,
                            ActionParser actionParser,
                            ObjectMapper objectMapper,
                            SkillGuardrailDispatcher skillGuardrailDispatcher) {
        this.llmInvocation = llmInvocation;
        this.toolDispatcher = toolDispatcher;
        this.contextProjectionBuilder = contextProjectionBuilder;
        this.actionParser = actionParser;
        this.objectMapper = objectMapper;
        this.skillGuardrailDispatcher = skillGuardrailDispatcher;
    }

    /**
     * Sprint 39 — backward-compatible 5-arg constructor preserved for the
     * many existing tests that exercise AgentRunLoop infrastructure
     * (deadline, parsing, clarification detection, etc.) and do not need
     * guardrail enforcement. Passing a null dispatcher disables guardrail
     * enforcement; tests that DO exercise predicate semantics
     * (Sprint11ProgressiveResolveTest, Sprint7IntakeStateTest,
     * AgentRunLoopS1FaqGroundedResolveGuardTest) use the 6-arg constructor
     * with {@code SkillTestFixtures.productionDispatcher()}.
     */
    public AgentRunLoopImpl(LlmInvocationService llmInvocation,
                            ToolDispatcher toolDispatcher,
                            ContextProjectionBuilder contextProjectionBuilder,
                            ActionParser actionParser,
                            ObjectMapper objectMapper) {
        this(llmInvocation, toolDispatcher, contextProjectionBuilder, actionParser,
                objectMapper, null);
    }

    @Override
    public AgentRunResult run(PhasePlan plan,
                              BotSession session,
                              String userMessage,
                              List<BotTurn> history) {
        if (plan == null) {
            log.error("AgentRunLoop.run called with null plan; returning error");
            return AgentRunResult.error("plan_required");
        }

        List<LlmCallEvent> llmEvents = new ArrayList<>();
        List<ToolEvent> toolEvents = new ArrayList<>();
        // Sprint 51 / M5 S2 — observation-only side-record: one
        // LlmCallRecord per successful LLM invocation in this run, carrying
        // the full raw response + the projection that fed THIS step (the
        // existing `lastProjection`/`lastLlmRawResponse` get overwritten by
        // the loop and only the final value survives into BotTurn's single
        // columns). Never read by the loop; passed through AgentRunResult to
        // TraceWriter for persistence in `bot_turn_llm_calls`.
        List<LlmCallRecord> llmCallRecords = new ArrayList<>();
        Map<String, Object> accumulatedToolResults = new LinkedHashMap<>();
        // Sprint 067 / S-Auto-12 (A1 idempotency 回挡, backstop half) —
        // per-run identity cache: (toolName | canonicalArgumentsHash) ->
        // the successful ToolEvent that first served that exact call this
        // run. Only success==true dispatches are recorded (so an
        // external-failure result is never cached and a legitimate retry
        // re-dispatches). A byte-identical repeat returns the cached result
        // without re-executing the tool — the deterministic backstop behind
        // the `already_called` binding soft signal. Map lifetime is exactly
        // one run() invocation, so it cannot leak across sessions/turns.
        Map<String, ToolEvent> successfulDispatchCache = new LinkedHashMap<>();
        // Sprint 069 / S-Auto-13b (A3 deterministic backstop) — per-run
        // tracker for the most-recent SUCCESSFUL search_knowledge dispatch
        // this run whose result carried `faq_miss=false` (a viable hit).
        // When a NEW search_knowledge dispatch arrives and this slot is
        // non-null, the gate below serves the cached viable-hit result
        // instead of re-executing the tool. Cleared (reset to null) on
        // any subsequent search_knowledge that returns `faq_miss=true` or
        // fails — so a re-search after a non-viable result is allowed and
        // a transient external failure does not freeze the gate. Lifetime
        // is one run() invocation (one outer turn); it cannot leak across
        // turns or sessions. Keyed purely on the `faq_miss` RESULT state
        // (not on query content / keyword / regex / enum / per-UC), so the
        // LLM still owns the FIRST search, what tool, what content — this
        // is the same falsification-→-deterministic-backstop pattern as
        // A1 (Sprint 19/20 soft-signal-alone falsified → S-Auto-12 hybrid),
        // applied to the paraphrase shape A1 (byte-identical only) cannot
        // catch. The S-Auto-13 A3 soft layer (`search_reuse_instruction`
        // projection echo + `grounding_instruction` paraphrase-discipline
        // line in resolve_faq_grounded_answer.yaml) STAYS beneath this
        // backstop as the soft-signal-first measure that fires before the
        // deterministic gate.
        ToolEvent lastSearchKnowledgeViableHit = null;
        int sequence = 0;
        String lastProjection = null;
        String lastLlmRawResponse = null;

        int maxSteps = Math.max(1, plan.maxToolSteps());

        // Sprint 7.1 §J0 — persist partial intake fields from the current
        // user turn + form context BEFORE the first projection so the
        // intake_state surface reflects what the user has already supplied.
        // Without this merge, ContextProjectionBuilder would only see fields
        // the LLM persisted via request_handover.arguments.intake_fields,
        // and a normal clarification turn would leave the projection
        // showing every required field as still-remaining (cs066 stall
        // shape). Runs once per user turn since userMessage / formContext
        // are stable across loop iterations.
        mergePartialIntakeFromContext(session, plan, userMessage);

        // Sprint 071 / S-Auto-15 (workstream A) — NEW, SEPARATE,
        // BotSession-scoped CROSS-TURN search_knowledge re-search
        // suppression gate. Runs ALONGSIDE — never modifying — the A1
        // per-run dedup cache (505-526) and the A3 within-turn
        // paraphrase gate (558-576); both of those `continue` first, so a
        // call only ever reaches this gate's dispatch-site check after they
        // have not fired.
        //
        // THE NARROW CROSS-TURN INVARIANT (default = ALLOW; suppress ONLY
        // when ALL four conditions PROVE true; FAIL OPEN on any ambiguity):
        //   1. session.getActiveUseCase() is non-null AND EQUAL to the UC at
        //      which the standing viable hit was captured (no UC change);
        //   2. NO drift this turn — driftType null/"none" AND
        //      activeUseCase == previousActiveUseCase;
        //   3. a standing viable-hit marker exists for THAT exact UC
        //      (persisted; set only on a search_knowledge faq_miss=false);
        //   4. [budget] at least ONE cross-turn search_knowledge for this UC
        //      has ALREADY been allowed since the standing hit (checked at
        //      the dispatch site below) — so the FIRST post-hit cross-turn
        //      re-search is NEVER suppressed.
        //
        // Conditions 1–3 are turn-stable, so they are evaluated ONCE here at
        // run start from a SNAPSHOT of the persisted standing state (loaded
        // from the DB this turn). Condition 4 (the budget) is the only
        // per-call check and lives at the dispatch site. Keyed PURELY on the
        // EXISTING activeUseCase / driftType / faq_miss signals + a
        // cardinality budget — NO query-content / keyword / regex /
        // similarity / per-UC matching.
        //
        // Reset (gate disabled + standing state CLEARED) on ANY drift signal
        // or UC change is handled right here at run start; the genuine-miss /
        // resolution / escalation / record_outcome resets clear the standing
        // state at their dispatch sites below. Because SessionManager saves
        // the session AFTER run() returns, every mutation below is written to
        // the entity directly (no per-return write-back needed).
        final String standingHitUcSnapshot = session.getCrossTurnFaqHitUseCase();
        final String standingHitPayloadSnapshot = session.getCrossTurnFaqHitPayload();
        final Integer standingBudgetBoxed = session.getCrossTurnSearchAllowedSinceHit();
        final int standingBudgetSnapshot = standingBudgetBoxed == null ? 0 : standingBudgetBoxed;
        final int standingHitTurnSnapshot = session.getTotalBotTurns() == null
                ? -1 : session.getTotalBotTurns();

        final String activeUc = session.getActiveUseCase();
        final boolean driftThisTurn = isDriftThisTurn(session);

        // If ANY drift signal (incl. a UC change) is observed this turn, the
        // standing marker is no longer valid for the current intent — DISABLE
        // the gate (handled by crossTurnGateConditionsHold below, which is
        // false when driftThisTurn) and CLEAR the persisted standing state so
        // the marker does not leak into the next turn. clearCrossTurnStandingHit
        // is a no-op when nothing is set.
        if (driftThisTurn) {
            clearCrossTurnStandingHit(session,
                    "drift signal this turn (driftType="
                            + session.getDriftType() + ")");
        }

        // Conditions 1–3: same non-null un-drifted UC AND a standing viable
        // hit (UC marker + payload) exists for THAT exact UC. Default ALLOW:
        // any null / missing signal leaves this false.
        final boolean crossTurnGateConditionsHold =
                !driftThisTurn
                        && activeUc != null
                        && standingHitUcSnapshot != null
                        && standingHitUcSnapshot.equals(activeUc)
                        && standingHitPayloadSnapshot != null
                        && !standingHitPayloadSnapshot.isBlank();

        // Run-local guard so the FIXED budget is spent at most ONCE per turn
        // for the first allowed cross-turn refinement (a turn has at most one
        // search_knowledge that escapes the within-turn A3 gate, but this
        // guards against any double-count).
        boolean crossTurnRefinementCountedThisTurn = false;

        for (int step = 0; step < maxSteps; step++) {
            // 1. Build plan-aware projection.
            //
            // Sprint 20 Track B (R-prompt-projection-already-called-soft-
            // signal): pass the per-run toolEvents list to the projection
            // builder so it can surface an `already_called` slot listing
            // every successful prior tool dispatch in this run.
            //
            // Sprint 067 / S-Auto-12 (M-Auto-3, A1 hybrid): the slot is now
            // a BINDING soft signal — the system prompt tells the LLM that
            // byte-identical repeats are auto-deduplicated and waste a turn,
            // so it should draft from accumulated_tool_results or take the
            // next action (it still owns WHICH tool / WHAT content). The
            // deterministic backstop half of the hybrid lives at the
            // dispatch site below (`successfulDispatchCache`): a byte-
            // identical success==true repeat is served from cache without
            // re-executing the tool. Soft-signal-alone was empirically
            // falsified (Sprint 19 chose soft-first, Sprint 20 shipped the
            // slot, the identical-retry storm persisted at temp=0), so both
            // halves ship together.
            String projection;
            try {
                projection = contextProjectionBuilder.build(
                        session, history, plan, userMessage, accumulatedToolResults,
                        toolEvents);
            } catch (Exception ex) {
                log.error("AgentRunLoop projection build failed at step {}: {}",
                        step, ex.getMessage(), ex);
                return AgentRunResult.error("projection_failed: " + ex.getMessage());
            }
            lastProjection = projection;

            // 2. Invoke LLM. Sprint 8.1 §M2: deadline / infra failures now
            //    surface as typed exceptions (LlmInvocationService no longer
            //    converts them into a synthetic SAFE_ESCALATION_RESPONSE).
            //    Catch each one and return a TerminalOutcome that the K0
            //    fallback gate can recognise as "no real LLM work happened" —
            //    accumulated llmEvents / toolEvents are preserved so the
            //    trace still reflects what ran before the failure.
            long t0 = System.currentTimeMillis();
            LlmResponse response;
            try {
                response = llmInvocation.invokeChat(projection, userMessage,
                        session.getSessionId(), session.getTotalBotTurns());
            } catch (LlmDeadlineExceededException ex) {
                log.warn("AgentRunLoop llm deadline exceeded at step {}: {}", step, ex.getMessage());
                return AgentRunResult.deadlineExceeded(
                        "llm_deadline_exceeded: " + ex.getMessage(),
                        llmEvents, toolEvents, lastProjection, llmCallRecords);
            } catch (LlmUnavailableException ex) {
                log.error("AgentRunLoop llm unavailable at step {} (failure_class={}): {}",
                        step, ex.getFailureClass(), ex.getMessage());
                return AgentRunResult.llmUnavailable(
                        "llm_unavailable: failure_class=" + ex.getFailureClass()
                                + " " + ex.getMessage(),
                        llmEvents, toolEvents, lastProjection, llmCallRecords);
            } catch (Exception ex) {
                log.error("AgentRunLoop llm invocation failed at step {}: {}",
                        step, ex.getMessage(), ex);
                return AgentRunResult.error("llm_invocation_failed: " + ex.getMessage());
            }
            long latency = System.currentTimeMillis() - t0;
            lastLlmRawResponse = response == null ? null : response.getContent();

            // 3. Record LlmCallEvent with monotonic sequence index
            LlmCallEvent base = LlmCallEvent.of(step, response, latency);
            llmEvents.add(new LlmCallEvent(
                    sequence++, step, base.model(), base.promptTokens(),
                    base.completionTokens(), base.latencyMs(), base.responseSummary()));

            // 4. Parse the response
            String content = response == null ? null : response.getContent();
            ParsedAction action;
            try {
                action = actionParser.parse(content);
            } catch (Exception ex) {
                log.warn("AgentRunLoop parse failure at step {}: {} — treating raw content as final user_message",
                        step, ex.getMessage());
                String fallbackMessage = (content == null || content.isBlank())
                        ? "I'm having trouble processing your request."
                        : content;
                // Sprint 51 — record this invocation even on parser failure
                // (raw response + projection are still useful for debugging
                // the malformed payload). Empty tool_calls since parsing
                // didn't yield any.
                llmCallRecords.add(new LlmCallRecord(
                        step, "chat", llmInvocation.getModelName(), latency,
                        lastLlmRawResponse, projection, List.of()));
                return AgentRunResult.finalAnswer(fallbackMessage, llmEvents, toolEvents,
                        lastProjection, lastLlmRawResponse, llmCallRecords);
            }

            if (action == null) {
                log.warn("AgentRunLoop parser returned null at step {}; treating raw content as final answer",
                        step);
                String fallbackMessage = (content == null || content.isBlank())
                        ? "I'm having trouble processing your request."
                        : content;
                llmCallRecords.add(new LlmCallRecord(
                        step, "chat", llmInvocation.getModelName(), latency,
                        lastLlmRawResponse, projection, List.of()));
                return AgentRunResult.finalAnswer(fallbackMessage, llmEvents, toolEvents,
                        lastProjection, lastLlmRawResponse, llmCallRecords);
            }

            List<ToolCall> calls = action.getToolCalls();
            String userMsg = action.getUserMessage();

            // Sprint 51 / M5 S2 — at the existing step boundary, snapshot a
            // full-fidelity record of this LLM invocation: stepIndex, call
            // type, model, latency, full raw response, the projection that
            // fed THIS step (vs `lastProjection` which is overwritten next
            // step), and the LLM-issued tool calls observed. Observation
            // only — never read by the loop, never alters control flow or
            // call timing.
            llmCallRecords.add(new LlmCallRecord(
                    step, "chat", llmInvocation.getModelName(), latency,
                    lastLlmRawResponse, projection,
                    calls == null ? List.of() : calls));

            // 5. No tool calls -> final user message (or clarification).
            // Sprint 8.1 follow-up (2026-05-06): distinguish clarifying
            // questions from real final answers so the phase mapper does
            // not eagerly transition RESOLVE → CONFIRM → CLOSE while the
            // bot is still asking the user for required details. The LLM
            // routinely emits no-tool-call clarifications like "Can you
            // confirm the ad ID?" which previously surfaced as
            // FINAL_ANSWER and chained RESOLVE → CONFIRM (turn N) →
            // CLOSE (turn N+1) — ending the chat mid-conversation. The
            // existing PhaseEvaluator.isClarificationTurn helper detects
            // the shape ("?"-suffix or clarifying phrase + no tool
            // calls); we use the same predicate here so the AgentRunLoop
            // and PhaseEvaluator agree on what counts as a clarification.
            if (calls == null || calls.isEmpty()) {
                String finalText = (userMsg == null || userMsg.isBlank())
                        ? "I'm looking into this for you."
                        : userMsg;
                // R2.a #3 — DISCOVER free-text clarification counter, wired on
                // the LIVE AgentRunLoopImpl path (the legacy PhaseEvaluator:880
                // +1 site is unreachable here, so session.clarificationCount
                // otherwise stays 0 forever and the BudgetChecker clarification
                // cap is never reachable — c3). STRUCTURAL CARDINALITY ONLY: no
                // content / similarity / Jaccard heuristic. `userMsg` is the
                // BOT's outgoing reply (action.getUserMessage(), the LLM's
                // user_message field) — NOT the `userMessage` run() parameter
                // (the customer's incoming turn). No tool calls in this branch
                // (so no UC commit possible this step); ucCommittedThisTurn is
                // computed defensively against the run-start snapshot.
                boolean ucCommittedThisTurn =
                        !Objects.equals(activeUc, session.getActiveUseCase());
                // S-Auto-30 — exclude the runtime-synthesised null-turn placeholder
                // from the clarification counter. ActionParser substitutes a non-blank
                // placeholder when the LLM emits an empty user_message + no tool_calls,
                // so `userMsg` is never blank here; the only structural discriminator
                // is the provenance flag. A runtime placeholder is not an LLM-authored
                // clarification attempt and must not burn the clarification budget.
                // Structural provenance only — no content / keyword gate is added (the
                // R2.a "structural cardinality only" design is preserved).
                if (action.isUserMessageSynthesised()) {
                    log.info("AgentRunLoop DISCOVER null-turn: user_message_synthesised=true "
                                    + "(runtime placeholder) at step {}; excluded from clarification counter",
                            step);
                }
                if (isDiscoverFreeTextClarification(
                        plan.phase(), false, ucCommittedThisTurn, userMsg)
                        && !action.isUserMessageSynthesised()) {
                    session.setClarificationCount(session.getClarificationCount() + 1);
                }
                if (isClarificationMessage(finalText)) {
                    return AgentRunResult.clarification(finalText, llmEvents, toolEvents,
                            lastProjection, lastLlmRawResponse, llmCallRecords);
                }
                return AgentRunResult.finalAnswer(finalText, llmEvents, toolEvents,
                        lastProjection, lastLlmRawResponse, llmCallRecords);
            }

            // 6. Dispatch each tool call
            boolean handoverRequested = false;
            String handoverReason = null;
            for (ToolCall call : calls) {
                String toolName = call == null ? null : call.getName();
                if (toolName == null || toolName.isBlank()) {
                    log.warn("AgentRunLoop received tool_call with no name at step {}", step);
                    continue;
                }

                // 6a. Validate against PhasePlan.allowedTools whitelist
                ToolResult validation = toolDispatcher.validateAgainstPlan(plan, toolName);
                if (validation == null || !validation.isSuccess()) {
                    String reason = validation == null
                            ? "tool_not_in_plan"
                            : validation.getErrorMessage();
                    ToolEvent base2 = ToolEvent.rejected(step, call, reason);
                    toolEvents.add(new ToolEvent(
                            sequence++, step, base2.toolName(), base2.arguments(),
                            base2.success(), base2.resultData(), base2.errorMessage(),
                            base2.latencyMs()));
                    continue;
                }

                // Sprint 39 (NEW M2) — unified Skill terminal-predicate
                // dispatcher per Sprint 37 freeze decision (h) §9. REPLACES the
                // three scattered Sprint 6 §G2 / Sprint 7 §I2 / Sprint 11 §M1
                // shouldRejectXxx predicates with a single dispatch surface
                // walking the active Skill's `guardrails[]` in declaration
                // order with short-circuit-on-first-reject semantics (design
                // doc §9.2). The Skill scope checks (FAQ_PATH_UCS / INTAKE_UCS)
                // are now implicit via `applicable_use_cases` on the RESOLVE
                // Skill YAMLs. For RESOLVE-INTAKE Skill, intake_complete_required
                // fires on request_handover; for RESOLVE-FAQ Skill,
                // faq_miss_handover_requires_resolve_attempt fires on
                // request_handover and premature_resolve_outcome_guard +
                // must_cite_source fire on record_outcome.
                if (HANDOVER_TOOL.equals(toolName)
                        && IntakeFieldsRegistry.isIntakeUseCase(plan.useCase())) {
                    // Sprint 7.1 §J0 — persist inline intake fields BEFORE
                    // the guardrail check so a single complete handover call
                    // merging the missing fields is allowed through. Side
                    // effect remains at the dispatch site (not migrated to
                    // the dispatcher, which is pure verdict).
                    persistInlineIntakeFields(session, call);
                }
                if (HANDOVER_TOOL.equals(toolName) && skillGuardrailDispatcher != null) {
                    DispatchContext ctx = new DispatchContext(
                            plan, session, accumulatedToolResults,
                            lastLlmRawResponse, Optional.ofNullable(userMsg));
                    Optional<RejectVerdict> verdict =
                            skillGuardrailDispatcher.checkBeforeDispatch(plan, call, ctx);
                    if (verdict.isPresent()) {
                        RejectVerdict v = verdict.get();
                        log.warn(
                                "AgentRunLoop {} guardrail rejected request_handover for UC '{}' at step {}: {}",
                                v.predicateName(), plan.useCase(), step, v.hint());
                        ToolEvent rejected = ToolEvent.rejected(step, call, v.predicateName());
                        toolEvents.add(new ToolEvent(
                                sequence++, step, rejected.toolName(), rejected.arguments(),
                                rejected.success(), rejected.resultData(), rejected.errorMessage(),
                                rejected.latencyMs()));
                        Map<String, Object> guardWrap = new LinkedHashMap<>();
                        guardWrap.put("error", v.predicateName());
                        guardWrap.put("hint", v.hint());
                        // Sprint 7 §I2 trace shape preserved: surface
                        // `missing_fields` (intake-complete guardrail
                        // verdict) on the LLM-visible accumulated_tool_results
                        // entry so the LLM can ask for the right fields on
                        // the next turn. Other trace fields stay in the
                        // dispatcher verdict trace (logged via v.hint()).
                        Object missing = v.trace().get("missing_fields");
                        if (missing != null) {
                            guardWrap.put("missing_fields", missing);
                        }
                        accumulatedToolResults.put(HANDOVER_TOOL, guardWrap);
                        continue;
                    }
                }

                if (RECORD_OUTCOME_TOOL.equals(toolName) && skillGuardrailDispatcher != null) {
                    Map<String, Object> rcArgs = call.getArguments();
                    Object outcomeObj = rcArgs == null ? null : rcArgs.get("outcome_class");
                    if (outcomeObj == null && rcArgs != null) {
                        outcomeObj = rcArgs.get("outcome");
                    }
                    String outcomeClassArg = outcomeObj == null ? null : outcomeObj.toString();
                    DispatchContext ctx = new DispatchContext(
                            plan, session, accumulatedToolResults,
                            lastLlmRawResponse, Optional.ofNullable(userMsg));
                    Optional<RejectVerdict> verdict =
                            skillGuardrailDispatcher.checkBeforeOutcomePersist(
                                    plan, outcomeClassArg, ctx);
                    if (verdict.isPresent()) {
                        RejectVerdict v = verdict.get();
                        log.warn(
                                "AgentRunLoop {} guardrail rejected record_outcome for UC '{}' at step {} (phase={}): {}",
                                v.predicateName(), plan.useCase(), step,
                                session == null ? "?" : session.getCurrentPhase(), v.hint());
                        ToolEvent rejected = ToolEvent.rejected(step, call, v.predicateName());
                        toolEvents.add(new ToolEvent(
                                sequence++, step, rejected.toolName(), rejected.arguments(),
                                rejected.success(), rejected.resultData(), rejected.errorMessage(),
                                rejected.latencyMs()));
                        Map<String, Object> guardWrap = new LinkedHashMap<>();
                        guardWrap.put("error", v.predicateName());
                        guardWrap.put("hint", v.hint());
                        accumulatedToolResults.put(RECORD_OUTCOME_TOOL, guardWrap);
                        // Sprint 12 §N0 — stamp the guard result on the
                        // session for trace fidelity. Sticky across loop
                        // iterations: the first rejection wins so a later
                        // allowed call does not erase the audit signal.
                        if (session != null && (session.getRecordOutcomeGuardResult() == null
                                || session.getRecordOutcomeGuardResult().isBlank()
                                || "none".equals(session.getRecordOutcomeGuardResult()))) {
                            session.setRecordOutcomeGuardResult("rejected:" + v.predicateName());
                        }
                        continue;
                    }
                }
                // Sprint 12 §N0 — observability: record_outcome calls that
                // pass the guardrails mark the session so the post-loop
                // RECORD_OUTCOME_GUARD event captures the allowed branch
                // for trace fidelity. A later rejection in the same loop
                // overwrites this back to rejected via the branch above.
                if (RECORD_OUTCOME_TOOL.equals(toolName) && session != null
                        && (session.getRecordOutcomeGuardResult() == null
                                || session.getRecordOutcomeGuardResult().isBlank()
                                || "none".equals(session.getRecordOutcomeGuardResult()))) {
                    session.setRecordOutcomeGuardResult("allowed");
                }

                // 6a-bis. Sprint 067 / S-Auto-12 (A1 idempotency 回挡,
                // backstop half) — if this exact call (tool name + canonical
                // arguments hash) already SUCCEEDED earlier in THIS run,
                // serve the cached result instead of re-dispatching. The
                // tool is not re-executed (no external call / budget), the
                // event is annotated `deduplicated` + `original_at_step`,
                // and the cached payload is accumulated so the LLM still
                // sees it under accumulated_tool_results. Read-only repeats
                // (search_knowledge, get_customer_context) dominate the
                // temp=0 identical-retry storm; the terminal / short-circuit
                // tools (request_handover, classify_use_case on DISCOVER)
                // end the run on first success so they can never be cached-
                // then-repeated, and a repeat record_outcome is idempotent
                // by design. No tool-name / UC branching: the key reuses the
                // existing order-insensitive canonicalArgumentsHash for
                // every tool. A "hash_error" arguments hash disables dedup
                // for that call (it always re-dispatches).
                String dedupHash = contextProjectionBuilder.canonicalArgumentsHash(
                        call.getArguments());
                String dedupKey = "hash_error".equals(dedupHash)
                        ? null
                        : toolName + "|" + dedupHash;
                if (dedupKey != null) {
                    ToolEvent cachedHit = successfulDispatchCache.get(dedupKey);
                    if (cachedHit != null) {
                        ToolEvent base3 = ToolEvent.deduplicated(
                                step, call, cachedHit.resultData(), cachedHit.stepIndex());
                        toolEvents.add(new ToolEvent(
                                sequence++, step, base3.toolName(), base3.arguments(),
                                base3.success(), base3.resultData(), base3.errorMessage(),
                                base3.latencyMs(), base3.deduplicated(), base3.originalAtStep()));
                        accumulatedToolResults.put(toolName, cachedHit.resultData());
                        log.info(
                                "AgentRunLoop A1 dedup: byte-identical {} at step {} served from "
                                        + "per-run cache (original_at_step={}); tool not re-dispatched",
                                toolName, step, cachedHit.stepIndex());
                        continue;
                    }
                }

                // 6a-ter. Sprint 069 / S-Auto-13b (A3 deterministic backstop)
                // — faq_miss-state-aware same-turn `search_knowledge`
                // re-search suppression gate. Runs AFTER A1 has cleared a
                // byte-identical match, so A1's logic is untouched: A1
                // catches byte-identical repeats (same query string), this
                // gate catches paraphrases of the same intent (different
                // query string, same `faq_miss=false` result state). The
                // gate is keyed on the EXISTING `faq_miss` RESULT flag
                // (NOT on query content / keyword / regex / enum / per-UC):
                // when a NEW search_knowledge dispatch arrives and the
                // most-recent prior search_knowledge this run returned a
                // viable hit (`faq_miss=false`, tracked above), serve that
                // prior hit + annotate `paraphrase_suppressed` + flatten
                // onto the trace via ControlKernel. The tool is NOT
                // re-executed (no step / budget charged — mirror A1). A
                // re-search after the most-recent search_knowledge returned
                // `faq_miss=true` (no viable hit) or failed is ALLOWED;
                // the tracker reset in 6c handles that case.
                //
                // The S-Auto-13 A3 soft layer (`search_reuse_instruction`
                // projection echo + `grounding_instruction` paraphrase-
                // discipline line in resolve_faq_grounded_answer.yaml) was
                // empirically falsified — `deepseek-v4-flash` ignores the
                // soft signal, the PARAPHRASE_STORM stayed 16/16/7 vs the
                // §11 ≤3 target. Per the soft-signal-first rule the soft
                // layer STAYS beneath this backstop as the first measure;
                // the backstop is the deterministic substrate the LLM
                // cannot route around. Same falsification-→-deterministic-
                // backstop pattern as A1 (Sprint 19/20 soft-signal-alone
                // → S-Auto-12 hybrid 回挡).
                if ("search_knowledge".equals(toolName)
                        && lastSearchKnowledgeViableHit != null) {
                    ToolEvent base4 = ToolEvent.paraphraseSuppressed(
                            step, call, lastSearchKnowledgeViableHit.resultData(),
                            lastSearchKnowledgeViableHit.stepIndex());
                    toolEvents.add(new ToolEvent(
                            sequence++, step, base4.toolName(), base4.arguments(),
                            base4.success(), base4.resultData(), base4.errorMessage(),
                            base4.latencyMs(), base4.deduplicated(), base4.originalAtStep(),
                            base4.paraphraseSuppressed(), base4.faqHitAtStep()));
                    accumulatedToolResults.put(toolName,
                            lastSearchKnowledgeViableHit.resultData());
                    log.info(
                            "AgentRunLoop A3 backstop: same-turn search_knowledge re-search at "
                                    + "step {} suppressed (prior viable hit at step {}); tool not "
                                    + "re-dispatched",
                            step, lastSearchKnowledgeViableHit.stepIndex());
                    continue;
                }

                // 6a-quater. Sprint 071 / S-Auto-15 (workstream A) — NEW
                // BotSession-scoped CROSS-TURN search_knowledge re-search
                // suppression gate. Runs AFTER A1 (505-526) and the A3
                // within-turn gate (558-576) — both `continue` first — so
                // reaching here means this search_knowledge call was neither
                // a byte-identical repeat nor a same-run paraphrase of a
                // viable hit captured THIS turn. It IS eligible for the
                // cross-turn gate ONLY when conditions 1–3 held at run start
                // (crossTurnGateConditionsHold: same non-null un-drifted UC +
                // a standing viable hit persisted for that exact UC from a
                // PRIOR turn) AND condition 4 (the FIXED budget) is spent.
                //
                // Default = ALLOW. The gate is structurally limited to
                // search_knowledge (read-only); the conditions are checked
                // against snapshots of the EXISTING activeUseCase / driftType
                // / faq_miss signals + a cardinality budget — NO content /
                // keyword / regex / similarity / per-UC matching. Drift and
                // UC change disabled it at run start.
                if (SEARCH_KNOWLEDGE_TOOL.equals(toolName)
                        && crossTurnGateConditionsHold) {
                    if (standingBudgetSnapshot >= CROSS_TURN_SUPPRESSION_BUDGET) {
                        // Budget spent: the legitimate first cross-turn
                        // refinement already happened in a PRIOR turn. Serve
                        // the standing payload + annotate; do NOT re-dispatch
                        // or charge a step (mirror A1 / within-turn A3).
                        Object standingPayload = deserializeStandingPayload(
                                standingHitPayloadSnapshot);
                        if (standingPayload != null) {
                            ToolEvent base5 = ToolEvent.crossTurnParaphraseSuppressed(
                                    step, call, standingPayload, standingHitTurnSnapshot);
                            toolEvents.add(new ToolEvent(
                                    sequence++, step, base5.toolName(), base5.arguments(),
                                    base5.success(), base5.resultData(), base5.errorMessage(),
                                    base5.latencyMs(), base5.deduplicated(),
                                    base5.originalAtStep(), base5.paraphraseSuppressed(),
                                    base5.faqHitAtStep(), base5.crossTurnParaphraseSuppressed(),
                                    base5.crossTurnHitAtTurn()));
                            accumulatedToolResults.put(toolName, standingPayload);
                            log.info(
                                    "AgentRunLoop A(cross-turn) backstop: cross-turn "
                                            + "search_knowledge re-search at step {} suppressed "
                                            + "(standing viable hit for UC={} captured at turn {}, "
                                            + "budget {} spent); tool not re-dispatched",
                                    step, standingHitUcSnapshot, standingHitTurnSnapshot,
                                    standingBudgetSnapshot);
                            continue;
                        }
                        // Malformed / unreadable standing payload → FAIL OPEN:
                        // fall through to a normal dispatch. (Never suppress
                        // on evidence we cannot serve.)
                    } else if (!crossTurnRefinementCountedThisTurn) {
                        // First cross-turn refinement since the standing hit —
                        // ALWAYS allowed (budget-1). Spend the budget so the
                        // NEXT turn's snapshot sees it as >= 1. Counted once
                        // per turn; the call falls through to a normal
                        // dispatch below.
                        crossTurnRefinementCountedThisTurn = true;
                        session.setCrossTurnSearchAllowedSinceHit(
                                standingBudgetSnapshot + 1);
                        log.info(
                                "AgentRunLoop A(cross-turn) backstop: first cross-turn "
                                        + "search_knowledge refinement for UC={} ALLOWED "
                                        + "(budget-1); spending budget to {}",
                                standingHitUcSnapshot, standingBudgetSnapshot + 1);
                    }
                }

                // 6b. Dispatch and record event
                long tt = System.currentTimeMillis();
                ToolResult result;
                try {
                    result = toolDispatcher.dispatch(toolName, session, call.getArguments());
                } catch (Exception ex) {
                    log.warn("AgentRunLoop tool dispatch failed for '{}' at step {}: {}",
                            toolName, step, ex.getMessage());
                    ToolEvent failed = new ToolEvent(
                            sequence++, step, toolName, call.getArguments(),
                            false, null, "tool_dispatch_exception: " + ex.getMessage(),
                            System.currentTimeMillis() - tt);
                    toolEvents.add(failed);
                    continue;
                }
                long toolLatency = System.currentTimeMillis() - tt;
                ToolEvent base2 = ToolEvent.of(step, call, result, toolLatency);
                ToolEvent recorded = new ToolEvent(
                        sequence++, step, base2.toolName(), base2.arguments(),
                        base2.success(), base2.resultData(), base2.errorMessage(),
                        base2.latencyMs());
                toolEvents.add(recorded);

                // Sprint 067 / S-Auto-12 — only success==true dispatches
                // enter the per-run identity cache, so an external-failure
                // result is never cached and a same-args retry re-dispatches.
                // putIfAbsent keeps the FIRST success as the canonical
                // original_at_step for every later byte-identical repeat.
                if (dedupKey != null && result != null && result.isSuccess()) {
                    successfulDispatchCache.putIfAbsent(dedupKey, recorded);
                }

                // 6c-bis. Sprint 069 / S-Auto-13b (A3 deterministic
                // backstop) — refresh the per-run viable-hit tracker on
                // every fresh search_knowledge dispatch. The slot points
                // at the most-recent search_knowledge ToolEvent whose
                // result carried `faq_miss=false`; the next time the LLM
                // tries to re-search this turn, the gate above (6a-ter)
                // serves that cached result instead of re-executing.
                //
                // Reset rules: a successful search whose result is
                // `faq_miss=true` (no viable hit), a malformed result map
                // (cannot read the flag), or any external-failure dispatch
                // ALL reset the tracker to null. This is the same
                // principle A1 follows ("failures don't enter the cache,
                // legitimate retries re-dispatch") applied to the
                // paraphrase shape: after a non-viable result, a fresh
                // search is warranted and the gate must not block it.
                if ("search_knowledge".equals(toolName)) {
                    if (result != null && result.isSuccess()) {
                        Object data = result.getData();
                        boolean viableHit = false;
                        if (data instanceof Map<?, ?> dataMap) {
                            Object faqMissFlag = dataMap.get("faq_miss");
                            if (faqMissFlag instanceof Boolean fm) {
                                viableHit = !fm;
                            }
                        }
                        lastSearchKnowledgeViableHit = viableHit ? recorded : null;
                    } else {
                        lastSearchKnowledgeViableHit = null;
                    }
                }

                // 6c-ter. Sprint 071 / S-Auto-15 (workstream A) — refresh /
                // reset the PERSISTED cross-turn standing viable-hit marker on
                // every fresh search_knowledge dispatch. SEPARATE from the A3
                // per-run tracker above (which lives only one turn). Mirrors
                // the same faq_miss-state rules but writes to the BotSession
                // columns so the marker carries to the NEXT turn:
                //   * faq_miss=false (viable hit) → capture the standing hit
                //     for the CURRENT active_use_case (UC marker + serialized
                //     payload) and RESET the budget to 0 — the legitimate
                //     first cross-turn refinement on the next turn is then
                //     allowed before any suppression.
                //   * faq_miss=true (genuine miss), a malformed result map, or
                //     any external-failure dispatch → CLEAR the standing state
                //     (a genuine miss warrants fresh searching; never freeze
                //     the gate on a non-viable result).
                if (SEARCH_KNOWLEDGE_TOOL.equals(toolName) && session != null) {
                    boolean viableHit = false;
                    if (result != null && result.isSuccess()) {
                        Object data = result.getData();
                        if (data instanceof Map<?, ?> dataMap) {
                            Object faqMissFlag = dataMap.get("faq_miss");
                            if (faqMissFlag instanceof Boolean fm) {
                                viableHit = !fm;
                            }
                        }
                    }
                    if (viableHit) {
                        captureCrossTurnStandingHit(session, result.getData());
                    } else {
                        clearCrossTurnStandingHit(session,
                                "non-viable search_knowledge result (genuine "
                                        + "miss / malformed / failure)");
                    }
                }

                // Sprint 12 §N0 — stamp terminal-evidence summary on the
                // session as soon as a record_outcome dispatch lands so
                // every run-loop exit path (final answer, escalate,
                // max_steps, error) leaves the trace evidence consistent.
                // The kernel post-loop helpers
                // ({@code emitResolveDispositionEvent},
                // {@code emitRecordOutcomeGuardEvent}) read these slots,
                // and the projection surfaces them under
                // {@code terminal_evidence}.
                if (RECORD_OUTCOME_TOOL.equals(toolName) && session != null) {
                    session.setRecordOutcomeAttempted(Boolean.TRUE);
                    if (result != null && result.isSuccess()) {
                        session.setRecordOutcomeSucceeded(Boolean.TRUE);
                    } else if (session.getRecordOutcomeSucceeded() == null) {
                        session.setRecordOutcomeSucceeded(Boolean.FALSE);
                    }
                    // Sprint 071 / S-Auto-15 (workstream A) — record_outcome is
                    // a resolution/terminal signal; CLEAR the cross-turn
                    // standing marker so it never carries past a recorded
                    // outcome.
                    clearCrossTurnStandingHit(session, "record_outcome dispatch");
                }

                // 6c. Accumulate result so the next iteration sees it.
                if (result != null && result.isSuccess()) {
                    accumulatedToolResults.put(toolName, result.getData());
                } else if (result != null) {
                    Map<String, Object> errorWrap = new LinkedHashMap<>();
                    errorWrap.put("error", result.getErrorMessage());
                    accumulatedToolResults.put(toolName, errorWrap);
                }

                // 6d. Handover short-circuits the loop only when the
                // dispatch actually SUCCEEDED. Sprint 9 §O1 — terminal-state
                // honesty: a failed request_handover (e.g. malformed payload,
                // SalesforceService failure) must not be treated as a
                // successful terminal escalation; the error is surfaced in
                // accumulated_tool_results above so the LLM can retry within
                // the remaining maxToolSteps. If the loop later hits MAX_STEPS
                // without a successful handover, PhaseEvaluator maps that to
                // ESCALATE/turn_budget_exhausted via the canonical mapping —
                // no synthetic success is ever stamped here.
                if (HANDOVER_TOOL.equals(toolName)
                        && result != null && result.isSuccess()) {
                    handoverRequested = true;
                    handoverReason = extractHandoverReason(call);
                    // Sprint 071 / S-Auto-15 (workstream A) — escalation /
                    // handover is a terminal signal; CLEAR the cross-turn
                    // standing marker.
                    if (session != null) {
                        clearCrossTurnStandingHit(session,
                                "successful request_handover dispatch");
                    }
                }

                // 6e. Sprint 8.1 §M3 — DISCOVER deterministic phase
                // boundary. When classify_use_case successfully commits a
                // non-blank active_use_case on a DISCOVER plan, return
                // USE_CASE_IDENTIFIED immediately. ControlKernel performs
                // a single bounded same-turn replan into RESOLVE for the
                // newly committed UC instead of letting the loop run out
                // its remaining DISCOVER steps (which the legacy mapper
                // mis-maps to ESCALATE / faq_miss_threshold_exceeded even
                // when search_knowledge returned hits and classify
                // succeeded).
                if (CLASSIFY_USE_CASE_TOOL.equals(toolName)
                        && DISCOVER_PHASE.equalsIgnoreCase(plan.phase())
                        && result != null && result.isSuccess()
                        && session != null
                        && session.getActiveUseCase() != null
                        && !session.getActiveUseCase().isBlank()) {
                    log.info(
                            "AgentRunLoop §M3: classify_use_case committed active_use_case={} "
                                    + "on DISCOVER plan; returning USE_CASE_IDENTIFIED for same-turn "
                                    + "replan into RESOLVE",
                            session.getActiveUseCase());
                    return AgentRunResult.useCaseIdentified(
                            session.getActiveUseCase(),
                            llmEvents, toolEvents,
                            lastProjection, lastLlmRawResponse, llmCallRecords);
                }
            }

            if (handoverRequested) {
                // AgentRunResult.escalate stores escalationReason; the
                // PhaseEvaluator.interpretRunResult mapping applies the
                // customer-facing escalation template. The LLM's userMsg, if
                // any, is captured in the last LlmCallEvent.responseSummary.
                return AgentRunResult.escalate(handoverReason, llmEvents, toolEvents,
                        lastProjection, lastLlmRawResponse, llmCallRecords);
            }
        }

        // Loop exhausted. Sprint 8.2 §M0b — preserve the last LLM raw
        // response on MAX_STEPS so the trace UI does not falsely render
        // "no LLM call for this turn" when multiple successful LLM calls
        // happened before the loop hit maxToolSteps. Terminal outcome and
        // PhaseEvaluator MAX_STEPS mapping are unchanged.
        log.warn("AgentRunLoop hit max_tool_steps={} without terminal outcome", maxSteps);
        return AgentRunResult.maxSteps(llmEvents, toolEvents, lastProjection,
                lastLlmRawResponse, llmCallRecords);
    }

    /**
     * Sprint 7.1 §J0 — merge partial intake field values supplied by the
     * current user turn (and seeded by the form context) into
     * {@link BotSession#getIntakeFields()} so the next
     * {@code intake_state} projection reflects them. No-op for non-intake
     * UCs and for UCs the {@link IntakeFieldExtractor} has no heuristics
     * for. Never overwrites a field already present in the session — the
     * LLM-supplied {@code intake_fields} (via
     * {@link #persistInlineIntakeFields}) always win.
     */
    void mergePartialIntakeFromContext(BotSession session, PhasePlan plan, String userMessage) {
        if (session == null || plan == null) return;
        String uc = plan.useCase();
        if (!IntakeFieldsRegistry.isIntakeUseCase(uc)) return;
        if (!IntakeFieldExtractor.handlesUc(uc)) return;

        Map<String, String> existing = IntakeFieldsRegistry.parseCollectedFields(
                objectMapper, session.getIntakeFields());
        Map<String, String> merged = IntakeFieldExtractor.mergeForUc(
                uc, existing, userMessage, session.getFormContext(), objectMapper);
        if (merged == existing || merged.equals(existing)) {
            return;
        }
        try {
            session.setIntakeFields(objectMapper.writeValueAsString(merged));
            log.debug("AgentRunLoop merged partial intake fields for UC {}: collected={}",
                    uc, merged.keySet());
        } catch (Exception ex) {
            log.warn("AgentRunLoop failed to persist partial intake_fields: {}", ex.getMessage());
        }
    }

    /**
     * Sprint 7 §I2 — persist the LLM-supplied {@code intake_fields} payload
     * (when present on a {@code request_handover} call) back to
     * {@link BotSession#getIntakeFields()} so subsequent turns and the
     * {@link IntakeFieldsRegistry#intakeComplete} predicate can see the
     * collected values. Field names are normalised to canonical form via
     * {@link IntakeFieldsRegistry#canonicalFieldName}; only non-blank
     * values are merged. Tolerant — never throws on malformed input.
     */
    void persistInlineIntakeFields(BotSession session, ToolCall call) {
        if (session == null || call == null || call.getArguments() == null) {
            return;
        }
        Object raw = call.getArguments().get("intake_fields");
        if (!(raw instanceof Map<?, ?> rawMap) || rawMap.isEmpty()) {
            return;
        }
        // Sprint 080 / R7 (path β): the merge + persist body was extracted to
        // {@link IntakeFieldsMerger} so the new update_intake_fields tool
        // persists through the same byte-equivalent code path. This method
        // keeps the request_handover-specific `intake_fields` key extraction
        // and null/empty guard; the shared helper owns parse → merge → persist.
        @SuppressWarnings("unchecked")
        Map<String, ?> incoming = (Map<String, ?>) rawMap;
        IntakeFieldsMerger.merge(session, incoming, objectMapper);
    }

    /**
     * Extract the {@code escalation_reason} argument from a
     * {@code request_handover} tool call (null-safe). Used to compute the
     * outer-loop {@code handoverReason} captured for the run result.
     */
    private String extractHandoverReason(ToolCall call) {
        if (call == null || call.getArguments() == null) return null;
        Object reason = call.getArguments().get("escalation_reason");
        return reason == null ? null : reason.toString();
    }

    /**
     * Sprint 071 / S-Auto-15 (workstream A) — is there ANY drift signal this
     * turn? Returns true (gate DISABLED) when {@code driftType} is non-null
     * and not the literal {@code "none"}, OR when {@code activeUseCase}
     * differs from {@code previousActiveUseCase} (a UC change). Both
     * transient slots are populated by
     * {@code ControlKernel.applyRerouteDecision} (it runs BEFORE
     * {@code AgentRunLoop.run}), so they are reliably available at gate time.
     * GUARDRAIL 0: drift always wins — any drift signal disables the gate,
     * no exception. Null/blank driftType + equal UCs = no drift.
     */
    static boolean isDriftThisTurn(BotSession session) {
        if (session == null) return true; // fail-open: no session → never suppress
        String drift = session.getDriftType();
        if (drift != null && !drift.isBlank() && !"none".equalsIgnoreCase(drift.trim())) {
            return true;
        }
        String active = session.getActiveUseCase();
        String previous = session.getPreviousActiveUseCase();
        // A null previous (first turn after intake) is NOT a drift signal by
        // itself; the standing-hit UC equality check (condition 1/3) already
        // requires a prior viable hit, which cannot exist on turn 0. Only a
        // CONCRETE change (both non-null and unequal) counts as a UC change.
        return active != null && previous != null && !active.equals(previous);
    }

    /**
     * Sprint 071 / S-Auto-15 (workstream A) — capture the PERSISTED standing
     * cross-turn viable-hit marker for the CURRENT active use case: the UC
     * marker, the serialized result payload (served on a future suppression),
     * and a budget RESET to 0 (so the next turn's first cross-turn refinement
     * is always allowed). Tolerant: a serialization failure clears the marker
     * (fail-open — never freeze the gate on payload we cannot store/serve).
     */
    private void captureCrossTurnStandingHit(BotSession session, Object resultData) {
        if (session == null) return;
        String uc = session.getActiveUseCase();
        if (uc == null || uc.isBlank() || resultData == null) {
            clearCrossTurnStandingHit(session,
                    "viable hit but no committed active_use_case / null payload");
            return;
        }
        String payloadJson;
        try {
            payloadJson = objectMapper.writeValueAsString(resultData);
        } catch (Exception ex) {
            log.warn("AgentRunLoop cross-turn: failed to serialize standing "
                    + "viable-hit payload; clearing marker (fail-open): {}",
                    ex.getMessage());
            clearCrossTurnStandingHit(session, "payload serialization failure");
            return;
        }
        // Preserve the budget when a standing marker ALREADY stands for the
        // SAME UC: a same-UC viable refinement just refreshes the payload; it
        // must NOT reset the cardinality budget, otherwise every cross-turn
        // re-search that itself returns a viable hit would perpetually reset
        // the budget to 0 and the gate could never reach its suppression
        // branch (the storm beyond the first refinement would never be
        // caught). The budget is reset to 0 ONLY on a genuinely NEW capture
        // (no prior marker, or a marker for a DIFFERENT UC) — that is the
        // point at which "the first cross-turn refinement is always allowed"
        // begins. GUARDRAIL 0 budget semantics are preserved: it still counts
        // ALLOWED cross-turn searches since the standing hit.
        Integer existingBudget = session.getCrossTurnSearchAllowedSinceHit();
        boolean sameUcMarkerStands = uc.equals(session.getCrossTurnFaqHitUseCase());
        int budgetToWrite = (sameUcMarkerStands && existingBudget != null)
                ? existingBudget
                : 0;
        session.setCrossTurnFaqHitUseCase(uc);
        session.setCrossTurnFaqHitPayload(payloadJson);
        session.setCrossTurnSearchAllowedSinceHit(budgetToWrite);
        log.info("AgentRunLoop cross-turn: captured standing viable hit for "
                + "UC={} at turn {} (budget={}, sameUcMarkerStands={})",
                uc, session.getTotalBotTurns(), budgetToWrite, sameUcMarkerStands);
    }

    /**
     * Sprint 071 / S-Auto-15 (workstream A) — CLEAR the PERSISTED standing
     * cross-turn viable-hit marker (UC marker + payload + budget). Called on
     * every reset event: drift / UC change (run start), genuine miss /
     * malformed / failed search, resolution (record_outcome), and escalation
     * (request_handover). No-op when already clear (avoids churn).
     */
    private void clearCrossTurnStandingHit(BotSession session, String reason) {
        if (session == null) return;
        boolean wasSet = session.getCrossTurnFaqHitUseCase() != null
                || session.getCrossTurnFaqHitPayload() != null
                || (session.getCrossTurnSearchAllowedSinceHit() != null
                        && session.getCrossTurnSearchAllowedSinceHit() != 0);
        if (!wasSet) return;
        session.setCrossTurnFaqHitUseCase(null);
        session.setCrossTurnFaqHitPayload(null);
        session.setCrossTurnSearchAllowedSinceHit(0);
        log.info("AgentRunLoop cross-turn: cleared standing viable-hit marker "
                + "({})", reason);
    }

    /**
     * Sprint 071 / S-Auto-15 (workstream A) — deserialize the persisted
     * standing payload JSON back into a Map to SERVE on suppression. Returns
     * {@code null} on any failure → the caller FAILS OPEN (falls through to a
     * normal dispatch; never suppresses on evidence it cannot serve).
     */
    private Object deserializeStandingPayload(String payloadJson) {
        if (payloadJson == null || payloadJson.isBlank()) return null;
        try {
            return objectMapper.readValue(payloadJson,
                    new com.fasterxml.jackson.core.type.TypeReference<
                            java.util.Map<String, Object>>() {});
        } catch (Exception ex) {
            log.warn("AgentRunLoop cross-turn: failed to deserialize standing "
                    + "payload; failing open (will re-dispatch): {}",
                    ex.getMessage());
            return null;
        }
    }

    // Sprint 39 (NEW M2) — the three Sprint 6 §G2 / Sprint 7 §I2 / Sprint 11
    // §M1 shouldRejectXxx static predicate methods (and the local
    // sessionCollected helper) are REMOVED per Sprint 37 freeze decision (g)
    // §8. Their semantics are migrated into typed handlers on
    // SkillGuardrailDispatcher (handleFaqMissHandoverRequiresResolveAttempt,
    // handleIntakeCompleteRequired, handlePrematureResolveOutcomeGuard
    // — the last preserves the Sprint 11 / 11.1 / 12 frozen
    // ResolveDispositionEvaluator delegation per
    // runtime_freeze_and_risk_policy.md §1.1 #3, only relocating the
    // invocation site).

    /**
     * Sprint 8.1 follow-up — heuristic clarification detection on a
     * no-tool-calls user_message. Mirrors the existing
     * {@link PhaseEvaluator#isClarificationTurn(com.gumtree.csagent.model.ParsedAction)}
     * predicate (kept duplicated here so the AgentRunLoop does not
     * depend on PhaseEvaluator). True when the message ends with
     * {@code '?'} or contains a recognised clarifying phrase
     * ("could you", "can you tell", "what is", "which", "do you have").
     *
     * <p>When this returns {@code true}, the loop emits
     * {@link com.gumtree.csagent.model.TerminalOutcome#CLARIFICATION_NEEDED}
     * so {@link PhaseEvaluator#interpretRunResult} keeps the session in
     * the current phase rather than chaining RESOLVE → CONFIRM → CLOSE
     * mid-conversation.
     */
    static boolean isClarificationMessage(String userMessage) {
        if (userMessage == null || userMessage.isBlank()) return false;
        String trimmed = userMessage.trim();
        if (trimmed.endsWith("?")) return true;
        String lower = trimmed.toLowerCase(java.util.Locale.ROOT);
        return lower.contains("could you")
                || lower.contains("can you tell")
                || lower.contains("what is")
                || lower.contains("which")
                || lower.contains("do you have");
    }

    /**
     * R2.a #3 — structural predicate for a DISCOVER free-text clarification
     * turn that increments {@code session.clarificationCount}. ALL four
     * STRUCTURAL criteria must hold; zero content / similarity matching:
     *
     * <ol>
     *   <li>phase is DISCOVER;</li>
     *   <li>the turn produced no tool calls;</li>
     *   <li>the turn did not commit an active use case;</li>
     *   <li>the turn produced a non-empty BOT free-text reply.</li>
     * </ol>
     *
     * <p><strong>{@code botReply} is the bot's outgoing reply</strong>
     * ({@code action.getUserMessage()} — the LLM's {@code user_message}
     * field), NOT the customer's incoming {@code userMessage} run() argument.
     * The predicate takes only the bot reply, so a customer message can never
     * be misclassified as a bot clarification (field-confusion regression
     * guard).
     */
    static boolean isDiscoverFreeTextClarification(String phase,
                                                   boolean hasToolCalls,
                                                   boolean ucCommittedThisTurn,
                                                   String botReply) {
        return DISCOVER_PHASE.equalsIgnoreCase(phase)
                && !hasToolCalls
                && !ucCommittedThisTurn
                && botReply != null && !botReply.isBlank();
    }
}
