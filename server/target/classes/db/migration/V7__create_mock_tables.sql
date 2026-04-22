-- Demo-only tables (dropped in dev/prod migration)
CREATE TABLE mock_cases (
    case_id         TEXT PRIMARY KEY,
    session_id      TEXT REFERENCES bot_sessions(session_id),
    use_case_id     TEXT NOT NULL,
    subject         TEXT,
    description     TEXT,
    contact_email   TEXT,
    ad_id           TEXT,
    status          TEXT NOT NULL DEFAULT 'New',
    queue_name      TEXT,
    intake_fields   JSONB,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE mock_handover_log (
    log_id          TEXT PRIMARY KEY,
    session_id      TEXT REFERENCES bot_sessions(session_id),
    handover_payload JSONB NOT NULL,
    transfer_result TEXT NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_handover_session ON mock_handover_log(session_id);
