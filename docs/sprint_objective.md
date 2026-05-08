# Sprint Objective

Date: 2026-05-09

## Sprint name

FAQ / KB Evidence Lineage and Safety Sprint 14

## Goal

Upgrade FAQ / KB grounding trustworthiness by making the knowledge source chain, published safety, canonical URL availability, and citation observability explicit.

Sprint 14 must improve factual evidence lineage without introducing a broad hard citation gate, a new skill runtime framework, broad routing rewrites, or additional mechanical escalation behaviour.

## Background

Recent FAQ / KB / scripts reviews found:

- FAQ source chain is mostly grounded, but Help_Site_URL / canonical URL and published safety need implementation audit and possible repair.
- Current trace evidence can conflate retrieved sources with resolved sources and actually cited sources.
- Citation should not become a broad runtime hard gate yet; first add passive observability and diagnostics.
- Fixed scripts are approved expression / tone ground truth, not factual ground truth.
- Hardcoded guardrails should not expand without clear Tier-0 invariant justification.

## Implement exactly these 3 actions

### L0. KB canonical URL / Help URL / published safety audit + fix

Audit and repair the FAQ / KB data path so customer-showable articles have trustworthy URL and published-state metadata.

Required behaviour:

- Trace the source path:
  - `docs/FAQ-knowledge_include_help_url.csv`
  - `scripts/build_knowledge_base.py`
  - `data/knowledge/knowledge_base_articles.json`
  - `KnowledgeIngestionRunner`
  - `kb_articles` / `kb_chunks`
  - `KnowledgeSearchService`
  - `SearchKnowledgeTool`
  - `ResolveArticleTool`
- Confirm whether `Help_Site_URL__c` is currently preserved or dropped.
- If dropped, add the smallest ingestion/schema/tool-contract change needed to preserve a user-clickable help URL as `canonical_url` or `help_url`.
- Ensure search only returns published / safe-to-show article candidates.
- Ensure `resolve_article` refuses or marks unsafe any article that is:
  - unpublished
  - missing required customer-safe body
  - missing canonical URL, unless explicitly classified as URL-missing data quality gap
- Add a data quality report listing:
  - total articles
  - published articles
  - articles with canonical URL
  - articles missing canonical URL
  - articles unsafe to show
  - UC mapping coverage
- Do not change FAQ content semantics.
- Do not edit CaseSpecs to hide data gaps.

Deliverables:

- implementation fix if needed
- focused tests
- `qa-reports/faq-kb-lineage-and-url-audit.md`

### L1. Separate retrieved / resolved / cited source evidence

Make source evidence explicit rather than overloading a single `sourceIds` concept.

Required behaviour:

- Distinguish at trace / BotTurn / event level:
  - `retrieved_source_ids`: article IDs returned by `search_knowledge`
  - `resolved_source_ids`: article IDs successfully passed through `resolve_article`
  - `cited_source_ids`: article IDs or canonical URLs actually referenced in the final user-visible message
- Add a passive citation extractor:
  - parse article IDs if present
  - parse canonical URLs if present
  - optionally match known article URL/title patterns
- Do not reject or rewrite the user message based only on missing citation.
- Do not introduce a hard runtime citation gate.
- Preserve existing eval compatibility as much as possible.
- Add tests proving:
  - retrieved-only evidence does not imply cited evidence
  - resolved evidence is tracked separately
  - cited evidence can be detected from source_id or URL
  - missing citation is observable, not blocking

Deliverables:

- code and tests
- trace/event contract note in `docs/faq_grounding_contract.md`

### L2. FAQ grounding contract + soft diagnostics

Define the factual-answer grounding contract and add soft diagnostics for violations.

Required behaviour:

- Create `docs/faq_grounding_contract.md`.
- Classify bot outputs into:
  - `factual_answer`
  - `clarification`
  - `empathy_ack`
  - `handover`
  - `tool_status`
  - `intake_collection`
- Only `factual_answer` requires grounding evidence.
- Clarification, empathy, intake, and handover must not require citation.
- Add soft diagnostic fields or events:
  - `faq_grounding_state`
  - `citation_present`
  - `citation_match`
  - `citation_drift`
  - `resolved_but_uncited`
  - `retrieved_but_unresolved`
- Diagnostics must not block the response in Sprint 14.
- If the audit discovers a current factual-answer bypass of search / resolve, document it as a candidate for Sprint 16 narrow S1 hardening rather than fixing it broadly in Sprint 14.

Deliverables:

- `docs/faq_grounding_contract.md`
- updated handoff with observed risks
- focused tests for diagnostics, if code is added

## Regression guards

- `L1:escalation_reason_consistency` remains 0.
- `CONTRACT_VIOLATION:active_use_case` remains 0.
- Existing FAQ S1 guard tests remain green.
- Sprint 6 G0 ReadTimeout closure remains intact:
  - 120s create-session timeout widen only
  - no ReadTimeout retry
- cs014 remains UC-C.
- cs066 remains UC-K.
- cs095 remains not UC-K and not incorrectly UC-FP.
- cs002 distress reconciliation remains green.
- cs029 remains UC-D + `user_requested`.
- cs176 explicit-human-help → `user_requested` focused regression remains green.

## Do not implement

- Broad hard citation runtime gate
- New skill runtime framework
- Broad S1 rewrite
- Broad Java guard
- Broad routing taxonomy rewrite
- Escalation enum changes
- CaseSpec churn
- FAQ content rewrite
- Judge calibration
- Anchor / exploration / promotion hard-gate expansion
- Risk keyword semantic changes
- Mechanical escalation on refund / scam / delete / angry keywords
- Prompt rewrite unrelated to FAQ grounding observability

## Success metrics

Primary:

- Help URL / canonical URL / published safety state is audited and, where narrowly possible, repaired.
- Trace distinguishes retrieved vs resolved vs cited evidence.
- Missing citation is observable without becoming a hard refusal gate.
- FAQ factual-answer grounding contract is documented.
- No broad runtime / prompt / routing scope is introduced.

Secondary:

- Smoke pass rate may remain flat.
- Remaining FAQ failures should be easier to classify as:
  - FAQ corpus gap
  - URL data quality gap
  - citation observability gap
  - factual-answer bypass requiring future narrow S1 hardening
  - judge volatility

## Review rule

Codex must review only whether L0 / L1 / L2 were implemented and whether Sprint 14 stayed focused on FAQ / KB evidence lineage and safety.

Codex should not request broad hard citation gating, new skill runtime framework, risk-policy rewrite, judge calibration, broad eval expansion, or CaseSpec churn unless Sprint 14 directly introduces a P0/P1 regression.