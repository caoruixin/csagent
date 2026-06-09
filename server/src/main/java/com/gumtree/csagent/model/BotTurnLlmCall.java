package com.gumtree.csagent.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * Sprint 51 / M5 S2 — per-invocation full-fidelity record. One row per LLM
 * call inside an {@link com.gumtree.csagent.service.runtime.AgentRunLoop} step
 * boundary. Carries the full untruncated raw response + that step's projection,
 * which the existing single-column {@link BotTurn#getLlmRawResponse()} /
 * {@link BotTurn#getProjectedContext()} overwrite each step. Backward-compat:
 * {@code BotTurn}'s columns keep the final-step value unchanged; this table is
 * additive observation.
 *
 * <p>Distinct from {@link LlmCallLog} (V9): {@code LlmCallLog} stores a 500-char
 * cheap summary per call, used by the frozen
 * {@code GET /sessions/{id}/llm-calls} endpoint (eval-harness contract). This
 * table stores the full payload + the per-step projection that
 * {@code llm_call_log} does not carry.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "bot_turn_llm_calls")
public class BotTurnLlmCall {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /** FK → {@link BotTurn#getTurnId()}. */
    @Column(name = "bot_turn_id", nullable = false)
    private String botTurnId;

    /** 0-based loop step index inside the owning {@code AgentRunLoop.run(...)}. */
    @Column(name = "step_index", nullable = false)
    private Integer stepIndex;

    /**
     * One of {@code chat} / {@code routing} / {@code rerank} — same vocabulary
     * as {@link LlmCallLog#getCallType()}. Routing / rerank calls are labelled
     * distinctly per the M5 S2 fence (never lumped as {@code chat}).
     */
    @Column(name = "call_type", nullable = false)
    private String callType;

    @Column(name = "model")
    private String model;

    /** Full / untruncated LLM raw response content for this invocation. */
    @Column(name = "llm_raw_response")
    private String llmRawResponse;

    /**
     * The projection built for THIS invocation (JSON). Distinct from
     * {@link BotTurn#getProjectedContext()} which retains only the final step.
     */
    @Column(name = "projected_context", columnDefinition = "jsonb")
    private String projectedContext;

    /** JSON array of tool calls observed at this step (LLM-issued). */
    @Column(name = "tool_calls", columnDefinition = "jsonb")
    private String toolCalls;

    @Column(name = "latency_ms")
    private Integer latencyMs;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}
