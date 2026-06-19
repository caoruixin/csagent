---
title: Action Bank — open backlog ledger
doc_tier: durable-connective
status: current
implementation_status: n/a
source_of_truth: this file (open R-items); docs/10-handoff.md §0 (current phase); docs/action_bank_archive.md (closed index)
last_reviewed: 2026-06-01
review_cadence: per milestone close (retention sweep — see §7)
soft_size_budget: 160 KB (post-2026-06-01 migration; long-term target ~80 KB as open R-items close + inline landed-history is trimmed per §7.1)
supersedes: []
superseded_by: null
notes: >
  LIVE action ledger: open / active / deferred R-items + deferred runtime
  candidates only. Closed sprints, milestones, and R-items are relocated to
  a compact pointer index in docs/action_bank_archive.md. Current phase /
  recently-closed narrative lives in docs/10-handoff.md §0. The full
  pre-migration history is preserved at git 17991c6 (docs/action_bank.md).
---

# Action Bank

## 1. Current phase

Current phase and recently-closed state: see `docs/10-handoff.md` §0
(cold-start table — source of truth for what is active now). Active
contract: see `docs/milestone_objective.md` + `docs/sprint_objective.md`.

This file tracks only the **open / active / deferred** R-item backlog
(§4 deferred runtime candidates; §5 eval-governance / non-runtime
backlog; §5.2 + dated sub-sections of surfaced-but-open R-items; the
open-questions ledger). Closed items are relocated to
`docs/action_bank_archive.md` per the §7 retention sweep.

## 2. Current accepted state

See `docs/10-handoff.md` §0 (current state) and
`docs/action_bank_archive.md` §B (closed milestone index).

## 3. Active / next actions

Active sub-sprint / milestone deliverables live in
`docs/sprint_objective.md` and `docs/milestone_objective.md`, not here.
The open R-item backlog the deliver-agent draws from is the remainder of
this file (§4 onward); each open row carries its id, layer
(`docs/current/iteration_governance.md` §3.2), source, and status.

## 4. Deferred runtime candidates

| id | candidate | status | owner | notes |
|---|---|---|---|---|
| D-cs015-description-keyword-moderation-cue | description-keyword moderation signal in `UseCaseRouter.buildModerationRoutingContext` for forms with no `ad_id` | deferred | runtime routing | only if selected later; preserve cs095 negative guard; no broad moderation suite |
| D-cs176-UC-I-drift | unjustified UC-I drift on cs_176 r2 | deferred | runtime routing | explicit-human-help → `user_requested` regression already green; drift is separate |
| D-S3-no-prior-search-guard | refuse `request_handover(faq_miss_threshold_exceeded)` without a prior `search_knowledge` | deferred | runtime/tool-use | not needed for cs259 Sprint 6/8 closure; only reconsider if new evidence appears |
| D-S5-Tier2-runtime-guard | runtime guard for Tier-2 policy reasoning if prompt-only fix proves insufficient | deferred | policy/runtime | do not implement unless prompt path proves insufficient |
| D-broad-routing-taxonomy | broad routing taxonomy rewrite | deferred / avoid | none | do not reopen without explicit new phase |
| D-advert-link-product-decision | whether the bot may provide a direct advert URL for the `tool_scope_blocked` follow-up shape (manual probe `a7e20173`) | deferred — product decision | product / policy | runtime currently routes the follow-up to handover with `tool_scope_blocked`; on-design until product policy says otherwise; do not implement an advert-link tool without policy sign-off |
| D-rerank-fallback-diagnostics | distinguish `rerank_llm` score from `rerank_fallback` score in observability | partially landed (logs only); full per-hit attribution in trace JSON / dashboard deferred until a corpus-level rerank investigation is opened | runtime/observability | prior landed history: `docs/sprints/sprint-015-handoff.md` (§M2 log-stream fallback surfacing + `top_is_fallback_score` stamp) |
| D-full-issue-ledger | full per-issue ledger (`issues[]`, per-issue budgets, all-UC task taxonomy) on top of the Sprint 11 progressive-resolve MVP | deferred / avoid | none | Sprint 11 explicitly carved this out; do not reopen without a new objective doc and explicit acceptance criteria |
| D-full-issue-ledger-light | thin push/pop session-side stack of `(prior_uc, prior_phase, prior_skill_state_snapshot)` on UC switch + restore on UC return (lighter than `D-full-issue-ledger`) | deferred-pending-research | none | NEW 2026-06-08 (CS3/CS4 doc §10.4); trigger = M-Auto-7 close + multi-issue trace survey (Path α: Phase 1 quantify → Phase 2 design → Phase 3 bundle-or-defer). Complementary to landed `prior_use_case_carry` soft signal; NOT folded into any M-Auto-7 sub-sprint; heavier sibling `D-full-issue-ledger` stays deferred/avoid above |
| D-skill-runtime-framework | full skill runtime framework + handover payload rewrite | **skill runtime framework SHIPPED (M2 CLOSED Sprint 41); handover payload rewrite remains separately deferred per M3-B `D-single-handover-orchestrator`** | runtime | NEW M2 (Skill Registry Abstraction) consumed the original Sprint 11/12/13-era deferral. prior landed history: `docs/milestones/M2_objective.md` + `docs/sprints/sprint-037-handoff.md`, `sprint-038-handoff.md`, `sprint-039-handoff.md`, `sprint-040-handoff.md`, `sprint-041-handoff.md` |
| D-prompt-risk-signal-handling | narrow "Risk signal handling" block in `system_prompt.txt` | deferred — Eval Governance trigger | prompt / Eval Governance | exact wording + 5 focused golden prompt tests pre-specified in `docs/runtime_freeze_and_risk_policy.md` §6.2; reopen on first real-traffic case demonstrating a refund / liability / appeal promise OR a credentials request |
| D-new-escalation-reason-enum | adding / renaming a value in the canonical 23-value `escalation_reason` enum (e.g. dedicated `risk_observed_continue`) | deferred / avoid | none | cross-cuts the eval-side `ESCALATION_TRIGGER_VALUES` set; needs a coordinated migration via a new objective doc |
| D-hard-citation-gate | hard runtime citation gate (refuse / rewrite / loop on missing citation) | deferred / avoid — **general gate REMAINS DEFERRED** for any generic / cross-FAQ-path / content-quality / `class=escalate` extension (reopen only on real-traffic P0/P1 evidence); the narrow M2 §6 #4 bounded inversion (S1 RESOLVE_FAQ `must_cite_source` presence-only check) SHIPPED Sprint 39 in `SkillGuardrailDispatcher.handleMustCiteSource` | none | Sprint 14 §L2 carve-out; observability via `citationPresent` / `citationMatch` / `citationDrift` / `resolvedButUncited` in place. prior landed history: `docs/sprints/sprint-039-handoff.md`, `docs/sprints/sprint-037-codex-review.md` (bounded-inversion design freeze + scope verification) |
| D-faq-grounded-resolve-bypass | refuse / replan a FINAL_ANSWER shape that paraphrases a `retrieved_but_unresolved` hit on a FAQ-path UC | deferred — Sprint 16 §S1 candidate | runtime / S1 | observable today as `retrieved_but_unresolved=true` on a `factual_answer` turn; current §G2 guard handles the dominant `request_handover(faq_miss_threshold_exceeded)` shape, not the FINAL_ANSWER shape |
| D-cited-but-unresolved | hallucination signal candidate when `citation_drift=true` with a cited source_id never retrieved/resolved | deferred — diagnostics-only | runtime / S1 | needs reproducible cases before any runtime hardening; watch the §L2 diagnostics surface |
| D-single-handover-orchestrator | Single Handover Orchestrator: exactly-once side-effect owner before Salesforce production cutover | deferred — release-gate blocker | runtime | Sprint 16 documented the contract in `docs/proposals/handover_orchestrator_design.md` and added repro tests; current LLM-driven path produces dual local handover persistence (P2 today, P1 launch-readiness, P0/P1 if real double transfer occurs in production); release-gate rule `docs/release_gate.md` §1.1 blocks real Salesforce cutover until handover side-effect is idempotent by `session_id`; trigger to start runtime sprint = real Salesforce client staged for cutover OR real-traffic case shows duplicated handover routing |

## 5. Eval governance / non-runtime backlog

| id | item | classification | owner | notes |
|---|---|---|---|---|
| G-stall-detector-calibration | cs066 / cs038 stall detector + persona pacing variance | judge_volatility / eval-side contract | Eval Governance | `STALL_AFTER_TOOL_INTENT` fires on legitimate intake clarification turns |
| G-L3-relevance-tone | L3 `relevance` and `tone_appropriateness` judges | judge_volatility | Eval Governance | flips across runs even on passing cases |
| G-FAQ-corpus-answerability | cs259 / cs192 / cs095 | faq_corpus_gap / product_policy_gap | Eval Governance / corpus audit | no resolve-grade article for the user's intent |
| G-label-confidence-registry | per-case label confidence + override audit consolidation | eval governance | Eval Governance | input to a future `case_label_confidence.md` |
| G-release-gate-policy | release gate definition (gates, thresholds, sign-off) | release governance | Eval Governance | input to a future `release_gate.md` |
| G-prompt-risk-signal-evaluation | when / whether to land the deferred Sprint 13 prompt edit | prompt governance | Eval Governance | trigger on first real-traffic refund-promise / liability-decision / credentials-request finding |
| G-canonical-url-corpus-curation | fill the 38 articles in the FAQ corpus that ship with no `Help_Site_URL__c` | corpus_audit / faq_corpus_gap | Eval Governance / corpus audit | observable today as `canonical_url_missing=true` on every search hit / resolve_article result for the affected articles; runtime fix already lands in Sprint 14 §L0 |
| G-resolved-but-uncited-rate | per-corpus rate of `resolved_but_uncited=true` factual-answer turns | judge_volatility / prompt | Eval Governance | a high rate suggests the prompt does not encourage citation; watch the Sprint 14 §L2 diagnostic before opening a prompt change |


### 5.2 G1 surfaced backlog

Sprint 18 G1 (2026-05-13) filed 10 Failure Briefs at
`docs/diagnostics/failure-briefs/` and surfaced the items below. R-item
ids are stable kebab-case; sources cite the brief file(s) that named
each R-item. Conditional-broadening (rename retroactively when a 2nd
instance confirms the pattern) is recorded in
`docs/sprints/sprint-018-handoff.md` §8.8.

**Tier-0 candidates**

| id | source briefs | description |
|----|---------------|-------------|
| R-escalation-reason-runtime-evidence-contract-review | cs_040 + cs_176 + manual-probe (3 instances) | Solidly systematic. Should the runtime enforce that escalation_reasons claiming session events (`intake_complete_for_uc_*`, `faq_miss_threshold_exceeded`, `clarification_budget_exhausted`, etc.) require corresponding event evidence? Scope: evidence-claiming subset only (not `user_requested` / `user_distress` which have separate contracts). Tier-0 promotion requires `human_review_required` per §3.2 Q2. |

**Systematic (≥2 instances confirmed)**

| id | source briefs | description |
|----|---------------|-------------|
| R-corpus-coverage-audit-per-uc | cs_095 (UC-D account/email) + cs_192 (UC-B free-items/giveaway) + cs_259 (UC-F payment) | Per-UC FAQ corpus coverage audit. Resolve-grade articles for common entry-point questions per UC. **No generator-synthesized articles** (only genuine help-center content). |
| R-faqMissCount-threshold-and-timing-review | cs_095 + cs_192 + cs_259 | Two-part: (a) `>= 2` threshold given auto-search burns one; (b) threshold check timing vs form-description fallback. Config governance, not runtime semantic change. |
| R-duplicated-greeting-projection-fix | cs_095 ("Hi Trish! Hi Trish") + cs_176 ("Hi Gary! Hi Gary") | Single projection / template-rendering bug rendering greeting twice. |

**Per-case L3 / governance**

| id | source brief | description |
|----|--------------|-------------|
| R-uc-b-customer-context-policy-review | cs_015 (Related observation) | Phase 2 §2.10 line 358 restricts `get_customer_context` to UC-A/UC-FP/UC-K. Should UC-B (Posting & Editing) be added? Tangential to cs_015's main failure but worth a separate product-policy review. |
| R-persona-goal-summary-scope-clarity | cs_040 | **Conditional** — only open if G2 case-family work shows the same scope-broadening pattern on other personas. |

**G2 input / future input**

| id | source brief | description |
|----|--------------|-------------|
| R-g2-multi-turn-followup-case-family-design | cs_095 | G2 case-family construction should intentionally include multi-turn followup cases on FAQ-resolve UCs to exercise the `skill_state` surface. |

**New infra (from manual probe)**

| id | source brief | description |
|----|--------------|-------------|
| R-runtime-orchestrator-tool-call-deduplication | manual-probe 2026-05-13 | **status: partial** — layer reclassified to `semantic_planner`; remediation split into read-side `R-prompt-projection-already-called-soft-signal` (`prompt_projection`) + write-side (future `HandoverOrchestrator` design freeze in `docs/proposals/handover_orchestrator_design.md`); write-side dedup since succeeded by `R-runtime-identical-tool-call-retry-storm` (A1 / S-Auto-12). prior landed history: `docs/sprints/sprint-019-handoff.md`, `docs/sprints/sprint-020-handoff.md` |

**External / regression discovery**

| id | source | description |
|----|--------|-------------|
| R-smoke-regression-investigation | 2026-05-05 → 2026-05-10 smoke run drop (9/14 = 64% → 3/14 = 21%) | **status: partial** — P1 investigation complete; remediation deferred across 3 follow-on R-items (`R-slow-llm-placeholder-coalesce` infra; `semantic_planner` escalate-on-explicit-request; `R-uc-k-intake-complete-case-id-binding` skill_state). prior landed history: `docs/sprints/sprint-019-handoff.md` §3 |

**Sprint 19 surfaced backlog**

Sprint 19 (2026-05-13) walked `docs/current/iteration_governance.md`
§3.2 per case for the two G1-blocking R-items and surfaced 7 new
R-items (6 from the handoff §11 + 1 promoted from the Sprint 18 G1
open observation per the conditional-broadening rule, now n=3 across
cs_259 + manual-probe + cs_011 T2 silence).

| id | source | description |
|----|--------|-------------|
| R-slow-llm-placeholder-coalesce | Sprint 19 §3 + §11 (cs_002 / cs_011 / cs_014 / cs_038 / cs_040) | `infra` layer. Two consecutive `LlmDeadlineExceededException` paths re-emit the same user-facing `"Sorry, I'm a bit slow right now. Please try sending that again in a moment."` message from `PhaseEvaluator.java` line 765; the loop / stall detector then flags it as a loop. Recommended remediation: (a) coalesce consecutive deadline events within a session into one user-facing beat (or move the placeholder to a side-channel typing-indicator path that does not consume a transcript turn); and/or (b) escalate honestly on the second consecutive deadline. Scope: `PhaseEvaluator.java` line 754–770 + `AgentRunResult.deadlineExceeded` propagation. **No keyword / regex / if-else / enum / per-UC matrix.** |
| R-uc-k-intake-complete-case-id-binding | Sprint 19 §3.6 + §11 (cs_066) | `skill_state` layer per §3.2 Q4. UC-K intake completes per the Sprint 7 §I2 intake-complete guard and the bot escalates with `intake_complete_for_uc_k`, but the assembled handover payload carries a null `case_id` because the auto-create-case step before escalation no longer reliably runs. Recommended scope: audit `CreateCaseControlledTool` call-site coverage across intake UCs (UC-G / H / I / J / K) + the post-intake-complete escalation path in `ControlKernel.java` line 1490–1595. |
| R-idempotent-read-tool-short-circuit | Sprint 19 §4.2 + §11 (manual-probe Track B Layer 2) | `infra` layer, **conditional** on the soft-signal-only approach (`R-prompt-projection-already-called-soft-signal`) proving insufficient. For side-effect-free tools (`search_knowledge`, `lookup_*`, `get_*_context`), short-circuit identical-args re-emission within the same `AgentRunLoop.run(...)` by returning the cached prior result. Write tools opt out; they are owned by the Sprint 16 `HandoverOrchestrator` design freeze. |
| R-sprint-narrative-vs-git-log-reconciliation | Sprint 19 doc-status warning #2 + open question 4 + §11 | governance / docs-only. The Sprint 18 G1 narrative that Sprints 14 / 14.1 / 15 / 16 made "no runtime semantic change" is contradicted by the git log (40 commits in the smoke gap window, 18 touching code). A reconciliation note in `docs/current/` or a fold-back into a `iteration_governance.md`-adjacent governance doc should land on `doc_governance.md` §"Code ahead of docs" cadence. |
| R-prompt-phase-plan-directive-followship | cs_259 (Sprint 7 §I0 violation, UC-F) + manual-probe (RESOLVE MUST-call-resolve_article violation, UC-A) + cs_011 T2 silence (Sprint 19 §3.3, UC-D) | **OPEN** (n=3 across 3 UCs and 3 directive shapes). `prompt_projection` layer. Bot ignores explicit phase-plan directives the runtime placed in the per-turn projection. Recommended approach: surface a soft directive-compliance diagnostic (e.g. `pending_directive_unfulfilled=true`) — **not** a Java check that the bot complied. Disposition: open; (R1) structural-sprint trigger (≥2 confirmed directive non-fulfillments) remains unmet (Sprint 29 probe contributed 1, D616.2). prior landed history (Sprint 27 directive enumeration + Sprint 28 trace-dump unblock + Sprint 29 R2 probe): `docs/sprints/sprint-027-handoff.md`, `docs/sprints/sprint-028-handoff.md`, `docs/sprints/sprint-029-handoff.md` |

**Open observations (NOT opened as R-items)**

n=1 evidence is insufficient to open an R-item; controlled multi-shape
testing needed (rule recorded in
`docs/sprints/sprint-018-handoff.md` §0 / §8.7).

- **ad_id form-vs-listing data consistency** — manual-probe (form
  `ad_id=ad-1003` vs listing `ad_id=AD-1001`). n=1; needs production
  data to know if this is a common shape. Not opening on n=1.

(Note: `R-prompt-phase-plan-directive-followship`, previously an
n=2 open observation in this section, was promoted to a Sprint 19
surfaced R-item above after the Sprint 19 §3.3 cs_011 T2 silence
finding constituted a 3rd instance across a 3rd UC.)

**Closed cross-references preserved** (closed by the milestone retention
sweep; detail lives in the cited archives, not here):
`R-per-case-trace-dump-for-smoke-harness` (closed Sprint 28; prerequisite
for the directive-followship probe) → `docs/sprints/sprint-028-handoff.md`;
`R-per-turn-phase-transition-dump-for-smoke-harness` (Sprint 29 proposed
follow-on, not opened) → `docs/sprints/sprint-029-handoff.md`;
`R-slow-llm-placeholder-coalesce-honest-next-step` (paired latency item) →
`docs/sprints/sprint-024-handoff.md`.

**Sprint 20 surfaced backlog**

Sprint 20 (2026-05-13, closed after fix iteration) delivered Track A
(case families + v0 shadow split) and Track B (`already_called`
soft-signal slot) and surfaced the items below. The Sprint 20 fix
iteration closed Codex's two original substantive blocking findings
(runtime non-enforcement evidence + `_ACCESS_BOUNDARY.md`
reconciliation). The fix re-review returned
`decision: out_of_scope_review, blocking_count: 1` on packaging
grounds only (commit-boundary observation that the dev's fix commit
bundled deliver-agent-owned files); substantive content closed
cleanly per Codex's own evidence. The packaging artefact is rolled
forward in the close commit.

| id | source | description |
|----|--------|-------------|
| R-shadow-include-flag-runner-gate | Sprint 20 §12 + Sprint 20 fix `_ACCESS_BOUNDARY.md` "Known v0 gaps / v1 hardening" | `infra` / eval harness. Sprint 20's v0 shadow-split mechanism relies on directory boundary + custom-path-only loading via `CaseSetManager.load_custom(path)` + documented self-restraint. A `--include-shadow` runner-flag gate is documented as a v1 hardening direction for a follow-on eval-governance sprint to add if the v0 boundary proves insufficient (e.g. accidental glob-loading by a dev agent in a later sprint). Scope: `eval_interactive/eval_interactive/batch/sets.py` + `eval_interactive/eval_interactive/cli.py`. Disposition: **deferred (Sprint 20 §12 + Sprint 20 fix `_ACCESS_BOUNDARY.md` "Known v0 gaps / v1 hardening")**. |
| R-case-family-runner-set | Sprint 20 §12 + §11 open question 4 | `infra` / eval harness. The 60 hand-authored CaseSpecs under `eval_interactive/case_specs/case_families/` load via `--path` only. Adding a `--set case-families` flag (or equivalent) would let the human + review agent sample-run a family for ground-truth validation. Scope: `eval_interactive/eval_interactive/batch/sets.py` + CLI. Disposition: **deferred (Sprint 20 §12 open question 4)**. |

**Sprint 20 open questions (next-sprint sequencing, not blockers)**

The Sprint 20 handoff §11 records three open questions (beyond §11 Q1
and Q5, which are tracked as R-items above) that the next sprint
should weigh when picking scope. They are sequencing concerns, not
blockers on Sprint 20 close.

- **cs_095 classification re-review urgency** (Sprint 20 §11 Q2).
  cs_095's existing Wave A2.1 override pins UC-A primary; the human
  brief re-review identifies UC-D primary
  (`R-cs095-uc-classification-l3-rereview`). Track A authored every
  cs095-family neighbor / negative / shadow assuming UC-D primary is
  the correct classification. If the L3 re-review reverses (UC-A
  confirmed), the family's expected fields need a refresh.
  Recommendation: the L3 review batch should run before any
  cs095-shape remediation sprint.
- **cs_011 cluster C regression interaction** (Sprint 20 §11 Q3).
  cs_011's Sprint 4 §E1 L3-approved override pins the 2026-05-05
  PASS shape; Sprint 19's Cluster C analysis annotated the
  2026-05-10 regression as slow-LLM placeholder + planner
  failure-to-escalate noise. When a future remediation sprint
  addresses the cs011 pattern, the open question is whether the
  regression noise (`R-slow-llm-placeholder-coalesce` and
  `R-prompt-phase-plan-directive-followship`) should be addressed
  first or remediation can proceed in parallel.
- **Hand-authored CaseSpec validation harness** (Sprint 20 §11 Q4).
  The 60 hand-authored CaseSpecs validate by inspection today
  (Sprint 20 objective allows this for the authoring deliverable).
  The validation question becomes load-bearing as soon as the first
  remediation sprint wants to consume a family; tracked separately
  as the `R-case-family-runner-set` R-item above.

**Recommendation:** the next sprint after Sprint 20 close should
pick the L3 review batch (Wave A5/A6 — `R-cs001-escalation-trigger-l3-review`,
`R-cs038-l3-review-intake-efficiency`,
`R-cs040-l3-review-intake-completion-semantics`,
`R-cs095-uc-classification-l3-rereview`,
`R-cs176-escalation-reason-l3-review`,
`R-cs192-secondary-ucs-duplicate-uc-b`, and the systematic
`R-generator-get-customer-context-policy-mismatch`) as the natural
follow-on. The cs095 Q2 sequencing concern names this batch as the
prerequisite for any cs095-shape remediation. (**Done — Sprint 21
delivered the batch; see Sprint 21 surfaced backlog below.**)

**Sprint 21 surfaced backlog**

Sprint 21 (2026-05-14, closed after fix iteration) delivered seven
L3 R-item dispositions across the Wave A5/A6 batch named above: 3
approved overrides (cs_001, cs_095, cs_192) written to
`eval_interactive/case_spec_overrides.yaml` (17 applied entries
total; 0 pending); 2 rejected (cs_176, systematic
`R-generator-get-customer-context-policy-mismatch`) with new
remediation R-items proposed at the correct layer; 2 deferred
(cs_038, cs_040) on the missing schema extension. The cascade rule
held (no Sprint 20 case-family content touched). The §1.7 forbidden
-line check ("widening eval spec to accept a genuine bot mistake")
was applied per disposition; cs_176 is the explicit example of
declining to widen an L1-correctly-hard-failed case. Codex fix
re-review verdict `decision: fix_required, blocking_count: 3` on
typographical-fidelity grounds only (5-character markdown emphasis
+ trailing-colon drops on brief-quote blocks); substantive
dispositions confirmed sound per Codex's own non-blocking checks
(override schema sane, no case-family edit, no judge-rubric edit,
no semantic hardcode, cs_095 dimension-distinction byte-identical).
Human accepted the evidence-package gap as close-eligible
(classification: A-with-evidence-gap-acknowledgment); the gap is
in documentation polish, not in any system behaviour or governance
surface. See `docs/sprints/sprint-021-handoff.md` §12 + the close
narrative in `docs/10-handoff.md` §1.

| id | source | description |
|----|--------|-------------|
| R-cs176-semantic-planner-escalation-family-discrimination | Sprint 21 §3.5 cs_176 rejection | `semantic_planner` layer. The LLM must discriminate user-intent escalation families (`user_requested`) from bot-limit escalation families (`faq_miss_threshold_exceeded`) on UC-E refund / fulfillment-demand shapes. Soft-signal candidate: a projection slot naming evidence dimensions (FAQ-search-evidence-count, user-explicit-demand-detected); LLM owns the choice; **NO** keyword / regex / per-UC matrix on user content. Conditional follow-on: G3+ remediation sprint can pick up once `R-slow-llm-placeholder-coalesce` and `R-prompt-phase-plan-directive-followship` (Sprint 19 backlog blocking cs_011-shape remediation) are in motion. |
| R-case-spec-overrides-schema-scoring-extension | Sprint 21 §3.2 (cs_038 deferral) + §3.3 (cs_040 deferral) | `infra` / eval harness. Extend the override schema at `eval_interactive/eval_interactive/case_spec/extractor.py:_CASE_OVERRIDE_EXPECTED_FIELDS` + `extractor.py:_normalise_expected_block` to permit `scoring.*` overrides per `source_session_id`, OR add new `Expected` dataclass fields (`intake_completion_turn_target`, `min_intake_fields_collected`, etc.) that scorer logic consumes. Unblocks the two deferred Sprint 21 R-items. Sprint 21 §11 Q4 records the path-choice sub-question. |
| R-cs095-family-refresh-post-l3-reversal | Sprint 21 §3.4 + Sprint 20 §11 Q2 | eval governance, **contingent / DORMANT**. Triggered only if a future disposition reverses cs_095 UC primary back to UC-A (or to any non-UC-D primary). The Sprint 20-authored cs_095 family at `eval_interactive/case_specs/case_families/cs095_uc_classification_account_aware/` and `eval_interactive/case_specs_shadow/case_families/cs095_uc_classification_account_aware/` was authored assuming UC-D primary; reversal would require the family's expected fields to refresh. Currently DORMANT (Sprint 21 UC-D confirmation matches the family's authoring direction). |

**Sprint 21 open questions (next-sprint sequencing, not blockers)**

The Sprint 21 handoff §11 records five open questions for the human.
Q1 (cs_011 override wording inconsistency) and Q2 (cs_095 UC-D-primary
contract gap) are both addressed by the recommended next sprint
`R-phase2-uc-cdf-customer-context-policy-widen`. Q3 (DORMANT-recording
posture for `R-cs095-family-refresh-post-l3-reversal`) is captured
above in the R-items table. Q4 (override-schema-extension scoping)
is captured in `R-case-spec-overrides-schema-scoring-extension`. Q5
(deferred-disposition follow-up cadence) is a sequencing concern,
not a blocker.

**Note (Sprint 22, 2026-05-14):** Sprint 21 §12.4's recommendation
that `R-phase2-uc-cdf-customer-context-policy-widen` be the next
sprint is rescinded. The widening premise was invalidated by Sprint
22's premise-verification check; the cross-UC allowlist at phase 2
§2.10.1 line 1098 already permits UC-C / UC-D / UC-F. Sprint 21's §11
Q1 (cs_011 override wording inconsistency) and Q2 (cs_095 UC-D-primary
contract gap) remain open and need a different remediation path —
see Sprint 22 handoff §10 for the rescoping.

**Sprint 22 surfaced behavioural R-items**

Sprint 22 (2026-05-14) was a docs-only scope-correction sprint. It
closed `R-generator-get-customer-context-policy-mismatch` and
`R-phase2-uc-cdf-customer-context-policy-widen` as
premise-invalidated (the underlying policy already permits the tool
for UC-C / UC-D / UC-F per phase 2 §2.10.1 line 1098). The
substantive behavioural question those R-items had been carrying —
does the bot actually use `get_customer_context` when account-state
matters on FAQ-miss shapes — is captured below as a new R-item.

| id | source | description |
|----|--------|-------------|
| R-uc-cdf-get-customer-context-bot-actual-usage | cs_001 (UC-C) + cs_011 (UC-D) + cs_259 (UC-F); discovered via Sprint 22 premise-verification | Layer hint: `prompt_projection` or `semantic_planner` (NOT `product_policy` — the policy already permits per phase 2 §2.10.1 line 1098, corroborated by `docs/customer_service_tool_spec_v0_2.yaml` line 260 and `docs/current/customer_service_tool_spec_v0_3.md` line 60). The three source briefs documented that on FAQ-miss shapes where account-state would plausibly matter, the bot did not call `get_customer_context` despite the tool being allowed for UC-C / UC-D / UC-F. Open question: does the per-turn projection surface the right signal to consider the tool, or does the semantic planner systematically under-call it? **Disposition: proposed (Sprint 22 close 2026-05-14); deferred for future investigation sprint.** No remediation in Sprint 22. |

**Sprint 23 surfaced R-items (close 2026-05-14)**

Sprint 23 closed on 2026-05-14 after one fix iteration on the PASS
branch. Track A bundle (`R-already-called-prompt-consumption`) landed
with target-reversal evidence; that R-item's row is updated in §5.2
above. Four follow-on R-items proposed in Sprint 23 handoff §11 are
captured here (one is the Track B narrow UX repair the strict bundle
gate deferred; one is its paired diagnostic; two are conditional /
n-ladder follow-ons).

| id | source | description |
|----|--------|-------------|
| R-llm-latency-budget-investigation | Sprint 23 §4.2 hypotheses (a) + (b); Sprint 19 §3.7 hypothesis as starting evidence | `infra` / diagnostic. The Sprint 19 §3.7 hypothesis (model switch `f2d4cb2` widened latency variance) remains unverified at per-turn granularity. Investigation only — NO budget edit; the per-call-instrumentation prerequisite is deferred to `R-per-llm-call-latency-instrumentation`. Disposition: **reframed-as-coarse-proxy; per-call instrumentation deferred** (prerequisite to any deadline-budget or model-revert decision). prior landed history: `docs/sprints/sprint-024-handoff.md` §4.1/§4.2 |
| R-cs040-uc-k-topic-subject-routing | Sprint 23 §4.4 + Sprint 19 §3.5 | `prompt_projection`; **n=1; conditional opening**. cs_040 `active_use_case=UC-C` in the 2026-05-10 smoke run vs expected `UC-K` per `primary_uc` (form_context `topic_subject=technical issue intake`); Sprint 19 §3.5 documented the topic_subject cue not anchoring routing post-`8282783`. Proposed scope: investigate whether the topic_subject projection signal is reaching the routing prompt with sufficient salience; if not, propose a soft-signal addition (e.g. surface the topic_subject in the routing projection's `routing_signals` slot with appropriate weight). Disposition gate: open only if controlled multi-shape testing surfaces a second instance of the same routing-cue-loss shape. Disposition: **proposed (Sprint 23 §4.4; n=1; conditional opening per the user's controlled-multi-shape-testing bar).** Sprint 24 fence held (Track A fixed the *placeholder shape* on cs_040; routing surface untouched per Sprint 24 §3.5 + §6.1). |
| R-accumulated-tool-results-prompt-consumption | Sprint 23 §10 question 5; cs_014 partial-reversal evidence | `prompt_projection`; **conditional**. `accumulated_tool_results` is in every projection (since Sprint 8) but `server/src/main/resources/prompts/system_prompt.txt` has zero standalone teaching about the slot. Sprint 23's `already_called` teaching paragraph cross-references it but does not stand alone as teaching for `accumulated_tool_results`. The cs_014 fix-iteration rerun (`eval_interactive/results/20260514-081022/results.json`) showed the duplicate-`search_knowledge` shape persists on that case despite the Sprint 23 teaching (user-visible PASS via handover, but underlying 4-consecutive sk shape present) — supporting evidence that the `already_called` cross-reference alone is not sufficient on every target shape. Proposed scope: add a short paragraph (parallel to the Sprint 23 `already_called` paragraph) describing the slot's lifecycle (last-write-wins per tool name; populated by every successful dispatch in the run; the LLM should consult it before deciding to re-emit ANY tool). Disposition gate: conditional on Sprint 23's teaching not being sufficient — cs_014 partial result already provides one instance; opening is warranted if a follow-on smoke rerun confirms the broader pattern. Disposition: **proposed (Sprint 23 §10 question 5 + §11; cs_014 partial result as initial supporting evidence).** |
| R-pre-post-f2d4cb2-pass-rate-isolation | Sprint 26 §5.2 + §7 (hypothesis (ii) walk; n=4 post + n=3 last-pre observation) | `eval_spec` / diagnostic; **conditional opening per the multi-shape-testing bar**. ~35pp post-`f2d4cb2` smoke pass-rate gap (post mean ~23% vs pre mean ~57%) is too large for n=14 variance alone, but concurrent surfaces (L3 overrides, prompt teaching, working-tree mod) confound model-attribution. Proposed scope: controlled rerun under pre-`f2d4cb2` model WITHOUT rolling back post-`f2d4cb2` prompt/corpus, to isolate the model share. NO rubric widening / CaseSpec edit / new instrumentation. Disposition: **proposed; deferred** (re-open trigger: additional smokes confirming the gap is stable OR human direction). prior landed history: `docs/sprints/sprint-026-handoff.md` |

**Sprint 31 surfaced R-item (smoke-rerun latency observation)**

Sprint 31 (2026-05-16) shipped the Option β projection slot and
observed a smoke-rerun latency widening with zero latency-relevant
change (internal hypotheses rejected; external LLM provider drift the
remaining candidate). The follow-on R-item below carries the open
diagnostic surface. prior landed history: `docs/sprints/sprint-031-handoff.md` §13.

| id | source | description |
|----|--------|-------------|
| R-llm-provider-latency-drift-2026-05-16 | Sprint 31 §13.7 fix-iteration recommendation + §13.4 latency widening evidence | `infra` / observability; **proposed**. Three Sprint 31 smoke reruns on the same bot codebase showed mean non-CV `elapsed_ms` widening +20%/+78%/+84% over the Sprint 28 reference with ZERO latency-relevant code edits; most likely root cause is external LLM provider drift (`deepseek-v4-flash` and/or `qwen-plus`). Proposed scope: A/B per-LLM-call latency (Sprint 25 `case_results[].llm_calls[]`) on a fresh DB to name the provider-attributable share. NO deadline-budget widening / model revert / prompt / eval-spec edit. Disposition: **proposed; deferred** behind a future diagnostic-sprint slot. prior landed history: `docs/sprints/sprint-031-handoff.md` |

### Sprint 35 surfaced R-item (constraint-removal architectural follow-on)

Sprint 35 (2026-05-17) closed `A — Clean close` (probe sprint). The
probe matrix confirmed `form_context.topic_subject` acts as a HARD
GATE on the intake router's candidate UC pool (cross-topic UCs
structurally absent regardless of evidence). The R-item below carries
the architectural follow-on (a **constraint-removal /
architecture-alignment** item, NOT a new Java component). prior landed
history: `docs/sprints/sprint-035-handoff.md` + `docs/diagnostics/option_beta_coverage_matrix.md`.

| id | source | description |
|----|--------|-------------|
| R-loosen-topic-uc-binding-llm-owned-drift | Sprint 35 §5–§7 + `docs/diagnostics/option_beta_coverage_matrix.md` §4–§5 + deliver-agent + human Sprint 35 close decision 2026-05-17 (reframes the §1.7-rejected "Option γ AlternateUseCaseSurveyor") | `prompt_projection` / `semantic_planner` (M2+ candidate; layer pre-classification — research-agent at planning refines). **Constraint-removal / architecture-alignment R-item, NOT a new Java component.** `UseCaseRouter` enforces `form_context.topic_subject → candidate UC list` as a HARD structural gate (3-of-3 weak-prior multi-candidate topics confirmed by the Sprint 35 matrix); this conflicts with Constitution §1.3 + §1.7. The Topic↔UC mapping is appropriate as a prefill prior at turn 1 but inappropriate as a runtime cage on cross-topic drift. **Acceptance bar (preliminary):** UC drift / topic-shift moves to LLM ownership for cross-topic cases; runtime preserves only safety / handover-only / Tier-0 boundaries as Java gates. **Explicit anti-framings (do NOT do):** (1) new live `AlternateUseCaseSurveyor` deterministic component (§1.7 re-introduction); (2) per-UC if-else / regex / enum-expansion for cross-topic drift (§1.5); (3) collapse `topic_subject` into the runtime (preserve it as a prefill prior). **Disposition: proposed; deferred to M3+** (supersedes `R-option-beta-coverage-gap-uc-a-uc-c-shape`). prior landed history (Sprint 35 probe matrix + design directions): `docs/sprints/sprint-035-handoff.md`, `docs/diagnostics/option_beta_coverage_matrix.md` |

### Sprint 37 design-freeze surfaced Tier-0 candidates (2026-05-17)

NEW Sprint 37 (Skill Registry design freeze; CLOSED PASS A) §4.1 walk surfaced 5 Tier-0 candidates; 2 are QUALIFIED-DEFER and tracked below as R-items for M3+ revisit (C1 REJECTED — existing invariant; C4/C5 NOT-A-CANDIDATE per M2 §6 #4 + Sprint 7 §I2). prior landed history (5-verdict walk + Codex confirmation): `docs/sprints/sprint-037-handoff.md`, `docs/sprints/sprint-037-codex-review.md`.

| id | source | description |
|----|--------|-------------|
| R-skill-guardrail-non-overridability-tier-0 | NEW Sprint 37 design-freeze §4.1 walk; design doc `docs/proposals/skill_registry_design.md` §12.2 (C2 candidate) | `java_guard` candidate — should "Skill terminal predicate refusal is non-overridable by LLM" be codified as a Tier-0 invariant in `docs/runtime_freeze_and_risk_policy.md` §1/§2? **Status: proposed; DEFER per `feedback_constitution_discipline_vs_planning_anticipation.md`.** Reason for DEFER: the Tier-0 claim is runtime non-overridability under real traces; deterministic Java tests (Sprint 39 dispatcher + 24 tests) are necessary but not sufficient production evidence; Codex confirmed DEFER at Sprint 39 + Sprint 41 + M2 close. **R-item STAYS OPEN; re-evaluation flagged at M3+** after production trace observation (per `docs/milestones/M2_objective.md` §12.7). **Anti-framings (do NOT do):** (a) elevate to Tier-0 on Sprint 39 deterministic Java-logic tests alone (premature lock); (b) ship a dispatcher with a non-overridability-violating fallback (defeats the §1.4 Runtime floor). prior landed history (the deferral-confirmation walk across Sprints 39/41 + M2 close): `docs/milestones/M2_codex-review.md`, `docs/sprints/sprint-039-codex-review.md`, `docs/sprints/sprint-041-codex-review.md` |
| R-skill-state-bus-boundary-enforcement-tier-0 | NEW Sprint 37 design-freeze §4.1 walk; design doc `docs/proposals/skill_registry_design.md` §12.3 (C3 candidate) | `java_guard` candidate — should "Skill-declared `state_inheritance` is enforced at session-state-bus boundary" be codified as a Tier-0 invariant in `docs/runtime_freeze_and_risk_policy.md` §1/§2? **Status: proposed; DEFER per `feedback_constitution_discipline_vs_planning_anticipation.md`.** Reason for DEFER: the Tier-0 claim is operationally about real traces having no override path; Sprint 41 shipped the FIRST evidence surface (`SkillStateBus.java` + per-Skill `state_inheritance` declarations + `prior_use_case_carry` projection + 39 tests) but deterministic tests are not sufficient production evidence; Codex confirmed DEFER at Sprint 41 + M2 close. **R-item STAYS OPEN; re-evaluation flagged at M3+** after production trace observation (per `docs/milestones/M2_objective.md` §12.7). **Anti-framings (do NOT do):** (a) elevate before production trace evidence; (b) encode per-UC-pair `state_inheritance` declarations (§1.7); (c) widen state-bus to LLM-owned semantic state (§1.3 vs §1.4). prior landed history (state-bus landing + deferral-confirmation Sprint 41 + M2): `docs/sprints/sprint-041-codex-review.md`, `docs/milestones/M2_codex-review.md` |

### Sprint 38 fix iteration #1 surfaced R-item (2026-05-17)

NEW Sprint 38 fix iteration #1 (CLOSED B-fix-iterated) surfaced one informational R-item from Codex fix-review OQ-FIX1.1 (OQ-FIX1.4 not opened — micro-optimization). prior landed history: `docs/sprints/sprint-038-handoff.md`, `docs/sprints/sprint-038-codex-review.md`.

| id | source | description |
|----|--------|-------------|
| R-skill-tool-name-canonical-source-centralization | NEW Sprint 38 fix iteration #1 OQ-FIX1.1; dev surface `SkillLoader.java` `VALID_TOOL_NAMES` Set | `infra` / governance — Sprint 38 introduced `VALID_TOOL_NAMES` as a hardcoded 11-tool Set in `SkillLoader.java` mirroring Java `Tool.getName()` values; this is the SECOND place naming canonical tool names. **Status: proposed; deferred to M3+ revisit** (refactor-level; current hardcode is grep-verifiable + stable). **Acceptance bar (preliminary):** a single canonical tool-name source (Java enum or registry) the Tool implementations + SkillLoader both reference. **Explicit anti-framings (do NOT do):** (1) auto-derive from `customer_service_tool_spec_v0_2.yaml` (YAML is design intent, Java is runtime SoT); (2) loosen the allowlist to "any string" (defeats §1.4 floor); (3) widen scope to centralize name+schema+spec at once. prior landed history: `docs/sprints/sprint-038-handoff.md`, `docs/sprints/sprint-038-codex-review.md` |

### M2 milestone close surfaced R-item (2026-05-18)

NEW M2 milestone close (closed A — Clean PASS) surfaced one informational R-item from Codex M2-shared review §11; NOT load-bearing for M2 close; M3+ cleanup candidate. prior landed history: `docs/milestones/M2_objective.md`, `docs/milestones/M2_codex-review.md`.

| id | source | description |
|----|--------|-------------|
| R-substitute-placeholders-uc-case-creation-eligibility-centralization | NEW M2 milestone close 2026-05-18; Codex M2-shared review §11 | `infra` / runtime cleanup — `substitutePlaceholders(...)` retains a legacy UC-H/J/K case-creation note; Codex confirmed NOT a §1.7 violation. **Status: proposed; deferred to M3+** (cleanup-level). prior landed history (acceptance bar + anti-framings + design directions): `docs/milestones/M2_codex-review.md` §11, `docs/milestones/M2_objective.md` §12.7 |

**Closed OQ cross-references preserved** (closed-context open-questions
whose detail was swept into the cited archives by this retention sweep;
listed here only so the ledger keeps the id pointer): `OQ-S38.1` (Sprint 37
design-doc editorial typo fold-back) + `OQ-S39.5` (C2 Tier-0 timing) +
`OQ-S40.1` / `OQ-S40.2` / `OQ-S40.3` / `OQ-S40.5` (Sprint 40 golden /
envelope / status-quo / line-number typo; note the Sprint 40 `D-f`
disposition) + `OQ-S41.2` (C3 Tier-0 timing; LOAD-BEARING at the Sprint 41 close per the
archived Codex Tier-0 verification section) →
`docs/sprints/sprint-037-handoff.md` .. `sprint-041-handoff.md` +
`docs/milestones/M2_codex-review.md`; `OQ-S47.1` (parallel-default yaml
alignment) → `docs/sprints/sprint-047-handoff.md`; `OQ-S48.2` / `OQ-S48.3`
(report-observability, merged) → `docs/sprints/sprint-048-handoff.md`;
`OQ-S52.4` (`knowledge_hits` canonicalization, projection-hygiene
milestone candidate) → `docs/sprints/sprint-053-handoff.md`.

### S-Eval-4 (Sprint 45) close surfaced R-item (2026-05-22)

| id | source | description |
|----|--------|-------------|
| R-bad-case-suite-uc-ghij-seed-from-real-sessions | S-Eval-4 (Sprint 45) close 2026-05-22; `docs/sprints/sprint-045-handoff.md` §7 OQ-S45.2 + §12.5 | `eval_spec` (data; bad-case suite expansion). UC-G/H/I/J intake-then-escalate failure modes have NO primary regression guards. **Status: proposed; impl deferred to M4+ planning.** Key §1.7 fence: do NOT synthesise UC-G/H/I/J bad cases from non-real sessions. prior landed history (3 implementation paths + anti-framings): `docs/sprints/sprint-045-handoff.md` |

### M3-Eval close surfaced R-items (2026-05-23)

NEW M3-Eval milestone-close bad-case suite manual review (§5.6 PRIMARY GATE) surfaced three R-items, all deferred to M4+ planning; none load-bearing for M3-Eval close. prior landed history: `docs/milestones/M3-Eval_objective.md`, `docs/milestones/M3-Eval_codex-review.md`.

| id | source | description |
|----|--------|-------------|
| R-iwzx-uc-k-vs-uc-h-routing-spurious-distress | M3-Eval close 2026-05-23 bad-case manual review of `iwzx_uc_k_advert_on_hold_restore` | `semantic_planner` (§3.2 Q5). UC-K "advert on hold restore" mis-routes to UC-H intake-lock then escalates spurious `user_distress` (cs011/cs014 anti-pattern); `_manifest.md` ledger = FAIL at M3-Eval close. **Status: proposed; impl deferred to M4+ planning.** Key §1.7 fence: no keyword "on hold"→UC-K hardcode, no per-UC prompt matrix, no `escalation_reason` enum widening. prior landed history (3 implementation paths + anti-framings): `docs/milestones/M3-Eval_objective.md` |
| R-bad-case-parallel-session-establishment-flakiness | M3-Eval close 2026-05-23 bad-case manual review (concurrency-correlated `contract_violation` / truncated sessions) | `infra` (eval-harness OR Java backend session creation under load). **Status: RE-PROMOTED toward ACTIVE** — path (3) (lower default `parallel` 5→1) was applied globally at S-Cleanup-1, but the flake has since RECURRED under SEQUENTIAL (`parallel=1`) conditions across cs029 (M4) + fg5q + iwzx (S3) + cs015 + fg5q + cs32s02 (S4) — 6 occurrences over 5 CaseSpecs and 3 milestones — confirming the parallel reduction did NOT address the root cause. Categorically upstream of the projection layer (bot never reached; `turns_traced=0`). **Root-cause investigation still DEFERRED, recommended for next-planning-round priority:** (1) Java backend `/v1/demo/sessions` session-creation path under load (connection pool / timeout / logging); (2) simulator session-create error handling / retry budget. **Anti-framings (do NOT do):** (1) assume per-case YAML bugs (cases pass at iso rerun); (2) disable parallel globally; (3) silent-retry the contract_violation without logging. NOTE: distinct from `R-shadow-fixture-empty-form-session-create-400` (deterministic empty-form HTTP-400). prior landed history (per-rerun forensic detail across M3-Eval/S-Cleanup-1/S3/S4): `docs/sprints/sprint-047-handoff.md`, `docs/sprints/sprint-053-handoff.md` |

### S-Cleanup-1 (Sprint 47) close surfaced R-item (2026-05-23)

NEW M4-Eval-Cleanup sub-sprint 1 close (Sprint 47 / S-Cleanup-1 closed A — Clean PASS) surfaced one informational R-item (docs-only orphan; not consumed by any code path; not load-bearing). prior landed history: `docs/sprints/sprint-047-handoff.md`.

| id | source | description |
|----|--------|-------------|
| R-case-families-manifest-cs095-smoke-vs-anchor-orphan | S-Cleanup-1 (Sprint 47) close 2026-05-23; `docs/sprints/sprint-047-handoff.md` §6 Item #7 + §8 OQ-S47.2 | `infra` / docs-only — one genuine orphan at `case_families/_manifest.yaml:114` declares `target_case_path: .../smoke/cs_interactive_095.yaml` but the file is at `anchor/`; the manifest is documentation-only (0 code consumers). **Status: proposed; impl deferred to M4+ planning.** **Three implementation paths:** (1) fix the manifest path to `anchor/`; (2) move the file to `smoke/`; (3) mark the manifest superseded if no longer load-bearing. **Anti-framings (do NOT do):** (1) silently delete the file (functional regression guard at `anchor/`); (2) add a synthetic `smoke/` copy (fixture duplication). prior landed history: `docs/sprints/sprint-047-handoff.md` |

### S-Cleanup-2 (Sprint 48) close surfaced R-items (2026-05-23)

NEW M4-Eval-Cleanup sub-sprint 2 close (Sprint 48 / S-Cleanup-2 closed A — Clean PASS) surfaced two low-priority informational R-items from OQ-S48.1/2/3; deferred to a future observability / consistency milestone. prior landed history: `docs/sprints/sprint-048-handoff.md`.

| id | source | description |
|----|--------|-------------|
| R-bad-case-metadata-field-name-canonicalize | S-Cleanup-2 (Sprint 48) close 2026-05-23; `docs/sprints/sprint-048-handoff.md` §8 OQ-S48.1 | `eval_spec` (data; bad-case fixture metadata consistency). The `bad_case_metadata` schema is non-uniform across the 12 bad cases (Alice `original_session_id` + top-level `source_session_id` + no `tier:` vs S-Eval-4 cases `source_session_id` inside the block + `tier:`). **Status: proposed; impl deferred to M4+ planning.** **Implementation paths:** (1) canonicalise field names across all 12 (additive / rename-only); (2) document the variance as intentional-historical in `_manifest.md`. **Anti-framings (do NOT do):** (1) couple the rename to any `case_passed` / closure_criterion gate; (2) drop any load-bearing metadata field. prior landed history: `docs/sprints/sprint-048-handoff.md` |

### Sprint 53 (M5 S4) + M5 milestone close surfaced R-item (2026-05-25)

Sprint 53 / M5 S4 close + M5 — Observability Coherence milestone close 2026-05-25 (A — Clean PASS) surfaced ONE new R-item from the mandatory S4 shadow rerun. prior landed history: `docs/sprints/sprint-053-handoff.md`.

| id | source | description |
|----|--------|-------------|
| R-shadow-fixture-empty-form-session-create-400 | Sprint 53 / M5 S4 close 2026-05-25 (shadow rerun, dev-blind); `docs/sprints/sprint-053-handoff.md` §12.2/§12.4 | `infra` / eval-harness or fixture. Two empty-`form_context` shadow CaseSpecs (`cs59s01`, `cs59s02`) deterministically fail `POST /v1/chat/sessions` HTTP 400 (bot never reached); distinct from `R-bad-case-parallel-session-establishment-flakiness`. **Status: proposed; impl deferred to next planning round** (low priority; upstream of bot behaviour; no §5.6 / shadow regression-safety impact). prior landed history (3 implementation paths + anti-framings): `docs/sprints/sprint-053-handoff.md` |

### Sprint 55 (M-Auto-1A S-Auto-2) surfaced R-item (2026-05-27)

Sprint 55 / M-Auto-1A S-Auto-2 close 2026-05-27 (A — Clean PASS) surfaced ONE governance-hygiene R-item: the dev schema-check confirmed the `eval/src/main/java/com/gumtree/csagent/eval/` Java module is legacy v1-demo-era code superseded by eval_interactive (Python), zero current callers, still in root `pom.xml`. The R-item below is governance hygiene (clarify or retire the legacy surface). prior landed history: `docs/sprints/sprint-055-handoff.md` §7.

| id | source | description |
|----|--------|-------------|
| R-eval-java-module-retirement | Sprint 55 / M-Auto-1A S-Auto-2 close 2026-05-27; `docs/sprints/sprint-055-handoff.md` §7 + §9 OQ-S55.4 | `infra` / governance-hygiene. The `eval/src/main/java/com/gumtree/csagent/eval/` Java module is legacy v1-demo-era code superseded by eval_interactive (Python); zero current callers; still in root `pom.xml`. **Status: proposed; impl deferred to M-Auto-2+ or a fold-back sub-sprint** (not a blocker). prior landed history (3 implementation paths — retire / document-only / revive-selectively — + anti-framings): `docs/sprints/sprint-055-handoff.md` |

### Sprint 061 / M-Auto-1B Phase 2 §5.6 review surfaced R-item (2026-05-30)

NEW M-Auto-1B Phase 2 §5.6 bad-case + shadow regression-safety joint review surfaced ONE observability-hygiene R-item (all 46 cases carry `judge_score: 0.0` despite the four-tier judge surface; the CLI "Passed: N" headline is structurally misleading; loader-counted `case_passed` is canonical). Predates M-Auto-1A; not a M-Auto-1B regression. prior landed history: `docs/milestones/M-Auto-1B_objective.md` §12.5/§12.7, `docs/milestones/M-Auto-1B_codex-review.md` Axis M9.

| id | source | description |
|----|--------|-------------|
| R-eval-interactive-judge-score-never-populated | Sprint 061 / M-Auto-1B Phase 2 §5.6 review 2026-05-30; `docs/milestones/M-Auto-1B_objective.md` §12.5/§12.7 + Codex Axis M9; **OQ-S76.judge-zero collapsed in (route c) 2026-06-05** | `infra` / observability hygiene (judge wiring; predates milestone). Every case shows `judge_score: 0.0`; the CLI "Passed: N" headline is structurally misleading; loader-counted `case_passed` IS canonical and always used. **Status: proposed; impl deferred to M-Auto-2+ planning round** (LOW priority; canonical signal works; confirmed UNCHANGED at M-Auto-1C close + M-Auto-5 close). NOT to be confused with `R-bad-case-fixture-migrate-to-l3-judge-dims` (CLOSED S-Cleanup-2; opposite side). prior landed history (3 implementation paths + anti-framings): `docs/sprints/sprint-061-handoff.md`, `docs/milestones/M-Auto-1B_codex-review.md`. **OQ-S76.judge-zero investigation update (research-agent 2026-06-05)** — two config-side sub-variants share the same `judge_score=0.0` outcome: (a) `bad_cases/*.yaml` (12/12) + `anchor_outcome/*.yaml` ship `llm_judge_dimensions: []` → `LlmJudge.judge()` (`eval_interactive/eval_interactive/scoring/llm_judge.py:96-119`) makes zero L3 calls → `composite.py:228-232` falls through to `judge_score = 0.0`; (b) `shadow/*.yaml` DOES invoke L3 (groundedness/relevance/tone) but every dim is `severity="advisory"`; the S-Eval-5 advisory-strip at `composite.py:228` removes them all → same 0.0 fallback. Four-run evidence (byte-identical across M-Auto-5 bracketing runs; NOT a regression): `m-auto-4-baseline-20260604` mean_judge=0.0 / L3 n=0; `m-auto-5-baseline-20260604` 0.0 / 0; `m-auto-5-baseline-20260605` 0.0 / 0; `results/20260604-152103/` (S-Auto-21 simfixed) 0.0 / 0. Older-run sanity: `results/20260425-083912/` mean_judge=0.5518 with L3 n=3 per case — judge worked when CaseSpecs carried critical-severity dims; the regression to 0 happened weeks before M-Auto-5 when bad_cases/anchor_outcome were authored with empty dim arrays + S-Eval-5 demoted shadow dims to advisory. **M-Auto-5 close decision**: judge layer EXCLUDED from paired-evidence review (canonical signal = composite + outcome + L1 + L2 `failure_tags` — these DO move and ARE the carriers of the simulator-fix signal); re-bless is NOT blocked; R-item stays LOW priority and NOT promoted to M-Auto-6 (judge is reporting-table-only on the human-review suites; canonical signal works). Future re-promotion trigger: a semantic-flexibility sprint that needs a working `groundedness` or similar gate. |

### Sprint 065 / S-Auto-10 surfaced R-items (2026-05-31)

S-Auto-10 first overnight batch surfaced FOUR OQs (handoff §9). Three are load-bearing for M-Auto-3 substrate / observability / proposer-quality scope and are formalized as R-items here per deliver-agent + human joint AskUserQuestion 2026-05-31 (recommended option: S65.1 + S65.2 + S65.3 as R-items; S65.4 ledger-only). Routing: M-Auto-3 candidate scope per S-Auto-10 §11 + M-Auto-2 §12 closure verdict Stage-2 direction (don't widen mutable surface yet — binding constraint is propose quality).

| id | source | description |
|----|--------|-------------|
| R-autoloop-run-sweeps-dirty-index | Sprint 065 / S-Auto-10 overnight 2026-05-31; `docs/sprints/sprint-065-handoff.md` §9 OQ-S65.1 evidence. Root cause: `autoloop run` per-exp git commit sweeps the **entire staged index** onto the first exp-branch commit (not just the target Skill YAML), then the checkout cycle (return to `auto-loop-branch`) reverts those staged changes from the working branch. When the autoloop session opens with uncommitted deliver-agent files in the staged index (commonly: close-prep docs / manifest updates / action_bank edits), the first overnight iter's exp-branch sweep + revert cycle DISCARDS those changes from the working branch. At S-Auto-10 the dev session recovered the bundle verbatim from `exp-20` commit `483c321` + committed at `9126c6f` to clean the tree before proceeding with overnight; substrate fix-in-place was not attempted (out of S-Auto-10 scope). **Status: proposed; impl candidate for M-Auto-3 substrate-hygiene sub-sprint** (M-Auto-3 candidate scope per S-Auto-10 §11 + M-Auto-2 §12). **Implementation paths (deliver-agent + research-agent at consumption time pick one):** (1) Pre-overnight assertion in `autoloop run` CLI: if `git status --porcelain` non-empty, FAIL with remediation message "stage + commit working tree before overnight, OR pass `--ignore-dirty`"; (2) Stash + restore pattern: at exp-branch creation, `git stash push --keep-index` to set aside uncommitted work, then `git stash pop` on return to `auto-loop-branch`; (3) Scope-limit the exp-branch commit to ONLY the target Skill YAML + autoloop result artefacts via explicit per-file `git add` instead of `git add -A` or implicit staged-index commit. **Reopen condition / prereq for resolution at M-Auto-3**: validated against a real overnight launch with intentionally-dirty working tree (deliver-agent staged docs); confirmed no working-tree state loss across the exp-branch lifecycle. **Severity**: load-bearing for future overnight launches with deliver-agent close-prep in flight (which IS the common workflow); also exp-20's branch is "poisoned" with the deliver bundle contents and must NEVER be cherry-picked. | `infra` / substrate hygiene (autoloop run + exp-branch lifecycle). **Status: proposed; impl deferred to M-Auto-3 substrate-hygiene sub-sprint candidate** (deliver-agent + human direction at S-Auto-10 close 2026-05-31; load-bearing for future overnight reliability; should NOT be deferred indefinitely). |
| R-overnight-eval-traces-not-persisted | Sprint 065 / S-Auto-10 overnight 2026-05-31; `docs/sprints/sprint-065-handoff.md` §5 + §9 OQ-S65.2 evidence. Root cause: the autoloop's eval phase invokes `eval_interactive` per iter against the alt-port backend, but the per-turn trace + per-case `case_results[].per_turn_trace[]` payloads are NOT persisted to `autoloop/results/runs/exp-<N>/` for downstream §5.6 manual review. Only the aggregated `verdict.layer_results` (5 layers; Tier 0-4 pass/fail/short-circuit) is persisted to `experiments.jsonl`. When deliver-agent + human attempt §5.6 manual review of overnight kept-candidates, the per-turn trace structure that §5.6 inspects is unavailable — they can only read the aggregated decision summary. At S-Auto-10 §5 review the kept-candidate slate was EMPTY so the impact was structural-only (no candidates to review); at FUTURE overnight runs with kept candidates, this is a HARD blocker for §5.6 trace review. **Status: proposed; load-bearing for the next overnight cycle that produces ≥1 kept candidate; impl candidate for M-Auto-3 observability sub-sprint** (M-Auto-3 candidate scope). **Implementation paths (deliver-agent + research-agent at consumption time pick one):** (1) Persist the full eval_interactive `results.json` (or equivalent canonical artefact) per iter to `autoloop/results/runs/exp-<N>/eval-results.json` alongside `verdict.json`; (2) Persist a compacted form retaining only `case_results[].per_turn_trace[]` + per-case verdicts (smaller storage; sufficient for §5.6); (3) Persist a pointer to the eval_interactive run-ID directory under `eval_interactive/results/<run-id>/` instead of duplicating storage (compact but couples autoloop result artefacts to eval_interactive result lifecycle). **Reopen condition / prereq for resolution at M-Auto-3**: validated against a real overnight run by deliver-agent + human jointly reading per-turn traces from kept-candidate `autoloop/results/runs/exp-<N>/` directly without consulting eval_interactive output. **Severity**: load-bearing for future M-Auto-3+ overnight that produces kept candidates (which IS the M-Auto-3+ proposer-quality milestone goal). | `infra` / observability hygiene (autoloop result artefact persistence). **Status: proposed; impl candidate for M-Auto-3 observability + proposer-quality sub-sprint scope** (deliver-agent + human direction at S-Auto-10 close 2026-05-31; load-bearing for future overnight kept-candidate §5.6 review). |
| R-tier1-bad-cases-regression-5-to-0-attribution-unverified | Sprint 065 / S-Auto-10 overnight 2026-05-31; `docs/sprints/sprint-065-handoff.md` §5 + §9 OQ-S65.3 evidence. Root cause: every Step-9-reaching iter in S-Auto-10's overnight batch discards at `tier1_bad_cases_regression_5_to_0` (13/13 iter; 0 kept; baseline shows ≥5 bad-cases passing pre-proposal, kept candidates would need ≥5 still passing post-proposal). The 5→0 attribution is structurally UNVERIFIED: it could mean (a) every proposed Skill YAML edit causes ALL 5 baseline-passing bad-cases to FAIL — a strong propose-quality signal indicating systematic proposer regression; OR (b) the tier1 outcome calculation has a bug that returns 0 regardless of proposal content — an infrastructure signal that would invalidate the 0-keep observation; OR (c) the propose-distribution covers a narrow Skill YAML surface that uniformly bricks bad-case execution — a propose-distribution signal. Without per-iter eval trace persistence (per `R-overnight-eval-traces-not-persisted`), distinguishing (a)/(b)/(c) requires either (i) inspecting `autoloop/results/runs/exp-<N>/` per-iter Skill YAML diff + manually rerunning the affected bad-cases against the diffed Skill YAML, OR (ii) instrumenting the tier1 calculation to log per-case PASS/FAIL contributions. **Status: proposed; load-bearing for understanding why 0-keep is structural; impl candidate for M-Auto-3 proposer-quality + observability scope** (M-Auto-3 candidate scope). **Implementation paths (deliver-agent + research-agent at consumption time pick one):** (1) Per-iter tier1 calculation instrumentation: log per-case PASS / FAIL contribution to `verdict.layer_results[1].evidence` (currently abridged); (2) Manual reproduction: deliver-agent + human pick 1-2 sample iter from overnight, manually rerun the iter's proposed Skill YAML against the bad-cases suite, verify pass count + identify failure pattern; (3) Hybrid (1) + (2). **Reopen condition / prereq for resolution at M-Auto-3**: deliver-agent + human jointly determine whether (a)/(b)/(c) hypothesis is true; if (a) confirmed (real propose regression), route to proposer-quality scope; if (b) (calc bug), route to tier1 calculation fix; if (c) (narrow surface), route to propose-distribution widening scope. **Severity**: load-bearing for interpreting the 0-keep result; without this, the 0-keep signal cannot be definitively interpreted as "proposer issue" vs "tier1 calc issue" vs "narrow propose-distribution". This R-item GATES the Stage-2 entry direction decision (per S-Auto-10 §11 + M-Auto-2 §12 closure verdict — Stage-2 deferred to AFTER M-Auto-3). | `infra` + `eval_spec` / verification + observability (tier_evaluator tier1 calc semantics + per-iter eval trace propagation). **Status: proposed; impl candidate for M-Auto-3 proposer-quality + observability sub-sprint scope** (deliver-agent + human direction at S-Auto-10 close 2026-05-31; gates Stage-2 entry direction decision per S-Auto-10 §11 dev recommendation). |

### Sprint 065 / S-Auto-10 bad_cases trace-dive surfaced R-items (2026-06-01)

Post-S-Auto-10 bad_cases baseline-robustness investigation (sim_temp=0 + bot_temp=0 + 60s deadline + parallel=1, 3 fresh passes on `eval_interactive/case_specs/bad_cases/`, runs `20260601-070530` / `20260601-071402` / `20260601-071943`) produced pass-counts 6/4/5 (range=2, median=5) — **identical to the temp=0.7 baseline**, demonstrating that simulator/bot temperature reduction did NOT close the residual variance. Per-case trace-audit across the 8 flipping cases × 3 passes = 24 case-runs (`/tmp/badcases3pass/trace_audit.py`) identified SEVEN recurring architectural failure patterns that explain the residual noise and structurally cannot be remediated within the autoloop's mutable surface (Skill YAML LLM-soft fields per `docs/proposals/autoloop_design.md`). The patterns are documented as R-items here for M-Auto-3 substrate-hygiene / runtime-fix sub-sprint candidate scope; they are the binding constraint on bad_cases gating stability AND on the interpretability of autoloop fitness signal. Routing per deliver-agent + human joint AskUserQuestion 2026-06-01 (recommended option: catalog all 7 as R-items; route to deliver-agent for milestone planning; deliver-agent picks research-agent dispatch per-R-item at consumption time). Bug counts across 24 case-runs: IDENTICAL_RETRY × 15, PARAPHRASE_STORM × 11, UC_MISCLASS × 8, ESCALATION_MISSTAMP × 5, TURN_BUDGET_HIT × 4, GATING_RACE × 3, CONTRACT_VIOL_TURN0 × 3.

**Key finding — verdict (PASS/FAIL) is not strongly correlated with bug occurrence within the same case**: across 8 flipping cases, 4 cases have bugs that appear ONLY in PASS runs (the bug is "did a bunch of useless work but accidentally landed on expected_outcome"), 3 cases have bugs that appear ONLY in FAIL runs, the rest are mixed. Implication: the 6/4/5 number does not represent true bot capability — it represents "bot does X pathological things, and case_spec expected_outcome is lenient enough that ⅔ of the dice rolls land in the spec's window." The autoloop's `tier1_bad_cases_regression_5_to_0` gate (per `R-tier1-bad-cases-regression-5-to-0-attribution-unverified`) is therefore measuring noise dominated by these 7 runtime-layer bugs, not propose-quality.

| id | source | description |
|----|--------|-------------|
| R-runtime-identical-tool-call-retry-storm | Sprint 065 / S-Auto-10 bad_cases trace-dive 2026-06-01; `/tmp/badcases3pass/trace_audit.py` output (15/24 case-runs across 8 flipping bad_cases). Root cause: the runtime tool dispatcher does NOT deduplicate identical-args tool calls within a single agent turn. The LLM frequently emits the same tool_name + identical arguments 2–6 times consecutively (e.g., `wmkb` p1 turn 0 emits `search_knowledge(query="change account from trader to private seller", uc_tags=["UC-D"])` three times back-to-back; `cs014` p2 turn 1 emits `get_customer_context(ad_id=..., email=...)` six times with identical args returning identical `account_found=false`; user-pasted `6f9645dc...` session emits `get_customer_context` six times). Each call consumes a tool budget step + a wall-clock latency hit and produces identical results. The retry storms inflate per-turn step counts past max_steps, triggering `bot_ended esc=turn_budget_exhausted` or `bot_ended esc=faq_miss_threshold_exceeded` (the latter via `R-runtime-escalation-reason-misstamp-maxsteps-faq` mis-stamp). **Status: proposed; M-Auto-3 substrate-hygiene sub-sprint candidate**. **Implementation paths (deliver-agent + research-agent at consumption time pick one):** (1) Runtime per-turn cache: hash `(tool_name, normalized_args)` per agent turn; on duplicate-key dispatch, return the cached `result` + emit a `warning` field in the trace and DO NOT charge a step. (2) LLM-side: add a per-turn projection field naming "tools already called this turn with args" so the LLM can self-deduplicate. (3) Hybrid: cache results (1) AND inject the already-called list into projection (2) — projection is the soft signal, cache is the deterministic backstop. **Reopen condition / prereq for resolution at M-Auto-3**: rerun bad_cases 3 passes after fix; expect the IDENTICAL_RETRY count to drop from 15/24 to ≤2/24 and per-case turn counts to converge (less divergence between PASS/FAIL paths). **Severity**: high — 15/24 case-runs affected; this is the dominant signal corruption source on bad_cases. **Layer**: `infra` (runtime tool dispatcher); NOT a Tier-0 invariant addition (no Tier-0 in `docs/runtime_freeze_and_risk_policy.md` § 1 / § 2 covers tool-call cardinality); `human_review_required` per § 3.2 if a Tier-0 elevation is debated. | `infra` / runtime tool dispatcher. **Status: proposed; M-Auto-3 substrate-hygiene sub-sprint candidate**. |
| R-runtime-paraphrase-storm-search-knowledge | Sprint 065 / S-Auto-10 bad_cases trace-dive 2026-06-01; `/tmp/badcases3pass/trace_audit.py` (11/24 case-runs). Root cause: the LLM emits `search_knowledge` ≥3 times in a single turn with paraphrased queries when the first retrieval returns hits but the LLM judges them insufficient — example `cs014` p3 turn 0 issues `search_knowledge` 5 times with queries "X", "rephrased X", "another rephrasing", "yet another", etc.; `wmkb` p1 turn 0 issues 6 paraphrased queries. Distinct from `R-runtime-identical-tool-call-retry-storm` (which is exact-string duplicates) — paraphrase storm is semantically-distinct queries against the same retrieval surface. The storms consume the turn budget and (combined with the escalation mis-stamp pattern) lead to `bot_ended esc=faq_miss_threshold_exceeded` even when individual searches returned `faq_miss=false` with grounded hits. **Status: proposed; M-Auto-3 candidate; possibly autoloop-scope-touchable per (2) below**. **Implementation paths (deliver-agent + research-agent at consumption time pick one):** (1) Runtime gate: hard-cap `search_knowledge` at 2 calls per turn unless prior call's `faq_miss=true`. (2) Skill prompt edit (AUTOLOOP-SCOPE): add to the `$.procedure` or `$.grounding_instruction` field — "if a `search_knowledge` call returns hits with `faq_miss=false`, do not call `search_knowledge` again this turn — propose a reply grounded on the existing hits or escalate"; this is reachable by the autoloop's mutable surface. (3) LLM-projection: surface the prior turn's search_knowledge results explicitly in the per-turn projection so the LLM does not re-search to confirm what it already retrieved. **Reopen condition / prereq for resolution at M-Auto-3**: rerun bad_cases 3 passes; expect PARAPHRASE_STORM to drop from 11/24 to ≤3/24; for autoloop-scope skill-prompt fix (path 2), the autoloop fitness improvement should be measurable against the pre-fix baseline. **Severity**: high — 11/24 case-runs; combined with the escalation mis-stamp it is the primary driver of `faq_miss_threshold_exceeded` mis-escalations. **Layer**: `semantic_planner` (LLM choice to over-retry) OR `prompt_projection` (skill prompt fails to constrain) OR `infra` (runtime hard cap). Decision routing per § 3.2 — for soft-signal-first per Constitution § 1.5, prefer path (2) skill prompt + path (3) projection before considering path (1) hard cap. | `semantic_planner` + `prompt_projection` + `infra` (mixed; autoloop-scope path is path-2 skill prompt). **Status: proposed; M-Auto-3 candidate; autoloop-touchable surface via path (2)**. |
| R-runtime-tool-gating-race-uc-none | Sprint 065 / S-Auto-10 bad_cases trace-dive 2026-06-01; `/tmp/badcases3pass/trace_audit.py` (3/24 case-runs at trace audit + 1 in user-pasted session `6f9645dc...`). Root cause: when `active_use_case='none'` (i.e. classify_use_case has not yet committed), the LLM may emit `search_knowledge` or `get_customer_context` in the same turn as `classify_use_case`, and the runtime dispatcher allows the non-classify tool to execute before classify commits — resulting in `Tool 'search_knowledge' is not allowed for use case 'none'` errors. Examples: `cs012` p2 turn 0 (`get_customer_context` blocked before classify); `cs015` p3 turn 0 (`search_knowledge` blocked); `fg5q` p1 turn 0 (`search_knowledge` blocked); user-pasted `6f9645dc...` turn 1 same pattern. The error wastes a step and forces the LLM to retry on the next turn, often triggering escalation mis-stamps downstream. **Status: proposed; M-Auto-3 substrate-hygiene sub-sprint candidate**. **Implementation paths (deliver-agent + research-agent at consumption time pick one):** (1) Dispatcher serialize-and-commit: when `active_use_case='none'`, scan the LLM's tool_calls for `classify_use_case`; if present, execute classify FIRST + commit `active_use_case`, then execute the remaining tools with the new gating context. (2) Reject-and-retry: when `active_use_case='none'` AND non-classify tools are emitted alongside classify, reject the entire turn's tool_calls with a structured error projection field "classify_use_case must complete before other tools — retry this turn with classify-only" so the LLM self-corrects. (3) Per-tool gating reclassification: change the tool's permission table so the affected tools are permitted in `use_case='none'` (loosen gating); evaluate semantic risk before adopting. **Reopen condition / prereq for resolution at M-Auto-3**: rerun bad_cases 3 passes; expect GATING_RACE to drop from 3+1=4 to ≤1; validate no new tool-misuse cases emerge (negative-control). **Severity**: medium — 3/24 + 1 user-pasted; not the dominant driver but a clean fix candidate with low side-effect risk. **Layer**: `infra` (runtime tool dispatcher). | `infra` / runtime tool dispatcher pre-classify gate. **Status: proposed; M-Auto-3 substrate-hygiene sub-sprint candidate**. |
| R-runtime-escalation-reason-misstamp-maxsteps-faq | Sprint 065 / S-Auto-10 bad_cases trace-dive 2026-06-01; `/tmp/badcases3pass/trace_audit.py` (5/24 case-runs). Continuation of the pattern in user-memory `project_faq_overescalate_maxsteps_misstamp.md`: Sprint 39 added a guardrail closing the LLM-self-stamp vector ("LLM declares `escalation_reason='faq_miss_threshold_exceeded'` while `faq_miss=false`"); the runtime resolver path (`resolveMaxStepsReason` or equivalent in `ChatController` / `LlmInvocationService`) on loop exhaustion is the SURVIVING vector and still emits `escalation_reason='faq_miss_threshold_exceeded'` when max_steps is hit, regardless of whether the last `search_knowledge.faq_miss=false`. Examples in this trace-dive: `cs001` p2 (esc=`faq_miss_threshold_exceeded`, last search returned `faq_miss=false` with grounded hits); `cs014` p1 + p3 (same); `cs095` p2 (same); `wmkb` p1 + p2 (same). Indistinguishable from `R-runtime-identical-tool-call-retry-storm` / `R-runtime-paraphrase-storm-search-knowledge` downstream — the storms exhaust max_steps and the resolver mis-stamps the reason. **Status: proposed; M-Auto-3 substrate-hygiene sub-sprint candidate**. **Implementation paths (deliver-agent + research-agent at consumption time pick one):** (1) Add reason-coercion guard at the resolver: if max_steps is hit AND last `search_knowledge.faq_miss=false`, stamp `escalation_reason='max_steps_exhausted'` instead of `faq_miss_threshold_exceeded`. (2) Track `last_faq_miss` flag in the session state explicitly + read at resolver time. (3) Stop coercing at the resolver entirely — pass the cause to the LLM via projection and let the LLM provide the reason on the final summary turn; if no LLM intent recorded, stamp a literal `max_steps_exhausted` without semantic claims. **Reopen condition / prereq for resolution at M-Auto-3**: rerun bad_cases 3 passes; expect ESCALATION_MISSTAMP to drop from 5/24 to ≤1/24; CaseSpec `expected_outcome` validation should distinguish `max_steps_exhausted` from `faq_miss_threshold_exceeded` (this is an `eval_spec` companion task). **Severity**: high — corrupts the autoloop's escalation-correctness signal AND case_spec mapping. **Layer**: `infra` (escalation-reason resolver) + companion `eval_spec` schema for the new reason value. | `infra` / runtime escalation-reason resolver; companion `eval_spec` for the new reason value. **Status: proposed; M-Auto-3 substrate-hygiene sub-sprint candidate; extends Sprint 39 partial fix**. |
| R-runtime-escalation-reason-turn-budget-conflated-with-intent | Sprint 065 / S-Auto-10 bad_cases trace-dive 2026-06-01; `/tmp/badcases3pass/trace_audit.py` (4/24 case-runs). Root cause: `escalation_reason='turn_budget_exhausted'` is emitted by the runtime resolver as a category distinct from LLM-intent escalation (`user_requested`, `mechanical_template`, `faq_miss_threshold_exceeded` when faq miss is real), but the case_spec's `expected_outcome=escalate` evaluation treats turn-budget-triggered escalations as semantically equivalent to LLM-intent escalations, which inflates the case_passed count for cases where the bot escalated because it ran out of turns rather than because it identified a need to escalate. Examples: `cs001` p3 (esc=`turn_budget_exhausted`, passed=True — but the bot did not "intentionally" escalate, it just ran out); `cs012` p1 (same); `cs014` p2 (esc=`turn_budget_exhausted`, passed=False — opposite outcome); `cs015` p2 (same). The non-correlation between `turn_budget_exhausted` and PASS/FAIL within the same case (see Pattern section above) indicates this is a case_spec validation surface issue, not a runtime behaviour issue per se. **Status: proposed; M-Auto-3 candidate; `eval_spec`-companion fix**. **Implementation paths (deliver-agent + research-agent at consumption time pick one):** (1) Case_spec schema additions: `expected_outcome` becomes a structured field `{outcome: escalate|resolve, valid_reasons: [user_requested, faq_miss_threshold_exceeded, ...]}` — `turn_budget_exhausted` is explicitly NOT a valid reason for cases that expect intent-driven escalation. (2) Runtime classification: split `escalation_reason` namespace into `intent_escalation` (LLM-driven) vs `runtime_escalation` (resolver-driven max_steps / loop_detected); case_spec validates against the appropriate namespace. (3) Status quo + per-case_spec opt-in `allow_turn_budget=true` field for cases where running out of turns is acceptable. **Reopen condition / prereq for resolution at M-Auto-3**: rerun bad_cases 3 passes after case_spec adjustment; expect TURN_BUDGET_HIT to remain visible (the runtime behavior is unchanged) but its correlation with case_passed to become deterministic (either always-PASS or always-FAIL per case_spec). **Severity**: medium — 4/24 case-runs; corrupts case_spec interpretability but not the autoloop layer-evaluator directly. **Layer**: `eval_spec` (case_spec schema) + `infra` (escalation-reason namespace if path 2). | `eval_spec` (primary) + `infra` (if escalation-reason namespace split). **Status: proposed; M-Auto-3 candidate; eval_spec-companion to escalation-reason cleanup**. |
| R-simulator-first-message-contract-violation-flake | Sprint 065 / S-Auto-10 bad_cases trace-dive 2026-06-01; `/tmp/badcases3pass/trace_audit.py` (3/24 case-runs: `cs015` p1, `fg5q` p2, `fg5q` p3). Root cause: the moonshot simulator (`eval_interactive/eval_interactive/simulator/user_simulator.py` calling `moonshot-v1-32k` chat completion via OpenAI-compatible API at `simulator_temperature=0.0`) occasionally produces a first-message output that violates the simulator response contract (`_parse_simulator_response` rejects with malformed JSON or missing fields), resulting in `stop_reason='contract_violation'` + `total_turns=0` before any bot turn runs. Persists at `simulator_temperature=0.0` — `temperature=0` does not guarantee bit-exact determinism on shared-pool OpenAI-compatible inference (fp16 batching variance). The simulator's two-attempt retry (`_call_llm` retries once) does not catch this — both attempts may produce the same malformed shape. The fallback message ("I'm still waiting for help with my issue.") only fires if BOTH attempts throw an exception; a malformed-but-parseable response slips through. **Status: proposed; M-Auto-3 candidate; simulator-side robustness scope**. **Implementation paths (deliver-agent + research-agent at consumption time pick one):** (1) Strengthen `_parse_simulator_response` validation: on parse failure, retry up to N=3 times with an explicit "your prior response was malformed, here is the schema you must produce" follow-up message. (2) Pin a deterministic-by-construction first-message override per case_spec: bypass the simulator's LLM call entirely on turn 0; use the case_spec's seeded `persona.seed_messages[0]` or `form_context.description` — the simulator code at `user_simulator.py:107-110` already prefers seed_messages over LLM call when seed_messages is present, but cases without seed_messages fall through to the LLM call. (3) Hybrid (1) + (2) — deterministic seed message on turn 0 + strengthened parse-retry on later turns. **Reopen condition / prereq for resolution at M-Auto-3**: rerun bad_cases 3 passes; expect CONTRACT_VIOL_TURN0 to drop from 3/24 to 0/24. **Severity**: medium — 3/24 case-runs; the 3 cases are predictable (cs015, fg5q) so easy to repro and validate. **Layer**: not a standard § 3.2 layer (simulator-side); closest fit is `eval_spec` / harness robustness OR `infra` (if framed as eval-pipeline infrastructure). | `eval_spec` / harness robustness (simulator first-message contract). **Status: proposed; M-Auto-3 candidate; simulator-side robustness scope**. |
| R-classifier-non-deterministic-uc-selection-at-temp-zero | Sprint 065 / S-Auto-10 bad_cases trace-dive 2026-06-01; `/tmp/badcases3pass/trace_audit.py` (8/24 case-runs total, but several are by-design — e.g. `wmkb_uc_a_trader_flag_secondary_uc_h` is intentionally testing recovery from UC-D misclassification; the load-bearing subset is 2 cases where the SAME case yields DIFFERENT UC classifications across the 3 passes at bot_temp=0). Root cause: `classify_use_case` returns different UC values for the same case across deterministic-temperature reruns — example `cs012_uc_fp_late_phone_failure_path` classifies as UC-B (p2) vs UC-A (p3) for identical session inputs; `cs015_uc_fp_appeal_edit_repost` classifies as UC-H (p2) vs UC-A (p3). Bot temperature=0.0 is set at `LlmRequest.java:19` (default) + `LlmInvocationService.java:114` (main agent) + `:267` (intake/classification). `temperature=0` does not guarantee bit-exact determinism on shared-pool OpenAI-compatible inference (deepseek-v4-flash) — same vendor-nondeterminism root as `R-simulator-first-message-contract-violation-flake` on the bot side. **Status: proposed; M-Auto-3 candidate; bot-side robustness scope**. **Implementation paths (deliver-agent + research-agent at consumption time pick one):** (1) Add a session-level classify cache: once `classify_use_case` commits for a session, the value is sticky across turns; eliminates intra-session classifier flips. (2) Distribution-aware classification: if classify_use_case confidence < threshold (e.g. 0.85), surface the alternates as a soft signal in the projection (per `prompt_projection` layer) so the LLM can disambiguate; bounds the impact of classifier instability rather than eliminating the underlying vendor noise. (3) Hybrid (1) + (2) — sticky commit within a session + alternate-candidate projection when confidence is low. **Reopen condition / prereq for resolution at M-Auto-3**: rerun bad_cases 3 passes; expect intra-session classifier output to be sticky (no per-turn flips) and the broader flip envelope to narrow. **Severity**: medium — 2/24 truly nondeterministic-classifier cases (small but a load-bearing source of the broader 8-flipping-case noise envelope). **Layer**: `semantic_planner` (classifier choice) + `prompt_projection` (soft signal if path 2). | `semantic_planner` (classifier behavior) + `prompt_projection`; autoloop-touchable via the projection path if the alternate-candidate slot is exposed. **Status: proposed; M-Auto-3 candidate; bot-side robustness scope; companion to R-simulator-first-message-contract-violation-flake**. |

### Sprint 066 / S-Auto-11 surfaced OQs (2026-06-01)

S-Auto-11 (eval-harness robustness + per-iter trace persistence + infra-error detection; handoff `docs/sprints/sprint-066-handoff.md` §6) surfaced FOUR OQs. None are fixed in S-Auto-11 (all out of its `infra`/harness scope); routing decided by deliver-agent + human at S-Auto-11 close 2026-06-01.

- **OQ-S66.1 — Java baseline golden drift (load-bearing; reconcile in S-Auto-13/14).** The true post-`b351648` Java baseline is `1183 / 10 / 0 / 2`, NOT the stated `1183 / 1 / 0 / 2`. The 9-failure delta is pre-existing `PhaseEvaluator max_tool_steps` golden-count drift (`resolve_faq_grounded_answer.yaml max_tool_steps: 6` vs test golden asserting `4`; `discover_triage.yaml max_tool_steps: 3` vs golden `2`), NOT determinism-attributable (`b351648` changed only `ChatController` deadline + `LlmRequest`/`LlmInvocationService` temperature). The skill-resource + test-file changes that drive these counts predate `b351648` (red since ~Sprint 44-52). Determinism config correctly NOT reverted (step-1 STOP trigger did not fire). Hard-fenced surfaces (skills YAML = A2/A3 / S-Auto-13; Java goldens = `server/src/test`). **Action**: reconcile goldens-vs-shipped `max_tool_steps` (pick one source of truth) when S-Auto-13 edits `discover_triage.yaml` and/or S-Auto-14 edits `PhaseEvaluator`; until then `1183/10/0/2` is the held reference (the 9 are NOT a new regression). **Status: ledgered; reconcile in S-Auto-13/14.**

- **OQ-S66.2 — `CONTRACT_VIOL_TURN0` is bot-side, not a `user_simulator` turn0 issue (load-bearing; re-scopes a milestone §11 metric).** The D1 hypothesis (turn0 falls through to a moonshot LLM call → flake) is falsified by code + data: `generate_first_message` was ALREADY deterministic for both seed and no-seed branches (it never called the LLM), and both target cases (`cs015`, `fg5q`) carry `seed_messages`. The real flake is a bot-side `CONTRACT_VIOLATION:active_use_case` (`reason=missing_after_turns`, `total_turns≈0`): the bot's first turn aborts before stamping a UC (`eval_interactive/.../trace/collector.py:367` raises it in strict mode). This is the OQ-S65.7/8 "infra masquerading as signal" phenomenon at the eval-trace-contract layer (first-turn LLM-deadline / give-up). No `user_simulator.py` edit can drive it to 0; S-Auto-11 shipped SAFE D1 hardening (N=3 parse-retry + non-empty turn0 guard; human-authorized "safe hardening + surface") and #4 infra-error detection now CORRECTLY classifies `cs015`/`fg5q` as infra-degraded (so they no longer masquerade as a Tier-0 fitness regression). The raw-count→0 fix is bot-side (candidate: S-Auto-13 A2 first-turn classify-first discipline) and/or eval-trace-contract leniency (OQ-S66.4). **Status: proposed; re-scope the §11 `CONTRACT_VIOL_TURN0 3/24→0` metric at S-Auto-13 planning (bot-side owner); D1 portion DONE as safe hardening.**

- **OQ-S66.3 — eval_interactive baseline drift (ledger-only).** The true pre-change baseline is `485 passed / 4 failed`, NOT the stated `486 passed / 3 failed`. The 4th pre-existing failure is `test_session_create_timeout_constants_widened_to_120s` (`assert 90.0 == 60.0`, a timeout-constant drift) alongside the 3 known (`test_case_spec_overrides.py` ×2, `test_corpus_lint.py` ×1); verified by stashing the S-Auto-11 changes on a clean tree. After D1's +14 tests the suite is `499 passed / 4 failed`. **Status: ledgered; baseline corrected to `499/4` (was stated `486/3`).**

- **OQ-S66.4 — eval-trace-contract leniency for first-turn-abort (optional; needs authorization).** Consider treating `active_use_case missing_after_turns` with `total_turns≈0` as an infra-error / lenient outcome in `eval_interactive/.../trace/collector.py` rather than a hard `CONTRACT_VIOLATION` — mirrors S-Auto-11 #4 at the `eval_interactive` layer and would stop the OQ-S66.2 masquerade at its source. Outside D1's in-scope file; flagged for deliver-agent + human. **Status: proposed (optional); needs human authorization before scoping.**

**R-items addressed by S-Auto-11 (NOT flipped to closed until M-Auto-3 milestone close per §7 routing):**

- `R-overnight-eval-traces-not-persisted` → **addressed by S-Auto-11 #3** (per-iter eval traces now persisted to `autoloop/results/runs/exp-<N>/eval-results.json` via `loop.py` orchestration; read-back verified live; no SHA-locked file touched). CLOSE candidate at M-Auto-3 close.
- `OQ-S65.7` / `OQ-S65.8` (infra-degradation must not masquerade as fitness) → **addressed by S-Auto-11 #4** (`loop.py` infra-error detection marks LLM-deadline/`service_degraded`/failed-eval iters `decision="error"` + `infra_error_reason`, skipping `tier_evaluator`; negative control proves genuine FAILs still reach the tier evaluator). CLOSE candidate at M-Auto-3 close.
- `R-simulator-first-message-contract-violation-flake` (D) → **partially addressed** (safe `user_simulator` hardening shipped) but the contract-violation ROOT is bot-side (OQ-S66.2), so this R-item's "→0" closure criterion moves to the bot-side owner. Status remains `proposed`.


### Sprint 067 / S-Auto-12 surfaced OQs (2026-06-01)

S-Auto-12 (A1 identical-retry-storm dedup; handoff `docs/sprints/sprint-067-handoff.md` §6) surfaced two OQs. Neither is a defect; both are observations for the M-Auto-3 close measurement.

- **OQ-S67.1 — the deterministic 回挡 is load-bearing (hybrid confirmed, not a defect).** Across the 3 post-change passes the backstop fired 16× (1+6+9) — i.e. the bot LLM still ATTEMPTS byte-identical repeats even with the binding `already_called` soft signal in the prompt (e.g. cs095 turn1 re-emitted `search_knowledge` 4×; cs015 turn4 3×). Unmitigated `IDENTICAL_RETRY` was 0/89 bot-turns only because the §1.4 idempotency 回挡 caught the remainder — soft-signal-ALONE would have leaked all 16 (the Sprint-19/20 failure mode). No action; confirms red line #2. A FUTURE prompt-projection refinement (surface the dedup hit back to the LLM more loudly so it re-emits less, raising `planner_ownership_ratio`) is a separate prompt-tuning item, NOT A1. **Status: observation; no R-item.**

- **OQ-S67.2 — A1 does NOT close the downstream max-steps/escalation mis-stamp (B1/S-Auto-14 owns it).** A1 stops the storm from CONSUMING steps, but the max-steps/budget-exhaustion → escalation-reason mis-stamp lives in `PhaseEvaluator.resolveMaxStepsReason` (hard-fenced from S-Auto-12; B1 = S-Auto-14). Whether eliminating the storm materially lowers max-steps EXITS (and thus mis-stamp frequency) should be re-measured at M-Auto-3 close once B1 lands. Links to `project_faq_overescalate_maxsteps_misstamp` + `R-runtime-escalation-reason-misstamp-maxsteps-faq` (B1 consumer). **Status: expected sequencing; closed by B1/S-Auto-14 + M-Auto-3 close re-measurement.**

**Measurement caveat (for the §11 close measurement):** the documented historical `IDENTICAL_RETRY 15/24` was a PRE-determinism-config trace-dive; on the current `b351648` harness the pre-A1 residual storm was ~3-5 unmitigated turns per pass (4/30, 5/32, 3/34), and A1 took it to 0/25, 0/34, 0/30. The §11 "15/24→≤2" target is MET (0), but the close measurement should cite the same-harness before/after, not the stale 15/24.

**R-item addressed by S-Auto-12 (NOT flipped to closed until M-Auto-3 milestone close per §7 routing):**

- `R-runtime-identical-tool-call-retry-storm` (A1) → **addressed by S-Auto-12** (hybrid dedup: per-run `(toolName, canonicalArgumentsHash)` idempotency 回挡 serving `success==true` byte-identical repeats from cache without re-dispatch/budget + `already_called` observation→binding soft-signal upgrade; `IDENTICAL_RETRY` 0 across 3 passes; trace-annotated `deduplicated`/`original_at_step`). Successor to `R-runtime-orchestrator-tool-call-deduplication` write-side. CLOSE candidate at M-Auto-3 close.

### Sprint 068 / S-Auto-13 surfaced OQs (2026-06-02)

S-Auto-13 (A2 classify-first + A3 paraphrase discipline + OQ-S66.1 goldens; handoff `docs/sprints/sprint-068-handoff.md`) closed PARTIAL: A2 + OQ-S66.1 met; the A3 soft layer shipped + fires but its target was not met (the model ignores the soft signal) → the A3 deterministic backstop is deferred to the new S-Auto-13b. Four OQs surfaced.

- **OQ-S68.1 — eval_interactive baseline drift from the 2026-06-01 action_bank split (`0323457`), NOT from S-Auto-13.** The true current eval_interactive baseline is **`495 passed, 8 failed`**, not the stated `499/4`. The +4 delta is `0323457` (the closed-index relocation to `docs/action_bank_archive.md`): 4 governance/lint tests assert on `action_bank.md` rows that moved to the archive. Disjoint from S-Auto-13's server-only changes (dev-verified). **Action**: a small housekeeping fix — point the 4 tests at `action_bank_archive.md` (or relax them) — OR accept `495/8` as the corrected baseline. Human owns (it is `0323457` fallout). **Status: proposed; quick-fix-or-accept decision needed before the M-Auto-3 close "Python baselines preserved" gate.**

- **OQ-S68.2 — RESOLVE-phase 'none' rejections are a separate "UC not committed" symptom.** Distinct from the DISCOVER gating-race A2 closed: some `search_knowledge` rejections occur in RESOLVE because `active_use_case` is not committed at that point ("UC not committed"), a different root cause than the DISCOVER classify-first ordering. A2 dropped RESOLVE-phase 'none' rejections 5→0 incidentally, but the underlying symptom may recur. **Status: observation; revisit if it resurfaces (candidate B-family / classifier-stability).**

- **OQ-S68.3 — A3 paraphrase-storm soft signal insufficient (→ S-Auto-13b backstop).** The soft `faq_miss`-keyed grounding instruction + projection echo are correct and demonstrably FIRE (15-17 `search_reuse_instruction`/run reach the LLM per-step before each re-search), but `deepseek-v4-flash` ignores the soft signal — `PARAPHRASE_STORM` stayed 16/16/7 vs the ≤3 target. Per the S-Auto-13 contract (soft-signal-first; surface before any hard cap), no hard cap was added and the A3 soft layer is kept as the correct first measure. **Routed to S-Auto-13b / Sprint 069** (faq_miss-state-aware same-turn re-search suppression backstop; per-sub-sprint Codex). **Status: open; S-Auto-13b consumes it.**

- **OQ-S68.4 — a byte-identical re-search escaped S-Auto-12 A1's dedup.** A1's per-run `(toolName, canonicalArgumentsHash)` 回挡 did not catch one byte-identical `search_knowledge` re-search (A1 was fenced in S-Auto-13). Likely a scope boundary (A1's cache is per-`AgentRunLoop.run`; a re-search in a different turn/run is by-design not deduped) OR a genuine gap. The S-Auto-13b `faq_miss`-state gate (same dispatch path) may incidentally subsume it. **Status: observation; verify in S-Auto-13b or at milestone close whether it is a real A1 gap vs by-design per-turn scope.**

**Addressed by S-Auto-13 (NOT flipped to closed until M-Auto-3 milestone close per §7 routing):**

- `R-runtime-tool-gating-race-uc-none` (A2) → **addressed**: `discover_triage.yaml` classify-first (the `faq-uc-search-before-commit` critical step reconciled in place to `faq-uc-classify-first`; count invariant 3/18 preserved; `tool-policy.yaml` untouched); DISCOVER gating-race 0/1/0 across 3 passes (aggregate 1 ≤ target 1). CLOSE candidate at M-Auto-3 close.
- `R-runtime-paraphrase-storm-search-knowledge` (A3) → **PARTIALLY addressed** (soft layer shipped + fires) but target not met (OQ-S68.3); the deterministic backstop is S-Auto-13b. Status remains `proposed`.
- **OQ-S66.1 (Java golden drift) → RESOLVED**: the stale `max_tool_steps` goldens (`resolve_faq_grounded_answer` 4→6, `discover_triage` 2→3) were a stale-value sync from commit `7871c62` (NOT an intended cap); the `PhaseEvaluator` golden tests were updated to the shipped YAML values. Java `Failures 10 → 1` (only the inherited `SystemPromptUserRequestedTiebreakerTest` per OQ-S41.5 remains); +6 new A2/A3 tests → `Tests run 1192`.

### Sprint 069 / S-Auto-13b surfaced OQs (2026-06-02)

S-Auto-13b (A3 paraphrase-storm deterministic backstop — a `faq_miss`-state-aware same-turn `search_knowledge` re-search suppression gate in `AgentRunLoopImpl`, ALONGSIDE the S-Auto-12 A1 回挡 and keyed purely on the EXISTING `faq_miss` result flag, NOT on query content; handoff `docs/sprints/sprint-069-handoff.md`) closed **CLEAN on its own contract** (the within-turn surface). Per-sub-sprint Codex `pass / 0 / approve` across all 10 axes (`docs/sprints/sprint-069-codex-review.md`); deliver-agent independently re-ran Java → `1198 / 1 / 0 / 2` (sole failure = inherited `SystemPromptUserRequestedTiebreakerTest`). One OQ surfaced; two inherited OQs dispositioned.

- **OQ-S69.1 — cross-turn paraphrase storm (OUT of the per-run gate's scope by design).** The S-Auto-13b gate has a `run()`-local (one bot turn) lifetime, so it catches WITHIN-turn paraphrase re-searches (12→0 across 3 passes) but cannot catch a paraphrase the LLM issues in a SUBSEQUENT bot turn referencing a prior turn's viable hit (cross-turn 21→22, unchanged; total 33→22). A fix would lift the tracker out of `run()`-local state into `BotSession`-scoped state (same `infra` layer, same `faq_miss` result-state key, no semantic-content change). **Does NOT auto-insert a sub-sprint**: M-Auto-3 is at the §8.1 5-sub-sprint ceiling, so pursuing cross-turn suppression requires a §8.5 split decision (deliver-agent + human), preferentially split to **post-M-Auto-3 / M-Auto-4**, NOT crammed into the current milestone. **Status: open; carrier decision at M-Auto-3 close (accept residual / split to M-Auto-4 / §8.5).**

- **OQ-S68.3 (A3 soft signal insufficient) → RESOLVED (within-turn surface) by S-Auto-13b.** The deterministic backstop closes the within-turn paraphrase storm the soft layer could not (soft layer stays in place beneath it, per soft-signal-first §1.5). The cross-turn residual is OQ-S69.1 (a separate surface).
- **OQ-S68.4 (a byte-identical re-search escaped A1) → LOGICALLY SUBSUMED by S-Auto-13b.** The A3 gate keys on `faq_miss` result-state, not on the args hash, so any future byte-identical `search_knowledge` re-search after a viable hit this run is ALSO caught by A3 — not actively triggered on this run (A1 caught all 20 byte-identical events). No separate fix required; close-out at M-Auto-3 close.

**Milestone §11 PARAPHRASE_STORM interpretation (deliver-agent + human decision, recorded for M-Auto-3 close):** the §11 unlock criterion `PARAPHRASE_STORM 11/24→≤3` is **NOT** "fully met". Read precisely — **within-turn surface ✅ met by S-Auto-13b** (12→0 across 3 passes); **cross-turn residual ⏳ open as OQ-S69.1** (21→22); **total storm count NOT yet ≤3 if cross-turn is included** (33→22). This residual is a MILESTONE-level open item, NOT a S-Auto-13b sprint-level failure. It must enter the M-Auto-3 close bad-case review / §11 interpretation, where the human + deliver-agent decide: accept the residual, split cross-turn to M-Auto-4, or trigger a §8.5 split. **Fence on the next sub-sprint:** S-Auto-14 handles escalation-reason honesty (B1) ONLY and does NOT also pick up cross-turn paraphrase storm.

**R-item addressed by S-Auto-13b (NOT flipped to closed until M-Auto-3 milestone close per §7 routing):**

- `R-runtime-paraphrase-storm-search-knowledge` (A3) → **within-turn surface addressed by S-Auto-13b** (deterministic `faq_miss`-state gate; within-turn PARAPHRASE_STORM 12→0; soft layer retained beneath). The **cross-turn surface remains open as OQ-S69.1** (BotSession-scoped state; §8.5 split / post-M-Auto-3). Within-turn = CLOSE candidate at M-Auto-3 close; the R-item stays `proposed` until the cross-turn carrier decision is made. Supersedes the S-Auto-13 "PARTIALLY addressed" note above.

### M-Auto-3 CLOSE (2026-06-03) — R-item dispositions + OQ ledger + M-Auto-4 handoff

**Milestone M-Auto-3 — Substrate-hygiene CLOSED 2026-06-03 (Class A)**; milestone-shared Codex `pass / 0` (`docs/milestones/M-Auto-3_codex-review.md`; range S-Auto-11..15 + bundled M-Auto-2 residual). This subsection is the AUTHORITATIVE close record; the per-sprint "surfaced OQs / addressed-by" notes above (Sprint 065-069) + the Sprint-065 7-R-item catalog are SUPERSEDED by it and are pending a §7.1 inline-history strip (deferred housekeeping, file still under budget).

**R-items CLOSED at this close (relocated to `action_bank_archive.md` §C):**

- `R-runtime-identical-tool-call-retry-storm` — A1 hybrid dedup (S-Auto-12).
- `R-runtime-tool-gating-race-uc-none` — A2 classify-first (S-Auto-13).
- `R-runtime-paraphrase-storm-search-knowledge` — within-turn (S-Auto-13b) + cross-turn rank-2+ (S-Auto-15) deterministic `faq_miss`-state gates, no content matching. **cross-turn rank-1 first-refinement = OBSERVATION carry** (not a defect; escalates to M-Auto-4 as a product/semantic-policy question ONLY if a future overnight shows it materially corrupts fitness — see the §11 three-class taxonomy in archived `M-Auto-3_objective.md` §5).
- `R-runtime-escalation-reason-misstamp-maxsteps-faq` — B1 evidence-aware `resolveMaxStepsReason` (S-Auto-14); NO new enum (reuses `turn_budget_exhausted`; the §4 `D-new-escalation-reason-enum` avoid-item respected — the old R-item "path 1" `max_steps_exhausted` enum was NOT taken).
- `R-simulator-first-message-contract-violation-flake` — S-Auto-11 safe hardening; root re-attributed bot-side (OQ-S66.2) + fitness-neutralized by #4 infra-error detection.
- `R-overnight-eval-traces-not-persisted` — S-Auto-11 #3.
- `R-tier1-bad-cases-regression-5-to-0-attribution-unverified` — SUPERSEDED: 0-keep re-attributed to provider MEASUREMENT noise (the M-Auto-4 thesis below).

**OQ ledger at close:** OQ-S65.7/8 RESOLVED (S-Auto-11 #4); OQ-S66.1 RESOLVED (S-Auto-13); OQ-S67.1/67.2 observation/closed-by-B1; OQ-S68.1 + OQ-S70.1 RESOLVED (S-Auto-15 B, `503/0` under `uv run`); OQ-S68.2/68.3/68.4 resolved/subsumed; OQ-S69.1 RESOLVED via the three-class taxonomy; OQ-S71.1 RESOLVED via the metric-classification re-frame (Codex `approve`, not §5.4 masking); **OQ-S71.2 (trace `active_use_case` null observability debt) = CARRY** (live session populated, gate unaffected) → M-Auto-4 observability sweep; OQ-S66.2/66.4 bot-side/optional carries.

**STILL OPEN / DEFERRED (NOT closed):**

- `R-runtime-escalation-reason-turn-budget-conflated-with-intent` (B3/R5) — deferred M-Auto-4+.
- `R-classifier-non-deterministic-uc-selection-at-temp-zero` (C/R7) — deferred M-Auto-4+; its root (temp-0 provider non-determinism) is the SAME one the M-Auto-4 measurement-reliability proposal addresses at the harness layer.
- `R-autoloop-run-sweeps-dirty-index` — standing hazard (`project_autoloop_dirty_index_hazard`); honored operationally, no code fix scoped.
- `R-eval-interactive-judge-score-never-populated` — LOW priority. **OQ-S76.judge-zero collapsed in 2026-06-05 (route c, chronic by-config, not a regression; both bad_cases empty-dim + shadow advisory-strip variants folded). NOT promoted to M-Auto-6.**

**M-Auto-4 SELECTED + ACTIVE (2026-06-03) — Autoloop Fitness Measurement Reliability** (`docs/milestone_objective.md`). The other slate items remain deferred (M3-B P0; M-Auto-1A carry-overs). Active milestone detail:

- **Autoloop fitness MEASUREMENT reliability** — `docs/proposals/autoloop_fitness_measurement_reliability.md` (the milestone spec; **status: partial / in-progress**). **Goal: autoloop fitness MEASUREMENT reliability, NOT bot-capability optimization** — make keep/discard reflect reproducible regression/improvement instead of single-run provider noise; explains the 0-keep the cleaned substrate still shows (residual provider non-determinism; temp already 0.0; seed/top_p unavailable). **Sub-sprint split (2026-06-03): S-Auto-16 / Sprint 072 CLOSED** (Route A Clean PASS) — k-of-n + aggregation + majority-input machinery shipped INERT at `samples_per_case=1` (byte-identical to today, §8.7); §0 diagnostic = 0% fallback + 4.3% infra_error + all 26 escalation-jitter cases `fallback_count==0` → genuine backend non-determinism (thesis confirmed); §6.1 variance ↓ all three suites (bootstrap −7.6%/−5.0%/−14.7%); `aggregate.py` joined a 5-file `scoring_code_baseline_sha` set `35305bd8…`→`bb3ced3d…`; autoloop `306/0`; per-sub-sprint Codex DEFERRED to the M-Auto-4 milestone-shared close (fence-#13). **S-Auto-17 / Sprint 073 ACTIVE** = baseline re-bless (with per-case stability classification) + configured-primary detector fix (OQ-S72.1) + live `samples_per_case=3` flip + validation-overnight gate DRAFT. **S-Auto-18** = §5 escalation-family tiers (`eval_spec`). Locked v1 decisions (§9): (1) `samples_per_case` default **n=3** (n=5 only as a later per-case high-flake override, not v1 main line); (2) fitness eval **primary-provider-only** — a deepseek→kimi fallback attempt is excluded from the majority vote (`provider_mixed`/`non_comparable`); (3) `min_valid_attempts=3` — short after retries → `infra_error`/`non_comparable`, never participates in keep/discard. Boundaries: **Tier-0 safety obligation NOT relaxed** (only the measurement decision changes — a Tier-0 violation must stably reproduce across k-of-n to count; never the safety policy); **bad_cases drop-budget NOT in v1**; editing the SHA-locked scoring files (now 5, incl. `aggregate.py`) **requires recomputing `scoring_code_baseline_sha`** (+ fence-#13 controlled override + per-sub-sprint Codex). **⚠️ Overnight HELD for human** — expected keeps ≈ 0 until S-Auto-17 lands (n=3 live + re-bless), and the launch is gated on the per-case stability report (OQ-S72.2). (Provenance: drafted in a Claude Code session 2026-06-03 on top of a multi-agent diagnosis; prior "Codex-authored" label corrected.)
- **M3-B Single Handover Orchestrator** — P0 release-gate blocker (`docs/proposals/handover_orchestrator_design.md`); deferred to a later milestone.
- M-Auto-1A carry-overs (7).

### Sprint 072 / S-Auto-16 surfaced OQs (2026-06-03)

S-Auto-16 (M-Auto-4 lead sub-sprint; k-of-n machinery INERT; handoff `docs/sprints/sprint-072-handoff.md`) closed Route A Clean PASS. Two load-bearing OQs surfaced, both routed to **S-Auto-17**:

- **OQ-S72.1 — fitness fallback detector anchors on the EMPIRICAL modal chat model, not the CONFIGURED primary.** The S-Auto-16 §0 detector defines "primary" as the dominant `callType=="chat"` model across a suite run (`eval_runner._modal_model`). Harmless at S-Auto-16 (0% observed fallback; the infra-error path defends a dead primary), but **for live gating (S-Auto-17) a whole-run fallback would be silently treated as "all primary"** — the anchor must come from the configured primary model name. **Status: open; S-Auto-17 #1 consumes it (configured-primary detector fix; `infra`; still keyed on model id, not content — §1.7-clean).**
- **OQ-S72.2 — the milestone anchor bad_cases may be near-coinflip, which k-of-n CANNOT stabilize.** §6.1 real-LLM evidence: `bad_cases` mean pass-rate ≈ **0.52**, **8/12 flaky**; at p≈0.5 a 3-sample majority gives ~0 variance reduction (math, not impl). The milestone's primary §5.6 acceptance anchor (cs011/cs014/cs029) plausibly sits here. **Implication: n=3 (and even n=5) will not stabilize a genuinely ~50/50 case — that is an `eval_spec`/semantic-ambiguity signal, not a sampling problem.** S-Auto-17's baseline re-bless MUST emit a per-case stability classification (**stable** pass_rate≈0/1 → hard anchor; **reducible-flaky** clear lean but jitters → later per-case n=5 candidate; **near-coinflip** pass_rate≈0.5 → eval_spec/semantic-ambiguity candidate, do NOT force with n=5). The validation overnight is **gated on this report**: if many anchor bad_cases are near-coinflip, do NOT launch — surface to human to choose per-case n=5 / case quarantine / eval_spec remediation. **No bad_cases drop-budget.** **Status: open; S-Auto-17 #2/#3/#4 consume it; may reshape the milestone acceptance bar.**

Carry-forward (not new OQs): n=3 stays the live default in S-Auto-17 (empirical-first per human 2026-06-03); the overnight launch remains HELD for explicit human instruction.

### Sprint 076 / S-Auto-21 surfaced OQs + M-Auto-6 candidates (2026-06-04)

S-Auto-21 (M-Auto-5 corrective #2; simulator role-inversion fix; handoff `docs/sprints/sprint-076-handoff.md`) closed ACCEPTED with **0/40 contamination on the simfixed bad-case re-render** and **852→0 D1-contaminated turns across the pre-fix corpus** (real-LLM evidence per §5.7). Input artifact: `docs/diagnostics/2026-06-04-eval-framework-and-simulator-audit.md`. Three OQs + a set of M-Auto-6 candidates carried forward:

- **OQ-S76.judge-zero — RESOLVED 2026-06-05 (route c; collapsed into `R-eval-interactive-judge-score-never-populated`).** Investigation (research-agent 2026-06-05) confirmed: chronic, by-configuration, NOT a regression from S-Auto-19/20/21. `mean_judge=0.0` is byte-identical across all four M-Auto-5 bracketing runs (m-auto-4-baseline-20260604 / m-auto-5-baseline-20260604 / m-auto-5-baseline-20260605 / simfixed `results/20260604-152103/`). Two config-side sub-variants (both folded into the same R-item): (a) `bad_cases/*.yaml` + `anchor_outcome/*.yaml` ship `llm_judge_dimensions: []` → no L3 calls → fallback 0.0; (b) `shadow/*.yaml` invokes L3 but every dim is `severity="advisory"` → S-Eval-5 advisory-strip removes them → same fallback. See the R-item entry at the §5.2 line `R-eval-interactive-judge-score-never-populated` for the full four-run table + sub-note breakdown. M-Auto-5 paired-evidence review **EXCLUDES the judge layer** (canonical signal = composite/outcome/L1+L2); the re-bless launch is NOT blocked. R-item stays LOW priority; NOT promoted to M-Auto-6 (judge is reporting-table-only; canonical signal works).
- **OQ-S76.drift-stop — literal `stop_reason="simulator_drift_blocked"` not surfaced.** S-Auto-21 dev shipped the in-fence equivalent: `SimulatorDriftError(detector, snippet, attempts)`, which the executor records as `stop_reason="error"`. Already absent from `_VALID_TERMINAL_STOP_REASONS`, so a blocked session correctly fails `trace_minimum` (no scoring vacuous-pass) — this OQ is for label fidelity in reports/audits, **NOT correctness**. One-line fix when the `session_runner.py` edit fence next opens (one `try/except SimulatorDriftError` around `session_runner.py:217` + an executor branch in `executor.py` to record the label distinctly from generic error). **Status: open; NOT a milestone-close blocker.**
- **OQ-S76.A6 — bot self-diagnoses simulator drift; runtime ignores.** From audit §2.6: on `csmp_s01_uc_a_search_hits_with_ad_id_mismatch` turn 4 the agent's LLM raw `reasoning` field contains *"The user's message is a bot apology, not a new user query."* — yet the runtime emitted a normal response. With S-Auto-21 shipping a clean simulator (0 contamination), A.6 has lost its trigger surface and may dissolve in practice. **Status: open; reassess at M-Auto-5 close;** if the simfixed full re-bless still shows any bot self-diagnoses of drift, route as a M-Auto-6 candidate (otherwise close as obsolete).

**M-Auto-6 candidates (from the 2026-06-04 audit, audit §5 routing; 2× parallel research-agent dispatch after M-Auto-5 close):**

- **Cluster B.1 — `per_turn_trace` truncation on 22/22 cases.** `len(per_turn_trace) < total_turns` (e.g. `cs11s02` total_turns=5 vs per_turn_trace length=3; `cs40s01` total_turns=3 vs length=1). Phase entries have no `turn_index`. Sprint 075's `trace_minimum` runtime stamp gates against incomplete data (thermometer in an empty room). **Route: research-first (short)** — confirm emitter side (runtime emission vs eval consumption); landed-status sample: 30-50 % per-turn entries missing. Overlaps OQ-S71.2 (trace `active_use_case` null observability debt).
- **Cluster B.2 — `primary_uc != active_use_case` on 40-60 % of cases.** 9/22 in `20260604-110801`, 7/12 in `20260604-045045`. CaseSpec UC tag disagrees with runtime classifier. No resolved authority; hidden in per-case `failure_tags`. **Route: research-first (decision)** — human + research must pick which side is authoritative before any deliver scope. Blocks any UC-routing semantic sprint.
- **Cluster C.1 — `cs59s*` empty-form-context cases die on `session_create_failed:400`.** `cs59s01_uc_d_empty_form_account_recovery` + `cs59s02_uc_f_empty_form_payout_timing` consistently abort with `HTTPStatusError("Client error '400 ' for url '…/v1/chat/sessions'")`, `total_turns=0`. These cases are designed to test the empty-form path. **Route: research-first (short)** — confirm empty form-context request contract.
- **Cluster C.2 — live-UI 500 + double-send (trace `46f5b2e9-523…`).** Human probe 2026-06-04 ~19:33; not present in `eval_interactive/results/` or `logs/` (live admin UI). Two distinct candidate defects: frontend double-submit (no debouncing / in-flight guard) and/or backend 500 on the duplicate not surfaced as a graceful error. **Route: research-first** — reproduce + localize before scoping.
- **Cluster C.3 — generic clarifier wastes opening turn on form-context cases.** ~17/22 sessions in `20260604-110801` open with "Could you tell me a bit more about what you need help with?" even when `source=form_context` already carries a concrete problem statement; 5 other sessions open with the skill-specific variant. Same `source=form_context` flag on both branches — branch-selection logic non-obvious from case data alone. Burns one of ~3-5 turns per session, inflating `turn_budget_exhausted` escalations. **Route: research-first** — read intake branch code.

### Sprint 077 / S-Auto-22 CLOSED 2026-06-05 — OQ-S77.stall-not-gated (RESOLVED PENDING SWEEP); two new OQs surfaced

S-Auto-22 ACCEPTED-WITH-DEVIATIONS-DOCUMENTED 2026-06-05 (dev commit `2ea65de`). Shipped: #1 STALL signal promotion (option 1a typed `stall_result.detected` boolean read, scoped to `composite==0`); #2 terminal-failure `_TERMINAL_FAILURE_STOP_REASONS` frozenset override (option 2b separate failure arm, `goal_impossible` excluded — see deviation); #3 zero-evidence refusal structural rule (no allowlist); #4 runtime `ControlKernel.shouldVoidResolvedStamp/voidResolvedStamp` + `BotSession.priorContainmentOutcome` provenance (option 4a overwrite to `incomplete_after_partial_answer`). Tests: 15 new in `test_oq_s77_false_positive_gates.py` + 12 new in `ControlKernelVoidResolvedStampTest`; eval pytest 553; Java 1244/1/0/2; autoloop 324. Deterministic re-score: 12 vacuous → FAIL + 1 correct extra (csmp_s01 a6); all 9 legitimate F→P preserved majority-PASS; 0 silent losses. Archives `docs/sprints/sprint-077-{objective,handoff}.md`. Two evidence-driven deviations + bookkeeping correction documented in handoff §1 + §2 + §0; both deviations carry to milestone-shared Codex verdict at M-Auto-5 close.

- **OQ-S77.stall-not-gated — CLOSED 2026-06-05 at M-Auto-5 milestone close.** The eval-gate fixes (#1 + #2 + #3) + runtime companion (#4) close the gap structurally. Final closure validated: §5.9 pre-flight sweep on the re-re-blessed corpus `m-auto-5-baseline-20260604-simfixed-stalledfix/` returned **0/414** vacuous-pass + terminal-failure matches. Framework-defect priority (§5.8) **LIFTED** 2026-06-05. **Status: CLOSED.**

- **OQ-S77.stall-detector-window — NEW (S-Auto-22 dev surfaced 2026-06-05).** The `eval_interactive/eval_interactive/scoring/stall_detector.py` window logic false-positives on out-of-window escalation recovery. Canonical evidence: cs095 a4 (`stall_detected=True + composite=0.5 + containment=escalated`) — the stall detector flagged the turn-1 "let me look into this" because the escalation recovery fell outside the detector's follow-up window. NOT a re-re-bless blocker (Fix #1's `composite==0` scoping spares these false positives). **Status: open; deferred. Candidate for M-Auto-6 observability sweep or a later eval-spec sub-sprint.** Layer (§3): `infra` (eval-framework scoring detector tuning) or possibly `eval_spec` (depending on whether the detector window is config or rubric).

- **OQ-S77.goal-impossible-resolved-evidence — NEW (S-Auto-22 dev surfaced 2026-06-05).** Whether a session with `containment_outcome="resolved" + stop_reason=goal_impossible + composite>0` (positive evidence) should itself be a hard-fail is an `eval_spec` judgment. S-Auto-22 #2's exclusion of `goal_impossible` from the terminal-failure set is the conservative choice consistent with S-Auto-20's deferral; the vacuous instances are still gated by #3 (`composite==0 AND l2==[]`). **Status: open; deferred consistent with S-Auto-20.** Layer (§3): `eval_spec`. Candidate for a future eval-spec sub-sprint (likely in M-Auto-7 or later, after M-Auto-6 = Clusters B + C).

Adjacent updates:
- **OQ-S72.2 DISSOLVED** 2026-06-05 (S-Auto-21 simfixed re-bless eliminated near-coinflip across all three suites; the corrected measurement no longer puts any case at p≈0.5).
- **OQ-S76.A6 (bot self-diagnoses sim drift, runtime ignores)**: at M-Auto-5 close 2026-06-05 — CARRY. Trigger surface dissolved at the simulator layer (0 contamination on simfixed runs); reassess against the re-re-blessed baseline when next operating against it. If no bot self-diagnoses of drift surface, close as obsolete; otherwise route as M-Auto-6 candidate.

### M-Auto-5 CLOSE (2026-06-05) — brief dispositions + OQ ledger + new R-item + M-Auto-6 handoff

**Milestone M-Auto-5 — Eval Verdict Correctness + Trace-Contract Honesty CLOSED 2026-06-05 (Class A — APPROVE_WITH_NON_BLOCKING_OBSERVATIONS)**; milestone-shared Codex `decision: pass`, `blocking_count: 0`, `final_verdict: APPROVE_WITH_NON_BLOCKING_OBSERVATIONS` (`docs/milestones/M-Auto-5_codex-review.md`; range S-Auto-19 + S-Auto-20 + S-Auto-21 + S-Auto-22). §5.9 pre-flight sweep on the re-re-blessed corpus: **0/414 vacuous-pass + terminal-failure matches** (framework-defect priority §5.8 LIFTS). Paired-evidence review: **10 F→P / 0 P→F** across bad_cases (alice / cs012 / cs015 / cs066 / cs095) + anchor_outcome (uc_a_visibility / uc_f_billing / uc_fp_removed) + shadow (cs01s01 / cs11s01). Persistent high-risk preserved (anchor uc_g_gdpr / uc_h_appeal / uc_i_payment / uc_j_safety + shadow cs38s* at 0.000 stable); anti-误杀 held both directions. `baseline_dir` moved `m-auto-4-baseline-20260604` → `m-auto-5-baseline-20260604-simfixed-stalledfix` (`autoloop/config.yaml:134`); `docs/current_eval_baseline.md` updated. This subsection is the AUTHORITATIVE close record for M-Auto-5; the per-sprint "surfaced OQs" entries above (Sprint 076-077) are pending a §7.1 inline-history strip (deferred housekeeping).

**Brief / R-item families CLOSED at this close** (work driven by failure-briefs and an eval-framework audit, not classic-named R-items):

- **Eval-read column** (S-Auto-19 / Sprint 074): 5 measurement-artifact fixes — `trace_minimum` unions all turns; source citations accumulate across session; `search-knowledge-before-faq` ATR union fallback; intake reads dict keys; PII allowlist for first-party + RFC 2606. Plus runtime `ControlKernel` stamps trace-contract fields + answer-turn `sourceIds`.
- **Runtime-stamp column** (S-Auto-20 / Sprint 075): `ControlKernel.isResolvedSuccessTerminal` broadened to `FINAL_ANSWER + READY_TO_CONFIRM|ANSWERED_SUBTASK` with grounding; `loop_detected` removed from valid blank-containment terminals (eval framework correction — a looped session can no longer pass on absent evidence).
- **Input column / simulator** (S-Auto-21 / Sprint 076): customer simulator role-inversion fix at `user_simulator.py:187` + per-turn persona re-anchor + negative-form `Forbidden` block + customer-voice drift guard D1/D2/D3 with 3-attempt retry + `SimulatorDriftError` escape. Pre-fix corpus sweep 852 → 0 contaminated turns; focused bad-case re-render 0/40. Source brief: `docs/diagnostics/2026-06-04-eval-framework-and-simulator-audit.md`.
- **Eval-gate vacuous-pass + runtime stamp downgrade** (S-Auto-22 / Sprint 077): STALL signal promotion scoped to `composite==0` + terminal-failure `_TERMINAL_FAILURE_STOP_REASONS` override (`goal_impossible` excluded) + zero-evidence refusal `composite==0 AND l2_results==[]` structural rule + runtime `ControlKernel.shouldVoidResolvedStamp` voids stale resolved stamp on `{MAX_STEPS, ERROR, DEADLINE_EXCEEDED, LLM_UNAVAILABLE}` → `CONTAINMENT_INCOMPLETE_AFTER_PARTIAL_ANSWER`. Two evidence-driven deviations independently verified by Codex. Source brief: `docs/diagnostics/failure-briefs/oq-s77-stall-not-gated.md`.

**OQ ledger at close:**

- **OQ-S77.stall-not-gated — CLOSED 2026-06-05** (§5.9 sweep returned 0/414; framework-defect priority §5.8 LIFTED).
- **OQ-S76.judge-zero — RESOLVED 2026-06-05** (route c; collapsed into chronic `R-eval-interactive-judge-score-never-populated`).
- **OQ-S72.2 — DISSOLVED 2026-06-05** (near-coinflip eliminated across all three suites on simfixed-stalledfix).
- **OQ-S76.A6 — CARRY** (trigger surface dissolved at simulator layer; reassess against re-re-blessed baseline when next operating against it; if no bot self-diagnoses surface, close as obsolete; otherwise M-Auto-6 candidate).
- **OQ-S77.stall-detector-window — CARRY** to M-Auto-6 planning (detector window-tuning; non-blocker per Codex Deviation #1 confirmation; layer `infra` / possibly `eval_spec`).
- **OQ-S77.goal-impossible-resolved-evidence — CARRY** (eval_spec policy Q whether `resolved+goal_impossible+positive-evidence` should hard-fail; deferred consistent with S-Auto-20; likely M-Auto-7+).
- **OQ-S76.drift-stop — CARRY** (label fidelity only; one-line fix when `session_runner.py` fence next opens).
- Earlier carries unchanged: **OQ-S72.1**, **OQ-S71.2** (overlaps M-Auto-6 Cluster B.2), **OQ-S66.2 / OQ-S66.4 / OQ-S64.3 / OQ-cv-ceiling-calibration**.

**Newly opened R-item from Codex non-blocking observation #3 (2026-06-05):**

- **`R-aggregate-retains-per-attempt-composite-l2`** — per-attempt `composite_score` + `l2_results` fields are absent from compact aggregate attempt rows; Codex was unable to independently reproduce the §5.9 414-draw predicate from the compact aggregate alone (Codex relied on stated evidence + run-local data + visible missed-gate symptom verification). For future close reviews, preserve per-attempt composite + L2 fields in the authoritative aggregate or include the sweep output artifact. Layer (§3): `infra` (eval framework aggregate emission). Routing: **M-Auto-6 Cluster B observability bundle candidate**; queued, not yet promoted to an active sub-sprint. Surfaced by `docs/milestones/M-Auto-5_codex-review.md` §5 observation #3.

**STILL OPEN / DEFERRED (unchanged at this close):**

- M3-B Single Handover Orchestrator P0 (`docs/proposals/handover_orchestrator_design.md`).
- M-Auto-1A carry-overs (7).
- `R-runtime-escalation-reason-turn-budget-conflated-with-intent` (B3/R5) — deferred behind M-Auto-6.
- `R-classifier-non-deterministic-uc-selection-at-temp-zero` (C/R7) — deferred behind M-Auto-6 (same root the M-Auto-4 measurement proposal addresses at the harness layer).
- `R-autoloop-run-sweeps-dirty-index` — standing hazard, honored operationally.
- `R-eval-interactive-judge-score-never-populated` — LOW; judge layer chronic by-config; canonical signals (composite + outcome + L1 + L2 failure_tags) populated.
- M-Auto-4 PAUSED — its **measurement-reliability core RESUMED + dev+Codex CLOSED 2026-06-16 as M-Auto-7 S-Y1.7** (Sprint 091 / S-Auto-37, commit `758503b6`, Codex `pass`/0; `R-autoloop-fitness-measurement-reliability` in the §5 M-Auto-7 table); the separate `S-Auto-18` escalation-family tier reshape (`eval_spec`) stays deferred to M-Auto-7+.

### M-Auto-6 CLOSE (2026-06-07) — closure pointer

M-Auto-6 closed 2026-06-07 as **route-(b) accept-with-known-regression** (per the milestone contract §5; archived at `docs/milestones/M-Auto-6_objective.md`). Codex milestone-shared §4.3 verdict: `APPROVE_M_AUTO_6_WITH_NON_BLOCKING_OBSERVATIONS / decision: pass / blocking_count: 0` (archived at `docs/milestones/M-Auto-6_codex-review.md`). Anti-误杀 / safety CLEAN (HARD=0 across 36 anchor attempts + 18 cs38s* attempts; 0/36 self-resolve; route (c) ruled out). Five known regressions accepted (Cluster 1 pre-existing semantic flakiness — R5/R6 exonerated; Cluster 2 excluded `infra_error` — R2.a/R2.a#5-ext exonerated; Cluster 3 session-start infra flake); no rollback; no fix sprint. A6 anti-误杀 reframed from "0.000 floor" to "safety-of-pass" (preflight-eval-checks.md §2/§3/§5/§7; scope = uc_g/h/i/j + cs38s01/cs38s02; tightening, not weakening). Baseline pointer flipped 2026-06-07 to `eval_interactive/results/m-auto-6-baseline-shared-20260607/` (`autoloop/config.yaml:baseline_dir` + `docs/current_eval_baseline.md` canonical section M-Auto-6 / M-Auto-5 demoted to "Previous canonical baseline"). Sub-sprint archives: `docs/sprints/sprint-078..083-*`. Closed R-items + sub-sprint rows + milestone row relocated to `docs/action_bank_archive.md` §A / §B / §C per §7.1.

### M-Auto-7 candidates (surfaced from M-Auto-6 close 2026-06-07)

**M-Auto-7 LAUNCHED 2026-06-08** — theme "Autoloop readiness and CS4
entity-context pilot" (`docs/milestone_objective.md`). Phase 1 blockers
(CS1 / CS3 / CS4 readiness) + Phase 2 CORE GATE (CS4 autoloop pilot) +
non-blocking back-half (CS2-original). The four CS rows below are
CONSUMED into M-Auto-7 sub-sprints; the two `OQ-M6.*` umbrella ids are
RETIRED into cleaner R-items (historical aliases preserved in the
source column). The remaining rows stay open backlog (not consumed by
M-Auto-7).

| id | source | description | layer | status |
|----|--------|-------------|-------|--------|
| R-uc-a-entity-context-verify-procedure-via-autoloop | M-Auto-7 S-Y1 + S-Y2 (CS4; consolidates retired alias `OQ-M6.uc-fp-resolve-vs-escalate-boundary`) | Before answering an ad-specific question, verify entity context (lookup if identifiers exist, else ask) instead of generic FAQ; autoloop authors the skill-yaml procedure candidate against authored CaseSpecs. PRIMARY = OBS-S1 (UC-A/UC-FP/UC-H verify-entity-context procedure); SECONDARY = OBS-S2 (UC-A vs UC-FP boundary cue). Scope spans S-Y1 readiness (projection infra + executable CaseSpecs) + S-Y2 pilot (autoloop run + §4.1 review + merge + re-bless). Solution docs: `docs/solutions/2026-06-07-cs3-cs4-discover-stall-uc-fp-boundary-empty-trace-ux.md` §3 + `docs/solutions/2026-06-08-cs4-casespec-drafts-appendix.md` | `prompt_projection` (Part A) + `eval_spec` (Part B) + `semantic_planner` (Part C autoloop-authored) | S-Y1 readiness DEV-SIDE CLOSED (Sprint 086a `77b1712` projection slots Java `1383/1/0/2` + 086b `0b111fd` 5 NEW+2 EXTEND CaseSpecs, all 7 schema-compile) / **S-Y2 pilot scope ACTIVE (CORE GATE)** — pre-pilot baseline re-bless DONE (Part C.1, `m-auto-7-prepilot-baseline-20260608`, gap confirmed); pre-pilot autoloop substrate patch S-Y1.5 (Sprint 087) inserted + dev+Codex CLOSED (see `R-autoloop-feedback-loop-thinness`); **S-Y2 Part C RAN (run-1/2/3, exp-66..85); exp-82 nominated→OVERTURNED n=13 (0/13)→WITHDRAWN; OQ-C/D/E surfaced.** Inserted §5.8 blocker S-Auto-38 (escalation_compliance tier-0 split) dev+Codex CLOSED 2026-06-18 + gate-trust validated by exp-86 `-n1` smoke (PASS / OFF_TARGET — not mergeable, not seed). Read-only acceptance review + OQ-S86b.3 forensic (2026-06-18): both PRIMARY bars product-correct (do NOT lower; `no_ad_id` resolve is correct per the product contract, over-escalation is the bug; `cs_uc_a_lookup_failed` already Tier-2; live PRIMARY = `no_ad_id` + `loaded_listing`); root cause of non-escalation failures = RESOLVE→CONFIRM/CLOSE phase deadlock → new `R-resolve-confirm-transition-deadlock` (Sprint 093). Objective-alignment annotation deferred to AFTER Sprint 093 (M-Auto-7 final readiness item); pilot HELD until Sprint 093 closes + passes Codex. OQ-S86a.1 + OQ-086b.1/.2 → milestone-shared Codex. **2026-06-19: registry-only autoloop pilot CLOSED — NO KEEP (control-surface limit reached for the current PRIMARY objective; exp-90 real DISCARD; `discover_triage.$.procedure` lever DEFERRED). Successor = OQ-S93.1 runtime/orchestration closure design milestone (`docs/proposals/runtime-closure-record-outcome-design-milestone-oq-s93.1.md`); analysis `docs/diagnostics/exp-90-real-result-and-primary-control-surface-2026-06-19.md`.** |
| R-resolve-confirm-transition-deadlock | M-Auto-7 Sprint 093 (OQ-S86b.3; surfaced 2026-06-18 by the read-only acceptance review + forensic after the exp-86 OFF_TARGET smoke) | A completed grounded UC-A resolve answer can deadlock in RESOLVE: `ResolveDispositionEvaluator.shouldRejectPrematureResolveOutcome` correctly rejects `record_outcome(resolve)` outside CONFIRM/CLOSE, but `evaluate()` only returns `READY_TO_CONFIRM` when a `record_outcome` already succeeded this run → no working path out of RESOLVE (trace: phase=RESOLVE 7 turns, `record_outcome(FAIL)` T0+T5 → `max_turns`/stale-resolved; or `bot_ended` blank). Manifests as blank/stale containment (`trace_minimum`/`correct_outcome` fail), turn-budget exhaustion, or mis-attributed escalation. Fix goal = a legitimate, reliable RESOLVE→CONFIRM/CLOSE path for a completed grounded resolve answer; NOT relax the premature guard, NOT raise max_turns, NOT a CaseSpec/PRIMARY exception, NOT lower the `record_outcome` requirement. Pre-dev forensic = per-draw classify `faq_miss_threshold_exceeded`/`turn_budget_exhausted`/`user_requested` into deadlock-downstream mis-stamp (cf. `project_faq_overescalate_maxsteps_misstamp`) vs genuine LLM escalation. Validation (human-approved (a)+(b)) = bounded NON-pilot real-LLM run (2 PRIMARY ×11 + `cs_uc_a_generic_policy_question` ×5 + 2 neighbors ×5) + zero-LLM attribution cross-check. Touches FROZEN `ResolveDispositionEvaluator`/`isResolvedSuccessTerminal` (adjacent to CS1/S-A). Source: acceptance review + OQ-S86b.3 forensic (handoff §1 2026-06-18) | `skill_state` (runtime phase machine) | **`closed-structural` / M-Auto-7 Sprint 093 / S-Auto-39 dev+Codex CLOSED 2026-06-18** (fix `033abaee` `PhaseEvaluator.mapFinalAnswer` structural RESOLVE→CONFIRM promotion; no premature-guard relax / no content heuristic / no max_turns bump / no CaseSpec exception; Java 1394/0-new-regression + 11 char-tests; bounded run 11/22 PRIMARY reach CONFIRM (0 baseline), blank/stale gone, anti-误杀 intact; Codex `pass`/0; `docs/sprints/sprint-093-*`). **Structural transition repaired + validated; the residual `record_outcome(resolve)` 0/42 non-landing is the SECOND layer → `R-oq-s93.1-confirm-record-vs-handover`.**) |
| R-oq-s93.1-confirm-record-vs-handover | M-Auto-7 (OQ-S93.1; surfaced 2026-06-18 by the Sprint 093 bounded run) | In CONFIRM, a grounded UC-A session that the persona keeps engaging neither lands `record_outcome(resolve)` (**0/42**) nor is credited via a clean confirm — it escalates with a mislabeled `user_requested` reason; resolved currently lands only via the existing `isResolvedSuccessTerminal` grounding stamp (7/22 PRIMARY). The deadlock had a SECOND layer beyond the phase transition Sprint 093 fixed. **Research-first (read-only): NO code / CaseSpec / evaluator / baseline change, NO new LLM run** — per-draw analysis of the Sprint 093 bounded-run NEW traces answering (1) direct cause of record_outcome 0/42 (not-selected / rejected / not-persisted / handover-preempted); (2) the CONFIRM-phase projection + available actions + planner guidance; (3) per-item classification of the 13/22 escalations (genuine user-accepted vs semantic-planner handover mischoice vs `user_requested` mislabel vs faq/turn-budget deadlock-downstream vs other); (4) the ~2 surface-plausible escalations' full behaviour chain (record only; no CaseSpec/bar change); (5) recommended next fix layer (`prompt_projection`/`semantic_planner` vs `skill_state` vs reason-resolver/`infra`, or split into WPs). Do not presume all escalations wrong; do not relax the guard because record_outcome=0. Output = evidence-backed Failure Brief + minimal-fix recommendation → human decides next sub-sprint. Source: Sprint 093 handoff §5 + Codex non-blocking note | `semantic_planner` / `prompt_projection` (the `user_requested` label is **LLM-supplied** in the request_handover args — NOT a runtime resolveMaxStepsReason mis-stamp; the budget-family reasons are the separate runtime vector) | **RESEARCH DONE 2026-06-18** (read-only; brief `docs/diagnostics/failure-briefs/oq-s93.1-confirm-record-vs-handover.md`). Findings: record_outcome 0/42 is NOT a mechanism bug — CONFIRM skill exposes+instructs it, projection signals it, guard permits it; it doesn't land because the bad-case personas never reach satisfaction → bot escalates. 13/22 PRIMARY escalations = **2 genuine user-accepted** + **6 bot-initiated `user_requested` MISLABEL** (LLM-supplied) + **5 budget-family runtime**. Recommended split: **WP1** = user_requested mislabel (`prompt_projection`-led, RECOMMENDED FIRST, low-risk; no user-message content heuristic); **WP2** = CONFIRM record-vs-handover on *satisfiable* flows (needs a satisfiable companion persona); + a **product question** (are these personas resolvable / is escalation-after-help correct? — record only, no CaseSpec/bar change). **OPEN / human decides next sub-sprint; gates objective-alignment annotation + M-Auto-7 pilot resume.** **2026-06-19: ELEVATED to the runtime/orchestration closure DESIGN milestone `M-Auto-9` (OQ-S93.1; charter `docs/proposals/runtime-closure-record-outcome-design-milestone-oq-s93.1.md`) when the registry-only pilot closed NO-KEEP; exp-90 confirmed this as the shared closure blocker independently gating BOTH PRIMARY. Design-only; fix fences unchanged (no guard relax / no max_turns / no record_outcome-requirement lowering / no CaseSpec exception / no content heuristic).** |
| R-autoloop-feedback-loop-thinness | M-Auto-7 S-Y1.5 (Sprint 087 / S-Auto-32; surfaced 2026-06-09 by the autoloop mechanism audit after the S-Y2 run-1 tranche exp-66/67/68 = 0/3 on-gap, 2/3 phase-incorrect) | Autoloop meta-agent feedback loop too thin to steer toward the active pilot's targets: P0-A proposer/analyzer strip `verdict.tier_breakdown` (no feedback gradient); P0-B analyzer reads only `baseline_dir` (landscape never reflects candidate-introduced regressions); P0-C proposer has no concept of the pilot's PRIMARY TARGETS (hunts off-gap); P1 May-31 lessons (pre-CS4) steer at the wrong cluster. Fix = `config.yaml:pilot` block + tier_breakdown / candidate-results passthrough (shadow firewall EXTENDED) + `lessons.enabled` opt-out + per-exp pilot snapshot + 4-layer hit-rate audit. Solution doc: `docs/solutions/2026-06-08-autoloop-mechanism-audit-and-feedback-loop-tightening.md` (Option B) | `infra` (autoloop meta-agent prompt builder + config) | dev-side CLOSED + per-sub-sprint Codex PASS (Sprint 087; commit `43cd9cf` autoloop/** only; pytest 324→331; shadow-firewall test proven load-bearing; `scoring_code_baseline_sha` unchanged → NO re-bless; `--dry-run -n 2` checklist 4/4 PASS; Codex `APPROVE_S_Y1_5 / blocking_count=0`; 3 P3 NBOs + OQ-S87.1/.2/.3/.4 → milestone-shared Codex) |
| R-autoloop-fitness-measurement-reliability | M-Auto-7 S-Y1.7 (Sprint 091 / S-Auto-37; resumed PAUSED M-Auto-4 scope; surfaced 2026-06-16 after the S-Y2 run-1/2/3 tranches = 0/14 keeps and the zero-LLM P0.7 retrospective calibration root-caused it) | The autoloop fitness gate's zero-tolerance majority-flip rule (`anchor_outcome_max_drop_cases:0` + raw tier2/shadow count gates) false-discards a behaviour-neutral candidate ~92–95% of the time at n=3–5 (selects on noise, not quality — the M-Auto-4 thesis, now directly evidenced). Fix = stability-tiered noise-aware rule (V3): tier0 floor UNCHANGED + TIER-S majority-flip anti-误杀 floor (NOT any-fail) + TIER-N Beta-Binomial/δ=0.10/BH-FDR-or-count≥2 + noise-aware tier2 (C1) + noise-aware shadow (C2) + samples_per_case 3→5 (11 primary) + non_comparable instrumentation (P0.6c). Inserted per §5.8 (PREEMPTS S-Y2 Part C). Acceptance = zero-LLM oracle replay of exp-66..79 (V3 matrix). Source: `docs/solutions/2026-06-12-sy2-autoloop-wording-analysis-and-zero-keep-root-cause.md` §5 P0 + `docs/solutions/p07-calibration/` (calibration) + `docs/solutions/2026-06-15-p0-fitness-measurement-reliability-sprint-contract.md` (promoted draft) | `infra` (autoloop fitness gate / scoring: `tier_evaluator.py` + `baseline_loader.py` + `aggregate.py` + `config.yaml`) | **dev+Codex CLOSED 2026-06-16 / M-Auto-7 S-Y1.7** (commit `758503b6`, 12 files autoloop/**+config only, +1694/−543; V3 rule shipped — TIER-S majority-flip anti-误杀 floor + TIER-N Beta-Binomial/δ/BH-count + noise-aware tier2 C1 + noise-aware shadow C2; zero-LLM oracle `test_fitness_gate_oracle.py` 22-pass reproduces the V3 matrix; full autoloop pytest 348→**390**; `scoring_code_baseline_sha` recomputed `f2f983cc…`, NO re-bless; per-sub-sprint Codex `pass`/blocking_count 0 / Kernel `approve` — `docs/sprints/sprint-091-codex-review.md`, NBO R-S91.1 = keep the F5 knob-confirmation gate. Handoff `docs/sprints/sprint-091-handoff.md`. F5: precise knobs confirmed on the first S-Y2 pilot run under the new gate) |
| R-controlkernel-default-resolved-on-close-anti误杀 | M-Auto-7 S-A (CS1) | `ControlKernel.java:575-579` Path B default-stamps `resolved` on phase=CLOSE without grounding (legacy D16.D, pre-M-Auto-6); gate the CLOSE-arm stamp through `isResolvedSuccessTerminal` (inline); lowers pass-rate honestly. Solution doc: `docs/solutions/2026-06-07-cs1-default-resolved-cs2-user-role-projection-gap.md` §3.1 | `infra` (measurement honesty) | dev-side closed / M-Auto-7 S-A (Sprint 084; 5 char-tests green, Java 1363/1/0/2; OQ-S84.1 3-file reconcile + OQ-S84.2 → milestone-shared Codex) |
| R-discover-null-turn-counter-anti误杀 | M-Auto-7 S-X (CS3; split from retired alias `OQ-M6.empty-trace-and-session-start-flake`) | DISCOVER null-turn placeholder (`AgentRunLoopImpl.java:438-441`) counted by the R2.a clarification counter (`:442-457`) → premature budget force-escalate before the LLM gets a recovery turn. Root cause TRACE-CONFIRMED (session 33edc1eb): `ActionParser.java:70-72` substitutes the placeholder at parse time, so the R2.a counter sees a non-blank reply and counts it (proposal Option C3.A keyed on `userMsg.isBlank()` is INERT). Fix: structural provenance flag (`ParsedAction.userMessageSynthesised`) excludes synthesised placeholders from the counter + §1.3-soft DISCOVER cue + `user_message_synthesised` trace diagnostic (no content heuristic — keeps R2.a structural-only). Solution doc: `docs/solutions/2026-06-07-cs3-cs4-discover-stall-uc-fp-boundary-empty-trace-ux.md` §2 (mechanism corrected per trace) | `infra` (R2.a counter provenance) + `prompt_projection` (soft cue) | dev-side closed / M-Auto-7 S-X (Sprint 085; 6 tests green, Java 1373/1/0/2; OQ-S85.1 golden update + OQ-S85.2 §5.7 note → milestone-shared Codex) |
| R-user-role-projection-slot-from-listing-ownership | M-Auto-7 S-B (CS2-original) | seller/buyer perspective slip; no `user_role` projection slot. Data-derived `ad_owner\|unknown` slot from form-email↔listing-`posted_by` match + soft cue. **Non-blocking back-half; NOT on the M-Auto-7 core acceptance bar; may carry to a follow-on.** Solution doc: `docs/solutions/2026-06-07-cs1-default-resolved-cs2-user-role-projection-gap.md` §3.2 | `prompt_projection` | active / M-Auto-7 S-B (non-blocking) |
| R-admin-empty-trace-zero-turn-affordance | M-Auto-6 close §5 (CS2-new; split from retired alias `OQ-M6.empty-trace-and-session-start-flake`) | empty-trace `BOT_HANDLING` sessions are architectural reality (not a defect); P3 admin UX affordance ("Awaiting first message" label + empty-state copy). Solution doc: `docs/solutions/2026-06-07-cs3-cs4-discover-stall-uc-fp-boundary-empty-trace-ux.md` §1 | `infra` + admin observability | P3 backlog (NOT in M-Auto-7) |
| R-r5-citation-result-binding-grounding-strengthening | M-Auto-6 milestone-shared Codex NBO #1 (2026-06-07) | R5 citation validator is presence-only by contract; any structural URL/article-ID satisfies it without proving the cited token came from the active `resolve_article` result. Future grounding-strengthening opportunity (bind citation acceptance to active result), NOT a semantic-hardcode fix | `infra` + grounding contract | open / M-Auto-7 candidate |
| R-standalone-reconcile-entry-gate-test | S-Auto-28 Codex NBO #1 | pin standalone `--reconcile` through `ApplicationArguments` to `run()` so a gate regression fails before the shared DB step | `infra` (test) | open / S-Auto-29+ pickup |
| R-docs-reconciliation-faq-grounding-contract-vs-must-cite-source | S-Auto-26 Codex NBO #2 | `docs/current/faq_grounding_contract.md:8-10` + `:92-99` historically stale relative to live Sprint-39 `must_cite_source` gate at `SkillGuardrailDispatcher.java:358-397`; post-M-Auto-6 docs-only sprint | docs | open / M-Auto-7 candidate |

Carry-over open R-items from S-Auto-25 Codex non-blocking observations (queued at M-Auto-7+): pin projected `update_intake_fields` schema directly with a `ContextProjectionBuilder` test; decide string-vs-arbitrary-coercion on `IntakeFieldsRegistry.mergeFields`; manual capability-registry drift across `IntakeFieldsRegistry` + skill YAML + `tool-policy.yaml` + `SkillLoader.VALID_TOOL_NAMES`.

Carry-over open R-items from S-Auto-24 (post-M-Auto-6 observability backlog): OQ-S79.2 single-session demo endpoint; OQ-S79.3 per-call timestamps.

Separate human triage: cross-UC stash-and-resume — `D-full-issue-ledger-light` (NEW; §4 deferred-pending-research; trigger = M-Auto-7 close + multi-issue trace survey per CS3/CS4 doc §10.4; lighter push/pop variant of the heavier `D-full-issue-ledger`) — NOT folded into any M-Auto-7 sub-sprint.

### Sprint 086 / S-Y1 (S-Auto-31) CLOSED dev-side (2026-06-08) — surfaced OQs

S-Y1 (CS4 readiness) closed dev-side as two sequenced dev sessions: 086a (Part A projection infra, commit `77b1712`) + 086b (Part B CaseSpec authoring, commit `0b111fd`). All three Phase-1 autoloop launch blockers (S-A / S-X / S-Y1) are now in. Archives: `docs/sprints/sprint-086-objective.md` + `sprint-086a-handoff.md` + `sprint-086b-handoff.md`. Next gate = pre-pilot baseline re-bless (= pilot Part C.1).

- **OQ-S86a.1** — A4's conditional `resolve_faq_grounded_answer.yaml` declaration of `discover_disambiguation_signals` was DECLINED (violates the M5-S4 §3.H audit invariant guarded by `Sprint53SkillDeclarationGatingTest`; only `discover_triage` may declare it). Safe for the pilot: CS4 UC-FP routing happens at DISCOVER (where the slot already surfaces); RESOLVE moderation grounding is covered by the existing `consult-moderation-context-on-removal-explanation` critical step. If S-Y2 genuinely needs the slot in RESOLVE-FAQ, that is a deliberate audit revisit (update the audit doc + guard test), not Part A wiring. → S-Y2 / milestone-shared Codex.
- **OQ-086b.1** — `cs_uc_a_lookup_failed` `escalation_trigger` set to `null` (the appendix's `lookup_failed_user_cannot_correct` is schema-invalid with `should_escalate: false` and not a canonical `EscalationTrigger` enum value). Intent preserved via `acceptable_outcomes: [resolve, escalate]` + closure_criterion (c); NOT a §5.4 widening (still pins `correct_uc`/`correct_outcome`/`tool_sequence_match`). Confirm `null` at the §5.6 tiering; a structured escalate pin would need `should_escalate: true` + a canonical enum value (e.g. `service_degraded`). → §5.6 / human.
- **OQ-086b.2** — doc-hygiene: the appendix + 086b dev prompt cited the CaseSpec schema at `eval_interactive/eval_interactive/specs/schema.py`; the real path is `eval_interactive/eval_interactive/case_spec/schema.py` (loader `…/case_spec/loader.py`; outcome-check registry `…/scoring/outcome_checks.py`). Compile ran against the real path; the live S-Y1 contract B3 reference was corrected before archival. No effect on the authored cases.

### Sprint 087 / S-Y1.5 (S-Auto-32) CLOSED dev-side + Codex (2026-06-09) — surfaced OQs + NBOs

S-Y1.5 (pre-pilot autoloop substrate patch, INSERTED between S-Y1 and the
S-Y2 CORE GATE after the S-Y2 run-1 validation tranche surfaced off-gap
targeting) closed dev-side (commit `43cd9cf`, autoloop/** only, pytest
324→331) + per-sub-sprint Codex `APPROVE_S_Y1_5 / blocking_count=0`. No
re-bless (`scoring_code_baseline_sha` unchanged). Archives:
`docs/sprints/sprint-087-{objective,handoff,codex-review}.md`. S-Y2 promoted
as Sprint 088 / S-Auto-33 (contract active; Part C NOT started — human gate).

- **OQ-S87.1** — L-006 (next lessons-compactor output at the K=10 boundary)
  will be written to `lessons.md` while `lessons.enabled:false`, but the
  proposer does NOT consume it. Open: when/whether to re-enable lessons
  post-pilot, and whether lessons authored during a disabled window are
  quarantined or trusted on re-enable. → S-Y2 / milestone-shared Codex.
- **OQ-S87.2** — `pilot.schema_version:1` is recorded in every
  `pilot_snapshot` but there is no version migration/validation yet. Open:
  reject-unknown vs best-effort-read when the pilot schema evolves; whether
  `validate_pilot_config` should hard-check the version. → milestone-shared Codex.
- **OQ-S87.3 (= Codex NBO-2, P3 infra)** — P1 lessons opt-out is
  PROPOSER-only; the analyzer still receives `LESSONS_MD`. The dominant
  feedback vector was the proposer (run-1 cited L-005), but the analyzer could
  carry pre-CS4 bias. Open: extend the opt-out to the analyzer? → watch in
  S-Y2 Part C; milestone-shared Codex.
- **OQ-S87.4** — `_read_recent_candidate_results` depends on per-iter
  `eval-results.json` surviving on disk (applier cleanup + forensic retention
  decide how many of the last K are readable). Open: confirm retention during
  the S-Y2 Part C run so the analyzer's candidate landscape is non-empty.
  → S-Y2 Part C.
- **Codex NBO-1 (P3 infra)** — only one post-patch dry-run artifact (`exp-69`)
  retained under `autoloop/results/runs/`; preserve both dry-run artifacts (or
  attach captured prompts to the handoff) for future independent rationale
  audits.
- **Codex NBO-3 (P3 infra)** — the analyzer `summary` scrub pattern predates
  the `cs_uc_a_*` pilot label shape and doesn't scrub it; not a blocker
  (structured labels are intentional + the labels-only directive holds), but
  future hardening could extend the defensive scrub if rationales start echoing
  `cs_*` labels.

### Sprint 090 / S-Y1.5b/c (S-Auto-35/36) Codex combo review (2026-06-11) — surfaced blocking R-item

The S-Y1.5b/c anti-hardcode detector combo (S-Y1.5b scope fix `9c62a86`
LANDED; S-Y1.5c severity calibration `75b302c`) went to a combined
per-sub-sprint §4.3 Codex review (`compact/sprint-090-review-prompt.md`).
Verdicts written to `docs/codex-findings.md`: **S-Y1.5b `pass` /
blocking_count 0** (standalone approve re-confirmed under the combined
lens); **S-Y1.5c `fix_required` / blocking_count 1** (Kernel verdict
`needs human architecture decision`). One blocking R-item surfaced.

- **R-S90.5** (`infra`; **OPEN — current blocking finding**) — S-Y1.5c's
  retained default-FAIL set is not limited to unambiguous §1.7 surfaces.
  `Q4.case_id_literal`'s regex `\bcs[0-9a-z_]{2,}\b`
  (`autoloop/autoloop/sandbox/anti_hardcode_check.py:274`) matches ANY word
  beginning `cs` + 2 alnum/underscore, so ordinary terms (`CSAT`,
  `csagent`, `css`) FALSE-FAIL as if they were CaseSpec-id leaks. This
  breaks S-Y1.5c's load-bearing claim that the four retained FAIL rules are
  all "surface form == constitutional intent", and re-opens a
  false-positive-discard vector on exactly the `resolve_faq` surface the
  S-Y2 pilot runs on. Codex confirmed `cs011`, `cs_uc_a_no_ad_id`,
  `session_id=abc`, `case_id: 42` all still correctly FAIL (the behavior
  the fix must preserve). **Disposition (human decision 2026-06-11):
  tighten the regex, keep FAIL** — require a CaseSpec-id discriminator
  (`cs` immediately followed by digit/underscore, e.g.
  `\bcs[0-9_][0-9a-z_]+\b`), keeping `Q4.case_id_literal` severity `_FAIL`
  so the §1.7 raw-eval-phrase hard-discard for real ids is preserved.
  Fix scoped as the targeted fix-iteration **S-Y1.5d / S-Auto-37**
  (`compact/sprint-090d-dev-prompt.md`; one regex line + precision tests:
  real ids FAIL, ordinary cs-words PASS; scoring SHA stable → no re-bless).
  A targeted §4.3 Codex re-review of the Q4 regex diff then flips the
  S-Y1.5c stanza `fix_required → pass`/0 and marks R-S90.5 resolved-by-d.
  **Status (2026-06-12):** S-Y1.5d attempt-1 (`40f8c07`) resolved the
  CSAT/csagent false-positive but introduced **R-S90.6** (below) and was
  HELD; the corrected attempt-2 resolves both. Refs:
  `docs/codex-findings.md` (R-S90.5 finding);
  `docs/solutions/2026-06-11-sy15bc-combo-implementation-plan.md` §11.
- **R-S90.6** (`infra`; **OPEN — current blocking finding**, surfaced by
  the S-Y1.5d targeted re-review 2026-06-12) — S-Y1.5d attempt-1's
  tightened regex `\bcs[0-9_][0-9a-z_]+\b` required the digit/underscore
  discriminator *immediately after* `cs`, so real `cs`+letter CaseSpec ids
  (the `manual_probe_uc_a_resolve_must` family:
  `csmp_g01_uc_a_genuine_faq_miss_obscure`, `csmp_s01_…`, `csmp_n01_…`)
  returned PASS — a false-negative that weakens the §1.7 Q4 hard-discard.
  Codex re-review verdict on S-Y1.5d: `fix_required` / blocking_count 1
  (S-Y1.5c stays held). **Deliver-agent verification (direct Python
  `os.walk` from the repo root): the corpus has 452 distinct cs-ids, every
  one containing a digit or underscore, and ZERO pure-letter cs-ids.**
  **Disposition (human decision 2026-06-12):
  proceed with the corrected regex `\bcs[a-z0-9_]*[0-9_][a-z0-9_]*\b`
  (digit/underscore ANYWHERE in the cs-token), keep `Q4.case_id_literal`
  `_FAIL`.** Verified: catches all 452 ids (0 misses incl. `csmp_*`),
  passes `csat`/`csagent`/`css`/`csv`; narrow residual (`css3`/`cs50`/
  `cs2go` still FAIL — non-blocking OQ). Corrected fix = S-Y1.5d attempt-2
  (revised `compact/sprint-090d-dev-prompt.md` + re-review against the full
  452-id matrix). **Status: RESOLVED 2026-06-12** by S-Y1.5d attempt-2
  (`708f8cb3`, regex `\bcs[a-z0-9_]*[0-9_][a-z0-9_]*\b`); targeted Codex
  re-review independently re-verified (452 cs-ids / 0 misses; `csmp_*` FAIL;
  `CSAT`/`csagent` PASS) and returned `pass`/0 on S-Y1.5b/c/d — **R-S90.5 AND
  R-S90.6 both resolved; the S-Y1.5b/c/d combo is closed**
  (`docs/codex-findings.md`). **Env note:** during this session a
  shell `cwd` drift into `autoloop/` (from an earlier `cd autoloop`) made
  recursive `grep -r`/`find`/`ls` over relative `eval_interactive/…` paths
  return empty (the dir doesn't exist under `autoloop/`) — briefly mistaken
  for a suppressed/hallucinated result. Lesson: run corpus checks from the
  repo root (or with absolute paths); a direct Python `os.walk` is the
  authoritative check.

### Sprint 092 / S-Auto-38 (escalation_compliance tier-0 split) CLOSED dev+Codex (2026-06-18) — surfaced OQs

Inserted §5.8 framework-defect blocker (the S-Y2 Part-C OQ-E forensic found a
tier-0 `escalation_compliance` *reason-label* flakiness flipping KEEP↔DISCARD).
WP-A `57cd93c5` (Part-1 behaviour stays tier-0 / Part-2 reason-family →
observation-only) + WP-B `8ed65cc6` (unified override schema, zero active
bindings; cs11s01 `pending_review`). Zero-LLM replay 43/43 `PART2_DEMOTION` / 0
regressions / neg-control discards / four safety checks byte-identical. Codex
`approve`/`pass`/0. Gate-trust validated by the exp-86 `-n1` smoke (PASS,
OFF_TARGET — not mergeable, not seed). Archives `docs/sprints/sprint-092-*`.
exp-82 stays WITHDRAWN; no PRIMARY success; full pilot tranche HELD.

- **OQ-S86b.3 — NEW (PRIMARY, → Sprint 093).** RESOLVE→CONFIRM/CLOSE
  phase-transition deadlock (see `R-resolve-confirm-transition-deadlock`).
  Root cause CONFIRMED read-only 2026-06-18. Layer (§3): `skill_state`
  (possibly `human_review_required` — frozen surface).
- **OQ-C / OQ-D — NEW (S-Y2 Part C).** exp-82 nominated a "preferred seed"
  then overturned by the n=13 re-eval; refinement-seed semantics. Addressed by
  the deferred objective-alignment annotation (proposal §4). Layer: `eval_spec`
  / reporting. → milestone-shared Codex.
- **OQ-E — RESOLVED (2026-06-18) by S-Auto-38.** Tier-0 `escalation_compliance`
  reason-label flakiness; Part-2 demotion removes it.
- **OQ-E.cs11s01 — OPEN (product-owner).** Is the FAQ-miss-first path in-scope
  for cs11s01? The cs11s01 override stays WP-B `pending_review` until the
  product owner confirms. Does NOT block Sprint 093. Layer: `eval_spec`.
- **OQ-E.git-lookup-WARN — OPEN (observation).** exp-86's benign
  `suspect_baseline_manipulation.git_lookup_failed` under the `--config`
  override (run launched from cwd `autoloop/`): make the gaming detector
  resolve the config path repo-root-relative + honor the active `--config`.
  No `scoring_code_drift` fired. Layer: `infra`. → backlog.
- **OQ-E.tier0-shortcircuit-reporting — OPEN (observation).** `tier0_majority`
  mixes enforced (`_TIER0_PY_FAMILY`) + non-enforced checks with no gating
  marker; a tier-0 discard short-circuits without surfacing pre-empted
  downstream signals. Layer: `infra` (observability). → backlog.

### Sprint 093 / S-Auto-39 (RESOLVE→CONFIRM/CLOSE phase-transition corrective) CLOSED dev+Codex (2026-06-18) — surfaced OQ

M-Auto-7 pre-pilot runtime corrective for OQ-S86b.3. Verdict: `CLOSE —
RESOLVE→CONFIRM structural transition repaired and validated; clean end-to-end
resolve recording remains blocked by OQ-S93.1`. Fix `033abaee`
(`PhaseEvaluator.mapFinalAnswer` structural RESOLVE→CONFIRM promotion;
no premature-guard relax / no content heuristic / no max_turns bump / no
CaseSpec/PRIMARY exception). Java 1394 run / 0 new regressions (11
`Sprint93ResolveConfirmDeadlockTest`); bounded NON-pilot real-LLM run (42 draws,
§5.9 GO): 22 PRIMARY qualifying draws, 11/22 reach CONFIRM (0 baseline), blank/stale
gone, anti-误杀 INTACT (`cs11g02` 5/5 escalate co=1.0; `generic_policy` 0
over-escalations; neighbors no regression). §5 cross-check: evaluator + 6 CaseSpecs
byte-identical `a428aa18`→HEAD, hashes pinned. Codex `pass`/0/`approve` (all 9
Pass/N-A). Archives `docs/sprints/sprint-093-{objective,handoff,codex-review}.md`;
Step-0 `docs/diagnostics/sprint-093-step0-escalation-attribution.md`; Failure Brief
`docs/diagnostics/failure-briefs/sprint-093-resolve-confirm-deadlock.md`. No
end-to-end resolve-recording success claimed; no PRIMARY majority flip (none
required); pilot HELD; exp-82 WITHDRAWN.

- **OQ-S93.1 — NEW (PRIMARY pre-pilot blocker, research-first).** The SECOND
  deadlock layer: `record_outcome(resolve)` lands 0/42 even in CONFIRM (resolved
  only via the `isResolvedSuccessTerminal` grounding stamp, 7/22); CONFIRM-phase
  record-vs-handover decision + `user_requested` reason mislabel. See
  `R-oq-s93.1-confirm-record-vs-handover` (§5 M-Auto-7 table). Read-only research
  FIRST; gates the pilot + objective-alignment annotation. Candidate layers:
  `semantic_planner` + `infra`/reason-resolver. Of the 13/22 escalating PRIMARY
  draws, ~2 are genuinely-correct user-accepted escalations scored `co=0.0` (a
  small defensible tail — record only, NO CaseSpec/bar change); ~11 carry the
  mislabeled `user_requested` reason (`project_faq_overescalate_maxsteps_misstamp`).

### Sprint 094 / S-Auto-40 WP1-A Phase 2 surfaced R-item (2026-06-19)

WP1-A measurement contract → conditional outcome acceptance **CLOSED 2026-06-19
at HEAD `052cc73b`** (human-accepted). Phase-2 bounded real-LLM run
`results/wp1a-phase2-measurement-20260619` (42/42 draws, evidence floor MET); all
4 `CONDITIONAL_ELIGIBLE` escalate traces human-REJECTED on closure-quality review
and recorded in `eval_interactive/case_specs/conditional_outcome_adjudications.yaml`.
**Scope boundary:** validates measurement + acceptance INFRASTRUCTURE only — does
NOT establish a conditional baseline and does NOT demonstrate improved bot
behaviour. Archives: `docs/sprints/sprint-094-{objective,handoff}.md` (§10 close);
Codex `pass` in `docs/codex-findings.md`.

**Next: M-Auto-7 pilot-resume PREP** (not launched) — resume from the **unchanged
canonical baseline** (`autoloop/config.yaml:baseline_dir`) using the new
measurement contract. exp-82 stays WITHDRAWN; **no re-bless**; WP1-B / WP2 /
objective-alignment annotation / the auto-triage R-item below remain HELD unless
pilot evidence shows them blocking.

- **R-conditional-adjudication-auto-triage** — prevent per-trace human closure
  adjudication from becoming a recurring auto-loop bottleneck. The
  `CONDITIONAL_ELIGIBLE` → human-verdict step must NOT block the loop when it
  cannot change an already-`case_passed=False` state. Design a declarative
  auto-adjudication pre-filter so human review is reserved for genuinely
  borderline semantic cases:
  1. **Auto-reject** clear cases — wrong UC, generic FAQ answer without
     context provenance, or any explicit CaseSpec FAIL condition — without a
     human turn.
  2. Treat **both tool-returned and pre-loaded** customer context as valid
     entity-context provenance (do not require a `get_customer_context` tool
     call when the CaseSpec accepts pre-loaded `customer_context`).
  3. Require **substantive use of listing-specific fields**, not merely
     mentioning the title/status, to count an answer as closure-grounded.
  4. Reserve human review for the **genuinely borderline** semantic cases only.
  5. **Do not block** the whole auto-loop on conditional reviews that cannot
     flip an already-false `case_passed`.
  Layer: `eval_spec` (declarative adjudication pre-filter) + `infra` (auto-loop
  gating). Source: S-Auto-40 WP1-A Phase 2 closure-quality review (2026-06-19).
  Status: **open / M-Auto-7 candidate** (WP1-B / WP2 + pilot remain HELD).
  Failure clusters seeded for this work: see
  `docs/diagnostics/failure-clusters-uc-a-listing-2026-06-19.md`.

### M-Auto-7 pilot resume + S-Auto-41 proposer steering (2026-06-19)

Pilot resumed from the unchanged pre-pilot fitness baseline using the WP1-A
measurement contract. **exp-87 ACCEPTED as a successful validation iteration**
(decision=discard; measurement contract, conditional acceptance, V3 gate,
rollback, and non-blocking adjudication all operated correctly): tier0 safety
CLEAN; discarded at the tier1 TIER-S anti-误杀 floor (candidate regressed
`cs01s01` 1/5 + others); both PRIMARY stayed 0 (no_ad_id 0/13, loaded_listing
0/12); contract integrity verified live (user_state signals + provenance present,
false-resolves correctly FAILed, **0 improper PASS**, no scoring_code_drift,
baseline frozen). exp-86 + exp-87 were both OFF_TARGET → the blocking issue is
**proposer steering, not evaluation correctness**.

- **S-Auto-41 (R-autoloop-feedback-loop-thinness follow-up — narrowly scoped,
  proposer-input only)** — enrich the proposer feedback with (1) target PRIMARY
  + baseline evidence, (2) the 3 observed failure clusters, (3) a REQUIRED
  causal hypothesis, (4) a REQUIRED expected trace-level change, and (5) a
  lightweight OFF_TARGET pre-check before the expensive fitness eval. Tool-
  returned AND pre-loaded customer context treated as equally valid; substantive
  use of listing-specific info required (NO hard-coded `get_customer_context`).
  Touches `proposer.py` / `config_validator.py` / `propose.txt` / `loop.py` +
  the `primary_target_steering` block in `config.pilot-s-auto-38.yaml`. **Does
  NOT change the keep gate, conditional rules, baseline, or canonical pointer;
  none of the 6 scoring-SHA files touched (sha still `7df8173c…`, no drift).**
  Layer: `infra` (autoloop meta-agent steering). Validation = 9 zero-LLM unit
  tests + dry-run proposal-only inspection, then ≤2 bounded real iterations.
  WP1-B / WP2 / objective-alignment annotation / `R-conditional-adjudication-auto-triage`
  remain HELD.

  **exp-88 + exp-89 result (2026-06-19, 2 bounded real iterations, both DISCARD —
  ACCEPTED):** the steering FIXED the targeting problem. Precise diagnosis:
  1. **Semantic target steering is now working** — both proposals were `on_target`
     (pre-check) and addressed the intended PRIMARY failure mechanisms (listing-
     context grounding + ask-for-ad-reference), a clean contrast to the OFF_TARGET
     exp-86/exp-87.
  2. **Both discarded at tier0 — but the attribution is NOT a proven causal
     regression (CORRECTED).** Both candidates edited the shared
     `resolve_faq_grounded_answer.$.procedure` and were discarded at tier0 on
     `escalation_compliance@cs38s01_uc_j_scam_seller_full_narrative`. However the
     read-only surface analysis shows this is most plausibly **flakiness / tier0
     mis-attribution, not cross-UC bleed**: (i) `cs38s01` is a **near-coinflip
     flaky shadow case** (baseline `pass_rate=0.545`, `flaky=True`, with **0/11
     escalation_compliance fails** at baseline); (ii) it is **UC-J (intake path)**,
     which a `resolve_faq` edit **cannot structurally reach** — the runtime
     projects only the active skill selected by `(phase, useCase)`
     (`ContextProjectionBuilder` ~L1071), so a RESOLVE-FAQ-skill edit never enters
     a UC-J intake/escalate session; (iii) across the 4 resolve_faq-editing
     candidates cs38s01 tier0 flipped clean/clean/fail/fail (exp-86/87/88/89).
     tier0 is zero-tolerance, so a single flaky escalation fail on a near-coinflip
     shadow case discards the candidate. ⇒ the real blockers are (a) mutation-
     surface scoping (structural: `$.procedure` is the broadest surface) AND (b)
     **tier0 flake-sensitivity on near-coinflip shadow cases** (overlaps
     `R-autoloop-fitness-measurement-reliability`), NOT a demonstrated escalation
     regression from the grounding edit.
  3. **No PRIMARY-improvement claim** — both runs short-circuited at Layer 0, so the
     proposed grounding changes are **NOT** shown to improve the PRIMARY; that
     remains UNPROVEN (k_c=0 observed, but moot pre-tier1).
  Contract integrity held (signals present, 0 improper PASS, no scoring_code_drift
  [sha `7df8173c`], baseline frozen, canonical pointer unchanged). No candidate
  merged; no gate / baseline / re-bless change. **Real iterations STOPPED** — a
  repeated, stable tier0 regression is sufficient evidence that further candidates
  on the broad shared-procedure surface are not justified.

  **Next narrow step (read-only first; HELD for human review before any real
  iteration):** a read-only mutation-surface analysis — compare the 4 mutable
  registry fields (`$.procedure` / `$.grounding_instruction` / `$.escalation_policy`
  / `$.critical_steps[*].desc`) and their runtime consumers across the 6 skills,
  and identify the NARROWEST legitimate surface for the UC-A entity-context /
  listing-grounding behavior that cannot suppress escalation. A UC-A-conditioned
  `critical_steps[*].desc` MAY be considered but must NOT be assumed in advance.
  The next dry-run proposal must state: (1) exact PRIMARY mechanism targeted; (2)
  why the edit surface is narrower than the shared procedure; (3) expected
  cross-UC blast radius; (4) an explicit trust-and-safety/scam escalation-
  precedence preservation rule; (5) expected PRIMARY trace-level changes; (6)
  confirmation it encodes no benchmark case names / fixed ad IDs / test-specific
  answers. Surface the scoping analysis + dry-run proposal for review.

  **Artifacts produced 2026-06-19 (surfaced for review; NO real iteration):**
  (1) Read-only surface analysis →
  `docs/diagnostics/autoloop-mutation-surface-scoping-2026-06-19.md` — narrowest
  legitimate surface is `$.grounding_instruction` (escalation-free, grounding-
  dedicated); `critical_steps[*].desc` gives **no runtime UC-scoping** (projection
  ignores `mandatory_for`) and a new UC-A step is outside the mutable surface.
  (2) Steering increment: 6 required dry-run disclosures + a justified
  `surface_preference` hint (not hard-coded) — `7f54b543`, 14 unit tests, scoring-
  SHA `7df8173c` unchanged. (3) Proposal-only **dry-run exp-90** (NOT applied, NOT
  evaluated): proposer chose `$.grounding_instruction`, JUSTIFIED the choice,
  supplied all 6 disclosures incl. an explicit escalation-preservation rule
  (`$.escalation_policy` byte-identical), `on_target` + anti-hardcode PASS +
  sandbox ACCEPT, after_value generic prose with no leakage. (4) Read-only tier0
  flake-sensitivity scope →
  `docs/diagnostics/autoloop-tier0-flake-sensitivity-2026-06-19.md` — `cs38s01` is
  stochastic shadow evidence not a deterministic floor; the candidate-vs-baseline
  attribution is statistically inconclusive (no significance test); proposes (does
  not implement) an `INCONCLUSIVE_FLAKY` confirmation path under
  `R-autoloop-fitness-measurement-reliability`. **HELD for human review before any
  further real iteration.**

### M-Auto-7 registry-only pilot CLOSED — NO KEEP + OQ-S93.1 runtime/orchestration design milestone opened (2026-06-19)

**Verdict: CLOSED — NO KEEP: registry-only control-surface limit reached for the
current PRIMARY objective.** (human-authorized 2026-06-19.)

- **S-Auto-42 VALIDATED on first real evidence.** exp-90 was the first real
  candidate to fire the bool-only flaky-tier0 defer rule: the near-coinflip shadow
  case `cs38s01_uc_j_scam_seller_full_narrative` (`escalation_compliance`,
  `baseline_flaky=true`, `candidate_majority_failed=true`) was **DEFERRED**, not
  tier0-discarded — it did NOT mask later evidence; the candidate still discarded
  for an **independent** Tier-1 regression. **Zero `HOLD_INCONCLUSIVE_FLAKY`** this
  run (the deferred entry was preempted by the Tier-1 discard) ⇒ **no posterior
  confirmation / bounded re-sampling justified** (no HOLD bottleneck). The
  posterior/re-sampling path stays deferred per the data blocker.
- **exp-90 real DISCARD (ACCEPTED; isolated experiment CLOSED).** One real
  iteration, proposal byte-identical to the reviewed dry-run (fp
  `c3e798d7dd3ef61f`; injected without a proposer LLM call; same fp in the
  persisted `experiments.jsonl` exp-90 row). Decision `discard` /
  `tier1_outcome_regressed_count_3_bh_1_thresh_2` (3 independent regressions:
  `cs095_uc_d` 0.818→0.0 P_regress 0.998 + BH-flag; `anchor_uc_fp_removed`
  0.70→0.20; `anchor_uc_g_gdpr` 0.636→0.20). **Neither PRIMARY improved**
  (`no_ad_id` 0/11→0/13; `loaded_listing` 1/11→0/12). Tier-0 PASS. Branch
  `autoloop/exp-90` + tag `autoloop/discard-90`;
  `autoloop/results/runs/exp-90/REAL_RUN_summary.json`.
- **Grounding conclusion (precise).** exp-90 shows the **reviewed**
  `$.grounding_instruction` hypothesis was **insufficient and harmful elsewhere**;
  it does NOT prove every possible registry wording is impossible. The stop is
  justified **because the shared runtime closure / `record_outcome` blocker already
  makes registry-only search unable to deliver the end-to-end PRIMARY objective**.
- **Per-blocker classification (read-only capability-to-mutation-surface
  analysis):** (#1) `no_ad_id` closure / `record_outcome` (RESOLVE→CONFIRM,
  OQ-S93.1) = **runtime/orchestration**; (#2) `loaded_listing` UC-A→UC-B misclass
  (`correct_uc` 8/13) = **registry-addressable, UNTRIED** (`discover_triage.$.procedure`);
  (#3) `loaded_listing` substantive grounding salience = **runtime/orchestration**
  (registry teaching exercised by exp-90 + insufficient; data projected but not
  salient; the CaseSpec-named mandatory UC-A consult-then-ground step is OUTSIDE
  the whitelisted soft-field surface); (#4) `loaded_listing` closure =
  **runtime/orchestration**; (#5) trace `source_ids` = **eval/observability**
  (minor, non-gating). Full analysis:
  `docs/diagnostics/exp-90-real-result-and-primary-control-surface-2026-06-19.md`.
- **`discover_triage.$.procedure` classification lever → DEFERRED.** Not run now:
  it may fix the `loaded_listing` misclass but cannot overcome the shared closure
  blocker that independently gates both PRIMARY, so it cannot make the objective
  keep-eligible. Revisit only AFTER the shared closure path is fixed.
- **`source_ids` trace projection → separate non-blocking observability item.**
  Per-turn trace shows `source_ids=[]` even when `resolve_article` ran; non-gating
  for the PRIMARY cases (`correct_outcome` reads `containment_outcome`; the
  `loaded_listing` PASS condition reads the bot `user_message` text). Track
  separately; do not bundle into the closure design.
- **OQ-S93.1 ELEVATED to the runtime/orchestration closure DESIGN milestone
  `M-Auto-9`** (formal id; M-Auto-8 is reserved for the human-confirmed
  primary-first staged-eval theme, so the runtime closure takes M-Auto-9) —
  charter `docs/proposals/runtime-closure-record-outcome-design-milestone-oq-s93.1.md`
  (design-only; covers (1) why grounded help doesn't reliably close, (2) the
  `PhaseEvaluator` / `ResolveDispositionEvaluator` / premature-resolve-guard
  interaction, (3) per-PRIMARY target trace behavior, (4) trust-and-safety / scam /
  payment / GDPR / explicit-human-request / genuinely-unresolved precedence
  preservation, (5) anti-hardcode + cross-UC regression protections, (6) the
  smallest runtime change + unit/integration/real-LLM validation plan). Builds on
  the OQ-S93.1 research (`R-oq-s93.1-confirm-record-vs-handover`), does not redo
  it. Formal milestone id = deliver-agent + human at promotion.
- **Holds (in force):** no exp-91; no mutable-surface change; no WP1-B / WP2 /
  objective-alignment annotation; no posterior/bounded-resampling; no re-bless /
  baseline move / canonical-pointer change; conditional-adjudication auto-triage
  HELD; exp-82 WITHDRAWN. **Deliver follow-up:** fold this closure into
  `docs/sprint_objective.md` / `docs/milestone_objective.md` / `docs/10-handoff.md`
  §0 and assign the formal successor-milestone id at the next planning step.

## 6. Closed index (relocated)

Closed sprints, milestones, and R-items are archived as a compact
**pointer index** in `docs/action_bank_archive.md`:

- §A — closed per-sprint index
- §B — closed milestone index
- §C — closed R-item index (id → closing sprint → archive pointer)

The live ledger above carries only open / active / deferred items.

## 7. Carry-over rule

If an item is not listed as active / next in this file, do
not implement it without updating
`docs/sprint_objective.md` first.

Avoid broad full-review → fix → full-review loops.

Runtime sprints should normally name a small exact scope.
Docs-only governance sprints may name docs deliverables instead.

`docs/action_bank.md` must stay a current action ledger,
not an append-only history. Before major rewrites, archive
the previous version under `docs/sprints/` (sprint closure)
or `docs/archive/current-docs/` (ad hoc transition).

Historical details belong in:

- `docs/sprints/`
- `docs/archive/current-docs/`
- `eval_interactive/results/`
- `qa-reports/`

### 7.1 Retention sweep (per milestone close)

`docs/action_bank.md` stays a current ledger of open / active / deferred
items only (per the §7 rule above). At each **milestone close** the
deliver-agent:

1. Moves newly-closed R-item rows out of this file into
   `docs/action_bank_archive.md` §C as one compact row
   (`id | closed-by | close date | archive pointer`).
2. Appends the milestone's compact closed-sprint rows (§A) and the
   closed-milestone row (§B) to the archive.
3. Strips inline landed-history that accreted on §4 / §5 items during
   the milestone, leaving a one-line closure pointer.
4. Verifies the live file holds only open / active / deferred items and is
   under `soft_size_budget` (front matter).

Closed prose is never copied into the archive — only pointers move; the
authoritative detail stays in `docs/sprints/` and `docs/milestones/`.
Historical `action_bank.md:<line>` citations inside immutable archives
(`docs/sprints/`, `docs/archive/`, `docs/milestones/`, `.chats/`,
`compact/`) refer to the pre-migration file at git 17991c6.
