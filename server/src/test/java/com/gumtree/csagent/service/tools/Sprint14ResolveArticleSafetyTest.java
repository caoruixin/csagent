package com.gumtree.csagent.service.tools;

import com.gumtree.csagent.model.BotSession;
import com.gumtree.csagent.model.KbArticle;
import com.gumtree.csagent.repository.KbArticleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/**
 * Sprint 14 §L0 — KB published-safety + canonical URL contract on
 * {@link ResolveArticleTool}.
 *
 * <p>Pins three contracts new in Sprint 14:
 * <ol>
 *   <li>Unpublished articles are refused with the deterministic
 *       {@link ResolveArticleTool#UNPUBLISHED_REJECT_REASON} reason — the
 *       agent cannot surface draft / safety-suspended content.</li>
 *   <li>Successful resolves expose the canonical URL under both the
 *       legacy {@code source_url} key AND the new {@code canonical_url}
 *       key (matching {@code search_knowledge.hits[*].canonical_url}) so
 *       the agent does not have to translate field names.</li>
 *   <li>Articles indexed without a Help_Site_URL surface
 *       {@code canonical_url=null} alongside {@code canonical_url_missing=true}
 *       — the gap is observable rather than hidden.</li>
 * </ol>
 */
@ExtendWith(MockitoExtension.class)
class Sprint14ResolveArticleSafetyTest {

    @Mock private KbArticleRepository kbArticleRepository;

    private ResolveArticleTool tool;

    @BeforeEach
    void setUp() {
        tool = new ResolveArticleTool(kbArticleRepository);
    }

    @Test
    void execute_refusesUnpublishedArticle_withDeterministicRejectReason() {
        KbArticle draft = KbArticle.builder()
                .articleId("kaDRAFT")
                .title("Draft article")
                .description("Body that has not been approved for publication.")
                .sourceUrl("https://help.example/draft")
                .ucTags(new String[]{"UC-A"})
                .isPublished(false)
                .build();
        when(kbArticleRepository.findById("kaDRAFT")).thenReturn(Optional.of(draft));

        ToolResult result = tool.execute(buildSession(), Map.of("source_id", "kaDRAFT"));

        assertFalse(result.isSuccess(),
                "resolve_article must refuse an unpublished article — Sprint 14 §L0 published-safety");
        assertNotNull(result.getErrorMessage());
        assertTrue(result.getErrorMessage().contains(
                        ResolveArticleTool.UNPUBLISHED_REJECT_REASON),
                "refusal must carry the canonical UNPUBLISHED_REJECT_REASON token so trace UIs can "
                        + "distinguish it from 'Article not found' or transport errors (was: "
                        + result.getErrorMessage() + ")");
    }

    @Test
    void execute_publishedArticle_exposesCanonicalUrlAlongsideLegacySourceUrl() {
        KbArticle published = KbArticle.builder()
                .articleId("kaPUB")
                .title("Where is my advert?")
                .summary("Common reasons your ad is not visible.")
                .description("Ads can be hidden during moderation review.")
                .sourceUrl("https://help.gumtree.com/s/policies?cat=Ad_Issues&article=Where-is-my-ad")
                .ucTags(new String[]{"UC-A"})
                .isPublished(true)
                .build();
        when(kbArticleRepository.findById("kaPUB")).thenReturn(Optional.of(published));

        ToolResult result = tool.execute(buildSession(), Map.of("source_id", "kaPUB"));

        assertTrue(result.isSuccess(), "published article must resolve successfully");
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) result.getData();
        assertNotNull(data);
        assertEquals(
                "https://help.gumtree.com/s/policies?cat=Ad_Issues&article=Where-is-my-ad",
                data.get("source_url"),
                "legacy `source_url` field must keep working for older callers / tests");
        assertEquals(
                "https://help.gumtree.com/s/policies?cat=Ad_Issues&article=Where-is-my-ad",
                data.get("canonical_url"),
                "Sprint 14 §L0 — `canonical_url` must mirror `search_knowledge.hits[*].canonical_url` "
                        + "so the agent does not have to translate between field names");
        assertEquals(Boolean.FALSE, data.get("canonical_url_missing"));
        assertEquals(Boolean.TRUE, data.get("safe_to_show"));
        assertEquals(Boolean.TRUE, data.get("is_published"));
    }

    @Test
    void execute_publishedArticleWithNoSourceUrl_classifiesGapAsObservable() {
        KbArticle noUrl = KbArticle.builder()
                .articleId("kaNOURL")
                .title("Article without canonical URL")
                .summary(null)
                .description("Body present but no Help_Site_URL on file.")
                .sourceUrl(null)
                .ucTags(new String[]{"UC-E"})
                .isPublished(true)
                .build();
        when(kbArticleRepository.findById("kaNOURL")).thenReturn(Optional.of(noUrl));

        ToolResult result = tool.execute(buildSession(), Map.of("source_id", "kaNOURL"));

        assertTrue(result.isSuccess(),
                "missing canonical URL is a data-quality gap, NOT a refusal — the article still "
                        + "resolves so the agent can use its body");
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) result.getData();
        assertNull(data.get("canonical_url"),
                "missing source_url must surface as canonical_url=null (no silent rewrite to '')");
        assertNull(data.get("source_url"),
                "legacy source_url must mirror the same null gap");
        assertEquals(Boolean.TRUE, data.get("canonical_url_missing"),
                "Sprint 14 §L0 — missing canonical_url must be flagged as observable DQ");
        // Body is present, so safe_to_show stays true even with the URL gap;
        // the URL gap is a visibility hint, not a published-safety refusal.
        assertEquals(Boolean.TRUE, data.get("safe_to_show"));
    }

    @Test
    void execute_publishedArticleWithBlankBody_marksUnsafeToShowButStillResolves() {
        // Sprint 14 §L0 — `safe_to_show` is the conjunctive predicate
        // (published AND non-blank body). Empty-body articles are not
        // refused outright (the agent may use the title), but the flag
        // surfaces so the prompt logic can decline to quote nothing.
        KbArticle emptyBody = KbArticle.builder()
                .articleId("kaEMPTY")
                .title("Title only article")
                .summary(null)
                .description("   ")
                .sourceUrl("https://help.example/empty")
                .ucTags(new String[]{"UC-E"})
                .isPublished(true)
                .build();
        when(kbArticleRepository.findById("kaEMPTY")).thenReturn(Optional.of(emptyBody));

        ToolResult result = tool.execute(buildSession(), Map.of("source_id", "kaEMPTY"));

        assertTrue(result.isSuccess());
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) result.getData();
        assertEquals(Boolean.FALSE, data.get("safe_to_show"));
        assertEquals(Boolean.TRUE, data.get("is_published"));
        assertEquals(Boolean.FALSE, data.get("canonical_url_missing"));
    }

    private static BotSession buildSession() {
        BotSession session = new BotSession();
        session.setSessionId("sess-resolve-safety-test");
        session.setActiveUseCase("UC-A");
        return session;
    }
}
