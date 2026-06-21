# Codex findings (LIVE)

This is the **live** Codex review ledger. Per-sub-sprint §4.1 verdicts and the
sprint-/milestone-close §4.2 headers accumulate here during the active milestone,
then are archived to `docs/milestones/M<N>_codex-review.md` at milestone close and
this file is reset to this scaffold.

**Conventions:**
- Per-PR / per-sub-sprint verdict (§4.1): `approve` | `approve with downgrade-to-signal
  follow-up` | `reject as semantic hardcode` | `needs human architecture decision`.
- Sprint-/milestone-close header (§4.2):
  ```
  ## (Sprint|Milestone) Review Decision — <id>
  decision: pass | fix_required | out_of_scope_review
  blocking_count: <number>
  summary: <one paragraph>
  ```

**Last archived:** M-Auto-9 → `docs/milestones/M-Auto-9_codex-review.md` (2026-06-21;
milestone-shared `pass`/0; bundled the carried M-Auto-7 sub-sprint verdicts S-Auto-38 /
S-Auto-39 / S-Auto-40 / S-Y1.7 / S-Y1.5b/c/d which had no milestone-shared review at
M-Auto-7's NO-KEEP close).

---

## Sub-sprint Design-Review Decision — S-Auto-47 (Sprint 099 / M-Auto-10 WP2)

verdict: **APPROVE**
type: **design review** (evidentiary soundness + scope discipline) — NOT the §4.1
anti-hardcode kernel (the sub-sprint ships no semantic/runtime/eval surface; design-only).
blocking_count: 0
reviewed: `9882b93e` (docs-only) + recorded WP2 traces + read-only `csagent` DB
dispatch: deliver-dispatched `codex exec --sandbox read-only` (model gpt-5.5,
reasoning_effort high); session `019ee91d-de5d-7b92-a15b-8a79545660d7`. Recorded verbatim
below.
required_changes: none. One **minor, explicitly-not-required** wording note (see §6/note).

> **Design Review**
>
> I verified commit `9882b93e` is docs-only and adds only:
>
> - `docs/proposals/m-auto-10-wp2-closure-path-canonical-decision.md`
> - `docs/sprints/sprint-099-handoff.md`
>
> No runtime/eval/prompt/simulator files are changed.
>
> **Assessment**
>
> 1. The PRIMARY traces do prove the explicit path can land end-to-end. In
>    `eval_interactive/results/20260621-015324/results.json`, a `CONFIRM` phase turn calls
>    `record_outcome` with `success=true`; the DB confirms session
>    `323dc3cd-480e-4477-95e6-8e70114a8602` reached `current_phase=CLOSE`,
>    `handling_state=CLOSED`, `containment_outcome=resolved`, with final
>    `bot_turns.phase_after=CLOSE`. `20260621-015210` also has `record_outcome` success in
>    `CONFIRM`.
>
> 2. The companion is structurally preempted. Across all 11
>    `cs_uc_a_loaded_listing_resolvable` draws, `stop_reason=goal_achieved`, `user_state`
>    reaches `satisfied`, and `per_turn_trace[].phase_plan.phase` never reaches `CONFIRM`.
>    `session_runner.py:259-269` breaks immediately on `goal_status=="achieved"`, before
>    another bot turn can run. One nuance: resolved companion `014438` persists
>    `phase_after=CONFIRM`, but there is still no subsequent CONFIRM `phase_plan` bot turn;
>    that supports, rather than weakens, the preemption analysis.
>
> 3. Grounding-gated closure is safer than the explicit path on the observed evidence. The
>    PRIMARY `record_outcome` successes in `015210` and `015324` occur while user signals
>    never reach `satisfied`; `015324` is scored as a false resolve against
>    `user_state=UNRESOLVED`. By contrast, `ControlKernel.isResolvedSuccessTerminal` is
>    gated by `FINAL_ANSWER`, resolved disposition, non-empty `articlesShown`, and no prior
>    containment value.
>
> 4. Mechanism (iv), a runtime closure defect, is reasonably ruled out for this decision.
>    `ResolveDispositionEvaluator.java:160-177` permits resolve recording in `CONFIRM/CLOSE`;
>    `confirm.yaml` exposes `record_outcome`; `PhaseEvaluator.java:807-818,882-889,929-951`
>    explains why first grounded RESOLVE turns do not immediately become explicit close
>    turns; and the PRIMARY proves the explicit path works once CONFIRM is actually reached.
>
> 5. Route (b) is correctly rejected. Relaxing the premature guard or forcing a CONFIRM turn
>    would alter the phase/runtime contract to satisfy a trace shape that the simulator
>    preempts by design. The evidence supports correcting the expectation, not changing the
>    phase machine.
>
> 6. Route (a) follow-ups are bounded correctly. The companion CaseSpec still literally
>    requires `record_outcome` in `expected_tool_sequence`, and
>    `test_sprint_097_uc_a_resolvable_companion.py:120-128` pins that. But
>    `test_satisfied_resolve_lands_pass` constructs a trace with
>    `containment_outcome="resolved"` and satisfied user state without requiring a
>    `record_outcome` tool call, and passes. Retiring/annotating the literal sequence is
>    therefore not a scored `conditional_outcome_acceptance` bar relaxation. The proposed
>    docs annotations are scope-correct; no runtime, simulator, scoring, prompt, or
>    conditional-acceptance change is being proposed.
>
> 7. OQ-S99.1 is correctly framed as Gate-D robustness / measurement completeness, not
>    proven mechanism (iv). The three companion empty outcomes are all satisfied and
>    simulator-preempted: `014322` and `015420` have premature RESOLVE `record_outcome`
>    rejections; `015038` has no `record_outcome` and remains a Gate-D-internal miss. That
>    is worth tracking, but it is not evidence that the explicit closure path is defective.
>
> Minor wording note only: the docs' shorthand "`014438` (UC-A, CONFIRM-promoted)" can be
> misread as "a CONFIRM phase_plan turn occurred." The trace shows no CONFIRM bot turn; DB
> state shows persisted `phase_after=CONFIRM`. Because the docs also explicitly state
> companion `reachedCONFIRM=0/11`, I do not consider this a required change.
>
> The design verdict is evidentially sound and scope-disciplined. Route (a) is supported:
> grounding-gated `isResolvedSuccessTerminal` is the canonical one-shot satisfiable closure
> path; explicit `record_outcome→CONFIRM→CLOSE` remains a sound multi-turn path; route (b)
> is correctly rejected.
>
> APPROVE

This satisfies the Sprint 099 / S-Auto-47 design-review close gate (verdict doc
`docs/proposals/m-auto-10-wp2-closure-path-canonical-decision.md`). Human sign-off remains
the only pending gate before M-Auto-10 closes (route (a) — no WP3) after the §5.6 rerun.

---

_(no active findings — next milestone's reviews accumulate below)_
