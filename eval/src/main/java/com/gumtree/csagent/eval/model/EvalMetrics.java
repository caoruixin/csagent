package com.gumtree.csagent.eval.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Aggregated metrics across all evaluated sessions.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EvalMetrics {

    private String suiteName;
    private Instant evaluatedAt;

    private int totalSessions;
    private int successfulReplays;
    private int failedReplays;

    // Routing
    private double routingAccuracy;
    private int routingCorrect;
    private int routingTotal;

    // Escalation
    private double escalationRecall;
    private double escalationPrecision;
    private double escalationF1;

    // Handover
    private double handoverCompleteness;

    // Policy
    private int forbiddenPhraseViolations;
    private int piiLeakageViolations;
    private int criticalPolicyViolations;

    // Control
    private double budgetEnforcementRate;
    private double phaseTransitionValidity;

    // Tool contract
    private int toolScopeViolations;

    // Drift
    private double driftDetectionAccuracy;

    // Model-based
    private double groundednessPassRate;
    private double avgRelevanceScore;
    private double avgSummaryQualityScore;

    // Containment
    private double containmentRate;
    private double wrongContainmentRate;

    // Out of scope
    private double outOfScopeDetectionRate;

    // Critical high risk
    private double criticalHighRiskEscalationRate;

    // Per-UC breakdown
    @Builder.Default
    private Map<String, Map<String, Double>> perUcMetrics = new HashMap<>();

    // Per-dataset breakdown
    @Builder.Default
    private Map<String, Map<String, Double>> perDatasetMetrics = new HashMap<>();

    // Timing
    private double avgReplayDurationMs;
    private long totalEvalDurationMs;
}
