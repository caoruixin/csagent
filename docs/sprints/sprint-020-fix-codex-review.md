## Sprint Review Decision (Sprint 20 fix re-review)
decision: out_of_scope_review
blocking_count: 1
summary: Finding 1 closure evidence is present: `AgentRunLoopAlreadyCalledNonEnforcementIntegrationTest` drives `AgentRunLoopImpl.run(...)` end-to-end, verifies two identical `search_knowledge` emissions reach `ToolDispatcher.dispatch(...)`, and parses the second-step `already_called` projection with `ObjectMapper`; `AlreadyCalledCs011T2ShapeTest` covers the cs_011 T2 slot-population target without asserting LLM response text. Finding 2 closure evidence is present: `_ACCESS_BOUNDARY.md` now lists only the shipped v0 mechanisms and moves `--include-shadow` to v1 hardening. The fix bundle still cannot be accepted because the committed fix diff contains files outside the authorized fix scope, including forbidden prompt/context artifacts; suggested non-blocking deferrals remain `R-shadow-include-flag-runner-gate` and `R-already-called-prompt-consumption`.

### Scope drift finding: fix commit includes forbidden out-of-scope files

- file paths: `docs/sprint_objective.md`, `compact/sprint-019-dev-prompt.md`, `compact/sprint-019-review-prompt.md`, `compact/sprint-020-dev-prompt.md`, `compact/sprint-020-review-prompt.md`, `compact/sprint-020-fix-dev-prompt.md`, `compact/sprint-020-fix-review-prompt.md`, `compact/sprint-deliver-orchestrator.md`
- diff evidence: `git diff --name-only dfebdd9..cec7da5` includes the files above in addition to the expected two tests, `_ACCESS_BOUNDARY.md`, and `docs/sprints/sprint-020-fix-handoff.md`.
- failing scope rule: the fix-review prompt says to flag scope drift if `docs/sprint_objective.md` appears, or if any prompt file appears. The expected fix diff is limited to the new runtime-level integration test, the cs_011 T2 test, the `_ACCESS_BOUNDARY.md` reconciliation, and the fix handoff.
- severity: blocking for this fix re-review (`out_of_scope_review`), even though the two original findings appear substantively closed.
- non-blocking extra packaging observed: `.gitignore`, `csagent_system_design_review.md`, and `eval_interactive/uv.lock` also appear outside the expected fix diff. I did not treat these as separate blockers because the forbidden scope drift above is already sufficient.

### Resolution evidence observed for the two original findings

**Finding 1 -- runtime non-enforcement + cs_011 T2 target coverage**

- `server/src/test/java/com/gumtree/csagent/integration/AgentRunLoopAlreadyCalledNonEnforcementIntegrationTest.java:120` adds `identicalArgsReEmission_bothDispatched_andSecondProjectionShowsSlot`.
- The test constructs a real `AgentRunLoopImpl` with a real `ContextProjectionBuilder`, supplies two identical LLM-emitted `search_knowledge` calls, and asserts `verify(toolDispatcher, times(2)).dispatch(eq("search_knowledge"), any(), any())` at `server/src/test/java/com/gumtree/csagent/integration/AgentRunLoopAlreadyCalledNonEnforcementIntegrationTest.java:192`.
- The same test captures the second LLM invocation projection, parses it through `ObjectMapper`, and asserts the `already_called` entry's `tool`, `at_step`, and independently computed `arguments_hash` at `server/src/test/java/com/gumtree/csagent/integration/AgentRunLoopAlreadyCalledNonEnforcementIntegrationTest.java:211`.
- `server/src/test/java/com/gumtree/csagent/service/runtime/AlreadyCalledCs011T2ShapeTest.java:110` adds the cs_011 T2 shape test. It asserts only slot population (`tool`, `at_step`, and hash shape) and does not assert customer-facing LLM response content.
- `docs/sprints/sprint-020-fix-handoff.md:92` cites the unit-vs-integration choice for the cs_011 T2 test, and `docs/sprints/sprint-020-fix-handoff.md:96` gives the required one-line justification.

**Finding 2 -- shadow access-boundary reconciliation**

- `eval_interactive/case_specs_shadow/_ACCESS_BOUNDARY.md:18` now has no `Eval-harness runner (with --include-shadow)` row in the access table.
- `eval_interactive/case_specs_shadow/_ACCESS_BOUNDARY.md:30` lists only the three shipped v0 mechanisms: directory boundary, custom-path-only loading via `CaseSetManager.load_custom(path)`, and documented self-restraint.
- `eval_interactive/case_specs_shadow/_ACCESS_BOUNDARY.md:60` moves the `--include-shadow` text to "Known v0 gaps / v1 hardening" and frames it as `R-shadow-include-flag-runner-gate`, not a current gate.

### Verification

- Targeted fix tests: `mvn -pl server test -Dtest=AgentRunLoopAlreadyCalledNonEnforcementIntegrationTest,AlreadyCalledCs011T2ShapeTest` -> 2 tests, 0 failures, 0 errors, 0 skipped, `BUILD SUCCESS`.
- Full server suite: `mvn -pl server test` -> 896 tests, 0 failures, 0 errors, 1 skipped, `BUILD SUCCESS`.
- Scope checks found no `server/src/main/**` changes, no `AlreadyCalledProjectionTest.java` changes, no `server/src/main/resources/prompts/**` changes, no CaseSpec/manifest/persona edits beyond `_ACCESS_BOUNDARY.md`, no Python harness edits under `eval_interactive/eval_interactive/**`, and no implemented `--include-shadow` flag.
