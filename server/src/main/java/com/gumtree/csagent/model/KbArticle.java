package com.gumtree.csagent.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "kb_articles")
public class KbArticle {

    @Id
    @Column(name = "article_id")
    private String articleId;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "summary")
    private String summary;

    @Column(name = "description")
    private String description;

    @Column(name = "source_url")
    private String sourceUrl;

    @Column(name = "url_category")
    private String urlCategory;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "uc_tags", columnDefinition = "text[]")
    private String[] ucTags;

    @Column(name = "is_published", nullable = false)
    @Builder.Default
    private Boolean isPublished = true;

    /**
     * R6 (Sub-sprint C-2b) — corpus-curation flag controlling whether this
     * article may surface on the LLM-facing {@code search_knowledge} retrieval
     * surface. Default {@code true} (entity + DB) so unflagged articles stay
     * visible. {@code false} hides the article from search only; direct
     * {@code resolve_article} by id is unaffected (human-CS access preserved).
     */
    @Column(name = "search_knowledge_eligible", nullable = false)
    @Builder.Default
    private boolean searchKnowledgeEligible = true;

    @Column(name = "token_count")
    private Integer tokenCount;

    @Column(name = "version", nullable = false)
    @Builder.Default
    private Integer version = 1;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
