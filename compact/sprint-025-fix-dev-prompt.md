Paste the content below this line into a fresh Claude Code session on branch `design-v1-without-human-review`.

---

# Sprint 25 fix iteration — dev prompt

## Loader stanza (read in this order before any edit)

1. `AGENTS.md` — repo constitution chain entry.
2. `docs/current/doc_governance.md` — tier model + source-of-truth rules.
3. `docs/current/iteration_governance.md` §1 (Constitution) + §3 (Fix Layer Classification) + §5 (Eval Acceptance Rules) + §7 (Required sprint-objective stanza).
4. `docs/sprint_objective.md` — read the **entire file**. The Sprint 25 parent objective (§1–§16) plus the appended `## Sprint 25 fix iteration` section are both authoritative. The fix-iteration section names exactly what you implement.
5. `docs/codex-findings.md` Finding 1 (lines 14–89) — the blocking evidence and the required-fix list. Read it verbatim; do not paraphrase.
6. `docs/sprints/sprint-025-handoff.md` — the file you edit. Pay particular attention to the four cited line ranges: **186–216** (DB query + placeholders + uncited driver), **172–184 vs 226–231** (chat extraction command vs rerank table with no command), **237–239** (derived deltas from incomplete recipes), **339 + 351** (overhead-delta claim with raw-printer `jq` and no Sprint 24 source path).
7. `eval_interactive/results/20260505-235231/results.json` — pre-`f2d4cb2` smoke (14 sessions). Source for the "pre" session_ids.
8. `eval_interactive/results/20260510-134558/results.json` — post-`f2d4cb2` smoke (14 sessions). Source for the "post" session_ids.
9. `eval_interactive/results/20260514-111724/results.json` — Sprint 25 smoke rerun (14 sessions, new `llm_calls` field present). Source for the Sprint 25 chat + rerank tables and the case-level `elapsed_ms` mean.
10. `docs/sprints/sprint-024-handoff.md` §4.1 — source path for the 52.9s mean comparison figure.

## Working tree at start

Expect deliver-agent-owned files to be uncommitted in your working tree at session start. Specifically: `docs/sprint_objective.md` carries the parent Sprint 25 objective plus the appended `## Sprint 25 fix iteration` section; `compact/sprint-025-fix-dev-prompt.md` (this file) and `compact/sprint-025-fix-review-prompt.md` may be present; `compact/sprint-deliver-orchestrator.md` may be updated; `docs/codex-findings.md` carries Codex's Sprint 25 review and is owned by the review agent. **Do not stage these yourself.** They are not your scope. For your fix commit, stage **only** `docs/sprints/sprint-025-handoff.md`. Avoid `git add -A` / `git add .`.

## The reproducibility bar (verbatim from parent §13)

> Every quantitative claim in the handoff (latency numbers, percentiles, sample counts, overhead deltas) MUST cite:
> 1. The exact source path.
> 2. The exact extraction command (literal `jq` filter, literal `psql -c "..."`, literal `python -c "..."`, or "manual eyeball" if no scripted extraction was used).
> 3. The aggregation window if the source has multiple plausible ones.
> 4. The derivation of any n-value.
> A claim that does not satisfy 1–4 is methodologically unsupported and Codex will block on it.

Sprint 25 was the sprint that baked this bar. The fix iteration closes the gap where the handoff itself fell short of it.

## What you do (the four gaps, one by one)

### Gap 1 — Placeholder session_id arrays + missing driver script

**Target:** `docs/sprints/sprint-025-handoff.md` §5.2, lines 186–216.

**What's missing:** lines 198 and 209 contain literal placeholder text `ARRAY[<14 session_id values, extracted via jq>]`. The driver was described as "a `python3 + psql` driver script during this sprint" with no script body provided.

**What to add:**

a. The `jq` command that extracts `session_id` from each smoke `results.json`. Use:

```
jq -r '[.case_results[].session_id] | unique' eval_interactive/results/20260505-235231/results.json
jq -r '[.case_results[].session_id] | unique' eval_interactive/results/20260510-134558/results.json
```

Run both; paste the literal output (the JSON array of UUIDs / session-id strings).

b. Replace both `ARRAY[<14 session_id values, extracted via jq>]` strings (lines 198 and 209) with literal SQL `ARRAY[...]` constructions populated from the jq output. SQL syntax: `ARRAY['<sid1>', '<sid2>', ..., '<sid14>']`. Each comma-separated value is a single-quoted string.

c. Inline the driver. Acceptable shapes (pick one — whichever you actually ran):

- A `psql ... <<'SQL' ... SQL` here-doc that contains the literal arrays inline (no Python wrapper). Self-contained; reader pastes and runs.
- A `python3 -c '...'` block that imports `psycopg2` / `subprocess`, builds the two arrays from the jq output, and runs the query. Inline the full script verbatim.

Either shape is fine, but it must be **executable as written** — no "extracted via" placeholders, no "driver script during this sprint" without the script body.

### Gap 2 — Rerank extraction command

**Target:** `docs/sprints/sprint-025-handoff.md` §5.2 around lines 172–184 and 226–231.

**What's missing:** the chat extraction command at lines 172–184 filters `callType == "chat"`. The table at lines 228–231 reports rerank values (n=176, p50=690, p95=998, max=1755) with no extraction command.

**What to add:** a sibling `python3 -c '...'` block immediately after the chat command, with the filter changed from `callType == "chat"` to `callType == "rerank"` (one-character edit on lines 180–182). Run it. The literal output must match the existing table row (n=176, p50=690, p95=998, max=1755). If it does not, **STOP and report** — do not silently update the table.

### Gap 3 — Mean-computation command + Sprint 24 source path

**Target:** `docs/sprints/sprint-025-handoff.md` §11 (line 339) and §12 (line 351).

**What's missing:**

- The cited `jq '.case_results[] | .elapsed_ms' eval_interactive/results/20260514-111724/results.json` only prints raw values; it does not compute the mean.
- The Sprint 24 source path for the 52.9s comparison mean is not cited inline.

**What to add:**

a. Replace the raw-printer `jq` with an executable mean-computation, e.g. `jq '[.case_results[].elapsed_ms] | add / length' eval_interactive/results/20260514-111724/results.json` (or an equivalent `python3 -c`). Run it; the output should round to ≈ 26139 (to match the existing claim at line 339). If it diverges, **STOP and report**.

b. Cite the Sprint 24 source path inline at line 339. Add: "The 14 case-level `elapsed_ms` values for the Sprint 24 post-`f2d4cb2` mean are recorded in `docs/sprints/sprint-024-handoff.md` §4.1 (per-case table); the underlying smoke artefact is `eval_interactive/results/20260510-134558/results.json`; the same mean reproduces via `jq '[.case_results[].elapsed_ms] | add / length' eval_interactive/results/20260510-134558/results.json`." Run that `jq`; it should round to ≈ 52900ms (52.9s). If it diverges, **STOP and report**.

### Gap 4 — Derived deltas (lines 237–239)

**Target:** `docs/sprints/sprint-025-handoff.md` §5.3, lines 237–239.

**What's missing:** the three derived-delta bullets ("p95 widened pre→post-`f2d4cb2` 8.4s → 11.7s → +3.3s", "Sprint 25 smoke lands at p95=10.0s", "synthetic baseline subtraction ≈9s") depend on the upstream §5.2 extractions. Once Gaps 1–3 close, the deltas inherit reproducibility as arithmetic on the cited tables.

**What to add:** no separate command. Confirm by re-running Gaps 1–2 and validating the existing tables (lines 218–224 DB rows, lines 228–231 Sprint 25 rerun rows, line 233 synthetic baseline). If any reproduction disagrees with the existing handoff numbers, **STOP and report** — the fix iteration explicitly forbids silently updating any number.

## Files in scope

- `docs/sprints/sprint-025-handoff.md` — the only file you edit.

## Files NOT in scope (hard fence)

- Any file under `eval_interactive/` (executor.py, agent_client.py, the new tests, the results.json artefacts — all final).
- Any file under `server/` (LlmSyntheticBaselineTest.java, runtime code, all final).
- Any file under `docs/sprints/sprint-NNN-*` other than `sprint-025-handoff.md` (all archives read-only).
- `docs/codex-findings.md` (review-agent owned).
- `docs/sprint_objective.md` (deliver-agent owned; the fix-iteration section was appended already).
- `docs/action_bank.md` (deliver agent updates at final close).
- All governance docs under `docs/current/`.
- All foundational docs under `docs/foundational/`.
- `docs/runtime_freeze_and_risk_policy.md`.
- All Sprint 25 parent fences still apply: no deadline-budget config, no model config, no `prompt_projection`, no eval-spec, no Tier-0, no Sprint 24-landed code, no Sprint 23 `system_prompt.txt`, no `AlreadyCalledPromptConsumptionTest.java`.

## Stop conditions

STOP immediately and report if you find yourself wanting to:

- Re-run the smoke. `eval_interactive/results/20260514-111724/results.json` is the artefact; Codex verified it. Do not regenerate.
- Edit any code file. The Sprint 25 commit range `1541e00..1b54b14` is final.
- Touch anything outside `docs/sprints/sprint-025-handoff.md`.
- Run a new DB extraction that changes the existing pre/post tables in handoff §5.2 (the numbers are correct per Codex's check; the recipes are what's missing).
- Reproduce a number and find it disagrees with the existing handoff table. The handoff was right per Codex; if your reproduction disagrees, you ran the recipe wrong, not the handoff. Report and ask before changing any cell.

## Fix iteration handoff (append to existing handoff)

Append a `## Fix iteration` H2 section to the end of `docs/sprints/sprint-025-handoff.md` (do NOT create a new file). Same convention as Sprint 21 / Sprint 23 fix-handoff. Content:

- One paragraph summarizing what the fix iteration closes (Codex Finding 1 reproducibility gap; four cited handoff line ranges).
- Four sub-sections (one per Gap above), each naming what was added and where in the handoff it now lives.
- A confirmation paragraph that no code changed; no other file changed; no Sprint 25 fence violated; no number revised.

## Commit

When all four gaps are closed:

```
git add docs/sprints/sprint-025-handoff.md
git commit -m "<your commit message>"
```

Do **not** `git add -A` / `git add .`. Stage only the handoff. Other working-tree files (sprint_objective fix-iteration append, compact/ prompts, codex-findings) are deliver-agent / review-agent owned and the human applies them separately.

## Return to deliver agent

After commit, report the commit SHA + a one-paragraph summary of what was added at each of the four cited handoff line ranges. The fix re-review will be dispatched to Codex per `compact/sprint-025-fix-review-prompt.md`.
