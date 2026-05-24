-- Sprint 51 / M5 S2 — per-invocation full-fidelity trace.
-- One row per LLM call inside an AgentRunLoop step boundary. The existing
-- bot_turns.{projected_context, llm_raw_response} columns keep the final-step
-- value unchanged (backward-compat); this table carries the full per-step
-- record so the admin trace can show every invocation. Frozen
-- llm_call_log table (V9) keeps its 500-char cheap summary — distinct surface.
CREATE TABLE IF NOT EXISTS bot_turn_llm_calls (
    id                  BIGSERIAL PRIMARY KEY,
    bot_turn_id         TEXT NOT NULL REFERENCES bot_turns(turn_id) ON DELETE CASCADE,
    step_index          INT NOT NULL,
    call_type           TEXT NOT NULL,
    model               TEXT,
    llm_raw_response    TEXT,
    projected_context   JSONB,
    tool_calls          JSONB,
    latency_ms          INT,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_bot_turn_llm_calls_turn
    ON bot_turn_llm_calls(bot_turn_id, step_index);
