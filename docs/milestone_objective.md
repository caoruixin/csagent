---
title: "M-Auto-12 — Objective-aligned KEEP semantics + autoloop verdict observability"
doc_tier: current-runtime
status: proposal
implementation_status: not_started
source_of_truth: this file (active-milestone pointer)
last_reviewed: 2026-06-22
review_cadence: per milestone
supersedes: []
superseded_by: null
notes: >
  DRAFT — deliver-agent scoping output, PENDING HUMAN APPROVAL before any dev session
  is launched. After M-Auto-9/10/11 cleared the runtime + eval substrate (no closure
  defect; escalation labels honest; trace-contract fixed; over-escalation not
  load-bearing), the remaining blocker to a USEFUL autoloop KEEP is not a runtime bug —
  it is that the autoloop's gate answers "is this candidate admissible / non-regressive?"
  but NOT "did it move the objective?" (exp-86 earned a gate KEEP on a peripheral case
  while both PRIMARY targets stayed flat — OFF_TARGET). M-Auto-12 makes the autoloop's
  KEEP semantics objective-aware and cleans up two verdict-observability gaps, so the
  next pilot iteration can be judged on whether it actually moved a PRIMARY. Source
  proposal: docs/proposals/m_auto_7_objective_alignment_and_staged_eval.md (§4 + §7).
  Bundle shape (human-selected 2026-06-22): WP1 annotation + WP2 observability fixes.
  Consumer wiring is "record now, wire later" (human-selected): the label is computed +
  serialized as observation-only reporting; merge-eligibility / refinement-seed
  consumers do NOT exist yet and are NOT built here. NOT in scope: primary-first staged
  eval (reserved M-Auto-8), the no_ad_id CaseSpec product decision (proposal §5), any
  keep/discard or safety-gate change, any re-point of PRIMARY targets, any pilot re-run,
  any re-bless / canonical-pointer move.
---

# M-Auto-12 — Objective-aligned KEEP semantics + autoloop verdict observability

> **STATUS: DRAFT — pending human approval. Do NOT launch from this file until approved.**

## 1. Why this milestone (the through-line)

The recent milestones cleared the autoloop's **substrate**: M-Auto-5 (eval verdict
correctness + trace-contract honesty), M-Auto-6 (runtime substrate hygiene), M-Auto-9
(escalation-reason honesty + resolve-can-land demonstrated), M-Auto-10 (closure-path
canonical; no closure defect), M-Auto-11 (over-escalation not load-bearing). The
conclusion across M-Auto-9/10/11 is that **the runtime + eval substrate is sound** — the
autoloop's failure to produce a useful KEEP (M-Auto-7 registry-only pilot CLOSED — NO
KEEP) is **not** a runtime defect.

Per `docs/proposals/m_auto_7_objective_alignment_and_staged_eval.md`, the real blockers
are autoloop-side: (1) the gate is **objective-blind** — exp-86 earned a gate `keep` on a
peripheral case (`anchor_outcome_uc_g_gdpr`) while both declared PRIMARY targets stayed
flat (`OFF_TARGET`), and the gate could not tell the difference; (2) two
verdict-observability gaps muddy trust in the gate output. M-Auto-12 fixes (1) and (2).
(The other proposal items — primary-first staged eval, and the `no_ad_id` objective/
CaseSpec product decision — are sequenced AFTER this, as M-Auto-8 and a human product
decision respectively.)

## 2. Milestone class (layer breakdown + §7 coverage)

| WP | Sub-sprint id | §3.2 layer | §7 stanza | Codex |
|----|---------------|-----------|-----------|-------|
| **WP1 — objective-alignment annotation** | Sprint 102 / S-Auto-50 | `eval_spec` / reporting (autoloop fitness label; reads existing posteriors; observation-only — does NOT change keep/discard or the safety gate) | **EXEMPT** (reporting/observation-only; no bot-semantic surface; zero-LLM-validated) — with an anti-hardcode note (thresholds are suggested defaults, not Tier-0; reads existing posteriors, no semantic hardcode) | per-sub-sprint read-only Codex (confirm: label is observation-only, thresholds are defaults, no semantic hardcode, no keep/discard change) |
| **WP2 — autoloop verdict observability** | Sprint 103 / S-Auto-51 | `infra` (autoloop gaming-detector path resolution + tier0 short-circuit reporting; no gate/keep-discard change) | **EXEMPT** (pure infra/observability) | §4.1 infra-EXEMPT; covered by the milestone-shared Codex at close |

## 3. Goal

Make the autoloop's verdict surface **objective-aware and trustworthy**: (WP1) attach an
observation-only objective-alignment label so a KEEP can be read as
`FULL_SUCCESS / OBJECTIVE_KEEP / PERIPHERAL_ONLY / OFF_TARGET`; (WP2) remove two
false/opaque signals in the verdict output. Neither WP changes the keep/discard verdict,
the V3 safety/noise gate, eval scoring, baselines, or the bot. This is the prerequisite
that lets the next pilot iteration (under M-Auto-8 staged eval, on a re-pointed achievable
objective) be judged on whether it actually moved a PRIMARY.

## 4. Sub-sprint sequence

### WP1 — Sprint 102 / S-Auto-50 — objective-alignment annotation (REQUIRED FIRST)

`eval_spec` / reporting. Add an `objective_alignment` label computed **after** the existing
`evaluate()` keep/discard verdict, from `pilot_snapshot.primary_targets` + the existing
per-case Tier-1 posteriors. Labels (proposal §4 table): `FULL_SUCCESS` (all PRIMARY
majority-pass) / `OBJECTIVE_KEEP` (≥1 PRIMARY majority-flip or `P_improve ≥ 0.8`, and no
PRIMARY credible regression) / `PERIPHERAL_ONLY` (gate KEEP but improvement only on
non-PRIMARY) / `OFF_TARGET` (no PRIMARY credible improvement). **Observation-only: does
NOT change `decision` (keep/discard) or the safety gate; computed + serialized into the
verdict + the `experiments.jsonl` row.** Consumer wiring deferred (record-now-wire-later):
merge-eligibility / refinement-seed consumers do not exist yet and are NOT built here — the
label is pure reporting until they are. Validated by a **zero-LLM replay over exp-81…86**
(via the `test_fitness_gate_oracle.py` pattern) producing a per-exp label report (must
reproduce exp-86 → `OFF_TARGET` despite gate KEEP) + new unit tests; the keep/discard
oracle matrix must stay **byte-unchanged**. Full contract: `docs/sprint_objective.md`; dev
prompt `compact/sprint-102-dev-prompt.md`.

### WP2 — Sprint 103 / S-Auto-51 — autoloop verdict observability (starts after WP1 closes)

`infra`. Two observation-only fixes from proposal §7:
1. **Gaming git-lookup path** (`autoloop/autoloop/scoring/gaming.py:264-303`
   `_check_suspect_baseline_manipulation` / `git_lookup_failed`): resolve the config path
   **repo-root-relative** and honor the active `--config` override, so a run launched from
   `autoloop/` with a `--config` override no longer raises a spurious
   `suspect_baseline_manipulation.git_lookup_failed` WARN. Observation-only; must not
   change `scoring_code_drift` or any gate outcome.
2. **Tier0 short-circuit reporting gap** (`autoloop/autoloop/scoring/tier_evaluator.py`
   `_TIER0_PY_FAMILY:135` + the short-circuit `_not_evaluated` path `:258-298`): mark which
   tier-0 checks actually **gate** (enforced `_TIER0_PY_FAMILY` vs non-enforced e.g.
   `escalation_reason_family_match`), and surface the downstream signals a tier-0 discard
   pre-empted. Reporting only — no change to which checks gate or to keep/discard.

WP2 is drafted as a fresh `sprint_objective.md` at WP1 close (not pre-embedded here).

## 5. Milestone acceptance bar

1. **WP1 (hard):** the `objective_alignment` label is computed + serialized; a zero-LLM
   replay over exp-81…86 yields a per-exp label report (exp-86 → `OFF_TARGET` reproduced;
   count of past KEEPs that were `PERIPHERAL_ONLY`; whether any true `OBJECTIVE_KEEP`
   exists); the **keep/discard oracle matrix is byte-unchanged** (observation-only proven);
   new unit tests + full autoloop pytest green; no re-bless / no canonical-pointer move.
2. **WP2 (hard):** the gaming git-lookup WARN no longer false-fires under the `--config` /
   `autoloop/`-cwd launch (a regression test reproduces the old false-WARN and proves it
   gone); the tier0 reporting surfaces which checks gate + the pre-empted signals; **no
   keep/discard or gate-membership change** (oracle matrix byte-unchanged); tests green.
3. **Milestone close (hard):** full autoloop pytest green; the keep/discard oracle matrix
   unchanged across both WPs; milestone-shared Codex `pass`/0; no re-bless, no
   `autoloop/config.yaml` baseline-pointer move, no `docs/current_eval_baseline.md` change.

## 6. Non-goals (explicit)

- **NOT building merge-eligibility / refinement-seed consumers** (record-now-wire-later;
  the label is observation-only until those are built).
- **NOT primary-first staged eval + adaptive sampling** — that is the reserved **M-Auto-8**
  (the proposal §6 next step, separate milestone).
- **NOT the `no_ad_id` objective / CaseSpec product decision** (proposal §5) — a human
  product decision; do not auto-optimize that case until it is resolved.
- **NOT re-pointing the PRIMARY targets** and **NOT re-running the pilot** — those follow
  once the objective is re-aligned (a later milestone).
- **No keep/discard change, no V3 safety/noise-gate change, no eval-scoring/baseline/
  canonical-pointer change, no bot/runtime/prompt/simulator change, no re-bless.**

## 7. Hard fences (milestone level)

- Both WPs are **observation-only on the verdict surface**: the keep/discard decision and
  the V3 safety/noise gate are FROZEN; the `test_fitness_gate_oracle.py` keep/discard
  matrix must stay byte-unchanged (the proof that nothing gating moved).
- Thresholds in the annotation (`P_improve ≥ 0.8`, the regression threshold) are
  **suggested defaults**, not Tier-0 invariants; adopter-overridable (framework-defaults
  rule). No semantic hardcode — the label reads existing posteriors + the configured
  primary-target list.
- No real-LLM run (zero-LLM replay is the validation for both WPs).
- No Tier-0 invention without `human_review_required`.

## 8. R-items consumed / surfaced

- **Consumed:** proposal §4 (objective-alignment annotation) + §7.1 (gaming git-lookup
  path) + §7.2 (tier0 short-circuit reporting gap).
- **Explicitly NOT consumed (stay open):** proposal §5 (no_ad_id product decision; human),
  §6 (primary-first staged eval = M-Auto-8), §7.3/§7.4 (anti-kill sampling power,
  oversampling cost — the latter partly addressed by M-Auto-8).

## 9. Next valid ids

- WP1: **Sprint 102 / S-Auto-50** (the M-Auto-11 WP2 conditional reservation was never
  activated → released).
- WP2: **Sprint 103 / S-Auto-51**.

## 10. Estimated duration (informational)

~2 sub-sprints (WP1 annotation + WP2 observability), both zero-LLM. Successors (separate
milestones / decisions): M-Auto-8 staged eval; the no_ad_id product decision + objective
re-point + pilot re-run.
