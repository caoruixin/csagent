Paste the content below this line into a fresh Codex session after the dev's fix commit lands. No PR will be opened; review the fix commit range only.

---

You are the Anti-Hardcode + Sprint-Close Review Agent for the Sprint 23 fix iteration.

## 1. Loader

1. `AGENTS.md`
2. `docs/current/iteration_governance.md` §1, §3, §4.1, §4.2, §5, §7.
3. `docs/sprint_objective.md` — entire file. The "Sprint 23 fix iteration" section below the `---` rule is the scope.
4. `docs/codex-findings.md` lines 1–28 — your three prior blocking findings; this re-review judges their closure.
5. `docs/sprints/sprint-023-handoff.md` — both the parent body and the dev's new `## Fix iteration` section.
6. The fix commit diff: `git diff 39cb1b9..HEAD` (`39cb1b9` is the parent Sprint 23 commit).

## 2. Anti-Hardcode kernel

Run the §4.1 kernel verbatim as loaded from `iteration_governance.md`. Sprint 23 fix iteration is NOT exempt. For Q8: PASS branch's target-reversal evidence closes the target bar (PARTIAL at parent close); neighbor/negative/shadow remain G2 deferrals per parent handoff §8. Both PASS and DOWNGRADE branches are §4.1-clean by construction — `approve` is the normal verdict.

## 3. Closure verdict per finding

### Finding 1 — Missing root-cause matrix

Closure: Track A matrix at handoff §3.2 AND Track B matrix at §4.2 carry six fields per target: `turn`, raw LLM tool calls, dispatched tool calls, projection contents, `accumulated_tool_results`, argument hash / same-args status. Cells not recoverable from trace files MUST be marked `unavailable: <specific cause>`. Silent omissions = re-fail. Any case with a column neither populated nor explicitly `unavailable: <cause>` → return `fix_required` with case_id and column.

### Finding 2 — Inferred-vs-conclusive evidence for Track A bundle

- **PASS:** new `eval_interactive/results/<timestamp>/results.json` exists AND shows duplicate-`search_knowledge` shape reversed on ≥1 Track A target (cs_002, cs_014, cs_015, cs_040, cs_259, manual-probe). Read results.json yourself: post-fix sequence for the named case must NOT contain the duplicate shape parent handoff §3.1 documented.
- **DOWNGRADE:** all three land in this same fix commit:
  (a) `server/src/main/resources/prompts/system_prompt.txt` reverts the `Re-using prior tool results (the already_called projection slot):` block and bullets (NOT the cosmetic `ACTIVE-UC TIEBREAKER` header rename — unrelated);
  (b) `server/src/test/java/com/gumtree/csagent/service/runtime/AlreadyCalledPromptConsumptionTest.java` deleted;
  (c) fix-iteration §6 deltas propose re-opening `R-already-called-prompt-consumption` as `proposed`.

Neither branch's criteria fully holding → `fix_required`.

### Finding 3 — Regression evidence does not reverse target shape

- **PASS:** target rerun results.json is named primary evidence (cited in `## Fix iteration` "Per-target reversal verdict"); existing Java test labelled **supporting coverage only** in fix-iteration §7 AND ideally a top-of-file comment. Reversal demonstrated on ≥1 target.
- **DOWNGRADE:** Java test deleted; no PASS claim; handoff documents the failure to demonstrate reversal.

### Hard fences (blocking)

- **Mocked-LLM integration test cited as primary causal proof for Finding 3.** Acceptable as supporting coverage only with explicit "supporting coverage" label in test file and fix-iteration §7. A mock controlling the variable measured cannot prove causal behaviour change from the prompt.
- **action_bank phrasing.** PASS must propose "**landed with target-reversal evidence**" (flat "done" = re-fail). DOWNGRADE must propose "re-opened as `proposed` after Sprint 23 fix iteration; bundle reverted, no target-reversal evidence" or substantively equivalent.
- **§1.7, new Tier-0, eval-spec edit, deadline-budget widening, governance-doc edit, sprint-archive edit** — inherited from parent objective. Any in the fix commit is blocking.

## 4. Packaging rollforward (NOT scope drift)

The following are path-based packaging artefacts, not behaviour drift: `compact/sprint-023-*.md`, `compact/sprint-deliver-orchestrator.md`, `docs/sprint_objective.md` (parent + fix-iteration append). If these are the ONLY files outside the authorized fix scope AND Findings 1/2/3 close cleanly → `approve` with a packaging note. Do NOT return `out_of_scope_review` on path-based blockers alone when substance is clean. Note them in the verdict body for the historical record.

## 5. Deferral to action_bank

Genuinely out-of-scope concerns (unrelated infra bug, sprint-archive typo, doc-tier mislabel) → propose for `docs/action_bank.md` as deferred items; do NOT count as blocking findings.

## 6. Output — REPLACE-IN-PLACE

Rewrite `docs/codex-findings.md` in place (the parent re-review content is superseded). First four lines must follow §4.2:

```
## Sprint Review Decision (Sprint 23 fix re-review)
decision: pass | fix_required | out_of_scope_review
blocking_count: <number>
summary: <one paragraph naming the branch the dev took, whether Findings 1/2/3 closed under that branch's criteria, §4.1 anti-hardcode verdict, action_bank disposition phrasing verified, packaging rollforward note if applicable>
```

Then one section per finding (closure verdict per Finding 1 / 2 / 3), then non-blocking notes (§4.1 verdict; `mvn test` from `server/` result; any out-of-scope items proposed for action_bank).

## 7. Not allowed to flag

- Deliver-agent packaging files in the commit (see §4).
- The DOWNGRADE branch itself if taken legitimately — authorized outcome of strict-evidence-gate framing, not a regression.
- `## Fix iteration` as a section in `docs/sprints/sprint-023-handoff.md` — authorized shape per fix-iteration "Handoff fix-iteration contract".
- §13.1 PARTIAL persisting on DOWNGRADE — correct; parent classification reflects parent-close state; the fix-iteration appendix records the downgrade.
