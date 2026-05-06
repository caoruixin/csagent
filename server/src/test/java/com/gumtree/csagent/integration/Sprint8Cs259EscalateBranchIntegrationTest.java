package com.gumtree.csagent.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gumtree.csagent.config.AgentRunLoopProperties;
import com.gumtree.csagent.model.AgentRunResult;
import com.gumtree.csagent.model.BotSession;
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
import com.gumtree.csagent.model.PhasePlan;
import com.gumtree.csagent.model.PhaseTransitionDecision;
import com.gumtree.csagent.service.runtime.PhaseEvaluator;
import com.gumtree.csagent.service.tools.CreateCaseControlledTool;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Sprint 8 §K0 integration regression: cs_interactive_259 r2 shape.
 *
 * <p>Pin the contract that when the {@code AgentRunLoop} ESCALATE
 * branch in {@link ControlKernel#processMessage} fires for a session
 * with no committed {@code activeUseCase} (the cs259 r2 failure mode
 * — the LLM made two {@code search_knowledge} calls, returned no
 * viable hits, and emitted {@code request_handover(faq_miss_threshold_exceeded)}
 * without ever calling {@code classify_use_case}), the runtime
 * commits the deterministic UC-F fallback BEFORE the persisted
 * tool-call trace is written, so the eval trace contract validator
 * does NOT raise {@code CONTRACT_VIOLATION:active_use_case}.
 *
 * <p>Counter-test: a session that already has {@code activeUseCase}
 * committed when entering the same branch is left alone — the
 * fallback never overrides an LLM-classified or deterministically-routed
 * UC.
 */
@ExtendWith(MockitoExtension.class)
class Sprint8Cs259EscalateBranchIntegrationTest {

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
        // Match production default — DISCOVER must be enabled for the
        // cs259-shape route key to engage the AgentRunLoop branch.
        props.setEnabledPhases(List.of(
                "RESOLVE_FAQ", "RESOLVE_INTAKE", "DISCOVER", "CONFIRM", "CLOSE", "ESCALATE"));

        controlKernel = new ControlKernel(
                turnRepository, eventRepository, budgetChecker, driftDetector,
                phaseEvaluator, controlPolicy, objectMapper, createCaseTool,
                eventEmitter, contextProjectionBuilder, props, agentRunLoop,
                escalationResolver);
    }

    private BotSession cs259ShapeSession() {
        // cs_interactive_259.yaml form_context: UNKNOWN topic, empty
        // description, ad_id empty. The persona's first user message
        // is the seed "How do I receive the payment when I sell an
        // item" (verbatim).
        BotSession s = new BotSession();
        s.setSessionId("sess-cs259-k0");
        s.setCurrentPhase("DISCOVER");
        s.setActiveUseCase(null);
        s.setTotalBotTurns(0);
        s.setFormTopicSubject("UNKNOWN");
        s.setFormContext("{\"first_name\":\"Lorraine\",\"email\":\"customer@example.com\","
                + "\"topic_subject\":\"UNKNOWN\",\"ad_id\":\"\",\"description\":\"\"}");
        return s;
    }

    private void mockCommonStubs() {
        when(budgetChecker.checkBudgets(any())).thenReturn(Optional.empty());
        when(driftDetector.detect(any(), anyString())).thenReturn(
                DriftResult.builder().type(DriftResult.DriftType.NONE).build());
        when(turnRepository.findBySessionIdOrderByTurnIndex(anyString()))
                .thenReturn(List.of());

        // Stub PhasePlan + PhaseTransitionDecision directly so the
        // AgentRunLoop is exercised through processMessage's
        // ESCALATE branch without spinning up the real DISCOVER plan
        // builder. This isolates the K0 contract surface.
        PhasePlan plan = PhasePlan.builder()
                .phase("DISCOVER")
                .useCase(null)
                .objective("Identify the user's use case")
                .allowedTools(List.of("search_knowledge", "classify_use_case"))
                .maxToolSteps(2)
                .systemInstruction("Cs259 K0 stub plan")
                .validTerminalOutcomes(Set.of(
                        com.gumtree.csagent.model.TerminalOutcome.ESCALATE,
                        com.gumtree.csagent.model.TerminalOutcome.FINAL_ANSWER,
                        com.gumtree.csagent.model.TerminalOutcome.CLARIFICATION_NEEDED))
                .build();
        when(phaseEvaluator.plan(any(), anyString(), any())).thenReturn(plan);

        // The LLM ran two search_knowledge calls (cs259 r2 lastAction)
        // and emitted request_handover(faq_miss_threshold_exceeded)
        // WITHOUT classify_use_case. Use the AgentRunResult.escalate
        // record-static constructor to model that terminal outcome.
        AgentRunResult runResult = AgentRunResult.escalate(
                "faq_miss_threshold_exceeded",
                List.of(),
                List.of(),
                "{\"phase\":\"DISCOVER\"}",
                "{\"tool_calls\":[]}");
        when(agentRunLoop.run(any(), any(), anyString(), any())).thenReturn(runResult);

        // PhaseTransitionDecision is a record — instantiate directly.
        PhaseTransitionDecision decision = new PhaseTransitionDecision(
                "ESCALATE",
                "Let me connect you with a specialist.",
                "faq_miss_threshold_exceeded",
                "agent_loop_escalate");
        when(phaseEvaluator.interpretRunResult(eq(plan), eq(runResult), any()))
                .thenReturn(decision);

        when(controlPolicy.isValidTransition("DISCOVER", "ESCALATE")).thenReturn(true);
    }

    @Test
    void cs259_escalateBranch_withMissingUc_commitsUcFFallback() {
        // ── Arrange: cs259 r2 shape — no UC committed before the
        // AgentRunLoop ESCALATE branch fires.
        BotSession session = cs259ShapeSession();
        mockCommonStubs();

        // ── Act ──────────────────────────────────────────────────
        controlKernel.processMessage(session,
                "How do I receive the payment when I sell an item");

        // ── Assert: §K0 contract holds ───────────────────────────
        assertNotNull(session.getActiveUseCase(),
                "AgentRunLoop ESCALATE branch must commit a fallback UC "
                        + "when the LLM did not call classify_use_case before "
                        + "request_handover. Without this, the trace contract "
                        + "validator raises CONTRACT_VIOLATION:active_use_case.");
        assertEquals("UC-F", session.getActiveUseCase(),
                "cs259 payment / sell-an-item shape must resolve to UC-F "
                        + "via the inferFallbackUseCase regex (the user message "
                        + "matches both 'payment' and 'sell').");

        // The fallback also seeds candidate_use_cases when empty — no
        // override of pre-existing candidates happens here because the
        // session entered with an empty array.
        assertEquals(1, session.getCandidateUseCases().length);
        assertEquals("UC-F", session.getCandidateUseCases()[0]);

        // The semantic escalation reason is unaffected — the fallback
        // only fills the missing UC slot. A cs259 FAQ-miss handover
        // with no viable search hits legitimately escalates as
        // faq_miss_threshold_exceeded.
        assertEquals("faq_miss_threshold_exceeded", session.getEscalationReason(),
                "Escalation reason must remain faq_miss_threshold_exceeded; "
                        + "the K0 fallback only fills the active_use_case slot.");

        // Phase transitioned to ESCALATE / handlingState QUEUE_TO_HUMAN /
        // containmentOutcome escalated — the rest of the ESCALATE branch
        // ran normally.
        assertEquals("ESCALATE", session.getCurrentPhase());
        assertEquals("QUEUE_TO_HUMAN", session.getHandlingState());
        assertEquals("escalated", session.getContainmentOutcome());

        // No runtime case is created for UC-F (case creation is gated
        // to UC-H/J/K only). The fallback stays in its lane.
        assertNull(session.getCaseId());
    }

    @Test
    void cs259ShapeSession_withCommittedUc_isNotOverwritten() {
        // ── Arrange: same cs259 shape but the LLM (in some hypothetical
        // future run) DID call classify_use_case(UC-K) before handover.
        // The K0 fallback must not overwrite the committed UC.
        BotSession session = cs259ShapeSession();
        session.setActiveUseCase("UC-K");
        session.setIntentConfidence(new java.math.BigDecimal("0.85"));
        mockCommonStubs();

        controlKernel.processMessage(session,
                "How do I receive the payment when I sell an item");

        assertEquals("UC-K", session.getActiveUseCase(),
                "K0 fallback must never overwrite an already-committed UC. "
                        + "The user message contains UC-F vocabulary, but the "
                        + "LLM-classified UC-K wins.");
        // Confidence preserved.
        assertEquals(0,
                new java.math.BigDecimal("0.85").compareTo(session.getIntentConfidence()),
                "intentConfidence must not be downgraded by the K0 fallback "
                        + "when the UC was already committed.");
    }

    // ─────────────────────────────────────────────────────────────
    // §K0 narrowing — gate fallback on AgentRunResult evidence so
    // service_degraded paths don't get masked with a regex UC.
    // Anchored on the live trace 6cd5320f-1758-4f3a-affe-51adf9b7ba36
    // observed on 2026-05-06: no LLM call recorded, 0 tool calls,
    // 12 320 ms elapsed, AgentRunLoop returned ERROR with empty
    // events; pre-narrowing K0 stamped UC-A from the regex match
    // on "ad" in the user message + form description, hiding the
    // upstream Kimi-call failure. The narrowing must leave
    // activeUseCase null in that path so service_degraded surfaces
    // are visible to operators and to the eval contract validator.
    // ─────────────────────────────────────────────────────────────

    private void mockCommonStubsWithRunResult(AgentRunResult runResult,
                                               PhaseTransitionDecision decision) {
        when(budgetChecker.checkBudgets(any())).thenReturn(Optional.empty());
        when(driftDetector.detect(any(), anyString())).thenReturn(
                DriftResult.builder().type(DriftResult.DriftType.NONE).build());
        when(turnRepository.findBySessionIdOrderByTurnIndex(anyString()))
                .thenReturn(List.of());
        PhasePlan plan = PhasePlan.builder()
                .phase("DISCOVER").useCase(null)
                .objective("Identify the user's use case")
                .allowedTools(List.of("search_knowledge", "classify_use_case"))
                .maxToolSteps(2).systemInstruction("Cs259 K0-narrowing stub")
                .validTerminalOutcomes(Set.of(
                        com.gumtree.csagent.model.TerminalOutcome.ESCALATE,
                        com.gumtree.csagent.model.TerminalOutcome.FINAL_ANSWER,
                        com.gumtree.csagent.model.TerminalOutcome.CLARIFICATION_NEEDED,
                        com.gumtree.csagent.model.TerminalOutcome.ERROR))
                .build();
        when(phaseEvaluator.plan(any(), anyString(), any())).thenReturn(plan);
        when(agentRunLoop.run(any(), any(), anyString(), any())).thenReturn(runResult);
        when(phaseEvaluator.interpretRunResult(any(), eq(runResult), any()))
                .thenReturn(decision);
        when(controlPolicy.isValidTransition("DISCOVER", "ESCALATE")).thenReturn(true);
    }

    private static PhaseTransitionDecision serviceDegradedDecision() {
        return new PhaseTransitionDecision(
                "ESCALATE",
                "I'm experiencing a technical issue. Let me connect you with a specialist.",
                "service_degraded",
                "agent_error");
    }

    private static PhaseTransitionDecision faqMissDecision() {
        return new PhaseTransitionDecision(
                "ESCALATE",
                "Let me connect you with a specialist.",
                "faq_miss_threshold_exceeded",
                "agent_escalated");
    }

    @Test
    void llmCallFailed_emptyEvents_errorOutcome_skipsFallback_leavesUcNull() {
        // ── Arrange: live-trace shape — Kimi chat call threw a
        // timeout; AgentRunLoopImpl line 165–168 returned
        // AgentRunResult.error("llm_invocation_failed: …") with empty
        // toolEvents and empty llmEvents. The user message is the
        // same "where is my ad" form-prefilled cs15-shape that the
        // regex would otherwise match to UC-A.
        AgentRunResult errorResult = AgentRunResult.error(
                "llm_invocation_failed: read timed out after 12000ms");
        BotSession session = cs259ShapeSession();
        // Replace the cs259 form context with the live-trace form
        // context (Ad Support topic, "where is my ad" description),
        // so the regex would resolve to UC-A if the fallback fired.
        session.setFormTopicSubject("Ad Support");
        session.setFormContext("{\"first_name\":\"joh\",\"email\":\"joh@e.com\","
                + "\"topic_subject\":\"Ad Support\",\"ad_id\":\"123\","
                + "\"description\":\"where is my ad?\"}");
        mockCommonStubsWithRunResult(errorResult, serviceDegradedDecision());

        // ── Act ──────────────────────────────────────────────────
        controlKernel.processMessage(session, "hi, why I can't see my ad");

        // ── Assert: K0 narrowing leaves UC null ──────────────────
        assertNull(session.getActiveUseCase(),
                "When the LLM call failed and produced no tool/llm events, "
                        + "K0 must NOT silently stamp UC-A from the regex. "
                        + "Leaving activeUseCase null is intentional so the "
                        + "trace contract validator and operators see the "
                        + "service_degraded surface.");
        assertEquals("service_degraded", session.getEscalationReason(),
                "Escalation reason must surface as service_degraded.");
        assertEquals("ESCALATE", session.getCurrentPhase());
        assertEquals("escalated", session.getContainmentOutcome());
    }

    @Test
    void llmRanReturnedEscalateWithNoEvents_treatedAsEvidence_appliesFallback() {
        // ── Arrange: edge case — terminalOutcome=ESCALATE but no
        // toolEvents / no llmEvents. This mirrors a future code path
        // where AgentRunLoop short-circuits to escalate without
        // recording events. The narrowing's evidence predicate
        // includes outcome != ERROR as one sufficient condition, so
        // ESCALATE-with-empty-events still triggers the fallback.
        // This preserves the original cs259-shape contract for
        // pre-existing callers.
        AgentRunResult escalateNoEvents = AgentRunResult.escalate(
                "faq_miss_threshold_exceeded",
                List.of(),
                List.of(),
                "{\"phase\":\"DISCOVER\"}",
                null);
        BotSession session = cs259ShapeSession();
        mockCommonStubsWithRunResult(escalateNoEvents, faqMissDecision());

        controlKernel.processMessage(session,
                "How do I receive the payment when I sell an item");

        assertEquals("UC-F", session.getActiveUseCase(),
                "outcome=ESCALATE is positive evidence (the bot decided "
                        + "to hand over rather than crashing); the fallback "
                        + "must still fire to satisfy the cs259 r2 contract.");
        assertEquals("faq_miss_threshold_exceeded", session.getEscalationReason());
    }

    @Test
    void llmRanWithToolEventsButNoLlmEvents_appliesFallback() {
        // ── Arrange: AgentRunLoop ran search_knowledge (tool event
        // recorded) then the LLM call wrapping the next step threw,
        // returning ERROR with toolEvents=[search_knowledge] and
        // llmEvents=[]. The bot DID reason about the user's request
        // (it ran a search), so K0 should still apply.
        com.gumtree.csagent.model.ToolEvent searchEvent =
                new com.gumtree.csagent.model.ToolEvent(
                        0, 0, "search_knowledge",
                        java.util.Map.of("query", "payment"),
                        true, null, null, 450L);
        AgentRunResult errorWithToolEvent = new AgentRunResult(
                List.of(),
                List.of(searchEvent),
                List.of(),
                com.gumtree.csagent.model.TerminalOutcome.ERROR,
                null,
                Optional.of("llm_invocation_failed: timeout"),
                "{\"phase\":\"DISCOVER\"}",
                null);
        BotSession session = cs259ShapeSession();
        mockCommonStubsWithRunResult(errorWithToolEvent, serviceDegradedDecision());

        controlKernel.processMessage(session,
                "How do I receive the payment when I sell an item");

        assertEquals("UC-F", session.getActiveUseCase(),
                "Even on terminal ERROR, recorded tool events count as "
                        + "evidence the bot actually ran — the fallback must "
                        + "fire so the cs259 r2 partial-progress shape still "
                        + "lands on a valid UC.");
    }

    @Test
    void llmRanWithLlmEventsButNoToolEvents_appliesFallback() {
        // ── Arrange: the LLM responded once (llmEvent recorded) but
        // no tool was called and the parser failed, returning ERROR.
        // Same evidence-positive reasoning as the previous test.
        com.gumtree.csagent.model.LlmCallEvent llmEvent =
                new com.gumtree.csagent.model.LlmCallEvent(
                        0, 0, "kimi-k2.6", 120, 40, 2400L, "raw response");
        AgentRunResult errorWithLlmEvent = new AgentRunResult(
                List.of(),
                List.of(),
                List.of(llmEvent),
                com.gumtree.csagent.model.TerminalOutcome.ERROR,
                null,
                Optional.of("parser_failed"),
                "{\"phase\":\"DISCOVER\"}",
                "raw response that couldn't be parsed");
        BotSession session = cs259ShapeSession();
        mockCommonStubsWithRunResult(errorWithLlmEvent, serviceDegradedDecision());

        controlKernel.processMessage(session,
                "How do I receive the payment when I sell an item");

        assertEquals("UC-F", session.getActiveUseCase(),
                "An LLM event without tool events still counts as "
                        + "evidence; the fallback must fire.");
    }

}
