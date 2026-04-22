package com.gumtree.csagent.eval.graders.code;

import com.gumtree.csagent.eval.model.EvalSession;
import com.gumtree.csagent.eval.model.GradeResult;
import com.gumtree.csagent.eval.model.SessionResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Grades escalation decisions: recall (did we escalate when we should have?)
 * and precision (did we avoid escalating when we shouldn't have?).
 */
@Slf4j
@Component
public class EscalationGrader {

    private static final String NAME = "escalation";

    public GradeResult grade(EvalSession session, SessionResult result) {
        if (!result.isReplaySuccess()) {
            return GradeResult.skip(NAME, "Replay failed");
        }

        boolean expectedEscalation = resolveExpectedEscalation(session);
        boolean actualEscalation = isActualEscalation(result);

        String detail;
        boolean passed;

        if (expectedEscalation && actualEscalation) {
            // True positive: correctly escalated
            detail = "Correctly escalated (TP)";
            passed = true;
        } else if (expectedEscalation && !actualEscalation) {
            // False negative: should have escalated but didn't
            detail = "Missed escalation (FN): expected escalation but bot resolved/continued";
            passed = false;
        } else if (!expectedEscalation && actualEscalation) {
            // False positive: escalated unnecessarily
            detail = "Unnecessary escalation (FP): bot escalated but should have resolved";
            passed = false;
        } else {
            // True negative: correctly did not escalate
            detail = "Correctly did not escalate (TN)";
            passed = true;
        }

        GradeResult grade = GradeResult.builder()
                .graderName(NAME)
                .passed(passed)
                .score(passed ? 1.0 : 0.0)
                .detail(detail)
                .metadata(Map.of(
                        "expected_escalation", expectedEscalation,
                        "actual_escalation", actualEscalation,
                        "type", classifyType(expectedEscalation, actualEscalation),
                        "escalation_reason", result.getBotEscalationReason() != null ? result.getBotEscalationReason() : ""
                ))
                .build();
        return grade;
    }

    private boolean resolveExpectedEscalation(EvalSession session) {
        // Prefer HR annotation
        if (session.isHrAnnotated()) {
            return session.isHrShouldEscalate();
        }
        // Fall back to dataset signal
        if (session.isHasEscalationSignal()) {
            return true;
        }
        // Check outcome class
        String outcome = session.getEffectiveOutcomeClass();
        return "escalate".equalsIgnoreCase(outcome);
    }

    private boolean isActualEscalation(SessionResult result) {
        // Check handling state
        if ("ESCALATED".equalsIgnoreCase(result.getBotHandlingState())) {
            return true;
        }
        // Check containment outcome
        if (StringUtils.isNotBlank(result.getBotContainmentOutcome())) {
            String outcome = result.getBotContainmentOutcome().toLowerCase();
            return outcome.contains("escalat") || outcome.contains("handover");
        }
        // Check escalation reason
        return StringUtils.isNotBlank(result.getBotEscalationReason());
    }

    private String classifyType(boolean expected, boolean actual) {
        if (expected && actual) return "TP";
        if (expected && !actual) return "FN";
        if (!expected && actual) return "FP";
        return "TN";
    }
}
