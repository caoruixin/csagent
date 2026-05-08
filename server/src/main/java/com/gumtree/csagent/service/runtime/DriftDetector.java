package com.gumtree.csagent.service.runtime;

import com.gumtree.csagent.model.BotSession;
import com.gumtree.csagent.model.DriftResult;
import com.gumtree.csagent.model.DriftResult.DriftType;
import com.gumtree.csagent.service.runtime.RiskKeywordsConfig.HardShiftGroup;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Detects topic drift in user messages.
 * Checks for hard shifts (safety/GDPR/refund keywords), escalation requests,
 * and minor topical drift.
 *
 * <p>Sprint 15 §M0: keyword groups and escalation regex are now sourced
 * from {@code config/risk-keywords.yaml} via {@link RiskKeywordsConfig}.
 * The matching contract is unchanged — same groups, same first-match
 * semantics, same precedence, same outputs. See
 * {@code docs/runtime_freeze_and_risk_policy.md} for the policy
 * boundary.
 */
@Slf4j
@Service
public class DriftDetector {

    private final RiskKeywordsConfig riskKeywordsConfig;

    public DriftDetector(RiskKeywordsConfig riskKeywordsConfig) {
        this.riskKeywordsConfig = riskKeywordsConfig;
    }

    /**
     * Convenience constructor used by unit tests that previously relied
     * on the hardcoded keyword list. Loads the bundled default
     * {@code config/risk-keywords.yaml} resource so behaviour matches
     * the production wiring exactly.
     */
    public DriftDetector() {
        try {
            this.riskKeywordsConfig = RiskKeywordsConfig.loadDefault();
        } catch (IOException e) {
            throw new IllegalStateException(
                    "DriftDetector: failed to load default risk-keywords config", e);
        }
    }

    /**
     * Detect drift in the user's message relative to the current session.
     */
    public DriftResult detect(BotSession session, String userMessage) {
        if (userMessage == null || userMessage.isBlank()) {
            return DriftResult.builder().type(DriftType.NONE).build();
        }

        String lowerMessage = userMessage.toLowerCase(Locale.ENGLISH);

        // 1. Check for explicit escalation request — distinct from topic drift
        Pattern escalationPattern = riskKeywordsConfig.getEscalationPattern();
        if (escalationPattern.matcher(userMessage).find()) {
            log.info("Session {}: user requested human agent escalation", session.getSessionId());
            return DriftResult.builder()
                    .type(DriftType.USER_ESCALATION_REQUEST)
                    .escalationRequested(true)
                    .newUseCase(null) // keep current UC, just escalate
                    .build();
        }

        // 2. Check for hard shift keywords that suggest a different UC
        String activeUc = session.getActiveUseCase();
        List<HardShiftGroup> groups = riskKeywordsConfig.getHardShiftGroups();
        for (HardShiftGroup dk : groups) {
            for (String keyword : dk.keywords()) {
                if (lowerMessage.contains(keyword)) {
                    // Only trigger if it suggests a DIFFERENT use case
                    if (activeUc != null && !dk.targetUseCase().equals(activeUc)) {
                        log.info("Session {}: hard shift detected via keyword '{}' -> {}",
                                session.getSessionId(), keyword, dk.targetUseCase());
                        return DriftResult.builder()
                                .type(DriftType.HARD_SHIFT)
                                .newUseCase(dk.targetUseCase())
                                .build();
                    }
                }
            }
        }

        // 3. No drift detected
        return DriftResult.builder().type(DriftType.NONE).build();
    }
}
