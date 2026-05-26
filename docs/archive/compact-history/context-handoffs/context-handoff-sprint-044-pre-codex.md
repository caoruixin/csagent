# Deliver-agent context handoff — Sprint 44 / S-Eval-3 post-dev / pre-Codex-dispatch

**Authored:** 2026-05-22 by deliver-agent (Sprint 44 close session, pre-Codex-dispatch)
**For:** the next deliver-agent instance picking up at S-Eval-3 Codex dispatch OR post-Codex-verdict classification
**Branch:** `refactor/remove-the-shackles`
**Repo:** `/Users/caoruixin/projects/csagent-latest`
**HEAD:** `01770ac` (Sprint 44 / S-Eval-3 dev commit; uncommitted: `?? compact/sprint-044-review-prompt.md`)

Read order on cold start: this file → `AGENTS.md` (auto-loads `iteration_governance.md` + `doc_governance.md` + `agent_context_guide.md`) → `compact/sprint-deliver-orchestrator.md` (deliver-agent role definition — long-term doctrine; NOT duplicated here) → `docs/milestone_objective.md` (M3-Eval north star) → `docs/sprint_objective.md` (live S-Eval-3 contract) → `docs/sprints/sprint-044-handoff.md` (dev archive) → `compact/sprint-044-review-prompt.md` (Codex brief drafted but not committed) → `docs/codex-findings.md` (scaffold; Codex will write here).

---

## 1. 背景 (Background)

- Repo builds a customer-service agent for an online classifieds marketplace. LLM-first; Java/Python runtime owns deterministic boundaries (tool schema, capability, PII/safety floor, grounding floor, idempotency, persistence, trace/eval contract).
- Governance chain (auto-loaded via `@AGENTS.md`): `docs/current/doc_governance.md` + `docs/current/agent_context_guide.md` + `docs/current/iteration_governance.md` (Constitution §1 / Failure Brief §2 / Fix Layer §3 / Anti-Hardcode §4 / Eval Acceptance §5 / Architecture Health §6 / §7 stanza / **§8 Milestone framework**).
- Multi-agent collaboration: Human → Research-agent (proposes) → Deliver-agent (plans + drafts contracts + prompts) → Dev-agent Claude Code (implements) → Review-agent Codex (reviews); agents do NOT share chat history; cross-session continuity via repo docs + compact handoff files.
- Active milestone: **M3-Eval — Coarse-to-Fine Evaluation Architecture** (third milestone under §8 framework; M1 closed 2026-05-17, M2 closed 2026-05-18, M3-Eval IN PROGRESS as of 2026-05-22).

## 2. 目标 (Goal)

### Milestone-level (M3-Eval, 5 sub-sprints)

Replace implicit L1/L2/L3 equal-weighted eval scoring with a **four-tier pyramid**:

- **Tier-0** (unchanged hard gate): safety floor (`no_pii_leakage`, `no_human_only_tool_exposure`, `no_critical_policy_violation`, `escalation_compliance`, `phase_transition_validity`).
- **Tier-1** (NEW mandatory; coarsest): outcome — bad-case suite manual review + outcome-only `anchor_outcome` suite + supplementary L3 `user_goal_achievement`.
- **Tier-2** (NEW): per-UC critical-flow correctness via Skill `critical_steps[]` with LLM-visible `desc` + eval-side `trace_check` DSL.
- **Tier-3** (advisory only): existing `tool_sequence_match` / `relevance` / `tone_appropriateness` / `groundedness` / latency / token cost — NEVER gate `case_passed`.

**Single-source-of-truth property** (proposal §5.2): `critical_steps[].desc` is consumed by BOTH runtime LLM (via `ContextProjectionBuilder` rendering inside `phase_plan.skill`) AND eval-side scoring (via `SkillProcedureExtractor` evaluating `trace_check` against trace). Same YAML; two consumers.

### Current sub-sprint (Sprint 44 / S-Eval-3)

S-Eval-3 populates `critical_steps` content (18-30 across 6 Skills) on top of S-Eval-2's schema + structural defence. **This IS the milestone's critical governance gate** per `iteration_governance.md` §4.3 trigger #2 (LLM-visible content + §1.7 forbidden-list adjacent). **Codex per-sub-sprint review REQUIRED at close BEFORE S-Eval-4 begins.**

## 3. 已确认事实 (Confirmed facts)

### Recent commits (chronological)

```
01770ac  sprint 44 / S-Eval-3: populate critical_steps for the 6 Skills (NEW M3-Eval sub-sprint 3)  ← HEAD
db19a47  docs: S-Eval-2 close (A — Clean PASS) + S-Eval-3 launch + v0_2 supersession landing
69ed77f  sprint 43 / S-Eval-2: Skill critical_steps schema + extractor + projection wiring
357e949  docs: S-Eval-1 close (A — Clean PASS) + M3-Eval setup-bundle (deliver-agent)
d91bd3d  sprint 42 / S-Eval-1: schema simplification + anchor_outcome suite
6ceae7c  上传知识库和 mock data
```

### Working tree state at HEAD `01770ac`

```
?? compact/sprint-044-review-prompt.md   (NEW; 150-line Codex brief; not yet committed)
```

Otherwise clean. Pre-existing `customer_service_tool_spec_v0_2.{md,yaml}` deletions LANDED in `db19a47`.

### Baselines at S-Eval-3 close (per dev handoff §9)

- **Java**: `Tests run: 1163, Failures: 1, Errors: 0, Skipped: 2` (exactly matches S-Eval-2 close baseline; inherited `SystemPromptUserRequestedTiebreakerTest:53` persists per M2-close STATUS QUO).
- **Python**: 392 passed / 9 pre-existing failed (exactly matches S-Eval-2 close baseline; 9 failures are 2 `test_escalation_enum_sync.py` v0_2 references + 5 `test_corpus_lint.py` PYTHONPATH leak + 2 `test_case_spec_overrides.py` 17-vs-15 override drift).
- **§1.7 structural defence test**: 36 passed (15 hardcode-flavoured rejections; 11 positive-grammar; 10 malformed-syntax). UNCHANGED since S-Eval-2 ship.

### S-Eval-3 dev artefacts (`01770ac`; 9 files; +1080 / -14)

- 6 Skill YAMLs: `discover_triage` (3 steps) + `confirm` (2) + `resolve_faq_grounded_answer` (5) + `resolve_intake_collect_and_handover` (5) + `escalate` (2) + `terminal` (1) = **18 total**. APPEND-ONLY (each YAML's existing `procedure` / `guardrails` / `state_inheritance` / `applicable_use_cases` / `system_instruction` UNCHANGED).
- **Mandatory : Advisory split = 10 : 8.**
- 2 anchor-test updates (S-Eval-2 checkpoint retirement per §7-a planned drift): `SkillCriticalStepsLoadingTest.java` (+46/-5; test renamed `_atSEval2_close` → `_atSEval3_close` + reshaped assertions) + `test_skill_procedure_extractor.py` (+50/-9; same shape).
- Dev handoff (657 lines; 12 sections per convention).

### Numbers-cite reconciliation against HEAD (S-Eval-1 launch, still valid)

- Existing anchor case count: **159** (NOT proposal's "30").
- `case_spec_overrides.yaml` approved entries: **17** (NOT proposal's "29"). S-Eval-4 source-pool sized to 17; 10-12 selection target unchanged.
- M2 archive at `docs/milestones/M2_objective.md` was complete at M2 close 2026-05-18 (no re-archive needed).

### Per-UC anchor distribution (S-Eval-1 §12.7 carry-over — LOAD-BEARING for S-Eval-3 / S-Eval-4)

UC-C 77 / UC-D 37 / UC-A 14 / UC-E 11 / UC-K 8 / UC-FP 6 / UC-B 5 / UC-F 1 / **UC-G UC-H UC-I UC-J all zero**. Intake-then-escalate UCs systematically under-represented across smoke + anchor + case-family.

### Tier-2 deterministic synthetic verification (dev §9 — 13 scenarios)

7 FAIL surfaces all map to expected failure modes; 6 PASS surfaces all map to canonical healthy shapes. **Alice anchor**: `uc-h-intake-complete-before-handover` Tier-2 mandatory FAIL on the canonical Alice trace shape (UC-H, intake_fields_collected=[], `request_handover` dispatched). ✓ Per contract §10 #5 expected outcome.

### Token-cost observation (dev §9)

Per-active-Skill turn delta: best 92 / worst 730 / mean ~378 tokens. Well within proposal §5.4 estimate (500-900) and milestone §5 acceptance bar (≤ 1000).

## 4. 决策记录 (Decision records)

### M3-Eval launch (2026-05-20)

- Path 1 research-driven; research-agent proposal at `docs/solutions/m3_eval_milestone_proposal.md`.
- 5 locked decisions per proposal §4: (1) Walk-A independent M3-Eval milestone with 5 sub-sprints; (2) M3-Corpus separate parallel-track milestone; (3) Bad-case suite 10-12 cases; (4) Corpus audit independent; (5) **`critical_steps[].desc` LLM-visible** (the §1.7 risk surface; per-sub-sprint Codex for S-Eval-3).

### S-Eval-1 launch human-approved constraints (2026-05-20; STILL ACTIVE through M3-Eval close)

1. **Anchor count 159 vs 30** accepted (proposal drift reconciled).
2. **S-Eval-4 fallback paths NOT locked** at launch — 3 options preserved in milestone_objective §3 S-Eval-4 step 1 ((a) accept narrower coverage / (b) supplement non-override real sessions / (c) defer missing dimension); decision deferred to S-Eval-4 planning after S-Eval-1/S-Eval-3 evidence.
3. **S-Eval-3 per-sub-sprint Codex HARD gate** — S-Eval-4 blocked until pass.
4. **`closure_criterion` CaseSpec-level preferred** (dev took this at S-Eval-1).
5. **Smallest backward-compat strict-coupling** for `escalation_trigger` relaxation (dev took minimum-edit path — replaced `raise ValueError` with `logging.warning`).

### S-Eval-1 close (2026-05-21)

- Classification **A — Clean PASS**; commit `d91bd3d`; close-out `357e949`.
- All 4 outcomes + Outcome 5 regression tests + Outcome 6 observation deliverable shipped.
- 4 OQs disposed non-blocking; OQ-S42.3 (D-2.2 demotion interpretation) **routed to M3-Eval-shared Codex review**.
- NEW R-item opened: `R-stale-test-escalation-enum-sync-v0_2-to-v0_3-migration` (3 paths; impl deferred to whichever milestone consumes it).
- §12.7 carry-over: per-UC anchor distribution observation surfaced UC-G/H/I/J zero coverage.

### S-Eval-2 launch + close (2026-05-21)

- Launch: deliver-agent drafted S-Eval-2 contract + dev prompt + R-item added to action_bank. Human did NOT commit launch bundle; dev launched against unstaged files; dev commit `69ed77f` shipped only dev substance.
- Close: classification **A — Clean PASS**; close-out `db19a47`.
- Schema + extractor + projection wiring shipped: NEW `CriticalStep.java` Java record + `Skill.criticalSteps` field via backward-compat secondary ctor (zero existing test ctor sites updated per Sprint 41 OQ-S41.3 precedent) + `SkillLoader` allowlist validation + `ContextProjectionBuilder` LLM-visible `phase_plan.critical_steps` slot as `[{id, desc}]` list-of-objects (empty-array parity preserved; 0-byte projection delta on 6 production Skills) + NEW Python `skill_procedure_check.py` with hand-rolled recursive-descent DSL parser (6 frozen primitives) + Tier-2 wiring in `composite.py`.
- **§1.7 structural defence over-delivered**: 15 hardcode-flavoured `trace_check` strings rejected (2.5× the contract §9 ≥6 floor).
- **5 S-Eval-2 OQ dispositions** (still relevant):
  - **OQ-S43.1** (DSL parser blocklist surface — positive grammar + illustrative blocklist vs uniform "unknown primitive") → **routed to M3-Eval-shared Codex review**.
  - **OQ-S43.2** (projection `id` inclusion) → deferred to S-Eval-3 author feedback. **Dev observed at S-Eval-3 close: NO `desc` cross-references another step's `id` by name**; M3-Eval-close calibration candidate (whether to drop the `id` field as follow-on simplification).
  - **OQ-S43.3** (Python Skill loader graduation) → defer until second consumer surfaces.
  - **OQ-S43.4** (`tool_event_seq` unsuccessful-dispatch `success=false` ignored) → **routed to M3-Eval-shared Codex review**.
  - **OQ-S43.5** (`session.<flag>_present` double-fallback permissiveness) → deferred to S-Eval-3 fix-iteration if too permissive. **Dev observed at S-Eval-3 close: ZERO use of `session.<flag>_present` primitive**; OQ-S43.5 monitoring posture preserved, NOT a fix-iteration candidate.

### S-Eval-3 dev close (2026-05-21; commit `01770ac`)

- 18 `critical_steps` populated across 6 Skills (3/2/5/5/2/1).
- Mandatory:advisory = 10:8.
- Dev's §5.3 self-walk: 18/18 row-1 ✅; zero row-2/3/4. **Deliver-agent has NOT independently verified**; Codex does at per-sub-sprint review.
- Anti-hardcode strategy dev took: anchor `desc` strings on canonical tool names (from `tool-policy.yaml`) + canonical intake-field names (from `IntakeFieldsRegistry.REQUIRED_FIELDS_BY_UC`) — NOT user-message keywords.
- 2 §7-a planned drifts on anchor tests (S-Eval-2 checkpoint retirement); deliver-agent + Codex should accept as planned-not-drift.

### Codex review prompt drafted (2026-05-22)

- File: `compact/sprint-044-review-prompt.md` (150 lines; NOT yet committed).
- Convention: §4.1 9-question kernel + §5.3 standard table verbatim for re-walk + §1.7 boundary check + hard-fence verification + schema/reproducibility checks + validation runs + 5 OQs surfaced **WITHOUT pre-decisions** per `feedback_constitution_discipline_vs_planning_anticipation.md` established pattern.

## 5. 当前任务 (Current task)

**Stage**: S-Eval-3 post-dev / pre-Codex-dispatch. Deliver-agent close-out work is partial:

- ✅ Codex per-sub-sprint review prompt drafted at `compact/sprint-044-review-prompt.md`.
- ⏸ NOT yet committed (single untracked file).
- ⏸ Codex NOT yet dispatched.
- ⏸ `docs/codex-findings.md` is still scaffold (Codex will write).
- ⏸ S-Eval-3 §12 closure verdict NOT yet appended to `docs/sprints/sprint-044-handoff.md` (deliver-agent + human + Codex jointly write AFTER Codex verdict returns).
- ⏸ `docs/sprints/sprint-044-objective.md` archive NOT yet created.
- ⏸ `docs/10-handoff.md` §1 lead NOT yet refreshed for post-S-Eval-3 state.
- ⏸ `docs/action_bank.md` Sprint 44 close-action row NOT yet appended.
- ⏸ `docs/sprint_objective.md` carries the live S-Eval-3 contract (will be archived + replaced with S-Eval-4 or fix-iteration after Codex verdict).

## 6. 下一步 (Next steps)

Immediate (deliver-agent + human cycle):

1. **Human commits `compact/sprint-044-review-prompt.md`** (single-file commit; suggested message: `docs: S-Eval-3 Codex per-sub-sprint review prompt (compact/sprint-044-review-prompt.md)`).
2. **Human dispatches Codex** against the prompt. Codex consumes HEAD `01770ac` + the contract + the handoff + the proposal §5.3 + governance docs; walks Q1-Q9 + §5.3 standard table + §1.7 + hard fences + reproducibility + validation runs + 5 OQ independent verdicts.
3. **Codex writes verdict** to `docs/codex-findings.md` (currently scaffold) with §4.2 sprint-close header at top.
4. **Deliver-agent + human classify Codex verdict** per §4.1 verdict set:
   - **`pass`** → close S-Eval-3 (A — Clean PASS). Bundle: append §12 to handoff; archive sprint_objective to `docs/sprints/sprint-044-objective.md`; replace live `sprint_objective.md` with S-Eval-4 contract (or placeholder); archive `codex-findings.md` to `docs/sprints/sprint-044-codex-review.md` + reset codex-findings to scaffold; refresh 10-handoff §1; append Sprint 44 close-action row to action_bank §6; **S-Eval-4 UNBLOCKS**.
   - **`approve with downgrade-to-signal follow-up`** → close with conversion R-item (likely OQ-S44.1 executor wiring named as the trigger). Similar bundle to `pass`; **S-Eval-4 UNBLOCKS** with the named trigger tracked.
   - **`reject as semantic hardcode`** → fix-iteration sub-sprint scoped to specific rejected `desc`/`trace_check` strings. **S-Eval-4 stays BLOCKED**. Deliver-agent + human + dev coordinate the fix-iteration; new sub-sprint contract drafted.
   - **`needs human architecture decision`** → halt; deliver-agent + human escalate (likely on OQ-S44.1 executor wiring scope).

After Codex verdict + classification:

5. If `pass` or `approve with downgrade-to-signal`: deliver-agent drafts S-Eval-4 contract + dev prompt (Bad-case suite expansion + trial milestone-close dry-run; layer `eval_spec`; estimated 5-7 dev-days heavy human-review collaboration; Codex milestone-shared per §4.3 default; 3 fallback paths still NOT locked per S-Eval-1 launch constraint #2; S-Eval-1 §12.7 carry-over informs UC-G/H/I/J prioritisation).
6. Subsequently: S-Eval-5 (L3 judge repositioning + R-item closure of 4 named R-items per milestone_objective §7); then M3-Eval milestone close (milestone-shared Codex review per §4.3 default; manual bad-case suite review per §5.6; M3-Eval closure verdict + archive).

## 7. 注意事项 (Caveats / red lines)

### Codex review specifics for S-Eval-3

- **DO NOT pre-decide Codex verdict** on any of the 5 OQs (OQ-S44.1 through S44.5). Surface to Codex WITHOUT recommendations per `feedback_constitution_discipline_vs_planning_anticipation.md` established pattern.
- **OQ-S44.1 (executor wiring gap) is LOAD-BEARING**. Dev §7 surfaces that `eval_interactive/eval_interactive/batch/executor.py:252` does NOT pass `tier2_result=` to `compute_composite(...)`. The populated content is STRUCTURALLY INERT in the production eval-harness path. Tier-2 only fires offline against synthetic traces (dev §9 verification). Possible classifications: (a) `out_of_scope_review` — routed to S-Eval-4 / S-Eval-5 / follow-on R-item; (b) blocking — broaden S-Eval-3 scope; (c) acceptable as-is — synthetic verification sufficient + carry-over to S-Eval-4 planning. Deliver-agent's reading is (a) or (c); my lean is (c); Codex independently classifies.
- **All 18 desc are dev's row-1 ✅ claim**. Deliver-agent has NOT independently verified each entry; Codex's §5.3 independent re-walk is the substantive semantic defence at the milestone level. If any entry comes back row-3 ❌ or row-4 ❌ → `reject as semantic hardcode`.
- **Dev's deterministic synthetic Tier-2 verification** (dev §9 table of 13 scenarios) is the substitute for a real-LLM eval-harness run per OQ-S44.5 rationale. The choice (synthetic vs real-LLM) is deliberate (avoids §5.5 LLM-provider-drift + the OQ-S44.1 executor-wiring gap). Codex independently weighs in on whether synthetic is sufficient for S-Eval-3 close OR M3-Eval close needs a real-LLM run.

### Process discipline

- **Numbers-cite per `feedback_deliver_agent_cited_numbers_must_be_reproducible.md`**: every numstat / line-count / test-count in the eventual S-Eval-3 §12 closure verdict + Sprint 44 action_bank row MUST be derived from `git show --numstat 01770ac` / `mvn test` / `uv run pytest` directly. Codex independently spot-checks per the review prompt §7-8.
- **Bundle policy per `feedback_commit_at_end_bundles_deliver_artefacts.md`**: dev (Claude Code) does NOT stage deliver-agent files. Deliver-agent close-out files are bundled separately by human at close commit. Dev's `01770ac` commit correctly excluded the deliver-agent S-Eval-3 launch bundle (which had been committed in `db19a47` earlier).
- **Handoff §12 closure verdict ownership per `feedback_handoff_verdict_section_delegation.md`**: dev leaves §12 EMPTY; deliver-agent + human + Codex jointly fill at close AFTER Codex verdict returns.
- **OOSR-with-packaging-note pattern per `feedback_out_of_scope_review_packaging_rollforward.md`**: if Codex returns a finding that's out-of-S-Eval-3-scope (likely OQ-S44.1 executor wiring), classify as OOSR + roll forward as carry-over to S-Eval-4 / S-Eval-5 with explicit packaging note.

### Hard fences inherited from prior sub-sprints

- **No new Tier-0 invariant** without human-review escalation (milestone §6 #2). C2 + C3 candidates (M2 close) STAY DEFERRED until production trace observation at M3+.
- **No edits to runtime semantic surfaces** (RuntimeIntentClassifier / IntentClassification / DriftResult / DriftDetector / UseCaseRouter / ClassifyUseCaseTool / PhaseEvaluator / AgentRunLoopImpl / ControlKernel / system_prompt.txt / tool-policy.yaml / IntakeFieldsRegistry / UseCaseRegistryService / ResolveDispositionEvaluator) inherits from M2 §6 #5.
- **No edits to case fixtures** (smoke / anchor / anchor_outcome / case_families / Alice / shadow / case_spec_overrides.yaml) inherits.
- **No `iteration_governance.md` edit** during S-Eval-3 (only S-Eval-1 §5.5 sentence + potential S-Eval-5 acceptance-bar refinement are allowed).
- **No widening of `escalation_reason` enum** (canonical 23 values; runtime contract).

### Carry-over queues

- **M3-Eval-shared Codex review queue (3 items)** to be addressed at M3-Eval milestone close (NOT S-Eval-3 close):
  1. **OQ-S42.3** (S-Eval-1 D-2.2 demotion interpretation — whether tagging `_check_escalation_reason_consistency` advisory when `expected.escalation_trigger is None` is the right surface).
  2. **OQ-S43.1** (S-Eval-2 DSL parser blocklist surface — positive grammar + illustrative blocklist vs uniform "unknown primitive").
  3. **OQ-S43.4** (S-Eval-2 `tool_event_seq` unsuccessful-dispatch `success=false` ignored — dev's conservative interpretation per proposal §5.3 intent).
- **M2 close fold-back queue (6 items)** STILL DEFERRED — separate governance commit per `doc_governance.md` cadence (Sprint 37 §7.8 typo + Sprint 38 OQ-S38.1 + Sprint 39 §6.2.5/§6.2.6 + Sprint 39 OQ-S39.7 + Sprint 41 OQ-S41.1 + Sprint 41 OQ-S41.4). Deliver-agent + human discretion on timing; can interleave anytime between sub-sprints.
- **Stale-test R-item** `R-stale-test-escalation-enum-sync-v0_2-to-v0_3-migration` (S-Eval-1 close; status proposed; impl deferred to whichever milestone consumes it). 3 paths verbatim: (1) add v0_3.yaml companion + repoint; (2) extract enum from v0_3.md YAML codefence; (3) relocate canonical enum to Python schema with back-link comment. Anti-framings: do NOT silently restore v0_2.{md,yaml}; do NOT widen to all v0_2 references at once; do NOT suppress failing tests without fixing underlying staleness. 2 failing tests in `tests/scoring/test_escalation_enum_sync.py` persist as known baseline state until R-item is consumed.

### Term definitions (key for the new deliver-agent)

- **§5.3 standard table** (proposal §5.3): 4-row table for evaluating `desc` strings — row-1 ✅ soft procedural narrative (acceptable); row-2 ⚠️ soft diagnostic signal (edge — Codex scrutinises); row-3 ❌ hard if-else rule (rejected); row-4 ❌ keyword enumeration matrix (rejected). The SEMANTIC defence on S-Eval-3 content.
- **6 frozen DSL primitives** (S-Eval-2 ship; in `eval_interactive/eval_interactive/scoring/skill_procedure_check.py` `parse_trace_check`): `accumulated_tool_results.<tool>` (presence) / `tool_event_seq(<a>) < tool_event_seq(<b>)` (order) / `intake_state.fields_collected.contains(<field>)` (slot presence) / `session.<flag>_present` (flag boolean) / `any_of(...)` (OR combinator) / `all_of(...)` (AND combinator). Parser rejects anything else at parse time with `TraceCheckDSLSyntaxError` — the STRUCTURAL defence on S-Eval-3 content.
- **`mandatory_for`**: per-step list of canonical UC ids declaring which UCs the step gates Tier-2 on. NOT a semantic decision branch; a scoping list.
- **§4.3 trigger #2**: per-sub-sprint Codex review required when sub-sprint touches §1.7 forbidden-list-adjacent territory (LLM-visible content authoring). S-Eval-3 is the only M3-Eval sub-sprint with this trigger.
- **§4.1 verdict set**: `pass` / `approve with downgrade-to-signal follow-up` / `reject as semantic hardcode` / `needs human architecture decision`.
- **A — Clean PASS** classification: dev shipped contract end-to-end; no §6 hard fence touched; no §10 stop signal fired; §4.1 Q1-Q8 pass + Q9 N/A; no contract drift beyond pre-existing baseline; OQs disposed non-blocking; Codex (if reviewed) returned `pass` first round.

### Key files / paths the new deliver-agent must know

#### Governance + planning (auto-loaded via AGENTS.md)

- `AGENTS.md` / `docs/current/doc_governance.md` / `docs/current/agent_context_guide.md` / `docs/current/iteration_governance.md`

#### Active sub-sprint state

- `docs/milestone_objective.md` (M3-Eval north star; 12 sections; §5.3 table referenced in §8.1)
- `docs/sprint_objective.md` (live S-Eval-3 contract; 12 sections; will archive at close)
- `docs/10-handoff.md` §1 lead (will refresh at close)
- `docs/codex-findings.md` (scaffold; Codex writes)
- `docs/action_bank.md` (R-items + §6 close-action index)
- `compact/sprint-044-dev-prompt.md` (the dev brief Claude Code consumed)
- `compact/sprint-044-review-prompt.md` (NEW; Codex brief; not yet committed)

#### Archives (immutable)

- `docs/sprints/sprint-042-objective.md` + `sprint-042-handoff.md` (S-Eval-1)
- `docs/sprints/sprint-043-objective.md` + `sprint-043-handoff.md` (S-Eval-2)
- `docs/sprints/sprint-044-handoff.md` (S-Eval-3 dev archive at HEAD)
- `docs/milestones/M1_objective.md` + `M2_objective.md` + `M2-Skill_objective.md` (superseded) + `M2_codex-review.md`

#### Code surfaces touched by M3-Eval (S-Eval-1 + S-Eval-2 + S-Eval-3)

- `eval_interactive/eval_interactive/case_spec/schema.py` (S-Eval-1)
- `eval_interactive/eval_interactive/case_spec/loader.py` (S-Eval-1)
- `eval_interactive/eval_interactive/scoring/hard_checks.py` (S-Eval-1)
- `eval_interactive/eval_interactive/scoring/outcome_checks.py` (S-Eval-1)
- `eval_interactive/eval_interactive/scoring/composite.py` (S-Eval-1 + S-Eval-2)
- `eval_interactive/eval_interactive/scoring/skill_procedure_check.py` (NEW S-Eval-2)
- `eval_interactive/case_specs/anchor_outcome/` (NEW S-Eval-1; 12 cases + manifest)
- `eval_interactive/tests/test_s_eval_1_schema_and_scoring.py` (NEW S-Eval-1; 20 tests)
- `eval_interactive/tests/test_skill_procedure_extractor.py` (NEW S-Eval-2; 30 tests; updated S-Eval-3 anchor)
- `eval_interactive/tests/test_skill_procedure_dsl_parser_rejects_hardcodes.py` (NEW S-Eval-2; 36 tests; §1.7 structural defence)
- `eval_interactive/tests/test_composite_gate.py` (S-Eval-1 + S-Eval-2 edits)
- `server/src/main/java/com/gumtree/csagent/service/runtime/skill/Skill.java` (S-Eval-2)
- `server/src/main/java/com/gumtree/csagent/service/runtime/skill/CriticalStep.java` (NEW S-Eval-2)
- `server/src/main/java/com/gumtree/csagent/service/runtime/skill/SkillLoader.java` (S-Eval-2)
- `server/src/main/java/com/gumtree/csagent/service/runtime/ContextProjectionBuilder.java` (S-Eval-2)
- `server/src/main/resources/skills/<6 YAMLs>` (S-Eval-3 content)
- `server/src/test/java/com/gumtree/csagent/service/runtime/skill/SkillCriticalStepsLoadingTest.java` (NEW S-Eval-2; updated S-Eval-3 anchor)
- `server/src/test/java/com/gumtree/csagent/service/runtime/CriticalStepsProjectionTest.java` (NEW S-Eval-2)

#### Source-of-truth artefacts

- `docs/solutions/m3_eval_milestone_proposal.md` (research-agent proposal; LOAD-BEARING §5 + §5.3 + §5.4 + §6 S-Eval-3)
- `docs/runtime_freeze_and_risk_policy.md` §1 + §2 (Tier-0 invariants; UNCHANGED at S-Eval-3)
- `docs/current/customer_service_tool_spec_v0_3.md` (current tool spec; v0_2.{md,yaml} deleted in `db19a47`)
- `docs/current/faq_grounding_contract.md`

### Deliver-agent memory (load on cold start from `~/.claude/agent-memory/sprint-deliver-orchestrator/`)

Most relevant for S-Eval-3 close + S-Eval-4 launch:
- `feedback_commit_at_end_bundles_deliver_artefacts.md` (dev does NOT stage deliver-agent files)
- `feedback_handoff_verdict_section_delegation.md` (dev leaves §12 empty; deliver-agent + human fill at close)
- `feedback_out_of_scope_review_packaging_rollforward.md` (OOSR-with-packaging-note pattern; likely fires on OQ-S44.1)
- `feedback_deliver_agent_cited_numbers_must_be_reproducible.md` (every numstat reproducible from `git show --numstat`)
- `feedback_constitution_discipline_vs_planning_anticipation.md` (surface OQs to Codex WITHOUT deliver-agent pre-decisions)
- `feedback_packaging_codex_findings_supersession.md` (delete-and-add pattern for codex-findings at archive)
- `feedback_corpus_undecidable_premise_check.md` (in-flight downgrade pattern if S-Eval-3 evidence falsifies milestone hypothesis)
- `feedback_mocked_llm_cannot_prove_prompt_causal_change.md` (real-LLM required for prompt-causal evidence; dev's synthetic Tier-2 verification is acceptable because it's structural not LLM-behaviour)
