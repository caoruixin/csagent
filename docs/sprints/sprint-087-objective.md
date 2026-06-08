---
title: Sprint 087 / S-Auto-32 (M-Auto-7 S-Y1.5) — Pre-pilot autoloop substrate patch
doc_tier: current-runtime
status: current
implementation_status: not_started
source_of_truth: this file
last_reviewed: 2026-06-09
review_cadence: per sprint
supersedes: docs/sprints/sprint-086-objective.md
superseded_by: null
notes: >
  INSERTED micro sub-sprint (deliver-agent + human decision 2026-06-09,
  Option B from docs/solutions/2026-06-08-autoloop-mechanism-audit-and-feedback-loop-tightening.md).
  Slots BETWEEN S-Y1 (Sprint 086, closed) and S-Y2 (the CORE GATE). Fixes
  three feedback-loop defects (P0-A/P0-B/P0-C) + one amplifier (P1) in the
  autoloop meta-agent that the S-Y2 run-1 validation tranche (exp-66/67/68;
  0/3 on-gap, 2/3 phase-incorrect) surfaced. Pure autoloop INFRA: touches only
  autoloop/autoloop/meta_agent/{proposer,analyzer}.py + autoloop/autoloop/loop.py
  + meta_agent/prompts/{analyze,propose}.txt + autoloop/config.yaml (new `pilot`
  + `lessons.enabled`) + autoloop/tests/*. NO agent semantic surface, NO
  scoring/gate/sandbox/mutable-surface/program.md/baseline_dir edit → NO
  re-bless, NO §5.6 bad-case rerun. The S-Y2 CORE-GATE contract is parked at
  compact/sprint-088-objective-PARKED.md (re-promote as Sprint 088 / S-Auto-33
  at S-Y1.5 close; the pre-pilot baseline evidence + baseline policy in it stay
  valid). Audit verdict was Option 2 (no go/no-go-blocking flaw); this patch
  tightens the feedback loop so S-Y2 Part C has a realistic PRIMARY-SUCCESS
  probability instead of near-certain §3.4 fallback.
---

# Sprint 087 / S-Auto-32 — Pre-pilot autoloop substrate patch (M-Auto-7 S-Y1.5)

## Class

| Field | Value |
|---|---|
| **Layer (§3.2)** | `infra` — the eval framework's auto-evolution loop (meta-agent prompt builder + analyzer input-shaping + config schema for pilot-target steering). §5.8 framework-defect priority applies *in spirit*: a meta-agent that locks the search direction is equivalent to a half-broken scorer still scoring. |
| **§7 stanza** | **REQUIRED** — P0-C introduces the `config.yaml:pilot` block, a new config-driven steering surface that the stanza must classify as NOT a semantic hardcode. See §7 below. |
| **Per-sub-sprint Codex (§4.3)** | **REQUIRED** — trigger #3 (§7-REQUIRED, new steering surface near the §1.7 "raw eval phrase" line). Must complete before S-Y2 Part C begins. |

## Goal

Tighten the autoloop feedback loop so the S-Y2 CORE-GATE pilot can search
**toward** the CS4 entity-context gap instead of hunting the pre-CS4
escalation cluster. Concretely: give the proposer (a) a real feedback gradient
(`tier_breakdown` on its prior candidates), (b) a failure landscape that
reflects what its **candidates** regressed (not just the static baseline), and
(c) explicit awareness of the active pilot's **PRIMARY TARGETS** + phase/UC
hints — and stop the May-31 lessons from steering it at a pre-CS4 cluster. The
downstream success metric is **observed in S-Y2 Part C**, not here: the
`full_on_gap_hit_rate` on the next `-n 8` tranche.

## Why this sub-sprint exists (insertion rationale)

The S-Y2 run-1 validation tranche (exp-66/67/68, 2026-06-09) produced **0 keeps
/ 3 candidates, all targeting `$.escalation_policy`, 0 touching the CS4
entity-context gap, 2/3 editing phase-incorrect skills** (confirm /
discover_triage — CS4 cases never reach CONFIRM/ESCALATE on the success path).
This is structural, not sampling noise. Root causes (code-grounded in the audit
doc §5):

- **P0-A** — `proposer.py` / `analyzer.py` strip `verdict.tier_breakdown` from
  the recent-iteration rows; the proposer sees only a one-line `discard_reason`
  string → no feedback gradient (exp-67 → exp-68 failed the *same case*
  `cs11s01` ~90 min apart without learning).
- **P0-B** — the analyzer only reads `baseline_dir`; the failure taxonomy never
  reflects regressions the *candidates* introduced → "taxonomy is one thing,
  the gate judges another."
- **P0-C** — the proposer has zero concept of the pilot's PRIMARY TARGETS;
  CS4 cases sit mixed in with cs001/cs011/cs014/cs11s01/cs11s02 → prior of
  picking CS4 ≈ 1/(failing-case-count) < 10%.
- **P1** — the May-31 lessons (authored 8 days before the CS4 cases existed)
  explicitly steer the proposer at the pre-CS4 escalation cluster; run-1
  rationales literally cite `L-2026-05-31-004/005` before regressing the case
  they cite.

Full analysis: `docs/solutions/2026-06-08-autoloop-mechanism-audit-and-feedback-loop-tightening.md`.

## Scope (5 items — execute in order; P0-A before P0-B before P1)

All anchors HEAD-verified at `a80fbe5` (the research doc cited `8b529ce`; line
numbers re-verified on the working tree).

### #1 — P0-A: tier_breakdown passthrough (~30 LOC)

The data is **already present** in the record dict (`loop.py:653-668`
`_record_to_dict_for_meta_agent` carries `verdict.tier_breakdown` at line 667);
the two prompt builders drop it.

1. `autoloop/autoloop/meta_agent/proposer.py:_build_user_input` (line 158-207):
   add `"tier_breakdown": r.get("verdict", {}).get("tier_breakdown", {})` to
   each serialized RECENT_ITERATIONS row (keep the existing 5 fields).
2. `autoloop/autoloop/meta_agent/analyzer.py:_build_user_input` (line 155-183):
   same addition to each RECENT_ITERATIONS_SUMMARY row.
3. No new agent-runtime signal is generated — an existing structured artefact is
   surfaced into the loop's already-existing decision layer.

### #2 — P0-B: candidate-results passthrough + shadow-firewall extension (~80 LOC + ~30 LOC prompt) — HIGHEST RISK

1. Add an optional param `recent_candidate_results: list[dict] | None = None` to
   `analyzer.analyze(...)` (analyzer.py:58).
2. In `loop.py` (around 158-167), read the last K candidate iterations'
   persisted per-suite results (written by `_persist_eval_traces`,
   loop.py:294/801). K from a new `meta_agent.recent_candidate_results_k`
   (default 3).
3. Summarize them with a candidate-side analog of
   `analyzer.build_baseline_summary` (analyzer.py:88-134) that **REUSES the
   existing shadow firewall** (lines 99-111: shadow suite → aggregate counts
   only; per-case shadow detail FORBIDDEN). This is the hardest correctness
   point: breaking the firewall = milestone-level failure (program.md §3.A.10).
4. Feed a new `CANDIDATE_RESULTS_SUMMARY` block into `analyze.txt`; instruct the
   LLM to label each failing case `BASELINE_FAILURE` vs
   `CANDIDATE_INTRODUCED_REGRESSION`.
5. **Hard-required test** (see Test reqs): a mock with shadow per-case content
   → assert the serialized analyzer prompt contains **no** shadow signature
   (`cs59s`, `case_specs_shadow`, any shadow case_id) + a `cs59s*`-style regex
   blacklist as belt-and-suspenders.

### #3 — P0-C: pilot-target steering (~80 LOC Python + ~40 LOC prompt + ~20 LOC config)

1. **New `autoloop/config.yaml:pilot` block** (placed near `fitness:`):
   ```yaml
   pilot:
     schema_version: 1
     active_sprint: "S-Y2"
     primary_targets:        # MUST flip to PASS post-merge (S-Y2 success)
       - cs_uc_a_no_ad_id_ad_specific
       - cs_uc_a_loaded_listing
     anti_kill_control:      # MUST STAY PASS (binding anti-误杀 gate)
       - cs_uc_a_generic_policy_question
     tier2_neighbors:        # should improve/hold; NOT a fail-gate
       - cs_uc_a_lookup_failed
       - cs_uc_fp_loaded_moderation
     phase_hint: [DISCOVER, RESOLVE]
     use_case_hint: [UC-A]
   ```
2. **Config-load-time validator** (in `loop.py` or a small new
   `autoloop/autoloop/config_validator.py`): reject any pilot case_id that
   appears in `eval_interactive/case_specs_shadow/` (read the directory
   **filenames only**, never contents — preserves the shadow firewall). A
   config-load error stops the loop before any iteration runs. Also: if the
   `pilot` block is ABSENT, fall back to empty `PILOT_PRIMARY_TARGETS`
   (pre-S-Y1.5 behaviour) — backward-compatible for non-targeted future loops.
3. **`SKILL_PHASE_USECASE_MAP`**: lazy-read (no cache) the 6 mutable Skill YAMLs'
   `applicable_phases` / `applicable_use_cases`; serialize into `analyze.txt` +
   `propose.txt`.
4. **`analyze.txt`**: instruct the analyzer to tag each
   `bad_cases_regressing` / `anchor_outcome_closure_criterion_fails` entry with
   a `target_role` ∈ {`primary`, `anti_kill_control`, `tier2_neighbor`,
   `general`} based on `PILOT_PRIMARY_TARGETS` membership.
5. **`propose.txt`**: add under the existing Inputs/heuristics —
   - selection BIAS: prefer skill files whose `applicable_use_cases` ∩
     `pilot.use_case_hint` AND `applicable_phases` ∩ `pilot.phase_hint` are
     non-empty; non-bias targets ALLOWED but the rationale MUST justify them;
   - "NEVER edit a Skill in a way you expect to regress an `anti_kill_control`
     entry — the pilot fails if the control regresses";
   - **labels-only directive** under the existing "# Forbidden patterns (§1.7
     Constitution — Forbidden)" section (propose.txt:32-58), the literal
     sentence: *"The primary target case IDs in PILOT_PRIMARY_TARGETS are
     evaluation bookkeeping labels. Use them ONLY to prioritize which failure
     cluster to analyze. Do NOT mention, encode, paraphrase, or create rules
     around these IDs or their literal fixture wording in any proposed
     after_value."* (Unit-tested for literal presence.)
6. **Field-level steering is OUT OF SCOPE** (do NOT tell the proposer "edit
   `$.procedure` not `$.escalation_policy`") — that is too close to the §1.7
   if-else-dump line. Phase/UC steering + PILOT_PRIMARY_TARGETS is sufficient;
   the audit MEASURES field-correctness (see #5) rather than prescribing it.

### #4 — P1: lessons opt-out (~5 LOC + 1 config line)

1. Add `lessons.enabled: false` to `autoloop/config.yaml:40`.
2. When false, the `propose.txt` LESSONS_MD block (proposer.py:185-186) becomes
   the placeholder `<lessons disabled for this run — historical lessons may not
   reflect the active pilot's targets>` (NOT an empty string — empty reads as a
   truncated prompt).
3. Preserve `lessons.md` on disk (forensic); the compactor still runs.
4. S-Y2 default = **false** (May-31 lessons pre-date the CS4 baseline by 8 days).

### #5 — Supplements: per-exp pilot snapshot + 4-layer hit-rate audit (~70 LOC)

1. **Per-exp snapshot** — embed in each `experiments.jsonl` row
   (`loop.py:_iteration_record_to_log_dict`, ~514) AND each
   `runs/<id>/hypothesis.json` (loop.py:705) a `pilot_snapshot` block
   (schema_version + the case_id lists + UC/phase hints + a 16-char
   `block_sha256` over normalized-JSON) + a `lessons_enabled` flag. Compute once
   per iteration at start. Forensic-only; shadow firewall holds by construction
   (snapshot can never contain a shadow case_id — the #3 validator forbids it).
2. **4-layer hit-rate audit metric** — extend `autoloop report` / `audit`
   output (no gate/baseline touch) with, computed from `SKILL_PHASE_USECASE_MAP`
   ∩ pilot hints:
   - `phase_usecase_hit_rate` — target_skill covers UC-A + DISCOVER/RESOLVE;
   - `skill_hit_rate` — target_skill == `resolve_faq_grounded_answer.yaml`;
   - `field_family_hit_rate` — target_field ∈ {`$.procedure`,
     `$.grounding_instruction`, `$.critical_steps[*].desc`} (NOT
     `$.escalation_policy`);
   - `full_on_gap_hit_rate` — all three (the **S-Y2 primary success metric**);
   - `partial_hit_breakdown` — venn-style counts (skill-only / field-only /
     phase_usecase-only / full).

## Hard fences / STOP conditions

**Edits CONFINED to:** `autoloop/autoloop/meta_agent/{proposer,analyzer}.py`,
`autoloop/autoloop/loop.py` (+ optional tiny `config_validator.py`),
`autoloop/autoloop/meta_agent/prompts/{analyze,propose}.txt`,
`autoloop/config.yaml` (new `pilot` + `lessons.enabled` + optional
`meta_agent.recent_candidate_results_k`), `autoloop/tests/*`. Nothing else.

**Forbidden (any of these = STOP + escalate):**
- No edits to `autoloop/autoloop/scoring/*.py` (would trip
  `gaming.scoring_code_drift`; `scoring_code_baseline_sha` `0d86b08f…` MUST stay
  stable → preserves the no-re-bless property).
- No edits to `autoloop/autoloop/sandbox/*.py` (mutable-surface sandbox /
  anti_hardcode / content_validator / applier).
- No edits to `autoloop/program.md` (locked under M-Auto-1A §8; changing it
  needs program.md-v2 milestone authorization — NOT this sub-sprint).
- No edits to `autoloop/config.yaml:mutable_surface` or `fitness.baseline_dir`
  (stays `m-auto-7-prepilot-baseline-20260608`).
- No edits to `eval_interactive/**`, `server/**`, `data/**`,
  `docs/foundational/**`, `docs/runtime_freeze_and_risk_policy.md`,
  `docs/sprints/*`, `docs/milestones/*`.
- **Shadow firewall MUST be preserved**: the #2 candidate-results passthrough
  aggregate-only-filters shadow output before serialization (unit-tested).
- **`pilot.primary_targets` / `anti_kill_control` / `tier2_neighbors` MUST NOT
  contain any shadow case_id** — the #3 validator rejects at config-load (reads
  `case_specs_shadow/` filenames only).
- **No field-level steering in the prompt** (phase/UC hints only).
- Not changing the 5-layer gate logic, the anti-hardcode detector, or the
  lessons-compactor logic (only adding the `enabled` flag).
- No new Tier-0 invariant.
- **STOP** and escalate if: a required anchor has drifted from this contract;
  the shadow-firewall test cannot be made to pass cleanly; or any change would
  require touching a forbidden file to work.

## Test / eval requirements

**No re-bless. No §5.6 bad-case rerun.** (No agent semantic surface touched —
config-driven steering + prompt-input shaping only.)

- `autoloop/tests/test_meta_agent.py` — fixtures updated to carry
  `tier_breakdown`; **4 new tests**:
  - `test_recent_iterations_passthrough_includes_tier_breakdown` (P0-A)
  - `test_candidate_results_passthrough_filters_shadow_per_case` (P0-B —
    feed mock with shadow per-case content; assert prompt body has no
    `cs59s` / `case_specs_shadow` / shadow case_id signature)
  - `test_pilot_primary_targets_block_serialized_in_propose_prompt` (P0-C —
    covers PILOT_PRIMARY_TARGETS + SKILL_PHASE_USECASE_MAP + the literal
    labels-only sentence)
  - `test_lessons_md_not_in_propose_user_input_when_disabled` (P1)
- `autoloop/tests/test_loop.py` — **2 new tests**:
  - `test_experiment_record_embeds_pilot_snapshot` (row has `pilot_snapshot`
    + stable `block_sha256`)
  - `test_experiment_record_embeds_lessons_enabled_flag`
- `autoloop/tests/test_cli_smoke.py` — **1 extension**: `autoloop run
  --dry-run -n 1` under a `pilot`-populated config → assert the propose prompt
  contains the PILOT_PRIMARY_TARGETS block and the proposer selects a
  phase-correct skill (resolve_faq / discover_triage / resolve_intake — any).
- The other ~24 autoloop tests stay green (baseline preservation).

**Close-gate dry-run rationale checklist** (record the result in the handoff;
any FAIL → do NOT close S-Y1.5, re-tune the prompt — guards against "prompt
injected but the model didn't listen"): run `autoloop run --dry-run -n 2` under
the S-Y2 pilot config and manually verify on both proposals:
1. Rationale does **not** cite `L-2026-05-31-004` / `L-2026-05-31-005`
   (confirms lessons-disable actually took effect).
2. The analyzer output tags ≥1 of the CS4 cases (`cs_uc_a_no_ad_id_ad_specific`
   / `cs_uc_a_loaded_listing`) as `target_role: primary`.
3. ≥1 target-skill rationale explains the UC-A / RESOLVE relevance (bonus if it
   picks `resolve_faq_grounded_answer.yaml`).
4. No `after_value` contains any case_id literal or fixture key phrase
   (labels-only rule holding).

## §7 Layer-classification + anti-hardcode stanza

**Target failure layer:** `infra` — autoloop meta-agent prompt builder +
analyzer input-shaping + config schema for pilot-target steering (the eval
framework's auto-evolution loop; §5.8 framework-defect priority applies in
spirit). No agent semantic surface is touched.

**Tier-0 invariant:** This sub-sprint adds no Tier-0 invariant. It preserves all
existing structural defenses (program.md §4): the mutable-surface sandbox, the
5-layer lexicographic fitness, the shadow-result firewall, the scoring-code
drift guard, the anti-hardcode detector, and the seven anti-gaming checks.

**Semantic hardcode:** No semantic hardcode introduced. The edits are (1) prompt
INPUT-shape changes surfacing artefacts that already exist (`tier_breakdown`
in the experiments log; per-iter `eval-results.json` already persisted) — the
shadow firewall is explicitly EXTENDED, not weakened; (2) config-driven STEERING
(`pilot` block = case_id lists + UC/phase hints) that biases skill SELECTION,
not a rule against any user utterance — the mutable surface stays the §2 6×4 set
and anti-hardcode Q1/Q2/Q4/Q5 still apply to `after_value`; (3) a `lessons.enabled`
opt-out flag; (4) forensic per-exp snapshot; (5) a labels-only propose.txt
sentence (defense-in-depth with anti-hardcode Q4). Codex must confirm the
proposer's resulting rationale describes "what kind of edit helps these cases",
not "search for text matching these case_ids" (the §1.7 "raw eval phrase" line).
Sunset plan: n/a (no hardcode introduced).

**Generalization coverage:** n/a — pure-infra sub-sprint; no target / neighbor /
negative / shadow CaseSpec touched. Validation = the unit tests above + the
`--dry-run -n 2` close checklist. The downstream success metric is the S-Y2
Part C `full_on_gap_hit_rate` (observed in the pilot, not here).

## Codex review plan (§4.3)

**Per-sub-sprint Codex REQUIRED** (trigger #3). Review the diff under the §4.1
nine-question kernel; verdict to `docs/codex-findings.md` (§4.2 header). Must
complete before S-Y2 Part C begins. Prompt:
`compact/M-Auto-7-S-Y1.5-review-prompt.md` (deliver-agent authors at close).

**Codex focus questions:**
- (P0-B) The candidate-results passthrough MUST read the per-iter
  `eval-results.json` and filter shadow to aggregate-only + a `cs59s*` regex
  blacklist. Independently verify the firewall test actually fails when the
  filter is removed.
- (P0-C) Confirm `pilot.primary_targets` / `anti_kill_control` exclude shadow
  case_ids (schema validator), `SKILL_PHASE_USECASE_MAP` is lazy-read (not
  cached), and spot-check 2-3 dry-run propose outputs that rationale describes
  the edit shape, not case_id matching.
- (P0-A) Confirm the passed-through `tier_breakdown.shadow_regression` exposes
  only aggregate keys (no per-case shadow detail).
- (P1) Confirm the disabled LESSONS_MD block is the placeholder string, not "".

## Handoff requirements

`docs/sprints/sprint-087-handoff.md` must record: the diff per scope item;
autoloop pytest count (was 324; +7 new); the shadow-firewall test evidence; the
`--dry-run -n 2` close checklist result (4 items PASS/FAIL + verdict); the
`pilot` block snapshot committed for S-Y2; confirmation that
`scoring_code_baseline_sha` is unchanged (no re-bless); the Codex verdict; and
any OQs (e.g. L-006 `consumed_by_proposer` disposition, pilot schema versioning).

## Commit discipline

Stage explicitly by file (NO `git add -A` — autoloop dirty-index hazard;
`autoloop run` sweeps the staged index). The dev scope is `autoloop/**` only.
Deliver-agent close-bundle artefacts (`docs/sprint_objective.md`,
`docs/milestone_objective.md`, `docs/10-handoff.md`, `docs/action_bank.md`,
handoff, the re-promoted Sprint-088 contract) are bundled by the human at close.
