package com.gumtree.csagent.eval.harness;

import com.gumtree.csagent.eval.config.EvalConfig;
import com.gumtree.csagent.eval.graders.code.*;
import com.gumtree.csagent.eval.graders.model.*;
import com.gumtree.csagent.eval.metrics.GateEvaluator;
import com.gumtree.csagent.eval.metrics.MetricsAggregator;
import com.gumtree.csagent.eval.model.*;
import com.gumtree.csagent.eval.report.HtmlReportGenerator;
import com.gumtree.csagent.eval.report.JsonReportGenerator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.*;

/**
 * Main eval orchestrator. Runs the full eval pipeline:
 *   1. Load suite config + datasets
 *   2. For each session: simulate -> grade -> collect
 *   3. Aggregate metrics
 *   4. Evaluate gates
 *   5. Generate reports
 */
@Slf4j
@Component
public class EvalRunner {

    private final EvalConfig config;
    private final EvalConfig.SuiteDefinition suiteDefinition;
    private final DatasetLoader datasetLoader;
    private final SessionSimulator sessionSimulator;
    private final ResultCollector resultCollector;
    private final MetricsAggregator metricsAggregator;
    private final GateEvaluator gateEvaluator;
    private final HtmlReportGenerator htmlReportGenerator;
    private final JsonReportGenerator jsonReportGenerator;

    // Code graders
    private final RoutingGrader routingGrader;
    private final EscalationGrader escalationGrader;
    private final HandoverGrader handoverGrader;
    private final ControlGrader controlGrader;
    private final ToolContractGrader toolContractGrader;
    private final PolicyGrader policyGrader;
    private final DriftGrader driftGrader;

    // Model graders
    private final GroundednessGrader groundednessGrader;
    private final RelevanceGrader relevanceGrader;
    private final SummaryQualityGrader summaryQualityGrader;
    private final ClarificationGrader clarificationGrader;

    public EvalRunner(EvalConfig config,
                      EvalConfig.SuiteDefinition suiteDefinition,
                      DatasetLoader datasetLoader,
                      SessionSimulator sessionSimulator,
                      ResultCollector resultCollector,
                      MetricsAggregator metricsAggregator,
                      GateEvaluator gateEvaluator,
                      HtmlReportGenerator htmlReportGenerator,
                      JsonReportGenerator jsonReportGenerator,
                      RoutingGrader routingGrader,
                      EscalationGrader escalationGrader,
                      HandoverGrader handoverGrader,
                      ControlGrader controlGrader,
                      ToolContractGrader toolContractGrader,
                      PolicyGrader policyGrader,
                      DriftGrader driftGrader,
                      GroundednessGrader groundednessGrader,
                      RelevanceGrader relevanceGrader,
                      SummaryQualityGrader summaryQualityGrader,
                      ClarificationGrader clarificationGrader) {
        this.config = config;
        this.suiteDefinition = suiteDefinition;
        this.datasetLoader = datasetLoader;
        this.sessionSimulator = sessionSimulator;
        this.resultCollector = resultCollector;
        this.metricsAggregator = metricsAggregator;
        this.gateEvaluator = gateEvaluator;
        this.htmlReportGenerator = htmlReportGenerator;
        this.jsonReportGenerator = jsonReportGenerator;
        this.routingGrader = routingGrader;
        this.escalationGrader = escalationGrader;
        this.handoverGrader = handoverGrader;
        this.controlGrader = controlGrader;
        this.toolContractGrader = toolContractGrader;
        this.policyGrader = policyGrader;
        this.driftGrader = driftGrader;
        this.groundednessGrader = groundednessGrader;
        this.relevanceGrader = relevanceGrader;
        this.summaryQualityGrader = summaryQualityGrader;
        this.clarificationGrader = clarificationGrader;
    }

    /**
     * Run the full evaluation pipeline.
     *
     * @return true if all hard gates passed
     */
    public boolean run() {
        long startTime = System.currentTimeMillis();

        log.info("========================================");
        log.info("Starting eval suite: {} ({})", suiteDefinition.getName(), suiteDefinition.getDescription());
        log.info("Bot URL: {}", config.getBotBaseUrl());
        log.info("========================================");

        // Step 1: Load datasets
        log.info("Step 1: Loading datasets...");
        List<EvalSession> sessions = datasetLoader.loadDatasets(suiteDefinition.getDatasets());
        log.info("Loaded {} total sessions", sessions.size());

        // Apply session selection
        sessions = applySessionSelection(sessions);
        log.info("After selection: {} sessions to evaluate", sessions.size());

        if (sessions.isEmpty()) {
            log.error("No sessions to evaluate. Aborting.");
            return false;
        }

        // Step 2: Simulate + Grade each session
        log.info("Step 2: Simulating and grading {} sessions...", sessions.size());
        resultCollector.clear();

        for (int i = 0; i < sessions.size(); i++) {
            EvalSession session = sessions.get(i);
            try {
                log.info("[{}/{}] Evaluating session: {} ({})",
                        i + 1, sessions.size(), session.getId(), session.getSourceDataset());

                // Simulate
                SessionResult result = sessionSimulator.simulate(session);

                // Grade with all code graders
                gradeSession(session, result);

                // Collect
                resultCollector.addResult(result);

            } catch (Exception e) {
                log.error("Session {} failed unexpectedly: {}", session.getId(), e.getMessage(), e);
                SessionResult errorResult = SessionResult.builder()
                        .evalSessionId(session.getId())
                        .sourceDataset(session.getSourceDataset())
                        .expectedUc(session.getEffectivePrimaryUc())
                        .expectedOutcome(session.getEffectiveOutcomeClass())
                        .replaySuccess(false)
                        .replayError("Unexpected error: " + e.getMessage())
                        .build();
                resultCollector.addResult(errorResult);
            }
        }

        // Step 3: Aggregate metrics
        log.info("Step 3: Aggregating metrics...");
        long totalDuration = System.currentTimeMillis() - startTime;
        EvalMetrics metrics = metricsAggregator.aggregate(
                suiteDefinition.getName(), resultCollector.getResults(), totalDuration);

        // Step 4: Evaluate gates
        log.info("Step 4: Evaluating quality gates...");
        GateResult.GateSummary gateSummary = gateEvaluator.evaluate(metrics);

        // Step 5: Generate reports
        log.info("Step 5: Generating reports...");
        try {
            Path htmlPath = htmlReportGenerator.generate(metrics, gateSummary, resultCollector.getResults());
            Path jsonPath = jsonReportGenerator.generate(metrics, gateSummary, resultCollector.getResults());
            log.info("Reports generated:");
            log.info("  HTML: {}", htmlPath.toAbsolutePath());
            log.info("  JSON: {}", jsonPath.toAbsolutePath());
        } catch (Exception e) {
            log.error("Failed to generate reports: {}", e.getMessage(), e);
        }

        // Print summary
        printSummary(metrics, gateSummary);

        return gateSummary.isAllHardGatesPassed();
    }

    private void gradeSession(EvalSession session, SessionResult result) {
        // Code-based graders (always run)
        safeGrade("routing", () -> routingGrader.grade(session, result), result);
        safeGrade("escalation", () -> escalationGrader.grade(session, result), result);
        safeGrade("handover", () -> handoverGrader.grade(session, result), result);
        safeGrade("control", () -> controlGrader.grade(session, result), result);
        safeGrade("tool_contract", () -> toolContractGrader.grade(session, result), result);
        safeGrade("policy", () -> policyGrader.grade(session, result), result);
        safeGrade("drift", () -> driftGrader.grade(session, result), result);

        // Model-based graders (may fail if LLM is not available)
        safeGrade("groundedness", () -> groundednessGrader.grade(session, result), result);
        safeGrade("relevance", () -> relevanceGrader.grade(session, result), result);
        safeGrade("summary_quality", () -> summaryQualityGrader.grade(session, result), result);
        safeGrade("clarification", () -> clarificationGrader.grade(session, result), result);
    }

    private void safeGrade(String name, GraderInvocation grader, SessionResult result) {
        try {
            GradeResult grade = grader.invoke();
            result.addGradeResult(grade);
        } catch (Exception e) {
            log.warn("Grader '{}' threw exception for session {}: {}",
                    name, result.getEvalSessionId(), e.getMessage());
            result.addGradeResult(GradeResult.error(name, e.getMessage()));
        }
    }

    @FunctionalInterface
    private interface GraderInvocation {
        GradeResult invoke();
    }

    private List<EvalSession> applySessionSelection(List<EvalSession> sessions) {
        int maxSessions = suiteDefinition.getMaxSessions();
        String selection = suiteDefinition.getSessionSelection();

        if ("all".equals(selection) || sessions.size() <= maxSessions) {
            return sessions.size() <= maxSessions
                    ? sessions
                    : sessions.subList(0, maxSessions);
        }

        if ("stratified_sample".equals(selection)) {
            return stratifiedSample(sessions, maxSessions);
        }

        // Default: truncate
        return sessions.subList(0, Math.min(sessions.size(), maxSessions));
    }

    /**
     * Stratified sampling: proportional representation from each dataset.
     */
    private List<EvalSession> stratifiedSample(List<EvalSession> sessions, int maxSessions) {
        Map<String, List<EvalSession>> byDataset = new LinkedHashMap<>();
        for (EvalSession s : sessions) {
            byDataset.computeIfAbsent(s.getSourceDataset(), k -> new ArrayList<>()).add(s);
        }

        List<EvalSession> sampled = new ArrayList<>();
        int totalAvailable = sessions.size();

        for (Map.Entry<String, List<EvalSession>> entry : byDataset.entrySet()) {
            List<EvalSession> dsSessions = entry.getValue();
            int dsAllocation = Math.max(1,
                    (int) Math.round((double) dsSessions.size() / totalAvailable * maxSessions));
            dsAllocation = Math.min(dsAllocation, dsSessions.size());

            // Shuffle for randomness then take the allocation
            List<EvalSession> shuffled = new ArrayList<>(dsSessions);
            Collections.shuffle(shuffled);
            sampled.addAll(shuffled.subList(0, dsAllocation));
        }

        // Trim if we over-allocated due to rounding
        if (sampled.size() > maxSessions) {
            sampled = sampled.subList(0, maxSessions);
        }

        return sampled;
    }

    private void printSummary(EvalMetrics metrics, GateResult.GateSummary gates) {
        log.info("========================================");
        log.info("EVAL SUMMARY: {}", metrics.getSuiteName());
        log.info("========================================");
        log.info("Sessions: {} total, {} replayed, {} failed",
                metrics.getTotalSessions(), metrics.getSuccessfulReplays(), metrics.getFailedReplays());
        log.info("Routing accuracy:     {}/{} ({}%)",
                metrics.getRoutingCorrect(), metrics.getRoutingTotal(),
                String.format("%.1f", metrics.getRoutingAccuracy() * 100));
        log.info("Escalation recall:    {}%", String.format("%.1f", metrics.getEscalationRecall() * 100));
        log.info("Escalation precision: {}%", String.format("%.1f", metrics.getEscalationPrecision() * 100));
        log.info("Containment rate:     {}%", String.format("%.1f", metrics.getContainmentRate() * 100));
        log.info("Groundedness:         {}%", String.format("%.1f", metrics.getGroundednessPassRate() * 100));
        log.info("Duration:             {}ms", metrics.getTotalEvalDurationMs());
        log.info("----------------------------------------");

        if (gates.isAllHardGatesPassed()) {
            log.info("RESULT: ALL HARD GATES PASSED");
        } else {
            log.error("RESULT: {} HARD GATE(S) FAILED", gates.getHardGatesFailed());
            gates.getGates().stream()
                    .filter(g -> g.isHard() && !g.isPassed())
                    .forEach(g -> log.error("  FAILED: {} (actual={}, threshold={} {})",
                            g.getGateName(), g.getActualValue(),
                            g.getOperator(), g.getThreshold()));
        }
        log.info("========================================");
    }
}
