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
@Table(name = "bot_turns")
public class BotTurn {

    @Id
    @Column(name = "turn_id")
    private String turnId;

    @Column(name = "session_id", nullable = false)
    private String sessionId;

    @Column(name = "turn_index", nullable = false)
    private Integer turnIndex;

    @Column(name = "user_message")
    private String userMessage;

    @Column(name = "projected_context", columnDefinition = "jsonb")
    private String projectedContext;

    @Column(name = "llm_raw_response")
    private String llmRawResponse;

    @Column(name = "tool_calls", columnDefinition = "jsonb")
    private String toolCalls;

    @Column(name = "bot_response")
    private String botResponse;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "source_ids", columnDefinition = "text[]")
    private String[] sourceIds;

    @Column(name = "phase_before")
    private String phaseBefore;

    @Column(name = "phase_after")
    private String phaseAfter;

    @Column(name = "active_use_case")
    private String activeUseCase;

    @Column(name = "latency_ms")
    private Integer latencyMs;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}
