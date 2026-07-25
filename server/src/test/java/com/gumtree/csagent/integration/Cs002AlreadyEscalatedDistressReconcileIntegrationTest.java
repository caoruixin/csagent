package com.gumtree.csagent.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gumtree.csagent.model.BotSession;
import com.gumtree.csagent.model.BotTurn;
import com.gumtree.csagent.model.ChatSessionResponse;
import com.gumtree.csagent.model.MockHandoverLog;
import com.gumtree.csagent.repository.BotEventRepository;
import com.gumtree.csagent.repository.BotSessionRepository;
import com.gumtree.csagent.repository.BotTurnRepository;
import com.gumtree.csagent.repository.MockHandoverLogRepository;
import com.gumtree.csagent.repository.SessionOutcomeRepository;
import com.gumtree.csagent.service.runtime.AgentRunLoop;
import com.gumtree.csagent.service.runtime.BudgetChecker;
import com.gumtree.csagent.service.runtime.ContextProjectionBuilder;
import com.gumtree.csagent.service.runtime.ControlKernel;
import com.gumtree.csagent.service.runtime.ControlPolicyService;
import com.gumtree.csagent.service.runtime.DriftDetector;
import com.gumtree.csagent.service.runtime.EscalationReasonResolver;
import com.gumtree.csagent.service.runtime.FormContextIngestionService;
import com.gumtree.csagent.service.runtime.HandoverPayloadAssembler;
import com.gumtree.csagent.service.runtime.PhaseEvaluator;
import com.gumtree.csagent.service.runtime.SessionManager;
import com.gumtree.csagent.service.runtime.UseCaseRegistryService;
import com.gumtree.csagent.service.runtime.UseCaseRouter;
import com.gumtree.csagent.service.observability.EventEmitter;
import com.gumtree.csagent.service.tools.CreateCaseControlledTool;
import com.gumtree.csagent.config.AgentRunLoopProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.invocation.InvocationOnMock;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Sprint 3.1 P1 — case-level integration regression for cs_interactive_002.
 *
 * <p>Reproduces the cs_002 shape exposed by the Sprint 3 second smoke
 * run ({@code eval_interactive/results/20260504-191541/results.json}):
 *
 * <ol>
 *   <li>Session-create's auto-search escalates the session with
 *       {@code escalation_reason=faq_miss_threshold_exceeded} and moves
 *       it to {@code ESCALATE} / {@code QUEUE_TO_HUMAN}.</li>
 *   <li>The user then sends the canonical distress seed
 *       {@code "How long do I have to wait to sort this out? Its been
 *       this way since day 1."}</li>
 *   <li>{@link SessionManager#processMessage} is called.</li>
 * </ol>
 *
 * <p>Before this fix, the early-return for already-escalated sessions
 * skipped {@link ControlKernel#processMessage}, so the §B1 distress
 * stamp never fired and {@code session.escalationReason} stayed at
 * {@code faq_miss_threshold_exceeded} despite the distress phrase.
 * This violated the Sprint 3 cs_002 contract and re-opened the L1
 * escalation_reason_consistency failure mode in the persisted surfaces.
 *
 * <p>The fix runs the resolver's high-priority semantic detectors
 * (callback / human request → {@code user_requested}; distress →
 * {@code user_distress}) against the incoming message BEFORE the
 * early return. When they fire, the session reason is upgraded via
 * the resolver, every persisted {@code request_handover.arguments.escalation_reason}
 * in {@code bot_turns} is normalised through
 * {@link ControlKernel#normalizeHandoverArgsToSessionReason}, and the
 * existing {@code mock_handover_log} payload is rewritten in place
 * (no second handover row).
 */
@ExtendWith(MockitoExtension.class)
class Cs002AlreadyEscalatedDistressReconcileIntegrationTest {

    private static final String CS002_DISTRESS_SEED =
            "How long do I have to wait to sort this out? Its been this way since day 1.";

    private static final String CS002_FORM_CONTEXT_JSON =
            "{\"first_name\":\"Richard\","
                    + "\"email\":\"customer@example.com\","
                    + "\"topic_subject\":\"Replies or Messaging\","
                    + "\"ad_id\":\"\","
                    + "\"description\":\""
                    + "I am not receiving notifications from you when messages "
                    + "are arriving in my inbox.\"}";

    @Mock private BotSessionRepository sessionRepository;
    @Mock private BotEventRepository eventRepository;
    @Mock private SessionOutcomeRepository outcomeRepository;
    @Mock private MockHandoverLogRepository handoverLogRepository;
    @Mock private BotTurnRepository botTurnRepository;
    @Mock private FormContextIngestionService formIngestion;
    @Mock private UseCaseRouter useCaseRouter;
    @Mock private ControlPolicyService controlPolicy;
    @Mock private UseCaseRegistryService useCaseRegistry;
    @Mock private com.gumtree.csagent.repository.KbArticleRepository kbArticleRepository;

    // ControlKernel collaborators (mocked individually so the SUT can
    // exercise the real ControlKernel.normalizeHandoverArgsToSessionReason
    // helper without dragging in an LLM stack).
    @Mock private BudgetChecker budgetChecker;
    @Mock private DriftDetector driftDetector;
    @Mock private PhaseEvaluator phaseEvaluator;
    @Mock private CreateCaseControlledTool createCaseTool;
    @Mock private EventEmitter eventEmitter;
    @Mock private ContextProjectionBuilder contextProjectionBuilder;
    @Mock private AgentRunLoop agentRunLoop;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final EscalationReasonResolver escalationResolver =
            new EscalationReasonResolver();

    private SessionManager sessionManager;
    private ControlKernel controlKernel;
    private HandoverPayloadAssembler handoverPayloadAssembler;

    @BeforeEach
    void setUp() {
        AgentRunLoopProperties props = new AgentRunLoopProperties();
        props.setEnabledPhases(List.of());
        controlKernel = new ControlKernel(
                botTurnRepository, eventRepository, budgetChecker, driftDetector,
                phaseEvaluator, controlPolicy, objectMapper, createCaseTool,
                eventEmitter, contextProjectionBuilder, props, agentRunLoop,
                escalationResolver);
        handoverPayloadAssembler = new HandoverPayloadAssembler(
                botTurnRepository, escalationResolver, objectMapper);
        sessionManager = new SessionManager(
                sessionRepository, eventRepository, outcomeRepository,
                handoverLogRepository, botTurnRepository, formIngestion,
                useCaseRouter, controlKernel, controlPolicy,
                useCaseRegistry, objectMapper, handoverPayloadAssembler,
                escalationResolver,
                new com.gumtree.csagent.service.knowledge.ArticleCardAssembler(kbArticleRepository));
    }

    /**
     * Build a session in the cs_002 already-escalated state — auto-search
     * exhausted FAQ search and stamped {@code faq_miss_threshold_exceeded}
     * before the user could send the distress message.
     */
    private BotSession buildAlreadyEscalatedSession(String reason) {
        BotSession session = new BotSession();
        session.setSessionId("sess-cs002-distress-1");
        session.setCurrentPhase("ESCALATE");
        session.setHandlingState("QUEUE_TO_HUMAN");
        session.setContainmentOutcome("escalated");
        session.setActiveUseCase("UC-C");
        session.setCandidateUseCases(new String[]{"UC-C", "UC-D", "UC-K"});
        session.setEscalationReason(reason);
        session.setTotalBotTurns(1);
        session.setFormTopicSubject("Replies or Messaging");
        session.setFormContext(CS002_FORM_CONTEXT_JSON);
        session.setCreatedAt(OffsetDateTime.now());
        session.setUpdatedAt(OffsetDateTime.now());
        return session;
    }

    /**
     * Build the persisted bot_turn that the auto-search path would have
     * saved, with a {@code request_handover} tool call carrying the
     * lower-priority reason.
     */
    private BotTurn buildExistingHandoverTurn(String reason) throws Exception {
        List<Map<String, Object>> toolCallsList = new ArrayList<>();
        Map<String, Object> handoverEntry = new LinkedHashMap<>();
        handoverEntry.put("tool_name", "request_handover");
        Map<String, Object> args = new LinkedHashMap<>();
        args.put("escalation_reason", reason);
        handoverEntry.put("arguments", args);
        toolCallsList.add(handoverEntry);
        return BotTurn.builder()
                .turnId(UUID.randomUUID().toString())
                .sessionId("sess-cs002-distress-1")
                .turnIndex(0)
                .userMessage("I am not receiving notifications from you when messages are arriving in my inbox.")
                .botResponse("Hi Richard! I'm having difficulty resolving this. Let me connect you with a specialist.")
                .phaseBefore("INIT")
                .phaseAfter("ESCALATE")
                .activeUseCase("UC-C")
                .latencyMs(0)
                .toolCalls(objectMapper.writeValueAsString(toolCallsList))
                .createdAt(OffsetDateTime.now())
                .build();
    }

    /**
     * Build the persisted handover log that the auto-search recordHandover
     * call would have produced.
     */
    private MockHandoverLog buildExistingHandoverLog(BotSession session,
                                                      String reason) throws Exception {
        Map<String, Object> payload = handoverPayloadAssembler.assemble(session, List.of());
        payload.put("escalation_reason", reason);
        return MockHandoverLog.builder()
                .logId(UUID.randomUUID().toString())
                .sessionId(session.getSessionId())
                .handoverPayload(objectMapper.writeValueAsString(payload))
                .customerMessage("This conversation has been transferred to a human agent who can better assist you.")
                .transcript("[]")
                .transferResult("mock_transfer")
                .createdAt(OffsetDateTime.now())
                .build();
    }

    @Test
    void cs002_distressUtterance_onAlreadyEscalatedSession_promotesReason() throws Exception {
        // ── Arrange: cs_002 auto-search escalation with faq_miss reason. ──
        BotSession session = buildAlreadyEscalatedSession("faq_miss_threshold_exceeded");
        BotTurn existingTurn = buildExistingHandoverTurn("faq_miss_threshold_exceeded");
        MockHandoverLog existingLog =
                buildExistingHandoverLog(session, "faq_miss_threshold_exceeded");

        when(sessionRepository.findById(session.getSessionId()))
                .thenReturn(Optional.of(session));
        // The reconcile path reads bot_turns first to normalise tool_calls.
        when(botTurnRepository.findBySessionIdOrderByTurnIndex(session.getSessionId()))
                .thenReturn(List.of(existingTurn));
        when(handoverLogRepository.findBySessionId(session.getSessionId()))
                .thenReturn(List.of(existingLog));

        List<BotTurn> savedTurns = new ArrayList<>();
        when(botTurnRepository.save(any(BotTurn.class))).thenAnswer((InvocationOnMock inv) -> {
            BotTurn t = inv.getArgument(0);
            savedTurns.add(t);
            return t;
        });
        List<MockHandoverLog> savedLogs = new ArrayList<>();
        when(handoverLogRepository.save(any(MockHandoverLog.class))).thenAnswer((InvocationOnMock inv) -> {
            MockHandoverLog hl = inv.getArgument(0);
            savedLogs.add(hl);
            return hl;
        });

        // ── Act: drive the cs_002 distress turn through SessionManager. ──
        ChatSessionResponse response = sessionManager.processMessage(
                session.getSessionId(), CS002_DISTRESS_SEED);

        // ── Assertion 8: early-return transfer-message behaviour preserved.
        // The user-facing reply must remain the canonical "transferred to a
        // human agent" message; only the persisted semantic surfaces are
        // corrected. shouldEndChat must remain true so the eval driver
        // does not loop on this session.
        assertNotNull(response);
        assertEquals(
                "This conversation has been transferred to a human agent. Please wait for them to respond.",
                response.getReplyText(),
                "Sprint 3.1 P1 contract: the early-return transfer reply text "
                        + "must be preserved when the session is already escalated.");
        assertTrue(response.isShouldEndChat(),
                "Sprint 3.1 P1 contract: shouldEndChat must remain true on the early-return path.");

        // ── Assertion 4: final session.escalationReason = user_distress. ──
        // The resolver must promote the session reason from
        // faq_miss_threshold_exceeded (priority 41) to user_distress
        // (priority 2) on the basis of the distress signal in the
        // incoming message.
        assertEquals("user_distress", session.getEscalationReason(),
                "Sprint 3 cs_002 contract: a distress utterance after auto-search "
                        + "escalation must promote the session reason to user_distress.");

        // ── Assertion 5: persisted request_handover.arguments.escalation_reason
        // is normalised to user_distress. ──
        assertTrue(savedTurns.size() >= 1,
                "Bot turn should be re-saved after tool_calls normalisation.");
        BotTurn updatedTurn = savedTurns.get(savedTurns.size() - 1);
        JsonNode toolCalls = objectMapper.readTree(updatedTurn.getToolCalls());
        assertTrue(toolCalls.isArray() && toolCalls.size() >= 1,
                "Persisted tool_calls must remain a non-empty JSON array.");
        JsonNode handoverEntry = null;
        for (JsonNode entry : toolCalls) {
            if ("request_handover".equals(entry.path("tool_name").asText())) {
                handoverEntry = entry;
                break;
            }
        }
        assertNotNull(handoverEntry,
                "request_handover entry must remain in the persisted trace.");
        String persistedReason = handoverEntry.path("arguments").path("escalation_reason").asText();
        assertEquals("user_distress", persistedReason,
                "§B0 normaliser contract: the persisted "
                        + "request_handover.arguments.escalation_reason must be "
                        + "rewritten to user_distress so the persisted trace agrees "
                        + "with the upgraded session reason.");

        // ── Assertion 6: handover payload escalation_reason matches. ──
        // The MockHandoverLog row is updated in place (same log_id) — no
        // second handover row is created.
        assertEquals(1, savedLogs.size(),
                "Sprint 3.1 P1 contract: exactly one handover log save (the "
                        + "existing row, updated in place). Do not create a second handover.");
        MockHandoverLog updatedLog = savedLogs.get(0);
        assertEquals(existingLog.getLogId(), updatedLog.getLogId(),
                "Handover log update must preserve the original log_id "
                        + "(no second handover row).");
        JsonNode payload = objectMapper.readTree(updatedLog.getHandoverPayload());
        assertEquals("user_distress", payload.path("escalation_reason").asText(),
                "Handover payload escalation_reason must equal session.escalationReason "
                        + "after the cs_002 reconcile.");

        // ── Assertion 7: pairwise consistency across all three surfaces. ──
        // L1:escalation_reason_consistency cannot fail when each pair agrees.
        String sessionReason = session.getEscalationReason();
        assertEquals(sessionReason, persistedReason,
                "L1:escalation_reason_consistency: session vs persisted tool call.");
        assertEquals(sessionReason, payload.path("escalation_reason").asText(),
                "L1:escalation_reason_consistency: session vs handover payload.");
        assertEquals(persistedReason, payload.path("escalation_reason").asText(),
                "L1:escalation_reason_consistency: persisted tool call vs handover payload.");

        // ── Assertion: ControlKernel.processMessage was NOT invoked. ──
        // The early return for already-escalated sessions is preserved —
        // the budget check, drift, phase evaluation, and second LLM turn
        // must NOT run again. (This is the contract that prevents the
        // bot from "re-engaging" a queued-to-human session.)
        verify(budgetChecker, never()).checkBudgets(any());
        verify(driftDetector, never()).detect(any(), anyString());
        verify(phaseEvaluator, never()).evaluate(any(), anyString(), any());

        // Session was persisted with the upgraded reason (saving on the
        // reconcile path is required so the L1 contract holds even when
        // the eval collector reads from the database between turns).
        verify(sessionRepository).save(session);
    }

    @Test
    void cs002_userRequestedUtterance_onAlreadyEscalatedSession_beatsFaqMiss() throws Exception {
        // user_requested (priority 1) beats faq_miss_threshold_exceeded (41).
        BotSession session = buildAlreadyEscalatedSession("faq_miss_threshold_exceeded");
        BotTurn existingTurn = buildExistingHandoverTurn("faq_miss_threshold_exceeded");
        MockHandoverLog existingLog =
                buildExistingHandoverLog(session, "faq_miss_threshold_exceeded");

        when(sessionRepository.findById(session.getSessionId()))
                .thenReturn(Optional.of(session));
        when(botTurnRepository.findBySessionIdOrderByTurnIndex(session.getSessionId()))
                .thenReturn(List.of(existingTurn));
        when(handoverLogRepository.findBySessionId(session.getSessionId()))
                .thenReturn(List.of(existingLog));
        List<BotTurn> savedTurns = new ArrayList<>();
        when(botTurnRepository.save(any(BotTurn.class))).thenAnswer((InvocationOnMock inv) -> {
            BotTurn t = inv.getArgument(0);
            savedTurns.add(t);
            return t;
        });
        List<MockHandoverLog> savedLogs = new ArrayList<>();
        when(handoverLogRepository.save(any(MockHandoverLog.class))).thenAnswer((InvocationOnMock inv) -> {
            MockHandoverLog hl = inv.getArgument(0);
            savedLogs.add(hl);
            return hl;
        });

        sessionManager.processMessage(session.getSessionId(),
                "Please call me back, I want to speak to a human agent now.");

        assertEquals("user_requested", session.getEscalationReason(),
                "user_requested (priority 1) must win over faq_miss_threshold_exceeded.");
        BotTurn updatedTurn = savedTurns.get(savedTurns.size() - 1);
        JsonNode toolCalls = objectMapper.readTree(updatedTurn.getToolCalls());
        JsonNode handoverEntry = toolCalls.get(0);
        assertEquals("user_requested",
                handoverEntry.path("arguments").path("escalation_reason").asText(),
                "Persisted tool call must carry user_requested.");
        JsonNode payload = objectMapper.readTree(savedLogs.get(0).getHandoverPayload());
        assertEquals("user_requested", payload.path("escalation_reason").asText(),
                "Handover payload must carry user_requested.");
    }

    @Test
    void cs002_userRequestedUtterance_onAlreadyEscalatedSession_beatsUserDistress() throws Exception {
        // cs_029-style precedence: user_requested still beats user_distress
        // even when both signals are theoretically present.
        BotSession session = buildAlreadyEscalatedSession("user_distress");
        BotTurn existingTurn = buildExistingHandoverTurn("user_distress");
        MockHandoverLog existingLog = buildExistingHandoverLog(session, "user_distress");

        when(sessionRepository.findById(session.getSessionId()))
                .thenReturn(Optional.of(session));
        when(botTurnRepository.findBySessionIdOrderByTurnIndex(session.getSessionId()))
                .thenReturn(List.of(existingTurn));
        when(handoverLogRepository.findBySessionId(session.getSessionId()))
                .thenReturn(List.of(existingLog));
        List<BotTurn> savedTurns = new ArrayList<>();
        when(botTurnRepository.save(any(BotTurn.class))).thenAnswer((InvocationOnMock inv) -> {
            BotTurn t = inv.getArgument(0);
            savedTurns.add(t);
            return t;
        });
        List<MockHandoverLog> savedLogs = new ArrayList<>();
        when(handoverLogRepository.save(any(MockHandoverLog.class))).thenAnswer((InvocationOnMock inv) -> {
            MockHandoverLog hl = inv.getArgument(0);
            savedLogs.add(hl);
            return hl;
        });

        sessionManager.processMessage(session.getSessionId(),
                "this is ridiculous, please call me back NOW");

        assertEquals("user_requested", session.getEscalationReason(),
                "user_requested (priority 1) must win over user_distress (priority 2).");
    }

    @Test
    void cs002_calmFollowUp_onAlreadyEscalatedSession_doesNotChangeReason() throws Exception {
        // Negative regression: a calm follow-up must NOT promote the reason,
        // and must NOT touch persisted bot_turns or handover log.
        BotSession session = buildAlreadyEscalatedSession("faq_miss_threshold_exceeded");

        when(sessionRepository.findById(session.getSessionId()))
                .thenReturn(Optional.of(session));

        sessionManager.processMessage(session.getSessionId(),
                "thanks, I will wait for the human agent.");

        assertEquals("faq_miss_threshold_exceeded", session.getEscalationReason(),
                "Calm follow-up must not promote the session reason.");
        // Reconcile path must short-circuit before fetching turns / logs.
        verify(botTurnRepository, never()).findBySessionIdOrderByTurnIndex(anyString());
        verify(handoverLogRepository, never()).findBySessionId(anyString());
        verify(handoverLogRepository, never()).save(any(MockHandoverLog.class));
    }
}
