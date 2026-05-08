## Sprint Review Decision

decision: fix_required
blocking_count: 1
summary: Sprint 11 implements the M0 state slots/entity reuse and adds the M2 regression suite, with `mvn -pl server test` and `python -m pytest -p no:capture eval_interactive/tests/` green. M1 is not closed because RESOLVE/FAQ `FINAL_ANSWER` still defaults to `READY_TO_CONFIRM` for any non-slot-looking answer, so an answered same-UC subtask can still collapse to CONFIRM without deterministic terminal evidence.

## Blocking Sprint Failures

- severity: P1
- target case or test: `ResolveDispositionEvaluator.evaluate` / missing non-slot UC-A answered-subtask regression
- blocks current sprint goal: yes
- evidence: `server/src/main/java/com/gumtree/csagent/service/runtime/ResolveDispositionEvaluator.java:107` handles `FINAL_ANSWER` by checking only whether the bot text looks like a question or soft ad-id request; otherwise `server/src/main/java/com/gumtree/csagent/service/runtime/ResolveDispositionEvaluator.java:123` returns `READY_TO_CONFIRM`. `server/src/main/java/com/gumtree/csagent/service/runtime/PhaseEvaluator.java:894` maps that disposition to `CONFIRM`, and `recordOutcomeSucceededThisRun` exists at `server/src/main/java/com/gumtree/csagent/service/runtime/ResolveDispositionEvaluator.java:179` but is not used, so `Sprint11ProgressiveResolveTest.test3c` passes because the text defaults ready, not because successful `record_outcome` is required. This leaves a UC-A same-UC follow-up answer such as an expiry/status answer vulnerable to RESOLVE -> CONFIRM instead of `ANSWERED_SUBTASK` / stay RESOLVE.
- exact minimal fix: In `ResolveDispositionEvaluator.evaluate`, check deterministic terminal evidence before returning `READY_TO_CONFIRM`; otherwise map non-slot RESOLVE/FAQ answers for the Sprint 11 same-UC path to `ANSWERED_SUBTASK` and keep RESOLVE. Add a focused test where a non-question UC-A listing-expiry/status answer with no successful `record_outcome` stays RESOLVE with `task_status=answered_subtask`, and strengthen `test3c` so the same text without the successful tool event does not return `READY_TO_CONFIRM`.

## Non-Blocking Notes

- severity: P2
- target case or test: Sprint 11 task-type token naming
- blocks current sprint goal: no
- exact minimal fix, if any: The implementation stays within UC-A/UC-C scope, but the concrete tokens differ from the objective (`listing_lifecycle_followup` / `messaging_diagnostic`, with no explicit `how_to_find_listing`, versus `listing_expiry_or_status_followup` / `messaging_replies_followup`). If downstream consumers require exact Sprint 11 labels, add aliases or rename the narrow tokens and pin them in `Sprint11ProgressiveResolveTest`.

- severity: P2
- target case or test: regression evidence
- blocks current sprint goal: no
- exact minimal fix, if any: None. Local verification is green: `mvn -pl server test` = 793 tests, 0 failures; `python -m pytest -p no:capture eval_interactive/tests/` = 294 passed. Referenced baseline result paths remain post-Sprint-8 because Sprint 11 did not promote a new smoke run.

## Regression Risks

- severity: P2
- target case or test: FAQ RESOLVE prompt/tool loop around `record_outcome`
- blocks current sprint goal: no
- exact minimal fix, if any: `PhaseEvaluator.plan` still allows and instructs the LLM to call `record_outcome` during RESOLVE/FAQ while `AgentRunLoopImpl` now rejects `record_outcome(resolve)` before CONFIRM/CLOSE. After the M1 fix, narrow the RESOLVE instruction to seek confirmation rather than recording resolve, or add a focused guard that proves repeated rejected `record_outcome` calls do not consume the whole tool budget.

## Recommended Next Phase

Sprint 11 closure fix
