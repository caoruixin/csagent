# Sprint Objective

Date: 2026-05-06

## Sprint name

Non-Blocking Chat Entry and Budgeted LLM Reliability Sprint 8.1

## Goal

Fix the production UX and runtime-honesty failure discovered after Sprint 8:

1. Pre-filled form submit must not block the user for 30s+ while waiting for LLM work.
2. The user should enter the chatbox quickly after deterministic session creation and form-context persistence.
3. The next user message must still carry the pre-filled form context into the bot turn.
4. Bot-side LLM calls should use budget-aware fast retry with at most one retry.
5. If LLM attempts fail or exceed the budget, the system should return an honest slow/unavailable response instead of a normal business escalation.
6. Sprint 8 K0 missing-UC fallback must not mask no-real-LLM-work timeout/deadline paths as normal UC-A / UC-F escalations.

This is a production UX + LLM reliability hotfix. It must not implement full async polling, SSE, websocket, background job queue, broad frontend rewrite, FAQ corpus changes, CaseSpec changes, judge calibration, or Eval Governance docs.

## Baseline / context

Sprint 8 closed with:
- Codex decision: pass
- blocking_count: 0
- targeted cs259 and two clean Sprint 8 smoke runs committed `active_use_case=UC-F`
- `CONTRACT_VIOLATION:active_use_case=0`
- K0 accepted for the narrow cs259 reasoned-but-missing-UC path

Post-Sprint-8 local production trace exposed a new issue:

- User submitted a pre-filled form and originally waited 30s+ before seeing timeout / exception.
- After an ad-hoc UX change, the user could enter chatbox, but a later chat turn:
  - waited about 12s
  - produced `DISCOVER -> ESCALATE`
  - stamped `active_use_case=UC-A`
  - showed no useful LLM reasoning and no real tool calls
  - hit `max_tool_steps=2`
  - used K0 fallback to fill the missing UC
- This masks LLM timeout/deadline/no-work failure as normal business escalation and makes eval / trace evidence less trustworthy.

## Implement exactly these 3 actions

### M0. Non-blocking pre-filled form submit and fast chatbox entry

Required behaviour:

- Pre-filled form submit should return quickly after deterministic session creation.
- Persist the form context before returning:
  - `topic_subject`
  - `description`
  - `ad_id`
  - `email`
  - `first_name`
  - any available listing/customer context that can be fetched without LLM blocking
- Do not block initial chatbox entry on LLM analysis.
- Do not perform a synchronous, user-blocking LLM first-turn analysis in the form-submit path.
- The frontend/chat entry response should support a default acknowledgement, for example:

  `We’ve received your request and loaded your submitted details. You can add more information while I analyse it.`

- The next user message must still see the persisted form context in the projected context, even if the user only says "hi".

Acceptance condition:

- Form submit returns a session/chatbox-ready response without waiting for LLM.
- A follow-up message such as `hi` or `hi, why I can't see my ad` sees the original submitted context:
  - topic: `Ad Support`
  - description: `where is my ad?`
  - ad_id present
  - listing/customer context present if deterministically available
- No full async polling/SSE/background job architecture is introduced in this sprint.

### M1. Budget-aware LLM fast retry with at most one retry

Required behaviour:

- Define a per-user-turn LLM deadline budget.
- Each LLM attempt must use a per-attempt timeout smaller than the total deadline.
- Retry at most once.
- Retry only transient model / infra failures:
  - read timeout
  - connect timeout
  - transient transport error
  - HTTP 429
  - HTTP 5xx
- Do not retry:
  - HTTP 401 / 403
  - deterministic 4xx
  - valid model response that chooses escalation
  - no FAQ hit
  - unclear user intent
  - tool validation failure
  - policy / safety refusal
- Before retrying, check remaining budget.
- Do not start a retry if remaining budget is below the minimum attempt budget.
- If a fallback provider exists, it may be used only as the one retry and only inside the same remaining budget.
- Record telemetry for each LLM attempt:
  - attempt_count
  - provider
  - elapsed_ms
  - failure_class
  - retry_decision
  - remaining_budget_ms
- If both attempts fail or the budget is exhausted, return `deadline_exceeded` / `llm_unavailable` / equivalent honest failure signal, not a normal business escalation.

Acceptance condition:

- One timeout can trigger one fast retry if budget remains.
- Two full 6s waits must not occur inside a nominal 10s user-turn budget.
- 401 / 403 remain non-retryable.
- No third attempt occurs.
- Retry failure produces a user-visible slow/unavailable response and a trace/log signal that clearly separates model availability failure from semantic no-answer.

### M2. Honest failure handling and K0 missing-UC fallback gating

Required behaviour:

- `LlmInvocationService.invokeChat` or equivalent must not convert deadline/timeout exhaustion into a synthetic normal assistant response.
- Synthetic `SAFE_ESCALATION_RESPONSE` must not count as real LLM reasoning evidence.
- AgentRunLoop must distinguish:
  - `deadline_exceeded`
  - `llm_unavailable`
  - `max_steps_exceeded`
  - normal escalation
- `ControlKernel.applyMissingUseCaseFallback` / K0 fallback must only fire when there is real evidence that the bot actually reasoned or executed relevant tool work.
- K0 may fire for the legitimate Sprint 8 cs259 path:
  - payment-sale-proceeds FAQ shape
  - LLM/tool path produced meaningful evidence
  - handover/escalation is semantically formed
  - active_use_case is missing only as a contract slot
- K0 must NOT fire when:
  - LLM did not produce real output
  - only synthetic safe escalation exists
  - terminal outcome is `ERROR`, `DEADLINE_EXCEEDED`, or `LLM_UNAVAILABLE`
  - terminal outcome is `MAX_STEPS` caused by synthetic safe escalation / invalid tool response
  - there are no real tool events
  - escalation reason is `service_degraded`, `system_failure`, `agent_error`, or deadline-related
- K0 must not stamp UC-A / UC-F / any UC on no-real-LLM-work timeout paths.

Acceptance condition:

- The localhost ad-visibility timeout shape does not receive fake UC-A fallback.
- The legitimate Sprint 8 cs259 reasoned-but-missing-UC path still receives UC-F.
- Timeout/deadline/no-work paths remain diagnostically visible.
- Raw smoke pass rate may fall if previously masked failures become visible; diagnostic truthfulness is more important.

## Regression guards

- Sprint 8 cs259 targeted contract path remains green.
- K0 still fixes reasoned-but-missing-UC cs259-like payment-sale-proceeds path.
- K0 does not hide no-real-LLM-work failure paths.
- `L1:escalation_reason_consistency` remains 0.
- cs014 remains UC-C.
- cs066 remains UC-K.
- cs095 remains UC-A / not UC-K / not UC-FP.
- cs002 remains UC-C + `user_distress`.
- cs029 remains UC-D + `user_requested`.
- cs176 explicit-human-help → `user_requested` focused regression remains green.
- Sprint 6 G0 remains intact:
  - eval-side create-session timeout widen only
  - no eval-client ReadTimeout retry
- Sprint 6 G2 S1 remains intact:
  - viable FAQ hits require `resolve_article` before faq-miss handover.

## Do not implement

- Full async polling architecture
- SSE
- websocket
- background job queue
- broad frontend rewrite
- Eval Governance docs
- FAQ corpus changes
- Product policy changes
- CaseSpec changes
- CaseSpec overrides
- judge calibration
- broad routing taxonomy rewrite
- cs015 follow-up
- cs066 / cs038 stall detector calibration
- cs176 UC-I drift fix
- S3 no-prior-search guard
- S5 Tier-2 runtime guard
- anchor / exploration / promotion hard-gate expansion

## Success metrics

Primary:

- Pre-filled form submit no longer blocks on LLM.
- User can enter chatbox quickly with form context persisted.
- Follow-up user turns see the persisted form context.
- LLM calls use budget-aware retry with max one retry.
- LLM failure after retry gives honest slow/unavailable response.
- No-real-LLM-work timeout path is not converted into normal UC-A escalation.
- Legitimate Sprint 8 cs259 K0 path remains green.
- No new P0/P1 regression.

Secondary:

- Smoke pass rate may stay flat or drop if previously hidden failures become visible.
- Trace truthfulness and production UX are more important than pass-rate preservation in this sprint.

## Review rule

Codex must review only M0 / M1 / M2.

Codex should not request async polling, SSE, websocket, Eval Governance, FAQ corpus, judge calibration, CaseSpec churn, broad routing rewrite, or deferred cs015/cs066/cs176 work unless Sprint 8.1 directly regresses them.