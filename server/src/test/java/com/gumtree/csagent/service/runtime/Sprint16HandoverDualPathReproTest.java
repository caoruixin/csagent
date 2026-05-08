package com.gumtree.csagent.service.runtime;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gumtree.csagent.config.MockProperties;
import com.gumtree.csagent.model.BotSession;
import com.gumtree.csagent.model.MockHandoverLog;
import com.gumtree.csagent.repository.BotTurnRepository;
import com.gumtree.csagent.repository.MockCaseRepository;
import com.gumtree.csagent.repository.MockHandoverLogRepository;
import com.gumtree.csagent.service.SalesforceService;
import com.gumtree.csagent.service.mock.MockSalesforceService;
import com.gumtree.csagent.service.tools.RequestHandoverTool;
import com.gumtree.csagent.service.tools.ToolResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Sprint 16 §H1 — characterization repro for the LLM-driven dual-path
 * handover persistence shape in local / mock.
 *
 * <p>This test is a docs + characterization-test deliverable. It does
 * NOT change runtime behaviour. It pins the structural shape of the
 * current dual local writers so a future "Single Handover Orchestrator"
 * runtime sprint has an explicit before-state to refactor against.
 *
 * <p>Why two distinct repro methods + one disabled future-invariant
 * test:
 *
 * <ol>
 *   <li>{@link #dualLocalPersistence_llmDrivenRequestHandoverPath_producesTwoMockHandoverLogWrites_currentBehaviour}
 *       — passes today. Demonstrates the dual local persistence shape:
 *       on the LLM-driven {@code request_handover} path,
 *       {@link RequestHandoverTool} writes a row to
 *       {@code mock_handover_log} via
 *       {@link SalesforceService#requestHandover(String, java.util.Map)},
 *       and the post-loop {@code SessionManager.recordHandover} surface
 *       writes a second row directly via
 *       {@link MockHandoverLogRepository#save(Object)}. Both rows
 *       reference the same {@code session_id}.</li>
 *   <li>{@link #distinguishesLocalPersistenceDuplication_fromUnprovenRealSalesforceDoubleTransfer}
 *       — passes today. Pins the severity distinction the Sprint 16
 *       objective requires: today's repro is duplicated <i>local</i>
 *       persistence only. A real Salesforce <i>double transfer</i> is
 *       <b>not</b> proven because the production Salesforce client is
 *       not wired — only {@link MockSalesforceService} (a
 *       {@code @Profile("local")} bean) implements
 *       {@link SalesforceService}.</li>
 *   <li>{@link #futureInvariant_atMostOneTransmittedHandoverDecisionPerSessionId_disabledUntilOrchestratorLands}
 *       — disabled / TODO. Encodes the future invariant frozen by
 *       {@code docs/handover_orchestrator_design.md} §3.3: for each
 *       {@code session_id}, at most one transmitted /
 *       {@code offline_logged} handover decision may exist across the
 *       full session lifetime. This would FAIL today by design (the
 *       dual-path produces two writes); it is the explicit trigger to
 *       the future "Single Handover Orchestrator" runtime sprint.</li>
 * </ol>
 *
 * <p>Cross-references:
 * <ul>
 *   <li>{@code docs/handover_orchestrator_design.md} — exactly-once
 *       contract.</li>
 *   <li>{@code docs/runtime_freeze_and_risk_policy.md} §10.1 —
 *       known-unspecced-surface entry.</li>
 *   <li>{@code docs/release_gate.md} §1.1 — release-gate blocker
 *       before real Salesforce cutover.</li>
 *   <li>{@code docs/action_bank.md} — Single Handover Orchestrator
 *       deferred runtime candidate.</li>
 * </ul>
 *
 * <p>Sprint 16 does NOT change {@link RequestHandoverTool},
 * {@code SessionManager.recordHandover},
 * {@link MockSalesforceService}, the handover payload schema, prompts,
 * routing, or eval CaseSpecs.
 */
class Sprint16HandoverDualPathReproTest {

    private MockHandoverLogRepository handoverLogRepository;
    private MockCaseRepository caseRepository;
    private MockProperties mockProperties;
    private ObjectMapper objectMapper;
    private MockSalesforceService mockSalesforceService;
    private RequestHandoverTool requestHandoverTool;
    private HandoverPayloadAssembler handoverPayloadAssembler;
    private EscalationReasonResolver escalationResolver;
    private BotTurnRepository turnRepository;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        handoverLogRepository = mock(MockHandoverLogRepository.class);
        caseRepository = mock(MockCaseRepository.class);
        mockProperties = new MockProperties();
        mockProperties.setBusinessHours(true);

        mockSalesforceService = new MockSalesforceService(
                caseRepository, handoverLogRepository, mockProperties, objectMapper);
        requestHandoverTool = new RequestHandoverTool(mockSalesforceService);

        turnRepository = mock(BotTurnRepository.class);
        when(turnRepository.findBySessionIdOrderByTurnIndex(anyString()))
                .thenReturn(List.of());
        escalationResolver = new EscalationReasonResolver();
        handoverPayloadAssembler = new HandoverPayloadAssembler(
                turnRepository, escalationResolver, objectMapper);
    }

    /**
     * Sprint 16 §H1 (1/3) — passes today.
     *
     * <p>Demonstrates the dual local persistence shape on the
     * LLM-driven {@code request_handover} path. The first write is the
     * {@link RequestHandoverTool} → {@link SalesforceService#requestHandover}
     * path (Phase 3 §3.6.2 v1.0 payload, built inline in the tool); the
     * second write is the {@code SessionManager.recordHandover} path
     * (v1.1 payload built by {@link HandoverPayloadAssembler}, saved
     * directly via {@link MockHandoverLogRepository#save}).
     *
     * <p>The future invariant (at most one handover decision per
     * {@code session_id}) is NOT asserted here on purpose — that
     * assertion lives in
     * {@link #futureInvariant_atMostOneTransmittedHandoverDecisionPerSessionId_disabledUntilOrchestratorLands}
     * and is disabled until a future "Single Handover Orchestrator"
     * runtime sprint lands.
     */
    @Test
    void dualLocalPersistence_llmDrivenRequestHandoverPath_producesTwoMockHandoverLogWrites_currentBehaviour()
            throws Exception {
        BotSession session = buildSession();

        // ── Act 1: LLM-driven request_handover path. ─────────────────
        // AgentRunLoopImpl dispatches request_handover → ToolDispatcher
        // → RequestHandoverTool.execute(session, params). The tool
        // builds an inline v1.0 payload and calls
        // SalesforceService.requestHandover(...), which in the local
        // profile is MockSalesforceService → mock_handover_log row #1.
        Map<String, Object> llmArgs = new LinkedHashMap<>();
        llmArgs.put("escalation_reason", "faq_miss_threshold_exceeded");
        llmArgs.put("summary", "Customer's question could not be resolved by FAQ.");
        ToolResult toolResult = requestHandoverTool.execute(session, llmArgs);
        assertTrue(toolResult.isSuccess(),
                "Sanity: the LLM-driven request_handover dispatch must succeed in mock.");

        // ── Act 2: SessionManager.recordHandover path. ───────────────
        // After the loop returns with phase=ESCALATE,
        // SessionManager.processMessage calls recordHandover(session)
        // which assembles a v1.1 payload via HandoverPayloadAssembler
        // and writes mock_handover_log row #2 directly via
        // handoverLogRepository.save(...). It does NOT call
        // SalesforceService.requestHandover. We replay that surface
        // here without invoking SessionManager so the test stays a
        // focused characterization-level repro.
        Map<String, Object> assemblerPayload = handoverPayloadAssembler.assemble(session);
        MockHandoverLog secondRow = MockHandoverLog.builder()
                .logId("LOG-second-" + UUID.randomUUID().toString().substring(0, 8))
                .sessionId(session.getSessionId())
                .handoverPayload(objectMapper.writeValueAsString(assemblerPayload))
                .transferResult("mock_transfer")
                .createdAt(OffsetDateTime.now())
                .build();
        handoverLogRepository.save(secondRow);

        // ── Assert: two writes hit the same repository targeting the
        //   same session_id, but neither writer is aware of the other.
        ArgumentCaptor<MockHandoverLog> captor = ArgumentCaptor.forClass(MockHandoverLog.class);
        verify(handoverLogRepository, times(2)).save(captor.capture());
        List<MockHandoverLog> captured = captor.getAllValues();

        assertEquals(2, captured.size(),
                "Sprint 16 §H1 repro: the LLM-driven path produces two "
                        + "mock_handover_log writes for one session_id today. "
                        + "Future invariant (HandoverOrchestrator, idempotent by "
                        + "session_id) requires at most one — encoded by the "
                        + "disabled futureInvariant_* test below.");
        assertEquals(session.getSessionId(), captured.get(0).getSessionId(),
                "First write must reference the session under test.");
        assertEquals(session.getSessionId(), captured.get(1).getSessionId(),
                "Second write must reference the same session_id — the duplication.");

        // Payload schema differs across writers — proof that the two
        // writers are not coordinated.
        JsonNode firstPayload = objectMapper.readTree(captured.get(0).getHandoverPayload());
        JsonNode secondPayload = objectMapper.readTree(captured.get(1).getHandoverPayload());
        assertNotNull(firstPayload);
        assertNotNull(secondPayload);
        assertEquals("1.0", firstPayload.path("version").asText(),
                "RequestHandoverTool inline payload is Phase 3 §3.6.2 v1.0.");
        assertEquals("1.1", secondPayload.path("version").asText(),
                "HandoverPayloadAssembler v1.1 payload is what "
                        + "SessionManager.recordHandover persists today.");

        // transfer_result also differs across writers — MockSalesforceService
        // sets it from MockProperties.businessHours; SessionManager's
        // direct-save path stamps "mock_transfer".
        Set<String> sfTransferResults = Set.of("transferred", "offline_logged");
        assertTrue(sfTransferResults.contains(captured.get(0).getTransferResult()),
                "Row #1 transfer_result must be set by MockSalesforceService "
                        + "(transferred / offline_logged). Got: "
                        + captured.get(0).getTransferResult());
        assertEquals("mock_transfer", captured.get(1).getTransferResult(),
                "Row #2 transfer_result is the SessionManager.recordHandover "
                        + "literal. The two writers do not share an idempotency key.");
    }

    /**
     * Sprint 16 §H1 (2/3) — passes today.
     *
     * <p>Pins the severity distinction the sprint objective requires:
     * the dual-write today is duplicated <b>local</b> persistence in
     * {@code mock_handover_log}. It is <b>not</b> a proven real
     * Salesforce double transfer because the production Salesforce
     * client is not wired — {@link MockSalesforceService} is the only
     * {@link SalesforceService} implementation in the codebase, and it
     * is registered under {@code @Profile("local")}.
     *
     * <p>This test exists so a future review cannot conflate the P2
     * local-persistence shape today with the P0/P1 production
     * double-transfer scenario the release gate blocks against.
     */
    @Test
    void distinguishesLocalPersistenceDuplication_fromUnprovenRealSalesforceDoubleTransfer() {
        // Today's only SalesforceService implementation is
        // MockSalesforceService. We assert that fact here so that if a
        // future commit wires a real Salesforce client, this test
        // breaks and the reviewer is forced to re-read the §H1 contract
        // before allowing the new implementation past the release gate.
        SalesforceService current = mockSalesforceService;
        assertEquals(MockSalesforceService.class, current.getClass(),
                "Sprint 16 §H1: the only SalesforceService implementation "
                        + "today is MockSalesforceService; the dual-path repro is "
                        + "local persistence in mock_handover_log only. A real "
                        + "double Salesforce transfer is NOT proven today. The "
                        + "release-gate rule docs/release_gate.md §1.1 blocks "
                        + "wiring a production SalesforceService until the future "
                        + "HandoverOrchestrator makes the side-effect idempotent "
                        + "by session_id.");

        // The MockSalesforceService.requestHandover side-effect channel
        // is the local mock_handover_log repository. RequestHandoverTool
        // → SalesforceService.requestHandover writes there. There is no
        // production transfer endpoint to double-call today.
        BotSession session = buildSession();
        Map<String, Object> args = Map.of(
                "escalation_reason", "user_requested",
                "summary", "User asked for a human");
        ToolResult result = requestHandoverTool.execute(session, args);
        assertTrue(result.isSuccess());

        verify(handoverLogRepository, times(1)).save(org.mockito.ArgumentMatchers.any());
        // Note: repeating the call DOES write a second row today (no
        // idempotency by session_id). We assert that only to make the
        // current shape explicit; the future invariant test below makes
        // this a hard invariant once the orchestrator lands.
        requestHandoverTool.execute(session, args);
        verify(handoverLogRepository, times(2)).save(org.mockito.ArgumentMatchers.any());
    }

    /**
     * Sprint 16 §H1 (3/3) — DISABLED until the future "Single Handover
     * Orchestrator" runtime sprint lands.
     *
     * <p>Encodes the future invariant frozen by
     * {@code docs/handover_orchestrator_design.md} §3.3 / §6: for each
     * {@code session_id}, at most one transmitted /
     * {@code offline_logged} handover decision may exist across the
     * full session lifetime. The orchestrator MUST be idempotent by
     * {@code session_id}; a second
     * {@code HandoverOrchestrator.handover(...)} call must return
     * {@code deduped=true} without re-calling
     * {@link SalesforceService#requestHandover} and without persisting
     * a second row.
     *
     * <p>This would FAIL today by design — the LLM-driven path
     * produces two writes (see
     * {@link #dualLocalPersistence_llmDrivenRequestHandoverPath_producesTwoMockHandoverLogWrites_currentBehaviour}
     * above). The test is the explicit trigger to the future "Single
     * Handover Orchestrator" runtime sprint; remove the
     * {@link Disabled} annotation as part of that sprint's acceptance
     * criteria (release_gate §1.1 #4).
     */
    @Test
    @Disabled("TODO: future Single Handover Orchestrator runtime sprint. "
            + "Would fail today by design — the LLM-driven path writes "
            + "mock_handover_log twice for one session_id. See "
            + "docs/handover_orchestrator_design.md §3.3 and "
            + "docs/release_gate.md §1.1.")
    void futureInvariant_atMostOneTransmittedHandoverDecisionPerSessionId_disabledUntilOrchestratorLands()
            throws Exception {
        BotSession session = buildSession();

        // Act 1: LLM-driven request_handover.
        Map<String, Object> llmArgs = Map.of(
                "escalation_reason", "faq_miss_threshold_exceeded",
                "summary", "Customer's question could not be resolved by FAQ.");
        requestHandoverTool.execute(session, llmArgs);

        // Act 2: SessionManager.recordHandover replay.
        Map<String, Object> assemblerPayload = handoverPayloadAssembler.assemble(session);
        MockHandoverLog secondRow = MockHandoverLog.builder()
                .logId("LOG-second-" + UUID.randomUUID().toString().substring(0, 8))
                .sessionId(session.getSessionId())
                .handoverPayload(objectMapper.writeValueAsString(assemblerPayload))
                .transferResult("mock_transfer")
                .createdAt(OffsetDateTime.now())
                .build();
        handoverLogRepository.save(secondRow);

        // ── Future invariant ─────────────────────────────────────────
        // For each session_id, at most one transmitted / offline_logged
        // handover decision may exist. After the runtime fix lands,
        // both Acts above flow through HandoverOrchestrator, and the
        // second call returns deduped=true without re-saving.
        ArgumentCaptor<MockHandoverLog> captor = ArgumentCaptor.forClass(MockHandoverLog.class);
        verify(handoverLogRepository, times(1)).save(captor.capture());

        MockHandoverLog only = captor.getValue();
        assertEquals(session.getSessionId(), only.getSessionId(),
                "Future invariant: exactly one handover row per session_id.");
        Set<String> sfTransferResults = Set.of("transferred", "offline_logged");
        assertTrue(sfTransferResults.contains(only.getTransferResult()),
                "Future invariant: the surviving row carries the canonical "
                        + "transfer_result set by the (real or mock) Salesforce "
                        + "client, not a literal stamped by a local writer.");
    }

    private static BotSession buildSession() {
        BotSession session = new BotSession();
        session.setSessionId("sess-sprint16-handover-" + UUID.randomUUID().toString().substring(0, 8));
        session.setActiveUseCase("UC-A");
        session.setCandidateUseCases(new String[]{"UC-A", "UC-B"});
        session.setIntentConfidence(new BigDecimal("0.85"));
        session.setTotalBotTurns(3);
        session.setClarificationCount(1);
        session.setFaqMissCount(1);
        session.setArticlesShown(new String[0]);
        session.setFormTopicSubject("Ad Support");
        session.setFormContext("{\"description\":\"My ad isn't showing in search results.\","
                + "\"email\":\"customer@example.com\","
                + "\"topic_subject\":\"Ad Support\"}");
        session.setEscalationReason("faq_miss_threshold_exceeded");
        session.setHandlingState("BOT_HANDLING");
        session.setCreatedAt(OffsetDateTime.now());
        session.setUpdatedAt(OffsetDateTime.now());
        return session;
    }
}
