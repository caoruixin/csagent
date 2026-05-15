package com.gumtree.csagent.service.runtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gumtree.csagent.config.LlmProperties;
import com.gumtree.csagent.model.BotSession;
import com.gumtree.csagent.model.LlmRequest;
import com.gumtree.csagent.model.LlmResponse;
import com.gumtree.csagent.service.llm.LlmClient;
import com.gumtree.csagent.service.observability.LlmCallLogger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Sprint 7 §I1 — focused regression for the C2 UC-FP vs UC-A routing
 * tiebreaker, anchored on cs_interactive_015 and the cs_095 / cs_014 /
 * cs_066 negative guards.
 *
 * <p>Tests:
 * <ul>
 *   <li>{@link UseCaseRouter#buildModerationRoutingContext} extracts a
 *       moderation cue from session moderation / listing / customer
 *       contexts when present.</li>
 *   <li>routing_prompt.txt contains the Sprint 7 §I1 UC-FP tiebreaker rule.</li>
 *   <li>{@link LlmInvocationService#invokeRouting(String, String, String, String, String)}
 *       substitutes {@code {routing_context}} into the prompt and falls back
 *       to a stable "unknown" string when the cue is null.</li>
 *   <li>cs_095 / cs_014 / cs_066 negative-guard cues remain in the prompt.</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class Sprint7RoutingTiebreakerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock private UseCaseRegistryService useCaseRegistry;
    @Mock private LlmClient llmClient;
    @Mock private LlmCallLogger llmCallLogger;
    @Mock private LlmInvocationService llmInvocationMock;

    private UseCaseRouter router;
    private LlmInvocationService realLlmInvocation;
    private String routingPromptTemplate;

    @BeforeEach
    void setUp() throws Exception {
        // Load the actual routing_prompt.txt resource so the test catches any
        // accidental removal of the §I1 tiebreaker text.
        ClassPathResource res = new ClassPathResource("prompts/routing_prompt.txt");
        routingPromptTemplate = new String(res.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

        LlmProperties llmProperties = new LlmProperties();
        llmProperties.getKimi().setModel("test-model");
        realLlmInvocation = new LlmInvocationService(llmClient, llmCallLogger, llmProperties);
        ReflectionTestUtils.setField(realLlmInvocation, "systemPromptTemplate", "system");
        ReflectionTestUtils.setField(realLlmInvocation, "routingPromptTemplate", routingPromptTemplate);

        router = new UseCaseRouter(useCaseRegistry, llmInvocationMock, objectMapper);
    }

    // -------- routing_prompt.txt content --------

    @Test
    void routingPrompt_containsSprint7TiebreakerRule() {
        assertTrue(routingPromptTemplate.contains("Sprint 7"),
                "routing_prompt.txt must reference Sprint 7 §I1 (so future edits are deliberate)");
        assertTrue(routingPromptTemplate.contains("UC-FP"),
                "routing_prompt.txt must mention UC-FP in the tiebreaker section");
        assertTrue(routingPromptTemplate.contains("moderation_status")
                        || routingPromptTemplate.contains("moderation_review_decision"),
                "routing_prompt.txt must reference the moderation signal name");
        assertTrue(routingPromptTemplate.contains("{routing_context}"),
                "routing_prompt.txt must declare the {routing_context} placeholder");
    }

    @Test
    void routingPrompt_preservesNegativeGuards() {
        // cs_095 negative guard: ad-visibility / no-adverts on UC-A must not over-route.
        assertTrue(routingPromptTemplate.contains("Ad not visible")
                        || routingPromptTemplate.contains("no adverts")
                        || routingPromptTemplate.contains("UC-A is correct only when"),
                "routing_prompt.txt must keep the UC-A guard for visibility / no-ads cases");
        // cs_014 negative guard: messaging cases on UC-C.
        assertTrue(routingPromptTemplate.contains("UC-C"),
                "routing_prompt.txt must keep the UC-C messaging rule");
        // cs_066 negative guard: UC-K technical regression / generic ad-disappeared.
        assertTrue(routingPromptTemplate.contains("UC-K"),
                "routing_prompt.txt must keep the UC-K technical-regression / intake rule");
    }

    // -------- buildModerationRoutingContext signal extraction --------

    @Test
    void buildModerationRoutingContext_returnsNullWhenSessionEmpty() {
        BotSession session = baseSession();
        assertNull(router.buildModerationRoutingContext(session));
    }

    @Test
    void buildModerationRoutingContext_extractsModerationDecision() {
        BotSession session = baseSession();
        session.setModerationContext("{\"decision\":\"REMOVED\",\"reason_code\":\"PROHIBITED\"}");

        String ctx = router.buildModerationRoutingContext(session);
        assertNotNull(ctx);
        assertTrue(ctx.contains("moderation_review_decision: removed"),
                "moderation decision must be surfaced lower-cased: " + ctx);
    }

    @Test
    void buildModerationRoutingContext_extractsListingStatus() {
        BotSession session = baseSession();
        session.setListingContext("{\"ad_id\":\"AD-1\",\"status\":\"REJECTED\"}");

        String ctx = router.buildModerationRoutingContext(session);
        assertNotNull(ctx);
        assertTrue(ctx.contains("listing_status: rejected"),
                "listing status must be surfaced: " + ctx);
    }

    @Test
    void buildModerationRoutingContext_extractsAccountStatus() {
        BotSession session = baseSession();
        session.setCustomerContext("{\"account_status\":\"BLACKLISTED\"}");

        String ctx = router.buildModerationRoutingContext(session);
        assertNotNull(ctx);
        assertTrue(ctx.contains("account_status: blacklisted"),
                "account status must be surfaced: " + ctx);
    }

    @Test
    void buildModerationRoutingContext_combinesMultipleSignals() {
        BotSession session = baseSession();
        session.setModerationContext("{\"decision\":\"REMOVED\"}");
        session.setListingContext("{\"status\":\"REMOVED\"}");
        session.setCustomerContext("{\"account_status\":\"ACTIVE\"}");

        String ctx = router.buildModerationRoutingContext(session);
        assertNotNull(ctx);
        assertTrue(ctx.contains("moderation_review_decision: removed"));
        assertTrue(ctx.contains("listing_status: removed"));
        assertTrue(ctx.contains("account_status: active"));
    }

    // -------- LlmInvocationService.invokeRouting placeholder substitution --------

    @Test
    void invokeRouting_substitutesRoutingContextPlaceholder() {
        when(llmClient.chat(any(LlmRequest.class))).thenReturn(LlmResponse.builder()
                .content("{\"use_case\":\"UC-FP\",\"confidence\":0.8}")
                .finishReason("stop")
                .latencyMs(50)
                .build());

        realLlmInvocation.invokeRouting(
                "- UC-A: visibility\n- UC-FP: removal",
                "Ad Support",
                "what happened to my ad?",
                "moderation_review_decision: removed",
                "sess-cs015");

        ArgumentCaptor<LlmRequest> captor = ArgumentCaptor.forClass(LlmRequest.class);
        verify(llmClient).chat(captor.capture());
        String body = captor.getValue().getMessages().get(0).getContent();
        assertTrue(body.contains("moderation_review_decision: removed"),
                "Routing prompt must contain the substituted moderation cue: " + body);
        assertFalse(body.contains("{routing_context}"),
                "{routing_context} placeholder must be replaced");
    }

    @Test
    void invokeRouting_nullRoutingContext_substitutesUnknownStub() {
        when(llmClient.chat(any(LlmRequest.class))).thenReturn(LlmResponse.builder()
                .content("{\"use_case\":\"UC-A\",\"confidence\":0.5}")
                .finishReason("stop")
                .latencyMs(50)
                .build());

        realLlmInvocation.invokeRouting(
                "- UC-A: visibility",
                "Ad Support",
                "Why aren't my ads showing?",
                null,
                "sess-cs095");

        ArgumentCaptor<LlmRequest> captor = ArgumentCaptor.forClass(LlmRequest.class);
        verify(llmClient).chat(captor.capture());
        String body = captor.getValue().getMessages().get(0).getContent();
        assertTrue(body.contains("moderation_status: unknown"),
                "null routing context must fall back to a stable unknown stub");
    }

    @Test
    void invokeRouting_legacyOverloadDelegatesToFiveArgForm() {
        // Legacy 4-arg signature must continue to pass through to the new
        // overload with a null routing_context (no behaviour change for callers
        // that have not yet been updated).
        when(llmClient.chat(any(LlmRequest.class))).thenReturn(LlmResponse.builder()
                .content("{\"use_case\":\"UC-A\",\"confidence\":0.5}")
                .finishReason("stop")
                .latencyMs(50)
                .build());

        realLlmInvocation.invokeRouting(
                "- UC-A: visibility",
                "Ad Support",
                "what happened to my ad?",
                "sess-legacy");

        ArgumentCaptor<LlmRequest> captor = ArgumentCaptor.forClass(LlmRequest.class);
        verify(llmClient).chat(captor.capture());
        String body = captor.getValue().getMessages().get(0).getContent();
        assertTrue(body.contains("moderation_status: unknown"),
                "legacy 4-arg invokeRouting must surface the unknown stub");
    }

    private BotSession baseSession() {
        return BotSession.builder()
                .sessionId("test-routing-i1")
                .currentPhase("DISCOVER")
                .handlingState("BOT_HANDLING")
                .totalBotTurns(0)
                .clarificationCount(0)
                .faqMissCount(0)
                .repeatedActionCount(0)
                .build();
    }
}
