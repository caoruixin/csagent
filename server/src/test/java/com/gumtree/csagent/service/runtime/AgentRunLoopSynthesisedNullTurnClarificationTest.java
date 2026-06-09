package com.gumtree.csagent.service.runtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gumtree.csagent.model.BotSession;
import com.gumtree.csagent.model.LlmResponse;
import com.gumtree.csagent.model.PhasePlan;
import com.gumtree.csagent.model.TerminalOutcome;
import com.gumtree.csagent.service.tools.ToolDispatcher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Sprint 085 / S-Auto-30 / M-Auto-7 — a runtime-synthesised DISCOVER
 * placeholder (injected by {@link ActionParser} when the LLM null-turns:
 * empty {@code user_message} + empty {@code tool_calls}) must NOT be counted
 * as a clarification round and must not burn the clarification budget.
 *
 * <p>Root cause (CS3 session {@code 33edc1eb}): the LLM emitted a true null
 * turn; {@code ActionParser} substitutes the placeholder
 * {@code "I'm looking into this for you."} AT PARSE TIME, so the loop sees a
 * NON-BLANK {@code userMsg} and the R2.a structural counter incremented on the
 * runtime placeholder (count 1→2), force-escalating the next turn before the
 * LLM saw the user's "Thank you." The fix is a structural provenance flag
 * ({@code action.isUserMessageSynthesised()}), NOT a content heuristic — the
 * R2.a "structural cardinality only" design is preserved.
 *
 * <p>These drive the live {@link AgentRunLoopImpl} path (the increment site is
 * {@code AgentRunLoopImpl:471}); the budget-gate consequence is pinned with the
 * pre-existing {@link BudgetChecker} (count {@code >=} cap → escalate). The
 * cap is the unchanged {@code max-clarification-rounds: 2}.
 */
@ExtendWith(MockitoExtension.class)
class AgentRunLoopSynthesisedNullTurnClarificationTest {

    private static final int MAX_CLARIFICATION_ROUNDS = 2;

    @Mock private LlmInvocationService llmInvocation;
    @Mock private ToolDispatcher toolDispatcher;
    @Mock private ContextProjectionBuilder contextProjectionBuilder;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ActionParser actionParser = new ActionParser(objectMapper);

    private AgentRunLoopImpl newLoop() {
        return new AgentRunLoopImpl(llmInvocation, toolDispatcher,
                contextProjectionBuilder, actionParser, objectMapper);
    }

    private static PhasePlan discoverPlan() {
        return PhasePlan.builder()
                .phase("DISCOVER")
                .useCase(null)
                .objective("Identify the use case")
                .allowedTools(List.of("classify_use_case", "search_knowledge"))
                .maxToolSteps(3)
                .systemInstruction("test")
                .validTerminalOutcomes(Set.of(
                        TerminalOutcome.FINAL_ANSWER,
                        TerminalOutcome.CLARIFICATION_NEEDED,
                        TerminalOutcome.ESCALATE))
                .build();
    }

    private static BotSession discoverSession(String id) {
        BotSession session = new BotSession();
        session.setSessionId(id);
        session.setCurrentPhase("DISCOVER");
        // DISCOVER has not committed a UC; activeUseCase stays null.
        // Explicitly seed counters: @Builder.Default initializers are not
        // applied via the no-args constructor, and the increment site does
        // getClarificationCount() + 1.
        session.setClarificationCount(0);
        session.setFaqMissCount(0);
        session.setRepeatedActionCount(0);
        session.setTotalBotTurns(0);
        return session;
    }

    private void stubProjection() {
        when(contextProjectionBuilder.build(any(), any(), any(), anyString(), any(), any()))
                .thenReturn("{}");
    }

    /** Build a no-tool-call LLM response carrying {@code userMsg} (use "" for a null turn). */
    private static LlmResponse noToolResponse(String userMsg) {
        String json = "{\"user_message\":\"" + userMsg + "\",\"reasoning\":\"x\",\"tool_calls\":[]}";
        return LlmResponse.builder().content(json).finishReason("stop").latencyMs(100L).build();
    }

    /** A real BudgetChecker wired to the unchanged control-policy budgets. */
    private static BudgetChecker budgetChecker() {
        ControlPolicyService cp = Mockito.mock(ControlPolicyService.class);
        // Lenient: which budget getters are reached depends on the count (the
        // clarification check short-circuits at the cap), so the non-clarification
        // limits are not always consulted.
        Mockito.lenient().when(cp.getMaxClarificationRounds()).thenReturn(MAX_CLARIFICATION_ROUNDS);
        Mockito.lenient().when(cp.getMaxFaqMiss()).thenReturn(2);
        Mockito.lenient().when(cp.getMaxRepeatedSameAction()).thenReturn(2);
        Mockito.lenient().when(cp.getMaxTotalBotTurns()).thenReturn(25);
        UseCaseRegistryService reg = Mockito.mock(UseCaseRegistryService.class);
        return new BudgetChecker(cp, reg);
    }

    // ---- Test #2: synthesised placeholder turn does NOT increment ----

    @Test
    void synthesisedNullTurn_doesNotIncrementClarificationCount() {
        stubProjection();
        when(llmInvocation.invokeChat(anyString(), anyString(), anyString(), anyInt()))
                .thenReturn(noToolResponse("")); // true null turn → ActionParser synthesises

        BotSession session = discoverSession("sess-synth-1");
        newLoop().run(discoverPlan(), session, "Why was my ad put on hold?", List.of());

        assertEquals(0, session.getClarificationCount(),
                "a runtime-synthesised null-turn placeholder must NOT increment "
                        + "the clarification counter");
    }

    // ---- Test #3: genuine clarification still increments (anti-误杀) ----

    @Test
    void genuineQuestionClarification_incrementsClarificationCount() {
        stubProjection();
        when(llmInvocation.invokeChat(anyString(), anyString(), anyString(), anyInt()))
                .thenReturn(noToolResponse("Which ad are you referring to?"));

        BotSession session = discoverSession("sess-genuine-1");
        newLoop().run(discoverPlan(), session, "my ad is gone", List.of());

        assertEquals(1, session.getClarificationCount(),
                "an LLM-authored '?'-clarification (not synthesised) must still "
                        + "increment the clarification counter — anti-误杀");
    }

    // ---- Test #4: non-synthesised non-blank reply → structural counting unchanged ----

    @Test
    void nonSynthesisedDeclarativeReply_structuralCountingUnchanged() {
        // The R2.a counter is cardinality-only: ANY non-empty, non-synthesised
        // DISCOVER free-text reply with no tool call and no UC commit counts,
        // regardless of whether it is "?"-shaped. The provenance exclusion must
        // not change this for non-synthesised replies.
        stubProjection();
        when(llmInvocation.invokeChat(anyString(), anyString(), anyString(), anyInt()))
                .thenReturn(noToolResponse("Tell me more about the problem."));

        BotSession session = discoverSession("sess-decl-1");
        newLoop().run(discoverPlan(), session, "help", List.of());

        assertEquals(1, session.getClarificationCount(),
                "existing structural counting must be unchanged for a "
                        + "non-synthesised non-blank reply");
    }

    // ---- Test #5: CS3 3-turn DISCOVER stall — placeholder does not push count to 2 ----

    @Test
    void cs3ReplayShape_placeholderDoesNotPushCountToCap() {
        stubProjection();
        // Turn 1: genuine clarification (count 0→1). Turn 2: null turn →
        // synthesised placeholder (count stays 1, would have been 2 before).
        when(llmInvocation.invokeChat(anyString(), anyString(), anyString(), anyInt()))
                .thenReturn(noToolResponse("Could you confirm the ad ID?"),
                        noToolResponse("")); // null turn

        BotSession session = discoverSession("sess-cs3-33edc1eb");
        AgentRunLoopImpl loop = newLoop();
        loop.run(discoverPlan(), session, "restore my ad please", List.of());
        loop.run(discoverPlan(), session, "AD-1002", List.of());

        assertEquals(1, session.getClarificationCount(),
                "the synthesised placeholder must NOT push the count to the cap; "
                        + "count stays at 1 (the single genuine clarification)");
        assertTrue(session.getClarificationCount() < MAX_CLARIFICATION_ROUNDS,
                "below cap → the pre-LLM budget gate does not fire");
        assertEquals(Optional.empty(), budgetChecker().checkBudgets(session),
                "with count below cap the clarification budget is NOT exhausted, so "
                        + "the next turn reaches the LLM (no premature force-escalate)");
    }

    // ---- Test #6: anti-误杀 — genuine over-clarifying still hits cap and escalates ----

    @Test
    void genuineOverClarifying_stillHitsCapAndEscalates() {
        stubProjection();
        // Two genuine clarifications: count 0→1→2 (the cap is reached honestly).
        when(llmInvocation.invokeChat(anyString(), anyString(), anyString(), anyInt()))
                .thenReturn(noToolResponse("Which ad are you referring to?"),
                        noToolResponse("Can you tell me the ad ID?"));

        BotSession session = discoverSession("sess-overclarify-1");
        AgentRunLoopImpl loop = newLoop();
        loop.run(discoverPlan(), session, "something is wrong", List.of());
        loop.run(discoverPlan(), session, "I don't know", List.of());

        assertEquals(MAX_CLARIFICATION_ROUNDS, session.getClarificationCount(),
                "a genuinely over-clarifying session must still reach the cap of 2 — "
                        + "the structural guard is not weakened by the provenance exclusion");
        assertEquals(Optional.of("max-clarification-rounds"),
                budgetChecker().checkBudgets(session),
                "at the cap the clarification budget is exhausted → force ESCALATE");
    }
}
