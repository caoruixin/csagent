package com.gumtree.csagent.eval.metrics;

import com.gumtree.csagent.eval.config.EvalConfig;
import com.gumtree.csagent.eval.model.EvalMetrics;
import com.gumtree.csagent.eval.model.GateResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Evaluates quality gates against computed metrics.
 * All gates defined in EvalConfig.HARD_GATES are hard gates (must pass for release).
 */
@Slf4j
@Component
public class GateEvaluator {

    public GateResult.GateSummary evaluate(EvalMetrics metrics) {
        GateResult.GateSummary summary = GateResult.GateSummary.builder().build();

        for (Map.Entry<String, EvalConfig.GateThreshold> entry : EvalConfig.HARD_GATES.entrySet()) {
            String gateName = entry.getKey();
            EvalConfig.GateThreshold threshold = entry.getValue();

            double actual = resolveMetricValue(gateName, metrics);
            boolean passed = threshold.evaluate(actual);

            GateResult gate = GateResult.builder()
                    .gateName(gateName)
                    .description(threshold.getDescription())
                    .hard(true)
                    .passed(passed)
                    .actualValue(actual)
                    .threshold(threshold.getThreshold())
                    .operator(threshold.getOperator())
                    .detail(String.format("%s: actual=%.4f %s %.4f -> %s",
                            gateName, actual, threshold.getOperator(), threshold.getThreshold(),
                            passed ? "PASS" : "FAIL"))
                    .build();

            summary.addGate(gate);

            if (!passed) {
                log.warn("GATE FAILED [HARD]: {} (actual={}, threshold={} {})",
                        gateName, actual, threshold.getOperator(), threshold.getThreshold());
            }
        }

        summary.computeSummary();
        log.info("Gate evaluation complete: {} hard gates, {} failed",
                summary.getGates().size(), summary.getHardGatesFailed());

        return summary;
    }

    private double resolveMetricValue(String gateName, EvalMetrics metrics) {
        return switch (gateName) {
            case "critical_policy_violation" -> metrics.getCriticalPolicyViolations();
            case "wrong_containment" -> metrics.getWrongContainmentRate();
            case "groundedness_pass_rate" -> metrics.getGroundednessPassRate();
            case "escalation_recall" -> metrics.getEscalationRecall();
            case "handover_completeness" -> metrics.getHandoverCompleteness();
            case "tool_scope_violation" -> metrics.getToolScopeViolations();
            case "forbidden_phrase" -> metrics.getForbiddenPhraseViolations();
            case "budget_enforcement" -> metrics.getBudgetEnforcementRate();
            case "phase_transition_validity" -> metrics.getPhaseTransitionValidity();
            case "critical_high_risk_escalation" -> metrics.getCriticalHighRiskEscalationRate();
            case "out_of_scope_detection" -> metrics.getOutOfScopeDetectionRate();
            default -> {
                log.warn("Unknown gate metric: {}", gateName);
                yield 0.0;
            }
        };
    }
}
