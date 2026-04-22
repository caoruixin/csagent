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
@Table(name = "mock_cases")
public class MockCase {

    @Id
    @Column(name = "case_id")
    private String caseId;

    @Column(name = "session_id")
    private String sessionId;

    @Column(name = "use_case_id", nullable = false)
    private String useCaseId;

    @Column(name = "subject")
    private String subject;

    @Column(name = "description")
    private String description;

    @Column(name = "contact_email")
    private String contactEmail;

    @Column(name = "ad_id")
    private String adId;

    @Column(name = "status", nullable = false)
    @Builder.Default
    private String status = "New";

    @Column(name = "queue_name")
    private String queueName;

    @Column(name = "intake_fields", columnDefinition = "jsonb")
    private String intakeFields;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
