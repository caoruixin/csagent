# Deliver-agent context handoff — Sprint 31 fix-iteration #2 in flight

**Authored:** 2026-05-16 by deliver-agent
**For:** the next deliver-agent instance picking up this sprint cycle
**Branch:** `refactor/remove-the-shackles`
**Repo:** `/Users/caoruixin/projects/csagent-latest`

Read order on cold start: this file → `AGENTS.md` → `docs/current/iteration_governance.md` § 1 / 3 / 5 / 7 → `compact/sprint-deliver-orchestrator.md` (the orchestrator brief — long-term role definition; **not duplicated here**).

---

## 1. 背景 (Background)

The project is a customer-service agent for an online classifieds marketplace, LLM-first architecture (Constitution at `docs/current/iteration_governance.md` §1). Sprint cycle is human + deliver-agent (this role) + dev-agent (Claude Code) + review-agent (Codex). The deliver-agent does NOT write business code; it plans sprints, drafts `docs/sprint_objective.md`, drafts the dev/review prompts under `compact/sprint-NNN-*-prompt.md`, and helps the human classify outcomes (close / targeted fix / out-of-scope review / human escalation).

**Where we entered this session:** Sprint 29 had just closed; the human asked the deliver-agent to investigate which features from an original Sprint 17–23 + Triggered Sprint roadmap (named in a now-deleted `csagent-solution-_20260514.md`) were already delivered vs not, and to lock down the next sprint scope.

**Where we are now:** Sprint 31 has been committed and closed by the human (`8908775`) but Codex post-close review returned `fix_required / 2`. The deliver-agent + human classified Finding 1 as `out_of_scope_review` (substance closes per the dev's §13 fix-iteration evidence) and Finding 2 as `fix_required` (targeted P1: strengthen the T8 non-enforcement test). The deliver-agent has authored `compact/sprint-031-fix-dev-prompt.md` + `compact/sprint-031-fix-review-prompt.md`. The human will launch the dev session externally in a fresh Claude Code window.

---

## 2. 目标 (Goals)

### Long-term

Help the human cycle through sprints in the pattern: human gives scope → deliver-agent plans + drafts sprint_objective + dev/review prompts → human reviews + approves → dev implements externally → review reviews externally → deliver-agent helps human classify close / targeted-fix / out-of-scope / next-sprint.

### Immediate (this cycle, Sprint 31 closure)

1. Wait for the human to launch the Sprint 31 fix-iteration #2 dev session externally (using `compact/sprint-031-fix-dev-prompt.md`).
2. After dev commits the strengthened T8 + new `docs/sprints/sprint-031-fix-handoff.md`, optionally launch Codex re-review (using `compact/sprint-031-fix-review-prompt.md`).
3. After Codex returns (expected `pass / 0`), do the final Sprint 31 close-out artefacts (deliver-agent owned):
   - Update Sprint 31 main handoff §12 closure verdict: `PASS` with Finding 2 closed by fix-iteration #2, Finding 1 accepted as `out_of_scope_review` carried by `R-llm-provider-latency-drift-2026-05-16`.
   - Refresh `docs/10-handoff.md` §1 lead: demote Sprint 31 to Preceding sprint; set the next sprint's prep state as Current.
   - Flip `R-alternate-uc-signal-data-source` to `done (Sprint 31)` in `docs/action_bank.md`.
   - Confirm `R-llm-provider-latency-drift-2026-05-16` is registered in `docs/action_bank.md` (or register it if §13 fix-iteration #1 commit didn't).

### Subsequent (the next sprint after Sprint 31 closes)

Two natural candidates per Sprint 31 handoff §12 follow-on-sprint-sequencing:

- **(a) `R-llm-provider-latency-drift-2026-05-16` diagnostic sprint** — `infra` / observability characterization of the smoke latency widening (+84% mean elapsed_ms vs Sprint 28 ref). Investigation-class sprint; ~Sprint 27/30 docs-only-or-mostly-docs precedent.
- **(b) Sprint 31+1 case-family-authoring sprint** per OQ4 pre-pick — `eval_spec` corpus authoring for the §7.2 UC-A↔UC-C target / neighbor / negative / shadow split. Validates the `alternate_candidate_use_cases` slot end-to-end.

Disjoint; can run in parallel or sequence. **Human picks at next planning.**

---

## 3. 已确认事实 (Confirmed facts — code-grounded, verify before relying)

### 3.1 Architectural premises (verified 2026-05-16 at HEAD `df8b8cd` and onward)

1. `RuntimeIntentClassifier.classify()` at `server/src/main/java/com/gumtree/csagent/service/runtime/RuntimeIntentClassifier.java:160–269` returns ONE `IntentClassification` with ONE `predictedUseCase`. No alternates list. The §7.2 hypothetical worked example in `iteration_governance.md` claimed the classifier already surfaces alternates — **that claim is wrong against current code**.
2. `IntentClassification.java` at `server/src/main/java/com/gumtree/csagent/model/IntentClassification.java` — no `alternates` / `candidates` field.
3. `DriftResult.java` at `server/src/main/java/com/gumtree/csagent/model/DriftResult.java:21–24` — fields `{type, newUseCase, escalationRequested}` only; single-target.
4. `BotSession.candidateUseCases` is a `String[]` that **collapses to single-element `[targetUc]` after first reroute** per `server/src/main/java/com/gumtree/csagent/service/runtime/ControlKernel.java:686` (fallback path at line 1201). On every post-DISCOVER turn `candidateUseCases - activeUseCase = []`.
5. `ContextProjectionBuilder` already projects existing `candidate_use_cases` slot at `server/src/main/java/com/gumtree/csagent/service/runtime/ContextProjectionBuilder.java:380–394` (Sprint 7 §I0 cue).
6. **Sprint 30 design pass surfaced one additional load-bearing premise:** `SessionManager.java:185–189` AMBIGUOUS branch currently **discards** `routingResult.ambiguousCandidates()`. The data exists on `RoutingResult` (a record with `ambiguousCandidates: List<String>` accessor) but the AMBIGUOUS branch proceeds directly to `setCurrentPhase("DISCOVER")` without preserving them. This is the load-bearing premise for Sprint 31's Option β implementation.

### 3.2 Sprint 17–23 + Triggered roadmap actual delivery status (matrix produced 2026-05-16)

| # | Original scope | Status | Where it lives / why diverged |
|---|---|---|---|
| 17 | Semantic Ownership & Shadow Baseline | **delivered** | Sprint 17 G0 → Constitution `docs/current/iteration_governance.md` §1.3 / §1.4. Shadow baseline (G2) shipped via Sprint 20 Track A. |
| 18 | Semantic Planner Shadow Mode + Soft-signal Projection | **not_started** | Sprint 18 was renamed to G1 Failure Portfolio. No shadow planner. Sprint 30/31 partially address the soft-signal projection piece via the `alternate_candidate_use_cases` slot. |
| 19 | Low-risk Live Reroute Pilot | **delivered earlier** | Reroute MVP shipped in Sprint 10: `RuntimeIntentClassifier.java:43`, `ControlKernel.applyRerouteDecision:651`. Sprint 19 itself became a smoke regression investigation sprint. |
| 20 | Skill Plan-state: FAQ-resolve + Intake | **partial** | Sprint 20 Track B shipped `already_called` slot. Track A did G2 case families. Broader "Skill Plan-state" durability is not built. |
| 21 | Escalation Reason + Human UX | **partial** | 23-value `escalation_reason` enum frozen at `PhaseEvaluator.java:39–63`. Sprint 21 was actually a Wave A5/A6 L3 review batch (eval-spec). No new human-UX work. |
| 22 | Issue Ledger MVP | **not_started** | Sprint 22 was docs-only phase-2 reconciliation. Issue Ledger explicitly deferred at `docs/runtime_freeze_and_risk_policy.md:125` and `docs/action_bank.md:116`. |
| 23 | Narrow Factual / URL Policy + Projection Cleanup | **partial** | Sprint 23 became repeated-FAQ-call + LLM-stall investigation. Projection cleanup (`already_called` prompt teaching at `system_prompt.txt:23–28`) shipped. Factual narrowing / URL policy not touched. |
| Triggered | Single Handover Orchestrator | **not_started** | Design freeze at Sprint 16 (`docs/proposals/handover_orchestrator_design.md`). NO `HandoverOrchestrator` class anywhere in `server/src/main/java/`. Listed as P0 launch blocker per `docs/release_gate.md` §1.1 and `csagent_system_design_review.md` Part 6. |

### 3.3 Sprint 30 outcome (committed at `3812153`)

**Class:** investigation-only docs-only design sprint. **Exempt** from §7 semantic-touching stanza per docs-only exemption (Sprint 16 / 22 / 26 / 27 precedent). Codex review intentionally skipped per §4.1 docs-only exemption. **Closed A-with-Codex-skipped** (third instance of the pattern).

**Deliverable:** `docs/proposals/alternate_uc_signal_data_source_design.md` (700 lines, 10 sections). The design pass evaluated 5 candidate data sources (Options α / β / γ / δ / ε) and **recommended Option β**: capture `RoutingResult.AMBIGUOUS` candidates at intake on a new `BotSession.intakeAmbiguousCandidates: String[]` field, project as `alternate_candidate_use_cases` minus the active UC. Layer: `prompt_projection` per §3.2 Q3.

**Five open questions for Sprint 31** surfaced in design-doc §8; deliver-agent pre-picked defaults (documented in archived `docs/sprints/sprint-031-objective.md` §5):

| OQ | Pre-pick | Why |
|---|---|---|
| 1. ROUTED behaviour | (a) simple null-on-ROUTED | matches design recommendation; single-track |
| 2. Teaching paragraph location | (a) sibling to `already_called` | universal soft-signal grouping; Sprint 23 pattern |
| 3. §7.2 fold-back | normal 3–5-sprint cadence | minor drift; not load-bearing |
| 4. CaseSpec sequencing | defer to Sprint 31+1 | avoid Sprint 23-style scope sprawl |
| 5. R-uc-cdf interaction | Sprint 31 ships first | two R-items independent |

**Honest acknowledgement Option β surfaces:** intake snapshot is fixed at session create time. A mid-session UC shift the intake router did NOT anticipate (e.g. UC-A → UC-D where topic-family was UC-A-only at intake) will NOT appear in the projection. The §7.2 UC-A↔UC-C shape DOES fall in intake-snapshot coverage (shared "Replies & Messaging" topic family); UC-A↔UC-D would not. Future sprint can layer Option γ on top if observed traces show insufficiency.

### 3.4 Sprint 31 outcome (committed at `2c1fd41` + `de47635` + `8908775`)

**Class:** implementation sprint, single-track, semantic-touching (`prompt_projection` + prompt edit). **§7 stanza REQUIRED**, filled in archived `docs/sprints/sprint-031-objective.md` §9. **Codex review REQUIRED**, no exemption.

**Shipped:** 7-row file table per design doc §5 verbatim:
- `BotSession.java` new field `intakeAmbiguousCandidates: String[]` + V14 Flyway migration.
- `SessionManager.java:185–189` AMBIGUOUS-branch assignment (captures what was discarded); ROUTED branch left null per OQ1.
- `ContextProjectionBuilder.java:380–394` adjacent new `alternate_candidate_use_cases` projection slot.
- `system_prompt.txt:23–28` adjacent sibling teaching paragraph mirroring `already_called` shape.
- 10 new Java tests: `IntakeAmbiguousCandidatesProjectionTest` (unit) + `AgentRunLoopAlternateCandidateUseCasesNonEnforcementIntegrationTest` (T8 integration).

**Java suite:** 912 / 1-inherited / 0 / 2 (inherited failure `SystemPromptUserRequestedTiebreakerTest` from a pre-existing unauthored `system_prompt.txt` working-tree mod, unchanged).

**Smoke gap (Sprint 31 §10 acceptance bar FAILED):** vs Sprint 28 reference `eval_interactive/results/20260514-181257/results.json`:
- `passed_cases` 2 → 1
- `mean_composite_score` 0.1299 → 0.0617
- `mean_outcome_score` 0.7302 → 0.6141
- `mean_judge_score` 0.6667 → 0.5714

**Fix-iteration #1** (`de47635`, no code; §13 evidence append on the handoff) ran three additional smoke reruns + a controlled prompt revert/restore. Conclusion:
- Cold-start race hypothesis **REJECTED** (warm bot rerun #2 was WORSE).
- Teaching paragraph hypothesis **REJECTED** (revert rerun #3 did NOT recover composite).
- External LLM provider drift dominates: +84% mean `elapsed_ms` widening between Sprint 28 (2026-05-15) and Sprint 31 reruns (2026-05-16) with zero Sprint 31 latency-relevant code/config change. Four AMBIGUOUS-intake cases (cs_015 / 040 / 176 / 192) hit `CONTRACT_VIOLATION:active_use_case` when bot is slow (classify_use_case never completes). cs_029 recovers to 0.9667 on rerun #3 (bot-instance LLM-latency-draw dependence).

**Path A close** (per §13.7 dev recommendation, human-applied 2026-05-16): PASS with smoke gap attributed to external drift. Classification: **A-with-fix-iteration-investigation**.

### 3.5 Codex post-close findings (committed somewhere ≤ `8908775` per Codex review header)

Codex returned `fix_required / blocking_count: 2`:

1. **Finding 1 (BLOCKING per Codex):** Smoke acceptance floor regressed vs Sprint 28 reference. Cites the same numbers as §3.4. Codex notes the §13 external-drift hypothesis "may be a valid follow-on explanation, but it does not clear the explicit Sprint 31 close gate."

2. **Finding 2 (BLOCKING per Codex):** T8 non-enforcement integration test is too weak. Tests exactly ONE populated-slot scenario (`["UC-A", "UC-C"]`); stubs no-tool-call final answer; only asserts the loop returns `FINAL_ANSWER`. Does NOT compare populated vs empty / unrelated / different contents; does NOT exercise dispatch / phase transition / reroute / escalation outputs under varying slot values. A future Java branch like `if alternate_candidate_use_cases.contains("UC-C") then ...` could still evade this test. Review prompt §3.3 made this a hard `fix_required` gate.

**Anti-hardcode kernel:** Codex confirmed `approve` on the kernel (no semantic hardcode found). Hard fence verification: no forbidden surfaces touched in `3812153..8908775`. Schema backwards-compat verified. The two blockers are Findings 1 and 2 only.

---

## 4. 决策记录 (Decision records)

### 4.1 Sprint 30 scope: D1 chosen over D / D2 / D3 (deprecated paths recorded for context)

- **Originally proposed (D):** ship the projection slot directly (small `prompt_projection`-only sprint mirroring §7.2 worked example).
- **Discovered:** the §7.2 worked example's data source assertion is wrong (premise gap per §3.1.1–§3.1.5).
- **D2 (rejected):** two-track sprint (data plumbing + projection slot) — rejected as too large, risk of Sprint 23-style scope sprawl.
- **D3 (rejected):** data plumbing only — rejected because it ships nothing the LLM can consume.
- **D1 (chosen):** investigation-only design sprint. Output: design doc + R-item. Sprint 31 implements.

### 4.2 Sprint 30 dev session shape: spawned as sub-agent

Per `feedback_commit_at_end_bundles_deliver_artefacts.md`, the dev sub-agent staged 3 files (design doc + action_bank append + handoff). Human committed (`3812153`) bundling Sprint 29 close + Sprint 30 design freeze.

### 4.3 Sprint 31 scope: Option β chosen (5 candidates evaluated)

See §3.3 above. Rationale in `docs/proposals/alternate_uc_signal_data_source_design.md` §4 (full Constitution justification).

### 4.4 Sprint 31 dev session shape: launched externally by human

Different from Sprint 30 (sub-agent). Sprint 31 was implementation-class with smoke run + Java tests; human preferred external execution.

### 4.5 Sprint 31 close: Path A (PASS) chosen over Path B (fix_required)

Per §13.7 dev recommendation. Human-applied 2026-05-16. Sprint 31 handoff §12 reflects this.

### 4.6 Codex post-close findings classification: Finding 1 OOSR, Finding 2 fix-iteration #2

**Finding 1 → `out_of_scope_review`:** the §13 fix iteration is rigorous disambiguation — both Sprint-31-cause hypotheses falsifiably REJECTED via controlled A/B. External LLM provider drift is the dominant cause. Constitution §1.6 (*"Eval is evidence, not authority"*): a regression provably not caused by the sprint's changes does not fire the close gate. Carried by new R-item `R-llm-provider-latency-drift-2026-05-16`. Precedent: `feedback_out_of_scope_review_packaging_rollforward.md`.

**Finding 2 → `fix_required` (targeted P1, small fix):** Codex is correct that T8 is too weak. The fix is bounded: parameterize T8 across 4–6 slot variants (null / empty / single-active / single-alternate / multi / unrelated) and assert behaviour invariance (TerminalOutcome, llm call count, zero tool dispatch, final message identity). ~2 hours of dev work; single-file change.

### 4.7 Sprint 31 fix-iteration #2 launch: external (per human pick 2026-05-16)

Same external pattern as Sprint 31 main dev session. Sub-agent NOT spawned.

---

## 5. 当前任务 (Current tasks — what's in flight right now)

### 5.1 Authored and waiting for human action

| Path | Status | What's needed |
|------|--------|---------------|
| `compact/sprint-031-fix-dev-prompt.md` | NEW, ready | Human pastes from line 3 onward into fresh Claude Code session on `refactor/remove-the-shackles`. Single-file fix: strengthen `AgentRunLoopAlternateCandidateUseCasesNonEnforcementIntegrationTest.java` with ≥4 parameterized variants. |
| `compact/sprint-031-fix-review-prompt.md` | NEW, ready | After dev commit lands, human optionally launches Codex with this. §3 has the mental-check gate for Finding 2 closure; §4 confirms Finding 1 OOSR acceptance. |

### 5.2 Pending deliver-agent work (after fix-iteration #2 lands)

- **Sprint 31 main handoff §12** — append final closure verdict reflecting fix-iteration #2 + Finding 1 OOSR + R-llm-provider-latency-drift carry. (Existing §12 has Path A close; update to PASS + fix-iteration #2 applied.)
- **`docs/10-handoff.md` §1 lead refresh** — demote Sprint 31 fully to Preceding sprint; set next sprint prep as Current. Current §1 lead is the Sprint 31 prep that I wrote pre-Sprint-31-commit; it's already stale (Sprint 31 is committed and partially closed).
- **`docs/action_bank.md` disposition updates:**
  - Flip `R-alternate-uc-signal-data-source` to `done (Sprint 31, with fix-iteration #2 strengthening T8)`.
  - Confirm `R-llm-provider-latency-drift-2026-05-16` is registered; register if absent.
  - Append §6 closed-action index row for Sprint 31.
- **Optional Sprint 31 archive at `docs/sprints/sprint-031-handoff.md`** — already exists from dev session; deliver-agent just updates §12. Archived objective at `docs/sprints/sprint-031-objective.md` already exists.

### 5.3 TaskList state (in my session, will not transfer)

The TaskCreate-tracked tasks 1–10 are conversation-scoped. New deliver-agent should re-create relevant ones; the only one still in-flight is "Draft Sprint 31 fix-iteration #2 prompts + final close artefacts" (split: prompts authored ✓; final close artefacts pending dev commit).

---

## 6. 下一步 (Next steps)

### 6.1 Immediate

1. Human launches Sprint 31 fix-iteration #2 dev session externally (paste `compact/sprint-031-fix-dev-prompt.md` content from line 3 onward).
2. Dev session runs, strengthens T8, writes `docs/sprints/sprint-031-fix-handoff.md`, runs `mvn -q -pl server -Dtest=AgentRunLoopAlternateCandidateUseCasesNonEnforcementIntegrationTest test` + full suite, stages two files (test + fix-handoff), commits.
3. Human (or new deliver-agent) verifies the commit diff matches the §5 scope-discipline gate in the fix-review prompt (single test file + one handoff file; no production code touched).
4. Human optionally launches Codex re-review with `compact/sprint-031-fix-review-prompt.md`. Expected `pass / 0`.
5. New deliver-agent does §5.2 final close-out artefacts.

### 6.2 After Sprint 31 fully closes

Human picks next sprint from §2 candidates (a) or (b) — diagnostic for latency drift OR case-family-authoring for the new slot. Or some other priority. Re-run the planning cycle: deliver-agent drafts `docs/sprint_objective.md` + `compact/sprint-NNN-dev-prompt.md` + `compact/sprint-NNN-review-prompt.md`.

### 6.3 Carried R-items (need deliver-agent attention in future planning)

- `R-llm-provider-latency-drift-2026-05-16` — opened by Sprint 31 §13 fix-iteration. Diagnostic sprint needed to characterize whether the +84% elapsed_ms widening is provider drift, accumulated DB state, or another infra factor.
- `R-prompt-phase-plan-directive-followship` at `docs/action_bank.md:450` — still **open**, n=1 D616.2 contribution from Sprint 29 (need n≥2 for (R1) structural trigger). NOT closed by Sprint 31.
- `R-per-turn-phase-transition-dump-for-smoke-harness` — Sprint 29 named-but-not-opened follow-on; check if/when to open.
- `R-sprint-31-case-family-authoring` (informally named, not opened) — OQ4 deferral, the Sprint 31+1 case-family sprint.
- **Single Handover Orchestrator (P0 launch blocker)** — still `not_started` (no `HandoverOrchestrator` class in `server/src/main/java/`). Design freeze at `docs/proposals/handover_orchestrator_design.md` (Sprint 16). The only P0 in `docs/release_gate.md` §1.1 / `csagent_system_design_review.md` Part 6.
- **Issue Ledger MVP** — `not_started`. Frozen-deferred per `docs/runtime_freeze_and_risk_policy.md:125`.

---

## 7. 注意事项 (Cautions — non-obvious constraints the new agent MUST honor)

### 7.1 Constitution + governance hard fences

- **§1.7 forbidden list:** no keyword / regex / if-else / per-UC matrix for soft semantic decisions unless a Tier-0 invariant is broken. The Sprint 31 anti-hardcode posture (soft signal, runtime never gates on slot value) is load-bearing.
- **§1.5 iteration rule:** LLM owns drift / topic shift / escalation posture / response strategy (§1.3); Runtime owns tool schema / capability / PII / grounding / persistence / trace contract (§1.4).
- **§1.6 Eval is evidence, not authority:** a regression provably not caused by the sprint can be OOSR-classified. This is exactly the rationale for the Finding 1 OOSR decision.
- **§5 Eval Acceptance Rules:** no regression on safety floor / grounding floor / wrong-containment / over-escalation (regardless of cause). Provider-drift OOSR is acceptable per §1.6 + Sprint 31 §13 precedent, but ONLY when the disambiguation is rigorous (controlled A/B, falsifiable hypothesis testing).

### 7.2 Deliver-agent role boundaries (per `compact/sprint-deliver-orchestrator.md`)

- **Do NOT write business code** (the dev agent does this).
- **Do NOT do code reviews** (the review agent does this).
- **Do NOT silently expand sprint scope** without human review.
- **Do NOT update `docs/sprint_objective.md` without human approval** (the human reviews + approves before dev/review run).
- **DO push back when scope is too big** (`feedback_probe_sprint_shape_for_conditional_broadening.md`, Sprint 23 fix-iteration precedent).
- **DO surface premise gaps before drafting** (e.g. the §7.2 worked example data-source gap surfaced before Sprint 30 was drafted).
- **DO offer the human a clear A / B / C / D classification** when Codex returns findings (per the orchestrator brief).

### 7.3 Specific sprint-31-cycle constraints

- **Finding 2 fix is bounded to ONE file** — `AgentRunLoopAlternateCandidateUseCasesNonEnforcementIntegrationTest.java`. The non-enforcement test must be strengthened to demonstrate INVARIANCE under varying slot values (Constitution §1.7), not just "the happy path doesn't crash with one slot value."
- **Finding 1 must NOT be re-litigated** in the fix-iteration — it's classified OOSR. If the new deliver-agent disagrees with the classification, raise it as a governance concern, not as a re-open of Sprint 31.
- **The pre-existing `system_prompt.txt` working-tree mod** (Sprint 6 §G1 1-line parenthetical removal) is the inherited baseline; Sprint 31 did NOT introduce a new perturbation; the `SystemPromptUserRequestedTiebreakerTest` inherited failure is documented as expected.
- **`docs/sprints/sprint-031-handoff.md` §13 is immutable** — it's the disambiguation record. Fix-iteration #2 dev writes a NEW file `docs/sprints/sprint-031-fix-handoff.md` (mirroring Sprint 20 fix-handoff shape), does NOT edit §13.
- **`iteration_governance.md` §7.2 is intentionally stale** post-Sprint-31. The worked example's "RuntimeIntentClassifier already surfaces" clause is now wrong; the OQ3 pre-pick deferred the fold-back to the normal 3–5-sprint cadence. New deliver-agent SHALL NOT silently edit §7.2 in place; it's tracked for fold-back.
- **Sprint 20 / Sprint 29 case-family cascade fence** — no edits to existing case families. The Sprint 31+1 case-family sprint (per OQ4) authors NEW cases under a new directory.

### 7.4 File-state warnings at this snapshot (2026-05-16, before Sprint 31 fix-iteration #2 dev session)

- `docs/sprint_objective.md` is the Sprint 31 contract (written by deliver-agent during Sprint 31 prep; was committed in `8908775` and remains). New deliver-agent SHOULD treat this as "frozen Sprint 31 contract" and only modify it during the next deliver-cycle (Sprint 32+ planning).
- `docs/sprints/sprint-031-objective.md` and `docs/sprints/sprint-031-handoff.md` are committed (Sprint 31 archive + main handoff with §13).
- `docs/10-handoff.md` was edited by deliver-agent during Sprint 31 prep and now describes Sprint 31 as "prep state" — this is **stale**. After fix-iteration #2 commits, the new deliver-agent SHALL refresh §1 lead.
- `compact/sprint-031-dev-prompt.md`, `compact/sprint-031-review-prompt.md`, `compact/sprint-031-fix-dev-prompt.md`, `compact/sprint-031-fix-review-prompt.md` — all deliver-agent-owned; NOT committed; NOT staged by the dev session.
- `csagent_system_design_review.md` working-tree mod is pre-existing unrelated; do not touch.
- `eval_interactive/case_specs/case_families/sprint29_directive_probe/` from Sprint 29 — committed; do not touch.

### 7.5 Memory file references (deliver-agent-specific feedback recorded in `~/.claude/agent-memory/sprint-deliver-orchestrator/`)

The deliver-agent role pattern relies on several memory files; the new deliver-agent should re-load them:

- `feedback_commit_at_end_bundles_deliver_artefacts.md` — dev does NOT stage deliver-agent files; human bundles at commit time.
- `feedback_handoff_verdict_section_delegation.md` — dev does NOT fill handoff §12 closure verdict; deliver-agent + human own it.
- `feedback_out_of_scope_review_packaging_rollforward.md` — the OOSR-with-packaging-note classification pattern (applies to Finding 1 OOSR here).
- `feedback_close_with_codex_skipped_docs_only_outcome.md` — A-with-Codex-skipped classification for docs-only sprints (Sprint 16 / 22 / 26 / 27 / 30).
- `feedback_corpus_undecidable_premise_check.md` — the in-flight downgrade pattern (Sprint 29).
- `feedback_deliver_agent_cited_numbers_must_be_reproducible.md` — every number in handoffs / design docs cites source path + extraction recipe.
- `feedback_mocked_llm_cannot_prove_prompt_causal_change.md` — real-LLM execution required for primary evidence on prompt-causal claims.
- `feedback_probe_sprint_shape_for_conditional_broadening.md` — probe sprint shape (Sprint 27 / 29 precedent).
- `feedback_multi_layer_prospective_stanza.md` — two-track stanza shape.
- `feedback_packaging_codex_findings_supersession.md` — delete-and-add supersession pattern for codex-findings at archive.

---

## 8. Reference glossary (terms that appear repeatedly)

- **AMBIGUOUS branch:** the `routingResult.type() == AMBIGUOUS` switch case at `SessionManager.java:185–189`. Discards `ambiguousCandidates()` pre-Sprint-31; Sprint 31 captures it.
- **ROUTED branch:** the `routingResult.type() == ROUTED` switch case at `SessionManager.java:255–259`. Sprint 31 leaves `intakeAmbiguousCandidates` null on this branch per OQ1.
- **Option α / β / γ / δ / ε:** the five candidate data sources from Sprint 30 design pass. Option β is chosen. α = extend `RuntimeIntentClassifier`; γ = new `AlternateUseCaseSurveyor` component; δ = extend `DriftResult`; ε = LLM structured-reflection step.
- **OQ1–OQ5:** five Open Questions from Sprint 30 design doc §8. All pre-picked by deliver-agent in `docs/sprints/sprint-031-objective.md` §5; human reviewed at Sprint 31 objective review.
- **T8:** the non-enforcement integration test (`AgentRunLoopAlternateCandidateUseCasesNonEnforcementIntegrationTest`). Sprint 31 shipped a v1 (one scenario). Fix-iteration #2 strengthens to ≥5 scenarios with invariance assertions.
- **§7.2 worked example:** the hypothetical CaseSpec `cs_example_001` UC-A↔UC-C topic-shift fix sketched in `docs/current/iteration_governance.md` §7.2. The data-source assertion in that example was falsified by Sprint 30 premise check.
- **§13 fix iteration:** Sprint 31 handoff section appended at commit `de47635`. Three smoke reruns + controlled prompt revert/restore. Disambiguates the smoke gap as external LLM provider drift, not Sprint 31 code.
- **Path A / Path B:** Sprint 31 §13.7 dev's two closure recommendations. Path A = close as PASS with external drift accepted (chosen). Path B = block Sprint 31 on a latency-drift mitigation sprint first (rejected by human + deliver-agent).
- **A-with-* classifications:** sprint-close patterns documented in deliver-agent memory.
  - A — clean close (Sprint 24, 28).
  - A-with-Codex-skipped — docs-only exemption (Sprint 16, 22, 26, 27, 30).
  - A-with-fix-iteration-investigation — Sprint 31 path A.
  - A-with-packaging-note — substance closes; packaging artefacts trigger OOSR on path grounds only (Sprint 20).
  - B — fix-iteration-on-code (Sprint 23, 25).
  - C — multi-iteration close.
- **R-items:** Registered action items in `docs/action_bank.md`. Open / proposal / done dispositions. Sprint-close commits flip dispositions.
- **n=N rule:** open-observations need n≥3 instances across ≥2 detection axes to promote to R-item; R-items need n≥2 confirmed observations to trigger (R1) structural-sprint per Sprint 27 / 29 precedent.

---

End of context handoff. The new deliver-agent should be able to resume the cycle from any of: (a) "dev committed, please verify and close" (§6.1 step 3+); (b) "Codex returned, please classify"; (c) "Sprint 31 is fully closed, what's next?" (§2 candidates a/b).
