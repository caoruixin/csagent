---
title: Milestone M4-Eval-Cleanup — Evaluation harness + governance gap cleanup
doc_tier: current-runtime
status: proposal
implementation_status: not_started
source_of_truth: this file
last_reviewed: 2026-05-23
review_cadence: per milestone
supersedes: []
superseded_by: null
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

## 12. Closure verdict (deferred to milestone close)

To be appended at M4-Eval-Cleanup close per `iteration_governance.md`
§8.4 + `compact/sprint-deliver-orchestrator.md` close-out discipline.
Sections 12.1-12.N follow the M3-Eval archive shape (objective +
sub-sprint disposition + per-acceptance-bar PASS/FAIL + Codex
M4-shared review pointer + R-item flips + cross-milestone sequencing
notes + closure classification).
