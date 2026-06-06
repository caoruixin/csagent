package com.gumtree.csagent.service.knowledge;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gumtree.csagent.config.KnowledgeRetrievalProperties;
import com.gumtree.csagent.model.BotSession;
import com.gumtree.csagent.model.KbArticle;
import com.gumtree.csagent.model.KbChunk;
import com.gumtree.csagent.model.KnowledgeHit;
import com.gumtree.csagent.model.KnowledgeSearchResult;
import com.gumtree.csagent.repository.KbArticleRepository;
import com.gumtree.csagent.repository.KbChunkRepository;
import com.gumtree.csagent.service.embedding.EmbeddingClient;
import com.gumtree.csagent.service.tools.ResolveArticleTool;
import com.gumtree.csagent.service.tools.ToolResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * R8 (Sub-sprint S-Auto-28) #4(c) — R6 end-to-end wiring (mock-level).
 *
 * <p>Chains the three components the milestone-shared re-bless depends on, in
 * the order the live {@code --reconcile} run exercises them:
 * <ol>
 *   <li>{@link KnowledgeIngestionRunner#reconcileExisting} flips the 2
 *       {@code (temp)} templates {@code search_knowledge_eligible: true ->
 *       false} from the JSON source-of-truth;</li>
 *   <li>{@link KnowledgeSearchService#search} (the R6 filter, byte-untouched
 *       by this sub-sprint; {@code SearchKnowledgeTool} purely delegates to it)
 *       then drops the now-ineligible templates from the LLM-facing hits;</li>
 *   <li>{@link ResolveArticleTool} direct-by-id STILL returns both templates
 *       (anti-误杀 #1 — the filter is at the search surface only).</li>
 * </ol>
 *
 * <p>This is wiring evidence per §5.7 — it proves the reconcile output feeds
 * the R6 filter correctly. The real-DB §0.3 spot-check (2 {@code false} / 216
 * {@code true}) is produced by the live {@code --reconcile} run in the
 * Definition-of-done sequence, not here.
 */
@ExtendWith(MockitoExtension.class)
class KnowledgeReconcileEndToEndTest {

    private static final String TEMPLATE_1 = "ka41r000000LIEJAA4";
    private static final String TEMPLATE_2 = "ka41r000000LIEEAA4";

    @Mock private EmbeddingClient embeddingClient;
    @Mock private KbChunkRepository kbChunkRepository;
    @Mock private KbArticleRepository kbArticleRepository;
    @Mock private ChunkingService chunkingService;
    @Mock private RerankService rerankService;

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void reconcileThenSearch_excludesBothTemplates_butDirectResolveStillReturnsThem() throws Exception {
        // (1) Two (temp) templates start eligible=true; one normal eligible article.
        KbArticle t1 = template(TEMPLATE_1, "(temp) Ad removed - By CS (general)");
        KbArticle t2 = template(TEMPLATE_2, "(temp) NTD Ad Removed Information");
        KbArticle eligible = eligibleArticle("kaELIG-1");

        // (1) Reconcile flips BOTH templates to false from the JSON source-of-truth.
        KnowledgeIngestionRunner runner = new KnowledgeIngestionRunner(
                kbArticleRepository, kbChunkRepository, chunkingService, embeddingClient, mapper);
        OffsetDateTime now = OffsetDateTime.now();
        assertTrue(runner.reconcileExisting(eligibleFalseJson(TEMPLATE_1), Map.of(), t1, now));
        assertTrue(runner.reconcileExisting(eligibleFalseJson(TEMPLATE_2), Map.of(), t2, now));
        assertFalse(t1.isSearchKnowledgeEligible(), "template 1 must reconcile to ineligible");
        assertFalse(t2.isSearchKnowledgeEligible(), "template 2 must reconcile to ineligible");

        // (2) Search surface drops the now-ineligible templates.
        KnowledgeSearchService search = new KnowledgeSearchService(
                embeddingClient, kbChunkRepository, kbArticleRepository, rerankService,
                new KnowledgeRetrievalProperties());
        wireRerankPassthrough();
        when(kbChunkRepository.findNearestByEmbeddingPublishedOnly(anyString(), anyInt()))
                .thenReturn(List.of(chunk(TEMPLATE_1), chunk("kaELIG-1"), chunk(TEMPLATE_2)));
        when(kbArticleRepository.findAllById(any())).thenReturn(List.of(t1, eligible, t2));

        KnowledgeSearchResult result = search.search("ad removed", List.of(), "sess-e2e", 0);
        List<String> ids = result.getHits().stream()
                .map(KnowledgeHit::getSourceId)
                .collect(Collectors.toList());
        assertFalse(ids.contains(TEMPLATE_1),
                "reconciled-ineligible template 1 must not surface on the search surface");
        assertFalse(ids.contains(TEMPLATE_2),
                "reconciled-ineligible template 2 must not surface on the search surface");
        assertTrue(ids.contains("kaELIG-1"),
                "the eligible article must still surface");

        // (3) Direct resolve still returns BOTH templates (anti-误杀 #1).
        ResolveArticleTool resolve = new ResolveArticleTool(kbArticleRepository);
        when(kbArticleRepository.findById(TEMPLATE_1)).thenReturn(Optional.of(t1));
        when(kbArticleRepository.findById(TEMPLATE_2)).thenReturn(Optional.of(t2));
        BotSession session = buildSession();
        ToolResult r1 = resolve.execute(session, Map.of("source_id", TEMPLATE_1));
        ToolResult r2 = resolve.execute(session, Map.of("source_id", TEMPLATE_2));
        assertTrue(r1.isSuccess(),
                "template 1 must still resolve directly by id after reconcile (filter is search-only)");
        assertTrue(r2.isSuccess(),
                "template 2 must still resolve directly by id after reconcile (filter is search-only)");
    }

    // ---------- helpers ----------

    private JsonNode eligibleFalseJson(String articleId) throws Exception {
        return mapper.readTree("""
                {
                  "article_id": "%s",
                  "title": "DB Title (unchanged by reconcile)",
                  "content_plain": "DB Body (unchanged by reconcile)",
                  "source_url": null,
                  "uc_tags": ["UC-B"],
                  "search_knowledge_eligible": false,
                  "published_status": true,
                  "token_estimate": 184
                }
                """.formatted(articleId));
    }

    private static KbArticle template(String articleId, String title) {
        // Published CS-only template, eligible=true pre-reconcile.
        return KbArticle.builder()
                .articleId(articleId)
                .title(title)
                .description("Sorry to hear your ad's not live! ... XXXXXXXXX ...")
                .sourceUrl(null)
                .ucTags(new String[]{"UC-B"})
                .isPublished(true)
                .searchKnowledgeEligible(true)
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

    private static float[] unitVector() {
        return new float[]{1.0f, 0.0f, 0.0f};
    }

    private static BotSession buildSession() {
        BotSession session = new BotSession();
        session.setSessionId("sess-reconcile-e2e");
        session.setActiveUseCase("UC-B");
        return session;
    }
}
