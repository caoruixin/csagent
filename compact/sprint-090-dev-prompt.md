# Dev prompt — Sprint 090 / S-Auto-35 (M-Auto-7 S-Y1.5b)

你是 **dev agent for Sprint 090 / S-Auto-35 (M-Auto-7 S-Y1.5b)**.

**One-line goal:** add baseline-whitelist suppression at the autoloop
`anti_hardcode_check()` entry point (generalized across Q1/Q2/Q4/Q5/Q6) so the
propose-stage detector stops false-firing on **pure-additive** candidates whose
untouched baseline prose already matches a rule. Pure autoloop-infra; **no re-bless**.

## Read order (minimal)

1. `AGENTS.md` (auto-loaded — governance chain).
2. **This prompt** — the self-contained contract is embedded below; you do not need
   any other doc.
3. Code anchors named inline below (read those specific files/lines when you reach the
   relevant scope item).

Canonical contract: `docs/sprint_objective.md` (this prompt is its executable view —
they are kept in sync). RCA / design rationale (read only if you want the full story):
`docs/solutions/2026-06-09-anti-hardcode-q1-pure-additive-false-positive.md` (Option B).

---

## Class

| Field | Value |
|---|---|
| **Layer (§3.2)** | `infra` — autoloop propose-stage sandbox detector |
| **§7 stanza** | **EXEMPT** — pure infra; detector *scope* refinement only; no semantic surface, no rule-body regex change, no `_normalize` change |
| **Per-sub-sprint Codex (§4.3)** | **REQUIRED** (trigger #3 — modifies the §1.7 anti-hardcode defense). Runs AFTER your dev close, BEFORE the downstream S-Y2 re-tranche |

## Goal

Add **baseline-whitelist suppression** in `anti_hardcode_check()` so a rule match is
ignored when the substring it matched already exists in the candidate's
`before_value`. This unblocks legitimate pure-additive candidates on
`resolve_faq_grounded_answer.yaml $.procedure` (the S-Y2 pilot's most-plausible CS4
surface) without weakening the detector against patterns the candidate actually adds.

## Why (verified defect evidence)

- `autoloop/autoloop/sandbox/anti_hardcode_check.py:353` —
  `text = hypothesis.after_value or ""`: the detector scans the FULL normalized
  `after_value`, never a diff, so a baseline pattern in the field is re-scanned on
  every iteration.
- `server/src/main/resources/skills/resolve_faq_grounded_answer.yaml:30` — baseline
  `$.procedure` contains `...source_url when available, otherwise its article_id) ->
  record_outcome. Only escalate via request_handover...`. After
  `_normalize(synonym_map_enabled=true)`, `when` → `if` and `->` → `→`, yielding an
  `\bif\b ... → ...` arrow-tree that `_RE_Q1_ARROW_TREE`
  (`anti_hardcode_check.py:133-136`) matches.
- Run-1 tranche `autoloop/results/experiments.jsonl`: exp-70 and exp-76 (both target
  `resolve_faq_grounded_answer.yaml $.procedure`) both got
  `anti_hardcode_verdict = FAIL / Q1.if_then_decision_tree` with a **byte-identical**
  `matched_substring` that is **100% baseline text** —
  `"if available, otherwise its article_id) → record_outcome. only escalate via
  request_handover after a valid resolve attempt cannot complete (no viable hit, or
  res"`. Their 1478 / 1441-char appended clauses contribute nothing to the match.
  Both were sandbox path-ACCEPTED but killed by the anti-hardcode check pre-eval.
- The other six candidates (exp-69/71/72/73/74/75) were `PASS` and discarded for
  gate-correct reasons — NOT in scope.

A replay of the real exp-70/76 pairs through baseline-suppression confirms both would
PASS (their appends introduce zero new Q1 matches) — i.e. the fix unblocks exactly
these false-positives while still catching candidate-introduced patterns.

## Code anchors

- `autoloop/autoloop/sandbox/anti_hardcode_check.py`:
  * entry point `anti_hardcode_check()` — line **336**; reads
    `synonym_enabled` (line ~365) and `normalized = _normalize(text, ...)` (line ~366);
    rule loop lines **368-389**; `text = hypothesis.after_value` line **353**.
  * `_normalize` lines **90-105** — **DO NOT CHANGE** (incl. `_SYNONYM_MAP` 69-81,
    `_RE_WHEN_WORD_BOUNDARY` 87).
  * rule-body regexes — **DO NOT CHANGE**: Q1 **128-151**, Q2 **190-197**,
    Q4 **232-240**, Q5 **259-275**.
  * rule helpers to refactor (first-match → iterate): `_q1_*` 154-166, `_q2_*`
    200-222, `_q4_*` 243-254, `_q5_*` 278-306.
  * `_RULES` registry lines **316-328** (the 11 rule entries to cover).
  * `AntiHardcodeResult` dataclass lines **44-61** (optional `matched_in_baseline`
    field for scope item #4).
- `autoloop/autoloop/meta_agent/proposer.py:77` — `Hypothesis.before_value: str`
  (the field you read at the entry point; `after_value` is line 78).
- Tests: `autoloop/tests/test_anti_hardcode_check.py` (432 lines);
  calibration `autoloop/tests/test_real_meta_agent_calibration.py` (17 fixtures, all
  `before_value=""`).
- exp-70/76 rows for the fixture: `autoloop/results/experiments.jsonl` (filter
  `iteration_id` in {`exp-70`,`exp-76`}; the `hypothesis.before_value` /
  `hypothesis.after_value` are the real pair to embed in the suppression test).

## Scope

1. **Entry-point baseline-whitelist suppression** in `anti_hardcode_check()`:
   compute `before_norm = _normalize(hypothesis.before_value or "",
   synonym_map_enabled=synonym_enabled)` once at entry, using the SAME
   `synonym_map_enabled` flag already read for the `after_value` normalization. Apply
   suppression **uniformly across all 11 rules**: return the first rule match whose
   matched span is **not** present in `before_norm`; otherwise PASS (or the existing
   FLAG_FOR_CODEX path for `Q5.standalone_must_borderline`).
2. **Refactor rule helpers to iterate** (`finditer`) instead of returning only the
   first match. Add a shared helper (e.g. `_first_new_match(regexes, text_norm,
   before_norm, predicate=None)`) or thread `before_norm` into each helper.
   **Preserve each rule's existing predicate exactly** — Q2/Q5 soft-dimension
   membership (`_SOFT_SEMANTIC_DIMENSIONS`), the Q2-tier0 / Q4-id_assign surrounding
   windows. Only change "return first match / None" → "return the first
   predicate-qualifying match whose normalized regex span (`m.group(0)`) is not in
   `before_norm`". Suppression is tested against the matched span; the
   `matched_substring` reported in the verdict stays in `after_value` coordinates
   (today's `_trim` display behavior unchanged).
3. **No-op invariant**: when `before_value` is empty / absent the suppression set is
   empty → behavior byte-identical to today. Preserves every existing fixture + the
   17-fixture calibration (all use `before_value=""`).
4. **(Optional, dev-discretion) audit field** — add `matched_in_baseline: bool` to
   `AntiHardcodeResult` ONLY if it stays observation-only metadata inside the verdict
   WITHOUT touching report/audit/schema plumbing beyond that dataclass. If it needs
   wider changes, **skip it** and record the skip in the handoff.

## Hard fences / STOP

- Edit **only** `autoloop/autoloop/sandbox/anti_hardcode_check.py` +
  `autoloop/tests/test_anti_hardcode_check.py` (+ the optional `AntiHardcodeResult`
  field, same file).
- **No regex relaxation** (Q1–Q5 rule-body patterns byte-identical).
- **No `_normalize` change** (incl. `_SYNONYM_MAP` / `_RE_WHEN_WORD_BOUNDARY`).
- **No** edit to `autoloop/autoloop/scoring/*.py` → `scoring_code_baseline_sha` MUST
  stay unchanged → **NO re-bless**.
- **No** edit to other sandbox files (`yaml_diff_validator.py`, `applier.py`,
  `content_validator.py`), `config.yaml`, `program.md`, skill YAMLs, CaseSpecs /
  fixtures, gate-policy / mutable-surface / baseline dirs, `server/**`,
  `eval_interactive/**`, `docs/sprints|milestones|archive`.
- **Double-encoding trade-off ACCEPTED + documented in a test docstring**: a candidate
  copying a baseline pattern verbatim to a new position is suppressed (still
  `in before_norm`) and would NOT fire; the field still shows the doubled pattern to
  audit. This is the accepted limit of baseline-whitelisting.
- **STOP + escalate** if: the 17-fixture calibration outcome changes;
  `scoring_code_baseline_sha` changes; or the fix would require touching any file
  outside the two named above.

## Test / eval requirements

- New `test_anti_hardcode_check.py` cases:
  * `test_q1_arrow_tree_baseline_match_suppressed` — REAL exp-70 `(before, after)`
    pair → assert PASS.
  * `test_q1_arrow_tree_new_match_still_caught` — baseline simple text; after =
    baseline + new `if X → Y` clause → assert FAIL.
  * `test_q1_if_then_baseline_match_suppressed` — synthetic IF/THEN in baseline,
    pure-additive after → assert PASS.
  * `test_q2_q4_q5_baseline_match_suppressed` — lightweight mirrors → assert PASS.
  * `test_double_encoding_bypass_documented` — pattern once in baseline, twice in
    after → assert PASS; docstring documents the accepted trade-off.
- 17-fixture calibration (`test_real_meta_agent_calibration.py`) outcome **UNCHANGED**
  (record before/after counts).
- Full `autoloop` pytest **green** (baseline 331 + new tests).
- `scoring_code_baseline_sha` **unchanged** (prove it; no re-bless).
- Mocked / unit only — per §5.7 this is not behavior-change evidence; the real-LLM
  evidence is the downstream S-Y2 `-n 3` verification tranche, not this sub-sprint.

## Handoff (`docs/sprints/sprint-090-handoff.md`)

Record: LOC; new tests + pass counts; full-suite pytest before/after; calibration
before/after (unchanged); proof `scoring_code_baseline_sha` unchanged (→ no re-bless);
the exp-70/76 replay phrased **exactly** as:
`exp-70/76 anti-hardcode replay: PASS through sandbox check only; not evidence of
eval/gate success.`
the `matched_in_baseline` audit-field decision (added inside verdict / skipped); plus
the standard §7 / surfaced-OQ section.

## Commit discipline

Stage explicitly by file (NO `git add -A`). The dev commit is `autoloop/**` only
(detector + tests). Deliver-agent close artefacts are bundled by the human at close.
**Do not run the autoloop on a dirty tree.**

## Self-check (tick before close)

- [ ] Suppression at the `anti_hardcode_check()` entry point, all 11 rules;
      `before_norm` uses the same `synonym_map_enabled` flag as `after_norm`.
- [ ] Rule-body regexes + `_normalize` byte-identical; only first-match→iterate;
      each rule predicate preserved.
- [ ] No-op when `before_value` empty (existing fixtures + calibration green).
- [ ] 5 new tests (incl. real exp-70 pair + new-pattern-still-FAIL +
      double-encoding-documented); full autoloop pytest green.
- [ ] 17-fixture calibration unchanged (counts recorded).
- [ ] `scoring_code_baseline_sha` unchanged (proof recorded); no re-bless.
- [ ] Only `anti_hardcode_check.py` + `test_anti_hardcode_check.py` touched.
- [ ] `matched_in_baseline` decision recorded.
- [ ] Handoff uses the precise replay phrasing; commit is `autoloop/**` only.
