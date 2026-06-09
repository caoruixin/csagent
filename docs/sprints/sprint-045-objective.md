---
title: Sprint 45 objective archive — Bad-case suite expansion + trial milestone-close dry-run (M3-Eval sub-sprint 4; S-Eval-4)
doc_tier: sprint-archive
status: archived
implementation_status: implemented
source_of_truth: this file
last_reviewed: 2026-05-22
review_cadence: ad hoc
supersedes: [docs/sprints/sprint-044-objective.md]
superseded_by: null
archive_notes: >
  Archived Sprint 45 contract at sub-sprint close 2026-05-22 (Sprint 45
  closed A — Clean PASS; Codex DEFERRED to M3-Eval milestone-shared
  close per `iteration_governance.md` §4.3 default — no per-sub-sprint
  trigger fires for S-Eval-4; dev commit `8b7ff40`; archive package:
  this file + `docs/sprints/sprint-045-handoff.md` 12 sections incl.
  §12 closure verdict appended by deliver-agent + human; NO separate
  Codex review archive at S-Eval-4 close since Codex deferred). Ships
  11 new bad-case YAMLs under `eval_interactive/case_specs/bad_cases/`
  sourced from the 17 approved entries in `case_spec_overrides.yaml`
  + `_manifest.md` lifecycle ledger + S-Eval-4 trial dry-run
  calibration note section + 1 §7-a planned-drift checkpoint-test
  update (S-Eval-1 anchor 1 → 12 count assertion). Total bad-case
  suite size at close: 12 (1 Alice + 11 new). Real-LLM run posture:
  Option A (synthetic / mocked trace; authorized 2026-05-22).
  Calibration deliverable: 11/11 well-calibrated or acceptable; 0
  rewrites applied; 1 minor `cs095` "placeholder" ambiguity flagged
  as fold-back candidate (OQ-S45.4). UC-G/H/I/J primary-corpus gap
  surfaced at planning round → fallback path (a) accept narrower
  coverage AUTHORIZED + new R-item `R-bad-case-suite-uc-ghij-seed-from-real-sessions`
  OPENED in `docs/action_bank.md` §5 governance-track backlog
  (status `proposed; impl deferred to M4+ planning`). 6 OQs disposed
  (4 NEW S-Eval-4 + 2 carry-overs from S-Eval-3): OQ-S45.1 routed to
  M3-Eval close manual review queue; OQ-S45.2 new R-item; OQ-S45.3
  flagged for M3-Eval close consideration (dual-encoding intentional);
  OQ-S45.4 fold-back candidate; OQ-S45.5 + OQ-S45.6 carry-overs
  preserved in M3-Eval-shared review queue. No §6 hard fence touched
  (verified empty `git diff` on Tier-0/Tier-1/Tier-2/Tier-3 scoring
  code + Skill abstraction + runtime semantic surfaces + existing
  case fixtures + governance/foundational/archives); no §10 STOP
  signal fired; §4.1 self-walk Q1-Q7 PASS / Q2 N/A / Q3 N/A / Q8
  filled / Q9 N/A. Contract drift ×1 §7-a planned (checkpoint-test
  count update per S-Eval-3 precedent). Java baseline 1163/1/0/2
  UNCHANGED; Python baseline `5 failed / 396 passed` via `uv run
  python -m pytest` UNCHANGED from S-Eval-3 close (per OQ-S44.6
  runner discipline carry-over). One non-blocking numbers-cite
  observation: dev handoff §3 + §11 cite "13 files" but actual
  commit is 14 files (under-counted itself; fold-back candidate at
  M3-Eval close to tighten `feedback_deliver_agent_cited_numbers_must_be_reproducible.md`).
  See handoff §12 for full closure verdict.
notes: >
  Sprint 45 is the FOURTH sub-sprint of NEW Milestone M3-Eval (Coarse-to-
  Fine Evaluation Architecture; see `docs/milestone_objective.md`).
  S-Eval-3 (Sprint 44) closed Clean PASS 2026-05-22 (dev commit
  `01770ac`; Codex per-sub-sprint review per §4.3 trigger #2 returned
  `decision: pass / blocking_count: 0` on first pass, single round;
  archive at `docs/sprints/sprint-044-objective.md` +
  `docs/sprints/sprint-044-handoff.md` + `docs/sprints/sprint-044-codex-review.md`).
  18 `critical_steps` populated across the 6 production Skill YAMLs
  (3 + 2 + 5 + 5 + 2 + 1; mandatory : advisory = 10 : 8); Codex
  independent §5.3 walk returned 18× row-1 ✅ on every populated `desc`.
  **S-Eval-4 UNBLOCKS** per §4.3 trigger #2 satisfied.

  **S-Eval-4 is the bad-case suite expansion + trial milestone-close
  dry-run sub-sprint.** It is a DATA sub-sprint (10-12 new bad-case
  YAMLs sourced from `eval_interactive/case_spec_overrides.yaml`
  `status: approved` entries — pool at HEAD is **17 approved entries**,
  NOT 29 as proposal §4 mistakenly stated; deliver-agent reconciled at
  S-Eval-1 launch 2026-05-20) PLUS a calibration deliverable (trial
  milestone-close manual review dry-run that walks each new bad case's
  trace and judges PASS / FAIL / IMPROVING against the case's
  `closure_criterion`; the load-bearing output is calibrated
  `closure_criterion` wording quality, NOT verified bot correctness).

  **Codex review plan**: milestone-shared at M3-Eval close per §4.3
  default (no per-sub-sprint trigger fires for S-Eval-4 by default;
  the only per-sub-sprint trigger in M3-Eval was S-Eval-3 §4.3 #2,
  already satisfied). S-Eval-4 contract is **data + calibration; not
  code change**; the dev does NOT touch Tier-0 / Tier-1 / Tier-2 check
  code (those are S-Eval-1 / S-Eval-2 / S-Eval-3 territory).

  **S-Eval-3 carry-over consumed at S-Eval-4 planning round**:
  - **Thin-corpus UCs preserved** (S-Eval-1 §12.7 + S-Eval-2 §12.7
    + S-Eval-3 §12.7): UC-G / UC-H / UC-I / UC-J = 0 anchor coverage;
    UC-F = 1; UC-B = 5. S-Eval-4 bad-case selection SHOULD prioritise
    UC-G / UC-H / UC-I / UC-J entries from the 17 approved overrides
    per S-Eval-1 close §12.7 direction. Re-verify D1-D4 × UC-FP/G/H
    coverage within 17 at S-Eval-4 planning round; if a dimension or
    UC family is undersampled, the 3 fallback paths from S-Eval-1
    launch constraint #2 (still NOT LOCKED): (a) accept narrower
    coverage; (b) supplement with non-override-sourced real sessions;
    (c) defer the missing dimension to a follow-on milestone with an
    R-item.
  - **Python baseline runner discipline (OQ-S44.6)**: all Python
    baseline citations in S-Eval-4 handoff §9 MUST use
    `uv run python -m pytest` (or whichever invocation reproduces
    consistently across local + Codex environments). Dev: do NOT
    cite via `uv run pytest` (stale shebang under some checkouts).
  - **OQ-S44.1 executor wiring carry-over (LOAD-BEARING)**: the
    populated `critical_steps` content is structurally INERT in the
    production eval-harness path until
    `eval_interactive/eval_interactive/batch/executor.py:252` is
    updated to pass `tier2_result=` to `compute_composite(...)`.
    S-Eval-4 trial real-LLM dry-run (if attempted; see §2 below)
    MAY surface this as the first observed real-LLM blocker. If so,
    dev STOPS and surfaces per §10 — deliver-agent + human decide
    whether to broaden S-Eval-4 scope to include the executor wiring
    fix (small change at `executor.py:252`) OR bundle the fix into
    S-Eval-5. Do NOT silently land the executor wiring without
    deliver-agent + human authorization.
  - **OQ-S44.3 UC-FP moderation-context calibration carry-over**: if
    the trial real-LLM dry-run surfaces a legitimate UC-FP no-context
    path that escalates or defers (instead of answering), the
    `resolve_faq_grounded_answer.consult-moderation-context-on-removal-explanation`
    step MAY need to either: (i) loosen its `trace_check` to accept
    the escalate-or-defer path, OR (ii) downgrade severity from
    mandatory to advisory. This would be a S-Eval-3 fix-iteration —
    dev STOPS and surfaces; do NOT silently edit the populated step.

  **Bundle policy**: dev ships 10-12 new bad-case YAMLs + `_manifest.md`
  ledger append + trial dry-run calibration note appended to
  `_manifest.md` + handoff in ONE bundle commit (per
  `feedback_commit_at_end_bundles_deliver_artefacts.md`). Dev does
  NOT stage deliver-agent close-out files (sprint_objective archive,
  10-handoff §1 lead refresh, codex-findings reset — codex-findings
  stays scaffold because Codex is milestone-shared, NOT per-sub-sprint
  — action_bank Sprint 45 row).

  **Cross-session continuity**: if a new dev-agent picks this up cold,
  the dev prompt at `compact/sprint-045-dev-prompt.md` is the entry
  point. Read order: AGENTS.md (auto-loaded) → this file →
  docs/milestone_objective.md (M3-Eval context, §3 S-Eval-4 row;
  §6 hard fences; §8 Codex review plan) → proposal §6 S-Eval-4
  (scope detail) → eval_interactive/case_specs/bad_cases/_manifest.md
  (Alice precedent + ledger schema) → eval_interactive/case_spec_overrides.yaml
  (the 17 approved entries source pool) → docs/sprints/sprint-044-handoff.md
  §12 closure verdict (carry-over context).
---

# Sprint 45 (NEW M3-Eval sub-sprint 4, S-Eval-4) — Bad-case suite expansion + trial milestone-close dry-run

## 1. Sub-sprint class

**Single-track semantic-touching sub-sprint, layer `eval_spec` per `iteration_governance.md` §3.2 Q6 (data, not code; CaseSpec authoring + bad-case suite lifecycle ledger maintenance + trial manual review dry-run as calibration deliverable).** §7 stanza REQUIRED. Codex review **milestone-shared at M3-Eval close** (default per §4.3; no per-sub-sprint trigger fires for S-Eval-4 by default).

Class breakdown:

- **Primary layer**: `eval_spec` (data) — authoring 10-12 new bad-case YAMLs in `eval_interactive/case_specs/bad_cases/`; appending lifecycle ledger rows in `_manifest.md`; appending calibration note in `_manifest.md` summarising the trial dry-run findings.
- **§1.7 forbidden-list adjacency**: **NO** — S-Eval-4 ships data only (new bad-case YAMLs + manifest append + dry-run calibration note); does NOT author LLM-visible content (S-Eval-3 territory) and does NOT modify any runtime / Java / Python scoring / extractor / harness source. The bad cases themselves are sourced from REAL session traces via `case_spec_overrides.yaml` `status: approved` entries; no synthetic case authoring.
- **Tier-0 invariant claim**: no new Tier-0 invariant added.
- **No new Java code, no new runtime behaviour, no new Python scoring code, no new YAML schema**: S-Eval-4 is data + calibration only.

## 2. Goal

Ship **10-12 new bad-case YAMLs** under `eval_interactive/case_specs/bad_cases/` (sourced from `eval_interactive/case_spec_overrides.yaml` `status: approved` entries; pool at HEAD is **17 entries**, NOT 29 as proposal §4 mistakenly stated) PLUS a **trial milestone-close manual review dry-run** that walks each new bad case's trace and judges PASS / FAIL / IMPROVING against the case's `closure_criterion`. The load-bearing output is **calibrated `closure_criterion` wording quality**, NOT verified bot correctness (per `docs/milestone_objective.md` §3 S-Eval-4 scope sentence 4 + `iteration_governance.md` §5.6 human-judgment-gate principle).

### 2.1 Bad-case selection criteria (per `docs/milestone_objective.md` §3 S-Eval-4 + S-Eval-1 §12.7 carry-over)

Select 10-12 entries from the 17 approved `case_spec_overrides.yaml` blocks per these priorities:

1. **UC-G / UC-H / UC-I / UC-J priority** (per S-Eval-1 §12.7 thin-corpus UC carry-over; preserved through S-Eval-2 + S-Eval-3 §12.7). Re-verify D1-D4 × UC-FP/G/H coverage within 17 at this planning round (dev runs `grep -c "status: approved"` and `grep "active_use_case" case_spec_overrides.yaml | sort | uniq -c` to confirm).
2. **D1-D4 dimension coverage** (per proposal §4 decision 3 + milestone §3 S-Eval-4): D1 mis-classification, D2 intake prefill gap, D3 lock-in escape, D4 stated_reason circularity. Prefer entries that span all 4 dimensions; if any dimension is undersampled in the 17, see §10 #3 STOP signal.
3. **Three highest-volume UC families** (per proposal §4 decision 3): UC-FP, UC-G, UC-H.
4. **Strongest semantic divergence from pre-override CaseSpec** (the override `notes:` field typically describes the divergence; pick entries where the divergence is qualitatively largest — e.g., entirely different `active_use_case` / `should_escalate` / `bot_handling_pattern` than the pre-override CaseSpec).

If 17 entries do not yield 10-12 cases meeting the priorities AND dimensions, dev STOPS per §10 #3 and surfaces. The 3 fallback paths from S-Eval-1 launch constraint #2 (still NOT LOCKED): (a) accept narrower coverage for this milestone; (b) supplement with non-override-sourced real sessions (still REAL sessions; NEVER synthetic per §10 #4); (c) defer the missing dimension to a follow-on milestone with an R-item.

### 2.2 Bad-case YAML schema (per `iteration_governance.md` §5.6 + Alice precedent)

Each new bad-case YAML in `eval_interactive/case_specs/bad_cases/<case_id>.yaml` carries the standard CaseSpec schema PLUS a `bad_case_metadata` block:

```yaml
bad_case_metadata:
  source_session_id: <the real session id from case_spec_overrides.yaml>
  surfaced_by: <human / sprint / colleague / experiment name>
  surfaced_date: <YYYY-MM-DD; for overrides, the date the override was approved>
  failure_shape: <one-line description of the multi-layer failure mode>
  layers_involved: [<list of layers per §3.2: prompt_projection / skill_state / semantic_planner / eval_spec / etc.>]
  related_dimensions: [<D1 | D2 | D3 | D4>]  # may be multiple
  related_r_items: [<list of related R-items in action_bank §3 or §5 if applicable>]
  tier: core | scope-relevant  # per §5.6.1 — core for cross-cutting / release-gate-relevant; scope-relevant for surface-specific
closure_criterion: <human-verified observable end-state(s) that count as resolved; multi-line OK; this is the GUIDANCE for the manual review judgment, NOT a programmatic PASS/FAIL match>
```

PLUS the standard CaseSpec schema fields (`persona`, `expected`, `scoring`, etc.) per `eval_interactive/eval_interactive/case_spec/schema.py`. Reuse `expected` fields ONLY where they are confirmed correct per the override `notes:`; otherwise leave optional fields as `None` (S-Eval-1 schema simplification permits this).

`closure_criterion` wording quality is the **calibration deliverable**. Examples of well-calibrated `closure_criterion` (per the Alice precedent at `eval_interactive/case_specs/bad_cases/alice_uc_a_uc_h_misclass.yaml`):
- "bot routes the topic-shift message to UC-C OR asks one focused clarifying question, instead of stamping UC-A through the drift"
- "bot retrieves a knowledge article AND cites source on factual_answer output class for the UC-F query"

Examples of POORLY calibrated `closure_criterion` (avoid):
- "bot does the right thing" (too vague)
- "case_passed = true" (programmatic — re-imports §5.5 confounding sources)
- "composite_score >= 0.8" (programmatic — same anti-pattern)

### 2.3 Trial milestone-close manual review dry-run (calibration deliverable)

After authoring the 10-12 new bad cases, dev runs:

```bash
cd eval_interactive && uv run eval-interactive run --path case_specs/bad_cases/
```

(OR the scope-relevant subset per `iteration_governance.md` §5.6.2 if the dry-run is scoped narrower for time.)

Dev then walks the per-case trace in `case_results[].per_turn_trace[]` and judges PASS / FAIL / IMPROVING against each case's `closure_criterion`. **This is a calibration exercise for the wording**, NOT a verification of bot correctness — the bot's current behaviour on these bad cases is expected to FAIL most of them (that's why they're bad cases; the milestone has not closed the underlying failures yet).

Dev appends a **calibration note** to `eval_interactive/case_specs/bad_cases/_manifest.md` summarising:
- For each new bad case: the manual review verdict (PASS / FAIL / IMPROVING) + a 1-2 sentence rationale that explicitly references how the `closure_criterion` wording either helped or hindered the human judgment.
- A summary section noting which `closure_criterion` wording patterns proved well-calibrated (clear PASS / FAIL signals) vs poorly calibrated (ambiguous; required re-reading the trace multiple times).
- Any `closure_criterion` rewording proposals (rewrite the criterion text in the YAML if the dry-run reveals a calibration issue — that's the point of the dry-run).

**Important**: if >50% of new bad cases have an ambiguous human judgment due to poor `closure_criterion` wording (per `iteration_governance.md` §10 stop condition #4 STOP signal at the milestone level), dev STOPS per §10 #5 and surfaces — deliver-agent + human pause new-bad-case authoring and rework existing closure_criteria using the dry-run findings.

### 2.4 Real-LLM run posture (OQ-S44.5 carry-over from S-Eval-3)

S-Eval-3 close §12.4 OQ-S44.5 disposition: synthetic Tier-2 evidence sufficient for S-Eval-3 close; real-LLM run carry-over needed by M3-Eval close. **S-Eval-4 trial dry-run is the natural calibration vehicle**.

Dev DECIDES at planning round + STOPS-and-surfaces if needed:
- **Option A — trial dry-run uses synthetic / mocked trace** (current Alice precedent uses real-LLM but with potentially stale trace). Lower-cost; does not exercise Tier-2 against populated `critical_steps`; does not surface OQ-S44.1 executor wiring blocker.
- **Option B — trial dry-run uses real-LLM end-to-end run** of the bad-case suite. Higher-cost; would exercise Tier-2 against populated `critical_steps` — BUT see OQ-S44.1: `executor.py:252` does NOT pass `tier2_result=` to `compute_composite(...)`, so Tier-2 results are structurally inert in the production eval-harness path. Option B WITHOUT executor wiring would produce the same composite scores as if `critical_steps` were empty — informative for Tier-1 / Tier-3 judgement but not for Tier-2 calibration.
- **Option C — trial dry-run uses real-LLM + executor wiring fix** (small change at `executor.py:252` to pass `tier2_result=`). Highest-confidence; would surface UC-FP moderation-context calibration (OQ-S44.3) + UC-H Alice intake-complete (S-Eval-3 §9 expected) under real-LLM. BUT this expands S-Eval-4 scope beyond data + calibration to include code change — STOP per §10 #1 and surface to deliver-agent + human who decide whether to authorise the scope expansion OR defer to S-Eval-5.

**Deliver-agent's read** (NOT a pre-decision; dev + human decide at planning round): Option A or Option B-without-wiring is the natural S-Eval-4 default; Option C is the cleanest path to surface real Tier-2 calibration evidence but requires explicit deliver-agent + human scope authorization.

## 3. Non-goals (explicit)

S-Eval-4 does NOT:

1. **Modify any Tier-0 / Tier-1 / Tier-2 check code** (those are S-Eval-1 / S-Eval-2 / S-Eval-3 territory). No edits to `eval_interactive/eval_interactive/scoring/hard_checks.py` / `outcome_checks.py` / `composite.py` / `skill_procedure_check.py` / `llm_judge.py` (S-Eval-5 territory).
2. **Modify `eval_interactive/eval_interactive/case_spec/schema.py` or `case_spec/loader.py`** (S-Eval-1 territory; schema simplification already shipped).
3. **Modify `eval_interactive/case_spec_overrides.yaml`** (read-only source pool for S-Eval-4 bad-case selection).
4. **Synthesise bad cases from non-real sessions.** Every new bad case MUST be sourced from a real session id (via `case_spec_overrides.yaml` `status: approved` entry OR via supplementation path (b) under §2.1 — still REAL sessions). No synthetic / hand-crafted bad cases.
5. **Modify the existing Alice bad case** `alice_uc_a_uc_h_misclass.yaml` (cascade fence; re-run as regression guard).
6. **Modify any case fixture** under smoke / anchor / anchor_outcome / case-families / shadow (cascade fence per milestone §6 #3-#7).
7. **Modify any Skill YAML or Skill Java/Python source** (S-Eval-2 + S-Eval-3 territory; cascade fence per milestone §6 #13).
8. **Modify the executor wiring** at `eval_interactive/eval_interactive/batch/executor.py` (OQ-S44.1 carry-over; if needed for the trial dry-run under Option C per §2.4, dev STOPS and surfaces; do NOT silently land the wiring fix).
9. **Modify the L3 judge / rubric** (`llm_judge.py` is S-Eval-5 territory).
10. **Touch governance docs** (`iteration_governance.md`, `doc_governance.md`, `agent_context_guide.md`, `runtime_freeze_and_risk_policy.md`), foundational docs, or sprint / milestone archives.
11. **Touch `docs/codex-findings.md`** (review-agent territory; Codex is milestone-shared at M3-Eval close; codex-findings stays scaffold during S-Eval-4).
12. **Touch `docs/milestone_objective.md`** (deliver-agent territory).
13. **Add new Tier-0 invariants.** Milestone-level §6 hard fence.
14. **Add any runtime if-else / keyword matrix / per-UC enumeration / regex anywhere in bad-case YAMLs or manifest.** §1.7 red line — bad-case YAMLs are CaseSpec data; the `bad_case_metadata` block must NOT encode hard semantic rules in `closure_criterion` (per §2.2 well-calibrated examples).
15. **Close any R-item.** The 4 M3-Eval R-items flip at S-Eval-5 per `docs/milestone_objective.md` §7; S-Eval-4 does NOT close them.

## 4. Premise check (deliver-agent verified 2026-05-22)

- ✅ `eval_interactive/case_spec_overrides.yaml` carries **17 `status: approved` entries** at HEAD `01770ac` (deliver-agent reconciled at S-Eval-1 launch 2026-05-20; proposal §4 decision 3 mistakenly stated "29 approved entries"). Source pool: 17. Selection target: 10-12.
- ✅ `eval_interactive/case_specs/bad_cases/` carries 1 case (Alice `alice_uc_a_uc_h_misclass.yaml`) + `_manifest.md` lifecycle ledger at HEAD. S-Eval-4 appends 10-12 new YAMLs + new ledger rows + calibration note section.
- ✅ S-Eval-3 (Sprint 44) closed Clean PASS 2026-05-22 (`docs/sprints/sprint-044-codex-review.md`); per-sub-sprint Codex per §4.3 trigger #2 returned `decision: pass / blocking_count: 0`. **S-Eval-4 UNBLOCKS.**
- ✅ S-Eval-3 §12.7 carry-overs LOAD-BEARING for S-Eval-4: thin-corpus UC priority (UC-G/H/I/J); Python baseline runner discipline (OQ-S44.6); OQ-S44.1 executor wiring blocker for Option C real-LLM dry-run; OQ-S44.3 UC-FP moderation-context potential fix-iteration trigger.
- ✅ `iteration_governance.md` §5.6 + §5.6.1 + §5.6.2 + §5.6.3 govern the bad-case suite lifecycle + manual-review-as-human-judgment-gate principle.

## 5. Files in scope (Sprint 45 dev ships)

**10-12 NEW bad-case YAMLs:**

- `eval_interactive/case_specs/bad_cases/<case_id>.yaml` × 10-12 (NEW each). Naming convention: `<case_id>_<uc>_<failure_shape_slug>.yaml` per Alice precedent (e.g., `alice_uc_a_uc_h_misclass.yaml`). Each YAML carries `bad_case_metadata` + `closure_criterion` + standard CaseSpec schema fields (per §2.2).
- Selection from the 17 approved `case_spec_overrides.yaml` entries per §2.1 priorities (UC-G/H/I/J first; D1-D4 dimension coverage; UC-FP/G/H family coverage; strongest semantic divergence).

**Manifest ledger append:**

- `eval_interactive/case_specs/bad_cases/_manifest.md` — append 10-12 new lifecycle ledger rows (one per new bad case) per the Alice row precedent. Each row records: case_id, source_session_id, surfaced_by, surfaced_date, failure_shape, layers_involved, tier, status (initially `active`).

**Calibration note append (in the same `_manifest.md` file):**

- Appended section "## S-Eval-4 trial milestone-close dry-run calibration notes (2026-05-22 → close date)" with: per-case manual review verdict + rationale + closure_criterion wording assessment + summary patterns + any rewording proposals. Per §2.3.

**Handoff document:**

- `docs/sprints/sprint-045-handoff.md` (dev-authored at sub-sprint close); follows 12-section template per Sprint 35 / 41 / 42 / 43 / 44 precedent.

**Optional minor edits** (only if §10 STOP-and-surface dialogue authorizes):

- `eval_interactive/eval_interactive/batch/executor.py` line 252 — pass `tier2_result=` to `compute_composite(...)` IF Option C real-LLM dry-run is authorised by deliver-agent + human per §2.4. **Default: NOT touched.**

## 6. Files NOT in scope (hard fences)

**1. Tier-0 / Tier-1 / Tier-2 / Tier-3 scoring code — NO touch:**

- `eval_interactive/eval_interactive/scoring/hard_checks.py` (Tier-0; S-Eval-1 demoted partial; NO further edit).
- `eval_interactive/eval_interactive/scoring/outcome_checks.py` (Tier-1 / Tier-3; S-Eval-1 demoted partial; NO further edit).
- `eval_interactive/eval_interactive/scoring/composite.py` (S-Eval-1 + S-Eval-2 wired; NO further edit).
- `eval_interactive/eval_interactive/scoring/skill_procedure_check.py` (S-Eval-2 shipped; NO further edit).
- `eval_interactive/eval_interactive/scoring/llm_judge.py` (S-Eval-5 territory; NO touch in S-Eval-4).

**2. CaseSpec schema + loader — NO touch (S-Eval-1 territory):**

- `eval_interactive/eval_interactive/case_spec/schema.py`.
- `eval_interactive/eval_interactive/case_spec/loader.py`.

**3. Skill abstraction (M2 + S-Eval-2 + S-Eval-3) — NO touch:**

- `server/src/main/java/com/gumtree/csagent/service/runtime/skill/` (all Java Skill files).
- `server/src/main/java/com/gumtree/csagent/service/runtime/ContextProjectionBuilder.java`.
- `server/src/main/resources/skills/` (all 6 Skill YAMLs; S-Eval-3 content UNCHANGED).
- `server/src/main/java/com/gumtree/csagent/service/runtime/IntakeFieldsRegistry.java`, `UseCaseRegistryService.java`, `ResolveDispositionEvaluator.java`.

**4. Runtime semantic surfaces — NO touch (M2 §6 #5 + M3-Eval §6 #1 inherits):**

- `RuntimeIntentClassifier.java`, `IntentClassification.java`, `DriftResult.java`, `DriftDetector.java`, `UseCaseRouter.java`, `ClassifyUseCaseTool.java`.
- `PhaseEvaluator.java`, `AgentRunLoopImpl.java`, `ControlKernel.java`.
- `system_prompt.txt`, `tool-policy.yaml`.

**5. Eval case fixtures — NO touch (cascade fence per milestone §6 #3-#7):**

- Any case under `eval_interactive/case_specs/smoke/` (14 cases).
- Any case under `eval_interactive/case_specs/anchor/` (159 cases).
- Any case under `eval_interactive/case_specs/anchor_outcome/` (12 cases).
- Any case under `eval_interactive/case_specs/case_families/<existing>/` (12 directories).
- The existing Alice bad case `eval_interactive/case_specs/bad_cases/alice_uc_a_uc_h_misclass.yaml` (cascade fence; re-run as regression guard at M3-Eval close).
- Shadow CaseSpecs under `eval_interactive/case_specs_shadow/`.
- `eval_interactive/case_spec_overrides.yaml` (read-only source pool).

**6. Eval harness / loader / simulator — NO touch (default; Option C in §2.4 is the ONLY authorised exception, and requires explicit deliver-agent + human approval):**

- `eval_interactive/eval_interactive/loader/`, `eval_interactive/eval_interactive/simulator/`.
- `eval_interactive/eval_interactive/batch/executor.py` (default NO touch; Option C exception per §2.4 + §5).

**7. Governance and archives — NO touch:**

- `docs/foundational/`, `docs/runtime_freeze_and_risk_policy.md`, `docs/customer_service_tool_spec_v0_3.md`.
- `docs/current/iteration_governance.md`, `docs/current/doc_governance.md`, `docs/current/agent_context_guide.md`, `docs/current/faq_grounding_contract.md`.
- `docs/sprints/sprint-001-*` through `docs/sprints/sprint-044-*` (including the just-archived S-Eval-3 contract + handoff + Codex review).
- `docs/milestones/M1_objective.md`, `M2_objective.md`, `M2-Skill_objective.md`, `M2_codex-review.md`.
- `docs/milestone_objective.md` (M3-Eval; deliver-agent territory).
- `docs/codex-findings.md` (review-agent territory; Codex is milestone-shared at M3-Eval close; codex-findings stays scaffold during S-Eval-4).

**8. Action bank — limited touch:**

- `docs/action_bank.md`: dev SHOULD NOT close R-items in S-Eval-4 (R-item closures happen at S-Eval-5 close per `docs/milestone_objective.md` §7). Dev MAY append a Sprint 45 close-action index row in §6 at sub-sprint close per existing convention.
- The S-Eval-1-opened R-item `R-stale-test-escalation-enum-sync-v0_2-to-v0_3-migration` is unrelated to S-Eval-4 and SHOULD NOT be addressed here.

## 7. Bundle policy

S-Eval-4 dev ships in **ONE bundle commit** containing:

- 10-12 NEW bad-case YAML files under `eval_interactive/case_specs/bad_cases/`.
- `_manifest.md` ledger append (10-12 new rows) + calibration note section append.
- Optional `executor.py` minor fix (Option C in §2.4) IF surfaced + deliver-agent + human authorized.
- Dev handoff `docs/sprints/sprint-045-handoff.md`.

Expected: 13-15 files in the bundle (10-12 NEW YAMLs + 1 manifest edit + 1 handoff + optional 1 executor edit).

Deliver-agent + human bundle (separately, post-close):

- This sub-sprint contract archive (`docs/sprints/sprint-045-objective.md`).
- Codex review at M3-Eval close (milestone-shared per §4.3 default; NOT per-sub-sprint).
- Live `docs/sprint_objective.md` replaced with S-Eval-5 contract (or M3-Eval close planning placeholder).
- `docs/10-handoff.md` §1 lead refresh.
- `docs/codex-findings.md` stays scaffold (Codex milestone-shared at M3-Eval close).
- `docs/action_bank.md` Sprint 45 close-action index row (if dev did not already append).

Per `feedback_commit_at_end_bundles_deliver_artefacts.md`: dev does NOT stage deliver-agent close-out files; human bundles at deliver-agent's commit.

## 8. Layer-classification + anti-hardcode stanza (per §7; REQUIRED)

**Target failure layer:** `eval_spec` (data + calibration; not code).

**Tier-0 invariant:** This sub-sprint adds no Tier-0 invariant. The bad-case suite expansion is a human-judgment-gate calibration deliverable per `iteration_governance.md` §5.6 (primary acceptance gate for milestone close), NOT a Tier-0 invariant.

**Semantic hardcode:** **No semantic hardcode introduced.** The 10-12 new bad-case YAMLs are CaseSpec data sourced from REAL session traces via `case_spec_overrides.yaml` `status: approved` entries (NOT synthesised; NOT hand-crafted; NOT keyword-derived). The `closure_criterion` strings are LLM-irrelevant (manual-review guidance for the human + deliver-agent at the trial dry-run); they are NOT consumed by runtime or by any programmatic gate (per `iteration_governance.md` §5.6 2026-05-17 refinement: human-judgment-gate, not programmatic). Bad-case metadata is provenance + scoping + lifecycle info only.

**Sunset plan**: N/A — bad-case suite expansion is intended permanent (the bad-case suite is the primary acceptance gate going forward per §5.6 + milestone §5).

**Generalization coverage:**

- **Target**: 10-12 new bad-case YAMLs covering D1-D4 dimensions × UC-FP/G/H families per §2.1 priorities; each YAML carries `bad_case_metadata` + `closure_criterion` per §2.2 schema; each `closure_criterion` is human-verified observable end-state(s).
- **Neighbor**: existing Alice bad case (`alice_uc_a_uc_h_misclass.yaml`) re-runs as regression guard during the trial dry-run; expected to remain `active` (per S-Eval-3 §12.7 — Alice's UC-H intake-complete failure surfaces as Tier-2 mandatory FAIL on `uc-h-intake-complete-before-handover` once executor wiring lands).
- **Negative**: any non-bad-case fixture (smoke / anchor / anchor_outcome / case-families) must NOT trigger or interact with the new bad cases. Verified via the read-only run: smoke / anchor / anchor_outcome runs continue to produce baseline results unchanged.
- **Shadow**: NO shadow bad cases authored in S-Eval-4 (the 3-5 shadow cases for M3-Eval close are held out per `eval_interactive/case_specs_shadow/_ACCESS_BOUNDARY.md` convention; S-Eval-4 dev does NOT read or author shadow CaseSpecs).

## 9. Success metrics (per `iteration_governance.md` §5)

**Hard gates (sub-sprint close PASS only if all clear):**

- [ ] **10-12 new bad-case YAMLs landed** at `eval_interactive/case_specs/bad_cases/` (named per Alice precedent; each carries `bad_case_metadata` + `closure_criterion` per §2.2 schema; sourced from REAL sessions per §10 #4).
- [ ] **`_manifest.md` lifecycle ledger updated** with 10-12 new rows (one per new bad case) + calibration note section appended summarising trial dry-run findings.
- [ ] **Trial milestone-close manual review dry-run executed** per §2.3: per-case manual review verdict (PASS / FAIL / IMPROVING) + rationale + `closure_criterion` wording assessment for each of the 10-12 new bad cases.
- [ ] **`closure_criterion` calibration deliverable**: dry-run findings clearly identify well-calibrated vs poorly calibrated `closure_criterion` wording patterns; any rewording proposals are applied to the YAMLs in the same bundle commit.
- [ ] **D1-D4 × UC-FP/G/H dimension coverage** verified at planning round AND re-verified post-selection (if undersampled, dev STOPS per §10 #3).
- [ ] **UC-G/H/I/J priority** honoured: thin-corpus UC entries are over-represented among the 10-12 selections (compared to the 17-entry source pool's UC distribution; dev records per-UC selection count vs source pool count in handoff §6).
- [ ] Java baseline preservation: pre-S-Eval-4 baseline `1163 / 1-inherited / 0 / 2` (S-Eval-3 close). S-Eval-4 adds 0 Java code; expected baseline UNCHANGED. Inherited `SystemPromptUserRequestedTiebreakerTest:53` persists unchanged.
- [ ] Python baseline preservation: pre-S-Eval-4 baseline TBD per OQ-S44.6 reconciliation (use `uv run python -m pytest` not `uv run pytest`; dev cites the reproduced baseline at planning round + at close handoff §9). Expected: NO new Python test failures introduced by S-Eval-4 (no Python code change).
- [ ] Numbers-cite discipline (per `feedback_deliver_agent_cited_numbers_must_be_reproducible.md`): every count in handoff (bad case count, dimension distribution, UC family distribution, source pool size, test counts) reproducible from `wc -l` / `grep -c` / test-run direct output. **Python baseline citations MUST use `uv run python -m pytest`** per OQ-S44.6 carry-over.

**Observation-only (recorded; does not gate close):**

- Smoke composite_score / pass-rate / judge dimensions (§5.5 demoted to observation; recorded at S-Eval-4 close for M3-Eval cumulative tracking).
- Architecture-health metric direction: `new_semantic_hardcode_count` = 0 (S-Eval-4 ships data + calibration only); `soft_signal_conversion_count` UNCHANGED at +18 from S-Eval-3 close; `planner_ownership_ratio` not decreased.
- Real-LLM run option choice (per §2.4 A / B / C): dev records which option was taken at planning round + outcome at close handoff §9.
- Per-UC selection count vs source pool count: dev records distribution for S-Eval-5 + M3-Eval close calibration input.
- Bad case `tier` distribution: dev records how many `core` vs `scope-relevant` per `iteration_governance.md` §5.6.1.

## 10. Stop conditions (dev-agent)

Dev STOPS and surfaces to deliver-agent + human (instead of pressing on) if any of:

1. **Option C executor wiring fix authorisation requested** per §2.4 (scope expansion to include `executor.py:252` change). Surface; deliver-agent + human decide whether to authorise in S-Eval-4 OR defer to S-Eval-5.
2. **A UC-FP moderation-context calibration trigger surfaces** per OQ-S44.3 carry-over (a legitimate UC-FP no-context path produces escalate-or-defer behaviour during trial dry-run that contradicts the S-Eval-3 mandatory step `consult-moderation-context-on-removal-explanation`). Surface; deliver-agent + human decide whether to trigger S-Eval-3 fix-iteration (loosen `trace_check` OR downgrade severity to advisory) OR accept the contradiction as expected (UC-FP no-context path is a degenerate edge case).
3. **D1-D4 × UC-FP/G/H dimension coverage cannot be achieved** within the 17 approved entries. Surface; deliver-agent + human decide among the 3 fallback paths from S-Eval-1 launch constraint #2: (a) accept narrower coverage; (b) supplement from non-override real sessions; (c) defer to follow-on milestone with new R-item.
4. **Any selection candidate is NOT sourced from a real session.** Bad cases MUST be real-session-derived. Synthesised / hand-crafted / keyword-derived bad cases are §1.7 red-line violations on the bad-case suite. Halt and surface; do NOT smuggle synthetic cases.
5. **Trial dry-run reveals >50% poorly-calibrated `closure_criterion` wording** (per `iteration_governance.md` §10 stop condition #4 at the milestone level). Halt + surface; deliver-agent + human pause new-bad-case authoring and rework existing `closure_criterion` strings using the dry-run findings.
6. **`_manifest.md` schema drift** discovered during ledger append (e.g., a column the Alice precedent uses that dev cannot fill from the override `notes:`). Halt + surface; deliver-agent + human decide whether to omit the column OR derive it from the underlying session trace.
7. **Java or Python baseline regresses** (any new test failure introduced by S-Eval-4 beyond the inherited `SystemPromptUserRequestedTiebreakerTest:53` and the existing pre-existing failures per OQ-S44.6). Halt and surface; do NOT commit.
8. **Any file under "Files NOT in scope" §6** needs touching to complete S-Eval-4 (Option C executor wiring fix per §10 #1 is the ONLY authorised exception, and only with explicit deliver-agent + human approval). Hard fence violation; halt and surface; do NOT smuggle scope.

## 11. Handoff document contract (12 sections per Sprint 35 / 41 / 42 / 43 / 44 shape)

`docs/sprints/sprint-045-handoff.md` must include:

§1 Sprint identity (Sprint 45 / S-Eval-4 / M3-Eval sub-sprint 4).
§2 Summary (one paragraph).
§3 Files shipped + per-file line-count numstat (reproducible via `git show --numstat <commit>`).
§4 Per-bad-case content map (which 10-12 cases landed; source_session_id mapping; D1-D4 dimension + UC family distribution; tier distribution).
§5 §5.6 schema compliance walk per bad case (table format; per-case verification of `bad_case_metadata` completeness + `closure_criterion` shape vs §2.2 examples; flag any case where `closure_criterion` had to be rewritten during dry-run).
§6 §4.1 anti-hardcode self-walk verdicts (Q1-Q9 with one-line justification each).
§7 Open questions (OQ-S45.N format; non-blocking decisions for deliver-agent + human at close).
§8 Generalization coverage filled (target / neighbor / negative / shadow counts per §8 stanza).
§9 Validation runs:
- Java test suite (`mvn test`): baseline vs pre-S-Eval-4 (1163).
- **Python test suite (`uv run python -m pytest`)** per OQ-S44.6 carry-over: baseline vs pre-S-Eval-4 (cite the reproduced baseline; do NOT use `uv run pytest`).
- Trial milestone-close manual review dry-run: per-case verdict table (PASS / FAIL / IMPROVING) + rationale + `closure_criterion` wording assessment + summary patterns + any rewording proposals.
- Real-LLM run option choice (A / B / C per §2.4) + outcome.
- Per-UC selection count vs 17-entry source pool distribution.
- Tier distribution (`core` vs `scope-relevant`).
§10 Contract drift (any deviation from this contract; classify per §7-a / §7-b / §7-c / §7-d convention).
§11 Bundle policy honored (dev shipped one commit; deliver-agent close-out files NOT staged).
§12 Closure verdict (LEFT EMPTY by dev per `feedback_handoff_verdict_section_delegation.md`).

## 12. M3-Eval milestone context (cross-reference, not Sprint 45 contract)

- `docs/milestone_objective.md` is the live M3-Eval milestone objective. Dev SHOULD load it on cold start for layer + scope context. ESPECIALLY §3 S-Eval-4 row (this sub-sprint's scope detail) + §5 milestone acceptance bar (curated bad-case suite manual review is PRIMARY GATE per §5.6) + §6 hard fences (15 milestone-level items; S-Eval-4 §6 inherits) + §8 Codex review plan (S-Eval-4 deferred to milestone-shared close per §4.3 default).
- `docs/solutions/m3_eval_milestone_proposal.md` is the research-agent proposal source. **LOAD-BEARING sections for S-Eval-4**: §6 S-Eval-4 (scope detail) + §4 decision 3 (bad-case selection criteria) + §5.6 (curated bad-case suite philosophy).
- `docs/sprints/sprint-044-handoff.md` is the S-Eval-3 dev archive. LOAD-BEARING sections: §12.7 closure verdict carry-overs (executor wiring OQ-S44.1; UC-FP moderation calibration OQ-S44.3; real-LLM run posture OQ-S44.5; Python baseline runner discipline OQ-S44.6).
- `docs/sprints/sprint-042-handoff.md` §12.7 — the per-UC anchor distribution observation (origin of the thin-corpus UC priority for S-Eval-4 bad-case selection).
- `eval_interactive/case_specs/bad_cases/_manifest.md` — the Alice precedent + lifecycle ledger schema.
- `eval_interactive/case_specs/bad_cases/alice_uc_a_uc_h_misclass.yaml` — the Alice precedent for bad-case YAML schema.
- `eval_interactive/case_spec_overrides.yaml` — the 17 approved entries source pool (read-only).
- `iteration_governance.md` §5.6 / §5.6.1 / §5.6.2 / §5.6.3 — bad-case suite governance + tiering + per-milestone selection + lifecycle downgrade.
- S-Eval-5 (next sub-sprint after S-Eval-4 close) handles L3 judge repositioning + R-item closure (the 4 M3-Eval R-items). M3-Eval close (after S-Eval-5) does milestone-shared Codex review per §4.3 default + bad-case suite manual review as primary gate per §5.6.
