-- Sprint 31 — Option β projection-data field.
--
-- Captures the intake-time alternate-UC snapshot
-- ({@code RoutingResult.ambiguousCandidates}) on the AMBIGUOUS routing
-- branch so the per-turn projection can surface
-- {@code alternate_candidate_use_cases} to the LLM as a soft signal.
-- See:
--   - docs/proposals/alternate_uc_signal_data_source_design.md §5
--   - docs/sprints/sprint-031-objective.md §6
--
-- Null is the valid "no snapshot" state (ROUTED / OUT_OF_SCOPE intake
-- paths and pre-Sprint-31 sessions); no backfill required.

ALTER TABLE bot_sessions
    ADD COLUMN IF NOT EXISTS intake_ambiguous_candidates text[];
