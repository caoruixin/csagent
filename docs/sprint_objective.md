---
title: Sprint 088 / S-Auto-33 (M-Auto-7 S-Y2) — CS4 entity-context autoloop pilot (CORE GATE)
doc_tier: current-runtime
status: current
implementation_status: not_started
source_of_truth: this file
last_reviewed: 2026-06-09
review_cadence: per sprint
supersedes: docs/sprints/sprint-087-objective.md
superseded_by: null
notes: >
  The M-Auto-7 CORE GATE. Part C.1 (pre-pilot baseline re-bless) is DONE
  (2026-06-08, m-auto-7-prepilot-baseline-20260608, n=9, git_commit 92c4076):
  GAP CONFIRMED — anti-误杀 control passes 1.0, Tier-1 targets stably fail,
  safety floor clean. This contract drafts Part C (autoloop authors a skill-yaml
  procedure candidate → human §4.1 review → merge) + Part D (milestone-close
  re-bless + per-sub-sprint Codex + close). Scope fence: skill-yaml procedure
  only — no Java/runtime, no CaseSpec/fixtures/scoring, no baseline_dir /
  current_eval_baseline canonical flip (deferred to milestone close).
  OQ-S87.baseline-dir RESOLVED (a) 2026-06-08: config.fitness.baseline_dir points
  at m-auto-7-prepilot-baseline-20260608 as the pilot fitness baseline (pilot
  setup only — NOT a canonical flip; current_eval_baseline.md unchanged). See
  §Baseline policy.
---

# Sprint 088 / S-Auto-33 — CS4 entity-context autoloop pilot (CORE GATE)

## Class

| Field | Value |
|---|---|
| **Layer (§3.2)** | `semantic_planner` (autoloop-authored skill-yaml procedure that guides the LLM's entity-context-verification choice) + `eval_spec`/governance (pilot acceptance) |
| **§7 stanza** | **REQUIRED** (semantic-touching) — see §7 below |
| **Per-sub-sprint Codex (§4.3)** | **REQUIRED** — the autoloop-authored procedure candidate is the §1.7 anti-hardcode binding gate (trigger #2). Must complete before milestone close (and before S-B if S-B carries). |

## Goal

Run the CS4 entity-context autoloop pilot **end-to-end** (the M-Auto-7 CORE
GATE): the autoloop authors ≥1 **skill-yaml procedure candidate** against the
S-Y1 Part B CaseSpecs on the honest pre-pilot baseline; the human reviews each
candidate under the §4.1 nine-question kernel + §1.7; an accepted candidate is
merged + re-blessed. **The pilot RUNNING is the gate; the candidate flip is the
success metric** (milestone §5.1.5). A `§3.4` hand-authored fallback (same
mutable surface, same CaseSpec gate, same §4.1 review) is the valid alternate
outcome if no autoloop candidate is acceptable.

## Pre-pilot baseline evidence (Part C.1 — DONE 2026-06-08)

Re-bless `eval_interactive/results/m-auto-7-prepilot-baseline-20260608/`
(`git_commit=92c4076`, n=9, `primary_model=deepseek-v4-flash`; `baseline_dir`
NOT moved). **GAP CONFIRMED:**

| Case | Role | Verdict | pass_rate | stability |
|---|---|---|---|---|
| `cs_uc_a_generic_policy_question` | anti-误杀 control | **PASS** | 1.0 (11/11) | stable |
| `cs_uc_a_no_ad_id_ad_specific` | Tier-1 primary target | **FAIL** | 0.0 | stable |
| `cs_uc_a_loaded_listing` | Tier-1 primary target | **FAIL** | 0.09 | stable |
| `cs_uc_a_lookup_failed` | Tier-2 neighbor | FAIL | 0.09 | stable |
| `cs_uc_fp_loaded_moderation` | Tier-2 neighbor | FAIL | 0.0 | stable |

**Safety floor CLEAN** — every sensitive case (anchor `uc_g/h/i/j` + shadow
`cs38s01/cs38s02`) escalated; zero unsafe self-resolve. CS4 cases had zero infra
noise.

### Carried flags (recorded; non-blocking for this pilot)

1. **`cs_uc_a_lookup_failed` demotion rationale CORRECTED** — it is NOT a case
   "the baseline already handles" (that came from a lucky single isolated run);
   under n=9 it stably FAILS (0.09). Reclassified as a **harder adjacent Tier-2
   neighbor / graceful-degradation regression guard**, not a primary target.
   Corrected in `cs_uc_a_lookup_failed.yaml` (`demotion_note` + `closure_criterion`)
   + `bad_cases/_manifest.md` Finding #1b.
2. **OQ-S87.uc-j-loop-detected (observation, NOT a safety breach)** —
   `anchor_outcome_uc_j_safety` had 1/11 attempts hit `loop_detected` (didn't
   escalate, but also did NOT pass → no unsafe containment). Carry to the
   milestone §5.6 safety review as an observation; not a safety-floor failure.
3. **OQ-S87.shadow-infra-noise (infra OQ, does not affect the CORE GATE)** —
   shadow `cs59s01`/`cs59s02` were `non_comparable` (0/11 valid, HTTP-400
   empty-form session-create) — reconfirms the existing
   `R-shadow-fixture-empty-form-session-create-400` (action_bank §5); plus ~5
   TIMEOUT + ~13 `CONTRACT_VIOLATION:active_use_case` scattered across suites,
   all invalidated/excluded from the majorities. Confined to shadow; bad_cases +
   anchor verdicts trustworthy.

## Tier classification (refines milestone §5.1 per the n=9 re-bless)

- **Tier-1 PRIMARY success targets** (MUST flip to PASS post-merge + re-bless):
  `cs_uc_a_no_ad_id_ad_specific` + `cs_uc_a_loaded_listing`.
- **anti-误杀 negative-control** (MUST STAY PASS — the binding anti-误杀 gate on
  any candidate): `cs_uc_a_generic_policy_question`.
- **Tier-2 neighbors** (should IMPROVE or HOLD; NOT a primary success target,
  NOT a fail-gate): `cs_uc_a_lookup_failed` + `cs_uc_fp_loaded_moderation`.
- **2 EXTEND cases** (stay green): `alice_uc_a_uc_h_misclass`,
  `wmkb_uc_a_trader_flag_secondary_uc_h`.
- **Safety + grounding floor unchanged** (HARD gate).

> **Refinement note:** milestone §5.1.2/§5.1.4 listed `cs_uc_a_lookup_failed`
> as a third Tier-1 target. The n=9 re-bless demotes it to a Tier-2 neighbor
> (stable fail; harder adjacent neighbor — see carried flag #1). Tier-1 success
> is the **two** primary targets only.

## Scope (Part C + Part D)

### Part C — autoloop authors → human reviews → merge

1. **Pilot setup.** Clean committed tree (the autoloop sweeps the staged index —
   `R-autoloop-run-sweeps-dirty-index`; NEVER run on a dirty tree). Backend up +
   on current code. **Comparison baseline = the pre-pilot baseline:**
   `config.fitness.baseline_dir` is set to
   `eval_interactive/results/m-auto-7-prepilot-baseline-20260608` (pilot setup
   only — see **§Baseline policy**). Do NOT touch `docs/current_eval_baseline.md`.
2. **Run the loop:** `cd autoloop && uv run python -m autoloop preflight` then
   `… run -n <N>` (start small, e.g. `-n 8`, scale if no keep). The hill-climber
   proposes edits to the **6×4 mutable surface only** (program.md §2:
   `discover_triage` / `resolve_faq_grounded_answer` / `resolve_intake_collect_and_handover`
   / `confirm` / `escalate` / `terminal` × `$.procedure` /
   `$.grounding_instruction` / `$.escalation_policy` / `$.critical_steps[*].desc`)
   and keeps only candidates that strictly improve the 4-tier fitness without
   shadow/safety regression. (The CS4 entity-context guidance most plausibly
   lands in `resolve_faq_grounded_answer.yaml` `procedure`/`grounding_instruction`
   or an existing `critical_steps[*].desc` — but the loop chooses; do not
   pre-script the target.)
3. **Inspect candidates:** `… report` / `… audit --experiment <id>`. Record each
   kept candidate's skill-yaml AST diff. (If `-n` yields 0 keeps, scale `-n` or
   invoke the §3.4 fallback — step 6.)
4. **Human §4.1 review (BINDING GATE):** present each candidate's diff for the
   human's §4.1 nine-question kernel + §1.7 review. Reject any semantic hardcode
   — per-UC if-else, `user_message` keyword check, enum widening, Java guard,
   raw-eval-phrase encoding.
5. **Merge accepted candidate:** `… apply --experiment <id>` (Hybrid: cherry-pick
   to the working branch + emit a proposed `config.yaml` baseline_dir patch +
   NO auto-commit). Human commits the cherry-picked skill-yaml diff only.
6. **§3.4 fallback (valid alternate outcome):** if NO autoloop candidate is
   acceptable, deliver/dev **hand-authors** the procedure text within the SAME
   6×4 surface, held to the SAME CaseSpec gate + the SAME §4.1 review. The pilot
   still produces a go/no-go + a merged, gated procedure.

### Part D — re-bless → Codex → close

7. **Milestone-close re-bless** (the 2nd of the two real-LLM gates; §8): on a
   clean committed tree, `cd autoloop && uv run python scripts/rebless_baseline.py
   --n 9 --out-dir ../eval_interactive/results/m-auto-7-close-baseline-<date>`.
   This WRITES a new dated baseline dir and does **NOT** flip
   `config.fitness.baseline_dir` / `current_eval_baseline.md` (that canonical flip
   is a separate, human-authorized milestone-close decision).
8. **Confirm SUCCESS** on the close re-bless: Tier-1 primary targets
   (`no_ad_id` + `loaded_listing`) flip to PASS; the anti-误杀 control STAYS PASS;
   Tier-2 neighbors improve/hold; the 2 EXTEND cases stay green; safety +
   grounding floor unchanged. (Per milestone §5.1.5 the pilot RUNNING is the
   gate; a flip is the success metric, the §3.4 fallback is a valid outcome.)
9. **Per-sub-sprint Codex §4.1 review (REQUIRED)** of the merged skill-yaml diff
   (the §1.7 binding gate; §4.3 trigger #2) — `compact/M-Auto-7-S-Y2-review-prompt.md`.
10. **Handoff + close** per §Handoff requirements.

## Hard fences / STOP conditions

**From the human (2026-06-08) + milestone §6 (S-Y1/S-Y2) + program.md §2/§3:**

- **Only a skill-yaml procedure candidate.** The mutable surface is the
  program.md §2 6×4 set; every other byte is structurally out of reach.
- **No Java / runtime change** (`server/src/main/java/**` byte-identical).
- **No CaseSpec / fixtures / scoring change**
  (`eval_interactive/case_specs/**`, `case_specs_shadow/**`, mocks, scoring code
  byte-identical). The negative-control CaseSpec is the anti-误杀 gate — do NOT
  edit it to make a candidate pass (§5.4).
- **No `baseline_dir` / `current_eval_baseline.md` canonical flip** in this
  sub-sprint (the Part D re-bless writes a new dir only; the canonical flip is a
  separate human-authorized milestone-close edit).
- **No per-UC if-else on `user_message`; no Java guard blocking
  `classify_use_case(UC-A)` on removed ads; no projecting raw
  `moderation_reason_text` (boolean only); candidate-UC name field stays
  additive.**
- **No new `critical_steps` entry / no delete / no reorder** (program.md §2 —
  only `critical_steps[*].desc` text is mutable).
- **No `tools_required` / `trace_check` / `applicable_use_cases` /
  `guardrails` edit** (Runtime-owned schema; program.md §6).
- **No cross-Skill / cross-file diff per candidate** (program.md fence #9).
- **autoloop output is a PROPOSAL — the human §4.1 review is the binding gate.**
- **No new Tier-0 invariant.**
- **STOP** and escalate to the human if: the loop proposes outside the 6×4
  surface (sandbox should reject — if it doesn't, that's a milestone bug); no
  candidate is §4.1-acceptable after a reasonable `-n` (→ invoke §3.4 fallback);
  the anti-误杀 control would regress under any candidate (reject the candidate).

## Test / eval requirements

- **Pilot fitness** is the autoloop's own 4-tier lexicographic evaluation
  (`bad_cases` + `anchor_outcome` + `shadow`) against the pre-pilot baseline;
  shadow detail is firewalled from the meta-agent (program.md §3.B.8).
- **Acceptance evidence = the Part D milestone-close re-bless** (real-LLM, n=9),
  read against the §Tier classification bars. Mocked-LLM is not primary evidence
  (§5.7).
- **Safety floor + grounding floor unchanged** (HARD).
- **§5.6 human review** of the merged candidate's bad-case traces is the primary
  gate (per badcase-lifecycle §5.6).

## §7 Layer-classification + anti-hardcode stanza

**Target failure layer:** `semantic_planner` (the autoloop-authored skill-yaml
`procedure` / `critical_steps[*].desc` guides the LLM's entity-context
verification choice on ad-specific UC-A questions). Secondary surface:
`eval_spec`/governance (pilot acceptance).

**Tier-0 invariant:** This sprint adds no Tier-0 invariant.

**Semantic hardcode:** No semantic hardcode introduced. The mutable surface
(program.md §2) is LLM-soft `procedure` / `grounding_instruction` /
`escalation_policy` / `critical_steps[*].desc` text. Defenses against a hardcode
slipping in: the `anti_hardcode_check.py` propose-stage sandbox (Q1/Q2/Q4/Q5),
the human §4.1 nine-question kernel review (the binding gate), and the anti-误杀
negative-control (`cs_uc_a_generic_policy_question`) which fails the fitness for
any candidate that over-elicits / forces ad_id on generic questions. The §3.4
hand-authored fallback is held to the same §4.1 review. Sunset plan: n/a (no
hardcode introduced).

**Generalization coverage:** target / neighbor / negative / shadow case counts:
**2 / 4 / 1 / 22** — targets = `no_ad_id` + `loaded_listing`; neighbors =
`lookup_failed` + `fp_loaded_moderation` + the 2 EXTEND cases; negative-control =
`generic_policy_question`; shadow = the 22-case held-out suite (dev-blind via the
autoloop shadow firewall).

## Codex review plan (§4.3)

**Per-sub-sprint Codex REQUIRED** (trigger #2 — autoloop output is the §1.7
binding gate). Review the merged skill-yaml diff under the §4.1 nine-question
kernel; verdict to `docs/codex-findings.md` (§4.2 header). Must complete before
milestone close. Prompt: `compact/M-Auto-7-S-Y2-review-prompt.md` (deliver-agent
authors at Part D).

## Handoff requirements

`docs/sprints/sprint-088-handoff.md` must record: the `-n` used + keep count;
each kept candidate's skill-yaml AST diff + the human §4.1 verdict; the
accepted/merged candidate (or the §3.4 hand-authored procedure) + its commit; the
Part D close re-bless dir + the Tier-1/control/neighbor/EXTEND/safety read; the
Codex verdict; the three carried flags' disposition; and whether SUCCESS (flip)
or VALID-ALTERNATE (fallback / pilot-ran-no-flip) was reached.

## Commit discipline

Stage explicitly by file (NO `git add -A` — program.md fence #6 + autoloop
dirty-index hazard). The merged candidate commit contains ONLY the cherry-picked
skill-yaml diff. Deliver-agent close-bundle artefacts (`docs/sprint_objective.md`,
`docs/10-handoff.md`, `docs/action_bank.md`, handoff, etc.) are bundled by the
human at close. Re-bless result dirs are gitignored (data, not committed).

## Baseline policy (S-Y2) — OQ-S87.baseline-dir RESOLVED (a), 2026-06-08

Human-authorized: `config.fitness.baseline_dir` (`autoloop/config.yaml`) is
pointed at `eval_interactive/results/m-auto-7-prepilot-baseline-20260608` as the
**S-Y2 pilot fitness / experiment baseline**. Explicit terms:

- `m-auto-7-prepilot-baseline-20260608` = the **pilot fitness baseline**
  (`config.fitness.baseline_dir`). This is a **pilot-setup change only**.
- `m-auto-6-baseline-shared-20260607` = the **previous canonical baseline**.
- `docs/current_eval_baseline.md` **remains UNCHANGED** — this is **NOT** a
  canonical baseline promotion; the canonical baseline is **NOT** updated before
  milestone close.
- After the S-Y2 candidate is merged + the Part D milestone-close re-bless +
  Codex review, **whether to promote the new result to canonical baseline is a
  separate human close decision** (the canonical-baseline flip).
- S-Y2 does **NOT** change Java / runtime / CaseSpecs / fixtures / scoring; the
  mutable surface stays limited to a **skill-yaml procedure candidate**
  (program.md §2 6×4 surface).
