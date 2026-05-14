-- Sprint 24 Track A: per-session consecutive-deadline counter for the
-- cross-turn slow-LLM placeholder coalesce. PhaseEvaluator.interpretRunResult
-- increments this on TerminalOutcome.DEADLINE_EXCEEDED and only swaps the
-- placeholder for a distinct honest next-step message once the count reaches
-- >= 2; non-DEADLINE_EXCEEDED outcomes reset it to 0 to preserve
-- "consecutive deadlines" semantics. Mirrors V12 runtime_error_count.
ALTER TABLE bot_sessions ADD COLUMN consecutive_deadline_count INTEGER NOT NULL DEFAULT 0;
