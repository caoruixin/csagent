# Sprint Deliver Orchestrator — Context Compact

## 1. 背景 (Background)

### 1.1 Project

LLM-first customer service agent. The repo's constitution lives at `docs/current/iteration_governance.md` (the LLM-first Constitution). Rules define boundaries; LLM owns semantic understanding. Forbidden: keyword/regex/if-else for semantic decisions; prompt-as-if-else dump; eval override masking bot bug.

### 1.2 Multi-agent collaboration model

```
human            → thoughts, goals, principles, constraints
research agent(s) → propose solutions (cross-checked)
deliver agent (YOU) → plan, sprint split, draft dev/review prompts, judge close/fix
dev agent (Claude Code, separate session) → implementation, tests, eval, handoff
review agent (Codex, separate session) → targeted review, write codex-findings.md
```

Agents do **not** share chat history. Cross-agent context = repo docs only.

### 1.3 Research-agent's overall plan

```
G0  Iteration Governance Lite   → install gate (docs-only)
G1  Human-led Failure Portfolio → 10-20 Failure Briefs from real failures
G2  Interactive Case Family + Shadow Split
G3+ Semantic Planner shadow mode → low-risk live replacement
```

Each gate uses iteration_governance.md §3 Fix Layer Classification + §4 Anti-Hardcode prompt + §5 Eval Acceptance Rules + §6 Architecture-Health Metric definitions.

---

## 2. 目标 (Current goals)

- **Sprint 17 (G0) — DONE**: docs-only governance scaffolding landed (commit `435cd8c`, closed `ac778bc`). Codex pass.
- **Sprint 18 (G1) — Brief authoring DONE, packaging PENDING**: 10 Failure Briefs landed (9 smoke + 1 manual-probe). Now must do G1 packaging (action_bank R-items append + iteration_governance §2 Method note + sprint_objective/handoff write + commit + archive).
- **Sprint 19 (G2) — NOT STARTED**: Interactive case-family + shadow split. Depends on G1 briefs as input.

---

## 3. 已确认事实 (Confirmed facts)

### 3.1 Sprint 17 (G0) state — closed and archived

Commits:

- `435cd8c` — dev agent landed G0.1–G0.4 (iteration_governance.md 6-section bundle + AGENTS.md constitution chain + action_bank §5.1 governance-track backlog + sprint-017-handoff.md)
- `ac778bc` — deliver-agent close-out (archive objective + codex-findings, refresh 10-handoff)

Artifacts:

- `docs/current/iteration_governance.md` (506 lines, 7 sections: Constitution + Failure Brief Template + Fix Layer Classification + Anti-Hardcode Review Prompt + Eval Acceptance Rules + Architecture-Health Metric definitions + Required sprint-objective stanza)
- `AGENTS.md` (Option A constitution chain — `@docs/current/iteration_governance.md` + `@doc_governance.md` + `@agent_context_guide.md`)
- `docs/sprints/sprint-017-iteration-governance-lite-objective.md` (archived)
- `docs/sprints/sprint-017-handoff.md` (dev agent handoff)
- `docs/sprints/sprint-017-codex-review.md` (Codex pass, decision: pass, blocking_count: 0)

Codex Sprint 17 review: 7 review questions all pass. One P3 informational note: `runtime_freeze_and_risk_policy.md` uses "hard invariants" not the literal word "Tier-0" — not blocking, future fold-back.

### 3.2 Sprint 18 (G1) — 10 briefs landed at `docs/diagnostics/failure-briefs/`

```
cs001-uc-c-template-escalate-on-faq-miss.md                          69 lines
cs011-uc-d-detailed-description-ignored-on-faq-miss.md               76 lines
cs015-uc-fp-mis-route-and-premature-escalate.md                      75 lines
cs038-uc-j-intake-redundancy-and-jargon-framing.md                   86 lines
cs040-uc-k-disengaged-jargon-intake-false-complete.md                95 lines
cs095-uc-classification-and-account-aware-path-skipped.md           125 lines
cs176-uc-e-wrong-escalation-reason-family.md                         96 lines
cs192-uc-b-mechanical-escalate-on-resolvable-giveaway-question.md   101 lines
cs259-uc-f-sprint7-i0-violation-on-payment-question.md              108 lines
manual-probe-2026-05-13-ad-visibility-multi-layer-failure.md        119 lines
```

Total: 950 lines, ~123KB. Filename convention α (case_id prefix + slug); manual-probe uses `manual-probe-<date>-<slug>`.

### 3.3 Brief structure (per `iteration_governance.md` §2)

Every brief has 6 fields:

1. What happened? (concrete observed bot behaviour)
2. What should a good CS agent have done? (user perspective; capability-only, NOT prescriptive bot logic)
3. Why does this matter? (impact + Constitution clause)
4. Is this a one-off or a pattern? (`one-off` / `pattern` / `unknown` + evidence)
5. Which layer is likely responsible? (one of: infra, java_guard, prompt_projection, skill_state, semantic_planner, eval_spec, product_policy, judge_calibration, human_review_required)
6. What should NOT be done? (anti-hardcode guardrail)

Plus header metadata: source case, source_session_id, CaseSpec path, approved L3 override status, runs, filed date.

Plus optional "Ground-truth chain" preamble before the 6 fields (added during cs_015 brief; documents Wave A5/A6 L3 override status + classifies CaseSpec authority).

Plus optional "Related observation" section after the 6 fields (for tangential phase 2 / eval_spec / corpus / R-item findings).

### 3.4 The smoke regression (Cluster C, deferred)

- 2026-05-05 (`label sprint8-r2`): 9/14 pass (64%)
- 2026-05-10 (`label smoke_rerun_20260510-214558`): 3/14 pass (21%)
- 6 cases regressed (cs_002, cs_011, cs_014, cs_038, cs_040, cs_066) — symptoms: empty escalation_reason, STALL:PLACEHOLDER_WITHOUT_FOLLOWUP, turn_budget_exhausted, UC mis-route, CONTRACT_VIOLATION:active_use_case
- Sprints 14/14.1/15/16 all declared "no runtime semantic change" — so this is not a documented intent
- **R-smoke-regression-investigation** (P1, must resolve before G2)
- Cluster C is deferred from G1 briefs per framing C decision

### 3.5 CaseSpec generation pipeline (project memory exists at `~/.claude/projects/-Users-caoruixin-projects-csagent/memory/project_casespec_override_pipeline.md`)

3-layer pipeline (per `docs/proposals/interactive_case_spec_generation_plan.md`):

- L1: deterministic rule extraction
- L2: bounded LLM persona reviewer (DeepSeek v4 Pro, persona fields only)
- L3: human-approved overrides in `eval_interactive/case_spec_overrides.yaml`, schema v2, keyed by `source_session_id` (NOT case_id)

L3 overrides may have `classification` / `expected` / `persona` blocks. Approved override with `migrated_from_legacy: true` allowed empty `supporting_turn_numbers`.

### 3.6 Cluster taxonomy (used in briefs)

- **Cluster A**: persistent failures (both 2026-05-05 and 2026-05-10 runs FAIL) — cs_015, cs_095, cs_176, cs_192, cs_259
- **Cluster B**: PASS by outcome but mechanical surface (L3 relevance/tone ≤ 2.0) — cs_001, cs_002, cs_011, cs_014, cs_038, cs_040, cs_066
- **Cluster C**: regression-only failures (only fail in 2026-05-10) — deferred via R-smoke-regression-investigation

cs_192 is technically the intersection of Cluster A and B (same template surface as B, outcome fail like A) — user said don't make this cross-cluster observation explicit in brief; cs_192 counts as Cluster A.

Cluster B has 3 sub-patterns observed during briefs (not formally taxonomized, but real):

- B-1: disengaged turn-0 template-escalate (cs_001, cs_011)
- B-2: engaged-but-mechanical (cs_038)
- B-3: disengaged + jargon-framing hybrid (cs_040, cs_066)

### 3.7 phase 2 §2.10 line 358 — get_customer_context allowed list

```
get_customer_context (限 UC-A/UC-FP/UC-K，对 UC-H 不可，因此仅靠 user_message 提问收集)
```

UC-B, UC-C, UC-D, UC-F, UC-J, UC-E excluded. CaseSpec generator includes `get_customer_context` in `expected_tool_sequence` regardless — systematic generator-vs-policy mismatch confirmed across cs_001 (UC-C), cs_011 (UC-D), cs_259 (UC-F) — 3 instances, 3 UCs.

### 3.8 Phase 5 evaluation_design line 951

Global L1 check `no_human_only_tool_exposure` (Wave B1.2): block-list `[moderation_enforcement_action, send_followup_email_or_async_update]`. Zero-tolerance, not per-case.

### 3.9 Working tree state (uncommitted)

After commit `ac778bc` (Sprint 17 close-out):

```
D docs/diagnostics/codex-findings.md   ← unexplained deletion; NOT caused by deliver or dev agent
```

Per "Executing actions with care": deliberately NOT staged this deletion. Surface to human if they ask; otherwise leave.

### 3.10 docs/codex-findings.md (top-level)

Contains Sprint 17 review content (decision: pass, blocking_count: 0). Until Codex runs again for a new sprint, this stays. There is **no Codex review for Sprint 18 (G1)** because Q3 = option 2 (no dev agent, no Codex review for G1 — deliver agent writes directly).

---

## 4. 决策记录 (Decision records — chronological)

### 4.1 Sprint 17 (G0) decisions

- **AGENTS.md gap**: Option A (seed AGENTS.md with constitution chain) — chosen over Option B (move includes to CLAUDE.md). Recommended by deliver, approved by human.
- **iteration_governance.md**: single file, not split into 5 files. Recommended, approved.
- **Sprint name**: "Sprint 17 — Iteration Governance Lite (G0)". Approved.
- **Phase 3 fold-back**: deferred (user had unstaged `docs/foundational/phase3_detailed_technical_design.md` modifications which were noted but excluded from G0 scope).

### 4.2 Sprint 18 (G1) framing decisions

- **Framing C** chosen (over A "all 10–20 briefs from both runs" and B "G0.5 regression investigation first"): narrow G1 to 5 persistent (Cluster A) + 4 mechanical (Cluster B representatives) + 1 manual probe = 10 briefs. Regression cluster (Cluster C) deferred via `R-smoke-regression-investigation`.
- **Co-author authorship**: human + deliver agent jointly in chat. Brief content is human-led capability judgment.
- **Cluster B representative selection**: 4 picks (B1 cs_001 canonical disengaged-template; B2 cs_011 user-detail-ignored extreme; B3 cs_038 mis-framing + made-up-name; B4 cs_040 jargon + disengaged hybrid). cs_002 / cs_014 / cs_066 deferred to G2 as neighbor cases.

### 4.3 Workflow / convention decisions

- **Q2 filename convention α**: `<case_id>-<uc>-<slug>.md`. Manual probe: `manual-probe-<date>-<slug>.md`.
- **Q3 packaging workflow option 2**: deliver agent writes briefs directly to `docs/diagnostics/failure-briefs/`. **No dev agent for G1.** **No Codex review for G1.** Deliver agent + human are the authors and reviewers.
- **Q4 brief order**: B1 (cs_001) → A1 (cs_015) → human's choice. User changed first to cs_015. Actual order: cs_015 → cs_001 → cs_011 → cs_038 → cs_040 → cs_095 → cs_176 → cs_192 → cs_259 → manual-probe-2026-05-13.

### 4.4 Co-author per-brief decisions (key)

- **cs_015**: Ground-truth chain preamble added (documents Wave A6 L3 override). Layer = prompt_projection primary + semantic_planner secondary; `product_policy` moved to Related observation (UC-B get_customer_context restriction). New backlog: `R-uc-b-customer-context-policy-review`.
- **cs_001**: No L3 override; CaseSpec has internal inconsistency (`escalation_trigger: clarification_budget_exhausted` + `intake fields (none)`). Two R-items: `R-cs001-escalation-trigger-l3-review` + `R-cs001-uc-c-customer-context-policy-conflict` (later broadened).
- **cs_011**: Has Sprint 4 §E1 override (escalation_trigger pinned). Brief broadened R-item: `R-cs001-uc-c-customer-context-policy-conflict` → `R-generator-get-customer-context-policy-mismatch` (cs_001 UC-C + cs_011 UC-D systematic). Retroactive edit applied to cs_001.
- **cs_038**: First R-item for L3 judge: `R-l3-judge-form-context-trust-rubric` (judge over-reaches by criticizing form-supplied first_name "Paul"). Plus `R-cs038-l3-review-intake-efficiency`.
- **cs_040**: No L3 override; "Mo" name from form_context (3 fields concerns raised: outcome_class confirmed escalate, send_followup_email_or_async_update verified absent, persona goal_summary scope). First Tier-0 candidate `R-intake-complete-runtime-contract-review` (later broadened). Plus `R-cs040-l3-review-intake-completion-semantics`, `R-persona-goal-summary-scope-clarity` (conditional).
- **cs_095**: Has Wave A2.1 legacy classification-only override (UC-A primary, but human review says PRD/Eval = UC-D; substantive mismatch). 5 R-items: `R-cs095-uc-classification-l3-rereview`, `R-corpus-uc-d-account-faq-gap` (later broadened), `R-faqMissCount-threshold-and-timing-review`, `R-l1-source-citation-quality-rubric`, `R-g2-multi-turn-followup-case-family-design`. Method note discovered during this brief (later proposed for iteration_governance.md §2). "What should have done" rewritten to be capability-only (not 5-step prescriptive).
- **cs_176**: No L3 override; cross-family escalation_reason mismatch. Broadened cs_040's R-item: `R-intake-complete-runtime-contract-review` → `R-escalation-reason-runtime-evidence-contract-review` (cs_040 + cs_176 systematic). Retroactive edit applied to cs_040. New: `R-cs176-escalation-reason-l3-review`, `R-duplicated-greeting-projection-fix` (cs_095 + cs_176).
- **cs_192**: No L3 override; first case where Cluster B mechanical surface produces outcome FAIL. **User said NO to cross-cluster meta-observation** — kept cluster classification pure (cs_192 = Cluster A). Broadened cs_095's R-item: `R-corpus-uc-d-account-faq-gap` → `R-corpus-coverage-audit-per-uc` (cs_095 + cs_192). Retroactive edit applied to cs_095. New (low-priority): `R-cs192-secondary-ucs-duplicate-uc-b`.
- **cs_259**: No L3 override. Bot **violated Sprint 7 §I0** weak-candidate cue (explicit phase-plan instruction in DISCOVER system_instruction). **User said NO to opening R-prompt-phase-plan-directive-followship** — pending more controlled testing across UC-B / UC-C / UC-D / UC-F empty-form shapes. Demoted to "open observation, not R-item". Third instance of `R-generator-get-customer-context-policy-mismatch` (cs_001 + cs_011 + cs_259).
- **manual-probe 2026-05-13**: NEW R-item `R-runtime-orchestrator-tool-call-deduplication` (infra layer; 1 LLM request → 3 search_knowledge executions, identical params/results). Third instance of `R-escalation-reason-runtime-evidence-contract-review` (cs_040 + cs_176 + manual-probe) — now solidly systematic Tier-0 candidate. Second instance of "phase-plan-directive-followship" open observation (still not R-item). `R-ad-id-form-vs-listing-data-consistency` flagged but NOT opened on n=1.

### 4.5 User-imposed rules / preferences (apply going forward)


| Rule                                                                                                                                                | Source                                      | Applies to                                                                                                |
| --------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------- | --------------------------------------------------------------------------------------------------------- |
| n=1 evidence insufficient to open an R-item; controlled multi-shape testing needed                                                                  | cs_259 decision                             | Future "open observations" — don't elevate to R-items without ≥2 confirming instances OR controlled tests |
| "条件升级 (conditional broadening)": when 2+ instances confirm a pattern, broaden per-case R-item to systematic; retroactively rename in earlier briefs | cs_001+cs_011, cs_040+cs_176, cs_095+cs_192 | Apply to any future R-item that gets a second instance                                                    |
| Briefs are capability-only (don't prescribe specific bot logic / step-by-step)                                                                      | cs_095 decision                             | All briefs                                                                                                |
| Don't make cross-cluster meta-observations explicit; keep cluster classification pure                                                               | cs_192 decision                             | All briefs                                                                                                |
| Filenames α convention (case_id-uc-slug or manual-probe-date-slug)                                                                                  | Q2 decision                                 | All briefs                                                                                                |
| Co-author flow: draft in chat → human reviews → write to disk                                                                                       | Q3 option 2                                 | G1 briefs (and possibly future deliver outputs)                                                           |
| No mention of date changes ("date has changed") in user-visible text                                                                                | system-reminder behavior                    | All responses                                                                                             |


---

## 5. 当前任务 (Current task) — G1 PACKAGING

### 5.1 Brief authoring: DONE (10/10)

### 5.2 G1 packaging (task #16) — PENDING — 7 STEPS

Pending sign-off from human on 3 quick-checks (Q1 packaging now vs later / commit shape; Q2 sprint_objective retrospective style; Q3 Method note placement in §2). See `Section 7. 下一步` for the questions to surface.

**Packaging step list (in execution order once approved):**

1. **Append R-items + open observations to `docs/action_bank.md` deferred backlog.** Use a new sub-section like "§5.2 G1 surfaced backlog" or extend existing §4 / §5.1. **18 R-items** total (full list in §6 below). Plus 2 open observations not opened on n=1 (phase-plan-directive-followship; ad_id form-vs-listing data consistency).
2. **Add "Method note for G1 briefs" to `docs/current/iteration_governance.md` §2 explanation** (after the example brief, before §3). Method note content:
  - Before filing a Failure Brief, check `eval_interactive/case_spec_overrides.yaml` for an approved L3 override by `source_session_id`. If yes, anchor on the override as ground truth. If no, the CaseSpec may need L3 triage via Wave A5/A6 — flag the failure as a potential `eval_spec` candidate per §3.2 Q6.
  - For manual-probe traces (no CaseSpec), derive ground truth from authoritative authoring sources: phase 2 policy + bot's own `phase_plan` `system_instruction` text. Document the derivation chain in the brief's Ground-truth chain preamble.
3. **Update `docs/action_bank.md` §5.1 G1 row** to `done` with brief counts: "G1 Human-led Failure Portfolio — done, 10 briefs filed (9 smoke + 1 manual-probe). See docs/diagnostics/failure-briefs/."
4. **Replace `docs/sprint_objective.md` with Sprint 18 G1 wrap-up** (retrospective writing, since briefs already landed). User confirmed retrospective style OK pending re-check. Should cover: sprint name, goal, deliverables done (10 briefs), what was NOT implemented per framing C, success metrics, link to handoff.
5. **Write `docs/sprints/sprint-018-handoff.md`** with full G1 handoff: brief table, R-item list, layer-summary, regression finding, methodology notes, next-sprint recommendation (G2). Aim for ~150–250 lines.
6. **Commit** — single commit OR split into 2 commits (deliver agent recommended: option A = single commit "docs: close sprint 18 g1 — file 10 failure briefs + r-items + method note"; option B = split into (a) docs updates and (b) archive copies). User to decide.
7. **Archive**: copy `docs/sprint_objective.md` → `docs/sprints/sprint-018-g1-failure-portfolio-objective.md`; the handoff itself is already at `docs/sprints/sprint-018-handoff.md` (per the Sprint 16 pattern). NO Codex review file for G1 (no review was run).

### 5.3 Tasks task list (in this session — for reference)

- #10 Sprint 17 close: archive objective + codex review — COMPLETED
- #11 Update docs/10-handoff.md to lead with Sprint 17 — COMPLETED
- #12 Close-out commit: Sprint 17 archival + running files — COMPLETED
- #13 Read both results.json fully to extract G1 candidate failures — COMPLETED
- #14 Draft Sprint 18 (G1) plan + sprint_objective + prompts — DELETED (superseded by option 2 workflow)
- #15 Co-author and write 10 G1 briefs — COMPLETED
- #16 G1 final packaging: action_bank + sprint_objective + handoff + commit — PENDING (this is the current focus)

---

## 6. R-items 完整列表 (18 R-items + 2 open observations to package)

### 6.1 Tier-0 candidates (highest priority)


| R-item                                                 | Source briefs                                                                                                   | Description                                                                                                                                                                                                                                                                                                                                       |
| ------------------------------------------------------ | --------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `R-escalation-reason-runtime-evidence-contract-review` | cs_040 (UC-K, intake_complete) + cs_176 (UC-E, faq_miss) + manual-probe (UC-A, faq_miss despite faq_miss=false) | Solidly systematic — 3 instances. Should the runtime enforce that escalation_reasons claiming session events (`*_complete_`*, `*_threshold_exceeded`, `clarification_budget_exhausted`, etc.) require corresponding event evidence? Scope: evidence-claiming subset only (NOT `user_requested` / `user_distress` which have their own contracts). |


### 6.2 Systematic (≥2 instances confirmed)


| R-item                                             | Source briefs                                                                           | Description                                                                                                                                                             |
| -------------------------------------------------- | --------------------------------------------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `R-generator-get-customer-context-policy-mismatch` | cs_001 (UC-C) + cs_011 (UC-D) + cs_259 (UC-F)                                           | 3 UCs. CaseSpec generator includes `get_customer_context` in `expected_tool_sequence` regardless of phase 2 §2.10 line 358 restrictions. Wave A5/A6 review priority.    |
| `R-l3-judge-form-context-trust-rubric`             | cs_038 (Paul) + cs_040 (Mo) + cs_192 (Rita)                                             | L3 judge over-reaches by criticizing form-supplied first_name as "without confirmation". The rubric should account for `form_context.first_name` as a trustable signal. |
| `R-corpus-coverage-audit-per-uc`                   | cs_095 (UC-D account/email) + cs_192 (UC-B free-items/giveaway) + cs_259 (UC-F payment) | 3 UCs. Per-UC FAQ corpus coverage audit. Resolve-grade articles needed for each UC's common entry-point questions. NOT generator-synthesized.                           |
| `R-faqMissCount-threshold-and-timing-review`       | cs_095 + cs_192 + cs_259                                                                | Two-part: (a) `>= 2` threshold given auto-search burns one; (b) threshold check timing vs form-description fallback. Config governance, not runtime semantic change.    |
| `R-duplicated-greeting-projection-fix`             | cs_095 ("Hi Trish! Hi Trish") + cs_176 ("Hi Gary! Hi Gary")                             | Single projection / template-rendering bug rendering greeting twice.                                                                                                    |


### 6.3 Per-case L3 / governance


| R-item                                          | Source brief | Description                                                                                                                     |
| ----------------------------------------------- | ------------ | ------------------------------------------------------------------------------------------------------------------------------- |
| `R-uc-b-customer-context-policy-review`         | cs_015       | Phase 2 line 358 restricts `get_customer_context` to UC-A/UC-FP/UC-K. Should UC-B (Posting & Editing) be added?                 |
| `R-cs001-escalation-trigger-l3-review`          | cs_001       | Trigger `clarification_budget_exhausted` conflicts with `intake fields (none)`. Wave A5/A6 review.                              |
| `R-cs038-l3-review-intake-efficiency`           | cs_038       | Should intake completion at T2 be the correct `turn_efficiency` target?                                                         |
| `R-cs040-l3-review-intake-completion-semantics` | cs_040       | Should `escalation_reason=intake_complete_for_uc_k` + `intake_fields_collected=0` be a hard outcome fail?                       |
| `R-persona-goal-summary-scope-clarity`          | cs_040       | **Conditional** — only open if G2 case-family construction shows the same scope-broadening pattern on other personas.           |
| `R-cs095-uc-classification-l3-rereview`         | cs_095       | PRD/Eval = UC-D vs CaseSpec override = UC-A. Wave A5/A6 L3 re-review. **Most impactful follow-up** for cs_095.                  |
| `R-l1-source-citation-quality-rubric`           | cs_095       | L1 `source_citation_present` accepts internal SF IDs (`ka44J000000gKxqQAE`). Tighten to require canonical_url OR article title. |
| `R-cs176-escalation-reason-l3-review`           | cs_176       | Is `user_requested` the optimal expected reason for UC-E refund demand, or should UC-E have a more specific reason?             |
| `R-cs192-secondary-ucs-duplicate-uc-b`          | cs_192       | Low priority. CaseSpec lists UC-B both as primary and as secondary_ucs. Generator quirk.                                        |


### 6.4 G2 input / future-input


| R-item                                        | Source brief | Description                                                                                                                                |
| --------------------------------------------- | ------------ | ------------------------------------------------------------------------------------------------------------------------------------------ |
| `R-g2-multi-turn-followup-case-family-design` | cs_095       | G2 case-family construction should intentionally include multi-turn-followup cases on FAQ-resolve UCs to exercise the skill_state surface. |


### 6.5 New infra (from manual-probe)


| R-item                                           | Source brief            | Description                                                                                                                                                                                                                                                                                   |
| ------------------------------------------------ | ----------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `R-runtime-orchestrator-tool-call-deduplication` | manual-probe 2026-05-13 | `infra` layer per §3.2 Q1. 1 LLM request → 3 identical search_knowledge executions. Investigate root cause among 3 hypotheses (phase transition re-trigger, Turn 1 failed-call replay, LLM new request). Document orchestrator's intended de-dup / idempotency contract. Add regression test. |


### 6.6 External / regression discovery


| R-item                             | Source                                             | Description                                                                                                                                                                                              |
| ---------------------------------- | -------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `R-smoke-regression-investigation` | 2026-05-05 → 2026-05-10 smoke run drop (64% → 21%) | **P1, must resolve before G2** otherwise G2 case-family construction has no clean baseline. 6 cases regressed; Sprints 14/14.1/15/16 declared no-runtime-semantic-change but trace evidence contradicts. |


### 6.7 Open observations (NOT opened on n=1; track only)


| Observation                                | Source                                                                                       | Why not R-item                                                                                                                                                                                          |
| ------------------------------------------ | -------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Bot ignores explicit phase-plan directives | cs_259 (Sprint 7 §I0 violation) + manual-probe (RESOLVE MUST-call-resolve_article violation) | 2 opportunistic observations, but user said controlled multi-shape testing needed across UC-B/C/D/F empty-form shapes. **Do NOT open `R-prompt-phase-plan-directive-followship` without that testing.** |
| ad_id form-vs-listing data consistency     | manual-probe (form ad-1003 vs listing AD-1001)                                               | n=1; needs production data to know if common shape. Not opening on n=1.                                                                                                                                 |


---

## 7. 下一步 (Next steps)

### 7.1 Before any packaging — surface 3 quick checks to human

1. **Packaging timing**: do it now in one go, OR let human review the 10 briefs first?
2. **Sprint 18 `sprint_objective.md` retrospective style**: confirmed once; re-confirm before drafting since it's unusual.
3. **Method note placement**: append to `iteration_governance.md` §2 explanation as a sub-paragraph after the example brief (not a new section).

### 7.2 Then execute 7 packaging steps (per §5.2)

Single commit recommended. Diff shape:

- `docs/action_bank.md` — append 18 R-items + 2 open observations; update §5.1 G1 row to done
- `docs/current/iteration_governance.md` — append Method note paragraph to §2
- `docs/sprint_objective.md` — replace Sprint 17 content with Sprint 18 G1 wrap-up
- `docs/sprints/sprint-018-handoff.md` — NEW file (G1 handoff)
- `docs/sprints/sprint-018-g1-failure-portfolio-objective.md` — NEW file (archive copy of the new sprint_objective)
- NOT `docs/codex-findings.md` (no Codex review for G1)

### 7.3 After G1 packaging — next sprint candidates (in priority order)

1. **R-smoke-regression-investigation** (P1, must resolve before G2)
2. **Sprint 19 G2** (Interactive Case Family + Shadow Split) — depends on (1) being resolved
3. OR a small targeted probe sprint: empty-form + FAQ-shaped messages across multiple UCs (UC-B / UC-C / UC-D / UC-F) to test phase-plan-directive-followship pattern. Could be done in parallel with (1).
4. Wave A5/A6 L3 review batch: address per-case L3 R-items (cs_001, cs_038, cs_040, cs_095, cs_176, cs_192) and the systematic CaseSpec generator mismatch (`R-generator-get-customer-context-policy-mismatch`).
5. Single Handover Orchestrator runtime sprint (deferred from Sprint 16 — only trigger when real Salesforce cutover staged OR real-traffic duplicate handover routing appears).

---

## 8. 注意事项 (Notes / hard rules)

### 8.1 Deliver-agent role boundaries

- **DO**: plan sprints, draft sprint_objective.md, draft dev/review prompts, judge close/fix/defer, help human navigate trade-offs, surface decisions before acting.
- **DO NOT**: write business code, run reviews, replace dev agent, replace review agent, silently expand sprint scope without human review.
- **G1 exception**: deliver agent wrote briefs directly to `docs/diagnostics/failure-briefs/` per Q3 option 2. This is a G1-specific carve-out — NOT a general license. For G2 and beyond, restore the standard "deliver plans + drafts; dev executes" boundary unless human re-decides.

### 8.2 Anti-hardcode rules (apply to every brief and every fix proposal)

- Never propose a regex / keyword / if-else / enum extension / per-UC matrix for a semantic decision.
- Never suggest filling corpus gaps with generator-synthesized articles (only genuine help-center content).
- Never edit a CaseSpec or override to mask a real bot bug (Eval acceptance rule).
- Never bypass the Wave A5/A6 L3 review by patching `expected.`* fields inline in CaseSpec YAML.
- All sprint_objective.md files that touch semantic surfaces must include the "Layer classification + anti-hardcode stanza" required by `iteration_governance.md` §7 (Pure infra/docs/config-governance/characterization-test sprints are exempt).

### 8.3 Sprint 17 / G0 governance bundle (use these as the gate)

- §3 Fix Layer Classification — 7-question first-match-wins checklist + judge-stability tail rule.
- §4 Anti-Hardcode Review Prompt — 9 questions + 4 verdicts (`approve` / `approve with downgrade-to-signal follow-up` / `reject as semantic hardcode` / `needs human architecture decision`).
- §5 Eval Acceptance Rules — target / neighbor / negative / shadow + safety / grounding / wrong-containment / over-escalation floors. Visible-eval improvement alone is INSUFFICIENT when shadow regresses.
- §6 Architecture-Health Metric definitions (collection_status: not_started for all 4): `new_semantic_hardcode_count`, `soft_signal_conversion_count`, `planner_ownership_ratio`, `shadow_disagreement_rate`.
- §7 Required sprint-objective stanza — applies to semantic-touching sprints.

### 8.4 Important file paths to remember

```
docs/current/iteration_governance.md             — 7-section G0 bundle (Method note pending in §2)
docs/current/doc_governance.md                   — tier model, fold-back cadence, Claude/Codex roles
docs/current/agent_context_guide.md              — per-task reading lists + Context Pack Prompt
docs/proposals/interactive_case_spec_generation_plan.md  — Wave A5/A6/A6.6 pipeline
docs/foundational/phase0_normative_freeze.md    — human-only tools §61
docs/foundational/phase2_domain_realization_spec.md     — UC definitions, tool policies (§2.10 line 358 critical)
docs/foundational/phase5_evaluation_design.md   — line 951 no_human_only_tool_exposure check
docs/runtime_freeze_and_risk_policy.md           — Tier-0 invariants (uses "hard invariants" not literal "Tier-0")
docs/release_gate.md                             — release gates (Single Handover Orchestrator block)
docs/action_bank.md                              — current ledger, §3 active / §4 deferred / §5.1 governance / §6 closed index
docs/sprint_objective.md                         — running current sprint (Sprint 17 G0 currently; replace with Sprint 18 G1 during packaging)
docs/codex-findings.md                           — running latest review (Sprint 17 G0 pass currently)
docs/10-handoff.md                               — running current handoff (Sprint 17 G0 lead currently)
docs/sprints/sprint-017-*.md                     — Sprint 17 archive (objective, handoff, codex-review)
docs/sprints/sprint-018-handoff.md               — will be created during packaging
docs/sprints/sprint-018-g1-failure-portfolio-objective.md  — will be created during packaging (archive copy)
docs/diagnostics/failure-briefs/*.md             — 10 G1 briefs (new dir, just populated)
docs/diagnostics/codex-findings.md               — uncommitted DELETION in working tree (NOT staged by deliver)
eval_interactive/case_specs/smoke/cs_interactive_*.yaml   — CaseSpecs
eval_interactive/case_spec_overrides.yaml        — L3 approved overrides (schema v2, keyed by source_session_id)
eval_interactive/results/20260505-235231/results.json   — pre-regression smoke run (9/14 pass)
eval_interactive/results/20260510-134558/results.json   — post-regression smoke run (3/14 pass)
AGENTS.md                                        — repo constitution chain (Option A includes)
CLAUDE.md                                        — Claude Code overlay (`@AGENTS.md` include)
~/.claude/projects/-Users-caoruixin-projects-csagent/memory/MEMORY.md   — auto-memory index (3 entries)
~/.claude/projects/-Users-caoruixin-projects-csagent/memory/project_casespec_override_pipeline.md   — CaseSpec L3 pipeline reference memory
.agent-prompts/sprint-017-g0-*.md                — Sprint 17 dev/review prompt drafts (kept as paper trail)
```

### 8.5 Memory entries (already saved; check before duplicating)

In `~/.claude/projects/-Users-caoruixin-projects-csagent/memory/MEMORY.md`:

- `feedback_cs_agent_posture.md` — CS agent should be human-flexible, not mechanical (avoid mechanical risk-keyword escalation; favor L1/L2 constrained continuation over auto-handover).
- `feedback_doc_governance.md` — Code is truth; fold back foundational docs every 3–5 sprints.
- `project_casespec_override_pipeline.md` — 3-layer CaseSpec pipeline; check L3 overrides by `source_session_id` before treating CaseSpec as authority.

### 8.6 Git status as of this compact

```
Current branch: design-v1-without-human-review
Recent commits:
  ac778bc docs: close sprint 17 g0 - archive objective/codex, refresh 10-handoff
  435cd8c docs: land sprint 17 g0 iteration governance bundle
  d928c52 add track
  9ce80f5 docs: clarify admin and production readiness gaps
  3d3cb13 docs: fix directory reorg links and layout references
  fef7c0a docs: add current runtime and tool contract docs
  434ee26 docs: close handover exactly-once contract sprint 16

Working tree:
  D docs/diagnostics/codex-findings.md   ← pre-existing, NOT caused by deliver
  ?? docs/diagnostics/failure-briefs/    ← 10 G1 briefs (untracked, pending G1 packaging commit)
```

---

## 9. 启动 checklist for new session

When resuming as deliver-agent in a fresh conversation:

1. Read this compact end-to-end.
2. Read `docs/current/iteration_governance.md` §3 (Fix Layer Classification) and §4 (Anti-Hardcode Review Prompt) — these are your daily-use gates.
3. Verify working tree state: `git status --short` should match §8.8 (or have moved forward consistently).
4. Verify briefs landed: `ls docs/diagnostics/failure-briefs/` should show 10 files.
5. Read `docs/sprint_objective.md` to see current sprint (likely still Sprint 17 G0 if packaging hasn't run; will be Sprint 18 G1 after packaging).
6. Acknowledge the 3 quick-checks pending in §7.1 if human hasn't answered them yet.
7. Do NOT re-spawn a dev agent / Codex review for G1 — the human chose option 2 (deliver-direct writes; no dev/review for G1).
8. Resume from the packaging step the human directs.

