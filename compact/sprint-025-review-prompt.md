Paste the content below this line into a fresh Codex session after the dev's commits land. No PR will be opened; review the commit range.

---

# Sprint 25 Codex review prompt — Per-LLM-call latency instrumentation

You are the **Anti-Hardcode Review Agent** for Sprint 25
(`R-per-llm-call-latency-instrumentation`).

## 1. Loader stanza

Load on cold start:

1. `AGENTS.md` → 2. `docs/current/doc_governance.md` →
   3. `docs/current/agent_context_guide.md` →
   4. `docs/current/iteration_governance.md` §1/§3/§4/§5/§7 →
   5. `docs/sprint_objective.md` (Sprint 25) end-to-end →
   6. `docs/sprints/sprint-025-handoff.md` end-to-end →
   7. `docs/sprints/sprint-024-handoff.md` §4 + §4.5 + §10 + §11
   (prior-sprint frame).

## 2. Anti-Hardcode kernel (§4.1)

Run the §4.1 kernel verbatim as loaded from
`iteration_governance.md`. Sprint 25 is **NOT exempt** —
instrumentation that adds a new field to a persisted artefact
consumed by the eval contract is on the eval-contract surface, so
§4.1 applies. Instrumentation should pass cleanly because no
semantic decision moves from LLM to Java.

## 3. Sprint-25-specific deviations and additional blocking criteria

The kernel covers the semantic-hardcode surface. Sprint 25 also
has these additional Sprint-specific checks; treat the failures
listed below as BLOCKING:

### 3.1 Bundled fix expectations

- A per-turn (or per-LLM-call) latency field exists on every
  `results.json` entry produced by a smoke run after this sprint.
  Verify by reading the produced `results.json` in the smoke
  run path the dev cites in handoff §5.
- The synthetic baseline produces reproducible numbers via a
  named command + source path the dev cites in handoff §4. You
  do not need to re-execute it; verify the command + output
  pairing is internally consistent and the n ≥ 30.
- The worked-example comparison cites methodology per Sprint 24
  §4.5 acceptance bar (synthetic baseline + post-`f2d4cb2`
  distribution + extraction commands per percentile).

### 3.2 Hard-fence violations = BLOCKING

Refuse the sprint if any of the following are present in the
commit range under review:

1. Deadline-budget edits (`application*.yml` / `application*.properties` /
   `LlmProperties.java` / anywhere) widening or changing the
   deadline.
2. Model config edits (`LlmProperties.java` / `application*.yml`)
   changing the model name or routing.
3. `prompt_projection` edits — new projected slot, signal surface
   change, prompt edit.
4. Eval-spec edits — `eval_interactive/case_specs/**`,
   `case_spec_overrides.yaml`, personas, judge rubric.
5. Tier-0 changes — anything that adds a kernel-level Runtime
   guarantee under `iteration_governance.md` §1.4 or
   `docs/runtime_freeze_and_risk_policy.md`.
6. Edits to Sprint 24-landed code:
   `BotSession.consecutiveDeadlineCount`,
   `SessionManager.consecutiveDeadlineCount(0)` builder line,
   `PhaseEvaluator` reset hook (lines 684–693),
   `PhaseEvaluator` `DEADLINE_EXCEEDED` branch (lines 765–803),
   `PhaseEvaluator` `LLM_UNAVAILABLE` branch (lines 804–817),
   `V13__add_consecutive_deadline_count.sql`,
   `Sprint24DeadlinePlaceholderCoalesceTest.java`.
7. Edits to Sprint 23-landed `system_prompt.txt` teaching paragraph.
8. Edits to `AlreadyCalledPromptConsumptionTest.java`.

### 3.3 Reproducibility check (BLOCKING)

Every quantitative claim in the handoff (latency numbers,
percentiles, sample counts, overhead deltas) MUST cite source
path + extraction command. A claim missing either is BLOCKING.
"Manual eyeball" is permitted only with explicit declaration AND
a reproducible source pointer.

Walk every numeric claim in handoff §4 / §5 / §6 / §12 and
verify each carries the source-path + extraction-command pair.
If any quantitative claim is "p95 was 35.2s" without the
extraction recipe, BLOCK.

### 3.4 Methodology reconciliation (BLOCKING)

The handoff §6 must name the comparison ground-truth method
(database query, `results.json` field, log file, or explicit
"source unknown / cannot be reconstructed"). Missing or vague =
BLOCKING.

### 3.5 Real-LLM smoke rerun (BLOCKING)

Per
`.claude/agent-memory/sprint-deliver-orchestrator/feedback_mocked_llm_cannot_prove_prompt_causal_change.md`:
latency data must come from real LLM execution, not a mocked
LLM. A mock controls the measured variable. If the smoke rerun
the dev cites in handoff §5 used a mocked LLM, BLOCK.

Verify by reading the dev's cited command + the produced
`results.json` — real-LLM runs typically have non-trivial
`elapsed_ms` and per-turn timing variance; mocked runs are
deterministic and fast.

## 4. Packaging-rollforward rule (NOT scope drift)

Deliver-agent-owned files in the commit (`docs/sprint_objective.md`,
`compact/sprint-025-*.md`, `compact/sprint-deliver-orchestrator.md`)
are NOT scope drift even if bundled via `git add -A`. If a path-based
blocker is your only blocking finding and substantive findings close
cleanly, NOTE the packaging artefact as non-blocking; do not block
on packaging. Deliver agent rolls forward with a packaging note.

## 5. Sprint-close header (§4.2)

Write the verdict to `docs/codex-findings.md`. Use the §4.2
header format verbatim:

```
## Sprint Review Decision
decision: pass | fix_required | out_of_scope_review
blocking_count: <number>
summary: <one paragraph>
```

Followed by the per-finding detail (one section per finding;
each finding labeled "Finding N: <name>" + severity + evidence).
For BLOCKING findings, the evidence must be reproducible (file
path + line range + the exact text or diff snippet).

## 6. Out-of-scope concerns

Concerns that fall outside the Sprint 25 scope (i.e. things
worth doing but not in this sprint's remit) belong in
`docs/action_bank.md` §5.2 as deferred R-items. Do NOT include
them as blocking findings. Mention them at the end of your
verdict body under "Deferred to action_bank" so the deliver
agent can apply them on close.
