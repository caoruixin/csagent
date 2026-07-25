package com.gumtree.csagent.model;

import java.util.List;
import java.util.Optional;

/**
 * Result of a single {@code AgentRunLoop.run(...)} invocation. Per Phase 3
 * §3.3.3 (v9 — D16). Aggregates every LLM call, tool dispatch, and outbound
 * message produced during the model↔tool exchange so {@code ControlKernel}
 * can flatten them into existing tables ({@code llm_call_log},
 * {@code bot_turns.tool_calls}, {@code bot_turns.bot_response}).
 *
 * <p>Collection components are stored as {@link List#copyOf} so callers can
 * accumulate into mutable lists during the loop and hand them off without
 * worrying about post-construction mutation.
 *
 * <p>{@link #escalationReason()} is only populated when
 * {@link #terminalOutcome()} is {@link TerminalOutcome#ESCALATE}; for other
 * outcomes it is {@link Optional#empty()}.
 *
 * <p>{@link #lastProjection()} is the JSON projection produced for the final
 * LLM iteration. {@code ControlKernel.recordRunResult()} persists it to
 * {@code bot_turns.projected_context} so trace UIs can replay the agent run.
 * It is {@code null} for {@link #error(String)} results.
 *
 * <p>{@link #lastLlmRawResponse()} is the verbatim {@code LlmResponse.content}
 * from the LLM call that produced the terminal outcome (final answer or
 * escalation). Persisted to {@code bot_turns.llm_raw_response}. {@code null}
 * for max-steps / error results.
 *
 * <p>Sprint 51 / M5 S2 — additive {@link #llmCallRecords()} list carries the
 * full per-step record (untruncated raw response + that step's projection +
 * tool_calls + latency + step_index + call_type) so the admin trace can render
 * every invocation, not just the final one. Existing fields' meaning is
 * unchanged: {@code lastProjection} / {@code lastLlmRawResponse} still carry
 * the final-step value; the records list is observation-only side data.
 */
public record AgentRunResult(
        List<AgentMessage> messages,
        List<ToolEvent> toolEvents,
        List<LlmCallEvent> llmEvents,
        TerminalOutcome terminalOutcome,
        String finalUserMessage,
        Optional<String> escalationReason,
        String lastProjection,
        String lastLlmRawResponse,
        List<LlmCallRecord> llmCallRecords
) {

    public AgentRunResult {
        messages = messages == null ? List.of() : List.copyOf(messages);
        toolEvents = toolEvents == null ? List.of() : List.copyOf(toolEvents);
        llmEvents = llmEvents == null ? List.of() : List.copyOf(llmEvents);
        escalationReason = escalationReason == null ? Optional.empty() : escalationReason;
        llmCallRecords = llmCallRecords == null ? List.of() : List.copyOf(llmCallRecords);
    }

    /**
     * Sprint 51 — backward-compat 8-arg canonical constructor: callers that
     * predate the {@code llmCallRecords} field (existing tests, prior
     * factories) keep compiling and produce an empty records list.
     */
    public AgentRunResult(
            List<AgentMessage> messages,
            List<ToolEvent> toolEvents,
            List<LlmCallEvent> llmEvents,
            TerminalOutcome terminalOutcome,
            String finalUserMessage,
            Optional<String> escalationReason,
            String lastProjection,
            String lastLlmRawResponse
    ) {
        this(messages, toolEvents, llmEvents, terminalOutcome, finalUserMessage,
                escalationReason, lastProjection, lastLlmRawResponse, List.of());
    }

    public static AgentRunResult finalAnswer(String userMessage,
                                              List<LlmCallEvent> llmEvents,
                                              List<ToolEvent> toolEvents) {
        return finalAnswer(userMessage, llmEvents, toolEvents, null, null);
    }

    public static AgentRunResult finalAnswer(String userMessage,
                                              List<LlmCallEvent> llmEvents,
                                              List<ToolEvent> toolEvents,
                                              String lastProjection,
                                              String lastLlmRawResponse) {
        return finalAnswer(userMessage, llmEvents, toolEvents, lastProjection,
                lastLlmRawResponse, List.of());
    }

    public static AgentRunResult finalAnswer(String userMessage,
                                              List<LlmCallEvent> llmEvents,
                                              List<ToolEvent> toolEvents,
                                              String lastProjection,
                                              String lastLlmRawResponse,
                                              List<LlmCallRecord> llmCallRecords) {
        return new AgentRunResult(
                List.of(AgentMessage.finalMessage(userMessage)),
                toolEvents,
                llmEvents,
                TerminalOutcome.FINAL_ANSWER,
                userMessage,
                Optional.empty(),
                lastProjection,
                lastLlmRawResponse,
                llmCallRecords
        );
    }

    public static AgentRunResult maxSteps(List<LlmCallEvent> llmEvents,
                                           List<ToolEvent> toolEvents) {
        return maxSteps(llmEvents, toolEvents, null, null);
    }

    public static AgentRunResult maxSteps(List<LlmCallEvent> llmEvents,
                                           List<ToolEvent> toolEvents,
                                           String lastProjection) {
        return maxSteps(llmEvents, toolEvents, lastProjection, null);
    }

    /**
     * Sprint 8.2 §M0b — observability honesty overload. Preserves
     * {@code lastLlmRawResponse} on a {@link TerminalOutcome#MAX_STEPS}
     * result so that {@code bot_turns.llm_raw_response} captures the verbatim
     * content of the last successful LLM call. Without this, the trace UI
     * misleadingly renders MAX_STEPS turns as having had no LLM call at all
     * even when several real LLM calls occurred before the loop exhausted
     * its tool-step budget. Terminal outcome and PhaseEvaluator MAX_STEPS
     * mapping are unchanged.
     */
    public static AgentRunResult maxSteps(List<LlmCallEvent> llmEvents,
                                           List<ToolEvent> toolEvents,
                                           String lastProjection,
                                           String lastLlmRawResponse) {
        return maxSteps(llmEvents, toolEvents, lastProjection, lastLlmRawResponse, List.of());
    }

    public static AgentRunResult maxSteps(List<LlmCallEvent> llmEvents,
                                           List<ToolEvent> toolEvents,
                                           String lastProjection,
                                           String lastLlmRawResponse,
                                           List<LlmCallRecord> llmCallRecords) {
        return new AgentRunResult(
                List.of(),
                toolEvents,
                llmEvents,
                TerminalOutcome.MAX_STEPS,
                null,
                Optional.empty(),
                lastProjection,
                lastLlmRawResponse,
                llmCallRecords
        );
    }

    public static AgentRunResult escalate(String reason,
                                           List<LlmCallEvent> llmEvents,
                                           List<ToolEvent> toolEvents) {
        return escalate(reason, llmEvents, toolEvents, null, null);
    }

    public static AgentRunResult escalate(String reason,
                                           List<LlmCallEvent> llmEvents,
                                           List<ToolEvent> toolEvents,
                                           String lastProjection,
                                           String lastLlmRawResponse) {
        return escalate(reason, llmEvents, toolEvents, lastProjection,
                lastLlmRawResponse, List.of());
    }

    public static AgentRunResult escalate(String reason,
                                           List<LlmCallEvent> llmEvents,
                                           List<ToolEvent> toolEvents,
                                           String lastProjection,
                                           String lastLlmRawResponse,
                                           List<LlmCallRecord> llmCallRecords) {
        return new AgentRunResult(
                List.of(),
                toolEvents,
                llmEvents,
                TerminalOutcome.ESCALATE,
                null,
                Optional.ofNullable(reason),
                lastProjection,
                lastLlmRawResponse,
                llmCallRecords
        );
    }

    /**
     * Sprint 8.1 follow-up (2026-05-06) — clarification factory. Used by
     * the AgentRunLoop when the LLM emits no tool calls AND the
     * {@code user_message} looks like a clarifying question (ends with
     * {@code '?'} or carries a clarifying phrase). Surfaces as
     * {@link TerminalOutcome#CLARIFICATION_NEEDED} so
     * {@code PhaseEvaluator.interpretRunResult} keeps the session in the
     * current phase rather than chaining to CONFIRM / CLOSE.
     */
    public static AgentRunResult clarification(String userMessage,
                                                List<LlmCallEvent> llmEvents,
                                                List<ToolEvent> toolEvents,
                                                String lastProjection,
                                                String lastLlmRawResponse) {
        return clarification(userMessage, llmEvents, toolEvents, lastProjection,
                lastLlmRawResponse, List.of());
    }

    public static AgentRunResult clarification(String userMessage,
                                                List<LlmCallEvent> llmEvents,
                                                List<ToolEvent> toolEvents,
                                                String lastProjection,
                                                String lastLlmRawResponse,
                                                List<LlmCallRecord> llmCallRecords) {
        return new AgentRunResult(
                List.of(AgentMessage.finalMessage(userMessage)),
                toolEvents,
                llmEvents,
                TerminalOutcome.CLARIFICATION_NEEDED,
                userMessage,
                Optional.empty(),
                lastProjection,
                lastLlmRawResponse,
                llmCallRecords
        );
    }

    public static AgentRunResult error(String message) {
        return new AgentRunResult(
                List.of(),
                List.of(),
                List.of(),
                TerminalOutcome.ERROR,
                null,
                Optional.ofNullable(message),
                null,
                null,
                List.of()
        );
    }

    /**
     * Sprint 8.1 §M2 — terminal outcome for an LLM call that could not
     * complete inside the per-turn wall-clock budget. Preserves the
     * accumulated {@code llmEvents} / {@code toolEvents} so the trace
     * still records what happened before the deadline fired.
     */
    public static AgentRunResult deadlineExceeded(String message,
                                                   List<LlmCallEvent> llmEvents,
                                                   List<ToolEvent> toolEvents,
                                                   String lastProjection) {
        return deadlineExceeded(message, llmEvents, toolEvents, lastProjection, List.of());
    }

    public static AgentRunResult deadlineExceeded(String message,
                                                   List<LlmCallEvent> llmEvents,
                                                   List<ToolEvent> toolEvents,
                                                   String lastProjection,
                                                   List<LlmCallRecord> llmCallRecords) {
        return new AgentRunResult(
                List.of(),
                toolEvents,
                llmEvents,
                TerminalOutcome.DEADLINE_EXCEEDED,
                null,
                Optional.ofNullable(message),
                lastProjection,
                null,
                llmCallRecords
        );
    }

    /**
     * Sprint 8.1 §M2 — terminal outcome for an LLM call that failed for an
     * infrastructure reason (transport / 5xx / 429 / 401 / 403 after retry
     * exhaustion). Same shape as {@link #deadlineExceeded}; preserved so
     * downstream gating can distinguish the two surfaces.
     */
    public static AgentRunResult llmUnavailable(String message,
                                                 List<LlmCallEvent> llmEvents,
                                                 List<ToolEvent> toolEvents,
                                                 String lastProjection) {
        return llmUnavailable(message, llmEvents, toolEvents, lastProjection, List.of());
    }

    public static AgentRunResult llmUnavailable(String message,
                                                 List<LlmCallEvent> llmEvents,
                                                 List<ToolEvent> toolEvents,
                                                 String lastProjection,
                                                 List<LlmCallRecord> llmCallRecords) {
        return new AgentRunResult(
                List.of(),
                toolEvents,
                llmEvents,
                TerminalOutcome.LLM_UNAVAILABLE,
                null,
                Optional.ofNullable(message),
                lastProjection,
                null,
                llmCallRecords
        );
    }

    /**
     * Sprint 8.1 §M3 — terminal outcome for a successful DISCOVER
     * classification. The LLM called {@code classify_use_case} with a
     * confident UC and the tool persisted it on the session. Carries the
     * committed UC string in {@code finalUserMessage} purely as a marker
     * (it is NOT customer-facing — the same-turn RESOLVE replan owns the
     * actual user reply). Returned by {@code AgentRunLoopImpl} immediately
     * after the successful tool dispatch instead of continuing the DISCOVER
     * plan to {@code maxToolSteps}, which would otherwise mis-map to
     * ESCALATE / {@code faq_miss_threshold_exceeded}.
     */
    public static AgentRunResult useCaseIdentified(String committedUc,
                                                    List<LlmCallEvent> llmEvents,
                                                    List<ToolEvent> toolEvents,
                                                    String lastProjection,
                                                    String lastLlmRawResponse) {
        return useCaseIdentified(committedUc, llmEvents, toolEvents, lastProjection,
                lastLlmRawResponse, List.of());
    }

    public static AgentRunResult useCaseIdentified(String committedUc,
                                                    List<LlmCallEvent> llmEvents,
                                                    List<ToolEvent> toolEvents,
                                                    String lastProjection,
                                                    String lastLlmRawResponse,
                                                    List<LlmCallRecord> llmCallRecords) {
        return new AgentRunResult(
                List.of(),
                toolEvents,
                llmEvents,
                TerminalOutcome.USE_CASE_IDENTIFIED,
                committedUc,
                Optional.empty(),
                lastProjection,
                lastLlmRawResponse,
                llmCallRecords
        );
    }

    /**
     * Sprint 103 / WS-6-A — terminal outcome for an honoured mid-session
     * re-route. The LLM called {@code propose_reroute} from a RESOLVE or
     * CONFIRM plan and the runtime moved {@code session.activeUseCase} to
     * the target UC. Carries the target UC in {@code finalUserMessage} as a
     * marker; it is NOT customer-facing — the same-turn replan into the
     * target UC's RESOLVE Skill owns the actual reply.
     */
    public static AgentRunResult useCaseRerouted(String targetUc,
                                                  List<LlmCallEvent> llmEvents,
                                                  List<ToolEvent> toolEvents,
                                                  String lastProjection,
                                                  String lastLlmRawResponse) {
        return useCaseRerouted(targetUc, llmEvents, toolEvents, lastProjection,
                lastLlmRawResponse, List.of());
    }

    public static AgentRunResult useCaseRerouted(String targetUc,
                                                  List<LlmCallEvent> llmEvents,
                                                  List<ToolEvent> toolEvents,
                                                  String lastProjection,
                                                  String lastLlmRawResponse,
                                                  List<LlmCallRecord> llmCallRecords) {
        return new AgentRunResult(
                List.of(),
                toolEvents,
                llmEvents,
                TerminalOutcome.USE_CASE_REROUTED,
                targetUc,
                Optional.empty(),
                lastProjection,
                lastLlmRawResponse,
                llmCallRecords
        );
    }
}
