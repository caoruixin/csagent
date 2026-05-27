---
title: Sprint objective — Sprint 57 / M-Auto-1A S-Auto-4 — Anti-hardcode kernel + content validator + gaming checks
doc_tier: current-runtime
status: current
implementation_status: not_started
source_of_truth: this file
last_reviewed: 2026-05-27
review_cadence: per sub-sprint
supersedes: [docs/sprints/sprint-056-objective.md]
superseded_by: null
notes: >
  FOURTH and FINAL sub-sprint of Milestone M-Auto-1A — Auto-Evolution
  Build. S-Auto-4 swaps the S-Auto-3 placeholder `anti_hardcode_check`
  hook for a real deterministic structural detector (regex + heuristic;
  NO second LLM in the propose-stage critical path); ships
  `content_validator.py` (placeholder/length/deny-list structural
  validity); ships `scoring/gaming.py` (6 anti-gaming checks ported
  from `docs/proposals/autoloop_design.md` §8 + the NEW
  `tier2_measurement_contract_change_attempt`); wires all three into
  `loop.py` at the correct stage boundaries (no signature change to
  the orchestrator). §7 REQUIRED (`eval_spec`); **Codex PER-SUB-SPRINT
  REQUIRED per §4.3 trigger #2** (the kernel IS the §1.7 structural
  guard; Codex must independently verify the detector has no design
  hole BEFORE M-Auto-1B begins running it against real meta-agent
  outputs). Codex must return `pass` (or `approve with
  downgrade-to-signal follow-up`) BEFORE M-Auto-1A milestone close.
  Builds on Sprint 56 / S-Auto-3 (pending bundle commit on top of
  `d9e4086`). Three human-locked design clauses 2026-05-27 (folded in
  below): D1 detector mechanism = regex+heuristic only (no second
  LLM); D2 detector pattern scope = generic structural ONLY (no
  hardcoding specific eval case wording / user utterances / expected
  answers / success-failure labels); D3 gaming.py input contracts =
  baseline-SHA missing emits WARN not guess; shadow leakage detector
  must NOT read shadow case content directly.
---

# Sprint 57 / M-Auto-1A S-Auto-4 — Anti-hardcode kernel + content validator + gaming checks

## Class

`eval_spec` (§3.2 Q6 boundary — the kernel is structural §1.7 defence at the meta-agent output boundary; it does not change runtime / projection / scoring / CaseSpec; it is the propose-stage gate that prevents §1.7 violations from reaching build / eval). **§7 REQUIRED** — S-Auto-4 closes the §1.7 enforcement chain whose plumbing S-Auto-1/2/3 built: sandbox white-list (S-Auto-1) + 4-tier lexicographic fitness with shadow firewall (S-Auto-2) + loop / meta-agent / 3-layer memory / applier (S-Auto-3) + **structural propose-stage detector + content validator + post-eval gaming checks (S-Auto-4)**.

## Goal

S-Auto-4 close 时：

1. **Real anti-hardcode detector replaces the S-Auto-3 placeholder.** The S-Auto-3 hook signature `def anti_hardcode_check(hypothesis, *, config) -> AntiHardcodeResult` is **verbatim preserved** — S-Auto-4 swaps the body only. The S-Auto-3 adversarial fixture in `autoloop/tests/test_loop.py` (`test_adversarial_fixture_passes_placeholder_anti_hardcode` — currently passes with `if user.message contains 'appeal' then route to UC-H else UC-A`) is **renamed + flipped** to `test_adversarial_fixture_fails_real_detection` with the assertion inverted (PASS → FAIL); this is the regression target across the placeholder → real transition.

2. **Content validator at propose-stage pre-sandbox.** A new `autoloop/autoloop/sandbox/content_validator.py` runs structural validity checks on `hypothesis.after_value`: broken placeholder syntax (only for tokens already present in `hypothesis.before_value`), zero-length, >5× growth, configurable forbidden-token deny-list. `FAIL` → iteration discards with `discard_reason=content_validator_rejected:<rule_id>` BEFORE sandbox YAML diff validation.

3. **Anti-gaming checks at post-eval.** A new `autoloop/autoloop/scoring/gaming.py` runs the 6 checks ported from `docs/proposals/autoloop_design.md` §8 + the NEW `tier2_measurement_contract_change_attempt`. **All checks are OBSERVATION-ONLY in v1** — they produce `GamingFlag` records (`WARN` or `ERROR` severity) that are persisted to `experiments_log.gaming_flags` and surfaced by `autoloop audit --experiment exp-N`, but **do NOT change the keep/discard verdict from `tier_evaluator`**. ERROR-severity flag gating is reserved for M-Auto-1B post-overnight evidence calibration.

4. **`autoloop/program.md` §4 row 3** flips from `S-Auto-4 — DEFERRED` to `S-Auto-4 — DELIVERED (deterministic regex+heuristic detector covering Q1/Q2/Q4/Q5 of the §4.1 nine-question kernel; FLAG_FOR_CODEX verdict for borderline cases)`. No other prose changes.

5. **Per-sub-sprint Codex `pass` (§4.3 trigger #2).** Deliver-agent dispatches Codex at S-Auto-4 close (NOT at S-Auto-4 open) with `compact/sprint-057-codex-review-prompt.md` covering the actual commit range. Codex must verify the detector has no design hole BEFORE M-Auto-1B can run it.

**Zero touch** to S-Auto-1/2/3 deliverable signatures (`anti_hardcode_check` body swaps but signature is verbatim; `loop.py` signature unchanged — wiring extensions only at the propose-stage pre-sandbox boundary and the post-eval gaming-checks boundary; `cli.py audit` extends its surface to render `gaming_flags` + `anti_hardcode_flag_for_codex` but signature unchanged; `eval_runner.py` / `tier_evaluator.py` / `baseline_loader.py` UNTOUCHED). **Zero touch** to `eval_interactive/eval_interactive/**`, case_spec, case_specs_shadow, `server/`, `eval/` Java, `data/`, `db/`, `docs/foundational/`, `docs/runtime_freeze_and_risk_policy.md`, `docs/current/`, sprint/milestone archives, `docs/codex-findings.md`.

## Scope (numbered; this is the contract)

### #1 — `autoloop/autoloop/sandbox/anti_hardcode_check.py` placeholder → real detector

Swap the S-Auto-3 placeholder body for a deterministic **regex + heuristic** structural detector (NO LLM call; NO new heavy dep; stdlib `re` + `unicodedata` + small synonym map only). Covers the auto-tractable subset of the §4.1 nine-question kernel:

**Q1 — Semantic hardcode patterns** (FAIL on high-confidence; FLAG_FOR_CODEX on borderline):
- `IF/WHEN ... THEN/ELSE` decision trees in soft narrative (recognise via normalized-case + punctuation + arrow tokens `→` / `->`).
- Enumerated keyword lists ≥3 items in OR-form (e.g., `"appeal" OR "refund" OR "compensation"`).
- Regex-style pattern enumeration in prose (`.contains(...)`, `.matches(...)`).
- Multi-line decomposition of the above (kernel must handle when an IF-THEN is split across newlines / bullet points).

**Q2 — Tier-0 invariant invention attempts** (FAIL on high-confidence):
- Language asserting new MUST-NEVER / Tier-0-equivalent semantics absent a current Tier-0 invariant (e.g., "MUST always escalate when X", "Runtime MUST reject any message containing Y").
- The detector recognises Tier-0-like phrasing structurally; it does NOT consult `docs/runtime_freeze_and_risk_policy.md` (no cross-doc dependency in propose-stage).

**Q4 — Eval-phrase / case_id / session_id references** (FAIL on high-confidence):
- Explicit `cs<NNN>` / `cs[a-z0-9]+` substring tokens.
- Explicit `session_id=...` / `case_id=...` / `iteration_id=...` substring tokens.
- Reserved: near-verbatim quoting of known eval CaseSpec phrasings — **NOT in v1 scope per D2 below** (would require hardcoding specific eval wording, which is exactly the forbidden surface).

**Q5 — LLM-ownership-shrinking language** (FAIL on high-confidence; FLAG_FOR_CODEX on borderline):
- "MUST execute X", "force the assistant to", "the bot MUST always answer with", "do not consider Y" (where Y is a soft-semantic dimension §1.3 says the LLM owns).
- A single "MUST" with a non-Tier-0 subject → FLAG_FOR_CODEX (not auto-FAIL — Codex / human judges).

#### Detector pattern scope (D2 — human-locked 2026-05-27):

**Allowed** (the detector MAY encode these as generic structural patterns):
- `IF/WHEN/THEN/ELSE` decision-tree shapes (case-insensitive; normalized);
- `cs<id>`-like tokens (regex `\bcs[0-9a-z_]{2,}\b`);
- `session_id`-like / `case_id`-like / `iteration_id`-like tokens;
- `MUST`/`NEVER`/`Tier-0`-like phrasing structures;
- `.contains(...)` / `.matches(...)` / explicit regex literal shapes.

**Forbidden** (the detector MUST NOT hardcode any of these):
- specific eval case wording (e.g., the literal phrasing of a `cs011` user turn);
- specific user utterances (e.g., "my account is locked" as a banned phrase);
- specific expected assistant answers;
- known case success/failure labels (e.g., `closure_criterion` text fragments).

The detector measures **generic structural shape**, not specific content. If a rule requires a specific eval phrase to fire, the rule is structurally wrong and must be redesigned.

API (signature verbatim from S-Auto-3):

```python
from dataclasses import dataclass
from typing import Literal

@dataclass
class AntiHardcodeResult:
    verdict: Literal["PASS", "FAIL", "FLAG_FOR_CODEX"]
    rule_id: str | None              # e.g. "Q1.if_then_decision_tree"
    matched_substring: str | None    # offending excerpt for audit (≤200 chars; redacted if user-content-like)
    placeholder: bool = False        # S-Auto-4: always False; S-Auto-3 placeholder set True

def anti_hardcode_check(hypothesis, *, config) -> AntiHardcodeResult: ...
```

Normalization pipeline (applied to `hypothesis.after_value` before each rule fires):
1. NFKC unicode normalize.
2. Lowercase.
3. Whitespace collapse (`\s+` → ` `).
4. Optional small synonym map for high-frequency obfuscation (e.g., `equal` → `=`, `equals` → `=`; configurable, default off).

Each rule returns its own verdict; the final verdict is the highest-severity result across all rules (FAIL > FLAG_FOR_CODEX > PASS). When `FAIL`, `rule_id` is the first rule that fired (deterministic; rule evaluation order is fixed by `rule_id` ascending).

**Detector self-discipline (the defender must not become a hardcode pump)**: the rule set must encode generic structural patterns ONLY (per D2 above). If Codex finds a rule whose match depends on a specific eval phrase / user utterance / expected answer / case-status label, the rule is REJECTED.

### #2 — `autoloop/autoloop/sandbox/content_validator.py` (NEW)

Lightweight structural validity checker run **before** sandbox YAML diff validation (so a content-broken hypothesis discards even cheaper than a sandbox-rejected one).

**Placeholder syntax integrity (D3 — human-locked 2026-05-27)**: detect corruption of placeholder-like tokens that are **already present in `before_value`**. Tokens covered:
- `{TOKEN}` and `{TOKEN_NAME}` (curly-brace template tokens; common in prompt templates);
- `${TOKEN}` and `${TOKEN_NAME}` (dollar-prefix template tokens);
- `<TOKEN>` / `<TOKEN_NAME>` (angle-bracket internal markers) IF already present in `before_value` (e.g., a Skill YAML field uses these as internal markers).

For each placeholder shape detected in `before_value`, the validator checks that the same token appears intact in `after_value` (no missing close-brace, no rename, no accidental deletion). If `before_value` has no such tokens, the check is skipped (no false positives on edits to plain prose).

**Do NOT** introduce Salesforce-specific semantic validation, currency validation, locale validation, etc. unless concrete token shapes are provided in `autoloop/config.yaml` `content_validator.custom_token_shapes: [...]`. v1 ships with the three generic shapes above only.

**Length sanity**:
- `len(after_value.strip()) == 0` → FAIL (`rule_id=content_validator.zero_length`).
- `len(after_value) > 5 * len(before_value)` → FAIL (`rule_id=content_validator.length_overflow`) — catches meta-agent rambling expansions.
- `len(after_value) < 0.1 * len(before_value)` AND `len(before_value) > 50` → FAIL (`rule_id=content_validator.length_underflow`) — catches near-deletions of substantial fields. The `0.1` ratio + `50` floor are config-tunable (`content_validator.length_underflow_ratio` / `length_underflow_min_before`); v1 defaults `0.1` + `50`.

**Forbidden-token deny-list**: small seeded list from §1.7 forbidden examples (e.g., explicit `case_id`, `session_id`, known prompt internal markers like `<<SYSTEM>>` if any). Configurable via `autoloop/config.yaml` `content_validator.deny_list: [...]`. v1 ships with a deliberately-small seed (≤10 tokens); expansion is a separate sprint decision.

API:

```python
@dataclass
class ContentValidationResult:
    verdict: Literal["PASS", "FAIL"]
    rule_id: str | None
    detail: str | None

def validate_content(hypothesis) -> ContentValidationResult: ...
```

`FAIL` → iteration discards with `discard_reason=content_validator_rejected:<rule_id>` BEFORE sandbox YAML diff validate.

### #3 — `autoloop/autoloop/scoring/gaming.py` (NEW)

Post-eval anti-gaming checks. **All checks OBSERVATION-ONLY in v1** — produce `GamingFlag` records, persist to `experiments_log.gaming_flags`, surface in `audit`, but DO NOT auto-discard the iteration in v1 (gating reserved for M-Auto-1B post-overnight calibration).

Port the 6 anti-gaming checks from `docs/proposals/autoloop_design.md` §8 + add the NEW `tier2_measurement_contract_change_attempt`:

1. **`anomalous_metric_movement`**: `LexicographicVerdict` Layer 3 improvement disproportionate to the size/scope of the diff. Heuristic: `improvement_count / max(1, len(diff_chars))` > threshold (default 0.05; configurable). WARN severity.

2. **`identical_eval_traces_across_different_hypotheses`**: two iterations with different `hypothesis_fingerprint` produce byte-identical `results.json` across all 3 v1 suites. ERROR severity (suggests eval not actually rerun). Detector reads `iterations_index.query_recent(n=K)` + hashes per-suite results files; flags when ≥2 distinct fingerprints share a results hash.

3. **`suspect_baseline_manipulation`**: current `config.fitness.baseline_dir` value differs from the value that was in `config.yaml` at the last `main`-branch commit (or at iteration 1 of the current overnight batch), and the change is NOT recorded as a human action in `iterations_index.notes`. WARN severity. Detector reads current config + walks `git log --follow autoloop/config.yaml` for the last `main` SHA + extracts the historical value via `git show <sha>:autoloop/config.yaml`. If `git` lookup fails for any reason, emit `WARN rule_id=suspect_baseline_manipulation.git_lookup_failed` (do not auto-pass).

4. **`eval_time_gaming_via_timeout_skip`**: rate of skipped / timed-out cases in current eval suite >2× baseline rate. WARN severity. Detector reads `results.json` per-case `terminal_outcome` field (skipped / timeout / error counts).

5. **`scoring_code_drift`** (D3 — human-locked 2026-05-27): the SHA of `autoloop/autoloop/scoring/{tier_evaluator,eval_runner,baseline_loader,gaming}.py` differs from the baseline-recorded SHA in `config.fitness.scoring_code_baseline_sha` (NEW config field; deliver-agent fills at S-Auto-4 close with the M-Auto-1A close commit SHA).
   - If `config.fitness.scoring_code_baseline_sha` is **missing or None**: emit WARN with `rule_id=scoring_code_drift.baseline_missing` and `detail="scoring_code_baseline_sha not set in config; cannot verify drift"`. **DO NOT guess**, DO NOT auto-pass, DO NOT compute a hash and silently use it as baseline.
   - If the baseline SHA is set and the current SHA differs: emit ERROR with `rule_id=scoring_code_drift.sha_changed`.

6. **`shadow_set_leakage`** (D3 — human-locked 2026-05-27): detect shadow per-case info appearing in any non-audit surface (proposer prompt context, `experiments_log` non-firewall fields, `iterations_index` rows). Implementation:
   - The detector scans the iteration_record + recent K iterations + the proposer prompt (if logged) for **redacted leak-signature patterns** seeded from `config.gaming.shadow_leak_signatures: [...]`.
   - v1 leak signatures: `["cs59s", "shadow_case_id", "shadow/", "case_specs_shadow"]` (configurable; sourced from human-approved config).
   - **The detector MUST NOT read `eval_interactive/case_specs_shadow/` content directly** — it does not load any shadow case_spec file, does not read case IDs from the shadow directory listing, does not depend on the shadow corpus content. If a human wishes to add a leak signature, they add it to the config (out-of-band; explicit approval).
   - ERROR severity on match.

7. **NEW `tier2_measurement_contract_change_attempt`** (belt-and-suspenders for S-Auto-1 sandbox): detect the structurally-impossible case where the diff was sandbox-ACCEPTED and `results.json` shows a previously-`FAIL` `critical_step` now scoring `N/A` (i.e., the measurement contract changed under the optimization, which would invalidate the lexicographic gate). Detector reads current `results.json` `tier2_result.per_step[]` and baseline `tier2_result.per_step[]` and flags any step whose `outcome` field changed from `"FAIL"` to `"N/A"` or `"skipped"`. ERROR severity. If ever triggered, indicates either an S-Auto-1 sandbox bypass OR an eval-time gaming issue not covered by check 4.

API:

```python
from typing import Literal

@dataclass
class GamingFlag:
    rule_id: str
    severity: Literal["WARN", "ERROR"]
    detail: str
    evidence: dict  # rule-specific evidence; never contains raw shadow case content

def detect(iteration_record, recent_records, eval_artefacts, *, config) -> list[GamingFlag]: ...
```

The returned `list[GamingFlag]` is appended to the iteration record under `iteration_record.gaming_flags = [...]` BEFORE `experiments_log.append(...)`. Empty list = no flags raised.

### #4 — Wire into `autoloop/autoloop/loop.py` (extension only; no signature change)

Three insertion points in the existing state machine:

**Insertion A — pre-sandbox content validator** (between current steps 2 and 3):
```
2. proposer.propose(...) → Hypothesis
2.5 content_validator.validate_content(hypothesis) → ContentValidationResult; FAIL → discard + log + return
3. sandbox.validate_skill_yaml_diff(...) → ValidationResult
```

**Insertion B — anti-hardcode at propose-stage** (existing step 4, S-Auto-3 hook): no wiring change; the call is already in place. S-Auto-4 only swaps the body of `anti_hardcode_check`. NEW: when verdict is `FLAG_FOR_CODEX`, the loop attaches `iteration_record.anti_hardcode_flag_for_codex = True` (does NOT discard); when `FAIL`, the loop discards with `discard_reason=anti_hardcode_rejected:<rule_id>` (existing behaviour from S-Auto-3 hook contract).

**Insertion C — post-eval gaming detect** (between current steps 9 and 10):
```
9.  tier_evaluator.evaluate(...) → LexicographicVerdict
9.5 gaming.detect(iter_record, recent_records, eval_artefacts, config=config) → list[GamingFlag]
    iter_record.gaming_flags = flags
10. memory.experiments_log.append(iter_record)
```

No `IterationResult` dataclass change required if `gaming_flags` is added to the iteration record dict that `experiments_log.append` consumes. If the dataclass needs a NEW field, it is an additive optional field (`gaming_flags: list[dict] = field(default_factory=list)`) — backward-compatible with S-Auto-3 tests.

### #5 — `autoloop/autoloop/cli.py audit --experiment exp-N` extension (no signature change)

Extend the audit output to surface:
- `anti_hardcode_flag_for_codex: True/False`;
- `gaming_flags: [list of {rule_id, severity, detail}]` — render as a small table per iteration.

Default `audit` mode continues to RESPECT the shadow firewall (only aggregate Layer 4 metrics from persisted records). `--include-shadow-detail` flag continues to be the only path to per-case shadow info.

### #6 — `autoloop/config.yaml` extension (append; no existing field touched)

Append:

```yaml
content_validator:
  custom_token_shapes: []                 # additional placeholder token shapes (e.g., regex patterns); default empty
  deny_list:                              # forbidden-token deny-list
    - "<<SYSTEM>>"                        # example seed; deliver-agent + human curate at S-Auto-4 close
    - "<<USER>>"                          # example seed
  length_overflow_ratio: 5.0              # >ratio × before_value length → FAIL
  length_underflow_ratio: 0.1             # <ratio × before_value length → FAIL (when before is substantial)
  length_underflow_min_before: 50         # only enforce underflow when before_value is at least this long

anti_hardcode:
  enabled: true
  synonym_map_enabled: false              # off by default; deliver-agent enables after calibration evidence
  flag_for_codex_rate_warn_threshold: 0.25  # if >25% of test fixtures hit FLAG, kernel is too noisy

gaming:
  enabled: true
  observation_only_in_v1: true            # documents the v1 policy; M-Auto-1B may flip
  shadow_leak_signatures:                 # CONFIG-driven only; detector must NOT read case_specs_shadow content directly
    - "cs59s"
    - "shadow_case_id"
    - "shadow/"
    - "case_specs_shadow"
  anomalous_metric_movement_threshold: 0.05   # improvement-count per char-of-diff

fitness:
  scoring_code_baseline_sha: null         # deliver-agent fills at S-Auto-4 close with the M-Auto-1A close commit SHA;
                                          # missing → gaming.scoring_code_drift emits WARN baseline_missing (NOT guess)
```

The S-Auto-1/2/3 fields are untouched.

### #7 — `autoloop/program.md` §4 row 3 status flip

Single-cell edit:

- **Row 3 (Anti-hardcode auto-check)**: `S-Auto-4 — DEFERRED ...` → `S-Auto-4 — DELIVERED (deterministic regex+heuristic detector covering Q1/Q2/Q4/Q5 of the §4.1 nine-question kernel; PASS / FAIL / FLAG_FOR_CODEX verdicts; FLAG_FOR_CODEX is observation-only in v1, never auto-promoted to FAIL by the detector itself)`.

No other prose change.

### #8 — Tests (~50-80 NEW; expected total 147 → 197-227)

`autoloop/tests/test_anti_hardcode_check.py` (NEW):
- ≥8 forbidden positives across Q1/Q2/Q4/Q5 (≥2 per category) each auto-rejected with correct `rule_id`;
- ≥4 clean soft-narrative positives (one per allowed Skill YAML field type) each PASS;
- Unicode NFKC normalization (e.g., `'ｉf'` full-width → matched);
- Multi-line decomposition of an `if-then` (across `\n` boundaries) still detected;
- `FLAG_FOR_CODEX` verdict on at least 2 borderline cases (single "MUST" with non-Tier-0 subject; standalone "force the assistant to" without verb specificity);
- **Detector self-discipline regression**: a fixture test that asserts the kernel rule definitions do NOT contain any specific eval phrase / user utterance / expected answer literal (`grep`-style scan of the rule source for `cs011` / `cs015` / `closure_criterion` etc. — must be zero).

`autoloop/tests/test_content_validator.py` (NEW):
- Placeholder corruption: `before_value="Hello {USER}"`, `after_value="Hello {USE"` → FAIL `rule_id=content_validator.placeholder_corrupted`;
- Placeholder absent in `before_value`: `before_value="Hello there"`, `after_value="Hello {x"` → PASS (no token in before; skip check);
- Length overflow: `len(after) > 5 × len(before)` → FAIL;
- Length underflow with floor: `before="..." (60 chars), after="" (0 chars)` → FAIL `zero_length` (zero-length check fires first);
- Length underflow ratio: `before="..." (100 chars), after="ok" (2 chars)` → FAIL `length_underflow`;
- Underflow ratio NOT enforced when before is short: `before="ok" (2 chars), after="" (0 chars)` → FAIL `zero_length` only (underflow ratio skipped since before < 50);
- Deny-list hit: `after_value` contains `<<SYSTEM>>` → FAIL `deny_list`;
- Clean edit: small soft narrative tweak → PASS;
- **D3 regression**: a fixture test that asserts the validator does NOT depend on Salesforce-specific token shapes when `custom_token_shapes` is empty (i.e., a Salesforce ID-shaped string in `after_value` does NOT trigger).

`autoloop/tests/test_gaming.py` (NEW):
- Each of the 7 checks triggered by a synthetic fixture (positive);
- Each of the 7 checks NOT triggered by a clean fixture (negative);
- `scoring_code_drift.baseline_missing` WARN when `config.fitness.scoring_code_baseline_sha` is None (D3 regression);
- `shadow_set_leakage` config-driven only: detector with empty `config.gaming.shadow_leak_signatures` produces NO flag even when iteration_record contains a `cs59s` string (D3 regression — detector must not have implicit defaults reading shadow content);
- `tier2_measurement_contract_change_attempt` triggered by a previously-FAIL critical_step now scoring N/A.

`autoloop/tests/test_loop_anti_hardcode_wire.py` (NEW; integration):
- **Adversarial-fixture regression target**: the S-Auto-3 test in `test_loop.py` (`test_adversarial_fixture_passes_placeholder_anti_hardcode`) is **renamed to `test_adversarial_fixture_fails_real_detection`** and asserts `decision="discard"` + `discard_reason` starts with `anti_hardcode_rejected:`. **This rename + assertion flip is the load-bearing test for the placeholder → real transition.**
- `test_content_validator_failure_discards_before_sandbox`: mocked content_validator returns FAIL → iteration discards; sandbox.validate_skill_yaml_diff is NEVER called.
- `test_gaming_flags_persisted_to_experiments_log_and_visible_in_audit`: 1-iter integration test that runs a happy-path loop with a fixture that triggers `anomalous_metric_movement`; assert `experiments_log` row has `gaming_flags` list non-empty; assert `audit --experiment exp-N` output renders the flag.
- `test_anti_hardcode_flag_for_codex_does_not_discard`: hypothesis triggers FLAG_FOR_CODEX (not FAIL); iteration continues to apply / eval; `experiments_log` row has `anti_hardcode_flag_for_codex: true`.

**Existing test updates (NOT new tests)**:
- `autoloop/tests/test_loop.py::test_adversarial_fixture_passes_placeholder_anti_hardcode` → renamed + flipped per above.
- `autoloop/tests/test_meta_agent.py` (S-Auto-3 anti-hardcode placeholder result `placeholder=True` assertion) → updated to assert `placeholder=False` (the detector is no longer a placeholder).

### #9 — `autoloop/README.md` (single line)

Update the CLI table `audit` row description to note that `audit` surfaces `gaming_flags` + `anti_hardcode_flag_for_codex` in S-Auto-4 onward. No other README change.

## Hard fences / STOP conditions

- **Signature preservation contract with S-Auto-3**:
  - `anti_hardcode_check(hypothesis, *, config) -> AntiHardcodeResult` — signature verbatim from S-Auto-3; ONLY the body changes.
  - `AntiHardcodeResult.placeholder` field stays in the dataclass but is `False` in all S-Auto-4 returns.
  - `loop.py` `IterationResult` dataclass — add `gaming_flags: list[dict] = field(default_factory=list)` as an **additive optional field** with default value, so S-Auto-3 tests that construct `IterationResult` without this field continue to PASS.
- **No touch** to `eval_interactive/eval_interactive/**`, case_spec, case_specs_shadow, `server/`, `eval/` Java, `data/`, `db/`, `server/src/main/resources/`, `docs/foundational/`, `docs/runtime_freeze_and_risk_policy.md`, `docs/current/`, `docs/sprints/*` archives, `docs/milestones/*` archives, `docs/codex-findings.md`.
- **No new heavy deps** in `autoloop/pyproject.toml` — stdlib `re` + `unicodedata` + `subprocess` (for `git show` in gaming check 3) only. If a dep is needed, STOP and surface to deliver-agent BEFORE adding.
- **No LLM call** inside `anti_hardcode_check` or `content_validator` (deterministic + cheap; no propose-stage latency hit; no second LLM dependency).
- **No semantic hardcode IN the detector itself** — the rules must encode generic structural shapes ONLY (per D2). If a rule definition contains a specific eval phrase / user utterance / expected answer / case-success-label literal, the rule is structurally wrong. The detector self-discipline test (`test_anti_hardcode_check.py` final fixture) scans the rule source for forbidden literals.
- **No shadow case content read by gaming.py** (per D3) — `gaming.detect` does NOT open any file under `eval_interactive/case_specs_shadow/`, does NOT depend on the shadow corpus content; `shadow_set_leakage` rule reads ONLY `config.gaming.shadow_leak_signatures` (out-of-band human-approved config).
- **No baseline guessing in gaming.py** (per D3) — if `config.fitness.scoring_code_baseline_sha` is missing/None, emit WARN `baseline_missing`; DO NOT compute a hash and silently use it as baseline.
- **STOP and surface** if the detector regex set exceeds ~30 rules total (sign of over-fit / pattern explosion — re-design before shipping).
- **STOP and surface** if `FLAG_FOR_CODEX` rate on the calibration table is >25% (detector too noisy — re-calibrate before shipping; `config.anti_hardcode.flag_for_codex_rate_warn_threshold` documents this threshold).
- **STOP and surface** if any Codex bypass spot-check (unicode/synonym/decomposition) succeeds during the §4.3 per-sub-sprint review — that finding becomes a fix-iteration before milestone close.
- **No live LLM call in tests** — all anti_hardcode / content_validator / gaming tests use synthetic fixtures.

## Test / eval requirements

- **Python autoloop suite**: `cd autoloop && uv run pytest -q` — Sprint 56 baseline `147 passed` MUST grow by ~50-80 NEW S-Auto-4 tests (total ~197-227 PASS, 0 fail). Sprint 54 + Sprint 55 tests UNCHANGED. Sprint 56 tests updated only at the two rename points listed in #8.
- **Existing Python eval_interactive suite UNCHANGED**: `cd eval_interactive && uv run python -m pytest --tb=no -q` reproduces `486 passed, 3 failed`.
- **Java baseline UNCHANGED**: skipped per S-Auto-4 Java-zero-touch (verify `git diff --stat HEAD -- server/ eval/src/main/java/` returns empty).
- **NO live LLM call in pytest tests** — all detector / validator / gaming tests use synthetic fixtures.
- **Calibration table** (deliver-agent ships in dev-prompt + dev verifies in pytest): ≥8 forbidden propose examples (including the §1.7 explicit example `IF user.message.contains('appeal') THEN active_use_case := UC-H`) all auto-rejected at propose-stage; ≥4 clean soft-narrative examples (one per allowed field type) all pass; ≥2 borderline FLAG_FOR_CODEX cases.
- **S-Auto-3 adversarial regression**: the existing `test_adversarial_fixture_passes_placeholder_anti_hardcode` renamed + flipped to `test_adversarial_fixture_fails_real_detection`; assertion `decision=="discard"` + `discard_reason.startswith("anti_hardcode_rejected:")`.

## §7 — Layer-classification + anti-hardcode stanza

**Target failure layer:** `eval_spec` (§3.2 Q6 — the kernel is structural §1.7 defence at the meta-agent output boundary; it does not change runtime / projection / scoring / CaseSpec; it is the propose-stage gate that prevents §1.7 violations from reaching build / eval). §7 REQUIRED applies: the kernel + content validator + gaming checks are the §1.7 structural guard whose enforcement plumbing S-Auto-1/2/3 built; S-Auto-4 closes the chain.

**Tier-0 invariant:** adds no Tier-0 invariant. The kernel is meta-loop infrastructure, NOT a runtime invariant per `docs/runtime_freeze_and_risk_policy.md` §1/§2. The kernel DETECTS attempts to invent new Tier-0 semantics (Q2 rule) — that's the OPPOSITE of introducing one.

**Semantic hardcode:** No semantic hardcode introduced. Justification by surface:

- `anti_hardcode_check.py` rules are **generic structural pattern matches** (per D2) — IF/THEN shapes, `cs<id>`-like tokens, MUST/NEVER phrasing structures, `.contains(...)` literals. The rules do NOT enumerate specific eval phrases / user utterances / expected answers / case-status labels. The detector self-discipline test (`test_anti_hardcode_check.py`) regression-guards this.
- `content_validator.py` is structural integrity only — placeholder tokens already present in `before_value`, length bounds, configurable deny-list. NO Salesforce-specific or domain-specific semantic logic (per D3 — only the three generic placeholder shapes; custom shapes are config-driven).
- `gaming.py` checks are numeric metric comparisons + structural artefact properties + config-driven leak-signature scans. NO shadow case content read. NO baseline guessing. The new `tier2_measurement_contract_change_attempt` is a structural impossibility alarm; if it fires, something is wrong with the sandbox, not a semantic decision.
- `loop.py` wiring extensions are dataclass-additive only; no signature change to the orchestrator or to S-Auto-1/2/3 deliverables.

**Generalization coverage:** target = the auto-tractable §4.1 kernel subset (Q1 + Q2 + Q4 + Q5) auto-rejects the 8+ forbidden calibration examples + accepts the 4+ clean calibration examples + the S-Auto-3 adversarial fixture flips PASS → FAIL. Neighbor = unicode NFKC + multi-line decomposition + small synonym-map equivalents of the forbidden patterns. Negative-control = clean soft-narrative edits per allowed field type (no false positive). Adversarial = Codex per-sub-sprint constructs ≥3 hand-authored adversarial proposes (unicode obfuscation, multi-line decomposition, semantic-equivalence via synonym swap); detector must reject each. Shadow firewall coverage = `shadow_set_leakage` detector verified by a fixture that includes a `cs59s` token in iteration_record AND a config WITHOUT that signature → no false positive (detector is config-driven, NOT implicit-default).

## Codex review plan (§4.3)

**PER-SUB-SPRINT REQUIRED — §4.3 trigger #2**. The anti-hardcode kernel + content validator + gaming checks ARE the §1.7 structural guard; Codex must independently verify the detector design BEFORE M-Auto-1B begins running it against real meta-agent outputs.

**Codex prompt timing**: deliver-agent authors `compact/sprint-057-codex-review-prompt.md` at S-Auto-4 **close** (NOT at open), covering the actual delivered commit range.

**Codex must verify**:

1. §4.1 nine-question kernel walk against the detector source (`anti_hardcode_check.py` + `content_validator.py` + `gaming.py`): does the defender encode §1.7-violating decision logic itself? (D2 regression — detector rules must be generic structural; specific eval phrases / user utterances / answers / labels are forbidden.)
2. The detector catches the 8+ forbidden calibration examples and at least 1 additional hand-authored adversarial example Codex constructs.
3. ≥3 bypass spot-checks: unicode obfuscation (NFKC + visual confusables), multi-line decomposition (if-then split across lines/bullets), semantic-equivalence via synonym swap (e.g., `IF` → `WHEN`, `THEN` → `→`). Detector must reject each.
4. D3 regressions: gaming.py does NOT read `case_specs_shadow/` content directly; `scoring_code_drift.baseline_missing` emits WARN (not guess) when config field is unset.
5. The verdict set has no obvious design hole; the FLAG_FOR_CODEX semantics are not abused to silently auto-accept borderline cases.

**Verdict expected**: `pass / 0` or `approve with downgrade-to-signal follow-up`. Must return BEFORE M-Auto-1A milestone close. `reject as semantic hardcode` triggers fix-iteration sub-sprint (extends M-Auto-1A; pre-mitigated by the deliver-agent ships calibration table in dev-prompt + dev runs detector self-discipline test before commit).

## Handoff requirements

- Author `docs/sprints/sprint-057-handoff.md` at S-Auto-4 close; leave **§12** empty (deliver-agent + human at milestone close).
- Record in handoff: `git show --numstat` for the S-Auto-4 commit; calibration table actual results (8+ forbidden / 4+ clean / 2+ FLAG_FOR_CODEX); detector LOC + total rule count (must be ≤30); gaming.py LOC + 7 checks × positive + negative fixture status; content_validator LOC + test count; pytest delta (147 → ?); rename of S-Auto-3 adversarial test; `autoloop/config.yaml` content_validator + anti_hardcode + gaming blocks (with deny_list + leak_signatures seeded); §7 self-walk; OQ-S57.x list including the FLAG_FOR_CODEX rate measurement on the calibration set.
- Author `compact/sprint-057-codex-review-prompt.md` AS PART OF the S-Auto-4 close-bundle (deliver-agent writes this; embeds §4.1 nine-question kernel verbatim + commit range + the 4 verification axes above; references the §6 hard fences from `docs/milestone_objective.md` + §6.1 OQ-S56.1 disposition for the stable hard-fence list).

## Commit discipline

Dev stages **only S-Auto-4 scope**: 3 NEW source files (`autoloop/autoloop/sandbox/anti_hardcode_check.py` [body swap; signature unchanged], `autoloop/autoloop/sandbox/content_validator.py`, `autoloop/autoloop/scoring/gaming.py`) + 4 NEW test files + 2 existing test updates (renames + assertion flips in `test_loop.py` + `test_meta_agent.py`) + extended `autoloop/autoloop/loop.py` (additive wiring + dataclass field) + extended `autoloop/autoloop/cli.py audit` rendering + extended `autoloop/config.yaml` (3 NEW blocks) + 1-row update to `autoloop/program.md` §4 row 3 + 1-line `autoloop/README.md` `audit` row note + NEW `docs/sprints/sprint-057-handoff.md`. **No `git add -A`** — deliver-agent close-bundle files (this objective archive rename, milestone_objective.md edits at milestone close, 10-handoff updates, Codex prompt + findings) bundled by the human at close.

One commit at sub-sprint close (commit-at-end pattern). Commit message: `Sprint 57 / S-Auto-4 — anti-hardcode kernel + content validator + gaming checks`.

## Scope size (§8.5 note)

M-Auto-1A with S-Auto-4 = 4 of 4 sub-sprints (FINAL); within §8.5 5-sub-sprint ceiling. S-Auto-4 is medium-large by LOC + test count + structural-defence importance; estimated 4 dev-days + Codex ~2 days. No conditional / deferred scope (everything in scope is required to hit close gates).

## OQ (open questions — filled during the sub-sprint)

- **OQ-S57.x candidates** (expected; dev surfaces as ambiguities encountered):
  - FLAG_FOR_CODEX rate measurement on the actual calibration set (target <25%);
  - Whether any rule needs synonym_map_enabled = true after calibration (default false; dev evidence informs);
  - Whether `gaming.scoring_code_drift` baseline SHA should be the M-Auto-1A close commit SHA or some other reference (deliver-agent fills at S-Auto-4 close; not dev's call);
  - Content_validator deny_list seed list size / contents (deliver-agent + human curate at S-Auto-4 close; dev ships minimal seed).
