package com.gumtree.csagent.model;

/**
 * Sprint 10 §L0 — output of the runtime-internal {@code RuntimeIntentClassifier}.
 *
 * <p>This is NOT an agent-visible tool surface. It is consumed by
 * {@code ControlKernel.processMessage} before {@code PhaseEvaluator.plan(...)}
 * to decide whether the user's current turn continues the active issue,
 * shifts to a new low-risk UC, enters a high-risk intake/handover path,
 * or escalates immediately.
 *
 * <p>Per Sprint 10 spec, only the MVP shapes are supported (UC-A → UC-C
 * soft shift, UC-A same issue, UC-A same UC follow-up, UC-A → UC-J risk
 * shift, explicit human request, payment ambiguity negative guard). All
 * other turns produce {@link IntentRelation#UNKNOWN} and the kernel
 * leaves the session untouched.
 *
 * @param predictedUseCase    UC the classifier believes the current turn
 *                            belongs to (may be the active UC for SAME_*
 *                            relations, or a new UC for shifts; null for
 *                            UNKNOWN).
 * @param confidence          [0.0, 1.0]; deterministic classifier uses
 *                            fixed values per pattern match.
 * @param relation            relation of this turn to the active issue.
 * @param taskType            optional descriptive task token (e.g.
 *                            {@code listing_visibility_diagnostic}); may be null.
 * @param primaryEntityType   optional primary entity type (e.g. {@code listing});
 *                            may be null.
 * @param primaryEntityValue  optional primary entity value (e.g. ad_id);
 *                            may be null.
 */
public record IntentClassification(
        String predictedUseCase,
        double confidence,
        IntentRelation relation,
        String taskType,
        String primaryEntityType,
        String primaryEntityValue
) {

    /** Sprint 10 §L0 — relation of the current turn to the active issue. */
    public enum IntentRelation {
        /** Same UC, same observable issue (e.g. "I still can't see my ad"). */
        SAME_ISSUE,
        /** Same UC, follow-up question on the same topic (e.g. "how long is it active for?"). */
        SAME_UC_NEW_TASK,
        /** Different low-risk UC (e.g. UC-A → UC-C "I haven't got replies"). */
        NEW_LOW_RISK_UC,
        /** Different high-risk UC requiring intake (e.g. UC-A → UC-J "I was scammed"). */
        NEW_HIGH_RISK_UC,
        /** Explicit "I want a human" request. Existing kernel path stamps user_requested. */
        HUMAN_REQUEST,
        /** Distress / imminent-harm signal. Existing kernel path stamps user_distress. */
        CRITICAL_ESCALATION,
        /** Classifier did not confidently match a Sprint 10 MVP shape. */
        UNKNOWN
    }

    /** Convenience constructor used by tests when only relation is interesting. */
    public static IntentClassification unknown() {
        return new IntentClassification(null, 0.0,
                IntentRelation.UNKNOWN, null, null, null);
    }

    public static IntentClassification of(String predictedUseCase, double confidence,
                                          IntentRelation relation) {
        return new IntentClassification(predictedUseCase, confidence, relation, null, null, null);
    }

    public static IntentClassification of(String predictedUseCase, double confidence,
                                          IntentRelation relation, String taskType,
                                          String entityType, String entityValue) {
        return new IntentClassification(predictedUseCase, confidence, relation,
                taskType, entityType, entityValue);
    }
}
