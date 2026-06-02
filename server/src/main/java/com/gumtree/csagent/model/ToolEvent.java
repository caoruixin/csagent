package com.gumtree.csagent.model;

import com.gumtree.csagent.service.tools.ToolResult;

import java.util.Map;

/**
 * One tool dispatch inside an {@code AgentRunLoop} iteration. Captures the
 * call request (name + arguments), execution outcome (success / error /
 * rejected-by-plan), the result payload, and latency. Per Phase 3 §3.3.3,
 * every {@link ToolEvent} is later flattened into {@code bot_turns.tool_calls}
 * for the trace UI and replay tooling.
 *
 * <p>{@code stepIndex} is the 0-based position within the loop;
 * {@code sequenceIndex} preserves intra-step ordering when an LLM step
 * requests multiple tools at once (e.g. {@code [get_customer_context,
 * search_knowledge]} in a single response).
 *
 * <p>Sprint 067 / S-Auto-12 (A1 idempotency 回挡): {@code deduplicated}
 * marks an event that was served from the per-run identity cache instead
 * of being re-dispatched (a byte-identical {@code success==true} repeat of
 * an earlier call in the same {@code AgentRunLoop.run(...)}). For such an
 * event the tool was NOT re-executed (no external call / budget);
 * {@code originalAtStep} names the {@code stepIndex} of the original
 * dispatch whose result was reused, and is {@code -1} for every
 * non-deduplicated event. Both fields are observation-only — they annotate
 * the trace and never alter control flow.
 *
 * <p>Sprint 069 / S-Auto-13b (A3 deterministic backstop):
 * {@code paraphraseSuppressed} marks a {@code search_knowledge} re-search
 * inside the same {@code AgentRunLoop.run(...)} that was suppressed by the
 * {@code faq_miss}-state gate because a prior {@code search_knowledge} in
 * the same run already returned a viable hit ({@code faq_miss=false}). The
 * tool was NOT re-executed; the prior viable-hit result is reused.
 * {@code faqHitAtStep} names the {@code stepIndex} of that prior viable-hit
 * dispatch, and is {@code -1} for every event the gate did not suppress.
 * Distinct from {@code deduplicated} on purpose: A1 keys on a byte-identical
 * arguments hash (same query string), this gate keys on the {@code faq_miss}
 * RESULT state (different query string, same intent — paraphrase). A given
 * event carries one annotation or the other, never both.
 *
 * <p>Sprint 071 / S-Auto-15 (M-Auto-3, workstream A):
 * {@code crossTurnParaphraseSuppressed} marks a {@code search_knowledge}
 * re-search that the NEW, SEPARATE, BotSession-scoped cross-turn gate
 * suppressed because a standing viable hit ({@code faq_miss=false}) for the
 * SAME un-drifted use case was captured in a PRIOR bot turn and the budget-1
 * cross-turn refinement was already spent. The tool was NOT re-executed; the
 * standing payload is reused. {@code crossTurnHitAtTurn} names the
 * {@code total_bot_turns} value at which the standing hit was captured, and
 * is {@code -1} for every event the cross-turn gate did not suppress.
 * Distinct from {@code paraphraseSuppressed} (the within-turn A3 gate, which
 * keys on a prior viable hit in the SAME run/turn): a given event carries at
 * most one of {@code deduplicated} / {@code paraphraseSuppressed} /
 * {@code crossTurnParaphraseSuppressed}.
 */
public record ToolEvent(
        int sequenceIndex,
        int stepIndex,
        String toolName,
        Map<String, Object> arguments,
        boolean success,
        Object resultData,
        String errorMessage,
        long latencyMs,
        boolean deduplicated,
        int originalAtStep,
        boolean paraphraseSuppressed,
        int faqHitAtStep,
        boolean crossTurnParaphraseSuppressed,
        int crossTurnHitAtTurn
) {

    public ToolEvent {
        arguments = arguments == null ? Map.of() : Map.copyOf(arguments);
    }

    /**
     * Backward-compatible 8-arg constructor (pre-Sprint-067 shape) for the
     * many call sites and tests that construct a normal, non-deduplicated,
     * non-paraphrase-suppressed event. Delegates to the canonical
     * constructor with both annotation flags false and both step indices
     * {@code -1}.
     */
    public ToolEvent(int sequenceIndex, int stepIndex, String toolName,
                     Map<String, Object> arguments, boolean success,
                     Object resultData, String errorMessage, long latencyMs) {
        this(sequenceIndex, stepIndex, toolName, arguments, success, resultData,
                errorMessage, latencyMs, false, -1, false, -1, false, -1);
    }

    /**
     * Backward-compatible 10-arg constructor (Sprint-067 dedup shape) for
     * the A1 dispatch site that materializes an event annotated only with
     * {@code deduplicated} / {@code originalAtStep}. Delegates to the
     * canonical constructor with {@code paraphraseSuppressed=false} and
     * {@code faqHitAtStep=-1}.
     */
    public ToolEvent(int sequenceIndex, int stepIndex, String toolName,
                     Map<String, Object> arguments, boolean success,
                     Object resultData, String errorMessage, long latencyMs,
                     boolean deduplicated, int originalAtStep) {
        this(sequenceIndex, stepIndex, toolName, arguments, success, resultData,
                errorMessage, latencyMs, deduplicated, originalAtStep, false, -1,
                false, -1);
    }

    /**
     * Backward-compatible 12-arg constructor (Sprint-069 within-turn A3
     * paraphrase-suppressed shape) for the A3 dispatch site that
     * materializes an event annotated only with {@code paraphraseSuppressed}
     * / {@code faqHitAtStep}. Delegates to the canonical constructor with
     * {@code crossTurnParaphraseSuppressed=false} and
     * {@code crossTurnHitAtTurn=-1}.
     */
    public ToolEvent(int sequenceIndex, int stepIndex, String toolName,
                     Map<String, Object> arguments, boolean success,
                     Object resultData, String errorMessage, long latencyMs,
                     boolean deduplicated, int originalAtStep,
                     boolean paraphraseSuppressed, int faqHitAtStep) {
        this(sequenceIndex, stepIndex, toolName, arguments, success, resultData,
                errorMessage, latencyMs, deduplicated, originalAtStep,
                paraphraseSuppressed, faqHitAtStep, false, -1);
    }

    /**
     * Build an event from a successful (or tool-side failed) dispatch. The
     * supplied {@code latency} is preferred over {@link ToolResult#getLatencyMs()}
     * because it is measured at the loop boundary (matches how the loop in
     * D16.B will time things).
     */
    public static ToolEvent of(int step, ToolCall call, ToolResult result, long latency) {
        boolean ok = result != null && result.isSuccess();
        Object data = result == null ? null : result.getData();
        String err = result == null ? null : result.getErrorMessage();
        return new ToolEvent(
                step,
                step,
                call == null ? null : call.getName(),
                call == null ? null : call.getArguments(),
                ok,
                data,
                err,
                latency
        );
    }

    /**
     * Build an event for a tool call that was rejected by
     * {@code ToolDispatcher.validateAgainstPlan(...)} before dispatch. Latency
     * is zero because the tool was never invoked.
     */
    public static ToolEvent rejected(int step, ToolCall call, String reason) {
        return new ToolEvent(
                step,
                step,
                call == null ? null : call.getName(),
                call == null ? null : call.getArguments(),
                false,
                null,
                reason,
                0L
        );
    }

    /**
     * Sprint 067 / S-Auto-12 — build an event for a tool call that was
     * served from the per-run identity cache instead of being re-dispatched
     * (a byte-identical {@code success==true} repeat). The tool was not
     * re-executed, so {@code latencyMs} is zero; {@code resultData} is the
     * cached payload from the original dispatch and {@code originalAtStep}
     * is that dispatch's {@code stepIndex}. {@code success} is true because
     * only successful results ever enter the cache.
     */
    public static ToolEvent deduplicated(int step, ToolCall call,
                                         Object cachedResultData, int originalAtStep) {
        return new ToolEvent(
                step,
                step,
                call == null ? null : call.getName(),
                call == null ? null : call.getArguments(),
                true,
                cachedResultData,
                null,
                0L,
                true,
                originalAtStep
        );
    }

    /**
     * Sprint 069 / S-Auto-13b — build an event for a {@code search_knowledge}
     * re-search that the {@code faq_miss}-state gate suppressed inside the
     * same run because a prior {@code search_knowledge} this run already
     * returned a viable hit ({@code faq_miss=false}). The tool was not
     * re-executed, so {@code latencyMs} is zero; {@code resultData} is the
     * cached payload from that prior viable-hit dispatch and
     * {@code faqHitAtStep} is that dispatch's {@code stepIndex}.
     * {@code success} is true because only viable hits ever feed the gate.
     * Distinct from {@link #deduplicated(int, ToolCall, Object, int)} on
     * purpose: A1 catches byte-identical repeats (same query string), this
     * gate catches paraphrases of the same intent (different query string,
     * same {@code faq_miss=false} result state).
     */
    public static ToolEvent paraphraseSuppressed(int step, ToolCall call,
                                                 Object cachedResultData,
                                                 int faqHitAtStep) {
        return new ToolEvent(
                step,
                step,
                call == null ? null : call.getName(),
                call == null ? null : call.getArguments(),
                true,
                cachedResultData,
                null,
                0L,
                false,
                -1,
                true,
                faqHitAtStep
        );
    }

    /**
     * Sprint 071 / S-Auto-15 — build an event for a {@code search_knowledge}
     * re-search that the NEW cross-turn gate suppressed because a standing
     * viable hit ({@code faq_miss=false}) for the SAME un-drifted use case
     * was captured in a PRIOR bot turn and the budget-1 cross-turn
     * refinement was already spent. The tool was not re-executed, so
     * {@code latencyMs} is zero; {@code resultData} is the standing payload
     * served back to the LLM and {@code crossTurnHitAtTurn} is the
     * {@code total_bot_turns} value at which the standing hit was captured.
     * {@code success} is true because only viable hits ever feed the gate.
     * Distinct from {@link #paraphraseSuppressed(int, ToolCall, Object, int)}
     * (within-turn) and {@link #deduplicated(int, ToolCall, Object, int)}
     * (byte-identical) — a given event carries at most one annotation.
     */
    public static ToolEvent crossTurnParaphraseSuppressed(
            int step, ToolCall call, Object standingResultData,
            int crossTurnHitAtTurn) {
        return new ToolEvent(
                step,
                step,
                call == null ? null : call.getName(),
                call == null ? null : call.getArguments(),
                true,
                standingResultData,
                null,
                0L,
                false,
                -1,
                false,
                -1,
                true,
                crossTurnHitAtTurn
        );
    }
}
