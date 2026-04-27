CREATE TABLE IF NOT EXISTS llm_call_log (
    id                  BIGSERIAL PRIMARY KEY,
    session_id          TEXT NOT NULL,
    turn_index          INT,
    call_type           TEXT NOT NULL,
    model               TEXT NOT NULL,
    prompt_tokens       INT,
    completion_tokens   INT,
    latency_ms          INT NOT NULL,
    request_summary     TEXT,
    response_summary    TEXT,
    success             BOOLEAN NOT NULL DEFAULT TRUE,
    error_message       TEXT,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_llm_call_session ON llm_call_log(session_id, turn_index);
CREATE INDEX IF NOT EXISTS idx_llm_call_type ON llm_call_log(call_type);
