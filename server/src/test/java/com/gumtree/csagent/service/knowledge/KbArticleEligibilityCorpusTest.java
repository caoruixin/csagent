package com.gumtree.csagent.service.knowledge;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * R6 (Sub-sprint C-2b) — corpus data-flip verification.
 *
 * <p>Pins the R6 #1 data edit against the live corpus
 * ({@code data/knowledge/knowledge_base_articles.json}): exactly the two
 * {@code (temp)} CS-only template articles carry
 * {@code search_knowledge_eligible=false}; every other article remains
 * {@code true}. This guards against an accidental over-flip (误杀) on a
 * future corpus edit.
 */
class KbArticleEligibilityCorpusTest {

    private static final String JSON_RELATIVE = "data/knowledge/knowledge_base_articles.json";
    private static final Set<String> EXPECTED_INELIGIBLE = Set.of(
            "ka41r000000LIEJAA4",
            "ka41r000000LIEEAA4");

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void corpus_exactlyTheTwoTemplateArticlesAreSearchIneligible() throws IOException {
        JsonNode articles = loadArticles();

        Set<String> ineligible = new HashSet<>();
        int total = 0;
        for (JsonNode article : articles) {
            total++;
            // Default-true: an article that omits the field is visible.
            boolean eligible = article.path("search_knowledge_eligible").asBoolean(true);
            if (!eligible) {
                ineligible.add(article.path("article_id").asText());
            }
        }

        assertTrue(total >= 200, "sanity: full corpus loaded (got " + total + " articles)");
        assertEquals(EXPECTED_INELIGIBLE, ineligible,
                "exactly the two (temp) CS-only template articles must be "
                        + "search_knowledge_eligible=false; no over-flip / under-flip");
    }

    @Test
    void corpus_flaggedArticlesRemainPublishedSoDirectResolveStillWorks() throws IOException {
        // The R6 filter is at the search surface only. The flagged articles
        // must stay published_status=true so resolve_article continues to
        // surface them for human-CS use (anti-误杀 #1 / #5).
        JsonNode articles = loadArticles();
        int checked = 0;
        for (JsonNode article : articles) {
            String id = article.path("article_id").asText();
            if (EXPECTED_INELIGIBLE.contains(id)) {
                checked++;
                assertFalse(article.path("search_knowledge_eligible").asBoolean(true),
                        id + " must be search-ineligible");
                assertTrue(article.path("published_status").asBoolean(true),
                        id + " must stay published so resolve_article still surfaces it");
            }
        }
        assertEquals(2, checked, "both flagged articles must be present in the corpus");
    }

    private JsonNode loadArticles() throws IOException {
        Path path = resolveCorpusPath();
        JsonNode root = mapper.readTree(path.toFile());
        JsonNode articles = root.path("articles");
        assertTrue(articles.isArray(), "expected an 'articles' array in the corpus JSON");
        return articles;
    }

    private static Path resolveCorpusPath() {
        // Tests run from the server/ module dir; the corpus lives at the repo
        // root. Try the direct path first, then the parent (mirrors
        // KnowledgeIngestionRunner.resolveDataPath).
        Path direct = Path.of(JSON_RELATIVE);
        if (Files.exists(direct)) {
            return direct;
        }
        Path fromParent = Path.of("..").resolve(JSON_RELATIVE);
        if (Files.exists(fromParent)) {
            return fromParent;
        }
        throw new IllegalStateException(
                "knowledge_base_articles.json not found at " + direct.toAbsolutePath()
                        + " or " + fromParent.toAbsolutePath());
    }
}
