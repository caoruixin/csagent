# Sprint 058 / M-Auto-1B S-Auto-5 — Dev Prompt

## Role identity

你是 **dev agent for Sprint 058 / M-Auto-1B sub-sprint S-Auto-5 — Live-iter bootstrap + Detector calibration (Fix-C hybrid)**.

**One-sentence goal**: retire the M-Auto-1A live-iter waiver (OQ-S56.5) by completing 3 prerequisites + running 1-3 real iterations against a real meta-agent LLM, implement Fix-C step 1 (word-boundary regex `\b(?:whenever|when)\b` → `if` in `_normalize`) + decide Fix-C step 2 final state (`synonym_map_enabled` toggle) based on ≥10 real-meta-agent sample calibration evidence, and prepare the per-sub-sprint Codex review at S-Auto-5 close.

This prompt is your **self-contained executable view** per `iteration_governance.md` §9 invariant. You do NOT need to read any other repo doc (governance chain auto-loaded via `AGENTS.md`). Code anchors are cited inline as `path:line` references for you to read on demand.

## Read order

1. **`AGENTS.md`** (auto-loaded; do NOT re-read explicitly) — Constitution + doc_governance + agent_context_guide + iteration_governance.
2. **THIS prompt** — your full executable contract.
3. **Code anchors on demand** (only read what you need when a step references it):
   - `autoloop/autoloop/sandbox/anti_hardcode_check.py` — the file you'll modify (Fix-C step 1 + optional step 2 Path B).
   - `autoloop/config.yaml` `anti_hardcode` block — the file you'll modify (Fix-C step 2 toggle).
   - `autoloop/tests/test_anti_hardcode_check.py` — existing tests + helpers (`make_hypothesis`, `make_config`); you'll add ≥4 new tests (Path A) or ≥6 (Path B).
   - `autoloop/autoloop/cli.py` — `check` and `run` subcommands you'll invoke (do NOT modify).
   - `autoloop/autoloop/loop.py` — the 14-step state machine you'll observe (do NOT modify; hard-fenced).
   - `autoloop/.env.local` — should already contain `AUTOLOOP_META_LLM_API_KEY` from M-Auto-1A close; verify presence, do NOT modify without surfacing.

You do NOT read: `docs/sprint_objective.md`, `docs/milestone_objective.md`, `docs/teams/deliver-agent.md`, sprint archives, milestone archives, Codex prompt or findings, `docs/foundational/**`, `docs/runtime_freeze_and_risk_policy.md`, `docs/current/**`, `docs/solutions/**`. All required content is embedded below.

## Class

`infra` (primary) + `eval_spec` (calibration). **§7 REQUIRED**.

`infra` boundary (§3.2 Q1): the first half of the sub-sprint is build-infrastructure execution (`mvn package`) + live-iter prerequisite verification + first real meta-agent iteration — none is a runtime semantic change. `eval_spec` boundary (§3.2 Q6): the second half is structural refinement of the meta-agent output boundary detector (`anti_hardcode_check.py`) + evidence-driven config toggle decision — both touch the §1.7 anti-hardcode defender, hence §7 REQUIRED.

## Goal (the contract — verbatim from `docs/sprint_objective.md`)

By S-Auto-5 close:

1. **Live-iter prerequisites retired.** `mvn package -pl server -am -DskipTests` produces `server/target/*.jar`; `AUTOLOOP_META_LLM_API_KEY` confirmed live via `python -m autoloop check` returning success; working tree clean on `auto-loop-branch` immediately before the first live iteration.

2. **≥1 real iteration completed end-to-end.** `python -m autoloop run --experiments 1` (NO `--dry-run`) drives the full 14-step state machine to a verdict (keep / discard / error all acceptable). Per-iteration elapsed wall-clock time recorded; if first iter <15 min, run 2-3 more to accumulate ≥10 real meta-agent propose samples in `autoloop/results/experiments.jsonl`.

3. **Fix-C step 1 implemented.** `_normalize()` in `autoloop/autoloop/sandbox/anti_hardcode_check.py` extended with `re.sub(r"\b(?:whenever|when)\b", "if", norm)` guarded by `synonym_map_enabled`. Existing `_SYNONYM_MAP` UNCHANGED. 17-fixture sweep still produces 11/11 forbidden FAIL + 4/4 clean PASS + 2/2 FLAG_FOR_CODEX. Detector self-discipline 3 regression tests PASS. Rule count ≤30. Codex Axis B exact bypass `Whenever the customer describes an appeal => route to escalation and skip normal triage.` now FAILS the detector.

4. **Real-meta-agent batch calibration with ≥10 samples**: per-sample verdict (FAIL/FLAG/PASS) recorded; FLAG rate <25%; FP rate on clean prose = 0; evidence in `docs/sprints/sprint-058-handoff.md` §X.

5. **Fix-C step 2 decision**:
   - **Path A** (FLAG <25% + FP=0 on clean prose): flip `autoloop/config.yaml` `anti_hardcode.synonym_map_enabled: false → true` (final state).
   - **Path B** (FP>0 on any clean prose): retain `synonym_map_enabled: false` + add new `_RE_Q1_WHEN_ARROW_FLAG` regex + `_q1_when_arrow_flag` rule emitting FLAG_FOR_CODEX (NOT auto-FAIL) on `\b(?:whenever|when)\b ... =>` shape.
   Decision + rationale + per-sample evidence recorded in handoff.

6. **Per-sub-sprint Codex `pass`** (§4.3 trigger #2). Deliver-agent dispatches Codex at S-Auto-5 close with `compact/sprint-058-codex-review-prompt.md`; YOU do NOT dispatch Codex. Codex must return `pass / 0` (or `approve with downgrade-to-signal follow-up`) BEFORE S-Auto-6 overnight starts.

**Zero touch** to all hard-fenced surfaces (see Hard fences section below).

## Scope (numbered execution steps)

### #1 — Build `server/` jar (live-iter prerequisite (a))

```bash
mvn package -pl server -am -DskipTests
```

Verify `ls server/target/*.jar` shows at least one `.jar` file. If `mvn package` fails (dependency issue, compilation error, disk space): **STOP and surface to deliver-agent + human**. Do NOT bypass with `-DskipTests` blanket; do NOT pin a dependency version; do NOT delete + re-clone the repo. Root-cause investigation first.

### #2 — Verify live-iter prerequisites (b) + (c)

```bash
cd autoloop && uv run --extra dev python -m autoloop check
```

Confirm the output reports `AUTOLOOP_META_LLM_API_KEY` is set (from `autoloop/.env.local`). Then:

```bash
git status
```

Must return clean (no uncommitted modifications). If either check fails, **STOP and surface**; do NOT manually edit `autoloop/.env.local` without surfacing.

### #3 — Run the first live iteration

```bash
cd autoloop && uv run --extra dev python -m autoloop run --experiments 1
```

NO `--dry-run`. The full 14-step state machine executes:

1. propose (meta-agent LLM analyze + propose)
2. content_validator (S-Auto-4 surface)
3. sandbox YAML diff validate (S-Auto-1 surface)
4. anti_hardcode_check (S-Auto-4 surface)
5. applier prepare branch (create `autoloop/exp-1`)
6. applier write YAML patch
7. applier git commit on exp branch
8. applier mvn compile (fast cache hit since #1 built jar)
9. applier Spring spawn on alt port + actuator health probe (120s timeout)
10. eval_runner three v1 suites: bad_cases (parallel=1) → anchor_outcome (parallel=4) → shadow (parallel=4); total 47 cases
11. tier_evaluator lexicographic verdict (Layer 0 → 4)
12. gaming.detect (post-eval, observation-only flags)
13. memory writes: experiments_log append + iterations_index insert
14. applier cleanup (SIGTERM/SIGKILL Spring; branch tag)

**Record**:
- Iteration outcome (keep / discard / error; if discard, the `discard_reason`; if error, the exception class + stage).
- Per-iteration wall-clock elapsed time.
- `autoloop/results/runs/exp-1/` contents: `hypothesis.json`, `diff.yaml`, per-stage verdict artefacts.

**Stop conditions in this step**:
- Iteration crashes in a way the S-Auto-3 crash-recovery substrate does NOT handle: STOP, capture full stack trace, surface. Do NOT patch `loop.py` mid-sub-sprint (hard fence).
- Per-iteration elapsed time >40 min: complete this iteration if possible, but STOP further iterations and surface.
- Total errors at N=1 = 100% error rate: STOP and surface.

### #4 — Run 2-3 additional live iterations (conditional)

IF first iteration <15 min AND total iter count <3 AND propose-sample count <10, run 2-3 more:

```bash
cd autoloop && uv run --extra dev python -m autoloop run --experiments 2
```

After all live iterations, extract `hypothesis.proposed_value` for each iteration from `experiments.jsonl` and save as a sanitized fixture at:

`autoloop/tests/fixtures/real_meta_agent_calibration_samples.json`

```json
{
  "samples": [
    {
      "iter_id": "exp-1",
      "target_skill": "...",
      "target_field": "procedure | grounding_instruction | escalation_policy | critical_steps[N].desc",
      "proposed_value": "...",
      "captured_date": "2026-MM-DD",
      "clean_prose_label": "clean | forbidden | borderline"
    },
    ...
  ]
}
```

**Sanitization**: strip any literal `case_id` / `session_id` / `cs<id>` substring tokens (the calibration fixture must NOT itself encode eval phrases — detector self-discipline carries forward). Replace with `<REDACTED_CASE_ID>` and note the redaction. If sanitization makes a sample meaningless, drop it and supplement with another iteration.

**If after 3 iterations propose-sample count <10** (e.g., meta-agent looped on similar hypotheses): **STOP and surface** to deliver-agent + human. Do NOT compose synthetic samples unilaterally — that's a planning-round joint call.

### #5 — Implement Fix-C step 1

Read `autoloop/autoloop/sandbox/anti_hardcode_check.py` first. The current `_normalize()` (around HEAD lines 85-105) shape is:

```python
def _normalize(text: str, *, synonym_map_enabled: bool) -> str:
    norm = unicodedata.normalize("NFKC", text).lower()
    norm = _RE_WHITESPACE.sub(" ", norm)
    if synonym_map_enabled:
        for src, dst in _SYNONYM_MAP.items():
            norm = norm.replace(src, dst)
    return norm
```

Modify by inserting a word-boundary regex substitution BEFORE the existing `_SYNONYM_MAP` whole-string substitution, ALSO guarded by `synonym_map_enabled`:

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

Add the new constant near the existing `_SYNONYM_MAP` / `_RE_*` constants (NOT inside `_normalize`):

```python
_RE_WHEN_WORD_BOUNDARY = re.compile(r"\b(?:whenever|when)\b")
```

**No other behaviour change** in this step. `_SYNONYM_MAP` keys NOT modified. The 11 existing rule functions NOT modified. Rule count stays at 11 (well under ≤30 cap).

**Verify locally**:

```bash
cd autoloop && uv run --extra dev pytest -q autoloop/tests/test_anti_hardcode_check.py
```

The existing 17-fixture sweep must produce 11/11 + 4/4 + 2/2 unchanged. The 3 detector self-discipline regression tests must PASS. **If ANY existing test fails after Fix-C step 1, STOP and re-design** — step 1 should be additive; failure means an unintended side effect.

Add new regression tests in `autoloop/tests/test_anti_hardcode_check.py` (reuse existing `make_hypothesis` + `make_config` helpers; read the file first to understand the helper pattern):

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

def test_fix_c_step1_clean_prose_when_user_describes_still_passes():
    """Fix-C step 1 must not false-positive on common 'when the user' clean prose."""
    hypothesis = make_hypothesis(
        after_value="When the user describes their issue, gather intake fields before proposing next steps.",
    )
    config = make_config(synonym_map_enabled=True)
    result = anti_hardcode_check(hypothesis, config=config)
    assert result.verdict == "PASS", f"Expected PASS on clean 'when' prose; got {result.verdict} ({result.rule_id})"

def test_fix_c_step1_clean_prose_whenever_subordinate_clause_still_passes():
    """'Whenever' as a subordinate clause introducing context must PASS."""
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

(Adjust assertion details to match the actual `make_hypothesis` / `make_config` helper signatures; do NOT change the helpers.)

### #6 — Real-meta-agent batch calibration

Create `autoloop/tests/test_real_meta_agent_calibration.py`:

```python
"""S-Auto-5 / M-Auto-1B: real-meta-agent batch calibration of the anti-hardcode detector
against ≥10 sanitized propose samples captured in step 4. Asserts FLAG rate <25% and
FP rate on clean prose = 0 (after Fix-C step 1; with synonym_map_enabled=True for the
measurement)."""

import json
from pathlib import Path

# Reuse the existing test helpers from test_anti_hardcode_check.py if accessible
# (e.g., via a fixtures module); otherwise import the production API directly.

FIXTURE_PATH = Path(__file__).parent / "fixtures" / "real_meta_agent_calibration_samples.json"

def _load_samples():
    with FIXTURE_PATH.open() as f:
        return json.load(f)["samples"]

def test_real_meta_agent_calibration_sample_count():
    """The fixture must contain ≥10 sanitized real-meta-agent propose samples."""
    samples = _load_samples()
    assert len(samples) >= 10, f"Expected ≥10 samples; got {len(samples)}"

def test_real_meta_agent_calibration_flag_rate_under_threshold():
    """FLAG rate across the sample set must be <25% (anti_hardcode.flag_for_codex_rate_warn_threshold)."""
    samples = _load_samples()
    # ... classify each sample through anti_hardcode_check with synonym_map_enabled=True;
    # count FLAG / FAIL / PASS; assert FLAG/len(samples) < 0.25.
    # Use make_hypothesis + make_config helpers from test_anti_hardcode_check.py.
    ...

def test_real_meta_agent_calibration_fp_zero_on_clean_prose():
    """FP rate on samples labelled clean_prose_label='clean' must be 0 (zero FAIL on clean prose)."""
    samples = _load_samples()
    clean = [s for s in samples if s.get("clean_prose_label") == "clean"]
    # For each clean sample, classify; assert verdict != "FAIL".
    ...
```

Complete the test bodies based on the actual helper signatures in `test_anti_hardcode_check.py`. Run:

```bash
cd autoloop && uv run --extra dev pytest -q autoloop/tests/test_real_meta_agent_calibration.py
```

Record per-sample verdict + aggregate counts in handoff §X "Real-meta-agent calibration evidence" as a table.

**If FLAG rate ≥25% OR FP>0 on clean prose**: Fix-C step 2 moves to Path B (see step 7 below); the step 6 test that asserts `flag_rate < 0.25` may need to be re-scoped to assert the path-specific outcome. Decide with care:
- If Path A is feasible (e.g., minor FP that's fixable by refining a sample's labelling), record evidence + try Path A.
- If FP is genuine on clean prose (e.g., "when the user describes" repeatedly false-positives), commit to Path B.

### #7 — Fix-C step 2: decision + final state

**Decision input**: the #6 per-sample evidence table.

**Path A** — Acceptance bar passes (FLAG <25% + FP=0 clean):

Edit `autoloop/config.yaml`. Find the `anti_hardcode` block (S-Auto-4 added it). Flip:

```yaml
anti_hardcode:
  enabled: true
  synonym_map_enabled: true             # Fix-C step 2 Path A: flipped after S-Auto-5 calibration;
                                        # see docs/sprints/sprint-058-handoff.md §X for per-sample evidence
  flag_for_codex_rate_warn_threshold: 0.25
```

No additional rule added. The Codex Axis B exact bypass FAILs the detector via the existing Q1 IF/THEN regex after `\b(?:whenever|when)\b` → `if` normalization.

**Path B** — Acceptance bar fails on FP>0 clean prose:

Retain `synonym_map_enabled: false` in `autoloop/config.yaml` (no edit needed if it stays at the M-Auto-1A baseline value `false`). Add a new FLAG rule in `autoloop/autoloop/sandbox/anti_hardcode_check.py`:

```python
_RE_Q1_WHEN_ARROW_FLAG = re.compile(
    r"\b(?:whenever|when)\b[\s\S]{1,120}?(?:→|=>|->)[\s\S]{1,120}",
    re.IGNORECASE,
)

def _q1_when_arrow_flag(text: str) -> tuple[str, str] | None:
    """Fix-C step 2 Path B: FLAG (not FAIL) on `\\b(?:whenever|when)\\b ... =>` shape.
    Detector emits FLAG_FOR_CODEX so Codex / human judges; not auto-FAIL because
    'when' is English-common and synonym_map_enabled=false keeps the FAIL gate
    narrow to the Q1 if-then regex."""
    m = _RE_Q1_WHEN_ARROW_FLAG.search(text)
    if m:
        return (_trim(m.group(0)), _FLAG)
    return None
```

(The exact `_FLAG` constant name and `_trim` helper come from the existing rule functions in the same file; reuse the existing pattern. Read the file's existing rule functions first to match the style.)

Register the new rule in the existing rule list under the Q1 category with rule_id `Q1.when_arrow_flag` (alphabetically sorted for deterministic evaluation order). Add corresponding test cases:

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

Rule count grows from 11 → 12 (still well under ≤30 cap). Verify the `test_detector_source_rule_count_bounded` assertion uses `<=30` (it should from M-Auto-1A baseline). If the assertion is hardcoded to a specific integer like `== 11`, surface to deliver-agent — that's a M-Auto-1A code smell needing a fix (NOT silently bumping the constant).

**Whichever path is chosen**, record in `docs/sprints/sprint-058-handoff.md` §X: "Fix-C step 2 final state = **Path A: synonym_map_enabled=true**" OR "Fix-C step 2 final state = **Path B: synonym_map_enabled=false + new Q1.when_arrow_flag FLAG rule**". Include the per-sample evidence table + the one-paragraph rationale linking evidence → decision.

### #8 — Tests (~6-12 NEW; expected total 216 → 222-228)

**Path A new tests** (≥6):
- `test_fix_c_step1_codex_axis_b_bypass_now_fails` (#5)
- `test_fix_c_step1_clean_prose_when_user_describes_still_passes` (#5)
- `test_fix_c_step1_clean_prose_whenever_subordinate_clause_still_passes` (#5)
- `test_fix_c_step1_multi_line_when_arrow_decomposition_fails` (#5)
- `test_real_meta_agent_calibration_flag_rate_under_threshold` (#6)
- `test_real_meta_agent_calibration_fp_zero_on_clean_prose` (#6)

**Path B new tests** (≥8): all of Path A plus:
- `test_fix_c_step2b_when_arrow_flag_on_codex_axis_b_bypass` (#7 Path B)
- `test_fix_c_step2b_when_arrow_flag_does_not_fire_on_clean_prose` (#7 Path B)

Also include `test_real_meta_agent_calibration_sample_count` (asserts ≥10 samples in the fixture; counts as +1 in either path).

### #9 — `autoloop/program.md` — no edit required

The §4 status table row 3 currently reads `S-Auto-4 — DELIVERED (...)`. S-Auto-5 does NOT flip a new status row; the calibration is a refinement of the existing detector, not a new feature row. If `program.md` ever needs an §4 update for M-Auto-1B calibration, that's a deliver-agent decision at S-Auto-5 close, NOT a dev-side edit.

## Hard fences / STOP conditions

These are non-negotiable. Violating any one triggers immediate STOP + surface to deliver-agent + human.

- **Zero touch** to `autoloop/autoloop/scoring/{tier_evaluator,eval_runner,baseline_loader,gaming}.py` — content-hash locked at `5177b674b5ad249d7c0e706f3c827010c8dab511240f239dbd3be6f689a0331c` per M-Auto-1B §6 fence #13. Any edit triggers `gaming.scoring_code_drift.sha_changed` ERROR.
- **Zero touch** to `autoloop/autoloop/loop.py` / `autoloop/autoloop/meta_agent/**` / `autoloop/autoloop/memory/**` / `autoloop/autoloop/cli.py` / `autoloop/autoloop/sandbox/{yaml_diff_validator,applier,content_validator}.py`. The S-Auto-1/2/3/4 substrate is consumed, not modified.
- **Zero touch** to `eval_interactive/eval_interactive/**`, `eval_interactive/case_specs/**`, `eval_interactive/case_specs_shadow/**`, `server/src/main/java/**`, `eval/src/main/java/**`, `data/`, `db/`, `server/src/main/resources/**`, `docs/foundational/`, `docs/runtime_freeze_and_risk_policy.md`, `docs/current/`, sprint archives `docs/sprints/sprint-001-*` through `docs/sprints/sprint-057-*`, milestone archives `docs/milestones/M*_*.md`, `docs/codex-findings.md` scaffold.
- **No new heavy deps** in `autoloop/pyproject.toml` — stdlib `re` + `unicodedata` only. If a dep is needed, STOP and surface.
- **No LLM call** inside `anti_hardcode_check` or its tests (the detector remains deterministic per D1).
- **No semantic hardcode IN the detector** — Fix-C step 1 regex `\b(?:whenever|when)\b` is a generic structural pattern (D2 compliant); optional Fix-B step 2 regex is similarly generic structural. The Codex Axis B exact phrase / any eval case_id / any user utterance / any expected answer / any case-status label MUST NOT appear in any detector rule (only in test fixtures for assertion targets, AND sanitized in the calibration fixture).
- **No `synonym_map_enabled` flip without #6 calibration evidence** — the toggle final state is evidence-driven, not intent-driven. Path A flip requires FLAG <25% + FP=0 on clean prose to be measured AND recorded.
- **No `git add -A`** — stage S-Auto-5 scope explicitly (see Commit discipline section).
- **STOP and surface** conditions:
  - Fix-C step 1 causes ANY existing 17-fixture test to fail.
  - Real-meta-agent batch produces <10 propose samples after 3 iterations.
  - Codex per-sub-sprint review (deliver-agent dispatches at close) returns `reject as semantic hardcode`.
  - Per-iteration elapsed time >40 min.
  - `mvn package` fails / `python -m autoloop check` fails / live-iter crashes in an unhandled path.
  - `test_detector_source_rule_count_bounded` is hardcoded to `== 11` (rather than `<= 30`) — surface as M-Auto-1A code smell rather than silently bumping the constant.

## Test / eval requirements

- **Python autoloop suite**: `cd autoloop && uv run --extra dev pytest -q` — baseline `216 passed, 1 warning` must grow by 6-12 NEW S-Auto-5 tests. Total `222-228 passed, 1 warning`. The 1 warning is the S-Auto-2 baseline-missing-shadow asserted behaviour, unchanged.
- **Existing Python eval_interactive suite UNCHANGED**: `cd eval_interactive && uv run python -m pytest --tb=no -q` reproduces `486 passed, 3 failed` (failures env-specific per OQ-S47.3).
- **Java baseline UNCHANGED**: skipped per Java-zero-touch (verify `git diff --stat -- server/src/main/java/ eval/src/main/java/` against your sub-sprint commit returns empty).
- **17-fixture calibration sweep**: 11/11 forbidden FAIL + 4/4 clean PASS + 2/2 borderline FLAG_FOR_CODEX UNCHANGED after Fix-C step 1.
- **Detector self-discipline regression tests**: 3 tests UNCHANGED — PASS.
- **Real-meta-agent batch calibration**: ≥10 samples; FLAG rate <25%; FP=0 on clean prose with the chosen `synonym_map_enabled` setting.
- **Live-iter end-to-end**: 1-3 real iterations completed without unhandled crash; per-iteration elapsed time recorded; `autoloop/results/runs/exp-N/` contents present.
- **No live LLM call in pytest tests** — calibration tests use the sanitized fixture file, NOT a fresh LLM call.

## §7 — Layer-classification + anti-hardcode stanza (REQUIRED)

**Target failure layer:** `infra` (primary; §3.2 Q1 — live-iter prerequisite + first real iteration are infrastructure execution + observation) + `eval_spec` (calibration; §3.2 Q6 — Fix-C step 1 + optional step 2 are structural refinements of the meta-agent output boundary detector). Sub-sprint does NOT change runtime / projection / scoring / CaseSpec / judge / smoke / shadow firewall posture.

**Tier-0 invariant:** adds no Tier-0 invariant. Fix-C step 1 word-boundary regex + optional Fix-B step 2 FLAG rule are meta-loop infrastructure refinements, NOT a runtime invariant per `docs/runtime_freeze_and_risk_policy.md` §1/§2. The existing Q2 rule (detecting Tier-0 invariant invention attempts) is UNCHANGED.

**Semantic hardcode:** No semantic hardcode introduced. Justification:

- Fix-C step 1 word-boundary regex `r"\b(?:whenever|when)\b"` is a **generic structural pattern** (D2 compliant) — matches the two English words "whenever" and "when" as word-boundary tokens. Does NOT enumerate specific eval phrases / user utterances / expected answers / case-status labels.
- Optional Fix-B step 2 FLAG rule `_RE_Q1_WHEN_ARROW_FLAG` similarly matches the **generic structural shape** `\b(?:whenever|when)\b ... (?:→|=>|->) ...` as FLAG (NOT auto-FAIL).
- `synonym_map_enabled` toggle is a **config-level feature flag** whose semantics are already in the codebase from S-Auto-4; flipping is evidence-driven (≥10 real-meta-agent samples + 17-fixture sweep both pass).
- Real-meta-agent calibration fixture at `autoloop/tests/fixtures/real_meta_agent_calibration_samples.json` is a **test data file** with sanitized samples; sanitization removes literal `case_id` / `session_id` / user-utterance tokens.
- Detector self-discipline regression tests (3) continue to grep-PASS — no literal `cs<id>` / `closure_criterion` / etc. tokens in the rule source.

**Generalization coverage:**

- **target** = (i) live-iter pipeline end-to-end on `auto-loop-branch` against a real meta-agent LLM (regardless verdict); (ii) R-S57 detector calibration: 17-fixture sweep maintains 11/11+4/4+2/2 + real-meta-agent batch FLAG <25% + FP=0 on clean prose + Codex Axis B exact bypass now FAILs the detector.
- **neighbor** = R-S57 adversarial variants (unicode obfuscation `ｗhenever`; multi-line decomposition `When ...\n=> ...`; semantic synonym swap "anytime ... ->"; logically-equivalent shapes "Given X happens, then output Y") — detector rejects (or FLAGs per Path).
- **negative-control** = 17-fixture clean PASS cases (4) + ≥3 real-meta-agent clean prose samples ("when the user describes their issue, ..." / "whenever possible, prefer concrete examples ..."). Must NOT false-positive after Fix-C step 1.
- **shadow** = shadow firewall posture UNCHANGED. `gaming.shadow_set_leakage` rule + `shadow_leak_signatures` config list seeded with `["cs59s", "shadow_case_id", "shadow/", "case_specs_shadow"]` from M-Auto-1A unchanged.

## Codex review plan (§4.3)

**PER-SUB-SPRINT REQUIRED** — §4.3 trigger #2. Fix-C detector change touches the §1.7 structural guard; Codex must independently verify Fix-C structural soundness + calibration evidence soundness BEFORE S-Auto-6 begins overnight.

**Codex prompt timing**: deliver-agent authors `compact/sprint-058-codex-review-prompt.md` at S-Auto-5 **close**, after you've committed. YOU do NOT dispatch Codex; the deliver-agent + human dispatch.

**Codex must verify** (these 7 axes you should pre-mitigate against in your local dev work):

1. §4.1 nine-question kernel walk against the modified detector source (`anti_hardcode_check.py` + the new `_RE_WHEN_WORD_BOUNDARY` + optional new `_RE_Q1_WHEN_ARROW_FLAG` + `_q1_when_arrow_flag`): does Fix-C step 1 or step 2 encode any §1.7-violating decision logic? (D2 regression — must remain generic structural.)
2. Detector catches the Codex Axis B exact bypass after Fix-C (FAIL on Path A; FLAG on Path B).
3. ≥3 NEW bypass spot-checks (unicode obfuscation, multi-line decomposition, semantic synonym swap "anytime" / "as soon as" / German / Spanish translation). Detector rejects or FLAGs each correctly.
4. Real-meta-agent calibration evidence audit: ≥10-sample fixture + per-sample verdict table in handoff §X; FLAG <25% + FP=0 on clean prose; Path A / Path B decision rationale matches evidence.
5. Detector self-discipline 3 regression tests PASS; rule count ≤30.
6. 17-fixture sweep still produces 11/11+4/4+2/2 after Fix-C step 1.
7. Live-iter end-to-end evidence documented (prerequisite + first real iteration + elapsed time + outcomes).

**Verdict expected**: `pass / 0` or `approve with downgrade-to-signal follow-up`. `reject as semantic hardcode` triggers fix-iteration S-Auto-5.1 BEFORE S-Auto-6 can begin.

**Pre-mitigation (your job before commit)**: locally run ≥3 adversarial spot-checks of your own (unicode, multi-line, synonym swap variants) and verify the detector rejects each; document attempts in handoff §X "Adversarial spot-check pre-Codex".

## Handoff requirements

Author `docs/sprints/sprint-058-handoff.md` at S-Auto-5 close. Leave **§12** empty (deliver-agent + human fill at milestone close). Required sections:

- **§1 Class + §7 stanza self-walk** — confirm the §7 stanza above against actual delivered scope; flag any deviation.
- **§2 Goal achievement** — bullet each of the 6 Goal items + whether each was hit, with evidence pointer.
- **§3 Scope execution log** — for each scope step #1-#9, brief status (DONE / PARTIAL / SKIPPED-WITH-REASON).
- **§4 Live-iter end-to-end record** — `mvn package` outcome; `python -m autoloop check` output summary; `git status` clean confirmation; per-iteration table (exp-1 ... exp-N with target_skill / target_field / verdict / discard_reason / elapsed_ms); cumulative propose-sample count.
- **§5 Real-meta-agent calibration evidence** — per-sample verdict table (≥10 rows: sample_id, target_field, proposed_value-first-80-chars, verdict, rule_id, clean_prose_label); aggregate FAIL/FLAG/PASS counts; FLAG rate; FP rate on clean prose; **Path A / Path B decision + one-paragraph rationale**.
- **§6 17-fixture sweep after Fix-C** — 11 FAIL + 4 PASS + 2 FLAG outcome breakdown unchanged; detector self-discipline 3 regression tests PASS status; rule count (11 in Path A; 12 in Path B).
- **§7 Adversarial spot-check pre-Codex** — ≥3 adversarial constructions you tried locally + detector verdict for each (dev pre-mitigation; Codex will independently verify).
- **§8 Code anchor table** — `git show --numstat <s-auto-5-commit-sha>` table format showing file paths + lines added/removed.
- **§9 Test count** — autoloop pytest baseline 216 → final count + delta breakdown by test file.
- **§10 OQ-S58.x list** — open questions surfaced during execution (see OQ section below for expected candidates).
- **§11 §7 stanza self-walk verification** — explicit "self-walk passed" / "deviations: ..." line.
- **§12 Closure** — empty; deliver-agent + human fill at milestone close.

You do NOT author the Codex review prompt or findings (deliver-agent's job at S-Auto-5 close).

## Commit discipline

Stage S-Auto-5 scope ONLY:

- Modified `autoloop/autoloop/sandbox/anti_hardcode_check.py` (Fix-C step 1; Path B: + step 2 FLAG rule).
- Modified `autoloop/config.yaml` (Path A: flip `synonym_map_enabled: false → true`; Path B: no change, or just a comment).
- Modified `autoloop/tests/test_anti_hardcode_check.py` (≥4 new tests for Fix-C step 1; Path B: +2 tests for step 2).
- New `autoloop/tests/test_real_meta_agent_calibration.py` (the calibration sweep test).
- New `autoloop/tests/fixtures/real_meta_agent_calibration_samples.json` (sanitized ≥10-sample fixture).
- Updates to `autoloop/results/runs/exp-N/` + `autoloop/results/experiments.jsonl` + `autoloop/results/iterations.sqlite` IF committed (verify `.gitignore` policy first — if `autoloop/results/` is git-ignored, evidence captured in handoff only).
- New `docs/sprints/sprint-058-handoff.md`.

**No `git add -A`**. **No bundle of deliver-agent close artefacts** (`docs/sprint_objective.md` / `docs/milestone_objective.md` / `docs/codex-findings.md` / `docs/10-handoff.md` / `docs/action_bank.md` / `compact/sprint-058-codex-review-prompt.md`) — those bundle by human at close per `feedback_commit_at_end_bundles_deliver_artefacts.md`.

**One commit** at sub-sprint close (commit-at-end pattern). Commit message:

```
Sprint 058 / S-Auto-5 / M-Auto-1B — live-iter bootstrap + Fix-C step 1 + calibration evidence + step 2 Path <A or B>

[2-3 sentences describing the live-iter outcome + Fix-C step 1 + step 2 decision]

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
```

(Replace `<A or B>` with the actual Path chosen.)

## Self-check checklist (before declaring sub-sprint done)

Before authoring §1-§11 of `docs/sprints/sprint-058-handoff.md` and committing, verify each:

- [ ] `mvn package` succeeded; `server/target/*.jar` exists.
- [ ] `python -m autoloop check` reports `AUTOLOOP_META_LLM_API_KEY` set.
- [ ] `git status` was clean immediately before the first `python -m autoloop run --experiments 1`.
- [ ] ≥1 live iteration completed end-to-end; outcome (keep/discard/error) recorded; elapsed time recorded.
- [ ] `autoloop/results/runs/exp-1/` contains `hypothesis.json` + `diff.yaml` + per-stage verdict artefacts.
- [ ] ≥10 real meta-agent propose samples captured in `autoloop/results/experiments.jsonl` (across 1-3 iterations).
- [ ] `autoloop/tests/fixtures/real_meta_agent_calibration_samples.json` contains ≥10 sanitized samples (no literal `case_id` / `session_id` / user-utterance tokens).
- [ ] Fix-C step 1: `_RE_WHEN_WORD_BOUNDARY = re.compile(r"\b(?:whenever|when)\b")` added; `_normalize` extended with `re.sub` guarded by `synonym_map_enabled`; `_SYNONYM_MAP` unchanged.
- [ ] 17-fixture calibration sweep still produces 11/11 forbidden FAIL + 4/4 clean PASS + 2/2 borderline FLAG_FOR_CODEX after Fix-C step 1.
- [ ] Detector self-discipline 3 regression tests PASS (`test_detector_source_does_not_hardcode_eval_case_ids` + `..._user_utterance_literals` + `..._rule_count_bounded`).
- [ ] Rule count ≤30 (11 if Path A; 12 if Path B).
- [ ] `test_fix_c_step1_codex_axis_b_bypass_now_fails` PASSES (the R-S57 Axis B bypass now FAILs the detector under `synonym_map_enabled=True`).
- [ ] ≥3 clean-prose negative-control tests PASS (the regex does NOT false-positive on "when the user describes" / "whenever possible" / similar).
- [ ] Multi-line decomposition test PASSES (`When ...\n=> ...` FAILs the detector).
- [ ] Real-meta-agent batch calibration: FLAG rate <25% AND FP rate on clean prose = 0 (with the chosen `synonym_map_enabled` setting).
- [ ] Fix-C step 2 decision recorded in handoff §5 with per-sample evidence + one-paragraph rationale (Path A: `synonym_map_enabled=true`; Path B: `synonym_map_enabled=false` + new `Q1.when_arrow_flag` FLAG rule).
- [ ] If Path B: ≥2 new tests for `_q1_when_arrow_flag` (FLAG on Codex Axis B exact phrase; no false-fire on clean "when" prose).
- [ ] `cd autoloop && uv run --extra dev pytest -q` reports 222-228 passed, 1 warning (baseline 216 + 6-12 new).
- [ ] `cd eval_interactive && uv run python -m pytest --tb=no -q` reports `486 passed, 3 failed` unchanged (OQ-S47.3 env-specific failures unchanged).
- [ ] `git diff --stat -- server/src/main/java/ eval/src/main/java/` against your sub-sprint commit returns empty (Java zero touch verified).
- [ ] `git diff --stat -- eval_interactive/eval_interactive/ eval_interactive/case_specs/ eval_interactive/case_specs_shadow/` returns empty (eval-side zero touch verified).
- [ ] `git diff --stat -- autoloop/autoloop/scoring/` returns empty (scoring code SHA preserved per fence #13).
- [ ] `git diff --stat -- autoloop/autoloop/loop.py autoloop/autoloop/meta_agent/ autoloop/autoloop/memory/ autoloop/autoloop/cli.py autoloop/autoloop/sandbox/yaml_diff_validator.py autoloop/autoloop/sandbox/applier.py autoloop/autoloop/sandbox/content_validator.py` returns empty (substrate untouched).
- [ ] `docs/sprints/sprint-058-handoff.md` §1-§11 filled; §12 empty.
- [ ] Adversarial spot-check pre-Codex: ≥3 constructions tried + detector verdict for each documented in handoff §7.
- [ ] No `git add -A` was run; staging was explicit per Commit discipline.
- [ ] Commit message follows the Sprint 058 / S-Auto-5 / M-Auto-1B template; footer `Co-Authored-By` present.

After all boxes ticked: surface to deliver-agent + human that S-Auto-5 is ready for Codex per-sub-sprint review. Deliver-agent dispatches Codex; you do NOT.

## OQ (open questions — fill in handoff §10 during execution)

Expected OQ-S58.x candidates:

- **OQ-S58.1** — whether the meta-agent's propose distribution produces enough diverse samples in 1-3 iterations to constitute "≥10 real samples", or if the meta-agent loops on similar hypotheses requiring deliver-agent + human supplementary samples.
- **OQ-S58.2** — exact wall-clock per-iteration elapsed time vs. M-Auto-1B §9 estimate (12-25 min target; >40 min triggers stop-condition discussion).
- **OQ-S58.3** — whether Path A or Path B was chosen at Fix-C step 2 and the per-sample evidence justification.
- **OQ-S58.4** — whether any adversarial spot-check beyond the 3 dev-tried-locally surfaced (Codex will independently verify; OQ-S58.4 records dev's pre-mitigation attempts).
- **OQ-S58.5** — whether `autoloop/results/` directory is `.gitignore`'d or committed (existing convention; if `.gitignore`'d, live-iter evidence captured in handoff only).
- **OQ-S58.6** — whether `make_hypothesis` and `make_config` test helpers in `test_anti_hardcode_check.py` support the new test patterns; surface if helper extensions are needed (additive, no signature break).
- **OQ-S58.7** — whether `test_detector_source_rule_count_bounded` is hardcoded to `== 11` (M-Auto-1A code smell; surface as fix-via-deliver-agent rather than silent bump).

Add OQ entries as ambiguities are encountered; record disposition (in-scope-resolve / surfaced-out / deferred-to-milestone-close) per OQ in handoff §10.

---

**END OF DEV PROMPT.** This is your full executable contract. Begin with #1 (mvn package). Surface to deliver-agent + human at any STOP condition or ambiguity.
