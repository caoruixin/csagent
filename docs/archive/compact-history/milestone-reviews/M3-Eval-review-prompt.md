Paste the content below this line into a fresh Codex session at M3-Eval milestone close, after Sprint 46 / S-Eval-5 has closed A — Clean PASS and after the deliver-agent + human have completed the bad-case suite manual review. No PR will be opened; review the milestone-shared cumulative commit range directly.

---

You are the Anti-Hardcode + Milestone-Close Review Agent for **NEW Milestone M3-Eval — Coarse-to-Fine Evaluation Architecture** per `docs/milestone_objective.md`. M3-Eval is the THIRD milestone planned and executed under the `iteration_governance.md` §8 milestone framework (M1 closed 2026-05-17; M2 closed 2026-05-18; M3-Eval cumulative work landed 2026-05-21 → 2026-05-23). It uses a milestone-shared cumulative Codex review per §4.3 second paragraph DEFAULT (no per-sub-sprint trigger fires for S-Eval-1 / S-Eval-2 / S-Eval-4 / S-Eval-5; **S-Eval-3 invoked §4.3 trigger #2 §1.7 forbidden-list adjacent and was Codex-reviewed at sub-sprint close** per `docs/sprints/sprint-044-codex-review.md` — verdict `pass / 0` first pass, archived).

M3-Eval replaces the implicit L1/L2/L3 equal-weighted eval scoring with a **four-tier pyramid** (per `docs/solutions/m3_eval_milestone_proposal.md` §2) and operationalizes a Tier-2 hard gate (`skill_procedure_followship`) anchored on Skill `critical_steps[]` content via the single-source-of-truth property (proposal §5.2): same YAML `critical_steps[].desc` consumed by BOTH the runtime LLM (via `ContextProjectionBuilder` rendering inside `phase_plan.skill`) AND eval-side scoring (via `SkillProcedureExtractor` evaluating `trace_check`).

M3-Eval shipped 5 sub-sprints across 10 commits (5 dev + 4 deliver-agent close + 1 follow-up) over 3 calendar days (2026-05-21 → 2026-05-23):

| Sub-sprint | Commit(s) | Outcome | Per-sub-sprint Codex |
|---|---|---|---|
| Sprint 42 (S-Eval-1; schema simplification + `anchor_outcome` suite) | `d91bd3d` (dev) + `357e949` (close + M3-Eval setup-bundle) | A — Clean PASS | Deferred to M3-Eval milestone-shared close per §4.3 default |
| Sprint 43 (S-Eval-2; Skill `critical_steps` schema + extractor + projection wiring) | `69ed77f` (dev) + `db19a47` (close + v0_2 supersession) | A — Clean PASS | Deferred to M3-Eval milestone-shared close per §4.3 default |
| Sprint 44 (S-Eval-3; populate `critical_steps` for the 6 Skills) | `01770ac` (dev) + `4dafaf5` (close + per-sub-sprint Codex archive) | A — Clean PASS | **§4.3 trigger #2 fired** (`critical_steps[].desc` LLM-visible; §1.7 forbidden-list adjacent); Codex `pass / 0` first pass single round per `docs/sprints/sprint-044-codex-review.md` |
| Sprint 45 (S-Eval-4; bad-case suite expansion + trial milestone-close dry-run) | `8b7ff40` (dev) + `a4a3bc6` (close + S-Eval-5 launch) | A — Clean PASS | Deferred to M3-Eval milestone-shared close per §4.3 default |
| Sprint 46 (S-Eval-5; L3 judge repositioning + R-item closure + Option A executor wiring) | `e0cd8aa` (dev bundle) + `7562a2d` (OQ-S46.1 real-LLM monotone-relaxing rerun follow-up) | A — Clean PASS | Deferred to M3-Eval milestone-shared close per §4.3 default |

**M3-Eval cumulative commit range: `d91bd3d..7562a2d` (10 commits).**

This **milestone-shared review** is the CUMULATIVE check across the full M3-Eval range. S-Eval-3 already received per-sub-sprint Codex review per `iteration_governance.md` §4.3 trigger #2 — that Codex pass verified the 18 populated `critical_steps[].desc` strings against the proposal §5.3 standard (18× row-1 ✅ soft procedural narrative; zero §1.7 forbidden patterns; `pass / 0` first pass). This M3-Eval-shared review re-walks the §4.1 nine-question kernel + §4.2 sprint-close header against the FULL M3-Eval commit range to verify (a) milestone-level scope discipline across the 5 sub-sprints (each `eval_spec` layer, no `runtime` semantic touch); (b) cumulative architectural coherence (the 5 sub-sprints work together as one Coarse-to-Fine Evaluation Architecture); (c) M3-Eval §6 hard fences honored across the full range (15 items); (d) the four-tier pyramid landed end-to-end (Tier-0 preserved + Tier-1 anchor_outcome + Tier-2 `critical_steps` operative via Option A wiring + Tier-3 advisory demotion); (e) no §1.7 forbidden hardcode introduced anywhere across the 5 dev commits + 1 follow-up; (f) the 4 R-items closed at S-Eval-5 are correctly annotated in `docs/action_bank.md` §6 + the 4 NEW R-items opened across the milestone (1 from S-Eval-4 + 3 from M3-Eval close manual review) are correctly scoped + deferred.

**M3-Eval ships (cumulative artefacts):**

- **Schema + scoring demotion** (Sprint 42 / S-Eval-1): 4 schema demotions in `eval_interactive/eval_interactive/case_spec/schema.py` + 3 scoring demotions in `eval_interactive/eval_interactive/scoring/hard_checks.py` / `outcome_checks.py` / `composite.py`; NEW `severity` field convention on `HardCheckResult` + `OutcomeCheckResult` (`critical` | `advisory`); NEW 12-case `case_specs/anchor_outcome/` suite (outcome-only Tier-1 anchors per UC); 20 NEW Python regression tests.
- **Skill `critical_steps` schema + extractor + projection wiring** (Sprint 43 / S-Eval-2): NEW `CriticalStep.java` record (5 fields: `id`, `desc`, `severity`, `mandatory_for`, `trace_check`; nested `Severity` enum {mandatory, advisory}) + `Skill.java` extended via backward-compat secondary 15-arg ctor + `SkillLoader.java` allowlist validation per design § + `ContextProjectionBuilder.java` renders `[{id, desc}]` list-of-objects inside `phase_plan.skill` + NEW Python `eval_interactive/eval_interactive/scoring/skill_procedure_check.py` (hand-rolled recursive-descent DSL parser; 6 frozen primitives: `accumulated_tool_results.<tool>` / `tool_event_seq(<a>) < tool_event_seq(<b>)` / `intake_state.fields_collected.contains(<field>)` / `session.<flag>_present` / `any_of(...)` / `all_of(...)`); parser rejects keyword / regex / message-content at parse time with `TraceCheckDSLSyntaxError` (anti-hardcode structural defence); `composite.py` Tier-2 wiring (`compute_composite(...)` accepts `tier2_result=` parameter); 36 §1.7 structural-defence tests covering 15 hardcode-flavoured rejections + 11 positive-grammar + 10 malformed-syntax.
- **`critical_steps` content** (Sprint 44 / S-Eval-3): 18 LLM-visible `critical_steps` populated across 6 Skill YAMLs at `server/src/main/resources/skills/` (`discover_triage`=3 / `confirm`=2 / `resolve_faq_grounded_answer`=5 / `resolve_intake_collect_and_handover`=5 / `escalate`=2 / `terminal`=1; mandatory:advisory split = 10:8). Anti-hardcode strategy: `desc` strings anchored on canonical tool names (from `server/src/main/resources/tool-policy.yaml`) + canonical intake field names (from `IntakeFieldsRegistry.REQUIRED_FIELDS_BY_UC`) — NOT user-message keywords. **Codex per-sub-sprint independent walk: 18× row-1 ✅ soft procedural narrative; verdict `pass / 0` first pass single round** per `docs/sprints/sprint-044-codex-review.md`. **No content edits at S-Eval-4 / S-Eval-5; the S-Eval-3 content is the cumulative ship.**
- **Bad-case suite expansion** (Sprint 45 / S-Eval-4): 11 new bad-case YAMLs landed in `eval_interactive/case_specs/bad_cases/` sourced from 17 approved entries in `eval_interactive/case_spec_overrides.yaml`; total suite = 12 (1 Alice + 11 new); per-primary-UC `{UC-A: 2, UC-C: 3, UC-FP: 3, UC-D: 2, UC-K: 2}`; tier distribution `core × 7 + scope-relevant × 4` (plus the existing Alice `core`); D-dim coverage `D1 × 9, D2 × 2, D3 × 0, D4 × 3`; **UC-G/H/I/J primary = 0** (documented gap; fallback path (a) AUTHORIZED for M3-Eval per S-Eval-4 close decision; NEW R-item `R-bad-case-suite-uc-ghij-seed-from-real-sessions` opened in `docs/action_bank.md` §5 with status `proposed; impl deferred to M4+ planning`). S-Eval-4 trial milestone-close dry-run used Option A (synthetic / mocked trace projected verdicts) for `closure_criterion` wording calibration; 11/11 well-calibrated or acceptable per `_manifest.md` calibration notes section; zero rewrites applied.
- **L3 judge repositioning + R-item closure + Option A executor wiring** (Sprint 46 / S-Eval-5): (a) 3 legacy L3 dims (`relevance`, `tone_appropriateness`, `groundedness`) demoted from composite contributors to Tier-3 advisory via the S-Eval-1 D-2.5 `severity` field convention applied in `eval_interactive/eval_interactive/scoring/llm_judge.py::_ADVISORY_DIMENSIONS` frozenset + `_severity_for` classmethod + `_parse_response(severity=...)` stamping + `eval_interactive/eval_interactive/scoring/composite.py` advisory filter on `judge_score` mean (gating filter at line 184-186 / 220-223 / 217 parity); (b) NEW `user_goal_achievement` L3 dim (Tier-1 supplementary advisory; coarse 1-5; anchored on `persona.user_goal_summary`; never flips `case_passed`); (c) 2 rubric prompt updates close `R-l3-judge-form-context-trust-rubric` + `R-l1-source-citation-quality-rubric` via narrative-only changes in `_judge_tone_appropriateness` + `_judge_groundedness` (NOT keyword / regex / per-UC matrix; semantic anchors only); (d) 4 R-item `succeeded-by` closure annotations in `docs/action_bank.md` §6 (preserved entries per §6 convention); (e) **Option A executor wiring** in `eval_interactive/eval_interactive/batch/executor.py` (`_SKILLS_DIR` class constant + lazy `SkillProcedureExtractor` via `_get_skill_extractor` + `_compute_tier2_result` aggregating per-Skill extractor passes via `tier2_results_to_gate` + `compute_composite(..., tier2_result=tier2_result)` pass-through + `_build_case_result` enrichment for `l3_results[].severity` + per-step Tier-2 detail serialisation). (f) Monotone-relaxing structural proof in `eval_interactive/tests/test_s_eval_5_l3_repositioning.py` (847 lines / 30 tests across 7 test classes; all pass). (g) Real-LLM monotone-relaxing rerun follow-up `7562a2d` (smoke 14 + anchor_outcome 12 + anchor 159 pre vs post-S-Eval-5 across 185 cases; **0 `case_passed=true → false` transitions** per `docs/sprints/sprint-046-handoff.md` §13 — verified by construction matches operational result).
- **M3-Eval close bad-case suite manual review** (deliver-agent + human, 2026-05-23 against post-S-Eval-5 HEAD `7562a2d`): 12-case real-LLM rerun (Moonshot `moonshot-v1-32k` simulator/judge); main batch `eval_interactive/results/20260522-110537/` (parallel=4) + 3 isolated reruns `20260522-162847/162848/162850` (parallel=1; cs012 / fg5q / iwzx OOSR cleared at parallel=1); per-case verdicts recorded in `eval_interactive/case_specs/bad_cases/_manifest.md` ledger M3 column + new "M3-Eval close real-LLM rerun + manual review" section. **Distribution: PASS × 5 (cs001, cs014, cs029, cs066, fg5q) + IMPROVING × 4 (alice, cs011, cs012, wmkb) + FAIL × 3 (cs015, cs095, iwzx — all 3 are the bad cases' raison d'être) + OOSR × 0 at parallel=1.** Tier-0 safety floor 12/12 PASS. Option A executor wiring operative across all sessions reaching scoring. S-Eval-5 rubric updates operationally verified on Alice (only bad case opting into L3 dims per OQ-S46.7 fixture gap — generalized to NEW R-item `R-bad-case-fixture-migrate-to-l3-judge-dims`).
- **Tests + baselines**: Java baseline `1163 / 1-inherited / 0 / 2` UNCHANGED across all 5 M3-Eval sub-sprints (the 1 inherited `SystemPromptUserRequestedTiebreakerTest.systemPrompt_marksActiveUcTiebreakerExplicitly:53` failure since Sprint 24-era working-tree mod persists unchanged; not M3-Eval-attributable). Python baseline trajectory: pre-M3-Eval (post-M2) → S-Eval-1 `4 failed, 311 passed` → S-Eval-2 `4 failed, 371 passed` → S-Eval-3 `5 failed, 392 passed` (OQ-S44.6 surfaced — runner-invocation discipline `uv run python -m pytest` required) → S-Eval-4 `5 failed, 396 passed` → S-Eval-5 `5 failed, 426 passed` (+30 NEW tests from `test_s_eval_5_l3_repositioning.py`). The 5 pre-existing failures (2× `test_case_spec_overrides.py` + 1× `test_corpus_lint.py` + 2× `test_escalation_enum_sync.py`) attributable to the `v0_2` → `v0_3` supersession debt (R-item `R-stale-test-escalation-enum-sync-v0_2-to-v0_3-migration` opened at S-Eval-1 close; STILL `proposed; deferred to whichever milestone consumes it`).

**M3-Eval acceptance bar** (per `docs/milestone_objective.md` §5; the milestone close primary-gate framing): the §5.6 bad-case suite manual review IS the primary acceptance gate (NOT recalibrated to OBSERVATION; this differs from M2 §5 recalibration). The S-Eval-5 monotone-relaxing rerun (§13) verified `case_passed` monotonicity across the 185-case smoke + anchor_outcome + anchor surface; the M3-Eval close real-LLM bad-case suite manual review (above) provides the closing PRIMARY GATE evidence. **Codex MUST NOT re-interpret §5.6 as a programmatic threshold** — per the 2026-05-17 refinement, the bad-case suite is a human-judgment gate where the `closure_criterion` field is GUIDANCE for human qualitative review, not a binary programmatic match. This milestone-shared review evaluates (a) the architecture; (b) the §4.1 nine-question kernel against the cumulative diff; (c) the §1.7 forbidden-list compliance; (d) the M3-Eval scope discipline; **NOT** a re-judging of the per-case PASS / FAIL / IMPROVING verdicts from the bad-case suite manual review (those are jointly owned by deliver-agent + human per §5.6).

---

## 1. Loader

Read in this order on a fresh Codex session. Do NOT skip; the prompt's later sections assume each is loaded.

1. **`AGENTS.md`** (transitively loads `docs/current/doc_governance.md` + `docs/current/agent_context_guide.md` + `docs/current/iteration_governance.md` §1 / §3 / §4 / §4.1 / §4.2 / **§4.3** (milestone-shared review provisions) / §5 / §5.5 / **§5.6** + §5.6.1 + §5.6.2 + §5.6.3 (bad-case suite as PRIMARY human-judgment gate; 2026-05-17 refinement) / §7 / **§8** (milestone framework)).
2. **`docs/milestone_objective.md`** — the M3-Eval milestone north star. Read end-to-end. Especially: §1 milestone class + §7 stanza coverage across S-Eval-1 → S-Eval-5; §2 Goal — the architectural outcome (Coarse-to-Fine Evaluation Architecture; four-tier pyramid; single-source-of-truth `critical_steps[].desc`); §3 sub-sprint sequence (S-Eval-1 → S-Eval-5); §4 non-goals; §5 acceptance bar (8 hard gates); §6 hard fences (15 items); §7 R-items consumed / surfaced; §8 Codex review plan (milestone-shared default per §4.3; per-sub-sprint trigger #2 fired at S-Eval-3); §11 cross-milestone sequencing.
3. **All 5 per-sub-sprint objective archives** (immutable per `doc_governance.md`):
   - `docs/sprints/sprint-042-objective.md` + `docs/sprints/sprint-042-handoff.md` (S-Eval-1; schema simplification + anchor_outcome).
   - `docs/sprints/sprint-043-objective.md` + `docs/sprints/sprint-043-handoff.md` (S-Eval-2; Skill `critical_steps` schema + extractor + projection wiring + DSL parser).
   - `docs/sprints/sprint-044-objective.md` + `docs/sprints/sprint-044-handoff.md` + `docs/sprints/sprint-044-codex-review.md` (S-Eval-3; populated `critical_steps` content; **per-sub-sprint Codex `pass / 0` first pass — read this verdict to confirm `desc` strings already cleared §1.7 + §5.3 standard**).
   - `docs/sprints/sprint-045-objective.md` + `docs/sprints/sprint-045-handoff.md` (S-Eval-4; bad-case suite expansion + dry-run; no Codex archive — deferred).
   - `docs/sprints/sprint-046-handoff.md` (S-Eval-5; this is the LIVE dev handoff at M3-Eval close — `docs/sprint_objective.md` will be archived to `docs/sprints/sprint-046-objective.md` as part of the deliver-agent close-out bundle after this Codex review).
4. **`docs/solutions/m3_eval_milestone_proposal.md`** — the research-agent proposal that drove M3-Eval (LOAD-BEARING). Read end-to-end:
   - §1 — context + scope.
   - §2 — four-tier pyramid (Tier-0 preserved + Tier-1 outcome + Tier-2 `skill_procedure_followship` + Tier-3 advisory).
   - §5.2 — single-source-of-truth property (`critical_steps[].desc` consumed by BOTH runtime LLM AND eval-side scoring).
   - §5.3 — `critical_steps[].desc` standard table (4-row evaluation: row 1 ✅ soft procedural; row 2 ⚠ soft diagnostic edge; row 3 ❌ hard if-else; row 4 ❌ keyword enumeration matrix). **Independently re-walk the 18 populated `desc` strings against this table** during §3 below to confirm S-Eval-3's per-sub-sprint Codex verdict still holds at M3-Eval close.
   - §5.4 — token-cost estimate (500-900 tokens per turn).
   - §6 — S-Eval-1 through S-Eval-5 sub-sprint plan (verify each sub-sprint's ship matches the proposal scope).
5. **`docs/current/iteration_governance.md`** — §1 Constitution; §1.3 LLM-owned; §1.4 Runtime-owned; §1.7 Forbidden; §3.2 layer classification; §4.1 nine-question kernel (Codex independently re-walks against the cumulative M3-Eval diff at §3 below); §4.2 sprint-close header convention (this milestone-shared review writes per §4.2 at M3-Eval close); §4.3 milestone-shared review provisions (this review is the operationalization); §5 acceptance bars; §5.5 smoke OBSERVATION; **§5.6 bad-case suite primary-gate framing (M3-Eval honors §5.6 as PRIMARY GATE, NOT recalibrated)**; §5.6.1 tiering; §5.6.2 per-milestone selection; §5.6.3 downgrade rule; §7 sprint-objective stanza; §8 milestone framework.
6. **`docs/runtime_freeze_and_risk_policy.md`** §1 + §2 — current Tier-0 invariant set at M3-Eval close. Verify M3-Eval dev commits across the cumulative range ADDED no Tier-0 invariant (M3-Eval §6 #2 fence preserved across all 5 sub-sprints; C2 + C3 candidates inherited as DEFERRED from M2 close).
7. **`docs/current/customer_service_tool_spec_v0_3.md`** + **`docs/current/faq_grounding_contract.md`** — tool schema + grounding-floor contract. Verify M3-Eval preserves the FAQ grounding contract; M3-Eval §6 #11 fence (no `escalation_reason` enum widening) preserved.
8. **`docs/action_bank.md`** — R-item tracker; pay attention to:
   - §5 newly opened R-items: `R-stale-test-escalation-enum-sync-v0_2-to-v0_3-migration` (S-Eval-1 close 2026-05-21) + `R-bad-case-suite-uc-ghij-seed-from-real-sessions` (S-Eval-4 close 2026-05-22) + **3 NEW M3-Eval close surfaced R-items 2026-05-23** (`R-iwzx-uc-k-vs-uc-h-routing-spurious-distress` semantic_planner + `R-bad-case-parallel-session-establishment-flakiness` infra + `R-bad-case-fixture-migrate-to-l3-judge-dims` eval_spec; all deferred to M4+).
   - §6 closed-action index rows: Sprint 42 / S-Eval-1 + Sprint 43 / S-Eval-2 + Sprint 44 / S-Eval-3 + Sprint 45 / S-Eval-4 (Sprint 46 / S-Eval-5 row will be added by deliver-agent at close-out bundle).
   - §6 R-item closure annotations 2026-05-22 (S-Eval-5): `R-l3-judge-form-context-trust-rubric` (line 397) + `R-l1-source-citation-quality-rubric` (line 412) + `R-cs038-l3-review-intake-efficiency` (line 408) + `R-cs040-l3-review-intake-completion-semantics` (line 409). Verify each `succeeded-by` annotation correctly references the closing surface + matches the actual code at HEAD `7562a2d`.
9. **`eval_interactive/case_specs/bad_cases/_manifest.md`** — bad-case suite lifecycle ledger. Read the M3 column verdicts + the "M3-Eval close real-LLM rerun + manual review (2026-05-23)" section at the bottom. **This is the PRIMARY GATE evidence for M3-Eval close per §5.6** — Codex does NOT re-judge per-case PASS / FAIL / IMPROVING (those are deliver-agent + human qualitative judgments per §5.6 2026-05-17 refinement) but DOES verify the manual review notes accurately summarize trace evidence + the M3 verdicts are consistent with what the cited result paths actually contain (spot-check 2-3 cases via `jq` against `eval_interactive/results/20260522-110537/results.json` + the isolated reruns).
10. **Code source files for cumulative cited-line spot-checking at HEAD `7562a2d`** (read on demand during §3 + §5 + §6 verification; NOT end-to-end):
    - `eval_interactive/eval_interactive/case_spec/schema.py` (S-Eval-1 schema demotions).
    - `eval_interactive/eval_interactive/scoring/hard_checks.py` + `outcome_checks.py` + `composite.py` (S-Eval-1 + S-Eval-2 + S-Eval-5 wiring) + `llm_judge.py` (S-Eval-5 L3 dim repositioning + 2 rubric updates + NEW `user_goal_achievement` dim) + `skill_procedure_check.py` (S-Eval-2 DSL parser; 6 frozen primitives).
    - `eval_interactive/eval_interactive/batch/executor.py` (S-Eval-5 Option A wiring; line 64 `_SKILLS_DIR` constant + line 293 `_compute_tier2_result` + line 308 `compute_composite(..., tier2_result=tier2_result)` + line 342 `_get_skill_extractor` + line 365 `_compute_tier2_result`).
    - `server/src/main/java/com/gumtree/csagent/service/runtime/skill/Skill.java` + `CriticalStep.java` + `SkillLoader.java` (S-Eval-2 schema + loader allowlist).
    - `server/src/main/java/com/gumtree/csagent/service/runtime/ContextProjectionBuilder.java` (S-Eval-2 projection rendering — list-of-objects format for `critical_steps`).
    - `server/src/main/resources/skills/*.yaml` (6 Skill YAMLs at HEAD `7562a2d`; S-Eval-3 populated content).
    - `eval_interactive/tests/test_s_eval_5_l3_repositioning.py` (S-Eval-5 regression suite; 30 tests).
    - `eval_interactive/case_specs/bad_cases/*.yaml` (12 bad cases at HEAD).
    - `eval_interactive/case_specs/anchor_outcome/*.yaml` (12 anchor_outcome cases; S-Eval-1 ship).

---

## 2. Cumulative scope claim

Walk the cumulative range `d91bd3d..7562a2d` and verify each sub-sprint shipped within its declared scope (per the respective `docs/sprints/sprint-NNN-objective.md` archive). For each sub-sprint:

1. **Sprint 42 (S-Eval-1)** — `git show d91bd3d --stat`; expected: schema demotions + scoring demotions + NEW `severity` field + 12-case `anchor_outcome/` suite + 20 NEW regression tests. Verify no out-of-scope file touched (no Java; no Skill YAMLs; no runtime).
2. **Sprint 43 (S-Eval-2)** — `git show 69ed77f --stat`; expected: NEW `CriticalStep.java` + `Skill.java` ctor extension + `SkillLoader.java` allowlist + `ContextProjectionBuilder.java` projection rendering + NEW Python `skill_procedure_check.py` (DSL parser) + 36 §1.7 structural-defence tests. Verify the DSL parser construction structurally forbids keyword / regex / message-content primitives (`tests/scoring/test_skill_procedure_dsl_parser_rejects_hardcodes.py` is the load-bearing test).
3. **Sprint 44 (S-Eval-3)** — `git show 01770ac --stat`; expected: 18 populated `critical_steps[].desc` strings across 6 Skill YAMLs (3+2+5+5+2+1 per `discover_triage`+`confirm`+`resolve_faq_grounded_answer`+`resolve_intake_collect_and_handover`+`escalate`+`terminal`). **Independently walk each `desc` string against proposal §5.3 standard** — re-verify the per-sub-sprint Codex `pass / 0` verdict still holds at M3-Eval close (no `desc` should be a row-3 hard if-else or row-4 keyword enumeration matrix).
4. **Sprint 45 (S-Eval-4)** — `git show 8b7ff40 --stat`; expected: 11 new bad-case YAMLs + manifest update + closure_criterion fields. Verify no `closure_criterion` references `case_passed = true` / `composite_score >= X` / keyword / regex / visible-eval CaseSpec id (per §5.6 + S-Eval-4 calibration notes section 4 anti-patterns NOT observed).
5. **Sprint 46 (S-Eval-5)** — `git show e0cd8aa --stat`; expected: `llm_judge.py` (L3 demotion + new dim + 2 rubric updates) + `composite.py` (advisory L3 filter) + `executor.py` (Option A wiring; +138 lines) + NEW test file `test_s_eval_5_l3_repositioning.py` (847 lines / 30 tests) + 4 R-item closure annotations in `docs/action_bank.md`. Plus follow-up `git show 7562a2d --stat`: §13 documentation-only update to `sprint-046-handoff.md` (real-LLM monotone-relaxing rerun PASS).

---

## 3. §4.1 nine-question anti-hardcode kernel walk (cumulative)

Walk each of the nine questions across the FULL M3-Eval cumulative range. For each "yes" or each concern, paste the diff snippet (cite commit + path:line) and the reasoning. Particular attention to:

- **Q1 (semantic hardcode added?)**: are the 18 populated `critical_steps[].desc` (S-Eval-3 content) soft narratives per the proposal §5.3 standard, or do any encode if-else / keyword-matrix / regex? Are the 2 S-Eval-5 rubric updates (R-l3-form-context-trust + R-l1-source-citation-quality) narrative-only (additive trust signal + tighter semantic requirement), or do they encode keyword / regex / per-UC-matrix? Is the NEW `user_goal_achievement` L3 dim rubric semantic-anchored (NOT keyword-anchored)? Is the Option A executor wiring (`_compute_tier2_result` iterates every Skill via `SkillProcedureExtractor.extract` whose DSL is structurally constrained per S-Eval-2 to forbid keyword / regex / message-content) introducing any NEW hardcode?
- **Q2 (Tier-0 invariant protection?)**: did M3-Eval add any new Tier-0 invariant? Expected: **NO** (per §6 fence #2 + inherited M2-close DEFER on C2 + C3 candidates).
- **Q3 (soft signal replacing hard branch?)**: do the L3 demotions move dims from hard gate contributors → advisory (HARD → SOFT direction)? Does the Option A wiring light up Tier-2 as a NEW hard gate, AND is that gate structurally constrained per S-Eval-2 (DSL parser anti-hardcode) + per S-Eval-3 Codex-verified (`desc` strings cleared §5.3)? The wiring lighting up a NEW hard gate is intentional per the M3-Eval north star; the question is whether the gate's STRUCTURE is anti-hardcode-compliant.
- **Q4 (eval-phrase / trace-specific phrasing / CaseSpec-id encoded?)**: walk each populated `desc` for any reference to visible-eval case text, trace-specific phrasing, or CaseSpec ids. Walk each S-Eval-5 rubric update for similar concerns (the citation-quality rubric example IDs `ka44J000000gKxqQAE` + `ka0xx00000xxxxxAAB` are ILLUSTRATIVE shapes of opaque IDs, not regex anchors — verify the rubric narrative makes this clear).
- **Q5 (LLM ownership shrunk?)**: does any M3-Eval change shrink what `iteration_governance.md` §1.3 says the LLM owns? Expected: **NO** (M3-Eval is eval_spec layer; the L3 demotions EXPAND LLM ownership on the eval-side by keeping advisory dims judged by LLM but removing them from gate contribution; the runtime semantic surfaces are UNCHANGED per §6 fences).
- **Q6 (prompt if-else added?)**: did any S-Eval-2/3/5 change touch `system_prompt.txt` / Skill YAML `procedure` text / Skill `critical_steps[].desc` with if-else structure? Walk Skill YAML procedure text + critical_steps content; verify no if-else / per-UC matrix / keyword enumeration introduced.
- **Q7 (tool schema / capability / PII / grounding floor preserved?)**: tool schema UNCHANGED across M3-Eval; PII / safety floor verified by Tier-0 12/12 PASS at M3-Eval close bad-case rerun + 185-case smoke/anchor/anchor_outcome PASS at S-Eval-5 §13; grounding floor STRENGTHENED by `R-l1-source-citation-quality-rubric` (bare Salesforce IDs now explicitly non-actionable per the new rubric).
- **Q8 (generalization coverage?)**: target / neighbor / negative / shadow counts. Target = 4 demoted L3 dims + 1 NEW dim + 2 rubric updates + 4 R-item closures + Option A wiring; neighbor = 12 bad cases re-loaded through new scoring path; negative = 2 critical L3 dims retained (`stall_quality` + `premature_finish`) + empty-`critical_steps` parity (S-Eval-2 backward-compat); shadow = `case_specs_shadow/` per `_ACCESS_BOUNDARY.md` (held out from dev session per S-Eval-5 §8 generalization stanza).
- **Q9 (rollback / sunset plan?)**: M3-Eval changes are intended permanent (the four-tier pyramid is the M3-Eval north star); no temporary guard / sunset trigger. N/A is correct for Q9.

---

## 4. M3-Eval §6 hard fences walk

Walk each of the 15 §6 hard fences in `docs/milestone_objective.md` and verify NO violation across the cumulative commit range:

1. No edits to `RuntimeIntentClassifier.java` / `IntentClassification.java` / `DriftResult.java` / `DriftDetector.java` / `UseCaseRouter.java` / `ClassifyUseCaseTool.java`.
2. No new Tier-0 invariant added.
3. No edits to existing case families under `eval_interactive/case_specs/case_families/<existing>/`.
4. No edits to existing shadow CaseSpecs under `eval_interactive/case_specs_shadow/case_families/<existing>/`.
5. No edits to existing cases under `eval_interactive/case_specs/smoke/` or `eval_interactive/case_specs/anchor/`.
6. No edits to the existing Alice bad case `alice_uc_a_uc_h_misclass.yaml`.
7. No edits to `eval_interactive/case_spec_overrides.yaml`.
8. No edits to `eval_interactive/eval_interactive/loader/` or `eval_interactive/eval_interactive/simulator/`.
9. No edits to `docs/foundational/`, `docs/runtime_freeze_and_risk_policy.md`, `docs/customer_service_tool_spec_v0_2.yaml`.
10. No edits to sprint archives `docs/sprints/sprint-001-*` through `docs/sprints/sprint-041-*` or milestone archives `docs/milestones/M1_objective.md` / `M2_objective.md` / `M2-Skill_objective.md` / `M2_codex-review.md`.
11. No widening of `escalation_reason` enum (canonical 23 values at `eval_interactive/eval_interactive/case_spec/schema.py:44-68` + `PhaseEvaluator.java:39-63`).
12. No `iteration_governance.md` edit during M3-Eval EXCEPT the S-Eval-1 §5.5 anchor_outcome sentence + the S-Eval-5 §5 acceptance-bar reference alignment (small wording updates only).
13. No modification of M2-landed Skill semantics (`SkillRegistry.select(...)` / `SkillStateBus.applyOnSkillSwitch(...)` / `SkillGuardrailDispatcher.dispatch(...)` / `Skill.guardrails` content / `Skill.state_inheritance` content / `Skill.procedure` text). M3-Eval ONLY adds the NEW `critical_steps` field + populates it.
14. No regex / keyword / message-content matching in the `trace_check` DSL (S-Eval-2 enforces via parser construction).
15. No if-else / keyword matrix / per-UC enumeration in `critical_steps[].desc` (S-Eval-3 self-walk per §5.3; per-sub-sprint Codex verified at S-Eval-3 close; **independently re-verify at M3-Eval close**).

---

## 5. Bad-case suite manual review verification

Per `iteration_governance.md` §5.6 (2026-05-17 refinement) the bad-case suite is a **human-judgment gate**, NOT a programmatic gate. Codex does NOT re-judge per-case PASS / FAIL / IMPROVING. Codex DOES verify:

(a) The 12-case verdicts in `eval_interactive/case_specs/bad_cases/_manifest.md` M3 column are consistent with the per-case trace evidence at the cited result paths. Spot-check at least 3 cases (suggest: 1 PASS + 1 IMPROVING + 1 FAIL) via:
- `jq '.case_results[] | select(.case_id == "<id>") | .transcript' eval_interactive/results/20260522-110537/results.json`
- Compare bot reply text against the case YAML's `bad_case_metadata.closure_criterion` field.
- Verify the deliver-agent's "Reasoning" column in the manifest M3-Eval close section accurately summarizes what the transcript shows.

(b) The 3 NEW R-items opened in `docs/action_bank.md` §5 (M3-Eval close surfaced 2026-05-23) correctly scope what they claim:
- `R-iwzx-uc-k-vs-uc-h-routing-spurious-distress` is `semantic_planner` layer (per §3.2 Q5) — verify by reading iwzx isolated rerun trace (`eval_interactive/results/20260522-162850/`) that the UC routing + escalation_reason failure mode matches the R-item description.
- `R-bad-case-parallel-session-establishment-flakiness` is `infra` / eval-harness layer — verify the parallel=4 batch run shows OOSR on cs012+fg5q+iwzx while parallel=1 isolated reruns show multi-turn completion.
- `R-bad-case-fixture-migrate-to-l3-judge-dims` is `eval_spec` fixture migration — verify by `grep -l "llm_judge_dimensions: \[\]" eval_interactive/case_specs/bad_cases/*.yaml` that 11 cases have empty L3 dim list and only Alice has the 3-dim list.

(c) The S-Eval-5 rubric updates land operationally (not just statically). Spot-check Alice's L3 scores in the rerun:
- `jq '.case_results[] | select(.case_id == "alice_uc_a_uc_h_misclass") | .l3_results' eval_interactive/results/20260522-110537/results.json` — expected: `tone_appropriateness=5.0` (form_context.first_name greeting accepted) + `groundedness=3.0` (generic policy citation not canonical URL).

If any spot-check surfaces inconsistency between the manifest verdict / R-item description / claimed operational evidence and the actual code / result paths, surface as a P0 finding for re-write before close.

---

## 6. Open question queue (16 items routed to M3-Eval-shared close)

Codex consumes the following 16 OQs surfaced across the 5 sub-sprint handoffs + M3-Eval close manual review (9 pre-S-Eval-5 carry-overs at items 1-9 + 7 S-Eval-5 OQ-S46.* at items 10-16). For each, the per-handoff source citation is given. Codex surfaces findings WITHOUT pre-decisions per `feedback_constitution_discipline_vs_planning_anticipation.md` — Codex's job is to assess + surface; deliver-agent + human decide disposition post-Codex.

1. **OQ-S42.3** — S-Eval-1 D-2.2 demotion interpretation. Source: `docs/sprints/sprint-042-handoff.md` §7. The L1 `escalation_reason_consistency` demotion to advisory: is the interpretation correct that the bot's family-level claim is the soft signal and the canonical L2 path is the hard signal? Codex independent reading welcomed.
2. **OQ-S43.1** — S-Eval-2 DSL parser blocklist surface. Source: `docs/sprints/sprint-043-handoff.md` §7. The DSL parser at `eval_interactive/eval_interactive/scoring/skill_procedure_check.py` rejects keyword/regex/message-content at parse time. Are there additional forbidden primitives Codex would surface as P1 / P2 worth adding to the rejection set?
3. **OQ-S43.4** — S-Eval-2 `tool_event_seq` unsuccessful-dispatch `success=false` ignored. Source: `docs/sprints/sprint-043-handoff.md` §7. The `tool_event_seq` primitive currently counts dispatched tool events regardless of success. Is this the right design — Codex weigh in?
4. **OQ-S44.6** — Python baseline runner-invocation confirmation. Source: `docs/sprints/sprint-044-handoff.md` §7 + S-Eval-3 close discussion. Discipline: `uv run python -m pytest` (NOT `uv run pytest`). Codex independently re-runs Python baseline at HEAD `7562a2d`: confirm `5 failed, 426 passed`?
5. **OQ-S45.1** — 3 legacy-migrated bad cases real-LLM evidence (wmkb / iwzx / fg5q). Source: `docs/sprints/sprint-045-handoff.md` §7. Was projected verdict at S-Eval-4 well-calibrated? **RESOLVED at M3-Eval close manual review** (wmkb IMPROVING + iwzx FAIL [new R-item] + fg5q PASS); Codex confirm the M3-Eval close ledger update is consistent.
6. **OQ-S45.3** — cs095 dual-encoding intentional pattern. Source: `docs/sprints/sprint-045-handoff.md` §7. cs095 (bad case; multi-layer FAILURE shape) and `anchor_outcome_uc_a_visibility` (anchor_outcome case; SUCCESS shape) both source from session `570Q5000008U5C9IAK`. Is the dual-encoding the intended pattern OR consolidate?
7. **OQ-S45.4** — cs095 placeholder framing fold-back. Source: `docs/sprints/sprint-045-handoff.md` §7. The `closure_criterion` "T1 placeholder ('I'm looking into this for you') without follow-on tool call" framing: rewording proposal — replace "placeholder reply" with "filler reply that does not name a concrete next step or trigger a tool call". Codex weigh in on fold-back to future bad-case authoring guidance.
8. **OQ-S45.5** — Carry-over from S-Eval-3 executor wiring decision. Source: `docs/sprints/sprint-045-handoff.md` §7. **CONSUMED by S-Eval-5 Option A** — verify the executor wiring landed at `eval_interactive/eval_interactive/batch/executor.py` and Tier-2 result is now consumed by `compute_composite` in the production eval-harness path.
9. **OQ-S45.6** — Carry-over UC-FP `consult-moderation-context-on-removal-explanation` calibration. Source: `docs/sprints/sprint-045-handoff.md` §7. **CONSUMED by S-Eval-5 §13 real-LLM rerun** — the `consult-moderation-context-on-removal-explanation` step ID was NOT in the failed-step-id list on the 159-case anchor POST run (per S-Eval-5 §13.4); no S-Eval-3 fix-iteration triggered. Verify.
10. **OQ-S46.1** — Real-LLM monotone-relaxing rerun deferred? Source: `docs/sprints/sprint-046-handoff.md` §7 + §13. **RESOLVED at S-Eval-5 §13 follow-up commit `7562a2d`**: 0 PASS→FAIL transitions across 185 cases; STOP signal #5 NOT triggered. Verify the §13 evidence matches what the cited `eval_interactive/results/20260522-085614/` (smoke pre) + `20260522-085721/` (anchor_outcome pre) + `20260518-091559/` (smoke pre alt) + `20260522-090056/` (anchor post) + comparable post-S-Eval-5 paths actually contain.
11. **OQ-S46.2** — Option A executor wiring iterates EVERY Skill vs Java `SkillRegistry.select(phase, useCase)` semantics. Source: `docs/sprints/sprint-046-handoff.md` §7. The eval-side wiring iterates all 6 Skills; Java runtime selects ONE skill per (phase, UC). Is this divergence acceptable (broader Tier-2 coverage as feature) OR should the eval-side replicate `select()` semantics?
12. **OQ-S46.3** — `_build_per_turn_trace` invoked twice per case. Source: `docs/sprints/sprint-046-handoff.md` §7. Minor structural smell; double-build cost negligible. M4+ housekeeping candidate.
13. **OQ-S46.4** — Test count 30 vs §2.8 expected 5-15. Source: `docs/sprints/sprint-046-handoff.md` §7 + §10. §7-a planned drift; each test pins a load-bearing assertion. Codex weigh in: acceptable scope OR consolidate?
14. **OQ-S46.5** — Per-run summary `mean_judge_score` per-advisory-dim split. Source: `docs/sprints/sprint-046-handoff.md` §7. Trend reports may want per-advisory-dim mean for visibility. M4+ reporting tweak candidate.
15. **OQ-S46.6** — L3 `stall_quality` dim redundant with L1 `stall_detector` path. Source: `docs/sprints/sprint-046-handoff.md` §7. Could be demoted in M4+ to consolidate stall signals on L1.
16. **OQ-S46.7** — 185 fixtures (smoke + anchor_outcome + anchor) do NOT opt into `user_goal_achievement` dim. Source: `docs/sprints/sprint-046-handoff.md` §13.7. **GENERALIZED via NEW R-item `R-bad-case-fixture-migrate-to-l3-judge-dims`** (M3-Eval close 2026-05-23; deferred to M4+). Verify the generalization is correct (bad cases also don't opt in except Alice).

NOTE: items 1-9 are pre-S-Eval-5 carry-overs (`OQ-S42.3` through `OQ-S45.6`); items 10-16 are S-Eval-5 dev-handoff-surfaced OQ-S46.* (`OQ-S46.1` through `OQ-S46.7`). Codex addresses all 16 items; the M3-Eval pre-dev handoff `compact/context-handoff-sprint-046-pre-dev.md` §4 listed 9 carry-overs before S-Eval-5 ran, and S-Eval-5 dev surfaced 7 more.

For EACH OQ, return:
- Codex assessment (1-3 sentences).
- Recommended disposition: `closed` / `closed-with-followup` / `surface-as-r-item` / `defer-to-future-milestone` / `human-architecture-decision`.

---

## 7. Output format

Write your verdict to `docs/codex-findings.md` at the top of the file (delete-and-add supersession per `feedback_packaging_codex_findings_supersession.md` — deliver-agent will archive the live file to `docs/milestones/M3-Eval_codex-review.md` after this review). Use the §4.2 4-line sprint-close header convention (this is a milestone-shared review but still writes per §4.2):

```
## Sprint Review Decision
decision: pass | fix_required | out_of_scope_review
blocking_count: <number>
summary: <one paragraph summarizing the cumulative M3-Eval verdict — architecture coherence + §1.7 compliance + §6 hard-fence honor + bad-case suite manual-review verification + 16-OQ disposition>
```

Then below the header:

1. **§3 nine-question kernel results** (one section per question; cite diff snippets for any "yes" or concern).
2. **§4 hard-fence walk results** (one row per fence; PASS / FAIL / N/A; cite evidence).
3. **§5 bad-case suite verification results** (per (a) / (b) / (c); spot-check evidence per case + per R-item).
4. **§6 OQ disposition table** (16 rows; one disposition per OQ).
5. **Optional Codex-surfaced new findings** (anything Codex sees that's not in the 16-OQ queue): surface as P0 / P1 / P2 / P3 with cite to commit:path:line; deliver-agent + human decide disposition post-Codex.
6. **Cumulative architecture coherence judgment** (1-2 paragraphs): does M3-Eval ship as one coherent Coarse-to-Fine Evaluation Architecture, OR are there inter-sub-sprint inconsistencies / dead code / orphaned wiring?

---

## 8. Anti-pre-decision discipline

Per `feedback_constitution_discipline_vs_planning_anticipation.md`: Codex's job is to assess + surface findings. Do NOT pre-decide whether a finding is M3-Eval-blocking vs M4+ deferral — surface the finding + the supporting evidence; deliver-agent + human + Codex jointly decide disposition at close. The §4.2 verdict header IS Codex's binding output on whether M3-Eval can close as PASS — but the per-finding routing (closed / surface-as-r-item / defer-to-future-milestone / human-architecture-decision) is collaborative.

Per `feedback_packaging_codex_findings_supersession.md`: write the verdict via delete-and-add supersession at the top of `docs/codex-findings.md`. The live file is the scaffold reset state; you ARE writing the milestone-shared review evidence here.

Do NOT edit any code. Do NOT propose code fixes beyond naming the layer in `docs/current/iteration_governance.md` §3 that the fix should target. Do NOT edit sprint archives (`docs/sprints/*`) or the milestone objective archive that will land at `docs/milestones/M3-Eval_objective.md` (deliver-agent owns that archive at close-out bundle time).
