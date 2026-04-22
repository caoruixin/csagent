package com.gumtree.csagent.service.guardrails;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Regex-based detection of forbidden phrases in bot responses.
 * All patterns are compiled at class initialization for performance.
 *
 * Categories align with docs/fixed_script_library_v1.md Section 8.
 */
@Slf4j
@Service
public class ForbiddenPhraseDetector {

    /** Map of category -> list of compiled patterns */
    private static final Map<String, List<Pattern>> FORBIDDEN_PATTERNS;

    /** Safe replacement text per category for sanitization */
    private static final Map<String, String> SAFE_REPLACEMENTS;

    static {
        FORBIDDEN_PATTERNS = new LinkedHashMap<>();

        // Identity impersonation — bot must not pretend to be human
        FORBIDDEN_PATTERNS.put("identity_impersonation", List.of(
                Pattern.compile("(?i)hello,?\\s+my name is"),
                Pattern.compile("(?i)you are speaking to a human"),
                Pattern.compile("(?i)i am a real human")
        ));

        // False action — bot cannot claim to have performed enforcement actions
        FORBIDDEN_PATTERNS.put("false_action", List.of(
                Pattern.compile("(?i)i'?ve?\\s+(fixed|removed|deleted|restricted|lifted|processed)"),
                Pattern.compile("(?i)your restriction is lifted"),
                Pattern.compile("(?i)the ads have now been removed")
        ));

        // False promise — bot cannot make financial commitments
        FORBIDDEN_PATTERNS.put("false_promise", List.of(
                Pattern.compile("(?i)i'?ll process your refund"),
                Pattern.compile("(?i)your refund has been issued"),
                Pattern.compile("(?i)i can guarantee")
        ));

        // Fake empathy — sounds insincere from bot
        FORBIDDEN_PATTERNS.put("fake_empathy", List.of(
                Pattern.compile("(?i)i understand how you feel")
        ));

        // AI disclosure — must not use technical AI terminology
        FORBIDDEN_PATTERNS.put("ai_disclosure", List.of(
                Pattern.compile("(?i)as an ai( language model)?")
        ));

        // Data promise — GDPR operations are handled by specialist team
        FORBIDDEN_PATTERNS.put("data_promise", List.of(
                Pattern.compile("(?i)i have deleted your data"),
                Pattern.compile("(?i)your account has been erased")
        ));

        // Authority claim — bot must not claim seniority or authority
        FORBIDDEN_PATTERNS.put("authority_claim", List.of(
                Pattern.compile("(?i)i'?m?\\s+(a|the)\\s+(manager|supervisor|senior)")
        ));

        // Legal advice — bot must not give legal recommendations
        FORBIDDEN_PATTERNS.put("legal_advice", List.of(
                Pattern.compile("(?i)you should (sue|take legal action|contact a lawyer)")
        ));

        // Contact info — bot must not fabricate contact details
        FORBIDDEN_PATTERNS.put("contact_info", List.of(
                Pattern.compile("(?i)(call|email|contact)\\s+us\\s+at\\s+[\\w@.]+")
        ));

        // Safe replacement text for each category
        SAFE_REPLACEMENTS = Map.of(
                "identity_impersonation", "I'm the Gumtree Support Assistant.",
                "false_action", "I've passed this to our specialist team for action.",
                "false_promise", "I'll pass this to our specialist team who handles these requests.",
                "fake_empathy", "I understand your concern.",
                "ai_disclosure", "I'm the Gumtree Support Assistant.",
                "data_promise", "Our privacy team will process your data request.",
                "authority_claim", "I'm the Gumtree Support Assistant.",
                "legal_advice", "For legal matters, please seek independent advice.",
                "contact_info", "Please visit our Help Centre for contact details."
        );
    }

    /**
     * Check a bot response for all forbidden phrases.
     *
     * @param botResponse the bot response text to check
     * @return list of all forbidden phrase matches found (empty if none)
     */
    public List<ForbiddenPhraseMatch> check(String botResponse) {
        if (botResponse == null || botResponse.isEmpty()) {
            return Collections.emptyList();
        }

        List<ForbiddenPhraseMatch> matches = new ArrayList<>();

        for (Map.Entry<String, List<Pattern>> entry : FORBIDDEN_PATTERNS.entrySet()) {
            String category = entry.getKey();
            for (Pattern pattern : entry.getValue()) {
                Matcher matcher = pattern.matcher(botResponse);
                while (matcher.find()) {
                    matches.add(new ForbiddenPhraseMatch(
                            category,
                            matcher.group(),
                            matcher.start()
                    ));
                    log.warn("Forbidden phrase detected [{}]: '{}' at position {}",
                            category, matcher.group(), matcher.start());
                }
            }
        }

        return matches;
    }

    /**
     * Quick check whether the bot response contains any forbidden phrases.
     *
     * @param botResponse the bot response text to check
     * @return true if any forbidden phrase is found
     */
    public boolean hasForbiddenPhrases(String botResponse) {
        if (botResponse == null || botResponse.isEmpty()) {
            return false;
        }

        for (List<Pattern> patterns : FORBIDDEN_PATTERNS.values()) {
            for (Pattern pattern : patterns) {
                if (pattern.matcher(botResponse).find()) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Sanitize a bot response by replacing forbidden phrases with safe alternatives.
     * Each forbidden phrase match is replaced with the category-specific safe text.
     *
     * @param botResponse the bot response text to sanitize
     * @return sanitized text with forbidden phrases replaced
     */
    public String sanitize(String botResponse) {
        if (botResponse == null || botResponse.isEmpty()) {
            return botResponse;
        }

        String result = botResponse;

        for (Map.Entry<String, List<Pattern>> entry : FORBIDDEN_PATTERNS.entrySet()) {
            String category = entry.getKey();
            String replacement = SAFE_REPLACEMENTS.getOrDefault(category, "[REDACTED]");

            for (Pattern pattern : entry.getValue()) {
                Matcher matcher = pattern.matcher(result);
                if (matcher.find()) {
                    log.info("Sanitizing forbidden phrase [{}] in bot response", category);
                    result = matcher.replaceAll(replacement);
                }
            }
        }

        return result;
    }
}
