---
title: Sprint 30 handoff — Alternate-UC signal data-source design (investigation-only, docs-only)
doc_tier: sprint-archive
status: current
implementation_status: implemented
source_of_truth: this file
last_reviewed: 2026-05-16
review_cadence: ad hoc
supersedes: []
superseded_by: null
notes: >
  Sprint 30 dev-agent handoff. Docs-only architectural-design sprint.
  All five §4 premise items in `docs/sprint_objective.md` re-verified
  against HEAD `df8b8cd` at session start; no drift. Design pass
  evaluated 5 candidate data sources (α through ε) for the future
  `alternate_candidate_use_cases` projection slot named in
  `docs/current/iteration_governance.md` §7.2 worked example.
  Recommendation: Option β — capture `RoutingResult.AMBIGUOUS`
  candidates at intake on a new `BotSession.intakeAmbiguousCandidates`
  field, project as `alternate_candidate_use_cases` minus the active
  UC. Layer: `prompt_projection` per §3.2 Q3. No semantic surface
  widens. No Tier-0 invariant added. 5 open questions surfaced for the
  human. Design doc lands at
  `docs/proposals/alternate_uc_signal_data_source_design.md` (~40K,
  10 sections). R-item `R-alternate-uc-signal-data-source` appended
  to `docs/action_bank.md` Sprint 23 surfaced backlog area (after the
  Sprint 25 `R-per-llm-call-latency-instrumentation` row, before §6
  Closed action index). Zero code change; zero test run; zero smoke;
  zero CaseSpec; zero `system_prompt.txt` edit; `R-prompt-phase-plan-directive-followship`
  at line 450 untouched; `iteration_governance.md` §7.2 untouched in
  place (fold-back queued for normal governance cadence).
---

# Sprint 30 handoff — Alternate-UC signal data-source design

## 1. Context Pack

### 1.1 Relevant docs (sampled & read)

| path | tier (best guess) | status (best guess) | one-line relevance |
|------|-------------------|---------------------|--------------------|
| `docs/sprint_objective.md` | current-runtime | current | Sprint 30 authoritative scope: §1 sprint class (docs-only design), §4 5-item premise check, §5.2/§5.3/§5.4 design doc minimum content + anti-hardcode hard gate, §7 hard fences, §11 12-section handoff contract, §12 stop conditions. |
| `compact/sprint-030-dev-prompt.md` | current-runtime | current | Dev-prompt §2 premise re-verification list (5 items), §3 design-doc 10-section structure, §4 R-item registration, §5 STOP conditions, §6 reproducibility rule, §7 anti-hardcode recap, §8 12-section handoff contract, §9 stop conditions, §10 working-tree expectations, §11 final-commit run commands. |
| `docs/current/iteration_governance.md` §1 / §3 / §5 / §7 | durable-connective | current | Constitution (§1.3 LLM-owns drift / §1.5 iteration rule / §1.7 forbidden list); Fix Layer Classification (§3.2 question routing — Sprint 30 uses Q3 / Q5 for option-layer classification); Eval Acceptance Rules (§5; advisory only — Sprint 30 ships no eval delta); §7 stanza template (Sprint 30 design pre-fills for Sprint 31); §7.2 hypothetical worked example whose premise this sprint repairs. |
| `docs/current/doc_governance.md` + `docs/current/agent_context_guide.md` | durable-connective | current | Tier model, front-matter schema, per-task reading lists; loaded transitively via `AGENTS.md`. |
| `docs/proposals/handover_orchestrator_design.md` | proposal | current | Sprint 16 docs-only design-freeze precedent. Shape Sprint 30 mirrors (§ headers, "what is confirmed vs not", "future shape", "acceptance criteria for the future runtime sprint"). |
| `docs/sprints/sprint-027-handoff.md` | sprint-archive | historical | Most recent investigation-only sprint precedent (probe sprint with closed-set recommendation). 12-section handoff template Sprint 30 mirrors. |
| `docs/sprints/sprint-020-handoff.md` §5 | sprint-archive | historical | `already_called` soft-signal slot precedent — the architectural pattern Option β mirrors (existing runtime state, surfaced as observable evidence, runtime non-enforcement, LLM-owned read decision). |
| `docs/sprints/sprint-028-handoff.md` | sprint-archive | historical | `per_turn_trace[]` enrichment that Sprint 31's smoke rerun will exercise to surface the new `alternate_candidate_use_cases` slot. |
| `docs/action_bank.md` lines 444–617 (R-items active section) | durable-connective | current | The R-item registry. Sprint 30 appends one new R-item after the Sprint 23-surfaced backlog's last row (`R-per-llm-call-latency-instrumentation` at line 617), before §6 Closed action index at line 619. |
| `docs/runtime_freeze_and_risk_policy.md` | foundational (top-level) | current | Tier-0 invariant catalogue. Sprint 30 references for the "no new Tier-0" anchor; design pass found no need to propose a new Tier-0 invariant under any option. |

Sprint 18 / 19 / 21 – 26 archives were not loaded; not required by
the design pass.

### 1.2 Relevant code paths (verified at session start, 2026-05-16)

| path | lines | what it governs |
|------|------:|-----------------|
| `server/src/main/java/com/gumtree/csagent/service/runtime/RuntimeIntentClassifier.java` | 160–269 | `classify(...)` six conditional branches in priority order; each returns one `IntentClassification`. Premise §4 item 1 anchor. |
| `server/src/main/java/com/gumtree/csagent/model/IntentClassification.java` | 32–39 | Six-field record (`predictedUseCase`, `confidence`, `relation`, `taskType`, `primaryEntityType`, `primaryEntityValue`); no alternates / candidates field. Premise §4 item 2 anchor. |
| `server/src/main/java/com/gumtree/csagent/model/DriftResult.java` | 12–26 | Three-field POJO (`DriftType type`, `String newUseCase`, `boolean escalationRequested`). Premise §4 item 3 anchor. |
| `server/src/main/java/com/gumtree/csagent/service/runtime/DriftDetector.java` | 55–94 | `detect(...)` walks two keyword passes and returns at first match. Context for §4 item 3. |
| `server/src/main/java/com/gumtree/csagent/model/BotSession.java` | 53–54 | `private String[] candidateUseCases` field; declared as `text[]` column; shape-capable but always single-element in practice. Premise §4 item 4 anchor. |
| `server/src/main/java/com/gumtree/csagent/service/runtime/ControlKernel.java` | 684–687 + 1199–1202 | The two writer sites where the `candidateUseCases` array is installed as a single-element array (one in reroute, one in missing-UC fallback). Premise §4 item 4 anchor (second half). |
| `server/src/main/java/com/gumtree/csagent/service/runtime/SessionManager.java` | 110 + 185–189 + 197–199 + 255–259 | Builder init (line 110); the AMBIGUOUS branch (lines 185–189) that today **discards** `routingResult.ambiguousCandidates()`; the "ensure not null" guard (lines 197–199) that installs `new String[0]`; the ROUTED branch (lines 255–259). These are Option β's primary edit sites. |
| `server/src/main/java/com/gumtree/csagent/service/runtime/UseCaseRouter.java` | 275–296 (entry points) + 297–416 (deterministic stages) + 750–769 (`RoutingResult` record + `ambiguous(candidates)` factory) | Intake-time router. `RoutingResult.AMBIGUOUS` already carries the multi-element `ambiguousCandidates: List<String>` field — the existing data source Option β captures. |
| `server/src/main/java/com/gumtree/csagent/service/runtime/ContextProjectionBuilder.java` | 380–394 (existing `candidate_use_cases` projection) + 707–819 (`build(...)` 6-arg overload including `priorToolEvents` for `already_called`) | The existing `candidate_use_cases` projection slot Sprint 7 §I0 added; the Sprint 20 `already_called` constructor-arg-expansion precedent. Option β's new slot lands adjacent to line 394. |
| `server/src/main/resources/prompts/system_prompt.txt` | 23–28 (`already_called` teaching paragraph) + 32 (`candidate_use_cases` DISCOVER reference) | Existing teaching-paragraph location precedent (Sprint 23 landed `already_called` between Rules and DISCOVER). Sprint 31's new paragraph location is Open Question #2 in the design doc. |

### 1.3 Doc-status warnings (drift observed)

- The Sprint 30 dev prompt §2 cites the `BotSession.candidateUseCases`
  collapse behaviour at `ControlKernel.java:686` and `:1201`. Both
  cited lines verified. The dev prompt did NOT cite
  `SessionManager.java:197–199` ("ensure not null" guard that installs
  `new String[0]`) which is the load-bearing line for the
  data-discard finding — Sprint 30 design doc §2.4 surfaces this for
  reproducibility. Not doc drift, just an additional cite the design
  doc adds beyond the dev prompt's premise list.
- Working-tree at session start carried pre-existing unrelated mods
  on `csagent_system_design_review.md`,
  `server/src/main/resources/prompts/system_prompt.txt`, and
  `docs/action_bank.md` (Sprint 29 close-window R-item updates), plus
  untracked Sprint 29 archives + sprint-030 deliver-agent prompts +
  `docs/10-handoff.md` deliver-agent §1 lead refresh. None are
  touched by this dev pass per dev-prompt §10 hard-fence. The
  deliver-agent-owned files are the human's to manage at commit
  boundary per `feedback_commit_at_end_bundles_deliver_artefacts.md`.

### 1.4 Source-of-truth decision

For the premise-check step (§3 below), the source of truth is the
verbatim Java source at the cited file:line ranges. Per
`doc_governance.md` "code ahead of docs" rule, code is the source of
truth for delivered behaviour; the `iteration_governance.md` §7.2
worked example is forward-looking text (`status: aspirational` in
intent if not in front matter), and Sprint 30 surfaces the
delivered-behaviour-vs-aspirational-text gap. The fold-back is queued
for the next governance cadence (Open Question #3 in the design doc).

For the design-pass step (§4 below), the source of truth is the
design doc itself
(`docs/proposals/alternate_uc_signal_data_source_design.md` —
`source_of_truth: this file` per its front matter). The design doc
freezes the architectural choice; Sprint 31 reads §5 (code-paths
table) and §6 (§7 stanza pre-fill) verbatim.

### 1.5 Implementation status

`implementation_status: implemented` — Sprint 30 ships exactly three
authored artefacts: this handoff +
`docs/proposals/alternate_uc_signal_data_source_design.md` (new
design doc) + one R-item appended to `docs/action_bank.md`. No
deferred work; no partial shipment; no proposal restructured. The
R-item itself opens with disposition `proposal (Sprint 30 design
freeze; Sprint 31 implements)`.

### 1.6 Risks before the design pass

- **Anti-hardcode boundary case (Option α / Option δ).** Both options
  would widen the semantic role of an existing keyword/regex matcher
  (`RuntimeIntentClassifier` for α, `DriftDetector` for δ) by
  promoting first-match-wins to collect-all-matches. The Sprint 30
  dev prompt §7 explicitly warns: *"the chosen design MUST NOT
  silently promote `RuntimeIntentClassifier`'s existing pattern set
  to a 'list' (each pattern fires independently and collapses to one
  UC; promoting 'all matching patterns become alternates' is a
  semantic shift the design SHALL discuss explicitly, not bake in)."*
  The design pass discusses both options explicitly (design doc §3.1
  and §3.4) and recommends against them in favour of Option β, which
  surfaces existing intake-time data without widening any matcher's
  semantic role.
- **`human_review_required` exit risk.** Per sprint-objective §5.4
  and dev-prompt §7, if no option satisfies the §1.7 anti-hardcode
  bar, the recommendation SHALL be `human_review_required` — not a
  dressed-up hard branch. The design pass found Option β satisfies
  the bar cleanly (same architectural pattern as Sprint 20
  `already_called` precedent); no `human_review_required` exit
  triggered. Option α (the closest borderline case) is named and
  ruled out with reasoning, not silently promoted as soft.
- **Field-staleness risk for the recommended option.** Option β's
  intake snapshot is fixed at create time; mid-session topic shifts
  the intake router did not anticipate (e.g. UC-A → UC-D
  account-lockout shape mid-session) will not appear in the snapshot.
  The slot's name and the proposed teaching paragraph make the
  provenance explicit; LLM uses semantic judgement on relevance. The
  design doc §3.2 risks block + Recommendation §4 honest
  acknowledgement both surface this limitation. A future sprint can
  layer Option γ-a (live deterministic surveyor) on top if observed
  traces show the snapshot is insufficient.
- **CaseSpec coverage gap.** Sprint 31 ships a Java regression test
  (`IntakeAmbiguousCandidatesProjectionTest`) but does NOT
  exercise end-to-end LLM behaviour against the §7.2 worked example's
  UC-A ↔ UC-C shape. End-to-end coverage requires a follow-on
  case-family-authoring sprint per the Sprint 20 / Sprint 29 corpus
  fence. Open Question #4 in the design doc flags this for human
  sequencing.

## 2. Sprint-objective recap

Sprint 30 is a docs-only architectural-design sprint that produces
ONE proposal doc naming the chosen data source for the future
`alternate_candidate_use_cases` projection slot whose hypothetical
was sketched in `docs/current/iteration_governance.md` §7.2 worked
example. The §7.2 hypothesised data source ("the existing
`RuntimeIntentClassifier` already surfaces" a list of alternate UCs)
does not exist in current code — a planning-turn premise check
(deliver-agent 2026-05-16) established and this dev pass re-verified
the gap. Sprint 30's deliverable is the design doc, plus an R-item
registration in `docs/action_bank.md` and a 12-section handoff (this
file). Sprint 30 ships ZERO Java code change, ZERO Python code
change, ZERO prompt change, ZERO CaseSpec change, ZERO test run,
ZERO smoke run, ZERO new Tier-0 invariant.

Per sprint-objective §1: exempt from the §7 semantic-touching stanza
(docs-only sprint, Sprint 16 + Sprint 27 precedent).

Per sprint-objective §5.4 anti-hardcode hard gate: the chosen option
MUST be a soft signal per Constitution §1.3 + §1.7; if no option
satisfies the bar, the recommendation SHALL be `human_review_required`
per §3.2 default tail. Sprint 30 did NOT exit as `human_review_required`
— Option β satisfies the bar cleanly; see §8 below for the
anti-hardcode self-walk and §5 for the recommendation summary.

## 3. Premise re-verification (each of §2's five points)

All five premise items in `compact/sprint-030-dev-prompt.md` §2 + the
sprint-objective §4 "Premise check" section were verified at session
start at SHA `df8b8cd` (`docs: close sprint 28 …`).

| # | premise | verification |
|---|---------|--------------|
| 1 | `RuntimeIntentClassifier.classify()` at `server/src/main/java/com/gumtree/csagent/service/runtime/RuntimeIntentClassifier.java:160–269` returns one `IntentClassification` with one `predictedUseCase`. | Read at lines 160–269. Confirmed: method signature `public IntentClassification classify(BotSession session, String userMessage, DriftResult driftResult)` returns single object. Six conditional branches (1 explicit-human/distress, 2 UC-J risk, 3 UC-C soft-shift, 4-5 UC-A shapes, 6 `DriftDetector` HARD_SHIFT fallback) each `return IntentClassification.of(...)` directly; final fallthrough at line 268 `return IntentClassification.unknown()`. No accumulation across branches. `grep -nE "return IntentClassification\." server/src/main/java/com/gumtree/csagent/service/runtime/RuntimeIntentClassifier.java` returns ~12 single-object return sites. |
| 2 | `IntentClassification.java` at `server/src/main/java/com/gumtree/csagent/model/IntentClassification.java` — no `alternates` / `candidates` field. | Read full file (76 LOC). Six-field record: `predictedUseCase`, `confidence`, `relation`, `taskType`, `primaryEntityType`, `primaryEntityValue`. `grep -cn "alternates\|candidates" server/src/main/java/com/gumtree/csagent/model/IntentClassification.java` returns `0`. |
| 3 | `DriftResult.java` at `server/src/main/java/com/gumtree/csagent/model/DriftResult.java` — fields are `{type, newUseCase, escalationRequested}` only. | Read full file (27 LOC). Confirmed: `private DriftType type`, `private String newUseCase`, `@Builder.Default private boolean escalationRequested = false`. Plus inner `enum DriftType { NONE, MINOR, SOFT_SHIFT, HARD_SHIFT, USER_ESCALATION_REQUEST }`. `grep -cn "alternates\|candidates" server/src/main/java/com/gumtree/csagent/model/DriftResult.java` returns `0`. |
| 4 | `BotSession.candidateUseCases` is a `String[]`; `ControlKernel.applyRerouteDecision` at line 686 collapses to single-element `[targetUc]` (and again at line 1201 fallback path). | Read `BotSession.java:53–54` — `@Column(name = "candidate_use_cases", columnDefinition = "text[]") private String[] candidateUseCases`. Read `ControlKernel.java:684–687` — `if (session.getCandidateUseCases() == null \|\| session.getCandidateUseCases().length == 0) { session.setCandidateUseCases(new String[]{targetUc}); }`. Read `ControlKernel.java:1199–1202` — same shape with `fallbackUc`. Both writers are guarded by "if null or empty"; neither expands an existing list. `grep -n "setCandidateUseCases" server/src/main/java/com/gumtree/csagent/service/runtime/{ControlKernel,SessionManager,UseCaseRouter}.java` returns 3 writer sites (the third is `SessionManager.java:110` builder init to `new String[0]`); none assigns a multi-element array from observable data. **Additional finding (not in dev-prompt §2 list, surfaced by the design pass):** at `SessionManager.java:185–189`, the AMBIGUOUS branch of `createSession` does NOT assign `routingResult.ambiguousCandidates()` (a multi-element `List<String>` already populated by `UseCaseRouter`) to `session.setCandidateUseCases(...)`; the list is discarded. This is the Option β data source. |
| 5 | `ContextProjectionBuilder` projects existing `candidate_use_cases` slot at `server/src/main/java/com/gumtree/csagent/service/runtime/ContextProjectionBuilder.java:380–394`. | Read at lines 380–394. Confirmed: `ArrayNode candidateUcsNode = objectMapper.createArrayNode(); if (session.getCandidateUseCases() != null) { for (String uc : session.getCandidateUseCases()) { if (uc != null && !uc.isBlank()) { candidateUcsNode.add(uc); } } } projection.set("candidate_use_cases", candidateUcsNode);`. Sprint 7 §I0 cue per inline comment. System prompt references at `system_prompt.txt:32` (DISCOVER guidance only, no other phase consumer). |

No premise-drift stop condition fired. Dev-prompt §9.1 / §5 not
triggered. HEAD matches the baseline `df8b8cd` cited in the
sprint-objective §4. All five items hold.

## 4. Design pass walkthrough

For each of the 5 candidate data sources evaluated, a one-paragraph
why-considered + why-recommended-or-ruled-out. Detail per option
lives in the design doc §3.1 – §3.5. The §5.3 informational list in
the sprint objective named α / β / γ / δ / ε; the design pass
evaluated all five and added no new candidates.

### 4.1 Option α — Extend `RuntimeIntentClassifier` for ranked alternates

**Why considered.** The §7.2 worked example explicitly named the
`RuntimeIntentClassifier` as the data source, so honest evaluation
required walking the option that takes §7.2's premise on its own
terms — what would it look like to actually surface alternates from
the classifier?

**Why ruled out.** The classifier today is built on Sprint 10 MVP
regex patterns + `DriftDetector` keyword groups; promoting every
matching regex into an alternates list converts a fall-through
priority chain into a per-pattern soft-signal list — each pattern
becomes a UC-candidate generator. The Sprint 30 dev prompt §7
explicitly warns this is a semantic shift the design SHALL discuss,
not bake in. The runtime still doesn't act on the list (so the LLM
owns the read), but the upstream pipeline's semantic role widens.
This is closer to the §1.7 boundary than the planning premise
anticipated, and Option β achieves a large fraction of the same
benefit without widening any matcher's semantic role.

### 4.2 Option β — Capture `RoutingResult.AMBIGUOUS` candidates at intake

**Why considered.** During the design pass, reading
`SessionManager.createSession`'s AMBIGUOUS branch surfaced a
load-bearing finding: `routingResult.ambiguousCandidates()` (a
multi-element `List<String>` already populated by the deterministic
intake router) is currently discarded — the user lands in DISCOVER
with `candidateUseCases = new String[0]` after the "ensure not null"
guard fires. Existing runtime data, thrown away, ready to be
captured.

**Why recommended.** Clean soft signal per §3.2 Q3 — the LLM
projection is impoverished (missing slot for alternate UCs the intake
router already considered plausible); fix it by surfacing existing
runtime data. No semantic surface widens (router and classifier
both keep their current single-purpose contracts; the new field is a
faithful capture of `RoutingResult.AMBIGUOUS` data the router already
produces). No new keyword / regex / per-UC matrix added. No Tier-0
invariant. Same architectural pattern as Sprint 20 `already_called`
(existing runtime state, surfaced as observable evidence, runtime
non-enforcement, LLM-owned read decision). Sprint 31 size: small to
medium, single-track (~2–3 days dev + 1 day review). The intake
snapshot has a documented staleness limitation (mid-session shifts
the intake router didn't anticipate won't appear); the design doc §3.2
honest-acknowledgement surfaces this and notes Option γ-a remains
available as a future overlay if observed traces show the snapshot
insufficient.

### 4.3 Option γ — New `AlternateUseCaseSurveyor` component (γ-a deterministic / γ-b LLM-using)

**Why considered.** Direct response to Option β's staleness
limitation. A live per-turn surveyor would close the mid-session
shift gap Option β cannot cover.

**Why ruled out (for Sprint 30 recommendation, surfaced as future
overlay).** γ-a widens the `UseCaseRouter`'s intake-only role into a
per-turn role (same §7-boundary concern as Option α). γ-b adds a
per-turn LLM call — Sprint 25 chat-call p95 is ~11.7s
(`docs/sprints/sprint-025-handoff.md` §5.2); doubling the critical
path is material; the off-critical-path "shadow planner mode"
alternative is out of scope per design doc §7 + dev-prompt §3.6
("shadow planner mode is OUT; that is a separate, larger sprint").
γ-b also breaks projection determinism (LLM variance leaks into the
projection itself, a property the §7.2 worked example does not
anticipate) and may require a Tier-0 invariant on its fallback path
— which would exit as `human_review_required` per §1.7 +
sprint-objective §5.4. Option β captures a large fraction of the
benefit at a fraction of the cost; γ-a remains available as a future
overlay sprint if observed traces show the snapshot insufficient.

### 4.4 Option δ — Extend `DriftResult` with alternates from the drift detector

**Why considered.** Symmetric option to α — the §7.2 worked example
mentioned the soft-signal slot in a drift / topic-shift context, and
`DriftDetector` is the obvious second runtime surface that
already detects soft shifts.

**Why ruled out.** Strictly worse than Option α on the §1.7
anti-hardcode axis. `DriftDetector` is explicitly keyword-based (the
`HardShiftGroup` keywords from `RiskKeywordsConfig`). Promoting each
keyword match into an alternates list is exactly "all matching
patterns become alternates" — the move Sprint 30 dev prompt §7 says
SHALL be discussed not baked in. The `riskKeywordsConfig` YAML would
become a per-UC alternates matrix in disguise. Fails the §1.7 bar
with no strong mitigation.

### 4.5 Option ε — LLM structured-reflection step on the prior turn

**Why considered.** Sprint-objective §5.3 informational note named ε
as a candidate worth surfacing for completeness even if ruled out.

**Why ruled out for Sprint 31.** Per sprint-objective §5.3: "probably
out of scope for this design pass." The added per-turn LLM cost is
disproportionate to the soft-signal value when Option β delivers a
large fraction of the §7.2 worked example's benefit at zero new LLM
call. Surfaced for completeness in case a future sprint revisits the
alternates-list shape with a different cost ceiling.

## 5. Recommendation summary

**Pick.** Option β — Capture `RoutingResult.AMBIGUOUS` candidates at
intake on a new `BotSession.intakeAmbiguousCandidates: String[]`
field, project as `alternate_candidate_use_cases` minus the active UC.

**Constitution citation.** §1.3 (LLM owns drift / topic shift) — the
slot makes the LLM better-informed about the alternate UC set the
intake router considered plausible; the LLM still owns the semantic
decision of whether the current turn's content warrants acting on an
alternate. §1.5 (iteration rule) — no semantic surface widens; the
data source is existing routing data the intake stage already
produces. §1.7 (forbidden list) — no keyword / regex / if-else /
per-UC matrix added; the single prompt edit follows the Sprint 20 /
Sprint 23 `already_called` teaching pattern verbatim; no eval-spec
widening; no visible-eval optimisation at the cost of
shadow/generalisation.

**Layer per §3.2.** `prompt_projection` per §3.2 Q3 — the LLM
chooses validly within the available options, but the projection /
context handed to it is impoverished (missing slot for alternate UCs
the intake router already considered plausible). Small `skill_state`
aspect (field must survive across turns); persistence concern, not a
state-machine concern. No `java_guard`, no `semantic_planner` (no
runtime semantic decision surface widens), no `infra` /
`eval_spec` / `product_policy` / `judge_calibration` /
`human_review_required` exit.

**Sprint 31 sizing.** Small to medium, single-track. ~2–3 days dev +
1 day review. Mirrors Sprint 20 `already_called` slot scope.

**No `human_review_required` exit triggered.** Per sprint-objective
§5.4 and dev-prompt §7, the design pass exits as
`human_review_required` only if no option satisfies the §1.7
soft-signal bar. Option β satisfies the bar cleanly; the design pass
exits with a positive recommendation, not an impasse.

## 6. Code-paths-to-touch table (restated from design doc §5)

Sprint 31 dev reads this verbatim. Out-of-scope hard fences enforced
on Sprint 31 are listed below the table.

| file path | line range | change type | notes |
|-----------|-----------:|-------------|-------|
| `server/src/main/java/com/gumtree/csagent/model/BotSession.java` | new declaration adjacent to line 53–54 `candidate_use_cases` | NEW `@Column(name = "intake_ambiguous_candidates", columnDefinition = "text[]")` field `intakeAmbiguousCandidates: String[]` | Mirrors existing `candidateUseCases` shape; null-safe via Lombok `@Data`. |
| `server/src/main/resources/db/migration/V<next>__intake_ambiguous_candidates.sql` | new file | NEW `ALTER TABLE bot_sessions ADD COLUMN intake_ambiguous_candidates text[]` | Sprint 31 picks the next Flyway version number (last shipped was V13 per Sprint 24). Backfill not required — null is valid "no snapshot" state. |
| `server/src/main/java/com/gumtree/csagent/service/runtime/SessionManager.java` | 110 (builder) | EDIT — add `.intakeAmbiguousCandidates(null)` to the `BotSession.builder()` call (or rely on Lombok default null) | Initialiser-side safety net. |
| `server/src/main/java/com/gumtree/csagent/service/runtime/SessionManager.java` | 185–189 (AMBIGUOUS branch in switch) | EDIT — before `setCurrentPhase("DISCOVER")`, assign `session.setIntakeAmbiguousCandidates(routingResult.ambiguousCandidates() == null ? null : routingResult.ambiguousCandidates().toArray(new String[0]))` | Captures the intake-time alternates the existing code throws away. |
| `server/src/main/java/com/gumtree/csagent/service/runtime/SessionManager.java` | 255–259 (ROUTED branch — OPTIONAL) | OPTIONAL EDIT — leave null on ROUTED outcome; document the choice | Sprint 31 SHOULD pick the simpler null-on-ROUTED path per design doc §3.2 ROUTED-branch-ambiguity risk note and Open Question #1. |
| `server/src/main/java/com/gumtree/csagent/service/runtime/ContextProjectionBuilder.java` | new block adjacent to line 380–394 `candidate_use_cases` projection | NEW projection slot — build an `ArrayNode` from `session.getIntakeAmbiguousCandidates()` minus the active UC; emit as `alternate_candidate_use_cases` | SHALL exclude the active UC. Empty array when no snapshot OR when snapshot equals `{activeUc}`. |
| `server/src/main/resources/prompts/system_prompt.txt` | adjacent to line 23–28 `already_called` teaching paragraph (or line 32 DISCOVER guidance — Open Question #2) | NEW short teaching paragraph naming the slot, its provenance (intake-time router snapshot), and the soft-signal posture (LLM owns read, runtime does not enforce) | One paragraph; mirror Sprint 20 / Sprint 23 `already_called` teaching shape. |
| `server/src/test/java/com/gumtree/csagent/service/runtime/IntakeAmbiguousCandidatesProjectionTest.java` (or similar) | new file | NEW regression test — assert (a) AMBIGUOUS intake captures candidates; (b) ROUTED leaves field null; (c) projection emits slot minus active UC; (d) runtime does NOT branch on the field (non-enforcement evidence — mirror Sprint 20 `AgentRunLoopAlreadyCalledNonEnforcementIntegrationTest`) | 5–8 tests; mirror `AlreadyCalledProjectionTest` shape. |

Out of scope for Sprint 31 (do NOT touch in the same commit):
`RuntimeIntentClassifier.java`, `IntentClassification.java`,
`DriftResult.java`, `DriftDetector.java`, `ClassifyUseCaseTool.java`,
`UseCaseRouter.java` (no return-shape change), `ControlKernel.java`
(reroute path keeps current behaviour). The `ContextProjectionBuilder`
edit lands inside the existing `buildProjection(...)` body — NO
constructor-arg expansion (unlike Sprint 20's `priorToolEvents`
expansion for `already_called`).

## 7. Open questions for the human

Five questions surfaced by the design pass; full text in the design
doc §8. Summarised here:

1. **ROUTED-outcome behaviour for the snapshot.** Leave field null
   on ROUTED (Sprint 31's simpler default), OR extend `UseCaseRouter`
   to surface the considered-but-not-routed alternates on ROUTED
   outcomes (closes the §7.2 worked example's UC-A ↔ UC-C shape more
   tightly but requires a router-return-shape change). Decision asked.
2. **System prompt teaching-paragraph location.** Adjacent to the
   Sprint 23 `already_called` teaching paragraph (sibling soft-signal
   teaching, universal), OR inside the DISCOVER-phase block adjacent
   to the existing `candidate_use_cases` reference at line 32
   (DISCOVER-specific). Decision asked.
3. **§7.2 worked-example fold-back cadence.** Once Sprint 31 ships,
   §7.2's "the existing `RuntimeIntentClassifier` already surfaces"
   clause becomes misleading. Sprint 30 does NOT edit §7.2 in place
   (dev-prompt §5 / §9.7 hard-fence). Decision asked: fold-back
   triggered by Sprint 31 close, or normal 3–5-sprint cadence?
4. **CaseSpec authoring sprint sequencing.** Sprint 31's Java
   regression test asserts the slot ships correctly but does NOT
   exercise end-to-end LLM behaviour on the §7.2 worked example's
   UC-A ↔ UC-C shape. End-to-end coverage requires a follow-on
   case-family-authoring sprint per Sprint 20 / Sprint 29 corpus
   fence. Decision asked: Sprint 31 ships runtime change in isolation,
   or does a Sprint 31+1 case-family sprint immediately follow?
5. **Interaction with `R-uc-cdf-get-customer-context-bot-actual-usage`**
   (`docs/action_bank.md:598`). The alternates-list slot's projection
   arguably overlaps that R-item's open question (an LLM that sees
   UC-D as an alternate on a UC-C active session may decide to
   investigate account state). Decision asked: is there a sequencing
   relationship the deliver agent should preserve?

## 8. Anti-hardcode self-walk (§5.4 hard gate)

Sprint 30 is a docs-only architectural-design sprint per dev-prompt
§1 and sprint-objective §1; per `iteration_governance.md` §4.1, pure
docs-only PRs are exempt from per-PR Anti-Hardcode review and may
return `approve` with a one-line exemption note. The diff stages
exactly three files: a new design doc, a new handoff doc, and a
single R-item append in `docs/action_bank.md`. No `.java`, `.py`,
`.yml`, `.yaml`, `.properties`, prompt, CaseSpec, judge, override,
foundational doc, governance doc, or sprint-archive edit. Exemption
verdict for Codex (if dispatched): `approve — exemption: docs-only
architectural-design sprint, no semantic surface touched`.

The hard-gate check is on the **content of the recommendation**, not
on the diff itself — Sprint 30 chose Option β, and the recommendation
SHALL be a soft signal per §5.4 / dev-prompt §7. Walking the §5.4 bar:

- **No keyword / regex / if-else** in the data-source pipeline that
  would synthesise an alternate UC from a phrase. **Pass.** Option β
  surfaces existing `RoutingResult.ambiguousCandidates` data; no new
  matcher introduced anywhere.
- **No per-UC matrix** that maps "if active UC is X and message
  contains Y, alternate is Z." **Pass.** Option β does not introduce
  any per-UC mapping logic; it captures the intake router's
  deterministic single output and surfaces it through the projection
  unchanged.
- **No Tier-0 invariant added.** **Pass.** Sprint 30 design SHALL
  NOT propose a new Tier-0 invariant; design doc §7 explicitly states
  Sprint 31 SHALL NOT add one; if implementation surfaces a need,
  raise to deliver agent for promotion consideration. No
  `human_review_required` exit per §3.2 default tail required.
- **No promotion of `RuntimeIntentClassifier`'s existing pattern set
  to a "list".** **Pass.** Option β explicitly does not touch the
  classifier. Options α and δ both discussed the boundary case
  honestly per dev-prompt §7 directive (design doc §3.1 and §3.4
  walk both); both were ruled out in favour of Option β.

The recommendation is a clean soft signal: the LLM reads
`alternate_candidate_use_cases` and decides; no Java branch acts on
the value; the projection is the only consumer. The design pass did
NOT silently dress a hard branch as soft. The design pass did NOT
exit as `human_review_required` because Option β satisfies the
soft-signal bar without ambiguity. No mid-sprint Tier-0 candidate
was raised.

Walking the nine §4.1 questions briefly for traceability (Sprint 30
diff itself, NOT the Sprint 31 recommendation):

1. **Adds keyword / regex / if-else / enum / per-UC matrix?** — No.
   Diff adds two doc files + one R-item table row.
2. **Justified by Tier-0?** — N/A. No Tier-0 added.
3. **Soft signal alternative considered?** — Yes. The chosen
   recommendation IS a soft signal; Options α / γ-a / δ were
   considered and ruled out for §1.7 boundary reasons; Option β was
   recommended.
4. **Visible-eval / trace phrasing / CaseSpec id encoded?** — No.
   Design doc references `cs_example_001` as the hypothetical brief
   from `iteration_governance.md` §135; no real CaseSpec id quoted in
   any rubric / override / persona / runtime / prompt surface.
5. **Moves semantic ownership from LLM to Java?** — No. The
   recommendation surfaces existing deterministic data as a soft
   signal the LLM reads; no Java enforcement is added.
6. **Adds if-else in prompt?** — No. The proposed Sprint 31 prompt
   edit is one short principle-level teaching paragraph mirroring the
   Sprint 20 / Sprint 23 `already_called` shape; no if-else.
7. **Preserves tool schema / capability / PII floor / grounding
   floor?** — Yes. No tool schema, capability, PII, or grounding
   surface touched in Sprint 30 or in the Sprint 31 plan.
8. **Generalization eval coverage shipped?** — Not applicable.
   Investigation-only / design sprint; design doc §6 stanza pre-fill
   names the target/neighbor/negative/shadow shape Sprint 31 will
   need to populate; concrete counts deferred to the Sprint 31
   case-family-selection turn.
9. **Temporary measure with sunset?** — Not applicable. Design doc is
   not a temporary measure; if Option β's intake snapshot proves
   insufficient on observed traces, a future sprint may layer Option
   γ-a on top.

## 9. Files changed (Sprint 30 diff scope)

| path | change type | one-line description |
|------|-------------|----------------------|
| `docs/proposals/alternate_uc_signal_data_source_design.md` | NEW | Design freeze for the future `alternate_candidate_use_cases` projection slot's data source. 10 sections per dev-prompt §3. ~40K. Recommendation: Option β. |
| `docs/sprints/sprint-030-handoff.md` | NEW | This file. Sprint 30 dev-agent handoff (12 sections per dev-prompt §8 / sprint-objective §11). |
| `docs/action_bank.md` | EDIT (single new R-item append after line 617) | New `R-alternate-uc-signal-data-source` row added in a new "Sprint 30 surfaced R-item (design freeze)" subsection between the Sprint 23 surfaced backlog and §6 Closed action index. Disposition: `proposal (Sprint 30 design freeze; Sprint 31 implements)`. NO edit to any existing R-item; NO close of `R-prompt-phase-plan-directive-followship` at line 450; NO edit to §6 Closed action index. |

Out-of-scope hard-fence (per dev-prompt §5 / sprint-objective §7):

- No `.java` / `.py` / `.ts` / `.tsx` / `.yml` / `.yaml` /
  `.properties` under `server/` / `ui/` / `eval_interactive/`.
- No edits to `server/src/main/resources/prompts/system_prompt.txt`
  or any other prompt asset.
- No new CaseSpecs under `eval_interactive/case_specs/` or any
  case-spec location.
- No edits to existing case families (Sprint 20 / Sprint 29 corpus
  fence).
- No `docs/foundational/**` edit.
- No `docs/current/iteration_governance.md` §7.2 edit (fold-back
  queued per Open Question #3).
- No other `docs/current/**` governance-doc edit.
- No `docs/sprints/sprint-001-*` through `sprint-029-*.md` edit.
- No Tier-0 / latency / budget / model-config edit.
- `R-prompt-phase-plan-directive-followship` at
  `docs/action_bank.md:450` NOT touched.
- No smoke run, no Java test run, no Python test run for evidence
  (none needed — Sprint 30 ships no code).

Pre-existing untouched working-tree mods (managed by the human, not
this dev): `csagent_system_design_review.md`,
`server/src/main/resources/prompts/system_prompt.txt`,
`docs/10-handoff.md` (deliver-agent §1 lead refresh),
`docs/action_bank.md` (Sprint 29 close-window updates — preserved;
Sprint 30's R-item append is additive and below the Sprint 29
modifications), `compact/sprint-029-*-prompt.md`,
`compact/sprint-030-*-prompt.md`, `docs/sprint_objective.md` (Sprint
30 contract; deliver-agent-owned), `docs/codex-findings.md`,
untracked `docs/sprints/sprint-029-objective.md` +
`docs/sprints/sprint-029-handoff.md`,
`eval_interactive/case_specs/case_families/sprint29_directive_probe/`
(Sprint 29 artefacts). None are staged by this dev per dev-prompt §10.

## 10. Layer-classification self-walk (§3)

**Sprint 30 itself.** Per `iteration_governance.md` §7 stanza
exemption clause (pure infra / docs-only / config-governance /
characterization-test sprints exempt). For traceability per §3.2 Q1:
Sprint 30 is docs-only, nothing crashes, deterministic output, no
runtime semantic surface touched — `infra` layer classification.
Sprint-objective §9 already lands this for the deliver-agent audit
trail; Sprint 30 dev concurs.

**Sprint 31 prospective (chosen Option β).** `prompt_projection`
layer per §3.2 Q3 — *"Did the LLM choose validly within the
available options, but the projection / context handed to it was
wrong or impoverished (missing slot, missing candidate, missing
diagnostic)? → `prompt_projection`."* Option β surfaces an existing
runtime data source (the intake router's `RoutingResult.ambiguousCandidates`)
that is currently impoverished in the projection (the LLM sees only
`session.active_use_case` + the single-element `candidate_use_cases`
slot, never the intake-ambiguous alternates the router considered).
Small `skill_state` aspect (the new field must survive across turns
— one new column persists the snapshot) but that is a persistence
concern, not a state-machine concern; the runtime does not enforce
any decision based on the field's value.

No layer is `java_guard`. No Tier-0 invariant is named. No
`semantic_planner` (no runtime semantic decision surface widens). No
`eval_spec` (no CaseSpec / rubric / judge change). No
`product_policy` (no product / policy decision required). No
`judge_calibration` (no judge involved). No `human_review_required`
exit triggered.

## 11. R-item registration

Exact text appended to `docs/action_bank.md` (after line 617 — the
last row of the Sprint 23 surfaced backlog
`R-per-llm-call-latency-instrumentation` — and before line 619 §6
Closed action index):

```markdown
**Sprint 30 surfaced R-item (design freeze)**

Sprint 30 (2026-05-16) was a docs-only architectural-design sprint
that produced one proposal doc at
`docs/proposals/alternate_uc_signal_data_source_design.md` naming the
data source for the future `alternate_candidate_use_cases` projection
slot whose hypothetical was sketched in
`docs/current/iteration_governance.md` §7.2. Premise check verified
all five §4 items in `docs/sprint_objective.md` against HEAD `df8b8cd`;
no drift. The design pass evaluated five candidate data sources (α
through ε) and recommends Option β (capture `RoutingResult.AMBIGUOUS`
candidates at intake, project across the session). Sprint 30 ships
ZERO code, ZERO prompt change, ZERO CaseSpec, ZERO Tier-0 invariant.

| id | source | description |
|----|--------|-------------|
| R-alternate-uc-signal-data-source | Sprint 30 design freeze (`docs/proposals/alternate_uc_signal_data_source_design.md`); §7.2 worked-example premise gap | `prompt_projection` layer per §3.2 Q3 (chosen Option β surfaces existing intake-time data to the LLM without widening any semantic decision surface). Disposition: **proposal (Sprint 30 design freeze; Sprint 31 implements).** … [single-paragraph R-item description per the existing row shape — full text in `docs/action_bank.md`] … |
```

The full R-item description (one prose paragraph in the third column)
names: the chosen Option β; the layer per §3.2 Q3 with one-sentence
justification; the 5 candidate options walked (α / β / γ / δ / ε)
with one-line dispositions each; the Constitution §1.3 / §1.5 / §1.7
justification for Option β; Sprint 31 sizing (small-to-medium,
single-track, ~2–3 days dev + 1 day review); the code-paths touch
list (`BotSession.java` new field, Flyway V<next> migration,
`SessionManager.java:110 / 185–189 / 255–259`,
`ContextProjectionBuilder.java` new slot adjacent to line 394,
`system_prompt.txt` one short teaching paragraph, new regression test
file); Sprint 31 out-of-scope (shadow planner mode, Options α / γ /
δ surfaces, Tier-0 invariants, new CaseSpecs); the 5 open questions
for the human; the Sprint 30 sprint-itself layer classification per
§3.2 Q1 (`infra`); and no `human_review_required` exit (Option β
satisfies §1.7). Pointer to detail at
`docs/proposals/alternate_uc_signal_data_source_design.md` +
`docs/sprints/sprint-030-handoff.md`.

R-item NOT closed; opens with disposition `proposal (Sprint 30
design freeze; Sprint 31 implements)`. No existing R-item touched;
`R-prompt-phase-plan-directive-followship` at line 450 untouched;
§6 Closed action index untouched (deliver-agent's close-turn job).

Bundle policy per sprint-objective §8: the R-item registration and
the design doc are bundled into the same commit (or a follow-on
commit — dev's choice). This dev session stages all three files for
a single commit to simplify the human's review surface; the human
runs the commit per `feedback_commit_at_end_bundles_deliver_artefacts.md`.

## 12. Closure verdict (filled at sprint close; deliver-agent owned)

Per `feedback_handoff_verdict_section_delegation.md`, this section is
filled by the deliver agent at sprint close, not by the dev agent.
Placeholder:

| field | value |
|-------|-------|
| status | *(deliver-agent fills at close)* |
| classification | *(deliver-agent fills at close — A / A-with-* / B / C variants per the Sprint 26 / Sprint 27 / Sprint 28 close precedent)* |
| Codex outcome | *(deliver-agent fills at close — likely "intentionally skipped per §4.1 docs-only exemption clause" per Sprint 26 / 27 precedent; human discretion)* |
| R-item disposition applied | `R-alternate-uc-signal-data-source` opens as `proposal (Sprint 30 design freeze; Sprint 31 implements)`. Re-open trigger: Sprint 31 lands the chosen design + smoke rerun observes the new `alternate_candidate_use_cases` slot in `case_results[].per_turn_trace[].projection`; OR explicit later human direction to revisit. |
| date | 2026-05-16 (dev work date; close date filled at close) |
