# Action Bank

Date: 2026-05-08
Mode: current action ledger

## 1. Current phase

Current phase:
Sprint 10 — Runtime Re-route MVP (in flight; awaiting Codex review).

Latest closed sprint:
Sprint 9 / 9.1 — Tool Contract and Trace Observability Fidelity.

Latest Codex decision (Sprint 9 re-review):
- decision: pass
- blocking_count: 0
- summary: Sprint 9.1 closed the previous O2 sanitizer blocker.

Current recommendation:
After Sprint 10 closes (assuming Codex pass + 0 blocking), the next
recommended phase is **Sprint 11 — Progressive Resolve MVP** (or a
closure fix if review surfaces one). Eval Governance docs-only work
remains a parallel option if no new runtime blocker is found.

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
  - `mvn -pl server test`: 760 / 0 / 0 / 0 at Sprint 9 close.

- Sprint 10 in flight:
  - L0 internal `RuntimeIntentClassifier` (NOT an agent-visible
    tool) + `IntentClassification` / `RerouteDecision` records
    covering the six Sprint-10 MVP shapes (UC-A → UC-C soft shift,
    UC-A same issue, UC-A same UC follow-up, UC-A → UC-J risk
    shift, explicit human request, payment-ambiguity negative
    guard).
  - L1 `ControlKernel.applyRerouteDecision` inserted between
    `DriftDetector` (legacy HARD_SHIFT immediate-escalate branch
    removed) and `PhaseEvaluator.plan(...)`. Existing distress /
    explicit-human / budget guards preserved.
  - L2 minimal projected issue-state slots
    (`previous_active_use_case`, `drift_type`,
    `current_task_type`, `primary_entity`,
    `issue_status_summary`) emitted after `candidate_use_cases`.
    `REROUTE_DECISION` event surfaces the decision payload.
  - `mvn -pl server test`: 782 / 0 / 0 / 0 (was 760 pre-Sprint-10;
    +22 Sprint-10 tests).
  - `python -m pytest -p no:capture eval_interactive/tests/`:
    294 / 0.
  - No FAQ corpus, CaseSpec, judge, broad routing taxonomy,
    handover payload rewrite, Issue Ledger, all-UC task taxonomy,
    or Eval Governance scope opened.

## 3. Active / next actions

Sprint 10 deliverables (in flight):

| id  | deliverable                                                | status |
|-----|------------------------------------------------------------|--------|
| L0  | Internal `RuntimeIntentClassifier` + `RerouteDecision` model | done — awaiting Codex review |
| L1  | Cross-UC soft / risk shift before `PhaseEvaluator.plan(...)` | done — awaiting Codex review |
| L2  | Minimal issue-state projection + drift observability       | done — awaiting Codex review |

After Sprint 10 closes (assuming Codex pass + 0 blocking), the
recommended next phase is **Sprint 11 — Progressive Resolve MVP**
(extends the Sprint-10 reroute layer with same-UC
progressive-resolve disposition handling). Eval Governance docs-only
work remains a viable alternative if no new runtime blocker is
found.

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
| D-progressive-resolve-mvp | same-UC progressive-resolve disposition handling on top of the Sprint-10 reroute layer | candidate Sprint 11 | runtime | covers the same-issue / same-UC follow-up loop with a `ResolveDisposition` enum + minimal `issue_status` lifecycle. Sprint 10 explicitly carved this out. |

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
| Sprint 10 | L0 RuntimeIntentClassifier + RerouteDecision; L1 cross-UC soft/risk shift before PhaseEvaluator.plan; L2 minimal issue-state projection | in flight (awaiting Codex review) | will archive under `docs/sprints/sprint-010-*` on closure |

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
