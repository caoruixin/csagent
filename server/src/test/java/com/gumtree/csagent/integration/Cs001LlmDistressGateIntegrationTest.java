package com.gumtree.csagent.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gumtree.csagent.config.AgentRunLoopProperties;
import com.gumtree.csagent.model.AgentRunResult;
import com.gumtree.csagent.model.BotSession;
import com.gumtree.csagent.model.DriftResult;
import com.gumtree.csagent.model.PhasePlan;
import com.gumtree.csagent.model.PhaseTransitionDecision;
import com.gumtree.csagent.model.TerminalOutcome;
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
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Sprint 4 §E1 regression: gate the bot LLM's
 * {@code user_distress} claim on a deterministic B1 signal.
 *
 * <p>cs_interactive_001's persona seeds (mild confusion about two
 * email addresses) do not trigger {@link EscalationReasonResolver#detectDistressSignal(String)};
 * yet the Sprint 3 r1 baseline (`20260504-191137`) shows the bot LLM
 * picking {@code user_distress} as the {@code request_handover} reason
 * anyway. Without a deterministic B1 hit on this session, that claim
 * is downgraded to {@code faq_miss_threshold_exceeded} — same
 * {@code bot_limit} family as cs_001's spec
 * ({@code clarification_budget_exhausted}), so the L1
 * {@code escalation_compliance} gate stops cross-family-failing on
 * an LLM-only distress label.
 *
 * <p>The contract preserved by these tests:
 * <ul>
 *   <li>Sprint §B1 still fires deterministically: cs_002-style seeds
 *       ("How long do I have to wait", "since day 1", ALL-CAPS) keep
 *       producing {@code user_distress} on the session.</li>
 *   <li>{@code user_requested} (priority 1) still beats both
 *       {@code user_distress} (priority 2) and the gate downgrade
 *       (priority 41) — cs_029 contract.</li>
 *   <li>Re-asserting an already-set {@code user_distress} is a no-op:
 *       the LLM may freely re-emit it after B1 fired earlier in the
 *       session.</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class Cs001LlmDistressGateIntegrationTest {

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
    private final EscalationReasonResolver resolver = new EscalationReasonResolver();

    private ControlKernel controlKernel;

    @BeforeEach
    void setUp() {
        AgentRunLoopProperties props = new AgentRunLoopProperties();
        props.setEnabledPhases(List.of("RESOLVE_FAQ"));
        controlKernel = new ControlKernel(
                turnRepository, eventRepository, budgetChecker, driftDetector,
                phaseEvaluator, controlPolicy, objectMapper, createCaseTool,
                eventEmitter, contextProjectionBuilder, props, agentRunLoop, resolver);
    }

    private BotSession baseSession() {
        BotSession s = new BotSession();
        s.setSessionId("sess-cs001-distress-gate");
        s.setCurrentPhase("RESOLVE");
        s.setActiveUseCase("UC-C");
        s.setTotalBotTurns(0);
        s.setFormContext("{\"description\":\"I am unable to receive messages on my account\"}");
        return s;
    }

    private PhasePlan dummyFaqPlan() {
        return PhasePlan.builder()
                .phase("RESOLVE_FAQ")
                .useCase("UC-C")
                .objective("FAQ_GROUNDING")
                .allowedTools(List.of("search_knowledge", "resolve_article",
                        "request_handover", "record_outcome"))
                .maxToolSteps(3)
                .validTerminalOutcomes(java.util.Set.of(
                        TerminalOutcome.FINAL_ANSWER, TerminalOutcome.ESCALATE))
                .systemInstruction("system")
                .groundingInstruction("grounding")
                .escalationPolicy("escalation")
                .build();
    }

    private void wireFaqAgentLoopThatEmitsHandoverWith(String llmReason) {
        when(controlPolicy.isValidTransition(anyString(), anyString())).thenReturn(true);
        when(driftDetector.detect(any(), anyString())).thenReturn(
                DriftResult.builder().type(DriftResult.DriftType.NONE).build());
        when(turnRepository.findBySessionIdOrderByTurnIndex(anyString()))
                .thenReturn(List.of());
        when(budgetChecker.checkBudgets(any())).thenReturn(Optional.empty());

        PhasePlan plan = dummyFaqPlan();
        when(phaseEvaluator.plan(any(), anyString(), any())).thenReturn(plan);
        AgentRunResult runResult = AgentRunResult.escalate(
                llmReason, List.of(), List.of());
        when(agentRunLoop.run(any(), any(), anyString(), any())).thenReturn(runResult);
        // The agent loop "decision" returned by interpretRunResult is what
        // ControlKernel passes through applyEscalationReason — i.e. the
        // path the LLM-supplied reason flows through.
        when(phaseEvaluator.interpretRunResult(any(), any(), any())).thenReturn(
                new PhaseTransitionDecision(
                        "ESCALATE",
                        "Let me connect you with a specialist.",
                        llmReason,
                        "agent_loop_escalate"));
    }

    /** cs_001 r1 shape: bot LLM picks {@code user_distress} on calm seeds. */
    @Test
    void cs001_llmEmitsUserDistressOnCalmSeeds_isDowngradedToFaqMiss() {
        BotSession s = baseSession();
        wireFaqAgentLoopThatEmitsHandoverWith("user_distress");

        // cs_001's actual replayed turn 1 user message — calm/mild,
        // no DISTRESS_PATTERN hit, no ALL-CAPS shout.
        controlKernel.processMessage(s,
                "I am confused i see you guys both of my email but when "
                        + "i log in i use bensmith@example.com. it's really confused me");

        assertEquals("faq_miss_threshold_exceeded", s.getEscalationReason(),
                "Sprint §E1: an LLM-supplied user_distress without a "
                        + "deterministic B1 hit must be downgraded into the "
                        + "bot_limit family so cs_001's L1 escalation_compliance "
                        + "(spec=clarification_budget_exhausted, family=bot_limit) "
                        + "no longer cross-family-fails on the LLM's subjective "
                        + "distress label.");
        assertNotEquals("user_distress", s.getEscalationReason());
    }

    /** cs_002 r1 shape: B1 fires AND LLM also picks user_distress. */
    @Test
    void cs002_b1FiresThenLlmReasserts_keepsUserDistress() {
        BotSession s = baseSession();
        wireFaqAgentLoopThatEmitsHandoverWith("user_distress");

        // cs_002 seed message: deterministic B1 hit ("How long do I have
        // to wait", "since day 1") fires step 2.4 → applyDeterministicDistressReason
        // → session.escalationReason = "user_distress" BEFORE the agent
        // loop ESCALATE branch runs. The LLM's own user_distress claim
        // is then a re-assertion, so the gate is satisfied and the
        // canonical reason stays user_distress.
        controlKernel.processMessage(s,
                "How long do I have to wait to sort this out? Its been this way since day 1.");

        assertEquals("user_distress", s.getEscalationReason(),
                "Sprint §B1 contract: a deterministic B1 hit must keep "
                        + "user_distress on the session even after the gate "
                        + "in applyEscalationReason runs.");
    }

    /** cs_029 contract: user_requested still beats both B1 and the gate. */
    @Test
    void cs029_explicitCallbackBeatsLlmDistressAndB1() {
        BotSession s = baseSession();
        // cs_029 turn-2-shape: ALL-CAPS shout (B1 fires) + explicit
        // callback request ("can you please call me now?"). Step 2.5
        // forceEscalates with user_requested before the agent loop runs.
        when(controlPolicy.isValidTransition(anyString(), anyString())).thenReturn(true);
        when(driftDetector.detect(any(), anyString())).thenReturn(
                DriftResult.builder().type(DriftResult.DriftType.NONE).build());
        when(turnRepository.findBySessionIdOrderByTurnIndex(anyString()))
                .thenReturn(List.of());
        when(budgetChecker.checkBudgets(any())).thenReturn(Optional.empty());

        controlKernel.processMessage(s,
                "Hi, my Gumtree business account is locked and I need it "
                        + "unlocked urgently to resume advertising. I've "
                        + "already submitted a form but haven't heard back "
                        + "— can you please call me now?");

        assertEquals("user_requested", s.getEscalationReason(),
                "cs_029 contract: an explicit callback / human request "
                        + "(priority 1) must outrank both the deterministic "
                        + "B1 stamp (priority 2) and any LLM-supplied "
                        + "user_distress.");
    }

    /** cs_014 shape regression: LLM picks user_distress on calm seeds. */
    @Test
    void cs014_llmEmitsUserDistressOnCalmSeeds_isDowngradedToFaqMiss() {
        BotSession s = baseSession();
        s.setFormContext("{\"description\":\"Hi. I am getting no response from sellers when I contact them.\"}");
        wireFaqAgentLoopThatEmitsHandoverWith("user_distress");

        // cs_014's actual replayed seed message — calm/cooperative,
        // no DISTRESS_PATTERN hit, no ALL-CAPS shout. The cs_014
        // expected.escalation_trigger is faq_miss_threshold_exceeded
        // (Sprint 2.1 P1 override path), so the gate's downgrade
        // matches the override target without any change to the
        // override registry.
        controlKernel.processMessage(s,
                "I'd be happy enough if the contact email address was "
                        + "reverted to the original account address");

        assertEquals("faq_miss_threshold_exceeded", s.getEscalationReason(),
                "Sprint §E1 + Sprint 2.1 cs_014 override: a calm/cooperative "
                        + "seed must not be mislabeled as user_distress just "
                        + "because the LLM picked it.");
    }

    /** Negative regression: an LLM picking a non-distress reason is unaffected. */
    @Test
    void llmPicksFaqMissDirectly_passesThroughUnchanged() {
        BotSession s = baseSession();
        wireFaqAgentLoopThatEmitsHandoverWith("faq_miss_threshold_exceeded");

        controlKernel.processMessage(s, "I am confused about my email setup");

        assertEquals("faq_miss_threshold_exceeded", s.getEscalationReason(),
                "Gate must not affect non-user_distress candidates.");
    }

    /** Negative regression: user_requested passes through unchanged. */
    @Test
    void llmPicksUserRequestedDirectly_passesThroughUnchanged() {
        BotSession s = baseSession();
        wireFaqAgentLoopThatEmitsHandoverWith("user_requested");

        controlKernel.processMessage(s, "I am confused about my email setup");

        assertEquals("user_requested", s.getEscalationReason(),
                "Gate must not affect user_requested (priority 1).");
    }
}
