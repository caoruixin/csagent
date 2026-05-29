---
title: Sprint 062 / S-Auto-7.2 — applier.py:370 mvn module-selection fix (M-Auto-1C — Auto-Evolution Calibration Continuation sub-sprint 1)
doc_tier: current-runtime
status: current
implementation_status: not_started
source_of_truth: this file
last_reviewed: 2026-05-30
review_cadence: per sub-sprint
supersedes: [docs/sprints/sprint-061-objective.md]
superseded_by: null
notes: >
  M-Auto-1C / Sprint 062 / S-Auto-7.2. **Layer**: `infra` (substrate
  plumbing repair on `autoloop/autoloop/sandbox/applier.py:370-378`
  mvn invocation; no semantic decision change; no projection / scoring
  semantic logic edit; no CaseSpec / judge change). **§7 stanza**:
  EXEMPT per pure-infra carve-out (self-walked for paper-trail). **Codex
  review plan**: DEFAULT milestone-shared at M-Auto-1C close — no §4.3
  trigger expected (1-line applier.py change isn't fenced surface or
  §1.7 borderline; controlled fence #18 override is BLESSED at M-Auto-1C
  planning round per `docs/milestone_objective.md` §6 fence #18; Codex
  milestone-shared verifies at close). UPGRADE to per-sub-sprint Codex
  per §4.3 trigger #3 ONLY IF dev encounters substrate brittleness
  requiring new fence touch (dev STOP-and-surfaces).

  **Estimated dev**: ~half-day (1-line diff + 1-2 tests + smoke iter
  end-to-end + handoff; single-commit pattern preferred). Codex review
  deferred to M-Auto-1C milestone close.

  **Critical context**: OQ-S61.1 surfaced at S-Auto-7.1 / Sprint 061
  close 2026-05-29 (handoff §6) as the substrate blocker preventing
  Goal #3 + Goal #4 (smoke iter through Step 9 with non-degenerate
  `tier_evaluator_verdict`). Root cause verified by direct mvn
  invocation: `pom.xml` declares `<artifactId>csagent-parent</artifactId>
  + <packaging>pom</packaging>` aggregating `<module>server</module>` +
  `<module>eval</module>`. `applier.py:370-378` invokes:

  ```python
  cmd = [
      "mvn", "-q",
      "-pl", "server", "-am",
      "spring-boot:run",
      f"-Dspring-boot.run.arguments=--server.port={port}",
  ]
  ```

  `-pl server -am` brings parent into reactor (server inherits from
  parent); mvn applies `spring-boot:run` to ALL selected modules;
  `csagent-parent` packaging=pom + no `mainClass` → spring-boot:run
  fails BEFORE reaching server submodule. Pre-flight (S-Auto-7.1)
  clears the foreground :8080 masking; the underlying mvn-invocation
  bug is now the dominant signal.

  **Fix options enumerated** at S-Auto-7.1 handoff §6 + carried forward
  to S-Auto-7.2 planning round (deliver-agent + human jointly pick at
  sprint planning round; recommended path is option (1) for smallest
  blast radius):

  - **(1) Drop `-am`**: `mvn -q -pl server spring-boot:run -Dspring-
    boot.run.arguments=...` (relies on parent + server already in
    local maven repo; reactor selects only server submodule; spring-
    boot:run applies only to server; PROS = 1-line `-am` removal, no
    cwd change; CONS = requires parent in local repo, normally the
    case in dev). **Recommended**.
  - **(2) Invoke from server submodule cwd**: `cwd=str(root / "server")`;
    `cmd = ["mvn", "-q", "spring-boot:run", f"-Dspring-boot.run.arguments=
    --server.port={port}"]` (no `-pl`; no `-am`; PROS = cleanest
    reactor scoping; CONS = cwd change requires test fixture
    adjustments + potential path-handling subtleties).
  - **(3) Spring-boot plugin scope**: `mvn -q -pl server -am org.spring
    framework.boot:spring-boot-maven-plugin:3.2.5:run -Dspring-boot.run
    .arguments=...` (fully-qualified plugin coordinate constrains
    goal-to-server-only; PROS = parent stays in reactor for `-am`;
    CONS = brittle to Spring Boot version bumps).

  Dev session selects option (1) by default; if applier exploration
  reveals option (2) or (3) is needed, dev STOP-and-surfaces.

  **Hard fence**: this is the M-Auto-1C §6 fence #18 controlled
  override scope. `applier.py` other-than-370-378 lines stay byte-
  identical from M-Auto-1B close. All other M-Auto-1B §6 fences
  inherit verbatim — see `docs/milestone_objective.md` §6 for the
  full 17+1 fence list.

  **Success metric**: smoke iter through Step 9 with non-degenerate
  `tier_evaluator_verdict` (Layer 0-4 all non-degenerate). This
  jointly retires Goal #3 + Goal #4 from M-Auto-1B.

  Dev session source-of-truth: `compact/sprint-062-dev-prompt.md`
  (self-contained per `iteration_governance.md` §9). Dev session
  reads ONLY: `AGENTS.md` (auto-loaded) + the dev prompt. No
  additional repo doc reads required outside specific code anchors
  named in the prompt.
---

# Sprint 062 / S-Auto-7.2 — applier.py:370 mvn module-selection fix

## Class

- **Layer (primary)**: `infra` (substrate plumbing repair on the auto-loop applier subprocess invocation; structural; no semantic decision change; no projection / scoring semantic logic edit; no CaseSpec / judge change).
- **§7 stanza**: **EXEMPT** per pure-infra carve-out (self-walked for paper-trail; see §7 below for the EXEMPT stanza).
- **Codex review plan (§4.3)**: **DEFAULT milestone-shared at M-Auto-1C close**. No §4.3 trigger expected. UPGRADE to per-sub-sprint Codex per §4.3 trigger #3 ONLY IF dev encounters substrate brittleness requiring new fence touch (dev STOP-and-surfaces).
- **Sub-sprint position in milestone**: 1st of 2 (S-Auto-7.2 substrate fix → S-Auto-8 first overnight + first cherry-pick).

## Goal

Resolve OQ-S61.1 (`applier.py:370-378` mvn module-selection bug on csagent-parent packaging=pom) via a 1-line fix per the chosen option (recommended option (1): drop `-am`), then verify end-to-end via smoke iter through Step 9 with non-degenerate `tier_evaluator_verdict`. This jointly retires Goal #3 + Goal #4 from M-Auto-1B.

## Scope (5 steps)

1. **Pick fix option** (recommend (1): drop `-am`). Edit `autoloop/autoloop/sandbox/applier.py:370-378` to remove `-am` from the mvn invocation: `cmd = ["mvn", "-q", "-pl", "server", "spring-boot:run", f"-Dspring-boot.run.arguments=--server.port={port}"]`. If applier exploration reveals option (1) does NOT work (e.g., parent not in local repo as expected), STOP-and-surface to deliver-agent + human BEFORE proceeding with option (2) or (3).
2. **Add 1-2 tests** in `autoloop/tests/test_applier_mvn_invocation.py` (NEW) OR extend existing `autoloop/tests/test_applier.py` (depending on current test layout; explore first). Minimum coverage: (a) test that the constructed subprocess command does NOT include `-am` in the reactor-selection style that triggers OQ-S61.1; (b) test that the chosen fix's command shape matches expectation (option (1) expected: `["mvn", "-q", "-pl", "server", "spring-boot:run", "-Dspring-boot.run.arguments=..."]`). Use `subprocess.Popen` mock fixtures (existing pattern in `autoloop/tests/`); do NOT require live mvn invocation in unit tests.
3. **Smoke iter end-to-end** via the now-fixed applier path: with foreground :8080 stopped (manual or `--auto-reboot` via S-Auto-7.1 pre-flight), run `python -m autoloop run --experiments 1` (NO `--dry-run`). The full 14-step state machine must complete; Steps 6 (Spring spawn) + 7 (eval_runner) + 9 (tier_evaluator) must all reach non-degenerate outputs (`tier_evaluator_verdict` with Layer 0-4 all non-null). Record per-iter elapsed time (this is the FIRST measurement of full Spring-spawn + 46-case-eval cycle with proper module selection). Iteration terminal verdict can be keep / discard / error (any of three is OK; the test is "did the state machine execute end-to-end").
4. **Hard-fence verification (cumulative)**: `git diff --stat HEAD -- autoloop/autoloop/scoring/ autoloop/autoloop/sandbox/anti_hardcode_check.py autoloop/autoloop/sandbox/content_validator.py autoloop/autoloop/sandbox/gaming.py autoloop/autoloop/loop.py autoloop/autoloop/meta_agent/ autoloop/autoloop/memory/ autoloop/autoloop/preflight.py autoloop/autoloop/cli.py eval_interactive/ server/ eval/ data/ db/ server/src/main/resources/ docs/foundational/ docs/runtime_freeze_and_risk_policy.md docs/current/` returns empty. All M-Auto-1C §6 17+1 hard fences honored except the planned controlled fence #18 override on `applier.py:370-378`.
5. **Author handoff** `docs/sprints/sprint-062-handoff.md` per the format below. Single-commit pattern preferred (~10-50 LOC including tests). If applier exploration surfaces a different fix layer requiring fence touch beyond `applier.py:370-378`, dev STOP-and-surfaces (and the §4.3 trigger #3 upgrade to per-sub-sprint Codex may fire).

## Hard fences / STOP conditions

**Hard fences** (M-Auto-1C §6 17+1 list inherited; see `docs/milestone_objective.md` §6):

- **No edits** to any file under `server/src/main/java/**`, `eval/src/main/java/**`, `eval_interactive/eval_interactive/**`, `eval_interactive/case_specs/**`, `eval_interactive/case_specs_shadow/**`, `server/src/main/resources/{prompts,scripts,config,mock,skills}/**` (skills/ is S-Auto-8-only cherry-pick path; NOT S-Auto-7.2), `data/**`, `db/**`, `docs/foundational/**`, `docs/runtime_freeze_and_risk_policy.md`, `docs/current/**`, `docs/teams/**`, sprint archives `docs/sprints/sprint-001-*` through `docs/sprints/sprint-061-*`, prior milestone archives under `docs/milestones/`.
- **No edits** to `autoloop/autoloop/scoring/{tier_evaluator,eval_runner,baseline_loader,gaming}.py` (fence #13 reasserted at hash `22548e20…`).
- **No edits** to `autoloop/autoloop/sandbox/{anti_hardcode_check,content_validator,gaming}.py`, `autoloop/autoloop/loop.py`, `autoloop/autoloop/meta_agent/**`, `autoloop/autoloop/memory/**`, `autoloop/autoloop/preflight.py` (S-Auto-7.1 final), `autoloop/autoloop/cli.py` (S-Auto-7.1 final).
- **The only writable applier.py scope** is lines 370-378 per the chosen fix option (controlled fence #18 override). All other applier.py lines stay byte-identical.
- **No `git add -A`** — stage only S-Auto-7.2 scope files explicitly: `autoloop/autoloop/sandbox/applier.py` (line edit) + new/extended test file + `docs/sprints/sprint-062-handoff.md`.

**STOP-and-surface conditions** (dev pauses + asks deliver-agent + human via AskUserQuestion):

- Option (1) drop-`-am` does NOT work in local dev (e.g., parent missing from local repo; reactor doesn't select correctly) → propose option (2) or (3) before proceeding.
- Applier.py exploration reveals a non-`applier.py:370-378` fix is needed (e.g., the actual root cause is somewhere upstream) → propose fence-touch scope expansion.
- Smoke iter crashes through a non-applier-mvn-related path (e.g., Spring spawn succeeds but Step 7 eval_runner crashes; or Step 9 tier_evaluator returns degenerate Layer 4 because baseline_dir is stale) → root-cause investigation; surface findings.
- A new substrate brittleness surfaces (e.g., Flyway lock contention; Redis pool exhaustion) on the smoke iter that is NOT applier-mvn-related → surface for triage; may become a future S-Auto-7.3 fix-iteration scope.

## Test / eval requirements

- **autoloop pytest baseline preserved**: `cd autoloop && uv run --extra dev pytest -q` returns ≥ 255 PASS (the S-Auto-7.1 close baseline). Expected end count: 256-258 PASS (1-3 new applier mvn invocation tests). Any new FAILs require investigation.
- **eval_interactive pytest baseline UNCHANGED**: `cd eval_interactive && uv run python -m pytest --tb=no -q` returns `486 passed, 3 failed` (the 3 env-specific failures per OQ-S47.3 STATUS QUO).
- **17-fixture detector sweep UNCHANGED**: `cd autoloop && uv run --extra dev pytest -p no:cacheprovider -q tests/test_anti_hardcode_check.py` returns `31 passed` (S-Auto-7.2 zero-touch to detector).
- **scoring_code_baseline_sha REASSERTED**: `cd autoloop && uv run --extra dev python -c "from autoloop.scoring.gaming import _compute_scoring_code_sha; print(_compute_scoring_code_sha())"` returns `22548e20ea50518b35c78cebddde891ddb46ea0452e32e49518ad4caab9188a9` (no change since S-Auto-7 close); `_check_scoring_code_drift(config) == []` silent.
- **Smoke iter recorded artefacts**: `autoloop/results/runs/exp-<N>/` contains `iteration_record.json` with non-empty `tier_evaluator_verdict` (Layer 0-4 all non-null); per-iter elapsed time recorded as the FIRST measurement of full Spring-spawn + 46-case-eval cycle.
- **Java baseline UNCHANGED**: `Tests run: 1183, Failures: 1, Errors: 0, Skipped: 2` (skip running if Java zero-touch verified via `git diff --stat HEAD -- server/ eval/src/main/java/` returning empty).

## §7 stanza (EXEMPT — self-walked for paper-trail)

S-Auto-7.2 is §7 EXEMPT per pure-infra carve-out (substrate plumbing repair on subprocess invocation; no semantic decision change). The stanza below is provided as self-walk paper-trail; it is NOT a contract:

**Target failure layer**: `infra` (substrate plumbing repair; structural; `applier.py:370-378` subprocess invocation correction).

**Tier-0 invariant**: This sub-sprint adds no Tier-0 invariant. C2/C3 candidates from M2 close remain DEFER unchanged.

**Semantic hardcode**: No semantic hardcode introduced. The `-am` removal is a maven-CLI structural correction (reactor scope: parent + server → server only); no keyword, regex, if-else, enum, or per-UC matrix is added.

**Generalization coverage**: target = OQ-S61.1 (csagent-parent packaging=pom mvn module-selection bug). Neighbor = none (the bug is structurally unique to the parent-included reactor scope; no neighboring shape exists in the current codebase). Negative = the smoke iter MUST NOT crash through a different substrate path; if it does, dev STOP-and-surfaces. Shadow = N/A (no semantic surface touched).

## Codex review plan (per §4.3)

**Default**: DEFAULT milestone-shared at M-Auto-1C close. Deliver-agent + human dispatch the M-Auto-1C milestone-shared review prompt at the close-bundle commit (after S-Auto-8 is complete).

**Trigger conditions** (dev STOP-and-surfaces if any fire; deliver-agent + human authorize upgrade to per-sub-sprint Codex):

- §4.3 trigger #3 (hard-fenced surface explicitly named out of scope) fires if applier exploration reveals a non-`applier.py:370-378` fix is needed. The planned `applier.py:370-378` scope IS a controlled fence override BLESSED at M-Auto-1C planning round (§6 fence #18); scope expansion beyond 370-378 needs explicit authorization.
- §4.3 trigger #2 (new Tier-0 candidate) DOES NOT fire (no Tier-0 invariant introduced).
- §4.3 trigger #1 (§1.7 forbidden-list red line) DOES NOT fire (pure-infra plumbing repair; no semantic hardcode introduced).

## Handoff requirements

`docs/sprints/sprint-062-handoff.md` author at sub-sprint close. Mandatory sections:

1. **§0 Sub-sprint summary**: Goal, scope, single-commit SHA, final autoloop pytest count, smoke iter terminal verdict + per-iter elapsed time + non-degenerate tier_evaluator_verdict evidence.
2. **§1 Cumulative changes**: per-file LOC summary; `git show --stat <commit>` output; classify each file (applier.py = controlled fence #18 override; test file = new test; handoff = scope artefact).
3. **§2 Fix option chosen + rationale**: which of (1) / (2) / (3); evidence supporting the choice; any STOP-and-surface decisions during exploration.
4. **§3 Smoke iter evidence**: full state-machine step-by-step record; Step 6 Spring spawn success transcript; Step 7 eval_runner success (results.json path); Step 9 tier_evaluator non-degenerate verdict (Layer 0-4 all non-null); per-iter elapsed time; terminal iteration verdict (keep / discard / error — any OK; the test is end-to-end execution).
5. **§4 Hard-fence verification**: `git diff --stat HEAD -- <all gated paths>` output; confirm only `autoloop/autoloop/sandbox/applier.py` + the test file + `docs/sprints/sprint-062-handoff.md` are in the commit; explicit fence #18 override evidence.
6. **§5 Test counts**: autoloop pytest before/after; eval_interactive baseline UNCHANGED; 17-fixture detector sweep UNCHANGED; scoring SHA REASSERTED; Java baseline UNCHANGED.
7. **§6 OQ surfaced (if any)**: any new OQ-S62.x surfaced during sub-sprint; for each, root cause + proposed disposition + carry-over recommendation.
8. **§7 STOP-and-surface log** (if any): timestamps + AskUserQuestion records of any human authorization during sub-sprint.
9. **§8 Cumulative deferral notes**: S-Auto-8 readiness checklist; M-Auto-1C close readiness; observation toward Stage-2 entry decision.

## Commit discipline

- **Single-commit pattern preferred** (~10-50 LOC including tests).
- **Commit message format**: `Sprint 062 / S-Auto-7.2 / M-Auto-1C — applier.py:370-378 mvn module-selection fix + smoke iter through Step 9` with bullet-point body summarizing the fix option + smoke iter evidence + test count delta.
- **Standard deliver-agent footer** at commit message end.
- **No `git add -A`** — stage explicitly:
  ```
  git add autoloop/autoloop/sandbox/applier.py
  git add autoloop/tests/test_applier_mvn_invocation.py  # OR extended test file
  git add docs/sprints/sprint-062-handoff.md
  git add autoloop/results/runs/exp-<N>/  # the smoke iter artefacts
  git add autoloop/results/experiments.jsonl autoloop/results/iterations.sqlite
  ```
- **Two-commit pattern acceptable** if smoke iter artefacts are committed separately (some `autoloop/results/` paths may have `.gitignore` policy implications; verify existing convention).

## Self-check (dev MUST verify before claiming done)

- [ ] `applier.py:370-378` mvn invocation no longer includes `-am` (option (1) chosen) OR uses cwd=server-submodule (option (2)) OR uses fully-qualified spring-boot plugin coordinate (option (3)).
- [ ] At least 1 new test verifies the chosen fix's command shape.
- [ ] autoloop pytest passes (≥256 PASS expected).
- [ ] eval_interactive pytest baseline UNCHANGED (486 passed, 3 failed).
- [ ] 17-fixture detector sweep PASS UNCHANGED.
- [ ] scoring SHA reasserted; `_check_scoring_code_drift(config) == []` silent.
- [ ] Smoke iter `python -m autoloop run --experiments 1` (NO `--dry-run`) reaches Step 9 with non-degenerate `tier_evaluator_verdict`.
- [ ] `autoloop/results/runs/exp-<N>/iteration_record.json` exists with non-empty `tier_evaluator_verdict` (Layer 0-4 all non-null).
- [ ] Per-iter elapsed time recorded in handoff §3.
- [ ] Hard-fence diff cumulative against all M-Auto-1C §6 gated paths returns empty except the planned `applier.py:370-378` line edit + scope artefacts.
- [ ] Handoff §0-§9 sections all filled per format.
- [ ] Commit staged explicitly (no `git add -A`); single-commit pattern preferred.
- [ ] No new files outside scope (`autoloop/autoloop/sandbox/applier.py`, test file, handoff, smoke iter artefacts).
