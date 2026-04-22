package com.gumtree.csagent.eval.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Parsed eval session combining dataset CSV row + its turns + optional HR annotations.
 *
 * Dataset CSV common columns:
 *   Id, CaseId, StartTime, EndTime, Platform, Subject, Reason, CSAT,
 *   total_turns, agent_turns, visitor_turns, primary_uc, all_ucs, uc_confidence,
 *   has_escalation_signal, has_frustration_signal, has_identifier_collection,
 *   escalation_evidence, frustration_evidence, identifier_evidence,
 *   quality_score, tags, eval_tier, (+ dataset-specific trailing columns)
 *
 * HR annotation columns (keyed by session_id):
 *   primary_uc_corrected, outcome_class, should_escalate, escalation_trigger,
 *   expected_tool_sequence, forbidden_tools, answer_must_not_contain,
 *   risk_level, etc.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EvalSession {

    private String id;
    private String caseId;
    private String sourceDataset;

    // Pre-chat form fields
    private String subject;
    private String reason;
    private String platform;
    private String description;

    // Expected classification
    private String primaryUc;
    private String allUcs;
    private String ucConfidence;

    // Signals
    private boolean hasEscalationSignal;
    private boolean hasFrustrationSignal;
    private boolean hasIdentifierCollection;
    private String escalationEvidence;
    private String frustrationEvidence;
    private String identifierEvidence;

    // Quality
    private int qualityScore;
    private String tags;
    private String evalTier;

    // Dataset-specific fields stored generically
    @Builder.Default
    private Map<String, String> extraFields = new HashMap<>();

    // Conversation turns
    @Builder.Default
    private List<EvalTurn> turns = new ArrayList<>();

    // --- Human Review Annotation overlay ---
    private String hrPrimaryUcCorrected;
    private String hrOutcomeClass;
    private String hrOutcomeReasoning;
    private boolean hrShouldEscalate;
    private String hrEscalationTrigger;
    private String hrEscalationTurn;
    private String hrExpectedToolSequence;
    private String hrForbiddenTools;
    private String hrAnswerMustNotContain;
    private String hrRiskLevel;
    private String hrDriftType;
    private String hrDriftHandling;
    private boolean hrHasFrustration;
    private String hrFrustrationType;
    private String hrGroundingSource;
    private String hrExpectedKnowledgeScope;
    private String hrTranscriptSummary;
    private boolean hrAnnotated;

    /**
     * Get the effective primary UC (HR-corrected if available, else source).
     */
    public String getEffectivePrimaryUc() {
        if (hrAnnotated && hrPrimaryUcCorrected != null && !hrPrimaryUcCorrected.isBlank()) {
            return hrPrimaryUcCorrected;
        }
        return primaryUc;
    }

    /**
     * Get the effective expected outcome (HR if available).
     */
    public String getEffectiveOutcomeClass() {
        if (hrAnnotated && hrOutcomeClass != null && !hrOutcomeClass.isBlank()) {
            return hrOutcomeClass;
        }
        return hasEscalationSignal ? "escalate" : "resolve";
    }

    /**
     * Get the pre-chat form turn if present.
     */
    public EvalTurn getPreChatFormTurn() {
        return turns.stream()
                .filter(EvalTurn::isPreChatForm)
                .findFirst()
                .orElse(null);
    }

    /**
     * Get only visitor turns (excluding pre-chat form).
     */
    public List<EvalTurn> getVisitorTurns() {
        return turns.stream()
                .filter(t -> t.isVisitor() && !t.isPreChatForm())
                .toList();
    }
}
