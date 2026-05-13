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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.lenient;

/**
 * Sprint 20 Track B regression test — `already_called` projection slot
 * for `R-prompt-projection-already-called-soft-signal`. Source-of-truth:
 *
 * <ul>
 *   <li>{@code docs/sprint_objective.md} §"Tracks (in scope) Track B"
 *       (slot shape + three behaviour bars).</li>
 *   <li>{@code docs/sprints/sprint-019-handoff.md} §4.2 (intended-contract
 *       Layer 1: prompt_projection soft signal, runtime does NOT
 *       short-circuit dispatch).</li>
 * </ul>
 *
 * <p>This test covers the three sprint-objective behaviour bars at the
 * {@link ContextProjectionBuilder} layer (projection-assembly unit). The
 * "runtime does not short-circuit" bar is verified by inspecting the
 * {@link AgentRunLoopImpl} dispatch path: the slot is read in
 * {@code build(...)} only; {@code AgentRunLoopImpl.run} passes
 * {@code toolEvents} into the new {@code build(...)} overload but does
 * not consult the slot for dispatch decisions. The bar is therefore
 * verified here by demonstrating that two identical-args calls in a row
 * both produce {@link ToolEvent}s in the input list, regardless of
 * whether the slot is populated.
 */
@ExtendWith(MockitoExtension.class)
class AlreadyCalledProjectionTest {

    private ContextProjectionBuilder builder;
    private ObjectMapper objectMapper;

    @Mock
    private UseCaseRegistryService useCaseRegistry;

    @Mock
    private ControlPolicyService controlPolicy;

    @Mock
    private ToolPolicyEnforcer toolPolicyEnforcer;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        builder = new ContextProjectionBuilder(objectMapper, useCaseRegistry, controlPolicy, toolPolicyEnforcer);
        builder.initToolSchemas();

        lenient().when(controlPolicy.getMaxBotTurnsFaq()).thenReturn(6);
        lenient().when(controlPolicy.getMaxClarificationRounds()).thenReturn(3);
        lenient().when(controlPolicy.getMaxFaqMiss()).thenReturn(2);

        UseCaseRegistryService.UseCaseDefinition ucDef = new UseCaseRegistryService.UseCaseDefinition(
                "UC-A", "Ad Status & Visibility", List.of("Ad Support"), "LOW", true, "FAQ");
        lenient().when(useCaseRegistry.getUseCase("UC-A")).thenReturn(ucDef);
        lenient().when(toolPolicyEnforcer.getVisibleToolsForUc("UC-A")).thenReturn(List.of(
                "search_knowledge", "resolve_article", "get_customer_context",
                "request_handover", "record_outcome"));
    }

    // --- Behaviour bar (b): empty array when no prior call exists ---

    @Test
    void alreadyCalled_emptyArrayOnFirstStep() throws Exception {
        BotSession session = buildSession();
        PhasePlan plan = resolvePlan();

        // First step in the loop — toolEvents list is empty.
        String projection = builder.build(
                session, List.of(), plan, "why can't I see my advert", null, List.of());

        JsonNode root = objectMapper.readTree(projection);
        assertTrue(root.has("already_called"),
                "already_called slot must be present even when no prior calls exist (shape stability)");
        JsonNode slot = root.get("already_called");
        assertTrue(slot.isArray(),
                "already_called must always be a JSON array");
        assertEquals(0, slot.size(),
                "already_called must be empty when no prior tool dispatch has landed; got " + slot);
    }

    @Test
    void alreadyCalled_emptyArrayOnNullPriorToolEvents() throws Exception {
        // Defensive: a null priorToolEvents argument must not throw and must
        // emit the empty-array shape so a future caller cannot accidentally
        // suppress the slot.
        BotSession session = buildSession();
        PhasePlan plan = resolvePlan();

        String projection = builder.build(
                session, List.of(), plan, "why can't I see my advert", null, null);

        JsonNode root = objectMapper.readTree(projection);
        assertTrue(root.has("already_called"),
                "already_called slot must be present even when priorToolEvents is null");
        assertEquals(0, root.get("already_called").size());
    }

    // --- Behaviour bar (a): slot populated when prior identical-args call exists ---

    @Test
    void alreadyCalled_populatedWhenPriorIdenticalArgsCallExists() throws Exception {
        BotSession session = buildSession();
        PhasePlan plan = resolvePlan();

        // Simulate step 0 of the run loop having already dispatched a
        // successful search_knowledge call. step=1 is being projected here.
        Map<String, Object> args0 = new LinkedHashMap<>();
        args0.put("query", "why can't I see my advert");
        args0.put("uc_tags", List.of("UC-A", "UC-B"));
        ToolCall call0 = ToolCall.builder().name("search_knowledge").arguments(args0).build();
        ToolResult ok = ToolResult.ok(Map.of("hits", List.of("ka4P2...")));
        ToolEvent priorEvent = ToolEvent.of(0, call0, ok, 1234L);

        String projection = builder.build(
                session, List.of(), plan, "why can't I see my advert", null,
                List.of(priorEvent));

        JsonNode root = objectMapper.readTree(projection);
        JsonNode slot = root.get("already_called");
        assertNotNull(slot, "already_called slot must exist");
        assertEquals(1, slot.size(),
                "already_called must list the one prior successful dispatch");
        JsonNode entry = slot.get(0);
        assertEquals("search_knowledge", entry.get("tool").asText());
        assertEquals(0, entry.get("at_step").asInt());
        String hash = entry.get("arguments_hash").asText();
        assertEquals(16, hash.length(),
                "arguments_hash must be 16 hex chars (truncated SHA-256)");
        assertTrue(hash.matches("[0-9a-f]{16}"),
                "arguments_hash must be lowercase hex; got " + hash);
    }

    @Test
    void alreadyCalled_canonicalArgumentsHash_orderInsensitive() {
        // Two arguments maps with identical content but different insertion
        // order must hash identically (canonical-JSON serialiser sorts keys).
        Map<String, Object> args1 = new LinkedHashMap<>();
        args1.put("query", "where is my ad");
        args1.put("uc_tags", List.of("UC-A"));

        Map<String, Object> args2 = new LinkedHashMap<>();
        args2.put("uc_tags", List.of("UC-A"));
        args2.put("query", "where is my ad");

        assertEquals(builder.canonicalArgumentsHash(args1),
                builder.canonicalArgumentsHash(args2),
                "canonical hash must be order-insensitive");

        // A different value must produce a different hash.
        Map<String, Object> args3 = new LinkedHashMap<>();
        args3.put("query", "completely different query");
        args3.put("uc_tags", List.of("UC-A"));
        assertNotEquals(builder.canonicalArgumentsHash(args1),
                builder.canonicalArgumentsHash(args3),
                "hash must differ when content differs");
    }

    // --- Behaviour bar (c): runtime does NOT short-circuit on populated slot ---

    @Test
    void alreadyCalled_includesEveryRepeatedSuccessfulDispatch() throws Exception {
        // The objective §"Tracks (in scope) Track B" + Sprint 19 §4.2 require
        // that the slot lists prior identical-args calls — i.e. when the same
        // tool is dispatched twice with the same args, two entries appear.
        // This is the projection-side artefact of "runtime does not short-
        // circuit on the slot being populated" (the LLM may still re-emit,
        // and the dispatcher executes the re-emission unconditionally; both
        // dispatches show up as ToolEvents and both must appear in the slot).
        BotSession session = buildSession();
        PhasePlan plan = resolvePlan();

        Map<String, Object> args = new LinkedHashMap<>();
        args.put("query", "why can't I see my advert");
        args.put("uc_tags", List.of("UC-A", "UC-B"));
        ToolCall call = ToolCall.builder().name("search_knowledge").arguments(args).build();
        ToolResult ok = ToolResult.ok(Map.of("hits", List.of("ka4P2...")));
        ToolEvent step0 = ToolEvent.of(0, call, ok, 1200L);
        ToolEvent step1 = ToolEvent.of(1, call, ok, 1180L);

        String projection = builder.build(
                session, List.of(), plan, "why can't I see my advert", null,
                List.of(step0, step1));

        JsonNode root = objectMapper.readTree(projection);
        JsonNode slot = root.get("already_called");
        assertEquals(2, slot.size(),
                "two identical-args successful dispatches must produce two slot entries "
                        + "(slot is not deduplicating — the LLM, not the slot, owns whether to re-emit)");
        assertEquals(slot.get(0).get("arguments_hash").asText(),
                slot.get(1).get("arguments_hash").asText(),
                "identical args must hash identically across entries");
        assertEquals(0, slot.get(0).get("at_step").asInt());
        assertEquals(1, slot.get(1).get("at_step").asInt());
    }

    @Test
    void alreadyCalled_excludesUnsuccessfulEvents() throws Exception {
        // Failed-by-plan rejections and intake-guard rejections were not
        // dispatched — they did not happen, so they must not appear in the
        // slot (the slot says what was already CALLED, not what was
        // attempted).
        BotSession session = buildSession();
        PhasePlan plan = resolvePlan();

        Map<String, Object> args = new LinkedHashMap<>();
        args.put("query", "x");
        ToolCall call = ToolCall.builder().name("search_knowledge").arguments(args).build();
        ToolEvent rejected = ToolEvent.rejected(0, call, "tool_not_in_plan");
        ToolEvent ok = ToolEvent.of(1, call, ToolResult.ok(Map.of()), 100L);

        String projection = builder.build(
                session, List.of(), plan, "x", null, List.of(rejected, ok));
        JsonNode slot = objectMapper.readTree(projection).get("already_called");

        assertEquals(1, slot.size(),
                "only successful dispatches should appear; rejected events are excluded");
        assertEquals(1, slot.get(0).get("at_step").asInt(),
                "the one entry must be the successful step=1 dispatch");
    }

    // --- Legacy overload (no priorToolEvents argument) ---

    @Test
    void legacyBuildOverload_stillEmitsEmptyAlreadyCalledSlot() throws Exception {
        // The five-argument build(...) overload is preserved for non-loop
        // callers; it delegates to the new overload with an empty list, so
        // the projection still carries the already_called slot (as an empty
        // array) — projection shape stability is preserved across callers.
        BotSession session = buildSession();
        PhasePlan plan = resolvePlan();

        String projection = builder.build(session, List.of(), plan, "x", null);
        JsonNode root = objectMapper.readTree(projection);
        assertTrue(root.has("already_called"),
                "Legacy build() overload must still emit already_called for shape stability");
        assertEquals(0, root.get("already_called").size());
    }

    // --- Helpers ---

    private BotSession buildSession() {
        return BotSession.builder()
                .sessionId("sprint20-trackb-test")
                .activeUseCase("UC-A")
                .currentPhase("RESOLVE")
                .handlingState("BOT_HANDLING")
                .totalBotTurns(0)
                .clarificationCount(0)
                .faqMissCount(0)
                .repeatedActionCount(0)
                .build();
    }

    private PhasePlan resolvePlan() {
        return PhasePlan.builder()
                .phase("RESOLVE")
                .useCase("UC-A")
                .objective("answer")
                .allowedTools(List.of("search_knowledge", "resolve_article",
                        "request_handover", "record_outcome"))
                .maxToolSteps(3)
                .systemInstruction("RESOLVE")
                .validTerminalOutcomes(Set.of(TerminalOutcome.FINAL_ANSWER,
                        TerminalOutcome.ESCALATE))
                .build();
    }
}
