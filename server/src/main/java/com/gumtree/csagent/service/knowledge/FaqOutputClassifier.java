package com.gumtree.csagent.service.knowledge;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Sprint 14 §L2 — classify a bot reply into one of the
 * {@link FaqOutputClass} taxonomy values.
 *
 * <p>The classifier is intentionally heuristic and conservative: it never
 * gates the response, it never decides escalation, and it explicitly
 * defaults to {@link FaqOutputClass#FACTUAL_ANSWER} only when no
 * lower-grounding shape matches — a deliberate choice so the §L2
 * grounding diagnostics err on the side of "this turn might need
 * grounding evidence" rather than silently exempting a borderline reply.
 *
 * <p>Stateless / side-effect-free; safe to invoke from
 * {@code ControlKernel.recordRunResult}.
 */
public final class FaqOutputClassifier {

    private static final Pattern HANDOVER_HINT = Pattern.compile(
            "(?i)\\b(connect(ing)?\\s+you|transfer(ring)?\\s+you|hand(ing)?\\s*over|"
                    + "human\\s+agent|live\\s+agent|specialist|representative|customer\\s+service\\s+team)\\b");

    private static final Pattern TOOL_STATUS_HINT = Pattern.compile(
            "(?i)\\b(looking\\s+into\\s+(this|it)|let\\s+me\\s+(check|look)|"
                    + "checking\\s+(now|on|that)|one\\s+moment|"
                    + "i\\s*'?ll\\s+(check|look)|please\\s+wait)\\b");

    private static final Pattern EMPATHY_HINT = Pattern.compile(
            "(?i)\\b(i\\s*'?m\\s+(so\\s+)?sorry|that\\s+sounds\\s+(really\\s+)?(frustrating|tough|stressful)|"
                    + "i\\s+understand\\s+(how|that)|i\\s+can\\s+see\\s+why)\\b");

    private static final Pattern INTAKE_HINT = Pattern.compile(
            "(?i)\\b(could\\s+you\\s+(please\\s+)?(provide|share|confirm)|"
                    + "can\\s+you\\s+(provide|share|tell\\s+me|confirm)|"
                    + "what\\s+is\\s+(your|the)\\s+(ad\\s+id|ad-id|order\\s+id|email|phone)|"
                    + "please\\s+(provide|share|send)|"
                    + "to\\s+help\\s+(further|with\\s+this)\\s+i\\s+(need|will\\s+need))\\b");

    private FaqOutputClassifier() {}

    public static FaqOutputClass classify(String botResponse, ClassifierContext context) {
        if (botResponse == null || botResponse.isBlank()) {
            return FaqOutputClass.TOOL_STATUS;
        }
        if (context == null) context = ClassifierContext.empty();

        // Hard signals from the runtime trump heuristic word matching.
        if (context.handoverDispatched()) {
            return FaqOutputClass.HANDOVER;
        }

        String text = botResponse.trim();
        String lower = text.toLowerCase(Locale.ROOT);
        boolean endsWithQuestionMark = text.endsWith("?");

        if (HANDOVER_HINT.matcher(text).find()) {
            return FaqOutputClass.HANDOVER;
        }
        if (TOOL_STATUS_HINT.matcher(text).find() && lower.length() < 200) {
            return FaqOutputClass.TOOL_STATUS;
        }
        if (EMPATHY_HINT.matcher(text).find() && !endsWithQuestionMark
                && lower.length() < 200) {
            return FaqOutputClass.EMPATHY_ACK;
        }
        if (context.intakeUseCase() && (INTAKE_HINT.matcher(text).find() || endsWithQuestionMark)) {
            return FaqOutputClass.INTAKE_COLLECTION;
        }
        if (endsWithQuestionMark || INTAKE_HINT.matcher(text).find()) {
            return FaqOutputClass.CLARIFICATION;
        }
        return FaqOutputClass.FACTUAL_ANSWER;
    }

    /**
     * Sprint 14 §L2 — minimal classifier context. {@code intakeUseCase}
     * lets the classifier prefer {@link FaqOutputClass#INTAKE_COLLECTION}
     * over {@link FaqOutputClass#CLARIFICATION} when the active UC is
     * UC-G/H/I/J/K. {@code handoverDispatched} forces
     * {@link FaqOutputClass#HANDOVER} regardless of wording — the
     * runtime is the source of truth on whether the customer was
     * transferred.
     */
    public record ClassifierContext(boolean intakeUseCase, boolean handoverDispatched) {
        public static ClassifierContext empty() {
            return new ClassifierContext(false, false);
        }
    }
}
