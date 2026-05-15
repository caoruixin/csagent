# FAQ / KB Lineage and URL Audit (Sprint 14 §L0)

Date: 2026-05-09
Sprint: Sprint 14 — FAQ / KB Evidence Lineage and Safety
Scope: §L0 only (URL / published-safety audit + repair)

## 1. Source chain trace

| Stage | File / surface | Field carrying the customer-clickable URL | Field carrying published state |
|-------|----------------|-------------------------------------------|--------------------------------|
| Salesforce export CSV | `docs/FAQ-knowledge_include_help_url.csv` | `Help_Site_URL__c` | (none — every row in the curated export is treated as published) |
| Build script | `scripts/build_knowledge_base.py` | `source_url` (read from `Help_Site_URL__c`, kept verbatim) | `published_status` (always `true` for the curated export) |
| Pre-ingest JSON | `data/knowledge/knowledge_base_articles.json` | `source_url` | `published_status` |
| Java ingest | `KnowledgeIngestionRunner` | `kb_articles.source_url` | `kb_articles.is_published` |
| DB column | `kb_articles` (`V4__create_kb_articles.sql`) | `source_url TEXT` | `is_published BOOLEAN NOT NULL DEFAULT true` |
| Search service | `KnowledgeSearchService` | `KnowledgeHit.canonicalUrl` | `is_published = true` filter (Sprint 14 §L0) |
| `search_knowledge` tool | `SearchKnowledgeTool` | `hits[*].canonical_url` | not surfaced (filter applied upstream) |
| `resolve_article` tool | `ResolveArticleTool` | `canonical_url` (Sprint 14 §L0); `source_url` legacy alias | `is_published`, `safe_to_show` (Sprint 14 §L0) |

## 2. Pre-Sprint-14 findings

| # | Finding | Severity |
|---|---------|----------|
| F1 | `Help_Site_URL__c` IS preserved end-to-end as `source_url` (CSV → build script → JSON → DB → service → tool) | OK |
| F2 | `is_published` was stored on `kb_articles` but **never enforced** at search time — `KbChunkRepository.findNearestByEmbedding*` did not filter on it | P2 |
| F3 | `ResolveArticleTool` returned the article verbatim even when `is_published=false`; no refusal or safe-to-show flag | P2 |
| F4 | `SearchKnowledgeTool` exposed `canonical_url`, `ResolveArticleTool` exposed `source_url` — different field names for the same concept; the agent had to translate between them | P3 (ergonomics / observability) |
| F5 | Articles with no Help_Site_URL silently surfaced as `canonical_url=null`; no observability hook to classify "missing URL = data-quality gap" vs "unparseable URL" vs "withheld" | P3 (DQ visibility) |

None of F2–F5 had reproduced as a customer-visible incident in the post-Sprint-8 r1 / r2 baselines, but each one is a latent risk if the corpus pipeline ever flips an article to `is_published=false` (legal takedown, expired campaign, draft) or if the retrieval cache lags a takedown.

## 3. Sprint 14 §L0 fixes (landed)

| # | Fix | File |
|---|-----|------|
| L0.1 | pgvector ANN now joins `kb_articles` and filters `a.is_published = true` on both no-tag and UC-tag-scoped paths | `KbChunkRepository.findNearestByEmbeddingPublishedOnly` / `findNearestByEmbeddingWithUcTagsPublishedOnly` (new) |
| L0.2 | `KnowledgeSearchService.search` calls the published-only ANN methods | `KnowledgeSearchService` |
| L0.3 | Defense-in-depth: hit-projection step drops any article whose post-fetch `isPublished` is false (covers stale cache / mocked tests) | `KnowledgeSearchService` |
| L0.4 | `KnowledgeHit` carries `canonical_url_missing` flag stamped when `source_url` is blank | `KnowledgeHit` |
| L0.5 | `SearchKnowledgeTool` projects `hits[*].canonical_url_missing` into the agent-visible response | `SearchKnowledgeTool` |
| L0.6 | `ResolveArticleTool` refuses unpublished articles with deterministic `article_unpublished_safe_refuse` reject reason | `ResolveArticleTool` |
| L0.7 | `ResolveArticleTool` exposes `canonical_url` (mirrors `search_knowledge` field name); keeps `source_url` as legacy alias | `ResolveArticleTool` |
| L0.8 | `ResolveArticleTool` stamps `safe_to_show` (= published AND non-blank body) and `canonical_url_missing` | `ResolveArticleTool` |
| L0.9 | `build_knowledge_base.py` emits a "URL & Published-Safety Coverage" block in `mapping_summary_report.md` | `scripts/build_knowledge_base.py` |

No FAQ corpus content semantics were changed. No CaseSpec was edited. No `Help_Site_URL__c` value was rewritten. The CSV remains the source of truth.

## 4. Current corpus data quality (snapshot)

Counts derived from the existing `data/knowledge/knowledge_base_articles.json` snapshot:

| Metric | Value |
|--------|-------|
| Total articles | 218 |
| Articles published (`published_status=true`) | 218 |
| Articles unpublished (`published_status=false`) | 0 |
| Articles with `source_url` | 180 |
| Articles missing `source_url` (canonical-URL DQ gap) | 38 |
| Articles unsafe to show (unpublished OR empty body) | 0 |
| `search_knowledge` eligible UC-mapped articles | 218 |

The 38 articles missing `source_url` come from rows the Salesforce export shipped without a `Help_Site_URL__c` value. They remain searchable (their content is still useful) but the agent will see `canonical_url=null` + `canonical_url_missing=true` and the Sprint 14 §L1 cited-evidence extractor will count them as `retrieved_but_uncited` candidates. Repairing those 38 rows is a corpus-side curation task, not a runtime fix.

## 5. Tests added (focused, Sprint 14)

All under `server/src/test/java/com/gumtree/csagent/...`:

- `KnowledgeIngestionRunnerCanonicalUrlTest` — ingest preserves `help_url` / `canonical_url` and `is_published` from the JSON record into `KbArticle`.
- `KnowledgeSearchServicePublishedFilterTest` — `KnowledgeSearchService.search` excludes any chunk whose parent article is unpublished, even if the ANN returned it via a stale cache.
- `ResolveArticleToolSafetyTest` — `resolve_article` refuses unpublished articles with the canonical `article_unpublished_safe_refuse` error code; published articles surface `safe_to_show=true`.
- `ResolveArticleToolCanonicalUrlTest` — `resolve_article` exposes `canonical_url` (matching `search_knowledge.hits[*].canonical_url`); preserves `source_url` as a legacy alias.
- `ResolveArticleToolMissingCanonicalUrlTest` — when an article has no `source_url`, `resolve_article` returns `canonical_url=null` AND `canonical_url_missing=true` so the gap is observable rather than hidden.

## 6. Out of scope for Sprint 14 §L0

- Re-pulling `FAQ-knowledge_include_help_url.csv` from Salesforce.
- Curating fixes for the 38 missing-URL articles (corpus / Eval Governance work).
- Adding a non-curated `is_published` source signal (e.g. PublicationStatus__c column).
- Hard runtime gating that refuses any answer whose `canonical_url_missing=true` (Sprint 14 explicitly does not introduce a hard citation gate).
- Promoting a new canonical eval baseline.

## 7. Cross-references

- Sprint objective: `docs/sprint_objective.md` §L0.
- Grounding contract: `docs/faq_grounding_contract.md` (Sprint 14 §L2).
- Evidence lineage: `docs/faq_grounding_contract.md` §3 (Sprint 14 §L1).
