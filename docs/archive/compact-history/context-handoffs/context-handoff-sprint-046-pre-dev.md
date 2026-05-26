# Deliver-agent context handoff — Sprint 46 / S-Eval-5 pre-dev (post Option-A commit)

**Authored:** 2026-05-22 by deliver-agent (end of S-Eval-3+S-Eval-4 close-cycle session)
**For:** the next deliver-agent instance picking up at S-Eval-5 dev dispatch OR S-Eval-5 close OR M3-Eval milestone close
**Branch:** `refactor/remove-the-shackles`
**Repo:** `/Users/caoruixin/projects/csagent-latest`
**HEAD:** `a4a3bc6` (S-Eval-4 close-out + S-Eval-5 launch with Option A locked in)
**Working tree:** clean. Branch 8 commits ahead of `origin/refactor/remove-the-shackles`.

Read order on cold start: this file → `AGENTS.md` (auto-loads `iteration_governance.md` + `doc_governance.md` + `agent_context_guide.md`) → `compact/sprint-deliver-orchestrator.md` (deliver-agent role doctrine; do NOT duplicate here) → `docs/milestone_objective.md` (M3-Eval north star) → `docs/sprint_objective.md` (live S-Eval-5 contract; 12 sections) → `compact/sprint-046-dev-prompt.md` (S-Eval-5 dev brief).

---

## 1. 背景

- Repo builds a customer-service agent for an online classifieds marketplace. **LLM-first**: LLM owns semantic understanding (goal, drift, UC hypothesis, escalation posture, response strategy, customer-facing wording); Java/Python runtime owns deterministic boundaries (tool schema, capability, PII/safety floor, grounding floor, idempotency, persistence, trace/eval contract).
- Governance chain (auto-loaded via `@AGENTS.md`): `docs/current/doc_governance.md` + `docs/current/agent_context_guide.md` + `docs/current/iteration_governance.md` (Constitution §1 / Failure Brief §2 / Fix Layer §3 / Anti-Hardcode §4 / Eval Acceptance §5 incl. §5.5 smoke→observation + §5.6 / §5.6.1-§5.6.3 bad-case suite as human-judgment gate / Architecture Health §6 / §7 stanza / **§8 Milestone framework**).
- **Multi-agent collaboration**: Human → Research-agent (proposes) → Deliver-agent (plans + drafts contracts + prompts) → Dev-agent Claude Code (implements) → Review-agent Codex (reviews). Agents do NOT share chat history; cross-session continuity via repo docs + compact handoff files.
- **Active milestone**: M3-Eval — Coarse-to-Fine Evaluation Architecture (third milestone under §8 framework; M1 closed 2026-05-17, M2 closed 2026-05-18). **4 of 5 sub-sprints CLOSED Clean PASS; S-Eval-5 is the LAST M3-Eval sub-sprint.**

## 2. 目标

### 2.1 Milestone-level (M3-Eval; 4 of 5 closed)

Replace the implicit L1/L2/L3 equal-weighted eval scoring with a **four-tier pyramid**:

- **Tier-0** (unchanged hard gate): safety floor (`no_pii_leakage` / `no_human_only_tool_exposure` / `no_critical_policy_violation` / `escalation_compliance` / `phase_transition_validity`).
- **Tier-1** (NEW mandatory; coarsest): outcome — bad-case suite manual review (PRIMARY GATE per §5.6) + outcome-only `anchor_outcome` suite (S-Eval-1 ship; 12 cases) + (NEW S-Eval-5) supplementary L3 `user_goal_achievement` dim.
- **Tier-2** (NEW): per-UC critical-flow correctness via Skill `critical_steps[]` with LLM-visible `desc` (S-Eval-3 ship; 18 populated steps; 10:8 mandatory:advisory) + eval-side `trace_check` DSL (S-Eval-2 ship; 6 frozen primitives + parser-rejects-keyword/regex/message-content). **Structurally INERT in production eval-harness path until executor wiring fix lands (S-Eval-5 Option A AUTHORIZED).**
- **Tier-3** (advisory only): existing L2/L3 dims kept for trend tracking; NEVER gate `case_passed`. (Post-S-Eval-5: `relevance` / `tone_appropriateness` / `groundedness` demoted from composite to Tier-3 advisory.)

**Single-source-of-truth property** (proposal §5.2): `critical_steps[].desc` consumed by BOTH runtime LLM (via `ContextProjectionBuilder` rendering inside `phase_plan.skill`) AND eval-side scoring (via `SkillProcedureExtractor` evaluating `trace_check`). Same YAML; two consumers.

### 2.2 Current sub-sprint (Sprint 46 / S-Eval-5 — LAST M3-Eval sub-sprint)

S-Eval-5 ships (per live `docs/sprint_objective.md`):

1. **L3 dim demotion**: `relevance` / `tone_appropriateness` / `groundedness` from composite contributors → Tier-3 advisory (reuse S-Eval-1 `severity="advisory"` field convention). Demoted dims still record numeric scores; do NOT flip `case_passed` / `composite_score`.
2. **NEW `user_goal_achievement` L3 dim**: coarse 1-5 against `persona.user_goal_summary`; wired as Tier-1 supplementary advisory (does NOT flip `case_passed`).
3. **Rubric prompt updates**: R-l3-judge-form-context-trust-rubric (state `form_context.first_name` is a TRUSTED signal; don't penalise greeting by first name) + R-l1-source-citation-quality-rubric (require `canonical_url` OR article title; reject bare Salesforce IDs as sole citation).
4. **Close 4 M3-Eval R-items** in `docs/action_bank.md` §6: R-l3-judge-form-context-trust-rubric + R-l1-source-citation-quality-rubric + R-cs040-l3-review-intake-completion-semantics + R-cs038-l3-review-intake-efficiency.
5. **Monotone-relaxing check** (LOAD-BEARING): run smoke (14) + anchor (159) + anchor_outcome (12) with old vs new scoring code; assert 0 `case_passed=true → false` transitions.
6. **Executor wiring fix (Option A AUTHORIZED 2026-05-22)**: `eval_interactive/eval_interactive/batch/executor.py:252` — one-line edit to pass `tier2_result=` to `compute_composite(...)` + 1-2 NEW tests. Monotone-relaxing rerun doubles as first real-LLM Tier-2 surface (consumes OQ-S44.5 + OQ-S44.3+S44.4 + OQ-S45.5+S45.6 simultaneously).

**Codex review plan**: milestone-shared at M3-Eval close per `iteration_governance.md` §4.3 default (no per-sub-sprint trigger fires for S-Eval-5). Codex consumes the cumulative S-Eval-1 → S-Eval-5 commit range at M3-Eval close.

**Estimated dev duration**: 2-3 days. Expected bundle: 6-10 files.

## 3. 已确认事实

### Recent commits (chronological; last 9)

```
a4a3bc6  docs: S-Eval-4 close (A — Clean PASS) + S-Eval-5 launch + R-bad-case-suite-uc-ghij-seed-from-real-sessions opened   ← HEAD
8b7ff40  sprint 45 / S-Eval-4: bad-case suite expansion + trial milestone-close dry-run (NEW M3-Eval sub-sprint 4)
4dafaf5  docs: S-Eval-3 close (A — Clean PASS) + S-Eval-4 launch + per-sub-sprint Codex pass archived
01770ac  sprint 44 / S-Eval-3: populate critical_steps for the 6 Skills (NEW M3-Eval sub-sprint 3)
db19a47  docs: S-Eval-2 close (A — Clean PASS) + S-Eval-3 launch + v0_2 supersession landing
69ed77f  sprint 43 / S-Eval-2: Skill critical_steps schema + extractor + projection wiring
357e949  docs: S-Eval-1 close (A — Clean PASS) + M3-Eval setup-bundle (deliver-agent)
d91bd3d  sprint 42 / S-Eval-1: schema simplification + anchor_outcome suite
6ceae7c  上传知识库和 mock data
```

### Baselines (independently re-reproduced 2026-05-22)

- **Java**: `Tests run: 1163, Failures: 1, Errors: 0, Skipped: 2` UNCHANGED since S-Eval-2 close. Inherited `SystemPromptUserRequestedTiebreakerTest.systemPrompt_marksActiveUcTiebreakerExplicitly:53` persists per M2-close STATUS QUO; NOT attributable to any M3-Eval sub-sprint.
- **Python**: `5 failed, 396 passed in ~13s` via **`uv run python -m pytest`** (per OQ-S44.6 carry-over discipline; do NOT use `uv run pytest` — stale `.venv/bin/pytest` shebang under some checkouts exits 139). The 5 pre-existing failure classes: 2× `test_case_spec_overrides.py` + 1× `test_corpus_lint.py` + 2× `test_escalation_enum_sync.py`. UNCHANGED since S-Eval-3 close Codex independent reproduction.
- **§1.7 structural defence test**: 36 passed (15 hardcode-flavoured rejections + 11 positive-grammar + 10 malformed-syntax) UNCHANGED since S-Eval-2 ship.

### M3-Eval cumulative state (post-S-Eval-4 close)

- **Schema + scoring** (S-Eval-1 ship 2026-05-21; commit `d91bd3d`): 4 schema demotions in `case_spec/schema.py` + 3 scoring demotions in `hard_checks.py` / `outcome_checks.py` / `composite.py` + NEW `severity` field convention + 12-case `anchor_outcome/` suite + 20 regression tests.
- **Skill `critical_steps` schema + extractor + projection** (S-Eval-2 ship 2026-05-21; commit `69ed77f`): NEW `CriticalStep.java` record (5 fields + nested `Severity` enum) + `Skill.java` extended via backward-compat secondary 15-arg ctor + `SkillLoader.java` allowlist validation + `ContextProjectionBuilder.java` renders `[{id, desc}]` list-of-objects + NEW Python `skill_procedure_check.py` (hand-rolled recursive-descent DSL parser; 6 frozen primitives) + `composite.py` Tier-2 wiring + 36 §1.7 structural-defence tests.
- **`critical_steps` content** (S-Eval-3 ship 2026-05-22; commit `01770ac`): 18 LLM-visible `critical_steps` across 6 Skill YAMLs (`discover_triage`=3 / `confirm`=2 / `resolve_faq_grounded_answer`=5 / `resolve_intake_collect_and_handover`=5 / `escalate`=2 / `terminal`=1; mandatory:advisory = 10:8). **Codex per-sub-sprint review per §4.3 trigger #2 returned `decision: pass / blocking_count: 0`** first pass, single round. Codex independent §5.3 walk: 18× row-1 ✅. Anti-hardcode strategy: `desc` strings anchored on canonical tool names (from `tool-policy.yaml`) + canonical intake field names (from `IntakeFieldsRegistry.REQUIRED_FIELDS_BY_UC`) — NOT user-message keywords.
- **Bad-case suite** (S-Eval-4 ship 2026-05-22; commit `8b7ff40`): 11 new bad-case YAMLs sourced from 17 approved entries in `eval_interactive/case_spec_overrides.yaml`; total suite = **12** (1 Alice + 11 new). Per-primary-UC: `{UC-A: 2, UC-C: 3, UC-FP: 3, UC-D: 2, UC-K: 2}`. Tier distribution: core × 7 + scope-relevant × 4. D-dim coverage: D1 × 9, D2 × 2 (cs066/iwzx), **D3 × 0** (Alice retains; documented gap), D4 × 3 (cs011/cs014/cs001). **UC-G/H/I/J primary = 0** in 17-pool (UC-H secondary in Alice + wmkb only) — documented gap; **fallback path (a) AUTHORIZED**. Trial milestone-close dry-run used Option A (synthetic / mocked trace); 11/11 `closure_criterion` well-calibrated or acceptable; zero rewrites applied.

### Tier-2 production wiring state (POST-S-Eval-5 commit `a4a3bc6`; pre-dev)

- Tier-2 result computation: WIRED (S-Eval-2).
- Production executor pass-through: **NOT YET WIRED** at HEAD; `eval_interactive/eval_interactive/batch/executor.py:252` does NOT pass `tier2_result=` to `compute_composite(...)`. S-Eval-5 dev will land this fix per Option A AUTHORIZED.
- After S-Eval-5: Tier-2 mandatory results will flip production `case_passed` end-to-end; populated `critical_steps` content from S-Eval-3 becomes operative.

### Per-UC anchor distribution (S-Eval-1 §12.7 carry-over; STILL LOAD-BEARING)

UC-C 77 / UC-D 37 / UC-A 14 / UC-E 11 / UC-K 8 / UC-FP 6 / UC-B 5 / UC-F 1 / **UC-G UC-H UC-I UC-J = 0**. Intake-then-escalate UCs systematically under-represented across smoke + anchor + case-family + bad-case + override pool. Carry-over basis for the NEW R-item `R-bad-case-suite-uc-ghij-seed-from-real-sessions`.

## 4. 决策记录

### S-Eval-3 close (2026-05-22; commit `01770ac` + close-out `4dafaf5`)

- **Classification A — Clean PASS** (deliver-agent + human + Codex per-sub-sprint review).
- **Codex per-sub-sprint per §4.3 trigger #2**: `decision: pass / blocking_count: 0` on first pass, single round. Archive: `docs/sprints/sprint-044-codex-review.md`.
- **6 OQs disposed** (5 dev-surfaced + 1 NEW deliver-agent-surfaced):
  - **OQ-S44.1** (executor wiring gap at `executor.py:252`): OOSR-carry; consumed by S-Eval-5 Option A.
  - **OQ-S44.2** (active Skill/UC resolution semantics): bundled with OQ-S44.1; consumed by Option A.
  - **OQ-S44.3** (UC-FP `consult-moderation-context-on-removal-explanation` mandatory tightness): monitoring posture preserved; S-Eval-5 real-LLM rerun is calibration surface; potential S-Eval-3 fix-iteration trigger if legitimate no-context path surfaces.
  - **OQ-S44.4** (mandatory:advisory split 10:8): ACCEPTED per Codex calibration verdict.
  - **OQ-S44.5** (synthetic vs real-LLM verification): ACCEPTED for S-Eval-3; real-LLM run carry-over consumed by S-Eval-5 Option A.
  - **OQ-S44.6** (NEW; Python baseline reproducibility lapse — dev cited 9/392 via `uv run pytest`; Codex got 5/396 via `uv run python -m pytest`; Codex hypothesis: stale shebang): routed to M3-Eval-shared review queue; runner discipline preserved (S-Eval-4 + S-Eval-5 dev prompts MUST use `uv run python -m pytest`).
- **No new R-items at close.**

### S-Eval-4 close (2026-05-22; commit `8b7ff40` + close-out `a4a3bc6`)

- **Classification A — Clean PASS** (deliver-agent + human; Codex deferred to M3-Eval close per §4.3 default).
- **6 OQs disposed** (4 NEW + 2 carry-overs from S-Eval-3):
  - **OQ-S45.1** (3 legacy-migrated bad cases `wmkb`/`iwzx`/`fg5q` low-confidence): routed to M3-Eval close manual review queue.
  - **OQ-S45.2** (UC-G/H/I/J primary gap): **NEW R-item OPENED** — `R-bad-case-suite-uc-ghij-seed-from-real-sessions` in `docs/action_bank.md` §5 (status `proposed; impl deferred to M4+`).
  - **OQ-S45.3** (cs095 + anchor_outcome session co-use): intentional dual-encoding; flag for M3-Eval close confirmation.
  - **OQ-S45.4** (cs095 placeholder framing): fold-back candidate.
  - **OQ-S45.5** (carry-over OQ-S44.1+S44.2): consumed by S-Eval-5 Option A.
  - **OQ-S45.6** (carry-over OQ-S44.3+S44.4): real-LLM calibration via S-Eval-5 monotone-relaxing rerun.
- **Fallback path (a) AUTHORIZED** for UC-G/H/I/J gap (accept narrower coverage for M3-Eval).
- **Minor non-blocking observation**: dev handoff bundle file count off-by-one (cited 13; actual 14). Fourth+ instance of `feedback_deliver_agent_cited_numbers_must_be_reproducible.md` pattern; fold-back candidate at M3-Eval close.

### S-Eval-5 planning round (2026-05-22; pre-dev)

- **Option A AUTHORIZED** (human direction): executor wiring fix at `executor.py:252` lands in S-Eval-5 bundle as REQUIRED scope. Locked into `docs/sprint_objective.md` §2.6 + §5 + `compact/sprint-046-dev-prompt.md` §2.2 + §7.

### M3-Eval-shared Codex review queue (9 items at S-Eval-5 pre-dev)

To be addressed at M3-Eval milestone-shared Codex review (NOT S-Eval-5 close):

1. **OQ-S42.3** (S-Eval-1 D-2.2 demotion interpretation).
2. **OQ-S43.1** (S-Eval-2 DSL parser blocklist surface).
3. **OQ-S43.4** (S-Eval-2 `tool_event_seq` unsuccessful-dispatch `success=false` ignored).
4. **OQ-S44.6** (Python baseline runner-invocation confirmation).
5. **OQ-S45.1** (3 legacy-migrated bad cases real-LLM evidence — will be consumed by S-Eval-5 monotone-relaxing rerun under Option A).
6. **OQ-S45.3** (cs095 dual-encoding intentional pattern confirmation).
7. **OQ-S45.4** (cs095 placeholder framing fold-back).
8. **OQ-S45.5** (consumed by S-Eval-5 Option A; will close at S-Eval-5).
9. **OQ-S45.6** (consumed by S-Eval-5 monotone-relaxing rerun under Option A; will close at S-Eval-5 OR M3-Eval close).

## 5. 当前任务

**Stage**: S-Eval-4 close + S-Eval-5 launch committed (`a4a3bc6`). Working tree clean. **S-Eval-5 ready for dev dispatch.**

Deliver-agent close-out work — ALL COMPLETE:

- ✅ Sprint 44 §12 closure verdict appended (committed in `4dafaf5`).
- ✅ Sprint 45 §12 closure verdict appended (committed in `a4a3bc6`).
- ✅ S-Eval-3 contract archived to `docs/sprints/sprint-044-objective.md`; Codex review archived to `docs/sprints/sprint-044-codex-review.md`; codex-findings reset to scaffold (all in `4dafaf5`).
- ✅ S-Eval-4 contract archived to `docs/sprints/sprint-045-objective.md` (in `a4a3bc6`); codex-findings stays scaffold (no per-sub-sprint Codex for S-Eval-4; M3-Eval close milestone-shared will write).
- ✅ Live `docs/sprint_objective.md` is the S-Eval-5 contract (Option A locked into §2.6 + §5 + §6 + §7 + §9 + §10).
- ✅ Live `docs/10-handoff.md` §1 lead reflects S-Eval-5 PRE-DEV state + Option A AUTHORIZED.
- ✅ `docs/action_bank.md` §6 Sprint 44 + 45 close-action rows + §5 NEW R-item `R-bad-case-suite-uc-ghij-seed-from-real-sessions`.
- ✅ `compact/sprint-046-dev-prompt.md` (S-Eval-5 dev brief; Option A locked in).
- ✅ `compact/context-handoff-sprint-045-post-close.md` (cross-session continuity; reflects Option A authorized).

## 6. 下一步

### Immediate (human → dev cycle)

1. **Human dispatches Claude Code** dev session against `compact/sprint-046-dev-prompt.md`.

### S-Eval-5 dev session (Claude Code; estimated 2-3 dev-days)

2. Dev reads contract + dev prompt + carry-overs. Reads `llm_judge.py` end-to-end to locate L3 dim declarations + rubric prompt location.
3. Dev implements: 3 L3 dim demotion + NEW `user_goal_achievement` dim + 2 rubric prompt updates + 4 R-item closures (`succeeded-by` annotations in §6) + monotone-relaxing tests + **REQUIRED executor wiring fix** at `executor.py:252` + 1-2 NEW tests.
4. Dev runs monotone-relaxing check: smoke + anchor + anchor_outcome with old vs new scoring code; diff `case_passed` transitions. **Expected: 0 `true → false`.** If any surface, STOP per S-Eval-5 §10 #5.
5. Dev runs the real-LLM Tier-2 surface via the monotone-relaxing rerun (Option A): if UC-FP no-context legitimate path surfaces (contradicting S-Eval-3 mandatory step `consult-moderation-context-on-removal-explanation` per OQ-S44.3 / OQ-S45.6), STOP per §10 #2 — deliver-agent + human decide S-Eval-3 fix-iteration (loosen `trace_check` OR downgrade severity to advisory).
6. Dev writes handoff `docs/sprints/sprint-046-handoff.md` (LEAVES §12 EMPTY per `feedback_handoff_verdict_section_delegation.md`). Validates §3 numstat sub-tally arithmetic by re-summing per OQ-S44.6 fold-back.
7. Dev commits ONE bundle (~6-10 files).

### S-Eval-5 close cycle (deliver-agent + human; Codex deferred to M3-Eval close)

8. Deliver-agent + human classify per §4.1 verdict set. A → close-out bundle (handoff §12 + sprint-046-objective archive + 10-handoff §1 refresh + action_bank Sprint 46 row + 4 R-item closure annotations + post-close context handoff). B-fix-iterate → scope fix-iteration. Codex NOT dispatched per-sub-sprint.

### M3-Eval milestone close

9. **Bad-case suite manual review** as PRIMARY GATE per §5.6: deliver-agent + human walk through 12 cases; judge PASS / FAIL / IMPROVING; consider OQ-S45.1 + OQ-S45.6 real-LLM evidence from S-Eval-5 monotone-relaxing rerun.
10. **Milestone-shared Codex review** per §4.3 default. Deliver-agent drafts `compact/M3-Eval-review-prompt.md` covering cumulative S-Eval-1 → S-Eval-5 commit range + 9-item M3-Eval-shared review queue.
11. **Codex writes verdict** to `docs/codex-findings.md`. Deliver-agent + human classify.
12. **M3-Eval close artefacts** per §8.4: milestone objective archive to `docs/milestones/M3-Eval_objective.md`; Codex archive to `docs/milestones/M3-Eval_codex-review.md` (delete-and-add supersession); §6.5 closed-milestone index row in action_bank; 10-handoff §1 lead refresh (demote M3-Eval to Preceding milestone); 4 R-item closure annotations + `R-bad-case-suite-uc-ghij-seed-from-real-sessions` deferred to M4+.
13. **M4+ candidate selection**: pick from remaining slate (M3-B Single Handover Orchestrator if Salesforce calendar pressure / M3-Latency / M3-Skill-Tuning / M3-Tier-0 re-evaluation / M3-cleanup / NEW UC-G/H/I/J bad-case seeding milestone consuming the new R-item / M3-Corpus separate parallel-track).

## 7. 注意事项

### S-Eval-5 specifics

- **Option A executor wiring is REQUIRED, not optional.** `executor.py:252` IS in scope. Dev does NOT need to surface STOP signal #1 (struck per planning-round decision).
- **Monotone-relaxing check is the gating test.** Any `case_passed=true → false` transition triggers STOP per §10 #5 — do NOT silently force rubric to preserve PASS state.
- **Rubric updates are additive trust signals + tighter requirements, NOT keyword/regex/per-UC-matrix encoding.** R-l3-judge-form-context-trust: "`form_context.first_name` is trusted; don't penalise greeting by first name." R-l1-source-citation-quality: "citation needs `canonical_url` OR article title; reject bare Salesforce IDs as sole citation." Surface rubric narrative wording in handoff §5 for deliver-agent + human + Codex review at M3-Eval close.
- **4 R-item closures**: 2 via §2.5 rubric updates; 2 via S-Eval-1 schema simplification route + new tiered architecture. Append `succeeded-by` annotations to §6 close-action index; **do NOT delete R-item entries** (preserve historical record).
- **`R-bad-case-suite-uc-ghij-seed-from-real-sessions`** (NEW at S-Eval-4 close) — NOT in S-Eval-5 scope; stays open in §5 governance-track backlog; deferred to M4+.

### Process discipline (cumulative; STILL ACTIVE for S-Eval-5)

- **Numbers-cite per `feedback_deliver_agent_cited_numbers_must_be_reproducible.md`**: every count reproducible from `git show --numstat <commit>` / `wc -l` / `grep -c` / test-run direct output. **Python baseline MUST use `uv run python -m pytest`** per OQ-S44.6 carry-over. **§3 numstat sub-tally arithmetic MUST validate by re-summing** per the fourth+ instance fold-back observation.
- **Bundle policy per `feedback_commit_at_end_bundles_deliver_artefacts.md`**: dev does NOT stage deliver-agent files. Deliver-agent close-out bundled separately by human.
- **Handoff §12 closure verdict ownership per `feedback_handoff_verdict_section_delegation.md`**: dev leaves §12 EMPTY; deliver-agent + human (no Codex at S-Eval-5 close since deferred) jointly fill.
- **OOSR-with-packaging-note pattern per `feedback_out_of_scope_review_packaging_rollforward.md`**: if M3-Eval close surfaces findings out-of-M3-Eval-scope, classify as OOSR + roll forward to M4+.
- **No pre-decisions on OQs surfaced to Codex** per `feedback_constitution_discipline_vs_planning_anticipation.md` — at M3-Eval close Codex prompt, surface the 9-item M3-Eval-shared queue WITHOUT pre-decisions.
- **Delete-and-add supersession** per `feedback_packaging_codex_findings_supersession.md`: codex-findings.md → archive at M3-Eval close to `docs/milestones/M3-Eval_codex-review.md` then reset scaffold.

### Hard fences inherited (still ACTIVE for S-Eval-5)

- No new Tier-0 invariant. C2 + C3 candidates (M2 close) STAY DEFERRED until production trace observation at M3+.
- No edits to runtime semantic surfaces: `RuntimeIntentClassifier` / `IntentClassification` / `DriftResult` / `DriftDetector` / `UseCaseRouter` / `ClassifyUseCaseTool` / `PhaseEvaluator` / `AgentRunLoopImpl` / `ControlKernel` / `system_prompt.txt` / `tool-policy.yaml` / `IntakeFieldsRegistry` / `UseCaseRegistryService` / `ResolveDispositionEvaluator`.
- No edits to Skill abstraction (M2 + S-Eval-2 + S-Eval-3 Java + 6 Skill YAMLs + `ContextProjectionBuilder.java`).
- No edits to Tier-0 / Tier-1 / Tier-2 hard-gate scoring code (`hard_checks.py` / `outcome_checks.py` / `skill_procedure_check.py`). S-Eval-5 touches `llm_judge.py` + possibly `composite.py` (per S-Eval-1 `severity` pattern).
- No edits to CaseSpec schema + loader (S-Eval-1 territory).
- No edits to case fixtures (smoke / anchor / anchor_outcome / case-families / 12 bad cases / shadow / `case_spec_overrides.yaml`).
- No edits to eval harness / loader / simulator EXCEPT `executor.py:252` per Option A.
- No `iteration_governance.md` edit (narrow §5 acceptance-bar refinement may be discussed separately; default NO touch).
- No widening of `escalation_reason` enum (canonical 23 values).

### Carry-over queues

- **M3-Eval-shared Codex review queue (9 items)** — see §4 above.
- **M2 close fold-back queue (6 items)** STILL DEFERRED: Sprint 37 §7.8 typo + Sprint 38 OQ-S38.1 + Sprint 39 §6.2.5/§6.2.6 + Sprint 39 OQ-S39.7 + Sprint 41 OQ-S41.1 + Sprint 41 OQ-S41.4. Separate governance commit per `doc_governance.md` cadence; deliver-agent + human discretion on timing.
- **Stale-test R-item** `R-stale-test-escalation-enum-sync-v0_2-to-v0_3-migration` (S-Eval-1 close): stays `proposed; impl deferred to whichever milestone consumes it`. 3 implementation paths preserved verbatim in `docs/action_bank.md` §5. 2 failing tests in `tests/scoring/test_escalation_enum_sync.py` persist as known baseline.
- **NEW R-item** `R-bad-case-suite-uc-ghij-seed-from-real-sessions` (S-Eval-4 close): stays `proposed; impl deferred to M4+ planning`. 3 implementation paths preserved verbatim in `docs/action_bank.md` §5.

### Term definitions

- **§5.3 standard table** (proposal §5.3): 4-row table for evaluating LLM-visible `desc` strings (row 1 ✅ soft procedural; row 2 ⚠ soft diagnostic edge; row 3 ❌ hard if-else; row 4 ❌ keyword enumeration matrix). Codex 18× row-1 ✅ at S-Eval-3 close.
- **6 frozen DSL primitives** (S-Eval-2; `eval_interactive/eval_interactive/scoring/skill_procedure_check.py`): `accumulated_tool_results.<tool>` / `tool_event_seq(<a>) < tool_event_seq(<b>)` / `intake_state.fields_collected.contains(<field>)` / `session.<flag>_present` / `any_of(...)` / `all_of(...)`. Parser rejects anything else at parse time with `TraceCheckDSLSyntaxError`.
- **§5.6 bad-case suite manual review**: PRIMARY ACCEPTANCE GATE at milestone close; human-judgment, NOT programmatic.
- **`bad_case_metadata` + `closure_criterion`**: schema per `iteration_governance.md` §5.6 (refinement 2026-05-17 — human-judgment gate, NOT programmatic match).
- **`tier: core` vs `scope-relevant`** (per §5.6.1): core re-runs at every milestone close; scope-relevant re-runs only when milestone touches the relevant surface.
- **Monotone-relaxing check** (S-Eval-5 specific): no previously-PASS case flips to FAIL on the new rubric.
- **Option A** (S-Eval-5 planning round 2026-05-22): land executor wiring fix at `executor.py:252` in S-Eval-5 bundle (vs Option B which would have carried to M3-Eval close).
- **§4.1 verdict set**: `pass` / `approve with downgrade-to-signal follow-up` / `reject as semantic hardcode` / `needs human architecture decision`.
- **A — Clean PASS** classification: dev shipped contract end-to-end; no §6 hard fence touched; no §10 stop signal fired; §4.1 Q1-Q8 pass + Q9 N/A; no contract drift beyond pre-existing baseline + planned §7-a drifts; OQs disposed non-blocking; Codex (if reviewed) returned `pass` first round.

### Key files / paths

**Governance** (auto-loaded via AGENTS.md): `AGENTS.md` / `docs/current/doc_governance.md` / `docs/current/agent_context_guide.md` / `docs/current/iteration_governance.md`.

**Active sub-sprint state**:
- `docs/milestone_objective.md` (M3-Eval north star; §3 S-Eval-5 row + §5 milestone acceptance bar + §6 hard fences + §7 R-items + §8 Codex plan)
- `docs/sprint_objective.md` (live S-Eval-5 contract; 12 sections; Option A locked into §2.6 + §5 + §6 + §7 + §9 + §10)
- `docs/10-handoff.md` §1 lead
- `docs/codex-findings.md` (scaffold; M3-Eval milestone-shared writes here at close)
- `docs/action_bank.md` (R-items §3-§5 + §6 close-action index + §6.5 closed-milestone index)
- `compact/sprint-046-dev-prompt.md` (S-Eval-5 dev brief; Option A locked in)
- `compact/context-handoff-sprint-045-post-close.md` (prior post-close handoff; cumulative carry-overs)
- THIS FILE (`compact/context-handoff-sprint-046-pre-dev.md`)

**Archives (immutable)**:
- `docs/sprints/sprint-042-objective.md` + `sprint-042-handoff.md` (S-Eval-1)
- `docs/sprints/sprint-043-objective.md` + `sprint-043-handoff.md` (S-Eval-2)
- `docs/sprints/sprint-044-objective.md` + `sprint-044-handoff.md` + `sprint-044-codex-review.md` (S-Eval-3 with per-sub-sprint Codex archive)
- `docs/sprints/sprint-045-objective.md` + `sprint-045-handoff.md` (S-Eval-4; no Codex archive — deferred)
- `docs/milestones/M1_objective.md` + `M2_objective.md` + `M2-Skill_objective.md` (superseded) + `M2_codex-review.md`

**Bad-case suite (12 cases; PRIMARY GATE at M3-Eval close)**:
- `eval_interactive/case_specs/bad_cases/alice_uc_a_uc_h_misclass.yaml` (Alice; UC-A↔UC-H mis-classification regression guard; D1+D3)
- `eval_interactive/case_specs/bad_cases/cs012_uc_fp_late_phone_failure_path.yaml` + 10 more (S-Eval-4 new; see `docs/sprints/sprint-045-handoff.md` §4 for full per-case content map)
- `eval_interactive/case_specs/bad_cases/_manifest.md` (lifecycle ledger + S-Eval-4 calibration notes section)
- `eval_interactive/case_spec_overrides.yaml` (17 approved entries; read-only source pool)

**Code surfaces touched by M3-Eval** (LOAD-BEARING):
- `eval_interactive/eval_interactive/case_spec/schema.py` + `loader.py` (S-Eval-1)
- `eval_interactive/eval_interactive/scoring/hard_checks.py` + `outcome_checks.py` + `composite.py` + `skill_procedure_check.py` (S-Eval-1 + S-Eval-2)
- `eval_interactive/eval_interactive/scoring/llm_judge.py` (S-Eval-5 primary edit target)
- `eval_interactive/eval_interactive/batch/executor.py:252` (S-Eval-5 Option A REQUIRED edit)
- `server/src/main/java/com/gumtree/csagent/service/runtime/skill/` (Skill.java + CriticalStep.java + SkillLoader.java; S-Eval-2; UNCHANGED in S-Eval-5)
- `server/src/main/java/com/gumtree/csagent/service/runtime/ContextProjectionBuilder.java` (S-Eval-2; UNCHANGED in S-Eval-5)
- `server/src/main/resources/skills/<6 YAMLs>` (S-Eval-3 content; UNCHANGED in S-Eval-5)

**Source-of-truth artefacts**:
- `docs/solutions/m3_eval_milestone_proposal.md` (research-agent proposal; LOAD-BEARING §6 S-Eval-5 + §2 four-tier pyramid)
- `docs/runtime_freeze_and_risk_policy.md` §1 + §2 (Tier-0 invariants; UNCHANGED through M3-Eval to date)
- `docs/current/customer_service_tool_spec_v0_3.md`
- `docs/current/faq_grounding_contract.md`

### Deliver-agent memory caveat

The handoff text references `~/.claude/agent-memory/sprint-deliver-orchestrator/` feedback files. Filesystem path does NOT exist; the actual memory dir at `/Users/caoruixin/.claude/projects/-Users-caoruixin-projects-csagent/memory/` has 5 unrelated files. **The feedback file rules ARE authoritative via the governance docs + handoff files themselves** — load them from this handoff + AGENTS.md + iteration_governance.md transitive context. Do NOT block on memory-file loading.
