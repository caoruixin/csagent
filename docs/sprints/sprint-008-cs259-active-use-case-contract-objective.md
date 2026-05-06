# Sprint Objective

Date: 2026-05-07

## Sprint name

ResolveArticle Contract and MAX_STEPS Trace Honesty Closure Sprint 8.2

## Goal

Close the current-runtime FAQ RESOLVE failure shape exposed by trace `6f24c6ab-3799-46b9-9d42-e3b6a17c01a8`.

The trace shows a real schema/tool implementation drift:

- `search_knowledge` returns FAQ hits with `source_id`.
- The projected `resolve_article` tool schema tells the LLM to call `resolve_article` with `source_id`.
- The LLM correctly calls `resolve_article({ "source_id": ... })`.
- `ResolveArticleTool` reads `article_id` instead.
- The tool returns `Parameter 'article_id' is required`.
- The LLM retries the same schema-valid call until `maxToolSteps`.
- `MAX_STEPS` is mapped to `ESCALATE`.
- `AgentRunResult.maxSteps(...)` drops `lastLlmRawResponse`, so `bot_turns.llm_raw_response` is persisted as `NULL`.
- The Trace UI renders this as `LLM Raw Response = —` / “No LLM call for this turn,” even though LLM calls did occur.

Sprint 8.2 is a single-action closure sprint. It must not reopen Eval Governance, FAQ corpus, CaseSpec, judge calibration, routing, search threshold tuning, broad TraceViewer redesign, or deferred cs015 / cs066 / cs176 work.

## Baseline / context

Sprint 8 closed with:

- Codex decision: pass
- `blocking_count: 0`
- targeted cs259 and two clean Sprint 8 smoke runs committed `active_use_case=UC-F`
- `CONTRACT_VIOLATION:active_use_case=0`
- K0 accepted for the narrow cs259 reasoned-but-missing-UC path

Sprint 8.1 closed the prior production UX / LLM budget / DISCOVER phase-boundary issues:

- pre-filled form submit no longer blocks on LLM work
- budget-aware retry is bounded
- no-real-LLM-work failures are not masked as normal UC fallback
- successful `classify_use_case` in DISCOVER transitions to RESOLVE via the §M3 phase boundary
- stale trace `99141e8e-c064-4455-8ed4-f8653858c75d` was confirmed as pre-fix runtime evidence and does not reopen runtime scope

The new trace `6f24c6ab-3799-46b9-9d42-e3b6a17c01a8` is different:

- §M3 DISCOVER → RESOLVE phase boundary works.
- `tool_schemas` are filtered to the current `PhasePlan.allowedTools`.
- Real LLM calls occur.
- The failure happens inside RESOLVE because `resolve_article` cannot be called with the parameter name advertised to the LLM.

## Implement exactly this 1 action

### M0. Close trace `6f24c6ab` FAQ RESOLVE failure shape

M0 has two tightly scoped subfixes:

- M0a. P1 runtime contract: align `resolve_article` schema and implementation on `source_id`.
- M0b. P2 observability honesty: preserve `lastLlmRawResponse` on `MAX_STEPS` without changing behaviour.

These are one closure action because they address the same trace failure shape: FAQ RESOLVE fails, terminates via `MAX_STEPS`, and the UI incorrectly appears to show no LLM response.

## M0a. Align `resolve_article` schema and implementation

Required behaviour:

- `resolve_article` must accept `source_id` as the canonical argument.
- `search_knowledge.hits[*].source_id` must be directly usable as the input to `resolve_article`.
- The projected tool schema, tool description, grounding instruction, and runtime implementation must agree on `source_id`.
- `article_id` may be accepted as a legacy alias, but it must not be the canonical schema.
- If neither `source_id` nor `article_id` is provided, return a clear tool error that names `source_id` as the required canonical argument.
- Do not change FAQ corpus content.
- Do not change CaseSpecs.
- Do not change expected outcomes.
- Do not change judge behaviour.
- Do not change routing taxonomy.
- Do not change the S1 FAQ-grounded-resolve policy except to make `resolve_article` callable as advertised.
- Do not tune `search_knowledge` score thresholds in this sprint.

Acceptance condition:

- Re-running the ad-visibility probe no longer fails with:
  `Parameter 'article_id' is required`.
- UC-A FAQ RESOLVE can execute:

  `search_knowledge -> resolve_article`

  using the `source_id` returned by search.
- The bot should either:
  - produce a grounded answer with citation and `record_outcome`, or
  - escalate only after a real no-answer / unresolved-article condition.
- The trace must not show repeated `resolve_article` parameter errors.
- If escalation remains, it must be classified as article / corpus / answerability / retrieval-threshold residual, not schema/tool drift.

## M0b. Preserve `lastLlmRawResponse` on `MAX_STEPS`

Required behaviour:

- `AgentRunResult.maxSteps(...)` should preserve `lastLlmRawResponse` when there was a prior successful LLM response in the loop.
- Add a `maxSteps` overload or equivalent minimal change that accepts `lastLlmRawResponse`.
- Update the `AgentRunLoopImpl` `MAX_STEPS` return path to pass through the current `lastLlmRawResponse`.
- Do not change the terminal outcome.
- Do not change `PhaseEvaluator` `MAX_STEPS` mapping.
- Do not change retry / deadline / routing / tool policy behaviour.
- Do not change the UI beyond what is strictly necessary, unless no UI change is required after persistence is fixed.
- This is an observability honesty fix, not a behaviour change.

Acceptance condition:

- A `MAX_STEPS` run after successful LLM responses persists a non-null `llm_raw_response`.
- The Trace UI no longer displays `LLM Raw Response = —` solely because `MAX_STEPS` dropped the last raw response.
- If no LLM call truly occurred, the trace may still show no raw response.
- Existing LLM call logs and tool events remain unchanged.

## Target trace

Primary reproduction shape:

- trace: `6f24c6ab-3799-46b9-9d42-e3b6a17c01a8`
- form:
  - `ad_id=22222`
  - `email=xxx@xx.com`
  - `first_name=xxx`
  - `description="where is my ad"`
  - `topic_subject="Ad Support"`
- user message:
  - `hi why can't I find my advert`

Observed before fix:

- DISCOVER commits UC-A.
- Same-turn replan enters RESOLVE.
- `search_knowledge` returns relevant hits with `source_id`.
- LLM calls `resolve_article({ "source_id": ... })`.
- Tool errors with `Parameter 'article_id' is required`.
- Loop reaches `MAX_STEPS`.
- Runtime escalates with “I'm having difficulty resolving this. Let me connect you with a specialist.”
- `llm_raw_response` is persisted as null.
- UI shows `LLM Raw Response = —`.

Expected after fix:

- `resolve_article({ "source_id": ... })` succeeds.
- No repeated `article_id` parameter errors.
- `MAX_STEPS`, if it still occurs for another reason, preserves the last raw LLM response.
- Any remaining escalation is no longer caused by schema/tool contract drift.

## Regression guards

- Sprint 8 K0 cs259 active-use-case contract remains green.
- `CONTRACT_VIOLATION:active_use_case` remains 0 on targeted guards.
- `L1:escalation_reason_consistency` remains 0.
- Sprint 6 G2 S1 FAQ-grounded-resolve guard remains green:
  - viable FAQ hits require `resolve_article` before faq-miss handover.
- cs014 remains UC-C.
- cs066 remains UC-K.
- cs095 remains UC-A / not UC-K / not UC-FP.
- cs002 remains UC-C + `user_distress`.
- cs029 remains UC-D + `user_requested`.
- cs176 explicit-human-help → `user_requested` focused regression remains green.
- Sprint 7 I2 intake-state persistence remains green.
- Sprint 8.1 §M3 DISCOVER successful classification still transitions to RESOLVE.
- Sprint 8.1 honest failure / retry budget contracts remain green.

## Tests required

Focused tests:

- `resolve_article` accepts `source_id`.
- `resolve_article` accepts `article_id` as a legacy alias, if alias is implemented.
- missing `source_id` / `article_id` returns a clear error naming `source_id`.
- a `search_knowledge` hit `source_id` can be passed directly into `resolve_article`.
- UC-A FAQ flow can execute `search_knowledge -> resolve_article` without `Parameter 'article_id' is required`.
- `MAX_STEPS` after a successful LLM response preserves `lastLlmRawResponse`.
- `MAX_STEPS` terminal outcome remains `MAX_STEPS`.
- `PhaseEvaluator` `MAX_STEPS` behaviour remains unchanged.

Regression tests to keep green:

- `AgentRunLoopS1FaqGroundedResolveGuardTest`
- Sprint 8 cs259 K0 tests
- Sprint 8.1 DISCOVER phase-boundary tests
- Sprint 7 intake-state persistence tests
- cs176 explicit-human-help test
- ReadTimeout no-retry tests
- full `mvn -pl server test`

Eval tests:

- Run `python -m pytest -p no:capture eval_interactive/tests/` only if eval-side Python files are touched.
- Otherwise state that no eval-side files changed and pytest was not required.

Manual / local probe:

- Re-run the trace `6f24c6ab` shape:
  - `ad_id=22222`
  - `email=xxx@xx.com`
  - `first_name=xxx`
  - `description="where is my ad"`
  - `topic_subject="Ad Support"`
  - message: `hi why can't I find my advert`
- Confirm:
  - no `Parameter 'article_id' is required`
  - `resolve_article` accepts `source_id`
  - if escalation remains, classify it as article / corpus / answerability / threshold residual, not schema/tool drift
  - if `MAX_STEPS` occurs, `llm_raw_response` is not null when a prior successful LLM response exists

## Do not implement

- FAQ corpus expansion
- Product policy changes
- CaseSpec changes
- CaseSpec overrides
- judge calibration
- Eval Governance docs
- cs015 moderation cue follow-up
- cs066 / cs038 stall detector calibration
- cs176 UC-I drift fix
- S3 no-prior-search guard
- S5 Tier-2 runtime guard
- broad routing rewrite
- search threshold tuning
- answer_miss / faq_miss semantic redesign
- tool-deadline guard
- bypass-DISCOVER redesign
- full async polling architecture
- SSE
- websocket
- background job queue
- broad TraceViewer redesign
- llm_call_log dashboard
- per-tool latency dashboard

## Success metrics

Primary:

- `resolve_article` source-id contract is aligned.
- Trace `6f24c6ab` no longer fails with `Parameter 'article_id' is required`.
- UC-A FAQ RESOLVE can proceed from `search_knowledge` hit to `resolve_article`.
- `MAX_STEPS` preserves `lastLlmRawResponse` without changing runtime behaviour.
- No new P0/P1 regression.
- No unrelated scope is introduced.

Secondary:

- If the ad-visibility probe still escalates, the remaining failure is clearly classified as:
  - FAQ corpus / answerability,
  - retrieval threshold / answer_miss policy,
  - article content issue,
  - or another explicitly documented residual.
- Smoke pass rate is not the target for this sprint.
- Diagnostic truthfulness is more important than pass-rate preservation.

## Review rule

Codex must review only M0.

Codex should not request FAQ corpus, CaseSpec, judge calibration, Eval Governance, routing changes, search threshold tuning, broad observability, tool-deadline guards, bypass-DISCOVER redesign, or deferred cs015 / cs066 / cs176 work unless M0 directly regresses them.

Codex pass condition:

- `decision=pass`
- `blocking_count=0`
- `resolve_article` accepts canonical `source_id`
- `search_knowledge.hits[*].source_id` can flow directly into `resolve_article`
- missing-parameter error references canonical `source_id`
- `MAX_STEPS` preserves `lastLlmRawResponse` when available
- trace `6f24c6ab` schema/tool failure shape is closed
- no unrelated runtime / prompt / CaseSpec / judge / eval-governance scope introduced