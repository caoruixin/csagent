package com.gumtree.csagent.service.runtime;

import com.gumtree.csagent.model.BotSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Checks all budget limits against the current session state.
 * Returns the name of the exceeded budget, or empty if all within limits.
 */
@Slf4j
@Service
public class BudgetChecker {

    private final ControlPolicyService controlPolicy;
    private final UseCaseRegistryService useCaseRegistry;

    public BudgetChecker(ControlPolicyService controlPolicy, UseCaseRegistryService useCaseRegistry) {
        this.controlPolicy = controlPolicy;
        this.useCaseRegistry = useCaseRegistry;
    }

    /**
     * Check if any budget has been exceeded for the given session.
     *
     * @return Optional containing the name of the exceeded budget, or empty if all within limits
     */
    public Optional<String> checkBudgets(BotSession session) {
        // 1. Clarification rounds
        if (session.getClarificationCount() >= controlPolicy.getMaxClarificationRounds()) {
            log.info("Session {}: clarification budget exceeded ({} >= {})",
                    session.getSessionId(), session.getClarificationCount(),
                    controlPolicy.getMaxClarificationRounds());
            return Optional.of("max-clarification-rounds");
        }

        // 2. FAQ miss count
        if (session.getFaqMissCount() >= controlPolicy.getMaxFaqMiss()) {
            log.info("Session {}: FAQ miss budget exceeded ({} >= {})",
                    session.getSessionId(), session.getFaqMissCount(),
                    controlPolicy.getMaxFaqMiss());
            return Optional.of("max-faq-miss");
        }

        // 3. Path-specific bot turns
        String activeUc = session.getActiveUseCase();
        if (activeUc != null) {
            UseCaseRegistryService.UseCaseDefinition ucDef = useCaseRegistry.getUseCase(activeUc);
            if (ucDef != null) {
                if ("FAQ".equals(ucDef.path()) && session.getTotalBotTurns() >= controlPolicy.getMaxBotTurnsFaq()) {
                    log.info("Session {}: FAQ bot turns budget exceeded ({} >= {})",
                            session.getSessionId(), session.getTotalBotTurns(),
                            controlPolicy.getMaxBotTurnsFaq());
                    return Optional.of("max-bot-turns-faq");
                }
                if ("INTAKE".equals(ucDef.path()) && session.getTotalBotTurns() >= controlPolicy.getMaxBotTurnsIntake()) {
                    log.info("Session {}: INTAKE bot turns budget exceeded ({} >= {})",
                            session.getSessionId(), session.getTotalBotTurns(),
                            controlPolicy.getMaxBotTurnsIntake());
                    return Optional.of("max-bot-turns-intake");
                }
            }
        }

        // 4. Repeated same action
        if (session.getRepeatedActionCount() >= controlPolicy.getMaxRepeatedSameAction()) {
            log.info("Session {}: repeated action budget exceeded ({} >= {})",
                    session.getSessionId(), session.getRepeatedActionCount(),
                    controlPolicy.getMaxRepeatedSameAction());
            return Optional.of("max-repeated-same-action");
        }

        // 5. Total bot turns (absolute cap)
        if (session.getTotalBotTurns() >= controlPolicy.getMaxTotalBotTurns()) {
            log.info("Session {}: total bot turns budget exceeded ({} >= {})",
                    session.getSessionId(), session.getTotalBotTurns(),
                    controlPolicy.getMaxTotalBotTurns());
            return Optional.of("max-total-bot-turns");
        }

        return Optional.empty();
    }
}
