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
@Table(name = "session_outcomes")
public class SessionOutcome {

    @Id
    @Column(name = "session_id")
    private String sessionId;

    @Column(name = "outcome", nullable = false)
    private String outcome;

    @Column(name = "use_case_id")
    private String useCaseId;

    @Column(name = "escalation_reason")
    private String escalationReason;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "articles_shown", columnDefinition = "text[]")
    private String[] articlesShown;

    @Column(name = "total_turns")
    private Integer totalTurns;

    @Column(name = "total_latency_ms")
    private Long totalLatencyMs;

    @Column(name = "trace_metadata", columnDefinition = "jsonb")
    private String traceMetadata;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}
