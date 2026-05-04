package com.gumtree.csagent.service.runtime;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gumtree.csagent.model.BotSession;
import com.gumtree.csagent.model.LlmResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Two-stage use case routing:
 * Stage 1: Check topic_subject against strong priors, handover-only topics
 * Stage 2: Use LLM to classify description against UC candidates
 */
@Slf4j
@Service
public class UseCaseRouter {

    /**
     * Description-based UC overrides for handover-only Topic Subjects.
     * Source: phase2 §2.11.4 + fixed_script_library_v1 §9.1 — "先用 Description
     * 文本做意图分类... 若 Description 匹配已有 UC（如 Delivery + 'scammed' →
     * UC-J-01）→ 路由到该 UC". The router must check these patterns before
     * falling through to OUT_OF_SCOPE_*.
     *
     * <p>Order of patterns inside each list matters: first match wins. Patterns
     * use case-insensitive regex with word-boundary anchors so partial-word
     * matches (e.g. "ratings" inside "rating") don't trigger.
     */
    private static final Pattern FRAUD_SAFETY_PATTERN = Pattern.compile(
            "\\b(scam|scammed|scammer|fraud|fraudulent|stolen|theft|" +
                    "harass|threat|abusive|abuse|impersonat|catfish|phish)\\w*",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern PAYMENT_DISPUTE_PATTERN = Pattern.compile(
            "\\b(refund|chargeback|charged?\\s*back|dispute|" +
                    "money\\s*back|paid\\s*\\S{0,4}\\s*(but|and|never)|" +
                    "pay\\s*and\\s*ship|inad|item\\s*not\\s*as\\s*described)\\w*",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern TECH_FAILURE_PATTERN = Pattern.compile(
            "\\b(error|crash|bug|broken|not\\s*work|doesn'?t\\s*work|" +
                    "can'?t\\s*(?:leave|post|submit|edit)|won'?t\\s*load|" +
                    "fail(?:ing|ed)?|glitch)\\w*",
            Pattern.CASE_INSENSITIVE);

    /**
     * UC-K technical-regression detector (Sprint §A2, codex round 6 §F-2 follow-up).
     * Routes a description to UC-K when the user describes an in-app
     * regression — a feature / button / option that <i>used to work</i>,
     * is <i>missing / disappeared / greyed out</i>, or is now erroring.
     *
     * <p>The pattern intentionally requires a regression signal, not just
     * a feature mention. "How do contact options work?" stays on UC-E
     * (FAQ); "the phone-number contact option disappeared" / "the button
     * is greyed out" / "it used to work on Android" / "I get an error
     * when enabling phone contact" all flip to UC-K.
     */
    private static final Pattern UC_K_REGRESSION_PATTERN = Pattern.compile(
            "\\b("
                    // disappeared / missing / not appearing — explicit feature loss.
                    + "(?:has\\s+)?(?:disappeared|gone\\s*missing|vanished)"
                    + "|(?:option|button|feature|field|tab|link|page)\\s+is\\s+(?:missing|gone|unavailable)"
                    // "not getting / seeing / receiving" must target a UI surface
                    // (option / button / etc.) so we don't over-match general
                    // complaints like "not getting messages on my app". Keep
                    // "the / an / any / a" between verb and target so the
                    // pattern only fires on a specific feature mention.
                    + "|not\\s+(?:getting|seeing|receiving)\\s+(?:the|an|any|a)\\s+"
                    + "(?:option|button|feature|tab|link|page|prompt|toggle|checkbox|setting|control)"
                    // visual disabled / greyed-out states
                    + "|grey(?:ed)?\\s*[- ]?out|gray(?:ed)?\\s*[- ]?out"
                    // regression / used-to-work — broader "used to <verb>"
                    // catches "used to be on the listing" / "used to allow"
                    // / "used to show".
                    + "|used\\s+to\\s+(?:work|appear|show|be\\b|let\\s+me|allow|let|display)"
                    + "|stopped\\s+working|no\\s+longer\\s+works?|won'?t\\s+(?:work|load|open)"
                    // "can't see the [adjective] option/button/..." — strict
                    // noun pairing so "cant see where it can be changed"
                    // (cs_095) does NOT fire.
                    + "|(?:cannot|can'?t|unable\\s+to)\\s+see\\s+(?:the|an|any|a)\\s+"
                    + "(?:\\w+\\s+){0,2}(?:option|button|feature|tab|link|page|toggle|prompt)"
                    // platform / app-specific failures
                    + "|app\\s+(?:keeps|crashes|won'?t|wont|broke|broken)"
                    + "|(?:android|ios|iphone)\\s+(?:bug|issue|problem|crash)"
                    // error when toggling a control
                    + "|error\\s+when\\s+(?:enabling|disabling|toggling|tapping|opening)"
                    + "|can'?t\\s+(?:enable|disable|toggle|tap)"
                    + ")\\b",
            Pattern.CASE_INSENSITIVE);

    /**
     * UC-E generic-FAQ detector. When the description matches this
     * pattern AND does NOT match {@link #UC_K_REGRESSION_PATTERN}, the
     * routing must NOT short-circuit to UC-K — the user is asking a
     * "how does X work?" / "where do I find Y?" question, not reporting
     * an in-app failure.
     */
    private static final Pattern UC_E_GENERIC_FAQ_PATTERN = Pattern.compile(
            "\\b("
                    + "how\\s+do(?:es)?(?:\\s+(?:i|you))?\\s+(?:contact|message|reach|"
                    + "find|search|filter|browse|use|enable|disable)"
                    + "|where\\s+(?:do\\s+i|can\\s+i|is)\\s+the"
                    + "|can\\s+buyers?\\s+(?:call|message|contact)"
                    + "|how\\s+can\\s+i"
                    + "|what\\s+is\\s+the"
                    + ")\\b",
            Pattern.CASE_INSENSITIVE);

    /**
     * Topic Subjects that are eligible for the UC-K technical-regression
     * override. The handover-only topics ({@code Delivery / Pro Contract /
     * Ratings Reviews / Account Manager Support}) are not in this set —
     * they have their own override table above. {@code Account Support}
     * is included because cs_interactive_066 (the canonical regression
     * case) submits with topic="Account Support" while describing a
     * listing-flow regression.
     */
    private static final Set<String> UC_K_OVERRIDE_TOPICS = Set.of(
            "Account Support",
            "Ad Support",
            "Technical Support",
            "Replies or Messaging",
            "Replies & Messaging");

    /**
     * Maps Topic Subject → ordered list of (pattern, target UC) overrides.
     * Pro Contract and Account Manager Support intentionally have no
     * overrides — phase2 §2.11.4 keeps them terminal hard-OOS because
     * the routing decision tree has no FAQ-resolvable target for those.
     */
    private static final Map<String, List<Map.Entry<Pattern, String>>> TOPIC_OVERRIDES = Map.of(
            "Delivery", List.of(
                    Map.entry(FRAUD_SAFETY_PATTERN, "UC-J"),
                    Map.entry(PAYMENT_DISPUTE_PATTERN, "UC-I")
            ),
            "Ratings Reviews", List.of(
                    Map.entry(FRAUD_SAFETY_PATTERN, "UC-J"),
                    Map.entry(TECH_FAILURE_PATTERN, "UC-K")
            )
    );

    private final UseCaseRegistryService useCaseRegistry;
    private final LlmInvocationService llmInvocation;
    private final ObjectMapper objectMapper;

    public UseCaseRouter(UseCaseRegistryService useCaseRegistry,
                         LlmInvocationService llmInvocation,
                         ObjectMapper objectMapper) {
        this.useCaseRegistry = useCaseRegistry;
        this.llmInvocation = llmInvocation;
        this.objectMapper = objectMapper;
    }

    /**
     * Route the session to the appropriate use case.
     * Modifies session in place: activeUseCase, candidateUseCases, intentConfidence.
     *
     * @return the routing result
     */
    public RoutingResult route(BotSession session, String topicSubject, String description) {
        log.info("Session {}: routing, topicSubject='{}', description length={}",
                session.getSessionId(), topicSubject,
                description != null ? description.length() : 0);

        // Stage 1: Check topic_subject for direct routing

        // Check handover-only topics first. Per phase2 §2.11.4 + fixed_script_library_v1
        // §9.1, attempt a description-based UC override BEFORE falling through to
        // OUT_OF_SCOPE_* — Delivery + scam/fraud → UC-J, Delivery + refund/dispute
        // → UC-I, Ratings Reviews + tech failure → UC-K, etc. Pro Contract and
        // Account Manager Support keep terminal hard-OOS (no override list).
        if (useCaseRegistry.isHandoverOnlyTopic(topicSubject)) {
            Optional<String> overrideUc = matchHandoverOnlyOverride(topicSubject, description);
            if (overrideUc.isPresent()) {
                String ucId = overrideUc.get();
                log.info("Session {}: handover-only topic '{}' -> description override {}",
                        session.getSessionId(), topicSubject, ucId);
                session.setActiveUseCase(ucId);
                session.setIntentConfidence(new BigDecimal("0.75"));
                return RoutingResult.routed(ucId, new BigDecimal("0.75"));
            }
            Optional<String> oosCategory = useCaseRegistry.getOutOfScopeCategory(topicSubject);
            String reason = oosCategory.orElse("OUT_OF_SCOPE");
            log.info("Session {}: handover-only topic '{}' -> OOS ({})",
                    session.getSessionId(), topicSubject, reason);
            return RoutingResult.outOfScope(reason);
        }

        // Check strong prior topics
        Optional<String> strongPrior = useCaseRegistry.getStrongPriorTopic(topicSubject);
        if (strongPrior.isPresent()) {
            String ucId = strongPrior.get();
            log.info("Session {}: strong prior topic '{}' -> {}",
                    session.getSessionId(), topicSubject, ucId);
            session.setActiveUseCase(ucId);
            session.setIntentConfidence(new BigDecimal("0.90"));
            return RoutingResult.routed(ucId, new BigDecimal("0.90"));
        }

        // Stage 1.5 (Sprint §A2): UC-K technical-regression deterministic
        // override. Cs_interactive_066-style cases ("the phone-number
        // contact option disappeared", "the button is greyed out", "it
        // used to work on Android", "I get an error when enabling phone
        // contact") describe an in-app regression, not a feature
        // explanation. Run this BEFORE the LLM classifier so a
        // weak-prior topic (Account Support / Technical Support / etc.)
        // cannot mis-route the case to UC-B / UC-E. Generic FAQ phrasing
        // ("how do contact options work?") deliberately stays on the
        // LLM path so UC-E remains reachable.
        if (matchUcKTechnicalRegression(topicSubject, description)) {
            log.info("Session {}: UC-K technical-regression override ('{}' / '{}')",
                    session.getSessionId(), topicSubject,
                    description == null ? "" : description);
            session.setActiveUseCase("UC-K");
            session.setIntentConfidence(new BigDecimal("0.85"));
            // Preserve the LLM-style candidate list so secondary-UC
            // diagnostics (UC-E in particular) survive the override.
            List<String> candidates = useCaseRegistry.getCandidateUcsForTopic(topicSubject);
            if (candidates != null && !candidates.isEmpty()) {
                session.setCandidateUseCases(candidates.toArray(new String[0]));
            } else {
                session.setCandidateUseCases(new String[]{"UC-K"});
            }
            return RoutingResult.routed("UC-K", new BigDecimal("0.85"));
        }

        // Stage 2: LLM classification for weak priors and "Account Support" (full candidate set)
        List<String> candidates = useCaseRegistry.getCandidateUcsForTopic(topicSubject);
        if (candidates.isEmpty()) {
            // No matching UCs for this topic — check if it's an unknown topic
            log.warn("Session {}: no candidate UCs for topic '{}', escalating",
                    session.getSessionId(), topicSubject);
            return RoutingResult.outOfScope("UNKNOWN_TOPIC");
        }

        return routeViaLlm(session, candidates, topicSubject, description);
    }

    /**
     * True iff the {@code (topicSubject, description)} pair describes a
     * UC-K technical regression. Visible for unit testing.
     *
     * <p>Rules (Sprint §A2):
     * <ul>
     *   <li>Topic must be in {@link #UC_K_OVERRIDE_TOPICS}.</li>
     *   <li>Description must match {@link #UC_K_REGRESSION_PATTERN}.</li>
     *   <li>If the description ALSO matches {@link #UC_E_GENERIC_FAQ_PATTERN}
     *       and does NOT contain a strong regression keyword
     *       (disappeared / missing / greyed out / used to work / app
     *       crash / error when), the override defers to the LLM path
     *       (UC-E remains reachable for "how do contact options work?").</li>
     * </ul>
     */
    static boolean matchUcKTechnicalRegression(String topicSubject, String description) {
        if (topicSubject == null || description == null || description.isBlank()) {
            return false;
        }
        if (!UC_K_OVERRIDE_TOPICS.contains(topicSubject)) {
            return false;
        }
        String desc = description.toLowerCase(Locale.ENGLISH);
        if (!UC_K_REGRESSION_PATTERN.matcher(desc).find()) {
            return false;
        }
        // Tie-breaker: the description matches UC_K_REGRESSION_PATTERN.
        // If it ALSO matches the generic-FAQ pattern (e.g. "how do
        // contact options work" with "how do") we still need to decide.
        // The strong-regression keywords below (disappeared / used to /
        // greyed / crashes / error when) imply the user is reporting a
        // failure, not asking how something works. Without a strong
        // keyword, defer to UC-E via the LLM path so we don't
        // mis-route generic FAQ.
        if (UC_E_GENERIC_FAQ_PATTERN.matcher(desc).find()
                && !containsStrongRegressionKeyword(desc)) {
            return false;
        }
        return true;
    }

    private static boolean containsStrongRegressionKeyword(String descLower) {
        // Strong regression markers — the user is definitely reporting a
        // failure, not asking how something works. Used as the tie-breaker
        // when both UC_K_REGRESSION and UC_E_GENERIC_FAQ patterns match.
        return descLower.contains("disappear")
                || descLower.contains("vanished")
                || descLower.contains("used to ")
                || descLower.contains("greyed")
                || descLower.contains("error when")
                || descLower.contains("crash")
                || descLower.contains("stopped working")
                || descLower.contains("won't load")
                || descLower.contains("wont load");
    }

    private RoutingResult routeViaLlm(BotSession session, List<String> candidates,
                                       String topicSubject, String description) {
        // Build candidate descriptions for the LLM
        String ucCandidatesText = candidates.stream()
                .map(ucId -> {
                    UseCaseRegistryService.UseCaseDefinition def = useCaseRegistry.getUseCase(ucId);
                    return String.format("- %s: %s (path: %s)", ucId, def.name(), def.path());
                })
                .collect(Collectors.joining("\n"));

        try {
            LlmResponse response = llmInvocation.invokeRouting(ucCandidatesText, topicSubject, description,
                    session.getSessionId());

            if ("error_fallback".equals(response.getFinishReason())) {
                // LLM failed — if we have a single candidate, use it; otherwise escalate
                if (candidates.size() == 1) {
                    String ucId = candidates.get(0);
                    session.setActiveUseCase(ucId);
                    session.setIntentConfidence(new BigDecimal("0.50"));
                    return RoutingResult.routed(ucId, new BigDecimal("0.50"));
                }
                return RoutingResult.ambiguous(candidates);
            }

            // Parse the routing response
            String content = response.getContent();
            JsonNode root = objectMapper.readTree(cleanJsonResponse(content));

            String useCaseId = root.has("use_case") ? root.get("use_case").asText("") : "";
            double confidence = root.has("confidence") ? root.get("confidence").asDouble(0.0) : 0.0;

            // Validate the returned UC is in our candidate set
            if (!candidates.contains(useCaseId)) {
                log.warn("Session {}: LLM returned UC '{}' not in candidates {}, picking first candidate",
                        session.getSessionId(), useCaseId, candidates);
                useCaseId = candidates.get(0);
                confidence = 0.4;
            }

            BigDecimal conf = BigDecimal.valueOf(confidence);
            session.setActiveUseCase(useCaseId);
            session.setIntentConfidence(conf);
            session.setCandidateUseCases(candidates.toArray(new String[0]));

            log.info("Session {}: LLM routed to {} with confidence {}",
                    session.getSessionId(), useCaseId, conf);

            return RoutingResult.routed(useCaseId, conf);

        } catch (Exception e) {
            log.error("Session {}: routing LLM parse failed: {}",
                    session.getSessionId(), e.getMessage());
            // Fallback: use first candidate with low confidence
            if (!candidates.isEmpty()) {
                String ucId = candidates.get(0);
                session.setActiveUseCase(ucId);
                session.setIntentConfidence(new BigDecimal("0.30"));
                return RoutingResult.routed(ucId, new BigDecimal("0.30"));
            }
            return RoutingResult.ambiguous(candidates);
        }
    }

    /**
     * Try description-based UC override for a handover-only Topic Subject.
     * Returns empty when the topic has no override list, the description is
     * blank, or no pattern matches.
     *
     * <p>Visible for unit testing.
     */
    static Optional<String> matchHandoverOnlyOverride(String topicSubject, String description) {
        if (topicSubject == null || description == null || description.isBlank()) {
            return Optional.empty();
        }
        List<Map.Entry<Pattern, String>> overrides = TOPIC_OVERRIDES.get(topicSubject);
        if (overrides == null) {
            return Optional.empty();
        }
        String desc = description.toLowerCase(Locale.ENGLISH);
        for (Map.Entry<Pattern, String> override : overrides) {
            if (override.getKey().matcher(desc).find()) {
                return Optional.of(override.getValue());
            }
        }
        return Optional.empty();
    }

    private String cleanJsonResponse(String raw) {
        if (raw == null) return "{}";
        String trimmed = raw.trim();
        if (trimmed.startsWith("```")) {
            int nl = trimmed.indexOf('\n');
            if (nl > 0) trimmed = trimmed.substring(nl + 1);
            if (trimmed.endsWith("```")) trimmed = trimmed.substring(0, trimmed.length() - 3);
            trimmed = trimmed.trim();
        }
        return trimmed;
    }

    /**
     * Routing result.
     */
    public record RoutingResult(
            RoutingOutcome outcome,
            String activeUseCase,
            BigDecimal confidence,
            String outOfScopeReason,
            List<String> ambiguousCandidates
    ) {
        public enum RoutingOutcome { ROUTED, OUT_OF_SCOPE, AMBIGUOUS }

        static RoutingResult routed(String ucId, BigDecimal confidence) {
            return new RoutingResult(RoutingOutcome.ROUTED, ucId, confidence, null, null);
        }

        static RoutingResult outOfScope(String reason) {
            return new RoutingResult(RoutingOutcome.OUT_OF_SCOPE, null, null, reason, null);
        }

        static RoutingResult ambiguous(List<String> candidates) {
            return new RoutingResult(RoutingOutcome.AMBIGUOUS, null, null, null, candidates);
        }
    }
}
