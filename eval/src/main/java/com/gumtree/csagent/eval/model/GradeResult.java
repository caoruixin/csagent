package com.gumtree.csagent.eval.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

/**
 * Result from a single grader for a single session.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GradeResult {

    private String graderName;
    private boolean passed;
    /** Numeric score (0.0 - 1.0 for binary, 1-5 for rating graders) */
    @Builder.Default
    private double score = 0.0;
    /** Human-readable detail of why it passed or failed */
    private String detail;
    /** Whether this grader was skipped (e.g. not applicable to this session) */
    @Builder.Default
    private boolean skipped = false;
    /** Whether there was an error running the grader */
    @Builder.Default
    private boolean error = false;
    private String errorMessage;
    /** Grader-specific metadata */
    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();

    public static GradeResult pass(String graderName, String detail) {
        return GradeResult.builder()
                .graderName(graderName)
                .passed(true)
                .score(1.0)
                .detail(detail)
                .build();
    }

    public static GradeResult fail(String graderName, String detail) {
        return GradeResult.builder()
                .graderName(graderName)
                .passed(false)
                .score(0.0)
                .detail(detail)
                .build();
    }

    public static GradeResult skip(String graderName, String reason) {
        return GradeResult.builder()
                .graderName(graderName)
                .passed(true)
                .skipped(true)
                .detail(reason)
                .build();
    }

    public static GradeResult error(String graderName, String message) {
        return GradeResult.builder()
                .graderName(graderName)
                .passed(false)
                .error(true)
                .errorMessage(message)
                .detail("Grader error: " + message)
                .build();
    }

    public static GradeResult scored(String graderName, double score, double threshold, String detail) {
        return GradeResult.builder()
                .graderName(graderName)
                .passed(score >= threshold)
                .score(score)
                .detail(detail)
                .build();
    }
}
