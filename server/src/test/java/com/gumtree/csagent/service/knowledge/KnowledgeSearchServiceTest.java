package com.gumtree.csagent.service.knowledge;

import com.gumtree.csagent.model.KbArticle;
import com.gumtree.csagent.model.KbChunk;
import com.gumtree.csagent.model.KnowledgeHit;
import com.gumtree.csagent.model.KnowledgeSearchResult;
import com.gumtree.csagent.repository.KbArticleRepository;
import com.gumtree.csagent.repository.KbChunkRepository;
import com.gumtree.csagent.service.embedding.EmbeddingClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * R6 (Sub-sprint C-2b) — KB search-surface {@code search_knowledge_eligible}
 * filter.
 *
 * <p>Pins the contract that {@link KnowledgeSearchService#search} drops any
 * candidate whose {@code KbArticle.searchKnowledgeEligible} is {@code false}
 * at the hit-projection step (mirroring the Sprint 14 §L0 published
 * defense-in-depth filter), while {@code true} / default candidates pass
 * through. The filter is data-field driven only — no title-keyword match on
 * {@code (temp)}, no body inspection, no hardcoded article-id.
 *
 * <p>{@link RerankService} is mocked because R6 is about the projection-stage
 * eligibility filter, not the rerank LLM.
 */
@ExtendWith(MockitoExtension.class)
class KnowledgeSearchServiceTest {

    @Mock private EmbeddingClient embeddingClient;
    @Mock private KbChunkRepository kbChunkRepository;
    @Mock private KbArticleRepository kbArticleRepository;
    @Mock private RerankService rerankService;

    private KnowledgeSearchService service;

    @BeforeEach
    void setUp() {
        service = new KnowledgeSearchService(
                embeddingClient, kbChunkRepository, kbArticleRepository, rerankService,
                new com.gumtree.csagent.config.KnowledgeRetrievalProperties());
    }

    @Test
    void search_dropsIneligibleArticle_keepsEligible() {
        wireRerankPassthrough();
        when(kbChunkRepository.findNearestByEmbeddingPublishedOnly(anyString(), anyInt()))
                .thenReturn(List.of(
                        chunk("kaELIG-1"),
                        chunk("kaTEMP-1"),
                        chunk("kaELIG-2")));
        when(kbArticleRepository.findAllById(any())).thenReturn(List.of(
                eligibleArticle("kaELIG-1"),
                ineligibleArticle("kaTEMP-1"),
                eligibleArticle("kaELIG-2")));

        KnowledgeSearchResult result = service.search(
                "ad removed query", List.of(), "sess-elig-1", 0);

        List<String> ids = result.getHits().stream()
                .map(KnowledgeHit::getSourceId)
                .collect(Collectors.toList());
        assertEquals(2, ids.size(), "the ineligible (temp) article must be filtered out");
        assertTrue(ids.contains("kaELIG-1"));
        assertTrue(ids.contains("kaELIG-2"));
        assertTrue(!ids.contains("kaTEMP-1"),
                "search_knowledge_eligible=false article must not surface on the search surface");
    }

    @Test
    void search_allEligible_hitsUnchanged() {
        wireRerankPassthrough();
        when(kbChunkRepository.findNearestByEmbeddingPublishedOnly(anyString(), anyInt()))
                .thenReturn(List.of(chunk("kaELIG-1"), chunk("kaELIG-2")));
        when(kbArticleRepository.findAllById(any())).thenReturn(List.of(
                eligibleArticle("kaELIG-1"),
                eligibleArticle("kaELIG-2")));

        KnowledgeSearchResult result = service.search(
                "normal query", List.of(), "sess-elig-2", 0);

        assertEquals(2, result.getHits().size(),
                "all-eligible candidate set must pass through unfiltered");
    }

    @Test
    void search_allIneligible_hitsEmpty() {
        wireRerankPassthrough();
        when(kbChunkRepository.findNearestByEmbeddingPublishedOnly(anyString(), anyInt()))
                .thenReturn(List.of(chunk("kaTEMP-1"), chunk("kaTEMP-2")));
        when(kbArticleRepository.findAllById(any())).thenReturn(List.of(
                ineligibleArticle("kaTEMP-1"),
                ineligibleArticle("kaTEMP-2")));

        KnowledgeSearchResult result = service.search(
                "all temp query", List.of(), "sess-elig-3", 0);

        assertTrue(result.getHits().isEmpty(),
                "an all-ineligible candidate set must yield zero hits");
    }

    @Test
    void search_defaultEligibleArticle_isReturned() {
        // A KbArticle built without setting searchKnowledgeEligible must
        // default to true (entity @Builder.Default) and therefore surface —
        // the back-compat / default-visible invariant.
        wireRerankPassthrough();
        when(kbChunkRepository.findNearestByEmbeddingPublishedOnly(anyString(), anyInt()))
                .thenReturn(List.of(chunk("kaDEFAULT-1")));
        KbArticle defaulted = KbArticle.builder()
                .articleId("kaDEFAULT-1")
                .title("Article relying on the default eligibility")
                .description("Body.")
                .sourceUrl("https://help.example/default")
                .ucTags(new String[]{"UC-A"})
                .isPublished(true)
                .build(); // searchKnowledgeEligible left to @Builder.Default=true
        when(kbArticleRepository.findAllById(any())).thenReturn(List.of(defaulted));

        KnowledgeSearchResult result = service.search(
                "default query", List.of(), "sess-elig-4", 0);

        assertEquals(1, result.getHits().size(),
                "an article with default (unset) eligibility must be treated as visible");
        assertEquals("kaDEFAULT-1", result.getHits().get(0).getSourceId());
    }

    // ---------- helpers ----------

    private void wireRerankPassthrough() {
        when(embeddingClient.embed(anyString())).thenReturn(unitVector());
        when(rerankService.rerank(anyString(), any(), anyString(), anyInt()))
                .thenAnswer(inv -> {
                    @SuppressWarnings("unchecked")
                    List<RerankService.RerankCandidate> cs =
                            (List<RerankService.RerankCandidate>) inv.getArgument(1);
                    return cs.stream()
                            .map(c -> new RerankService.ScoredCandidate(
                                    c.articleId(), c.chunkId(), c.chunkText(),
                                    c.sectionHeading(), c.cosineSimilarity(), 4.5))
                            .collect(Collectors.toList());
                });
    }

    private static KbChunk chunk(String articleId) {
        return KbChunk.builder()
                .chunkId(articleId + "_chunk_0")
                .articleId(articleId)
                .chunkIndex(0)
                .chunkText("Body for " + articleId)
                .embedding(unitVector())
                .tokenCount(10)
                .sectionHeading(null)
                .build();
    }

    private static KbArticle eligibleArticle(String articleId) {
        return KbArticle.builder()
                .articleId(articleId)
                .title("Eligible " + articleId)
                .description("Body for " + articleId)
                .sourceUrl("https://help.example/" + articleId)
                .ucTags(new String[]{"UC-A"})
                .isPublished(true)
                .searchKnowledgeEligible(true)
                .build();
    }

    private static KbArticle ineligibleArticle(String articleId) {
        // Published but search-ineligible — a (temp) CS-only template shape.
        return KbArticle.builder()
                .articleId(articleId)
                .title("(temp) " + articleId)
                .description("CS-only template body for " + articleId)
                .sourceUrl(null)
                .ucTags(new String[]{"UC-B"})
                .isPublished(true)
                .searchKnowledgeEligible(false)
                .build();
    }

    private static float[] unitVector() {
        return new float[]{1.0f, 0.0f, 0.0f};
    }
}
