package com.gumtree.csagent.eval.graders.code;

import com.gumtree.csagent.eval.model.EvalSession;
import com.gumtree.csagent.eval.model.GradeResult;
import com.gumtree.csagent.eval.model.SessionResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Grades budget enforcement and phase transition validity.
 *
 * Budget: max bot turns should not exceed the configured limit.
 * Phase transitions: valid sequences are INIT -> INTAKE -> DIAGNOSTICS -> RESOLUTION -> CLOSED
 *                    (with possible ESCALATED from any phase).
 */
@Slf4j
@Component
public class ControlGrader {

    private static final String NAME = "control";

    /** Maximum allowed bot turns before escalation is forced */
    private static final int MAX_BOT_TURNS = 15;

    /** Valid phase values */
    private static final Set<String> VALID_PHASES = Set.of(
            "INIT", "INTAKE", "DIAGNOSTICS", "RESOLUTION", "CLOSED", "ESCALATED");

    public GradeResult grade(EvalSession session, SessionResult result) {
        if (!result.isReplaySuccess()) {
            return GradeResult.skip(NAME, "Replay failed");
        }

        List<String> violations = new ArrayList<>();
        Map<String, Object> state = result.getBotSessionState();

        // Budget enforcement: check total bot turns
        int botTurns = result.getBotTurnCount();
        Object totalTurnsObj = state != null ? state.get("totalBotTurns") : null;
        if (totalTurnsObj instanceof Number n) {
            botTurns = Math.max(botTurns, n.intValue());
        }

        boolean budgetOk = botTurns <= MAX_BOT_TURNS;
        if (!budgetOk) {
            violations.add(String.format("Budget exceeded: %d turns (max %d)", botTurns, MAX_BOT_TURNS));
        }

        // Phase transition validity: check current phase is a valid value
        String currentPhase = result.getBotCurrentPhase();
        boolean phaseValid = true;
        if (currentPhase != null && !currentPhase.isBlank()) {
            if (!VALID_PHASES.contains(currentPhase.toUpperCase())) {
                phaseValid = false;
                violations.add("Invalid phase: " + currentPhase);
            }
        }

        boolean passed = violations.isEmpty();
        String detail = passed
                ? String.format("Control OK: %d turns, phase=%s", botTurns, currentPhase)
                : "Control violations: " + String.join("; ", violations);

        return GradeResult.builder()
                .graderName(NAME)
                .passed(passed)
                .score(passed ? 1.0 : 0.0)
                .detail(detail)
                .metadata(Map.of(
                        "bot_turns", botTurns,
                        "max_turns", MAX_BOT_TURNS,
                        "budget_ok", budgetOk,
                        "current_phase", currentPhase != null ? currentPhase : "",
                        "phase_valid", phaseValid,
                        "violations", violations
                ))
                .build();
    }
}
