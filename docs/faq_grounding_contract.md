# FAQ Grounding Contract

Sprint: Sprint 14 — FAQ / KB Evidence Lineage and Safety
Date: 2026-05-09
Status: Normative
Scope: §L1 (evidence lineage) + §L2 (grounding contract + soft diagnostics)

This document is the canonical contract for FAQ grounding observability.
It does NOT introduce a hard runtime citation gate; that surface remains
explicitly out of scope per `docs/sprint_objective.md` §"Do not implement"
and `docs/runtime_freeze_and_risk_policy.md` §1 (frozen runtime).

## 1. Term distinctions

The Sprint 14 §L1 contract splits the historically-overloaded `sourceIds`
concept into three independently-observable dimensions:

| Field | Stage | Origin | Sprint 14 §L1 slot |
|-------|-------|--------|---------------------|
| `retrieved_source_ids` | search-knowledge hit list | `SearchKnowledgeTool` → `KnowledgeHit.sourceId` (post §L0 published-safety filter) | `BotSession.retrievedSourceIds` (transient) |
| `resolved_source_ids` | resolve-article success | `ResolveArticleTool` (only successful results that survived the §L0 unpublished-refusal guard) | `BotSession.resolvedSourceIds` (transient) |
| `cited_source_ids` | passive extraction from bot reply | `CitationExtractor` over the customer-visible message; URL or title matches map back to known candidates | `BotSession.citedSourceIds` (transient) |
| `cited_canonical_urls` | passive extraction from bot reply | URLs in the reply that did NOT correspond to any candidate `canonical_url` (typically a third-party domain such as `gov.uk`) | `BotSession.citedCanonicalUrls` (transient) |

The three sets are independent. A turn can:

- retrieve without resolving (LLM short-circuited, deadline, refusal);
- resolve without citing (paraphrased / uncited answer);
- cite without retrieving (LLM repeated a URL out of training data);
- retrieve, resolve, AND cite (the §L2 `factual_grounded` ideal state).

## 2. Output class taxonomy

Sprint 14 §L2 classifies every customer-visible bot reply into one of six
output classes (`FaqOutputClass.java`):

| Class | Definition | Requires grounding? | Heuristic signal (`FaqOutputClassifier`) |
|-------|-----------|---------------------|------------------------------------------|
| `factual_answer` | Bot states a fact about Gumtree policy, behaviour, fees, refunds, account, listing rules, etc. | YES | Default class when no lower-grounding shape matches. |
| `clarification` | Bot asks the user a clarifying question. | NO | Trailing `?`, or "could you share / can you provide" with no intake-UC context. |
| `empathy_ack` | Bot acknowledges user emotion / distress without making a factual claim. | NO | "I'm sorry", "that sounds frustrating / tough / stressful", "I understand how", in a short non-question sentence. |
| `handover` | Bot announces handover to a human agent. | NO | `handover_dispatched` runtime flag (canonical), or "connecting you / transferring you / human agent / specialist / customer service team" wording. |
| `tool_status` | Bot is reporting a tool / system status. | NO | "looking into this / let me check / one moment / I'll check" in a short sentence. |
| `intake_collection` | Bot is collecting required intake fields for an intake UC (UC-G/H/I/J/K). | NO | Active UC is intake AND ("could you provide / please provide / what is your X / to help further I will need" or trailing `?`). |

Only `factual_answer` requires grounding evidence. The other five classes
are observability-classified but never require citation.

The classifier is intentionally conservative: when the reply has no
clear non-factual shape, it defaults to `factual_answer` so the §L2
diagnostics err on the side of "this turn might need grounding".

## 3. Sprint 14 §L1 contract — retrieved / resolved / cited evidence

### 3.1 Construction

`SourceEvidenceLineage.fromToolEvents(toolEvents, botResponse)` walks the
`AgentRunLoop` events for the current turn:

- every successful `search_knowledge` event contributes to
  `retrievedSourceIds` (de-duplicated, insertion order preserved); also
  capture each hit's `canonical_url` and `title` for the citation
  extractor;
- every successful `resolve_article` event contributes to
  `resolvedSourceIds`. Sprint 14 §L0 — a refused unpublished resolve
  (`article_unpublished_safe_refuse`) does NOT count as resolved and
  the article remains in the retrieved-but-unresolved set;
- the customer-visible `bot_response` is fed to `CitationExtractor`
  alongside the union of retrieved + resolved candidates so URL / title
  matches map back to a `source_id`.

### 3.2 Citation patterns (in order of confidence)

1. **Source ID mention** — Salesforce Knowledge ID pattern
   `\bka[A-Za-z0-9]{13}([A-Za-z0-9]{3})?\b`. Tight pattern to avoid false
   positives on unrelated alphanumerics.
2. **Canonical URL mention** — `https?://\S+`, trimmed of trailing
   punctuation. Matched first against the candidate `canonical_url`
   set; if no candidate matches, the URL is recorded under
   `citedCanonicalUrls` as a free-form citation (typically third-party).
3. **Title fuzzy match** — case-insensitive contiguous-substring match
   against any candidate whose title is at least three words (avoids
   false positives like "Login" or "Help"). Only used when the cited
   `source_id` set does not already include the candidate.

### 3.3 Non-blocking guarantee

Sprint 14 §L1 explicitly does NOT use the result of citation extraction
to reject, rewrite, or loop the response. The output is observability
material consumed by:

- trace surfaces (`projection.faq_grounding`, `RECORD_OUTCOME_GUARD`
  enrichment);
- §L2 grounding diagnostics (`faq_grounding_state`,
  `resolved_but_uncited`, `retrieved_but_unresolved`,
  `citation_present`, `citation_match`, `citation_drift`).

The agent run loop is unchanged: no new short-circuits, no new escalation
reasons, no new retries.

## 4. Sprint 14 §L2 contract — soft grounding diagnostics

`FaqGroundingDiagnostics.compute(outputClass, lineage)` returns six
observability fields per turn:

| Field | Meaning |
|-------|---------|
| `faq_grounding_state` | Coarse state token, one of: `factual_grounded`, `factual_uncited`, `factual_unresolved`, `factual_unretrieved`, `non_factual`, `unknown`. |
| `citation_present` | True iff the reply cited at least one `source_id` or canonical URL. |
| `citation_match` | True iff at least one cited `source_id` was in the retrieved or resolved candidate set. |
| `citation_drift` | True iff at least one cited `source_id` or URL was NOT in the candidate set (third-party drift / hallucination warning). |
| `resolved_but_uncited` | True iff at least one resolved article was not cited in the reply. |
| `retrieved_but_unresolved` | True iff at least one retrieved article was never resolved (potential §G2 grounded-resolve gap on a factual answer). |

### 4.1 State transition table

| `outputClass` | `retrieved` | `resolved` | `citation_present` | `citation_match` | `citation_drift` | → `faq_grounding_state` |
|---------------|-------------|-----------|---------------------|------------------|------------------|-------------------------|
| `factual_answer` | yes | yes | yes | yes | no | `factual_grounded` |
| `factual_answer` | yes | yes | no | (n/a) | (n/a) | `factual_uncited` |
| `factual_answer` | yes | no | (any) | (n/a) | (n/a) | `factual_unresolved` |
| `factual_answer` | no | no | (any) | (n/a) | (n/a) | `factual_unretrieved` |
| `clarification` | (any) | (any) | (any) | (any) | (any) | `non_factual` |
| `empathy_ack` | (any) | (any) | (any) | (any) | (any) | `non_factual` |
| `handover` | (any) | (any) | (any) | (any) | (any) | `non_factual` |
| `tool_status` | (any) | (any) | (any) | (any) | (any) | `non_factual` |
| `intake_collection` | (any) | (any) | (any) | (any) | (any) | `non_factual` |
| (null) | (any) | (any) | (any) | (any) | (any) | `unknown` |

### 4.2 Non-blocking guarantee

Sprint 14 §L2 diagnostics are observability-only. They MUST NOT:

- block the user response;
- short-circuit the agent run loop;
- introduce a new escalation reason;
- alter the resolved disposition;
- rewrite the user-visible message;
- trigger a clarification re-loop;
- override the §L0 published-safety guard;
- override the §G2 FAQ-grounded-resolve guard (§L2 is observability that
  COMPLEMENTS §G2; §G2 stays the only enforced runtime check).

## 5. Wiring (current Sprint 14 surface)

Diagnostics are stamped per-turn on `BotSession` transient fields:

- §L1: `retrievedSourceIds`, `resolvedSourceIds`, `citedSourceIds`,
  `citedCanonicalUrls`.
- §L2: `faqOutputClass`, `faqGroundingState`, `citationPresent`,
  `citationMatch`, `citationDrift`, `resolvedButUncited`,
  `retrievedButUnresolved`.

Population happens in `ControlKernel.recordRunResult` (the same site
that already collects `sourceIds` for `bot_turns.source_ids`). The
existing `bot_turns.source_ids` column is preserved verbatim — no
schema migration. Trace UIs can pick up the new fields from the next
context projection or from a future enrichment of an existing event
payload (out of scope for Sprint 14).

## 6. Future narrow Sprint 16 candidates (deferred)

The §L0 audit and §L2 diagnostics surface a handful of narrow
factual-answer hardening opportunities. Each one is a candidate for a
future narrow Sprint 16 §S1 hardening, NOT for Sprint 14:

- **R-faq-grounded-resolve-bypass** — when a factual answer is emitted
  with `retrieved_but_unresolved=true` AND the active UC is FAQ-path,
  the existing Sprint 6 §G2 guard already refuses the
  `request_handover(faq_miss_threshold_exceeded)` shape, but it does NOT
  refuse the FINAL_ANSWER shape that paraphrases an unresolved hit.
  Future narrow §S1 hardening could refuse / replan that exact shape.
- **R-cited-but-unresolved** — `citation_drift=true` with a cited
  `source_id` that was never retrieved or resolved this turn. Could be
  a hallucination signal if reproduced across multiple cases.
- **R-resolved-but-uncited-rate** — track the per-corpus rate of
  `resolved_but_uncited=true` factual turns; consistently high rates
  suggest the prompt is not encouraging citation. Eval Governance owns
  this until a real-traffic incident escalates it.
- **R-canonical-url-missing-rate** — 38 articles in the current corpus
  have a missing `canonical_url`. A future corpus-curation sprint can
  fill those in; Sprint 14 only ensures the gap is observable.

These are recorded here so Codex / future authors can reference them
without re-deriving the analysis. Sprint 14 explicitly does not open
any of them as runtime work.

## 7. References

- `docs/sprint_objective.md` §L0 / §L1 / §L2.
- `docs/10-handoff.md` Sprint 14 summary.
- `qa-reports/faq-kb-lineage-and-url-audit.md` (§L0 audit + repair).
- `docs/runtime_freeze_and_risk_policy.md` §1 (frozen runtime contract).
- Source: `server/src/main/java/com/gumtree/csagent/service/knowledge/CitationExtractor.java`.
- Source: `server/src/main/java/com/gumtree/csagent/service/knowledge/SourceEvidenceLineage.java`.
- Source: `server/src/main/java/com/gumtree/csagent/service/knowledge/FaqOutputClass.java`.
- Source: `server/src/main/java/com/gumtree/csagent/service/knowledge/FaqOutputClassifier.java`.
- Source: `server/src/main/java/com/gumtree/csagent/service/knowledge/FaqGroundingDiagnostics.java`.
- Tests: `server/src/test/java/com/gumtree/csagent/service/knowledge/Sprint14*Test.java`.
- Tests: `server/src/test/java/com/gumtree/csagent/service/tools/Sprint14ResolveArticleSafetyTest.java`.
