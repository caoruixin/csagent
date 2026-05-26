Paste the content below this line into a fresh Codex session after the Sprint 37 dev commit lands. No PR will be opened; review the commit directly.

---

You are the Anti-Hardcode + Sprint-Close Review Agent for Sprint 37 — the first sub-sprint of **NEW Milestone M2 (Skill Registry Abstraction + Wholesale Retroactive Externalization, LLM-led Policy-bounded)** per `docs/milestone_objective.md`.

Sprint 37 is a **design-freeze sub-sprint** (Sprint-30 / Sprint-36 shape; docs-only; NO `server/` code). The dev produced `docs/proposals/skill_registry_design.md` — a 13-section, ~3633-line architectural decision doc that locks 10 design decisions (a)-(j) covering Skill data model, SkillRegistry shape, PhaseEvaluator-as-Skill-Selector integration, procedure-vs-guardrails responsibility split, retroactive migration mappings for all 6 phase YAMLs + Sprint 23/31/33 teaching paragraphs + Sprint 6/7/11 predicates, unified Skill terminal-predicate dispatcher, session-level state model + per-Skill `state_inheritance` semantics, and a §4.1 nine-question kernel walk on the proposed design.

The Sprint 37 commit at `51c327c` (HEAD) stages only the design doc + the 12-section handoff. The substantive review surface is the **proposed design semantics in the design doc**, NOT the Sprint 37 diff (which is docs-only and qualifies for §4.1 per-PR exemption per the Sprint 30 / 36 precedent).

Per `iteration_governance.md` §4.3, Sprint 37 fires per-sub-sprint Codex review per **trigger #1** (Tier-0 candidate surface — the design doc §12 enumerates 5 candidates; C2 + C3 are QUALIFIED with DEFER recommendation; deliver-agent + human pre-decided DEFER for both — Codex independently verifies each verdict + each DEFER recommendation is sound) + **trigger #2** (§1.7 boundary discussion on Skill Registry abstraction shape — Codex verifies the proposed design does NOT smuggle per-UC-branch if-else into any Skill body, and that the §6 #4 verbatim bounded inversion for S1 `must_cite_source` is reproduced word-for-word).

**Human authorization at M2 approval round (2026-05-17, carried forward verbatim from OLD M2-Skill §6 #4) governs the S1 predicate scope** per `docs/milestone_objective.md` §6 #4 (verbatim quote):

> "Accept the Skill-bounded exception to D-hard-citation-gate. This is not a generic Java grounding-citation gate. It is a narrow S1 terminal predicate that only applies when the bot attempts `record_outcome(class=resolve)` inside the `RESOLVE_FAQ` scope, and only checks citation presence as the minimum grounding-floor condition. The original deferral of a generic Java citation gate remains valid."

The design doc §8.2.4 (decision (g) RESOLVE_FAQ predicate row for NEW S1 `must_cite_source`) MUST reproduce this authorization verbatim or with a verbatim quote pointer to `docs/milestone_objective.md` §6 #4. Codex verifies the design doc D3 / §8.2.4 predicate scope matches exactly: (a) Phase = `RESOLVE_FAQ`; (b) Tool call = `record_outcome` with `class=resolve`; (c) Check = `source_id` citation present. Any expansion (citation on `class=escalate/abandon`, content-quality judgement of the citation, fan-out beyond RESOLVE_FAQ) is a BLOCKING finding.

**NEW M2 supersession context (2026-05-17, mid-flight):** The OLD M2-Skill milestone (which Sprint 36 design freeze shipped under) was wholesale superseded by NEW M2 on 2026-05-17 per human direction (the OLD minimum-surface framing preserved the scattered Zhang-Sanfeng pattern that M2 was supposed to fix; NEW M2 promotes Skill to a first-class abstraction with externalized YAML/JSON definitions + SkillRegistry + retroactive migration of ALL 6 phase content + Sprint 23/31/33 teaching paragraphs + Sprint 6/7/11 predicates). The supersession bundle landed in deliver-agent commit `fd7396a` (before Sprint 37 dev commit `51c327c`), and includes: NEW M2 `docs/milestone_objective.md`; NEW Sprint 37 `docs/sprint_objective.md`; OLD `docs/proposals/skill_foundation_design.md` frontmatter flipped to `status: superseded` + `superseded_by: docs/proposals/skill_registry_design.md`; OLD `docs/proposals/skill_orchestration_candidates.md` chained `superseded_by: docs/proposals/skill_foundation_design.md` (chained pointer); OLD M2-Skill milestone archived to `docs/milestones/M2-Skill_objective.md`. **All these housekeeping edits are deliver-agent-owned at fd7396a, NOT in Sprint 37 dev commit at `51c327c`.** Codex verifies the dev commit at `51c327c` touches NEITHER `skill_foundation_design.md` NOR `skill_orchestration_candidates.md` — those edits already landed in `fd7396a`.

**Sprint 37-close pre-decisions (deliver-agent + human, 2026-05-17 post-handoff):** Before Codex dispatch, deliver-agent + human pre-decided the four Sprint-37-close OQs that the dev surfaced in handoff §7 ("decision needed at Sprint 37 close" column). Codex INDEPENDENTLY verifies each pre-decision is sound (see §8 + §9 for the verification surface):

- **OQ-7.2 (C2 — guardrail refusal non-overridability Tier-0 elevation):** **DEFER** per `feedback_constitution_discipline_vs_planning_anticipation.md`. C2's enforcement surface is Sprint 39 (unified dispatcher); Sprint 39 hasn't shipped → no observed runtime evidence; locking Tier-0 prematurely creates fragility. R-item `R-skill-guardrail-non-overridability-tier-0` opened in `docs/action_bank.md` at Sprint 37 close for M3+ revisit (after Sprint 39 evidence observed). Codex independent verdict requested in §8; disagreement → `out_of_scope_review` (the deferral is deliver-agent + human authority per M2 §10 stop condition #1).
- **OQ-7.3 (C3 — state-bus boundary enforcement Tier-0 elevation):** **DEFER** per same rationale; Sprint 41 hasn't shipped → no observed evidence; R-item `R-skill-state-bus-boundary-enforcement-tier-0` opened for M3+ revisit. Codex independent verdict requested in §8; disagreement → `out_of_scope_review`.
- **OQ-7.4 (Tier-0 candidate write-up file convention for QUALIFIED-DEFER candidates C2 + C3):** **NO separate write-up file**; design-doc §12 enumeration is sufficient. Matches Sprint 36 precedent (OLD candidates 7.1 + 7.2 stayed in `skill_foundation_design.md` §8 without separate files). The R-items in `action_bank.md` carry the candidates forward; no foundational-doc edit happens at Sprint 37 close for DEFER. Codex may note any disagreement as informational; not blocking.
- **OQ-7.10 (supersede pattern timing for OLD predecessors):** **No dev revisit**; supersession already correctly applied by deliver-agent at `fd7396a` per Sprint 37 §6 #9 / #10 fence default. Codex verifies in §2 scope-discipline that the dev commit at `51c327c` did NOT re-edit these files.
- **OQ-7.11 (`request_handover` decision tree §4.1 Q6 verdict):** Deliver-agent + human pre-position is **agree with dev (NO if-else; situation-to-enum, NOT per-UC-pair)**. Codex INDEPENDENT verdict REQUIRED — see §3 Q6 below for the verification surface (read `system_prompt.txt:54-101` directly). **If Codex disagrees**, the classification is **`out_of_scope_review` + open new R-item** like `R-system-prompt-handover-decision-tree-shape` for M3+ — NOT `fix_required`. Rationale: the decision tree predates M1; fixing it would be a Sprint 40+ scope change beyond Sprint 37's design-freeze; Sprint 37 close does NOT broaden scope to fix pre-existing surfaces. The §12 expected verdict shape below encodes this routing.

## 1. Loader

Read in this order on a fresh Codex session.

1. **`AGENTS.md`** (transitively loads `docs/current/doc_governance.md` + `docs/current/agent_context_guide.md` + `docs/current/iteration_governance.md` §1 / §3 / §4 / §5 / §5.5 / §5.6 / §7 / §8).
2. **`docs/milestone_objective.md`** — the NEW M2 milestone north star (status `current`; supersedes OLD M2-Skill). Read carefully: §1 5-sub-sprint layer breakdown (Sprint 37/38/39/40/41) + per-sub-sprint Codex review default (all 5 fire triggers); §2 goal (Skill Registry abstraction + wholesale retroactive externalization); §3 sub-sprint sequence (Sprint 37 row carries the 10 design decisions (a)-(j) at the milestone level); §5 acceptance recalibration (Alice + eval = observation, NOT gate for THIS milestone — architecture-focused milestone; this differs from OLD M2-Skill's S1-anchored bar); §6 hard fences (22 items; esp. #1 no per-UC-branch if-else, #4 verbatim S1 authorization carry-forward, #8 Tier-0 escalation, #20 no OLD design deletion); §10 stop conditions (esp. #1 Tier-0 candidate escalation path; #2 §1.7 boundary violation in proposed design).
3. **`docs/sprint_objective.md`** — the Sprint 37 contract. §1 sub-sprint class (design-freeze; docs-only; multi-layer prospective §7 stanza covering Sprints 38/39/40/41); §2 goal (10 design decisions (a)-(j) enumerated with detailed scope per decision); §3 non-goals; §4 premise check (10 items; dev re-verified at session start with one refinement on premise #7); §5 file table + §5.1 design doc structure (13 sections); §6 hard fences (19 items); §8 §7 stanza (PROSPECTIVE multi-layer); §10 stop conditions (12 items); §11 12-section handoff contract; §12 M2 milestone context cross-reference.
4. **`docs/sprints/sprint-037-handoff.md`** — the dev's archive. Compare §9 (Files changed) against §5 of the objective; verify no scope creep. §1.2 lists the code-shape table verified at HEAD `6d97888` with cited line numbers across `PhaseEvaluator.java`, `AgentRunLoopImpl.java`, `ContextProjectionBuilder.java`, `system_prompt.txt`, `RecordOutcomeTool.java`, `EscalationReasonResolver.java`, `IntakeFieldsRegistry.java`, `UseCaseRegistryService.java`, `PhasePlan.java` — Codex spot-checks a sample. §3 carries premise #7 refinement (`system_prompt.txt` is 101 lines, NOT ~40-60 per the contract estimate); the refinement does NOT change Sprint 37 scope but is load-bearing for Sprint 40 dev judgement. §4 walks the 13-section design doc (table). §5 summarizes the §4.1 walk verdicts per question. §6 carries the 5-candidate Tier-0 enumeration outcome (no write-up file shipped — all candidates DEFER or NOT-A-CANDIDATE). §7 lists 11 open questions (OQ-7.2/7.3/7.4/7.10/7.11 need Sprint 37-close decisions; rest defer to downstream sub-sprint planning). §8 walks §4.1 on Sprint 37 itself (verdict `approve` — docs-only exemption). §10 names Sprint 37 layer (docs/design only) + PROSPECTIVE coverage for Sprints 38-41. §11 adapts §5 acceptance bars for design-freeze + M2 §5 recalibration. §12 placeholder for closure verdict.
5. **`docs/proposals/skill_registry_design.md`** — **the substantive review surface.** Read end-to-end (13 sections; 3633 lines). §1 Background + relation to upstream (esp. §1.2 relation to OLD Sprint 36 freeze — what carries forward, what is rejected; §1.3 relation to Sprint 5 (F2) original proposal; §1.5 the research-agent proposal verbatim quote — load-bearing architectural input); §2 decision (a) Skill data model (12 fields + JSON Schema validation contract); §3 decision (b) SkillRegistry shape (Java class location; `select(phase, useCase)` semantics; SkillLoader fail-fast); §4 decision (c) PhaseEvaluator integration (composeSkillPhasePlan helper; backward-compatible PhasePlan data shape; intermediate state during migration); §5 decision (d) procedure vs guardrails split (declarative DSL for guardrails; content placement rules); §6 decision (e) per-phase YAML migration mapping (the largest section ~710 lines; 6 sub-rows for DISCOVER + CONFIRM + CLOSE/TERMINAL + ESCALATE + RESOLVE_INTAKE + RESOLVE_FAQ); §7 decision (f) Sprint 23/31/33 teaching paragraph migration; §8 decision (g) Sprint 6/7/11 predicate migration + NEW S1 `must_cite_source` (§8.2.4 — the verbatim §6 #4 reproduction is HERE) + NEW S2 `intake_complete_required` (§8.2.5); §9 decision (h) unified Skill terminal-predicate dispatcher (composition order; per-tool-call dispatch site; failure-mode trace; Tier-0 candidate question §9.9 — C2); §10 decision (i) session-level state model + state_inheritance (Tier-0 candidate question §10.10 — C3; NEW `prior_use_case_carry` projection slot §10.4; UC-switching invariant matrix §10.5 carried from OLD Sprint 36 D5); §11 decision (j) §4.1 nine-question walk on PROPOSED design (per-Q verdict + reasoning at §11.1-§11.9; walk verdict §11.10 `approve`); §12 Tier-0 candidate enumeration (5 candidates C1-C5 with per-candidate verdict at §12.1-§12.5; aggregate verdict §12.6); §13 downstream sub-sprint reference index + premise refinements + 11 OQs + self-walk.
6. **`docs/proposals/skill_foundation_design.md`** — OLD Sprint 36 design freeze. Verify frontmatter at HEAD carries `status: superseded` + `superseded_by: docs/proposals/skill_registry_design.md` (the supersession was applied by deliver-agent at commit `fd7396a`, BEFORE Sprint 37 dev commit `51c327c`). Verify the file body was NOT edited in `51c327c`. Load §6 #4 (verbatim S1 authorization) + §1.2 (MATERIAL FINDING on Sprint 6/7/11 predicates already shipped) — both carry forward into NEW Sprint 37 (the verbatim authorization is reproduced in NEW M2 §6 #4; the MATERIAL FINDING informs design doc §8 decision (g)).
7. **`docs/proposals/skill_orchestration_candidates.md`** — Sprint 5 (F2) original proposal. Verify frontmatter at HEAD carries chained `superseded_by: docs/proposals/skill_foundation_design.md` + `status: superseded` (applied by deliver-agent at fd7396a per the chained pattern). Verify the body was NOT edited in `51c327c`. Body explicit recommendation §6 "Do NOT introduce a new skill runtime framework. Implement S1 as a parametrized PhasePlan inside PhaseEvaluator.plan(...)." — NEW M2 reinterprets the "do not introduce framework" line per the research-agent proposal (the constraint was a 2024 prudence; the Skill Registry's externalization addresses the original concern by KEEPING PhaseEvaluator-as-shell + EXTERNALIZING the per-phase content). Context for design doc §1.3.
8. **`docs/milestones/M1_objective.md`** §12 closure verdict + **`docs/sprints/M1-codex-review.md`** Finding 1 — context for NEW S1 citation predicate's anchor (the Alice multi-trace fabrication shape on Codex independent rerun at M1 close; opened new R-item `R-grounding-discipline-iterative-search-fabrication` as the M2 anchor; NEW S1 predicate is the deeper-layer fix).
9. **`docs/current/iteration_governance.md`** — §1.3 (LLM-owned: user goal / issue relation / UC hypothesis / drift / topic shift / next action / escalation posture / response strategy / natural customer-facing wording — verify design doc D1 envelope is teaching, not enforcement); §1.4 (Runtime-owned: tool schema / capability / permission / PII / safety floor / grounding floor / budget / timeout / idempotency / persistence / trace and eval contract — verify design doc D2 / D3 / D4 predicates fit within this floor); §1.7 (Forbidden — verify design doc has NO per-UC-branch if-else across (a)-(i)); §3.2 Q3 (`prompt_projection` layer for downstream Sprints 38/40/41) + Q4 (`skill_state` layer for downstream Sprint 38/41) + Q6 (`eval_spec` layer for design-freeze sub-sprint Sprint 37); §4.1 nine-question kernel; §4.3 per-sub-sprint Codex triggers (Sprint 37 fires #1 + #2); §5 / §5.5 / §5.6 acceptance bars (Sprint 37 + M2 §5 recalibration: bad-case suite OBSERVATION not gate for THIS milestone); §7 sprint-objective stanza (multi-layer prospective per `feedback_multi_layer_prospective_stanza.md`); §8 milestone framework.
10. **`docs/runtime_freeze_and_risk_policy.md`** §1 / §2 — read the current Tier-0 invariant set BEFORE evaluating the design doc §12 Tier-0 candidate write-ups. The Tier-0 catalogue is the load-bearing reference for §12 candidate qualification.
11. **`docs/current/faq_grounding_contract.md`** — grounding-floor contract; `L1:source_citation_present` is the boundary the NEW S1 `must_cite_source` guardrail extends per design doc §8.2.4 + §6 #4 verbatim authorization.
12. **`compact/sprint-037-dev-prompt.md`** — what the dev was authorized to do vs what landed (cross-check for in-scope vs out-of-scope edits).
13. **Code source files for cited-line spot-checking** (read on demand during §5 + §6 verification, NOT end-to-end):
    - `server/src/main/java/com/gumtree/csagent/service/runtime/PhaseEvaluator.java` (1628 lines; INTAKE_UCS at line 30; phase dispatch switch at 355-360; per-phase branches at DISCOVER 397-457 / CONFIRM 462-487 / CLOSE 492-509 / ESCALATE 513-542 / RESOLVE-INTAKE 552-596 / RESOLVE-FAQ 598-665).
    - `server/src/main/java/com/gumtree/csagent/service/runtime/AgentRunLoopImpl.java` (787 lines; FAQ_PATH_UCS at lines 106-108; INTAKE_COMPLETE_GUARD_REJECT_REASON at line 120; intake dispatch at 317; premature-resolve dispatch at 356; faq-miss dispatch at 413; `shouldRejectIncompleteIntakeHandover` at 628-651 [Sprint 7 §I2]; `shouldRejectFaqMissHandover` at 690-734 [Sprint 6 §G2]; `shouldRejectPrematureResolveOutcome` at 745-759 [Sprint 11 §M1]).
    - `server/src/main/java/com/gumtree/csagent/service/runtime/IntakeFieldsRegistry.java` (222 lines; `REQUIRED_FIELDS_BY_UC` at line 53+; `isIntakeUseCase` at 110-112; `requiredFieldsFor` at 118-121; `canonicalFieldName` at 127-131; `fieldsRemaining` at 167-178; `intakeComplete` at 184-193). **Source of truth for per-UC intake fields** (NOT `UseCaseDefinition.requiredIntakeFields` per the OLD Sprint 36 §8.1 premise-name correction; the Sprint 37 design doc D4 / §8.2.5 cites IntakeFieldsRegistry).
    - `server/src/main/java/com/gumtree/csagent/service/runtime/UseCaseRegistryService.java` (174 lines; `UseCaseDefinition` record at lines 166-173 with 6 fields: `ucId / name / topicSubjects / riskLevel / allowBotResolution / path`; NO `requiredIntakeFields` field — verifies the premise-name refinement).
    - `server/src/main/java/com/gumtree/csagent/service/runtime/ContextProjectionBuilder.java` (1161 lines; `intake_state` slot at 346-377; `alternate_candidate_use_cases` at 396-416; `discover_disambiguation_signals` at 418-432; `already_called` at 704+).
    - `server/src/main/resources/prompts/system_prompt.txt` (**101 lines** per premise #7 refinement; Sprint 23 `already_called` at 23-28; Sprint 31 `alternate_candidate_use_cases` at 30-34; Sprint 33 `discover_disambiguation_signals` at 36-43; DISCOVER phase guidance at 45-52; `request_handover` decision tree at 54-101 — the OQ-7.11 surface).
    - `server/src/main/java/com/gumtree/csagent/service/tools/RecordOutcomeTool.java` (132 lines; `execute` at 36-94; `normalizeOutcomeClass` at 110-121 — canonical `resolve / escalate / abandon` enum; NEW S1 predicate Sprint 39 fires at this dispatch site).
    - `server/src/main/java/com/gumtree/csagent/service/runtime/EscalationReasonResolver.java` (384 lines; PRIORITY map at 84-103; resolve precedence at 232-247; canonicalize at 256-285).
    - `server/src/main/java/com/gumtree/csagent/model/PhasePlan.java` (146 lines; backward-compatible per design doc decision (c) §4.2).
    - `server/src/main/resources/skills/` — verify directory does NOT exist at HEAD `51c327c` (Sprint 38 creates on first migration; this is a sanity check on Sprint 37 scope discipline).

## 2. Scope-discipline gate (BLOCKING)

Verify the dev commit at `51c327c` touches ONLY the following surfaces:

- `docs/proposals/skill_registry_design.md` (NEW; the 13-section design doc; 3633 lines)
- `docs/sprints/sprint-037-handoff.md` (NEW; 12-section dev archive)

Run `git diff 6d97888..51c327c --stat` to enumerate touched files; confirm exactly two files added; zero files modified; zero files deleted.

Any of the following in the dev commit at `51c327c` is a **BLOCKING scope violation**:

- ANY file under `server/src/main/**` (Sprint 37 must NOT touch production code; M2 §6 #1-#3 / Sprint 37 §6 #1 fence).
- ANY file under `server/src/test/**` (Sprint 37 must NOT touch test code).
- `server/src/main/resources/prompts/system_prompt.txt` (no prompt edit; Sprint 23/31/33 teaching paragraph extraction is Sprint 40 scope).
- ANY file under `server/src/main/resources/skills/` (no Skill YAML creation; Sprint 38 onward scope; directory should not exist post-Sprint-37).
- ANY file under `eval_interactive/` (no eval surface edit; M2 §6 #9 / #10 / #11 / #13 cascade fences).
- ANY file under `docs/foundational/` (no foundational doc edit; M2 §6 #15 / Sprint 37 §6 #3 fences).
- `docs/current/iteration_governance.md` OR `docs/current/doc_governance.md` OR `docs/current/agent_context_guide.md` (constitution-discipline; M2 §6 #12 / Sprint 37 §6 #2 fences).
- `docs/runtime_freeze_and_risk_policy.md` (no Tier-0 edit in Sprint 37 dev commit; M2 §6 #8 / Sprint 37 §6 #2 / Sprint 37 §6 #19 fences. Tier-0 candidate write-up lives in `docs/proposals/skill_registry_design.md` §12 ONLY — not in the foundational doc; if any candidate is QUALIFIED-AND-ELEVATE at Sprint 37 close, the foundational-doc edit is a SEPARATE deliver-agent commit AFTER Sprint 37 close, not in `51c327c`).
- ANY file under `docs/sprints/sprint-001-*` through `docs/sprints/sprint-036-*` (no archive edit; M2 §6 #14 / Sprint 37 §6 #4 fences).
- ANY file under `docs/milestones/` (M1 archive immutable per M2 §6 #16; OLD M2-Skill archive at `M2-Skill_objective.md` is deliver-agent-owned per Sprint 37 §6 #5 fence; both must be untouched in `51c327c`).
- `docs/sprint_objective.md` (deliver-agent-owned per Sprint 37 §6 #6).
- `docs/milestone_objective.md` (deliver-agent-owned per Sprint 37 §6 #6).
- `docs/10-handoff.md` (deliver-agent-owned per Sprint 37 §5.1 / §6 #6).
- `docs/action_bank.md` (deliver-agent-owned at sub-sprint close per Sprint 37 §6 #7; `feedback_commit_at_end_bundles_deliver_artefacts.md`).
- `docs/codex-findings.md` (Codex writes at Sprint 37 close per Sprint 37 §6 #8; deliver-agent dispatches; you own this file as part of the review verdict — verify dev commit at `51c327c` did NOT write here).
- `compact/sprint-037-dev-prompt.md` OR `compact/sprint-037-review-prompt.md` (deliver-agent-owned per Sprint 37 §6 #6).
- `docs/proposals/skill_foundation_design.md` (supersession edit was applied by deliver-agent at `fd7396a` per M2 §6 #20 + Sprint 37 §6 #9 fences; verify NO further edit in `51c327c`).
- `docs/proposals/skill_orchestration_candidates.md` (chained supersession edit was applied by deliver-agent at `fd7396a` per Sprint 37 §6 #10 fence; verify NO further edit in `51c327c`).
- NEW Tier-0 candidate write-up file (e.g., `docs/proposals/tier0_candidate_<name>.md`) — Sprint 37 §5 makes this CONDITIONAL on a candidate being QUALIFIED-AND-ELEVATE; per handoff §6 + design doc §12, no candidate is QUALIFIED-AND-ELEVATE (C2 + C3 are QUALIFIED-DEFER; C1 REJECTED; C4 + C5 NOT-A-CANDIDATE); therefore NO write-up file is expected. If the dev shipped one anyway, it is NOT a scope violation per Sprint 37 §5 (the contract permits conditional NEW file) — BUT verify the candidate qualification reasoning in the file is honest + matches the design doc §12 enumeration. If the dev shipped a write-up file for a QUALIFIED-DEFER candidate (against the dev's own interpretation in handoff §6), note as informational (the deliver-agent + human decide at Sprint 37 close per OQ-7.4).

Surface any scope-discipline failure as **Finding #1** with the diff snippet quoted (run `git show --stat 51c327c` for the full file list).

**ALSO verify** that `docs/proposals/skill_foundation_design.md` frontmatter at HEAD (post-`51c327c`) shows `status: superseded` + `superseded_by: docs/proposals/skill_registry_design.md` — this edit was applied by deliver-agent at `fd7396a` (run `git log --oneline -- docs/proposals/skill_foundation_design.md` to confirm the supersession edit is at `fd7396a` and not at `51c327c`); the file body was unchanged. Similarly for `docs/proposals/skill_orchestration_candidates.md` with chained `superseded_by: docs/proposals/skill_foundation_design.md`. If either frontmatter edit is missing OR appears in `51c327c` (instead of `fd7396a`), surface as a Finding (the deliver-agent's housekeeping order is part of the supersession-bundle convention per `feedback_packaging_codex_findings_supersession.md`).

## 3. §4.1 Anti-Hardcode kernel walk (against the PROPOSED DESIGN in `skill_registry_design.md`, NOT against the Sprint 37 diff)

Sprint 37's diff itself is docs-only and qualifies for §4.1 exemption (the dev's handoff §8 correctly notes this with the one-line exemption verdict `approve — exemption: design-freeze sub-sprint, no semantic surface touched`). The SUBSTANTIVE review is on the design doc's proposed semantics for decisions (a)-(i), which the dev walked in design doc §11. Codex INDEPENDENTLY walks the same nine questions against (a)-(i) and verifies the dev's per-Q verdicts at §11.1-§11.9.

The 10 decisions (a)-(j) at design-doc §-levels:

| Decision | Design doc § | Surface to check for Q1 (per-UC-branch if-else) |
|---|---|---|
| (a) Skill data model | §2 (12 fields + JSON Schema) | Schema field set; per-UC encoded? |
| (b) SkillRegistry shape | §3 (`select(phase, useCase)` semantics) | Lookup vs branch table? |
| (c) PhaseEvaluator integration | §4 (composeSkillPhasePlan helper) | Per-UC if-else introduced? |
| (d) procedure vs guardrails | §5 (declarative DSL for guardrails) | Per-UC encoded in guardrail DSL? |
| (e) 6 phase YAML migration | §6 (per-phase mapping) — **largest section** | Per-UC branch in YAML `procedure` or `guardrails` for any of 6 Skills? |
| (f) Sprint 23/31/33 teaching migration | §7 (per-paragraph) | Per-UC branch in migrated paragraph? Duplication across Skills smuggles if-else? |
| (g) Sprint 6/7/11 + S1/S2 predicate migration | §8 (per-predicate) — **load-bearing** | Expansion of S1 beyond §6 #4 scope? Migration of existing predicates introduces per-UC logic? |
| (h) Unified dispatcher | §9 (composition order; dispatch site routing) | Per-UC branch in dispatcher composition? |
| (i) Session state model + state_inheritance | §10 (per-Skill `state_inheritance` schema; invariant matrix §10.5) | Per-UC-PAIR branch table in invariant matrix? Per-UC in `state_inheritance` declarations? |
| (j) §4.1 walk on proposed design | §11 (dev's walk; verdict §11.10 `approve`) | Walk completeness + verdict honesty |

Walk each of the nine §4.1 questions against decisions (a)-(i) independently:

1. **Q1 keyword / regex / if-else / enum / per-UC matrix for semantic decision?** Walk each decision (a)-(i). For each: verify no proposed surface adds a per-UC-branch decision. The highest-risk surfaces:
   - **(e) 6 phase YAML migration**: each Skill body (`procedure` text + `guardrails` declarations) should be principle-level across UCs in that phase; verify no per-UC body within `discover_triage.yaml`, `confirm.yaml`, `resolve_faq_grounded_answer.yaml`, `resolve_intake_collect_and_handover.yaml`, `escalate.yaml`, `terminal.yaml`.
   - **(f) Sprint 23/31/33 teaching migration**: Sprint 31 `alternate_candidate_use_cases` paragraph already principle-level (Sprint 31 baseline); Sprint 33 `discover_disambiguation_signals` already principle-level. Verify the migration preserves the principle-level form across phase boundary. Per design doc §7.2.2 + OQ-7.6, Sprint 31 paragraph stays in `discover_triage.yaml` ONLY; verify duplication into resolve-side Skills (which would multiply per-UC branches) is NOT proposed.
   - **(g) Sprint 6/7/11 + S1/S2 predicate migration**: predicates are typed (`must_cite_source`, `intake_complete_required`, `faq_miss_handover_threshold`, `premature_resolve_outcome`); verify per-predicate declaration is single-condition, NOT per-UC enum/branch.
   - **(i) Session state + state_inheritance**: `state_inheritance` per-Skill should be principle-level (e.g., `inherit: [customer_context]`, `reset: [intake_fields]`); the §10.5 invariant matrix carried from OLD Sprint 36 D5 §6.3 is registry-driven intersection via `IntakeFieldsRegistry.canonicalFieldName` + `requiredFieldsFor`, NOT a per-UC-PAIR branch table. Verify §10.5 matrix shape preserves the registry-driven framing.
   
   Expected: NO per-UC-branch if-else in any decision (a)-(i). The design doc §11.1 verdict is `NO` per the dev's walk; Codex independently confirms or surfaces dissent.

2. **Q2 Tier-0 justification?** N/A for Q1=NO; Tier-0 candidates enumerated separately in §12 (handled in §8 of this review prompt below). Design doc §11.2 verdict: N/A.

3. **Q3 soft signal achievable?** Mixed PASS / NA verdict per design doc §11.3:
   - **PARTIALLY YES**: state-bus soft-signal dimensions (the NEW `prior_use_case_carry` projection slot §10.4 IS the soft-signal alternative for cross-Skill state visibility; rides on Sprint 31/33 precedent).
   - **PARTIALLY NO**: terminal predicates (D2 / D3 / D4) are §1.4 hard surfaces per `feedback_packaging_codex_findings_supersession.md` (Sprint 6/7/11 precedent) + §6 #4 verbatim authorization; soft-signal alternative is OUT OF SCOPE for terminal predicates (they protect Runtime-owned floor by construction).
   - Verify the design doc D2 §3 (terminal-predicate boundary) names the boundary correctly. Verify §10.4 `prior_use_case_carry` slot is positioned as soft-signal per the Sprint 31/33 precedent.

4. **Q4 visible-eval / trace phrasing / CaseSpec id encoded into runtime/prompt/judge?** Verify design doc does NOT encode any specific CaseSpec id (e.g., `alice_uc_a_uc_h_misclass`) or trace phrasing into the freeze contract. Analytical references to "the Alice failure shape" or "M1 Codex Finding 1" are PROSE; they do NOT feed runtime/prompt/judge. Design doc §11.4 verdict: NO. Codex confirms.

5. **Q5 semantic ownership shift LLM → Java?** Verify design doc:
   - D1 envelope (Skill `procedure`) is TEACHING — LLM owns recommended-order deviation per §1.3.
   - D2 / D3 / D4 / D-state-inheritance enforce Runtime-owned §1.4 floor ONLY.
   - LLM retains §1.3 ownership: UC hypothesis, next action, escalation posture, response strategy, customer-facing language, per-step argument choice.
   Design doc §11.5 verdict: NO. Codex confirms.

6. **Q6 prompt as if-else dump?** Verify:
   - Skill `procedure` paragraphs proposed for migration (Sprint 38/39/40 ship in YAML; Sprint 40 reorganizes `system_prompt.txt`) are principle-level adjacent to existing Sprint 23 / 31 / 33 precedents.
   - **OQ-7.11 (Codex independent verification REQUIRED at Sprint 37 close)**: the `system_prompt.txt:54-101` `request_handover` decision tree (USER-EXPLICIT REQUESTS → ... → INTAKE COMPLETION → BOT LIMITS → ...) stays in the orchestration shell per design doc §7.3 + decision (f) §7.1. The decision-tree shape is a SCOPED LLM-DECISION-WALK across the 23-value `escalation_reason` enum. Is this an unflagged "if-else in prompt" per §4.1 Q6? The dev's design doc §11.6 verdict: NO (the decision-tree maps situation-to-enum, NOT per-UC-pair; the enum is `escalation_reason`-valued, NOT UC-valued; the walk teaches the LLM how to choose the right escalation reason given observable situation, which is §1.3 LLM-owned semantic decision support). **Codex independently walks this decision tree text (read `system_prompt.txt:54-101` directly) + judges**: (a) does the walk encode any per-UC-branch ("if UC == UC-X then escalate as reason Y") that would be a §1.7 violation? (b) is the walk teaching at PRINCIPLE level (situation-to-enum) per the dev's framing? (c) does keeping the walk in shell vs moving to Skill make a difference for §4.1 Q6 compliance? Surface verdict (`agree with dev — NO if-else in prompt` OR `disagree — IS an if-else dump`) + reasoning.

7. **Q7 tool schema / capability / PII / grounding floor preserved?** Verify:
   - **Tool schema unchanged**: Skill `tools_required` is a whitelist composed into `PhasePlan.allowedTools`; the tool dispatch path is unchanged. Existing tool schemas are unchanged.
   - **Capability / permission boundary preserved**: `PhasePlan.allowedTools` enforcement is unchanged (the existing `ToolDispatcher.validateAgainstPlan` continues to enforce the whitelist).
   - **PII / safety floor unchanged**: existing Tier-0 safety invariants in `runtime_freeze_and_risk_policy.md` §1 / §2 remain green; no new PII surface.
   - **Grounding floor**: NARROWLY extended per design doc §11.7 + §8.2.4 — the NEW S1 `must_cite_source` predicate is a Skill-bounded extension of `faq_grounding_contract.md` `L1:source_citation_present`. The extension scope is governed by §6 #4 verbatim authorization (cited verbatim in design doc §8.2.4 OR with verbatim quote pointer to milestone_objective §6 #4). **Codex verifies (load-bearing per §1.7 boundary check below)**: the predicate scope is EXACTLY (a) RESOLVE_FAQ phase; (b) `record_outcome(class=resolve)`; (c) `source_id` citation present. Any expansion is a BLOCKING violation (handled in §4 boundary check below).
   - The generic `D-hard-citation-gate` deferral remains preserved (the inversion is bounded; the generic deferral is NOT reverted).
   Design doc §11.7 verdict: YES with the principled narrow extension. Codex confirms.

8. **Q8 generalization coverage?** Sprint 37 itself N/A (design-freeze; zero behaviour change). Sprint 38 / 39 / 40 / 41 PROSPECTIVE coverage in §8 stanza of Sprint 37 objective:
   - Sprint 38: behavioural-equivalence on 4 simpler phase Skills (DISCOVER + CONFIRM + ESCALATE + TERMINAL) — pre/post-migration `PhasePlan` observationally identical for representative UCs.
   - Sprint 39: behavioural-equivalence on RESOLVE_FAQ + RESOLVE_INTAKE Skills + Sprint 6/7/11 predicate semantics preserved + NEW S1 / S2 predicate negative-control coverage (S1 does NOT fire on `class=escalate/abandon`; S2 does NOT fire when intake complete).
   - Sprint 40: behavioural-equivalence on Sprint 23 / 31 / 33 teaching paragraph migration — LLM still observes teaching content via Skill envelope projection equivalent to monolithic `system_prompt.txt`.
   - Sprint 41: UC switch + state preservation behavioural evidence on real-LLM traces (qualitative per M2 §5 recalibration; not metric).
   Verify the PROSPECTIVE coverage is internally consistent (no contradictions across sub-sprints; the Sprint 38 → Sprint 39 migration order makes sense given complex resolution paths concentrate at Sprint 39).

9. **Q9 rollback / sunset?** Skill Registry abstraction is permanent; no sunset needed. The design freeze itself is the contract; if M2 close evidence shows the foundation is wrong, M3 fold-back addresses (per M2 §11 cross-milestone sequencing). Design doc §11.9 verdict: N/A. Codex confirms.

Expected verdict on the §4.1 kernel walked against the PROPOSED DESIGN: **approve** (with the 5 Tier-0 candidate questions surfaced in §12 + the OQ-7.11 decision-tree question for Codex independent verification at Q6; the dev's recommendation across the board is DEFER on Tier-0 + NO if-else in prompt on OQ-7.11; Codex independently verifies each verdict is sound).

## 4. §1.7 boundary check on the proposed design (BLOCKING if a violation is found)

§1.7 forbidden list applies to the PROPOSED DESIGN, not the Sprint 37 diff. Specifically verify, by reading the design doc §2-§10 (decisions a-i) + §11 walk + §12 enumeration:

- **D1 envelope** (Skill `procedure` text per decision (a) §2 + decisions (e) §6 + (f) §7) does NOT encode "raw eval phrases into Java or prompt" — the migrated paragraphs proposed for `system_prompt.txt` reorganization (Sprint 40 ships) MUST stay principle-level (sibling to Sprint 23 `already_called` / Sprint 31 `alternate_candidate_use_cases` / Sprint 33 `discover_disambiguation_signals` teaching paragraphs). Verify proposed migrated content carries no verbatim CaseSpec quotes, no specific trace text, no per-UC enumerated examples.

- **D2 / D3 / D4 predicates** (Skill `guardrails` per decision (d) §5 + decisions (g) §8) do NOT use "the prompt as an if-else rule dump" — predicates live in Java (Skill `guardrails` declarations consumed by the unified dispatcher §9). The envelope teaching paragraph (Skill `procedure`) mentions the recommended sequence as guidance only, NOT as an enforcement rule list. Verify no design doc surface (especially in §5.2 guardrail declaration schema) embeds free-form per-UC code into a `procedure` text or a `guardrails` declaration body.

- **D3 NEW S1 `must_cite_source` predicate scope** (decision (g) §8.2.4) is NARROW per the human authorization. Verify by reading design doc §8.2.4 verbatim:
  - (a) Phase = `RESOLVE_FAQ`.
  - (b) Tool call = `record_outcome` with `class=resolve`.
  - (c) Check = `source_id` citation present.
  - The §6 #4 verbatim authorization quote is reproduced word-for-word OR with a verbatim quote pointer to `docs/milestone_objective.md` §6 #4.
  - Any expansion in the design doc (citation on `class=escalate/abandon`, content-quality judgement, fan-out to other phases, fan-out to other tool calls) is a **BLOCKING §1.7 + governance violation**.

- **D4 NEW S2 `intake_complete_required` predicate** (decision (g) §8.2.5) does NOT widen `escalation_reason` enum (`D-new-escalation-reason-enum` deferral preserved per M2 §6 #7). The predicate downgrades to the existing canonical reason `incomplete_intake` (already in the enum at `EscalationReasonResolver.java:84-103`). Verify design doc §8.2.5 does NOT propose any new enum value.

- **D-existing-Sprint-6/7/11 migration** (decision (g) §8.2.1 through §8.2.3) preserves predicate semantics — the migration is mechanical relocation from Java method to Skill `guardrails` declaration consumed by the unified dispatcher. Verify the design doc §8.2.1-§8.2.3 does NOT expand any existing predicate's scope (e.g., does not generalize Sprint 6 `shouldRejectFaqMissHandover` from the FAQ_PATH_UCS scope to all phases; does not generalize Sprint 7 `shouldRejectIncompleteIntakeHandover` beyond the existing UCs in `IntakeFieldsRegistry.REQUIRED_FIELDS_BY_UC`; does not generalize Sprint 11 `shouldRejectPrematureResolveOutcome` beyond the existing dispatch).

- **D5 UC-switching continuity invariants** (decision (i) §10.5 — the invariant matrix carried forward from OLD Sprint 36 D5 §6.3) are NOT per-UC-PAIR if-else. Verify §10.5 matrix is principle-level (one rule per state dimension; per-UC variation enters only via registry-level intersection on `intakeFields` per `IntakeFieldsRegistry.canonicalFieldName` + `requiredFieldsFor`). A per-UC-PAIR branch table would be a §1.7 violation. **Load-bearing surface** — this is the most likely place a per-UC-pair table could sneak in.

- **D-state_inheritance per-Skill declarations** (decision (i) §10.2) are principle-level. Verify the schema (`inherit: [...]`, `reset: [...]`, `soft_signal_via_projection: [...]`) operates at state-dimension granularity, NOT per-UC-pair granularity. Any per-UC-pair declaration in a Skill's `state_inheritance` block (e.g., `if prior_uc == UC-A and new_uc == UC-C then inherit X`) would be a §1.7 violation.

- **D-dispatcher composition order** (decision (h) §9.2) does NOT use per-UC routing — the dispatch site routing §9.3 routes by tool name + outcome class, NOT by UC. Verify §9.3 routing logic.

- **D-no per-UC YAML in 6 Skill bodies** (decision (e) §6, per-phase §6.2.1-§6.2.6) — the 6 Skill YAML drafts in §6 should each be principle-level for the UCs in that phase scope. Verify by reading each `procedure` text and `guardrails` declaration in §6.2.1-§6.2.6 for per-UC-branch encoding.

If ANY proposed design surface encodes a §1.7 violation, raise as a **BLOCKING finding** and recommend `fix_required` for design doc revision.

## 5. Hard-fence verification

- **Sprint 37 contract §6 hard fences (19 items):** verify each is honored in the dev commit at `51c327c`. Especially:
  - #1 No `server/` code change (no `.java` / `.py` / `.yml` / `.yaml` / `.properties` under `server/` / `eval_interactive/`).
  - #2 No `iteration_governance.md` / `doc_governance.md` / `agent_context_guide.md` / `runtime_freeze_and_risk_policy.md` edit (constitution-discipline).
  - #3 No edit to other docs under `docs/foundational/` or `docs/current/`.
  - #4 No edits to sprint archives under `docs/sprints/sprint-001-*` through `sprint-036-*`.
  - #5 No edits to milestone archives under `docs/milestones/`.
  - #6 No edits to deliver-agent-owned files (`docs/milestone_objective.md`, `docs/sprint_objective.md`, `docs/10-handoff.md`, `docs/codex-findings.md`, `compact/sprint-037-*-prompt.md`).
  - #7 No edits to `docs/action_bank.md` in dev session.
  - #8 No edits to `docs/codex-findings.md` in dev session.
  - #9 No edits to `docs/proposals/skill_foundation_design.md` (supersession applied by deliver-agent at `fd7396a`, not Sprint 37 dev).
  - #10 No edits to `docs/proposals/skill_orchestration_candidates.md` (chained supersession applied by deliver-agent at `fd7396a`).
  - #11-#13 No edits to existing case families / shadow CaseSpecs / harness.
  - #14 No edits to `eval_interactive/case_specs/bad_cases/alice_uc_a_uc_h_misclass.yaml` (Alice observation per M2 §5 recalibration).
  - #15 No widening of `eval_interactive/case_spec_overrides.yaml`.
  - #16 No mocked-LLM as primary evidence (Sprint 37 is docs-only).
  - #17 No per-UC-branch if-else in PROPOSED design — verified by §3 + §4 above.
  - #18 No pre-decision of Sprint 38 / 39 / 40 / 41 implementation specifics beyond what (a)-(j) decides.
  - #19 No Tier-0 invariant added to `runtime_freeze_and_risk_policy.md` in Sprint 37 dev commit.

- **M2 milestone-level hard fences (22 items per `docs/milestone_objective.md` §6):** verify the design doc + dev commit honor each. Especially:
  - #1 No per-UC-branch if-else in any Skill body (YAML or Java or prompt) — verified by §3 + §4 above.
  - #2 Skill terminal predicate is Java guard ONLY for Runtime-owned floor per §1.4 — verified by §3 Q7 + §4 boundary check.
  - #3 Skill recommended order is soft prompt guidance, NOT hard enforcement — verified by §3 Q5 + Q6.
  - #4 **HARD FENCE INVERSION on `D-hard-citation-gate`** — verbatim authorization preserved (verified by §4 D3 boundary check). The verbatim quote at design doc §8.2.4 OR with quote pointer to milestone_objective §6 #4. Codex MUST verify the design doc reproduces the authorization correctly and the predicate scope matches exactly.
  - #5 No `RuntimeIntentClassifier.java` / `IntentClassification.java` / `DriftResult.java` / `DriftDetector.java` / `UseCaseRouter.java` / `ClassifyUseCaseTool.java` touch (M3-D Topic↔UC binding loosening deferred).
  - #6 No edit to `INTAKE_UCS` set at `PhaseEvaluator.java:30` (M3-B Single Handover Orchestrator deferred).
  - #7 No `escalation_reason` enum widening at `PhaseEvaluator.java:39-63` (M3-A sibling fields deferred).
  - #8 No Tier-0 invariant added without Sprint 37 freeze pre-authorization + explicit human-review escalation — verified by §8 below (5 candidates DEFER / NOT-A-CANDIDATE).
  - #9-#11 No edits to existing case families / shadow case families / harness — verified by scope-discipline §2.
  - #12 No `iteration_governance.md` / `doc_governance.md` / `agent_context_guide.md` / `runtime_freeze_and_risk_policy.md` edit during M2 — verified by scope-discipline §2. (The §6 #12 EXCEPTION clause for Tier-0 candidate elevation is N/A in Sprint 37 dev commit; deliver-agent at Sprint 37 close handles if any candidate is elevated.)
  - #20 No deletion of OLD `docs/proposals/skill_foundation_design.md` — supersession pattern only (verified by §2 scope-discipline above).
  - #21 No reset / migration of M1-shipped FUNCTIONAL surfaces (IntakeFieldExtractor, Sprint71PartialIntakePersistenceTest, Sprint 32/33 projections) — Sprint 37 ships zero Java change so by construction PASS.
  - #22 No deletion or relocation of `eval_interactive/case_specs/bad_cases/alice_uc_a_uc_h_misclass.yaml` — Sprint 37 ships zero eval surface change so by construction PASS.

- **MATERIAL FINDING verification:** independently verify the OLD Sprint 36 §1.2 / NEW Sprint 37 handoff §1.2 claim that Sprint 6 / Sprint 7 / Sprint 11 partial predicates already ship in `AgentRunLoopImpl.java`. Read the cited line ranges at HEAD `51c327c`:
  - Sprint 6 §G2 `shouldRejectFaqMissHandover` at `AgentRunLoopImpl.java:690-734` (dispatched line 413).
  - Sprint 7 §I2 `shouldRejectIncompleteIntakeHandover` at `AgentRunLoopImpl.java:628-651` (dispatched line 317).
  - Sprint 11 §M1 `shouldRejectPrematureResolveOutcome` at `AgentRunLoopImpl.java:745-759` (dispatched line 356).
  
  Confirm dev's reading is correct. If dev mis-read, raise as **BLOCKING** (design doc §8 decision (g) migration mapping depends on the finding; mis-read invalidates the migration plan).

- **Premise #7 refinement verification (`system_prompt.txt` line count):** independently read `server/src/main/resources/prompts/system_prompt.txt` at HEAD `51c327c`. Verify:
  - File is **101 lines total** (NOT ~40-60 as the Sprint 37 contract §4.7 estimate; the dev surfaced this in handoff §3 premise #7 + design doc §13.2 Refinement 1; the refinement does NOT change Sprint 37 scope but informs Sprint 40 awareness).
  - Sprint 23 `already_called` at lines 23-28 (verified).
  - Sprint 31 `alternate_candidate_use_cases` at lines 30-34 (premise said 30-33; off by 1).
  - Sprint 33 `discover_disambiguation_signals` at lines 36-43 (premise said 36-40; off by 3).
  - Additional content at lines 45-52 (DISCOVER phase guidance) and lines 54-101 (`request_handover` decision tree) — both cross-Skill universal content informing Sprint 40 + OQ-7.11.
  If dev's refinement is wrong (line count or paragraph locations don't match HEAD), raise as **BLOCKING** (the §4.1 Q6 OQ-7.11 verdict depends on accurate reading of the decision-tree content at 54-101).

- **Premise #8 / IntakeFieldsRegistry verification (carried from OLD Sprint 36):** independently verify `UseCaseRegistryService.java` `UseCaseDefinition` record at lines 166-173 has 6 fields (`ucId / name / topicSubjects / riskLevel / allowBotResolution / path`) and does NOT carry `requiredIntakeFields`. Verify `IntakeFieldsRegistry.java` IS the actual source (with `REQUIRED_FIELDS_BY_UC` map at line 53+, `canonicalFieldName` at 127-131, `requiredFieldsFor` at 118-121, `intakeComplete` at 184-193). Confirm design doc decision (g) §8.2.5 (S2 `intake_complete_required`) cites `IntakeFieldsRegistry.intakeComplete` correctly. If dev's correction itself is wrong, raise as **BLOCKING**.

- **Sprint 33 cue/slot split verification (premise refinement #2):** independently verify the Sprint 33 ad-status disambiguation cue is at `PhaseEvaluator.java:432-448` (in the DISCOVER `systemInstruction`) AND the Sprint 33 slot description is at `system_prompt.txt:36-43` (in the system prompt teaching block). The split is preserved in M2 per design doc §13.2 Refinement 2 — Sprint 38 migrates the cue (as part of decision (e) DISCOVER); Sprint 40 migrates the slot description (as part of decision (f)). Both end up in `discover_triage.yaml` `procedure`. Verify the design doc §13.2 + §6.2.1 (DISCOVER mapping) describes the split accurately.

## 6. Schema and reproducibility checks

- **Design doc structure:** verify 13 sections per `docs/sprints/sprint-037-handoff.md` §4 walkthrough table are all present + cover all 10 sub-decisions (a)-(j):
  - §1 Background + relation to upstream
  - §2 Decision (a) — Skill data model
  - §3 Decision (b) — SkillRegistry shape
  - §4 Decision (c) — PhaseEvaluator integration
  - §5 Decision (d) — procedure vs guardrails responsibility split
  - §6 Decision (e) — 6 phase YAML migration mapping
  - §7 Decision (f) — Sprint 23/31/33 teaching paragraph migration
  - §8 Decision (g) — Sprint 6/7/11 + S1/S2 predicate migration (§8.2.4 carries verbatim S1 authorization)
  - §9 Decision (h) — unified Skill terminal-predicate dispatcher
  - §10 Decision (i) — session-level state model + state_inheritance
  - §11 Decision (j) — §4.1 nine-question walk on PROPOSED design (Q1-Q9 at §11.1-§11.9; walk verdict §11.10)
  - §12 Tier-0 candidate enumeration (5 candidates C1-C5 at §12.1-§12.5; aggregate verdict §12.6)
  - §13 Downstream sub-sprint reference index + premise refinements + 11 OQs + self-walk
  
  Verify each decision section (§2-§10) carries the dev-contracted structure: decision statement / rationale / alternatives considered + why rejected / §1.7 boundary check / §1.3 / §1.4 boundary check / downstream sub-sprint reference. Some sections also carry: schema validation contract (§2.2), guardrail declaration schema (§5.2), Tier-0 candidate question (§9.9 and §10.10).

- **Frontmatter on design doc** carries `doc_tier: proposal`, `status: proposal`, `implementation_status: not_started`, `source_of_truth: this file`, `supersedes: [docs/proposals/skill_foundation_design.md, docs/proposals/skill_orchestration_candidates.md]`, `superseded_by: null`, `last_reviewed: <Sprint 37 commit date>`. Verify.

- **Frontmatter on superseded predecessors (already applied by deliver-agent at `fd7396a`, not in `51c327c`):**
  - `docs/proposals/skill_foundation_design.md` carries `status: superseded` + `superseded_by: docs/proposals/skill_registry_design.md`. Verify at HEAD; verify edit landed at `fd7396a` (run `git log --oneline --diff-filter=M -- docs/proposals/skill_foundation_design.md` and check last modification SHA).
  - `docs/proposals/skill_orchestration_candidates.md` carries chained `superseded_by: docs/proposals/skill_foundation_design.md` + `status: superseded`. Verify at HEAD.

- **Handoff structure:** verify 12-section shape per Sprint 37 §11. §12 closure verdict placeholder (NOT filled by dev per `feedback_handoff_verdict_section_delegation.md` carried from OLD Sprint 36).

- **Reproducibility (per `feedback_deliver_agent_cited_numbers_must_be_reproducible.md`):** verify every claim in the design doc + handoff about current code shape cites file:line. Spot-check 5-6 cited file:line ranges; confirm they exist + match the design doc / handoff claim:
  - `PhaseEvaluator.java:30` INTAKE_UCS set.
  - `PhaseEvaluator.java:598-665` RESOLVE-FAQ branch (Sprint 6 §G2 S1 sequence teaching at 633-665).
  - `AgentRunLoopImpl.java:690-734` `shouldRejectFaqMissHandover`.
  - `AgentRunLoopImpl.java:745-759` `shouldRejectPrematureResolveOutcome`.
  - `IntakeFieldsRegistry.java:184-193` `intakeComplete`.
  - `system_prompt.txt:54-101` `request_handover` decision tree (load-bearing for OQ-7.11 Codex verdict).
  - `EscalationReasonResolver.java:84-103` PRIORITY map.

## 7. Validation runs (you re-execute)

From a clean checkout of the dev commit `51c327c`:

- **Java test baseline preservation:**

  ```bash
  cd server && mvn test -q
  ```

  Expected: `Tests run: 983, Failures: 1, Errors: 0, Skipped: 2` (byte-identical to M1 close baseline / post-Sprint-36-close baseline). The 1 inherited failure is `SystemPromptUserRequestedTiebreakerTest.systemPrompt_marksActiveUcTiebreakerExplicitly:53` attributed to dirty working-tree `system_prompt.txt` per M1 Codex review notes + Sprint 36 close (the working-tree dirty state was the pre-existing baseline at M1 close; carries forward unchanged). Any new failure delta is a **BLOCKING finding** (Sprint 37 ships zero Java change).

- **No design-doc YAML loader check needed** — design doc is markdown prose, not a YAML CaseSpec. Skip schema validation.

- **No eval run required** at Sprint 37 close. Sprint 37 ships zero behaviour change.

- **No bad-case suite run required** at Sprint 37 close. Per M2 §5 acceptance recalibration, Alice + bad-case suite are OBSERVATION ONLY for THIS milestone; Sprint 37 is docs-only AND M2 §5 has them as observation. Skip.

## 8. Tier-0 candidate independent verification (Codex-substantive)

The design doc §12 surfaces **5 Tier-0 candidates** (C1-C5). Deliver-agent + human pre-decided per Sprint 37 close authorization:

| Candidate | Statement | Dev verdict (design doc §12) | Deliver-agent + human recommendation |
|---|---|---|---|
| **C1** | Skill tool-whitelist enforcement unconditional | REJECTED as NEW candidate (existing invariant via `PhasePlan.allowedTools` composition + `ToolDispatcher.validateAgainstPlan` enforcement) | Confirm — no action; preserved by construction |
| **C2** | Skill terminal predicate refusal non-overridable by LLM | QUALIFIED → **DEFER** per `feedback_constitution_discipline_vs_planning_anticipation.md` | DEFER; carry as R-item `R-skill-guardrail-non-overridability-tier-0` in `action_bank.md` for M3+ revisit |
| **C3** | Skill `state_inheritance` enforced at session-state-bus boundary | QUALIFIED → **DEFER** | DEFER; carry as R-item `R-skill-state-bus-boundary-enforcement-tier-0` for M3+ revisit |
| **C4** | S1 `must_cite_source` predicate semantics | NOT A CANDIDATE per M2 §6 #4 verbatim bounded inversion (the inversion explicitly bounds S1 to Runtime-floor narrow guard, NOT Tier-0 structural invariant; `D-hard-citation-gate` deferral preserved) | Confirm — no action; the bounded inversion is the principled exception |
| **C5** | S2 `intake_complete_required` predicate semantics | NOT A CANDIDATE per Sprint 7 §I2 precedent (predicate already in production since Sprint 7; M2 only relocates) | Confirm — no action; M2 relocation preserves semantics |

Codex INDEPENDENTLY verifies each verdict + each DEFER recommendation soundness:

- **For C1 (REJECTED as new candidate):** Is the "existing invariant" claim correct? Independently verify by reading `PhasePlan.allowedTools` field declaration at `PhasePlan.java` and tracing the dispatch path:
  - Find the tool dispatch site that consumes `PhasePlan.allowedTools` (likely in `ToolDispatcher.java` or similar; grep `allowedTools` in `server/src/main/java/com/gumtree/csagent/`).
  - Verify the dispatcher rejects any tool call not on the whitelist.
  - If the existing enforcement is sound, the C1 REJECTED verdict is correct (no new Tier-0 needed; the invariant exists by construction). If the existing enforcement is missing or has gaps, surface as a finding (C1 may need to be promoted from REJECTED to QUALIFIED-DEFER OR a separate observation).
  - Expected verdict: REJECTED is sound. Confirm.

- **For C2 (QUALIFIED-DEFER, guardrail refusal non-overridability):** Is DEFER reasonable given:
  - (a) The Skill Registry abstraction has NOT YET SHIPPED — Sprint 39 (which implements the unified dispatcher + Sprint 6/7/11 migration + S1/S2 predicates) hasn't run; observed runtime evidence on dispatcher refusal semantics is needed before deciding Tier-0 permanence.
  - (b) The Skill abstraction enforces refusal non-overridability by construction per §1.4 ownership of Runtime floor (the dispatcher refuses; the LLM cannot override without going outside the Skill envelope, which is barred by `PhasePlan.allowedTools` enforcement).
  - (c) The existing Sprint 6 / 7 / 11 predicates have been in production without Tier-0 status; the migration to the unified dispatcher preserves semantics, so the dispatcher inherits the same non-overridability status as the individual methods.
  - (d) Adding Tier-0 status now (before Sprint 39 ships) is premature commit — locking the dispatcher's refusal semantics as Tier-0 before observing the migrated behaviour creates fragility (if Sprint 39 evidence shows refinement is needed, Tier-0 is hard to retreat).
  Expected verdict: DEFER is sound; revisit at M2 close (or earlier if Sprint 39 evidence warrants). If Codex disagrees (e.g., the dispatcher refusal IS Tier-0 territory even before Sprint 39 ships), surface as a Finding with rationale; this becomes `out_of_scope_review` per §11 expected shape (the deferral decision is deliver-agent + human authority per M2 §10 stop condition #1 + `feedback_constitution_discipline_vs_planning_anticipation.md`).

- **For C3 (QUALIFIED-DEFER, state-bus boundary enforcement):** Is DEFER reasonable given:
  - (a) The state-bus + `state_inheritance` per-Skill declarations have NOT YET SHIPPED — Sprint 41 (which implements `SkillStateBus.java` + per-Skill `state_inheritance` declarations + NEW `prior_use_case_carry` projection slot) hasn't run; observed runtime evidence on cross-Skill state boundary semantics is needed.
  - (b) The state-bus enforcement is a NEW architectural surface not present before M2; locking it as Tier-0 before observation creates fragility.
  - (c) The §10.5 invariant matrix (carried from OLD Sprint 36 D5) uses registry-driven intersection via `IntakeFieldsRegistry.canonicalFieldName` + `requiredFieldsFor` — the enforcement is principle-level, NOT per-UC-pair, so Tier-0 codification would lock a design that hasn't been observed to behave correctly in production.
  Expected verdict: DEFER is sound; revisit at M2 close (or earlier if Sprint 41 evidence warrants). If Codex disagrees, surface as a Finding with rationale.

- **For C4 (NOT A CANDIDATE, S1 `must_cite_source` per §6 #4 bounded inversion):** Is the "not a candidate" framing correct — i.e., does the §6 #4 bounded inversion mean the predicate is NOT Tier-0 territory? Independent verdict:
  - The §6 #4 verbatim authorization explicitly bounds the predicate to Runtime-floor narrow guard scope (Phase = RESOLVE_FAQ; Tool = `record_outcome(class=resolve)`; Check = `source_id` present). This is a NARROW grounding-floor extension per §1.4, NOT a structural cross-cutting invariant that all phases / all tools must respect.
  - Tier-0 invariants (per `runtime_freeze_and_risk_policy.md` §1 / §2) are STRUCTURAL ABSOLUTES (pre-plan reroute; same-UC progressive resolve; ResolveDisposition guard; record_outcome guard; observability/validation; etc.). The S1 predicate is BOUNDED CONTEXTUAL (only fires in S1 RESOLVE_FAQ scope).
  - The `D-hard-citation-gate` deferral remains preserved (no generic Java citation gate); the S1 predicate is the bounded exception per the verbatim authorization, NOT a generic Tier-0 invariant.
  Expected verdict: NOT A CANDIDATE framing is sound. If Codex believes the bounded inversion IS Tier-0 territory (e.g., the S1 predicate IS Tier-0 even with the bounded scope), surface as a Finding — this would be a substantive disagreement with the §6 #4 authorization and could be `out_of_scope_review` (the §6 #4 authorization is human-pre-decided at M2 approval round; Codex's disagreement is informational input to a possible re-approval round at Sprint 37 close).

- **For C5 (NOT A CANDIDATE, S2 `intake_complete_required` per Sprint 7 precedent):** Is the "not a candidate" framing correct — i.e., does Sprint 7 §I2 precedent mean the predicate is NOT Tier-0 territory? Independent verdict:
  - Sprint 7 `shouldRejectIncompleteIntakeHandover` has been in production since Sprint 7 (`AgentRunLoopImpl.java:628-651`); adding Tier-0 status now adds ZERO new architectural protection beyond what's already shipped + tested.
  - The M2 work on S2 is RELOCATION (from `AgentRunLoopImpl.java` method to `resolve_intake_collect_and_handover.yaml` `guardrails` block consumed by the unified dispatcher); semantics preserved; no new enforcement surface.
  - Per Constitution §3.2 Q2 framing, Tier-0 is invoked only when a current invariant is being broken AND no existing one covers it; Sprint 7 §I2 has been the de-facto invariant for the intake-completeness floor; explicit Tier-0 codification is informational but not load-bearing.
  Expected verdict: NOT A CANDIDATE framing is sound. If Codex disagrees, surface as a Finding (would be `out_of_scope_review` per §11 expected shape).

If Codex CONFIRMS all 5 dev verdicts AND all DEFER recommendations are sound: note in the verdict header that Tier-0 elevation is correctly deferred + flag C2 + C3 for re-evaluation at M2 close (after Sprint 39 + Sprint 41 evidence observed).

If Codex DISAGREES with any verdict OR any DEFER recommendation: raise as `out_of_scope_review` (the Tier-0 elevation / deferral decision is deliver-agent + human authority per M2 §10 stop condition #1 + `feedback_constitution_discipline_vs_planning_anticipation.md`; Codex's disagreement is informational input to the human's deliberation, not a Sprint 37 blocker on its own).

## 9. Open question independent verification

The 11 OQs in handoff §7 carry dev-recommended defaults + deliver-agent + human authority. Only **OQ-7.11 (`request_handover` decision tree shape per §4.1 Q6)** requires Codex INDEPENDENT verdict at Sprint 37 close — see §3 Q6 above for the verdict surface.

The other 10 OQs are deliver-agent + human authority OR future-sub-sprint planning round:

| OQ | Subject | Codex action |
|---|---|---|
| 7.1 | CLOSE → TERMINAL phase enum rename (Sprint 38 planning) | Note any disagreement as informational; not blocking |
| 7.2 | C2 Tier-0 elevation | **Pre-decided DEFER** (see header + §8 below). Codex independently verifies DEFER soundness; disagreement → `out_of_scope_review` |
| 7.3 | C3 Tier-0 elevation | **Pre-decided DEFER** (see header + §8 below). Codex independently verifies; disagreement → `out_of_scope_review` |
| 7.4 | Tier-0 candidate write-up file convention | **Pre-decided §12-only** (see header). No separate write-up file at Sprint 37 close. Codex notes any disagreement as informational; not blocking |
| 7.5 | S2 `intake_complete_required` `on_fail` mode (Sprint 39 planning) | Note any disagreement as informational |
| 7.6 | Sprint 31 paragraph duplication scope (Sprint 40/41 planning) | Note any disagreement as informational |
| 7.7 | `prior_use_case_carry` cap=3, aging=4 turns (Sprint 41 planning) | Note any disagreement as informational |
| 7.8 | Dropped-intake-fields trace surface at UC switch (Sprint 41 planning) | Note any disagreement as informational |
| 7.9 | Premise #7 refinement + Sprint 40 `system_prompt.txt:45-52` migration (Sprint 40 planning) | Note any disagreement as informational |
| 7.10 | Supersede pattern timing (Sprint 37 close verify only) | Handled in §2 scope-discipline above |
| **7.11** | **`request_handover` decision tree §4.1 Q6 verdict** | **Codex INDEPENDENT verdict REQUIRED — see §3 Q6 above** |

For OQs other than 7.11, Codex may NOTE any disagreement with the dev-recommended default as informational; not blocking for Sprint 37 close (the picks happen at downstream sub-sprint planning rounds OR at Sprint 37 close per deliver-agent + human authority).

## 10. Deferred / non-blocking observations

- The MATERIAL FINDING (Sprint 6/7/11 partial predicates already ship) is honest dev surfacing (carried from OLD Sprint 36 §1.2); not a Sprint 37 blocker. It changes Sprint 38 + 39 + 40 scoping (Sprint 38 = 4 simpler phases; Sprint 39 = 2 complex phases + predicate migration + S1 + S2 + dispatcher consolidated). Codex notes as informational; deliver-agent + human bake into Sprint 38 / 39 contract drafts at planning rounds.
- The premise #7 refinement (`system_prompt.txt` 101 vs ~40-60 lines) is a mechanical estimate correction; not a Sprint 37 blocker. The refinement informs Sprint 40 dev judgement on lines 45-52 (DISCOVER phase guidance) — migrate into `discover_triage.yaml` (default per OQ-7.9) or compress in shell. Deliver-agent bakes into Sprint 40 contract draft.
- The Sprint 33 cue/slot split (cue at `PhaseEvaluator.java:432-448`; slot at `system_prompt.txt:36-43`) is a verified premise refinement; not a Sprint 37 blocker. Preserved through M2 with Sprint 38 migrating the cue + Sprint 40 migrating the slot.
- The 5 Tier-0 candidates' aggregate verdict per design doc §12.6 is "no separate write-up file shipped because no candidate is QUALIFIED-AND-ELEVATE" — dev's interpretation of Sprint 37 §10 stop condition #2. **OQ-7.4 pre-decided at Sprint 37 close (see header)**: §12 enumeration sufficient; no separate write-up file. Codex may note any disagreement as informational; not blocking.
- C2 + C3 are pre-decided DEFER per `feedback_constitution_discipline_vs_planning_anticipation.md` + Sprint 37 close pre-decision (see header). Codex may note any tension as informational; not blocking. R-items `R-skill-guardrail-non-overridability-tier-0` (C2) + `R-skill-state-bus-boundary-enforcement-tier-0` (C3) opened by deliver-agent in `action_bank.md` at Sprint 37 close.

## 11. Output format (write to `docs/codex-findings.md`)

Replace the file content with the standard §4.2 sprint-close header:

```
## Sprint Review Decision
decision: pass | fix_required | out_of_scope_review
blocking_count: <number>
summary: <one paragraph>

## Review Evidence
<bullet list — review scope (Sprint 37 commit 51c327c against pre-dev HEAD 6d97888; design doc 3633 lines; handoff 12 sections; cited file:line ranges spot-checked at HEAD), what you re-ran (Java baseline `mvn test -q`; cited line ranges spot-checked), what passed, what was independently verified (MATERIAL FINDING on Sprint 6/7/11 predicates + premise #7 system_prompt.txt 101-line refinement + premise #8 IntakeFieldsRegistry source + Sprint 33 cue/slot split)>

## Blocking Findings (if any)
<numbered list; each entry quotes diff snippet OR design-doc paragraph + cited file:line; references back to §2 scope-discipline / §4 §1.7 boundary / §5 hard-fence / §8 Tier-0 / §3 Q6 OQ-7.11>

## Anti-Hardcode Kernel
<nine-question walk against the PROPOSED DESIGN (decisions a-i); each Q with one-line verdict; cross-reference design doc §11 dev verdict; Codex agreement or dissent per Q>

## §1.7 Boundary Check
<the §4 design-doc boundary checks; pass/fail per surface: D1 envelope; D2 / D3 / D4 predicates; D3 NEW S1 scope match to §6 #4 verbatim authorization; D4 NEW S2 enum non-widening; D-existing-Sprint-6/7/11 migration preservation; D5 UC-switching invariant matrix principle-level; D-state_inheritance per-Skill principle-level; D-dispatcher composition; D-no-per-UC-YAML across 6 Skill bodies>

## Hard-Fence Verification
<the §5 contract + milestone fences; pass/fail per fence; MATERIAL FINDING + premise #7 + premise #8 + Sprint 33 cue/slot split verification noted; supersession pattern at fd7396a confirmed not in 51c327c>

## Schema And Reproducibility Checks
<the §6 checks; design doc structure (13 sections) + frontmatter (supersedes pair) + handoff structure (12 sections) + cited file:line spot-checks (5-6 ranges) reproduced; supersession frontmatter on predecessors verified>

## Validation Runs
<the §7 results; `mvn test -q` output; baseline preservation 983/1-inherited/0/2; no new failure delta>

## Tier-0 Candidate Independent Verification
<the §8 walk; CONFIRM or DISAGREE per candidate (C1 REJECTED / C2 QUALIFIED-DEFER / C3 QUALIFIED-DEFER / C4 NOT A CANDIDATE / C5 NOT A CANDIDATE); rationale; flag C2 + C3 for re-evaluation at M2 close after Sprint 39 + Sprint 41 evidence observed>

## OQ Independent Verification
<the §9 walk on OQ-7.11 `request_handover` decision tree §4.1 Q6 verdict; other OQ informational notes if any>

## Deferred / Non-Blocking Notes
<the §10 items>
```

## 12. Expected verdict shape

If all gates pass + design doc honors §6 #4 verbatim authorization + 5 Tier-0 verdicts sound + OQ-7.11 verdict aligns with dev (`NO if-else in prompt`): **`decision: pass / blocking_count: 0`**. The cleanest outcome for a design-freeze sub-sprint where the dev shipped per spec is a single-pass close. Matches Sprint 36's first-pass `pass / 0` precedent for the OLD M2-Skill design freeze.

If §2 scope-discipline fails (e.g., dev commit touched a Java file or a deliver-agent-owned doc): **`decision: fix_required`** with the violating diff snippet quoted as Finding #1.

If the design doc §8.2.4 expands the S1 `must_cite_source` predicate scope beyond the human-authorized scope (citation on `class=escalate/abandon`, content-quality judgement, fan-out beyond RESOLVE_FAQ): **`decision: fix_required`** with the violating design-doc paragraph quoted; cite the verbatim authorization in `docs/milestone_objective.md` §6 #4 as the load-bearing reference.

If the design doc §6 / §7 / §8 / §10 (per-phase YAML migration mapping, teaching paragraph migration, predicate migration, state-inheritance schema) encodes a per-UC-branch if-else (a §1.7 violation in the PROPOSED design per §4): **`decision: fix_required`** — design doc must be revised to remove the violation OR scope must be split.

If the MATERIAL FINDING (Sprint 6/7/11 partial shipping) is wrong (dev mis-read the existing surface): **`decision: fix_required`** — design doc §8 decision (g) framing depends on the finding.

If the premise #7 refinement is wrong (file line count or paragraph locations don't match HEAD): **`decision: fix_required`** — the §4.1 Q6 OQ-7.11 verdict depends on accurate reading.

If the premise #8 / IntakeFieldsRegistry verification is wrong (dev's correction is itself incorrect): **`decision: fix_required`** — design doc §8.2.5 must cite the correct source.

If the OQ-7.11 verdict comes back DISAGREEING with the dev (the `request_handover` decision tree IS an unflagged if-else in prompt per §4.1 Q6 — i.e., the decision tree IS a §1.7 violation in the existing `system_prompt.txt`): **`decision: out_of_scope_review`** with the decision-tree paragraph quoted + a new R-item proposed (e.g., `R-system-prompt-handover-decision-tree-shape`) for M3+ revisit. **Do NOT classify as `fix_required`.** Per deliver-agent + human pre-decision (see header OQ-7.11): the decision tree predates M1; fixing it would be a Sprint 40+ scope change beyond Sprint 37's design-freeze; Sprint 37 close does NOT broaden scope to fix pre-existing surfaces. The disagreement is informational input to a possible M3+ scope decision, not a Sprint 37 blocker.

If Codex DISAGREES with the Tier-0 candidate verdicts (C1-C5) OR DEFER recommendations (C2, C3) per §8: **`decision: out_of_scope_review`** — the Tier-0 elevation / deferral is deliver-agent + human authority per M2 §10 stop condition #1; Codex's disagreement is informational input to a possible re-evaluation at Sprint 37 close.

If a substance concern is found that is NOT in scope for Sprint 37 (e.g., a critique of the NEW M2 milestone scope itself — the milestone is approved; the per-sub-sprint review evaluates the sub-sprint per the Sprint 37 contract; OR a critique of the OLD Sprint 36 minimum-surface freeze that NEW M2 superseded — the supersession is human-approved at fd7396a; not Sprint 37 dev's scope): **`decision: out_of_scope_review`** with the concern named and the milestone-contract reference cited.

## 13. Self-check before submitting

- [ ] §2 scope-discipline gate walked; every disallowed surface checked against `git diff 6d97888..51c327c --stat`; supersession-on-predecessors verified at `fd7396a` (NOT in `51c327c`).
- [ ] §3 §4.1 nine-question kernel walked against the PROPOSED DESIGN (decisions a-i) per design doc §11; per-Q verdict aligned with or independently dissenting from dev's verdict.
- [ ] §4 §1.7 boundary check walked against D1 / D2 / D3 (NEW S1) / D4 (NEW S2) / D-existing-Sprint-6/7/11 / D5 (UC-switching invariant matrix §10.5) / D-state_inheritance per-Skill / D-dispatcher composition / D-no-per-UC-YAML across 6 Skill bodies; verbatim §6 #4 authorization at design doc §8.2.4 verified.
- [ ] §5 hard-fence verification (Sprint 37 contract §6 19 items + M2 §6 22 items + MATERIAL FINDING + premise #7 refinement + premise #8 IntakeFieldsRegistry + Sprint 33 cue/slot split).
- [ ] §6 schema + reproducibility (design doc structure 13 sections + frontmatter supersedes pair + handoff structure 12 sections + cited file:line spot-checks 5-6 ranges + supersession frontmatter on predecessors).
- [ ] §7 Java baseline re-run from clean checkout; 983/1-inherited/0/2 byte-identical.
- [ ] §8 Tier-0 candidate independent verification (C1 REJECTED + C2 QUALIFIED-DEFER + C3 QUALIFIED-DEFER + C4 NOT A CANDIDATE + C5 NOT A CANDIDATE) per candidate.
- [ ] §9 OQ independent verification (OQ-7.11 §4.1 Q6 decision tree shape; other OQs informational).
- [ ] §10 deferred items noted as non-blocking.
- [ ] `docs/codex-findings.md` written per §11 format.
- [ ] Verdict per §12 expected shape.
