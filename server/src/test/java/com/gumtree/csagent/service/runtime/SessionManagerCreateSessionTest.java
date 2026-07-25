package com.gumtree.csagent.service.runtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gumtree.csagent.model.BotSession;
import com.gumtree.csagent.model.ChatSessionResponse;
import com.gumtree.csagent.repository.BotEventRepository;
import com.gumtree.csagent.repository.BotSessionRepository;
import com.gumtree.csagent.repository.BotTurnRepository;
import com.gumtree.csagent.repository.MockHandoverLogRepository;
import com.gumtree.csagent.repository.SessionOutcomeRepository;
import com.gumtree.csagent.service.runtime.UseCaseRouter.RoutingResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Sprint 8 §D1: session creation must never block on an LLM call. The previous
 * "auto-search at create-time" feature was removed because it routinely
 * exceeded the user-facing wait budget; this test pins the new contract so a
 * future change cannot accidentally re-introduce a synchronous LLM call on the
 * Start Chat path.
 *
 * <p>Replaces the deleted {@code SessionManagerAutoSearchTest}.
 */
@ExtendWith(MockitoExtension.class)
class SessionManagerCreateSessionTest {

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

    private SessionManager sessionManager;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        EscalationReasonResolver resolver = new EscalationReasonResolver();
        HandoverPayloadAssembler handoverAssembler = new HandoverPayloadAssembler(
                botTurnRepository, resolver, objectMapper);
        sessionManager = new SessionManager(
                sessionRepository, eventRepository, outcomeRepository,
                handoverLogRepository, botTurnRepository, formIngestion,
                useCaseRouter, controlKernel, controlPolicy,
                useCaseRegistry, objectMapper, handoverAssembler, resolver,
                new com.gumtree.csagent.service.knowledge.ArticleCardAssembler(kbArticleRepository));
    }

    @Test
    void createSession_routedFaqUcWithSubstantiveDescription_doesNotInvokeControlKernel() {
        // Previously this combination triggered auto-search; under §D1 it
        // must produce a static greeting and zero LLM calls.
        stubRoutingToUc("UC-A");

        ChatSessionResponse response = sessionManager.createSession(
                "Alice", "alice@test.com", "Account Support",
                null, "My account is locked and I cannot log in at all");

        assertNotNull(response);
        assertTrue(response.getReplyText().contains("Let me look into this for you"),
                "ROUTED FAQ UC must produce the static greeting, not a kernel-generated one. Got: "
                        + response.getReplyText());
        verify(controlKernel, never()).processMessage(any(BotSession.class), anyString());
    }

    @Test
    void createSession_routedIntakeUc_doesNotInvokeControlKernel() {
        // INTAKE UCs were already excluded from auto-search; verify the new
        // unified contract: no LLM call at create-time, period.
        stubRoutingToUc("UC-H");

        sessionManager.createSession(
                "Frank", "f@test.com", "Ad removed",
                "ad-123", "My ad was removed unfairly and I want to appeal the decision");

        verify(controlKernel, never()).processMessage(any(BotSession.class), anyString());
    }

    @Test
    void createSession_ambiguousRouting_doesNotInvokeControlKernel() {
        when(useCaseRouter.routeNonBlocking(any(BotSession.class), anyString(), anyString()))
                .thenReturn(RoutingResult.ambiguous(List.of("UC-A", "UC-B")));

        ChatSessionResponse response = sessionManager.createSession(
                "Mia", "m@test.com", "Account Support",
                null, "I have a complex issue with my account and billing");

        assertNotNull(response);
        // P1-B (2026-07-25): the assertion used to require the literal
        // "Could you tell me a bit more" here even though this fixture DOES
        // supply a description. Re-asking for information the form already
        // carried is the defect; the disambiguating question is now reserved
        // for the description-blank case, pinned by
        // SessionManagerGreetingFormContextTest.
        assertFalse(response.getReplyText().contains("Could you tell me a bit more"),
                "Ambiguous routing with a supplied description must NOT re-ask for it. Got: "
                        + response.getReplyText());
        assertTrue(response.getReplyText().contains("complex issue with my account and billing"),
                "Ambiguous greeting must carry forward the description the form supplied. Got: "
                        + response.getReplyText());
        verify(controlKernel, never()).processMessage(any(BotSession.class), anyString());
    }

    @Test
    void createSession_routed_doesNotEndChat() {
        // Auto-search used to be able to end the chat at create-time via
        // kernelResult.shouldEndChat(); under §D1 only hard-OOS can do that.
        stubRoutingToUc("UC-A");

        ChatSessionResponse response = sessionManager.createSession(
                "Pat", "p@test.com", "Account Support",
                null, "How can I delete my old advertisements?");

        assertFalse(response.isShouldEndChat(),
                "ROUTED createSession must keep the chat open so the user can send their first message.");
    }

    @Test
    void createSession_callsRouteNonBlocking_neverInvokesRouteThatMayCallLlm() {
        // Sprint 8.1 §M0: the create-session path must call the
        // non-blocking router variant so the LLM is never invoked
        // synchronously during form submit. Pin it.
        when(useCaseRouter.routeNonBlocking(any(BotSession.class), anyString(), anyString()))
                .thenReturn(RoutingResult.ambiguous(List.of("UC-A", "UC-B")));

        sessionManager.createSession(
                "Alice", "alice@test.com", "Ad Support",
                "123", "where is my ad?");

        verify(useCaseRouter).routeNonBlocking(any(BotSession.class),
                eq("Ad Support"), eq("where is my ad?"));
        verify(useCaseRouter, never()).route(any(BotSession.class), anyString(), anyString());
        verify(controlKernel, never()).processMessage(any(BotSession.class), anyString());
    }

    @Test
    void createSession_persistsFormContextOnAdSupport_returnsImmediately() {
        // Sprint 8.1 §M0: the prefilled "Ad Support" form must persist
        // form context (topic / description / ad_id / email / first_name)
        // and return a chat-ready response without any LLM call.
        when(useCaseRouter.routeNonBlocking(any(BotSession.class), anyString(), anyString()))
                .thenAnswer(invocation -> {
                    // The non-blocking router does not commit a UC for
                    // weak-prior topics; SessionManager will land the
                    // session in DISCOVER.
                    return RoutingResult.ambiguous(List.of("UC-A", "UC-B", "UC-FP"));
                });

        long start = System.currentTimeMillis();
        ChatSessionResponse response = sessionManager.createSession(
                "joh", "joh@e.com", "Ad Support",
                "123", "where is my ad?");
        long elapsed = System.currentTimeMillis() - start;

        assertNotNull(response);
        assertNotNull(response.getSessionId());
        assertFalse(response.isShouldEndChat(),
                "Pre-filled Ad Support submit must keep the chat open so "
                        + "the user can add more information.");
        assertTrue(elapsed < 1000L,
                "Sprint 8.1 §M0: pre-filled form submit must return well "
                        + "below the 1 s mark (no LLM call). Got: " + elapsed + " ms.");
        verify(formIngestion).ingest(any(BotSession.class),
                eq("joh"), eq("joh@e.com"), eq("Ad Support"),
                eq("123"), eq("where is my ad?"));
        verify(controlKernel, never()).processMessage(any(BotSession.class), anyString());
    }

    private void stubRoutingToUc(String ucId) {
        when(useCaseRouter.routeNonBlocking(any(BotSession.class), anyString(), anyString()))
                .thenAnswer(invocation -> {
                    BotSession session = invocation.getArgument(0);
                    session.setActiveUseCase(ucId);
                    session.setIntentConfidence(new BigDecimal("0.85"));
                    return RoutingResult.routed(ucId, new BigDecimal("0.85"));
                });
    }
}
