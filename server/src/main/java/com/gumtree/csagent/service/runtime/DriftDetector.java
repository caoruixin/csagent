package com.gumtree.csagent.service.runtime;

import com.gumtree.csagent.model.BotSession;
import com.gumtree.csagent.model.DriftResult;
import com.gumtree.csagent.model.DriftResult.DriftType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Detects topic drift in user messages.
 * Checks for hard shifts (safety/GDPR/refund keywords), escalation requests,
 * and minor topical drift.
 */
@Slf4j
@Service
public class DriftDetector {

    private static final Pattern ESCALATION_PATTERN = Pattern.compile(
            "(?i)(talk to (an?\\s+)?agent|human agent|real person|transfer me|" +
            "speak (to|with) (a\\s+)?(human|person|agent|someone)|" +
            "connect me|live agent|customer service|live support)"
    );

    private static final List<DriftKeyword> HARD_SHIFT_KEYWORDS = List.of(
            new DriftKeyword(List.of("scam", "scammed", "fraud", "fraudulent"), "UC-J"),
            new DriftKeyword(List.of("delete my data", "delete my account", "gdpr", "data deletion", "right to be forgotten"), "UC-G"),
            new DriftKeyword(List.of("refund", "money back", "charge back", "chargeback", "dispute payment"), "UC-I"),
            new DriftKeyword(List.of("unsafe", "harassment", "threatening", "danger", "abusive"), "UC-J"),
            new DriftKeyword(List.of("ad removed", "ad deleted", "ad taken down", "why was my ad removed", "appeal"), "UC-H")
    );

    /**
     * Detect drift in the user's message relative to the current session.
     */
    public DriftResult detect(BotSession session, String userMessage) {
        if (userMessage == null || userMessage.isBlank()) {
            return DriftResult.builder().type(DriftType.NONE).build();
        }

        String lowerMessage = userMessage.toLowerCase(Locale.ENGLISH);

        // 1. Check for explicit escalation request — distinct from topic drift
        if (ESCALATION_PATTERN.matcher(userMessage).find()) {
            log.info("Session {}: user requested human agent escalation", session.getSessionId());
            return DriftResult.builder()
                    .type(DriftType.USER_ESCALATION_REQUEST)
                    .escalationRequested(true)
                    .newUseCase(null) // keep current UC, just escalate
                    .build();
        }

        // 2. Check for hard shift keywords that suggest a different UC
        String activeUc = session.getActiveUseCase();
        for (DriftKeyword dk : HARD_SHIFT_KEYWORDS) {
            for (String keyword : dk.keywords) {
                if (lowerMessage.contains(keyword)) {
                    // Only trigger if it suggests a DIFFERENT use case
                    if (activeUc != null && !dk.targetUc.equals(activeUc)) {
                        log.info("Session {}: hard shift detected via keyword '{}' -> {}",
                                session.getSessionId(), keyword, dk.targetUc);
                        return DriftResult.builder()
                                .type(DriftType.HARD_SHIFT)
                                .newUseCase(dk.targetUc)
                                .build();
                    }
                }
            }
        }

        // 3. No drift detected
        return DriftResult.builder().type(DriftType.NONE).build();
    }

    private record DriftKeyword(List<String> keywords, String targetUc) {}
}
