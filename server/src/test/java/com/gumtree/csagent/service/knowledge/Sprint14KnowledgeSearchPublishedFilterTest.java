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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Sprint 14 §L0 — KB published-safety filter regression.
 *
 * <p>Pins two contracts:
 * <ol>
 *   <li>{@link KnowledgeSearchService#search} routes through the
 *       {@code findNearestByEmbedding*PublishedOnly} repository methods so
 *       unpublished candidates never enter the rerank pipeline.</li>
 *   <li>If a stale ANN cache or a mocked repository ever returns an
 *       unpublished article post-rerank, the hit-projection step drops it
 *       (defense-in-depth).</li>
 * </ol>
 *
 * <p>The {@link RerankService} is mocked because Sprint 14 §L0 is about the
 * SQL filter and projection guard — the rerank LLM is out of scope.
 */
@ExtendWith(MockitoExtension.class)
class Sprint14KnowledgeSearchPublishedFilterTest {

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
    void search_routesToPublishedOnlyAnnPath_whenUcTagsSupplied() {
        when(embeddingClient.embed(anyString())).thenReturn(new float[]{0.1f, 0.2f, 0.3f});
        when(kbChunkRepository.findNearestByEmbeddingWithUcTagsPublishedOnly(
                anyString(), any(String[].class), anyInt())).thenReturn(List.of());

        KnowledgeSearchResult result = service.search(
                "where is my ad", List.of("UC-A"), "sess-pub-filter-1", 0);

        verify(kbChunkRepository).findNearestByEmbeddingWithUcTagsPublishedOnly(
                anyString(), any(String[].class), anyInt());
        verify(kbChunkRepository, never()).findNearestByEmbeddingWithUcTags(
                anyString(), any(String[].class), anyInt());
        assertTrue(result.isRetrievalMiss(),
                "no chunks → retrieval_miss; published-filter routing is the contract being pinned");
    }

    @Test
    void search_routesToPublishedOnlyAnnPath_whenNoUcTags() {
        when(embeddingClient.embed(anyString())).thenReturn(new float[]{0.1f, 0.2f, 0.3f});
        when(kbChunkRepository.findNearestByEmbeddingPublishedOnly(anyString(), anyInt()))
                .thenReturn(List.of());

        KnowledgeSearchResult result = service.search(
                "where is my ad", List.of(), "sess-pub-filter-2", 0);

        verify(kbChunkRepository).findNearestByEmbeddingPublishedOnly(anyString(), anyInt());
        verify(kbChunkRepository, never()).findNearestByEmbedding(anyString(), anyInt());
        assertTrue(result.isRetrievalMiss());
    }

    @Test
    void search_postRerankProjection_dropsAnyArticleStillUnpublished() {
        // Defense-in-depth: even if the published-only ANN somehow returned
        // an unpublished article (stale cache, mocked repo, race window), the
        // hit-projection step must drop it before the agent sees it.
        when(embeddingClient.embed(anyString())).thenReturn(unitVector());

        KbChunk publishedChunk = KbChunk.builder()
                .chunkId("kaPUB-1_chunk_0")
                .articleId("kaPUB-1")
                .chunkIndex(0)
                .chunkText("Ads can be hidden during moderation review.")
                .embedding(unitVector())
                .tokenCount(10)
                .sectionHeading(null)
                .build();
        KbChunk unpublishedChunk = KbChunk.builder()
                .chunkId("kaUNPUB-1_chunk_0")
                .articleId("kaUNPUB-1")
                .chunkIndex(0)
                .chunkText("Draft article body — must not surface.")
                .embedding(unitVector())
                .tokenCount(10)
                .sectionHeading(null)
                .build();
        when(kbChunkRepository.findNearestByEmbeddingPublishedOnly(anyString(), anyInt()))
                .thenReturn(List.of(publishedChunk, unpublishedChunk));

        // Force the rerank to emit BOTH the published and unpublished
        // candidates as high-scoring results so the projection-stage filter
        // is the sole guard left.
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

        KbArticle publishedArticle = KbArticle.builder()
                .articleId("kaPUB-1")
                .title("Where is my advert?")
                .description("Ads can be hidden during moderation review.")
                .sourceUrl("https://help.example/where-is-my-ad")
                .ucTags(new String[]{"UC-A"})
                .isPublished(true)
                .build();
        KbArticle unpublishedArticle = KbArticle.builder()
                .articleId("kaUNPUB-1")
                .title("Draft article")
                .description("Draft article body.")
                .sourceUrl("https://help.example/draft")
                .ucTags(new String[]{"UC-A"})
                .isPublished(false)
                .build();
        when(kbArticleRepository.findAllById(any()))
                .thenReturn(List.of(publishedArticle, unpublishedArticle));

        KnowledgeSearchResult result = service.search(
                "where is my ad", List.of(), "sess-pub-defense", 0);

        assertNotNull(result);
        List<KnowledgeHit> hits = result.getHits();
        assertEquals(1, hits.size(),
                "unpublished article must be dropped by the projection-stage published filter");
        assertEquals("kaPUB-1", hits.get(0).getSourceId());
        assertFalse(hits.get(0).isCanonicalUrlMissing(),
                "published article carrying a canonical URL must report canonical_url_missing=false");
    }

    @Test
    void search_stampsCanonicalUrlMissingFlag_whenArticleHasNoSourceUrl() {
        when(embeddingClient.embed(anyString())).thenReturn(unitVector());

        KbChunk chunk = KbChunk.builder()
                .chunkId("kaNOURL_chunk_0")
                .articleId("kaNOURL")
                .chunkIndex(0)
                .chunkText("This article has no Help_Site_URL on file.")
                .embedding(unitVector())
                .tokenCount(10)
                .build();
        when(kbChunkRepository.findNearestByEmbeddingPublishedOnly(anyString(), anyInt()))
                .thenReturn(List.of(chunk));

        when(rerankService.rerank(anyString(), any(), anyString(), anyInt()))
                .thenReturn(List.of(new RerankService.ScoredCandidate(
                        "kaNOURL", "kaNOURL_chunk_0",
                        "This article has no Help_Site_URL on file.",
                        null, 0.9, 4.0)));

        KbArticle article = KbArticle.builder()
                .articleId("kaNOURL")
                .title("Missing URL article")
                .description("This article has no Help_Site_URL on file.")
                .sourceUrl(null)
                .ucTags(new String[]{"UC-E"})
                .isPublished(true)
                .build();
        when(kbArticleRepository.findAllById(any())).thenReturn(List.of(article));

        KnowledgeSearchResult result = service.search(
                "missing url query", List.of(), "sess-no-url", 0);

        assertEquals(1, result.getHits().size());
        KnowledgeHit hit = result.getHits().get(0);
        assertEquals("kaNOURL", hit.getSourceId());
        assertTrue(hit.isCanonicalUrlMissing(),
                "missing canonical_url must be visible as a data-quality gap, not silently null");
    }

    private static float[] unitVector() {
        // Small non-zero embedding so cosine_similarity is comfortably above
        // RETRIEVAL_GATE_THRESHOLD (0.3) and rerank decisions own the gate.
        return new float[]{1.0f, 0.0f, 0.0f};
    }
}
