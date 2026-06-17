# Dev prompt — Sprint 092 / S-Auto-38 (escalation_compliance tier-0 reclassification)

Paste into a fresh Claude Code session. You implement an **approved** plan; do not
re-design. **Read first**, in order:

1. `docs/sprint_objective.md` (the binding contract — Sprint 092 / S-Auto-38).
2. `docs/solutions/2026-06-18-escalation-compliance-gate-reclassification-subsprint-plan.md` (rev 2 — design, schema, replay matrix, verification).
3. `docs/solutions/2026-06-18-cs11s01-expected-trigger-override-decision.md` (WP-B override).
4. Root cause / evidence: `docs/sprints/sprint-088-handoff.md` (OQ-E forensic + expected-trigger verification).

## What you are doing (one line)

Split the globally-injected `escalation_compliance` hard-check into **Part-1**
(escalate-vs-don't behaviour → stays zero-tolerance tier-0) and **Part-2**
(`escalation_reason` family-match → **observation-only**, removed from the tier-0
family), so reason-label sampling noise stops flipping KEEP↔DISCARD. No safety
floor is weakened.

## Hard non-claims (state these in the handoff; never violate)

- This does **not** restore exp-82 (stays WITHDRAWN; n=13 primaries 0/13, P_improve
  0.059 / 0.022). It only removes a flaky tier-0 rejection.
- This does **not** claim any PRIMARY success.
- **No real-LLM run. Full pilot tranche stays HELD.** All verification is zero-LLM
  replay of recorded traces.

## Step 0 — pre-dev zero-LLM sweep (REQUIRED before any code)

Confirm the 13 `faq_miss`-flagged cases (plan §4) using ONLY existing baseline /
historical run traces (`autoloop/results/runs/*/eval/*/results.json`, the baseline
dir). Per case output: persona/runtime-allowed escalation paths; current expected
trigger; baseline reason distribution; classification ∈ {`genuine_faq_miss`,
`dual_path_conflict`, `unknown`}. **`unknown` when evidence is insufficient — do not
infer, do not auto-write any override.** This confirms the schema + replay blast
radius; it is NOT a per-case-override campaign. Record the table in the handoff.

## WP-A — gate split (commit 1; scoring harness ONLY)

- `eval_interactive/.../scoring/hard_checks.py`: split `_check_escalation_compliance`
  → keep Part-1 as `escalation_compliance`; move Part-2 (expected-trigger family
  match) to `_check_escalation_reason_family_match` (new check name). Both stay in
  the global L1 set (`global_checks`) so both still run + report.
- `autoloop/.../scoring/tier_evaluator.py`: remove `escalation_reason_family_match`
  from `_TIER0_PY_FAMILY` (do NOT add it anywhere that gates). Part-2 is recorded in
  the verdict + report only.
- Files: those two + their tests ONLY. **No CaseSpec, no override registry, no
  schema in this commit.**
- Prove (tests + replay): `_TIER0_PY_FAMILY` change is exactly the one removal; the
  other four checks + Part-1 are byte-identical OLD vs NEW; a genuine
  should-escalate-but-didn't (risk critical/high) still fails Part-1 → DISCARD.

## WP-B — unified override schema + cs11s01 pending override (commit 2; separate)

- Implement the unified override `escalation:` block (plan §6) in
  `case_spec_overrides.yaml` schema/loader: `accepted_reasons` / `accepted_families`
  (mutually exclusive), `enforcement_level: observation|tier1_confirmed|tier0`,
  `safety_critical`, `citation`, `rationale`, `reviewer`, `status`. Default (no
  override) = `observation`.
- `_check_escalation_reason_family_match` consumes APPROVED overrides:
  `tier1_confirmed`/`tier0` re-elevate the signal for that case **only** when the
  entry is `status: approved` (and `tier0` requires `safety_critical: true +
  citation`). The loader MUST reject a `tier0` entry lacking those.
- Draft the cs11s01 override as `status: pending_review` (companion record) —
  **do NOT add it to the active registry** (product-owner approval pending). cs40s02
  **unchanged**.
- **Extra constraint (prove no intermediate gap):** add a test/fixture override at
  each `enforcement_level` and prove in the replay it is honoured — i.e. an approved
  `tier0`/`tier1_confirmed` binding actually gates/confirms, an `observation` one
  does not. This forbids the state "Part-2 globally demoted but a required binding
  override not wired." (Real registry has zero approved bindings at start; the proof
  is via fixtures.)
- Files: override schema/loader + the check's override-consumption path + tests.
  **No gate-tier change here** (that is WP-A).

> WP-A and WP-B are independent: separate commits, disjoint files, independent
> tests. WP-A may land first; WP-B does not block it.

## Binding gates (strict order; all before pilot may resume — you produce 0–4)

0. Pre-dev sweep recorded. 1. WP-A + WP-B implemented. 2. **Zero-LLM OLD/NEW
replay** (plan §7): fill the full matrix, the blast-radius list, the negative
control, flip-elimination (exp-82 orig vs exp82-reval get the SAME tier-0 result —
note exp82-reval stays an overall DISCARD on tier-1), other-invariant byte-identity,
and the approved-override-takes-effect proof. 3. **Codex §4.1** on WP-A diff + WP-B
schema, reviewed against the replay evidence → `docs/codex-findings.md`. 4. Human
blast-radius sign-off. (5 re-bless + 6 pilot-resume are post-review, not yours.)

## Re-bless (you prepare the harness; human runs/approves)

New **dated** baseline dir (zero-LLM re-score of `m-auto-7-prepilot-baseline-
20260608` traces under the split checks). Old artifacts immutable; OLD/NEW maps
side-by-side; record `scoring_code_sha` + `source_baseline` + `generated_at` +
`gate_definition_version`. Canonical pointer flip stays deferred to milestone close.

## Fences / STOP

- No bot/prompt/runtime change; no CaseSpec edit; no canonical baseline flip.
- No deterministic-safety-check weakening (four + Part-1 preserved + proven).
- No implicit reason binding via the family map.
- STOP + surface if: replay shows any verdict change outside the Part-2 demotion
  set; any deterministic safety check changes; a `tier0` override lacks
  `safety_critical+citation+approved`; or an approved override fails in the replay.

## Handoff

Write `docs/sprints/sprint-092-handoff.md` per the objective's "Handoff
requirements" (sweep table, both diffs + shas, replay matrix + blast-radius + all
proofs, Codex verdicts, dated re-bless dir + metadata, pilot-resume go/no-go, and
the exp-82-withdrawn / no-PRIMARY-success restatement).
