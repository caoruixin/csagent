# Dev prompt — Sprint 102 / S-Auto-50 (M-Auto-12 WP1) — objective-alignment annotation

> Self-contained executable view of `docs/sprint_objective.md` (canonical contract). If
> this prompt and the objective diverge, the objective wins. **Do NOT launch until the
> human approves the M-Auto-12 scope.**

## Role identity

You are the **dev agent for Sprint 102 / S-Auto-50 (M-Auto-12 WP1)**. One-line goal: **add
an observation-only `objective_alignment` label to every autoloop verdict** (FULL_SUCCESS /
OBJECTIVE_KEEP / PERIPHERAL_ONLY / OFF_TARGET), computed AFTER the existing keep/discard
verdict from the run's PRIMARY targets + the existing per-case Tier-1 posteriors, **without
changing keep/discard or the safety gate** — validated by zero-LLM replay over exp-81…86.

## Read order (minimal)

`AGENTS.md` (auto-loaded) + this prompt. Background (read-only): the source proposal
`docs/proposals/m_auto_7_objective_alignment_and_staged_eval.md` §4 + §8.2 (the why + the
label table + the validation plan).

## Class

- `eval_spec` / reporting (autoloop fitness verdict label); **observation-only** — does not
  change keep/discard or the V3 safety/noise gate.
- **§7 EXEMPT** (reporting/observation-only; zero-LLM-validated). Anti-hardcode note:
  threshold-on-existing-posteriors classifier; thresholds = suggested defaults
  (config-overridable), not Tier-0; no keyword/regex/per-case hardcode.
- **§4.1 read-only Codex REQUIRED** (deliver-dispatched at close).
- **Real-LLM NOT required** (zero-LLM replay is the validation).

## Code anchors (read-only, grounded)

- Verdict + posteriors: `autoloop/autoloop/scoring/tier_evaluator.py` —
  `LexicographicVerdict` (`:79-99`); per-case Tier-1 posteriors in `_build_gate_context`
  (`:389-502`) as `ctx.tier_n[case_id]` = `{tier, p_base, p_cand, P_improve, P_regress}`
  (`:437-444`); surfaced in Layer-1 `metrics_observed.tier_n.per_case` + `regressed`/
  `ambiguous`/`improved` (`:753-763`).
- PRIMARY targets: `pilot_snapshot.primary_targets` — `config_validator.build_pilot_snapshot()`
  (`config_validator.py:134-156`) from `config.yaml pilot.primary_targets` (`:51-58`);
  created per-iteration `loop.py:183-185` (`result.pilot_snapshot`), available BEFORE
  `evaluate()`.
- Verdict computed: `loop.py` ~`:370-383` (`evaluate()` → `result.verdict` → `result.decision`).
- Ledger row: `loop.py:_build_record_dict` (`:624-651`) — already carries `pilot_snapshot`
  (`:649`) + `verdict` via `_serialize_verdict` (`:737-748`). Add the label field here.
- Zero-LLM replay harness: `autoloop/tests/test_fitness_gate_oracle.py` (`_evaluate(n)`
  `:75-85`; pinned keep/discard matrix `_DISCARDS`/`_KEEPS` `:50-59`).

## Label definition (implement faithfully + test)

Computed after `evaluate()`, from `pilot_snapshot.primary_targets` + per-case Tier-1
posteriors. Per-PRIMARY signal = that primary's `tier_n` entry (tier + P_improve/P_regress +
majority-flip; TIER-S that holds = pass; TIER-F at 0 with P_improve<thr → OFF_TARGET).

| label | rule |
|-------|------|
| `FULL_SUCCESS` | all PRIMARY targets majority-pass |
| `OBJECTIVE_KEEP` | ≥1 PRIMARY majority-flip OR `P_improve ≥ 0.8`, AND no PRIMARY credible regression |
| `PERIPHERAL_ONLY` | gate KEEP but no PRIMARY credible improvement (improvement only on non-PRIMARY) |
| `OFF_TARGET` | no PRIMARY credible improvement (all `P_improve < 0.8`, no flip) |

- Also record observation-only `merge_eligible = gate-KEEP AND (OBJECTIVE_KEEP or
  FULL_SUCCESS)` — **recorded only, NO consumer wired** (record-now-wire-later).
- **Edge cases (handle + test):** empty `primary_targets` → `UNSCOPED` (never crash);
  PRIMARY absent from `tier_n` → treat as not-improved + record a diagnostic; DISCARD verdict
  → still label (informational; `merge_eligible=false`).
- Thresholds (`P_improve ≥ 0.8`; reuse the gate's existing `P_regress` threshold for
  "credible regression") read from config with these suggested defaults.

## Scope (step-by-step)

1. **Classifier** — a deterministic pure function `(primary_targets, per_case_tier_n,
   gate_decision) → (label, merge_eligible)` (new `autoloop/autoloop/scoring/objective_alignment.py`
   or a `tier_evaluator.py` function). No I/O, no LLM.
2. **Wire observation-only** in `loop.py` after `evaluate()` (read `result.pilot_snapshot` +
   `result.verdict`), attach `result.objective_alignment`, serialize into the
   `experiments.jsonl` row in `_build_record_dict` (`:624-651`). **Do NOT touch
   `result.decision`, the verdict keep/discard, or the safety gate.**
3. **Unit tests** (new `test_objective_alignment.py` or extend `test_tier_evaluator.py`): 4
   labels + `UNSCOPED` + edge cases; thresholds read from config (override flips the label);
   classifier never mutates the verdict.
4. **Zero-LLM replay over exp-81…86** (extend the `test_fitness_gate_oracle.py` pattern):
   recompute each archived exp's label from `results/runs/exp-{n}/eval/` + its primary
   targets; per-exp label report; **assert exp-86 → `OFF_TARGET`**; assert the keep/discard
   oracle matrix **byte-unchanged**.
5. **Validation report** (handoff): count of exp-81…86 gate-KEEPs that were
   `PERIPHERAL_ONLY`; whether any true `OBJECTIVE_KEEP`/`FULL_SUCCESS`; how seed selection
   *would* change once a consumer reads the label (informational — no consumer built).

## Test / eval

- New classifier unit tests + exp-81…86 zero-LLM label-replay test, all green.
- **`test_fitness_gate_oracle.py` keep/discard matrix byte-unchanged** (proof: observation-only).
- Full autoloop pytest green; existing tests only additively touched; no re-bless; no
  `autoloop/config.yaml` baseline-pointer move; no `docs/current_eval_baseline.md` change. No
  real-LLM run.

## Hard fences / STOP

**Touch NONE:** keep/discard decision; the V3 safety/noise gate (`_evaluate_layer*`, FS
anti-误杀 floor, cross-case gate); eval scoring / CaseSpecs / baselines / canonical pointers;
bot / runtime / prompts / simulator; merge/seed consumers (do NOT build); the PRIMARY-target
list (do NOT re-point); the `no_ad_id` CaseSpec.

**STOP + surface (do not force) if:** a clean label requires changing any keep/discard
verdict or the gate; the exp-81…86 replay shows the keep/discard matrix would move (then the
label is not observation-only — STOP); or a PRIMARY's posterior is not recoverable from the
archived runs (record the gap, do not fabricate).

## Codex review plan

Per-sub-sprint read-only Codex (deliver-dispatched at close; `model_reasoning_effort=minimal`
or `low` + a watchdog — the gateway hangs on high/xhigh): confirm label is observation-only
(oracle matrix byte-identical), thresholds are defaults, no semantic hardcode.

## Handoff requirements

`docs/sprints/sprint-102-handoff.md`: anchors implemented; unit-test + zero-LLM replay
evidence; per-exp label report (exp-81…86) + `PERIPHERAL_ONLY`/true-`OBJECTIVE_KEEP` counts +
exp-86 → `OFF_TARGET`; proof keep/discard oracle matrix byte-unchanged; §4.1 Codex verdict;
§12 explicit records (changed = autoloop scoring/loop + tests; not-changed = keep/discard,
gate, scoring, baseline, bot).

## Commit discipline

Stage by file: classifier + `loop.py` wiring + tests (autoloop/** only) + handoff. No
eval-scoring/baseline/canonical/bot/CaseSpec files. Tree green at each boundary.

## Self-check (tick before done)

- [ ] Deterministic classifier; 4 labels + `UNSCOPED` + edge cases tested; thresholds
      config-default-overridable.
- [ ] Wired observation-only after `evaluate()`; serialized to `experiments.jsonl`;
      decision/verdict-keep-discard/safety-gate untouched.
- [ ] exp-81…86 zero-LLM label replay; exp-86 → `OFF_TARGET`; keep/discard oracle matrix
      byte-unchanged.
- [ ] Full autoloop pytest green; additive-only; no re-bless / no canonical-pointer move.
- [ ] No forbidden surface touched; any STOP recorded.
- [ ] §4.1 read-only Codex `approve`; handoff + validation report written; commit autoloop/**
      + docs only.
