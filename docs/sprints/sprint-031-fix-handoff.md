---
title: Sprint 31 fix-iteration #2 handoff
doc_tier: sprint-archive
status: current
implementation_status: implemented
source_of_truth: this file (plus the strengthened T8 test file cited under §6)
last_reviewed: 2026-05-16
review_cadence: per sprint
supersedes: []
superseded_by: null
notes: >
  Narrow fix-iteration #2 on branch refactor/remove-the-shackles,
  closing Codex Finding 2 of the Sprint 31 close review (T8
  non-enforcement test too weak). Finding 1 (smoke regression) was
  classified out_of_scope by the deliver-agent + human per the §13
  fix-iteration #1 evidence on docs/sprints/sprint-031-handoff.md and
  is carried by R-llm-provider-latency-drift-2026-05-16; this
  iteration does NOT touch it. One test file strengthened from a
  single happy-path scenario to six parameterised slot-value variants
  with five invariance bars per variant. No production code edited.
---

# Sprint 31 fix-iteration #2 handoff

Date: 2026-05-16
Parent sprint: 31 (objective at `docs/sprints/sprint-031-objective.md`)
Codex findings closed (this iteration): 1 of 2 (Finding 2)
Codex findings classified out_of_scope: 1 of 2 (Finding 1)
Branch: `refactor/remove-the-shackles`
Iteration index: #2 (iteration #1 was the §13 disambiguation walk at
commit `de47635`; this iteration adds the third commit on top of
`2c1fd41` + `de47635`)

## 1. Context

Sprint 31 closed on commit `8908775` with Codex returning
`fix_required / blocking_count: 2` (`docs/codex-findings.md` lines
1–56). The deliver-agent + human classified the two findings:

- **Finding 1 (smoke acceptance floor regression vs Sprint 28
  reference, `docs/codex-findings.md` line 15)** → **out_of_scope**.
  Sprint 31 §13 (fix-iteration #1) ran three back-to-back smoke
  reruns with falsifiable disambiguation between three hypotheses;
  H1 (cold-start race) and H2 (system_prompt teaching paragraph)
  were both REJECTED, and H3 (external LLM provider latency drift)
  was the surviving candidate with strong corroborating signal (mean
  `elapsed_ms` widening ~+84% across reruns, three-run latency
  ladder, smoke-shape preserved across reruns). The new R-item
  `R-llm-provider-latency-drift-2026-05-16` carries the investigation
  forward. This iteration does NOT touch the latency / smoke /
  provider surface.

- **Finding 2 (T8 non-enforcement test too weak,
  `docs/codex-findings.md` line 17)** → **fix_required**, scope of
  this iteration. The original
  `AgentRunLoopAlternateCandidateUseCasesNonEnforcementIntegrationTest`
  exercised exactly one populated-slot scenario
  (`["UC-A", "UC-C"]`), asserted only that the loop reached
  `FINAL_ANSWER`, and did not compare populated vs empty / unrelated
  / different-contents inputs. A hypothetical future Java branch
  such as `if alternate_candidate_use_cases contains "UC-C" then
  short-circuit X` could evade this test if it did not affect this
  exact no-tool-call happy path. Strengthening the test to a
  parameterised suite covering six slot-value variants with five
  invariance bars per variant closes the gap.

§4.1 failing question: Q8 (generalization coverage). §3.1 layer:
infra (test coverage), no production change.

## 2. Premise re-verification

Verified at session start (2026-05-16, HEAD on
`refactor/remove-the-shackles` = `de47635`):

- File `server/src/test/java/com/gumtree/csagent/integration/AgentRunLoopAlternateCandidateUseCasesNonEnforcementIntegrationTest.java`
  exists; pre-fix line count = 173; contains exactly one `@Test`
  method (`populatedAlternateSlot_appearsInProjection_andRuntimeDoesNotShortCircuit`).
  Confirmed via `wc -l`.
- File
  `server/src/test/java/com/gumtree/csagent/integration/AgentRunLoopAlreadyCalledNonEnforcementIntegrationTest.java`
  exists and uses a single `@Test` per scenario shape (no parameterised
  structure; the Sprint 20 precedent collapses three LLM responses into
  one test rather than parameterising). Mirrored as the structural
  precedent for setup + capture; this iteration's strengthening goes
  beyond the Sprint 20 shape by adding parameterisation, which is the
  appropriate tool for the invariance-across-inputs property under
  test.
- `grep -rn "intakeAmbiguousCandidates\|IntakeAmbiguousCandidates"
  server/src/main/java/` returns exactly three sites:
  - `model/BotSession.java:70` — `private String[]
    intakeAmbiguousCandidates;` (field declaration).
  - `service/runtime/SessionManager.java:194` — the AMBIGUOUS-branch
    capture (`session.setIntakeAmbiguousCandidates(...)`).
  - `service/runtime/ContextProjectionBuilder.java:409–410` — the
    projection slot emission (read + filter loop).

  No Java decision path consumes the field. The non-enforcement
  guarantee Sprint 31 closed on holds at the time of this iteration;
  no stop-condition fired.

## 3. Implementation walkthrough

Single test file rewritten. No production code edited.

### 3.1 Structure choice — parameterised, single method

Per the dev prompt's §3.1 "Preferred" option, the existing single
`@Test` method was rolled into a `@ParameterizedTest` +
`@MethodSource("slotVariants")` method named
`runtimeBehaviorIsInvariantToSlotValue`. The static
`slotVariants()` provider returns six `Arguments.of(label, input,
expected)` rows; each row drives one parameterised invocation. The
existing `@BeforeEach setUp()` block carries through largely
unchanged — the LLM-mock stubbing (single canned response, no tool
calls, canonical final-answer text) moves into `setUp()` so it is
shared across all six variants, and only the variant-specific
`BotSession` field + variant label change per case. The original
happy-path scenario (`["UC-A", "UC-C"]`) is captured as variant
`V5_multi_element_active_plus_alternates` (with `UC-D` added so the
multi-element nature is observable in the expected output); the
table below lists all six.

Why parameterise rather than copy the `@Test` six times: the
invariance bars (`TerminalOutcome`, `invokeChat` count, `verifyNoInteractions(toolDispatcher)`,
final user-message text) must be identical *across* variants for
the non-enforcement property to hold. A future regression that
breaks invariance on one variant only — exactly the failure mode
Codex Finding 2 flagged — is most legibly detected when the same
test body runs against each variant and a single variant fails.
Six separate `@Test` methods would still detect a regression but
would scatter the failure across the suite output; parameterisation
makes the variant label the failure key.

### 3.2 The six slot-value variants

| variant | `intakeAmbiguousCandidates` input | expected projection slot |
|---------|------------------------------------|--------------------------|
| **V1_null_ROUTED_style** | `null` (Lombok default) | `[]` |
| **V2_empty_array** | `new String[0]` | `[]` |
| **V3_single_element_matching_active** | `{"UC-A"}` (active = UC-A) | `[]` (active filtered) |
| **V4_single_alternate** | `{"UC-C"}` | `["UC-C"]` |
| **V5_multi_element_active_plus_alternates** | `{"UC-A","UC-C","UC-D"}` | `["UC-C","UC-D"]` |
| **V6_unrelated_ucs_only** | `{"UC-F","UC-G"}` | `["UC-F","UC-G"]` |

V1–V3 produce an empty projection slot. V4–V6 produce a non-empty
slot. The projection contract under test (`ContextProjectionBuilder.java:407–416`)
drops null / blank entries and any entry equal to the active UC; V3
exercises the active-UC filter, V6 exercises that unrelated UCs
pass through unfiltered (the projection slot does not consult the
use-case registry for alternates). The active UC is the same
(`UC-A`) on every variant so the only varying input is the slot's
contents.

### 3.3 The five invariance assertions

Each parameterised invocation asserts the following on its result:

1. **Projection slot value** matches the expected per-variant list
   (table above). Captured via `ArgumentCaptor<String>` on
   `llmInvocation.invokeChat(...)`; the JSON is parsed with the same
   `ObjectMapper` the loop builds with, then the
   `alternate_candidate_use_cases` array is read into a
   `List<String>` and compared with `assertEquals`. The slot is
   asserted to always be present (shape stability, §N0 nullable-field
   convention) regardless of whether it is empty or populated.
2. **TerminalOutcome** is `FINAL_ANSWER` on every variant — the slot
   does NOT redirect the loop to `ESCALATE` or terminate it early.
3. **`llmInvocation.invokeChat` call count** is exactly 1 on every
   variant. The captor block uses `verify(llmInvocation, times(1))`,
   which would fail if the slot caused a short-circuit before the
   LLM call (count = 0) or a re-prompt (count > 1). Same canned LLM
   response is stubbed regardless of the projection contents.
4. **`toolDispatcher` is never invoked** on any variant.
   `verifyNoInteractions(toolDispatcher)` catches a hypothetical
   slot-gated dispatch (e.g. "if slot contains UC-C, force-dispatch
   classify_use_case") that would fire on V4 / V5 but not V1–V3.
5. **Final user-message text** is the canonical constant
   `Here is what I found about your advert visibility.` on every
   variant. A slot-driven rewrite of the final answer would diverge
   here even if outcome + counts stayed identical.

The five bars are independent: any single Java branch on the slot's
value that produces a different runtime path between V1–V3 and
V4–V6 (or amongst V4 / V5 / V6 with different contents) trips at
least one bar on at least one variant, and the parameterised
runner names the failing variant in the surefire output.

### 3.4 What did NOT change

Per the dev prompt's §3.4 scope fence:

- No file under `server/src/main/java/` edited.
- No other test file edited.
- `system_prompt.txt`, `ContextProjectionBuilder.java`,
  `SessionManager.java`, `BotSession.java`, `AgentRunLoopImpl.java`
  — all untouched.
- No CaseSpec authored.
- No edit to `docs/action_bank.md`, `docs/proposals/`, or
  `docs/sprints/sprint-031-handoff.md` (the §13 fix-iteration
  evidence stays as-is; the §12 closure verdict remains pending the
  deliver-agent + human at fix-iteration #2 close).

## 4. Verification

### 4.1 Targeted test run

Command:

```
mvn -pl server -Dtest=AgentRunLoopAlternateCandidateUseCasesNonEnforcementIntegrationTest test
```

Tail of stdout (filtered to surefire summary lines):

```
[INFO] Tests run: 6, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.652 s -- in com.gumtree.csagent.integration.AgentRunLoopAlternateCandidateUseCasesNonEnforcementIntegrationTest
[INFO] Tests run: 6, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

All six parameterised variants PASS in a single run. Time-elapsed
on the parameterised method is 0.652 s, comparable to the pre-fix
single-scenario test (~0.6 s in the original Sprint 31 close run).
The six rows in the `slotVariants()` provider correspond 1:1 with
the six `Tests run` count.

### 4.2 Full server suite run

Command:

```
mvn -pl server test
```

Tail of stdout (filtered to summary + the inherited failure):

```
[ERROR] Failures: 
[ERROR]   SystemPromptUserRequestedTiebreakerTest.systemPrompt_marksActiveUcTiebreakerExplicitly:53 ACTIVE-UC TIEBREAKER must be tagged with the Sprint 6 anchor ==> expected: <true> but was: <false>
[ERROR] Tests run: 917, Failures: 1, Errors: 0, Skipped: 2
[INFO] BUILD FAILURE
```

Full suite: **917 tests run, 1 failure, 0 errors, 2 skipped**. The
single failure is the inherited
`SystemPromptUserRequestedTiebreakerTest.systemPrompt_marksActiveUcTiebreakerExplicitly`
baseline carried since Sprint 24+; Sprint 31 main close also ran
against this same inherited failure (`docs/sprints/sprint-031-handoff.md`
§11.1 acceptance run). No new regression introduced by this
iteration.

Suite-count delta vs the Sprint 31 close baseline (`912 / 1 / 0 / 2`
per the dev prompt §4): `+5` tests. Explanation: the original
`AgentRunLoopAlternateCandidateUseCasesNonEnforcementIntegrationTest`
contributed 1 test (its single happy-path `@Test`); the strengthened
version contributes 6 (the parameterised method runs once per
`Arguments.of` row). Net delta `+6 − 1 = +5` matches `917 − 912 = 5`.
No other test was added or removed.

### 4.3 §5 Eval Acceptance bars

This is a fix-iteration on a test-only change; no eval harness was
re-run as part of this iteration (would not change the Sprint 31
close eval evidence). The Sprint 31 close §11.1 / §11.2 / §11.3
acceptance bars stand on the close run; the eval-side regression
hypothesis remains the out-of-scope Finding 1 carried by
`R-llm-provider-latency-drift-2026-05-16`. Test-suite preservation
on the inherited baseline (§4.2 above) is the only acceptance bar
this iteration was required to clear.

## 5. Anti-hardcode self-walk

Walking the Anti-Hardcode Kernel (`docs/codex-findings.md` lines
19–30) against this iteration:

- **Q1 keyword / regex / if-else / enum / per-UC matrix:** No. The
  iteration adds parameterised test scenarios; no decision matrix
  introduced anywhere.
- **Q2 Tier-0 justification:** N/A. No Tier-0 invariant added,
  `docs/runtime_freeze_and_risk_policy.md` untouched.
- **Q3 soft signal achievable:** Yes (already the landed design).
  This iteration tests the soft-signal property; it does not
  introduce or replace a hard signal.
- **Q4 eval text / CaseSpec encoding:** No. The test contains no
  `cs_NNN` reference and no trace-specific text. The canonical
  final-answer constant is generic FAQ-shaped boilerplate inherited
  from the pre-fix file (`Here is what I found about your advert
  visibility.`), not eval text.
- **Q5 semantic ownership shift:** No. The test does not move any
  decision from the LLM to Java; it pins the *absence* of such a
  decision in the runtime by asserting invariance across slot
  values.
- **Q6 prompt if-else dump:** No prompt edited.
- **Q7 tool / capability / PII / grounding floor:** Preserved. No
  tool schema, capability, PII, or grounding-floor change.
- **Q8 generalization coverage:** Strengthened. The original test
  exercised one populated-slot scenario; the strengthened version
  exercises six variants covering null, empty, active-only,
  single-alternate, multi-alternate, and unrelated-only inputs. The
  five invariance bars per variant cover the four runtime decision
  surfaces Codex Finding 2 named as missing (dispatch, phase
  transition, outcome, user-facing message). Shadow CaseSpec
  authoring remains the Sprint 31+1 deferral per OQ4
  (`docs/sprints/sprint-031-handoff.md:679`); it is not in scope
  here.
- **Q9 rollback / sunset:** N/A — this is a test-coverage
  strengthening, not a temporary measure.

**Mental check** (per dev prompt §4 "would your test catch a
hypothetical Java branch?"): imagine someone added
`if (session.getIntakeAmbiguousCandidates() != null
&& Arrays.asList(session.getIntakeAmbiguousCandidates()).contains("UC-C"))
{ /* return ESCALATE with reason="ambiguous" */ }` inside
`AgentRunLoopImpl.run`. Predicted variant behaviour:

- V1 (null) — branch condition false; outcome `FINAL_ANSWER`,
  `invokeChat=1`, `toolDispatcher` not invoked, message canonical.
- V4 (`{"UC-C"}`) — branch condition TRUE; outcome `ESCALATE` and
  `invokeChat=0` (short-circuit before LLM call) or different
  final-message text.

V1 and V4 would disagree on invariance bars 2 + 3 + 5
simultaneously; the test fails on at least three of the five bars
on V4 (and likely also V5, which contains UC-C). Symmetric mental
check against a slot-gated tool dispatch (e.g. "if slot contains
UC-F, force-dispatch classify_use_case"): trips bar 4
(`verifyNoInteractions`) on V6 and bar 3 (count = 2) at the same
time. Both forms of regression are caught.

PR-level hardcode verdict for this iteration: **approve** under
§4.1's per-PR rubric — pure test-coverage strengthening that
extends generalisation coverage on an existing soft-signal
projection slot, no decision-path edit anywhere.

## 6. Files changed

Exactly one file:

- `server/src/test/java/com/gumtree/csagent/integration/AgentRunLoopAlternateCandidateUseCasesNonEnforcementIntegrationTest.java`
  — strengthened from 1 happy-path `@Test` (pre-fix 173 lines) to a
  6-variant `@ParameterizedTest` + `@MethodSource` covering V1–V6
  with five invariance bars per variant.

No production source under `server/src/main/java/` edited. No
other test file edited. No edit to
`docs/sprints/sprint-031-handoff.md` (the §13 fix-iteration #1
evidence stays as-is). The deliver-agent will bundle this iteration
into `docs/sprints/sprint-031-handoff.md` §12 on close.

Working-tree files that exist but are NOT staged by this iteration
(deliver-agent owned or pre-existing): the mock-data /
mock-service surface mods, `csagent_system_design_review.md`,
`docs/codex-findings.md`, `docs/sprint_objective.md`,
`docs/sprints/sprint-030-handoff.md`,
`docs/sprints/sprint-030-objective.md`,
`docs/sprints/sprint-031-objective.md`,
`docs/sprints/sprint-031-handoff.md`,
`docs/sprints/sprint-031-codex-review.md`,
`docs/mock-test-data-guide.md`,
`server/src/main/resources/prompts/system_prompt.txt`, and the
`compact/sprint-031-fix-*-prompt.md` files. Per the dev prompt §7,
these are bundled by the human at commit time; this iteration's
commit stages only the strengthened test file and this handoff.

## 7. Open questions for human

None. The five invariance bars on six variants close the
generalisation-coverage gap Codex Finding 2 named; no new R-item
required for this iteration. The standing R-items (carried from
Sprint 31 main close +`docs/action_bank.md`) are unchanged:

- `R-llm-provider-latency-drift-2026-05-16` — carries the
  out-of-scope Finding 1 investigation; not touched here.
- `R-alternate-uc-signal-shadow-caseset` — Sprint 31+1 shadow
  CaseSpec authoring; not touched here.
- `R-uc-cdf-get-customer-context-bot-actual-usage` — pre-Sprint-31
  open item; unchanged.

## 8. Closure verdict

Pending deliver-agent + human at fix-iteration #2 close. Suggested
shape (to be filled by deliver-agent at sprint-close, mirroring the
`docs/sprints/sprint-031-handoff.md` §12 header convention):

```
decision: pass | fix_required | out_of_scope_review
blocking_count: <number>
summary: <one paragraph>
```

Recommended Codex re-review scope on this iteration: the single
strengthened test file (§6) + this handoff. Finding 1 closure
remains an open question for the broader (post-)Sprint 31 process
and is carried by `R-llm-provider-latency-drift-2026-05-16`.
