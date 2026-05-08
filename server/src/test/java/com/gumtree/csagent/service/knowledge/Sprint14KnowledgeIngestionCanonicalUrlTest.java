package com.gumtree.csagent.service.knowledge;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gumtree.csagent.model.KbArticle;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Sprint 14 §L0 — KB ingest canonical-URL / published-state preservation.
 *
 * <p>Pins the FAQ source-chain contract: a Salesforce Knowledge article
 * arriving via {@code knowledge_base_articles.json} must reach the
 * {@link KbArticle} entity with both its {@code source_url}
 * (Help_Site_URL__c) and its {@code is_published} flag preserved verbatim.
 * Without this guarantee the {@code search_knowledge} /
 * {@code resolve_article} surfaces fall back to {@code canonical_url=null}
 * and {@code is_published=true} regardless of the source intent — the
 * latent risk that motivated Sprint 14 §L0.
 */
class Sprint14KnowledgeIngestionCanonicalUrlTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void buildKbArticleFromJson_preservesHelpSiteUrlAsCanonicalSourceUrl() throws Exception {
        JsonNode node = mapper.readTree("""
                {
                  "article_id": "ka4P2000000021pIAA",
                  "title": "Where is my advert?",
                  "summary": "Common reasons your ad is not visible.",
                  "content_plain": "Ads can be hidden during moderation review.",
                  "source_url": "https://help.gumtree.com/s/policies?cat=Ad_Issues&article=Where-is-my-ad",
                  "url_category": "Ad_Issues",
                  "uc_tags": ["UC-A"],
                  "published_status": true,
                  "token_estimate": 42
                }
                """);

        KbArticle article = KnowledgeIngestionRunner.buildKbArticleFromJson(
                node, Map.of(), OffsetDateTime.now());

        assertEquals("ka4P2000000021pIAA", article.getArticleId());
        assertEquals(
                "https://help.gumtree.com/s/policies?cat=Ad_Issues&article=Where-is-my-ad",
                article.getSourceUrl(),
                "Help_Site_URL__c must reach KbArticle.source_url verbatim — Sprint 14 §L0 source-chain audit");
        assertEquals(Boolean.TRUE, article.getIsPublished());
        assertNotNull(article.getUcTags());
        assertArrayEquals(new String[]{"UC-A"}, article.getUcTags());
    }

    @Test
    void buildKbArticleFromJson_respectsExplicitUnpublishedFlag() throws Exception {
        // If the source JSON ever flips an article to published_status=false
        // (legal takedown, draft, expired campaign), ingestion must respect
        // that — silently coercing to true would defeat the §L0 published
        // safety filter on the search/resolve surface.
        JsonNode node = mapper.readTree("""
                {
                  "article_id": "kaDRAFT00001",
                  "title": "Draft article",
                  "content_plain": "Not yet ready to publish.",
                  "source_url": "https://help.gumtree.com/s/policies?cat=Ad_Issues&article=Draft",
                  "url_category": "Ad_Issues",
                  "uc_tags": ["UC-A"],
                  "published_status": false,
                  "token_estimate": 10
                }
                """);

        KbArticle article = KnowledgeIngestionRunner.buildKbArticleFromJson(
                node, Map.of(), OffsetDateTime.now());

        assertEquals(Boolean.FALSE, article.getIsPublished(),
                "ingest must NOT silently coerce a falsy published_status to true");
    }

    @Test
    void buildKbArticleFromJson_missingSourceUrlSurfacesAsNullDqGap() throws Exception {
        // 38 articles in the current corpus snapshot have a null source_url.
        // Sprint 14 §L0 requires this to flow through as null (a visible
        // data-quality gap) rather than be silently rewritten to "".
        JsonNode node = mapper.readTree("""
                {
                  "article_id": "kaNOURL00001",
                  "title": "Missing URL article",
                  "content_plain": "Body without a Help_Site_URL on file.",
                  "source_url": null,
                  "url_category": null,
                  "uc_tags": ["UC-E"],
                  "published_status": true,
                  "token_estimate": 5
                }
                """);

        KbArticle article = KnowledgeIngestionRunner.buildKbArticleFromJson(
                node, Map.of(), OffsetDateTime.now());

        assertNull(article.getSourceUrl(),
                "missing Help_Site_URL must surface as null so observability can flag a DQ gap");
        assertEquals(Boolean.TRUE, article.getIsPublished());
    }

    @Test
    void buildKbArticleFromJson_fallsBackToCsvUcMappingWhenJsonHasNoUcTags() throws Exception {
        JsonNode node = mapper.readTree("""
                {
                  "article_id": "kaTAGFALLBACK",
                  "title": "Tag fallback",
                  "content_plain": "Body.",
                  "source_url": "https://help.gumtree.com/s/policies?article=Tag",
                  "published_status": true,
                  "token_estimate": 1
                }
                """);

        KbArticle article = KnowledgeIngestionRunner.buildKbArticleFromJson(
                node, Map.of("kaTAGFALLBACK", new String[]{"UC-B"}),
                OffsetDateTime.now());

        assertArrayEquals(new String[]{"UC-B"}, article.getUcTags());
        // Sanity: published_status default holds when present and true.
        assertTrue(Boolean.TRUE.equals(article.getIsPublished()));
        // And canonical URL is preserved.
        assertFalse(article.getSourceUrl() == null || article.getSourceUrl().isBlank());
    }
}
