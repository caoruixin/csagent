---
title: Milestone M4-Eval-Cleanup — Evaluation harness + governance gap cleanup
doc_tier: milestone-archive
status: archived
implementation_status: implemented
source_of_truth: this file
last_reviewed: 2026-05-24
review_cadence: ad hoc
supersedes: [docs/milestones/M3-Eval_objective.md]
superseded_by: docs/milestone_objective.md (M5+ candidate selection pending)
notes: >
  M4-Eval-Cleanup is the fourth milestone under the §8 framework (after
  M1 DISCOVER + Intake; M2 Skill Registry Abstraction; M3-Eval Coarse-
  to-Fine Evaluation Architecture). Path 1 research-driven scoping —
  consumes the post-M3-Eval cleanup audit dispatched by deliver-agent
  + human 2026-05-23. The audit identified 9 P1 actionable items + 1
  design decision touching the M3-Eval four-tier evaluation pyramid
  (Tier-0 safety / Tier-1 outcome / Tier-2 skill_procedure_followship
  / Tier-3 advisory). This is the first explicitly cleanup-flavored
  milestone under §8 (M1/M2/M3-Eval were implementation /
  architecture-flavored); the shape — non-bad-case-anchored
  acceptance bar + multi-sub-sprint cleanup theme — establishes a
  precedent for future cleanup milestones.

  Audit source: deliver-agent verification (read-only) of a research-
  agent cleanup report covering 17 items (10 P1 + 7 P2). Verification
  confirmed 9 of 10 P1 claims (1 disputed: cs_interactive_095 still
  exists). The 9 verified items + 1 Tier-2 design decision form this
  milestone's scope.
---

# Milestone M4-Eval-Cleanup — Evaluation harness + governance gap cleanup

## 1. Milestone class

Mixed-layer milestone touching multiple semantic surfaces — §7 stanza
required per sub-sprint per `iteration_governance.md` §7:

- **S1 layer**: `infra` (CLI registration, default parallel, compact
  prompt cleanup) + `eval_spec` (fixture seeding, stale test migration).
- **S2 layer**: `eval_spec` (Alice fixture schema unification) +
  `infra` (executor suite-mode awareness).
- **S3 layer**: `eval_spec` (handover_completeness demotion) +
  `judge_calibration` (Tier-2 design decision) + possibly governance-
  touch (clarification of §5.6 wording if Tier-2 decision or
  handover_completeness demotion requires governance text).

No Tier-0 candidate expected; no §1.7 forbidden-list cross expected
under default scope.

## 2. Goal

Close the post-M3-Eval cleanup audit findings (9 P1 actionable items +
1 design decision on Tier-2 wiring) and re-align the eval harness
implementation with M3-Eval governance intent — smoke as observation-
only (§5.5), bad-case suite as primary acceptance gate (§5.6),
Tier-3 dims as advisory (M3-Eval pyramid). Reduce future-milestone-
close deliver-agent overhead by making `--set bad_cases` and
`--set anchor_outcome` selectable via CLI and stabilizing default
parallel for bad-case rerun reliability.

This is a CLEANUP milestone; no new feature; no new bad case; no new
runtime semantic surface.

## 3. Sub-sprint sequence (preliminary)

The preliminary sequence below is refined at each sub-sprint planning
round per `iteration_governance.md` §8.3 — deliver-agent + human may
re-shape S2 / S3 scope based on S1 findings.

### Sub-sprint 1 — Sprint 47 / S-Cleanup-1: Eval harness polish + R-item closure

**Class**: semantic-touching (eval_spec via fixture seeding); §7 stanza
required.

**Layer**: `infra` + `eval_spec`.

**Scope** (3 sentences): Land 6 small-scope polish items — register
`bad_cases` + `anchor_outcome` as CLI set names (P1#1); change default
`parallel` from 5 to 1 for bad-case rerun stability (P1#3); seed
`user_goal_achievement` on 2-3 representative production fixtures
(P1#6); migrate `test_escalation_enum_sync` from deleted v0_2 spec
path to v0_3 (P1#8); cleanup `--output` references in compact dev-
prompt template — sprint archives remain immutable (P1#10); verify
P1#7 cs_interactive_095 reference (disputed by audit — confirm or
document final disposition). Consumes R-items
`R-stale-test-escalation-enum-sync-v0_2-to-v0_3-migration` (CLOSE)
+ `R-bad-case-parallel-session-establishment-flakiness` (PARTIAL-
CLOSE — parallel default reduction is one of several mitigations).

**Estimated duration**: 1-2 dev days.

### Sub-sprint 2 — Sprint 48 / S-Cleanup-2: Bad-case fixture schema unification + executor suite-mode

**Class**: semantic-touching (eval_spec); §7 stanza required.

**Layer**: `eval_spec` + `infra`.

**Scope** (3 sentences): Resolve Alice bad-case fixture schema
inconsistency — Alice retains full legacy `outcome_checks` (7 items)
+ `llm_judge_dimensions` (3 items) while the other 11 bad cases have
empty lists per M3-Eval design intent (P1#5). Default direction
(approved 2026-05-23): strip Alice's legacy `outcome_checks` +
`llm_judge_dimensions` lists to align with M3-Eval empty-list schema;
**HOWEVER**, the strip MUST NOT weaken Alice's `closure_criterion`
or remove load-bearing `bad_case_metadata` (source_session_id,
surfaced_by, surfaced_date, failure_shape, expected_behavior) — those
are human-judgment-gate inputs per §5.6 and stay intact. S2 planning
verifies direction via M3-Eval design docs + deliver-agent + human
concurrence; if planning finds the reverse direction is safer (i.e.,
the 11 bad cases should grow legacy fields rather than Alice
stripping), STOP and surface to deliver-agent per §10. Add executor
suite-mode awareness — for `bad_cases/` + `anchor_outcome/` suites,
annotate report output to make explicit that programmatic
`case_passed` PASS/FAIL is NOT the human-judgment gate (per §5.6) —
likely a flag on `CaseResult.case_passed_authority: "human_review" |
"programmatic"` or report-level annotation (P1#2). Consumes R-item
`R-bad-case-fixture-migrate-to-l3-judge-dims` (CLOSE).

**Estimated duration**: 1-2 dev days.

### Sub-sprint 3 — Sprint 49 / S-Cleanup-3: Governance alignment — handover_completeness demotion + Tier-2 design decision

**Class**: semantic-touching (eval_spec + judge_calibration); §7
stanza required. Possibly §4.3 trigger #2 (semantic-decision touch);
deliver-agent + human jointly decide at S3 planning whether per-sub-
sprint Codex review is needed.

**Layer**: `eval_spec` + `judge_calibration` + (possibly) governance-
touch.

**Scope** (3 sentences): Demote `handover_completeness` +
`case_id_present` from mandatory-on-escalate (`composite.py:81,84` in
`_conditional_mandatory_l2`) to advisory Tier-3 per M3-Eval four-tier
pyramid intent — code change + (if needed) governance text
clarification in `iteration_governance.md` §5.6 or M3-Eval close
artefact pointer (P1#4). Resolve P1#9 Tier-2 wiring design decision:
full-Skill traversal (current `executor.py:_compute_tier2_result`
iterates `ext.skills_by_name`) vs runtime-selected Skill — deliver-
agent + human jointly decide at S3 planning round; outcomes:
(a) keep current full-Skill traversal + document design rationale in
`skill_procedure_check.py` docstring + governance pointer,
(b) switch to runtime-selected Skill (requires plumbing active_skill
through trace; out-of-scope for cleanup unless trivial), or
(c) hybrid (current traversal + warn if no runtime Skill matches).
Tier-2 design decision is the highest-risk item in M4-Eval-Cleanup;
if option (b) is chosen and exceeds cleanup scope, surface to
deliver-agent for milestone re-scoping or defer to a separate
milestone.

**Estimated duration**: 2-3 dev days.

## 4. Non-goals

- No new bad cases opened (cleanup-focused).
- No `semantic_planner` changes; no `prompt_projection` slot additions;
  no `skill_state` modifications; no Skill / SkillRegistry runtime
  touches.
- No new Tier-0 invariants; no `runtime_freeze_and_risk_policy.md`
  edits.
- No editing of `docs/sprints/sprint-NNN-*` files (immutable archives).
- No editing of `docs/milestones/M3-Eval_*` (immutable archive).
- No `iteration_governance.md` §1 (Constitution) edits.
- No `iteration_governance.md` §5.5 / §5.6 edits except where S3
  explicitly requires clarifying handover_completeness Tier-3 status
  or Tier-2 design decision rationale (Codex must verify if so).
- No `eval_interactive/case_specs/shadow/` reads (held-out, dev-blind).
- No M4+ candidate slate (M3-Eval §12.10) work — other M4+ candidates
  (M3-B Single Handover Orchestrator, UC-G/H/I/J bad-case seeding,
  semantic-planner soft-signal extension, M3-Corpus, Latency / Skill-
  Tuning / Tier-0 re-evaluation) remain in slate for separate planning
  rounds.

## 5. Milestone acceptance bar

Per `iteration_governance.md` §5.6, the primary acceptance gate is the
curated bad-case suite manual review. M4-Eval-Cleanup is a CLEANUP
milestone (NOT bad-case-driven); no new bad case is in scope. The
acceptance bar is anchored to suite-stability + Codex pass + per-item
verification:

1. **Bad-case suite regression-safety** — post-cleanup rerun at
   `parallel=1` against milestone-close HEAD reproduces the M3-Eval
   close distribution: PASS × 5 (cs001, cs014, cs029, cs066, fg5q) +
   IMPROVING × 4 (alice, cs011, cs012, wmkb) + FAIL × 3 (cs015, cs095,
   iwzx) + OOSR × 0. Per-case `terminal_outcome` and
   `closure_criterion` match observations are recorded in the close
   handoff. If a regression surfaces (any PASS case flips to FAIL),
   deliver-agent + human jointly classify B fix-iteration vs out-of-
   scope vs new R-item before close.
2. **Codex M4-Eval-Cleanup-shared review** returns `decision: pass /
   blocking_count: 0` on the cumulative commit range per `iteration_
   governance.md` §4.3 default milestone-shared.
3. **Java test suite** no new regression beyond inherited M3-Eval
   baseline `1163 / 1-inherited / 0 / 2` (the inherited
   `SystemPromptUserRequestedTiebreakerTest` failure since Sprint 24-
   era is the documented baseline; no NEW failures).
4. **Python test suite** improves: `test_escalation_enum_sync` PASSES
   post-S1 v0_3 migration. Baseline 5 fail → 4 fail (or fewer if other
   tests also touched).
5. **Tier-2 wiring decision documented** — option (a) / (b) / (c)
   rationale lives in `skill_procedure_check.py` docstring + cross-
   reference from M3-Eval close artefact OR a fresh M4-Eval-Cleanup
   close artefact.
6. **handover_completeness / case_id_present demotion verified** —
   `composite.py` no longer adds these dims to
   `_conditional_mandatory_l2` on `outcome_class == "escalate"`;
   cross-reference governance doc clarifies these are Tier-3 advisory
   per M3-Eval pyramid.
7. **CLI usability** — `eval-interactive run --set bad_cases
   --parallel 1` succeeds end-to-end (smoke verification, not a full
   rerun); same for `--set anchor_outcome`.
8. **Alice fixture aligned** — `alice_uc_a_uc_h_misclass.yaml`
   `outcome_checks` + `llm_judge_dimensions` lists are empty (default
   S2 direction) OR all 12 bad cases carry the same schema (alternate
   S2 direction).

## 6. Hard fences (milestone-level — sub-sprints inherit + may add more)

- No `server/src/main/java/**` runtime code touches (Tier-2 wiring
  cleanup is eval-side `executor.py` only; if S3 option (b) requires
  runtime changes, milestone halts for re-scoping).
- No `system_prompt.txt` edits.
- No Skill YAML edits (`server/src/main/resources/skills/*.yaml` —
  Skill YAMLs are runtime; eval-side fixtures only).
- No editing of `docs/milestones/M3-Eval_*` (immutable archive).
- No editing of any `docs/sprints/sprint-NNN-*` files (immutable
  archives).
- No new Tier-0 invariant; no `runtime_freeze_and_risk_policy.md`
  edits.
- No `iteration_governance.md` §1.7 (Constitution forbidden-list)
  edits.
- No `iteration_governance.md` §5.5 / §5.6 wording rewrites except a
  minimal clarification edit in S3 for handover_completeness Tier-3
  status or Tier-2 design decision rationale (Codex must verify the
  edit at milestone close).
- No new bad cases opened; no `eval_interactive/case_specs/bad_cases/`
  directory schema breaking change beyond S2 unification.
- No `eval_interactive/case_specs/shadow/` reads (held-out, dev-blind).
- No editing of `docs/codex-findings.md` mid-flight (review-agent
  territory; only deliver-agent resets at milestone close per §4.2).
- No M4+ candidate slate (M3-Eval §12.10) work; other candidates stay
  in slate.

## 7. R-items consumed / surfaced

**Consumed** (this milestone closes / partially closes these):
- `R-stale-test-escalation-enum-sync-v0_2-to-v0_3-migration` (S1
  CLOSE) — test path migration.
- `R-bad-case-fixture-migrate-to-l3-judge-dims` (S2 CLOSE) — Alice
  fixture schema unification.
- `R-bad-case-parallel-session-establishment-flakiness` (S1 PARTIAL-
  CLOSE — parallel default reduction is one mitigation; full closure
  requires Tier-2 wiring or session-establishment redesign).

**Possibly surfaced** (NEW R-items if scope creep or design decision
spawns follow-on work):
- If S3 Tier-2 design decision chooses option (b) (switch to runtime-
  selected Skill) or (c) (hybrid), spawn R-item `R-tier2-runtime-skill-
  plumbing-design` for a downstream milestone.
- If S3 handover_completeness demotion reveals other Tier-3 dims also
  incorrectly mandatory, spawn R-item `R-tier3-advisory-demotion-
  audit`.
- If S2 unification direction reveals deeper schema fragmentation
  (e.g., more than 1 deviating fixture), spawn R-item `R-bad-case-
  fixture-schema-canonicalize` for a future cleanup pass.

**Not consumed** (deferred to separate M4+ candidates):
- `R-bad-case-suite-uc-ghij-seed-from-real-sessions` (deferred to
  separate M4+ UC-G/H/I/J bad-case seeding milestone — requires real-
  session source material).
- `R-iwzx-uc-k-vs-uc-h-routing-spurious-distress` (deferred to
  semantic-planner soft-signal extension M4+ candidate).

## 8. Codex review plan

Per `iteration_governance.md` §4.3 default: **milestone-shared review
at M4-Eval-Cleanup close** (one Codex review against the cumulative
commit range covering S1 + S2 + S3).

Per-sub-sprint review triggers per §4.3:
- **S-Cleanup-1**: NOT a trigger (infra + char-test + R-item closure;
  no Tier-0 candidate; no §1.7 cross; no hard-fence violation).
  Default deferred to milestone close.
- **S-Cleanup-2**: NOT a trigger (eval_spec fixture schema unification
  + executor suite-mode annotation; no Tier-0 candidate; no §1.7
  cross). Default deferred to milestone close.
- **S-Cleanup-3**: **POSSIBLY TRIGGER #2** — Tier-2 wiring change if
  option (b) or (c) is chosen could cross §1.7 boundary (per-Skill
  mapping introduction or runtime-selected-Skill plumbing); the
  handover_completeness demotion if it touches §5.6 wording might
  trigger §4.3 trigger #4 (governance-touch); deliver-agent + human
  jointly decide at S3 planning whether per-sub-sprint Codex review
  is needed. Default position: defer to milestone close UNLESS the
  Tier-2 decision lands on option (b) or (c) with non-trivial
  plumbing.

## 9. Estimated milestone duration

4-7 dev days total (S1: 1-2 days, S2: 1-2 days, S3: 2-3 days).
Calendar estimate 1-2 weeks depending on Codex review cadence,
sub-sprint spacing, and any S3 per-sub-sprint Codex review trigger.

## 10. Stop conditions

- If S1 default `parallel=1` change introduces NEW bad-case suite
  failures (any PASS → FAIL flip), halt S1 and re-scope.
- If S2 fixture unification direction reveals Alice schema is actually
  closer to M3-Eval intent than the other 11 bad cases (opposite of
  default direction), halt S2 and re-scope (might require updating
  the 11 bad cases instead of stripping Alice).
- If the S2 Alice strip would require removing or weakening
  `closure_criterion` or load-bearing `bad_case_metadata` fields
  (source_session_id, surfaced_by, surfaced_date, failure_shape,
  expected_behavior) to align with the 11-case empty-list schema,
  halt S2 — those are §5.6 human-judgment-gate inputs and must be
  preserved regardless of unification direction. Re-scope or surface
  to deliver-agent for separate R-item.
- If S3 Tier-2 design decision lands on option (b) (switch to runtime-
  selected Skill) AND requires `server/src/main/java/**` touches,
  halt the milestone — this crosses the §6 hard fence; re-scope as a
  separate runtime-touching milestone.
- If S2 or S3 reveals deeper schema fragmentation or governance
  inconsistency beyond the audit findings, halt and re-scope or open
  new R-items.
- If Codex per-sub-sprint review trigger fires unexpectedly on S1 or
  S2, halt and re-scope.

## 11. Cross-milestone sequencing context

M3-Eval was the immediate predecessor (closed 2026-05-23 A — Clean
PASS); the four-tier evaluation pyramid is in place but with
documented gaps that M4-Eval-Cleanup addresses. Other M4+ candidates
in the slate (per `docs/milestones/M3-Eval_objective.md` §12.10):

1. M3-B Single Handover Orchestrator (release-gate-blocker if
   Salesforce calendar pressure surfaces).
2. M4 Bad-case fixture migration + parallel-session flakiness fix —
   **PARTIALLY ABSORBED** by M4-Eval-Cleanup S1 + S2.
3. M4 UC-G/H/I/J bad-case seeding — separate Path 2 milestone.
4. M4 Semantic-planner soft-signal extension (iwzx R-item) — separate
   milestone.
5. M4 M3-Corpus parallel-track — separate research-driven milestone.
6. M4 Latency / Skill-Tuning / Tier-0 re-evaluation — separate
   milestone.

M4-Eval-Cleanup is the cleanup track; bad-case-driven (#3, #4) and
feature-driven (#1, #5, #6) M4+ tracks proceed on their own cadence.
Cleanup-track precedent under §8: this is the first explicitly
cleanup-flavored milestone; the shape — non-bad-case acceptance bar
+ multi-sub-sprint cleanup theme — establishes a precedent for future
cleanup milestones if needed.

## 12. M4-Eval-Cleanup closure verdict (appended at milestone close 2026-05-24)

**Filled by deliver-agent + human jointly at M4-Eval-Cleanup milestone close 2026-05-24 per `iteration_governance.md` §8.4.**

### 12.1 Classification

**A — Clean PASS.**

M4-Eval-Cleanup closed the post-M3-Eval cleanup audit (9 verified P1 items + the #9 Tier-2 design decision) across three sub-sprints, all eval-harness-side with **zero `server/` touch**. The Codex milestone-shared review returned `fix_required / 1` on its first pass (P0-F1 — the §5.6 bad-case close package was not yet present + cs029 was a session-establishment flake in the cited run); both gaps were the deliver-agent's close-package timing, not a dev-code issue. After the deliver-agent completed the bad-case close package (M4 ledger + cs029 isolated rerun) and committed S-Cleanup-3, the Codex re-review against the committed range cleared P0-F1 and returned `pass / 0`. No sub-sprint code change was requested. M4 closes Clean PASS on the code/anti-hardcode axis + the bad-case regression-safety primary gate + Tier-0 safety floor preservation.

### 12.2 Cumulative commit range + baselines

- **Cumulative commit range**: `4344662..d5b1508` (6 commits: S1 dev `1576070` + S1 close `8ccedbf` + S2 dev `b989833` + S2 close `36ade6e` + governance consolidation `8f32dbd` + S3 dev + bad-case close `d5b1508`).
- **Java test baseline**: `Tests run: 1163, Failures: 1, Errors: 0, Skipped: 2` UNCHANGED across all three sub-sprints (zero `server/` touch). The 1 inherited `SystemPromptUserRequestedTiebreakerTest...:53` failure (Sprint 24-era) persists; not M4-attributable.
- **Python test baseline**: `3 failed, 460 passed` via `uv run python -m pytest` at HEAD `d5b1508` (Codex re-review independent confirmation). The `uv run pytest` console-script segfault is a stale `.venv/bin/pytest` shebang (OQ-S47.3 root cause), NOT an M4 regression. S-Cleanup-1 #8 fixed the 2 `test_escalation_enum_sync` failures; the residual 3 are env-specific (`test_case_spec_overrides` + `test_corpus_lint`).
- **New test counts (cumulative)**: ≈ +6 (S1 #8 enum-sync migration) + +17 (S2 case_passed_authority) + +15 (S3 tier2 phase-plan-scoping + composite-gate) ≈ +38 NEW Python tests; +0 NEW Java tests.

### 12.3 Codex milestone-shared verdict (first pass → re-review 2026-05-24)

- **decision**: `fix_required → pass`. First-pass Codex verdict was `fix_required / blocking_count: 1` (P0-F1: bad-case close package missing + cs029 zero-turn CONTRACT_VIOLATION in run `20260523-075141`). The deliver-agent completed the §5.6 close package (M4 ledger in `_manifest.md` + cs029 isolated rerun `20260523-095557`) and committed S-Cleanup-3 (`d5b1508`); the Codex re-review against the committed range cleared P0-F1 and returned `pass / blocking_count: 0`. NOT a dev-code fix — the blocker was deliver-agent close-package timing (Codex reviewed before the deliver-agent finished the §5.6 bad-case manual review). Both passes archived in `docs/milestones/M4-Eval-Cleanup_codex-review.md`.
- **Architectural axis (re-confirmed against `d5b1508`)**: §4.1 nine-question kernel Q1-Q9 all PASS; §1.7 boundary all PASS; all 11 §6 hard fences PASS (headline: empty `git diff 4344662..d5b1508 -- server/`); §5 bad-case regression-safety verified.
- **Cumulative coherence judgment** (verbatim): "M4-Eval-Cleanup now ships as a coherent cleanup milestone. S1 keeps human-judgment suites out of `--set all`, S2 annotates `case_passed_authority` so bad-case results cannot be mistaken for programmatic gates, and S3 aligns Tier-2 with M3-Eval intent by consuming the runtime's presented `critical_steps` while demoting process-completeness dimensions to advisory trend signals."

### 12.4 Per-sub-sprint Codex verdicts

None. All three sub-sprints (S-Cleanup-1/2/3) deferred Codex to the milestone-shared close per `iteration_governance.md` §4.3 default. S-Cleanup-3's §8 RECOMMENDED a per-sub-sprint review (it touches the `case_passed` Tier-2 gate), but since S3 was the LAST sub-sprint the milestone-shared review covered its commit range; the recommended review was folded into the milestone-shared review (which gave #9 the highest scrutiny).

### 12.5 Bad-case suite manual review verdict (PRIMARY GATE per §5.6)

Real-LLM rerun 2026-05-23/24 (Moonshot `moonshot-v1-32k` simulator/judge). Result paths: main batch (parallel=1) `eval_interactive/results/20260523-075141/` + cs029 isolated rerun `eval_interactive/results/20260523-095557/`. Per-case verdicts recorded in `eval_interactive/case_specs/bad_cases/_manifest.md` ledger M4 column + "M4-Eval-Cleanup close bad-case suite manual review (2026-05-24)" section.

**Distribution**: **PASS × 5** (cs001, cs014, cs029, cs066, fg5q) + **IMPROVING × 4** (alice, cs011, cs012, wmkb) + **FAIL × 3** (cs015, cs095, iwzx — raison d'être) + **OOSR × 0**. **IDENTICAL to M3-Eval close** → §5.1 regression-safety bar MET (cleanup milestone, zero bot-code change; no PASS→FAIL flip).

**Tier-0 safety floor**: effectively intact. The 2 `no_pii_leakage` programmatic tags (cs011 T1, cs012 T3) are benign email-regex false positives (system `noreply@gumtree.com`; user-echoed fixture `kitten.seller@example.com`), not third-party leaks — recorded note-only per the human's 2026-05-24 disposition. #9's zero-misflip property holds on the suite (`escalate-via-request-handover` absent from all `failed_step_ids`); #4 demotion observable (cs066 `case_id_present` informational, not gating).

### 12.6 Tier-0 candidate dispositions

No new Tier-0 invariant. C2 (Skill guardrail non-overridability) + C3 (Skill state bus boundary enforcement) remain DEFERRED per M2-close; M4-Eval-Cleanup was eval-side and did not trigger re-evaluation. Status unchanged in `docs/action_bank.md` §5.2.

### 12.7 R-item flips

- ✅ **CLOSED during M4**: `R-stale-test-escalation-enum-sync-v0_2-to-v0_3-migration` (S1 #8 markdown-regex migration); `R-bad-case-fixture-migrate-to-l3-judge-dims` (S2 #5 Alice strip — LAST legacy fixture).
- 🔁 **PARTIAL-CLOSED**: `R-bad-case-parallel-session-establishment-flakiness` (S1 #3 parallel default; cs029's M4-close flake reconfirms the root-cause investigation is still open — annotated at M4 close).
- 🆕 **NEW R-items opened during M4** (all deferred): `R-case-families-manifest-cs095-smoke-vs-anchor-orphan` (S1); `R-bad-case-metadata-field-name-canonicalize` (S2); `R-eval-report-observability` (S2; absorbs OQ-S49.3).
- **No new R-items at M4 close** (per the human's note-only disposition 2026-05-24).

### 12.8 P3 dispositions (note-only) + architecture-health direction

Codex surfaced 3 non-blocking P3 findings; per the human's 2026-05-24 note-only disposition, all are recorded here + in the close handoff with no new R-items:
- **P3-F1** — the S49 handoff §6/§6.4 has stale/internally-inconsistent bad-case numbers ("all 12 ran end-to-end" + "6 true / 6 false"; the real run-`075141` split is 5 true / 7 false with cs029 a 0-turn CONTRACT_VIOLATION). Superseded by the authoritative `_manifest.md` M4 section; corrected via the handoff §12 close annotation.
- **P3-F2** — `outcome_checks.py` comments (lines 71/81/88) still describe `handover_completeness` / `case_id_present` as conditionally-mandatory / mirrored with `composite._conditional_mandatory_l2` (stale after #4 made it return `()`). Code is correct (intentional score-retention per S3 handoff §9); comment-accuracy nit only.
- **P3-F3** — `.venv/bin/pytest` stale shebang (OQ-S47.3 root cause); `uv run python -m pytest` is the reliable command. Env/reproducibility note; venv-regen is the fix.

Architecture-health (`iteration_governance.md` §6): `new_semantic_hardcode_count` = 0 (Codex Q1 PASS); #9 + #4 SHRINK the deterministic surface (soft-signal direction); `planner_ownership_ratio` unchanged (eval-side only); shadow integrity preserved (no shadow reads).

### 12.9 OQ dispositions

- **OQ-S49.1** (residual 148/159 anchor FAILs) — `closed-with-followup`: legitimate bot-side / contract-trace failures, not eval-gate bugs; route to existing M5+ candidates (UC-G/H/I/J seeding, semantic-planner, contract-trace); no new R-item per note-only.
- **OQ-S49.2** (§5.6 governance fold-in for #4) — `closed`: no §5.6 edit needed; the demotion is carried by the `composite.py` docstring + the manifest + this verdict.
- **OQ-S49.3** (composite-threshold vs gate reporting) — `closed-with-followup`: fits existing `R-eval-report-observability`.
- **OQ-S47.3** (Python baseline framing) — `closed-with-followup`: root cause = stale `.venv/bin/pytest` shebang; reliable command `uv run python -m pytest` = `3 failed, 460 passed`; venv-regen is the fix (noted, no R-item).

### 12.10 Next milestone (M5+) planning round seed

M4-Eval-Cleanup closes; `sprint_objective.md` + `milestone_objective.md` reset to next-milestone-TBD placeholder per this close-out bundle. Candidate next milestones (deliver-agent + human pick at next planning round; ordered roughly by release-gate proximity), carried from the M3-Eval §12.10 slate minus the M4-Eval-Cleanup-absorbed cleanup items:

1. **M3-B Single Handover Orchestrator** — `docs/release_gate.md` §1.1 release-gate-blocker.
2. **UC-G/H/I/J bad-case seeding** — consume `R-bad-case-suite-uc-ghij-seed-from-real-sessions`; requires real-session source material (Alice precedent).
3. **Semantic-planner soft-signal extension** — consume `R-iwzx-uc-k-vs-uc-h-routing-spurious-distress` + per-UC moderation-context projection.
4. **M3-Corpus** — separate parallel-track; `R-corpus-coverage-audit-per-uc`.
5. **Latency / Skill-Tuning / Tier-0 re-evaluation** — preceding candidates remain in slate.

No pre-decision applied. Deliver-agent + human pick at next planning round.
