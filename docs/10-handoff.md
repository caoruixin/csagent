# Current Handoff

Date: 2026-05-13
Branch: `design-v1-without-human-review`

## 1. Current phase

Current phase:
Sprint 20 (G2 Interactive Case Family + Shadow Split + Already-Called
Soft Signal — parallel A + B) is the active sprint as of 2026-05-13.
Sprint 20 is a two-track semantic-touching sprint. Track A is
content-authoring on the eval-spec surface; it converts the 10
G1 Failure Briefs into the four-class case-family structure
required by `docs/current/iteration_governance.md` §5.1 (target /
neighbor / negative / shadow) and builds the v0 shadow-split
mechanism that hides shadow-class cases from the dev agent.
Track B is the canonical `prompt_projection` bundle per Sprint 19
§"Bundle-or-defer policy": it surfaces an `already_called: [{tool,
arguments_hash, at_step}]` diagnostic slot in the per-step
projection so the LLM can see prior identical-args calls before
re-emitting. **Outcome: both tracks land.** Track A explicitly
does NOT remediate any G1 brief; remediation is a G3+ sprint that
consumes the families. Three deliverable surfaces changed:

- `eval_interactive/case_specs/case_families/` (new) and
  `eval_interactive/case_specs_shadow/` (new) — 61 new CaseSpec
  files across 10 families plus visible + shadow manifests + an
  access-boundary doc. 9 smoke targets referenced via the visible
  manifest (no file duplication per Context Pack §7.5 Option A);
  1 manual-probe target hand-authored from the brief's quoted
  system_instruction MUST clause + phase 2 UC-A policy. Totals:
  10 targets + 20 neighbors + 20 negatives + 20 shadows = 70
  CaseSpec-shaped entries.
- `server/src/main/java/com/gumtree/csagent/service/runtime/ContextProjectionBuilder.java`
  and `AgentRunLoopImpl.java` — new 6-argument `build(...)` overload
  emits `already_called` from successful prior `ToolEvent`s; legacy
  5-argument overload preserved (delegates with an empty list, still
  emits the slot for projection shape stability). Argument hash via
  stable Jackson canonicalisation + first-16-hex-chars of SHA-256.
  Slot is observability-only; the runtime does NOT short-circuit
  dispatch on slot population.
- `docs/sprints/sprint-020-handoff.md` (new) — 12-section handoff
  with the Context Pack, per-family detail, shadow-split mechanism
  description, slot wiring detail, 9-question anti-hardcode self-walk
  (verdict `approve`), generalization-coverage table, and §12 action
  bank delta recommendations (2 updated rows + 3 proposed new R-items
  + out-of-scope deferrals).
- `docs/10-handoff.md` (this file) updated lead to Sprint 20; Sprint
  19 demoted to "Preceding sprint".

Track A — case families and shadow split. Each of the 10 G1 briefs
gets a family with ≥1 target + ≥2 neighbor + ≥2 negative + ≥2 shadow.
Hand-authored neighbor / negative / shadow CaseSpecs carry
`source_dataset: case_family_authored` for grep-able provenance and
do not encode keyword / regex / per-UC matrix into YAML content
— each spec describes a user shape + expected behaviour, and a
remediation that ships against the visible target is independently
checkable against the family's negatives. The shadow-split v0
mechanism is a sibling `case_specs_shadow/` directory not loaded
by the default CaseSetManager; `_ACCESS_BOUNDARY.md` documents who
may read what and the v1 hardening direction. The cs_192 §3.2 Q2
stop-gate did NOT fire during target authoring (the target stays
on the brief's 2026-05-05 PASS-by-CaseSpec shape).

Track B — `already_called` slot. The `AgentRunLoopImpl.run(...)`
projection-build call site now passes the accumulated
`List<ToolEvent>` into the new 6-argument
`ContextProjectionBuilder.build(...)` overload. The builder filters to
successful events and emits an array of `{tool, arguments_hash,
at_step}` entries. New regression test
`AlreadyCalledProjectionTest` (7 cases) covers the three
sprint-objective behaviour bars: (a) populated when prior identical-
args call exists, (b) empty array otherwise, (c) runtime does not
short-circuit (two identical-args dispatches produce two slot
entries; both calls land in toolEvents). Full server suite re-run
after wiring: **894 / 0 / 0 / 1 (1 pre-existing skip)**.

This sprint declares the `docs/current/iteration_governance.md` §7
Layer-classification + anti-hardcode stanza fulfilled in
**multi-layer** form per the sprint objective: Track A = `eval_spec`
(case-family authoring) + `infra` (shadow-split mechanism); Track B
= `prompt_projection`. No Tier-0 invariant is added; no semantic
hardcode is introduced; no `human_review_required` flag fired. The
generalization-coverage table is the deliverable itself for Track A.

Preceding sprint (most recently closed, Codex pending sprint-close
review):
Sprint 19 (Smoke Regression Investigation + Orchestrator Tool-Call
De-Dup — parallel A + B) walked `docs/current/iteration_governance.md`
§3.2 per case for the two R-items the Sprint 18 G1 backlog named
as blocking G2: `R-smoke-regression-investigation` (Track A) and
`R-runtime-orchestrator-tool-call-deduplication` (Track B). Outcome
was proposal-only on both tracks: Track A found five of six regressed
cases share a slow-LLM placeholder shape (layer `infra` per §3.2
Q1; deferred remediation as `R-slow-llm-placeholder-coalesce`),
cs_011 lands at `semantic_planner` (failure to escalate on explicit
handover request), cs_066 at `skill_state` (UC-K case_id-binding
loss). Track B re-classified the orchestrator-de-dup R-item from
`infra` to `semantic_planner` and proposed two follow-on items:
`R-prompt-projection-already-called-soft-signal` (read-side soft
signal — now delivered by Sprint 20 Track B) and
`R-idempotent-read-tool-short-circuit` (conditional). Full handoff:
`docs/sprints/sprint-019-handoff.md`.

Earlier sprint (no Codex run):
Sprint 18 (Human-led Failure Portfolio — G1) converted 10
representative real failures into structured Failure Briefs under
`docs/diagnostics/failure-briefs/` per the Sprint 17 G0 §2
Failure Brief Template, populated the deferred-backlog ledger
with the R-items those briefs surfaced, and documented the two
ground-truth derivation techniques the briefs depend on in the
Sprint 18 handoff:

- `docs/diagnostics/failure-briefs/` carries 10 briefs: 9 smoke
  briefs covering Cluster A persistent failures (cs_015, cs_095,
  cs_176, cs_192, cs_259) and Cluster B mechanical-surface
  representatives (cs_001, cs_011, cs_038, cs_040) plus 1
  manual-probe brief
  (`manual-probe-2026-05-13-ad-visibility-multi-layer-failure.md`).
- `docs/action_bank.md` §5.2 records 18 G1-surfaced R-items (1
  Tier-0 candidate, 5 systematic, 9 per-case L3 / governance, 1
  G2 input, 1 new infra, 1 external / regression discovery) + 2
  open observations not opened on n=1 evidence.

No runtime, prompt, FAQ corpus, CaseSpec, judge, eval harness,
test, or script changed in Sprint 18. No Codex review was run
(per the human's packaging-workflow decision option 2: deliver
agent + human jointly authored the briefs in chat). G1 declared
the §7 Layer-classification + anti-hardcode stanza **exempt**
under the docs-only governance exemption. Full Sprint 18 handoff:
`docs/sprints/sprint-018-handoff.md`. Archive:
`docs/sprints/sprint-018-g1-failure-portfolio-objective.md`.

Earlier sprint (Codex pass):
Sprint 17 (Iteration Governance Lite — G0) landed the docs-only
governance bundle (`docs/current/iteration_governance.md` 7
sections + `AGENTS.md` constitution chain + `docs/action_bank.md`
§5.1 governance-track backlog); Codex `decision: pass,
blocking_count: 0`; archive under `docs/sprints/sprint-017-*`.

Earlier accepted sprint:
Sprint 16 (Handover Exactly-Once Contract and Repro) landed as a
docs + characterization-test sprint. No runtime behaviour was
changed.

Sprint 16 deliverables:

- §H0 — defined the handover exactly-once contract in a new
  `docs/proposals/handover_orchestrator_design.md`. The doc separates three
  historically-conflated contracts: (1) trace evidence via the
  `request_handover` tool entry on `bot_turns.tool_calls`, (2)
  outcome persistence via `record_outcome` →
  `session_outcomes`, and (3) the future `HandoverOrchestrator`
  handover side-effect (Salesforce transfer + payload persistence
  + decision persistence + `ESCALATION_REQUESTED` emission, owned
  by a single writer that is idempotent by `session_id`). Added
  a known-unspecced-surface entry to
  `docs/runtime_freeze_and_risk_policy.md` §10.1.
- §H1 — added `Sprint16HandoverDualPathReproTest` (3 cases). Two
  cases pass and characterize the current dual local-persistence
  shape: on the LLM-driven `request_handover` path,
  `RequestHandoverTool` writes `mock_handover_log` row #1 via
  `SalesforceService.requestHandover(...)` and
  `SessionManager.recordHandover` writes `mock_handover_log` row
  #2 directly through the same repository. The third case
  (`futureInvariant_atMostOneTransmittedHandoverDecisionPerSessionId_disabledUntilOrchestratorLands`)
  is `@Disabled` and encodes the future invariant — it would fail
  today by design and is the explicit trigger to the future
  "Single Handover Orchestrator" runtime sprint.
- §H2 — opened a new `docs/release_gate.md` with §1.1 blocking
  rule (no real Salesforce cutover until handover side-effect is
  idempotent by `session_id`); added a **Single Handover
  Orchestrator: exactly-once side-effect owner before Salesforce
  production cutover** action to `docs/action_bank.md` §3 + §4. The
  action item is **not** marked fixed.

Sprint 16 also adds future-invariant docstrings (no behaviour
change) on `RequestHandoverTool` and `SessionManager.recordHandover`
pointing to the design doc and the characterization test.

Earlier accepted sprint:
Sprint 15 closed by Codex review: decision=pass, blocking_count=0.

Sprint 15 (Script / Policy Config Governance) landed three
behaviour-preserving config-governance moves:
- §M0 externalizes the previously hardcoded `DriftDetector` risk
  keyword list and escalation regex into
  `server/src/main/resources/config/risk-keywords.yaml` via a new
  `RiskKeywordsConfig` loader (with structural validation).
- §M1 pins a `library_version` / `library_version_date` on
  `server/src/main/resources/scripts/templates.yaml` and adds a
  build-time docs ↔ YAML consistency test against
  `docs/fixed_script_library_v1.md`.
- §M2 externalizes the previously hardcoded retrieval / answer-gate /
  rerank fallback thresholds into a new
  `KnowledgeRetrievalProperties` `@ConfigurationProperties` bean
  (`knowledge.retrieval.*` in `application.yml`) with start-up range
  validation and rerank-fallback / threshold-gate diagnostics.

Sprint 15 introduced no runtime semantic change: no escalation
reasons, risk levels, prompt copy, routing rules, FAQ corpus
content, judge calibration, CaseSpec, or eval-output schema were
touched. The Codex Sprint 15 review accepted the diff as inside the
config-governance scope.

Earlier Sprint 14 + Sprint 14.1 background (closed, Codex pass):

Sprint 14 (FAQ / KB Evidence Lineage and Safety) initially returned
Codex `decision: fix_required, blocking_count: 1` — the §L1 / §L2
diagnostics were computed but stamped only on `@Transient` `BotSession`
fields, so a save / reload trace could not see them. Sprint 14.1
closed that single blocker by persisting the snake_case lineage +
diagnostics into the existing `bot_turns.projected_context` JSONB
column under a new `faq_grounding` object. The Sprint 14.1 closure
review returned `decision: pass, blocking_count: 0`, accepting Sprint
14 + 14.1 as a unit. No DB migration, no hard citation gate, no broad
S1 rewrite landed.

Latest accepted sprint:
Sprint 15 — Script / Policy Config Governance
(Codex `decision: pass, blocking_count: 0`; ready to archive under
`docs/sprints/sprint-015-*` on the next ad-hoc transition).

Previously closed sprint:
Sprint 14 + Sprint 14.1 — FAQ / KB Evidence Lineage and Safety, with
the persistence closure (closed earlier under Codex pass; will
archive under `docs/sprints/sprint-014-*` and
`docs/sprints/sprint-014.1-*` on the next ad-hoc transition).

## 2. Sprint 14 goal

Sprint 14 upgrades FAQ / KB grounding trustworthiness by making the
knowledge source chain, published safety, canonical URL availability,
and citation observability explicit — without introducing a broad hard
runtime citation gate, a new skill runtime framework, broad routing
rewrites, or additional mechanical escalation.

Scope is exactly the three Sprint 14 actions L0 / L1 / L2.

## 3. Sprint 14 implementation

### L0 — KB canonical URL / Help URL / published safety audit + fix

Code changes:

- `server/src/main/java/com/gumtree/csagent/repository/KbChunkRepository.java`
  — added `findNearestByEmbeddingPublishedOnly` and
  `findNearestByEmbeddingWithUcTagsPublishedOnly` queries that join
  `kb_articles` with `is_published = true`. Existing legacy methods
  preserved for backward compatibility.
- `server/src/main/java/com/gumtree/csagent/service/knowledge/KnowledgeSearchService.java`
  — routes through the new published-only ANN methods so unpublished
  candidates never enter the rerank pipeline. Added defense-in-depth
  hit-projection filter that drops any post-fetch unpublished article
  even if the ANN returned it via a stale cache. Stamps
  `canonical_url_missing` per hit.
- `server/src/main/java/com/gumtree/csagent/model/KnowledgeHit.java`
  — added `canonicalUrlMissing` boolean (Jackson `canonical_url_missing`).
- `server/src/main/java/com/gumtree/csagent/service/tools/SearchKnowledgeTool.java`
  — projects `hits[*].canonical_url_missing` into the agent-visible
  response so missing-URL data-quality gaps are observable.
- `server/src/main/java/com/gumtree/csagent/service/tools/ResolveArticleTool.java`
  — refuses unpublished articles with deterministic
  `article_unpublished_safe_refuse` reject reason
  (`UNPUBLISHED_REJECT_REASON` constant). Exposes `canonical_url`
  alongside legacy `source_url`. Stamps `canonical_url_missing` and
  `safe_to_show` (= published AND non-blank body).
- `server/src/main/java/com/gumtree/csagent/service/knowledge/KnowledgeIngestionRunner.java`
  — extracted `buildKbArticleFromJson(...)` package-private helper so
  the FAQ → KbArticle field-preservation contract (including
  `source_url` and `is_published`) is unit-testable.
- `scripts/build_knowledge_base.py` — additive: `mapping_summary_report.md`
  now carries a "URL & Published-Safety Coverage" block enumerating
  total / published / unpublished / with-canonical-URL / missing-URL /
  unsafe-to-show counts. JSON content unchanged.

QA report: `qa-reports/faq-kb-lineage-and-url-audit.md`.

Tests added (5 focused tests of the form Sprint 14 §L0 prescribed):

- `Sprint14KnowledgeIngestionCanonicalUrlTest`
  (4 cases — preservation of `Help_Site_URL__c` → `source_url`, respects
  explicit `published_status=false`, missing URL surfaces null,
  CSV-mapping fallback for `uc_tags`).
- `Sprint14KnowledgeSearchPublishedFilterTest`
  (4 cases — UC-scoped path routes through published-only ANN, no-tag
  path same, post-rerank projection drops still-unpublished article,
  missing-URL hit stamps `canonical_url_missing=true`).
- `Sprint14ResolveArticleSafetyTest`
  (4 cases — refuses unpublished with canonical reject reason,
  exposes `canonical_url` mirroring `search_knowledge`, missing URL
  classified as observable DQ gap, blank body marks `safe_to_show=false`).

### L1 — Separate retrieved / resolved / cited source evidence

Code:

- `server/src/main/java/com/gumtree/csagent/service/knowledge/CitationExtractor.java`
  (new) — passive citation extractor. Detects Salesforce KA `source_id`
  pattern, canonical URL pattern, and (conservatively) title fuzzy
  match against retrieved/resolved candidates. Stateless;
  side-effect-free.
- `server/src/main/java/com/gumtree/csagent/service/knowledge/SourceEvidenceLineage.java`
  (new) — record `(retrievedSourceIds, resolvedSourceIds,
  citedSourceIds, citedCanonicalUrls, retrievedCanonicalUrls)` with
  `fromToolEvents(toolEvents, botResponse)` constructor walking
  `search_knowledge` (retrieved) and `resolve_article` (resolved) tool
  events. §L0 contract: a refused unpublished resolve is NOT counted
  as resolved — the article remains in the
  `retrievedButUnresolvedSourceIds` set.
- `server/src/main/java/com/gumtree/csagent/model/BotSession.java`
  — added 4 new `@Transient` slots: `retrievedSourceIds`,
  `resolvedSourceIds`, `citedSourceIds`, `citedCanonicalUrls`. Schema
  unchanged; `bot_turns.source_ids` column preserved verbatim.
- `server/src/main/java/com/gumtree/csagent/service/runtime/ControlKernel.java`
  — `recordRunResult(...)` now stamps the new transient slots via the
  new `stampFaqGroundingObservability(...)` helper. The existing
  `bot_turns.source_ids` write path is untouched. Stamping is wrapped
  in try/catch so any unexpected pattern in tool events / bot text
  cannot break record-turn persistence.

Tests:

- `Sprint14SourceEvidenceLineageTest` (7 cases):
  retrieved-only does not imply cited, resolved evidence tracked
  separately, source_id mention detected, URL mention detected and
  mapped back to candidate, third-party URL recorded as free-form
  citation, missing citation observable but non-blocking, refused
  unpublished resolve excluded from resolved set.

### L2 — FAQ grounding contract + soft diagnostics

Docs:

- `docs/current/faq_grounding_contract.md` (new, normative) — defines the
  output class taxonomy, the §L1 evidence lineage construction, the
  §L2 diagnostic state transition table, and the non-blocking
  guarantee. Records future narrow Sprint 16 §S1 hardening candidates
  surfaced by the §L0 audit (`R-faq-grounded-resolve-bypass`,
  `R-cited-but-unresolved`, `R-resolved-but-uncited-rate`,
  `R-canonical-url-missing-rate`).

Code:

- `FaqOutputClass` enum — six classes: `factual_answer`,
  `clarification`, `empathy_ack`, `handover`, `tool_status`,
  `intake_collection`. Only `factual_answer` requires grounding.
- `FaqOutputClassifier` — heuristic classifier with
  `ClassifierContext(intakeUseCase, handoverDispatched)` so the
  runtime's `handoverDispatched` flag overrides any wording-based
  guess and intake UCs prefer `intake_collection` over
  `clarification` on a question shape.
- `FaqGroundingDiagnostics` — record `(outputClass, faqGroundingState,
  citationPresent, citationMatch, citationDrift, resolvedButUncited,
  retrievedButUnresolved)`. State table: `factual_grounded`,
  `factual_uncited`, `factual_unresolved`, `factual_unretrieved`,
  `non_factual`, `unknown`.
- `BotSession` — added 7 transient slots: `faqOutputClass`,
  `faqGroundingState`, `citationPresent`, `citationMatch`,
  `citationDrift`, `resolvedButUncited`, `retrievedButUnresolved`.
- `ControlKernel.stampFaqGroundingObservability(...)` — single call
  from `recordRunResult` populates §L1 + §L2 slots; observability-only
  with no rejection / rewrite / loop.

Tests:

- `Sprint14FaqGroundingDiagnosticsTest` (14 cases):
  taxonomy exemption check (only factual requires grounding),
  classifier coverage for each shape (clarification / empathy /
  handover-flag override / tool_status / intake / factual default), and
  the full diagnostic state table (`factual_grounded`,
  `factual_uncited`, `factual_unresolved`, `factual_unretrieved`,
  `citation_drift`, all five non-factual classes skip grounding,
  missing citation is observable not blocking).

### Audit findings (factual-answer bypass — Sprint 16 candidate)

Per Sprint 14 §L2, if the audit found a current factual-answer bypass
of search/resolve, document it as a future narrow Sprint 16 candidate.
The audit recorded **R-faq-grounded-resolve-bypass** in
`docs/current/faq_grounding_contract.md` §6: the existing Sprint 6 §G2 guard
refuses the `request_handover(faq_miss_threshold_exceeded)` shape, but
does NOT refuse a FINAL_ANSWER shape that paraphrases an unresolved
hit. This is observable today as `retrieved_but_unresolved=true` on a
factual-answer turn. Sprint 14 explicitly does NOT close it — the
guidance is for a future narrow Sprint 16 §S1 hardening if real-traffic
evidence motivates it.

## 4. Files changed (Sprint 14)

Production code:

- `server/src/main/java/com/gumtree/csagent/repository/KbChunkRepository.java`
- `server/src/main/java/com/gumtree/csagent/service/knowledge/KnowledgeSearchService.java`
- `server/src/main/java/com/gumtree/csagent/service/knowledge/KnowledgeIngestionRunner.java`
- `server/src/main/java/com/gumtree/csagent/service/knowledge/CitationExtractor.java` (new)
- `server/src/main/java/com/gumtree/csagent/service/knowledge/SourceEvidenceLineage.java` (new)
- `server/src/main/java/com/gumtree/csagent/service/knowledge/FaqOutputClass.java` (new)
- `server/src/main/java/com/gumtree/csagent/service/knowledge/FaqOutputClassifier.java` (new)
- `server/src/main/java/com/gumtree/csagent/service/knowledge/FaqGroundingDiagnostics.java` (new)
- `server/src/main/java/com/gumtree/csagent/service/runtime/ControlKernel.java`
  (added §L1/§L2 stamping helper; preserved all existing logic)
- `server/src/main/java/com/gumtree/csagent/service/tools/SearchKnowledgeTool.java`
- `server/src/main/java/com/gumtree/csagent/service/tools/ResolveArticleTool.java`
- `server/src/main/java/com/gumtree/csagent/model/KnowledgeHit.java`
- `server/src/main/java/com/gumtree/csagent/model/BotSession.java`
  (added 11 `@Transient` slots; no schema change)

Tests (new):

- `Sprint14KnowledgeIngestionCanonicalUrlTest` (4 cases)
- `Sprint14KnowledgeSearchPublishedFilterTest` (4 cases)
- `Sprint14ResolveArticleSafetyTest` (4 cases)
- `Sprint14SourceEvidenceLineageTest` (7 cases)
- `Sprint14FaqGroundingDiagnosticsTest` (14 cases)

Docs:

- `docs/current/faq_grounding_contract.md` (new, normative)
- `qa-reports/faq-kb-lineage-and-url-audit.md` (new — §L0 audit + repair)
- `docs/10-handoff.md` (this file)
- `docs/action_bank.md` (Sprint 14 row added)
- `docs/sprint_objective.md` retained — already contains the Sprint 14
  objective.

Scripts:

- `scripts/build_knowledge_base.py` — additive URL & Published-Safety
  Coverage block in the auto-generated mapping report.

No FAQ corpus content, CaseSpec, judge, broad routing taxonomy,
handover payload contract, Salesforce backend contract, full Issue
Ledger, all-UC task taxonomy, escalation enum, or system prompt was
touched.

No DB schema migration. The `bot_turns.source_ids` column is
preserved verbatim; new lineage / diagnostics fields are
session-transient observability material consumed via projection /
trace evidence.

## 5. Tests run

- `mvn -pl server test -Dtest='Sprint14*'`
  → **33 / 0 / 0 / 0** (Sprint 14 §L0/§L1/§L2 focused tests).
- `mvn -pl server test -Dtest='Sprint10*Test,Sprint11*Test,
  Sprint12*Test,Sprint13*Test,PhaseEvaluatorPlanTest,Cs014*,
  Cs066*,Cs095*,Cs002*,Cs029*,Cs176*,Cs001*,EscalationReason*Test,
  Sprint6*,Sprint7*Test,Sprint8*Test,Sprint9*Test'`
  → **269 / 0 / 0 / 0** (named Sprint 14 regression guards).
- `mvn -pl server test`
  → **854 / 0 / 0 / 0** at the Sprint 14 milestone (was 821
  pre-Sprint-14; +33 new Sprint 14 §L0/§L1/§L2 tests). Post Sprint
  14.1 closure the suite is **859 / 0 / 0 / 0** (+5 trace-persistence
  tests; see §15).
- `pytest eval_interactive/tests/`
  → **294 / 0** (full Python eval test suite).
- Smoke runs were NOT executed for Sprint 14. Sprint 14 introduces no
  FAQ corpus content change, no judge change, no CaseSpec change, no
  prompt change, no escalation enum change, no routing taxonomy
  change, and no eval-output schema change. The current canonical
  baseline (`docs/current_eval_baseline.md`) is preserved
  (post-Sprint-8 r1 / r2 runs).

## 6. KB URL / published audit result

| Metric | Value |
|--------|-------|
| Total articles | 218 |
| Articles published (`published_status=true`) | 218 |
| Articles unpublished (`published_status=false`) | 0 |
| Articles with `source_url` (canonical URL) | 180 |
| Articles missing `source_url` (canonical-URL DQ gap) | 38 |
| Articles unsafe to show (unpublished OR empty body) | 0 |

The 38 articles missing a canonical URL come from rows the Salesforce
export shipped without `Help_Site_URL__c`. They remain searchable but
the §L0 fix surfaces the gap as `canonical_url_missing=true` on every
search-hit and resolve-article response so a reviewer can classify the
case as a corpus-side curation task rather than a runtime fix. The
broader curation work is out of Sprint 14 scope and queued for the Eval
Governance / corpus-audit owner alongside `G-FAQ-corpus-answerability`.

Per `qa-reports/faq-kb-lineage-and-url-audit.md` §3, nine concrete §L0
fixes landed this sprint (published-safety filter at SQL layer + at hit
projection, refusal of unpublished `resolve_article`, exposure of
`canonical_url` alongside `source_url`, observable
`canonical_url_missing` flag on both tools, and the `safe_to_show`
conjunctive predicate on `resolve_article`).

## 7. Retrieved / resolved / cited evidence contract

Sprint 14 §L1 splits the historically-overloaded `sourceIds` concept
into four observably-distinct dimensions, all populated each turn and
durably persisted on `bot_turns.projected_context.faq_grounding`
(Sprint 14.1 closure) and mirrored onto in-memory `BotSession`
transient slots:

- `retrievedSourceIds` — IDs from successful `search_knowledge` events
  (post §L0 published-safety filter). De-duplicated; insertion order
  preserved.
- `resolvedSourceIds` — IDs that successfully passed through
  `resolve_article`. A refused unpublished resolve is NOT counted.
- `citedSourceIds` — IDs detected in the customer-visible reply by
  the passive `CitationExtractor` (source_id mention, URL mention
  mapped back to candidate, conservative title match).
- `citedCanonicalUrls` — URLs in the reply that did NOT correspond to
  any candidate `canonical_url`. Typically third-party (e.g. gov.uk).

The `bot_turns.source_ids` column is preserved verbatim as the
backward-compatible aggregate. Sprint 14 §L1 explicitly does NOT use
the diff between the four dimensions to gate / rewrite / loop the bot
response. The output is observability material — see
`docs/current/faq_grounding_contract.md` §3 for the canonical contract.

## 8. Soft diagnostic fields

Sprint 14 §L2 surfaces seven additional fields per turn under
`bot_turns.projected_context.faq_grounding` (Sprint 14.1 closure;
`docs/current/faq_grounding_contract.md` §4) and mirrors the same values onto
`BotSession` transient slots for in-process readers:

- `faqOutputClass` — taxonomy token (`factual_answer`, `clarification`,
  `empathy_ack`, `handover`, `tool_status`, `intake_collection`).
- `faqGroundingState` — coarse state (`factual_grounded`,
  `factual_uncited`, `factual_unresolved`, `factual_unretrieved`,
  `non_factual`, `unknown`).
- `citationPresent`, `citationMatch`, `citationDrift` — citation
  observability triplet.
- `resolvedButUncited`, `retrievedButUnresolved` — evidence-diff
  observability pair.

All seven are observability-only. Sprint 14 §L2 does NOT block, rewrite,
re-loop, or escalate based on any of them. The §G2 FAQ-grounded-resolve
guard from Sprint 6 remains the only enforced runtime check on this
surface.

## 9. Regression guard outcomes

All Sprint 14 active guards pass:

- `L1:escalation_reason_consistency` = **0** (never re-introduced).
- `CONTRACT_VIOLATION:active_use_case` = **0**.
- Existing FAQ S1 guard tests remain green
  (`AgentRunLoopS1FaqGroundedResolveGuardTest`).
- Sprint 6 §G0 ReadTimeout closure intact
  (`test_agent_client_session_create_timeout.py`).
- cs014 remains UC-C
  (`Cs014RouteAndLoopHandoverIntegrationTest`,
  `Cs014RouteAndDistressRegressionTest`).
- cs066 remains UC-K.
- cs095 remains UC-A / not UC-K / not UC-FP.
- cs002 distress reconciliation remains green.
- cs029 remains UC-D + `user_requested`.
- cs176 explicit-human-help → `user_requested` regression remains
  green (`Cs176ExplicitHumanHelpHandoverIntegrationTest`).
- All Sprint 7 / 7.1 / 8 / 8.1 / 8.2 / 9 / 9.1 / 10 / 11 / 11.1 / 12 /
  13 hard invariants remain green
  (named regression suite: 269 / 0).
- `RuntimeIntentClassifier` remains runtime-internal.
- `bot_turns.source_ids` write path unchanged.
- `bot_turns` / `bot_sessions` / `kb_articles` / `kb_chunks` schemas
  unchanged.

New Sprint 14 §L0 / §L1 / §L2 guards (33 deterministic JUnit tests):

- §L0 — `Sprint14KnowledgeIngestionCanonicalUrlTest`,
  `Sprint14KnowledgeSearchPublishedFilterTest`,
  `Sprint14ResolveArticleSafetyTest`.
- §L1 — `Sprint14SourceEvidenceLineageTest`.
- §L2 — `Sprint14FaqGroundingDiagnosticsTest`.

## 10. Remaining risks

**No new P0 / P1 blockers opened by Sprint 14.** The change is additive
observability + a narrow §L0 published-safety / canonical-URL fix; no
runtime main-flow architecture file was modified.

Sprint 14 §L0 audit surfaced four narrow follow-ups (recorded in
`docs/current/faq_grounding_contract.md` §6 — all deferred):

- **R-faq-grounded-resolve-bypass** — factual answers paraphrasing
  retrieved-but-unresolved hits. Observable today as
  `retrieved_but_unresolved=true` on a factual-answer turn. Future
  narrow Sprint 16 §S1 hardening candidate IF real-traffic shows
  reproducible cases.
- **R-cited-but-unresolved** — `citation_drift=true` with cited
  source_id never retrieved/resolved. Hallucination signal candidate;
  Eval Governance owns until reproduced.
- **R-resolved-but-uncited-rate** — corpus-level rate of uncited
  factual answers. Eval Governance.
- **R-canonical-url-missing-rate** — 38 articles missing
  `Help_Site_URL__c`. Corpus curation, not runtime.

Residuals carried forward from Sprint 13 §8 (unchanged):

- `R-cs015-description-keyword`, `R-cs176-UC-I-drift`,
  `R-S3-no-prior-search-guard`, `R-S5-Tier2-runtime-guard`,
  `R-stall-detector-calibration`, `R-L3-relevance-tone`,
  `R-FAQ-corpus-answerability`, `R-advert-link-product-decision`,
  `R-rerank-fallback-diagnostics`, `R-task-type-token-naming`,
  `R-record-outcome-loop`, `R-clean-baseline-promote`,
  `R-full-issue-ledger`, `R-skill-runtime-framework`,
  `R-prompt-risk-signal-handling`.

The current canonical eval baseline remains the post-Sprint-8 r1 / r2
runs documented in `docs/current_eval_baseline.md`. Sprint 14
explicitly does NOT promote a new canonical eval baseline.

## 11. Recommendation for Sprint 15 or Sprint 16

Recommended next phase:

**Eval Governance docs sprint** (or equivalent governance-only work)
remains the primary recommendation. Sprint 14 + 14.1 closed the FAQ /
KB evidence lineage and safety workstream (including the persistence
gap Codex flagged) that Sprint 13 §8 / §9 had open; the largest
residual category is still `judge_volatility` / `faq_corpus_gap` /
`product_policy_gap`, all governance-owned.

(The Sprint 14.1 Codex review surfaced **Script / Policy Config
Governance Sprint** as an alternative recommended next phase. Either
phase is reasonable; choose by prioritisation, not by Sprint 14 / 14.1
state.)

Alternative phases ranked:

1. **Eval Governance** — primary recommendation (above).
2. **Narrow Sprint 16 §S1 hardening** — only if real-traffic evidence
   demonstrates `R-faq-grounded-resolve-bypass` reproducibly affects
   answer correctness on a high-traffic UC. The Sprint 14 §L1 / §L2
   diagnostics surface (`retrieved_but_unresolved=true` on a
   `factual_answer` turn) is the trigger to look for. Until then, the
   §G2 prompt-side nudge already handles the dominant case.
3. **Narrow Corpus Curation** — fill the 38 missing
   `Help_Site_URL__c` rows. Owner: Eval Governance / corpus audit.
4. **Re-run validation after infra cleanup** — viable if Kimi /
   smoke credentials are available; runs alongside Eval Governance.
5. **Release Candidate Hardening** — premature; depends on a
   clean canonical baseline that has not yet been promoted.

Do not start another runtime sprint unless triage finds a new
P0 / P1 runtime blocker. Sprint 14 explicitly does NOT promote a new
canonical eval baseline.

## 12. Was Sprint 14 objective met?

Sprint 14 alone was **not** met under its initial Codex review: the
review correctly identified that the §L1 / §L2 diagnostics were stamped
only onto `@Transient` `BotSession` fields after `BotTurn.save(...)`,
so a normal save / reload trace could not observe them. Sprint 14.1
(§15 below) closed that single blocking gap. The Sprint 14.1 closure
review returned `decision: pass, blocking_count: 0`. Sprint 14 + 14.1
together are accepted:

- L0 KB canonical URL / Help URL / published safety audit + fix landed.
  Source chain traced (CSV → build script → JSON → DB → service →
  tools); `Help_Site_URL__c` confirmed preserved; published-safety
  filter added at SQL + projection layer; `resolve_article` refuses
  unpublished with deterministic reject reason; missing canonical URL
  classified as observable DQ gap; QA report
  `qa-reports/faq-kb-lineage-and-url-audit.md` shipped; 12 focused
  tests.
- L1 separate retrieved / resolved / cited source evidence landed.
  `SourceEvidenceLineage` value object splits the historically-
  overloaded `sourceIds` concept; `CitationExtractor` provides passive
  citation extraction (source_id pattern, URL pattern, conservative
  title match); existing `bot_turns.source_ids` write path preserved
  for back-compat; 7 focused tests confirm retrieved-only does not
  imply cited, resolved tracked separately, citation extractor
  patterns work, missing citation observable not blocking.
- L2 FAQ grounding contract + soft diagnostics landed.
  `docs/current/faq_grounding_contract.md` defines the output class taxonomy,
  the §L1 evidence lineage construction, the §L2 diagnostic state
  table, and the non-blocking guarantee. `FaqOutputClass`,
  `FaqOutputClassifier`, `FaqGroundingDiagnostics` services compute
  the seven soft diagnostic fields per turn. 14 focused tests.
- Diagnostics are durably observable on
  `bot_turns.projected_context.faq_grounding` (Sprint 14.1 closure);
  the in-memory `BotSession` transient slots are mirrored from the
  same computed values for in-process readers / tests. No DB schema
  migration; no broad runtime / prompt / routing scope; no hard
  citation gate; existing §G2 guard preserved verbatim.
- Full server suite 859 / 0 (post Sprint 14.1; was 854 pre-closure);
  named Sprint 14 regression suite 269 / 0; full Python eval 294 / 0;
  `L1:escalation_reason_consistency = 0`;
  `CONTRACT_VIOLATION:active_use_case = 0`; cs014 / cs066 / cs095 /
  cs002 / cs029 / cs176 regressions all green.

Out-of-scope items (broad hard citation gate, new skill runtime
framework, broad S1 rewrite, broad routing taxonomy rewrite,
escalation enum changes, CaseSpec churn, FAQ corpus content rewrite,
judge calibration, eval expansion, broad prompt rewrite, mechanical
risk-keyword escalation) were NOT touched.

## 13. Current-doc maintenance rule

`docs/10-handoff.md`, `docs/diagnostics/codex-findings.md`,
`docs/sprint_objective.md`, and `docs/action_bank.md` are
overwrite-current-state files. Before replacing one of them:

1. archive the previous version under `docs/sprints/` if it
   belongs to a sprint closure, or
   `docs/archive/current-docs/` if it is an ad hoc transition;
2. then overwrite the working file;
3. keep only actionable current state in the working file.

Historical detail belongs in `docs/sprints/`,
`docs/archive/current-docs/`, `eval_interactive/results/`, and
`qa-reports/`.

Sprint 13 archives are at `docs/sprints/sprint-013-*`;
Sprint 14 will archive to `docs/sprints/sprint-014-*` on closure.

## 14. Do not reopen

- broad full review
- broad routing rewrite
- judge calibration implementation
- CaseSpec churn
- anchor / exploration / promotion hard-gate expansion
- smoke 14/14 optimization
- S3 no-prior-search guard unless explicitly selected
- S5 Tier-2 runtime guard unless explicitly selected
- broad TraceViewer redesign
- llm_call_log dashboard / per-tool latency dashboard
- search threshold tuning / answer_miss / faq_miss semantic redesign
- tool-deadline guard / bypass-DISCOVER redesign
- advert-link generator / direct listing URL tool
- full Issue Ledger / `issues[]` / per-issue budgets / all-UC task
  taxonomy / full skill runtime framework / handover payload rewrite
- new escalation reason enum value
- runtime sprint unless a new P0 / P1 runtime blocker is found
- broad system prompt rewrite — narrow risk-signal-handling proposal
  in `docs/runtime_freeze_and_risk_policy.md` §6.2 only on Eval
  Governance trigger
- broad FAQ corpus rewrite — narrow corpus curation for the 38
  `Help_Site_URL__c` gap rows is a queued Eval Governance / corpus
  audit task
- hard runtime citation gate — Sprint 14 §L2 explicitly carved this
  out; only consider after real-traffic evidence escalates the
  `citation_drift` / `retrieved_but_unresolved` signals

## 15. Sprint 14.1 closure (FAQ grounding observability persistence)

Status: **closed (Codex pass)**. The Sprint 14.1 closure review
returned `decision: pass, blocking_count: 0`, accepting Sprint 14 +
14.1 as a unit.

The initial Sprint 14 Codex review had returned `decision:
fix_required, blocking_count: 1` against the Sprint 14 diff. The
single blocker was:

> Sprint 14 L1/L2 lineage + grounding diagnostics are computed but not
> durably observable. They are stamped onto `@Transient` `BotSession`
> fields after `BotTurn` is saved, so a normal save/reload trace cannot
> see `retrieved_source_ids`, `resolved_source_ids`, `cited_source_ids`,
> `faq_grounding_state`, `citation_present`, `citation_match`,
> `citation_drift`, `resolved_but_uncited`, `retrieved_but_unresolved`.

### Closure summary

- **Blocker fixed:** the §L1 `SourceEvidenceLineage`, §L2
  `FaqOutputClass`, and §L2 `FaqGroundingDiagnostics` are now computed
  in `ControlKernel.recordRunResult(...)` BEFORE
  `turnRepository.save(turn)`, and the snake_case payload is merged
  into the existing `bot_turns.projected_context` JSONB column under a
  new top-level `faq_grounding` key.
- **Trace surface used:**
  `bot_turns.projected_context.faq_grounding` (JSONB; no migration —
  the column already exists and is opaque JSON).
- **Fields persisted under `faq_grounding`:**
  - `retrieved_source_ids` (array of source_id strings),
  - `resolved_source_ids` (array of source_id strings),
  - `cited_source_ids` (array of source_id strings),
  - `cited_canonical_urls` (array of free-form URLs that did NOT
    correspond to any candidate `canonical_url`),
  - `output_class` (one of `factual_answer`, `clarification`,
    `empathy_ack`, `handover`, `tool_status`, `intake_collection`, or
    `null` when classification was skipped),
  - `faq_grounding_state` (`factual_grounded` / `factual_uncited` /
    `factual_unresolved` / `factual_unretrieved` / `non_factual` /
    `unknown`),
  - `citation_present`, `citation_match`, `citation_drift`,
  - `resolved_but_uncited`, `retrieved_but_unresolved`.
- **Backwards compatibility:** the existing
  `bot_turns.source_ids` `text[]` column is preserved verbatim; its
  write path in `recordRunResult` is unchanged. The `BotSession`
  `@Transient` slots are still populated (mirroring the durable
  values) so any in-memory reader / projection / test that already
  consumes them keeps working.
- **Out of scope (carved out, per Sprint 14.1 task):**
  - no DB migration (uses the existing JSONB column);
  - no hard citation gate (no rejection / rewrite / re-loop on
    missing citation);
  - no new skill runtime framework;
  - no broad S1 hardening;
  - no FAQ corpus / CaseSpec / judge / prompt / routing /
    escalation-enum / risk-policy edit.

### Files changed (Sprint 14.1)

Production code:

- `server/src/main/java/com/gumtree/csagent/service/runtime/ControlKernel.java`
  — `recordRunResult(...)` reordered to compute lineage / output class
  / diagnostics BEFORE saving the turn; new
  `mergeFaqGroundingIntoProjection(...)`,
  `buildFaqGroundingPayload(...)`, and
  `stampFaqGroundingObservabilityFromComputed(...)` helpers replace
  the old post-save `stampFaqGroundingObservability(...)` call site.

Tests added:

- `server/src/test/java/com/gumtree/csagent/integration/Sprint141FaqGroundingTracePersistenceTest.java`
  (5 cases — retrieved-only, resolved-only, cited-by-source-id,
  cited-by-canonical-url, missing-citation-non-blocking).

Docs:

- `docs/10-handoff.md` (this section + corrections to §1, §7, §8,
  §12 — Sprint 14 was NOT met before Codex review).
- `docs/current/faq_grounding_contract.md` §5 — wiring narrative updated to
  name the durable trace surface
  (`bot_turns.projected_context.faq_grounding`).
- `docs/action_bank.md` — Sprint 14 status reflects the §14.1 closure.

### Tests run

- `mvn -pl server test -Dtest='Sprint14*,Sprint141*'`
  → **38 / 0 / 0 / 0** (33 Sprint 14 + 5 Sprint 14.1 closure tests).
- `mvn -pl server test -Dtest='Sprint10*Test,Sprint11*Test,
  Sprint12*Test,Sprint13*Test,PhaseEvaluatorPlanTest,Cs014*,
  Cs066*,Cs095*,Cs002*,Cs029*,Cs176*,Cs001*,EscalationReason*Test,
  Sprint6*,Sprint7*Test,Sprint8*Test,Sprint9*Test'`
  → **269 / 0 / 0 / 0** (named Sprint regression suite — including
  Sprint 6 §G2 FAQ S1 guard, cs014 / cs066 / cs095 / cs002 / cs029 /
  cs176 regressions).
- `mvn -pl server test`
  → **859 / 0 / 0 / 0** (was 854; +5 new Sprint 14.1 closure tests).
- `pytest eval_interactive/tests/` — not re-run; Sprint 14.1 changes
  no eval-output schema and no Python parsing path.
- Smoke runs not required — Sprint 14.1 changes no FAQ corpus, no
  prompt, no judge, no CaseSpec, no escalation enum, no routing
  taxonomy, no eval-output schema.

### What Sprint 14.1 explicitly does NOT do

- No DB migration. The trace surface is the existing `jsonb`
  `bot_turns.projected_context` column — `faq_grounding` rides as an
  additive top-level key. Old rows simply won't have the key.
- No hard citation gate. Missing citation remains observable
  (`citation_present=false`, optionally `resolved_but_uncited=true` /
  `retrieved_but_unresolved=true`) but is never used to reject,
  rewrite, or re-loop the bot reply.
- No broad S1 hardening. The Sprint 16 §S1 candidates documented in
  `docs/current/faq_grounding_contract.md` §6 (`R-faq-grounded-resolve-bypass`,
  `R-cited-but-unresolved`, `R-resolved-but-uncited-rate`,
  `R-canonical-url-missing-rate`) remain deferred.
- No new skill runtime framework, no new escalation reason value, no
  CaseSpec edits, no FAQ corpus content edit, no judge / prompt /
  routing / risk-policy change.

## 16. Sprint 15 — Script / Policy Config Governance

Status: **accepted / ready to archive**. The Sprint 15 Codex review
returned `decision: pass, blocking_count: 0`, accepting Sprint 15
as inside the config-governance scope (no prompt, routing, judge,
corpus, CaseSpec, hard citation gate, hot reload, dashboard, or
broad runtime work). Sprint 15 is the latest accepted sprint and
introduced no runtime semantic change. Diff stays narrow: three
actions only.

### 16.1 Implemented actions

#### M0 — Externalize DriftDetector / risk keywords to YAML

What landed:

- New config: `server/src/main/resources/config/risk-keywords.yaml`
  (`version: 1`). Carries the exact same five hard-shift groups
  (UC-J / UC-G / UC-I / UC-J / UC-H, in the original declaration
  order) with the exact same keyword strings as the previous
  hardcoded `DriftDetector.HARD_SHIFT_KEYWORDS` list, plus the same
  escalation regex under `escalation-pattern`.
- New loader: `RiskKeywordsConfig` (Spring `@Service`) loads the YAML
  at startup, compiles the escalation pattern, and validates:
  required fields, non-empty groups, non-blank keywords, no
  cross-group keyword duplicates (which would be unreachable under
  first-match-wins), and a parseable regex. Fail-fast on any
  violation.
- `DriftDetector` constructor-injects `RiskKeywordsConfig`. The
  matching contract is unchanged — same lower-case substring check,
  same first-match-wins precedence within and across groups, same
  same-UC suppression, same `DriftResult` outputs.
- A no-arg test convenience constructor on `DriftDetector` loads the
  bundled default classpath resource so legacy unit tests
  (`DriftDetectorTest`) keep working without behavioural change.

Sprint 15 §M0 explicitly does NOT add new risk semantics, new risk
levels, new escalation reasons, or auto-handover for Level 1 / Level
2 risk signals. Future risk-policy reviews are expected to land as
config diff rather than Java source edits.

Tests added:

- `Sprint15RiskKeywordsConfigTest` (11 cases) — config validation
  parity (default loads, group / keyword set parity vs the hardcoded
  reference, version pin), six structural-validation negative tests
  (missing escalation pattern, empty group list, empty keywords,
  blank target UC, duplicate keyword, invalid regex), the full
  parity matrix across escalation phrases / hard-shift groups /
  same-UC suppression / escalation-precedence / no-drift baseline,
  the cross-group first-match-wins precedence assertion, and a
  keyword-uniqueness sanity guard.

#### M1 — Script library version pin and docs ↔ YAML consistency check

What landed:

- `server/src/main/resources/scripts/templates.yaml` now declares
  `library_version: "v1.1"` and `library_version_date: "2026-04-21"`,
  matching the latest entry of the `## 11. Version` table in
  `docs/fixed_script_library_v1.md`.
- `ScriptLibraryService` reads and exposes the version pin
  (`getLibraryVersion()`, `getLibraryVersionDate()`) and fails fast
  at startup if `library_version` is missing.
- New build-time check:
  `Sprint15ScriptLibraryConsistencyTest` parses both surfaces and
  fails the build when:
  - `library_version` / `library_version_date` is absent or blank,
  - the YAML version pin diverges from the latest doc version row,
  - any required template ID listed in the test's reference manifest
    is missing from the YAML,
  - a required template's `variables:` declaration drifts from the
    doc's variable contract,
  - a top-level template id is duplicated.

Sprint 15 §M1 explicitly does NOT rewrite script copy, change tone /
escalation wording, alter forbidden-phrase rules, or introduce a new
template engine. ScriptLibraryService substitution semantics are
unchanged except for additive metadata validation.

Tests added:

- `Sprint15ScriptLibraryConsistencyTest` (5 cases) — version pin
  presence, version + date parity vs the doc's §11 Version table,
  required-template-IDs presence, required-variables contract
  parity for every template that takes parameters, and duplicate
  top-level id detection.

#### M2 — Retrieval / answer gate / rerank fallback thresholds + diagnostics

What landed:

- New `@ConfigurationProperties("knowledge.retrieval")` bean
  `KnowledgeRetrievalProperties` carrying the previously hardcoded
  thresholds with **defaults unchanged**:
  - `ann-limit: 20`
  - `retrieval-gate-threshold: 0.3`
  - `answer-gate-threshold: 3.5`
  - `rerank-candidates: 8`
  - `top-results: 3`
  - `rerank-fallback-score: 2.5`
- Range validation runs in a `@PostConstruct` `validate()` step:
  `ann-limit ≥ rerank-candidates ≥ top-results ≥ 1`, retrieval gate
  ∈ [0.0, 1.0], answer gate ∈ [1.0, 5.0], fallback score ∈ [1.0,
  5.0], and `rerank-fallback-score < answer-gate-threshold` so a
  fallback-score result can never satisfy the answer gate.
- `application.yml` adds an explicit `knowledge.retrieval` block
  whose values match the defaults verbatim — future tuning becomes
  an auditable config diff rather than a hidden Java edit.
- `KnowledgeSearchService` no longer holds `static final`
  thresholds; it reads them per-call from the injected properties
  bean. Pipeline behaviour (ANN limit, retrieval gate test, dedup,
  rerank window, answer gate test, top-results truncation) is
  unchanged.
- `RerankService` now sources the neutral fallback score from
  `KnowledgeRetrievalProperties.rerankFallbackScore()` (default 2.5,
  unchanged) for both the parse-fallback path and the LLM-failure /
  future-failure paths.
- Diagnostics added (observability-only, no behaviour change):
  - `KnowledgeSearchService` logs `retrieval_miss` /
    `answer_miss` with `reason=retrieval_gate` or `reason=answer_gate`
    and the threshold value; on `answer_miss` it also logs
    `top_is_fallback_score=true|false` so a parse-failure /
    call-failure attribution is visible.
  - `RerankService` logs `Rerank parse fallback` with
    `reason=empty_response | unparseable` plus the fallback score
    used, and `Rerank LLM call failed` /
    `Rerank future failed` with the fallback score on the failure
    paths.

Sprint 15 §M2 explicitly does NOT tune thresholds, change FAQ
answerability semantics, change corpus content, or change eval
expected outcomes.

Tests added:

- `Sprint15KnowledgeRetrievalConfigTest` (9 cases) — defaults parity
  (each default pinned by literal value), defaults pass validation,
  and seven structural-validation negative tests (ann-limit < 1,
  ann-limit < rerank-candidates, rerank-candidates < top-results,
  retrieval gate out of range, answer gate out of range,
  fallback ≥ answer gate, fallback out of range).

Updates to existing tests for constructor signature changes:

- `RerankServiceTest` — passes the default
  `KnowledgeRetrievalProperties` instance into the constructor; the
  pre-existing parse-failure / call-failure cases remain green and
  continue to assert the 2.5 fallback score (now sourced from the
  default config).
- `Sprint14KnowledgeSearchPublishedFilterTest` — passes the default
  `KnowledgeRetrievalProperties` instance into the constructor;
  Sprint 14 §L0 published-only filter / projection contract is
  preserved and remains green.

### 16.2 Files changed (Sprint 15)

Production code:

- `server/src/main/java/com/gumtree/csagent/service/runtime/RiskKeywordsConfig.java` (new)
- `server/src/main/java/com/gumtree/csagent/service/runtime/DriftDetector.java`
  (constructor-injects `RiskKeywordsConfig`; matching contract
  unchanged)
- `server/src/main/java/com/gumtree/csagent/service/guardrails/ScriptLibraryService.java`
  (reads `library_version` / `library_version_date`; fail-fast on
  missing pin; substitution semantics unchanged)
- `server/src/main/java/com/gumtree/csagent/config/KnowledgeRetrievalProperties.java` (new)
- `server/src/main/java/com/gumtree/csagent/service/knowledge/KnowledgeSearchService.java`
  (constructor-injects `KnowledgeRetrievalProperties`; reads
  thresholds from config; adds threshold-gate diagnostics; pipeline
  behaviour unchanged)
- `server/src/main/java/com/gumtree/csagent/service/knowledge/RerankService.java`
  (constructor-injects `KnowledgeRetrievalProperties`; reads
  fallback score from config; adds parse / call / future failure
  diagnostics; scoring contract unchanged)

Config / resources:

- `server/src/main/resources/config/risk-keywords.yaml` (new)
- `server/src/main/resources/scripts/templates.yaml`
  (additive `library_version` + `library_version_date` keys; no
  template copy change)
- `server/src/main/resources/application.yml`
  (additive `knowledge.retrieval.*` block matching the previous
  hardcoded defaults verbatim)

Tests (new):

- `Sprint15RiskKeywordsConfigTest` (11 cases — config validation,
  group / keyword parity, full DriftDetector behavioural parity
  matrix, first-match-wins precedence)
- `Sprint15ScriptLibraryConsistencyTest` (5 cases — version pin
  presence, doc-vs-YAML version + date parity, required template
  IDs, required variables contract, duplicate-id guard)
- `Sprint15KnowledgeRetrievalConfigTest` (9 cases — defaults parity,
  validation positive + 7 negatives)

Tests (updated for constructor signature):

- `RerankServiceTest`
- `Sprint14KnowledgeSearchPublishedFilterTest`

Docs:

- `docs/10-handoff.md` (this section + §1 update).
- `docs/action_bank.md` (Sprint 15 deliverables added).
- `docs/sprint_objective.md` retained — already contains the
  Sprint 15 objective.
- No edit to `docs/current_eval_baseline.md` — Sprint 15 changes no
  eval expected outcome; baseline remains the post-Sprint-8 r1 / r2
  runs.

No FAQ corpus content, CaseSpec, judge, broad routing taxonomy,
handover payload contract, Salesforce backend contract, full Issue
Ledger, all-UC task taxonomy, escalation enum, system prompt, DB
schema migration, runtime main-flow architecture file, or hard
citation gate was touched.

### 16.3 Tests run

- `mvn -pl server test -Dtest='Sprint15*,DriftDetectorTest,RerankServiceTest'`
  → **55 / 0 / 0 / 0** (25 Sprint 15 §M0 / §M1 / §M2 focused tests +
  18 legacy `DriftDetectorTest` cases + 12 `RerankServiceTest`
  cases, confirming parity under the new wiring).
- `mvn -pl server test -Dtest='Sprint10*Test,Sprint11*Test,
  Sprint12*Test,Sprint13*Test,PhaseEvaluatorPlanTest,Cs014*,
  Cs066*,Cs095*,Cs002*,Cs029*,Cs176*,Cs001*,EscalationReason*Test,
  Sprint6*,Sprint7*Test,Sprint8*Test,Sprint9*Test,Sprint14*,
  Sprint141*,Sprint15*'`
  → **332 / 0 / 0 / 0** (extended named regression suite — including
  Sprint 6 §G2 FAQ S1 guard, cs014 / cs066 / cs095 / cs002 / cs029 /
  cs176 regressions, all Sprint 14 §L0 / §L1 / §L2 + §14.1 trace
  persistence, plus the new Sprint 15 §M0 / §M1 / §M2 focused
  suite).
- `mvn -pl server test`
  → **884 / 0 / 0 / 0** (was 859 pre-Sprint-15; +25 new Sprint 15
  §M0 / §M1 / §M2 tests).
- `pytest eval_interactive/tests/` — not re-run; Sprint 15 changes
  no Python parsing path and no eval-output schema.
- Smoke runs not required — Sprint 15 changes no FAQ corpus, no
  prompt, no judge, no CaseSpec, no escalation enum, no routing
  taxonomy, no eval-output schema. The current canonical baseline
  (`docs/current_eval_baseline.md`) is preserved (post-Sprint-8 r1 /
  r2 runs).

### 16.4 Behaviour-preservation evidence

- §M0 — `Sprint15RiskKeywordsConfigTest.detect_parityMatrix_matchesHardcodedReference`
  exercises every escalation phrase, every hard-shift keyword group,
  the same-UC suppression rule, the escalation-precedence rule, and
  the no-drift baseline against the previously hardcoded
  expectations. The legacy `DriftDetectorTest` (18 cases) keeps
  passing under the new YAML-loaded path. The
  cross-group-first-match-wins assertion locks the precedence
  ordering. No DriftResult shape, type, or field has changed.
- §M1 — `ScriptLibraryService.getTemplate(...)` /
  `renderTemplate(...)` substitution semantics are unchanged. The
  approved template copy (50+ templates / 14 categories) is
  bit-for-bit identical to v1.1 of the doc; the consistency test
  enforces no silent copy drift in the variables contract.
- §M2 — `Sprint15KnowledgeRetrievalConfigTest.defaults_matchPreviouslyHardcodedConstants`
  pins each default to the literal value of the prior `static
  final` constant. `Sprint14KnowledgeSearchPublishedFilterTest`
  (Sprint 14 §L0 contract) and `RerankServiceTest` (parse / call
  failure → 2.5 fallback) keep passing under the new wiring. The
  full retrieval pipeline (ANN limit, retrieval gate test, dedup,
  rerank window, answer gate test, top-results truncation) is
  observable-equivalent under default config; the validator
  forbids any threshold combination that would change semantic
  ordering.

### 16.5 Config files added / changed

- `server/src/main/resources/config/risk-keywords.yaml` (added) —
  source of truth for `DriftDetector` escalation pattern + hard-shift
  groups.
- `server/src/main/resources/scripts/templates.yaml` (changed —
  additive `library_version`, `library_version_date` only).
- `server/src/main/resources/application.yml` (changed — additive
  `knowledge.retrieval.*` block; defaults match prior hardcoded
  constants verbatim).

### 16.6 Regression guard outcomes

All Sprint 15 active guards pass:

- `L1:escalation_reason_consistency` = **0** (never re-introduced).
- `CONTRACT_VIOLATION:active_use_case` = **0**.
- Sprint 14 `bot_turns.projected_context.faq_grounding` persistence
  trace remains intact (`Sprint141FaqGroundingTracePersistenceTest`,
  5 cases).
- Sprint 14 published-only KB search remains intact
  (`Sprint14KnowledgeSearchPublishedFilterTest`, 4 cases).
- Existing FAQ S1 guard remains green
  (`AgentRunLoopS1FaqGroundedResolveGuardTest`).
- Sprint 6 §G0 ReadTimeout no-retry closure intact.
- cs014 remains UC-C
  (`Cs014RouteAndLoopHandoverIntegrationTest`,
  `Cs014RouteAndDistressRegressionTest`).
- cs066 remains UC-K.
- cs095 remains UC-A / not UC-K / not UC-FP.
- cs002 distress reconciliation remains green.
- cs029 remains UC-D + `user_requested`.
- cs176 explicit-human-help → `user_requested` regression remains
  green.
- All Sprint 7 / 7.1 / 8 / 8.1 / 8.2 / 9 / 9.1 / 10 / 11 / 11.1 /
  12 / 13 hard invariants remain green
  (named regression suite: 332 / 0).
- `RuntimeIntentClassifier` remains runtime-internal.
- `bot_turns.source_ids` write path unchanged.
- `bot_turns` / `bot_sessions` / `kb_articles` / `kb_chunks`
  schemas unchanged.

### 16.7 Remaining risks

**No new P0 / P1 blockers opened by Sprint 15.** The change is
config-governance + diagnostics; no runtime main-flow architecture
file gained new semantics, no DriftResult / KnowledgeSearchResult /
ScoredCandidate shape changed.

Carry-over notes:

- The `library_version` pin must be updated whenever
  `docs/fixed_script_library_v1.md` ships a new approved version
  row — `Sprint15ScriptLibraryConsistencyTest` will fail the build
  if the two surfaces diverge. This is the intended invariant.
- Future threshold tuning must land as a `knowledge.retrieval.*`
  config diff. The validator constraints
  (`fallback < answer-gate`, `ann ≥ rerank ≥ top`) are the floor,
  not a tuned recommendation.
- `RiskKeywordsConfig` is loaded once at startup. Hot-reload remains
  out of scope; Sprint 15 explicitly does NOT introduce ops-owned
  runtime config. A risk-keyword change still requires a redeploy.

Residuals carried forward from Sprint 14 §10 remain unchanged
(`R-faq-grounded-resolve-bypass`, `R-cited-but-unresolved`,
`R-resolved-but-uncited-rate`, `R-canonical-url-missing-rate`, plus
the older Sprint 13 §8 list).

### 16.8 Recommended next phase

The Sprint 15 closure surfaces no runtime regression. The previously
listed alternatives remain viable:

1. **Eval Governance docs sprint** — primary recommendation. The
   largest residual category is still `judge_volatility` /
   `faq_corpus_gap` / `product_policy_gap`, all governance-owned.
2. **Narrow Sprint 16 §S1 hardening** — only if real-traffic
   evidence demonstrates `R-faq-grounded-resolve-bypass`
   reproducibly affects answer correctness on a high-traffic UC.
3. **Narrow Corpus Curation** — fill the 38 missing
   `Help_Site_URL__c` rows. Owner: Eval Governance / corpus audit.
4. **Re-run validation after infra cleanup** — viable if Kimi /
   smoke credentials are available; runs alongside Eval Governance.

Do not start another runtime sprint unless triage finds a new
P0 / P1 runtime blocker. Sprint 15 explicitly does NOT promote a
new canonical eval baseline.

### 16.9 What Sprint 15 explicitly does NOT do

- No new risk semantics, no new escalation reasons, no new risk
  levels, no auto-handover for Level 1 / Level 2 risk signals.
- No new hard Java guard.
- No prompt rewrite, no system prompt edit, no broad routing
  rewrite.
- No hard runtime citation gate.
- No FAQ corpus rewrite.
- No judge calibration, no CaseSpec changes, no eval expansion.
- No anchor / exploration / promotion hard-gate expansion.
- No hot reload / ops-owned runtime config.
- No dashboard work, no full TraceViewer redesign.
- No new skill runtime framework.
- No broad S1 rewrite.
- No DB migration. No schema change. No bot_turns / bot_sessions /
  kb_articles / kb_chunks edit.
- No threshold tuning. Defaults are pinned to the previous
  hardcoded values verbatim.

## 17. Sprint 16 — Handover Exactly-Once Contract and Repro

Status: **landed (docs + characterization-test sprint, no runtime
change)**. Sprint 16 freezes the handover exactly-once contract and
adds the characterization tests that demonstrate the current
LLM-driven dual local-persistence shape, without modifying any
runtime behaviour. It does not yet implement
`HandoverOrchestrator`; that is the explicitly deferred future
runtime sprint trigger.

### 17.1 Exact docs / tests changed

Docs (new + edited):

- `docs/proposals/handover_orchestrator_design.md` (new) — defines the
  exactly-once contract. §1 names the current dual-path shape
  (writer #1 = `RequestHandoverTool` →
  `SalesforceService.requestHandover` →
  `MockSalesforceService` → `mock_handover_log` row #1; writer #2
  = `SessionManager.recordHandover` → direct
  `handoverLogRepository.save(...)` → `mock_handover_log` row
  #2). §2 distinguishes confirmed local persistence duplication
  from unproven real Salesforce double transfer. §3 freezes three
  contracts: trace evidence (`request_handover`), outcome
  persistence (`record_outcome`), and the future
  `HandoverOrchestrator` handover side-effect (idempotent by
  `session_id`). §4 sketches the future orchestrator shape. §5
  records why Sprint 16 is docs + characterization only. §6
  pre-specifies acceptance criteria for the future runtime sprint.
- `docs/runtime_freeze_and_risk_policy.md` (edited) — added §10
  "Known unspecced surfaces" with §10.1 entry covering the
  handover dual-path. Renumbered References to §11; added
  cross-reference links to the Sprint 16 design / release_gate /
  source files.
- `docs/release_gate.md` (new) — opens the release-gate ledger.
  §1.1 blocking rule: before real Salesforce cutover, the
  handover side-effect must be idempotent by `session_id`. The
  rule is **not satisfied** at Sprint 16 close.
- `docs/action_bank.md` (edited) — Sprint 16 row added to §3
  active deliverables (H0 / H1 / H2); new
  D-single-handover-orchestrator entry in §4 deferred runtime
  candidates ("Single Handover Orchestrator: exactly-once
  side-effect owner before Salesforce production cutover");
  Sprint 16 row added to §6 closed-action index.
- `docs/sprint_objective.md` retained — already contains the
  Sprint 16 objective.
- `docs/10-handoff.md` (this file) — §1 and new §17 (this
  section).

Tests (new):

- `server/src/test/java/com/gumtree/csagent/service/runtime/Sprint16HandoverDualPathReproTest.java`
  (3 cases): two passing characterization cases plus one
  `@Disabled` future-invariant case.
  - `dualLocalPersistence_llmDrivenRequestHandoverPath_producesTwoMockHandoverLogWrites_currentBehaviour`
    (passing) — exercises `RequestHandoverTool.execute(...)` with
    a real `MockSalesforceService` backed by a mocked
    `MockHandoverLogRepository`, then replays the
    `SessionManager.recordHandover` surface (real
    `HandoverPayloadAssembler` → direct `repo.save(...)`).
    Asserts two saves on the same repository for the same
    `session_id`; asserts the two payloads carry distinct
    `version` (`"1.0"` vs `"1.1"`) and distinct `transfer_result`
    (`MockSalesforceService` set vs `mock_transfer` literal).
  - `distinguishesLocalPersistenceDuplication_fromUnprovenRealSalesforceDoubleTransfer`
    (passing) — pins that today the only `SalesforceService`
    implementation is `MockSalesforceService`. Designed to break
    on first wire-up of a real Salesforce client so the reviewer
    is forced to re-read the §10.1 contract before passing the
    release gate.
  - `futureInvariant_atMostOneTransmittedHandoverDecisionPerSessionId_disabledUntilOrchestratorLands`
    (`@Disabled`, TODO) — encodes the future invariant: at most
    one transmitted / `offline_logged` handover decision per
    `session_id`. Would fail today by design (the LLM-driven
    path writes twice). Removing the `@Disabled` annotation is
    listed as one of the acceptance criteria for the future
    "Single Handover Orchestrator" runtime sprint
    (`docs/release_gate.md` §1.1 #4-#5).

Source comments (new, no behaviour change):

- `server/src/main/java/com/gumtree/csagent/service/tools/RequestHandoverTool.java`
  — class-level Javadoc carries a Sprint 16 §H0
  known-unspecced-surface marker pointing to
  `docs/proposals/handover_orchestrator_design.md` and the future-invariant
  test.
- `server/src/main/java/com/gumtree/csagent/service/runtime/SessionManager.java`
  — `recordHandover(...)` carries the matching marker on its
  Javadoc.

### 17.2 Current confirmed risk

- **Duplicated local persistence (P2 today, mock / local).** On
  the LLM-driven `request_handover` path two
  `mock_handover_log` rows are written for the same `session_id`
  on the same ESCALATE turn. The two writers are unaware of each
  other and persist payloads with different `version` and
  `transfer_result` shapes.
- **P1 launch-readiness severity.** The dual-path shape is one
  wire-up away from a real double transfer at the moment a
  production `SalesforceService` implementation lands.
- **P0 / P1 production-incident severity** if a real double
  transfer occurs after cutover (case re-routed twice in
  Salesforce: re-assignment, mis-prioritisation, queue
  duplication; user-visible).
- The hard-OOS path (`SessionManager.createSession` → synthetic
  `request_handover` evidence + single `recordHandover`) and the
  kernel force-escalate / Step 2.5 path remain single-write today
  (pinned by Sprint 16 §H0 contract; not opened or weakened by
  Sprint 16).

### 17.3 What is not confirmed

- A real **double Salesforce transfer** is not currently proven.
  The production Salesforce client is not wired; only
  `MockSalesforceService` (`@Profile("local")`) implements
  `SalesforceService`, and `SessionManager.recordHandover` does
  not flow through `SalesforceService` at all (it writes directly
  to `mock_handover_log`).
- `HandoverPayloadAssembler` is the candidate single payload
  builder for the future orchestrator. Sprint 16 does not commit
  to it as the final shape; the orchestrator design doc names it
  as the candidate but leaves payload-schema decisions to the
  runtime sprint.
- No production case routing is currently affected. The
  characterization test runs against an in-memory mocked
  repository.

### 17.4 Future runtime sprint trigger

A future "Single Handover Orchestrator" runtime sprint must start
when either:

1. a real Salesforce client is staged for cutover (the launch-time
   wire-up forces the orchestrator to land first to satisfy
   `docs/release_gate.md` §1.1), or
2. a real-traffic case shows duplicated handover routing in
   Salesforce (would be the first P0 / P1 confirmation that
   moves the issue out of P2 territory).

Until then, `Sprint16HandoverDualPathReproTest` is the standing
guard that the dual-path shape has not been silently fixed (the
two passing cases would break) and that the future invariant has
not been silently re-introduced as a hard gate (the `@Disabled`
case would need to be re-enabled deliberately).

The runtime sprint's acceptance criteria are pre-specified in
`docs/proposals/handover_orchestrator_design.md` §6 and
`docs/release_gate.md` §1.1.

### 17.5 Tests run

- `mvn -pl server test
  -Dtest='Sprint16*,RequestHandoverToolTest,HandoverPayloadAssemblerTest,Cs014RouteAndLoopHandoverIntegrationTest,Cs176ExplicitHumanHelpHandoverIntegrationTest,AgentRunLoopHandoverReasonNormalizationIntegrationTest'`
  → **22 / 0 / 0 / 1** (Sprint 16 §H1 focused suite + handover
  regression suite; the 1 skipped is the `@Disabled`
  future-invariant case).
- `mvn -pl server test`
  → **887 / 0 / 0 / 1** (was 884 pre-Sprint-16; +2 new passing
  Sprint 16 tests + 1 new disabled future-invariant test). No
  regression in any Sprint 6 / 7 / 7.1 / 8 / 8.2 / 9 / 9.1 / 10
  / 11 / 11.1 / 12 / 13 / 14 / 14.1 / 15 invariant. The cs014 /
  cs066 / cs095 / cs002 / cs029 / cs176 / cs001 regression suite
  remains green. `L1:escalation_reason_consistency = 0` and
  `CONTRACT_VIOLATION:active_use_case = 0` are preserved.
- `pytest eval_interactive/tests/` — not re-run; Sprint 16
  changes no Python file, no eval-output schema, no judge, no
  CaseSpec, no FAQ corpus. The Sprint 16 diff is two Markdown
  files (new), three Markdown files (edited), one new Java test,
  and two Javadoc-only edits on existing Java source.
- Smoke runs not required — Sprint 16 changes no FAQ corpus, no
  prompt, no judge, no CaseSpec, no escalation enum, no routing
  taxonomy, no eval-output schema, no DB schema. The current
  canonical baseline (`docs/current_eval_baseline.md`) is
  preserved (post-Sprint-8 r1 / r2 runs).

### 17.6 Next recommended action

Recommended next phase: **defer**. Sprint 16 is intentionally a
docs + characterization sprint; the runtime fix
("Single Handover Orchestrator") is queued as a deferred runtime
candidate (`docs/action_bank.md` §4 row
`D-single-handover-orchestrator`) and gated by the real-Salesforce
cutover plan (`docs/release_gate.md` §1.1).

In priority order:

1. **Eval Governance Follow-up** — primary recommendation
   carried over from Sprint 15. Largest residual category is
   still `judge_volatility` / `faq_corpus_gap` /
   `product_policy_gap`.
2. **Single Handover Orchestrator runtime sprint** — start ONLY
   when (a) a real Salesforce client is staged for cutover, or
   (b) a real-traffic case shows duplicated handover routing.
   Acceptance criteria pre-specified in
   `docs/proposals/handover_orchestrator_design.md` §6 and
   `docs/release_gate.md` §1.1; the `@Disabled` future-invariant
   test is the closing artifact.
3. **Narrow Sprint 16 §S1 hardening** (FAQ-grounded resolve
   bypass) — only on real-traffic evidence per
   `docs/current/faq_grounding_contract.md` §6.
4. **Narrow Corpus Curation** — fill the 38 missing
   `Help_Site_URL__c` rows.

Do not start the Single Handover Orchestrator runtime sprint
opportunistically. Sprint 13's runtime freeze remains in force; a
new runtime sprint requires a P0 / P1 trigger or the real
Salesforce cutover plan.

### 17.7 What Sprint 16 explicitly does NOT do

- No `HandoverOrchestrator` implementation. Sprint 16 is design
  + characterization only; the runtime fix is the future
  "Single Handover Orchestrator" sprint.
- No production Salesforce client. The release-gate rule
  (`docs/release_gate.md` §1.1) blocks that wire-up until the
  orchestrator lands.
- No change to `RequestHandoverTool` behaviour. Class-level
  Javadoc adds a §H0 marker only.
- No change to `SessionManager.recordHandover` behaviour. Method
  Javadoc adds a §H0 marker only.
- No change to `MockSalesforceService` behaviour or the
  `SalesforceService` interface.
- No change to the Phase 3 §3.6.2 handover payload schema.
- No change to `HandoverPayloadAssembler` behaviour.
- No prompt edit. No `system_prompt.txt` change.
- No routing change. No new escalation reason. No CaseSpec
  change. No FAQ corpus change. No judge calibration change. No
  eval expected-outcome change.
- No DB migration. No schema change. `mock_handover_log` /
  `bot_turns` / `bot_sessions` / `kb_articles` / `kb_chunks`
  schemas unchanged.
- No new live smoke baseline. No anchor / exploration /
  promotion hard-gate change.
