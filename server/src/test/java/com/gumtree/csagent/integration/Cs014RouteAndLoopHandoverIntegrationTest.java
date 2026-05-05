package com.gumtree.csagent.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gumtree.csagent.config.AgentRunLoopProperties;
import com.gumtree.csagent.model.BotSession;
import com.gumtree.csagent.model.BotTurn;
import com.gumtree.csagent.model.DriftResult;
import com.gumtree.csagent.model.LlmResponse;
import com.gumtree.csagent.model.ParsedAction;
import com.gumtree.csagent.model.ToolCall;
import com.gumtree.csagent.repository.BotEventRepository;
import com.gumtree.csagent.repository.BotTurnRepository;
import com.gumtree.csagent.service.observability.EventEmitter;
import com.gumtree.csagent.service.runtime.ActionParser;
import com.gumtree.csagent.service.runtime.AgentRunLoop;
import com.gumtree.csagent.service.runtime.AgentRunLoopImpl;
import com.gumtree.csagent.service.runtime.BudgetChecker;
import com.gumtree.csagent.service.runtime.ContextProjectionBuilder;
import com.gumtree.csagent.service.runtime.ControlKernel;
import com.gumtree.csagent.service.runtime.ControlPolicyService;
import com.gumtree.csagent.service.runtime.DriftDetector;
import com.gumtree.csagent.service.runtime.EscalationReasonResolver;
import com.gumtree.csagent.service.runtime.HandoverPayloadAssembler;
import com.gumtree.csagent.service.runtime.PhaseEvaluator;
import com.gumtree.csagent.service.tools.CreateCaseControlledTool;
import com.gumtree.csagent.service.tools.ToolDispatcher;
import com.gumtree.csagent.service.tools.ToolResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.invocation.InvocationOnMock;

/**
 * Sprint 2.1 P1 — case-level integration regression for cs_interactive_014.
 *
 * <p>The earlier {@code Cs014RouteAndDistressRegressionTest} pinned helper
 * behaviour (distress detector, B2 messaging bias, topic alias, UC-K
 * negative, reason canonicalisation) but did not exercise
 * {@link ControlKernel} / {@link AgentRunLoop} or the persistence layer.
 * Codex Sprint 2.1 P1 #2 explicitly asked for one focused
 * {@code processMessage}-level integration test that:
 * <ol>
 *   <li>Starts from cs_014's verbatim form context.</li>
 *   <li>Replays the relevant follow-up turn through the actual
 *       route/loop path.</li>
 *   <li>Asserts the persisted tool_calls trace, session state, and
 *       handover payload all carry the same canonical
 *       {@code escalation_reason} (the L1
 *       {@code escalation_reason_consistency} contract).</li>
 *   <li>Asserts {@code active_use_case} is {@code UC-C} so the corrected
 *       smoke CaseSpec ({@code 570Q5000008u9gjIAA}) cannot regress.</li>
 * </ol>
 *
 * <p>Distress is intentionally NOT triggered on this case (the cs_014
 * seed messages are calm — pinned by
 * {@code Cs014RouteAndDistressRegressionTest}); the bot reaches handover
 * via the FAQ-miss path, so the LLM emits
 * {@code request_handover{escalation_reason=faq_miss_threshold_exceeded}}.
 * The resolver canonicalises that and the {@code §B0} normaliser keeps
 * the persisted tool-call argument in lockstep with
 * {@code session.escalationReason} and the
 * {@link HandoverPayloadAssembler}-built payload.
 */
@ExtendWith(MockitoExtension.class)
class Cs014RouteAndLoopHandoverIntegrationTest {

    /** Verbatim cs_interactive_014 form context (mirrors the smoke spec). */
    private static final String CS014_TOPIC = "Replies & Messaging";
    private static final String CS014_DESCRIPTION =
            "Hi. I am getting no response from sellers when I contact them. "
                    + "My account has 2 email addresses and I'm wondering if it's "
                    + "causing some issues in sending or receiving messages.";
    private static final String CS014_FORM_CONTEXT_JSON =
            "{\"first_name\":\"Paul\","
                    + "\"email\":\"customer@example.com\","
                    + "\"topic_subject\":\"Replies & Messaging\","
                    + "\"ad_id\":\"\","
                    + "\"description\":\""
                    + "Hi. I am getting no response from sellers when I contact them. "
                    + "My account has 2 email addresses and I'm wondering if it's "
                    + "causing some issues in sending or receiving messages.\"}";

    /** cs_014 follow-up user turn. Calm rhetorical complaint — must NOT
     *  trigger distress detection (pinned by the helper test). */
    private static final String CS014_FOLLOWUP_TURN =
            "How come an administrator can't atleast make the contact email "
                    + "match the main account email?";

    @Mock private BotTurnRepository turnRepository;
    @Mock private BotEventRepository eventRepository;
    @Mock private BudgetChecker budgetChecker;
    @Mock private DriftDetector driftDetector;
    @Mock private ControlPolicyService controlPolicy;
    @Mock private CreateCaseControlledTool createCaseTool;
    @Mock private EventEmitter eventEmitter;
    @Mock private ContextProjectionBuilder contextProjectionBuilder;
    @Mock private com.gumtree.csagent.service.runtime.LlmInvocationService llmInvocation;
    @Mock private ToolDispatcher toolDispatcher;
    @Mock private ActionParser actionParser;
    @Mock private com.gumtree.csagent.service.runtime.UseCaseRegistryService useCaseRegistry;
    @Mock private com.gumtree.csagent.service.knowledge.KnowledgeSearchService knowledgeSearchService;
    @Mock private com.gumtree.csagent.service.guardrails.ScriptLibraryService scriptLibrary;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final EscalationReasonResolver escalationResolver = new EscalationReasonResolver();

    private ControlKernel controlKernel;
    private HandoverPayloadAssembler handoverPayloadAssembler;

    @BeforeEach
    void setUp() {
        PhaseEvaluator phaseEvaluator = new PhaseEvaluator(
                useCaseRegistry, knowledgeSearchService, scriptLibrary,
                llmInvocation, contextProjectionBuilder, actionParser,
                objectMapper, createCaseTool, eventEmitter, toolDispatcher);

        AgentRunLoop agentRunLoop = new AgentRunLoopImpl(
                llmInvocation, toolDispatcher, contextProjectionBuilder, actionParser, objectMapper);

        AgentRunLoopProperties props = new AgentRunLoopProperties();
        // RESOLVE_FAQ is the route key for cs_014 (UC-C in RESOLVE phase).
        props.setEnabledPhases(List.of("RESOLVE_FAQ"));

        controlKernel = new ControlKernel(
                turnRepository, eventRepository, budgetChecker, driftDetector,
                phaseEvaluator, controlPolicy, objectMapper, createCaseTool,
                eventEmitter, contextProjectionBuilder, props, agentRunLoop,
                escalationResolver);

        handoverPayloadAssembler = new HandoverPayloadAssembler(
                turnRepository, escalationResolver, objectMapper);
    }

    /**
     * Build a session that mirrors the cs_014 form context after
     * UseCaseRouter has committed UC-C (the strong-prior + B2 bias path
     * is pinned in unit tests; this test focuses on what happens once
     * UC-C is committed and the FAQ flow runs out of articles).
     */
    private BotSession buildCs014Session() {
        BotSession session = new BotSession();
        session.setSessionId("sess-cs014-routeloop-1");
        session.setCurrentPhase("RESOLVE");
        session.setActiveUseCase("UC-C");
        // Carry the UC-C strong-prior committed candidate so payload assembly
        // surfaces it without depending on a router roundtrip.
        session.setCandidateUseCases(new String[]{"UC-C", "UC-K", "UC-D"});
        session.setIntentConfidence(new java.math.BigDecimal("0.85"));
        session.setTotalBotTurns(0);
        session.setFormTopicSubject(CS014_TOPIC);
        session.setFormContext(CS014_FORM_CONTEXT_JSON);
        return session;
    }

    @Test
    void cs014_routeLoopReachesHandover_withConsistentFaqMissReason() throws Exception {
        // ── Arrange: cs_014 session in RESOLVE/UC-C; replay the calm
        // follow-up turn. The LLM (mocked) decides this is a FAQ miss
        // and emits request_handover with faq_miss_threshold_exceeded —
        // matching the corrected smoke CaseSpec for cs_014.
        BotSession session = buildCs014Session();

        when(budgetChecker.checkBudgets(any())).thenReturn(Optional.empty());
        when(driftDetector.detect(any(), anyString())).thenReturn(
                DriftResult.builder().type(DriftResult.DriftType.NONE).build());
        when(turnRepository.findBySessionIdOrderByTurnIndex(anyString()))
                .thenReturn(List.of());
        when(useCaseRegistry.getUseCase("UC-C")).thenReturn(
                new com.gumtree.csagent.service.runtime.UseCaseRegistryService.UseCaseDefinition(
                        "UC-C", "Messages & Replies",
                        List.of("Replies or Messaging"), "LOW", true, "FAQ"));
        when(contextProjectionBuilder.build(any(), any(), any(), anyString(), any()))
                .thenReturn("{\"phase\":\"RESOLVE\",\"use_case\":\"UC-C\"}");
        when(controlPolicy.isValidTransition("RESOLVE", "ESCALATE")).thenReturn(true);

        LlmResponse turnResp = LlmResponse.builder()
                .content("{\"user_message\":\"I'm sorry I cannot make that change "
                        + "from my side. Let me connect you with a specialist who can "
                        + "review the contact-email association.\","
                        + "\"tool_calls\":[{\"name\":\"request_handover\","
                        + "\"arguments\":{\"escalation_reason\":\"faq_miss_threshold_exceeded\"}}]}")
                .promptTokens(60).completionTokens(30).build();
        when(llmInvocation.invokeChat(anyString(), anyString(), anyString(), anyInt()))
                .thenReturn(turnResp);
        when(actionParser.parse(turnResp.getContent())).thenReturn(ParsedAction.builder()
                .toolCalls(List.of(ToolCall.builder()
                        .name("request_handover")
                        .arguments(Map.of("escalation_reason", "faq_miss_threshold_exceeded"))
                        .build()))
                .userMessage("I'm sorry I cannot make that change from my side. "
                        + "Let me connect you with a specialist who can review the "
                        + "contact-email association.")
                .build());
        when(toolDispatcher.validateAgainstPlan(any(), eq("request_handover")))
                .thenReturn(ToolResult.ok(null));
        when(toolDispatcher.dispatch(eq("request_handover"), any(), any()))
                .thenReturn(ToolResult.ok(Map.of("status", "queued")));

        // Capture the persisted turn so the handover payload assertion can
        // see the same tool_calls JSON the eval pipeline reads.
        List<BotTurn> savedTurns = new ArrayList<>();
        when(turnRepository.save(any(BotTurn.class))).thenAnswer((InvocationOnMock inv) -> {
            BotTurn t = inv.getArgument(0);
            savedTurns.add(t);
            return t;
        });

        // ── Act: drive the actual ControlKernel/AgentRunLoop path ──
        controlKernel.processMessage(session, CS014_FOLLOWUP_TURN);

        // ── Assertion 1: active_use_case is UC-C ──────────────────
        // The strong-prior + B2 bias committed UC-C upstream; the
        // route/loop path must not drift away from it on the
        // escalation turn.
        assertEquals("UC-C", session.getActiveUseCase(),
                "cs_014 must escalate as UC-C; any drift to UC-B / UC-D / "
                        + "UC-F here regresses the smoke CaseSpec contract.");

        // ── Assertion 2: session.escalationReason is the corrected reason ──
        // The LLM emitted faq_miss_threshold_exceeded and the resolver
        // accepts it (no higher-priority semantic reason was present:
        // distress did NOT fire on the calm cs_014 seed). This is the
        // canonical SEMANTIC reason that downstream surfaces must carry.
        assertEquals("faq_miss_threshold_exceeded", session.getEscalationReason(),
                "Session reason must be the corrected faq_miss_threshold_exceeded; "
                        + "user_distress must NOT win here because cs_014's calm "
                        + "seeds do not match DISTRESS_PATTERNS.");

        // Sanity: phase moved to ESCALATE and the trace was persisted.
        assertEquals("ESCALATE", session.getCurrentPhase(),
                "ControlKernel must transition to ESCALATE on the request_handover turn.");
        assertEquals(1, savedTurns.size(),
                "Exactly one BotTurn should be persisted for this turn.");

        // ── Assertion 3: persisted request_handover.arguments.escalation_reason ──
        // matches session.escalationReason. This is the §B0 normaliser
        // contract — without it, an LLM-emitted lower-priority literal
        // could leak into the trace and disagree with the session.
        BotTurn savedTurn = savedTurns.get(0);
        assertNotNull(savedTurn.getToolCalls(),
                "Tool calls JSONB must be persisted on escalation");
        JsonNode toolCalls = objectMapper.readTree(savedTurn.getToolCalls());
        assertTrue(toolCalls.isArray() && toolCalls.size() >= 1,
                "Persisted tool_calls must be a non-empty JSON array.");
        JsonNode handoverEntry = null;
        for (JsonNode entry : toolCalls) {
            if ("request_handover".equals(entry.path("tool_name").asText())) {
                handoverEntry = entry;
                break;
            }
        }
        assertNotNull(handoverEntry,
                "request_handover entry must be present in the persisted trace.");
        String persistedReason = handoverEntry.path("arguments").path("escalation_reason").asText();
        assertEquals(session.getEscalationReason(), persistedReason,
                "Persisted request_handover.arguments.escalation_reason must equal "
                        + "session.escalationReason — the L1 escalation_reason_consistency "
                        + "gate cannot be re-opened on the cs_014 path.");
        assertEquals("faq_miss_threshold_exceeded", persistedReason,
                "Persisted reason must be the canonical faq_miss_threshold_exceeded.");

        // ── Assertion 4: handover payload escalation_reason matches ──
        // The HandoverPayloadAssembler is the deterministic source of
        // the payload that lands in MockHandoverLog. It re-canonicalises
        // session.escalationReason via the same resolver, so the three
        // surfaces (session / persisted tool call / payload) MUST agree.
        Map<String, Object> payload =
                handoverPayloadAssembler.assemble(session, List.of(savedTurn));
        assertEquals("faq_miss_threshold_exceeded", payload.get("escalation_reason"),
                "Handover payload escalation_reason must equal session.escalationReason.");
        assertEquals(session.getEscalationReason(), payload.get("escalation_reason"),
                "Handover payload escalation_reason must equal session.escalationReason.");
        // Payload also surfaces the committed UC for the eval contract.
        assertEquals("UC-C", payload.get("primary_use_case"),
                "Handover payload primary_use_case must be UC-C for cs_014.");

        // ── Assertion 5: no L1 escalation_reason_consistency regression ──
        // This is the explicit gate the cs_014 P1 fix protects. Asserting
        // pairwise equality across all three surfaces fails the test if any
        // future change breaks the §B0 normalisation invariant.
        String sessionReason = session.getEscalationReason();
        assertEquals(sessionReason, persistedReason,
                "L1:escalation_reason_consistency: session vs persisted tool call.");
        assertEquals(sessionReason, payload.get("escalation_reason"),
                "L1:escalation_reason_consistency: session vs handover payload.");
        assertEquals(persistedReason, payload.get("escalation_reason"),
                "L1:escalation_reason_consistency: persisted tool call vs handover payload.");

        // The escalation event was emitted with the canonical session reason
        // (post-resolver), so the event stream agrees with the other surfaces.
        verify(eventEmitter, atLeastOnce()).emitEscalationRequested(
                eq(session.getSessionId()), anyInt(), eq("faq_miss_threshold_exceeded"));

        // ArgumentCaptor sanity: the saved turn's phaseAfter field is ESCALATE.
        ArgumentCaptor<BotTurn> turnCaptor = ArgumentCaptor.forClass(BotTurn.class);
        verify(turnRepository).save(turnCaptor.capture());
        assertEquals("ESCALATE", turnCaptor.getValue().getPhaseAfter(),
                "Persisted BotTurn.phaseAfter must record the ESCALATE transition.");
    }
}
