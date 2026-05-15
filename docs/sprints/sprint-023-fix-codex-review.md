## Sprint Review Decision (Sprint 23 fix re-review)
decision: pass
blocking_count: 0
summary: PASS branch taken: the fix iteration keeps the parent Track A prompt-teaching bundle and supplies real-LLM target rerun evidence. Finding 1 closes because the Track A and Track B matrices now carry the required six observable columns per target, with unrecoverable trace fields explicitly marked `unavailable: <cause>`; Findings 2 and 3 close because `eval_interactive/results/20260514-080835/results.json` shows cs_040's duplicate `search_knowledge` shape reversed and the Java test is labelled supporting coverage only. The §4.1 anti-hardcode verdict is `approve`; the PASS action_bank disposition phrase `landed with target-reversal evidence` is verified. Packaging rollforward artefacts in the working tree (`compact/sprint-023-*.md`, `docs/sprint_objective.md`) are noted as deliver-agent context, not scope drift; the committed fix diff itself only changes the Sprint 23 handoff and a supporting-coverage test comment.

## Finding 1 — Missing Root-Cause Matrix
closure_verdict: closed
file: `docs/sprints/sprint-023-handoff.md:355`

Track A now has an augmented six-column matrix at `docs/sprints/sprint-023-handoff.md:355`: `turn`, raw LLM tool calls, dispatched tool calls, projection contents, `accumulated_tool_results`, and argument-hash / same-args status. It covers all six Track A targets (`cs_interactive_002`, `cs_interactive_014`, `cs_interactive_015`, `cs_interactive_040`, `cs_interactive_259`, and the manual probe). The manual-probe same-args status is populated from the failure brief; the unrecoverable smoke-run fields are explicitly marked `unavailable:` with a cause or a direct cross-reference to the source limitation stated in the matrix preamble and first row.

Track B now has the same six-column coverage at `docs/sprints/sprint-023-handoff.md:577` for all seven Track B targets (`cs_interactive_002`, `cs_interactive_014`, `cs_interactive_040`, `cs_interactive_015`, `cs_interactive_038`, `cs_interactive_259`, `cs_interactive_176`). Placeholder turns are populated from `transcript[]`; tool/projection/result fields that the 2026-05-10 snapshot does not carry are marked `unavailable:` with the stated source limitation, and the same-args column is explicitly marked not applicable because Track B's duplicate surface is the PhaseEvaluator placeholder text path, not a tool dispatch.

No case has a required column silently omitted. The matrix still records that the original `results.json` snapshot lacks per-turn ToolEvent payloads, projection-slot state, and `accumulated_tool_results`, but that limitation is now explicit rather than hidden.

## Finding 2 — Inferred-vs-Conclusive Evidence For Track A Bundle
closure_verdict: closed under PASS branch
file: `eval_interactive/results/20260514-080835/results.json`

The PASS branch criteria hold. A new target rerun result exists at `eval_interactive/results/20260514-080835/results.json`, and I read it directly. For `cs_interactive_040`, the original 2026-05-10 sequence was `['search_knowledge', 'classify_use_case', 'search_knowledge', 'resolve_article', 'record_outcome', 'search_knowledge']`; the post-fix rerun sequence is `['classify_use_case', 'search_knowledge', 'resolve_article', 'record_outcome']`. That post-fix sequence has only one `search_knowledge` call and does not contain the duplicate shape documented in the parent handoff.

The handoff's `## Fix iteration` section names this as the PASS branch at `docs/sprints/sprint-023-handoff.md:1212`, cites the rerun path at `docs/sprints/sprint-023-handoff.md:1271`, and records the per-target reversal verdict at `docs/sprints/sprint-023-handoff.md:1297`. The cs_014 rerun at `eval_interactive/results/20260514-081022/results.json` is honestly recorded as partial because its underlying duplicate-call shape persists, but the fix-iteration gate only requires reversal on at least one Track A target; cs_040 satisfies that bar.

## Finding 3 — Regression Evidence Does Not Reverse Target Shape
closure_verdict: closed under PASS branch
file: `docs/sprints/sprint-023-handoff.md:1297`

The primary causal evidence is now the real-LLM target rerun, not the Java prompt-text test. The handoff's per-target reversal table at `docs/sprints/sprint-023-handoff.md:1297` identifies cs_040 as full reversal of the duplicate-call shape, and `docs/sprints/sprint-023-handoff.md:1322` updates §13.1 from PARTIAL to PASS with the `eval_interactive/results/20260514-080835/results.json` path.

The existing Java test is labelled supporting coverage only in the handoff at `docs/sprints/sprint-023-handoff.md:1357`, and the test file has a top-of-file comment at `server/src/test/java/com/gumtree/csagent/service/runtime/AlreadyCalledPromptConsumptionTest.java:1` stating that it is not primary evidence for behaviour reversal. No mocked-LLM integration test is used as primary causal proof.

## Non-Blocking Notes

- §4.1 anti-hardcode verdict: `approve`. Applying the loaded nine-question kernel: Q1 no new keyword / regex / if-else / enum / per-UC semantic matrix in the fix diff; Q2 no Tier-0 invariant; Q3 the parent bundle is a soft `already_called` signal consumed by the LLM; Q4 no visible-eval text or CaseSpec id is encoded into runtime, prompt, or judge config; Q5 semantic ownership remains with the LLM; Q6 no new prompt if-else block; Q7 tool schema, permission, PII/safety, and grounding floors are untouched; Q8 the PASS branch's cs_040 rerun closes target evidence while neighbor / negative / shadow stay G2 deferrals per the parent handoff; Q9 no temporary hardcode needs a rollback plan.
- Hard-fence check: `git diff 39cb1b9..HEAD` changes only `docs/sprints/sprint-023-handoff.md` and `server/src/test/java/com/gumtree/csagent/service/runtime/AlreadyCalledPromptConsumptionTest.java`. No eval-spec, governance-doc, foundational-doc, deadline/timeout config, model config, new Tier-0, or older sprint-archive edit is present in the committed fix diff.
- action_bank phrasing: PASS wording is verified at `docs/sprints/sprint-023-handoff.md:1339` as `landed with target-reversal evidence`, not flat `done`. `docs/action_bank.md` itself remains deliver-agent-owned and unchanged in this review pass.
- Server suite: `mvn test` from `server/` was run locally and currently reports 898 tests, 1 failure, 0 errors, 1 skipped. The failing test is `SystemPromptUserRequestedTiebreakerTest.systemPrompt_marksActiveUcTiebreakerExplicitly` (`server/target/surefire-reports/com.gumtree.csagent.service.runtime.SystemPromptUserRequestedTiebreakerTest.txt:4`), caused by the unrelated working-tree `ACTIVE-UC TIEBREAKER` header rename that is not in `git diff 39cb1b9..HEAD`; this is recorded as a test-state note, not a closure blocker for Findings 1-3.
- Packaging rollforward: current working-tree packaging files such as `compact/sprint-023-*.md` and `docs/sprint_objective.md` are treated as deliver-agent rollforward context per the fix-review instructions, not scope drift. They are not part of the committed fix diff under review.
- Deferred/out-of-scope action_bank items remain non-blocking: the handoff continues to propose follow-ons such as `R-accumulated-tool-results-prompt-consumption`, `R-slow-llm-placeholder-coalesce-honest-next-step`, and `R-llm-latency-budget-investigation`; these are outside the fix-iteration closure gate.
