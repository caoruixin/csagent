## Sprint Review Decision
decision: pass
blocking_count: 0
summary: Sprint 28 passes substantive review. The landed commit `ca55d8e` ships Option B only: a writer-side eval-harness enrichment in `eval_interactive/eval_interactive/batch/executor.py` plus `eval_interactive/tests/test_executor_per_turn_trace_enrichment.py`, with no Java, prompt, eval-spec, foundational/governance, Tier-0, deadline/model config, or Sprint 23-27 landed-code edits. The new `per_turn_trace` field is strictly additive, carries only the authorized per-turn `tool_calls`, `phase_plan`, and `projection` axes while preserving Sprint 25 `llm_calls[]`, and the remaining out-of-scope working-tree files are packaging/pre-existing artifacts outside the reviewed Sprint 28 commit.

Review scope:

- Reviewed commit: `ca55d8e` (`sprint 28: per-case trace dump surfaces into results.json`).
- Parent reviewed for Sprint 28 substance: `8a7703a` (Sprint 27 close). The older branch-ahead docs commits for Sprints 26/27 were treated as prior close/packaging history, not Sprint 28 implementation substance.
- Changed paths in the reviewed commit: `docs/10-handoff.md`, `docs/sprints/sprint-028-handoff.md`, `eval_interactive/eval_interactive/batch/executor.py`, `eval_interactive/tests/test_executor_per_turn_trace_enrichment.py`.
- Dirty/untracked working-tree paths observed but not part of the reviewed commit: `csagent_system_design_review.md`, `server/src/main/resources/prompts/system_prompt.txt`, `csagent-solution-_20260514.md`, `docs/sprint_objective.md`, `compact/sprint-028-*.md`.

Findings:

- None.

Anti-hardcode kernel:

- Verdict: `approve` — exemption applies because this is pure `infra` / eval-harness writer-side serialization. No semantic surface, Java semantic decision, prompt branch, keyword/regex/enum/per-UC matrix, tool/capability boundary, PII/safety-floor code, or grounding-floor code is changed.

Hard-fence verification:

- Option B verified: `executor.py` reads already-collected `trace_data.turns[]`; no new bot endpoint, HTTP client method, Java persistence, Flyway migration, or DB schema was added.
- Forbidden paths stayed untouched in the Sprint 28 commit: `server/src/main/resources/prompts/system_prompt.txt`, `AlreadyCalledPromptConsumptionTest.java`, `BotSession.consecutiveDeadlineCount`, Sprint 25 `agent_client.py get_llm_calls`, `LlmSyntheticBaselineTest.java`, eval spec/case family files, `docs/current/*`, foundational docs, Tier-0 policy, deadline/model/retry config.
- Opportunistic field expansion check passed: each populated `per_turn_trace[]` record has exactly `phase_plan`, `projection`, and `tool_calls`; `LlmCallEvents` remains the pre-existing Sprint 25 `case_results[].llm_calls[]` axis.
- Sprint 28 premise re-verification is present in handoff §1.2/§1.6/§3, including the minor V2 line-number drift note for `bot_turns.tool_calls`.

Schema and reproducibility checks:

- Backwards-compat key delta re-run: comparing `eval_interactive/results/20260514-111724/results.json` to `eval_interactive/results/20260514-181257/results.json` reports `added=['per_turn_trace']` and `removed=[]` for `case_results[0]`.
- Sprint 25 `llm_calls[]` shape preserved: every non-empty `llm_calls[]` row in the Sprint 28 smoke has the same 13 keys (`callType`, `completionTokens`, `createdAt`, `errorMessage`, `id`, `latencyMs`, `model`, `promptTokens`, `requestSummary`, `responseSummary`, `sessionId`, `success`, `turnIndex`). Two cases have empty `llm_calls` because their run path produced no persisted LLM-call rows; the field itself remains present as an array on every case result, matching the Sprint 25 placeholder contract.
- Re-ran handoff recipes against `eval_interactive/results/20260514-181257/results.json`: Recipe A returns `search_knowledge` / `resolve_article`; Recipe B returns phase `RESOLVE`, use case `UC-C`, and the expected allowed-tool list; Recipe C returns the 29 projection keys; Recipe D returns `39`; Recipe E returns 14 cases with counts `[0,1,1,2,3,2,2,2,2,2,3,1,2,0]`.
- Cross-checked quantitative claims: chat-call aggregation returns `n=54 p50=3848 p95=9736 max=11101 mean=4949`; elapsed means return `26139.428571428572`, `28846.64285714286`, and `26437.64285714286` for the Sprint 25, Sprint 26, and Sprint 28 result files respectively.

Validation run:

- `cd eval_interactive && uv run --extra dev pytest -q tests/test_executor_per_turn_trace_enrichment.py` -> `7 passed`.
- `cd eval_interactive && uv run --extra dev pytest` -> `306 passed, 3 failed`; the three failures are the pre-existing case-spec/fixture guard failures named in the handoff, and the new Sprint 28 test file passes.
- `mvn -q -pl server test` -> `Tests run: 902, Failures: 1, Errors: 0, Skipped: 2`; the failure is the pre-existing `SystemPromptUserRequestedTiebreakerTest` tied to the unauthored working-tree prompt modification, and Sprint 28 touched no Java.

Deferred / non-blocking notes:

- Handoff §7 already names valid follow-ons (`phase_before` / `phase_after`, explicit `turn_index`, broader trace surfaces). Per the deferral rule, these should stay out of this review and can be routed by the deliver agent if desired.
- Packaging note: because the out-of-scope prompt/doc artifacts are not part of `ca55d8e`, this review is a substantive `pass`, not `out_of_scope_review`.
