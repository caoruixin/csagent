# Codex Findings: Latest Implementation Review

Date: 2026-05-03

Reviewed inputs:

- `docs/10-handoff.md`
- `docs/phase0_normative_freeze.md`
- `docs/phase1_solution_input_pack.md`
- `docs/phase2_domain_realization_spec.md`
- `docs/phase3_detailed_technical_design.md`
- `docs/phase4_demo_coding_agent_implementation_packet.md`
- `docs/phase5_evaluation_design.md`
- `docs/interactive_case_spec_generation_plan.md`
- `docs/fixed_script_library_v1.md`
- `docs/customer_service_tool_spec_v0_2.yaml`
- `eval_interactive/case_specs/smoke/*.yaml`
- `eval_interactive/results/20260503-065357/results.json`
- `git diff HEAD~1..HEAD`

## Executive Summary

HEAD contains only two implementation-relevant source changes:

1. `eval_interactive/eval_interactive/scoring/hard_checks.py`: narrows the source-citation gate to "substantive factual answers".
2. `server/src/main/resources/prompts/routing_prompt.txt`: fixes the UC-A/UC-D account-login label mismatch. The same generated resource is also committed under `server/target/classes/...`.

Everything else in the diff is documentation and generated eval output. The latest smoke run is still not production-close:

| Metric | `20260503-065357` |
| --- | ---: |
| Total cases | 14 |
| Passed cases | 1 |
| Failed cases | 13 |
| Task success rate | 0.0714 |
| Mean composite | 0.0691 |
| Mean outcome score | 0.5500 |
| Mean judge score | 0.6476 |
| Escalation correctness | 0.5714 |
| Stall rate | 0.0714 |
| Policy compliance rate | 1.0 |

The single pass is `cs_interactive_040` (UC-K technical issue intake). The run still has one `CONTRACT_VIOLATION`, one stall, six escalation-reason failures, six handover-completeness failures, seven UC failures, and six outcome failures.

My review conclusion: the latest patch is a useful narrow eval cleanup, but it does not yet address the production-critical runtime defects. The proposed next work in `docs/10-handoff.md` is directionally right, but several failure root causes are misassigned, the rubric remains gameable, and the smoke cases do not yet enforce the tool, case-creation, handover, source-support, latency, and human-escalation contracts that matter for production.

## Diff Review

### Source-citation gate change

The change in `hard_checks.py` exempts bot turns that look like greetings, acknowledgements, progress messages, or clarifying questions. That fixes the original false-positive class, but the implementation is now too permissive:

- Any uncited factual answer shorter than 50 characters can pass.
- Any answer ending in `?` with at most one period can pass, even if it embeds factual claims.
- A factual answer can avoid citation by including a leading pattern such as "I'm looking into..." or "to help you".
- It only treats a turn as KB-backed when `search_knowledge` or `resolve_article` was called on the same turn. If retrieval happened on an earlier tool-only turn and the next visible answer lacks `source_ids`, the heuristic may miss it.
- It does not validate that `source_ids` support the answer. Presence is enough.
- No committed test in `eval_interactive/tests` appears to cover the new citation heuristic. `rg` finds no source-citation-specific tests for clarifying vs factual answers.

This change partially matches the proposal, but it should not be considered complete until citation-scope and citation-support tests exist.

### Routing-prompt UC-A/UC-D fix

The prompt fix is correct: login/password/wrong-email belongs to UC-D, while ad visibility/moderation belongs to UC-A. This aligns with `use-case-registry.yaml`, Phase 2, and the KB mapping report.

Limitations:

- This is prompt-only, not deterministic routing logic.
- The latest eval was run against a server that may not have reloaded the changed prompt; `LlmInvocationService` loads prompts at `@PostConstruct`.
- The server still uses non-zero temperatures for chat/routing (`0.3` and `0.1`), and for Kimi models the client skips the temperature field entirely. So route behavior can still vary run to run.
- Committing `server/target/classes/prompts/routing_prompt.txt` is generated-artifact churn and does not guarantee the running JVM used the new prompt.

### No runtime fixes landed

The diff does not implement the production-side proposals:

- no cheap deterministic `createSession()`;
- no server-side escalation-reason resolver;
- no complete handover emission on every escalation path;
- no Delivery+refund semantic override before hard OOS;
- no runtime-owned case creation contract for UC-H/J/K;
- no deterministic answer-after-retrieval step;
- no stricter hard gates for contract violations/stalls/tool sequence/case IDs.

## Latest Eval Findings

Per-case latest state:

| Case | Expected | Actual | Primary problem |
| --- | --- | --- | --- |
| `cs_interactive_001` | UC-C escalate | UC-B escalate, `turn_budget_exhausted` | wrong UC, wrong reason, missing handover |
| `cs_interactive_002` | UC-C escalate | UC-F escalate, `turn_budget_exhausted` | wrong UC, wrong reason, missing handover |
| `cs_interactive_004` | UC-D resolve | UC-D escalate, `user_distress` | premature escalation despite correct UC |
| `cs_interactive_011` | UC-D escalate | UC-D escalate, `user_distress` | wrong reason, missing handover, weak handling |
| `cs_interactive_014` | UC-C escalate | UC-C escalate, `turn_budget_exhausted` | wrong reason, missing handover |
| `cs_interactive_015` | UC-FP resolve | UC-A escalate, `user_distress` | wrong UC, premature escalation |
| `cs_interactive_029` | UC-C escalate | contract violation | trace/runtime contract failure; root cause not proven by result alone |
| `cs_interactive_036` | UC-I escalate | OOS escalate | Delivery hard-OOS overrides payment dispute semantics |
| `cs_interactive_038` | UC-J escalate | UC-J escalate, `intake_complete_for_uc_j` | reason precedence bug: fraud should be `trust_safety_required` |
| `cs_interactive_040` | UC-K escalate | UC-K escalate, `intake_complete_for_uc_k` | pass |
| `cs_interactive_066` | UC-E escalate | UC-K escalate, `turn_budget_exhausted` | wrong UC, stall, wrong reason, missing handover |
| `cs_interactive_095` | UC-A resolve | UC-D escalate, `turn_budget_exhausted` | wrong UC, wrong outcome |
| `cs_interactive_192` | UC-B resolve | UC-B escalate, `turn_budget_exhausted` | immediate over-escalation of FAQ case |
| `cs_interactive_259` | UC-F resolve | UC-C escalate, `turn_budget_exhausted` | wrong UC, wrong outcome |

The latest result confirms that the dominant failure surface is still runtime routing/control, not KB tagging.

## 1. Correctness Bugs

### 1.1 Result status serialization can disagree with summary PASS

`BatchExecutor._build_case_result()` writes `"status": "PASS"` when `case_passed` is true, but top-line pass count requires `case_passed and composite_score >= 0.7`. That contradicts the Phase 5 status definition and can mark a low-composite case as `PASS` in per-case JSON while the summary counts it as failed.

Minimal fix:

- Serialize per-case `status` using the same condition as the summary: `case_passed and composite_score >= 0.7`.
- Add a regression test for `case_passed=True, composite_score=0.69`.

### 1.2 Handoff misassigns several root causes

`docs/10-handoff.md` is useful, but several case-level assignments are too broad or wrong:

- `cs_interactive_004` has correct UC-D in latest run. The root cause is premature escalation / resolve-before-escalate failure, not routing.
- `cs_interactive_015` is not a Delivery/OOS issue. It is UC-FP vs UC-A routing plus premature escalation.
- `cs_interactive_066` did not fail session creation in the latest run. It failed because phone-contact/listing behavior was routed to UC-K, stalled after tool intent, and escalated with the wrong reason. Cheap session creation remains a risk, but it is not the latest root cause.
- `cs_interactive_192` is not a handover-completeness problem in the latest run; it is immediate over-escalation of a resolvable UC-B FAQ case.
- `cs_interactive_259` is shown in the handoff table as `resolved`, but the latest JSON says `escalated` with `turn_budget_exhausted`.
- `cs_interactive_029` has `total_turns=0` in the serialized failure result. Calling it "runtime emitted an invalid UC field" is plausible but not proven from the JSON alone; the raw trace/session state should be inspected before assigning blame.

Minimal fix:

- Generate the handoff failure table directly from `results.json`.
- Separate "observed failure tag" from "hypothesized root cause".
- Require a raw trace link for every `CONTRACT_VIOLATION`.

### 1.3 Citation gate is now gameable

The new `_is_substantive_factual_answer()` heuristic fixes false positives but opens false negatives.

Minimal fix:

- Replace length/pattern-only logic with turn-type evidence:
  - if the bot makes a factual/policy/procedural answer in a FAQ-grounded case, citation is required;
  - clarifying/progress/handover turns are exempt;
  - retrieval on prior turns counts for the next visible answer;
  - source IDs must support the answer, not merely exist.
- Add tests for short factual uncited answers, long clarifying questions, progress text with factual claims, and prior-turn retrieval followed by an uncited answer.

### 1.4 Smoke specs list expected tools but do not score them

Every smoke CaseSpec has an `expected_tool_sequence`, but none of the 14 smoke specs configure `tool_sequence_match`. `case_id_present`, `turn_efficiency`, `issue_preservation`, and canonical `escalation_timing` are also absent from smoke scoring. The aliases `escalation_triggered` and `intake_fields_collected` run timing/handover checks, but the production tool-order contract is effectively inert.

Minimal fix:

- Add `tool_sequence_match` to smoke outcome checks where `expected_tool_sequence` is non-empty.
- Make it mandatory at least for UC-G/H/I/J/K and any case that expects `get_customer_context` before grounded answering.
- Add `case_id_present` to UC-H/J/K smoke cases.

### 1.5 `fixed_script_adherence` only checks "no knowledge tools"

In `hard_checks.py`, `fixed_script_adherence` aliases to `_check_intake_no_knowledge_tool()`. That does not verify fixed-script template selection, required intake fields, case creation, SLA language, forbidden promises, or safe policy wording.

Minimal fix:

- Split into explicit checks:
  - `intake_no_knowledge_tool`;
  - `fixed_script_template_used`;
  - `required_intake_fields_collected`;
  - `case_created_when_required`;
  - `fixed_script_forbidden_claims_absent`.

### 1.6 Escalation correctness summary is too weak

The summary `escalation_correctness` only compares expected and actual outcome class. It does not include reason enum correctness, timely escalation, or handover completeness. The latest run reports `0.5714` even though six cases fail exact escalation reason and six fail handover completeness.

Minimal fix:

- Split summary metrics:
  - `escalation_decision_accuracy`;
  - `escalation_reason_accuracy`;
  - `handover_completeness_rate`;
  - `escalation_timing_rate`.

### 1.7 Server eval is not deterministic

The eval config sets simulator temperature to `0.7`; the server sets chat/routing temperatures to `0.3`/`0.1`, and Kimi requests omit temperature. The two committed reruns show large swings in outcome and escalation correctness.

Minimal fix:

- Add a deterministic replay smoke mode using scripted user turns from `seed_messages`, not LLM-generated follow-up turns.
- Pin server-side routing/chat temperature for eval mode.
- Record model, provider, temperature, prompt version, and prompt hash in every eval result.

### 1.8 Session creation still performs resolution work

Phase 4 D14.6 explicitly asks `createSession()` to auto-run `ControlKernel.processMessage()` on form descriptions. The latest findings recommend the opposite because session creation timeouts previously broke `cs_interactive_066`.

This is a design contradiction across docs and implementation.

Minimal fix:

- Decide the product contract: session creation should either be cheap/static, or it is allowed to perform resolution and must have latency/error gates.
- My recommendation: make session creation cheap; move auto-resolution to the first normal bot turn.
- Add a hard eval case that fails if session creation calls LLM/KB or exceeds the session-create latency budget.

## 2. Missing Eval Cases

The smoke set does test some production-relevant behavior: routing across common UCs, FAQ vs fixed-script handling, payment dispute, safety/fraud, technical intake, and several bad-case/drift examples. It is not enough to validate production readiness.

Missing production-critical cases:

1. Explicit "talk to a human" from first turn and mid-conversation, with hard `user_requested_escalation`.
2. Session creation latency and no-heavy-work-on-create.
3. Tool sequence and negative tool access for each UC.
4. UC-H/UC-J/UC-K case creation with `case_id` in handover.
5. Handover quality, not just presence of required fields.
6. Same-thread continuity / Omni-Channel transfer contract.
7. Offline vs business-hours escalation messages.
8. Tool timeout, backend error, and partial-result fallback behavior.
9. Source citation support quality, including irrelevant source IDs.
10. KB retrieval miss vs weak retrieval vs wrong-UC-filtered retrieval.
11. Phone-contact/product-contact-option policy.
12. Login email vs contact email vs messaging email policy.
13. Seller payment guidance and safe-payment disclaimers.
14. Delivery logistics OOS vs Delivery+refund dispute vs Delivery scam/fraud.
15. PII minimization beyond regex leakage: account status summaries, email/phone repetition, evidence upload guidance.
16. Human-only tool promise cases: "I removed the ad", "I refunded you", "I emailed you", "I banned the seller".
17. Reproducibility cases: scripted deterministic smoke alongside LLM-simulated smoke.

## 3. Weak Rubric Dimensions

### 3.1 Source citation is presence-based

`source_ids` presence is scored, but source relevance/support is not. A hallucinated or irrelevant article ID can satisfy the L1 citation gate.

### 3.2 Tool behavior is mostly advisory or unused

The Phase 5 design includes `tool_sequence_match`, but smoke does not configure it. Production-critical tool behavior can be wrong while the case still passes if UC/outcome/handover happen to align.

### 3.3 Policy compliance is undercounted

Summary `policy_compliance_rate` only looks for `no_critical_policy_violation` and `no_pii_leakage` failure tags. It ignores wrong containment, wrong escalation reason, handover failure, unsafe payment/fraud handling, and human-only tool promises unless those exact hard checks fire.

### 3.4 L3 can reward procedural non-answers

Groundedness can be high when the bot makes no factual claims, even if the user needed a resolvable FAQ answer. This is why over-escalation can look semantically clean while failing the actual customer task.

### 3.5 No direct customer-effort score

The rubric does not explicitly score whether the user had to repeat themselves, whether the bot used form context, whether a next step was clear, or whether the bot prematurely dumped the user to a human.

### 3.6 Fixed-script quality is not measured

The docs define detailed fixed-script templates, but the current hard check only prevents knowledge retrieval for fixed-script UCs. It does not assert template semantics or required safe phrasing.

### 3.7 Interactive metrics are still advisory

Phase 5 says interactive task success and stall rate are advisory in V1. Given the current architecture, that is too weak. Current smoke has a stall and contract violation; both should be release blockers for this agent class.

## 4. Agent Design Problems

1. The LLM still controls exact escalation reason in places where exact enum correctness is a hard gate.
2. Runtime-owned side effects are still partly described as things the LLM should call or order.
3. `create_case_controlled` is runtime-only in the tool spec, but intake prompts/plans still instruct tool ordering around it.
4. The routing prompt fix is not enough for high-impact disambiguation; hard deterministic rules are needed for account/login, payment dispute, trust/safety, and hard OOS exceptions.
5. `UseCaseRouter` checks handover-only topics before semantic dispute/safety overrides, so Delivery+refund is forced OOS.
6. `ControlKernel` maps exceeded budgets to generic `turn_budget_exhausted`, masking more specific root causes.
7. The server can auto-answer during session creation, which mixes initialization, routing, retrieval, and response generation into one latency-sensitive API call.
8. Prompt/design docs conflict on whether auto-search during session creation is desirable.
9. Eval and production runtime use different temperature controls, so smoke results are not reproducible enough for regression attribution.

## 5. Tool-Use Risks

1. Wrong UC changes tool access. `cs_interactive_066` becoming UC-K moves a phone/contact-option FAQ into technical intake behavior.
2. UC-filtered retrieval can hide the right article after a wrong route.
3. Human-only tools are blocked as calls, but verbal promises around refunds, emails, moderation actions, and account changes need stronger checks.
4. Runtime-only case creation must not depend on model tool-call ordering.
5. `record_outcome` is expected but not scored in smoke; analytics can silently degrade.
6. `request_handover` is agent-visible and accepts many reason enums; a server-side resolver should validate or override unsafe choices.
7. Tool errors and timeouts need customer-visible fallback checks. A stall after "I'm looking into this" is already present in the latest run.
8. Committing `server/target/classes` makes it harder to know whether source resources or generated resources are authoritative.

## 6. Customer Service Policy Gaps

1. Human escalation availability is not hard-tested, despite BRD requiring access to a human at any point.
2. Payment/refund/dispute boundaries are still ambiguous across UC-F, UC-I, Delivery OOS, and UC-J scam/fraud.
3. Trust and safety urgency must override generic intake completion. `cs_interactive_038` shows the current opposite.
4. Phone contact option policy is not codified clearly enough for UC-E/UC-B vs UC-K.
5. Email/contact/login/messaging identity needs a canonical answer path; several UC-C/UC-D cases expose this.
6. Seller payment guidance needs safe, grounded policy language that avoids promising payment protection.
7. Delivery exceptions need explicit policy: plain courier logistics can be OOS, but delivery-related refund disputes and scams should route to UC-I/UC-J.
8. Offline handover messaging and business-hours routing are specified but not tested.
9. PII handling around evidence, email addresses, phone numbers, and fraud reports needs more than regex-based leak detection.

## 7. Recommended Minimal Fixes

### Fix A: Make the eval result contract consistent

- Fix per-case `status` serialization to include the `composite >= 0.7` threshold.
- Add top-level counts for `ERROR`, `TIMEOUT`, `CONTRACT_VIOLATION`, `STALL`, and `session_create_failed`.
- Make any non-zero count in those buckets fail smoke.

### Fix B: Add regression tests for the citation-gate change

Minimum tests:

- short factual uncited answer fails;
- long clarifying question passes;
- progress/acknowledgement without facts passes;
- progress phrase plus factual claim fails;
- prior-turn KB retrieval followed by uncited answer fails;
- factual answer with relevant source IDs passes.

### Fix C: Activate production-critical smoke checks

- Add `tool_sequence_match` to every smoke case with `expected_tool_sequence`.
- Add `case_id_present` to UC-H/J/K cases.
- Add `turn_efficiency` and `issue_preservation` where drift is expected.
- Add `user_requested_escalation` cases and configure the hard check.
- Replace `fixed_script_adherence` alias with real fixed-script checks.

### Fix D: Make server-side control deterministic before prompt work

Implement a narrow resolver service with unit tests:

- input: active UC, risk, user-requested flag, safety/fraud signals, payment dispute signals, budget state, intake completion;
- output: canonical `escalation_reason`;
- tested precedence: `trust_safety_required` over intake-complete, payment dispute over Delivery OOS, user-requested over distress, clarification exhaustion over generic turn budget.

### Fix E: Fix handover emission once, centrally

- Every `ESCALATE` terminal path must produce one complete handover record.
- Required payload fields should match Phase 3 section 3.6.2.
- UC-H/J/K must include `case_id`.
- Add integration tests for FAQ escalation, hard OOS, payment dispute, trust/safety, and technical intake.

### Fix F: Resolve the session-creation contradiction

- Decide whether session creation may call LLM/KB.
- Recommended minimal path: remove auto-search from `createSession()` and perform resolution on first user turn.
- Add a smoke case or unit test proving session creation is fast and tool-free.

### Fix G: Separate deterministic smoke from LLM-simulated smoke

- Add a deterministic mode that replays `seed_messages` exactly.
- Keep the LLM simulator as exploratory/adversarial testing, not the only smoke signal.
- Pin server eval temperatures and record prompt hashes.

### Fix H: Correct root-cause reporting

- Generate `docs/10-handoff.md` case tables from the latest result JSON.
- Include actual UC/outcome/reason from JSON.
- Keep hypotheses separate from observed failure tags.
- For contract violations, include raw trace/session payload or a link to it.

### Fix I: Defer KB authoring until runtime gates are stable

KB content still matters for phone contact, email/contact distinction, seller payment, and delivery/refund policy. But authoring those articles before routing, handover, and tool-sequence gates are stable will produce noisy signal. The next useful KB work is retrieval-probe-driven, not broad article creation.

## Priority Order

1. Fix eval contract/status and add hard top-level gates.
2. Add tests for the source-citation heuristic.
3. Enable smoke scoring for tool sequence, case ID, user-requested escalation, and real fixed-script adherence.
4. Implement deterministic server-side escalation reason resolver.
5. Guarantee complete handover on every escalation path.
6. Fix Delivery+refund/fraud semantic routing before hard OOS.
7. Make session creation cheap or explicitly gate its latency/tool behavior.
8. Rerun deterministic smoke and then LLM-sim smoke.
9. Only then proceed to targeted KB content updates and residual prompt tuning.

## Verification Notes

I attempted to run the targeted eval-interactive pytest subset referenced in `docs/10-handoff.md`, but `python -m pytest ...` exited with code `-1` and produced no output in this environment. I did not edit source code. This file is the only intended change from this review.
