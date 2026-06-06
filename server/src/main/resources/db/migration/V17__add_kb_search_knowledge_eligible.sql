-- R6 (Sub-sprint C-2b) — corpus-curation flag for the LLM-facing
-- search_knowledge surface. NOT NULL DEFAULT TRUE backfills every
-- existing row to visible; the JSON re-ingestion flips the two
-- CS-only placeholder template articles to FALSE. Direct resolve_article
-- by id is unaffected (no filter at the resolve surface).
ALTER TABLE kb_articles
    ADD COLUMN search_knowledge_eligible BOOLEAN NOT NULL DEFAULT TRUE;
