#!/usr/bin/env python3
"""
Build Knowledge Base & Article → UC Mapping from FAQ CSV.

Input:  docs/FAQ-knowledge_include_help_url.csv (219 Salesforce Knowledge articles)
Output:
  - data/knowledge/knowledge_base_articles.json   — pgvector article-level table (ready for chunking pipeline)
  - data/knowledge/article_uc_mapping.csv          — article → UC mapping for review
  - data/knowledge/mapping_summary_report.md       — coverage report
"""

import csv
import json
import os
import re
import sys
from collections import defaultdict
from html.parser import HTMLParser

# ---------------------------------------------------------------------------
# Config
# ---------------------------------------------------------------------------

PROJECT_ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
INPUT_CSV = os.path.join(PROJECT_ROOT, "docs", "FAQ-knowledge_include_help_url.csv")
OUTPUT_DIR = os.path.join(PROJECT_ROOT, "data", "knowledge")

# Use Cases that can call search_knowledge (Phase 2 §2.5 + §2.10)
SEARCH_ELIGIBLE_UCS = {"UC-A", "UC-B", "UC-C", "UC-D", "UC-E", "UC-F", "UC-FP"}
# Intake-only UCs — fixed scripts, no search_knowledge
INTAKE_ONLY_UCS = {"UC-G", "UC-H", "UC-I", "UC-J", "UC-K"}

# ---------------------------------------------------------------------------
# URL cat= → UC mapping (primary rules from Phase 2 §2.5 Knowledge Scope Map)
# ---------------------------------------------------------------------------
# UC-A: Ad Status & Visibility — ad review, status, category visibility
# UC-B: Posting & Editing Guidance — posting, editing, category/location/image rules
# UC-C: Messages & Replies — messaging, inbox, notifications
# UC-D: Account & Login — password, login, email, alerts
# UC-E: General Product & Search — search, features, safety awareness, feedback
# UC-F: Payment Inquiry — payments policy, fees, promoting features
# UC-FP: Correct Deletion / Compliance — community standards, ad policies, prohibited content

CAT_TO_UC = {
    # Ad lifecycle
    "Creating_Ads":                 ["UC-B"],
    "Managing_Ads":                 ["UC-B"],
    "Ad_Issues":                    ["UC-A", "UC-B"],

    # Account
    "Accounts_Profiles":            ["UC-D"],
    "Account_Issues":               ["UC-D"],

    # Messaging
    "Searching_Replying":           ["UC-C", "UC-E"],
    "Message_Issues":               ["UC-C"],

    # Safety & trust (user-facing awareness → UC-E; policy explanation → UC-FP)
    "Safely_Buying_Selling":        ["UC-E"],
    "Reporting_Bad_Activity":       ["UC-E", "UC-FP"],
    "Types_of_Scams":               ["UC-E"],
    "Keeping_Your_Information_Safe": ["UC-E"],

    # Policies & compliance
    "General_Policies":             ["UC-FP"],
    "Posting_Policies":             ["UC-B", "UC-FP"],
    "Category_Specific_Policies":   ["UC-B", "UC-FP"],
    "Pets_Policies":                ["UC-B", "UC-FP"],

    # Payments & features
    "Payments_on_Gumtree":          ["UC-F"],
    "Promoting_Ads":                ["UC-F"],
    "Features":                     ["UC-B", "UC-F"],
    "Feature_Issues":               ["UC-F"],

    # Business
    "Advertising_a_Business":       ["UC-B"],

    # Technical / troubleshooting
    "Troubleshooting":              ["UC-E", "UC-K"],
    "Other_Errors":                 ["UC-E", "UC-K"],

    # Misc
    "What_s_New_at_Gumtree":        ["UC-E"],
}

# ---------------------------------------------------------------------------
# Title keyword → UC fallback rules (for 39 articles without URL)
# ---------------------------------------------------------------------------

TITLE_KEYWORD_RULES = [
    # (pattern, UC list) — first match wins; order matters
    (r"(?i)\b(disputes?|complaints?|complain)\b",                               ["UC-E", "UC-FP"]),
    (r"(?i)\b(gdpr|data\s+(?:deletion|protection|request|privacy))\b",        ["UC-FP"]),
    (r"(?i)\b(scams?|fraud|phishing|spoof\w*|suspicious)\b",                    ["UC-E"]),
    (r"(?i)\b(report\w*|flag\w*|abuse)\b",                                     ["UC-E", "UC-FP"]),
    (r"(?i)\b(post\w*|creat\w*|listing|adverts?|(?<!\w)ads?\b)",               ["UC-B"]),
    (r"(?i)\b(edit|manag|delet|remov)\w*\s+(ads?|listing|adverts?)",           ["UC-B"]),
    (r"(?i)\b(accounts?|login|log\s*in|passwords?|register|sign\s*up|emails?)\b", ["UC-D"]),
    (r"(?i)\b(messages?|inbox|repl(?:y|ies|ying)|notifications?|chat)\b",      ["UC-C"]),
    (r"(?i)\b(search\w*|find|filter|browse)\b",                                ["UC-E"]),
    (r"(?i)\b(pay\w*|fees?|refunds?|bumps?|spotlight|urgent|featured|promot\w*)\b", ["UC-F"]),
    (r"(?i)\b(polic(?:y|ies)|rules?|standards?|prohibited|restrict\w*|complian\w*)\b", ["UC-FP"]),
    (r"(?i)\b(pets?|animals?|dogs?|cats?|kittens?|pupp(?:y|ies))\b",           ["UC-B", "UC-FP"]),
    (r"(?i)\b(propert(?:y|ies)|rent\w*|tenants?|landlords?|motors?|cars?|vehicles?)\b", ["UC-B"]),
    (r"(?i)\b(jobs?|recruit\w*|employers?|hiring)\b",                          ["UC-B"]),
    (r"(?i)\b(services?|business\w*|commercial|dealers?|auctions?)\b",         ["UC-B"]),
    (r"(?i)\b(safe\w*|protect\w*|security)\b",                                 ["UC-E"]),
    (r"(?i)\b(errors?|bugs?|issues?|problems?|troubleshoot\w*|can'?t|unable)\b", ["UC-E", "UC-K"]),
    (r"(?i)\b(photos?|images?|videos?|uploads?)\b",                            ["UC-B"]),
    (r"(?i)\b(apps?|android|ios|mobile)\b",                                    ["UC-E"]),
    (r"(?i)\b(moderat\w*|reviews?|takedowns?|removals?)\b",                    ["UC-A", "UC-FP"]),
]


# ---------------------------------------------------------------------------
# HTML Stripper
# ---------------------------------------------------------------------------

class HTMLStripper(HTMLParser):
    def __init__(self):
        super().__init__()
        self.result = []
        self._in_style = False
        self._in_script = False

    def handle_starttag(self, tag, attrs):
        if tag in ("style", "script"):
            self._in_style = True
        # Insert line break for block elements
        if tag in ("p", "br", "div", "li", "h1", "h2", "h3", "h4", "h5", "h6", "tr"):
            self.result.append("\n")

    def handle_endtag(self, tag):
        if tag in ("style", "script"):
            self._in_style = False
        if tag in ("p", "div", "li", "tr"):
            self.result.append("\n")

    def handle_data(self, d):
        if not self._in_style and not self._in_script:
            self.result.append(d)

    def get_text(self):
        text = "".join(self.result)
        # Normalize whitespace: collapse runs of spaces/tabs but keep newlines
        text = re.sub(r"[ \t]+", " ", text)
        # Collapse 3+ newlines into 2
        text = re.sub(r"\n{3,}", "\n\n", text)
        return text.strip()


def strip_html(html: str) -> str:
    if not html:
        return ""
    s = HTMLStripper()
    try:
        s.feed(html)
    except Exception:
        # Fallback: brute-force strip
        return re.sub(r"<[^>]+>", " ", html).strip()
    return s.get_text()


# ---------------------------------------------------------------------------
# UC Mapping Logic
# ---------------------------------------------------------------------------

def extract_url_category(url: str) -> str | None:
    if not url:
        return None
    m = re.search(r"cat=([^&]+)", url)
    return m.group(1) if m else None


def map_article_to_ucs(title: str, url: str, content: str) -> tuple[list[str], str]:
    """
    Returns (uc_list, mapping_method).
    mapping_method: 'url_category' | 'title_keyword' | 'content_keyword' | 'unmatched'
    """
    # Strategy 1: URL cat= parameter
    cat = extract_url_category(url)
    if cat and cat in CAT_TO_UC:
        ucs = CAT_TO_UC[cat]
        # Special case: GDPR articles in General_Policies → also tag UC-G for coverage
        if cat == "General_Policies" and re.search(r"(?i)\bgdpr\b", title):
            ucs = list(set(ucs + ["UC-G"]))
        return sorted(ucs), "url_category"

    # Strategy 2: Title keyword matching
    combined_text = title
    for pattern, ucs in TITLE_KEYWORD_RULES:
        if re.search(pattern, combined_text):
            return sorted(ucs), "title_keyword"

    # Strategy 3: Content keyword matching (broader search)
    search_text = (title + " " + content)[:2000]  # limit to first 2000 chars
    for pattern, ucs in TITLE_KEYWORD_RULES:
        if re.search(pattern, search_text):
            return sorted(ucs), "content_keyword"

    return ["UNMATCHED"], "unmatched"


def estimate_tokens(text: str) -> int:
    """Rough token estimate: ~4 chars per token for English."""
    return max(1, len(text) // 4)


# ---------------------------------------------------------------------------
# Main
# ---------------------------------------------------------------------------

def main():
    os.makedirs(OUTPUT_DIR, exist_ok=True)

    # Parse CSV
    with open(INPUT_CSV, encoding="utf-8") as f:
        rows = list(csv.DictReader(f))

    print(f"Parsed {len(rows)} articles from CSV")

    articles = []
    mapping_rows = []
    uc_article_counts = defaultdict(int)
    method_counts = defaultdict(int)
    unmatched = []

    for row in rows:
        article_id = row["Id"].strip()
        if not article_id:
            continue  # skip empty trailing rows
        title = row["Title"].strip()
        summary = row["Summary"].strip()
        url = row["Help_Site_URL__c"].strip()
        url_category = extract_url_category(url)
        content_plain = strip_html(row["Description__c"])

        # Map to UCs
        uc_tags, mapping_method = map_article_to_ucs(title, url, content_plain)
        method_counts[mapping_method] += 1

        if "UNMATCHED" in uc_tags:
            unmatched.append({"article_id": article_id, "title": title})

        # Determine search_knowledge eligibility
        eligible_ucs = [uc for uc in uc_tags if uc in SEARCH_ELIGIBLE_UCS]
        search_eligible = len(eligible_ucs) > 0

        token_est = estimate_tokens(content_plain)

        # Build article record (pgvector article-level schema)
        article = {
            "article_id": article_id,
            "title": title,
            "summary": summary if summary else None,
            "content_plain": content_plain,
            "source_url": url if url else None,
            "url_category": url_category,
            "uc_tags": uc_tags,
            "search_knowledge_eligible": search_eligible,
            "eligible_uc_scope": sorted(eligible_ucs),
            "mapping_method": mapping_method,
            "token_estimate": token_est,
            "published_status": True,
            "source_type": "salesforce_knowledge",
        }
        articles.append(article)

        # Mapping CSV row
        mapping_rows.append({
            "article_id": article_id,
            "title": title,
            "url_category": url_category or "",
            "uc_tags": "|".join(uc_tags),
            "search_eligible": search_eligible,
            "mapping_method": mapping_method,
            "token_estimate": token_est,
            "source_url": url,
        })

        for uc in uc_tags:
            uc_article_counts[uc] += 1

    # --- Write outputs ---

    # 1. Knowledge base JSON
    kb_path = os.path.join(OUTPUT_DIR, "knowledge_base_articles.json")
    with open(kb_path, "w", encoding="utf-8") as f:
        json.dump({
            "metadata": {
                "version": "v1.0",
                "source": "FAQ-knowledge_include_help_url.csv",
                "total_articles": len(articles),
                "search_eligible_articles": sum(1 for a in articles if a["search_knowledge_eligible"]),
                "schema_note": "Article-level table; ready for chunking pipeline (256-512 tokens, 10-20% overlap) → pgvector chunk table",
                "pending_dependencies": [
                    "#8 Embedding model selection (determines vector dimensions)",
                    "Chunking pipeline implementation"
                ],
            },
            "articles": articles,
        }, f, ensure_ascii=False, indent=2)
    print(f"  → {kb_path}")

    # 2. Mapping CSV
    mapping_path = os.path.join(OUTPUT_DIR, "article_uc_mapping.csv")
    with open(mapping_path, "w", encoding="utf-8", newline="") as f:
        writer = csv.DictWriter(f, fieldnames=[
            "article_id", "title", "url_category", "uc_tags",
            "search_eligible", "mapping_method", "token_estimate", "source_url",
        ])
        writer.writeheader()
        writer.writerows(mapping_rows)
    print(f"  → {mapping_path}")

    # 3. Summary report
    report_lines = []
    report_lines.append("# Knowledge Base — Article → UC Mapping Report")
    report_lines.append("")
    report_lines.append(f"**Generated**: auto (build_knowledge_base.py)")
    report_lines.append(f"**Source**: `docs/FAQ-knowledge_include_help_url.csv`")
    report_lines.append("")
    report_lines.append("## Overview")
    report_lines.append("")
    report_lines.append(f"| Metric | Value |")
    report_lines.append(f"|--------|-------|")
    report_lines.append(f"| Total articles | {len(articles)} |")
    report_lines.append(f"| search_knowledge eligible | {sum(1 for a in articles if a['search_knowledge_eligible'])} |")
    report_lines.append(f"| Intake-only UC articles | {sum(1 for a in articles if not a['search_knowledge_eligible'] and 'UNMATCHED' not in a['uc_tags'])} |")
    report_lines.append(f"| Unmatched | {len(unmatched)} |")
    report_lines.append("")
    report_lines.append("## Mapping Method Distribution")
    report_lines.append("")
    report_lines.append("| Method | Count |")
    report_lines.append("|--------|-------|")
    for method, count in sorted(method_counts.items(), key=lambda x: -x[1]):
        report_lines.append(f"| {method} | {count} |")
    report_lines.append("")
    report_lines.append("## Per-UC Article Count")
    report_lines.append("")
    report_lines.append("| UC | Name | Article Count | search_knowledge |")
    report_lines.append("|-----|------|--------------|------------------|")

    UC_NAMES = {
        "UC-A": "Ad Status & Visibility",
        "UC-B": "Posting & Editing Guidance",
        "UC-C": "Messages & Replies",
        "UC-D": "Account & Login",
        "UC-E": "General Product & Search",
        "UC-F": "Payment Inquiry",
        "UC-FP": "Correct Deletion / Compliance",
        "UC-G": "GDPR / Data Deletion",
        "UC-H": "Incorrect Deletion Appeal",
        "UC-I": "Refund / Payment Dispute",
        "UC-J": "Trust & Safety / Fraud Report",
        "UC-K": "Technical Issue",
    }
    for uc in ["UC-A", "UC-B", "UC-C", "UC-D", "UC-E", "UC-F", "UC-FP",
               "UC-G", "UC-H", "UC-I", "UC-J", "UC-K"]:
        count = uc_article_counts.get(uc, 0)
        eligible = "Yes" if uc in SEARCH_ELIGIBLE_UCS else "No (fixed scripts)"
        name = UC_NAMES.get(uc, "")
        report_lines.append(f"| {uc} | {name} | {count} | {eligible} |")

    if unmatched:
        report_lines.append("")
        report_lines.append(f"| UNMATCHED | — | {len(unmatched)} | — |")

    report_lines.append("")
    # Sprint 14 §L0 — KB URL / published-safety data-quality block.
    # Surface missing canonical URLs as a visible DQ gap rather than
    # silently coalescing them to null at ingest time. The Java tools
    # (`SearchKnowledgeTool`, `ResolveArticleTool`) also stamp
    # `canonical_url_missing` per hit at runtime.
    articles_with_url = sum(1 for a in articles if a["source_url"])
    articles_missing_url = len(articles) - articles_with_url
    articles_published = sum(1 for a in articles if a.get("published_status") is True)
    articles_unpublished = len(articles) - articles_published
    articles_unsafe = sum(
        1 for a in articles
        if not a.get("published_status") or not a.get("content_plain")
    )
    report_lines.append("## URL & Published-Safety Coverage (Sprint 14 §L0)")
    report_lines.append("")
    report_lines.append("| Metric | Value |")
    report_lines.append("|--------|-------|")
    report_lines.append(f"| Total articles | {len(articles)} |")
    report_lines.append(f"| Articles published | {articles_published} |")
    report_lines.append(f"| Articles unpublished | {articles_unpublished} |")
    report_lines.append(f"| Articles with canonical_url | {articles_with_url} |")
    report_lines.append(f"| Articles missing canonical_url | {articles_missing_url} |")
    report_lines.append(f"| Articles unsafe to show (unpublished or empty body) | {articles_unsafe} |")
    report_lines.append("")
    report_lines.append("## Token Budget Summary")
    report_lines.append("")
    tokens = [a["token_estimate"] for a in articles]
    report_lines.append(f"| Metric | Value |")
    report_lines.append(f"|--------|-------|")
    report_lines.append(f"| Total tokens (all articles) | ~{sum(tokens):,} |")
    report_lines.append(f"| Avg tokens/article | ~{sum(tokens)//len(tokens):,} |")
    report_lines.append(f"| Max tokens (single article) | ~{max(tokens):,} |")
    report_lines.append(f"| Min tokens (single article) | ~{min(tokens):,} |")
    report_lines.append(f"| Articles > 512 tokens (need chunking) | {sum(1 for t in tokens if t > 512)} |")
    report_lines.append(f"| Articles <= 512 tokens (single chunk) | {sum(1 for t in tokens if t <= 512)} |")

    if unmatched:
        report_lines.append("")
        report_lines.append("## Unmatched Articles (need manual UC assignment)")
        report_lines.append("")
        report_lines.append("| article_id | title |")
        report_lines.append("|------------|-------|")
        for u in unmatched:
            report_lines.append(f"| `{u['article_id']}` | {u['title']} |")

    report_lines.append("")
    report_lines.append("## Phase 3 Readiness Checklist")
    report_lines.append("")
    report_lines.append("- [x] Article-level knowledge base JSON generated")
    report_lines.append("- [x] Article → UC mapping (auto, initial version)")
    report_lines.append("- [ ] Human review of UC mapping (especially multi-UC and unmatched articles)")
    report_lines.append("- [ ] #8 Embedding model selection → determines vector(N) dimension")
    report_lines.append("- [ ] Chunking pipeline (256-512 tokens, 10-20% overlap)")
    report_lines.append("- [ ] HNSW index creation (`m=16, ef_construction=64, vector_cosine_ops`)")
    report_lines.append("- [ ] End-to-end retrieval validation (query → Top-K → rerank → grounding)")
    report_lines.append("")
    report_lines.append("## Output Files")
    report_lines.append("")
    report_lines.append("| File | Purpose |")
    report_lines.append("|------|---------|")
    report_lines.append("| `knowledge_base_articles.json` | pgvector article-level table (ready for chunking) |")
    report_lines.append("| `article_uc_mapping.csv` | Article → UC mapping for human review |")
    report_lines.append("| `mapping_summary_report.md` | This report |")

    report_path = os.path.join(OUTPUT_DIR, "mapping_summary_report.md")
    with open(report_path, "w", encoding="utf-8") as f:
        f.write("\n".join(report_lines) + "\n")
    print(f"  → {report_path}")

    # Print summary to stdout
    print(f"\n{'='*60}")
    print(f"SUMMARY")
    print(f"{'='*60}")
    print(f"Total articles:           {len(articles)}")
    print(f"search_knowledge eligible: {sum(1 for a in articles if a['search_knowledge_eligible'])}")
    print(f"Unmatched:                {len(unmatched)}")
    print(f"Mapping methods:          {dict(method_counts)}")
    print(f"\nPer-UC counts:")
    for uc in ["UC-A","UC-B","UC-C","UC-D","UC-E","UC-F","UC-FP","UC-G","UC-H","UC-I","UC-J","UC-K"]:
        c = uc_article_counts.get(uc, 0)
        bar = "█" * c
        print(f"  {uc:6s} ({c:3d}): {bar}")
    if unmatched:
        print(f"\n  UNMATCHED ({len(unmatched)}):")
        for u in unmatched:
            print(f"    - {u['title']}")


if __name__ == "__main__":
    main()
