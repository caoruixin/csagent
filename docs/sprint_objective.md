---
title: "Sprint 102 / S-Auto-50 (M-Auto-12 WP1) — objective-alignment annotation (observation-only)"
doc_tier: current-runtime
status: proposal
implementation_status: not_started
source_of_truth: this file (active sub-sprint contract); parent milestone = docs/milestone_objective.md
last_reviewed: 2026-06-22
review_cadence: per sprint
supersedes: []
superseded_by: null
notes: >
  DRAFT — deliver-agent scoping output, PENDING HUMAN APPROVAL before launch. Adds an
  observation-only objective-alignment label (FULL_SUCCESS / OBJECTIVE_KEEP /
  PERIPHERAL_ONLY / OFF_TARGET) computed AFTER the existing keep/discard verdict, from the
  run's PRIMARY targets + the existing per-case Tier-1 posteriors. Does NOT change
  keep/discard or the V3 safety/noise gate. Consumer wiring (merge/seed) deferred
  (record-now-wire-later). Validated by zero-LLM replay over exp-81…86. NO real-LLM run,
  NO re-bless, NO canonical-pointer move, NO bot/runtime/prompt/simulator/eval-scoring
  change. Source: docs/proposals/m_auto_7_objective_alignment_and_staged_eval.md §4 + §8.2.
---

# Sprint 102 / S-Auto-50 (M-Auto-12 WP1) — objective-alignment annotation

> **STATUS: DRAFT — pending human approval. Do NOT launch in the scoping session.**

## 1. Class

- **Layer:** `eval_spec` / reporting (autoloop fitness verdict label). Reads existing
  posteriors + the configured PRIMARY-target list; **observation-only** — does not change
  the keep/discard decision or the V3 safety/noise gate.
- **§7 stanza:** **EXEMPT** (reporting / observation-only; no bot-semantic surface;
  zero-LLM-validated). Anti-hardcode note: the label is a threshold-on-existing-posteriors
  classifier; thresholds are **suggested defaults** (config-overridable), not Tier-0; no
  keyword/regex/per-case hardcode (it reads `pilot_snapshot.primary_targets` + `tier_n`).
- **§4.1 Codex:** per-sub-sprint **read-only Codex REQUIRED** — confirm the label is
  observation-only (keep/discard unchanged), thresholds are defaults, and no semantic
  hardcode is introduced.
- **Real-LLM validation:** **NOT required** — zero-LLM replay over archived exp results is
  the validation (proposal §8.2).

## 2. Goal

Attach an observation-only `objective_alignment` label to every autoloop verdict so a KEEP
can be read as "did it move a PRIMARY?" (`FULL_SUCCESS` / `OBJECTIVE_KEEP`) vs "merely
admissible" (`PERIPHERAL_ONLY` / `OFF_TARGET`). Prove on archived evidence that this
distinguishes exp-86's peripheral KEEP (→ `OFF_TARGET`) from a real objective win — without
changing any keep/discard verdict.

## 3. Code anchors (read-only, grounded)

- **Verdict + per-case posteriors:** `autoloop/autoloop/scoring/tier_evaluator.py` —
  `LexicographicVerdict` (`:79-99`; fields `decision`, `discard_reason`, `classification`,
  `tier_breakdown`); the per-case Tier-1 posteriors are built in `_build_gate_context`
  (`:389-502`), stored as `ctx.tier_n[case_id]` with `tier`, `p_base`, `p_cand`,
  `P_improve`, `P_regress` (`:437-444`), and surfaced in the Layer-1 `metrics_observed`
  `tier_n.per_case` + the `regressed`/`ambiguous`/`improved` lists (`:753-763`).
- **PRIMARY targets:** `pilot_snapshot.primary_targets` — built by
  `config_validator.build_pilot_snapshot()` (`config_validator.py:134-156`, key
  `primary_targets`) from `config.yaml pilot.primary_targets` (`config.yaml:51-58`); created
  per-iteration at `loop.py:183-185` (`result.pilot_snapshot`), available **before**
  `evaluate()` runs.
- **Verdict computed:** `loop.py` ~`:370-383` (`evaluate()` → `result.verdict` →
  `result.decision`).
- **Ledger row:** `loop.py:_build_record_dict` (`:624-651`) writes the `experiments.jsonl`
  row (already carries `pilot_snapshot` `:649` + `verdict` via `_serialize_verdict`
  `:737-748` with `tier_breakdown`). The new label field is added here.
- **Zero-LLM replay harness:** `autoloop/tests/test_fitness_gate_oracle.py` (`_evaluate(n)`
  `:75-85` replays `results/runs/exp-{n}/eval/`; pinned keep/discard matrix `_DISCARDS`/
  `_KEEPS` `:50-59`).

## 4. Label definition (proposal §4 — implement faithfully + test)

Computed **after** `evaluate()`, from `pilot_snapshot.primary_targets` + the per-case
Tier-1 posteriors. Per-PRIMARY signals come from that primary's `tier_n` entry (tier +
`P_improve` / `P_regress` + majority-flip; handle TIER-S/F/N — a TIER-S primary that holds
counts as a pass, a TIER-F primary at 0 with `P_improve` below threshold contributes
OFF_TARGET).

| label | rule |
|-------|------|
| `FULL_SUCCESS` | all PRIMARY targets majority-pass |
| `OBJECTIVE_KEEP` | ≥1 PRIMARY majority-flip **or** `P_improve ≥ 0.8`, **and** no PRIMARY credible regression |
| `PERIPHERAL_ONLY` | gate KEEP but improvement only on non-PRIMARY cases (no PRIMARY credible improvement, but the run is a gate KEEP) |
| `OFF_TARGET` | no PRIMARY credible improvement (all `P_improve < 0.8`, no flip) |

- Also record an observation-only `merge_eligible` boolean = `gate-KEEP AND (OBJECTIVE_KEEP
  or FULL_SUCCESS)` — **recorded only, no consumer wired** (record-now-wire-later).
- **Edge cases (must handle gracefully, with tests):** empty `primary_targets` (non-pilot
  run) → a distinct `UNSCOPED` label (objective alignment not assessable; never crash); a
  PRIMARY target absent from the eval set / `tier_n` → documented handling (treat as not-
  improved, record a diagnostic); a DISCARD verdict → still compute the label (informational;
  `merge_eligible=false` since not a gate-KEEP).
- **Thresholds** (`P_improve ≥ 0.8`; reuse the gate's existing `P_regress` threshold for
  "credible regression") read from config with these **suggested defaults**; do not
  hardcode beyond the default constant.

## 5. Scope (numbered)

1. **Implement the label classifier** (pure function: `(primary_targets, per_case_tier_n,
   gate_decision) → (label, merge_eligible)`), placed where it can read the verdict's
   posteriors (e.g. a new `objective_alignment.py` in `autoloop/autoloop/scoring/`, or a
   function in `tier_evaluator.py`). No I/O, no LLM, deterministic.
2. **Wire it observation-only** in `loop.py` after `evaluate()` (using `result.pilot_snapshot`
   + `result.verdict`), attach to `result` (e.g. `result.objective_alignment`), and serialize
   into the `experiments.jsonl` row via `_build_record_dict` (`:624-651`) — **without
   touching `result.decision`, the verdict's keep/discard, or the safety gate.**
3. **Unit tests** (new `test_objective_alignment.py` or extend `test_tier_evaluator.py`):
   each of the 4 labels + `UNSCOPED` + the edge cases; assert thresholds are read from
   config (override changes the label); assert the classifier never mutates the verdict.
4. **Zero-LLM replay over exp-81…86** (extend the `test_fitness_gate_oracle.py` pattern):
   recompute each archived exp's label from its `results/runs/exp-{n}/eval/` + the run's
   primary targets; produce a per-exp label report; **assert exp-86 → `OFF_TARGET`** (the
   motivating case) and that the **existing keep/discard matrix is byte-unchanged**.
5. **Validation report** (in the handoff): how many of exp-81…86's gate-KEEPs were
   `PERIPHERAL_ONLY`; whether any true `OBJECTIVE_KEEP`/`FULL_SUCCESS` exists; and a note on
   how refinement-seed selection *would* change once a consumer reads the label (informational
   — no consumer built).

## 6. Test / eval requirements

- New classifier unit tests + the exp-81…86 zero-LLM label-replay test, all green.
- **The `test_fitness_gate_oracle.py` keep/discard matrix is byte-unchanged** (the proof the
  label is observation-only — nothing gating moved).
- Full `autoloop` pytest green (currently ~412 / the S-Y1.7+ suite); no existing test
  altered except additive.
- `scoring_code_baseline_sha`: if the gate-scoring code is functionally unchanged (the label
  is additive, off the keep/discard path), confirm whether the SHA moves; if it moves only
  because new code was added but no gate behaviour changed, the offline oracle replay is the
  acceptance (no re-bless). **No `autoloop/config.yaml` baseline-pointer move; no
  `docs/current_eval_baseline.md` change.**
- No real-LLM run.

## 7. Hard fences / STOP conditions

**Forbidden (touch NONE):** the keep/discard decision; the V3 safety/noise gate
(`_evaluate_layer*`, the FS anti-误杀 floor, cross-case gate); eval scoring / CaseSpecs /
baselines / canonical pointers; the bot / runtime / prompts / simulator; merge-eligibility
or seed-selection consumers (do NOT build them); the PRIMARY-target list (do NOT re-point);
the `no_ad_id` CaseSpec (proposal §5, separate product decision).

**STOP and surface (do not force) if:** implementing the label cleanly requires changing
any keep/discard verdict or the gate; or the exp-81…86 replay shows the keep/discard matrix
would move (that means the label is not observation-only — STOP); or a PRIMARY target's
posterior is not recoverable from the archived runs (record the gap, do not fabricate).

## 8. Codex review plan (§4.3)

Per-sub-sprint **read-only Codex** (deliver-dispatched; use `model_reasoning_effort=minimal`
or `low` + a watchdog — the gateway hangs on high/xhigh, see the codex-gateway reference):
confirm (1) the label is observation-only (keep/discard + safety gate unchanged; oracle
matrix byte-identical); (2) thresholds are suggested defaults, not Tier-0; (3) no semantic
hardcode (reads existing posteriors + the configured primary list; no keyword/regex/per-case
branch). Record the verdict in `docs/codex-findings.md`.

## 9. Handoff requirements

`docs/sprints/sprint-102-handoff.md`: the implemented anchors; the unit-test + zero-LLM
replay evidence; the per-exp label report (exp-81…86) with the `PERIPHERAL_ONLY` / true-
`OBJECTIVE_KEEP` counts + exp-86 → `OFF_TARGET`; proof the keep/discard oracle matrix is
byte-unchanged; the §4.1 Codex verdict; §12 explicit records (changed = autoloop scoring/
loop + tests; not-changed = keep/discard, gate, scoring, baseline, bot).

## 10. Commit discipline

Stage by file: the classifier + the `loop.py` wiring + tests (autoloop/** only) +
handoff. No eval-scoring/baseline/canonical/bot/CaseSpec files. Tree green at each boundary.

## 11. Self-check (dev ticks before done)

- [ ] Label classifier implemented as a deterministic pure function; 4 labels + `UNSCOPED`
      + edge cases covered by unit tests; thresholds read from config (default-overridable).
- [ ] Wired observation-only after `evaluate()`; serialized into the `experiments.jsonl`
      row; `result.decision` / verdict keep-discard / safety gate untouched.
- [ ] Zero-LLM replay over exp-81…86 produces the per-exp label report; **exp-86 →
      `OFF_TARGET`**; keep/discard oracle matrix **byte-unchanged**.
- [ ] Full autoloop pytest green; no existing test altered except additive; no re-bless / no
      canonical-pointer move.
- [ ] No forbidden surface touched (§7); any STOP recorded.
- [ ] §4.1 read-only Codex `approve`; handoff + validation report written; commit autoloop/**
      + docs only.
