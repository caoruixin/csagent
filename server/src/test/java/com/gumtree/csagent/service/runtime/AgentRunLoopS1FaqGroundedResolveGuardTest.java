package com.gumtree.csagent.service.runtime;

import com.gumtree.csagent.model.AgentRunResult;
import com.gumtree.csagent.model.BotSession;
import com.gumtree.csagent.model.LlmResponse;
import com.gumtree.csagent.model.ParsedAction;
import com.gumtree.csagent.model.PhasePlan;
import com.gumtree.csagent.model.TerminalOutcome;
import com.gumtree.csagent.model.ToolCall;
import com.gumtree.csagent.service.tools.ToolDispatcher;
import com.gumtree.csagent.service.tools.ToolResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Sprint 6 §G2 — focused regression tests for the S1 FAQ-grounded-resolve
 * guard inside {@link AgentRunLoopImpl}.
 *
 * <p>The guard refuses {@code request_handover(faq_miss_threshold_exceeded)}
 * when the FAQ-path RESOLVE plan has:
 *
 * <ul>
 *   <li>{@code search_knowledge} present in {@code accumulated_tool_results}
 *       with {@code faq_miss=false} and at least one hit, AND</li>
 *   <li>{@code resolve_article} not yet present in
 *       {@code accumulated_tool_results}.</li>
 * </ul>
 *
 * <p>The two failure shapes anchored by this test class are:
 *
 * <ol>
 *   <li>cs_192 shape — search-not-yet-run / uncited-answer. The guard does
 *       not fire here (search hasn't run); the prompt
 *       (PhaseEvaluator FAQ-path systemInstruction / groundingInstruction)
 *       owns the search-before-answer nudge. We assert the negative case:
 *       a faq_miss handover BEFORE search runs is allowed through.</li>
 *   <li>cs_259 shape — search ran but resolve did not complete. The guard
 *       fires here and rejects the handover so the LLM is forced to run
 *       resolve_article on the next iteration.</li>
 * </ol>
 */
@ExtendWith(MockitoExtension.class)
class AgentRunLoopS1FaqGroundedResolveGuardTest {

    @Mock private LlmInvocationService llmInvocation;
    @Mock private ToolDispatcher toolDispatcher;
    @Mock private ContextProjectionBuilder contextProjectionBuilder;
    @Mock private ActionParser actionParser;

    private AgentRunLoopImpl loop;

    @BeforeEach
    void setUp() {
        loop = new AgentRunLoopImpl(llmInvocation, toolDispatcher,
                contextProjectionBuilder, actionParser, new com.fasterxml.jackson.databind.ObjectMapper());
    }

    private PhasePlan faqPlan(String uc) {
        return PhasePlan.builder()
                .phase("RESOLVE")
                .useCase(uc)
                .objective("test")
                .allowedTools(List.of(
                        "get_customer_context", "search_knowledge",
                        "resolve_article", "record_outcome", "request_handover"))
                .maxToolSteps(4)
                .validTerminalOutcomes(Set.of(
                        TerminalOutcome.FINAL_ANSWER,
                        TerminalOutcome.ESCALATE,
                        TerminalOutcome.MAX_STEPS))
                .build();
    }

    private BotSession session(String uc) {
        BotSession s = new BotSession();
        s.setSessionId("sess-1");
        s.setCurrentPhase("RESOLVE");
        s.setActiveUseCase(uc);
        s.setTotalBotTurns(0);
        return s;
    }

    private static Map<String, Object> viableSearchHits() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("faq_miss", false);
        data.put("retrieval_miss", false);
        data.put("answer_miss", false);
        data.put("hits", List.of(Map.of(
                "source_id", "kb-001",
                "title", "How to receive payment",
                "snippet", "...",
                "score", 0.95)));
        return data;
    }

    private static Map<String, Object> emptySearch() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("faq_miss", true);
        data.put("hits", List.of());
        return data;
    }

    // ---------- Predicate unit tests (no mocks) ----------

    @Test
    void predicate_fires_for_cs259_shape_search_ran_resolve_missing() {
        PhasePlan plan = faqPlan("UC-F");
        ToolCall handover = ToolCall.builder()
                .name("request_handover")
                .arguments(Map.of("escalation_reason", "faq_miss_threshold_exceeded"))
                .build();
        Map<String, Object> acc = new LinkedHashMap<>();
        acc.put("search_knowledge", viableSearchHits());

        assertTrue(AgentRunLoopImpl.shouldRejectFaqMissHandover(plan, handover, acc),
                "cs_259 shape (search hits, no resolve_article yet) must trigger the S1 guard");
    }

    @Test
    void predicate_does_not_fire_when_resolve_article_already_attempted() {
        PhasePlan plan = faqPlan("UC-F");
        ToolCall handover = ToolCall.builder()
                .name("request_handover")
                .arguments(Map.of("escalation_reason", "faq_miss_threshold_exceeded"))
                .build();
        Map<String, Object> acc = new LinkedHashMap<>();
        acc.put("search_knowledge", viableSearchHits());
        acc.put("resolve_article", Map.of("article", "x", "source_id", "kb-001"));

        assertFalse(AgentRunLoopImpl.shouldRejectFaqMissHandover(plan, handover, acc),
                "Once resolve_article has been attempted, faq_miss handover is allowed");
    }

    @Test
    void predicate_does_not_fire_for_cs192_shape_search_not_yet_run() {
        PhasePlan plan = faqPlan("UC-B");
        ToolCall handover = ToolCall.builder()
                .name("request_handover")
                .arguments(Map.of("escalation_reason", "faq_miss_threshold_exceeded"))
                .build();
        Map<String, Object> acc = new LinkedHashMap<>();
        // No search_knowledge in accumulated_tool_results.

        assertFalse(AgentRunLoopImpl.shouldRejectFaqMissHandover(plan, handover, acc),
                "cs_192 shape (search not yet run) is owned by the prompt nudge; "
                        + "the S1 guard must not fire and pre-empt a legitimate "
                        + "no-search escalation path");
    }

    @Test
    void predicate_does_not_fire_when_search_returned_no_hits() {
        PhasePlan plan = faqPlan("UC-B");
        ToolCall handover = ToolCall.builder()
                .name("request_handover")
                .arguments(Map.of("escalation_reason", "faq_miss_threshold_exceeded"))
                .build();
        Map<String, Object> acc = new LinkedHashMap<>();
        acc.put("search_knowledge", emptySearch());

        assertFalse(AgentRunLoopImpl.shouldRejectFaqMissHandover(plan, handover, acc),
                "When search_knowledge returns no viable hit, faq_miss_threshold_exceeded is allowed");
    }

    @Test
    void predicate_does_not_fire_for_user_requested_handover() {
        PhasePlan plan = faqPlan("UC-F");
        ToolCall handover = ToolCall.builder()
                .name("request_handover")
                .arguments(Map.of("escalation_reason", "user_requested"))
                .build();
        Map<String, Object> acc = new LinkedHashMap<>();
        acc.put("search_knowledge", viableSearchHits());

        assertFalse(AgentRunLoopImpl.shouldRejectFaqMissHandover(plan, handover, acc),
                "user_requested handover must always pass through — Sprint 6 §G1 priority 1");
    }

    @Test
    void predicate_does_not_fire_for_user_distress_handover() {
        PhasePlan plan = faqPlan("UC-F");
        ToolCall handover = ToolCall.builder()
                .name("request_handover")
                .arguments(Map.of("escalation_reason", "user_distress"))
                .build();
        Map<String, Object> acc = new LinkedHashMap<>();
        acc.put("search_knowledge", viableSearchHits());

        assertFalse(AgentRunLoopImpl.shouldRejectFaqMissHandover(plan, handover, acc),
                "user_distress handover must always pass through");
    }

    @Test
    void predicate_does_not_fire_for_intake_uc() {
        PhasePlan plan = PhasePlan.builder()
                .phase("RESOLVE")
                .useCase("UC-K")
                .objective("test")
                .allowedTools(List.of("request_handover"))
                .maxToolSteps(3)
                .validTerminalOutcomes(Set.of(TerminalOutcome.ESCALATE))
                .build();
        ToolCall handover = ToolCall.builder()
                .name("request_handover")
                .arguments(Map.of("escalation_reason", "faq_miss_threshold_exceeded"))
                .build();
        Map<String, Object> acc = new LinkedHashMap<>();
        acc.put("search_knowledge", viableSearchHits());

        assertFalse(AgentRunLoopImpl.shouldRejectFaqMissHandover(plan, handover, acc),
                "INTAKE-path UCs (UC-G/H/I/J/K) must not be affected by the S1 guard");
    }

    @Test
    void predicate_does_not_fire_outside_resolve_phase() {
        PhasePlan discoverPlan = PhasePlan.builder()
                .phase("DISCOVER")
                .useCase(null)
                .objective("test")
                .allowedTools(List.of("classify_use_case"))
                .maxToolSteps(2)
                .build();
        ToolCall handover = ToolCall.builder()
                .name("request_handover")
                .arguments(Map.of("escalation_reason", "faq_miss_threshold_exceeded"))
                .build();
        Map<String, Object> acc = new LinkedHashMap<>();
        acc.put("search_knowledge", viableSearchHits());

        assertFalse(AgentRunLoopImpl.shouldRejectFaqMissHandover(
                discoverPlan, handover, acc));
    }

    // ---------- Integration tests (full loop) ----------

    @Test
    void cs259_shape_loop_rejects_faq_miss_handover_and_continues() {
        // Simulate a 3-step loop:
        //   step 0: LLM calls search_knowledge (returns viable hit)
        //   step 1: LLM calls request_handover(faq_miss_threshold_exceeded)
        //           — guard must reject; loop continues
        //   step 2: LLM calls resolve_article + grounded answer; loop ends
        //           via FINAL_ANSWER (or via handover with a different reason).
        when(contextProjectionBuilder.build(any(), any(), any(PhasePlan.class), anyString(), any()))
                .thenReturn("{}");

        when(llmInvocation.invokeChat(anyString(), anyString(), anyString(), anyInt()))
                .thenReturn(LlmResponse.builder().content("step0").build())
                .thenReturn(LlmResponse.builder().content("step1").build())
                .thenReturn(LlmResponse.builder().content("step2").build());

        when(actionParser.parse("step0")).thenReturn(ParsedAction.builder()
                .toolCalls(List.of(ToolCall.builder()
                        .name("search_knowledge")
                        .arguments(Map.of("query", "How do I receive payment"))
                        .build()))
                .userMessage("")
                .build());
        when(actionParser.parse("step1")).thenReturn(ParsedAction.builder()
                .toolCalls(List.of(ToolCall.builder()
                        .name("request_handover")
                        .arguments(Map.of(
                                "escalation_reason", "faq_miss_threshold_exceeded",
                                "summary", "could not resolve"))
                        .build()))
                .userMessage("")
                .build());
        when(actionParser.parse("step2")).thenReturn(ParsedAction.builder()
                .toolCalls(List.of())
                .userMessage("Here's how Gumtree payments work… [kb-001]")
                .build());

        when(toolDispatcher.validateAgainstPlan(any(PhasePlan.class), eq("search_knowledge")))
                .thenReturn(ToolResult.ok(null));
        when(toolDispatcher.validateAgainstPlan(any(PhasePlan.class), eq("request_handover")))
                .thenReturn(ToolResult.ok(null));
        when(toolDispatcher.dispatch(eq("search_knowledge"), any(), any()))
                .thenReturn(ToolResult.ok(viableSearchHits()));

        AgentRunResult result = loop.run(faqPlan("UC-F"), session("UC-F"),
                "How do I receive payment", List.of());

        // The handover should have been REJECTED (S1 guard) so the loop did
        // not short-circuit on step 1. Step 2 then produced a final answer.
        assertEquals(TerminalOutcome.FINAL_ANSWER, result.terminalOutcome());
        assertNotNull(result.finalUserMessage());
        assertTrue(result.finalUserMessage().contains("[kb-001]"));

        // Tool events: 1 successful search_knowledge + 1 rejected request_handover.
        assertEquals(2, result.toolEvents().size());
        assertEquals("search_knowledge", result.toolEvents().get(0).toolName());
        assertTrue(result.toolEvents().get(0).success());
        assertEquals("request_handover", result.toolEvents().get(1).toolName());
        assertFalse(result.toolEvents().get(1).success(),
                "S1 guard must reject the faq_miss handover");
        assertTrue(
                result.toolEvents().get(1).errorMessage()
                        .contains("s1_resolve_required_before_faq_miss_handover"),
                "rejection must carry the S1 guard reason tag");
    }

    @Test
    void cs192_shape_loop_allows_faq_miss_handover_when_search_returned_no_hit() {
        // search_knowledge runs but returns faq_miss=true; the LLM then
        // escalates with faq_miss_threshold_exceeded — the guard must NOT
        // fire because there is no viable evidence to ground.
        when(contextProjectionBuilder.build(any(), any(), any(PhasePlan.class), anyString(), any()))
                .thenReturn("{}");

        when(llmInvocation.invokeChat(anyString(), anyString(), anyString(), anyInt()))
                .thenReturn(LlmResponse.builder().content("step0").build())
                .thenReturn(LlmResponse.builder().content("step1").build());

        when(actionParser.parse("step0")).thenReturn(ParsedAction.builder()
                .toolCalls(List.of(ToolCall.builder()
                        .name("search_knowledge")
                        .arguments(Map.of("query", "what items are allowed"))
                        .build()))
                .userMessage("")
                .build());
        when(actionParser.parse("step1")).thenReturn(ParsedAction.builder()
                .toolCalls(List.of(ToolCall.builder()
                        .name("request_handover")
                        .arguments(Map.of(
                                "escalation_reason", "faq_miss_threshold_exceeded",
                                "summary", "no FAQ match"))
                        .build()))
                .userMessage("Let me hand you to a specialist.")
                .build());

        when(toolDispatcher.validateAgainstPlan(any(PhasePlan.class), eq("search_knowledge")))
                .thenReturn(ToolResult.ok(null));
        when(toolDispatcher.validateAgainstPlan(any(PhasePlan.class), eq("request_handover")))
                .thenReturn(ToolResult.ok(null));
        when(toolDispatcher.dispatch(eq("search_knowledge"), any(), any()))
                .thenReturn(ToolResult.ok(emptySearch()));
        when(toolDispatcher.dispatch(eq("request_handover"), any(), any()))
                .thenReturn(ToolResult.ok(Map.of("transfer_result", "queued")));

        AgentRunResult result = loop.run(faqPlan("UC-B"), session("UC-B"),
                "what items are allowed", List.of());

        assertEquals(TerminalOutcome.ESCALATE, result.terminalOutcome());
        assertEquals("faq_miss_threshold_exceeded", result.escalationReason().orElse(null));
        // 2 successful tool events (search + handover)
        assertEquals(2, result.toolEvents().size());
        assertTrue(result.toolEvents().get(1).success(),
                "handover must succeed when search returned no viable hit");
    }

    @Test
    void cs192_shape_no_prior_search_loop_allows_faq_miss_handover() {
        // Defensive: even when search_knowledge has not run at all (cs_192
        // turn-0 shape), the guard does not pre-empt the handover. The
        // prompt-side nudge in PhaseEvaluator's groundingInstruction owns
        // the search-before-answer behaviour.
        when(contextProjectionBuilder.build(any(), any(), any(PhasePlan.class), anyString(), any()))
                .thenReturn("{}");

        when(llmInvocation.invokeChat(anyString(), anyString(), anyString(), anyInt()))
                .thenReturn(LlmResponse.builder().content("step0").build());

        when(actionParser.parse("step0")).thenReturn(ParsedAction.builder()
                .toolCalls(List.of(ToolCall.builder()
                        .name("request_handover")
                        .arguments(Map.of(
                                "escalation_reason", "faq_miss_threshold_exceeded",
                                "summary", "no clear answer"))
                        .build()))
                .userMessage("Let me hand you to a specialist.")
                .build());

        when(toolDispatcher.validateAgainstPlan(any(PhasePlan.class), eq("request_handover")))
                .thenReturn(ToolResult.ok(null));
        when(toolDispatcher.dispatch(eq("request_handover"), any(), any()))
                .thenReturn(ToolResult.ok(Map.of("transfer_result", "queued")));

        AgentRunResult result = loop.run(faqPlan("UC-B"), session("UC-B"),
                "anything",
                List.of());

        // Handover passes through (guard does not own the cs_192 shape).
        assertEquals(TerminalOutcome.ESCALATE, result.terminalOutcome());
        assertEquals("faq_miss_threshold_exceeded", result.escalationReason().orElse(null));
    }

    @Test
    void user_requested_handover_passes_through_even_with_search_hits() {
        // Cross-check with G1: user_requested wins over the FAQ family;
        // the S1 guard never blocks a user_requested handover, even with
        // viable search hits in accumulated_tool_results.
        when(contextProjectionBuilder.build(any(), any(), any(PhasePlan.class), anyString(), any()))
                .thenReturn("{}");

        when(llmInvocation.invokeChat(anyString(), anyString(), anyString(), anyInt()))
                .thenReturn(LlmResponse.builder().content("step0").build())
                .thenReturn(LlmResponse.builder().content("step1").build());

        when(actionParser.parse("step0")).thenReturn(ParsedAction.builder()
                .toolCalls(List.of(ToolCall.builder()
                        .name("search_knowledge")
                        .arguments(Map.of("query", "how to receive payment"))
                        .build()))
                .userMessage("")
                .build());
        when(actionParser.parse("step1")).thenReturn(ParsedAction.builder()
                .toolCalls(List.of(ToolCall.builder()
                        .name("request_handover")
                        .arguments(Map.of(
                                "escalation_reason", "user_requested",
                                "summary", "user explicitly asked for human help"))
                        .build()))
                .userMessage("Connecting you with a specialist now.")
                .build());

        when(toolDispatcher.validateAgainstPlan(any(PhasePlan.class), eq("search_knowledge")))
                .thenReturn(ToolResult.ok(null));
        when(toolDispatcher.validateAgainstPlan(any(PhasePlan.class), eq("request_handover")))
                .thenReturn(ToolResult.ok(null));
        when(toolDispatcher.dispatch(eq("search_knowledge"), any(), any()))
                .thenReturn(ToolResult.ok(viableSearchHits()));
        when(toolDispatcher.dispatch(eq("request_handover"), any(), any()))
                .thenReturn(ToolResult.ok(Map.of("transfer_result", "queued")));

        AgentRunResult result = loop.run(faqPlan("UC-F"), session("UC-F"),
                "How do I receive payment", List.of());

        assertEquals(TerminalOutcome.ESCALATE, result.terminalOutcome());
        assertEquals("user_requested", result.escalationReason().orElse(null));
    }
}
