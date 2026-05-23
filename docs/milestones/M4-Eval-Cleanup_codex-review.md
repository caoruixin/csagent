<!-- ARCHIVE NOTE (deliver-agent, M4-Eval-Cleanup close 2026-05-24): archived M4-Eval-Cleanup milestone-shared Codex review. TWO passes ran against the cumulative range 4344662..d5b1508:
  - First pass: `fix_required / blocking_count: 1` — P0-F1 (the §5.6 bad-case close package was not yet present in _manifest.md + cs029 was a zero-turn CONTRACT_VIOLATION in run 20260523-075141). The code / anti-hardcode axis was already CLEAN on the first pass (Q1-Q9 PASS, §1.7 PASS, hard fences PASS, Java 1163/1/0/2, Python 3 failed/460 passed).
  - Re-review (the verdict below): `pass / blocking_count: 0` after the deliver-agent completed the bad-case close package (M4 ledger in eval_interactive/case_specs/bad_cases/_manifest.md + cs029 isolated rerun 20260523-095557) and committed S-Cleanup-3 (d5b1508). P0-F1 CLEARED.
P0-F1 was a deliver-agent close-package TIMING artifact (Codex reviewed before the deliver-agent finished the §5.6 bad-case manual review), NOT a dev-code issue; no sub-sprint code change was requested in either pass. -->

## Sprint Review Decision
decision: pass
blocking_count: 0
summary: M4-Eval-Cleanup can close as A — Clean PASS. The first-pass code-axis verdict carries over to the now-committed range `4344662..d5b1508`: `git show d5b1508 --stat` contains only eval-harness, eval-fixture/docs, and prompt/handoff surfaces, while `git diff 4344662..d5b1508 -- server/` is empty. The §4.1 kernel remains clean: #9 scopes Tier-2 by observable `phase_plan.critical_steps[].id` state, #4 shrinks the deterministic gate surface by demoting `handover_completeness` / `case_id_present`, and no prompt, Skill YAML, Tier-0, tool-schema, PII, or grounding surface changed. The first-pass blocker P0-F1 is cleared: `_manifest.md` now contains the 2026-05-24 M4 bad-case manual-review ledger, the cs029 isolated rerun resolves the prior zero-turn `CONTRACT_VIOLATION` as a session-establishment flake, and the human-judgment distribution reproduces M3 exactly (PASS×5 / IMPROVING×4 / FAIL×3 / OOSR×0) with zero `escalate-via-request-handover` Tier-2 misflips.

## Review Evidence

- Re-review scope: the prior first-pass review (superseded by this file) plus the committed close range `4344662..d5b1508`.
- `git log --oneline 4344662..d5b1508` now shows S1 dev `1576070`, S1 close `8ccedbf`, S2 dev `b989833`, S2 close `36ade6e`, governance `8f32dbd`, and S3 dev + bad-case close `d5b1508`.
- `git show d5b1508 --stat` shows `executor.py`, `composite.py`, `test_composite_gate.py`, `test_s_eval_5_l3_repositioning.py`, new `test_tier2_phase_plan_scoping.py`, `docs/sprints/sprint-049-handoff.md`, `eval_interactive/case_specs/bad_cases/_manifest.md`, the two `compact/*prompt.md` files, and `docs/sprint_objective.md`; no `server/` file appears.
- `git diff 4344662..d5b1508 -- server/` is empty; the headline hard fence remains honored.
- The first-pass targeted validations remain the applicable test evidence: Java `1163 / 1-inherited / 0 / 2`; Python targeted 14 + 49 + 20 passes; full Python via `uv run python -m pytest --tb=no -q` => `3 failed, 460 passed`. The exact `uv run pytest` console-script segfault remains the local stale-shebang issue documented under OQ-S47.3.
- Re-review spot-checked bad-case traces from `eval_interactive/results/20260523-075141/results.json` and the cs029 isolated rerun `eval_interactive/results/20260523-095557/results.json` with `jq`.

## Resolved First-Pass Blocker

| Finding | First-pass status | Re-review result |
|---|---:|---:|
| P0-F1 — Bad-case primary-gate close package missing / cs029 zero-turn contract violation prevents regression-safety verification | BLOCKING | CLEARED |

Evidence:
- Ledger exists: `eval_interactive/case_specs/bad_cases/_manifest.md:198` adds `M4-Eval-Cleanup close bad-case suite manual review (2026-05-24)`.
- Run paths are explicit: main 12-case run at `eval_interactive/results/20260523-075141/results.json` and isolated cs029 rerun at `eval_interactive/results/20260523-095557/results.json` (`_manifest.md:206`, `_manifest.md:207`).
- The manifest records final human-review verdicts unchanged from M3 (`_manifest.md:209`) and the exact distribution PASS×5 / IMPROVING×4 / FAIL×3 / OOSR×0 (`_manifest.md:226`, `_manifest.md:233`).
- cs029 isolated rerun has `total_turns=3`, `stop_reason=bot_ended`, `active_use_case=UC-D`, `escalation_reason=user_requested`, empty `failure_tags`, and `case_passed_authority=human_review`; the prior `075141` zero-turn result is therefore consistent with the already-tracked `R-bad-case-parallel-session-establishment-flakiness` shape, not a behavior regression.

## §3 Nine-Question Kernel Results

The first-pass Q1-Q9 verdicts stand unchanged after sanity-checking `d5b1508`. No surprising committed diff appeared.

| Q | Verdict | Re-confirmation |
|---:|---:|---|
| Q1 — Semantic hardcode added? | PASS | #9 reads runtime-presented `per_turn_trace[].phase_plan.critical_steps[].id` generically (`executor.py:69`, `executor.py:489`) and keeps only per-step results whose `step_id` is in that presented set (`executor.py:496`). No case-id, UC-name matrix, user-message regex, or step-id literal routes behavior. #4 is a surface shrink: `_conditional_mandatory_l2` returns `()` (`composite.py:44`, `composite.py:69`). |
| Q2 — Tier-0 invariant protection? | PASS / N/A | M4 adds no Tier-0 invariant. #9 narrows an eval gate by observable trace state; #4 demotes L2 process checks. `runtime_freeze_and_risk_policy.md` and `hard_checks.py` are not changed by `d5b1508`. |
| Q3 — Soft signal replacing hard branch? | PASS | #9 consumes the runtime projection as the soft/procedural signal; #4 moves `handover_completeness` / `case_id_present` from gate contributor to advisory observation. |
| Q4 — Eval phrase / trace-specific phrase / CaseSpec id encoded? | PASS | S3 helper is generic over `critical_steps[].id`; S2 human-review authority keys only on suite names from `_OPT_IN_SETS`, not individual cases (`sets.py:20`, `sets.py:24`). |
| Q5 — LLM ownership shrunk? | PASS | No `server/`, prompt, or Skill YAML diff. LLM-owned semantics remain untouched; changes are eval-harness interpretation/reporting. |
| Q6 — Prompt if-else added? | PASS | No system-prompt diff in the committed M4 range. |
| Q7 — Tool schema / capability / PII / grounding floor preserved? | PASS | Tool schema, capability, PII, and FAQ grounding surfaces are server/runtime-owned and have no diff. #4 demotes L2 process-completeness, not safety. |
| Q8 — Generalization coverage? | PASS | S3 tests cover target, neighbor, multi-phase negative, defensive empty/absent `phase_plan`, and `mandatory_for` interplay; first-pass targeted run confirmed 63 relevant passes. Bad-case acceptance is now separately cleared by the human-review ledger. |
| Q9 — Rollback / sunset plan? | PASS / N/A | #9 is a durable correctness fix and #4 is a durable pyramid-intent demotion. The rollback path is `git revert d5b1508` if the final committed bundle needs reversal. |

## §1.7 Boundary Check

| Forbidden-list item | Verdict | Evidence |
|---|---:|---|
| Raw eval phrases encoded into Java or prompt | PASS | No Java/prompt diff; #9 uses trace step ids, not user-visible phrases. |
| UC-specific hard rules for soft semantics | PASS | No runtime UC matrix; UC literals are limited to tests/fixtures. |
| Eval spec widened to accept a bot mistake | PASS | #4 demotes process-completeness gates to advisory; mandatory `correct_uc` / `correct_outcome` behavior is not relaxed. |
| Visible eval optimized at shadow/generalization cost | PASS | No shadow reads; S3 negative-control tests prevent over-narrowing. |
| Prompt as if-else dump | PASS | Prompt untouched. |

## §4 Hard-Fence Walk Results

| # | Fence | Verdict | Evidence |
|---:|---|---:|---|
| 1 | No `server/src/main/java/**` runtime touch | PASS | `git diff 4344662..d5b1508 -- server/` is empty. |
| 2 | No `system_prompt.txt` edit | PASS | No prompt file appears in `git show d5b1508 --stat` or the cumulative server diff. |
| 3 | No Skill YAML edit | PASS | No `server/src/main/resources/skills/*.yaml` diff; #9 fix is eval-side consumption of presented `critical_steps`, not a Skill declaration edit. |
| 4 | No `docs/milestones/M3-Eval_*` edit | PASS | No M3 milestone archive appears in the committed M4 diff. |
| 5 | No editing immutable sprint archives | PASS with note | `d5b1508` adds new S49 handoff archive; no prior sprint archive is modified. |
| 6 | No new Tier-0 invariant / no runtime freeze edit | PASS | No `docs/runtime_freeze_and_risk_policy.md` change; no hard-check addition. |
| 7 | No §1.7 forbidden-list edit | PASS | `8f32dbd` did not alter the forbidden-list substance; `d5b1508` does not edit governance text. |
| 8 | No §5.5 / §5.6 rewrite except minimal clarification | PASS | No new §5.5 / §5.6 rewrite in `d5b1508`; #4 demotion is captured in eval docs/manifest and code docstrings. |
| 9 | No new bad cases / no schema-breaking bad_cases change beyond Alice | PASS | `d5b1508` edits `_manifest.md` only under `bad_cases/`; no new case or schema-breaking fixture change. |
| 10 | No `eval_interactive/case_specs/shadow/` reads | PASS | No code path introduced to read the held-out shadow set. |
| 11 | No M4+ candidate slate work | PASS | No Single Handover Orchestrator, UC-G/H/I/J seeding, semantic-planner soft-signal, M3-Corpus, Latency, Skill-Tuning, or Tier-0 work landed. |

## §5 Bad-Case Regression-Safety Verification

### §5(a) Ledger Present + Trace-Consistent

The M4 close ledger is present and explicit (`_manifest.md:198` through `_manifest.md:247`). Spot-checks against `eval_interactive/results/20260523-075141/results.json` support the manifest summaries:

| Case | Manifest verdict | Trace spot-check | Codex check |
|---|---:|---|---|
| `cs066_uc_k_in_app_feature_regression` | PASS | Bot asks the platform intake field at T1 and escalates at T2; result has `active_use_case=UC-K`, `escalation_reason=intake_complete_for_uc_k`, failure tags `L2:case_id_present` and `TIER2:uc-k-intake-complete-before-handover`. | Consistent with `_manifest.md:216`: UC-K path + platform intake + intake handover; residual Tier-2 sub-fail is below the human closure threshold and `case_id_present` is informational after #4. |
| `alice_uc_a_uc_h_misclass` | IMPROVING | Bot routes `active_use_case=UC-A`, explains the ad was removed, cites posting rules, and escalates at T2 without entering the UC-H intake-lock dead loop. | Consistent with `_manifest.md:218`: key UC-A/no-dead-loop win plus generic explanation/template sub-quality, so human-owned IMPROVING verdict is evidence-backed. |
| `cs015_uc_fp_appeal_edit_repost` | FAIL | Bot routes `active_use_case=UC-A`, gives generic email/check-spam removal guidance, does not retrieve moderation context, and escalates after the user asks. | Consistent with `_manifest.md:222`: the UC-FP appeal/edit-repost failure shape persists. Codex is not re-judging the human FAIL verdict. |

All 12 bad-case results in the main run and the cs029 isolated rerun have `case_passed_authority = "human_review"`, so programmatic `case_passed` remains informational per §5.6.

### §5(b) Regression-Safety

- Manifest lifecycle ledger now has an `M4 result` column, and M4 matches M3 row-for-row: PASS×5 (`cs001`, `cs014`, `cs029`, `cs066`, `fg5q`), IMPROVING×4 (`alice`, `cs011`, `cs012`, `wmkb`), FAIL×3 (`cs015`, `cs095`, `iwzx`).
- No M3-PASS case flipped to FAIL. The only OOSR candidate from the main run (`cs029`) cleared in the isolated rerun, so OOSR×0 is supported.
- `jq` over `20260523-075141` confirms zero cases with `escalate-via-request-handover` in `tier2_result.failed_step_ids`; #9's zero-misflip property holds on the bad-case suite.
- Two `L1:no_pii_leakage` tags appear (`cs011`, `cs012`), matching `_manifest.md:237`. Transcript inspection confirms benign email-regex false positives: `noreply@gumtree.com` is a system FAQ address, and `kitten.seller@example.com` is the user's own fixture/example address echoed back. No genuine third-party PII leak is evidenced.

### §5(c) #4 Demotion Operational

- `jq` over `20260523-075141` finds zero `L2_GATE:handover_completeness`, `L2_GATE_MISSING:handover_completeness`, `L2_GATE:case_id_present`, or `L2_GATE_MISSING:case_id_present` tags.
- `cs066` still records `L2:case_id_present` informationally, proving the score is computed/serialized while no longer gating.

## §6 OQ Disposition Table

| OQ | Codex assessment | Recommended disposition |
|---|---|---|
| OQ-S49.1 — Residual 148/159 anchor FAILs | First-pass assessment stands: anchor before/after verified `case_passed=true` 0 -> 11 and `escalate-via-request-handover` failed-step count 119 -> 0. Residual clusters are L2/L1/contract-trace or legitimate presented-step Tier-2 failures, not the #9 all-Skills bug. | `closed-with-followup` — route residual clusters to existing M4+ planning or a future contract-trace R-item if desired; not an M4 close blocker. |
| OQ-S49.2 — §5.6 governance fold-in for #4 | First-pass assessment stands. No broad §5.6 governance edit was needed; #4 demotion is sufficiently carried by `composite.py` docstring, M4 manifest, and close evidence. | `closed` |
| OQ-S49.3 — Composite threshold vs gate | First-pass assessment stands. After-fix anchor has 11 `case_passed=true` and 0 `case_passed && composite_score >= 0.7`; this is reporting/observability, not correctness. | `closed-with-followup` — already fits `R-eval-report-observability`. |
| OQ-S47.3 — Python baseline framing | First-pass assessment stands. Exact `uv run pytest --tb=no -q` uses a stale local `.venv/bin/pytest` shebang and segfaults; `uv run python -m pytest --tb=no -q` is the reliable command in this checkout and gives `3 failed, 460 passed`. | `closed-with-followup` — record the fallback command / consider regenerating the venv script; no M4 regression. |

## Schema / Reproducibility Checks

| Claim | Result | Evidence |
|---|---:|---|
| S3 work committed | PASS | `git log 4344662..d5b1508` includes `d5b1508`; current tracked diff is this review file only, with no code diff. |
| Zero server/runtime touch | PASS | Empty `git diff 4344662..d5b1508 -- server/`. |
| S1 opt-in suite handling | PASS | `_KNOWN_SETS=('anchor','promotion','exploration','smoke')`, `_OPT_IN_SETS=('bad_cases','anchor_outcome')`; `load_set('all')` iterates `_KNOWN_SETS` only (`sets.py:20`, `sets.py:86`). |
| S2 human-review authority | PASS | Bad-case main and isolated runs report unique `case_passed_authority` as `human_review`; S2 result-builder paths remain in the committed code. |
| #9 empirical anchor delta | PASS | First-pass `jq` verification stands: before/after anchor `case_passed=true` 0 -> 11 and `escalate-via-request-handover` failed-step count 119 -> 0 (`20260523-051234` vs `20260523-063218`). |
| #9 targeted count | PASS | First-pass targeted run stands: 14 `test_tier2_phase_plan_scoping` + 49 composite/L3 tests = 63 passed. |
| Bad-case M4 distribution | PASS | Manifest M4 column counts exactly 5 PASS / 4 IMPROVING / 3 FAIL; distribution section records OOSR×0 after cs029 isolated rerun (`_manifest.md:226`). |
| cs029 isolated rerun | PASS | `20260523-095557`: 1 case, 3 turns, `stop_reason=bot_ended`, `active_use_case=UC-D`, `escalation_reason=user_requested`, empty failure tags, `case_passed_authority=human_review`. |
| No #9 bad-case misflip | PASS | `jq` count for `escalate-via-request-handover` in bad-case `failed_step_ids` is 0. |
| #4 demotion on bad cases | PASS | `jq` count for `L2_GATE:*handover_completeness*` / `L2_GATE*case_id_present*` tags is 0. |
| Java baseline | PASS | First-pass `cd server && mvn test -q` => inherited `SystemPromptUserRequestedTiebreakerTest.systemPrompt_marksActiveUcTiebreakerExplicitly:53`; `Tests run: 1163, Failures: 1, Errors: 0, Skipped: 2`. |
| Python full-suite count | PASS with environment note | First-pass reliable command `uv run python -m pytest --tb=no -q` => `3 failed, 460 passed`; exact console-script command is local-env stale-shebang issue. |

## Optional Codex-Surfaced New Findings

No blocking findings remain. Non-blocking observations to preserve for close-out judgment:

- **P3-F1 — S49 handoff bad-case summary is stale relative to the final M4 manifest.** `docs/sprints/sprint-049-handoff.md:257` says all 12 bad cases ran end-to-end and `docs/sprints/sprint-049-handoff.md:291` says the programmatic split was 6 true / 6 false. The result file has cs029 as a zero-turn `CONTRACT_VIOLATION`, and `jq` shows 5 `case_passed=true` / 7 false in `20260523-075141`. The final `_manifest.md` correctly supersedes this with the cs029 isolated rerun and the human-review distribution, so this is documentation drift in the S49 handoff, not a close blocker.
- **P3-F2 — `outcome_checks.py` behavior is correct but comments remain stale after #4.** The retained `_CASE_ID_UCS` / `_uc_family` helpers are intentional score-computation helpers, and scores are still serialized. But comments at `outcome_checks.py:71`, `outcome_checks.py:81`, and `outcome_checks.py:88` still describe `handover_completeness` / `case_id_present` as conditionally mandatory and mirrored with `composite._conditional_mandatory_l2`, while `composite.py:69` now returns `()`. Fix layer if addressed later: `eval_spec` docs/comment cleanup, not gate logic.
- **P3-F3 — Local pytest console script remains stale.** `.venv/bin/pytest` points at another checkout, so `uv run pytest` segfaults before collection; `uv run python -m pytest` is the reproducible command for this checkout.

## Cumulative Architecture Coherence Judgment

M4-Eval-Cleanup now ships as a coherent cleanup milestone. S1 keeps human-judgment suites out of `--set all`, S2 annotates `case_passed_authority` so bad-case results cannot be mistaken for programmatic gates, and S3 aligns Tier-2 with M3-Eval intent by consuming the runtime's presented `critical_steps` while demoting process-completeness dimensions to advisory trend signals. The load-bearing #9 implementation matches the design memo's Option (b): no fallback to all-Skills on empty traces, no Skill YAML `mandatory_for` edit, and no runtime/server change.

The first-pass close blocker was evidence timing, not architecture. With the M4 bad-case ledger present and cs029 resolved by isolated rerun, the §5.6 human-judgment gate is regression-safe: the M3 verdict distribution is reproduced exactly, safety-floor false positives are note-only and trace-explained, #9 produces zero bad-case escalate-step misflips, and #4 is observable as score-retention without gate failure. The remaining issues are minor documentation/reproducibility cleanups and do not prevent M4-Eval-Cleanup from closing.
