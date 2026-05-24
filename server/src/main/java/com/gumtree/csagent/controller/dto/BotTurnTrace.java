package com.gumtree.csagent.controller.dto;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.gumtree.csagent.model.BotTurn;
import com.gumtree.csagent.model.BotTurnLlmCall;

import java.util.List;

/**
 * Sprint 51 / M5 S2 — admin trace projection that nests the per-step LLM
 * invocations under each {@link BotTurn}. The base BotTurn fields are emitted
 * inline via {@link JsonUnwrapped} so the existing trace consumers see the
 * same shape as before (final-step single-column llm_raw_response /
 * projected_context preserved); the new {@code llm_calls} array carries the
 * full per-step records from {@code bot_turn_llm_calls}.
 *
 * <p>This DTO is admin/demo-only ({@code /v1/demo/sessions/{id}/trace}); it
 * does NOT touch the frozen
 * {@code GET /v1/demo/sessions/{id}/llm-calls} payload consumed by the eval
 * harness.
 */
public record BotTurnTrace(
        @JsonUnwrapped BotTurn turn,
        List<BotTurnLlmCall> llmCalls
) {

    public BotTurnTrace {
        llmCalls = llmCalls == null ? List.of() : List.copyOf(llmCalls);
    }
}
