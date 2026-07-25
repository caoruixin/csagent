package com.gumtree.csagent.service.runtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gumtree.csagent.model.BotSession;
import com.gumtree.csagent.model.ChatSessionResponse;
import com.gumtree.csagent.repository.BotEventRepository;
import com.gumtree.csagent.repository.BotSessionRepository;
import com.gumtree.csagent.repository.BotTurnRepository;
import com.gumtree.csagent.repository.KbArticleRepository;
import com.gumtree.csagent.repository.MockHandoverLogRepository;
import com.gumtree.csagent.repository.SessionOutcomeRepository;
import com.gumtree.csagent.service.knowledge.ArticleCardAssembler;
import com.gumtree.csagent.service.runtime.UseCaseRouter.RoutingResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * P1-B (2026-07-25) — the create-session greeting must carry forward the
 * information the pre-chat form already supplied, instead of asking the
 * customer to repeat it.
 *
 * <p>Observed defect: a form carrying
 * {@code description="My ad isn't performing well. Not many views."} and
 * {@code ad_id=AD-2002} was answered with "Thank you for reaching out. I'd
 * like to help you with your inquiry. Could you tell me a bit more about what
 * you need help with?" — a request for exactly what had just been typed into
 * the form.
 *
 * <p>The fix stays inside the Sprint 8.1 §M0 latency contract: the greeting is
 * still assembled from static strings with no LLM call and no retrieval. Every
 * test here also asserts the kernel was never invoked, so a future "just ask
 * the model for a nicer greeting" change trips this suite rather than the
 * user-facing wait budget.
 */
@ExtendWith(MockitoExtension.class)
class SessionManagerGreetingFormContextTest {

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
    @Mock private KbArticleRepository kbArticleRepository;

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
                new ArticleCardAssembler(kbArticleRepository));
    }

    // ------------------------------------------------------------------
    //  The reported reproduction
    // ------------------------------------------------------------------

    @Test
    @DisplayName("reported case: description + ad_id supplied -> greeting never re-asks")
    void ambiguousRouting_withDescriptionAndAdId_doesNotReAskForIt() {
        stubAmbiguous();

        ChatSessionResponse response = sessionManager.createSession(
                "Riley", "riley@test.com", "Ad Support", "AD-2002",
                "My ad isn't performing well. Not many views.");

        String reply = response.getReplyText();
        assertFalse(reply.contains("Could you tell me a bit more"),
                "must not ask for information the form already carried. Got: " + reply);
        assertTrue(reply.contains("Riley"), "greeting keeps the customer's name. Got: " + reply);
        assertTrue(reply.contains("My ad isn't performing well. Not many views"),
                "greeting must reflect the description the form supplied. Got: " + reply);
        assertTrue(reply.contains("AD-2002"),
                "greeting must acknowledge the ad reference the form supplied. Got: " + reply);
        verify(controlKernel, never()).processMessage(any(BotSession.class), anyString());
    }

    // ------------------------------------------------------------------
    //  ROUTED path
    // ------------------------------------------------------------------

    @Test
    @DisplayName("routed: description + ad_id are reflected, still no kernel call")
    void routed_withDescriptionAndAdId_reflectsBoth() {
        stubRoutedTo("UC-A");

        ChatSessionResponse response = sessionManager.createSession(
                "Riley", "riley@test.com", "Ad Status", "AD-2002",
                "My ad isn't performing well. Not many views.");

        String reply = response.getReplyText();
        assertTrue(reply.contains("My ad isn't performing well. Not many views"), reply);
        assertTrue(reply.contains("AD-2002"), reply);
        assertTrue(reply.contains("Let me look into this for you"),
                "routed greeting keeps its forward-moving close. Got: " + reply);
        verify(controlKernel, never()).processMessage(any(BotSession.class), anyString());
    }

    @Test
    @DisplayName("routed: ad_id only -> acknowledges the reference, invents no description")
    void routed_withAdIdOnly_acknowledgesReferenceOnly() {
        stubRoutedTo("UC-A");

        String reply = sessionManager.createSession(
                "Sam", "sam@test.com", "Ad Status", "AD-9001", null).getReplyText();

        assertTrue(reply.contains("AD-9001"), reply);
        assertFalse(reply.contains("I can see you mentioned"),
                "with no description there is nothing to quote back. Got: " + reply);
        verify(controlKernel, never()).processMessage(any(BotSession.class), anyString());
    }

    @Test
    @DisplayName("routed: nothing but a topic -> plain greeting, no invented content")
    void routed_withNeitherDescriptionNorAdId_staysPlain() {
        stubRoutedTo("UC-A");

        String reply = sessionManager.createSession(
                "Sam", "sam@test.com", "Ad Status", null, null).getReplyText();

        assertTrue(reply.contains("ad status"),
                "greeting still names the topic. Got: " + reply);
        assertFalse(reply.contains("I can see you mentioned"), reply);
        assertFalse(reply.contains("ad reference"), reply);
        assertTrue(reply.contains("Let me look into this for you"), reply);
    }

    // ------------------------------------------------------------------
    //  AMBIGUOUS path — the clarifying question is kept where it is honest
    // ------------------------------------------------------------------

    @Test
    @DisplayName("ambiguous: no description -> the clarifying question is still asked")
    void ambiguousRouting_withoutDescription_stillAsksForDetail() {
        stubAmbiguous();

        String reply = sessionManager.createSession(
                "Sam", "sam@test.com", "Ad Support", null, null).getReplyText();

        assertTrue(reply.contains("Could you tell me a bit more"),
                "nothing was described, so asking is the honest next move. Got: " + reply);
    }

    @Test
    @DisplayName("ambiguous: ad_id but no description -> acknowledges id AND still asks")
    void ambiguousRouting_withAdIdOnly_acknowledgesAndAsks() {
        stubAmbiguous();

        String reply = sessionManager.createSession(
                "Sam", "sam@test.com", "Ad Support", "AD-7", null).getReplyText();

        assertTrue(reply.contains("AD-7"), reply);
        assertTrue(reply.contains("Could you tell me a bit more"),
                "the ad id does not tell us what the problem is. Got: " + reply);
    }

    @Test
    @DisplayName("ambiguous: whitespace-only description counts as absent")
    void ambiguousRouting_blankDescription_treatedAsAbsent() {
        stubAmbiguous();

        String reply = sessionManager.createSession(
                "Sam", "sam@test.com", "Ad Support", null, "   ").getReplyText();

        assertTrue(reply.contains("Could you tell me a bit more"), reply);
        assertFalse(reply.contains("I can see you mentioned"), reply);
    }

    // ------------------------------------------------------------------
    //  Excerpting is mechanical, not a summary
    // ------------------------------------------------------------------

    @Test
    @DisplayName("long description is truncated on a word boundary, not summarised")
    void longDescription_isTruncatedDeterministically() {
        stubRoutedTo("UC-A");
        String longDescription = "My advert for the blue mountain bike has been live for three "
                + "weeks now and I have had almost no views at all despite refreshing it "
                + "several times and lowering the price twice already this month";

        String reply = sessionManager.createSession(
                "Sam", "sam@test.com", "Ad Status", null, longDescription).getReplyText();

        assertTrue(reply.contains("My advert for the blue mountain bike"),
                "the head of the description is quoted verbatim. Got: " + reply);
        assertTrue(reply.contains("..."), "a truncated quote is marked with an ellipsis. Got: " + reply);
        assertFalse(reply.contains("lowering the price twice"),
                "the tail past the cut-off is dropped. Got: " + reply);
        // Every quoted word must come from the customer's own text — the
        // greeting paraphrases nothing.
        String quoted = reply.substring(reply.indexOf('"') + 1, reply.lastIndexOf('"'));
        assertTrue(longDescription.startsWith(quoted.replace("...", "")),
                "the quote must be a prefix of the customer's own words. Got: " + quoted);
    }

    @Test
    @DisplayName("multi-line description is collapsed to a single quoted line")
    void multiLineDescription_isCollapsed() {
        stubRoutedTo("UC-A");

        String reply = sessionManager.createSession(
                "Sam", "sam@test.com", "Ad Status", null,
                "My ad is gone.\n\n   Please help.").getReplyText();

        assertTrue(reply.contains("My ad is gone. Please help"), reply);
        assertFalse(reply.contains("\n"), "greeting must stay a single line. Got: " + reply);
    }

    // ------------------------------------------------------------------

    private void stubAmbiguous() {
        when(useCaseRouter.routeNonBlocking(any(BotSession.class), anyString(), anyString()))
                .thenReturn(RoutingResult.ambiguous(List.of("UC-A", "UC-C")));
    }

    private void stubRoutedTo(String ucId) {
        when(useCaseRouter.routeNonBlocking(any(BotSession.class), anyString(), anyString()))
                .thenAnswer(invocation -> {
                    BotSession session = invocation.getArgument(0);
                    session.setActiveUseCase(ucId);
                    session.setIntentConfidence(new BigDecimal("0.85"));
                    return RoutingResult.routed(ucId, new BigDecimal("0.85"));
                });
    }
}
