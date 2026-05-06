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

    @Test
    void execute_unknownSourceId_returnsArticleNotFoundError() {
        when(kbArticleRepository.findById("missing")).thenReturn(Optional.empty());

        ToolResult result = tool.execute(buildSession(), Map.of("source_id", "missing"));

        assertFalse(result.isSuccess());
        assertTrue(result.getErrorMessage().contains("Article not found"));
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
