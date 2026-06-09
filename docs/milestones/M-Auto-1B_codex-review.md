# M-Auto-1B Codex Reviews (cumulative archive)

This file bundles all 3 Codex reviews for M-Auto-1B in chronological order:

1. **Per-sub-sprint S-Auto-5 / Sprint 058** — 2026-05-28 (originally lived in `git show 95089c2:docs/codex-findings.md`; reproduced verbatim here at M-Auto-1B Phase 3 close 2026-05-30 per Codex Axis M10 downgrade trigger #2 — close-package metadata reconciliation; the live `docs/codex-findings.md` at S-Auto-7 close-bundle `307f69a` no longer carried the S-Auto-5 verdict because S-Auto-7 had appended its own per-sub-sprint review on top).
2. **Per-sub-sprint S-Auto-7 / Sprint 060** — 2026-05-29 (originally lived in the live `docs/codex-findings.md` at S-Auto-7 close-bundle `307f69a`; preserved here).
3. **Milestone-shared M-Auto-1B** — 2026-05-30 (appended at Phase 2 dispatch after the deliver-agent + human §5.6 + shadow rerun evidence collection).

All 3 returned `decision: pass / blocking_count: 0` sub-classified `approve with downgrade-to-signal follow-up`. Sub-classification downgrade triggers (3 at S-Auto-5 / 1 at S-Auto-7 / 3 at milestone-shared) are non-blocking per `iteration_governance.md` §4.1; M-Auto-1B closure verdict in `docs/milestones/M-Auto-1B_objective.md` §12 is A-with-acceptance-bar-revision per `docs/current/deliver_close_taxonomy.md`.

---

## 1. Per-sub-sprint S-Auto-5 / Sprint 058 Codex review (2026-05-28)

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
| `Wheneve​r the customer describes an appeal => route to escalation.` | FAIL/FLAG | PASS | none |
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

---

## 2. Per-sub-sprint S-Auto-7 / Sprint 060 Codex review (2026-05-29)

## Sprint Review Decision
decision: pass
blocking_count: 0
summary: S-Auto-7 passes the per-sub-sprint anti-hardcode review with no semantic-hardcode blockers. Axis A confirms the cumulative diff is pure infra; Axes B-D verify both hard-fence overrides are plumbing-only, the scoring hash reproduces, and the blessed baseline directory is consumable; Axes E-I preserve the expected test, fence, and commit baselines. The final §4.1 verdict is `approve with downgrade-to-signal follow-up` only for governance hygiene: deliver-agent + human should add a controlled-override annotation for the in-session fence #2 `loader.py` repair to `docs/milestone_objective.md` §6 at the S-Auto-7 close-bundle.

### Axis A - Nine-question Kernel: PASS

The §4.1 exemption hypothesis holds: the S-Auto-7 diff is pure infra and does not introduce a semantic hardcode.

- Q1: No keyword, regex, if/else, enum, or per-UC matrix was added for drift, escalation, UC selection, risk classification, follow-up, or routing. `eval_runner.py` removes a nonexistent CLI flag and stages eval output; `loader.py` changes path traversal; `config.yaml` advances baseline metadata.
- Q2: Not applicable. No new Tier-0 invariant is claimed.
- Q3: Not applicable. There is no LLM-projected surface change; the work is subprocess and filesystem plumbing.
- Q4: PASS. Diff grep over `eval_runner.py`, `loader.py`, and `config.yaml` found no added `cs<id>`, `closure_criterion`, `expected_behavior`, `primary_uc`, `failure_tags`, `source_session_id`, or status-label literal. The config comments name run IDs and suite counts as baseline metadata, not decision logic.
- Q5: PASS. No Java, prompt, or semantic-planner ownership moved from the LLM to runtime code.
- Q6: PASS. No prompt if/else block was added.
- Q7: PASS. Tool schema, capability boundary, safety floor, and grounding floor are preserved; Axis G hard-fence checks are clean.
- Q8: PASS for infra scope. The correct analog to semantic generalization coverage is the substrate verification matrix: unit tests for symlink logic, real eval-interactive timestamped outputs, clean `baseline_loader.load`, silent scoring drift check, and loader count negative-controls.
- Q9: Not applicable. The code changes are not temporary semantic rules.

Scope-exemption note: pure infra; §4.3 trigger #3 fence override controlled + verified.

### Axis B - Controlled Fence Overrides: CONCERN

Both overrides are genuinely plumbing-only, but the in-session fence #2 override should be reflected in the live milestone objective for auditability.

Override 1, fence #13 `eval_runner.py`: PASS.

- `docs/milestone_objective.md` documents the planning-round controlled fence #13 override for `autoloop/autoloop/scoring/eval_runner.py`.
- The other scoring files are byte-identical across the S-Auto-7 range: `git diff --stat b6084f9..HEAD -- autoloop/autoloop/scoring/{tier_evaluator,baseline_loader,gaming}.py` returned empty.
- The diff removes `--output-dir`, snapshots `eval_interactive/results/`, finds the new timestamp dir by set-diff plus mtime tie-break, and symlinks `<results_root>/<suite>` to the timestamp dir. No case-text branch, regex, keyword list, or per-UC matrix is present.
- Axis C confirms the rebaselined scoring hash is reproducible and drift checking is silent.

Override 2, fence #2 `loader.py`: PASS on code shape, CONCERN on governance annotation.

Snippet:

```diff
-    for yaml_file in sorted(directory.glob("*.yaml")):
+    for yaml_file in sorted(directory.rglob("*.yaml")):
```

Reasoning: `rglob` plus skipping basenames that start with `_` is substrate path handling for nested shadow case layouts and manifests. It does not interpret case contents or encode semantic decisions. The flat-dir negative controls load the expected counts: anchor 159, promotion 101, exploration 107, smoke 14, bad_cases 12, anchor_outcome 12; shadow loads 22 after skipping `_manifest.yaml`. However, unlike fence #13, `docs/milestone_objective.md` still names `eval_interactive/eval_interactive/` as no-touch and lacks a parallel controlled-override annotation for this in-session human-approved repair.

Fix target layer: `infra`. Recommended verdict downgrade: non-blocking `approve with downgrade-to-signal follow-up`. Trigger: deliver-agent + human update `docs/milestone_objective.md` §6 fence #2 with a controlled-override annotation parallel to the fence #13 annotation at the S-Auto-7 close-bundle.

### Axis C - scoring_code_baseline_sha Reproducibility: PASS

The rebaselined scoring hash matches the actual scoring content and the drift guard is silent.

Verification:

```text
_compute_scoring_code_sha()
22548e20ea50518b35c78cebddde891ddb46ea0452e32e49518ad4caab9188a9

_check_scoring_code_drift(config=config)
PASS: scoring_code_drift silent
```

This validates the fence #13 controlled override and preserves the anti-tampering guard.

### Axis D - Blessed baseline_dir: PASS

The blessed baseline directory is well formed for downstream consumption.

`eval_interactive/results/m-auto-1b-baseline-20260529/` contains three symlinks:

```text
anchor_outcome -> eval_interactive/results/20260529-102054
bad_cases      -> eval_interactive/results/20260529-101324
shadow         -> eval_interactive/results/20260529-102949
```

Each suite has a non-empty `results.json` with the expected case counts: bad_cases 12, anchor_outcome 12, shadow 22. `baseline_loader.load` returns zero warnings and:

```text
bad_cases: total=12 passed=5 tier2_fails=4
anchor_outcome: total=12 passed=7 tier2_fails=4
shadow: total=22 passed=4 tier2_fails=10
```

`autoloop/config.yaml` advances `fitness.baseline_dir` from the placeholder to `eval_interactive/results/m-auto-1b-baseline-20260529`, so Layer 3 and Layer 4 now load a non-empty baseline snapshot.

### Axis E - Goal #4 BLOCKED Disposition: PASS

The smoke iter blockage is upstream of S-Auto-7's fixed surfaces and was handled per the dev contract.

Handoff §7 shows exp-6 reached Step 6 and failed during alt-port Spring spawn with `rc=1` at 90.4s. Steps 7 and 9, where `eval_runner.py` and `tier_evaluator` would be exercised, were structurally unreachable. The diagnosis is consistent with the OQ-S58.7 carryover: concurrent foreground `:8080` Spring plus alt-port spawn competing over Flyway, Maven target/classpath state, and Redis resources.

The independent substrate evidence is adequate for this infra review: `autoloop` pytest covers the new symlink logic, real eval-interactive CLI runs produced the timestamped outputs, `baseline_loader.load` consumes the blessed baseline with zero warnings, `_check_scoring_code_drift` is silent, and `_locate_new_results_dir` is covered by set-diff and mtime tests. OQ-S60.7 remains a downstream substrate issue for deliver-agent + human triage, not a Blocker A/B hardcode concern.

### Axis F - Test Count Baselines: PASS

The test baselines reproduce the handoff claims.

```text
cd autoloop && uv run --extra dev pytest -q
228 passed, 1 warning in 3.34s

cd eval_interactive && uv run python -m pytest --tb=no -q
3 failed, 486 passed in 13.04s

cd autoloop && uv run --extra dev pytest -q tests/test_anti_hardcode_check.py
31 passed in 0.02s
```

The eval-interactive failures are the inherited three: `test_v2_schema_loads_cleanly`, `test_smoke_review_report_tracks_smoke_set_and_overrides`, and `test_full_corpus_lints_clean_with_smoke_subset_flag`.

### Axis G - M-Auto-1B Hard-Fence Walk: PASS

The enumerated hard-fenced surfaces are clean apart from the two reviewed overrides.

- The prescribed `git diff --stat 60c5b67..HEAD -- server/src/main/java/ eval/src/main/java/ eval_interactive/case_specs/ eval_interactive/case_specs_shadow/ data/ db/migration/ server/src/main/resources/ docs/foundational/ docs/runtime_freeze_and_risk_policy.md docs/current/iteration_governance.md docs/teams/` returned empty.
- `eval_interactive/eval_interactive/cli.py` diff is empty, confirming fix path (b) did not add `--output-dir`.
- `server/src/main/resources/skills/` diff is empty; S-Auto-8 remains the only allowed skill-cherry-pick surface.
- Sprint archives `docs/sprints/sprint-001-*` through `docs/sprints/sprint-058-*` are untouched.
- `autoloop/autoloop/sandbox/anti_hardcode_check.py`, `autoloop/autoloop/loop.py`, `autoloop/autoloop/meta_agent/`, `autoloop/autoloop/memory/`, and `autoloop/cli.py` are unchanged in the S-Auto-7 range.

### Axis H - Two-Commit Pattern: PASS

The S-Auto-7 range contains exactly the declared two commits after `60c5b67`, with clean file boundaries.

- `559927a` changes only `autoloop/autoloop/scoring/eval_runner.py`, `autoloop/config.yaml`, and `autoloop/tests/test_eval_runner.py`; its message documents Blocker B fix path (b), the five new tests, two updated tests, and scoring hash rebaseline.
- `b0a3704` changes only `autoloop/config.yaml`, `eval_interactive/eval_interactive/case_spec/loader.py`, and `docs/sprints/sprint-060-handoff.md`; its message documents baseline blessing, Blocker C, the blocked smoke iter, and OQ-S60.7.
- `git rev-list --count 60c5b67..HEAD` returned `2`; no third S-Auto-7 commit is present.

This gives a clean audit trail: scoring override plus hash rebaseline first, baseline blessing plus in-session loader repair second.

### Axis I - Out-of-scope README Commit: PASS

`60c5b67` is a standalone documentation commit and does not affect the S-Auto-7 substrate verdict.

`git show --stat --name-only 60c5b67 --` shows only `README.md`. It does not touch any M-Auto-1B §6 fenced surface, runtime code, eval code, config, or case specs. Planning observation for deliver-agent + human: if intentional, acknowledge it in the close lead; if accidental, decide separately whether to keep or revert it. This observation is not a Codex blocker.

### §4.1 Verdict

`approve with downgrade-to-signal follow-up`

Follow-up trigger: deliver-agent + human update `docs/milestone_objective.md` §6 fence #2 with a controlled-override annotation for `eval_interactive/eval_interactive/case_spec/loader.py`, parallel to the fence #13 annotation, at the S-Auto-7 close-bundle.

The code/config changes under review are pure infra and do not encode a semantic hardcode.

---

## 3. Milestone-shared M-Auto-1B Codex review (2026-05-30)

## M-Auto-1B Milestone-Shared Review Decision
decision: pass
blocking_count: 0
summary: M-Auto-1B passes the milestone-shared anti-hardcode review with zero blocking findings. The cumulative agent-loop changes from S-Auto-5 through S-Auto-7.1 introduce no new semantic hardcode: Fix-C remains a generic detector-normalization change, the S-Auto-7 `eval_runner.py` and `loader.py` overrides compose as substrate plumbing, and S-Auto-7.1 preflight is infra-only. The §8.5 split is compliant and scope-clean, and the §5.6 close evidence is present; the final §4.1 verdict is `approve with downgrade-to-signal follow-up` for non-blocking close-package hygiene and M-Auto-1C drift-envelope discipline, not for any code-level hardcode blocker.

### Axis M1 - §4.1 Nine-question Kernel: PASS

The cumulative agent-loop diff does not add a runtime or prompt semantic hardcode.

- Q1: PASS. The only new regex-like semantic-adjacent change is `_RE_WHEN_WORD_BOUNDARY = re.compile(r"\b(?:whenever|when)\b")`, which canonicalizes existing structural detector synonyms before the existing Q1 arrow-tree rule. It does not route a customer, pick a UC, classify risk, escalate, or alter prompt ownership.
- Q2: PASS. No new Tier-0 invariant is added; C2/C3 remain deferred.
- Q3: PASS. The detector itself is the meta-loop review boundary; no Java or prompt hard branch was added where an LLM soft signal should exist.
- Q4: PASS. `anti_hardcode_check.py` grep found no `closure_criterion`, `expected_behavior`, `primary_uc`, `failure_tags`, `source_session_id`, or `cs<id>` literal.
- Q5: PASS. No agent-loop change moves semantic ownership from the LLM to Java.
- Q6: PASS. No prompt if/else block was added.
- Q7: PASS with Axis M5/M11 caveat. Agent-loop tool schema, capability boundary, safety floor, and grounding floor are preserved; the only runtime/Skill YAML changes are the human-authored ambient commit handled in M11.
- Q8: PASS for the scoped work. S-Auto-5 has 3 real samples + 17 fixture calibration points; S-Auto-7 has substrate tests + real baseline consumption; S-Auto-7.1 has 27 preflight tests and operator transcript.
- Q9: PASS. R-S58 remains the explicit follow-up for zero-width/format-character detector calibration; no temporary runtime semantic rule was introduced.

### Axis M2 - Cumulative §1.7 Composition: PASS

Fix-C normalization, the non-taken Fix-B path, `eval_runner.py` symlink staging, and `loader.py` recursive path handling do not compose into a forbidden-list violation. The sampled diff is limited to the documented changes: word-boundary normalization, removal of `--output-dir` plus timestamp-dir symlink staging, and `glob` -> `rglob` with `_*.yaml` filtering.

### Axis M3 - Sub-sprint Dispositions: PASS

S-Auto-5, S-Auto-6, S-Auto-7, and S-Auto-7.1 classifications are internally consistent.

- S-Auto-5: PASS. `git show 95089c2:docs/codex-findings.md` contains the S-Auto-5 `decision: pass / blocking_count: 0`; `docs/action_bank.md` records R-S57 closed and R-S58 opened.
- S-Auto-6: PASS. `git show --stat 1943ed5` is handoff-only: one file, `docs/sprints/sprint-059-handoff.md`, +325 lines. No baseline rerun, overnight, cherry-pick, LLM budget, or fence override landed.
- S-Auto-7: PASS. The live `docs/codex-findings.md` contains the S-Auto-7 `pass / 0`; `307f69a` adds the fence #2 annotation to `docs/milestone_objective.md`.
- S-Auto-7.1: PASS. `19213b1` is pure autoloop + handoff scope; `applier.py` is untouched; preflight and OQ-S60.10 landed, while OQ-S61.1 is correctly carried to M-Auto-1C.

### Axis M4 - Acceptance Bar Audit: PASS

The 8/15 met and 7/15 deferred framing is honest. The seven deferred hard gates all trace to the smoke iter not reaching Step 9, now root-caused to `applier.py:370` invoking `mvn -q -pl server -am spring-boot:run` against a parent `packaging=pom` reactor. `pom.xml` confirms `artifactId=csagent-parent`, `<packaging>pom</packaging>`, and modules `server` + `eval`; `git diff b6b627b..HEAD -- autoloop/autoloop/sandbox/applier.py` is empty.

### Axis M5 - Cumulative Hard-fence Verification: PASS WITH DOCUMENTED EXCEPTIONS

The hard-fence walk is clean after applying the two documented exception classes.

- Controlled overrides: other scoring siblings are byte-identical (`tier_evaluator.py`, `baseline_loader.py`, `gaming.py` diff empty); `eval_interactive/eval_interactive/cli.py` diff empty; scoring SHA reproduces as `22548e20ea50518b35c78cebddde891ddb46ea0452e32e49518ad4caab9188a9`.
- Ambient human work: protected `server/src/main/java/.../ToolCallTraceSanitizer.java` and two Skill YAML budget edits trace to `7871c62`; handled in M11.
- Bad-case ledger: `eval_interactive/case_specs/bad_cases/_manifest.md` changed in `5b9c143`. Live `docs/milestone_objective.md` §6 now clarifies this as a §5.6 lifecycle-ledger close surface, and `iteration_governance.md` §5.6 names `_manifest.md` as the lifecycle ledger. CaseSpec YAML content remains byte-identical: `git diff --stat b6b627b..HEAD -- 'eval_interactive/case_specs/**/*.yaml' 'eval_interactive/case_specs_shadow/**/*.yaml'` returned empty.
- Prior milestone archives: `docs/milestones/M-Auto-1A_*` are in the raw `b6b627b..HEAD` range because M-Auto-1A archival commits landed after `b6b627b`. This is a range-metadata issue, not an M-Auto-1B semantic/code issue; see M10.

### Axis M6 - §5.6 Bad-case + Shadow Evidence: PASS WITH FOLLOW-UP

The Phase 2 evidence is present in `docs/milestone_objective.md` §12.5 and `_manifest.md`; it is not missing, so there is no P0 timing blocker.

The bad-case primary gate is jointly judged PASS by deliver-agent + human: `bad_cases` held at 5/12 case_passed with two regressions and two improvements/flake clears. Anchor/shadow show a higher provider-drift signature than M-Auto-1A: anchor_outcome 7/12 -> 3/12 and shadow 4/22 -> 1/22, clustered on UC-D/E/F/FP auth/account/billing flows. I do not overrule the human-primary §5.6 judgment, and I do not see evidence tying the drift to agent-loop code; however, S-Auto-8 should establish a >=2 rerun drift envelope before overnight.

### Axis M7 - §8.5 Split Decision: PASS

The split is §8.5-compliant and scope-clean. M-Auto-1B reached the five-sub-sprint ceiling at S-Auto-7.1; adding S-Auto-7.2 would make a sixth sub-sprint, so splitting at the next planning round follows the "SHALL split" directive. M-Auto-1C's draft scope, S-Auto-7.2 applier fix plus S-Auto-8 overnight/cherry-pick, is coherent continuation scope rather than unrelated work.

### Axis M8 - Reproducibility Spot-checks: PASS WITH TEST GAP

Spot-checked claims reproduced:

- `_compute_scoring_code_sha()` -> `22548e20ea50518b35c78cebddde891ddb46ea0452e32e49518ad4caab9188a9`.
- `uv run --extra dev pytest -q tests/test_preflight.py` -> `27 passed`.
- `uv run --extra dev pytest -q tests/test_anti_hardcode_check.py` -> `31 passed`.
- `baseline_loader.load(...)` -> warnings `[]`; suites `anchor_outcome`, `bad_cases`, `shadow`; counts `(12,7,4)`, `(12,5,4)`, `(22,4,10)`.
- `pom.xml` confirms the OQ-S61.1 parent packaging/module-selection root cause.

Test gap: I attempted the full autoloop pytest run, but it did not complete promptly in this shell and was terminated after partial progress. I do not use that aborted run as evidence; the targeted checks above satisfy the required spot-check threshold.

### Axis M9 - R-item Flips Audit: CONCERN, NON-BLOCKING

R-S57 closed and R-S58 opened are correctly annotated in `docs/action_bank.md` §5. The seven M-Auto-1A carry-overs remain present, and OQ-S61.1 is documented in `docs/milestone_objective.md` and `docs/10-handoff.md` as an M-Auto-1C transition candidate.

Non-blocking ledger concern: Phase 2 text names a new `R-eval-interactive-judge-score-never-populated`, but I did not find a corresponding `docs/action_bank.md` entry yet. Phase 3 should either add that R-item to action_bank or explicitly demote it to an observation before archiving M-Auto-1B.

### Axis M10 - Review-package Consistency: CONCERN, NON-BLOCKING

The prompt package has two reproducibility drifts that do not affect the semantic verdict.

- Commit range count: `git rev-list --count b6b627b..HEAD` is 18, not 14. The extra commits include M-Auto-1A archival commits after `b6b627b` plus Phase 1.6 evidence (`5b9c143`). The actual graph confirms `b6b627b` is an ancestor, but the previous-milestone "closed at b6b627b" wording is stale relative to `7ed95e7` archival close.
- Live findings file: current `docs/codex-findings.md` contains S-Auto-7 plus this appended review; S-Auto-5's verdict is preserved in history at `95089c2:docs/codex-findings.md` and in `docs/action_bank.md`, but not in the live file despite the prompt saying both per-sub-sprint reviews are live.

Recommended close-package fix target layer: `infra` / docs governance. This is not a semantic hardcode blocker.

### Axis M11 - Ambient Human Commit 7871c62: PASS

The ambient commit exists in the range and matches the disclosed content: `git show --stat 7871c62` reports the PII sanitizer refactor, Java test updates, two Skill YAML `max_tool_steps` edits, README, and runbook line. `git diff 7871c62..19213b1 --stat` shows S-Auto-7.1 does not depend on that Java/Skill content; it is autoloop + handoff scope only. The disposition is recorded in `docs/milestone_objective.md` §12.9 and §12.13.

I accept the Axis I-style treatment as ambient human work rather than an agent-loop scope violation. This does not create precedent for agents to bypass the S-Auto-8 cherry-pick mechanism.

### Axis M12 - Architecture-health Metric Direction: PASS

- `new_semantic_hardcode_count`: 0 for the M-Auto-1B agent-loop changes. Direction held flat at 0.
- `soft_signal_conversion_count`: 0; expected because no agent-loop cherry-pick landed.
- `planner_ownership_ratio`: unchanged for agent-loop scope.
- `shadow_disagreement_rate`: first overnight measurement deferred to M-Auto-1C/S-Auto-8; Phase 2 shadow rerun is regression-safety evidence, not the overnight metric.

### §4.1 Verdict

`approve with downgrade-to-signal follow-up`

Follow-up trigger: M-Auto-1C opening / S-Auto-8 planning should (1) establish a >=2 rerun drift envelope before overnight, given the elevated UC-D/E/F/FP drift in Phase 2; (2) reconcile close-package metadata by correcting the stale commit-range/count wording and preserving the S-Auto-5 Codex verdict pointer in the archive; and (3) either action-bank `R-eval-interactive-judge-score-never-populated` or demote it to an observation.

This downgrade is for close-package and drift-envelope hygiene only. The cumulative agent-loop changes do not encode a semantic hardcode.
