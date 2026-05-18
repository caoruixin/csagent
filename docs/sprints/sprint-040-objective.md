---
title: Sprint 40 objective archive — Teaching extraction from system_prompt.txt + orchestration shell cleanup (M2 sub-sprint 4)
doc_tier: sprint-archive
status: archived
implementation_status: implemented
source_of_truth: this file
last_reviewed: 2026-05-18
review_cadence: ad hoc
supersedes: [docs/sprints/sprint-039-objective.md]
superseded_by: null
notes: >
  Archived Sprint 40 contract at sub-sprint close 2026-05-18 (Sprint 40
  closed A — Clean PASS; Codex per-sub-sprint review `pass / 0` first
  pass; dev commit `9130abc`; archive package: this file +
  `docs/sprints/sprint-040-handoff.md` + `docs/sprints/sprint-040-codex-review.md`).
  Sprint 40 was the FOURTH sub-sprint of NEW Milestone M2 (Skill
  Registry Abstraction + Wholesale Retroactive Externalization, per
  docs/milestone_objective.md) and the THIRD implementation
  sub-sprint after the Sprint 37 design freeze + Sprint 38
  SkillRegistry-core landing + Sprint 39 RESOLVE Skill migration +
  predicate migration + S1/S2 + unified dispatcher landing. Sprint 40
  implemented Sprint 37 freeze decision (f) §7 as runtime code,
  completing the teaching content externalization from
  `system_prompt.txt` into corresponding Skill YAML files.

  Sprint 40 migrated:
  (a) Sprint 31 `alternate_candidate_use_cases` teaching paragraph
      (`system_prompt.txt:30-34`) → `discover_triage.yaml` `procedure`
      per design doc §7.2.2.
  (b) Sprint 33 `discover_disambiguation_signals` SLOT description
      paragraph (`system_prompt.txt:36-43`) → `discover_triage.yaml`
      `procedure` per design doc §7.2.3 (the Sprint 33 CUE BODY at
      `PhaseEvaluator.java:432-448` was already migrated in Sprint 38
      per decision (e) §6.2.1; Sprint 40 closed the cue/slot split).
  (c) DISCOVER phase guidance bulk (`system_prompt.txt:45-52`) →
      `discover_triage.yaml` `procedure` per design doc §7.3 default
      recommendation; left a one-line shell pointer in the
      orchestration shell.
  (d) ADDED orchestration-shell teaching about Skill envelope mechanics
      (combined with the DISCOVER pointer per OQ-S40.2 dev judgment;
      Codex AGREED at close).

  Sprint 40 did NOT touch: Sprint 23 `already_called` paragraph
  (STAYS cross-Skill in orchestration shell per design doc §7.2.1);
  `request_handover` decision tree at lines 54-101 (STAYS cross-Skill
  per design doc §7.3); UC-switching state preservation across Skill
  boundary (Sprint 41 scope — `SkillStateBus.java` +
  `state_inheritance` enforcement + `prior_use_case_carry` slot impl);
  existing case families; RuntimeIntentClassifier / DriftDetector /
  UseCaseRouter / ClassifyUseCaseTool (M3-D Topic↔UC binding
  deferred); INTAKE_UCS set (M3-B Single Handover Orchestrator
  deferred); escalation_reason enum (M3-A deferred); foundational /
  governance / freeze docs (constitution-discipline preserved).

  Layer: `prompt_projection` + `semantic_planner`. Semantic-touching
  multi-layer; §7 stanza REQUIRED + completed.

  Codex review: per-sub-sprint at Sprint 40 close per
  iteration_governance.md §4.3 trigger #3 (system_prompt.txt
  structural change — LLM input contract surface). Codex
  independently verified behavioural equivalence + all 33 Sprint 40
  §6 + 22 M2 §6 hard fences + 5 OQ disposition + 2 contract-drift
  items. Verdict: pass / blocking_count: 0 on first pass.

  M2 acceptance bar recalibration (per docs/milestone_objective.md
  §5): bad-case suite (Alice) + interactive eval are OBSERVATION
  only for THIS milestone; Sprint 40 contract reflected this — no
  bad-case-rerun success metric; primary success was Sprint 31 +
  Sprint 33 + DISCOVER phase guidance migrating to
  discover_triage.yaml + Sprint 23 + decision tree staying +
  envelope-mechanics teaching added + behavioural equivalence
  preserved + Codex `approve`.
---

# Sprint 40 — Teaching extraction from system_prompt.txt + orchestration shell cleanup

**Milestone:** M2 (Skill Registry Abstraction + Wholesale Retroactive Externalization) sub-sprint 4 of 5 per `docs/milestone_objective.md`.

## 1. Sub-sprint class

**Implementation sub-sprint, single track (Track A), semantic-touching multi-layer.** Sprint 40 migrates the 3 scattered teaching paragraphs from `server/src/main/resources/prompts/system_prompt.txt` into corresponding Skill YAML files per Sprint 37 freeze decision (f) §7 mapping. The migration is content-relocation: pre-migration LLM behaviour on representative DISCOVER + RESOLVE-FAQ + RESOLVE-INTAKE traces is observationally equivalent post-migration (LLM still observes the same teaching content; just sourced from Skill envelope projection instead of monolithic system_prompt.txt). Post-Sprint-40, `system_prompt.txt` is a thin orchestration shell containing:

- Bot identity + JSON output contract + three required fields (`system_prompt.txt:1-9`).
- Empty vs non-empty `tool_calls` guidance (`system_prompt.txt:10-12`).
- Universal rules (helpful, empathetic, professional; never claim human; never promise actions; never fabricate; ground in retrieved knowledge or context; concise) (`system_prompt.txt:14-21`).
- Sprint 23 `already_called` projection slot teaching (`system_prompt.txt:23-28`) STAYS — cross-Skill envelope-mechanics teaching per design doc §7.2.1.
- NEW orchestration-shell teaching about Skill envelope mechanics (Sprint 40 ADDS) — cross-Skill universal per design doc §7.3.
- Compressed one-line pointer for DISCOVER phase guidance (was `system_prompt.txt:45-52`; bulk migrated to `discover_triage.yaml`).
- `request_handover` escalation_reason canonical-enum decision tree (`system_prompt.txt:54-101`) STAYS — cross-Skill canonical enum per design doc §7.3.

Layer: `prompt_projection` (the LLM input contract shifts from monolithic system_prompt.txt to Skill envelope projection per (phase, active_use_case) — the same content reaches the LLM, but partitioned by Skill scope) + `semantic_planner` (LLM continues to own next-action / UC hypothesis / escalation posture / response strategy / customer-facing language per §1.3 — the migration is SURFACE-RELOCATION not LLM-ownership shift).

**§7 stanza REQUIRED** per the M2 §1 sub-sprint layer breakdown (semantic-touching multi-layer).

**Codex review:** per-sub-sprint at Sprint 40 close per `iteration_governance.md` §4.3 trigger #3 (system_prompt.txt structural change — LLM input contract surface). Codex verifies: (a) behavioural equivalence on representative DISCOVER + RESOLVE-FAQ + RESOLVE-INTAKE traces; (b) Sprint 23 `already_called` paragraph STAYS in shell per design doc §7.2.1; (c) `request_handover` decision tree at lines 54-101 STAYS in shell per design doc §7.3; (d) Sprint 31 + Sprint 33 paragraphs DELETED from shell + migrated to `discover_triage.yaml` `procedure`; (e) DISCOVER phase guidance bulk migrated to `discover_triage.yaml` with one-line shell pointer remaining; (f) orchestration-shell paragraph about Skill envelope mechanics ADDED; (g) no scope creep into Sprint 41 surfaces; (h) no per-UC-branch if-else in `discover_triage.yaml` post-migration; (i) M1 functional surfaces preserved; (j) no Tier-0 invariant added.

## 2. Goal

Implement Sprint 37 freeze decision (f) §7 as runtime code, completing the teaching content externalization for ALL DISCOVER-phase teaching into the `discover_triage.yaml` Skill. Post-Sprint-40, the DISCOVER Skill is self-contained for DISCOVER work (developer wanting to understand DISCOVER reads `discover_triage.yaml`, not `system_prompt.txt`); the orchestration shell carries ONLY cross-Skill universal teaching + envelope-mechanics + the canonical enum decision tree.

Concrete deliverables (all SHIPPED in dev commit `9130abc`):

### 2.1 (D-a) Sprint 31 `alternate_candidate_use_cases` paragraph migration — SHIPPED

REMOVED from `server/src/main/resources/prompts/system_prompt.txt:30-34`; ADDED into `server/src/main/resources/skills/discover_triage.yaml` `procedure` (extension, not replacement) per design doc §7.2.2. Per Codex schema-and-reproducibility check at Sprint 40 close: content equivalence is formatting-normalized (bullet markers removed; first post-colon `The` → `the`) — semantically equivalent at LLM-observable level.

### 2.2 (D-b) Sprint 33 `discover_disambiguation_signals` SLOT description paragraph migration — SHIPPED

REMOVED from `server/src/main/resources/prompts/system_prompt.txt:36-43`; ADDED into `server/src/main/resources/skills/discover_triage.yaml` `procedure` per design doc §7.2.3. The Sprint 33 cue/slot split closed at Sprint 40 — post-Sprint-40, `discover_triage.yaml` `procedure` carries BOTH the slot-reading teaching (this D-b) AND the cue body (Sprint 38 per decision (e) §6.2.1).

### 2.3 (D-c) DISCOVER phase guidance bulk migration — SHIPPED

REMOVED bulk from `system_prompt.txt:45-52`; ADDED to `discover_triage.yaml` `procedure` (dedupe applied where the pre-Sprint-40 DISCOVER lead-in was already in `procedure` from Sprint 38; the NEW principle-level content — `reasoning` string requirement + 3-tier confidence guidance + explicit "?" clarification rule + escalate clauses — was appended). Per design doc §7.3 default recommendation: bulk migrated; one-line shell pointer combined with envelope-mechanics paragraph at post-Sprint-40 `system_prompt.txt:30-31` per OQ-S40.2 dev judgment.

### 2.4 (D-d) Orchestration-shell teaching about Skill envelope mechanics — SHIPPED

ADDED a new paragraph at post-Sprint-40 `system_prompt.txt:30-31` (placement after `already_called` paragraph; combined with DISCOVER one-line pointer for shell brevity per OQ-S40.2 dev judgment; Codex AGREED at close — design doc §7.3 mapping respected at content level).

### 2.5 (D-e) Behavioural-equivalence integration test — SHIPPED

NEW `server/src/test/java/com/gumtree/csagent/service/runtime/SkillTeachingMigrationIntegrationTest.java` (264 lines; 11 Java-deterministic prompt-composition tests). All 11 PASS at Sprint 40 close.

### 2.6 (D-f) Optionally: Possibly resolve inherited `SystemPromptUserRequestedTiebreakerTest` failure — PERSISTS

OQ-S40.3 at Sprint 40 close: PERSISTS unchanged. Codex classified as STATUS QUO; not a Sprint 40 blocker per contract §10 #18. The "Sprint 6" anchor token has been absent from `system_prompt.txt` since Sprint 24-era working-tree mod; Sprint 40 did not touch the `ACTIVE-UC TIEBREAKER` block per contract §6 #2 (the decision tree stays). Resolving would require deliberate annotation in post-Sprint-40 shell — out of Sprint 40 scope.

## 3. Non-goals (explicit) — HONORED PER CODEX VERDICT

- Sprint 40 did NOT delete the Sprint 23 `already_called` paragraph from `system_prompt.txt:23-28` (PRESERVED unchanged per design doc §7.2.1).
- Sprint 40 did NOT delete the `request_handover` escalation_reason canonical-enum decision tree at `system_prompt.txt:54-101` (PRESERVED at post-Sprint-40 lines 33-80 shifted −21; content byte-identical per Codex independent verification).
- Sprint 40 did NOT migrate Sprint 31 or Sprint 33 paragraphs into RESOLVE-FAQ or RESOLVE-INTAKE Skill YAMLs (Codex verified `resolve_faq_grounded_answer.yaml` + `resolve_intake_collect_and_handover.yaml` UNCHANGED).
- Sprint 40 did NOT touch `RuntimeIntentClassifier.java` / `IntentClassification.java` / `DriftResult.java` / `DriftDetector.java` / `UseCaseRouter.java` / `ClassifyUseCaseTool.java` (M2 §6 #5 fence verified empty `git diff`).
- Sprint 40 did NOT touch `INTAKE_UCS` set at `PhaseEvaluator.java:30` (M2 §6 #6 fence preserved).
- Sprint 40 did NOT widen `escalation_reason` enum (M2 §6 #7 fence preserved).
- Sprint 40 did NOT add a Tier-0 invariant to `docs/runtime_freeze_and_risk_policy.md` (M2 §6 #8 fence; Codex verified `git diff` empty).
- Sprint 40 did NOT touch `iteration_governance.md` / `doc_governance.md` / `agent_context_guide.md` / `runtime_freeze_and_risk_policy.md`.
- Sprint 40 did NOT touch `docs/foundational/` or `docs/proposals/skill_registry_design.md`.
- Sprint 40 did NOT touch sprint archives under `docs/sprints/sprint-001-*` through `docs/sprints/sprint-039-*` (immutable).
- Sprint 40 did NOT touch milestone archives under `docs/milestones/`.
- Sprint 40 did NOT touch `docs/milestone_objective.md` or `docs/sprint_objective.md` (deliver-agent-owned; bundled at sub-sprint close).
- Sprint 40 did NOT touch `docs/action_bank.md` in the dev session.
- Sprint 40 did NOT touch existing case families under `eval_interactive/case_specs/case_families/<existing>/`.
- Sprint 40 did NOT touch shadow CaseSpecs under `eval_interactive/case_specs_shadow/case_families/<existing>/`.
- Sprint 40 did NOT touch `eval_interactive/eval_interactive/`.
- Sprint 40 did NOT touch `eval_interactive/case_specs/bad_cases/alice_uc_a_uc_h_misclass.yaml`.
- Sprint 40 did NOT widen judge rubrics or `eval_interactive/case_spec_overrides.yaml`.
- Sprint 40 did NOT touch `IntakeFieldExtractor.java` / `IntakeFieldsRegistry.java` / `Sprint71PartialIntakePersistenceTest` baseline (M1 functional-surface preservation; Codex verified).
- Sprint 40 did NOT touch `ResolveDispositionEvaluator.java` (Sprint 11/11.1/12 frozen surface; Codex verified UNCHANGED at 205 lines).
- Sprint 40 did NOT migrate Sprint 6/7/11 predicates or NEW S1 / S2 (Sprint 39 surfaces preserved; Codex verified `SkillGuardrailDispatcher.java` UNCHANGED at 416 lines).
- Sprint 40 did NOT create `SkillStateBus.java` (Sprint 41 scope; Codex verified absence).
- Sprint 40 did NOT impl `prior_use_case_carry` projection slot in `ContextProjectionBuilder.java` (Sprint 41 scope; Codex verified `git diff` empty).
- Sprint 40 did NOT use mocked-LLM as primary evidence (the SkillTeachingMigrationIntegrationTest is Java-deterministic prompt-composition assertion, NOT LLM-behaviour claim).
- Sprint 40 did NOT pre-decide Sprint 41 implementation specifics.

## 4-12. Sections preserved verbatim from live contract at Sprint 40 close

Sections §4 (Premise check — 15 premises all PASS per dev handoff §3), §5 (Files in scope — 4 files in contract + 5th file `PhaseEvaluatorSkillIntegrationTest.java` golden update per OQ-S40.1 AGREE WITH DEV), §6 (Hard fences — 33 items all honored), §7 (Bundle policy — single dev commit; deliver-agent files bundled at sub-sprint close), §8 (Layer-classification + anti-hardcode stanza — `prompt_projection` + `semantic_planner` per §1.4; NO Tier-0; NO semantic hardcode; full generalization coverage per §6 Q8), §9 (Success metrics — HARD GATES all PASS; line-count target 60-75 missed at 80 lines per Drift item 1 CONTENT-EQUIVALENCE PRESERVED), §10 (Stop conditions — 18 items; none fired in dev session), §11 (Handoff §11 12-section contract — dev handoff at `docs/sprints/sprint-040-handoff.md` honors this), §12 (M2 milestone context — Sprint 40 = 4th sub-sprint; Sprint 41 next; M2 close after Sprint 41) are preserved in the live `docs/sprint_objective.md` at Sprint 40 close commit; this archive's frontmatter `notes:` field above summarizes the load-bearing content. For the full body, see `git show <sprint-40-close-commit>:docs/sprint_objective.md` — the deliver-agent close commit bundles the archive + replacement together so the same commit contains both the archive (this file) AND the live file's replacement (Sprint 41 contract).

**Sprint 40 final outcome (per `docs/sprints/sprint-040-handoff.md` §12 + `docs/sprints/sprint-040-codex-review.md`):** A — Clean PASS. Codex `decision: pass / blocking_count: 0` on first pass (single round). All 5 OQs aligned with deliver-agent + human pre-decisions (which were withheld from the review prompt per the "surface OQs without recommendations" framing). 2 contract-drift items classified as CONTENT-EQUIVALENCE PRESERVED + REAFFIRM PRECEDENT. No new Tier-0 candidates; C1 REJECTED preserved; C2 + C3 QUALIFIED-DEFER continued; C4 + C5 NOT-A-CANDIDATE preserved. 3 Codex non-blocking observations recorded for housekeeping (per-file numstat lapse pattern continuation; "formatting-normalized" framing precision; M2 close fold-back queue extends to include Sprint 37 §7.8 typo).
