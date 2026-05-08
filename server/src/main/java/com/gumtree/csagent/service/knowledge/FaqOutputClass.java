package com.gumtree.csagent.service.knowledge;

/**
 * Sprint 14 §L2 — bot output class taxonomy.
 *
 * <p>See {@code docs/faq_grounding_contract.md} for the canonical
 * definitions and grounding-requirement table. Only
 * {@link #FACTUAL_ANSWER} requires grounding evidence (retrieved /
 * resolved / cited from {@link SourceEvidenceLineage}); the other
 * classes are observability-classified but never require citation.
 */
public enum FaqOutputClass {

    /** Bot states a fact about Gumtree policy, behaviour, fees, etc. */
    FACTUAL_ANSWER("factual_answer", true),

    /** Bot asks the user a clarifying question. */
    CLARIFICATION("clarification", false),

    /** Bot acknowledges user emotion / distress without making a claim. */
    EMPATHY_ACK("empathy_ack", false),

    /** Bot announces handover to a human agent. */
    HANDOVER("handover", false),

    /** Bot is reporting a tool / system status (looking into / checking). */
    TOOL_STATUS("tool_status", false),

    /** Bot is collecting required intake fields. */
    INTAKE_COLLECTION("intake_collection", false);

    private final String token;
    private final boolean requiresGrounding;

    FaqOutputClass(String token, boolean requiresGrounding) {
        this.token = token;
        this.requiresGrounding = requiresGrounding;
    }

    public String token() {
        return token;
    }

    /**
     * Sprint 14 §L2 — true iff this output class requires grounding
     * evidence. {@code factual_answer} is the only class that does;
     * everything else is exempt by definition.
     */
    public boolean requiresGrounding() {
        return requiresGrounding;
    }
}
