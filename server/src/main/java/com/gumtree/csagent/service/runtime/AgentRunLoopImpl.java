package com.gumtree.csagent.service.runtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gumtree.csagent.model.AgentRunResult;
import com.gumtree.csagent.model.BotSession;
import com.gumtree.csagent.model.BotTurn;
import com.gumtree.csagent.model.LlmCallEvent;
import com.gumtree.csagent.model.LlmResponse;
import com.gumtree.csagent.model.ParsedAction;
import com.gumtree.csagent.model.PhasePlan;
import com.gumtree.csagent.model.ToolCall;
import com.gumtree.csagent.model.ToolEvent;
import com.gumtree.csagent.service.tools.ToolDispatcher;
import com.gumtree.csagent.service.tools.ToolResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
    private static final String SEARCH_TOOL = "search_knowledge";
    private static final String RESOLVE_TOOL = "resolve_article";

    /**
     * Sprint 6 §G2 — S1 FAQ-grounded-resolve guard.
     *
     * <p>Refuses {@code request_handover(faq_miss_threshold_exceeded)} when
     * {@code search_knowledge} already produced viable evidence
     * ({@code faq_miss=false} with at least one hit) AND
     * {@code resolve_article} has not yet been attempted in the current
     * agent run. Anchors cs_192 (search-not-yet-run / uncited-answer
     * shape, indirectly via the prompt nudge) and cs_259 (search-ran-
     * but-resolve-did-not-complete shape, directly via this predicate).
     *
     * <p>The guard fires only on FAQ-path UCs in RESOLVE; intake,
     * DISCOVER, CONFIRM, CLOSE, and ESCALATE are unaffected. If the
     * search returned no viable hit (faq_miss=true) the
     * {@code faq_miss_threshold_exceeded} handover is allowed through.
     */
    private static final Set<String> FAQ_PATH_UCS = Set.of(
            "UC-A", "UC-B", "UC-C", "UC-D", "UC-E", "UC-F", "UC-FP"
    );

    private static final String FAQ_MISS_REASON = "faq_miss_threshold_exceeded";
    static final String S1_GUARD_REJECT_REASON =
            "s1_resolve_required_before_faq_miss_handover";

    /**
     * Sprint 7 §I2 — intake-complete guard reject reason. Surfaced in
     * {@code accumulated_tool_results.request_handover.error} when the LLM
     * calls {@code request_handover(intake_complete_for_uc_X)} for an
     * intake UC before all required fields have actually been collected.
     */
    static final String INTAKE_COMPLETE_GUARD_REJECT_REASON =
            "intake_required_fields_missing_for_intake_complete";

    private final LlmInvocationService llmInvocation;
    private final ToolDispatcher toolDispatcher;
    private final ContextProjectionBuilder contextProjectionBuilder;
    private final ActionParser actionParser;
    private final ObjectMapper objectMapper;

    public AgentRunLoopImpl(LlmInvocationService llmInvocation,
                            ToolDispatcher toolDispatcher,
                            ContextProjectionBuilder contextProjectionBuilder,
                            ActionParser actionParser,
                            ObjectMapper objectMapper) {
        this.llmInvocation = llmInvocation;
        this.toolDispatcher = toolDispatcher;
        this.contextProjectionBuilder = contextProjectionBuilder;
        this.actionParser = actionParser;
        this.objectMapper = objectMapper;
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
        Map<String, Object> accumulatedToolResults = new LinkedHashMap<>();
        int sequence = 0;
        String lastProjection = null;
        String lastLlmRawResponse = null;

        int maxSteps = Math.max(1, plan.maxToolSteps());

        for (int step = 0; step < maxSteps; step++) {
            // 1. Build plan-aware projection
            String projection;
            try {
                projection = contextProjectionBuilder.build(
                        session, history, plan, userMessage, accumulatedToolResults);
            } catch (Exception ex) {
                log.error("AgentRunLoop projection build failed at step {}: {}",
                        step, ex.getMessage(), ex);
                return AgentRunResult.error("projection_failed: " + ex.getMessage());
            }
            lastProjection = projection;

            // 2. Invoke LLM (LlmInvocationService catches exceptions internally
            //    and returns a safe-escalation response; we still wrap defensively).
            long t0 = System.currentTimeMillis();
            LlmResponse response;
            try {
                response = llmInvocation.invokeChat(projection, userMessage,
                        session.getSessionId(), session.getTotalBotTurns());
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
                return AgentRunResult.finalAnswer(fallbackMessage, llmEvents, toolEvents,
                        lastProjection, lastLlmRawResponse);
            }

            if (action == null) {
                log.warn("AgentRunLoop parser returned null at step {}; treating raw content as final answer",
                        step);
                String fallbackMessage = (content == null || content.isBlank())
                        ? "I'm having trouble processing your request."
                        : content;
                return AgentRunResult.finalAnswer(fallbackMessage, llmEvents, toolEvents,
                        lastProjection, lastLlmRawResponse);
            }

            List<ToolCall> calls = action.getToolCalls();
            String userMsg = action.getUserMessage();

            // 5. No tool calls -> final user message (or clarification)
            if (calls == null || calls.isEmpty()) {
                String finalText = (userMsg == null || userMsg.isBlank())
                        ? "I'm looking into this for you."
                        : userMsg;
                return AgentRunResult.finalAnswer(finalText, llmEvents, toolEvents,
                        lastProjection, lastLlmRawResponse);
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

                // 6a''. Sprint 7 §I2 — intake-complete guard. Refuse
                // request_handover(intake_complete_for_uc_X) when the
                // session's intake_fields are not yet complete for the
                // active UC. The LLM may also supply fields under
                // arguments.intake_fields; merge those into
                // session.intakeFields BEFORE evaluating the guard so a
                // single complete handover call is allowed through.
                if (HANDOVER_TOOL.equals(toolName)
                        && IntakeFieldsRegistry.isIntakeUseCase(plan.useCase())) {
                    persistInlineIntakeFields(session, call);
                    if (shouldRejectIncompleteIntakeHandover(plan, call, session)) {
                        Map<String, String> collected = IntakeFieldsRegistry.parseCollectedFields(
                                objectMapper, session.getIntakeFields());
                        List<String> missing = IntakeFieldsRegistry.fieldsRemaining(
                                plan.useCase(), collected);
                        log.warn(
                                "AgentRunLoop intake-complete guard rejected request_handover for "
                                        + "intake UC '{}' at step {}: required fields missing={}",
                                plan.useCase(), step, missing);
                        ToolEvent rejected = ToolEvent.rejected(step, call,
                                INTAKE_COMPLETE_GUARD_REJECT_REASON);
                        toolEvents.add(new ToolEvent(
                                sequence++, step, rejected.toolName(), rejected.arguments(),
                                rejected.success(), rejected.resultData(), rejected.errorMessage(),
                                rejected.latencyMs()));
                        Map<String, Object> guardWrap = new LinkedHashMap<>();
                        guardWrap.put("error", INTAKE_COMPLETE_GUARD_REJECT_REASON);
                        guardWrap.put("missing_fields", missing);
                        guardWrap.put("hint",
                                "Ask the user for the missing intake fields above, then call "
                                        + "request_handover with arguments.intake_fields populated.");
                        accumulatedToolResults.put(HANDOVER_TOOL, guardWrap);
                        continue;
                    }
                }

                // 6a'. Sprint 6 §G2 — S1 FAQ-grounded-resolve guard.
                // Refuse request_handover(faq_miss_threshold_exceeded) when
                // search_knowledge already returned viable evidence and
                // resolve_article has not yet been attempted. The LLM is
                // nudged toward calling resolve_article on the next loop
                // iteration. Other handover reasons (user_requested,
                // user_distress, out_of_scope, real Tier-2 reasons) pass
                // through unchanged.
                if (HANDOVER_TOOL.equals(toolName)
                        && shouldRejectFaqMissHandover(plan, call, accumulatedToolResults)) {
                    log.warn(
                            "AgentRunLoop S1 guard rejected request_handover(faq_miss_threshold_exceeded) "
                                    + "for FAQ-path UC '{}' at step {}: search_knowledge has viable evidence "
                                    + "but resolve_article has not been attempted yet.",
                            plan.useCase(), step);
                    ToolEvent rejected = ToolEvent.rejected(step, call, S1_GUARD_REJECT_REASON);
                    toolEvents.add(new ToolEvent(
                            sequence++, step, rejected.toolName(), rejected.arguments(),
                            rejected.success(), rejected.resultData(), rejected.errorMessage(),
                            rejected.latencyMs()));
                    // Surface the rejection in accumulated_tool_results so the
                    // next LLM iteration sees the gap and can call resolve_article.
                    Map<String, Object> guardWrap = new LinkedHashMap<>();
                    guardWrap.put("error", S1_GUARD_REJECT_REASON);
                    guardWrap.put("hint",
                            "Call resolve_article for the top search_knowledge hit before "
                                    + "escalating with faq_miss_threshold_exceeded.");
                    accumulatedToolResults.put(HANDOVER_TOOL, guardWrap);
                    continue;
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
                toolEvents.add(new ToolEvent(
                        sequence++, step, base2.toolName(), base2.arguments(),
                        base2.success(), base2.resultData(), base2.errorMessage(),
                        base2.latencyMs()));

                // 6c. Accumulate result so the next iteration sees it.
                if (result != null && result.isSuccess()) {
                    accumulatedToolResults.put(toolName, result.getData());
                } else if (result != null) {
                    Map<String, Object> errorWrap = new LinkedHashMap<>();
                    errorWrap.put("error", result.getErrorMessage());
                    accumulatedToolResults.put(toolName, errorWrap);
                }

                // 6d. Handover short-circuits the loop
                if (HANDOVER_TOOL.equals(toolName)) {
                    handoverRequested = true;
                    handoverReason = extractHandoverReason(call);
                }
            }

            if (handoverRequested) {
                // AgentRunResult.escalate stores escalationReason; the
                // PhaseEvaluator.interpretRunResult mapping applies the
                // customer-facing escalation template. The LLM's userMsg, if
                // any, is captured in the last LlmCallEvent.responseSummary.
                return AgentRunResult.escalate(handoverReason, llmEvents, toolEvents,
                        lastProjection, lastLlmRawResponse);
            }
        }

        // Loop exhausted
        log.warn("AgentRunLoop hit max_tool_steps={} without terminal outcome", maxSteps);
        return AgentRunResult.maxSteps(llmEvents, toolEvents, lastProjection);
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
        Map<String, String> existing = IntakeFieldsRegistry.parseCollectedFields(
                objectMapper, session.getIntakeFields());
        @SuppressWarnings("unchecked")
        Map<String, ?> incoming = (Map<String, ?>) rawMap;
        Map<String, String> merged = IntakeFieldsRegistry.mergeFields(existing, incoming);
        if (merged.equals(existing)) {
            return;
        }
        try {
            session.setIntakeFields(objectMapper.writeValueAsString(merged));
        } catch (Exception ex) {
            log.warn("AgentRunLoop failed to persist inline intake_fields: {}", ex.getMessage());
        }
    }

    /**
     * Sprint 7 §I2 — intake-complete guard predicate.
     *
     * <p>Returns true iff:
     * <ol>
     *   <li>The plan is RESOLVE on an intake-path UC (UC-G/H/I/J/K).</li>
     *   <li>The handover call's escalation_reason is the canonical
     *       {@code intake_complete_for_uc_<g|h|i|j|k>} for the active UC.</li>
     *   <li>{@link IntakeFieldsRegistry#intakeComplete(String, Map)} is
     *       false against the merged session.intakeFields (i.e. at least
     *       one canonical required field is still missing or blank).</li>
     * </ol>
     *
     * <p>Other handover reasons (user_requested, user_distress,
     * incomplete_intake, out_of_scope, real Tier-2 reasons) pass through
     * unchanged.
     */
    static boolean shouldRejectIncompleteIntakeHandover(PhasePlan plan,
                                                        ToolCall call,
                                                        BotSession session) {
        if (plan == null || !"RESOLVE".equals(plan.phase())) {
            return false;
        }
        String activeUc = plan.useCase();
        if (!IntakeFieldsRegistry.isIntakeUseCase(activeUc)) {
            return false;
        }
        if (call == null || call.getArguments() == null) {
            return false;
        }
        Object reasonObj = call.getArguments().get("escalation_reason");
        String reason = reasonObj == null ? null : reasonObj.toString();
        if (reason == null) return false;
        String expected = "intake_complete_for_uc_" + activeUc.substring(activeUc.length() - 1)
                .toLowerCase(java.util.Locale.ENGLISH);
        if (!expected.equals(reason)) {
            return false;
        }
        Map<String, String> collected = sessionCollected(session);
        return !IntakeFieldsRegistry.intakeComplete(activeUc, collected);
    }

    private static Map<String, String> sessionCollected(BotSession session) {
        if (session == null) return Collections.emptyMap();
        // Static helper duplicates the parse logic; the test path constructs
        // its own ObjectMapper, so the predicate must remain static.
        try {
            ObjectMapper m = new ObjectMapper();
            return IntakeFieldsRegistry.parseCollectedFields(m, session.getIntakeFields());
        } catch (Exception ex) {
            return Collections.emptyMap();
        }
    }

    private String extractHandoverReason(ToolCall call) {
        if (call == null || call.getArguments() == null) return null;
        Object reason = call.getArguments().get("escalation_reason");
        return reason == null ? null : reason.toString();
    }

    /**
     * Sprint 6 §G2 — S1 FAQ-grounded-resolve predicate.
     *
     * <p>Returns true iff:
     * <ol>
     *   <li>The plan is RESOLVE on a FAQ-path UC (UC-A / UC-B / UC-C /
     *       UC-D / UC-E / UC-F / UC-FP).</li>
     *   <li>The handover call's escalation_reason is
     *       {@code faq_miss_threshold_exceeded}.</li>
     *   <li>{@code accumulated_tool_results.search_knowledge} contains a
     *       viable hit (faq_miss=false AND at least one hit).</li>
     *   <li>{@code accumulated_tool_results.resolve_article} is empty.</li>
     * </ol>
     *
     * <p>If any condition is unmet, the handover passes through. In
     * particular, when search_knowledge returned no viable hit
     * (faq_miss=true), the LLM is allowed to escalate with
     * faq_miss_threshold_exceeded.
     */
    static boolean shouldRejectFaqMissHandover(
            PhasePlan plan,
            ToolCall call,
            Map<String, Object> accumulatedToolResults) {
        if (plan == null || !"RESOLVE".equals(plan.phase())) {
            return false;
        }
        if (plan.useCase() == null || !FAQ_PATH_UCS.contains(plan.useCase())) {
            return false;
        }
        if (call == null || call.getArguments() == null) {
            return false;
        }
        Object reasonObj = call.getArguments().get("escalation_reason");
        String reason = reasonObj == null ? null : reasonObj.toString();
        if (!FAQ_MISS_REASON.equals(reason)) {
            return false;
        }
        if (accumulatedToolResults == null) {
            return false;
        }
        // Already resolved? Allow through.
        Object resolveData = accumulatedToolResults.get(RESOLVE_TOOL);
        if (resolveData != null && !(resolveData instanceof Map<?, ?> rm && rm.containsKey("error"))) {
            return false;
        }
        // Search not yet run? Allow through (the cs_192 prompt nudge owns
        // search-before-answer; this guard only catches the cs_259 shape).
        Object searchData = accumulatedToolResults.get(SEARCH_TOOL);
        if (!(searchData instanceof Map<?, ?> searchMap)) {
            return false;
        }
        if (searchMap.containsKey("error")) {
            return false;
        }
        Object faqMiss = searchMap.get("faq_miss");
        if (Boolean.TRUE.equals(faqMiss)) {
            return false;
        }
        Object hits = searchMap.get("hits");
        if (!(hits instanceof List<?> hitList) || hitList.isEmpty()) {
            return false;
        }
        return true;
    }
}
