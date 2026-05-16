package com.gumtree.csagent.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gumtree.csagent.model.AgentRunResult;
import com.gumtree.csagent.model.BotSession;
import com.gumtree.csagent.model.LlmResponse;
import com.gumtree.csagent.model.ParsedAction;
import com.gumtree.csagent.model.PhasePlan;
import com.gumtree.csagent.model.TerminalOutcome;
import com.gumtree.csagent.service.runtime.ActionParser;
import com.gumtree.csagent.service.runtime.AgentRunLoop;
import com.gumtree.csagent.service.runtime.AgentRunLoopImpl;
import com.gumtree.csagent.service.runtime.ContextProjectionBuilder;
import com.gumtree.csagent.service.runtime.ControlPolicyService;
import com.gumtree.csagent.service.runtime.LlmInvocationService;
import com.gumtree.csagent.service.runtime.UseCaseRegistryService;
import com.gumtree.csagent.service.tools.ToolDispatcher;
import com.gumtree.csagent.service.tools.ToolPolicyEnforcer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Sprint 31 — runtime non-enforcement of the
 * {@code alternate_candidate_use_cases} projection slot
 * ({@code R-alternate-uc-signal-data-source}, Option β).
 *
 * <p>Drives {@link AgentRunLoopImpl#run} end-to-end against a session
 * whose {@code intakeAmbiguousCandidates} snapshot is populated and
 * asserts the two parent-objective bars that
 * {@code IntakeAmbiguousCandidatesProjectionTest} only demonstrates at
 * the {@link ContextProjectionBuilder} unit layer:
 *
 * <ol>
 *   <li>The projection produced by
 *       {@link ContextProjectionBuilder#build} (as called from inside
 *       {@code AgentRunLoopImpl.run}) contains the populated
 *       {@code alternate_candidate_use_cases} array (active UC excluded).</li>
 *   <li>The runtime reaches a final answer without short-circuiting on
 *       the slot — i.e. the slot's value does not gate dispatch,
 *       phase transition, or escalation decisions. The implementation
 *       does not consult {@code session.intakeAmbiguousCandidates} from
 *       any decision path; this integration test pins that property
 *       against future regressions.</li>
 * </ol>
 *
 * <p>Mirrors the Sprint 20
 * {@code AgentRunLoopAlreadyCalledNonEnforcementIntegrationTest} shape.
 * Real {@link AgentRunLoopImpl} and {@link ContextProjectionBuilder};
 * mocked {@link LlmInvocationService} / {@link ToolDispatcher} /
 * {@link ActionParser}.
 */
@ExtendWith(MockitoExtension.class)
class AgentRunLoopAlternateCandidateUseCasesNonEnforcementIntegrationTest {

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
    void populatedAlternateSlot_appearsInProjection_andRuntimeDoesNotShortCircuit() throws Exception {
        // Session models a UC-A↔UC-C intake-ambiguous session whose
        // intake routing picked UC-A as the active UC and surfaced UC-C
        // as the alternate. The projection emitted at each loop iteration
        // SHALL carry alternate_candidate_use_cases = ["UC-C"].
        BotSession session = BotSession.builder()
                .sessionId("sprint31-altuc-runtime")
                .activeUseCase("UC-A")
                .currentPhase("RESOLVE")
                .handlingState("BOT_HANDLING")
                .totalBotTurns(0)
                .clarificationCount(0)
                .faqMissCount(0)
                .repeatedActionCount(0)
                .intakeAmbiguousCandidates(new String[]{"UC-A", "UC-C"})
                .build();

        PhasePlan plan = PhasePlan.builder()
                .phase("RESOLVE")
                .useCase("UC-A")
                .objective("answer")
                .allowedTools(List.of("search_knowledge", "resolve_article",
                        "request_handover", "record_outcome"))
                .maxToolSteps(3)
                .systemInstruction("RESOLVE")
                .validTerminalOutcomes(Set.of(TerminalOutcome.FINAL_ANSWER,
                        TerminalOutcome.ESCALATE))
                .build();

        // Single LLM response: no tool calls, direct final answer. The
        // loop completes in one iteration.
        LlmResponse raw = LlmResponse.builder()
                .content("{\"step\":0}")
                .promptTokens(10).completionTokens(5).build();
        when(llmInvocation.invokeChat(anyString(), anyString(), anyString(), anyInt()))
                .thenReturn(raw);
        when(actionParser.parse(raw.getContent())).thenReturn(ParsedAction.builder()
                .toolCalls(List.of())
                .userMessage("Here is what I found about your advert visibility.")
                .reasoning("final answer").build());

        // ── Act ─────────────────────────────────────────────────────
        AgentRunResult result = agentRunLoop.run(plan, session, "where is my advert", List.of());

        // ── Assert (a): the projection sent to the LLM carries the slot
        //                populated with the alternate UC, active UC filtered.
        ArgumentCaptor<String> projectionCaptor = ArgumentCaptor.forClass(String.class);
        verify(llmInvocation).invokeChat(projectionCaptor.capture(), anyString(), anyString(), anyInt());
        JsonNode root = objectMapper.readTree(projectionCaptor.getValue());

        assertTrue(root.has("alternate_candidate_use_cases"),
                "Projection inside AgentRunLoop must carry the alternate_candidate_use_cases slot.");
        JsonNode slot = root.get("alternate_candidate_use_cases");
        assertTrue(slot.isArray(), "alternate_candidate_use_cases must be a JSON array.");
        assertEquals(1, slot.size(),
                "Active UC (UC-A) must be filtered out of the alternates list. Got: " + slot);
        assertEquals("UC-C", slot.get(0).asText());

        // ── Assert (b): the runtime reached a final answer normally —
        //                no branch on the slot prevented or short-circuited
        //                the loop. The non-enforcement guarantee is that
        //                no decision path consults
        //                session.intakeAmbiguousCandidates; this assertion
        //                pins the outcome layer in case a future change
        //                quietly wires the slot into a runtime decision.
        assertNotNull(result);
        assertEquals(TerminalOutcome.FINAL_ANSWER, result.terminalOutcome(),
                "Loop must reach FINAL_ANSWER; populated alternates slot does NOT "
                        + "gate runtime decisions.");
    }
}
