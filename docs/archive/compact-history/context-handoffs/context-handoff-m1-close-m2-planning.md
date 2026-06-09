# Deliver-agent context handoff — M1 closed + M2 milestone objective drafted (Sprint 36 pending human review)

**Authored:** 2026-05-17 by deliver-agent
**For:** the next deliver-agent instance picking up after M1 close
**Branch:** `refactor/remove-the-shackles`
**Repo:** `/Users/caoruixin/projects/csagent-latest`
**HEAD:** `eb65e2b` (Sprint 35; M1 cumulative tip; deliver-agent-owned files NOT committed yet)

Read order on cold start: this file → `AGENTS.md` (loads `iteration_governance.md` + `doc_governance.md` + `agent_context_guide.md`) → `compact/sprint-deliver-orchestrator.md` (deliver-agent role definition — long-term doctrine; **not duplicated here**) → `docs/milestone_objective.md` (M2 contract — pending review) → `docs/sprint_objective.md` (M2 planning placeholder).

---

## 1. 背景 (Background)

LLM-first customer-service agent for online classifieds marketplace (Gumtree). Java + Spring Boot backend (`server/`), Python eval harness (`eval_interactive/`). Constitution at `docs/current/iteration_governance.md` §1: LLM owns semantic decisions (UC hypothesis, drift, escalation, response); Java owns deterministic boundaries (tool schema, capability, PII, grounding floor, idempotency, persistence, trace).

**Sprint cycle:** human + deliver-agent (this role) + dev-agent (Claude Code, external session) + review-agent (Codex, external session).

**Governance framework upgrade 2026-05-16** (per `iteration_governance.md` §8): milestone framework introduced — 3-5 sub-sprints per milestone, milestone-shared Codex review at close, smoke composite_score demoted to observation (§5.5), curated bad-case suite at `eval_interactive/case_specs/bad_cases/` as new primary acceptance gate (§5.6).

**Where we entered this session:** Sprint 33 (M1 sub-sprint 1) had just shipped at commit `8a22aa6` with Alice closure-criterion (a) PASS single-run evidence. The deliver-agent picked up to evaluate Sprint 33 close and plan Sprint 34.

**Where we are now (2026-05-17):** M1 (DISCOVER + Intake) is fully closed. M2 (Resolution + Escalation + Grounding Discipline) milestone objective is drafted at `docs/milestone_objective.md` pending human review. Sprint 36 (M2 sub-sprint 1) contract has NOT been drafted yet — awaits human approval of M2 objective first.

---

## 2. 目标 (Goals)

### Long-term

Help the human cycle through milestones in the §8 framework: human gives architectural theme → deliver-agent drafts `docs/milestone_objective.md` + per-sub-sprint `docs/sprint_objective.md` + dev/review prompts → human reviews + approves → dev/review agents execute externally → deliver-agent helps human classify close / targeted-fix / OOSR / next-milestone.

### Immediate (this cycle, M2 planning round)

1. **Wait for human review of M2 milestone objective at `docs/milestone_objective.md`.** Three reactions possible:
   - Approve as-is → deliver-agent drafts Sprint 36 contract.
   - Approve with edits → deliver-agent revises M2 objective per human edits.
   - Defer Sprint 36 to a later round → no immediate action.
2. **After M2 objective approved:** draft `docs/sprint_objective.md` for Sprint 36 (replacing the M2 planning placeholder) + draft `compact/sprint-036-dev-prompt.md`. Surface for human review before dev session launch.
3. **Deliver-agent-owned files not committed yet** (per `feedback_commit_at_end_bundles_deliver_artefacts.md`). Human bundles when ready. Suggested commit message captured at end of previous deliver-agent message (M1 close summary message).

### Subsequent (M2 progression after Sprint 36 closes)

Per `docs/milestone_objective.md` §3 sub-sprint sequence:
- Sprint 36 — Grounding discipline on iterative search (`semantic_planner` + `prompt_projection`; anchored on `R-grounding-discipline-iterative-search-fabrication`)
- Sprint 37 — Escalation rationale + confidence SIBLING FIELDS (NO enum widening; `D-new-escalation-reason-enum` preserved)
- Sprint 38 — Handover human-message UX rewrite
- Sprint 39 — URL policy + factual narrowing (Sprint 23 extension; may decide `D-advert-link-product-decision` within scope if product-policy sign-off captured)
- Sprint 40 (parallel-track) — Per-LLM-call latency characterization (consumes `R-llm-provider-latency-drift-2026-05-16`)
- M2 close: deliver-agent + human dispatch milestone-shared Codex review at `compact/M2-review-prompt.md`; M2 closure verdict + archive to `docs/milestones/M2_objective.md`; §6.5 row append in `docs/action_bank.md`.

### M3 candidates (informational; not pre-decided)

- M3-B: Lifecycle (`D-single-handover-orchestrator` P0 launch blocker + scoped `D-full-issue-ledger` + semantic planner shadow mode)
- M3-D: Topic↔UC binding loosening (`R-loosen-topic-uc-binding-llm-owned-drift` constraint-removal; requires research-agent investigation round before scoping)
- Any R-item M2 surfaces

---

## 3. 已确认事实 (Confirmed facts — code-grounded, verify before relying)

### 3.1 M1 sub-sprint commits (HEAD `eb65e2b`)

```
eb65e2b sprint 35: Option β coverage probe + §7.2 worked-example re-anchor decision (M1 sub-sprint 3)
e532f0d sprint 34: intake field prefill UC-G/H/I/J (M1 sub-sprint 2)
8a22aa6 sprint 33: DISCOVER UC-A/FP/H soft-signal + classification guidance (M1 sub-sprint 1)
c9edb37 sprint 32: alternate_candidate_use_cases case family (Sprint 31 OQ4 deferral) — M1 base
```

M1 cumulative range: `c9edb37..eb65e2b`. Java baseline: 932 → 983 (+51 new tests; UC-K regression guard `Sprint71PartialIntakePersistenceTest` 14/14 preserved through M1).

### 3.2 Sprint 33 outcome (M1 sub-sprint 1, `prompt_projection`)

- New `discover_disambiguation_signals` projection slot in `ContextProjectionBuilder.java` (built from `listing_context.status` + `form_topic_subject` + `useCaseRegistry.getCandidateUcsForTopic`); slot is observation-only, runtime non-enforcement (proved by 6-variant parameterised integration test).
- DISCOVER `systemInstruction` extension at `PhaseEvaluator.java:411-431` with principle-level disambiguation paragraph (no regex, no per-UC matrix).
- Sibling teaching paragraph in `system_prompt.txt` adjacent to `already_called` (Sprint 23) and `alternate_candidate_use_cases` (Sprint 31) paragraphs.
- 33 new Java tests: 9 projection + 6 integration variants × 5 invariance bars + supporting. All PASS.
- Alice bad-case real-LLM rerun PASS on closure-criterion (a). `eval_interactive/results/20260516-110928/results.json`.
- 5 OQs surfaced (all M2/M3 candidates or docs-only fold-backs).
- Closure verdict deferred to M1 milestone-shared close per §4.3 default.

### 3.3 Sprint 34 outcome (M1 sub-sprint 2, `skill_state`)

- Extended `IntakeFieldExtractor.java` from UC-K-only to also handle UC-G / UC-H / UC-I / UC-J. UC-I implemented as **deliberate no-op** for symmetry (form-context schema lacks `transaction_reference` source); documented in handoff §4.1 OQ1.
- New `extractFormContextField` generalised field reader; new per-UC helpers (`extractUcGFields`, `extractUcHFields`, `extractUcJFields`, `extractUcIFields`); `handlesUc` extended for UC-G/H/I/J/K.
- Zero edit to `AgentRunLoopImpl.java` (predicted in Sprint 34 contract §5; held).
- Zero edit to `IntakeFieldsRegistry.java` (schema stable; consumed existing aliases).
- 51 new Java tests: 33 extractor unit + 12 projection-guard + 6 integration. All PASS.
- Existing `Sprint71PartialIntakePersistenceTest` 14/14 unchanged green (UC-K regression guard preserved).
- 5 OQs surfaced (form-schema extension for UC-I; inline user-content extraction discipline; UC-K userMessage-mining asymmetry; Sprint71 stale comment fold-back; AgentRunLoopImpl no-edit prediction). All deferred per A-with-OQs-deferred disposition; not flipped to action_bank.

### 3.4 Sprint 35 outcome (M1 sub-sprint 3, `eval_spec` — diagnostic / characterization)

- 25 probe CaseSpecs at NEW directory `eval_interactive/case_specs/probe/option_beta_coverage/` + manifest.
- Analysis doc at `docs/diagnostics/option_beta_coverage_matrix.md` with per-(topic_subject, description-shape) AMBIGUOUS/ROUTED matrix.
- Sprint 32 finding empirically generalized across all three weak-prior multi-candidate topics: Ad Support cross-topic UC-C, Payments cross-topic UC-D, Tech Support cross-topic UC-F — all show topic-bounded alternates only; cross-topic UC structurally absent.
- Probe run: `eval_interactive/results/20260516-140339/results.json` (25 cases; 19 from results.json + 6 recovered via backend trace endpoint for contract-violation cases).
- Zero Java change. Zero production code change.
- Closes `R-option-beta-coverage-gap-uc-a-uc-c-shape`. Opens `R-loosen-topic-uc-binding-llm-owned-drift` (constraint-removal architecture-alignment; deferred to M3+).
- 5 OQs surfaced (informational).

### 3.5 Codex M1 milestone-shared review

- Archive: `docs/sprints/M1-codex-review.md` (snapshot of `docs/codex-findings.md` at M1 close).
- Codex verdict raw: `fix_required / blocking_count: 2`.
- Finding 1: Alice bad-case grounding fabrication on Codex's independent rerun. Trace at `eval_interactive/results/20260516-235235/results.json`. Bot converted generic NTD/IP-rights article `ka41r000000LIEJAA4` into a specific claim about `AD-2001`'s removal reason. Codex's fix-layer triage: `semantic_planner` / grounding-answer discipline (NOT Java keyword/regex guard).
- Finding 2: `docs/diagnostics/option_beta_coverage_matrix.md` §5/§6 recommendations stale vs the actual close decisions in `docs/action_bank.md`.
- All other Codex checks PASSED (scope-discipline per sub-sprint, anti-hardcode kernel walk × 3, hard-fence verification, schema/reproducibility, Java baseline 983/0/0/2 on Codex's clean-detached HEAD).

### 3.6 Alice bad-case multi-trace state at HEAD `eb65e2b`

| trace | path | active_use_case | outcome |
|---|---|---|---|
| Sprint 33 single-run | `eval_interactive/results/20260516-110928/results.json` | UC-A | (a) PASS |
| Deliver-agent M1-close rerun | `eval_interactive/results/20260516-232938/results.json` | UC-A | (a) PASS — graceful `faq_miss_threshold_exceeded` escalation, no fabrication |
| Codex independent rerun | `eval_interactive/results/20260516-235235/results.json` | UC-A | **FAIL** — fabricated specific reason from generic article |

2-of-3 PASS / 1-of-3 FAIL. The FAIL is LLM-variance-on-iterative-grounding (NOT a deterministic bot defect). The new R-item `R-grounding-discipline-iterative-search-fabrication` is opened to address this at the right layer (semantic_planner).

### 3.7 Two parallel research-agents cross-validated M2 selection 2026-05-17

Both general-purpose research-agents read M1 evidence + action_bank + release_gate + Codex findings + Constitution + Sprint 35 matrix, then compared M2-A / M2-B / M2-C / M2-D + the proposed M2-E (Codex Finding 1 grounding candidate, 3 option shapes).

**Agreement points** (both converged):
- M2-C should NOT be a milestone-shape (parallel-track or milestone-of-one).
- M2-D should NOT be M2 (defer to M3+; M1's success was avoiding semantic-routing surfaces; constitution-discipline §7.2-drop is fresh).
- `D-new-escalation-reason-enum` (`action_bank.md:347`) deferral preserved in any M2-A scope (sibling fields only; no enum widening).
- `D-full-issue-ledger` (`action_bank.md:344`) requires explicit human authorization if in M2-B scope.
- Semantic-planner shadow mode in M2-B is under-scoped (define or drop).
- Codex Finding 1 is load-bearing (grounding-floor violation).

**Divergence** (the real choice):
- Agent #1: M2 = M2-A ∪ M2-E.option-3 rolled in; M2-C parallel-track; M2-B → M3; M2-D defer. Codex Finding 1 → OOSR carry (M1-out-of-scope per hard fence). Reason: Codex Finding 1 is fresh real failure > standing P0 without calendar pressure.
- Agent #2: M2-E.option-2 (M1 fix-iteration sub-sprint) FIRST; then M2-B orchestrator. Reason: M2-B is the ONLY candidate that retires a `release_gate.md` §1 blocking rule.

**Human decision** (Decision C, 2026-05-17): Agent #1 path.

### 3.8 Files modified or created in this deliver-agent session (NOT yet committed)

```
M  docs/10-handoff.md                                    (§1 lead refreshed to M2)
M  docs/action_bank.md                                   (R-item flips + new R-item + §6.5 closed-milestone index)
M  docs/diagnostics/option_beta_coverage_matrix.md       (§6.5 "Close decision update" addendum)
M  docs/milestone_objective.md                           (REPLACED with M2 contract; old M1 archived)
M  docs/sprint_objective.md                              (REPLACED with M2 planning placeholder; old M1-close coordination superseded)
?? docs/milestones/M1_objective.md                       (NEW; cp from old milestone_objective + §12 closure verdict appended + frontmatter flipped to archived)
?? docs/sprints/M1-codex-review.md                       (NEW; cp from codex-findings.md at M1 close)
?? docs/sprints/sprint-034-objective.md                  (NEW from earlier session; Sprint 34 contract archive)
?? docs/sprints/sprint-035-objective.md                  (NEW from earlier session; Sprint 35 contract archive)
?? compact/M1-review-prompt.md                           (NEW; drafted in session; used by Codex; preserved for M1 record)
?? compact/sprint-034-dev-prompt.md                      (NEW from earlier session)
?? compact/sprint-035-dev-prompt.md                      (NEW from earlier session)
?? compact/context-handoff-m1-close-m2-planning.md       (THIS file)
?? .claude/agent-memory/sprint-deliver-orchestrator/feedback_constitution_discipline_vs_planning_anticipation.md (NEW)
```

(Some `??` files may have been committed earlier in the session by the human; verify via `git status` at cold start.)

---

## 4. 决策记录 (Decision records)

### 4.1 Sprint 33 close evaluation (earlier in session): A-acceptable; proceed to Sprint 34 with Option C reframe

Sprint 33 shipped Alice (a) PASS single-run + 15 Java tests. Surprise: Alice closed on Sprint 33 alone (M1 plan predicted IMPROVING + Sprint 34 needed for D2). Deliver-agent surfaced three options: A (proceed to Sprint 34 as planned), B (close M1 early), **C (proceed to Sprint 34 but reframe scope — Sprint 34 still ships for broader UC-G/H/I/J regression-depth coverage, not as Alice-gated D2 fix)**. Human picked C.

### 4.2 Sprint 34 close: A-with-OQs-deferred (5 OQs all deferred; no action_bank flip)

Sprint 34 shipped per contract; +51 Java tests; UC-K regression preserved; zero `AgentRunLoopImpl` edit (prediction held). Five OQs surfaced — all deferred per human's "do not flip into action_bank.md now" disposition. Verdict deferred to M1 milestone-shared close.

### 4.3 Sprint 35 close decisions (2026-05-17, deliver-agent + human round)

- **Decision (a) — §7.2 worked-example re-anchor**: initially proposed in M1 plan §3 Sprint 35 row; human first approved (UC-A↔UC-FP target); deliver-agent executed with over-reach (added Note paragraph + Sprint 31 attribution + R-item ID cross-reference in `iteration_governance.md` §7.2); human pushed back on constitution-discipline grounds ("把'原则教学'留在 governance，把'当前实现限制 / 后续优化'放回 action bank"); deliver-agent did **α full revert** of the §7.2 fold-back. Final state: §7.2 stays at UC-A↔UC-C as principled teaching example; current-implementation limit fully captured in `action_bank.md` R-item.
- **Decision (b) — open new R-item for the constraint-removal architectural follow-on**: opened `R-loosen-topic-uc-binding-llm-owned-drift` in `docs/action_bank.md` §5.2 with explicit anti-framings against (1) new live `AlternateUseCaseSurveyor` Java component, (2) per-UC regex/if-else/enum-expansion, (3) collapsing `topic_subject` entirely. The R-item is **constraint-removal / architecture-alignment** per Constitution §1.3 + §1.7, NOT a new component.

### 4.4 Constitution-discipline lesson (captured in agent-memory)

The α revert above prompted a new agent-memory file at `.claude/agent-memory/sprint-deliver-orchestrator/feedback_constitution_discipline_vs_planning_anticipation.md` ("Planning anticipation does not override constitution-discipline review at execution time"). The lesson: a milestone plan or sprint contract that pre-authorizes a governance-doc edit ("if (a) chosen, §X fold-back") authorizes the SCOPE — it does NOT authorize the CONTENT until execution-time review against tier-discipline (governance teaches principles; current-implementation limits belong in optimization backlog). The 4-question execution-time review + anti-pattern signature is documented in the memory file.

### 4.5 M1 close decisions (2026-05-17)

- **Decision A = C-1**: Codex Finding 1 (Alice grounding fabrication) classified as `out_of_scope_review` per M1 §6 hard fence #1 (semantic_planner work is M1-out-of-scope by construction). Opened new R-item `R-grounding-discipline-iterative-search-fabrication` as **high-priority M2 anchor**. Human rationale: "这次问题的 fix layer 是 semantic_planner / grounding-answer discipline，不是 M1 原本三个 sub-sprint 的合同范围。把它硬塞回 M1，会让 M1 scope 膨胀。但必须把它作为 M2 的高优先级问题。"
- **Decision B = B-2a**: Codex Finding 2 (matrix §5/§6 stale) resolved via deliver-agent commit appending "Close decision update (2026-05-17)" addendum to `docs/diagnostics/option_beta_coverage_matrix.md` (NEW §6.5). Preserves dev's analysis-time reasoning + documents constitution-discipline review process. Human rationale: "原来的诊断分析不是错，而是后续 close decision 发生了变化。加 addendum 最诚实，也最符合治理文档的可追溯性"
- **Decision C — M2 pick**: Agent #1 path. M2 = M2-A ∪ M2-E.option-3 (grounding + escalation + customer-honesty); M2-C parallel-track (`R-llm-provider-latency-drift-2026-05-16`); M2-B → M3 unless Salesforce cutover pressure; M2-D → M3+. Human rationale: "优先解决 grounding / customer honesty，现在真正新鲜、真实、影响用户信任的问题，是 bot 编造具体原因。而 orchestrator 虽然是 standing P0，但文中也承认没有明确 Salesforce cutover calendar pressure."
- **M2-A scope discipline**: NO `escalation_reason` enum widening. Use sibling fields only. Preserve `D-new-escalation-reason-enum` deferral per `action_bank.md:347`.

### 4.6 M1 final closure verdict (deliver-agent + human, 2026-05-17)

- Codex verdict raw: `fix_required / blocking_count: 2`
- Deliver-agent + human verdict: **PASS — A-with-Codex-finding-OOSR-classification-and-deliver-agent-finding-2-fix-in-close**
- First milestone-shared close under §8 framework; first instance of this classification pattern.
- Archive: `docs/milestones/M1_objective.md` §12 closure verdict appended; `docs/sprints/M1-codex-review.md` permanent Codex archive.

### 4.7 Sprint 36 (original M1 §3 row — INTAKE-locked reroute investigation) deferred

The original M1 §3 "Sprint 36 conditional" row was an INTAKE-locked reroute trigger investigation that would fire "if Sprint 33+34+35 do NOT sufficiently close Alice". Sprint 33 closed Alice (a); the conditional trigger did NOT fire. The new Sprint 36 in M2 is **a different sprint** — Sprint 36 = M2 sub-sprint 1, anchored on `R-grounding-discipline-iterative-search-fabrication`. The old INTAKE-locked reroute investigation is deferred to M3+ if D3-dimension concerns surface in M2 close evidence.

---

## 5. 当前任务 (Current tasks — what's in flight right now)

### 5.1 Authored and waiting for human review

- `docs/milestone_objective.md` (M2 contract) — drafted 2026-05-17; **pending human review**. If approved, proceed to 5.2. If edits needed, revise.
- `docs/sprint_objective.md` (M2 planning placeholder) — drafted 2026-05-17; tells the human/next deliver-agent that Sprint 36 contract is pending.
- All deliver-agent-owned files staged but NOT committed. Human bundles per `feedback_commit_at_end_bundles_deliver_artefacts.md`. Suggested commit message in last deliver-agent message (M1 close summary).

### 5.2 Pending deliver-agent work (after M2 milestone objective approved)

- Draft `docs/sprint_objective.md` for Sprint 36 (replacing M2 planning placeholder) per the 12-section sub-sprint contract shape. Sprint 36 scope per `docs/milestone_objective.md` §3 Sprint 36 row.
- Draft `compact/sprint-036-dev-prompt.md` per the dev-prompt convention (look at `compact/sprint-035-dev-prompt.md` for the most recent shape).
- Surface to human for review before dev session launch.

### 5.3 Pending deliver-agent work (after Sprint 36 closes)

- Read Sprint 36 dev handoff at `docs/sprints/sprint-036-handoff.md`.
- Help human classify Sprint 36 outcome (close / targeted fix / OOSR).
- If close: draft Sprint 37 contract (Sprint 37 scope per `docs/milestone_objective.md` §3 Sprint 37 row).
- Defer Codex review to M2 milestone-shared close per §4.3 default.

### 5.4 Pending deliver-agent work (M2 close)

- Draft `compact/M2-review-prompt.md` against cumulative M2 commit range (Sprint 36 + 37 + 38 + 39 + 40 commits).
- Help human dispatch Codex; help classify findings.
- Append §12 closure verdict to M2 milestone objective; archive to `docs/milestones/M2_objective.md`.
- Append §6.5 row for M2 in `docs/action_bank.md`.
- Refresh `docs/10-handoff.md` §1 lead.
- Help human pick M3.

---

## 6. 下一步 (Next steps for the next deliver-agent session)

When the next deliver-agent instance starts:

1. **Read this file first.** Then `AGENTS.md` + `compact/sprint-deliver-orchestrator.md`.
2. **Verify git state.** Run `git -C /Users/caoruixin/projects/csagent-latest status` + `git log --oneline -10`. Some deliver-agent-owned files may have been committed by the human since this handoff was authored.
3. **Verify M2 milestone objective approval state.** Ask human if M2 is approved (`docs/milestone_objective.md`) before drafting Sprint 36. If unclear, ask explicitly.
4. **If M2 approved and Sprint 36 not yet drafted:** draft Sprint 36 contract per the M2 §3 Sprint 36 row. Three design directions sketched in `R-grounding-discipline-iterative-search-fabrication` (action_bank.md §5.2 Sprint 35 close / M1 close surfaced subsection) — NOT pre-decided. Sprint 36 dev or a Sprint-30-shape design freeze sprint picks the direction. Mirror Sprint 33 / 34 / 35 contract shape (12 sections).
5. **If M2 not yet approved:** wait for human signal. Do NOT pre-draft Sprint 36.
6. **If M2 needs edits:** revise `docs/milestone_objective.md` per human direction; do not overshoot.
7. **Throughout: respect constitution-discipline.** Especially for any edit to `docs/current/iteration_governance.md` / `docs/foundational/*` / `docs/runtime_freeze_and_risk_policy.md` — re-walk the 4-question check in `.claude/agent-memory/sprint-deliver-orchestrator/feedback_constitution_discipline_vs_planning_anticipation.md` BEFORE committing.

---

## 7. 注意事项 (Cautions / hard constraints / lessons)

### 7.1 Constitution-discipline (new lesson from this session)

When a milestone plan or sprint objective pre-authorizes an edit to a governance-tier doc (`iteration_governance.md`, `doc_governance.md`, `runtime_freeze_and_risk_policy.md`, foundational specs): the planning anticipation authorizes the SCOPE; it does NOT authorize the CONTENT until execution-time review. Before committing such an edit, walk the 4-question check in `.claude/agent-memory/sprint-deliver-orchestrator/feedback_constitution_discipline_vs_planning_anticipation.md`. **Anti-pattern signature**: if the about-to-commit edit adds sprint numbers, R-item IDs, matrix run IDs, dates, or current-state cross-topic / cross-UC enumeration into a governance doc — STOP. That content belongs in `action_bank.md` or `docs/diagnostics/`.

### 7.2 Hard fences preserved through M2

- **No `escalation_reason` enum widening** (sibling fields only on `request_handover`). `D-new-escalation-reason-enum` deferral per `action_bank.md:347` preserved.
- **No Java grounding-citation gate.** `D-hard-citation-gate` deferral per `action_bank.md:348` preserved. Sprint 36 grounding fix is prompt-teaching + soft signal at semantic_planner layer per Constitution §1.3.
- **No `RuntimeIntentClassifier.java` / `IntentClassification.java` / `DriftResult.java` / `DriftDetector.java` / `UseCaseRouter.java` / `ClassifyUseCaseTool.java` touch.** M2-D Topic↔UC binding loosening is deferred to M3+.
- **No `D-single-handover-orchestrator`** (M2-B → M3 unless Salesforce cutover surfaces).
- **No `D-full-issue-ledger`** (hard-deferred per `action_bank.md:344`).
- **No Tier-0 invariant** added without human-review escalation.
- **No edits to existing case families** under `eval_interactive/case_specs/case_families/` (cascade fence carried from M1).
- **No `iteration_governance.md` edit during M2** (M1 close confirmed the constitution-discipline review pattern).

### 7.3 Codex review pattern (per §4.3)

- Default: milestone-shared at milestone close. Single Codex review against cumulative commit range.
- Per-sub-sprint review triggered only by: (1) new Tier-0 candidate, (2) §1.7 forbidden-list red line crossed, (3) hard-fenced surface touched, (4) fix-iteration on prior sub-sprint.
- M1 demonstrated this: 3 sub-sprints × deferred = 1 milestone-shared Codex review at M1 close (single dispatch).

### 7.4 Deliver-agent vs dev / Codex boundary

- Deliver-agent does NOT write business code. Drafts contracts + prompts; helps classify; never edits `server/src/main/`.
- Deliver-agent does NOT dispatch Codex without human authorization.
- Deliver-agent files (`docs/sprint_objective.md`, `docs/milestone_objective.md`, `docs/10-handoff.md`, `docs/action_bank.md`, `docs/codex-findings.md`, `compact/*-prompt.md`, `compact/context-handoff-*.md`) are NOT staged by dev. Human bundles them at deliver-agent commit per `feedback_commit_at_end_bundles_deliver_artefacts.md`.
- Both `docs/milestone_objective.md` and `docs/sprint_objective.md` require human review before being used to dispatch dev/review agents.

### 7.5 Bad-case suite primary gate (§5.6)

The new primary gate (post-2026-05-16) is manual review of `eval_interactive/case_specs/bad_cases/` traces at sub-sprint / milestone close. NOT smoke composite_score (demoted to observation per §5.5). Currently the bad-case suite has 1 case: Alice (`alice_uc_a_uc_h_misclass.yaml`). M2 may grow it via deliver-agent + human curation per §5.6 lifecycle.

### 7.6 Multi-trace LLM variance is real

Alice closure-criterion (a) was 2-of-3 PASS / 1-of-3 FAIL at M1 close. The FAIL trace was Codex's independent rerun — it surfaced a real fabrication shape that the deliver-agent's own rerun did NOT trigger. This is why M2 anchors Sprint 36 on grounding-discipline: LLM variance can hit the fabrication condition; the bot should be hardened against it.

### 7.7 Open R-items M2 does NOT consume (deferred / avoid list)

- `R-llm-provider-latency-drift-2026-05-16` — M2 parallel-track Sprint 40 consumes
- `R-grounding-discipline-iterative-search-fabrication` — M2 Sprint 36 anchor
- `R-loosen-topic-uc-binding-llm-owned-drift` — deferred to M3+
- `D-single-handover-orchestrator` — M3 candidate (M2-B) unless cutover pressure
- `D-full-issue-ledger` — hard-deferred; needs explicit new-objective approval
- `R-cs176-semantic-planner-escalation-family-discrimination` — addressed by M2 Sprint 37 (sibling fields)
- `D-advert-link-product-decision` — M2 Sprint 39 may decide within scope if product-policy sign-off captured
- Sprint 33 OQ1-5, Sprint 34 OQ1-5, Sprint 35 OQ1-5 — all M2 / M3 / docs fold-back candidates; not all flipped to action_bank yet

### 7.8 Key memory files (cross-session) in `.claude/agent-memory/sprint-deliver-orchestrator/`

Load all on cold start:
- `feedback_commit_at_end_bundles_deliver_artefacts.md` — dev NOT stage deliver-agent files; human bundles
- `feedback_handoff_verdict_section_delegation.md` — dev §12 closure verdict delegation
- `feedback_out_of_scope_review_packaging_rollforward.md` — OOSR-with-packaging-note pattern
- `feedback_close_with_codex_skipped_docs_only_outcome.md` — A-with-Codex-skipped for docs-only
- `feedback_corpus_undecidable_premise_check.md` — in-flight downgrade pattern
- `feedback_deliver_agent_cited_numbers_must_be_reproducible.md` — every number cites source + recipe
- `feedback_mocked_llm_cannot_prove_prompt_causal_change.md` — real-LLM required for prompt-causal evidence
- `feedback_probe_sprint_shape_for_conditional_broadening.md` — probe sprint shape
- `feedback_multi_layer_prospective_stanza.md` — two-track stanza shape
- `feedback_packaging_codex_findings_supersession.md` — delete-and-add supersession for codex-findings
- **`feedback_constitution_discipline_vs_planning_anticipation.md` — NEW THIS SESSION; planning anticipation ≠ execution-time authorization for governance-tier edits**

---

## 8. Quick verification checklist for the next deliver-agent

On cold start, run these to confirm state:

```bash
# Verify HEAD + commit graph
git -C /Users/caoruixin/projects/csagent-latest log --oneline -5
# Expected: top commit is eb65e2b (sprint 35); 8a22aa6 / e532f0d below

# Verify deliver-agent-owned files state (may have been committed by human since handoff)
git -C /Users/caoruixin/projects/csagent-latest status --short docs/milestone_objective.md docs/sprint_objective.md docs/10-handoff.md docs/action_bank.md docs/milestones/

# Verify M1 archive exists
ls -la /Users/caoruixin/projects/csagent-latest/docs/milestones/M1_objective.md

# Verify M1 Codex review archive exists
ls -la /Users/caoruixin/projects/csagent-latest/docs/sprints/M1-codex-review.md

# Verify Codex findings on the topic of M1 (read first 20 lines)
head -20 /Users/caoruixin/projects/csagent-latest/docs/codex-findings.md
# Expected: Codex M1 milestone review header showing fix_required / 2

# Verify new R-item is in action_bank §5.2
grep -n "R-grounding-discipline-iterative-search-fabrication" /Users/caoruixin/projects/csagent-latest/docs/action_bank.md

# Verify §6.5 closed-milestone index has M1 row
grep -n "^## 6.5" /Users/caoruixin/projects/csagent-latest/docs/action_bank.md
```

If anything is unexpected vs this handoff (e.g., HEAD moved, M2 milestone objective edited, new commits), pause and re-read state before acting.

---

## 9. Suggested commit message (if human hasn't bundled yet)

```
docs: close M1 (PASS A-with-Codex-OOSR-classification + deliver-agent-finding-2-fix);
draft M2 (Resolution + Escalation + Grounding Discipline)

- M1 closure: archive to docs/milestones/M1_objective.md with §12 verdict;
  Codex M1 review snapshotted to docs/sprints/M1-codex-review.md;
  Finding 1 → out_of_scope_review carry to new R-item
  R-grounding-discipline-iterative-search-fabrication (M2 anchor);
  Finding 2 → resolved in close via diagnostic-doc addendum.
- M2 drafted: docs/milestone_objective.md replaced with M2 contract;
  5 sub-sprints across Track A (36-39) + Track C parallel (40);
  hard fences preserve D-new-escalation-reason-enum + D-hard-citation-gate;
  M2-B orchestrator → M3; M2-D Topic↔UC loosening → M3+.
- §6.5 closed-milestone index added to docs/action_bank.md.
- docs/10-handoff.md §1 lead refreshed: Current=M2; Preceding milestone=M1.
- New deliver-agent memory: feedback_constitution_discipline_vs_planning_anticipation.md.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
```

---

End of context handoff. The next deliver-agent reads this + the role brief + verifies state, then proceeds per §5.2 (Sprint 36 contract drafting) when human signals.
