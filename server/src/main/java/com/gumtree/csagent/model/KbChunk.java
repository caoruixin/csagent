package com.gumtree.csagent.model;

import com.gumtree.csagent.config.VectorType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "kb_chunks")
public class KbChunk {

    @Id
    @Column(name = "chunk_id")
    private String chunkId;

    @Column(name = "article_id", nullable = false)
    private String articleId;

    @Column(name = "chunk_index", nullable = false)
    private Integer chunkIndex;

    @Column(name = "chunk_text", nullable = false)
    private String chunkText;

    @Type(VectorType.class)
    @Column(name = "embedding", columnDefinition = "vector(768)", nullable = false)
    private float[] embedding;

    @Column(name = "token_count", nullable = false)
    private Integer tokenCount;

    @Column(name = "section_heading")
    private String sectionHeading;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}
