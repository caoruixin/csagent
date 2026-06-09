package com.gumtree.csagent.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gumtree.csagent.config.AgentRunLoopProperties;
import com.gumtree.csagent.model.BotSession;
import com.gumtree.csagent.model.BotTurn;
import com.gumtree.csagent.model.DriftResult;
import com.gumtree.csagent.repository.BotEventRepository;
import com.gumtree.csagent.repository.BotTurnRepository;
import com.gumtree.csagent.service.knowledge.KnowledgeSearchService;
import com.gumtree.csagent.service.guardrails.ScriptLibraryService;
import com.gumtree.csagent.service.llm.LlmDeadlineExceededException;
import com.gumtree.csagent.service.llm.LlmUnavailableException;
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
import com.gumtree.csagent.service.runtime.LlmInvocationService;
import com.gumtree.csagent.service.runtime.PhaseEvaluator;
import com.gumtree.csagent.service.runtime.UseCaseRegistryService;
import com.gumtree.csagent.service.tools.CreateCaseControlledTool;
import com.gumtree.csagent.service.tools.ToolDispatcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Sprint 8.1 §M2 follow-up (2026-05-06) — bot_response persistence under
 * graceful give-up.
 *
 * <p>Pinpoints the LLM-reliability symptom seen in trace
 * {@code f1555995-ff1...}: after the first turn's LLM call, subsequent
 * turns showed {@code bot_response: null} in {@code conversation_history}
 * and a blank Output in the trace UI even though the user-facing API
 * response carried the graceful "Sorry, I'm a bit slow" / "Sorry, I'm
 * having trouble reaching the assistant" text. Root cause was
 * {@code ControlKernel.recordRunResult} writing
 * {@code result.finalUserMessage()} (always {@code null} for non-FINAL_ANSWER
 * outcomes) into {@code bot_turns.bot_response} instead of the displayed
 * reply text.
 *
 * <p>This regression test exercises the full {@link ControlKernel} +
 * {@link AgentRunLoopImpl} + {@link PhaseEvaluator} flow with a mocked
 * {@link LlmInvocationService} that throws
 * {@link LlmDeadlineExceededException} (and separately
 * {@link LlmUnavailableException}). It asserts that:
 * <ul>
 *   <li>The user-facing response text is the graceful give-up message.</li>
 *   <li>The persisted {@link BotTurn#getBotResponse()} matches the same
 *       text — never {@code null}.</li>
 *   <li>The session stays in DISCOVER (not auto-escalated).</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class AgentRunLoopDeadlineExceededBotResponseIntegrationTest {

    @Mock private BotTurnRepository turnRepository;
    @Mock private BotEventRepository eventRepository;
    @Mock private BudgetChecker budgetChecker;
    @Mock private DriftDetector driftDetector;
    @Mock private ControlPolicyService controlPolicy;
    @Mock private CreateCaseControlledTool createCaseTool;
    @Mock private EventEmitter eventEmitter;
    @Mock private ContextProjectionBuilder contextProjectionBuilder;
    @Mock private LlmInvocationService llmInvocation;
    @Mock private ToolDispatcher toolDispatcher;
    @Mock private ActionParser actionParser;
    @Mock private UseCaseRegistryService useCaseRegistry;
    @Mock private KnowledgeSearchService knowledgeSearchService;
    @Mock private ScriptLibraryService scriptLibrary;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private ControlKernel controlKernel;
    private PhaseEvaluator phaseEvaluator;
    private AgentRunLoop agentRunLoop;

    @BeforeEach
    void setUp() {
        phaseEvaluator = new PhaseEvaluator(
                useCaseRegistry, knowledgeSearchService, scriptLibrary,
                llmInvocation, contextProjectionBuilder, actionParser,
                objectMapper, createCaseTool, eventEmitter, toolDispatcher,
                com.gumtree.csagent.service.runtime.skill.SkillTestFixtures.productionRegistry(), null);

        agentRunLoop = new AgentRunLoopImpl(
                llmInvocation, toolDispatcher, contextProjectionBuilder,
                actionParser, objectMapper);

        AgentRunLoopProperties props = new AgentRunLoopProperties();
        props.setEnabledPhases(List.of("DISCOVER"));

        controlKernel = new ControlKernel(
                turnRepository, eventRepository, budgetChecker, driftDetector,
                phaseEvaluator, controlPolicy, objectMapper, createCaseTool,
                eventEmitter, contextProjectionBuilder, props, agentRunLoop,
                new EscalationReasonResolver());
    }

    private BotSession discoverSession() {
        BotSession session = new BotSession();
        session.setSessionId("sess-deadline-bot-response");
        session.setCurrentPhase("DISCOVER");
        session.setTotalBotTurns(0);
        session.setClarificationCount(0);
        session.setRuntimeErrorCount(0);
        session.setFormContext(
                "{\"email\":\"hh@hh.com\",\"ad_id\":\"888\","
                        + "\"first_name\":\"hh\",\"description\":\"where is my ad\","
                        + "\"topic_subject\":\"Ad Support\"}");
        return session;
    }

    private void wireDiscoverHappyPath() {
        when(budgetChecker.checkBudgets(any())).thenReturn(Optional.empty());
        when(driftDetector.detect(any(), anyString())).thenReturn(
                DriftResult.builder().type(DriftResult.DriftType.NONE).build());
        when(turnRepository.findBySessionIdOrderByTurnIndex(anyString()))
                .thenReturn(List.of());
        when(contextProjectionBuilder.build(any(), any(), any(), anyString(), any(), any()))
                .thenReturn("{\"phase\":\"DISCOVER\"}");
    }

    @Test
    void deadlineExceeded_persistsGracefulGiveUpMessage_asBotResponse() {
        wireDiscoverHappyPath();
        when(llmInvocation.invokeChat(anyString(), anyString(), anyString(), anyInt()))
                .thenThrow(new LlmDeadlineExceededException(
                        "LLM provider chain attempt budget exhausted"));

        BotSession session = discoverSession();

        ControlKernel.KernelResult result = controlKernel.processMessage(session, "I can't find my advert");

        // 1. The user-facing text is the graceful give-up message.
        assertNotNull(result.responseText());
        assertTrue(result.responseText().toLowerCase().contains("slow"),
                "User-facing reply should be the deadline-exceeded give-up: "
                        + result.responseText());
        assertFalse(result.shouldEndChat(),
                "Deadline-exceeded must not end the chat — the user should be able to retry");

        // 2. The persisted BotTurn.bot_response matches what the user saw.
        ArgumentCaptor<BotTurn> turnCaptor = ArgumentCaptor.forClass(BotTurn.class);
        verify(turnRepository).save(turnCaptor.capture());
        BotTurn savedTurn = turnCaptor.getValue();
        assertNotNull(savedTurn.getBotResponse(),
                "Sprint 8.1 §M2 follow-up regression: bot_response must NEVER be null "
                        + "on a graceful deadline-exceeded turn — the trace UI and the "
                        + "next turn's conversation_history projection both depend on it.");
        assertEquals(result.responseText(), savedTurn.getBotResponse(),
                "Persisted bot_response must equal the user-facing reply text");

        // 3. Session stays in DISCOVER (not auto-escalated on deadline).
        assertEquals("DISCOVER", session.getCurrentPhase());
    }

    @Test
    void llmUnavailable_persistsGracefulGiveUpMessage_asBotResponse() {
        wireDiscoverHappyPath();
        when(llmInvocation.invokeChat(anyString(), anyString(), anyString(), anyInt()))
                .thenThrow(new LlmUnavailableException(
                        "LLM provider chain failed (failure_class=llm_server_error)",
                        "llm_server_error", new RuntimeException("503")));

        BotSession session = discoverSession();

        ControlKernel.KernelResult result = controlKernel.processMessage(session, "hi");

        assertNotNull(result.responseText());
        assertTrue(result.responseText().toLowerCase().contains("trouble")
                        || result.responseText().toLowerCase().contains("assistant"),
                "User-facing reply should be the llm-unavailable give-up: "
                        + result.responseText());

        ArgumentCaptor<BotTurn> turnCaptor = ArgumentCaptor.forClass(BotTurn.class);
        verify(turnRepository).save(turnCaptor.capture());
        BotTurn savedTurn = turnCaptor.getValue();
        assertNotNull(savedTurn.getBotResponse(),
                "Sprint 8.1 §M2 follow-up regression: bot_response must NEVER be null "
                        + "on a graceful llm-unavailable turn.");
        assertEquals(result.responseText(), savedTurn.getBotResponse());

        assertEquals("DISCOVER", session.getCurrentPhase());
    }
}
