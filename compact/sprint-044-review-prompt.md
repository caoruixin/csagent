Paste the content below this line into a fresh Codex session. Codex writes verdicts to `docs/codex-findings.md` (scaffold currently in place). This is the **per-sub-sprint review** required by `iteration_governance.md` §4.3 trigger #2 (LLM-visible content; §1.7 forbidden-list adjacent). **S-Eval-4 is BLOCKED until this review returns `decision: pass`** (or `approve with downgrade-to-signal follow-up` per §4.1 verdict set).

---

You are Codex doing the **per-sub-sprint review** for **Sprint 44 — Populate `critical_steps` for the 6 Skills** (S-Eval-3; third sub-sprint of Milestone M3-Eval Coarse-to-Fine Evaluation Architecture, per `docs/milestone_objective.md`).

This is the milestone's **critical governance gate**. Per `iteration_governance.md` §4.3 trigger #2: LLM-visible content + §1.7 forbidden-list adjacent → per-sub-sprint review required. S-Eval-2 shipped the structural defence (DSL parser); S-Eval-3 ships the content. Your job is the **semantic defence**: independently re-walk every populated `desc` against the proposal §5.3 standard.

## 1. Scope

**Commit range**: `db19a47..01770ac` (single dev commit `01770ac`; 9 files; +1080 / -14 per `git show --numstat 01770ac`).

**Sub-sprint contract**: `docs/sprint_objective.md` at HEAD (S-Eval-3 contract, 12 sections; §8 stanza reproduces the §5.3 standard table verbatim).

**Dev handoff**: `docs/sprints/sprint-044-handoff.md` (657 lines; 12 sections per Sprint 35/41/42/43 shape).

## 2. Read order

1. `AGENTS.md` (auto-loads doc_governance.md / agent_context_guide.md / iteration_governance.md §1 / §3 / §4 / §5 / §7).
2. `docs/milestone_objective.md` — M3-Eval north star; especially §3 S-Eval-3 row + §6 hard fences (15 items inherited).
3. `docs/sprint_objective.md` — S-Eval-3 contract; §5 Files in scope + §6 Files NOT in scope + §8 §7-stanza with §5.3 table + §10 STOP signals.
4. `docs/sprints/sprint-044-handoff.md` — dev's 657-line archive. **LOAD-BEARING sections**: §3 (file shipments + OQ-S43.2/S43.5 observations), §4 (per-Skill content map), **§5 (§5.3 self-walk — the table you re-walk independently)**, §6 (§4.1 self-walk Q1-Q9), §7 (5 OQs), §9 (validation runs + Tier-2 verification table + projection token-cost observation), §10 (contract drift §7-a × 2 planned).
5. `docs/solutions/m3_eval_milestone_proposal.md` §5 (decision-5 deep-dive) + **§5.3 (anti-hardcode standard table — your primary criterion)** + §5.4 (token cost estimate).
6. `docs/sprints/sprint-043-handoff.md` §12.7 (S-Eval-2 close carry-over — the §1.7 structural defence layer you build on).
7. `docs/current/iteration_governance.md` §1.3 (LLM-owned) + §1.4 (Runtime-owned) + **§1.7 (Forbidden — re-read carefully)** + §3.2 Q6 (`eval_spec` layer) + **§4.1 (nine-question kernel — you walk this independently)** + §4.2 (sprint-close header convention you write at top of `docs/codex-findings.md`) + §4.3 trigger #2 (this review).
8. `docs/runtime_freeze_and_risk_policy.md` §1 + §2 — verify no Tier-0 invariant added.
9. **Code source files to verify at HEAD `01770ac`**:
   - 6 Skill YAMLs under `server/src/main/resources/skills/` (verify all 18 populated `critical_steps`).
   - `eval_interactive/eval_interactive/scoring/skill_procedure_check.py` (S-Eval-2 DSL parser; confirm all 18 `trace_check` strings parse via `SkillProcedureExtractor.from_skills(...)` without raising `TraceCheckDSLSyntaxError`).
   - `eval_interactive/tests/test_skill_procedure_dsl_parser_rejects_hardcodes.py` (15 hardcode-flavoured rejections — the negative-example library; confirm none of the populated `trace_check` strings resemble any rejected pattern).
   - `eval_interactive/eval_interactive/scoring/composite.py` (Tier-2 gate band; confirm no semantic change since S-Eval-2).
   - `server/src/main/java/com/gumtree/csagent/service/runtime/skill/Skill.java` / `CriticalStep.java` / `SkillLoader.java` (S-Eval-2 ship; confirm UNCHANGED at S-Eval-3 commit).
   - `server/src/main/java/com/gumtree/csagent/service/runtime/ContextProjectionBuilder.java` (S-Eval-2 ship; confirm UNCHANGED).
   - Dev's anchor-test updates: `SkillCriticalStepsLoadingTest.java` (+46/-5) + `test_skill_procedure_extractor.py` (+50/-9) — verify these are checkpoint-test rename + assertion-reshape per dev §10 §7-a planned drift, NOT scope creep.

## 3. §4.1 nine-question kernel walk (REQUIRED — write verdicts to `docs/codex-findings.md` §"Anti-Hardcode Kernel")

Walk Q1-Q9 against the cumulative diff. Cite file:line for each verdict.

- **Q1** Semantic hardcode introduced? **Special attention for S-Eval-3**: independently re-walk §5.3 standard table (§4 below) on every populated `desc`. Dev's claim is 18/18 row-1 ✅; verify by reading each `desc` in the 6 Skill YAMLs at HEAD.
- **Q2** Tier-0 invariant protection claim? Expected: pass — no Tier-0 added.
- **Q3** Could a soft signal replace a hard branch? Expected: pass — `desc` IS the soft signal; no hard branch added.
- **Q4** Eval phrase / trace-specific phrasing / CaseSpec-id encoded? **Special attention**: verify no `cs_*` ids, no "Alice" / "Trish" / persona names, no smoke / anchor fixture text in any populated `desc` or `trace_check`. Dev's claim is references are only to canonical tool names (from `tool-policy.yaml`) and intake-field names (from `IntakeFieldsRegistry.REQUIRED_FIELDS_BY_UC`); verify.
- **Q5** LLM ownership shrunk (§1.3 surfaces moved to Java)? Expected: pass — projection slot gains soft signal; LLM still owns goal / drift / UC hypothesis / next action / escalation posture / response strategy / customer-facing wording. Several `desc` strings explicitly cede ownership (e.g., `removed-listing-route-with-moderation-context`: "The LLM owns whether the user's stated need is understanding or acting"); verify.
- **Q6** Prompt if-else added? Expected: pass — no `system_prompt.txt` edit; no Skill `procedure` text edit on any of the 6 Skills.
- **Q7** Tool schema / capability / PII / grounding floor preserved? Expected: pass — no tool schema change; FAQ grounding contract preserved (some `desc` strings reinforce the grounding-floor anchor).
- **Q8** Generalization coverage (target / neighbor / negative / shadow)? Verify dev handoff §8 + §9 Tier-2 verification table covers target (18 populated steps) + neighbor (all fixtures load unchanged) + negative (per-cluster scoping verified via §9 Tier-2 verification table rows 5 + 6) + shadow (dev did NOT read shadow CaseSpecs).
- **Q9** Rollback / sunset plan if temporary? Expected: N/A — S-Eval-3 changes are intended permanent (proposal §5.2 single-source-of-truth claim).

## 4. §5.3 anti-hardcode standard table (independently re-walk every populated `desc`)

Reproduced verbatim from `docs/solutions/m3_eval_milestone_proposal.md` §5.3 (also in `docs/sprint_objective.md` §8 stanza):

| Form of `desc` | Example | Verdict |
|---|---|---|
| Soft procedural narrative | "For UC-FP issues, consult the moderation context before explaining the reason" | ✅ LLM-first; procedural guidance the LLM owns acting on |
| Soft diagnostic signal | "If the user mentions 'appeal' or 'review', consider whether this is actually UC-H rather than UC-FP" | ⚠️ Edge — Codex will scrutinise; acceptable as guidance, NOT as a rule |
| Hard if-else rule | "IF user.message.contains('appeal') THEN active_use_case := UC-H" | ❌ Violates §1.7; Codex rejects |
| Keyword enumeration matrix | "Trigger words for UC-H: ['appeal', 'review', 'wrongly', 'unfairly', ...]" | ❌ Violates §1.7; Codex rejects |

**Independent walk procedure**: read each populated `desc` directly from the 6 Skill YAMLs at HEAD (do NOT rely on dev §5's table; verify directly). For each, assign a row 1-4 verdict + one-line rationale. Write the per-step table in your output `docs/codex-findings.md` Anti-Hardcode Kernel Q1 sub-section.

**Acceptable verdict pattern**: 18× row-1 ✅, OR a small number of row-2 ⚠️ with explicit rationale per entry (Codex scrutinises). Row-3 ❌ or row-4 ❌ on any entry → `reject as semantic hardcode`.

## 5. §1.7 boundary check (REQUIRED — write to `docs/codex-findings.md` §"§1.7 Boundary Check")

Walk a-e:
- (a) keyword / regex / if-else / enum entry added to runtime or prompt for semantic decision? Verify no runtime touch + no prompt touch.
- (b) per-UC matrix added? S-Eval-3 ships per-UC `mandatory_for` lists on `critical_steps` — verify these are scoping lists (which UCs the step gates) per S-Eval-2 schema contract, NOT semantic decision branches.
- (c) eval phrase / CaseSpec id encoded? Cross-check §4 Q4.
- (d) tool schema / capability / PII / grounding floor preservation? Cross-check §4 Q7.
- (e) LLM ownership shrink? Cross-check §4 Q5.

## 6. Hard-fence verification (per `docs/sprint_objective.md` S-Eval-3 §6)

Verify empty `git diff` on each:
- `Skill.java` / `CriticalStep.java` / `SkillRegistry.java` / `SkillLoader.java` / `SkillStateBus.java` / `SkillGuardrailDispatcher.java` / `StateInheritance.java` / `Guardrail.java` / `ContextProjectionBuilder.java` — all S-Eval-2-shipped Java surfaces UNCHANGED.
- 6 Skill YAMLs `procedure` / `guardrails` / `state_inheritance` / `applicable_use_cases` / `system_instruction` text UNCHANGED (only `critical_steps:` block appended; verify via `git diff db19a47..01770ac -- <yaml>` shows appended block only).
- `RuntimeIntentClassifier.java` / `IntentClassification.java` / `DriftResult.java` / `DriftDetector.java` / `UseCaseRouter.java` / `ClassifyUseCaseTool.java` / `PhaseEvaluator.java` / `AgentRunLoopImpl.java` / `ControlKernel.java` / `system_prompt.txt` / `tool-policy.yaml` / `IntakeFieldsRegistry.java` / `UseCaseRegistryService.java` / `ResolveDispositionEvaluator.java` UNCHANGED.
- All case fixtures (smoke / anchor / anchor_outcome / case_families / Alice / shadow / case_spec_overrides.yaml) UNCHANGED.
- Eval harness / loader / simulator (`eval_interactive/eval_interactive/loader/`, `simulator/`, `case_spec/schema.py`, `case_spec/loader.py`, `scoring/hard_checks.py`, `scoring/outcome_checks.py`, `scoring/llm_judge.py`) UNCHANGED.
- `skill_procedure_check.py` and `composite.py` (S-Eval-2 ship) — verify UNCHANGED at S-Eval-3 commit (S-Eval-3 is content-only authoring; per dev §10 / §11 confirmed).
- Governance docs + foundational docs + all sprint / milestone archives UNCHANGED.
- `docs/codex-findings.md` UNCHANGED by dev (you write here; dev did not touch).
- `docs/milestone_objective.md` UNCHANGED by dev.

## 7. Schema and reproducibility checks (per `feedback_deliver_agent_cited_numbers_must_be_reproducible.md`)

Re-derive numbers cited in dev handoff:
- **Skill YAML line additions**: 56 + 39 + 92 + 67 + 49 + 24 = 327 lines. Verify via `git show --numstat 01770ac -- server/src/main/resources/skills/`.
- **Per-Skill `critical_steps` count**: 3 / 2 / 5 / 5 / 2 / 1. Verify via `grep -c "^  - id:" <yaml>` or YAML parse.
- **Total critical_steps**: 18. Verify (3+2+5+5+2+1=18).
- **Mandatory : Advisory split**: 10 : 8. Verify via `grep -c "severity: mandatory" <yamls>` vs `severity: advisory`.
- **Java baseline**: 1163 / 1-inherited / 0 / 2. Verify via fresh `mvn test`.
- **Python baseline**: 392 passed / 9 pre-existing failed. Verify via fresh `uv run pytest`.
- **§1.7 structural defence test**: 36 passed. Verify via fresh `uv run pytest tests/test_skill_procedure_dsl_parser_rejects_hardcodes.py -v`.
- **Skill loader output**: 6 Skills load; total critical_steps=18. Verify via the Python one-liner in dev §9 backward-compat section.

## 8. Validation runs (independently re-execute selected gates)

- **Java baseline**: `mvn test 2>&1 | grep "Tests run" | tail -1` — confirm exactly `Tests run: 1163, Failures: 1, Errors: 0, Skipped: 2`.
- **Python baseline**: `cd eval_interactive && uv run pytest 2>&1 | tail -1` — confirm exactly `9 failed, 392 passed`.
- **§1.7 structural defence**: `cd eval_interactive && uv run pytest tests/test_skill_procedure_dsl_parser_rejects_hardcodes.py -v` — confirm 36 passed.
- **Skill loader sanity**: re-run the Python one-liner in dev §9 backward-compat section; confirm 6 Skills load + 18 total `critical_steps` + extractor builds without `TraceCheckDSLSyntaxError`.
- **Tier-2 deterministic verification**: spot-check at least 3 of the 13 scenarios in dev §9 Tier-2 verification table by constructing the synthetic trace and running `SkillProcedureExtractor.extract(trace, active_skill)` + `tier2_results_to_gate(results, mandatory_for, active_uc)`. **Especially verify the Alice anchor**: `uc-h-intake-complete-before-handover` Tier-2 mandatory FAIL on the canonical Alice trace shape (UC-H, intake_fields_collected empty, `request_handover` dispatched).

## 9. OQ independent verification (5 OQs surfaced WITHOUT pre-decisions per established pattern)

Surface your independent verdict on each:

- **OQ-S44.1 (LOAD-BEARING — executor wiring gap)**: dev §7 notes that the production executor at `eval_interactive/eval_interactive/batch/executor.py:252` does NOT pass `tier2_result=` to `compute_composite(...)`. The populated content is structurally inert in the production eval-harness path; offline deterministic verification (dev §9 Tier-2 verification table) is the substitute evidence. **Classify**: (a) `out_of_scope_review` — executor wiring is out of S-Eval-3 contract §5 / §6; routed to S-Eval-4 / S-Eval-5 / follow-on; (b) blocking — S-Eval-3 must broaden scope to include the wiring before close; (c) acceptable as-is — synthetic verification is sufficient for S-Eval-3, executor wiring tracked via carry-over to S-Eval-4 planning. State your verdict with rationale.
- **OQ-S44.2** (`active_skill` / `active_use_case` resolution at extractor call site): dev §7 + §9 ran Alice's case against BOTH (UC-A, resolve_faq) and (UC-H, resolve_intake), surfacing failures in both. The resolution semantics question: which active_skill + active_uc does the production harness pick? State whether this is acceptable to defer to OQ-S44.1's resolution OR needs its own scoping at S-Eval-3 close.
- **OQ-S44.3** (UC-FP `consult-moderation-context-on-removal-explanation` mandatory tightness): UC-FP cases without an available moderation context would FAIL this mandatory step even if the bot did its best with the unavailable signal. State whether this is the right semantic OR if the step should downgrade to advisory at fix-iteration.
- **OQ-S44.4** (mandatory : advisory split 10 : 8): is the calibration appropriate? Too many mandatory → false-positive Tier-2 fails on legitimate alternate paths; too few defeats Tier-2 gating. State your independent calibration verdict.
- **OQ-S44.5** (synthetic vs real-LLM verification): the §9 Tier-2 verification used deterministic synthetic traces (avoids §5.5 LLM-provider-drift confound + the OQ-S44.1 executor-wiring gap). State whether synthetic evidence is sufficient for S-Eval-3 close OR whether a real-LLM run is needed at S-Eval-3 close vs M3-Eval close.

## 10. Tier-0 candidate independent verification

Expected: no new Tier-0 candidates from S-Eval-3 (content-only authoring). Confirm `docs/runtime_freeze_and_risk_policy.md` §1 + §2 UNCHANGED.

## 11. Contract drift independent verification

Dev §10 logs 2 §7-a planned drifts: `SkillCriticalStepsLoadingTest.java` test rename + assertion reshape (+46/-5) + `test_skill_procedure_extractor.py::TestProductionSkillLoad` test rename + assertion loosening (+50/-9). Verify these match the S-Eval-2-checkpoint-retirement pattern dev claims (test names with `_atSEval2_close` / `empty_critical_steps` suffixes were checkpoints flagged for retirement at S-Eval-3 close). State whether you accept both drifts as planned-not-drift OR flag as scope creep.

## 12. Deferred / non-blocking notes

Record any observations that do not change the verdict but are worth recording for M3-Eval close OR M4+ planning:
- Token-cost observation: dev §9 per-Skill table; mean per-active-Skill turn ~378 tokens (well under proposal §5.4 / milestone §5 acceptance bar of 500-900 / ≤1000). Note any anomaly per Skill if observed.
- `id` cross-reference usage (OQ-S43.2 from S-Eval-2): dev §3 reports `id` was NOT cross-referenced in any populated `desc`. Recorded as M3-Eval-close calibration finding per S-Eval-2 §12.4 disposition.
- `session.<flag>_present` reliance (OQ-S43.5 from S-Eval-2): dev §3 reports zero use. OQ-S43.5 monitoring posture preserved; not surfaced as fix-iteration candidate.
- Any §11 dev claims worth flagging for M3-Eval-shared review (e.g., OQ-S43.1 DSL blocklist surface + OQ-S43.4 `tool_event_seq` unsuccessful-dispatch — both S-Eval-2 carry-overs routed to M3-Eval-shared review; you may pre-note observations here).

## 13. Verdict (write to `docs/codex-findings.md` top per §4.2 convention)

```
## Sprint Review Decision
decision: pass | approve with downgrade-to-signal follow-up | reject as semantic hardcode | needs human architecture decision
blocking_count: <number>
summary: <one paragraph>
```

**Verdict set per §4.1**:
- `pass` — all §3 Q1-Q9 + §4 §5.3 walk + §5 §1.7 + §6 hard fences clear; OQs disposed without blocking; S-Eval-4 unblocks.
- `approve with downgrade-to-signal follow-up` — change acceptable as interim; name a trigger that should fire conversion (e.g., OQ-S44.1 executor wiring at S-Eval-4 close); S-Eval-4 unblocks.
- `reject as semantic hardcode` — any populated `desc` is row-3 ❌ or row-4 ❌; any populated `trace_check` violates §1.7; any §6 hard fence touched without justification. S-Eval-4 stays blocked; deliver-agent + human + dev coordinate fix-iteration.
- `needs human architecture decision` — OQ-S44.1 (or any other surface) is ambiguous in scope and the verdict requires human input before Codex can decide. S-Eval-4 stays blocked; deliver-agent + human escalate.

**Do not edit code**. Do not rewrite the PR. Name the layer (`eval_spec` / `prompt_projection` / `semantic_planner` / etc. per `iteration_governance.md` §3) if a fix is needed; deliver-agent + human + dev coordinate the fix-iteration.

Write the full verdict + Q1-Q9 + §5.3 per-step walk + §1.7 + hard-fence verification + schema/reproducibility + validation runs + OQ independent verification + Tier-0 + contract drift + deferred notes to `docs/codex-findings.md` using the scaffold already in place.
