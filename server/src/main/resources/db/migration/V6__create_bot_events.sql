CREATE TABLE bot_events (
    event_id        TEXT PRIMARY KEY,
    session_id      TEXT NOT NULL REFERENCES bot_sessions(session_id),
    event_type      TEXT NOT NULL,
    turn_index      INT,
    payload         JSONB,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_events_session ON bot_events(session_id, created_at);
CREATE INDEX idx_events_type ON bot_events(event_type);
