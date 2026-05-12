# Action Bank

Date: 2026-05-11
Mode: current action ledger

## 1. Current phase

Current phase:
Sprint 17 (Iteration Governance Lite — G0) is the active sprint as
of 2026-05-11. G0 is a docs-only governance sprint that extends
`docs/current/iteration_governance.md` from a Constitution-only
file into the full six-section governance bundle (Constitution +
Failure Brief Template + Fix Layer Classification Checklist +
Anti-Hardcode Review Prompt + Eval Acceptance Rules +
Architecture-Health Metric definitions), closes the `AGENTS.md`
constitution-chain gap (Option A: seed `AGENTS.md` with includes
of the three `docs/current/` governance docs), and cross-wires the
required Layer-classification + anti-hardcode stanza into the
sprint-objective format. G0 does not change runtime, prompt, FAQ
corpus, CaseSpec, judge, eval harness, or any test. G1 (Failure
Portfolio) and G2 (Interactive Eval Case Family + Shadow Split)
are deferred to subsequent governance sprints; see §5.1.

Preceding sprint (most recently closed, Codex pass):
Sprint 16 (Handover Exactly-Once Contract and Repro) landed as a
docs + characterization-test sprint. It documents the current
LLM-driven dual local handover persistence shape, separates three
historically-conflated contracts (trace evidence via
`request_handover`, outcome persistence via `record_outcome`, and
the future `HandoverOrchestrator` handover side-effect), adds a
known-unspecced-surface entry to the runtime freeze doc, opens
`docs/release_gate.md` with a blocking rule against real Salesforce
cutover until the handover side-effect is idempotent by
`session_id`, and ships characterization tests that repro the dual
local persistence (with the LLM-driven repro disabled / TODO
because it would fail under current behaviour by design — the
explicit trigger to a future "Single Handover Orchestrator" runtime
sprint).

Sprint 16 introduces no runtime semantic change. `RequestHandoverTool`,
`SessionManager.recordHandover`, `SalesforceService` /
`MockSalesforceService`, the handover payload schema, prompts,
routing, and eval CaseSpecs are all unchanged.

Earlier latest accepted sprint:
Sprint 15 closed by Codex review: decision=pass, blocking_count=0.
Sprint 15 (Script / Policy Config Governance) landed M0 / M1 / M2
with no runtime semantic change.

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
contract (`docs/current/faq_grounding_contract.md`) is the canonical source
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
    `docs/current/faq_grounding_contract.md` (new, normative) defines
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

Sprint 17 deliverables (G0 — Iteration Governance Lite; docs-only
governance sprint; no runtime, prompt, FAQ corpus, CaseSpec, judge,
eval harness, or test change):

| id | deliverable | status |
|----|-------------|--------|
| G0.1 | Extend `docs/current/iteration_governance.md` to the full six-section bundle | done — front matter added; Constitution promoted verbatim to Section 1 (with subsections 1.1–1.7); Sections 2–6 land Failure Brief Template, Fix Layer Classification Checklist (9 layers, 7 ordered questions, judge_calibration tail, human_review_required default), Anti-Hardcode Review Prompt (9 questions + 4 verdict values), Eval Acceptance Rules (target / neighbor / negative / shadow + safety / grounding / wrong-containment / over-escalation / architecture-health bars), and Architecture-Health Metric definitions (4 metrics, all `collection_status: not_started`) |
| G0.2 | Close the `AGENTS.md` constitution-chain gap (Option A) | done — `AGENTS.md` seeded with repo intent + explicit includes of `docs/current/doc_governance.md`, `docs/current/agent_context_guide.md`, `docs/current/iteration_governance.md` + how-to-use paragraph; `CLAUDE.md` unchanged; `@AGENTS.md` now resolves to a non-empty constitution chain |
| G0.3 | Add the Layer-classification + anti-hardcode stanza to the sprint-objective format | done — landed as Section 7 of `iteration_governance.md` with stanza template (Target failure layer / Tier-0 invariant / Semantic hardcode / Generalization coverage), exemption list (pure infra, docs-only, config-governance, characterization-test), and a hypothetical Sprint 18 worked example using `cs_example_001` |
| G0.4 | Update this file to reflect Sprint 17 (G0) and the next-up G1 / G2 backlog | done — §1 current-phase narrative now leads with Sprint 17 (G0); §3 carries this Sprint 17 entry; §5.1 (new) captures G1 / G2 as deferred governance-track backlog |

Sprint 16 deliverables (docs + characterization-test sprint; no
runtime change):

| id | deliverable | status |
|----|-------------|--------|
| H0 | Define handover exactly-once contract | done — new `docs/proposals/handover_orchestrator_design.md` separates three contracts (trace evidence via `request_handover`, outcome persistence via `record_outcome`, and the future `HandoverOrchestrator` handover side-effect); future invariant: at most one transmitted / `offline_logged` handover decision per `session_id`; Sprint 16 §10.1 entry added to `docs/runtime_freeze_and_risk_policy.md` |
| H1 | Characterization tests for dual-path handover | done — new `Sprint16HandoverDualPathReproTest` (mock / local persistence repro; passing today) and `Sprint16HandoverDualPathLlmDrivenReproTest` (LLM-driven dual local persistence; **disabled / TODO** because the dual write fails the future invariant by design — the explicit trigger to the future "Single Handover Orchestrator" runtime sprint); tests distinguish duplicated local log / payload from unproven real Salesforce double transfer |
| H2 | Release-gate / action-bank tracking | done — new `docs/release_gate.md` §1.1 blocking rule (no real Salesforce cutover until handover side-effect is idempotent by `session_id`); new "Single Handover Orchestrator" deferred runtime candidate in §4 below; the rule remains **not satisfied** at Sprint 16 close |

Single Handover Orchestrator action item (NOT marked fixed):

> **Single Handover Orchestrator.** Future runtime sprint to make
> the handover side-effect (Salesforce transfer + handover payload
> persistence + handover decision persistence + `ESCALATION_REQUESTED`
> emission) the responsibility of a single owner that is idempotent
> by `session_id`. Required before real Salesforce production
> cutover. Cross-references: `docs/proposals/handover_orchestrator_design.md`,
> `docs/release_gate.md` §1.1, `docs/runtime_freeze_and_risk_policy.md`
> §10.1.

Sprint 15 deliverables (accepted / archived — Codex
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
| L2 | FAQ grounding contract + soft diagnostics | done — `docs/current/faq_grounding_contract.md` (canonical); `FaqOutputClass`, `FaqOutputClassifier`, `FaqGroundingDiagnostics`; persisted under `bot_turns.projected_context.faq_grounding` (Sprint 14.1 closure); 7 `BotSession` transient slots mirrored; 14 focused tests |
| 14.1 | FAQ grounding observability persistence closure | done — Codex Sprint 14 blocker fix: lineage + diagnostics computed BEFORE `BotTurn.save(...)`, snake_case payload merged into `bot_turns.projected_context.faq_grounding`; no DB migration; no hard citation gate; 5 focused trace-persistence tests; Sprint 14.1 closure review returned `decision: pass, blocking_count: 0` |

With Sprint 14 + 14.1 and Sprint 15 all closed (Codex pass), the
next recommended phase is **Eval Governance Follow-up** (surfaced
by the Sprint 15 Codex review). No new runtime sprint unless a new
P0 / P1 runtime blocker is found. Narrow Sprint 16 §S1 hardening
candidates surfaced by the Sprint 14 §L0 audit are recorded in
`docs/current/faq_grounding_contract.md` §6 (R-faq-grounded-resolve-bypass,
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

### 5.1 Governance track backlog

The governance track is the multi-sprint sequence that converts the
research-proposed iteration governance into delivered gates. G0 is
the docs scaffolding (Sprint 17). G1 and G2 build on G0's templates.

| id | candidate | status | owner | notes |
|---|---|---|---|---|
| G1 | Human-led Failure Portfolio (10–20 representative failures from human experience / past traces, each filed as a Failure Brief per `docs/current/iteration_governance.md` §2) | done — Sprint 18 G1; 10 briefs filed (9 smoke + 1 manual-probe) at `docs/diagnostics/failure-briefs/`; full handoff `docs/sprints/sprint-018-handoff.md`; objective archive `docs/sprints/sprint-018-g1-failure-portfolio-objective.md` | deliver / human | unblocked G2; see §5.2 for the 18 R-items + 2 open observations the briefs surfaced |
| G2 | Interactive Eval Case Family + Shadow Split (target / neighbor / negative / shadow per failure brief, with the shadow split readable only to the human / review agent per `docs/current/iteration_governance.md` §5.1) | deferred — after G1 (now unblocked); also blocked on `R-smoke-regression-investigation` from §5.2 below | deliver / eval governance | depends on G1 briefs (delivered Sprint 18); lands the case families and the shadow split. Recommended to follow the regression investigation sprint so G2 starts from a clean smoke baseline |

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
| R-generator-get-customer-context-policy-mismatch | cs_001 (UC-C) + cs_011 (UC-D) + cs_259 (UC-F) | CaseSpec generator includes `get_customer_context` in `expected_tool_sequence` regardless of phase 2 §2.10 line 358 UC-A/UC-FP/UC-K restriction. Wave A5/A6 priority. |
| R-l3-judge-form-context-trust-rubric | cs_038 (Paul) + cs_040 (Mo) + cs_192 (Rita) | L3 judge over-reaches by criticizing form-supplied `first_name` as "without confirmation". Rubric should account for `form_context.first_name` as a trustable signal. |
| R-corpus-coverage-audit-per-uc | cs_095 (UC-D account/email) + cs_192 (UC-B free-items/giveaway) + cs_259 (UC-F payment) | Per-UC FAQ corpus coverage audit. Resolve-grade articles for common entry-point questions per UC. **No generator-synthesized articles** (only genuine help-center content). |
| R-faqMissCount-threshold-and-timing-review | cs_095 + cs_192 + cs_259 | Two-part: (a) `>= 2` threshold given auto-search burns one; (b) threshold check timing vs form-description fallback. Config governance, not runtime semantic change. |
| R-duplicated-greeting-projection-fix | cs_095 ("Hi Trish! Hi Trish") + cs_176 ("Hi Gary! Hi Gary") | Single projection / template-rendering bug rendering greeting twice. |

**Per-case L3 / governance**

| id | source brief | description |
|----|--------------|-------------|
| R-uc-b-customer-context-policy-review | cs_015 (Related observation) | Phase 2 §2.10 line 358 restricts `get_customer_context` to UC-A/UC-FP/UC-K. Should UC-B (Posting & Editing) be added? Tangential to cs_015's main failure but worth a separate product-policy review. |
| R-cs001-escalation-trigger-l3-review | cs_001 | CaseSpec trigger `clarification_budget_exhausted` conflicts with `intake fields (none)`. Wave A5/A6 review. |
| R-cs038-l3-review-intake-efficiency | cs_038 | Should intake completion at T2 be the correct `turn_efficiency` target? |
| R-cs040-l3-review-intake-completion-semantics | cs_040 | Should `escalation_reason=intake_complete_for_uc_k` + `intake_fields_collected=0` be a hard outcome fail (not just an L3 quality issue)? |
| R-persona-goal-summary-scope-clarity | cs_040 | **Conditional** — only open if G2 case-family work shows the same scope-broadening pattern on other personas. |
| R-cs095-uc-classification-l3-rereview | cs_095 | PRD / Eval = UC-D vs CaseSpec override (Wave A2.1 legacy) = UC-A. Substantive mismatch. Wave A5/A6 L3 re-review. **Most impactful follow-up for cs_095.** |
| R-l1-source-citation-quality-rubric | cs_095 | L1 `source_citation_present` accepts internal SF IDs (`ka44J000000gKxqQAE`). Tighten to require canonical_url OR article title. |
| R-cs176-escalation-reason-l3-review | cs_176 | Is `user_requested` the optimal expected reason for UC-E refund demand, or should UC-E have a more specific reason? |
| R-cs192-secondary-ucs-duplicate-uc-b | cs_192 | Low priority. CaseSpec lists UC-B both as primary and as `secondary_ucs`. Generator quirk. |

**G2 input / future input**

| id | source brief | description |
|----|--------------|-------------|
| R-g2-multi-turn-followup-case-family-design | cs_095 | G2 case-family construction should intentionally include multi-turn followup cases on FAQ-resolve UCs to exercise the `skill_state` surface. |

**New infra (from manual probe)**

| id | source brief | description |
|----|--------------|-------------|
| R-runtime-orchestrator-tool-call-deduplication | manual-probe 2026-05-13 | `infra` layer per §3.2 Q1. 1 LLM request → 3 identical `search_knowledge` executions, same params / results. Investigate root cause among 3 hypotheses (phase transition re-trigger, Turn 1 failed-call replay, LLM new request). Document orchestrator's intended de-dup / idempotency contract. Add regression test. |

**External / regression discovery**

| id | source | description |
|----|--------|-------------|
| R-smoke-regression-investigation | 2026-05-05 → 2026-05-10 smoke run drop (9/14 = 64% → 3/14 = 21%) | **P1, must resolve before G2** otherwise G2 case-family construction has no clean baseline. 6 cases regressed (cs_002, cs_011, cs_014, cs_038, cs_040, cs_066) with symptoms ranging from empty `escalation_reason` to `STALL:PLACEHOLDER_WITHOUT_FOLLOWUP` to `CONTRACT_VIOLATION:active_use_case`. Sprints 14 / 14.1 / 15 / 16 all declared no-runtime-semantic-change; trace evidence may contradict. Recommended next sprint. |

**Open observations (NOT opened as R-items)**

n=1 evidence is insufficient to open an R-item; controlled multi-shape
testing needed (rule recorded in
`docs/sprints/sprint-018-handoff.md` §0 / §8.7).

- **Bot ignores explicit phase-plan directives** — cs_259 (Sprint 7
  §I0 weak-candidate cue violation) + manual-probe (RESOLVE
  MUST-call-resolve_article violation). n=2 opportunistic
  observations across mixed surfaces; controlled multi-shape testing
  across UC-B / UC-C / UC-D / UC-F empty-form shapes needed before
  opening `R-prompt-phase-plan-directive-followship`. Tracked here
  for visibility.
- **ad_id form-vs-listing data consistency** — manual-probe (form
  `ad_id=ad-1003` vs listing `ad_id=AD-1001`). n=1; needs production
  data to know if this is a common shape. Not opening on n=1.

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
| Sprint 16 | H0 define handover exactly-once contract (`docs/proposals/handover_orchestrator_design.md` + runtime-freeze §10.1 known-unspecced-surface entry); H1 characterization tests for dual-path handover (LLM-driven repro disabled / TODO by design); H2 release-gate blocker + action-bank "Single Handover Orchestrator" item | docs + characterization-test sprint; no runtime change | will archive under `docs/sprints/sprint-016-*` |

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
