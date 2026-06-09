---
title: Sprint 37 handoff — Skill Registry + state-across-Skill design freeze (NEW M2 sub-sprint 1)
doc_tier: sprint-archive
status: archived
implementation_status: implemented
source_of_truth: this file
last_reviewed: 2026-05-17
review_cadence: ad hoc
supersedes: []
superseded_by: null
notes: >
  Sprint 37 is the FIRST sub-sprint of NEW Milestone M2 (Skill
  Registry Abstraction + Wholesale Retroactive Externalization) per
  `docs/milestone_objective.md`. Layer: docs/design only (NO
  `server/` code) per `docs/sprint_objective.md` §1. Sprint 37 ships
  one architectural decision doc + this handoff; no Tier-0 candidate
  write-up file (all five surfaced candidates recommend DEFER per
  the freeze §12). §7 stanza REQUIRED — single-track multi-layer
  PROSPECTIVE per `feedback_multi_layer_prospective_stanza.md`.
  Codex per-sub-sprint review fires at Sprint 37 close per
  `iteration_governance.md` §4.3 trigger #1 + #2. Closure verdict
  (§12) left for deliver-agent + human + Codex per
  `feedback_handoff_verdict_section_delegation.md`.
---

# Sprint 37 handoff — Skill Registry + state-across-Skill design freeze

## 1. Context Pack

### 1.1 Relevant docs (read at session start)

| path | tier (best guess) | status | one-line relevance |
|------|-------------------|--------|--------------------|
| `docs/sprint_objective.md` | current-runtime | current | Sprint 37 binding scope: §1 sub-sprint class (design-freeze, multi-layer prospective); §2 goal (10 decisions a-j); §3 non-goals; §4 premise check (10 items); §6 hard fences (19 items); §10 stop conditions; §11 12-section handoff contract. |
| `docs/milestone_objective.md` | current-runtime | current | NEW M2 milestone north star: §1 5-sub-sprint layer breakdown; §2 goal (Skill Registry abstraction + retroactive externalization); §5 acceptance recalibration (Alice + eval = observation, NOT gate for this milestone); §6 hard fences (#4 verbatim S1 authorization carried forward). |
| `docs/proposals/skill_orchestration_candidates.md` | proposal | superseded (chained: → `skill_foundation_design.md` → `skill_registry_design.md`) | Sprint 5 (F2) original proposal: 5 candidate skills (S1-S5); explicit "Do NOT introduce a new skill runtime framework" constraint that NEW M2 reinterprets. Upstream reasoning archive. |
| `docs/proposals/skill_foundation_design.md` | proposal | superseded 2026-05-17 (frontmatter already updated by deliver-agent in commit fd7396a) | OLD M2-Skill Sprint 36 design freeze: minimum-surface incrementalism (reuse PhasePlan + add teaching paragraph + add adjacent predicate). Historical reference; §6 #4 verbatim S1 authorization carries forward into NEW M2; §1.2 MATERIAL FINDING on Sprint 6/7/11 predicates already shipped informs Sprint 37 decision (g). |
| `docs/sprints/sprint-036-handoff.md` | sprint-archive | archived | OLD Sprint 36 dev archive: §1.2 cites verified line numbers for PhaseEvaluator / AgentRunLoopImpl predicates at HEAD eb65e2b — preserved verbatim at HEAD 6d97888 (no drift since Sprint 36 close); informs Sprint 37 decision (g) predicate migration mapping. |
| `docs/runtime_freeze_and_risk_policy.md` §1.1 / §2 | foundational | current | Tier-0 invariant catalogue. §1.1 five frozen surfaces (pre-plan reroute, same-UC progressive resolve, ResolveDisposition guard, record_outcome guard, observability/validation); §2 seven-rule frozen contract. Read for §11 (decision (j)) §4.1 walk Q2 + §12 Tier-0 candidate evaluation. NOT EDITED by Sprint 37. |
| `docs/current/iteration_governance.md` | durable-connective | current | §1 Constitution; §3 Fix Layer Checklist; §4.1 nine-question anti-hardcode kernel walked in design doc §11; §4.3 per-sub-sprint Codex triggers; §5 / §5.5 / §5.6 Eval Acceptance (Sprint 37 ships zero behaviour change); §7 stanza shape; §8 milestone framework. |
| `docs/current/faq_grounding_contract.md` | current-runtime | current | Grounding-floor contract; `L1:source_citation_present` is the boundary the NEW S1 `must_cite_source` guardrail extends per §6 #4 verbatim authorization. |
| `.claude/agent-memory/sprint-deliver-orchestrator/feedback_multi_layer_prospective_stanza.md` | feedback memory | current | Multi-layer prospective §7 stanza convention for design-freeze sub-sprint with multi-sub-sprint downstream coverage. |
| `.claude/agent-memory/sprint-deliver-orchestrator/feedback_constitution_discipline_vs_planning_anticipation.md` | feedback memory | current | Why Sprint 37 dev does NOT edit governance docs even if §11 walk suggests Tier-0 elevation; candidates surfaced in §12 of design doc + §7 OQ of this handoff, with DEFER recommendation. |
| `.claude/agent-memory/sprint-deliver-orchestrator/feedback_commit_at_end_bundles_deliver_artefacts.md` | feedback memory | current | Bundle policy — dev does NOT stage deliver-agent-owned files (`docs/sprint_objective.md`, `docs/milestone_objective.md`, `docs/10-handoff.md`, `docs/action_bank.md`, `docs/codex-findings.md`, `compact/sprint-037-*.md`). |
| `.claude/agent-memory/sprint-deliver-orchestrator/feedback_deliver_agent_cited_numbers_must_be_reproducible.md` | feedback memory | current | Every code citation in design doc + this handoff names file path + line number + (for greps) the exact command used to extract. |
| `.claude/agent-memory/sprint-deliver-orchestrator/feedback_handoff_verdict_section_delegation.md` (inferred from OLD Sprint 36) | feedback memory | current | §12 closure-verdict placeholder convention. |

### 1.2 Code shape verified at session start (HEAD 6d97888, 2026-05-17)

Read-only for Sprint 37. The design freeze characterizes these
surfaces; Sprints 38/39/40/41 implement.

| path | total lines | what it governs | landmarks cited in design doc |
|------|------------:|-----------------|-------------------------------|
| `server/src/main/java/com/gumtree/csagent/service/runtime/PhaseEvaluator.java` | 1628 | Per-phase `PhasePlan` construction; phase dispatch | Line 30: `INTAKE_UCS` set; lines 33-: escalation_reason enum docs; lines 112-: `UC_TEAM_NAME` map; lines 207-208: `buildIntakeSystemInstruction` helper; line 355-360: phase dispatch switch; lines 397-457: DISCOVER branch (Sprint 7 §I0 weak-candidate cue at 418-431; Sprint 33 ad-status disambiguation cue at 432-448); lines 462-487: CONFIRM; lines 492-509: CLOSE; lines 513-542: ESCALATE; lines 552-596: RESOLVE-INTAKE; lines 598-665: RESOLVE-FAQ (Sprint 6 §G2 S1 sequence teaching at 633-665). |
| `server/src/main/java/com/gumtree/csagent/model/PhasePlan.java` | 146 | Existing `PhasePlan` record (11 fields) | Backward-compatible per decision (c) §4.2. |
| `server/src/main/java/com/gumtree/csagent/service/runtime/AgentRunLoopImpl.java` | 787 | Tool dispatch loop + Sprint 6/7/11 predicates | Lines 106-108: `FAQ_PATH_UCS`; line 120: `INTAKE_COMPLETE_GUARD_REJECT_REASON`; line 317: intake-complete guard dispatch (Sprint 7 §I2); line 356: shouldRejectPrematureResolveOutcome dispatch (Sprint 11 §M1); line 413: shouldRejectFaqMissHandover dispatch (Sprint 6 §G2); lines 628-651: `shouldRejectIncompleteIntakeHandover` method (Sprint 7); lines 690-734: `shouldRejectFaqMissHandover` method (Sprint 6); lines 745-759: `shouldRejectPrematureResolveOutcome` method (Sprint 11). |
| `server/src/main/java/com/gumtree/csagent/service/runtime/ContextProjectionBuilder.java` | 1161 | Per-turn projection | Lines 346-377: `intake_state` slot (Sprint 7 §I2); lines 396-416: `alternate_candidate_use_cases` slot (Sprint 31); lines 418-432: `discover_disambiguation_signals` slot (Sprint 33); lines 704+: `already_called` slot (Sprint 20 Track B). |
| `server/src/main/resources/prompts/system_prompt.txt` | **101 (PREMISE REFINEMENT — see §3)** | LLM system prompt | Lines 1-22: core rules (identity, JSON contract, universal rules); lines 23-28: Sprint 23 `already_called` teaching; lines 30-34: Sprint 31 `alternate_candidate_use_cases` teaching; lines 36-43: Sprint 33 `discover_disambiguation_signals` teaching; **lines 45-52: DISCOVER phase guidance (additional content beyond §4 premise #7); lines 54-101: `request_handover` escalation_reason decision tree (additional cross-Skill content; largest single block)**. |
| `server/src/main/java/com/gumtree/csagent/service/tools/RecordOutcomeTool.java` | 132 | record_outcome dispatch | Lines 36-94: `execute`; lines 110-121: `normalizeOutcomeClass` (canonical `resolve / escalate / abandon` enum). Sprint 39 NEW S1 predicate fires at this dispatch site. |
| `server/src/main/java/com/gumtree/csagent/service/runtime/EscalationReasonResolver.java` | 384 | escalation_reason precedence + canonicalization | Lines 84-103: PRIORITY map (`user_requested=1`, `user_distress=2`, `intake_complete_for_uc_g/h/i/j/k=20-24`, `incomplete_intake=25`); lines 232-247: resolve precedence; lines 256-285: canonicalize. S2 `intake_complete_required` guardrail dispatcher reads this for downgrade alternative (deferred via OQ-13.3.6). |
| `server/src/main/java/com/gumtree/csagent/service/runtime/IntakeFieldsRegistry.java` | 222 | Per-UC required intake fields | Lines 53-: `REQUIRED_FIELDS_BY_UC` map; lines 110-112: `isIntakeUseCase`; lines 118-121: `requiredFieldsFor`; lines 127-131: `canonicalFieldName`; lines 167-178: `fieldsRemaining`; lines 184-193: `intakeComplete`. **Source of truth for per-UC intake fields** (NOT `UseCaseDefinition.requiredIntakeFields` per OLD Sprint 36 §8.1 premise-name correction). |
| `server/src/main/java/com/gumtree/csagent/service/runtime/UseCaseRegistryService.java` | 174 | UC registry | Lines 166-173: `UseCaseDefinition` record (6 fields: `ucId / name / topicSubjects / riskLevel / allowBotResolution / path`). NO `requiredIntakeFields` field. |
| `server/src/main/resources/skills/` | (does not exist) | NEW directory at Sprint 38 first migration | Per Sprint 37 §4 premise #9; verified absent at session start. |

### 1.3 Doc-status warnings observed

- **PREMISE REFINEMENT for `system_prompt.txt`:** the file is 101
  lines at HEAD (premise §4.7 cited "~40-60 lines"). Sprint 23 /
  Sprint 31 / Sprint 33 paragraphs are present at the cited
  approximate locations (off by 0-3 lines); additional content
  exists at lines 45-52 (DISCOVER phase guidance) and lines 54-101
  (`request_handover` decision tree). The refinement does NOT
  change Sprint 37 decision (f)'s scope (Sprint 23/31/33 paragraphs
  only); but it IS load-bearing for Sprint 40 dev judgement on the
  post-migration shell shape. Surfaced in design doc §13.2
  Refinement 1 + §7.3 + this handoff §3.
- **Sprint 33 ad-status disambiguation cue split** (cue lives in
  PhaseEvaluator's DISCOVER systemInstruction at lines 432-448;
  slot description lives in `system_prompt.txt:36-43`). The split
  is preserved in M2 — Sprint 38 migrates the cue (as part of
  decision (e) DISCOVER); Sprint 40 migrates the slot description
  (as part of decision (f)). Both end up in `discover_triage.yaml`
  `procedure`. Surfaced in design doc §13.2 Refinement 2.
- **Working tree at session start was clean** (`git status` empty;
  HEAD `6d97888`). The deliver-agent's M2 supersession bundle
  (commit `fd7396a`) is already committed, including: NEW M2
  `milestone_objective.md`; NEW Sprint 37 `sprint_objective.md`;
  OLD `skill_foundation_design.md` frontmatter flipped to
  `status: superseded`; OLD `skill_orchestration_candidates.md`
  chained `superseded_by` pointer. No dev action required on
  these files (per Sprint 37 §6 #9 / #10 fences).

### 1.4 Source-of-truth decision

For the premise-check step (§3 below), source of truth is the
verbatim Java / prompt source at HEAD 6d97888. Per
`doc_governance.md` "code ahead of docs" rule, code is the source
of truth for delivered behaviour; the §4 premise #7 line-count
estimate drifted between draft and HEAD (101 vs ~40-60); the
design freeze + this handoff cite the verified count.

For the design freeze step (§4 below), source of truth is the
design doc itself
(`docs/proposals/skill_registry_design.md` —
`source_of_truth: this file` per its frontmatter). Sprints 38 /
39 / 40 / 41 read the design doc verbatim.

### 1.5 Implementation status

`implementation_status: implemented` — Sprint 37 ships exactly two
authored artefacts in this dev commit:

1. `docs/proposals/skill_registry_design.md` (NEW — the
   architectural decision doc; 13 sections; 10 decisions (a)-(j)
   + §4.1 walk + Tier-0 enumeration + downstream reference
   index; 3633 lines).
2. `docs/sprints/sprint-037-handoff.md` (this file; 12 sections
   per Sprint 37 contract §11).

NO Tier-0 candidate write-up file shipped — all five candidates
in design doc §12 recommend DEFER per
`feedback_constitution_discipline_vs_planning_anticipation.md`;
deliver-agent + human + Codex evaluate at Sprint 37 close.

No deferred work; no partial shipment. Sprints 38 / 39 / 40 / 41
implementation rounds happen separately; the freeze is complete.

### 1.6 Risks before the design freeze (assessed at session start)

- **Premise refinement risk (mitigated):** `system_prompt.txt`
  line count differs from premise §4.7 estimate. Mitigated by
  surfacing in §3 + design doc §13.2 Refinement 1; decision (f)
  scope unchanged (Sprint 23/31/33 paragraphs); Sprint 40 dev
  awareness preserved.
- **Existing-surface alignment risk (mitigated):** the OLD Sprint
  36 §1.2 MATERIAL FINDING (Sprint 6/7/11 predicates already
  shipped) carries forward as load-bearing for decision (g).
  Mitigated by design doc §8.2 explicit migration mapping per
  predicate with cited HEAD line numbers.
- **Tier-0 candidate surface risk (active):** five candidates
  surfaced in design doc §12. Default DEFER per
  `feedback_constitution_discipline_vs_planning_anticipation.md`.
  Deliver-agent + human + Codex per-sub-sprint review at Sprint
  37 close evaluate elevation choice BEFORE Sprint 38 begins.
- **§1.7 boundary risk (assessed clean):** the §4.1 walk in design
  doc §11 finds NO §1.7 violation in the PROPOSED design. Each
  Skill `procedure` is principle-level; `guardrails` are
  registry-driven / single-condition; `state_inheritance` uses
  registry-level intersection (no per-UC-pair branch).
- **§1.4 boundary close-call (assessed):** decision (g) §8 S1
  `must_cite_source` introduces a NEW Runtime-floor guard; scope
  bounded per §6 #4 verbatim authorization; not a generic
  citation gate. Codex Sprint 39 review verifies scope match.
- **Java baseline risk (no-op for Sprint 37):** Sprint 37 ships
  zero Java change. The 983/1-inherited/0/2 baseline post-M1
  carries forward unchanged; Sprint 37 §9 of dev prompt
  explicitly says NO test run required.

## 2. Sub-sprint-objective recap

Sprint 37 is the FIRST sub-sprint of NEW Milestone M2 (Skill
Registry Abstraction + Wholesale Retroactive Externalization) per
`docs/milestone_objective.md`. Layer: docs/design only (NO
`server/` code) per `docs/sprint_objective.md` §1; multi-layer
PROSPECTIVE per `feedback_multi_layer_prospective_stanza.md`
covering Sprints 38 / 39 / 40 / 41 implementation layers.

Sprint 37 produces `docs/proposals/skill_registry_design.md` — an
architectural decision doc that locks 10 design decisions (a)-(j)
covering Skill data model, SkillRegistry shape, PhaseEvaluator
integration, procedure-vs-guardrails responsibility split,
retroactive migration mappings (6 phase YAMLs + Sprint 23/31/33
teaching paragraphs + Sprint 6/7/11 predicates), unified Skill
terminal-predicate dispatcher design, session-level state model
with per-Skill `state_inheritance` semantics, and §4.1 nine-question
anti-hardcode kernel walk on the proposed design.

The freeze enables Sprints 38 / 39 / 40 / 41 contracts to be
drafted directly from the doc without further architectural
rounds:

- Sprint 38 = SkillRegistry core + 4 simpler phase migrations
  (DISCOVER + CONFIRM + ESCALATE + TERMINAL/CLOSE).
- Sprint 39 = RESOLVE_FAQ + RESOLVE_INTAKE migrations + Sprint
  6/7/11 predicate migration + NEW S1 `must_cite_source` + S2
  `intake_complete_required` relocation + unified dispatcher.
- Sprint 40 = Sprint 23/31/33 teaching extraction from
  `system_prompt.txt` + orchestration shell cleanup.
- Sprint 41 = UC switch + state preservation across Skill
  boundary (SkillStateBus + `state_inheritance` declarations on
  all 6 Skills + NEW `prior_use_case_carry` projection slot).

§7 stanza REQUIRED — single-track multi-layer PROSPECTIVE per
`feedback_multi_layer_prospective_stanza.md`. Codex review:
per-sub-sprint at Sprint 37 close per `iteration_governance.md`
§4.3 trigger #1 (possible Tier-0 candidate from Skill predicate /
state-bus enforcement semantics) + #2 (§1.7 boundary discussion on
Skill abstraction shape).

Sprint 37 ships ZERO Java code change, ZERO Python code change,
ZERO prompt change, ZERO CaseSpec change, ZERO test run, ZERO
smoke run, ZERO bad-case suite run, ZERO new Tier-0 invariant in
`runtime_freeze_and_risk_policy.md` (five Tier-0 candidates are
ENUMERATED in design doc §12; the actual elevation/decline
decision is deliver-agent + human + Codex at Sprint 37 close, NOT
Sprint 37 dev commit).

## 3. Premise re-verification (each of §4's 10 points)

All 10 premise items in `docs/sprint_objective.md` §4 re-verified
at session start at HEAD `6d97888`.

| # | premise | verification |
|---|---------|--------------|
| 1 | **M2 milestone objective approved.** `docs/milestone_objective.md` carries the NEW M2 contract (status `current`; supersedes OLD M2-Skill); approved by human 2026-05-17. | Read `docs/milestone_objective.md` end-to-end (399 lines). Title "Milestone M2 — Skill Registry Abstraction + Wholesale Retroactive Externalization (LLM-led, Policy-bounded)"; status `current`; supersedes the OLD M2-Skill archive + M1_objective.md per frontmatter. §5 acceptance recalibration (Alice + eval = observation, NOT gate for THIS milestone) is explicit. §6 hard fences #4 carries forward the verbatim S1 authorization. **PASS.** |
| 2 | **Sprint 37 contract approved.** `docs/sprint_objective.md` (this file) is the active sub-sprint contract. | Read `docs/sprint_objective.md` end-to-end. Title "Sprint 37 objective — Skill Registry + state-across-Skill design freeze (M2 sub-sprint 1)"; status `current`; supersedes `docs/sprints/sprint-036-objective.md`. §2 enumerates 10 design decisions (a)-(j). **PASS.** |
| 3 | **Sprint 5 (F2) original proposal** at `docs/proposals/skill_orchestration_candidates.md` — `status: superseded` (chained pointer through `skill_foundation_design.md`); 5 skill candidates S1-S5. | Read (499 lines). 5 candidates per §3 (S1 / S2 / S3 / S4 / S5); explicit recommendation §6 "Do NOT introduce a new skill runtime framework. Implement S1 as a parametrized PhasePlan inside PhaseEvaluator.plan(...)." Frontmatter `status: superseded` + `superseded_by: docs/proposals/skill_foundation_design.md`. NEW Sprint 37 freeze chained-supersedes per its frontmatter. **PASS.** |
| 4 | **OLD Sprint 36 design freeze** at `docs/proposals/skill_foundation_design.md` — supersede pattern already applied by deliver-agent (commit `fd7396a`); `status: superseded`; `superseded_by: docs/proposals/skill_registry_design.md`. | Read frontmatter + §"Why superseded (2026-05-17)" section. Frontmatter at lines 1-32 carries `status: superseded`, `superseded_by: docs/proposals/skill_registry_design.md`. Body §"Why superseded" explains the human's rejection of minimum-surface framing. **PASS** (deliver-agent owns this edit per Sprint 37 §6 #9 fence; dev confirmed pattern applied + does NOT re-edit). |
| 5 | **PhaseEvaluator.java at HEAD** carries hardcoded `systemInstruction` / `groundingInstruction` / `escalationPolicy` per phase. | Verified at HEAD 6d97888. File 1628 lines. INTAKE_UCS at line 30; phase dispatch switch at lines 355-360; per-phase branches at lines 397-457 (DISCOVER), 462-487 (CONFIRM), 492-509 (CLOSE), 513-542 (ESCALATE), 552-596 (RESOLVE-INTAKE), 598-665 (RESOLVE-FAQ). Premise approximate locations PASS at exact line numbers. **PASS.** |
| 6 | **AgentRunLoopImpl.java at HEAD** carries Sprint 6/7/11 shouldReject* family. | Verified at HEAD 6d97888. File 787 lines. FAQ_PATH_UCS at lines 106-108; INTAKE_COMPLETE_GUARD_REJECT_REASON at line 120; shouldRejectIncompleteIntakeHandover at lines 628-651 (Sprint 7 §I2; dispatched line 317); shouldRejectFaqMissHandover at lines 690-734 (Sprint 6 §G2; dispatched line 413); shouldRejectPrematureResolveOutcome at lines 745-759 (Sprint 11 §M1; dispatched line 356). Premise exact-line-numbers PASS. **PASS.** |
| 7 | **system_prompt.txt at HEAD** carries Sprint 23/31/33 teaching paragraphs. | Verified at HEAD 6d97888. **Premise refinement** (does not change Sprint 37 scope): file is 101 lines total (premise §4.7 cited "~40-60 lines"). Sprint 23 `already_called` at lines 23-28 (verified); Sprint 31 `alternate_candidate_use_cases` at lines 30-34 (premise said 30-33; off by 1); Sprint 33 `discover_disambiguation_signals` at lines 36-43 (premise said 36-40; off by 3). Additional content: lines 45-52 (DISCOVER phase guidance) and lines 54-101 (`request_handover` decision tree) — both cross-Skill universal content. Surfaced in design doc §13.2 Refinement 1 + §7.3 for Sprint 40 awareness. **PASS (with refinement).** |
| 8 | **ContextProjectionBuilder.java at HEAD** carries M1 Sprint 32 + Sprint 33 projection slots. | Verified at HEAD 6d97888. File 1161 lines. intake_state at lines 346-377 (Sprint 7); alternate_candidate_use_cases at lines 396-416 (Sprint 31); discover_disambiguation_signals at lines 418-432 (Sprint 33); already_called at lines 704+ (Sprint 20 Track B). **PASS.** |
| 9 | **server/src/main/resources/skills/** does NOT exist at HEAD. | Verified `ls` returned "directory does NOT exist". Sprint 38 creates on first Skill YAML migration. **PASS.** |
| 10 | **Java baseline** 983 / 1-inherited / 0 / 2 post-Sprint-36-close; Sprint 37 is docs-only so baseline UNCHANGED. | NOT re-run (Sprint 37 §9 of dev prompt: "Sprint 37 is docs-only: NO Java test run"). Working tree at session start CLEAN per `git status`; no orphaned working-tree changes from prior deliver-agent session. **PASS (no rerun needed; baseline holds by construction).** |

**Premise drift summary:** 1 premise refinement (#7 —
`system_prompt.txt` line-count estimate drifted by ~50 lines;
intent preserved — Sprint 23/31/33 paragraphs are present at the
cited approximate locations). 9 premises PASS as written. **No
STOP condition fired** — the #7 refinement does NOT change the
freeze's design intent (decision (f) §7 still maps Sprint 23/31/33
paragraphs as named; additional shell content is identified for
Sprint 40 dev awareness, not Sprint 37 scope).

## 4. Design doc walkthrough

`docs/proposals/skill_registry_design.md` lands with 13 sections;
3633 lines total. Section structure:

| § | header | length (lines) | key content |
|---|--------|---------------:|-------------|
| §1 | Background + relation to upstream | ~430 | §1.1 what this freeze decides (10 decisions enumerated); §1.2 relation to OLD Sprint 36 freeze (carry-forward + reject lists); §1.3 relation to Sprint 5 (F2) original proposal; §1.4 relation to M2 milestone + §5 acceptance recalibration; §1.5 research-agent proposal (three-tier abstraction + four orchestration patterns + Skill-as-Prompt rationale + key insight quote); §1.6 architectural diagram (ascii); §1.7 what this freeze does NOT decide. |
| §2 | Decision (a) Skill data model | ~190 | Skill data model field set (12 fields per §2.1 table); JSON Schema validation contract (§2.2); rationale (§2.3); alternatives considered (§2.4 5 alternatives rejected); §1.7 + §1.3/§1.4 boundary checks; downstream sub-sprint reference. |
| §3 | Decision (b) SkillRegistry shape | ~190 | SkillRegistry + SkillLoader Java classes; `select(phase, useCase) → Skill` semantics (§3.2 exact-match → wildcard → null fallback); SkillLoader fail-fast at Spring boot (§3.3); rationale + alternatives + boundary checks. |
| §4 | Decision (c) PhaseEvaluator integration | ~210 | `composeSkillPhasePlan` private helper (§4.1); backward-compatible PhasePlan data shape (§4.2); mixed state during Sprint 38 → Sprint 39 window (§4.3); rationale + alternatives + boundary checks. |
| §5 | Decision (d) procedure vs guardrails split | ~200 | Field-level separation of LLM-soft (`procedure` / `grounding_instruction` / `escalation_policy`) vs Runtime-hard (`tools_required` / `guardrails`); guardrail declaration schema (§5.2 typed objects); content placement rules table (§5.3); rationale + 5 alternatives rejected including `observe_only` exclusion. |
| §6 | Decision (e) phase YAML migration mapping (6 phases) | ~710 | Per-phase mapping table (§6.1); per-phase field mapping with cited HEAD line numbers (§6.2.1-§6.2.6); CLOSE → TERMINAL consolidation note + OQ; content overlap analysis (§6.3); behavioural-equivalence test pattern (§6.4); rationale + alternatives + boundary checks. **The largest section; carries the load-bearing 6 Skill YAML drafts.** |
| §7 | Decision (f) Sprint 23/31/33 teaching paragraph migration | ~200 | Sprint 31 + Sprint 33 → discover_triage `procedure`; Sprint 23 → orchestration shell (cross-Skill teaching); post-Sprint-40 orchestration shell shape (§7.3); rationale + alternatives + boundary checks. |
| §8 | Decision (g) Sprint 6/7/11 predicate migration | ~310 | Per-predicate migration to Skill guardrails (§8.2.1-§8.2.5); S1 `must_cite_source` verbatim §6 #4 authorization quote (§8.2.4); S2 `intake_complete_required` already-shipped framing (§8.2.5); rationale + alternatives + boundary checks. |
| §9 | Decision (h) unified Skill terminal-predicate dispatcher | ~270 | `SkillGuardrailDispatcher.java` public surface (§9.1); short-circuit composition order (§9.2); per-tool-call dispatch site routing (§9.3); failure-mode trace + LLM-visible hint shape (§9.4); rationale + alternatives + boundary checks; Tier-0 candidate question (§9.9 non-overridability). |
| §10 | Decision (i) session-level state model + state_inheritance | ~270 | `SkillStateBus.java` + per-Skill `state_inheritance` schema; UC switch invariant matrix carried forward from OLD Sprint 36 D5 §6.3 (§10.5); NEW `prior_use_case_carry` projection slot (§10.4); rationale + alternatives + boundary checks; Tier-0 candidate question (§10.10 state-bus boundary enforcement). |
| §11 | Decision (j) §4.1 nine-question anti-hardcode kernel walk | ~280 | Per-Q (Q1-Q9) verdict + reasoning on the PROPOSED design; walk verdict `approve`. |
| §12 | Tier-0 candidate enumeration | ~220 | 5 candidates enumerated (C1-C5): C1 tool-whitelist (REJECTED — existing invariant); C2 guardrail non-overridability (QUALIFIED → DEFER); C3 state-bus boundary (QUALIFIED → DEFER); C4 S1 `must_cite_source` (NOT A CANDIDATE per §6 #4 bounded inversion); C5 S2 `intake_complete_required` (NOT A CANDIDATE per Sprint 7 §I2 precedent). NO separate Tier-0 candidate write-up file filed in dev commit because no candidate is QUALIFIED-AND-ELEVATE. |
| §13 | Downstream sub-sprint reference index + premise refinements | ~150 | Decision-to-sub-sprint reference table (§13.1); premise refinements (§13.2 — system_prompt.txt 101-line refinement + Sprint 33 cue/slot split); 11 open questions (§13.3) for deliver-agent + human; self-walk on Sprint 37 itself (§13.4). |

No departures from Sprint 37 contract §2 framing on the 10
decisions. The §11 walk on the PROPOSED design is the substantive
anti-hardcode verification; the §13.4 self-walk is on the
Sprint 37 commit diff (this design doc + handoff), per Sprint 37
§11 contract.

## 5. §4.1 anti-hardcode walk-through summary

Per design doc §11 (decision (j)), each of the nine §4.1 questions
was walked against the PROPOSED design. Summary verdict per Q:

| Q | Question (abbreviated) | Verdict on PROPOSED design | Cited design doc § |
|---|------------------------|---------------------------|-------------------|
| Q1 | Keyword / regex / if-else / enum / per-UC matrix for semantic decision? | **NO** | §11.1 walks each design surface (a)-(i) |
| Q2 | If yes, Tier-0 justified? | N/A (Q1=NO) — Tier-0 candidates enumerated in §12 | §11.2 |
| Q3 | Soft signal alternative? | PARTIALLY YES (state-bus soft-signal dimensions); PARTIALLY NO (terminal predicates are §1.4 hard surfaces per §6 #4 authorization + Sprint 6/7/11 precedents) | §11.3 |
| Q4 | Visible-eval / trace phrasing / CaseSpec id encoded? | **NO** | §11.4 |
| Q5 | Moves semantic ownership from LLM to Java? | **NO** — `procedure` LLM-soft; `guardrails` enforce Runtime-floor only; state-bus enforces capability-floor only | §11.5 |
| Q6 | If-else in prompt instead of principle-level guidance? | **NO** — every Skill `procedure` is principle-level; orchestration shell carries cross-Skill universal norms | §11.6 |
| Q7 | Tool schema / capability / PII / grounding floor preserved? | **YES** with one principled extension — grounding floor narrowly extended per §6 #4 verbatim authorization for S1 `must_cite_source`; the generic-Java-citation-gate `D-hard-citation-gate` deferral remains preserved | §11.7 |
| Q8 | Generalization eval coverage? | Sprint 37 itself zero behaviour change; PROSPECTIVE coverage specified for Sprints 38-41 per §8 stanza | §11.8 |
| Q9 | Temporary measure with sunset? | N/A — Skill abstraction is permanent | §11.9 |

**Walk verdict (design doc §11.10):** `approve` — the proposed
design honors Constitution §1.3 / §1.4 / §1.7. No semantic hardcode
introduced. Five Tier-0 candidates surfaced in §12 for deliver-
agent + human + Codex evaluation at Sprint 37 close (default
recommendation DEFER per
`feedback_constitution_discipline_vs_planning_anticipation.md`).

## 6. Tier-0 candidate enumeration outcome

Per design doc §12, five Tier-0 candidates enumerated. Summary:

| Candidate | Statement | Verdict | Recommendation | Write-up file? |
|---|---|---|---|---|
| C1 | Skill tool-whitelist enforcement unconditional | REJECTED as new candidate (existing invariant via `PhasePlan.allowedTools` + `ToolDispatcher.validateAgainstPlan`) | No action; preserved by construction | No |
| C2 | Guardrail refusal non-overridability by LLM | QUALIFIED | **DEFER** per `feedback_constitution_discipline_vs_planning_anticipation.md`; carry as `R-skill-guardrail-non-overridability-tier-0` in `action_bank.md` for M3+ revisit | No (DEFER recommendation; no qualified-and-elevate) |
| C3 | State-bus boundary enforcement for `state_inheritance` | QUALIFIED | **DEFER**; carry as `R-skill-state-bus-boundary-enforcement-tier-0` | No |
| C4 | S1 `must_cite_source` predicate semantics | NOT A CANDIDATE per M2 §6 #4 verbatim bounded inversion (NEW M2 §6 #4 explicitly bounds S1 to Runtime-floor narrow guard, NOT Tier-0 structural invariant; `D-hard-citation-gate` deferral preserved) | No action; OLD Sprint 36 §8.3 DEFER preserved | No |
| C5 | S2 `intake_complete_required` predicate semantics | NOT A CANDIDATE per Sprint 7 §I2 precedent (predicate already in production since Sprint 7; M2 only relocates) | No action; OLD Sprint 36 §8.4 DEFER preserved | No |

**No separate Tier-0 candidate write-up file shipped in Sprint
37 dev commit** because no candidate's recommendation is
QUALIFIED-AND-ELEVATE. Sprint 37 contract §10 stop condition #2
requires "STOP; file candidate write-up file" on candidate
surfacing; the dev interpretation is that "file a write-up file"
applies to QUALIFIED-AND-ELEVATE candidates (where the
recommendation is to add the Tier-0 row to
`runtime_freeze_and_risk_policy.md` at Sprint 37 close). For
DEFER candidates, the §12 enumeration in the design doc + this
§6 + §7 OQ surface the candidates for human evaluation
adequately. If deliver-agent + human prefer a separate write-up
file for QUALIFIED-DEFER candidates (C2 + C3), surface in OQ
§7.4 below for re-decision.

If deliver-agent + human + Codex evaluate at Sprint 37 close
that C2 OR C3 should be ELEVATED, the deliver-agent commits the
`runtime_freeze_and_risk_policy.md` §1 / §2 addition as a
SEPARATE commit per
`feedback_commit_at_end_bundles_deliver_artefacts.md`
BEFORE Sprint 39 (C2) or Sprint 41 (C3) begins. NEW Sprint 39 /
Sprint 41 §7 stanza Tier-0 invariant line then cites the added
invariant id. Otherwise, the candidates stay open as R-items in
`action_bank.md` for M3+ revisit.

## 7. Open questions for deliver-agent + human

Eleven open questions surfaced from this design freeze for
deliver-agent + human + Codex per-sub-sprint review at Sprint 37
close (or downstream sub-sprint planning rounds). Per design doc
§13.3:

| # | Question | Source design § | Recommended default | Decision needed by |
|---|----------|-----------------|---------------------|---------------------|
| 7.1 | CLOSE → TERMINAL phase enum rename: keep phase enum unchanged (default) or rename at Sprint 38 (touches runtime phase machine; riskier)? | design doc §6.1 + §6.2.4 + §13.3 OQ-13.3.1 | Keep enum unchanged; Skill file named `terminal.yaml` carries architectural intent | Sprint 38 planning round |
| 7.2 | C2 Tier-0 elevation (guardrail refusal non-overridability)? | design doc §12.2 + §13.3 OQ-13.3.4 | **DEFER** per constitution-discipline; carry as R-item | Sprint 37 close (before Sprint 38 begins) |
| 7.3 | C3 Tier-0 elevation (state-bus boundary enforcement)? | design doc §12.3 + §13.3 OQ-13.3.5 | **DEFER**; carry as R-item | Sprint 37 close |
| 7.4 | Tier-0 candidate write-up file convention: should QUALIFIED-DEFER candidates (C2, C3) get separate write-up files, or is design doc §12 enumeration sufficient? | This handoff §6 | Design doc §12 enumeration sufficient (dev interpretation of Sprint 37 §10 condition #2) | Sprint 37 close |
| 7.5 | Guardrail `on_fail: downgrade_reason` mode for S2 `intake_complete_required` (OLD Sprint 36 §5.3 Option B carried forward as Sprint 39 OQ)? | design doc §9.6 Alternative D + §13.3 OQ-13.3.6 | Default Option A (reject-and-hint); Option B remains future-extensible | Sprint 39 planning round |
| 7.6 | Sprint 31 `alternate_candidate_use_cases` teaching duplication: only in `discover_triage.yaml` (default) or copy into resolve-side Skills for mid-session topic-shift teaching? | design doc §7.2.2 + §13.3 OQ-13.3.7 | Only DISCOVER + rely on `prior_use_case_carry` for resolve-side | Sprint 40 / 41 planning round |
| 7.7 | `prior_use_case_carry` slot cap (3 citations default) + aging window (4 turns default)? | design doc §10.4 + §13.3 OQ-13.3.8 | cap=3, aging=4 turns (OLD Sprint 36 §6.6 carried forward) | Sprint 41 planning round |
| 7.8 | Dropped-intake-fields trace surface at UC switch (project as soft-signal slot `carried_intake_fields_dropped_at_switch`, or silent drop)? | design doc §13.3 OQ-13.3.9 (carried from OLD Sprint 36 §6.7) | Project as soft signal | Sprint 41 planning round |
| 7.9 | Premise refinement (`system_prompt.txt` 101-line shape): Sprint 40 dev judgement on lines 45-52 (DISCOVER phase guidance) — migrate into `discover_triage.yaml` (default) or compress in shell? | design doc §7.3 + §13.2 + §13.3 OQ-13.3.10 | Migrate bulk into Skill; leave one-line shell pointer | Sprint 40 planning round |
| 7.10 | Supersede pattern timing for OLD `skill_foundation_design.md` and `skill_orchestration_candidates.md`: already applied by deliver-agent at fd7396a per §6 #9 fence default. Any need for dev to revisit at Sprint 37 close? | This handoff §1.3 + design doc §13.3 OQ-13.3.11 | No revisit; pattern already correctly applied; dev does NOT edit | Sprint 37 close (verify only) |
| 7.11 | The `system_prompt.txt` lines 54-101 (`request_handover` decision tree) Tier-0-ness vs orchestration-shell residency: stays in shell per decision (f) §7.3, but the decision-tree shape (USER-EXPLICIT REQUESTS → ... → INTAKE COMPLETION → BOT LIMITS → ...) is a SCOPED LLM-DECISION-WALK across the 23-value enum. Is this an unflagged "if-else in prompt" per §4.1 Q6 that Codex should evaluate? | design doc §11.6 verdict NO (decision-tree maps situation-to-enum, not per-UC-pair) | Codex per-sub-sprint review verifies §4.1 Q6 verdict independently | Sprint 37 Codex review |

## 8. Anti-hardcode self-walk (§4.1 nine questions) on Sprint 37 itself

Sprint 37 itself is a docs-only / design-freeze sub-sprint per
`docs/sprint_objective.md` §1 + §3. Per `iteration_governance.md`
§4.1, pure docs-only / design-freeze PRs are exempt from per-PR
anti-hardcode review and may return `approve` with a one-line
exemption note. The diff stages exactly two files: the new design
doc + this handoff. No `.java`, `.py`, `.yml`, `.yaml`,
`.properties`, prompt, CaseSpec, judge, override, foundational
doc, governance doc, or sprint-archive edit.

**Exemption verdict for Codex (if dispatched):** `approve —
exemption: design-freeze sub-sprint, no semantic surface touched.
The design doc itself is the freeze contract; Codex review's
substantive work is verifying the proposed design semantics, which
is the design doc §11 walk-through, not the Sprint 37 diff.`

Walking the nine §4.1 questions for traceability (Sprint 37 diff
itself, NOT the proposed design semantics — those are walked in
design doc §11):

| Q | Question (abbreviated) | Answer |
|---|------------------------|--------|
| Q1 | Adds keyword / regex / if-else / enum / per-UC matrix? | **NO.** Diff adds two doc files. |
| Q2 | Tier-0 justified? | N/A. No Tier-0 added in this commit. Design doc §12 surfaces 5 candidates for human-review escalation (DEFER recommendation across all 5). |
| Q3 | Soft signal alternative considered? | **YES.** Design doc §11.3 walks Q3 explicitly: state-bus soft-signal dimensions where applicable; terminal predicates as §1.4 hard surfaces per verbatim authorizations + precedents. |
| Q4 | Visible-eval / trace phrasing / CaseSpec id encoded? | **NO.** Design doc references the Codex M1 Finding 1 Alice fabrication shape in prose; does NOT encode CaseSpec ids or trace phrasing into runtime / prompt / judge. |
| Q5 | Moves semantic ownership from LLM to Java? | **NO.** Skill `procedure` is LLM-soft; `guardrails` enforce Runtime-floor only; state-bus enforces capability-floor only. |
| Q6 | Adds if-else in prompt? | **NO.** Migrated teaching paragraphs are principle-level (sibling to existing Sprint 23 precedents); `system_prompt.txt` lines 54-101 decision tree stays cross-Skill (Q7.11 surfaces for Codex independent verification). |
| Q7 | Preserves tool schema / capability / PII / grounding floor? | **YES.** Tool schema unchanged; capability unchanged; PII unchanged. Grounding floor extended NARROWLY per §6 #4 verbatim authorization (S1 `must_cite_source`). |
| Q8 | Generalization eval coverage shipped? | N/A for Sprint 37 itself (docs-only). Prospective coverage in §10 Layer-classification self-walk below names what Sprints 38 / 39 / 40 / 41 will ship. |
| Q9 | Temporary measure with sunset? | N/A. Skill abstraction is permanent. |

**Sprint 37 self-walk verdict: `approve`** — Sprint 37 itself is
exempt per §4.1 exemption clause; the substantive review is on the
proposed design semantics, walked in design doc §11 (verdict
`approve` with 5 Tier-0 candidates surfaced for deliver-agent +
human + Codex evaluation at Sprint 37 close).

## 9. Files changed (Sprint 37 dev commit scope)

| path | change type | one-line description |
|------|-------------|----------------------|
| `docs/proposals/skill_registry_design.md` | **NEW** | Architectural decision doc for NEW M2 Skill Registry abstraction. 13 sections (10 decisions a-j + §4.1 walk + Tier-0 enumeration + downstream reference index + premise refinements). 3633 lines. Frontmatter: `doc_tier: proposal`, `status: proposal`, `implementation_status: not_started`, `source_of_truth: this file`, `supersedes: [docs/proposals/skill_foundation_design.md, docs/proposals/skill_orchestration_candidates.md]`. |
| `docs/sprints/sprint-037-handoff.md` | **NEW** | This file. 12-section dev-authored archive per `docs/sprint_objective.md` §11. |

Out-of-scope hard fences (per Sprint 37 §6 + dev prompt §6):

- No `.java` / `.py` / `.ts` / `.tsx` / `.yml` / `.yaml` /
  `.properties` under `server/` / `ui/` / `eval_interactive/`.
- No edits to `server/src/main/resources/prompts/system_prompt.txt`.
- No new CaseSpecs.
- No edits to existing case families / shadow case families / smoke /
  bad cases / probe / overrides.
- No `docs/foundational/**` edit.
- No `docs/current/iteration_governance.md` /
  `docs/current/doc_governance.md` /
  `docs/current/agent_context_guide.md` /
  `docs/runtime_freeze_and_risk_policy.md` edit (constitution-
  discipline preserved per
  `feedback_constitution_discipline_vs_planning_anticipation.md`;
  Tier-0 candidate write-up lives in design doc §12, NOT in
  foundational docs).
- No `docs/sprints/sprint-001-*` through `sprint-036-*` edit.
- No `docs/milestones/M1_objective.md` /
  `docs/milestones/M2-Skill_objective.md` edit.
- No `docs/milestone_objective.md` /
  `docs/sprint_objective.md` /
  `docs/10-handoff.md` /
  `docs/action_bank.md` /
  `docs/codex-findings.md` edit by dev (deliver-agent owns at
  Sprint 37 close).
- No `compact/sprint-037-*-prompt.md` edit (deliver-agent owns).
- No edits to OLD `docs/proposals/skill_foundation_design.md` OR
  `docs/proposals/skill_orchestration_candidates.md` (supersede
  pattern already applied by deliver-agent at commit fd7396a per
  Sprint 37 §6 #9 / #10 fences).
- No NEW Tier-0 candidate write-up file (per §6 dev interpretation:
  all 5 candidates recommend DEFER; no qualified-and-elevate
  candidate warrants a separate file).
- No Java test rerun, no smoke run, no bad-case suite run, no
  interactive eval rerun (Sprint 37 is docs-only).

Pre-existing untouched working-tree state at session start: CLEAN
per `git status`; HEAD `6d97888`. The deliver-agent's M2
supersession bundle is already committed at fd7396a; no orphaned
working-tree changes.

## 10. Layer-classification self-walk per Sprint 37 §8 stanza

**Sprint 37 itself: docs/design only.** No `server/` code change;
no test code; no Java change; no prompt change; no eval change.
The architectural decision doc is a governance / eval-spec
instrument that sets the contract Sprints 38 / 39 / 40 / 41
implement. Per `iteration_governance.md` §3.2 default tail
classification when no §3.1 layer cleanly fires for a docs-only
artefact: this is the same shape as OLD Sprint 30 / 35 / 36
design-freeze sprints.

**PROSPECTIVE coverage for Sprints 38 / 39 / 40 / 41** per Sprint
37 §8 stanza multi-layer prospective:

- **Sprint 38** layer: `prompt_projection` (Skill envelope reaches
  LLM via Skill-composed PhasePlan) + `skill_state` (SkillRegistry
  holds Skill definitions across requests; SkillLoader on boot) +
  Runtime-owned PhasePlan composition per §1.4.
  - Per design doc §3 + §4 + §6.2.1-§6.2.4 + §13.1 reference index.
- **Sprint 39** layer: `prompt_projection` (RESOLVE Skills reach
  LLM via PhasePlan) + `semantic_planner` (LLM still owns next-
  action choice within S1/S2 Skill envelope per §1.3) +
  `skill_state` (SkillGuardrailDispatcher reads active Skill's
  guardrails) + Runtime-owned grounding floor per §1.4 (NEW S1
  `must_cite_source`) + Runtime-owned capability floor per §1.4
  (S2 `intake_complete_required` relocated; existing predicates
  migrated).
  - Per design doc §6.2.5 + §6.2.6 + §8 + §9 + §13.1.
- **Sprint 40** layer: `prompt_projection` (`system_prompt.txt`
  structural change to orchestration shell + DISCOVER Skill
  `procedure` extension) + `semantic_planner` (LLM input contract
  change — same teaching content, different surface).
  - Per design doc §7 + §13.1.
- **Sprint 41** layer: `skill_state` (SkillStateBus + per-Skill
  `state_inheritance`; UC switch detection on existing M1
  surfaces) + `prompt_projection` (NEW `prior_use_case_carry`
  slot in `ContextProjectionBuilder`).
  - Per design doc §10 + §13.1.

**§7 stanza fields filled** (Track A for Sprint 37 itself; Track B
prospective per `feedback_multi_layer_prospective_stanza.md`):

- **Target failure layer:** Sprint 37 itself: docs/design only —
  no behaviour change; no layer touch. Sprint 37 produces an
  architectural decision doc. PROSPECTIVE per Sprints 38-41 per
  the layer breakdown above.
- **Tier-0 invariant:** Sprint 37 adds NO Tier-0 invariant by
  default. Design doc §12 surfaces five candidates (C1 rejected
  as existing invariant; C2 + C3 QUALIFIED-DEFER; C4 + C5 not
  candidates per verbatim authorizations + precedents). Deliver-
  agent + human + Codex evaluate at Sprint 37 close; possible
  separate commit per `feedback_commit_at_end_bundles_deliver_artefacts.md`
  if C2 OR C3 elevated; otherwise no governance change.
- **Semantic hardcode:** No semantic hardcode introduced by
  Sprint 37 (docs-only). The PROPOSED design at
  `docs/proposals/skill_registry_design.md` is Constitution-
  compliant per the §4.1 walk in design doc §11 (verdict
  `approve`).
- **Generalization coverage:** N/A for Sprint 37 itself. PROSPECTIVE
  coverage per Sprints 38-41 per design doc §11.8 + Sprint 37
  contract §8 generalization-coverage field.

## 11. §5 Eval Acceptance bars — adapted for design-freeze sub-sprint per M2 §5 recalibration

Walking each §5.1 bar in `iteration_governance.md` adapted for
Sprint 37's no-behaviour-change scope (per M2 §5 acceptance
recalibration 2026-05-17 + Sprint 37 §9 success metrics):

| Bar | Sprint 37 status | Evidence |
|-----|------------------|----------|
| **Target cases pass** | **N/A — design-freeze sprint, no behaviour change.** The "target" for Sprint 37 is the design doc itself; coverage is the §4.1 walk completeness + Tier-0 candidate enumeration + 10 design decisions documentation. | Design doc §1-§13 complete; design doc §11 walk verdict `approve`; design doc §12 5 candidates enumerated; this handoff §3 premise re-verification 10/10 (1 refinement, 9 PASS). |
| **Neighbor cases no regression** | **N/A — design-freeze sprint, no behaviour change.** | |
| **Negative-control cases unchanged** | **N/A — design-freeze sprint, no behaviour change.** | |
| **Shadow cases no regression** | **N/A — design-freeze sprint, no behaviour change.** | |
| **Safety floor unchanged** | **YES — design-freeze sprint, zero runtime change.** | Working tree clean at session start; HEAD 6d97888; baseline 983/1-inherited/0/2 preserved by construction. |
| **Grounding floor unchanged** | **YES — design-freeze sprint, zero runtime change.** The grounding-floor diagnostics per `faq_grounding_contract.md` are unchanged. Design doc §11.7 names the prospective Sprint 39 NARROW extension of the grounding floor per S1 `must_cite_source` per §6 #4 verbatim authorization; Sprint 37 ships none of it. | |
| **Wrong-containment rate unchanged or down** | **N/A — design-freeze sprint.** | |
| **Over-escalation rate unchanged or down** | **N/A — design-freeze sprint.** | |
| **Architecture-health metrics not regressed** | **No regression** — `new_semantic_hardcode_count` = 0 (no runtime code added); `soft_signal_conversion_count` = 0 (Sprint 41 implements `prior_use_case_carry`, not Sprint 37); `planner_ownership_ratio` unchanged; `shadow_disagreement_rate` not measured. | per `iteration_governance.md` §6 metric definitions; Sprint 37 dev confirmed zero runtime change. |

**Per §5.5 smoke composite_score demotion** (effective 2026-05-16):
Sprint 37 does not run smoke; the demoted observation is not
applicable.

**Per §5.6 bad-case suite manual review + M2 §5 acceptance
recalibration** (effective 2026-05-17): Sprint 37 does not run the
bad-case suite; per M2 §5 the bad-case suite (Alice) is
OBSERVATION ONLY for THIS milestone, not gate. Sprint 37 ships no
behaviour change so observation is N/A.

**Sprint 37 hard gates (per Sprint 37 §9):**

- ✅ `docs/proposals/skill_registry_design.md` exists and locks
  all 10 design decisions (a)-(j) per §2 (verified: §2-§11 of
  design doc).
- ✅ Each design decision section names: decision; rationale;
  alternatives considered + why rejected; §1.7 boundary check;
  downstream sub-sprint reference (verified: every decision §
  follows the structure).
- ✅ §4.1 anti-hardcode kernel walk-through completed (design doc
  §11; verdict `approve`).
- ✅ Tier-0 candidate enumeration completed (design doc §12; 5
  candidates with qualification verdicts).
- ✅ Retroactive migration mappings cite line numbers at HEAD
  (design doc §6 + §7 + §8 per HEAD 6d97888 citations).
- ✅ Constitution-compliance verified (design doc §11 per-Q
  verdicts; no per-UC-branch if-else in proposed design).
- ✅ Handoff §8 walks §4.1 kernel on Sprint 37 itself; verdict
  `approve`.
- ✅ Reproducibility: every code citation in design doc + handoff
  cites file path + line number per HEAD 6d97888 (extraction via
  read + grep tools recorded in this handoff §1.2 table).

**Codex per-sub-sprint review at Sprint 37 close** (per Sprint 37
§9 Codex review section + §4.3 trigger #1 + #2): expected verdict
`approve`. Codex verifies:

- §4.1 anti-hardcode kernel verdict matches design doc §11.
- §6 #4 verbatim authorization preserved (S1 `must_cite_source`
  scope bounded; not generalized).
- No per-UC-branch if-else in PROPOSED Skill bodies (Skill data
  model fields, `procedure` text, `guardrails` declarations,
  `state_inheritance` declarations).
- Tier-0 candidate enumeration (5 candidates) assessed
  independently; verdicts ALIGN with design doc §12.

## 12. Closure verdict placeholder

Per `feedback_handoff_verdict_section_delegation.md` (carried
forward from OLD Sprint 36 §12 pattern), this section is a
placeholder. The deliver-agent + human + Codex per-sub-sprint
review fill it in at Sprint 37 close per Sprint 37 §11 + §12 +
`iteration_governance.md` §4.3 trigger #1 + #2.

The deliver-agent dispatches Codex per-sub-sprint review at
Sprint 37 close per Sprint 37 §5.1 sub-sprint-close artefacts
(deliver-agent owns `compact/sprint-037-review-prompt.md`
drafting + Codex dispatch). Codex review verdict (per
`iteration_governance.md` §4.1 + `docs/codex-findings.md`
sprint-close header per §4.2) is written to `docs/codex-findings.md`
at Sprint 37 close; deliver-agent archives to
`docs/sprints/sprint-037-codex-review.md` at sub-sprint close.

| field | value |
|-------|-------|
| status | **CLOSED PASS** |
| classification | **A — clean PASS** (no blocking findings; Codex `pass / 0` on first pass; all 5 Tier-0 candidate verdicts CONFIRMED; OQ-7.11 verdict AGREED WITH DEV; both load-bearing pre-decisions sound) per `iteration_governance.md` §4 close decision rule A. NOT "A-with-Codex-skipped" (Codex fired per §4.3 trigger #1 + #2). NOT "A-with-Codex-finding-OOSR" (no OOSR classification surfaced; all Codex findings substantive PASS). Matches Sprint 36's first-pass `pass / 0` precedent for the OLD M2-Skill design freeze — a second instance of the design-freeze sub-sprint clean-close pattern. |
| Codex outcome | `decision: pass / blocking_count: 0` (Sprint 37 per-sub-sprint Codex review at commit range `6d97888..51c327c`). Live `docs/codex-findings.md` archived to `docs/sprints/sprint-037-codex-review.md` per `feedback_packaging_codex_findings_supersession.md` delete-and-add pattern; live file reset to scaffold awaiting Sprint 38 close. |
| Tier-0 candidate disposition | All 5 candidates per design doc §12 evaluated at Sprint 37 close: **C1 (tool-whitelist enforcement)** REJECTED as new candidate — Codex confirmed existing invariant via `PhasePlan.allowedTools` (`PhasePlan.java:15-16`) + `ToolDispatcher.validateAgainstPlan` (`ToolDispatcher.java:183-195`). **C2 (guardrail refusal non-overridability)** QUALIFIED → **DEFER** per `feedback_constitution_discipline_vs_planning_anticipation.md`; opened R-item `R-skill-guardrail-non-overridability-tier-0` in `docs/action_bank.md` for M3+ revisit after Sprint 39 dispatcher evidence. **C3 (state-bus boundary enforcement)** QUALIFIED → **DEFER** per same rationale; opened R-item `R-skill-state-bus-boundary-enforcement-tier-0` for M3+ revisit after Sprint 41 state-bus evidence. **C4 (S1 `must_cite_source`)** NOT A CANDIDATE per M2 §6 #4 verbatim bounded inversion — the generic `D-hard-citation-gate` deferral remains valid. **C5 (S2 `intake_complete_required`)** NOT A CANDIDATE per Sprint 7 §I2 precedent. **No `runtime_freeze_and_risk_policy.md` edit** at Sprint 37 close; no separate Tier-0 candidate write-up file (per OQ-7.4 §12-only pre-decision). |
| Open question disposition | **Sprint-37-close OQs (5 total)**: OQ-7.2 + OQ-7.3 DEFER both (encoded above); OQ-7.4 §12-only enumeration sufficient (no separate write-up file); OQ-7.10 supersession pattern already correctly applied at deliver-agent commit `fd7396a` (Codex verified in §2 scope-discipline); OQ-7.11 AGREE WITH DEV — `system_prompt.txt:54-101` is situation-to-enum, NOT per-UC-pair (Codex's exact note: "The UC-specific mentions at lines 86 and 88-89 are compatibility/exclusion guidance for canonical reason selection, not a hidden Skill-body branch table"). **Downstream-planning OQs (6 total)**: OQ-7.1 (CLOSE→TERMINAL enum rename) deferred to Sprint 38 planning round; OQ-7.5 (S2 `on_fail` mode) deferred to Sprint 39 planning round; OQ-7.6 (Sprint 31 paragraph duplication) deferred to Sprint 40/41 planning round; OQ-7.7 (`prior_use_case_carry` cap=3 / aging=4) deferred to Sprint 41 planning round; OQ-7.8 (dropped-intake-fields trace at UC switch) deferred to Sprint 41 planning round; OQ-7.9 (premise refinement Sprint 40 dev judgement) deferred to Sprint 40 planning round. |
| Premise refinement disposition | Premise #7 (`system_prompt.txt` 101 lines — Sprint 23 23-28; Sprint 31 30-34; Sprint 33 36-43; DISCOVER guidance 45-52; `request_handover` decision tree 54-101) preserved through Sprint 38 + Sprint 40 contract drafting; informs Sprint 40 dev judgement on lines 45-52 (default per OQ-7.9: migrate bulk to `discover_triage.yaml`). Sprint 33 cue/slot split (`PhaseEvaluator.java:432-448` cue + `system_prompt.txt:36-43` slot) preserved exactly — Sprint 38 migrates cue (part of decision (e) DISCOVER), Sprint 40 migrates slot (part of decision (f)); both end up in `discover_triage.yaml` `procedure`. Premise #8 IntakeFieldsRegistry correction (NOT `UseCaseDefinition.requiredIntakeFields`) preserved through Sprint 39 contract drafting for S2 `intake_complete_required` predicate. |
| date | 2026-05-17 |
