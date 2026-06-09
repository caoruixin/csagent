package com.gumtree.csagent.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Parsed LLM response carrier under the OpenAI-style single-layer tool-use contract:
 * {@code {user_message, reasoning, tool_calls:[{name, arguments}]}}.
 *
 * <p>See {@code docs/phase0_normative_freeze.md} §0.6 (Deviation 2026-05-01) and
 * {@code docs/phase3_detailed_technical_design.md} §3.3.3 for the contract.
 *
 * <p>Task #10 removed the legacy synthetic {@code action} / {@code parameters}
 * fields. Callers must inspect {@link #toolCalls} and {@link #userMessage} to
 * derive control flow semantics (escalation / clarification / answer / retrieval).
 *
 * <p>Class name retained as {@code ParsedAction} to minimise churn across callers
 * and tests; the renamed-suggestion {@code LlmToolResponse} was deferred — see
 * Task #10 report.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParsedAction {

    /** Canonical: list of tool calls requested by the LLM (may be empty, never null after parsing). */
    private List<ToolCall> toolCalls;

    /** Customer-facing message (may be empty when tool_calls is non-empty). */
    private String userMessage;

    /** Internal reasoning, not shown to the customer. */
    private String reasoning;

    /**
     * Structural provenance flag: true when {@link #userMessage} was
     * runtime-synthesised by {@code ActionParser} at the null-turn fallback
     * ({@code tool_calls} empty AND {@code user_message} blank), NOT authored
     * by the LLM. The R2.a DISCOVER clarification counter excludes synthesised
     * turns so a runtime placeholder cannot burn the clarification budget.
     * The parse-failure handover apology ({@code buildFallback}) leaves this
     * false — it is a different case. Primitive default {@code false}.
     */
    private boolean userMessageSynthesised;
}
