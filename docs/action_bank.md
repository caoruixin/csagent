# Action Bank

Date: 2026-05-09
Mode: current action ledger

## 1. Current phase

Current phase:
Sprint 13 — Runtime Freeze, Risk Policy, and Eval Guardrails
(in flight; awaiting Codex review).

Latest closed sprint:
Sprint 12 — Runtime Alignment Hardening and Validation
(archived under `docs/sprints/sprint-012-*`).

Latest Codex decision (Sprint 12 review):
- decision: pass
- blocking_count: 0
- summary: Sprint 12 meets N0/N1/N2: backward-compatible drift /
  task / phase observability, deterministic targeted runtime
  alignment validation suite, residual classification + Eval
  Governance recommendation. Full server suite 809 / 0; Python
  eval 294 / 0.

Current recommendation:
After Sprint 13 closes (assuming Codex pass + 0 blocking), the next
recommended phase remains **Eval Governance docs sprint** (or
equivalent governance-only work). No new runtime sprint unless a
new P0 / P1 runtime blocker is found. The Sprint 13 risk-policy
doc (`docs/runtime_freeze_and_risk_policy.md`) is the single
canonical source for the freeze decision + risk taxonomy + the
deferred prompt change proposal; the Sprint 13 §O2 deterministic
guardrails are the runtime-side regression coverage. Further
runtime workstreams (full Issue Ledger, per-issue budgets, all-UC
task taxonomy, full skill runtime, handover payload rewrite,
new escalation reason enum value) remain on the deferred / avoid
list.

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
  - O0 `record_outcome` aligned on canonical `outcome_class`.
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
    `DriftDetector` and `PhaseEvaluator.plan(...)`.
  - L2 minimal projected issue-state slots emitted after
    `candidate_use_cases`. `REROUTE_DECISION` event surfaces the
    decision payload.

- Sprint 11 closed (Sprint 11.1 closure fix accepted by Codex):
  - M0 minimal same-UC task / entity state — Sprint 10 §L2 slots
    reused; new `task_status` + `last_entity_context_ref`
    transient fields on `BotSession`. Same-UC ad_id capture.
  - M1 `ResolveDisposition` enum + `ResolveDispositionEvaluator`.
    Sprint 11.1 closure: FINAL_ANSWER requires deterministic
    terminal evidence before READY_TO_CONFIRM.
  - M2 progressive UC-A / UC-C regression suite
    (`Sprint11ProgressiveResolveTest`, 14 tests).

- Sprint 12 closed (Codex pass):
  - N0 drift / task / phase observability hardening — eight
    backward-compatible `BotSession` `@Transient` slots; alias-key
    enrichment on `REROUTE_DECISION`; new `RESOLVE_DISPOSITION` and
    `RECORD_OUTCOME_GUARD` events with `terminal_evidence`;
    `drift_history` and `task_history` projection aggregates.
  - N1 targeted runtime alignment validation suite
    (`Sprint12RuntimeAlignmentValidationTest`, 13 tests).
  - N2 residual classification + next-phase recommendation captured
    in `docs/sprints/sprint-012-handoff.md`. Recommended next
    phase: **Eval Governance docs sprint**.

- Sprint 13 in flight (awaiting Codex review):
  - O0 runtime freeze decision + risk taxonomy doc landed at
    `docs/runtime_freeze_and_risk_policy.md` (frozen surfaces,
    tunable surfaces, future-runtime-sprint surfaces, frozen
    runtime contract, Level 1 / 2 / 3 risk taxonomy, term
    distinctions, five canonical examples, prompt change
    proposal, eval-guardrail cross-reference, acceptance
    criteria, out-of-scope list, references).
  - O1 risk-aware prompt / policy tuning **decision: docs-only
    deferral**. The exact narrow prompt change proposal is
    pre-specified in
    `docs/runtime_freeze_and_risk_policy.md` §6.2 with five
    focused golden prompt tests as acceptance criteria for the
    future edit; no `server/src/main/resources/prompts/system_prompt.txt`
    change landed in Sprint 13.
  - O2 eval guardrails landed at
    `server/src/test/java/com/gumtree/csagent/service/runtime/Sprint13RiskPolicyGuardrailsTest.java`
    — 12 deterministic Java tests covering Level 1 (paid Top Ad
    not showing), Level 2 (money back without dispute), Level 3a
    (explicit human request), Level 3b (scam → UC-J, GDPR / delete
    account → UC-G), and the negative-guard / cross-cutting
    risk_flag-vs-escalation_trigger separation. No live LLM, no
    FAQ service, no agent run loop dependence.
  - `mvn -pl server test`: 821 / 0 / 0 / 0 (was 809 pre-Sprint-13;
    +12 Sprint-13 §O2 regressions).
  - `mvn -pl server test -Dtest='Sprint10*Test,Sprint11*Test,
    Sprint12*Test,Sprint13*Test,PhaseEvaluatorPlanTest,Cs014*,
    Cs066*,Cs095*,Cs002*,Cs029*,Cs176*,Cs001*,EscalationReason*Test,
    Sprint6*,Sprint7*Test,Sprint8*Test,Sprint9*Test'`: 269 / 0 /
    0 / 0.
  - `python -m pytest eval_interactive/tests/`: 294 / 0.
  - Smoke runs were NOT executed — Sprint 13 is docs + tests only;
    no FAQ corpus, CaseSpec, judge, prompt, runtime production
    code, or routing taxonomy change. The current canonical
    baseline (`docs/current_eval_baseline.md`) was NOT promoted;
    remains the post-Sprint-8 r1 / r2 runs.
  - No FAQ corpus, CaseSpec, judge, broad routing taxonomy,
    handover payload rewrite, full Issue Ledger, all-UC task
    taxonomy, new escalation reason enum value, system prompt
    edit, or Eval Governance file was opened.

## 3. Active / next actions

Sprint 13 deliverables (in flight; awaiting Codex review):

| id | deliverable                                                                        | status |
|----|------------------------------------------------------------------------------------|--------|
| O0 | Runtime freeze decision + risk taxonomy doc                                        | done — `docs/runtime_freeze_and_risk_policy.md` (canonical) |
| O1 | Risk-aware prompt / policy tuning                                                  | done — docs-only deferral; exact narrow prompt change proposal pre-specified in policy doc §6.2; no `system_prompt.txt` edit |
| O2 | Eval guardrails for constrained continue vs immediate escalation                   | done — `Sprint13RiskPolicyGuardrailsTest` 12 / 0; Levels 1 / 2 / 3a / 3b + negative guard + cross-cut |

After Sprint 13 closes (assuming Codex pass + 0 blocking), the
recommended next phase is **Eval Governance docs sprint** (or
equivalent governance-only work). No new runtime sprint unless a
new P0 / P1 runtime blocker is found.

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
| D-skill-runtime-framework | full skill runtime framework + handover payload rewrite | deferred / avoid | none | not needed for Sprint 11 / 12 / 13; reopen only with a new objective doc |
| D-prompt-risk-signal-handling | narrow "Risk signal handling" block in `system_prompt.txt` | deferred — Eval Governance trigger | prompt / Eval Governance | exact wording + 5 focused golden prompt tests pre-specified in `docs/runtime_freeze_and_risk_policy.md` §6.2; reopen on first real-traffic case demonstrating a refund / liability / appeal promise OR a credentials request |
| D-new-escalation-reason-enum | adding / renaming a value in the canonical 23-value `escalation_reason` enum (e.g. dedicated `risk_observed_continue`) | deferred / avoid | none | cross-cuts the eval-side `ESCALATION_TRIGGER_VALUES` set; needs a coordinated migration via a new objective doc |

## 5. Eval governance / non-runtime backlog

| id | item | classification | owner | notes |
|---|---|---|---|---|
| G-stall-detector-calibration | cs066 / cs038 stall detector + persona pacing variance | judge_volatility / eval-side contract | Eval Governance | `STALL_AFTER_TOOL_INTENT` fires on legitimate intake clarification turns |
| G-L3-relevance-tone | L3 `relevance` and `tone_appropriateness` judges | judge_volatility | Eval Governance | flips across runs even on passing cases |
| G-FAQ-corpus-answerability | cs259 / cs192 / cs095 | faq_corpus_gap / product_policy_gap | Eval Governance / corpus audit | no resolve-grade article for the user's intent |
| G-label-confidence-registry | per-case label confidence + override audit consolidation | eval governance | Eval Governance | input to a future `case_label_confidence.md` |
| G-release-gate-policy | release gate definition (gates, thresholds, sign-off) | release governance | Eval Governance | input to a future `release_gate.md` |
| G-prompt-risk-signal-evaluation | when / whether to land the deferred Sprint 13 prompt edit | prompt governance | Eval Governance | trigger on first real-traffic refund-promise / liability-decision / credentials-request finding |

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
| Sprint 11 | M0 minimal same-UC task/entity state; M1 ResolveDisposition + transition guard (Sprint 11.1 terminal-evidence closure); M2 progressive UC-A/UC-C regression suite | closed | `docs/sprints/sprint-011-*` |
| Sprint 12 | N0 drift/task/phase observability hardening; N1 targeted runtime alignment validation suite; N2 residual classification + next-phase decision | closed | `docs/sprints/sprint-012-*` |
| Sprint 13 | O0 runtime freeze decision + risk taxonomy doc; O1 risk-aware prompt/policy tuning (docs-only deferral); O2 eval guardrails for constrained continue vs immediate escalation | in flight (awaiting Codex review) | will archive under `docs/sprints/sprint-013-*` on closure |

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
