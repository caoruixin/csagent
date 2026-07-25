package com.gumtree.csagent.service.runtime;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gumtree.csagent.model.BotSession;
import com.gumtree.csagent.model.ChatSessionResponse;
import com.gumtree.csagent.repository.BotEventRepository;
import com.gumtree.csagent.repository.BotSessionRepository;
import com.gumtree.csagent.repository.BotTurnRepository;
import com.gumtree.csagent.repository.MockHandoverLogRepository;
import com.gumtree.csagent.repository.SessionOutcomeRepository;
import com.gumtree.csagent.service.runtime.UseCaseRouter.RoutingResult;
import com.gumtree.csagent.service.tools.ToolPolicyEnforcer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Sprint 31 — regression coverage for the
 * {@code alternate_candidate_use_cases} projection slot
 * ({@code R-alternate-uc-signal-data-source}, Option β).
 *
 * <p>Three behaviour bars from {@code docs/sprint_objective.md} §2 / §6
 * are pinned at the {@link SessionManager} + {@link ContextProjectionBuilder}
 * unit layers:
 *
 * <ul>
 *   <li><b>AMBIGUOUS intake captures the candidates.</b>
 *       {@code SessionManager.createSession}'s AMBIGUOUS branch assigns
 *       {@code routingResult.ambiguousCandidates()} onto the new
 *       {@code BotSession.intakeAmbiguousCandidates} field. Prior to
 *       Sprint 31 this list was discarded.</li>
 *   <li><b>ROUTED intake leaves the field null.</b> Per OQ1 pre-pick
 *       (Sprint 31 objective §5), the ROUTED branch does not populate
 *       the snapshot; Lombok's null default is preserved.</li>
 *   <li><b>The projection emits the slot minus the active UC.</b>
 *       {@link ContextProjectionBuilder} surfaces
 *       {@code alternate_candidate_use_cases} on every turn for
 *       projection-shape stability; the active UC is filtered out.</li>
 * </ul>
 *
 * <p>The non-enforcement bar (the runtime does NOT branch on the slot's
 * value) is covered by the companion integration test
 * {@code AgentRunLoopAlternateCandidateUseCasesNonEnforcementIntegrationTest}
 * — mirroring the Sprint 20 split between
 * {@link AlreadyCalledProjectionTest} and the
 * {@code AgentRunLoopAlreadyCalledNonEnforcementIntegrationTest}.
 */
@ExtendWith(MockitoExtension.class)
class IntakeAmbiguousCandidatesProjectionTest {

    // -- SessionManager fixtures --

    @Mock private BotSessionRepository sessionRepository;
    @Mock private BotEventRepository eventRepository;
    @Mock private SessionOutcomeRepository outcomeRepository;
    @Mock private MockHandoverLogRepository handoverLogRepository;
    @Mock private BotTurnRepository botTurnRepository;
    @Mock private FormContextIngestionService formIngestion;
    @Mock private UseCaseRouter useCaseRouter;
    @Mock private ControlKernel controlKernel;
    @Mock private ControlPolicyService controlPolicy;
    @Mock private UseCaseRegistryService useCaseRegistry;
    @Mock private com.gumtree.csagent.repository.KbArticleRepository kbArticleRepository;

    // -- ContextProjectionBuilder fixtures --

    @Mock private ToolPolicyEnforcer toolPolicyEnforcer;

    private SessionManager sessionManager;
    private ContextProjectionBuilder projectionBuilder;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        EscalationReasonResolver resolver = new EscalationReasonResolver();
        HandoverPayloadAssembler handoverAssembler = new HandoverPayloadAssembler(
                botTurnRepository, resolver, objectMapper);
        sessionManager = new SessionManager(
                sessionRepository, eventRepository, outcomeRepository,
                handoverLogRepository, botTurnRepository, formIngestion,
                useCaseRouter, controlKernel, controlPolicy,
                useCaseRegistry, objectMapper, handoverAssembler, resolver,
                new com.gumtree.csagent.service.knowledge.ArticleCardAssembler(kbArticleRepository));

        projectionBuilder = new ContextProjectionBuilder(
                objectMapper, useCaseRegistry, controlPolicy, toolPolicyEnforcer, null);
        projectionBuilder.initToolSchemas();

        lenient().when(controlPolicy.getMaxBotTurnsFaq()).thenReturn(6);
        lenient().when(controlPolicy.getMaxClarificationRounds()).thenReturn(3);
        lenient().when(controlPolicy.getMaxFaqMiss()).thenReturn(2);
    }

    // ---------------------------------------------------------------
    // Bar 1 — AMBIGUOUS intake captures the candidates onto BotSession
    // ---------------------------------------------------------------

    @Test
    void ambiguousIntake_capturesCandidatesOntoSession() {
        when(useCaseRouter.routeNonBlocking(any(BotSession.class), anyString(), anyString()))
                .thenReturn(RoutingResult.ambiguous(List.of("UC-A", "UC-C")));

        sessionManager.createSession(
                "Mia", "m@test.com", "Replies & Messaging",
                null, "I have a complex issue with my messages and listings");

        ArgumentCaptor<BotSession> sessionCaptor = ArgumentCaptor.forClass(BotSession.class);
        verify(sessionRepository).save(sessionCaptor.capture());

        BotSession persisted = sessionCaptor.getValue();
        assertNotNull(persisted.getIntakeAmbiguousCandidates(),
                "AMBIGUOUS intake must capture the router's candidates onto "
                        + "BotSession.intakeAmbiguousCandidates (Sprint 31 Option β).");
        assertArrayEquals(new String[]{"UC-A", "UC-C"}, persisted.getIntakeAmbiguousCandidates(),
                "Snapshot must preserve the router's ambiguousCandidates list verbatim.");
    }

    @Test
    void ambiguousIntake_withEmptyCandidates_assignsEmptyArray() {
        // Sprint 8 §M0 fallback path: when routing throws, the
        // RoutingResult.ambiguous(List.of()) is returned. The snapshot
        // SHALL be a (possibly empty) array, not a null silently dropped
        // by the toArray conversion — that way the projection filter
        // is exercised consistently.
        when(useCaseRouter.routeNonBlocking(any(BotSession.class), anyString(), anyString()))
                .thenReturn(RoutingResult.ambiguous(List.of()));

        sessionManager.createSession(
                "Mia", "m@test.com", "Some weak-prior topic",
                null, "I have a question");

        ArgumentCaptor<BotSession> sessionCaptor = ArgumentCaptor.forClass(BotSession.class);
        verify(sessionRepository).save(sessionCaptor.capture());

        BotSession persisted = sessionCaptor.getValue();
        assertNotNull(persisted.getIntakeAmbiguousCandidates(),
                "Empty AMBIGUOUS list must still produce a non-null snapshot.");
        assertEquals(0, persisted.getIntakeAmbiguousCandidates().length,
                "Empty AMBIGUOUS list must become an empty array on the session.");
    }

    // -------------------------------------------------------------------
    // Bar 2 — ROUTED intake leaves the field null (OQ1 pre-pick adherence)
    // -------------------------------------------------------------------

    @Test
    void routedIntake_leavesIntakeAmbiguousCandidatesNull() {
        when(useCaseRouter.routeNonBlocking(any(BotSession.class), anyString(), anyString()))
                .thenAnswer(invocation -> {
                    BotSession session = invocation.getArgument(0);
                    session.setActiveUseCase("UC-A");
                    session.setIntentConfidence(new BigDecimal("0.85"));
                    return RoutingResult.routed("UC-A", new BigDecimal("0.85"));
                });

        sessionManager.createSession(
                "Alice", "alice@test.com", "Account Support",
                null, "My account is locked and I cannot log in");

        ArgumentCaptor<BotSession> sessionCaptor = ArgumentCaptor.forClass(BotSession.class);
        verify(sessionRepository).save(sessionCaptor.capture());

        BotSession persisted = sessionCaptor.getValue();
        assertNull(persisted.getIntakeAmbiguousCandidates(),
                "ROUTED intake must leave intakeAmbiguousCandidates null (Sprint 31 OQ1 "
                        + "pre-pick — no snapshot on a deterministic single-UC route).");
    }

    @Test
    void outOfScopeIntake_leavesIntakeAmbiguousCandidatesNull() {
        when(useCaseRouter.routeNonBlocking(any(BotSession.class), anyString(), anyString()))
                .thenReturn(RoutingResult.outOfScope("OUT_OF_SCOPE_DELIVERY"));

        sessionManager.createSession(
                "Sam", "s@test.com", "Delivery",
                null, "A delivery issue");

        ArgumentCaptor<BotSession> sessionCaptor = ArgumentCaptor.forClass(BotSession.class);
        verify(sessionRepository, org.mockito.Mockito.atLeastOnce()).save(sessionCaptor.capture());

        BotSession persisted = sessionCaptor.getAllValues().get(0);
        assertNull(persisted.getIntakeAmbiguousCandidates(),
                "OUT_OF_SCOPE intake must also leave intakeAmbiguousCandidates null; "
                        + "the snapshot is intake-AMBIGUOUS-only per Sprint 31 OQ1 pre-pick.");
    }

    // -------------------------------------------------------------------
    // Bar 3 — Projection emits the slot minus the active UC
    // -------------------------------------------------------------------

    @Test
    void projection_emitsAlternateSlot_minusActiveUseCase() throws Exception {
        BotSession session = baseSession();
        session.setActiveUseCase("UC-A");
        session.setIntakeAmbiguousCandidates(new String[]{"UC-A", "UC-C"});

        String projection = projectionBuilder.buildProjection(
                session, List.of(), null, "and BTW my messages aren't going through");
        JsonNode root = objectMapper.readTree(projection);

        assertTrue(root.has("alternate_candidate_use_cases"),
                "Projection must surface alternate_candidate_use_cases on every turn.");
        JsonNode slot = root.get("alternate_candidate_use_cases");
        assertTrue(slot.isArray(), "alternate_candidate_use_cases must be a JSON array.");
        assertEquals(1, slot.size(),
                "Active UC must be filtered out of the alternates list; expected [UC-C], got " + slot);
        assertEquals("UC-C", slot.get(0).asText());
    }

    @Test
    void projection_emitsEmptyArray_whenSnapshotIsNull() throws Exception {
        // ROUTED / OUT_OF_SCOPE intake leaves intakeAmbiguousCandidates null.
        BotSession session = baseSession();
        session.setActiveUseCase("UC-A");
        session.setIntakeAmbiguousCandidates(null);

        String projection = projectionBuilder.buildProjection(
                session, List.of(), null, "where is my advert?");
        JsonNode root = objectMapper.readTree(projection);

        assertTrue(root.has("alternate_candidate_use_cases"),
                "Slot must be present even when no snapshot exists (shape stability).");
        assertEquals(0, root.get("alternate_candidate_use_cases").size(),
                "Null snapshot must produce an empty alternate_candidate_use_cases array.");
    }

    @Test
    void projection_emitsEmptyArray_whenOnlyEntryIsActiveUseCase() throws Exception {
        // Snapshot {UC-A} with active UC-A — the filter removes the only
        // entry; the slot must still be present as an empty array.
        BotSession session = baseSession();
        session.setActiveUseCase("UC-A");
        session.setIntakeAmbiguousCandidates(new String[]{"UC-A"});

        String projection = projectionBuilder.buildProjection(
                session, List.of(), null, "where is my advert?");
        JsonNode root = objectMapper.readTree(projection);

        assertEquals(0, root.get("alternate_candidate_use_cases").size(),
                "When the only snapshot entry equals the active UC, the slot must "
                        + "filter to an empty array (not absent, not null).");
    }

    @Test
    void projection_keepsMultipleAlternates_minusActiveUc() throws Exception {
        // Snapshot of three UCs, one of which is active — two survivors.
        BotSession session = baseSession();
        session.setActiveUseCase("UC-A");
        session.setIntakeAmbiguousCandidates(new String[]{"UC-A", "UC-C", "UC-D"});

        String projection = projectionBuilder.buildProjection(
                session, List.of(), null, "user message");
        JsonNode root = objectMapper.readTree(projection);
        JsonNode slot = root.get("alternate_candidate_use_cases");

        assertEquals(2, slot.size(),
                "Three-element snapshot minus the active UC must surface two alternates; got " + slot);
        assertEquals("UC-C", slot.get(0).asText());
        assertEquals("UC-D", slot.get(1).asText());
    }

    @Test
    void projection_schemaStability_keyPresentOnEveryTurn() throws Exception {
        // Whether the snapshot is null, empty, single-active, or
        // multi-element, the alternate_candidate_use_cases key MUST appear
        // in the projection so downstream consumers can rely on a stable
        // schema (§N0 nullable-field convention).
        for (String[] snapshot : new String[][] {
                null,
                new String[]{},
                new String[]{"UC-A"},
                new String[]{"UC-A", "UC-C"}}) {
            BotSession session = baseSession();
            session.setActiveUseCase("UC-A");
            session.setIntakeAmbiguousCandidates(snapshot);

            String projection = projectionBuilder.buildProjection(
                    session, List.of(), null, "any message");
            JsonNode root = objectMapper.readTree(projection);
            assertTrue(root.has("alternate_candidate_use_cases"),
                    "alternate_candidate_use_cases key must be present even when "
                            + "snapshot is " + java.util.Arrays.toString(snapshot));
            assertTrue(root.get("alternate_candidate_use_cases").isArray(),
                    "alternate_candidate_use_cases must always be an array.");
        }
    }

    // --- Helpers ------------------------------------------------------

    private BotSession baseSession() {
        return BotSession.builder()
                .sessionId("sprint31-altuc-test")
                .currentPhase("DISCOVER")
                .handlingState("BOT_HANDLING")
                .totalBotTurns(0)
                .clarificationCount(0)
                .faqMissCount(0)
                .repeatedActionCount(0)
                .build();
    }
}
