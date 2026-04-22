package com.gumtree.csagent.eval.report;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Generates machine-readable JSON report with all metrics, gate results, and per-session details.
 */
@Slf4j
@Component
public class JsonReportGenerator {

    private final EvalConfig config;
    private final ObjectMapper objectMapper;

    public JsonReportGenerator(EvalConfig config) {
        this.config = config;
        this.objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .enable(SerializationFeature.INDENT_OUTPUT)
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    public Path generate(EvalMetrics metrics, GateResult.GateSummary gates,
                         List<SessionResult> results) throws IOException {
        Path reportDir = Path.of(config.getReportDir());
        Files.createDirectories(reportDir);

        String timestamp = DateTimeFormatter.ISO_INSTANT.format(Instant.now())
                .replace(":", "-").replace(".", "-");
        Path reportFile = reportDir.resolve("eval-report-" + timestamp + ".json");

        Map<String, Object> report = new LinkedHashMap<>();
        report.put("generated_at", Instant.now().toString());
        report.put("suite", metrics.getSuiteName());
        report.put("summary", buildSummary(metrics, gates));
        report.put("metrics", metrics);
        report.put("gates", buildGatesSection(gates));
        report.put("per_uc_metrics", metrics.getPerUcMetrics());
        report.put("per_dataset_metrics", metrics.getPerDatasetMetrics());
        report.put("sessions", buildSessionsSummary(results));

        objectMapper.writeValue(reportFile.toFile(), report);
        log.info("JSON report written to: {}", reportFile);
        return reportFile;
    }

    private Map<String, Object> buildSummary(EvalMetrics metrics, GateResult.GateSummary gates) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("total_sessions", metrics.getTotalSessions());
        summary.put("successful_replays", metrics.getSuccessfulReplays());
        summary.put("failed_replays", metrics.getFailedReplays());
        summary.put("all_hard_gates_passed", gates.isAllHardGatesPassed());
        summary.put("hard_gates_failed", gates.getHardGatesFailed());
        summary.put("routing_accuracy", metrics.getRoutingAccuracy());
        summary.put("escalation_recall", metrics.getEscalationRecall());
        summary.put("escalation_precision", metrics.getEscalationPrecision());
        summary.put("containment_rate", metrics.getContainmentRate());
        summary.put("avg_replay_duration_ms", metrics.getAvgReplayDurationMs());
        summary.put("total_eval_duration_ms", metrics.getTotalEvalDurationMs());
        return summary;
    }

    private List<Map<String, Object>> buildGatesSection(GateResult.GateSummary gates) {
        return gates.getGates().stream().map(g -> {
            Map<String, Object> gateMap = new LinkedHashMap<>();
            gateMap.put("name", g.getGateName());
            gateMap.put("passed", g.isPassed());
            gateMap.put("hard", g.isHard());
            gateMap.put("actual", g.getActualValue());
            gateMap.put("threshold", g.getThreshold());
            gateMap.put("operator", g.getOperator());
            gateMap.put("description", g.getDescription());
            return gateMap;
        }).toList();
    }

    private List<Map<String, Object>> buildSessionsSummary(List<SessionResult> results) {
        return results.stream().map(r -> {
            Map<String, Object> sessionMap = new LinkedHashMap<>();
            sessionMap.put("eval_session_id", r.getEvalSessionId());
            sessionMap.put("source_dataset", r.getSourceDataset());
            sessionMap.put("expected_uc", r.getExpectedUc());
            sessionMap.put("expected_outcome", r.getExpectedOutcome());
            sessionMap.put("bot_session_id", r.getBotSessionId());
            sessionMap.put("bot_active_uc", r.getBotActiveUseCase());
            sessionMap.put("bot_outcome", r.getBotContainmentOutcome());
            sessionMap.put("replay_success", r.isReplaySuccess());
            sessionMap.put("replay_duration_ms", r.getReplayDurationMs());
            sessionMap.put("all_passed", r.allPassed());
            sessionMap.put("grades", r.getGradeResults().stream().map(g -> {
                Map<String, Object> gradeMap = new LinkedHashMap<>();
                gradeMap.put("grader", g.getGraderName());
                gradeMap.put("passed", g.isPassed());
                gradeMap.put("score", g.getScore());
                gradeMap.put("skipped", g.isSkipped());
                gradeMap.put("detail", g.getDetail());
                return gradeMap;
            }).toList());
            if (!r.isReplaySuccess()) {
                sessionMap.put("replay_error", r.getReplayError());
            }
            return sessionMap;
        }).toList();
    }
}
