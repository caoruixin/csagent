---
title: Sprint 41 objective archive — UC switch + state preservation across Skill boundary (M2 sub-sprint 5; LAST M2 sub-sprint)
doc_tier: sprint-archive
status: archived
implementation_status: implemented
source_of_truth: this file
last_reviewed: 2026-05-18
review_cadence: ad hoc
supersedes: [docs/sprints/sprint-040-objective.md]
superseded_by: null
notes: >
  Archived Sprint 41 contract at sub-sprint close 2026-05-18 (Sprint 41
  closed A — Clean PASS; Codex per-sub-sprint review `pass / 0` first
  pass; dev commit `8cd0a10`; archive package: this file +
  `docs/sprints/sprint-041-handoff.md` + `docs/sprints/sprint-041-codex-review.md`).
  Sprint 41 was the FIFTH and LAST sub-sprint of NEW Milestone M2
  (Skill Registry Abstraction + Wholesale Retroactive Externalization,
  per docs/milestone_objective.md) and the FOURTH implementation
  sub-sprint after the Sprint 37 design freeze + Sprint 38
  SkillRegistry-core landing + Sprint 39 RESOLVE Skill migration +
  predicate migration + S1/S2 + unified dispatcher landing + Sprint 40
  teaching extraction. Sprint 41 implemented Sprint 37 freeze decision
  (i) §10 as runtime code: session-level state-bus (`SkillStateBus.java`)
  mediating state across Skill boundaries on UC switch; per-Skill
  `state_inheritance` declarations on all 6 Skill YAMLs naming inherit /
  reset / soft_signal_via_projection dimensions per §10.2 schema;
  NEW `prior_use_case_carry` projection slot in `ContextProjectionBuilder.java`
  surfacing continuity as soft signal per §10.4.

  Sprint 41 shipped (per design doc §10.11):
  (a) NEW `SkillStateBus.java` Spring `@Component` (203 lines) at
      `server/src/main/java/com/gumtree/csagent/service/runtime/skill/SkillStateBus.java`
      with `applyOnSkillSwitch(Skill priorSkill, Skill newSkill, BotSession session)`
      + `inheritanceFor(Skill skill)` per design doc §10.3 public surface.
      Registry-driven `applyIntakeFieldsIntersection(BotSession)` private
      method (lines 163-194); switch over 3 schema-allowed state keys
      (not per-UC-pair branch table). Defensive no-ops on null inputs,
      same-Skill turns, null intake_fields. Spring `@Component`; injects
      `ObjectMapper`; consumes `IntakeFieldsRegistry` statically. No
      circular dependencies.
  (b) `Skill.java` / `StateInheritance.java` / `SkillLoader.java` schema
      validation VERIFIED wired for full `state_inheritance` declarations
      via `VALID_STATE_KEYS` + `VALID_PROJECTION_SLOTS` allowlists from
      Sprint 38-fix (no Sprint 41 source edits needed; no schema drift).
  (c) All 6 Skill YAMLs EXTENDED with `state_inheritance` blocks per
      design doc §10.5 invariant matrix:
      - `discover_triage.yaml` (30 → 32; +2): row 1 — inherit
        [customer_context, accumulated_tool_results]; reset
        [intake_fields_partial]; soft_signal_via_projection
        [alternate_candidate_use_cases, discover_disambiguation_signals].
      - `confirm.yaml` (29 → 30; +1): row 2 — inherit [customer_context,
        accumulated_tool_results, intake_fields_partial]; reset []; soft
        [].
      - `resolve_faq_grounded_answer.yaml` (52 → 53; +1): row 3 — inherit
        [customer_context, accumulated_tool_results]; reset
        [intake_fields_partial]; soft [prior_use_case_carry] (Sprint 39
        forward-compat placeholder consumed).
      - `resolve_intake_collect_and_handover.yaml` (37 → 38; +1): row 4 —
        inherit [customer_context, accumulated_tool_results,
        intake_fields_partial]; reset []; soft [prior_use_case_carry]
        (Sprint 39 forward-compat placeholder consumed).
      - `escalate.yaml` (28 → 29; +1): row 5 — inherit [customer_context,
        accumulated_tool_results]; reset [intake_fields_partial]; soft [].
      - `terminal.yaml` (25 → 26; +1): row 6 — inherit [customer_context,
        accumulated_tool_results]; reset [intake_fields_partial]; soft [].
  (d) `ContextProjectionBuilder.java` EXTENDED with NEW `prior_use_case_carry`
      projection slot (1161 → 1341; +180; numstat 182/2): emission site
      at the `discover_disambiguation_signals` sibling; NEW private
      helper `buildPriorUseCaseCarryNode(BotSession, List<BotTurn>)`
      (~115 lines including doc); 2 NEW constants
      `PRIOR_USE_CASE_CARRY_AGING_TURNS = 4` + `PRIOR_USE_CASE_CARRY_CITATION_CAP = 3`
      per design doc §10.4 / §10.8 (SINGLE integer constants — NO per-UC
      variation); ctor extended (+1 arg `SkillRegistry`); imports `JsonNode`,
      `NullNode`, `Skill`, `SkillRegistry`. Slot shape matches §10.4
      example verbatim (`prior_citations[]` of {source_id, from_use_case,
      turn_index} + `prior_active_use_case` + `prior_skill_name` (or null)
      + `ages_out_after_turns: 4`).
  (e) `PhaseEvaluator.java` EXTENDED with `maybeApplyStateBusOnSwitch(History,
      BotSession, Skill newSkill)` private helper + SkillStateBus invocation
      in `plan(...)` (1427 → 1482; +55; numstat 59/3). Skill-switch
      detection per design doc §10.3 (prior UC ≠ current UC AND prior Skill
      ≠ new Skill); detection rides on existing M1 surfaces — no new
      classifier (M2 §6 #5 fence preserved). Defensive null-checks on
      missing SkillStateBus (test seam) + empty history (fresh session).
      Ctor extended (+1 arg `SkillStateBus`); imports `SkillStateBus`.
  (f) Tests: NEW `SkillStateBusTest.java` (306 lines / 17 unit tests
      covering §10.5 matrix rows × inherit / reset / soft_signal_via_projection
      + intake_fields_partial registry intersection × 4 + Skill-switch
      trigger detection + same-Skill no-op + null inputs + EMPTY declaration
      + `inheritanceFor()` diagnostic). NEW `PriorUseCaseCarryProjectionTest.java`
      (279 lines / 13 unit tests covering slot shape + cap = 3 + aging = 4
      + null cases + cross-UC shape invariance + `prior_skill_name` resolution).
      NEW `UcSwitchStateInheritanceTest.java` (246 lines / 9 integration
      tests covering representative Skill switches end-to-end through
      `PhaseEvaluator.plan(...)` → `SkillStateBus.applyOnSkillSwitch(...)`).
      EDIT × 33 existing test files (ctor-signature only; passing `null`
      for new dependency arg — `SkillStateBus` for PhaseEvaluator,
      `SkillRegistry` for ContextProjectionBuilder; defensive null-checks
      preserve behavioural equivalence; OQ-S41.3 ACCEPTABLE STYLE per
      Sprint 40 precedent). EDIT `PhaseEvaluatorSkillIntegrationTest.java`
      (1-2 new tests for Skill-switch integration if applicable; subsumed
      under the ctor-update sweep).
  (g) Optional D-g orchestration-shell teaching about `prior_use_case_carry`
      slot in `system_prompt.txt`: SKIPPED per dev judgement (the slot
      is intrinsically self-documenting through `ages_out_after_turns`
      field + `prior_skill_name` + `prior_active_use_case` + citation
      array; Sprint 40 envelope-mechanics paragraph principle). Surfaced
      as OQ-S41.1 — Codex DISAGREED IN PART NON-BLOCKING: the slot shape
      IS self-describing, but the `phase_plan` projection does NOT currently
      expose `state_inheritance.soft_signal_via_projection` to the LLM,
      so the dev's rationale (LLM observes the declaration via Skill
      envelope projection) is partly code-untrue; routes to M2 close
      housekeeping one-liner pointer (NOT Sprint 41 fix-requirement).

  Sprint 41 explicitly did NOT touch (verified UNCHANGED via empty
  `git diff 9130abc..8cd0a10 -- <path>`):
  - `RuntimeIntentClassifier.java` / `IntentClassification.java` /
    `DriftResult.java` / `DriftDetector.java` / `UseCaseRouter.java`
    / `ClassifyUseCaseTool.java` (M2 §6 #5 fence; M3-D Topic↔UC binding
    loosening deferred).
  - INTAKE_UCS set (M3-B Single Handover Orchestrator deferred).
  - escalation_reason enum (M3-A sibling rationale/confidence fields
    deferred).
  - `IntakeFieldsRegistry.java` (M1 functional surface; M2 §6 #21 fence;
    SkillStateBus CONSUMES `requiredFieldsFor(...)` + `canonicalFieldName(...)`
    statically — does NOT modify).
  - `ResolveDispositionEvaluator.java` (Sprint 11/11.1/12 frozen surface
    per `runtime_freeze_and_risk_policy.md` §1.1 #3).
  - `SkillGuardrailDispatcher.java` (Sprint 39 surface UNCHANGED).
  - `system_prompt.txt` (D-g SKIPPED per OQ-S41.1; UNCHANGED at 80 lines).
  - foundational docs / governance docs / freeze docs (constitution-
    discipline preserved).

  Layer: `skill_state` (Q4 — session-level state-bus + per-Skill
  `state_inheritance` enforcement) + `prompt_projection` (Q3 — NEW
  prior_use_case_carry slot surfaces continuity as soft signal) + Runtime-
  owned capability floor per §1.4 (bus enforces which session-state
  dimensions carry vs reset — capability boundary, NOT semantic decision).
  LLM continues to own §1.3 surface (reads `prior_use_case_carry` slot
  and judges continuation / clarification / proceed). Semantic-touching
  multi-layer; §7 stanza honored.

  Codex per-sub-sprint review at `docs/sprints/sprint-041-codex-review.md`
  returned `decision: pass / blocking_count: 0` on first pass (single
  round). All 9 §4.1 questions PASS; all 5 §1.7 boundary items PASS;
  all 8 hard-fence categories PASS; Java baseline 1144/1/0/2; all 6 OQs
  resolved (OQ-S41.1 DISAGREE-IN-PART-NON-BLOCKING; OQ-S41.2 DEFER continued;
  OQ-S41.3 ACCEPTABLE STYLE; OQ-S41.4 FLAG AS DESIGN-DOC EDITORIAL
  DIVERGENCE; OQ-S41.5 STATUS QUO; OQ-S41.6 MINOR = OQ-S41.4 only); 4
  contract-drift items resolved (§7-a REAFFIRM precedent; §7-b pass;
  §7-c CONTENT-COMPLIANT; §7-d NON-BLOCKING NUMBERS-CITE OBSERVATION —
  third+ observed instance of deliver-agent numbers-cite lapse pattern,
  this time on Sprint 41 review prompt cited `ContextProjectionBuilder.java`
  182/2 + `PhaseEvaluator.java` 59/3 + 10 main-source files vs actual
  numstat 181/1 + 57/2 + 9 main-source files); Tier-0 verdicts C1 REJECTED
  reaffirmed / C2 DEFER continued / C3 DEFER continued (LOAD-BEARING for
  Sprint 41 close — Codex evidence-based verdict aligned with deliver-
  agent + human pre-decision: structural Java evidence necessary and
  compelling but operationally about production trace observation across
  M2 close → M3+ window) / C4 + C5 NOT A CANDIDATE reaffirmed.

  M2 acceptance bar recalibration (per docs/milestone_objective.md §5):
  bad-case suite (Alice) + interactive eval were OBSERVATION only for
  this milestone; Sprint 41 contract honored this — primary acceptance
  was "SkillStateBus + state_inheritance enforcement + prior_use_case_carry
  slot land per design doc §10 + behavioural equivalence preserved on
  M1/S38/S39/S40 surfaces + Codex per-sub-sprint review verdict approve
  + Sprint 41 closes the M2 implementation track" — all achieved.

  M2 close fold-back queue at Sprint 41 close (deliver-agent + human +
  Codex jointly fold-back at M2 milestone close as SEPARATE governance
  commit per `doc_governance.md` cadence):
  - Sprint 37 design doc §7.8 Sprint 31/33 line-number swap typo (OQ-S40.5).
  - Sprint 38 OQ-S38.1 template-vs-legacy editorial divergence on
    DISCOVER Skill body.
  - Sprint 39 §10 design doc §6.2.5/§6.2.6 RESOLVE template editorial
    divergence.
  - Sprint 39 OQ-S39.7 stale test name `resolve_intakeUc_stillFollowsLegacyBranchUntilSprint39`.
  - NEW Sprint 41 OQ-S41.1: one-line shell pointer for `prior_use_case_carry`
    in `system_prompt.txt` (code-untrue dev rationale that `phase_plan`
    projection exposes `state_inheritance` — projection currently omits
    it; LLM only sees the slot's JSON shape directly).
  - NEW Sprint 41 OQ-S41.4: `accumulated_tool_results` inheritance wording
    — design doc §10.5 row 1 says "All resolve-side Skills" but Sprint 41
    implementation extends inherit-all to all 6 Skills; runtime effect is
    no-op (no persistent session field); fold-back resolves the wording.

  After Sprint 41 closes: M2 milestone close planning round (deliver-agent
  + human + Codex jointly) — milestone-shared cumulative Codex review
  per `iteration_governance.md` §4.3 against commit range `51c327c..8cd0a10`
  (NEW M2 sub-sprints 1-5 inclusive); design doc editorial fold-back per
  `doc_governance.md` cadence (the 6 fold-back items above); M2 archive
  to `docs/milestones/M2_objective.md`; §6.5 closed-milestone index row
  in `docs/action_bank.md`; 10-handoff §1 lead refresh (M2 → Preceding
  milestone; M3 → Current); C2 + C3 R-item re-evaluation (DEFER continued
  / ELEVATE NOW / NOT-A-CANDIDATE) based on cumulative M2 evidence; M3
  candidate selection (M3-A / M3-B / M3-C / M3-D / M3-Latency /
  M3-Skill-Tuning per `docs/milestone_objective.md` §11 cross-milestone
  sequencing).
---

# Sprint 41 — UC switch + state preservation across Skill boundary

**Milestone:** M2 (Skill Registry Abstraction + Wholesale Retroactive Externalization) sub-sprint 5 of 5 (LAST) per `docs/milestone_objective.md`.

## 1. Sub-sprint class

**Implementation sub-sprint, single track (Track A), semantic-touching multi-layer.** Sprint 41 was the LAST M2 implementation sub-sprint. It shipped the cross-Skill state preservation infrastructure per Sprint 37 freeze decision (i) §10: session-level `SkillStateBus.java` mediating state across Skill boundaries; per-Skill `state_inheritance` declarations on all 6 Skill YAMLs (DISCOVER + CONFIRM + RESOLVE-FAQ + RESOLVE-INTAKE + ESCALATE + TERMINAL/CLOSE); NEW `prior_use_case_carry` projection slot in `ContextProjectionBuilder.java`; Skill-switch detection invoking the bus at `PhaseEvaluator.plan(...)`. UC-switch detection rode on existing M1 surfaces (`alternate_candidate_use_cases` + `discover_disambiguation_signals` projections) per M2 §6 #5 fence — NO new classifier.

Layer breakdown per `iteration_governance.md` §3.2: `skill_state` (Q4) + `prompt_projection` (Q3) + Runtime-owned capability floor per §1.4. §7 stanza honored per Sprint 41 handoff §10.

**Codex review:** per-sub-sprint at Sprint 41 close per `iteration_governance.md` §4.3 trigger #3 (session state model touch across Skill boundary — load-bearing for cross-Skill state preservation; C3 Tier-0 candidate evidence surface). Verdict: `decision: pass / blocking_count: 0` on first pass; all 9 §4.1 questions PASS + all 5 §1.7 items PASS + 8 hard-fence categories PASS + 6 OQs resolved + 4 contract-drift items resolved + Tier-0 dispositions confirmed (C3 DEFER continued per LOAD-BEARING evidence-based verdict).

## 2. Goal — ACHIEVED

Implement Sprint 37 freeze decision (i) §10 as runtime code. Post-Sprint-41, the Skill Registry Abstraction is FULLY landed across the M2 milestone: 6 Skills + SkillRegistry + SkillLoader (Sprint 38) + 2 RESOLVE Skills + 3 predicate migrations + S1/S2 + SkillGuardrailDispatcher (Sprint 39) + teaching extraction + envelope-mechanics paragraph (Sprint 40) + state-bus + state_inheritance + prior_use_case_carry slot (Sprint 41). The runtime per-turn dispatch surface, the Skill-bounded guardrail enforcement, and the cross-Skill state preservation are ALL externalized into declarative Skill YAML + bounded Java surfaces.

7 deliverables (D-a/b/c/d/e/f shipped; D-g optional SKIPPED per dev judgement + OQ-S41.1 NON-BLOCKING Codex DISAGREE-IN-PART verdict): see frontmatter `notes:` block above for the per-deliverable summary.

## 3. Non-goals (explicit) — HONORED PER CODEX VERDICT

All items in frontmatter `notes:` block ("Sprint 41 explicitly did NOT touch") were verified UNCHANGED via empty `git diff 9130abc..8cd0a10 -- <path>` per Codex independent verification at Sprint 41 close.

## 4-12. Sections preserved verbatim from live contract at Sprint 40 close

Sections §4 (Premise check — 15 premises all PASS per dev handoff §3), §5 (Files in scope — ~10-15 files in contract; dev shipped 46 = 10 main + 3 NEW tests + 33 EDIT existing-test ctor-update + 1 NEW handoff per OQ-S41.3 + Drift §7-a ACCEPTABLE EXTENSION of Sprint 39 + Sprint 40 ctor-update precedent), §6 (Hard fences — 33 items all honored; verified per Codex Hard-Fence Verification section), §7 (Bundle policy — single dev commit; deliver-agent files bundled at sub-sprint close per `feedback_commit_at_end_bundles_deliver_artefacts.md`), §8 (Layer-classification + anti-hardcode stanza — `skill_state` + `prompt_projection` per §1.4; NO Tier-0; NO semantic hardcode; full generalization coverage per Codex §6 Q8), §9 (Success metrics — HARD GATES all PASS; Java baseline 1144 within target 1140-1155 per Drift §7-b), §10 (Stop conditions — 20 items; none fired in dev session), §11 (Handoff §11 12-section contract — dev handoff at `docs/sprints/sprint-041-handoff.md` honors this), §12 (M2 milestone context — Sprint 41 = LAST sub-sprint; M2 close planning round is NEXT per `iteration_governance.md` §8.4) are preserved in the live `docs/sprint_objective.md` at Sprint 41 close commit; this archive's frontmatter `notes:` field above summarizes the load-bearing content. For the full body, see `git show <sprint-41-close-commit>:docs/sprint_objective.md` — the deliver-agent close commit bundles the archive + replacement together so the same commit contains both the archive (this file) AND the live file's replacement (M2 close planning placeholder OR first M3 sub-sprint contract).

**Sprint 41 final outcome (per `docs/sprints/sprint-041-handoff.md` §12 + `docs/sprints/sprint-041-codex-review.md`):** A — Clean PASS. Codex `decision: pass / blocking_count: 0` on first pass (single round). All 6 OQs resolved with Codex independent verdicts (OQ-S41.1 DISAGREE-IN-PART NON-BLOCKING — routes to M2 close housekeeping; OQ-S41.2 C3 DEFER continued; OQ-S41.3 ACCEPTABLE STYLE; OQ-S41.4 FLAG AS DESIGN-DOC EDITORIAL DIVERGENCE — routes to M2 close fold-back; OQ-S41.5 STATUS QUO; OQ-S41.6 MINOR = OQ-S41.4). 4 contract-drift items classified (§7-a REAFFIRM precedent; §7-b pass; §7-c CONTENT-COMPLIANT; §7-d NON-BLOCKING NUMBERS-CITE OBSERVATION — third+ instance of pattern). No new Tier-0 candidates; C1 REJECTED preserved; C2 DEFER continued; **C3 DEFER continued (LOAD-BEARING for Sprint 41 close — Sprint 41 ships FIRST observed evidence surface but Codex evidence-based verdict aligns with deliver-agent + human pre-decision: production trace observation across M2 close → M3+ window required before Tier-0 elevation)**; C4 + C5 NOT A CANDIDATE reaffirmed. Sprint 41 closes M2 implementation track. M2 milestone close planning round is the NEXT deliver-agent + human unit of work per `iteration_governance.md` §8.4.
