---
title: Sprint 30 objective — Alternate-UC signal data-source design (investigation-only, docs-only)
doc_tier: sprint-archive
status: archived
implementation_status: historical
source_of_truth: docs/sprints/sprint-030-handoff.md (delivered behaviour); docs/proposals/alternate_uc_signal_data_source_design.md (design freeze)
last_reviewed: 2026-05-16
review_cadence: ad hoc
supersedes: [docs/sprints/sprint-029-objective.md]
superseded_by: docs/sprint_objective.md
notes: >
  Sprint 30 is a docs-only architectural-design sprint. It produces ONE
  proposal doc that names the chosen data source for the future
  `alternate_candidate_use_cases` projection slot named in
  `docs/current/iteration_governance.md` §7.2 worked example. Sprint 30
  ships ZERO runtime / prompt / Java / Python code. Sprint 31 will ship
  the chosen design once the human approves Sprint 30's recommendation.
  Sprint 30 is **exempt** from the §7 semantic-touching stanza per
  `docs/current/iteration_governance.md` §7 (docs-only sprint, Sprint 16
  precedent — see `docs/proposals/handover_orchestrator_design.md`).
---

# Sprint 30 — Alternate-UC signal data-source design (investigation-only)

## 1. Sprint class

**Investigation-only / docs-only design sprint.** Single track. Single
deliverable: one new proposal doc plus one R-item registration in
`docs/action_bank.md`. Sprint 30 is **exempt** from the §7
semantic-touching stanza per `docs/current/iteration_governance.md` §7
("Pure infra, docs-only, config-governance, and characterization-test
sprints are exempt"). Precedent: Sprint 16 handover-orchestrator
design freeze (`docs/proposals/handover_orchestrator_design.md`),
Sprint 27 directive-shape probe (docs-only investigation).

## 2. Goal

Decide where the data for the future `alternate_candidate_use_cases`
projection slot comes from. The slot was named in
`docs/current/iteration_governance.md` §7.2 hypothetical worked
example as a soft signal "the existing `RuntimeIntentClassifier`
already surfaces" — a planning-turn premise check (deliver-agent
2026-05-16) found the asserted data source does **not** exist in
current code (see §4 below). Sprint 30 produces a design doc that:

- enumerates candidate data sources;
- recommends one with rationale;
- pre-specifies the code-paths-to-touch for Sprint 31; and
- classifies the chosen design's layer per
  `docs/current/iteration_governance.md` §3.

The design doc IS the deliverable. Sprint 30 ships **no** code, **no**
prompt change, **no** CaseSpecs, **no** test files.

## 3. Non-goals (explicit)

- Sprint 30 does NOT implement the projection slot. That is Sprint 31
  (or the sprint that ships the chosen design — naming is the next
  deliver-cycle's job).
- Sprint 30 does NOT edit `ContextProjectionBuilder.java`,
  `RuntimeIntentClassifier.java`, `IntentClassification.java`,
  `DriftResult.java`, `BotSession.java`, `UseCaseRouter.java`, or any
  other Java / Python source file under `server/` or
  `eval_interactive/`.
- Sprint 30 does NOT edit `system_prompt.txt` or any other prompt
  asset under `server/src/main/resources/prompts/`.
- Sprint 30 does NOT author CaseSpecs.
- Sprint 30 does NOT run smoke / Java tests / Python tests for
  evidence (no code is being changed; there is nothing to evidence).
- Sprint 30 does NOT pre-commit Sprint 31's scope. Sprint 31 scope is
  the next deliver-cycle's job after the human reviews Sprint 30's
  recommendation.
- Sprint 30 does NOT touch `R-prompt-phase-plan-directive-followship`
  at `docs/action_bank.md:450`. That R-item is its own workstream.
- Sprint 30 does NOT extend `docs/current/iteration_governance.md`
  §7.2 in place. The §7.2 worked example will be reconciled with
  Sprint 30's findings on the next governance fold-back cadence
  (`docs/current/doc_governance.md` "Fold-back cadence"), not by this
  sprint.

## 4. Premise check (verified by deliver-agent 2026-05-16)

The §7.2 worked example asserts: *"the new
`alternate_candidate_use_cases` projected slot is a soft signal — a
list of UCs the existing `RuntimeIntentClassifier` already surfaces —
exposed to the LLM through the per-turn projection."*

Empirical premise check, against HEAD = `df8b8cd`:

1. **`RuntimeIntentClassifier.classify()` returns ONE
   `IntentClassification` with ONE `predictedUseCase`** at
   `server/src/main/java/com/gumtree/csagent/service/runtime/RuntimeIntentClassifier.java:160–269`.
   No alternates list is returned.
2. **`IntentClassification`** at
   `server/src/main/java/com/gumtree/csagent/model/IntentClassification.java`
   has no `alternates` / `candidates` field. `grep -n "alternates\|candidates"
   IntentClassification.java` returns nothing.
3. **`DriftResult`** at
   `server/src/main/java/com/gumtree/csagent/model/DriftResult.java:21–24`
   has fields `{type, newUseCase, escalationRequested}` only — single
   `newUseCase`, no alternates list.
4. **`BotSession.candidateUseCases`** at
   `server/src/main/java/com/gumtree/csagent/model/BotSession.java`
   exists as a `String[]` but is **collapsed to a single-element array
   `[targetUc]` after the first reroute** by
   `ControlKernel.applyRerouteDecision` at
   `server/src/main/java/com/gumtree/csagent/service/runtime/ControlKernel.java:686`
   (and again at line 1201 in the fallback path). On every
   post-DISCOVER turn `candidateUseCases - activeUseCase = []`.
5. **`ContextProjectionBuilder` already projects the existing
   `candidate_use_cases` slot** at lines 380–394 (Sprint 7 §I0 cue).
   That slot's data is the same `BotSession.candidateUseCases` field
   — it is **not** a per-turn alternates list.

**Conclusion.** The §7.2 worked example was aspirational about the
data source. Sprint 30's job is to repair this premise gap by naming
where alternates actually come from before Sprint 31 ships any
projection slot.

## 5. Sprint 30 deliverables

### 5.1 Implement only

- One new proposal doc under `docs/proposals/`. Recommended name:
  `alternate_uc_signal_data_source_design.md`. Front matter required
  per `docs/current/doc_governance.md` (`doc_tier: foundational` or
  `proposal`; `status: proposal`; `implementation_status:
  not_started`; `source_of_truth: this file`; `last_reviewed:
  2026-05-XX`; `review_cadence: ad hoc`).
- One new R-item registered in `docs/action_bank.md` (append at the
  appropriate section; do not edit existing R-items). Recommended
  name: `R-alternate-uc-signal-data-source`. Disposition: `proposal
  (Sprint 30 design freeze; Sprint 31 implements)`.
- A 12-section sprint handoff at `docs/sprints/sprint-030-handoff.md`
  at close, with the standard contract (see §11).

### 5.2 Design doc minimum content

The design doc SHALL contain:

1. **Problem statement.** Restate §4 premise check verbatim — the
   §7.2 worked example's data-source assertion does not hold against
   current code; the soft-signal slot needs an upstream source.
2. **Candidate data sources.** Enumerate at least three credible
   options. Each option SHALL include:
   - One-paragraph description.
   - Code paths the option would touch (file:line ranges).
   - Layer classification per
     `docs/current/iteration_governance.md` §3.
   - Anti-hardcode posture: is this option a **soft signal** (LLM
     reads, decides; no Java branch on the value) or a **hard
     branch** (Java acts on the value)? Soft signal is required;
     hard branches are §1.7 forbidden.
   - Estimated Sprint 31 size (small / medium / large) and the
     two-track-vs-single-track recommendation.
   - Risk: what could break in adjacent surfaces (per-turn cost,
     per-turn determinism, eval-trace shape, drift-detector
     interaction).
3. **Recommendation.** Pick one option. Justify the pick against the
   Constitution §1.3 (LLM owns drift / topic shift) + §1.5
   (iteration rule) + §1.7 (forbidden list).
4. **Code-paths-to-touch list (for the chosen option).** A table
   with file:line ranges and proposed change type (additive field on
   model / new method / new projection branch / etc.). Sprint 31 dev
   reads this table verbatim — be precise.
5. **Layer-classification + anti-hardcode pre-walk for Sprint 31.**
   Pre-fill the §7 stanza Sprint 31 will need: target layer, Tier-0
   posture, semantic-hardcode posture, generalization coverage
   target/neighbor/negative/shadow per the chosen option's behaviour.
6. **Out-of-scope (for Sprint 31).** Name what Sprint 31 will NOT
   ship even after the data-source design lands (e.g. shadow planner
   mode is OUT — that is a separate, larger sprint).
7. **Open questions for the human.** Any item that the design pass
   could not decide without product / architecture input. Each open
   question SHALL name the impasse and the decision the human is
   being asked to make.

### 5.3 Candidate data sources to consider (informational; not prescriptive)

The dev SHALL design from first principles. The bullets below are
informational; the dev MAY add other candidates the design pass
surfaces. Anti-hardcode discipline (§1.5 / §1.7) applies to every
option.

- **Option α — Extend `RuntimeIntentClassifier` to return a ranked
  alternates list.** New field on `IntentClassification` (e.g.
  `List<String> alternateUseCases`); the classifier's existing
  per-rule branches optionally surface alternates rather than
  collapsing to a single `predictedUseCase`. Layer:
  `semantic_planner` (per §3.2 Q5; the classifier is a runtime
  semantic decision surface).
- **Option β — Run `UseCaseRouter` per-turn (currently runs at
  intake / form-context only) and surface its top-k as alternates.**
  Layer: `infra` (re-running an existing classifier) + `skill_state`
  (per-turn cache to avoid double-cost).
- **Option γ — New `AlternateUseCaseSurveyor` component that
  consumes the per-turn projection plus the user message and returns
  a ranked list.** Layer: `semantic_planner` (new semantic surface)
  + `prompt_projection` (slot publication).
- **Option δ — Extend `DriftResult` to carry alternates from the
  drift detector.** Layer: `semantic_planner` (DriftDetector is a
  runtime semantic decision surface; widening its output is a
  semantic-planner change).
- **Option ε — Source from the LLM itself via a structured
  reflection step on the prior turn.** Layer: `semantic_planner` +
  per-turn cost concern. Probably out of scope for this design pass;
  the dev may rule it out with a one-line rationale.

The dev SHALL evaluate at least three of α / β / γ / δ / ε and pick
one. The dev MAY add new options the design pass surfaces.

### 5.4 Anti-hardcode posture (hard gate on the recommendation)

The chosen option MUST be a **soft signal** per Constitution §1.3
(LLM owns drift) + §1.7 ("do not fix semantic failures by adding
keyword / regex / if-else / enum / per-UC matrix unless a Tier-0
invariant is broken"). Specifically:

- **No keyword / regex / if-else** in the data-source pipeline that
  would synthesise an alternate UC from a phrase.
- **No per-UC matrix** that maps "if active UC is X and message
  contains Y, alternate is Z."
- **No Tier-0 invariant added.** Sprint 30 design SHALL NOT propose
  a new Tier-0 invariant; if the chosen option requires one, that is
  a `human_review_required` exit per §3.2 default tail.
- **No promotion of `RuntimeIntentClassifier`'s existing pattern
  set to a "list".** The classifier today fires one branch per
  pattern match; promoting that to "all matching patterns become
  alternates" is a semantic shift the design SHALL discuss, not
  silently bake in.

If the design pass concludes that no option satisfies the soft-signal
bar, the recommendation SHALL be `human_review_required` per §3.2
default tail — surface the failure honestly and let the human decide
whether to (a) add a Tier-0 invariant, (b) accept a `prompt_projection`
post-hoc derivation that risks staleness, or (c) defer the slot
entirely.

## 6. Files in scope

| path | change type |
|------|-------------|
| `docs/proposals/alternate_uc_signal_data_source_design.md` | NEW — design doc per §5.2 |
| `docs/action_bank.md` | EDIT — append `R-alternate-uc-signal-data-source` per §5.1 |
| `docs/sprints/sprint-030-handoff.md` | NEW (at close) — 12-section handoff per §11 |

### 6.1 Sprint-close artefacts (deliver-agent owned, not part of dev commit)

| path | change type |
|------|-------------|
| `docs/sprint_objective.md` | EDIT (this file; archive at close to `docs/sprints/sprint-030-objective.md`) |
| `docs/10-handoff.md` | EDIT (refresh §1 lead pointer) |
| `compact/sprint-030-dev-prompt.md` + `compact/sprint-030-review-prompt.md` | deliver-agent-owned planning prompts (already authored 2026-05-16) |

## 7. Files NOT in scope (hard fences)

1. **No edits to `server/`** — zero Java code change.
2. **No edits to `eval_interactive/`** — zero Python code change.
3. **No edits to `server/src/main/resources/prompts/`** — zero
   prompt change.
4. **No new CaseSpecs** under
   `eval_interactive/case_specs/case_families/` or any other
   case-spec location.
5. **No edits to existing case families** (Sprint 20 cascade fence,
   Sprint 29 corpus). Same fence as Sprints 21–29.
6. **No edits to foundational docs** under `docs/foundational/`.
7. **No edits to governance docs** under `docs/current/`. The §7.2
   worked-example fold-back is deferred to the normal cadence per §3.
8. **No edits to sprint archives** under `docs/sprints/sprint-001-*`
   through `sprint-029-*`.
9. **No Tier-0 changes**. Sprint 30 adds no Tier-0 invariant.
10. **No deadline / model / retry / budget config edits**.
11. **No smoke run, no Java test run, no Python test run for
    evidence.** Sprint 30 ships no code; there is nothing to test.
    The dev MAY run `find` / `grep` / `cat` for design pass research,
    but no `mvn test` / `pytest` / `eval-interactive run`.
12. **Every quantitative claim in the design doc and handoff
    reproducible** per `feedback_deliver_agent_cited_numbers_must_be_reproducible.md`
    — cite source path + extraction command for every number.
13. **No mocked-LLM evidence.** Sprint 30 ships no LLM-touching
    code; mocked-LLM is not a concern this sprint, but if the
    design discusses an LLM-using option (Option ε), the design
    SHALL note that primary evidence in the future Sprint 31 would
    require real-LLM execution.
14. **No editing of `R-prompt-phase-plan-directive-followship` at
    line 450.** That R-item is its own workstream.

## 8. Bundle policy

- Single deliverable (the design doc) lands as one commit. The
  R-item registration in `docs/action_bank.md` may be the same
  commit or a follow-on; dev's choice. Document in handoff §3.
- No mid-sprint scope expansion. If the design pass surfaces a
  follow-on need (e.g. a prerequisite eval-harness change to
  observe alternates in trace), the dev SHALL name it in handoff §7
  as a proposed follow-on R-item — NOT open it, NOT ship it this
  sprint.

## 9. Layer-classification stanza (exempt; informational)

Sprint 30 is **exempt** from the §7 semantic-touching stanza
(docs-only). The Sprint 30 design pass IS, however, asked to
**pre-fill** the §7 stanza Sprint 31 will need (per §5.2 item 5);
that pre-fill lives in the design doc, not in this objective.

For the deliver-agent's own audit trail: Sprint 30 itself classifies
as `infra` per §3.2 Q1 (docs-only sprint; nothing crashes;
deterministic output). The design doc's RECOMMENDATION will likely
land on `semantic_planner` for Sprint 31 — that is expected and is
exactly why Sprint 30 exists (to scope Sprint 31's semantic surface
honestly before Sprint 31 ships).

## 10. Success metrics

- **Design doc exists** at `docs/proposals/alternate_uc_signal_data_source_design.md`
  with all seven §5.2 sections.
- **At least three candidate data sources evaluated** in §5.2 item 2.
- **One recommendation** in §5.2 item 3 with explicit Constitution
  citation (or `human_review_required` per §5.4 if no option
  satisfies the soft-signal bar).
- **Code-paths-to-touch table** in §5.2 item 4 with file:line ranges
  precise enough that Sprint 31 dev does NOT re-investigate.
- **R-item registered** at `docs/action_bank.md` with disposition
  `proposal (Sprint 30 design freeze)`.
- **No code committed.** `git diff --name-only` against the merge
  base shows zero files under `server/`, `eval_interactive/`, or
  `server/src/main/resources/prompts/`.
- **Reproducibility:** every cited code path verified at session
  start (file exists, line range matches). Cite `wc -l` or `head`
  output where useful.
- **Codex review** is **OPTIONAL** for Sprint 30 per
  `iteration_governance.md` §4.1 docs-only exemption + Sprint
  16/22/26/27 precedent. The deliver-agent may skip Codex review at
  close; if requested, Codex reviews the design doc against §5.2 +
  §5.4 anti-hardcode posture (kernel exempt).

## 11. Handoff document contract (12 sections)

The dev SHALL produce `docs/sprints/sprint-030-handoff.md` with:

1. Context Pack (relevant docs, code paths sampled, doc-status
   warnings, source-of-truth decision, implementation status, risks).
2. Sprint-objective recap.
3. Premise re-verification — confirm §4 premise items at session
   start (re-grep, re-read line ranges); surface drift if found.
4. Design pass walkthrough — for each candidate evaluated, the
   one-paragraph why-considered + why-recommended-or-ruled-out.
5. Recommendation summary — restate §5.2 item 3 recommendation +
   Constitution citation.
6. Code-paths-to-touch table — restate §5.2 item 4 table for the
   chosen option.
7. Open questions for the human — Sprint 31 scope decisions, design
   ambiguities the design pass could not resolve.
8. Anti-hardcode self-walk — confirm §5.4 hard gate; if the
   recommendation is `human_review_required` per §5.4, document why.
9. Files changed — diff scope; pre-existing untouched mods.
10. Layer-classification self-walk per §3 (Sprint 30 itself + the
    Sprint 31 prospective).
11. R-item registration — exact text appended to
    `docs/action_bank.md`, disposition recorded.
12. Closure verdict (filled at close; deliver-agent owned per
    `feedback_handoff_verdict_section_delegation.md`).

## 12. Stop conditions (dev-agent)

- **Premise drift** — if §4 premise items don't verify at session
  start (e.g. Sprint 29-or-later commit added an alternates field I
  missed), STOP and report. The design problem may have changed.
- **Code-edit temptation** — if the design pass surfaces a "trivial"
  one-line code addition that would let the dev validate the design,
  STOP. Sprint 30 is docs-only; validation belongs in Sprint 31.
- **Sprint 31 objective draft temptation** — STOP. Drafting Sprint
  31 scope is the next deliver-cycle's job.
- **§7.2 worked-example edit temptation** — STOP. The
  `iteration_governance.md` fold-back is a separate cadence per §3.
- **Tier-0 invariant addition temptation** — STOP. If the chosen
  option requires a new Tier-0 invariant, surface it as
  `human_review_required` per §5.4 and §3.2 default tail; do not
  invent a new Tier-0 invariant in a docs-only sprint.
- **CaseSpec authoring temptation** — STOP. Sprint 29's authoring
  surface stays untouched.
- **Smoke / Java-test / Python-test temptation** — STOP. No code
  change; no evidence run is needed or appropriate.
