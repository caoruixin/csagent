-- Step 3b: per-session runtime error counter for ERROR retry threshold.
-- PhaseEvaluator.interpretRunResult increments this on TerminalOutcome.ERROR
-- and only escalates once the count reaches >= 2; non-ERROR outcomes reset
-- it to 0 to preserve "consecutive errors in same phase" semantics.
ALTER TABLE bot_sessions ADD COLUMN runtime_error_count INTEGER NOT NULL DEFAULT 0;
