package com.gumtree.csagent.eval.metrics;

import com.gumtree.csagent.eval.model.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Aggregates per-session grading results into overall metrics.
 */
@Slf4j
@Component
public class MetricsAggregator {

    public EvalMetrics aggregate(String suiteName, List<SessionResult> results, long totalDurationMs) {
        EvalMetrics metrics = EvalMetrics.builder()
                .suiteName(suiteName)
                .evaluatedAt(Instant.now())
                .totalSessions(results.size())
                .successfulReplays((int) results.stream().filter(SessionResult::isReplaySuccess).count())
                .failedReplays((int) results.stream().filter(r -> !r.isReplaySuccess()).count())
                .totalEvalDurationMs(totalDurationMs)
                .build();

        List<SessionResult> successful = results.stream()
                .filter(SessionResult::isReplaySuccess)
                .toList();

        if (successful.isEmpty()) {
            log.warn("No successful replays to aggregate metrics from");
            return metrics;
        }

        // Routing accuracy
        computeRoutingMetrics(metrics, successful);

        // Escalation metrics
        computeEscalationMetrics(metrics, successful);

        // Handover completeness
        computeHandoverMetrics(metrics, successful);

        // Policy metrics
        computePolicyMetrics(metrics, successful);

        // Control metrics
        computeControlMetrics(metrics, successful);

        // Tool contract metrics
        computeToolContractMetrics(metrics, successful);

        // Drift detection metrics
        computeDriftMetrics(metrics, successful);

        // Model-based metrics
        computeModelMetrics(metrics, successful);

        // Containment metrics
        computeContainmentMetrics(metrics, successful);

        // Timing
        metrics.setAvgReplayDurationMs(successful.stream()
                .mapToLong(SessionResult::getReplayDurationMs)
                .average()
                .orElse(0));

        // Per-UC and per-dataset breakdowns
        computePerUcBreakdown(metrics, successful);
        computePerDatasetBreakdown(metrics, successful);

        return metrics;
    }

    private void computeRoutingMetrics(EvalMetrics metrics, List<SessionResult> results) {
        List<GradeResult> routingGrades = extractGrades(results, "routing");
        List<GradeResult> evaluated = routingGrades.stream().filter(g -> !g.isSkipped()).toList();
        if (!evaluated.isEmpty()) {
            long correct = evaluated.stream().filter(GradeResult::isPassed).count();
            metrics.setRoutingCorrect((int) correct);
            metrics.setRoutingTotal(evaluated.size());
            metrics.setRoutingAccuracy((double) correct / evaluated.size());
        }
    }

    private void computeEscalationMetrics(EvalMetrics metrics, List<SessionResult> results) {
        List<GradeResult> escGrades = extractGrades(results, "escalation");
        List<GradeResult> evaluated = escGrades.stream().filter(g -> !g.isSkipped()).toList();

        long tp = 0, fp = 0, fn = 0, tn = 0;
        for (GradeResult g : evaluated) {
            String type = (String) g.getMetadata().getOrDefault("type", "");
            switch (type) {
                case "TP" -> tp++;
                case "FP" -> fp++;
                case "FN" -> fn++;
                case "TN" -> tn++;
            }
        }

        metrics.setEscalationRecall(tp + fn > 0 ? (double) tp / (tp + fn) : 1.0);
        metrics.setEscalationPrecision(tp + fp > 0 ? (double) tp / (tp + fp) : 1.0);
        double recall = metrics.getEscalationRecall();
        double precision = metrics.getEscalationPrecision();
        metrics.setEscalationF1(recall + precision > 0
                ? 2 * recall * precision / (recall + precision) : 0);
    }

    private void computeHandoverMetrics(EvalMetrics metrics, List<SessionResult> results) {
        List<GradeResult> grades = extractGrades(results, "handover_completeness");
        List<GradeResult> evaluated = grades.stream().filter(g -> !g.isSkipped()).toList();
        if (!evaluated.isEmpty()) {
            metrics.setHandoverCompleteness(evaluated.stream()
                    .mapToDouble(GradeResult::getScore)
                    .average()
                    .orElse(0));
        } else {
            metrics.setHandoverCompleteness(1.0);
        }
    }

    private void computePolicyMetrics(EvalMetrics metrics, List<SessionResult> results) {
        List<GradeResult> grades = extractGrades(results, "policy");
        List<GradeResult> evaluated = grades.stream().filter(g -> !g.isSkipped()).toList();

        int forbiddenCount = 0;
        int piiCount = 0;
        for (GradeResult g : evaluated) {
            Object fp = g.getMetadata().get("forbidden_phrase_count");
            Object pi = g.getMetadata().get("pii_leakage_count");
            if (fp instanceof Number n) forbiddenCount += n.intValue();
            if (pi instanceof Number n) piiCount += n.intValue();
        }
        metrics.setForbiddenPhraseViolations(forbiddenCount);
        metrics.setPiiLeakageViolations(piiCount);
        metrics.setCriticalPolicyViolations(forbiddenCount + piiCount);
    }

    private void computeControlMetrics(EvalMetrics metrics, List<SessionResult> results) {
        List<GradeResult> grades = extractGrades(results, "control");
        List<GradeResult> evaluated = grades.stream().filter(g -> !g.isSkipped()).toList();

        if (!evaluated.isEmpty()) {
            long budgetOk = evaluated.stream()
                    .filter(g -> Boolean.TRUE.equals(g.getMetadata().get("budget_ok")))
                    .count();
            long phaseOk = evaluated.stream()
                    .filter(g -> Boolean.TRUE.equals(g.getMetadata().get("phase_valid")))
                    .count();
            metrics.setBudgetEnforcementRate((double) budgetOk / evaluated.size());
            metrics.setPhaseTransitionValidity((double) phaseOk / evaluated.size());
        } else {
            metrics.setBudgetEnforcementRate(1.0);
            metrics.setPhaseTransitionValidity(1.0);
        }
    }

    private void computeToolContractMetrics(EvalMetrics metrics, List<SessionResult> results) {
        List<GradeResult> grades = extractGrades(results, "tool_contract");
        List<GradeResult> evaluated = grades.stream().filter(g -> !g.isSkipped()).toList();
        long violations = evaluated.stream().filter(g -> !g.isPassed()).count();
        metrics.setToolScopeViolations((int) violations);
    }

    private void computeDriftMetrics(EvalMetrics metrics, List<SessionResult> results) {
        List<GradeResult> grades = extractGrades(results, "drift_detection");
        List<GradeResult> evaluated = grades.stream().filter(g -> !g.isSkipped()).toList();
        if (!evaluated.isEmpty()) {
            long correct = evaluated.stream().filter(GradeResult::isPassed).count();
            metrics.setDriftDetectionAccuracy((double) correct / evaluated.size());
        }
    }

    private void computeModelMetrics(EvalMetrics metrics, List<SessionResult> results) {
        // Groundedness
        List<GradeResult> groundednessGrades = extractGrades(results, "groundedness");
        List<GradeResult> gEval = groundednessGrades.stream()
                .filter(g -> !g.isSkipped() && !g.isError()).toList();
        if (!gEval.isEmpty()) {
            long passed = gEval.stream().filter(GradeResult::isPassed).count();
            metrics.setGroundednessPassRate((double) passed / gEval.size());
        } else {
            metrics.setGroundednessPassRate(1.0);
        }

        // Relevance
        List<GradeResult> relevanceGrades = extractGrades(results, "relevance");
        List<GradeResult> rEval = relevanceGrades.stream()
                .filter(g -> !g.isSkipped() && !g.isError()).toList();
        if (!rEval.isEmpty()) {
            metrics.setAvgRelevanceScore(rEval.stream()
                    .mapToDouble(g -> (double) g.getMetadata().getOrDefault("raw_score", 3.0))
                    .average()
                    .orElse(0));
        }

        // Summary quality
        List<GradeResult> summaryGrades = extractGrades(results, "summary_quality");
        List<GradeResult> sEval = summaryGrades.stream()
                .filter(g -> !g.isSkipped() && !g.isError()).toList();
        if (!sEval.isEmpty()) {
            metrics.setAvgSummaryQualityScore(sEval.stream()
                    .mapToDouble(g -> (double) g.getMetadata().getOrDefault("raw_score", 3.0))
                    .average()
                    .orElse(0));
        }
    }

    private void computeContainmentMetrics(EvalMetrics metrics, List<SessionResult> results) {
        // Containment = sessions where bot resolved without escalation
        long resolved = results.stream()
                .filter(r -> !"ESCALATED".equalsIgnoreCase(r.getBotHandlingState()))
                .count();
        metrics.setContainmentRate((double) resolved / results.size());

        // Wrong containment = resolved when should have escalated
        List<GradeResult> escGrades = extractGrades(results, "escalation");
        long wrongContainment = escGrades.stream()
                .filter(g -> !g.isSkipped() && "FN".equals(g.getMetadata().get("type")))
                .count();
        metrics.setWrongContainmentRate(results.isEmpty() ? 0 : (double) wrongContainment / results.size());

        // Critical high risk escalation
        long criticalHighRisk = 0;
        long criticalHighRiskTotal = 0;
        for (int i = 0; i < results.size(); i++) {
            SessionResult r = results.get(i);
            // Check if session is critical/high risk (from HR annotation or escalation grader metadata)
            GradeResult escGrade = r.getGradeByName("escalation");
            if (escGrade != null && !escGrade.isSkipped()) {
                boolean expectedEscalation = (boolean) escGrade.getMetadata().getOrDefault("expected_escalation", false);
                if (expectedEscalation) {
                    criticalHighRiskTotal++;
                    boolean actualEscalation = (boolean) escGrade.getMetadata().getOrDefault("actual_escalation", false);
                    if (actualEscalation) criticalHighRisk++;
                }
            }
        }
        metrics.setCriticalHighRiskEscalationRate(
                criticalHighRiskTotal > 0 ? (double) criticalHighRisk / criticalHighRiskTotal : 1.0);

        // Out of scope detection (use routing grader for non-matching UCs)
        metrics.setOutOfScopeDetectionRate(metrics.getRoutingAccuracy());
    }

    private void computePerUcBreakdown(EvalMetrics metrics, List<SessionResult> results) {
        Map<String, List<SessionResult>> byUc = results.stream()
                .filter(r -> r.getExpectedUc() != null && !r.getExpectedUc().isBlank())
                .collect(Collectors.groupingBy(SessionResult::getExpectedUc));

        Map<String, Map<String, Double>> perUc = new HashMap<>();
        for (Map.Entry<String, List<SessionResult>> entry : byUc.entrySet()) {
            Map<String, Double> ucMetrics = new HashMap<>();
            List<SessionResult> ucResults = entry.getValue();
            ucMetrics.put("total_sessions", (double) ucResults.size());

            List<GradeResult> routing = extractGrades(ucResults, "routing").stream()
                    .filter(g -> !g.isSkipped()).toList();
            if (!routing.isEmpty()) {
                ucMetrics.put("routing_accuracy",
                        (double) routing.stream().filter(GradeResult::isPassed).count() / routing.size());
            }

            perUc.put(entry.getKey(), ucMetrics);
        }
        metrics.setPerUcMetrics(perUc);
    }

    private void computePerDatasetBreakdown(EvalMetrics metrics, List<SessionResult> results) {
        Map<String, List<SessionResult>> byDs = results.stream()
                .filter(r -> r.getSourceDataset() != null)
                .collect(Collectors.groupingBy(SessionResult::getSourceDataset));

        Map<String, Map<String, Double>> perDs = new HashMap<>();
        for (Map.Entry<String, List<SessionResult>> entry : byDs.entrySet()) {
            Map<String, Double> dsMetrics = new HashMap<>();
            List<SessionResult> dsResults = entry.getValue();
            dsMetrics.put("total_sessions", (double) dsResults.size());
            dsMetrics.put("pass_rate", (double) dsResults.stream().filter(SessionResult::allPassed).count() / dsResults.size());
            perDs.put(entry.getKey(), dsMetrics);
        }
        metrics.setPerDatasetMetrics(perDs);
    }

    private List<GradeResult> extractGrades(List<SessionResult> results, String graderName) {
        return results.stream()
                .map(r -> r.getGradeByName(graderName))
                .filter(Objects::nonNull)
                .toList();
    }
}
