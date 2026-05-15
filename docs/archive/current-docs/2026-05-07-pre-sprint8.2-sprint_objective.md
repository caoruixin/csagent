# Sprint Objective

Date: 2026-05-06

## Sprint name

Non-Blocking Chat Entry, Budgeted LLM Reliability, and DISCOVER Phase Boundary Sprint 8.1

## Goal

Fix the production UX, LLM reliability, runtime honesty, and DISCOVER phase-boundary bugs discovered after Sprint 8.

Sprint 8.1 has exactly four hotfix actions:

- M0. Non-blocking pre-filled form submit and fast chatbox entry.
- M1. Budget-aware LLM fast retry with at most one retry.
- M2. Honest failure handling and K0 missing-UC fallback gating.
- M3. DISCOVER successful classification phase-boundary replan.

This sprint must preserve the valid Sprint 8 K0 behaviour for cs259-like “LLM reasoned but skipped classify_use_case” paths, while preventing runtime failures and phase-boundary bugs from being masked as normal business escalations.

This is a production UX + runtime reliability hotfix. It must not implement full async polling, SSE, websocket, background job queues, Eval Governance docs, FAQ corpus changes, CaseSpec changes, judge calibration, broad routing rewrite, or deferred cs015/cs066/cs176 work.

## Baseline / context

Sprint 8 closed with:
- Codex decision: pass
- blocking_count: 0
- targeted cs259 and two clean Sprint 8 smoke runs committed `active_use_case=UC-F`
- `CONTRACT_VIOLATION:active_use_case=0`
- K0 accepted for the narrow cs259 reasoned-but-missing-UC path

Post-Sprint-8 local production traces exposed two additional issues:

### Issue A — submit / LLM latency UX

- User submitted a pre-filled form and originally waited 30s+ before seeing timeout / exception.
- This indicates the pre-filled form submit / session-create path can block user chatbox entry on LLM work.

### Issue B — timeout / no-real-work masking

- After a local workaround, the user could enter chatbox, but a later chat turn:
  - waited about 12s
  - produced `DISCOVER -> ESCALATE`
  - stamped `active_use_case=UC-A`
  - showed no useful LLM reasoning and no real tool calls
  - hit `max_tool_steps=2`
  - used K0 fallback to fill the missing UC
- This masks LLM timeout/deadline/no-work failure as normal business escalation.

### Issue C — DISCOVER phase-boundary bug

A second local trace showed the LLM and tools actually worked, but the runtime still escalated:

- DISCOVER plan allowed tools: `search_knowledge`, `classify_use_case`.
- DISCOVER `maxToolSteps=2`.
- LLM followed the instruction and called:
  1. `search_knowledge`
  2. `classify_use_case`
- `search_knowledge` returned hits.
- `classify_use_case` successfully committed `active_use_case=UC-A`.
- AgentRunLoop kept running the original DISCOVER plan after the UC mutation.
- It did not re-plan DISCOVER -> RESOLVE.
- The loop fell through to `MAX_STEPS`.
- `PhaseEvaluator.interpretRunResult` mapped `MAX_STEPS` to `ESCALATE`.
- `resolveMaxStepsReason` stamped `faq_miss_threshold_exceeded` even though search had hits and classification succeeded.
- The user saw chat end even though DISCOVER actually succeeded.

This is a real runtime phase-boundary bug. Do not fix it only by raising `maxToolSteps` or prompt-tuning.

## Implement exactly these 4 actions

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

- The next user message must still see the persisted form context in the projected context, even if the user only says `hi`.

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
  - successful phase-boundary classification
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

### M3. DISCOVER successful classification phase-boundary replan

Required behaviour:

- Treat successful `classify_use_case` in DISCOVER as a deterministic phase boundary.
- Add a non-escalating AgentRunResult terminal outcome, for example:
  - `USE_CASE_IDENTIFIED`
  - `DISCOVER_CLASSIFIED`
  - `CLASSIFICATION_COMMITTED`
- In `AgentRunLoopImpl`, when the current plan is DISCOVER and `classify_use_case` successfully commits an active UC, return this outcome immediately instead of continuing the original DISCOVER plan to `maxToolSteps`.
- In `PhaseEvaluator.interpretRunResult`, map DISCOVER + successful-classification outcome to:
  - next phase: `RESOLVE`
  - transition reason: `uc_identified`
  - no escalation reason
  - no synthetic `request_handover`
- In `ControlKernel`, allow exactly one bounded same-turn replan for DISCOVER -> RESOLVE using the same user message.
- The second run must use a fresh RESOLVE plan for the newly committed active UC.
- The second run must share the same turn deadline / retry budget; it must not create an unbounded extra LLM budget.
- If there is not enough budget to run RESOLVE in the same HTTP turn, return a non-terminal transitional response and leave the session in RESOLVE for the next user turn.
- Record one bot turn containing the combined tool events from both phases where same-turn replan runs, so the trace preserves:
  - `search_knowledge`
  - `classify_use_case`
  - any RESOLVE tools
- Harden projection/tool-schema behaviour:
  - when a PhasePlan is present, projected `tool_schemas` must be filtered to `phase_plan.allowedTools`
  - do not project UC-specific policy tools that the active PhasePlan would reject
- Do not fix this by only raising DISCOVER `maxToolSteps`.
- Do not allow `request_handover` in DISCOVER just to escape the bug.
- Do not use prompt-only tightening as the primary fix.

Acceptance condition:

- DISCOVER search + classify success transitions to RESOLVE, not ESCALATE.
- The Ad Support shape:
  - form topic: `Ad Support`
  - description: `where is my ad?`
  - message: `where is my advert? I can't see it`
  commits UC-A and transitions to RESOLVE rather than ending chat.
- `faq_miss_threshold_exceeded` is not stamped merely because maxToolSteps was reached after search hits and classify success.
- `request_handover` is not synthesized for successful DISCOVER classification.
- Same-turn replan happens at most once.
- If same-turn RESOLVE runs, it uses the fresh RESOLVE plan and filtered allowed tools.
- If budget is insufficient, the session transitions to RESOLVE and returns a non-terminal “I’m looking into it” style response rather than escalating.

## Regression guards

- Sprint 8 cs259 targeted contract path remains green.
- K0 still fixes reasoned-but-missing-UC cs259-like payment-sale-proceeds path.
- K0 does not hide no-real-LLM-work failure paths.
- DISCOVER successful classification does not become escalation.
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
- Sprint 7 I0 weak-candidate cue remains narrow:
  - search-before-classify cue applies only when candidate_use_cases are empty/weak AND form context is empty/UNKNOWN AND the message is FAQ/payment-sale-proceeds shaped.

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
- prompt-only fix for M3
- maxToolSteps-only fix for M3

## Success metrics

Primary:

- Pre-filled form submit no longer blocks on LLM.
- User can enter chatbox quickly with form context persisted.
- Follow-up user turns see the persisted form context.
- LLM calls use budget-aware retry with max one retry.
- LLM failure after retry gives honest slow/unavailable response.
- No-real-LLM-work timeout path is not converted into normal UC-A escalation.
- DISCOVER successful classification transitions to RESOLVE instead of escalating.
- Legitimate Sprint 8 cs259 K0 path remains green.
- No new P0/P1 regression.

Secondary:

- Smoke pass rate may stay flat or drop if previously hidden failures become visible.
- Trace truthfulness and production UX are more important than pass-rate preservation in this sprint.

## Review rule

Codex must review only M0 / M1 / M2 / M3.

Codex should not request async polling, SSE, websocket, Eval Governance, FAQ corpus, judge calibration, CaseSpec churn, broad routing rewrite, or deferred cs015/cs066/cs176 work unless Sprint 8.1 directly regresses them.
