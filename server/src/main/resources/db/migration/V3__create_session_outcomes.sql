CREATE TABLE session_outcomes (
    session_id          TEXT PRIMARY KEY REFERENCES bot_sessions(session_id),
    outcome             TEXT NOT NULL,
    use_case_id         TEXT,
    escalation_reason   TEXT,
    articles_shown      TEXT[],
    total_turns         INT,
    total_latency_ms    BIGINT,
    trace_metadata      JSONB,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
