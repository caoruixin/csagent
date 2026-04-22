package com.gumtree.csagent.service.guardrails;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Represents a single forbidden phrase match found in a bot response.
 */
@Data
@AllArgsConstructor
public class ForbiddenPhraseMatch {

    /** The category of the forbidden phrase (e.g. "identity_impersonation", "false_action") */
    private final String category;

    /** The actual text that matched the forbidden pattern */
    private final String matchedText;

    /** The start position (index) of the match in the original text */
    private final int startPosition;
}
