---
title: Sprint objective — Sprint 53 / M5 S4 — Skill-declaration audit + context-key gating (C2 #2) + soft-signal gating (C2 #5)
doc_tier: current-runtime
status: current
implementation_status: not_started
source_of_truth: this file
last_reviewed: 2026-05-25
review_cadence: per sub-sprint
supersedes: [docs/sprints/sprint-052-objective.md]
superseded_by: null
notes: >
  DRAFT pending human approval (2026-05-25). FOURTH sub-sprint of Milestone M5 —
  Observability Coherence; the milestone's highest-risk sub-sprint. Completes the
  M5 #3 Skill-driven projection convergence that S3 began: S3 landed C2 #3
  (moderation_context strip) + #4 (tool_schemas Skill-registry base) and
  STOP-surfaced #2 (Skill-declared context-key gating) + #5
  (soft_signal_via_projection gating) to S4 because the C1 matrix flagged the
  Skill requiredContextKeys/stateInheritance declarations as INCOMPLETE — gating
  on them as-is would drop LLM-visible context. S4 therefore runs in two gated
  phases: Phase A (Skill-declaration AUDIT — complete + intentionalize the 6
  Skills' declarations; delivered + reviewed BEFORE any gating, analogous to S3's
  C1) then Phase B (the #2 + #5 convergence gated by the audit). C3 (projection
  dedup/denoise) + OQ-S52.4 (knowledge_hits canonicalization) are CONDITIONAL /
  deferrable (decided at S4 close) to keep S4 risk-bounded. §7 REQUIRED;
  per-sub-sprint Codex at S4 close. **The real-LLM bad-case rerun AND a shadow
  rerun are MANDATORY evidence gates** (mocked-LLM covers wiring only). Whole-
  solution proposal: docs/solutions/observability_coherence_admin_trace_and_projection.md
  §2.C / §4.C (C2/C3) / §8 (#3 fences). HARD ORDER: audit DELIVERED + reviewed
  BEFORE any projection-gating change.
---

# Sprint 53 / M5 S4 — Skill-declaration audit + context-key gating (C2 #2) + soft-signal gating (C2 #5)

## Class

`prompt_projection` (§3.2 Q3). **§7 REQUIRED** — S4 changes *what context slots
and soft signals are projected* and how they are gated. The central invariant
(as in S3) is that it must NOT change *what semantic information is available to
the LLM*; S4 is higher-risk than S3 because #2/#5 touch the context/soft-signal
surface the LLM actually reads (S3's #3/#4 were a stale declaration + a
run-loop-byte-identical tool base).

## Goal

Complete the M5 #3 Skill-driven projection convergence. Today
`ContextProjectionBuilder` emits context slots (`form_context`,
`customer_context`, `listing_context`, …) and soft-signal slots
(`alternate_candidate_use_cases` `:441`, `discover_disambiguation_signals`
`:457`, `prior_use_case_carry` `:475`) **unconditionally / via legacy
UC-driven logic**, while the Skill declarations that SHOULD drive them
(`PhasePlan.requiredContextKeys()` ← `skill.requiredContextKeys()` at
`PhaseEvaluator:455`; `Skill.stateInheritance().softSignalViaProjection()`) are
**declared but not read at the projection layer** and are **incomplete** (the
S3 C1 matrix: `discover_triage` does not declare `customer_context`; only the
FAQ Skill declares `listing_context`). S4 makes the projection Skill-declaration-
driven for these slots — but only after an audit completes the declarations so
gating cannot drop an LLM-visible signal.

## Scope (numbered; this is the contract)

### Phase A — Skill-declaration audit (DELIVERED + reviewed BEFORE any Phase-B gating)

**#1 — Skill-declaration completeness audit.** For each of the 6 production
Skills, audit (a) which context slots the Skill's flow actually consumes (read
the Skill `procedure` + the bad-case UCs it serves), (b) what
`requiredContextKeys` it currently declares, (c) what the projection currently
emits unconditionally for that Skill's (phase, UC), and (d) the
`stateInheritance.softSignalViaProjection` posture for the three soft-signal
slots. Produce a **declaration-completeness matrix** (`docs/diagnostics/m5-s4-skill-declaration-audit.md`)
naming, per Skill × slot: declared / emitted-today / actually-needed / GAP. The
matrix MUST resolve, for every context + soft-signal slot, whether the Skill
declaration is the safe gate (complete) or whether the declaration must be
COMPLETED first. **No Phase-B gating change lands for any slot whose audit row
shows an unresolved gap.** Reviewed by deliver-agent + human at the Phase-A gate
(analogous to S3's C1 review).

**#1a — Complete the declarations (the safe-gating prerequisite).** Where the
audit finds a Skill needs a context/soft-signal slot it does not declare, ADD
the declaration to the Skill YAML (`server/src/main/resources/skills/*.yaml`)
so the declaration set becomes the complete, intentional source of truth. This
is registry data, not a semantic hardcode. (If a slot is needed by ALL Skills,
that is a signal it should stay unconditional, not gated — record that in the
matrix.)

### Phase B — Convergence (each item gated by its Phase-A audit row)

**#2 — Skill-declared context-key gating.** Make `ContextProjectionBuilder`
read `PhasePlan.requiredContextKeys()` (now complete per #1a) to decide which
context slots to emit, replacing the unconditional/UC-driven emission.
Registry/Skill-driven; **no per-UC if-else / keyword / enum** (§1.7). A slot is
emitted iff a Skill declares it (or it is in the audited always-on set).

**#3 — soft_signal_via_projection gating.** Route the three soft-signal slots
(`alternate_candidate_use_cases`, `discover_disambiguation_signals`,
`prior_use_case_carry`) through `Skill.stateInheritance().softSignalViaProjection()`
rather than the legacy UC/phase logic. The soft signal stays a *signal the LLM
owns*; gating only decides *whether the slot is projected for this Skill*, never
forces an action.

**#4 (CONDITIONAL — decided at S4 close) — C3 dedup/denoise + OQ-S52.4
knowledge_hits canonicalization.** Only if Phase A+B land cleanly with budget
left: the original-S4 C3 (separate eval-trace-contract projection from
LLM-decision projection; merge duplicated `session.*` vs `budget_state`/`phase_plan`;
evaluate `task_summary`, the ~15 always-null §N0 slots, the
`drift_history`/`task_history` re-parse) + the `knowledge_hits` vs
`accumulated_tool_results` canonical-path decision (OQ-S52.4). **Default:
DEFER** these to an S5 or a separate projection-hygiene milestone (see §"Scope
size" below) — do NOT let them expand S4.

**#5 — Tests + the mandatory evidence gates.** Java wiring/rendering tests
proving the Skill-declared gating emits the same slots a correct flow needed
(per the audit) + the soft-signal gating routes correctly. **Mocked-LLM covers
wiring ONLY.** The behaviour-risk gates are BOTH: (a) the **real-LLM bad-case
suite rerun** holding the M4-close distribution, AND (b) a **shadow rerun**
(held-out cases, dev-blind; deliver-agent / human read only) showing no
regression — per milestone §3 S4 "shadow + bad-case rerun mandatory."

## Hard fences / STOP conditions (do NOT do)

- **AUDIT BEFORE GATING**: no context/soft-signal slot is gated (Phase B) before
  its Phase-A audit row confirms the Skill declaration is complete and gating
  will not drop an LLM-visible signal. STOP and surface any unresolved gap
  (the S3 #2 STOP precedent — do not gate on incomplete declarations).
- **Registry/Skill-driven — no per-UC if-else / keyword / enum** (§1.7). Gating
  reads Skill / `PhasePlan` declarations + `stateInheritance`; it adds no UC
  branch. No semantic ownership moves LLM → Java.
- **No change to LLM-visible semantic information** — gating may change *whether
  a slot is projected for a Skill*, never *whether the LLM can see* a semantic
  signal it could see before for the cases that exercise that Skill. The
  real-LLM + shadow reruns are the proof.
- No `escalation_reason` enum / tool-schema-definition / PII / safety /
  grounding-floor change; no `composite.py` / eval-fixture / scoring change; the
  OQ-S51.2 FAQ-grounding overlay stays untouched; the S2 `bot_turn_llm_calls` /
  `/trace` surface stays untouched.
- Do NOT let the CONDITIONAL #4 (C3 + knowledge_hits) expand S4 — default DEFER.
- **STOP and surface to deliver-agent** if any in-scope gating cannot be done
  without removing an LLM-visible signal, OR if the real-LLM / shadow rerun
  regresses the bad-case distribution (in-flight downgrade — do not force the
  change; revert the offending gate and surface).

## Test / eval requirements

- **Phase-A audit** (`docs/diagnostics/m5-s4-skill-declaration-audit.md`)
  reviewed by deliver-agent + human BEFORE Phase B. Phase A alone (audit +
  completed declarations, no gating) is a valid partial close if Phase B proves
  larger than one sub-sprint (deliver-agent + human decide).
- **Java**: `mvn test -B` — no NEW regression vs `1172 / 1-inherited / 0 / 2`
  (the inherited `SystemPromptUserRequestedTiebreakerTest` persists per OQ-S41.5;
  count may grow with new S4 tests).
- **Real-LLM bad-case suite rerun is a MANDATORY S4 evidence gate** (needs
  backend + LLM keys): rerun `eval_interactive/case_specs/bad_cases/` at
  `parallel=1` and confirm the M4-close distribution holds — **PASS×5** (cs001,
  cs014, cs029, cs066, fg5q) + **IMPROVING×4** (alice, cs011, cs012, wmkb) +
  **FAIL×3** (cs015, cs095, iwzx) + **OOSR×0** (isolate any
  session-establishment flake per `R-bad-case-parallel-session-establishment-flakiness`).
- **Shadow rerun is a MANDATORY S4 evidence gate** (per milestone §3 S4):
  rerun the held-out shadow set (`eval_interactive/case_specs_shadow/`;
  dev-blind — deliver-agent / human run + read only) and confirm no regression.
  A regression on either rerun is an in-flight downgrade → STOP.
- **Python**: `cd eval_interactive && uv run python -m pytest --tb=no -q` — no
  NEW regression vs `3 failed, 486 passed` (S4 should not touch eval-harness code).

## §7 — Layer-classification + anti-hardcode stanza

**Target failure layer:** `prompt_projection` (§3.2 Q3) — S4 gates which context
slots + soft signals the per-turn projection surfaces.

**Tier-0 invariant:** adds no Tier-0 invariant. The Constitution's
"prompt_projection owns whether the LLM has the inputs to make a correct
semantic choice" governs; the projection is inside the Runtime's "trace and eval
contract" responsibility (§1.4). No new Java guard.

**Semantic hardcode:** No semantic hardcode introduced. Gating is
**registry/Skill-driven** — the projection reads `PhasePlan.requiredContextKeys()`
(completed in Phase A) + `Skill.stateInheritance().softSignalViaProjection()` to
decide slots, REPLACING legacy unconditional/UC-driven emission. Completing a
Skill's declaration (#1a) is registry data, not a keyword/regex/enum/per-UC
matrix. If any slot cannot be gated without a UC branch or without dropping an
LLM-visible signal, STOP and surface (do not introduce the branch — the S3 #2
precedent).

**Generalization coverage:** target = the in-scope context + soft-signal slots
gate on completed Skill declarations with the bad-case distribution unchanged on
the real-LLM rerun AND no shadow regression. Neighbor = the other bad-case UCs
project their correct slots. Negative = a Skill that does not declare a slot
does not receive it (no spurious projection) AND a Skill that NEEDS a slot still
gets it (no dropped signal — the key risk). Shadow = the held-out set (mandatory
this sub-sprint). Counts finalized at the Phase-A audit (the matrix names which
slots × which Skills × which bad/shadow cases exercise them).

## Codex review plan (§4.3)

**PER-SUB-SPRINT Codex review at S4 close** (S4 is semantic-touching + the
highest-risk M5 sub-sprint; same trigger as S3 per `milestone_objective.md` §8).
The deliver-agent generates `compact/sprint-053-codex-review-prompt.md` at S4
close (§4.1 kernel + §4.2 header) over the S4 commit range, in addition to the
milestone-shared review at M5 close. **Dispatch discipline (per memory
`feedback_milestone_close_bad_case_before_codex`): record the real-LLM + shadow
rerun evidence into `_manifest.md` + handoff §12 AND commit the S4 dev scope
BEFORE dispatching Codex**, else expect a missing-evidence P0 + uncommitted-range
P1 (timing artifacts → re-review). Dev does NOT dispatch Codex.

## Handoff requirements

- Author `docs/sprints/sprint-053-handoff.md`; leave **§12** empty
  (deliver-agent + human at close).
- Ship the Phase-A audit matrix as a committed artifact under `docs/diagnostics/`.
- Record: `git show --numstat`; the Phase-A → Phase-B ordering evidence (audit +
  completed declarations delivered before any gating); the real-LLM bad-case
  rerun + the shadow rerun results dirs + distributions; the §7 self-walk; any
  slot whose gating was STOPPED-and-surfaced rather than forced.

## Commit discipline

Dev stages **only S4 scope**: `server/src/main/java/**` (ContextProjectionBuilder
+ any Skill/PhasePlan wiring it reads), `server/src/main/resources/skills/*.yaml`
(the #1a completed declarations), the Phase-A audit under `docs/diagnostics/**`,
the new Java tests, and NEW `docs/sprints/sprint-053-handoff.md`. **No
`git add -A`** — deliver-agent close-bundle files are bundled by the human at
close.

## Scope size (§8.5 note)

M5 with S4 = **4 sub-sprints** (S1-S4), within the §8.5 5-sub-sprint ceiling.
S4's core is Phase A + #2 + #5 (the convergence completion). The CONDITIONAL #4
(C3 dedup/denoise + knowledge_hits canonicalization) DEFAULTS TO DEFER: if it
warrants a dedicated sub-sprint, that would be S5 — pushing M5 to the
5-sub-sprint ceiling, which is the §8.5 signal to instead spin C3/knowledge_hits
into a **separate "projection hygiene" milestone** rather than overload M5. The
deliver-agent + human decide at S4 close.

## OQ (open questions — filled during the sub-sprint)

- **OQ-S52.1 (#2) + OQ-S52.2 (#5)** — the STOP-surfaced S3 items, now S4 Phase-B
  scope, gated by the Phase-A audit.
- **OQ-S52.4** (`knowledge_hits` dual-path) — CONDITIONAL #4; default deferred.
- _others surfaced during Phase A to be listed here_
