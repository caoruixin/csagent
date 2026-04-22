CREATE TABLE bot_turns (
    turn_id             TEXT PRIMARY KEY,
    session_id          TEXT NOT NULL REFERENCES bot_sessions(session_id),
    turn_index          INT NOT NULL,
    user_message        TEXT,
    projected_context   JSONB,
    llm_raw_response    TEXT,
    action_selected     TEXT,
    action_parameters   JSONB,
    tool_calls          JSONB,
    bot_response        TEXT,
    source_ids          TEXT[],
    phase_before        TEXT,
    phase_after         TEXT,
    active_use_case     TEXT,
    latency_ms          INT,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_turns_session ON bot_turns(session_id, turn_index);
