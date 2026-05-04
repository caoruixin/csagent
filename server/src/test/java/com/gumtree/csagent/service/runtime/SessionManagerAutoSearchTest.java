package com.gumtree.csagent.service.runtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gumtree.csagent.model.BotSession;
import com.gumtree.csagent.model.ChatSessionResponse;
import com.gumtree.csagent.repository.BotEventRepository;
import com.gumtree.csagent.repository.BotSessionRepository;
import com.gumtree.csagent.repository.BotTurnRepository;
import com.gumtree.csagent.repository.MockHandoverLogRepository;
import com.gumtree.csagent.repository.SessionOutcomeRepository;
import com.gumtree.csagent.service.runtime.ControlKernel.KernelResult;
import com.gumtree.csagent.service.runtime.UseCaseRouter.RoutingResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

/**
 * Unit tests for SessionManager.createSession() auto-search feature (D14.6).
 *
 * Auto-search behaviour: when routing identifies a FAQ use case (not INTAKE:
 * UC-G/H/I/J/K) AND the form description is substantive (> 10 chars),
 * the bot calls controlKernel.processMessage() and uses the result as the
 * greeting instead of the static "Let me look into this for you" template.
 */
@ExtendWith(MockitoExtension.class)
class SessionManagerAutoSearchTest {

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
                useCaseRegistry, objectMapper, handoverAssembler, resolver);
    }

    /**
     * FAQ UC + substantive description -> processMessage called, greeting
     * contains kernel result text.
     */
    @Test
    void createSession_faqUcWithSubstantiveDescription_shouldAutoSearch() {
        // Arrange: route to UC-A (FAQ), not in INTAKE set
        stubRoutingToUc("UC-A", "Account Support FAQ");

        KernelResult kernelResult = new KernelResult(
                "Based on our knowledge base, here is how to reset your password...", false, 150L);
        when(controlKernel.processMessage(any(BotSession.class), eq("My account is locked and I cannot log in at all")))
                .thenReturn(kernelResult);

        // Act
        ChatSessionResponse response = sessionManager.createSession(
                "Alice", "alice@test.com", "Account Support",
                null, "My account is locked and I cannot log in at all");

        // Assert: greeting should contain kernel result, not static template
        assertNotNull(response);
        assertTrue(response.getReplyText().contains("Based on our knowledge base"),
                "Greeting should contain the kernel result text, not the static template. Got: " + response.getReplyText());
        assertTrue(response.getReplyText().startsWith("Hi Alice!"),
                "Greeting should start with personalised 'Hi <name>!'. Got: " + response.getReplyText());
        assertFalse(response.getReplyText().contains("Let me look into this for you"),
                "Greeting should NOT contain the static template text");

        // Verify processMessage was invoked
        verify(controlKernel).processMessage(any(BotSession.class), eq("My account is locked and I cannot log in at all"));
        // Verify session was saved BEFORE processMessage (for turn recording)
        // sessionRepository.save is called at least twice: once before processMessage, once after
        verify(sessionRepository, atLeast(2)).save(any(BotSession.class));
    }

    /**
     * FAQ UC + short description (<=10 chars) -> static greeting, no auto-search.
     */
    @Test
    void createSession_faqUcWithShortDescription_shouldReturnStaticGreeting() {
        stubRoutingToUc("UC-B", "Billing FAQ");

        // Act: description is only 5 chars, below the >10 threshold
        ChatSessionResponse response = sessionManager.createSession(
                "Bob", "bob@test.com", "Billing",
                null, "help!");

        // Assert: static greeting (not auto-search)
        assertNotNull(response);
        assertTrue(response.getReplyText().contains("Let me look into this for you"),
                "Short description should produce static greeting. Got: " + response.getReplyText());

        // Verify processMessage was NOT called
        verify(controlKernel, never()).processMessage(any(BotSession.class), anyString());
    }

    /**
     * FAQ UC + null description -> static greeting, no auto-search.
     */
    @Test
    void createSession_faqUcWithNullDescription_shouldReturnStaticGreeting() {
        stubRoutingToUc("UC-A", "Account Support FAQ");

        ChatSessionResponse response = sessionManager.createSession(
                "Charlie", "c@test.com", "Account Support",
                null, null);

        assertNotNull(response);
        assertTrue(response.getReplyText().contains("Let me look into this for you"),
                "Null description should produce static greeting. Got: " + response.getReplyText());
        verify(controlKernel, never()).processMessage(any(BotSession.class), anyString());
    }

    /**
     * FAQ UC + exactly 10 chars description -> static greeting (boundary: > 10, not >= 10).
     */
    @Test
    void createSession_faqUcWithExactly10CharDescription_shouldReturnStaticGreeting() {
        stubRoutingToUc("UC-A", "Account Support FAQ");

        // Exactly 10 chars: "1234567890"
        String tenChars = "1234567890";
        assertEquals(10, tenChars.length());

        ChatSessionResponse response = sessionManager.createSession(
                "Dave", "d@test.com", "Account Support",
                null, tenChars);

        assertNotNull(response);
        assertTrue(response.getReplyText().contains("Let me look into this for you"),
                "Exactly 10-char description should produce static greeting (> 10 required). Got: " + response.getReplyText());
        verify(controlKernel, never()).processMessage(any(BotSession.class), anyString());
    }

    /**
     * FAQ UC + 11 chars description -> should trigger auto-search (boundary: > 10).
     */
    @Test
    void createSession_faqUcWith11CharDescription_shouldAutoSearch() {
        stubRoutingToUc("UC-A", "Account Support FAQ");

        String elevenChars = "12345678901";
        assertEquals(11, elevenChars.length());

        KernelResult kernelResult = new KernelResult("Auto-search result", false, 100L);
        when(controlKernel.processMessage(any(BotSession.class), eq(elevenChars)))
                .thenReturn(kernelResult);

        ChatSessionResponse response = sessionManager.createSession(
                "Eve", "e@test.com", "Account Support",
                null, elevenChars);

        assertNotNull(response);
        assertTrue(response.getReplyText().contains("Auto-search result"),
                "11-char description should trigger auto-search. Got: " + response.getReplyText());
        verify(controlKernel).processMessage(any(BotSession.class), eq(elevenChars));
    }

    /**
     * INTAKE UC (UC-H) + substantive description -> static greeting, no auto-search.
     * INTAKE UCs must never trigger auto-search because they use fixed script templates.
     */
    @Test
    void createSession_intakeUcH_withSubstantiveDescription_shouldReturnStaticGreeting() {
        stubRoutingToUc("UC-H", "Ad Appeal Intake");

        ChatSessionResponse response = sessionManager.createSession(
                "Frank", "f@test.com", "Ad removed",
                "ad-123", "My ad was removed unfairly and I want to appeal the decision");

        assertNotNull(response);
        assertTrue(response.getReplyText().contains("Let me look into this for you"),
                "INTAKE UC-H should produce static greeting regardless of description. Got: " + response.getReplyText());
        verify(controlKernel, never()).processMessage(any(BotSession.class), anyString());
    }

    /**
     * INTAKE UC (UC-G) should not trigger auto-search.
     */
    @Test
    void createSession_intakeUcG_withSubstantiveDescription_shouldReturnStaticGreeting() {
        stubRoutingToUc("UC-G", "Data Protection Intake");

        ChatSessionResponse response = sessionManager.createSession(
                "Grace", "g@test.com", "Data Protection",
                null, "I want to request deletion of my personal data from your platform");

        assertNotNull(response);
        verify(controlKernel, never()).processMessage(any(BotSession.class), anyString());
    }

    /**
     * INTAKE UC (UC-I) should not trigger auto-search.
     */
    @Test
    void createSession_intakeUcI_withSubstantiveDescription_shouldReturnStaticGreeting() {
        stubRoutingToUc("UC-I", "Payment Intake");

        ChatSessionResponse response = sessionManager.createSession(
                "Ivy", "i@test.com", "Payments",
                null, "I was charged twice for my ad and need a refund please");

        verify(controlKernel, never()).processMessage(any(BotSession.class), anyString());
    }

    /**
     * INTAKE UC (UC-J) should not trigger auto-search.
     */
    @Test
    void createSession_intakeUcJ_withSubstantiveDescription_shouldReturnStaticGreeting() {
        stubRoutingToUc("UC-J", "Safety Reporting Intake");

        ChatSessionResponse response = sessionManager.createSession(
                "Joe", "j@test.com", "Safety",
                null, "I want to report a fraudulent listing on the platform");

        verify(controlKernel, never()).processMessage(any(BotSession.class), anyString());
    }

    /**
     * INTAKE UC (UC-K) should not trigger auto-search.
     */
    @Test
    void createSession_intakeUcK_withSubstantiveDescription_shouldReturnStaticGreeting() {
        stubRoutingToUc("UC-K", "Technical Support Intake");

        ChatSessionResponse response = sessionManager.createSession(
                "Kate", "k@test.com", "Technical",
                null, "The website keeps crashing whenever I try to upload photos");

        verify(controlKernel, never()).processMessage(any(BotSession.class), anyString());
    }

    /**
     * FAQ UC + processMessage throws exception -> falls back to static greeting.
     */
    @Test
    void createSession_faqUcProcessMessageThrows_shouldFallbackToStaticGreeting() {
        stubRoutingToUc("UC-A", "Account Support FAQ");

        when(controlKernel.processMessage(any(BotSession.class), anyString()))
                .thenThrow(new RuntimeException("LLM service unavailable"));

        ChatSessionResponse response = sessionManager.createSession(
                "Leo", "l@test.com", "Account Support",
                null, "I need help resetting my password and recovering my account");

        assertNotNull(response);
        assertTrue(response.getReplyText().contains("Let me look into this for you"),
                "When processMessage throws, should fall back to static greeting. Got: " + response.getReplyText());
        assertFalse(response.isShouldEndChat(),
                "Fallback greeting should not end the chat");
    }

    /**
     * AMBIGUOUS routing (no activeUseCase) -> no auto-search, ambiguous greeting.
     */
    @Test
    void createSession_ambiguousRouting_shouldNotAutoSearch() {
        when(useCaseRouter.route(any(BotSession.class), anyString(), anyString()))
                .thenReturn(RoutingResult.ambiguous(List.of("UC-A", "UC-B")));

        ChatSessionResponse response = sessionManager.createSession(
                "Mia", "m@test.com", "Account Support",
                null, "I have a complex issue with my account and billing");

        assertNotNull(response);
        // Ambiguous greeting asks for clarification
        assertTrue(response.getReplyText().contains("Could you tell me a bit more"),
                "Ambiguous routing should produce disambiguating greeting. Got: " + response.getReplyText());
        verify(controlKernel, never()).processMessage(any(BotSession.class), anyString());
    }

    /**
     * When kernelResult.shouldEndChat() is true, the response should reflect that.
     */
    @Test
    void createSession_faqUcAutoSearchEndsChat_shouldSetShouldEndChat() {
        stubRoutingToUc("UC-A", "Account Support FAQ");

        KernelResult kernelResult = new KernelResult(
                "Your issue has been resolved.", true, 200L);
        when(controlKernel.processMessage(any(BotSession.class), anyString()))
                .thenReturn(kernelResult);

        ChatSessionResponse response = sessionManager.createSession(
                "Nora", "n@test.com", "Account Support",
                null, "How do I change my email address on my profile?");

        assertNotNull(response);
        assertTrue(response.isShouldEndChat(),
                "When kernel says shouldEndChat=true, response should end chat");
        assertTrue(response.getReplyText().contains("Your issue has been resolved"),
                "Greeting should contain the kernel result. Got: " + response.getReplyText());
    }

    /**
     * When firstName is null/blank, auto-search greeting should use "there" as fallback.
     */
    @Test
    void createSession_faqUcAutoSearch_nullFirstName_shouldUseThereAsFallback() {
        stubRoutingToUc("UC-A", "Account Support FAQ");

        KernelResult kernelResult = new KernelResult("Here is the answer.", false, 100L);
        when(controlKernel.processMessage(any(BotSession.class), anyString()))
                .thenReturn(kernelResult);

        ChatSessionResponse response = sessionManager.createSession(
                null, "anon@test.com", "Account Support",
                null, "I need help with my account settings");

        assertNotNull(response);
        assertTrue(response.getReplyText().startsWith("Hi there!"),
                "Null firstName should use 'there' in auto-search greeting. Got: " + response.getReplyText());
    }

    /**
     * Verify that auto-search saves the session BEFORE calling processMessage.
     * This is critical because ControlKernel.processMessage records turns to DB
     * and requires the session to be persisted first.
     */
    @Test
    void createSession_faqUcAutoSearch_sessionSavedBeforeProcessMessage() {
        stubRoutingToUc("UC-A", "Account Support FAQ");

        KernelResult kernelResult = new KernelResult("Answer text", false, 100L);
        when(controlKernel.processMessage(any(BotSession.class), anyString()))
                .thenReturn(kernelResult);

        sessionManager.createSession(
                "Pat", "p@test.com", "Account Support",
                null, "How can I delete my old advertisements?");

        // Verify save was called at least twice
        // First save: before processMessage (so ControlKernel can record turns)
        // Second save: after greeting is determined (final session state)
        ArgumentCaptor<BotSession> captor = ArgumentCaptor.forClass(BotSession.class);
        verify(sessionRepository, atLeast(2)).save(captor.capture());

        // The first save should have the session with phase RESOLVE and UC set
        BotSession firstSave = captor.getAllValues().get(0);
        assertEquals("RESOLVE", firstSave.getCurrentPhase(),
                "Session should be in RESOLVE phase before processMessage");
        assertEquals("UC-A", firstSave.getActiveUseCase(),
                "Session should have activeUseCase set before processMessage");
    }

    // --- Helper methods ---

    /**
     * Stubs the routing to return ROUTED to a specific UC.
     * Also stubs the UC registry to return a definition with the given name.
     */
    private void stubRoutingToUc(String ucId, String ucName) {
        when(useCaseRouter.route(any(BotSession.class), anyString(), anyString()))
                .thenAnswer(invocation -> {
                    BotSession session = invocation.getArgument(0);
                    session.setActiveUseCase(ucId);
                    session.setIntentConfidence(new BigDecimal("0.85"));
                    return RoutingResult.routed(ucId, new BigDecimal("0.85"));
                });

        UseCaseRegistryService.UseCaseDefinition ucDef =
                new UseCaseRegistryService.UseCaseDefinition(
                        ucId, ucName, List.of(), "low", true, "FAQ");
        lenient().when(useCaseRegistry.getUseCase(ucId)).thenReturn(ucDef);
    }
}
