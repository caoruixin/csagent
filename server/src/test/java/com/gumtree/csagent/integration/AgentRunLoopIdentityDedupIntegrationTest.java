package com.gumtree.csagent.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gumtree.csagent.model.AgentRunResult;
import com.gumtree.csagent.model.BotSession;
import com.gumtree.csagent.model.LlmResponse;
import com.gumtree.csagent.model.ParsedAction;
import com.gumtree.csagent.model.PhasePlan;
import com.gumtree.csagent.model.TerminalOutcome;
import com.gumtree.csagent.model.ToolCall;
import com.gumtree.csagent.model.ToolEvent;
import com.gumtree.csagent.service.runtime.ActionParser;
import com.gumtree.csagent.service.runtime.AgentRunLoop;
import com.gumtree.csagent.service.runtime.AgentRunLoopImpl;
import com.gumtree.csagent.service.runtime.ContextProjectionBuilder;
import com.gumtree.csagent.service.runtime.ControlPolicyService;
import com.gumtree.csagent.service.runtime.LlmInvocationService;
import com.gumtree.csagent.service.runtime.UseCaseRegistryService;
import com.gumtree.csagent.service.tools.ToolDispatcher;
import com.gumtree.csagent.service.tools.ToolPolicyEnforcer;
import com.gumtree.csagent.service.tools.ToolResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Sprint 067 / S-Auto-12 (M-Auto-3) — end-to-end coverage of the A1
 * identity-dedup 回挡 (deterministic backstop half of the hybrid).
 *
 * <p>This test supersedes the Sprint 20 {@code
 * AgentRunLoopAlreadyCalledNonEnforcementIntegrationTest}, which asserted
 * the OLD observation-only contract (both byte-identical calls reach
 * {@link ToolDispatcher#dispatch}). S-Auto-12 deliberately reverses that
 * contract: a byte-identical {@code success==true} repeat is now served
 * from a per-run identity cache without re-dispatching, while the
 * {@code already_called} projection slot continues to render for shape
 * stability and the LLM still owns which tool to call / what to send.
 *
 * <p>Drives {@link AgentRunLoopImpl#run} with two identical-args
 * {@code search_knowledge} calls followed by a final answer and asserts:
 *
 * <ol>
 *   <li>{@link ToolDispatcher#dispatch} is invoked exactly ONCE — the
 *       second byte-identical call is deduped (the deterministic
 *       backstop).</li>
 *   <li>The second {@link ToolEvent} is annotated {@code deduplicated=true}
 *       with {@code originalAtStep=0} and reuses the cached payload at zero
 *       latency.</li>
 *   <li>The step-1 projection still carries the {@code already_called} slot
 *       referencing the step-0 dispatch (projection shape preserved).</li>
 *   <li>The loop reaches the no-tool-call final answer.</li>
 * </ol>
 *
 * <p>Wiring: real {@link AgentRunLoopImpl} + real
 * {@link ContextProjectionBuilder}; mocks for {@link LlmInvocationService},
 * {@link ToolDispatcher}, {@link ActionParser}, and the projection
 * builder's collaborator services so the loop iterates deterministically
 * over three LLM responses (tool call, identical tool call, final answer).
 */
@ExtendWith(MockitoExtension.class)
class AgentRunLoopIdentityDedupIntegrationTest {

    @Mock private LlmInvocationService llmInvocation;
    @Mock private ToolDispatcher toolDispatcher;
    @Mock private ActionParser actionParser;
    @Mock private UseCaseRegistryService useCaseRegistry;
    @Mock private ControlPolicyService controlPolicy;
    @Mock private ToolPolicyEnforcer toolPolicyEnforcer;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private ContextProjectionBuilder projectionBuilder;
    private AgentRunLoop agentRunLoop;

    @BeforeEach
    void setUp() {
        projectionBuilder = new ContextProjectionBuilder(
                objectMapper, useCaseRegistry, controlPolicy, toolPolicyEnforcer, null);
        // initToolSchemas() is package-private (Spring @PostConstruct); not
        // calling it here leaves tool_schemas empty, which only affects the
        // tool_schemas slot — the already_called slot under test renders
        // independently.

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

    @Test
    void identicalArgsReEmission_secondCallDeduped_andSecondProjectionShowsSlot() throws Exception {
        BotSession session = BotSession.builder()
                .sessionId("sauto12-identity-dedup")
                .activeUseCase("UC-A")
                .currentPhase("RESOLVE")
                .handlingState("BOT_HANDLING")
                .totalBotTurns(0)
                .clarificationCount(0)
                .faqMissCount(0)
                .repeatedActionCount(0)
                .build();

        PhasePlan plan = PhasePlan.builder()
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

        // Two LLM responses each ask for search_knowledge with identical
        // arguments; the third returns a final user-facing answer so the
        // loop terminates cleanly. The LLM payload shape is canned and
        // parsed via a mocked ActionParser, so the response strings only
        // need to be non-null.
        String userQuery = "why can't I see my advert";
        Map<String, Object> identicalArgs = new LinkedHashMap<>();
        identicalArgs.put("query", userQuery);
        identicalArgs.put("uc_tags", List.of("UC-A"));

        ToolCall identicalCallStep0 = ToolCall.builder()
                .name("search_knowledge").arguments(identicalArgs).build();
        ToolCall identicalCallStep1 = ToolCall.builder()
                .name("search_knowledge").arguments(identicalArgs).build();

        LlmResponse rawA = LlmResponse.builder().content("{\"step\":0}").promptTokens(10).completionTokens(5).build();
        LlmResponse rawB = LlmResponse.builder().content("{\"step\":1}").promptTokens(10).completionTokens(5).build();
        LlmResponse rawC = LlmResponse.builder().content("{\"step\":2}").promptTokens(10).completionTokens(5).build();

        when(llmInvocation.invokeChat(anyString(), anyString(), anyString(), anyInt()))
                .thenReturn(rawA).thenReturn(rawB).thenReturn(rawC);

        when(actionParser.parse(rawA.getContent())).thenReturn(ParsedAction.builder()
                .toolCalls(List.of(identicalCallStep0))
                .userMessage("").reasoning("look up the FAQ").build());
        when(actionParser.parse(rawB.getContent())).thenReturn(ParsedAction.builder()
                .toolCalls(List.of(identicalCallStep1))
                .userMessage("").reasoning("re-emit same query").build());
        when(actionParser.parse(rawC.getContent())).thenReturn(ParsedAction.builder()
                .toolCalls(List.of())
                .userMessage("Here is what I found about your advert visibility.")
                .reasoning("final answer").build());

        Map<String, Object> dispatchData = Map.of(
                "faq_miss", false,
                "hits", List.of(Map.of("source_id", "ka4P2-faq", "title", "Ad visibility")));
        when(toolDispatcher.validateAgainstPlan(any(), eq("search_knowledge")))
                .thenReturn(ToolResult.ok(null));
        when(toolDispatcher.dispatch(eq("search_knowledge"), any(), any()))
                .thenReturn(ToolResult.ok(dispatchData));

        // ── Act ─────────────────────────────────────────────────────
        AgentRunResult result = agentRunLoop.run(plan, session, userQuery, List.of());

        // ── Assert (a): only ONE real dispatch — second identical call is
        //    served from the per-run identity cache (A1 backstop).
        verify(toolDispatcher, times(1))
                .dispatch(eq("search_knowledge"), any(), any());

        assertNotNull(result);
        assertEquals(TerminalOutcome.FINAL_ANSWER, result.terminalOutcome(),
                "loop should reach the no-tool-call final answer after the dedup");

        // ── Assert (b): the second ToolEvent is a dedup annotation that
        //    reuses the cached payload at zero latency.
        List<ToolEvent> events = result.toolEvents();
        assertNotNull(events);
        long searchEvents = events.stream()
                .filter(e -> "search_knowledge".equals(e.toolName())).count();
        assertEquals(2, searchEvents,
                "both LLM-emitted search_knowledge calls must produce a ToolEvent "
                        + "(one real dispatch + one dedup annotation); got " + events);
        ToolEvent realEvent = events.stream()
                .filter(e -> "search_knowledge".equals(e.toolName()) && !e.deduplicated())
                .findFirst().orElseThrow();
        ToolEvent dedupEvent = events.stream()
                .filter(e -> "search_knowledge".equals(e.toolName()) && e.deduplicated())
                .findFirst().orElseThrow();
        assertEquals(0, realEvent.stepIndex(), "the real dispatch landed at step 0");
        assertTrue(realEvent.success(), "the real dispatch succeeded");
        assertEquals(1, dedupEvent.stepIndex(), "the dedup hit was at step 1");
        assertEquals(0, dedupEvent.originalAtStep(),
                "dedup event must reference the step-0 original dispatch");
        assertTrue(dedupEvent.success(),
                "a deduped event mirrors the cached success result");
        assertEquals(0L, dedupEvent.latencyMs(),
                "a deduped event records zero latency (tool not re-executed)");
        assertEquals(dispatchData, dedupEvent.resultData(),
                "the deduped event must carry the cached payload from the original dispatch");

        // ── Assert (c): step-1 projection still carries the already_called
        //    slot referencing the step-0 dispatch (projection shape preserved
        //    even though dispatch is now deduped).
        ArgumentCaptor<String> projectionCaptor = ArgumentCaptor.forClass(String.class);
        verify(llmInvocation, times(3))
                .invokeChat(projectionCaptor.capture(), anyString(), anyString(), anyInt());

        List<String> projectionsByStep = projectionCaptor.getAllValues();
        assertEquals(3, projectionsByStep.size(),
                "expected three projections (one per LLM invocation); got " + projectionsByStep.size());

        JsonNode step0Slot = objectMapper.readTree(projectionsByStep.get(0)).get("already_called");
        assertNotNull(step0Slot, "step-0 projection must always emit the already_called slot");
        assertTrue(step0Slot.isArray() && step0Slot.size() == 0,
                "step-0 already_called must be empty (no prior dispatch yet)");

        JsonNode step1Slot = objectMapper.readTree(projectionsByStep.get(1)).get("already_called");
        assertNotNull(step1Slot, "step-1 projection must carry the already_called slot");
        assertTrue(step1Slot.size() >= 1,
                "step-1 already_called must list the step-0 dispatch; got " + step1Slot);
        JsonNode firstEntry = step1Slot.get(0);
        assertEquals("search_knowledge", firstEntry.get("tool").asText(),
                "first slot entry tool must match the step-0 LLM-emitted tool name");
        assertEquals(0, firstEntry.get("at_step").asInt(),
                "first slot entry at_step must reference step 0 of the current run");
    }
}
