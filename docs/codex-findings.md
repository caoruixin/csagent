# Codex Findings - Latest Interactive Eval Review

Date: 2026-05-04

Reviewed scope:

- Design docs from phase 0 through phase 5, handoff, interactive case generation plan, fixed script library, and customer service tool spec.
- Smoke case specs under `eval_interactive/case_specs/smoke`.
- Latest interactive result: `eval_interactive/results/20260504-071557/results.json`.
- Latest implementation diff: `HEAD~1..HEAD` at `bb39363`.

## Executive Summary

The latest scoring update moves in the right direction by demoting exact tool-sequence matching, but it currently over-corrects. The new rubric can be made to pass non-answers if specs start adding broad `acceptable_outcomes: [resolve, escalate]` without a service-quality gate.

The most important issue is not a model answer issue. The interactive runner omits the create-session form turn and `bot_greeting` from the stored transcript, L3 judge input, JSON results, and HTML report. This directly explains the weird `cs_interactive_192` trace: the UI shows an initial answer to the form description, then the result transcript starts only at the follow-up. That means downstream scoring and review are judging an incomplete conversation.

The current run is `4/14` passed. The three new passes are mostly due to gate demotion and semantic escalation-family matching. The remaining failures are a mix of real runtime/agent defects, eval instrumentation defects, and expected-spec mistakes. Treating them all as "acceptable over-escalation" would hide production-critical behavior gaps.

## Direct Answers To The Review Questions

1. The smoke eval partly tests production-critical behavior, but it is too thin for launch readiness. It covers some UC routing, handover, and FAQ resolution paths, but misses GDPR/account deletion, moderation appeals, out-of-scope handling, explicit human-request handling, payment answer quality, tool errors, and source-backed policy details.

2. The rubric can be gamed. `acceptable_outcomes` accepts raw terminal outcomes (`resolve`, `escalate`) rather than service outcomes. A bot can escalate immediately with a generic message and pass `correct_outcome` if a spec lists escalation as acceptable.

3. Several failures are assigned to the wrong root cause. The clearest example is `cs_interactive_029`: the handoff says expected `clarification_budget_exhausted`, but the case spec expects `user_requested`. The actual `turn_budget_exhausted` is a cross-family mismatch, so the L1 failure is valid.

4. The proposed design changes are minimal in code size, but not yet minimal and testable as a product-eval design. The implementation added a small schema field and relaxed gates, but deferred the hard gates that would make that relaxation safe.

5. Hard gates are missing around transcript/trace completeness, required escalation independent of risk level, useful handover for over-escalation, answer usefulness for resolve-capable FAQs, and enum/schema synchronization.

6. Implementation changes match only part of the proposal. The Phase 4 action schema update and tool-sequence demotion match. The richer service-outcome taxonomy, grounded-truth gate, useful-handover gate, and drift-safe issue matching were not implemented.

## Specific Smoke Trace Notes

- Trace/session `33c2e411-a10e-4a02-824a-51d625aef9b7` (`cs_interactive_259`) is a real product failure, not just a strict-path failure. The user asked "How do I receive the payment when I sell an item" and the bot immediately escalated with a generic FAQ-miss message. The UI note "No LLM call for this turn" points to deterministic runtime fallback or downstream state handling, not an LLM answer-quality decision. This case should remain failing until the agent can answer or produce a useful issue-specific handover.
- Trace/session `78e29e2e-13c9-4344-a137-6ff20f0308de` (`cs_interactive_192`) exposes the transcript split. The create-session form question was answered in the trace, but the stored result transcript starts at the follow-up "OK. What items are allowed?" and then escalates. The downstream service is receiving or reporting a conversation path that does not match the full user-visible interaction.

## Correctness Bugs

### P0. Session-create turns are missing from eval transcripts

`SessionRunner` creates the bot session and stores `bot_greeting`, but it does not append the form description or bot greeting to the transcript. The transcript starts with `generate_first_message()` instead. See [session_runner.py](/Users/caoruixin/projects/csagent-design-v1-without-human-review-dataset/eval_interactive/eval_interactive/simulator/session_runner.py:100) and [user_simulator.py](/Users/caoruixin/projects/csagent-design-v1-without-human-review-dataset/eval_interactive/eval_interactive/simulator/user_simulator.py:107).

The runner then passes only `session_result.transcript` to the L3 judge and serializes only that same transcript into results. See [executor.py](/Users/caoruixin/projects/csagent-design-v1-without-human-review-dataset/eval_interactive/eval_interactive/batch/executor.py:237) and [executor.py](/Users/caoruixin/projects/csagent-design-v1-without-human-review-dataset/eval_interactive/eval_interactive/batch/executor.py:282).

Impact:

- `cs_interactive_192` has form text asking how to give away free items, but the results transcript only starts at "OK. What items are allowed?" The UI trace shows the omitted initial answer. See [cs_interactive_192.yaml](/Users/caoruixin/projects/csagent-design-v1-without-human-review-dataset/eval_interactive/case_specs/smoke/cs_interactive_192.yaml:9).
- L2 tool traces may include actions from session creation while L3 and the report judge a shorter conversation.
- Any root-cause analysis based on the result transcript can be wrong because the user-visible first bot reply is missing.

Minimal fix: represent session creation as turn 0 in transcript/results/judge input, or explicitly serialize `form_context`, `bot_greeting`, and an `init_trace` section and make all scorers aware of it. Add a trace-minimum hard gate that every user-visible bot reply is present in the eval artifact.

### P0. `acceptable_outcomes` can turn a non-answer into a pass

The current implementation gives full `correct_outcome` credit when the raw actual outcome is listed in `Expected.acceptable_outcomes`. See [outcome_checks.py](/Users/caoruixin/projects/csagent-design-v1-without-human-review-dataset/eval_interactive/eval_interactive/scoring/outcome_checks.py:156).

This is unsafe for cases such as `cs_interactive_259`, where the customer asks how they receive payment when selling an item and the bot immediately escalates with "I'm having difficulty resolving this." The case is resolve-capable and FAQ-backed. See [cs_interactive_259.yaml](/Users/caoruixin/projects/csagent-design-v1-without-human-review-dataset/eval_interactive/case_specs/smoke/cs_interactive_259.yaml:20). Marking escalation as acceptable would reward a generic FAQ miss instead of a payment answer.

Minimal fix: do not use raw `resolve/escalate` as the only acceptable-outcome taxonomy. Add service outcomes such as `resolved_acceptably`, `partially_answered_then_escalated`, `escalated_with_useful_handover`, and `failed_non_answer`. For resolve-capable FAQ cases, escalation should pass only if the bot provided a useful partial answer or a useful, issue-specific handover.

### P1. Handoff misdiagnoses `cs_interactive_029`

The latest handoff states that `cs_interactive_029` expected `clarification_budget_exhausted` and should pass as same-family with `turn_budget_exhausted`. See [10-handoff.md](/Users/caoruixin/projects/csagent-design-v1-without-human-review-dataset/docs/10-handoff.md:195).

The actual case spec expects `user_requested`. See [cs_interactive_029.yaml](/Users/caoruixin/projects/csagent-design-v1-without-human-review-dataset/eval_interactive/case_specs/smoke/cs_interactive_029.yaml:29). The latest result escalates with `turn_budget_exhausted`. That is `user_intent` versus `bot_limit`, so the L1 `escalation_compliance` failure is assigned correctly by the scorer and incorrectly by the handoff.

Minimal fix: update the handoff triage and add a regression assertion for this case. If the desired behavior is "call me now" equals `user_requested`, test that reason precedence wins over turn budget.

### P1. Required escalation is only hard-enforced for high/critical risk

`_check_escalation_compliance` only hard-fails missing escalation for `should_escalate=true` when risk is high or critical. Low and medium required escalations fall through unless the bot happens to escalate. See [hard_checks.py](/Users/caoruixin/projects/csagent-design-v1-without-human-review-dataset/eval_interactive/eval_interactive/scoring/hard_checks.py:472).

This becomes dangerous once `correct_outcome` is relaxed. A low-risk but policy-required handover could be bot-resolved and still avoid L1 failure.

Minimal fix: add an L1 `required_escalation` gate based on `should_escalate=true`, `allow_bot_resolution=false`, and specific UC families that require human handling, independent of risk severity.

### P1. Tool spec and runtime/eval escalation enums are out of sync

The YAML tool spec lists escalation reasons through `tool_scope_blocked` and omits `runtime_error_threshold`. See [customer_service_tool_spec_v0_2.yaml](/Users/caoruixin/projects/csagent-design-v1-without-human-review-dataset/docs/customer_service_tool_spec_v0_2.yaml:433).

The eval schema says the canonical enum has 23 values and includes `runtime_error_threshold`. See [schema.py](/Users/caoruixin/projects/csagent-design-v1-without-human-review-dataset/eval_interactive/eval_interactive/case_spec/schema.py:35).

Minimal fix: choose one source of truth and add an enum-sync test across YAML, eval schema, and runtime tool schema.

### P1. Incomplete final turns are not hard-gated

`cs_interactive_066` has a second user turn asking "Can I speak to someone?", but there is no bot response in the serialized transcript. The result has blank `containment_outcome` and no escalation reason, yet it is not surfaced as a trace contract violation.

Minimal fix: add a trace-minimum hard gate requiring every simulator user turn to have a bot response or an explicit tool/runtime error. Blank containment at terminal state should be a hard failure tagged as instrumentation/runtime, not just L2 outcome failure.

### P2. Code comments contradict implemented semantic families

The comment in `hard_checks.py` says `intake_complete_for_uc_j` versus `trust_safety_required` is cross-family, but the map intentionally treats them as the same `trust_safety` family. See [hard_checks.py](/Users/caoruixin/projects/csagent-design-v1-without-human-review-dataset/eval_interactive/eval_interactive/scoring/hard_checks.py:487).

Minimal fix: update the comment. This is not behavioral, but it will mislead the next reviewer.

## Missing Eval Cases

Add cases before treating the smoke set as production-critical:

- Session-create follow-up case: form description is answered during `create_session`, then user asks a follow-up. The judge must see both turns. `cs_interactive_192` already exposes this, but the harness does not test it explicitly.
- UC-F payment FAQ case: "How do I receive payment when I sell an item?" should be answered from source, not escalated on FAQ miss.
- Free-items allowed/prohibited case: answer should cite or use the relevant allowed/prohibited items policy and avoid overbroad claims.
- Explicit human request: "Can I speak to someone?" and "call me now" should produce `user_requested` or approved equivalent, not budget exhaustion.
- UC-G GDPR/account deletion and identity verification intake.
- UC-H moderation/deletion appeal with case creation and required identifiers.
- UC-I payment dispute/refund execution boundary: explain process but do not promise or issue refund.
- UC-J scam/fraud/safety report with case id and safety handover.
- Out-of-scope topics such as delivery couriers, pro contracts, legal advice, and reviews/ratings.
- Tool failure/timeouts: knowledge search empty, resolver timeout, create-case failure, handover failure.
- Business-hours/offline handover phrasing, including no false email promise from the bot.
- Drift handling where the final user issue differs from the form issue, and the handover must preserve both.

## Weak Rubric Dimensions

### `correct_uc` gives full credit for any secondary UC

The implementation awards `1.0` when actual UC matches any `secondary_ucs`. See [outcome_checks.py](/Users/caoruixin/projects/csagent-design-v1-without-human-review-dataset/eval_interactive/eval_interactive/scoring/outcome_checks.py:129).

This masks wrong routing in cases where secondary UCs were generated broadly. `cs_interactive_095` is labeled expected UC-A but actual UC-D; it receives full `correct_uc` credit because UC-D is secondary, while the bot still escalates a weak/non-useful handover.

Minimal fix: replace this with `handled_issue_match`. Secondary UC should earn full credit only when the transcript shows a real user drift to that issue and the original issue is answered or preserved in handover. Otherwise score partial diagnostic credit.

### `correct_outcome` is not service-outcome aware

The current field answers "did the terminal state match one of these words?" It does not answer "did the customer get a useful answer or useful handover?"

Minimal fix: introduce a separate `acceptable_service_outcomes` field or change `acceptable_outcomes` values to service outcomes. Raw terminal outcome should remain a lower-level diagnostic.

### Policy compliance can look perfect while customer service fails

The latest run reports `policy_compliance_rate: 1.0` even though multiple cases produce generic escalation, missing final bot response, wrong route, or no useful answer. This shows the policy layer is checking forbidden text/tool exposure, not service success.

Minimal fix: split policy compliance into safety compliance, truth grounding, required handling, and service usefulness.

### Handover completeness is too generic

The current handover completeness logic can pass if generic fields exist, but it does not prove the human receives the actual user ask, source/status checks already performed, identifiers, or next action.

Minimal fix: require issue-specific payload fields: active issue summary, original form issue if different, collected identifiers, source/status checks, escalation reason family, and case id where policy requires case creation.

## Agent Design Problems

- FAQ miss fallback is too eager. `cs_interactive_259` and `cs_interactive_192` both escalate on questions that should be answerable from knowledge. The fallback should distinguish "no answer after grounded search" from "retrieval failed or no source resolved."
- The session-create answer and subsequent message handling are not aligned. The bot can answer the form description during create-session, then fail on the next follow-up without the evaluator preserving the context.
- Reason precedence is weak. User-requested escalation should beat budget exhaustion when the user explicitly asks to be called or speak to someone.
- Routing is still fragile across account, ad, payment, and technical issues. `cs_interactive_001`, `002`, `014`, `015`, `066`, and `095` show wrong active UC or generic fallback.
- Generic escalation copy is not enough for resolve-capable FAQs. A useful handover should state what was searched and what question remains unresolved.

## Tool-Use Risks

- Trace and transcript are not the same artifact. Tool calls from session initialization can influence state while the transcript shown to L3 and humans omits the initial exchange.
- The eval does not hard-fail missing bot replies or blank terminal containment.
- Relaxing tool sequence is reasonable, but no replacement evidence gate was added. A status claim should still require status tools; a policy answer should still require knowledge support.
- Enum drift between the YAML tool spec and eval schema can make traces appear valid in one layer and invalid in another.
- No tests were added for the new `acceptable_outcomes`, secondary-UC full credit, semantic reason families, or transcript inclusion behavior. The `HEAD~1..HEAD` diff changes schema and scorer behavior only.

## Customer Service Policy Gaps

- Free-items guidance needs explicit allowed/prohibited item boundaries, not only "use Community and set price to free."
- Payment guidance must distinguish seller payment arrangements from Gumtree payments/refund/dispute flows. The bot should not imply Gumtree receives or releases all seller payments unless source-backed.
- Email follow-up phrasing is inconsistent. The fixed script library contains "You'll hear back by email" and "I've passed your details" templates, while hard checks mainly catch first-person "I'll send/email you" promises. See [fixed_script_library_v1.md](/Users/caoruixin/projects/csagent-design-v1-without-human-review-dataset/docs/fixed_script_library_v1.md:90) and [hard_checks.py](/Users/caoruixin/projects/csagent-design-v1-without-human-review-dataset/eval_interactive/eval_interactive/scoring/hard_checks.py:198).
- Refund, restoration, deletion, moderation, and trust/safety commitments need explicit approved wording and negative tests.
- Business-hours/offline handover scripts should say what has actually happened in the system. "I've created a case" is only allowed after case creation succeeded and a case id is present.

## Implementation Match To Proposals

Matches:

- Phase 4 legacy action schema was updated to the tool-calls model.
- `tool_sequence_match` was demoted from mandatory gating.
- `Expected.acceptable_outcomes` was added to schema/loader.
- Escalation reason matching now supports semantic families.

Does not match or is only partial:

- H1 grounded-truth gate was not implemented.
- H4 useful-handover gate was not implemented beyond existing generic fields.
- H6 trace-minimum gate does not catch omitted init turns or missing final bot replies.
- Service-outcome taxonomy was not implemented; raw `resolve/escalate` is too weak.
- Drift-safe issue matching was not implemented; secondary UC now gives unconditional full credit.
- No tests were added for the relaxed behavior.
- The handoff says no Java/runtime changes are needed, but the latest traces still show runtime/control issues: omitted init transcript, generic FAQ miss escalation, reason precedence, and blank final state.

## Recommended Minimal Fixes

1. Fix eval instrumentation first. Include create-session form text and `bot_greeting` in transcript/results/L3 input, or serialize them separately and update scorers. Add a hard gate for trace/transcript alignment.

2. Do not mark `cs_interactive_259` as accepting escalation. Keep it failing until the agent can answer the seller-payment question from a source or produce a genuinely useful partial answer plus handover.

3. Replace raw `acceptable_outcomes` with service outcomes, or gate escalation alternatives with `answer_or_useful_handover`.

4. Add `required_escalation` as an L1 hard gate independent of risk level.

5. Replace unconditional secondary-UC full credit with `handled_issue_match`, requiring evidence of real user drift and issue preservation.

6. Add a trace-minimum gate for missing bot replies, blank terminal containment, missing `bot_greeting`, and mismatched trace/result turn counts.

7. Sync escalation reason enums across YAML, eval schema, and runtime; add an automated test.

8. Fix the `cs_interactive_029` handoff diagnosis and add a reason-precedence test for user-requested escalation.

9. Add targeted smoke cases for GDPR, appeal, trust/safety, payment FAQ, payment dispute, out-of-scope, tool errors, and explicit human requests.

10. Add tests for every scoring relaxation introduced in `HEAD~1..HEAD`: `acceptable_outcomes`, secondary UC, same-family escalation reasons, and mandatory-gate behavior.

Verification note: I attempted to run targeted pytest checks, but `python -m pytest eval_interactive/tests/scoring/test_escalation_trigger_match.py eval_interactive/tests/test_outcome_checks.py -q` exited with code `-1` and no output in this environment. No test files were changed by the latest diff.
