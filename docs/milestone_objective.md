---
title: Milestone M-Auto-1C — Auto-Evolution Calibration Continuation (applier mvn fix + first overnight + first cherry-pick)
doc_tier: current-runtime
status: current
implementation_status: not_started
source_of_truth: this file
last_reviewed: 2026-05-30
review_cadence: per milestone
supersedes: [docs/milestones/M-Auto-1B_objective.md]
superseded_by: null
notes: >
  M-Auto-1B Phase 3 close-bundle 2026-05-30 split per §8.5 directive
  ("A milestone that exceeds 5 sub-sprints is a signal that the
  milestone scope is too large; the deliver-agent SHALL split it at the
  next milestone planning round"). M-Auto-1B reached the 5-sub-sprint
  ceiling at S-Auto-7.1 / Sprint 061 (CLOSED 2026-05-29 A — Clean PASS
  at sub-sprint level; pre-flight env check + auto-reboot LANDED +
  operator-verified end-to-end against live pid=8613); Goal #3 smoke
  iter through Step 9 BLOCKED on the NEW OQ-S61.1 `applier.py:370`
  mvn module-selection bug = structurally a different substrate
  surface. M-Auto-1C is the 2-sub-sprint continuation milestone that
  finishes the M-Auto-1B substrate validation end-to-end.

  This is the SEVENTH milestone under `iteration_governance.md` §8
  framework. Codex Axis M7 at M-Auto-1B milestone-shared close
  2026-05-30 independently confirmed the §8.5 split is scope-clean:
  "M-Auto-1C's draft scope, S-Auto-7.2 applier fix plus S-Auto-8
  overnight/cherry-pick, is coherent continuation scope rather than
  unrelated work."

  **Core thesis**: M-Auto-1B inherits 5/15 unmet hard gates that all
  trace to the smoke iter through Step 9 being structurally blocked
  by OQ-S61.1. M-Auto-1C resolves OQ-S61.1 in S-Auto-7.2 (~half-day;
  1-line `applier.py:370-378` change + 1-2 tests) then exercises the
  now-validated substrate end-to-end in S-Auto-8 (the original
  S-Auto-6 / S-Auto-8 scope — first overnight batch + first human
  review + first cherry-pick to main; ~3-5 days). M-Auto-1C's
  acceptance bar inherits the 5 deferred M-Auto-1B gates: live iter
  end-to-end (Goal #4 + Goal #3 unblocked); pre-batch baseline drift
  envelope; first overnight batch ≥10 iterations; first human review;
  first cherry-pick decision via AskUserQuestion.

  **Why two sub-sprints**: 2 sub-sprints fits comfortably within the
  §8.5 5-sub-sprint ceiling; margin = 3 for fix-iteration if either
  surfaces second-order substrate brittleness. The clean substrate
  scope (S-Auto-7.2) → exercise (S-Auto-8) decomposition mirrors
  M-Auto-1B's intended S-Auto-5 (calibration) → S-Auto-6 (overnight
  + cherry-pick) flow that the original 2-sub-sprint plan was meant
  to deliver; M-Auto-1B's growth to 5 sub-sprints was substrate
  remediation overhead, and M-Auto-1C's tighter scope reflects the
  resolved-substrate state.

  **§8.1 conformance**: M-Auto-1C has 2 sub-sprints within 3-5
  ceiling.

  **Codex review plan (§4.3)**: milestone-shared at M-Auto-1C close
  DEFAULT. S-Auto-7.2 expected to NOT trigger per-sub-sprint review
  (1-line applier.py change isn't fenced surface or §1.7 borderline);
  S-Auto-8 conditional per-sub-sprint per §4.3 trigger #3 if cherry-
  pick candidate borderline-§5.3 surfaces (i.e., manual review finds
  a candidate at the "drift to keyword bot" edge where programmatic
  PASS but human judgment is split). Per-sub-sprint judged at
  cherry-pick decision point.

  **R-item coupling (consumed)**: M-Auto-1C consumes the
  **`OQ-S61.1-applier-mvn-module-selection-csagent-parent`**
  candidate (S-Auto-7.1 handoff §6 surfaced 2026-05-29; resolved at
  S-Auto-7.2 close; may or may not be formalized as a tracked R-item
  per deliver-agent + human discretion at milestone open). M-Auto-1C
  ALSO finalizes R-S58 disposition (defer-to-M-Auto-2 OR
  extend-with-S-Auto-9-Fix-D) based on S-Auto-8 overnight propose-
  distribution scan evidence.

  **R-item coupling (read-only awareness)**: `R-bad-case-parallel-
  session-establishment-flakiness` (bad_cases stays parallel=1;
  overnight batch accumulates further data points);
  `R-iwzx-uc-k-vs-uc-h-routing-spurious-distress` (natural overnight
  optimization target — meta-agent may propose `discover_triage` or
  `resolve_intake_collect_and_handover.yaml` edits to address it; a
  cherry-pick candidate touching the iwzx behaviour requires manual
  review against the iwzx-case trace at S-Auto-8 close);
  `R-shadow-fixture-empty-form-session-create-400` (cs59s01 / cs59s02
  stays excluded from bot-side regression count per S-Auto-2 baseline
  handling); **`R-eval-interactive-judge-score-never-populated`**
  (M-Auto-1B Phase 2 NEW; long-standing observability hygiene
  parallel to `observability_debt_pattern`; loader-counted
  `case_passed` continues as canonical signal during S-Auto-8 manual
  review).

  **Phase 2 drift signature from M-Auto-1B close** (M-Auto-1C
  planning input, NOT a milestone gate; Codex M-Auto-1B Axis M6
  trigger #1): anchor_outcome 7/12 → 3/12 + shadow 4/22 → 1/22 = 7
  one-direction regressions clustered on UC-D/E/F/FP auth/account/
  billing flows; consistent with LLM-provider drift; NOT bot-code
  regression (zero `server/` / Skill YAML / Java edits from M-Auto-1B
  agent-loop scope). Drift magnitude 7/34 = 20.6% vs M-Auto-1A close
  5/34 = 14.7%; elevated-but-in-envelope. **S-Auto-8 SHALL establish
  a ≥2 baseline rerun drift envelope BEFORE overnight kick-off** —
  this is M-Auto-1C's primary risk mitigation against false-positive
  "keeps" produced by overnight comparison against a single noisy
  baseline run.

  **Bad-case cherry-pick reference signals from M-Auto-1B Phase 2**:
  cs001_uc_c_mechanical_template_escalate (Skill YAML edit candidate
  on `resolve_faq_grounded_answer.yaml` procedure / critical_steps[*]
  .desc to gate `goal_impossible` terminal-state on policy-mandated
  `search_knowledge` completion) + wmkb_uc_a_trader_flag_secondary_uc_h
  (Skill YAML edit candidate on `resolve_intake_collect_and_handover
  .yaml` critical_steps[*].desc to gate handover on intake
  completion). These are REFERENCE SIGNALS (deliver-agent + human use
  during manual review at S-Auto-8 close), NOT pre-committed cherry-
  picks; the meta-agent owns proposal authorship and the cherry-pick
  decision is made via AskUserQuestion on the actual overnight kept-
  candidate slate.

  **What NOT in M-Auto-1C scope** (deferred):
  - Stage-2 unlock (templates.yaml / system_prompt.txt) — decision
    point AFTER M-Auto-1C close (same deferral as M-Auto-1B);
  - M3-B Single Handover Orchestrator P0 — deferred per human's
    M-Auto-1-first decision; re-evaluated at M-Auto-1C close;
  - Projection-hygiene milestone candidate (M5 carry-over) —
    independent track;
  - UC-G/H/I/J bad-case seeding (`R-bad-case-suite-uc-ghij-seed-
    from-real-sessions`) — natural optimization target once seeded;
    not pre-seeded;
  - `R-eval-java-module-retirement` — M-Auto-2+ governance-hygiene;
  - `R-eval-interactive-judge-score-never-populated` — M-Auto-2+
    observability-hygiene (canonical `case_passed` works; deferred);
  - Ambient-human-work governance-doc formalization (M-Auto-2+
    optional; Codex Axis M11 PASS without trigger at M-Auto-1B);
  - Deeper substrate fixes for Flyway lock contention / in-memory DB
    / Docker sandbox — M-Auto-2+ as observation (pre-flight from
    S-Auto-7.1 + applier mvn fix from S-Auto-7.2 accept current
    substrate ergonomic).
---

# Milestone M-Auto-1C — Auto-Evolution Calibration Continuation

## 1. Milestone class

**Multi-layer milestone, 2 coordinated sub-sprints.** Layer + §7-stanza + Codex breakdown:

| Sub-sprint | Layer (primary) | §7 stanza | Codex review |
|---|---|---|---|
| S-Auto-7.2 / Sprint 062 — applier.py:370 mvn module-selection fix | `infra` (substrate plumbing repair) | EXEMPT (pure-infra carve-out; self-walked for paper-trail) | Milestone-shared (default) — 1-line applier change isn't fenced surface or §1.7 borderline; no §4.3 trigger expected |
| S-Auto-8 / Sprint 063 — first overnight batch + first human review + first cherry-pick to main | `eval_spec` (per-iteration fitness on now-validated substrate; cherry-pick decision via AskUserQuestion + §5.6 manual review) | REQUIRED | Milestone-shared (default) UNLESS cherry-pick candidate borderline-§5.3 surfaces (then per-sub-sprint per §4.3 trigger #2 pre-milestone-close) |

S-Auto-7.2 expected to land in ~half-day with single-commit pattern (1-line diff in `autoloop/autoloop/sandbox/applier.py:370-378` + 1-2 tests + handoff). S-Auto-8 expected ~3-5 days (1-2 days execution + 1 overnight + 1-2 days deliver-agent + human manual review + cherry-pick decision + apply + close-bundle).

## 2. Goal

Finish what M-Auto-1B substrate-fixed but didn't validate end-to-end. At M-Auto-1C close, the M-Auto-1A/B substrate has been demonstrated to:

1. **Smoke iter through Step 9 with non-degenerate `tier_evaluator_verdict`** (Layer 0-4 all non-degenerate). S-Auto-7.2 retires Goal #3 + Goal #4 jointly (both blocked on OQ-S61.1 at M-Auto-1B close); the 14-step state machine executes against the real meta-agent LLM end-to-end with proper applier mvn invocation on the server submodule.
2. **Pre-batch baseline drift envelope established (≥2 rerun)** at S-Auto-8 BEFORE overnight kick-off, per Codex M-Auto-1B Axis M6 downgrade trigger #1. Median + IQR per-suite case_passed recorded; if drift exceeds 10/34 cases (~30%; 2× the M-Auto-1A close-day signature), HALT + deliver-agent + human jointly investigate per `iteration_governance.md` §10 stop condition.
3. **First overnight batch executed** at S-Auto-8: 10-20 iterations (target 10-20; final count ≥10 is the gate); 6-8h budget; per-iteration verdicts serialized to `autoloop/results/runs/exp-<N>/` + `experiments.jsonl` + `iterations.sqlite` + `lessons.md` (first K=10 lesson compaction triggers automatically).
4. **First human review of overnight kept candidates** via §5.6-style joint deliver-agent + human reading of per-turn traces; PASS / FAIL / IMPROVING / borderline-§5.3 classification recorded per candidate; cherry-pick-eligible / deferred-to-M-Auto-2+ / discarded classification recorded.
5. **First cherry-pick decision via AskUserQuestion**: human selects EXACTLY ONE candidate to cherry-pick to main OR 0 candidates with explicit "no human-approved candidate" justification. If cherry-pick lands: `python -m autoloop apply --experiment exp-<N>` Hybrid mode (cherry-pick + emit baseline patch + NO auto-commit per OQ-S55.1; human manually commits with standard deliver-agent footer); `config.fitness.baseline_dir` advances past the cherry-pick commit so the next milestone's baseline is post-cherry-pick.
6. **Final R-S58 disposition** (defer-to-M-Auto-2 OR extend-with-S-Auto-9-Fix-D OR resolve-in-S-Auto-8): decided at M-Auto-1C close based on S-Auto-8 overnight propose-distribution scan evidence (whether the U+200B zero-width bypass shape appears in real-meta-agent output at meaningful rate).

**What ships in main at M-Auto-1C close**:

- Zero `server/src/main/java/` / `eval/src/main/java/` / `eval_interactive/eval_interactive/**` / case_spec / case_specs_shadow / `server/src/main/resources/{prompts,scripts,config,mock}/**` edits (these stay byte-identical to M-Auto-1B close on the agent-loop scope; ambient-human-work commit `7871c62` from M-Auto-1B Phase 1.5 is now in main lineage but isn't M-Auto-1C scope).
- Substrate fix lands in `autoloop/autoloop/sandbox/applier.py` (1-line change to `applier.py:370-378` per the S-Auto-7.2 scope; smaller blast radius than alternatives; controlled fence override required since `applier.py` was previously hard-fenced; rebaseline NOT required since `applier.py` is not in the scoring-code fence-#13 4-file group `tier_evaluator.py / eval_runner.py / baseline_loader.py / gaming.py`).
- Optionally (S-Auto-8 cherry-pick): exactly ONE Skill YAML LLM-soft field edit on `server/src/main/resources/skills/*.yaml` (procedure / grounding_instruction / escalation_policy / critical_steps[*].desc only — sandbox-validated, anti-hardcode PASS, gaming 0-ERROR; human-approved via AskUserQuestion + §5.6 manual review; human-committed).
- Auto-loop result accumulation under `autoloop/results/runs/exp-*/`, `experiments.jsonl`, `iterations.sqlite`, `lessons.md` (first K=10 compaction lesson triggers during overnight); the configured `baseline_dir` advances IF cherry-pick lands.

**Dataset scope (v1 unchanged from M-Auto-1A/B)**: per-iteration fitness suite = `bad_cases` (12) + `anchor_outcome` (12) + `shadow` (22 — `_manifest.yaml` filtered) = 46 cases per iter, expected ~12-15 min per iteration. `anchor` 159 NOT consulted as per-iteration fitness; M-Auto-1C does NOT extend this scope.

**Layer 0 scope (v1 unchanged from M-Auto-1A/B)**: Python `hard_checks` Tier-0 family. The legacy `eval/src/main/java/com/gumtree/csagent/eval/` Java module remains superseded (governance-hygiene `R-eval-java-module-retirement` deferred to M-Auto-2+).

M-Auto-1C does NOT widen the mutable surface beyond Skill YAML LLM-soft fields (Stage-1 only). Stage-2 entry decision (templates.yaml / system_prompt.txt unlock) is reserved for a SEPARATE milestone AFTER M-Auto-1C close, judged against the evidence accumulated during M-Auto-1B + M-Auto-1C (cumulative ≥3 kept human-approved cherry-picks + 0 borderline §5.3 cases across both milestones would be a positive signal; M-Auto-1C itself does NOT pre-commit Stage-2 entry criteria).

## 3. Sub-sprint sequence

### S-Auto-7.2 / Sprint 062 — applier.py:370 mvn module-selection fix — NEXT

**Layer:** `infra` (substrate plumbing repair on the auto-loop applier subprocess invocation; structural; no semantic decision change; no projection / scoring semantic logic edit; no CaseSpec / judge change). **§7 stanza:** **EXEMPT** per pure-infra carve-out (self-walked for paper-trail). **Codex:** **DEFAULT milestone-shared at M-Auto-1C close** (no §4.3 trigger expected; planned scope lives in `autoloop/autoloop/sandbox/applier.py` 1-line change + 1-2 new/updated tests; not a fenced surface or §1.7 borderline. UPGRADE to per-sub-sprint Codex per §4.3 trigger #3 ONLY IF dev encounters substrate brittleness requiring new fence touch — dev STOP-and-surfaces before proceeding). **Estimated dev:** ~half-day + Codex deferred to milestone close.

**Scope (5 sentences):**

1. Resolve OQ-S61.1 at `autoloop/autoloop/sandbox/applier.py:370-378`. Current invocation `mvn -q -pl server -am spring-boot:run` applies the `spring-boot:run` goal to `csagent-parent` (`pom.xml` declares `<packaging>pom</packaging>` aggregating `<module>server</module>` + `<module>eval</module>` with no `mainClass`) BEFORE reaching the server submodule because `-pl server -am` brings the parent into the reactor. Implement ONE of three fix options (deliver-agent + human jointly pick at sprint planning round; recommended path is option (1) for smallest blast radius):
   - **(1) Drop `-am`**: `mvn -q -pl server spring-boot:run -Dspring-boot.run.arguments=...` (relies on parent + server already in local maven repo; reactor selects only server submodule; spring-boot:run applies only to server; PROS = 1-line `-am` removal, no cwd change; CONS = requires parent in local repo, which is normally the case in dev).
   - **(2) Invoke from server submodule cwd**: `cwd=str(root / "server")`; `cmd = ["mvn", "-q", "spring-boot:run", f"-Dspring-boot.run.arguments=--server.port={port}"]` (no `-pl`; no `-am`; PROS = cleanest reactor scoping; CONS = cwd change requires test fixture adjustments + potential path-handling subtleties for the existing free-port socket bind + actuator probe).
   - **(3) Spring-boot plugin scope**: `mvn -q -pl server -am org.springframework.boot:spring-boot-maven-plugin:3.2.5:run -Dspring-boot.run.arguments=...` (fully-qualified plugin coordinate constrains goal-to-server-only despite reactor still including parent; PROS = parent stays in reactor for `-am` dependency resolution; CONS = brittle to Spring Boot version bumps; requires version match against `pom.xml` parent's `<spring-boot.version>`).
2. Add 1-2 new tests in `autoloop/tests/test_applier_mvn_invocation.py` (NEW) OR extend existing `autoloop/tests/test_applier.py` (depending on current test layout per dev exploration). At minimum: (a) test that the subprocess command does NOT include `-pl server -am` in the reactor-selection style that triggers OQ-S61.1; (b) test that the chosen fix's command shape matches expectation (option-specific). Use `subprocess.Popen` mock fixtures (existing pattern in `autoloop/tests/`); do NOT require live mvn invocation in unit tests.
3. Run smoke iter end-to-end via the now-fixed applier path: with foreground :8080 stopped OR `--auto-reboot` invoked (S-Auto-7.1 pre-flight handles this), `python -m autoloop run --experiments 1` (NO `--dry-run`) drives the full 14-step state machine to a verdict on `auto-loop-branch`; Steps 6 (Spring spawn) + 7 (eval_runner) + 9 (tier_evaluator) all reach non-degenerate outputs; per-iter elapsed time recorded as FIRST measurement of full Spring-spawn + 46-case-eval cycle with proper module selection. **This jointly retires Goal #3 + Goal #4 from M-Auto-1B.**
4. Hard-fence verification: `git diff --stat <S-Auto-7.2-base>..HEAD -- autoloop/autoloop/scoring/ autoloop/autoloop/sandbox/anti_hardcode_check.py autoloop/autoloop/sandbox/content_validator.py autoloop/autoloop/loop.py autoloop/autoloop/meta_agent/ autoloop/autoloop/memory/ eval_interactive/ server/ eval/ data/ db/ server/src/main/resources/ docs/foundational/ docs/runtime_freeze_and_risk_policy.md docs/current/` returns empty. All M-Auto-1B §6 17 hard fences (inherited verbatim — see §6 below) honored except the NEW M-Auto-1C §6 fence #18 controlled override on `applier.py` substrate plumbing.
5. Author handoff `docs/sprints/sprint-062-handoff.md` per dev prompt §"Handoff requirements"; Codex review at M-Auto-1C close is default milestone-shared (NOT per-sub-sprint). Single-commit pattern preferred (~10-50 LOC including tests). If `applier.py` exploration surfaces a different fix layer that requires per-sub-sprint Codex (e.g., a non-applier fence touch), dev STOP-and-surfaces per dev prompt convention.

**Files in scope** (S-Auto-7.2 only):

- **EXTEND**: `autoloop/autoloop/sandbox/applier.py` (1-line fix at lines 370-378 per chosen option; controlled fence override).
- **NEW or EXTEND**: `autoloop/tests/test_applier_mvn_invocation.py` (NEW; OR extend existing applier test file with 1-2 new tests for the fixed mvn invocation shape).
- **NEW**: `autoloop/results/runs/exp-<N>/` (smoke iter end-to-end artefacts), `autoloop/results/experiments.jsonl` (append entry), `autoloop/results/iterations.sqlite` (insert row).
- **NEW**: `docs/sprints/sprint-062-{objective,handoff}.md` (objective archived at S-Auto-7.2 close per existing convention; handoff committed by dev at close).
- **CONDITIONAL** (if §4.3 trigger #3 upgrade fires): `compact/sprint-062-codex-review-prompt.md` (deliver-agent authors at S-Auto-7.2 close if needed).
- **NO touch** to `autoloop/autoloop/scoring/` (fence #13 reasserted at hash `22548e20…`), `autoloop/autoloop/sandbox/{anti_hardcode_check,content_validator,gaming}.py`, `autoloop/autoloop/loop.py`, `autoloop/autoloop/meta_agent/`, `autoloop/autoloop/memory/`, `autoloop/autoloop/preflight.py` (S-Auto-7.1 closed clean), `autoloop/autoloop/cli.py` (S-Auto-7.1 closed clean), `eval_interactive/` (loader.py controlled override from S-Auto-7 stays UNCHANGED; no further edits), `eval_interactive/case_specs/`, `eval_interactive/case_specs_shadow/`, `server/src/main/java/`, `eval/src/main/java/`, `data/`, `db/`, `server/src/main/resources/` (NO cherry-pick in S-Auto-7.2; that's S-Auto-8 ONLY), `docs/foundational/`, `docs/runtime_freeze_and_risk_policy.md`, `docs/current/`, sprint/milestone archives, `docs/codex-findings.md` scaffold.

### S-Auto-8 / Sprint 063 — first overnight batch + first human review + first cherry-pick to main — AFTER S-Auto-7.2

**Layer:** `eval_spec` (consumes per-iteration fitness verdict sequence on the now-validated substrate + repaired eval_runner + repaired applier; §5.6 manual review + cherry-pick are eval-side acceptance bars). **§7 stanza:** REQUIRED (semantic-touching via cherry-pick if it lands; same as original S-Auto-6 / S-Auto-8 framing). **Codex:** milestone-shared (default) at M-Auto-1C close UNLESS cherry-pick candidate borderline-§5.3 surfaces (then per-sub-sprint Codex pre-milestone-close per §4.3 trigger #2). **Estimated dev:** 1-2 dev-days execution + 1 overnight (6-8h auto-loop) + 1-2 days deliver-agent + human manual review + cherry-pick decision + apply + close-bundle.

**Scope (6 sentences):**

1. **Pre-batch baseline rerun ≥2 (NEW per Codex M-Auto-1B Axis M6 downgrade trigger #1)**: run the v1 46-case fitness suite (bad_cases ×12 parallel=1 + anchor_outcome ×12 parallel=4 + shadow ×22 parallel=4) **TWO OR MORE TIMES** before the overnight begins. Establish a **drift envelope** (per-suite median case_passed + IQR + bidirectional drift count) and record in `docs/sprints/sprint-063-handoff.md` §X "Baseline drift envelope". If baseline-vs-baseline drift exceeds 10/34 cases (~30%; 2× the M-Auto-1A close-day signature; matches M-Auto-1B Phase 2's 7/34 = 20.6% elevated-but-in-envelope rate), surface to deliver-agent + human BEFORE starting overnight — provider drift may have widened; halt or proceed-with-widened-envelope is a joint call. The ≥2 rerun discipline is M-Auto-1C's primary risk mitigation against false-positive "keeps" produced by overnight comparison against a single noisy baseline run.
2. **Overnight batch**: `python -m autoloop run --experiments 15` (target 10-20; budget 6-8h; sub-sprint accepts any final count ≥10) on `auto-loop-branch`. Each iteration writes to `autoloop/results/runs/exp-<N>/` + `experiments.jsonl` + `iterations.sqlite` + `lessons.md` (first K=10 lesson compaction triggers automatically if iteration count reaches K). Crash recovery handles transient LLM API errors; if total errors >50%, halt overnight + surface (loop crash recovery is M-Auto-1A S-Auto-3 substrate; M-Auto-1C does NOT add new recovery code).
3. **Propose-distribution scan for R-S58 evidence** (concurrent with §2 above): during overnight monitoring, accumulate the per-iteration propose-stage real-meta-agent output corpus. After overnight closes, scan the corpus for U+200B / Unicode `category() == "Cf"` format characters in propose text (especially around WHEN / WHENEVER + arrow shapes); record per-iteration occurrence count. This provides the evidence for R-S58 final disposition at M-Auto-1C close (defer-to-M-Auto-2 if 0 occurrences across 10-20 iterations OR extend-with-S-Auto-9-Fix-D if the bypass shape appears at meaningful rate).
4. **Morning: §5.6-style manual review of kept candidates**: deliver-agent + human read per-turn traces of EACH kept candidate's bad_cases + anchor_outcome runs (open `eval_interactive/results/<run-id>/` + `autoloop/results/runs/exp-<N>/`). For each kept candidate, judge PASS / FAIL / IMPROVING jointly (NOT programmatic alone), filter through the drift envelope from §1, and classify: **eligible for cherry-pick** (manual review PASS + no §5.3 borderline) OR **deferred to M-Auto-2+** (manual review PASS but borderline §5.3) OR **discarded** (manual review FAIL despite programmatic PASS). Use the M-Auto-1B Phase 2 REFERENCE SIGNALS as orientation: cs001 (`resolve_faq_grounded_answer.yaml` procedure / critical_steps[*].desc gating `goal_impossible` terminal-state on policy-mandated search) + wmkb (`resolve_intake_collect_and_handover.yaml` critical_steps[*].desc gating handover on intake completion) are existing failure shapes the meta-agent MAY propose candidates for; the manual review specifically validates against the cs001 + wmkb per-turn traces if such candidates surface, BUT does NOT pre-commit any specific cherry-pick.
5. **Deliver-agent surfaces candidate slate to human via AskUserQuestion**: each eligible cherry-pick candidate gets a row with (target_skill, target_field, edit_summary, programmatic verdict, manual review verdict, deliver-agent recommendation). Human selects EXACTLY ONE candidate to cherry-pick (or 0 candidates with explicit "no human-approved candidate" justification — close PASS still possible on other gates). For the selected candidate, execute `python -m autoloop apply --experiment exp-<N>` in Hybrid mode (cherry-pick + emit baseline patch + NO auto-commit per OQ-S55.1); human inspects `git status`, stages explicitly, commits manually with message `Sprint 063 / S-Auto-8 / M-Auto-1C — apply exp-<N> to main` and the standard deliver-agent footer.
6. **After cherry-pick (or 0-cherry-pick close decision)**: record final observations (per-iteration elapsed-time average; cumulative FLAG rate observed across overnight; `shadow_disagreement_rate` first measurement against baseline envelope; gaming flag count + severity distribution); update `autoloop/config.yaml` `fitness.baseline_dir` to advance past the cherry-pick commit (if any); finalize R-S58 disposition based on §3 evidence; recommend Stage-2 entry decision direction for M-Auto-2+ planning round (≥3 cumulative cherry-picks + 0 borderline §5.3 across M-Auto-1B + M-Auto-1C would be a positive signal).

**Files in scope** (S-Auto-8 only):

- **NEW**: `autoloop/results/runs/exp-<N>/` × (10-20 overnight iterations), `autoloop/results/experiments.jsonl` (append 10-20 entries), `autoloop/results/iterations.sqlite` (insert 10-20 rows), `autoloop/results/lessons.md` (append ≥1 K=10 lesson if iteration count reaches K), optional `autoloop/results/report-m-auto-1c.html` (per §10 observability).
- **EXTEND**: `autoloop/config.yaml` (`fitness.baseline_dir` advance if cherry-pick lands; `scoring_code_baseline_sha` unchanged since scoring code is hard-fenced #13 — still locked at `22548e20…`).
- **CONDITIONAL** (if cherry-pick lands): exactly ONE Skill YAML edit on `server/src/main/resources/skills/<skill_name>.yaml` to ONE LLM-soft field class (procedure / grounding_instruction / escalation_policy / critical_steps[*].desc) per the cherry-picked `autoloop/exp-<N>` branch.
- **NEW**: `docs/sprints/sprint-063-{objective,handoff}.md`.
- **NO touch** (UNCHANGED FROM S-Auto-7.2 hard-fences): `server/src/main/java/`, `eval/src/main/java/`, `eval_interactive/eval_interactive/`, `eval_interactive/case_specs/`, `eval_interactive/case_specs_shadow/`, `data/`, `db/migration/`, `server/src/main/resources/{prompts,scripts,config,mock}/` (skills/ is the SOLE writable path and only via the cherry-pick mechanism), `docs/foundational/`, `docs/runtime_freeze_and_risk_policy.md`, `docs/current/iteration_governance.md`, sprint/milestone archives, `docs/codex-findings.md` (scaffold; milestone-shared Codex writes at M-Auto-1C close).

## 4. Non-goals (explicit)

- M-Auto-1C does NOT unlock Stage-2 mutable surface (templates.yaml / system_prompt.txt / routing_prompt.txt). Stage-2 entry is a SEPARATE milestone decision after M-Auto-1C close (same deferral as M-Auto-1B), evaluated against cumulative M-Auto-1B + M-Auto-1C evidence (≥3 kept human-approved cherry-picks + 0 borderline §5.3 cases would be a positive signal).
- M-Auto-1C does NOT introduce a new Tier-0 invariant. C2/C3 candidates from M2 close remain DEFER unchanged.
- M-Auto-1C does NOT touch the Single Handover Orchestrator (M3-B P0; deferred per human's M-Auto-1-first decision; re-evaluated at M-Auto-1C close).
- M-Auto-1C does NOT modify `docs/runtime_freeze_and_risk_policy.md`, `docs/foundational/**`, `docs/current/iteration_governance.md`, `docs/teams/**`.
- M-Auto-1C does NOT modify `eval_interactive/eval_interactive/**` (inner module; loader.py controlled override from M-Auto-1B S-Auto-7 stays FINALIZED), `eval_interactive/case_specs/**`, `eval_interactive/case_specs_shadow/**`.
- M-Auto-1C does NOT modify `eval/src/main/java/**` (legacy Java replay; `R-eval-java-module-retirement` deferred to M-Auto-2+).
- M-Auto-1C does NOT modify the four `autoloop/autoloop/scoring/` baseline files (`tier_evaluator.py` / `eval_runner.py` / `baseline_loader.py` / `gaming.py`). Any change would trigger `gaming.scoring_code_drift.sha_changed` ERROR against the configured baseline SHA `22548e20ea50518b35c78cebddde891ddb46ea0452e32e49518ad4caab9188a9` (set at M-Auto-1B S-Auto-7 close).
- M-Auto-1C does NOT replace the §5.6 bad-case manual-review human-judgment gate. The auto-loop's per-iteration programmatic `case_passed` count is a PROGRAMMATIC SUB-SIGNAL fed to tier_evaluator; human review on kept candidates remains the primary gate at sub-sprint close (for cherry-pick decision) and milestone close (per §5.6 cadence).
- M-Auto-1C does NOT promote any gaming check from observation-only to gating. `gaming.observation_only_in_v1: true` stays; evidence on ERROR-severity flag accumulation from the overnight batch is M-Auto-2+ planning input.
- M-Auto-1C does NOT widen the per-iteration fitness suite beyond 46 cases. `anchor` 159 stays excluded per the §2 dataset scope decision frozen at M-Auto-1A.
- M-Auto-1C does NOT replace Codex anti-hardcode review. Manual review evidence is belt-and-suspenders to the milestone-shared Codex spot-check, NEVER a substitute.
- M-Auto-1C does NOT pre-seed new bad cases (UC-G/H/I/J seeding is `R-bad-case-suite-uc-ghij-seed-from-real-sessions` — independent track).
- M-Auto-1C does NOT alter shadow-firewall posture. Per-case shadow info NEVER reaches `meta_agent/proposer.py`; aggregate `regression_detected: y/n` is the only shadow signal the loop sees.
- M-Auto-1C allows AT MOST 1 cherry-pick to main (S-Auto-8 only; S-Auto-7.2 is substrate-fix with no cherry-pick scope). If overnight produces multiple eligible kept candidates, the human picks ONE; the rest stay on `autoloop/keep-<N>` branches awaiting M-Auto-2+ review.
- M-Auto-1C does NOT close `R-eval-interactive-judge-score-never-populated` (deferred to M-Auto-2+ observability hygiene). Loader-counted `case_passed` continues as canonical signal during manual review; judge_score 0.0 across all 46 cases is the long-standing observability surface, not a M-Auto-1C scope.

## 5. Milestone acceptance bar

**Hard gates (close decision is PASS only if all clear):**

- [ ] **Tier-0 safety floor unchanged**: M-Auto-1C ships zero `server/src/main/java/` / `eval/src/main/java/` / Python eval-code edits (apart from S-Auto-8 conditional cherry-pick on Skill YAML LLM-soft field). Verified by `git diff --stat <M-Auto-1B-close>..<M-Auto-1C-close> -- server/src/main/java/ eval/src/main/java/ eval_interactive/eval_interactive/ data/ db/migration/` returning empty (or only the conditional Skill YAML row if cherry-pick lands).
- [ ] **Java test baseline preserved**: `Tests run: 1183, Failures: 1, Errors: 0, Skipped: 2` UNCHANGED from M-Auto-1B close (the inherited `SystemPromptUserRequestedTiebreakerTest` per OQ-S41.5 STATUS QUO persists). If cherry-pick on Skill YAML lands and Java tests touch the affected Skill, expect 0 NEW failures (Skill YAML is read by `SkillLoader` validation; tests should pass under valid YAML).
- [ ] **Python test baseline preserved**: eval_interactive `3 failed, 486 passed` UNCHANGED. `autoloop/tests/` grows from 255 baseline by ~2-4 new tests at S-Auto-7.2 (applier mvn invocation tests); S-Auto-8 typically adds 0 new tests (execution-driven sub-sprint).
- [ ] **Live iteration end-to-end through Step 9 (NOW required; retires M-Auto-1B's 5 deferred gates)**: `python -m autoloop run --experiments 1` (NO `--dry-run`) drives at least ONE full iteration to a verdict; Steps 6 (Spring spawn) + 7 (eval_runner) + 9 (tier_evaluator) all reach non-degenerate outputs (Layer 0-4 verdict not null). Recorded in `autoloop/results/runs/exp-<N>/`. This is the gate that M-Auto-1B's Goal #3 + Goal #4 were blocked on; M-Auto-1C retires both.
- [ ] **Pre-batch baseline drift envelope at S-Auto-8 open (Codex M-Auto-1B trigger #1)**: ≥2 baseline reruns of the 46-case suite produce per-suite median + IQR envelope. If envelope width materially exceeds the M-Auto-1B Phase 2 7/34 (20.6%) signature, deliver-agent + human jointly decide proceed-with-widened-envelope vs halt-for-investigation. Decision + envelope recorded in `docs/sprints/sprint-063-handoff.md`.
- [ ] **First overnight batch executed at S-Auto-8**: ≥10 iterations completed end-to-end (target 10-20; final count ≥10 is the gate); per-iteration verdicts serialized to `experiments.jsonl` + `iterations.sqlite`. Iteration crashes during overnight acceptable IF total errors ≤50% (loop crash-recovery substrate from S-Auto-3 handles transient API errors).
- [ ] **First human review of overnight kept candidates**: deliver-agent + human jointly read per-turn traces of EACH kept candidate; PASS / FAIL / IMPROVING judgment recorded per candidate in `docs/sprints/sprint-063-handoff.md`. Candidates classified as cherry-pick-eligible / deferred-to-M-Auto-2+ / discarded.
- [ ] **First cherry-pick decision via AskUserQuestion**: human selects exactly ONE candidate to cherry-pick OR 0 candidates with explicit "no human-approved candidate" justification recorded in handoff. If cherry-pick lands, the apply Hybrid path executes correctly + human manual commit signature is present + `git log main` shows the new commit.
- [ ] **Curated bad-case suite manual review pass (PRIMARY GATE per §5.6)**: deliver-agent + human run the bad-case suite at M-Auto-1C close (parallel=1 per `R-bad-case-parallel-session-establishment-flakiness` M5 priority). Distribution: if cherry-pick landed, expected ≥1 case moves to IMPROVING or PASS (especially cs001 or wmkb if those reference signals are the cherry-pick target); if no cherry-pick, expected distribution matches M-Auto-1B Phase 2 close (bidirectional LLM-provider drift signature). Joint judgment recorded.
- [ ] **Shadow regression-safety gate (parity with M-Auto-1B)**: deliver-agent runs `eval_interactive/case_specs_shadow/` at M-Auto-1C close. Expected: no NEW shadow regression beyond `R-shadow-fixture-empty-form-session-create-400`. If cherry-pick landed, shadow drop must be ≤3% (per S-Auto-2 tier_evaluator Layer 4 threshold from M-Auto-1A).
- [ ] **OQ-S61.1 resolved**: S-Auto-7.2 commit verifies applier.py:370-378 invocation no longer applies `spring-boot:run` to csagent-parent; smoke iter through Step 9 with non-degenerate `tier_evaluator_verdict` recorded in `docs/sprints/sprint-062-handoff.md`.
- [ ] **R-S58 disposition recorded**: final disposition decision (defer-to-M-Auto-2 OR extend-with-S-Auto-9-Fix-D OR resolve-in-S-Auto-8) recorded in `docs/sprints/sprint-063-handoff.md` based on S-Auto-8 overnight propose-distribution scan evidence.
- [ ] **Milestone-shared Codex review at M-Auto-1C close**: `pass / 0` (or `approve with downgrade-to-signal follow-up`) over cumulative `<M-Auto-1B-close>..<M-Auto-1C-close>` range. Codex consumes both sub-sprint handoffs + the cherry-pick commit (if any) + the pre-batch baseline drift envelope evidence + the propose-distribution scan evidence.

**Observation-only (recorded; does not gate close):**

- Per-iteration elapsed-time average across overnight (expected 12-25 min; observation toward M-Auto-2 optimization if >40 min).
- `shadow_disagreement_rate` (§6 architecture-health metric) first measurement on the overnight kept-vs-discarded distribution.
- Cumulative FLAG rate across overnight propose-stage anti-hardcode checks.
- Gaming flag counts + severity distribution across overnight (observation-only_in_v1 per `autoloop/config.yaml`).
- Lessons compaction: `autoloop/results/lessons.md` should contain ≥1 LLM-distilled lesson if overnight reaches K=10 iteration count.
- `new_semantic_hardcode_count` (§6) = **0** (S-Auto-7.2 substrate fix is pure-infra; S-Auto-8 cherry-pick passes anti-hardcode detector at propose stage by sandbox guarantee).
- `soft_signal_conversion_count` (§6): if cherry-pick lands and the edit downgrades a hardcoded behaviour to a soft signal, count as 1; otherwise 0.
- `planner_ownership_ratio`: unchanged if no cherry-pick lands; if cherry-pick lands and the edit shifts a decision boundary from runtime to LLM, observation-record toward M-Auto-2 measurement infrastructure.

## 6. Hard fences (milestone-level)

M-Auto-1C inherits M-Auto-1B §6 17 hard fences with the loader.py + eval_runner.py controlled overrides FINALIZED, PLUS one NEW fence #18 controlled override on `applier.py` substrate plumbing. Most are unchanged; the changed / new items are flagged.

1. **No edits** to any file under `server/src/main/java/**`. Runtime byte-identical to M-Auto-1B close on the agent-loop scope.
2. **No edits** to `eval/src/main/java/**`, `eval_interactive/eval_interactive/**`, `eval_interactive/case_specs/**`, `eval_interactive/case_specs_shadow/**`. The loader.py controlled override from M-Auto-1B S-Auto-7 is FINALIZED; subsequent sub-sprints MUST NOT edit it. Case_spec YAML content byte-identical rule continues to hold. The `_manifest.md` lifecycle ledger is the expected close-bundle update surface per §5.6 (NOT a fence #2 violation; same clarification as M-Auto-1B §6 fence #2).
3. **Cherry-pick exception (UNCHANGED FROM M-Auto-1B)**: `server/src/main/resources/skills/*.yaml` files are **conditionally writable EXACTLY ONCE** in M-Auto-1C — via the S-Auto-8 cherry-pick mechanism only. Outside of cherry-pick, no edits. `server/src/main/resources/{prompts,scripts,config,mock}/**` stays byte-identical.
4. **No edits** to `data/**`, `db/migration/**`, `docs/foundational/**`, `docs/runtime_freeze_and_risk_policy.md`, `docs/current/iteration_governance.md`, `docs/teams/**`.
5. **No edits** to sprint archives `docs/sprints/sprint-001-*` through `docs/sprints/sprint-061-*` or any prior milestone archive under `docs/milestones/` (including the newly-archived M-Auto-1B archives at `docs/milestones/M-Auto-1B_objective.md` + `M-Auto-1B_codex-review.md`).
6. **No `git add -A`** by the dev agent. Stage only S-Auto-N scope files explicitly. Deliver-agent close-bundle artefacts bundled by the human at close.
7. **Cherry-pick to main is ALLOWED EXACTLY ONCE** during S-Auto-8 — for ONE human-approved kept candidate via `python -m autoloop apply --experiment exp-<N>` Hybrid mode. Any additional kept candidates stay on `autoloop/keep-<N>` branches awaiting M-Auto-2+ review.
8. **No new Tier-0 invariant**. C2/C3 DEFER continues per M2-close verdict.
9. **No cross-file diff** by meta-agent ever (sandbox enforces). Single-Skill, single-field-class diff per iteration.
10. **No shadow-set leakage to meta-agent**. Aggregate `{shadow_regression_detected: yes|no, drop_pct: <float>}` only.
11. **No mutation of `eval_interactive/results/` schema**. Auto-loop output stays under `autoloop/results/` only.
12. **No editing of `docs/codex-findings.md` during M-Auto-1C execution**. The live scaffold receives content at M-Auto-1C close (milestone-shared Codex writes); archived to `docs/milestones/M-Auto-1C_codex-review.md` at milestone close.
13. **No modification of `autoloop/autoloop/scoring/` baseline files** (`tier_evaluator.py` / `eval_runner.py` / `baseline_loader.py` / `gaming.py`): locked against M-Auto-1B S-Auto-7 close content hash `22548e20ea50518b35c78cebddde891ddb46ea0452e32e49518ad4caab9188a9`. Any change triggers `gaming.scoring_code_drift.sha_changed` ERROR.
14. **No hardcoding of specific eval case wording / user utterance / expected answer / case-status label** in any anti-hardcode detector rule or cherry-pick Skill YAML edit (per D2 detector self-discipline + §1.7).
15. **No LLM call inside detector or content_validator** (per D1 detector regex-heuristic-only rule).
16. **No promotion of `gaming.observation_only_in_v1`** from `true` to `false` in M-Auto-1C.
17. **At MOST 1 cherry-pick** during S-Auto-8 (fence #7 above is the exact rule; explicitly named here for redundancy).
18. **NEW (M-Auto-1C-only controlled override)**: `autoloop/autoloop/sandbox/applier.py` lines 370-378 are conditionally writable in **S-Auto-7.2 ONLY** for the OQ-S61.1 mvn module-selection fix (1-line change per the chosen option; controlled fence override BLESSED at M-Auto-1C planning round; Codex milestone-shared verifies at M-Auto-1C close). S-Auto-8 MUST NOT edit `applier.py`. All other lines of `applier.py` (the 505-LOC body excluding 370-378) stay byte-identical from M-Auto-1B close. The override is parallel to M-Auto-1B's fence #13 eval_runner.py override pattern — controlled scope, single line, paper-trail documented.

### 6.1 OQ-S56.1 disposition (inherited from M-Auto-1A §6.1 / M-Auto-1B §6.1; unchanged)

The blessing for `eval_interactive/eval_interactive.yaml` env-var indirection (`bot.base_url: http://localhost:8080` → `${CSAGENT_BACKEND_URL}` + 7-line comment block) carries forward to M-Auto-1C unchanged.

## 7. R-items consumed / surfaced

**Consumed by M-Auto-1C (closed at close)**:

- **`OQ-S61.1-applier-mvn-module-selection-csagent-parent`** — S-Auto-7.2 1-line fix at `applier.py:370-378` per chosen option. M-Auto-1C close = end-to-end smoke iter validation through Step 9.
- **R-S58 final disposition** — defer-to-M-Auto-2 OR extend-with-S-Auto-9-Fix-D OR resolve-in-S-Auto-8. Based on S-Auto-8 overnight propose-distribution scan evidence.

**Coupled (read-only awareness; not consumed):**

- `R-bad-case-parallel-session-establishment-flakiness` (M5-close priority-bumped) — S-Auto-8 overnight + close-day bad-case rerun use `parallel=1` per the recurrence evidence.
- `R-iwzx-uc-k-vs-uc-h-routing-spurious-distress` (semantic_planner) — natural overnight optimization target.
- `R-bad-case-suite-uc-ghij-seed-from-real-sessions` — NOT pre-seeded.
- `R-shadow-fixture-empty-form-session-create-400` — cs59s01/cs59s02 deterministic HTTP-400 stays excluded.
- `R-eval-java-module-retirement` — M-Auto-2+ governance-hygiene; NOT consumed.
- `R-eval-interactive-judge-score-never-populated` (NEW from M-Auto-1B Phase 2) — M-Auto-2+ observability hygiene; loader-counted `case_passed` is canonical during manual review; NOT consumed.
- M5 carry-over projection-hygiene candidate (#4 C3 dedup + OQ-S52.4 `knowledge_hits` canonicalization) — independent milestone candidate; not consumed.

**Surfaced by M-Auto-1C (expected)**:

- Potential R-item: per-iteration elapsed-time observation if average overnight elapsed >40 min.
- Potential R-item: any new bypass surfaced by S-Auto-7.2 or S-Auto-8 Codex review beyond R-S58 (would be downgrade-to-signal follow-up).
- Potential R-item: `shadow_disagreement_rate` first measurement (§6) — if rate is materially high, surface for M-Auto-2.
- Potential R-item: any gaming check ERROR-severity hits during overnight (observation-only_in_v1 records them; pattern accumulation drives M-Auto-2+ gating decision).
- Potential R-item: any new bad case discovered during S-Auto-8 manual review (a manual-review FAIL on previously-uncatalogued failure shape → new bad case → `bad_cases/_manifest.md` ledger entry per §5.6).
- Potential R-item: lessons_compactor LLM-distilled lesson surface — if K=10 lesson reveals a meta-prompt opportunity.

**NOT consumed by M-Auto-1C (intentionally deferred)**:

- M3-B Single Handover Orchestrator P0 — release_gate.md §1.1 blocker; remains in candidate slate; deliver-agent + human re-evaluate at M-Auto-1C close.
- M5 carry-over projection-hygiene candidate — independent track.
- All other open R-items in `docs/action_bank.md` §5 unrelated to auto-evolution calibration continuation or first cherry-pick.

## 8. Codex review plan (per §4.3)

**Default**: milestone-shared review at M-Auto-1C close. Single cumulative Codex pass over the commit range covering S-Auto-7.2 + S-Auto-8 + optional cherry-pick commit. Codex consumes:

- Both sub-sprint objectives + handoffs (`docs/sprints/sprint-062-{objective,handoff}.md` + `docs/sprints/sprint-063-{objective,handoff}.md`).
- This milestone objective (live during execution; archived to `docs/milestones/M-Auto-1C_objective.md` at close).
- The cumulative commit range produced by both sub-sprints + cherry-pick (if any).
- The Python test baseline reproducibility check + S-Auto-7.2 applier invocation test + S-Auto-8 overnight result artefacts.
- The bad-case suite manual review notes from the M-Auto-1C close run + shadow rerun result.
- The propose-distribution scan evidence for R-S58 disposition.
- The pre-batch baseline drift envelope evidence per Codex M-Auto-1B trigger #1 acceptance.

**Per-sub-sprint trigger expectations**:

- S-Auto-7.2: NOT expected to fire — 1-line applier.py change is fenced surface override but not §1.7 borderline; Codex milestone-shared verifies the fence #18 override at M-Auto-1C close. UPGRADE to per-sub-sprint ONLY IF dev encounters substrate brittleness requiring new fence touch (e.g., applier.py exploration surfaces a different fix layer).
- S-Auto-8: conditional per §4.3 trigger #2 ONLY IF the cherry-pick candidate touches a §1.7 borderline (i.e., manual review finds a candidate at "drift to keyword bot" edge where programmatic PASS but human judgment is split). Deliver-agent + human judge at cherry-pick decision point; if per-sub-sprint Codex is triggered, it covers S-Auto-8 commit range + cherry-pick commit BEFORE M-Auto-1C close.

**Verdict set** (§4.1): `approve` / `approve with downgrade-to-signal follow-up` / `reject as semantic hardcode` / `needs human architecture decision`.

## 9. Estimated milestone duration

**Calendar estimate (informational; not a gate):**

- S-Auto-7.2 / Sprint 062: ~half-day dev + Codex deferred to milestone close.
- S-Auto-8 / Sprint 063: 1-2 dev-days execution (pre-batch baseline ≥2 rerun + overnight kick + monitoring) + 1 overnight (6-8h auto-loop) + 1-2 days deliver-agent + human manual review + cherry-pick decision + apply.
- M-Auto-1C close: deliver-agent + human bad-case manual review + shadow rerun + milestone-shared Codex review + close-out artefacts ~1-2 days.

**Total**: ~1-1.5 calendar weeks for the full milestone, assuming sub-sprints execute sequentially, applier fix is straightforward (option 1 recommended), milestone-shared Codex returns `pass` on first pass, and no overnight halt-trigger fires.

Risk to duration:
- S-Auto-7.2 applier.py exploration surfaces a different fix layer requiring per-sub-sprint Codex — extends ~1 day. Mitigation: dev STOP-and-surfaces before proceeding so deliver-agent + human can authorize the wider scope.
- Overnight infrastructure halt (LLM API ban, mvn cache miss, Spring restart pathology) — could lose 1 overnight window; mitigation: pre-flight env check from S-Auto-7.1 catches obvious infra problems before overnight.
- First cherry-pick candidate manual review surfaces borderline §5.3 case requiring discussion — could extend manual review by 1-2 days; mitigation: deliver-agent prepares §5.3 decision rubric ahead of human review session.
- Pre-batch baseline drift envelope exceeds halt threshold (10/34) — could halt overnight + require upstream LLM-provider investigation; mitigation: deliver-agent + human jointly own the halt-or-widen-envelope decision.

## 10. Stop conditions (milestone-level)

**Stop signals (deliver-agent + human reassess scope; possibly invoke in-flight downgrade):**

1. S-Auto-7.2 applier exploration cannot resolve OQ-S61.1 with a 1-line change AND any alternative fix touches a non-allowed surface. Halt; surface to deliver-agent + human; possibly re-scope to a wider S-Auto-7.2 with controlled override expansion.
2. S-Auto-7.2 smoke iter crashes inside the 14-step state machine in a way that is NOT applier-mvn-related (e.g., a previously-unobserved Spring spawn pathology surfaces). Halt; root-cause investigation; possibly fix-iteration S-Auto-7.3 before S-Auto-8 begins.
3. S-Auto-8 pre-batch baseline drift envelope >10/34 cases (~30%; 2× the M-Auto-1A close-day signature) per Codex M-Auto-1B trigger #1 threshold. Halt; upstream LLM-provider behaviour shifted materially; pause overnight + investigate.
4. S-Auto-8 overnight halts before iteration 5 (e.g., total errors >50% within the first 5 iters). Halt; LLM API + infrastructure investigation; possibly recover the iters already completed.
5. S-Auto-8 overnight produces ZERO kept candidates AND deliver-agent + human jointly judge this is a meta-agent prompt issue (not just bad luck). Halt; surface as M-Auto-2 meta-prompt refinement R-item; M-Auto-1C close PASS still possible IF all OTHER gates pass.
6. M-Auto-1C close-time bad-case manual review surfaces a NEW failure shape (a previously-PASSing case now FAILing) NOT explained by either the cherry-pick edit OR LLM-provider drift. Halt; investigate.
7. M-Auto-1C close-time shadow rerun surfaces ANY NEW regression beyond `R-shadow-fixture-empty-form-session-create-400` AND beyond cherry-pick-attributable drift ≤3%. Halt; investigate; revert cherry-pick if necessary.

**Continue signals (do NOT halt):**

- Per-iteration elapsed time observation: >25 min but <40 min — record as observation, plan for M-Auto-2 optimization.
- LLM-provider drift in close-day bad-case / shadow rerun matching the M-Auto-1B Phase 2 signature (7/34 ≈ 20.6%) — observation only; bot byte-identical-or-cherry-pick-attributable check distinguishes drift from regression.
- Smoke composite_score moves due to LLM provider drift — per §5.5, observation only; not a stop signal.
- S-Auto-8 overnight produces 0 kept candidates DUE TO Tier-0 / Tier-1 / Tier-2 / shadow-drop discards (substrate working as designed). M-Auto-1C close PASS is possible with 0 cherry-pick + explicit "no human-approved candidate" justification, IF all other gates pass.

## 11. Cross-milestone sequencing context

**Prior milestones**:

- **M-Auto-1B (Auto-Evolution Calibration)** — A-with-acceptance-bar-revision 2026-05-30. 5/15 deferred hard gates inherited by M-Auto-1C (the live iter end-to-end + drift envelope + first overnight + first human review + first cherry-pick gates all blocked on OQ-S61.1 at M-Auto-1B close). M-Auto-1C resolves OQ-S61.1 + exercises the now-validated substrate end-to-end.
- **M-Auto-1A (Auto-Evolution Build)** — Clean PASS 2026-05-28. Substrate complete.
- **M5 (Observability Coherence)** — Clean PASS 2026-05-25. Provides observability surfaces S-Auto-8 human review uses.

**Next milestones (post-M-Auto-1C)**:

- **Stage-2 entry decision** — separate milestone determination after M-Auto-1C close. Evaluated against cumulative M-Auto-1B + M-Auto-1C evidence (≥3 kept human-approved cherry-picks + 0 borderline §5.3 cases would be a positive signal).
- **M3-B Single Handover Orchestrator (P0)** — release_gate.md §1.1 blocker; remains in candidate slate; deliver-agent + human re-evaluate at M-Auto-1C close.
- **Projection-hygiene milestone candidate** (M5 carry-over) — independent track.
- **M-Auto-2 (extended optimization / overnight scale-up)** + `R-eval-java-module-retirement` + `R-eval-interactive-judge-score-never-populated` (observability hygiene) + UC-G/H/I/J bad-case seeding + R-S58 if not resolved at M-Auto-1C + Latency / Skill-Tuning / Tier-0 re-evaluation — remain in candidate slate.

## 12. Closure verdict

*Filled by deliver-agent + human at M-Auto-1C close per `iteration_governance.md` §8.4. Placeholder during M-Auto-1C execution.*
