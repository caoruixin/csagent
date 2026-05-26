# Deliver-agent context handoff — OLD M2-Skill superseded mid-flight + NEW M2 (Skill Registry Abstraction) drafted (NEW Sprint 37 contract + dev prompt pending human commit)

**Authored:** 2026-05-17 by deliver-agent
**For:** the next deliver-agent instance picking up after NEW M2 + NEW Sprint 37 approval bundle is committed (OR helping the human decide pre-commit edits)
**Branch:** `refactor/remove-the-shackles`
**Repo:** `/Users/caoruixin/projects/csagent-latest`
**HEAD:** `ed71031` (Sprint 36 = OLD M2-Skill sub-sprint 1 design freeze; deliver-agent NEW M2 supersession bundle NOT committed yet)

Read order on cold start: this file → `AGENTS.md` (loads `iteration_governance.md` + `doc_governance.md` + `agent_context_guide.md`) → `compact/sprint-deliver-orchestrator.md` (deliver-agent role definition — long-term doctrine; **not duplicated here**) → `docs/milestone_objective.md` (NEW M2 contract — drafted + approved 2026-05-17) → `docs/sprint_objective.md` (NEW Sprint 37 design freeze contract — drafted + approved 2026-05-17) → `compact/sprint-037-dev-prompt.md` (the dev brief that Claude Code will consume) → `docs/milestones/M2-Skill_objective.md` (OLD M2-Skill archive for context on what was superseded) → `docs/proposals/skill_foundation_design.md` "Why superseded" section (OLD Sprint 36 freeze's supersession addendum).

---

## 1. Background (Why this session happened)

Previous deliver-agent session (handoff at `compact/context-handoff-m1-close-m2-planning.md`) closed M1 + drafted M2 framing "Resolution + Escalation + Grounding Discipline" (M2-customer-honesty). At that handoff's authoring time (2026-05-17 morning), M2-customer-honesty was pending human review.

Between that handoff and this session, the human + a prior deliver-agent run made two further moves:

1. **Pivoted M2-customer-honesty → M2-Skill** (Decision β-2026-05-17): the architectural framing in `docs/proposals/skill_orchestration_candidates.md` (Sprint 5 F2 proposal, status `proposal` / `not_started`, never shipped) was promoted to milestone status. M2-customer-honesty's customer-honesty surface deferred to M3-A.
2. **Sprint 36 (M2-Skill sub-sprint 1) shipped** (commit `ed71031`): design freeze for Skill foundation under "minimum-surface incrementalism" framing — reuse PhasePlan + new system_prompt.txt teaching paragraph + new predicate adjacent to existing shouldRejectXxx family. Produced `docs/proposals/skill_foundation_design.md` (~1100 lines). Codex per-sub-sprint review `pass / 0` on first pass.

OLD M2-Skill Sprint 37 contract was drafted at Sprint 36 close (pending human review BEFORE dev session launch), implementing S1 `Resolve.FAQ.GroundedAnswer` per the minimum-surface freeze: ANOTHER teaching paragraph in `system_prompt.txt` + ANOTHER `shouldRejectXxx` method in `AgentRunLoopImpl.java` adjacent to existing Sprint 6/7/11 predicates.

**Where THIS session entered (2026-05-17 afternoon):** the human observed that the OLD M2-Skill framing — despite naming itself "Skill Foundation" — was actually preserving the scattered Zhang-Sanfeng pattern (Sprint 23/31/33 teaching paragraphs continued accumulating side-by-side in system_prompt.txt; Sprint 6/7/11 predicates continued accumulating side-by-side in AgentRunLoopImpl). The human pushed back: "怎么看上去好像还是在小修小补呢？我们需要步子迈得大一些，不要再小修小补了" — and directed the deliver-agent to redirect M2 to a real Skill abstraction with retroactive extraction.

THIS session reframed M2 wholesale to **Skill Registry Abstraction + Wholesale Retroactive Externalization** + drafted NEW Sprint 37 design freeze + applied supersede pattern to OLD M2-Skill artefacts.

---

## 2. Goals

### Long-term

Help the human cycle through milestones in the §8 framework: human gives architectural theme → deliver-agent drafts `docs/milestone_objective.md` + per-sub-sprint `docs/sprint_objective.md` + dev/review prompts → human reviews + approves → dev/review agents execute externally → deliver-agent helps human classify close / targeted-fix / OOSR / next-milestone.

### Immediate (this cycle, NEW Sprint 37 launch round)

1. **Wait for human commit** of the deliver-agent supersession + NEW M2 + NEW Sprint 37 bundle (see §3.8 for file list + suggested commit message).
2. **After commit + dev session launch:** NEW Sprint 37 dev (Claude Code via `compact/sprint-037-dev-prompt.md`) produces `docs/proposals/skill_registry_design.md` (10 design decisions a-j) + 12-section handoff at `docs/sprints/sprint-037-handoff.md` + (conditional) Tier-0 candidate write-up files.
3. **After NEW Sprint 37 dev commit:** deliver-agent + human review Sprint 37 handoff + design doc; deliver-agent drafts `compact/sprint-037-review-prompt.md` + dispatches Codex per-sub-sprint review per §4.3 trigger #1 + #2; deliver-agent + human classify Codex verdict.
4. **After Sprint 37 close:** draft NEW Sprint 38 contract (SkillRegistry core + 4 simpler phase Skills) + dev prompt; surface to human review.

### Subsequent (NEW M2 progression after Sprint 37 closes)

Per `docs/milestone_objective.md` §3 sub-sprint sequence:

- **NEW Sprint 38** — SkillRegistry core (Skill.java + SkillRegistry.java + SkillLoader.java) + 4 simpler phase Skills (DISCOVER + CONFIRM + ESCALATE + TERMINAL) migration; behavioural equivalence verification.
- **NEW Sprint 39** — RESOLVE_FAQ + RESOLVE_INTAKE Skill migration + Sprint 6/7/11 predicate migration into Skill `guardrails` + S1 `must_cite_source` + S2 `intake_complete_required` new predicates + unified Skill terminal-predicate dispatcher (the BIG sub-sprint with multi-fence convergence).
- **NEW Sprint 40** — Teaching extraction from `system_prompt.txt` (Sprint 23 `already_called` + Sprint 31 `alternate_candidate_use_cases` + Sprint 33 `discover_disambiguation_signals`) into corresponding Skill YAML files; `system_prompt.txt` shrunk to orchestration shell.
- **NEW Sprint 41** — UC switch + state preservation across Skill boundary (session-level state-bus + per-Skill `state_inheritance` declaration + `prior_use_case_carry` projection slot).
- **NEW M2 close**: deliver-agent + human dispatch milestone-shared cumulative Codex review at `compact/M2-review-prompt.md`; M2 closure verdict + archive to `docs/milestones/M2_objective.md`; §6.5 row append in `docs/action_bank.md`.

### M3 candidates (informational; not pre-decided)

- M3-A: Customer-honesty surface (carried from OLD M2-customer-honesty + OLD M2-Skill): sibling rationale/confidence fields, handover UX rewrite, URL policy.
- M3-B: Lifecycle (`D-single-handover-orchestrator` P0 + scoped `D-full-issue-ledger` + semantic planner shadow mode).
- M3-C: Sprint 5 S3/S4/S5 skills (rides validated M2 Skill abstraction).
- M3-D: Topic↔UC binding loosening (`R-loosen-topic-uc-binding-llm-owned-drift` requires research-agent investigation).
- M3-Latency: consume `R-llm-provider-latency-drift-2026-05-16` (deferred from M2).
- **M3-Skill-Tuning** (NEW candidate this session): post-M2 behavioural tuning of individual Skills if needed (e.g., Alice closure-criterion (a) PASS if not achieved organically through abstraction landing; UC-switch specific scenarios).
- M3-other: any R-item NEW M2 surfaces.

---

## 3. Confirmed facts (code-grounded, verify before relying)

### 3.1 Git state at supersession authorship (2026-05-17 afternoon)

```
ed71031 sprint 36: Skill foundation + UC-switching continuity design freeze (M2-Skill sub-sprint 1)
eb65e2b sprint 35: Option β coverage probe + §7.2 worked-example re-anchor decision (M1 sub-sprint 3)
e532f0d sprint 34: intake field prefill UC-G/H/I/J (M1 sub-sprint 2)
8a22aa6 sprint 33: DISCOVER UC-A/FP/H soft-signal + classification guidance (M1 sub-sprint 1)
c9edb37 sprint 32: alternate_candidate_use_cases case family (Sprint 31 OQ4 deferral) — M1 base
```

Working-tree modifications at supersession authorship (deliver-agent bundle pending commit):

```
M docs/milestone_objective.md                      (REPLACED with NEW M2 contract; OLD M2-Skill archived elsewhere)
M docs/sprint_objective.md                          (REPLACED with NEW Sprint 37 design freeze contract; OLD Sprint 37 S1 pending contract wholesale replaced)
M docs/10-handoff.md                                (§1 lead refreshed: NEW M2 current; Sprint 36 demoted to "Preceding sub-sprint")
M docs/action_bank.md                               (D-hard-citation-gate + R-grounding-discipline-iterative-search-fabrication updated to reflect NEW M2 carry-forward + OLD M2-Skill supersession)
M docs/proposals/skill_foundation_design.md         (frontmatter status: superseded + superseded_by + new "Why superseded" section added; original Sprint 36 content preserved verbatim below addendum)
M compact/sprint-037-dev-prompt.md                  (REPLACED with NEW Sprint 37 design freeze dev prompt; OLD prompt for S1 implementation per M2-Skill superseded)
?? docs/milestones/M2-Skill_objective.md            (NEW; OLD M2-Skill milestone_objective archive with §0 Supersession verdict + carry-forward inventory + original M2-Skill §1-§11 preserved)
?? compact/context-handoff-m2-skill-supersede-new-m2-skillreg.md  (THIS file)
```

### 3.2 OLD M2-Skill outcome (what shipped before supersession)

- **Only Sprint 36 shipped** (commit `ed71031`; design freeze docs-only; produced `docs/proposals/skill_foundation_design.md`; Codex per-sub-sprint review `pass / 0` on first pass).
- **OLD Sprint 37 (S1 implementation per minimum-surface) contract was drafted but never dev'd** — it was the live `docs/sprint_objective.md` content at session start; THIS session wholesale replaced it with NEW Sprint 37 design freeze contract.
- **OLD Sprints 38 / 39 / 40 never contracted.**
- All Sprint 36 archives stay immutable per `doc_governance.md` sprint-archive rule (`docs/sprints/sprint-036-objective.md` + `docs/sprints/sprint-036-handoff.md` + `docs/sprints/sprint-036-codex-review.md`).

### 3.3 NEW M2 framing (this session's product)

- `docs/milestone_objective.md` carries NEW M2 contract (Skill Registry Abstraction + Wholesale Retroactive Externalization).
- 5 sub-sprints: NEW S37 (design freeze) → NEW S38 (SkillRegistry core + 4 simpler phase Skills) → NEW S39 (RESOLVE_FAQ + RESOLVE_INTAKE + Sprint 6/7/11 predicate migration + S1/S2 new predicates + unified dispatcher) → NEW S40 (teaching extraction from system_prompt.txt) → NEW S41 (UC switch + state preservation).
- **§5 acceptance bar recalibrated per human direction 2026-05-17**: bad-case suite (Alice) + interactive eval composite_score are OBSERVATION only (NOT hard gate) for THIS milestone; primary gate = functional review + Java tests + Sprint 37 freeze decisions honored across implementation sub-sprints. Rationale in §5 with 4 reasons: architecture-focused milestone; eval instability per §5.5; case sparsity; "easier to tune Skill after abstraction lands" principle.
- **§6 hard fence #4** carries OLD M2-Skill §6 #4 verbatim human authorization for S1 `must_cite_source` bounded inversion of `D-hard-citation-gate`. Authorization quote preserved.
- **§6 hard fence #20** (NEW): no deletion of `docs/proposals/skill_foundation_design.md` — supersede pattern only per `doc_governance.md`.
- **§6 hard fence #21** (NEW): M1 FUNCTIONAL behaviour preserved (IntakeFieldExtractor + Sprint71 + Sprint 32/33 projections); individual case behaviour MAY shift per §5 recalibration.
- **§6 hard fence #22** (NEW): Alice case stays as observation case (not gate); closure_criterion may be refined (sharpen, NOT widen per §1.7).
- §6 hard fence #12 EXCEPTION clause: if Sprint 37 surfaces Tier-0 candidate authorized at M2 close, `runtime_freeze_and_risk_policy.md` MAY be edited as SEPARATE deliver-agent commit.

### 3.4 NEW Sprint 37 framing (this session's product)

- `docs/sprint_objective.md` carries NEW Sprint 37 contract (design freeze, docs-only, NO server/ code).
- 10 design decisions (a)-(j): Skill data model; SkillRegistry shape; PhaseEvaluator-as-Selector integration; procedure vs guardrails responsibility split; retroactive migration mapping for ALL 6 phase YAMLs; Sprint 23/31/33 teaching mapping; Sprint 6/7/11 predicate mapping; unified Skill terminal-predicate dispatcher design; session-level state model + state_inheritance semantics; §4.1 anti-hardcode kernel walk.
- Produces: `docs/proposals/skill_registry_design.md` + (conditional) `docs/proposals/tier0_candidate_<name>.md` + `docs/sprints/sprint-037-handoff.md`.
- §7 stanza single-track multi-layer PROSPECTIVE covering Sprints 38/39/40/41 layers.
- Codex per-sub-sprint review at NEW Sprint 37 close per §4.3 trigger #1 (Tier-0 candidate from Skill predicate / tool-whitelist enforcement) + #2 (§1.7 boundary discussion on Skill abstraction shape).

### 3.5 NEW Sprint 37 dev prompt (this session's product)

- `compact/sprint-037-dev-prompt.md` REPLACED with NEW Sprint 37 design freeze dev prompt (11 sections; ~430 lines).
- Includes full quote of user-provided research-agent proposal (3-tier abstraction; 4 orchestration patterns; Skill-as-Prompt rationale; architecture diagram; 3-level branch logic framing; key insight) as architectural foundation for §2-§11 design decisions.
- Includes 5 initial Tier-0 candidates (C1-C5) for dev to evaluate at §4.1 walk + §12 enumeration.
- Includes hard fences (13 items) + stop conditions (12 items) + bundle policy + commit message template.

### 3.6 OLD M2-Skill archive (this session's product)

- `docs/milestones/M2-Skill_objective.md` (NEW file) carries OLD M2-Skill milestone_objective content (preserved verbatim §1-§11 from the M2-Skill draft live in `docs/milestone_objective.md` at moment of supersession) + new §0 Supersession verdict section (carry-forward inventory + NEW M2 reframing summary + sub-sprint sequence + cross-references).
- Frontmatter `status: superseded`; `superseded_by: docs/milestone_objective.md` (NEW M2).

### 3.7 OLD Sprint 36 freeze doc supersede pattern (this session's product)

- `docs/proposals/skill_foundation_design.md` frontmatter updated: `status: superseded`; `superseded_by: docs/proposals/skill_registry_design.md` (to be authored by NEW Sprint 37 dev).
- NEW "Why superseded (2026-05-17)" section added immediately after frontmatter, BEFORE the original "## 1. Purpose + relation to upstream" — explains what the freeze decided + why it was rejected + what carries forward into NEW M2 + what's no longer binding.
- Original Sprint 36 §1-§8 content preserved verbatim below the addendum (~1080 lines unchanged).

### 3.8 Files modified/created in this deliver-agent session (NOT yet committed)

```
M docs/milestone_objective.md
M docs/sprint_objective.md
M docs/10-handoff.md
M docs/action_bank.md
M docs/proposals/skill_foundation_design.md
M compact/sprint-037-dev-prompt.md
?? docs/milestones/M2-Skill_objective.md
?? compact/context-handoff-m2-skill-supersede-new-m2-skillreg.md (THIS file)
```

Per `feedback_commit_at_end_bundles_deliver_artefacts.md`: dev does NOT stage these; human bundles at deliver-agent commit. Suggested commit message in §9 below.

---

## 4. Decision records

### 4.1 Decision γ-2026-05-17 — OLD M2-Skill superseded mid-flight; NEW M2 = Skill Registry Abstraction

Human observed (this session) that OLD M2-Skill, despite naming itself "Skill Foundation", preserved the scattered Zhang-Sanfeng pattern (just adding ANOTHER paragraph + ANOTHER predicate per sub-sprint). Human direction: "步子迈得大一些，不要再小修小补了". Redirect M2 to first-class Skill Registry abstraction with retroactive extraction of existing scattered content.

Deliver-agent surveyed options (with human via AskUserQuestion):
- (a) Skill abstraction first + retroactive extraction — **HUMAN CHOSE THIS**.
- (b) Preserve OLD M2-Skill incremental; add Sprint 41+ retroactive sub-sprint after M2.
- (c) Split into two parallel milestones (Skill-Architecture + Customer-Honesty).
- (d) Human reframes first.

### 4.2 Decision δ-2026-05-17 — OLD Sprint 36 freeze: supersede pattern (not delete)

Human initially asked to delete OLD Sprint 36 freeze doc entirely; deliver-agent pushed back per `doc_governance.md` supersede-not-delete rule. Three options:
- (a) Supersede pattern (preserve doc with status: superseded + superseded_by; add "Why superseded" section) — **HUMAN CHOSE THIS**.
- (b) Explicitly override governance rule + delete.
- (c) Amend `doc_governance.md` to allow delete for wholesale-rejected proposals.

### 4.3 Decision ε-2026-05-17 — NEW M2 acceptance bar recalibration (Alice + interactive eval = observation, NOT gate)

Human direction: "interactive eval目前只用于observation，不作为gate，允许在eval结果上变差，因为eval本身都不稳定 ... 确保有功能的 review和测试通过即可". Recalibration rationale (§5 of NEW M2):

1. Architecture-focused milestone (M2 ships abstraction, not behaviour fix).
2. Eval instability per §5.5 demotion (multiple confounding sources).
3. Single-case sparsity (only Alice in bad_cases/).
4. "Easier to tune Skill after abstraction lands" principle.

Primary gate = functional review + Java tests + Sprint 37 freeze decisions honored. Secondary observations = Alice + interactive eval + architectural-health metrics (allowed to regress).

This is a human-judgment recalibration of `iteration_governance.md` §5.6 "primary gate" framing for THIS milestone. Future milestones (esp. M3-A customer-honesty OR M3-B Single Handover Orchestrator) may revert to bad-case-suite-primary if behaviour-focused.

### 4.4 Decision ζ-2026-05-17 — Sub-sprint sequence: 5 sub-sprints with bigger steps

Human direction: "所有的phase都纳入" + bigger steps + faster replacement of hardcode.

Sub-sprint sequence (NEW M2):
- S37 design freeze (docs-only; 10 decisions a-j).
- S38 SkillRegistry core + 4 simpler phase Skills (DISCOVER + CONFIRM + ESCALATE + TERMINAL) — easier phases first; abstraction validated.
- S39 RESOLVE_FAQ + RESOLVE_INTAKE Skills + Sprint 6/7/11 predicate migration + S1/S2 new predicates + unified dispatcher — the BIG multi-fence sub-sprint.
- S40 Teaching extraction (Sprint 23/31/33 paragraphs) from system_prompt.txt + orchestration shell cleanup.
- S41 UC switch + state preservation across Skill boundary.

Sprint 40 latency parallel-track (originally OLD M2-Skill Sprint 40) deferred to next milestone (orthogonal infra; `R-llm-provider-latency-drift-2026-05-16` preserved in action_bank deferred).

### 4.5 Constitution-discipline check applied this session

Several proposed actions were screened through `feedback_constitution_discipline_vs_planning_anticipation.md` 4-question check:

1. Wholesale-deleting OLD Sprint 36 freeze doc — REJECTED per `doc_governance.md` supersede-not-delete (human re-decided to supersede).
2. NEW M2 §5 acceptance bar recalibration — accepted as human-judgment recalibration of §5.6 primary-gate framing (the human owns judgment per §5.6 lifecycle); documented with rationale; NOT an `iteration_governance.md` edit.
3. NEW Sprint 37 design freeze potentially surfacing Tier-0 candidate — process locked per §6 hard fence #12 EXCEPTION: candidate write-up file separate; `runtime_freeze_and_risk_policy.md` edit only at deliver-agent commit post-human-approval.

---

## 5. Current tasks (what's in flight right now)

### 5.1 Authored and waiting for human commit

- `docs/milestone_objective.md` (NEW M2 contract).
- `docs/sprint_objective.md` (NEW Sprint 37 design freeze contract).
- `docs/10-handoff.md` (§1 lead refreshed).
- `docs/action_bank.md` (D-hard-citation-gate + R-grounding R-item updated).
- `docs/proposals/skill_foundation_design.md` (supersede pattern applied).
- `compact/sprint-037-dev-prompt.md` (NEW Sprint 37 dev prompt).
- `docs/milestones/M2-Skill_objective.md` (NEW; OLD M2-Skill archive).
- `compact/context-handoff-m2-skill-supersede-new-m2-skillreg.md` (THIS file).

All staged but NOT committed. Human bundles per `feedback_commit_at_end_bundles_deliver_artefacts.md`. Suggested commit message in §9 below.

### 5.2 Pending deliver-agent work (after human commit + Sprint 37 dev session)

- (After NEW Sprint 37 dev launches via `compact/sprint-037-dev-prompt.md`): no deliver-agent action; wait for dev commit.
- (After NEW Sprint 37 dev commit): read Sprint 37 dev handoff at `docs/sprints/sprint-037-handoff.md` + design doc at `docs/proposals/skill_registry_design.md` + (conditional) Tier-0 candidate write-up files. Validate 10 design decisions documented; §4.1 walk completed; Tier-0 candidates qualified/rejected; constitution-compliance.
- Draft `compact/sprint-037-review-prompt.md` + dispatch Codex per-sub-sprint review per §4.3 trigger #1 + #2.
- Help human classify Codex verdict at Sprint 37 close.
- If Tier-0 candidate surfaced AND human authorizes addition: commit `runtime_freeze_and_risk_policy.md` §1/§2 addition as SEPARATE deliver-agent commit per `feedback_commit_at_end_bundles_deliver_artefacts.md`.

### 5.3 Pending deliver-agent work (after NEW Sprint 37 closes)

- Draft NEW Sprint 38 contract at `docs/sprint_objective.md` (replacing NEW Sprint 37; archive NEW Sprint 37 to `docs/sprints/sprint-037-objective.md`).
- Draft `compact/sprint-038-dev-prompt.md`.
- Update `docs/action_bank.md` (flip relevant R-items per Sprint 37 close).
- Surface for human review BEFORE NEW Sprint 38 dev session launch.
- Iterate through Sprints 38/39/40/41 per same pattern.

### 5.4 Pending deliver-agent work (NEW M2 close)

- Draft `compact/M2-review-prompt.md` against cumulative NEW M2 commit range (Sprints 37 + 38 + 39 + 40 + 41 commits, IN ADDITION TO per-sub-sprint reviews).
- Help human dispatch Codex; help classify findings.
- Append §12 closure verdict to NEW M2 milestone objective; archive to `docs/milestones/M2_objective.md`.
- Append §6.5 row for NEW M2 in `docs/action_bank.md`.
- Refresh `docs/10-handoff.md` §1 lead.
- Help human pick M3.

---

## 6. Next steps for the next deliver-agent session

When the next deliver-agent instance starts:

1. **Read this file first.** Then `AGENTS.md` + `compact/sprint-deliver-orchestrator.md`.
2. **Verify git state.** Run `git -C /Users/caoruixin/projects/csagent-latest status` + `git log --oneline -10`. Confirm whether the human has committed the deliver-agent bundle from §3.8.
3. **Verify NEW M2 + NEW Sprint 37 state.** Confirm `docs/milestone_objective.md` is NEW M2 (not OLD M2-Skill) and `docs/sprint_objective.md` is NEW Sprint 37 design freeze (not OLD M2-Skill Sprint 37 S1 implementation). If unclear, ask explicitly.
4. **If Sprint 37 dev session has not yet run:** wait for human to launch (via `compact/sprint-037-dev-prompt.md`). No deliver-agent action.
5. **If Sprint 37 dev session has committed:** read Sprint 37 dev handoff + design doc + Tier-0 candidate write-up files; help human classify outcome; draft Sprint 37 Codex review prompt + dispatch; help classify Codex verdict.
6. **If Sprint 37 closes PASS:** draft NEW Sprint 38 contract per `docs/milestone_objective.md` §3 Sprint 38 row + dev prompt; surface for human review.
7. **If Sprint 37 surfaces Tier-0 candidate authorized at close:** commit `runtime_freeze_and_risk_policy.md` §1/§2 addition as SEPARATE commit per `feedback_commit_at_end_bundles_deliver_artefacts.md` BEFORE drafting Sprint 38 contract.
8. **Throughout: respect constitution-discipline.** Re-walk the 4-question check in `.claude/agent-memory/sprint-deliver-orchestrator/feedback_constitution_discipline_vs_planning_anticipation.md` BEFORE committing any edit to `docs/current/iteration_governance.md` / `docs/foundational/*` / `docs/runtime_freeze_and_risk_policy.md`. NEW M2 §6 #12 EXCEPTION clause allows `runtime_freeze_and_risk_policy.md` edit ONLY at Sprint 37 close + human authorization.

---

## 7. Cautions / hard constraints / lessons

### 7.1 NEW M2 §5 acceptance recalibration is THIS milestone only

Bad-case suite (Alice) + interactive eval are OBSERVATION not gate for NEW M2 (architecture-focused). Per `iteration_governance.md` §5.6 + NEW M2 §5 rationale, this is human-judgment recalibration for THIS milestone. Future milestones may revert to bad-case-suite-primary if behaviour-focused. Per-sub-sprint Codex review prompts (S37/S38/S39/S40/S41) MUST explicitly state this recalibration to avoid Codex re-importing the §5.6 default.

### 7.2 Hard fences preserved through NEW M2

- **§6 #4 verbatim authorization for S1 `must_cite_source`** — carried forward from OLD M2-Skill verbatim; bounded scope: RESOLVE_FAQ + `record_outcome(class=resolve)` + `source_id` citation presence; expansion requires new human authorization.
- **No `escalation_reason` enum widening** (`D-new-escalation-reason-enum` deferral preserved; M3-A sibling rationale/confidence fields deferred).
- **No `RuntimeIntentClassifier` / `IntentClassification` / `DriftResult` / `DriftDetector` / `UseCaseRouter` / `ClassifyUseCaseTool` touch** (M3-D Topic↔UC binding deferred to M3+).
- **No `D-single-handover-orchestrator`** (M3-B → M3 unless Salesforce cutover surfaces).
- **No `D-full-issue-ledger`** (hard-deferred per `action_bank.md`).
- **No Tier-0 invariant added** without Sprint 37 freeze pre-authorization + human-review escalation.
- **No edits to existing case families** under `eval_interactive/case_specs/case_families/` (cascade fence from M1).
- **No `iteration_governance.md` edit during NEW M2** (constitution-discipline preserved).
- **No mocked-LLM as primary evidence** for LLM-behaviour claims.
- **No Skill prescribes customer-facing language** (LLM-owned per §1.3).
- **No Skill hard-encodes per-step argument values** (LLM-owned per §1.3).
- **No deletion of `docs/proposals/skill_foundation_design.md`** (OLD Sprint 36 freeze) — supersession pattern only.
- **No reset/migration of M1-shipped FUNCTIONAL surfaces** (IntakeFieldExtractor + Sprint71 + Sprint 32/33 projections) beyond explicit retroactive scope. Individual case behaviour MAY shift per §5 recalibration; FUNCTIONAL behaviour preserved.

### 7.3 NEW Sprint 37 dev session expected outputs

Single dev commit with:
- `docs/proposals/skill_registry_design.md` (NEW; the design freeze doc; ~13 sections covering 10 decisions + Tier-0 enumeration + downstream cross-refs).
- (CONDITIONAL) `docs/proposals/tier0_candidate_<name>.md` (NEW; only if §4.1 walk surfaces qualified Tier-0 candidates).
- `docs/sprints/sprint-037-handoff.md` (NEW; 12 sections per Sprint 31-36 shape).

Dev does NOT stage: `docs/sprint_objective.md`, `docs/milestone_objective.md`, `docs/10-handoff.md`, `docs/action_bank.md`, `docs/codex-findings.md`, `compact/sprint-037-*.md`, anything under `server/`, anything under `eval_interactive/`.

### 7.4 Codex per-sub-sprint review trigger for NEW Sprint 37

§4.3 trigger #1 (possible Tier-0 candidate from Skill predicate / tool-whitelist enforcement semantics) + #2 (§1.7 boundary discussion on Skill abstraction shape). Codex verifies: §4.1 nine-question kernel walk on PROPOSED design; §6 #4 verbatim authorization preservation; no per-UC-branch if-else in PROPOSED Skill bodies; Tier-0 candidate enumeration completeness.

### 7.5 Key memory files (cross-session) in `.claude/agent-memory/sprint-deliver-orchestrator/`

Load all on cold start:
- `feedback_commit_at_end_bundles_deliver_artefacts.md` — dev NOT stage deliver-agent files; human bundles.
- `feedback_handoff_verdict_section_delegation.md` — dev §12 closure verdict delegation.
- `feedback_out_of_scope_review_packaging_rollforward.md` — OOSR-with-packaging-note pattern.
- `feedback_close_with_codex_skipped_docs_only_outcome.md` — A-with-Codex-skipped for docs-only sprints.
- `feedback_corpus_undecidable_premise_check.md` — in-flight downgrade pattern.
- `feedback_deliver_agent_cited_numbers_must_be_reproducible.md` — every number cites source + recipe.
- `feedback_mocked_llm_cannot_prove_prompt_causal_change.md` — real-LLM required for prompt-causal evidence.
- `feedback_probe_sprint_shape_for_conditional_broadening.md` — probe sprint shape.
- `feedback_multi_layer_prospective_stanza.md` — two-track stanza shape.
- `feedback_packaging_codex_findings_supersession.md` — delete-and-add supersession for codex-findings.
- `feedback_constitution_discipline_vs_planning_anticipation.md` — planning anticipation ≠ execution-time authorization for governance-tier edits.

### 7.6 Lessons from THIS session (additions to deliver-agent doctrine, not yet memory files)

- **"Bigger steps" sometimes means abstraction-first instead of incremental.** Don't conflate "minimum-surface" with "good practice"; if the architectural goal is extraction-of-scattered-content, then a minimum-surface freeze that preserves scattering is anti-pattern. Surface the diagnosis to human at planning round; let human decide aggressiveness.
- **Acceptance bar recalibration per-milestone is a legitimate human judgment per §5.6**, but MUST be explicitly documented in the milestone objective AND in per-sub-sprint Codex review prompts (Codex will otherwise re-import §5.6 default and reject milestone close on observation-only metrics).
- **Mid-flight wholesale supersession is messier than fresh milestone.** Original M2-Skill was approved + 1 sub-sprint shipped before supersession; carry-forward inventory must be explicit (which §6 fences preserve; which authorizations carry verbatim; which decisions are reframed). Archive OLD as `docs/milestones/<name>_objective.md` with §0 Supersession verdict; supersede OLD design doc in-place per `doc_governance.md`.

---

## 8. Quick verification checklist for the next deliver-agent

On cold start, run these to confirm state:

```bash
# Verify HEAD + commit graph
git -C /Users/caoruixin/projects/csagent-latest log --oneline -5
# Expected: top commit may be ed71031 (deliver-agent bundle not committed) OR a NEW commit if human bundled

# Verify deliver-agent-owned files state
git -C /Users/caoruixin/projects/csagent-latest status --short docs/milestone_objective.md docs/sprint_objective.md docs/10-handoff.md docs/action_bank.md docs/proposals/skill_foundation_design.md compact/sprint-037-dev-prompt.md docs/milestones/ compact/context-handoff-*

# Verify NEW M2 content (should mention "Skill Registry Abstraction" in title)
head -3 /Users/caoruixin/projects/csagent-latest/docs/milestone_objective.md

# Verify NEW Sprint 37 content (should mention "design freeze" in title)
head -3 /Users/caoruixin/projects/csagent-latest/docs/sprint_objective.md

# Verify OLD M2-Skill archive exists
ls -la /Users/caoruixin/projects/csagent-latest/docs/milestones/M2-Skill_objective.md

# Verify OLD Sprint 36 freeze doc is superseded (frontmatter status: superseded)
head -25 /Users/caoruixin/projects/csagent-latest/docs/proposals/skill_foundation_design.md

# Verify NEW Sprint 37 dev prompt content (should mention "NEW M2 sub-sprint 1" in title)
head -10 /Users/caoruixin/projects/csagent-latest/compact/sprint-037-dev-prompt.md

# Verify action_bank R-grounding-discipline disposition updated
grep -A 2 "R-grounding-discipline-iterative-search-fabrication" /Users/caoruixin/projects/csagent-latest/docs/action_bank.md | head -5
```

If anything is unexpected vs this handoff (e.g., HEAD moved unexpectedly, NEW M2 or Sprint 37 edited post-handoff), pause and re-read state before acting.

---

## 9. Suggested commit message (if human hasn't bundled yet)

```
docs: supersede OLD M2-Skill mid-flight; draft NEW M2 (Skill Registry Abstraction + Wholesale Retroactive Externalization)

OLD M2-Skill framing (Sprint 36 minimum-surface design freeze; OLD
Sprint 37 S1 implementation contract pending) was wholesale superseded
on 2026-05-17 per human direction — the OLD framing preserved the
scattered Zhang-Sanfeng pattern (Sprint 23/31/33 teaching paragraphs +
Sprint 6/7/11 predicates continuing to accumulate side-by-side) that
M2 was supposed to fix. NEW M2 promotes Skill to first-class
abstraction (Skill Registry + externalized YAML/JSON definitions +
PhaseEvaluator-as-Skill-Selector + unified Skill terminal-predicate
dispatcher) with retroactive migration of ALL 6 phase content + scattered
teaching paragraphs + scattered predicates.

- docs/milestone_objective.md: REPLACED with NEW M2 contract (Skill
  Registry Abstraction + Wholesale Retroactive Externalization). 5
  sub-sprints: NEW S37 design freeze → NEW S38 SkillRegistry core +
  4 simpler phase Skills migration → NEW S39 RESOLVE_FAQ + RESOLVE_INTAKE
  + Sprint 6/7/11 predicate migration + S1/S2 new predicates + unified
  dispatcher → NEW S40 teaching extraction from system_prompt.txt →
  NEW S41 UC switch + state preservation.
- docs/sprint_objective.md: REPLACED with NEW Sprint 37 design freeze
  contract (docs-only; 10 design decisions a-j; produces
  docs/proposals/skill_registry_design.md).
- NEW M2 §5 acceptance bar recalibrated 2026-05-17 per human direction:
  bad-case suite (Alice) + interactive eval composite_score are
  OBSERVATION only (NOT hard gate); primary gate = functional review +
  Java tests + Sprint 37 freeze decisions honored across implementation
  sub-sprints.
- docs/milestones/M2-Skill_objective.md: NEW; archives OLD M2-Skill
  milestone_objective with §0 Supersession verdict + carry-forward
  inventory + original §1-§11 preserved verbatim.
- docs/proposals/skill_foundation_design.md: supersede pattern applied
  in-place (status: superseded; superseded_by: docs/proposals/skill_registry_design.md;
  new "Why superseded" section); original Sprint 36 content preserved
  verbatim.
- docs/10-handoff.md: §1 lead refreshed (Current=NEW M2; Preceding
  sub-sprint=Sprint 36 with OLD M2-Skill supersession note; Preceding
  milestone=M1).
- docs/action_bank.md: D-hard-citation-gate + R-grounding-discipline-
  iterative-search-fabrication updated to reflect NEW M2 carry-forward
  + OLD M2-Skill supersession.
- compact/sprint-037-dev-prompt.md: REPLACED with NEW Sprint 37 design
  freeze dev prompt; OLD Sprint 37 S1 implementation prompt superseded.
- compact/context-handoff-m2-skill-supersede-new-m2-skillreg.md: NEW;
  cross-session handoff for next deliver-agent.

Carry-forward into NEW M2 (preserved verbatim where applicable):
§6 #4 verbatim human authorization for S1 must_cite_source bounded
inversion of D-hard-citation-gate; all hard fences on
RuntimeIntentClassifier / DriftDetector / UseCaseRouter / escalation_reason
enum / INTAKE_UCS / D-full-issue-ledger; cascade fence on existing case
families; constitution-discipline; Sprint 36 §1.3 MATERIAL FINDING about
Sprint 11 §M1 precedent.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
```

---

End of context handoff. The next deliver-agent reads this + the role brief + verifies state, then proceeds per §5.2 (Sprint 37 dev result review + Codex dispatch) OR §5.3 (Sprint 38 contract drafting) depending on where the dev session has reached.
