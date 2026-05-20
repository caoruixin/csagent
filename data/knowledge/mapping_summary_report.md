# Knowledge Base — Article → UC Mapping Report

**Generated**: auto (build_knowledge_base.py)
**Source**: `docs/FAQ-knowledge_include_help_url.csv`

## Overview

| Metric | Value |
|--------|-------|
| Total articles | 218 |
| search_knowledge eligible | 218 |
| Intake-only UC articles | 0 |
| Unmatched | 0 |

## Mapping Method Distribution

| Method | Count |
|--------|-------|
| url_category | 177 |
| title_keyword | 37 |
| content_keyword | 4 |

## Per-UC Article Count

| UC | Name | Article Count | search_knowledge |
|-----|------|--------------|------------------|
| UC-A | Ad Status & Visibility | 8 | Yes |
| UC-B | Posting & Editing Guidance | 101 | Yes |
| UC-C | Messages & Replies | 13 | Yes |
| UC-D | Account & Login | 16 | Yes |
| UC-E | General Product & Search | 77 | Yes |
| UC-F | Payment Inquiry | 18 | Yes |
| UC-FP | Correct Deletion / Compliance | 60 | Yes |
| UC-G | GDPR / Data Deletion | 2 | No (fixed scripts) |
| UC-H | Incorrect Deletion Appeal | 0 | No (fixed scripts) |
| UC-I | Refund / Payment Dispute | 0 | No (fixed scripts) |
| UC-J | Trust & Safety / Fraud Report | 0 | No (fixed scripts) |
| UC-K | Technical Issue | 10 | No (fixed scripts) |

## Token Budget Summary

| Metric | Value |
|--------|-------|
| Total tokens (all articles) | ~105,085 |
| Avg tokens/article | ~482 |
| Max tokens (single article) | ~5,707 |
| Min tokens (single article) | ~23 |
| Articles > 512 tokens (need chunking) | 73 |
| Articles <= 512 tokens (single chunk) | 145 |

## Phase 3 Readiness Checklist

- [x] Article-level knowledge base JSON generated
- [x] Article → UC mapping (auto, initial version)
- [ ] Human review of UC mapping (especially multi-UC and unmatched articles)
- [ ] #8 Embedding model selection → determines vector(N) dimension
- [ ] Chunking pipeline (256-512 tokens, 10-20% overlap)
- [ ] HNSW index creation (`m=16, ef_construction=64, vector_cosine_ops`)
- [ ] End-to-end retrieval validation (query → Top-K → rerank → grounding)

## Output Files

| File | Purpose |
|------|---------|
| `knowledge_base_articles.json` | pgvector article-level table (ready for chunking) |
| `article_uc_mapping.csv` | Article → UC mapping for human review |
| `mapping_summary_report.md` | This report |
