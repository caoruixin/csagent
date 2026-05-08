package com.gumtree.csagent.service.runtime;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gumtree.csagent.model.BotSession;
import com.gumtree.csagent.model.DriftResult;
import com.gumtree.csagent.model.IntentClassification;
import com.gumtree.csagent.model.IntentClassification.IntentRelation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Sprint 10 §L0 — runtime-internal intent classifier.
 *
 * <p>Consumed by {@link ControlKernel#processMessage} BEFORE
 * {@link PhaseEvaluator#plan(BotSession, String, java.util.List)} so the
 * kernel can decide whether to continue the current issue, soft-shift to
 * another low-risk UC, enter a high-risk intake/handover path, or
 * escalate. The output is wrapped in a {@link com.gumtree.csagent.model.RerouteDecision}
 * by {@link RerouteDecider}.
 *
 * <p><b>Constraints (Sprint 10 spec):</b>
 * <ul>
 *   <li>Not an agent-visible tool surface. Never registered with the
 *       LLM tool schema; never invoked via {@code classify_use_case}.</li>
 *   <li>Reuses existing deterministic matchers ({@link EscalationReasonResolver}
 *       distress / explicit-human, {@link DriftDetector} hard-shift)
 *       plus narrow Sprint 10 MVP regex shapes. Does NOT call the LLM
 *       routing service.</li>
 *   <li>Supports only the Sprint 10 MVP cases; unrecognised messages
 *       return {@link IntentRelation#UNKNOWN} so the kernel leaves
 *       state untouched.</li>
 *   <li>Preserves the payment-ambiguity negative guard: a UC-A session
 *       saying "I paid for Top Ad but it's not showing" stays UC-A,
 *       NOT UC-I.</li>
 * </ul>
 */
@Slf4j
@Service
public class RuntimeIntentClassifier {

    /** Sprint 10 §L0 — UC-A → UC-C soft shift signal ("I haven't got replies"). */
    private static final Pattern UC_C_NEW_REPLIES_PATTERN = Pattern.compile(
            "\\b("
                    + "haven'?t\\s+(?:got|received|had)\\s+(?:any\\s+)?(?:replies|responses|messages)"
                    + "|no\\s+(?:replies|responses|messages)"
                    + "|not\\s+(?:getting|receiving)\\s+(?:any\\s+)?(?:replies|responses|messages)"
                    + "|nobody\\s+(?:has\\s+)?(?:replied|responded|messaged)"
                    + "|no\\s+one\\s+(?:has\\s+)?(?:replied|responded|messaged)"
                    + ")\\b",
            Pattern.CASE_INSENSITIVE);

    /** Sprint 10 §L0 — UC-A same-issue rebound ("I still can't see my ad"). */
    private static final Pattern UC_A_SAME_ISSUE_PATTERN = Pattern.compile(
            "\\b("
                    + "still\\s+(?:can'?t|cannot|unable\\s+to|don'?t|do\\s+not)\\s+see\\s+(?:my\\s+)?(?:ad|advert|listing|posting|post)"
                    + "|(?:ad|advert|listing|posting)\\s+(?:is\\s+)?still\\s+(?:not|missing|invisible|hidden|gone)"
                    + "|still\\s+(?:not|isn'?t|aren'?t)\\s+(?:showing|appearing|visible|live)"
                    + "|(?:my\\s+)?(?:ad|advert|listing|posting)\\s+still\\s+(?:not|isn'?t)\\s+(?:showing|appearing|visible|live)"
                    + ")\\b",
            Pattern.CASE_INSENSITIVE);

    /** Sprint 10 §L0 — UC-A same-UC follow-up ("how long is it active for?"). */
    private static final Pattern UC_A_FOLLOWUP_DURATION_PATTERN = Pattern.compile(
            "\\b("
                    + "how\\s+long\\s+(?:is|does|will|do)\\s+(?:it|my\\s+(?:ad|advert|listing|posting))\\s+(?:active|live|stay|last|remain)"
                    + "|how\\s+long\\s+(?:does|do)\\s+(?:my\\s+)?(?:ad|advert|listing|posting)s?\\s+last"
                    + "|when\\s+(?:does|will)\\s+(?:my\\s+)?(?:ad|advert|listing|posting)\\s+expire"
                    + "|expir(?:e|y|ation)\\s+(?:date|time)\\s+(?:of|for)\\s+(?:my\\s+)?(?:ad|advert|listing|posting)"
                    + ")\\b",
            Pattern.CASE_INSENSITIVE);

    /** Sprint 10 §L0 — UC-J risk shift ("I was scammed", fraud, harassment). */
    private static final Pattern UC_J_RISK_SHIFT_PATTERN = Pattern.compile(
            "\\b("
                    + "(?:i\\s+(?:was|am|got)|been)\\s+(?:scammed|defrauded|conned|cheated|catfished)"
                    + "|(?:this\\s+is\\s+a\\s+|its?\\s+a\\s+|it's\\s+a\\s+)?(?:scam|fraud)\\b"
                    + "|fraudulent\\s+(?:listing|seller|buyer|account|ad|transaction)"
                    + "|(?:reporting|report)\\s+(?:a\\s+)?(?:scam|fraud|scammer)"
                    + "|seller\\s+(?:scammed|defrauded|stole|cheated)"
                    + "|harassment|harassed|threatened|threatening|abusive"
                    + ")\\b",
            Pattern.CASE_INSENSITIVE);

    /**
     * Sprint 11 §M0 — ad_id user-message capture. Same numeric shape used
     * by the {@code PiiRedactionFilter} ({@code \\d{10,}}) plus a
     * dash-prefixed variant ({@code AD-12345...}) and an inline
     * "ad id: 123..." form. Used to reuse {@code primary_entity} across
     * same-UC follow-up turns when the user types the listing ID directly
     * into the chat.
     */
    private static final Pattern AD_ID_FROM_USER_MESSAGE_PATTERN = Pattern.compile(
            "(?:^|[^\\w])("
                    + "AD-\\d{4,}"
                    + "|\\d{10,}"
                    + ")(?=$|[^\\w])",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern AD_ID_LABELED_PATTERN = Pattern.compile(
            "\\b(?:ad(?:vert)?[-\\s]?id|advert\\s+id|ad\\s+number|listing\\s+id)\\s*[:#=]?\\s*"
                    + "(AD-\\d{4,}|\\d{4,})",
            Pattern.CASE_INSENSITIVE);

    /**
     * Sprint 10 §L0 — payment-ambiguity negative guard.
     * "I paid for Top Ad but it's not showing" / "paid for promotion but no
     * boost" must NOT route to UC-I (Payments). The user paid for an ad
     * boost / Top-Ad / promotion product and the visibility outcome failed
     * — the live intent is UC-A (Ad Status &amp; Visibility), not a
     * payment dispute. UC-I is reserved for refund / chargeback / payment
     * dispute shapes covered elsewhere.
     */
    private static final Pattern PAYMENT_AMBIGUITY_AD_VISIBILITY_PATTERN = Pattern.compile(
            "\\b("
                    + "paid\\s+for\\s+(?:top[-\\s]?ad|promot(?:e|ed|ion|ional)|featured|bump|boost|premium|spotlight)"
                    + "|bought\\s+(?:top[-\\s]?ad|promot(?:e|ed|ion|ional)|featured|bump|boost|premium|spotlight)"
                    + "|purchased\\s+(?:top[-\\s]?ad|promot(?:e|ed|ion|ional)|featured|bump|boost|premium|spotlight)"
                    + ")\\b.*\\b("
                    + "not\\s+(?:showing|appearing|visible|live|active)"
                    + "|isn'?t\\s+(?:showing|appearing|visible|live|active)"
                    + "|hasn'?t\\s+(?:shown|appeared|gone\\s+live)"
                    + "|don'?t\\s+see"
                    + "|can'?t\\s+see"
                    + ")\\b",
            Pattern.CASE_INSENSITIVE);

    /** Active UC tokens that are FAQ-class and considered low-risk for soft shift. */
    private static final java.util.Set<String> LOW_RISK_FAQ_UCS = java.util.Set.of(
            "UC-A", "UC-B", "UC-C", "UC-D", "UC-E", "UC-F", "UC-FP");

    /** Active UC tokens that require intake (high-risk class). */
    private static final java.util.Set<String> HIGH_RISK_INTAKE_UCS = java.util.Set.of(
            "UC-G", "UC-H", "UC-I", "UC-J", "UC-K");

    private final EscalationReasonResolver escalationResolver;
    private final ObjectMapper objectMapper;

    public RuntimeIntentClassifier(EscalationReasonResolver escalationResolver,
                                   ObjectMapper objectMapper) {
        this.escalationResolver = escalationResolver;
        this.objectMapper = objectMapper;
    }

    /**
     * Classify the current user turn relative to the active session state.
     *
     * <p>Inputs are kept minimal: the current session, the current user
     * message, and the {@link DriftResult} the kernel already produced
     * via {@link DriftDetector}. The drift result is used as one signal
     * (the existing hard-shift keyword set is a useful fallback for
     * shifts the Sprint 10 regexes do not cover yet). The classifier
     * returns {@link IntentRelation#UNKNOWN} when no Sprint 10 MVP shape
     * matches — the kernel then leaves state untouched and the planner
     * runs as today.
     */
    public IntentClassification classify(BotSession session, String userMessage,
                                         DriftResult driftResult) {
        if (userMessage == null || userMessage.isBlank()) {
            return IntentClassification.unknown();
        }
        String activeUc = session == null ? null : session.getActiveUseCase();

        // 1. Distress / human-request precedence: surface the relation so
        //    callers can record observability, but the kernel's existing
        //    step 2.4 / 2.5 / DriftDetector USER_ESCALATION_REQUEST paths
        //    are what actually drive the escalation. We never override
        //    those — the relation is informational.
        if (escalationResolver != null && escalationResolver.detectExplicitUserEscalation(userMessage)) {
            return IntentClassification.of(activeUc, 0.95, IntentRelation.HUMAN_REQUEST);
        }
        if (escalationResolver != null && escalationResolver.detectDistressSignal(userMessage)) {
            return IntentClassification.of(activeUc, 0.90, IntentRelation.CRITICAL_ESCALATION);
        }

        // 2. Sprint 10 MVP — UC-J risk shift on fraud / scam / harassment
        //    intent. Returns NEW_HIGH_RISK_UC even when the active UC is
        //    already UC-J so the kernel can rebound into RESOLVE_INTAKE
        //    rather than the FAQ loop.
        if (UC_J_RISK_SHIFT_PATTERN.matcher(userMessage).find()) {
            // Same-UC re-assertion still maps to NEW_HIGH_RISK_UC for
            // intent-routing purposes; the decision layer interprets it
            // as REBOUND_TO_RESOLVE when targetUc == activeUc.
            return IntentClassification.of("UC-J", 0.88, IntentRelation.NEW_HIGH_RISK_UC,
                    "fraud_or_safety_intake", null, null);
        }

        // 3. Sprint 10 MVP — UC-A → UC-C soft shift ("I haven't got replies").
        //    Only fires when the message clearly references replies /
        //    messages and the user is NOT also asserting an ad-visibility
        //    issue that would belong to UC-A. Returns NEW_LOW_RISK_UC for
        //    UC-A; for sessions already on UC-C this becomes SAME_ISSUE.
        if (UC_C_NEW_REPLIES_PATTERN.matcher(userMessage).find()) {
            if ("UC-C".equals(activeUc)) {
                return IntentClassification.of("UC-C", 0.85, IntentRelation.SAME_ISSUE,
                        "messaging_diagnostic", null, null);
            }
            return IntentClassification.of("UC-C", 0.80, IntentRelation.NEW_LOW_RISK_UC,
                    "messaging_diagnostic", null, null);
        }

        // 4. Sprint 10 §L0 payment-ambiguity NEGATIVE guard.
        //    "I paid for Top Ad but it's not showing" must not route to
        //    UC-I; it stays UC-A (visibility intent). Run BEFORE the
        //    same-issue regex so we attach a useful task type.
        boolean adVisibilityPaymentAmbiguity =
                PAYMENT_AMBIGUITY_AD_VISIBILITY_PATTERN.matcher(userMessage).find();

        // 5. Sprint 10 MVP — UC-A same-issue rebound ("I still can't see my
        //    ad") and UC-A same-UC follow-up ("how long is it active for?").
        boolean sameIssueAd = UC_A_SAME_ISSUE_PATTERN.matcher(userMessage).find();
        boolean uc_a_followup = UC_A_FOLLOWUP_DURATION_PATTERN.matcher(userMessage).find();

        if (sameIssueAd || adVisibilityPaymentAmbiguity) {
            String taskType = adVisibilityPaymentAmbiguity
                    ? "listing_visibility_paid_promotion"
                    : "listing_visibility_diagnostic";
            String adId = preferAdId(readAdIdFromForm(session),
                    extractAdIdFromUserMessage(userMessage));
            // Always SAME_ISSUE for ad-visibility shape: predicted UC = UC-A.
            // If the active UC is already UC-A, this rebounds CONFIRM →
            // RESOLVE on the same UC. If the active UC is something else
            // but the user's intent now is clearly ad-visibility, it
            // becomes a NEW_LOW_RISK_UC reroute to UC-A.
            if (activeUc == null || "UC-A".equals(activeUc)) {
                return IntentClassification.of("UC-A", 0.85, IntentRelation.SAME_ISSUE,
                        taskType, adId == null ? null : "listing", adId);
            }
            return IntentClassification.of("UC-A", 0.80, IntentRelation.NEW_LOW_RISK_UC,
                    taskType, adId == null ? null : "listing", adId);
        }

        if (uc_a_followup) {
            String adId = preferAdId(readAdIdFromForm(session),
                    extractAdIdFromUserMessage(userMessage));
            if (activeUc == null || "UC-A".equals(activeUc)) {
                return IntentClassification.of("UC-A", 0.80, IntentRelation.SAME_UC_NEW_TASK,
                        "listing_lifecycle_followup",
                        adId == null ? null : "listing", adId);
            }
            return IntentClassification.of("UC-A", 0.70, IntentRelation.NEW_LOW_RISK_UC,
                    "listing_lifecycle_followup",
                    adId == null ? null : "listing", adId);
        }

        // 6. DriftDetector hard-shift fallback. The legacy detector covers
        //    additional shifts (refund→UC-I, GDPR→UC-G, ad removed→UC-H).
        //    We surface them here as NEW_HIGH_RISK_UC for UC-G/H/J and
        //    NEW_LOW_RISK_UC for UC-I (still FAQ class until intake fields
        //    are required). The kernel's decision layer maps those to the
        //    appropriate plan (intake vs FAQ) using the same UC-set
        //    membership rule used elsewhere.
        if (driftResult != null
                && driftResult.getType() == DriftResult.DriftType.HARD_SHIFT
                && driftResult.getNewUseCase() != null
                && !driftResult.getNewUseCase().equals(activeUc)) {
            String target = driftResult.getNewUseCase();
            IntentRelation rel = HIGH_RISK_INTAKE_UCS.contains(target)
                    ? IntentRelation.NEW_HIGH_RISK_UC
                    : IntentRelation.NEW_LOW_RISK_UC;
            return IntentClassification.of(target, 0.70, rel,
                    "drift_detector_hard_shift", null, null);
        }

        return IntentClassification.unknown();
    }

    private String readAdIdFromForm(BotSession session) {
        if (session == null || session.getFormContext() == null
                || session.getFormContext().isBlank()) {
            return null;
        }
        try {
            JsonNode node = objectMapper.readTree(session.getFormContext());
            JsonNode ad = node.get("ad_id");
            if (ad != null && !ad.isNull()) {
                String s = ad.asText(null);
                if (s != null && !s.isBlank()) {
                    return s;
                }
            }
        } catch (Exception ex) {
            log.debug("RuntimeIntentClassifier: failed to read ad_id from form_context: {}",
                    ex.getMessage());
        }
        return null;
    }

    /**
     * Sprint 11 §M0 — extract an ad_id from the user's chat message. The
     * progressive UC-A flow asks the user to provide their advert ID
     * after a soft "send the advert ID" answer; on the next turn the
     * user may type the ID without any re-stated context. Capturing it
     * here lets the runtime stamp {@code primary_entity} so subsequent
     * follow-up turns reuse the entity instead of re-asking.
     *
     * <p>Visible for unit tests so the Sprint 11 progressive-resolve
     * regression can pin the regex shapes.
     */
    static String extractAdIdFromUserMessage(String userMessage) {
        if (userMessage == null || userMessage.isBlank()) {
            return null;
        }
        java.util.regex.Matcher labeled = AD_ID_LABELED_PATTERN.matcher(userMessage);
        if (labeled.find()) {
            return labeled.group(1);
        }
        java.util.regex.Matcher numeric = AD_ID_FROM_USER_MESSAGE_PATTERN.matcher(userMessage);
        if (numeric.find()) {
            return numeric.group(1);
        }
        return null;
    }

    /**
     * Sprint 11 §M0 — same-UC ad_id capture. When the user message has
     * no Sprint 10 MVP shape but contains a bare ad_id, surface it so
     * {@code ControlKernel.applyRerouteDecision} can persist it on
     * {@code session.primaryEntity} for the next same-UC follow-up
     * turn. Returns {@code null} when no ad_id is present in the
     * message.
     */
    public String captureSameUcAdIdHint(BotSession session, String userMessage) {
        if (session == null || userMessage == null || userMessage.isBlank()) {
            return null;
        }
        String activeUc = session.getActiveUseCase();
        // Only capture for FAQ-class UCs that benefit from progressive
        // resolve (UC-A in particular). High-risk intake UCs already
        // capture entities via the intake_fields / handover payload.
        if (!"UC-A".equals(activeUc)) {
            return null;
        }
        return extractAdIdFromUserMessage(userMessage);
    }

    /** Prefer the user-supplied ad_id over a stale form_context entry. */
    private static String preferAdId(String fromForm, String fromUserMessage) {
        if (fromUserMessage != null && !fromUserMessage.isBlank()) {
            return fromUserMessage;
        }
        return fromForm;
    }

    static boolean isLowRiskFaqUc(String uc) {
        return uc != null && LOW_RISK_FAQ_UCS.contains(uc);
    }

    static boolean isHighRiskIntakeUc(String uc) {
        return uc != null && HIGH_RISK_INTAKE_UCS.contains(uc);
    }

    /** Visible for unit tests so we can pin the Sprint 10 regex shapes. */
    static boolean matchesUcCNewReplies(String userMessage) {
        return userMessage != null && UC_C_NEW_REPLIES_PATTERN.matcher(userMessage).find();
    }

    static boolean matchesUcASameIssue(String userMessage) {
        return userMessage != null && UC_A_SAME_ISSUE_PATTERN.matcher(userMessage).find();
    }

    static boolean matchesUcAFollowup(String userMessage) {
        return userMessage != null && UC_A_FOLLOWUP_DURATION_PATTERN.matcher(userMessage).find();
    }

    static boolean matchesUcJRiskShift(String userMessage) {
        return userMessage != null && UC_J_RISK_SHIFT_PATTERN.matcher(userMessage).find();
    }

    static boolean matchesPaymentAmbiguityAdVisibility(String userMessage) {
        return userMessage != null
                && PAYMENT_AMBIGUITY_AD_VISIBILITY_PATTERN.matcher(userMessage.toLowerCase(Locale.ENGLISH)).find();
    }
}
