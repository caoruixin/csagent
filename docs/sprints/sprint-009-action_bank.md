# Action Bank

Date: 2026-05-07
Mode: current action ledger

## 1. Current phase

Current phase:
Sprint 9 — Tool Contract and Trace Observability Fidelity (in flight;
Sprint 9.1 sanitization closure fix applied — awaiting Codex re-review).

Latest closed sprint:
Sprint 8.2 — ResolveArticle Contract and MAX_STEPS Trace Honesty Closure.

Latest Codex decision (Sprint 9 first review):
- decision: fix_required
- blocking_count: 1
- blocker: O2 sanitization for failed-tool error surfaces and generic
  result maps. Closed in Sprint 9.1 (see `docs/10-handoff.md` §3.1).

Current recommendation:
After Sprint 9 closes, resume Eval Governance docs-only work. Do not
start another runtime sprint unless triage finds a new P0/P1 runtime
blocker.

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
  - partial intake-field persistence across clarification
    turns.

- Sprint 8 closed:
  - cs259 `active_use_case` contract hardened.
  - targeted cs259 and clean smoke runs commit UC-F.
  - `CONTRACT_VIOLATION:active_use_case = 0` in accepted
    clean runs.

- Sprint 8.2 closed:
  - M0a `resolve_article` schema/tool alignment on canonical
    `source_id` (legacy `article_id` alias accepted; missing
    error names `source_id`).
  - M0b `AgentRunResult.maxSteps` overload preserves
    `lastLlmRawResponse`; `AgentRunLoopImpl` passes it through
    on the loop-exhausted return.

- Sprint 9 in flight:
  - O0 `record_outcome` aligned on canonical `outcome_class`
    (lowercase `resolve | escalate | abandon`); legacy
    `outcome=RESOLVED` and uppercase aliases preserved;
    successful `outcome_class=resolve` writes a `session_outcomes`
    row; result payload carries normalised `outcome_class` +
    persisted `outcome`.
  - O0 `request_handover` schema now exposes optional `summary`;
    tool derives a safe size-bounded fallback summary from session
    + topic + reason + LLM `current_user_message`;
    canonical 23-value escalation-reason enum unchanged.
  - O1 `AgentRunLoopImpl` short-circuits to ESCALATE only on a
    SUCCESSFUL `request_handover` dispatch; `PhaseEvaluator`
    keeps RESOLVE on a failed-only `record_outcome`; failed
    terminal tools surface in `accumulated_tool_results` for LLM
    retry.
  - O2 `bot_turns.tool_calls` persists bounded sanitized
    `result_data` + one-line `result_summary` per entry;
    `resolve_article` body dropped in favour of safe summary
    fields; email PII redacted; size cap enforced; TraceViewer
    renders both legacy and new shapes.
  - O2 hardening (Sprint 9.1): failed-tool `error_message` and
    `result_summary` now share the same redaction path as
    `result_data`; generic sanitizer redacts values under
    sensitive keys (`password / token / secret / api_key /
    authorization / bearer / credential / credentials / …`)
    and sensitive-shaped values (email, phone, UK postcode,
    bearer header, api-key prefix, 32+ char credential) under
    benign keys.
  - `mvn -pl server test`: 760 / 0 / 0 / 0 (Sprint 9.1 adds
    7 tests; first-pass Sprint 9 was 753).
  - No FAQ corpus, CaseSpec, judge, routing, search threshold,
    advert-link tool, broad TraceViewer redesign, or Eval
    Governance scope opened.

## 3. Active / next actions

Sprint 9 deliverables (in flight):

| id  | deliverable                                                | status |
|-----|------------------------------------------------------------|--------|
| O0  | Align `record_outcome` and `request_handover` contracts    | done — awaiting Codex re-review |
| O1  | Terminal-state honesty for failed terminal tools           | done — awaiting Codex re-review |
| O2  | Bounded sanitized tool result_data / result_summary        | done (closure fix applied in Sprint 9.1) — awaiting Codex re-review |

After Sprint 9 closes (assuming Codex pass + 0 blocking), the
recommended next phase is Eval Governance docs-only work — but ONLY
if no new P0/P1 runtime blocker surfaces in the post-Sprint-9
manual probe.

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
| Sprint 9 | O0 record_outcome + request_handover contracts; O1 terminal-state honesty; O2 bounded sanitized tool result_data | in flight (awaiting Codex review) | will archive under `docs/sprints/sprint-009-*` on closure |

## 7. Carry-over rule

If an item is not listed as active / next in this file, do
not implement it without updating
`docs/sprint_objective.md` first.

Avoid broad full-review → fix → full-review loops.

Runtime sprints should normally name a small exact scope.
Docs-only governance sprints may name docs deliverables
instead.

`docs/action_bank.md` must stay a current action ledger,
not an append-only history. Before major rewrites, archive
the previous version under `docs/sprints/` (sprint closure)
or `docs/archive/current-docs/` (ad hoc transition).

Historical details belong in:

- `docs/sprints/`
- `docs/archive/current-docs/`
- `eval_interactive/results/`
- `qa-reports/`
