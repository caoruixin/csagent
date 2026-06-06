package com.gumtree.csagent.service.tools;

import com.gumtree.csagent.model.BotSession;
import com.gumtree.csagent.model.KbArticle;
import com.gumtree.csagent.model.KnowledgeHit;
import com.gumtree.csagent.repository.KbArticleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/**
 * Sprint 8.2 §M0a — schema/tool contract alignment for {@code resolve_article}.
 *
 * <p>Trace {@code 6f24c6ab} showed the LLM correctly calling
 * {@code resolve_article({"source_id": ...})} (matching the projected schema
 * and the {@code search_knowledge.hits[*].source_id} field) but the tool
 * implementation read {@code article_id} and returned
 * {@code Parameter 'article_id' is required}. The LLM retried until
 * {@code maxToolSteps} and the loop terminated as MAX_STEPS → ESCALATE.
 *
 * <p>These tests pin the canonical contract:
 * <ul>
 *   <li>{@code source_id} is the canonical input.</li>
 *   <li>{@code article_id} is accepted as a legacy alias.</li>
 *   <li>Missing both surfaces a clear error naming {@code source_id}.</li>
 *   <li>A {@link KnowledgeHit#getSourceId()} from {@code search_knowledge}
 *       can be passed straight through as {@code source_id} without
 *       translation.</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class ResolveArticleToolTest {

    private static final String FAQ_ID = "faq-find-my-ad";

    @Mock
    private KbArticleRepository kbArticleRepository;

    private ResolveArticleTool tool;

    @BeforeEach
    void setUp() {
        tool = new ResolveArticleTool(kbArticleRepository);
    }

    @Test
    void getName_returnsResolveArticle() {
        assertEquals("resolve_article", tool.getName());
    }

    @Test
    void execute_withCanonicalSourceId_resolvesArticle() {
        when(kbArticleRepository.findById(FAQ_ID)).thenReturn(Optional.of(buildArticle()));

        ToolResult result = tool.execute(buildSession(), Map.of("source_id", FAQ_ID));

        assertTrue(result.isSuccess(),
                "resolve_article must accept the canonical source_id parameter");
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) result.getData();
        assertNotNull(data);
        assertEquals(FAQ_ID, data.get("source_id"),
                "result data should expose source_id so downstream callers do not have to translate");
        assertEquals(FAQ_ID, data.get("article_id"),
                "article_id remains in the response payload for backward compatibility");
        assertEquals("Where is my advert?", data.get("title"));
    }

    @Test
    void execute_withLegacyArticleIdAlias_resolvesArticle() {
        when(kbArticleRepository.findById(FAQ_ID)).thenReturn(Optional.of(buildArticle()));

        ToolResult result = tool.execute(buildSession(), Map.of("article_id", FAQ_ID));

        assertTrue(result.isSuccess(),
                "resolve_article must accept article_id as a legacy alias for source_id");
    }

    @Test
    void execute_prefersSourceIdOverArticleIdWhenBothSupplied() {
        when(kbArticleRepository.findById(FAQ_ID)).thenReturn(Optional.of(buildArticle()));

        Map<String, Object> args = new LinkedHashMap<>();
        args.put("source_id", FAQ_ID);
        args.put("article_id", "some-other-id");

        ToolResult result = tool.execute(buildSession(), args);

        assertTrue(result.isSuccess(),
                "When both source_id and article_id are supplied, source_id wins");
    }

    @Test
    void execute_missingBothParams_returnsErrorNamingSourceId() {
        ToolResult result = tool.execute(buildSession(), Map.of());

        assertFalse(result.isSuccess(),
                "missing source_id (and article_id) must surface a tool error");
        String err = result.getErrorMessage();
        assertNotNull(err);
        assertTrue(err.contains("source_id"),
                "missing-parameter error must name the canonical 'source_id' (was: " + err + ")");
        assertFalse(err.equals("Parameter 'article_id' is required"),
                "missing-parameter error must not advertise the legacy alias as canonical (was: " + err + ")");
    }

    @Test
    void execute_blankSourceIdFallsThroughToArticleIdAlias() {
        when(kbArticleRepository.findById(FAQ_ID)).thenReturn(Optional.of(buildArticle()));

        Map<String, Object> args = new LinkedHashMap<>();
        args.put("source_id", "  ");
        args.put("article_id", FAQ_ID);

        ToolResult result = tool.execute(buildSession(), args);

        assertTrue(result.isSuccess(),
                "blank source_id must fall back to article_id alias rather than fail");
    }

    @Test
    void execute_searchKnowledgeHitSourceId_flowsDirectlyIntoResolveArticle() {
        // Sprint 8.2 §M0a — the canonical UC-A FAQ flow is:
        //   search_knowledge -> (pick top hit by source_id) -> resolve_article
        // This test wires both halves so the contract is end-to-end.
        when(kbArticleRepository.findById(FAQ_ID)).thenReturn(Optional.of(buildArticle()));

        // Simulate a KnowledgeHit shape projected by SearchKnowledgeTool.hits[*].
        KnowledgeHit hit = KnowledgeHit.builder()
                .sourceId(FAQ_ID)
                .title("Where is my advert?")
                .snippet("If you can't find your ad...")
                .canonicalUrl("https://help.example/faq/where-is-my-ad")
                .score(0.91)
                .build();
        List<KnowledgeHit> hits = List.of(hit);

        // Hand the hit's source_id straight to resolve_article — no translation.
        ToolResult result = tool.execute(buildSession(),
                Map.of("source_id", hits.get(0).getSourceId()));

        assertTrue(result.isSuccess(),
                "search_knowledge.hits[*].source_id must be directly callable as resolve_article(source_id=...)");
    }

    // ---------- R5 #1: display_citation ----------

    @Test
    void execute_displayCitation_isSourceUrl_whenSourceUrlPresent() {
        when(kbArticleRepository.findById(FAQ_ID)).thenReturn(Optional.of(buildArticle()));

        ToolResult result = tool.execute(buildSession(), Map.of("source_id", FAQ_ID));

        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) result.getData();
        assertEquals("https://help.example/faq/where-is-my-ad", data.get("display_citation"),
                "display_citation must be the source_url when it is present and non-blank");
    }

    @Test
    void execute_displayCitation_fallsBackToArticleId_whenSourceUrlNull() {
        KbArticle article = KbArticle.builder()
                .articleId(FAQ_ID)
                .title("Where is my advert?")
                .description("Ads can be hidden during moderation review.")
                .sourceUrl(null)
                .isPublished(true)
                .build();
        when(kbArticleRepository.findById(FAQ_ID)).thenReturn(Optional.of(article));

        ToolResult result = tool.execute(buildSession(), Map.of("source_id", FAQ_ID));

        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) result.getData();
        assertEquals(FAQ_ID, data.get("display_citation"),
                "display_citation must fall back to article_id when source_url is null");
    }

    @Test
    void execute_displayCitation_fallsBackToArticleId_whenSourceUrlBlank() {
        KbArticle article = KbArticle.builder()
                .articleId(FAQ_ID)
                .title("Where is my advert?")
                .description("Ads can be hidden during moderation review.")
                .sourceUrl("   ")
                .isPublished(true)
                .build();
        when(kbArticleRepository.findById(FAQ_ID)).thenReturn(Optional.of(article));

        ToolResult result = tool.execute(buildSession(), Map.of("source_id", FAQ_ID));

        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) result.getData();
        assertEquals(FAQ_ID, data.get("display_citation"),
                "display_citation must fall back to article_id when source_url is blank");
    }

    @Test
    void execute_displayCitation_isAdditive_existingFieldsPreserved() {
        when(kbArticleRepository.findById(FAQ_ID)).thenReturn(Optional.of(buildArticle()));

        ToolResult result = tool.execute(buildSession(), Map.of("source_id", FAQ_ID));

        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) result.getData();
        // Negative A — existing article_id field still populated as-was.
        assertEquals(FAQ_ID, data.get("article_id"),
                "display_citation is additive: article_id must remain unchanged");
        // Negative B — existing source_url field still populated as-was.
        assertEquals("https://help.example/faq/where-is-my-ad", data.get("source_url"),
                "display_citation is additive: source_url must remain unchanged");
    }

    @Test
    void execute_unknownSourceId_returnsArticleNotFoundError() {
        when(kbArticleRepository.findById("missing")).thenReturn(Optional.empty());

        ToolResult result = tool.execute(buildSession(), Map.of("source_id", "missing"));

        assertFalse(result.isSuccess());
        assertTrue(result.getErrorMessage().contains("Article not found"));
    }

    // ---------- R6 (Sub-sprint C-2b): direct-resolve invariant ----------

    @Test
    void execute_searchIneligibleArticle_stillResolvesByDirectId() {
        // anti-误杀 #1 — the R6 search-surface filter (KnowledgeSearchService)
        // hides search_knowledge_eligible=false articles from the LLM-facing
        // search surface ONLY. resolve_article resolves directly by id via the
        // repository (it never goes through KnowledgeSearchService), so a
        // human-CS agent (or the LLM following a known article_id) must still
        // be able to fetch a (temp) CS-only template. The eligibility flag is
        // NOT a published-safety refusal: the article is published, so it
        // resolves successfully.
        KbArticle csTemplate = KbArticle.builder()
                .articleId("ka41r000000LIEJAA4")
                .title("(temp) Ad removed - By CS (general)")
                .description("Sorry to hear your ad's not live! ... XXXXXXXXX ...")
                .sourceUrl(null)
                .ucTags(new String[]{"UC-B"})
                .isPublished(true)
                .searchKnowledgeEligible(false)
                .build();
        when(kbArticleRepository.findById("ka41r000000LIEJAA4"))
                .thenReturn(Optional.of(csTemplate));

        ToolResult result = tool.execute(buildSession(),
                Map.of("source_id", "ka41r000000LIEJAA4"));

        assertTrue(result.isSuccess(),
                "a search_knowledge_eligible=false (but published) article must still "
                        + "resolve via resolve_article — the R6 filter is at the search surface only");
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) result.getData();
        assertEquals("ka41r000000LIEJAA4", data.get("source_id"));
        assertEquals("(temp) Ad removed - By CS (general)", data.get("title"));
        assertEquals(Boolean.TRUE, data.get("safe_to_show"),
                "published + non-blank body → safe_to_show true; eligibility does not gate direct resolve");
    }

    private static BotSession buildSession() {
        BotSession session = new BotSession();
        session.setSessionId("sess-resolve-article-test");
        session.setActiveUseCase("UC-A");
        return session;
    }

    private static KbArticle buildArticle() {
        return KbArticle.builder()
                .articleId(FAQ_ID)
                .title("Where is my advert?")
                .summary("Common reasons your ad isn't visible.")
                .description("Ads can be hidden during moderation review.")
                .sourceUrl("https://help.example/faq/where-is-my-ad")
                .urlCategory("ad-support")
                .ucTags(new String[]{"UC-A"})
                .isPublished(true)
                .build();
    }
}
