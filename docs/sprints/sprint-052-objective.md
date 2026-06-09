---
title: Sprint objective — Sprint 52 / M5 S3 — Projection audit (C1) + Skill-driven convergence (C2)
doc_tier: current-runtime
status: current
implementation_status: not_started
source_of_truth: this file
last_reviewed: 2026-05-25
review_cadence: per sub-sprint
supersedes: [docs/sprints/sprint-051-objective.md]
superseded_by: null
notes: >
  DRAFT pending human approval (2026-05-25). THIRD sub-sprint of Milestone M5 —
  Observability Coherence and the milestone's ONLY semantic surface (§7 REQUIRED).
  S3 first produces a zero-risk **field consumption matrix (C1)** — each projection
  field × who consumes it (system_prompt / eval trace contract / drift-task
  reconstruction / a Skill declaration / a real trace shows the LLM using it) —
  then converges (C2) the projection to be **Skill-driven** where M2 left it
  UC-driven, WITHOUT changing the semantic information available to the LLM
  (verified by a real-LLM bad-case rerun, not mocked tests). Depends on S2 (the
  per-invocation trace gives C1 the per-step projections it audits). Consumes
  OQ-S51.2 (the S2 eyeball finding: BotTurn.projected_context carries a pre-existing
  mergeFaqGroundingIntoProjection overlay that the raw per-invocation projection
  does not). Per milestone_objective.md §8 + iteration_governance.md §4.3, S3 gets
  a PER-SUB-SPRINT Codex review at S3 close (before any S4). Whole-solution
  proposal: docs/solutions/observability_coherence_admin_trace_and_projection.md
  §2.C / §4.C (C1/C2) / §8 (#3 fences). HARD ORDER inside S3: C1 map DELIVERED +
  reviewed BEFORE any C2 projection-field change.
---

# Sprint 52 / M5 S3 — Projection audit (C1) + Skill-driven convergence (C2)

## Class

`prompt_projection` (§3.2 Q3 — what state/signals/candidate-lists are surfaced to
the LLM in the per-turn projection). **§7 REQUIRED** — this is the milestone's only
semantic surface. C2 changes *what is projected*; the contract's central invariant
is that it must NOT change *what semantic information is available to the LLM*.

## Goal

The per-turn projection built by `ContextProjectionBuilder`
(`server/src/main/java/com/gumtree/csagent/service/runtime/ContextProjectionBuilder.java`)
still carries a pre-M2 **UC-driven** field set plus duplication and at least one
declared-but-never-emitted contract gap. M2 made tool/skill selection Skill-driven
(`PhasePlan.allowedTools()` / `requiredContextKeys()`, `Skill` registry) but the
projection layer did not follow. S3:

1. **C1 (diagnostic, zero behaviour risk):** produces an auditable **field
   consumption matrix** — for each projection field, which consumers depend on it.
2. **C2 (convergence, gated by C1):** makes the projection **read Skill
   declarations** to decide which slots to project, removes a computed-then-
   discarded UC-driven base computation, and closes the `moderation_context`
   declared-but-never-emitted gap — **registry/Skill-driven, no per-UC if-else**,
   and with **no change to the LLM-visible semantic information** (real-LLM
   bad-case rerun is the evidence gate).

A human opening the admin trace (now per-invocation, post-S2) sees a projection
whose slots are explained by Skill declarations, not by a legacy UC matrix; the
eval trace contract and drift/task-history reconstruction still see every field
they depend on.

## Scope (numbered; this is the contract)

### Part C1 — Field consumption matrix (DELIVERED + reviewed BEFORE any C2 change)

**#1 — Projection field consumption matrix.** Enumerate every field the projection
emits (walk `ContextProjectionBuilder.buildProjection` / `build` and the nested
builders — `session.*`, `budget_state`, `phase_plan`, `tool_schemas`,
`knowledge_hits`, `drift_history`, `task_history`, `task_summary`, the §N0
always-null slots, etc.). For each field produce a row × these consumer columns:
(a) the **system_prompt** explains/instructs on it; (b) the **eval trace contract
validator** requires it (`eval_interactive/.../trace/collector.py` + the harness
trace-contract checks — cite the exact check); (c) **drift/task-history
reconstruction** depends on it (`ContextProjectionBuilder.buildDriftAndTaskHistory`
~`:1265`); (d) a **Skill declares** it (`PhasePlan.requiredContextKeys()` /
`Skill` registry — establish the exact source); (e) a **real trace shows the LLM
using it** (use the S2 per-invocation projections + raw responses as evidence).
Deliver the matrix as a markdown artifact under `docs/diagnostics/` (e.g.
`docs/diagnostics/m5-s3-projection-consumption-map.md`). **This artifact is the
gate for every C2 change below** — no field is touched in C2 unless its row shows
no consumer that would break.

**#1a — OQ-S51.2 row (consumed from S2).** The matrix MUST include the
`BotTurn.projected_context` vs per-invocation-projection distinction surfaced by
the S2 eyeball (OQ-S51.2): the persisted single-column value is
`result.lastProjection()` with a PRE-EXISTING `ControlKernel.mergeFaqGroundingIntoProjection`
overlay, while the per-step record stores the raw projection the LLM received.
The matrix names which of the two is canonical for (the eval trace contract) vs
(admin-trace display) vs (what the LLM actually saw), and recommends a disposition
(leave as-is / note / converge) — **decision deferred to the human at C1 review**;
S3 does not silently change the overlay.

### Part C2 — Skill-driven convergence (each item gated by its #1 matrix row)

**#2 — Skill-declared context-key gating.** Where the projection currently decides
context slots by UC, make it read the Skill declaration
(`PhasePlan.requiredContextKeys()` / the active `Skill`) to decide which slots to
project. Registry/Skill-driven; **no per-UC if-else, no keyword/enum** (§1.7).

**#3 — `moderation_context` coherence gap.** C1 is expected to confirm a Skill
(per the proposal, the FAQ Skill) declares `moderation_context` required while
`ContextProjectionBuilder` never emits it. Resolve the gap in the Skill-driven
direction the matrix supports — either emit it (if a consumer needs it) or remove
the stale declaration (if no consumer does). **Pick the direction the C1 row
justifies; do not guess.**

**#4 — Remove computed-then-discarded UC-driven `tool_schemas` base.** The builder
computes a UC-driven `tool_schemas` base (~`:633`) that is then overwritten by the
Skill/plan-filtered set (~`:862`). Remove the discarded base computation in favour
of the Skill-driven `plan.allowedTools()` filtered set — **only if** the #1 matrix
confirms nothing consumes the base before it is overwritten.

**#5 — `state_inheritance.soft_signal_via_projection` gating.** Gate soft signals
(e.g. `prior_use_case_carry`) through the `state_inheritance.soft_signal_via_projection`
configuration path rather than a UC branch — confirm the config key + its current
wiring in C1 first, then route the soft signal through it. Soft signal stays a
*signal the LLM owns*, never a hard branch.

**#6 — Tests + the real-LLM evidence gate.** Java: projection-rendering / wiring
tests proving (a) the Skill-driven gating projects the same slots a correct UC
path projected for the cases in scope, (b) `moderation_context` is now coherent
(emitted or declaration removed, per #3), (c) `tool_schemas` still ends with the
correct Skill-filtered set. **Mocked-LLM tests cover wiring/rendering ONLY (per
§5.6 eval-evidence gate).** The behaviour-risk gate is the **real-LLM bad-case
suite rerun** (see Test/eval requirements) — it must hold the M4-close distribution,
demonstrating C2 did not change LLM-visible semantic availability.

## Hard fences / STOP conditions (do NOT do)

- **C1 BEFORE C2**: no projection field is deleted, renamed, or changed in content
  before the #1 consumption matrix is delivered and confirms no consumer depends on
  it via (a) the eval trace contract validator, (b)
  `ContextProjectionBuilder.buildDriftAndTaskHistory`, or (c) admin-trace rendering.
  STOP and surface if a field's consumers are ambiguous.
- **Registry/Skill-driven — no per-UC if-else** (§1.7). Convergence reads Skill /
  `PhasePlan` declarations; it does NOT add a keyword / regex / enum / per-UC matrix
  for any semantic decision. No semantic ownership moves from the LLM to Java.
- **No change to LLM-visible semantic information.** C2 may change *which slot
  carries* information or *how it is gated*, never *whether the LLM can see* a
  semantic signal it could see before. The real-LLM rerun is the proof.
- No `escalation_reason` enum change, no tool-schema change, no PII / safety /
  grounding floor change smuggled in. No `composite.py` / eval-fixture / scoring
  change.
- Do NOT alter the OQ-S51.2 FAQ-grounding overlay behaviour without an explicit
  human decision at C1 review (it is PRE-EXISTING; S3 maps it, the human decides
  whether to converge it — likely S4 or a later milestone).
- Do NOT change the S2 `bot_turn_llm_calls` schema or the `/trace` endpoint shape
  (S2 is closed; S3 consumes its per-step projections as C1 evidence, read-only).
- **STOP and surface to deliver-agent** if the C1 matrix shows that any in-scope
  convergence (#2-#5) cannot be done without removing an LLM-visible signal, or if
  the real-LLM rerun regresses the bad-case distribution — that is an in-flight
  downgrade (do not push the change to close the symptom).

## Test / eval requirements

- **C1 deliverable** (`docs/diagnostics/m5-s3-projection-consumption-map.md`)
  reviewed by deliver-agent + human BEFORE C2 lands. C1 alone is a valid partial
  close if C2 proves larger than one sub-sprint (deliver-agent + human decide).
- **Java**: `mvn test -B` — **no NEW regression** vs `1165 / 1-inherited / 0 / 2`
  (the inherited `SystemPromptUserRequestedTiebreakerTest` persists per OQ-S41.5;
  the count may grow with new S3 tests).
- **Real-LLM bad-case suite rerun is the S3 evidence gate** (needs backend +
  LLM keys): rerun `eval_interactive/case_specs/bad_cases/` (deliver-agent confirms
  the `core` vs `scope-relevant` run set against `bad_cases/_manifest.md` §5.6.1)
  and confirm the M4-close distribution holds — **PASS×5** (cs001, cs014, cs029,
  cs066, fg5q) + **IMPROVING×4** (alice, cs011, cs012, wmkb) + **FAIL×3** (cs015,
  cs095, iwzx) + **OOSR×0**. A regression here is an in-flight downgrade → STOP.
  (Mocked-LLM tests are NOT acceptable as the primary evidence that C2 preserved
  LLM-visible semantics — they control the measured variable.)
- **Python**: `cd eval_interactive && uv run python -m pytest --tb=no -q` — no NEW
  regression vs `3 failed, 486 passed` (S3 should not touch eval-harness code).

## §7 — Layer-classification + anti-hardcode stanza

**Target failure layer:** `prompt_projection` (§3.2 Q3) — S3 changes which context
slots the per-turn projection surfaces and how they are gated.

**Tier-0 invariant:** This sub-sprint adds no Tier-0 invariant. The Constitution's
"prompt_projection owns whether the LLM has the inputs to make a correct semantic
choice" governs; the projection sits inside the Runtime's "trace and eval contract"
responsibility (§1.4). No new Java guard.

**Semantic hardcode:** No semantic hardcode introduced. Convergence is
**registry/Skill-driven** — the projection reads `PhasePlan.requiredContextKeys()` /
`Skill` declarations and `state_inheritance.soft_signal_via_projection` config to
decide slots, REPLACING (not adding to) the legacy UC-driven branching. No keyword /
regex / enum / per-UC matrix is added; soft signals stay LLM-owned. If any C2 item
cannot be done without a UC branch, STOP and surface (do not introduce the branch).

**Generalization coverage:** target = the in-scope projection slots converge to
Skill-driven gating with the bad-case distribution unchanged on a real-LLM rerun
(no LLM-visible semantic loss). Neighbor = the other UCs exercised by the bad-case
suite project their correct slots. Negative = a UC whose Skill does not declare a
slot does not receive it (no spurious projection). Shadow = the held-out bad-case /
shadow traces (human / review-agent readable only) show no regression. Counts
finalized at C1 (the matrix names which slots × which bad cases exercise them).

## Codex review plan (§4.3)

**PER-SUB-SPRINT Codex review at S3 close** (locked human-confirmed addition per
`milestone_objective.md` §8; deliver-agent discretion per §4.3) — S3 is the only
semantic surface and is coupled to the eval trace contract; verifying it in
isolation de-risks the milestone before any S4. The deliver-agent generates
`compact/sprint-052-codex-review-prompt.md` at S3 close (the §4.1 nine-question
kernel + the §4.2 sprint-close header) against the S3 commit range, in ADDITION to
the milestone-shared review at M5 close. Dev does NOT dispatch Codex; deliver-agent
+ human dispatch at S3 close.

## Handoff requirements

- Author `docs/sprints/sprint-052-handoff.md`; leave **§12** empty (deliver-agent +
  human at close).
- Ship the C1 consumption matrix as a committed artifact under `docs/diagnostics/`.
- Record: `git show --numstat`; the C1 → C2 ordering evidence (matrix delivered
  before field changes); the real-LLM bad-case rerun results dir + per-case
  distribution; the §7 self-walk; any field whose convergence was STOPPED-and-
  surfaced rather than forced.

## Commit discipline

Dev stages **only S3 scope**: `server/src/main/java/**` (ContextProjectionBuilder +
any Skill/PhasePlan wiring it reads), the C1 matrix under `docs/diagnostics/**`, the
new Java tests, and NEW `docs/sprints/sprint-052-handoff.md`. **No `git add -A`** —
deliver-agent close-bundle files (objective archive, 10-handoff lead, action_bank
row, the per-sub-sprint Codex review prompt) are bundled by the human at close.

## OQ (open questions — filled during the sub-sprint)

- **OQ-S51.2 (inherited from S2)** — `BotTurn.projected_context` FAQ-grounding
  overlay vs raw per-invocation projection: mapped in C1 (#1a); convergence
  disposition decided by the human at C1 review (not auto-converged in S3).
- _others surfaced during C1 to be listed here_
