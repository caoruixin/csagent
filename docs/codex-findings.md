## Sprint Review Decision
decision: pass
blocking_count: 0
summary: S-Auto-5 passes the per-sub-sprint anti-hardcode review with no blockers. Fix-C step 1 is a generic `eval_spec` normalization refinement of the existing `when` / `whenever` synonym behavior, the Path A config flip is supported by the documented calibration evidence, hard fences hold, and local verification passes. Axis C found one new zero-width obfuscation bypass, so the final §4.1 outcome is `approve with downgrade-to-signal follow-up` rather than a clean approve.

### Axis A - Nine-question Kernel: PASS

The §4.1 kernel does not identify a blocking semantic hardcode in Fix-C step 1 or the Path A toggle.

- Q1: The diff adds a regex, but it is not a runtime semantic decision branch. It canonicalizes the same two structural connector words already present in `_SYNONYM_MAP`; it does not add a new UC, escalation, drift, routing, or risk enum.

  Diff snippet:

  ```diff
  +_RE_WHEN_WORD_BOUNDARY = re.compile(r"\b(?:whenever|when)\b")
  +        norm = _RE_WHEN_WORD_BOUNDARY.sub("if", norm)
  ```

- Q2: No new Tier-0 invariant is claimed or needed. The change lives in the meta-loop anti-hardcode detector, not runtime Java or prompt behavior.
- Q3: The detector is already the soft review boundary for proposed skill edits; Fix-C step 1 only routes a previously missed structural `Whenever ... =>` shape into the existing Q1 arrow-tree detector. Path A is sound for this sub-sprint because calibration found 0 FLAG and 0 FP on the three real clean samples plus preserved the 17-fixture distribution.
- Q4: No visible-eval case text, CaseSpec id, trace-specific phrase, user utterance, expected answer, or case-status label is encoded in the detector rule source. Grep spot-check found no `cs011` / `cs015` / `cs042` / `cs101` / `cs59s`, `closure_criterion`, `expected_behavior`, `source_session_id`, `primary_uc`, or `failure_tags` literal in `anti_hardcode_check.py`.
- Q5: No semantic ownership moves from the LLM to Java. `server/src/main/java/**` is unchanged in the review range and byte-identical by empty diff from both `b6b627b` and `c9390dc` for that path.
- Q6: No prompt if/else block was added.
- Q7: Tool schema, capability/permission boundary, PII/safety floor, and grounding floor are preserved. The detector remains stdlib `re` / `unicodedata` with no LLM call.
- Q8: Generalization coverage is adequate for S-Auto-5: target Axis B bypass, neighbor/multi-line/capitalization variants, negative controls, three real-meta-agent samples, and the existing 17-fixture suite. The dev's five broader generalization gaps remain appropriate Fix-D deferrals; Axis C below adds one new R-item.
- Q9: Fix-C is not presented as temporary. The only new rollback/follow-up need is the non-blocking Axis C bypass item.

### Axis B - Codex Axis B Exact Bypass: PASS

`test_fix_c_step1_codex_axis_b_bypass_now_fails` uses the exact after-value `Whenever the customer describes an appeal => route to escalation and skip normal triage.`, runs with `synonym_map_enabled=True`, and asserts `FAIL` with a `Q1.` rule id. Local command:

```text
cd autoloop && PYTHONDONTWRITEBYTECODE=1 uv run --extra dev pytest -p no:cacheprovider -q tests/test_anti_hardcode_check.py::test_fix_c_step1_codex_axis_b_bypass_now_fails
1 passed in 0.06s
```

Routing matches the handoff: NFKC/lowercase leaves `whenever ... => ...`; `_RE_WHEN_WORD_BOUNDARY` maps `whenever` to `if`; `_SYNONYM_MAP` maps `=>` to `→`; `_RE_Q1_ARROW_TREE` matches and returns `Q1.if_then_decision_tree`.

### Axis C - New Adversarial Bypass Spot-checks: CONCERN

Four of five new Codex probes behaved as expected, but one new bypass surfaced. This is not a blocker for S-Auto-5 because the shipped target fix works and the bypass requires invisible-character obfuscation, but it should become a new R-item for Fix-D calibration.

| Construction | Expected | Actual | rule_id |
|---|---:|---:|---|
| `Wheneve\u200br the customer describes an appeal => route to escalation.` | FAIL/FLAG | PASS | none |
| `Whenever the customer describes an appeal --> route to escalation.` | FAIL | FAIL | `Q1.if_then_decision_tree` |
| `WHENEVER the customer describes an appeal => route to escalation.` | FAIL | FAIL | `Q1.if_then_decision_tree` |
| `(Whenever the customer describes an appeal) => route to escalation.` | FAIL | FAIL | `Q1.if_then_decision_tree` |
| `Note: whenever the customer describes an appeal => route to escalation.` | FAIL | FAIL | `Q1.if_then_decision_tree` |

Concern snippet:

```diff
+_RE_WHEN_WORD_BOUNDARY = re.compile(r"\b(?:whenever|when)\b")
+        norm = _RE_WHEN_WORD_BOUNDARY.sub("if", norm)
```

Reasoning: U+200B inside `whenever` survives NFKC normalization, so the word-boundary regex never sees the token and the arrow tree lacks the leading `if`. Fix target layer: `eval_spec` anti-hardcode detector calibration. Recommended downgrade: non-blocking `approve with downgrade-to-signal follow-up`; trigger `R-S58-anti-hardcode-zero-width-when-arrow-bypass` should evaluate generic Unicode format-character handling or a FLAG_FOR_CODEX signal before any new FAIL rule is promoted.

### Axis D - Real-meta-agent Calibration Evidence: PASS

The fixture contains exactly three samples (`exp-2`, `exp-3`, `exp-4`). Each has the required fields, `synonym_map_enabled: true`, `clean_prose_label: clean`, `captured_date: 2026-05-28`, and sanitization notes. Spot-check over `proposed_value`, `before_value`, and `rationale` found no `cs<id>`, `case_id`, `session_id`, or `source_session_id` token.

`tests/test_real_meta_agent_calibration.py` contains the three expected tests and passes locally:

```text
cd autoloop && PYTHONDONTWRITEBYTECODE=1 uv run --extra dev pytest -p no:cacheprovider -q tests/test_real_meta_agent_calibration.py
3 passed in 0.06s
```

I also spot-checked the three samples under both `synonym_map_enabled=true` and `false`; all three return `PASS` with no rule id in both modes.

Judgment on the `3 < 10` gap: adequate for this Path A toggle, not a blocker. The three real samples are clean and all PASS under both configs; the existing 17-fixture suite continues to span forbidden, clean, and borderline shapes. This is weaker than ten real samples, but the human-approved augmented-evidence path plus unanimous PASS pattern is enough for a meta-loop detector toggle. I do not add a separate sample-count downgrade trigger.

### Axis E - Detector Self-discipline and Rule Count: PASS

The three self-discipline tests are present and pass as part of the full detector file. Independent source grep found no forbidden eval ids or user-utterance/status labels in `anti_hardcode_check.py`. `_RULES` remains 11, unchanged and below the ≤30 cap; Fix-C step 1 adds no registered rule.

### Axis F - 17-fixture Sweep: PASS

The detector test file passes locally:

```text
cd autoloop && PYTHONDONTWRITEBYTECODE=1 uv run --extra dev pytest -p no:cacheprovider -q tests/test_anti_hardcode_check.py
31 passed in 0.06s
```

The source preserves the handoff's calibration shape: 11 forbidden FAIL expectations, 4 clean PASS/non-FAIL controls, 2 borderline FLAG_FOR_CODEX expectations, plus the 4 new Fix-C step 1 tests.

### Axis G - Live-iter End-to-end Evidence: PASS

The handoff documents the required live-iter evidence: `mvn package` success with `server/target/csagent-server-0.1.0-SNAPSHOT.jar` (locally present at 58M), `python -m autoloop check` success, clean pre-iteration git status, and a four-iteration table with terminal verdicts for `exp-1` through `exp-4`. I reran the local check:

```text
cd autoloop && PYTHONDONTWRITEBYTECODE=1 uv run python -m autoloop check
PASS: all 6 Skill YAML paths exist on disk
PASS: sandbox self-test (1 positive + 1 negative)
```

`OQ-S58.7` is correctly classified as a substrate observation: the Spring spawn failure happened after propose-stage capture, terminal error is allowed by scope, and `applier.py` is hard-fenced in S-Auto-5.

### Axis H - Two-commit Deviation: PASS

The two-commit pattern is justified and scope-contained. `b0ce174` adds only `anthropic` to `autoloop/pyproject.toml` and regenerates `uv.lock`; `ae0ec3e` carries the detector, config, tests, fixture, and handoff. Because `uv add anthropic` was required before the first real live iteration could run from a clean working tree, the separate bootstrap commit improves the audit trail rather than hiding scope creep. No loader-path follow-up is needed for this verdict; the `_load_env_local` mismatch remains a documented substrate OQ outside S-Auto-5.

### Axis I - Hard-fence Verification: PASS

The prescribed hard-fence diff is empty:

```text
git diff --stat 6e8692d..ae0ec3e -- server/src/main/java/ eval/src/main/java/ eval_interactive/eval_interactive/ eval_interactive/case_specs/ eval_interactive/case_specs_shadow/ data/ db/migration/ server/src/main/resources/ docs/foundational/ docs/runtime_freeze_and_risk_policy.md docs/current/iteration_governance.md docs/teams/ autoloop/autoloop/scoring/
<empty>
```

Additional spot-checks: prior sprint archive diff through `docs/sprints/sprint-057-*` is empty; `server/src/main/resources/skills/*.yaml` diff is empty; `autoloop/autoloop/meta_agent/llm_client.py` and `eval_interactive/eval_interactive.yaml` are unchanged; the scoring hash is still `5177b674b5ad249d7c0e706f3c827010c8dab511240f239dbd3be6f689a0331c`; `gaming.observation_only_in_v1` remains `true`; `git status --short` was clean before writing this review.

### Suite Sanity Check

Full autoloop pytest also passes:

```text
cd autoloop && PYTHONDONTWRITEBYTECODE=1 uv run --extra dev pytest -p no:cacheprovider -q
223 passed, 1 warning in 3.06s
```

The warning is the documented baseline-missing-shadow warning from `baseline_loader.py`.

### §4.1 Verdict

`approve with downgrade-to-signal follow-up`

Non-blocking follow-up trigger: open `R-S58-anti-hardcode-zero-width-when-arrow-bypass` for M-Auto-1B / Fix-D calibration. The follow-up should evaluate whether generic Unicode format-character normalization or a `FLAG_FOR_CODEX` signal is appropriate for invisible-character obfuscation before any broader FAIL rule is promoted. S-Auto-6 is not blocked by this finding.
