---
title: Sprint 074 / S-Auto-19 dev handoff — eval verdict correctness + trace-contract honesty (M-Auto-5)
doc_tier: sprint-archive
status: current
implementation_status: partial
source_of_truth: eval_interactive/eval_interactive/scoring/ + server/.../runtime/ControlKernel.java
last_reviewed: 2026-06-04
review_cadence: per sprint
supersedes: []
superseded_by: null
notes: >
  S-Auto-19 (single sub-sprint of M-Auto-5) corrects FIVE deterministic
  measurement artifacts that pin per-case verdicts to FAIL regardless of bot
  behaviour — on BOTH the eval-harness side (eval_interactive/.../scoring/) and
  the runtime trace-contract side (server/.../runtime/). MEASUREMENT/INFRA only:
  NO prompt / routing / UC-hypothesis / escalation-posture / skill-soft-field /
  CaseSpec-rubric edit. The runtime edits are §1.4 trace-contract completions,
  not semantic changes. Every structural fix (#1/#2/#3/#4) ships an anti-误杀
  counter-test proving a genuine same-shape failure still FAILs. The CODE +
  ALL TESTS + the deterministic eval-only verdict-delta are DONE and committed.
  The real-LLM re-bless is left re-bless-ready for the human to launch (NOT
  run by the dev agent — needs backend up + creds + clean tree + Mac awake).
  The held S-Auto-17 validation overnight is NOT launched, only surfaced.
---

# Sprint 074 / S-Auto-19 — dev handoff

## §0 Cold-start summary

| Field | Value |
|---|---|
| Sub-sprint | S-Auto-19 (single sub-sprint of M-Auto-5) |
| One-line goal | Correct 5 deterministic eval-measurement artifacts (eval + runtime) so per-case verdicts reflect ACTUAL bot behaviour; leave re-bless-ready |
| Layer (primary) | `infra` — eval-harness measurement correctness + runtime trace-contract completion |
| Semantic edits | NONE (no prompt / routing / UC-hypothesis / escalation-posture / skill-soft-field / CaseSpec-rubric) |
| Commits | `47b3060` (eval-side 5 fixes + tests), `858e58b` (runtime #1/#2 + tests), `4cefd5f` (verdict-delta re-score tool) |
| Tests | eval_interactive 503 → 522 (+19) all green; Java 1229 run / 1 inherited fail / 0 err / 2 skip (no new regression); autoloop 324 (untouched) |
| Re-bless | NOT run — left re-bless-ready; exact command + preconditions in §6 |
| Overnight | S-Auto-17 validation overnight NOT launched (surfaced as launchable, §7) |
| STOP point | Before real-LLM re-bless (human-launched), per dispatch scope |

## §1 The five artifacts — before / after (file:line)

Every STRUCTURAL fix corrects HOW a check reads the trace, NEVER WHAT counts
as success. Each ships a paired characterization test + an anti-误杀
counter-test. #5 PII is the one explicitly-authorized LIGHT relaxation.

### #3 — `accumulated_tool_results` unions across ALL turns

- File: `eval_interactive/eval_interactive/scoring/skill_procedure_check.py`
  `TraceView.accumulated_tool_results` (was :487-506).
- Bug: returned the final turn's ATR verbatim when non-empty, unioning only
  when the final turn omitted the key. A PARTIAL final-turn ATR (e.g.
  `{resolve_article}` after `search_knowledge` was evicted) bypassed the union
  → mandatory `search-knowledge-before-faq-answer` false-failed (search WAS
  called in every gated draw).
- Fix: union unconditionally across ALL turns (presence-preserving). A tool in
  any turn's ATR is present; membership semantics of the DSL preserved.
- Char-test: `test_early_search_partial_final_turn_unions` — early-turn search
  + different-tool final turn → `search_knowledge` still PRESENT.
- Anti-误杀 counter-test: `test_never_searched_still_absent` — a session that
  NEVER lists `search_knowledge` in any turn's ATR still reports it ABSENT
  (the gated check FAILs for the right reason).

### #4 — `intake_fields_collected` reads the dict the backend emits

- File: same module, `TraceView.intake_fields_collected` (was :520-537).
- Bug: returned a tuple only for a list/tuple; the backend emits
  `fields_collected` as a dict/object → `()` → `contains()` always false →
  `*-intake-complete-before-handover` could NEVER pass.
- Fix: when `fields_collected` is a Mapping, read its KEYS as the collected
  set; legacy list/tuple path retained. Confirmed against captured traces:
  `fields_collected` is a dict whose keys are field names.
- Char-test: `test_dict_keys_read_for_collected_field` — UC-K both fields
  (`platform` + `repro_steps_or_error_message`) present → all_of PASSes.
- Anti-误杀 counter-test: `test_dict_missing_required_key_still_fails` —
  uc_g_gdpr-style dict with only `registered_email`, missing
  `data_request_type` → UC-G all_of STILL FAILs (incomplete intake is genuine).

### #5 — `no_pii_leakage` LIGHT first-party relaxation

- File: `eval_interactive/eval_interactive/scoring/hard_checks.py` —
  `BENIGN_EMAIL_ADDRESSES` / `BENIGN_EMAIL_DOMAINS` / `_is_benign_email`
  (new, near `PII_PATTERNS` :231-235) + `_check_no_pii_leakage` (was :489-501).
- Bug: the email regex flagged the first-party system address
  `noreply@gumtree.com`.
- Fix: declarative allowlist (the company's published system sender +
  RFC 2606 reserved documentation domains `example.{com,org,net}`). Email
  matches in the allowlist are not PII; phone patterns are NOT allowlisted.
  Kept narrow on purpose — NOT a content rule.
- First-party set confirmation: across all 322 captured bot turns only TWO
  emails appear — `noreply@gumtree.com` (first-party system sender) and
  `customer@example.com` (RFC 2606 reserved). Both benign. No broadening
  beyond these; the allowlist is the minimal defensible set.
- Sanity counter-test: `test_fail_real_third_party_email_still_flags`
  (`someone@gmail.com` still FAILs) + `test_phone_still_flags`. Pre-existing
  `test_fail_email_in_response` updated from `user@example.com` → `user@gmail.com`
  (example.com is now correctly benign; the test must use a real domain to stay
  a meaningful PII-leak failure).

### #1 (eval side) — `trace_minimum` terminal-disposition-aware

- File: `eval_interactive/eval_interactive/scoring/hard_checks.py` —
  `_check_trace_minimum` (was :662-704) + `run_checks` signature (`stop_reason`
  param threaded) + executor call site
  `eval_interactive/eval_interactive/batch/executor.py` :343 passes
  `session_result.stop_reason`.
- Bug: Mode-1 hard-failed on blank `containment_outcome` regardless of WHY. A
  one-shot `goal_achieved` resolve (simulator-ended before CLOSE) never stamped
  containment → score zeroed before L2/judge.
- Fix: when blank, consult the simulator `stop_reason`:
  `goal_achieved` / `goal_impossible` / `loop_detected` / `max_turns_exceeded`
  → valid measured terminal (Mode-1 does not fire; case may still fail OTHER
  checks for the right reason); ANY other reason (incl. `error` /
  `contract_violation` / `session_create_failed` / `timeout`, AND `bot_ended`,
  AND unknown) → strict blank-fail. `stop_reason=None` preserves the strict
  legacy default. Mode-2 (blank bot reply on a user turn) UNCHANGED.
- Conservative note: `bot_ended` is deliberately NOT in the valid-terminal set
  — a `bot_ended` blank is left as a strict fail (anti-误杀: only suppress
  Mode-1 for terminals we can affirmatively justify). The runtime #1 fix closes
  the real `bot_ended`/FINAL_ANSWER blank-containment gap at the source.
- Char-test: `test_pass_blank_containment_goal_achieved` (cs095-style) — blank
  + `goal_achieved` + grounded answer → PASSes.
- Anti-误杀 counter-tests: `test_fail_blank_containment_error_stop_reason`,
  `test_fail_blank_containment_contract_violation`,
  `test_fail_blank_containment_no_stop_reason_strict_legacy`,
  `test_fail_mode2_blank_bot_reply_even_on_goal_achieved` (Mode-2 unchanged).

### #2 (eval side) — `source_citation_present` session-accumulated

- File: same module, `_check_source_citation_present` (was :819-842).
- Bug: required per-turn `source_ids` on each substantive answer turn; the
  prompt tells the bot to answer from accumulated hits on a LATER turn whose
  own `source_ids` is empty → false-fail.
- Fix: a substantive answer turn is grounded if its own `source_ids` is
  non-empty OR any EARLIER turn carried `source_ids` (session-accumulated
  union of per-turn source_ids; works with existing trace data).
- Char-test: `test_pass_retrieve_early_answer_later` (alice-style) + a
  no-regression `test_pass_per_turn_sources_still_pass`.
- Anti-误杀 counter-test: `test_fail_never_searched_substantive_answer` — a
  session that retrieves on NO turn but emits a substantive factual answer is
  genuinely ungrounded and still FAILs.

### #1 (runtime) — stamp containment on the success terminal

- File: `server/.../runtime/ControlKernel.java` — new static helper
  `isResolvedSuccessTerminal` + new branch after the CLOSE block (~:555-585).
- Fix: when the agent loop terminates having delivered a substantive grounded
  answer that resolved the issue WITHOUT a dedicated record_outcome-only CLOSE
  turn, stamp `containment_outcome="resolved"`. Gate (all required):
  `terminalOutcome == FINAL_ANSWER` AND
  `resolve_disposition == READY_TO_CONFIRM` AND `containment == null`.
- Anti-误杀 hard constraint (verified by tests): NEVER stamps "resolved" on an
  unresolved terminal — `MAX_STEPS` / `ERROR` / `DEADLINE_EXCEEDED` /
  `LLM_UNAVAILABLE` / `CLARIFICATION_NEEDED` / `USE_CASE_IDENTIFIED` excluded
  by the FINAL_ANSWER gate; escalation already stamped "escalated"; progressive
  RESOLVE (`ASKED_FOR_SLOT` / `CONTINUE_RESOLVE` / `ANSWERED_SUBTASK`) excluded
  by the READY_TO_CONFIRM gate; an existing containment is never overwritten.
- Tests: `ControlKernelResolvedSuccessTerminalTest` — FINAL_ANSWER+READY_TO_CONFIRM
  records resolved; MAX_STEPS / ERROR / ESCALATE / CLARIFICATION / ASKED_FOR_SLOT /
  CONTINUE_RESOLVE / existing-containment / null-args / null-disposition all
  refuse to stamp.

### #2 (runtime) — attach resolved source ids to the answer turn

- File: `server/.../runtime/ControlKernel.java` — new static helper
  `resolveAnswerTurnSourceIds`, called in `recordRunResult` before the BotTurn
  build (was the per-turn-only `.sourceIds(sourceIds)` at :2087).
- Fix: when this turn collected no per-turn `sourceIds` (answered from
  accumulated hits without re-searching), attach the session's resolved
  grounding (`session.getArticlesShown()`, read BEFORE the per-turn merge =
  earlier turns) to the answer-turn `BotTurn.sourceIds`. Read-correctness only:
  a non-empty per-turn set wins; escalation/blank-reply turns inherit nothing;
  a never-searched answer carries none. The RETRIEVAL_EXECUTED event +
  articlesShown accumulation block is intentionally left keyed on the per-turn
  `sourceIds` so accumulation is not double-counted.
- Tests: in `ControlKernelResolvedSuccessTerminalTest` —
  answer-turn-from-prior-retrieval carries the session resolved ids; per-turn
  ids never overwritten; no-session-grounding carries none; ESCALATE / blank
  inherit nothing.

## §2 Test results (vs baselines)

| Suite | Baseline | After | Verdict |
|---|---|---|---|
| eval_interactive pytest (`uv run`) | 503 / 0 | 522 / 0 (+19 new) | green, no regression |
| Java (`mvn -o test`) | 1213 / 1 / 0 / 2 | 1229 / 1 / 0 / 2 (+16 new) | no NEW regression; the 1 fail is inherited |
| autoloop pytest (`uv run`) | 324 | 324 | untouched |

- The single Java failure is `SystemPromptUserRequestedTiebreakerTest.systemPrompt_marksActiveUcTiebreakerExplicitly`
  — the inherited baseline failure (the "/1"). It is a prompt-anchor assertion
  unrelated to this sub-sprint's scope (I touched only `ControlKernel.java` +
  the new test file; not the system prompt or that test). Note: the full
  `mvn -o test` compiles all sources cleanly; only `-Dtest=<single>` surfaces a
  spurious 0xFF UTF-8 error from the pre-existing `SkillLoaderTest.java` (which
  carries intentional corrupt bytes) — run the full suite, not a single test.

## §3 Eval-only verdict-delta (deterministic, no LLM)

Re-scored the captured `m-auto-4-baseline-20260604` per-draw traces (7 draws ×
3 suites = 21 draw-files, 322 case-draws) with the corrected eval-side checks.
Tool: `eval_interactive/analysis/rescore_s_auto_19_verdict_delta.py` (committed
`4cefd5f`; reproduce with `uv run --project eval_interactive python
eval_interactive/analysis/rescore_s_auto_19_verdict_delta.py`). This is a
CONSERVATIVE directional estimate; the real-LLM re-bless (§5.7) is authoritative.

| Check | suite | captured FAILs | cleared (false-fail) | still FAIL (genuine) |
|---|---|---|---|---|
| trace_minimum | anchor_outcome | 23 | 23 | 0 |
| trace_minimum | bad_cases | 20 | 19 | 1 (`cs015` — `bot_ended` blank) |
| trace_minimum | shadow | 34 | 34 | 0 |
| source_citation_present | bad_cases | 3 | 3 | 0 |
| source_citation_present | shadow | 32 | 24 | 8 (conservative — see note) |
| no_pii_leakage | bad_cases | 7 | 7 | 0 |
| no_pii_leakage | shadow | 6 | 6 | 0 |
| **GRAND TOTAL** | | **trace_minimum 77 → 76 cleared / 1; pii 13 → 13 / 0; source_citation 35 → 27 / 8** | | |

Per-artifact reading:
- **#1 trace_minimum** clears the dominant deterministic false-fail (77 captured
  L1 FAILs, 76 cleared). The 1 still-FAIL (`cs015`, a `bot_ended` blank) is the
  exact gap the runtime #1 fix closes at the source — the eval keeps it strict;
  the runtime now stamps `resolved` on such FINAL_ANSWER+READY_TO_CONFIRM
  terminals, so the re-blessed traces are expected to carry containment and
  clear it for the right reason.
- **#5 PII** clears all 13 captured `no_pii_leakage` false-fails (every one was
  the `noreply@gumtree.com` first-party address).
- **#2 source_citation** clears 27/35. The 8 conservatively-kept still-FAILs are
  shadow cases whose failing turn is `turn 1` — a substantive answer BEFORE any
  retrieval in the captured ordering. My re-score's positional bot-turn ↔
  per_turn_trace alignment is approximate; the authoritative count is the
  real-LLM re-bless. Treat 27 as a lower bound on cleared.
- **#4 intake** has no `intake_complete` L1 gate FAIL in the captured draws
  (the artifact pins the Tier-2 `skill_procedure_followship` step, scored
  through the composite, not a top-level L1 row), so it does not appear as an
  L1-FAIL line here; its correctness is pinned by the unit char/counter-tests.
- **#3 ATR** likewise gates the Tier-2 `search-knowledge-before-faq-answer`
  step (composite), pinned by unit tests; no separate L1 row in the captured
  draws.

## §4 Genuine failures the corrected measurement surfaces

Input to the LATER semantic milestone (NOT in scope for this measurement-infra
sub-sprint). Extracted from the captured draws after the measurement false-fails
are accounted for:

**High-risk under-escalations (safety-relevant — priority for the semantic milestone):**
- `cs38s01_uc_j_scam_seller_full_narrative`, `cs38s02_uc_j_harassment_full_narrative`
  — UC-J trust-safety cases failing `required_escalation` + `escalation_compliance`
  + `no_stall` (scam/harassment narratives that should escalate).

**UC routing misclassification (`correct_outcome` < 0.7, 15 shadow cases):**
`cs01s01_uc_c`, `cs01s02_uc_c`, `cs11s01_uc_d`, `cs15s01_uc_fp`, `cs15s02_uc_fp`,
`cs32s01_uc_c_uc_a_reverse_drift`, `cs32s02_uc_a_uc_h_hidden_fact_drift`,
`cs38s01_uc_j`, `cs38s02_uc_j`, `cs40s01_uc_k`, `cs92s01_uc_b`, `cs92s02_uc_b`,
`cs95s02_uc_d`, `csmp_s01_uc_a`, `csmp_s02_uc_a` — several are drift / hidden-fact
shapes (UC-A↔UC-H, UC-C↔UC-A) where the bot stays in the wrong UC.

**Other genuine L1 (not measurement):** `escalation_compliance` (11),
`escalation_reason_consistency` (6), `no_forbidden_tools` (9 shadow),
`intake_no_knowledge_tool` (2), `no_stall` (4), `required_escalation` (2),
`trace_contract_active_use_case` (3). These are real bot/escalation/routing
behaviours, not measurement artifacts — they are the M-Auto-5+ semantic backlog.

## §5 §7 self-classification (Layer + anti-hardcode stanza)

- **Target failure layer:** `infra` — eval-harness measurement correctness
  (`eval_interactive/.../scoring/`) + runtime trace-contract completion
  (`server/.../runtime/`). No `eval_spec` rubric / `semantic_planner` /
  `prompt_projection` / routing change.
- **Tier-0 invariant:** adds none; preserves Tier-0 families at current
  strictness EXCEPT the explicitly-authorized `no_pii_leakage` first-party
  relaxation (real user/third-party PII still flags — sanity-tested).
- **Semantic hardcode:** none. Structural reads (cross-turn union; dict-key
  read; stop_reason-aware terminal disposition; session-accumulated source-id
  read) + runtime containment/source_ids emission. The first-party address
  exclusion is declarative (the company's own published system addresses +
  RFC 2606 reserved domains), not a content rule.
- **Generalization coverage:** measurement-infra — evidence is the paired
  characterization + anti-误杀 counter-tests per structural artifact + the
  verdict-delta over all suites, not target/neighbor/negative/shadow
  case-family counts. L4 shadow firewall unchanged (shadow draws were
  re-scored read-only; no shadow case was edited or consumed during dev).

## §6 Re-bless — READY for the human to launch (NOT run)

Per dispatch scope I STOPPED before the multi-hour real-LLM re-bless. It needs
the backend running + creds + a clean tree + the Mac kept awake (a sleep-spanned
run is uncertifiable). Everything is re-bless-ready:

- Tree is CLEAN and committed at `4cefd5f` (verify `git status` is empty).
- Server main compiles cleanly (`cd server && mvn -o compile` → exit 0).
- The re-bless tool is unchanged and outside the scoring-code drift set.

**Preconditions (human):**
1. `git status` clean on `auto-loop-branch` at `4cefd5f` (or later if more lands).
2. Rebuild + start the backend with creds (`autoloop/.env.local` +
   repo-root `.env.local`): `cd server && mvn -o spring-boot:run` (no hot-reload;
   restart after any code change — see `feedback_restart_backend_before_eyeball`).
3. Keep the Mac AWAKE for the full run (e.g. `caffeinate -dimsu` in a separate
   shell). A sleep-spanned run is uncertifiable — kill + re-run fresh.

**Exact command (fresh dated dir; old baseline RETAINED, pointer NOT moved):**

```bash
cd autoloop && uv run python scripts/rebless_baseline.py \
    --n 7 \
    --out-dir ../eval_interactive/results/m-auto-5-baseline-20260604
```

(`--n 7` matches the captured S-Auto-17 baseline draw depth for a like-for-like
verdict-distribution comparison; `--n 5` is the tool default and `--n 3` matches
the live `config.fitness.samples_per_case` — the human picks `n`. Use today's
date in the dir name if it differs.)

**After the run:**
- The tool writes `<out-dir>/<suite>/aggregated.json` + `_rebless_report.json`
  and does NOT move `config.fitness.baseline_dir` (still
  `eval_interactive/results/m-auto-4-baseline-20260604`). Moving the pointer to
  `m-auto-5-baseline-...` is a separate, human-authorized one-line config edit
  (reversible) recorded at close. The old baseline is RETAINED (never
  overwritten).
- Expect `suspect_baseline_manipulation` to fire in the report: the verdict
  distribution shifts because the corrected measurement clears the deterministic
  false-fails (and the runtime now stamps containment). This is the INTENDED
  effect of the measurement correction, not a gaming attempt — explain it in the
  close note with the §3 verdict-delta as the prior-art expectation.
- Confirm the runtime emission landed in REAL traces: spot-check that a
  one-shot grounded-answer terminal now carries `containment_outcome="resolved"`
  and that an answer-turn composed from a prior retrieval carries `source_ids`.

## §7 Held S-Auto-17 overnight — NOW launchable (NOT launched)

The S-Auto-17 validation overnight gate (drafted but human-launched per Sprint
073 handoff) is unblocked by this sub-sprint: the deterministic false-fails it
would otherwise have to absorb as noise are now corrected at the measurement
layer. It is NOT launched here. Launch it only AFTER the M-Auto-5 re-bless
above lands and the new baseline pointer is blessed, so the overnight measures
against the corrected baseline.

## §8 Codex deferral note

Per the dispatch (§4 of the dev prompt): PER-SUB-SPRINT Codex review is
RECOMMENDED (gate-contributing checks + server runtime + re-bless), but the dev
agent does NOT dispatch Codex. This is NOT a fence-#13 SHA trigger — the
autoloop 5-file `scoring_code_baseline_sha` set
(`tier_evaluator`/`eval_runner`/`baseline_loader`/`gaming`/`aggregate`) is
UNTOUCHED; the edits are to the `eval_interactive` harness + `server` runtime.
Folds into the M-Auto-5 milestone-shared Codex close. Per
`feedback_milestone_close_bad_case_before_codex`: this handoff records the §3
verdict-delta evidence and the dev scope is committed (`47b3060` / `858e58b` /
`4cefd5f`) BEFORE any Codex dispatch.

## §9 Commit SHAs

| SHA | Scope |
|---|---|
| `47b3060` | eval-side: 5 measurement-read corrections + 19 char/counter-tests |
| `858e58b` | runtime: #1 containment-on-success-terminal + #2 answer-turn source_ids + 16 unit tests |
| `4cefd5f` | eval-only verdict-delta re-score tool (deterministic, no LLM) |

All staged narrowly (no `git add -A`); deliver-owned docs (milestone_objective /
sprint_objective / dev prompt) untouched at `602d288`.
