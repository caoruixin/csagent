---
title: Sprint 090 / S-Auto-35 (M-Auto-7 S-Y1.5b) handoff — autoloop anti-hardcode baseline-whitelist suppression patch
doc_tier: sprint-archive
status: current
implementation_status: implemented
source_of_truth: autoloop/autoloop/sandbox/anti_hardcode_check.py, autoloop/tests/test_anti_hardcode_check.py
last_reviewed: 2026-06-10
review_cadence: per sprint
supersedes: []
superseded_by: null
notes: >
  Pure autoloop/ INFRA patch (Layer §3.2 = infra; §7 stanza EXEMPT). Adds
  baseline-whitelist suppression at the anti_hardcode_check() entry point so a
  propose-stage rule match is ignored when the matched substring already exists
  in the candidate's untouched before_value. Closes the verified Q1 (+ symmetric
  Q2/Q4/Q5) false-positive that killed pure-additive candidates exp-70/exp-76 on
  resolve_faq_grounded_answer.yaml $.procedure pre-eval. NO rule-body regex
  change; NO _normalize change; NO scoring/ change → scoring_code_baseline_sha
  0d86b08f… reproduces → NO re-bless. RCA + design:
  docs/solutions/2026-06-09-anti-hardcode-q1-pure-additive-false-positive.md
  (Option B). Dev scope is autoloop/** only (detector + tests); docs/close
  artefacts bundled by the human at close. Per-sub-sprint Codex (§4.3 trigger #3)
  REQUIRED before the S-Y2 Part C `-n 3` verification re-tranche.
---

# Sprint 090 / S-Auto-35 — M-Auto-7 S-Y1.5b — autoloop anti-hardcode baseline-whitelist suppression handoff

## Class

| Field | Value |
|---|---|
| **Layer (§3.2)** | `infra` — autoloop propose-stage sandbox detector |
| **§7 stanza** | **EXEMPT** — pure infra; detector *scope* refinement only; no semantic surface, no rule-body regex change, no `_normalize` change |
| **Per-sub-sprint Codex (§4.3)** | **REQUIRED** (trigger #3 — modifies the §1.7 anti-hardcode defense). Runs AFTER this dev close, BEFORE the downstream S-Y2 re-tranche |

## §1 — What shipped

Baseline-whitelist suppression at the `anti_hardcode_check()` entry point,
generalized **uniformly across all 11 rules** (Q1/Q2/Q4/Q5 families). A rule
match is now ignored when the substring it matched already exists in the
candidate's untouched `before_value`. Two-part change:

1. **Entry-point suppression** (`anti_hardcode_check.py` ~line 404): compute
   `before_norm = _normalize(hypothesis.before_value or "", synonym_map_enabled=synonym_enabled)`
   ONCE at entry, using the SAME `synonym_map_enabled` flag already read for the
   `after_value` normalization. `before_norm` is threaded into every rule helper
   call (`fn(normalized, before_norm)`).

2. **Rule helpers refactored first-match → iterate** via a new shared helper
   `_first_new_match(regexes, text, before_norm, *, predicate=None, render=None)`
   (`anti_hardcode_check.py` ~line 123). It iterates every match of every regex
   (`finditer`) and returns the trimmed display string of the **first match that
   (a) satisfies the rule's existing predicate, and (b) whose normalized regex
   span `m.group(0)` is NOT present in `before_norm`**. `render(m)` reproduces
   the window-display rules (Q2-tier0 ±80, Q4-id_assign −20/+40) verbatim;
   suppression is always tested against `m.group(0)`, never the rendered window.
   The `matched_substring` reported in the verdict stays in `after_value`
   coordinates (today's `_trim` display behavior unchanged).

**Preserved exactly** (verified byte-identical in the diff): all Q1–Q5 rule-body
regexes; `_normalize` (incl. `_SYNONYM_MAP`, `_RE_WHEN_WORD_BOUNDARY`); the
`_SOFT_SEMANTIC_DIMENSIONS` membership predicate for Q2-must / Q5-do-not-consider;
the Q2-tier0 add-verb window predicate; the `_RULES` registry order; the
FLAG_FOR_CODEX path for `Q5.standalone_must_borderline`; the empty/whitespace
`after_value` early-return.

**No-op invariant:** when `before_value` is empty/absent, `before_norm` is empty
→ the suppression set is empty → `_first_new_match` returns the first
predicate-qualifying match, which for non-predicate rules is byte-identical to
the previous `re.search` first-match. Verified empirically: the real exp-70/76
`after_value` with an EMPTY baseline still returns `FAIL / Q1.if_then_decision_tree`.

## §2 — LOC

| File | +added | −removed |
|---|---|---|
| `autoloop/autoloop/sandbox/anti_hardcode_check.py` | 104 | 59 |
| `autoloop/tests/test_anti_hardcode_check.py` | 144 | 0 |

(`git diff --numstat HEAD`. Detector net +45; tests purely additive.)

## §3 — New tests + pass counts

5 new cases in `test_anti_hardcode_check.py`, all green:

- `test_q1_arrow_tree_baseline_match_suppressed` — REAL exp-70 `(before, after)`
  pair (embedded verbatim, JSON-safe) → **PASS**. The arrow-tree span is 100%
  baseline text; the 1478-char additive clause adds no new Q1 match.
- `test_q1_arrow_tree_new_match_still_caught` — clean baseline + candidate-added
  `if … -> …` clause → **FAIL** (`Q1.`). Proves suppression does not fire on a
  candidate-introduced pattern.
- `test_q1_if_then_baseline_match_suppressed` — synthetic IF/THEN in baseline,
  pure-additive after → **PASS**.
- `test_q2_q4_q5_baseline_match_suppressed` — Q2 (must-always vs soft dimension),
  Q4 (case-id token), Q5 (force-assistant) baselines + clean additive → **PASS**
  for every family.
- `test_double_encoding_bypass_documented` — a `.contains(` pattern once in
  baseline, copied verbatim a second time in after → **PASS**; the docstring
  documents the accepted substring-suppression trade-off (see §6).

Non-vacuity: every "suppressed → PASS" case above was independently verified to
return **FAIL** when the same `after_value` is paired with an EMPTY baseline, so
the PASS is a genuine suppression and not a clean-prose pass.

File-level: `test_anti_hardcode_check.py` + `test_real_meta_agent_calibration.py`
= **39 passed** (34 prior + 5 new).

## §4 — Full-suite pytest before / after

| | Result | Command |
|---|---|---|
| Before | **331 passed**, 1 warning (144.62s) | `python -m pytest tests/ -q` |
| After  | **336 passed**, 1 warning (120.05s) | `python -m pytest tests/ -q` |

Δ = +5 (the new cases). No prior test regressed. The 1 warning is the
pre-existing `test_layer4_shadow_missing_keeps_with_warning` baseline-missing
UserWarning (unchanged).

## §5 — 17-fixture calibration — UNCHANGED

`test_real_meta_agent_calibration.py` exercises the 3 sanitized real-meta-agent
samples (exp-2/exp-3/exp-4) plus the 17-data-point calibration suite already in
`test_anti_hardcode_check.py`. The 3 real samples all carry **non-empty**
`before_value` (contra the prompt's "all use before_value=''" — recorded here as
a corrected observation), and all are `clean_prose_label: clean`.

| Sample | before/after verdict |
|---|---|
| exp-2 (clean) | PASS → PASS |
| exp-3 (clean) | PASS → PASS |
| exp-4 (clean) | PASS → PASS |

FLAG_FOR_CODEX rate = 0/3 = 0% (<25% bar) — unchanged. FP-on-clean = 0 —
unchanged. Because all calibration samples already PASS (no rule match),
suppression — which only ever *removes* a match — cannot change their outcome;
verified empirically before and after the patch. The 3 calibration assertions
plus the 17-data-point suite remain green.

## §6 — Decisions & accepted trade-offs

- **`matched_in_baseline` audit field — SKIPPED.** Adding the optional
  `matched_in_baseline: bool` to `AntiHardcodeResult` as *meaningful* metadata
  would require tracking suppression events across the rule helpers (which today
  return only the final display string, not "did I suppress a baseline match").
  On any returned FAIL/FLAG verdict the match is by definition NOT in baseline,
  so a bare field on the verdict would be uninformative `False` without the wider
  suppression-event plumbing. Per scope item #4 ("If it needs wider changes, skip
  it"), the field is skipped. `AntiHardcodeResult` is unchanged.

- **Double-encoding trade-off — ACCEPTED + documented** (in the
  `test_double_encoding_bypass_documented` docstring): a candidate that copies a
  baseline pattern verbatim to a NEW position is suppressed (the copied span is
  still `in before_norm`) and will NOT fire. The field still shows the doubled
  pattern to a human / Codex auditor; the propose-stage detector simply does not
  block on it. This is the deliberate limit of substring-based baseline-whitelisting.

- **Iterate-vs-first-match for predicate rules** (Q2-must, Q2-tier0,
  Q5-do-not-consider): the refactor checks *all* regex matches and returns the
  first predicate-qualifying, baseline-new one — where the old code checked only
  the first regex match's predicate and returned None if it failed. This is the
  intended "iterate" change (a latent precision improvement) and is outcome-neutral
  for every existing fixture + the calibration suite (all green). When
  `before_value` is empty the first predicate-qualifying match is still returned.

## §7 — scoring_code_baseline_sha proof → NO re-bless

`fitness.scoring_code_baseline_sha` (config.yaml:340) =
`0d86b08fc0309196d9fc49f4924fe446b36d6c04301c4bcb406ca12337dd149e`.
`_compute_scoring_code_sha()` recomputed on the working tree =
`0d86b08fc0309196d9fc49f4924fe446b36d6c04301c4bcb406ca12337dd149e` — **identical**.
The SHA covers `autoloop/autoloop/scoring/{tier_evaluator,eval_runner,baseline_loader,gaming,aggregate}.py`
only; `git diff --numstat HEAD -- autoloop/autoloop/scoring/` is empty (scoring
untouched). `anti_hardcode_check.py` lives under `sandbox/`, outside the SHA set.
→ **NO re-bless.** The `m-auto-7-prepilot-baseline-20260608` fitness baseline
stays valid as the S-Y2 pilot baseline.

## §8 — exp-70/76 replay

`exp-70/76 anti-hardcode replay: PASS through sandbox check only; not evidence of
eval/gate success.`

Replay of the real exp-70 + exp-76 `(before_value, after_value)` pairs (config
`synonym_map_enabled: true`) through the patched `anti_hardcode_check()` returns
**PASS** for both, where the Run-1 tranche recorded
`FAIL / Q1.if_then_decision_tree` with a byte-identical 100%-baseline
`matched_substring`. Per §5.7 this mocked/unit replay is NOT behaviour-change
evidence — the real-LLM evidence is the downstream S-Y2 Part C `-n 3`
verification re-tranche, not this sub-sprint.

## §9 — Commit / scope discipline

Files touched (staged explicitly by file, NO `git add -A`):
`autoloop/autoloop/sandbox/anti_hardcode_check.py`,
`autoloop/tests/test_anti_hardcode_check.py`. The dev commit is `autoloop/**`
only. Pre-existing working-tree state (`docs/sprint_objective.md`,
`compact/*`, `docs/solutions/2026-06-09-…`) and this handoff are deliver-agent /
human close artefacts, bundled at close — NOT part of the dev commit. No autoloop
run was executed (tree was kept clean of any loop-generated commits).

## §10 — Codex review plan (§4.3 trigger #3, REQUIRED)

Per-sub-sprint Codex REQUIRED before the S-Y2 Part C re-tranche. Focus:

- **Suppression correctness:** `before_norm` uses the SAME `synonym_map_enabled`
  flag as the `after_value` normalization (so matched spans live in one surface);
  suppression tested against `m.group(0)`, not the rendered window.
- **Regex / normalize immutability:** confirm Q1–Q5 rule-body regexes and
  `_normalize` (incl. `_SYNONYM_MAP`, `_RE_WHEN_WORD_BOUNDARY`) are byte-identical
  to HEAD — the diff must show only first-match→iterate refactors + the entry-point
  `before_norm` line.
- **Predicate preservation:** Q2/Q5 `_SOFT_SEMANTIC_DIMENSIONS` membership and the
  Q2-tier0 / Q4-id_assign windows are unchanged.
- **No-op invariant:** verify the empty-`before_value` path is byte-identical
  (the existing 34 fixtures + 3 calibration samples are the proof; the exp-70/76
  empty-baseline replay still FAILs).
- **D2 self-discipline:** `anti_hardcode_check.py` contains no eval CaseSpec
  literal (the embedded exp-70 strings live in the TEST file only).
- **Scope:** only `anti_hardcode_check.py` + `test_anti_hardcode_check.py` touched;
  `scoring_code_baseline_sha` reproduces (no re-bless).

## §11 — Open questions

- **OQ-S90.1 (greedy-trailing span widening):** for rules with a greedy trailing
  capture (`Q1` if/then & arrow `[\s\S]{1,120}`; `Q2.must_always` / `Q5`
  `[\s\S]{0,80}`), the matched span can extend up to 80–120 chars PAST the core
  pattern. A baseline pattern immediately followed in `after_value` by NEW text
  (e.g. an appended clause begun <120 chars after the baseline pattern's tail)
  yields a span that is NOT a clean baseline substring → the candidate would NOT
  be suppressed and would FAIL. exp-70/76 are unaffected (their appends start
  well past the baseline arrow-tree's trailing window). Open: whether a
  diff-anchored detector (scan only the changed span) is the durable replacement
  for substring-whitelisting; the RCA's Option C.
- **OQ-S90.2 (double-encoding blind spot):** see §6 — substring-whitelisting
  cannot distinguish pure-additive context-carry from deliberate verbatim
  double-encoding of a forbidden pattern to a new position. Currently accepted;
  the doubled pattern is still visible to a human/Codex auditor. Open: whether the
  downstream Codex pass or a count-based heuristic should flag N>1 occurrences of
  the same matched span.
- **OQ-S90.3 (Q6 coverage):** the contract names "Q1/Q2/Q4/Q5/Q6" but the live
  `_RULES` registry has no Q6 rule (the detector covers Q1/Q2/Q4/Q5 only).
  Suppression is applied uniformly to all 11 registered rules, so any future Q6
  rule inherits it automatically. Open: confirm no separate Q6 surface was
  intended for this patch (none exists to modify today).

<!-- ===================================================================== -->
<!-- S-Y1.5c (S-Auto-36) appended below. The S-Y1.5b section above is the  -->
<!-- S-Auto-35 dev close and is intentionally left untouched.              -->
<!-- ===================================================================== -->

# S-Y1.5c — severity calibration (S-Auto-36)

## Class

| Field | Value |
|---|---|
| **Layer (§3.2)** | `infra` — autoloop propose-stage sandbox detector *severity* calibration (eval-framework-adjacent) |
| **§7 stanza** | **REQUIRED** (changes what the propose-stage filter treats as a §1.7 violation). Stanza in §7c below. |
| **Per-sub-sprint Codex (§4.3)** | **REQUIRED** — one combined nine-question walk covers the cumulative S-Y1.5b + S-Y1.5c diff; Codex writes TWO §4.2 stanzas to `docs/codex-findings.md`. Runs AFTER this dev close, BEFORE the downstream S-Y2 Part C re-tranche. |

## §1c — Goal

S-Y1.5b (LANDED, `9c62a86`) stopped baseline text from re-triggering a rule.
S-Y1.5c fixes the *severity*: **FAIL = "definitely a hardcode — discard
pre-eval"; FLAG_FOR_CODEX = "borderline — let the per-sub-sprint Codex Kernel
review judge."** The S-Auto-4 detector FAILed on eleven rules, several of which
fire on shapes that are legitimate principle-level narrative / few-shot teaching
(e.g. exp-70/exp-76 on `resolve_faq_grounded_answer.yaml`). The semantic gate is
restored to where the Constitution puts it (§4.1/§4.3 Codex review); the cheap
syntactic FAILs whose surface form == constitutional intent are kept.

## §2c — Diff summary

Commit range: **`9c62a86` (S-Y1.5b)** → **`1b18474` (docs-only plan approve)** →
**`75b302c` (this S-Y1.5c dev commit)**. The S-Y1.5c-only dev commit is
`75b302c`; the combined b+c autoloop diff Codex reviews is `9c62a86^..75b302c`.

| File | what changed |
|---|---|
| `autoloop/autoloop/sandbox/anti_hardcode_check.py` | 6 `_RULES` severities `_FAIL`→`_FLAG`; new `_effective_severity()` helper; rule loop branches on `eff` not raw registry `severity` |
| `autoloop/config.yaml` | `anti_hardcode.severity_overrides: {}` added; stale "11/4/2" comment + Fix-C narrative updated to ~2/4/11 (`scoring_code_baseline_sha` line UNCHANGED) |
| `autoloop/autoloop/meta_agent/prompts/propose.txt` | new `# Acceptable patterns` section (positive guidance only; forbidden list byte-unchanged) |
| `autoloop/tests/test_anti_hardcode_check.py` | 13 demoted per-pattern FAIL tests → FLAG with Kernel rationale; `test_fail_carries_matched_substring` re-pointed to a kept-FAIL fixture; 7 new severity tests; 3 narrative comments corrected |
| `autoloop/tests/test_real_meta_agent_calibration.py` | docstring "11/4/2" → "~2/4/11" (narrative only; 3 assertions untouched + green) |
| `autoloop/tests/test_propose_prompt.py` (NEW) | substring guard so the propose.txt clarification cannot silently disappear |

## §3c — Per-rule severity table

Final FAIL set = 4; final FLAG set = 7; total 11 (unchanged count).

| rule_id (registry) | helper | before | after | basis |
|---|---|---|---|---|
| `Q1.contains_or_matches_literal` | `_q1_contains_matches` | FAIL | **FAIL (kept)** | code-style `.contains(`/`.matches(` — surface == intent |
| `Q1.enumerated_or_keywords` | `_q1_or_keywords` | FAIL | **FLAG** | Q3 — may be few-shot illustrative teaching |
| `Q1.if_then_decision_tree` | `_q1_if_then` | FAIL | **FLAG** | Q6 — may be principle-level narrative |
| `Q1.arrow_tree` (`if X → Y`) | inside `_q1_if_then` | (n/a — no separate id) | **FLAG via `Q1.if_then_decision_tree`** | arrow regex `_RE_Q1_ARROW_TREE` runs *inside* `_q1_if_then`; flipping the one entry demotes both forms — NO new rule_id, NO helper split (proven by `test_default_severity_demotes_q1_arrow_tree_to_flag`) |
| `Q2.must_always_against_soft_dimension` | `_q2_must_always` | FAIL | **FLAG** | Q6 — "must always" vs soft dim may be narrative emphasis |
| `Q2.tier0_invariant_invention_attempt` | `_q2_tier0_invention` | FAIL | **FAIL (kept)** | Tier-0 minting authorization is not the proposer's |
| `Q4.case_id_literal` | `_q4_case_id_token` | FAIL | **FAIL (kept)** | raw `cs<id>` token — §1.7 raw-eval-phrase |
| `Q4.id_assignment_literal` | `_q4_id_assign` | FAIL | **FAIL (kept)** | `session_id:`/`case_id:` — identifier binding |
| `Q5.bot_must_always` | `_q5_bot_must_always` | FAIL | **FLAG** | Q6 — borderline LLM-ownership language |
| `Q5.do_not_consider_soft_dimension` | `_q5_do_not_consider` | FAIL | **FLAG** | Q3/Q6 — semantic judgment about LLM reasoning scope |
| `Q5.force_assistant_to` | `_q5_force_assistant` | FAIL | **FLAG** | Q6 — borderline ownership-shrinking |
| `Q5.standalone_must_borderline` | `_q5_standalone_must` | FLAG | **FLAG (unchanged)** | already borderline |

## §4c — Demoted-fixture FAIL→FLAG test-update table + per-test Kernel rationale

The flips were grounded in the actual suite run (13 failures after Items 1–3,
exactly the demoted set; ZERO kept-FAIL tests changed verdict → no STOP).

| test | now asserts | rule_id | Kernel rationale comment |
|---|---|---|---|
| `test_q1_if_then_decision_tree_rejected` | FLAG | `Q1.if_then_decision_tree` | Q6 principle-level narrative |
| `test_q1_or_keyword_enumeration_rejected` | FLAG | `Q1.enumerated_or_keywords` | Q3 few-shot illustrative |
| `test_q2_runtime_must_always_on_soft_dimension_rejected` | FLAG | `Q2.must_always_against_soft_dimension` | Q6 narrative emphasis; genuine Tier-0 invention still caught by `Q2.tier0…` (FAIL) |
| `test_q5_force_assistant_to_rejected` | FLAG | `Q5.force_assistant_to` | Q6 borderline ownership |
| `test_q5_bot_must_always_rejected` | FLAG | `Q5.bot_must_always` | Q6 narrative emphasis |
| `test_q5_do_not_consider_soft_dimension_rejected` | FLAG | `Q5.do_not_consider_soft_dimension` | Q3/Q6 reasoning-scope judgment |
| `test_unicode_nfkc_full_width_if_detected` | FLAG | `Q1.if_then_decision_tree` | normalization still proven; verdict demoted (Q6) |
| `test_multi_line_decomposition_if_then_detected` | FLAG | `Q1.if_then_decision_tree` | Q6 |
| `test_synonym_map_arrow_when_enabled` | FLAG | `Q1.if_then_decision_tree` | arrow shares id; Q6 |
| `test_fix_c_step1_codex_axis_b_bypass_now_caught` (renamed `_fails`→`_caught`) | FLAG | `Q1.if_then_decision_tree` | bypass still caught; "caught" now = FLAG→Codex |
| `test_fix_c_step1_multi_line_when_arrow_decomposition_caught` (renamed `_fails`→`_caught`) | FLAG | `Q1.if_then_decision_tree` | Q6 |
| `test_q1_arrow_tree_new_match_still_caught` | FLAG | `Q1.if_then_decision_tree` | suppression-no-op proof preserved; triggers (FLAG) not suppressed-PASS |
| `test_fail_carries_matched_substring` | **FAIL (kept)** via re-pointed fixture | `Q1.contains_or_matches_literal` | re-pointed to a kept-FAIL pattern so the FAIL-path result-shape contract still has coverage |

Kept-FAIL tests (unchanged, all green): `test_q1_contains_literal_rejected`,
`test_q1_matches_literal_rejected`, `test_q2_tier0_invention_attempt_rejected`,
`test_q4_case_id_token_rejected`, `test_q4_session_id_assignment_rejected`,
`test_q4_case_id_in_assignment_rejected` (FAIL via `Q4.id_assignment_literal`
even though its if/then now only FLAGs — comment corrected).

New tests (8): `test_severity_overrides_promote_demoted_rule_back_to_FAIL`,
`test_severity_overrides_demote_fail_to_flag`,
`test_default_severity_demotes_q1_if_then_to_flag`,
`test_default_severity_demotes_q1_arrow_tree_to_flag`,
`test_default_severity_keeps_q4_case_id_as_fail`,
`test_default_severity_keeps_q1_contains_matches_as_fail`,
`test_default_severity_keeps_q2_tier0_invention_as_fail`,
`test_propose_txt_contains_acceptable_patterns_section` (in `test_propose_prompt.py`).

## §5c — Doc-hygiene narrative fix

`config.yaml` Fix-C comment (line ~394) and `test_real_meta_agent_calibration.py`
module docstring updated from "11 FAIL + 4 PASS + 2 FLAG" to the post-demote
**~2 FAIL / 4 PASS / 11 FLAG** distribution. Narrative only — the smoke /
composite distribution is not a close gate (§5.5); the three calibration
assertions (count ≥1, FLAG-rate <0.25, 0-FP-on-clean) are mechanically untouched
and green (the 3 clean samples PASS, so the demote — which only ever turns a FAIL
into a FLAG — cannot move them).

## §6c — §3.4 pre-close dry-run sanity

`cd autoloop && uv run python -m autoloop run --dry-run -n 2` on the clean tree
(after committing `75b302c`). Result: `keep=0 discard=1 error=1`.

**exp-77 — clean snapshot (primary §3.4 evidence).** Target
`resolve_faq_grounded_answer.yaml $.critical_steps[0].desc` — the SAME file
exp-70/76 were false-positive-blocked on. Verdicts: content_validator PASS,
sandbox ACCEPT, **anti_hardcode PASS** (no rule matched), decision discard /
`dry_run_no_apply` (expected for dry-run). Per-iteration checklist:

- raw eval CaseSpec ids in `after_value`? **NO** — uses UC-A…UC-FP use-case
  names + `listing_context`/`customer_context`/moderation schema names; no
  `cs<id>` token (Q4 would still bite if present). ✓
- gratuitous "the bot must always X" / "the runtime must never Y" against §1.3
  soft dimensions in `rationale`? **NO** — descriptive ("additive clause…",
  "names the grounding hierarchy", "conditions on observable projection
  state"). ✓
- reads as principle-level narrative / few-shot teaching, NOT an if-then rule
  dump with quoted user keywords? **YES, principle-level** — "grounding
  hierarchy … case-specific tool data first, then the retrieved knowledge
  surface"; "when the projection already carries the listing's state, the
  answer should reflect that state; when the listing reference is missing or
  ambiguous, a focused clarifying question …" (Acceptable-patterns shape #2:
  principle + observable-state conditioning). ✓

**exp-78 — infra error, NOT a proposer-over-correction signal.**
`decision=error`, `anti_hardcode_verdict=None`,
`error=APITimeoutError: Request timed out or interrupted` — the failure was in
the **proposer LLM call**; the iteration never reached the detector, so it
carries no information about proposer output or detector behaviour. Independent
of S-Y1.5c (env/network timeout). No re-run performed: the §3.4 STOP condition
is "if ANY iteration shows over-correction toward real rule dumps" — exp-77
shows none, exp-78 shows nothing, so no STOP; one clean snapshot satisfies the
"1–2 iterations" bar.

**Verdict: PROCEED** — no proposer over-correction observed; the one clean
iteration is exactly the on-target principle-level narrative the calibration was
built to admit, and the detector correctly PASSed it.

## §7c — Layer-classification + anti-hardcode stanza

**Target failure layer:** `infra` (autoloop sandbox detector severity
calibration; eval-framework-adjacent — §5.8 framework-defect priority applies in
spirit because the propose-stage filter was blocking on-target candidates from
reaching eval for false-positive reasons during an in-flight semantic pilot).

**Tier-0 invariant:** This sub-sprint adds no Tier-0 invariant. The demote does
NOT change what §1.7 forbids; it changes how the propose-stage filter expresses
uncertainty about borderline semantic patterns (FLAG_FOR_CODEX = "Codex must
judge" vs FAIL = "definitely a hardcode"). The semantic gate is restored to
per-sub-sprint Codex review (§4.3), where it lived constitutionally before the
detector's S-Auto-4 over-tightening. Structural defenses retained:
`Q4.case_id_literal` + `Q4.id_assignment_literal` stay FAIL (§1.7
raw-eval-phrase / identifier-binding); `Q2.tier0_invariant_invention_attempt`
stays FAIL (§3.2 Tier-0 minting authorization); `Q1.contains_or_matches_literal`
stays FAIL (code-style enumeration, surface == intent); shadow firewall,
mutable-surface YAML sandbox, and 5-layer lexicographic fitness gate all
unaffected; per-sub-sprint Codex review (§4.3) IS the semantic-judgment binding
gate. Reversibility: `anti_hardcode.severity_overrides` re-promotes any demoted
rule to FAIL per-experiment without a code change.

**Semantic hardcode:** No semantic hardcode introduced. This patch removes
false-positive hardcode rejections that were blocking legitimate principle-level
narrative + few-shot illustrative teaching (e.g. exp-70 / exp-76 on
`resolve_faq.$.procedure`). The propose.txt clarification adds positive guidance
without weakening the §1.7 forbidden list.

**Generalization coverage:** target / neighbor / negative / shadow case counts:
n/a — pure-infra sub-sprint touching only the detector severity registry +
`_effective_severity` helper + propose.txt clarification + `severity_overrides`
config schema + the two test files. No agent semantic surface touched.
Validation = unit tests + per-pattern demoted-fixture re-baseline + §3.4
pre-close 1–2 dry-run sanity check.

## §8c — `severity_overrides` reversibility example

If Codex review at S-Y2 close shows a demoted rule slipped a real hardcode,
re-promote in `autoloop/config.yaml` with NO code change:

```yaml
anti_hardcode:
  severity_overrides:
    Q1.if_then_decision_tree: "FAIL"   # re-promote: now FAILs new if/then again
```

Proven by `test_severity_overrides_promote_demoted_rule_back_to_FAIL` (override →
FAIL on a NEW Q1.if_then) and `test_severity_overrides_demote_fail_to_flag`
(machinery works both directions; DEFAULT Q4 stays FAIL, hard-fenced).
`_effective_severity` ignores any value that is not exactly `"FAIL"` /
`"FLAG_FOR_CODEX"`, so a config typo cannot disable a rule.

## §9c — No-re-bless / no-scoring / no-§1.7 / no-Kernel proof

- `scoring_code_baseline_sha` (config.yaml:340) =
  `0d86b08fc0309196d9fc49f4924fe446b36d6c04301c4bcb406ca12337dd149e`;
  `_compute_scoring_code_sha()` on the working tree reproduces it **identically**
  → `gaming.scoring_code_drift.sha_changed` will not fire → **NO re-bless**.
  `git diff --numstat HEAD -- autoloop/autoloop/scoring/` is empty.
- `docs/current/iteration_governance.md` (§1.7) — **byte-unchanged**.
- `docs/current/anti-hardcode-review-kernel.md` (Kernel) — **byte-unchanged**.
- No rule-body regex / `_normalize` / `_first_new_match` / predicate change —
  severity is the ONLY behavioral change.
- No CaseSpec / fixtures / baseline change (`eval_interactive/**`), no Skill YAML
  change (`server/**`), no `mutable_surface` change, no `baseline_dir` /
  `current_eval_baseline.md` flip, no Java change (Java baseline `1383/1/0/2`
  stays).
- Shadow firewall unaffected — the detector reads only `before_value` +
  `after_value` strings.

## §10c — Suite evidence

Full `autoloop` pytest: **344 passed**, 1 warning (the pre-existing
`test_layer4_shadow_missing_keeps_with_warning` baseline-missing UserWarning),
166s. Baseline was 336 post-S-Y1.5b; Δ = +8 = the 7 new severity tests + the 1
new `test_propose_prompt.py` test. `test_anti_hardcode_check.py` +
`test_propose_prompt.py` + `test_real_meta_agent_calibration.py` = 47 passed.

## §11c — FLAG surfacing preserved (end-to-end, Codex focus #5)

The demote is only safe if FLAG_FOR_CODEX is surfaced (otherwise it is a §1.7
bypass). Unchanged paths: `loop.py:271` sets
`result.anti_hardcode_flag_for_codex = True` on a FLAG verdict; persisted to the
experiments row (`loop.py:610-611`) and to
`runs/<id>/anti_hardcode_verdict.json` (`loop.py:808-813`). `anti_hardcode_check`
returns the same `AntiHardcodeResult` shape; only the chosen `verdict` field
value changes. The dry-run exp-77 `anti_hardcode_verdict.json` confirms the
verdict artefact is still written.

## §12c — Codex review plan (§4.3, REQUIRED — combo)

One §4.3 nine-question Kernel walk covers the cumulative S-Y1.5b + S-Y1.5c diff
(`9c62a86^..75b302c`). Codex writes TWO §4.2 stanzas to `docs/codex-findings.md`
(one for b, one for c). S-Y1.5c focus: (1) FAIL set reduced to ONLY
surface==intent rules; demoted rules genuinely semantic-judgment per Kernel
Q3/Q6; (2) each FAIL→FLAG test's Kernel rationale justified; (3) Q4 default
unchanged (`cs011`/`cs_uc_a_no_ad_id`/`session_id=` still FAIL); (4) propose.txt
clarification opens no §1.7 bypass; (5) FLAG surfacing preserved end-to-end
(§11c); (6) shadow firewall unaffected; (7) `scoring_code_baseline_sha` stable;
(8) reversibility actually reversible.

## §13c — Observability follow-up (R-item, not a §5.X gate)

**R-S90.4:** Track Codex `reject` rate on FLAG_FOR_CODEX patterns at S-Y2 close.
If > 30% of FLAGged candidates are rejected by Codex as real hardcode, reconsider
the demote scope at M-Auto-8 (a high reject rate would mean the demote moved too
much judgment downstream). Observability only — does not gate any close.

## §14c — Commit / scope discipline

Dev commit `75b302c` is `autoloop/**` only (detector + config + propose.txt +
tests), staged explicitly by file (NO `git add -A`). This handoff section and any
`docs/` close artefacts are deliver-agent / human close artefacts, bundled at
close — NOT part of the dev commit. The dev commit was made BEFORE the §3.4
dry-run so the tree was clean (the autoloop sweeps the staged index; dry-run
additionally short-circuits before any apply/commit). No non-dry-run autoloop was
executed.

<!-- ===================================================================== -->
<!-- S-Y1.5d (S-Auto-37) appended below. The S-Y1.5b/c sections above are   -->
<!-- prior dev closes and are intentionally left untouched.                 -->
<!-- ===================================================================== -->

# S-Y1.5d — Q4 regex precision fix (resolves R-S90.5 + R-S90.6) (S-Auto-37)

> **ATTEMPT-2 (2026-06-12).** Attempt-1 (commit `9718dfa6`, regex
> `\bcs[0-9_][0-9a-z_]+\b`) was HELD by Codex blocker **R-S90.6**: that
> discriminator required a digit/underscore *immediately after* `cs`, so real
> `csmp_*` CaseSpec ids (`cs`+letter, e.g.
> `csmp_g01_uc_a_genuine_faq_miss_obscure`) slipped through → PASS →
> false-negative that weakened the §1.7 Q4 hard-discard. This section is
> rewritten for attempt-2: the **corrected regex** (digit/underscore ANYWHERE
> in the cs-token), a new R-S90.6 regression-guard test, and a HARD
> corpus-completeness scan (452 cs-ids, 0 misses) proving the regex catches
> every real CaseSpec id. NEW commit on top of attempt-1.

## Class

| Field | Value |
|---|---|
| **Layer (§3.2)** | `infra` — autoloop sandbox detector regex precision fix (eval-framework-adjacent) |
| **§7 stanza** | **REQUIRED** (touches the §1.7 enforcement detector). Stanza in §7d below. |
| **Per-sub-sprint Codex (§4.3)** | **REQUIRED** — targeted re-review of ONLY the Q4 regex diff; resolves R-S90.5 + R-S90.6 and flips the S-Y1.5c stanza `fix_required → pass`. |

## §1d — The blockers (R-S90.5 + R-S90.6)

**R-S90.5** (`docs/codex-findings.md`): the original Q4 case-id regex
`\bcs[0-9a-z_]{2,}\b` matched ANY word beginning `cs` + 2 alnum/underscore
chars, so independent probes returned `FAIL Q4.case_id_literal` on `CSAT`
("Use CSAT feedback…") and `csagent` ("The csagent should provide grounded
answers.") — neither is a CaseSpec id. This broke S-Y1.5c's load-bearing claim
that the four retained default-FAIL rules are all "surface form ==
constitutional intent", and re-opened a false-positive-discard vector on the
`resolve_faq` surface the S-Y2 pilot runs on. Human decision (2026-06-11):
**tighten the regex, keep FAIL.**

**R-S90.6** (attempt-1 hold): the attempt-1 fix `\bcs[0-9_][0-9a-z_]+\b`
required the digit/underscore *immediately after* `cs`. That is too narrow —
real `csmp_*` CaseSpec ids (`cs`+letter, e.g.
`csmp_g01_uc_a_genuine_faq_miss_obscure`,
`csmp_s01_uc_a_search_hits_with_ad_id_mismatch`) carry the discriminator
LATER in the token, so they slipped through → PASS → a false-negative that
re-opened the §1.7 Q4 hard-discard on a whole CaseSpec-id family. Fix:
require the digit/underscore **anywhere** in the cs-token.

## §2d — The fix (one regex line)

`autoloop/autoloop/sandbox/anti_hardcode_check.py` — `_RE_Q4_CASE_ID_TOKEN`:

| | regex |
|---|---|
| original (S-Y1.5c) | `\bcs[0-9a-z_]{2,}\b` |
| attempt-1 (R-S90.6 hold) | `\bcs[0-9_][0-9a-z_]+\b` |
| **attempt-2 (this fix)** | `\bcs[a-z0-9_]*[0-9_][a-z0-9_]*\b` |

Reads: `cs` + any run of `[a-z0-9_]` that **contains at least one digit or
underscore anywhere**. Every real CaseSpec id has one — numeric (`cs011`),
underscore-slug (`cs_uc_a_no_ad_id`), shadow (`cs38s01`), AND `cs`+letter
`csmp_*` ids (the discriminator is the `_` / `0` later in the token). Ordinary
`cs`+letter words (`CSAT`, `csagent`, `css`, `csv`, `cstring`) are pure
letters → no match. `\bcs` still anchors to an identifier start (mid-word `cs`
in `discuss`/`customers` excluded). The rule comment was updated to describe
the "digit/underscore anywhere" discriminator; it contains **no real `cs`-id
literal** (the D2 `test_detector_source_does_not_hardcode_eval_case_ids` guard
stays green — attempt-1's first comment draft was rejected by that test for
embedding `cs011`; the final comment uses only generic `cs<NNN>`/`cs_<slug>`
shape placeholders).

**`Q4.case_id_literal` severity is unchanged — still `_FAIL` in `_RULES`.
The registry, `_normalize`, `_first_new_match`, all other rule regexes,
`severity_overrides`, `propose.txt`, and `config.yaml` are untouched.**

## §3d — Test matrix (the binding contract) — all green

| Input (in clean prose) | Expected | Result |
|---|---|---|
| `cs011`, `cs042`, `cs101` | FAIL `Q4.case_id_literal` | ✓ FAIL |
| `cs_uc_a_no_ad_id` | FAIL `Q4.case_id_literal` | ✓ FAIL |
| `cs38s01`, `cs59s` | FAIL `Q4.case_id_literal` | ✓ FAIL |
| `csmp_g01_…`, `csmp_s01_…`, `csmp_n01_…` (**R-S90.6 class**) | FAIL `Q4.case_id_literal` | ✓ FAIL |
| `CSAT` / "Use CSAT feedback…" | PASS | ✓ PASS |
| `csagent` / "The csagent should provide grounded answers." | PASS | ✓ PASS |
| `css`, `cstring`, `csv`, `cscience` (clean prose) | PASS | ✓ PASS |
| "discuss the customer's case" (mid-word `cs`) | PASS | ✓ PASS (existing test) |

Tests in `test_anti_hardcode_check.py` (attempt-1 added the first two; attempt-2
adds the R-S90.6 guard + a pure-letter PASS companion):

- `test_q4_ordinary_cs_words_pass` (attempt-1) — `CSAT`, `csagent`, `css`,
  `cstring` → assert verdict `PASS`.
- `test_q4_underscore_and_shadow_case_ids_still_fail` (attempt-1) —
  `cs_uc_a_no_ad_id`, `cs38s01`, `cs59s` → assert `FAIL` + rule_id
  `Q4.case_id_literal`.
- `test_q4_csmp_letter_prefixed_case_ids_still_fail` (**NEW, R-S90.6 guard**) —
  `csmp_g01_uc_a_genuine_faq_miss_obscure`,
  `csmp_s01_uc_a_search_hits_with_ad_id_mismatch`,
  `csmp_n01_uc_a_visibility_search_hits` → assert `FAIL` + rule_id
  `Q4.case_id_literal`. Attempt-1's regex let these PASS.
- `test_q4_pure_letter_cs_words_still_pass` (**NEW, over-correction guard**) —
  `CSAT`/`csagent`/`css`/`cstring`/`csv`/`cscience` → assert `PASS`.

Existing FAIL tests for genuine ids stay green and use only real
`cs<digit|_>` literals (`test_q4_case_id_token_rejected` = `cs042`/`cs101`;
`test_default_severity_keeps_q4_case_id_as_fail` = `cs_uc_a_no_ad_id`;
`test_q4_clean_text_with_cs_in_word_passes` mid-word `cs`). No existing FAIL test
asserted on a `cs`+letter fake id, so none flipped.

## §3d.1 — Corpus-completeness check (HARD — R-S90.6 root-cause guard) — 452 / 0

Per the attempt-1 failure mode, the chosen regex was proven against **every**
`cs`-prefixed CaseSpec id in the corpus via a direct Python `os.walk` scan run
**from the repo root** (recursive `grep -r`/`find` gave misleading empty
results in earlier sessions because the shell `cwd` had drifted into
`autoloop/`). The scan walks `eval_interactive/case_specs` +
`eval_interactive/case_specs_shadow`, collecting every `cs`-prefixed filename
stem AND every `case_id:`/`id:` field literal inside the YAMLs, then asserts the
regex matches all of them:

```
yaml files scanned: 509
cs-id count: 452
MISSES: []
```

**452 distinct cs-ids, 0 misses** — identical to the deliver-agent reference
result. Every real CaseSpec id is caught by the corrected regex.

## §4d — Suite evidence

Full `autoloop` pytest (`cd autoloop && uv run python -m pytest tests/ -q`):
**348 passed**, 1 warning, ~138s. Attempt-1 baseline was 346 (§4d original);
Δ = +2 = the two new attempt-2 tests
(`test_q4_csmp_letter_prefixed_case_ids_still_fail`,
`test_q4_pure_letter_cs_words_still_pass`). No existing test regressed. (The
attempt-2 comment draft initially tripped the D2
`test_detector_source_does_not_hardcode_eval_case_ids` guard by embedding
`cs011` in the rule comment; the comment was rewritten with generic shape
placeholders and the suite is fully green.) The 1 warning is the pre-existing
`test_layer4_shadow_missing_keeps_with_warning` baseline-missing UserWarning
(unchanged).

## §5d — scoring_code_baseline_sha proof → NO re-bless

`fitness.scoring_code_baseline_sha` (config.yaml:340) =
`0d86b08fc0309196d9fc49f4924fe446b36d6c04301c4bcb406ca12337dd149e`.
`_compute_scoring_code_sha()` on the working tree reproduces it **identically**.
`git diff --numstat HEAD -- autoloop/autoloop/scoring/` is empty (scoring
untouched); `anti_hardcode_check.py` lives under `sandbox/`, outside the SHA set.
→ **NO re-bless.** No §3.4 dry-run is required: the change only makes the
detector match a slightly LARGER set of real CaseSpec-id shapes (the R-S90.6
`csmp_*` family) while still removing the R-S90.5 false positives; it cannot
alter proposer output, so the §3d test matrix + §3d.1 corpus-completeness scan
are the evidence. Per §5.7 this mocked/unit evidence is not behaviour-change
proof — the real-LLM evidence remains the downstream S-Y2 Part C re-tranche.

## §6d — S-Y1.5c thesis now holds

With the discriminator, `Q4.case_id_literal` matches only a CaseSpec-id surface
(`cs` + digit/underscore), so all four retained default-FAIL rules are again
"surface form == constitutional intent": `Q1.contains_or_matches_literal`
(code-style `.contains(`/`.matches(`), `Q2.tier0_invariant_invention_attempt`
(Tier-0 minting), `Q4.case_id_literal` (CaseSpec-id token), `Q4.id_assignment_literal`
(`session_id:`/`case_id:` binding). The §1.7 raw-eval-phrase hard-discard for real
ids is preserved.

## §7d — Layer-classification + anti-hardcode stanza

**Target failure layer:** `infra` (autoloop sandbox detector regex precision fix;
eval-framework-adjacent — removes a false-positive-discard class that was blocking
legitimate `cs…` prose pre-eval).

**Tier-0 invariant:** This sub-sprint adds no Tier-0 invariant. It narrows an
over-broad §1.7 enforcement regex; the Q4 raw-eval-phrase hard-discard is
preserved (real `cs<digit|_>` ids still FAIL by default).

**Semantic hardcode:** No semantic hardcode introduced. The change REMOVES false
positives (ordinary `CSAT` / `csagent` language) without weakening the
forbidden-list intent; it does not encode any specific eval phrase or word
whitelist — it uses a structural discriminator (digit/underscore after `cs`) that
distinguishes CaseSpec-id shape from ordinary words.

**Generalization coverage:** target / neighbor / negative / shadow case counts:
n/a — pure-infra detector regex fix. Validation = the §3d Test matrix unit tests
(real ids FAIL; ordinary cs-words PASS) + full autoloop pytest green + scoring SHA
stable.

## §8d — Codex re-review plan (§4.3, REQUIRED — targeted)

Targeted re-review of ONLY the Q4 regex diff (resolves R-S90.5 + R-S90.6).
Codex confirms: (1) `CSAT` / `csagent` now PASS; (2) `cs011` /
`cs_uc_a_no_ad_id` / `cs38s01` **and the `csmp_*` `cs`+letter family** still
FAIL `Q4.case_id_literal`; (3) the corpus-completeness scan reports 452 cs-ids
/ 0 misses; (4) no other rule-body, severity, scoring, or propose.txt change;
(5) full pytest green; (6) `scoring_code_baseline_sha` stable. On pass, the
`## Sub-sprint Review Decision — S-Y1.5c` stanza flips `fix_required → pass`,
`blocking_count → 0`, and R-S90.5 + R-S90.6 are marked resolved-by-S-Y1.5d.

## §9d — Commit / scope discipline

Files touched (staged explicitly by file, NO `git add -A`):
`autoloop/autoloop/sandbox/anti_hardcode_check.py`,
`autoloop/tests/test_anti_hardcode_check.py`. The dev commit is `autoloop/**`
only (one regex line + two tests). This handoff section is a deliver/human close
artefact bundled at close — NOT part of the dev commit. No autoloop run was
executed.
