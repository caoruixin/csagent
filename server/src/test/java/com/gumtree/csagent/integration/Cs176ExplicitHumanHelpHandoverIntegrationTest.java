package com.gumtree.csagent.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gumtree.csagent.config.AgentRunLoopProperties;
import com.gumtree.csagent.model.BotSession;
import com.gumtree.csagent.model.BotTurn;
import com.gumtree.csagent.model.DriftResult;
import com.gumtree.csagent.repository.BotEventRepository;
import com.gumtree.csagent.repository.BotTurnRepository;
import com.gumtree.csagent.service.observability.EventEmitter;
import com.gumtree.csagent.service.runtime.AgentRunLoop;
import com.gumtree.csagent.service.runtime.BudgetChecker;
import com.gumtree.csagent.service.runtime.ContextProjectionBuilder;
import com.gumtree.csagent.service.runtime.ControlKernel;
import com.gumtree.csagent.service.runtime.ControlPolicyService;
import com.gumtree.csagent.service.runtime.DriftDetector;
import com.gumtree.csagent.service.runtime.EscalationReasonResolver;
import com.gumtree.csagent.service.runtime.PhaseEvaluator;
import com.gumtree.csagent.service.tools.CreateCaseControlledTool;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Sprint 6 §G1 closure regression — focused
 * {@code cs_interactive_176} runtime evidence for {@code user_requested}.
 *
 * <p>Codex Sprint 6 review flagged that the §G1 prompt-snapshot test
 * pins the corrected language but no runtime/eval evidence shows
 * that an explicit human-help cue produces or preserves
 * {@code escalation_reason=user_requested}. This focused test fills
 * that gap by forcing the cs176 explicit human-help seed into the
 * transcript at the {@link ControlKernel#processMessage} surface
 * (rather than depending on the persona simulator paraphrasing or
 * progressing through the seed list during a live eval).
 *
 * <p>Contract pinned by this regression:
 * <ol>
 *   <li>The first persisted {@code request_handover} entry's
 *       {@code arguments.escalation_reason} is {@code user_requested}.</li>
 *   <li>{@code session.escalationReason} is {@code user_requested}.</li>
 *   <li>None of the four forbidden substitutes
 *       ({@code service_degraded},
 *       {@code intake_complete_for_uc_k},
 *       {@code payment_dispute_detected},
 *       {@code faq_miss_threshold_exceeded}) win on this turn — they
 *       are NOT family-match against {@code user_requested} (Sprint
 *       5.1 codex correction) and the deterministic precedence path
 *       must keep the priority-1 reason.</li>
 * </ol>
 *
 * <p>Note: cs176 r2 UC-I drift remains an explicitly deferred
 * residual risk per the Sprint 6 acceptance condition. This test
 * pins reason selection on the explicit-human-help cue only; UC
 * drift on the same case is not in scope.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class Cs176ExplicitHumanHelpHandoverIntegrationTest {

    /** cs_interactive_176 form context — verbatim from the smoke spec. */
    private static final String CS176_TOPIC = "Technical Support";
    private static final String CS176_FORM_CONTEXT_JSON =
            "{\"first_name\":\"Gary\","
                    + "\"email\":\"customer@example.com\","
                    + "\"topic_subject\":\"Technical Support\","
                    + "\"ad_id\":\"\","
                    + "\"description\":\""
                    + "I pad to put my ad for my business and it's not at the top\"}";

    /** cs_interactive_176 persona seed message #2 — the explicit
     *  human-help cue that the live persona simulator does not
     *  reliably reach (Codex Sprint 6 review evidence). Using this
     *  literal text guarantees the test exercises the
     *  explicit-human-help code path. */
    private static final String CS176_EXPLICIT_HUMAN_HELP_MESSAGE =
            "What about giving a phone number to talk to someone";

    /** Forbidden substitutes from the H1 / Sprint 5.1 codex
     *  correction. The regression hard-fails if any of these is
     *  picked over {@code user_requested} on the explicit
     *  human-help turn. */
    private static final Set<String> FORBIDDEN_SUBSTITUTES = Set.of(
            "service_degraded",
            "intake_complete_for_uc_k",
            "payment_dispute_detected",
            "faq_miss_threshold_exceeded"
    );

    @Mock private BotTurnRepository turnRepository;
    @Mock private BotEventRepository eventRepository;
    @Mock private BudgetChecker budgetChecker;
    @Mock private DriftDetector driftDetector;
    @Mock private PhaseEvaluator phaseEvaluator;
    @Mock private ControlPolicyService controlPolicy;
    @Mock private CreateCaseControlledTool createCaseTool;
    @Mock private EventEmitter eventEmitter;
    @Mock private ContextProjectionBuilder contextProjectionBuilder;
    @Mock private AgentRunLoop agentRunLoop;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final EscalationReasonResolver escalationResolver = new EscalationReasonResolver();

    private ControlKernel controlKernel;

    @BeforeEach
    void setUp() {
        AgentRunLoopProperties props = new AgentRunLoopProperties();
        // RESOLVE_FAQ is the route key that would normally drive UC-E /
        // UC-B. We never reach the agent loop on this turn — the
        // explicit-human-help cue at Step 2.5 short-circuits via
        // forceEscalate — but we still wire an enabled phase so the
        // ControlKernel constructor is exercised in the same shape as
        // production.
        props.setEnabledPhases(List.of("RESOLVE_FAQ"));
        controlKernel = new ControlKernel(
                turnRepository, eventRepository, budgetChecker, driftDetector,
                phaseEvaluator, controlPolicy, objectMapper, createCaseTool,
                eventEmitter, contextProjectionBuilder, props, agentRunLoop,
                escalationResolver);
    }

    private BotSession buildCs176Session() {
        BotSession session = new BotSession();
        session.setSessionId("sess-cs176-explicit-help-1");
        session.setCurrentPhase("RESOLVE");
        // cs176 classifies to UC-E (paid feature / Top-Ad) — routing
        // is upstream of this test. Set it directly so forceEscalate
        // skips inferFallbackUseCase and the active UC carried into
        // the persisted turn matches the spec.
        session.setActiveUseCase("UC-E");
        session.setCandidateUseCases(new String[]{"UC-E", "UC-B", "UC-K"});
        session.setIntentConfidence(new java.math.BigDecimal("0.80"));
        session.setTotalBotTurns(0);
        session.setFormTopicSubject(CS176_TOPIC);
        session.setFormContext(CS176_FORM_CONTEXT_JSON);
        return session;
    }

    /**
     * Primary §G1 closure assertion: the first persisted
     * {@code request_handover} on the explicit-human-help turn
     * carries {@code escalation_reason=user_requested}, and none of
     * the four forbidden substitutes wins.
     */
    @Test
    void cs176_explicitHumanHelpCue_yieldsUserRequestedHandover() throws Exception {
        BotSession session = buildCs176Session();

        // Step-2.5 forceEscalate path needs ESCALATE to be a valid
        // transition out of RESOLVE.
        when(controlPolicy.isValidTransition("RESOLVE", "ESCALATE")).thenReturn(true);
        when(budgetChecker.checkBudgets(any())).thenReturn(Optional.empty());
        when(driftDetector.detect(any(), anyString())).thenReturn(
                DriftResult.builder().type(DriftResult.DriftType.NONE).build());
        when(turnRepository.findBySessionIdOrderByTurnIndex(anyString()))
                .thenReturn(List.of());

        // Capture the persisted BotTurn(s) so we can inspect tool_calls.
        List<BotTurn> savedTurns = new ArrayList<>();
        when(turnRepository.save(any(BotTurn.class))).thenAnswer((InvocationOnMock inv) -> {
            BotTurn t = inv.getArgument(0);
            savedTurns.add(t);
            return t;
        });

        // ── Act: force the cs176 explicit human-help cue into the
        // transcript at the same surface the live runtime sees. ──
        controlKernel.processMessage(session, CS176_EXPLICIT_HUMAN_HELP_MESSAGE);

        // ── Assertion 1: session reason is user_requested. ────────
        assertEquals("user_requested", session.getEscalationReason(),
                "cs176 §G1 closure: an explicit human-help cue must produce "
                        + "session.escalationReason=user_requested via the "
                        + "deterministic Step 2.5 / EscalationReasonResolver "
                        + "precedence path.");

        // The reason must NOT be one of the four forbidden substitutes
        // (Sprint 5.1 codex correction). These are NOT family-match
        // against user_requested.
        for (String forbidden : FORBIDDEN_SUBSTITUTES) {
            assertNotEquals(forbidden, session.getEscalationReason(),
                    "cs176 §G1 closure: forbidden substitute '" + forbidden
                            + "' must NOT replace user_requested on the "
                            + "explicit-human-help turn.");
        }

        // ── Assertion 2: phase moved to ESCALATE and exactly one
        // BotTurn was persisted on this turn. ─────────────────────
        assertEquals("ESCALATE", session.getCurrentPhase(),
                "ControlKernel must transition to ESCALATE on the "
                        + "explicit-human-help turn.");
        assertEquals(1, savedTurns.size(),
                "Exactly one BotTurn should be persisted for this turn.");

        // ── Assertion 3: the first persisted request_handover entry
        // carries escalation_reason=user_requested. ───────────────
        BotTurn savedTurn = savedTurns.get(0);
        assertNotNull(savedTurn.getToolCalls(),
                "Tool calls JSONB must be persisted on escalation");
        JsonNode toolCalls = objectMapper.readTree(savedTurn.getToolCalls());
        assertTrue(toolCalls.isArray() && toolCalls.size() >= 1,
                "Persisted tool_calls must be a non-empty JSON array.");

        JsonNode firstHandover = null;
        for (JsonNode entry : toolCalls) {
            if ("request_handover".equals(entry.path("tool_name").asText())) {
                firstHandover = entry;
                break;
            }
        }
        assertNotNull(firstHandover,
                "First persisted tool_calls entry of name request_handover "
                        + "must be present on the explicit-human-help turn.");

        String firstReason = firstHandover.path("arguments")
                .path("escalation_reason").asText();
        assertEquals("user_requested", firstReason,
                "cs176 §G1 closure: the FIRST request_handover reason on "
                        + "the explicit-human-help turn must be "
                        + "user_requested. Service_degraded / "
                        + "intake_complete_for_uc_k / payment_dispute_detected "
                        + "/ faq_miss_threshold_exceeded are explicitly "
                        + "forbidden substitutes (Sprint 5.1 codex "
                        + "correction).");

        for (String forbidden : FORBIDDEN_SUBSTITUTES) {
            assertNotEquals(forbidden, firstReason,
                    "cs176 §G1 closure: persisted request_handover reason "
                            + "must NOT be the forbidden substitute '"
                            + forbidden + "'.");
        }
    }

    /**
     * Defensive guard: the four forbidden substitutes must lose to
     * {@code user_requested} via the resolver's deterministic
     * precedence table. Combined with the primary test above, this
     * keeps the §G1 contract green even if a future refactor moves
     * the explicit-human-help detection out of the early-return
     * path.
     */
    @Test
    void resolverPrecedence_userRequestedBeatsAllForbiddenSubstitutes() {
        for (String forbidden : FORBIDDEN_SUBSTITUTES) {
            // Existing reason is the forbidden substitute, candidate
            // is user_requested -> user_requested wins.
            assertEquals("user_requested",
                    escalationResolver.resolve(forbidden, "user_requested"),
                    "user_requested (priority 1) must beat '" + forbidden
                            + "' on resolve(existing=forbidden, candidate=user_requested).");
            // And vice-versa: existing user_requested beats a later
            // forbidden candidate.
            assertEquals("user_requested",
                    escalationResolver.resolve("user_requested", forbidden),
                    "user_requested (priority 1) must beat '" + forbidden
                            + "' on resolve(existing=user_requested, candidate=forbidden).");
        }
    }
}
