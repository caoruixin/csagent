# Deliver-agent context handoff — M3-Eval CLOSE (post-close)

**Authored:** 2026-05-23 by deliver-agent at M3-Eval milestone close (post Codex review + bad-case manual review + close-out bundle)
**For:** the next deliver-agent instance picking up M4+ planning round OR any further M3-Eval housekeeping
**Branch:** `refactor/remove-the-shackles`
**Repo:** `/Users/caoruixin/projects/csagent-latest`
**HEAD at M3-Eval close**: `7562a2d` (S-Eval-5 OQ-S46.1 real-LLM monotone-relaxing rerun PASS follow-up; LAST M3-Eval dev commit)
**Working tree at this handoff write time**: M3-Eval close-out bundle uncommitted (deliver-agent files staged for human's next commit per `feedback_commit_at_end_bundles_deliver_artefacts.md`).

Read order on cold start: this file → `AGENTS.md` (auto-loads `iteration_governance.md` + `doc_governance.md` + `agent_context_guide.md`) → `compact/sprint-deliver-orchestrator.md` (deliver-agent role doctrine; do NOT duplicate here) → `docs/milestones/M3-Eval_objective.md` (M3-Eval north star + §12 closure verdict) → `docs/milestones/M3-Eval_codex-review.md` (cumulative Codex review verdict) → `docs/milestone_objective.md` (M4+ planning placeholder) → `docs/sprint_objective.md` (sub-sprint planning placeholder).

---

## 1. 背景

- Repo builds a customer-service agent for an online classifieds marketplace. **LLM-first**: LLM owns semantic understanding (goal, drift, UC hypothesis, escalation posture, response strategy, customer-facing wording); Java/Python runtime owns deterministic boundaries (tool schema, capability, PII/safety floor, grounding floor, idempotency, persistence, trace/eval contract).
- Governance chain (auto-loaded via `@AGENTS.md`): `docs/current/doc_governance.md` + `docs/current/agent_context_guide.md` + `docs/current/iteration_governance.md` (Constitution §1 / Failure Brief §2 / Fix Layer §3 / Anti-Hardcode §4 / Eval Acceptance §5 incl. §5.5 smoke→observation + §5.6 / §5.6.1-§5.6.3 bad-case suite as human-judgment gate / Architecture Health §6 / §7 stanza / **§8 Milestone framework**).
- **Multi-agent collaboration**: Human → Research-agent (proposes) → Deliver-agent (plans + drafts contracts + prompts) → Dev-agent Claude Code (implements) → Review-agent Codex (reviews). Agents do NOT share chat history; cross-session continuity via repo docs + compact handoff files.
- **M3-Eval CLOSED 2026-05-23** (third milestone under §8 framework; closed A — Clean PASS with deliver-agent documentation-only fix-iteration on P0-F1 evidence-package inconsistency).

## 2. M3-Eval close state at this handoff write

### 2.1 Milestone-level

- **Classification**: A — Clean PASS (with documentation-only fix-iteration on deliver-agent evidence-package).
- **Cumulative commit range**: `d91bd3d..7562a2d` (10 commits: 5 sub-sprint dev + 4 deliver-agent close-out + 1 OQ-S46.1 follow-up).
- **Sub-sprint count**: 5 (range maximum per `iteration_governance.md` §8.5 5-sub-sprint cap; all closed Clean PASS per per-sub-sprint verdicts; only S-Eval-3 received per-sub-sprint Codex review per §4.3 trigger #2 — `pass / 0` first pass single round).
- **Codex milestone-shared review verdict**: `decision: fix_required / blocking_count: 1` on cumulative range; the 1 blocker (P0-F1) was deliver-agent documentation inconsistency in `_manifest.md` (Tier-0 "12 of 12 PASS" sentence contradicted by cs001 `escalation_compliance` L1 fail); deliver-agent applied documentation-only fix per human-accepted Path 2 disposition; NOT a re-Codex round; NO S-Eval-N code change requested. Codex P2-F2 non-blocking finding (hard fence #9 v0_2 deletion documented as inherited supersession housekeeping exception) addressed via the same manifest update.
- **Architectural axis**: PASS (no §1.7 hardcode; 15 hard fences honored with #9 documented exception; 16 OQs disposed — 10 closed + 2 closed-with-followup + 2 surface-as-r-item + 2 human-architecture-decision + 2 defer-to-future-milestone; cumulative architecture coherence judgment PASS).
- **Bad-case suite manual review at M3-Eval close 2026-05-23** (PRIMARY GATE per §5.6): real-LLM rerun against HEAD `7562a2d` (Moonshot `moonshot-v1-32k`). Distribution **PASS × 5 (cs001, cs014, cs029, cs066, fg5q) + IMPROVING × 4 (alice, cs011, cs012, wmkb) + FAIL × 3 (cs015, cs095, iwzx — all 3 are documented bad-case raison d'être failure shapes, NOT regressions) + OOSR × 0 at parallel=1**.
- **Tier-0 safety floor**: invariants 12/12 PASS where measured (`no_pii_leakage` / `no_human_only_tool_exposure` / `no_critical_policy_violation` / `phase_transition_validity`); `escalation_compliance` 11/12 (cs001 fails as the documented closure_criterion sub-shape failure; NOT M3-Eval-introduced regression).
- **Java baseline**: `1163 / 1-inherited / 0 / 2` UNCHANGED across all 5 sub-sprints (M3-Eval ships 0 Java production source).
- **Python baseline at HEAD `7562a2d`**: `5 failed, 426 passed` (5 pre-existing failures tracked via R-item `R-stale-test-escalation-enum-sync-v0_2-to-v0_3-migration`; +30 NEW tests in `test_s_eval_5_l3_repositioning.py` from S-Eval-5).

### 2.2 Sub-sprint-level (per-sub-sprint outcomes)

| Sub-sprint | Commit(s) | Outcome | Per-sub-sprint Codex |
|---|---|---|---|
| Sprint 42 / S-Eval-1 | `d91bd3d` (dev) + `357e949` (close + M3-Eval setup) | A — Clean PASS | Deferred to M3-Eval milestone-shared close per §4.3 default |
| Sprint 43 / S-Eval-2 | `69ed77f` (dev) + `db19a47` (close + v0_2 supersession) | A — Clean PASS | Deferred to M3-Eval milestone-shared close per §4.3 default |
| Sprint 44 / S-Eval-3 | `01770ac` (dev) + `4dafaf5` (close + per-sub-sprint Codex archive) | A — Clean PASS | **§4.3 trigger #2 fired** (`critical_steps[].desc` LLM-visible); Codex `pass / 0` first pass single round archived at `docs/sprints/sprint-044-codex-review.md` |
| Sprint 45 / S-Eval-4 | `8b7ff40` (dev) + `a4a3bc6` (close + S-Eval-5 launch) | A — Clean PASS | Deferred to M3-Eval milestone-shared close per §4.3 default |
| Sprint 46 / S-Eval-5 | `e0cd8aa` (dev bundle) + `7562a2d` (OQ-S46.1 real-LLM monotone-relaxing rerun follow-up) | A — Clean PASS | Deferred to M3-Eval milestone-shared close per §4.3 default |

### 2.3 R-item flips during M3-Eval

- ✅ **4 closed by S-Eval-5** (annotations at `docs/action_bank.md` §6 lines 397 / 408 / 409 / 412; commit `e0cd8aa`):
  - `R-l3-judge-form-context-trust-rubric` — closed via `_judge_tone_appropriateness` rubric trust-signal addition.
  - `R-l1-source-citation-quality-rubric` — closed via `_judge_groundedness` rubric tightening (bare Salesforce IDs explicitly non-actionable).
  - `R-cs038-l3-review-intake-efficiency` — closed via cumulative four-tier architectural shift (S-Eval-1 schema simplification + S-Eval-5 L3 advisory demotion).
  - `R-cs040-l3-review-intake-completion-semantics` — closed via cumulative four-tier decomposition (Tier-1 `user_goal_achievement` + Tier-2 `skill_procedure_followship`).

- 🔁 **2 unblocked-but-not-closed graduated to follow-on**:
  - `R-case-spec-overrides-schema-scoring-extension` (S-Eval-1 schema-block lifted; full closure depends on follow-on sprint).
  - `R-escalation-reason-runtime-evidence-contract-review` (S-Eval-1 escalation_trigger exact-match demotion touched; full closure requires runtime evidence — out of M3-Eval scope).

- 🆕 **5 NEW R-items opened during M3-Eval** (all deferred to M4+; recorded in `docs/action_bank.md` §5):
  1. `R-stale-test-escalation-enum-sync-v0_2-to-v0_3-migration` (S-Eval-1 close 2026-05-21) — `infra` / eval-harness; the 2 failing `test_escalation_enum_sync.py` tests reference deleted v0_2 path.
  2. `R-bad-case-suite-uc-ghij-seed-from-real-sessions` (S-Eval-4 close 2026-05-22) — `eval_spec`; UC-G/H/I/J primary coverage gap.
  3. `R-iwzx-uc-k-vs-uc-h-routing-spurious-distress` (M3-Eval close 2026-05-23) — `semantic_planner`; iwzx UC-K hold case routes to UC-H intake-lock + spurious user_distress.
  4. `R-bad-case-parallel-session-establishment-flakiness` (M3-Eval close 2026-05-23) — `infra` / eval-harness; OOSR cluster only manifests at parallel=4 batch.
  5. `R-bad-case-fixture-migrate-to-l3-judge-dims` (M3-Eval close 2026-05-23) — `eval_spec` fixture migration; 11 S-Eval-4 bad cases have `llm_judge_dimensions: []` (only Alice opts in).

- 🆕 **2 Codex `surface-as-r-item` candidates** (from M3-Eval-shared review §6 OQ disposition table):
  - OQ-S46.6 — `stall_quality` L3 dim potentially redundant with L1 `stall_detector` path; M4+ signal-consolidation candidate.
  - OQ-S46.7 — Smoke/anchor/anchor_outcome fixture gap on `user_goal_achievement` (broader than the bad-case-specific R-item already opened); M4+ broader fixture-migration candidate.

### 2.4 Tier-0 candidate disposition

- **C2 (Skill guardrail non-overridability)**: DEFERRED continued per M2-close DEFER verdict. M3-Eval shipped 0 runtime production source; production trace observation that would warrant elevation not accumulated. Status unchanged in `docs/action_bank.md` §5.2 as `R-skill-guardrail-non-overridability-tier-0`.
- **C3 (Skill state bus boundary enforcement)**: DEFERRED continued per M2-close DEFER verdict; same rationale. Status unchanged as `R-skill-state-bus-boundary-enforcement-tier-0`.

## 3. M3-Eval close-out bundle (deliver-agent files; uncommitted at this handoff write time)

Per `feedback_commit_at_end_bundles_deliver_artefacts.md`: deliver-agent does NOT stage; human bundles at separate close commit. The following files are PREPARED but uncommitted at the time of this handoff write:

- `docs/sprints/sprint-046-handoff.md` — §12 closure verdict filled by deliver-agent + human (sub-sprint A — Clean PASS).
- `docs/sprints/sprint-046-objective.md` — NEW archive (copy of `docs/sprint_objective.md` at S-Eval-5 close).
- `docs/milestones/M3-Eval_objective.md` — NEW archive (copy of `docs/milestone_objective.md` with §12 closure verdict appended; front-matter updated to `doc_tier: milestone-archive` / `status: archived` / `implementation_status: implemented`).
- `docs/milestones/M3-Eval_codex-review.md` — NEW archive (copy of `docs/codex-findings.md` Codex M3-Eval-shared review with archive front-matter prepended).
- `docs/milestones/M2_objective.md` — superseded_by field updated from "docs/milestone_objective.md (M3 candidate selection pending)" → "docs/milestones/M3-Eval_objective.md".
- `docs/codex-findings.md` — RESET to scaffold per `feedback_packaging_codex_findings_supersession.md` delete-and-add supersession.
- `docs/action_bank.md` — Sprint 46 row added to §6; M3-Eval row added to §6.5; 3 NEW R-items added to §5 (M3-Eval close surfaced 2026-05-23) with full description + 3 impl paths + anti-framings + prerequisites + disposition; manifest M3 column verdicts cross-referenced.
- `docs/10-handoff.md` §1 lead refreshed (line 9 replaced from 27k-char M3-Eval-IN-PROGRESS narrative to 4-paragraph M3-Eval-CLOSED + M4+-pending lead with preceding-milestone + earlier-milestones structure; date updated 2026-05-22 → 2026-05-23).
- `docs/sprint_objective.md` — RESET to next-milestone-TBD planning placeholder.
- `docs/milestone_objective.md` — RESET to next-milestone-TBD planning placeholder.
- `eval_interactive/case_specs/bad_cases/_manifest.md` — M3 column verdicts populated (12 cases); NEW section "M3-Eval close real-LLM rerun + manual review (2026-05-23)" appended; Tier-0 evidence-package fix applied per Codex P0-F1 (partial-pass sentence replaces blanket "12 of 12 PASS"; hard-fence #9 v0_2 deletion documented exception added per Codex P2-F2).
- `compact/context-handoff-M3-Eval-post-close.md` — THIS FILE.
- `compact/M3-Eval-review-prompt.md` — Codex review prompt (drafted before dispatch; stays as historical artifact).
- `compact/context-handoff-sprint-046-pre-dev.md` — pre-dev handoff (was untracked at session start; bundled by human at commit time).

**Human's next action**: bundle these files in a single deliver-agent close commit. Suggested commit message: `docs: M3-Eval close (A — Clean PASS with deliver-agent doc-only fix-iteration on P0-F1)`.

## 4. M4+ planning candidates

From `docs/milestones/M3-Eval_objective.md` §12.10 (ordered roughly by release-gate proximity + dependency):

1. **M3-B Single Handover Orchestrator** — `docs/release_gate.md` §1.1 release-gate-blocker; consume `D-single-handover-orchestrator` and related R-items from `docs/action_bank.md` §3 / §4. Pick if Salesforce cutover calendar pressure surfaces.
2. **M4 Bad-case fixture migration + parallel-session flakiness fix** — consume `R-bad-case-fixture-migrate-to-l3-judge-dims` + `R-bad-case-parallel-session-establishment-flakiness` (both M3-Eval close surfaced). Smaller scope; cleanup-flavored; reduces eval-harness debt.
3. **M4 UC-G/H/I/J bad-case seeding** — consume `R-bad-case-suite-uc-ghij-seed-from-real-sessions` (S-Eval-4 close surfaced). Requires real-session source material; mock-account-experiment-driven (Alice precedent).
4. **M4 Semantic-planner soft-signal extension for iwzx** — consume `R-iwzx-uc-k-vs-uc-h-routing-spurious-distress` (M3-Eval close surfaced) + related per-UC moderation-context projection enhancements. Semantic_planner layer; LLM-first soft-signal expansion path.
5. **M4 M3-Corpus** — separate parallel-track milestone consuming `R-corpus-coverage-audit-per-uc` (preceding M3-Eval R-item; runtime-corpus-coverage scope; per-UC FAQ corpus audit).
6. **M4 Latency / Skill-Tuning / Tier-0 re-evaluation** — preceding M2-close candidates; remain in slate.

**Selection methodology at next planning round**:
- Path 1 vs Path 2 decision per `compact/sprint-deliver-orchestrator.md` Workflow inputs.
- M3-B is bad-case-blocked (release-gate-driven); 2/3/4 are bad-case-driven (Path 2; require research-agent investigation); 5/6 are research-driven (Path 1).
- Human + deliver-agent pick; research-agent investigation required for Path 2 candidates BEFORE deliver-agent scopes the milestone.

## 5. Process discipline (carry-overs that REMAIN ACTIVE for M4+)

- **Numbers-cite per `feedback_deliver_agent_cited_numbers_must_be_reproducible.md`**: every count reproducible from `git show --numstat <commit>` / `wc -l` / `grep -c` / test-run direct output. **Python baseline MUST use `uv run python -m pytest`** per OQ-S44.6 carry-over. **§3 numstat sub-tally arithmetic MUST validate by re-summing** per the fourth+ instance fold-back observation (S-Eval-4 close §12.2 minor non-blocking; M3-Eval close M3-Eval-shared review confirmed via Python baseline `5 failed, 426 passed`).
- **Bundle policy per `feedback_commit_at_end_bundles_deliver_artefacts.md`**: dev does NOT stage deliver-agent files. Deliver-agent close-out bundled separately by human.
- **Handoff §12 closure verdict ownership per `feedback_handoff_verdict_section_delegation.md`**: dev leaves §12 EMPTY; deliver-agent + human (no Codex at sub-sprint close UNLESS §4.3 trigger fires) jointly fill.
- **OOSR-with-packaging-note pattern per `feedback_out_of_scope_review_packaging_rollforward.md`**: if M4+ close surfaces findings out-of-scope, classify as OOSR + roll forward to M5+.
- **No pre-decisions on OQs surfaced to Codex** per `feedback_constitution_discipline_vs_planning_anticipation.md` — deliver-agent surfaces findings + supporting evidence; Codex assesses + dispositions are collaborative at close.
- **Delete-and-add supersession** per `feedback_packaging_codex_findings_supersession.md`: codex-findings.md → archive at milestone close to `docs/milestones/M<N>_codex-review.md` then reset scaffold (applied at M3-Eval close 2026-05-23).
- **Constitution discipline vs planning anticipation** per `feedback_constitution_discipline_vs_planning_anticipation.md`: planning-anticipated changes that are NOT load-bearing for the current scope are CONSIDERED-AND-DROPPED, not silently applied.
- **Probe sprint shape for conditional broadening** per `feedback_probe_sprint_shape_for_conditional_broadening.md`: when a sprint must investigate before deciding scope, ship investigation-only docs-only probe sprint first; do NOT pre-decide structural action.
- **Mocked-LLM cannot prove prompt-causal change** per `feedback_mocked_llm_cannot_prove_prompt_causal_change.md`: real-LLM evidence required for prompt-causal verdicts (M3-Eval honored via OQ-S46.1 real-LLM rerun follow-up).

## 6. Hard fences inherited (still ACTIVE for M4+)

Inherited from M1 + M2 + M3-Eval; remain LOAD-BEARING:

- No new Tier-0 invariant without explicit human review + research-agent + Codex evidence. C2 + C3 candidates STAY DEFERRED until production trace observation accumulates.
- No edits to runtime semantic surfaces: `RuntimeIntentClassifier` / `IntentClassification` / `DriftResult` / `DriftDetector` / `UseCaseRouter` / `ClassifyUseCaseTool` / `PhaseEvaluator` / `AgentRunLoopImpl` / `ControlKernel` / `system_prompt.txt` / `tool-policy.yaml` / `IntakeFieldsRegistry` / `UseCaseRegistryService` / `ResolveDispositionEvaluator` — unless scoped explicitly in the milestone objective.
- No widening of `escalation_reason` enum (canonical 23 values at `eval_interactive/eval_interactive/case_spec/schema.py:47` + `PhaseEvaluator.java:39-63`).
- No silent restoration of superseded `docs/customer_service_tool_spec_v0_2.{md,yaml}` (anti-framed in `R-stale-test-escalation-enum-sync-v0_2-to-v0_3-migration`).
- No regex / keyword / message-content matching in the `trace_check` DSL (S-Eval-2 enforces via parser construction).
- No if-else / keyword matrix / per-UC enumeration in `critical_steps[].desc` (S-Eval-3 self-walk per §5.3; verified by Codex per-sub-sprint review).
- No editing of existing case fixtures (smoke / anchor / anchor_outcome / case-families / shadow / overrides) without milestone-scope explicit authorization.
- No editing of sprint archives `docs/sprints/sprint-NNN-*` or milestone archives `docs/milestones/M<N>_*` (immutable per `doc_governance.md`).
- No editing of `docs/foundational/` or `docs/runtime_freeze_and_risk_policy.md` outside scheduled fold-back cadence.
- No `iteration_governance.md` edit outside scheduled fold-back cadence except small alignment wording (M3-Eval honored via S-Eval-1 §5.5 anchor_outcome sentence + S-Eval-5 §5 acceptance bar reference alignment).

## 7. Term definitions (reference)

- **§5.3 standard table** (M3-Eval proposal §5.3): 4-row table for evaluating LLM-visible `desc` strings (row 1 ✅ soft procedural; row 2 ⚠ soft diagnostic edge; row 3 ❌ hard if-else; row 4 ❌ keyword enumeration matrix). Codex S-Eval-3 per-sub-sprint walk: 18× row-1 ✅.
- **6 frozen DSL primitives** (S-Eval-2 `skill_procedure_check.py`): `accumulated_tool_results.<tool>` / `tool_event_seq(<a>) < tool_event_seq(<b>)` / `intake_state.fields_collected.contains(<field>)` / `session.<flag>_present` / `any_of(...)` / `all_of(...)`. Parser rejects anything else at parse time.
- **§5.6 bad-case suite manual review**: PRIMARY ACCEPTANCE GATE at milestone close; human-judgment, NOT programmatic (2026-05-17 refinement).
- **§4.1 verdict set**: `pass` / `approve with downgrade-to-signal follow-up` / `reject as semantic hardcode` / `needs human architecture decision`.
- **§4.2 sprint-close header**: `## Sprint Review Decision\ndecision: pass | fix_required | out_of_scope_review\nblocking_count: <number>\nsummary: <paragraph>`.
- **§4.3 trigger #2**: per-sub-sprint Codex review fires when sub-sprint crosses a §1.7 forbidden-list red line (M3-Eval honored at S-Eval-3 — `critical_steps[].desc` LLM-visible).
- **A — Clean PASS classification**: dev shipped contract end-to-end; no §6 hard fence touched; no §10 stop signal fired; §4.1 Q1-Q8 pass + Q9 N/A; no contract drift beyond pre-existing baseline + planned §7-a drifts; OQs disposed non-blocking; Codex (if reviewed) returned `pass` first round.
- **A — Clean PASS with documentation-only fix-iteration** (NEW classification pattern at M3-Eval close 2026-05-23): deliver-agent fix-iteration on evidence-package consistency (NOT dev-code); fix is documentation-only; human accepts as Path 2 disposition without re-Codex round; the milestone closes with the original Codex verdict + the deliver-agent fix annotation archived together.

## 8. Key files / paths (reference for next deliver-agent instance)

**Governance** (auto-loaded via AGENTS.md): `AGENTS.md` / `docs/current/doc_governance.md` / `docs/current/agent_context_guide.md` / `docs/current/iteration_governance.md`.

**Active scope state** (post-M3-Eval-close; both placeholders pending M4+ selection):
- `docs/milestone_objective.md` (M4+ planning placeholder).
- `docs/sprint_objective.md` (sub-sprint planning placeholder).
- `docs/10-handoff.md` §1 lead (refreshed 2026-05-23 — current phase = no active milestone; preceding = M3-Eval; earlier = M2 + M1 + M2-Skill-superseded).
- `docs/codex-findings.md` (scaffold; reset 2026-05-23 per delete-and-add supersession; next milestone close writes here).

**M3-Eval archives**:
- `docs/milestones/M3-Eval_objective.md` (milestone objective + §12 closure verdict).
- `docs/milestones/M3-Eval_codex-review.md` (cumulative Codex milestone-shared review verdict).
- `docs/sprints/sprint-042-objective.md` + `-handoff.md` (S-Eval-1).
- `docs/sprints/sprint-043-objective.md` + `-handoff.md` (S-Eval-2).
- `docs/sprints/sprint-044-objective.md` + `-handoff.md` + `-codex-review.md` (S-Eval-3 with per-sub-sprint Codex).
- `docs/sprints/sprint-045-objective.md` + `-handoff.md` (S-Eval-4; no Codex archive — deferred).
- `docs/sprints/sprint-046-objective.md` + `-handoff.md` (S-Eval-5; no Codex archive — deferred and consumed by M3-Eval-shared).

**Backlog + history**:
- `docs/action_bank.md` (R-items §3-§5 + §6 close-action index + §6.5 closed-milestone index; M3-Eval row added 2026-05-23).
- `compact/M3-Eval-review-prompt.md` (Codex review prompt; preserved as historical artifact).
- `compact/context-handoff-sprint-046-pre-dev.md` (pre-dev handoff for S-Eval-5; preserved).
- `compact/context-handoff-sprint-045-post-close.md` (prior post-close handoff; preserved).
- `compact/M1-review-prompt.md` + `compact/M2-review-prompt.md` (precedent review prompts; preserved).

**Eval surface**:
- `eval_interactive/case_specs/bad_cases/` (curated bad-case suite; 12 cases; PRIMARY GATE per §5.6).
- `eval_interactive/case_specs/bad_cases/_manifest.md` (lifecycle ledger; M3 column populated 2026-05-23 + M3-Eval close section appended).
- `eval_interactive/case_specs/anchor_outcome/` (NEW S-Eval-1 ship; 12 outcome-only Tier-1 anchor cases).
- `eval_interactive/case_specs/smoke/` + `case_specs/anchor/` (smoke + anchor suites; OBSERVATION only per §5.5).
- `eval_interactive/case_specs_shadow/` (held-out shadow class per `_ACCESS_BOUNDARY.md`).
- `eval_interactive/results/` (per-run results archive; recent runs include 20260522-110537 bad-case main + 20260522-162847/162848/162850 bad-case isolated + 20260522-090056/085721/085614 §13 monotone-relaxing).

**Code surfaces touched by M3-Eval** (LOAD-BEARING for next deliver-agent reading):
- `eval_interactive/eval_interactive/case_spec/schema.py` + `loader.py` (S-Eval-1).
- `eval_interactive/eval_interactive/scoring/hard_checks.py` + `outcome_checks.py` + `composite.py` + `skill_procedure_check.py` + `llm_judge.py` (cumulative S-Eval-1 + S-Eval-2 + S-Eval-5).
- `eval_interactive/eval_interactive/batch/executor.py` (S-Eval-5 Option A wiring).
- `server/src/main/java/com/gumtree/csagent/service/runtime/skill/` (S-Eval-2 Java surface; UNCHANGED since S-Eval-2).
- `server/src/main/java/com/gumtree/csagent/service/runtime/ContextProjectionBuilder.java` (S-Eval-2 projection rendering).
- `server/src/main/resources/skills/` (6 Skill YAMLs; S-Eval-3 populated `critical_steps`; UNCHANGED since S-Eval-3).

**Source-of-truth artefacts**:
- `docs/solutions/m3_eval_milestone_proposal.md` (research-agent proposal; LOAD-BEARING for M3-Eval understanding).
- `docs/runtime_freeze_and_risk_policy.md` §1 + §2 (Tier-0 invariants; UNCHANGED through M3-Eval to date).
- `docs/current/customer_service_tool_spec_v0_3.md` (current tool spec).
- `docs/current/faq_grounding_contract.md` (grounding floor contract).
- `docs/release_gate.md` (release-gate framework; §1.1 Single Handover Orchestrator P0 blocker).

## 9. Deliver-agent memory caveat (carry-over)

This file references `~/.claude/agent-memory/sprint-deliver-orchestrator/` feedback files. The expected filesystem path does NOT exist; the actual memory dir at `/Users/caoruixin/.claude/projects/-Users-caoruixin-projects-csagent/memory/` may contain unrelated files. **The feedback file rules ARE authoritative via the governance docs + handoff files themselves** — load them from this handoff + AGENTS.md + iteration_governance.md transitive context. Do NOT block on memory-file loading. Surfaced as a fold-back candidate in M3-Eval §12.9 (path text correction at next pre-dev handoff authoring round).
