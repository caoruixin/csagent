---
title: Sprint 44 Codex review archive — Populate `critical_steps` for the 6 Skills (M3-Eval sub-sprint 3; S-Eval-3) — per-sub-sprint review per §4.3 trigger #2
doc_tier: sprint-archive
status: archived
implementation_status: historical
source_of_truth: this file
last_reviewed: 2026-05-22
review_cadence: ad hoc
supersedes: []
superseded_by: null
archive_notes: >
  Archived from `docs/codex-findings.md` at S-Eval-3 close 2026-05-22
  per `feedback_packaging_codex_findings_supersession.md` delete-and-add
  pattern. Live `docs/codex-findings.md` is reset to scaffold immediately
  after this archive lands. Codex per-sub-sprint review per
  `iteration_governance.md` §4.3 trigger #2 (LLM-visible content
  authoring; §1.7 forbidden-list adjacent) returned **`decision: pass /
  blocking_count: 0`** on first pass, single round, against commit range
  `db19a47..01770ac` (single dev commit `01770ac`; 9 files; +1080 / -14).
  Codex independent §5.3 re-walk on each populated `desc`: 18× Row 1 ✅
  / 0× Row 2 / 0× Row 3 / 0× Row 4. §4.1 nine-question kernel Q1-Q8
  pass with Q9 N/A. §1.7 boundary check (a-e) all pass. Hard fences all
  empty-diff confirmed. Tier-0: no new candidate added. Contract drift
  ×2 §7-a planned: both accepted as planned-not-drift. 5 OQ verdicts
  surfaced (S44.1 OOSR-carry executor wiring; S44.2 bundled with S44.1;
  S44.3 calibration; S44.4 acceptable; S44.5 synthetic sufficient).
  One non-blocking concern: Python baseline reproducibility — Codex
  cited `5 failed / 396 passed` via `uv run python -m pytest` vs dev
  handoff's `9 failed / 392 passed` via `uv run pytest`. Surfaced by
  deliver-agent as OQ-S44.6 at S-Eval-3 close (routed to M3-Eval-shared
  Codex review queue for runner-invocation confirmation; non-blocking
  for S-Eval-3 close because both reproductions confirm no S-Eval-3-
  attributable regression). **S-Eval-4 UNBLOCKS.** See
  `docs/sprints/sprint-044-handoff.md` §12 for the close-time deliver-
  agent + human classification + OQ dispositions + carry-overs.
notes: >
  Sprint 44 / S-Eval-3 is the THIRD sub-sprint of NEW Milestone M3-Eval
  (Coarse-to-Fine Evaluation Architecture). Per `iteration_governance.md`
  §4.3 trigger #2 — sub-sprints introducing LLM-visible content
  adjacent to §1.7 forbidden-list territory require per-sub-sprint
  Codex review at close BEFORE the next sub-sprint begins. S-Eval-3
  populates 18 `critical_steps[].desc` entries across the 6 production
  Skill YAMLs (3 + 2 + 5 + 5 + 2 + 1; mandatory:advisory = 10:8) on
  top of the S-Eval-2 schema + structural-defence DSL parser (rejects
  15 hardcode-flavoured `trace_check` strings at parse time per
  `test_skill_procedure_dsl_parser_rejects_hardcodes.py`). The §5.3
  standard table (proposal §5 + sub-sprint contract §8 stanza
  reproduced) is the semantic defence; the DSL parser is the
  structural defence. Together they prevent §1.7 violations in
  LLM-visible territory. The Codex review prompt at
  `compact/sprint-044-review-prompt.md` carried the §4.1 9-question
  kernel + §5.3 standard table verbatim for independent re-walk + §1.7
  boundary check + hard-fence verification + schema/reproducibility
  checks + validation runs + 5 OQs surfaced WITHOUT pre-decisions per
  `feedback_constitution_discipline_vs_planning_anticipation.md`
  established pattern.
---

# Sprint 44 / S-Eval-3 Codex per-sub-sprint review archive

## Sprint Review Decision
decision: pass
blocking_count: 0
summary: S-Eval-3 passes the per-sub-sprint semantic gate: all 18 populated LLM-visible `critical_steps[].desc` entries are soft procedural narratives under proposal §5.3, all 18 `trace_check` strings parse through the S-Eval-2 DSL structural defence, and the commit stays inside the content/test/handoff scope. S-Eval-4 is unblocked. Carry forward OQ-S44.1 executor wiring, OQ-S44.2 active Skill/UC resolution, and the UC-FP moderation-context calibration before Tier-2 is made production-harness-active.

## Review Evidence
- Reviewed commit range `db19a47..01770ac` at HEAD `01770ac`; `git show --numstat 01770ac` reports 9 files and `1080 insertions(+), 14 deletions(-)`.
- Loaded the required governance/source docs: `AGENTS.md`, `docs/milestone_objective.md`, `docs/sprint_objective.md`, `docs/sprints/sprint-044-handoff.md`, `docs/solutions/m3_eval_milestone_proposal.md`, `docs/sprints/sprint-043-handoff.md`, `docs/current/iteration_governance.md`, and `docs/runtime_freeze_and_risk_policy.md`.
- Independently read the 6 Skill YAML `critical_steps:` blocks at HEAD and re-walked each `desc` against proposal §5.3; see the Q1 table below.
- Verified the S-Eval-2 structural defence surfaces in `eval_interactive/eval_interactive/scoring/skill_procedure_check.py:122`, `eval_interactive/eval_interactive/scoring/skill_procedure_check.py:623`, and `eval_interactive/tests/test_skill_procedure_dsl_parser_rejects_hardcodes.py:36`.
- Re-ran Java baseline, Python targeted structural-defence test, Skill loader sanity, and deterministic Tier-2 synthetic scenarios. Full Python baseline has a local runner reproducibility caveat recorded under Validation Runs.

## Blocking Findings (if any)
None.

## Anti-Hardcode Kernel (per `iteration_governance.md` §4.1)

### Q1 - Semantic Hardcode / Proposal §5.3 Walk
Verdict: pass. I classify all 18 entries as proposal §5.3 Row 1: soft procedural narrative. Several entries mention canonical UC ids, tool names, field names, or semantic user stance, but none encode a keyword/regex/message-content if-else or keyword enumeration matrix. Tool names come from `server/src/main/resources/config/tool-policy.yaml:2`; intake field names come from `server/src/main/java/com/gumtree/csagent/service/runtime/IntakeFieldsRegistry.java:53`. The parser pre-parses every populated `trace_check` at extractor construction (`eval_interactive/eval_interactive/scoring/skill_procedure_check.py:623`) and the curated hardcode examples reject message/regex/keyword surfaces (`eval_interactive/tests/test_skill_procedure_dsl_parser_rejects_hardcodes.py:36`).

| Skill | Step | Source | Verdict | Rationale |
|---|---|---:|---|---|
| `discover_triage` | `faq-uc-search-before-commit` | `server/src/main/resources/skills/discover_triage.yaml:34` | Row 1 pass | Procedural guidance to ground a FAQ-path classification in retrieval evidence; no user-message keyword trigger. |
| `discover_triage` | `removed-listing-route-with-moderation-context` | `server/src/main/resources/skills/discover_triage.yaml:55` | Row 1 pass | Uses projected listing-status/moderation state as observable context and explicitly keeps the understanding-vs-acting judgment with the LLM. |
| `discover_triage` | `intake-uc-classified-without-pre-search-noise` | `server/src/main/resources/skills/discover_triage.yaml:71` | Row 1 pass | Advises prompt classification for intake-shaped cases using form topic and semantic intake posture; no phrase list or hard branch. |
| `confirm` | `record-outcome-on-confirmed-resolve` | `server/src/main/resources/skills/confirm.yaml:32` | Row 1 pass | Phase hygiene: when resolution is confirmed, bind it with `record_outcome`; tool-name-grounded, not keyword-grounded. |
| `confirm` | `confirm-honours-user-dissatisfaction` | `server/src/main/resources/skills/confirm.yaml:50` | Row 1 pass | Describes semantic dissatisfaction posture in CONFIRM and healthy exits; no enumerated phrases and advisory severity avoids a hard semantic gate. |
| `resolve_faq_grounded_answer` | `search-knowledge-before-faq-answer` | `server/src/main/resources/skills/resolve_faq_grounded_answer.yaml:55` | Row 1 pass | Grounding-floor procedure for factual FAQ answers, consistent with runtime-owned grounding in `iteration_governance.md` §1.4. |
| `resolve_faq_grounded_answer` | `resolve-article-after-search-hit` | `server/src/main/resources/skills/resolve_faq_grounded_answer.yaml:76` | Row 1 pass | Tool-result sequence guidance with explicit no-hit handover exemption; no message-content matching. |
| `resolve_faq_grounded_answer` | `record-outcome-on-grounded-answer` | `server/src/main/resources/skills/resolve_faq_grounded_answer.yaml:96` | Row 1 pass | State-of-record procedure after a delivered FAQ-grounded answer; advisory for legitimate handover alternate paths. |
| `resolve_faq_grounded_answer` | `consult-moderation-context-on-removal-explanation` | `server/src/main/resources/skills/resolve_faq_grounded_answer.yaml:115` | Row 1 pass | UC-FP factual-removal explanation must be anchored in moderation context; a grounding rule, not a keyword rule. Calibration caveat in OQ-S44.3. |
| `resolve_faq_grounded_answer` | `reroute-on-actionable-appeal-intent` | `server/src/main/resources/skills/resolve_faq_grounded_answer.yaml:130` | Row 1 pass | Semantically describes a user's stance shifting from understanding to acting; it does not list appeal keywords or assign `active_use_case`. |
| `resolve_intake_collect_and_handover` | `uc-g-intake-complete-before-handover` | `server/src/main/resources/skills/resolve_intake_collect_and_handover.yaml:40` | Row 1 pass | Canonical UC-G field-completeness procedure from `IntakeFieldsRegistry`; field names are schema, not message keywords. |
| `resolve_intake_collect_and_handover` | `uc-h-intake-complete-before-handover` | `server/src/main/resources/skills/resolve_intake_collect_and_handover.yaml:54` | Row 1 pass | Canonical UC-H field-completeness procedure; this is the Alice Tier-2 intake-complete surface, not a CaseSpec phrase. |
| `resolve_intake_collect_and_handover` | `uc-i-intake-complete-before-handover` | `server/src/main/resources/skills/resolve_intake_collect_and_handover.yaml:70` | Row 1 pass | Canonical UC-I field-completeness procedure; no phrase/regex trigger. |
| `resolve_intake_collect_and_handover` | `uc-j-intake-complete-before-handover` | `server/src/main/resources/skills/resolve_intake_collect_and_handover.yaml:82` | Row 1 pass | Canonical UC-J field-completeness procedure; no visible-eval text. |
| `resolve_intake_collect_and_handover` | `uc-k-intake-complete-before-handover` | `server/src/main/resources/skills/resolve_intake_collect_and_handover.yaml:94` | Row 1 pass | Canonical UC-K field-completeness procedure; no keyword matrix. |
| `escalate` | `escalate-via-request-handover` | `server/src/main/resources/skills/escalate.yaml:31` | Row 1 pass | ESCALATE-phase tool-completion procedure using canonical `request_handover`; no semantic routing branch. |
| `escalate` | `record-outcome-after-handover-on-escalate` | `server/src/main/resources/skills/escalate.yaml:55` | Row 1 pass | Post-handover state-of-record ordering guidance; advisory, tool-sequence based. |
| `terminal` | `terminal-records-outcome` | `server/src/main/resources/skills/terminal.yaml:28` | Row 1 pass | CLOSE-phase final-disposition procedure; tool-name-grounded. |

Summary: Row 1 pass = 18/18; Row 2 edge = 0/18; Row 3 reject = 0/18; Row 4 reject = 0/18.

### Q1-Q9 Kernel Verdicts
| Q | Verdict | Citation | Independent rationale |
|---|---|---:|---|
| Q1 | pass | `server/src/main/resources/skills/resolve_faq_grounded_answer.yaml:130` | All 18 `desc` entries are Row 1 soft procedural narratives; all `trace_check` strings use only allowed primitives and parse at `SkillProcedureExtractor.from_skills(...)`. |
| Q2 | pass | `docs/runtime_freeze_and_risk_policy.md:43` | No Tier-0 invariant is added; `docs/runtime_freeze_and_risk_policy.md` §1/§2 is unchanged in the commit range. |
| Q3 | pass | `server/src/main/java/com/gumtree/csagent/service/runtime/ContextProjectionBuilder.java:872` | The change is the soft signal: LLM-visible `critical_steps[].desc`; no Java/prompt hard branch was added. |
| Q4 | pass | `server/src/main/resources/config/tool-policy.yaml:2` | No `cs_*` id, persona name, smoke/anchor fixture text, or message-content phrase appears in populated `desc`/`trace_check`; references are canonical tools and canonical intake fields. |
| Q5 | pass | `docs/current/iteration_governance.md:51` | LLM ownership is not shrunk: projection adds guidance, while user goal, UC hypothesis, next action, escalation posture, response strategy, and wording remain LLM-owned. |
| Q6 | pass | `server/src/main/resources/skills/confirm.yaml:31` | No `system_prompt.txt` edit and no Skill `procedure` edits; YAML diffs append `critical_steps:` after existing blocks only. |
| Q7 | pass | `docs/current/iteration_governance.md:62` | Tool schema, capability/permission, PII/safety, and grounding floors are unchanged; several descs reinforce grounding rather than weakening it. |
| Q8 | pass | `eval_interactive/eval_interactive/scoring/skill_procedure_check.py:659` | Target = 18 populated steps; neighbor fixtures are untouched; negative scoping returns N/A outside `mandatory_for`; shadow CaseSpecs were not read or edited. |
| Q9 | N/A | `docs/solutions/m3_eval_milestone_proposal.md:237` | The content is intended permanent as the Skill YAML single source of truth, not a temporary workaround. |

## §1.7 Boundary Check
| Item | Verdict | Rationale |
|---|---|---|
| (a) keyword/regex/if-else/enum entry added to runtime or prompt | pass | Commit touches no runtime semantic file or prompt; only 6 YAML `critical_steps` appendices plus checkpoint tests and handoff. `system_prompt.txt` is unchanged. |
| (b) per-UC matrix added | pass | `mandatory_for` lists are Tier-2 scoping lists: `SkillProcedureExtractor.extract()` returns N/A when `active_use_case` is not in the list (`eval_interactive/eval_interactive/scoring/skill_procedure_check.py:659`). They are not runtime routing branches. |
| (c) eval phrase / CaseSpec id encoded | pass | Populated steps contain no CaseSpec ids or persona names. The word "anchor" appears only in generic prose such as "route is anchored," not fixture references. |
| (d) tool schema / capability / PII / grounding floor preserved | pass | `tool-policy.yaml`, tool schemas, PII/safety surfaces, and FAQ grounding docs are unchanged; tool names in descs are canonical registry entries. |
| (e) LLM ownership shrink | pass | `ContextProjectionBuilder` renders `{id, desc}` as an additive soft narrative and states the LLM owns whether to act on it (`server/src/main/java/com/gumtree/csagent/service/runtime/ContextProjectionBuilder.java:872`). |

## Hard-Fence Verification
- pass - S-Eval-2/M2 Java Skill surfaces unchanged: `Skill.java`, `CriticalStep.java`, `SkillRegistry.java`, `SkillLoader.java`, `SkillStateBus.java`, `SkillGuardrailDispatcher.java`, `StateInheritance.java`, `Guardrail.java`, and `ContextProjectionBuilder.java` have empty `git diff db19a47..01770ac`.
- pass - Runtime semantic surfaces unchanged: `RuntimeIntentClassifier.java`, `IntentClassification.java`, `DriftResult.java`, `DriftDetector.java`, `UseCaseRouter.java`, `ClassifyUseCaseTool.java`, `PhaseEvaluator.java`, `AgentRunLoopImpl.java`, `ControlKernel.java`, `system_prompt.txt`, `tool-policy.yaml`, `IntakeFieldsRegistry.java`, `UseCaseRegistryService.java`, and `ResolveDispositionEvaluator.java` have empty diffs.
- pass - Skill YAML non-`critical_steps` fields unchanged: each of the 6 YAML diffs appends `critical_steps:` after `state_inheritance` only; no `procedure`, `guardrails`, `state_inheritance`, `applicable_use_cases`, or `system_instruction` text changed.
- pass - Eval fixtures and shadow fixtures unchanged: empty diff for `eval_interactive/case_specs`, `eval_interactive/case_specs_shadow`, and `eval_interactive/case_spec_overrides.yaml`.
- pass - Eval harness/loader/simulator unchanged: empty diff for `eval_interactive/eval_interactive/loader`, `simulator`, `case_spec/schema.py`, `case_spec/loader.py`, `scoring/hard_checks.py`, `scoring/outcome_checks.py`, and `scoring/llm_judge.py`.
- pass - S-Eval-2 Python scoring surfaces unchanged: empty diff for `skill_procedure_check.py` and `composite.py`; `composite.py` still defaults missing `tier2_result` to empty-list advisory PASS (`eval_interactive/eval_interactive/scoring/composite.py:213`).
- pass - Governance/foundational/milestone docs unchanged by dev: empty diff for `docs/current`, `docs/foundational`, `docs/runtime_freeze_and_risk_policy.md`, `docs/milestone_objective.md`, `docs/milestones`, and `docs/codex-findings.md`; the only `docs/sprints` addition is the new Sprint 44 handoff archive.
- pass - `docs/codex-findings.md` was scaffold-only before this review and had empty diff in `db19a47..01770ac`; this file is now written by Codex as requested.

## Schema And Reproducibility Checks
- pass - Numstat reproduced: Skill YAML additions are `56 + 39 + 92 + 67 + 49 + 24 = 327` lines and 0 deletions; total commit is 9 files, `+1080 / -14`.
- pass - Per-Skill counts reproduced by YAML parse/grep: `discover_triage=3`, `confirm=2`, `resolve_faq_grounded_answer=5`, `resolve_intake_collect_and_handover=5`, `escalate=2`, `terminal=1`; total = 18.
- pass - Severity split reproduced: `severity: mandatory` = 10 and `severity: advisory` = 8.
- pass - All 18 `trace_check` strings parse via `SkillProcedureExtractor.from_skills(...)`; no `TraceCheckDSLSyntaxError` raised.
- pass - Structural-defence negative library remains aligned: the 15 hardcode-flavoured examples in `eval_interactive/tests/test_skill_procedure_dsl_parser_rejects_hardcodes.py:36` are message/regex/keyword-list patterns, and no populated `trace_check` resembles them.
- concern, non-blocking - Full Python count from the exact handoff command is not reproducible in this local checkout: `uv run pytest` invokes a stale/broken pytest entrypoint (`.venv/bin/pytest` shebang points at another checkout) and exits 139 under `set -o pipefail`. The equivalent project-interpreter invocation `uv run python -m pytest` completes with `5 failed, 396 passed`, not `9 failed, 392 passed`. The 5 failures are the same pre-existing classes named in the handoff subset: 2 enum-sync, 2 override-count/pipeline, 1 corpus-lint aggregate. No S-Eval-3-specific failure appears.

## Validation Runs
- Java baseline: `mvn test 2>&1 | grep "Tests run" | tail -1` returned exactly `[ERROR] Tests run: 1163, Failures: 1, Errors: 0, Skipped: 2`.
- Python baseline: exact `uv run pytest` is locally blocked by the stale/broken pytest entrypoint noted above; `uv run python -m pytest 2>&1 | tail -1` returned `======================== 5 failed, 396 passed in 12.43s ========================`.
- §1.7 structural defence: exact `uv run pytest tests/test_skill_procedure_dsl_parser_rejects_hardcodes.py -v` hits the same runner issue; `uv run python -m pytest tests/test_skill_procedure_dsl_parser_rejects_hardcodes.py -v` returned `36 passed`.
- Skill loader sanity: `load_skills_from_dir('../server/src/main/resources/skills')` loaded 6 Skills and total `critical_steps=18`; extractor construction parsed all `trace_check` strings cleanly.
- Tier-2 deterministic verification: independently exercised the dev's synthetic shapes with `SkillProcedureExtractor.extract(...)` + `tier2_results_to_gate(...)`; Alice UC-H empty-fields trace fails `uc-h-intake-complete-before-handover`, Alice-as-UC-A FAQ trace fails `search-knowledge-before-faq-answer`, UC-FP no-moderation trace fails `consult-moderation-context-on-removal-explanation`, and the UC-A/UC-FP negative scoping traces pass with non-applicable steps excluded.

## Tier-0 Candidate Independent Verification (if applicable)
No new Tier-0 candidate. `docs/runtime_freeze_and_risk_policy.md` §1/§2 is unchanged, and S-Eval-3 adds no runtime invariant or Java guard. Existing C2/C3 candidate dispositions remain outside this sub-sprint.

## OQ Independent Verification
- OQ-S44.1 executor wiring gap: acceptable as-is for S-Eval-3, with carry-over. The gap is real (`eval_interactive/eval_interactive/batch/executor.py:252` does not pass `tier2_result=`), but executor wiring is outside S-Eval-3 §5/§6 and the content review can close on synthetic Tier-2 evidence. S-Eval-4/S-Eval-5 planning must decide where the production harness call-site is wired before Tier-2 can be a true eval hard gate.
- OQ-S44.2 active Skill / active UC resolution: acceptable to defer with OQ-S44.1. I agree this belongs at the extractor call-site design: expected Skill/UC, actual Skill/UC, or both. The synthetic evidence shows both Alice interpretations surface meaningful failures, so S-Eval-3 content is not blocked.
- OQ-S44.3 UC-FP moderation-context mandatory tightness: semantically defensible for a factual UC-FP removal explanation, but calibration is needed before executor wiring makes it production-active. If a legitimate no-context path escalates or defers instead of answering, convert the trace check to accept that path or downgrade the step to advisory.
- OQ-S44.4 mandatory/advisory split: acceptable. The 10 mandatory steps cover grounding, intake completeness, escalation handover, terminal outcome, and UC-FP moderation grounding; the 8 advisory steps cover heterogeneous or alternate-path-prone behaviours. Only the UC-FP no-context path needs close calibration.
- OQ-S44.5 synthetic vs real-LLM verification: synthetic evidence is sufficient for S-Eval-3 close because the sprint is content-only and the production executor is not wired. A real-LLM run is still needed by M3-Eval close, after executor wiring/resolution semantics are decided, to confirm actual rendered projection and trace shape.

## Contract Drift Independent Verification
- Accepted as planned-not-drift: `SkillCriticalStepsLoadingTest.java` updates the S-Eval-2 checkpoint test from empty `critical_steps` to populated counts and field-presence assertions (`server/src/test/java/com/gumtree/csagent/service/runtime/skill/SkillCriticalStepsLoadingTest.java:84`). This is the expected S-Eval-2-to-S-Eval-3 checkpoint retirement.
- Accepted as planned-not-drift: `test_skill_procedure_extractor.py::TestProductionSkillLoad` updates the empty-production-skill assertion to the populated per-Skill count map and loosens the crash smoke to list-shape only (`eval_interactive/tests/test_skill_procedure_extractor.py:531`). This matches the same checkpoint-retirement pattern, not scope creep.
- No production Java/Python source was changed to satisfy these tests; the write set remains disjoint from hard-fenced runtime/extractor/composite code.

## Deferred / Non-Blocking Notes
- Token-cost observation independently matches the handoff direction: worst active Skill payload is `resolve_faq_grounded_answer` at about 728 tokens, `resolve_intake_collect_and_handover` about 603, and a DISCOVER->RESOLVE->CONFIRM->CLOSE mean is about 377 tokens/turn, within the proposal §5.4 estimate and the ≤1000-token milestone bar.
- OQ-S43.2 id cross-reference usage: no populated `desc` cross-references another step id; ids still serve as stable authoring/eval identifiers. Record at M3-Eval close when deciding whether the projected id field is worth keeping.
- OQ-S43.5 session flag reliance: zero populated `trace_check` strings use `session.<flag>_present`; the S-Eval-2 fallback permissiveness question remains dormant.
- S-Eval-3 content does not cover the proposal's optional `create_case_controlled` before `request_handover` process surface. I do not block S-Eval-3 because the live `resolve_intake_collect_and_handover` Skill exposes `request_handover` as the required tool and the sprint's Alice gate is intake-complete-before-handover, but M3-Eval close should decide whether controlled-case creation needs a future Tier-2 step.
- OQ-S43.1 DSL blocklist surface and OQ-S43.4 unsuccessful `tool_event_seq` semantics remain M3-Eval-shared review items; S-Eval-3 content neither relies on `session.<flag>_present` nor `create_case_controlled` ordering.
