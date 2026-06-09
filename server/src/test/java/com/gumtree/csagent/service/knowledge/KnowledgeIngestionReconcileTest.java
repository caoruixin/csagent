package com.gumtree.csagent.service.knowledge;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gumtree.csagent.model.KbArticle;
import com.gumtree.csagent.repository.KbArticleRepository;
import com.gumtree.csagent.repository.KbChunkRepository;
import com.gumtree.csagent.service.embedding.EmbeddingClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * R8 (Sub-sprint S-Auto-28) — {@code KnowledgeIngestionRunner} metadata-only
 * {@code --reconcile} data-application path.
 *
 * <p>Closes the gap the R6 unit tests missed: those exercise only the
 * empty-table insert path (via {@code buildKbArticleFromJson} on synthetic
 * articles). None covered the insert-only loader's skip-existing behaviour
 * against a <strong>populated</strong> table, which is exactly why R6's
 * {@code search_knowledge_eligible: true -> false} flip could never land on a
 * row that already existed (the M-Auto-6 pre-flight NO-GO at §0.3, brief
 * {@code preflight-2026-06-07-kb-ingest-skip-existing-blocks-r6-flag}).
 *
 * <p>These tests pin, with mock-interaction counts (not just final-state
 * assertions):
 * <ul>
 *   <li><b>#4(a)</b> an existing row's curation columns reconcile to the JSON
 *       source-of-truth ({@code search_knowledge_eligible} true→false, plus
 *       {@code is_published} and {@code uc_tags} neighbours);</li>
 *   <li><b>#4(b)</b> reconcile NEVER re-embeds / writes {@code kb_chunks}
 *       (anti-误杀 #2), NEVER overwrites content columns even when the JSON
 *       differs (anti-误杀 #3), a new id under {@code --reconcile} still
 *       inserts+embeds, and plain {@code --ingest} leaves an existing row
 *       skipped + untouched (anti-误杀 #4).</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class KnowledgeIngestionReconcileTest {

    @Mock private KbArticleRepository kbArticleRepository;
    @Mock private KbChunkRepository kbChunkRepository;
    @Mock private ChunkingService chunkingService;
    @Mock private EmbeddingClient embeddingClient;

    private final ObjectMapper mapper = new ObjectMapper();
    private KnowledgeIngestionRunner runner;

    @BeforeEach
    void setUp() {
        runner = new KnowledgeIngestionRunner(
                kbArticleRepository, kbChunkRepository, chunkingService, embeddingClient, mapper);
    }

    // ---------- #4(a) populated-DB reconcile regression ----------

    @Test
    void reconcile_flipsSearchKnowledgeEligible_trueToFalse_onExistingRow() throws Exception {
        // The exact M-Auto-6 R6 scenario: a (temp)-shaped row already in the DB
        // at search_knowledge_eligible=true; the JSON source-of-truth declares
        // false. Reconcile must apply the flip in place.
        KbArticle existing = dbArticle(true);
        JsonNode node = json("""
                {
                  "article_id": "kaX",
                  "title": "DB Title",
                  "content_plain": "DB Body",
                  "source_url": "https://db/url",
                  "uc_tags": ["UC-A"],
                  "search_knowledge_eligible": false,
                  "published_status": true,
                  "token_estimate": 100
                }
                """);

        boolean changed = runner.reconcileExisting(node, Map.of(), existing, OffsetDateTime.now());

        assertTrue(changed, "a true→false eligibility flip must be reported as a change");
        assertFalse(existing.isSearchKnowledgeEligible(),
                "search_knowledge_eligible must reconcile to the JSON value (false)");
        verify(kbArticleRepository, times(1)).save(existing);
        verify(embeddingClient, never()).embedBatch(anyList());
        verify(kbChunkRepository, never()).saveAll(any());
    }

    @Test
    void reconcile_appliesIsPublishedChange_onExistingRow() throws Exception {
        KbArticle existing = dbArticle(true);
        existing.setIsPublished(true);
        JsonNode node = json("""
                {
                  "article_id": "kaX",
                  "title": "DB Title",
                  "content_plain": "DB Body",
                  "source_url": "https://db/url",
                  "uc_tags": ["UC-A"],
                  "search_knowledge_eligible": true,
                  "published_status": false,
                  "token_estimate": 100
                }
                """);

        boolean changed = runner.reconcileExisting(node, Map.of(), existing, OffsetDateTime.now());

        assertTrue(changed, "an is_published change must be reported");
        assertEquals(Boolean.FALSE, existing.getIsPublished(),
                "is_published must reconcile to the JSON published_status value");
        verify(kbArticleRepository, times(1)).save(existing);
        verify(embeddingClient, never()).embedBatch(anyList());
    }

    @Test
    void reconcile_appliesUcTagsChange_onExistingRow() throws Exception {
        KbArticle existing = dbArticle(true);
        existing.setUcTags(new String[]{"UC-A"});
        JsonNode node = json("""
                {
                  "article_id": "kaX",
                  "title": "DB Title",
                  "content_plain": "DB Body",
                  "source_url": "https://db/url",
                  "uc_tags": ["UC-B", "UC-C"],
                  "search_knowledge_eligible": true,
                  "published_status": true,
                  "token_estimate": 100
                }
                """);

        boolean changed = runner.reconcileExisting(node, Map.of(), existing, OffsetDateTime.now());

        assertTrue(changed, "a uc_tags change must be reported");
        assertArrayEquals(new String[]{"UC-B", "UC-C"}, existing.getUcTags(),
                "uc_tags must reconcile to the JSON value");
        verify(kbArticleRepository, times(1)).save(existing);
    }

    // ---------- #4(b) negative / anti-误杀 tests ----------

    @Test
    void reconcile_doesNotReEmbedOrWriteChunks_onExistingRow() throws Exception {
        // anti-误杀 #2 — a curation update must NEVER recompute chunks/embeddings.
        KbArticle existing = dbArticle(true);
        JsonNode node = json("""
                {
                  "article_id": "kaX",
                  "title": "DB Title",
                  "content_plain": "DB Body",
                  "source_url": "https://db/url",
                  "uc_tags": ["UC-A"],
                  "search_knowledge_eligible": false,
                  "published_status": true,
                  "token_estimate": 100
                }
                """);

        runner.reconcileExisting(node, Map.of(), existing, OffsetDateTime.now());

        verify(embeddingClient, never()).embedBatch(anyList());
        verify(embeddingClient, never()).embed(anyString());
        verify(kbChunkRepository, never()).saveAll(any());
        verify(kbChunkRepository, never()).save(any());
    }

    @Test
    void reconcile_preservesContentColumns_evenWhenJsonDiffers() throws Exception {
        // anti-误杀 #3 — content columns are DB-owned under reconcile. The JSON
        // here deliberately carries DIFFERENT content values; reconcile must
        // ignore them and leave the existing row's content byte-identical.
        KbArticle existing = KbArticle.builder()
                .articleId("kaX")
                .title("DB Title")
                .summary("DB Summary")
                .description("DB Body")
                .sourceUrl("https://db/url")
                .urlCategory("db-cat")
                .ucTags(new String[]{"UC-A"})
                .isPublished(true)
                .searchKnowledgeEligible(true)
                .tokenCount(100)
                .version(1)
                .build();
        JsonNode node = json("""
                {
                  "article_id": "kaX",
                  "title": "JSON DIFFERENT Title",
                  "summary": "JSON DIFFERENT Summary",
                  "content_plain": "JSON DIFFERENT Body",
                  "source_url": "https://json/different",
                  "url_category": "json-cat",
                  "uc_tags": ["UC-A"],
                  "search_knowledge_eligible": false,
                  "published_status": true,
                  "token_estimate": 999
                }
                """);

        runner.reconcileExisting(node, Map.of(), existing, OffsetDateTime.now());

        // Only the curation flag moved...
        assertFalse(existing.isSearchKnowledgeEligible());
        // ...every content column is byte-identical to its pre-reconcile value.
        assertEquals("DB Title", existing.getTitle());
        assertEquals("DB Summary", existing.getSummary());
        assertEquals("DB Body", existing.getDescription());
        assertEquals("https://db/url", existing.getSourceUrl());
        assertEquals("db-cat", existing.getUrlCategory());
        assertEquals(100, existing.getTokenCount().intValue(),
                "token_count is a content-derived column and must not be reconciled");
    }

    @Test
    void reconcile_noOpWhenAlreadyInSync_returnsFalse_noSave() throws Exception {
        // The 216 already-in-sync rows: reconcile must be a no-op (no write,
        // counted "unchanged") so the run summary reports articlesReconciled as
        // exactly the rows whose curation actually moved.
        KbArticle existing = dbArticle(true);
        existing.setUcTags(new String[]{"UC-A"});
        existing.setIsPublished(true);
        JsonNode node = json("""
                {
                  "article_id": "kaX",
                  "title": "DB Title",
                  "content_plain": "DB Body",
                  "source_url": "https://db/url",
                  "uc_tags": ["UC-A"],
                  "search_knowledge_eligible": true,
                  "published_status": true,
                  "token_estimate": 100
                }
                """);

        boolean changed = runner.reconcileExisting(node, Map.of(), existing, OffsetDateTime.now());

        assertFalse(changed, "an already-in-sync row must report no change");
        verify(kbArticleRepository, never()).save(any());
    }

    @Test
    void reconcile_newId_takesInsertPath_andEmbeds() throws Exception {
        // anti-误杀 / contract #2 — under --reconcile, an article NOT in the DB
        // still inserts (chunk + embed + save). Empty existingById ⇒ new id.
        wireInsertPath();
        JsonNode node = json("""
                {
                  "article_id": "kaNEW",
                  "title": "Brand New Article",
                  "content_plain": "Fresh body that needs embedding.",
                  "source_url": "https://help.example/new",
                  "uc_tags": ["UC-A"],
                  "search_knowledge_eligible": true,
                  "published_status": true,
                  "token_estimate": 12
                }
                """);

        KnowledgeIngestionRunner.IngestionStats stats =
                runner.processArticles(List.of(node), Map.of(), Map.of(), true);

        assertEquals(1, stats.articlesInsertedNew(), "a new id under --reconcile must insert");
        assertEquals(0, stats.articlesReconciled());
        assertEquals(0, stats.articlesUnchanged());
        verify(embeddingClient, times(1)).embedBatch(anyList());
        verify(kbChunkRepository, times(1)).saveAll(any());
        verify(kbArticleRepository, times(1)).save(any());
    }

    @Test
    void plainIngest_existingRow_skippedAndUntouched_noWriteNoEmbed() throws Exception {
        // anti-误杀 #4 — without --reconcile, an existing row is skipped and
        // untouched EVEN IF the JSON declares a different curation flag. This is
        // the byte-for-byte insert-only behaviour that must be preserved.
        KbArticle existing = dbArticle(true);
        JsonNode node = json("""
                {
                  "article_id": "kaX",
                  "title": "DB Title",
                  "content_plain": "DB Body",
                  "source_url": "https://db/url",
                  "uc_tags": ["UC-A"],
                  "search_knowledge_eligible": false,
                  "published_status": true,
                  "token_estimate": 100
                }
                """);

        KnowledgeIngestionRunner.IngestionStats stats =
                runner.processArticles(List.of(node), Map.of(), Map.of("kaX", existing), false);

        assertEquals(1, stats.articlesUnchanged(), "plain --ingest must skip the existing row");
        assertEquals(0, stats.articlesReconciled());
        assertEquals(0, stats.articlesInsertedNew());
        assertTrue(existing.isSearchKnowledgeEligible(),
                "plain --ingest must NOT apply the JSON flag flip to an existing row");
        verify(kbArticleRepository, never()).save(any());
        verify(embeddingClient, never()).embedBatch(anyList());
        verify(kbChunkRepository, never()).saveAll(any());
    }

    @Test
    void processArticles_reconcile_countsChangedVsUnchangedVsInserted() throws Exception {
        // End-to-end loop counts: one existing-changed (eligibility flip), one
        // existing-unchanged (already in sync), one brand-new (insert). Mirrors
        // the live-DB shape (2 changed / 216 unchanged / 0 new) at small scale.
        wireInsertPath();
        KbArticle changed = dbArticle(true);
        changed.setArticleId("kaCHANGED");
        KbArticle unchanged = dbArticle(true);
        unchanged.setArticleId("kaUNCHANGED");
        unchanged.setUcTags(new String[]{"UC-A"});

        List<JsonNode> articles = List.of(
                json("""
                        {"article_id":"kaCHANGED","title":"DB Title","content_plain":"DB Body",
                         "source_url":"https://db/url","uc_tags":["UC-A"],
                         "search_knowledge_eligible":false,"published_status":true,"token_estimate":100}
                        """),
                json("""
                        {"article_id":"kaUNCHANGED","title":"DB Title","content_plain":"DB Body",
                         "source_url":"https://db/url","uc_tags":["UC-A"],
                         "search_knowledge_eligible":true,"published_status":true,"token_estimate":100}
                        """),
                json("""
                        {"article_id":"kaNEW","title":"New","content_plain":"Fresh body.",
                         "source_url":"https://help.example/new","uc_tags":["UC-A"],
                         "search_knowledge_eligible":true,"published_status":true,"token_estimate":5}
                        """));

        KnowledgeIngestionRunner.IngestionStats stats = runner.processArticles(
                articles, Map.of(),
                Map.of("kaCHANGED", changed, "kaUNCHANGED", unchanged), true);

        assertEquals(1, stats.articlesReconciled(), "only the eligibility-flip row counts as reconciled");
        assertEquals(1, stats.articlesUnchanged(), "the already-in-sync row counts as unchanged");
        assertEquals(1, stats.articlesInsertedNew(), "the brand-new id counts as inserted");
        // Embedding happens for the NEW article only — never for the 2 existing.
        verify(embeddingClient, times(1)).embedBatch(anyList());
    }

    // ---------- helpers ----------

    private JsonNode json(String body) throws Exception {
        return mapper.readTree(body);
    }

    /** A populated-DB row shape with the given eligibility flag and stable content. */
    private static KbArticle dbArticle(boolean eligible) {
        return KbArticle.builder()
                .articleId("kaX")
                .title("DB Title")
                .summary("DB Summary")
                .description("DB Body")
                .sourceUrl("https://db/url")
                .urlCategory("db-cat")
                .ucTags(new String[]{"UC-A"})
                .isPublished(true)
                .searchKnowledgeEligible(eligible)
                .tokenCount(100)
                .version(1)
                .build();
    }

    /** Stub the insert pipeline so a NEW article reaches embedBatch + saveAll. */
    private void wireInsertPath() {
        when(chunkingService.cleanHtml(anyString())).thenReturn("clean body");
        when(chunkingService.chunk(anyString(), anyString()))
                .thenReturn(List.of(new ChunkingService.ChunkRecord("clean body", 0, "heading")));
        when(chunkingService.estimateTokens(anyString())).thenReturn(3);
        when(embeddingClient.embedBatch(anyList())).thenReturn(List.of(new float[768]));
    }
}
