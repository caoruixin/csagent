package com.gumtree.csagent.eval.graders.code;

import com.gumtree.csagent.eval.model.EvalSession;
import com.gumtree.csagent.eval.model.GradeResult;
import com.gumtree.csagent.eval.model.SessionResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Grades handover payload completeness when the bot escalates.
 * Checks that the handover payload includes all required fields.
 */
@Slf4j
@Component
public class HandoverGrader {

    private static final String NAME = "handover_completeness";

    /** Required fields in a handover session state */
    private static final List<String> REQUIRED_HANDOVER_FIELDS = List.of(
            "activeUseCase",
            "escalationReason",
            "formTopicSubject"
    );

    /** Desirable fields */
    private static final List<String> DESIRABLE_HANDOVER_FIELDS = List.of(
            "customerContext",
            "formContext"
    );

    public GradeResult grade(EvalSession session, SessionResult result) {
        if (!result.isReplaySuccess()) {
            return GradeResult.skip(NAME, "Replay failed");
        }

        // Only applicable when an escalation actually occurred
        boolean actualEscalation = isActualEscalation(result);
        if (!actualEscalation) {
            return GradeResult.skip(NAME, "No escalation occurred, handover check N/A");
        }

        Map<String, Object> state = result.getBotSessionState();
        if (state == null || state.isEmpty()) {
            return GradeResult.fail(NAME, "Escalation occurred but no session state available for handover validation");
        }

        // Check required fields
        List<String> missingRequired = new ArrayList<>();
        for (String field : REQUIRED_HANDOVER_FIELDS) {
            Object val = state.get(field);
            if (val == null || (val instanceof String s && s.isBlank())) {
                missingRequired.add(field);
            }
        }

        // Check desirable fields
        List<String> missingDesirable = new ArrayList<>();
        for (String field : DESIRABLE_HANDOVER_FIELDS) {
            Object val = state.get(field);
            if (val == null || (val instanceof String s && s.isBlank())) {
                missingDesirable.add(field);
            }
        }

        int totalRequired = REQUIRED_HANDOVER_FIELDS.size();
        int presentRequired = totalRequired - missingRequired.size();
        double completeness = (double) presentRequired / totalRequired;

        boolean passed = missingRequired.isEmpty();
        String detail;
        if (passed) {
            detail = String.format("Handover payload complete (%d/%d required fields present)",
                    presentRequired, totalRequired);
            if (!missingDesirable.isEmpty()) {
                detail += ". Missing desirable: " + String.join(", ", missingDesirable);
            }
        } else {
            detail = String.format("Handover payload incomplete: missing required fields: %s",
                    String.join(", ", missingRequired));
        }

        return GradeResult.builder()
                .graderName(NAME)
                .passed(passed)
                .score(completeness)
                .detail(detail)
                .metadata(Map.of(
                        "missing_required", missingRequired,
                        "missing_desirable", missingDesirable,
                        "completeness", completeness
                ))
                .build();
    }

    private boolean isActualEscalation(SessionResult result) {
        if ("ESCALATED".equalsIgnoreCase(result.getBotHandlingState())) return true;
        if (StringUtils.isNotBlank(result.getBotContainmentOutcome())) {
            String outcome = result.getBotContainmentOutcome().toLowerCase();
            return outcome.contains("escalat") || outcome.contains("handover");
        }
        return StringUtils.isNotBlank(result.getBotEscalationReason());
    }
}
