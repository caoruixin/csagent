package com.gumtree.csagent.repository;

import com.gumtree.csagent.model.KbChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface KbChunkRepository extends JpaRepository<KbChunk, String> {

    List<KbChunk> findByArticleIdOrderByChunkIndex(String articleId);

    @Query(value = "SELECT * FROM kb_chunks ORDER BY embedding <=> cast(:embedding as vector) LIMIT :limit",
           nativeQuery = true)
    List<KbChunk> findNearestByEmbedding(@Param("embedding") String embedding,
                                          @Param("limit") int limit);

    @Query(value = "SELECT c.* FROM kb_chunks c " +
                   "JOIN kb_articles a ON c.article_id = a.article_id " +
                   "WHERE a.uc_tags && cast(:ucTags as text[]) " +
                   "ORDER BY c.embedding <=> cast(:embedding as vector) " +
                   "LIMIT :limit",
           nativeQuery = true)
    List<KbChunk> findNearestByEmbeddingWithUcTags(@Param("embedding") String embedding,
                                                     @Param("ucTags") String[] ucTags,
                                                     @Param("limit") int limit);

    /**
     * Sprint 14 §L0 — published-safety filter on the no-UC-tag pgvector ANN
     * path. Mirrors {@link #findNearestByEmbedding} but joins
     * {@code kb_articles} so unpublished articles are excluded before they
     * can become a search-knowledge hit.
     */
    @Query(value = "SELECT c.* FROM kb_chunks c " +
                   "JOIN kb_articles a ON c.article_id = a.article_id " +
                   "WHERE a.is_published = true " +
                   "ORDER BY c.embedding <=> cast(:embedding as vector) " +
                   "LIMIT :limit",
           nativeQuery = true)
    List<KbChunk> findNearestByEmbeddingPublishedOnly(@Param("embedding") String embedding,
                                                       @Param("limit") int limit);

    /**
     * Sprint 14 §L0 — published-safety filter on the UC-tag-scoped pgvector
     * ANN path. Mirrors {@link #findNearestByEmbeddingWithUcTags} with an
     * added {@code is_published = true} predicate so the search-knowledge
     * surface never returns draft / unpublished / safety-suspended articles.
     */
    @Query(value = "SELECT c.* FROM kb_chunks c " +
                   "JOIN kb_articles a ON c.article_id = a.article_id " +
                   "WHERE a.is_published = true AND a.uc_tags && cast(:ucTags as text[]) " +
                   "ORDER BY c.embedding <=> cast(:embedding as vector) " +
                   "LIMIT :limit",
           nativeQuery = true)
    List<KbChunk> findNearestByEmbeddingWithUcTagsPublishedOnly(@Param("embedding") String embedding,
                                                                  @Param("ucTags") String[] ucTags,
                                                                  @Param("limit") int limit);
}
