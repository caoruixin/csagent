# Milestone M-Auto-3 — Codex Milestone-Close Review (archived 2026-06-03)

> Cumulative range `git diff b71d6b5~1..HEAD` = S-Auto-11..15 + the bundled M-Auto-2 residual. Review prompt: `compact/M-Auto-3-review-prompt.md`. Archived from the live `docs/codex-findings.md` at M-Auto-3 close.

## Sprint Review Decision
decision: pass
blocking_count: 0
summary: Reviewed `git diff b71d6b5~1..HEAD` for the cumulative M-Auto-3 code range plus bundled M-Auto-2 residual code. The current checkout is one docs-only close-prep commit past the prompt's stated `ad715d5`; code verdicts below ignore docs/handoff/close artefacts except for the required PARAPHRASE_STORM metric-classification review. No blocking semantic hardcode, no new `escalation_reason` enum value, no self-invented Tier-0 invariant, no visible CaseSpec text/ids encoded into runtime or prompt, and no §5.4 eval masking found. Scoring SHA held at `35305bd84402ac455b126be5bcf8b2cb3b3fa55e3399f2c02443ea74bd8704e8`; S-Auto-15 left the A1/A3 within-turn gates and B1 resolver byte-untouched.

## M-Auto-2 Residual

### Concern: fence-#13 scoring overrides

Diff snippet:

```diff
diff --git a/autoloop/autoloop/scoring/eval_runner.py b/autoloop/autoloop/scoring/eval_runner.py
+    resolved_path = spec.path if spec.path.is_absolute() else (_REPO_ROOT / spec.path)
...
-        str(spec.path),
+        str(resolved_path.resolve()),

diff --git a/autoloop/autoloop/scoring/tier_evaluator.py b/autoloop/autoloop/scoring/tier_evaluator.py
-    l0 = _evaluate_layer0(current_suites)
+    l0 = _evaluate_layer0(current_suites, baseline)
...
+    """Tier-0 floor - any NEW (loop-introduced) Tier-0 violation -> discard.
+    ...
+    A candidate failure whose baseline status is unknown/missing is treated
+    conservatively as a violation (safety floor: do not mask).
```

Reasoning: `eval_runner.py` is a cwd/path fix and does not encode semantic content. `tier_evaluator.py` changes the autoloop fitness layer from absolute failures to candidate-vs-baseline delta failures. That is a scoring-contract correction for candidate fitness, not a CaseSpec widening: pre-existing baseline failures are reported as ignored, while unknown/missing baseline status remains a violation. This is within the human-authorized fence-#13 override and does not weaken the curated bad-case manual gate.

Verdict: `approve`

### Concern: determinism config touches Java runtime

Diff snippet:

```diff
diff --git a/server/src/main/java/com/gumtree/csagent/controller/ChatController.java
-    private static final long USER_FACING_LLM_DEADLINE_MS = 30_000L;
+    private static final long USER_FACING_LLM_DEADLINE_MS = 60_000L;

diff --git a/server/src/main/java/com/gumtree/csagent/model/LlmRequest.java
-    private double temperature = 0.3;
+    private double temperature = 0.0;

diff --git a/server/src/main/java/com/gumtree/csagent/service/runtime/LlmInvocationService.java
-                    .temperature(0.3)
+                    .temperature(0.0)
...
-                    .temperature(0.1)
+                    .temperature(0.0)
```

Reasoning: This is determinism/timeout configuration, not a semantic routing, escalation, drift, or UC rule. It does not introduce keywords, regexes, enum values, or per-UC matrices. The `autoloop/config.yaml` concurrency change is the same infra class. The scoring SHA was rebaselined only for the two scoring overrides and still computes to `35305bd8...704e8`.

Verdict: `approve`

## S-Auto-11

### Exemption: loop orchestration and eval harness

Diff snippet:

```diff
diff --git a/autoloop/autoloop/loop.py b/autoloop/autoloop/loop.py
+        result.eval_traces_path = _persist_eval_traces(
+            results_root, suite_run_results, iteration_id
+        )
...
+        is_infra, infra_reason = _assess_infra_error(suite_run_results, config)
+        if is_infra:
+            result.infra_error = True
+            result.infra_error_reason = infra_reason
+            result.decision = "error"
+            result.error = f"infra-error: {infra_reason}"
+            _persist_iteration(...)
+            return _finalize(result, started)
```

Reasoning: This is infra/eval-harness work: persist eval traces and classify corrupted eval evidence as `decision="error"` instead of passing it into fitness scoring. It does not keep a candidate or widen a CaseSpec to accept bot behavior; it prevents empty/failed/pervasively degraded evidence from being mis-scored. The active-use-case contract-violation signal is broad, but it is used only to mark an iteration as infra-error, not to approve a bot response.

Verdict: `approve`

## S-Auto-12

### Concern: A1 hard branch in dispatch path plus binding prompt signal

Diff snippet:

```diff
diff --git a/server/src/main/java/com/gumtree/csagent/service/runtime/AgentRunLoopImpl.java
+        Map<String, ToolEvent> successfulDispatchCache = new LinkedHashMap<>();
...
+                String dedupHash = contextProjectionBuilder.canonicalArgumentsHash(
+                        call.getArguments());
+                String dedupKey = "hash_error".equals(dedupHash)
+                        ? null
+                        : toolName + "|" + dedupHash;
+                if (dedupKey != null) {
+                    ToolEvent cachedHit = successfulDispatchCache.get(dedupKey);
+                    if (cachedHit != null) {
+                        ToolEvent base3 = ToolEvent.deduplicated(...);
+                        accumulatedToolResults.put(toolName, cachedHit.resultData());
+                        continue;
+                    }
+                }
...
+                if (dedupKey != null && result != null && result.isSuccess()) {
+                    successfulDispatchCache.putIfAbsent(dedupKey, recorded);
+                }

diff --git a/server/src/main/resources/prompts/system_prompt.txt
+Read this slot before emitting tool calls - it is a binding signal, not merely an observation.
+... You own the judgement on which tool to call and what content to send; the signal only discourages byte-identical repeats.
```

Reasoning: Q1 is yes only in the sense that a Java branch was added, but it is not a semantic decision branch. The key is `toolName|canonicalArgumentsHash` and applies uniformly to all tools; it does not inspect user text, query content, UC-specific wording, or CaseSpec ids. Success-only caching preserves legitimate retries after external failure. The prompt instruction is observable-state guidance around a runtime idempotency guarantee and explicitly preserves the LLM's ownership of tool choice and content.

Verdict: `approve`

## S-Auto-13

### Concern: A2 classify-first skill guidance and A3 soft anti-research signal

Diff snippet:

```diff
diff --git a/server/src/main/resources/skills/discover_triage.yaml b/server/src/main/resources/skills/discover_triage.yaml
-Instead, gather enough evidence to classify toward the right FAQ-path UC: call `search_knowledge` ...
+Instead, classify FIRST toward the right FAQ-path UC: call `classify_use_case` ...
+Do NOT call `search_knowledge` before `classify_use_case` in DISCOVER - while no use case is committed (active_use_case=none) a knowledge search is outside the tool's policy scope and only wastes a step
...
-    trace_check: "tool_event_seq(search_knowledge) < tool_event_seq(classify_use_case)"
+    trace_check: "accumulated_tool_results.classify_use_case"

diff --git a/server/src/main/java/com/gumtree/csagent/service/runtime/ContextProjectionBuilder.java
+                if (priorSearch != null && priorSearch.has("faq_miss")
+                        && !priorSearch.path("faq_miss").asBoolean(true)) {
+                    projection.put("prior_search_knowledge_viable_hit", true);
+                    projection.put("search_reuse_instruction", "... Do NOT call "
+                            + "search_knowledge again this turn ... A fresh search_knowledge "
+                            + "is only warranted if the prior result was faq_miss=true or your "
+                            + "new query is materially different ...");
+                }
```

Reasoning: The A2 change removes a tool-policy contradiction: `search_knowledge` is not allowed while `active_use_case=none`, so DISCOVER now uses the already-allowed `classify_use_case` first. The pre-existing FAQ-shaped cue text remains prompt-layer guidance, not a runtime branch. A3 is deliberately a soft projected diagnostic and prompt instruction. It does not block dispatch and leaves "materially different" to the LLM. Q6 is reviewed: the prompt has observable-state guidance based on `faq_miss`, not a new if/else keyword decision tree.

Verdict: `approve`

## S-Auto-13b

### Concern: within-turn A3 deterministic backstop suppresses re-search

Diff snippet:

```diff
diff --git a/server/src/main/java/com/gumtree/csagent/service/runtime/AgentRunLoopImpl.java
+        ToolEvent lastSearchKnowledgeViableHit = null;
...
+                if ("search_knowledge".equals(toolName)
+                        && lastSearchKnowledgeViableHit != null) {
+                    ToolEvent base4 = ToolEvent.paraphraseSuppressed(
+                            step, call, lastSearchKnowledgeViableHit.resultData(),
+                            lastSearchKnowledgeViableHit.stepIndex());
+                    accumulatedToolResults.put(toolName,
+                            lastSearchKnowledgeViableHit.resultData());
+                    continue;
+                }
...
+                if ("search_knowledge".equals(toolName)) {
+                    if (result != null && result.isSuccess()) {
+                        ...
+                        lastSearchKnowledgeViableHit = viableHit ? recorded : null;
+                    } else {
+                        lastSearchKnowledgeViableHit = null;
+                    }
+                }
```

Reasoning: Q1 is yes as a hard dispatch branch, but it is keyed purely on the existing `faq_miss=false` result state and only for `search_knowledge`; it does not inspect query text, keywords, similarity, CaseSpec ids, or UC matrices. Q3 is satisfied because S-Auto-13 shipped the soft projection first and the handoff evidence shows it fired but was ignored by the model. Q5 is not a semantic ownership transfer: the LLM still owns the first search and the query content; runtime enforces a budget/idempotency backstop after a viable hit on a read-only tool. The prior per-sub-sprint Codex pass for Sprint 069 is consistent with this cumulative review.

Verdict: `approve`

## S-Auto-14

### Concern: B1 resolver changes escalation reason for MAX_STEPS

Diff snippet:

```diff
diff --git a/server/src/main/java/com/gumtree/csagent/service/runtime/PhaseEvaluator.java
-        boolean searchedKnowledge = false;
+        ToolEvent lastSearch = null;
         if (result != null && result.toolEvents() != null) {
             for (ToolEvent te : result.toolEvents()) {
                 if ("search_knowledge".equals(te.toolName())) {
-                    searchedKnowledge = true;
-                    break;
+                    lastSearch = te;
                 }
             }
         }
-        if (searchedKnowledge) {
+        if (lastSearch != null && lastSearch.resultData() instanceof Map<?, ?> data
+                && Boolean.TRUE.equals(data.get("faq_miss"))) {
             return "faq_miss_threshold_exceeded";
         }
         return "turn_budget_exhausted";
```

Reasoning: Q1 is yes in the narrow sense that escalation-reason labeling changed, but the change removes a lossy tool-presence heuristic and replaces it with evidence already produced by the tool. It does not add an enum; it reuses `turn_budget_exhausted` for viable-hit/no-search/null cases and keeps `faq_miss_threshold_exceeded` only for a genuine miss. This improves reason honesty without moving escalation posture from the LLM to Java because it only labels the runtime's own `MAX_STEPS` fallback.

Verdict: `approve`

### Concern: zero-case eval sync and §5.4

Diff snippet:

```text
git diff b71d6b5~1..HEAD -- \
  eval_interactive/eval_interactive/case_spec/schema.py \
  eval_interactive/case_specs/bad_cases \
  eval_interactive/case_specs/promotion

No bad_cases, promotion CaseSpec, or escalation enum schema changes in this range.
```

Relevant unchanged scorer contract:

```python
"_ESCALATION_REASON_FAMILY": {
    "faq_miss_threshold_exceeded": "bot_limit",
    "turn_budget_exhausted": "bot_limit",
}
...
if actual_reason != expected_trigger and not same_family:
    return HardCheckResult("escalation_compliance", False, ...)
```

Reasoning: The zero-sync is §5.4-honest. Since the hard check is family-based and both reasons remain `bot_limit`, exact sibling re-stamps do not fail L1. Editing genuine-miss specs from `faq_miss_threshold_exceeded` to `turn_budget_exhausted` would have widened the spec to accept churn, so not editing is the correct non-masking choice.

Verdict: `approve`

## S-Auto-15

### Concern: new BotSession-scoped cross-turn gate

Diff snippet:

```diff
diff --git a/server/src/main/java/com/gumtree/csagent/model/BotSession.java
+    @Column(name = "cross_turn_faq_hit_use_case")
+    private String crossTurnFaqHitUseCase;
+    @Column(name = "cross_turn_faq_hit_payload", columnDefinition = "jsonb")
+    private String crossTurnFaqHitPayload;
+    @Column(name = "cross_turn_search_allowed_since_hit", nullable = false)
+    @Builder.Default
+    private Integer crossTurnSearchAllowedSinceHit = 0;

diff --git a/server/src/main/java/com/gumtree/csagent/service/runtime/AgentRunLoopImpl.java
+    private static final String SEARCH_KNOWLEDGE_TOOL = "search_knowledge";
+    private static final int CROSS_TURN_SUPPRESSION_BUDGET = 1;
...
+        final boolean crossTurnGateConditionsHold =
+                !driftThisTurn
+                        && activeUc != null
+                        && standingHitUcSnapshot != null
+                        && standingHitUcSnapshot.equals(activeUc)
+                        && standingHitPayloadSnapshot != null
+                        && !standingHitPayloadSnapshot.isBlank();
...
+                if (SEARCH_KNOWLEDGE_TOOL.equals(toolName)
+                        && crossTurnGateConditionsHold) {
+                    if (standingBudgetSnapshot >= CROSS_TURN_SUPPRESSION_BUDGET) {
+                        Object standingPayload = deserializeStandingPayload(
+                                standingHitPayloadSnapshot);
+                        if (standingPayload != null) {
+                            ToolEvent base5 = ToolEvent.crossTurnParaphraseSuppressed(...);
+                            accumulatedToolResults.put(toolName, standingPayload);
+                            continue;
+                        }
+                    } else if (!crossTurnRefinementCountedThisTurn) {
+                        session.setCrossTurnSearchAllowedSinceHit(
+                                standingBudgetSnapshot + 1);
+                    }
+                }
```

Reasoning: Q1 is yes as a Java branch on a tool call, but it is a structural cardinality/idempotency backstop, not a semantic content classifier. The gate is limited to read-only `search_knowledge`, fails open on missing/malformed payload, disables on drift/UC change, allows the first cross-turn refinement, and keys only on existing `activeUseCase`, `driftType`, `faq_miss`, and the fixed budget counter. It adds no query-content, keyword, similarity, CaseSpec-id, or per-UC matching. Q3 is satisfied by the S-Auto-13 soft-signal-first evidence: the same model ignored the viable-hit reuse instruction on the same paraphrase shape, so repeating a cross-turn soft-only sprint was not required before adding a deterministic substrate backstop. Q5 does not fail because the LLM still owns the first search, first cross-turn refinement, drift semantics, and query content; Java only prevents rank-2+ same-UC/no-drift re-searches after grounded evidence is already available.

Verdict: `approve`

### Concern: PARAPHRASE_STORM three-class re-frame and §5.4

Diff snippet:

```diff
diff --git a/docs/milestone_objective.md b/docs/milestone_objective.md
+`PARAPHRASE_STORM` **(MET - RE-FRAMED ... into THREE classes ...
+(1) within-turn PARAPHRASE_STORM = HARD failure metric -> `0/0/0`.
+(2) cross-turn rank-2+ repeated storm ... = HARD failure metric ...
+(3) cross-turn rank-1 first refinement ... = OBSERVATION metric ONLY ...
+Reported (`19` across the 3-pass) but not gated.
+cross-turn TOTAL (`22`) is still REPORTED but is NOT a hard gate
+the detector is NOT changed ... budget stays 1 ... no query-content/keyword/similarity matching.
```

Reasoning: This is a legitimate metric-classification correction, not §5.4 masking. The rank-1 first cross-turn refinement cannot be declared wrong without content/similarity matching, which would itself cross the anti-hardcode boundary, and the runtime budget floor intentionally preserves it. The detector still counts and reports rank-1 and total cross-turn events; no detector edit hides them, no CaseSpec is widened, and no budget was lowered to 0 to force a visible metric pass. Rank-2+ repeated re-search remains the hard failure class and is handled by the S-Auto-15 gate plus the human/deliver zero-false-suppression audit.

Verdict: `approve`

### Concern: S-Auto-15 test-infra housekeeping and §5.4

Diff snippet:

```diff
diff --git a/eval_interactive/tests/regression/test_corpus_lint.py b/eval_interactive/tests/regression/test_corpus_lint.py
+def _venv_python() -> str:
+    venv = os.environ.get("VIRTUAL_ENV")
+    if venv:
+        candidate = Path(venv) / "bin" / "python"
+        if candidate.exists():
+            return str(candidate)
+    return sys.executable
...
-def test_full_corpus_lints_clean_with_smoke_subset_flag():
+def test_canonical_corpus_lints_clean_with_smoke_subset_flag(tmp_path):
...
+    for bucket in CORPUS_BUCKETS:
+        src = CASE_SPECS_ROOT / bucket
+        if src.exists():
+            shutil.copytree(src, staged_root / bucket)

diff --git a/eval_interactive/tests/test_s_eval_5_l3_repositioning.py
+        for name in ("action_bank.md", "action_bank_archive.md"):
+            ...
+        # scan EVERY occurrence of the R-item for a closure marker

diff --git a/eval_interactive/tests/test_agent_client_session_create_timeout.py
-    assert DEFAULT_READ_TIMEOUT_SECONDS == 60.0
+    assert DEFAULT_READ_TIMEOUT_SECONDS == 90.0
```

Reasoning: These are test-infra/eval-governance alignments: subprocess interpreter selection under `uv run`, action-bank archive split awareness, a timeout constant re-pin to match the harness constant, and override-count/doc sync tests. The corpus linter rule was not relaxed; the test now targets the canonical golden buckets instead of intentionally non-golden suite directories such as `bad_cases`. No CaseSpec expected behavior was widened to accept an agent mistake.

Verdict: `approve`

### Fence and scope verification

Diff snippet:

```text
git diff --stat 4efc825..37dbb35 -- \
  server/src/main/java/com/gumtree/csagent/service/runtime/PhaseEvaluator.java \
  server/src/main/java/com/gumtree/csagent/service/runtime/AgentRunLoopImpl.java

AgentRunLoopImpl.java | 324 +++++++++++++++++++++
1 file changed, 324 insertions(+)

git diff 4efc825..37dbb35 -- PhaseEvaluator.java
<no diff>
```

Reasoning: S-Auto-15 did not modify B1 `resolveMaxStepsReason`. In `AgentRunLoopImpl`, the S-Auto-15 commit adds a separate `6a-quater` cross-turn gate and helpers after the existing A1 `successfulDispatchCache` and A3 `lastSearchKnowledgeViableHit` blocks; the diff for that interval is insertions-only. No M-Auto-4 deferred surfaces were pulled in: no Module B2/B3 clarification-budget/fallback-UC work, no Module C classifier non-determinism work, no new `escalation_reason`, and no hard-fenced scoring-file edits after the M-Auto-2 residual overrides.

Verdict: `approve`
