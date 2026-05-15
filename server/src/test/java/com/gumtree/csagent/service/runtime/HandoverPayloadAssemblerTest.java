package com.gumtree.csagent.service.runtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gumtree.csagent.model.BotSession;
import com.gumtree.csagent.model.BotTurn;
import com.gumtree.csagent.repository.BotTurnRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Sprint 2026-05-04 §A3: deterministic handover payload assembly.
 *
 * <p>The assembler must guarantee the payload includes: an issue-specific
 * summary (NOT a generic "User needs help" sentence), the detected UC,
 * the canonical escalation reason, collected identifiers, source / status
 * checks, a partial-answer-or-blocker line, and the unresolved question.
 */
class HandoverPayloadAssemblerTest {

    private ObjectMapper objectMapper;
    private EscalationReasonResolver resolver;
    private BotTurnRepository turnRepository;
    private HandoverPayloadAssembler assembler;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        resolver = new EscalationReasonResolver();
        turnRepository = mock(BotTurnRepository.class);
        when(turnRepository.findBySessionIdOrderByTurnIndex(anyString()))
                .thenReturn(List.of());
        assembler = new HandoverPayloadAssembler(turnRepository, resolver, objectMapper);
    }

    @Test
    void cs066_payload_summaryReferencesIssueAndUcAndReason() {
        BotSession session = sessionFor(
                "UC-K",
                "Account Support",
                "{"
                        + "\"first_name\":\"Stephen\","
                        + "\"email\":\"customer@example.com\","
                        + "\"ad_id\":\"\","
                        + "\"description\":\"Why am I not getting the option to add my phone number as a "
                        + "point of contact when listening an item any more\","
                        + "\"topic_subject\":\"Account Support\""
                        + "}",
                "intake_complete_for_uc_k");
        session.setCaseId("CS-066-CASE");

        Map<String, Object> payload = assembler.assemble(session, List.of());
        String summary = (String) payload.get("summary");

        assertNotNull(summary);
        // Must NOT be a generic non-answer.
        assertFalse(summary.toLowerCase().equals("user needs help."));
        assertFalse(summary.toLowerCase().equals("customer requires assistance."));
        // Must reference the UC and the canonical reason.
        assertTrue(summary.contains("UC-K"));
        assertTrue(summary.contains("intake_complete_for_uc_k"));
        // Must reference content-bearing tokens from the form description so
        // the eval-side _handover_summary_mentions check passes (4-char
        // minimum tokens like "phone", "contact", "option", "listening").
        String summaryLower = summary.toLowerCase();
        assertTrue(summaryLower.contains("phone"));
        assertTrue(summaryLower.contains("contact"));
        // Must include collected identifiers.
        assertEquals("intake_complete", payload.get("current_status"));
        assertEquals("UC-K", payload.get("primary_use_case"));
        assertEquals("intake_complete_for_uc_k", payload.get("escalation_reason"));
        assertEquals("CS-066-CASE", payload.get("case_id"));
        @SuppressWarnings("unchecked")
        Map<String, String> identifiers = (Map<String, String>) payload.get("identifiers_collected");
        assertEquals("customer@example.com", identifiers.get("email"));
        assertEquals("CS-066-CASE", identifiers.get("case_id"));
    }

    @Test
    void cs029_userRequestedReason_setsTerminalCloseFieldWhenBudgetReason() {
        BotSession session = sessionFor("UC-D", "Account Support",
                "{\"description\":\"My account is locked and I need to use it for advertising\","
                        + "\"email\":\"baz@example.com\"}",
                "user_requested");
        Map<String, Object> payload = assembler.assemble(session, List.of());

        // Semantic reason is user_requested → no terminal_close_reason field.
        assertEquals("user_requested", payload.get("escalation_reason"));
        assertNull(payload.get("terminal_close_reason"));

        // Same shape, but escalation reason is a budget close-out → field
        // appears so reviewers can see the control-plane termination signal.
        session.setEscalationReason("turn_budget_exhausted");
        Map<String, Object> budgetPayload = assembler.assemble(session, List.of());
        assertEquals("turn_budget_exhausted", budgetPayload.get("escalation_reason"));
        assertEquals("turn_budget_exhausted", budgetPayload.get("terminal_close_reason"));
    }

    @Test
    void payload_includesAllCanonicalFields() {
        BotSession session = sessionFor("UC-A", "Ad Support",
                "{\"description\":\"My ad got removed and I want to know why\","
                        + "\"email\":\"alice@example.com\"}",
                "faq_miss_threshold_exceeded");
        Map<String, Object> payload = assembler.assemble(session, List.of());

        // Required fields per phase2 §2.7 + Sprint §A3 spec.
        assertNotNull(payload.get("session_id"));
        assertNotNull(payload.get("primary_use_case"));
        assertNotNull(payload.get("summary"));
        assertNotNull(payload.get("escalation_reason"));
        assertNotNull(payload.get("user_issue"));
        assertNotNull(payload.get("unresolved_question"));
        assertNotNull(payload.get("partial_answer_or_blocker"));
        assertNotNull(payload.get("identifiers_collected"));
        assertNotNull(payload.get("status_checks_performed"));
        assertNotNull(payload.get("knowledge_tools_invoked"));
        assertNotNull(payload.get("total_bot_turns"));
    }

    @Test
    void summary_isIssueSpecific_notGeneric() {
        BotSession session = sessionFor("UC-A", "Ad Support",
                "{\"description\":\"Ad got removed unfairly please review\"}",
                "appeal_requires_human");
        Map<String, Object> payload = assembler.assemble(session, List.of());
        String summary = ((String) payload.get("summary")).toLowerCase();

        // Anti-patterns we explicitly rule out.
        assertFalse(summary.equals("user needs help."));
        assertFalse(summary.equals("customer requires assistance."));
        // The form-description content tokens must land in the summary.
        assertTrue(summary.contains("removed"));
        assertTrue(summary.contains("review"));
    }

    @Test
    void digestToolUsage_countsKnowledgeAndStatusTools() {
        BotSession session = sessionFor("UC-A", "Ad Support",
                "{\"description\":\"any issue\"}",
                "faq_miss_threshold_exceeded");
        session.setArticlesShown(new String[]{"KB-1", "KB-2"});

        BotTurn t1 = BotTurn.builder()
                .turnId("t1")
                .sessionId("s")
                .turnIndex(1)
                .toolCalls("[{\"tool_name\":\"search_knowledge\",\"arguments\":{}},"
                        + "{\"tool_name\":\"get_customer_context\",\"arguments\":{}}]")
                .build();
        BotTurn t2 = BotTurn.builder()
                .turnId("t2")
                .sessionId("s")
                .turnIndex(2)
                .toolCalls("[{\"tool_name\":\"resolve_article\",\"arguments\":{}}]")
                .build();

        HandoverPayloadAssembler.ToolUsageDigest digest =
                assembler.digestToolUsage(session, List.of(t1, t2));

        assertEquals(2, digest.knowledgeTools);
        assertEquals(2, digest.articlesShown);
        assertTrue(digest.statusChecks.contains("get_customer_context"));
    }

    @Test
    void buildPartialAnswerOrBlocker_carriesReasonHint() {
        HandoverPayloadAssembler.ToolUsageDigest empty =
                new HandoverPayloadAssembler.ToolUsageDigest(0, 0, List.of());

        String userRequested = assembler.buildPartialAnswerOrBlocker(null, empty, "user_requested");
        assertTrue(userRequested.toLowerCase().contains("explicitly asked"));

        String tsHint = assembler.buildPartialAnswerOrBlocker(null, empty, "trust_safety_required");
        assertTrue(tsHint.toLowerCase().contains("trust"));

        String budgetHint = assembler.buildPartialAnswerOrBlocker(null, empty, "turn_budget_exhausted");
        assertTrue(budgetHint.toLowerCase().contains("turn budget"));
    }

    @Test
    void unresolvedQuestion_prefersLastUserMessage_thenFormDescription() {
        BotSession session = sessionFor("UC-A", "Ad Support",
                "{\"description\":\"original form description text\"}",
                "user_requested");

        // No turns: falls back to form description.
        Map<String, Object> payload1 = assembler.assemble(session, List.of());
        assertTrue(((String) payload1.get("unresolved_question"))
                .contains("original form description text"));

        // Turns present: last non-empty user_message wins.
        BotTurn t1 = BotTurn.builder()
                .turnId("t1").sessionId("s").turnIndex(1)
                .userMessage("the latest thing the customer asked").build();
        BotTurn t2 = BotTurn.builder()
                .turnId("t2").sessionId("s").turnIndex(2)
                .userMessage("").build();
        Map<String, Object> payload2 = assembler.assemble(session, List.of(t1, t2));
        assertTrue(((String) payload2.get("unresolved_question"))
                .contains("the latest thing the customer asked"));
    }

    @Test
    void canonicalizesNonCanonicalSessionReason() {
        BotSession session = sessionFor("UC-A", "Ad Support",
                "{\"description\":\"any\"}",
                "user_requested_escalation"); // legacy literal
        Map<String, Object> payload = assembler.assemble(session, List.of());
        // Must collapse onto canonical user_requested.
        assertEquals("user_requested", payload.get("escalation_reason"));
    }

    @Test
    void blankSession_handover_stillProducesNonEmptySummary() {
        BotSession session = new BotSession();
        session.setSessionId("sess-empty");
        session.setActiveUseCase(null);
        session.setEscalationReason("service_degraded");

        Map<String, Object> payload = assembler.assemble(session, List.of());
        String summary = (String) payload.get("summary");
        assertNotNull(summary);
        assertFalse(summary.isBlank());
        assertTrue(summary.contains("service_degraded"));
    }

    // ---------------- helpers ----------------

    private static BotSession sessionFor(String uc, String topic, String formContextJson,
                                          String reason) {
        BotSession session = new BotSession();
        session.setSessionId("sess-" + System.nanoTime());
        session.setActiveUseCase(uc);
        session.setFormTopicSubject(topic);
        session.setFormContext(formContextJson);
        session.setEscalationReason(reason);
        session.setCandidateUseCases(new String[]{uc});
        session.setArticlesShown(new String[0]);
        session.setTotalBotTurns(3);
        session.setClarificationCount(1);
        session.setFaqMissCount(0);
        session.setIntentConfidence(new BigDecimal("0.8"));
        session.setCreatedAt(OffsetDateTime.now());
        session.setUpdatedAt(OffsetDateTime.now());
        return session;
    }

    @SuppressWarnings("unused")
    private static List<BotTurn> mutableTurnList(BotTurn... turns) {
        // Convenience helper kept for clarity if tests want to build mutable lists.
        List<BotTurn> list = new ArrayList<>();
        for (BotTurn t : turns) list.add(t);
        return list;
    }
}
