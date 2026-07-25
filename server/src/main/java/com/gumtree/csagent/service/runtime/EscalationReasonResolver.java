package com.gumtree.csagent.service.runtime;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Deterministic resolver for the {@code escalation_reason} field.
 *
 * <p>Three problems this resolver fixes (Sprint 2026-05-04 §A1):
 * <ol>
 *   <li><b>Precedence drift.</b> Multiple write sites (budget check,
 *       drift detector, agent loop, force escalate) used to call
 *       {@code session.setEscalationReason(...)} with the most recent
 *       observation, so a late {@code turn_budget_exhausted} could
 *       overwrite an earlier {@code user_requested}. The resolver
 *       merges the existing session reason with a new candidate using
 *       a fixed priority table — once an explicit user-request /
 *       distress / safety reason is set, lower-priority budget
 *       fallbacks cannot overwrite it.</li>
 *   <li><b>Semantic vs. terminal-close conflation.</b> A
 *       {@code turn_budget_exhausted} stop is a control-plane
 *       <i>terminal close reason</i>; a {@code user_requested}
 *       handover is the <i>semantic escalation reason</i>. The
 *       resolver classifies each reason and applies precedence so the
 *       semantic reason always wins when present.</li>
 *   <li><b>Missed callback / "call me now" patterns.</b> The
 *       per-turn user-message check used to live inside
 *       {@link DriftDetector} and ran <i>after</i> the budget check,
 *       so a user message asking for a callback on the same turn the
 *       budget exhausted produced {@code turn_budget_exhausted}. The
 *       resolver exposes {@link #detectExplicitUserEscalation} so the
 *       control kernel can detect callback / human-request patterns
 *       up-front.</li>
 * </ol>
 *
 * <p>All values returned by the resolver are members of the canonical
 * 24-value enum defined in
 * {@code docs/current/customer_service_tool_spec_v0_3.md} and mirrored by
 * {@code eval_interactive.case_spec.schema.ESCALATION_TRIGGER_VALUES}.
 */
@Slf4j
@Service
public class EscalationReasonResolver {

    /** Canonical 24-value enum — kept in lockstep with PhaseEvaluator. */
    static final Set<String> CANONICAL_REASONS = Set.of(
            "user_requested",
            "user_distress",
            "imminent_harm",
            "trust_safety_required",
            "payment_dispute_detected",
            "appeal_requires_human",
            "incorrect_deletion_appeal",
            "gdpr_intake",
            "identity_verification_required",
            "account_compliance",
            "intake_complete_for_uc_g",
            "intake_complete_for_uc_h",
            "intake_complete_for_uc_i",
            "intake_complete_for_uc_j",
            "intake_complete_for_uc_k",
            "incomplete_intake",
            "out_of_scope",
            "tool_scope_blocked",
            "service_degraded",
            "runtime_error_threshold",
            "clarification_budget_exhausted",
            "faq_miss_threshold_exceeded",
            "turn_budget_exhausted",
            "agent_unable_to_resolve"
    );

    /**
     * Priority table — <i>lower number wins</i>. Ranking encodes the
     * Phase 2 §2.4 precedence: explicit user signal &gt; safety /
     * dispute / compliance &gt; intake-complete / scope failures &gt;
     * budget close-out. {@link #resolve(String, String)} prefers the
     * value with the lowest priority number.
     */
    private static final Map<String, Integer> PRIORITY = Map.ofEntries(
            // Tier 0 — explicit user signal beats everything else.
            Map.entry("imminent_harm", 0),
            Map.entry("user_requested", 1),
            Map.entry("user_distress", 2),
            // Tier 1 — high-risk semantic categories.
            Map.entry("trust_safety_required", 10),
            Map.entry("payment_dispute_detected", 11),
            Map.entry("appeal_requires_human", 12),
            Map.entry("incorrect_deletion_appeal", 13),
            Map.entry("gdpr_intake", 14),
            Map.entry("identity_verification_required", 15),
            Map.entry("account_compliance", 16),
            // Tier 2 — intake-complete and scope failures.
            Map.entry("intake_complete_for_uc_g", 20),
            Map.entry("intake_complete_for_uc_h", 21),
            Map.entry("intake_complete_for_uc_i", 22),
            Map.entry("intake_complete_for_uc_j", 23),
            Map.entry("intake_complete_for_uc_k", 24),
            Map.entry("incomplete_intake", 25),
            Map.entry("out_of_scope", 26),
            Map.entry("tool_scope_blocked", 27),
            // Tier 3 — infrastructure / runtime fallbacks.
            Map.entry("service_degraded", 30),
            Map.entry("runtime_error_threshold", 31),
            // Tier 4 — budget / capacity (terminal-close family).
            Map.entry("clarification_budget_exhausted", 40),
            Map.entry("faq_miss_threshold_exceeded", 41),
            Map.entry("turn_budget_exhausted", 42),
            // Tier 5 — semantic last-resort (Sprint 096 / S-Auto-44,
            // M-Auto-9 WP1). A bot-initiated, in-scope, exhausted-resolution,
            // unresolved handover. Deliberately the LOWEST priority of any
            // reason so it never displaces a genuinely-present reason —
            // including a real budget close-out (40-42). It surfaces only
            // when nothing genuinely-higher applies. It is a SEMANTIC reason
            // (LLM-owned), NOT a terminal-close, so it is intentionally absent
            // from TERMINAL_CLOSE_REASONS below.
            Map.entry("agent_unable_to_resolve", 50)
    );

    /** Reasons that classify as a <i>terminal close</i> rather than a
     *  semantic escalation. They never overwrite a semantic reason. */
    static final Set<String> TERMINAL_CLOSE_REASONS = Set.of(
            "clarification_budget_exhausted",
            "faq_miss_threshold_exceeded",
            "turn_budget_exhausted"
    );

    /**
     * Sprint §B1 patterns that signal user <i>distress</i> / sustained
     * frustration. Each match stamps {@code user_distress} on the session
     * before the budget check fires, so the resolver's precedence table
     * keeps the higher-priority semantic reason instead of letting
     * {@code faq_miss_threshold_exceeded} or {@code turn_budget_exhausted}
     * win on the same turn.
     *
     * <p>Pattern intent (cs_002 / cs_014 / cs_029-shape complaints):
     * <ul>
     *   <li>"you are not helping" / "your no helping" — direct frustration
     *       at the bot's failure to resolve.</li>
     *   <li>"no one is helping" / "nobody helps" — repeated unsuccessful
     *       channels.</li>
     *   <li>"i (have) (already) followed (your) process" / "i did
     *       everything" — exhaustion after compliant troubleshooting.</li>
     *   <li>"this is ridiculous" / "absurd" / "joke" / "useless" —
     *       overt frustration markers.</li>
     *   <li>"so called process / so-called <X>" — sarcastic complaint.</li>
     *   <li>"How long do I have to wait" / "still not fixed" — chronic
     *       pain after multiple turns.</li>
     * </ul>
     * The {@link #ALL_CAPS_FRUSTRATION_PATTERN} below catches
     * shouting independently of the literal phrasing above.
     */
    private static final Pattern[] DISTRESS_PATTERNS = {
            Pattern.compile("\\b(?:you|your|youre|you're|ur)\\s+(?:are|r)?\\s*not\\s+help(?:ing|ful)\\b",
                    Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(?:you|your|youre|you're|ur)\\s+no\\s+help(?:ing|ful)?\\b",
                    Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(?:no\\s+(?:one|body)|nobody)\\s+(?:is\\s+)?help(?:ing|ed|s)?\\b",
                    Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bi(?:'ve|\\s+have)?\\s+(?:already\\s+)?followed\\s+(?:your\\s+|the\\s+|this\\s+)?(?:so[-\\s]called\\s+)?process\\b",
                    Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bso[-\\s]called\\s+(?:process|support|help)\\b",
                    Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bthis\\s+is\\s+(?:ridiculous|absurd|a\\s+joke|useless|unacceptable|insane)\\b",
                    Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(?:complete\\s+)?waste\\s+of\\s+(?:my\\s+)?time\\b",
                    Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(?:fed\\s+up|sick\\s+of\\s+this|frustrating)\\b",
                    Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bhow\\s+long\\s+do\\s+i\\s+have\\s+to\\s+wait\\b",
                    Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bsince\\s+day\\s+(?:1|one)\\b",
                    Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(?:still\\s+not\\s+(?:fixed|working|resolved)|been\\s+this\\s+way)\\b",
                    Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(?:prior|previous|earlier)\\s+request(?:s)?\\s+(?:were|was|are)\\s+ignored\\b",
                    Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bignored\\s+(?:my\\s+)?(?:prior|previous|earlier)?\\s*request",
                    Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bdo\\s+i\\s+have\\s+to\\b.*\\?",
                    Pattern.CASE_INSENSITIVE),
            // Strong intensifier — "really frustrated", "extremely angry", etc.
            Pattern.compile("\\b(?:really|so|extremely|incredibly|deeply)\\s+(?:frustrated|angry|upset|annoyed)\\b",
                    Pattern.CASE_INSENSITIVE)
    };

    /**
     * Sprint §B1: ALL-CAPS frustration shouting. Treated as distress when
     * the message has at least 8 alpha characters and ≥ 70% of those are
     * uppercase. The lower threshold (8 chars) catches short shouts like
     * "HELP ME" while still ignoring product names ("UK", "FAQ") and
     * single-word affirmations.
     */
    private static final int ALL_CAPS_MIN_LETTERS = 8;
    private static final double ALL_CAPS_RATIO = 0.7;

    /**
     * Patterns that signal an <i>explicit user-driven escalation</i>:
     * the user asked for a human or a callback. Mirrors the eval-side
     * {@code ESCALATION_REQUEST_PATTERNS} so the runtime cannot
     * silently disagree with the L1 gate.
     */
    private static final Pattern[] EXPLICIT_USER_ESCALATION_PATTERNS = {
            // "speak to a human / agent / person / representative"
            Pattern.compile("\\btalk\\s+to\\s+(an?\\s+)?(human|agent|person|representative|someone)\\b",
                    Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bspeak\\s+to\\s+(an?\\s+)?(human|agent|person|representative|someone)\\b",
                    Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bconnect\\s+me\\s+to\\s+(an?\\s+)?(human|agent|person|representative|someone)\\b",
                    Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\btransfer\\s+(me\\s+)?to\\s+(an?\\s+)?(human|agent|person|representative|someone)\\b",
                    Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bi\\s+want\\s+(an?\\s+)?(human|real\\s+person|agent)\\b",
                    Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bget\\s+me\\s+(an?\\s+)?(human|agent|person|representative)\\b",
                    Pattern.CASE_INSENSITIVE),
            // Callback / phone / ring patterns — round 6 §P1.
            Pattern.compile("\\b(please\\s+)?call\\s+me\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bgive\\s+me\\s+a\\s+call\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(ring|phone)\\s+me\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bcall(\\s+me)?\\s+(back|now)\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bcallback\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bcan\\s+(someone|anyone)\\s+(call|phone|ring)\\s+me\\b",
                    Pattern.CASE_INSENSITIVE)
    };

    /**
     * Return the canonical winner between an existing session reason
     * and a candidate. The lower-priority value loses; ties prefer the
     * existing reason so repeated identical writes are no-ops.
     *
     * <p>Non-canonical values are mapped to {@code service_degraded}
     * before comparison so the L1 trace contract enum check stays
     * green. {@code null} / blank inputs are treated as "no signal" —
     * the other side wins automatically.
     */
    public String resolve(String existing, String candidate) {
        String existingCanon = canonicalize(existing);
        String candidateCanon = canonicalize(candidate);
        if (existingCanon == null) {
            return candidateCanon;
        }
        if (candidateCanon == null) {
            return existingCanon;
        }
        int existingPriority = PRIORITY.getOrDefault(existingCanon, Integer.MAX_VALUE);
        int candidatePriority = PRIORITY.getOrDefault(candidateCanon, Integer.MAX_VALUE);
        if (candidatePriority < existingPriority) {
            return candidateCanon;
        }
        return existingCanon;
    }

    /**
     * Map a possibly-non-canonical reason to a canonical enum value.
     * {@code null} / blank stays {@code null}. Anything outside the
     * canonical set is mapped to {@code service_degraded} (Phase 2
     * §2.4 catch-all) so persisted reasons always satisfy the trace
     * contract enum.
     */
    public String canonicalize(String reason) {
        if (reason == null) {
            return null;
        }
        String trimmed = reason.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        if (CANONICAL_REASONS.contains(trimmed)) {
            return trimmed;
        }
        String lower = trimmed.toLowerCase(Locale.ENGLISH);
        // Common runtime literals that pre-date the canonical enum.
        if ("user_requested_escalation".equals(lower)
                || "user_request".equals(lower)
                || "human_requested".equals(lower)
                || "callback_requested".equals(lower)) {
            return "user_requested";
        }
        if ("drift_hard_shift".equals(lower)
                || "unexpected_phase".equals(lower)
                || "unknown_use_case".equals(lower)
                || "agent_error".equals(lower)
                || "system_failure".equals(lower)
                || "llm_determined_escalation".equals(lower)) {
            return "service_degraded";
        }
        log.debug("EscalationReasonResolver: non-canonical reason '{}' -> service_degraded", reason);
        return "service_degraded";
    }

    /**
     * Detect whether the given user message contains an explicit
     * request for human contact (callback / "speak to an agent" /
     * "phone me" / etc.). Used by {@link ControlKernel} to set
     * {@code user_requested} <i>before</i> the budget check so
     * cs_interactive_029-style cases (user asks for a callback while
     * the clarification budget is also exhausted) do not get
     * serialised as a budget reason.
     */
    public boolean detectExplicitUserEscalation(String userMessage) {
        if (userMessage == null || userMessage.isBlank()) {
            return false;
        }
        for (Pattern p : EXPLICIT_USER_ESCALATION_PATTERNS) {
            if (p.matcher(userMessage).find()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Sprint §B1: detect distress / sustained frustration in a user
     * message. Returns true on any of:
     * <ul>
     *   <li>An ALL-CAPS shout (≥ {@value #ALL_CAPS_MIN_LETTERS} letters
     *       and ≥ {@value #ALL_CAPS_RATIO} uppercase ratio).</li>
     *   <li>A literal frustration phrase from
     *       {@link #DISTRESS_PATTERNS} (e.g. "you are not helping",
     *       "no one is helping", "this is ridiculous", "followed your
     *       so called process").</li>
     * </ul>
     *
     * <p>Used by {@link ControlKernel} to stamp {@code user_distress}
     * via the resolver's precedence table BEFORE the budget close-out
     * fires. With {@code user_distress} on the session, the resolver
     * keeps it ahead of {@code faq_miss_threshold_exceeded} and
     * {@code turn_budget_exhausted} (priorities 41 / 42), even if the
     * budget bucket later trips on the same turn.
     */
    public boolean detectDistressSignal(String userMessage) {
        return hasDistressSignal(userMessage);
    }

    /**
     * WS-3 / D2 (2026-07-25) — static twin of
     * {@link #detectDistressSignal(String)}.
     *
     * <p>Exists so {@code ContextProjectionBuilder} can surface the signal to
     * the LLM as an advisory {@code user_sentiment_signal} slot without taking
     * a constructor dependency on this bean. The detection logic is unchanged
     * and lives in exactly one place.
     *
     * <p><b>What this signal is and is not.</b> It is an <i>observation</i>
     * that the customer sounds frustrated. It is NOT an escalation trigger.
     * The runtime never calls {@code forceEscalate} on it (see
     * {@code ControlKernel.processMessage} step 2.4) and the projection marks
     * it advisory. Per D2 the decision — de-escalate and keep solving, or hand
     * over — belongs to the LLM, which is the only party that can tell whether
     * the underlying ask is still explanation- or query-class.
     */
    public static boolean hasDistressSignal(String userMessage) {
        if (userMessage == null || userMessage.isBlank()) {
            return false;
        }
        if (isAllCapsShout(userMessage)) {
            return true;
        }
        for (Pattern p : DISTRESS_PATTERNS) {
            if (p.matcher(userMessage).find()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Visible for testing. {@code true} when the message is dominated
     * by uppercase letters — a deterministic shouting signal that
     * couples cleanly with {@link #detectDistressSignal}.
     */
    static boolean isAllCapsShout(String message) {
        if (message == null) return false;
        int letters = 0;
        int uppers = 0;
        for (int i = 0; i < message.length(); i++) {
            char c = message.charAt(i);
            if (Character.isLetter(c)) {
                letters++;
                if (Character.isUpperCase(c)) {
                    uppers++;
                }
            }
        }
        if (letters < ALL_CAPS_MIN_LETTERS) {
            return false;
        }
        return ((double) uppers / (double) letters) >= ALL_CAPS_RATIO;
    }

    /**
     * Whether {@code reason} represents a control-plane terminal
     * close (turn cap / clarification cap / FAQ-miss cap) rather
     * than a semantic escalation. The handover payload assembler
     * uses this to keep the semantic reason and the terminal-close
     * reason on separate fields when needed.
     */
    public boolean isTerminalCloseReason(String reason) {
        String canon = canonicalize(reason);
        return canon != null && TERMINAL_CLOSE_REASONS.contains(canon);
    }

    /**
     * Test-only accessor for the priority table.
     */
    static int priorityOf(String reason) {
        return PRIORITY.getOrDefault(reason, Integer.MAX_VALUE);
    }
}
