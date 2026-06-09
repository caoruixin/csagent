Paste the content below this line into a fresh Codex session after the dev agent's fix commits land on `design-v1-without-human-review`. Point Codex at the fix commit range, not the full Sprint 20 PR.

---

# Sprint 20 Fix Iteration — Review Agent Prompt

You are the Anti-Hardcode Review Agent. Sprint 20 closed `fix_required / 2`. The dev agent has now landed a fix-iteration bundle targeting those two findings. Re-review the **fix bundle only** — not the full Sprint 20 PR.

## Loader

1. `AGENTS.md`.
2. `docs/current/iteration_governance.md` §3, §4, §4.2.
3. `docs/sprint_objective.md` — the "Sprint 20 fix iteration" section at the bottom is your authoritative scope; above the `---` divider is parent context.
4. `docs/sprints/sprint-020-fix-handoff.md` — end-to-end.
5. `docs/codex-findings.md` — the existing Sprint 20 header at top (you replace it). Finding 1 at lines 6–54, Finding 2 at lines 56–85.

## Scope

Re-review commits on `design-v1-without-human-review` since the original Sprint 20 review.

**Expected in fix diff:**
- 1 new integration test under `server/src/test/java/com/gumtree/csagent/integration/`.
- 1 new cs_011 T2 test under `.../integration/` OR `.../service/runtime/`.
- Edits to `eval_interactive/case_specs_shadow/_ACCESS_BOUNDARY.md`.
- New `docs/sprints/sprint-020-fix-handoff.md`.

**Flag as scope drift if any of these appear:**
- Any `server/src/main/**` file.
- `AlreadyCalledProjectionTest.java` (do not edit).
- Any prompt file.
- Any CaseSpec (visible or shadow) other than `_ACCESS_BOUNDARY.md`.
- `case_spec_overrides.yaml`, `personas*.yaml`.
- Any Python harness under `eval_interactive/` (implementing `--include-shadow` is forbidden).
- `docs/sprints/sprint-020-handoff.md`, `docs/sprint_objective.md`, `docs/codex-findings.md` (you re-write this), `docs/action_bank.md`, `docs/current/*`, `docs/foundational/*`, `docs/runtime_freeze_and_risk_policy.md`, any other sprint archive.

## §4.1 narrowness

§4.1 Q8 here = the two findings demonstrably closed; you are not re-litigating Sprint 20's overall coverage matrix. Walking §4.1 Q1–Q9 against the fix diff: Q1/Q2/Q4/Q5/Q6 should be "no" (test evidence + doc reconciliation only). Q3 does not apply (no branch added). Q8 turns on whether the new integration test demonstrates non-enforcement at `AgentRunLoopImpl.run(...)` granularity, not at `ContextProjectionBuilder` granularity alone.

## Finding 1 resolution checklist

- [ ] New integration test drives `AgentRunLoopImpl.run(...)` end-to-end (NOT a hand-constructed `List<ToolEvent>` passed to `ContextProjectionBuilder.build(...)`; that is the existing `AlreadyCalledProjectionTest.java` shape).
- [ ] Asserts (a) both identical LLM-emitted calls reach `ToolDispatcher.dispatch(...)` (`verify(toolDispatcher, times(2)).dispatch(...)` or `ArgumentCaptor`).
- [ ] Asserts (b) the second-step projection contains an `already_called` entry whose `tool` + `arguments_hash` reference the first call; `ObjectMapper`-based, no raw-string matching.
- [ ] cs_011 T2 test exists; unit-vs-integration choice + one-line justification cited in the fix handoff.
- [ ] cs_011 T2 test only asserts slot population; any assertion on LLM response content = scope drift.
- [ ] Both tests pass under `mvn -pl server test`; full server suite green; handoff quotes the `BUILD SUCCESS` tail.
- [ ] No `server/src/main/**` edited; `AlreadyCalledProjectionTest.java` unchanged.

Missing cs_011 T2 OR scope-drift assertion OR hand-constructed-events integration test → `fix_required`.

## Finding 2 resolution checklist

Inspect `eval_interactive/case_specs_shadow/_ACCESS_BOUNDARY.md`:

- [ ] Enforcement table no longer contains `Eval-harness runner (with --include-shadow)` row.
- [ ] `Enforcement (v0)` lists only the three actually-shipped mechanisms: (i) directory boundary; (ii) custom-path-only loading via `CaseSetManager.load_custom(path)`; (iii) documented self-restraint. Language matches `sprint-020-handoff.md` lines 443–448.
- [ ] `--include-shadow` text moved into a "Known v0 gaps / v1 hardening" section, framed as a documented v1 hardening direction (`R-shadow-include-flag-runner-gate`), NOT a current gate.
- [ ] No Python file under `eval_interactive/` edited.
- [ ] No CaseSpec, manifest, or persona file edited.

Partial reconciliation (row removed but section still names active flag, or vice versa) → `fix_required`.

## §4.2 header replacement

**Replace the existing `## Sprint Review Decision` header at the top of `docs/codex-findings.md` IN PLACE.** Do not append below; do not leave the `fix_required` header.

New header:

```
## Sprint Review Decision (Sprint 20 fix re-review)
decision: pass | fix_required | out_of_scope_review
blocking_count: <0 if pass; else count of new blocking>
summary: <one paragraph naming F1 closure + F2 closure + any new blocking>
```

If verdict `pass`: delete the original Finding 1 + Finding 2 detail sections; replace with a short "Resolution evidence" section quoting the new test names + the reconciled `_ACCESS_BOUNDARY.md` lines.

If verdict `fix_required`: keep the unresolved finding's detail; update summary.

## Out-of-scope → action_bank deferrals

Non-F1, non-F2 concerns — `R-shadow-include-flag-runner-gate` priority, `R-already-called-prompt-consumption`, unit-vs-integration choice for cs_011 T2, "Known v0 gaps" section heading — record as suggested action-bank deferrals in the summary. NOT blocking findings.

Specifically forbidden:

- Do NOT flag the missing `--include-shadow` CLI flag as blocking. Separate scope.
- Do NOT flag missing prompt-side consumption of `already_called`. That is `R-already-called-prompt-consumption` per `sprint-020-handoff.md:910`.
- Do NOT flag absence of additional shadow regression evidence beyond Sprint 20's.
- Do NOT flag absence of architecture-health metric collection.

## Verdict

One of `pass`, `fix_required`, `out_of_scope_review`. `out_of_scope_review` is reserved for fix bundles that scope-drift (runtime change snuck in, CaseSpec edited, prompt touched, `--include-shadow` actually implemented). Otherwise `pass` (both findings closed) or `fix_required` (one or both unresolved).

Human commits the header replacement; deliver agent applies action-bank deltas separately.
