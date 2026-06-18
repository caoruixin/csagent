# Sprint 093 / S-Auto-39 — Codex review (archived)

> Per-sub-sprint anti-hardcode review for the RESOLVE→CONFIRM/CLOSE deadlock
> corrective. Archived verbatim from `docs/codex-findings.md` at the S-Auto-39
> sub-sprint close (2026-06-18). The live `docs/codex-findings.md` retains this
> verdict until M-Auto-7 milestone close (then folds into `docs/milestones/`).

## Sub-sprint Review Decision — S-Auto-39 (Sprint 093, RESOLVE→CONFIRM/CLOSE deadlock corrective)
decision: pass
blocking_count: 0
summary: Commit 033abaee implements the RESOLVE→CONFIRM repair as a structural `skill_state` transition keyed on prior persisted turn evidence (`phase_after == RESOLVE` plus non-empty `source_ids`) and the existing `ANSWERED_SUBTASK` disposition; it does not inspect the new user message, add per-case/runtime UC exceptions, expand enums, weaken `shouldRejectPrematureResolveOutcome`, or lower `record_outcome`. The Sprint 9 §O1 retry exclusion is narrowly limited to premature-guard-only `record_outcome` rejection with prior grounding, while genuine record failures, slot-request turns, first grounded answers, and explicit escalation/distress paths remain outside the promotion.

Reviewed: runtime diff `033abaee` (`PhaseEvaluator.java`, `ControlKernel.java`, `BotSession.java`) + `Sprint93ResolveConfirmDeadlockTest.java`, against the Step-0 attribution (`docs/diagnostics/sprint-093-step0-escalation-attribution.md`), the §4 bounded real-LLM run, and the §5 byte-identity cross-check (`docs/sprints/sprint-093-handoff.md`). Read-only `codex exec` (model_reasoning_effort=high).

Kernel verdict: `approve`.

Per-question (§4.1 nine-question kernel):
- Q1 Pass: no new keyword/regex/per-UC semantic decision; the new branch is structural phase/history state plus existing disposition.
- Q2 N/A: no Tier-0 exception is needed; the change preserves the premature guard and trace-contract intent.
- Q3 Pass: projecting a soft signal would not repair the Java phase-machine deadlock; this belongs in `skill_state`.
- Q4 Pass: no visible eval phrase, CaseSpec id, or trace-specific text is encoded into runtime.
- Q5 Pass: LLM ownership is not shrunk; Java only permits a confirmable phase after a prior grounded RESOLVE answer.
- Q6 Pass: no prompt if-else or prompt change is introduced.
- Q7 Pass: tool schema, grounding floor, and premature guard are preserved; genuine `record_outcome` failures still retry.
- Q8 Pass: tests cover target deadlock, first-answer negative, DISCOVER-search negative, slot-request anti-误杀, genuine failure retry, guard truth table, and handoff reports neighbor/genuine-escalation validation.
- Q9 Pass: not framed as temporary; no rollback/sunset required.

Note (non-blocking, recorded for the human/deliver): the dev handoff §5 surfaces OQ-S93.1 — the structural deadlock is repaired (CONFIRM reachable) but `record_outcome(resolve)` lands in 0/42 bounded-run draws (resolved reached only via the grounding terminal stamp), with a downstream CONFIRM-phase record-vs-handover issue and a `user_requested` reason-mislabel. This is a follow-up scope item, not a defect in this diff. Pilot stays HELD.

## Close disposition

Per-sub-sprint Codex gate satisfied (`pass` / blocking_count 0 / `approve`). Consumed at the S-Auto-39 sub-sprint close. The milestone-shared §4.3 review still fires at M-Auto-7 close over the cumulative range. OQ-S93.1 carried as the next pre-pilot blocker (research-first); pilot + objective-alignment annotation stay HELD.
