Paste the content below this line into a fresh Codex session for the **M4-Eval-Cleanup P0-F1 RE-REVIEW**. It supersedes the first-pass verdict currently at the top of `docs/codex-findings.md`. No PR will be opened; review the committed range directly.

---

You are the M4-Eval-Cleanup Milestone-Close Re-Review Agent. A prior Codex session already produced a first-pass milestone-close review of **Milestone M4-Eval-Cleanup** (per `docs/milestone_objective.md`), currently at the top of `docs/codex-findings.md`, with verdict `fix_required / blocking_count: 1`. That first pass found:

- **Code / anti-hardcode axis CLEAN**: the §4.1 nine-question kernel Q1–Q9 all PASS; §1.7 boundary check all PASS; the §6 hard fences honored (the headline — zero `server/` touch — confirmed by an empty `git diff --stat -- server/`); Java baseline `1163 / 1-inherited / 0 / 2`; Python `3 failed, 460 passed` via `uv run python -m pytest` (the `uv run pytest` console-script segfault is a stale `.venv/bin/pytest` shebang per OQ-S47.3 — environmental, not an M4 regression).
- **ONE blocking finding — P0-F1**: the §5.6 bad-case primary-gate close package was MISSING (no M4 close section in `eval_interactive/case_specs/bad_cases/_manifest.md`), and the cited bad-case run (`results/20260523-075141`) had `cs029_uc_d_account_locked_callback` — an M3-PASS case — as a zero-turn `CONTRACT_VIOLATION`, so the M4 acceptance bar's required `PASS×5 / IMPROVING×4 / FAIL×3 / OOSR×0` regression-safety distribution could not be verified.

P0-F1 was a **premature-review timing artifact**: the deliver-agent + human completed the §5.6 bad-case close package AFTER the first pass ran. Both gaps are now resolved and committed. Your job is to **re-verify P0-F1**; the code-axis verdict stands unless task A below shows the committed code differs from what the first pass reviewed.

This is a CLEANUP milestone (eval-harness-side only; no new bad case; no new runtime semantic surface). The §5.6 bad-case suite is a **human-judgment gate**, NOT a programmatic threshold (2026-05-17 refinement) — you verify the close package is consistent with the traces; you do NOT re-judge the per-case PASS / FAIL / IMPROVING verdicts (those are the deliver-agent + human's qualitative call).

## What changed since the first pass

1. **The S-Cleanup-3 work is committed.** The M4-Eval-Cleanup cumulative range is now `4344662..d5b1508` (`git log --oneline 4344662..d5b1508` = S1 dev `1576070` + S1 close `8ccedbf` + S2 dev `b989833` + S2 close `36ade6e` + governance `8f32dbd` + **S3 dev + bad-case close `d5b1508`**). The first pass reviewed the S3 code as an uncommitted working-tree diff; it is now in `d5b1508`.
2. **The M4 bad-case close package now exists** in `eval_interactive/case_specs/bad_cases/_manifest.md`: a new `## M4-Eval-Cleanup close bad-case suite manual review (2026-05-24)` section (Posture / Run paths / per-case verdict table / Overall pattern / Tier-0 safety floor / #9+#4 operational confirmation / Decision) + a new `M4 result` column in the lifecycle ledger table.
3. **cs029 has a clean isolated rerun**: `results/20260523-095557/results.json` (label `s-cleanup-3-cs029-isolated`).

## Re-verification tasks

**A. Confirm the code-axis verdict carries over (sanity check, NOT a full redo).** Verify the committed S3 code equals what your first pass reviewed:
- `git show d5b1508 --stat` — expect `executor.py` + `composite.py` + `test_composite_gate.py` + `test_s_eval_5_l3_repositioning.py` + `test_tier2_phase_plan_scoping.py` (new) + `sprint-049-handoff.md` + `_manifest.md` + the two `compact/*prompt.md` + `sprint_objective.md`; and NO `server/` file.
- `git diff 4344662..d5b1508 -- server/` — expect EMPTY (the headline hard fence).
If both hold, your first-pass Q1–Q9 / §1.7 / hard-fence verdicts stand unchanged (the reviewed code is identical, now committed). Do NOT re-litigate the passed kernel unless a diff surprises you.

**B. P0-F1 §5(a) — ledger now present + consistent.** Read the `## M4-Eval-Cleanup close bad-case suite manual review (2026-05-24)` section of `_manifest.md`. Spot-check at least 3 cases (1 PASS + 1 IMPROVING + 1 FAIL) by comparing the recorded verdict + reasoning against the actual bot transcript:
```bash
jq -r '.case_results[] | select(.case_id=="<id>") | .transcript[] | select(.role=="bot") | "[\(.turn_index)] \(.message)"' eval_interactive/results/20260523-075141/results.json
```
Suggested: `cs066_uc_k_in_app_feature_regression` (PASS), `alice_uc_a_uc_h_misclass` (IMPROVING), `cs015_uc_fp_appeal_edit_repost` (FAIL). Confirm the manifest's reasoning matches the trace.

**C. P0-F1 cs029 resolution.** Verify `results/20260523-095557/results.json`: cs029 ran 3 turns, `stop=bot_ended`, `active_use_case=UC-D`, `escalation_reason=user_requested`, and the ALL-CAPS turn did NOT trigger `user_distress` → closure-criterion PASS. Confirm the `075141` `CONTRACT_VIOLATION` was a session-establishment flake (not a behaviour change), tracked to `R-bad-case-parallel-session-establishment-flakiness`.
```bash
jq '.case_results[] | {status, total_turns, active_use_case, escalation_reason, failure_tags}' eval_interactive/results/20260523-095557/results.json
```

**D. P0-F1 §5(b) — regression-safety.** Confirm the M4 distribution reproduces M3 exactly: `PASS×5` (cs001, cs014, cs029, cs066, fg5q) / `IMPROVING×4` (alice, cs011, cs012, wmkb) / `FAIL×3` (cs015, cs095, iwzx) / `OOSR×0`; no M3-PASS case flipped to FAIL. The two `no_pii_leakage` programmatic tags (cs011 T1, cs012 T3) are benign email-regex false positives (the FAQ's system `noreply@gumtree.com`; the user's own echoed `kitten.seller@example.com`), NOT third-party PII leaks — recorded **note-only** per the human's 2026-05-24 disposition; the Tier-0 safety floor is preserved. Confirm `escalate-via-request-handover` is absent from every case's `tier2_result.failed_step_ids` (the #9 fix's zero-misflip property holds on the suite).

## Output

Rewrite `docs/codex-findings.md` with the updated verdict (delete-and-add supersession at the top of the file, per `feedback_packaging_codex_findings_supersession.md`). If P0-F1 clears, `decision: pass / blocking_count: 0`. Carry forward the §4.1 kernel + §1.7 + hard-fence results from the first pass, noting they were re-confirmed against `d5b1508` (task A). Include the §6 OQ-disposition table — if the first pass already dispositioned OQ-S49.1 / OQ-S49.2 / OQ-S49.3 / OQ-S47.3, carry those; you already diagnosed OQ-S47.3 (stale-shebang → `uv run python -m pytest` = `3 failed, 460 passed`).

Do NOT edit code, sprint archives (`docs/sprints/*`), or the milestone objective archive that lands at `docs/milestones/M4-Eval-Cleanup_objective.md` at close (deliver-agent owns that). Do NOT propose code fixes beyond naming the `iteration_governance.md` §3 layer. The full first-pass kernel context, if you need it, is in `compact/M4-Eval-Cleanup-review-prompt.md`.
