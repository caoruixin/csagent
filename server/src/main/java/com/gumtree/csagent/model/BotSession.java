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

    /**
     * Sprint 31 — intake-time snapshot of the alternate use cases the
     * {@link com.gumtree.csagent.service.runtime.UseCaseRouter}
     * considered plausible for the session's topic-subject family
     * when {@code RoutingResult.AMBIGUOUS} fired at session creation.
     * Captured by {@code SessionManager.createSession}'s AMBIGUOUS
     * branch; null on the ROUTED / OUT_OF_SCOPE paths. Surfaced by
     * {@code ContextProjectionBuilder} as the
     * {@code alternate_candidate_use_cases} per-turn projection slot
     * (minus the active UC). Soft signal; the runtime does NOT branch
     * on this value.
     */
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "intake_ambiguous_candidates", columnDefinition = "text[]")
    private String[] intakeAmbiguousCandidates;

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

    @Column(name = "consecutive_deadline_count", nullable = false)
    @Builder.Default
    private Integer consecutiveDeadlineCount = 0;

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

    /**
     * Sprint 071 / S-Auto-15 (M-Auto-3, workstream A) — PERSISTED
     * cross-turn search_knowledge re-search suppression state. Carried
     * across turns (NOT {@code @Transient}) because
     * {@code SessionManager.processMessage} reloads the BotSession from the
     * DB every turn, so the within-turn A3 per-run tracker cannot see a
     * paraphrase the LLM issues in a SUBSEQUENT bot turn. These three
     * columns let a NEW, SEPARATE, fail-open, drift-aware cross-turn gate
     * in {@code AgentRunLoopImpl.run} suppress only the cross-turn storm
     * beyond the first allowed refinement, ALONGSIDE — not modifying — the
     * within-turn A3 gate and the A1 dedup cache.
     *
     * <p>{@link #crossTurnFaqHitUseCase}: the {@code active_use_case} at
     * which a standing {@code search_knowledge} viable hit
     * ({@code faq_miss=false}) was captured. Invariant condition 1 compares
     * it to the current {@code active_use_case}. Null = no standing hit =
     * gate disabled (fail-open).
     */
    @Column(name = "cross_turn_faq_hit_use_case")
    private String crossTurnFaqHitUseCase;

    /**
     * The serialized {@code search_knowledge} result payload (JSON) of the
     * standing viable hit, served back to the LLM on suppression so a
     * suppressed cross-turn re-search still sees the prior viable hit under
     * {@code accumulated_tool_results}. Null = no standing payload = gate
     * disabled.
     */
    @Column(name = "cross_turn_faq_hit_payload", columnDefinition = "jsonb")
    private String crossTurnFaqHitPayload;

    /**
     * Cardinality budget counter: how many cross-turn
     * {@code search_knowledge} calls for the standing-hit UC have ALREADY
     * been allowed since the standing hit was captured. Budget FIXED at 1 —
     * the FIRST post-hit cross-turn re-search (the legitimate refinement) is
     * always allowed (counter -> 1); only the 2nd+ (counter &gt;= 1) is
     * eligible for suppression. Reset to 0 when a fresh standing hit is
     * captured and on every reset event (UC change, drift, genuine miss,
     * resolution, escalation, record_outcome).
     */
    @Column(name = "cross_turn_search_allowed_since_hit", nullable = false)
    @Builder.Default
    private Integer crossTurnSearchAllowedSinceHit = 0;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "articles_shown", columnDefinition = "text[]")
    private String[] articlesShown;

    @Column(name = "intake_fields", columnDefinition = "jsonb")
    private String intakeFields;

    @Column(name = "containment_outcome")
    private String containmentOutcome;

    /**
     * Sprint 077 / S-Auto-22 (OQ-S77 #4 — runtime trace-contract honesty):
     * provenance for a downgraded {@code containment_outcome}. When a later
     * turn reaches a runtime-observable unresolved failure terminal (ERROR /
     * DEADLINE_EXCEEDED / LLM_UNAVAILABLE / MAX_STEPS) AFTER an earlier turn
     * stamped {@code "resolved"}, {@link
     * com.gumtree.csagent.service.runtime.ControlKernel#shouldVoidResolvedStamp}
     * fires and {@code containment_outcome} is overwritten to
     * {@code "incomplete_after_partial_answer"}; the prior {@code "resolved"}
     * value is preserved here so the trace records that an earlier turn DID
     * stamp success before the session failed. {@code @Transient}: the durable
     * provenance lives on the emitted {@code SESSION_CLOSED} event; this field
     * is the in-process / same-turn record (no DB migration). Null when no
     * downgrade fired.
     */
    @Transient
    private String priorContainmentOutcome;

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

    /**
     * Sprint 12 §N0 — runtime alignment observability slots (transient).
     *
     * <p>Populated each turn so the projection / trace evidence captures
     * the latest reroute + progressive-resolve decision. They are
     * back-compat additions: existing fields are preserved verbatim, and
     * downstream code that ignores these slots continues to work.
     *
     * <p>The slots answer the audit questions enumerated in the Sprint 12
     * objective (why did the bot stay in current UC / soft-shift /
     * risk-shift / stay RESOLVE / allow or reject record_outcome).
     */
    /** Latest classifier {@code predicted_use_case}. */
    @Transient
    private String predictedUseCase;

    /** Latest classifier {@code IntentRelation} token (string form). */
    @Transient
    private String intentRelation;

    /** Latest decider {@code RerouteAction} token (string form). */
    @Transient
    private String rerouteAction;

    /**
     * Latest {@code transition_reason} stamped during the turn — set by
     * {@link com.gumtree.csagent.service.runtime.ControlKernel} on a reroute
     * decision and updated by {@code PhaseEvaluator.interpretRunResult}
     * when the phase transitions. Surfaces the canonical reason a
     * reviewer reads to answer "why did this turn end here".
     */
    @Transient
    private String phaseTransitionReason;

    /**
     * Latest {@link ResolveDisposition} token (string form) — set by
     * {@code ResolveDispositionEvaluator} via
     * {@code PhaseEvaluator.mapFinalAnswer} on RESOLVE / FAQ plans.
     */
    @Transient
    private String resolveDisposition;

    /**
     * Sprint 12 §N0 — terminal evidence summary for the current turn's
     * agent run. Populated by
     * {@link com.gumtree.csagent.service.runtime.AgentRunLoopImpl} after
     * the loop completes. {@code true / false / null} flags answer the
     * "why did the bot stay RESOLVE instead of CONFIRM" audit question
     * in one place.
     */
    @Transient
    private Boolean recordOutcomeAttempted;

    @Transient
    private Boolean recordOutcomeSucceeded;

    /**
     * Sprint 12 §N0 — record-outcome guard outcome for the current turn.
     * One of {@code none / allowed / rejected:<reason>}. Populated by
     * {@link com.gumtree.csagent.service.runtime.AgentRunLoopImpl} when
     * the §M1 guard runs.
     */
    @Transient
    private String recordOutcomeGuardResult;

    /**
     * Sprint 14 §L1 — turn-level source evidence lineage (transient).
     *
     * <p>Splits the historically-overloaded {@code sourceIds} concept into
     * three observably-distinct slots:
     *
     * <ul>
     *   <li>{@link #retrievedSourceIds} — IDs returned by every successful
     *       {@code search_knowledge} call this turn.</li>
     *   <li>{@link #resolvedSourceIds} — IDs that successfully passed
     *       through {@code resolve_article} (and survived the §L0
     *       published-safety guard).</li>
     *   <li>{@link #citedSourceIds} — IDs detected in the customer-visible
     *       reply by the passive {@code CitationExtractor}.</li>
     * </ul>
     *
     * <p>Populated by {@code ControlKernel.recordRunResult}. Sprint 14 §L1
     * explicitly does NOT use these fields to gate the response — they
     * are observability material consumed by trace surfaces and the §L2
     * grounding diagnostics.
     */
    @Transient
    private String[] retrievedSourceIds;

    @Transient
    private String[] resolvedSourceIds;

    @Transient
    private String[] citedSourceIds;

    @Transient
    private String[] citedCanonicalUrls;

    /**
     * Sprint 14 §L2 — soft FAQ grounding diagnostics (transient).
     *
     * <p>{@link #faqGroundingState} carries a coarse-grained string token
     * describing the grounding state of the current turn (one of
     * {@code factual_grounded}, {@code factual_uncited},
     * {@code factual_unresolved}, {@code factual_unretrieved},
     * {@code non_factual}, {@code unknown}).
     *
     * <p>The remaining boolean slots are independently observable: a
     * reviewer can answer "did the bot cite anything", "did the cited
     * source come from the resolved set", "did the bot drift from the
     * retrieved evidence", "are there resolved-but-uncited articles", and
     * "are there retrieved-but-unresolved articles" without re-deriving
     * any of them from the others.
     *
     * <p>All five fields are observability-only — Sprint 14 §L2 does not
     * gate the response on any of them.
     */
    @Transient
    private String faqGroundingState;

    @Transient
    private Boolean citationPresent;

    @Transient
    private Boolean citationMatch;

    @Transient
    private Boolean citationDrift;

    @Transient
    private Boolean resolvedButUncited;

    @Transient
    private Boolean retrievedButUnresolved;

    /**
     * Sprint 14 §L2 — bot output classification token. One of
     * {@code factual_answer}, {@code clarification}, {@code empathy_ack},
     * {@code handover}, {@code tool_status}, {@code intake_collection}.
     * Populated by {@code FaqOutputClassifier}; surfaced in trace
     * evidence so a reviewer can see WHY a turn was (or was not)
     * subject to factual-grounding observability.
     */
    @Transient
    private String faqOutputClass;
}
