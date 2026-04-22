package com.gumtree.csagent.eval.harness;

import com.gumtree.csagent.eval.model.EvalSession;
import com.gumtree.csagent.eval.model.EvalTurn;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Normalizes eval session data into the form fields expected by the bot API.
 *
 * The pre-chat form in the turns CSV has format:
 *   [Form] Subject: Ad Support | Description: Not receiving replies
 *
 * The bot API expects:
 *   { "first_name": "...", "email": "...", "topic_subject": "...", "ad_id": "...", "description": "..." }
 */
@Slf4j
@Component
public class FormNormalizer {

    private static final Pattern FORM_SUBJECT_PATTERN = Pattern.compile(
            "\\[Form\\]\\s*Subject:\\s*(.+?)(?:\\s*\\|\\s*Description:\\s*(.*))?$");

    /**
     * Build the session creation payload from the eval session.
     */
    public Map<String, String> buildCreateSessionPayload(EvalSession session) {
        Map<String, String> payload = new HashMap<>();

        // Use subject from dataset CSV first
        String topicSubject = session.getSubject();
        String description = session.getDescription();

        // Try to parse from pre-chat form turn if available
        EvalTurn formTurn = session.getPreChatFormTurn();
        if (formTurn != null) {
            String msg = formTurn.getMessageRedacted();
            if (msg != null) {
                Matcher m = FORM_SUBJECT_PATTERN.matcher(msg);
                if (m.find()) {
                    String parsedSubject = m.group(1).trim();
                    if (StringUtils.isNotBlank(parsedSubject)) {
                        topicSubject = parsedSubject;
                    }
                    if (m.group(2) != null) {
                        String parsedDesc = m.group(2).trim();
                        if (StringUtils.isNotBlank(parsedDesc)) {
                            description = parsedDesc;
                        }
                    }
                }
            }
        }

        payload.put("topic_subject", topicSubject != null ? topicSubject : "General");
        if (StringUtils.isNotBlank(description)) {
            payload.put("description", description);
        }

        // Synthesize a first name for the eval session
        payload.put("first_name", "EvalUser");

        // Synthesize an email from the identifier evidence if present
        String email = extractEmail(session);
        if (StringUtils.isNotBlank(email)) {
            payload.put("email", email);
        } else {
            payload.put("email", "eval-" + session.getId() + "@test.gumtree.com");
        }

        // Extract ad_id if present in identifier evidence
        String adId = extractAdId(session);
        if (StringUtils.isNotBlank(adId)) {
            payload.put("ad_id", adId);
        }

        return payload;
    }

    /**
     * Extract email from identifier evidence or HR annotations.
     */
    private String extractEmail(EvalSession session) {
        String evidence = session.getIdentifierEvidence();
        if (StringUtils.isNotBlank(evidence)) {
            // Look for email patterns in the evidence
            Pattern emailPattern = Pattern.compile("[\\w.+-]+@[\\w.-]+\\.[a-zA-Z]{2,}");
            Matcher m = emailPattern.matcher(evidence);
            if (m.find()) {
                return m.group();
            }
        }
        return null;
    }

    /**
     * Extract ad_id from identifier evidence.
     */
    private String extractAdId(EvalSession session) {
        String evidence = session.getIdentifierEvidence();
        if (StringUtils.isNotBlank(evidence)) {
            // Look for "ad ID" or "ad_id" followed by a value
            Pattern adPattern = Pattern.compile("(?:ad[_ ]?(?:ID|id)|listing[_ ]?(?:ID|id))\\s*[:|]?\\s*(\\S+)");
            Matcher m = adPattern.matcher(evidence);
            if (m.find()) {
                return m.group(1);
            }
        }
        return null;
    }
}
