---
title: Milestone M2-Skill — Skill Foundation + UC-Switching Continuity (LLM-led, Policy-bounded) [SUPERSEDED MID-FLIGHT 2026-05-17]
doc_tier: current-runtime
status: superseded
implementation_status: superseded_mid_flight (only Sprint 36 design freeze shipped; downstream Sprints 37-40 per OLD framing never implemented)
source_of_truth: docs/milestone_objective.md (NEW M2 — Skill Registry Abstraction)
last_reviewed: 2026-05-17
review_cadence: archived; no future review
supersedes: [docs/milestones/M1_objective.md]
superseded_by: docs/milestone_objective.md (NEW M2 — Skill Registry Abstraction + Wholesale Retroactive Externalization)
notes: >
  M2-Skill was drafted + approved + partially shipped on 2026-05-17,
  then superseded mid-flight the same day by NEW M2 (Skill Registry
  Abstraction). Only Sprint 36 design freeze shipped (commit ed71031);
  Sprint 37 contract was drafted but never dev'd; Sprints 38-40 never
  contracted. This file archives the M2-Skill objective text for
  historical reference per doc_governance.md supersede-not-delete
  rule. The supersession verdict + carry-forward inventory are in
  §0 (NEW section, NOT part of original M2-Skill draft). The
  original §1-§11 below are preserved verbatim from the M2-Skill
  draft that was live in docs/milestone_objective.md at the moment
  of supersession on 2026-05-17.
---

# Milestone M2-Skill — Skill Foundation + UC-Switching Continuity [ARCHIVED — SUPERSEDED MID-FLIGHT 2026-05-17]

## 0. Supersession verdict (2026-05-17; NEW section, NOT part of original M2-Skill draft)

**Status**: superseded mid-flight by `docs/milestone_objective.md` (NEW M2 — Skill Registry Abstraction + Wholesale Retroactive Externalization).

**Sub-sprints shipped before supersession**:

- **Sprint 36** (M2-Skill sub-sprint 1) — design freeze for minimum-surface incrementalism. Commit `ed71031`. Archive at `docs/sprints/sprint-036-objective.md` + `docs/sprints/sprint-036-handoff.md` + `docs/sprints/sprint-036-codex-review.md`. Produced `docs/proposals/skill_foundation_design.md` (~1100 lines; now superseded in-place via supersede pattern with status: superseded + superseded_by: docs/proposals/skill_registry_design.md).

**Sub-sprints contracted but NOT shipped (originally planned, abandoned at supersession)**:

- **Sprint 37 (OLD)** — S1 (`Resolve.FAQ.GroundedAnswer`) implementation per minimum-surface framing. Contract was drafted at Sprint 36 close (pending human review); NEVER dev'd. Wholesale replaced by NEW Sprint 37 = Skill Registry design freeze (per NEW M2).
- **Sprint 38 (OLD)** — S2 (`Resolve.Intake.CollectAndHandover`) implementation. NEVER contracted; never dev'd. Re-homed inside NEW M2 Sprint 39 (RESOLVE_INTAKE Skill migration + S2 `intake_complete_required` guardrail).
- **Sprint 39 (OLD)** — UC-switching wide continuity state implementation. NEVER contracted; never dev'd. Re-homed inside NEW M2 Sprint 41 (UC switch + state preservation across Skill boundary, redesigned coherently with Skill Registry abstraction).
- **Sprint 40 (OLD)** — Per-LLM-call latency characterization (parallel-track, consumed `R-llm-provider-latency-drift-2026-05-16`). NEVER contracted. Deferred to next milestone (orthogonal infra parallel-track; preserved in `docs/action_bank.md` deferred).

**Why superseded**: M2-Skill Sprint 36 design freeze locked "reuse existing PhasePlan + new system_prompt.txt teaching paragraph + new predicate adjacent to existing shouldRejectXxx family" — **minimum-surface incrementalism** that PRESERVED the scattered Zhang-Sanfeng pattern of accumulating teaching paragraphs in `system_prompt.txt` (Sprint 23/31/33 + would-be Sprint 37 paragraph would continue accumulating side-by-side) and predicates in `AgentRunLoopImpl.java` (Sprint 6/7/11 + would-be Sprint 37 predicate would continue accumulating side-by-side). "Skill" in OLD framing was a label for "another paragraph plus another adjacent predicate" — there was no extracted abstraction.

The human's M2 architectural intent was always "extract scattered content into a Skill abstraction" (per Sprint 5 F2 original proposal + user-engaged research-agent proposal). The OLD framing's minimum-surface freeze inverted that intent. On 2026-05-17, after observing the OLD Sprint 37 contract (which would have added ANOTHER teaching paragraph + ANOTHER `shouldRejectXxx` Java method to the scattered pattern), the human directed deliver-agent to redirect M2 to **Skill Registry abstraction** (first-class externalized Skill definitions + retroactive migration of existing scattered content). NEW M2 is the result.

**Carry-forward into NEW M2** (preserved verbatim where applicable):

- **§6 #4 verbatim human authorization for S1 `must_cite_source` bounded inversion of `D-hard-citation-gate`** — quoted verbatim in NEW M2 §6 #4 and in NEW Sprint 37 contract §6 #4 framing.
- **All hard fences on `RuntimeIntentClassifier.java` / `IntentClassification.java` / `DriftResult.java` / `DriftDetector.java` / `UseCaseRouter.java` / `ClassifyUseCaseTool.java` touch** (M3-D `R-loosen-topic-uc-binding-llm-owned-drift` deferred to M3+; preserved in NEW M2 §6 #5).
- **All hard fences on `escalation_reason` enum widening** at `PhaseEvaluator.java:39-63` (`D-new-escalation-reason-enum` deferred to M3-A; preserved in NEW M2 §6 #7).
- **All hard fences on `PhaseEvaluator.INTAKE_UCS` edit** (M3-B `D-single-handover-orchestrator` deferred unless Salesforce cutover pressure; preserved in NEW M2 §6 #6).
- **All hard fences on `D-full-issue-ledger`** (hard-deferred; requires explicit new-objective approval; preserved in NEW M2 §4 non-goals).
- **All hard fences on existing case families + shadow case families** (cascade fence carried from M1; preserved in NEW M2 §6 #9 + #10).
- **Constitution-discipline** preserved per `feedback_constitution_discipline_vs_planning_anticipation.md` (preserved in NEW M2 §6 #12).
- **No-mocked-LLM-as-primary-evidence** preserved per `feedback_mocked_llm_cannot_prove_prompt_causal_change.md` (preserved in NEW M2 §6 #17).
- **No-Skill-prescribes-customer-facing-language** + **no-per-step-argument-values-hardcoded** preserved per Constitution §1.3 (NEW M2 §6 #18 + #19).
- **Sprint 36 §1.3 MATERIAL FINDING** about Sprint 11 §M1 `shouldRejectPrematureResolveOutcome` precedent — informs NEW Sprint 37 design decision (g) on predicate migration mapping.

**NEW M2 reframing** (architectural):

- **Skill = first-class abstraction**: YAML/JSON externalized definitions under `server/src/main/resources/skills/`; new `Skill` data class + `SkillRegistry` + `SkillLoader` + `SkillGuardrailDispatcher` Java classes; `PhaseEvaluator` becomes Skill Selector (`skillRegistry.select(phase, useCase) → Skill`).
- **All 6 phases migrated**: DISCOVER + CONFIRM + RESOLVE_FAQ + RESOLVE_INTAKE + ESCALATE + TERMINAL Java-string content extracted to Skill YAMLs (NEW Sprint 38 + 39).
- **All scattered teaching migrated**: Sprint 23 / 31 / 33 paragraphs from `system_prompt.txt` factored into Skill `procedure` / `guardrails` blocks (NEW Sprint 40); `system_prompt.txt` shrinks to orchestration shell.
- **All scattered predicates migrated**: Sprint 6/7/11 Java methods in `AgentRunLoopImpl.java` factored into Skill `guardrails` enforced via unified Skill terminal-predicate dispatcher (NEW Sprint 39); post-Sprint-39 `AgentRunLoopImpl` carries ZERO `shouldRejectXxx` methods.
- **S1 + S2 ride the abstraction**: S1 `must_cite_source` declared in `resolve_faq_grounded_answer.yaml` `guardrails` (NEW Sprint 39); S2 `intake_complete_required` declared in `resolve_intake_collect_and_handover.yaml` `guardrails` (NEW Sprint 39).
- **UC switch + state preservation integrated**: session-level state-bus + per-Skill `state_inheritance` declaration designed coherently with Skill abstraction (NEW Sprint 41), not as separate add-on.
- **Acceptance bar recalibrated**: bad-case suite (Alice) + interactive eval are OBSERVATION, NOT gate for THIS milestone (architecture-focused, not behaviour-fix-focused; per NEW M2 §5 recalibration). Primary gate = functional review + Java tests + Sprint 37 freeze decisions honored.

**NEW M2 sub-sprint sequence**:

- **NEW Sprint 37** (NEW M2 sub-sprint 1) — Skill Registry + state-across-Skill design freeze (10 design decisions; docs-only; produces `docs/proposals/skill_registry_design.md`).
- **NEW Sprint 38** — SkillRegistry core + 4 simpler phase Skills migration (DISCOVER + CONFIRM + ESCALATE + TERMINAL).
- **NEW Sprint 39** — RESOLVE_FAQ + RESOLVE_INTAKE Skill migration + Sprint 6/7/11 predicate migration + S1/S2 new predicates + unified Skill terminal-predicate dispatcher (the big sub-sprint).
- **NEW Sprint 40** — Teaching extraction from `system_prompt.txt` (Sprint 23/31/33 paragraphs) + orchestration shell cleanup.
- **NEW Sprint 41** — UC switch + state preservation across Skill boundary.

**Cross-references**:

- NEW M2 milestone objective: `docs/milestone_objective.md`.
- NEW Sprint 37 contract: `docs/sprint_objective.md`.
- NEW Sprint 37 dev prompt: `compact/sprint-037-dev-prompt.md`.
- NEW Skill Registry design freeze doc (to be authored by NEW Sprint 37 dev): `docs/proposals/skill_registry_design.md` (path locked; doc not yet authored).
- OLD Sprint 36 design freeze (superseded in-place): `docs/proposals/skill_foundation_design.md` (status: superseded; superseded_by: docs/proposals/skill_registry_design.md).
- Sprint 36 archive (immutable): `docs/sprints/sprint-036-objective.md` + `docs/sprints/sprint-036-handoff.md` + `docs/sprints/sprint-036-codex-review.md`.

---

# Original M2-Skill content below (preserved verbatim 2026-05-17 at moment of supersession; do NOT edit)

> The §1-§11 below are the M2-Skill draft text as it was in
> `docs/milestone_objective.md` at the moment of supersession on
> 2026-05-17. The original draft was created earlier on 2026-05-17
> (after M1 close + after Sprint 36 ship) and was active for several
> hours before the human directed the supersession. Only Sprint 36
> (M2-Skill sub-sprint 1) shipped under this framing. Read this
> alongside NEW `docs/milestone_objective.md` (NEW M2) to understand
> the recalibration; read alongside `docs/proposals/skill_foundation_design.md`
> "Why superseded" section for the design-doc supersession rationale.

## 1. Milestone class (ORIGINAL M2-Skill)

**Multi-layer milestone, 5 coordinated sub-sprints across `prompt_projection` + `semantic_planner` + `skill_state` + `infra`.** All sub-sprints except Sprint 40 (pure infra parallel-track) are semantic-touching and REQUIRE the §7 stanza. Sub-sprint layer breakdown:

| Sub-sprint | Track | Layer | §7 stanza | Codex review |
|---|---|---|---|---|
| Sprint 36 — Skill foundation + UC-switching continuity design freeze | A (anchor) | docs/design only (NO `server/` code) | REQUIRED (multi-layer prospective covering Sprints 37/38/39) | Per-sub-sprint (§4.3 trigger #1 + #2) |
| Sprint 37 — S1 (`Resolve.FAQ.GroundedAnswer`) implementation | A | `prompt_projection` + `semantic_planner` + (predicate) Runtime-owned floor | REQUIRED | Per-sub-sprint (§4.3 trigger #3: Runtime grounding-floor surface) |
| Sprint 38 — S2 (`Resolve.Intake.CollectAndHandover`) implementation | A | `prompt_projection` + `skill_state` + (predicate) Runtime-owned floor | REQUIRED | Per-sub-sprint (§4.3 trigger #3: new skill envelope surface on intake) |
| Sprint 39 — UC-switching wide continuity state implementation | A | `skill_state` + `prompt_projection` | REQUIRED | Per-sub-sprint (§4.3 trigger #3: session state model touch) |
| Sprint 40 — Per-LLM-call latency characterization | C (parallel) | `infra` / observability | EXEMPT (pure infra) | Milestone-shared at M2 close (§4.1 exemption eligible) |

**Codex review default for M2-Skill: PER-SUB-SPRINT for semantic-touching sub-sprints** (Sprints 36, 37, 38, 39), milestone-shared for the infra parallel-track (Sprint 40), plus a cumulative architectural-posture check at M2 close.

## 2. Goal (ORIGINAL M2-Skill)

Promote the Sprint 5 (F2) skill orchestration proposal (`docs/proposals/skill_orchestration_candidates.md`, status `proposal` / `not_started`) to code via a **hybrid Skill foundation**, and add wide UC-switching continuity. Concretely:

- **Skill foundation hybrid framing landed.** A Skill is a parametrized `PhasePlan` with: (1) tool whitelist surfaced as prompt envelope; (2) recommended ordering surfaced as soft prompt guidance (LLM-owned per Constitution §1.3); (3) terminal predicate enforced via Java guard for Runtime-owned floor ONLY per Constitution §1.4. Skill bodies do NOT contain per-UC-branch if-else (§1.7 enforced).
- **Codex M1 Finding 1 retired via S1 grounding-floor predicate.** S1 (`Resolve.FAQ.GroundedAnswer`) implements the predicate "`RecordOutcomeTool.recordOutcome(class=resolve)` refuses persistence without a `source_id` citation present in the user-facing message." Alice closure-criterion (a) PASSes across multi-trace reruns with zero fabrication-condition triggers.
- **S2 intake skill landed.** S2 (`Resolve.Intake.CollectAndHandover`) implements per-UC-G/H/I/J/K field-by-field intake with the terminal predicate "refuse `intake_complete_for_uc_X` escalation until `requiredIntakeFields` is fully populated."
- **UC-switching wide continuity landed.** Sprint 39 implements continuity invariants for `accumulated_tool_results`, `session.customerContext`, `session.intakeFields` partial state, prior citations / grounding-history.
- **Latency characterization shipped (parallel-track).** Sprint 40 produces the per-LLM-call attribution table consuming `R-llm-provider-latency-drift-2026-05-16`.

## 3. Sub-sprint sequence (ORIGINAL M2-Skill; preliminary)

> Detailed scope per Sprint 36 / 37 / 38 / 39 / 40 — full text preserved here in the original draft. **Note: only Sprint 36 shipped** before supersession. The Sprint 37 contract was drafted; never dev'd. Sprints 38/39/40 never contracted.

(Detailed scope text from original §3 sub-sections preserved; readers may consult `git show ed71031:docs/milestone_objective.md` if the full original is needed for archaeology. The Sprint 36 row is faithfully reflected in `docs/sprints/sprint-036-objective.md` immutable archive. The Sprint 37 (OLD) row is reflected in the pre-supersession `docs/sprint_objective.md` working-tree state at 2026-05-17 morning.)

## 4-11. Other sections (ORIGINAL M2-Skill)

The original M2-Skill draft had these additional sections; their decisions are either carried forward into NEW M2 (see §0 supersession verdict carry-forward inventory) or superseded by NEW M2 framing:

- **§4 Non-goals** — Most non-goals carry forward into NEW M2 verbatim (no M2-customer-honesty deferral; no `RuntimeIntentClassifier` touch; no `D-single-handover-orchestrator` consumption; etc.).
- **§5 Acceptance bar** — Alice multi-trace primary gate was the OLD framing's bar; **NEW M2 RECALIBRATED Alice to observation only** (see NEW M2 §5 recalibration rationale).
- **§6 Hard fences (19 items)** — Most fences carry forward (esp. §6 #4 S1 bounded inversion; §6 #5-#7 deferrals; §6 #12 constitution-discipline; §6 #17 no-mocked-LLM; §6 #18-#19 customer-facing language). NEW M2 adds fence #20 (no deletion of skill_foundation_design.md) + #21 (M1 functional behaviour preservation with case-behaviour drift allowance per §5 recalibration) + #22 (Alice case stays as observation).
- **§7 R-items consumed/surfaced** — `R-grounding-discipline-iterative-search-fabrication` and Sprint 5 F2 promotion carry forward; consumption mechanism changed (NEW Sprint 39 ships S1 inside Skill abstraction, not as adjacent Java method).
- **§8 Codex review plan** — Per-sub-sprint pattern preserved + extended to cover NEW M2's 5 semantic-touching sub-sprints (37/38/39/40/41).
- **§9 Estimated duration** — OLD M2-Skill estimated 2-3 weeks; NEW M2 estimated 3-4 weeks (bigger steps + more migration surface).
- **§10 Stop conditions** — Most carry forward; NEW M2 §10 #10 adds "behavioural equivalence test failure on migration" stop condition; NEW M2 §10 explicitly notes LLM-observed behaviour shift does NOT trigger stop per §5 recalibration.
- **§11 Cross-milestone sequencing** — M3-A / M3-B / M3-C / M3-D / M3-Latency candidates carry forward; NEW M2 adds **M3-Skill-Tuning** candidate (post-M2 behavioural tuning of individual Skills if needed; e.g., Alice closure-criterion (a) PASS if not achieved organically).

---

**End of OLD M2-Skill milestone objective archive.** For the live M2 contract, see `docs/milestone_objective.md` (NEW M2 — Skill Registry Abstraction). For the live first sub-sprint contract, see `docs/sprint_objective.md` (NEW Sprint 37 — Skill Registry design freeze).
