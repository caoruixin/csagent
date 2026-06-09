Paste the content below this line into a fresh Claude Code session on the current branch `design-v1-without-human-review`. Dev agent has zero conversation context.

---

# Sprint 20 Fix Iteration — Dev Agent Prompt

Sprint 20 closed `decision: fix_required / blocking_count: 2`. The deliver agent appended a "Sprint 20 fix iteration" section to `docs/sprint_objective.md` scoping this fix to those two findings. You do not open a new sprint. You add test evidence (Finding 1) and reconcile one doc file (Finding 2). Nothing else.

## Loader (read in order)

1. `AGENTS.md` — constitution chain.
2. `docs/current/doc_governance.md` — tier model + docs PR rules.
3. `docs/current/agent_context_guide.md` — per-task reading lists + Context Pack Prompt.
4. `docs/current/iteration_governance.md` §3 (Fix Layer Classification), §5 (Eval Acceptance Rules), §7 (sprint-objective stanza).
5. `docs/sprint_objective.md` — whole file. Parent Sprint 20 objective sits above the `---` divider near the bottom; the "Sprint 20 fix iteration" section sits below it. Both are context. The fix-iteration section is your action scope.
6. `docs/codex-findings.md` — Finding 1 (lines 6–54), Finding 2 (lines 56–85).
7. `docs/sprints/sprint-020-handoff.md` — read only. §11 Q1 (`--include-shadow` open question); lines 443–448 (v0 shadow mechanism that actually shipped); §12 (`R-shadow-include-flag-runner-gate`).

After loading, write a short Context Pack (per `agent_context_guide.md`): source-of-truth for each finding (test code for F1, `_ACCESS_BOUNDARY.md` for F2), implementation status, and 2–3 fix-specific risks. Pause and surface if any cited path/line drifted.

## Hard fences (both findings)

- No keyword/regex/if-else/enum/per-UC matrix for any semantic decision (§1.5/§1.7).
- No edit to: `docs/current/iteration_governance.md`, `docs/current/doc_governance.md`, `docs/current/agent_context_guide.md`, `docs/foundational/*`, `docs/runtime_freeze_and_risk_policy.md`, any sprint archive under `docs/sprints/sprint-001..020-*.md` (incl. `sprint-020-handoff.md`), `docs/sprint_objective.md`, `docs/codex-findings.md`, `docs/action_bank.md`.
- No new R-items. Out-of-scope observations go into `sprint-020-fix-handoff.md` under "Out-of-scope observations"; stop.
- No new CaseSpec; no edit to existing CaseSpecs, `eval_interactive/case_spec_overrides.yaml`, `eval_interactive/personas*.yaml`, or `FAQ-knowledge_include_help_url.csv`.
- No prompt file edit (`system_prompt.txt` or template asset).
- No runtime semantic change. Do not add short-circuit logic to `AgentRunLoopImpl.run(...)`, do not modify `ContextProjectionBuilder.build(...)` projection-assembly logic, do not branch on `already_called`. Fix is test evidence + doc reconciliation only.
- No new Tier-0. If Finding 1's investigation reveals an existing short-circuit in `AgentRunLoopImpl.run(...)` that contradicts the parent objective's non-enforcement claim, flag `human_review_required` per §3.2 Q2 and stop.

## Finding 1 — runtime non-enforcement + cs_011 T2 target coverage

**Codex:** `docs/codex-findings.md` lines 6–54. **§4.1 Q:** Q8. **Layer:** `prompt_projection` (regression-test extension at the runtime boundary). **Parent bar:** `docs/sprint_objective.md` lines 160–161 (Track B "runtime does not short-circuit") + lines 231–233 (cs_011 T2 target coverage).

### Fix scope

**Test A — new integration test** under `server/src/test/java/com/gumtree/csagent/integration/`. Drives `AgentRunLoopImpl.run(...)` end-to-end with two identical LLM-emitted tool calls, asserting:

(a) Both calls reach `ToolDispatcher.dispatch(...)`. Use Mockito `ArgumentCaptor` or `verify(toolDispatcher, times(2)).dispatch(...)` on the dispatcher mock. Mirror `AgentRunLoopAd1002IntegrationTest.java` for the wiring shape.

(b) The second-step projection produced by `ContextProjectionBuilder.build(...)` (as called inside `AgentRunLoopImpl.run`) contains an `already_called` entry whose `tool` matches the first call's tool name and whose `arguments_hash` matches the hash function applied to the first call's normalized arguments. Parse via `ObjectMapper`; no raw-string matching.

The test must use the **actual** `AgentRunLoopImpl`, not a hand-constructed `List<ToolEvent>` passed to `ContextProjectionBuilder.build(...)`. The existing `AlreadyCalledProjectionTest.java` is the hand-constructed shape; Codex F1 explicitly requires the bar be demonstrated at `AgentRunLoopImpl.run` granularity so a future runtime short-circuit added before `ToolEvent` creation is caught.

**Test B — cs_011 T2 shape test.** Your call: unit (`server/src/test/java/com/gumtree/csagent/service/runtime/`) OR integration (`server/src/test/java/com/gumtree/csagent/integration/`). The cs_011 T2 shape is documented at `docs/action_bank.md:464` and in the cs_011 brief at `docs/diagnostics/failure-briefs/cs011-uc-d-detailed-description-ignored-on-faq-miss.md`: a UC-D T2 turn where the projection would surface a prior identical-args call. Assert only that the slot is populated for the cs_011-shape input. Do NOT remediate cs_011's underlying failure. Cite your unit-vs-integration choice with a one-line justification in the fix handoff.

### Files in scope (F1)

- New: `server/src/test/java/com/gumtree/csagent/integration/<TestA>.java` (you choose the filename; it should signal Sprint 20 fix + already-called runtime non-enforcement).
- New: `server/src/test/java/com/gumtree/csagent/{integration|service/runtime}/<TestB>.java`.

### Files NOT in scope (F1)

- `server/src/main/**` — no production edit.
- `server/src/test/java/com/gumtree/csagent/service/runtime/AlreadyCalledProjectionTest.java` — do not edit.
- Any other existing test file.

### Accept criteria (F1)

- `verify(toolDispatcher, times(2)).dispatch(...)` (or `ArgumentCaptor` equivalent) passes on Test A.
- The captured projection at step 1 contains an `already_called` array with ≥1 entry whose `tool` + `arguments_hash` reference the first call.
- Test B passes; shape choice + one-line justification cited in the fix handoff.
- No production source file edited.
- Full server suite green.

## Finding 2 — shadow access-boundary reconciliation (Option A)

**Codex:** `docs/codex-findings.md` lines 56–85. **§4.1 Q:** Q8. **Layer:** `infra` (eval-harness doc). **Parent bar:** `docs/sprint_objective.md` lines 110–119 (Track A shadow-split mechanism consistency). Cross-reference: `docs/sprints/sprint-020-handoff.md` lines 443–448 (v0 mechanism that actually shipped).

### Fix scope (Option A only)

Edit `eval_interactive/case_specs_shadow/_ACCESS_BOUNDARY.md`:

1. **Remove** the `| Eval-harness runner (with --include-shadow) | yes | yes |` row from the enforcement table (line 23 of the pre-fix file).
2. **Move** the `--include-shadow` flag description from `Enforcement (v0)` item #2 (lines 44–48 pre-fix) into a new "Known v0 gaps / v1 hardening" section. Frame the flag as a **documented v1 hardening direction** the next eval-governance sprint may add (cite `R-shadow-include-flag-runner-gate` per `sprint-020-handoff.md:927`), NOT as a currently-enforced gate. Preserve existing "Known v0 gaps" content in the consolidated section.
3. **Reconcile** `Enforcement (v0)` so it enumerates only the v0 mechanism actually shipped: (i) directory boundary; (ii) custom-path-only loading via `CaseSetManager.load_custom(path)`; (iii) documented self-restraint. Language should match `sprint-020-handoff.md` lines 443–448.

### Forbidden in this fix

- Do NOT implement the `--include-shadow` CLI flag. That is `R-shadow-include-flag-runner-gate`, separate scope.
- Do NOT edit `eval_interactive/eval_interactive/batch/sets.py` or any Python harness file.
- Do NOT edit `_manifest.yaml` (either path) or any CaseSpec.

### Files in scope (F2)

- `eval_interactive/case_specs_shadow/_ACCESS_BOUNDARY.md` (edit only).

### Files NOT in scope (F2)

- Any Python file under `eval_interactive/`.
- Any CaseSpec, manifest, or persona file.
- `docs/current/agent_context_guide.md`.

### Accept criteria (F2)

- Line-23 row removed.
- `Enforcement (v0)` enumerates only the three actually-shipped mechanisms.
- `--include-shadow` text relocated to "Known v0 gaps / v1 hardening", framed as v1 hardening.
- No Python file changed.

## Fix handoff requirement

Write `docs/sprints/sprint-020-fix-handoff.md` (NEW). NOT a 12-section sprint handoff. One section per finding plus header + verification. Suggested shape:

```
# Sprint 20 fix-iteration handoff
Date: <YYYY-MM-DD>
Parent sprint: 20 (objective at docs/sprint_objective.md "Sprint 20 fix iteration" section)
Codex findings closed: 2
Branch: design-v1-without-human-review

## Finding 1 — runtime non-enforcement + cs_011 T2
- Diff summary
- Test evidence (names, what they assert, why integration shape is correct for Test A)
- cs_011 T2 shape: <unit|integration> — one-line justification
- Parent bar satisfied: docs/sprint_objective.md lines 160–161 + lines 231–233
- Test runner tail

## Finding 2 — shadow access-boundary reconciliation
- Diff summary
- 3 actions performed (remove row, move flag, reconcile section)
- Parent bar satisfied: docs/sprint_objective.md lines 110–119
- Cross-reference: sprint-020-handoff.md lines 443–448

## Verification
- Full server suite tail (~10 lines, including BUILD SUCCESS + test totals)
- Per-finding accept-criteria confirmation

## Out-of-scope observations (if any)
```

Target ~80–150 lines.

## Verification (full server suite)

Maven multi-module: `csagent-parent` parent pom; modules `server` and `eval` (per repo `pom.xml` lines 20–23). Canonical command:

```
mvn -pl server test
```

Run from repo root. If a Maven wrapper (`./mvnw`) is used locally, use it and note in the handoff. Build must show `BUILD SUCCESS` and zero test failures. Quote the tail (~10 lines) in the handoff under "Verification".

## Stop conditions (surface to human; do not continue)

1. Existing short-circuit in `AgentRunLoopImpl.run(...)`. If during Test A drafting you find `AgentRunLoopImpl.run(...)` already short-circuits on the slot (or otherwise blocks the second identical-args call from reaching `ToolDispatcher.dispatch(...)`), this contradicts the parent objective's non-enforcement claim and is `human_review_required` per §3.2 Q2.
2. Test fails for an unrelated reason. Do not paper with extra mocks.
3. `_ACCESS_BOUNDARY.md` line numbers drifted (line 23 is not the `--include-shadow` row; lines 44–48 do not contain the runner-gate description).
4. Test runner command differs (`mvn -pl server test` is wrong for this repo).
5. Out-of-scope finding (another `_ACCESS_BOUNDARY.md` inconsistency, another Track B regression-test gap). Record in fix handoff and stop.

## When the fix is done

1. `sprint-020-fix-handoff.md` exists with two finding sections + verification + (any) out-of-scope observations.
2. Tests A and B pass; full server suite green; tail quoted.
3. `_ACCESS_BOUNDARY.md` reconciled per F2 Option A.
4. No file outside the four scoped paths edited.

Human commits the fix bundle (commit-at-end). You do not commit.
