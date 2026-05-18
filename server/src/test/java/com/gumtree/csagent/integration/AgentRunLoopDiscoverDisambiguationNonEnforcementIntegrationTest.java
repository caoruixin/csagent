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

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
 * Sprint 33 — runtime non-enforcement of the
 * {@code discover_disambiguation_signals} projection slot.
 *
 * <p>Drives {@link AgentRunLoopImpl#run} end-to-end across six variants
 * of {@code (formTopicSubject, listingContext)} inputs on the
 * {@link BotSession}. Each variant uses the same canned LLM response
 * (no tool calls, direct final answer) so the only thing that varies
 * across invocations is the projected slot's value. The test asserts:
 *
 * <ol>
 *   <li><strong>Projection slot value</strong> — the
 *       {@code discover_disambiguation_signals} object in the JSON
 *       the run loop hands to the LLM matches the expected per the
 *       projection contract (REMOVED/SUSPENDED/EXPIRED populate
 *       {@code ad_status_observed}; multi-candidate topics flip the
 *       multi flag; null inputs produce null / false / empty).</li>
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
 * {@code session.listingContext.status} or
 * {@code session.formTopicSubject} from any decision path in the
 * agent run loop (the LLM owns the DISCOVER classification per
 * Constitution §1.3); this parameterised test pins that property
 * against future regressions. A hypothetical future Java branch such
 * as {@code if (ad_status_observed == REMOVED) force-clarify} would
 * cause at least one of the four invariance assertions on one of the
 * REMOVED variants to disagree with the LIVE / null variants and
 * fail the suite.
 *
 * <p>Mirrors the Sprint 31 fix-iteration #2
 * {@code AgentRunLoopAlternateCandidateUseCasesNonEnforcementIntegrationTest}
 * shape (parameterised invariance bars). Real {@link AgentRunLoopImpl}
 * and {@link ContextProjectionBuilder}; mocked
 * {@link LlmInvocationService} / {@link ToolDispatcher} /
 * {@link ActionParser}.
 */
@ExtendWith(MockitoExtension.class)
class AgentRunLoopDiscoverDisambiguationNonEnforcementIntegrationTest {

    private static final String CANONICAL_FINAL_ANSWER =
            "Here is what I found about your advert status.";
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
                objectMapper, useCaseRegistry, controlPolicy, toolPolicyEnforcer, null);
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

        // Topic → candidate-UCs registry stubs covering the variants.
        lenient().when(useCaseRegistry.getCandidateUcsForTopic("Ad Support"))
                .thenReturn(List.of("UC-A", "UC-B", "UC-FP", "UC-H"));
        lenient().when(useCaseRegistry.getCandidateUcsForTopic("Replies or Messaging"))
                .thenReturn(List.of("UC-C"));

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
     * Six (topic, listing_context) variants exercising the projection
     * slot's full domain:
     * <ul>
     *   <li>V1 — REMOVED listing + multi-candidate topic (signal fully
     *       fires).</li>
     *   <li>V2 — LIVE listing + multi-candidate topic (ad_status null;
     *       multi flag on).</li>
     *   <li>V3 — REMOVED listing + single-candidate topic (ad_status
     *       populated; multi flag off).</li>
     *   <li>V4 — null listing_context + multi-candidate topic
     *       (ad_status null; multi flag on).</li>
     *   <li>V5 — REMOVED listing + null topic (ad_status populated;
     *       multi flag off).</li>
     *   <li>V6 — SUSPENDED listing + multi-candidate topic (ad_status
     *       populated; multi flag on).</li>
     * </ul>
     * All six SHALL reach the same {@link TerminalOutcome}, the same
     * {@code invokeChat} call count, the same final user message, and
     * SHALL NOT invoke the {@link ToolDispatcher}.
     */
    static Stream<Arguments> slotVariants() {
        return Stream.of(
                Arguments.of("V1_REMOVED_AdSupport",
                        "Ad Support",
                        "{\"status\":\"REMOVED\",\"ad_id\":\"AD-2001\"}",
                        "REMOVED", true, List.of("UC-A", "UC-B", "UC-FP", "UC-H")),
                Arguments.of("V2_LIVE_AdSupport",
                        "Ad Support",
                        "{\"status\":\"LIVE\",\"ad_id\":\"AD-9001\"}",
                        null, true, List.of("UC-A", "UC-B", "UC-FP", "UC-H")),
                Arguments.of("V3_REMOVED_Messaging",
                        "Replies or Messaging",
                        "{\"status\":\"REMOVED\",\"ad_id\":\"AD-3001\"}",
                        "REMOVED", false, List.<String>of()),
                Arguments.of("V4_nullListing_AdSupport",
                        "Ad Support",
                        null,
                        null, true, List.of("UC-A", "UC-B", "UC-FP", "UC-H")),
                Arguments.of("V5_REMOVED_nullTopic",
                        null,
                        "{\"status\":\"REMOVED\",\"ad_id\":\"AD-4001\"}",
                        "REMOVED", false, List.<String>of()),
                Arguments.of("V6_SUSPENDED_AdSupport",
                        "Ad Support",
                        "{\"status\":\"SUSPENDED\",\"ad_id\":\"AD-5001\"}",
                        "SUSPENDED", true, List.of("UC-A", "UC-B", "UC-FP", "UC-H"))
        );
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("slotVariants")
    void runtimeBehaviorIsInvariantToSlotValue(
            String variantLabel,
            String formTopicSubject,
            String listingContextJson,
            String expectedAdStatus,
            boolean expectedMultiCandidate,
            List<String> expectedCandidateUcs) throws Exception {

        BotSession session = BotSession.builder()
                .sessionId("sprint33-disambig-runtime-" + variantLabel)
                .activeUseCase(ACTIVE_UC)
                .currentPhase("DISCOVER")
                .handlingState("BOT_HANDLING")
                .totalBotTurns(0)
                .clarificationCount(0)
                .faqMissCount(0)
                .repeatedActionCount(0)
                .formTopicSubject(formTopicSubject)
                .listingContext(listingContextJson)
                .build();

        PhasePlan plan = PhasePlan.builder()
                .phase("DISCOVER")
                .useCase(ACTIVE_UC)
                .objective("classify")
                .allowedTools(List.of("search_knowledge", "classify_use_case"))
                .maxToolSteps(2)
                .systemInstruction("DISCOVER")
                .validTerminalOutcomes(Set.of(TerminalOutcome.FINAL_ANSWER,
                        TerminalOutcome.CLARIFICATION_NEEDED,
                        TerminalOutcome.ESCALATE))
                .build();

        // ── Act ─────────────────────────────────────────────────────
        AgentRunResult result = agentRunLoop.run(plan, session, USER_QUERY, List.of());

        // ── Invariance assertion 1: projection slot value matches the
        //                            expected per the projection
        //                            contract.
        ArgumentCaptor<String> projectionCaptor = ArgumentCaptor.forClass(String.class);
        verify(llmInvocation, times(1))
                .invokeChat(projectionCaptor.capture(), anyString(), anyString(), anyInt());
        JsonNode root = objectMapper.readTree(projectionCaptor.getValue());

        assertTrue(root.has("discover_disambiguation_signals"),
                variantLabel + ": projection must always carry the "
                        + "discover_disambiguation_signals slot (shape stability).");
        JsonNode slot = root.get("discover_disambiguation_signals");
        assertTrue(slot.isObject(),
                variantLabel + ": discover_disambiguation_signals must be a JSON object.");

        if (expectedAdStatus == null) {
            assertTrue(slot.get("ad_status_observed").isNull(),
                    variantLabel + ": ad_status_observed must be null when "
                            + "listing is LIVE / absent. Got " + slot.get("ad_status_observed"));
        } else {
            assertEquals(expectedAdStatus, slot.get("ad_status_observed").asText(),
                    variantLabel + ": ad_status_observed mismatch.");
        }

        assertEquals(expectedMultiCandidate,
                slot.get("topic_subject_carries_multiple_candidate_ucs").asBoolean(),
                variantLabel + ": topic_subject_carries_multiple_candidate_ucs mismatch.");

        JsonNode candidatesNode = slot.get("candidate_ucs_for_topic");
        assertTrue(candidatesNode.isArray(),
                variantLabel + ": candidate_ucs_for_topic must be an array.");
        assertEquals(expectedCandidateUcs.size(), candidatesNode.size(),
                variantLabel + ": candidate_ucs_for_topic size mismatch. Got " + candidatesNode);
        for (int i = 0; i < expectedCandidateUcs.size(); i++) {
            assertEquals(expectedCandidateUcs.get(i), candidatesNode.get(i).asText(),
                    variantLabel + ": candidate_ucs_for_topic[" + i + "] mismatch.");
        }

        // ── Invariance assertion 2: TerminalOutcome is FINAL_ANSWER on
        //                            every variant — the slot does NOT
        //                            redirect the loop to ESCALATE or
        //                            terminate it early.
        assertNotNull(result, variantLabel + ": run() must return a non-null result.");
        assertEquals(TerminalOutcome.FINAL_ANSWER, result.terminalOutcome(),
                variantLabel + ": loop must reach FINAL_ANSWER; the slot's value "
                        + "MUST NOT gate the terminal outcome.");

        // ── Invariance assertion 3: invokeChat called exactly once on
        //                            every variant — already verified
        //                            above via times(1) on the captor.
        //                            A hypothetical slot-gated re-prompt
        //                            (e.g. "if ad_status_observed ==
        //                            REMOVED then prepend a clarifying
        //                            preface and re-invoke") would
        //                            diverge here on V1 / V3 / V5 / V6
        //                            but not V2 / V4.

        // ── Invariance assertion 4: ToolDispatcher is never invoked on
        //                            any variant. A hypothetical
        //                            slot-gated dispatch (e.g. "if
        //                            ad_status_observed == REMOVED then
        //                            force-dispatch search_knowledge")
        //                            would trip this on V1 / V3 / V5 /
        //                            V6 but not V2 / V4.
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

        // ── Cross-cutting: when ad_status_observed is null, the slot
        //                  must be a JSON null (not the string "null")
        //                  so downstream parsers see the right type.
        if (expectedAdStatus == null) {
            assertFalse(slot.get("ad_status_observed").isTextual(),
                    variantLabel + ": ad_status_observed must be JSON null, not the "
                            + "literal string \"null\".");
        }
    }
}
