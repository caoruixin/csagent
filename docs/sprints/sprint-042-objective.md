---
title: Sprint 42 (NEW M3-Eval sub-sprint 1, S-Eval-1) — Schema simplification + outcome-only anchor suite
doc_tier: current-runtime
status: current
implementation_status: not_started
source_of_truth: this file (until S-Eval-1 close, then archived to docs/sprints/sprint-042-objective.md)
last_reviewed: 2026-05-20
review_cadence: per sub-sprint
supersedes: [docs/sprints/sprint-041-objective.md]
superseded_by: null
notes: >
  Sprint 42 is the FIRST sub-sprint of NEW Milestone M3-Eval (Coarse-to-
  Fine Evaluation Architecture; see `docs/milestone_objective.md`). Path
  1 research-driven; research-agent proposal at
  `docs/solutions/m3_eval_milestone_proposal.md` (2026-05-20); human
  locked 5 decisions per proposal §4. S-Eval-1 is the foundation sub-
  sprint: it loosens the CaseSpec schema (4 fields demoted to optional /
  scoring-effect-relaxed) and creates a new outcome-only `anchor_outcome`
  suite (10-15 cases). S-Eval-1 ships NO Skill YAML changes (those are
  S-Eval-2 + S-Eval-3 territory) and NO judge / rubric / L3 changes
  (those are S-Eval-5 territory).

  **Numbers-cite reconciliation against HEAD (2026-05-20)**:
  - proposal §6 S-Eval-1 step #1 + #3 referenced "30 anchor cases" for
    the backward-compat scope; actual count at
    `eval_interactive/case_specs/anchor/` is **159** YAMLs. This
    sub-sprint uses 159 as the load-and-run backward-compat scope.
  - proposal §6 S-Eval-1 step #3 says "10-15 cases" for the new
    `anchor_outcome` suite — this is unchanged at the actual count
    (the count is a target, not a HEAD count).

  **Codex review plan**: milestone-shared at M3-Eval close (per §4.3
  default). S-Eval-1 does NOT trigger per-sub-sprint Codex review
  (S-Eval-3 is the only M3-Eval sub-sprint with a per-sub-sprint
  trigger per §4.3 trigger #2).

  **Bundle policy**: this is a "small contract" sub-sprint focused on a
  single layer (`eval_spec`) with bounded scope (4 schema field
  demotions + 2-2 scoring demotions + 1 new directory of 10-15 YAML
  files + 1 governance text update). Dev ships in one bundle commit;
  deliver-agent + human bundle this objective archive + handoff in a
  separate close-out commit per `feedback_commit_at_end_bundles_deliver_artefacts.md`.

  **Cross-session continuity**: if a new dev-agent picks this up cold,
  the dev prompt at `compact/sprint-042-dev-prompt.md` is the entry
  point. Read order: AGENTS.md (auto-loaded) → this file →
  docs/milestone_objective.md (M3-Eval context) → proposal §6 S-Eval-1
  + §1.2 + §1.5 (why this sub-sprint exists) → AGENTS.md transitive
  governance docs.
---

# Sprint 42 (NEW M3-Eval sub-sprint 1, S-Eval-1) — Schema simplification + outcome-only anchor suite

## 1. Sub-sprint class

**Single-track semantic-touching sub-sprint, layer `eval_spec` per `iteration_governance.md` §3.2 Q6.** §7 stanza REQUIRED. Codex review default (milestone-shared at M3-Eval close).

Class breakdown:

- **Primary layer**: `eval_spec` (schema + scoring code + new outcome-only suite).
- **Touched layers (auxiliary)**: none in this sub-sprint. (Subsequent S-Eval-2 multi-layer touches `prompt_projection` + `eval_spec`; S-Eval-1 is `eval_spec`-only.)
- **Tier-0 invariant claim**: no new Tier-0 invariant; this sub-sprint relaxes existing eval-side gates that were never Tier-0.
- **§1.7 forbidden-list adjacency**: no. All changes are *relaxations* (required → optional; hard-gate → advisory). No new keyword, regex, if-else, or per-UC matrix is introduced. The new `anchor_outcome` suite contains ONLY outcome declarations + persona + closure_criterion — no procedural narrative, no expected tool sequence, no forbidden tool list.

## 2. Goal

Replace the implicit hard-gating of process-correctness CaseSpec fields with **schema convergence** that matches the M3-Eval four-tier pyramid (`docs/milestone_objective.md` §2):

- **Outcome 1** (schema): four `Expected` fields demoted from required / mandatory-effect to optional with default-off behaviour: `bot_handling_pattern` (required `str` → `Optional[str] = None`); `expected_tool_sequence` (kept as `list[str]` default-empty but scoring effect relaxed); `forbidden_tools` (kept as `list[str]` default-empty but scoring effect relaxed); `escalation_trigger` strict post-init coupling relaxed (if present, used for diagnostic; if absent on `should_escalate=true`, do NOT error — defer to acceptable_outcomes / Tier-1 outcome judgement).
- **Outcome 2** (scoring code): `hard_checks.py` `no_forbidden_tools` demoted to Tier-3 advisory (still records when `forbidden_tools` present; skipped when absent). `outcome_checks.py` `tool_sequence_match` demoted to Tier-3 diagnostic. `hard_checks.py` `escalation_reason_consistency` family-matching demoted to advisory when `escalation_trigger` absent on the spec. The per-case identical 7-item `scoring.outcome_checks` checklist demoted to advisory (case-specific opt-in via the existing list-of-strings; absent → no checks contribute to gate).
- **Outcome 3** (new suite): NEW directory `eval_interactive/case_specs/anchor_outcome/` with **10-15 cases**, one to three per UC, declaring ONLY `outcome_class` + `persona` (including `user_goal_summary`) + a NEW optional `closure_criterion: str`. Reuses `source_session_id`s from existing smoke / anchor where representative; NO new fixture content invented.
- **Outcome 4** (governance text): update `docs/current/iteration_governance.md` §5.5 to state "anchor_outcome is the second human-judgment surface beside the bad-case suite; smoke remains observation-only by design."

**Backward-compat invariant**: existing 14 smoke cases + **159 anchor cases** + 12 case-family directories + 1 bad case (Alice) load and run with `case_passed` unchanged from the demoted Tier-3 advisories. Tier-0 hard gates remain unchanged.

## 3. Non-goals (explicit)

S-Eval-1 does NOT:

1. Delete any existing field from `Expected`. (Kept for backward compat; deletion is a future fold-back, not this sub-sprint.)
2. Touch any L3 judge dim. (Deferred to S-Eval-5.)
3. Touch any Skill YAML. (Deferred to S-Eval-2 schema + S-Eval-3 content.)
4. Touch any case under `eval_interactive/case_specs/smoke/` or `eval_interactive/case_specs/anchor/` or `eval_interactive/case_specs/case_families/<existing>/` or `eval_interactive/case_specs/bad_cases/`. (Cascade fence.)
5. Touch `eval_interactive/case_spec_overrides.yaml`. (Source-only for S-Eval-4 bad-case selection.)
6. Touch any runtime Java surface (no `PhaseEvaluator` / `RuntimeIntentClassifier` / `SkillRegistry` / `ContextProjectionBuilder` / etc. change).
7. Touch `docs/foundational/` or `docs/runtime_freeze_and_risk_policy.md`.
8. Touch any sprint or milestone archive under `docs/sprints/sprint-*` or `docs/milestones/M*`.
9. Add new Tier-0 invariants. (Milestone-level §6 hard fence.)
10. Introduce any regex / keyword / if-else / per-UC matrix.
11. Widen the `escalation_reason` enum or change any of its 23 canonical values.
12. Modify the eval harness, loader, or simulator (`eval_interactive/eval_interactive/loader/`, `eval_interactive/eval_interactive/simulator/`).

## 4. Premise check (deliver-agent verified 2026-05-20)

- ✅ `eval_interactive/eval_interactive/case_spec/schema.py` exists; `Expected` dataclass carries all 4 target demotion fields verified at HEAD: `bot_handling_pattern: str` (required, no default — `schema.py:154`); `expected_tool_sequence: list[str] = field(default_factory=list)` (`schema.py:157`); `forbidden_tools: list[str] = field(default_factory=list)` (`schema.py:158`); `escalation_trigger: Optional[EscalationTrigger] = None` (`schema.py:155`). Post-init coupling at `schema.py:170-204` enforces `should_escalate` ↔ `escalation_trigger` strict pairing — this is the strictness to relax.
- ✅ `eval_interactive/eval_interactive/scoring/hard_checks.py` (37K), `outcome_checks.py` (22K), `llm_judge.py` (11K), `composite.py` (11K) all exist at HEAD.
- ✅ `eval_interactive/case_specs/smoke/` has **14** YAML files.
- ✅ `eval_interactive/case_specs/anchor/` has **159** YAML files (NOT 30 as proposal §6 mistakenly stated; reconciled in this contract).
- ✅ `eval_interactive/case_specs/case_families/<...>` has 12 family directories.
- ✅ `eval_interactive/case_specs/bad_cases/` has 1 case (Alice) + `_manifest.md`.
- ✅ `eval_interactive/case_specs/anchor_outcome/` does NOT exist yet (S-Eval-1 creates it).
- ✅ `docs/current/iteration_governance.md` §5.5 exists with the 2026-05-16 smoke-demotion text; S-Eval-1 will add the anchor_outcome reference.

## 5. Files in scope (Sprint 42 dev ships)

**Schema (1 file):**

- `eval_interactive/eval_interactive/case_spec/schema.py`:
  - `Expected.bot_handling_pattern`: `str` (no default) → `Optional[str] = None`. Adjust `__post_init__` to skip the non-empty-string check when None; preserve the check when a string is supplied.
  - `Expected.escalation_trigger` post-init coupling at `schema.py:190-204`: when `should_escalate=true` AND `escalation_trigger=None`, downgrade from `ValueError` to a non-fatal "diagnostic-only" path. **Constraint (human-approved 2026-05-21): pick the smallest backward-compatible implementation; do NOT introduce schema churn unless the relaxation requires it.** Concretely: prefer the minimum-edit path that loosens the existing `__post_init__` branch (e.g., replace the `raise ValueError` with a no-op or a `logging.warning` when `escalation_trigger is None` on `should_escalate=true`). Class-level flags, opt-in/opt-out parameters, or new dataclass-level facilities are LAST-RESORT only; document any deviation from the minimum-edit path in handoff §3 with explicit justification. Backward-compat invariant: all 159 anchor cases must continue to load.
  - NEW `closure_criterion: Optional[str] = None` field. **Constraint (human-approved 2026-05-21): preferred placement is `CaseSpec`-level adjacent to `expected` (so it is semantically a case-level closure target, not bound to the expected-bot-behaviour block).** Place at `CaseSpec` if schema-compat allows — verify by running existing loader on the 14 smoke + 159 anchor + 12 family + 1 Alice fixtures with the new field present and confirm no breakage. Fall back to `Expected`-level **only** if `CaseSpec`-level introduces an unavoidable schema-compat break; document the fallback rationale in handoff §3 with explicit justification.
  - DO NOT delete `expected_tool_sequence` or `forbidden_tools` or `bot_handling_pattern` from the schema. They remain present, with the demoted scoring effect handled in `hard_checks.py` / `outcome_checks.py`.

**Scoring code (3-4 files):**

- `eval_interactive/eval_interactive/scoring/hard_checks.py`:
  - `no_forbidden_tools`: when `expected.forbidden_tools` is non-empty, record the check as advisory (does not contribute to `case_passed`); when empty, skip entirely. Tier-3 emit only.
  - `escalation_reason_consistency`: when `expected.escalation_trigger` is None on a `should_escalate=true` spec, demote family-match strictness to advisory (record dim, do not flip gate).
- `eval_interactive/eval_interactive/scoring/outcome_checks.py`:
  - `tool_sequence_match`: demoted from L2 LCS contributor to Tier-3 diagnostic. Score still computed; not weighted into composite gate.
  - Per-case identical 7-item `scoring.outcome_checks` checklist: opt-in via existing `scoring.outcome_checks: list[str]` field (preserved for backward compat; absent → no checks contribute to gate; present → only the listed checks are evaluated as advisory).
- `eval_interactive/eval_interactive/scoring/composite.py`:
  - Update composite-scoring gate logic to read the demoted dims as advisory instead of contributors.
- (Possibly) `eval_interactive/eval_interactive/scoring/__init__.py` if a new "tier" enum or helper is added; otherwise untouched.

**New directory + 10-15 cases:**

- NEW `eval_interactive/case_specs/anchor_outcome/<case_id>.yaml` × 10-15:
  - 1-3 cases per UC across the canonical UC set (UC-A / UC-B / UC-C / UC-D / UC-E / UC-F / UC-FP / UC-G / UC-H / UC-I / UC-J / UC-K). Dev + deliver-agent coordinate UC coverage at sub-sprint planning round.
  - Each case carries: `case_id`, `source_session_id` (reused from smoke / anchor where representative; flagged as `synthetic` only if no real session matches the UC), `source_dataset`, `form_context`, `persona` (with `user_goal_summary`), `expected.outcome_class`, optional `expected.closure_criterion`. **NO** `expected.expected_tool_sequence` / `forbidden_tools` / `bot_handling_pattern` / `escalation_trigger` (omit or set to None).
  - Optional `_manifest.md` documenting per-UC coverage + reuse provenance.
- NO `_ACCESS_BOUNDARY.md` required (this is a visible suite per the loader contract, not shadow).

**Governance text (1 edit):**

- `docs/current/iteration_governance.md` §5.5: append one sentence stating "anchor_outcome (under `eval_interactive/case_specs/anchor_outcome/`) is the second human-judgment surface beside the curated bad-case suite at `eval_interactive/case_specs/bad_cases/`; smoke remains observation-only by design." Place near the existing §5.5 text about smoke demotion.

**Regression tests (Python):**

- One or more new test files under `eval_interactive/tests/` (or equivalent existing test location) asserting:
  - Schema loads existing 14 smoke + 159 anchor + 12 case-family + 1 Alice bad-case fixtures unchanged (no schema-side breakage).
  - `bot_handling_pattern: None` is accepted on a new `anchor_outcome` case.
  - `should_escalate=true` with `escalation_trigger=None` is accepted (no `ValueError`) when the loosened coupling is applied.
  - A demoted advisory does NOT flip `case_passed` (parity test: previously-passing case stays passing).
  - A NEW anchor_outcome case loads cleanly through the existing loader.

**Handoff document:**

- `docs/sprints/sprint-042-handoff.md` (dev-authored at sub-sprint close); follows 12-section template per Sprint 35 / 41 precedent.

## 6. Files NOT in scope (hard fences)

**1. M2-landed Skill surfaces — NO touch:**

- `server/src/main/java/com/gumtree/csagent/service/runtime/skill/Skill.java`
- `server/src/main/java/com/gumtree/csagent/service/runtime/skill/SkillRegistry.java`
- `server/src/main/java/com/gumtree/csagent/service/runtime/skill/SkillLoader.java`
- `server/src/main/java/com/gumtree/csagent/service/runtime/skill/SkillStateBus.java`
- `server/src/main/java/com/gumtree/csagent/service/runtime/skill/SkillGuardrailDispatcher.java`
- `server/src/main/java/com/gumtree/csagent/service/runtime/skill/StateInheritance.java`
- All 6 Skill YAMLs under `server/src/main/resources/skills/` (discover_triage / confirm / resolve_faq_grounded_answer / resolve_intake_collect_and_handover / escalate / terminal)

**2. Runtime surfaces — NO touch:**

- `RuntimeIntentClassifier.java`, `IntentClassification.java`, `DriftResult.java`, `DriftDetector.java`, `UseCaseRouter.java`, `ClassifyUseCaseTool.java`
- `PhaseEvaluator.java`, `ContextProjectionBuilder.java`, `AgentRunLoopImpl.java`, `ControlKernel.java`
- `system_prompt.txt`
- `server/src/main/resources/config/tool-policy.yaml`

**3. Existing eval surfaces — NO touch:**

- Any existing case under `eval_interactive/case_specs/smoke/`, `eval_interactive/case_specs/anchor/`, `eval_interactive/case_specs/case_families/<existing>/`
- The existing Alice bad case `eval_interactive/case_specs/bad_cases/alice_uc_a_uc_h_misclass.yaml`
- `eval_interactive/case_specs/bad_cases/_manifest.md` (S-Eval-4 territory)
- `eval_interactive/case_specs_shadow/case_families/<existing>/` (shadow class per `_ACCESS_BOUNDARY.md` — dev does not read or edit shadow)
- `eval_interactive/case_spec_overrides.yaml` (S-Eval-4 read-only source)
- `eval_interactive/eval_interactive/loader/`, `eval_interactive/eval_interactive/simulator/` (stable per Sprint 28 / Sprint 32 precedent)

**4. Judge surfaces — NO touch in S-Eval-1 (S-Eval-5 territory):**

- `eval_interactive/eval_interactive/scoring/llm_judge.py` (S-Eval-5 will repositioning L3 dims + add `user_goal_achievement`)
- Any L3 rubric prompt text

**5. Governance and archives — NO touch:**

- `docs/foundational/`
- `docs/runtime_freeze_and_risk_policy.md`
- `docs/customer_service_tool_spec_v0_2.yaml`
- `docs/sprints/sprint-001-*` through `docs/sprints/sprint-041-*`
- `docs/milestones/M1_objective.md`, `M2_objective.md`, `M2-Skill_objective.md`, `M2_codex-review.md`, `M1*`
- `docs/milestone_objective.md` (M3-Eval; deliver-agent territory)
- `docs/codex-findings.md` (review-agent territory; scaffold at S-Eval-1 start)

**6. Action bank — limited touch:**

- `docs/action_bank.md`: dev SHOULD NOT close R-items in S-Eval-1 (R-item closure for the 4 R-items is S-Eval-5 scope). Dev MAY append a Sprint 42 close-action index row in §6 close-action index at sub-sprint close per existing convention.

## 7. Bundle policy

S-Eval-1 dev ships in **ONE bundle commit** containing:

- Schema edit (`schema.py`).
- Scoring code edits (`hard_checks.py`, `outcome_checks.py`, `composite.py`, optional `__init__.py`).
- NEW `anchor_outcome/` directory with 10-15 YAML files (+ optional `_manifest.md`).
- Governance text edit (`docs/current/iteration_governance.md` §5.5 sentence).
- New regression tests.
- Dev handoff `docs/sprints/sprint-042-handoff.md`.

Deliver-agent + human bundle (separately, post-close):

- This sub-sprint contract archive (`docs/sprints/sprint-042-objective.md`).
- Live `docs/sprint_objective.md` replaced with S-Eval-2 contract (when S-Eval-2 planning round completes).
- `docs/10-handoff.md` §1 lead refresh (demote S-Eval-1 to Preceding sub-sprint; set S-Eval-2 as Current).
- `docs/action_bank.md` Sprint 42 close-action index row (if dev did not already append).

Per `feedback_commit_at_end_bundles_deliver_artefacts.md`: dev does NOT stage deliver-agent close-out files; human bundles at deliver-agent's commit.

## 8. Layer-classification + anti-hardcode stanza (per §7; REQUIRED)

**Target failure layer:** `eval_spec`.

**Tier-0 invariant:** This sub-sprint adds no Tier-0 invariant. The CaseSpec schema and scoring code are not Tier-0 surfaces per `docs/runtime_freeze_and_risk_policy.md` §1/§2; they are eval-side contract per `iteration_governance.md` §3.2 Q6.

**Semantic hardcode:** No semantic hardcode introduced. All changes are *relaxations*: four `Expected` fields move from required / mandatory-effect to optional / advisory; two `hard_checks` and one `outcome_checks` dimension demote from gate-contributor to advisory. The new `anchor_outcome` suite contains ONLY outcome declarations + persona + closure_criterion (no procedural narrative, no tool sequence, no forbidden tool list). No new keyword, regex, if-else, or per-UC matrix is introduced anywhere.

**Generalization coverage:**

- **Target**: 4 schema fields demoted (`bot_handling_pattern`, `expected_tool_sequence` scoring effect, `forbidden_tools` scoring effect, `escalation_trigger` post-init strictness) + 2-3 scoring dims demoted + 10-15 NEW `anchor_outcome` cases spanning the canonical 12 UCs.
- **Neighbor**: existing 14 smoke + **159 anchor** + 12 case-family directories + 1 Alice bad case must continue to load and run unchanged at the gate level.
- **Negative**: a CaseSpec with deliberately-wrong `expected_tool_sequence` must NOT cause `case_passed: false` post-demotion (parity test). A CaseSpec with `should_escalate=true` and `escalation_trigger=None` must load without raising (post-demotion).
- **Shadow**: downstream sub-sprints (S-Eval-2 through S-Eval-5) exercise the loosened schema cumulatively at M3-Eval close. S-Eval-1 does NOT touch shadow CaseSpecs.

## 9. Success metrics (per `iteration_governance.md` §5)

**Hard gates (sub-sprint close PASS only if all clear):**

- [ ] Schema loads existing 14 smoke + 159 anchor + 12 case-family + 1 Alice bad case fixtures **unchanged**. Verified by regression-test suite.
- [ ] `case_passed` parity: every previously-PASS case stays PASS; every previously-FAIL case stays FAIL on the demoted dimensions. (No false flips. Tier-3 advisory scores may differ; gate must not.)
- [ ] NEW `anchor_outcome/` directory has 10-15 cases loading cleanly through the existing loader. Per-UC coverage documented in `_manifest.md` (if created) or handoff §3.
- [ ] Python test suite passes (`uv run pytest` or equivalent) including the new regression tests.
- [ ] `docs/current/iteration_governance.md` §5.5 sentence added; doc passes any structural lint (front-matter unchanged; tier model untouched).
- [ ] Numbers-cite discipline (per `feedback_deliver_agent_cited_numbers_must_be_reproducible.md`): every count in the handoff (case counts, test counts, file counts) reproducible from `find ... | wc -l` or `pytest --collect-only`.

**Observation-only (recorded; does not gate close):**

- Java baseline: no change expected (S-Eval-1 is Python + YAML + docs only). Verify `mvn test` baseline `1144 / 1-inherited / 0 / 2` preserved as a guard.
- Smoke composite_score: observation only per §5.5; may shift if a smoke case happens to exercise a demoted dim. Recorded; not a gate.
- **Anchor parse/load timing + distribution recording (human-approved 2026-05-21 observation deliverable)**: dev records (a) wall-clock parse/load time for the full 159-case anchor suite through the loosened schema (single representative measurement; rough `time` or `pytest --durations` is sufficient — no need for statistical rigour); (b) per-UC distribution of the 159 anchor cases (how many cases per UC across the canonical UC set). The purpose is to confirm the enlarged backward-compat scope (159 vs the proposal's mistaken "30") is still cheap to load and to surface any UC-imbalance that may shape S-Eval-3 / S-Eval-4 scope decisions. Recorded in handoff §9 Validation runs; flag explicitly if load time exceeds, e.g., several seconds on a representative machine.

## 10. Stop conditions (dev-agent)

Dev STOPS and surfaces to deliver-agent + human (instead of pressing on) if any of:

1. **Backward-compat break on >5% of the 159 anchor cases** under the loosened schema. (Empirical evidence the demotion broke an unanticipated invariant; halt and re-scope.)
2. **>20% of existing smoke / anchor / family / bad cases need YAML edits** to satisfy the new schema. (The schema demotion should be additive / relaxation-only; any required content edits indicate a deletion-side change crept in.)
3. **`escalation_trigger` coupling relaxation requires extending the canonical 23-value enum** at `schema.py:44-68`. (Out of scope; enum widening is runtime-contract territory; halt and surface.)
4. **`anchor_outcome` per-UC coverage cannot be achieved with 10-15 cases** given the 12 canonical UCs (would need ≥ 13 cases at 1/UC; doable but tight). (Surface to deliver-agent + human for scope decision: tighter coverage with notes, OR widen to 12-15 cases.)
5. **Composite-scoring rewrite turns out to need a `tier` enum / structural model change** beyond simple advisory-vs-gate split. (Surface; deliver-agent + human decide whether to expand S-Eval-1 scope or defer to S-Eval-2 / S-Eval-5.)
6. **Any file under "Files NOT in scope" §6 needs to be touched** to complete S-Eval-1. (Hard fence violation; halt and surface; do not smuggle scope.)

## 11. Handoff document contract (12 sections per Sprint 35 / 41 shape)

`docs/sprints/sprint-042-handoff.md` must include:

§1 Sprint identity (Sprint 42 / S-Eval-1 / M3-Eval sub-sprint 1).
§2 Summary (one paragraph).
§3 Files shipped + per-file line-count numstat (reproducible via `git show --numstat <commit>`).
§4 Schema demotion details (which 4 fields; how strictness was relaxed; rationale per implementation choice — including (a) the minimum-edit choice for `escalation_trigger` coupling relaxation per §5 D-1.2 constraint, with explicit justification if dev deviates from the minimum-edit path; (b) the `closure_criterion` placement choice — preferred `CaseSpec`-level per §5 D-1.3 constraint, with explicit justification if dev falls back to `Expected`-level).
§5 Scoring code demotion details (which 2-3 dims; advisory-vs-gate wiring change in `composite.py`).
§6 NEW `anchor_outcome/` suite (10-15 cases; per-UC coverage table; source_session_id reuse provenance).
§7 Open questions (OQ-S42.N format; each OQ surfaces a non-blocking decision for deliver-agent + human at close).
§8 Generalization coverage filled (target / neighbor / negative / shadow counts per §8 stanza).
§9 Validation runs (Python test suite + Java baseline guard + spot-check that 14 smoke + 159 anchor + 12 family + 1 Alice load unchanged + **anchor parse/load timing** (wall-clock for full 159-case load) + **per-UC anchor distribution** (count per canonical UC), per §9 observation deliverable).
§10 Contract drift (any deviation from this contract; classify per §7-a / §7-b / §7-c / §7-d convention from M2 precedent).
§11 Bundle policy honored (dev shipped one commit; deliver-agent close-out files NOT staged).
§12 Closure verdict (LEFT EMPTY by dev; deliver-agent + human fill at close per `feedback_handoff_verdict_section_delegation.md`).

## 12. M3-Eval milestone context (cross-reference, not Sprint 42 contract)

- `docs/milestone_objective.md` is the live M3-Eval milestone objective. Dev SHOULD load it on cold start for layer + scope context.
- `docs/solutions/m3_eval_milestone_proposal.md` is the research-agent proposal source. Dev MAY load §1 (why this milestone exists), §2 (four-tier architecture), §5.3 (anti-hardcode standard — informational for S-Eval-3 prep, not S-Eval-1), §6 S-Eval-1 (scope detail; reconcile with this contract — this contract takes precedence on numeric discrepancies).
- S-Eval-2 (next sub-sprint after S-Eval-1 close) ships the Skill `critical_steps` schema + extractor + projection wiring. Dev SHOULD NOT pre-anticipate S-Eval-2 scope inside S-Eval-1.
- S-Eval-5 closes the 4 R-items named in `docs/milestone_objective.md` §7. Dev SHOULD NOT touch the R-items in S-Eval-1.
