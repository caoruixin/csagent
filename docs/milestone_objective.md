---
title: Milestone M-Auto-1A — Auto-Evolution Build (Skill-Driven Hill-Climbing Infrastructure)
doc_tier: current-runtime
status: current
implementation_status: partial
source_of_truth: this file
last_reviewed: 2026-05-27
review_cadence: per milestone
supersedes: [docs/milestones/M5_objective.md]
superseded_by: null
notes: >
  Path 1 research-driven milestone consuming
  `docs/solutions/auto_evolution_skill_driven_v1.md` (2026-05-26, research-agent).
  Human-locked planning decisions (2026-05-27): (1) mutable surface narrows to
  Skill YAML LLM-soft narrative fields ONLY (4 field classes × 6 Skills); all
  config/prompt/templates locked. (2) M-Auto-1 takes priority over M3-B P0
  release-gate blocker (human's call — feature-additive direction). (3) The
  proposal's 5-sub-sprint plan splits into M-Auto-1A (build, 4 sub-sprints
  S-Auto-1..4) and M-Auto-1B (calibration, S-Auto-5 + first kept-candidate
  human-review batch). M-Auto-1A is what this file scopes; M-Auto-1B is
  drafted at M-Auto-1A close. This is the SIXTH milestone under §8 framework.

  **Core thesis**: build the structural defences (sandbox white-list + 4-tier
  lexicographic fitness + 3-layer memory + anti-hardcode kernel auto-checks)
  that make a Skill-YAML hill-climbing loop SAFE before any auto-loop output
  ever lands on main. M-Auto-1A ships zero Skill YAML edits, zero server
  changes, zero eval-code changes; it ships ONLY the autoloop/ subsystem
  (new directory) + tests. M-Auto-1B is where the first overnight run + first
  kept-candidate human review happen.

  **Why a separate build milestone**: Stage-1 (= Alternative A in the
  proposal) hinges on three structural guarantees — (a) sandbox CANNOT
  accept a diff that touches Skill structural fields / tool-policy /
  control-policy / trace_check / mandatory_for / guardrails /
  state_inheritance; (b) tier_evaluator returns a lexicographic verdict
  that respects §1.6 (no down-tier compensation for up-tier loss); (c)
  anti_hardcode_check.py automatically rejects propose outputs containing
  if-else patterns / keyword lists / eval-case-id references BEFORE they
  ever reach build/eval. If any of the three is brittle, calibration in
  M-Auto-1B will surface false-positive "keeps" that waste human review
  time, or worse, slip a §1.7 violation into main. M-Auto-1A is the
  safety-defence milestone; M-Auto-1B is the first real workout.

  **§8.1 conformance**: M-Auto-1A has 4 sub-sprints (within 3-5 ceiling).
  M-Auto-1B is initially scoped at 1 sub-sprint (S-Auto-5) but may grow if
  calibration surfaces fix-iteration needs.

  **Codex review plan (§4.3)**: milestone-shared at M-Auto-1A close DEFAULT
  with S-Auto-4 per-sub-sprint Codex (trigger #2 — the anti-hardcode
  kernel + Tier-2 measurement-contract defence ARE the §1.7 structural
  guard; Codex must independently verify the detector has no design hole
  BEFORE M-Auto-1B begins running it against real propose outputs).

  **M5 carry-over**: projection-hygiene candidate (#4 C3 dedup + OQ-S52.4
  `knowledge_hits`) deferred unchanged; not in M-Auto-1A scope. M3-B
  Single Handover Orchestrator P0 deferred per human's M-Auto-1-first
  decision; remains in the candidate slate.

  **R-item coupling** (read-only awareness, not consumed):
  `R-bad-case-parallel-session-establishment-flakiness` (M5 priority-bumped)
  affects how tier_evaluator runs bad_cases — S-Auto-2 defaults
  bad_cases to `parallel=1` per the recurrence evidence. `R-iwzx-uc-k-vs-uc-h
  -routing-spurious-distress` and `R-bad-case-suite-uc-ghij-seed-from-real
  -sessions` will become natural auto-loop optimization targets in
  M-Auto-1B and beyond; no M-Auto-1A action required.
---

# Milestone M-Auto-1A — Auto-Evolution Build

## 1. Milestone class

**Multi-layer milestone, 4 coordinated sub-sprints.** Layer + §7-stanza + Codex breakdown:

| Sub-sprint | Layer (primary) | §7 stanza | Codex review |
|---|---|---|---|
| S-Auto-1 / Sprint 54 — Mutable-surface contract + YAML diff sandbox | `infra` | REQUIRED | Milestone-shared (default) |
| S-Auto-2 — Four-tier fitness evaluator (tier_evaluator + shadow runner) | `eval_spec` | REQUIRED | Milestone-shared (default) |
| S-Auto-3 — Loop orchestrator + meta-agent + 3-layer memory | `infra` | REQUIRED | Milestone-shared (default) |
| S-Auto-4 — Anti-gaming kernel + content validator + propose-stage anti-hardcode auto-check | `eval_spec` | REQUIRED | **Per-sub-sprint (§4.3 trigger #2 — the kernel IS the §1.7 structural guard; Codex must independently verify the detector before M-Auto-1B runs it)** |

S-Auto-4 per-sub-sprint Codex must return `pass` (or `approve with downgrade-to-signal follow-up`) **BEFORE M-Auto-1A milestone close**. The milestone-shared Codex review at M-Auto-1A close then evaluates the cumulative range covering all 4 sub-sprints; S-Auto-1/2/3 are Codex-deferred to that close.

## 2. Goal

Stand up the full **build-time substrate** for a Skill-YAML-only auto-evolution loop so that, at M-Auto-1A close, a single command can:

1. Take ONE meta-agent-proposed hypothesis (a single Skill YAML edit on a white-listed LLM-soft field).
2. Validate it through `autoloop/sandbox/` — reject anything that touches structural fields, trace_check, mandatory_for, guardrails, state_inheritance, tools_required, applicable_use_cases, or crosses multiple files / multiple Skills.
3. Run it through `autoloop/sandbox/anti_hardcode_check.py` — auto-reject if it encodes if-else patterns, keyword lists, eval-case-id references, or LLM-ownership-shrinking language.
4. Apply, mvn-compile, Spring-restart on an alternate port, run the four-tier eval (Tier-0 Python `hard_checks` family + Tier-1 anchor_outcome + bad_cases programmatic + Tier-2 critical_steps + shadow set; **Java replay surface is legacy / superseded** — see "Layer 0 scope" paragraph below), and produce a lexicographic 5-layer verdict (Tier-0 → Tier-1 → Tier-2 → improvement-threshold → shadow regression).
5. Write the verdict + diff + raw eval artefacts + sanitized failure taxonomy to three persistent memory layers: `experiments.jsonl` (raw), `iterations.sqlite` (structured index), `lessons.md` (K-iter LLM-distilled compaction — first lesson emitted only after K iterations accumulate, which happens in M-Auto-1B).

**Dataset scope (v1 minimal — human-locked planning decision 2026-05-27)**: the per-iteration fitness suite is `bad_cases` (12) + `anchor_outcome` (12) + `shadow` (23) = **47 cases**, expected ~12-15 min per iteration. The `anchor` 159-case suite is intentionally **NOT used as a per-iteration fitness signal in v1**, because its legacy `expected_tool_sequence` / `bot_handling_pattern` / `outcome_class` targets were authored in the Sprint 1-30 era before §5.6 manual-review discipline existed; they reflect historical bot behaviour, not human-validated "correct" behaviour; optimizing against unvalidated targets would corrupt the loop's direction. `anchor` is instead run as a one-time backward-compat check at M-Auto-1A close (per §5 acceptance bar). The 47-case v1 minimum preserves all three load-bearing signals — §5.6 PRIMARY GATE (`bad_cases`) + Tier-1 outcome non-regression (`anchor_outcome`) + anti-overfitting (`shadow`, firewalled: aggregate `regression_detected: y/n` only to meta-agent; per-case shadow failure never reaches the loop). Smoke and the L3 `user_goal_achievement` advisory dim are likewise **NOT** used as v1 hard-gate signals (smoke is §5.5 observation; `user_goal_achievement` is M3-Eval Tier-1 supplementary advisory). M-Auto-2+ may extend the fitness suite as additional datasets mature their human-validated targets.

**Layer 0 scope (v1 — clarified after Sprint 55 schema-check 2026-05-27)**: v1 auto-loop Layer 0 = Python `hard_checks` Tier-0 family (`no_pii_leakage` / `no_human_only_tool_exposure` / `no_critical_policy_violation` / `escalation_compliance` / `phase_transition_validity`) — these are the safety floor every M-class milestone (M1 through M5) has been operating with. The `eval/src/main/java/com/gumtree/csagent/eval/` Java module (containing `GateEvaluator` with the 11 historical "Java replay" gates `critical_policy_violation` / `wrong_containment` / `groundedness_pass_rate` / `escalation_recall` / `handover_completeness` / `tool_scope_violation` / `forbidden_phrase` / `budget_enforcement` / `phase_transition_validity` / `critical_high_risk_escalation` / `out_of_scope_detection`) is **legacy v1-demo-era code, superseded by eval_interactive** at commit `1b71fa4`; last touched at `e247c56` (pre-Sprint-1); zero callers in any current eval flow. The proposal `docs/solutions/auto_evolution_skill_driven_v1.md` §3.3 referenced the Java replay surface as if it were live, but Sprint 55 schema-check (`docs/sprints/sprint-055-handoff.md` §7) confirmed eval-interactive's `results.json` does not produce these gates and never has been wired to. v1 `tier_evaluator.py` Layer 0 reads Python `hard_checks` per case + marks the Java replay 11 gates `{status: "skipped", reason: "not produced by eval-interactive"}` (forward-compatible — if a future stage decides to add a gate it goes into `eval_interactive/eval_interactive/scoring/hard_checks.py`, **NOT** the Java surface). This is NOT a v1 downgrade — it is the actual safety floor history. Governance-hygiene R-item `R-eval-java-module-retirement` opened for M-Auto-2+ cleanup of the legacy Java module.

Crucially M-Auto-1A ships **no Skill YAML edits and no cherry-picks to main**. The dry-run mode + a single live iteration that PRODUCES a verdict (regardless of keep/discard) are the close-gate evidence. The first overnight batch + first human review of kept candidates + first cherry-pick to main are M-Auto-1B territory.

This milestone implements §1, §3.1, §3.3, §3.4, §5 of `docs/solutions/auto_evolution_skill_driven_v1.md` as concrete code. §8 hard fences carry verbatim.

## 3. Sub-sprint sequence

### S-Auto-1 / Sprint 54 — Mutable-surface contract + YAML diff sandbox

**Layer:** `infra`. **§7 stanza:** REQUIRED. **Codex:** milestone-shared (default). **Estimated dev:** 3 days.

**Scope (4 sentences):**

1. Create the `autoloop/` directory tree per proposal §5 (`pyproject.toml`, `autoloop/program.md`, `autoloop/config.yaml`, `autoloop/cli.py` scaffold with `check`/`dry-run`/`run`/`report`/`apply`/`audit` subcommand placeholders, `autoloop/sandbox/` package skeleton). The `program.md` content is the verbatim human contract derived from this milestone's §6 hard fences + the proposal §8 hard fences merged.
2. Implement `autoloop/sandbox/yaml_diff_validator.py` — given an input Skill YAML diff (`unified diff` or `before/after YAML pair`), return ACCEPT / REJECT + reason. Whitelist by AST path: only `procedure`, `grounding_instruction`, `escalation_policy`, `critical_steps[*].desc` ACROSS the 6 production Skill YAMLs may be modified. Anything else (structural fields, guardrails, state_inheritance, trace_check, mandatory_for, severity, id, applicable_use_cases, applicable_phases, tools_required, required_context_keys, max_tool_steps, allow_interim_message, valid_terminal_outcomes, name, description, objective) → REJECT. Cross-file diffs, cross-Skill diffs, new-file diffs, file-delete diffs → REJECT.
3. Implement `autoloop/sandbox/applier.py` (skeleton — actual git commit deferred to S-Auto-3) and `autoloop/sandbox/__init__.py` exposing a unified `validate(diff) -> ValidationResult` API consumed by S-Auto-3's loop.
4. Author `autoloop/tests/` with full coverage of the reject set: structural-field edit, trace_check edit, mandatory_for edit, guardrails edit, state_inheritance edit, multi-Skill edit, multi-file edit (Skill + config), new Skill file, deleted Skill field, malformed YAML, plus 4 positive cases (one edit per LLM-soft field type). Tests run via `pytest`; integrate with the existing repo Python tooling (uv-managed).

**Files in scope** (S-Auto-1 only):
- NEW `autoloop/` directory: `pyproject.toml`, `autoloop/program.md`, `autoloop/config.yaml`, `autoloop/cli.py` (subcommand scaffold), `autoloop/sandbox/{__init__.py,yaml_diff_validator.py,applier.py}`, `autoloop/tests/{__init__.py,test_yaml_diff_validator.py,fixtures/}` with the 12+ fixture diff pairs.
- README or top-level `autoloop/README.md` documenting v1 contract + CLI synopsis.
- NO touch to `server/`, `eval/`, `eval_interactive/`, `data/`, `db/`, `docs/foundational/`, `docs/runtime_freeze_and_risk_policy.md`, `docs/current/iteration_governance.md`, any existing Skill YAML, any case_spec.

### S-Auto-2 — Four-tier fitness evaluator + shadow runner

**Layer:** `eval_spec`. **§7 stanza:** REQUIRED. **Codex:** milestone-shared (default). **Estimated dev:** 3-4 days.

**Scope (4 sentences):**

1. Implement `autoloop/scoring/tier_evaluator.py` — consume an existing `eval_interactive/results/<run-id>/results.json` (no new eval invocation in this sub-sprint; runner invocation is S-Auto-3) and produce a `LexicographicVerdict` per the proposal §3.3 5-layer model narrowed to v1 dataset scope: Layer 0 Tier-0 safety floor (Python `hard_checks` family in eval-interactive; Java replay surface is legacy/superseded — see §2 Layer 0 scope) → Layer 1 Tier-1 outcome non-regression (`anchor_outcome` programmatic `case_passed` rate + `bad_cases` programmatic `case_passed` count; L3 `user_goal_achievement` mean **NOT** consulted per v1 hard-gate-only discipline) → Layer 2 Tier-2 critical-flow non-regression (Tier-2 mandatory `critical_step` failure count across **anchor_outcome + bad_cases only** — anchor 159 excluded per §2 Dataset scope; per-UC mandatory fail rate) → Layer 3 improvement threshold (≥1 case-count improvement across {bad_cases programmatic `case_passed` count +1, anchor_outcome programmatic `case_passed` count +1, Tier-2 mandatory failure count −1} — absolute case-count mode per human-locked 2026-05-27, NOT percent) → Layer 4 shadow regression (≤3% drop allowed). Output: `{layer_results: [...], decision: 'keep'|'discard', discard_reason: '...'|null, tier_breakdown: {...}}`.
2. Implement `autoloop/scoring/eval_runner.py` — subprocess wrappers around `cd eval_interactive && uv run eval-interactive run --path <suite>` for each of the **three v1 suites (bad_cases, anchor_outcome, shadow) per the §2 Dataset scope decision; anchor 159 excluded from per-iteration fitness**. Bad_cases defaults to `parallel=1` per `R-bad-case-parallel-session-establishment-flakiness` evidence (M5-close priority-bumped). Shadow set loads via `CaseSetManager.load_custom(path=...)` against `eval_interactive/case_specs_shadow/`. NO change to any scoring code, case_spec, or runner code.
3. Implement `autoloop/scoring/baseline_loader.py` — read a configured baseline directory (e.g. the last `main`-branch eval run results) and produce baseline metric snapshots that `tier_evaluator.py` compares against. Baseline path configurable via `autoloop/config.yaml`.
4. Add `autoloop/tests/test_tier_evaluator.py` with synthetic `results.json` fixtures covering: clean PASS (all 5 layers pass → keep), Tier-0 fail (→ discard), Tier-1 regression (→ discard), Tier-2 regression (→ discard), no improvement (→ discard), shadow regression (→ discard), edge cases (empty suites, missing dim).

**Files in scope** (S-Auto-2 only):
- NEW `autoloop/scoring/{__init__.py,tier_evaluator.py,eval_runner.py,baseline_loader.py}`.
- NEW `autoloop/tests/test_tier_evaluator.py` + `autoloop/tests/fixtures/results_*.json`.
- NO touch to any existing scoring code, case_spec, eval runner, results schema.

### S-Auto-3 — Loop orchestrator + meta-agent + 3-layer memory

**Layer:** `infra`. **§7 stanza:** REQUIRED. **Codex:** milestone-shared (default). **Estimated dev:** 5 days.

**Scope (5 sentences):**

1. Implement `autoloop/loop.py` — the top-level orchestrator. State machine per iteration: `propose → sandbox validate → anti_hardcode check (S-Auto-4 hook; placeholder no-op in S-Auto-3) → apply (git commit on autoloop/exp-{N} branch) → mvn compile → spring-boot restart on alt port → eval_runner three v1 suites (bad_cases + anchor_outcome + shadow per §2 Dataset scope) → tier_evaluator verdict → memory log → branch tag`. Dry-run mode short-circuits at the propose-stage outputs and skips apply/build/eval.
2. Implement `autoloop/meta_agent/{analyzer.py,proposer.py,lessons_compactor.py}` with companion `prompts/{analyze.txt,propose.txt,compact.txt}`. `analyzer.py` reads the last baseline eval `results.json` and produces a sanitized failure taxonomy (per-Skill aggregation: which Skill's `critical_steps` are advisory FAIL most often; which UC's bad_cases regress most; which anchor_outcome closure_criteria fail). `proposer.py` takes taxonomy + `lessons.md` content + recent K iterations from `iterations.sqlite` and asks the meta-agent LLM for ONE hypothesis (target_skill + target_field + edit description + rationale). `lessons_compactor.py` triggers when iteration count modulo `K=10` (configurable) and compacts recent N iterations into structured lessons appended to `lessons.md`.
3. Implement `autoloop/memory/{experiments_log.py,iterations_index.py,lessons_log.py}` — `experiments_log.py` appends one JSON line per iteration to `autoloop/results/experiments.jsonl` (hypothesis + diff + per-layer verdict + gaming flags + decision + timestamp). `iterations_index.py` uses Python stdlib `sqlite3` (no new dependency) per proposal §3.4 Layer B schema. `lessons_log.py` reads/writes `autoloop/results/lessons.md` markdown.
4. Wire `autoloop/cli.py` `dry-run`, `run`, `report`, `apply`, `audit` subcommands end-to-end against the implemented pieces. `run --experiments 1` should drive ONE full iteration to verdict; `dry-run` should produce a hypothesis + sandbox verdict only (no build, no eval, no memory write beyond a dedicated dry-run log entry).
5. Add `autoloop/tests/test_loop_integration.py` exercising the dry-run path end-to-end with mocked subprocess calls + a fixture meta-agent stub. NO live LLM call in tests.

**Files in scope** (S-Auto-3 only):
- NEW `autoloop/loop.py`, `autoloop/meta_agent/{__init__.py,analyzer.py,proposer.py,lessons_compactor.py}`, `autoloop/meta_agent/prompts/{analyze.txt,propose.txt,compact.txt}`.
- NEW `autoloop/memory/{__init__.py,experiments_log.py,iterations_index.py,lessons_log.py}`.
- EXTEND `autoloop/cli.py` (subcommand wiring; no schema change).
- NEW `autoloop/tests/test_loop_integration.py` + `autoloop/tests/test_memory_layers.py`.
- NO touch to existing code; meta-agent LLM provider configured via `autoloop/config.yaml` (reuse existing provider env vars; no new infra).

### S-Auto-4 — Anti-gaming kernel + propose-stage anti-hardcode auto-check

**Layer:** `eval_spec`. **§7 stanza:** REQUIRED. **Codex:** **PER-SUB-SPRINT (§4.3 trigger #2)**. **Estimated dev:** 4 days + Codex ~2 days.

**Scope (5 sentences):**

1. Implement `autoloop/sandbox/anti_hardcode_check.py` — an automatic checker that takes a proposed diff (a `procedure` / `grounding_instruction` / `escalation_policy` / `critical_steps[].desc` text edit) and runs the auto-tractable subset of the §4.1 nine-question kernel: Q1 (semantic hardcode patterns — enumerated if-then lists, keyword regex), Q2 (Tier-0-invariant invention attempts), Q4 (eval-phrase / case_id / session_id references), Q5 (LLM-ownership-shrinking language like "MUST execute X" / "force the assistant to"). Each check returns PASS / FAIL / FLAG-FOR-CODEX with reason; ANY FAIL at propose-stage → discard before build/eval. FLAG-FOR-CODEX → keep but tag for the human review batch in M-Auto-1B.
2. Implement `autoloop/sandbox/content_validator.py` — placeholder / `{VARIABLE}` syntax validity (catches the case where the meta-agent breaks a Salesforce ID or `{POLICY_URL}` placeholder), text length sanity (no zero-length edit, no >5× growth of a single field), forbidden-token deny-list (specific phrases the human bans — initially small list seeded from §1.7 forbidden examples).
3. Implement `autoloop/scoring/gaming.py` — port the 6 anti-gaming checks from `docs/proposals/autoloop_design.md` §8 (anomalous metric movement, identical eval traces across "different" hypotheses, suspect baseline manipulation, eval-time gaming via timeout/skip, scoring-code drift, shadow-set leakage). ADD the NEW check `tier2_measurement_contract_change_attempt` that flags any iteration where the diff was structurally accepted (impossible per S-Auto-1 sandbox, but belt-and-suspenders) AND results.json shows a previously-FAIL critical_step now scoring N/A.
4. Wire `anti_hardcode_check.py` into `autoloop/loop.py` propose-stage; wire `gaming.py` into the post-eval verdict pipeline; wire `content_validator.py` into the propose-stage pre-sandbox path. Update `autoloop/cli.py audit --experiment exp-N` to surface gaming flags in human-readable form.
5. Tests: full coverage of the auto-hardcode auto-check (positive: clean soft narrative passes; negative: 8+ forbidden patterns must each be auto-rejected including a literal §1.7 example like "if user.message.contains('appeal') then UC-H"); coverage of all 7 gaming checks (synthetic eval-result fixtures triggering each); coverage of content_validator (placeholder break, length anomaly, forbidden token).

**Files in scope** (S-Auto-4 only):
- NEW `autoloop/sandbox/{anti_hardcode_check.py,content_validator.py}`.
- NEW `autoloop/scoring/gaming.py`.
- EXTEND `autoloop/loop.py` (propose-stage + post-eval wiring; no behaviour change to S-Auto-3 main path).
- EXTEND `autoloop/cli.py audit` subcommand.
- NEW `autoloop/tests/{test_anti_hardcode_check.py,test_content_validator.py,test_gaming.py}` with fixture coverage.

**Per-sub-sprint Codex review (§4.3 trigger #2)**: Codex independently verifies the anti-hardcode kernel + gaming checks correctly detect at minimum the 8 forbidden examples listed in the test fixtures + at least one additional hand-authored adversarial example Codex constructs. Codex confirms the detector has no obvious bypass (e.g. unicode obfuscation of "if"/"then", multi-line decomposition of an if-else, semantic equivalence via synonym swap). Codex must return `pass / 0` (or `approve with downgrade-to-signal follow-up` if a borderline case surfaces) **BEFORE M-Auto-1A milestone close**.

## 4. Non-goals (explicit)

- M-Auto-1A does NOT edit any Skill YAML. Auto-loop produces proposals but never commits to main in this milestone; S-Auto-5 / M-Auto-1B is where the first kept candidate goes through human review + cherry-pick.
- M-Auto-1A does NOT change `eval_interactive/eval_interactive/scoring/` or any case_spec. The tier_evaluator CONSUMES `results.json`; it does not modify scoring logic.
- M-Auto-1A does NOT introduce a new Tier-0 invariant. C2/C3 candidates remain DEFER per M2-close verdict; if production trace evidence surfaces during M-Auto-1A execution, surface as separate R-item but do not ship Tier-0 elevation inside M-Auto-1A scope.
- M-Auto-1A does NOT widen the mutable surface beyond Skill YAML LLM-soft fields. Stage-2 candidate (templates.yaml unlock) is explicitly deferred to a future milestone decision; this milestone does not pre-commit a Stage-2 entry criterion.
- M-Auto-1A does NOT run an overnight batch or stress-test the meta-agent at scale. One live iteration in S-Auto-3 + dry-run in S-Auto-1 are the close gates; sustained-load characterization is M-Auto-1B.
- M-Auto-1A does NOT touch the Single Handover Orchestrator (M3-B P0 territory; deferred per human's planning decision).
- M-Auto-1A does NOT modify `docs/runtime_freeze_and_risk_policy.md`, `docs/foundational/**`, `docs/current/iteration_governance.md`. If lessons from M-Auto-1A execution surface a governance fold-back, that is a separate doc-PR on the normal cadence.
- M-Auto-1A does NOT replace `§5.6` bad-case manual review. The auto-loop's `bad_cases` programmatic `case_passed` count is a PROGRAMMATIC SUB-SIGNAL of `case_passed` (a derived bit), NOT a substitute for the human-judgment gate; human review remains the primary gate at milestone close per §5.6.
- M-Auto-1A does NOT replace Codex anti-hardcode review. The S-Auto-4 auto-check is BELT-AND-SUSPENDERS to Codex review, never a substitute.
- M-Auto-1A does NOT touch `server/src/main/resources/skills/*.yaml` content (only the sandbox VALIDATES diffs against them; no actual edits land).

## 5. Milestone acceptance bar

**Hard gates (close decision is PASS only if all clear):**

- [ ] **Tier-0 safety floor unchanged**: M-Auto-1A ships zero `server/` / Java / eval-code edits, so Tier-0 trivially preserved. Verified by `git diff main...HEAD --stat` showing zero lines under `server/`, `eval/`, `eval_interactive/eval_interactive/`, `data/`, `db/migration/`, `server/src/main/resources/`.
- [ ] **Java test baseline preserved**: `1183 / 1-inherited / 0 / 2` at M5-close `c9390dc` unchanged (no Java touched).
- [ ] **Python test baseline preserved**: `486 passed, 3 failed` at M5-close unchanged for the existing `eval_interactive/` test surface; new `autoloop/tests/` all PASS.
- [ ] **Dry-run end-to-end**: `python -m autoloop run --experiments 1 --dry-run` produces a complete `autoloop/results/runs/exp-1/{hypothesis.json,diff.yaml,sandbox_verdict.json,anti_hardcode_verdict.json}` without invoking mvn/Spring/eval and without writing to long-term memory layers (dry-run flag short-circuits).
- [ ] **Live iteration end-to-end**: `python -m autoloop run --experiments 1` (no dry-run) drives ONE full iteration to a verdict — meta-agent proposes a hypothesis, sandbox validates, anti-hardcode check passes, applier commits on a `autoloop/exp-1` branch, mvn compiles, Spring restarts on alt port, **three v1 suites run (bad_cases + anchor_outcome + shadow per §2 Dataset scope; anchor 159 excluded)**, tier_evaluator returns `{decision: keep|discard, ...}`, memory layers write. Regardless of keep/discard, the FULL pipeline must execute without crash.
- [ ] **S-Auto-4 Codex per-sub-sprint review `pass / 0`** (or `approve with downgrade-to-signal follow-up`) covering the anti-hardcode kernel + gaming checks. Reject verdict triggers fix-iteration sub-sprint before milestone close.
- [ ] **Anti-hardcode auto-check regression test**: ≥8 forbidden propose examples (including an explicit §1.7 example: `"IF user.message.contains('appeal') THEN active_use_case := UC-H"`) all auto-rejected at propose-stage; ≥4 clean soft-narrative examples (one per allowed field type) all pass.
- [ ] **Sandbox structural-reject coverage**: the 12+ fixture reject cases all REJECT; the 4 positive fixture cases all ACCEPT. Includes regression test that no diff touching `critical_steps[*].trace_check` / `mandatory_for` / `severity` / `id` / `guardrails` / `state_inheritance` / `tools_required` / `applicable_use_cases` / `applicable_phases` / `required_context_keys` is ever accepted.
- [ ] **Curated bad-case suite manual review pass (PRIMARY GATE per §5.6)**: deliver-agent + human run the bad-case suite at M-Auto-1A close (`parallel=1` per M5-close priority-bumped flake R-item). Expected trivial PASS since M-Auto-1A ships zero bot-behaviour change. Distribution must match M5-close: PASS×5 (cs001, cs014, cs029, cs066, fg5q) + IMPROVING×4 (alice, cs011, cs012, wmkb) + FAIL×3 (cs015, cs095, iwzx). Any deviation = stop and investigate (autoloop/ has somehow side-effected the bot).
- [ ] **Shadow regression-safety gate** (parity with M5 NEW shadow gate): deliver-agent runs `eval_interactive/case_specs_shadow/` once at M-Auto-1A close; expected no regression (no bot edit) and no new session-establishment failures beyond the M5-close-known `R-shadow-fixture-empty-form-session-create-400` (cs59s01/cs59s02 deterministic HTTP-400, categorically upstream of any bot/projection change).

**Observation-only (recorded; does not gate close):**

- Live iteration elapsed time (Spring restart + 4-suite eval): expected 15-30 minutes per iteration based on §9.1 of the proposal. If >40 minutes, surface as observation toward an R-item for M-Auto-2 optimization (a 6-hour overnight budget at 30 min/iter caps at ~12 iter; 40 min/iter caps at ~9 iter — pressure on the M-Auto-1B batch size).
- `new_semantic_hardcode_count` (§6) = **0** (M-Auto-1A ships no Java decision-path hardcode; the only soft "logic" is the anti-hardcode rejection regex, which is anti-§1.7 by construction, not pro-§1.7).
- `soft_signal_conversion_count` (§6): M-Auto-1A is infrastructure; soft-signal conversion happens in M-Auto-1B kept candidates onward.
- `planner_ownership_ratio`: unchanged (no runtime behaviour change).
- New Python test count: +90 to +150 expected across the 4 sub-sprints (sandbox + tier_evaluator + loop + anti-gaming + content + 6 gaming checks). Reproducible per `feedback_deliver_agent_cited_numbers_must_be_reproducible.md`.

## 6. Hard fences (milestone-level)

These are the structural guarantees that distinguish M-Auto-1A from any "auto-loop builds and accidentally ships a Skill YAML edit" failure mode. They are **the milestone**, not optional.

1. **No edits** to any file under `server/src/main/java/**`. The Runtime side stays byte-identical to M5-close `c9390dc`.
2. **No edits** to any file under `eval/src/main/java/**`, `eval_interactive/eval_interactive/**`, `eval_interactive/case_specs/**`, `eval_interactive/case_specs_shadow/**`. The Evaluator side stays byte-identical.
3. **No edits** to any file under `server/src/main/resources/{skills,prompts,scripts,config,mock}/**`. The agent runtime-loaded artefact set stays byte-identical. (Auto-loop VALIDATES diffs that target Skill YAMLs but never WRITES them in M-Auto-1A.)
4. **No edits** to `data/**`, `db/migration/**`, `docs/foundational/**`, `docs/runtime_freeze_and_risk_policy.md`, `docs/current/iteration_governance.md`. Foundational + governance stay frozen for this milestone.
5. **No edits** to sprint archives `docs/sprints/sprint-001-*` through `docs/sprints/sprint-053-*` or any prior milestone archive under `docs/milestones/`.
6. **No `git add -A`** by the dev agent. Stage only S-Auto-N scope files explicitly. Deliver-agent close-bundle artefacts (`docs/milestone_objective.md`, `docs/sprint_objective.md`, `docs/10-handoff.md`, `docs/action_bank.md`, etc.) bundled by the human at close per `feedback_commit_at_end_bundles_deliver_artefacts.md`.
7. **No cherry-pick to main** of any `autoloop/exp-N` branch in M-Auto-1A. The S-Auto-3 live-iteration close gate accepts ANY decision (keep or discard) on the test branch; main branch carries only the `autoloop/` infrastructure code, never a Skill YAML edit from an auto-loop iteration.
8. **No new Tier-0 invariant**. C2/C3 DEFER continues per M2-close verdict.
9. **No cross-file diff** by meta-agent ever (sandbox enforces). Single-Skill, single-field-class diff per iteration.
10. **No shadow-set leakage to meta-agent**. The shadow result fed back to the loop is the aggregate `{shadow_regression_detected: yes|no, drop_pct: <float>}` only. Per-case shadow failures NEVER reach `meta_agent/proposer.py`. Implemented as a structural firewall in `autoloop/scoring/tier_evaluator.py` (separate API surface for the loop vs. for the human audit `report` subcommand).
11. **No mutation of `eval_interactive/results/` schema**. Auto-loop produces parallel output under `autoloop/results/` only; eval invocation reuses existing schema unchanged.
12. **No editing of `docs/codex-findings.md` during M-Auto-1A sub-sprint execution**. The live `docs/codex-findings.md` is a scaffold; S-Auto-4 per-sub-sprint Codex writes to it at S-Auto-4 close; milestone-shared Codex review writes to it at M-Auto-1A close; archived to `docs/milestones/M-Auto-1A_codex-review.md` at milestone close per standard deliver-agent close-out.

### 6.1 OQ-S56.1 disposition (Sprint 56 / S-Auto-3 close 2026-05-27)

S-Auto-3 surfaced **OQ-S56.1**: one out-of-standard-scope edit to `eval_interactive/eval_interactive.yaml` (literal `bot.base_url: http://localhost:8080` → env-var indirection `bot.base_url: ${CSAGENT_BACKEND_URL}` + 7-line comment block documenting the OQ). Deliver-agent + human disposition 2026-05-27 at S-Auto-3 close: **BLESSED as in-scope**. Reasoning:

1. **Hard-fence letter check**: §6 item 2 names `eval_interactive/eval_interactive/**` (the inner Python module path) + `eval_interactive/case_specs/**` + `eval_interactive/case_specs_shadow/**`. The edited file is `eval_interactive/eval_interactive.yaml` — a **sibling of the inner `eval_interactive/eval_interactive/` directory**, not a child of it. The fence as written does NOT cover the top-level config.
2. **Spirit of the fence**: the milestone goal is to keep the eval harness Python module + case specs byte-identical so any per-iteration eval invocation is identical to a baseline eval invocation. The env-var indirection touches only the backend URL routing — it adds NO semantic logic, NO case-spec change, NO Python-module change, NO new dependency.
3. **Why required by the contract**: §5 "Live iteration end-to-end" acceptance bar requires the eval-interactive subprocess to route HTTP calls to the alt-port Spring spawned by the applier. eval-interactive's YAML config loader supports `${VAR}` substitution; without env-var indirection, the only path is a literal port number (defeats the alt-port mechanism) or a runtime YAML overwrite (more invasive than env-var indirection).
4. **Why structurally safe**: the loop orchestrator sets `CSAGENT_BACKEND_URL=http://127.0.0.1:<alt-port>` before invoking eval-interactive; `eval_runner.py` sets a fallback default `http://localhost:8080` if the env-var is unset (so standalone eval-interactive usage continues to work); the human's `.env.local` also carries the default. The change is benign across all invocation paths.
5. **Per-fence re-affirmation**: §6 item 2 still applies in full to `eval_interactive/eval_interactive/**` (inner module — byte-identical) + `eval_interactive/case_specs/**` (byte-identical) + `eval_interactive/case_specs_shadow/**` (byte-identical). The blessing is scoped to the **single 9-line diff** at `eval_interactive/eval_interactive.yaml` documented in `docs/sprints/sprint-056-handoff.md` §8.

This disposition is captured at S-Auto-3 close so the milestone-shared Codex review at M-Auto-1A close consumes a stable hard-fence list (top-level YAML edit is pre-blessed; Codex does not need to re-litigate it).

### 6.2 S-Auto-4 close + M-Auto-1A milestone-close gate state (2026-05-27)

All 4 sub-sprints of M-Auto-1A are CLOSED at this point:

- **Sprint 54 / S-Auto-1** — A — Clean PASS 2026-05-27; dev commit `85fc409`; archives `docs/sprints/sprint-054-{objective,handoff}.md`.
- **Sprint 55 / S-Auto-2** — A — Clean PASS 2026-05-27; dev commit `eb55322`; archives `docs/sprints/sprint-055-{objective,handoff}.md`.
- **Sprint 56 / S-Auto-3** — A — Clean PASS 2026-05-27; dev commit pending in `cf0127f` parent line; archives `docs/sprints/sprint-056-{objective,handoff}.md`.
- **Sprint 57 / S-Auto-4** — A — Clean PASS 2026-05-27; dev commit `1feef1f`; archives `docs/sprints/sprint-057-{objective,handoff}.md` + per-sub-sprint Codex prompt `compact/sprint-057-codex-review-prompt.md`.

**Gates satisfied at S-Auto-4 close (deliver-agent verified 2026-05-27)**:

- §5 "Java test baseline preserved" — `1183 / 1-inherited / 0 / 2` UNCHANGED from M5 close `c9390dc` (S-Auto-4 zero Java touch verified via `git diff --stat cf0127f..1feef1f -- server/ eval/src/main/java/` returns empty).
- §5 "Python test baseline preserved" — eval_interactive `486 passed, 3 failed` UNCHANGED; new autoloop tests all PASS (216 total, 147 baseline + 69 S-Auto-4 new).
- §5 "Anti-hardcode auto-check regression test" — calibration table satisfies the ≥8 forbidden auto-rejected + ≥4 clean pass bar with margin (11/11 + 4/4 + 2/2 FLAG); 11.76% FLAG_FOR_CODEX rate is well under the 25% warn threshold.
- §5 "Sandbox structural-reject coverage" — satisfied at S-Auto-1 close; carries through.

**Gates PENDING at S-Auto-4 close (block M-Auto-1A milestone close until satisfied)**:

1. **Per-sub-sprint Codex review of S-Auto-4** (§4.3 trigger #2) — REQUIRED. Human dispatches `compact/sprint-057-codex-review-prompt.md` against commit `1feef1f`. Codex must return `pass` (or `approve with downgrade-to-signal follow-up`). `reject` triggers fix-iteration sub-sprint extending M-Auto-1A.
2. **Milestone-shared Codex review on cumulative S-Auto-1..S-Auto-4 range** (§4.3 default) — REQUIRED at milestone close. Deliver-agent authors `compact/M-Auto-1A-review-prompt.md` AFTER the per-sub-sprint Codex returns `pass` (so the milestone-shared prompt can cross-reference the per-sub-sprint verdict notes for the cumulative scope claim).
3. **Bad-case suite manual review** (§5.6 primary gate) — REQUIRED. Deliver-agent + human run `eval_interactive/case_specs/bad_cases/` with `parallel=1` (per `R-bad-case-parallel-session-establishment-flakiness`). Distribution expected to match M5-close exactly (PASS×5 + IMPROVING×4 + FAIL×3) since M-Auto-1A ships zero bot-behaviour change. Any deviation = STOP and investigate (autoloop/ has somehow side-effected the bot).
4. **Shadow regression-safety rerun** (§5 acceptance bar; M5 NEW parity gate) — REQUIRED. Deliver-agent runs `eval_interactive/case_specs_shadow/` once at M-Auto-1A close; expected no regression beyond `R-shadow-fixture-empty-form-session-create-400`.
5. **Live iteration end-to-end** (§5 acceptance bar; OQ-S56.5) — REQUIRED. Human runs `uv run python -m autoloop run --experiments 1` with `AUTOLOOP_META_LLM_API_KEY` filled in `.env.local` + a built `server/` jar so `mvn -pl server -am spring-boot:run` succeeds + a clean working tree on the autoloop branch. Records keep/discard verdict + elapsed time (expected ~12-15 min). Regardless of keep/discard, the FULL pipeline must execute without crash.

**§6.1 OQ-S56.1 surface unchanged at S-Auto-4 close**: deliver-agent verified `git diff cf0127f..1feef1f -- eval_interactive/eval_interactive.yaml` returns empty. The blessing established at S-Auto-3 close holds for the cumulative scope claim consumed by the milestone-shared Codex.

**§6 hard fences re-affirmed across S-Auto-1..S-Auto-4 cumulative range**: deliver-agent verified `git diff --stat cf0127f..1feef1f` against all enumerated gated prefixes returns empty (only the new sub-sprint's own archive `docs/sprints/sprint-057-handoff.md` appears, in-scope per §6 item 5 carve-out for sprints not in the `001-053` range). The cumulative range covers all four sub-sprints; the milestone-shared Codex consumes this against the §6 fences + §6.1 OQ-S56.1 disposition.

**`config.fitness.scoring_code_baseline_sha` backfilled `"5177b674b5ad249d7c0e706f3c827010c8dab511240f239dbd3be6f689a0331c"`** by deliver-agent at S-Auto-4 close-bundle fix-up 2026-05-27. First fill attempt was literal git commit `"1feef1f"` per a literal reading of the S-Auto-4 contract phrasing "M-Auto-1A close commit SHA"; per-sub-sprint Codex review flagged it as a representation mismatch — `gaming._compute_scoring_code_sha()` compares against a SHA-256 content hash over the four scoring files (`tier_evaluator.py` + `eval_runner.py` + `baseline_loader.py` + `gaming.py`), not a git commit id. Resolved at close-bundle fix-up by invoking `_compute_scoring_code_sha()` directly and recording the actual content hash. Verified: `_check_scoring_code_drift(config=config)` returns `[]` (silent in steady state). Anchors `gaming.scoring_code_drift` to the S-Auto-4 dev-commit form of the four files; future commits touching any of those four files would trigger drift ERROR per D3.

## 7. R-items consumed / surfaced

**Consumed by M-Auto-1A (closed at close):** None expected. M-Auto-1A is infrastructure; it closes no semantic R-items.

**Coupled (read-only awareness):**
- `R-bad-case-parallel-session-establishment-flakiness` — M5-close priority-bumped (6× recurrence at parallel=1 across 5 cases and 3 milestones). S-Auto-2 `eval_runner.py` defaults bad_cases to `parallel=1` per this evidence; the M-Auto-1B overnight batch will accumulate further data points. Root-cause investigation (Java backend `/v1/demo/sessions` under load + simulator session-create error handling) is separate from M-Auto-1A scope.
- `R-iwzx-uc-k-vs-uc-h-routing-spurious-distress` — semantic_planner; will become a natural auto-loop optimization target in M-Auto-1B and beyond when meta-agent proposes edits to `discover_triage.procedure` / `critical_steps[].desc`. No M-Auto-1A action.
- `R-bad-case-suite-uc-ghij-seed-from-real-sessions` — new bad cases (when seeded) will automatically enter auto-loop fitness via `bad_cases/` programmatic case_passed count. No M-Auto-1A action; M-Auto-1B and beyond benefit.
- `R-shadow-fixture-empty-form-session-create-400` (M5 NEW) — cs59s01/cs59s02 HTTP-400 deterministic; tier_evaluator should NOT count this as bot-side regression. S-Auto-2 needs to handle the `terminal_outcome=null + turns_traced=0 + elapsed_ms=0` shape as "session-establishment failure, not bot-attributable" per the M5 S4 close documentation.

**Surfaced by M-Auto-1A (expected):**
- Potential R-item: per-iteration elapsed-time observation (if >40 min). Defer to M-Auto-1B confirmation.
- Potential R-item: any anti-hardcode auto-check false-positive surfaced by S-Auto-4 Codex (the `--explain` mode override would log the case; defer to M-Auto-1B for accumulated evidence).
- Potential R-item: any `autoloop/program.md` contract gap surfaced by S-Auto-1 dev session (the program.md is the human contract; dev finding ambiguity surfaces as deliver-agent fold-back at S-Auto-1 close).

**NOT consumed by M-Auto-1A (intentionally deferred):**
- M3-B Single Handover Orchestrator P0 (`D-single-handover-orchestrator`) — release_gate.md §1.1 blocker; human's M-Auto-1-first decision defers this; remains in the candidate slate for the milestone AFTER M-Auto-1A + M-Auto-1B.
- M5-carry-over projection-hygiene candidate (#4 C3 dedup + OQ-S52.4 `knowledge_hits` canonicalization) — independent milestone candidate.
- All other open R-items in `docs/action_bank.md` §5 unrelated to auto-evolution infrastructure.

## 8. Codex review plan (per §4.3)

**Default**: milestone-shared review at M-Auto-1A close. Single cumulative Codex pass over the commit range covering Sprints 54 / S-Auto-1 through S-Auto-4. Codex consumes:
- All 4 sub-sprint objectives + handoffs (`docs/sprints/sprint-NNN-{objective,handoff}.md`).
- This milestone objective (live during execution; archived to `docs/milestones/M-Auto-1A_objective.md` at close).
- The cumulative commit range produced by the 4 sub-sprints.
- The Python test baseline reproducibility check + S-Auto-4 anti-hardcode regression test coverage.
- The bad-case suite manual review notes from the M-Auto-1A close run.

**Per-sub-sprint trigger**: S-Auto-4 invokes §4.3 trigger #2. The anti-hardcode kernel + gaming checks ARE the §1.7 structural guard; Codex must independently verify the detector design BEFORE M-Auto-1B begins running it against real meta-agent outputs. Codex must return `pass` BEFORE M-Auto-1A close.

**Verdict set** (§4.1): `approve` / `approve with downgrade-to-signal follow-up` / `reject as semantic hardcode` / `needs human architecture decision`.

### 8.1 M-Auto-1A-shared review prompt outline (drafted at close)

1. **Cumulative scope claim**: review commits `<start>..<end>` covering S-Auto-1 through S-Auto-4 against this milestone objective.
2. **§4.1 nine-question kernel walk** (cumulative). Special attention to:
   - Q1 / Q5 / Q6: does the `autoloop/sandbox/anti_hardcode_check.py` regex / heuristic set itself encode §1.7-violating decision logic? (The defender must not itself be a hardcode pump.)
   - Q2: does any auto-loop code path attempt to create a new Tier-0 invariant?
   - Q4: does the meta-agent `propose.txt` prompt leak any eval case_id / session_id / known eval phrase that could prejudice the meta-agent's outputs?
3. **§1.7 boundary check** on every Python regex / pattern in `anti_hardcode_check.py` + `content_validator.py` + `gaming.py`.
4. **§4.2 sprint-close header** filled (`pass | fix_required | out_of_scope_review` + `blocking_count` + summary).
5. **Hard-fence verification** against §6 (12 items) via `git diff main...HEAD --stat` and targeted spot-checks.
6. **Reproducibility checks** per `feedback_deliver_agent_cited_numbers_must_be_reproducible.md` — every numeric claim in the deliver-agent's M-Auto-1A close package must be reproducible.
7. **Sandbox bypass spot-check**: Codex constructs ≥3 adversarial Skill-YAML diffs (e.g. unicode-confusable field name, YAML anchor abuse, comment-injection) and confirms the sandbox rejects each.
8. **Anti-hardcode bypass spot-check**: Codex constructs ≥3 adversarial propose outputs (e.g. semantic-equivalence via synonym swap, multi-line decomposition of an if-else, encoded keyword list) and confirms the auto-check rejects each.
9. **Bad-case suite human-judgment-gate respect**: Codex MUST NOT auto-PASS / auto-FAIL the bad-case manual review; Codex reads the deliver-agent + human manual review verdicts and verifies internal consistency.
10. **Deferred / non-blocking notes**: observations for M-Auto-1B planning.

## 9. Estimated milestone duration

**Calendar estimate (informational; not a gate):**

- S-Auto-1 / Sprint 54: 3 dev-days.
- S-Auto-2: 3-4 dev-days.
- S-Auto-3: 5 dev-days.
- S-Auto-4: 4 dev-days + per-sub-sprint Codex ~2 days.
- M-Auto-1A close: deliver-agent + human bad-case manual review + Codex milestone-shared review + close-out artefacts ~2-3 days.

**Total**: ~2.5-3 calendar weeks for the full milestone, assuming sub-sprints execute sequentially and S-Auto-4 Codex returns `pass` on first pass.

Risk to duration: S-Auto-4 Codex `reject` would trigger a fix-iteration sub-sprint (extending the milestone). Mitigation: deliver-agent pre-walks the anti-hardcode kernel test fixtures against the proposal §5.3 standard before S-Auto-4 commit; deliver-agent drafts a calibration table of accepted / rejected example proposes in the S-Auto-4 dev prompt.

## 10. Stop conditions (milestone-level)

**Stop signals (deliver-agent + human reassess scope; possibly invoke in-flight downgrade):**

1. S-Auto-1 sandbox proves it CANNOT distinguish a valid LLM-soft edit from a structural edit reliably (e.g. YAML AST library limitation, ambiguous YAML feature in production Skill files). Halt; re-scope to a constrained text-diff approach + a separate full YAML-AST follow-up.
2. S-Auto-2 tier_evaluator surfaces that the existing `results.json` schema is missing fields needed for lexicographic gating (e.g. Tier-2 mandatory-fail count not exposed per-case). Halt; surface as R-item; either patch the schema in a separate sub-sprint OR accept a degraded evaluator that defers some gates to M-Auto-1B.
3. S-Auto-3 live iteration cannot complete a single end-to-end run within 60 minutes (4× the upper estimate). Halt; investigate (Spring restart pathology, eval timeout, mvn cache miss). Either fix or accept and surface as a Stage-2 R-item.
4. S-Auto-4 Codex returns `reject as semantic hardcode` on >25% of the anti-hardcode kernel patterns themselves. Halt; the detector itself violates §1.7 — re-design.
5. M-Auto-1A close-time bad-case manual review surfaces ANY deviation from the M5-close distribution. Halt; investigate (autoloop/ has somehow side-effected the bot, even though it should not be able to).
6. M-Auto-1A close-time shadow rerun surfaces ANY new regression beyond `R-shadow-fixture-empty-form-session-create-400`. Halt; investigate.

**Continue signals (do NOT halt):**

- Per-iteration elapsed time observation: >30 min but <60 min — record as observation, plan for M-Auto-1B optimization.
- S-Auto-4 anti-hardcode kernel surfaces false-positives on Codex's adversarial spot-check at <25% rate: record as `--explain` override candidates; do NOT block close.
- Smoke composite_score moves due to LLM provider drift — per §5.5, observation only; not a stop signal.

## 11. Cross-milestone sequencing context

**Prior milestones**:
- **M5 (Observability Coherence)** — Clean PASS 2026-05-25. M-Auto-1A inherits the post-M5 architecture: four-tier verdict in `report.html`, per-invocation admin trace, Skill-registry-driven projection. M-Auto-1A does NOT modify any of these.
- **M4-Eval-Cleanup** — Clean PASS 2026-05-24. Smoke composite_score formally demoted to observation per §5.5. M-Auto-1A respects this — tier_evaluator never uses smoke composite_score as a gate.
- **M3-Eval (Four-Tier Pyramid)** — Clean PASS 2026-05-23. The four-tier evaluation framework M-Auto-1A consumes is shipped by M3-Eval. Tier-2 `critical_steps` Skill YAML field exists per M3-Eval S-Eval-3; its `desc` is one of the four allowed mutable fields for the auto-loop.
- **M2 (Skill Registry)** — Clean PASS 2026-05-18. The 6 Skill YAMLs M-Auto-1A's sandbox guards exist per M2.

**Next milestones (post-M-Auto-1A)**:
- **M-Auto-1B (Calibration)** — drafted at M-Auto-1A close. S-Auto-5 overnight batch + first kept-candidate human review + first cherry-pick to main + Stage-2 entry decision.
- **M3-B Single Handover Orchestrator (P0)** — release_gate.md §1.1 blocker; deferred per human's M-Auto-1-first decision; re-evaluated at M-Auto-1B close.
- **Projection-hygiene milestone candidate** (M5 carry-over #4 + OQ-S52.4) — independent track; deliver-agent + human reassess at M-Auto-1B close.
- **UC-G/H/I/J bad-case seeding, semantic-planner soft-signal extension, M3-Corpus, Latency / Skill-Tuning / Tier-0 re-evaluation** — remain in candidate slate.

## 12. Closure verdict

**Filled by deliver-agent + human jointly at M-Auto-1A milestone close per `iteration_governance.md` §8.4.**

(Empty — to be completed at close.)
