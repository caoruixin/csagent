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
 * 23-value enum defined in
 * {@code customer_service_tool_spec_v0_2.yaml} and mirrored by
 * {@code eval_interactive.case_spec.schema.ESCALATION_TRIGGER_VALUES}.
 */
@Slf4j
@Service
public class EscalationReasonResolver {

    /** Canonical 23-value enum — kept in lockstep with PhaseEvaluator. */
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
            "turn_budget_exhausted"
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
            Map.entry("turn_budget_exhausted", 42)
    );

    /** Reasons that classify as a <i>terminal close</i> rather than a
     *  semantic escalation. They never overwrite a semantic reason. */
    static final Set<String> TERMINAL_CLOSE_REASONS = Set.of(
            "clarification_budget_exhausted",
            "faq_miss_threshold_exceeded",
            "turn_budget_exhausted"
    );

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
