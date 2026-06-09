package com.gumtree.csagent.service.runtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gumtree.csagent.model.AgentRunResult;
import com.gumtree.csagent.model.BotSession;
import com.gumtree.csagent.model.LlmResponse;
import com.gumtree.csagent.model.ParsedAction;
import com.gumtree.csagent.model.PhasePlan;
import com.gumtree.csagent.model.TerminalOutcome;
import com.gumtree.csagent.model.ToolCall;
import com.gumtree.csagent.model.ToolEvent;
import com.gumtree.csagent.service.tools.ToolDispatcher;
import com.gumtree.csagent.service.tools.ToolPolicyEnforcer;
import com.gumtree.csagent.service.tools.ToolResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Sprint 067 / S-Auto-12 (M-Auto-3) — negative controls + prompt-binding
 * coverage for the A1 identity-dedup 回挡.
 *
 * <p>The end-to-end "byte-identical success repeat is deduped" bar is
 * covered by {@code AgentRunLoopIdentityDedupIntegrationTest}. This class
 * covers the two negative controls that keep the backstop from
 * over-firing, plus the binding-soft-signal half of the hybrid:
 *
 * <ul>
 *   <li>(a) a normal single call is NOT deduped (no spurious cache hit);</li>
 *   <li>(b) a same-args retry after a non-{@code success} (external-failure)
 *       result RE-dispatches — failed results never enter the cache, so a
 *       legitimate retry still reaches the tool;</li>
 *   <li>(d) the system prompt teaches the {@code already_called} slot as a
 *       BINDING signal (auto-dedup of byte-identical repeats) while still
 *       leaving the LLM ownership of which tool / what content.</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class AgentRunLoopIdentityDedupTest {

    @Mock private LlmInvocationService llmInvocation;
    @Mock private ToolDispatcher toolDispatcher;
    @Mock private ActionParser actionParser;
    @Mock private UseCaseRegistryService useCaseRegistry;
    @Mock private ControlPolicyService controlPolicy;
    @Mock private ToolPolicyEnforcer toolPolicyEnforcer;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private AgentRunLoop agentRunLoop;

    @BeforeEach
    void setUp() {
        ContextProjectionBuilder projectionBuilder = new ContextProjectionBuilder(
                objectMapper, useCaseRegistry, controlPolicy, toolPolicyEnforcer, null);
        agentRunLoop = new AgentRunLoopImpl(
                llmInvocation, toolDispatcher, projectionBuilder, actionParser, objectMapper);

        lenient().when(controlPolicy.getMaxBotTurnsFaq()).thenReturn(6);
        lenient().when(controlPolicy.getMaxClarificationRounds()).thenReturn(3);
        lenient().when(controlPolicy.getMaxFaqMiss()).thenReturn(2);

        UseCaseRegistryService.UseCaseDefinition uca = new UseCaseRegistryService.UseCaseDefinition(
                "UC-A", "Ad Status & Visibility", List.of("Ad Support"), "LOW", true, "FAQ");
        lenient().when(useCaseRegistry.getUseCase("UC-A")).thenReturn(uca);
        lenient().when(toolPolicyEnforcer.getVisibleToolsForUc("UC-A")).thenReturn(List.of(
                "search_knowledge", "resolve_article", "get_customer_context",
                "request_handover", "record_outcome"));
    }

    private static PhasePlan resolvePlan() {
        return PhasePlan.builder()
                .phase("RESOLVE")
                .useCase("UC-A")
                .objective("answer")
                .allowedTools(List.of("search_knowledge", "resolve_article",
                        "request_handover", "record_outcome"))
                .maxToolSteps(4)
                .systemInstruction("RESOLVE")
                .validTerminalOutcomes(Set.of(TerminalOutcome.FINAL_ANSWER,
                        TerminalOutcome.ESCALATE))
                .build();
    }

    private static BotSession session() {
        return BotSession.builder()
                .sessionId("sauto12-negctl")
                .activeUseCase("UC-A")
                .currentPhase("RESOLVE")
                .handlingState("BOT_HANDLING")
                .totalBotTurns(0)
                .clarificationCount(0)
                .faqMissCount(0)
                .repeatedActionCount(0)
                .build();
    }

    private static Map<String, Object> args() {
        Map<String, Object> a = new LinkedHashMap<>();
        a.put("query", "why can't I see my advert");
        a.put("uc_tags", List.of("UC-A"));
        return a;
    }

    // ── (a) normal single call is NOT deduped ──────────────────────────
    @Test
    void singleCall_isNotDeduped() {
        ToolCall call = ToolCall.builder().name("search_knowledge").arguments(args()).build();

        LlmResponse rawA = LlmResponse.builder().content("{\"step\":0}").build();
        LlmResponse rawB = LlmResponse.builder().content("{\"step\":1}").build();
        when(llmInvocation.invokeChat(anyString(), anyString(), anyString(), anyInt()))
                .thenReturn(rawA).thenReturn(rawB);
        when(actionParser.parse(rawA.getContent())).thenReturn(ParsedAction.builder()
                .toolCalls(List.of(call)).userMessage("").reasoning("look up").build());
        when(actionParser.parse(rawB.getContent())).thenReturn(ParsedAction.builder()
                .toolCalls(List.of()).userMessage("Here is what I found.").reasoning("done").build());
        when(toolDispatcher.validateAgainstPlan(any(), eq("search_knowledge")))
                .thenReturn(ToolResult.ok(null));
        when(toolDispatcher.dispatch(eq("search_knowledge"), any(), any()))
                .thenReturn(ToolResult.ok(Map.of("faq_miss", false)));

        AgentRunResult result = agentRunLoop.run(resolvePlan(), session(), "why can't I see my advert", List.of());

        verify(toolDispatcher, times(1)).dispatch(eq("search_knowledge"), any(), any());
        assertEquals(TerminalOutcome.FINAL_ANSWER, result.terminalOutcome());
        boolean anyDeduped = result.toolEvents().stream().anyMatch(ToolEvent::deduplicated);
        assertFalse(anyDeduped, "a single dispatch must not be flagged deduplicated; got " + result.toolEvents());
    }

    // ── (b) external-failure same-args retry RE-dispatches ─────────────
    @Test
    void sameArgsRetryAfterExternalFailure_reDispatches() {
        ToolCall call0 = ToolCall.builder().name("search_knowledge").arguments(args()).build();
        ToolCall call1 = ToolCall.builder().name("search_knowledge").arguments(args()).build();

        LlmResponse rawA = LlmResponse.builder().content("{\"step\":0}").build();
        LlmResponse rawB = LlmResponse.builder().content("{\"step\":1}").build();
        LlmResponse rawC = LlmResponse.builder().content("{\"step\":2}").build();
        when(llmInvocation.invokeChat(anyString(), anyString(), anyString(), anyInt()))
                .thenReturn(rawA).thenReturn(rawB).thenReturn(rawC);
        when(actionParser.parse(rawA.getContent())).thenReturn(ParsedAction.builder()
                .toolCalls(List.of(call0)).userMessage("").reasoning("try once").build());
        when(actionParser.parse(rawB.getContent())).thenReturn(ParsedAction.builder()
                .toolCalls(List.of(call1)).userMessage("").reasoning("retry after failure").build());
        when(actionParser.parse(rawC.getContent())).thenReturn(ParsedAction.builder()
                .toolCalls(List.of()).userMessage("Here is what I found.").reasoning("done").build());

        when(toolDispatcher.validateAgainstPlan(any(), eq("search_knowledge")))
                .thenReturn(ToolResult.ok(null));
        // First dispatch fails (external failure, success=false → NOT cached);
        // the identical retry succeeds and must reach the dispatcher again.
        when(toolDispatcher.dispatch(eq("search_knowledge"), any(), any()))
                .thenReturn(ToolResult.error("backend timeout"))
                .thenReturn(ToolResult.ok(Map.of("faq_miss", false)));

        AgentRunResult result = agentRunLoop.run(resolvePlan(), session(), "why can't I see my advert", List.of());

        // The failed result was never cached, so the identical retry
        // re-dispatched: two real dispatches, zero dedup events.
        verify(toolDispatcher, times(2)).dispatch(eq("search_knowledge"), any(), any());
        boolean anyDeduped = result.toolEvents().stream().anyMatch(ToolEvent::deduplicated);
        assertFalse(anyDeduped,
                "a same-args retry after an external failure must re-dispatch, not dedup; got "
                        + result.toolEvents());
    }

    // ── (d) prompt teaches the slot as a BINDING signal (hybrid half) ──
    @Test
    void systemPrompt_teachesAlreadyCalledAsBindingSignal() throws IOException {
        String prompt = loadSystemPrompt().toLowerCase();
        assertTrue(prompt.contains("already_called"),
                "prompt must still name the already_called slot");
        assertTrue(prompt.contains("binding"),
                "S-Auto-12: prompt must teach the slot as a binding signal, not merely an observation");
        assertTrue(prompt.contains("deduplicat"),
                "S-Auto-12: prompt must tell the LLM the runtime auto-deduplicates byte-identical repeats");
        // Hybrid: still leaves the LLM ownership of which tool / what content.
        assertTrue(prompt.contains("you own"),
                "prompt must still leave the re-emit judgement to the LLM (soft-signal ownership)");
    }

    private static String loadSystemPrompt() throws IOException {
        try (var is = new ClassPathResource("prompts/system_prompt.txt").getInputStream()) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
