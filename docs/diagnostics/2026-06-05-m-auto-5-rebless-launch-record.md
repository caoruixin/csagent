---
title: 2026-06-05 M-Auto-5 authoritative re-bless launch record (evidence generation; NOT M-Auto-5 close)
doc_tier: diagnostic
status: diagnostic
implementation_status: historical
source_of_truth: this file (launch record only — outcome lives in `eval_interactive/results/m-auto-5-baseline-20260604-simfixed/`)
last_reviewed: 2026-06-05
review_cadence: ad hoc
supersedes: []
superseded_by: null
notes: >
  Single-purpose record. Captures the exact launch conditions for the authoritative
  M-Auto-5 multi-suite re-bless on the simulator-fixed corpus, so the paired-
  evidence review + milestone-shared Codex that follow can verify provenance and
  reproducibility. This document is the launch precondition snapshot, NOT the
  evidence artifact. The evidence artifact is the re-blessed dir on disk.

  This is NOT the M-Auto-5 close. M-Auto-5 close = paired-evidence review +
  milestone-shared Codex + `baseline_dir` pointer move + archive sweeps; it
  happens AFTER this run completes and the review passes. Do not interpret a
  successful re-bless as a milestone close on its own.
---

# 2026-06-05 — M-Auto-5 authoritative re-bless launch record

## 1. Framing — what this is and what it is not

**This is**: the human-launched authoritative multi-suite re-bless on the
simulator-fixed corpus. It generates the **evidence** that the M-Auto-5
milestone-close review will read.

**This is not**: the M-Auto-5 milestone close. The close happens after
this run completes and the paired-evidence + Codex review pass. The
`baseline_dir` pointer is **NOT** moved by the act of running this; the
deliver-agent moves it only after the review classifies the run as
acceptable.

The act of recording this launch-context doc here, ahead of the run, is
deliberate: if the run fails partway, this file is the authoritative
record of the launch conditions to reproduce against.

## 2. Launch parameters

| Field | Value |
|---|---|
| Branch | `auto-loop-branch` |
| HEAD SHA at launch | `1bc77c153ddf11c4647c6658dc0c911a563ac153` (short `1bc77c1`) |
| Working tree | MUST be clean before launch (see §3 preconditions) |
| Bot code base | byte-identical to the S-Auto-21 dev build (`45618df`); no `server/` files changed since |
| Simulator code base | `eval_interactive/eval_interactive/simulator/user_simulator.py` at `1bc77c1` — includes the S-Auto-21 role-map fix + per-turn re-anchor + negative-form Forbidden block + D1/D2/D3 drift guard + 3-attempt retry + `SimulatorDriftError` escape |
| Eval scoring base | S-Auto-19 + S-Auto-20 eval-side corrections (`hard_checks.py` + `composite.py` etc.); autoloop 5-file scoring SHA-locked set untouched |
| Output dir | `eval_interactive/results/m-auto-5-baseline-20260604-simfixed` (fresh dated dir; old baselines RETAINED) |
| Sampling | `--n 7` (matches S-Auto-17 draw depth; deterministic per-attempt at `simulator_temperature=0.0`) |
| `baseline_dir` config | UNCHANGED — stays `m-auto-4-baseline-20260604` until post-review pointer move |

### Exact command

```bash
cd autoloop && uv run python scripts/rebless_baseline.py \
    --n 7 \
    --out-dir ../eval_interactive/results/m-auto-5-baseline-20260604-simfixed
```

Source: copied verbatim from `docs/sprints/sprint-076-handoff.md` §6
(the dev-authored re-bless-ready command).

## 3. Preconditions (verify each before launch)

1. **Clean working tree** — `git status` shows no staged or unstaged
   changes. Per `project_autoloop_dirty_index_hazard`, a dirty index
   poisons per-exp commits and is the historical OQ-S65.1 root cause.
   Any in-progress governance / unrelated edits must be committed or
   stashed before launch.
2. **HEAD = `1bc77c1` or later** — the bookkeeping commit that resolves
   OQ-S76.judge-zero into the chronic R-item.
3. **Backend booted fresh** — per
   `feedback_restart_backend_before_eyeball`, `mvn spring-boot:run` has
   no hot-reload, so a stale backend masks any in-flight runtime change.
   Run:

   ```bash
   cd server && mvn -o spring-boot:run -Dspring-boot.run.profiles=local
   # then in another shell:
   curl -s localhost:8080/actuator/health   # expect status:UP
   ```

   The bot code is untouched between `45618df` (S-Auto-21 dev) and
   `1bc77c1` (deliver bookkeeping), so re-using a backend booted from
   either SHA is safe. If unsure, reboot.
4. **Postgres + Redis up** — Postgres for session storage; Redis for
   bot-internal state. Bot LLM creds in repo-root `.env.local`;
   simulator LLM creds in `autoloop/.env.local`.
5. **Mac kept awake** — per `feedback_long_llm_run_no_sleep`, a
   multi-hour real-LLM run that spans a sleep is uncertifiable; resumed
   draws may exhibit provider-side state changes. Wrap with:

   ```bash
   caffeinate -dimsu uv run python scripts/rebless_baseline.py …
   ```

   (or run `caffeinate -dimsu &` for the duration of the run).
6. **Disk space** — multi-suite n=7 produces 3 suites × n attempts ×
   per-case trace+results JSON; ~hundreds of MB. Confirm
   `eval_interactive/results/` has headroom.

## 4. Forensic-baseline policy

Three pre-existing baseline dirs MUST be retained and treated as
forensic-only — do NOT consume their stability classifications, pass
rates, or judge scores as input to any downstream sprint:

- `eval_interactive/results/m-auto-4-baseline-20260604/` — the live
  `baseline_dir` pointed at this; correct measurement *before* the
  M-Auto-5 corrections. Retained as the pre-fix comparator for the
  paired-evidence verdict-distribution review.
- `eval_interactive/results/m-auto-5-baseline-20260604/` — the
  S-Auto-19 re-bless scratch. Forensic-only: it was rendered against
  the buggy simulator AND the S-Auto-19 runtime stamp was 100 % inert
  (per S-Auto-20 Step-0 diagnosis).
- `eval_interactive/results/m-auto-5-baseline-20260605/` — the human
  let this finish per the 2026-06-04 forensic-only direction. Rendered
  against the buggy simulator. Do NOT consume its stability
  classifications.

The new dir produced by this launch (`m-auto-5-baseline-20260604-simfixed/`)
is the authoritative target. The `baseline_dir` config pointer is moved
to it ONLY after the deliver+human paired-evidence review + milestone-
shared Codex pass.

## 5. Expected anomalies (interpret, do not treat as gaming)

- **`suspect_baseline_manipulation` will likely fire.** The corrected
  measurement legitimately shifts the verdict distribution across all
  three layers in play:
  - **Eval-read column (S-Auto-19)**: `accumulated_tool_results` cross-
    turn union; `intake_fields_collected` dict-key acceptance;
    first-party PII relaxation; `trace_minimum` terminal-disposition-
    aware; `source_citation_present` session-accumulated.
  - **Runtime-stamp column (S-Auto-20)**: `ControlKernel.
    isResolvedSuccessTerminal` broadened to also accept
    `ANSWERED_SUBTASK` with grounding; `loop_detected` removed from
    `_VALID_TERMINAL_STOP_REASONS`.
  - **Input column (S-Auto-21)**: simulator role-map inversion fixed +
    drift guard; 0/40 contamination on the focused re-render +
    852→0 across the pre-fix corpus.

  Read the flag as **"the simulator-fix expected distribution shift"**,
  not as gaming. Codex §4.1 question on "did any structural fix widen
  what counts as success?" is the appropriate check — the structural
  fixes corrected HOW the trace is read / WHO produced the input, not
  WHAT counts as a bot success.
- **Judge layer excluded from paired-evidence review.**
  `mean_judge=0.0` is byte-identical 0.0 across all four bracketing
  runs (m-auto-4 / m-auto-5-0604 / m-auto-5-0605 / simfixed
  20260604-152103). Per OQ-S76.judge-zero resolution 2026-06-05 (route
  c, collapsed into the chronic
  `R-eval-interactive-judge-score-never-populated` R-item), the canonical
  signal for paired-evidence attribution is **composite + outcome + L1
  + L2 `failure_tags`**. Do not attempt to read meaning into
  `mean_judge` deltas between runs.
- **Some `goal_achieved` cases that previously vacuous-passed (0/0/0)
  should now produce real composite+outcome scores.** Per S-Auto-20
  Step-0 diagnosis, the simfixed run will exhibit 51 `goal_achieved`
  draws stamped `containment_outcome="resolved"` (vs 0 in the prior
  scratch). Real verdicts may pass OR fail; the absence of vacuous
  passes is the structural correctness signal.

## 6. Post-run procedure (do NOT move the pointer until review passes)

1. **Confirm the run completed** — check `out-dir/_rebless_report.json`
   for the run-level summary; per-suite `out-dir/<suite>/aggregated.json`
   for completeness. If the run was sleep-spanned or aborted, kill +
   re-launch on a clean tree, do NOT bless a partial run.
2. **Deliver + human paired-evidence review** (§5.6 primary gate per
   `docs/current/process/badcase-lifecycle.md`):
   - Read 12 curated bad-case traces.
   - Classify each PASS / FAIL / IMPROVING.
   - Verdict-distribution shift vs `m-auto-4-baseline-20260604` must
     be explainable by the three columns above; no unexplained verdict
     flip.
   - Judge layer **excluded** (see §5 above); canonical signal =
     composite + outcome + L1 + L2 `failure_tags`.
3. **Milestone-shared Codex review** (§4.1 nine-question anti-hardcode
   kernel) over the M-Auto-5 cumulative commit range. Author the
   review prompt at `compact/M-Auto-5-review-prompt.md` per the deliver-
   agent §Review prompt convention.
4. **If both gates pass**: deliver-agent moves `config.fitness.
   baseline_dir` from `m-auto-4-baseline-20260604` to
   `m-auto-5-baseline-20260604-simfixed`, flips
   `docs/current_eval_baseline.md` `implementation_status` to
   `current`, and executes the standard milestone-close archive sweep
   (objective + codex-review → `docs/milestones/M-Auto-5_*`;
   `docs/codex-findings.md` reset; §1 truncation per
   `doc_governance.md` retention rule; §2 archive index row; action_bank
   §7.1 retention sweep).
5. **If either gate fails**: do NOT move the pointer. Surface findings
   to the human; classify as fix-iteration (open a fix sub-sprint) /
   out-of-scope (rewrite the Codex review) / in-flight downgrade
   (replan M-Auto-5). Per deliver-agent role §Milestone close.

## 7. Explicit non-gating follow-ups (NOT preconditions for this re-bless)

Confirmed with human 2026-06-05 — none of these gate the launch:

- **Audit Cluster B + C** — routed to M-Auto-6 (parallel 2× research-
  agent dispatch), opened AFTER M-Auto-5 close. Action_bank.md §5.2
  Sprint 076 surfaced section captures the five candidates (B.1 trace
  truncation; B.2 `primary_uc` vs `active_use_case` authority; C.1
  cs59s 400; C.2 46f5b2e9 500 + double-send; C.3 generic clarifier
  branch).
- **OQ-S76.A6** (bot self-diagnoses sim drift, runtime ignores) —
  reassess at M-Auto-5 close review; not a re-bless precondition. With
  the simfixed simulator at 0 % contamination, A.6 has lost its trigger
  surface and may dissolve; if any sim-drift reasoning surfaces in the
  re-blessed traces, route as M-Auto-6 candidate.
- **OQ-S76.drift-stop** (literal `stop_reason="simulator_drift_blocked"`
  not surfaced — currently recorded as `stop_reason="error"`, which
  already fails `trace_minimum` correctly) — label fidelity only; NOT
  a blocker.
- **`R-eval-interactive-judge-score-never-populated`** (chronic judge
  layer; OQ-S76.judge-zero collapsed in 2026-06-05) — LOW priority;
  NOT promoted to M-Auto-6; NOT a re-bless blocker. Judge layer
  excluded from paired-evidence review.
- **S-Auto-18 (M-Auto-4 §5 escalation-family tiers, `eval_spec`)** —
  remains deferred behind M-Auto-6; not a re-bless precondition.

## 8. Provenance + cross-references

- Sub-sprint sequence that produced this evidence base:
  - `docs/sprints/sprint-074-{objective,handoff}.md` — S-Auto-19, eval-
    read column (5 corrections).
  - `docs/sprints/sprint-075-{objective,handoff}.md` — S-Auto-20,
    runtime-stamp + `loop_detected`.
  - `docs/sprints/sprint-076-{objective,handoff}.md` — S-Auto-21, input
    column / simulator role-inversion fix. §6 of the handoff carries
    the exact re-bless command.
- M-Auto-5 milestone spec: `docs/milestone_objective.md`.
- Input artifact: `docs/diagnostics/2026-06-04-eval-framework-and-
  simulator-audit.md`.
- OQ-S76.judge-zero resolution evidence: `docs/action_bank.md` §5.2
  R-item `R-eval-interactive-judge-score-never-populated` (four-run
  table + sub-variant breakdown).
- Cold-start state at launch: `docs/10-handoff.md` §0 (HEAD `1bc77c1`).
