-- Authorized by phase0_normative_freeze.md §0.6 (deviation 2026-05-01).
-- Action layer removed; semantic actions are derived from tool_calls JSON.
ALTER TABLE bot_turns DROP COLUMN IF EXISTS action_selected;
ALTER TABLE bot_turns DROP COLUMN IF EXISTS action_parameters;
