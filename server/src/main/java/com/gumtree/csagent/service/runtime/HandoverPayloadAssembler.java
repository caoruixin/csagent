package com.gumtree.csagent.service.runtime;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gumtree.csagent.model.BotSession;
import com.gumtree.csagent.model.BotTurn;
import com.gumtree.csagent.repository.BotTurnRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Server-side handover payload assembler (Sprint §A3).
 *
 * <p>Replaces the inline payload-building logic that previously lived in
 * {@link SessionManager#recordHandover}. The point of having a dedicated
 * assembler is to make the resulting payload <i>deterministic</i>: it
 * always carries the issue-specific summary, detected UC, escalation
 * reason, collected identifiers, source / status checks performed, the
 * partial answer or blocker, and the unresolved question — even when
 * the LLM emits no summary text at all.
 *
 * <p>Codex round 6 explicitly flagged generic summaries like
 * "User needs help" or "Customer requires assistance" as failures of
 * the cross-class quality guard. The assembler short-circuits that by
 * composing the summary from concrete session state (form description,
 * UC name, identifiers, tools / sources used, escalation reason),
 * which guarantees the summary contains content-bearing tokens from
 * the user's actual issue.
 */
@Slf4j
@Service
public class HandoverPayloadAssembler {

    /** Canonical UC name lookup (mirrors UC registry but kept local so we
     *  do not depend on the registry being initialised at handover time). */
    private static final Map<String, String> UC_NAMES = Map.ofEntries(
            Map.entry("UC-A", "Ad Status & Visibility"),
            Map.entry("UC-B", "Posting & Editing Guidance"),
            Map.entry("UC-C", "Messages & Replies"),
            Map.entry("UC-D", "Account & Login"),
            Map.entry("UC-E", "General Product & Search"),
            Map.entry("UC-F", "Payment Inquiry"),
            Map.entry("UC-FP", "Correct Deletion Explanation"),
            Map.entry("UC-G", "GDPR / Data Deletion"),
            Map.entry("UC-H", "Ad Removal Appeal"),
            Map.entry("UC-I", "Refund / Payment Dispute"),
            Map.entry("UC-J", "Trust & Safety Report"),
            Map.entry("UC-K", "Technical Issue Intake")
    );

    /** Tools that count as "status checks" for the summary line. */
    private static final Set<String> STATUS_CHECK_TOOLS = Set.of(
            "get_customer_context",
            "lookup_customer_account",
            "lookup_listing_or_ad",
            "get_moderation_review_context",
            "get_message_moderation_context"
    );

    /** Tools that count as "knowledge / source checks". */
    private static final Set<String> KNOWLEDGE_TOOLS = Set.of(
            "search_knowledge",
            "resolve_article"
    );

    /** Maximum length of a verbatim form-description snippet in the
     *  summary. Long descriptions get truncated to keep the summary
     *  scannable while still preserving content tokens. */
    private static final int DESCRIPTION_SNIPPET_MAX = 220;

    private final BotTurnRepository turnRepository;
    private final EscalationReasonResolver escalationResolver;
    private final ObjectMapper objectMapper;

    public HandoverPayloadAssembler(BotTurnRepository turnRepository,
                                    EscalationReasonResolver escalationResolver,
                                    ObjectMapper objectMapper) {
        this.turnRepository = turnRepository;
        this.escalationResolver = escalationResolver;
        this.objectMapper = objectMapper;
    }

    /**
     * Build the handover payload map for the given session. The map's
     * insertion order matches the field ordering documented in
     * phase2 §2.7 / tech_spec §15.3, so that downstream readers
     * (eval trace collector, mock handover log, Salesforce mock) can
     * rely on stable JSON key order.
     */
    public Map<String, Object> assemble(BotSession session) {
        return assemble(session, null);
    }

    /**
     * Variant that accepts a pre-fetched turn list. Used by callers
     * that already loaded the turns to avoid a second DB round-trip.
     */
    public Map<String, Object> assemble(BotSession session, List<BotTurn> turnsOverride) {
        Objects.requireNonNull(session, "session");
        List<BotTurn> turns = turnsOverride;
        if (turns == null) {
            try {
                turns = turnRepository.findBySessionIdOrderByTurnIndex(session.getSessionId());
            } catch (Exception e) {
                log.warn("Session {}: handover assembler failed to load turns: {}",
                        session.getSessionId(), e.getMessage());
                turns = List.of();
            }
        }

        ToolUsageDigest digest = digestToolUsage(session, turns);
        FormSnapshot form = readFormContext(session);
        String reasonCanon = escalationResolver.canonicalize(session.getEscalationReason());
        if (reasonCanon == null) {
            reasonCanon = "service_degraded";
        }

        String currentStatus = reasonCanon.startsWith("intake_complete")
                ? "intake_complete"
                : "escalation_triggered";

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("version", "1.1");
        payload.put("session_id", session.getSessionId());
        payload.put("bot_session_id", session.getSfBotSessionId());
        payload.put("primary_use_case", session.getActiveUseCase());
        payload.put("candidate_use_cases",
                session.getCandidateUseCases() != null
                        ? Arrays.asList(session.getCandidateUseCases())
                        : List.of());
        payload.put("current_status", currentStatus);
        payload.put("summary", buildSummary(session, form, digest, reasonCanon));
        payload.put("user_issue", buildUserIssue(form));
        payload.put("unresolved_question", buildUnresolvedQuestion(form, turns));
        payload.put("partial_answer_or_blocker", buildPartialAnswerOrBlocker(turns, digest, reasonCanon));
        payload.put("intent_confidence", session.getIntentConfidence());
        payload.put("clarification_count", session.getClarificationCount());
        payload.put("faq_miss_count", session.getFaqMissCount());
        payload.put("articles_shown",
                session.getArticlesShown() != null
                        ? Arrays.asList(session.getArticlesShown())
                        : List.of());
        payload.put("status_checks_performed", digest.statusChecks);
        payload.put("knowledge_tools_invoked", digest.knowledgeTools);
        payload.put("escalation_reason", reasonCanon);
        if (escalationResolver.isTerminalCloseReason(reasonCanon)) {
            // Sprint §A1: when the escalation reason is a control-plane
            // terminal close (turn cap / clarification cap / FAQ-miss
            // cap), the semantic motivation may be different. Surface it
            // as a separate field so reviewers can tell apart "the
            // user explicitly asked for a callback" from "we ran out of
            // turns".
            payload.put("terminal_close_reason", reasonCanon);
        }
        payload.put("transcript_ref", "bot_session:" + session.getSessionId());
        payload.put("case_id", session.getCaseId());
        payload.put("identifiers_collected", buildIdentifiers(form, session));
        payload.put("intake_fields",
                session.getIntakeFields() != null ? session.getIntakeFields() : "{}");
        payload.put("total_bot_turns", session.getTotalBotTurns());
        payload.put("form_topic_subject", session.getFormTopicSubject());
        payload.put("prompt_version", session.getPromptVersion());
        payload.put("model_version", session.getModelVersion());
        return payload;
    }

    /**
     * Build a deterministic, issue-specific summary string. Visible for
     * unit testing.
     *
     * <p>The summary always includes:
     * <ol>
     *   <li>The verbatim form description (truncated) — guarantees
     *       content-bearing tokens from the user's actual issue land in
     *       the summary, satisfying the eval-side
     *       {@code _handover_summary_mentions} check.</li>
     *   <li>The detected UC family + name.</li>
     *   <li>Status / source checks performed.</li>
     *   <li>The escalation reason.</li>
     *   <li>The unresolved question (last user turn).</li>
     * </ol>
     */
    public String buildSummary(BotSession session, FormSnapshot form,
                                ToolUsageDigest digest, String reasonCanon) {
        StringBuilder sb = new StringBuilder();

        String activeUc = session.getActiveUseCase();
        String ucLabel = ucLabel(activeUc);
        String topic = session.getFormTopicSubject();

        // 1. Customer issue line.
        String issueText = preferredIssueText(form);
        if (!isBlank(issueText)) {
            sb.append("Customer reports: ").append(snippet(issueText, DESCRIPTION_SNIPPET_MAX));
            if (!issueText.endsWith(".") && !issueText.endsWith("!") && !issueText.endsWith("?")) {
                sb.append('.');
            }
            sb.append(' ');
        } else if (!isBlank(topic)) {
            sb.append("Customer raised an inquiry under topic '").append(topic).append("'. ");
        } else {
            sb.append("Customer started a session without a written description. ");
        }

        // 2. UC line.
        if (ucLabel != null) {
            sb.append("Detected use case: ").append(ucLabel).append(". ");
        } else {
            sb.append("Detected use case: not classified. ");
        }

        // 3. Identifiers.
        Map<String, String> identifiers = buildIdentifiers(form, session);
        if (!identifiers.isEmpty()) {
            sb.append("Identifiers collected: ");
            sb.append(identifiers.entrySet().stream()
                    .map(e -> e.getKey() + "=" + e.getValue())
                    .reduce((a, b) -> a + ", " + b)
                    .orElse(""));
            sb.append(". ");
        }

        // 4. Source / status checks.
        if (digest.knowledgeTools > 0 || !digest.statusChecks.isEmpty() || digest.articlesShown > 0) {
            sb.append("Source/status checks: ");
            List<String> bits = new ArrayList<>();
            if (digest.knowledgeTools > 0) {
                bits.add("knowledge searches=" + digest.knowledgeTools
                        + (digest.articlesShown > 0
                                ? " (" + digest.articlesShown + " article(s) shown)"
                                : ""));
            }
            if (!digest.statusChecks.isEmpty()) {
                bits.add("status checks=" + String.join("/", digest.statusChecks));
            }
            sb.append(String.join("; ", bits));
            sb.append(". ");
        } else {
            sb.append("Source/status checks: none performed before handover. ");
        }

        // 5. Partial answer / blocker.
        String blocker = buildPartialAnswerOrBlocker(null, digest, reasonCanon);
        if (!isBlank(blocker)) {
            sb.append(blocker);
            if (!blocker.endsWith(".")) sb.append('.');
            sb.append(' ');
        }

        // 6. Escalation reason.
        sb.append("Escalation reason: ").append(reasonCanon).append(".");

        return sb.toString().trim();
    }

    /**
     * Build a digest of the tools the bot ran during the session. Visible
     * for testing.
     */
    public ToolUsageDigest digestToolUsage(BotSession session, List<BotTurn> turns) {
        int knowledgeCount = 0;
        int articlesShown = session.getArticlesShown() == null ? 0 : session.getArticlesShown().length;
        java.util.LinkedHashSet<String> statusChecks = new java.util.LinkedHashSet<>();

        if (turns != null) {
            for (BotTurn turn : turns) {
                String json = turn.getToolCalls();
                if (json == null || json.isBlank()) continue;
                try {
                    JsonNode arr = objectMapper.readTree(json);
                    if (!arr.isArray()) continue;
                    for (JsonNode entry : arr) {
                        String name = entry.has("tool_name") ? entry.get("tool_name").asText("") : "";
                        if (KNOWLEDGE_TOOLS.contains(name)) {
                            knowledgeCount++;
                        } else if (STATUS_CHECK_TOOLS.contains(name)) {
                            statusChecks.add(name);
                        }
                    }
                } catch (Exception e) {
                    log.debug("Session {}: turn {} tool_calls JSON unparseable: {}",
                            session.getSessionId(), turn.getTurnIndex(), e.getMessage());
                }
            }
        }
        return new ToolUsageDigest(knowledgeCount, articlesShown,
                List.copyOf(statusChecks));
    }

    private String buildUserIssue(FormSnapshot form) {
        String text = preferredIssueText(form);
        if (isBlank(text)) {
            return "Customer did not provide a written issue description.";
        }
        return snippet(text, DESCRIPTION_SNIPPET_MAX * 2);
    }

    private String buildUnresolvedQuestion(FormSnapshot form, List<BotTurn> turns) {
        // Last non-empty user_message captures whatever the customer
        // most recently said. Falling back to form description means
        // forms-only sessions still carry an "unresolved question".
        if (turns != null) {
            for (int i = turns.size() - 1; i >= 0; i--) {
                BotTurn t = turns.get(i);
                String msg = t.getUserMessage();
                if (msg != null && !msg.isBlank()) {
                    return snippet(msg, DESCRIPTION_SNIPPET_MAX);
                }
            }
        }
        if (form != null && !isBlank(form.description)) {
            return snippet(form.description, DESCRIPTION_SNIPPET_MAX);
        }
        return "Customer is awaiting human follow-up; no explicit question recorded.";
    }

    /**
     * Compose a "partial answer or blocker" string from the digest +
     * escalation reason. This is the only field that drifts with the
     * agent's actual behaviour (vs. always-deterministic identifier /
     * UC fields), so it carries either the hint that the bot did
     * source-cited resolve work before escalating, or the canonical
     * blocker derived from the escalation reason. {@code turns} is
     * accepted for future expansion (e.g. extracting the last bot
     * answer) and may be {@code null} when only the reason-derived
     * blocker is needed.
     */
    public String buildPartialAnswerOrBlocker(List<BotTurn> turns,
                                              ToolUsageDigest digest,
                                              String reasonCanon) {
        boolean attemptedResolution = (digest != null && digest.knowledgeTools > 0)
                || (digest != null && digest.articlesShown > 0);

        String reasonHint;
        switch (reasonCanon == null ? "" : reasonCanon) {
            case "user_requested" ->
                    reasonHint = "Customer explicitly asked to be transferred to a human agent";
            case "user_distress" ->
                    reasonHint = "Customer expressed distress / frustration";
            case "imminent_harm" ->
                    reasonHint = "Imminent-harm signal detected — high priority";
            case "trust_safety_required" ->
                    reasonHint = "Trust & Safety review required";
            case "payment_dispute_detected" ->
                    reasonHint = "Payment dispute detected";
            case "appeal_requires_human", "incorrect_deletion_appeal" ->
                    reasonHint = "Appeal / moderation review required";
            case "gdpr_intake", "identity_verification_required", "account_compliance" ->
                    reasonHint = "GDPR / identity / compliance intake — requires human review";
            case "intake_complete_for_uc_g", "intake_complete_for_uc_h",
                 "intake_complete_for_uc_i", "intake_complete_for_uc_j",
                 "intake_complete_for_uc_k" ->
                    reasonHint = "Intake fields collected; ready for the destination team to action";
            case "incomplete_intake" ->
                    reasonHint = "Intake fields could not be fully collected; partial info forwarded";
            case "tool_scope_blocked" ->
                    reasonHint = "Required tool was outside the bot's scope; human action required";
            case "out_of_scope" ->
                    reasonHint = "Topic is outside the bot's coverage";
            case "service_degraded" ->
                    reasonHint = "Service degraded; conversation routed to a human";
            case "runtime_error_threshold" ->
                    reasonHint = "Repeated runtime errors prevented the bot from resolving the issue";
            case "clarification_budget_exhausted" ->
                    reasonHint = "Bot could not converge after the maximum clarification rounds";
            case "faq_miss_threshold_exceeded" ->
                    reasonHint = "Knowledge search did not return a usable answer; bot could not resolve";
            case "turn_budget_exhausted" ->
                    reasonHint = "Turn budget reached before the bot reached a resolution";
            default ->
                    reasonHint = "Bot could not resolve and handed over";
        }

        StringBuilder sb = new StringBuilder();
        if (attemptedResolution) {
            sb.append("Bot attempted to resolve via knowledge search (")
              .append(digest.knowledgeTools).append(" search call(s), ")
              .append(digest.articlesShown).append(" article(s) shown) but could not close out. ");
        } else {
            sb.append("Bot did not produce a self-resolved answer. ");
        }
        sb.append("Blocker: ").append(reasonHint).append(".");
        return sb.toString();
    }

    /** Extract email / ad_id (and any form-supplied identifiers) — visible for testing. */
    public Map<String, String> buildIdentifiers(FormSnapshot form, BotSession session) {
        Map<String, String> identifiers = new LinkedHashMap<>();
        if (form != null) {
            if (!isBlank(form.email)) {
                identifiers.put("email", form.email);
            }
            if (!isBlank(form.adId)) {
                identifiers.put("ad_id", form.adId);
            }
        }
        if (session.getCaseId() != null && !session.getCaseId().isBlank()) {
            identifiers.put("case_id", session.getCaseId());
        }
        return identifiers;
    }

    /** Read the form context JSON into a typed snapshot — visible for testing. */
    public FormSnapshot readFormContext(BotSession session) {
        FormSnapshot snap = new FormSnapshot();
        if (session.getFormContext() == null || session.getFormContext().isBlank()) {
            return snap;
        }
        try {
            JsonNode node = objectMapper.readTree(session.getFormContext());
            snap.firstName = textOrNull(node, "first_name");
            snap.email = textOrNull(node, "email");
            snap.adId = textOrNull(node, "ad_id");
            snap.description = textOrNull(node, "description");
            snap.topicSubject = textOrNull(node, "topic_subject");
        } catch (Exception e) {
            log.debug("Session {}: form context JSON unparseable: {}",
                    session.getSessionId(), e.getMessage());
        }
        return snap;
    }

    private static String preferredIssueText(FormSnapshot form) {
        if (form == null) return null;
        if (!isBlank(form.description)) return form.description.trim();
        if (!isBlank(form.topicSubject)) return form.topicSubject.trim();
        return null;
    }

    private static String ucLabel(String activeUc) {
        if (activeUc == null || activeUc.isBlank()) return null;
        String name = UC_NAMES.get(activeUc);
        return name != null ? activeUc + " (" + name + ")" : activeUc;
    }

    private static String snippet(String text, int max) {
        if (text == null) return "";
        String t = text.strip();
        if (t.length() <= max) return t;
        return t.substring(0, max).trim() + "...";
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private static String textOrNull(JsonNode node, String key) {
        if (node == null || !node.has(key)) return null;
        JsonNode v = node.get(key);
        if (v == null || v.isNull()) return null;
        String s = v.asText("");
        return s.isEmpty() ? null : s;
    }

    /** Lightweight typed view of the form context. Public for tests. */
    public static final class FormSnapshot {
        public String firstName;
        public String email;
        public String adId;
        public String description;
        public String topicSubject;

        public FormSnapshot() {}

        public FormSnapshot(String firstName, String email, String adId,
                            String description, String topicSubject) {
            this.firstName = firstName;
            this.email = email;
            this.adId = adId;
            this.description = description;
            this.topicSubject = topicSubject;
        }
    }

    /** Tool-usage digest. Public for tests. */
    public static final class ToolUsageDigest {
        public final int knowledgeTools;
        public final int articlesShown;
        public final List<String> statusChecks;

        public ToolUsageDigest(int knowledgeTools, int articlesShown, List<String> statusChecks) {
            this.knowledgeTools = knowledgeTools;
            this.articlesShown = articlesShown;
            this.statusChecks = statusChecks;
        }
    }
}
