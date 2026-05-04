# Codex Findings - Round 5 Interactive Eval Review

Date: 2026-05-04

Reviewed scope:

- Design docs from phase 0 through phase 5, handoff, interactive case generation plan, fixed script library, and customer service tool spec.
- Smoke case specs under `eval_interactive/case_specs/smoke`.
- Latest interactive result: `eval_interactive/results/20260504-081853/results.json`.
- Latest implementation diff: `HEAD~1..HEAD` at `32b0779`.

## Executive Summary

The latest implementation fixes several prior evaluator defects: session-create replies are now visible in the transcript, the YAML escalation enum is synced to 23 values, `trace_minimum` and `required_escalation` were added as global L1 checks, `acceptable_outcomes` has a cross-class quality guard, and secondary-UC credit was narrowed. Those are directionally correct and mostly minimal.

The eval is still not ready to act as a production gate. The latest run remains `4/14` passed, with `stall_rate=7.1%`. More importantly, the new run exposes remaining root-cause and rubric issues: `cs_interactive_029` reports `escalation_reason=turn_budget_exhausted` but L1 `escalation_compliance` passes; `cs_interactive_066` likely penalizes a reasonable UC-K technical intake because the case spec expects UC-E; and `cs_interactive_015` gets a stall failure from a session-create placeholder even though the next bot turn asks a clarifying question.

The largest gaming risk is reduced but not closed. A cross-class resolve-to-escalate fallback now needs a handover summary of at least 10 characters and a reason, but that is still not a useful handover. A generic summary like "User needs help" would pass the guard without answering the customer or proving the handover contains source/status evidence.

## Direct Answers

1. The smoke cases partly test production-critical behavior, but not enough. They cover some FAQ, routing, escalation, and handover paths, but they still miss GDPR deletion, moderation appeal, tool errors, out-of-scope handling, explicit callback requests, payment FAQ safety advice, and policy-specific free-items restrictions.

2. The rubric can still be gamed. The new `acceptable_outcomes` guard is syntactic, not semantic; `correct_uc` trusts `candidate_use_cases`; and L3 groundedness can give a 5 to a conversation that contains an unsupported turn-0 factual answer.

3. Some failures are assigned to the wrong root cause. `cs_interactive_029` is the clearest mismatch: the reported session reason is cross-family with the expected trigger, but L1 passes. `cs_interactive_066` also looks like a case-spec/root-cause problem rather than a bot failure.

4. The design changes are small and mostly testable, but the tests cover scorer fixtures more than end-to-end behavior. There is no regression test proving session-create transcript rows align with trace rows, stall detection, L3 judge input, or report output.

5. Missing hard gates remain: escalation reason consistency, global user-requested escalation, trace/transcript alignment, semantic useful handover, turn-0 source grounding, and service-outcome quality.

6. Implementation changes match the proposals only partially. The code fixes the obvious prior issues, but the proposed H1 grounded-truth gate, H4 useful-handover gate, full service-outcome taxonomy, and transcript-based issue matching remain incomplete.

## Correctness Bugs

### P0. Escalation reason consistency is not enforced

`cs_interactive_029` expected `user_requested`, because the user asks "can you please call me now?" The latest result displays `escalation_reason=turn_budget_exhausted`, but `escalation_compliance` passes. See [results.json](/Users/caoruixin/projects/csagent-design-v1-without-human-review-dataset/eval_interactive/results/20260504-081853/results.json:1073).

This can only happen if the scorer is reading a different reason from the value serialized in `session_state` or result output. `_check_escalation_compliance` reads the first `request_handover` tool call via `_first_handover_escalation_reason`, while the report displays `trace.session_state.escalation_reason`. See [hard_checks.py](/Users/caoruixin/projects/csagent-design-v1-without-human-review-dataset/eval_interactive/eval_interactive/scoring/hard_checks.py:542).

Minimal fix: add an L1 `escalation_reason_consistency` gate requiring the first/terminal `request_handover` tool call, handover payload, and `session_state.escalation_reason` to agree or explicitly record an approved override. The result should display the same reason used for scoring.

### P0. `cs_interactive_066` likely has the wrong expected root cause

The case description asks why the phone-number contact option is missing when listing an item. The bot routes to UC-K technical intake, collects Android/app details, asks whether the option is missing/greyed out/erroring, then escalates. The result fails on `no_forbidden_tools`, `escalation_compliance`, and `correct_uc` because the spec expects UC-E and forbids `create_case_controlled`. See [cs_interactive_066.yaml](/Users/caoruixin/projects/csagent-design-v1-without-human-review-dataset/eval_interactive/case_specs/smoke/cs_interactive_066.yaml:21) and [results.json](/Users/caoruixin/projects/csagent-design-v1-without-human-review-dataset/eval_interactive/results/20260504-081853/results.json:1778).

Based on the domain docs, "option missing / used to work / app regression" belongs closer to UC-K than UC-E product/search guidance. Penalizing `create_case_controlled` here risks teaching the agent not to escalate real technical regressions.

Minimal fix: reclassify this smoke case as UC-K, or split it into two cases: a UC-E "how do contact options work?" FAQ and a UC-K "phone option disappeared in app" technical intake.

### P1. The cross-class `acceptable_outcomes` guard is still too weak

The new guard requires only `summary >= 10` and a non-empty `escalation_reason` for resolve-to-escalate fallback. See [outcome_checks.py](/Users/caoruixin/projects/csagent-design-v1-without-human-review-dataset/eval_interactive/eval_interactive/scoring/outcome_checks.py:225).

That blocks completely empty handovers, but it still allows generic non-answers. It does not require the summary to mention the user's actual issue, identifiers, sources searched, partial answer, or unresolved question.

Minimal fix: make `escalated_acceptably` require issue-specific summary text, evidence of attempted source/status checks where applicable, and either a partial answer or a clear reason the bot could not answer.

### P1. Secondary UC credit still relies on runtime candidates, not handled issue evidence

`correct_uc` now gives full credit for a secondary UC if the primary UC appears in `candidate_use_cases`. See [outcome_checks.py](/Users/caoruixin/projects/csagent-design-v1-without-human-review-dataset/eval_interactive/eval_interactive/scoring/outcome_checks.py:154).

This is better than unconditional secondary credit, but it is still gameable. `cs_interactive_095` gets `correct_uc=1.0` because UC-A is preserved in candidates while actual UC is UC-D, yet the bot immediately escalates and does not answer the email/app/no-adverts question. See [results.json](/Users/caoruixin/projects/csagent-design-v1-without-human-review-dataset/eval_interactive/results/20260504-081853/results.json:1960).

Minimal fix: full secondary-UC credit should require transcript or handover evidence that the secondary issue was actually handled and the original issue was answered or preserved. Candidate-list presence should be diagnostic only.

### P1. User-requested escalation is not a global hard gate and misses callback language

`user_requested_escalation` is available but not global, and none of the smoke specs configure it. The patterns also cover "speak/talk/connect/transfer" but not callback language such as "call me now", "phone me", "ring me", or "callback". See [hard_checks.py](/Users/caoruixin/projects/csagent-design-v1-without-human-review-dataset/eval_interactive/eval_interactive/scoring/hard_checks.py:251) and [hard_checks.py](/Users/caoruixin/projects/csagent-design-v1-without-human-review-dataset/eval_interactive/eval_interactive/scoring/hard_checks.py:665).

Minimal fix: make explicit human/callback request handling a global L1 gate, and expand patterns to callback language. Add a reason-precedence assertion that user-requested escalation beats `turn_budget_exhausted`.

### P1. Trace minimum is still not trace/transcript alignment

`trace_minimum` now catches blank containment and trace turns with user text but no bot reply. See [hard_checks.py](/Users/caoruixin/projects/csagent-design-v1-without-human-review-dataset/eval_interactive/eval_interactive/scoring/hard_checks.py:621).

It does not check that the transcript rows injected by `SessionRunner` have corresponding trace rows, tool evidence, source IDs, or turn-count alignment. This matters because `SessionRunner` now injects turn-0 form and greeting rows into transcript, but the trace collector may still only contain runtime turns. See [session_runner.py](/Users/caoruixin/projects/csagent-design-v1-without-human-review-dataset/eval_interactive/eval_interactive/simulator/session_runner.py:113).

Minimal fix: either synthesize a trace turn for session-create with source/tool metadata, or add a separate transcript/trace alignment gate in the executor after both artifacts exist.

### P1. Stall detection likely false-positives on session-create placeholders

`cs_interactive_015` now fails `L1:no_stall` because turn 0 says "I'm looking into this for you." The next bot turn asks a clarifying question ("Are you asking how to edit your ad, or how to appeal a removal?"), which is forward progress even if the overall answer is wrong. See [results.json](/Users/caoruixin/projects/csagent-design-v1-without-human-review-dataset/eval_interactive/results/20260504-081853/results.json:922).

The visible-result regex recognizes "could/can you tell/share/confirm" but not "are you asking" or "to confirm". See [stall_detector.py](/Users/caoruixin/projects/csagent-design-v1-without-human-review-dataset/eval_interactive/eval_interactive/scoring/stall_detector.py:42).

Minimal fix: either exclude session-create greetings from stall detection unless they include a tool-backed promise, or broaden visible follow-up patterns to include clarification forms such as "are you asking", "do you mean", and "to confirm".

### P2. Turn-0 factual answers can still evade groundedness review

`cs_interactive_192` now includes the form question and bot's initial answer, which is good. But L3 groundedness still reasons only about the final escalation turn and gives groundedness 5, despite the turn-0 answer making factual claims about free-item posting. See [results.json](/Users/caoruixin/projects/csagent-design-v1-without-human-review-dataset/eval_interactive/results/20260504-081853/results.json:2117).

The deterministic citation gate also exempts short factual answers unless a knowledge tool was called or the text is long enough. See [hard_checks.py](/Users/caoruixin/projects/csagent-design-v1-without-human-review-dataset/eval_interactive/eval_interactive/scoring/hard_checks.py:128).

Minimal fix: apply groundedness to every factual bot turn, including session-create turn 0. Short policy/process claims should still require source support when `grounding_mode=faq_source_backed`.

## Missing Eval Cases

Add or fix these before using smoke as a production gate:

- UC-F seller payment FAQ: "How do I receive payment when I sell an item?" should provide safe source-backed guidance and distinguish Gumtree payments from buyer/seller arrangements.
- Free-items allowed/prohibited policy: not just "set price to free"; test what items are allowed and banned.
- Explicit callback/human request: "call me now", "ring me", "can someone phone me", "speak to a human".
- UC-G GDPR/account deletion and identity verification intake.
- UC-H moderation/deletion appeal with case creation and required identifiers.
- UC-I payment dispute/refund boundary: explain process and collect intake without promising refund.
- UC-J scam report versus general scam-safety advice. Mentioning "scam" in a payment FAQ should not always create a trust/safety case.
- UC-K technical regression with case creation, separate from UC-E feature explanation.
- Out-of-scope topics such as delivery courier disputes, legal advice, pro contracts, and reviews/ratings.
- Tool errors/timeouts: knowledge search empty, resolver timeout, create-case failure, and handover failure.
- Session-create transcript alignment: form description, bot greeting, trace turn, sources, L3 input, and HTML output all match.

## Weak Rubric Dimensions

- `correct_outcome` still checks terminal class first and service quality second. Same-class matches get full credit even if the bot resolved with a weak answer. See [outcome_checks.py](/Users/caoruixin/projects/csagent-design-v1-without-human-review-dataset/eval_interactive/eval_interactive/scoring/outcome_checks.py:216).
- `correct_uc` uses `candidate_use_cases` as proof of preserved issue. Candidate presence is easy to satisfy without actually handling or handing over the issue.
- `handover_completeness` is too generic. It can pass without proving that the handover contains the user's actual ask, collected identifiers, searched sources, or unresolved question.
- `source_citation_present` remains a heuristic for source IDs, not a grounded-truth classifier. It does not classify status claims, policy claims, possible explanations, and commitments.
- `policy_compliance_rate=1.0` in the summary does not mean the customer-service behavior is safe or useful. It mainly reflects no forbidden phrases/PII/human-only promises.
- L3 groundedness is too forgiving for procedural escalations and can ignore earlier factual turns in the same transcript.

## Agent Design Problems

- FAQ miss fallback is still too eager. `cs_interactive_192` and `cs_interactive_259` both ask answerable FAQ/policy questions and end in escalation.
- Routing overreacts to safety terms. `cs_interactive_259` becomes UC-J after the user says they do not want to get scammed, even though the core question is seller payment mechanics.
- Session-create copy has duplicated greetings in several cases ("Hi Rita! Hi Rita!", "Hi Anthony! Hi Anthony!").
- Reason precedence is still weak. User-requested callback language should not end up serialized as `turn_budget_exhausted`.
- The simulator can produce odd user turns, such as `cs_interactive_002` where the simulated user says "Let me check your notification settings...", which reads like an assistant response and can pollute root-cause analysis.

## Tool-Use Risks

- `create_case_controlled` is invoked in `cs_interactive_259` after wrong UC-J routing, causing `no_forbidden_tools` on a resolve-capable payment FAQ. See [results.json](/Users/caoruixin/projects/csagent-design-v1-without-human-review-dataset/eval_interactive/results/20260504-081853/results.json:2274).
- Tool-sequence demotion is fine, but no replacement evidence gate ensures required lookup/search tools were used before factual answers or handovers.
- The YAML enum is now synced by adding `runtime_error_threshold`, but there is still no automated cross-source enum test. See [customer_service_tool_spec_v0_2.yaml](/Users/caoruixin/projects/csagent-design-v1-without-human-review-dataset/docs/customer_service_tool_spec_v0_2.yaml:458).
- `trace_minimum` allows a blank `bot_response` when a handover tool call exists. That may be acceptable internally, but customer-facing transcript must still show a handover message.
- Session-create transcript rows are not covered by trace tool/source checks, which creates a gap for grounding, stall detection, and auditability.

## Customer Service Policy Gaps

- Free-items guidance needs allowed/prohibited item boundaries and source backing.
- Payment guidance needs a clear distinction between seller-arranged payment, Gumtree payment products, refund/dispute handling, and scam safety.
- The fixed script library still uses email follow-up phrasing such as "You'll hear back by email" and "I've passed your details", while hard checks mainly catch first-person "I'll send/email you" promises. See [fixed_script_library_v1.md](/Users/caoruixin/projects/csagent-design-v1-without-human-review-dataset/docs/fixed_script_library_v1.md:90) and [hard_checks.py](/Users/caoruixin/projects/csagent-design-v1-without-human-review-dataset/eval_interactive/eval_interactive/scoring/hard_checks.py:200).
- Refund, restoration, deletion, moderation, and trust/safety commitments need explicit approved wording plus negative tests.
- Case-created wording should only be allowed when `create_case_controlled` succeeded and a case id is present.

## Implementation Match

Matches the proposals:

- Session-create form and bot greeting are now serialized into transcript.
- `runtime_error_threshold` was added to the YAML enum.
- `trace_minimum` and `required_escalation` were added as global L1 checks.
- The misleading escalation-family comment was fixed.
- `acceptable_outcomes` gained a cross-class quality guard.
- Secondary-UC credit was narrowed from unconditional full credit.
- Tests were added for scorer-level `acceptable_outcomes`, secondary UC, `required_escalation`, and `trace_minimum`.

Still partial or missing:

- No end-to-end test proves session-create rows appear in results, report, L3 judge input, and trace alignment.
- `required_escalation` only applies when `allow_bot_resolution=false`; explicit user-request and other `should_escalate=true` cases can still rely on softer mechanisms.
- No reason-consistency gate checks tool call reason versus session state versus handover payload.
- Useful handover remains a length/reason heuristic, not a semantic payload check.
- Grounded-truth gate is still deferred.
- Service-outcome taxonomy is still deferred.
- Smoke specs were not corrected or expanded.

## Recommended Minimal Fixes

1. Add `escalation_reason_consistency` as an L1 hard gate and make reports display the same reason the scorer used.

2. Make `user_requested_escalation` global; add callback/ring/phone patterns and a reason-precedence test for `call me now`.

3. Reclassify `cs_interactive_066` as UC-K or split it into separate UC-E and UC-K cases.

4. Strengthen cross-class `acceptable_outcomes`: require issue-specific handover summary, searched sources/status checks where applicable, and a partial answer or clear blocker.

5. Replace candidate-list secondary UC credit with transcript/handover evidence that the active issue was actually handled and the original issue was preserved.

6. Add a trace/transcript alignment gate after trace collection: turn indexes, bot replies, source IDs, handover messages, and `total_turns` must reconcile.

7. Update stall detection for session-create placeholders and broader clarification phrasing.

8. Apply groundedness checks to every factual bot turn, including short turn-0 answers.

9. Add an automated enum sync test across YAML, eval schema, and runtime.

10. Add missing production-critical smoke cases listed above, then rerun the smoke suite.

Verification note: I attempted `python -m pytest eval_interactive/tests/test_hard_checks.py eval_interactive/tests/test_outcome_checks.py eval_interactive/tests/test_stall_detector.py -q`; it exited with code `-1` and no output in this environment.
