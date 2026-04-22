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
}
