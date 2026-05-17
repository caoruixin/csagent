## Sprint Review Decision (fix-iteration #2)
decision: pass
blocking_count: 0
summary: Finding 2 is closed by commit `8d3e73b`: the T8 non-enforcement integration test is now parameterized across six slot-value variants and asserts invariant TerminalOutcome, LLM call count, zero tool dispatch, final user-message text, and per-variant projection-slot contents. Finding 1 remains accepted as out_of_scope_review: the fix-iteration #2 commit contains no production, prompt, latency/provider, smoke, config, or eval-surface mitigation and leaves that investigation carried by `R-llm-provider-latency-drift-2026-05-16`.

## Review Evidence (fix-iteration #2)

- Reviewed commit range: `8908775..8d3e73b` (`sprint 31 fix-iteration #2: strengthen T8 non-enforcement test (parameterized across 6 slot variants)`).
- Scope check passed: the commit touches exactly `server/src/test/java/com/gumtree/csagent/integration/AgentRunLoopAlternateCandidateUseCasesNonEnforcementIntegrationTest.java` and new `docs/sprints/sprint-031-fix-handoff.md`; no `server/src/main/**`, prompt, CaseSpec, action-bank, or main Sprint 31 handoff file is touched.
- T8 coverage check passed: `@ParameterizedTest` + `@MethodSource` covers six variants — null, empty array, single active-only, single alternate, multi active-plus-alternates, and unrelated UCs — with expected projection-slot assertions for each.
- Invariance check passed: the test asserts `TerminalOutcome.FINAL_ANSWER`, `verify(llmInvocation, times(1))`, `verifyNoInteractions(toolDispatcher)`, identical final user-message text, and expected per-variant projection-slot contents.
- Focused validation was run from a clean local clone checked out at `8d3e73b`: `mvn -q -pl server -Dtest=AgentRunLoopAlternateCandidateUseCasesNonEnforcementIntegrationTest test` -> pass, `Tests run: 6, Failures: 0, Errors: 0, Skipped: 0`.
- Mental check passed: a hypothetical `AgentRunLoopImpl` branch on non-null / non-empty `session.getIntakeAmbiguousCandidates()` would diverge on at least V4/V5/V6 (and on V3 for length-only checks) via outcome, LLM-call count, tool-dispatch, or final-message assertions, so the strengthened test would catch the regression Finding 2 required it to catch.
- §4.1 kernel verdict: approve. This is a pure test-strengthening + handoff commit; no production semantic surface is touched, no eval-text / CaseSpec id is encoded into runtime, and tool / capability / PII / grounding floors are preserved.

---

## Sprint Review Decision
decision: fix_required
blocking_count: 2
summary: Sprint 31 ships the intended Option beta projection bundle shape without adding a runtime semantic hardcode: the committed code preserves `RoutingResult.ambiguousCandidates()`, projects only `alternate_candidate_use_cases`, leaves hard-fenced classifier/router/drift/tool surfaces untouched, and the prompt paragraph is principle-level guidance adjacent to `already_called`. Closure is still blocked because the required smoke acceptance floor regressed vs the Sprint 28 reference, and the load-bearing T8 non-enforcement integration test is too weak to prove runtime decisions are unchanged as a function of the slot's contents.

Review scope:

- Reviewed committed range: `3812153..8908775` on `refactor/remove-the-shackles`.
- Substantive implementation commit: `2c1fd41` (`sprint 31: alternate_candidate_use_cases projection slot (Option beta)`).
- Fix / close docs commits reviewed for evidence: `de47635` and `8908775`.
- Dirty working-tree files observed but not treated as Sprint 31 committed substance: mock-data / mock-service files, `csagent_system_design_review.md`, `docs/sprint_objective.md`, `docs/sprints/sprint-030-handoff.md`, `server/src/main/resources/prompts/system_prompt.txt`, and the untracked Sprint 31 prompts / mock fixtures.

## Blocking Findings

1. BLOCKING - smoke acceptance floor regressed vs Sprint 28 reference. `docs/sprints/sprint-031-handoff.md:456` reports the Sprint 31 smoke at `eval_interactive/results/20260516-024934/results.json` against the Sprint 28 reference `eval_interactive/results/20260514-181257/results.json`: `passed_cases` 2 -> 1, `task_success_rate` 0.1429 -> 0.0714, `mean_composite_score` 0.1299 -> 0.0617, `mean_outcome_score` 0.7302 -> 0.6141, and `mean_judge_score` 0.6667 -> 0.5714. The handoff itself records the stop condition at `docs/sprints/sprint-031-handoff.md:499` and the eval acceptance failure at `docs/sprints/sprint-031-handoff.md:1028`. I re-ran the summary jq extraction and got the same numbers. Review prompt section 3.5 makes this no-regression bar blocking; the section 13 external-provider-drift hypothesis may be a valid follow-on explanation, but it does not clear the explicit Sprint 31 close gate.

2. BLOCKING - the T8 non-enforcement integration test does not demonstrate that runtime decisions are unchanged as a function of `alternate_candidate_use_cases`. `server/src/test/java/com/gumtree/csagent/integration/AgentRunLoopAlternateCandidateUseCasesNonEnforcementIntegrationTest.java:102` runs exactly one populated-slot scenario (`["UC-A", "UC-C"]`), `server/src/test/java/com/gumtree/csagent/integration/AgentRunLoopAlternateCandidateUseCasesNonEnforcementIntegrationTest.java:132` stubs a no-tool-call final answer, and `server/src/test/java/com/gumtree/csagent/integration/AgentRunLoopAlternateCandidateUseCasesNonEnforcementIntegrationTest.java:168` only asserts the loop returns `FINAL_ANSWER`. That proves the current happy path does not short-circuit on this one value, but it does not compare populated vs empty / unrelated / different contents, and it does not exercise dispatch, phase transition, reroute, or escalation outputs under varying slot values. A future Java branch such as `if alternate_candidate_use_cases contains UC-C then ...` could still evade this test if it did not affect this exact no-tool-call path. Review prompt section 3.3 requires T8 to show the runtime ignores the slot value; this test is too weak even though a static `git grep` finds no committed decision branch today.

## Anti-Hardcode Kernel

- Q1 keyword / regex / if-else / enum / per-UC matrix: No semantic decision matrix added. The new `SessionManager` assignment at `server/src/main/java/com/gumtree/csagent/service/runtime/SessionManager.java:193` preserves the existing `routingResult.ambiguousCandidates()` list; the projection `if` checks at `server/src/main/java/com/gumtree/csagent/service/runtime/ContextProjectionBuilder.java:409` are null / blank / active-UC filtering, not user-content or per-UC routing logic.
- Q2 Tier-0 justification: N/A. No Tier-0 invariant is added and `docs/runtime_freeze_and_risk_policy.md` is untouched.
- Q3 soft signal achievable: Yes. The landed design is the soft signal: persisted intake snapshot plus projection slot, with the LLM owning whether to use it.
- Q4 eval text / CaseSpec encoding: No. The runtime and prompt diff contain no `cs_NNN` / trace-specific text; the prompt names only the slot and observable provenance.
- Q5 semantic ownership shift: No in the committed code. Static references to `intakeAmbiguousCandidates` are confined to the model field, AMBIGUOUS capture, projection builder, migration, prompt, tests, and handoff text; no Java decision path gates routing / phase / escalation on it.
- Q6 prompt if-else dump: No. `server/src/main/resources/prompts/system_prompt.txt:30` is a sibling teaching paragraph to `already_called`, with provenance + soft-signal posture and no UC-specific or tool-specific branches.
- Q7 tool / capability / PII / grounding: Preserved. No tool schema or permission boundary changed; the new persisted data is UC ids, not PII; FAQ grounding files are untouched.
- Q8 generalization coverage: Incomplete for close because T8 is weak (Finding 2). Target / projection / negative unit coverage exists, and shadow CaseSpec authoring is explicitly deferred to Sprint 31+1 in `docs/sprints/sprint-031-handoff.md:679`, consistent with OQ4, but the required non-enforcement evidence is not strong enough.
- Q9 rollback / sunset: Permanent additive slot; no feature-flag scaffolding required. Reverting the migration + projection/prompt block remains the rollback path.

PR-level hardcode verdict: no semantic hardcode found in the code or prompt. Sprint-close verdict remains `fix_required` because Findings 1 and 2 fail required close gates.

## Hard-Fence Verification

- No forbidden Option alpha / delta / router / tool surfaces were touched in `3812153..8908775`: `RuntimeIntentClassifier.java`, `IntentClassification.java`, `DriftResult.java`, `DriftDetector.java`, `UseCaseRouter.java`, and `ClassifyUseCaseTool.java` are absent from the range diff.
- No `eval_interactive/case_specs/**`, `docs/current/**`, `docs/foundational/**`, `docs/runtime_freeze_and_risk_policy.md`, deadline / model / retry / budget config, or `pom.xml` paths were touched in the committed range.
- Projection expansion is limited to one key: `ContextProjectionBuilder.java` adds only `projection.set("alternate_candidate_use_cases", ...)`.
- OQ checks pass: no ROUTED-branch setter, prompt paragraph is adjacent to `already_called`, `iteration_governance.md` is unchanged, no CaseSpecs were added, and `R-uc-cdf-get-customer-context-bot-actual-usage` is unchanged.
- Packaging / close docs in the committed range (`docs/10-handoff.md`, `docs/action_bank.md`, `docs/sprints/sprint-031-objective.md`, `docs/sprints/sprint-031-handoff.md`) are not treated as substance blockers.

## Schema And Reproducibility Checks

- Backwards-compat projection-key check passed on populated turns: union of keys in `eval_interactive/results/20260516-024934/results.json` is a superset of `eval_interactive/results/20260514-181257/results.json` with `added=["alternate_candidate_use_cases"]` and `removed=[]`.
- Slot presence recipe re-run: `[{"k":false,"n":1},{"k":true,"n":22}]` for `alternate_candidate_use_cases`; the false row is the documented defensive empty-projection / contract-violation turn.
- Non-empty alternates recipe re-run: four unique cases carry non-empty alternates (`cs_interactive_015`, `cs_interactive_040`, `cs_interactive_176`, `cs_interactive_192`), matching handoff section 5.4.
- Smoke summary recipe re-run: Sprint 28 reference returns `passed=2 mean_comp=0.1299 mean_out=0.7302 mean_judge=0.6667`; Sprint 31 rerun #1 returns `passed=1 mean_comp=0.0617 mean_out=0.6141 mean_judge=0.5714`; reruns #2/#3 return the handoff section 13 numbers.

## Validation Run

- Ran the new Sprint 31 tests from a clean temporary worktree at `8908775`: `mvn -q -pl server -Dtest=IntakeAmbiguousCandidatesProjectionTest,AgentRunLoopAlternateCandidateUseCasesNonEnforcementIntegrationTest test` -> passed.
- I did not re-run the full 14-case smoke; the review re-used the committed result artifacts named by the handoff and re-ran the cited jq extractions against them.

## Deferred / Non-Blocking Notes

- The follow-on latency diagnostic (`R-llm-provider-latency-drift-2026-05-16`) and the Sprint 31+1 CaseSpec-authoring surface are valid follow-ons already named in the handoff/action bank. Per the deferral rule, they are not blocker findings beyond the explicit smoke close-gate failure above.
