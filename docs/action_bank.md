# Action Bank

Date: 2026-05-09
Mode: current action ledger

## 1. Current phase

Current phase:
Sprint 15 closed by Codex review: decision=pass, blocking_count=0.
Ready for the next sprint to be selected. Sprint 15 (Script /
Policy Config Governance) landed M0 / M1 / M2 with no runtime
semantic change.

Sprint 14 alone returned `decision: fix_required, blocking_count: 1`
on its initial Codex review: §L1 lineage + §L2 diagnostics were
computed but stamped only on `@Transient` `BotSession` slots after
`BotTurn` was saved, so a save / reload trace could not observe them.
Sprint 14.1 closed that single blocker by persisting the snake_case
payload into `bot_turns.projected_context.faq_grounding` BEFORE save.
The Sprint 14.1 closure review returned `decision: pass,
blocking_count: 0`, accepting Sprint 14 + 14.1 as a unit. No DB
migration, no hard citation gate, no broad S1 rewrite.

Latest closed sprint:
Sprint 14 + Sprint 14.1 — FAQ / KB Evidence Lineage and Safety, with
the persistence closure (will archive under
`docs/sprints/sprint-014-*` and `docs/sprints/sprint-014.1-*` on the
next ad-hoc transition).

Latest accepted sprint:
Sprint 15 — Script / Policy Config Governance (Codex pass; ready
to archive under `docs/sprints/sprint-015-*` on the next ad-hoc
transition).

Previously closed sprint:
Sprint 14 + Sprint 14.1 — FAQ / KB Evidence Lineage and Safety,
with the persistence closure (Codex pass; will archive under
`docs/sprints/sprint-014-*` and `docs/sprints/sprint-014.1-*`).

Earlier closed sprint:
Sprint 13 — Runtime Freeze, Risk Policy, and Eval Guardrails
(archived under `docs/sprints/sprint-013-*`).

Latest Codex decisions:
- Sprint 13: decision: pass, blocking_count: 0 — runtime main flow
  remains frozen, risk signal vs escalation trigger is documented
  and covered by deterministic guardrails, diff stays within docs +
  tests. O1 deferred (docs-only) with narrow prompt proposal +
  golden-test acceptance criteria pre-specified.
- Sprint 14 (initial review): decision: fix_required, blocking_count:
  1 — §L1 / §L2 diagnostics were computed but stamped only on
  `@Transient` `BotSession` slots after `BotTurn` was saved, so a
  save / reload trace could not see them. Closed by Sprint 14.1.
- Sprint 14.1 (closure review): decision: pass, blocking_count: 0 —
  snake_case payload now persisted in
  `bot_turns.projected_context.faq_grounding` BEFORE save; Sprint 14
  + 14.1 accepted as a unit; citation extraction remains passive and
  non-blocking; diff stays narrow (no hard citation gate, no DB
  migration, no broad framework, no corpus / CaseSpec / judge /
  prompt / routing change, no escalation-enum change).
- Sprint 15: decision: pass, blocking_count: 0 — DriftDetector risk
  keywords are YAML-backed with parity coverage, script template
  version metadata is pinned against the approved docs, and
  knowledge retrieval / rerank thresholds are config-visible with
  unchanged defaults and diagnostics. Diff stays inside
  config-governance scope (no prompt, routing, judge, corpus,
  CaseSpec, hard citation gate, hot reload, dashboard, or broad
  runtime work). Sprint 15 introduced no runtime semantic change.

Current recommendation:
Sprint 15 closed (Codex pass). Recommended next phases are **Eval
Governance Follow-up** (primary, surfaced by the Sprint 15 Codex
review), narrow Sprint 16 §S1 hardening (only on real-traffic
evidence), or narrow corpus curation. No new runtime sprint unless
a new P0 / P1 runtime blocker is found. The Sprint 14 FAQ grounding
contract (`docs/faq_grounding_contract.md`) is the canonical source
for the §L1 evidence lineage and §L2 grounding diagnostics; the
Sprint 14 §L0 audit + repair (`qa-reports/faq-kb-lineage-and-url-audit.md`)
records the published-safety + canonical-URL findings. Further
runtime workstreams (full Issue Ledger, per-issue budgets, all-UC
task taxonomy, full skill runtime, handover payload rewrite,
new escalation reason enum value, hard runtime citation gate) remain
on the deferred / avoid list.

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

- Sprint 13 closed (Codex pass):
  - O0 runtime freeze decision + risk taxonomy doc landed at
    `docs/runtime_freeze_and_risk_policy.md`.
  - O1 risk-aware prompt / policy tuning recorded as docs-only
    deferral with the exact narrow prompt change proposal
    pre-specified.
  - O2 eval guardrails landed at
    `Sprint13RiskPolicyGuardrailsTest` (12 deterministic tests).

- Sprint 14 + 14.1 closed (Codex pass on the Sprint 14.1 closure
  review; Sprint 14 + 14.1 accepted as a unit):
  - L0 KB canonical URL / Help URL / published safety audit + fix
    landed. Source chain traced (CSV → build script → JSON → DB →
    service → tools); `Help_Site_URL__c` confirmed preserved end-
    to-end as `KbArticle.source_url`; published-safety filter
    enforced at SQL layer (`KbChunkRepository.findNearestByEmbedding*PublishedOnly`)
    AND at hit projection (defense-in-depth);
    `ResolveArticleTool` refuses unpublished with deterministic
    `article_unpublished_safe_refuse` reject reason; exposes
    `canonical_url` mirroring `search_knowledge.hits[*].canonical_url`
    plus `canonical_url_missing` and `safe_to_show` flags;
    `SearchKnowledgeTool` projects `hits[*].canonical_url_missing`
    so missing-URL DQ gaps are observable. QA report
    `qa-reports/faq-kb-lineage-and-url-audit.md` shipped.
  - L1 separate retrieved / resolved / cited source evidence landed
    via `SourceEvidenceLineage` value object +
    `CitationExtractor` (passive source_id / URL / conservative
    title match). Sprint 14.1 closure: the snake_case
    `retrieved_source_ids`, `resolved_source_ids`, `cited_source_ids`,
    `cited_canonical_urls` lists are now durably persisted on
    `bot_turns.projected_context.faq_grounding` (computed BEFORE
    `BotTurn.save(...)`); the same values are mirrored onto the
    `BotSession` `@Transient` slots for in-process readers. The
    legacy `bot_turns.source_ids` `text[]` write path is preserved
    verbatim. Sprint 14 §L1 explicitly does NOT use the lineage
    diff to gate / rewrite / loop the response.
  - L2 FAQ grounding contract + soft diagnostics landed.
    `docs/faq_grounding_contract.md` (new, normative) defines
    the six-class output taxonomy (`factual_answer`,
    `clarification`, `empathy_ack`, `handover`, `tool_status`,
    `intake_collection`) — only `factual_answer` requires
    grounding. `FaqOutputClassifier` heuristically classifies the
    bot reply with the runtime's `handoverDispatched` flag as the
    authoritative override. `FaqGroundingDiagnostics.compute(...)`
    derives the 6 observable diagnostic fields; Sprint 14.1 closure
    persists them under `bot_turns.projected_context.faq_grounding`
    (`output_class`, `faq_grounding_state`, `citation_present`,
    `citation_match`, `citation_drift`, `resolved_but_uncited`,
    `retrieved_but_unresolved`) and mirrors the same values onto the
    7 `BotSession` transient slots (`faqOutputClass`,
    `faqGroundingState`, `citationPresent`, `citationMatch`,
    `citationDrift`, `resolvedButUncited`, `retrievedButUnresolved`).
    Diagnostics are observability-only; Sprint 14 §L2 / Sprint 14.1
    explicitly do NOT introduce a hard citation gate, and the §G2
    FAQ-grounded-resolve guard remains the only enforced runtime
    check on this surface.
  - `mvn -pl server test`: 859 / 0 / 0 / 0 (was 854 pre-Sprint-14.1;
    +5 Sprint-14.1 closure persistence regressions).
  - `mvn -pl server test -Dtest='Sprint14*,Sprint141*'`: 38 / 0 / 0 / 0
    (Sprint 14 §L0 ingestion / search filter / resolve safety,
    §L1 lineage, §L2 diagnostics, plus Sprint 14.1 trace
    persistence closure).
  - `mvn -pl server test -Dtest='Sprint10*Test,Sprint11*Test,
    Sprint12*Test,Sprint13*Test,PhaseEvaluatorPlanTest,Cs014*,
    Cs066*,Cs095*,Cs002*,Cs029*,Cs176*,Cs001*,EscalationReason*Test,
    Sprint6*,Sprint7*Test,Sprint8*Test,Sprint9*Test'`: 269 / 0 /
    0 / 0.
  - `pytest eval_interactive/tests/`: 294 / 0.
  - Smoke runs were NOT executed — Sprint 14 introduces no FAQ
    corpus content change, no judge change, no CaseSpec change,
    no prompt change, no escalation enum change, no routing
    taxonomy change, and no eval-output schema change. The
    current canonical baseline (`docs/current_eval_baseline.md`)
    was NOT promoted; remains the post-Sprint-8 r1 / r2 runs.
  - No FAQ corpus, CaseSpec, judge, broad routing taxonomy,
    handover payload rewrite, full Issue Ledger, all-UC task
    taxonomy, new escalation reason enum value, system prompt
    edit, DB schema migration, or hard citation gate was opened.

## 3. Active / next actions

Sprint 15 deliverables (accepted / ready to archive — Codex
`decision: pass, blocking_count: 0`):

| id | deliverable | status |
|----|-------------|--------|
| M0 | Externalize DriftDetector / risk keywords to YAML | done — new `config/risk-keywords.yaml` (5 hard-shift groups + escalation regex parity); `RiskKeywordsConfig` loader + structural validation (required fields, no duplicate keywords, valid regex, fail-fast); `DriftDetector` constructor-injects the config; behaviour parity verified by `Sprint15RiskKeywordsConfigTest` (11 cases) and the legacy `DriftDetectorTest` (18 cases) under the new wiring |
| M1 | Script library version pin + docs ↔ YAML consistency check | done — `templates.yaml` declares `library_version: "v1.1"` and `library_version_date: "2026-04-21"` matching `docs/fixed_script_library_v1.md` §11; `ScriptLibraryService` reads + fail-fast validates the pin; `Sprint15ScriptLibraryConsistencyTest` (5 cases) fails the build on version drift, missing required template IDs, drifted variable contract, or duplicate ids; no script copy edits, no forbidden-phrase rule edits |
| M2 | Retrieval / answer gate / rerank fallback thresholds config + diagnostics | done — new `KnowledgeRetrievalProperties` `@ConfigurationProperties("knowledge.retrieval")` with defaults pinned to previous hardcoded values (ANN 20 / retrieval-gate 0.3 / answer-gate 3.5 / rerank-candidates 8 / top-results 3 / fallback 2.5); start-up range validation (`fallback < answer-gate`, `ann ≥ rerank ≥ top`); `KnowledgeSearchService` + `RerankService` constructor-injected; threshold-gate / parse-failure / call-failure diagnostics added; `Sprint15KnowledgeRetrievalConfigTest` (9 cases — defaults parity + 7 negative validation tests); `RerankServiceTest` and `Sprint14KnowledgeSearchPublishedFilterTest` updated to pass the default properties bean and remain green |

Sprint 14 + Sprint 14.1 deliverables (closed — Codex pass on the
Sprint 14.1 closure review):

| id | deliverable | status |
|----|-------------|--------|
| L0 | KB canonical URL / Help URL / published safety audit + fix | done — published-only ANN queries, `ResolveArticleTool` refusal of unpublished, `canonical_url` / `canonical_url_missing` / `safe_to_show` flags, `qa-reports/faq-kb-lineage-and-url-audit.md`; 12 focused tests |
| L1 | Separate retrieved / resolved / cited source evidence | done — `SourceEvidenceLineage` + `CitationExtractor`; persisted under `bot_turns.projected_context.faq_grounding` (Sprint 14.1 closure); 4 `BotSession` transient slots mirrored; legacy `bot_turns.source_ids` preserved; 7 focused tests |
| L2 | FAQ grounding contract + soft diagnostics | done — `docs/faq_grounding_contract.md` (canonical); `FaqOutputClass`, `FaqOutputClassifier`, `FaqGroundingDiagnostics`; persisted under `bot_turns.projected_context.faq_grounding` (Sprint 14.1 closure); 7 `BotSession` transient slots mirrored; 14 focused tests |
| 14.1 | FAQ grounding observability persistence closure | done — Codex Sprint 14 blocker fix: lineage + diagnostics computed BEFORE `BotTurn.save(...)`, snake_case payload merged into `bot_turns.projected_context.faq_grounding`; no DB migration; no hard citation gate; 5 focused trace-persistence tests; Sprint 14.1 closure review returned `decision: pass, blocking_count: 0` |

With Sprint 14 + 14.1 and Sprint 15 all closed (Codex pass), the
next recommended phase is **Eval Governance Follow-up** (surfaced
by the Sprint 15 Codex review). No new runtime sprint unless a new
P0 / P1 runtime blocker is found. Narrow Sprint 16 §S1 hardening
candidates surfaced by the Sprint 14 §L0 audit are recorded in
`docs/faq_grounding_contract.md` §6 (R-faq-grounded-resolve-bypass,
R-cited-but-unresolved, R-resolved-but-uncited-rate,
R-canonical-url-missing-rate) — all deferred until real-traffic
evidence motivates them.

## 4. Deferred runtime candidates

| id | candidate | status | owner | notes |
|---|---|---|---|---|
| D-cs015-description-keyword-moderation-cue | description-keyword moderation signal in `UseCaseRouter.buildModerationRoutingContext` for forms with no `ad_id` | deferred | runtime routing | only if selected later; preserve cs095 negative guard; no broad moderation suite |
| D-cs176-UC-I-drift | unjustified UC-I drift on cs_176 r2 | deferred | runtime routing | explicit-human-help → `user_requested` regression already green; drift is separate |
| D-S3-no-prior-search-guard | refuse `request_handover(faq_miss_threshold_exceeded)` without a prior `search_knowledge` | deferred | runtime/tool-use | not needed for cs259 Sprint 6/8 closure; only reconsider if new evidence appears |
| D-S5-Tier2-runtime-guard | runtime guard for Tier-2 policy reasoning if prompt-only fix proves insufficient | deferred | policy/runtime | do not implement unless prompt path proves insufficient |
| D-broad-routing-taxonomy | broad routing taxonomy rewrite | deferred / avoid | none | do not reopen without explicit new phase |
| D-advert-link-product-decision | whether the bot may provide a direct advert URL for the `tool_scope_blocked` follow-up shape (manual probe `a7e20173`) | deferred — product decision | product / policy | runtime currently routes the follow-up to handover with `tool_scope_blocked`; on-design until product policy says otherwise; do not implement an advert-link tool without policy sign-off |
| D-rerank-fallback-diagnostics | distinguish `rerank_llm` score from `rerank_fallback` score in observability | partially landed in Sprint 15 §M2 (logs only) | runtime/observability | Sprint 15 §M2 surfaces parse-failure / call-failure / future-failure fallback usage in the log stream and stamps `top_is_fallback_score=true|false` on `answer_miss` diagnostic; full per-hit attribution in trace JSON / dashboard remains deferred until a corpus-level rerank investigation is opened |
| D-full-issue-ledger | full per-issue ledger (`issues[]`, per-issue budgets, all-UC task taxonomy) on top of the Sprint 11 progressive-resolve MVP | deferred / avoid | none | Sprint 11 explicitly carved this out; do not reopen without a new objective doc and explicit acceptance criteria |
| D-skill-runtime-framework | full skill runtime framework + handover payload rewrite | deferred / avoid | none | not needed for Sprint 11 / 12 / 13; reopen only with a new objective doc |
| D-prompt-risk-signal-handling | narrow "Risk signal handling" block in `system_prompt.txt` | deferred — Eval Governance trigger | prompt / Eval Governance | exact wording + 5 focused golden prompt tests pre-specified in `docs/runtime_freeze_and_risk_policy.md` §6.2; reopen on first real-traffic case demonstrating a refund / liability / appeal promise OR a credentials request |
| D-new-escalation-reason-enum | adding / renaming a value in the canonical 23-value `escalation_reason` enum (e.g. dedicated `risk_observed_continue`) | deferred / avoid | none | cross-cuts the eval-side `ESCALATION_TRIGGER_VALUES` set; needs a coordinated migration via a new objective doc |
| D-hard-citation-gate | hard runtime citation gate (refuse / rewrite / loop on missing citation) | deferred / avoid | none | Sprint 14 §L2 explicitly carved this out; observability via `citationPresent` / `citationMatch` / `citationDrift` / `resolvedButUncited` is in place. Reopen only when real-traffic evidence escalates the diagnostic signals into a P0/P1 blocker |
| D-faq-grounded-resolve-bypass | refuse / replan a FINAL_ANSWER shape that paraphrases a `retrieved_but_unresolved` hit on a FAQ-path UC | deferred — Sprint 16 §S1 candidate | runtime / S1 | observable today as `retrieved_but_unresolved=true` on a `factual_answer` turn; current §G2 guard handles the dominant `request_handover(faq_miss_threshold_exceeded)` shape, not the FINAL_ANSWER shape |
| D-cited-but-unresolved | hallucination signal candidate when `citation_drift=true` with a cited source_id never retrieved/resolved | deferred — diagnostics-only | runtime / S1 | needs reproducible cases before any runtime hardening; watch the §L2 diagnostics surface |

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
| Sprint 13 | O0 runtime freeze decision + risk taxonomy doc; O1 risk-aware prompt/policy tuning (docs-only deferral); O2 eval guardrails for constrained continue vs immediate escalation | closed | `docs/sprints/sprint-013-*` |
| Sprint 14 | L0 KB canonical URL / Help URL / published safety audit + fix; L1 separate retrieved/resolved/cited source evidence; L2 FAQ grounding contract + soft diagnostics | closed (initial review fix_required → resolved by Sprint 14.1; accepted as a unit on the Sprint 14.1 closure pass) | will archive under `docs/sprints/sprint-014-*` |
| Sprint 14.1 | FAQ grounding observability persistence closure (`bot_turns.projected_context.faq_grounding`); fix the Sprint 14 Codex blocker | closed (Codex pass) | will archive under `docs/sprints/sprint-014.1-*` |
| Sprint 15 | M0 externalize DriftDetector / risk keywords to YAML; M1 script library version pin + docs ↔ YAML consistency check; M2 retrieval / answer gate / rerank fallback thresholds config + diagnostics | closed (Codex pass) | will archive under `docs/sprints/sprint-015-*` |

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
