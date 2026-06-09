Paste the content below this line into a fresh Claude Code session after the human commits the deliver-agent's M2 close + M3-Eval launch bundle. The S-Eval-1 sub-sprint contract is `docs/sprint_objective.md` at HEAD.

---

You are Claude Code working as the **dev agent** for **Sprint 42 — Schema simplification + outcome-only anchor suite** (S-Eval-1; the FIRST sub-sprint of NEW Milestone M3-Eval: Coarse-to-Fine Evaluation Architecture, per `docs/milestone_objective.md`).

M2 (Skill Registry Abstraction + Wholesale Retroactive Externalization) closed Clean PASS 2026-05-18 at the milestone level (cumulative commit `51c327c~..8cd0a10`; archive at `docs/milestones/M2_objective.md` + `docs/milestones/M2_codex-review.md`; milestone-shared Codex `pass / blocking_count: 0` on first pass). M3-Eval rides on the M2 Skill abstraction; **M3-Eval does NOT touch M2-landed Skill semantic surfaces** (M3-Eval §6 hard-fence #13).

Sprint 42 is the **foundation sub-sprint** of M3-Eval: it loosens the CaseSpec schema (4 fields demoted to optional / scoring-effect-relaxed) and creates a NEW outcome-only `anchor_outcome` suite (10-15 cases). Sprint 42 ships **NO Skill YAML changes** (those are S-Eval-2 contract + S-Eval-3 content), **NO judge / rubric / L3 changes** (those are S-Eval-5 territory), and **NO runtime Java changes** (entire sub-sprint is Python + YAML + docs).

The scope is locked at `docs/sprint_objective.md` §2 (Goal: 4 outcomes) + §5 (Files in scope) + §6 (Files NOT in scope). All 12 sections of the sub-sprint contract are binding.

Sprint 42 implements proposal §6 S-Eval-1 with two HEAD-reconciled numbers:

- Existing `eval_interactive/case_specs/anchor/` has **159 cases**, NOT 30 as the proposal §6 stated. Backward-compat scope is 159.
- The new `anchor_outcome/` suite target is **10-15 cases** (unchanged from proposal — that's a target, not a HEAD count).

Sprint 42 explicitly DOES NOT:

- Delete any existing field from `Expected` (kept for backward compat; deletion is a future fold-back).
- Touch any L3 judge dim or rubric prompt (`llm_judge.py` UNCHANGED in S-Eval-1; that is S-Eval-5 territory).
- Touch any Skill YAML or any Java source under `server/src/main/java/com/gumtree/csagent/service/runtime/skill/` (`SkillRegistry`, `SkillLoader`, `SkillStateBus`, `SkillGuardrailDispatcher`, `Skill`, `StateInheritance`).
- Touch any case under `eval_interactive/case_specs/smoke/`, `eval_interactive/case_specs/anchor/`, `eval_interactive/case_specs/case_families/<existing>/`, or `eval_interactive/case_specs/bad_cases/` (cascade fence).
- Touch `eval_interactive/case_spec_overrides.yaml` (S-Eval-4 read-only source).
- Touch the eval harness, loader, or simulator (`eval_interactive/eval_interactive/loader/`, `eval_interactive/eval_interactive/simulator/`).
- Touch `system_prompt.txt`, `tool-policy.yaml`, `RuntimeIntentClassifier.java`, `PhaseEvaluator.java`, `ContextProjectionBuilder.java`, `AgentRunLoopImpl.java`, `ControlKernel.java`, or any other runtime Java surface.
- Touch `docs/foundational/`, `docs/runtime_freeze_and_risk_policy.md`, `docs/customer_service_tool_spec_v0_2.yaml`, or any sprint / milestone archive (`docs/sprints/*`, `docs/milestones/*`).
- Add Tier-0 invariants. (Milestone-level §6 hard fence.)
- Add any regex / keyword / if-else / per-UC matrix anywhere.
- Widen the 23-value canonical `escalation_reason` / `EscalationTrigger` enum.
- Use mocked-LLM as primary evidence for any LLM-behaviour claim.

## 1. Read order on cold start

1. **`AGENTS.md`** (transitively loads `docs/current/doc_governance.md` + `docs/current/agent_context_guide.md` + `docs/current/iteration_governance.md` §1 / §3 / §4 / §5 / §5.5 / §5.6 / §7 / §8).
2. **`docs/milestone_objective.md`** — NEW M3-Eval north star. ESPECIALLY:
   - §1 Milestone class (5 sub-sprints; S-Eval-1 is layer `eval_spec`).
   - §2 Goal (four-tier pyramid; S-Eval-1's role as foundation).
   - §3 Sub-sprint sequence → S-Eval-1 scope detail.
   - §4 Non-goals (M3-Eval-level; inherited into S-Eval-1).
   - §6 Hard fences (15 items at the milestone level; S-Eval-1 §6 inherits all).
   - §8 Codex review plan (S-Eval-1 deferred to milestone close per §4.3 default).
3. **`docs/sprint_objective.md`** — Sprint 42 / S-Eval-1 contract. ALL 12 sections binding. §4 premise check already verified by deliver-agent; you may re-verify but DO NOT skip if §4 line counts or field locations have drifted (HEAD verification is required before edit).
4. **`docs/solutions/m3_eval_milestone_proposal.md`** — proposal source. LOAD-BEARING sections: §1.2 (why these 6 surfaces are problematic) + §1.5 (governance ahead of schema gap) + §2 (four-tier architecture) + §6 S-Eval-1 (proposal scope; **this contract takes precedence on numeric discrepancies — 159 anchor, not 30; 17 approved overrides, not 29**). CONTEXTUAL only: §5 (decision-5 deep-dive — relevant for S-Eval-2/3, not S-Eval-1).
5. **`docs/current/iteration_governance.md`** specifically:
   - §1.3 LLM-owned (verify S-Eval-1 does NOT shift LLM-owned decisions to Java — should be trivially preserved; S-Eval-1 only loosens eval-side checks).
   - §1.4 Runtime-owned (verify S-Eval-1 adds NO new Runtime-floor enforcement).
   - §1.7 Forbidden (verify S-Eval-1 introduces NO new keyword / regex / if-else / enum widening anywhere).
   - §3.2 Q6 layer classification (S-Eval-1 is `eval_spec`).
   - §4.1 nine-question kernel (you self-walk against the S-Eval-1 diff at handoff §6 per the contract).
   - §5 / §5.5 / §5.6 acceptance bars (S-Eval-1 + M3-Eval §5 recalibration).
   - §7 sprint-objective stanza (the contract §8 stanza is already filled).
6. **`docs/runtime_freeze_and_risk_policy.md`** §1 + §2 — verify NO Tier-0 invariant added at S-Eval-1.
7. **`docs/sprints/sprint-041-handoff.md`** + **`docs/sprints/sprint-041-codex-review.md`** — the M2 LAST sub-sprint archives. CONTEXTUAL ONLY (S-Eval-1 does NOT touch M2-landed surfaces).
8. **Code source files to spot-check at HEAD `8cd0a10`** (read on demand during §3 premise re-verification + §4 implementation, NOT end-to-end):
   - `eval_interactive/eval_interactive/case_spec/schema.py` (~227 lines pre-S-Eval-1; the 4 demotion targets are at lines 153-204 verified by deliver-agent 2026-05-20).
   - `eval_interactive/eval_interactive/scoring/hard_checks.py` (~37 KB; locate `no_forbidden_tools` and `escalation_reason_consistency` dimensions).
   - `eval_interactive/eval_interactive/scoring/outcome_checks.py` (~22 KB; locate `tool_sequence_match` and the per-case 7-item checklist).
   - `eval_interactive/eval_interactive/scoring/composite.py` (~11 KB; locate the gate-vs-advisory wiring).
   - `eval_interactive/eval_interactive/scoring/llm_judge.py` (~11 KB; **READ-ONLY for S-Eval-1 — DO NOT MODIFY**).
   - `eval_interactive/case_specs/smoke/` (14 YAMLs; verify load-shape; DO NOT MODIFY).
   - `eval_interactive/case_specs/anchor/` (159 YAMLs; verify load-shape on a sampled subset; DO NOT MODIFY).
   - `eval_interactive/case_specs/bad_cases/alice_uc_a_uc_h_misclass.yaml` (UNCHANGED in S-Eval-1).
   - `docs/current/iteration_governance.md` §5.5 (locate existing smoke-demotion text; S-Eval-1 appends one sentence).

## 2. Goal (4 outcomes + 1 regression-test block)

### Outcome 1 — Schema demotions in `schema.py`

In `eval_interactive/eval_interactive/case_spec/schema.py`:

- **D-1.1** `Expected.bot_handling_pattern`: change from `str` (required, no default) to `Optional[str] = None`. Adjust `__post_init__` at `schema.py:179-181` to skip the non-empty-string check when None; preserve the check when a string is supplied.
- **D-1.2** `Expected.escalation_trigger` post-init coupling at `schema.py:190-204`: when `should_escalate=true` AND `escalation_trigger=None`, downgrade from `ValueError` to a non-fatal "diagnostic-only" path. **Constraint (human-approved 2026-05-21): pick the smallest backward-compatible implementation; do NOT introduce schema churn unless the relaxation requires it.** Concretely: prefer the minimum-edit path that loosens the existing `__post_init__` branch (e.g., replace the `raise ValueError` with a no-op or a `logging.warning` when `escalation_trigger is None` on `should_escalate=true`). Class-level flags, opt-in/opt-out parameters, or new dataclass-level facilities are LAST-RESORT only; document any deviation from the minimum-edit path in handoff §3 with explicit justification. Backward-compat invariant: all 159 anchor cases must continue to load.
- **D-1.3** NEW `closure_criterion: Optional[str] = None`. **Constraint (human-approved 2026-05-21): preferred placement is `CaseSpec`-level adjacent to `expected` (semantically a case-level closure target, not bound to the expected-bot-behaviour block).** Place at `CaseSpec` if schema-compat allows — verify by running the existing loader on the 14 smoke + 159 anchor + 12 family + 1 Alice fixtures with the new field present and confirm no breakage. Fall back to `Expected`-level **only** if `CaseSpec`-level introduces an unavoidable schema-compat break; document the fallback rationale in handoff §3 with explicit justification.
- **D-1.4** DO NOT delete `expected_tool_sequence`, `forbidden_tools`, or `bot_handling_pattern` from the schema. They remain present; demoted scoring effect is handled in `hard_checks.py` / `outcome_checks.py`.

### Outcome 2 — Scoring code demotions

In `eval_interactive/eval_interactive/scoring/`:

- **D-2.1** `hard_checks.py` `no_forbidden_tools`: when `expected.forbidden_tools` is non-empty, record as Tier-3 advisory (does not contribute to `case_passed`); when empty, skip entirely.
- **D-2.2** `hard_checks.py` `escalation_reason_consistency`: when `expected.escalation_trigger` is None on a `should_escalate=true` spec, demote family-match strictness to advisory (record dim; do not flip gate).
- **D-2.3** `outcome_checks.py` `tool_sequence_match`: demote from L2 LCS gate-contributor to Tier-3 diagnostic. Score still computed; not weighted into composite gate.
- **D-2.4** `outcome_checks.py` per-case identical 7-item `scoring.outcome_checks` checklist: opt-in via existing list-of-strings field (absent → no checks contribute to gate; present → only listed checks evaluated, as advisory by default unless explicitly marked Tier-2 — but Tier-2 is S-Eval-2 territory; S-Eval-1 keeps these advisory).
- **D-2.5** `composite.py`: update composite-scoring gate logic to read the demoted dims as advisory instead of contributors.

### Outcome 3 — New outcome-only suite

Create `eval_interactive/case_specs/anchor_outcome/` with **10-15 YAMLs**:

- 1-3 cases per UC across the canonical UC set (UC-A / UC-B / UC-C / UC-D / UC-E / UC-F / UC-FP / UC-G / UC-H / UC-I / UC-J / UC-K). Coordinate per-UC coverage at planning round (deliver-agent + dev align before committing the case set).
- Each case carries: `case_id`, `source_session_id` (reused from existing smoke / anchor cases where representative — read the source case to confirm `persona.user_goal_summary` aligns; flag as `synthetic` only if no real session matches the UC), `source_dataset`, `form_context`, `persona` (with `user_goal_summary`), `expected.outcome_class`, optional `expected.closure_criterion`. **OMIT or set to None**: `expected.expected_tool_sequence`, `expected.forbidden_tools`, `expected.bot_handling_pattern`, `expected.escalation_trigger`.
- Optional `_manifest.md` documenting per-UC coverage + reuse provenance (recommended; useful for S-Eval-4 dry-run + M3-Eval close).
- DO NOT add `_ACCESS_BOUNDARY.md` — this is a visible suite, not shadow.

### Outcome 4 — Governance text

`docs/current/iteration_governance.md` §5.5: append one sentence stating "anchor_outcome (under `eval_interactive/case_specs/anchor_outcome/`) is the second human-judgment surface beside the curated bad-case suite at `eval_interactive/case_specs/bad_cases/`; smoke remains observation-only by design." Place near the existing §5.5 text about smoke demotion.

### Outcome 5 (regression-test block) — Backward-compat + parity tests

One or more new test files under `eval_interactive/tests/` asserting:

- Schema loads existing 14 smoke + 159 anchor + 12 case-family + 1 Alice bad-case fixtures unchanged.
- `bot_handling_pattern: None` is accepted on a new `anchor_outcome` case.
- `should_escalate=true` with `escalation_trigger=None` is accepted (no `ValueError`) when the loosened coupling is applied.
- A demoted advisory does NOT flip `case_passed` (parity test: previously-passing case stays passing).
- A NEW anchor_outcome case loads cleanly through the existing loader.

### Outcome 6 (observation deliverable, human-approved 2026-05-21) — Anchor parse/load timing + distribution

Record in handoff §9 Validation runs:

- **(a) Anchor parse/load timing**: wall-clock time to parse + load the full 159-case anchor suite through the loosened schema. A single representative measurement is sufficient — `time python -c "..."` or `pytest --durations` on the backward-compat test is enough; no statistical rigour required. **Purpose**: confirm the enlarged backward-compat scope (159 vs proposal's mistaken "30") is still cheap. **Flag explicitly** if load time exceeds, e.g., several seconds on a representative dev machine.
- **(b) Per-UC anchor distribution**: count of anchor cases per canonical UC (UC-A / UC-B / UC-C / UC-D / UC-E / UC-F / UC-FP / UC-G / UC-H / UC-I / UC-J / UC-K). Quick `grep -c "primary_uc:.*UC-X"` per UC, or `pytest`-collected pivot. **Purpose**: surface any UC-imbalance in the 159 that may shape S-Eval-3 / S-Eval-4 scope decisions.

This is observation-only — not a gate; does not block close even if findings are surprising.

## 3. Read order specific to deliverables

- For D-1.x: `eval_interactive/eval_interactive/case_spec/schema.py` lines 132-227 (Expected dataclass + post-init coupling).
- For D-2.x: `eval_interactive/eval_interactive/scoring/hard_checks.py` (grep for `no_forbidden_tools` and `escalation_reason_consistency`); `outcome_checks.py` (grep for `tool_sequence_match`); `composite.py` (gate-vs-advisory wiring).
- For Outcome 3: read one or two existing anchor cases (`eval_interactive/case_specs/anchor/cs_interactive_001.yaml` for shape reference); use grep on `case_specs/smoke/` to find representative source_session_ids per UC.
- For Outcome 4: `docs/current/iteration_governance.md` §5.5 (locate the smoke-demotion paragraph dated 2026-05-16 update).
- For Outcome 5: existing test files (if any) under `eval_interactive/tests/` for convention; otherwise create new test file with clear naming.

## 4. STOP discipline (6 LOAD-BEARING per contract §10)

You STOP and surface to deliver-agent + human (instead of pressing on) if any of:

1. **Backward-compat break on >5% of the 159 anchor cases** under the loosened schema.
2. **>20% of existing smoke / anchor / family / bad cases need YAML edits** to satisfy the new schema. (Schema demotion should be additive / relaxation-only.)
3. **`escalation_trigger` coupling relaxation requires extending the canonical 23-value enum** at `schema.py:44-68`. (Out of scope; enum widening is runtime-contract territory.)
4. **`anchor_outcome` per-UC coverage cannot be achieved with 10-15 cases** given the 12 canonical UCs. (Surface for scope decision.)
5. **Composite-scoring rewrite needs a `tier` enum / structural model change** beyond simple advisory-vs-gate split. (Surface; deliver-agent + human decide expand-S-Eval-1-scope or defer.)
6. **Any file under "Files NOT in scope" §6 needs to be touched** to complete S-Eval-1. (Hard fence violation; halt and surface; do not smuggle scope.)

## 5. Hard fences (per contract §6; 6 categories)

1. **M2-landed Skill surfaces — NO touch** (Skill.java, SkillRegistry, SkillLoader, SkillStateBus, SkillGuardrailDispatcher, StateInheritance, 6 Skill YAMLs).
2. **Runtime surfaces — NO touch** (RuntimeIntentClassifier, IntentClassification, DriftResult, DriftDetector, UseCaseRouter, ClassifyUseCaseTool, PhaseEvaluator, ContextProjectionBuilder, AgentRunLoopImpl, ControlKernel, system_prompt.txt, tool-policy.yaml).
3. **Existing eval surfaces — NO touch** (any case under `case_specs/smoke/`, `case_specs/anchor/`, `case_specs/case_families/<existing>/`, the Alice bad case, `case_specs/bad_cases/_manifest.md`, shadow CaseSpecs under `case_specs_shadow/`, `case_spec_overrides.yaml`, harness loader, simulator).
4. **Judge surfaces — NO touch in S-Eval-1** (`llm_judge.py` is S-Eval-5 territory).
5. **Governance and archives — NO touch** (`docs/foundational/`, `runtime_freeze_and_risk_policy.md`, `customer_service_tool_spec_v0_2.yaml`, all sprint / milestone archives, live `milestone_objective.md`, live `codex-findings.md`).
6. **Action bank — limited touch** (do not close R-items in S-Eval-1; you MAY append a Sprint 42 close-action index row in §6 close-action index at sub-sprint close).

## 6. §4.1 anti-hardcode self-walk template

Before commit, walk each Q1-Q9 against the S-Eval-1 diff. Write the verdicts in handoff §6:

- **Q1** Semantic hardcode introduced (keyword / regex / if-else / enum / per-UC matrix)? Expected: **pass** — all changes are relaxations; no new branches.
- **Q2** Tier-0 invariant protection claimed? Expected: **pass** — no Tier-0 invariant added; not applicable.
- **Q3** Could a soft-projection signal replace a hard branch? Expected: **pass** — no hard branch added in the first place.
- **Q4** Eval phrase / trace-specific phrasing / CaseSpec-id encoded? Expected: **pass** — schema demotions are structural; no fixture text or CaseSpec-id encoded.
- **Q5** LLM ownership shrunk (§1.3 surfaces moved to Java)? Expected: **pass** — eval-side schema change does not touch §1.3 surfaces.
- **Q6** Prompt if-else added? Expected: **pass** — no prompt change in S-Eval-1.
- **Q7** Tool schema / capability / PII / grounding floor preserved? Expected: **pass** — eval-side schema change does not touch runtime floors.
- **Q8** Generalization coverage (target / neighbor / negative / shadow)? Expected: **pass** — §8 stanza in the contract names target / neighbor / negative / shadow counts.
- **Q9** Rollback / sunset plan if temporary? Expected: **N/A** — S-Eval-1 changes are intended permanent (the demotion is the deliverable).

If any Q1-Q9 returns `concern` or `fail`, STOP and surface to deliver-agent + human before commit.

## 7. Handoff §11 12-section contract

`docs/sprints/sprint-042-handoff.md` must include:

- **§1** Sprint identity (Sprint 42 / S-Eval-1 / M3-Eval sub-sprint 1).
- **§2** Summary (one paragraph).
- **§3** Files shipped + per-file line-count numstat (reproducible via `git show --numstat <commit>`). Includes schema implementation-choice rationale: (a) D-1.2 minimum-edit choice for `escalation_trigger` coupling relaxation with explicit justification if deviating from the minimum-edit path; (b) D-1.3 `closure_criterion` placement — preferred `CaseSpec`-level per constraint, with explicit justification if falling back to `Expected`-level.
- **§4** Schema demotion details (4 fields; how strictness was relaxed).
- **§5** Scoring code demotion details (2-3 dims; advisory-vs-gate wiring change in `composite.py`).
- **§6** §4.1 anti-hardcode self-walk verdicts (Q1-Q9 with one-line justification each).
- **§7** Open questions (OQ-S42.N format; non-blocking decisions for deliver-agent + human at close).
- **§8** Generalization coverage filled (target / neighbor / negative / shadow counts).
- **§9** Validation runs:
  - Python test suite (new + existing; preserve all currently-passing tests).
  - Sample backward-compat run: load all 14 smoke + 159 anchor + 12 family + 1 Alice fixtures; assert `case_passed` parity on a representative sub-sample (full re-run optional if loader is fast).
  - Java baseline guard (`mvn test`): unchanged (S-Eval-1 is Python + YAML + docs only; baseline `1144 / 1-inherited / 0 / 2` from M2 close preserved).
  - **Anchor parse/load timing**: wall-clock for the full 159-case load through the loosened schema (single representative measurement; per Outcome 6 (a)).
  - **Per-UC anchor distribution**: count of anchor cases per canonical UC (per Outcome 6 (b)).
- **§10** Contract drift (any deviation from this contract; classify per §7-a / §7-b / §7-c / §7-d convention from M2 precedent).
- **§11** Bundle policy honored (dev ships one commit; deliver-agent close-out files NOT staged).
- **§12** Closure verdict (LEFT EMPTY by dev per `feedback_handoff_verdict_section_delegation.md`).

## 8. Bundle policy

Dev ships **ONE bundle commit** containing:

- `eval_interactive/eval_interactive/case_spec/schema.py` (D-1.x edits).
- `eval_interactive/eval_interactive/scoring/hard_checks.py` + `outcome_checks.py` + `composite.py` (D-2.x edits).
- NEW directory `eval_interactive/case_specs/anchor_outcome/` with 10-15 YAML files + optional `_manifest.md`.
- `docs/current/iteration_governance.md` §5.5 sentence append.
- New regression-test files under `eval_interactive/tests/`.
- `docs/sprints/sprint-042-handoff.md` (dev-authored).

Dev does NOT stage:

- `docs/sprint_objective.md` archive copy (deliver-agent territory at close).
- `docs/milestone_objective.md` updates (deliver-agent territory).
- `docs/10-handoff.md` §1 lead refresh (deliver-agent territory).
- `docs/codex-findings.md` reset (deliver-agent + Codex territory).
- `docs/action_bank.md` Sprint 42 close-action row (you MAY append at sub-sprint close; deliver-agent + human may also do this at close-out bundle).

Suggested commit message:

```
sprint 42 / S-Eval-1: schema simplification + anchor_outcome suite (NEW M3-Eval sub-sprint 1)
```

## 9. Self-check before commit

Run these mental checks against your diff before staging:

1. **Hard fence audit**: walk §5 of this prompt; for each "NO touch" item, run `git diff <path>` and confirm empty.
2. **Numbers-cite discipline**: every count in handoff §3 must be reproducible. Use `find eval_interactive/case_specs/anchor_outcome -name '*.yaml' | wc -l` for the new suite count; use `git show --numstat <staged-commit>` (or `git diff --numstat`) for per-file line counts.
3. **§4.1 self-walk**: Q1-Q9 verdicts written in handoff §6 (none `concern` / `fail`).
4. **Backward-compat smoke**: load all 14 smoke + 159 anchor + 12 family + 1 Alice fixtures using the new schema; assert no `case_passed` flip on a sample (full re-run optional).
5. **Outcome 6 observation captured**: anchor parse/load timing recorded + per-UC distribution recorded; both written to handoff §9. Even if findings are unremarkable, the values MUST appear in handoff §9 so the deliver-agent + human can confirm the enlarged backward-compat scope is still cheap.
6. **Schema implementation choice rationale**: D-1.2 minimum-edit choice + D-1.3 `closure_criterion` placement choice both documented in handoff §3 with explicit justification if deviating from the constraint defaults (minimum-edit / CaseSpec-level).
7. **No-mocked-LLM-as-proof**: if any handoff claim relies on LLM behaviour, cite a real-LLM source (eval_interactive run path + case_id) — but S-Eval-1 should not need any LLM-behaviour claim; this is structural / schema / fixture work.
8. **Bundle policy**: `git status` shows ONLY files listed in §8; nothing under `docs/milestones/`, `docs/sprints/sprint-001-*..sprint-041-*`, no live `docs/milestone_objective.md` edit, no live `docs/codex-findings.md` edit.
9. **Stop-conditions review**: any of the 6 STOP signals in §4 fired? If yes, STOP and surface; do NOT commit.

After commit, surface to deliver-agent + human:

- Commit SHA.
- Java baseline preservation status.
- Python test pass status.
- Backward-compat sample-run result.
- Anchor parse/load timing + per-UC distribution summary (Outcome 6 observation).
- D-1.2 minimum-edit choice + D-1.3 `closure_criterion` placement choice (with justification if deviating).
- Open questions OQ-S42.N list.
- Any close-readiness notes (your guess at A / A-with-X / B classification — deliver-agent + human make the actual call at close).
