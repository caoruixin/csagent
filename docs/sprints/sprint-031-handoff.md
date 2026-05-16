---
title: Sprint 31 handoff — alternate_candidate_use_cases projection slot (Option β implementation)
doc_tier: sprint-archive
status: current
implementation_status: partial
source_of_truth: this file
last_reviewed: 2026-05-16
review_cadence: per sprint
supersedes: []
superseded_by: null
notes: >
  Sprint 31 ships the Option β runtime + prompt change frozen in
  `docs/proposals/alternate_uc_signal_data_source_design.md` (Sprint
  30 close, 2026-05-16). Code lands and the new
  `alternate_candidate_use_cases` projection slot is verified at the
  unit + integration test layers AND in the live smoke `per_turn_trace[]`.
  However, the §10 smoke acceptance check surfaced a regression on the
  composite-score / outcome / judge floors vs the Sprint 28 reference,
  with one previously-stable case (cs_interactive_029) flipping to
  `contract_violation`. Per Sprint 31 dev prompt §9.3, the dev STOPPED
  rather than silently working around. Closure verdict (§12) deferred
  to the human + deliver-agent.
---

# Sprint 31 handoff — alternate_candidate_use_cases projection slot (Option β)

Date: 2026-05-16
Branch: `refactor/remove-the-shackles`
HEAD at session start (smoke reference): `3812153` (Sprint 30 close).
Sprint class: implementation, single-track, semantic-touching. Layer:
`prompt_projection` per `iteration_governance.md` §3.2 Q3.

## 1. Context Pack

Per `docs/current/agent_context_guide.md` Context Pack Prompt; produced
before any code.

### 1.1 Relevant docs (sampled & read)

- `AGENTS.md` — durable-connective; current. Constitution-chain entry;
  transitively loads `doc_governance.md`, `agent_context_guide.md`,
  `iteration_governance.md`.
- `docs/sprint_objective.md` — current-runtime; current. Sprint 31
  scope; §4 premise re-verification item, §5 pre-picked defaults for
  OQ1–OQ5, §6 file table (7 rows), §7 hard fences (15 items), §9 §7
  stanza, §10 success metrics, §11 12-section handoff contract, §12
  stop conditions.
- `docs/proposals/alternate_uc_signal_data_source_design.md` —
  proposal; design-freeze. §2 premise gap, §3 5-option evaluation, §4
  Option β justification, §5 code-paths table (the verbatim §6 ship
  list), §6 §7 stanza pre-fill, §8 5 open questions.
- `docs/sprints/sprint-030-handoff.md` — sprint-archive; archived.
  Sprint 30 design pass (docs-only); §4–§7 explain Option β over α / γ
  / δ / ε.
- `docs/sprints/sprint-020-handoff.md` — sprint-archive; archived. §5
  `already_called` slot architectural precedent that Sprint 31 mirrors
  verbatim.
- `docs/sprints/sprint-023-handoff.md` — sprint-archive; archived. §3
  / §5 — `already_called` system_prompt teaching paragraph precedent
  (sibling-placement target per OQ2).
- `docs/sprints/sprint-028-handoff.md` — sprint-archive; archived. §11
  / §12 — `per_turn_trace[]` enrichment Sprint 31 reads at smoke
  acceptance; reference run is
  `eval_interactive/results/20260514-181257/results.json`.

### 1.2 Relevant code paths (verified at session start, 2026-05-16, HEAD `3812153`)

- `server/src/main/java/com/gumtree/csagent/model/BotSession.java`
  lines 52–54 — existing `candidateUseCases: String[]` field (mirror
  target for new `intakeAmbiguousCandidates`).
- `server/src/main/java/com/gumtree/csagent/service/runtime/SessionManager.java`
  lines 142–194 — routing-outcome switch (ROUTED 143–158, OUT_OF_SCOPE
  159–183, AMBIGUOUS **185–189**, default 190–193). The AMBIGUOUS
  branch reads `routingResult.outcome()` and proceeds to
  `setCurrentPhase("DISCOVER")` + `buildAmbiguousGreeting(...)`; the
  `routingResult.ambiguousCandidates()` accessor is **not consulted**
  pre-Sprint-31. Confirmed by direct read at session start.
- `server/src/main/java/com/gumtree/csagent/service/runtime/UseCaseRouter.java`
  lines 750–770 — `RoutingResult` record carries
  `ambiguousCandidates: List<String>`; factory `RoutingResult.ambiguous(candidates)`
  at line 767. Confirmed unchanged.
- `server/src/main/java/com/gumtree/csagent/service/runtime/ContextProjectionBuilder.java`
  lines 380–394 — existing `candidate_use_cases` projection (mirror
  target for new `alternate_candidate_use_cases` slot).
- `server/src/main/resources/prompts/system_prompt.txt` lines 23–28 —
  existing `already_called` teaching paragraph (sibling-placement
  target per OQ2 pre-pick).
- `server/src/main/resources/db/migration/V13__add_consecutive_deadline_count.sql`
  — latest Flyway migration before Sprint 31; the new migration is
  V14.

### 1.3 Doc-status warnings (drift observed)

1. `docs/current/iteration_governance.md` §7.2 worked example
   references *"a soft signal — a list of UCs the existing
   `RuntimeIntentClassifier` already surfaces"*. After Sprint 31
   close, the data source is `RoutingResult.ambiguousCandidates`
   (intake-time router snapshot), not the classifier. Per OQ3
   pre-pick the fold-back is deferred to the normal 3–5-sprint
   governance cadence; this handoff §7 flags the drift for the
   fold-back agent.
2. `SystemPromptUserRequestedTiebreakerTest` is the documented
   inherited failure from the unauthored working-tree mod on
   `system_prompt.txt:60` (removal of the `(Sprint 6 §G1)`
   parenthetical from `ACTIVE-UC TIEBREAKER`). Sprint 31's own
   edit to `system_prompt.txt` is upstream of that line and does
   not touch `Sprint 6` or `ACTIVE-UC TIEBREAKER`; the inherited
   failure is preserved exactly (see §11).

### 1.4 Source-of-truth decision

- For the §6 ship list: `docs/proposals/alternate_uc_signal_data_source_design.md`
  §5 is authoritative per Sprint 31 dev prompt §1 loader stanza item
  3 and §3 introduction.
- For the §7 stanza wording: the design-doc §6 pre-fill is
  authoritative; Sprint 31 fills the remaining counts in §10 below.
- For the smoke acceptance bars: `docs/sprint_objective.md` §10 is
  authoritative; the Sprint 28 reference run is
  `eval_interactive/results/20260514-181257/results.json`.

### 1.5 Implementation status (sprint-start)

`not_started`. No `BotSession.intakeAmbiguousCandidates` field; no
V14 Flyway migration; no `alternate_candidate_use_cases` projection
slot; no `system_prompt.txt` teaching paragraph for the slot; no
regression test file. All verified by direct read at session start.

### 1.6 Risks before coding

1. **`SessionManager.java:185–189` premise re-verification.** The
   §4 premise of `sprint_objective.md` says this branch discards
   `routingResult.ambiguousCandidates()`. Verified at session start
   (see §3 below).
2. **Working-tree `system_prompt.txt` mod on line 60** removes
   `(Sprint 6 §G1)` and is the inherited failure baseline. Sprint
   31's own prompt edit is upstream of that line; the staging
   discipline at commit time MUST exclude that unrelated hunk.
3. **Pre-existing uncommitted files in working tree** (per dev
   prompt §10) include `docs/10-handoff.md`,
   `docs/sprint_objective.md`, `docs/sprints/sprint-030-handoff.md`,
   `csagent_system_design_review.md`, and the deliver-agent-owned
   `compact/sprint-031-*.md` + `docs/sprints/sprint-030-objective.md`.
   None of these are Sprint 31 dev's; commit discipline must not
   bundle them.
4. **Smoke acceptance bar.** Even with the slot landed, semantic-
   touching prompt changes can shift LLM behavior in stochastic
   ways. The §10 acceptance check requires no regression vs Sprint
   28 reference on composite / pass-rate / safety / grounding /
   wrong-containment / over-escalation floors. **This risk
   materialised — see §5 and §11.**

## 2. Sprint-objective recap

Per `docs/sprint_objective.md` §2: ship the Option β design frozen
in Sprint 30. Concretely:

- New persisted `BotSession.intakeAmbiguousCandidates: String[]` field
  + V14 Flyway migration.
- `SessionManager.createSession`'s AMBIGUOUS branch
  (`SessionManager.java:185–189`) captures
  `routingResult.ambiguousCandidates()` onto the new field before the
  existing `setCurrentPhase("DISCOVER")` call.
- ROUTED / OUT_OF_SCOPE branches leave the field null per OQ1 pre-pick.
- `ContextProjectionBuilder.buildProjection` adjacent to the existing
  `candidate_use_cases` block emits an `alternate_candidate_use_cases`
  array built from `session.getIntakeAmbiguousCandidates()` minus
  the active UC. Schema-stable across turns (always present; empty
  array when no snapshot OR filter removes all entries).
- `system_prompt.txt` adjacent to the existing `already_called`
  teaching paragraph (lines 23–28) carries one new short teaching
  paragraph per OQ2 pre-pick, naming the slot + its intake-router
  provenance + the soft-signal posture.
- One new regression test file with 5–8 tests mirroring the Sprint
  20 `AlreadyCalledProjectionTest` shape, plus a non-enforcement
  integration test mirroring
  `AgentRunLoopAlreadyCalledNonEnforcementIntegrationTest`.

§7 hard fences (no edits to `RuntimeIntentClassifier`,
`IntentClassification`, `DriftResult`, `DriftDetector`,
`UseCaseRouter`, `ClassifyUseCaseTool`, foundational docs,
governance docs, sprint archives, CaseSpecs, Tier-0 catalogue,
deadline/model/retry/budget config, the §I0
`R-prompt-phase-plan-directive-followship` R-item at
`docs/action_bank.md:450`).

## 3. Premise re-verification

`SessionManager.java:185–189` AMBIGUOUS branch at HEAD `3812153`,
re-read by the dev at session start:

```java
case AMBIGUOUS -> {
    // Move to DISCOVER to disambiguate
    session.setCurrentPhase("DISCOVER");
    greeting = buildAmbiguousGreeting(firstName, topicSubject);
}
```

`routingResult.ambiguousCandidates()` is NOT consulted in this
branch; the list is produced by `UseCaseRouter` (verified at
`UseCaseRouter.java:767` — `RoutingResult.ambiguous(candidates)`
returns `new RoutingResult(RoutingOutcome.AMBIGUOUS, null, null, null, candidates)`)
and discarded at the SessionManager-AMBIGUOUS boundary.

The premise holds at HEAD `3812153`. The `RoutingResult` record's
`ambiguousCandidates()` accessor is intact; the
`RoutingResult.ambiguous(candidates)` factory at line 767 is
unchanged. No drift surfaced.

Spot-checks (per dev prompt §2):

- `BotSession.java:52–54` — `candidateUseCases: String[]` mirror
  target intact. Confirmed.
- `ContextProjectionBuilder.java:380–394` — `candidate_use_cases`
  projection emission intact. Confirmed.
- `system_prompt.txt:23–28` — `already_called` teaching paragraph
  intact. Confirmed.
- Latest Flyway migration = `V13__add_consecutive_deadline_count.sql`.
  Confirmed. Sprint 31's new migration is V14.

## 4. Implementation walkthrough

Each §6 row of `docs/sprint_objective.md` is landed verbatim; no
opportunistic fields added; no hard-fenced surface touched.

### 4.1 `BotSession.intakeAmbiguousCandidates` field

`server/src/main/java/com/gumtree/csagent/model/BotSession.java` —
new field declared adjacent to `candidateUseCases` (immediately
after lines 52–54). Annotated identically:

```java
@JdbcTypeCode(SqlTypes.ARRAY)
@Column(name = "intake_ambiguous_candidates", columnDefinition = "text[]")
private String[] intakeAmbiguousCandidates;
```

Lombok `@Data` generates the accessor pair; null is the valid
"no snapshot" default; no `@Builder.Default` is required (Lombok's
builder leaves uninitialised fields null).

A Javadoc paragraph on the field cites Sprint 31, names the
`SessionManager` AMBIGUOUS-branch writer, names the
`ContextProjectionBuilder` reader, and explicitly tags the slot as
soft signal with no runtime enforcement.

### 4.2 V14 Flyway migration

`server/src/main/resources/db/migration/V14__intake_ambiguous_candidates.sql`
— new file. Contents (verbatim):

```sql
ALTER TABLE bot_sessions
    ADD COLUMN IF NOT EXISTS intake_ambiguous_candidates text[];
```

`ADD COLUMN IF NOT EXISTS` ensures idempotency on an already-
migrated DB. Backfill is not required: null is the valid "no
snapshot" state. No constraint, no default, no NOT NULL — the
field is observable-only.

### 4.3 `SessionManager.createSession` AMBIGUOUS-branch assignment

`server/src/main/java/com/gumtree/csagent/service/runtime/SessionManager.java`
— AMBIGUOUS branch of the routing switch (the lines that read
`case AMBIGUOUS ->` per §3 premise). Edit inserts the snapshot
assignment BEFORE the existing `setCurrentPhase("DISCOVER")` call:

```java
case AMBIGUOUS -> {
    // Sprint 31 — Option β: preserve the intake-time alternate-UC
    // snapshot the router considered plausible for this
    // topic-subject family. Until Sprint 31 this list was
    // discarded; the slot is now persisted onto the session and
    // surfaced as the per-turn `alternate_candidate_use_cases`
    // projection soft signal. The LLM owns whether to act on it;
    // the runtime does NOT branch on the value.
    List<String> ambiguousCandidates = routingResult.ambiguousCandidates();
    session.setIntakeAmbiguousCandidates(
            ambiguousCandidates == null
                    ? null
                    : ambiguousCandidates.toArray(new String[0]));
    // Move to DISCOVER to disambiguate
    session.setCurrentPhase("DISCOVER");
    greeting = buildAmbiguousGreeting(firstName, topicSubject);
}
```

`java.util.List` is available via the existing `import java.util.*;`
on line 17 — no new import needed.

The null-guard is defensive: `RoutingResult.ambiguous(List.of())`
produces a non-null empty list (kept on the AMBIGUOUS path's
fallback at line 135), and that becomes an empty `String[0]` on
the session — consistent with the empty-array shape the
projection filter handles in §4.5. The defensive null path is
exercised by test `routedIntake_leavesIntakeAmbiguousCandidatesNull`
and the OUT_OF_SCOPE variant (neither sets the field, leaving
Lombok's null default).

### 4.4 ROUTED / OUT_OF_SCOPE branches — left null per OQ1 pre-pick

Per `docs/sprint_objective.md` §5 OQ1 pre-pick: no edit to the
ROUTED branch (lines 143–158) or to the OUT_OF_SCOPE branch
(lines 159–183). Lombok's builder default leaves
`intakeAmbiguousCandidates` null on those paths; no explicit
`null` assignment is required. The `if (session.getCandidateUseCases() == null)`
ensure-not-null guard at lines 197–199 is **not** mirrored for
the new field — null is a valid "no snapshot" state for the
alternates field by design (vs `candidateUseCases` which is a
non-null-empty-array surface for `text[]` Postgres compatibility
predating Sprint 8).

The two negative-case tests (`routedIntake_leavesIntakeAmbiguousCandidatesNull`
and `outOfScopeIntake_leavesIntakeAmbiguousCandidatesNull` — see
§9) pin this OQ1-adherence.

### 4.5 `ContextProjectionBuilder` — new slot

`server/src/main/java/com/gumtree/csagent/service/runtime/ContextProjectionBuilder.java`
— new block inserted immediately after the existing
`candidate_use_cases` emission (after the line that reads
`projection.set("candidate_use_cases", candidateUcsNode);`):

```java
ArrayNode alternateCandidateUcsNode = objectMapper.createArrayNode();
String activeUcForAlternate = session.getActiveUseCase();
if (session.getIntakeAmbiguousCandidates() != null) {
    for (String uc : session.getIntakeAmbiguousCandidates()) {
        if (uc != null && !uc.isBlank() && !uc.equals(activeUcForAlternate)) {
            alternateCandidateUcsNode.add(uc);
        }
    }
}
projection.set("alternate_candidate_use_cases", alternateCandidateUcsNode);
```

Filter semantics:

- Null snapshot → empty array.
- Empty snapshot → empty array.
- Snapshot containing only the active UC → empty array (after
  filter).
- Snapshot of N > 1 UCs with active UC inside → N − 1 element
  array.
- Null `activeUseCase` → no filter (all non-blank entries survive)
  — this is the post-AMBIGUOUS pre-classification turn shape;
  intentional, so the LLM sees the full alternate set on the very
  first DISCOVER turn.

Slot is always present in the projection for shape stability
(§N0 nullable-field convention). Verified by the unit test
`projection_schemaStability_keyPresentOnEveryTurn`.

The slot is added to the `buildProjection(...)` body — the
shared path both `build(...)` overloads delegate to (see
`ContextProjectionBuilder.java:707` 6-arg `build` → line 717
delegates `baseJson = buildProjection(...)`). Both runtime call
sites — the legacy 5-arg `build` (`AgentRunLoopImpl` and other
non-loop callers) and the 6-arg `build` (`AgentRunLoopImpl`
post-Sprint-20) — see the slot in their projection JSON without
any constructor / signature change. Consistent with
`docs/proposals/alternate_uc_signal_data_source_design.md` §7
hard fence "No `ContextProjectionBuilder` constructor / signature
change".

### 4.6 `system_prompt.txt` — teaching paragraph

`server/src/main/resources/prompts/system_prompt.txt` — new
paragraph inserted IMMEDIATELY after the existing `already_called`
teaching block (lines 23–28), before the DISCOVER phase guidance
heading. Per OQ2 pre-pick: sibling placement to `already_called`.

Paragraph shape (verbatim):

```
Alternate candidate use cases (the `alternate_candidate_use_cases` projection slot):
The per-turn projection includes an `alternate_candidate_use_cases` array. Each entry names a use case the intake router considered plausible for this session's topic-subject family at session creation — i.e. the UCs the user's intake message was ambiguous between. The router chose the session's `active_use_case` and surfaced the rest here as observable evidence; the currently active UC is excluded from the array.
- When a later user turn surfaces evidence that the active UC is no longer the best fit (the user starts talking about a different concern that maps to one of the alternates), the slot tells you which alternates the router already considered plausible. You own the judgement of whether to ask a clarifying question, propose a reroute, or stay on the active UC. The runtime does not block dispatch or gate any phase transition on this slot.
- An empty `alternate_candidate_use_cases` array means either the intake routed deterministically to one UC (no alternates considered) or the active UC was the only surviving candidate after filtering. Proceed normally.
This is intake-time evidence; mid-session shifts the intake router did not anticipate may not appear in the slot. Treat it as one input among many, weighted by the current turn's content.
```

Principled — no `search_knowledge` / `resolve_article` / per-UC
matrix branching; the literal strings `UC-A` / `UC-B` / … / `UC-K`
/ `UC-FP` do NOT appear in the block (verified by re-read; also
verified indirectly by `AlreadyCalledPromptConsumptionTest`'s
1400-char window check around `already_called`, which is unaffected
by the new paragraph because the new paragraph immediately follows
the already_called block but does not contain those strings).
Ownership cues present: "You own the judgement" + "does not block
dispatch" — matching the `already_called` shape's "you own" /
"not block" language.

### 4.7 Regression tests

Two new test files (see §9 for paths + counts):

- Unit test
  `IntakeAmbiguousCandidatesProjectionTest.java` — 9 tests
  covering the three behaviour bars (AMBIGUOUS captures, ROUTED
  null, projection filter), plus schema-stability and the
  filter-edge cases. Loaded via `@ExtendWith(MockitoExtension.class)`,
  mocks `BotSessionRepository` + `BotEventRepository` +
  `SessionOutcomeRepository` + `MockHandoverLogRepository` +
  `BotTurnRepository` + `FormContextIngestionService` +
  `UseCaseRouter` + `ControlKernel` + `ControlPolicyService` +
  `UseCaseRegistryService` + `ToolPolicyEnforcer`; uses
  `ArgumentCaptor<BotSession>` on `sessionRepository.save(...)` to
  capture the post-creation session.
- Integration test
  `AgentRunLoopAlternateCandidateUseCasesNonEnforcementIntegrationTest.java`
  — 1 test mirroring the Sprint 20
  `AgentRunLoopAlreadyCalledNonEnforcementIntegrationTest` shape.
  Real `AgentRunLoopImpl` + real `ContextProjectionBuilder` (so the
  actual projection JSON containing the slot is exercised); mocks
  for `LlmInvocationService` + `ToolDispatcher` + `ActionParser` +
  `UseCaseRegistryService` + `ControlPolicyService` +
  `ToolPolicyEnforcer`. Sets a session with
  `intakeAmbiguousCandidates = ["UC-A", "UC-C"]` and
  `activeUseCase = "UC-A"`; runs the loop; verifies (a) the
  projection captured from `llmInvocation.invokeChat(...)` carries
  `alternate_candidate_use_cases = ["UC-C"]` (active UC filtered);
  (b) the loop reaches `TerminalOutcome.FINAL_ANSWER` — the
  populated slot does NOT gate dispatch or terminate the loop
  early.

10 new tests total; 10 / 10 pass (see §11).

## 5. Worked-example smoke rerun

### 5.1 Run command

```bash
cd /Users/caoruixin/projects/csagent-latest/eval_interactive
uv run eval-interactive run --set smoke --label sprint-31-alt-uc-slot
```

Bot probed pre-run with
`curl -sS -o /dev/null -w "HTTP %{http_code}\n" http://localhost:8080/v1/demo/sessions`
→ `HTTP 200` (bot was restarted by the human after Sprint 31 code
landed so the live smoke exercises the new slot — see §7 §11
notes).

### 5.2 Result location

`eval_interactive/results/20260516-024934/results.json` (label
`sprint-31-alt-uc-slot`; 14 cases; elapsed 202289 ms wall).

### 5.3 Summary metrics — Sprint 31 vs Sprint 28 reference

| metric | Sprint 28 ref (`20260514-181257/`) | Sprint 31 (`20260516-024934/`) | delta |
|---|---:|---:|---:|
| total_cases | 14 | 14 | 0 |
| passed_cases | 2 | 1 | **−1** |
| failed_cases | 12 | 13 | +1 |
| task_success_rate | 0.1429 | 0.0714 | **−0.0715** |
| stall_rate | 0.0714 | 0.0714 | 0 |
| mean_composite_score | 0.1299 | **0.0617** | **−0.0682 (−52%)** |
| mean_outcome_score | 0.7302 | 0.6141 | **−0.1161 (−16%)** |
| mean_judge_score | 0.6667 | 0.5714 | **−0.0953 (−14%)** |
| mean elapsed_ms | 26437.64 | 29345.07 | +2907.43 (+11%) |

Recipe:

```bash
jq '.summary | {total_cases, passed_cases, failed_cases, task_success_rate, stall_rate, mean_composite_score, mean_outcome_score, mean_judge_score}' \
  eval_interactive/results/20260516-024934/results.json
jq '[.case_results[].elapsed_ms] | add / length' \
  eval_interactive/results/20260516-024934/results.json
```

Sprint 31's mean composite (0.0617) is **below** the floor across
five recent smoke baselines on the 14-case set:

| run | passed | mean_composite |
|---|---:|---:|
| `20260513-042712` | 4 / 14 | 0.2674 |
| `20260514-111724` | 4 / 14 | 0.2491 |
| `20260514-114628` | 2 / 14 | 0.1371 |
| `20260514-181257` (Sprint 28 ref) | 2 / 14 | 0.1299 |
| **`20260516-024934` (Sprint 31)** | **1 / 14** | **0.0617** |

Recipe:

```bash
for run in 20260513-042712 20260514-111724 20260514-114628 20260514-181257 20260516-024934; do
  jq -r "\"$run: passed=\(.summary.passed_cases) mean_comp=\(.summary.mean_composite_score)\"" \
    eval_interactive/results/$run/results.json
done
```

**This meets Sprint 31 dev prompt §9.3 stop condition** (smoke
regression on composite_score / outcome / judge floors vs the
Sprint 28 reference). The dev STOPPED rather than silently working
around. The closure verdict (§12) is deferred to the human +
deliver-agent.

### 5.4 New slot — presence on AMBIGUOUS-intake cases

The `alternate_candidate_use_cases` slot appears in
`case_results[].per_turn_trace[].projection` on every populated
turn:

| presence | turn count |
|---|---:|
| key present (true) | 22 |
| key absent (false) | 1 |

The single absent turn is on the cs_interactive_029 case (see
§5.5 below) which contract-violated at turn 0 with an empty
`per_turn_trace[]` for that specific turn slot; the absence
reflects a defensive {}-projection branch in the eval harness,
not a Sprint 31 schema break. Recipe:

```bash
jq -r '[.case_results[].per_turn_trace[]? | .projection | has("alternate_candidate_use_cases")] | group_by(.) | map({k: .[0], n: length})' \
  eval_interactive/results/20260516-024934/results.json
```

Four AMBIGUOUS-intake cases carry **non-empty** alternate lists
(the cases the §10 acceptance bar asks for ≥1 of):

| case_id | alternates (post-filter, deduplicated) |
|---|---|
| cs_interactive_015 | `["UC-A","UC-FP","UC-H"]` |
| cs_interactive_040 | `["UC-A","UC-B","UC-C","UC-D","UC-E","UC-F","UC-FP","UC-G","UC-H","UC-I","UC-J","UC-K"]` |
| cs_interactive_176 | `["UC-E","UC-K"]` |
| cs_interactive_192 | `["UC-A","UC-B","UC-FP"]` |

Recipe:

```bash
jq -r '.case_results[] | select(.per_turn_trace[]?.projection.alternate_candidate_use_cases | length > 0) | "\(.case_id): alts=\([.per_turn_trace[].projection.alternate_candidate_use_cases // []] | flatten | unique)"' \
  eval_interactive/results/20260516-024934/results.json
```

cs_interactive_040's 12-element alternates list is the
`AMBIGUOUS_FALLBACK_CANDIDATES`-shaped result from the fallback
path at `SessionManager.java:135` (`RoutingResult.ambiguous(java.util.List.of())`)
or a router pass that returned all available UCs as plausible — the
post-filter set excludes none because `session.getActiveUseCase()`
is null on the unresolved AMBIGUOUS path at first projection (the
classify_use_case call hasn't fired). This is consistent with the
filter semantics in §4.5 ("Null `activeUseCase` → no filter") and
is the expected shape.

### 5.5 Per-case shifts vs Sprint 28 reference

| case_id | Sprint 28 composite | Sprint 31 composite | Sprint 28 stop | Sprint 31 stop | notes |
|---|---:|---:|---|---|---|
| cs_interactive_001 | 0.0 | 0.0 | bot_ended | bot_ended | stable |
| cs_interactive_002 | 0.0 | 0.0 | bot_ended | bot_ended | stable |
| cs_interactive_011 | 0.0 | 0.0 | goal_impossible | goal_impossible | stable |
| **cs_interactive_014** | **0.8524** | **0.0** | bot_ended (3 turns) | goal_impossible (4 turns) | regressed — case is historically high-variance: 0.0 / 0.0 / 0.8524 / 0.0 across 4 recent runs (see below) |
| cs_interactive_015 | 0.0 | 0.0 | goal_achieved | bot_ended | classified-as-failed in both runs |
| **cs_interactive_029** | **0.9667** | **0.0** | bot_ended (5 turns, elapsed 7012) | **contract_violation** (0 turns, elapsed 0) | **load-bearing regression** — case had been stably passing at 0.9667 across 3 prior runs; now fires `CONTRACT_VIOLATION:active_use_case` (`field='active_use_case'`, `phase='session'`, `reason='missing_after_turns'`) with zero turns / zero elapsed |
| cs_interactive_036 | 0.0 | **0.8643** | bot_ended | bot_ended | **improved** — Sprint 31 newly passes (UC-I; the only Sprint 31 pass) |
| cs_interactive_038 | 0.0 | 0.0 | bot_ended | bot_ended | stable |
| cs_interactive_040 | 0.0 | 0.0 | bot_ended | bot_ended | stable |
| cs_interactive_066 | 0.0 | 0.0 | bot_ended | bot_ended | stable |
| cs_interactive_095 | 0.0 | 0.0 | goal_impossible | bot_ended | classified-as-failed in both runs |
| cs_interactive_176 | 0.0 | 0.0 | bot_ended (4 turns) | bot_ended (12 turns) | classified-as-failed in both runs; turn count widened |
| cs_interactive_192 | 0.0 | 0.0 | bot_ended | bot_ended | stable |
| cs_interactive_259 | 0.0 | 0.0 | contract_violation | contract_violation | stable (inherited shape per Sprint 28) |

Recipe:

```bash
jq -r '.case_results[] | "\(.case_id): composite=\(.composite_score) stop=\(.stop_reason) turns=\(.total_turns) elapsed=\(.elapsed_ms)"' \
  eval_interactive/results/20260516-024934/results.json
jq -r '.case_results[] | "\(.case_id): composite=\(.composite_score) stop=\(.stop_reason) turns=\(.total_turns) elapsed=\(.elapsed_ms)"' \
  eval_interactive/results/20260514-181257/results.json
```

**Variance check (cs_interactive_014):** the case has fluctuated
across four recent smoke runs. Composite by run:
`20260514-111724` 0.0 (turns=3, elapsed=39408), `20260514-114628`
0.0 (turns=3, elapsed=23103), `20260514-181257` (Sprint 28 ref)
**0.8524** (turns=3, elapsed=49297), `20260516-024934` (Sprint 31)
0.0 (turns=4, elapsed=62044). Three of four recent runs already
failed this case; the Sprint 31 result is consistent with the
two-out-of-four prior-failure pattern, not a Sprint 31 induced
regression. The case is high-variance at the floor.

**Variance check (cs_interactive_029):** stable PASS in three
prior runs, FAIL in Sprint 31:

| run | composite | stop | turns | elapsed |
|---|---:|---|---:|---:|
| `20260514-111724` | 0.9667 | bot_ended | 2 | 6698 |
| `20260514-114628` | 0.9667 | bot_ended | 2 | 4554 |
| `20260514-181257` (Sprint 28) | 0.9667 | bot_ended | 2 | 7012 |
| **`20260516-024934` (Sprint 31)** | **0.0** | **contract_violation** | **0** | **0** |

This is the load-bearing concern. Recipe:

```bash
for run in 20260514-111724 20260514-114628 20260514-181257 20260516-024934; do
  jq -r ".case_results[] | select(.case_id==\"cs_interactive_029\") | \"$run: composite=\(.composite_score) stop=\(.stop_reason) turns=\(.total_turns) elapsed=\(.elapsed_ms)\"" \
    eval_interactive/results/$run/results.json
done
```

The contract-violation detail (from `case_results[*]` for
cs_interactive_029):

```json
{
  "field": "active_use_case",
  "phase": "session",
  "reason": "missing_after_turns",
  "available_keys": [..., "intakeAmbiguousCandidates", ...]
}
```

`available_keys` includes `intakeAmbiguousCandidates` — confirming
Sprint 31's new field reached Hibernate / the BotSession DTO. The
violation's substantive content is `active_use_case` empty in
phase `session` after turns; cs_interactive_029 has `expected_outcome=escalate`,
`primary_uc=UC-D`. Prior runs had `active_use_case=UC-D` stamped
during session creation (ROUTED path).

**The dev did NOT root-cause cs_interactive_029's regression to a
specific Sprint 31 code change.** The candidate hypotheses:

1. The added system-prompt teaching paragraph (Sprint 31 §4.6)
   shifts the LLM behavior on UC-D-routed sessions in a way that
   triggers the eval harness's `missing_after_turns` check
   prematurely. *Plausibility:* the teaching paragraph is upstream
   of all UC-specific guidance and adds ~6 sentences; a non-zero
   LLM-behavior shift is possible but the contract check fires on
   the **session JSON** (post-turns), not on the LLM output, so the
   path would need to be: prompt-shift → LLM-output-shift →
   routing-shift → `activeUseCase` not stamped at create-time.
   But the bot's routing decision in `UseCaseRouter` is
   deterministic (non-LLM); the prompt cannot shift the routing
   outcome. So this hypothesis fails as a single-step explanation.
2. **Bot-restart cold-start race.** The user restarted the bot
   immediately before the smoke run; the smoke run hit
   cs_interactive_029 early in the case list while
   Hibernate / Flyway / Spring Boot warm-up was still completing.
   Some startup-time race could have produced a BotSession row
   where the routing's `setActiveUseCase("UC-D")` did not persist
   before the eval harness queried the session. *Plausibility:*
   matches the elapsed_ms=0 and turns=0 shape (eval harness
   short-circuited on contract violation before driving any
   turn); does NOT match a deterministic causal chain.
3. **Sprint 31's V14 Flyway migration introduced a Hibernate
   ordering issue** that caused the session save to fail silently
   for the specific UC-D row Hibernate produced. *Plausibility:*
   the V14 migration is additive (`ALTER TABLE ADD COLUMN IF NOT EXISTS`)
   and Hibernate's field is annotated identically to
   `candidateUseCases` (which has shipped since Sprint 1). No
   schema mismatch. The mvn test suite (which exercises Flyway via
   the standard Spring Boot test profile) ran clean with zero new
   regressions (912 tests; only inherited
   `SystemPromptUserRequestedTiebreakerTest` failure). The
   Hibernate-ordering hypothesis fails the test-suite evidence.
4. **Smoke variance.** cs_interactive_029 was stable at 0.9667 for
   three runs; the variance hypothesis would require accepting
   that the case is stochastic enough to flip to
   contract_violation on a single rerun. *Plausibility:* low —
   `contract_violation, turns=0, elapsed=0` is not a stochastic
   LLM-output shape; it's a deterministic eval-harness short-
   circuit on the session JSON.

**Most likely cause: bot-restart cold-start race (#2) or a
yet-unidentified interaction the dev did not surface.** The
single-run smoke is insufficient to disambiguate without a second
rerun on a warm bot.

## 6. Generalization coverage table

Mirrors the §9 prospective stanza in `docs/sprint_objective.md`.
Counts as shipped:

| family | target (count) | neighbor (count) | negative (count) | shadow (count) | total |
|---|---:|---:|---:|---:|---:|
| UC-A ↔ UC-C drift (§7.2 worked example) | 0 (CaseSpec deferred to Sprint 31+1 per OQ4) | 0 (deferred) | 0 (deferred) | 0 (deferred) | 0 |
| Sprint 31 Java unit + integration test coverage | 9 unit + 1 integration | n/a (Java-only) | n/a | n/a | 10 |

Per OQ4 pre-pick (Sprint 31 objective §5): Sprint 31's Java
regression coverage asserts the slot **ships correctly**
(AMBIGUOUS captures, ROUTED null, projection filter, schema
stability, non-enforcement) at the runtime layer. End-to-end LLM
behaviour validation against the §7.2 worked example shape
(target / neighbor / negative / shadow CaseSpecs for the UC-A ↔
UC-C drift) is **deferred to Sprint 31+1** as a separate
case-family-authoring sprint per the Sprint 20 / Sprint 29 corpus
fence.

The smoke confirms the slot **appears** on the four AMBIGUOUS-
intake cases in the 14-case set (cs_interactive_015 / 040 / 176 /
192 — see §5.4) — observable evidence the LLM saw the slot. None
of those four cases were Sprint 28 passes; the smoke does not
provide LLM-acted-on-the-slot evidence (and the §10 acceptance bar
explicitly does not require it — the LLM may or may not act on
the slot in the 14-case set).

## 7. Open questions for human

1. **cs_interactive_029 contract-violation regression — fix
   iteration or investigation?** The dev did not root-cause the
   specific causal chain. Candidate paths:
   - (a) **Fix iteration** with a second smoke rerun on a fully-
     warm bot to disambiguate cold-start race vs Sprint 31-code
     vs smoke variance. If the rerun shows cs_interactive_029
     back to 0.9667, the regression was a one-off (cold-start
     race); Sprint 31 closes cleanly. If the rerun still shows
     contract_violation, deeper investigation is warranted.
   - (b) **Investigation-only handoff** — Sprint 31 ships the
     code; the human + deliver-agent open a follow-on R-item
     (`R-cs029-contract-violation-after-sprint-31-restart`) to
     investigate without blocking Sprint 31 close.
   - (c) **Defer closure** — keep Sprint 31 open until
     cs_interactive_029 is back to its baseline.
   The dev recommends (a) — the cold-start race hypothesis is the
   most likely cause and the cheapest to disambiguate.
2. **System-prompt teaching paragraph length / placement
   sensitivity.** The added paragraph is ~6 sentences (~120
   words). It sits between the `already_called` teaching block
   and the DISCOVER phase guidance, expanding the system prompt
   by ~1% of total length. The smoke regression on
   mean_outcome_score (−16%) and mean_judge_score (−14%) is
   broader than a single case; whether this is sample noise on
   n=14 or a systematic LLM-behavior shift from the prompt edit
   cannot be disambiguated without a second smoke run. **Decision
   asked:** does the human want the dev to (a) tighten the
   teaching paragraph (e.g. 3 sentences instead of 6) or (b)
   accept the current 6-sentence shape and confirm via a second
   smoke?
3. **`iteration_governance.md` §7.2 fold-back triggering.** Per
   OQ3 pre-pick, Sprint 31 close does NOT trigger a special
   fold-back of the §7.2 worked example (the "RuntimeIntentClassifier
   already surfaces" clause becomes stale). Confirm the normal
   3–5-sprint governance cadence is still the right path, or pull
   the fold-back forward if Sprint 31 close is the right anchor.
4. **Case-family authoring sprint for the §7.2 worked example.**
   OQ4 pre-pick defers to Sprint 31+1. The Sprint 31 handoff
   surfaces the case family as a **recommended** next sprint but
   does not commit. **Decision asked:** does the deliver-agent
   want to make Sprint 31+1 the case-family-authoring sprint, or
   sequence something else first (e.g. cs_029 investigation)?
5. **R-item registration for `R-alternate-uc-signal-data-source`.**
   Sprint 31 ships the code; the disposition flip in
   `docs/action_bank.md` is the deliver-agent's responsibility.
   Recommended new state: `done — Sprint 31; per_turn_trace
   confirms the slot is present on 22/23 turns in
   eval_interactive/results/20260516-024934/results.json with
   ≥1 case (4 cases, in fact) carrying non-empty alternates`. The
   smoke regression is captured as a follow-on observation per
   §7.1 above, not a re-opening of the R-item.

## 8. Anti-hardcode self-walk (§4.1, nine questions)

Sprint 31 PR is semantic-touching (`prompt_projection` layer +
prompt edit). The §4.1 kernel applies in full (not exempt).

1. **Keyword / regex / if-else / enum / per-UC matrix added for a
   semantic decision?** No. The new field is a faithful capture of
   `RoutingResult.ambiguousCandidates()` — existing routing data the
   intake stage already produces. The new projection slot filters
   the snapshot by the current active UC (a 1-line `.equals()`
   check, no per-UC branching). The teaching paragraph is
   principled — no `search_knowledge` / `resolve_article` mentions,
   no `UC-A`...`UC-K` literals (verified by re-read; also indirectly
   verified by the `AlreadyCalledPromptConsumptionTest` 1400-char
   window check around `already_called`).
2. **Tier-0 invariant justification?** N/A (Q1 = no). Sprint 31
   adds no Tier-0 invariant. The Constitution's drift / topic-shift
   bullet (§1.3) governs the LLM's read decision; the projection
   surface sits inside the Runtime's "trace and eval contract"
   responsibility (§1.4). The new `BotSession.intakeAmbiguousCandidates`
   field is persistence state serving the projection, not a Tier-0
   invariant.
3. **Soft-signal projection alternative considered?** YES — the
   slot IS the soft signal. The runtime emits it; the LLM owns
   whether to act on it. The integration test
   `AgentRunLoopAlternateCandidateUseCasesNonEnforcementIntegrationTest`
   pins runtime non-enforcement at the loop layer.
4. **Visible-eval case text encoded into runtime / prompt / judge?**
   No. The Sprint 31 diff contains zero `cs_NNN` literals in runtime
   / prompt / judge code. The teaching paragraph names the slot
   by its projection-key (`alternate_candidate_use_cases`) and
   `active_use_case`; no CaseSpec ids, no trace-specific phrasing.
5. **Semantic ownership moved from LLM to Java?** No. The §1.3 LLM-
   owns scope (user goal / issue relation / use case hypothesis /
   drift / next action / escalation posture / response strategy /
   natural customer-facing wording) is unchanged. Sprint 31 only
   adds observable evidence the LLM may consider — no decision
   path branches on the slot in Java.
6. **Prompt grew an if-else block?** No. The teaching paragraph
   is principled prose (~6 sentences). Soft-signal posture
   (ownership cues "you own the judgement" + "does not block
   dispatch"); no UC-specific clauses; no tool-specific clauses;
   no eval-case-specific clauses.
7. **Tool schema, capability / permission, PII / safety floor,
   grounding floor preserved?** Yes. No tool schema edit. No
   permission boundary change. The new field stores UC ids (≤ 5
   characters each, no PII). The grounding diagnostics
   (`docs/current/faq_grounding_contract.md`) are not touched.
8. **Generalization eval coverage (target / neighbor / negative /
   shadow)?** Sprint 31 Java unit + integration tests cover the
   slot's runtime contract (10 tests; see §9). End-to-end LLM-
   behavior CaseSpec coverage is **deferred to Sprint 31+1** per
   OQ4 pre-pick. The Sprint 31 handoff §6 names the case family as
   the recommended next sprint.
9. **Temporary change → sunset plan?** Not temporary. The slot is
   the long-term `prompt_projection` Layer 1 for the §7.2 UC-A↔UC-C
   shape. If observed traces show the snapshot is insufficient on
   non-AMBIGUOUS-intake drift shapes (e.g. UC-A → UC-D mid-session
   shifts the intake router did not anticipate), a future sprint
   can layer Option γ-a on top per the Sprint 30 design doc §3.3 +
   §4 honest-acknowledgement note. Sprint 31 lands the cheaper
   option first; no sunset.

**Sprint 31 PR-level verdict (per §4.1):** **`approve`** —
Sprint 31 is a soft-signal projection bundle with regression
tests + a principled teaching paragraph. No semantic hardcode. The
smoke regression (§5.3, §5.5) is a sprint-close acceptance concern
on §13 success metrics and is handed off to the human +
deliver-agent for the §12 closure verdict; it does not
re-classify the PR's anti-hardcode disposition.

## 9. Files changed

### Sprint 31 dev-authored (the 7-row §6 file table)

| path | change type | line range / count |
|---|---|---|
| `server/src/main/java/com/gumtree/csagent/model/BotSession.java` | EDIT — NEW `@JdbcTypeCode(SqlTypes.ARRAY) @Column(name = "intake_ambiguous_candidates", columnDefinition = "text[]") private String[] intakeAmbiguousCandidates;` field adjacent to lines 52–54 (`candidateUseCases`) with Javadoc | +16 lines |
| `server/src/main/resources/db/migration/V14__intake_ambiguous_candidates.sql` | NEW — `ALTER TABLE bot_sessions ADD COLUMN IF NOT EXISTS intake_ambiguous_candidates text[];` | 14-line file |
| `server/src/main/java/com/gumtree/csagent/service/runtime/SessionManager.java` | EDIT — AMBIGUOUS branch (case `AMBIGUOUS ->`) inserts the snapshot assignment BEFORE the existing `setCurrentPhase("DISCOVER")` call | +12 lines |
| `server/src/main/java/com/gumtree/csagent/service/runtime/SessionManager.java` (ROUTED branch) | NO CHANGE per OQ1 pre-pick (Lombok default null preserved) | 0 lines |
| `server/src/main/java/com/gumtree/csagent/service/runtime/ContextProjectionBuilder.java` | EDIT — new `alternate_candidate_use_cases` ArrayNode emission adjacent to lines 380–394 (`candidate_use_cases`); active UC filtered out; schema-stable empty-array semantics | +22 lines |
| `server/src/main/resources/prompts/system_prompt.txt` | EDIT — NEW teaching paragraph inserted IMMEDIATELY after the existing `already_called` block (lines 23–28), sibling placement per OQ2 pre-pick | +7 net lines (the pre-existing 1-line unrelated working-tree mod on line 60 is NOT part of Sprint 31's stage; see §10) |
| `server/src/test/java/com/gumtree/csagent/service/runtime/IntakeAmbiguousCandidatesProjectionTest.java` | NEW — 9 unit tests (5 SessionManager-side; 4 ContextProjectionBuilder-side); mirror Sprint 20 `AlreadyCalledProjectionTest` shape | 287-line file, 9 `@Test` methods |
| `server/src/test/java/com/gumtree/csagent/integration/AgentRunLoopAlternateCandidateUseCasesNonEnforcementIntegrationTest.java` | NEW — 1 integration test driving `AgentRunLoopImpl.run` end-to-end against a session with populated `intakeAmbiguousCandidates`; mirrors Sprint 20 `AgentRunLoopAlreadyCalledNonEnforcementIntegrationTest` shape | 158-line file, 1 `@Test` method |

`git diff --stat HEAD -- server/src/main/` (Sprint 31 dev's main-
side mods):

```
 .../java/com/gumtree/csagent/model/BotSession.java | +16
 .../service/runtime/ContextProjectionBuilder.java  | +22
 .../csagent/service/runtime/SessionManager.java    | +12
 .../src/main/resources/prompts/system_prompt.txt   | +8 -1
 4 files changed, 57 insertions(+), 1 deletion(-)
```

The `-1` on `system_prompt.txt` is **NOT** Sprint 31's. It is the
inherited working-tree mod on line 60 (`ACTIVE-UC TIEBREAKER`
parenthetical removal). Sprint 31's `system_prompt.txt` stage uses
`git add -p` to include only the Sprint 31 teaching-paragraph
hunk; the pre-existing 1-line mod stays unstaged per dev prompt
§10.

### Deliver-agent-owned (NOT staged by Sprint 31 dev)

Per dev prompt §10, the following uncommitted files at session
start are deliver-agent-owned and are not part of the Sprint 31
dev commit:

- `docs/sprint_objective.md` (the Sprint 31 contract; archive
  at close)
- `docs/10-handoff.md` (§1 lead refresh)
- `compact/sprint-031-dev-prompt.md` + `compact/sprint-031-review-prompt.md`
- `docs/sprints/sprint-030-objective.md` (Sprint 30 archive)
- `docs/sprints/sprint-030-handoff.md` (Sprint 30 dev handoff with
  §12 closure verdict filled by deliver-agent at Sprint 30 close)
- `csagent_system_design_review.md` (unrelated pre-existing mod;
  not Sprint 31's)

The deliver agent bundles these on Sprint 31 close per the
commit-at-end pattern.

## 10. Layer-classification self-walk (per §3)

§3.2 first-match-wins walk against the Sprint 31 deliverable:

| § | question | answer |
|---|---|---|
| Q1 | Infra failure (orchestration / transport / persistence / timeout / OOM)? | No. Sprint 31 adds an additive field + projection slot + prompt teaching; no infra surface failed. |
| Q2 | Tier-0 invariant being broken? | No. The Constitution's drift / topic-shift bullet (§1.3 LLM owns) governs the behaviour; the projection sits inside Runtime's §1.4 "trace and eval contract" responsibility. No new Tier-0. |
| Q3 | LLM chose validly within available options but projection / context was impoverished (missing slot / candidate / diagnostic)? | **YES.** The LLM today cannot see the intake router's considered-alternate UCs because the AMBIGUOUS branch discarded them at intake; Sprint 31 preserves the data, projects it, and teaches the LLM to read it. First-match → `prompt_projection`. **Confirmed.** |

Tail-rule check: smoke regression is not a same-prompt-different-
rerun judge flip (the regression is mean_composite + mean_outcome
+ mean_judge widening across many cases, not a single CaseSpec
that flipped); the regression is not a `judge_calibration` issue.

§3.3 "no Java guard by default" check: PASS. Sprint 31 added zero
Java guards. The runtime does NOT branch on the slot's value
(verified by the non-enforcement integration test).

**Final classification:** `prompt_projection`. Matches the
Sprint 31 §9 prospective stanza in `docs/sprint_objective.md`.

### 10.1 §7 stanza filled in (verbatim, with counts from Sprint 31)

```markdown
## Layer-classification + anti-hardcode stanza

**Target failure layer:** prompt_projection

**Tier-0 invariant:** This sprint adds no Tier-0 invariant. The
Constitution's drift / topic-shift bullet (§1.3 LLM owns) governs
the behaviour the projection enables; the projection itself sits
inside the Runtime's "trace and eval contract" responsibility
(§1.4). The new `BotSession.intakeAmbiguousCandidates` field is
persistence state serving the projection, not a Tier-0 invariant.

**Semantic hardcode:** No semantic hardcode introduced. The new
`alternate_candidate_use_cases` projected slot is a soft signal —
a faithful capture of the `UseCaseRouter`'s existing intake-time
ambiguity (`RoutingResult.ambiguousCandidates`), preserved across
the session and exposed to the LLM through the per-turn
projection. The LLM owns whether to act on it. No new keyword,
regex, per-UC matrix, or if-else is added to
`RuntimeIntentClassifier`, `DriftDetector`, `UseCaseRouter`,
`ControlKernel`, or any prompt branch. The single prompt edit is
one short teaching paragraph naming the slot and its provenance,
in the shape of the Sprint 20 / Sprint 23 `already_called`
teaching paragraph.

**Generalization coverage:** target / neighbor / negative / shadow
counts: 0 / 0 / 0 / 0 — CaseSpec authoring for the UC-A ↔ UC-C
§7.2 worked-example family is **deferred to Sprint 31+1** per
OQ4 pre-pick. Sprint 31's runtime coverage is Java-only: 9 unit
tests + 1 integration test (10 total) pinning AMBIGUOUS-captures
/ ROUTED-null / OUT_OF_SCOPE-null / projection-emits-minus-active
/ schema-stability / runtime-non-enforcement. End-to-end LLM
behavior validation against the §7.2 worked example is the
follow-on case-family sprint's surface.
```

## 11. §5 Eval Acceptance bars

Walked line-by-line per `docs/current/iteration_governance.md` §5.1.

| bar | result | evidence |
|---|---|---|
| Target cases pass | **PARTIAL.** The §10 acceptance bar requires (a) no regression on `composite_score` / `pass-rate` / safety / grounding / wrong-containment / over-escalation vs Sprint 28 reference, AND (b) the new slot appears in `case_results[].per_turn_trace[].projection` on cases that hit the AMBIGUOUS intake branch with ≥1 such case identified. **(b) PASS:** 4 cases (cs_interactive_015 / 040 / 176 / 192) carry non-empty alternates per §5.4. **(a) FAIL:** mean_composite 0.1299 → 0.0617 (−52%), mean_outcome 0.7302 → 0.6141 (−16%), mean_judge 0.6667 → 0.5714 (−14%); 1 previously-stable case (cs_interactive_029) flipped to contract_violation. Sprint 31 dev prompt §9.3 stop condition triggered. See §5.3 + §5.5. |
| Neighbor cases no regression | **FAIL.** Same data as the previous row. The two Sprint 28 passes (cs_interactive_014, cs_interactive_029) both fail in Sprint 31; cs_interactive_036 newly passes. Net: −1 pass. |
| Negative-control cases unchanged | **PARTIAL.** The negative-control posture for Sprint 31 is the OQ1 pre-pick (ROUTED / OUT_OF_SCOPE leaves the field null; projection emits empty array). Verified by the unit tests `routedIntake_leavesIntakeAmbiguousCandidatesNull` + `outOfScopeIntake_leavesIntakeAmbiguousCandidatesNull` + `projection_emitsEmptyArray_whenSnapshotIsNull` (all PASS). Smoke-level negative-control evidence: every non-AMBIGUOUS-intake case in the 14-case set emits an empty `alternate_candidate_use_cases` array (negative result, no false positive). |
| Shadow cases no regression | **N/A — deferred.** Sprint 31's Java-test coverage does not consume shadow cases; shadow coverage is the Sprint 31+1 case-family-authoring sprint's surface per OQ4 pre-pick. |
| Safety floor unchanged | **N/A on Sprint 31 surface.** No PII / safety / identity-verification / imminent-harm surface edited. The new field stores UC ids (≤ 5 characters each), no PII. The judge's safety floor categories are unchanged. (Note: the smoke's mean_outcome regression includes outcome-score weighting, but a per-category safety breakdown is not in the summary block; the dev did not extract it.) |
| Grounding floor unchanged | **N/A on Sprint 31 surface.** No edit to `docs/current/faq_grounding_contract.md`, no edit to FAQ corpus, no edit to grounding diagnostics. |
| Wrong-containment rate unchanged or down | **PARTIAL / not directly measured.** Sprint 31 does not regress the *intent* of wrong-containment (the slot is observability-only). The smoke's mean_outcome regression includes containment as one input but does not break out per-category numbers in the summary. |
| Over-escalation rate unchanged or down | **PARTIAL / not directly measured.** Same as the previous row. cs_interactive_029's contract_violation shape has no escalation_reason; the case did not over-escalate. cs_interactive_014's failure shape is `goal_impossible` (turn-budget / max-step), not over-escalation. |
| Architecture-health metrics not regressed | **N/A.** Sprint 31 adds **no** new semantic hardcode (`new_semantic_hardcode_count` += 0), does **not** downgrade an existing hardcode (`soft_signal_conversion_count` += 0 on the runtime side, though arguably the slot itself is a *new* soft signal — but the §6 metric definition is downgrade-of-existing-hardcode), `planner_ownership_ratio` is unchanged (no decision moved out of the LLM), `shadow_disagreement_rate` is not collected (no shadow set consumed). All four metrics remain `collection_status: not_started` per `iteration_governance.md` §6. |

### 11.1 Java test suite (`mvn -q -pl server test`)

Full run at 2026-05-16 (with Sprint 31's code applied):

- **Total: 912 tests run** (Sprint 28 reference: 902, per
  `docs/sprints/sprint-028-handoff.md` §11; +10 tests = the
  Sprint 31 9 unit + 1 integration).
- **Failures: 1.** Only the inherited
  `SystemPromptUserRequestedTiebreakerTest.systemPrompt_marksActiveUcTiebreakerExplicitly`
  (line 53: "ACTIVE-UC TIEBREAKER must be tagged with the Sprint
  6 anchor"). This is the same inherited failure documented in
  Sprint 24 / 25 / 26 / 27 / 28 handoffs, caused by the
  unauthored working-tree mod on `system_prompt.txt:60` removing
  `(Sprint 6 §G1)`. Sprint 31's own `system_prompt.txt` edit is
  upstream of that line and does not perturb the inherited
  failure.
- **Errors: 0.**
- **Skipped: 2.** (Sprint 28 reference: 2 skips per its handoff
  §11; unchanged.)
- **Zero new regressions** from Sprint 31's code: all 901 Sprint
  28-era tests still pass; the single inherited failure is the
  same byte-for-byte as the Sprint 28 baseline.

Per-test counts for Sprint 31's new tests (extracted from
`server/target/surefire-reports/`):

| test file | tests run | failures | errors | skipped | time |
|---|---:|---:|---:|---:|---:|
| `IntakeAmbiguousCandidatesProjectionTest` | 9 | 0 | 0 | 0 | 0.240 s |
| `AgentRunLoopAlternateCandidateUseCasesNonEnforcementIntegrationTest` | 1 | 0 | 0 | 0 | 0.617 s |
| **total Sprint 31** | **10** | **0** | **0** | **0** | — |

Recipe:

```bash
mvn -q -pl server test
cat server/target/surefire-reports/com.gumtree.csagent.service.runtime.IntakeAmbiguousCandidatesProjectionTest.txt
cat server/target/surefire-reports/com.gumtree.csagent.integration.AgentRunLoopAlternateCandidateUseCasesNonEnforcementIntegrationTest.txt
```

### 11.2 eval_interactive pytest suite (`cd eval_interactive && uv run pytest`)

Full run at 2026-05-16:

- **Total: 309 tests run** (Sprint 28 reference: 309 per its §11).
- **Passed: 302.**
- **Failed: 7.** Sprint 28 reference had **3** inherited failures
  (per its §11.2: `test_case_spec_overrides.py × 2 +
  test_corpus_lint.py × 1`). Sprint 31 sees **4 additional
  failures** on `test_corpus_lint.py` (`test_regenerated_corpus_bucket_lints_clean[anchor]`,
  `[promotion]`, `[exploration]`, `[smoke]` +
  `test_full_corpus_lints_clean_with_smoke_subset_flag`).
- **Error shape:** every failure is a Python subprocess
  `ModuleNotFoundError: No module named 'eval_interactive.case_spec'`
  invoked under `/Users/caoruixin/miniconda3/bin/python` with a
  working directory reference to a sibling repo (`csagent-design-v1-without-human-review-dataset/eval_interactive/`).
  This is **environment-dependent** (uv vs miniconda subprocess
  path resolution); it is **not caused by Sprint 31 code** because
  Sprint 31 touches **no files under `eval_interactive/`** — `git
  diff --stat HEAD -- eval_interactive/` is empty.
- The 4 additional failures vs Sprint 28's baseline are
  inherited-environment-shift, not Sprint 31 code regressions; the
  honest disposition is "inherited environment-dependent failures
  on test_corpus_lint subprocess path resolution; Sprint 31 did
  not perturb."

### 11.3 Smoke acceptance check

See §5 for the full table. **The smoke acceptance bar FAILS the
§10 'no regression on composite_score / pass-rate / safety /
grounding / wrong-containment / over-escalation floors vs Sprint
28' check.** Dev STOPPED per §9.3 rather than working around. The
slot-presence check on AMBIGUOUS-intake cases PASSES (4 cases
carry non-empty alternates per §5.4).

## 12. Closure verdict placeholder (deliver-agent owned)

| field | value |
|---|---|
| status | **TBD by deliver-agent** — see §7.1 for the three candidate dispositions (fix iteration with rerun / investigation-only handoff / defer closure). The dev's recommendation is **(a) fix iteration with a second smoke rerun on a warm bot to disambiguate cs_interactive_029's regression**; the cold-start race hypothesis is the cheapest to disambiguate. |
| classification | TBD by deliver-agent. |
| Codex outcome | Pending — Sprint 31 is semantic-touching (`prompt_projection` + prompt edit); Codex review is REQUIRED per `docs/sprint_objective.md` §10 (not exempt). |
| R-item disposition | `R-alternate-uc-signal-data-source` — proposed transition **to `done (Sprint 31)`** pending closure verdict; the disposition flip is deliver-agent-owned. The follow-on `R-cs029-contract-violation-after-sprint-31-restart` (or equivalent name) is proposed in §7.1 if the deliver-agent picks the investigation-only path. |
| date | TBD by deliver-agent at sprint close. |

Per `feedback_handoff_verdict_section_delegation.md`: the dev does
NOT fill the closure verdict; the human + deliver-agent own it.
The dev hands off with the §11 evidence and the §7 open questions
laid out.

---

**Sprint 31 dev-agent handoff complete.** Code lands; tests pass
(912 / 1-inherited / 0 / 2; 10 new Sprint 31 tests PASS); the new
slot is verifiably populated on AMBIGUOUS-intake cases in the
live smoke. The §10 smoke acceptance bar FAILS on
composite_score / outcome / judge floors with one previously-
stable case (cs_interactive_029) flipping to contract_violation
on the just-restarted bot; the dev STOPPED per §9.3 rather than
silently working around, and surfaces the regression honestly
with full reproducibility for the human + deliver-agent to
decide closure.
