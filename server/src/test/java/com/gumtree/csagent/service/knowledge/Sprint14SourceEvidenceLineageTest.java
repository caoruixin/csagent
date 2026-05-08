package com.gumtree.csagent.service.knowledge;

import com.gumtree.csagent.model.ToolEvent;
import com.gumtree.csagent.service.tools.ResolveArticleTool;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Sprint 14 §L1 — split {@code retrieved} / {@code resolved} /
 * {@code cited} source evidence.
 *
 * <p>Pins the contract that retrieved evidence does NOT imply cited
 * evidence, that the citation extractor surfaces source_id and canonical
 * URL mentions deterministically, and that missing citation is observable
 * but never blocks the response.
 */
class Sprint14SourceEvidenceLineageTest {

    private static final String SEARCH_TOOL = "search_knowledge";
    private static final String RESOLVE_TOOL = "resolve_article";

    @Test
    void retrievedSource_doesNotImplyCitedSource() {
        // search_knowledge returned a hit but the bot reply does not
        // mention any source_id, canonical URL, or distinctive title.
        SourceEvidenceLineage lineage = SourceEvidenceLineage.fromToolEvents(
                List.of(searchEvent(0, hit("ka4P2000000021pIAA",
                        "Property Scams",
                        "https://help.gumtree.com/property-scams"))),
                "Hi, I'm looking into this for you.");

        assertEquals(List.of("ka4P2000000021pIAA"), lineage.retrievedSourceIds());
        assertTrue(lineage.resolvedSourceIds().isEmpty(),
                "no resolve_article call → no resolved evidence");
        assertTrue(lineage.citedSourceIds().isEmpty(),
                "Sprint 14 §L1 — retrieved evidence must NOT imply cited evidence");
        assertFalse(lineage.citedAny());
    }

    @Test
    void resolvedSource_doesNotImplyCitedSource() {
        // search_knowledge + resolve_article both succeeded, but the bot
        // reply paraphrased the article without quoting the source_id /
        // URL / distinctive title — uncited answer shape.
        ToolEvent search = searchEvent(0, hit("ka4P200000001AbIAI",
                "Property Scams Awareness",
                "https://help.gumtree.com/scams"));
        ToolEvent resolve = resolveSuccessEvent(1, "ka4P200000001AbIAI",
                "Property Scams Awareness",
                "https://help.gumtree.com/scams");

        SourceEvidenceLineage lineage = SourceEvidenceLineage.fromToolEvents(
                List.of(search, resolve),
                "If something seems too good to be true, please double-check before paying.");

        assertEquals(List.of("ka4P200000001AbIAI"), lineage.retrievedSourceIds());
        assertEquals(List.of("ka4P200000001AbIAI"), lineage.resolvedSourceIds());
        assertTrue(lineage.citedSourceIds().isEmpty(),
                "Sprint 14 §L1 — resolved evidence must be tracked separately from cited evidence");
        assertEquals(List.of("ka4P200000001AbIAI"), lineage.resolvedButUncitedSourceIds(),
                "resolved-but-uncited must surface as observable §L1 evidence diff");
        assertTrue(lineage.retrievedButUnresolvedSourceIds().isEmpty(),
                "retrieval was followed by a successful resolve, so unresolved set is empty");
    }

    @Test
    void citationExtractor_detectsSourceIdMentionInBotReply() {
        ToolEvent search = searchEvent(0, hit("ka4P2000000021pIAA",
                "Where is my advert",
                "https://help.gumtree.com/where-is-my-ad"));

        SourceEvidenceLineage lineage = SourceEvidenceLineage.fromToolEvents(
                List.of(search),
                "Per article ka4P2000000021pIAA, ads can be hidden during moderation.");

        assertEquals(List.of("ka4P2000000021pIAA"), lineage.citedSourceIds(),
                "Sprint 14 §L1 — citation extractor must detect a Salesforce KA source_id mention");
        assertTrue(lineage.citedAny());
    }

    @Test
    void citationExtractor_detectsCanonicalUrlMentionAndMapsBackToSourceId() {
        ToolEvent search = searchEvent(0, hit("ka4P2000000021pIAA",
                "Where is my advert",
                "https://help.gumtree.com/where-is-my-ad"));
        ToolEvent resolve = resolveSuccessEvent(1, "ka4P2000000021pIAA",
                "Where is my advert",
                "https://help.gumtree.com/where-is-my-ad");

        SourceEvidenceLineage lineage = SourceEvidenceLineage.fromToolEvents(
                List.of(search, resolve),
                "More details here: https://help.gumtree.com/where-is-my-ad.");

        assertEquals(List.of("ka4P2000000021pIAA"), lineage.citedSourceIds(),
                "URL mention must map back to the candidate source_id");
        assertTrue(lineage.citedCanonicalUrls().isEmpty(),
                "URL that matched a candidate canonical_url should NOT also appear as a free-form URL");
        assertTrue(lineage.resolvedButUncitedSourceIds().isEmpty(),
                "the cited URL credits the resolved article, so resolved-but-uncited is empty");
    }

    @Test
    void citationExtractor_detectsThirdPartyUrlAsFreeFormCitation() {
        // The bot links to a third-party domain (e.g. gov.uk) that the
        // runtime never retrieved. The URL must surface as a free-form
        // canonical citation, NOT silently dropped.
        SourceEvidenceLineage lineage = SourceEvidenceLineage.fromToolEvents(
                List.of(),
                "You can read more about consumer rights at https://www.gov.uk/consumer-rights.");

        assertTrue(lineage.citedSourceIds().isEmpty());
        assertEquals(1, lineage.citedCanonicalUrls().size());
        assertEquals("https://www.gov.uk/consumer-rights",
                lineage.citedCanonicalUrls().get(0));
    }

    @Test
    void missingCitation_isObservable_butNonBlocking() {
        // Sprint 14 §L1 — missing citation must be observable. We
        // confirm the diff between retrieved/resolved/cited is computable
        // and that the lineage record is still constructed (no exception,
        // no blocking) when the bot reply contains nothing citation-like.
        ToolEvent search = searchEvent(0, hit("ka4P2000000021pIAA",
                "Where is my advert",
                "https://help.gumtree.com/where-is-my-ad"));
        ToolEvent resolve = resolveSuccessEvent(1, "ka4P2000000021pIAA",
                "Where is my advert",
                "https://help.gumtree.com/where-is-my-ad");

        SourceEvidenceLineage lineage = SourceEvidenceLineage.fromToolEvents(
                List.of(search, resolve),
                "I understand. Could you share the ad ID so I can take a closer look?");

        assertTrue(lineage.retrievedAny());
        assertTrue(lineage.resolvedAny());
        assertFalse(lineage.citedAny(),
                "missing citation is an observable signal — not a block");
        assertEquals(List.of("ka4P2000000021pIAA"),
                lineage.resolvedButUncitedSourceIds(),
                "resolved-but-uncited diff must be available for §L2 diagnostics");
    }

    @Test
    void resolveArticle_unpublishedRefusal_isNotCountedAsResolved() {
        // §L0 published-safety: ResolveArticleTool refuses unpublished
        // articles. AgentRunLoopImpl wraps a failed ToolResult into a
        // success=false ToolEvent with the canonical reject reason in
        // errorMessage. The resolved set must NOT include the refused id.
        ToolEvent search = searchEvent(0, hit("kaUNPUB",
                "Draft article",
                "https://help.example/draft"));
        ToolEvent refusedResolve = new ToolEvent(
                1, 1, RESOLVE_TOOL, Map.of("source_id", "kaUNPUB"),
                false, null,
                ResolveArticleTool.UNPUBLISHED_REJECT_REASON
                        + ": article 'kaUNPUB' is not published",
                5L);

        SourceEvidenceLineage lineage = SourceEvidenceLineage.fromToolEvents(
                List.of(search, refusedResolve),
                "I will check this for you.");

        assertEquals(List.of("kaUNPUB"), lineage.retrievedSourceIds());
        assertTrue(lineage.resolvedSourceIds().isEmpty(),
                "a refused unpublished resolve must NOT show up as resolved evidence");
        assertEquals(List.of("kaUNPUB"), lineage.retrievedButUnresolvedSourceIds());
    }

    // -------------------------------------------------------------------
    //  Test helpers
    // -------------------------------------------------------------------

    private static ToolEvent searchEvent(int seq, Map<String, Object> hit) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("faq_miss", false);
        data.put("retrieval_miss", false);
        data.put("answer_miss", false);
        data.put("hits", List.of(hit));
        return new ToolEvent(seq, seq, SEARCH_TOOL,
                Map.of("query", "test"), true, data, null, 12L);
    }

    private static Map<String, Object> hit(String sourceId, String title,
                                             String canonicalUrl) {
        Map<String, Object> hit = new LinkedHashMap<>();
        hit.put("source_id", sourceId);
        hit.put("title", title);
        hit.put("snippet", "snippet");
        hit.put("canonical_url", canonicalUrl);
        hit.put("canonical_url_missing", canonicalUrl == null || canonicalUrl.isBlank());
        hit.put("score", 4.5);
        return hit;
    }

    private static ToolEvent resolveSuccessEvent(int seq, String sourceId, String title,
                                                   String canonicalUrl) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("source_id", sourceId);
        data.put("article_id", sourceId);
        data.put("title", title);
        data.put("description", "Body of article " + sourceId);
        data.put("source_url", canonicalUrl);
        data.put("canonical_url", canonicalUrl);
        data.put("canonical_url_missing", canonicalUrl == null || canonicalUrl.isBlank());
        data.put("is_published", true);
        data.put("safe_to_show", true);
        return new ToolEvent(seq, seq, RESOLVE_TOOL,
                Map.of("source_id", sourceId), true, data, null, 8L);
    }
}
