Paste the content below this line into a fresh Claude Code session on branch `design-v1-without-human-review`.

---

You are the dev agent for the Sprint 23 fix iteration. Parent close returned `fix_required` with three P1 blockers. Resolve them under a **strict evidence gate** with a built-in **PASS / DOWNGRADE branch** in this same fix commit. No third round.

## 1. Loader

1. `AGENTS.md`
2. `docs/current/iteration_governance.md` §1 (Constitution), §3 (Fix Layer Classification), §5 (Eval Acceptance Rules), §7. §1.7 is the hard fence on what NOT to do.
3. `docs/sprint_objective.md` — read the entire file. The "Sprint 23 fix iteration" section below the `---` rule is your scope.
4. `docs/codex-findings.md` lines 1–28 — the three blocking findings, verbatim.
5. `docs/sprints/sprint-023-handoff.md` §3.1 / §3.2 / §4.1 / §4.2 / §5 / §11 / §13.1 (lines 256–426, 622–698, 854–871).
6. The trace files under `eval_interactive/results/20260510-134558/` (the per-session transcript JSON files siblings of `results.json`).

Do not load other sprint archives. Do not load Sprint 19 / 20 case-family content.

## 2. Strict evidence gate (verbatim)

Keep the `already_called` prompt-consumption change and the existing test for now, but do not close Track A as PASS until there is **target-reversal evidence**. Prefer a real smoke / target rerun over a mocked-LLM alternative — a mocked LLM controls the variable being measured and cannot prove causal behaviour change from the prompt. If at least one Track A target shows reversal, update §11 / §13.1 from PARTIAL to PASS and phrase the action_bank status precisely as "**landed with target-reversal evidence**," not flat "done." If the rerun is infeasible or shows no reversal, downgrade in-flight: revert the prompt edit, delete `AlreadyCalledPromptConsumptionTest`, flag the R-item for re-open as `proposed`.

A mocked-LLM integration test is acceptable as supporting coverage only — NOT as primary causal proof.

## 3. Steps

### Step A — Matrix augmentation (Finding 1; both branches)

For each Track A target (cs_002, cs_014, cs_015, cs_040, cs_259, manual-probe per handoff §3.1) and each Track B target (cs_002, cs_014, cs_040, cs_015, cs_038, cs_259, cs_176 per §4.1), open the per-session trace files under `eval_interactive/results/20260510-134558/` and re-derive these six columns (per turn where turns matter):

1. `turn`
2. raw LLM tool calls (LLM's emitted `tool_calls` for that turn, before dispatch)
3. dispatched tool calls (actual ToolEvent records, or the `l2_results.tool_sequence_match.detail` proxy)
4. projection contents (the `already_called` slot state + a one-line excerpt of the projection payload)
5. `accumulated_tool_results` contents
6. argument hash / same-args status per call

For any column not recoverable from the trace files, write `unavailable: <specific cause>` (e.g. `unavailable: results.json snapshot lacks per-turn ToolEvent payloads`). No silent omissions.

Replace the matrices at `docs/sprints/sprint-023-handoff.md:346` (Track A) and `:532` (Track B), or append clearly-labelled "Augmented matrix (Sprint 23 fix iteration)" subsections alongside the existing tables — your discretion on layout. Every target case must have all six columns or explicit `unavailable: <cause>` cells.

### Step B — Target rerun (Findings 2+3, PASS-attempt)

Harness is `eval_interactive` (CLI registered in `eval_interactive/pyproject.toml`). From the `eval_interactive/` directory:

```
eval-interactive run --path case_specs/smoke/cs_interactive_040.yaml --label sprint23_fix_cs040_rerun
```

Or for the full smoke set: `eval-interactive run --set smoke --label sprint23_fix_target_rerun`.

cs_040 has the strongest evidence per handoff §3.2:351 (turn 1 `[sk, sk, ra]` with no intervening user turn) — defensible single-case rerun. cs_014 (5 `search_knowledge` calls, 3 consecutive) is the second-strongest alternative.

Pre-flight:
- Backend up at `http://localhost:8080` (`eval_interactive/eval_interactive.yaml:2`). If not reachable → rerun **infeasible** → DOWNGRADE.
- LLM provider env vars populated (`DASHSCOPE_BASE_URL`, `DASHSCOPE_API_KEY`, `DASHSCOPE_CHAT_MODEL`). If not → rerun **infeasible** → DOWNGRADE.

After running: record the new `eval_interactive/results/<timestamp>/` path. For each rerun target, compare the post-`39cb1b9` tool sequence against the original 2026-05-10 sequence. Reversal = the duplicate `search_knowledge` shape is gone. PASS branch needs reversal on ≥1 target.

### Step C — Branch

**Step C.PASS (reversal on ≥1 target):**

1. Update handoff §13.1 (line 854) PARTIAL → PASS with the rerun cite.
2. Update handoff §11 (lines 967–986): propose disposition phrasing "**landed with target-reversal evidence**" (NOT "done").
3. Append `## Fix iteration` section to handoff with subsections per §4 below.
4. Existing `AlreadyCalledPromptConsumptionTest.java` stays. Optionally add a top-of-file comment: `// Supporting coverage for Sprint 23 prompt teaching; not primary evidence for behaviour reversal.`

**Step C.DOWNGRADE (no reversal OR infeasible):**

1. Revert `server/src/main/resources/prompts/system_prompt.txt` for the teaching paragraph the parent commit `39cb1b9` added (the `Re-using prior tool results (the already_called projection slot):` block + bullets + trailing blank line). `git show 39cb1b9 -- server/src/main/resources/prompts/system_prompt.txt` shows the exact addition. Do NOT revert the cosmetic `ACTIVE-UC TIEBREAKER` header rename — unrelated.
2. Delete `server/src/test/java/com/gumtree/csagent/service/runtime/AlreadyCalledPromptConsumptionTest.java`.
3. Append `## Fix iteration` section to handoff with subsections per §4 below.
4. §13.1 PARTIAL stays in the parent body (it accurately reflects parent-close state); the fix-iteration appendix records the downgrade.

## 4. Handoff fix-iteration contract

Do NOT create a new file. Append a `## Fix iteration` section to `docs/sprints/sprint-023-handoff.md`. Subsections:

1. **Branch taken:** PASS or DOWNGRADE.
2. **Augmented matrix:** Finding 1 closure summary; pointer to augmented §3.2 / §4.2.
3. **Rerun command + results path:** exact command, new results.json path. If infeasible, specific reason + diagnostic command output.
4. **Per-target reversal verdict (PASS) / per-target failure verdict (DOWNGRADE):** one row per attempted target with before/after tool sequences.
5. **§11 / §13.1 status update (PASS only):** quote the new PASS phrasing.
6. **action_bank disposition phrasing:** exact string the deliver agent will apply. PASS = "landed with target-reversal evidence". DOWNGRADE = "re-opened as `proposed` after Sprint 23 fix iteration; bundle reverted, no target-reversal evidence".
7. **Mocked-LLM supporting coverage status:** "none added" OR test path + the explicit "supporting coverage only" label.
8. **Cause of downgrade (DOWNGRADE only):** "no reversal on N target reruns" OR "rerun infeasible: <reason>".
9. **Anti-hardcode self-walk:** one paragraph (not the full nine questions — those were walked at parent §7).

## 5. Files in scope

Always: `docs/sprints/sprint-023-handoff.md` (append + matrix augmentations + on PASS update §11 / §13.1); `eval_interactive/results/<new-rerun-dir>/**` (new from harness).

DOWNGRADE only: revert `server/src/main/resources/prompts/system_prompt.txt` to `HEAD~1` state for the teaching paragraph lines; delete `server/src/test/java/com/gumtree/csagent/service/runtime/AlreadyCalledPromptConsumptionTest.java`.

## 6. Files NOT in scope (hard fence)

- No new Track A bundled fix beyond the existing teaching (or its revert on DOWNGRADE).
- No Track B work of any kind.
- No eval-spec edits (`eval_interactive/case_specs/**`, `case_spec_overrides.yaml`, `case_specs_shadow/**`, persona files, `eval_interactive/scripts/llm_review_specs.py`).
- No Sprint 20 case-family edits.
- No foundational doc edits (`docs/foundational/**`).
- No governance doc edits (`docs/current/**`).
- No sprint-archive edits (`docs/sprints/sprint-001-*.md` through `docs/sprints/sprint-022-*.md`).
- No edit to `docs/sprint_objective.md` (deliver-agent owned).
- No deadline / timeout configuration edit.
- No model configuration edit.
- No new prompt edit beyond the revert-on-DOWNGRADE scenario.

## 7. Mocked-LLM constraint (hard fence)

A mocked-LLM integration test is acceptable as **supporting coverage only**. NOT acceptable as primary causal evidence for behaviour reversal. The real-LLM target rerun is the evidence gate. If you add one:
- Label "supporting coverage" in a top-of-file comment.
- List in fix-handoff §7 under "Mocked-LLM supporting coverage status".
- Do NOT cite as Finding 3 closure evidence.

If you find yourself constructing a mock LLM that "bakes in" duplicate-call avoidance (e.g. mock emits one call instead of two given the new prompt), STOP — that measures the mock's configuration, not the prompt's causal effect.

## 8. §1.7 hard gate

Any matrix entry that would mask a real bot mistake on a target case → STOP and surface. No rubric widening; no scope shrink to avoid recording an inconvenient cell.

## 9. Stop conditions

- Rerun returns no `results.json` on target case(s) → DOWNGRADE branch (infeasible).
- Rerun shows duplicate-call shape persists on every attempted target → DOWNGRADE branch (no reversal).
- Trace JSON files don't carry required columns AND no alternative source exists → `unavailable: <cause>` cells are acceptable per Finding 1's recommended_action; STOP only if even the cause is unknown.
- Tempted to use mocked-LLM integration test as primary evidence → STOP, see §7.
- Tempted to claim PASS without target-reversal evidence → STOP, you are on DOWNGRADE.
- Tempted to defer the downgrade to a follow-on sprint → STOP, resolve in this iteration.

## 10. Working tree at start

Expect deliver-agent-owned files uncommitted in your working tree:
- `docs/sprint_objective.md` carries the fix-iteration append below the parent objective.
- `compact/sprint-023-fix-dev-prompt.md`, `compact/sprint-023-fix-review-prompt.md`, `compact/sprint-deliver-orchestrator.md`.

Do NOT stage or edit these yourself; do not be surprised if the human bundles them at commit time. They are not your scope. If you use `git add` for your fix-commit, stage only the files you authored under §5; avoid `git add -A` / `git add .` for this specific commit.

Commit message stem: `sprint 23 fix: <branch> — augment matrices, target rerun, <action_bank phrasing>`.

## 11. Definition of done

PASS: Findings 1, 2, 3 close per parent fix-iteration objective's "Success metrics (the strict evidence gate)" subsection. `mvn test` from `server/` is 898/0/0/1.

DOWNGRADE: Finding 1 closes via augmented matrix; Findings 2/3 close via revert + delete + R-item re-open phrasing in handoff §11. `mvn test` is 897/0/0/1.
