---
title: Sprint 36 handoff — Skill foundation + UC-switching continuity design freeze (M2-Skill sub-sprint 1)
doc_tier: sprint-archive
status: archived
implementation_status: implemented
source_of_truth: this file
last_reviewed: 2026-05-17
review_cadence: ad hoc
supersedes: []
superseded_by: null
notes: >
  Sprint 36 is the first sub-sprint of Milestone M2-Skill (Skill
  Foundation + UC-Switching Continuity) per
  `docs/milestone_objective.md`. Layer `eval_spec` / governance per
  `docs/current/iteration_governance.md` §3.2 Q6 (design-freeze
  output is a governance / eval-spec instrument). Design-freeze
  sub-sprint; no production code change. §7 stanza REQUIRED
  (multi-layer prospective per
  `feedback_multi_layer_prospective_stanza.md`). Codex per-sub-sprint
  review fires at Sprint 36 close per §4.3 trigger #1 + #2 (NOT
  deferred to M2 milestone close). Closure verdict (§12) is left
  for the deliver-agent + human + Codex per-sub-sprint review per
  `feedback_handoff_verdict_section_delegation.md`.
---

# Sprint 36 handoff — Skill foundation + UC-switching continuity design freeze

## 1. Context Pack

### 1.1 Relevant docs (sampled & read)

| path | tier (best guess) | status (best guess) | one-line relevance |
|------|-------------------|---------------------|--------------------|
| `docs/sprint_objective.md` | current-runtime | current | Sprint 36 authoritative scope: §1 sub-sprint class (design-freeze; multi-layer prospective §7 stanza); §2 goal (D1-D6 sub-decisions); §3 non-goals; §4 premise check (10 items); §5 file table + §5.1 design doc structure (8 sections); §6 hard fences (19 items); §8 §7 stanza (Track A + Track B prospective); §9 success metrics; §10 stop conditions; §11 12-section handoff contract. |
| `docs/milestone_objective.md` | current-runtime | current | M2-Skill milestone north star. §1 5-sub-sprint layer breakdown; §2 goal (6-decision design freeze; S1 grounding-floor predicate is the Codex M1 Finding 1 fix at deeper layer); §6 hard fences (#1 §1.7 enforced; #2 §1.4 Runtime-floor only; #4 HARD FENCE INVERSION with human authorization quote — load-bearing for Sprint 36 §4 D3); §10 stop conditions. |
| `docs/proposals/skill_orchestration_candidates.md` | proposal | superseded (this sprint flips frontmatter) | Sprint 5 (F2) original proposal. 5 candidate skills (S1 + S2 + S3 + S4 + S5); explicit "Do NOT introduce a new skill runtime framework. Implement S1 as a parametrized `PhasePlan` inside `PhaseEvaluator.plan(...)`." Upstream source for `skill_foundation_design.md`. |
| `docs/milestones/M1_objective.md` §12 closure verdict | current-runtime | archived | M1 closed PASS A-with-Codex-OOSR-classification 2026-05-17. Codex M1 Finding 1 (Alice grounding fabrication) is the R-item Sprint 37 S1 addresses; Sprint 36 names the predicate that prevents the failure shape. |
| `docs/sprints/M1-codex-review.md` | sprint-archive | archived | Codex M1 review snapshot. Finding 1 details: Alice trace shows the bot converted generic FAQ article `ka41r000000LIEJAA4` into a specific claim about AD-2001 removal reason. This is the failure shape S1's citation predicate prevents. |
| `docs/sprints/sprint-030-handoff.md` + `sprint-030-objective.md` | sprint-archive | archived | The closest prior Sprint-30-shape design-freeze precedent. Sprint 36 design-doc shape mirrors Sprint 30 (architectural decision doc + frontmatter edit on upstream + 12-section handoff) but is NOT a strict template — Sprint 36 has the multi-layer prospective §7 stanza + Tier-0 candidate surfacing path. |
| `docs/sprints/sprint-033-objective.md` + `sprint-034-objective.md` + `sprint-035-objective.md` | sprint-archive | archived | Most recent sub-sprint contract shapes. Sprint 35 §8 stanza (eval_spec for diagnostic sprint) is the closest analogue for Track A. |
| `docs/current/iteration_governance.md` | durable-connective | current | Constitution + Fix Layer Classification (§3.2 Q6 governance/design-doc layer); §4.1 nine-question kernel walked in design doc §7; §4.3 per-sub-sprint Codex triggers; §5/§5.5/§5.6 acceptance bars (Sprint 36 ships zero behaviour change); §7 sprint-objective stanza shape; §8 milestone framework. |
| `docs/current/faq_grounding_contract.md` | current-runtime | current | Grounding-floor contract — `L1:source_citation_present` is the boundary the S1 citation predicate aligns with (design doc D3 §4.7). |
| `docs/runtime_freeze_and_risk_policy.md` §1 / §2 | foundational (top-level) | current | Tier-0 invariant catalogue. Read for D6 §4.1 walk-through Q2 + §8 Tier-0 candidate evaluation. Not edited in Sprint 36. |
| `docs/action_bank.md` | durable-connective | current | R-item registry. Read for M2 R-items expected (R-grounding-discipline-iterative-search-fabrication, D-hard-citation-gate deferral status, D-new-escalation-reason-enum deferral). Not edited by dev in Sprint 36 (deliver-agent owns at close). |
| `.claude/agent-memory/sprint-deliver-orchestrator/feedback_multi_layer_prospective_stanza.md` | feedback memory | current | Track A + Track B prospective stanza convention for §8 / §10 of handoff. |
| `.claude/agent-memory/sprint-deliver-orchestrator/feedback_constitution_discipline_vs_planning_anticipation.md` | feedback memory | current | Why Sprint 36 dev does NOT edit governance / foundational docs (Tier-0 candidate in §8 of design doc is a write-up, NOT a `runtime_freeze_and_risk_policy.md` edit). |
| `.claude/agent-memory/sprint-deliver-orchestrator/feedback_commit_at_end_bundles_deliver_artefacts.md` | feedback memory | current | Bundle policy — dev does NOT stage deliver-agent-owned files. |
| `.claude/agent-memory/sprint-deliver-orchestrator/feedback_handoff_verdict_section_delegation.md` | feedback memory | current | §12 closure-verdict placeholder convention; deliver-agent fills at close. |

### 1.2 Relevant code paths (verified at session start, 2026-05-17, HEAD `eb65e2b`)

Read-only for Sprint 36. The Sprint 36 design freeze characterizes
these surfaces; Sprint 37 / 38 / 39 implement.

| path | lines | what it governs |
|------|------:|-----------------|
| `server/src/main/java/com/gumtree/csagent/service/runtime/PhaseEvaluator.java` | 30 (`INTAKE_UCS` set); 390-457 (DISCOVER branch + Sprint 33 ad-status disambiguation paragraph); 459-510 (CONFIRM + CLOSE branches); 512-543 (ESCALATE branch); 552-596 (RESOLVE-INTAKE branch + `buildIntakeSystemInstruction`); 598-665 (RESOLVE-FAQ branch including the existing S1 sequence teaching in `systemInstruction` / `groundingInstruction` / `escalationPolicy`) | Per-phase `PhasePlan` construction. S1 envelope teaching at line 633-665 already names the FAQ-grounded-resolve sequence. Design doc D1 §2.1 cites verbatim. |
| `server/src/main/java/com/gumtree/csagent/model/PhasePlan.java` | 21-33 (record fields); 35-145 (builder pattern) | Existing `PhasePlan` shape: 11 fields. Design doc D1 §2.1 picks **reuse + extend** (no new field; envelope data + prompt teaching). |
| `server/src/main/java/com/gumtree/csagent/service/tools/RecordOutcomeTool.java` | 36-94 (execute); 110-121 (normalizeOutcomeClass with canonical `resolve / escalate / abandon` enum) | Existing record_outcome dispatch. No citation predicate currently. Design doc D3 §4.4 cites — S1 NEW predicate fires at this dispatch site. |
| `server/src/main/java/com/gumtree/csagent/service/runtime/EscalationReasonResolver.java` | 84-103 (PRIORITY table: `user_requested=1`, `user_distress=2`, `intake_complete_for_uc_g/h/i/j/k=20-24`, `incomplete_intake=25`); 232-247 (resolve precedence); 256-285 (canonicalize) | Existing escalation-reason precedence + canonicalization. Design doc D2 §3.2 cites the §E1 downgrade pattern (`ControlKernel.applyEscalationReason` line 155-167) for D4 alternative option (B). |
| `server/src/main/java/com/gumtree/csagent/service/runtime/ContextProjectionBuilder.java` | 346-377 (`intake_state` slot, Sprint 7 §I2); 396-416 (`alternate_candidate_use_cases` slot, Sprint 31); 418-432 (`discover_disambiguation_signals` slot, Sprint 33); 867+ (build helpers) | Existing projection slots. Design doc D5 §6.6 cites for the NEW `prior_use_case_carry` slot shape (sibling pattern). |
| `server/src/main/java/com/gumtree/csagent/service/runtime/UseCaseRegistryService.java` | 23 (registry map); 100 (`getUseCase`); 166-173 (`UseCaseDefinition` record: `ucId / name / topicSubjects / riskLevel / allowBotResolution / path`) | UC registry. **Premise correction surfaced (see §3 below):** `UseCaseDefinition` does NOT carry `requiredIntakeFields`. The actual source is `IntakeFieldsRegistry` (see next row). |
| `server/src/main/java/com/gumtree/csagent/service/runtime/IntakeFieldsRegistry.java` | 30 (purpose); 110-112 (`isIntakeUseCase`); 118-121 (`requiredFieldsFor`); 127-131 (`canonicalFieldName`); 167-178 (`fieldsRemaining`); 184-193 (`intakeComplete`) | **Material finding:** the actual registry for required intake fields. `IntakeFieldsRegistry.intakeComplete(uc, collected)` is the predicate S2's terminal predicate calls per design doc D4 §5.2. |
| `server/src/main/java/com/gumtree/csagent/service/runtime/AgentRunLoopImpl.java` | 106-108 (`FAQ_PATH_UCS`); 120-121 (`INTAKE_COMPLETE_GUARD_REJECT_REASON`); 300-340 (intake-complete guard dispatch, Sprint 7 §I2); 343-402 (record-outcome guard dispatch, Sprint 11 §M1); 404-460 (S1 FAQ-grounded-resolve guard dispatch, Sprint 6 §G2); 588-610 (persistInlineIntakeFields); 628-651 (`shouldRejectIncompleteIntakeHandover`, Sprint 7 §I2); 690-734 (`shouldRejectFaqMissHandover`, Sprint 6 §G2); 745-759 (`shouldRejectPrematureResolveOutcome`, Sprint 11 §M1) | **Critical existing surface:** S1 + S2 partial predicates ALREADY shipped as Sprint 6 / 7 / 11 work. Design doc §1.2 + §1.3 enumerates; Sprint 37 / 38 contracts must be drafted with the understanding the NEW work is narrower than the F2 proposal implied. |
| `server/src/main/resources/prompts/system_prompt.txt` | 23-28 (`already_called` teaching paragraph, Sprint 23); 30-33 (`alternate_candidate_use_cases` teaching paragraph, Sprint 31); 36-37 (`discover_disambiguation_signals` teaching paragraph, Sprint 33) | Existing teaching-paragraph location precedent. Design doc D1 §2.1 names the S1 / S2 / UC-switching envelope teaching paragraphs as sibling additions in Sprints 37 / 38 / 39. |
| `server/src/main/java/com/gumtree/csagent/service/runtime/ControlKernel.java` | 155-167 (`applyEscalationReason` with un-confirmed `user_distress` downgrade to `faq_miss_threshold_exceeded` — the §E1 pattern) | The §E1 downgrade pattern reference for D4 §5.3 Option (B). |

### 1.3 Doc-status warnings (drift observed)

- **`docs/sprint_objective.md` §4 premise #8 cites the wrong class.**
  Premise says `UseCaseRegistryService.UseCaseDefinition.requiredIntakeFields`
  field exists for UC-G/H/I/J/K per M1 Sprint 34 work. Reality at
  HEAD `eb65e2b`: `UseCaseDefinition` is a 6-field record with no
  `requiredIntakeFields` field. The actual source is the separate
  `IntakeFieldsRegistry` class (static `REQUIRED_FIELDS_BY_UC` map
  + helper methods). The premise *intent* (registry-driven source,
  no schema change) holds; only the class name is wrong. Design
  doc cites the correct source; surfaced in design doc §8.1 +
  this handoff §3.
- **Sprint 36 dev session loaded an additional material finding
  beyond §4 premises:** S1 + S2 partial predicates already ship
  as Sprint 6 §G2 / Sprint 7 §I2 / Sprint 11 §M1 work in
  `AgentRunLoopImpl`. The design freeze formalizes + extends these
  rather than introducing them from scratch. Design doc §1.2 +
  §8.2 documents.
- Working-tree at session start carried pre-existing unrelated
  mods (per `git status` summary in the session start). None
  touched by this dev pass per dev-prompt §10 hard fence. The
  deliver-agent-owned files are the human's to manage at commit
  boundary per `feedback_commit_at_end_bundles_deliver_artefacts.md`.

### 1.4 Source-of-truth decision

For the premise-check step (§3 below), the source of truth is the
verbatim Java source at the cited file:line ranges. Per
`doc_governance.md` "code ahead of docs" rule, code is the source
of truth for delivered behaviour; the §4 premise #8 in
`docs/sprint_objective.md` is an unverified premise that drifted
between draft and HEAD; the design freeze cites the correct
source-of-truth and surfaces the premise refinement.

For the design-freeze step (§4 + §5 below), the source of truth is
the design doc itself
(`docs/proposals/skill_foundation_design.md` —
`source_of_truth: this file` per its front matter). Sprint 37 /
38 / 39 read the design doc verbatim.

### 1.5 Implementation status

`implementation_status: implemented` — Sprint 36 ships exactly three
authored artefacts:

1. `docs/proposals/skill_foundation_design.md` (NEW — the architectural
   decision doc; 8 sections; D1-D6 + §4.1 walk-through + Tier-0
   candidate write-up).
2. `docs/proposals/skill_orchestration_candidates.md` (frontmatter
   EDIT only: `superseded_by` pointer + `status: proposal` →
   `status: superseded`; body unchanged).
3. `docs/sprints/sprint-036-handoff.md` (this file; 12 sections per
   `docs/sprint_objective.md` §11).

No deferred work; no partial shipment. Sprint 37 / 38 / 39
implementation rounds happen separately; the design freeze is
complete.

### 1.6 Risks before the design freeze

- **Premise correction risk (mitigated):** §4 premise #8 cited a
  non-existent class member. Mitigated by reading the actual code
  + citing the correct source in the design doc + surfacing in
  this handoff §3. Sprint 38 implementation round must reference
  `IntakeFieldsRegistry` (not `UseCaseDefinition.requiredIntakeFields`).
- **Existing-surface alignment risk (mitigated):** The Sprint 5
  (F2) proposal framing implies S1 + S2 are new; reality is they
  are partially shipped. Mitigated by §1.2 of the design doc and
  §1.5 of this handoff naming the existing partial implementations
  explicitly; Sprint 37 / 38 contracts will be drafted aware.
- **Tier-0 candidate surface risk (active):** D3 + D4 predicate
  semantics may qualify as Tier-0 territory. Surfaced in design
  doc §8.3 + §8.4 with explicit human-review escalation request
  per M2-Skill §10 stop condition #1. Deliver-agent + human +
  Codex per-sub-sprint review evaluate at Sprint 36 close BEFORE
  Sprint 37 begins.
- **§1.7 boundary risk (assessed clean):** the §4.1 walk-through
  in design doc §7 finds no §1.7 violation. The envelope teaching
  is principle-level; the predicates protect Runtime-owned floor
  only; D5 UC-switching continuity invariants are registry-level
  intersection check, not per-UC-pair branch table.
- **Java baseline risk (assessed):** Sprint 36 ships zero Java
  change. The 983/1-inherited/0/2 baseline confirmed at session
  start (the 1 inherited failure is `SystemPromptUserRequestedTiebreakerTest`
  per M1 Codex Hard-Fence Verification notes, attributed to the
  dirty working-tree `system_prompt.txt` modification, not clean
  HEAD).

## 2. Sub-sprint-objective recap

Sprint 36 is the first sub-sprint of Milestone M2-Skill (Skill
Foundation + UC-Switching Continuity, LLM-led Policy-bounded) per
`docs/milestone_objective.md`. Layer: `eval_spec` / governance per
`docs/current/iteration_governance.md` §3.2 Q6 (the architectural
decision doc IS a governance / eval-spec instrument; it sets the
contract that Sprints 37 / 38 / 39 implement).

Sprint 36 produces `docs/proposals/skill_foundation_design.md` — an
architectural decision doc that locks the hybrid framing (envelope +
recommended order via prompt; terminal predicate via Java guard for
Runtime-owned floor ONLY) for Sprints 37 / 38 AND the UC-switching
wide continuity invariant matrix for Sprint 39. The output enables
Sprint 37 / 38 / 39 to be scoped directly from the freeze without
further architectural rounds.

Six sub-decisions per `docs/sprint_objective.md` §2: D1 (envelope
shape); D2 (terminal predicate shape); D3 (S1 trigger + predicate
with verbatim human authorization quote); D4 (S2 trigger + predicate);
D5 (UC-switching wide continuity invariant matrix); D6 (§4.1
anti-hardcode kernel walk-through).

§7 stanza REQUIRED — multi-layer prospective per
`feedback_multi_layer_prospective_stanza.md`. Codex review:
per-sub-sprint at Sprint 36 close per `iteration_governance.md`
§4.3 trigger #1 (possible new Tier-0 candidate) + #2 (§1.7 boundary
discussion on hybrid framing).

Sprint 36 ships ZERO Java code change, ZERO Python code change,
ZERO prompt change, ZERO CaseSpec change, ZERO test run, ZERO smoke
run, ZERO new Tier-0 invariant in `runtime_freeze_and_risk_policy.md`
(Tier-0 candidate write-up is in the design doc §8, NOT in the
foundational doc).

## 3. Premise re-verification (each of §4's 10 points)

All 10 premise items in `docs/sprint_objective.md` §4 verified at
session start at HEAD `eb65e2b` (`sprint 35: Option β coverage
probe + §7.2 worked-example re-anchor decision`).

| # | premise | verification |
|---|---------|--------------|
| 1 | **M2-Skill milestone objective approved.** `docs/milestone_objective.md` carries the M2-Skill contract; human approved at 2026-05-17 review round with the verbatim authorization on §6 #4 hard fence inversion. | Read `docs/milestone_objective.md` end-to-end. Title "Milestone M2 — Skill Foundation + UC-Switching Continuity (LLM-led, Policy-bounded)"; status `current`; §6 #4 carries the verbatim "Accept the Skill-bounded exception to D-hard-citation-gate..." authorization 2026-05-17. No scope drift since 2026-05-17. **PASS.** |
| 2 | **`docs/proposals/skill_orchestration_candidates.md`** at HEAD carries the Sprint 5 (F2) original proposal: 5 skill candidates (S1 / S2 / S3 / S4 / S5); status `proposal`, `implementation_status: not_started`. | Read end-to-end. 5 candidates per §3 (S1 / S2 / S3 / S4 / S5); explicit recommendation §6 *"Do NOT introduce a new skill runtime framework. Implement S1 as a parametrized `PhasePlan` inside `PhaseEvaluator.plan(...)`."* (verbatim). Frontmatter `status: proposal`, `implementation_status: not_started`. **PASS.** |
| 3 | **`PhaseEvaluator.java` at HEAD** owns `PhasePlan` construction per phase. RESOLVE-FAQ + RESOLVE-INTAKE branch shapes are READ surfaces. | Read lines 390-665. Confirmed: DISCOVER branch at 397-457 includes Sprint 33 disambiguation paragraph at lines 432-448; CONFIRM at 462-487; CLOSE at 492-509; ESCALATE at 513-542; RESOLVE-INTAKE at 552-596; RESOLVE-FAQ at 598-665 with the existing S1 sequence teaching in `systemInstruction` / `groundingInstruction` / `escalationPolicy` (lines 633-665). **PASS.** |
| 4 | **`PhasePlan.java` at HEAD** defines record fields. | Read full file. 11 fields: `phase / useCase / objective / allowedTools / requiredContextKeys / maxToolSteps / allowInterimMessage / validTerminalOutcomes / systemInstruction / groundingInstruction / escalationPolicy`. Builder pattern at 55-145. **PASS.** |
| 5 | **`RecordOutcomeTool.java` at HEAD** owns dispatch + outcome-class enum. | Read full file. `execute(BotSession, parameters)` at 42-94; `normalizeOutcomeClass` at 110-121 with canonical lowercase `resolve / escalate / abandon` + legacy uppercase `RESOLVED / ESCALATED / ABANDONED` aliases. No citation predicate currently. **PASS.** |
| 6 | **`EscalationReasonResolver.java` at HEAD** owns precedence + §E1 pattern. | Read PRIORITY table at 84-103: `user_requested=1`, `user_distress=2`, `intake_complete_for_uc_g/h/i/j/k=20-24`, `incomplete_intake=25`. Canonical reasons set at 50+ includes `incomplete_intake` (67). §E1 downgrade pattern lives in `ControlKernel.applyEscalationReason` at 155-167 (verified via separate file read); the resolver itself is precedence-only. **PASS.** |
| 7 | **`ContextProjectionBuilder.java` at HEAD** owns per-turn projection. M1 Sprint 32 / 33 / 34 added slots. | Confirmed: `intake_state` at 346-377 (Sprint 7 §I2); `alternate_candidate_use_cases` at 396-416 (Sprint 31); `discover_disambiguation_signals` at 418-432 (Sprint 33). **PASS.** |
| 8 | **`UseCaseRegistryService.java` at HEAD** carries `UseCaseDefinition.requiredIntakeFields` for UC-G/H/I/J/K per M1 Sprint 34 work. | **PREMISE REFINEMENT (not drift; intent preserved):** Read `UseCaseDefinition` record at 166-173. Six fields: `ucId / name / topicSubjects / riskLevel / allowBotResolution / path`. **NO `requiredIntakeFields` field on this record.** The actual source for per-UC required-intake-fields is a SEPARATE static class `IntakeFieldsRegistry` at `server/src/main/java/com/gumtree/csagent/service/runtime/IntakeFieldsRegistry.java` with map `REQUIRED_FIELDS_BY_UC` + helpers `requiredFieldsFor(uc)`, `isIntakeUseCase(uc)`, `fieldsRemaining(uc, collected)`, `intakeComplete(uc, collected)`. The premise *intent* (registry-driven source, no schema change) holds; the premise *class name* is wrong. Design doc D4 §5.2 cites the correct source. Surfaced in design doc §8.1. **Refined (not a stop condition); Sprint 38 contract reads correct source.** |
| 9 | **`runtime_freeze_and_risk_policy.md` §1 / §2** carries existing Tier-0 surface. | Read §0 + §1.1-§1.3 + §2. Confirmed: §1.1 frozen surfaces (pre-plan reroute, same-UC progressive resolve, ResolveDisposition terminal-evidence guard, record_outcome guard, observability/validation layer); §1.3 explicitly deferred (Full Issue Ledger, per-issue budgets, all-UC task taxonomy, handover payload redesign, new escalation reason enum); §2 seven-rule runtime contract (1 explicit human always wins → 7 tool results inform but don't over-trigger). Sprint 36 does not edit; design doc §8.3 + §8.4 evaluate Tier-0 candidates. **PASS.** |
| 10 | **Java baseline** 983 / 1-inherited / 0 / 2 post-M1-close. Sprint 36 ships zero Java change; baseline SHALL run clean. | Ran `cd server && mvn test -q` at session start. Output: `Tests run: 983, Failures: 1, Errors: 0, Skipped: 2`. The 1 inherited failure: `SystemPromptUserRequestedTiebreakerTest.systemPrompt_marksActiveUcTiebreakerExplicitly:53` — attributed to the dirty working-tree `system_prompt.txt` modification per M1 Codex review notes. No Sprint 36-attributable regression (Sprint 36 ships zero Java change). **PASS (baseline holds).** |

**Premise drift summary:** 1 premise refinement (#8 — class name
was wrong; intent preserved). 9 premises PASS as written.
**No STOP condition fired** — the §10 condition #1 stop is for
premise drift that changes scope; #8 refinement is a class-name
correction that does not change the freeze's design intent
(registry-driven source, no schema change). Surfaced in design doc
§8.1 + this handoff §3 for transparent record.

## 4. Implementation walkthrough — design doc structure

`docs/proposals/skill_foundation_design.md` lands with 8 sections
per `docs/sprint_objective.md` §5.1 baseline. No departures from
the §5.1 baseline shape.

| § | header | content summary | length |
|---|--------|-----------------|-------:|
| §1 | Purpose + relation to upstream | M2-Skill anchor + relation to Sprint 5 (F2) proposal + relation to existing partial implementations (Sprint 6 / 7 / 11) + relation to M2-Skill milestone objective. §1.1 + §1.2 + §1.3 sub-sections. | ~3.5 pages |
| §2 | D1 Skill envelope shape | Proposed shape (PhasePlan data + system_prompt.txt teaching paragraph; reuse + extend, no new framework). §2.1 shape + §2.2 rationale + §2.3 D1.1 open question. | ~3 pages |
| §3 | D2 Skill terminal predicate shape | Where predicate code lives (existing precedent in `AgentRunLoopImpl`). §3.1 location + §3.2 fail-behaviour (reject / downgrade / observation-out-of-scope) + §3.3 scope discipline + §3.4 trace-log entry shape. | ~2 pages |
| §4 | D3 S1 trigger + predicate | §4.1 **verbatim human authorization quote** from M2-Skill §6 #4 (load-bearing); §4.2 trigger conditions; §4.3 **predicate scope EXACTLY** per (a)/(b)/(c) per authorization; §4.4 predicate-fail behaviour (default reject-and-hint; alternative resolve_no_citation outcome class); §4.5 predicate-input data surface; §4.6 **explicit out-of-scope enumeration**; §4.7 expansion-is-stop-condition note. | ~3 pages |
| §5 | D4 S2 trigger + predicate | §5.1 trigger conditions; §5.2 predicate scope **noting the predicate is already shipped** as `shouldRejectIncompleteIntakeHandover`; §5.3 fail behaviour (Option A reject-and-hint default; Option B canonical-reason downgrade alternative); §5.4 predicate-input data surface; §5.5 explicit out-of-scope enumeration. | ~2 pages |
| §6 | D5 UC-switching wide continuity invariant matrix | §6.1 continuity dimensions (5 rows); §6.2 behaviour-on-switch (5 columns: S/C/N/R/L); §6.3 invariant matrix (5 cells filled); §6.4 per-cell rationale; §6.5 §1.7 enforcement note; §6.6 `prior_use_case_carry` slot shape; §6.7 D5.1 dropped-fields-projection open question. | ~3 pages |
| §7 | D6 §4.1 nine-question anti-hardcode kernel walk-through | Q1-Q9 each with stated question (verbatim from `iteration_governance.md` §4.1), design's answer, cited D1-D5 section. Q-walk verdict: `approve` with one Tier-0 candidate question surfaced from D3 + D4. | ~3 pages |
| §8 | Open questions / Tier-0 candidate write-up | §8.1 premise correction (premise #8 class-name refinement); §8.2 material finding (S1 + S2 partial implementations already shipped); §8.3 Tier-0 candidate question — S1 citation predicate semantics (with explicit human-review escalation request); §8.4 Tier-0 candidate question — S2 intake-completeness predicate semantics; §8.5-§8.9 the five D-specific open questions (D3.1 / D4.1 / D5.1 / D1.1 / D5.2). | ~2 pages |

Total design doc length: ~20 pages (well within the §5 estimate of
"~300-500 lines"; actual ~750 lines including code-block examples
and tables).

## 5. Design freeze decisions — D1-D6 chosen direction + rationale

### 5.1 D1 — Skill envelope shape

**Chosen direction:** **Reuse the existing `PhasePlan` data surface
+ NEW `system_prompt.txt` teaching paragraph (one per Skill).** No
new class. No new `PhasePlan` field. The S1 envelope teaching
paragraph + S2 envelope teaching paragraph + UC-switching continuity
envelope teaching paragraph all land in Sprint 37 / 38 / 39 dev
sessions as siblings to the existing Sprint 23 / 31 / 33 teaching
paragraphs.

**Rationale:**

- Honors Sprint 5 (F2) §6 "Do NOT introduce a new skill runtime
  framework" verbatim.
- Reuses the existing RESOLVE-FAQ `PhasePlan` recommended-order
  teaching at `PhaseEvaluator.java:633-665` (already names the S1
  sequence `search_knowledge → resolve_article → grounded answer
  with source_id citation → record_outcome`).
- Honors §1.3 (LLM owns customer language, per-step argument
  choice, recommended-order deviation) by keeping the envelope
  surface teaching, not enforcement.
- Honors §1.7 by keeping the envelope teaching paragraph
  principle-level (sibling to the existing precedents).

**Upstream cite:** Sprint 5 (F2) §3 S1 + §3 S2 candidate definitions;
existing `PhaseEvaluator.RESOLVE-FAQ` branch as the implementation
precedent.

### 5.2 D2 — Skill terminal predicate shape

**Chosen direction:** **Predicate code lives in the Runtime tool
dispatch layer** (`AgentRunLoopImpl` static methods + dispatch hooks;
existing precedent per Sprint 6 / 7 / 11). Predicate-fail behaviour
is **rejection (reject-and-hint)** as default; **canonical-reason
downgrade** is the alternative for predicates aligned with the §E1
precedence pattern (D4 §5.3 carries this for S2); **observation-only**
is explicitly out-of-scope for a *terminal predicate*.

**Rationale:**

- Existing precedent at `AgentRunLoopImpl.java:300-460` for the
  three already-shipped predicates (Sprint 6 §G2 / Sprint 7 §I2 /
  Sprint 11 §M1). Sprint 37 + 38 NEW predicates follow the same
  dispatch pattern.
- §1.4 boundary discipline: predicate enforces ONLY Runtime-owned
  floor (citation presence; intake-completeness). LLM-owned
  decisions stay LLM-owned.
- Trace-log entry shape (§3.4 of design doc) ensures reproducibility
  per `feedback_deliver_agent_cited_numbers_must_be_reproducible.md`.

**Upstream cite:** existing `shouldRejectFaqMissHandover` (Sprint
6 §G2), `shouldRejectIncompleteIntakeHandover` (Sprint 7 §I2),
`shouldRejectPrematureResolveOutcome` (Sprint 11 §M1) as
implementation precedents.

### 5.3 D3 — S1 trigger + predicate (`Resolve.FAQ.GroundedAnswer`)

**Chosen direction:**

- **Trigger:** `current_phase == RESOLVE` AND `active_use_case ∈
  FAQ-path UCs` (per `UseCaseRegistryService` + existing
  `AgentRunLoopImpl.FAQ_PATH_UCS` set: UC-A, UC-B, UC-C, UC-D,
  UC-E, UC-F, UC-FP).
- **Predicate scope (verbatim per human authorization §6 #4
  2026-05-17):** fires ONLY when (a) Phase = `RESOLVE_FAQ`; (b)
  Tool call = `record_outcome` with `class=resolve`; (c) Check =
  `source_id` citation present in user-facing message.
- **Fail behaviour (recommended default):** rejection (reject-and-hint)
  at the dispatch site adjacent to `shouldRejectPrematureResolveOutcome`
  in `AgentRunLoopImpl`. Reject reason: `s1_citation_presence_required`
  (or equivalent canonical label; Sprint 37 picks final constant).
- **Out-of-scope enumeration:** no fire on `class=escalate` /
  `class=abandon`; no fire outside RESOLVE-FAQ; no content-quality
  judgement; no generic Java grounding-citation gate (the
  `D-hard-citation-gate` deferral preserved); no encoding of
  CaseSpec ids or trace phrasing.

**Rationale:**

- **Verbatim human authorization at M2-Skill §6 #4 2026-05-17** is
  the load-bearing scope-bound. Design doc D3 §4.1 carries the
  quote verbatim.
- The Codex M1 Finding 1 Alice fabrication shape (the bot converted
  generic FAQ article `ka41r000000LIEJAA4` into a specific claim
  about `AD-2001`'s removal reason) is prevented by enforcing that
  the bot cannot persist a "resolve" outcome on a fabricated answer
  — the citation-presence check at dispatch time intercepts.
- Adjacent to existing `shouldRejectPrematureResolveOutcome` Sprint
  11 §M1 guard; both fire at `RecordOutcomeTool` dispatch; S1
  predicate adds the citation check on top of the existing
  phase-disposition gate.

**Upstream cite:** Sprint 5 (F2) §3 S1 candidate definition; M2-Skill
§6 #4 human authorization verbatim; Codex M1 review Finding 1
Alice trace evidence; existing
`shouldRejectPrematureResolveOutcome` as precedent.

### 5.4 D4 — S2 trigger + predicate (`Resolve.Intake.CollectAndHandover`)

**Chosen direction:**

- **Trigger:** `current_phase == RESOLVE` AND `active_use_case ∈
  {UC-G, UC-H, UC-I, UC-J, UC-K}` per existing `PhaseEvaluator.INTAKE_UCS`.
- **Predicate scope:** fires when (a) Phase = `RESOLVE_INTAKE`;
  (b) Tool call = `request_handover` with
  `escalation_reason == intake_complete_for_uc_X`; (c) Check =
  `requiredIntakeFields` not fully populated in `session.intakeFields`
  per `IntakeFieldsRegistry.intakeComplete(uc, collected)`.
- **Predicate is already shipped** as
  `shouldRejectIncompleteIntakeHandover` per Sprint 7 §I2.
- **Fail behaviour (recommended default — Option A):** keep the
  existing reject-and-hint pattern. **Option (B) canonical-reason
  downgrade to `incomplete_intake`** carried as an open question
  for Sprint 38 planning round (D4.1).
- **Out-of-scope enumeration:** no field-requirement-determination
  (registry-driven); no content-quality judgement; no fan-out to
  non-INTAKE-path UCs; no fan-out to non-`intake_complete_for_uc_X`
  reasons; no schema change to `IntakeFieldsRegistry`.

**Rationale:**

- Existing predicate at `AgentRunLoopImpl.shouldRejectIncompleteIntakeHandover`
  (lines 628-651) already implements (a) + (b) + (c) verbatim.
- Sprint 38 contribution is mostly envelope teaching (the
  per-skill `system_prompt.txt` paragraph) + optional reframing
  (Option B); the predicate logic is in production.
- Per §1.4 boundary, the predicate protects capability/permission
  floor (refuses an un-earned "intake complete" reason); mirrors
  the §E1 pattern of refusing an un-earned Tier-X reason.

**Upstream cite:** Sprint 5 (F2) §3 S2 candidate definition; M1
Sprint 34 `IntakeFieldExtractor` UC-G/H/I/J/K extension; existing
`shouldRejectIncompleteIntakeHandover` Sprint 7 §I2 as the shipped
predicate.

### 5.5 D5 — UC-switching wide continuity invariant matrix

**Chosen direction:** A 5-row × 5-column matrix with these
behaviours (per design doc §6.3 + §6.4):

| Dimension | Behaviour |
|---|---|
| `accumulated_tool_results` | **S** (survives unconditionally) |
| `session.customerContext` | **S** (survives unconditionally) |
| `session.intakeFields` partial state | **C** (survives if compatible — registry-level intersection check via `IntakeFieldsRegistry.canonicalFieldName` + `requiredFieldsFor`) |
| Prior citations / grounding-history | **N** (NEW `prior_use_case_carry` soft-signal projection slot) |
| Skill terminal predicate state | **L** (scoped to current UC only; not session-state) |

**Rationale:**

- Per Q3 human pick "Wide full continuity" at M2-Skill approval
  2026-05-17.
- Per `docs/milestone_objective.md` §6 hard fence #1 (§1.7) +
  Sprint 36 §10 stop condition #9: invariants stated as principles
  + observable-state guards (registry-level intersection check),
  NOT per-UC-pair branch tables. The matrix above is ONE rule per
  row; the per-UC variation enters only via the registry-level
  intersection on intakeFields (a single rule, not a per-UC-pair
  table).
- The `prior_use_case_carry` slot is a sibling of the existing
  Sprint 31 `alternate_candidate_use_cases` / Sprint 33
  `discover_disambiguation_signals` slots — same observability /
  non-enforcement posture per Constitution §1.3.

**Upstream cite:** M2-Skill §2 goal (UC-switching wide continuity);
existing `IntakeFieldsRegistry.canonicalFieldName` +
`requiredFieldsFor` as the registry-level intersection mechanism;
Sprint 31 / 33 projection slot pattern as the soft-signal precedent.

### 5.6 D6 — §4.1 anti-hardcode kernel walk-through

**Chosen direction:** complete walk-through in design doc §7.
**Verdict: `approve`** with one Tier-0 candidate question surfaced
for human-review escalation (design doc §8.3 + §8.4).

**Rationale per Q:**

- Q1 (semantic hardcode?): NO. Sprint 36 zero code change; Sprint
  37 + 38 + 39 predicates protect Runtime-owned floor (not
  semantic).
- Q2 (Tier-0 justified?): N/A for Q1=NO; but design doc §8.3 +
  §8.4 surface Tier-0 candidates for human-review escalation.
- Q3 (soft signal alternative?): partial yes (D5 IS soft signal);
  partial no (D3 + D4 are hard surfaces, bounded to §1.4 floor).
- Q4 (visible-eval encoding?): NO. No CaseSpec ids, no user-message
  text matching, no per-trace phrasing.
- Q5 (LLM ownership shrink?): NO. The envelope is teaching, not
  enforcement; the LLM retains all §1.3 ownership.
- Q6 (prompt if-else?): NO. Envelope teaching paragraph is
  principle-level (sibling to existing Sprint 23 / 31 / 33
  precedents).
- Q7 (tool/PII/grounding floor preserved?): YES (grounding floor
  is narrowly extended per the human authorization §6 #4).
- Q8 (generalization coverage?): Sprint 36 itself N/A; Sprints 37
  / 38 / 39 prospective coverage per Track B in §6 below + design
  doc §7 Q8.
- Q9 (sunset?): N/A. D1-D5 are permanent architectural surfaces.

**Upstream cite:** `docs/current/iteration_governance.md` §4.1
nine-question kernel verbatim.

## 6. Generalization coverage table per §8 — Track A + Track B prospective

### 6.1 Track A — Sprint 36 itself (design-freeze sub-sprint)

| Stanza field | Value |
|---|---|
| **Target failure layer** | `eval_spec` / governance per `iteration_governance.md` §3.2 Q6. The design doc IS a governance / eval-spec instrument that sets the contract Sprints 37 / 38 / 39 implement. |
| **Tier-0 invariant** | **No new Tier-0 invariant** added by Sprint 36. The design doc §8.3 + §8.4 surface S1 + S2 predicate semantics as Tier-0 candidates for human-review escalation per `docs/milestone_objective.md` §10 stop condition #1. The Tier-0 write-up is in the design doc; the actual Tier-0 addition (if authorized) is a SEPARATE deliver-agent + human escalation commit per `feedback_commit_at_end_bundles_deliver_artefacts.md`. |
| **Semantic hardcode** | **No semantic hardcode introduced.** Sprint 36 ships zero runtime / prompt / judge change. The design doc is descriptive prose + a §4.1 walk-through; it does NOT add keyword / regex / if-else / enum / per-UC matrix anywhere. |
| **Generalization coverage** | **Target:** the `skill_foundation_design.md` doc itself — 6 sub-decisions (D1-D6) all addressed; §4.1 walk-through complete; §8 Tier-0 candidate surfaced. **Neighbor:** N/A (design-freeze sprints have no regression neighbor). **Negative:** explicit out-of-scope enumerations in D3 §4.6 + D4 §5.5 + D5 §6.5 serve as negative controls (each names what the design does NOT do). **Shadow:** N/A (design-freeze sprints have no held-out shadow). |

### 6.2 Track B — prospective for Sprints 37 / 38 / 39

The §7 stanza fields each downstream sub-sprint will need to fill,
named here so Codex Sprint 36 review can verify the prospective
alignment.

| Downstream sprint | Target failure layer | Tier-0 invariant | Semantic hardcode | Generalization coverage |
|---|---|---|---|---|
| **Sprint 37** (S1 implementation) | `prompt_projection` + `semantic_planner` + Runtime grounding floor per §1.4 | "No new Tier-0 invariant" by default; if Sprint 36 close authorizes the §8.3 Tier-0 candidate, cite the added invariant id from `runtime_freeze_and_risk_policy.md` §1/§2 | "No semantic hardcode introduced — S1 envelope teaching paragraph is principle-level adjacent to existing teaching paragraphs (§1.7 enforced); S1 predicate is bounded per human authorization (M2-Skill §6 #4 + design doc §4)" | Target = Alice bad case multi-trace closure-criterion (a); Neighbor = other FAQ-path UC traces (UC-B / UC-C-FAQ / UC-D-FAQ / UC-E / UC-F-FAQ / UC-FP-FAQ); Negative = `record_outcome(class=escalate)` traces (predicate must NOT fire); Shadow = held-out FAQ-grounded-resolve traces |
| **Sprint 38** (S2 implementation) | `prompt_projection` + `skill_state` + Runtime capability floor per §1.4 | "No new Tier-0 invariant" by default; if Sprint 36 close authorizes the §8.4 Tier-0 candidate, cite the added invariant id | "No semantic hardcode introduced — S2 envelope teaching is principle-level (§1.7); S2 predicate is the §E1-pattern extension already shipped as `shouldRejectIncompleteIntakeHandover` Sprint 7 §I2; Sprint 38 reframes + adds envelope teaching, no semantic change" | Target = cs_066 (UC-K) regression close; Neighbor = cs_036 (UC-I) + cs_038 (UC-J) + cs_040 (UC-K) regression guards + Alice UC-G/H carry-over; Negative = legitimate `intake_complete_for_uc_X` traces with all fields populated (predicate must NOT downgrade); Shadow = held-out UC-G/H/I/J/K intake traces |
| **Sprint 39** (UC-switching wide continuity) | `skill_state` + `prompt_projection` | "No new Tier-0 invariant" by default | "No semantic hardcode introduced — UC-switching continuity invariants are stated as principles + observable-state guards (registry-level intersection check on intakeFields via `IntakeFieldsRegistry.canonicalFieldName`), NOT per-UC-pair branch tables (§1.7); `prior_use_case_carry` is a sibling soft-signal projection slot per Sprint 31 / 33 precedent" | Target = a synthetic UC-switching trace (e.g., UC-A → UC-C mid-flow); Neighbor = other UC-pair transitions per the Sprint 36 freeze D5 matrix; Negative = single-UC traces (no switch; continuity invariants must NOT trigger spurious carry-over); Shadow = held-out UC-switching traces |

Track B is prospective. Each downstream sprint's contract will
re-state its stanza when drafted. Codex Sprint 36 review verifies
the prospective alignment is internally consistent and honors the
M2-Skill milestone fences.

## 7. Open questions for deliver-agent + human

The design doc §8 surfaces the open questions; restated here for
the deliver-agent + human + Codex sub-sprint close review.

| # | Question | Source design § | Recommended default | Decision needed by |
|---|----------|-----------------|---------------------|---------------------|
| 7.1 | Tier-0 candidate — S1 citation predicate semantics qualify for `runtime_freeze_and_risk_policy.md` §1 / §2 elevation? | design doc §8.3 | **DEFER** — re-evaluate post-Sprint-37 observed traces | Sprint 36 close (BEFORE Sprint 37 begins per M2-Skill §10 stop condition #1) |
| 7.2 | Tier-0 candidate — S2 intake-completeness predicate semantics qualify for `runtime_freeze_and_risk_policy.md` §1 / §2 elevation? | design doc §8.4 | **DEFER** — predicate has been in production since Sprint 7; no urgency | Sprint 36 close |
| 7.3 | D3.1 — S1 predicate-fail behaviour: rejection (default) vs new `resolve_no_citation` outcome class (alternative)? | design doc §4.4 + §8.5 | rejection (reject-and-hint) | Sprint 37 planning round |
| 7.4 | D4.1 — S2 predicate-fail behaviour: keep existing reject-and-hint (Option A default) vs canonical-reason downgrade to `incomplete_intake` (Option B)? | design doc §5.3 + §8.6 | Option A (keep existing) | Sprint 38 planning round |
| 7.5 | D5.1 — `intake_fields` dropped-at-switch projection: soft-signal slot `carried_intake_fields_dropped_at_switch` (default) vs silent drop? | design doc §6.7 + §8.7 | project as soft signal | Sprint 39 planning round |
| 7.6 | D1.1 — explicit `skill_envelope` projection field naming the active skill, or rely on existing `phase` + `active_use_case`? | design doc §2.3 + §8.8 | rely on existing (no new field) | Sprint 37 planning round |
| 7.7 | D5.2 — `prior_use_case_carry` cap (3 most recent? other?) + aging window (4 turns? other?)? | design doc §6.6 + §8.9 | cap=3, aging=4 turns | Sprint 39 planning round |
| 7.8 | Premise #8 refinement — class name correction (`UseCaseDefinition.requiredIntakeFields` → `IntakeFieldsRegistry.requiredFieldsFor`); does the deliver-agent need to amend the Sprint 38 contract draft accordingly? | design doc §8.1 + this handoff §3 | amend Sprint 38 contract draft to cite the correct class | Sprint 38 contract draft round |

Codex Sprint 36 review should examine all eight questions; §7.1 +
§7.2 are the highest-stakes (Tier-0 territory).

## 8. Anti-hardcode self-walk (§4.1 nine questions) — Track A

Sprint 36 itself is a docs-only / design-freeze sub-sprint per
`docs/sprint_objective.md` §1 + §3. Per `iteration_governance.md`
§4.1, pure docs-only / design-freeze PRs are exempt from per-PR
anti-hardcode review and may return `approve` with a one-line
exemption note. The diff stages exactly three files: the new
design doc, the frontmatter EDIT on the upstream proposal, and
this handoff. No `.java`, `.py`, `.yml`, `.yaml`, `.properties`,
prompt, CaseSpec, judge, override, foundational doc, governance
doc, or sprint-archive edit.

**Exemption verdict for Codex (if dispatched):** `approve —
exemption: design-freeze sub-sprint, no semantic surface touched.
The design doc itself is the freeze contract; Codex review's
substantive work is verifying the proposed design semantics, which
is the design doc §7 walk-through, not the Sprint 36 diff.`

Walking the nine §4.1 questions for traceability (Sprint 36 diff
itself, NOT the proposed design semantics, which the design doc §7
handles):

| Q | Question | Answer |
|---|----------|--------|
| Q1 | Adds keyword / regex / if-else / enum / per-UC matrix? | **NO.** Diff adds two doc files + one frontmatter EDIT. |
| Q2 | Tier-0 justified? | N/A. No Tier-0 added in this commit. Design doc §8.3 + §8.4 surface candidates for human-review escalation (separate process). |
| Q3 | Soft signal alternative considered? | **YES.** The design doc §3 (D2) explicitly enumerates soft-signal (observation-only) as out-of-scope for a terminal predicate; D5 design IS a soft signal (`prior_use_case_carry`) wherever possible. |
| Q4 | Visible-eval / trace phrasing / CaseSpec id encoded? | **NO.** Design doc references the Alice failure shape and the M1 Codex Finding 1 in prose, but does NOT encode CaseSpec ids or trace phrasing into runtime / prompt / judge config. |
| Q5 | Moves semantic ownership from LLM to Java? | **NO.** The Skill envelope is teaching, not enforcement. The terminal predicates protect Runtime-owned floor (§1.4) only. |
| Q6 | Adds if-else in prompt? | **NO.** Envelope teaching paragraphs (Sprint 37 / 38 / 39 ship) are principle-level, sibling to existing Sprint 23 / 31 / 33 precedents. |
| Q7 | Preserves tool schema / capability / PII / grounding floor? | **YES.** Tool schema unchanged; capability unchanged; PII unchanged. Grounding floor extended NARROWLY per human authorization §6 #4 (Sprint 37 ships). |
| Q8 | Generalization eval coverage shipped? | N/A in regression sense (design doc is the contract). Track B prospective coverage in §6.2 above names what Sprints 37 / 38 / 39 will ship. |
| Q9 | Temporary measure with sunset? | N/A. D1-D5 are permanent architectural surfaces. |

**Track A verdict: `approve`** — Sprint 36 itself is exempt per
§4.1 exemption clause; the substantive review is on the design
doc semantics, walked in design doc §7.

## 9. Files changed (Sprint 36 diff scope)

| path | change type | one-line description |
|------|-------------|----------------------|
| `docs/proposals/skill_foundation_design.md` | **NEW** | Architectural decision doc for the M2-Skill foundation hybrid framing. 8 sections (D1-D6 + walk-through + Tier-0 candidate write-up). ~750 lines. |
| `docs/proposals/skill_orchestration_candidates.md` | **EDIT (frontmatter only)** | Add `superseded_by: docs/proposals/skill_foundation_design.md`; flip `status: proposal` → `status: superseded`; append one-line pointer to `notes:`. Body unchanged (preserved per `doc_governance.md` "Forward-looking proposal docs are first-class citizens"). |
| `docs/sprints/sprint-036-handoff.md` | **NEW** | This file. 12-section dev-authored archive per `docs/sprint_objective.md` §11. |

Out-of-scope hard fences (per dev-prompt + sprint-objective §6 +
§10):

- No `.java` / `.py` / `.ts` / `.tsx` / `.yml` / `.yaml` /
  `.properties` under `server/` / `ui/` / `eval_interactive/`.
- No edits to `server/src/main/resources/prompts/system_prompt.txt`.
- No new CaseSpecs under `eval_interactive/case_specs/` or any
  case-spec location.
- No edits to existing case families / shadow case families / smoke /
  bad cases / probe / overrides.
- No `docs/foundational/**` edit.
- No `docs/current/iteration_governance.md` edit (governance
  discipline per `feedback_constitution_discipline_vs_planning_anticipation.md`).
- No `docs/current/doc_governance.md` or `docs/current/agent_context_guide.md`
  edit.
- No `docs/runtime_freeze_and_risk_policy.md` edit (Tier-0
  candidate write-up lives in the proposal doc §8, NOT in the
  foundational doc).
- No `docs/sprints/sprint-001-*` through `sprint-035-*.md` edit.
- No `docs/milestones/M1_objective.md` edit (M1 archive is
  immutable).
- No `docs/milestone_objective.md` edit (M2-Skill contract stays
  stable across sub-sprints; deliver-agent updates only at M2
  close).
- No `docs/action_bank.md` edit by dev (deliver-agent owns at
  Sprint 36 close).
- No Tier-0 / latency / budget / model-config edit.
- No smoke run, no eval rerun, no bad-case suite rerun, no Java
  test rerun for evidence (Java baseline run at session start is
  reproducibility; no behaviour change to validate).

Pre-existing untouched working-tree mods (managed by the human,
not this dev): per the git status snapshot at session start
(numerous deliver-agent-owned files: `docs/sprint_objective.md`,
`docs/milestone_objective.md`, `docs/10-handoff.md`,
`docs/action_bank.md`, `docs/codex-findings.md`,
`compact/sprint-deliver-orchestrator.md`,
`csagent_system_design_review.md`,
`server/src/main/resources/prompts/system_prompt.txt`,
multiple `server/src/main/resources/mock/**` files, and others).
None are staged by this dev per
`feedback_commit_at_end_bundles_deliver_artefacts.md`.

## 10. Layer-classification self-walk (§3)

**Sprint 36 itself: `eval_spec` / governance per `iteration_governance.md`
§3.2 Q6.** The architectural decision doc IS a governance / eval-spec
instrument; it sets the contract Sprints 37 / 38 / 39 implement.
Sprint 36 ships zero behaviour change. Per `iteration_governance.md`
§7 stanza exemption clause, a strict reading would classify Sprint
36 as design-freeze / docs-only (analogous to Sprint 30
characterization of `infra` per §3.2 Q1). The multi-layer prospective
§7 stanza convention per `feedback_multi_layer_prospective_stanza.md`
prefers `eval_spec` because Sprint 36's deliverable IS an eval/governance
contract.

**Sprint 37 prospective:** `prompt_projection` (S1 envelope teaching
paragraph in `system_prompt.txt`) + `semantic_planner` (LLM owns
recommended-order deviation per §1.3; the teaching paragraph
references LLM's own decision surface) + Runtime grounding floor
per §1.4 (the citation predicate in `RecordOutcomeTool` /
`AgentRunLoopImpl` dispatch).

**Sprint 38 prospective:** `prompt_projection` (S2 envelope teaching
paragraph in `system_prompt.txt`) + `skill_state` (the existing
`session.intakeFields` is read by the predicate; no new field) +
Runtime capability floor per §1.4 (the intake-completeness predicate
in `AgentRunLoopImpl` or `EscalationReasonResolver` dispatch).

**Sprint 39 prospective:** `skill_state` (the registry-level
intersection check on intakeFields preservation across UC switch
+ the new `prior_use_case_carry` projection slot lifecycle) +
`prompt_projection` (the UC-switching continuity envelope teaching
paragraph in `system_prompt.txt`).

No layer is `java_guard` in the sense the §3.2 Q2 question intends
(the Sprint 37 / 38 predicates protect Runtime-owned §1.4 floor, not
LLM-owned semantic decisions). No `infra` (no infrastructure
change). No `eval_spec` for Sprint 37 / 38 / 39 (those rounds
implement behaviour change; the eval/governance contract is set in
Sprint 36). No `product_policy` (no product / policy decision
required). No `judge_calibration` (no judge involved). No
`human_review_required` exit triggered for the chosen design
(Sprint 36 §8 Tier-0 candidates are surfaced for human-review
escalation per `docs/milestone_objective.md` §10 stop condition #1
— a procedural escalation, NOT a Sprint 36 STOP per §3.2 default
tail).

## 11. §5 Eval Acceptance bars — adapted for design-freeze sub-sprint

Walk each §5.1 bar in `iteration_governance.md` adapted for Sprint
36's no-behaviour-change scope:

| Bar | Sprint 36 status | Evidence |
|-----|------------------|----------|
| **Target cases pass** | **N/A — design-freeze sprint, no behaviour change.** The "target" for Sprint 36 is the design doc itself (per §6.1 Track A); coverage is the §4.1 walk-through completeness, not eval evidence. | Design doc §1-§8 complete; design doc §7 walk-through verdict `approve`; design doc §8 Tier-0 candidates surfaced. |
| **Neighbor cases no regression** | **N/A — design-freeze sprint, no behaviour change.** | |
| **Negative-control cases unchanged** | **N/A — design-freeze sprint, no behaviour change.** | |
| **Shadow cases no regression** | **N/A — design-freeze sprint, no behaviour change.** | |
| **Safety floor unchanged** | **YES — design-freeze sprint, zero runtime change.** | Java baseline run at session start 983/1-inherited/0/2 (matches documented M1-close baseline; inherited failure is `SystemPromptUserRequestedTiebreakerTest` attributed to dirty working-tree `system_prompt.txt`, not Sprint 36-attributable). |
| **Grounding floor unchanged** | **YES — design-freeze sprint, zero runtime change.** The grounding-floor diagnostics per `faq_grounding_contract.md` are unchanged. The design doc §4.7 names the prospective Sprint 37 NARROW extension of the grounding floor per human authorization §6 #4, but Sprint 36 ships none of it. | |
| **Wrong-containment rate unchanged or down** | **N/A — design-freeze sprint, no behaviour change.** | |
| **Over-escalation rate unchanged or down** | **N/A — design-freeze sprint, no behaviour change.** | |
| **Architecture-health metrics not regressed** | **No regression** — `new_semantic_hardcode_count` = 0 (no runtime code added); `soft_signal_conversion_count` = 0 (Sprint 39 implements continuity slot, not Sprint 36); `planner_ownership_ratio` unchanged; `shadow_disagreement_rate` not measured (Sprint 36 ships no eval). | per `iteration_governance.md` §6 metric definitions; Sprint 36 dev confirmed zero runtime change. |

**Per §5.5 smoke composite_score demotion** (effective 2026-05-16):
Sprint 36 does not run smoke; the demoted observation is not
applicable.

**Per §5.6 bad-case suite manual review** (effective 2026-05-16):
Sprint 36 does not run the bad-case suite; the deliver-agent +
human review at Sprint 36 close per M2-Skill milestone acceptance
bar evaluates Alice closure-criterion (a) AFTER Sprint 37 ships
the S1 predicate, NOT at Sprint 36 close.

## 12. Closure verdict placeholder

Per `feedback_handoff_verdict_section_delegation.md`, this section
is a placeholder. The deliver-agent + human + Codex per-sub-sprint
review fill in at Sprint 36 close per `docs/sprint_objective.md`
§11 + §12 + `iteration_governance.md` §4.3 trigger #1 + #2.

The deliver-agent dispatches Codex per-sub-sprint review at Sprint
36 close per `docs/sprint_objective.md` §5.2 (deliver-agent owns
`compact/sprint-036-review-prompt.md` drafting + Codex dispatch).
Codex review verdict (per `iteration_governance.md` §4.1 +
`docs/codex-findings.md` sprint-close header per §4.2) is written
to `docs/codex-findings.md` at Sprint 36 close; deliver-agent
archives to `docs/sprints/sprint-036-codex-review.md` at
sub-sprint close.

| field | value |
|-------|-------|
| status | **PLACEHOLDER — filled by deliver-agent at Sprint 36 close** |
| classification | per `iteration_governance.md` §4.1 + §4.2 verdict + classification matrix |
| Codex outcome | per Codex per-sub-sprint review per §4.3 trigger #1 + #2 |
| Tier-0 candidate disposition | per design doc §8.3 + §8.4 — deliver-agent + human evaluate at Sprint 36 close BEFORE Sprint 37 planning per `docs/milestone_objective.md` §10 stop condition #1 |
| Open question disposition | the 8 open questions in §7 above + the design doc §8.5-§8.9 surface for deliver-agent + human picks; Sprint 37 / 38 / 39 planning rounds carry the per-sub-sprint picks |
| date | (filled at Sprint 36 close) |
