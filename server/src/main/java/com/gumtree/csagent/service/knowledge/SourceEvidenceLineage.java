package com.gumtree.csagent.service.knowledge;

import com.gumtree.csagent.model.ToolEvent;
import com.gumtree.csagent.service.tools.ResolveArticleTool;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Sprint 14 §L1 — turn-level source evidence lineage.
 *
 * <p>Splits the historically-overloaded {@code sourceIds} concept into
 * three observably-distinct dimensions so a reviewer can answer:
 *
 * <ul>
 *   <li>Did the runtime <em>retrieve</em> any candidate articles
 *       this turn? (= {@link #retrievedSourceIds()}, populated from every
 *       successful {@code search_knowledge} tool call)</li>
 *   <li>Did the runtime <em>resolve</em> any of those candidates? (=
 *       {@link #resolvedSourceIds()}, populated from every successful
 *       {@code resolve_article} tool call that passed the §L0
 *       published-safety guard)</li>
 *   <li>Did the bot <em>cite</em> any source in the customer-visible
 *       reply? (= {@link #citedSourceIds()} /
 *       {@link #citedCanonicalUrls()}, populated by
 *       {@link CitationExtractor})</li>
 * </ul>
 *
 * <p>The three sets are independent: a turn can retrieve without
 * resolving (LLM short-circuited), resolve without citing (uncited
 * answer), or cite without resolving (the LLM quoted a {@code source_id}
 * directly from {@code search_knowledge.hits}). Sprint 14 §L1 explicitly
 * does NOT use the difference to reject or rewrite the response — these
 * fields are observability material that downstream §L2 grounding
 * diagnostics turn into soft signals.
 */
public record SourceEvidenceLineage(
        List<String> retrievedSourceIds,
        List<String> resolvedSourceIds,
        List<String> citedSourceIds,
        List<String> citedCanonicalUrls,
        Map<String, String> retrievedCanonicalUrls
) {

    public SourceEvidenceLineage {
        retrievedSourceIds = retrievedSourceIds == null
                ? List.of() : List.copyOf(retrievedSourceIds);
        resolvedSourceIds = resolvedSourceIds == null
                ? List.of() : List.copyOf(resolvedSourceIds);
        citedSourceIds = citedSourceIds == null
                ? List.of() : List.copyOf(citedSourceIds);
        citedCanonicalUrls = citedCanonicalUrls == null
                ? List.of() : List.copyOf(citedCanonicalUrls);
        retrievedCanonicalUrls = retrievedCanonicalUrls == null
                ? Map.of() : Map.copyOf(retrievedCanonicalUrls);
    }

    public static SourceEvidenceLineage empty() {
        return new SourceEvidenceLineage(
                List.of(), List.of(), List.of(), List.of(), Map.of());
    }

    public boolean retrievedAny() {
        return !retrievedSourceIds.isEmpty();
    }

    public boolean resolvedAny() {
        return !resolvedSourceIds.isEmpty();
    }

    public boolean citedAny() {
        return !citedSourceIds.isEmpty() || !citedCanonicalUrls.isEmpty();
    }

    /**
     * Sprint 14 §L1 — articles that survived retrieval and resolution but
     * never appeared in the customer-visible reply. A non-empty set
     * indicates the bot pulled a fact but did not quote / link it.
     */
    public List<String> resolvedButUncitedSourceIds() {
        if (resolvedSourceIds.isEmpty() || citedSourceIds.containsAll(resolvedSourceIds)) {
            return List.of();
        }
        Set<String> uncited = new LinkedHashSet<>(resolvedSourceIds);
        uncited.removeAll(citedSourceIds);
        return List.copyOf(uncited);
    }

    /**
     * Sprint 14 §L1 — articles that were retrieved but never resolved.
     * A non-empty set on a FAQ factual-answer turn indicates a §G2
     * grounded-resolve gap (search ran, top hit was not deepened into a
     * full article body before the answer was emitted).
     */
    public List<String> retrievedButUnresolvedSourceIds() {
        if (retrievedSourceIds.isEmpty()) return List.of();
        if (resolvedSourceIds.isEmpty()) return List.copyOf(new LinkedHashSet<>(retrievedSourceIds));
        Set<String> unresolved = new LinkedHashSet<>(retrievedSourceIds);
        unresolved.removeAll(resolvedSourceIds);
        return List.copyOf(unresolved);
    }

    // -------------------------------------------------------------------
    //  Construction helpers
    // -------------------------------------------------------------------

    /**
     * Build a lineage record by walking the loop's tool events plus the
     * customer-visible bot response. Tolerant of missing / malformed
     * tool result data: defaults to empty subsets rather than throwing.
     *
     * <p>{@code resolvedSourceIds} are stamped only from
     * {@code resolve_article} calls that <em>succeeded</em> AND that did
     * NOT carry the §L0 {@code article_unpublished_safe_refuse} reject
     * reason — this preserves the §L0 published-safety contract: a
     * refused unpublished article is "retrieved but not resolved", never
     * "resolved".
     */
    public static SourceEvidenceLineage fromToolEvents(List<ToolEvent> toolEvents,
                                                        String botResponse) {
        if (toolEvents == null || toolEvents.isEmpty()) {
            // Bot may still cite a known URL even with no tool calls;
            // we cannot map it back without candidates, but we can record
            // it as an unmapped canonical URL.
            CitationExtractor.Citations citations = CitationExtractor.extract(
                    botResponse, List.of());
            return new SourceEvidenceLineage(
                    List.of(), List.of(),
                    citations.citedSourceIds(),
                    citations.citedCanonicalUrls(),
                    Map.of());
        }

        List<String> retrieved = new ArrayList<>();
        List<String> resolved = new ArrayList<>();
        Map<String, String> canonicalUrls = new LinkedHashMap<>();
        Map<String, String> titles = new LinkedHashMap<>();

        for (ToolEvent te : toolEvents) {
            if (te == null || !te.success() || te.resultData() == null) continue;
            if ("search_knowledge".equals(te.toolName())) {
                ingestSearchKnowledge(te.resultData(), retrieved, canonicalUrls, titles);
            } else if ("resolve_article".equals(te.toolName())) {
                ingestResolveArticle(te.resultData(), resolved, canonicalUrls, titles);
            }
        }

        // Build candidate hints (retrieved ∪ resolved) for the citation
        // extractor so URL / title matches can map back to a source_id.
        Set<String> candidateIds = new LinkedHashSet<>();
        candidateIds.addAll(retrieved);
        candidateIds.addAll(resolved);
        List<CitationExtractor.ArticleCandidate> candidates = new ArrayList<>();
        for (String id : candidateIds) {
            candidates.add(new CitationExtractor.ArticleCandidate(
                    id, titles.get(id), canonicalUrls.get(id)));
        }

        CitationExtractor.Citations citations = CitationExtractor.extract(
                botResponse, candidates);

        // De-dup retrieved (preserving order) since search_knowledge can
        // return the same source_id across multiple loop iterations.
        return new SourceEvidenceLineage(
                List.copyOf(new LinkedHashSet<>(retrieved)),
                List.copyOf(new LinkedHashSet<>(resolved)),
                citations.citedSourceIds(),
                citations.citedCanonicalUrls(),
                Map.copyOf(canonicalUrls));
    }

    @SuppressWarnings("unchecked")
    private static void ingestSearchKnowledge(Object resultData,
                                                List<String> retrieved,
                                                Map<String, String> canonicalUrls,
                                                Map<String, String> titles) {
        if (!(resultData instanceof Map<?, ?> dataMap)) return;
        Object hitsObj = ((Map<String, Object>) dataMap).get("hits");
        if (!(hitsObj instanceof List<?> hits)) return;
        for (Object hit : hits) {
            if (!(hit instanceof Map<?, ?> hitMap)) continue;
            Map<String, Object> h = (Map<String, Object>) hitMap;
            Object sid = h.get("source_id");
            if (sid == null) continue;
            String sourceId = sid.toString();
            retrieved.add(sourceId);
            Object url = h.get("canonical_url");
            if (url != null) {
                canonicalUrls.putIfAbsent(sourceId, url.toString());
            }
            Object title = h.get("title");
            if (title != null) {
                titles.putIfAbsent(sourceId, title.toString());
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static void ingestResolveArticle(Object resultData,
                                               List<String> resolved,
                                               Map<String, String> canonicalUrls,
                                               Map<String, String> titles) {
        if (!(resultData instanceof Map<?, ?> dataMap)) return;
        Map<String, Object> data = (Map<String, Object>) dataMap;
        // §L0 published-safety: a refused unpublished resolve carries
        // an `error` field with UNPUBLISHED_REJECT_REASON. Such results
        // arrive as {error: ...} via AgentRunLoopImpl wrapping; defensive
        // double-check here in case a future caller encodes the refusal
        // inside the success payload.
        Object errObj = data.get("error");
        if (errObj instanceof String err
                && err.contains(ResolveArticleTool.UNPUBLISHED_REJECT_REASON)) {
            return;
        }
        Object sid = data.get("source_id");
        if (sid == null) sid = data.get("article_id");
        if (sid == null) return;
        String sourceId = sid.toString();
        resolved.add(sourceId);
        Object url = data.get("canonical_url");
        if (url == null) url = data.get("source_url");
        if (url != null) {
            canonicalUrls.putIfAbsent(sourceId, url.toString());
        }
        Object title = data.get("title");
        if (title != null) {
            titles.putIfAbsent(sourceId, title.toString());
        }
    }
}
