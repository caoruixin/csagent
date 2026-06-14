---
title: S-Y1.5b/c combo close — anti-hardcode detector scope + severity calibration (implementation plan)
doc_tier: current-runtime
status: current
implementation_status: in_progress
source_of_truth: this file
authored_by: research-agent
authored_date: 2026-06-11
last_reviewed: 2026-06-12
review_cadence: per sub-sprint
supersedes: []
superseded_by: null
notes: >
  Implementation plan promoting two anti-hardcode detector refinements
  into an in-flight S-Y1.5 add-on close before S-Y2 Part C resumes.
  S-Y1.5b (scope fix) is already LANDED in HEAD via S-Auto-35 (the
  `_first_new_match` + `before_norm` baseline-suppression refactor in
  `autoloop/autoloop/sandbox/anti_hardcode_check.py`); this doc records
  its close retrospectively. S-Y1.5c (severity calibration) is NEW
  work: demote Q1.if_then / Q1.arrow_tree / Q1.or_keywords /
  Q2.must_always / Q5.* from FAIL to FLAG_FOR_CODEX while keeping Q4.*
  + Q2.tier0_invention + Q1.contains_matches as FAIL. Per
  deliver-agent decision (2026-06-11), Codex per-sub-sprint review at
  the combo close covers both b and c against the Kernel 9-question
  semantic gate. Resumption: S-Y2 Part C `-n 3` verification tranche
  fires only after combo close + Codex APPROVE + clean tree.

  Source proposals (NOT superseded — kept as research record):
  - docs/solutions/2026-06-09-anti-hardcode-q1-pure-additive-false-positive.md (= S-Y1.5b)
  - docs/solutions/2026-06-11-anti-hardcode-detector-vs-kernel-calibration.md (= S-Y1.5c)
---

# S-Y1.5b/c combo — Anti-hardcode detector scope + severity calibration

## 0. Class and bundle structure

| Field | S-Y1.5b (scope, retrospective) | S-Y1.5c (severity, new) |
|---|---|---|
| **Status** | LANDED — present in HEAD via S-Auto-35 commit set | NOT STARTED |
| **Layer (§3.2)** | `infra` (sandbox scope refinement) | `infra` (sandbox severity calibration; eval-framework-adjacent) |
| **§7 stanza** | §7-EXEMPT (pure-infra, no rule-body change) | **REQUIRED** — touches what counts as §1.7 propose-stage violation |
| **Per-sub-sprint Codex (§4.3)** | REQUIRED — folded into the combo close review | REQUIRED — folded into the combo close review |
| **Re-bless requirement** | None (scoring code SHA stable) | None |
| **Mutable surface touched?** | No | No |

**Bundle close rule** (deliver-agent decision 2026-06-11):

- **One combo handoff, split sections; two §4.2 stanzas** (deliver-agent
  correction 2026-06-11): the close is recorded in a SINGLE combo handoff
  `docs/sprints/sprint-090-handoff.md` with two split sections —
  `S-Y1.5b` (scope fix) and `S-Y1.5c` (severity calibration). The
  S-Y1.5b section is already present (S-Auto-35, LANDED); the S-Y1.5c
  section is added at its close. Codex reviews both in one pass but
  writes TWO distinct `§4.2` close stanzas to `docs/codex-findings.md`
  (one for b, one for c).
- **Codex review is COMBINED**: one §4.3 nine-question walk covers the
  cumulative `anti_hardcode_check.py` diff (b + c) + the propose.txt
  clarification + the 17-fixture re-baseline.
- **Landing order**: scope fix (b) first — verified DONE — then
  severity fix (c). No re-ordering.

## 1. Goal

Make the autoloop sandbox detector a structurally-correct cheap
filter again. Two surgical changes:

1. (S-Y1.5b, DONE) Detector scope: scan the candidate's NEW content
   only (suppress matches present verbatim in `before_value` after
   the same normalization the detector applies to `after_value`).
2. (S-Y1.5c) Detector severity: keep FAIL only on Q-rules whose
   surface form is unambiguous evidence of §1.7 violation
   (Q4.case_id_literal, Q4.id_assign, Q2.tier0_invention,
   Q1.contains_matches code-style literal). Demote Q-rules whose
   surface form requires semantic judgment about
   "rule-dump vs principle-level narrative / few-shot teaching" to
   FLAG_FOR_CODEX, where the per-sub-sprint Codex review at §4.3
   applies the Kernel Q3/Q6 properly.

Combined effect: autoloop pre-eval discards only on ground-truth
syntactic offenses; the §4.1 Kernel becomes the semantic gate it was
constitutionally designed to be.

## 2. S-Y1.5b retrospective close record (scope fix — LANDED)

### 2.1 Change in HEAD (verified)

`autoloop/autoloop/sandbox/anti_hardcode_check.py`:

- New helper `_first_new_match(regexes, text, before_norm, *,
  predicate=None, render=None)` at ~line 120-150. Iterates every
  regex match; returns the first match NOT contained verbatim in
  `before_norm`. When `before_norm` is empty (first-edit scenarios
  or rewrite where suppression doesn't apply) the function is
  byte-identical to the previous `re.search` first-match behavior.
- All `_q1_*` / `_q2_*` / `_q4_*` / `_q5_*` helpers refactored to
  call `_first_new_match` and accept `before_norm` parameter.
- `anti_hardcode_check()` entry point (~line 336+) normalizes
  `hypothesis.before_value` with the SAME `synonym_map_enabled` flag
  used for `after_value`, then threads `before_norm` through to each
  rule helper.

### 2.2 Evidence S-Y1.5b is correct

- Direct: HEAD scan confirms `_first_new_match` + `before_norm`
  parameter on every Q-rule helper.
- exp-70 + exp-76 matched_substring was byte-identical and 100%
  present in `before_value` post-normalization (verified
  programmatically before the patch landed). With baseline
  suppression, the same `before_value`/`after_value` pair returns
  PASS (no NEW match exists).
- 24-slot baseline scan (six skill YAMLs × four mutable field
  classes) reproduces only `resolve_faq.$.procedure` as
  Q1.arrow_tree positive at HEAD — the documented case. After
  scope fix that match is suppressed for pure-additive candidates.

### 2.3 Close artefacts for S-Y1.5b

- Source proposal: `docs/solutions/2026-06-09-anti-hardcode-q1-pure-additive-false-positive.md`.
- Tests added: `tests/test_anti_hardcode_check.py` baseline-suppression
  fixture set (verify via git log on the file).
- §4.2 header to be written into `docs/codex-findings.md` at combo
  close:

```
## Sub-sprint Review Decision — S-Y1.5b
decision: <pass | fix_required | out_of_scope_review>
blocking_count: <number>
summary: Anti-hardcode detector scope refinement. _first_new_match()
  + before_norm baseline-whitelist refactor across Q1/Q2/Q4/Q5
  helpers. No rule-body / regex / severity / mutable-surface change.
  Verified suppresses exp-70/exp-76 false-positive on
  resolve_faq.$.procedure; 24-slot HEAD scan shows zero remaining
  baseline-only Q1 positives. Shadow firewall unaffected; 5-layer
  fitness gate unaffected. Scoring-code SHA stable.
```

## 3. S-Y1.5c contract (severity calibration — TO IMPLEMENT)

### 3.1 Scope (4 items, in order)

#### Item 1 — `_RULES` registry severity changes

Edit `autoloop/autoloop/sandbox/anti_hardcode_check.py` `_RULES`
tuple (~line 305 in HEAD). Apply the rule-severity classification
the deliver-agent locked in 2026-06-11:

| Rule id | Current severity | New default severity | Rationale |
|---|---|---|---|
| `Q1.contains_or_matches_literal` | FAIL | **FAIL (keep)** | `.contains(` / `.matches(` / `.startswith(` / `.endswith(` is code-style enumeration in prose; surface == intent; no legitimate few-shot equivalent |
| `Q1.enumerated_or_keywords` | FAIL | **FLAG_FOR_CODEX** | `'X' or 'Y' or 'Z'` is semantically equivalent to `(e.g., 'X', 'Y', 'Z')` (already production-blessed in confirm.yaml); requires Kernel Q3/Q6 judgment |
| `Q1.if_then_decision_tree` | FAIL | **FLAG_FOR_CODEX** | `if X then Y` shape is shared by principle-level narrative + rule-dump; requires Kernel Q3/Q6 judgment |
| `Q1.arrow_tree` | FAIL | **FLAG_FOR_CODEX** | Arrow form (`if X → Y`, post-normalization) also covers sequence narration ("search → resolve → record"); requires Kernel Q3/Q6 judgment. **Code reality:** `Q1.arrow_tree` is NOT a standalone `_RULES` entry — the arrow regex `_RE_Q1_ARROW_TREE` is evaluated inside `_q1_if_then`, which the registry registers under the single id `Q1.if_then_decision_tree`. Flipping that one registry entry from `_FAIL` to `_FLAG` demotes BOTH the if/then and arrow forms together; do NOT mint a new registry id or split the helper. |
| `Q2.must_always_against_soft_dimension` | FAIL | **FLAG_FOR_CODEX** | Predicate matches §1.3 soft-dim subject, but cannot distinguish "the bot must always preserve PII safety floor" (Runtime-owned, legit) from "the bot must always route UC-A to RESOLVE-FAQ" (LLM-owned, hardcode) — semantic judgment required |
| `Q2.tier0_invariant_invention_attempt` | FAIL | **FAIL (keep)** | Tier-0 minting requires §3.2 explicit authorization; surface form `add tier-0` / `new invariant` / `introduce hard gate` near `add/new/introduce/require` is unambiguous |
| `Q4.case_id_literal` | FAIL | **FAIL (keep)** | `cs<digits>` / `cs_<id>` token IS the §1.7 "encoding raw eval phrases" surface; no semantic interpretation |
| `Q4.id_assignment_literal` | FAIL | **FAIL (keep)** | `session_id:` / `case_id:` / `iteration_id:` / `exp_id:` near `:` or `=` is identifier-binding; surface == intent |
| `Q5.bot_must_always` | FAIL | **FLAG_FOR_CODEX** | Predicate cannot distinguish Runtime-owned safety statements (legit) from LLM-owned soft-decision hardcode — explicit deliver-agent acceptance 2026-06-11 |
| `Q5.do_not_consider_soft_dimension` | FAIL | **FLAG_FOR_CODEX** | LLM-ownership-shrink predicate near §1.3 dim is necessary-but-insufficient; needs Kernel Q3/Q5 |
| `Q5.force_assistant_to` | FAIL | **FLAG_FOR_CODEX** | "Force the assistant to X" — semantic judgment about what X is |
| `Q5.standalone_must_borderline` | FLAG_FOR_CODEX | **FLAG_FOR_CODEX (unchanged)** | Already correctly demoted in S-Auto-4 |

Final FAIL set after this patch: **Q1.contains_or_matches_literal,
Q2.tier0_invariant_invention_attempt, Q4.case_id_literal,
Q4.id_assignment_literal** (4 registry rules).
Final FLAG_FOR_CODEX set: **Q1.if_then_decision_tree,
Q1.enumerated_or_keywords, Q2.must_always_against_soft_dimension,
Q5.bot_must_always, Q5.do_not_consider_soft_dimension,
Q5.force_assistant_to, Q5.standalone_must_borderline** (7 registry
rules). The `Q1.arrow_tree` pattern (above) is covered by the
`Q1.if_then_decision_tree` registry entry via the shared `_q1_if_then`
helper and demotes with it — it is a documented pattern, not a
separate registry id. The registry severity change is therefore
exactly the 11 enumerated entries (4 FAIL + 7 FLAG); no entry is
added or removed.

#### Item 2 — `severity_overrides` config schema

Add to `autoloop/config.yaml` under the EXISTING `anti_hardcode:`
block (per deliver-agent decision 2026-06-11 — nested, not top-level):

```yaml
anti_hardcode:
  enabled: true
  synonym_map_enabled: true                          # unchanged
  flag_for_codex_rate_warn_threshold: 0.25           # unchanged
  # S-Y1.5c — per-rule severity overrides. Empty default. Each key is
  # a rule_id from autoloop/autoloop/sandbox/anti_hardcode_check.py
  # _RULES; value is "FAIL" or "FLAG_FOR_CODEX". Overrides the default
  # severity defined in _RULES. Reversibility hatch — if Codex review
  # at S-Y2 close shows a demoted rule was real hardcode that slipped,
  # re-promote here without code change.
  severity_overrides: {}
```

Detector code reads this map and applies overrides BEFORE consulting
the registry default. Map empty → byte-identical to registry default.
Map populated → per-rule_id override wins.

Implementation sketch (in `anti_hardcode_check.py` near `_RULES`):

```python
def _effective_severity(rule_id: str, default: str, config: dict | None) -> str:
    """Return the effective severity for a rule, honoring overrides.

    Default order: severity_overrides[rule_id] > _RULES default.
    Only "FAIL" and "FLAG_FOR_CODEX" are valid; any other value is
    silently ignored (treated as no override) — this prevents config
    typos from disabling rules.
    """
    overrides = ((config or {}).get("anti_hardcode") or {}).get(
        "severity_overrides"
    ) or {}
    candidate = overrides.get(rule_id)
    if candidate in ("FAIL", "FLAG_FOR_CODEX"):
        return candidate
    return default
```

The main `anti_hardcode_check()` loop calls `_effective_severity`
per rule before deciding whether to short-circuit. FLAG_FOR_CODEX
verdicts still attach to the iteration record via the existing
`anti_hardcode_flag_for_codex = True` path in `loop.py:230-238`.

#### Item 3 — `propose.txt` clarification

Edit `autoloop/autoloop/meta_agent/prompts/propose.txt` "# Forbidden
patterns (§1.7 Constitution — Forbidden)" section. **Code-accuracy
note (deliver-agent 2026-06-11):** HEAD's forbidden list now has SIX
items (lines 32-63; item #6 = the PILOT_PRIMARY_TARGETS bookkeeping
rule), not five, and is immediately followed by the "# Pilot target
steering" section at line 65. Insert the new "# Acceptable patterns"
section AFTER the forbidden list (after line 63) and BEFORE
"# Pilot target steering":

```text
# Acceptable patterns (NOT forbidden — these are legitimate)

The §1.7 forbidden list above targets RULE DUMPS — decision trees
that bind the LLM's semantic choice on dimensions §1.3 says the LLM
owns. The following shapes are NOT rule dumps and are acceptable in
your `after_value`:

1. **Few-shot illustrative examples** that teach a CLASS of
   behavior: `(e.g., "how do I receive payment", "how does payout
   work")` teaches the LLM what a FAQ-shaped question looks like;
   the LLM still judges whether any given customer turn matches.
2. **Principle-level narrative** with observable-state conditioning:
   "When the projection shows a listing context, anchor the answer
   on that listing's state" describes a principle + observable
   signal; the LLM judges applicability.
3. **Sequence narration** describing tool flow: "search_knowledge ->
   resolve_article -> grounded answer -> record_outcome" describes
   the FAQ-path workflow contract; not a rule dump.
4. **Explicit Tool / enum surface legibility**: making
   Runtime-owned schemas (escalation_reason enum,
   intake_state.intake_complete) legible to the LLM via narrative
   that uses the schema's literal names — this is necessary
   projection equivalence, not hardcode.

The autoloop sandbox detector now FLAGs (does NOT FAIL) Q1.if_then,
Q1.arrow_tree, Q1.enumerated_or_keywords, Q2.must_always, and Q5.*
patterns — these flags route to Codex per-sub-sprint review under
the Kernel 9-question gate (`docs/current/anti-hardcode-review-kernel.md`).
A FLAG is not a discard; it is a signal that Codex must judge.

Q4.case_id_literal (raw `cs<id>` tokens) and Q4.id_assignment_literal
(`session_id:` / `case_id:` etc.) and Q1.contains_or_matches_literal
(`.contains(...)` / `.matches(...)`) and Q2.tier0_invariant_invention_attempt
remain FAIL because their surface form == constitutional intent.
```

This clarification keeps the §1.7 norm visible to the proposer
while removing the false implication that any if-then shape triggers
FAIL.

#### Item 4 — demoted-fixture re-baseline (per-fixture review, not bulk)

**Code-accuracy note (deliver-agent 2026-06-11):** there is no single
"11 FAIL / 4 PASS / 2 FLAG over 17 fixtures" assertion (see the §3.2
code-accuracy note for the actual two-file split). The "11 FAIL + 4
PASS + 2 FLAG" figure is calibration narrative in the
`test_real_meta_agent_calibration.py` module docstring and the mirror
comment at `config.yaml:394` (inside the `anti_hardcode:` block, lines
381-406). The assertions the demote actually flips are the individual
per-pattern `assert verdict == "FAIL"` tests in
`test_anti_hardcode_check.py`. After severity demote, the descriptive
distribution shifts to approximately **~2 FAIL / 4 PASS / 11 FLAG** —
update the docstring + config comment to match (doc-hygiene).

**Per-fixture procedure (deliver-agent decision 2026-06-11 — NOT
bulk)**:

1. Run the suite once at HEAD post-Item-1+2+3 implementation.
2. For each fixture whose verdict changes (expected: most FAIL→FLAG,
   no PASS→FAIL, no PASS→FLAG, no FLAG→PASS, no FLAG→FAIL — verify),
   add a one-line Kernel Q3/Q6 rationale to the fixture's
   docstring or test comment: e.g.,
   `# Q3: equivalent soft-signal projection exists via candidate_use_cases — flag, do not fail`
   or `# Q6: principle-level narrative with observable-state guidance — flag, do not fail`.
3. Codex per-sub-sprint review reads each FAIL→FLAG case
   individually against Kernel Q3/Q6 at combo close (§4.2 below).
4. Commit message for the re-baseline must enumerate each
   FAIL→FLAG case with its Kernel rationale (deliver-agent
   close-out review reads commit message).

If any fixture EXPECTED to stay FAIL changes verdict, or vice
versa, **STOP and escalate to human** — the registry change is
incorrect for that pattern.

### 3.2 Test/eval requirements

Add to `autoloop/tests/test_anti_hardcode_check.py`:

- `test_severity_overrides_promote_demoted_rule_back_to_FAIL`:
  set `anti_hardcode.severity_overrides: {"Q1.if_then_decision_tree":
  "FAIL"}`; feed a `(before_value, after_value)` pair that triggers
  Q1.if_then with a NEW pattern; assert verdict = FAIL, rule_id =
  Q1.if_then_decision_tree.
- `test_severity_overrides_demote_fail_to_flag`: set
  `anti_hardcode.severity_overrides: {"Q4.case_id_literal":
  "FLAG_FOR_CODEX"}`; feed an `after_value` with a literal `cs011`;
  assert verdict = FLAG_FOR_CODEX. (Demoting Q4 is HARD-FENCED in
  §4.2 — this test verifies the override MACHINERY works; a
  separate Codex review focus question 3.2 below asserts the
  default registry does NOT demote Q4.)
- `test_default_severity_demotes_q1_if_then_to_flag`: with default
  config (no overrides), feed a NEW Q1.if_then pattern; assert
  verdict = FLAG_FOR_CODEX (NOT FAIL).
- `test_default_severity_keeps_q4_case_id_as_fail`: with default
  config, feed an `after_value` containing `cs_uc_a_no_ad_id`;
  assert verdict = FAIL.
- `test_default_severity_keeps_q1_contains_matches_as_fail`: with
  default config, feed `after_value` containing
  `.contains("refund")`; assert verdict = FAIL.
- `test_default_severity_keeps_q2_tier0_invention_as_fail`: with
  default config, feed `after_value` with "add new tier-0
  invariant"; assert verdict = FAIL.
- `test_propose_txt_contains_acceptable_patterns_section` (in
  `test_loop_anti_hardcode_wire.py` or a new
  `test_propose_prompt.py`): assert the propose.txt file contains
  the literal substring "Few-shot illustrative examples"
  (defensive — clarification must not silently disappear).

Update existing tests:

- All tests that expected `verdict=FAIL` + `rule_id` in the demoted
  set → change expected to `verdict=FLAG_FOR_CODEX`.
- Tests verifying the `anti_hardcode_flag_for_codex` flag attaches
  to the iteration record (in `tests/test_loop.py` or
  `tests/test_loop_anti_hardcode_wire.py`) — verify still true after
  demote (more iterations now produce FLAGs).

Demoted-rule fixture re-baseline:

**Code-accuracy note (deliver-agent 2026-06-11):** the "17-fixture
11-FAIL / 4-PASS / 2-FLAG" distribution is NOT a single exact-count
assertion anywhere. It is split across two files:

1. `test_anti_hardcode_check.py` holds individual per-pattern tests
   (e.g. `test_q1_if_then_decision_tree_rejected`,
   `test_q1_or_keyword_enumeration_rejected`,
   `test_q2_runtime_must_always_on_soft_dimension_rejected`, the Q5.*
   rejection tests) that each `assert res.verdict == "FAIL"`. **These
   are the assertions the demote breaks** — for every test whose
   `rule_id` is in the demoted set, change the expected verdict to
   `FLAG_FOR_CODEX`. Keep the FAIL expectation on the four retained
   rules (Q1.contains/matches, Q2.tier0, Q4.case_id, Q4.id_assign).
   Grep the file for every `== "FAIL"` and triage each against the
   §3.1 Item 1 table — do not assume the list above is exhaustive.
2. `test_real_meta_agent_calibration.py` asserts only (a) sample
   count ≥ 1, (b) FLAG_FOR_CODEX rate < 0.25, and (c) zero FP on
   clean-prose samples — computed over the THREE real samples
   (exp-2/3/4), which are all `clean → PASS`. The demote changes
   only the severity of *matches*; clean-prose samples never match,
   so all three stay PASS and **this file's three assertions stay
   green untouched.** The "11/4/2" wording lives in its module
   docstring (and a mirror comment in `config.yaml`) as historical
   calibration narrative — update both to the new approximate
   distribution (~2 FAIL / 4 PASS / 11 FLAG) as doc-hygiene; neither
   is an executable gate.

- DO NOT bulk-rewrite. Add the Kernel Q3/Q6 rationale line to each
  demoted per-pattern test per §3.1 Item 4.

Re-bless / §5.6 / scoring:

- **No re-bless** of `m-auto-7-prepilot-baseline-20260608` — scoring
  code SHA unchanged.
- **No §5.6 bad-case rerun** — no agent semantic surface touched.
- **No mutable-surface change** — Skill YAMLs byte-identical.
- **No CaseSpec change** — eval_interactive byte-identical.
- **Java baseline `1383/1/0/2` unchanged** — no Java code touched.

### 3.3 Per-sub-sprint Codex review plan (§4.3)

REQUIRED (combo). One §4.3 nine-question kernel walk covers the
cumulative diff (S-Y1.5b + S-Y1.5c). Two §4.2 headers written to
`docs/codex-findings.md` — one for b, one for c.

**Codex focus questions for S-Y1.5c specifically**:

1. **Severity demote scope correctness**: is the FAIL-set
   reduced to ONLY rules whose surface form == constitutional
   intent (Q4.case_id_literal, Q4.id_assignment_literal,
   Q2.tier0_invariant_invention_attempt,
   Q1.contains_or_matches_literal)? Are the demoted rules
   genuinely semantic-judgment-requiring per Kernel Q3/Q6? Read
   each rule's regex + predicate against the
   `_SOFT_SEMANTIC_DIMENSIONS` set and the Kernel.
2. **17-fixture per-fixture review**: read each FAIL→FLAG fixture's
   added Kernel Q3/Q6 rationale; verify the demote is justified.
   If ≥ 1 fixture's demote is unjustified per Kernel, the patch
   FAILs Codex review.
3. **Q4 default unchanged**: confirm default `_RULES` registry
   still produces FAIL for `cs011` / `cs_uc_a_no_ad_id` style
   literals + `session_id=` style assignments. (Override
   machinery works for Q4 → FLAG demote — that's intentional
   reversibility — but the DEFAULT must keep Q4 as FAIL.)
4. **propose.txt clarification compatibility**: read the new
   "# Acceptable patterns" section. Does it open any §1.7 bypass
   the proposer could exploit (e.g., framing real rule dumps as
   "few-shot examples")? Compare against Kernel Q3 + Q6.
5. **FLAG_FOR_CODEX surfacing path**: verify the iteration record
   schema (`anti_hardcode_flag_for_codex` + `anti_hardcode_verdict`)
   is preserved end-to-end through `experiments.jsonl` +
   `runs/<id>/anti_hardcode_verdict.json` + `autoloop audit`
   output. Without surfacing, the demote becomes a §1.7 bypass.
6. **Shadow firewall unaffected**: confirm neither S-Y1.5b nor
   S-Y1.5c reads or routes shadow data.
7. **Scoring-code SHA stable**: confirm the patch does not edit any
   file in `_SCORING_CODE_FILES` (`tier_evaluator.py`,
   `eval_runner.py`, `baseline_loader.py`, `gaming.py`,
   `aggregate.py`); `gaming.scoring_code_drift.sha_changed` must
   NOT fire post-merge.
8. **Reversibility actually reversible**: write a `dry-run` test
   where `anti_hardcode.severity_overrides:
   {Q1.if_then_decision_tree: "FAIL"}` is set; confirm a propose
   that triggers Q1.if_then on NEW content returns
   `verdict=FAIL`. Without this hatch, the demote is one-way.

**§4.2 headers for `docs/codex-findings.md`** (two stanzas):

```
## Sub-sprint Review Decision — S-Y1.5b
decision: <pass | fix_required | out_of_scope_review>
blocking_count: <number>
summary: <one paragraph>

## Sub-sprint Review Decision — S-Y1.5c
decision: <pass | fix_required | out_of_scope_review>
blocking_count: <number>
summary: <one paragraph>
```

### 3.4 Pre-close sanity check (deliver-agent decision 2026-06-11)

Run a **1-2 dry-run iteration sanity pass** before combo close:

```bash
cd autoloop && uv run python -m autoloop run --dry-run -n 2
```

Then read `runs/exp-<N>/hypothesis.json` for each dry-run iteration
and confirm:

- The proposer's `after_value` does NOT contain raw eval CaseSpec
  ids (Q4 protection still bites at dry-run).
- The `rationale` does NOT excessively use "the bot must always
  X" / "the runtime must never Y" against §1.3 soft dimensions
  (Q5.* still surfaces via FLAG).
- The `after_value` reads as principle-level narrative or
  few-shot teaching — NOT as if-then rule dumps with quoted
  user_message keywords.

If ANY dry-run iteration shows the proposer over-correcting toward
real rule dumps (Q4 leakage; gratuitous always/must/never wording;
case_id literals; per-UC if-else lattices), **STOP and tune the
propose.txt clarification** — the proposer must not infer "FLAG is
acceptable so I'll write anything." If ALL dry-run iterations look
normal, proceed to close.

The dry-run evidence (the 1-2 hypothesis.json snapshots + a short
checklist verdict per each) is recorded in the S-Y1.5c handoff
under "Pre-close sanity check evidence" section.

## 4. Combined hard fences

These hold across BOTH S-Y1.5b (retrospective; already enforced) and
S-Y1.5c (new; enforced by code review + Codex):

- **No §1.7 constitution change**: `docs/current/iteration_governance.md`
  §1.7 byte-identical.
- **No Kernel change**:
  `docs/current/anti-hardcode-review-kernel.md` byte-identical.
- **No 5-layer fitness gate change**:
  `autoloop/autoloop/scoring/*.py` byte-identical;
  `gaming.scoring_code_drift.sha_changed` MUST NOT fire post-merge
  (`scoring_code_baseline_sha = 0d86b08f…` stays).
- **No CaseSpec / fixtures / baseline change**:
  `eval_interactive/**` byte-identical.
- **No Skill YAML change**:
  `server/src/main/resources/skills/**` byte-identical.
- **No mutable-surface change**:
  `autoloop/config.yaml:mutable_surface` block byte-identical.
- **No re-bless** of `m-auto-7-prepilot-baseline-20260608`.
- **No `baseline_dir` / `current_eval_baseline.md` canonical flip**.
- **Q4 raw-eval-phrase protection stays FAIL**: default `_RULES`
  registry keeps `Q4.case_id_literal` + `Q4.id_assignment_literal`
  as FAIL. The override map MAY demote Q4 per-experiment for
  forensic reasons, but the DEFAULT must not.
- **Java code byte-identical**: `server/src/main/java/**`
  byte-identical (Java baseline `1383/1/0/2` unchanged).
- **Shadow firewall unaffected**: detector reads only `before_value`
  + `after_value` strings; no eval result reads; no shadow data
  involved.
- **propose.txt clarification must NOT silently disappear**: unit
  test asserts the literal "Few-shot illustrative examples"
  substring is present.

## 5. Sequence + dependencies

| Step | Owner | Depends on | Time est |
|---|---|---|---|
| 1. Verify S-Y1.5b LANDED in HEAD (spot-check `_first_new_match` + `before_norm` presence) | dev agent | — | 5 min |
| 2. Implement S-Y1.5c Item 1 (`_RULES` severity changes) | dev agent | 1 | 15 min |
| 3. Implement S-Y1.5c Item 2 (`severity_overrides` config + `_effective_severity` helper) | dev agent | 2 | 30 min |
| 4. Implement S-Y1.5c Item 3 (propose.txt clarification) | dev agent | — (parallel to 2-3) | 10 min |
| 5. Add unit tests per §3.2 | dev agent | 2-4 | 45 min |
| 6. Run 17-fixture suite; per-fixture re-baseline per §3.1 Item 4 + commit message enumeration | dev agent | 2-5 | 60-90 min |
| 7. Run §3.4 pre-close sanity check (1-2 dry-run iterations) | deliver / dev agent | 2-6 | 15-30 min |
| 8. Extend the single combo handoff `docs/sprints/sprint-090-handoff.md` — it already carries the S-Y1.5b scope-fix section (S-Auto-35, LANDED); add the S-Y1.5c severity-calibration section + the two split §4.2 Codex stanzas | deliver agent | 7 | 30 min |
| 9. Codex per-sub-sprint combo review per §3.3 | Codex | 8 + clean tree | (Codex side) |
| 10. Write `docs/codex-findings.md` two §4.2 headers | Codex | 9 | (Codex side) |
| 11. Verify combo close: `docs/codex-findings.md` shows two PASS stanzas + blocking_count=0 for both | deliver agent / human | 10 | 10 min |
| 12. Resume S-Y2 Part C: `run -n 3` under the existing S-Y2 pilot block, with special attention to whether `resolve_faq.$.procedure` candidate-shape proposals now pass anti-hardcode and enter eval | deliver agent | 11 + clean tree | (S-Y2 side) |

**Total S-Y1.5c dev time**: ~3-4.5 h (steps 2-7).
**Critical path to S-Y2 resume**: steps 1-11.

## 6. Handoff requirements

The combo close is recorded in ONE handoff file —
`docs/sprints/sprint-090-handoff.md` — with two split sections (per
deliver-agent correction 2026-06-11). The **S-Y1.5b scope-fix section
is already present in HEAD** (the S-Auto-35 handoff, commit set landing
`_first_new_match` + `before_norm`). The dev agent for S-Y1.5c **adds**
the S-Y1.5c severity-calibration section to the SAME file; it does not
create a separate handoff. Codex writes two §4.2 stanzas (one for b,
one for c) into `docs/codex-findings.md`.

### 6.1 `docs/sprints/sprint-090-handoff.md` — Section A: S-Y1.5b scope fix (ALREADY PRESENT)

The existing S-Auto-35 handoff body is the S-Y1.5b section. No
re-authoring at S-Y1.5c close — it is retained as-is. For reference,
its content covers: `infra` / §7-EXEMPT / Codex REQUIRED (combo); the
`_first_new_match` + `before_norm` baseline-whitelist refactor; the
baseline-suppression test fixtures; the exp-70 / exp-76 reproduction
(now PASS under suppression); `scoring_code_baseline_sha` stable → no
re-bless. Its §4.2 verdict slot is filled by Codex at combo close.

### 6.2 `docs/sprints/sprint-090-handoff.md` — Section B: S-Y1.5c severity calibration (ADD AT CLOSE)

Append a clearly-delimited "## S-Y1.5c — severity calibration" section
with:

- Class — `infra` / §7-REQUIRED / Codex REQUIRED (combo)
- Goal — anti-hardcode detector severity calibration: demote
  semantic-judgment Q-rules to FLAG_FOR_CODEX; keep
  surface-form-unambiguous rules as FAIL; add `severity_overrides`
  reversibility hatch
- Diff summary — list the 4 changed files
  (`anti_hardcode_check.py`, `config.yaml`, `propose.txt`,
  `test_anti_hardcode_check.py` + `test_real_meta_agent_calibration.py`)
  + commit SHA range
- Per-rule severity table (copy of §3.1 Item 1, incl. the
  `Q1.arrow_tree` row)
- Demoted-fixture re-baseline — the per-pattern FAIL→FLAG test-update
  table (the individual `assert verdict == "FAIL"` tests in
  `test_anti_hardcode_check.py` for demoted rule_ids) + Kernel Q3/Q6
  rationale for each, plus the stale-count hygiene fix to the
  `test_real_meta_agent_calibration.py` docstring + `config.yaml`
  comment (see §3.2 code-accuracy note)
- §3.4 Pre-close sanity check evidence — 1-2 dry-run
  hypothesis.json snapshots + checklist verdict
- §7 stanza (copy of §7 below)
- §4.1/§4.3 verdict — Codex (combo) per `docs/codex-findings.md` §4.2 header
- Reversibility — `severity_overrides` config map; example flip
  documented
- No re-bless / no Skill YAML touch / no scoring code touch /
  no §1.7 / Kernel / 5-layer change
- Observability follow-up R-item — "Track Codex `reject` rate on
  FLAG_FOR_CODEX patterns at S-Y2 close; if > 30%, reconsider
  demote scope at M-Auto-8" (per deliver-agent decision 2026-06-11
  — observability R-item, not §5.X gate)

### 6.3 `docs/codex-findings.md` close artefacts

Two §4.2 headers (drafts in §2.3 + §3.3 above). Codex fills in
`decision` / `blocking_count` / `summary` after review.

## 7. §7 Layer-classification + anti-hardcode stanza (S-Y1.5c)

```markdown
## Layer-classification + anti-hardcode stanza

**Target failure layer:** `infra` (autoloop sandbox detector severity
calibration; eval-framework-adjacent — §5.8 framework-defect priority
precedent applies in spirit because the propose-stage filter was
blocking on-target candidates from reaching eval for false-positive
reasons during an in-flight semantic pilot).

**Tier-0 invariant:** This sub-sprint adds no Tier-0 invariant. The
demote does NOT change what §1.7 forbids; it only changes how the
autoloop sandbox's propose-stage filter expresses uncertainty about
borderline semantic patterns (FLAG_FOR_CODEX = "Codex must judge"
vs FAIL = "definitely a hardcode"). The semantic gate is restored
to per-sub-sprint Codex review (§4.3) which is where it lived
constitutionally before the detector's S-Auto-4 over-tightening.
Structural defenses retained:
- Q4.case_id_literal stays FAIL (§1.7 "encoding raw eval phrases").
- Q4.id_assignment_literal stays FAIL.
- Q2.tier0_invariant_invention_attempt stays FAIL (§3.2 Tier-0
  minting authorization).
- Q1.contains_or_matches_literal stays FAIL (code-style enumeration
  surface == intent).
- The shadow firewall is unaffected.
- The mutable surface YAML-diff sandbox is unaffected.
- The 5-layer lexicographic fitness gate is unaffected.
- Per-sub-sprint Codex review (§4.3) IS the semantic-judgment
  binding gate, which is what §4.1 Kernel already specifies.

Reversibility: `anti_hardcode.severity_overrides` config map lets
any demoted rule be re-promoted to FAIL per-experiment without code
change. If post-S-Y1.5c Codex review at S-Y2 close shows a specific
FLAG_FOR_CODEX pattern is leaking real hardcode through, the
override flips that rule back to FAIL.

**Semantic hardcode:** No semantic hardcode introduced. This patch
removes false-positive hardcode rejections that were blocking
legitimate principle-level narrative + few-shot illustrative
teaching (e.g. exp-70 / exp-76 on `resolve_faq.$.procedure`). The
propose.txt clarification adds positive guidance (few-shot examples,
principle-level narrative, sequence narration, schema legibility are
acceptable) without weakening the §1.7 forbidden list.

**Generalization coverage:** target / neighbor / negative / shadow
case counts: n/a — pure-infra sub-sprint touching only
`autoloop/autoloop/sandbox/anti_hardcode_check.py` +
`autoloop/autoloop/meta_agent/prompts/propose.txt` +
`autoloop/config.yaml` (severity_overrides schema) +
`autoloop/tests/test_anti_hardcode_check.py` +
`autoloop/tests/test_real_meta_agent_calibration.py`. No agent
semantic surface touched. Validation = unit tests + 17-fixture
per-fixture re-baseline + §3.4 pre-close 1-2 dry-run sanity check.
```

## 8. Decisions log (deliver-agent decisions, 2026-06-11)

Mapping deliver-agent §13 decisions from the source proposal to
their concrete location in this implementation plan:

| Decision | Location |
|---|---|
| Bundle with 2026-06-09 sibling scope fix | §0 bundle structure + §2 + §3 |
| Landing order: scope first → severity second; close records SPLIT | §0 + §2.3 + §3.3 (two §4.2 headers) |
| 17-fixture re-baseline per-fixture, not bulk | §3.1 Item 4 + §3.2 17-fixture procedure + §3.3 Codex focus #2 + §6.2 §4 handoff section |
| FLAG_FOR_CODEX surfacing in Codex close-out is HARD requirement | §3.3 Codex focus #5 + §4 (last fence) + §6.2 §4 handoff section |
| `severity_overrides` under existing `anti_hardcode:` block (nested, not top-level) | §3.1 Item 2 |
| propose.txt clarification + 1-2 dry-run sanity check before close | §3.1 Item 3 + §3.4 + §6.2 §5 handoff section |
| Codex reject rate on FLAG patterns is observability R-item, NOT §5.X gate | §6.2 §10 handoff section |
| Q5.bot_must_always demote accepted (semantic-judgment case) | §3.1 Item 1 row 9 |
| Q1.contains_matches stays FAIL (code-style is surface == intent) | §3.1 Item 1 row 1 |
| Q4.* stays FAIL (raw eval phrase + identifier binding) | §3.1 Item 1 rows 7-8 + §4 (last fence) + §3.3 Codex focus #3 |
| Q2.tier0_invention stays FAIL | §3.1 Item 1 row 6 |

## 9. Resumption gate for S-Y2 Part C

After combo close + Codex APPROVE for both S-Y1.5b and S-Y1.5c +
`docs/codex-findings.md` shows `blocking_count=0` on both + clean
working tree:

1. Verify `gaming.scoring_code_drift.sha_changed` did NOT fire on
   the merged commit (`gaming.py` SHA stable).
2. Verify `autoloop preflight` returns clean.
3. Run `-n 3` under the existing S-Y2 pilot block, with special
   attention to whether `resolve_faq.$.procedure` candidate-shape
   proposals now pass anti-hardcode and enter eval. There is no
   targeted/replay mode; the proposer's natural steering after
   S-Y1.5 is already biased to `resolve_faq_grounded_answer.yaml`.
4. Read each iteration's `anti_hardcode_verdict.json` + the
   experiments.jsonl row's `anti_hardcode_flag_for_codex` field.
5. Apply S-Y2 §3 decision rules (4-layer hit-rate;
   `full_on_gap_hit_rate` thresholds; § 3.4 fallback on ≤ 25%,
   strong-pass on ≥ 50%, partial-pass / extend on 25-50%).
6. Two evaluations that MUST resolve at the n=3 tranche:
   - Do exp-70 / exp-76 candidate-shape candidates now reach eval?
     (i.e. Q1.arrow_tree FLAG no longer kills them).
   - Among the eval-reached candidates, does any flip the
     CS4 PRIMARIES (`cs_uc_a_no_ad_id_ad_specific` /
     `cs_uc_a_loaded_listing`)? If yes → S-Y2 PRIMARY SUCCESS
     pathway. If no → §3.4 hand-authored fallback with the
     PRIMARIES question honestly answered.

## 10. Sources of truth

- This implementation plan is binding; it is the source-of-truth for
  the S-Y1.5b/c combo close. Codex review reads against this doc +
  the underlying proposals.
- Source proposals (research record; NOT superseded):
  - `docs/solutions/2026-06-09-anti-hardcode-q1-pure-additive-false-positive.md` (= S-Y1.5b)
  - `docs/solutions/2026-06-11-anti-hardcode-detector-vs-kernel-calibration.md` (= S-Y1.5c)
- Constitution: `docs/current/iteration_governance.md` §1.7 / §3.2 /
  §4.1 / §4.3 (governance unchanged; the implementation plan does not
  edit these).
- Kernel: `docs/current/anti-hardcode-review-kernel.md` (the
  9-question semantic gate Codex applies at combo close;
  byte-identical post-merge).
- Code: `autoloop/autoloop/sandbox/anti_hardcode_check.py`
  (S-Y1.5b LANDED + S-Y1.5c to be edited),
  `autoloop/autoloop/meta_agent/prompts/propose.txt`,
  `autoloop/config.yaml`,
  `autoloop/tests/test_anti_hardcode_check.py`,
  `autoloop/tests/test_real_meta_agent_calibration.py`,
  `autoloop/tests/test_loop_anti_hardcode_wire.py` (or sibling).
- Pilot context: `docs/sprint_objective.md` (S-Y2 PLACEHOLDER —
  paused; resumes per §9 after combo close).
- Forensic: `autoloop/results/experiments.jsonl` (exp-69..exp-76;
  particularly exp-70 / exp-76 for S-Y1.5b validation).

## 11. Combo review outcome + S-Y1.5d targeted fix-iteration (2026-06-11)

The S-Y1.5b/c combo was implemented and reviewed:

- **S-Y1.5b** = commit `9c62a86` (landed earlier).
- **S-Y1.5c** = commit `75b302c` (registry severity demote + override
  hatch + propose.txt clarification + tests; autoloop pytest 344 green;
  `scoring_code_baseline_sha` stable → no re-bless; §3.4 dry-run exp-77
  clean → PROCEED).

**Combined per-sub-sprint §4.3 Codex review** (prompt
`compact/sprint-090-review-prompt.md`; verdicts in
`docs/codex-findings.md`):

- **S-Y1.5b: `pass` / blocking_count 0** — the standalone approve is
  re-confirmed under the combined lens; c does not touch suppression /
  normalization / rule bodies.
- **S-Y1.5c: `fix_required` / blocking_count 1** — Kernel verdict
  `needs human architecture decision`, blocker **R-S90.5**.

**R-S90.5** (layer `infra`): `Q4.case_id_literal`'s regex
`\bcs[0-9a-z_]{2,}\b` (`anti_hardcode_check.py:274`) false-FAILs ordinary
`cs…` words (`CSAT`, `csagent`, `css`), so the retained default-FAIL set is
NOT limited to "surface == constitutional intent" — undermining S-Y1.5c's
load-bearing claim and re-opening a false-positive-discard vector on the
`resolve_faq` pilot surface. Codex confirmed `cs011`, `cs_uc_a_no_ad_id`,
`session_id=abc`, `case_id: 42` still correctly FAIL. Logged in
`docs/action_bank.md` (Sprint 090 surfaced backlog) as the current blocking
finding.

**Human architecture decision (2026-06-11): tighten the regex, keep FAIL.**
Resolved by a targeted fix-iteration **S-Y1.5d / S-Auto-37** (dev prompt
`compact/sprint-090d-dev-prompt.md`):

- Tighten `_RE_Q4_CASE_ID_TOKEN` to require a CaseSpec-id discriminator
  (`cs` immediately followed by digit/underscore, e.g.
  `\bcs[0-9_][0-9a-z_]+\b`); keep `Q4.case_id_literal` severity `_FAIL`
  (the §1.7 raw-eval-phrase hard-discard for real ids is preserved).
- Binding test matrix: `cs011`/`cs_uc_a_no_ad_id`/`cs38s01`/`cs59s` FAIL;
  `CSAT`/`csagent`/`css` PASS; mid-word `cs` stays PASS.
- One regex line + tests, `autoloop/**` only; full pytest green;
  `scoring_code_baseline_sha` stable → no re-bless; no §3.4 dry-run needed
  (detector only becomes more permissive on a false-positive class).
- A **targeted §4.3 Codex re-review** of the Q4 regex diff then flips the
  S-Y1.5c stanza `fix_required → pass`/0 and marks R-S90.5
  resolved-by-S-Y1.5d. This makes S-Y1.5c's "four retained FAILs are
  surface==intent" thesis actually true (the discriminator makes
  `cs<digit|_>` a CaseSpec-id surface).

**Update 2026-06-12 — R-S90.6 (S-Y1.5d needed a second attempt).** S-Y1.5d
attempt-1 (`40f8c07`, regex `\bcs[0-9_][0-9a-z_]+\b`) was HELD by the
targeted Codex re-review (`fix_required`/1): requiring the discriminator
*immediately after* `cs` let real `cs`+letter ids (the
`manual_probe_uc_a_resolve_must` family `csmp_g01…`/`csmp_s01…`/`csmp_n01…`)
PASS — a false-negative weakening the Q4 hard-discard (**R-S90.6**, logged
in `docs/action_bank.md`). Deliver-agent verified (direct Python `os.walk` from the
repo root) that the corpus has **452 distinct cs-ids, all containing a
digit or underscore, 0 pure-letter cs-ids**. (Recursive shell searches gave
misleading empty results mid-investigation — a shell `cwd` had drifted into
`autoloop/`, so relative `eval_interactive/…` searches found nothing; run
from the repo root or use absolute paths.) **Human decision 2026-06-12: corrected regex
`\bcs[a-z0-9_]*[0-9_][a-z0-9_]*\b`** (digit/underscore ANYWHERE in the
token), keep `_FAIL`. Verified: catches all 452 ids (0 misses incl.
`csmp_*`), passes `csat`/`csagent`/`css`/`csv`; residual `css3`/`cs50`
non-blocking OQ. Attempt-2 = revised `compact/sprint-090d-dev-prompt.md` +
re-review against the full 452-id matrix.

**Updated close + resumption sequence** (supersedes the b+c framing in §0 /
§5 / §9): combo close = **b + c + d**. The §5 step 12 / §9 S-Y2 Part C
`run -n 3` re-tranche is gated behind **S-Y1.5d attempt-2 landing + its
targeted Codex re-review passing + a clean tree**, not behind the original
b+c close.

---

End of implementation plan. Awaiting dev-agent execution of §5
sequence + Codex per-sub-sprint combo review per §3.3 + deliver-agent
combo close per §6 handoff. Research agent has not modified code.
