Paste the content below this line into a fresh Codex session after the dev's fix commits land on `design-v1-without-human-review`.

---

# Sprint 25 fix re-review — Codex prompt

## Loader stanza

Read in this order before issuing any verdict:

1. `AGENTS.md` — repo constitution chain.
2. `docs/current/iteration_governance.md` §1 (Constitution) + §3 (Fix Layer Classification) + §4.1 (Anti-Hardcode Review Prompt) + §4.2 (Sprint-close header) + §5 (Eval Acceptance Rules) + §7 (Required sprint-objective stanza).
3. `docs/sprint_objective.md` — read the full file. The parent Sprint 25 objective (§1–§16) and the appended `## Sprint 25 fix iteration` section are both authoritative. The fix iteration section names the bounded review scope.
4. `docs/sprints/sprint-025-handoff.md` — the file under review. Pay attention to the four originally-cited line ranges (186–216, 172–184/226–231, 237–239, 339/351) and the new `## Fix iteration` H2 section appended at the end.
5. The originally-cited `docs/codex-findings.md` Finding 1 (lines 14–89 at fix-dev-session start) — your own prior verdict; the fix iteration closes the single blocker named there.

## Review scope (bounded)

This is a fix re-review of a **single in-scope blocker** (`docs/codex-findings.md` Finding 1: reproducibility hygiene on handoff §§5, 6, 11/12). The fix is documentation-only: the dev edited `docs/sprints/sprint-025-handoff.md` and committed. **No code changes are expected.** The Sprint 25 commit range `1541e00..1b54b14` is final; the fix commit's only file change should be `docs/sprints/sprint-025-handoff.md`.

Run the §4.1 Anti-Hardcode kernel verbatim as loaded from `iteration_governance.md`. Sprint 25 fix iteration is **exempt** from §4.1 substantive review (it is a docs-only fix on a sprint-archive handoff). Return `approve` with a one-line note naming the exemption. Your re-review verdict lives in the §4.2 sprint-close header, not the per-PR kernel.

## What you verify (the four cited gaps closed)

1. **Handoff §5.2 placeholder arrays + driver.** The two `ARRAY[<14 session_id values, extracted via jq>]` placeholders (originally at lines 198, 209) replaced with literal `ARRAY['<sid1>', ..., '<sid14>']`. A `jq` command extracting session_ids from each pre/post smoke `results.json` cited and reproducible. The driver inlined (either a `psql ... <<'SQL' ... SQL` here-doc or a `python3` block) — no remaining placeholder, no "driver script during this sprint" without script body.

2. **Handoff §5.2 rerank extraction command.** A `python3 -c '...'` block (sibling of the chat block) filtering `callType == "rerank"` present alongside lines 172–184, reproducing the existing rerank row (n=176, p50=690, p95=998, max=1755).

3. **Handoff §11/§12 mean-computation + Sprint 24 source path.** The raw-printer `jq` (originally at line 339) replaced/supplemented by an executable mean-computation (`jq '[.case_results[].elapsed_ms] | add / length'` or `python3 -c` equivalent). Sprint 24 source-path citation present inline: `docs/sprints/sprint-024-handoff.md` §4.1 + underlying `eval_interactive/results/20260510-134558/results.json`.

4. **Handoff §5.3 derived deltas (lines 237–239).** No separate command required — once Gaps 1–3 close, the deltas inherit reproducibility as arithmetic on the cited tables. Confirm the bullets still hold against the now-reproducible upstream extractions.

## Hard-fence violations (any → BLOCKING)

- Any code change in the fix commit (`eval_interactive/`, `server/`, anywhere outside docs).
- Any new smoke run (no new `eval_interactive/results/*` directory expected).
- Any data revision: if a handoff table cell (pre/post DB rows at lines 218–224, Sprint 25 rerun rows at lines 228–231, synthetic baseline at lines 133–136 / line 233, mean values at line 339) has changed in the fix commit, that is a fence violation. The dev was instructed to STOP and report on any reproduction-vs-table disagreement, not silently update.
- Any edit to a file outside `docs/sprints/sprint-025-handoff.md`.
- Any Sprint 25 parent fence violation (deadline-budget, model config, prompt_projection, eval-spec, Tier-0, Sprint 24-landed code, Sprint 23 `system_prompt.txt`, `AlreadyCalledPromptConsumptionTest.java`). See parent objective §12 for the full list.

If any of the above appears in the fix commit, return `decision: fix_required` with a fence-violation finding.

## Packaging-rollforward rule

The fix-dev prompt instructed the dev to stage only `docs/sprints/sprint-025-handoff.md`. If the dev nonetheless bundled deliver-agent files (`docs/sprint_objective.md`, `compact/sprint-025-fix-*-prompt.md`, `compact/sprint-deliver-orchestrator.md`) into the fix commit, treat as **packaging artefact** per the established A-with-packaging-note pattern. Note the bundled files in Non-Blocking Checks; do NOT raise as blocking unless the bundled content itself violates a fence (e.g. one of those files contains a code edit).

## Verdict

Replace the existing `## Sprint Review Decision` header at the top of `docs/codex-findings.md` **in place** with a new §4.2 header:

```
## Sprint Review Decision (Sprint 25 fix re-review)
decision: pass | fix_required | out_of_scope_review
blocking_count: <number>
summary: <one paragraph>
```

If the four cited gaps are closed and no fence is violated, return `pass`. If a fence is violated or a gap remains open, return `fix_required` with finding(s). If your review surfaces an issue outside the bounded fix scope, deferral to `docs/action_bank.md` is the right path — do NOT broaden this fix re-review to that issue.

After the §4.2 header, list any non-blocking checks (packaging-rollforward note, anti-hardcode kernel exemption note, observations on handoff style).

Do not rewrite the handoff. Do not propose code fixes. The fix re-review is bounded to confirming the four cited gaps closed.
