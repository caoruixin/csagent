package com.gumtree.csagent.eval.graders.code;

import com.gumtree.csagent.eval.model.EvalSession;
import com.gumtree.csagent.eval.model.GradeResult;
import com.gumtree.csagent.eval.model.SessionResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Grades UC routing accuracy: exact match between expected and actual use case.
 */
@Slf4j
@Component
public class RoutingGrader {

    private static final String NAME = "routing";

    public GradeResult grade(EvalSession session, SessionResult result) {
        if (!result.isReplaySuccess()) {
            return GradeResult.skip(NAME, "Replay failed, cannot evaluate routing");
        }

        String expected = session.getEffectivePrimaryUc();
        String actual = result.getBotActiveUseCase();

        if (StringUtils.isBlank(expected)) {
            return GradeResult.skip(NAME, "No expected UC defined");
        }

        if (StringUtils.isBlank(actual)) {
            return GradeResult.fail(NAME,
                    "Bot did not assign a use case (expected: " + expected + ")");
        }

        // Exact match (case-insensitive)
        boolean match = expected.trim().equalsIgnoreCase(actual.trim());

        GradeResult grade = match
                ? GradeResult.pass(NAME, "Correct routing: " + actual)
                : GradeResult.fail(NAME,
                "Routing mismatch: expected=" + expected + ", actual=" + actual);

        grade.setMetadata(Map.of(
                "expected_uc", expected,
                "actual_uc", actual != null ? actual : "",
                "match", match));
        return grade;
    }
}
