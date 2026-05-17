## Milestone Review Decision
milestone: M1 - DISCOVER + Intake
commit_range: c9edb37..eb65e2b
sub_sprints_reviewed: Sprint 33, Sprint 34, Sprint 35
decision: fix_required
blocking_count: 2
summary: Scope discipline, hard fences, schema loading, the Sprint 33/34 Java regression surface, and the three anti-hardcode kernel passes all cleared. M1 still cannot close on this Codex pass because the independent Alice bad-case rerun on the cumulative HEAD produced a factual over-narrowing/fabrication about the specific removal reason, and because the Sprint 35 diagnostic recommendation remains stale against the deliver-agent/human action_bank close decision.

## Review Evidence
- Reviewed commit range `c9edb37..eb65e2b` and per-sub-sprint ranges `8a22aa6^..8a22aa6`, `8a22aa6..e532f0d`, and `e532f0d..eb65e2b`.
- Loaded the governance chain (`AGENTS.md`, `docs/current/doc_governance.md`, `docs/current/agent_context_guide.md`, `docs/current/iteration_governance.md`) plus M1 objective, Sprint 33/34/35 objectives and handoffs, action bank entries, bad-case suite, Sprint 35 matrix, probe corpus, dev prompts, and cited result JSONs.
- Ran the requested cumulative hard-fence diff scan; output was empty for all forbidden semantic/runtime/eval surfaces.
- Ran Sprint 35 probe schema load from a detached clean worktree at `eb65e2b`: `load_case_specs('case_specs/probe/option_beta_coverage')` returned `25`.
- Re-extracted Sprint 33 Alice evidence from `eval_interactive/results/20260516-110928/results.json`: terminal active UC-A, 4 turns, judge 0.8, stop `goal_achieved`, and populated `discover_disambiguation_signals`.
- Re-extracted M1-close baseline Alice evidence from `eval_interactive/results/20260516-232938/results.json`: active UC-A, 3 turns, stop `bot_ended`, `request_handover` reason `faq_miss_threshold_exceeded`, and populated `discover_disambiguation_signals`.
- Ran targeted Java regression command for new Sprint 33/34 tests plus `Sprint71PartialIntakePersistenceTest`; all passed.
- Ran full Java baseline from clean detached HEAD `eb65e2b`; surefire XML totals were `Tests run: 983, Failures: 0, Errors: 0, Skipped: 2`. This is no regression; the expected inherited failure only appears when the dirty, unauthored `system_prompt.txt` working-tree modification is present.
- Ran an independent Alice bad-case rerun against clean HEAD code on port 18080 using overlaid bad-case and Alice mock fixtures because those fixtures are not tracked at `eb65e2b`; result copied to `eval_interactive/results/20260516-235235/results.json`.

## Blocking Findings
1. M1 primary gate is not stable: Codex's independent Alice rerun fabricated a specific takedown reason.

   Evidence: `eval_interactive/results/20260516-235235/results.json` has `active_use_case=UC-A`, `total_turns=4`, `stop_reason=goal_impossible`, and tool sequence `search_knowledge -> resolve_article -> record_outcome -> search_knowledge -> resolve_article`. The trace did use the UC-A FAQ path, but on the second bot answer it converted a generic FAQ article into a specific claim about AD-2001:

   ```text
   Based on the information we have, your ad (AD-2001) appears to have been removed because a rights owner reported it for breaching their Intellectual Property Rights (source: ka41r000000LIEEAA4).
   ```

   The tool evidence immediately before that response was a generic `resolve_article` on `ka41r000000LIEEAA4`, not an AD-2001-specific moderation record. This hits the Alice CaseSpec fail condition "bot fabricates an answer about why the ad was removed" in `eval_interactive/case_specs/bad_cases/alice_uc_a_uc_h_misclass.yaml`. The failure is not a hard-fence or semantic-hardcode diff issue; it is a primary acceptance gate failure. Likely fix layer for triage is `semantic_planner` / grounding-answer discipline, not a Java keyword/regex guard.

2. Sprint 35 diagnostic recommendation is stale relative to the recorded close decision.

   Evidence: `docs/diagnostics/option_beta_coverage_matrix.md:347` still says the primary recommendation is to re-anchor `iteration_governance.md` Section 7.2, and `docs/diagnostics/option_beta_coverage_matrix.md:379` still recommends surfacing `R-option-gamma-alternate-uc-surveyor-design`. That conflicts with `docs/action_bank.md:667`, which records that the Section 7.2 re-anchor was considered and dropped on constitution-discipline review, and with `docs/action_bank.md:685`, which opens `R-loosen-topic-uc-binding-llm-owned-drift` as a constraint-removal / architecture-alignment item while explicitly rejecting a new live `AlternateUseCaseSurveyor` deterministic component. The matrix itself is populated and reproducible, but the requested Section 6.1/8 verification that the matrix Section 5 decision recommendation matches the action_bank R-item flips fails.

## Anti-Hardcode Kernel - Sprint 33 (prompt_projection)
1. Q1: Approve. The new runtime logic projects observable state (`listing_context.status`, `form_topic_subject`, registry candidates); no regex or user-text keyword branch routes the user.
2. Q2: N/A. No Tier-0 invariant is claimed or added.
3. Q3: Approve. The change is the soft-signal projection that Q3 prefers over a Java routing branch.
4. Q4: Approve. No Alice CaseSpec id, trace wording, or raw eval phrase is encoded into runtime, prompt, or judge config.
5. Q5: Approve. `ContextProjectionBuilder.java:418` explicitly surfaces evidence and says runtime does not enforce; the LLM still owns classification.
6. Q6: Approve. `PhaseEvaluator.java:432` and `system_prompt.txt:36` add principle-level guidance, not an if/else decision table.
7. Q7: Approve. Tool schema, permissions, PII/safety floor, and FAQ grounding surfaces are untouched.
8. Q8: Approve with scoped deferral. Target/neighbor/negative Java coverage shipped; shadow validation is the M1 bad-case rerun per Section 5.6/4.3.
9. Q9: N/A. The slot is not temporary and does not need a sunset plan.

## Anti-Hardcode Kernel - Sprint 34 (skill_state)
1. Q1: Approve. The new `IntakeFieldExtractor.java:112` branches map already-known `form_context` fields after UC classification; they do not decide UC routing, drift, escalation, or risk.
2. Q2: N/A. No Tier-0 invariant is claimed or added.
3. Q3: Approve. This is plumbing for the existing soft projection `intake_state`, not a candidate for another soft signal.
4. Q4: Approve. No visible-eval text or Alice-specific phrasing is encoded; fixtures are generic and field-schema based.
5. Q5: Approve. LLM still owns UC classification, inline `intake_fields` capture, and escalation choice; Java only preserves form-context state.
6. Q6: N/A. No prompt edit in Sprint 34.
7. Q7: Approve. Tool schema, permission boundary, PII/safety floor, and grounding floor are unchanged; intake-complete guard remains untouched.
8. Q8: Approve with scoped deferral. Target UC-G/H/I/J, neighbor UC-K, and negative UC-A/UC-I coverage shipped at Java layer; shadow remains M1 close.
9. Q9: N/A. The extractor extension is permanent plumbing.

## Anti-Hardcode Kernel - Sprint 35 (eval_spec)
1. Q1: Approve. Probe CaseSpecs and the matrix are observation instruments, not runtime decision logic.
2. Q2: N/A. No Tier-0 invariant is claimed or added.
3. Q3: N/A. Sprint 35 ships no LLM-consumed runtime surface.
4. Q4: Approve. Probe ids are new diagnostics and do not feed runtime, prompt, or judge config.
5. Q5: Approve. Zero production runtime change; no semantic ownership moved from LLM to Java.
6. Q6: Approve. Prompt is untouched.
7. Q7: Approve. Tool schema, permission boundary, PII/safety floor, and grounding floor are untouched.
8. Q8: Approve for probe-sprint shape. Target is the 25-cell matrix; strong-prior/handover cells act as controls; no shadow concept applies to this probe corpus.
9. Q9: N/A. Probe corpus is a retained diagnostic baseline, not a temporary workaround.

## Hard-Fence Verification (per sub-sprint + milestone-level)
- Sprint 33 `8a22aa6^..8a22aa6`: PASS. Diff touched only `ContextProjectionBuilder.java`, `PhaseEvaluator.java`, `system_prompt.txt`, two new tests, and `docs/sprints/sprint-033-handoff.md`.
- Sprint 33 forbidden surfaces: PASS. No diff to `RuntimeIntentClassifier.java`, `IntentClassification.java`, `DriftResult.java`, `DriftDetector.java`, `UseCaseRouter.java`, `ClassifyUseCaseTool.java`, `IntakeFieldExtractor.java`, eval harness, case families, bad/smoke/shadow specs, governance docs, foundational docs, or deliver-agent-owned files.
- Sprint 34 `8a22aa6..e532f0d`: PASS. Diff touched only `IntakeFieldExtractor.java`, three new Java test files, and `docs/sprints/sprint-034-handoff.md`.
- Sprint 34 forbidden surfaces: PASS. Empty diff for `IntakeFieldsRegistry.java`, `AgentRunLoopImpl.java`, `Sprint71PartialIntakePersistenceTest.java`, `ContextProjectionBuilder.java`, `FormContextIngestionService.java`, and `system_prompt.txt`; UC-K regex/helper bodies unchanged.
- Sprint 35 `e532f0d..eb65e2b`: PASS. Diff contains only the new probe directory, `docs/diagnostics/option_beta_coverage_matrix.md`, and `docs/sprints/sprint-035-handoff.md`.
- Sprint 35 forbidden surfaces: PASS. Empty diff for all `server/src/main/`, all `server/src/test/`, eval harness, existing case families/shadow/smoke/bad cases, overrides, `docs/current/iteration_governance.md`, deliver-agent-owned docs, and foundational docs.
- Milestone-level hard fences: PASS. Requested cumulative forbidden-path stat scan over `c9edb37..eb65e2b` returned empty output; no escalation_reason enum widening at `PhaseEvaluator.java:39-63`.

## Schema And Reproducibility Checks
- Sprint 33 Alice trace recipe reproduced from `eval_interactive/results/20260516-110928/results.json`: `{active: UC-A, turns: 4, judge: 0.8, stop: goal_achieved}` plus `discover_disambiguation_signals={REMOVED, [UC-A, UC-B, UC-FP, UC-H], true}`.
- M1-close baseline Alice recipe reproduced from `eval_interactive/results/20260516-232938/results.json`: `{active: UC-A, total_turns: 3, stop_reason: bot_ended, judge_score: 0.8667, containment_outcome: escalated}`, `alternate_candidate_use_cases=[]`, final handover reason `faq_miss_threshold_exceeded`.
- Sprint 35 probe loader validation from clean HEAD returned `25` CaseSpecs.
- Sprint 35 matrix default extraction reproduced all 19 result-json cells from `eval_interactive/results/20260516-140339/results.json`.
- Sprint 35 matrix fallback extraction reproduced all 6 contract-violation cells from `/v1/demo/sessions/<sid>/trace` with no `Accept: application/json` header: Ad Support fallback cells all returned `[UC-A, UC-B, UC-FP, UC-H]`; Tech Support fallback cells all returned `[UC-E, UC-K]`.
- Sprint 35 matrix population: PASS. Every probe id in `docs/diagnostics/option_beta_coverage_matrix.md` Section 3 has a cited source recipe.
- Sprint 35 decision consistency: FAIL. See Blocking Finding 2.

## Validation Runs
- `cd /tmp/csagent-m1-review-eb65e2b/eval_interactive && uv run python -c "from eval_interactive.case_spec.loader import load_case_specs; specs = load_case_specs('case_specs/probe/option_beta_coverage'); print(len(specs))"` -> `25`.
- `mvn -q -pl server test -Dtest='DiscoverDisambiguationSignalsProjectionTest,AgentRunLoopDiscoverDisambiguationNonEnforcementIntegrationTest,Sprint34IntakePrefillExtractorTest,Sprint34IntakePrefillProjectionAndGuardTest,AgentRunLoopUcGHIJIntakePrefillIntegrationTest,Sprint71PartialIntakePersistenceTest'` -> PASS.
- `mvn -q -pl server test` from clean detached HEAD `eb65e2b` -> surefire totals `983 / 0 / 0 / 2`; no Java regression. The documented `983 / 1 / 0 / 2` baseline appears tied to the dirty working-tree `system_prompt.txt` modification, not clean HEAD.
- Alice rerun setup: detached clean HEAD on port 18080; overlaid `eval_interactive/case_specs/bad_cases/` and Alice mock account/listing/moderation fixtures because these files are not tracked at `eb65e2b`; eval config used `qwen-plus` for simulator/judge and the local backend's real provider chain for the bot.
- `cd eval_interactive && uv run eval-interactive run --path case_specs/bad_cases/ --config eval_interactive.m1-close-review.yaml --label m1-codex-review-alice-rerun-qwen --parallel 1` -> result copied to `eval_interactive/results/20260516-235235/results.json`; closure criterion FAIL due Blocking Finding 1.
- The first Alice rerun attempt had an empty simulator/judge model because `${DASHSCOPE_CHAT_MODEL}` was unset; I discarded that attempt for closure judgment and reran with explicit `qwen-plus`.

## M1 Close Decisions Verified
- Section 7.2 fold-back considered and dropped: PASS in `docs/action_bank.md:667` and by absence of `docs/current/iteration_governance.md` from the Sprint 35 and cumulative diffs. Current `docs/current/iteration_governance.md:615` still uses the UC-A/UC-C worked example.
- `R-option-beta-coverage-gap-uc-a-uc-c-shape` closure: PASS in `docs/action_bank.md:667`, with considered-and-dropped framing and succeeded-by note.
- `R-loosen-topic-uc-binding-llm-owned-drift` opened: PASS in `docs/action_bank.md:685` as constraint-removal / architecture-alignment, with explicit anti-framings against a live deterministic `AlternateUseCaseSurveyor`, per-UC regex/if-else/enum expansion, and collapsing `topic_subject` entirely.
- Diagnostic-doc alignment with the close decision: FAIL. See Blocking Finding 2.

## Deferred / Non-Blocking Notes
- `R-llm-provider-latency-drift-2026-05-16` remains proposed/deferred; no M1 blocker assigned from latency observations.
- Sprint 33 OQ1-OQ5 and Sprint 34 OQ1-OQ5 remain M2 or docs-fold-back candidates; none were treated as M1 blockers.
- Sprint 36 remains deferred by scope: the planned trigger was Alice not being sufficiently closed by Sprint 33/34/35. This Codex pass blocks M1 on the Alice primary gate, but the observed failure is a grounded-answer fabrication/semantic-planner issue rather than evidence that an INTAKE-locked reroute trigger is required.
- Sprint 35 OQ2 Account Support 12-UC fallback, OQ3 Delete cross-topic mid-loop reclassification, and OQ4 trace endpoint content-negotiation quirk are informational M2/backend candidates, not M1 hard-fence violations.
- The primary worktree was dirty before this review. I did not revert or modify unrelated files; diff audits used commit ranges, and tests used a detached clean worktree where possible.
