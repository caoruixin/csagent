package com.gumtree.csagent.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "mock_handover_log")
public class MockHandoverLog {

    @Id
    @Column(name = "log_id")
    private String logId;

    @Column(name = "session_id")
    private String sessionId;

    @Column(name = "handover_payload", columnDefinition = "jsonb", nullable = false)
    private String handoverPayload;

    @Column(name = "transfer_result", nullable = false)
    private String transferResult;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}
