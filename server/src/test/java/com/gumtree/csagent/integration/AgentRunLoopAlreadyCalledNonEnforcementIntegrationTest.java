package com.gumtree.csagent.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.gumtree.csagent.model.AgentRunResult;
import com.gumtree.csagent.model.BotSession;
import com.gumtree.csagent.model.LlmResponse;
import com.gumtree.csagent.model.ParsedAction;
import com.gumtree.csagent.model.PhasePlan;
import com.gumtree.csagent.model.TerminalOutcome;
import com.gumtree.csagent.model.ToolCall;
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

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
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
 * Sprint 20 fix iteration — Codex Finding 1, Track B runtime-level
 * non-enforcement of the {@code already_called} projection slot.
 *
 * <p>Drives {@link AgentRunLoopImpl#run} end-to-end with two identical
 * LLM-emitted {@code search_knowledge} tool calls (same name, same
 * arguments) and asserts the two parent-objective bars that
 * {@code AlreadyCalledProjectionTest} only demonstrated at the
 * {@link ContextProjectionBuilder} unit layer:
 *
 * <ol>
 *   <li>Both identical-args calls reach {@link ToolDispatcher#dispatch}
 *       — the runtime does <strong>not</strong> short-circuit the second
 *       dispatch based on the slot being populated (parent objective
 *       lines 160–161, "the runtime does <strong>not</strong>
 *       short-circuit on the slot being populated ... identical-args
 *       re-emission is still dispatched unless the LLM itself avoids
 *       it").</li>
 *   <li>The second-step projection produced by
 *       {@link ContextProjectionBuilder#build} (as called from inside
 *       {@code AgentRunLoopImpl.run}) contains an {@code already_called}
 *       entry whose {@code tool} matches the first call's tool name and
 *       whose {@code arguments_hash} matches the canonical hash applied
 *       to the first call's normalized arguments.</li>
 * </ol>
 *
 * <p>Wiring: real {@link AgentRunLoopImpl} and real
 * {@link ContextProjectionBuilder} (so the actual projection JSON
 * containing the slot is exercised). Mocks for {@link LlmInvocationService},
 * {@link ToolDispatcher}, {@link ActionParser}, and the projection
 * builder's collaborator services so the loop iterates deterministically
 * over three LLM responses (tool call, tool call, final answer).
 */
@ExtendWith(MockitoExtension.class)
class AgentRunLoopAlreadyCalledNonEnforcementIntegrationTest {

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
                objectMapper, useCaseRegistry, controlPolicy, toolPolicyEnforcer);
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
    void identicalArgsReEmission_bothDispatched_andSecondProjectionShowsSlot() throws Exception {
        BotSession session = BotSession.builder()
                .sessionId("sprint20-fix-trackb-runtime")
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
        // loop terminates cleanly. The intent of this test is the
        // dispatch-count + projection-content bars; the LLM payload shape
        // is canned and parsed via a mocked ActionParser, so the response
        // text strings only need to be non-null.
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

        when(toolDispatcher.validateAgainstPlan(any(), eq("search_knowledge")))
                .thenReturn(ToolResult.ok(null));
        when(toolDispatcher.dispatch(eq("search_knowledge"), any(), any()))
                .thenReturn(ToolResult.ok(Map.of(
                        "faq_miss", false,
                        "hits", List.of(Map.of("source_id", "ka4P2-faq", "title", "Ad visibility")))));

        // ── Act ─────────────────────────────────────────────────────
        AgentRunResult result = agentRunLoop.run(plan, session, userQuery, List.of());

        // ── Assert (a): both identical-args dispatches reached ──────
        // ToolDispatcher.dispatch unconditionally — runtime did NOT
        // short-circuit on the populated already_called slot.
        verify(toolDispatcher, times(2))
                .dispatch(eq("search_knowledge"), any(), any());

        // The run loop must have produced a final answer (third LLM
        // response). If a future short-circuit landed before either
        // ToolEvent was created, the loop would exit early and the
        // dispatch-count assertion above would fail. This assertion
        // backs that up at the outcome layer.
        assertNotNull(result);
        assertEquals(TerminalOutcome.FINAL_ANSWER, result.terminalOutcome(),
                "loop should reach the no-tool-call final answer after both dispatches");

        // ── Assert (b): step-1 projection contains the already_called ──
        // entry referencing the step-0 dispatch by tool name +
        // canonical argument hash. Captures all three projection calls
        // produced by ContextProjectionBuilder.build inside the run loop
        // (one per loop iteration / LLM invocation). The slot read from
        // the SECOND captured value (step index 1) is the parent
        // objective's "second-step projection".
        ArgumentCaptor<String> projectionCaptor = ArgumentCaptor.forClass(String.class);
        verify(llmInvocation, times(3))
                .invokeChat(projectionCaptor.capture(), anyString(), anyString(), anyInt());

        List<String> projectionsByStep = projectionCaptor.getAllValues();
        assertEquals(3, projectionsByStep.size(),
                "expected three projections (one per LLM invocation); got " + projectionsByStep.size());

        // Step 0 — slot is present but empty.
        JsonNode step0Slot = objectMapper.readTree(projectionsByStep.get(0)).get("already_called");
        assertNotNull(step0Slot, "step-0 projection must always emit the already_called slot for shape stability");
        assertTrue(step0Slot.isArray(), "already_called must be a JSON array");
        assertEquals(0, step0Slot.size(),
                "step-0 already_called must be empty (no prior dispatch yet)");

        // Step 1 — slot must list the step-0 dispatch.
        JsonNode step1Slot = objectMapper.readTree(projectionsByStep.get(1)).get("already_called");
        assertNotNull(step1Slot, "step-1 projection must carry the already_called slot");
        assertTrue(step1Slot.size() >= 1,
                "step-1 already_called must contain at least one entry for the step-0 dispatch; got " + step1Slot);

        JsonNode firstEntry = step1Slot.get(0);
        assertEquals("search_knowledge", firstEntry.get("tool").asText(),
                "first slot entry tool must match the step-0 LLM-emitted tool name");
        assertEquals(0, firstEntry.get("at_step").asInt(),
                "first slot entry at_step must reference step 0 of the current run");

        // Independently compute the canonical hash the production builder
        // contract specifies (Jackson ObjectMapper with
        // ORDER_MAP_ENTRIES_BY_KEYS, SHA-256, first 16 hex chars). Computing
        // the expected value here rather than calling the production
        // canonicalArgumentsHash() helper proves the slot uses the
        // documented algorithm, not some private reimplementation.
        String expectedHash = canonicalArgsHashContract(identicalArgs);
        assertEquals(expectedHash, firstEntry.get("arguments_hash").asText(),
                "arguments_hash on the step-1 slot entry must equal the canonical hash of the "
                        + "step-0 call's normalized arguments");
    }

    private static String canonicalArgsHashContract(Map<String, Object> args) throws Exception {
        ObjectMapper canon = new ObjectMapper()
                .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
        byte[] digest = MessageDigest.getInstance("SHA-256")
                .digest(canon.writeValueAsString(args).getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder(16);
        for (int i = 0; i < 8; i++) {
            sb.append(String.format("%02x", digest[i] & 0xff));
        }
        return sb.toString();
    }
}
