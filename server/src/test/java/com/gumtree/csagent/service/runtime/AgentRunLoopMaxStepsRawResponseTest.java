package com.gumtree.csagent.service.runtime;

import com.fasterxml.jackson.databind.ObjectMapper;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Sprint 8.2 §M0b — AgentRunLoop must preserve {@code lastLlmRawResponse} on
 * the {@link TerminalOutcome#MAX_STEPS} return path so the trace UI does not
 * misleadingly show "no LLM call for this turn" when several real LLM calls
 * occurred before the loop exhausted its tool-step budget.
 *
 * <p>Reproduction of the trace {@code 6f24c6ab} shape: the LLM repeatedly
 * calls a tool that errors (here mocked as {@code resolve_article} returning
 * a parameter error); after {@code maxToolSteps} the loop returns MAX_STEPS.
 * The terminating raw LLM response must be carried into the result.
 *
 * <p>The terminal outcome itself remains MAX_STEPS — this is an
 * observability honesty fix, not a behaviour change.
 */
@ExtendWith(MockitoExtension.class)
class AgentRunLoopMaxStepsRawResponseTest {

    @Mock private LlmInvocationService llmInvocation;
    @Mock private ToolDispatcher toolDispatcher;
    @Mock private ContextProjectionBuilder contextProjectionBuilder;
    @Mock private ActionParser actionParser;

    private AgentRunLoopImpl loop;

    @BeforeEach
    void setUp() {
        loop = new AgentRunLoopImpl(llmInvocation, toolDispatcher,
                contextProjectionBuilder, actionParser, new ObjectMapper());
    }

    @Test
    void maxSteps_preserves_last_llm_raw_response_after_repeated_tool_errors() {
        // 2-step loop, both steps:
        //   * LLM emits the same resolve_article(source_id=...) call.
        //   * tool dispatcher returns an error (repro of trace 6f24c6ab
        //     before §M0a alignment, but we mock it here so this test is
        //     not coupled to the real ResolveArticleTool).
        //   * loop runs out of steps and returns MAX_STEPS.
        PhasePlan plan = PhasePlan.builder()
                .phase("RESOLVE")
                .useCase("UC-A")
                .objective("test")
                .allowedTools(List.of("search_knowledge", "resolve_article", "request_handover"))
                .maxToolSteps(2)
                .validTerminalOutcomes(Set.of(
                        TerminalOutcome.FINAL_ANSWER,
                        TerminalOutcome.ESCALATE,
                        TerminalOutcome.MAX_STEPS))
                .build();

        BotSession session = new BotSession();
        session.setSessionId("sess-maxsteps-raw-response");
        session.setCurrentPhase("RESOLVE");
        session.setActiveUseCase("UC-A");
        session.setTotalBotTurns(0);

        when(contextProjectionBuilder.build(any(), any(), any(PhasePlan.class), anyString(), any(), any()))
                .thenReturn("{}");

        String firstRaw = "{\"tool_calls\":[{\"name\":\"resolve_article\",\"arguments\":{\"source_id\":\"kb-001\"}}]}";
        String terminatingRaw = "{\"tool_calls\":[{\"name\":\"resolve_article\",\"arguments\":{\"source_id\":\"kb-001\"}}],"
                + "\"user_message\":\"Let me try once more.\"}";

        when(llmInvocation.invokeChat(anyString(), anyString(), anyString(), anyInt()))
                .thenReturn(LlmResponse.builder().content(firstRaw).build())
                .thenReturn(LlmResponse.builder().content(terminatingRaw).build());

        ParsedAction resolveCall = ParsedAction.builder()
                .toolCalls(List.of(ToolCall.builder()
                        .name("resolve_article")
                        .arguments(Map.of("source_id", "kb-001"))
                        .build()))
                .userMessage("")
                .build();
        when(actionParser.parse(firstRaw)).thenReturn(resolveCall);
        when(actionParser.parse(terminatingRaw)).thenReturn(resolveCall);

        // Plan-validation passes for resolve_article; the dispatched tool
        // returns an error (mimicking the pre-§M0a parameter mismatch). The
        // loop continues so it can hit MAX_STEPS.
        when(toolDispatcher.validateAgainstPlan(eq(plan), eq("resolve_article")))
                .thenReturn(ToolResult.ok(Map.of()));
        when(toolDispatcher.dispatch(eq("resolve_article"), eq(session), any()))
                .thenReturn(ToolResult.error("Article not found: kb-001"));

        AgentRunResult result = loop.run(plan, session, "where is my advert?", List.of());

        assertEquals(TerminalOutcome.MAX_STEPS, result.terminalOutcome(),
                "Sprint 8.2 §M0b is observability-only: terminalOutcome must remain MAX_STEPS");
        assertEquals(2, result.llmEvents().size(),
                "Both LLM calls should have been recorded as events before the loop exhausted");
        assertNotNull(result.lastLlmRawResponse(),
                "Sprint 8.2 §M0b: the verbatim content of the terminating LLM call must survive on MAX_STEPS");
        assertEquals(terminatingRaw, result.lastLlmRawResponse(),
                "MAX_STEPS must carry through the LAST raw LLM response, not an earlier one");
    }

    /**
     * Sprint 8.2 §M0a integration shape — UC-A FAQ flow:
     * {@code search_knowledge -> resolve_article(source_id=...)} runs without
     * a {@code Parameter 'article_id' is required} error, and the loop
     * terminates with a grounded final answer.
     *
     * <p>This is the post-fix expected shape of trace {@code 6f24c6ab}.
     */
    @Test
    void ucA_faq_flow_search_then_resolve_article_with_source_id_completes() {
        PhasePlan plan = PhasePlan.builder()
                .phase("RESOLVE")
                .useCase("UC-A")
                .objective("test")
                .allowedTools(List.of("search_knowledge", "resolve_article", "request_handover"))
                .maxToolSteps(4)
                .validTerminalOutcomes(Set.of(
                        TerminalOutcome.FINAL_ANSWER,
                        TerminalOutcome.ESCALATE,
                        TerminalOutcome.MAX_STEPS))
                .build();

        BotSession session = new BotSession();
        session.setSessionId("sess-uca-faq-flow");
        session.setCurrentPhase("RESOLVE");
        session.setActiveUseCase("UC-A");
        session.setTotalBotTurns(0);

        when(contextProjectionBuilder.build(any(), any(), any(PhasePlan.class), anyString(), any(), any()))
                .thenReturn("{}");

        when(llmInvocation.invokeChat(anyString(), anyString(), anyString(), anyInt()))
                .thenReturn(LlmResponse.builder().content("step0-search").build())
                .thenReturn(LlmResponse.builder().content("step1-resolve").build())
                .thenReturn(LlmResponse.builder().content("step2-answer").build());

        when(actionParser.parse("step0-search")).thenReturn(ParsedAction.builder()
                .toolCalls(List.of(ToolCall.builder()
                        .name("search_knowledge")
                        .arguments(Map.of("query", "where is my advert"))
                        .build()))
                .userMessage("")
                .build());
        when(actionParser.parse("step1-resolve")).thenReturn(ParsedAction.builder()
                .toolCalls(List.of(ToolCall.builder()
                        .name("resolve_article")
                        // The LLM passes the canonical source_id straight from
                        // search_knowledge.hits[0].source_id — Sprint 8.2 §M0a
                        // contract.
                        .arguments(Map.of("source_id", "kb-001"))
                        .build()))
                .userMessage("")
                .build());
        when(actionParser.parse("step2-answer")).thenReturn(ParsedAction.builder()
                .toolCalls(List.of())
                .userMessage("Your ad may be hidden during moderation review. [kb-001]")
                .build());

        when(toolDispatcher.validateAgainstPlan(eq(plan), eq("search_knowledge")))
                .thenReturn(ToolResult.ok(null));
        when(toolDispatcher.validateAgainstPlan(eq(plan), eq("resolve_article")))
                .thenReturn(ToolResult.ok(null));

        Map<String, Object> hits = new LinkedHashMap<>();
        hits.put("faq_miss", false);
        hits.put("retrieval_miss", false);
        hits.put("answer_miss", false);
        hits.put("hits", List.of(Map.of(
                "source_id", "kb-001",
                "title", "Where is my advert?",
                "snippet", "...",
                "score", 0.91)));
        when(toolDispatcher.dispatch(eq("search_knowledge"), eq(session), any()))
                .thenReturn(ToolResult.ok(hits));

        // resolve_article succeeds with source_id — the post-§M0a contract.
        Map<String, Object> articlePayload = new LinkedHashMap<>();
        articlePayload.put("source_id", "kb-001");
        articlePayload.put("article_id", "kb-001");
        articlePayload.put("title", "Where is my advert?");
        articlePayload.put("description", "Ads can be hidden during moderation review.");
        when(toolDispatcher.dispatch(eq("resolve_article"), eq(session), any()))
                .thenReturn(ToolResult.ok(articlePayload));

        AgentRunResult result = loop.run(plan, session,
                "hi why can't I find my advert", List.of());

        assertEquals(TerminalOutcome.FINAL_ANSWER, result.terminalOutcome(),
                "post-§M0a UC-A FAQ flow must reach FINAL_ANSWER, not MAX_STEPS / ESCALATE");
        assertNotNull(result.finalUserMessage());
        assertTrue(result.finalUserMessage().contains("[kb-001]"),
                "grounded answer must cite the resolved source_id");
        assertEquals(2, result.toolEvents().size(),
                "expected one search_knowledge + one resolve_article event with no parameter errors");
        assertEquals("search_knowledge", result.toolEvents().get(0).toolName());
        assertTrue(result.toolEvents().get(0).success());
        assertEquals("resolve_article", result.toolEvents().get(1).toolName());
        assertTrue(result.toolEvents().get(1).success(),
                "resolve_article must succeed when called with source_id (no 'Parameter article_id is required' error)");
    }
}
