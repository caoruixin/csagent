---
title: Sprint 31 objective — alternate_candidate_use_cases projection slot (Option β implementation)
doc_tier: sprint-archive
status: archived
implementation_status: implemented
source_of_truth: docs/sprints/sprint-031-handoff.md (delivered behaviour); docs/proposals/alternate_uc_signal_data_source_design.md (design freeze)
last_reviewed: 2026-05-16
review_cadence: ad hoc
supersedes: [docs/sprints/sprint-030-objective.md]
superseded_by: docs/sprint_objective.md
notes: >
  Sprint 31 closed 2026-05-16 as PASS path A (deliver-agent + human
  applied; see `docs/sprints/sprint-031-handoff.md` §12). Two commits:
  `2c1fd41` (Sprint 31 ship — BotSession field + V14 Flyway +
  SessionManager AMBIGUOUS-branch assignment + ContextProjectionBuilder
  slot + system_prompt teaching paragraph + 10 new Java tests) and
  `de47635` (§13 fix-iteration append documenting three smoke reruns
  that disambiguated the §10 acceptance gap as external LLM provider
  drift, not Sprint 31 code). All five OQ1–OQ5 pre-picks adhered to.
  Follow-on R-item `R-llm-provider-latency-drift-2026-05-16` opened
  in `docs/action_bank.md` for the latency characterization.
  `R-alternate-uc-signal-data-source` flipped to `done (Sprint 31)`
  in the same action-bank edit.
---

# Sprint 31 — alternate_candidate_use_cases projection slot (Option β)

## 1. Sprint class

**Implementation sprint, single track, semantic-touching.** Layer:
`prompt_projection` per `docs/current/iteration_governance.md` §3.2
Q3 (the LLM is choosing validly within the options it sees, but the
projection is missing a candidate it should see — Option β surfaces
the missing alternate-UC list onto the per-turn projection). **§7
stanza REQUIRED** — pre-filled in
`docs/proposals/alternate_uc_signal_data_source_design.md` §6 (Sprint
31 dev pastes verbatim into this objective on dev session start, OR
into the handoff §10 self-walk, whichever the dev prefers).

## 2. Goal

Ship the Option β design frozen in Sprint 30. Concretely:

- Add a new persisted field `BotSession.intakeAmbiguousCandidates:
  String[]` with a Flyway migration.
- In `SessionManager.createSession`'s AMBIGUOUS routing branch
  (`SessionManager.java:185–189` per design doc §5), assign the
  `routingResult.ambiguousCandidates()` list onto the new field
  before the existing `setCurrentPhase("DISCOVER")` call. This data
  is currently produced and **discarded** — Sprint 31 preserves it.
- In the ROUTED branch (`SessionManager.java:255–259`), leave the
  field null. (Per Sprint 31 pre-pick on Sprint 30 §8 Open Question 1
  — see §5 of this objective.)
- In `ContextProjectionBuilder.buildProjection` (adjacent to the
  existing `candidate_use_cases` block at lines 380–394), add a new
  `alternate_candidate_use_cases` ArrayNode emission, built from
  `session.getIntakeAmbiguousCandidates()` filtered to exclude the
  current `activeUseCase`. Empty array when no snapshot OR when the
  filter removes the only entry.
- In `server/src/main/resources/prompts/system_prompt.txt` (adjacent
  to the existing `already_called` teaching paragraph at lines
  23–28 per the action-bank §5 `R-already-called-prompt-consumption`
  entry), add ONE short teaching paragraph naming:
  - the slot name `alternate_candidate_use_cases`;
  - its provenance (intake-time router snapshot — the UCs the router
    considered plausible for the session's topic-subject family);
  - the soft-signal posture (LLM owns the read decision; runtime
    does not enforce; empty array is the common case; non-empty
    array is observable evidence the LLM may use to inform a topic
    reroute or a clarifying question, but the LLM is not required to
    act on it).
- One new regression-test file (Java, suggested name
  `IntakeAmbiguousCandidatesProjectionTest.java`) with 5–8 tests
  mirroring the Sprint 20 `AlreadyCalledProjectionTest` shape (per
  design-doc §5 last row). Tests assert: (a) AMBIGUOUS intake
  captures the candidates onto `BotSession`; (b) ROUTED intake
  leaves the field null; (c) the projection emits the slot minus
  the active UC; (d) the runtime does NOT branch on the field
  (non-enforcement evidence — mirror Sprint 20
  `AgentRunLoopAlreadyCalledNonEnforcementIntegrationTest`).

## 3. Non-goals (explicit)

- Sprint 31 does NOT implement Option α (extending
  `RuntimeIntentClassifier`).
- Sprint 31 does NOT implement Option γ (new
  `AlternateUseCaseSurveyor` component).
- Sprint 31 does NOT extend `DriftResult` (Option δ).
- Sprint 31 does NOT add LLM structured-reflection step (Option ε).
- Sprint 31 does NOT extend `UseCaseRouter`'s return shape on the
  ROUTED branch — pre-pick on OQ1 keeps the snapshot
  intake-AMBIGUOUS-only (see §5 OQ1).
- Sprint 31 does NOT author CaseSpecs for end-to-end LLM behaviour
  validation. The regression-test surface is Java-level only
  (slot-population + projection-emission + non-enforcement). End-to-
  end behaviour validation against the §7.2 worked example is a
  Sprint 31+1 CaseSpec-authoring sprint per the pre-pick on OQ4
  (see §5 OQ4).
- Sprint 31 does NOT edit `iteration_governance.md` §7.2 in place.
  The §7.2 worked example's "RuntimeIntentClassifier already
  surfaces" clause becomes stale once Sprint 31 ships; the fold-back
  is deferred to the normal 3–5-sprint governance cadence per the
  pre-pick on OQ3 (see §5 OQ3).
- Sprint 31 does NOT close
  `R-prompt-phase-plan-directive-followship` at
  `docs/action_bank.md:450`. That R-item is its own workstream.

## 4. Premise check (deliver-agent verified 2026-05-16)

The Sprint 30 dev session re-verified all 5 §4 premise items from
the Sprint 30 objective at HEAD `df8b8cd`. No drift. The
Sprint 30 design pass surfaced one additional code-grounded fact
that becomes Sprint 31's load-bearing premise:

- **`SessionManager.java:185–189` AMBIGUOUS branch currently
  discards `routingResult.ambiguousCandidates()`.** The data exists
  on `RoutingResult` (a record with an `ambiguousCandidates: List<String>`
  accessor) but the AMBIGUOUS branch reads only the active-UC
  decision and proceeds to `setCurrentPhase("DISCOVER")` without
  preserving the candidates. Sprint 31's edit at this line is the
  smallest change that captures what's already produced.

Sprint 31 dev SHALL re-verify this at session start by reading
`SessionManager.java:180–195` (one read; one paragraph in handoff
§3). If `RoutingResult.ambiguousCandidates()` has been renamed,
removed, or restructured between 2026-05-16 and the dev session
start, STOP and surface the drift; do NOT code under a false premise.

## 5. Pre-picked defaults for the 5 Sprint 30 open questions

Sprint 30 design-doc §8 surfaced 5 open questions the design pass
did not resolve. The deliver agent has pre-picked defaults consistent
with the design-doc recommendation pattern. The human reviews this
section at Sprint 31 objective review and pushes back if any default
is wrong; the dev agent treats this section as **authoritative
unless the human edits it before dev session start**.

### OQ1 — ROUTED-outcome behaviour

**Pre-pick: (a) simple — leave `intakeAmbiguousCandidates` null on
a deterministic ROUTED intake.**

Rationale: matches the design doc's own recommendation; keeps Sprint
31 single-track; the §7.2 UC-A ↔ UC-C shape still falls inside the
intake snapshot's coverage on AMBIGUOUS intakes (both UCs share the
"Replies & Messaging" topic family); future sprint can extend
`UseCaseRouter` to surface considered-but-not-routed alternates on
ROUTED outcomes if observed traces show the simple path is
insufficient.

### OQ2 — System prompt teaching paragraph location

**Pre-pick: (a) sibling to the `already_called` teaching paragraph
at `server/src/main/resources/prompts/system_prompt.txt:23–28`.**

Rationale: the slot is a universal soft signal (not DISCOVER-phase-
specific — it's also useful in CONFIRM / RESOLVE for late topic
shifts the user surfaces); placing it adjacent to `already_called`
groups all "observable-state-the-LLM-reads-but-runtime-does-not-
enforce" teaching together, which matches the Sprint 23 pattern and
reduces the risk that a future prompt edit splits the convention.

### OQ3 — `iteration_governance.md` §7.2 fold-back cadence

**Pre-pick: normal 3–5-sprint governance cadence** per
`docs/current/doc_governance.md` "Fold-back cadence" section.

Rationale: the §7.2 worked example's "RuntimeIntentClassifier
already surfaces" clause becoming stale on Sprint 31 close is a
minor doc-vs-code drift, not a load-bearing falsehood; the fold-
back can happen on the normal cadence (likely Sprint 33 / 34 / 35
window). Sprint 31 does NOT trigger a special fold-back cycle; the
handoff §1.3 doc-status-warnings table SHALL flag the drift for the
fold-back agent to pick up.

### OQ4 — CaseSpec authoring sprint sequencing

**Pre-pick: ship Sprint 31 alone; defer the case-family-authoring
sprint to Sprint 31+1 (separate planning decision).**

Rationale: Sprint 31's Java regression-test coverage is sufficient
to verify the slot ships correctly; end-to-end LLM behaviour
validation against the §7.2 worked example requires authoring
CaseSpecs (target / neighbor / negative / shadow split) per the
Sprint 20 / Sprint 29 corpus pattern — that is its own sprint with
its own §7 stanza and its own scope discipline. Bundling them risks
Sprint 23-style scope sprawl. The Sprint 31 handoff §7 SHALL name
the follow-on case-family sprint as a recommended next direction
without committing to it.

### OQ5 — Interaction with `R-uc-cdf-get-customer-context-bot-actual-usage`

**Pre-pick: Sprint 31 ships first (no sequencing dependency
imposed on the `get_customer_context` follow-on).**

Rationale: Sprint 31's projection landing first gives any future
`get_customer_context` sprint a richer projection surface to read,
but the two R-items are otherwise independent; deliver agent does
not impose a sequencing constraint. Sprint 31 handoff §7 SHALL
note the overlap as informational, not blocking.

## 6. Files in scope (Sprint 31 dev reads verbatim)

Per `docs/proposals/alternate_uc_signal_data_source_design.md` §5
table. Reproduced here for the dev's convenience:

| path | change type |
|------|-------------|
| `server/src/main/java/com/gumtree/csagent/model/BotSession.java` | NEW `@Column(name = "intake_ambiguous_candidates", columnDefinition = "text[]")` field `intakeAmbiguousCandidates: String[]` adjacent to line 53–54 (`candidateUseCases` shape). Null-safe accessors via Lombok `@Data`. |
| `server/src/main/resources/db/migration/V<next>__intake_ambiguous_candidates.sql` | NEW Flyway migration `ALTER TABLE bot_sessions ADD COLUMN intake_ambiguous_candidates text[]`. Sprint 31 picks the next Flyway version number (last shipped was V13 per Sprint 24). Backfill not required — null is the valid "no snapshot" state. |
| `server/src/main/java/com/gumtree/csagent/service/runtime/SessionManager.java` | EDIT line 110 (builder) — add `.intakeAmbiguousCandidates(null)` to the `BotSession.builder()` call (or rely on Lombok null-default if builder semantics permit). |
| `server/src/main/java/com/gumtree/csagent/service/runtime/SessionManager.java` | EDIT line 185–189 (AMBIGUOUS branch in switch) — before `setCurrentPhase("DISCOVER")`, assign `session.setIntakeAmbiguousCandidates(routingResult.ambiguousCandidates() == null ? null : routingResult.ambiguousCandidates().toArray(new String[0]))`. Captures the intake-time alternates currently thrown away. |
| `server/src/main/java/com/gumtree/csagent/service/runtime/SessionManager.java` | OPTIONAL EDIT line 255–259 (ROUTED branch) — per OQ1 pre-pick, leave field null; document choice in handoff §4. |
| `server/src/main/java/com/gumtree/csagent/service/runtime/ContextProjectionBuilder.java` | NEW projection block adjacent to line 380–394 (`candidate_use_cases` projection). Build `ArrayNode` from `session.getIntakeAmbiguousCandidates()` minus the active UC; emit as `alternate_candidate_use_cases`. Empty array when no snapshot OR when filtered result is empty. Schema-stable across turns. |
| `server/src/main/resources/prompts/system_prompt.txt` | NEW short teaching paragraph adjacent to line 23–28 (`already_called` teaching). Per OQ2 pre-pick, sibling placement. Names slot + provenance + soft-signal posture. One paragraph; mirror `already_called` shape. |
| `server/src/test/java/com/gumtree/csagent/service/runtime/IntakeAmbiguousCandidatesProjectionTest.java` (or similar) | NEW regression test file, 5–8 tests, mirroring `AlreadyCalledProjectionTest` shape. Asserts: AMBIGUOUS-captures, ROUTED-null, projection-emits-minus-active, runtime-non-enforcement. |

### 6.1 Sprint-close artefacts (deliver-agent owned)

| path | change type |
|------|-------------|
| `docs/sprint_objective.md` | EDIT (this file; archive at close to `docs/sprints/sprint-031-objective.md`) |
| `docs/10-handoff.md` | EDIT (refresh §1 lead pointer) |
| `docs/action_bank.md` | EDIT (line `R-alternate-uc-signal-data-source` disposition flipped to `done (Sprint 31)`; §6 closed-action index append) |
| `docs/sprints/sprint-031-handoff.md` | NEW (12-section dev-authored archive) |
| `compact/sprint-031-dev-prompt.md` + `compact/sprint-031-review-prompt.md` | deliver-agent-owned planning prompts (authored 2026-05-16) |

## 7. Files NOT in scope (hard fences)

1. **No edits to `RuntimeIntentClassifier.java`** — Option α NOT
   taken; the classifier's per-turn semantic surface stays unchanged.
2. **No edits to `IntentClassification.java`** — no alternates field
   added; Option α NOT taken.
3. **No edits to `DriftResult.java` / `DriftDetector.java`** —
   Option δ NOT taken.
4. **No edits to `UseCaseRouter.java`** — no change to the router's
   return shape per OQ1 pre-pick.
5. **No edits to `ClassifyUseCaseTool.java`** — LLM-facing tool
   stays unchanged.
6. **No new CaseSpecs** under
   `eval_interactive/case_specs/case_families/`. Per OQ4 pre-pick,
   case-family authoring is deferred to Sprint 31+1.
7. **No edits to existing case families** (Sprint 20 / Sprint 29
   cascade fence still in force).
8. **No edits to foundational docs** under `docs/foundational/`.
9. **No edits to governance docs** under `docs/current/`. Per OQ3
   pre-pick, `iteration_governance.md` §7.2 fold-back is deferred
   to the normal cadence; handoff §1.3 SHALL flag the drift.
10. **No edits to sprint archives** under `docs/sprints/sprint-001-*`
    through `sprint-030-*`.
11. **No Tier-0 changes**. Sprint 31 adds no Tier-0 invariant.
12. **No deadline / model / retry / budget config edits**.
13. **Every quantitative claim in the handoff reproducible** per
    `feedback_deliver_agent_cited_numbers_must_be_reproducible.md`.
14. **No mocked-LLM as primary evidence** for any prompt-causal
    change per `feedback_mocked_llm_cannot_prove_prompt_causal_change.md`.
    The Sprint 31 prompt edit is the soft-signal teaching paragraph;
    primary evidence is the smoke rerun observing the new slot in
    `case_results[].per_turn_trace[].projection` (no LLM-behaviour
    claim requires real-LLM proof for Sprint 31 close — the §1.7
    posture is that the LLM owns the read decision and may or may
    not act on the slot, so the smoke rerun's job is to confirm the
    slot is present and populated where the AMBIGUOUS intake fired,
    NOT to prove the LLM acted on it).
15. **No editing of `R-prompt-phase-plan-directive-followship`** at
    `docs/action_bank.md:450`.

## 8. Bundle policy

- Single track, single feature. All §6 file changes land in one dev
  commit (the dev's own commit). The R-item disposition flip in
  `docs/action_bank.md` happens at sprint close (deliver-agent
  owned), NOT in the dev commit.
- Smoke rerun on the 14-case set IS required at sprint close for
  Sprint 31 (semantic-touching sprint per §5.1 Eval Acceptance
  Rules). The smoke confirms (a) no regression on existing
  `composite_score` / pass-rate / safety / grounding floors; (b)
  the new `alternate_candidate_use_cases` slot is present and
  populated on cases that hit the AMBIGUOUS intake branch. The
  smoke's pass-rate IS NOT the primary success metric (Sprint 31
  is a projection-surface ship, not a behaviour-change ship — the
  LLM may or may not act on the new slot in 14-case smoke, and
  either outcome is acceptable as long as no regression on existing
  floors).
- Java test suite SHALL run clean: zero new regressions, only the
  pre-existing inherited failures already documented in Sprint 24 /
  25 / 26 / 27 / 28 handoffs (the `SystemPromptUserRequestedTiebreakerTest`
  inherited from the unauthored `system_prompt.txt` working-tree
  mod — Sprint 31 makes its own change to `system_prompt.txt` so
  the dev SHALL re-verify whether this inherited failure is
  perturbed; surface in handoff §11 if so).

## 9. Layer-classification + anti-hardcode stanza (per §7; REQUIRED)

**Target failure layer:** `prompt_projection` per
`docs/current/iteration_governance.md` §3.2 Q3 — "Did the LLM
choose validly within the available options, but the projection /
context handed to it was wrong or impoverished (missing slot,
missing candidate, missing diagnostic)? → `prompt_projection`." The
LLM today cannot see the intake router's considered-alternate UCs
because the AMBIGUOUS branch discards them at intake; Sprint 31
preserves the data, projects it, and teaches the LLM to read it.

**Tier-0 invariant:** This sprint adds no Tier-0 invariant. The
projection surface is governed by the Runtime's "trace and eval
contract" responsibility per Constitution §1.4; the LLM owns the
semantic decision of whether to act on the slot per §1.3 (drift /
topic shift).

**Semantic hardcode:** No semantic hardcode introduced. The new
`alternate_candidate_use_cases` slot is a soft signal — a faithful
capture of what `RoutingResult.AMBIGUOUS` already produced at
intake, persisted across the session and projected per-turn (minus
the active UC). No keyword / regex / if-else / enum / per-UC matrix
is added to runtime, prompt, judge, or any decision layer. The
runtime does NOT gate any tool dispatch, phase transition, or
escalation decision on the slot's value (verified by the
non-enforcement integration test per §6 last row).

**Generalization coverage:**

- **Target:** UC-A ↔ UC-C drift (the §7.2 worked example shape).
  AMBIGUOUS intakes on the shared "Replies & Messaging" topic-
  subject family produce both UCs in the candidates list; one
  becomes active, the other appears in
  `alternate_candidate_use_cases` for the LLM to read on subsequent
  turns. Java regression test asserts the slot population shape;
  end-to-end LLM behaviour validation is the Sprint 31+1 CaseSpec-
  authoring sprint's job per OQ4 pre-pick.
- **Neighbor:** other UC pairs that share an intake topic-subject
  family (per `docs/foundational/phase2_domain_realization_spec.md`
  topic-family map — the dev SHALL identify ≥ 2 families in
  handoff §6 for the regression-test fixture).
- **Negative:** single-issue session that routes deterministically
  to one UC (ROUTED branch — `intakeAmbiguousCandidates` stays
  null; projection emits empty array). Verifies the slot does NOT
  fire on routed sessions.
- **Shadow:** held-out shadow CaseSpec corpus per the Sprint 20
  Track A G2 split (`eval_interactive/case_specs/case_families/_manifest.yaml`).
  Sprint 31's Java regression-test coverage does NOT consume shadow
  cases (no end-to-end behaviour assertion); shadow coverage is the
  Sprint 31+1 CaseSpec-authoring sprint's surface. Handoff §6 SHALL
  name shadow as "deferred to Sprint 31+1" per OQ4 pre-pick.

## 10. Success metrics

- **Code-paths shipped:** all 8 rows of the §6 table land in the
  dev commit (one row may be optional per the table notes).
- **Java tests pass:** `mvn -q -pl server test` returns zero new
  regressions vs the inherited-failure baseline; the new
  `IntakeAmbiguousCandidatesProjectionTest` 5–8 tests pass.
- **Flyway migration applies cleanly** on a fresh DB and on an
  existing DB with prior schema (idempotent ALTER).
- **Smoke rerun on the 14-case set** (`eval_interactive` smoke set)
  confirms:
  - No regression on `composite_score` / `pass-rate` / safety floor
    / grounding floor / wrong-containment / over-escalation per
    `iteration_governance.md` §5.1.
  - The new `alternate_candidate_use_cases` slot appears in
    `case_results[].per_turn_trace[].projection` on cases that hit
    the AMBIGUOUS intake branch (the dev SHALL identify ≥ 1 such
    case in handoff §5; if zero such cases exist in the 14-case
    set, the dev SHALL note this honestly — the slot is observable-
    only this sprint and the Sprint 31+1 case-family sprint will
    author cases that exercise the AMBIGUOUS branch deliberately).
- **Anti-hardcode self-walk:** handoff §8 walks the §4.1 nine
  questions; the chosen design's soft-signal posture verified
  against §1.7 forbidden list.
- **Reproducibility:** every quantitative claim in handoff cites
  source path + extraction recipe per
  `feedback_deliver_agent_cited_numbers_must_be_reproducible.md`.
- **Codex review** is REQUIRED at sprint close (Sprint 31 is
  semantic-touching: `prompt_projection`-layer + prompt edit). The
  §4.1 kernel applies in full (not exempt).

## 11. Handoff document contract (12 sections)

Standard Sprint 19 / 20 / 23 / 28-shape:

1. Context Pack.
2. Sprint-objective recap.
3. Premise re-verification (the §4 premise item; spot-check
   `SessionManager.java:185–189` AMBIGUOUS branch + `RoutingResult.ambiguousCandidates()`).
4. Implementation walkthrough — each §6 file change cited by line
   range; rationale for the OQ1 ROUTED-null choice; rationale for
   the OQ2 sibling-placement choice in `system_prompt.txt`.
5. Worked-example smoke rerun — run command + result file path +
   per-case extraction of the new slot's presence in
   `per_turn_trace[].projection`.
6. Generalization coverage table — target / neighbor / negative /
   shadow per §9.
7. Open questions for the human — any item the dev surfaces but
   does not act on. Each becomes a possible follow-on R-item.
8. Anti-hardcode self-walk (§4.1 nine questions).
9. Files changed — table with paths + line range / test counts.
10. Layer-classification self-walk per §3 (lands on `prompt_projection`).
11. §5 Eval Acceptance bars — each of the nine bars line by line
    with cited evidence.
12. Closure verdict placeholder — leave for human + deliver agent
    per `feedback_handoff_verdict_section_delegation.md`.

## 12. Stop conditions (dev-agent)

STOP and report (do NOT silently work around) when:

1. **Premise drift on `SessionManager.java:185–189`** — if
   `RoutingResult.ambiguousCandidates()` has been renamed, removed,
   or restructured between 2026-05-16 and dev session start, STOP
   and surface in handoff §1.6 + §3.
2. **Java test regression** — `SystemPromptUserRequestedTiebreakerTest`
   inherited failure baseline changes due to Sprint 31's
   `system_prompt.txt` edit. Investigate, surface, do not silently
   adjust the test.
3. **Smoke regression** — `composite_score` drops on any case vs
   the Sprint 28 reference (`eval_interactive/results/20260514-181257/results.json`),
   OR safety/grounding/wrong-containment/over-escalation floor
   regresses. STOP and propose either (a) a fix iteration, or (b)
   an investigation-only handoff if the regression's root cause is
   not in Sprint 31's diff.
4. **Tempted to extend `UseCaseRouter`** to surface ROUTED
   alternates — STOP. OQ1 pre-pick keeps Sprint 31 single-track.
5. **Tempted to bundle case-family authoring** — STOP. OQ4
   pre-pick defers to Sprint 31+1.
6. **Tempted to extend `iteration_governance.md` §7.2 in place** —
   STOP. OQ3 pre-pick defers to the normal fold-back cadence.
7. **Tempted to add a runtime gate on the slot's value** — STOP.
   §1.7 forbidden list + non-enforcement test surface required.
8. **Tempted to touch `RuntimeIntentClassifier` / `IntentClassification`
   / `DriftResult` / `UseCaseRouter` / `ClassifyUseCaseTool`** —
   STOP. Hard fence per §7.
9. **Tempted to widen scope beyond the §6 table** — STOP and
   propose a follow-on R-item for the surfaced concern.
