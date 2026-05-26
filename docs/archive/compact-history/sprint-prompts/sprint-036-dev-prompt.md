Paste the content below this line into a fresh Claude Code session on branch `refactor/remove-the-shackles`.

---

You are the dev agent for Sprint 36 — the first sub-sprint of Milestone M2-Skill (Skill Foundation + UC-Switching Continuity, LLM-led Policy-bounded) per `docs/milestone_objective.md`.

Sprint 36 is a **design-freeze sub-sprint** (Sprint-30-shape): you produce an architectural decision doc + an upstream-proposal frontmatter edit + a handoff. You do NOT change production code, prompts, Java tests, or any eval surface. You ARE producing the contract that Sprint 37 (S1 implementation) + Sprint 38 (S2 implementation) + Sprint 39 (UC-switching wide continuity implementation) ride on.

**Framing.** At dev session close you commit the design doc + handoff. The deliver-agent + human review and decide proceed / fix / Tier-0-escalate at sub-sprint close. If §8 of the design doc surfaces a Tier-0 candidate (e.g., the S1 citation predicate semantics), the deliver-agent + human evaluate at sub-sprint close BEFORE Sprint 37 begins; you do NOT add Tier-0 invariants to `runtime_freeze_and_risk_policy.md` in the dev commit.

**Codex review:** PER-SUB-SPRINT at Sprint 36 close per `iteration_governance.md` §4.3 trigger #1 (possible new Tier-0 candidate) + #2 (§1.7 boundary discussion on hybrid framing). You do NOT dispatch Codex; the deliver-agent + human do at Sprint 36 close.

**Human authorization governs S1 predicate scope.** Per `docs/milestone_objective.md` §6 #4 (2026-05-17): the S1 citation predicate fires ONLY when (a) Phase = RESOLVE_FAQ; (b) Tool call = `record_outcome` with `class=resolve`; (c) Check = `source_id` citation present in user-facing message. NO expansion. Quote this verbatim in your design doc §4 (D3).

## 1. Loader

1. `AGENTS.md` (transitively loads `iteration_governance.md` §1 / §3 / §4 / §5 / §7 / §8 + `doc_governance.md` + `agent_context_guide.md`).
2. `docs/milestone_objective.md` — the M2-Skill milestone north star. Read carefully: §1 (5-sub-sprint layer breakdown + per-sub-sprint Codex review default); §2 goal (6-decision design freeze; S1 grounding-floor predicate is the Codex M1 Finding 1 fix at the deeper layer); §3 Sprint 36 row (scope + hard fences + files in scope); §6 hard fences #1-#19 (esp. #1 §1.7 enforced; #2 §1.4 Runtime-floor only; #4 HARD FENCE INVERSION with human authorization quote — load-bearing for Sprint 36); §7 R-items consumed; §8 per-sub-sprint Codex review plan; §10 stop conditions (esp. #1 Tier-0 candidate surfaced).
3. `docs/sprint_objective.md` — your Sprint 36 contract. §1 sub-sprint class (design-freeze; multi-layer prospective §7 stanza); §2 goal (D1-D6 sub-decisions); §3 non-goals; §4 premise check (10 items); §5 file table + §5.1 design doc structure (8 sections, D1-D6 + walk-through + Tier-0 candidate write-up); §6 hard fences; §8 §7 stanza (Track A + Track B prospective); §9 success metrics; §10 stop conditions.
4. `docs/proposals/skill_orchestration_candidates.md` — Sprint 5 (F2) original proposal. Status `proposal` / `not_started`. Read end-to-end: §2 why scan now (cs_259 + cs_066 anchors); §3 candidate skills (esp. S1 + S2 — these are what M2-Skill promotes); §5 skill / Java guard / prompt boundary table (the conceptual frame your design freeze refines); §7 stop conditions (the design-freeze gate). Your `skill_foundation_design.md` SUPERSEDES this proposal (frontmatter `superseded_by` pointer). DO NOT delete the body — it's the upstream reasoning archive.
5. `docs/milestones/M1_objective.md` §12 closure verdict — M1 closed PASS A-with-Codex-OOSR-classification 2026-05-17. Codex M1 Finding 1 (Alice grounding fabrication) is the R-item Sprint 37 S1 addresses; you frame the S1 design accordingly.
6. `docs/sprints/M1-codex-review.md` — Codex M1 review snapshot. Finding 1 details: Alice trace shows the bot converted generic FAQ article `ka41r000000LIEJAA4` into a specific claim about `AD-2001`'s removal reason. This is the failure shape S1's citation predicate prevents.
7. `docs/sprints/sprint-033-objective.md` + `docs/sprints/sprint-034-objective.md` + `docs/sprints/sprint-035-objective.md` — the most recent sub-sprint contract shapes. Sprint 33 §8 stanza (single-layer prompt_projection); Sprint 35 §8 stanza (eval_spec for diagnostic sprint). Your Track A stanza is closest to Sprint 35's shape (eval_spec / governance for a design-freeze sprint); your Track B stanza is the multi-layer prospective shape per `feedback_multi_layer_prospective_stanza.md`.
8. `docs/sprints/sprint-030-objective.md` + `docs/sprints/sprint-030-handoff.md` — the closest prior Sprint-30-shape design-freeze sprint. Use for design-freeze sprint precedent (file shape + handoff §5-§7 adaptation). NOT a strict template — Sprint 36 has unique features (multi-layer prospective stanza, Tier-0 candidate surfacing path) — but a useful prior reference.
9. `docs/current/iteration_governance.md` — §1.3 (LLM-owned: next action / response strategy — your D1 envelope shape must honor this), §1.4 (Runtime-owned: grounding floor / capability — your D3 + D4 predicates protect this floor), §1.7 (Forbidden — your D6 walk MUST verify no per-UC-branch if-else in skill bodies), §3.2 Q6 (eval_spec layer for governance/design docs), §4.1 nine-question anti-hardcode kernel (you walk this against the proposed design in your §7 D6), §4.3 per-sub-sprint Codex triggers, §5/§5.5/§5.6 acceptance bars (smoke demoted; bad-case suite primary; Sprint 36 ships no behaviour change so neither runs), §7 sprint-objective stanza (your §8 Track A + Track B), §8 milestone framework.
10. `docs/runtime_freeze_and_risk_policy.md` §1/§2 — read to understand the current Tier-0 invariant surface BEFORE evaluating whether D3 / D4 predicates are Tier-0 candidates. You do NOT edit this file; you write up any candidate in `skill_foundation_design.md` §8 with explicit human-review escalation request.
11. `docs/current/faq_grounding_contract.md` — read for D3 context. The current grounding-floor diagnostics define the six output classes + citation diagnostics; your D3 citation predicate aligns with the `L1:source_citation_present` boundary the contract already names.
12. `server/src/main/java/com/gumtree/csagent/service/runtime/PhaseEvaluator.java` — READ-ONLY. Grep the `PhasePlan` construction per phase (DISCOVER / RESOLVE / CONFIRM / ESCALATE / TERMINAL). Note the RESOLVE-FAQ branch + RESOLVE-INTAKE branch shapes (these are where Sprint 37/38 will wire the skill envelope). Note Sprint 33's DISCOVER `systemInstruction` extension at ~lines 411-431 as the principle-level teaching paragraph precedent. Cite line numbers in design doc §2 D1.
13. `server/src/main/java/com/gumtree/csagent/model/PhasePlan.java` — READ-ONLY. Note the existing fields (typically `allowedTools`, `maxToolSteps`, `objective`, `systemInstruction`, `groundingInstruction`). Your D1 proposes how a new envelope attaches; cite the proposed field name + shape in design doc §2 D1.
14. `server/src/main/java/com/gumtree/csagent/service/tools/RecordOutcomeTool.java` — READ-ONLY. Note the existing dispatch + outcome-class enum + any prior validation guards (e.g., `L1:source_citation_present` related code). Cite in design doc §4 D3 — the predicate adds a narrow citation check at the RESOLVE_FAQ + `class=resolve` boundary.
15. `server/src/main/java/com/gumtree/csagent/service/runtime/EscalationReasonResolver.java` — READ-ONLY. Note the existing precedence table + the §E1 (un-earned-Tier-X downgrade) pattern. Cite in design doc §5 D4 — your S2 predicate mirrors §E1 (downgrade `intake_complete_for_uc_X` → `incomplete_intake` when fields incomplete).
16. `server/src/main/java/com/gumtree/csagent/service/runtime/ContextProjectionBuilder.java` — READ-ONLY. Note the existing slots: `alternate_candidate_use_cases` (Sprint 31), `discover_disambiguation_signals` (Sprint 33), `intake_fields_remaining` (Sprint 34). Your D5 proposes a new `prior_use_case_carry` soft signal; cite the existing slot pattern as precedent.
17. `server/src/main/java/com/gumtree/csagent/runtime/UseCaseRegistryService.java` (verify path) — READ-ONLY. Note `UseCaseDefinition.requiredIntakeFields` field (used by D4 S2 predicate). Note FAQ-path UC enumeration (used by D3 S1 trigger).
18. `server/src/main/resources/prompts/system_prompt.txt` — READ-ONLY. Note the existing teaching paragraphs: Sprint 23 `already_called`, Sprint 31 `alternate_candidate_use_cases`, Sprint 33 `discover_disambiguation_signals`. Your D1 + D5 envelope teaching paragraph shape is sibling to these precedents (lands in Sprint 37/38/39 dev sessions, NOT this Sprint 36 design freeze).
19. `eval_interactive/case_specs/bad_cases/alice_uc_a_uc_h_misclass.yaml` — read the Alice bad case shape. Sprint 37 anchors S1 on Alice closure-criterion (a) multi-trace PASS; your D3 design names how the predicate prevents the Codex-rerun fabrication shape.
20. `docs/action_bank.md` — confirm the M2 R-items currently expected: `R-grounding-discipline-iterative-search-fabrication` (M2 Sprint 37 anchor), `R-llm-provider-latency-drift-2026-05-16` (Sprint 40), `R-loosen-topic-uc-binding-llm-owned-drift` (M3-D deferred), `D-hard-citation-gate` (still deferred — your D3 honors the bounded inversion per M2-Skill §6 #4), `D-new-escalation-reason-enum` (still deferred — Sprint 37 sibling-field deferral preserved).
21. `.claude/agent-memory/sprint-deliver-orchestrator/feedback_multi_layer_prospective_stanza.md` — load to understand the Track A + Track B prospective stanza convention for your §8.
22. `.claude/agent-memory/sprint-deliver-orchestrator/feedback_constitution_discipline_vs_planning_anticipation.md` — load to understand why you do NOT edit `iteration_governance.md` / `runtime_freeze_and_risk_policy.md` / `docs/foundational/` / any constitutional doc in the dev commit.

## 2. Premise re-verification

Spot-check at session start (HEAD `eb65e2b` post-M1-close):

1. **M2-Skill milestone objective approved.** `docs/milestone_objective.md` carries the M2-Skill contract with human authorization on §6 #4 (verbatim quote). If the file has been edited since 2026-05-17 in a way that changes scope, STOP and surface.
2. **`docs/proposals/skill_orchestration_candidates.md`** at HEAD — confirm status `proposal` / `implementation_status: not_started`. Confirm S1 + S2 candidate definitions match what M2-Skill §3 Sprint 36 / 37 / 38 cite.
3. **`PhaseEvaluator.java` at HEAD** — confirm RESOLVE-FAQ + RESOLVE-INTAKE branch shapes per Loader §12. If the file has been edited since 2026-05-17 in a way that changes the `PhasePlan` construction approach, STOP and surface.
4. **`PhasePlan.java` at HEAD** — confirm existing field shape per Loader §13.
5. **`RecordOutcomeTool.java` at HEAD** — confirm existing dispatch + outcome-class enum per Loader §14.
6. **`EscalationReasonResolver.java` at HEAD** — confirm existing precedence + §E1 downgrade pattern per Loader §15.
7. **`ContextProjectionBuilder.java` at HEAD** — confirm `alternate_candidate_use_cases` + `discover_disambiguation_signals` + `intake_fields_remaining` slots present per Loader §16.
8. **`UseCaseRegistryService` at HEAD** — confirm `UseCaseDefinition.requiredIntakeFields` field exists for UC-G/H/I/J/K per Loader §17.
9. **`runtime_freeze_and_risk_policy.md` §1/§2** — read the current Tier-0 invariant set. Understand it BEFORE evaluating Tier-0 candidate questions in your D6 walk-through.
10. **Java baseline.** Run `cd server && mvn test -q` (or equivalent). Confirm 983/1-inherited/0/2 baseline holds since M1 close.

If any premise drifts, STOP and surface in handoff §3.

## 3. The work

### 3.1 Read upstream + cite

Re-read `docs/proposals/skill_orchestration_candidates.md` end-to-end. Re-read M1 Codex review (Finding 1 Alice trace evidence). Re-read M2-Skill milestone objective §6 #4 (human authorization quote).

Build your mental model of what the design freeze must produce. The 6 sub-decisions D1-D6 per `docs/sprint_objective.md` §2 are the load-bearing structure; everything you write should map to one of them.

### 3.2 Draft `docs/proposals/skill_foundation_design.md`

Path: `docs/proposals/skill_foundation_design.md`. Front matter:

```yaml
---
title: Skill Foundation Design — Hybrid framing (envelope + Runtime-floor predicate); UC-switching wide continuity invariants
doc_tier: proposal
status: proposal
implementation_status: not_started
source_of_truth: this file
last_reviewed: 2026-05-17
review_cadence: per milestone
supersedes: [docs/proposals/skill_orchestration_candidates.md]
superseded_by: null
notes: >
  Sprint 36 (M2-Skill sub-sprint 1) design-freeze output. Refines
  the Sprint 5 (F2) skill orchestration proposal into a hybrid
  framing locked at M2-Skill approval round 2026-05-17: envelope +
  recommended order via prompt; terminal predicate via Java guard
  for Runtime-owned floor ONLY. Six sub-decisions D1-D6 covered.
  Carries the human authorization (verbatim quote) on the S1
  citation predicate scope per `docs/milestone_objective.md` §6 #4.
  Walks the §4.1 nine-question anti-hardcode kernel against the
  proposed design pre-implementation. §8 surfaces any Tier-0
  candidate question for human-review escalation per M2-Skill §10
  stop condition #1.
---
```

Body section shape (8 sections covering D1-D6 + walk-through + Tier-0 candidate):

**§1 Purpose + relation to upstream**

One paragraph naming the goal (M2-Skill §2 anchor), the relation to the Sprint 5 (F2) original proposal (superseded; reasoning preserved in `skill_orchestration_candidates.md` body), the relation to the M2-Skill milestone objective. CITE the human authorization at M2-Skill §6 #4 (2026-05-17) verbatim — this is load-bearing for D3.

**§2 D1 Skill envelope shape**

Defines what "envelope" means concretely. Cover:

- The proposed addition to `PhasePlan` (cite the existing fields per Loader §13; propose either a new `skillEnvelope` field carrying skill name + tool whitelist + recommended order; OR an overload of existing `objective` + `systemInstruction` fields per Sprint 5 F2 "implement as parametrized PhasePlan inside PhaseEvaluator.plan(...)" guidance). Pick ONE; state the rationale.
- How `PhaseEvaluator.plan(...)` constructs the envelope per phase + per active UC (cite the existing per-phase branch structure in `PhaseEvaluator` from Loader §12).
- How `system_prompt.txt` references the envelope (envelope teaching paragraph shape; sibling to Sprint 23 + 31 + 33 precedent — name the precedent files, cite line numbers).
- Honors §1.7: envelope text contains NO per-UC-branch if-else; it states the envelope at the principle level + names the recommended order as guidance, NOT enforcement.

**§3 D2 Skill terminal predicate shape**

Defines what "terminal predicate" means concretely. Cover:

- Where the predicate code lives (Runtime tool layer per §1.4 grounding-floor / capability-floor ownership). Cite the relevant tool classes (`RecordOutcomeTool.java`, `EscalationReasonResolver.java`).
- How predicate-fail behaves: **downgrade** (analogous to §E1) is default; **rejection** is alternative when downgrade has no safe target; **observation** is OUT OF SCOPE for a predicate.
- The trace-log entry shape per `feedback_deliver_agent_cited_numbers_must_be_reproducible.md` — so Sprint 37/38 trace evidence is reproducible.

**§4 D3 S1 trigger + predicate (Resolve.FAQ.GroundedAnswer)**

**CRITICAL — carry the verbatim human authorization quote from `docs/milestone_objective.md` §6 #4 (2026-05-17):**

> "Accept the Skill-bounded exception to D-hard-citation-gate. This is not a generic Java grounding-citation gate. It is a narrow S1 terminal predicate that only applies when the bot attempts `record_outcome(class=resolve)` inside the `RESOLVE_FAQ` scope, and only checks citation presence as the minimum grounding-floor condition. The original deferral of a generic Java citation gate remains valid."

This authorization governs the predicate scope. Cover:

- S1 trigger conditions per Sprint 5 F2 §3 S1: `current_phase == RESOLVE` AND `active_use_case ∈ FAQ-path UCs` per `UseCaseRegistryService` registry (cite the registry source; do NOT enumerate UC ids hardcoded — name the registry-driven source).
- Predicate scope EXACTLY (per authorization): (a) Phase = `RESOLVE_FAQ`; (b) Tool call = `record_outcome` with `class=resolve`; (c) Check = `source_id` citation present in user-facing message.
- Downgrade behaviour on predicate fail: propose specific design (e.g., downgrade `class=resolve` to a NEW outcome class TBD; OR refuse the tool call with a diagnostic surfaced via projection). The design doc presents options; deliver-agent + human pick at Sprint 36 close.
- Trace-log entry shape.
- EXPLICIT enumeration of what the predicate does NOT do: no enforcement on `class=escalate`; no content-quality judgement of the citation; no fan-out beyond `RESOLVE_FAQ` phase. Per the authorization.

**§5 D4 S2 trigger + predicate (Resolve.Intake.CollectAndHandover)**

Per Sprint 5 F2 §3 S2 + M1 Sprint 34 IntakeFieldExtractor extension. Cover:

- S2 trigger conditions: `current_phase == RESOLVE` AND `active_use_case ∈ {UC-G, UC-H, UC-I, UC-J, UC-K}`.
- Predicate scope: when LLM calls `request_handover(escalation_reason=intake_complete_for_uc_X)` AND `requiredIntakeFields` for active UC are NOT fully populated in `session.intakeFields`, `EscalationReasonResolver` downgrades to `incomplete_intake` (mirrors §E1 un-earned-Tier-X pattern).
- Source for `requiredIntakeFields` (the existing `UseCaseRegistryService.UseCaseDefinition.requiredIntakeFields` field — NO schema change).
- Trace-log entry shape.
- EXPLICIT enumeration of what the predicate does NOT do: no enforcement of which fields are required (registry-driven); no judgment of field content quality (only presence); no fan-out beyond INTAKE-path UCs.

**§6 D5 UC-switching wide continuity invariant matrix**

Per Q3 human pick "Wide: full continuity". Cover:

- Continuity dimensions (rows): `accumulated_tool_results` / `session.customerContext` / `session.intakeFields` / citations-grounding-history / skill-terminal-predicate-state.
- Behaviour-on-switch (columns): survives unconditionally / survives if predicate met / surfaces as soft signal / resets / scoped to current UC only.
- For each cell: state the invariant at the principle level (e.g., "preserve intakeFields whose field key exists in the new active UC's `requiredIntakeFields`" — a registry-level intersection check, NOT a per-UC-pair table).
- The new `prior_use_case_carry` projection slot shape: what fields, what aging behaviour, what visibility to LLM.
- EXPLICIT: invariants are stated as principles + observable-state guards (registry-level intersection check), NOT per-UC-pair branch tables (§1.7 enforced).

**§7 D6 §4.1 anti-hardcode kernel walk-through**

Walk the §4.1 nine questions against the proposed D1-D5 design BEFORE implementation. For each question:
1. State the question (verbatim from `iteration_governance.md` §4.1).
2. State the design's answer.
3. Cite the relevant D1-D5 section.

Surface any §1.7 boundary case + name any Tier-0 candidate question (esp. on D3 / D4 predicate semantics).

If the walk surfaces a §1.7 violation in the proposed design that cannot be cleanly resolved within hybrid framing, **STOP per §10 condition #4** of `docs/sprint_objective.md`. Do NOT commit the design doc with the violation; surface to deliver-agent + human for reframe.

**§8 Open questions / Tier-0 candidate write-up**

List any open question the design surface raises. **CRITICAL**: if D3 / D4 predicate semantics qualify as Tier-0 territory (e.g., the S1 citation predicate enforces a grounding-floor invariant that warrants `runtime_freeze_and_risk_policy.md` §1/§2 codification), write up the candidate explicitly:

- Name the candidate (working id).
- Cite the section of D3 / D4 the candidate originates from.
- State the human-review escalation request: deliver-agent + human evaluate at Sprint 36 close per M2-Skill §10 stop condition #1 whether to add the Tier-0 invariant in a separate human-review-escalation commit BEFORE Sprint 37 begins.

Other open questions: any D3 / D4 / D5 sub-decision where the design doc presents options for deliver-agent + human pick at Sprint 36 close (NOT for Sprint 36 dev to pre-decide). List with rationale + recommended default.

### 3.3 Edit `docs/proposals/skill_orchestration_candidates.md` frontmatter

Frontmatter EDIT ONLY (do NOT touch the body):
- Add `superseded_by: docs/proposals/skill_foundation_design.md`.
- Flip `status: proposal` → `status: superseded`.
- Leave `implementation_status: not_started` (this is a frozen historical record).
- Optionally append to `notes:` a one-line pointer to the Sprint 36 freeze.

The body stays as the upstream reasoning archive per `doc_governance.md` "Forward-looking proposal docs are first-class citizens" — DO NOT delete.

### 3.4 Author the handoff archive

Path: `docs/sprints/sprint-036-handoff.md`. Front matter per Sprint 35 archive shape (adjusted for Sprint 36):

```yaml
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
```

Section ordering and contents per §11 of `docs/sprint_objective.md` (12 sections; §12 is closure-verdict-placeholder, NOT a verdict).

## 4. Self-walk checklist before commit

1. **Design doc authored.** `skill_foundation_design.md` carries all 8 sections + all 6 sub-decisions D1-D6. The human authorization quote on §6 #4 is reproduced verbatim in §4 (D3).
2. **§4.1 walk-through complete** in §7 (D6) of the design doc. Every one of the nine questions has an answer + a cited D1-D5 reference. Surface any §1.7 boundary case explicitly.
3. **Tier-0 candidate evaluation.** §8 of the design doc names any Tier-0 candidate question (esp. D3 / D4 predicate semantics) with explicit human-review escalation request. If NO Tier-0 candidate is surfaced, §8 says so explicitly with rationale.
4. **Upstream proposal frontmatter edited.** `skill_orchestration_candidates.md` frontmatter carries `superseded_by` + `status: superseded`. Body unchanged.
5. **Handoff written.** `sprint-036-handoff.md` follows 12-section shape per `docs/sprint_objective.md` §11. §12 is closure-verdict-placeholder.
6. **Anti-hardcode self-walk for Track A** in handoff §8 (the §4.1 nine questions applied to Sprint 36 itself — the design-freeze sub-sprint that ships zero behaviour change). Expected verdict `approve`.
7. **Track B prospective stanza** in handoff §6 (or §10 layer-classification — adapt to existing handoff section ordering). Names the layers each downstream sub-sprint (Sprint 37 / 38 / 39) will touch + the §7 stanza fields they'll need to fill.
8. **Java baseline unchanged.** Run full server test suite (`cd server && mvn test -q`). 983/1-inherited/0/2 preserved. NO Sprint 36-attributable regression (Sprint 36 ships zero Java change, so any regression is suspect — diagnose before commit).
9. **Files-changed table.** Handoff §9 lists every staged file with paths + counts.
10. **§5 Eval Acceptance bars table.** Handoff §11 walks each bar adapted for a design-freeze sprint (most read "N/A — design-freeze sprint, no behaviour change").
11. **Closure verdict placeholder.** Handoff §12 explicitly defers verdict to deliver-agent + human + Codex per-sub-sprint review. DO NOT write a PASS/FAIL verdict yourself.
12. **No `runtime_freeze_and_risk_policy.md` edit in dev commit.** Any Tier-0 candidate is written up in `skill_foundation_design.md` §8 ONLY; the actual addition (if authorized) is a SEPARATE deliver-agent + human escalation commit per `feedback_commit_at_end_bundles_deliver_artefacts.md`.

## 5. Bundle policy

Single commit with all dev-authored files:
- `docs/proposals/skill_foundation_design.md` (NEW)
- `docs/proposals/skill_orchestration_candidates.md` (frontmatter EDIT only — `superseded_by` + `status: superseded`; body unchanged)
- `docs/sprints/sprint-036-handoff.md` (NEW)

Per `feedback_commit_at_end_bundles_deliver_artefacts.md`: do NOT stage deliver-agent-owned files (`docs/sprint_objective.md` / `docs/milestone_objective.md` / `docs/10-handoff.md` / `docs/action_bank.md` / `docs/codex-findings.md` / `compact/sprint-036-*.md` / `docs/current/iteration_governance.md` / `docs/runtime_freeze_and_risk_policy.md`). Human bundles those at deliver-agent commit.

## 6. Stop conditions (mirroring §10 of sprint_objective)

STOP and report when:

1. **Premise drift on §4 items** (10 premises). Surface in handoff §3.
2. **Tempted to edit any production code** under `server/src/main/`.
3. **Tempted to expand the S1 predicate beyond human-authorized scope** per M2-Skill §6 #4 (e.g., enforce citation on `class=escalate`, enforce content-quality of citation, fan out beyond `RESOLVE_FAQ` phase). The authorization is verbatim; quote it in your design doc §4 (D3) and do NOT broaden.
4. **§4.1 anti-hardcode kernel walk-through (D6) surfaces a §1.7 violation in the proposed design** that cannot be cleanly resolved within the hybrid framing (e.g., the only way to encode S1's recommended order is per-UC-branch if-else; OR D5's UC-switching continuity invariants require a per-UC-pair branch table). Do NOT commit the design doc with the violation; surface to deliver-agent + human for reframe.
5. **D3 / D4 predicate semantics surface a Tier-0 invariant candidate.** DO NOT silently add to `runtime_freeze_and_risk_policy.md` in the dev commit. Write up the candidate in `skill_foundation_design.md` §8 with explicit human-review escalation request.
6. **Tempted to draft Sprint 37 / 38 / 39 implementation contracts or implementation code.** Sprint 36 is the design freeze; implementation is for separate sub-sprint planning rounds with separate deliver-agent + human review.
7. **Tempted to edit `iteration_governance.md`, `doc_governance.md`, `agent_context_guide.md`, or any doc under `docs/foundational/` or `docs/runtime_freeze_and_risk_policy.md`** in the dev commit. Constitutional / foundational edits are out of scope per `feedback_constitution_discipline_vs_planning_anticipation.md`.
8. **Tempted to delete `docs/proposals/skill_orchestration_candidates.md` body** (instead of just flipping frontmatter). The Sprint 5 (F2) reasoning archive remains valuable per `doc_governance.md`.
9. **Tempted to author per-UC-pair branch tables in D5 UC-switching continuity matrix.** D5 invariants are principle-level + observable-state guards (registry-level intersection check); per-UC-pair branches violate §1.7.
10. **Java test regression appears for any reason.** Sprint 36 changes zero Java code; any regression is suspect (test flake, external state). Diagnose before commit.
11. **D6 §4.1 walk-through reveals the proposed design encodes any visible-eval CaseSpec id / trace phrasing** into the freeze contract (Q4 question). The freeze contract names trace shapes + UC names per the registry, NOT specific CaseSpec ids or trace phrasing.

When you STOP, write your STOP rationale in handoff §3 or §7 and surface to the human.

## 7. Handoff document

Per §11 of `docs/sprint_objective.md`: 12-section shape; §12 is closure-verdict-placeholder.

Adapted for a design-freeze sprint:
- §5 reports design-freeze decisions (D1-D6 chosen direction + rationale + cited upstream source), NOT runtime evidence.
- §6 generalization coverage table covers Track A (this sub-sprint) + Track B (prospective for Sprints 37/38/39 per `feedback_multi_layer_prospective_stanza.md`).
- §11 §5 Eval Acceptance bars are adapted: most read "N/A — design-freeze sprint, no behaviour change" with cited rationale.

---

End of Sprint 36 dev prompt.
