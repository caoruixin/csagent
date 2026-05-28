---
title: Sprint objective — Sprint 058 / M-Auto-1B S-Auto-5 — Live-iter bootstrap + Detector calibration (Fix-C hybrid)
doc_tier: current-runtime
status: current
implementation_status: not_started
source_of_truth: this file
last_reviewed: 2026-05-28
review_cadence: per sub-sprint
supersedes: [docs/sprints/sprint-057-objective.md]
superseded_by: null
notes: >
  FIRST sub-sprint of Milestone M-Auto-1B — Auto-Evolution
  Calibration. S-Auto-5 retires the OQ-S56.5 live-iter waiver from
  M-Auto-1A close, runs 1-3 real iterations against a real
  meta-agent LLM to accumulate ≥10 propose samples, and consumes
  `R-S57-anti-hardcode-whenever-arrow-synonym-bypass` via Fix-C
  hybrid (word-boundary regex + evidence-driven
  `synonym_map_enabled` toggle decision + optional Fix-B FLAG rule
  fallback). §7 REQUIRED (`infra` primary + `eval_spec`
  calibration); **Codex PER-SUB-SPRINT REQUIRED per §4.3 trigger
  #2** — the Fix-C detector change touches the §1.7 structural
  guard; Codex must independently verify Fix-C has no design hole
  + calibration evidence is sound BEFORE S-Auto-6 begins overnight
  running it against real propose outputs. Codex must return
  `pass` (or `approve with downgrade-to-signal follow-up`) BEFORE
  S-Auto-6 overnight starts.

  Builds on M-Auto-1A close `b6b627b` (Sprint 057 / S-Auto-4 — Anti-
  hardcode kernel + content validator + gaming checks). The
  baseline detector content hash `5177b674b5ad249d7c0e706f3c827010c8dab511240f239dbd3be6f689a0331c`
  is the M-Auto-1A close baseline for the four `autoloop/autoloop/scoring/`
  files; those four files are LOCKED in M-Auto-1B (hard fence #13)
  — detector calibration change lands in `autoloop/autoloop/sandbox/anti_hardcode_check.py`
  + `autoloop/config.yaml` + `autoloop/tests/` only.

  Three human-locked planning decisions 2026-05-28 (AskUserQuestion):
  (a) proposal `docs/solutions/m_auto_1b_calibration_planning.md`
  adopted as planning baseline; (b) sub-sprint slicing = Alt-S2
  (S-Auto-5 + S-Auto-6); (c) R-S57 fix path = Fix-C hybrid. This
  contract operationalizes Fix-C step 1 (mandatory; word-boundary
  regex) + Fix-C step 2 (evidence-driven toggle decision based on
  ≥10 real-meta-agent calibration samples).
---

# Sprint 058 / M-Auto-1B S-Auto-5 — Live-iter bootstrap + Detector calibration (Fix-C hybrid)

## Class

`infra` (primary; §3.2 Q1 boundary — the sub-sprint's first half is build infrastructure + live-iter prerequisite + first real iteration, none of which is a runtime semantic change) + `eval_spec` (calibration; §3.2 Q6 boundary — the sub-sprint's second half is structural detector refinement + evidence-driven config toggle decision, both at the meta-agent output boundary, not at the runtime / projection / scoring / CaseSpec layer). **§7 REQUIRED** — S-Auto-5 introduces structural changes to the §1.7 anti-hardcode defender (word-boundary regex extension + optional FLAG rule fallback) and the evidence-driven `synonym_map_enabled` toggle decision; both are semantic-touching at the meta-agent output boundary even though no runtime / projection / scoring change occurs.

## Goal

S-Auto-5 close 时:

1. **Live-iter prerequisites retired.** `mvn package -pl server -am -DskipTests` produces `server/target/*.jar`; `AUTOLOOP_META_LLM_API_KEY` confirmed live via `python -m autoloop check` returning success; working tree clean on `auto-loop-branch` immediately before the first live iteration.

2. **≥1 real iteration completed end-to-end.** `python -m autoloop run --experiments 1` (NO `--dry-run`) drives the full 14-step state machine to a verdict (keep / discard / error all acceptable) on `auto-loop-branch`. Per-iteration elapsed wall-clock time is recorded; if the first iter completes <15 min, run 2-3 more iterations to accumulate ≥10 real meta-agent propose samples in `autoloop/results/experiments.jsonl` for calibration input.

3. **Fix-C step 1 implemented.** `autoloop/autoloop/sandbox/anti_hardcode_check.py` `_normalize()` extended with a word-boundary regex substitution `re.sub(r"\b(?:whenever|when)\b", "if", norm)` guarded by `if synonym_map_enabled`. Existing `_SYNONYM_MAP` whole-string substitutions UNCHANGED (Fix-C step 1 is additive). Existing 17-fixture calibration table sweep continues to produce **11/11 forbidden FAIL + 4/4 clean PASS + 2/2 borderline FLAG_FOR_CODEX**; existing 3 detector self-discipline regression tests (`test_detector_source_does_not_hardcode_eval_case_ids` + `..._user_utterance_literals` + `..._rule_count_bounded`) continue to PASS; rule count stays ≤30. The Codex Axis B exact bypass `Whenever the customer describes an appeal => route to escalation and skip normal triage.` now FAILS the detector (no longer PASS-bypasses).

4. **Real-meta-agent batch calibration recorded with evidence.** The ≥10 real meta-agent propose samples are classified through the detector with `synonym_map_enabled=true` (temporary toggle for measurement); per-sample verdict (FAIL / FLAG_FOR_CODEX / PASS) is recorded in `docs/sprints/sprint-058-handoff.md` §X "Real-meta-agent calibration evidence" section. Acceptance: **FLAG rate <25%** (under `flag_for_codex_rate_warn_threshold`); **FP rate on clean prose = 0** (zero false-positive on clean-prose negative-control samples). The Fix-C step 2 decision is anchored to this evidence.

5. **Fix-C step 2 decision and final state recorded.** Based on the step 4 evidence:
   - **IF acceptance bar passes** (FLAG <25% + FP=0 on clean prose) → flip `synonym_map_enabled: false → true` in `autoloop/config.yaml` `anti_hardcode` block (final state).
   - **IF FP>0 on any clean prose sample** → retain `synonym_map_enabled: false` AND implement Fix-B FLAG rule (new `_RE_Q1_WHEN_ARROW_FLAG` regex emitting FLAG_FOR_CODEX on `\b(?:whenever|when)\b[\s\S]{1,120}?(?:→|=>|->)[\s\S]{1,120}` pattern; NOT auto-FAIL). The detector still structurally closes the Codex Axis B exact bypass through the Fix-B FLAG rule path.
   The decision + supporting evidence MUST be recorded in `docs/sprints/sprint-058-handoff.md` §X for per-sub-sprint Codex consumption.

6. **Per-sub-sprint Codex `pass` (§4.3 trigger #2).** Deliver-agent dispatches Codex at S-Auto-5 close (NOT at S-Auto-5 open) with `compact/sprint-058-codex-review-prompt.md` self-contained per §9 invariant. Codex must verify Fix-C step 1 + step 2 structural soundness, calibration evidence audit, ≥3 adversarial spot-checks, detector self-discipline regression test status. Codex must return `pass / 0` (or `approve with downgrade-to-signal follow-up`) BEFORE S-Auto-6 overnight starts.

**Zero touch** to `autoloop/autoloop/scoring/{tier_evaluator,eval_runner,baseline_loader,gaming}.py` (M-Auto-1B hard fence #13 — content hash locked against `5177b674b5ad249d7c0e706f3c827010c8dab511240f239dbd3be6f689a0331c`). **Zero touch** to `autoloop/autoloop/loop.py` / `autoloop/autoloop/meta_agent/**` / `autoloop/autoloop/memory/**` / `autoloop/autoloop/sandbox/{yaml_diff_validator,applier,content_validator}.py` / `autoloop/autoloop/cli.py` (S-Auto-1/2/3/4 deliverables — signature + body unchanged in S-Auto-5). **Zero touch** to `eval_interactive/eval_interactive/**`, case_spec, case_specs_shadow, `server/src/main/java/**`, `eval/src/main/java/**`, `data/`, `db/`, `server/src/main/resources/**` (the cherry-pick fence #7 of M-Auto-1B §6 allows ONE Skill YAML edit in S-Auto-6 only; S-Auto-5 ships zero `server/` edits). **Zero touch** to `docs/foundational/`, `docs/runtime_freeze_and_risk_policy.md`, `docs/current/`, sprint/milestone archives, `docs/codex-findings.md` scaffold (per-sub-sprint Codex writes at close).

## Scope (numbered; this is the contract)

### #1 — Build `server/` jar (live-iter prerequisite (a))

Run:

```bash
mvn package -pl server -am -DskipTests
```

(or equivalent fast-build command if the dev session has a faster path that produces the jar without sacrificing the jar artefact). Verify `ls server/target/*.jar` shows at least one `.jar` file. The applier's mvn alt-port Spring spawn (S-Auto-3 substrate) reads from this artefact path; without it, live-iter cannot start.

**If `mvn package` fails** for any reason (dependency issue, compilation error, disk space): STOP and surface to deliver-agent + human. Do NOT bypass with `-DskipTests` blanket, do NOT pin a dependency version, do NOT delete + re-clone the repo. Root-cause investigation first.

### #2 — Verify live-iter prerequisites (b) + (c)

Run:

```bash
cd autoloop && uv run --extra dev python -m autoloop check
```

Confirm the output reports `AUTOLOOP_META_LLM_API_KEY` is set (from `autoloop/.env.local`). The exact output shape comes from S-Auto-3's CLI; expected format includes a line confirming the env-var presence and provider identity.

Confirm `git status` returns clean (no uncommitted modifications) BEFORE the next step. The applier expects a clean working tree to commit on a new `autoloop/exp-1` branch.

**If `python -m autoloop check` reports a problem** (env-var missing despite `.env.local` presence; provider unreachable; ...): STOP and surface to deliver-agent + human; do NOT manually edit `autoloop/.env.local` without surfacing context first.

### #3 — Run the first live iteration

Run:

```bash
cd autoloop && uv run --extra dev python -m autoloop run --experiments 1
```

(NO `--dry-run` flag.) The full 14-step state machine must execute:

1. propose (meta-agent LLM call: analyze → propose)
2. content_validator (S-Auto-4 surface; PASS or FAIL → discard)
3. sandbox YAML diff validate (S-Auto-1 surface; ACCEPT or REJECT → discard)
4. anti_hardcode_check (S-Auto-4 surface; PASS / FAIL / FLAG_FOR_CODEX; FAIL → discard, FLAG → continue + flag)
5. applier prepare branch (S-Auto-3 surface; create `autoloop/exp-1`)
6. applier write YAML patch
7. applier git commit on exp branch
8. applier mvn compile on exp branch (or fast cache hit since jar exists from #1)
9. applier Spring spawn on alt port + actuator health probe (120s timeout)
10. eval_runner three v1 suites in sequence: bad_cases (parallel=1) → anchor_outcome (parallel=4) → shadow (parallel=4) — total 47 cases
11. tier_evaluator lexicographic verdict (Layer 0 → 1 → 2 → 3 → 4)
12. gaming.detect (post-eval observation-only flags)
13. memory writes: experiments_log append + iterations_index insert + (no lessons compaction unless K=10 reached, which won't happen here)
14. applier cleanup (SIGTERM / SIGKILL Spring; branch tag)

Record:
- Iteration outcome (keep / discard / error; if discard, the `discard_reason`; if error, the exception class + stage).
- Per-iteration wall-clock elapsed time.
- `autoloop/results/runs/exp-1/` directory contents: `hypothesis.json` (meta-agent proposed hypothesis) + `diff.yaml` (proposed YAML diff) + per-stage verdict artefacts (sandbox_verdict.json, anti_hardcode_verdict.json, etc.).

**Stop conditions during this step**:

- Iteration crashes in a way the S-Auto-3 crash-recovery substrate does NOT handle (e.g., a path-not-tested-during-S-Auto-3-integration-test): STOP, capture full stack trace, surface to deliver-agent + human. Do NOT patch loop.py mid-sub-sprint (hard fence: zero touch to loop.py).
- Per-iteration elapsed time >40 min: continue this iteration to completion if possible, but STOP additional iterations and surface as observation toward milestone §10 stop condition consideration.
- Total errors during the first 1 iteration (= 100% error rate at N=1): STOP, surface to deliver-agent + human; investigate (LLM API, infra).

### #4 — Run 2-3 additional live iterations (conditional)

**IF the first iteration completed <15 min** (well within budget) AND total iteration count so far <3 AND propose-sample count <10, run 2-3 more iterations to accumulate ≥10 real meta-agent propose samples:

```bash
cd autoloop && uv run --extra dev python -m autoloop run --experiments 2
```

(Adjust `--experiments` to bring cumulative total to 3.) Each additional iteration appends to `autoloop/results/experiments.jsonl` + `iterations.sqlite`.

After all live iterations are done, extract the ≥10 propose samples from `experiments.jsonl` — specifically `hypothesis.proposed_value` (the candidate `procedure` / `grounding_instruction` / `escalation_policy` / `critical_steps[].desc` text) for each iteration. Save these as a sanitized JSON fixture at `autoloop/tests/fixtures/real_meta_agent_calibration_samples.json`:

```json
{
  "samples": [
    {
      "iter_id": "exp-1",
      "target_skill": "...",
      "target_field": "...",
      "proposed_value": "...",
      "captured_date": "2026-MM-DD"
    },
    ...
  ]
}
```

**Sanitization**: strip any literal eval `case_id` / `session_id` / `cs<id>` substring tokens (the calibration fixture must NOT itself encode eval phrases — detector self-discipline carries forward). If a sample contains such a token, replace with a redacted placeholder (`<REDACTED_CASE_ID>`) and note the redaction in the JSON record. If sanitization makes the sample meaningless, drop it and supplement with another iteration.

**If after 3 iterations** the propose-sample count is still <10 (because the meta-agent proposed identical / near-identical hypotheses across iterations, or because some were discarded before reaching `experiments.jsonl`): STOP and surface to deliver-agent + human. Possible mitigation: deliver-agent + human jointly compose supplementary calibration samples from the §1.7 forbidden examples + the M-Auto-1A close-day drift cases, but this is a planning-round decision NOT a dev-side call.

### #5 — Implement Fix-C step 1

Edit `autoloop/autoloop/sandbox/anti_hardcode_check.py`. The current `_normalize()` (HEAD `b6b627b` lines ~85-105) shape is:

```python
def _normalize(text: str, *, synonym_map_enabled: bool) -> str:
    norm = unicodedata.normalize("NFKC", text).lower()
    norm = _RE_WHITESPACE.sub(" ", norm)
    if synonym_map_enabled:
        for src, dst in _SYNONYM_MAP.items():
            norm = norm.replace(src, dst)
    return norm
```

Modify to insert the word-boundary regex substitution BEFORE the `_SYNONYM_MAP` whole-string substitution, ALSO guarded by `synonym_map_enabled`:

```python
def _normalize(text: str, *, synonym_map_enabled: bool) -> str:
    norm = unicodedata.normalize("NFKC", text).lower()
    norm = _RE_WHITESPACE.sub(" ", norm)
    if synonym_map_enabled:
        norm = _RE_WHEN_WORD_BOUNDARY.sub("if", norm)
        for src, dst in _SYNONYM_MAP.items():
            norm = norm.replace(src, dst)
    return norm
```

Add the new regex constant near the existing `_SYNONYM_MAP` / `_RE_*` constants:

```python
_RE_WHEN_WORD_BOUNDARY = re.compile(r"\b(?:whenever|when)\b")
```

**No other behaviour change** in this step. `_SYNONYM_MAP` keys are NOT modified. The 11 existing rule functions are NOT modified. Rule count stays at the M-Auto-1A close value (11; well under the ≤30 cap).

Verify locally:

```bash
cd autoloop && uv run --extra dev pytest -q autoloop/tests/test_anti_hardcode_check.py
```

The existing 17-fixture calibration sweep must continue to produce 11/11 forbidden FAIL + 4/4 clean PASS + 2/2 borderline FLAG_FOR_CODEX. The 3 detector self-discipline regression tests must continue to PASS. If ANY existing test fails after Fix-C step 1, STOP and re-design (Fix-C step 1 should be additive — failing an existing test means a side effect on a rule the deliver-agent didn't intend).

Add a fresh regression test in `autoloop/tests/test_anti_hardcode_check.py` for the Codex Axis B exact bypass:

```python
def test_fix_c_step1_codex_axis_b_bypass_now_fails():
    """R-S57: Codex Axis B `Whenever ... =>` previously PASS-bypassed; Fix-C step 1 should FAIL."""
    hypothesis = make_hypothesis(
        after_value="Whenever the customer describes an appeal => route to escalation and skip normal triage.",
    )
    config = make_config(synonym_map_enabled=True)
    result = anti_hardcode_check(hypothesis, config=config)
    assert result.verdict == "FAIL", f"Expected FAIL after Fix-C step 1; got {result.verdict}"
    assert result.rule_id.startswith("Q1."), f"Expected Q1 family rule; got {result.rule_id}"
```

Add ≥3 clean-prose negative-control tests to guard against false-positive on common "when" usage:

```python
def test_fix_c_step1_clean_prose_when_user_describes_still_passes():
    """Fix-C step 1 must not false-positive on common 'when the user' clean prose."""
    hypothesis = make_hypothesis(
        after_value="When the user describes their issue, gather intake fields before proposing next steps.",
    )
    config = make_config(synonym_map_enabled=True)
    result = anti_hardcode_check(hypothesis, config=config)
    assert result.verdict == "PASS", f"Expected PASS on clean 'when' prose; got {result.verdict} ({result.rule_id})"

def test_fix_c_step1_clean_prose_whenever_subordinate_clause_still_passes():
    """'Whenever' as a subordinate clause introducing context (not an if-then) must PASS."""
    hypothesis = make_hypothesis(
        after_value="Whenever possible, prefer concrete examples over abstract policy text in the response.",
    )
    config = make_config(synonym_map_enabled=True)
    result = anti_hardcode_check(hypothesis, config=config)
    assert result.verdict == "PASS", f"Expected PASS on subordinate 'whenever' clause; got {result.verdict} ({result.rule_id})"

def test_fix_c_step1_multi_line_when_arrow_decomposition_fails():
    """Multi-line 'When ...\\n=> ...' decomposition must FAIL (Q1 regex spans newlines)."""
    hypothesis = make_hypothesis(
        after_value="When the customer mentions a refund\n=> escalate to UC-H without further triage.",
    )
    config = make_config(synonym_map_enabled=True)
    result = anti_hardcode_check(hypothesis, config=config)
    assert result.verdict == "FAIL", f"Expected FAIL on multi-line 'When => '; got {result.verdict}"
```

(`make_hypothesis` + `make_config` are existing test helpers in `test_anti_hardcode_check.py`; reuse the existing patterns rather than inventing new ones.)

### #6 — Real-meta-agent batch calibration

Write a small helper (or run interactively via pytest fixture or a script under `autoloop/scripts/` if helper-scripts convention exists; otherwise put it inline in a dedicated test file `autoloop/tests/test_real_meta_agent_calibration.py`) that:

1. Loads the samples from `autoloop/tests/fixtures/real_meta_agent_calibration_samples.json`.
2. For each sample, constructs a `Hypothesis` (use existing `make_hypothesis` helper pattern).
3. Runs `anti_hardcode_check(hyp, config=make_config(synonym_map_enabled=True))` for each.
4. Records per-sample verdict (FAIL / FLAG_FOR_CODEX / PASS) + rule_id + matched_substring (if applicable).
5. Computes aggregate FAIL count + FLAG count + PASS count + FLAG rate (`FLAG / total`).
6. Asserts FLAG rate <25% (the existing `flag_for_codex_rate_warn_threshold`).
7. Asserts FP rate on clean-prose samples = 0 (clean prose = samples the deliver-agent + dev jointly classify as "should-PASS" prior to calibration; if no clean-prose samples exist among the ≥10 real-meta-agent set, supplement with the existing 4 clean fixtures from the 17-fixture table).

Run:

```bash
cd autoloop && uv run --extra dev pytest -q autoloop/tests/test_real_meta_agent_calibration.py
```

Record the aggregate counts + per-sample evidence table in `docs/sprints/sprint-058-handoff.md` §X "Real-meta-agent calibration evidence" section. The table format:

| sample_id | target_field | proposed_value (first 80 chars + ...) | verdict | rule_id | clean_prose_label |
|---|---|---|---|---|---|
| exp-1.proposed_value | ... | ... | PASS / FAIL / FLAG | ... | clean / forbidden / borderline |

**If FLAG rate ≥25%** OR **FP rate >0 on clean prose**: do NOT flip `synonym_map_enabled` in #7; the Fix-C step 2 decision moves to the Fix-B FLAG rule fallback path (see #7 below).

### #7 — Fix-C step 2: decision + final state

Based on the #6 evidence, choose ONE of two final states:

**Path A — Acceptance bar passes** (FLAG rate <25% + FP=0 on clean prose):

Edit `autoloop/config.yaml` `anti_hardcode` block:

```yaml
anti_hardcode:
  enabled: true
  synonym_map_enabled: true             # Fix-C step 2 Path A: flipped after S-Auto-5 calibration evidence;
                                        # see docs/sprints/sprint-058-handoff.md §X for per-sample evidence
  flag_for_codex_rate_warn_threshold: 0.25
```

No additional rule added.

**Path B — Acceptance bar fails on FP>0 clean prose**:

Retain `synonym_map_enabled: false`. Add a new FLAG rule in `autoloop/autoloop/sandbox/anti_hardcode_check.py`:

```python
_RE_Q1_WHEN_ARROW_FLAG = re.compile(
    r"\b(?:whenever|when)\b[\s\S]{1,120}?(?:→|=>|->)[\s\S]{1,120}",
    re.IGNORECASE,
)

def _q1_when_arrow_flag(text: str) -> tuple[str, str] | None:
    """Fix-C step 2 Path B: FLAG (not FAIL) on `\\b(?:whenever|when)\\b ... =>` shape.
    Detector emits FLAG_FOR_CODEX so Codex / human judges; not auto-FAIL because
    'when' is English-common and `synonym_map_enabled=false` keeps the FAIL gate
    narrow to the Q1 if-then regex."""
    m = _RE_Q1_WHEN_ARROW_FLAG.search(text)
    if m:
        return (_trim(m.group(0)), _FLAG)
    return None
```

Register the new rule in the existing rule list under the `Q1` category, with rule_id `Q1.when_arrow_flag` (alphabetically sorted in the rule list to maintain deterministic evaluation order). Add corresponding test cases in `test_anti_hardcode_check.py`:

```python
def test_fix_c_step2b_when_arrow_flag_on_codex_axis_b_bypass():
    """Path B: Codex Axis B `Whenever ... =>` triggers FLAG_FOR_CODEX (not FAIL)."""
    hypothesis = make_hypothesis(
        after_value="Whenever the customer describes an appeal => route to escalation and skip normal triage.",
    )
    config = make_config(synonym_map_enabled=False)
    result = anti_hardcode_check(hypothesis, config=config)
    assert result.verdict == "FLAG_FOR_CODEX"
    assert result.rule_id == "Q1.when_arrow_flag"

def test_fix_c_step2b_when_arrow_flag_does_not_fire_on_clean_prose():
    """Path B: 'when the user describes' clean prose does NOT trigger Q1.when_arrow_flag (no arrow token)."""
    hypothesis = make_hypothesis(
        after_value="When the user describes their issue, gather intake fields before proposing next steps.",
    )
    config = make_config(synonym_map_enabled=False)
    result = anti_hardcode_check(hypothesis, config=config)
    assert result.verdict == "PASS"
```

Rule count grows from 11 to 12 (still well under the ≤30 cap). Update the rule_count assertion in `test_detector_source_rule_count_bounded` if it's hardcoded to 11 (the M-Auto-1A close test used `<=30` so likely already passes; verify).

**Whichever path is chosen**, record the decision + reasoning + per-sample evidence in `docs/sprints/sprint-058-handoff.md` §X. The handoff MUST clearly state: "Fix-C step 2 final state = Path A: `synonym_map_enabled=true`" OR "Fix-C step 2 final state = Path B: `synonym_map_enabled=false` + new `Q1.when_arrow_flag` FLAG rule".

### #8 — Tests (~6-12 NEW; expected total 216 → 222-228)

Reproducibility: the actual new test count depends on whether Path A or Path B is chosen at #7.

**Path A new tests** (≥6):
- `test_fix_c_step1_codex_axis_b_bypass_now_fails` (#5)
- `test_fix_c_step1_clean_prose_when_user_describes_still_passes` (#5)
- `test_fix_c_step1_clean_prose_whenever_subordinate_clause_still_passes` (#5)
- `test_fix_c_step1_multi_line_when_arrow_decomposition_fails` (#5)
- `test_real_meta_agent_calibration_flag_rate_under_threshold` (#6; runs the calibration over the ≥10 sample fixture and asserts FLAG <25%)
- `test_real_meta_agent_calibration_fp_zero_on_clean_prose` (#6; asserts FP=0 on the clean-prose subset)

**Path B new tests** (≥8): all of Path A plus:
- `test_fix_c_step2b_when_arrow_flag_on_codex_axis_b_bypass` (#7 Path B)
- `test_fix_c_step2b_when_arrow_flag_does_not_fire_on_clean_prose` (#7 Path B)

### #9 — `autoloop/program.md` — no edit required

The `autoloop/program.md` §4 status table row 3 currently reads `S-Auto-4 — DELIVERED (deterministic regex+heuristic detector ...)`. S-Auto-5 does NOT flip a new status row; the calibration is a refinement of the existing detector, not a new feature row. If `program.md` ever needs an §4 update for M-Auto-1B calibration, that's a deliver-agent decision at S-Auto-5 close, NOT a dev-side edit during the sub-sprint.

## Hard fences / STOP conditions

- **Zero touch** to `autoloop/autoloop/scoring/{tier_evaluator,eval_runner,baseline_loader,gaming}.py` — these four files are content-hash locked at `5177b674b5ad249d7c0e706f3c827010c8dab511240f239dbd3be6f689a0331c` per M-Auto-1B §6 fence #13; any edit would trigger `gaming.scoring_code_drift.sha_changed` ERROR.
- **Zero touch** to `autoloop/autoloop/loop.py` / `autoloop/autoloop/meta_agent/**` / `autoloop/autoloop/memory/**` / `autoloop/autoloop/cli.py` / `autoloop/autoloop/sandbox/{yaml_diff_validator,applier,content_validator}.py` — these are the S-Auto-1/2/3/4 deliverable substrate; S-Auto-5 consumes them, does NOT modify them.
- **Zero touch** to `eval_interactive/eval_interactive/**`, case_spec, case_specs_shadow, `server/src/main/java/**`, `eval/src/main/java/**`, `data/`, `db/`, `server/src/main/resources/**`, `docs/foundational/`, `docs/runtime_freeze_and_risk_policy.md`, `docs/current/`, sprint/milestone archives, `docs/codex-findings.md`.
- **No new heavy deps** in `autoloop/pyproject.toml` — stdlib `re` + `unicodedata` only (continues M-Auto-1A baseline). If a dep is needed, STOP and surface to deliver-agent BEFORE adding.
- **No LLM call** inside `anti_hardcode_check` or its tests (the detector remains deterministic per D1).
- **No semantic hardcode IN the detector itself** — Fix-C step 1 word-boundary regex `\b(?:whenever|when)\b` is a generic structural pattern (D2 compliant); optional Fix-B step 2 `_RE_Q1_WHEN_ARROW_FLAG` is similarly generic structural. The Codex Axis B exact phrase, any eval case_id, any user utterance, any expected answer, any case-status label MUST NOT appear in any detector rule (only in test fixtures for assertion targets).
- **No hardcoding** of `synonym_map_enabled` toggle flip without the #6 calibration evidence (the toggle final state is evidence-driven, not intent-driven).
- **No `git add -A`** by dev — stage only S-Auto-5 scope files explicitly: the modified `anti_hardcode_check.py` + `config.yaml` + new test files + `experiments.jsonl` / `iterations.sqlite` updates from live-iter + the new calibration fixture + `docs/sprints/sprint-058-handoff.md`. Deliver-agent close-bundle artefacts (this objective file, the milestone objective, Codex prompt + findings, 10-handoff updates, action_bank R-item annotations) bundled by the human at close.
- **STOP and surface** if Fix-C step 1 implementation causes ANY existing 17-fixture test to fail (Fix-C step 1 should be additive; failure means a side effect on a rule the deliver-agent didn't intend).
- **STOP and surface** if real-meta-agent batch produces <10 propose samples after 3 iterations (the meta-agent may be proposing near-identical hypotheses; root-cause investigation needed before deliver-agent + human jointly compose supplementary samples).
- **STOP and surface** if Codex per-sub-sprint review returns `reject as semantic hardcode` on Fix-C step 1 or step 2 — that triggers a fix-iteration sub-sprint S-Auto-5.1 BEFORE S-Auto-6 can begin.
- **STOP and surface** if per-iteration elapsed time >40 min (M-Auto-1B §10 stop condition consideration).
- **STOP and surface** if `mvn package` fails OR `python -m autoloop check` fails OR live-iter crashes in an unhandled path.

## Test / eval requirements

- **Python autoloop suite**: `cd autoloop && uv run --extra dev pytest -q` — M-Auto-1A close baseline `216 passed, 1 warning` MUST grow by 6-12 NEW S-Auto-5 tests (Path A: ~6; Path B: ~8; possibly +2 if dev adds extra adversarial fixtures). Total `222-228 passed, 1 warning`. The 1 warning is the S-Auto-2 baseline-missing-shadow asserted behaviour, unchanged.
- **Existing Python eval_interactive suite UNCHANGED**: `cd eval_interactive && uv run python -m pytest --tb=no -q` reproduces `486 passed, 3 failed` (failures are env-specific per OQ-S47.3).
- **Java baseline UNCHANGED**: skipped per S-Auto-5 Java-zero-touch (verify `git diff --stat -- server/src/main/java/ eval/src/main/java/` against the S-Auto-5 dev commit returns empty).
- **17-fixture calibration sweep**: 11/11 forbidden FAIL + 4/4 clean PASS + 2/2 borderline FLAG_FOR_CODEX UNCHANGED after Fix-C step 1.
- **Detector self-discipline regression tests**: 3 tests (`test_detector_source_does_not_hardcode_eval_case_ids` + `test_detector_source_does_not_hardcode_user_utterance_literals` + `test_detector_source_rule_count_bounded`) UNCHANGED — PASS.
- **Real-meta-agent batch calibration**: ≥10 samples; FLAG rate <25%; FP=0 on clean prose (with the chosen `synonym_map_enabled` setting from #7).
- **Live-iter end-to-end**: 1-3 real iterations completed without unhandled crash; per-iteration elapsed time recorded; `autoloop/results/runs/exp-N/` contents present.
- **No live LLM call in pytest tests** (continues M-Auto-1A baseline) — calibration tests use sanitized fixture file, NOT a fresh LLM call.

## §7 — Layer-classification + anti-hardcode stanza

**Target failure layer:** `infra` (primary; §3.2 Q1 — live-iter prerequisite + first real iteration are infrastructure execution + observation) + `eval_spec` (calibration; §3.2 Q6 — Fix-C step 1 + optional step 2 are structural refinements of the meta-agent output boundary detector). The sub-sprint does NOT change runtime / projection / scoring / CaseSpec / judge / smoke / shadow firewall posture.

**Tier-0 invariant:** adds no Tier-0 invariant. The Fix-C step 1 word-boundary regex + optional Fix-B step 2 FLAG rule are meta-loop infrastructure refinements, NOT a runtime invariant per `docs/runtime_freeze_and_risk_policy.md` §1/§2. The detector itself DETECTS attempts to invent new Tier-0 semantics (existing Q2 rule); Fix-C does not alter this Q2 detection behaviour.

**Semantic hardcode:** No semantic hardcode introduced. Justification by surface:

- **Fix-C step 1 word-boundary regex** `r"\b(?:whenever|when)\b"` is a **generic structural pattern** (per D2 detector self-discipline) — matches the two English-language words "whenever" and "when" as word-boundary tokens. The regex does NOT enumerate specific eval phrases / user utterances / expected answers / case-status labels. The detector self-discipline regression tests (`test_detector_source_does_not_hardcode_eval_case_ids` + `..._user_utterance_literals`) continue to grep-PASS (no literal `cs<id>` / `closure_criterion` / etc. tokens in the rule source).
- **Optional Fix-B step 2 FLAG rule** `_RE_Q1_WHEN_ARROW_FLAG` is similarly a **generic structural pattern** — matches the structural shape `\b(?:whenever|when)\b ... (?:→|=>|->) ...` as a FLAG (NOT auto-FAIL) signal. The regex encodes a generic IF/WHEN ... THEN arrow-tree shape, NOT specific content.
- **Calibration evidence-driven `synonym_map_enabled` toggle** is a config-level decision NOT a hardcode — flipping `false → true` is enabling a feature flag whose semantics are already in the codebase from S-Auto-4. The decision is evidence-driven (≥10 real-meta-agent samples + 17-fixture sweep both pass before flipping).
- **Real-meta-agent calibration fixture** at `autoloop/tests/fixtures/real_meta_agent_calibration_samples.json` is a **test data file** containing sanitized meta-agent propose outputs. Sanitization removes any literal `case_id` / `session_id` / user-utterance tokens before commit. The fixture is consumed by `test_real_meta_agent_calibration.py` for regression purposes (asserting the detector verdict against the sanitized samples remains stable across future detector edits).

**Generalization coverage:**

- **target** = (i) live-iter pipeline end-to-end on `auto-loop-branch` against a real meta-agent LLM (regardless keep / discard / error verdict); (ii) R-S57 detector calibration: 17-fixture sweep maintains 11/11 + 4/4 + 2/2 + real-meta-agent batch FLAG rate <25% + FP=0 on clean prose + Codex Axis B exact bypass now FAILs the detector.
- **neighbor** = R-S57 adversarial variants surfaced by Codex per-sub-sprint review (unicode obfuscation of "whenever" e.g., `ｗhenever`; multi-line decomposition `When ...\n=> ...`; semantic synonym swap "anytime ... ->"; logically-equivalent shapes "Given X happens, then output Y"). Detector must reject (or FLAG, depending on Path A / B) each.
- **negative-control** = 17-fixture clean PASS cases (4 cases) + ≥3 real-meta-agent clean prose samples ("when the user describes their issue, ..." / "whenever possible, prefer concrete examples ..." / similar). MUST NOT false-positive after Fix-C step 1.
- **shadow** = the shadow firewall posture is unchanged by S-Auto-5 (no detector-side change to `gaming.shadow_set_leakage` rule; the `shadow_leak_signatures` config list stays seeded with `["cs59s", "shadow_case_id", "shadow/", "case_specs_shadow"]` from M-Auto-1A). S-Auto-6 overnight is where shadow accumulates evidence.

## Codex review plan (§4.3)

**PER-SUB-SPRINT REQUIRED — §4.3 trigger #2**. The Fix-C step 1 detector change + Fix-C step 2 decision touch the §1.7 structural guard. Codex must independently verify Fix-C structural soundness + calibration evidence is sound BEFORE S-Auto-6 begins overnight running it against real propose outputs.

**Codex prompt timing**: deliver-agent authors `compact/sprint-058-codex-review-prompt.md` at S-Auto-5 **close** (NOT at open), covering the actual delivered commit range.

**Codex must verify**:

1. §4.1 nine-question kernel walk against the modified detector source (`anti_hardcode_check.py` + the new `_RE_WHEN_WORD_BOUNDARY` constant + optional new `_RE_Q1_WHEN_ARROW_FLAG` + `_q1_when_arrow_flag`): does Fix-C step 1 or step 2 encode any §1.7-violating decision logic? (D2 regression — the new regex must remain generic structural; no specific eval phrase / user utterance / answer / label.)
2. The detector catches the Codex Axis B exact bypass after Fix-C step 1 (FAIL on `synonym_map_enabled=true` Path A; FLAG_FOR_CODEX on `synonym_map_enabled=false` + new FLAG rule Path B).
3. ≥3 NEW bypass spot-checks (unicode obfuscation, multi-line decomposition, semantic synonym swap "anytime" / "as soon as" / German / Spanish translation). Detector must reject or FLAG each correctly; if a residual bypass surfaces, Codex returns `approve with downgrade-to-signal follow-up` with the new R-item recommendation.
4. Real-meta-agent calibration evidence audit: read the ≥10-sample fixture + the per-sample verdict table in `docs/sprints/sprint-058-handoff.md` §X "Real-meta-agent calibration evidence"; verify FLAG rate <25% + FP=0 on clean prose; verify the Path A / Path B decision rationale matches the evidence (no flip-without-justification).
5. Detector self-discipline 3 regression tests (`test_detector_source_does_not_hardcode_eval_case_ids` + `..._user_utterance_literals` + `..._rule_count_bounded`) PASS after Fix-C; rule count stays ≤30.
6. The 17-fixture calibration sweep still produces 11/11 + 4/4 + 2/2 after Fix-C step 1 (no side effect on existing rules).
7. Live-iter end-to-end evidence (Sprint 058 handoff §X "Live-iter end-to-end record") shows the prerequisite + first real iteration + per-iteration elapsed time + any error/discard outcomes are documented.

**Verdict expected**: `pass / 0` or `approve with downgrade-to-signal follow-up`. Must return BEFORE S-Auto-6 overnight starts. `reject as semantic hardcode` triggers fix-iteration sub-sprint S-Auto-5.1. Pre-mitigation: deliver-agent + dev jointly run all detector self-discipline regression tests + the 17-fixture sweep + the real-meta-agent calibration before commit.

## Handoff requirements

- Author `docs/sprints/sprint-058-handoff.md` at S-Auto-5 close; leave **§12** empty (deliver-agent + human at milestone close).
- Record in handoff:
  - `git show --numstat <s-auto-5-commit-sha>` for the sub-sprint commit.
  - Section "Live-iter end-to-end record": prereq verification (mvn package outcome, `python -m autoloop check` output summary, `git status` clean confirmation); per-iteration table (exp-1 ... exp-N with target_skill / target_field / verdict / discard_reason / elapsed_ms); cumulative propose-sample count.
  - Section "Real-meta-agent calibration evidence": the per-sample verdict table from #6 (≥10 rows); aggregate FAIL/FLAG/PASS counts; FLAG rate (under 25% expected); FP rate on clean prose (0 expected); whether Path A or Path B was chosen at #7; one-paragraph rationale linking the evidence to the decision.
  - Section "17-fixture sweep after Fix-C": the 17/17 outcome breakdown unchanged (11 FAIL + 4 PASS + 2 FLAG); the detector self-discipline 3 regression tests PASS status; rule count.
  - Section "Adversarial spot-check pre-Codex" (dev pre-mitigation): ≥3 adversarial constructions the dev tried locally and the detector verdict for each; this is a dry-run of what Codex will independently verify.
  - Section "§7 stanza self-walk": dev confirms the §7 stanza above against the actual delivered scope (any deviation flagged).
  - Section "OQ list" (OQ-S58.x candidates if surfaced).
- Author `compact/sprint-058-codex-review-prompt.md` AS PART OF the S-Auto-5 close-bundle (deliver-agent writes this; embeds §4.1 nine-question kernel verbatim + commit range + the 7 verification axes above + the §6 hard fences from `docs/milestone_objective.md` + §6.1 OQ-S56.1 disposition).

## Commit discipline

Dev stages **only S-Auto-5 scope** explicitly:

- Modified `autoloop/autoloop/sandbox/anti_hardcode_check.py` (Fix-C step 1; optional Fix-C step 2 Path B FLAG rule).
- Modified `autoloop/config.yaml` (Fix-C step 2 `synonym_map_enabled` final state).
- Modified `autoloop/tests/test_anti_hardcode_check.py` (≥4 new tests for Fix-C step 1; Path B: +2 tests for step 2 fallback rule).
- New `autoloop/tests/test_real_meta_agent_calibration.py` (the calibration sweep test).
- New `autoloop/tests/fixtures/real_meta_agent_calibration_samples.json` (sanitized ≥10-sample fixture).
- Updates to `autoloop/results/runs/exp-N/` + `autoloop/results/experiments.jsonl` + `autoloop/results/iterations.sqlite` from the live iterations (whether committed depends on `.gitignore` policy at `autoloop/results/`; if `.gitignore`'d, the live-iter evidence lives in the handoff record only — verify the existing convention).
- New `docs/sprints/sprint-058-handoff.md`.

**No `git add -A`**. Deliver-agent close-bundle files (this objective archive rename at close, milestone objective, codex prompt + findings, 10-handoff updates, action_bank R-item annotations) bundled by the human at close per `feedback_commit_at_end_bundles_deliver_artefacts.md`.

One commit at sub-sprint close (commit-at-end pattern). Commit message: `Sprint 058 / S-Auto-5 / M-Auto-1B — live-iter bootstrap + Fix-C step 1 + calibration evidence + step 2 final state`. (Replace the trailing fragment with the actual Path A or Path B decision; e.g., `... + step 2 Path A: synonym_map_enabled=true` OR `... + step 2 Path B: synonym_map_enabled=false + when_arrow_flag rule`.)

Commit footer: `Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>` per standard convention.

## Scope size (§8.5 note)

M-Auto-1B with S-Auto-5 = 1 of 2 sub-sprints planned; within §8.5 5-sub-sprint ceiling (margin = 3 if fix-iteration needed). S-Auto-5 is medium by LOC + test count + structural-defence importance; estimated 4-5 dev-days + Codex ~2 days. The Fix-C step 2 Path A / Path B branching adds conditional scope but each path is ≤2 hours dev work. No deferred scope (everything in scope is required to hit close gates).

## OQ (open questions — filled during the sub-sprint)

- **OQ-S58.x candidates** (expected; dev surfaces as ambiguities encountered):
  - Whether the meta-agent's propose distribution produces enough diverse samples in 1-3 iterations to constitute "≥10 real samples" (or if the meta-agent loops on similar hypotheses; surfaces if so).
  - Exact wall-clock per-iteration elapsed time vs. M-Auto-1B §9 estimate (12-25 min target; >40 min triggers stop-condition discussion).
  - Whether Path A or Path B was chosen at Fix-C step 2 and the per-sample evidence justification.
  - Whether any adversarial spot-check beyond the 3-dev-tried-locally surfaces (Codex will independently verify; OQ-S58 records dev's pre-mitigation attempts).
  - Whether `autoloop/results/` directory is `.gitignore`'d or committed (existing convention to verify; if `.gitignore`'d, live-iter evidence captured in handoff only).
  - Whether `make_hypothesis` and `make_config` test helpers in `test_anti_hardcode_check.py` support the new test patterns; surface if helper extensions are needed (additive, no signature break to existing tests).
