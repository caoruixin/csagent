package com.gumtree.csagent.eval.report;

import com.gumtree.csagent.eval.config.EvalConfig;
import com.gumtree.csagent.eval.model.EvalMetrics;
import com.gumtree.csagent.eval.model.GateResult;
import com.gumtree.csagent.eval.model.SessionResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * Generates a self-contained HTML report with inline CSS.
 */
@Slf4j
@Component
public class HtmlReportGenerator {

    private final EvalConfig config;

    public HtmlReportGenerator(EvalConfig config) {
        this.config = config;
    }

    public Path generate(EvalMetrics metrics, GateResult.GateSummary gates,
                         List<SessionResult> results) throws IOException {
        Path reportDir = Path.of(config.getReportDir());
        Files.createDirectories(reportDir);

        String timestamp = DateTimeFormatter.ISO_INSTANT.format(Instant.now())
                .replace(":", "-").replace(".", "-");
        Path reportFile = reportDir.resolve("eval-report-" + timestamp + ".html");

        StringBuilder html = new StringBuilder();
        html.append(buildHead(metrics));
        html.append(buildSummarySection(metrics, gates));
        html.append(buildGatesTable(gates));
        html.append(buildMetricsBreakdown(metrics));
        html.append(buildPerUcTable(metrics));
        html.append(buildPerDatasetTable(metrics));
        html.append(buildFailedSessionsTable(results));
        html.append(buildFoot());

        Files.writeString(reportFile, html.toString());
        log.info("HTML report written to: {}", reportFile);
        return reportFile;
    }

    private String buildHead(EvalMetrics metrics) {
        return """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Eval Report - %s</title>
                <style>
                  * { margin: 0; padding: 0; box-sizing: border-box; }
                  body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
                         background: #f5f5f5; color: #333; padding: 20px; }
                  .container { max-width: 1200px; margin: 0 auto; }
                  h1 { color: #1a1a2e; margin-bottom: 8px; }
                  h2 { color: #16213e; margin: 24px 0 12px; border-bottom: 2px solid #0f3460; padding-bottom: 4px; }
                  .summary { display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 16px; margin: 16px 0; }
                  .card { background: white; border-radius: 8px; padding: 16px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }
                  .card .label { font-size: 12px; color: #666; text-transform: uppercase; letter-spacing: 0.5px; }
                  .card .value { font-size: 28px; font-weight: 700; margin-top: 4px; }
                  .pass { color: #27ae60; }
                  .fail { color: #e74c3c; }
                  .warn { color: #f39c12; }
                  table { width: 100%%; border-collapse: collapse; background: white; border-radius: 8px;
                          overflow: hidden; box-shadow: 0 2px 4px rgba(0,0,0,0.1); margin: 12px 0; }
                  th { background: #1a1a2e; color: white; padding: 10px 12px; text-align: left; font-size: 13px; }
                  td { padding: 8px 12px; border-bottom: 1px solid #eee; font-size: 13px; }
                  tr:hover td { background: #f8f9fa; }
                  .badge { display: inline-block; padding: 2px 8px; border-radius: 4px; font-size: 11px; font-weight: 600; }
                  .badge-pass { background: #d4edda; color: #155724; }
                  .badge-fail { background: #f8d7da; color: #721c24; }
                  .badge-skip { background: #e2e3e5; color: #383d41; }
                  .timestamp { color: #999; font-size: 12px; }
                  .overall-badge { font-size: 18px; padding: 8px 16px; border-radius: 8px; display: inline-block; margin: 8px 0; }
                </style>
                </head>
                <body>
                <div class="container">
                <h1>CS Agent Eval Report</h1>
                <p class="timestamp">Suite: %s | Generated: %s</p>
                """.formatted(
                metrics.getSuiteName(),
                metrics.getSuiteName(),
                metrics.getEvaluatedAt() != null ? metrics.getEvaluatedAt().toString() : Instant.now().toString()
        );
    }

    private String buildSummarySection(EvalMetrics metrics, GateResult.GateSummary gates) {
        String overallBadge = gates.isAllHardGatesPassed()
                ? "<span class=\"overall-badge badge-pass\">ALL HARD GATES PASSED</span>"
                : "<span class=\"overall-badge badge-fail\">%d HARD GATE(S) FAILED</span>"
                .formatted(gates.getHardGatesFailed());

        return """
                %s
                <div class="summary">
                  <div class="card"><div class="label">Total Sessions</div><div class="value">%d</div></div>
                  <div class="card"><div class="label">Successful Replays</div><div class="value pass">%d</div></div>
                  <div class="card"><div class="label">Failed Replays</div><div class="value %s">%d</div></div>
                  <div class="card"><div class="label">Routing Accuracy</div><div class="value">%.1f%%</div></div>
                  <div class="card"><div class="label">Escalation Recall</div><div class="value">%.1f%%</div></div>
                  <div class="card"><div class="label">Escalation Precision</div><div class="value">%.1f%%</div></div>
                  <div class="card"><div class="label">Containment Rate</div><div class="value">%.1f%%</div></div>
                  <div class="card"><div class="label">Avg Replay (ms)</div><div class="value">%.0f</div></div>
                </div>
                """.formatted(
                overallBadge,
                metrics.getTotalSessions(),
                metrics.getSuccessfulReplays(),
                metrics.getFailedReplays() > 0 ? "fail" : "", metrics.getFailedReplays(),
                metrics.getRoutingAccuracy() * 100,
                metrics.getEscalationRecall() * 100,
                metrics.getEscalationPrecision() * 100,
                metrics.getContainmentRate() * 100,
                metrics.getAvgReplayDurationMs()
        );
    }

    private String buildGatesTable(GateResult.GateSummary gates) {
        StringBuilder sb = new StringBuilder();
        sb.append("<h2>Quality Gates</h2>\n<table>\n");
        sb.append("<tr><th>Gate</th><th>Status</th><th>Actual</th><th>Threshold</th><th>Description</th></tr>\n");

        for (GateResult gate : gates.getGates()) {
            String badge = gate.isPassed()
                    ? "<span class=\"badge badge-pass\">PASS</span>"
                    : "<span class=\"badge badge-fail\">FAIL</span>";
            sb.append("<tr><td>%s</td><td>%s</td><td>%.4f</td><td>%s %.4f</td><td>%s</td></tr>\n"
                    .formatted(gate.getGateName(), badge, gate.getActualValue(),
                            gate.getOperator(), gate.getThreshold(), gate.getDescription()));
        }
        sb.append("</table>\n");
        return sb.toString();
    }

    private String buildMetricsBreakdown(EvalMetrics metrics) {
        return """
                <h2>Detailed Metrics</h2>
                <table>
                <tr><th>Metric</th><th>Value</th></tr>
                <tr><td>Routing Accuracy</td><td>%.4f (%d/%d)</td></tr>
                <tr><td>Escalation Recall</td><td>%.4f</td></tr>
                <tr><td>Escalation Precision</td><td>%.4f</td></tr>
                <tr><td>Escalation F1</td><td>%.4f</td></tr>
                <tr><td>Handover Completeness</td><td>%.4f</td></tr>
                <tr><td>Forbidden Phrase Violations</td><td>%d</td></tr>
                <tr><td>PII Leakage Violations</td><td>%d</td></tr>
                <tr><td>Budget Enforcement Rate</td><td>%.4f</td></tr>
                <tr><td>Phase Transition Validity</td><td>%.4f</td></tr>
                <tr><td>Tool Scope Violations</td><td>%d</td></tr>
                <tr><td>Drift Detection Accuracy</td><td>%.4f</td></tr>
                <tr><td>Groundedness Pass Rate</td><td>%.4f</td></tr>
                <tr><td>Avg Relevance Score</td><td>%.2f / 5</td></tr>
                <tr><td>Avg Summary Quality</td><td>%.2f / 5</td></tr>
                <tr><td>Wrong Containment Rate</td><td>%.4f</td></tr>
                <tr><td>Critical High-Risk Escalation Rate</td><td>%.4f</td></tr>
                <tr><td>Out-of-Scope Detection Rate</td><td>%.4f</td></tr>
                </table>
                """.formatted(
                metrics.getRoutingAccuracy(), metrics.getRoutingCorrect(), metrics.getRoutingTotal(),
                metrics.getEscalationRecall(),
                metrics.getEscalationPrecision(),
                metrics.getEscalationF1(),
                metrics.getHandoverCompleteness(),
                metrics.getForbiddenPhraseViolations(),
                metrics.getPiiLeakageViolations(),
                metrics.getBudgetEnforcementRate(),
                metrics.getPhaseTransitionValidity(),
                metrics.getToolScopeViolations(),
                metrics.getDriftDetectionAccuracy(),
                metrics.getGroundednessPassRate(),
                metrics.getAvgRelevanceScore(),
                metrics.getAvgSummaryQualityScore(),
                metrics.getWrongContainmentRate(),
                metrics.getCriticalHighRiskEscalationRate(),
                metrics.getOutOfScopeDetectionRate()
        );
    }

    private String buildPerUcTable(EvalMetrics metrics) {
        if (metrics.getPerUcMetrics().isEmpty()) return "";

        StringBuilder sb = new StringBuilder();
        sb.append("<h2>Per-UC Breakdown</h2>\n<table>\n");
        sb.append("<tr><th>Use Case</th><th>Sessions</th><th>Routing Accuracy</th></tr>\n");

        for (Map.Entry<String, Map<String, Double>> entry : metrics.getPerUcMetrics().entrySet()) {
            Map<String, Double> m = entry.getValue();
            sb.append("<tr><td>%s</td><td>%.0f</td><td>%.1f%%</td></tr>\n"
                    .formatted(
                            entry.getKey(),
                            m.getOrDefault("total_sessions", 0.0),
                            m.getOrDefault("routing_accuracy", 0.0) * 100
                    ));
        }
        sb.append("</table>\n");
        return sb.toString();
    }

    private String buildPerDatasetTable(EvalMetrics metrics) {
        if (metrics.getPerDatasetMetrics().isEmpty()) return "";

        StringBuilder sb = new StringBuilder();
        sb.append("<h2>Per-Dataset Breakdown</h2>\n<table>\n");
        sb.append("<tr><th>Dataset</th><th>Sessions</th><th>Pass Rate</th></tr>\n");

        for (Map.Entry<String, Map<String, Double>> entry : metrics.getPerDatasetMetrics().entrySet()) {
            Map<String, Double> m = entry.getValue();
            sb.append("<tr><td>%s</td><td>%.0f</td><td>%.1f%%</td></tr>\n"
                    .formatted(
                            entry.getKey(),
                            m.getOrDefault("total_sessions", 0.0),
                            m.getOrDefault("pass_rate", 0.0) * 100
                    ));
        }
        sb.append("</table>\n");
        return sb.toString();
    }

    private String buildFailedSessionsTable(List<SessionResult> results) {
        List<SessionResult> failed = results.stream()
                .filter(r -> !r.allPassed() || !r.isReplaySuccess())
                .toList();

        if (failed.isEmpty()) {
            return "<h2>Failed Sessions</h2>\n<p>No failed sessions.</p>\n";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("<h2>Failed Sessions (%d)</h2>\n<table>\n".formatted(failed.size()));
        sb.append("<tr><th>Eval Session</th><th>Dataset</th><th>Expected UC</th><th>Bot UC</th><th>Failed Graders</th><th>Error</th></tr>\n");

        for (SessionResult r : failed) {
            String failedGraders = r.getFailedGrades().stream()
                    .map(g -> g.getGraderName())
                    .reduce((a, b) -> a + ", " + b)
                    .orElse("-");
            String error = r.isReplaySuccess() ? "-" : r.getReplayError();
            if (error != null && error.length() > 80) {
                error = error.substring(0, 80) + "...";
            }

            sb.append("<tr><td>%s</td><td>%s</td><td>%s</td><td>%s</td><td>%s</td><td>%s</td></tr>\n"
                    .formatted(
                            r.getEvalSessionId(),
                            r.getSourceDataset(),
                            r.getExpectedUc(),
                            r.getBotActiveUseCase() != null ? r.getBotActiveUseCase() : "-",
                            failedGraders,
                            error != null ? error : "-"
                    ));
        }
        sb.append("</table>\n");
        return sb.toString();
    }

    private String buildFoot() {
        return """
                </div>
                </body>
                </html>
                """;
    }
}
