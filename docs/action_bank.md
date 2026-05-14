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
| G2 | Interactive Eval Case Family + Shadow Split (target / neighbor / negative / shadow per failure brief, with the shadow split readable only to the human / review agent per `docs/current/iteration_governance.md` §5.1) | done — Sprint 20 Track A; 10 case families × (≥1 target + ≥2 neighbor + ≥2 negative + ≥2 shadow) = 70 CaseSpec-shaped entries delivered; v0 shadow-split mechanism (directory boundary + custom-path-only loading via `CaseSetManager.load_custom(path)` + documented self-restraint) delivered; full handoff `docs/sprints/sprint-020-handoff.md`; fix iteration `docs/sprints/sprint-020-fix-handoff.md`; objective archive `docs/sprints/sprint-020-objective.md`; Codex fix re-review `docs/sprints/sprint-020-fix-codex-review.md` (substantive findings closed, packaging-only blocker rolled forward in close commit) | deliver / eval governance | v1 hardening direction: `R-shadow-include-flag-runner-gate` (`infra`); prompt consumption: `R-already-called-prompt-consumption` (`prompt_projection`) |

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
| R-generator-get-customer-context-policy-mismatch | cs_001 (UC-C) + cs_011 (UC-D) + cs_259 (UC-F) | **status: done — Sprint 21 rejected as non-eval_spec (§3.2 Q7); reclassified to `product_policy` and routed to new R-phase2-uc-cdf-customer-context-policy-widen (phase 2 §2.10 line 358 widening). See `docs/sprints/sprint-021-handoff.md` §3.7 + §8.2.** **Sprint 22 correction (2026-05-14):** the policy-mismatch premise was based on a misread of phase 2 line 358 (a UC-H-local prose annotation inside the UC-H-01 YAML block) as the cross-UC rule. The actual cross-UC allowlist at `docs/foundational/phase2_domain_realization_spec.md` §2.10.1 line 1098 explicitly permits `get_customer_context` for UC-C, UC-D, and UC-F (corroborated by `docs/customer_service_tool_spec_v0_2.yaml` line 260 and `docs/current/customer_service_tool_spec_v0_3.md` line 60). The CaseSpec generator was correct; no policy widening is needed. The behavioural question — does the bot actually use the tool when account-state matters on FAQ-miss shapes in UC-C / UC-D / UC-F? — is moved to `R-uc-cdf-get-customer-context-bot-actual-usage`. |
| R-l3-judge-form-context-trust-rubric | cs_038 (Paul) + cs_040 (Mo) + cs_192 (Rita) | L3 judge over-reaches by criticizing form-supplied `first_name` as "without confirmation". Rubric should account for `form_context.first_name` as a trustable signal. |
| R-corpus-coverage-audit-per-uc | cs_095 (UC-D account/email) + cs_192 (UC-B free-items/giveaway) + cs_259 (UC-F payment) | Per-UC FAQ corpus coverage audit. Resolve-grade articles for common entry-point questions per UC. **No generator-synthesized articles** (only genuine help-center content). |
| R-faqMissCount-threshold-and-timing-review | cs_095 + cs_192 + cs_259 | Two-part: (a) `>= 2` threshold given auto-search burns one; (b) threshold check timing vs form-description fallback. Config governance, not runtime semantic change. |
| R-duplicated-greeting-projection-fix | cs_095 ("Hi Trish! Hi Trish") + cs_176 ("Hi Gary! Hi Gary") | Single projection / template-rendering bug rendering greeting twice. |

**Per-case L3 / governance**

| id | source brief | description |
|----|--------------|-------------|
| R-uc-b-customer-context-policy-review | cs_015 (Related observation) | Phase 2 §2.10 line 358 restricts `get_customer_context` to UC-A/UC-FP/UC-K. Should UC-B (Posting & Editing) be added? Tangential to cs_015's main failure but worth a separate product-policy review. |
| R-cs001-escalation-trigger-l3-review | cs_001 | **status: done — Sprint 21 approved override (case_spec_overrides.yaml entry for source_session_id 570Q5000008kr6LIAQ); `expected.escalation_trigger` flipped `clarification_budget_exhausted` → `faq_miss_threshold_exceeded` with matching `bot_handling_pattern` rewrite. See `docs/sprints/sprint-021-handoff.md` §3.1.** |
| R-cs038-l3-review-intake-efficiency | cs_038 | **status: deferred — Sprint 21 schema-blocked. The override schema does not currently support `scoring.*` overrides (e.g. moving the intake-efficiency check between `outcome_checks` and `hard_checks`); depends on `R-case-spec-overrides-schema-scoring-extension`. See `docs/sprints/sprint-021-handoff.md` §3.2.** |
| R-cs040-l3-review-intake-completion-semantics | cs_040 | **status: deferred — Sprint 21 schema-blocked + Tier-0 candidate territory. Depends on `R-case-spec-overrides-schema-scoring-extension` (eval-spec path) OR `R-escalation-reason-runtime-evidence-contract-review` (runtime path). See `docs/sprints/sprint-021-handoff.md` §3.3.** |
| R-persona-goal-summary-scope-clarity | cs_040 | **Conditional** — only open if G2 case-family work shows the same scope-broadening pattern on other personas. |
| R-cs095-uc-classification-l3-rereview | cs_095 | **status: done — Sprint 21 approved override (case_spec_overrides.yaml entry for source_session_id 570Q5000008U5C9IAK; supersedes the Wave A2.1 legacy UC-A pin, which is removed from the legacy block + header comment updated). UC primary flipped UC-A → UC-D; secondary [UC-D, UC-K] → [UC-A, UC-K]. Cascade rule satisfied in-direction (Sprint 20 cs_095 family unchanged; family was authored assuming UC-D primary). See `docs/sprints/sprint-021-handoff.md` §3.4 + §5.** |
| R-l1-source-citation-quality-rubric | cs_095 | L1 `source_citation_present` accepts internal SF IDs (`ka44J000000gKxqQAE`). Tighten to require canonical_url OR article title. |
| R-cs176-escalation-reason-l3-review | cs_176 | **status: done (rejected) — Sprint 21 rejected as non-eval_spec (§3.2 Q5); CaseSpec `user_requested` is correct (the user explicitly demanded refund); the bot chose the wrong escalation_reason family (`faq_miss_threshold_exceeded`). Reclassified to `semantic_planner`; remediation routed to new `R-cs176-semantic-planner-escalation-family-discrimination`. See `docs/sprints/sprint-021-handoff.md` §3.5 + §8.1.** |
| R-cs192-secondary-ucs-duplicate-uc-b | cs_192 | **status: done — Sprint 21 approved override (case_spec_overrides.yaml entry for source_session_id 570Q5000008rbcjIAA); `secondary_ucs` deduped [UC-K, UC-D, UC-B] → [UC-K, UC-D]; primary stays UC-B. Zero-scoring-impact correction (the L2 `correct_uc` check uses union of primary + secondary; dedupe leaves union unchanged). See `docs/sprints/sprint-021-handoff.md` §3.6.** |

**G2 input / future input**

| id | source brief | description |
|----|--------------|-------------|
| R-g2-multi-turn-followup-case-family-design | cs_095 | G2 case-family construction should intentionally include multi-turn followup cases on FAQ-resolve UCs to exercise the `skill_state` surface. |

**New infra (from manual probe)**

| id | source brief | description |
|----|--------------|-------------|
| R-runtime-orchestrator-tool-call-deduplication | manual-probe 2026-05-13 | **status: partial** — layer reclassified to `semantic_planner` per Sprint 19 §4.3 (LLM emitted three identical `search_knowledge` calls across three consecutive AgentRunLoop steps within the same T2 RESOLVE; no orchestrator-side amplification; `AgentRunLoopImpl.java` has no de-duplication path). Remediation split into read-side `R-prompt-projection-already-called-soft-signal` (`prompt_projection`) for the soft-signal slot and write-side (the future `HandoverOrchestrator` design freeze in `docs/proposals/handover_orchestrator_design.md`). Sprint 19 de-dup / idempotency contract write-up at `docs/sprints/sprint-019-handoff.md` §4.2. |

**External / regression discovery**

| id | source | description |
|----|--------|-------------|
| R-smoke-regression-investigation | 2026-05-05 → 2026-05-10 smoke run drop (9/14 = 64% → 3/14 = 21%) | **status: partial** — P1 investigation complete (Sprint 19); remediation deferred across 3 follow-on R-items. 6 regressed cases resolved to a multi-shape root cause: 4 cases (cs_002 / cs_014 / cs_038 / cs_040) and 1 partial case (cs_011) share the slow-LLM placeholder loop (`infra`, see `R-slow-llm-placeholder-coalesce`); cs_011 additionally shows planner failure to escalate-on-explicit-request (`semantic_planner`); cs_066 shows UC-K intake-complete `case_id` binding loss (`skill_state`, see `R-uc-k-intake-complete-case-id-binding`). Sprint 19 §3.2 walks at `docs/sprints/sprint-019-handoff.md` §3. |

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
| R-prompt-projection-already-called-soft-signal | Sprint 19 §4.2 + §11 (manual-probe Track B Layer 1) | **status: done** — delivered by Sprint 20 Track B. Slot landed in `ContextProjectionBuilder.build(..., List<ToolEvent> priorToolEvents)` via a new 6-argument overload; regression test `AlreadyCalledProjectionTest` (7 cases) demonstrates populated / empty / runtime-non-short-circuit bars; Sprint 20 fix iteration added `AgentRunLoopAlreadyCalledNonEnforcementIntegrationTest` (runtime-level non-enforcement evidence) and `AlreadyCalledCs011T2ShapeTest` (cs_011 T2 slot-population target). Full server suite 896 / 0 / 0 / 1 post-fix. See `docs/sprints/sprint-020-handoff.md` §5 and `docs/sprints/sprint-020-fix-handoff.md`. |
| R-idempotent-read-tool-short-circuit | Sprint 19 §4.2 + §11 (manual-probe Track B Layer 2) | `infra` layer, **conditional** on the soft-signal-only approach (`R-prompt-projection-already-called-soft-signal`) proving insufficient. For side-effect-free tools (`search_knowledge`, `lookup_*`, `get_*_context`), short-circuit identical-args re-emission within the same `AgentRunLoop.run(...)` by returning the cached prior result. Write tools opt out; they are owned by the Sprint 16 `HandoverOrchestrator` design freeze. |
| R-per-case-trace-dump-for-smoke-harness | Sprint 19 doc-status warning #1 + open question 6 + §11 | `infra` / eval harness. Re-enable per-case JSON trace dumping (tool_calls, phase_plan, projection, LlmCallEvents) under each `eval_interactive/results/<run-id>/` directory so future Track A §3.2 walks and Track B per-step investigations have first-party evidence. Scope: `eval_interactive/eval_interactive/` Python package (smoke runner). |
| R-sprint-narrative-vs-git-log-reconciliation | Sprint 19 doc-status warning #2 + open question 4 + §11 | governance / docs-only. The Sprint 18 G1 narrative that Sprints 14 / 14.1 / 15 / 16 made "no runtime semantic change" is contradicted by the git log (40 commits in the smoke gap window, 18 touching code). A reconciliation note in `docs/current/` or a fold-back into a `iteration_governance.md`-adjacent governance doc should land on `doc_governance.md` §"Code ahead of docs" cadence. |
| R-prompt-phase-plan-directive-followship | cs_259 (Sprint 7 §I0 violation, UC-F) + manual-probe (RESOLVE MUST-call-resolve_article violation, UC-A) + cs_011 T2 silence (Sprint 19 §3.3, UC-D) | **Promoted from Sprint 18 G1 open observation** per the conditional-broadening rule (n=3 across 3 UCs and 3 directive shapes). `prompt_projection` layer. Bot ignores explicit phase-plan directives the runtime has just placed in the per-turn projection (Sprint 7 §I0 weak-candidate cue, RESOLVE MUST-call-resolve_article, T2 cs_011 explicit handover request that bot promised to honor). Recommended approach: surface a soft directive-compliance diagnostic (e.g. `pending_directive_unfulfilled=true` on the next projection) so the LLM has explicit signal before responding — **not** a Java check that the bot complied. |

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
| R-already-called-prompt-consumption | Sprint 20 §12 + §11 open question 5; landed in Sprint 23 | `prompt_projection`. Sprint 23 landed a principled teaching paragraph in `server/src/main/resources/prompts/system_prompt.txt` between the Rules section and the DISCOVER phase guidance — names the Sprint 20 `already_called` slot, describes its three observable fields (`arguments_hash`, `at_step`, implicit `tool`), points the LLM at `accumulated_tool_results` for the prior payload, and leaves the re-emit decision to the LLM (soft signal; the slot does not block dispatch). Supporting coverage test `AlreadyCalledPromptConsumptionTest` (2 tests) passing; full server suite 898/0/0/1. cs_040 target rerun (`eval_interactive/results/20260514-080835/results.json`) shows the duplicate `search_knowledge` shape reversed (3 → 1 sk; no back-to-back duplicates) vs the original 2026-05-10 sequence. cs_014 rerun (`eval_interactive/results/20260514-081022/results.json`) is honestly reported as partial — user-visible PASS via handover, but the 4-consecutive `search_knowledge` shape persists on the underlying tool sequence. The fix-iteration evidence gate (≥1 target reversal) is satisfied by cs_040. Disposition: **landed with target-reversal evidence — teaching paragraph in server/src/main/resources/prompts/system_prompt.txt (between the Rules section and DISCOVER phase guidance); supporting coverage test AlreadyCalledPromptConsumptionTest (2 tests) passing; full server suite 898/0/0/1; cs_040 target rerun (eval_interactive/results/20260514-080835/results.json) shows the duplicate search_knowledge shape reversed (3 → 1 sk; no back-to-back duplicates); see Sprint 23 handoff §3 / §5 / §13.1 + "Fix iteration" section** (per Sprint 23 fix iteration objective §11). |
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
| R-phase2-uc-cdf-customer-context-policy-widen | Sprint 21 §3.7 systematic disposition (rejected) | **status: done — closed as premise-invalidated by Sprint 22 (2026-05-14).** The widening premise that phase 2 §2.10 line 358 forbade `get_customer_context` for UC-C / UC-D / UC-F was based on a misread of UC-H-local prose inside the UC-H-01 YAML block. The normative cross-UC matrix at `docs/foundational/phase2_domain_realization_spec.md` §2.10.1 line 1098 already permits the tool for UC-C, UC-D, and UC-F (corroborated by `docs/customer_service_tool_spec_v0_2.yaml` line 260 and `docs/current/customer_service_tool_spec_v0_3.md` line 60). No widening is needed. Closed without further action. Any residual behavioural question is captured in `R-uc-cdf-get-customer-context-bot-actual-usage` (Sprint 22 §5.2 open). Original Sprint 21 framing preserved for history: `product_policy` layer. Phase 2 §2.10 line 358 widening to add UC-C, UC-D, UC-F to the `get_customer_context` allowlist (n=3 instances across cs_001 / cs_011 / cs_259, human-reviewed). Scope: `docs/foundational/phase2_domain_realization_spec.md` §2.10 line 358; downstream runtime tool-policy update in `server/` if any Java-side guard mirrors the allowlist. Confidence: high. Recommended for the next product-policy / phase-2 fold-back sprint. **Sprint 21 §12.4 names this the recommended next sprint** because it unblocks both §11 Q1 (cs_011 override wording inconsistency) and §11 Q2 (cs_095 UC-D-primary contract gap) on the eval-spec surface. |
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
| R-slow-llm-placeholder-coalesce-honest-next-step | Sprint 23 §4.3 (Track B investigation-only deferral) | `infra`. The narrow Track B UX repair Sprint 23 deferred under the strict bundle gate. Proximate cause confirmed: `PhaseEvaluator.java` lines 754–770 emit identical placeholder text on every `DEADLINE_EXCEEDED` outcome without session-scope state-tracking; consecutive deadlines therefore produce back-to-back identical bot messages, which the runtime loop-detector flags. Proposed scope: add a `consecutiveDeadlineCount` column on `BotSession` (mirror of V12 `runtime_error_count`); increment in `PhaseEvaluator`'s `DEADLINE_EXCEEDED` branch, reset on any non-deadline outcome; first deadline emits existing placeholder; second consecutive deadline emits principled honest-next-step text naming the slowness and offering an actionable next step (handover / retry). Regression test bar: 2 consecutive `DEADLINE_EXCEEDED` outcomes produce ONE placeholder + ONE honest message. NO deadline-budget widening; NO model config change. Disposition: **implemented (Sprint 24 Track A).** Closed by Sprint 24 Track A. Delivered the proposed shape verbatim: V13 migration + `BotSession.consecutive_deadline_count` field + `SessionManager` builder init + `PhaseEvaluator` reset hook + DEADLINE_EXCEEDED threshold-gated message. Distinct 2nd-deadline text: *"I'm still having trouble responding in time. If you'd like, I can connect you with a specialist, or you can try again in a few minutes."* Regression `Sprint24DeadlinePlaceholderCoalesceTest` (3 tests) asserts placeholder / distinct-message / reset shape. NO deadline-budget widen; NO model config change. See `docs/sprints/sprint-024-handoff.md` §3. |
| R-llm-latency-budget-investigation | Sprint 23 §4.2 hypotheses (a) + (b); Sprint 19 §3.7 hypothesis as starting evidence | `infra` / diagnostic; paired with `R-slow-llm-placeholder-coalesce-honest-next-step`. The Sprint 19 §3.7 hypothesis (model switch `f2d4cb2 fix: switch primary llm to deepseek-v4-flash` widened latency variance) remains unverified because per-turn LLM latency data is not in `eval_interactive/results/20260510-134558/results.json`. Proposed scope: instrument per-turn LLM round-trip timing in `LlmInvocationService.invokeChat` (or surface the existing log-emitted timing into a structured field on results.json); collect a baseline distribution from a smoke rerun; compare to pre-`f2d4cb2` baseline if available. Output: a data table per case + per turn that distinguishes "deadline hit because model is genuinely slow" from "deadline hit because budget was too tight". Decision input to whether a budget / retry / model change is needed (out of Sprint 23 scope per §3 hard fence). NO budget edit in this R-item — investigation only. Disposition: **reframed-as-coarse-proxy + per-call-instrumentation deferred to follow-on R-item.** Sprint 24 Track B reframed the entry: confirmed that per-LLM-call latency is NOT in `eval_interactive/results/*/results.json` at the per-turn granularity needed; produced the coarse-proxy baseline from case-level `elapsed_ms` (see Sprint 24 handoff §4.1 / §4.2); reported the observed p95 widening with an explicit no-per-call-claim paragraph; deferred the per-call instrumentation work to the new `R-per-llm-call-latency-instrumentation` R-item below. Sprint 24 §10 question 1 surfaces a methodology question on the pre/post magnitude divergence between planning-turn citation and dev-session independent extraction; follow-on instrumentation R-item is the prerequisite to any deadline-budget or model-revert decision. |
| R-cs040-uc-k-topic-subject-routing | Sprint 23 §4.4 + Sprint 19 §3.5 | `prompt_projection`; **n=1; conditional opening**. cs_040 `active_use_case=UC-C` in the 2026-05-10 smoke run vs expected `UC-K` per `primary_uc` (form_context `topic_subject=technical issue intake`); Sprint 19 §3.5 documented the topic_subject cue not anchoring routing post-`8282783`. Proposed scope: investigate whether the topic_subject projection signal is reaching the routing prompt with sufficient salience; if not, propose a soft-signal addition (e.g. surface the topic_subject in the routing projection's `routing_signals` slot with appropriate weight). Disposition gate: open only if controlled multi-shape testing surfaces a second instance of the same routing-cue-loss shape. Disposition: **proposed (Sprint 23 §4.4; n=1; conditional opening per the user's controlled-multi-shape-testing bar).** Sprint 24 fence held (Track A fixed the *placeholder shape* on cs_040; routing surface untouched per Sprint 24 §3.5 + §6.1). |
| R-accumulated-tool-results-prompt-consumption | Sprint 23 §10 question 5; cs_014 partial-reversal evidence | `prompt_projection`; **conditional**. `accumulated_tool_results` is in every projection (since Sprint 8) but `server/src/main/resources/prompts/system_prompt.txt` has zero standalone teaching about the slot. Sprint 23's `already_called` teaching paragraph cross-references it but does not stand alone as teaching for `accumulated_tool_results`. The cs_014 fix-iteration rerun (`eval_interactive/results/20260514-081022/results.json`) showed the duplicate-`search_knowledge` shape persists on that case despite the Sprint 23 teaching (user-visible PASS via handover, but underlying 4-consecutive sk shape present) — supporting evidence that the `already_called` cross-reference alone is not sufficient on every target shape. Proposed scope: add a short paragraph (parallel to the Sprint 23 `already_called` paragraph) describing the slot's lifecycle (last-write-wins per tool name; populated by every successful dispatch in the run; the LLM should consult it before deciding to re-emit ANY tool). Disposition gate: conditional on Sprint 23's teaching not being sufficient — cs_014 partial result already provides one instance; opening is warranted if a follow-on smoke rerun confirms the broader pattern. Disposition: **proposed (Sprint 23 §10 question 5 + §11; cs_014 partial result as initial supporting evidence).** |
| R-per-llm-call-latency-instrumentation | Sprint 24 §4.5 (Track B coarse-proxy reframe) | `infra` / eval-harness. Per-LLM-call latency is not in `eval_interactive/results/*/results.json` at per-turn granularity (Sprint 23 §4.2 + Sprint 24 §4.3 confirm). Proposed scope: instrument per-turn LLM round-trip timing in `LlmInvocationService.invokeChat` (the call site where `LlmDeadlineExceededException` originates) OR surface the existing `LlmCallLogger`-emitted timing into a structured field on `results.json`; collect a baseline from a smoke rerun and compare against pre-`f2d4cb2`. **PREREQUISITE to any future deadline-budget widening or model-revert decision.** Until per-call latency is instrumented and persisted, the coarse proxy is a signal, not evidence; budget / model decisions made on the proxy alone are not justified. Sprint 24 §10 question 1 carries an open methodology question: the planning-turn coarse-proxy citation (pre p50≈10.4s / p95≈24.6s; post p50≈10.5s / p95≈27.6s; n="105 + 27 case-turns") could not be reproduced by the dev-session independent extraction (pre p50≈19.9s / p95≈30.5s; post p50≈41.1s / p95≈92.3s; n_cases=14 each); both agree on direction (post-`f2d4cb2` p95 widened), magnitude diverges. The instrumentation sprint must name the comparison ground-truth method (e.g. `llm_call_log` rows vs `results.json` case-level `elapsed_ms`) so the post-instrumentation rerun has an unambiguous baseline. Acceptance bar for the follow-on sprint: a per-turn duration column in `results.json` (or sibling artefact) populated on every smoke run; a worked example comparing post-`f2d4cb2` per-call p50/p95 against a fixed-prompt synthetic baseline that isolates LLM round-trip from tool dispatch / persistence overhead. Disposition: **proposed (Sprint 24 §4.5; investigation-only this sprint; instrumentation is a future sprint).** |

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
| Sprint 17 | G0.1 extend `docs/current/iteration_governance.md` to the 7-section governance bundle; G0.2 `AGENTS.md` constitution chain (Option A); G0.3 §7 Layer-classification + anti-hardcode stanza for the sprint-objective format; G0.4 action_bank refresh + §5.1 governance-track backlog | closed (Codex pass; docs-only governance sprint, no runtime change) | `docs/sprints/sprint-017-*` |
| Sprint 18 | G1 Human-led Failure Portfolio — 10 Failure Briefs at `docs/diagnostics/failure-briefs/` (9 smoke + 1 manual-probe); §5.2 G1 surfaced backlog with 18 R-items + 2 open observations | closed (docs-only governance; no Codex review per packaging-workflow option 2; no runtime change) | `docs/sprints/sprint-018-*` |
| Sprint 19 | A + B parallel investigation — Track A smoke-regression diagnosis (`R-smoke-regression-investigation` → multi-shape root cause; 5/6 cases share slow-LLM placeholder loop, cs_066 UC-K state-loss); Track B orchestrator tool-call de-dup investigation (`R-runtime-orchestrator-tool-call-deduplication` reclassified `infra` → `semantic_planner`; remediation split into read-side soft signal + write-side HandoverOrchestrator); 7 new R-items surfaced (incl. promoted `R-prompt-phase-plan-directive-followship` per conditional-broadening rule, n=3) | closed (Codex pass; investigation-only sprint per Bundle-or-defer policy; proposal-only on both tracks; no bundled fixes) | `docs/sprints/sprint-019-*` |
| Sprint 20 | G2 Interactive Case Family + Shadow Split (Track A) + `already_called` soft-signal slot (Track B) — Track A delivered 10 case families × (≥1 target + ≥2 neighbor + ≥2 negative + ≥2 shadow) = 70 CaseSpec-shaped entries + v0 shadow-split mechanism (directory boundary + custom-path-only loading + documented self-restraint); Track B delivered `already_called: [{tool, arguments_hash, at_step}]` projection slot in `ContextProjectionBuilder.build(...)` (observability-only, runtime non-enforcement). Required a narrow fix iteration after Codex first review `decision: fix_required, blocking_count: 2` (runtime non-enforcement test at `AgentRunLoopImpl.run` granularity + `_ACCESS_BOUNDARY.md` reconciliation). Codex fix re-review verdict `decision: out_of_scope_review, blocking_count: 1` on commit-boundary / packaging grounds only — both substantive findings closed cleanly per Codex's own evidence. | closed (substantive findings closed; packaging-only blocker rolled forward in close commit; full server suite 896 / 0 / 0 / 1) | `docs/sprints/sprint-020-*` |
| Sprint 21 | Wave A5/A6 L3 Review Batch (per-case + 1 systematic) — single-track semantic-touching sprint on the eval_spec surface. Delivered 7 L3 dispositions: 3 approved overrides (cs_001 escalation_trigger flip + bot_handling_pattern rewrite; cs_192 secondary_ucs dedupe; cs_095 UC-A → UC-D classification flip, supersedes Wave A2.1 legacy entry) written to `eval_interactive/case_spec_overrides.yaml` (17 applied / 0 pending); 2 rejected (cs_176 reclassified eval_spec → `semantic_planner`; systematic `R-generator-get-customer-context-policy-mismatch` reclassified eval_spec → `product_policy`) with new remediation R-items routed; 2 deferred (cs_038, cs_040) on missing override-schema scoring extension. 4 new R-items proposed: `R-phase2-uc-cdf-customer-context-policy-widen`, `R-cs176-semantic-planner-escalation-family-discrimination`, `R-case-spec-overrides-schema-scoring-extension`, `R-cs095-family-refresh-post-l3-reversal` (DORMANT). Cascade rule held (no Sprint 20 case-family content touched). Required a narrow fix iteration after Codex first review `decision: fix_required, blocking_count: 3` (approved-override evidence-gap on cs_001 / cs_095 / cs_192 — Ground-truth chain quoted but not brief `What happened?` / `What should` fields). Fix iteration was paste-in evidence remediation only (six verbatim brief-quote blocks added at handoff lines ~280 / ~452 / ~608, two per approved override); no override re-litigation, no case-family edits, no runtime / prompt / judge / YAML changes; cs_095 dimension-distinction language preserved byte-identical. Codex fix re-review verdict `decision: fix_required, blocking_count: 3` on typographical-fidelity grounds only (dropped markdown emphasis `*way*` / `**account-state-aware investigation**` / `**...**` + one missing trailing colon on cs_001's quote). | closed (A-with-evidence-gap-acknowledgment; substantive dispositions confirmed sound per Codex's own non-blocking checks at `docs/sprints/sprint-021-codex-review.md` lines 71–74 — override schema sane, no case-family edit, no judge-rubric edit, no semantic hardcode, cs_095 dimension-distinction byte-identical; human accepted the typographical evidence-package gap as close-eligible 2026-05-14; no further re-review round dispatched) | `docs/sprints/sprint-021-*` |
| Sprint 22 | phase 2 line 358 reconciliation + R-item closure (`get_customer_context` policy not-mismatch + new `R-uc-cdf-get-customer-context-bot-actual-usage`) | closed (docs-only governance) | `docs/sprints/sprint-022-*` |
| Sprint 23 | Track A bundle `R-already-called-prompt-consumption` (principled teaching paragraph in `server/src/main/resources/prompts/system_prompt.txt` between Rules and DISCOVER phase guidance, naming the Sprint 20 `already_called` slot + cross-referencing `accumulated_tool_results`, soft-signal / no enforcement) + Track B investigation-only (proximate cause `PhaseEvaluator.java` lines 754–770 confirmed; deeper cause model-latency hypothesis unverified — strict bundle gate read; narrow UX-repair shape proposed as new `R-slow-llm-placeholder-coalesce-honest-next-step` paired with `R-llm-latency-budget-investigation` diagnostic). Required a narrow strict-evidence-gate fix iteration after Codex first review `decision: fix_required, blocking_count: 3` (missing root-cause matrix columns; inferred-vs-conclusive evidence on Track A bundle; regression evidence not reversing target shape). Fix iteration took the **PASS branch**: augmented both root-cause matrices in handoff §3.2 / §4.2 with the six observable columns (`unavailable: <cause>` cells named the source-of-truth gaps in the 2026-05-10 `results.json` snapshot); ran a real-LLM target rerun of `cs_interactive_040` (`eval_interactive/results/20260514-080835/results.json`) showing the duplicate-`search_knowledge` shape reversed (3 → 1 sk; no back-to-back duplicates); relabelled the supporting `AlreadyCalledPromptConsumptionTest.java` with a top-of-file comment naming the rerun as primary evidence. cs_014 rerun honestly reported as partial. Mocked-LLM-as-primary-evidence hard fence honored (no new mocked-LLM integration test written; primary causal evidence is the real-LLM rerun, not a mock). Codex fix re-review verdict `decision: pass, blocking_count: 0` — Findings 1/2/3 all closed per Codex's own evidence cites. | closed (Codex fix re-review pass; PASS branch with target-reversal evidence on cs_040; full server suite 898/0/0/1 modulo the pre-existing uncommitted cosmetic `ACTIVE-UC TIEBREAKER` header rename which is unrelated to either Sprint 23 commit) | `docs/sprints/sprint-023-*` |
| Sprint 24 | Slow-LLM Placeholder Coalesce + Coarse Latency Proxy — two-track, semantic-touching on Track A (`infra`) and investigation-only on Track B (`infra` diagnostic). Track A delivered the deterministic UX repair on the cross-turn placeholder-loop surface verbatim per the objective: new `consecutive_deadline_count` `@Column` + `@Builder.Default = 0` `Integer` field on `BotSession` (mirrors V12 `runtime_error_count` shape); `SessionManager` builder init `.consecutiveDeadlineCount(0)`; `PhaseEvaluator` reset hook in the outcome-dispatch prologue (mirrors the existing ERROR reset with `DEADLINE_EXCEEDED` substituted); split `DEADLINE_EXCEEDED` / `LLM_UNAVAILABLE` case-block (`DEADLINE_EXCEEDED` increments the counter and threshold-gates between the existing placeholder on the first consecutive deadline and a distinct honest next-step message *"I'm still having trouble responding in time. If you'd like, I can connect you with a specialist, or you can try again in a few minutes."* on the second; `LLM_UNAVAILABLE` preserved unchanged); single Flyway V13 migration; behaviour-level regression suite `Sprint24DeadlinePlaceholderCoalesceTest` (3 methods asserting placeholder / distinct-next-step intent + no auto-handover / reset-to-placeholder). Trigger is the *event-shape* count of consecutive `DEADLINE_EXCEEDED` outcomes, NOT a regex / keyword / if-else on user content. Track B documented the honest coarse-proxy latency baseline from case-level `elapsed_ms` and `total_turns` in the two `results.json` files (pre n_cases=14 sum_turns=32 p50≈19.9s p95≈30.5s; post n_cases=14 sum_turns=42 p50≈41.1s p95≈92.3s — direction matches the planning-turn citation, magnitude diverges substantially), included the explicit no-per-call-claim paragraph, and proposed `R-per-llm-call-latency-instrumentation` as the prerequisite to any future deadline-budget widening or model-revert decision. Codex sprint-close review verdict `decision: pass, blocking_count: 0` on the first pass (no fix iteration required); §4.1 per-PR Anti-Hardcode verdict `approve`; all 8 Sprint 24 checks pass; hard fences all hold (no deadline-budget widening, no model config change, no `prompt_projection` work, no eval-spec work, no Tier-0 change, no coarse proxy represented as per-call evidence, `ChatController.java:125` untouched, `cs_040` UC-K routing untouched). Two Codex informational observations (non-blocking): (1) Track B magnitude discrepancy correctly carried as human open question for the follow-on instrumentation R-item; (2) deliver-agent-owned working-tree files are not scope drift under the packaging-rollforward rule. | closed (Codex sprint-close pass on first review; A — Clean close; full server suite 901/1/0/1 modulo the pre-existing `SystemPromptUserRequestedTiebreakerTest` failure inherited from an unrelated `system_prompt.txt` working-tree mod, not part of commit `e21b1b6`) | `docs/sprints/sprint-024-*` |

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
