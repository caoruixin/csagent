---
title: Sprint 20 fix-iteration handoff
doc_tier: sprint-archive
status: current
implementation_status: implemented
source_of_truth: this file (plus the new test files cited under Finding 1)
last_reviewed: 2026-05-13
review_cadence: per sprint
supersedes: []
superseded_by: null
notes: >
  Narrow fix iteration on the same branch design-v1-without-human-review,
  closing the two blocking Codex findings on Sprint 20. Adds test
  evidence at the AgentRunLoopImpl.run granularity for Track B
  non-enforcement + cs_011 T2 target coverage, and reconciles
  _ACCESS_BOUNDARY.md with the v0 shadow-split mechanism that actually
  shipped. No production code edited; no new R-items opened.
---

# Sprint 20 fix-iteration handoff

Date: 2026-05-13
Parent sprint: 20 (objective at `docs/sprint_objective.md` "Sprint 20 fix
iteration" section, lines 375–618)
Codex findings closed: 2
Branch: `design-v1-without-human-review`

## Finding 1 — runtime non-enforcement + cs_011 T2 target coverage

**Codex reference:** `docs/codex-findings.md` lines 6–54 (Finding 1).
**§4.1 failing question:** Q8. **§3.1 layer:** `prompt_projection`.

### Diff summary

Two new test files; no production code edited.

- `server/src/test/java/com/gumtree/csagent/integration/AgentRunLoopAlreadyCalledNonEnforcementIntegrationTest.java`
  (Test A, integration shape).
- `server/src/test/java/com/gumtree/csagent/service/runtime/AlreadyCalledCs011T2ShapeTest.java`
  (Test B, unit shape).

### Test evidence

**Test A — `AgentRunLoopAlreadyCalledNonEnforcementIntegrationTest`.**
Drives the real `AgentRunLoopImpl.run(...)` end-to-end with a real
`ContextProjectionBuilder` (collaborator services mocked so the loop is
deterministic). Three canned LLM responses (search_knowledge tool call,
identical search_knowledge tool call, no-tool-call final answer). The
test asserts:

- `verify(toolDispatcher, times(2)).dispatch(eq("search_knowledge"),
  any(), any())` — both identical-args calls reach the dispatcher; the
  runtime does **not** short-circuit on the second emission. This is
  the parent objective bar at `docs/sprint_objective.md` lines 160–161
  ("the runtime does **not** short-circuit on the slot being populated
  ... identical-args re-emission is still dispatched unless the LLM
  itself avoids it"), demonstrated at the `AgentRunLoopImpl.run`
  granularity Codex Finding 1 required (lines 51–54 of
  `codex-findings.md`).
- An `ArgumentCaptor<String>` on `llmInvocation.invokeChat(...)`
  captures the projection JSON passed at every loop iteration. Parses
  via `ObjectMapper`; no raw-string matching. Asserts the step-1
  projection's `already_called[0].tool == "search_knowledge"`,
  `at_step == 0`, and `arguments_hash ==` an independently computed
  16-hex SHA-256 prefix of the canonical-JSON-serialised step-0
  arguments. Computing the expected hash inline (Jackson
  ObjectMapper with `ORDER_MAP_ENTRIES_BY_KEYS`, SHA-256, first 16 hex
  chars) rather than calling the package-private
  `ContextProjectionBuilder.canonicalArgumentsHash` confirms the slot
  uses the documented algorithm.

Why integration shape is correct for Test A: Codex Finding 1's pasted
diff at `codex-findings.md:38` flagged that the existing
`AlreadyCalledProjectionTest.alreadyCalled_includesEveryRepeatedSuccessfulDispatch`
hand-constructs two `ToolEvent`s and calls `ContextProjectionBuilder`
directly, so it cannot detect a future runtime short-circuit added
before event creation. The new test exercises the entire run loop and
verifies the dispatcher was invoked twice for the identical-args calls,
which is the bar that future-proofs the non-enforcement claim.

**Test B — `AlreadyCalledCs011T2ShapeTest`.** Unit shape under
`server/src/test/java/com/gumtree/csagent/service/runtime/`. Builds a
UC-D / FAQ-path session at the T2 boundary (faqMissCount=1 per the
cs_011 override shape), constructs a `ToolEvent` representing the prior
`search_knowledge` dispatch with a password-reset-loop query, calls
`ContextProjectionBuilder.build(..., List.of(priorEvent))` directly,
and asserts the `already_called` slot contains exactly one entry
matching the prior call. The user-message field uses an abbreviated
verbatim quote from the cs_011 source case so the test is recognisable
as the cs_011 T2 input shape on grep.

### cs_011 T2 shape — unit vs integration choice

**Choice:** unit shape (under `service/runtime/`).

**One-line justification:** the cs_011 T2 coverage bar from Codex
Finding 1 + parent objective lines 231–233 is narrowly that the slot
is populated for the cs_011 input; Test A already covers the runtime
non-enforcement bar at `AgentRunLoopImpl.run` granularity, so Test B
does not need to re-prove it, and the unit shape next to
`AlreadyCalledProjectionTest` is the natural home for projection-side
shape coverage.

### Parent bar satisfied

- `docs/sprint_objective.md` lines 160–161 — Track B "runtime does
  **not** short-circuit on the slot being populated". Demonstrated by
  Test A's `verify(toolDispatcher, times(2)).dispatch(...)` + the
  `TerminalOutcome.FINAL_ANSWER` assertion proving the loop iterated
  past both identical-args dispatches before exiting.
- `docs/sprint_objective.md` lines 231–233 — Track B's target coverage
  including "the slow-LLM cs_011 T2 shape". Demonstrated by Test B's
  UC-D / faqMissCount=1 / cs_011-quote projection assertion.

### Test runner tail (Test A + Test B in isolation)

```
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.609 s -- in com.gumtree.csagent.integration.AgentRunLoopAlreadyCalledNonEnforcementIntegrationTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS

[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.568 s -- in com.gumtree.csagent.service.runtime.AlreadyCalledCs011T2ShapeTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

## Finding 2 — shadow access-boundary reconciliation

**Codex reference:** `docs/codex-findings.md` lines 56–85 (Finding 2).
**§4.1 failing question:** Q8. **§3.1 layer:** `infra` (eval-harness
documentation).

### Diff summary

Edit only to `eval_interactive/case_specs_shadow/_ACCESS_BOUNDARY.md`.
No code path touched, no other file edited.

### Three actions performed (Option A)

1. **Removed** the `Eval-harness runner (with --include-shadow)` row
   from the "Who may read what" enforcement table (previously line 23
   of the pre-fix file). The remaining table now correctly enumerates
   the four roles whose access claims match the v0 mechanism actually
   shipped: dev agent (no), human reviewer (yes), review agent (yes),
   eval-harness runner default invocation (no).
2. **Moved** the `--include-shadow` flag description out of the
   `Enforcement (v0)` section (previously lines 44–48). The relocated
   text now sits under a renamed "Known v0 gaps / v1 hardening"
   section and frames the flag as a v1 hardening direction the next
   eval-governance sprint may add. Citation to
   `R-shadow-include-flag-runner-gate` per
   `docs/sprints/sprint-020-handoff.md:927` is preserved.
3. **Reconciled** the `Enforcement (v0)` section so it enumerates only
   the three mechanisms that actually shipped (per
   `docs/sprints/sprint-020-handoff.md` lines 443–448):
   (i) directory boundary, (ii) custom-path-only loading via
   `CaseSetManager.load_custom(path)`, (iii) documented self-restraint.
   The prior "Runner gate" item #2 is replaced with the
   custom-path-only loading item; the documented self-restraint item
   keeps its numbering.

The pre-fix "Known v0 gaps" content (the find/glob visibility note and
the deferred v1 hardened-mechanism note) is preserved in the renamed
section.

### Parent bar satisfied

- `docs/sprint_objective.md` lines 110–119 — Track A shadow-split
  mechanism consistency. After the reconciliation, the
  `_ACCESS_BOUNDARY.md` "Enforcement (v0)" section enumerates exactly
  the v0 mechanism actually shipped, and the `--include-shadow` flag
  is framed as a v1 hardening direction rather than a currently
  enforced gate.

### Cross-reference

`docs/sprints/sprint-020-handoff.md:443–448` (sprint archive, not
edited) describes the v0 mechanism that actually shipped:

> Sprint 20 does **not** add a `--include-shadow` flag to the CLI; the
> runner-flag gate is documented in `_ACCESS_BOUNDARY.md` as a v1
> hardening direction, and the v0 mechanism relies on the directory
> boundary + custom-path-only loading for shadow access.

The reconciled `_ACCESS_BOUNDARY.md` now matches this statement
verbatim in intent (three shipped mechanisms + `--include-shadow` as
v1 hardening).

Independent verification: `grep -n "include-shadow\|include_shadow"`
across `eval_interactive/eval_interactive/batch/sets.py` and
`eval_interactive/eval_interactive/cli.py` returns zero matches,
confirming the flag is not implemented.

## Verification

### Full server suite (canonical command)

Maven multi-module — parent pom is `csagent-parent`; modules `server`
and `eval` per `pom.xml` lines 20–23. The canonical command is `mvn -pl
server test`, run from the repository root. No Maven wrapper (`./mvnw`)
is used for this repo.

### Verification tail

```
[INFO] Running com.gumtree.csagent.service.knowledge.RerankServiceTest
... (verbose service logs elided for clarity) ...
[INFO] Tests run: 12, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.005 s -- in com.gumtree.csagent.service.knowledge.RerankServiceTest
[INFO]
[INFO] Results:
[INFO]
[WARNING] Tests run: 896, Failures: 0, Errors: 0, Skipped: 1
[INFO]
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  26.169 s
[INFO] Finished at: 2026-05-13T13:39:55+08:00
[INFO] ------------------------------------------------------------------------
```

Full suite: 896 tests run, 0 failures, 0 errors, 1 skipped, BUILD
SUCCESS. The single skipped test predates this fix (no new skip
introduced).

### Per-finding accept-criteria confirmation

**Finding 1:**

- `verify(toolDispatcher, times(2)).dispatch(eq("search_knowledge"),
  any(), any())` passes on Test A. ✓
- The captured step-1 projection contains an `already_called` array
  with exactly one entry whose `tool == "search_knowledge"`,
  `at_step == 0`, and `arguments_hash` equals the independently
  computed canonical hash of the step-0 arguments. ✓
- Test B passes; shape choice (unit) + one-line justification cited
  above. ✓
- No production source file edited (verified by diff scope: only the
  two new `*Test.java` files and the doc files cited here). ✓
- Full server suite green (896/0/0/1). ✓

**Finding 2:**

- Line-23 row (`Eval-harness runner (with --include-shadow)`) removed
  from the access table. ✓
- `Enforcement (v0)` enumerates only the three actually-shipped
  mechanisms (directory boundary, custom-path-only loading, documented
  self-restraint). ✓
- `--include-shadow` text relocated to "Known v0 gaps / v1 hardening",
  framed as v1 hardening with `R-shadow-include-flag-runner-gate`
  cross-reference. ✓
- No Python file changed (verified by diff scope). ✓

## Out-of-scope observations

None. The fix iteration's two findings closed cleanly within the scope
named by `docs/sprint_objective.md`'s "Sprint 20 fix iteration"
section. No additional inconsistencies in `_ACCESS_BOUNDARY.md`
surfaced during the F2 edit; no additional Track B regression-test gap
surfaced during the F1 test drafting. The runtime stop-condition check
for an existing `AgentRunLoopImpl.run(...)` short-circuit did not fire
— the loop's dispatch path (line 286–526 of `AgentRunLoopImpl.java`)
calls `toolDispatcher.dispatch(...)` unconditionally for tool calls
that survive the existing intake / handover / record-outcome guards,
none of which consult `already_called`.
