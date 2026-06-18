---
title: M-Auto-7 — objective-aligned KEEP semantics + primary-first staged evaluation
doc_tier: proposal
status: proposal
implementation_status: not_started
source_of_truth: this file
last_reviewed: 2026-06-18
review_cadence: ad hoc
supersedes: []
superseded_by: null
notes: >
  Authored 2026-06-18 after the S-Auto-38 exp-86 -n1 real-LLM smoke (run-scoped
  Option-R baseline). The smoke passed gate-trust but exposed a gap: a candidate
  whose two declared PRIMARY targets both stayed 0/13 still earned a gate KEEP on
  a peripheral improvement. This proposal records the PRIMARY failure taxonomy,
  the OFF_TARGET conclusion, an objective-alignment annotation layer (no safety-
  gate change), a primary-first staged-eval design, the unresolved no_ad_id
  CaseSpec question, and a recommended next-step ordering. Nothing here is
  implemented. Full pilot tranche stays HELD; exp-82 stays WITHDRAWN; no PRIMARY
  success is claimed. Intended hand-off: delivery agent plans the sub-sprints.
---

# M-Auto-7 — objective-aligned KEEP semantics + primary-first staged evaluation

## 0. One-paragraph summary

The S-Auto-38 exp-86 `-n 1` real-LLM smoke validated the run-scoped Option-R
baseline binding end-to-end (gate-trust PASS). It also surfaced a **targeting
gap**: the candidate's two declared PRIMARY targets
(`cs_uc_a_no_ad_id_ad_specific`, `cs_uc_a_loaded_listing`) both stayed at
near-zero pass rate, yet the experiment earned a **gate `keep`** on a single
peripheral improvement (`anchor_outcome_uc_g_gdpr`). The existing gate answers
"is this candidate admissible / non-regressive?" but not "did it move the
objective?". This proposal adds an **observation-only objective-alignment label**
on top of the unchanged safety/noise-aware gate, a **primary-first staged
evaluation** to cut the cost of OFF_TARGET candidates, records the **PRIMARY
failure taxonomy** (which shows the proposer optimized a non-gating dimension),
and names an **open CaseSpec question** that must be resolved before that case is
auto-optimized again.

## 1. Evidence base (all zero-LLM-reproducible except the smoke itself)

| artifact | path / ref |
|----------|-----------|
| Smoke run record | `autoloop/results/experiments.jsonl` (row `exp-86`), `autoloop/results/runs/exp-86/eval-results.json` |
| Launch record + smoke outcome | `docs/sprints/sprint-092-pilot-launch-record.md` §7 (commit `1c8ce52c`) |
| Run-scoped baseline (Option-R) | `eval_interactive/results/m-auto-7-prepilot-baseline-20260618-s_auto_38_split_full/` (harness `eval_interactive/analysis/rescore_s_auto_38_full_baseline.py`, commit `2f5c5e9`) |
| Run-scoped override | `autoloop/config.pilot-s-auto-38.yaml` (commit `7dc5a0d`) |
| Proposer | `claude-sonnet-4-6` (meta_agent); hypothesis at `autoloop/results/runs/exp-86/hypothesis.json` |
| PRIMARY CaseSpecs | `eval_interactive/case_specs/bad_cases/cs_uc_a_loaded_listing.yaml`, `…/cs_uc_a_no_ad_id_ad_specific.yaml` |

## 2. exp-86 → OFF_TARGET (the motivating conclusion)

`pilot_snapshot.primary_targets = [cs_uc_a_no_ad_id_ad_specific, cs_uc_a_loaded_listing]`.
Tier-1 per-case posteriors (gate's own numbers, against the Option-R baseline):

| PRIMARY | tier | p_base | p_cand | P_improve | P_regress | majority flip |
|---------|------|--------|--------|-----------|-----------|---------------|
| cs_uc_a_loaded_listing | TIER-N | 0.091 (1/11) | 0.077 (1/13) | **0.146** | 0.223 | no |
| cs_uc_a_no_ad_id_ad_specific | TIER-F | 0.0 (0/11) | 0.0 (0/13) | **0.059** | 0.084 | no |

Neither PRIMARY shows credible improvement (`P_improve ≪ 0.8`, no flip), even
after PRIMARY oversampling to 13 valid attempts each. The gate `keep` is real and
correct **at the admissibility level**, but the experiment is **OFF_TARGET** at
the objective level. The KEEP must not (today, silently) make exp-86 a refinement
seed or a merge candidate.

## 3. PRIMARY failure taxonomy (zero-LLM, from recorded traces)

The proposer's hypothesis edited `server/.../skills/resolve_faq_grounded_answer.yaml`
to add "Ad-context grounding (UC-A): call `get_customer_context` and anchor on the
listing before knowledge search," on the theory that UC-A fails by giving a generic
FAQ answer instead of a listing-grounded one. The traces refute that theory as the
**gating** cause: `grounding_compliance` never appears in any failure tag for
either case, and the edit left both targets flat (baseline→exp-86 modes unchanged).

### 3.1 cs_uc_a_loaded_listing (TIER-N, 1/13; baseline 1/11) — high-variance multi-mode

- **Main failure type:** three interleaved gating modes across draws —
  **UC misclassification** UC-A→UC-B (`L2_GATE:correct_uc`), **over-escalation**
  resolved↔escalated (`L2_GATE:correct_outcome`), and **STALL / trace_minimum**:
  the resolve flow loops `search_knowledge → resolve_article` with **empty bot
  answers and no `record_outcome`** (`STALL:STALL_AFTER_TOOL_INTENT`). The
  closest-to-correct draw (UC-A + resolved) still failed on STALL, and it did not
  even call `get_customer_context`.
- **Hypothesis vs actual:** misaligned. Gating is UC stability + escalate-vs-
  resolve + answer-emission, not grounding content.
- **Skill / phase / observable:** `classify_use_case` (unstable UC-A↔UC-B);
  `resolve_faq_grounded_answer` flow fails to terminate into one grounded customer
  answer + `record_outcome`; escalation posture over-fires on a low-risk case.
- **Skill-edit-fixable vs evaluator:** predominantly skill / semantic_planner;
  no clear evaluator artifact (the stall is a real empty-answer loop). The
  grounding edit optimized a non-gating dimension.
- **Feedback for next proposer:** stabilize UC-A classification; make the resolve
  flow emit a single grounded answer + `record_outcome` (stop re-searching);
  don't escalate a resolvable low-risk UC-A.

### 3.2 cs_uc_a_no_ad_id_ad_specific (TIER-F, 0/13; baseline 0/11) — over-escalation

- **Main failure type:** over-escalation / wrong outcome — bot **escalates**
  (baseline 7/11) instead of resolving (expected `resolve`), so `correct_outcome`
  gates; plus `trace_minimum` on resolved draws. UC is classified correctly
  (UC-A). The draw that called `get_customer_context` with a perfect 5/5 tool
  sequence and resolved still failed on `trace_minimum`.
- **Hypothesis vs actual:** misaligned. With no ad_id the grounding edit cannot
  even apply; escalate-vs-resolve + answer-completeness gate it.
- **Skill / phase / observable:** escalation posture (hands over rather than
  helping when no ad_id); incomplete terminal answer / `record_outcome` when it
  does resolve.
- **Skill-edit-fixable vs evaluator:** skill / semantic_planner for the
  over-escalation; **plus an open CaseSpec question** (§5).
- **Feedback for next proposer:** when an ad-specific question arrives with no
  ad_id, do not escalate; clarify which ad (if acceptable — see §5) or resolve
  with general guidance + `record_outcome`.

### 3.3 Cross-cutting

The proposer inferred "grounding" from a bare "near-zero pass rate." It should
instead be handed the **per-dimension gating failure rates** (`correct_uc`,
`correct_outcome`, `no_stall`, `trace_minimum`) for each PRIMARY so it targets
what actually gates. This is the target-specific feedback the next round needs.

## 4. Proposed objective-alignment annotation (observation-only)

A label computed **after** the existing keep/discard verdict, from
`pilot_snapshot.primary_targets` + the existing tier-1 per-case posteriors. It
does **not** change the safety/noise-aware gate and does **not** change
keep/discard. It gates **merge-eligibility** and **refinement-seed selection**.

| label | rule | merge-eligible (with gate KEEP) | refinement seed |
|-------|------|:---:|:---:|
| `FULL_SUCCESS` | all PRIMARY majority-pass | ✅ | ✅ |
| `OBJECTIVE_KEEP` | ≥1 PRIMARY majority-flip **or** `P_improve ≥ 0.8`, **and** no PRIMARY credible regression | ✅ | ✅ |
| `PERIPHERAL_ONLY` | gate KEEP but improvement only on non-PRIMARY cases | ❌ | ❌ (record only) |
| `OFF_TARGET` | no PRIMARY credible improvement (all `P_improve < 0.8`, no flip) | ❌ | ❌ |

- **Merge-eligible ≡ gate-KEEP AND (`OBJECTIVE_KEEP` or `FULL_SUCCESS`).**
- exp-86 → `OFF_TARGET` (despite gate KEEP) → not a seed, not merge-eligible.
- Thresholds (`0.8`, the regression threshold) are **suggested defaults**, not
  Tier-0 invariants; the adopter may override (per the framework-defaults rule).
- **Layer classification:** this is an `eval_spec` / reporting-layer measurement
  label, not a runtime semantic decision and not a Tier-0 invariant. No semantic
  hardcode; it reads existing posteriors.

## 5. Open CaseSpec question — `cs_uc_a_no_ad_id_ad_specific` product terminal

The spec demands `outcome_class: resolve, should_escalate: false`. The observed
dominant failure is the bot **escalating** instead of resolving. Two sub-questions
must be answered by the human / product owner **before** this case is auto-optimized
again:

1. When the ad-specific question arrives with **no ad_id**, is **"clarify / ask
   which ad"** an acceptable terminal state, or is strict `resolve` required?
2. If clarify is acceptable, the CaseSpec's acceptable-outcome set is too narrow
   and should be widened (an `eval_spec` fix), distinct from the bot's
   over-escalation (a `semantic_planner` fix).

Until (1) is decided, do **not** spend proposer/eval budget auto-optimizing this
case — the optimization target is undefined.

## 6. Proposed primary-first staged evaluation (separate sub-sprint)

Execution-flow + cost optimization; orthogonal to §4's gate semantics.

- **Stage A:** eval only the PRIMARY targets (+ a minimal tier-0 / anti-kill
  control subset) at the configured samples + PRIMARY oversample.
- **Early-stop gate:** if no PRIMARY shows credible movement (all `P_improve < τ`,
  no flip) → early-stop `OFF_TARGET`, skip full fitness. (continue / stop /
  ambiguous thresholds + adaptive sampling are the sub-sprint's design surface.)
- **Stage B (only on a target signal):** full `bad_cases` / `anchor_outcome` /
  `shadow` fitness + the **full safety gate, which remains the binding merge
  gate**. Stage A never authorizes a merge.
- **Safety property:** early-stop is always a **reject** (OFF_TARGET), so skipping
  full safety never admits an unsafe candidate — only rejected candidates skip it.
- **Payoff:** OFF_TARGET candidates drop from a ~66-min full eval (exp-86's
  elapsed) to a Stage-A fraction; directly addresses the oversampling cost (§7.4).
- **Validation before live use:** historical replay / cost simulation on
  exp-81…86 (zero-LLM where possible) + a staged-eval dry validation.

## 7. Recorded, non-blocking (track; do not gate on these)

1. **Gaming git-lookup WARN** — exp-86 raised one WARN
   `suspect_baseline_manipulation.git_lookup_failed` ("git log produced no commits
   for autoloop/config.yaml"): a cwd / `--config`-override artifact (the run was
   launched from `autoloop/` with the override). Make the detector resolve the
   config path repo-root-relative and honor the active `--config`. Observation-only;
   **no `scoring_code_drift`** fired.
2. **Tier-0 short-circuit reporting gap** — `tier0_majority` mixes enforced
   (`_TIER0_PY_FAMILY`) and non-enforced checks (e.g. `escalation_reason_family_match`,
   the S-Auto-38 Part-2) with no marker of which actually gates, and a tier-0
   discard short-circuits without surfacing the downstream signals it pre-empted.
   Observability improvement.
3. **Anti-kill sampling power** — the TIER-S anti-误杀 floor at n≈5 (+ oversample)
   has limited statistical power (p07 F5 ceiling); thin-sample posteriors may
   under-detect.
4. **Oversampling cost** — PRIMARY oversampling drove `bad_cases` to 13 attempts
   (~4× base), a main driver of exp-86's ~66-min wall-clock; partly addressed by §6.

## 8. Recommended next-step ordering (for the delivery agent)

1. **This doc** — tracked record of the taxonomy, OFF_TARGET conclusion,
   target-alignment semantics, staged-eval plan, and the open CaseSpec question.
2. **Implement target-alignment annotation first** (lowest risk: no safety-gate
   change, no keep/discard change, additive label only; affects merge-eligibility
   + seed selection). Validate by **zero-LLM replay over exp-81…86**: how many past
   KEEPs were actually `PERIPHERAL_ONLY`; whether any true `OBJECTIVE_KEEP` exists;
   how seed selection would change.
3. **Confirm the `no_ad_id` product terminal** (§5) with the human / product owner.
   Do not auto-optimize that case until resolved.
4. **Design primary-first staged eval** as an independent sub-sprint (§6): Stage A
   case set; continue/stop/ambiguous thresholds; adaptive sampling; Stage B binding
   safety; historical replay / cost simulation.
5. **Resume real-LLM runs** in order:
   target-annotation replay → staged-eval dry validation → `-n 1` smoke →
   `-n 2` pilot → (evidence permitting) `-n 4`.

Throughout: full pilot tranche stays **HELD**; exp-82 stays **WITHDRAWN**; the
global `autoloop/config.yaml` baseline pointer and `docs/current_eval_baseline.md`
stay **UNCHANGED** (canonical promotion is a separate milestone-close decision);
the run-scoped Option-R baseline binds via `--config config.pilot-s-auto-38.yaml`
only.
