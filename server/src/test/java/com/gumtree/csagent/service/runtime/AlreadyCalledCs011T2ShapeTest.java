package com.gumtree.csagent.service.runtime;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gumtree.csagent.model.BotSession;
import com.gumtree.csagent.model.PhasePlan;
import com.gumtree.csagent.model.TerminalOutcome;
import com.gumtree.csagent.model.ToolCall;
import com.gumtree.csagent.model.ToolEvent;
import com.gumtree.csagent.service.tools.ToolPolicyEnforcer;
import com.gumtree.csagent.service.tools.ToolResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;

/**
 * Sprint 20 fix iteration — Codex Finding 1, Track B cs_011 T2 target
 * coverage. Closes the gap named in
 * {@code docs/codex-findings.md:54} ("the handoff also names only the
 * manual-probe trace shape for Track B target coverage and omits the
 * required cs_011 T2 target") against the parent Sprint 20 objective bar
 * at lines 231–233 ("Track B's coverage = (target) the manual-probe
 * trace + the slow-LLM cs_011 T2 shape").
 *
 * <p>The cs_011 T2 shape is documented at:
 * <ul>
 *   <li>{@code docs/diagnostics/failure-briefs/cs011-uc-d-detailed-description-ignored-on-faq-miss.md}
 *       — UC-D primary; user opens with a dense, multi-symptom
 *       password-reset reproduction; bot's prior turn already ran
 *       {@code search_knowledge}; the second loop iteration is the
 *       turn where the projection should surface that prior call so
 *       the LLM does not silently re-emit.</li>
 *   <li>{@code docs/action_bank.md:464} — promotes the cs_011 T2
 *       silence into the {@code R-prompt-phase-plan-directive-followship}
 *       evidence base, citing the projection-side gap as the proximate
 *       cause.</li>
 * </ul>
 *
 * <p>Per the fix-iteration prompt: assert <strong>only</strong> that the
 * {@code already_called} slot is populated for the cs_011 input shape.
 * This test does <strong>not</strong> remediate cs_011's underlying
 * failure (the bot's template-escalation surface is a separate, deferred
 * remediation per the parent Sprint 20 objective's "Do not implement"
 * list and the brief's "What should NOT be done" rules).
 *
 * <p>Unit shape was chosen over integration shape because the
 * cs_011-specific coverage bar is narrow: the slot is populated for the
 * cs_011-shape projection-builder input. The runtime-level non-enforcement
 * bar (the dispatcher executes the second identical-args call even with
 * the slot populated) is already covered by
 * {@code AgentRunLoopAlreadyCalledNonEnforcementIntegrationTest} at the
 * {@link AgentRunLoopImpl#run} granularity Codex Finding 1 required.
 * Mirroring the unit-test structure of {@code AlreadyCalledProjectionTest}
 * also keeps the cs_011 shape adjacent to the other Track B
 * projection-assembly assertions, which is the natural home for
 * shape-coverage tests on the slot.
 */
@ExtendWith(MockitoExtension.class)
class AlreadyCalledCs011T2ShapeTest {

    private static final String CS011_USER_MESSAGE =
            "Your be the third agent today to try n resolve the issue im having. "
                    + "I've clearly explained to the previous to agents that I am "
                    + "unable to log into my account. When I try to login it says "
                    + "pw or email incorrect. I then get a pw reset link sent to "
                    + "my email. I use this link to reset my pw using upper n "
                    + "lower case with number n special character.";

    private ContextProjectionBuilder builder;
    private ObjectMapper objectMapper;

    @Mock private UseCaseRegistryService useCaseRegistry;
    @Mock private ControlPolicyService controlPolicy;
    @Mock private ToolPolicyEnforcer toolPolicyEnforcer;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        builder = new ContextProjectionBuilder(
                objectMapper, useCaseRegistry, controlPolicy, toolPolicyEnforcer);
        builder.initToolSchemas();

        lenient().when(controlPolicy.getMaxBotTurnsFaq()).thenReturn(6);
        lenient().when(controlPolicy.getMaxClarificationRounds()).thenReturn(3);
        lenient().when(controlPolicy.getMaxFaqMiss()).thenReturn(2);

        // UC-D primary (per cs_011 brief) — Account Support / login issues.
        // Configured as a FAQ-path UC so the budget projection picks the
        // FAQ branch.
        UseCaseRegistryService.UseCaseDefinition ucd = new UseCaseRegistryService.UseCaseDefinition(
                "UC-D", "Account Support", List.of("Account / Login"), "MEDIUM", true, "FAQ");
        lenient().when(useCaseRegistry.getUseCase("UC-D")).thenReturn(ucd);
        lenient().when(toolPolicyEnforcer.getVisibleToolsForUc("UC-D")).thenReturn(List.of(
                "search_knowledge", "resolve_article",
                "request_handover", "record_outcome"));
    }

    @Test
    void cs011T2Shape_slotPopulatedForPriorSearchKnowledgeDispatch() throws Exception {
        // cs_011 UC-D session at the T2 boundary — search_knowledge already
        // ran on the prior loop step with the cs_011 query, but no
        // resolve-grade FAQ article was available (faq_miss=true per
        // override). The projection at the next loop step must surface the
        // prior call in the already_called slot so the LLM can avoid
        // silently re-emitting the same query.
        BotSession session = BotSession.builder()
                .sessionId("cs011-trackb-t2-shape")
                .activeUseCase("UC-D")
                .currentPhase("RESOLVE")
                .handlingState("BOT_HANDLING")
                .totalBotTurns(0)
                .clarificationCount(0)
                // T2 shape: prior search_knowledge call already returned
                // an empty viable hit set per cs_011's faq_miss override.
                .faqMissCount(1)
                .repeatedActionCount(0)
                .build();

        PhasePlan plan = PhasePlan.builder()
                .phase("RESOLVE")
                .useCase("UC-D")
                .objective("answer")
                .allowedTools(List.of("search_knowledge", "resolve_article",
                        "request_handover", "record_outcome"))
                .maxToolSteps(3)
                .systemInstruction("RESOLVE")
                .validTerminalOutcomes(Set.of(TerminalOutcome.FINAL_ANSWER,
                        TerminalOutcome.ESCALATE))
                .build();

        // Step 0 (the cs_011 T1 → T2 boundary representative): the bot
        // already dispatched search_knowledge with the user's
        // password-reset-loop query. The brief records the actual cs_011
        // tool sequence as `search_knowledge + 3× resolve_article +
        // request_handover` — this test models the simplest version of
        // the slot-population concern, namely that the projection at the
        // step immediately after the search_knowledge dispatch surfaces
        // that prior call.
        Map<String, Object> priorArgs = new LinkedHashMap<>();
        priorArgs.put("query", "password reset link not recognised login still rejects");
        priorArgs.put("uc_tags", List.of("UC-D"));
        ToolCall priorCall = ToolCall.builder()
                .name("search_knowledge").arguments(priorArgs).build();
        ToolResult priorResult = ToolResult.ok(Map.of(
                "faq_miss", true,
                "hits", List.of()));
        ToolEvent priorEvent = ToolEvent.of(0, priorCall, priorResult, 940L);

        String projection = builder.build(
                session, List.of(), plan, CS011_USER_MESSAGE, null, List.of(priorEvent));

        JsonNode root = objectMapper.readTree(projection);
        JsonNode slot = root.get("already_called");

        assertNotNull(slot, "cs_011 T2 projection must carry the already_called slot");
        assertTrue(slot.isArray(), "already_called must be a JSON array");
        assertEquals(1, slot.size(),
                "cs_011 T2 projection must contain exactly one already_called entry for "
                        + "the prior search_knowledge dispatch; got " + slot);

        JsonNode entry = slot.get(0);
        assertEquals("search_knowledge", entry.get("tool").asText(),
                "the slot entry tool must match the cs_011 T2 prior dispatch");
        assertEquals(0, entry.get("at_step").asInt(),
                "the slot entry at_step must reference step 0 (the prior dispatch in the run)");

        String hash = entry.get("arguments_hash").asText();
        assertEquals(16, hash.length(),
                "arguments_hash must be 16 hex chars (truncated SHA-256 per the slot contract)");
        assertTrue(hash.matches("[0-9a-f]{16}"),
                "arguments_hash must be lowercase hex; got " + hash);
    }
}
