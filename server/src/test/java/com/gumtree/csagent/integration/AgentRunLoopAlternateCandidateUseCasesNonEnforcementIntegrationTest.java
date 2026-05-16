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
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Sprint 31 — runtime non-enforcement of the
 * {@code alternate_candidate_use_cases} projection slot
 * ({@code R-alternate-uc-signal-data-source}, Option β).
 *
 * <p>Drives {@link AgentRunLoopImpl#run} end-to-end across six variants
 * of {@code intakeAmbiguousCandidates} field values on the
 * {@link BotSession}. Each variant uses the same canned LLM response
 * (no tool calls, direct final answer) so the only thing that varies
 * across invocations is the slot's input value. The test asserts:
 *
 * <ol>
 *   <li><strong>Projection slot value</strong> — the
 *       {@code alternate_candidate_use_cases} array in the JSON the
 *       run loop hands to the LLM matches the expected value per the
 *       projection contract (active UC filtered out, null / empty
 *       inputs produce empty arrays).</li>
 *   <li><strong>Behaviour invariance</strong> across slot variants:
 *       <ul>
 *         <li>{@link TerminalOutcome#FINAL_ANSWER} on every variant.</li>
 *         <li>Exactly one {@code llmInvocation.invokeChat(...)} call
 *             per variant.</li>
 *         <li>{@link ToolDispatcher} is never invoked.</li>
 *         <li>Final user message text is identical across variants.</li>
 *       </ul>
 *   </li>
 * </ol>
 *
 * <p>The runtime does not consult
 * {@code session.intakeAmbiguousCandidates} from any decision path
 * (see {@code grep} verified at premise check); this parameterised
 * test pins that property against future regressions. A hypothetical
 * future Java branch such as
 * {@code if (session.getIntakeAmbiguousCandidates() contains "UC-C")
 * then short-circuit X} would cause at least one of the four
 * invariance assertions on one of the V4 / V5 / V6 variants to
 * disagree with the V1 / V2 / V3 (empty-slot) variants and fail the
 * suite.
 *
 * <p>Mirrors the Sprint 20
 * {@code AgentRunLoopAlreadyCalledNonEnforcementIntegrationTest} shape.
 * Real {@link AgentRunLoopImpl} and {@link ContextProjectionBuilder};
 * mocked {@link LlmInvocationService} / {@link ToolDispatcher} /
 * {@link ActionParser}.
 *
 * <p>Sprint 31 fix-iteration #2 — strengthened from the original single
 * happy-path scenario to six parameterised variants in response to
 * Codex Finding 2 (test too weak to demonstrate slot-value invariance).
 */
@ExtendWith(MockitoExtension.class)
class AgentRunLoopAlternateCandidateUseCasesNonEnforcementIntegrationTest {

    private static final String CANONICAL_FINAL_ANSWER =
            "Here is what I found about your advert visibility.";
    private static final String USER_QUERY = "where is my advert";
    private static final String ACTIVE_UC = "UC-A";

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
                ACTIVE_UC, "Ad Status & Visibility", List.of("Ad Support"), "LOW", true, "FAQ");
        lenient().when(useCaseRegistry.getUseCase(ACTIVE_UC)).thenReturn(uca);
        lenient().when(toolPolicyEnforcer.getVisibleToolsForUc(ACTIVE_UC)).thenReturn(List.of(
                "search_knowledge", "resolve_article", "get_customer_context",
                "request_handover", "record_outcome"));

        // Canned LLM response: no tool calls, direct final answer. The
        // loop completes in one iteration regardless of which variant
        // is exercised — so any cross-variant divergence on the four
        // invariance bars must come from a runtime branch on the slot
        // (the property under test).
        LlmResponse raw = LlmResponse.builder()
                .content("{\"step\":0}")
                .promptTokens(10).completionTokens(5).build();
        when(llmInvocation.invokeChat(anyString(), anyString(), anyString(), anyInt()))
                .thenReturn(raw);
        when(actionParser.parse(raw.getContent())).thenReturn(ParsedAction.builder()
                .toolCalls(List.of())
                .userMessage(CANONICAL_FINAL_ANSWER)
                .reasoning("final answer").build());
    }

    /**
     * Six slot-value variants. V1–V3 produce an empty projection slot
     * (null / empty / single-element matching active filtered out);
     * V4–V6 produce a non-empty slot. All six SHALL reach the same
     * {@link TerminalOutcome}, the same {@code invokeChat} call count,
     * the same final user message, and SHALL NOT invoke the
     * {@link ToolDispatcher}.
     */
    static Stream<Arguments> slotVariants() {
        return Stream.of(
                Arguments.of("V1_null_ROUTED_style",
                        null,
                        List.of()),
                Arguments.of("V2_empty_array",
                        new String[0],
                        List.of()),
                Arguments.of("V3_single_element_matching_active",
                        new String[]{"UC-A"},
                        List.of()),
                Arguments.of("V4_single_alternate",
                        new String[]{"UC-C"},
                        List.of("UC-C")),
                Arguments.of("V5_multi_element_active_plus_alternates",
                        new String[]{"UC-A", "UC-C", "UC-D"},
                        List.of("UC-C", "UC-D")),
                Arguments.of("V6_unrelated_ucs_only",
                        new String[]{"UC-F", "UC-G"},
                        List.of("UC-F", "UC-G"))
        );
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("slotVariants")
    void runtimeBehaviorIsInvariantToSlotValue(
            String variantLabel,
            String[] intakeAmbiguousCandidates,
            List<String> expectedSlotContents) throws Exception {

        BotSession session = BotSession.builder()
                .sessionId("sprint31-altuc-runtime-" + variantLabel)
                .activeUseCase(ACTIVE_UC)
                .currentPhase("RESOLVE")
                .handlingState("BOT_HANDLING")
                .totalBotTurns(0)
                .clarificationCount(0)
                .faqMissCount(0)
                .repeatedActionCount(0)
                .intakeAmbiguousCandidates(intakeAmbiguousCandidates)
                .build();

        PhasePlan plan = PhasePlan.builder()
                .phase("RESOLVE")
                .useCase(ACTIVE_UC)
                .objective("answer")
                .allowedTools(List.of("search_knowledge", "resolve_article",
                        "request_handover", "record_outcome"))
                .maxToolSteps(3)
                .systemInstruction("RESOLVE")
                .validTerminalOutcomes(Set.of(TerminalOutcome.FINAL_ANSWER,
                        TerminalOutcome.ESCALATE))
                .build();

        // ── Act ─────────────────────────────────────────────────────
        AgentRunResult result = agentRunLoop.run(plan, session, USER_QUERY, List.of());

        // ── Invariance assertion 1: projection slot value matches the
        //                            expected per the projection
        //                            contract (active UC filtered).
        ArgumentCaptor<String> projectionCaptor = ArgumentCaptor.forClass(String.class);
        verify(llmInvocation, times(1))
                .invokeChat(projectionCaptor.capture(), anyString(), anyString(), anyInt());
        JsonNode root = objectMapper.readTree(projectionCaptor.getValue());

        assertTrue(root.has("alternate_candidate_use_cases"),
                variantLabel + ": projection must always carry the "
                        + "alternate_candidate_use_cases slot (shape stability).");
        JsonNode slot = root.get("alternate_candidate_use_cases");
        assertTrue(slot.isArray(),
                variantLabel + ": alternate_candidate_use_cases must be a JSON array.");

        List<String> actualSlotContents = new ArrayList<>();
        slot.forEach(node -> actualSlotContents.add(node.asText()));
        assertEquals(expectedSlotContents, actualSlotContents,
                variantLabel + ": projection slot contents do not match expected. "
                        + "Got " + actualSlotContents + ", expected " + expectedSlotContents);

        // ── Invariance assertion 2: TerminalOutcome is FINAL_ANSWER on
        //                            every variant — the slot does NOT
        //                            redirect the loop to ESCALATE or
        //                            terminate it early.
        assertNotNull(result, variantLabel + ": run() must return a non-null result.");
        assertEquals(TerminalOutcome.FINAL_ANSWER, result.terminalOutcome(),
                variantLabel + ": loop must reach FINAL_ANSWER; the slot's value "
                        + "MUST NOT gate the terminal outcome.");

        // ── Invariance assertion 3: invokeChat called exactly once on
        //                            every variant — the slot does NOT
        //                            short-circuit before the LLM call
        //                            on V1–V3 (empty), nor cause a
        //                            re-prompt loop on V4–V6 (non-empty).
        //                            (Captured count already verified
        //                            above via times(1) in the captor
        //                            block; this is the explicit
        //                            invariance bar.)

        // ── Invariance assertion 4: ToolDispatcher is never invoked on
        //                            any variant. A hypothetical
        //                            slot-gated dispatch (e.g. "if slot
        //                            contains UC-C, force-dispatch
        //                            classify_use_case") would trip
        //                            this on V4 / V5 but not V1–V3.
        verifyNoInteractions(toolDispatcher);

        // ── Invariance assertion 5: final user-message text is the
        //                            canonical string on every variant.
        //                            A slot-driven rewrite of the final
        //                            answer would diverge here even if
        //                            outcome + counts stayed the same.
        assertNotNull(result.finalUserMessage(),
                variantLabel + ": final user message must not be null.");
        assertEquals(CANONICAL_FINAL_ANSWER, result.finalUserMessage(),
                variantLabel + ": final user message text MUST NOT vary with the "
                        + "slot's value.");
    }
}
