package com.gumtree.csagent.service.runtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gumtree.csagent.config.AgentRunLoopProperties;
import com.gumtree.csagent.model.BotSession;
import com.gumtree.csagent.repository.BotEventRepository;
import com.gumtree.csagent.repository.BotTurnRepository;
import com.gumtree.csagent.service.observability.EventEmitter;
import com.gumtree.csagent.service.tools.CreateCaseControlledTool;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Sprint 2026-05-04 §B3: deterministic UC fallback for soft-OOS
 * UNKNOWN-topic sessions that escalate before any UC has been
 * committed (cs_interactive_029).
 *
 * <p>The fallback is exposed via the package-private helper
 * {@link ControlKernel#inferFallbackUseCase(BotSession, String)} so
 * the inference rules can be pinned without spinning up the full
 * Spring context.
 */
@ExtendWith(MockitoExtension.class)
class ControlKernelB3FallbackUseCaseTest {

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

    private ControlKernel kernel() {
        return new ControlKernel(
                turnRepository, eventRepository, budgetChecker, driftDetector,
                phaseEvaluator, controlPolicy, new ObjectMapper(), createCaseTool,
                eventEmitter, contextProjectionBuilder, new AgentRunLoopProperties(),
                agentRunLoop, new EscalationReasonResolver());
    }

    private BotSession sessionWithFormDescription(String description) {
        BotSession s = new BotSession();
        s.setSessionId("sess-b3");
        if (description != null) {
            s.setFormContext("{\"description\":\"" + description.replace("\"", "\\\"") + "\"}");
        }
        return s;
    }

    @Test
    void cs029_accountInUserMessage_routesToUcD() {
        // cs_interactive_029 verbatim: empty form description, first
        // user message is the all-caps shout "HI MY ACCOUNT OS".
        BotSession s = sessionWithFormDescription("");
        assertEquals("UC-D", kernel().inferFallbackUseCase(s, "HI MY ACCOUNT OS"));
    }

    @Test
    void loginKeyword_routesToUcD() {
        BotSession s = sessionWithFormDescription("");
        assertEquals("UC-D", kernel().inferFallbackUseCase(s, "I cannot login"));
    }

    @Test
    void messageKeyword_routesToUcC() {
        BotSession s = sessionWithFormDescription("");
        assertEquals("UC-C", kernel().inferFallbackUseCase(s, "I am not getting messages from buyers"));
    }

    @Test
    void notificationKeyword_routesToUcC() {
        BotSession s = sessionWithFormDescription("");
        assertEquals("UC-C",
                kernel().inferFallbackUseCase(s, "no notifications appearing in my inbox"));
    }

    @Test
    void advertKeyword_routesToUcA() {
        BotSession s = sessionWithFormDescription("");
        assertEquals("UC-A",
                kernel().inferFallbackUseCase(s, "my advert is missing from search"));
    }

    @Test
    void refundKeyword_routesToUcF() {
        BotSession s = sessionWithFormDescription("");
        assertEquals("UC-F",
                kernel().inferFallbackUseCase(s, "I want a refund for my listing fee"));
    }

    @Test
    void formDescriptionPrevailsWhenUserMessageBlank() {
        BotSession s = sessionWithFormDescription("My account is locked");
        assertEquals("UC-D", kernel().inferFallbackUseCase(s, ""));
    }

    @Test
    void blankInput_returnsSafeDefault_UcD() {
        BotSession s = sessionWithFormDescription("");
        assertEquals("UC-D", kernel().inferFallbackUseCase(s, ""));
    }

    @Test
    void unrelatedInput_returnsSafeDefault_UcD() {
        BotSession s = sessionWithFormDescription("");
        // No matching keywords — default to UC-D so the trace contract
        // is still satisfied (anything is better than null).
        assertEquals("UC-D",
                kernel().inferFallbackUseCase(s, "hello there how are you"));
    }
}
