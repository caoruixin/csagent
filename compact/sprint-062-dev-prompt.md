# Sprint 062 / S-Auto-7.2 / M-Auto-1C — Dev Session Prompt

You are the dev agent (Claude Code) for **Sprint 062 / S-Auto-7.2 / M-Auto-1C — Auto-Evolution Calibration Continuation, sub-sprint 1 of 2**. Your goal in one sentence: **resolve OQ-S61.1 (`applier.py:370-378` mvn module-selection bug on csagent-parent packaging=pom) via a 1-line fix per the chosen option, then verify smoke iter through Step 9 with non-degenerate `tier_evaluator_verdict`** — this jointly retires Goal #3 + Goal #4 from M-Auto-1B.

This prompt is **self-contained per `iteration_governance.md` §9 invariant**. You do NOT need to read any repo doc beyond `AGENTS.md` (auto-loaded via constitution chain) + this prompt to execute the sub-sprint. Specific code anchors are named below where you need to read code (NOT docs).

## Read order (minimal)

1. `AGENTS.md` is auto-loaded by Claude Code on session start; the constitution chain `doc_governance.md` → `agent_context_guide.md` → `iteration_governance.md` loads transitively. **Do NOT manually read** these.
2. This prompt.
3. **Code anchors** (read on demand during work):
   - `autoloop/autoloop/sandbox/applier.py` (focus on lines 370-378 mvn invocation block; surrounding context for free-port socket bind + actuator probe timeout).
   - `pom.xml` (repo root; verify `<packaging>pom</packaging>` on `csagent-parent` + `<module>server</module>` + `<module>eval</module>`).
   - `server/pom.xml` (verify server submodule inherits parent + has `mainClass` configured for spring-boot-maven-plugin).
   - `autoloop/tests/test_applier.py` OR `autoloop/tests/test_applier_mvn_invocation.py` (whichever exists; explore current test layout before adding new tests).
4. **Do NOT read**: `docs/sprint_objective.md` (the source-of-truth contract; this prompt is its self-contained executable view per §9.3 sync rule), `docs/milestone_objective.md`, `docs/10-handoff.md`, `docs/sprints/sprint-061-*` (S-Auto-7.1 archives; the relevant context is embedded below).

## Sub-sprint contract (embedded verbatim from `docs/sprint_objective.md`)

### Class

- **Layer (primary)**: `infra` (substrate plumbing repair on the auto-loop applier subprocess invocation; structural; no semantic decision change; no projection / scoring semantic logic edit; no CaseSpec / judge change).
- **§7 stanza**: **EXEMPT** per pure-infra carve-out.
- **Codex review plan**: **DEFAULT milestone-shared at M-Auto-1C close**. No §4.3 trigger expected. UPGRADE to per-sub-sprint Codex per §4.3 trigger #3 ONLY IF dev encounters substrate brittleness requiring new fence touch (dev STOP-and-surfaces).
- **Sub-sprint position in milestone**: 1st of 2 (S-Auto-7.2 substrate fix → S-Auto-8 first overnight + first cherry-pick).

### Goal

Resolve OQ-S61.1 (`applier.py:370-378` mvn module-selection bug on csagent-parent packaging=pom) via a 1-line fix per the chosen option (recommended option (1): drop `-am`), then verify end-to-end via smoke iter through Step 9 with non-degenerate `tier_evaluator_verdict`. This jointly retires Goal #3 + Goal #4 from M-Auto-1B.

### Critical context (verified by S-Auto-7.1 + deliver-agent independent sample 2026-05-29)

Root cause: `pom.xml` declares `<artifactId>csagent-parent</artifactId> + <packaging>pom</packaging>` aggregating `<module>server</module>` + `<module>eval</module>`. `applier.py:370-378` invokes:

```python
cmd = [
    "mvn", "-q",
    "-pl", "server", "-am",
    "spring-boot:run",
    f"-Dspring-boot.run.arguments=--server.port={port}",
]
```

`-pl server -am` brings parent into the reactor (server inherits from parent); mvn applies `spring-boot:run` to **ALL** selected modules; `csagent-parent` packaging=pom + no `mainClass` → spring-boot:run fails on parent BEFORE reaching server submodule:

```
[ERROR] Failed to execute goal org.springframework.boot:spring-boot-maven-plugin:3.2.5:run
   on project csagent-parent: Unable to find a suitable main class
```

S-Auto-7.1 pre-flight clears the foreground :8080 masking; the underlying mvn-invocation bug is now the dominant signal. The historical OQ-S58.7 / OQ-S60.7 "Flyway lock contention + maven race + Redis pool" attribution was INCORRECT — that masked-attribution is RETIRED. The actual root cause is the mvn module-selection bug.

### Fix options (deliver-agent + human have NOT pre-locked an option; you pick at planning round; recommended is (1))

- **(1) Drop `-am`**: `mvn -q -pl server spring-boot:run -Dspring-boot.run.arguments=...`. Relies on parent + server already in local maven repo (normally the case in dev). Reactor selects only server submodule; spring-boot:run applies only to server. PROS: 1-line `-am` removal, no cwd change, no plugin coordinate brittleness. CONS: requires parent in local repo. **Recommended starting point.**
- **(2) Invoke from server submodule cwd**: `cwd=str(root / "server")`; `cmd = ["mvn", "-q", "spring-boot:run", f"-Dspring-boot.run.arguments=--server.port={port}"]`. No `-pl`; no `-am`. PROS: cleanest reactor scoping. CONS: cwd change requires test fixture adjustments + potential path-handling subtleties for existing free-port socket bind + actuator probe.
- **(3) Spring-boot plugin scope**: `mvn -q -pl server -am org.springframework.boot:spring-boot-maven-plugin:3.2.5:run -Dspring-boot.run.arguments=...`. Fully-qualified plugin coordinate constrains goal-to-server-only. PROS: parent stays in reactor for `-am` dependency resolution. CONS: brittle to Spring Boot version bumps; requires version match against parent's `<spring-boot.version>`.

If option (1) does NOT work (e.g., parent not in local repo as expected; reactor doesn't select correctly), **STOP-and-surface** to deliver-agent + human via AskUserQuestion BEFORE proceeding with option (2) or (3).

### Scope (5 steps — execute in order)

1. **Pick fix option** (recommended (1): drop `-am`). Edit `autoloop/autoloop/sandbox/applier.py:370-378` to remove `-am` from the mvn invocation. Single-line diff of the `cmd` list construction. Preserve `mvn -q`; preserve `-pl server`; preserve `spring-boot:run`; preserve the `-Dspring-boot.run.arguments=--server.port={port}` flag. If option (1) doesn't work in exploration, STOP-and-surface.

2. **Add 1-2 tests** in `autoloop/tests/test_applier_mvn_invocation.py` (NEW) OR extend `autoloop/tests/test_applier.py` if it exists (explore first — `ls autoloop/tests/` to see current layout). Minimum coverage:
   - (a) Test that the constructed subprocess command does NOT include `-am` in the reactor-selection style that triggers OQ-S61.1.
   - (b) Test that the chosen fix's command shape matches expectation. For option (1): expect `cmd == ["mvn", "-q", "-pl", "server", "spring-boot:run", "-Dspring-boot.run.arguments=--server.port=<port>"]` (with `<port>` matched by regex `r"--server.port=\d+"` if specific port assertion impractical).
   - Use `subprocess.Popen` mock fixtures (existing pattern in `autoloop/tests/`); do NOT require live mvn invocation in unit tests.

3. **Smoke iter end-to-end** via the now-fixed applier path:
   - Restart foreground :8080 if needed (the human's Thursday backend) OR rely on S-Auto-7.1 `--auto-reboot` flag.
   - Run `python -m autoloop run --experiments 1` (NO `--dry-run`).
   - The full 14-step state machine must complete; Steps 6 (Spring spawn) + 7 (eval_runner) + 9 (tier_evaluator) must all reach non-degenerate outputs.
   - `autoloop/results/runs/exp-<N>/iteration_record.json` must contain non-empty `tier_evaluator_verdict` (Layer 0-4 all non-null).
   - Record per-iter elapsed time (this is the FIRST measurement of full Spring-spawn + 46-case-eval cycle with proper module selection).
   - Iteration terminal verdict can be keep / discard / error — any of three is OK; the test is "did the state machine execute end-to-end".

4. **Hard-fence verification (cumulative)** — `git diff --stat HEAD -- autoloop/autoloop/scoring/ autoloop/autoloop/sandbox/anti_hardcode_check.py autoloop/autoloop/sandbox/content_validator.py autoloop/autoloop/sandbox/gaming.py autoloop/autoloop/loop.py autoloop/autoloop/meta_agent/ autoloop/autoloop/memory/ autoloop/autoloop/preflight.py autoloop/autoloop/cli.py eval_interactive/ server/ eval/ data/ db/ server/src/main/resources/ docs/foundational/ docs/runtime_freeze_and_risk_policy.md docs/current/` returns **empty**. All M-Auto-1C §6 17+1 hard fences honored except the planned controlled fence #18 override on `applier.py:370-378`.

5. **Author handoff** `docs/sprints/sprint-062-handoff.md` per §"Handoff requirements" below. Single-commit pattern preferred (~10-50 LOC including tests). If applier exploration surfaces a different fix layer beyond `applier.py:370-378`, dev STOP-and-surfaces (§4.3 trigger #3 upgrade to per-sub-sprint Codex may fire).

### Hard fences / STOP conditions

**Hard fences** (M-Auto-1C §6 17+1 list):

- **No edits** to any file under: `server/src/main/java/**`, `eval/src/main/java/**`, `eval_interactive/eval_interactive/**`, `eval_interactive/case_specs/**`, `eval_interactive/case_specs_shadow/**`, `server/src/main/resources/{prompts,scripts,config,mock,skills}/**` (skills/ is S-Auto-8-only cherry-pick path; NOT S-Auto-7.2), `data/**`, `db/**`, `docs/foundational/**`, `docs/runtime_freeze_and_risk_policy.md`, `docs/current/**`, `docs/teams/**`, sprint archives `docs/sprints/sprint-001-*` through `docs/sprints/sprint-061-*`, prior milestone archives under `docs/milestones/`.
- **No edits** to `autoloop/autoloop/scoring/{tier_evaluator,eval_runner,baseline_loader,gaming}.py` (fence #13 reasserted at hash `22548e20ea50518b35c78cebddde891ddb46ea0452e32e49518ad4caab9188a9`).
- **No edits** to `autoloop/autoloop/sandbox/{anti_hardcode_check,content_validator,gaming}.py`, `autoloop/autoloop/loop.py`, `autoloop/autoloop/meta_agent/**`, `autoloop/autoloop/memory/**`, `autoloop/autoloop/preflight.py` (S-Auto-7.1 final), `autoloop/autoloop/cli.py` (S-Auto-7.1 final).
- **The only writable applier.py scope** is lines 370-378 per the chosen fix option (controlled fence #18 override). All other applier.py lines stay byte-identical.
- **No `git add -A`** — stage only S-Auto-7.2 scope files explicitly: `autoloop/autoloop/sandbox/applier.py` (line edit) + new/extended test file + `docs/sprints/sprint-062-handoff.md` + smoke iter artefacts under `autoloop/results/runs/exp-<N>/`.

**STOP-and-surface conditions** (use AskUserQuestion):

- Option (1) drop-`-am` does NOT work in local dev (parent missing from local repo; reactor doesn't select correctly) → propose option (2) or (3).
- Applier.py exploration reveals a non-`applier.py:370-378` fix is needed → propose fence-touch scope expansion.
- Smoke iter crashes through a non-applier-mvn-related path (e.g., Spring spawn succeeds but Step 7 eval_runner crashes; or Step 9 tier_evaluator returns degenerate Layer 4) → root-cause investigation.
- A new substrate brittleness surfaces (Flyway lock contention; Redis pool exhaustion) NOT applier-mvn-related → may become future S-Auto-7.3 fix-iteration.

### Test / eval requirements

- **autoloop pytest baseline preserved**: `cd autoloop && uv run --extra dev pytest -q` returns ≥ 255 PASS (S-Auto-7.1 close baseline). Expected end count: 256-258 PASS (1-3 new applier mvn invocation tests). Any new FAILs require investigation.
- **eval_interactive pytest baseline UNCHANGED**: `cd eval_interactive && uv run python -m pytest --tb=no -q` returns `486 passed, 3 failed` (3 env-specific failures per OQ-S47.3 STATUS QUO).
- **17-fixture detector sweep UNCHANGED**: `cd autoloop && uv run --extra dev pytest -p no:cacheprovider -q tests/test_anti_hardcode_check.py` returns `31 passed`.
- **scoring_code_baseline_sha REASSERTED**: `cd autoloop && uv run --extra dev python -c "from autoloop.scoring.gaming import _compute_scoring_code_sha; print(_compute_scoring_code_sha())"` returns `22548e20ea50518b35c78cebddde891ddb46ea0452e32e49518ad4caab9188a9`; `_check_scoring_code_drift(config) == []` silent.
- **Smoke iter recorded artefacts**: `autoloop/results/runs/exp-<N>/iteration_record.json` contains non-empty `tier_evaluator_verdict` (Layer 0-4 all non-null); per-iter elapsed time recorded.
- **Java baseline UNCHANGED**: `Tests run: 1183, Failures: 1, Errors: 0, Skipped: 2`. SKIP running if Java zero-touch verified via `git diff --stat HEAD -- server/ eval/src/main/java/` returning empty.

### §7 stanza (EXEMPT — self-walked)

S-Auto-7.2 is §7 EXEMPT per pure-infra carve-out (substrate plumbing repair). Self-walk paper-trail:

- **Target failure layer**: `infra` (substrate plumbing repair; `applier.py:370-378` subprocess invocation correction).
- **Tier-0 invariant**: This sub-sprint adds no Tier-0 invariant. C2/C3 candidates remain DEFER.
- **Semantic hardcode**: No semantic hardcode introduced. `-am` removal is a maven-CLI structural correction (reactor scope: parent + server → server only); no keyword, regex, if-else, enum, or per-UC matrix added.
- **Generalization coverage**: target = OQ-S61.1. Neighbor = none (structurally unique). Negative = smoke iter MUST NOT crash through a different substrate path; if it does, STOP-and-surface. Shadow = N/A (no semantic surface touched).

### Codex review plan (per §4.3)

**Default**: DEFAULT milestone-shared at M-Auto-1C close. Deliver-agent + human dispatch the M-Auto-1C milestone-shared review prompt at close-bundle commit (after S-Auto-8 complete).

**Trigger conditions** (dev STOP-and-surfaces if any fire):

- §4.3 trigger #3 (hard-fenced surface explicitly named out of scope) fires if applier exploration reveals a non-`applier.py:370-378` fix is needed. The planned `applier.py:370-378` scope IS a controlled fence override BLESSED at M-Auto-1C planning round (§6 fence #18); scope expansion beyond 370-378 needs explicit authorization.
- §4.3 trigger #2 (new Tier-0 candidate) DOES NOT fire (no Tier-0 invariant introduced).
- §4.3 trigger #1 (§1.7 forbidden-list red line) DOES NOT fire (pure-infra plumbing repair; no semantic hardcode).

### Handoff requirements

`docs/sprints/sprint-062-handoff.md` author at sub-sprint close. Mandatory sections:

1. **§0 Sub-sprint summary**: Goal, scope, single-commit SHA, final autoloop pytest count, smoke iter terminal verdict + per-iter elapsed time + non-degenerate tier_evaluator_verdict evidence.
2. **§1 Cumulative changes**: per-file LOC summary; `git show --stat <commit>` output; classify each file (applier.py = controlled fence #18 override; test file = new test; handoff = scope artefact).
3. **§2 Fix option chosen + rationale**: which of (1) / (2) / (3); evidence supporting the choice; any STOP-and-surface decisions during exploration.
4. **§3 Smoke iter evidence**: full state-machine step-by-step record; Step 6 Spring spawn success transcript; Step 7 eval_runner success (results.json path); Step 9 tier_evaluator non-degenerate verdict (Layer 0-4 all non-null); per-iter elapsed time; terminal iteration verdict.
5. **§4 Hard-fence verification**: `git diff --stat HEAD -- <all gated paths>` output; confirm only the in-scope files are in the commit; explicit fence #18 override evidence.
6. **§5 Test counts**: autoloop pytest before/after; eval_interactive baseline UNCHANGED; 17-fixture detector sweep UNCHANGED; scoring SHA REASSERTED; Java baseline UNCHANGED.
7. **§6 OQ surfaced (if any)**: any new OQ-S62.x surfaced during sub-sprint; for each, root cause + proposed disposition + carry-over recommendation.
8. **§7 STOP-and-surface log** (if any): timestamps + AskUserQuestion records of any human authorization during sub-sprint.
9. **§8 Cumulative deferral notes**: S-Auto-8 readiness checklist; M-Auto-1C close readiness; observation toward Stage-2 entry decision.

### Commit discipline

- **Single-commit pattern preferred** (~10-50 LOC including tests).
- **Commit message format**: `Sprint 062 / S-Auto-7.2 / M-Auto-1C — applier.py:370-378 mvn module-selection fix + smoke iter through Step 9` with bullet-point body summarizing the fix option + smoke iter evidence + test count delta.
- **Standard deliver-agent footer** at commit message end.
- **No `git add -A`** — stage explicitly:
  ```
  git add autoloop/autoloop/sandbox/applier.py
  git add autoloop/tests/test_applier_mvn_invocation.py  # OR extended test file
  git add docs/sprints/sprint-062-handoff.md
  git add autoloop/results/runs/exp-<N>/
  git add autoloop/results/experiments.jsonl autoloop/results/iterations.sqlite
  ```
- **Two-commit pattern acceptable** if smoke iter artefacts are committed separately.

## Self-check checklist (MUST verify before claiming done)

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
- [ ] No new files outside scope.

When all self-check items are complete, this sub-sprint is ready for deliver-agent + human review. Deliver-agent will dispatch S-Auto-8 / Sprint 063 after S-Auto-7.2 sub-sprint close evaluation.
