---
title: Sprint 29 objective — (R2) probe follow-on for R-prompt-phase-plan-directive-followship (CaseSpec authoring + phase-derivation verification)
doc_tier: sprint-archive
status: archived
implementation_status: historical
source_of_truth: docs/sprints/sprint-029-handoff.md (delivered behaviour)
last_reviewed: 2026-05-16
review_cadence: ad hoc
supersedes: [docs/sprints/sprint-028-objective.md]
superseded_by: docs/sprint_objective.md
notes: >
  Sprint 29 is the (R2) targeted probe sprint follow-on that Sprint 27
  recommended and Sprint 28 unblocked. Two tracks. Track A authors N≥3
  hand-authored CaseSpecs that exercise three target directives'
  preconditions (D485.2 CLOSE record_outcome-if-not-already-recorded,
  D564.7 INTAKE intake-complete handover, D616.2 RESOLVE FAQ
  only-escalate-after-valid-resolve-attempt). Track B verifies whether
  per-turn (c) phase-derivation (`phase_before[N] = phase_plan.phase[N-1]`,
  `phase_after[N] = phase_plan.phase[N]`) works on Track A's new
  authored-corpus output. Sprint 29 is semantic-touching
  (`eval_spec`-layer authoring) — §7 stanza required, multi-layer
  prospective per-track. The (R2) probe surfaces evidence; remediation of
  any directive-followship finding is a FUTURE sprint, not this one.
---

# Sprint 29 — (R2) probe follow-on: CaseSpec authoring for D485.2 / D564.7 / D616.2 + (c) phase-derivation verification

## 1. Sprint class

**Investigation+bundle, two-track, semantic-touching.** Track A is an
`eval_spec`-layer CaseSpec authoring track (per §3.2 Q6 + Sprint 20 G2
precedent — authoring cases that exercise behaviour the runtime can
and should perform, NOT widening a rubric to accept a known bot
mistake). Track B is an `infra`-layer (verification-only on Track A's
new corpus output) post-hoc analysis with an explicit in-flight
downgrade clause. **§7 stanza required.** Per-track layers enumerated
in §11.

## 2. Goal

Run the (R2) follow-on probe per `docs/sprints/sprint-027-handoff.md`
§6 and §11. Consume the per-turn `phase_plan` + `projection` surface
that Sprint 28 added to `case_results[].per_turn_trace[]` (Sprint 28
commit `ca55d8e`). Two parallel tracks:

- **Track A — Author (R2) CaseSpecs.** Hand-author N≥3 CaseSpecs total
  across three target directives (D485.2 / D564.7 / D616.2), then run
  them through smoke. The dev SHALL record, per case, whether the
  precondition fires AND whether the directive is followed-through.
  Per-directive follow-through observation is the deliverable.
- **Track B — Verify (c) phase-derivation on the new corpus.** After
  Track A's authored cases run, extract per-turn `phase_plan.phase`
  deltas on each case's `per_turn_trace[]` and verify whether the
  derivation rule (`phase_before[N] = phase_plan.phase[N-1]`,
  `phase_after[N] = phase_plan.phase[N]`) works for the consumer's
  needs — most importantly, for detecting CLOSE-entry on a case that
  exercises D485.2. If derivation works on the new corpus, no
  follow-on R-item needed (the field is consumable from existing
  data). If derivation does NOT work, surface the failure and
  propose `R-per-turn-phase-transition-dump-for-smoke-harness` for a
  future sprint per the Sprint 28 §7 named-but-not-opened follow-on.

## 3. Non-goals (explicit)

- Sprint 29 does NOT open `R-per-turn-phase-transition-dump-for-smoke-harness`.
  That R-item is proposed only if Track B verification fails.
- Sprint 29 does NOT act on D485.2 / D564.7 / D616.2 follow-through
  findings. The (R2) probe surfaces evidence; remediation is a future
  sprint.
- Sprint 29 does NOT widen any rubric, override, or eval-spec to mask
  a known bot mistake.
- Sprint 29 does NOT touch prompt, runtime semantic decision code,
  judge calibration, Tier-0, or any Sprint 20-landed case family.
- Sprint 29 does NOT speculatively bundle a phase-transition extension
  to `per_turn_trace[]`; per `feedback_corpus_undecidable_premise_check.md`,
  the (c) verification must run on Track A's authored corpus before
  any extension is justified.
- Sprint 29 does NOT touch `R-prompt-phase-plan-directive-followship`
  closure. The R-item stays open; Sprint 29's findings update its
  disposition.
- Sprint 29 does NOT use a mocked LLM to measure directive
  follow-through. Real LLM execution is required for primary evidence
  (per `feedback_mocked_llm_cannot_prove_prompt_causal_change.md`).

## 4. Premise carried forward (verified by deliver-agent 2026-05-15)

Premise items verified at session start (last commit `df8b8cd`,
Sprint 28 close). All 8 items below were checked against current
code / data; do NOT re-verify in dev session unless §10 stop condition
fires.

1. **Sprint 20 case-family directory structure** at
   `eval_interactive/case_specs/case_families/` exists with 10
   families. Each family has `_manifest.yaml` membership in the
   single shared `eval_interactive/case_specs/case_families/_manifest.yaml`
   (NOT a per-family file). Verified: `_manifest.yaml` is 12.3 KB at
   the case_families root; each entry has `family_id`,
   `target_case_path`, `target_case_ids`, `neighbor_case_ids`,
   `negative_case_ids`, `shadow_case_ids`, plus a `notes` paragraph.
2. **Sprint 20 hidden_facts + expected_behaviors CaseSpec pattern.**
   Read `eval_interactive/case_specs/case_families/cs011_uc_d_description_ignored/neighbor/cs11n01_uc_d_2fa_loop_verbose.yaml`:
   YAML with `case_id`, `source_session_id`,
   `source_dataset: case_family_authored`, `form_context`, `persona`
   (with `hidden_facts` list-of-dicts; each has `fact` + `disclose_when`
   fields), `expected` (with `outcome_class`, `primary_uc`,
   `should_escalate`, `escalation_trigger`, `expected_tool_sequence`,
   `forbidden_tools`, `grounding_mode`, `answer_must_not_contain`,
   `max_turns`), `scoring` (with `hard_checks`, `outcome_checks`,
   `llm_judge_dimensions`).
3. **`--path` flag** in `eval_interactive/eval_interactive/cli.py:142`
   (`--path` -> `custom_path` option). `--set` at line 139 lists
   `_KNOWN_SETS` (anchor / promotion / exploration / smoke). Sprint 29
   runs new case families via `--path <family_dir>` like Sprint 25
   did; does NOT add new entries to `_KNOWN_SETS`.
4. **`_KNOWN_SETS` registry** at
   `eval_interactive/eval_interactive/batch/sets.py:17`. Verified: 4
   entries (`anchor`, `promotion`, `exploration`, `smoke`). Sprint 29
   does NOT add a 5th.
5. **PhaseEvaluator branches** at
   `server/src/main/java/com/gumtree/csagent/service/runtime/PhaseEvaluator.java`
   lines 411 (DISCOVER) / 458 (CONFIRM) / 485 (CLOSE) / 517 (ESCALATE)
   / 564 (RESOLVE INTAKE) / 616 (RESOLVE FAQ). Verified at the cited
   lines.
   **CONFIRM → CLOSE transition condition** at lines 952–957 of
   `PhaseEvaluator.java`: when the bot emits `FINAL_ANSWER` from
   the CONFIRM phase, the next phase becomes `CLOSE` (the bot is
   satisfied with the user's prior answer). To reach CLOSE in Track
   A's authored cases, the case must first reach CONFIRM (i.e. the
   bot must produce a RESOLVE-grade answer the user can react to),
   then the bot must emit `FINAL_ANSWER` from CONFIRM. Track A's
   D485.2 cases SHALL author preconditions that drive this path.
6. **`record_outcome` tool schema** at
   `server/src/main/java/com/gumtree/csagent/service/tools/RecordOutcomeTool.java:28–94`.
   Verified: takes `outcome_class` arg (or legacy alias `outcome`),
   accepts `{resolve, escalate, abandon}` (also legacy
   `{RESOLVED, ESCALATED, ABANDONED}`); also takes
   `escalation_reason` when `outcome_class == escalate`.
   `record_outcome` is allowed-tools on CLOSE branch
   (`PhaseEvaluator.java:480` — list `[record_outcome]`).
7. **`request_handover` `intake_complete_for_uc_*` enum values** at
   `PhaseEvaluator.java:39–63`. Verified: 23-value canonical
   escalation_reason set includes `intake_complete_for_uc_g`,
   `intake_complete_for_uc_h`, `intake_complete_for_uc_i`,
   `intake_complete_for_uc_j`, `intake_complete_for_uc_k`. The INTAKE
   branch builds `escalationPolicy` with this trigger via
   `intakeCompleteTrigger(activeUc)` at line 575.
8. **FAQ knowledge index** at `docs/FAQ-knowledge_include_help_url.csv`.
   Verified: file exists. The "viable hit" semantics for D616.2 are
   defined by `search_knowledge → resolve_article` returning a
   `resolve_grade=true` article. Track A's D616.2 cases SHALL author
   `form_context.description` text that surfaces a known
   viable-hit article ID for the chosen UC (the dev SHALL pick from
   the existing FAQ csv; no synthesised articles per
   `iteration_governance.md` §1.7 and Sprint 18 §8.2).

**Empirical premise check on Sprint 28 smoke artefact** (per
`feedback_corpus_undecidable_premise_check.md`):
`eval_interactive/results/20260514-181257/results.json` contains 14
cases × `per_turn_trace[]`. Extraction
`jq '[.case_results[].per_turn_trace[] | {phase: (.phase_plan.phase // "NULL")}] | group_by(.phase) | map({phase: .[0].phase, count: length})' eval_interactive/results/20260514-181257/results.json`
returns:

- `DISCOVER`: 1 turn (cs_029 turn 0)
- `NULL`: 1 turn (cs_029 turn 1; third defensive shape — populated
  trace, NULL `phase_plan`)
- `RESOLVE`: 21 turns (across the remaining populated cases)

**Across all 23 populated turns the smoke exercises ZERO phase
transitions**: 11 of 12 populated cases stay in `RESOLVE`; cs_029
moves DISCOVER → NULL but that is the third-defensive-shape, not a
real `DISCOVER → RESOLVE` transition. The corpus is therefore
**undecidable** for (c) phase-derivation on the consumer's
load-bearing case (D485.2 CLOSE-entry detection). This is the
reason Track B's (c) verification is bundled onto Track A's new
authored corpus, NOT run standalone on the existing smoke.

## 5. Sprint 27 (R2) probe scope (carried forward)

Per `docs/sprints/sprint-027-handoff.md` §6 + §11:

- Author N≥3 CaseSpecs total across three target directives. Coverage
  suggestion (from Sprint 27 §6):
  - **D485.2** CLOSE record_outcome-if-not-already-recorded: 1–2
    cases reaching CLOSE phase, with no prior `record_outcome`.
  - **D564.7** INTAKE intake-complete handover: 1–2 cases where user
    supplies all required intake fields in 2–3 turns, on an INTAKE
    UC (UC-G/H/I/J/K). The bot SHOULD call `request_handover` with
    `intake_complete_for_uc_*`.
  - **D616.2** RESOLVE FAQ premature-escalation prohibition: 1–2
    cases where `search_knowledge` returns viable hit AND user
    expresses mild frustration that would tempt premature escalation.
- **D517.2 (ESCALATE ensure-handover-called)** is OUT of scope per
  Sprint 27 §6 ("tautology recap" — structurally degenerate; can
  only be reached after `request_handover` has been called).

Sprint 29 SHALL meet the N≥3 floor; 6 cases (2 per directive) is the
suggested upper bound. The dev MAY add `neighbor` / `negative` cases
per the Sprint 20 G2 pattern if the precondition shape benefits from
them; shadow cases are NOT required (Sprint 29 is a probe; the
generalization-coverage bar is "exercises each target directive's
precondition with confirmed observation").

## 6. Track A — CaseSpec authoring scope

### 6.1 Implement only

- A new case-family-style directory layout under
  `eval_interactive/case_specs/case_families/sprint29_directive_probe/`
  OR a similar Sprint-29-specific location. The dev may pick:
  (i) one combined directory with subdirs `d485_2/`, `d564_7/`,
  `d616_2/`; OR (ii) three separate top-level families. The choice
  is the dev's; document the rationale in handoff §3.
- N≥3 hand-authored CaseSpec YAML files, one per directive at
  minimum. Provenance fields:
  - `source_dataset: case_family_authored`
  - `source_session_id: synthetic-sprint29-<directive>-<idx>`
  (mirroring the Sprint 20 G2 naming pattern).
- A manifest entry (or 3) appended to the existing
  `eval_interactive/case_specs/case_families/_manifest.yaml` (if
  Sprint 29 uses the case_families layout) OR a new
  `_manifest.yaml` inside the Sprint-29-specific directory (if the
  dev picks a separate location). Document choice in handoff §4.
- Smoke run for each authored case via
  `cd eval_interactive && uv run eval-interactive run --path <case_spec_path_or_dir>`.
  Result file under `eval_interactive/results/<run-id>/results.json`.
- Per-case bot-turn count, per-case phase transition observed,
  per-directive follow-through observation recorded in handoff §6.

### 6.2 Track A authoring discipline (§1.7 hard gate)

Per `iteration_governance.md` §1.7 + Sprint 18 §5.2 Method note +
Sprint 20 G2 precedent: hand-authored CaseSpecs MUST use
**principled** `hidden_facts` and `expected_behaviors`. The directive
shape is **structural**, not content; the dev authors cases that
EXERCISE the directive's precondition; the dev does NOT bake the
directive text into the case spec.

Specific guardrails:

- **No trace-specific text in `hidden_facts`** (e.g. do NOT copy a
  bot transcript line as a hidden_fact "the bot said X"). Hidden
  facts describe the user's situation and what would make a good CS
  agent succeed.
- **No keyword/regex preconditions in `expected.*` fields.** Use the
  structured fields (`primary_uc`, `should_escalate`,
  `escalation_trigger`, `expected_tool_sequence`, `forbidden_tools`,
  `grounding_mode`, `answer_must_not_contain`).
- **No CaseSpec id mirrored into runtime/prompt/judge code.** No
  edit to any code, prompt, or judge config.
- **No rubric widening to accept a known bot mistake.** If the bot
  fails the authored case, the failure is the **finding**; do not
  edit the case to make the failure go away. (This is the entire
  point of the probe.)
- **No L3 override authored** for the new cases. The Sprint 20 G2
  pattern is principled CaseSpecs WITHOUT L3 overrides; overrides
  are post-hoc human reviews, not authoring-time corrections.
- **No editing of existing case families** (`cs001_*`, `cs011_*`,
  `cs015_*`, `cs038_*`, `cs040_*`, `cs095_*`, `cs176_*`, `cs192_*`,
  `cs259_*`, `manual_probe_*`). Sprint 20 cascade fence still in
  force.

### 6.3 Per-directive design intent (informational; not prescriptive)

The dev SHALL design the cases from first principles. The bullets
below are informational sketches drawn from Sprint 27 §4 directive
text; the dev SHALL apply the §6.2 discipline.

- **D485.2 CLOSE record_outcome-if-not-already-recorded.** Precondition:
  the case reaches CLOSE phase. Per §4 premise item 5, CLOSE is
  entered via CONFIRM `FINAL_ANSWER` (i.e. a satisfied-user shape on
  the bot's prior RESOLVE-grade answer). The case SHALL exercise a
  resolve-path UC (NOT an intake UC) so the bot can reach CONFIRM.
  Whether the bot then calls `record_outcome` at CLOSE is the
  observation.
- **D564.7 INTAKE intake-complete handover.** Precondition: case is on
  an INTAKE UC (one of UC-G/H/I/J/K). User supplies all required
  intake fields (per phase 2 UC-specific YAML; check
  `docs/foundational/phase2_domain_realization_spec.md` for the
  required fields per UC) in 2–3 turns. Whether the bot then calls
  `request_handover` with `escalation_reason=intake_complete_for_uc_<x>`
  is the observation.
- **D616.2 RESOLVE FAQ premature-escalation prohibition.** Precondition:
  case is on a FAQ-path UC where a known viable-hit article exists
  in `docs/FAQ-knowledge_include_help_url.csv`. User expresses mild
  frustration that would tempt premature `request_handover` with
  `faq_miss_threshold_exceeded`. Whether the bot escalates
  prematurely (before completing a valid resolve attempt) is the
  observation.

### 6.4 Real-LLM execution required (no mocked-LLM)

Per `feedback_mocked_llm_cannot_prove_prompt_causal_change.md`: the
directive-followship measurement requires **real LLM execution** for
primary evidence. Sprint 29's primary evidence is the smoke run of
the authored cases against the harness's default real-LLM
configuration. Mocked-LLM unit tests (if any) are **supporting
coverage only**, NOT primary evidence; the supporting-coverage label
SHALL be explicit in any such test file and in the handoff.

## 7. Track B — (c) phase-derivation verification scope

### 7.1 Implement only

After Track A's authored corpus runs through smoke and produces a
`results.json`, extract per-turn `phase_plan.phase` deltas on each
case's `per_turn_trace[]` and verify the derivation rule:

- `phase_before[N] = phase_plan.phase[N-1]` (for `N ≥ 1`)
- `phase_after[N] = phase_plan.phase[N]`
- For `N == 0` (first bot turn), `phase_before` is conventionally
  the session's start phase (typically `DISCOVER`); the derivation
  rule for `N == 0` is documented as a special case.

The verification SHALL answer four questions, recorded in handoff §7:

1. Is `phase_plan` populated on every bot turn of every Track A
   authored case? (If any turn lands the **third defensive shape**
   from §4 — populated trace, NULL `phase_plan` — the derivation
   must defensively skip that turn, not throw.)
2. Does the consumer's load-bearing case (D485.2 CLOSE entry: a
   case that exercises a `CONFIRM → CLOSE` transition) actually
   appear in Track A's authored corpus?
3. If yes to (2), does `phase_plan.phase[N-1] == "CONFIRM"` and
   `phase_plan.phase[N] == "CLOSE"` correctly identify the CLOSE
   entry turn?
4. Are there any **other** phase transitions in Track A's authored
   corpus (e.g. `DISCOVER → RESOLVE`, `RESOLVE → CONFIRM`,
   `RESOLVE → ESCALATE`) where derivation could fail in a way that
   matters for a future consumer?

### 7.2 In-flight downgrade clause (per Sprint 23 lesson)

If Track B verification fails — i.e. the (c) derivation does NOT
work on the new authored corpus (e.g. the consumer's load-bearing
case never appears; OR the third defensive shape lands on a
load-bearing turn; OR a different unforeseen mode fires) — the sprint
**downgrades in-flight**:

- Track B SHALL surface the failure mode explicitly in handoff §7
  (which question failed, why, with reproducible jq extraction).
- Track B SHALL propose `R-per-turn-phase-transition-dump-for-smoke-harness`
  (Sprint 28 §7 named-but-not-opened follow-on) for a future sprint
  to formally extend `per_turn_trace[i]` with `phase_before`,
  `phase_after`, and `turn_index`.
- The dev SHALL NOT retry the verification mid-sprint by re-running
  smoke with modified scope. The honest finding is "the existing
  corpus is undecidable; ship the prerequisite R-item and try
  again."
- Track A's CaseSpecs still land; the corpus is valuable even if
  the derivation analysis defers. Handoff §7 documents the downgrade.

### 7.3 No code change in Track B

Track B is **verification only**. The dev SHALL NOT add a new
`phase_before` / `phase_after` field to `per_turn_trace[i]`; that is
the proposed follow-on R-item, deferred to a future sprint.

### 7.4 cs_029 third defensive shape handling

Per §4: cs_029 turn 1 has a populated trace with NULL `phase_plan`.
This is a **third defensive shape** Sprint 28 §5 did not explicitly
document (Sprint 28 documented two empty-trace shapes:
`cs_interactive_001` 500-error and `cs_interactive_259`
CONTRACT_VIOLATION). Track B's derivation logic MUST defensively
skip a turn whose `phase_plan` is NULL — return a "phase unknown"
verdict for that turn rather than throwing.

If Track A's authored corpus lands on the cs_029 shape on a
load-bearing turn (i.e. the CLOSE-entry turn has NULL `phase_plan`),
Track B SHALL surface this as a failure mode under §7.2 and
downgrade. The cs_029 shape is observably out-of-scope of Sprint
28's two documented defensive paths; investigation of its root
cause is a future R-item, not Sprint 29.

## 8. Files in scope

### 8.1 Track A

| path | change type |
|------|-------------|
| `eval_interactive/case_specs/case_families/sprint29_directive_probe/` (or similar) | NEW directory + N≥3 new CaseSpec YAML files |
| `eval_interactive/case_specs/case_families/_manifest.yaml` | EDIT (append Sprint 29 family entries) OR NEW manifest in the Sprint-29 directory (dev's choice; document) |
| `eval_interactive/results/<run-id>/` | NEW (smoke run output) |

### 8.2 Track B

Verification only; no file changes. Track B is an extraction +
analysis recorded in the handoff §7.

### 8.3 Sprint-close artefacts (deliver-agent owned)

| path | change type |
|------|-------------|
| `docs/sprint_objective.md` | EDIT (this file; archive at close) |
| `docs/sprints/sprint-029-handoff.md` | NEW |
| `docs/action_bank.md` | EDIT (update the line-450 R-item with Sprint 29 findings; do NOT close it) |
| `docs/10-handoff.md` | EDIT (refresh lead pointer) |
| `compact/sprint-029-dev-prompt.md` + `compact/sprint-029-review-prompt.md` | deliver-agent-owned |

## 9. Files NOT in scope (hard fences)

1. **No edits to Sprint 20 case families** under
   `eval_interactive/case_specs/case_families/cs{001,011,015,038,040,095,176,192,259}_*`
   or `manual_probe_*`. Sprint 20 cascade fence still in force.
2. **No edits to Sprint 23 / 24 / 25 / 26 / 27 / 28-landed code**
   under `server/` or `eval_interactive/eval_interactive/`.
3. **No prompt edits** at
   `server/src/main/resources/prompts/system_prompt.txt`. (The
   pre-existing working-tree mod on that file is the human's; the
   dev does NOT stage it.)
4. **No eval-spec rubric widening** to accept a known bot mistake
   (per §1.7 + Sprint 18 §5.2 Method note).
5. **No keyword/regex/if-else in CaseSpec `hidden_facts` or
   `expected_behaviors`** (per §6.2).
6. **No new R-items opened unilaterally.** The proposed follow-on
   `R-per-turn-phase-transition-dump-for-smoke-harness` is named
   only if Track B fails — opening is a future planning decision.
7. **No foundational doc edits** under `docs/foundational/`.
8. **No governance doc edits** under `docs/current/`.
9. **No sprint archive edits** under `docs/sprints/sprint-001-*`
   through `sprint-028-*.md`.
10. **No Tier-0 changes.** Sprint 29 adds no Tier-0 invariant.
11. **No deadline / model / retry / budget config edits.**
12. **No mocked-LLM as primary evidence** for directive
    follow-through (per §6.4).
13. **No cs_040 routing surface touched.** The Sprint 22
    UC-K→UC-C routing fence still stands.
14. **Every quantitative claim reproducible** (per
    `feedback_deliver_agent_cited_numbers_must_be_reproducible.md`).
    Cite source path + extraction command for every number in the
    handoff.
15. **No retry on Track B verification failure** — surface +
    downgrade in-flight per §7.2.

## 10. Bundle policy

- **Track A authoring + smoke run** are independent of Track B's
  verification outcome — Track A lands regardless.
- **Track B verification** is conditional: if it succeeds (the
  consumer's load-bearing CLOSE-entry case is observable in Track A's
  authored corpus AND derivation works), no follow-on R-item is
  proposed and Sprint 29 closes cleanly. If it fails, Sprint 29
  downgrades in-flight (per §7.2) — Track A still lands, but Track B
  records the failure mode + the proposed R-item for a future sprint.
- **No mid-sprint retry on Track B.** If verification fails on the
  first analysis pass, the honest finding is the downgrade.
- **No speculative bundling of `phase_before` / `phase_after`
  extension.** Even if Track B verification fails, Sprint 29 does
  NOT add the new fields to `per_turn_trace[i]`; that is the
  proposed follow-on R-item, deferred.

## 11. Layer-classification + anti-hardcode stanza (§7; multi-layer prospective per-track)

**Target failure layer:** multi-layer prospective per-track. The dev
walks §3.2 per track.

- **Track A primary layer: `eval_spec`.** Per §3.2 Q6 + Sprint 20 G2
  precedent: hand-authored CaseSpecs that exercise behaviour the
  runtime can and should perform. NOT widening any rubric to accept
  a bot mistake; NOT widening a frozen enum; NOT mis-rubricing.
  Authoring principled cases that probe the runtime's directive
  follow-through.
- **Track B primary layer (verify-only): `infra`.** Per §3.2 Q1:
  post-hoc extraction + analysis on existing eval-harness output. No
  new code in `infra`; no edits to `_build_per_turn_trace`. The
  verification consumes `case_results[].per_turn_trace[]` (Sprint 28
  shipped) without modification.
- **Track B fallback layer (if mid-sprint failure surfaces):
  `human_review_required`.** Per §3.2 default tail: the in-flight
  downgrade per §7.2 fires when (c) derivation does not work on the
  authored corpus AND no obvious §3.2 layer matches the failure mode.
  The honest exit is propose `R-per-turn-phase-transition-dump-for-
  smoke-harness` for a future sprint and surface the failure for the
  human.

**Tier-0 invariant:** This sprint adds no Tier-0 invariant. Neither
Track A nor Track B is a Tier-0 surface.

**Semantic hardcode:** No semantic hardcode introduced. Track A is
CaseSpec authoring (no code, no prompt, no judge config). Track B is
post-hoc analysis on existing data (no code). Specifically:

- No keyword / regex / if-else / enum / per-UC matrix added to
  runtime, prompt, judge, or CaseSpec rubric.
- No CaseSpec text quotes any bot trace verbatim.
- No L3 override authored for the new cases.
- No prompt teaching paragraph added (per §6.2 + §9.3).
- No mocked-LLM serves as primary evidence (per §6.4 + §9.12).

**Generalization coverage:**

- **Track A:**
  - **target:** N≥3 hand-authored CaseSpecs across D485.2 / D564.7 /
    D616.2; each case exercises its directive's precondition.
  - **neighbor:** OPTIONAL — dev may add neighbor cases per Sprint 20
    G2 precedent if the precondition shape benefits. Not required;
    Sprint 29 is a probe.
  - **negative:** OPTIONAL — same rationale as neighbor.
  - **shadow:** **NOT required.** Sprint 29 is a (R2) probe; shadow
    split is a Sprint 20 G2 generalisation surface that Sprint 29
    does not consume. If a future structural sprint ships from the
    Sprint 29 finding, shadow cases authored at that point are the
    correct surface.
- **Track B:**
  - **target:** the new authored Track A corpus.
  - **neighbor / negative / shadow:** N/A — Track B is verification,
    not authoring.

## 12. Success metrics

- **Track A — pass criteria:**
  - At least 3 hand-authored CaseSpecs land at the chosen path,
    covering all 3 target directives (D485.2 / D564.7 / D616.2).
  - Each case runs through smoke via
    `cd eval_interactive && uv run eval-interactive run --path <…>`
    against a real-LLM configuration.
  - The handoff §6 records, per case: precondition fires (yes/no);
    directive followed (yes/no); reproducible jq / python extraction
    for the observation.
  - No edits to Sprint 20-landed case families. No prompt edit. No
    code edit. No rubric widening.
- **Track B — pass criteria:**
  - All 4 verification questions from §7.1 answered in handoff §7
    with reproducible extraction.
  - If derivation works: handoff §7 documents "no follow-on R-item
    proposed; (c) is consumable from Sprint 28's `phase_plan`
    field."
  - If derivation does NOT work: handoff §7 documents the failure
    mode + proposes `R-per-turn-phase-transition-dump-for-smoke-
    harness` for a future sprint. **No retry on the verification.**
- **Cross-track — sprint-close criteria:**
  - Reproducibility: every number cited in handoff is reproducible
    via a named jq / python invocation on a named file path.
  - `R-prompt-phase-plan-directive-followship` updated at
    `docs/action_bank.md:450` with the Sprint 29 findings. Disposition
    refined; R-item **NOT closed.**
  - Per `iteration_governance.md` §5.1: safety floor unchanged;
    grounding floor unchanged; wrong-containment unchanged. Sprint 29
    is a probe — the metrics floor is "no regression visible from
    the 14-case smoke set, which Sprint 29 does NOT touch." Sprint
    29's new authored cases are a separate corpus; they record
    findings but do not gate the existing smoke set.
- **Codex review** is REQUIRED at sprint close (Sprint 29 is
  semantic-touching: `eval_spec`-layer authoring). The §4.1 kernel
  applies. Mocked-LLM as primary evidence is BLOCKING per §11
  (Track A's evidence must be real-LLM).

## 13. Memory + governance pointers (deliver-agent reference)

- `feedback_corpus_undecidable_premise_check.md` — Sprint 29 IS the
  (b) execution of this pattern.
- `feedback_premise_verification_before_next_sprint.md` + sister
  premise-verification memories — premise check executed in §4.
- `feedback_mocked_llm_cannot_prove_prompt_causal_change.md` — Track
  A's directive-followship measurement requires real LLM (§6.4).
- `feedback_deliver_agent_cited_numbers_must_be_reproducible.md` —
  every quantitative claim in handoff cites source path +
  extraction command.
- `feedback_multi_layer_prospective_stanza.md` — two-track stanza
  shape (Track A: `eval_spec`; Track B: `infra` verification or
  `human_review_required` if mid-sprint downgrade fires).
- `feedback_commit_at_end_bundles_deliver_artefacts.md` — dev SHALL
  not stage deliver-agent files; bundling at close is the human's
  decision.
- `feedback_probe_sprint_shape_for_conditional_broadening.md` final
  paragraph — Sprint 29 (the (R2) probe sprint that authors
  CaseSpecs) is NOT a docs-only probe; it follows the standard
  semantic-touching / §7-stanza-required pattern.
- `iteration_governance.md` §1 + §3 + §5 + §7 (Constitution + Fix
  Layer Classification + Eval Acceptance Rules + sprint-objective
  stanza requirement).

## 14. Handoff document contract (12 sections, per Sprint 19/20/27)

The dev SHALL produce `docs/sprints/sprint-029-handoff.md` with the
following sections (mirroring the two-track Sprint 19/20 pattern):

1. Context Pack (relevant docs, code paths, doc-status warnings,
   source-of-truth decision, implementation status, risks)
2. Sprint-objective recap
3. Premise re-verification — confirm §4 premise items at session
   start; do NOT re-derive; surface drift if found.
4. Track A — CaseSpec authoring
   - 4.1 Files authored (paths, per-case provenance, manifest entry
     choice)
   - 4.2 Per-directive design rationale (informational, per §6.3)
   - 4.3 §6.2 authoring-discipline self-check (anti-hardcode walk)
5. Track A — Smoke run results
   - 5.1 Run command + result file path
   - 5.2 Per-case observation table (precondition fires; directive
     followed; bot-turn count; phase transitions observed)
   - 5.3 Per-directive aggregate (target directives confirmed /
     non-confirmed)
6. Track B — (c) phase-derivation verification
   - 6.1 Question 1–4 from §7.1 with reproducible extraction
   - 6.2 Verdict (works / does-not-work with failure mode)
   - 6.3 If failure: proposed follow-on R-item shape + scope
7. R-item disposition update (line 450)
8. Anti-hardcode self-walk (§4.1; nine questions)
9. Files changed (diff scope; pre-existing untouched mods)
10. Layer-classification self-walk (§3; per-track)
11. Open questions for the human
12. Closure verdict (filled at close; deliver-agent owned per
    `feedback_handoff_verdict_section_delegation.md`)

## 15. Stop conditions (dev-agent)

- **Premise drift** — if §4 premise items don't verify at dev session
  start, STOP and report. Do NOT proceed.
- **Track B mid-sprint failure** — surface the failure + propose
  the follow-on R-item per §7.2. Do NOT retry the derivation
  analysis. Track A still lands; close the sprint with the downgrade
  documented.
- **Rubric widening temptation** — if Track A's bot-failure observation
  tempts a rubric edit to make the failure go away, STOP and report.
  The failure IS the finding.
- **Sprint 20 case-family edit temptation** — STOP. Cascade fence in
  force.
- **Trace-text encoding temptation** — if a `hidden_fact` is about to
  copy bot transcript text verbatim, STOP and redraft.
- **cs_040 routing surface touched** — if Track A's case shape
  touches the UC-K → UC-C routing fence (Sprint 22), STOP and report.
- **Mocked-LLM as primary evidence temptation** — STOP. Real-LLM
  execution is required.
- **`R-prompt-phase-plan-directive-followship` closure temptation** —
  STOP. R-item stays open; Sprint 29 updates disposition.
- **Sprint scope expansion temptation** — if work outside §8 (Files
  in scope) is needed, STOP and propose a follow-on R-item; do not
  silently expand scope.
