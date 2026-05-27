---
title: Sprint 57 / M-Auto-1A S-Auto-4 — real anti-hardcode kernel + content validator + 7 anti-gaming checks — dev handoff
doc_tier: sprint-archive
status: current
implementation_status: implemented
source_of_truth: this file (sub-sprint dev archive); autoloop/autoloop/sandbox/{anti_hardcode_check,content_validator}.py + autoloop/autoloop/scoring/gaming.py + autoloop/autoloop/loop.py (additive wiring) + autoloop/autoloop/cli.py (audit extension) + autoloop/config.yaml (3 NEW blocks + scoring_code_baseline_sha) + autoloop/program.md §4 row 3 + autoloop/README.md audit row
last_reviewed: 2026-05-27
review_cadence: ad hoc
supersedes: []
superseded_by: null
notes: >
  Fourth sub-sprint of Milestone M-Auto-1A. S-Auto-4 swaps the
  S-Auto-3 placeholder always-PASS `anti_hardcode_check` body for a
  deterministic regex+heuristic detector covering Q1/Q2/Q4/Q5 of the
  §4.1 nine-question kernel (PASS / FAIL / FLAG_FOR_CODEX verdicts;
  FLAG_FOR_CODEX is observation-only in v1). Adds a new pre-sandbox
  `content_validator` (placeholder integrity + length sanity + deny
  list) and a new post-eval `scoring/gaming.py` (7 anti-gaming checks
  per `docs/proposals/autoloop_design.md` §8 plus a NEW
  `tier2_measurement_contract_change_attempt` check). All gaming
  checks ship OBSERVATION-ONLY in v1; ERROR-severity promotion to
  gating is a M-Auto-1B decision. Wires the three modules into
  `loop.py` at three additive insertion points (pre-sandbox content
  validator at step 2.5; FLAG_FOR_CODEX flag-attach at step 4;
  post-eval `gaming.detect` at step 9.5). `IterationResult`
  dataclass extended with three additive optional fields
  (`content_validator_verdict`, `gaming_flags`,
  `anti_hardcode_flag_for_codex`) — S-Auto-3 tests that construct
  `IterationResult(iteration_id=...)` without these fields still
  PASS. `autoloop/cli.py audit` extended to surface
  `gaming_flags_summary` + `anti_hardcode_flag_for_codex`; default
  mode continues to RESPECT the shadow firewall;
  `--include-shadow-detail` flag behavior unchanged. 69 new pytest
  tests; total autoloop suite 147 → 216 PASS (0 fail).
  eval_interactive baseline reproduces `486 passed, 3 failed`. Zero
  edits to `server/`, `eval/`, inner `eval_interactive/eval_interactive/`,
  `eval_interactive/case_specs/`, `eval_interactive/case_specs_shadow/`,
  `data/`, `db/`, `server/src/main/resources/`, `docs/foundational/`,
  `docs/runtime_freeze_and_risk_policy.md`, `docs/current/`,
  `docs/sprints/sprint-0*` (other than the new S-Auto-4 handoff
  authored here), `docs/milestones/`, `docs/codex-findings.md`. §12
  reserved for deliver-agent + human at sub-sprint close. Per §4.3
  trigger #2 (the kernel IS a structural §1.7 defense), per-sub-sprint
  Codex review is REQUIRED at S-Auto-4 close BEFORE M-Auto-1A milestone
  close; deliver-agent authors the Codex prompt at S-Auto-4 close, dev
  does NOT dispatch Codex.
---

# Sprint 57 / M-Auto-1A S-Auto-4 — real anti-hardcode kernel + content validator + 7 anti-gaming checks — dev handoff

## 1. Goal and outcome

**Goal (from `compact/sprint-057-dev-prompt.md`):**
Replace the S-Auto-3 placeholder `anti_hardcode_check` hook with a
deterministic regex+heuristic structural detector (NO second LLM; NO
new heavy dependency); ship `content_validator.py` (placeholder
integrity, length sanity, deny-list); ship `scoring/gaming.py` (6
anti-gaming checks from `docs/proposals/autoloop_design.md` §8 plus a
NEW `tier2_measurement_contract_change_attempt` check, all
observation-only in v1); wire all three into `loop.py` at the correct
stage boundaries without changing the signature of any S-Auto-1/2/3
deliverable; flip `autoloop/program.md` §4 row 3 status from DEFERRED
to DELIVERED.

Close gates per contract:
- Detector covers Q1 / Q2 / Q4 / Q5 of the §4.1 nine-question kernel;
  PASS / FAIL / FLAG_FOR_CODEX verdicts.
- Calibration table: ≥ 8 forbidden propose examples all auto-rejected;
  ≥ 4 clean soft-narrative examples all PASS; ≥ 2 borderline
  FLAG_FOR_CODEX cases; FLAG_FOR_CODEX rate ≤ 25% over the
  calibration set.
- D2 regression: detector source contains no specific eval CaseSpec
  id literal / user utterance / expected assistant answer / case-
  success label.
- Rule count ≤ 30 (anti-pattern explosion guard).
- D3 regressions: `gaming.scoring_code_drift.baseline_missing` emits
  WARN when `config.fitness.scoring_code_baseline_sha` is None;
  `gaming.shadow_set_leakage` is config-driven (no shadow-corpus
  read).
- Existing S-Auto-3 `test_adversarial_fixture_passes_placeholder_anti_hardcode`
  renamed + assertion-flipped to
  `test_adversarial_fixture_fails_real_detection`; new assertion is
  `decision=="discard"` AND
  `discard_reason.startswith("anti_hardcode_rejected:")`.
- `cd autoloop && uv run pytest -q` → ≥ 197 PASS (147 baseline + 50+
  NEW); 0 FAIL.
- `cd eval_interactive && uv run python -m pytest --tb=no -q` →
  `486 passed, 3 failed` UNCHANGED.
- Zero edits to hard-fenced surfaces enumerated above.

**Outcome:** delivered against contract.

- 3 NEW source artefacts:
  - `autoloop/autoloop/sandbox/anti_hardcode_check.py` — placeholder
    body swapped for real detector. 389 LOC. 11 rules (well under
    the 30 cap).
  - `autoloop/autoloop/sandbox/content_validator.py` — NEW. 187 LOC.
    5 rules: zero_length, deny_list, length_overflow, length_underflow,
    placeholder_corrupted.
  - `autoloop/autoloop/scoring/gaming.py` — NEW. 627 LOC. 7 checks
    spread across `_check_*` functions; all observation-only.

- 4 NEW test files + 2 updated tests in existing file:
  - `autoloop/tests/test_anti_hardcode_check.py` — 348 LOC, 27 tests
    (forbidden positives across Q1/Q2/Q4/Q5; clean negatives;
    borderline FLAG_FOR_CODEX; unicode/multi-line/synonym
    normalization; detector self-discipline grep regression).
  - `autoloop/tests/test_content_validator.py` — 251 LOC, 17 tests
    (zero_length / placeholder corruption / length sanity / deny
    list / D3 regression).
  - `autoloop/tests/test_gaming.py` — 468 LOC, 16 tests (positive +
    negative per check + D3 regressions on baseline_missing and
    shadow leak signature config-drivenness).
  - `autoloop/tests/test_loop_anti_hardcode_wire.py` — 340 LOC, 4
    tests covering the three insertion points + IterationResult
    backward-compat.
  - `autoloop/tests/test_loop.py` — TWO updates:
    - `test_anti_hardcode_placeholder_always_passes` renamed +
      rewritten as `test_anti_hardcode_real_detector_passes_clean_propose`;
      now asserts `verdict == "PASS"` and `placeholder is False`.
    - `test_adversarial_fixture_passes_placeholder_anti_hardcode`
      renamed + flipped as `test_adversarial_fixture_fails_real_detection`;
      now asserts `decision == "discard"` and
      `discard_reason.startswith("anti_hardcode_rejected:")`. This
      is the load-bearing placeholder → real transition regression
      target.

- Modifications (additive only; no signature change to any
  S-Auto-1/2/3 deliverable):
  - `autoloop/autoloop/loop.py` — three insertion points added:
    (A) pre-sandbox content validator at step 2.5;
    (B) FLAG_FOR_CODEX flag-attach at step 4 (no discard);
    (C) post-eval `gaming.detect` at step 9.5 (`gaming_flags`
    serialized into the experiments_log row).
    `IterationResult` dataclass extended with three additive
    optional fields (`content_validator_verdict`, `gaming_flags`,
    `anti_hardcode_flag_for_codex`) — all with default values so
    S-Auto-3 callers that build `IterationResult(iteration_id=...)`
    without these fields still PASS. Top-of-file docstring updated
    to reflect 2.5 / 9.5 steps. `_build_record_dict`,
    `_run_gaming_detect`, and `_write_dry_run_artefacts` extended
    additively. `discard_reason` formatter for anti-hardcode FAIL
    now uses `ah_verdict.rule_id` (the canonical S-Auto-4 field) in
    place of the placeholder's free-form `reason` string.
  - `autoloop/autoloop/cli.py audit` — surfaces
    `anti_hardcode_flag_for_codex` + `gaming_flags_summary` in the
    rendered JSON. Default mode RESPECTS shadow firewall (no
    per-case shadow lookup). `--include-shadow-detail` flag behavior
    unchanged.
  - `autoloop/autoloop/sandbox/__init__.py` — re-exports
    `ContentValidationResult` + `validate_content`.
  - `autoloop/autoloop/scoring/__init__.py` — re-exports `GamingFlag`
    + `detect`.
  - `autoloop/config.yaml` — appended three NEW blocks
    (`content_validator`, `anti_hardcode`, `gaming`) plus the NEW
    `fitness.scoring_code_baseline_sha: null` field. No existing
    field touched. Deliver-agent fills `scoring_code_baseline_sha`
    with the M-Auto-1A close commit SHA at S-Auto-4 close.
  - `autoloop/program.md` §4 row 3 — DEFERRED → DELIVERED, single
    cell edit; other prose unchanged.
  - `autoloop/README.md` — single-line `audit` row note updated to
    mention `gaming_flags` + `anti_hardcode_flag_for_codex`.

## 2. Files added / modified

- **NEW source:**
  - `autoloop/autoloop/sandbox/content_validator.py` (187 LOC)
  - `autoloop/autoloop/scoring/gaming.py` (627 LOC)
- **NEW tests:**
  - `autoloop/tests/test_anti_hardcode_check.py` (348 LOC; 27 tests)
  - `autoloop/tests/test_content_validator.py` (251 LOC; 17 tests)
  - `autoloop/tests/test_gaming.py` (468 LOC; 16 tests)
  - `autoloop/tests/test_loop_anti_hardcode_wire.py` (340 LOC; 4
    integration tests)
- **MODIFIED source:**
  - `autoloop/autoloop/sandbox/anti_hardcode_check.py` — body swap
    (placeholder → real detector). Function signature
    `anti_hardcode_check(hypothesis, *, config) -> AntiHardcodeResult`
    preserved verbatim. `AntiHardcodeResult` dataclass redesigned per
    contract spec (`verdict`, `rule_id`, `matched_substring`,
    `placeholder`). 389 LOC (was 81).
  - `autoloop/autoloop/sandbox/__init__.py` — re-exports new validator.
  - `autoloop/autoloop/scoring/__init__.py` — re-exports gaming module.
  - `autoloop/autoloop/loop.py` — three additive insertion points +
    additive `IterationResult` fields + serializer extension.
  - `autoloop/autoloop/cli.py` — `audit` rendering extension.
  - `autoloop/config.yaml` — 3 new blocks appended + new `fitness.scoring_code_baseline_sha` field.
  - `autoloop/program.md` — §4 row 3 single-cell status flip.
  - `autoloop/README.md` — single-line `audit` row note update.
- **MODIFIED tests:**
  - `autoloop/tests/test_loop.py` — `test_anti_hardcode_placeholder_always_passes`
    rewritten as `test_anti_hardcode_real_detector_passes_clean_propose`;
    `test_adversarial_fixture_passes_placeholder_anti_hardcode`
    renamed + flipped as `test_adversarial_fixture_fails_real_detection`.

`git diff --stat HEAD -- autoloop/`:

```
 autoloop/README.md                               |   2 +-
 autoloop/autoloop/cli.py                         |  16 +
 autoloop/autoloop/loop.py                        | 151 +++++++--
 autoloop/autoloop/sandbox/__init__.py            |   3 +
 autoloop/autoloop/sandbox/anti_hardcode_check.py | 413 ++++++++++++++++++++---
 autoloop/autoloop/scoring/__init__.py            |   3 +
 autoloop/config.yaml                             |  70 ++++
 autoloop/program.md                              |   2 +-
 autoloop/tests/test_loop.py                      |  52 +--
```

Untracked (NEW): 6 files (3 source + 3 tests as listed above; this
handoff under `docs/sprints/` is the 4th untracked file pre-commit).

## 3. Detector design and rule taxonomy

The detector ships **11 rules total** organized by §4.1 kernel
question:

| rule_id | severity | covers |
|---|---|---|
| `Q1.contains_or_matches_literal` | FAIL | `.contains(...)` / `.matches(...)` / `.startswith(...)` / `.endswith(...)` literals in prose |
| `Q1.enumerated_or_keywords` | FAIL | ≥ 3 quoted-or-bare tokens joined by " or " |
| `Q1.if_then_decision_tree` | FAIL | `if … then …` or `if … →` shapes (multi-line tolerated) |
| `Q2.must_always_against_soft_dimension` | FAIL | `(runtime|bot|system) must (always|never|reject|escalate) <soft-semantic-dimension>` |
| `Q2.tier0_invariant_invention_attempt` | FAIL | `tier-0` / `invariant` / `hard gate` phrasing within ~80 chars of `add` / `new` / `introduce` / `require` |
| `Q4.case_id_literal` | FAIL | `\bcs[0-9a-z_]{2,}\b` token (CaseSpec id leakage) |
| `Q4.id_assignment_literal` | FAIL | `(session_id|case_id|iteration_id|exp_id) :=` |
| `Q5.bot_must_always` | FAIL | `(the )?(bot|assistant|llm|model) must (always|never) (answer|respond|escalate|…)` |
| `Q5.do_not_consider_soft_dimension` | FAIL | `(do not|don't|never) (consider|evaluate|reason about) <soft-semantic-dimension>` |
| `Q5.force_assistant_to` | FAIL | `force (the )?(assistant|bot|llm|model) to …` |
| `Q5.standalone_must_borderline` | FLAG_FOR_CODEX | standalone `(must|never) (always|escalate|refuse|reject|block|emit)` without a Runtime-like subject |

Rules are walked in id-sorted (alphabetical) order. The first FAIL
short-circuits and is returned. If no FAIL fires but a
`FLAG_FOR_CODEX` fires, the verdict is `FLAG_FOR_CODEX` —
observation-only at the loop level (the iteration continues through
apply / eval; the flag is attached for human / Codex review).

**Normalization pipeline** (applied to `hypothesis.after_value`
before each rule fires):

1. NFKC unicode normalize
2. Lowercase
3. Whitespace collapse (`\s+` → ` `)
4. Optional synonym map (config-driven, off by default): `equals` → `=`,
   `->` / `=>` / `==>` → `→`, `when` / `whenever` → `if`. Enables
   coverage of e.g. arrow-based decision trees without growing the
   regex set. Deliver-agent flips the toggle after calibration
   evidence shows it adds detection coverage without spiking false
   positives.

**D2 discipline (detector self-discipline regression).** The rule
source MUST encode generic structural patterns only — no specific
eval CaseSpec id literal, user utterance, expected assistant answer,
or case-status label. The regression test
`test_detector_source_does_not_hardcode_eval_case_ids` greps
`anti_hardcode_check.py` source for `cs011`, `cs015`, `cs042`,
`cs101`, `cs59s` (the last one IS allowed in `gaming.py` shadow leak
signatures — the test scopes to anti_hardcode source only).
`test_detector_source_does_not_hardcode_user_utterance_literals`
greps for `my account is locked`, `closure_criterion`,
`expected_behavior`, `source_session_id`, `primary_uc`,
`failure_tags`. `test_detector_source_rule_count_bounded` asserts
`len(_RULES) <= 30`.

## 4. Calibration table actual results

Run via `uv run pytest -q tests/test_anti_hardcode_check.py` and
also reproduced inline via a standalone calibration sweep. Results:

- 11 forbidden propose examples → 11 / 11 FAIL (100% detection):
  - The explicit §1.7 example
    `if user.message contains 'appeal' then route to UC-H` → FAIL
    (rule_id `Q1.if_then_decision_tree`).
  - `escalate if message contains 'appeal' or 'refund' or 'compensation'` → FAIL (Q1.enumerated_or_keywords).
  - `when message.contains('refund') classify as priority` → FAIL (Q1.contains_or_matches_literal).
  - `when message.matches(/refund/i) flag the case` → FAIL.
  - `the runtime must always reject any user goal that mentions refunds.` → FAIL (Q2.must_always_against_soft_dimension).
  - `add a new Tier-0 invariant that all messages require escalation.` → FAIL (Q2.tier0_invariant_invention_attempt).
  - `apply special handling for cs042 and cs101 sessions.` → FAIL (Q4.case_id_literal).
  - `set session_id = abc-123 in the trace header.` → FAIL (Q4.id_assignment_literal).
  - `force the assistant to always emit the canned reply.` → FAIL (Q5.force_assistant_to).
  - `the bot must always escalate whenever the user mentions refunds.` → FAIL (Q5.bot_must_always).
  - `do not consider the user goal — proceed directly to escalation.` → FAIL (Q5.do_not_consider_soft_dimension).
- 4 clean soft-narrative examples → 4 / 4 PASS:
  - "When the user describes a billing concern, gather the relevant intake fields before proposing next steps."
  - "Discuss the customer's case carefully before escalating."
  - "Confirm the relevant facts with the user before drafting a response."
  - "Greet the user warmly and acknowledge their concern."
- 2 borderline → 2 / 2 FLAG_FOR_CODEX:
  - "Operators must always escalate suspicious payment activity." (FLAG)
  - "Never reject a verified document without supervisor review." (FLAG)
- **FLAG_FOR_CODEX rate over the 17-case calibration set:
  2 / 17 = 11.76%** — well under the 25% warn threshold
  (`config.anti_hardcode.flag_for_codex_rate_warn_threshold`).

Additional positive coverage tests:

- Unicode NFKC: full-width `ｉｆ user.message contains 'appeal' then …`
  matched after normalization.
- Multi-line decomposition: `if … and case is already open\n then route …`
  matched (regex tolerates `\s+` across newlines).
- Synonym map (enabled): `if … => route to UC-H` matched.

Negative-control tests:

- "Discuss the customer's case carefully before escalating." PASSes —
  no false positive on the `cs` substring inside `Discuss` or
  `customer's`.
- Empty / whitespace-only `after_value` PASSes (it is a
  content_validator concern, not an anti-hardcode concern; the
  detector explicitly defers).

## 5. Gaming detector design and 7 checks

`scoring/gaming.py` ships seven `_check_*` functions invoked by the
top-level `detect()` API. All checks return `GamingFlag` records
with severity WARN or ERROR; the loop attaches them to the iteration
record and the audit subcommand renders them, but no flag alters
the keep/discard verdict (observation-only in v1).

| check | severity | trigger |
|---|---|---|
| `anomalous_metric_movement` | WARN | `improvement_count / max(1, len(diff_chars)) > threshold` (default 0.05; configurable). |
| `identical_eval_traces_across_different_hypotheses` | ERROR | byte-identical `results.json` content hash across distinct `hypothesis.fingerprint`. |
| `suspect_baseline_manipulation` | WARN | `config.fitness.baseline_dir` value differs from the value at the last `main` commit for `autoloop/config.yaml`, with no notes-blob record of the change. Sub-rule `.git_lookup_failed` emits WARN when `git log` / `git show` is unavailable (instead of guessing). |
| `eval_time_gaming_via_timeout_skip` | WARN | per-suite skipped / timed-out case rate ≥ 2× baseline rate, OR ≥ 10% absolute floor when no baseline. |
| `scoring_code_drift.sha_changed` | ERROR | SHA of `autoloop/autoloop/scoring/{tier_evaluator,eval_runner,baseline_loader,gaming}.py` differs from `config.fitness.scoring_code_baseline_sha`. |
| `scoring_code_drift.baseline_missing` | WARN | `config.fitness.scoring_code_baseline_sha` is None — D3: emit WARN, NEVER guess and silently use the current SHA as baseline. |
| `shadow_set_leakage` | ERROR | any signature in `config.gaming.shadow_leak_signatures` appears in a non-audit surface (iteration_record JSON, proposer_prompt, recent_records). |
| `tier2_measurement_contract_change_attempt` | ERROR | a critical_step whose baseline outcome was FAIL now scores N/A / skipped — indicates the measurement contract may have changed under optimization. |

**D3 invariants (`gaming.py` MUST NOT violate):**

1. `gaming.py` never opens any file under
   `eval_interactive/case_specs_shadow/`. The `shadow_set_leakage`
   check is config-driven only via `config.gaming.shadow_leak_signatures`.
   Verified by inspection AND by
   `test_shadow_set_leakage_d3_no_default_signatures` /
   `test_shadow_set_leakage_empty_list_in_config_does_not_trigger`.
2. `scoring_code_drift` emits `baseline_missing` WARN when the
   configured baseline SHA is None. Verified by
   `test_scoring_code_drift_baseline_missing_emits_warn` /
   `test_scoring_code_drift_baseline_unset_treated_as_missing`.

**Per-check positive/negative coverage:**

- `anomalous_metric_movement`: trigger (5-case improvement on
  0-char delta hypothesis) + clean (1-case improvement on 50-char
  delta hypothesis).
- `identical_eval_traces`: trigger (synthetic prior iteration with
  matching results hash + distinct fingerprint) + clean (distinct
  hash).
- `suspect_baseline_manipulation`: trigger (mocked git failure) +
  clean (no baseline_dir configured at all).
- `eval_time_gaming_via_timeout_skip`: trigger (50% skip rate) +
  clean (0% skip rate).
- `scoring_code_drift`: baseline_missing + diverged_sha (forced
  zero-SHA) + clean (current SHA configured).
- `shadow_set_leakage`: trigger (signature configured + matched in
  record) + clean (empty signature list) + D3 (no signatures key
  at all).
- `tier2_measurement_contract_change_attempt`: trigger (baseline
  FAIL → current N/A) + clean (baseline FAIL → current FAIL).

## 6. Wiring into loop.py

Three additive insertion points; no orchestrator signature change.

**Insertion A — pre-sandbox content validator (step 2.5):**

```
2.   proposer.propose(...)                        → Hypothesis
2.5  content_validator.validate_content(hyp)      → ContentValidationResult
     FAIL → discard + persist (discard_reason=
            content_validator_rejected:<rule_id>); sandbox NOT invoked.
3.   sandbox.validate_skill_yaml_diff(...)        → ValidationResult
```

**Insertion B — FLAG_FOR_CODEX flag-attach at step 4:** the
`anti_hardcode_check` call site was already in place from S-Auto-3.
The body swap returns `verdict ∈ {PASS, FAIL, FLAG_FOR_CODEX}`. FAIL
discards with `discard_reason=anti_hardcode_rejected:<rule_id>` (the
canonical S-Auto-4 field; loop.py previously read `.reason`, now
reads `.rule_id`). FLAG_FOR_CODEX sets
`iteration_record.anti_hardcode_flag_for_codex = True` and the
iteration CONTINUES through apply / eval.

**Insertion C — post-eval gaming.detect (step 9.5):**

```
9.   tier_evaluator.evaluate(...)                 → LexicographicVerdict
9.5  gaming.detect(iter_record, recent_records,
                   eval_artefacts, config=config) → list[GamingFlag]
     iter_record.gaming_flags = flags             # never alters decision
10.  experiments_log.append(iter_record)
```

The gaming detector is wrapped in a `try/except` so its own failures
cannot block iteration persistence (observation-only stance).

**`IterationResult` extension:**

```python
@dataclass
class IterationResult:
    # ... S-Auto-3 fields unchanged ...
    content_validator_verdict: ContentValidationResult | None = None
    gaming_flags: list[dict[str, Any]] = field(default_factory=list)
    anti_hardcode_flag_for_codex: bool = False
```

All three are additive optional fields with defaults — backward
compatibility for S-Auto-3 callers that construct
`IterationResult(iteration_id=...)` without these fields, verified by
`test_iteration_result_constructible_without_new_fields`.

## 7. §7 — Layer-classification + anti-hardcode stanza (self-walk)

**Target failure layer:** `eval_spec` (§3.2 Q6 boundary — the kernel
IS a structural §1.7 defense at propose-stage; it does NOT modify
runtime / projection / scoring / CaseSpec; it is the last link in
the §1.7 enforcement chain after S-Auto-1 sandbox white-list +
S-Auto-2 lexicographic gate + shadow firewall + S-Auto-3 loop /
memory / applier).

**Tier-0 invariant:** This sub-sprint adds no Tier-0 invariant. The
kernel is meta-loop infrastructure, NOT a Runtime invariant per
`docs/runtime_freeze_and_risk_policy.md` §1 / §2. The kernel DETECTS
attempts to invent new Tier-0 semantics (Q2 rule
`Q2.tier0_invariant_invention_attempt`) — exactly the opposite of
adding one.

**Semantic hardcode:** No semantic hardcode introduced. Justification
by surface:

- `anti_hardcode_check.py` rules are **generic structural pattern
  matches** (per D2): IF/THEN shapes, `cs<id>`-like tokens,
  MUST/NEVER phrasing structures, `.contains(...)` literals. Rules
  do NOT enumerate specific eval phrases / user utterances /
  expected answers / case-status labels. Detector self-discipline
  regression test grep-asserts this.
- `content_validator.py` is purely structural integrity —
  placeholder tokens already in `before_value`, length bounds,
  configurable deny-list. NO Salesforce-specific or domain-specific
  semantic logic (D3: only 3 generic placeholder shapes; custom
  shapes config-driven).
- `gaming.py` checks are numeric metric comparisons + structural
  artefact properties + config-driven leak-signature scans. NO
  shadow case content read. NO baseline guessing. The NEW
  `tier2_measurement_contract_change_attempt` check is a structural
  impossibility alarm; if it fires it means the sandbox or the
  eval-time-skip check missed a gap — it is a meta-loop integrity
  signal, not a semantic decision.
- `loop.py` wiring extensions are dataclass-additive and call-site
  additive only; no signature change to orchestrator or
  S-Auto-1/2/3 deliverables.

**Generalization coverage:**
- **target** = §4.1 kernel auto-tractable subset (Q1 + Q2 + Q4 + Q5)
  auto-reject all 11 forbidden calibration examples; accept all 4
  clean calibration examples; S-Auto-3 adversarial fixture flips
  PASS → FAIL.
- **neighbor** = unicode NFKC normalization (full-width `if` →
  matched) + multi-line decomposition (`if … \n … then …` →
  matched) + small synonym-map equivalents (`=>` → `→` → matched).
- **negative-control** = each allowed Skill YAML field type gets
  one clean soft-narrative edit case in the test fixture set (no
  false positive on plain prose); `Discuss` / `customer's` /
  `discussion` do not false-trigger Q4.case_id_literal.
- **adversarial** = the S-Auto-3 adversarial fixture (now flipped)
  is the load-bearing regression target; Codex per-sub-sprint
  authors ≥ 3 additional hand-crafted adversarial proposes
  (unicode obfuscation, multi-line decomposition, synonym swap) at
  S-Auto-4 close.
- **shadow firewall coverage** = `shadow_set_leakage` detector
  unit test uses a synthetic iteration_record containing `cs59s`
  AND an empty `shadow_leak_signatures` config → no false
  positive; detector is config-driven (D3) and never reads the
  shadow corpus.

## 8. Test deltas

Sprint 56 baseline: **147 passed.** S-Auto-4 close: **216 passed,
0 failed.** Delta: **+ 69 NEW tests** (well within the contract's
50-80 target).

Breakdown of new tests by file:

- `tests/test_anti_hardcode_check.py` — 27 new tests:
  - 4 in Q1 (one per rule + one clean control).
  - 3 in Q2 (one per rule + one clean control).
  - 4 in Q4 (one per rule + a mixed one + clean control on `cs`-
    inside-word).
  - 5 in Q5 (one per rule + 2 FLAG_FOR_CODEX borderlines).
  - 3 normalization regressions (NFKC, multi-line, synonym map).
  - 2 result-shape contract checks (`placeholder is False`;
    `matched_substring` populated + bounded to 200 chars).
  - 2 empty / whitespace input safety.
  - 4 detector self-discipline regressions (no eval case_id
    literals; no user-utterance literals; rule count ≤ 30).
- `tests/test_content_validator.py` — 17 new tests covering all 5
  rules + config tunables + D3 regressions (no Salesforce-specific
  default + custom shapes opt-in).
- `tests/test_gaming.py` — 16 new tests covering all 7 checks
  positive + negative + D3 regressions (baseline_missing,
  config-driven shadow signatures).
- `tests/test_loop_anti_hardcode_wire.py` — 4 new integration
  tests (content_validator FAIL discards before sandbox;
  FLAG_FOR_CODEX continues; gaming flags persisted + visible in
  audit; IterationResult backward compat).
- `tests/test_loop.py` — 1 net delta (the two updated tests
  collectively): old placeholder-pass + adversarial-pass replaced
  with the real-detector-passes-clean + adversarial-rejects pair.

eval_interactive: `486 passed, 3 failed` — UNCHANGED from baseline
(the 3 pre-existing failures are in
`tests/regression/test_case_spec_overrides.py::test_v2_schema_loads_cleanly`,
`tests/regression/test_case_spec_overrides.py::test_smoke_review_report_tracks_smoke_set_and_overrides`,
`tests/regression/test_corpus_lint.py::test_full_corpus_lints_clean_with_smoke_subset_flag`;
all pre-S-Auto-4 baseline).

Java baseline: UNCHANGED — no edits to `server/src/main/java/**` or
`eval/src/main/java/**` (verified by `git diff --stat HEAD --
server/ eval/src/main/java/` returning empty).

## 9. Hard-fence + scope discipline

`git diff --stat HEAD -- server/ eval/src/main/java/ eval_interactive/eval_interactive/ eval_interactive/case_specs/ eval_interactive/case_specs_shadow/ data/ db/ server/src/main/resources/ docs/foundational/ docs/runtime_freeze_and_risk_policy.md docs/current/ docs/sprints/sprint-0 docs/milestones/ docs/codex-findings.md`
→ empty (no edits under any hard-fenced surface).

Commit-at-end discipline: dev stages only the S-Auto-4 scope
explicitly (no `git add -A`). Deliver-agent close-bundle artefacts
(`docs/sprint_objective.md` archive rename, `docs/milestone_objective.md`,
`docs/10-handoff.md`, `docs/action_bank.md`,
`compact/sprint-057-codex-review-prompt.md`,
`docs/codex-findings.md` Sprint-57 entry) bundled by the human at
close.

## 10. Backward-compat surface preservation

S-Auto-1 / S-Auto-2 / S-Auto-3 deliverable signatures preserved
verbatim:

- `validate_skill_yaml_diff(before_yaml, after_yaml, file_path, *, allowed_skill_files, allowed_field_paths)` — UNCHANGED.
- `evaluate(current_results, baseline, *, config, ...)` — UNCHANGED.
- `BaselineSnapshot` / `SuiteSnapshot` / `LexicographicVerdict` /
  `LayerResult` — UNCHANGED.
- `run_v1_fitness_suite(*, results_root, config)` — UNCHANGED.
- `run_one_iteration(*, config, iteration_id, dry_run, client, repo_root)` — UNCHANGED.
- `run_iterations(*, config, count, dry_run, client, repo_root, start_index)` — UNCHANGED.
- `applier.apply(...)` / `applier.cleanup(...)` — UNCHANGED.
- `analyzer.analyze(...)` / `proposer.propose(...)` /
  `lessons_compactor.compact(...)` — UNCHANGED.
- `anti_hardcode_check(hypothesis, *, config) -> AntiHardcodeResult` —
  function signature UNCHANGED; only the body swapped + the dataclass
  internal shape redesigned per the contract API
  (`verdict` / `rule_id` / `matched_substring` / `placeholder`). The
  `placeholder` field is preserved per the contract; S-Auto-4 always
  sets it to False.

`IterationResult` extended additively (three optional fields with
default values); S-Auto-3 callers that construct
`IterationResult(iteration_id=...)` without specifying the new
fields still work — verified by
`test_iteration_result_constructible_without_new_fields`.

## 11. Open questions for deliver-agent (OQ-S57.x)

- **OQ-S57.1 — FLAG_FOR_CODEX calibration rate on the test set.**
  Measured 2 / 17 = 11.76% on the in-handoff calibration set
  (§ "Calibration table actual results"). Well under the 25% warn
  threshold. Recommend keeping
  `config.anti_hardcode.flag_for_codex_rate_warn_threshold: 0.25`
  unchanged in v1; deliver-agent re-measures during M-Auto-1B
  overnight runs against real meta-agent outputs and revises if the
  rate creeps up.
- **OQ-S57.2 — `config.anti_hardcode.synonym_map_enabled` default.**
  Ships `false` in v1. The map covers `equals` → `=`, `->` /
  `=>` → `→`, `when` / `whenever` → `if`. Enabling it picks up
  arrow-based decision trees and "when" phrasings without growing
  the regex set. Deliver-agent's recommendation: keep disabled
  until M-Auto-1B observes a real meta-agent propose that the
  detector misses BECAUSE the synonym is disabled. If 0 such
  misses after the first overnight batch, leave disabled. If ≥ 1,
  flip to true at the next config edit.
- **OQ-S57.3 — `config.fitness.scoring_code_baseline_sha` value.**
  Ships as `null` in v1; deliver-agent fills with the M-Auto-1A
  close commit SHA at S-Auto-4 close. Until filled,
  `gaming.scoring_code_drift.baseline_missing` emits a WARN on
  every iteration (intentional; documented behaviour per D3).
- **OQ-S57.4 — `config.content_validator.deny_list` seed.** Ships
  with `<<SYSTEM>>` and `<<USER>>` only (deliberately small per
  the contract). Deliver-agent + human curate at S-Auto-4 close.
  Adding entries is a separate sprint decision; no recommendation
  from dev at this time.
- **OQ-S57.5 — `test_meta_agent.py` placeholder assertion update.**
  The contract calls for updating S-Auto-3 `test_meta_agent.py`
  `placeholder=True` assertions to `placeholder=False`. A grep of
  `tests/test_meta_agent.py` for `placeholder` / `anti_hardcode` /
  `AntiHardcode` returns 0 matches — no such assertion exists in
  that file. The only `placeholder=True` assertions in the autoloop
  test tree were both in `tests/test_loop.py` (lines 222, 498),
  both of which S-Auto-4 has updated. No-op on test_meta_agent.py;
  noting here so the deliver-agent does not look for a missing
  edit.
- **OQ-S57.6 — Codex bypass spot-checks.** The contract names ≥ 3
  Codex-authored adversarial proposes (unicode obfuscation,
  multi-line decomposition, synonym swap) at S-Auto-4 close. Dev
  has unit tests for each shape (`test_unicode_nfkc_full_width_if_detected`,
  `test_multi_line_decomposition_if_then_detected`,
  `test_synonym_map_arrow_when_enabled`) — these are NOT a
  substitute for Codex-authored adversarials; they are smoke tests
  that the detector handles the obvious obfuscations. Codex
  authors the harder adversarials at close.

## 12. Reserved for deliver-agent + human (sub-sprint close)

[Deliver-agent + human fill at S-Auto-4 close: per-sub-sprint Codex
review verdict (per §4.3 trigger #2 — the kernel IS a structural §1.7
defense); SHA backfill for `config.fitness.scoring_code_baseline_sha`;
deny-list seed curation outcome; FLAG_FOR_CODEX rate re-measurement
on any additional adversarial proposes Codex authors; M-Auto-1A
milestone-close planning per §8.]
