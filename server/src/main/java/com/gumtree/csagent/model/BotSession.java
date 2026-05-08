package com.gumtree.csagent.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "bot_sessions")
public class BotSession {

    @Id
    @Column(name = "session_id")
    private String sessionId;

    @Column(name = "sf_bot_session_id")
    private String sfBotSessionId;

    @Column(name = "case_id")
    private String caseId;

    @Column(name = "traffic_variant", nullable = false)
    @Builder.Default
    private String trafficVariant = "bot_v1";

    @Column(name = "handling_state", nullable = false)
    @Builder.Default
    private String handlingState = "BOT_HANDLING";

    @Column(name = "current_phase", nullable = false)
    @Builder.Default
    private String currentPhase = "INIT";

    @Column(name = "active_use_case")
    private String activeUseCase;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "candidate_use_cases", columnDefinition = "text[]")
    private String[] candidateUseCases;

    @Column(name = "intent_confidence", precision = 4, scale = 2)
    private BigDecimal intentConfidence;

    @Column(name = "total_bot_turns", nullable = false)
    @Builder.Default
    private Integer totalBotTurns = 0;

    @Column(name = "clarification_count", nullable = false)
    @Builder.Default
    private Integer clarificationCount = 0;

    @Column(name = "faq_miss_count", nullable = false)
    @Builder.Default
    private Integer faqMissCount = 0;

    @Column(name = "repeated_action_count", nullable = false)
    @Builder.Default
    private Integer repeatedActionCount = 0;

    @Column(name = "runtime_error_count", nullable = false)
    @Builder.Default
    private Integer runtimeErrorCount = 0;

    @Column(name = "last_action")
    private String lastAction;

    @Column(name = "form_topic_subject")
    private String formTopicSubject;

    @Column(name = "form_context", columnDefinition = "jsonb")
    private String formContext;

    @Column(name = "customer_context", columnDefinition = "jsonb")
    private String customerContext;

    @Column(name = "listing_context", columnDefinition = "jsonb")
    private String listingContext;

    @Column(name = "moderation_context", columnDefinition = "jsonb")
    private String moderationContext;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "articles_shown", columnDefinition = "text[]")
    private String[] articlesShown;

    @Column(name = "intake_fields", columnDefinition = "jsonb")
    private String intakeFields;

    @Column(name = "containment_outcome")
    private String containmentOutcome;

    @Column(name = "escalation_reason")
    private String escalationReason;

    @Column(name = "prompt_version", nullable = false)
    @Builder.Default
    private String promptVersion = "v1.0.0";

    @Column(name = "model_version", nullable = false)
    @Builder.Default
    private String modelVersion = "local";

    @Column(name = "projection_version", nullable = false)
    @Builder.Default
    private String projectionVersion = "v1.0.0";

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "closed_at")
    private OffsetDateTime closedAt;

    /**
     * Sprint 10 §L2 — minimal projected issue-state (transient).
     *
     * <p>Populated by {@code ControlKernel.processMessage} from the
     * latest {@link RerouteDecision} so the next
     * {@code ContextProjectionBuilder.buildProjection(...)} call can
     * surface {@code previous_active_use_case}, {@code drift_type},
     * {@code current_task_type}, {@code primary_entity}, and
     * {@code issue_status_summary}. Not persisted: only meaningful
     * for the current turn.
     */
    @Transient
    private String previousActiveUseCase;

    @Transient
    private String driftType;

    @Transient
    private String currentTaskType;

    @Transient
    private String primaryEntityType;

    @Transient
    private String primaryEntityValue;

    @Transient
    @Builder.Default
    private String issueStatusSummary = "open";

    /**
     * Sprint 11 §M0 — minimal same-UC task state for progressive resolve
     * (transient). Populated each turn by {@code ControlKernel.applyRerouteDecision}
     * from the latest {@link com.gumtree.csagent.model.ResolveDisposition}.
     *
     * <p>Stable token reflecting where the current task sits in the
     * progressive-resolve lifecycle. One of:
     * <ul>
     *   <li>{@code in_progress} — task started, no terminal signal yet</li>
     *   <li>{@code asked_for_slot} — bot asked the user for a missing slot
     *       such as {@code ad_id}</li>
     *   <li>{@code answered_subtask} — bot delivered a soft / partial answer
     *       and is awaiting confirmation or follow-up</li>
     *   <li>{@code ready_to_confirm} — deterministic terminal condition met</li>
     *   <li>{@code escalate} — task should escalate</li>
     * </ul>
     */
    @Transient
    private String taskStatus;

    /**
     * Sprint 11 §M0 — optional pointer to the last known entity context
     * reference (e.g. {@code listing_context_ad_id}, {@code session.ad_id}).
     * Surfaced in the projection so the LLM avoids re-asking known
     * information across same-UC follow-up turns.
     */
    @Transient
    private String lastEntityContextRef;
}
