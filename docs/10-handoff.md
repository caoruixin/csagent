# Current Handoff

Date: 2026-05-07
Branch: `design-v1-without-human-review`

## 1. Current phase

Current phase:
Sprint 8.2 — ResolveArticle Contract and MAX_STEPS Trace Honesty
Closure (in flight; awaiting Codex review).

Latest closed sprint:
Sprint 8 — Targeted cs259 Active-Use-Case Contract Hardening.

Latest Codex decision (pre-Sprint-8.2):
- decision: pass
- blocking_count: 0

## 2. Sprint 8.2 root cause

Trace `6f24c6ab-3799-46b9-9d42-e3b6a17c01a8` (form
`ad_id=22222 / description="where is my ad" / topic_subject="Ad Support"`,
user message `"hi why can't I find my advert"`) exposed a single
schema/tool drift inside RESOLVE that has been latent since the
runtime first projected `resolve_article`:

- `ContextProjectionBuilder` advertises
  `resolve_article.arguments_schema = {"source_id": string, required:["source_id"]}`
  and the description tells the LLM to call it with the `source_id`
  it received from `search_knowledge.hits[*].source_id`.
- `SearchKnowledgeTool` projects `hits[*].source_id` (canonical).
- The LLM correctly emitted
  `resolve_article({"source_id": "<faq>"})`.
- `ResolveArticleTool.execute` read `parameters.get("article_id")`
  and returned `Parameter 'article_id' is required` because that
  parameter was never present.
- The LLM kept retrying the same schema-valid call until the
  RESOLVE plan exhausted `maxToolSteps`, the loop returned
  `MAX_STEPS`, and `PhaseEvaluator` mapped that to ESCALATE.
- `AgentRunResult.maxSteps(...)` discarded the verbatim
  `lastLlmRawResponse`, so `bot_turns.llm_raw_response` was
  persisted as `NULL` and the Trace UI rendered the turn as
  "No LLM call for this turn" even though several real LLM calls
  occurred.

The §M3 DISCOVER → RESOLVE phase boundary, the plan-aware tool
schema filter, the S1 FAQ-grounded-resolve guard, the K0 cs259
contract hardening, and every Sprint 7 / 7.1 contract were all
behaving as designed; the failure was contained to the two surfaces
above.

## 3. Sprint 8.2 fixes

### M0a — `resolve_article` source_id alignment (P1 runtime contract)

`server/src/main/java/com/gumtree/csagent/service/tools/ResolveArticleTool.java`

- Canonical input is now `source_id`, matching the projected schema
  and `search_knowledge.hits[*].source_id`.
- `article_id` is accepted as a legacy alias; if both are supplied,
  `source_id` wins.
- Missing both arguments returns
  `Parameter 'source_id' is required` (no longer the misleading
  "article_id" error).
- Result payload now exposes both `source_id` and `article_id`
  (both backed by the same DB primary key) so callers that already
  read `article_id` keep working while the canonical name surfaces
  to the LLM.
- The FAQ corpus, CaseSpecs, judges, S1 policy,
  `search_knowledge` thresholds, and routing taxonomy are all
  unchanged. No `ContextProjectionBuilder` schema change was needed
  — the projection already advertised `source_id`.

### M0b — `lastLlmRawResponse` preservation on MAX_STEPS (P2 observability honesty)

`server/src/main/java/com/gumtree/csagent/model/AgentRunResult.java`
`server/src/main/java/com/gumtree/csagent/service/runtime/AgentRunLoopImpl.java`

- New 4-arg `AgentRunResult.maxSteps(llmEvents, toolEvents,
  lastProjection, lastLlmRawResponse)` overload. The 2-arg and
  3-arg overloads remain (back-compatible with existing callers
  and tests).
- `AgentRunLoopImpl.run` now passes the loop's running
  `lastLlmRawResponse` (already tracked per LLM call) into the
  MAX_STEPS factory on the loop-exhausted return path.
- `TerminalOutcome.MAX_STEPS` is unchanged.
- `PhaseEvaluator` MAX_STEPS mapping is unchanged.
- No retry / deadline / routing / tool policy behaviour changed.
- This is observability honesty only — the trace UI now sees the
  real terminating raw response instead of a `NULL`.

## 4. Files changed

Production:
- `server/src/main/java/com/gumtree/csagent/service/tools/ResolveArticleTool.java`
- `server/src/main/java/com/gumtree/csagent/model/AgentRunResult.java`
- `server/src/main/java/com/gumtree/csagent/service/runtime/AgentRunLoopImpl.java`

Tests:
- `server/src/test/java/com/gumtree/csagent/service/tools/ResolveArticleToolTest.java`
  (new — M0a)
- `server/src/test/java/com/gumtree/csagent/service/runtime/AgentRunLoopMaxStepsRawResponseTest.java`
  (new — M0a UC-A FAQ flow + M0b MAX_STEPS preservation)
- `server/src/test/java/com/gumtree/csagent/model/AgentRunResultTest.java`
  (extended — M0b factory overloads)

Docs:
- `docs/10-handoff.md` (this file; pre-Sprint-8.2 version
  archived to `docs/archive/current-docs/2026-05-07-pre-sprint8.2-10-handoff.md`)
- `docs/action_bank.md` (Sprint 8.2 row added; statuses unchanged
  for Sprint 9 governance backlog)

## 5. Tests run

- `mvn -pl server test` → **726 tests / 0 failures / 0 errors / 0 skipped**.
- Focused regression sweep (re-run before commit):
  - `ResolveArticleToolTest` (8 tests, all green) — canonical
    `source_id`, legacy `article_id` alias, source_id-wins-when-both,
    missing-param error names `source_id`, blank-source_id falls back
    to alias, search_knowledge hit source_id flows directly,
    unknown source_id error, `getName`.
  - `AgentRunLoopMaxStepsRawResponseTest` (2 tests, all green) —
    MAX_STEPS preserves last raw response after repeated tool errors;
    UC-A FAQ flow `search_knowledge → resolve_article(source_id=…)`
    completes with FINAL_ANSWER and no parameter errors.
  - `AgentRunResultTest` (8 tests, all green) — new M0b
    `maxSteps_preservesLastLlmRawResponseWhenSupplied` and
    `maxSteps_legacyOverloadStillReturnsNullRawResponse` plus all
    pre-existing factories.
  - `AgentRunLoopS1FaqGroundedResolveGuardTest` (12 tests, all
    green) — Sprint 6 §G2 guard intact under §M0a + §M0b.
- Sprint 8 K0 cs259 contract test, Sprint 8.1 §M3 DISCOVER
  phase-boundary test, Sprint 7 intake-state persistence tests,
  cs176 explicit-human-help → `user_requested` regression: all
  green inside the full `mvn -pl server test` run above.

No `eval_interactive/**` Python files were touched, so
`python -m pytest -p no:capture eval_interactive/tests/` was not
required.

## 6. Manual probe

Trace `6f24c6ab` shape was reproduced as a focused JUnit
integration test rather than a live-server probe (the trace
involves a real Kimi LLM call which is not deterministic enough to
assert on; the JUnit shape pins the exact contract change with no
LLM dependency):

`AgentRunLoopMaxStepsRawResponseTest.ucA_faq_flow_search_then_resolve_article_with_source_id_completes`

Form / message shape: `ad_id=22222`, `email=xxx@xx.com`,
`first_name=xxx`, `description="where is my ad"`,
`topic_subject="Ad Support"`, user message `"hi why can't I find
my advert"`.

Confirmed:

- No `Parameter 'article_id' is required` surfaces from the
  resolve_article dispatch path.
- `resolve_article` accepts `source_id` and returns a successful
  payload that the LLM cites in the final answer.
- The loop reaches `TerminalOutcome.FINAL_ANSWER` (not MAX_STEPS,
  not ESCALATE).
- The MAX_STEPS test (separate scenario) confirms that if the
  loop ever does exhaust on this UC for an unrelated reason
  (tool-error retry storm, etc.), `lastLlmRawResponse` survives
  on the result.

If a live re-probe of trace `6f24c6ab` still escalates after this
sprint, the residual must be classified as one of:

- FAQ corpus / answerability gap (no resolve-grade article for the
  ad-visibility intent),
- retrieval threshold / `answer_miss` policy residual,
- article content quality issue,

and explicitly NOT as `resolve_article` schema/tool drift.

## 7. Before / after

Before:

```
[discover] classify_use_case(UC-A)               -> committed
[resolve] search_knowledge("where is my ad")     -> hits[source_id=faq-…]
[resolve] resolve_article(source_id=faq-…)       -> ERROR Parameter 'article_id' is required
[resolve] resolve_article(source_id=faq-…)       -> ERROR Parameter 'article_id' is required
... (loop hits maxToolSteps)
TerminalOutcome.MAX_STEPS
AgentRunResult.lastLlmRawResponse = null
bot_turns.llm_raw_response       = NULL
TraceViewer: "No LLM call for this turn."
```

After:

```
[discover] classify_use_case(UC-A)               -> committed
[resolve] search_knowledge("where is my ad")     -> hits[source_id=faq-…]
[resolve] resolve_article(source_id=faq-…)       -> ok (article payload)
[resolve] (LLM grounds answer with [faq-…] citation)
TerminalOutcome.FINAL_ANSWER
```

If MAX_STEPS still occurs in some other shape:

```
TerminalOutcome.MAX_STEPS
AgentRunResult.lastLlmRawResponse = "<verbatim LLM content>"
bot_turns.llm_raw_response       = "<verbatim LLM content>"
TraceViewer: renders the actual final raw response
```

## 8. Residuals

- Live re-probe of trace `6f24c6ab` against a Kimi-backed deploy
  is recommended once the canonical post-Sprint-8.2 baseline run
  is captured. Any remaining escalation must be triaged into the
  FAQ corpus / answerability / retrieval-threshold residual
  buckets above, NOT runtime contract.
- Eval Governance backlog (cs015 / cs066 / cs176 deferrals,
  L3 judge volatility, FAQ corpus answerability) is unchanged.
  Sprint 9 (Eval Governance docs-only) remains the recommended
  next phase.

## 9. Sprint 8.2 objective

Met:

- `resolve_article` accepts the canonical `source_id` advertised
  by the projected schema and produced by `search_knowledge`.
- `article_id` accepted as a legacy alias.
- Missing-parameter error names `source_id`.
- UC-A FAQ flow `search_knowledge → resolve_article` runs without
  the parameter-name error.
- MAX_STEPS preserves `lastLlmRawResponse`; terminal outcome and
  PhaseEvaluator MAX_STEPS mapping unchanged.
- No Sprint 6 / 7 / 7.1 / 8 / 8.1 contract is regressed
  (full server suite green, 726/726).
- No FAQ corpus, CaseSpec, judge, routing, search threshold, or
  Eval Governance scope was opened.

## 10. Regression guards

Currently-active guards (all green in the full server run):

- `L1:escalation_reason_consistency` = 0
- `CONTRACT_VIOLATION:active_use_case` = 0
- cs014 remains UC-C
- cs066 remains UC-K
- cs095 remains not UC-K / not UC-FP
- cs002 remains UC-C + `user_distress`
- cs029 remains UC-D + `user_requested`
- cs176 explicit-human-help → `user_requested` focused regression
- Sprint 6 §G0 no ReadTimeout retry
- Sprint 6 §G2 FAQ-grounded-resolve guard
- Sprint 7 §I2 intake_state persistence + intake-complete guard
- Sprint 7.1 §J0 partial intake persistence
- Sprint 8 §K0 cs259 UC-F contract hardening
- Sprint 8.1 §M3 DISCOVER phase boundary

New Sprint 8.2 guards:

- `resolve_article` accepts canonical `source_id`
  (`ResolveArticleToolTest`).
- `resolve_article` accepts legacy `article_id` alias.
- Missing both surfaces a `source_id`-named error.
- UC-A FAQ flow `search_knowledge → resolve_article(source_id=…)`
  reaches FINAL_ANSWER without parameter errors
  (`AgentRunLoopMaxStepsRawResponseTest`).
- MAX_STEPS preserves `lastLlmRawResponse` after successful LLM
  calls (`AgentRunResultTest` + `AgentRunLoopMaxStepsRawResponseTest`).

## 11. Next recommended phase

Sprint 9 — Eval Governance and Release Gate Definition (docs-only)
remains the recommended next phase. Sprint 8.2 did not open or
defer any new runtime work.

Do not start another runtime sprint unless Sprint 9 triage finds a
new P0/P1 runtime blocker.

## 12. Current-doc maintenance rule

`docs/10-handoff.md`, `docs/codex-findings.md`,
`docs/sprint_objective.md`, and `docs/action_bank.md` are
overwrite-current-state files. Before replacing one of them:

1. archive the previous version under `docs/sprints/` if it
   belongs to a sprint closure, or
   `docs/archive/current-docs/` if it is an ad hoc transition;
2. then overwrite the working file;
3. keep only actionable current state in the working file.

Historical detail belongs in `docs/sprints/`,
`docs/archive/current-docs/`, `eval_interactive/results/`, and
`qa-reports/`.

## 13. Do not reopen

- broad full review
- broad routing rewrite
- judge calibration implementation
- CaseSpec churn
- anchor / exploration / promotion hard-gate expansion
- smoke 14/14 optimization
- S3 no-prior-search guard unless explicitly selected
- S5 Tier-2 runtime guard unless explicitly selected
- broad TraceViewer redesign
- llm_call_log dashboard / per-tool latency dashboard
- search threshold tuning / answer_miss / faq_miss semantic redesign
- tool-deadline guard / bypass-DISCOVER redesign
- runtime sprint unless a new P0/P1 runtime blocker is found
