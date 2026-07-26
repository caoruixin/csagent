package com.gumtree.csagent.service.runtime;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gumtree.csagent.model.BotSession;
import com.gumtree.csagent.service.tools.ToolPolicyEnforcer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;

/**
 * Sprint 104 — the DISCOVER clarification-budget projection contract.
 *
 * <p><strong>These are WIRING tests, not behaviour evidence</strong>
 * (iteration_governance §5.7). They pin what the runtime PROJECTS. Whether the
 * LLM acts differently on it is measured only by a real-LLM run; see the
 * sprint-104 handoff §4.
 *
 * <p>What is being fixed. Before this sprint the slot carried {@code used} and
 * {@code max} and nothing else. Both numbers were accurate, and both were
 * insufficient: nothing told the model what happens AT the cap.
 * {@link BudgetChecker} fires at {@code clarificationCount >= max} inside
 * {@code ControlKernel.processMessage} step 3, which force-escalates the
 * session WITHOUT building a projection and WITHOUT invoking the LLM. So the
 * model's last chance to act is the turn on which {@code remaining == 1}, and
 * it had no way to know that.
 *
 * <p>Measured on 2026-07-26 across the ten Sprint 103 sessions: in 4 of the 4
 * sessions that genuinely exhausted this budget, the force-escalated turn was
 * the turn on which the customer supplied exactly what the bot had just asked
 * for, and {@code projected_context} is NULL on each of those turns.
 *
 * <p>What this is NOT: the added fields state what the runtime does, not what
 * to conclude. The choice between committing a use case and asking another
 * question stays with the LLM (Constitution §1.3). No keyword, no similarity
 * metric, no per-UC branch.
 */
@ExtendWith(MockitoExtension.class)
class DiscoverClarificationBudgetProjectionTest {

    @Mock private UseCaseRegistryService useCaseRegistry;
    @Mock private ControlPolicyService controlPolicy;
    @Mock private ToolPolicyEnforcer toolPolicyEnforcer;

    private ContextProjectionBuilder projectionBuilder;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        projectionBuilder = new ContextProjectionBuilder(
                objectMapper, useCaseRegistry, controlPolicy, toolPolicyEnforcer, null);
        projectionBuilder.initToolSchemas();

        lenient().when(controlPolicy.getMaxBotTurnsFaq()).thenReturn(15);
        lenient().when(controlPolicy.getMaxClarificationRounds()).thenReturn(2);
        lenient().when(controlPolicy.getMaxFaqMiss()).thenReturn(2);
    }

    // ---- the pre-existing contract, preserved -----------------------------

    @Test
    void discoverProjection_stillCarriesUsedAndMax() throws Exception {
        JsonNode slot = clarificationSlot(discoverSession(1));

        assertEquals(1, slot.get("used").asInt());
        assertEquals(2, slot.get("max").asInt());
    }

    @Test
    void slotRemainsDiscoverOnly() throws Exception {
        // DISCOVER is the only phase in which clarification rounds are counted
        // (AgentRunLoopImpl:1497-1505 gates the increment on the phase), so the
        // slot stays absent elsewhere. Unchanged by Sprint 104.
        BotSession resolving = discoverSession(1);
        resolving.setCurrentPhase("RESOLVE");
        resolving.setActiveUseCase("UC-A");

        JsonNode root = objectMapper.readTree(projectionBuilder.buildProjection(
                resolving, List.of(), null, "any update?"));

        assertFalse(root.has("budgets"),
                "budgets.clarification must remain DISCOVER-only; found it in RESOLVE.");
    }

    // ---- Sprint 104 additions ---------------------------------------------

    @Test
    void remaining_isMaxMinusUsed() throws Exception {
        assertEquals(2, clarificationSlot(discoverSession(0)).get("remaining").asInt());
        assertEquals(1, clarificationSlot(discoverSession(1)).get("remaining").asInt());
    }

    @Test
    void remaining_neverGoesNegative() throws Exception {
        // Defensive: the counter is monotonic and the cap is enforced at turn
        // start, so used > max should not occur — but a clamped value is
        // preferable to projecting "-1 remaining" at the LLM if it ever does.
        assertEquals(0, clarificationSlot(discoverSession(5)).get("remaining").asInt());
    }

    @Test
    void onExhaustion_statesThatTheNextTurnIsEscalatedBeforeTheLlmIsConsulted()
            throws Exception {
        // The load-bearing fact. ControlKernel:310 -> BudgetChecker:32 ->
        // forceEscalate(:1317) never builds a projection and never calls the
        // LLM. This is the single thing the model could not previously know.
        String text = clarificationSlot(discoverSession(1)).get("on_exhaustion").asText();

        assertTrue(text.contains("before you are consulted"),
                "on_exhaustion must state that the escalation pre-empts the LLM; got: " + text);
        assertTrue(text.contains("automatically"),
                "on_exhaustion must state the escalation is automatic; got: " + text);
    }

    @Test
    void countsTowardUsed_namesTheStructuralPredicateNotAContentRule() throws Exception {
        // Mirrors isDiscoverFreeTextClarification(AgentRunLoopImpl:1497-1505):
        // free text without a classify_use_case call increments; a turn that
        // calls classify_use_case does not. Structural, per R2.a.
        String text = clarificationSlot(discoverSession(0)).get("counts_toward_used").asText();

        assertTrue(text.contains("classify_use_case"),
                "counts_toward_used must name the tool whose presence exempts the turn; got: " + text);
    }

    @Test
    void slotIsFactual_carriesNoInstructionToCommitOrEscalate() throws Exception {
        // Anti-hardcode guard (§1.5 / §1.7): the runtime reports state; it must
        // not tell the LLM which action to take. If a future edit turns this
        // slot into advice, this test fails.
        JsonNode slot = clarificationSlot(discoverSession(1));
        String all = slot.toString().toLowerCase(java.util.Locale.ROOT);

        assertFalse(all.contains("you should"),
                "clarification slot must not prescribe an action; got: " + slot);
        assertFalse(all.contains("you must"),
                "clarification slot must not prescribe an action; got: " + slot);
        assertFalse(all.contains("instead of asking"),
                "clarification slot must not prescribe an action; got: " + slot);
    }

    @Test
    void nullClarificationCount_doesNotBreakTheSlot() throws Exception {
        BotSession session = discoverSession(0);
        session.setClarificationCount(null);

        JsonNode slot = clarificationSlot(session);

        assertEquals(0, slot.get("used").asInt());
        assertEquals(2, slot.get("remaining").asInt());
    }

    // ---- helpers -----------------------------------------------------------

    private JsonNode clarificationSlot(BotSession session) throws Exception {
        JsonNode root = objectMapper.readTree(projectionBuilder.buildProjection(
                session, List.of(), null, "I also need to edit one of my live ads"));
        assertTrue(root.has("budgets"), "DISCOVER projection must carry budgets");
        JsonNode slot = root.get("budgets").get("clarification");
        assertTrue(slot != null && !slot.isNull(), "budgets.clarification must be present");
        return slot;
    }

    private BotSession discoverSession(int clarifications) {
        return BotSession.builder()
                .sessionId("sprint104-clarification-budget-test")
                .currentPhase("DISCOVER")
                .handlingState("BOT_HANDLING")
                .totalBotTurns(clarifications)
                .clarificationCount(clarifications)
                .faqMissCount(0)
                .repeatedActionCount(0)
                .build();
    }
}
