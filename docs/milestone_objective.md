---
title: Milestone M-Auto-6 — Runtime substrate hygiene at intake + DISCOVER surfaces (unlock honest autoloop signal)
doc_tier: current-runtime
status: proposal
implementation_status: not_started
source_of_truth: this file + docs/solutions/2026-06-05-runtime-bad-cases-handover-schema-discover-counter.md (research-agent input)
last_reviewed: 2026-06-05
review_cadence: per milestone
supersedes: []
superseded_by: null
notes: >
  Opened 2026-06-05 at M-Auto-5 close (Class A — APPROVE_WITH_NON_BLOCKING_OBSERVATIONS,
  baseline_dir moved to m-auto-5-baseline-20260604-simfixed-stalledfix). M-Auto-5
  established that the eval measurement floor is now honest across all four
  columns. M-Auto-6 attacks the BOT-RUNTIME hygiene gaps that prevent the honest
  measurement from converging into a stable autoloop signal at intake-UC +
  DISCOVER surfaces.

  Source-of-truth proposal:
  `docs/solutions/2026-06-05-runtime-bad-cases-handover-schema-discover-counter.md`
  (Path-2 bad-case-driven, research-agent 2026-06-05, 9 traces c1–c9). It surfaces
  4 runtime/infra R-items (R1 / R2 / R3 / R4) + 5 semantic OBSERVATIONs (deferred
  to autoloop). Code anchors verified at HEAD `021a86a` per the proposal §4.

  **Human-approved packaging (2026-06-05): proposal §6.5 alternative split**:
  - **Sub-sprint A = R1 + R2 + R4** (S-Auto-23 / Sprint 078) — three
    runtime/projection items that share `ContextProjectionBuilder.java`
    edit context; R1 + R4 are `prompt_projection`, R2 is `infra` + `skill_state`.
    Single Codex review (semantic-touching). The falsifiable prediction
    on `reducible-flaky` / uc_f_billing / uc_fp_removed (§5) is measured
    AFTER this sub-sprint's re-bless.
  - **Sub-sprint B = R3** (S-Auto-24 / Sprint 079, drafted as planning
    context) — UI-only admin trace observability (session-list completeness +
    dedup ToolEvent UI folding). Isolated from runtime so it can be
    verified independently; merges with audit Cluster B.1 + C.2.
  Audit Cluster B.2 (`primary_uc` vs `active_use_case` authority) +
  Cluster C.1 (cs59s 400) + Cluster C.3 (generic clarifier branch — already
  largely subsumed by R2.a) remain queued for S-Auto-25+, pending the
  post-A re-bless evidence (route (a) / (b) in §3).

  M-Auto-4's S-Auto-18 (escalation-family tier reshape) stays deferred behind
  M-Auto-6 (to M-Auto-7+).

  **Causal model (proposal §5.1 + §5.3)**: post-M-Auto-5, the eval signal floor
  is honest but two systematic runtime patterns still inject ~draw-noise on
  specific UC surfaces:
    - **Intake-UC handover** (UC-G / UC-H / UC-I / UC-J / UC-K): every
      `request_handover` first call fails `intake_required_fields_missing_for_intake_complete`
      because the projected schema does not declare the `intake_fields` field
      the validator expects; LLM recovers on retry from the hint, but burns ≥ 1
      turn-budget per intake handover + occasional non-recovery (c1 = 6 turns,
      no clean handover). c1/c5/c6 = 3 of 6 intake-UC traces.
    - **DISCOVER clarification loop**: `BudgetChecker.maxClarificationRounds(=2)`
      is dead-code on the live `AgentRunLoopImpl` path; `session.clarificationCount`
      increments only in legacy `PhaseEvaluator.java:880` which the live path
      doesn't reach. Result: DISCOVER has no clarification cap → LLM-self-regulated
      loop depth varies across draws (c3 = 2 verbatim-identical clarifications,
      no dedup, no budget escape). When some bot then hits
      `max-repeated-same-action=2` budget the `mapBudgetToEscalationReason`
      fall-through stamps `turn_budget_exhausted` instead of the more accurate
      `clarification_budget_exhausted` (c9 label-misleading).

  Both are LIVE-path wiring defects, not semantic decisions. Fixing them is
  pure `infra` (R2) + `prompt_projection` (R1 surfaces validator-expected
  schema; required-fields list comes from already-existing
  `IntakeFieldsRegistry`).

  **Forbidden until human authorizes**: editing this file's `status` from
  `proposal` to `current` requires explicit human approval per
  `deliver-agent.md` §Milestone 开始前 (deliver-agent drafts; human reviews + flips).
---

# Milestone M-Auto-6 — Runtime substrate hygiene at intake + DISCOVER surfaces (unlock honest autoloop signal)

## 1. Milestone class

**Class:** runtime substrate-hygiene milestone (Tier-1 mechanical wiring +
prompt_projection contract completion). NOT a semantic milestone — no bot
prompt, no skill yaml, no UC routing, no escalation posture, no judge
calibration, no CaseSpec rubric edited.

**§7 stanza requirement:** REQUIRED at the milestone level + at each
sub-sprint level. Although R2 is pure `infra` and R3 is `infra` (observability),
R1 (`request_handover` schema) and R4 (`customer_context_status` slot) touch
the LLM-facing projection surface → milestone is semantic-touching and the
stanza applies per `iteration_governance.md` §7.

**Layer breakdown** (per `iteration_governance.md` §3 + the proposal §2):

| R-item | Layer | Sub-sprint | Reason |
|---|---|---|---|
| R1 — `request_handover` intake-fields schema declaration | `prompt_projection` | A (#1) | Projected schema lacks the field validator expects; required-fields source already exists in `IntakeFieldsRegistry`. Zero new semantic surface; zero new per-UC matrix in projection (registry contents projected as-is). |
| R2 — DISCOVER clarification counter live-path wiring + budget projection + mapping fix | `infra` + `skill_state` | A (#1) | Dead-code counter that should accumulate across turns on the live path; budget projection added as LLM-facing soft signal (cardinality, not content); mapping fix relabels `max-repeated-same-action` → `clarification_budget_exhausted` (reuse existing enum value). |
| R4 — ad-context premise projection slot (`customer_context_status` enum) | `prompt_projection` | A (#1) | Projects already-known runtime facts (form_context state, lookup tool result) as structured boolean/enum; zero content matching. Shares `ContextProjectionBuilder.java` edit context with R1 → bundled with R1+R2 to give one coherent projection-surface review. Pairs with OBS-S1 (UC-A verify-ad procedure step) deferred to autoloop AFTER M-Auto-6 close. |
| R3 — admin trace observability (session list completeness + dedup ToolEvent UI folding) | `infra` (observability) | B (#2) | Pure UI/ops; zero runtime/Java logic. Isolated to a dedicated sub-sprint so the UI changes can be verified independently of the runtime bundle. Merges with audit Cluster B.1 (per_turn_trace truncation) + C.2 (46f5b2e9 500 + double-send live UI). |

## 2. Goal

Make the post-M-Auto-5 honest measurement floor converge to a STABLE
autoloop signal at the **intake-UC handover** + **DISCOVER clarification**
surfaces by eliminating two LIVE-path runtime wiring defects that
systematically inject draw-noise independent of bot semantic competence.

**Why this milestone exists**: M-Auto-5 closed the eval-honesty floor
(0/414 vacuous-pass + 10 F→P paired-evidence + near-coinflip ELIMINATED).
The post-close baseline `m-auto-5-baseline-20260604-simfixed-stalledfix`
shows 6/12 `reducible-flaky` cases in `bad_cases` — material residual
noise that prevents autoloop semantic optimization from making clean
fitness signals on intake + DISCOVER skill surfaces. The proposal §5.3
attributes that residual primarily to R1 (intake-UC first-call rejection
+ retry) and R2 (DISCOVER loop depth drift). The falsifiable prediction
(§5 below) tests this attribution.

NOT a goal: bot-capability optimization on any UC surface. Semantic
optimization (OBS-S1 UC-A verify-ad procedure step; OBS-S2 DISCOVER
disambiguation cue; OBS-S3 FAQ fidelity at c8 turn 6) is explicitly
deferred to autoloop AFTER M-Auto-6 lowers the measurement noise floor.

## 3. Sub-sprint sequence (planned)

Sequence is PROVISIONAL — each sub-sprint after #1 is scoped only after
the prior sub-sprint's evidence + human review.

### Sub-sprint A — S-Auto-23 / Sprint 078 — R1+R2+R4 bundle (runtime + projection) — PROPOSAL (scope drafted)

R1.a (per proposal §6.1) + R2.a (per proposal §6.2) + R4.a (per
proposal §6.4). All three share the `ContextProjectionBuilder.java`
edit context and present one coherent LLM-facing projection-surface
change to Codex. Dev prompt: `compact/sprint-078-dev-prompt.md`.
Sprint contract: `docs/sprint_objective.md` (active until A closes).

### Sub-sprint B — S-Auto-24 / Sprint 079 — R3 UI/observability — PROPOSAL (scope drafted as planning context)

R3.a (per proposal §6.3) — UI-only admin trace observability: session
list `handling_state` completeness + `TraceViewer.tsx` dedup ToolEvent
folding (with preserved expandable detail to maintain PARAPHRASE_STORM
auditability). Zero runtime/Java. Subsumes audit Cluster B.1
(per_turn_trace truncation) + C.2 (46f5b2e9 500 + double-send live UI)
upon scoping. Dev prompt: `compact/sprint-079-dev-prompt.md` (drafted
as planning context — replaces sprint-078's `docs/sprint_objective.md`
at A close per `deliver-agent.md` §Sub-sprint 完成 → Milestone 内继续).

### Sub-sprint gate (post-A re-bless, between A and B) — falsifiable prediction check

After A closes and the post-A re-bless runs, deliver+human evaluate the
§5 falsifiable hypothesis on `bad_cases` `reducible-flaky` (6/12 →
≤ 2/12 expected) + uc_f_billing / uc_fp_removed (→ stable ~1.00
expected). Two routes:

- **(a) Prediction holds**: B (R3 UI) proceeds as planned. Then S-Auto-25
  Cluster B.2 / C.1 (the remaining audit items not subsumed by A or B).
- **(b) Prediction fails**: pause B; open a re-diagnosis sub-sprint to
  identify the unaddressed residual noise source. Do NOT add semantic
  hardcodes to "force" stability; do NOT widen R1/R2/R4 with content
  heuristics.

Decision is human-gated.

### S-Auto-25 — TBD (Cluster B.2 + C.1 + R-aggregate-retains-per-attempt-composite-l2)

Per the M-Auto-6 audit-cluster routing + the M-Auto-5 Codex
non-blocking observation #3 R-item. May bundle the per-attempt
aggregate retention fix with the residual observability items. Opens
only after A + B land + post-B re-bless reconfirms the floor.

### S-Auto-26+ — TBD (autoloop semantic optimization on cleaned surfaces)

Once A + B (+ optionally S-Auto-25) land and re-bless shows a stable
measurement floor, an autoloop sub-sprint can target OBS-S1 (UC-A
verify-ad procedure step — depends on R4 surface), OBS-S2 (DISCOVER
disambiguation cue), OBS-S3 (FAQ fidelity / c8 turn 6 fabrication).
May be deferred to M-Auto-7.

## 4. Non-goals (explicit)

- **No semantic edits**. No bot prompt, no skill yaml, no UC routing
  cue, no escalation posture, no judge calibration, no CaseSpec rubric.
  OBS-S1 (UC-A verify-ad procedure step), OBS-S2 (DISCOVER
  disambiguation), OBS-S3 (FAQ fidelity / c8 turn 6 fabrication) are
  ALL deferred to autoloop AFTER M-Auto-6 close.
- **No `IntakeFieldsRegistry.java:53-67` field-definition edits.** R1.a
  projects the existing required-fields list as-is; if any UC's
  required-fields set is itself wrong, that's a separate `eval_spec`
  question outside M-Auto-6.
- **No new `escalation_reason` enum value.** Per `D-new-escalation-reason-enum`
  deferred (action_bank §4) + M-Auto-3 §4 verdict. R2.a re-maps
  `max-repeated-same-action` to the existing `clarification_budget_exhausted`
  value, no enum widening.
- **No identical-clarification cross-turn semantic dedup**. Cardinality
  budget only. M-Auto-3 §11 anti-误杀 rank-1 floor preserved (per
  `R-runtime-paraphrase-storm-search-knowledge` close + proposal §5.2).
- **No `SkillGuardrailDispatcher` reject-logic edits.** Validator remains
  the last line of defence; R1.a only adds the LLM-facing contract.
- **No `record_outcome` premature guard edits** (OBS-S4 by-design).
- **No simulator / eval-framework / scoring SHA / autoloop 5-file set
  edits.** Fence-#13 SHA respected.
- **No M-Auto-4 S-Auto-18 (escalation-family tier reshape) work.**
  Deferred to M-Auto-7+.

## 5. Milestone acceptance bar

**Falsifiable hypothesis (from proposal §5.3)**: R1 + R2 are the two
dominant residual noise sources on the intake-UC + DISCOVER surfaces
after M-Auto-5. If true, after R1+R2 lands and a baseline re-bless runs
on the corrected runtime, the following should observably move:

| Signal | Pre-M-Auto-6 (simfixed-stalledfix) | Post-R1+R2 prediction | Falsifier |
|---|---|---|---|
| `bad_cases` `reducible-flaky` count | 6/12 | ≤ 2/12 | If stays > 2/12 and the remaining flaky cases hit intake + DISCOVER, R1+R2 was not the residual noise source — re-diagnose |
| anchor uc_f_billing pass_rate | 0.89 (reducible-flaky) | Toward stable ~1.00 | If drifts ≤ 0.78, R1 was not the residual noise on this case |
| anchor uc_fp_removed pass_rate | 0.89 (reducible-flaky) | Toward stable ~1.00 | Same |
| Intake-UC first-call rejection rate (UC-G / UC-H / UC-I / UC-J / UC-K) | TBD characterization at S-Auto-23 baseline | 0 | If > 0 after R1, schema declaration is not honored as expected |
| DISCOVER clarification cap-hit count | 0 (counter never increments) | > 0 on c3/c9-like cases | If 0 stays after R2, counter wiring did not take |
| Anti-误杀 (persistent high-risk cases) | 0.000 stable on anchor uc_g_gdpr / uc_h_appeal / uc_i_payment / uc_j_safety + shadow cs38s* | UNCHANGED at 0.000 stable | If any rises (an artifact mis-passes), reject the change as masking |

**Hard close gates** (unchanged from `iteration_governance.md` §5.5 +
`process/badcase-lifecycle.md` §5.6):

- Codex §4.1 nine-question kernel pass at milestone close (per-sub-sprint
  Codex review for the semantic-touching R1/R4 bundles is recommended;
  the milestone-shared review remains the formal close).
- Java test suite no new regression vs the documented inherited baseline
  (`1244 / 1 / 0 / 2` post-S-Auto-22).
- Safety floor unchanged (anti-误杀 listed above).
- Grounding floor unchanged.
- Curated bad-case suite manual review (primary gate).

## 6. Hard fences (milestone-level)

1. NO bot semantic / prompt / routing / UC-hypothesis / escalation /
   skill yaml / CaseSpec edit anywhere in M-Auto-6.
2. NO `IntakeFieldsRegistry` content changes; R1.a projects existing
   contract as-is.
3. NO new `escalation_reason` enum values; R2.a re-maps to existing
   `clarification_budget_exhausted`.
4. NO identical-clarification cross-turn content/semantic dedup; R2.a
   is cardinality-only.
5. NO `SkillGuardrailDispatcher` reject-logic edits.
6. NO simulator / eval-framework / scoring SHA / autoloop 5-file set
   edits.
7. NO M-Auto-4 S-Auto-18 work.
8. NO autoloop semantic optimization sub-sprint until M-Auto-6 close +
   measurement-floor re-verification.

## 7. R-items consumed / surfaced

**Consumed at milestone open** (queued from M-Auto-5 close + the 2026-06-05
proposal):

- **Sub-sprint A (S-Auto-23 / Sprint 078):**
  - `R-request-handover-intake-fields-schema-projection` (R1; proposal §4.1; layer `prompt_projection`).
  - `R-discover-clarification-counter-live-wireup` (R2; proposal §4.2; layer `infra` + `skill_state`; merges with M-Auto-6 Cluster C.3 "generic clarifier branch wasting opening turn").
  - `R-ad-context-premise-projection-slot` (R4; proposal §4.4; pairs with OBS-S1 deferred to autoloop).
- **Sub-sprint B (S-Auto-24 / Sprint 079):**
  - `R-admin-trace-observability-session-list-and-dedup-folding` (R3; proposal §4.3; merges with M-Auto-6 Cluster B.1 + C.2).

**Queued for S-Auto-25+:**

- `R-aggregate-retains-per-attempt-composite-l2` (from M-Auto-5 Codex non-blocking observation #3).
- M-Auto-6 Cluster B.2 (`primary_uc` vs `active_use_case` mismatch; authority undecided; needs research-agent decision sub-sprint).
- M-Auto-6 Cluster C.1 (cs59s session_create 400; research-first short).
- M-Auto-6 Cluster C.3 residual (most subsumed by R2.a in A; if anything remains, here).

**Status carry-forward**: M3-B P0; M-Auto-1A carry-overs (7);
`R-runtime-escalation-reason-turn-budget-conflated-with-intent` (B3/R5);
`R-classifier-non-deterministic-uc-selection-at-temp-zero` (C/R7);
`R-autoloop-run-sweeps-dirty-index` standing hazard;
`R-eval-interactive-judge-score-never-populated` (chronic LOW). M-Auto-4
PAUSED.

## 8. Codex review plan (per §4.3)

**Per-sub-sprint Codex review** is **REQUIRED** for **Sub-sprint A**
(S-Auto-23; R1+R2+R4 bundle) because both R1.a and R4.a touch the
LLM-facing projection surface (`prompt_projection` layer). R2.a alone
would be exempt as pure infra, but bundled with R1.a / R4.a triggers
semantic-touching review per `iteration_governance.md` §7 +
`process/milestone-framework.md` §4.3.

**Sub-sprint B** (S-Auto-24; R3 UI-only) is pure `infra` (observability)
and §7-EXEMPT per the proposal §7.2 worked stanza; per-sub-sprint Codex
review is OPTIONAL for B (UI changes are visually verifiable). The
milestone-shared Codex at M-Auto-6 close covers both A and B
cumulatively.

The dev-prompt + Codex-prompt artifacts must each be self-contained per
`prompt-artifact-rules.md` §9.1-§9.6; both will embed:

- Full §4.1 nine-question kernel (NOT a reference).
- Full §7 stanza (Target failure layer / Tier-0 invariant / Semantic
  hardcode / Generalization coverage).
- Anti-误杀 invariants (anchor uc_g/h/i/j/safety + shadow cs38s* must
  stay at 0.000 stable).
- File-path fence enumerating allowed edit surface for the sub-sprint.

**Milestone-shared Codex review** at M-Auto-6 close covers the cumulative
commit range per `iteration_governance.md` §4.3.

## 9. Estimated duration

**Sub-sprint A (S-Auto-23 / Sprint 078; R1+R2+R4)**: ~1-2 dev sessions
per the proposal §6.5 estimate (~190 LOC + ~160 test LOC; ~7 files:
`ContextProjectionBuilder.java` for R1+R2 budget+R4, `AgentRunLoopImpl.java`,
`ControlKernel.java`, plus 3-4 new test classes). Plus real-LLM PRE-fix
characterization + post-fix HUMAN-LAUNCHED re-bless (real-LLM, multi-suite).

**Sub-sprint B (S-Auto-24 / Sprint 079; R3 UI-only)**: ~1 dev session
per the proposal §6.5 estimate (~80 LOC UI + ~60 test LOC; 2 main files:
`SessionList.tsx`, `TraceViewer.tsx`, plus UI test files). Visual
verification + admin-trace smoke evidence (no re-bless required).

**M-Auto-6 total**: dependent on whether route (a) or (b) fires at A
close. Best case (route (a) + clean A + clean B + S-Auto-25 Cluster B.2 /
C.1 / aggregate-retention bundle): ~3-4 sub-sprints. Worst case (route
(b) re-diagnosis after A): +1 sub-sprint.

## 10. Stop conditions (milestone-level)

- **Re-bless evidence contradicts the falsifiable hypothesis AND
  diagnosis converges on a non-runtime cause** (e.g. the residual flake
  is genuine bot semantic ambiguity, not wiring): downgrade M-Auto-6 to
  Class C — In-flight downgrade; route remaining residual to autoloop
  semantic milestone.
- **Codex per-sub-sprint review surfaces blocking findings that cannot
  converge in ≤ 2 fix-iterations**: stop the milestone, human review
  required.
- **Anti-误杀 violation** (any persistent high-risk case mis-passes due
  to an R1/R2 change): immediate revert + diagnose; potential downgrade.
- **Eval CaseSpec misalignment with the R2.a `clarification_budget_exhausted`
  re-mapping is wider than ~3 affected cases**: triage as `eval_spec`
  question; may require an interim spec-sync sub-sprint before R2.a ships.

## 11. Cross-milestone sequencing

- M-Auto-5 (closed 2026-06-05) — eval-honesty floor LIVE; baseline_dir
  flipped to `m-auto-5-baseline-20260604-simfixed-stalledfix`.
- M-Auto-6 (this milestone) — runtime substrate hygiene at intake +
  DISCOVER surfaces; unlocks honest autoloop signal.
- M-Auto-7+ — candidate: autoloop semantic optimization on cleaned
  surfaces (OBS-S1 / OBS-S2 / OBS-S3) OR M-Auto-4 S-Auto-18 resumption
  (escalation-family tier reshape) OR M3-B Single Handover Orchestrator
  P0. Decision deferred to M-Auto-6 close.
