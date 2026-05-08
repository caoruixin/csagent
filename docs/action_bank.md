# Action Bank

Date: 2026-05-08
Mode: current action ledger

## 1. Current phase

Current phase:
Sprint 11 — Progressive Resolve MVP (in flight; awaiting Codex review).

Latest closed sprint:
Sprint 10 — Runtime Re-route MVP.

Latest Codex decision (Sprint 10 review):
- decision: pass
- blocking_count: 0
- summary: L0 / L1 / L2 reroute MVP within scope; regression evidence
  green.

Current recommendation:
After Sprint 11 closes (assuming Codex pass + 0 blocking), the next
recommended phase is **closure or Eval Governance docs sprint**. No
new runtime sprint unless a P0 / P1 runtime blocker is found.
Sprint 11 closes the same-UC continuation gap explicitly carved out
by Sprint 10; further runtime workstreams (full Issue Ledger,
per-issue budgets, all-UC task taxonomy, full skill runtime,
handover payload rewrite) remain on the deferred / avoid list.

## 2. Current accepted state

- Sprint 6 closed:
  - G0 ReadTimeout mitigation — 120 s create-session timeout
    widen only; no ReadTimeout retry.
  - G1 explicit human-help → `user_requested` focused
    evidence.
  - G2 FAQ-grounded-resolve S1 guard.

- Sprint 7 + 7.1 closed:
  - `candidate_use_cases` projection + DISCOVER cue.
  - UC-FP vs UC-A routing tiebreaker wiring.
  - `intake_state` projection.
  - partial intake-field persistence across clarification turns.

- Sprint 8 closed:
  - cs259 `active_use_case` contract hardened.
  - targeted cs259 and clean smoke runs commit UC-F.
  - `CONTRACT_VIOLATION:active_use_case = 0` in accepted clean runs.

- Sprint 8.2 closed:
  - M0a `resolve_article` schema/tool alignment on canonical
    `source_id` (legacy `article_id` alias accepted).
  - M0b `AgentRunResult.maxSteps` overload preserves
    `lastLlmRawResponse`.

- Sprint 9 + 9.1 closed:
  - O0 `record_outcome` aligned on canonical `outcome_class`
    (lowercase `resolve | escalate | abandon`); legacy aliases
    preserved.
  - O0 `request_handover` schema exposes optional `summary` with
    safe runtime fallback derivation.
  - O1 `AgentRunLoopImpl` short-circuits to ESCALATE only on a
    SUCCESSFUL `request_handover` dispatch; failed `record_outcome`
    keeps RESOLVE.
  - O2 `bot_turns.tool_calls` carries bounded sanitized
    `result_data` + `result_summary` per entry.
  - O2 hardening: failed-tool error_message + result_summary share
    the redaction path; sensitive keys + sensitive-shaped values
    redacted.

- Sprint 10 closed:
  - L0 internal `RuntimeIntentClassifier` (NOT an agent-visible
    tool) + `IntentClassification` / `RerouteDecision` records
    covering the six Sprint-10 MVP shapes.
  - L1 `ControlKernel.applyRerouteDecision` inserted between
    `DriftDetector` (legacy HARD_SHIFT immediate-escalate branch
    removed) and `PhaseEvaluator.plan(...)`.
  - L2 minimal projected issue-state slots
    (`previous_active_use_case`, `drift_type`,
    `current_task_type`, `primary_entity`,
    `issue_status_summary`) emitted after `candidate_use_cases`.
    `REROUTE_DECISION` event surfaces the decision payload.

- Sprint 11 in flight (Sprint 11.1 closure fix applied):
  - M0 minimal same-UC task / entity state — Sprint 10 §L2 slots
    reused; new `task_status` + `last_entity_context_ref`
    transient fields on `BotSession`. Same-UC ad_id capture from
    the user message on UC-A turns; captured ad_id persisted into
    `form_context` for cross-turn reuse. `drift_type` now also
    surfaces `SAME_ISSUE` / `SAME_UC_NEW_TASK` on
    `CONTINUE_CURRENT` turns so the progressive-resolve signal
    survives the projection.
  - M1 `ResolveDisposition` enum (`CONTINUE_RESOLVE /
    ASKED_FOR_SLOT / ANSWERED_SUBTASK / READY_TO_CONFIRM /
    ESCALATE`) + `ResolveDispositionEvaluator`. `PhaseEvaluator.
    mapFinalAnswer` consults the evaluator on RESOLVE / FAQ
    FINAL_ANSWER. **Sprint 11.1 closure fix:** the FINAL_ANSWER
    branch now requires deterministic terminal evidence
    (successful `record_outcome` dispatch on this run) before
    returning `READY_TO_CONFIRM`; non-slot, non-clarifying
    grounded answers default to `ANSWERED_SUBTASK` and stay in
    RESOLVE. Soft next-step / clarifying answers continue to
    return `ASKED_FOR_SLOT`. `AgentRunLoopImpl` rejects
    `record_outcome(outcome_class=resolve)` on RESOLVE / FAQ when
    the session is not in CONFIRM / CLOSE (unchanged).
  - M2 progressive UC-A / UC-C regression suite
    (`Sprint11ProgressiveResolveTest`, 14 tests; +3 Sprint-11.1
    closure regressions for the terminal-evidence guard).
  - `mvn -pl server test`: 796 / 0 / 0 / 0 (was 793 pre-Sprint-11.1;
    +3 Sprint-11.1 closure tests; was 782 pre-Sprint-11).
  - `python -m pytest -p no:capture eval_interactive/tests/`:
    294 / 0.
  - No FAQ corpus, CaseSpec, judge, broad routing taxonomy,
    handover payload rewrite, full Issue Ledger, all-UC task
    taxonomy, or Eval Governance scope opened.

## 3. Active / next actions

Sprint 11 deliverables (in flight; Sprint 11.1 closure fix applied):

| id  | deliverable                                                   | status |
|-----|---------------------------------------------------------------|--------|
| M0  | Minimal same-UC task / entity state on top of Sprint 10 §L2   | done — accepted by Codex Sprint 11 review |
| M1  | `ResolveDisposition` enum + RESOLVE → CONFIRM transition guard | Sprint 11.1 closure applied — terminal-evidence requirement; awaiting Codex re-review |
| M2  | Progressive UC-A / UC-C regression suite                      | done — accepted by Codex Sprint 11 review (+3 Sprint-11.1 closure regressions) |

After Sprint 11 closes (assuming Codex pass + 0 blocking), the
recommended next phase is **closure or Eval Governance docs sprint**.
No new runtime sprint unless a new P0 / P1 runtime blocker is found.

## 4. Deferred runtime candidates

| id | candidate | status | owner | notes |
|---|---|---|---|---|
| D-cs015-description-keyword-moderation-cue | description-keyword moderation signal in `UseCaseRouter.buildModerationRoutingContext` for forms with no `ad_id` | deferred | runtime routing | only if selected later; preserve cs095 negative guard; no broad moderation suite |
| D-cs176-UC-I-drift | unjustified UC-I drift on cs_176 r2 | deferred | runtime routing | explicit-human-help → `user_requested` regression already green; drift is separate |
| D-S3-no-prior-search-guard | refuse `request_handover(faq_miss_threshold_exceeded)` without a prior `search_knowledge` | deferred | runtime/tool-use | not needed for cs259 Sprint 6/8 closure; only reconsider if new evidence appears |
| D-S5-Tier2-runtime-guard | runtime guard for Tier-2 policy reasoning if prompt-only fix proves insufficient | deferred | policy/runtime | do not implement unless prompt path proves insufficient |
| D-broad-routing-taxonomy | broad routing taxonomy rewrite | deferred / avoid | none | do not reopen without explicit new phase |
| D-advert-link-product-decision | whether the bot may provide a direct advert URL for the `tool_scope_blocked` follow-up shape (manual probe `a7e20173`) | deferred — product decision | product / policy | runtime currently routes the follow-up to handover with `tool_scope_blocked`; on-design until product policy says otherwise; do not implement an advert-link tool without policy sign-off |
| D-rerank-fallback-diagnostics | distinguish `rerank_llm` score from `rerank_fallback` score in observability | deferred — diagnostics-only | runtime/observability | needed for honest rerank attribution; no semantic redesign or threshold tuning is in scope; revisit only when a corpus-level rerank investigation is opened |
| D-full-issue-ledger | full per-issue ledger (`issues[]`, per-issue budgets, all-UC task taxonomy) on top of the Sprint 11 progressive-resolve MVP | deferred / avoid | none | Sprint 11 explicitly carved this out; do not reopen without a new objective doc and explicit acceptance criteria |
| D-skill-runtime-framework | full skill runtime framework + handover payload rewrite | deferred / avoid | none | not needed for Sprint 11 progressive resolve; reopen only with a new objective doc |

## 5. Eval governance / non-runtime backlog

| id | item | classification | owner | notes |
|---|---|---|---|---|
| G-stall-detector-calibration | cs066 / cs038 stall detector + persona pacing variance | judge_volatility / eval-side contract | Eval Governance | `STALL_AFTER_TOOL_INTENT` fires on legitimate intake clarification turns |
| G-L3-relevance-tone | L3 `relevance` and `tone_appropriateness` judges | judge_volatility | Eval Governance | flips across runs even on passing cases |
| G-FAQ-corpus-answerability | cs259 / cs192 / cs095 | faq_corpus_gap / product_policy_gap | Eval Governance / corpus audit | no resolve-grade article for the user's intent |
| G-label-confidence-registry | per-case label confidence + override audit consolidation | eval governance | Eval Governance | input to a future `case_label_confidence.md` |
| G-release-gate-policy | release gate definition (gates, thresholds, sign-off) | release governance | Eval Governance | input to a future `release_gate.md` |

## 6. Closed action index

| sprint   | action                                          | status | archive                       |
|----------|-------------------------------------------------|--------|-------------------------------|
| Sprint 1 | A1 / A2 / A3                                    | closed | `docs/sprints/sprint-001-*`   |
| Sprint 2 | B0 / B1 / B2 / B3                               | closed | `docs/sprints/sprint-002-*`   |
| Sprint 2.1 | cs014 CaseSpec correction (override path)     | closed | `docs/sprints/sprint-002-*`   |
| Sprint 3 | C0 / C1 / C2                                    | closed | `docs/sprints/sprint-003-*`   |
| Sprint 4 | E1 / E2 / E3                                    | closed | `docs/sprints/sprint-004-*`   |
| Sprint 5 | F0 / F1 / F2 / F3 (diagnostic only)             | closed | `docs/sprints/sprint-005-*`   |
| Sprint 6 | G0 / G1 / G2                                    | closed | `docs/sprints/sprint-006-*`   |
| Sprint 6.1 | H0 / H1 closure-normalisation                 | closed | `docs/sprints/sprint-006-*`   |
| Sprint 7 | I0 / I1 / I2                                    | closed | `docs/sprints/sprint-007-*`   |
| Sprint 7.1 | J0 partial intake persistence                 | closed | `docs/sprints/sprint-007-*`   |
| Sprint 8 | K0 cs259 active-use-case contract               | closed | `docs/sprints/sprint-008-*`   |
| Sprint 8.2 | M0a resolve_article source_id + M0b max-steps raw response | closed | `docs/sprints/sprint-008.2-*` |
| Sprint 9 | O0 record_outcome + request_handover contracts; O1 terminal-state honesty; O2 bounded sanitized tool result_data | closed | `docs/sprints/sprint-009-*`   |
| Sprint 9.1 | trace sanitization closure (failed-tool error_message + sensitive-key value redaction) | closed | `docs/sprints/sprint-009-*` |
| Sprint 10 | L0 RuntimeIntentClassifier + RerouteDecision; L1 cross-UC soft/risk shift before PhaseEvaluator.plan; L2 minimal issue-state projection | closed | `docs/sprints/sprint-010-*` |
| Sprint 11 | M0 minimal same-UC task/entity state; M1 ResolveDisposition + transition guard (Sprint 11.1 terminal-evidence closure); M2 progressive UC-A/UC-C regression suite | in flight (Sprint 11.1 closure applied; awaiting Codex re-review) | will archive under `docs/sprints/sprint-011-*` on closure |

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
