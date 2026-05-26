# Deliver-agent context handoff — Sprint 44 / S-Eval-3 close (A — Clean PASS) → Sprint 45 / S-Eval-4 launch

**Authored:** 2026-05-22 by deliver-agent (Sprint 44 S-Eval-3 close session, post-Codex-verdict, post-classification, post-S-Eval-4-launch-draft)
**For:** the next deliver-agent instance picking up at S-Eval-4 dev launch dispatch OR S-Eval-4 close classification
**Branch:** `refactor/remove-the-shackles`
**Repo:** `/Users/caoruixin/projects/csagent-latest`
**HEAD at this handoff authoring:** `01770ac` (Sprint 44 / S-Eval-3 dev commit). At this handoff authoring, the deliver-agent close-out bundle is STAGED in working tree (NOT yet committed by human). Human will bundle + commit; HEAD will advance.

Read order on cold start: this file → `AGENTS.md` (auto-loads `iteration_governance.md` + `doc_governance.md` + `agent_context_guide.md`) → `compact/sprint-deliver-orchestrator.md` (deliver-agent role definition — long-term doctrine; NOT duplicated here) → `docs/milestone_objective.md` (M3-Eval north star) → `docs/sprint_objective.md` (live S-Eval-4 contract; ~575 lines; 12 sections) → `docs/sprints/sprint-044-handoff.md` (S-Eval-3 dev archive + §12 closure verdict the deliver-agent + human + Codex jointly filled) → `docs/sprints/sprint-044-codex-review.md` (S-Eval-3 per-sub-sprint Codex review archive) → `compact/sprint-045-dev-prompt.md` (S-Eval-4 dev brief drafted but NOT committed at this handoff authoring).

---

## 1. 背景 (Background)

- Repo builds a customer-service agent for an online classifieds marketplace. LLM-first; Java/Python runtime owns deterministic boundaries (tool schema, capability, PII/safety floor, grounding floor, idempotency, persistence, trace/eval contract).
- Governance chain (auto-loaded via `@AGENTS.md`): `docs/current/doc_governance.md` + `docs/current/agent_context_guide.md` + `docs/current/iteration_governance.md` (Constitution §1 / Failure Brief §2 / Fix Layer §3 / Anti-Hardcode §4 / Eval Acceptance §5 incl. §5.5 smoke→observation + §5.6 / §5.6.1 / §5.6.2 / §5.6.3 bad-case suite as human-judgment gate / Architecture Health §6 / §7 stanza / **§8 Milestone framework**).
- Multi-agent collaboration: Human → Research-agent (proposes) → Deliver-agent (plans + drafts contracts + prompts) → Dev-agent Claude Code (implements) → Review-agent Codex (reviews); agents do NOT share chat history; cross-session continuity via repo docs + compact handoff files.
- Active milestone: **M3-Eval — Coarse-to-Fine Evaluation Architecture** (third milestone under §8 framework; M1 closed 2026-05-17, M2 closed 2026-05-18, M3-Eval IN PROGRESS as of 2026-05-22; 3 of 5 sub-sprints CLOSED Clean PASS).

## 2. 目标 (Goal)

### Milestone-level (M3-Eval, 5 sub-sprints; 3 of 5 CLOSED)

Replace implicit L1/L2/L3 equal-weighted eval scoring with a **four-tier pyramid**:

- **Tier-0** (unchanged hard gate): safety floor.
- **Tier-1** (NEW mandatory; coarsest): outcome — bad-case suite manual review + outcome-only `anchor_outcome` suite + supplementary L3 `user_goal_achievement`.
- **Tier-2** (NEW): per-UC critical-flow correctness via Skill `critical_steps[]` with LLM-visible `desc` + eval-side `trace_check` DSL.
- **Tier-3** (advisory only): existing L1/L2/L3 dims kept for trend tracking; NEVER gate `case_passed`.

**Single-source-of-truth property** (proposal §5.2): `critical_steps[].desc` consumed by BOTH runtime LLM AND eval-side scoring. Same YAML; two consumers. (Shipped at S-Eval-2 schema + S-Eval-3 content close 2026-05-22.)

### Current sub-sprint (Sprint 45 / S-Eval-4)

S-Eval-4 ships **10-12 new bad-case YAMLs** at `eval_interactive/case_specs/bad_cases/` sourced from the **17 approved entries** in `eval_interactive/case_spec_overrides.yaml` (NOT 29 per proposal §4 mistake; reconciled at S-Eval-1 launch 2026-05-20) + a **trial milestone-close manual review dry-run** with calibrated `closure_criterion` wording as the load-bearing output. Layer `eval_spec` (data; not code). **Codex review deferred to M3-Eval milestone-shared close** per §4.3 default (no per-sub-sprint trigger for S-Eval-4).

## 3. 已确认事实 (Confirmed facts)

### Recent commits (chronological)

```
01770ac  sprint 44 / S-Eval-3: populate critical_steps for the 6 Skills (NEW M3-Eval sub-sprint 3)  ← HEAD at this handoff authoring
db19a47  docs: S-Eval-2 close (A — Clean PASS) + S-Eval-3 launch + v0_2 supersession landing
69ed77f  sprint 43 / S-Eval-2: Skill critical_steps schema + extractor + projection wiring
357e949  docs: S-Eval-1 close (A — Clean PASS) + M3-Eval setup-bundle (deliver-agent)
d91bd3d  sprint 42 / S-Eval-1: schema simplification + anchor_outcome suite
6ceae7c  上传知识库和 mock data
```

### Working tree state at handoff authoring (HEAD `01770ac`; deliver-agent close-out + S-Eval-4 launch bundle STAGED)

```
M  docs/codex-findings.md                                  (RESET to scaffold per delete-and-add supersession; Codex per-sub-sprint review archived to sprints/sprint-044-codex-review.md)
M  docs/sprints/sprint-044-handoff.md                      (§12 closure verdict appended; rest unchanged)
M  docs/sprint_objective.md                                (REPLACED with S-Eval-4 contract; supersedes sprint-044-objective.md)
M  docs/10-handoff.md                                      (§1 lead refreshed for S-Eval-3 → S-Eval-4 transition)
M  docs/action_bank.md                                     (Sprint 44 close-action row appended to §6)
?? compact/context-handoff-sprint-044-pre-codex.md         (PRIOR cross-session handoff; safe to leave — historical)
?? compact/context-handoff-sprint-044-post-close.md        (THIS FILE; NEW; cross-session continuity for next deliver-agent)
?? compact/sprint-044-review-prompt.md                     (S-Eval-3 Codex review prompt; consumed; safe to leave or commit for history)
?? compact/sprint-045-dev-prompt.md                        (NEW; S-Eval-4 dev brief)
?? docs/sprints/sprint-044-codex-review.md                 (NEW; S-Eval-3 Codex per-sub-sprint review archive)
?? docs/sprints/sprint-044-objective.md                    (NEW; S-Eval-3 contract archive)
```

Suggested human commit message:
```
docs: S-Eval-3 close (A — Clean PASS) + S-Eval-4 launch + per-sub-sprint Codex pass archived
```

### Baselines at S-Eval-3 close (post-Codex independent verification 2026-05-22)

- **Java**: `Tests run: 1163, Failures: 1, Errors: 0, Skipped: 2` (UNCHANGED from S-Eval-2 close baseline; S-Eval-3 added 0 Java production source; test-class checkpoint slot reused via §7-a planned drift; inherited `SystemPromptUserRequestedTiebreakerTest:53` persists per M2-close STATUS QUO).
- **Python**: dev cited `392 passed / 9 pre-existing failed` via `uv run pytest`; **Codex reproduced `5 failed / 396 passed` via `uv run python -m pytest`** (Codex hypothesis: stale `.venv/bin/pytest` shebang exits 139 under Codex's local checkout). The 5 failure classes are a subset of the 9 dev cited; no S-Eval-3-attributable regression under either runner. **OQ-S44.6 (NEW deliver-agent-surfaced) routed to M3-Eval-shared Codex review queue** for runner-invocation confirmation. S-Eval-4 dev prompt carries explicit note: **MUST use `uv run python -m pytest`**.
- **§1.7 structural defence test**: 36 passed UNCHANGED since S-Eval-2 ship.
- **Skill loader sanity**: 6 Skills load + total `critical_steps = 18`; extractor parses all 18 `trace_check` cleanly.

### S-Eval-3 dev artefacts (commit `01770ac`; 9 files; +1080 / -14)

- 6 Skill YAMLs: `discover_triage` (3 steps) + `confirm` (2) + `resolve_faq_grounded_answer` (5) + `resolve_intake_collect_and_handover` (5) + `escalate` (2) + `terminal` (1) = **18 total**. APPEND-ONLY (each YAML's existing `procedure` / `guardrails` / `state_inheritance` / `applicable_use_cases` / `system_instruction` UNCHANGED, confirmed by Codex hard-fence verification).
- **Mandatory : Advisory split = 10 : 8** (Codex independently reproduced).
- 2 anchor-test updates (§7-a planned drift; S-Eval-2 checkpoint retirement): `SkillCriticalStepsLoadingTest.java` (+46/-5) + `test_skill_procedure_extractor.py` (+50/-9). Codex accepted as planned-not-drift via Contract Drift Independent Verification.
- Dev handoff: 657 lines; 12 sections; §12 closure verdict appended by deliver-agent + human + Codex at this close.

### Codex per-sub-sprint review verdict (per §4.3 trigger #2; archive at `docs/sprints/sprint-044-codex-review.md`)

```
decision: pass
blocking_count: 0
```

- §5.3 walk: **18× Row-1 ✅ / 0× Row-2 / 0× Row-3 / 0× Row-4** (Codex independent re-walk per-step table in Anti-Hardcode Kernel Q1; cites `tool-policy.yaml:2` + `IntakeFieldsRegistry.java:53` as canonical anchors used by `desc` strings — NOT user-message keywords).
- §4.1 Q1-Q9: Q1-Q8 pass with Q9 N/A.
- §1.7 boundary (a-e): all pass (esp. (b) `mandatory_for` confirmed as Tier-2 scoping list, NOT runtime branch).
- Hard fences: all empty-diff confirmed across 8 fence categories.
- Tier-0: no new candidate.
- Contract drift ×2 §7-a planned: both accepted.
- 1 non-blocking concern: Python baseline reproducibility (OQ-S44.6 NEW).
- 5 dev OQs + 1 NEW deliver-agent OQ disposed (none blocking; see §4 below).

### Per-UC anchor distribution (S-Eval-1 §12.7 carry-over — STILL LOAD-BEARING for S-Eval-4 / S-Eval-5)

UC-C 77 / UC-D 37 / UC-A 14 / UC-E 11 / UC-K 8 / UC-FP 6 / UC-B 5 / UC-F 1 / **UC-G UC-H UC-I UC-J all zero**. Intake-then-escalate UCs systematically under-represented across smoke + anchor + case-family. **S-Eval-4 bad-case selection SHOULD prioritise UC-G/H/I/J entries from the 17 approved overrides** per S-Eval-1 close §12.7 direction (confirmed at S-Eval-3 close §12.7 carry-over).

### S-Eval-3 §12.7 carry-overs to S-Eval-4 planning (LOAD-BEARING)

1. **Executor wiring (OQ-S44.1)** — natural S-Eval-4-or-S-Eval-5 blocker for Tier-2 becoming a true production eval gate. `eval_interactive/eval_interactive/batch/executor.py:252` does NOT pass `tier2_result=` to `compute_composite(...)`; populated `critical_steps` content structurally inert in production eval-harness path.
2. **Active Skill/UC resolution semantics (OQ-S44.2)** — bundled with OQ-S44.1.
3. **UC-FP moderation-context mandatory tightness (OQ-S44.3 + OQ-S44.4)** — S-Eval-4 trial real-LLM run is natural calibration surface; potential S-Eval-3 fix-iteration trigger if a legitimate UC-FP no-context path surfaces.
4. **Real-LLM run by M3-Eval close (OQ-S44.5)** — S-Eval-4 trial dry-run is planned calibration vehicle.
5. **Python baseline runner discipline (OQ-S44.6)** — `compact/sprint-045-dev-prompt.md` carries explicit note; dev MUST use `uv run python -m pytest`.

## 4. 决策记录 (Decision records)

### M3-Eval launch (2026-05-20)

- Path 1 research-driven; research-agent proposal at `docs/solutions/m3_eval_milestone_proposal.md`.
- 5 locked decisions per proposal §4: (1) Walk-A independent M3-Eval milestone with 5 sub-sprints; (2) M3-Corpus separate parallel-track milestone; (3) Bad-case suite 10-12 cases; (4) Corpus audit independent; (5) `critical_steps[].desc` LLM-visible (the §1.7 risk surface; per-sub-sprint Codex for S-Eval-3 — satisfied 2026-05-22).

### S-Eval-1 launch human-approved constraints (2026-05-20; STILL ACTIVE through M3-Eval close)

1. **Anchor count 159 vs 30** accepted (proposal drift reconciled).
2. **S-Eval-4 fallback paths NOT locked** at launch — 3 options preserved in milestone_objective §3 S-Eval-4 step 1 ((a) accept narrower coverage / (b) supplement non-override real sessions / (c) defer missing dimension); **decision deferred to S-Eval-4 planning** after S-Eval-1/S-Eval-3 evidence — this is THE upcoming S-Eval-4 planning decision.
3. **S-Eval-3 per-sub-sprint Codex HARD gate** — **SATISFIED 2026-05-22**; S-Eval-4 UNBLOCKED.
4. **`closure_criterion` CaseSpec-level preferred** (dev took this at S-Eval-1; same pattern carries to S-Eval-4 bad-case authoring).
5. **Smallest backward-compat strict-coupling** for `escalation_trigger` relaxation (S-Eval-1; preserved).

### S-Eval-1 + S-Eval-2 + S-Eval-3 closes (2026-05-21 + 2026-05-21 + 2026-05-22)

- S-Eval-1: A — Clean PASS; commit `d91bd3d`; close-out `357e949`. 4 OQs disposed non-blocking; OQ-S42.3 routed to M3-Eval-shared Codex review.
- S-Eval-2: A — Clean PASS; commit `69ed77f`; close-out `db19a47`. 5 OQs disposed non-blocking; OQ-S43.1 + OQ-S43.4 routed to M3-Eval-shared review; OQ-S43.2 + OQ-S43.3 + OQ-S43.5 deferred to S-Eval-3 author feedback (resolved at S-Eval-3 close §12.4 dispositions).
- **S-Eval-3: A — Clean PASS; commit `01770ac`; close-out pending human commit (this handoff bundle).** **Codex per-sub-sprint per §4.3 trigger #2 returned `decision: pass / blocking_count: 0` on first pass, single round.** 18× row-1 ✅ per Codex independent §5.3 walk; no hard fence touched; no contract drift beyond 2 §7-a planned drifts Codex accepted. **6 OQs disposed (5 dev + 1 NEW deliver-agent OQ-S44.6)**:
  - **OQ-S44.1** (executor wiring gap) **OOSR-CARRY** to S-Eval-4 / S-Eval-5 per `feedback_out_of_scope_review_packaging_rollforward.md`.
  - **OQ-S44.2** (active Skill/UC resolution) **bundled with OQ-S44.1**.
  - **OQ-S44.3** (UC-FP moderation-context mandatory tightness) **MONITORING POSTURE PRESERVED**; potential S-Eval-3 fix-iteration trigger if S-Eval-4 trial dry-run surfaces a legitimate no-context path.
  - **OQ-S44.4** (mandatory:advisory split 10:8) **ACCEPTED** per Codex; bundled with OQ-S44.3 as single calibration surface.
  - **OQ-S44.5** (synthetic vs real-LLM) **ACCEPTED for S-Eval-3**; real-LLM run carry-over to M3-Eval close (S-Eval-4 trial dry-run is planned vehicle).
  - **OQ-S44.6 (NEW deliver-agent-surfaced; Python baseline reproducibility lapse)** **ROUTED to M3-Eval-shared Codex review queue** for runner-invocation confirmation; non-blocking.
- **No new R-items at S-Eval-3 close**; the 6 OQ dispositions use OOSR-with-packaging-note pattern instead of fresh R-items.
- **Carry-over to S-Eval-4 planning (LOAD-BEARING)** per §3 above.

### M3-Eval-shared Codex review queue (4 items at S-Eval-3 close; +1 NEW)

To be addressed at M3-Eval milestone close (NOT S-Eval-4 close):
1. **OQ-S42.3** (S-Eval-1 D-2.2 demotion interpretation).
2. **OQ-S43.1** (S-Eval-2 DSL parser blocklist surface).
3. **OQ-S43.4** (S-Eval-2 `tool_event_seq` unsuccessful-dispatch `success=false` ignored).
4. **OQ-S44.6 (NEW)** (Python baseline reproducibility — runner-invocation confirmation; whether `uv run python -m pytest` vs `uv run pytest` reveals a latent test-collection issue OR is purely environment-dependent).

## 5. 当前任务 (Current task)

**Stage**: S-Eval-3 close-out + S-Eval-4 launch bundle STAGED in working tree at handoff authoring time; awaiting human commit + S-Eval-4 dev launch.

Deliver-agent close-out work at S-Eval-3 close — ALL COMPLETE:

- ✅ §12 closure verdict appended to `docs/sprints/sprint-044-handoff.md` (deliver-agent + human + Codex jointly per `feedback_handoff_verdict_section_delegation.md`).
- ✅ `docs/sprints/sprint-044-objective.md` archive created (S-Eval-3 contract immutable archive).
- ✅ `docs/sprints/sprint-044-codex-review.md` archive created (S-Eval-3 Codex per-sub-sprint review archive; delete-and-add supersession per `feedback_packaging_codex_findings_supersession.md`).
- ✅ Live `docs/codex-findings.md` RESET to scaffold (ready for next Codex review; M3-Eval milestone-shared at close).
- ✅ Live `docs/sprint_objective.md` REPLACED with full S-Eval-4 contract (~575 lines; 12 sections).
- ✅ `docs/10-handoff.md` §1 lead REFRESHED (S-Eval-3 → S-Eval-4 transition; A — Clean PASS summary).
- ✅ `docs/action_bank.md` §6 Sprint 44 close-action row APPENDED.
- ✅ NEW `compact/sprint-045-dev-prompt.md` drafted (S-Eval-4 dev brief; 8 sections).
- ✅ NEW `compact/context-handoff-sprint-044-post-close.md` (this file; cross-session continuity).

Pending (NOT deliver-agent work):

- ⏸ **Human commits the bundle** (suggested commit message above).
- ⏸ **Human dispatches Claude Code** dev session against `compact/sprint-045-dev-prompt.md` for S-Eval-4 implementation.
- ⏸ **Dev session** ships S-Eval-4 bundle (~13-15 files; 10-12 bad-case YAMLs + `_manifest.md` ledger append + calibration note + handoff).
- ⏸ **S-Eval-4 close cycle** (deliver-agent + human classify per §4.1 verdict set; Codex review deferred to M3-Eval milestone-shared close).

## 6. 下一步 (Next steps)

### Immediate (human cycle)

1. **Human commits the close-out + launch bundle** (single-commit or split; deliver-agent recommends single commit per `feedback_commit_at_end_bundles_deliver_artefacts.md`; suggested message: `docs: S-Eval-3 close (A — Clean PASS) + S-Eval-4 launch + per-sub-sprint Codex pass archived`).
2. **Human dispatches Claude Code** dev session against `compact/sprint-045-dev-prompt.md`.

### S-Eval-4 dev session (Claude Code; estimated 5-7 dev-days heavy in human-review collaboration time)

3. Dev consumes `compact/sprint-045-dev-prompt.md`; reads `docs/sprint_objective.md` (S-Eval-4 contract) + `docs/milestone_objective.md` + carry-over context.
4. Dev implements: select 10-12 from 17 approved entries (UC-G/H/I/J priority + D1-D4 coverage + UC-FP/G/H families); author bad-case YAMLs; append `_manifest.md`; run trial milestone-close manual review dry-run; append calibration note.
5. Dev decides Option A / B / C real-LLM run choice (per `docs/sprint_objective.md` §2.4); if Option C surfaces, dev STOPS and surfaces per §10 #1.
6. Dev writes handoff `docs/sprints/sprint-045-handoff.md` (LEAVES §12 EMPTY).
7. Dev commits one bundle (~13-15 files).

### S-Eval-4 close cycle (deliver-agent + human; Codex deferred to M3-Eval close)

8. Deliver-agent + human review dev handoff: A / A-with-X / B-fix-iterate per §4.1 verdict set. Codex NOT dispatched (deferred to M3-Eval close).
9. If A or A-with-X: deliver-agent drafts S-Eval-5 contract (L3 judge repositioning + R-item closure; ~2-3 dev-days; Codex milestone-shared). Close bundle for S-Eval-4: handoff §12 closure verdict + sprint-045-objective.md archive + 10-handoff §1 lead refresh + action_bank §6 Sprint 45 row + NEW `compact/sprint-046-dev-prompt.md` + NEW `compact/context-handoff-sprint-045-post-close.md`. Codex-findings stays scaffold (no per-sub-sprint review).
10. If B (fix-iterate): scope fix-iteration sub-sprint; do NOT silently fix-and-continue.

### M3-Eval milestone close (after S-Eval-5 close)

11. Milestone-shared Codex review per §4.3 default at `compact/M3-Eval-review-prompt.md` (deliver-agent drafts at M3-Eval close; cumulative commit range covering S-Eval-1 through S-Eval-5; addresses the 4 M3-Eval-shared review queue items including OQ-S44.6).
12. Bad-case suite manual review as PRIMARY GATE per `iteration_governance.md` §5.6 (deliver-agent + human manually review the cumulative bad-case suite: Alice + 10-12 S-Eval-4 + any closed-as-regression-guard cases).
13. M3-Eval close verdict (A / A-with-X / B); archive to `docs/milestones/M3-Eval_objective.md` + `docs/milestones/M3-Eval_codex-review.md`.

## 7. 注意事项 (Caveats / red lines)

### S-Eval-4 specifics

- **Source pool is 17 approved entries** in `case_spec_overrides.yaml`, NOT 29 as proposal §4 stated. Deliver-agent reconciled at S-Eval-1 launch 2026-05-20; preserved through S-Eval-3 close.
- **UC-G / UC-H / UC-I / UC-J priority** in selection — over-represent vs source pool distribution per S-Eval-1 §12.7 carry-over.
- **D1-D4 × UC-FP/G/H dimension coverage** must be verified at planning round; if undersampled within 17, dev STOPS per §10 #3 and surfaces. 3 fallback paths from S-Eval-1 launch constraint #2 still NOT LOCKED: (a) accept narrower coverage; (b) supplement with non-override REAL sessions; (c) defer to follow-on milestone with R-item.
- **Real-LLM run option choice (A / B / C)** is a planning-round decision (per `docs/sprint_objective.md` §2.4). Option C (executor wiring fix at `executor.py:252`) expands scope and requires explicit deliver-agent + human authorization. Deliver-agent's read (NOT pre-decision): Option A or Option B-without-wiring is the natural default.
- **Bad cases MUST be real-session-derived.** Synthetic / hand-crafted / keyword-derived bad cases are §1.7 red-line violations on the bad-case suite (NEW S-Eval-4 §10 #4 STOP signal).
- **`closure_criterion` wording is the calibration deliverable**, NOT verified bot correctness. Trial dry-run finding well-calibrated vs poorly-calibrated wording IS the load-bearing output. If >50% poorly-calibrated, dev STOPS per §10 #5.

### Process discipline (carry-over from S-Eval-3 + earlier)

- **Numbers-cite per `feedback_deliver_agent_cited_numbers_must_be_reproducible.md`**: every count in S-Eval-4 handoff + Sprint 45 action_bank row MUST be derived from `git show --numstat 01770ac` (or post-S-Eval-4 commit) / `grep -c` / test-run direct output. **Python baseline MUST use `uv run python -m pytest`** per OQ-S44.6 carry-over.
- **Bundle policy per `feedback_commit_at_end_bundles_deliver_artefacts.md`**: dev does NOT stage deliver-agent files. Deliver-agent close-out files bundled separately by human at close commit.
- **Handoff §12 closure verdict ownership per `feedback_handoff_verdict_section_delegation.md`**: dev leaves §12 EMPTY; deliver-agent + human (no Codex at S-Eval-4 since deferred) jointly fill at close.
- **OOSR-with-packaging-note pattern per `feedback_out_of_scope_review_packaging_rollforward.md`**: if Codex returns a finding that's out-of-S-Eval-4-scope at M3-Eval close, classify as OOSR + roll forward.
- **No pre-decisions on OQs surfaced to Codex** per `feedback_constitution_discipline_vs_planning_anticipation.md` — at M3-Eval close Codex prompt, surface the 4-item M3-Eval-shared queue WITHOUT pre-decisions.

### Hard fences inherited from prior sub-sprints (still ACTIVE for S-Eval-4)

- **No new Tier-0 invariant** without human-review escalation (milestone §6 #2). C2 + C3 candidates (M2 close) STAY DEFERRED until production trace observation at M3+.
- **No edits to runtime semantic surfaces** (RuntimeIntentClassifier / IntentClassification / DriftResult / DriftDetector / UseCaseRouter / ClassifyUseCaseTool / PhaseEvaluator / AgentRunLoopImpl / ControlKernel / system_prompt.txt / tool-policy.yaml / IntakeFieldsRegistry / UseCaseRegistryService / ResolveDispositionEvaluator).
- **No edits to Skill abstraction** (M2 + S-Eval-2 + S-Eval-3 Java; 6 Skill YAMLs; ContextProjectionBuilder.java).
- **No edits to Tier-0 / Tier-1 / Tier-2 / Tier-3 scoring code** (S-Eval-1 / S-Eval-2 / S-Eval-3 / S-Eval-5 territory).
- **No edits to CaseSpec schema + loader** (S-Eval-1 territory).
- **No edits to `case_spec_overrides.yaml`** (read-only source pool for S-Eval-4).
- **No edits to existing case fixtures** (smoke / anchor / anchor_outcome / case-families / Alice / shadow).
- **No edits to eval harness / loader / simulator** (default; Option C `executor.py:252` is the ONLY authorised exception, requires explicit approval).
- **No `iteration_governance.md` edit** during S-Eval-4 (only S-Eval-1 §5.5 sentence + potential S-Eval-5 acceptance-bar refinement are allowed).
- **No widening of `escalation_reason` enum** (canonical 23 values; runtime contract).

### Carry-over queues (UNCHANGED at S-Eval-3 close)

- **M3-Eval-shared Codex review queue (4 items)** to address at M3-Eval close: OQ-S42.3, OQ-S43.1, OQ-S43.4, **NEW OQ-S44.6** (Python baseline reproducibility).
- **M2 close fold-back queue (6 items)** STILL DEFERRED — separate governance commit per `doc_governance.md` cadence (Sprint 37 §7.8 typo + Sprint 38 OQ-S38.1 + Sprint 39 §6.2.5/§6.2.6 + Sprint 39 OQ-S39.7 + Sprint 41 OQ-S41.1 + Sprint 41 OQ-S41.4). Deliver-agent + human discretion on timing; can interleave anytime between sub-sprints.
- **Stale-test R-item** `R-stale-test-escalation-enum-sync-v0_2-to-v0_3-migration` (S-Eval-1 close; status `proposed; impl deferred to whichever milestone consumes it`). 3 paths verbatim: (1) add v0_3.yaml companion + repoint; (2) extract enum from v0_3.md YAML codefence; (3) relocate canonical enum to Python schema with back-link comment. 2 failing tests in `tests/scoring/test_escalation_enum_sync.py` persist as known baseline state until R-item consumed.

### Term definitions (key for the new deliver-agent)

- **§5.3 standard table** (proposal §5.3): 4-row table for evaluating `desc` strings (S-Eval-3 close used this; Codex 18× row-1 ✅).
- **6 frozen DSL primitives** (S-Eval-2 ship): `accumulated_tool_results.<tool>` / `tool_event_seq(...)` / `intake_state.fields_collected.contains(...)` / `session.<flag>_present` / `any_of(...)` / `all_of(...)`. Parser rejects anything else at parse time with `TraceCheckDSLSyntaxError`.
- **`bad_case_metadata`** (per `iteration_governance.md` §5.6 + Alice precedent): `source_session_id` / `surfaced_by` / `surfaced_date` / `failure_shape` / `layers_involved` / `related_dimensions` / `related_r_items` / `tier`. Each new S-Eval-4 bad-case YAML carries this block.
- **`closure_criterion`** (per `iteration_governance.md` §5.6 2026-05-17 refinement): human-verified observable end-state(s) that count as resolved; multi-line OK; GUIDANCE for the human + deliver-agent manual review judgment, NOT a programmatic PASS/FAIL match.
- **`tier: core` vs `scope-relevant`** (per §5.6.1): `core` for cross-cutting / release-gate-relevant failure modes (re-run at every milestone close); `scope-relevant` for surface-specific failures (re-run only when milestone touches the relevant surface).
- **§4.1 verdict set**: `pass` / `approve with downgrade-to-signal follow-up` / `reject as semantic hardcode` / `needs human architecture decision`.
- **A — Clean PASS** classification: dev shipped contract end-to-end; no §6 hard fence touched; no §10 stop signal fired; §4.1 Q1-Q8 pass + Q9 N/A; no contract drift beyond pre-existing baseline; OQs disposed non-blocking; Codex (if reviewed) returned `pass` first round.

### Key files / paths the new deliver-agent must know

#### Governance + planning (auto-loaded via AGENTS.md)

- `AGENTS.md` / `docs/current/doc_governance.md` / `docs/current/agent_context_guide.md` / `docs/current/iteration_governance.md`

#### Active sub-sprint state

- `docs/milestone_objective.md` (M3-Eval north star; §3 S-Eval-4 row LOAD-BEARING; §5 milestone acceptance bar; §6 hard fences; §8 Codex review plan)
- `docs/sprint_objective.md` (live S-Eval-4 contract; 12 sections; will archive at close)
- `docs/10-handoff.md` §1 lead (refreshed at this close; will refresh again at S-Eval-4 close)
- `docs/codex-findings.md` (scaffold; M3-Eval milestone-shared Codex at close writes here)
- `docs/action_bank.md` (R-items + §6 close-action index; Sprint 44 row appended at this close)
- `compact/sprint-045-dev-prompt.md` (NEW S-Eval-4 dev brief; not yet committed at handoff authoring)
- `compact/context-handoff-sprint-044-post-close.md` (THIS FILE; cross-session continuity)

#### Archives (immutable)

- `docs/sprints/sprint-042-objective.md` + `sprint-042-handoff.md` (S-Eval-1)
- `docs/sprints/sprint-043-objective.md` + `sprint-043-handoff.md` (S-Eval-2)
- `docs/sprints/sprint-044-objective.md` + `sprint-044-handoff.md` + **`sprint-044-codex-review.md`** (S-Eval-3 with per-sub-sprint Codex archive)
- `docs/milestones/M1_objective.md` + `M2_objective.md` + `M2-Skill_objective.md` (superseded) + `M2_codex-review.md`

#### Bad-case suite (LOAD-BEARING for S-Eval-4)

- `eval_interactive/case_specs/bad_cases/alice_uc_a_uc_h_misclass.yaml` (Alice precedent; cascade fence — do NOT modify in S-Eval-4)
- `eval_interactive/case_specs/bad_cases/_manifest.md` (lifecycle ledger; S-Eval-4 appends rows + calibration note section)
- `eval_interactive/case_spec_overrides.yaml` (17 approved entries source pool; read-only)

#### Code surfaces touched by M3-Eval (S-Eval-1 + S-Eval-2 + S-Eval-3; UNCHANGED in S-Eval-4)

- `eval_interactive/eval_interactive/case_spec/schema.py` (S-Eval-1)
- `eval_interactive/eval_interactive/case_spec/loader.py` (S-Eval-1)
- `eval_interactive/eval_interactive/scoring/hard_checks.py` (S-Eval-1)
- `eval_interactive/eval_interactive/scoring/outcome_checks.py` (S-Eval-1)
- `eval_interactive/eval_interactive/scoring/composite.py` (S-Eval-1 + S-Eval-2)
- `eval_interactive/eval_interactive/scoring/skill_procedure_check.py` (S-Eval-2)
- `eval_interactive/case_specs/anchor_outcome/` (S-Eval-1; 12 cases + manifest)
- `eval_interactive/tests/test_s_eval_1_schema_and_scoring.py` (S-Eval-1)
- `eval_interactive/tests/test_skill_procedure_extractor.py` (S-Eval-2; updated S-Eval-3 anchor)
- `eval_interactive/tests/test_skill_procedure_dsl_parser_rejects_hardcodes.py` (S-Eval-2; §1.7 structural defence; 36 tests)
- `eval_interactive/tests/test_composite_gate.py` (S-Eval-1 + S-Eval-2 edits)
- `server/src/main/java/com/gumtree/csagent/service/runtime/skill/Skill.java` (S-Eval-2)
- `server/src/main/java/com/gumtree/csagent/service/runtime/skill/CriticalStep.java` (S-Eval-2)
- `server/src/main/java/com/gumtree/csagent/service/runtime/skill/SkillLoader.java` (S-Eval-2)
- `server/src/main/java/com/gumtree/csagent/service/runtime/ContextProjectionBuilder.java` (S-Eval-2)
- `server/src/main/resources/skills/<6 YAMLs>` (S-Eval-3 content)
- `server/src/test/java/com/gumtree/csagent/service/runtime/skill/SkillCriticalStepsLoadingTest.java` (S-Eval-2; updated S-Eval-3 anchor)
- `server/src/test/java/com/gumtree/csagent/service/runtime/CriticalStepsProjectionTest.java` (S-Eval-2)

#### Source-of-truth artefacts

- `docs/solutions/m3_eval_milestone_proposal.md` (research-agent proposal; LOAD-BEARING §4 decision 3 + §5 + §5.3 + §5.4 + §6 S-Eval-4)
- `docs/runtime_freeze_and_risk_policy.md` §1 + §2 (Tier-0 invariants; UNCHANGED through S-Eval-3)
- `docs/current/customer_service_tool_spec_v0_3.md` (current tool spec; v0_2.{md,yaml} deleted in `db19a47`)
- `docs/current/faq_grounding_contract.md`

### Deliver-agent memory (referenced but file system location differs — see note)

The handoff files reference `~/.claude/agent-memory/sprint-deliver-orchestrator/` deliver-agent memory files (`feedback_commit_at_end_bundles_deliver_artefacts.md`, etc.). At this handoff authoring, the actual filesystem path `/Users/caoruixin/.claude/agent-memory/sprint-deliver-orchestrator/` does NOT exist; the actual memory directory at `/Users/caoruixin/.claude/projects/-Users-caoruixin-projects-csagent/memory/` has 5 different memory files (cs_agent_posture, doc_governance, llm_fail_fast, casespec_override_pipeline, async_chat_direction). The referenced deliver-agent feedback file rules ARE captured in the governance docs + handoff files themselves; the rules are still authoritative. **Action for next deliver-agent**: do NOT rely on memory-file loading; load the rules from this handoff + AGENTS.md + iteration_governance.md transitive context.

Most relevant rules for S-Eval-4 close + S-Eval-5 launch (rule names referenced throughout governance docs):
- `feedback_commit_at_end_bundles_deliver_artefacts.md` — dev does NOT stage deliver-agent files
- `feedback_handoff_verdict_section_delegation.md` — dev leaves handoff §12 EMPTY; deliver-agent + human (+ Codex if applicable) fill at close
- `feedback_out_of_scope_review_packaging_rollforward.md` — OOSR-with-packaging-note pattern (used at S-Eval-3 close for OQ-S44.1)
- `feedback_deliver_agent_cited_numbers_must_be_reproducible.md` — every numstat reproducible (NEW S-Eval-4 specific: Python baseline MUST use `uv run python -m pytest` per OQ-S44.6 carry-over)
- `feedback_constitution_discipline_vs_planning_anticipation.md` — surface OQs to Codex WITHOUT pre-decisions (applies at M3-Eval close)
- `feedback_packaging_codex_findings_supersession.md` — delete-and-add pattern for codex-findings at archive (used at S-Eval-3 close: sprint-044-codex-review.md archive + codex-findings.md scaffold reset)
- `feedback_corpus_undecidable_premise_check.md` — in-flight downgrade pattern (applies if S-Eval-4 trial dry-run falsifies milestone hypothesis on bad-case suite viability)
- `feedback_mocked_llm_cannot_prove_prompt_causal_change.md` — real-LLM required for prompt-causal evidence (applies to S-Eval-4 Option B/C real-LLM run choice)
