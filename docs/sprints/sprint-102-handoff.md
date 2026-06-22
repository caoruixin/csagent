---
title: "Sprint 102 / S-Auto-50 (M-Auto-12 WP1) — dev handoff"
doc_tier: sprint-archive
status: archived
implementation_status: implemented
source_of_truth: >
  Code — autoloop/autoloop/scoring/objective_alignment.py (the classifier),
  autoloop/autoloop/loop.py (the observation-only wiring), and the two test
  files autoloop/tests/test_objective_alignment.py +
  test_objective_alignment_oracle.py (the evidence). This handoff is the
  narrative record.
last_reviewed: 2026-06-22
review_cadence: ad hoc
supersedes: []
superseded_by: null
notes: >
  Reporting/observation-only sub-sprint. Class: eval_spec / reporting (autoloop
  fitness verdict label). §7 EXEMPT (observation-only, zero-LLM-validated).
  §4.1 read-only Codex REQUIRED (deliver-dispatched at close). Real-LLM NOT
  required — zero-LLM replay over exp-81…86 is the validation. Does NOT change
  keep/discard, the V3 safety/noise gate, eval scoring, CaseSpecs, baselines, or
  any canonical pointer. Implements proposal
  docs/proposals/m_auto_7_objective_alignment_and_staged_eval.md §4 + §8.2 step 2.
---

# Sprint 102 / S-Auto-50 (M-Auto-12 WP1) — dev handoff

## 0. One-line result

**DELIVERED.** An observation-only `objective_alignment` label
(`FULL_SUCCESS` / `OBJECTIVE_KEEP` / `PERIPHERAL_ONLY` / `OFF_TARGET` /
`UNSCOPED`) is now computed for every autoloop verdict **after** the existing
keep/discard verdict, from the run's PRIMARY targets + the gate's own per-case
Tier-1 posteriors, and serialized into the `experiments.jsonl` row. It changes
**nothing** about keep/discard or the V3 safety/noise gate. Validated by a
zero-LLM label replay over exp-81…86: **exp-86 → `OFF_TARGET`** (the binding
pin), all six pilot-smoke candidates → `OFF_TARGET`, **0** true
`OBJECTIVE_KEEP`/`FULL_SUCCESS`, **0** merge-eligible. The pre-existing
keep/discard oracle matrix (`test_fitness_gate_oracle.py`, exp-66…79) is
**byte-unchanged**. Full autoloop pytest: **440 passed** (28 new).

## 1. Class + scope discipline

- **Layer (§3):** `eval_spec` / reporting (autoloop fitness verdict label).
  **Observation-only** — does not change keep/discard or the V3 safety/noise
  gate.
- **§7 stanza:** EXEMPT (reporting / observation-only, zero-LLM-validated).
  Anti-hardcode: the classifier is a threshold-on-existing-posteriors function;
  the two thresholds (`p_improve`, `p_regress`) are **suggested defaults read
  from config and overridable** (framework-defaults rule), NOT Tier-0; **no**
  keyword / regex / per-case hardcode; no benchmark case names encoded in code.
- **§4.1 read-only Codex:** REQUIRED — deliver-dispatched at close (see §11).
- **Real-LLM:** NOT required; zero-LLM replay is the validation (§5.7 does not
  apply — nothing about the measured variable is mocked; the label logic is the
  unit under test).

## 2. Anchors implemented

| anchor | grounded reference | what landed |
|--------|--------------------|-------------|
| Classifier (pure, no I/O, no LLM) | new `autoloop/autoloop/scoring/objective_alignment.py` | `classify(primary_targets, per_case_tier_n, gate_decision, *, p_improve, p_regress) → ObjectiveAlignment`; `evaluate_alignment(verdict, primary_targets, config)` wrapper |
| Per-case Tier-1 posteriors source | `tier_evaluator.py` `_build_gate_context` `ctx.tier_n` → surfaced at `verdict.tier_breakdown['tier1_outcome']['tier_n']['per_case']` (keyed `"<suite>:<case_id>"`) | classifier reads this dict; never recomputes a posterior |
| PRIMARY targets source | `pilot_snapshot.primary_targets` (`config_validator.build_pilot_snapshot`) — already on `result.pilot_snapshot`, available after `evaluate()` | read in `loop.py` step 9.4 |
| Wiring (observation-only) | `loop.py` step **9.4** (new), right after `evaluate()` sets `result.verdict`/`decision`/`discard_reason` | `result.objective_alignment = evaluate_alignment(verdict, primary_targets, config)`; best-effort try/except (never blocks persistence) |
| Ledger row | `loop.py` `_build_record_dict` (`:624-651`) | new `"objective_alignment"` key via `_safe_asdict`; independent of `decision`/`verdict` |
| `IterationResult` field | `loop.py` `IterationResult` | new optional `objective_alignment: ObjectiveAlignment | None = None` |
| Zero-LLM replay harness | new `autoloop/tests/test_objective_alignment_oracle.py` (mirrors `test_fitness_gate_oracle.py`) | replays exp-81…86 under the Option-R config |

## 3. Label definition (as implemented)

First-match-wins ladder over the PRIMARY signals (each PRIMARY's `tier_n` entry
→ `tier`, `p_base`, `p_cand`, `P_improve`, `P_regress`):

- per-PRIMARY `majority_pass` = `p_cand > 0.5`; `majority_flip` = majority_pass
  AND baseline `p_base ≤ 0.5`; `credible_improve` = flip OR `P_improve ≥
  p_improve` (default 0.8); `credible_regress` = `P_regress ≥ p_regress` (reuses
  the gate's `fitness.tier_decision.p_regress`, default 0.8); `stuck_hard` =
  present-but-`p_cand==0`-and-not-improved **OR** absent from posteriors.

1. `UNSCOPED` — no PRIMARY targets declared (never crashes).
2. `FULL_SUCCESS` — every PRIMARY majority-passes.
3. `OBJECTIVE_KEEP` — ≥1 PRIMARY credibly improves AND no PRIMARY credibly
   regresses.
4. `PERIPHERAL_ONLY` — gate KEEP AND no PRIMARY credible improvement AND no
   PRIMARY regressed/stuck-hard AND ≥1 **non-PRIMARY** case credibly improved
   (a benign peripheral keep).
5. `OFF_TARGET` — otherwise (catch-all: no PRIMARY credible improvement).

`merge_eligible = (gate == keep) AND label ∈ {FULL_SUCCESS, OBJECTIVE_KEEP}` —
**recorded only; NO consumer wired** (record-now-wire-later, proposal §8.2 step
2). Edge cases: empty primaries → `UNSCOPED`; PRIMARY absent from `tier_n` →
treated as not-improved + stuck-hard + a `diagnostics` entry; DISCARD verdict →
still labelled (informational), `merge_eligible=false`, never `PERIPHERAL_ONLY`.

### 3.1 PERIPHERAL_ONLY vs OFF_TARGET — disambiguation (documented decision)

The proposal §4 gives PERIPHERAL_ONLY ("gate KEEP but improvement only on
non-PRIMARY") and OFF_TARGET ("no PRIMARY credible improvement, all P_improve <
thr, no flip") rules that **both match exp-86** (gate KEEP carried by a single
non-PRIMARY improvement, neither PRIMARY improved). The binding pin — the
proposal §2 headline and this sub-sprint's hard assert — is **exp-86 →
OFF_TARGET**. The overlap is resolved using the proposal's **own** per-PRIMARY
note *"TIER-F at 0 with P_improve < thr → OFF_TARGET"*: a PRIMARY stuck at
hard-zero (or absent) forces OFF_TARGET, so PERIPHERAL_ONLY is reserved for the
benign case where the PRIMARIES at least exist, are non-zero, and none
regressed. exp-86's `cs_uc_a_no_ad_id_ad_specific` is exactly "TIER-F at 0,
P_improve 0.059 < 0.8" → stuck-hard → OFF_TARGET. Both labels remain reachable +
unit-tested. **Functional consequence is identical** (PERIPHERAL_ONLY and
OFF_TARGET are both non-merge, non-seed), so the exact boundary is
observation-only. **OQ-S102.1** (low-stakes, for deliver/Codex): confirm or
re-point this PERIPHERAL_ONLY/OFF_TARGET precedence — no scored bar moves either
way.

## 4. Test evidence — unit (zero-LLM, zero-I/O)

`autoloop/tests/test_objective_alignment.py` — **18 passed**. Covers: all four
labels + `UNSCOPED`; edge cases (empty primaries, PRIMARY absent → diagnostic,
DISCARD still labelled + merge-ineligible, DISCARD never PERIPHERAL_ONLY,
suite-prefixed vs bare keys); thresholds config-default-overridable (lowering
`p_improve` 0.8→0.1 **flips** OFF_TARGET→OBJECTIVE_KEEP; lowering `p_regress`
flips the credible-regression verdict); `evaluate_alignment` reads thresholds
from config + tolerates a short-circuited verdict with no `tier1_outcome` block;
structural invariants (classifier does not mutate its inputs;
`evaluate_alignment` does not mutate `verdict.decision`; result is a frozen
dataclass).

## 5. Test evidence — zero-LLM replay over exp-81…86 (PRIMARY evidence)

`autoloop/tests/test_objective_alignment_oracle.py` — **10 passed** (~8 s).
Replays each archived candidate under `autoloop/config.pilot-s-auto-38.yaml`
(the run-scoped Option-R split-full baseline the smoke actually bound — proposal
§1), reproducing the proposal §2 posteriors exactly.

| exp | gate | label | merge-eligible | PRIMARY posteriors (cs_uc_a_no_ad_id / cs_uc_a_loaded_listing) |
|-----|------|-------|:---:|---|
| 81 | discard | `OFF_TARGET` | ❌ | both absent (Tier-0 short-circuit) → stuck-hard + diagnostic |
| 82 | discard | `OFF_TARGET` | ❌ | both absent → stuck-hard + diagnostic |
| 83 | discard | `OFF_TARGET` | ❌ | both absent → stuck-hard + diagnostic |
| 84 | discard | `OFF_TARGET` | ❌ | both absent → stuck-hard + diagnostic |
| 85 | discard | `OFF_TARGET` | ❌ | both absent → stuck-hard + diagnostic |
| **86** | **keep** | **`OFF_TARGET`** | ❌ | no_ad_id TIER-F 0.0→0.0, P_improve **0.059** (stuck-hard); loaded_listing TIER-N 0.091→0.077, P_improve **0.146** (no flip); keep carried by non-PRIMARY `anchor_outcome_uc_g_gdpr` |

**Validation report (handoff requirement / proposal §8.2 step 2):**

- Gate-KEEPs among exp-81…86: **1** (exp-86). Of these, **0** are
  `PERIPHERAL_ONLY`, **0** are true `OBJECTIVE_KEEP`/`FULL_SUCCESS`; **1** is
  `OFF_TARGET`.
- gate-DISCARDs (exp-81…85): all `OFF_TARGET` (PRIMARY posteriors absent because
  the verdict short-circuited at the Tier-0 floor; the label degrades safely to
  "not improved" + a `primary_absent_from_posteriors` diagnostic, never
  crashes).
- **exp-86 → `OFF_TARGET` asserted** (binding pin) — gate KEEP, but neither
  PRIMARY moved; the keep was a single peripheral improvement.
- **How seed selection *would* change once a consumer reads the label
  (informational — no consumer built):** all six pilot-smoke candidates are
  `merge_eligible=false`, so a future seed/merge consumer gated on
  `merge_eligible` would select **none** of exp-81…86 as a refinement seed or
  merge candidate — including exp-86, which the keep/discard gate alone would
  have admitted. This is exactly the targeting gap the proposal §2 named.

## 6. Observation-only proof (keep/discard untouched)

- `test_label_is_observation_only_gate_decision_unchanged` (oracle): for every
  exp-81…86, `evaluate()` alone and `evaluate()`-then-`evaluate_alignment()`
  yield the **same** gate decision — the label is a pure read.
- `test_fitness_gate_oracle_keep_discard_matrix_byte_unchanged` (oracle):
  imports `test_fitness_gate_oracle._DISCARDS`/`_KEEPS` and asserts the exp-66…79
  matrix is intact.
- `git diff --stat HEAD -- autoloop/tests/test_fitness_gate_oracle.py` is
  **empty** — the existing oracle file is byte-unchanged.
- Full autoloop pytest: **440 passed, 1 warning** (the warning is the
  pre-existing `test_layer4_shadow_missing_keeps_with_warning` fixture, not
  introduced here).

## 7. STOP conditions — none triggered

- A clean label did **not** require changing any keep/discard verdict or the
  gate (proven in §6).
- The exp-81…86 replay shows the keep/discard matrix does **not** move (the
  label is observation-only).
- Every PRIMARY posterior is recoverable from the archived runs (exp-86 both
  present + reproduce proposal §2; exp-81…85 legitimately absent due to the
  Tier-0 short-circuit — recorded as a diagnostic, **not fabricated**).

## 8. Env

No backend / no real-LLM / no eval run. Validation is pure Python over archived
`autoloop/results/runs/exp-{81..86}/eval/` dirs + the frozen Option-R baseline,
executed with `autoloop/.venv/bin/python` (3.13.1, PyYAML 6.0.3). Dev tree on
`auto-loop-branch`, parent HEAD `3249a573`.

## 9. Config

No `autoloop/config.yaml` edit (no baseline-pointer move, no
`docs/current_eval_baseline.md` change). The classifier reads
`fitness.objective_alignment.p_improve` (suggested default 0.8) and reuses
`fitness.tier_decision.p_regress` (default 0.8) **with defaults**, so an absent
`objective_alignment` block is fully backward compatible — no config key needs
to exist for the label to compute.

## 10. Self-check (contract)

- [x] Deterministic classifier; 4 labels + `UNSCOPED` + edge cases tested;
      thresholds config-default-overridable (override flips the label).
- [x] Wired observation-only after `evaluate()`; serialized to
      `experiments.jsonl`; decision / verdict keep-discard / safety gate
      untouched.
- [x] exp-81…86 zero-LLM label replay; exp-86 → `OFF_TARGET`; keep/discard
      oracle matrix byte-unchanged.
- [x] Full autoloop pytest green (440); additive-only; no re-bless / no
      canonical-pointer move.
- [x] No forbidden surface touched; STOP conditions recorded as none-triggered.
- [ ] §4.1 read-only Codex `approve` — deliver-dispatched at close (§11).

## 11. §4.1 Codex review (pending — deliver-dispatched at close)

Per-sub-sprint read-only Codex review REQUIRED at close
(`model_reasoning_effort=minimal|low` + watchdog; the CRS gateway hangs on
high/xhigh). Reviewer should confirm: (1) the label is observation-only — the
keep/discard oracle matrix is byte-identical and `evaluate_alignment` reads but
never writes `verdict.decision`; (2) the two thresholds are config-overridable
defaults, not Tier-0; (3) no semantic hardcode (no benchmark case names / fixed
ids in code). Verdict to be recorded verbatim here + in `docs/codex-findings.md`
at close.

## 12. Explicit change record

**Changed (autoloop only):**

- `autoloop/autoloop/scoring/objective_alignment.py` — **new**: the classifier
  (`classify`, `evaluate_alignment`, `ObjectiveAlignment`, `PrimarySignal`).
- `autoloop/autoloop/scoring/__init__.py` — export `classify`,
  `evaluate_alignment`, `ObjectiveAlignment`.
- `autoloop/autoloop/loop.py` — import the module; new `IterationResult`
  `objective_alignment` field; step 9.4 computes it after `evaluate()`
  (best-effort); `_build_record_dict` serializes the new
  `"objective_alignment"` ledger key.
- `autoloop/tests/test_objective_alignment.py` — **new**: 18 unit tests.
- `autoloop/tests/test_objective_alignment_oracle.py` — **new**: 10 zero-LLM
  replay tests.
- `docs/sprints/sprint-102-handoff.md` — this handoff.

**NOT changed (verified):**

- keep/discard decision; the V3 safety/noise gate (`_evaluate_layer*`, FS
  anti-误杀 floor, cross-case gate) — `tier_evaluator.py` untouched.
- `test_fitness_gate_oracle.py` keep/discard matrix — byte-unchanged.
- eval scoring / CaseSpecs / baselines / canonical pointers
  (`autoloop/config.yaml`, `docs/current_eval_baseline.md`) — untouched.
- bot / runtime / prompts / simulator (`server/`, `ui/`, `eval_interactive/`) —
  untouched.
- merge / seed consumers — NOT built (`merge_eligible` recorded only).
- the PRIMARY-target list — NOT re-pointed; the `no_ad_id` CaseSpec — untouched.
