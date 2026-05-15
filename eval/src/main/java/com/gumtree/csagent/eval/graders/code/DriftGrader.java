package com.gumtree.csagent.eval.graders.code;

import com.gumtree.csagent.eval.model.EvalSession;
import com.gumtree.csagent.eval.model.GradeResult;
import com.gumtree.csagent.eval.model.SessionResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Grades drift detection accuracy for sessions with known drift patterns.
 *
 * Uses dataset-specific fields:
 *   - drift_complexity (from drift_control_dataset)
 *   - uc_count
 * And HR annotation fields:
 *   - drift_type (soft_shift, hard_switch, minor_drift, none)
 *   - drift_handling (expected behavior description)
 */
@Slf4j
@Component
public class DriftGrader {

    private static final String NAME = "drift_detection";

    public GradeResult grade(EvalSession session, SessionResult result) {
        if (!result.isReplaySuccess()) {
            return GradeResult.skip(NAME, "Replay failed");
        }

        // Only applicable for sessions with known drift patterns
        String driftType = session.isHrAnnotated() ? session.getHrDriftType() : null;
        String driftComplexity = session.getExtraFields().get("drift_complexity");

        if (StringUtils.isBlank(driftType) && StringUtils.isBlank(driftComplexity)) {
            return GradeResult.skip(NAME, "No drift data available for this session");
        }

        // For drift sessions, check that the bot's active UC matches expected
        String expectedUc = session.getEffectivePrimaryUc();
        String actualUc = result.getBotActiveUseCase();

        boolean ucCorrect = StringUtils.isNotBlank(expectedUc)
                && expectedUc.equalsIgnoreCase(actualUc);

        // For multi-UC sessions, the bot should maintain the primary UC and not drift
        String allUcs = session.getAllUcs();
        boolean isMultiUc = allUcs != null && allUcs.contains("|");

        String detail;
        boolean passed;

        if ("none".equalsIgnoreCase(driftType)) {
            // No drift expected: bot should stay on primary UC
            passed = ucCorrect;
            detail = passed
                    ? "No drift expected, bot stayed on primary UC: " + actualUc
                    : "No drift expected but bot routed to: " + actualUc + " (expected: " + expectedUc + ")";
        } else if (StringUtils.isNotBlank(driftType)) {
            // Drift present: bot should still resolve to the corrected primary UC
            passed = ucCorrect;
            detail = passed
                    ? String.format("Drift type=%s handled correctly, maintained UC: %s", driftType, actualUc)
                    : String.format("Drift type=%s mishandled: bot on %s, expected %s",
                    driftType, actualUc, expectedUc);
        } else {
            // Only complexity info, no specific type
            passed = ucCorrect;
            detail = passed
                    ? "Drift control passed for complexity=" + driftComplexity
                    : "Drift control failed: UC mismatch for complexity=" + driftComplexity;
        }

        return GradeResult.builder()
                .graderName(NAME)
                .passed(passed)
                .score(passed ? 1.0 : 0.0)
                .detail(detail)
                .metadata(Map.of(
                        "drift_type", driftType != null ? driftType : "",
                        "drift_complexity", driftComplexity != null ? driftComplexity : "",
                        "expected_uc", expectedUc != null ? expectedUc : "",
                        "actual_uc", actualUc != null ? actualUc : "",
                        "multi_uc", isMultiUc
                ))
                .build();
    }
}
