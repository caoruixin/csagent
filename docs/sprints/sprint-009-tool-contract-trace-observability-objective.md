# Sprint Objective

Date: 2026-05-07

## Sprint name

Tool Contract and Trace Observability Fidelity Sprint 9

## Goal

Fix the narrow runtime-contract and trace-observability issues exposed by the post-Sprint-8 manual trace:

- `record_outcome` schema/tool drift
- `request_handover` schema/tool drift
- terminal tool failure being treated as successful terminal state
- blank tool result panels in the trace UI / tree log

Sprint 9 must stay narrowly scoped to terminal tool contract fidelity and trace observability. It must not reopen routing, FAQ corpus, product policy, judge calibration, CaseSpec churn, or broad eval governance work.

## Implement exactly these 3 actions

### O0. Align terminal tool contracts: `record_outcome` and `request_handover`

Required behaviour:

- `record_outcome` must accept the schema-advertised argument:
  - `outcome_class`
  - lowercase values: `resolve`, `escalate`, `abandon`
- `record_outcome` may keep legacy compatibility:
  - `outcome`
  - uppercase values: `RESOLVED`, `ESCALATED`, `ABANDONED`
- Internally normalize to the canonical persisted outcome enum.
- Missing-parameter errors should name `outcome_class`.
- A successful `record_outcome(outcome_class="resolve")` must write a `session_outcomes` row.
- The result payload should expose a stable normalized result, including `outcome_class`.

For `request_handover`:

- Align projected schema and tool implementation.
- Either:
  - add `summary` to projected schema and prompt requirements, OR
  - make `RequestHandoverTool` derive a safe fallback summary when summary is missing.
- Preferred: do both defensively:
  - schema exposes / recommends `summary`
  - tool can derive fallback summary from session / current user message / escalation reason
- `request_handover(escalation_reason="tool_scope_blocked")` should not fail solely because summary is missing.
- Preserve canonical escalation reason validation.
- Do not alter the 23-value escalation reason enum.

### O1. Terminal-state honesty for failed terminal tools

Required behaviour:

- A failed `record_outcome` dispatch must not by itself trigger `RESOLVE -> CONFIRM`.
- A failed `request_handover` dispatch must not be treated as a successful terminal escalation.
- The loop should surface the tool error back into accumulated tool results so the LLM can retry within bounded `maxToolSteps`.
- If retry is not possible because max steps are exhausted, record an explicit tool failure / service degraded outcome rather than silently marking success.
- Keep behaviour unchanged when terminal tool dispatch succeeds.
- Preserve existing successful paths:
  - FAQ resolve → `record_outcome` success → CONFIRM
  - valid handover → ESCALATE
  - Sprint 6 S1 FAQ-grounded-resolve guard
  - Sprint 7 intake-complete guard
  - Sprint 8 cs259 active-use-case contract

### O2. Trace observability fidelity: bounded sanitized tool result display

Required behaviour:

- Persist a bounded, sanitized `result_data` or `result_summary` field for each tool call in `bot_turns.tool_calls`.
- Include result data for successful tools where safe.
- Include useful diagnostic data for failed tools:
  - `success=false`
  - `error_message`
  - result summary such as `{ "error": "..." }`
- Bound payload size.
- Avoid secret / PII leakage.
- For large objects like `resolve_article`, prefer summary fields:
  - `source_id`
  - `title`
  - `source_url` if already safe / public
  - short excerpt or description
- Update TraceViewer mapping so it handles backend shape:
  - `tool_name`
  - `arguments`
  - `success`
  - `latency_ms`
  - `error_message`
  - `result_data` / `result_summary`
- UI should no longer show blank Result panels for tool rows that have success/error payloads.
- Keep this additive and backwards compatible.

## Do not implement

- advert-link generator
- new listing URL tool
- FAQ corpus changes
- rerank threshold tuning
- rerank fallback semantic redesign
- judge calibration
- CaseSpec changes
- eval YAML changes
- routing changes
- S3 no-prior-search guard
- S5 Tier-2 runtime guard
- broad TraceViewer redesign
- broad Eval Governance docs
- anchor / exploration / promotion hard-gate expansion

## Deferred notes

Add deferred action-bank entries, but do not implement:

- Product decision: whether the bot may provide a direct advert URL.
- Rerank diagnostic honesty: distinguish `rerank_llm` score from `rerank_fallback` score.
- FAQ corpus / answerability gaps such as cs192 / cs259.
- Judge volatility.

## Regression guards

- Sprint 6 G0 ReadTimeout closure remains intact:
  - 120s create-session timeout widen only
  - no ReadTimeout retry
- Sprint 6 G1 explicit human-help → `user_requested` remains green.
- Sprint 6 G2 S1 FAQ-grounded-resolve remains green.
- Sprint 7 I0 / I1 / I2 tests remain green.
- Sprint 7.1 partial intake persistence remains green.
- Sprint 8 K0 cs259 active-use-case contract remains green.
- `L1:escalation_reason_consistency` remains 0.
- `CONTRACT_VIOLATION:active_use_case` remains 0 on clean smoke if smoke is run.

## Success metrics

Primary:

- `record_outcome(outcome_class="resolve")` succeeds and persists a session outcome.
- `request_handover(tool_scope_blocked)` does not fail solely because `summary` is absent.
- Failed terminal tools no longer silently advance the phase as though they succeeded.
- Trace UI shows non-empty result/error information for tool calls.
- No unrelated runtime / routing / eval / CaseSpec work is introduced.

Secondary:

- Manual trace shape from `a7e20173` shows:
  - search result visible
  - resolve_article result visible
  - record_outcome success visible
  - follow-up `tool_scope_blocked` behaviour unchanged unless handover summary fallback is now visible

## Review rule

Codex must review only O0 / O1 / O2 and regression guards.

Codex should not request advert-link tools, FAQ corpus changes, rerank tuning, judge calibration, CaseSpec churn, routing rewrite, or broad Eval Governance work unless Sprint 9 directly regresses them.