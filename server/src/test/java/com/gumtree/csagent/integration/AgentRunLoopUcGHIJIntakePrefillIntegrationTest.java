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

import java.util.LinkedHashSet;
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
 * Sprint 34 — end-to-end wiring proof for the UC-G / UC-H / UC-I / UC-J
 * form-context intake prefill extension. Drives
 * {@link AgentRunLoopImpl#run} end-to-end with a real
 * {@link ContextProjectionBuilder} and mocked LLM / dispatcher; each
 * parameterised variant pins:
 *
 * <ol>
 *   <li><strong>Projection slot evidence</strong>: the
 *       {@code intake_state.fields_collected} block in the JSON the run
 *       loop hands to the LLM contains the expected canonical names per
 *       UC (or stays empty for UC-I no-op + UC-A negative control).</li>
 *   <li><strong>Behaviour invariance</strong> across variants:
 *       <ul>
 *         <li>{@link TerminalOutcome#FINAL_ANSWER} on every variant
 *             (plumbing must not gate the terminal outcome).</li>
 *         <li>Exactly one {@code llmInvocation.invokeChat(...)} call per
 *             variant.</li>
 *         <li>{@link ToolDispatcher} is never invoked (no tool call in
 *             the canned LLM response).</li>
 *         <li>Final user message is the canonical string on every
 *             variant.</li>
 *       </ul>
 *   </li>
 * </ol>
 *
 * <p>Mirrors the Sprint 33
 * {@code AgentRunLoopDiscoverDisambiguationNonEnforcementIntegrationTest}
 * + Sprint 31 fix-iteration #2 parameterised-invariance test pattern.
 * Real {@link AgentRunLoopImpl} and {@link ContextProjectionBuilder};
 * mocked {@link LlmInvocationService} / {@link ToolDispatcher} /
 * {@link ActionParser}.
 */
@ExtendWith(MockitoExtension.class)
class AgentRunLoopUcGHIJIntakePrefillIntegrationTest {

    private static final String CANONICAL_FINAL_ANSWER =
            "Got it — I'll take it from here.";

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
        lenient().when(controlPolicy.getMaxBotTurnsIntake()).thenReturn(8);
        lenient().when(controlPolicy.getMaxClarificationRounds()).thenReturn(3);
        lenient().when(controlPolicy.getMaxFaqMiss()).thenReturn(2);

        wireUcDef("UC-A", "Ad Status & Visibility", "Ad Support", "LOW", true, "FAQ");
        wireUcDef("UC-G", "GDPR / Data Action", "Account & Privacy", "MEDIUM", false, "INTAKE");
        wireUcDef("UC-H", "Ad Removal Appeal", "Ad Support", "MEDIUM", false, "INTAKE");
        wireUcDef("UC-I", "Refund / Payment Dispute", "Payments", "MEDIUM", false, "INTAKE");
        wireUcDef("UC-J", "Trust & Safety Report", "Trust & Safety", "MEDIUM", false, "INTAKE");
        wireUcDef("UC-K", "Technical Issue Intake", "Technical Support", "MEDIUM", false, "INTAKE");

        lenient().when(useCaseRegistry.getCandidateUcsForTopic("Ad Support"))
                .thenReturn(List.of("UC-A", "UC-B", "UC-FP", "UC-H"));
        lenient().when(useCaseRegistry.getCandidateUcsForTopic("Account & Privacy"))
                .thenReturn(List.of("UC-G"));
        lenient().when(useCaseRegistry.getCandidateUcsForTopic("Payments"))
                .thenReturn(List.of("UC-I"));
        lenient().when(useCaseRegistry.getCandidateUcsForTopic("Trust & Safety"))
                .thenReturn(List.of("UC-J"));
        lenient().when(useCaseRegistry.getCandidateUcsForTopic("Technical Support"))
                .thenReturn(List.of("UC-K"));

        // Canned LLM response: no tool calls, direct final answer. The
        // loop completes in one iteration regardless of which variant is
        // exercised — so any cross-variant divergence on the invariance
        // bars must come from a runtime branch on the prefill state (the
        // property under test).
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
     * Six variants × five invariance bars. Each variant is one
     * (active UC, form_context JSON) tuple. {@code expectedCollected}
     * is the set of canonical intake-field names that MUST appear in
     * {@code intake_state.fields_collected} on the projection sent to
     * the LLM. UC-I and UC-A variants assert the empty set
     * (deliberate no-op + FAQ-path negative control); UC-K is the
     * regression-guard variant inherited from Sprint 7.1.
     */
    static Stream<Arguments> prefillVariants() {
        return Stream.of(
                Arguments.of("V1_UC-H_AdIdAndEmail",
                        "UC-H", "Ad Support",
                        "{\"first_name\":\"Alice\","
                                + "\"email\":\"alice@example.com\","
                                + "\"topic_subject\":\"Ad Support\","
                                + "\"ad_id\":\"AD-2001\","
                                + "\"description\":\"can't see my ad\"}",
                        Set.of("ad_id_or_listing_url", "registered_email")),
                Arguments.of("V2_UC-G_EmailOnly",
                        "UC-G", "Account & Privacy",
                        "{\"first_name\":\"Dave\","
                                + "\"email\":\"dave@example.com\","
                                + "\"topic_subject\":\"Account & Privacy\","
                                + "\"description\":\"delete my account\"}",
                        Set.of("registered_email")),
                Arguments.of("V3_UC-J_DescriptionOnly",
                        "UC-J", "Trust & Safety",
                        "{\"first_name\":\"Frank\","
                                + "\"email\":\"frank@example.com\","
                                + "\"topic_subject\":\"Trust & Safety\","
                                + "\"description\":\"user XYZ posts spam repeatedly\"}",
                        Set.of("description")),
                Arguments.of("V4_UC-I_NoOp",
                        "UC-I", "Payments",
                        "{\"first_name\":\"Ivan\","
                                + "\"email\":\"ivan@example.com\","
                                + "\"topic_subject\":\"Payments\","
                                + "\"description\":\"refund didn't arrive\"}",
                        Set.<String>of()),
                Arguments.of("V5_UC-K_RegressionReference",
                        "UC-K", "Technical Support",
                        "{\"first_name\":\"Stephen\","
                                + "\"email\":\"stephen@example.com\","
                                + "\"topic_subject\":\"Technical Support\","
                                + "\"description\":\"Why am I not getting the option "
                                + "to add my phone number any more\"}",
                        Set.of("repro_steps_or_error_message")),
                Arguments.of("V6_UC-A_FaqNegativeControl",
                        "UC-A", "Ad Support",
                        "{\"first_name\":\"Alice\","
                                + "\"email\":\"alice@example.com\","
                                + "\"topic_subject\":\"Ad Support\","
                                + "\"ad_id\":\"AD-9000\","
                                + "\"description\":\"where is my advert\"}",
                        Set.<String>of())
        );
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("prefillVariants")
    void runtimePrefillBehaviorIsInvariantAcrossVariants(
            String variantLabel,
            String activeUc,
            String topicSubject,
            String formContextJson,
            Set<String> expectedCollected) throws Exception {

        BotSession session = BotSession.builder()
                .sessionId("sprint34-prefill-" + variantLabel)
                .activeUseCase(activeUc)
                .currentPhase("RESOLVE")
                .handlingState("BOT_HANDLING")
                .totalBotTurns(0)
                .clarificationCount(0)
                .faqMissCount(0)
                .repeatedActionCount(0)
                .formTopicSubject(topicSubject)
                .formContext(formContextJson)
                .build();

        List<String> allowedTools = "UC-A".equals(activeUc)
                ? List.of("search_knowledge", "resolve_article",
                          "request_handover", "record_outcome")
                : List.of("request_handover");
        PhasePlan plan = PhasePlan.builder()
                .phase("RESOLVE")
                .useCase(activeUc)
                .objective("UC-A".equals(activeUc) ? "faq" : "intake")
                .allowedTools(allowedTools)
                .maxToolSteps(3)
                .validTerminalOutcomes(Set.of(TerminalOutcome.FINAL_ANSWER,
                        TerminalOutcome.CLARIFICATION_NEEDED,
                        TerminalOutcome.ESCALATE))
                .build();

        // ── Act ─────────────────────────────────────────────────────
        AgentRunResult result = agentRunLoop.run(plan, session,
                "user turn 1 message", List.of());

        // ── Invariance assertion 1: projection captured handed to LLM
        //                            has intake_state.fields_collected
        //                            matching the expected canonical
        //                            names exactly. FAQ-path UCs (V6
        //                            UC-A) emit NO intake_state slot at
        //                            all — proves the prefill path is
        //                            gated correctly per the existing
        //                            IntakeFieldsRegistry.isIntakeUseCase
        //                            check in ContextProjectionBuilder.
        ArgumentCaptor<String> projectionCaptor = ArgumentCaptor.forClass(String.class);
        verify(llmInvocation, times(1))
                .invokeChat(projectionCaptor.capture(), anyString(), anyString(), anyInt());
        JsonNode root = objectMapper.readTree(projectionCaptor.getValue());

        boolean isIntakeUc = !"UC-A".equals(activeUc);
        if (!isIntakeUc) {
            assertFalse(root.has("intake_state"),
                    variantLabel + ": FAQ-path UC-A must emit no intake_state slot — "
                            + "Sprint 34 must not erode the existing isIntakeUseCase gate");
        } else {
            assertTrue(root.has("intake_state"),
                    variantLabel + ": intake-path UC must carry intake_state slot");
            JsonNode collectedNode = root.get("intake_state").get("fields_collected");
            assertNotNull(collectedNode,
                    variantLabel + ": intake_state.fields_collected must be present");
            assertTrue(collectedNode.isObject(),
                    variantLabel + ": intake_state.fields_collected must be a JSON object");

            Set<String> actualCollected = new LinkedHashSet<>();
            collectedNode.fieldNames().forEachRemaining(actualCollected::add);
            assertEquals(expectedCollected, actualCollected,
                    variantLabel + ": intake_state.fields_collected canonical names mismatch. "
                            + "Got " + actualCollected);
        }

        // ── Invariance assertion 2: TerminalOutcome is FINAL_ANSWER on
        //                            every variant — the prefill state
        //                            does NOT gate the terminal outcome.
        assertNotNull(result, variantLabel + ": run() must return a non-null result");
        assertEquals(TerminalOutcome.FINAL_ANSWER, result.terminalOutcome(),
                variantLabel + ": prefill state MUST NOT gate the terminal outcome");

        // ── Invariance assertion 3: invokeChat called exactly once on
        //                            every variant (already verified via
        //                            times(1) on the captor above). A
        //                            hypothetical prefill-gated re-prompt
        //                            ("if intake_complete then prepend a
        //                            confirmation preface and re-invoke")
        //                            would diverge here on V1/V2/V3/V5
        //                            but not V4/V6.

        // ── Invariance assertion 4: ToolDispatcher is never invoked on
        //                            any variant — the canned LLM
        //                            response has no tool calls; the
        //                            prefill state must not cause the
        //                            run loop to inject one.
        verifyNoInteractions(toolDispatcher);

        // ── Invariance assertion 5: final user-message text is the
        //                            canonical string on every variant.
        //                            A prefill-driven rewrite of the
        //                            final answer would diverge here
        //                            even if outcome + counts matched.
        assertNotNull(result.finalUserMessage(),
                variantLabel + ": final user message must not be null");
        assertEquals(CANONICAL_FINAL_ANSWER, result.finalUserMessage(),
                variantLabel + ": final user-message text MUST NOT vary with prefill state");

        // ── Cross-cutting: persisted session.intakeFields for the
        //                  intake-UC variants must contain the same
        //                  canonical names that the projection surfaced.
        //                  UC-I + UC-A leave session.intakeFields null
        //                  per the no-op + FAQ-path semantics.
        if (expectedCollected.isEmpty()) {
            assertTrue(session.getIntakeFields() == null
                            || session.getIntakeFields().isBlank()
                            || objectMapper.readTree(session.getIntakeFields()).size() == 0,
                    variantLabel + ": session.intakeFields must be unwritten "
                            + "(or empty) for no-op / FAQ-path variants");
        } else {
            assertNotNull(session.getIntakeFields(),
                    variantLabel + ": session.intakeFields must be persisted");
            JsonNode persisted = objectMapper.readTree(session.getIntakeFields());
            for (String key : expectedCollected) {
                assertTrue(persisted.has(key),
                        variantLabel + ": persisted session.intakeFields must contain "
                                + key + ". Got " + persisted);
            }
        }

        // ── Cross-cutting: intake_complete flag matches reality. For
        //                  the variants where only some required fields
        //                  are prefilled (V1 missing stated_reason; V2
        //                  missing data_request_type; V3 missing
        //                  report_target + report_type; V5 missing
        //                  platform), intake_complete must remain false.
        //                  V4 UC-I no-op keeps the original two
        //                  required fields missing → false. V6 UC-A is
        //                  FAQ-path with no intake_state slot at all
        //                  (asserted above) so this check is skipped.
        if (isIntakeUc) {
            assertFalse(root.get("intake_state").get("intake_complete").asBoolean(),
                    variantLabel + ": intake_complete must remain false for every "
                            + "Sprint 34 intake variant — no variant supplies the full required set");
        }
    }

    private void wireUcDef(String uc, String name, String topic, String risk,
                           boolean implemented, String phaseShape) {
        UseCaseRegistryService.UseCaseDefinition def =
                new UseCaseRegistryService.UseCaseDefinition(
                        uc, name, List.of(topic), risk, implemented, phaseShape);
        lenient().when(useCaseRegistry.getUseCase(uc)).thenReturn(def);
        lenient().when(toolPolicyEnforcer.getVisibleToolsForUc(uc)).thenReturn(List.of(
                "search_knowledge", "resolve_article", "get_customer_context",
                "request_handover", "record_outcome"));
    }
}
