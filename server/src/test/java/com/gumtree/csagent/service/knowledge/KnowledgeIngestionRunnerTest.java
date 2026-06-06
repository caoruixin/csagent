package com.gumtree.csagent.service.knowledge;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gumtree.csagent.model.KbArticle;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * R6 (Sub-sprint C-2b) — KB ingest {@code search_knowledge_eligible}
 * preservation.
 *
 * <p>Pins the corpus-curation source-chain: a {@code search_knowledge_eligible}
 * flag arriving via {@code knowledge_base_articles.json} must reach the
 * {@link KbArticle} entity. The field mirrors the {@code published_status}
 * ingest idiom ({@code path(...).asBoolean(true)}): an explicit {@code false}
 * is respected; an absent / null field defaults to {@code true} so the 216
 * articles that have always carried {@code true} (and any future article that
 * omits the field) stay visible on the search surface.
 */
class KnowledgeIngestionRunnerTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void buildKbArticleFromJson_respectsExplicitSearchKnowledgeEligibleFalse() throws Exception {
        // The two (temp) CS-only template articles carry
        // search_knowledge_eligible=false; ingestion must persist that so the
        // search-surface filter can hide them.
        JsonNode node = mapper.readTree("""
                {
                  "article_id": "ka41r000000LIEJAA4",
                  "title": "(temp) Ad removed - By CS (general)",
                  "content_plain": "Sorry to hear your ad's not live! ... XXXXXXXXX ...",
                  "source_url": null,
                  "uc_tags": ["UC-B"],
                  "search_knowledge_eligible": false,
                  "published_status": true,
                  "token_estimate": 184
                }
                """);

        KbArticle article = KnowledgeIngestionRunner.buildKbArticleFromJson(
                node, Map.of(), OffsetDateTime.now());

        assertFalse(article.isSearchKnowledgeEligible(),
                "explicit search_knowledge_eligible=false must reach KbArticle — "
                        + "the search-surface filter depends on it");
    }

    @Test
    void buildKbArticleFromJson_persistsSearchKnowledgeEligibleTrue() throws Exception {
        JsonNode node = mapper.readTree("""
                {
                  "article_id": "ka4P2000000021pIAA",
                  "title": "Where is my advert?",
                  "content_plain": "Ads can be hidden during moderation review.",
                  "source_url": "https://help.gumtree.com/s/policies?article=Where-is-my-ad",
                  "uc_tags": ["UC-A"],
                  "search_knowledge_eligible": true,
                  "published_status": true,
                  "token_estimate": 42
                }
                """);

        KbArticle article = KnowledgeIngestionRunner.buildKbArticleFromJson(
                node, Map.of(), OffsetDateTime.now());

        assertTrue(article.isSearchKnowledgeEligible(),
                "explicit search_knowledge_eligible=true must reach KbArticle as visible");
    }

    @Test
    void buildKbArticleFromJson_defaultsToTrue_whenFieldAbsent() throws Exception {
        // Back-compat: an article that never carried the field (or a future
        // export that omits it) defaults to visible — NOT silently hidden.
        JsonNode node = mapper.readTree("""
                {
                  "article_id": "kaNOFIELD00001",
                  "title": "Legacy article without the eligibility field",
                  "content_plain": "Body.",
                  "source_url": "https://help.gumtree.com/s/policies?article=Legacy",
                  "uc_tags": ["UC-A"],
                  "published_status": true,
                  "token_estimate": 5
                }
                """);

        KbArticle article = KnowledgeIngestionRunner.buildKbArticleFromJson(
                node, Map.of(), OffsetDateTime.now());

        assertTrue(article.isSearchKnowledgeEligible(),
                "absent search_knowledge_eligible must default to true (visible), "
                        + "mirroring the published_status default-true idiom");
    }
}
