---
title: Alternate-UC signal data-source design
doc_tier: proposal
status: proposal
implementation_status: not_started
source_of_truth: this file
last_reviewed: 2026-05-16
review_cadence: ad hoc
supersedes: []
superseded_by: null
notes: >
  Design freeze authored Sprint 30 (docs-only). Names the data source
  for the future `alternate_candidate_use_cases` projection slot whose
  hypothetical was sketched in `docs/current/iteration_governance.md`
  §7.2. Sprint 31 implements the chosen design.
---

# Alternate-UC signal data-source design

Date: 2026-05-16
Sprint: Sprint 30 — Alternate-UC signal data-source design (docs-only)
Status: **design freeze (docs only); no runtime change in Sprint 30**

## 1. Purpose

`docs/current/iteration_governance.md` §7.2 (the hypothetical worked
example for the `cs_example_001` UC-A ↔ UC-C topic-shift failure
brief) sketches a future projection slot called
`alternate_candidate_use_cases` and describes it as *"a soft signal —
a list of UCs the existing `RuntimeIntentClassifier` already surfaces
— exposed to the LLM through the per-turn projection."* The Sprint 30
planning-turn premise check (deliver-agent 2026-05-16) found the
asserted upstream data source does not exist in current code: the
classifier returns one UC per call, no model carries an alternates
list, and `BotSession.candidateUseCases` collapses to a single-element
array on every post-DISCOVER turn.

This document is the Sprint 30 design freeze. It (a) re-states the
premise gap with reproducible cites, (b) enumerates five candidate
data sources for the future slot, (c) recommends one option subject
to the Constitution §1.3 / §1.5 / §1.7 anti-hardcode bar, (d) lists
the precise file:line ranges Sprint 31 will touch under the
recommendation, and (e) pre-fills the §7 layer-classification +
anti-hardcode stanza Sprint 31 will need.

Sprint 30 ships zero code, zero prompt change, zero CaseSpec change,
zero Tier-0 invariant. Sprint 31 (or whichever sprint the deliver
agent picks up the recommendation in) implements the chosen design.

## 2. Premise gap — what §7.2 assumed vs what code actually does

Each item below cites `file:line` at HEAD `df8b8cd` and an exact
shell invocation that confirms it. All five re-verified at Sprint 30
session start (2026-05-16); none drifted from the planning-turn
premise check.

### 2.1 `RuntimeIntentClassifier.classify()` returns ONE UC per call

`RuntimeIntentClassifier.java:160–269`. Six conditional branches fire
in priority order (explicit-human / distress → UC-J risk → UC-C
soft-shift → UC-A shapes → `DriftDetector` HARD_SHIFT fallback);
each builds and returns a single `IntentClassification`. No path
accumulates a list across branches.

Reproduce: `grep -nE "return IntentClassification\." server/src/main/java/com/gumtree/csagent/service/runtime/RuntimeIntentClassifier.java`
returns ~12 `return` sites (lines 163 / 173 / 176 / 187 / 198 / 201 /
229 / 232 / 240 / 244 / 264 / 268), each emitting one
`IntentClassification`.

### 2.2 `IntentClassification` has no `alternates` / `candidates` field

`IntentClassification.java` is a six-field record (`predictedUseCase`,
`confidence`, `relation`, `taskType`, `primaryEntityType`,
`primaryEntityValue`); 76 LOC.

Reproduce: `grep -cn "alternates\|candidates" server/src/main/java/com/gumtree/csagent/model/IntentClassification.java`
returns `0`.

### 2.3 `DriftResult` is single-target

`DriftResult.java:12–26` carries `DriftType type`, `String newUseCase`,
`boolean escalationRequested`. The detector
(`DriftDetector.java:55–94`) walks two keyword passes (escalation
pattern first, then `HardShiftGroup` keywords in order) and returns
at first match.

Reproduce: `grep -cn "alternates\|candidates" server/src/main/java/com/gumtree/csagent/model/DriftResult.java`
returns `0`. File length 27 LOC.

### 2.4 `BotSession.candidateUseCases` collapses to single-element

`BotSession.java:53–54` declares the field as a `text[]` column —
shape-capable of many UCs. Both writer sites in
`ControlKernel.applyRerouteDecision` install a single-element array
guarded by "if null or empty":

- `ControlKernel.java:684–687` — reroute branch installs
  `new String[]{targetUc}`.
- `ControlKernel.java:1199–1202` — fallback path
  (`applyMissingUseCaseFallback`) installs `new String[]{fallbackUc}`.

Neither expands an existing list. Critically, at intake time
`RoutingResult.AMBIGUOUS` already carries a multi-element
`ambiguousCandidates: List<String>` field
(`UseCaseRouter.java:767–769`), but `SessionManager.createSession`
at lines 185–189 does NOT assign it to
`session.setCandidateUseCases(...)`; it falls through to lines
197–199's "ensure not null" guard that installs `new String[0]`.
**This is the key Option-β data source the design recommends
capturing.**

Reproduce: `grep -n "setCandidateUseCases" server/src/main/java/com/gumtree/csagent/service/runtime/{ControlKernel,SessionManager,UseCaseRouter}.java`
returns 3 writer sites (`ControlKernel.java:686`,
`ControlKernel.java:1201`, `SessionManager.java:110` — the
`BotSession.builder().candidateUseCases(new String[0])` initialiser).
None assigns a multi-element array from observable data.

### 2.5 `ContextProjectionBuilder` already projects `candidate_use_cases`

`ContextProjectionBuilder.java:380–394` iterates
`session.getCandidateUseCases()` into an `ArrayNode` named
`candidate_use_cases`. Same data source as §2.4 — single-element on
every post-DISCOVER turn (or empty if DISCOVER ran without
`classify_use_case` having fired). The system prompt references the
slot once at `system_prompt.txt:32` (DISCOVER-phase guidance only);
no RESOLVE / CONFIRM / CLOSE consumer.

Reproduce: `grep -n "candidate_use_cases" server/src/main/java/com/gumtree/csagent/service/runtime/ContextProjectionBuilder.java`
returns one hit at line 394. `grep -n "candidate_use_cases"
server/src/main/resources/prompts/system_prompt.txt` returns one hit
at line 32.

### 2.6 Conclusion

The §7.2 hypothetical "the existing `RuntimeIntentClassifier` already
surfaces" alternates is aspirational. No runtime model carries an
alternates list; no projection consumer reads one. Sprint 30 must
pick a data source for `alternate_candidate_use_cases` before Sprint
31 ships any projection slot — the choice has anti-hardcode
consequences (§3).

## 3. Candidate data sources

Five options are evaluated below. Three (α, β, γ) are credible
single-track candidates; δ and ε are surfaced for completeness and
ruled out for reasons documented inline. Each option includes a
one-paragraph description, the code paths it would touch, its §3 layer
classification per `iteration_governance.md`, its anti-hardcode
posture, an estimated Sprint 31 size, and the risks it carries.

### 3.1 Option α — Extend `RuntimeIntentClassifier` to surface a ranked alternates list

**Description.** Add a new optional `alternateUseCases: List<…>` field
to `IntentClassification`; rewrite `RuntimeIntentClassifier.classify(...)`
from first-match-wins to either collect-all-matches with priority
weighting OR a second pass that flags drift-candidate UCs the active
UC's branch does not cover. Kernel persists the list onto a new
`BotSession` field; projection reads it.

**Code paths Option α would touch.**

| path | line range | change type |
|------|-----------:|-------------|
| `IntentClassification.java` | 32–39 | NEW field `alternateUseCases: List<…>` (record extension) |
| `RuntimeIntentClassifier.java` | 160–269 | REWRITE branch dispatch — first-match-wins replaced by collect-all-matches with priority weighting |
| `BotSession.java` | adjacent to line 53 | NEW `alternateCandidateUseCases: String[]` column + Flyway migration |
| `ControlKernel.java` | 680–706 | NEW persistence step after `applyRerouteDecision` |
| `ContextProjectionBuilder.java` | adjacent to line 394 | NEW `alternate_candidate_use_cases` projection slot |

**Layer per §3.** `semantic_planner` per §3.2 Q5 —
`RuntimeIntentClassifier` is a runtime semantic decision surface
(Sprint 10 §L0). Widening its output is a semantic shift, not a
projection-only change. The Sprint 30 dev prompt §7 explicitly flags
this: *"the chosen design MUST NOT silently promote
`RuntimeIntentClassifier`'s existing pattern set to a 'list' (each
pattern fires independently and collapses to one UC; promoting 'all
matching patterns become alternates' is a semantic shift the design
SHALL discuss explicitly, not bake in)."*

**Anti-hardcode posture.** On the §1.7 boundary. The classifier ships
Sprint 10 MVP regex patterns (`UC_J_RISK_SHIFT_PATTERN`,
`UC_C_NEW_REPLIES_PATTERN`, `UC_A_SAME_ISSUE_PATTERN`,
`UC_A_FOLLOWUP_DURATION_PATTERN`, `PAYMENT_AMBIGUITY_AD_VISIBILITY_PATTERN`)
plus `DriftDetector` keyword groups from `RiskKeywordsConfig`.
Promoting every matching regex into an alternates list converts a
fall-through priority chain into a per-pattern soft-signal list —
each pattern becomes a UC-candidate generator. The runtime still
doesn't act on the list (so the LLM owns the read), but the upstream
pipeline's semantic role widens. The patterns themselves are not new;
their role is. This is closer to the §1.7 boundary than the planning
premise anticipated.

**Sprint 31 size.** Large + risky. Touches one model + one runtime
surface + persistence (new Flyway migration) + projection. Two-track
or fix-iteration likely. Estimated 4–8 days dev + 1–2 days review.

**Risks.** (a) Semantic-shift boundary the §7 hard-gate flags
explicitly. (b) `IntentClassification` shape change ripples through
tests, traces, and kernel logging — multiple L1 invariants reference
`predictedUseCase`. (c) `DriftDetector` is branch #6 of the
classifier today; Option α must either re-run it inside the
classifier or have the kernel publish `DriftResult.newUseCase`
separately — surfaces a coupling the design needs to spec exactly.
(d) Sprint 10 freeze interaction — the classifier was delivered with
an explicit MVP-only scope; Sprint 30-era rewrite extends the
surface materially. Per-turn cost / determinism / projection-cost
regressions are all negligible.

### 3.2 Option β — Capture `RoutingResult.AMBIGUOUS` candidates at intake + re-project

**Description.** At intake time, when
`UseCaseRouter.routeNonBlocking(...)` returns
`RoutingResult.AMBIGUOUS`, the `ambiguousCandidates: List<String>`
field is already populated by the router's deterministic stages —
the candidate UCs that would have gone to LLM disambiguation had
session-create been allowed an LLM call
(`UseCaseRouter.java:415–416` returns `RoutingResult.ambiguous(candidates)`).
**`SessionManager.createSession` at lines 185–189 currently throws
this list away** — the user lands in DISCOVER with
`candidateUseCases = new String[0]` after the line 197–199 "ensure
not null" guard fires.

Option β captures the discarded list onto a new
`BotSession.intakeAmbiguousCandidates: String[]` field (preserved
across the session; not overwritten by reroute).
`ContextProjectionBuilder` projects it as `alternate_candidate_use_cases`
minus the active UC. On a suspected topic shift (e.g. UC-A ↔ UC-C
in the §7.2 brief), the LLM sees the alternate UCs the intake router
already considered plausible for the same topic subject.

**Code paths Option β would touch.**

| path | line range | change type |
|------|-----------:|-------------|
| `BotSession.java` | adjacent to line 53 | NEW `intakeAmbiguousCandidates: String[]` column + Flyway migration |
| `SessionManager.java` | 185–189 (AMBIGUOUS branch) + 110 (builder init) | NEW assignment `session.setIntakeAmbiguousCandidates(routingResult.ambiguousCandidates().toArray(...))` |
| `SessionManager.java` | 255–259 (ROUTED branch) | OPTIONAL — leave null on ROUTED, OR populate per Open Question #1 |
| `ContextProjectionBuilder.java` | adjacent to line 394 | NEW `alternate_candidate_use_cases` slot, reading the new field minus the active UC |
| `ControlKernel.java` | NO CHANGE | reroute path does NOT touch the intake-snapshot field |

**Layer per §3.** `prompt_projection` per §3.2 Q3 — the data already
exists in the runtime (`RoutingResult.ambiguousCandidates`); Option β
captures it durably and exposes it to the LLM. No semantic surface
widens; router and classifier keep their current single-purpose
contracts. Small `skill_state` aspect (field survives across turns)
but that's a persistence concern, not a state-machine concern;
runtime does not enforce any decision on the value.

**Anti-hardcode posture.** Clean soft signal. The data source is the
`UseCaseRouter`'s deterministic intake stages (topic-subject
strong-prior table, B2 bias, UC-K regression override, weak-prior
table) — existing routing data the runtime already consults at
intake. Promoting from "throw away after intake" to "preserve and
project across the session" adds NO new keyword / regex / per-UC
matrix. The slot lives next to the existing `candidate_use_cases`
slot with the same shape (LLM-readable UC list) but a different
provenance (intake snapshot vs current session UC state). LLM owns
the read; runtime does not branch on the value, gate tool dispatch,
or flip phase. Same architectural pattern as the Sprint 20
`already_called` soft-signal slot (Sprint 20 handoff §5): existing
runtime state, surfaced as observable evidence, runtime
non-enforcement, LLM-owned read decision.

**Sprint 31 size.** Small–medium, single-track. One model field +
one Flyway migration + one assignment in `SessionManager.createSession`
+ one projection slot + one short prompt-teaching paragraph + one
regression test file. ~2–3 days dev + 1 day review. The Sprint 20
`already_called` slot landed in a similar budget; Option β is the
same architectural shape with a smaller scope (no
`buildProjection(...)` signature change required because the new
field reads from `session` directly).

**Risks.**

- **Field staleness on long sessions.** The snapshot is intake-fixed.
  Mid-session topic shifts the intake router didn't anticipate (e.g.
  UC-A → UC-D account-lockout after a satisfying UC-A turn) won't
  appear in the snapshot. The slot's name (`alternate_candidate_use_cases`,
  with the teaching paragraph naming the intake-snapshot provenance)
  is honest about this; the LLM uses semantic judgement on relevance.
  The §7.2 worked example's UC-A ↔ UC-C shape DOES fall inside the
  intake ambiguity (both share the "Replies & Messaging" topic
  family). Option γ-a remains available as a future overlay if the
  snapshot proves insufficient on observed traces.
- **ROUTED-branch ambiguity.** The intake `ROUTED` outcome
  (deterministic single-UC pick) does NOT produce an alternates list.
  Option β SHALL choose between (a) leave field null on ROUTED
  (projection slot empty); or (b) extend `UseCaseRouter` to surface
  the considered-but-not-routed alternates even on ROUTED. (a) is
  Sprint 31's simpler default; (b) closes the §7.2 shape more tightly
  but requires a router-return-shape change. Open Question #1 below
  flags this for human resolution.
- **Per-turn cost / determinism.** Negligible / preserved.
- **Eval-trace shape.** Additive — Sprint 28's `per_turn_trace[]`
  picks up the new slot automatically.
- **Drift-detector interaction.** None. `DriftResult.newUseCase`
  remains an independent soft signal; both can coexist.
- **Projection-cost regression.** Negligible.

### 3.3 Option γ — New `AlternateUseCaseSurveyor` component (live per-turn surfacing)

**Description.** New runtime component that runs on every turn and
emits a ranked alternate-UC list. Two flavours:

- **γ-a (deterministic).** Reuses `UseCaseRouter`'s prior tables but
  evaluates against live user message + active UC instead of
  intake-time topic-subject. Builds a per-turn UC priors list.
- **γ-b (LLM-using).** Per-turn structured-output LLM call asking
  "which alternate UCs are plausible given current context?" Returns
  ranked list.

**Code paths Option γ would touch.**

| path | line range | change type |
|------|-----------:|-------------|
| `AlternateUseCaseSurveyor.java` | new file | NEW class — pure-Java (γ-a) or LLM-using (γ-b) |
| `ContextProjectionBuilder.java` | adjacent to 394 + constructor parameter | NEW dependency injection + projection slot |
| `ControlKernel.java` | projection-build call site | OPTIONAL — kernel-orchestrated vs builder-orchestrated |
| (γ-b only) `LlmInvocationService.java` or wrapper | new method | NEW structured-output method for the surveyor's LLM call |

**Layer per §3.** `semantic_planner` + `prompt_projection` per §3.2
Q3 + Q5. γ-a adds a new deterministic semantic surface; γ-b adds a
new LLM-using one. Either shifts ownership boundaries the runtime
freeze should be consulted on before opening.

**Anti-hardcode posture.** γ-a sits on the §1.7 boundary like Option
α (widens routing-matrix role). γ-b is soft-signal by construction
but at per-turn LLM cost. For γ-b specifically, the design SHALL
note that primary evidence in any future iteration requires real-LLM
execution per sprint-objective §7 hard rule 13.

**Sprint 31 size.** Large. γ-a: 5–7 days (new component + projection
+ per-UC regression suite). γ-b: 7–10 days, two-track (Track A
behaviour + Track B latency budget / cost containment).

**Risks.** (a) Widens runtime's semantic-decision footprint —
neither flavour is a small change. (b) γ-b adds an LLM round-trip
per turn; Sprint 25 chat-call p95 is already ~11.7s
(`sprint-025-handoff.md` §5.2). Doubling the critical path is
material; the alternative shadow / off-critical-path execution is
out of scope per §7. (c) γ-b breaks projection determinism — LLM
variance leaks into per-turn projection, a property the §7.2 worked
example does not anticipate. (d) Both flavours overlap
`DriftDetector`'s role, raising ownership questions. (e) γ-b's
failure modes (surveyor timeout / unavailable / bad output) may
require a Tier-0 invariant the fallback path enforces — if so, γ-b
exits as `human_review_required` per §1.7 + sprint-objective §5.4.

### 3.4 Option δ — Extend `DriftResult` to carry alternates from the drift detector

**Description.** Extend `DriftResult` with `alternateUseCases: List<String>`;
rewrite `DriftDetector.detect(...)` from first-match-wins to
collect-all-matches across `HardShiftGroup` targets. Kernel persists;
projection reads.

**Code paths.** `DriftResult.java:22–25` + `DriftDetector.java:55–94`
+ `BotSession.java` new field + `ControlKernel.java` new persistence
step + `ContextProjectionBuilder.java` new slot.

**Layer per §3 + posture.** `semantic_planner` per §3.2 Q5.
**Worse** than Option α on the anti-hardcode axis: `DriftDetector`
is explicitly keyword-based (the `HardShiftGroup` keywords loaded
from `RiskKeywordsConfig`). Promoting each keyword match into an
alternates list is exactly "all matching patterns become alternates"
— the move Sprint 30 dev prompt §7 says SHALL be discussed not
baked in. The `riskKeywordsConfig` YAML would become a per-UC
alternates matrix in disguise. Fails the §1.7 anti-hardcode bar with
no strong mitigation. **Ruled out** as strictly worse than Option α.

**Sprint 31 size.** Medium (3–5 days). Not recommended.

### 3.5 Option ε — LLM structured-reflection step on the prior turn

**Description.** Per-turn (or per-N-turn) structured-output LLM call
consuming prior-turn context and emitting a ranked alternate-UC list.

**Layer + posture.** `semantic_planner` per §3.2 Q5. Soft-signal by
construction (LLM owns both production AND consumption); no new
keyword / regex / per-UC matrix.

**Sprint 31 size.** Very large (10+ days). Two-track minimum.

**Ruled out for Sprint 31.** Per sprint-objective §5.3 informational
note: Option ε is "probably out of scope for this design pass." The
added per-turn LLM cost is disproportionate to the soft-signal value
when Option β delivers a large fraction of the §7.2 worked example's
benefit at a fraction of the cost with zero new LLM call. Surfaced
for completeness in case a future sprint revisits with a different
cost ceiling.

## 4. Recommendation — Option β

**Pick.** Option β — Capture `RoutingResult.AMBIGUOUS` candidates at
intake + re-project as a per-turn soft signal.

**Justification against the Constitution.**

- **§1.3 LLM owns drift / topic shift.** Option β makes the LLM
  better-informed about the alternate UC set the intake router
  considered plausible for this topic-subject family. The LLM still
  owns the semantic decision of whether the current turn's content
  warrants acting on an alternate. No Java surface gates a tool
  dispatch on the slot's value; no Java surface flips phase on the
  slot's value. The slot is observable evidence the LLM reads.
- **§1.5 Iteration rule.** Option β does not fix a semantic failure
  by adding a keyword / regex / if-else / enum / per-UC matrix. The
  data source is existing routing data the intake stage already
  produces; the only change is to preserve and project that data
  across the session. No semantic surface widens its decision shape
  (`UseCaseRouter` still answers "where does this user go at intake?"
  with one UC OR one ambiguous-candidates list; the new field is a
  faithful capture of what the router already said).
- **§1.7 Forbidden list.** Option β does not encode raw eval phrases
  into Java or prompt, does not add UC-specific hard rules for soft
  semantic decisions, does not widen the eval spec, does not optimise
  visible eval at the cost of shadow/generalization, and does not
  turn the prompt into an if-else rule dump. The single prompt edit
  Sprint 31 will make (one short teaching paragraph naming the new
  slot and its provenance) follows the Sprint 20 / Sprint 23
  `already_called` teaching pattern verbatim.

**Why Option β over Options α / γ / δ / ε.**

- **vs Option α.** Option α widens the `RuntimeIntentClassifier`'s
  semantic role from "pick one UC" to "publish all matching patterns
  as alternates" — the Sprint 30 prompt §7's explicit boundary case.
  Option β avoids that move by capturing the router's intake-time
  ambiguity instead of widening the classifier's per-turn role.
- **vs Option γ.** Both flavours of Option γ widen the runtime's
  semantic-decision footprint (new component, new per-turn decision
  surface). γ-b adds per-turn LLM cost the Sprint 25 latency
  observability does not yet have headroom for. Option β captures a
  ~80% of the same signal with a single model field, zero new
  component, zero new LLM call.
- **vs Option δ.** Option δ promotes a denser keyword matcher to an
  alternates list — strictly worse than Option α on the
  anti-hardcode axis.
- **vs Option ε.** Per-turn LLM cost disproportionate to the soft-signal
  value; explicitly out of scope per sprint-objective §5.3.

**Honest acknowledgement of Option β's limitation.** The intake
snapshot is fixed at create time; a mid-session topic shift triggered
by content the intake router did not anticipate (e.g. the user starts
on a UC-A "where is my listing" topic, gets a satisfactory answer,
then mid-session asks about a UC-D account-lockout shape) will not
appear in the snapshot. Option β is honest about this — the slot is
named to make the provenance explicit, the LLM treats the snapshot
as one input among many. The §7.2 worked example's UC-A ↔ UC-C shape
falls inside the intake snapshot's coverage (both UCs share the
intake topic-family); the UC-A ↔ UC-D shape would not. A future
sprint can layer Option γ-a on top if the intake snapshot proves
insufficient on observed traces; Sprint 31 lands the cheaper option
first and observes whether the snapshot is sufficient.

## 5. Code-paths-to-touch table for Option β (Sprint 31 reads this verbatim)

| file path | line range | change type | notes |
|-----------|-----------:|-------------|-------|
| `server/src/main/java/com/gumtree/csagent/model/BotSession.java` | new declaration adjacent to line 53–54 `candidate_use_cases` | NEW `@Column(name = "intake_ambiguous_candidates", columnDefinition = "text[]")` field `intakeAmbiguousCandidates: String[]` | Mirrors the existing `candidateUseCases` shape; null-safe accessors via Lombok `@Data`. Initialise in builder. |
| `server/src/main/resources/db/migration/V<next>__intake_ambiguous_candidates.sql` | new file | NEW `ALTER TABLE bot_sessions ADD COLUMN intake_ambiguous_candidates text[]` | Sprint 31 picks the next Flyway version number (last shipped was V13 per Sprint 24). Backfill not required — null is the valid "no snapshot" state. |
| `server/src/main/java/com/gumtree/csagent/service/runtime/SessionManager.java` | 110 (builder) | EDIT — add `.intakeAmbiguousCandidates(null)` to the `BotSession.builder()` call (or leave null-default if builder semantics permit) | Initialiser-side safety net; Lombok default is null. |
| `server/src/main/java/com/gumtree/csagent/service/runtime/SessionManager.java` | 185–189 (AMBIGUOUS branch in switch) | EDIT — before the `setCurrentPhase("DISCOVER")` call, assign: `session.setIntakeAmbiguousCandidates(routingResult.ambiguousCandidates() == null ? null : routingResult.ambiguousCandidates().toArray(new String[0]))` | Captures the intake-time alternates that the existing code throws away. |
| `server/src/main/java/com/gumtree/csagent/service/runtime/SessionManager.java` | 255–259 (ROUTED branch — OPTIONAL) | OPTIONAL EDIT — leave null on ROUTED outcome; document the choice | Sprint 31 SHOULD pick the simpler null-on-ROUTED path per §3.2 §β-risks-ROUTED-branch-ambiguity. |
| `server/src/main/java/com/gumtree/csagent/service/runtime/ContextProjectionBuilder.java` | new block adjacent to line 380–394 `candidate_use_cases` projection | NEW projection slot — build an `ArrayNode` from `session.getIntakeAmbiguousCandidates()` minus the active UC; emit as `alternate_candidate_use_cases` | The projection SHALL exclude the active UC from the list (the LLM already sees the active UC in `session.active_use_case`). Empty array when no snapshot OR when the snapshot equals `{activeUc}`. |
| `server/src/main/resources/prompts/system_prompt.txt` | adjacent to line 23–28 `already_called` teaching paragraph | NEW short teaching paragraph naming the slot, its provenance (intake-time router snapshot), and the soft-signal posture (LLM owns the read decision, runtime does not enforce) | One paragraph; mirror the Sprint 20 / Sprint 23 `already_called` teaching shape. |
| `server/src/test/java/com/gumtree/csagent/service/runtime/IntakeAmbiguousCandidatesProjectionTest.java` (or similar) | new file | NEW regression test — assert (a) AMBIGUOUS intake captures the candidates onto `BotSession`; (b) ROUTED intake leaves the field null; (c) the projection emits the slot minus the active UC; (d) the runtime does NOT branch on the field (non-enforcement evidence — mirror Sprint 20 `AgentRunLoopAlreadyCalledNonEnforcementIntegrationTest`) | 5–8 tests; mirror `AlreadyCalledProjectionTest` shape. |

Out of scope for Sprint 31 (do NOT touch in the same commit):

- `RuntimeIntentClassifier.java` — Option α is explicitly NOT taken.
- `IntentClassification.java` — no field change.
- `DriftResult.java` / `DriftDetector.java` — Option δ is explicitly
  NOT taken.
- `ClassifyUseCaseTool.java` — the LLM-facing tool stays unchanged.
- `UseCaseRouter.java` — no change to the router's return shape.

## 6. §7 stanza pre-fill for Sprint 31 (under Option β)

The dev agent who picks up Sprint 31 SHALL paste this stanza into
`docs/sprint_objective.md` and fill in any remaining counts:

```markdown
## Layer-classification + anti-hardcode stanza

**Target failure layer:** prompt_projection

**Tier-0 invariant:** This sprint adds no Tier-0 invariant. The
Constitution's drift / topic-shift bullet (§1.3 LLM owns) governs the
behaviour the projection enables; the projection itself sits inside
the Runtime's "trace and eval contract" responsibility (§1.4). The
new `BotSession.intakeAmbiguousCandidates` field is persistence state
serving the projection, not a Tier-0 invariant.

**Semantic hardcode:** No semantic hardcode introduced. The new
`alternate_candidate_use_cases` projected slot is a soft signal — a
faithful capture of the `UseCaseRouter`'s existing intake-time
ambiguity (`RoutingResult.ambiguousCandidates`), preserved across the
session and exposed to the LLM through the per-turn projection. The
LLM owns whether to act on it. No new keyword, regex, per-UC matrix,
or if-else is added to `RuntimeIntentClassifier`, `DriftDetector`,
`UseCaseRouter`, `ControlKernel`, or any prompt branch. The single
prompt edit is one short teaching paragraph naming the slot and its
provenance, in the shape of the Sprint 20 / Sprint 23 `already_called`
teaching paragraph.

**Generalization coverage:** target = UC-A ↔ UC-C drift (the §7.2
worked example's `cs_example_001` shape) and other intake-ambiguous
topic-family pairs (UC-A ↔ UC-B, UC-A ↔ UC-D shapes that share the
"Replies & Messaging" / "Listings" topic families). Neighbor = other
weak-prior topic-family pairs identified in
`UseCaseRegistryService.getCandidateUcsForTopic` results. Negative =
ROUTED-outcome sessions where the intake snapshot is null and the
slot SHALL be empty (no false positive). Shadow = held-out
intake-ambiguous traces, not visible to the dev agent. Concrete case
counts (T/N/G/S) are deferred to the Sprint 31 case-family selection
turn — the Sprint 30 design pass does not pre-author CaseSpecs per
sprint-objective §3 / §7.4 hard fence.
```

## 7. Out of scope for Sprint 31

Even after Option β lands, Sprint 31 SHALL NOT ship:

- **Shadow planner mode.** A future sprint may layer a shadow
  reflection step on top (e.g. an alternate-UC judge that compares
  the LLM's actual choice against the projected alternates and writes
  a shadow-disagreement diagnostic). Separate, larger sprint with
  its own scope; Sprint 31 does NOT pre-build any shadow surface.
- **Live per-turn surveyor (Option γ).** Sprint 31 lands intake
  snapshot only. If a follow-on smoke/probe surfaces that the
  snapshot is insufficient on observed traces (e.g. UC-A ↔ UC-D
  mid-session shifts), a future sprint may layer Option γ-a on top.
- **`DriftResult.alternateUseCases` (Option δ) / `RuntimeIntentClassifier`
  rewrite (Option α).** Neither touched. Both surfaces keep current
  single-target shapes.
- **Tier-0 invariant.** Sprint 31 adds none. If implementation
  surfaces a need (e.g. "projection MUST never publish an alternate
  UC equal to the active UC"), raise to deliver agent for promotion
  consideration; do NOT add Tier-0 invariants in Sprint 31 itself.
- **New CaseSpecs.** Sprint 31 ships runtime + prompt change + Java
  regression test only. CaseSpec authoring for the UC-A ↔ UC-C
  target/neighbor/negative/shadow family is a separate
  case-family-authoring sprint per the Sprint 20 / Sprint 29 corpus
  fence.
- **`ContextProjectionBuilder` constructor / signature change.** The
  new slot SHALL be added inside `buildProjection(...)`'s body — no
  constructor-arg expansion (unlike the Sprint 20 `priorToolEvents`
  expansion for `already_called`).
- **`SessionOutcome` / analytics shape change.** Untouched.

## 8. Open questions for the human

The design pass surfaces five questions Sprint 30 did not resolve.
Each names the impasse and the decision the human is asked to make.

1. **ROUTED-outcome behaviour for the snapshot.** Option β's §3.2
   risk note "ROUTED-branch ambiguity" raises two paths: (a) leave
   `intakeAmbiguousCandidates` null on a ROUTED intake (projection
   slot empty); or (b) extend `UseCaseRouter` to surface the
   considered-but-not-routed alternates even on a ROUTED outcome. The
   design recommends (a) for Sprint 31 simplicity, but (b) would
   close the §7.2 worked example's UC-A ↔ UC-C shape more tightly
   (UC-A and UC-C share the "Replies & Messaging" topic family;
   today a "Replies & Messaging" topic with a clear UC-C-shaped
   description routes deterministically to UC-C and (a) would leave
   the slot empty — losing the alternate-UC signal for the exact
   shift the §7.2 worked example targets). **Decision asked:** (a)
   simple, no router change, intake-AMBIGUOUS-only signal; or (b)
   richer, extend `UseCaseRouter`'s return shape to always surface
   considered alternates.
2. **System prompt teaching-paragraph location.** Sprint 23's
   `already_called` teaching paragraph lives between the Rules
   section and DISCOVER-phase guidance
   (`server/src/main/resources/prompts/system_prompt.txt:23–28` —
   per the action-bank §5 R-already-called-prompt-consumption entry).
   Sprint 31's new paragraph could sit (a) adjacent to the
   `already_called` paragraph as a sibling soft-signal teaching, or
   (b) inside the DISCOVER-phase block adjacent to the existing
   `candidate_use_cases` reference at line 32. (a) treats the slot as
   universal; (b) ties it to DISCOVER specifically. **Decision
   asked:** (a) or (b).
3. **§7.2 worked-example fold-back cadence.** The §7.2 hypothetical
   was written against an aspirational data source. Once Sprint 31
   ships under Option β, the §7.2 worked example's "the existing
   `RuntimeIntentClassifier` already surfaces" clause becomes
   misleading — the data source is `RoutingResult.ambiguousCandidates`,
   not the classifier. Per the Sprint 30 dev prompt §5 / §9.7
   hard-fence ("No editing of `iteration_governance.md` §7.2 in
   place. The fold-back is a separate cadence."), Sprint 30 does NOT
   edit §7.2 in place; the fold-back is queued for the next governance
   cadence. **Decision asked:** is the next governance fold-back
   cadence triggered by Sprint 31 close, or does it sit on the normal
   3–5-sprint review window?
4. **CaseSpec authoring sprint sequencing.** The Sprint 31 dev test
   (`IntakeAmbiguousCandidatesProjectionTest`) is a Java regression
   test — it asserts the slot ships correctly but does NOT exercise
   end-to-end LLM behaviour against the §7.2 worked example shape.
   End-to-end coverage requires authoring CaseSpecs for the UC-A ↔
   UC-C target / neighbor / negative / shadow family, which is its
   own sprint per the Sprint 20 / Sprint 29 corpus fence. **Decision
   asked:** does Sprint 31 ship the runtime change in isolation
   (slot lands, behaviour validation deferred), or does a Sprint 31+1
   case-family-authoring sprint immediately follow?
5. **Interaction with `R-uc-cdf-get-customer-context-bot-actual-usage`.**
   The Sprint 22 surfaced R-item at `docs/action_bank.md (R-uc-cdf-get-customer-context-bot-actual-usage)` carries
   a related observation — does the bot use `get_customer_context`
   when account-state matters on FAQ-miss shapes in UC-C / UC-D /
   UC-F. The alternates-list slot's projection arguably overlaps that
   open question (an LLM that sees UC-D as an alternate on a UC-C
   active session may decide to investigate account state). **Decision
   asked:** is there a sequencing relationship the deliver agent
   should preserve (e.g. Sprint 31 lands first to give the
   `get_customer_context` follow-on more projection evidence)?

## 9. Acceptance criteria for Sprint 31

Sprint 31 closes when:

- A new `BotSession.intakeAmbiguousCandidates: String[]` field exists,
  with a paired Flyway migration. Default value is null; backfill is
  not required.
- `SessionManager.createSession`'s AMBIGUOUS branch captures
  `routingResult.ambiguousCandidates()` onto the new field. The
  ROUTED branch leaves the field null (or populates per Open
  Question #1's resolved path).
- `ContextProjectionBuilder` emits an `alternate_candidate_use_cases`
  array on every per-turn projection. The array EXCLUDES the active
  UC. Empty array when the field is null OR when the field equals
  `[activeUc]`.
- A new regression test file (suggested name
  `IntakeAmbiguousCandidatesProjectionTest.java`) asserts: (a) the
  AMBIGUOUS intake path captures the candidates onto `BotSession`;
  (b) the ROUTED intake path leaves the field null (or per Open
  Question #1's resolution); (c) the projection slot emits with the
  active UC excluded; (d) the runtime does NOT enforce any decision
  on the slot's value — non-enforcement evidence at the
  `AgentRunLoopImpl.run(...)` granularity, mirroring Sprint 20's
  `AgentRunLoopAlreadyCalledNonEnforcementIntegrationTest`.
- The system prompt carries one short teaching paragraph naming the
  slot, its intake-snapshot provenance, and the soft-signal posture
  (LLM owns the read decision, runtime does not enforce). Location
  per Open Question #2's resolved path.
- Existing L1 invariants remain green; the cs014 / cs176 / cs002 /
  cs029 / cs066 / cs095 regression suite remains green.
- A reference smoke run is captured under
  `eval_interactive/results/<run-id>/` after the change lands, with
  the new `alternate_candidate_use_cases` slot visible in
  `case_results[].per_turn_trace[].projection` (Sprint 28
  `per_turn_trace` enrichment carries the new slot automatically).
- Sprint 31 ships no Tier-0 change, no router-return-shape change
  beyond Open Question #1's resolution, no
  `RuntimeIntentClassifier` change, no `DriftDetector` change, no
  `IntentClassification` field change, no `DriftResult` field change.
- The §7 stanza in `docs/sprint_objective.md` is filled in per the
  §6 pre-fill (target failure layer = `prompt_projection`; Tier-0 =
  none; semantic hardcode = none; generalization coverage = per
  §6's text, with concrete T/N/G/S counts named at Sprint 31
  case-family-selection turn).

The Sprint 30 design freeze is the failing-by-design trigger that
flips to passing the moment Sprint 31 lands. Sprint 30 itself ships
no acceptance gate beyond the design doc being complete.

## 10. References

Governance and precedent:

- `docs/sprint_objective.md` — Sprint 30 objective.
- `docs/current/iteration_governance.md` §1 / §3 / §5 / §7 —
  Constitution + Fix Layer Classification + Eval Acceptance + §7
  stanza template; §7.2 carries the worked example this doc repairs.
- `docs/current/doc_governance.md` — front-matter + tier model.
- `docs/current/agent_context_guide.md` — per-task reading list.
- `docs/proposals/handover_orchestrator_design.md` — Sprint 16
  docs-only design-freeze precedent.
- `docs/sprints/sprint-020-handoff.md` §5 — `already_called`
  soft-signal slot precedent (architectural pattern Option β mirrors).
- `docs/sprints/sprint-027-handoff.md` — 12-section investigation
  handoff template.
- `docs/sprints/sprint-028-handoff.md` — `per_turn_trace[]` enrichment
  that Sprint 31's smoke rerun will exercise.
- `docs/runtime_freeze_and_risk_policy.md` — Tier-0 invariant
  catalogue.
- `docs/action_bank.md` — R-item registry.

Code paths cited in this doc (HEAD `df8b8cd`):

- Option β edit sites: `BotSession.java:53–54`,
  `SessionManager.java:110 / 185–189 / 255–259`,
  `ContextProjectionBuilder.java:380–394`,
  `system_prompt.txt:23–28 / 32`.
- Option β data source: `UseCaseRouter.java:297–416` (deterministic
  stages), `:750–769` (`RoutingResult` record).
- Premise-check context: `RuntimeIntentClassifier.java:160–269`,
  `IntentClassification.java` (76 LOC),
  `DriftResult.java:12–26`, `DriftDetector.java:55–94`,
  `ControlKernel.java:684–687` and `:1199–1202`.
- Options α / δ would-be targets (NOT taken):
  `RuntimeIntentClassifier.java`, `IntentClassification.java`,
  `DriftResult.java`, `DriftDetector.java`.
