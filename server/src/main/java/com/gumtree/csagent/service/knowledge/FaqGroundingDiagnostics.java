package com.gumtree.csagent.service.knowledge;

/**
 * Sprint 14 §L2 — soft FAQ grounding diagnostics for one bot turn.
 *
 * <p>Combines {@link FaqOutputClass} with {@link SourceEvidenceLineage}
 * to compute the observability fields named in
 * {@code docs/faq_grounding_contract.md} §4:
 *
 * <ul>
 *   <li>{@link #faqGroundingState()} — coarse-grained state token.</li>
 *   <li>{@link #citationPresent()} — true iff the bot cited a source
 *       (any source_id or canonical URL).</li>
 *   <li>{@link #citationMatch()} — true iff every cited source_id is
 *       part of the resolved-or-retrieved candidate set this turn.</li>
 *   <li>{@link #citationDrift()} — true iff the bot cited a source that
 *       was NOT retrieved or resolved this turn (third-party drift or
 *       hallucination warning).</li>
 *   <li>{@link #resolvedButUncited()} — true iff at least one resolved
 *       article was not cited in the reply.</li>
 *   <li>{@link #retrievedButUnresolved()} — true iff at least one
 *       retrieved article was never resolved (potential §G2 grounded-
 *       resolve gap on a factual answer).</li>
 * </ul>
 *
 * <p>All fields are observability-only. Sprint 14 §L2 does NOT use any
 * of them to gate, rewrite, or loop the reply.
 */
public record FaqGroundingDiagnostics(
        FaqOutputClass outputClass,
        String faqGroundingState,
        boolean citationPresent,
        boolean citationMatch,
        boolean citationDrift,
        boolean resolvedButUncited,
        boolean retrievedButUnresolved
) {

    public static final String STATE_FACTUAL_GROUNDED = "factual_grounded";
    public static final String STATE_FACTUAL_UNCITED = "factual_uncited";
    public static final String STATE_FACTUAL_UNRESOLVED = "factual_unresolved";
    public static final String STATE_FACTUAL_UNRETRIEVED = "factual_unretrieved";
    public static final String STATE_NON_FACTUAL = "non_factual";
    public static final String STATE_UNKNOWN = "unknown";

    /**
     * Compute the diagnostics for a turn from its classification + evidence
     * lineage. Tolerant of nulls (returns the {@code unknown} state).
     */
    public static FaqGroundingDiagnostics compute(FaqOutputClass outputClass,
                                                    SourceEvidenceLineage lineage) {
        if (lineage == null) lineage = SourceEvidenceLineage.empty();
        if (outputClass == null) {
            return new FaqGroundingDiagnostics(null, STATE_UNKNOWN,
                    false, false, false, false, false);
        }

        boolean citationPresent = lineage.citedAny();
        boolean retrievedButUnresolved = !lineage.retrievedButUnresolvedSourceIds().isEmpty();
        boolean resolvedButUncited = !lineage.resolvedButUncitedSourceIds().isEmpty();

        // citation_match / citation_drift only make sense when the bot
        // cited a source_id this turn. URL-only citations to third-party
        // domains (e.g. gov.uk) count as drift, since the runtime did not
        // ground them.
        boolean citationMatch = false;
        boolean citationDrift = false;
        if (!lineage.citedSourceIds().isEmpty()) {
            boolean anyMatch = false;
            boolean anyDrift = false;
            for (String cited : lineage.citedSourceIds()) {
                if (lineage.resolvedSourceIds().contains(cited)
                        || lineage.retrievedSourceIds().contains(cited)) {
                    anyMatch = true;
                } else {
                    anyDrift = true;
                }
            }
            citationMatch = anyMatch;
            citationDrift = anyDrift;
        }
        if (!lineage.citedCanonicalUrls().isEmpty()) {
            // A free-form URL citation by definition was not in the
            // candidate canonical_url map, so it counts as drift.
            citationDrift = true;
        }

        String state;
        if (!outputClass.requiresGrounding()) {
            state = STATE_NON_FACTUAL;
        } else if (citationPresent && citationMatch && !citationDrift) {
            state = STATE_FACTUAL_GROUNDED;
        } else if (lineage.resolvedAny() && !citationPresent) {
            state = STATE_FACTUAL_UNCITED;
        } else if (lineage.retrievedAny() && !lineage.resolvedAny()) {
            state = STATE_FACTUAL_UNRESOLVED;
        } else if (!lineage.retrievedAny()) {
            state = STATE_FACTUAL_UNRETRIEVED;
        } else {
            state = STATE_UNKNOWN;
        }

        return new FaqGroundingDiagnostics(
                outputClass,
                state,
                citationPresent,
                citationMatch,
                citationDrift,
                resolvedButUncited,
                retrievedButUnresolved);
    }
}
