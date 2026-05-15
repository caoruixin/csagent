# Codex Findings - Targeted Runtime Behavior Sprint 1

## Blocking Sprint Failures

None found.

The canonical sprint eval `eval_interactive/results/20260504-100538/results.json` shows the targeted blockers moved in the intended direction: `L1:escalation_reason_consistency` 1 -> 0, `L2:handover_completeness` 2 -> 0, `L2:correct_uc` 6 -> 2, and `cs_interactive_066` passes with `active_use_case=UC-K` and `escalation_reason=intake_complete_for_uc_k`. Targeted server tests for `EscalationReasonResolver`, `HandoverPayloadAssembler`, and `UseCaseRouterUcKRegressionTest` pass.

## Non-Blocking Notes

- severity: P2
  target case or test: `cs_interactive_066`
  blocks current sprint goal: no
  finding: The UC-K routing objective is met for the canonical technical-regression case. The case now expects UC-K, routes to UC-K, escalates for `intake_complete_for_uc_k`, and passes. The remaining `L2:tool_sequence_match` detail is weak (`actual=['request_handover', 'create_case_controlled']`) but does not block the sprint objective because the requested scope was routing plus payload assembly, not full tool ordering.
  exact minimal fix: No current-sprint fix. If UC-K tool order becomes a next-sprint gate, record runtime `create_case_controlled` before the persisted `request_handover` entry and either make `get_customer_context` mandatory for UC-K technical intake or remove it from the expected sequence.

- severity: P2
  target case or test: `cs_interactive_029`
  blocks current sprint goal: no
  finding: `turn_budget_exhausted` is no longer shown as the semantic escalation reason in the latest canonical result; the previous reason-consistency blocker is gone. The case now fails earlier with `CONTRACT_VIOLATION:active_use_case` because the UNKNOWN-topic soft-OOS path has no committed UC before escalation. That is the deferred soft-OOS interaction already tracked in `docs/action_bank.md`.
  exact minimal fix: No current-sprint fix. For the deferred item, either commit a default UC before immediate user-requested escalation on UNKNOWN-topic sessions or defer forced escalation until after one DISCOVER turn.

- severity: P2
  target case or test: `eval_interactive/results/20260504-100538/results.json`
  blocks current sprint goal: no
  finding: The latest eval reduced the targeted blocker counts, but introduced or exposed non-target blockers: `L1:no_forbidden_tools` is now 2 and `CONTRACT_VIOLATION:active_use_case` is now 1. These do not invalidate A1/A2/A3, but the handoff should not imply the full smoke suite is stable.
  exact minimal fix: Keep the baseline comparison anchored to targeted tags only; document the new non-target failures as next-sprint routing/soft-OOS work, not as A1/A2/A3 regressions.

## Regression Risks

- severity: P1
  target case or test: AgentRunLoop `request_handover` reason consistency
  blocks current sprint goal: no
  finding: The resolver centralizes session reason writes, but `ControlKernel.recordRunResult` persists existing AgentRunLoop `request_handover` tool-call arguments as emitted by the LLM. If the LLM emits a non-canonical or lower-priority reason, the session and handover payload can be resolved/canonical while the persisted tool call remains raw, re-opening `escalation_reason_consistency` failures on that path.
  exact minimal fix: In `recordRunResult`, before serializing `toolCallsList`, rewrite every `request_handover.arguments.escalation_reason` to the resolved `session.getEscalationReason()`. Add an integration test where the LLM emits `user_requested_escalation` and the trace, session state, and handover payload all persist `user_requested`.

- severity: P2
  target case or test: `eval_interactive/tests/test_hard_checks.py`
  blocks current sprint goal: no
  finding: The new global `escalation_reason_consistency` hard check has no direct unit fixture. Server tests cover resolver precedence, but the eval gate itself is only exercised indirectly by smoke results.
  exact minimal fix: Add hard-check tests for: all three surfaces matching, tool-call/session mismatch, handover/session mismatch, and escalated trace with no reason on any surface.

- severity: P2
  target case or test: `handover_completeness`
  blocks current sprint goal: no
  finding: The server-side assembler adds `user_issue`, `unresolved_question`, `partial_answer_or_blocker`, `status_checks_performed`, and `knowledge_tools_invoked`, but the eval `handover_completeness` check still only requires `session_id`, `primary_use_case`, `summary`, `escalation_reason`, and `total_bot_turns`. A future regression could drop the Sprint A3 fields while still scoring 1.0.
  exact minimal fix: Extend the handover completeness gate to require the new Sprint A3 fields, require the summary to mention content tokens from the issue, and assert source/status evidence fields are present as empty arrays or populated lists as applicable.

- severity: P2
  target case or test: `cs_interactive_095`
  blocks current sprint goal: no
  finding: The deterministic UC-K helper does not match the cs_095 description, but the canonical eval still ends `cs_interactive_095` as `active_use_case=UC-K` through the broader LLM route. This does not block cs_066, but it is a routing regression risk around the same UC-K surface.
  exact minimal fix: Add a full-router regression test, not just the helper test, proving the cs_095 form description does not end on UC-K. Stabilize with a deterministic email/app/account-sync rule to UC-A/UC-D before the LLM route.

- severity: P2
  target case or test: enum sync tests
  blocks current sprint goal: no
  finding: The new enum sync test compares YAML, eval schema, and `PhaseEvaluator`, but the sprint added another canonical enum in `EscalationReasonResolver` and there is also a duplicate set in `ToolDispatcher`. Future enum drift could leave the resolver or dispatcher out of sync while the Python test remains green.
  exact minimal fix: Extend `test_escalation_enum_sync.py` to parse `EscalationReasonResolver.CANONICAL_REASONS` and `ToolDispatcher.CANONICAL_ESCALATION_REASONS`, or replace the duplicate runtime sets with one shared source.

## Recommended Next Sprint Actions

- severity: P1
  target case or test: `cs_interactive_002`, `cs_interactive_014`
  blocks current sprint goal: no
  finding: Distress cases still escalate with `faq_miss_threshold_exceeded` because the runtime does not set `user_distress`; the resolver will preserve it once set.
  exact minimal fix: Add a pre-budget distress/frustration detector that stamps `user_distress` for repeated complaint, ALL-CAPS frustration, and explicit "you are not helping" patterns. Add regression tests proving `user_distress` beats FAQ and turn-budget reasons.

- severity: P1
  target case or test: `cs_interactive_095`, `cs_interactive_001`, `cs_interactive_002`, `cs_interactive_014`
  blocks current sprint goal: no
  finding: Messaging/account/email routing remains unstable and can still drift into UC-K or unrelated FAQ UCs.
  exact minimal fix: Add deterministic pre-LLM routing bias for email/account-sync/message-notification phrases to UC-C or UC-D, with negative UC-K regression coverage for cs_095.

- severity: P2
  target case or test: `cs_interactive_029`
  blocks current sprint goal: no
  finding: UNKNOWN-topic soft-OOS plus immediate user-requested escalation still produces a missing `active_use_case` contract violation.
  exact minimal fix: Commit an active UC before handover for UNKNOWN-topic sessions when the user request clearly identifies the issue family, or allow one DISCOVER/classify turn before honoring the immediate escalation.

- severity: P2
  target case or test: `handover_completeness`, `escalation_reason_consistency`
  blocks current sprint goal: no
  finding: The runtime-side fixes are stronger than the eval-side regression fixtures.
  exact minimal fix: Add focused eval fixtures for the new reason-consistency gate and the expanded Sprint A3 handover payload contract before treating these as durable gates.
