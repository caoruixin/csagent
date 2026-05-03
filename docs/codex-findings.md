# Codex Findings - Latest Implementation Review

Date: 2026-05-03

Reviewed:

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
- `eval_interactive/results/20260503-081557/results.json`
- `eval_interactive/results/20260503-082110/results.json`
- `git diff HEAD~1..HEAD`

## Executive Summary

The latest update tightens the eval contract and removes one important tool-contract violation: `create_case_controlled` is no longer exposed as an LLM-callable tool. That is the right direction.

The implementation is still not production-close. The final smoke run, `20260503-082110`, remains `1/14` pass with mean composite `0.0563`. The only passing case, `cs_interactive_040`, still fails both `tool_sequence_match` and `case_id_present`. That proves the current rubric can pass a UC-K handover even when the runtime does not expose the required case ID and the observed tool sequence is only `request_handover`.

The biggest review correction is in `docs/10-handoff.md`: it rejects the Delivery semantic override as contradicting Phase 2, but Phase 2 section 2.11.4 and `fixed_script_library_v1.md` section 9.1 explicitly say handover-only topics should first classify the description and route Delivery+fraud to UC-J or Delivery+Pay-and-Ship refund to UC-I. The current `UseCaseRouter` checks handover-only topics before description classification, so `cs_interactive_036` is not just a case-spec problem; it exposes a production routing bug or, at minimum, a spec conflict that must be resolved before more code changes.

## Latest Eval State

`20260503-082110` summary:

| Metric | Value |
| --- | ---: |
| Total cases | 14 |
| Passed cases | 1 |
| Failed cases | 13 |
| Task success rate | 0.0714 |
| Mean composite | 0.0563 |
| Mean outcome | 0.5669 |
| Mean judge | 0.7619 |
| Escalation correctness | 0.7143 |
| Stall rate | 0.0 |
| Policy compliance rate | 1.0 |
| Mean turns | 1.79 |

Final-run failure pattern:

| Case | Expected | Observed | Primary review finding |
| --- | --- | --- | --- |
| `cs_interactive_001` | UC-C escalate | UC-F resolved | Wrong UC and wrong outcome; not an escalation-reason-only issue. |
| `cs_interactive_002` | UC-C escalate | UC-I escalated | Wrong UC; reason resolver alone cannot fix this. |
| `cs_interactive_004` | UC-D resolve | UC-D escalated | Correct UC but premature FAQ escalation. |
| `cs_interactive_011` | UC-D escalate `user_requested` | UC-D escalated `user_distress` | Spec expects explicit user request, but the seed messages show frustration, not a direct human request. |
| `cs_interactive_014` | UC-C escalate | UC-B escalated | Wrong UC plus reason mismatch. |
| `cs_interactive_015` | UC-FP resolve | UC-A resolved | Correct outcome, wrong UC. |
| `cs_interactive_029` | UC-C escalate | UC-B escalated | Wrong UC; reason mismatch is downstream. |
| `cs_interactive_036` | UC-I escalate | OOS escalated | Runtime hard-OOS routing conflicts with Phase 2 / fixed-script override language. |
| `cs_interactive_038` | UC-J escalate `trust_safety_required` | UC-J escalated `intake_complete_for_uc_j` | Correct UC/outcome, wrong reason precedence, missing case ID. |
| `cs_interactive_040` | UC-K escalate | UC-K escalated | Passes despite missing case ID and wrong tool sequence. |
| `cs_interactive_066` | UC-E escalate | UC-B escalated | Wrong UC, incomplete handover. |
| `cs_interactive_095` | UC-A resolve | UC-D escalated | Wrong UC and premature escalation. |
| `cs_interactive_192` | UC-B resolve | UC-B escalated | Correct UC, premature FAQ escalation. |
| `cs_interactive_259` | UC-F resolve | UC-C resolved | Wrong UC and uncited factual answer. |

The rerun immediately before this, `20260503-081557`, produced different active UCs and outcomes for multiple cases (`001`, `029`, `066`, `259`) even though the final update was primarily eval-spec scoring. That makes failure attribution unstable unless deterministic replay and model settings are pinned.

## Review Against The Requested Questions

### 1. Do the eval cases test production-critical behavior?

Partly. The smoke suite now covers tool sequence expectations for all 14 cases and case-ID expectations for UC-J/UC-K. That is a real improvement.

The suite still misses critical production behavior:

- no UC-G GDPR/privacy smoke case;
- no UC-H incorrect deletion appeal smoke case, despite UC-H being one of the highest-volume escalation UCs in Phase 1;
- no explicit "I want a human/person/agent" case, even though several specs expect `user_requested`;
- no hard test for business-hours vs offline handover messaging;
- no test for Omni-Channel transfer result, queue selection, or same-thread preservation;
- no tool-timeout, tool-error, retry, or partial payload tests;
- no session-create latency / cheap-session hard gate;
- no PII minimization checks for handover payloads;
- no duplicate-case policy test when a user already provides an existing case number, as in `cs_interactive_040`;
- no citation-support test proving the cited article actually supports the answer.

### 2. Can the rubric be gamed?

Yes.

- `tool_sequence_match` and `case_id_present` are advisory L2 checks. `cs_interactive_040` passes with `tool_sequence_match=0.25`, actual tool sequence `['request_handover']`, and `case_id_present=0.0`.
- Phase 5 says Case ID Linkage should be `100%` and UC-H/J/K tool sequence should be at least `90%`, but the composite gate does not enforce either.
- `tool_sequence_match` uses longest-common-subsequence partial credit. A bot can skip required tools and still get non-zero credit.
- `source_citation_present` is still heuristic. A short factual answer under 50 characters, a progress phrase with embedded factual claims, or a citation whose source does not support the answer can pass.
- `policy_compliance_rate` is `1.0` while task success is `1/14`, which means the policy metric is not measuring the failures that would matter to customers.
- L3 groundedness/relevance/tone can raise composite after mandatory gates pass, even when advisory production contracts are broken.

### 3. Are failures assigned to the right root cause?

Not consistently.

- Several failures in `docs/10-handoff.md` are attributed to escalation-reason selection, but the observed active UC is wrong. `cs_interactive_002`, `014`, and `029` need routing fixes before reason fixes are meaningful.
- `cs_interactive_036` is treated as a spec-correct hard-OOS path. That conflicts with Phase 2 section 2.11.4 and `fixed_script_library_v1.md` section 9.1, which require description-based matching before OOS fallback.
- `cs_interactive_011` expects `user_requested`, but the seed transcript does not contain an explicit request for a human. This should be either a spec override to `user_distress` or a new case with an explicit human request.
- `cs_interactive_038` is a reason-precedence problem, not a generic intake-complete failure. Fraud/safety signals should deterministically map to `trust_safety_required`.
- `cs_interactive_040` should not be counted as production-pass while missing the case ID. It is an eval-gate issue as well as a runtime propagation issue.

### 4. Are proposed design changes minimal and testable?

Some are.

Minimal and testable changes landed:

- per-case JSON `status` now matches `case_passed && composite >= 0.7`;
- `handover_completeness` auto-runs for expected escalate cases;
- `tool_sequence_match` is activated across smoke;
- `create_case_controlled` is removed from LLM-visible plan tools and prompt instructions.

Changes that are not yet sufficiently testable:

- `ControlKernel.mapBudgetToEscalationReason()` has no direct unit test.
- `PhaseEvaluator.resolveMaxStepsReason()` has no direct unit test. Its heuristic chooses `faq_miss_threshold_exceeded` whenever any `search_knowledge` tool event exists, before checking clarification count, so mixed search-plus-clarification loops can be assigned the wrong reason.
- Runtime-only case creation was removed from the LLM plan, but smoke specs still expect `create_case_controlled` in `expected_tool_sequence`. Either runtime-only tool events must be recorded in the trace sequence, or the CaseSpec should split LLM-visible sequence from runtime-side-effect sequence.
- The handoff claims all remaining Delivery behavior is spec-correct, but the docs disagree. That is not testable until the policy is normalized.

### 5. Are hard gates missing?

Yes.

Add hard gates for:

- `case_id_present` when expected UC is UC-H, UC-J, or UC-K and the case escalates;
- exact or thresholded `tool_sequence_match` for UC-H/J/K, UC-I, and FAQ-grounded answer cases;
- runtime-only side effects present in trace: case creation, handover, outcome recording;
- handover payload completeness for any actual escalation, not only expected escalations, for diagnostics and production safety;
- top-level zero tolerance for `ERROR`, `TIMEOUT`, `CONTRACT_VIOLATION`, `session_create_failed`, and `max_steps_exceeded` in smoke;
- source support, not just source presence;
- fixed-script template use and forbidden-claim absence for UC-G/H/I/J/K and OOS;
- deterministic replay variance: a case should not change UC/outcome across immediate reruns under the same code and seed.

### 6. Do implementation changes match the proposals?

Partially.

| Proposal area | Match | Review |
| --- | --- | --- |
| Eval status serialization | Yes | The executor fix is correct. |
| Handover completeness gate visibility | Partial | Auto-running the check is good; it still does not make all actual handovers safe. |
| Production-critical smoke checks | Partial | Checks were added, but key ones remain advisory and some cases are missing. |
| Runtime-only case creation | Partial | LLM exposure was fixed, but traces/specs still expect `create_case_controlled` in the normal tool sequence and case IDs are missing. |
| Escalation reason resolver | Partial | Budget and max-step mappings improved; wrong-UC cases and fraud/payment/user-request precedence remain unresolved. |
| Delivery/OOS routing | No | Current router checks OOS before description classification, contrary to Phase 2 and fixed-script notes. |
| Deterministic eval | No | Immediate reruns still produce materially different UC/outcome assignments. |
| Generated artifact hygiene | No | `server/target` reports and `.jar.original` are committed in the diff. |

## Correctness Bugs

1. `UseCaseRouter` returns OOS for handover-only topics before description classification. This contradicts Phase 2 section 2.11.4 and `fixed_script_library_v1.md` section 9.1, both of which say Delivery descriptions matching fraud or Pay-and-Ship refund/dispute should route to UC-J or UC-I before OOS fallback.

2. `cs_interactive_040` passes despite missing `case_id` in the handover payload. Phase 5 defines case linkage as a 100% target. This is a release-gate bug.

3. Smoke specs still encode `create_case_controlled` in `expected_tool_sequence` for UC-J/UC-K while the implementation correctly makes it runtime-only. The evaluator currently sees actual `['request_handover']` for `cs_interactive_038` and `040`; it cannot tell whether the runtime failed to create a case, failed to trace it, or the spec is checking the wrong sequence.

4. `resolveMaxStepsReason()` is a heuristic, not a deterministic resolver. It can mislabel mixed search/clarification failures and has no unit coverage in the latest diff.

5. `mapBudgetToEscalationReason()` has no direct tests. The mapping is small enough that a parameterized test should be required.

6. `cs_interactive_011` has an expected `user_requested` trigger without an explicit human-request seed message. That makes the eval punish a defensible `user_distress` classification.

7. The latest diff commits generated files under `server/target`, including surefire reports and `csagent-server-0.1.0-SNAPSHOT.jar.original`. This creates noisy review diffs and risks stale artifacts being mistaken for source.

## Missing Eval Cases

Add the following before treating smoke as production-representative:

- UC-G privacy/delete-account intake and handover;
- UC-H incorrect deletion appeal with runtime case creation and case ID in handover;
- explicit user-requested escalation, distinct from frustration;
- user frustration without explicit human request;
- Delivery + plain courier issue to OOS;
- Delivery + scam/fraud to UC-J;
- Delivery + Pay-and-Ship refund/payment dispute to UC-I, unless policy owners intentionally remove that override from Phase 2 and the fixed-script library;
- UC-J fraud report with required safety/fraud details collected;
- UC-K technical issue where the user already has a case number, to decide whether to create a new case or link the existing one;
- FAQ answer with correct citation, missing citation, wrong citation, and citation that does not support the answer;
- retrieval miss, weak hit, and wrong-UC filtered retrieval;
- tool timeout and retry for `search_knowledge`, `get_customer_context`, `request_handover`, and `create_case_controlled`;
- offline handover vs business-hours transfer behavior;
- no-human-only-tool-promise cases such as "delete my ad", "refund me", "ban the scammer", and "send me an email";
- PII minimization in handover payloads.

## Weak Rubric Dimensions

- `tool_sequence_match` should be a hard gate or near-hard gate for production-critical flows, not an advisory mean component.
- `case_id_present` should be mandatory for UC-H/J/K escalations.
- `handover_completeness` should be checked for actual escalations even when the expected outcome is resolve, so over-escalation still verifies operational safety.
- `source_citation_present` should be split into `citation_required`, `citation_present`, and `citation_supports_answer`.
- `fixed_script_adherence` should check the selected template family, required wording/variables, forbidden promises, and intake-field preservation. Today it is too close to "did not search knowledge".
- `policy_compliance_rate` should include critical customer-service failures: missing case ID, wrong queue/reason, unsupported claims, human-only promises, PII leakage, and premature escalation.
- `turn_efficiency` and `issue_preservation` are absent from smoke scoring.

## Agent Design Problems

1. Routing still depends too heavily on LLM classification. The Account Support, Delivery, payment, fraud, UC-FP vs UC-A, and UC-C messaging boundaries need deterministic pre-routing rules before the LLM is asked.

2. Escalation reason selection is still partly LLM-owned. Fraud, payment dispute, explicit human request, distress, appeal, and budget reasons should be resolved by a server-side precedence table after route selection.

3. Runtime side effects and LLM tool calls are not cleanly separated in traces. The design now correctly treats `create_case_controlled` as runtime-only, but the eval still looks for it in a tool sequence without evidence that runtime events are captured consistently.

4. The bot over-escalates resolvable FAQ cases (`cs_interactive_004`, `192`, `095`) instead of making a grounded answer attempt. This is a control-flow issue, not just a KB coverage issue.

5. Intake responses are too generic. `cs_interactive_038` and `040` end with "Let me connect you with a specialist" while failing tool/case checks; the customer-facing fixed-script contract is not visibly enforced.

6. Session creation may still perform heavy resolution work. Phase 4 authorizes this, but production needs explicit latency, timeout, fallback, and no-side-effect-on-create tests.

7. The eval/runtime model configuration is not deterministic enough for root-cause triage. `081557` and `082110` show materially different routing under near-identical implementation state.

## Tool-Use Risks

- Runtime-only `create_case_controlled` may execute without being trace-visible, making evaluation and incident review unreliable.
- `request_handover` payload can be complete enough for `handover_completeness` but still miss `case_id`, queue correctness, or existing-case linkage.
- `record_outcome` is expected in many specs but absent from actual tool sequences for passing/near-passing handovers.
- Wrong UC routing exposes or hides the wrong tools. For example, UC-J/UC-K should not search knowledge; FAQ cases should search before answering or escalating.
- The LCS tool-sequence scorer rewards partial compliance instead of enforcing required side-effect order.
- Citation checks verify presence, not support.
- Tool failures and timeouts are not exercised as release blockers.
- Committed `server/target` artifacts can mask what code actually changed.

## Customer Service Policy Gaps

- Delivery policy is unresolved. Current docs say both "handover-only OOS" and "route Delivery fraud/refund descriptions to UC-J/UC-I". Pick one canonical policy and update Phase 2, fixed scripts, smoke specs, and router together.
- Existing case number handling is undefined. `cs_interactive_040` includes "Case number 00394953 is already open"; the policy should say whether the bot links that case, creates a new case, or hands over without creating another.
- Phone contact, email/app sync, and seller payment coverage remain weak as customer-facing policy topics.
- Distress vs explicit human request is blurred. The eval should not expect `user_requested` unless the user actually asks for a human.
- Trust & Safety handover should include minimum safety/fraud intake and customer-facing safety wording, not only generic transfer language.
- Payment inquiry vs payment dispute needs deterministic boundaries and examples.
- Business-hours/offline messaging and expected follow-up channel are not enforced in smoke.
- Human-only commitments need stronger policy tests: refunds, bans, ad deletion/restoration, and outbound email promises.

## Recommended Minimal Fixes

1. Make the eval gate honest first:
   - promote `case_id_present` to mandatory for UC-H/J/K escalations;
   - promote `tool_sequence_match` to mandatory for UC-H/J/K, UC-I, and FAQ-grounded answer flows, or require a high threshold such as `>=0.9`;
   - fail smoke on any `ERROR`, `TIMEOUT`, `CONTRACT_VIOLATION`, `session_create_failed`, or `max_steps_exceeded`;
   - add top-level counts for these hard-fail buckets.

2. Split tool sequencing into two dimensions:
   - `llm_tool_sequence_match` for LLM-visible tools;
   - `runtime_side_effect_sequence_match` for `create_case_controlled`, handover persistence, case ID propagation, and outcome recording.

3. Fix Delivery/OOS routing before more T3 content work:
   - classify description against known UC overrides before OOS fallback for handover-only topics;
   - add tests for Delivery plain OOS, Delivery fraud to UC-J, and Delivery Pay-and-Ship refund/dispute to UC-I;
   - if product wants terminal hard-OOS instead, update Phase 2 and fixed scripts first.

4. Add a deterministic `EscalationReasonResolver` after routing:
   - inputs: active UC, latest user message signals, explicit human request flag, fraud/payment/appeal signals, clarification count, FAQ miss count, intake completeness, runtime errors;
   - outputs: canonical `request_handover.escalation_reason`;
   - tests: reason precedence for `user_requested`, `trust_safety_required`, `payment_dispute_detected`, `appeal_requires_human`, `clarification_budget_exhausted`, `faq_miss_threshold_exceeded`, `incomplete_intake`, and `turn_budget_exhausted`.

5. Fix case ID propagation:
   - assert UC-J/UC-K handover payload includes `case_id`;
   - decide existing-case behavior for user-provided case numbers;
   - ensure the trace collector can see runtime-created cases.

6. Add resolve-before-escalate guardrails for FAQ UCs:
   - do not escalate UC-B/D/F/A FAQ cases on first miss without a grounded retry, clarifying question, or explicit reason;
   - add tests for `cs_interactive_004`, `192`, `259`, and `095` style questions.

7. Pin deterministic eval mode:
   - fixed model settings for routing and chat during smoke;
   - prompt/version hash in results;
   - rerun-stability check for UC, outcome, and escalation reason.

8. Clean repository hygiene:
   - remove generated `server/target` files from version control;
   - add `target/` and `*.jar.original` to ignore rules unless there is a deliberate artifact-publishing reason.

These fixes are small enough to land in isolated patches and should move the smoke suite from "mostly diagnostic" to a release-relevant regression gate.
