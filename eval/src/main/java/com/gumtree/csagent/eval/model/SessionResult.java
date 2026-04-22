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
 * Per-session grading result including bot responses and all grader outputs.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionResult {

    private String sessionId;
    private String evalSessionId;
    private String sourceDataset;
    private String expectedUc;
    private String expectedOutcome;

    // Bot replay results
    private String botSessionId;
    private String botActiveUseCase;
    private String botContainmentOutcome;
    private String botHandlingState;
    private String botCurrentPhase;
    private String botEscalationReason;

    @Builder.Default
    private List<String> botResponses = new ArrayList<>();
    @Builder.Default
    private List<String> toolsCalled = new ArrayList<>();
    private int botTurnCount;
    private long replayDurationMs;
    private boolean replaySuccess;
    private String replayError;

    // Bot session state (raw JSON from GET /v1/chat/sessions/{id})
    @Builder.Default
    private Map<String, Object> botSessionState = new HashMap<>();

    // Grading results
    @Builder.Default
    private List<GradeResult> gradeResults = new ArrayList<>();

    public void addGradeResult(GradeResult result) {
        gradeResults.add(result);
    }

    public boolean allPassed() {
        return gradeResults.stream()
                .filter(r -> !r.isSkipped())
                .allMatch(GradeResult::isPassed);
    }

    public List<GradeResult> getFailedGrades() {
        return gradeResults.stream()
                .filter(r -> !r.isPassed() && !r.isSkipped())
                .toList();
    }

    public GradeResult getGradeByName(String graderName) {
        return gradeResults.stream()
                .filter(r -> graderName.equals(r.getGraderName()))
                .findFirst()
                .orElse(null);
    }
}
