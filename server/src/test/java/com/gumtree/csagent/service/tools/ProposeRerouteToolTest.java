package com.gumtree.csagent.service.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gumtree.csagent.model.BotEvent;
import com.gumtree.csagent.model.BotSession;
import com.gumtree.csagent.model.enums.EventType;
import com.gumtree.csagent.repository.BotEventRepository;
import com.gumtree.csagent.repository.BotSessionRepository;
import com.gumtree.csagent.service.runtime.UseCaseRegistryService;
import com.gumtree.csagent.service.runtime.skill.Skill;
import com.gumtree.csagent.service.runtime.skill.SkillRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Sprint 103 / WS-6-A — unit tests for {@link ProposeRerouteTool}.
 *
 * <p>These pin the tool's <em>capability</em> contract only: what it accepts,
 * what it moves, and what it declines. Per iteration_governance §5.7 they are
 * NOT evidence that the LLM re-routes on a drifted conversation — that claim
 * needs a real-LLM run and is recorded in the sprint handoff.
 *
 * <p>The load-bearing assertion for §1.3 is
 * {@link #execute_honoursAnyKnownServedTarget_regardlessOfSemantics}: the tool
 * must not second-guess whether the proposed UC "fits" the conversation.
 */
@ExtendWith(MockitoExtension.class)
class ProposeRerouteToolTest {

    @Mock private UseCaseRegistryService useCaseRegistry;
    @Mock private SkillRegistry skillRegistry;
    @Mock private BotSessionRepository botSessionRepository;
    @Mock private BotEventRepository botEventRepository;
    @Mock private Skill someSkill;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private ProposeRerouteTool tool;

    @BeforeEach
    void setUp() {
        tool = new ProposeRerouteTool(useCaseRegistry, skillRegistry,
                botSessionRepository, botEventRepository, objectMapper);
    }

    @Test
    void getName_returnsProposeReroute() {
        assertEquals("propose_reroute", tool.getName());
    }

    @Test
    void execute_honouredReroute_movesSessionAndEmitsEvent() {
        BotSession session = newSession("UC-D");
        when(useCaseRegistry.isKnownUseCase("UC-H")).thenReturn(true);
        when(skillRegistry.select("RESOLVE", "UC-H")).thenReturn(Optional.of(someSkill));

        ToolResult result = tool.execute(session, args("UC-H", "customer switched to an appeal"));

        assertTrue(result.isSuccess());
        assertEquals(Boolean.TRUE, result.getData().get("honoured"));
        assertEquals("UC-H", result.getData().get("target_use_case"));
        assertEquals("UC-D", result.getData().get("previous_use_case"));
        assertEquals("RESOLVE", result.getData().get("landing_phase"));

        assertEquals("UC-H", session.getActiveUseCase());
        assertEquals("UC-D", session.getPreviousActiveUseCase(),
                "the previous UC must survive the switch — prior_use_case_carry reads it");
        verify(botSessionRepository).save(session);

        ArgumentCaptor<BotEvent> event = ArgumentCaptor.forClass(BotEvent.class);
        verify(botEventRepository).save(event.capture());
        assertEquals(EventType.CLASSIFICATION_COMMITTED.name(), event.getValue().getEventType());
        String payload = event.getValue().getPayload();
        assertTrue(payload.contains("\"via\":\"propose_reroute\""),
                "the `via` discriminator is what separates a mid-session re-route from an "
                        + "intake-time classify_use_case commit in the trace: " + payload);
        assertTrue(payload.contains("\"previous_use_case_id\":\"UC-D\""), payload);
    }

    @Test
    void execute_honoursAnyKnownServedTarget_regardlessOfSemantics() {
        // §1.3: the runtime does not adjudicate whether the customer's new ask
        // "really" belongs to the proposed UC. A target that looks unrelated to
        // the active UC is still honoured — the LLM owns that judgement, and a
        // runtime that second-guessed it would be the per-UC matrix §1.7 bars.
        BotSession session = newSession("UC-A");
        when(useCaseRegistry.isKnownUseCase("UC-J")).thenReturn(true);
        when(skillRegistry.select("RESOLVE", "UC-J")).thenReturn(Optional.of(someSkill));

        ToolResult result = tool.execute(session, args("UC-J", "whatever the model reasoned"));

        assertEquals(Boolean.TRUE, result.getData().get("honoured"));
        assertEquals("UC-J", session.getActiveUseCase());
    }

    @Test
    void execute_unknownUseCase_returnsError() {
        BotSession session = newSession("UC-D");
        when(useCaseRegistry.isKnownUseCase("UC-ZZ")).thenReturn(false);

        ToolResult result = tool.execute(session, args("UC-ZZ", "made up"));

        assertFalse(result.isSuccess());
        assertTrue(result.getErrorMessage().startsWith("unknown_use_case"));
        assertEquals("UC-D", session.getActiveUseCase());
        verify(botSessionRepository, never()).save(any());
    }

    @Test
    void execute_targetAlreadyActive_declinesWithoutTouchingSession() {
        BotSession session = newSession("UC-D");
        when(useCaseRegistry.isKnownUseCase("UC-D")).thenReturn(true);

        ToolResult result = tool.execute(session, args("UC-D", "same one"));

        assertTrue(result.isSuccess(), "a decline is information, not an error");
        assertEquals(Boolean.FALSE, result.getData().get("honoured"));
        assertEquals("already_active", result.getData().get("reason"));
        assertEquals("UC-D", session.getActiveUseCase());
        assertNull(session.getPreviousActiveUseCase());
        verify(botSessionRepository, never()).save(any());
        verify(botEventRepository, never()).save(any());
    }

    @Test
    void execute_noSkillServesTarget_declinesWithoutTouchingSession() {
        // Capability constraint, not a semantic one: a re-route lands in
        // RESOLVE, so without a Skill for (RESOLVE, target) the same-turn
        // replan would have a null plan to run.
        BotSession session = newSession("UC-D");
        when(useCaseRegistry.isKnownUseCase("UC-H")).thenReturn(true);
        when(skillRegistry.select("RESOLVE", "UC-H")).thenReturn(Optional.empty());

        ToolResult result = tool.execute(session, args("UC-H", "appeal"));

        assertTrue(result.isSuccess());
        assertEquals(Boolean.FALSE, result.getData().get("honoured"));
        assertEquals("no_skill_for_target_use_case", result.getData().get("reason"));
        assertEquals("UC-D", session.getActiveUseCase());
        verify(botSessionRepository, never()).save(any());
    }

    @Test
    void execute_missingTargetUseCase_returnsError() {
        BotSession session = newSession("UC-D");

        assertFalse(tool.execute(session, new HashMap<>()).isSuccess());
        assertFalse(tool.execute(session, args(null, "x")).isSuccess());
        assertFalse(tool.execute(session, args("   ", "x")).isSuccess());
        assertEquals("UC-D", session.getActiveUseCase());
    }

    @Test
    void execute_nullParameters_returnsError() {
        assertFalse(tool.execute(newSession("UC-D"), null).isSuccess());
    }

    private static Map<String, Object> args(String targetUc, String reasoning) {
        Map<String, Object> args = new HashMap<>();
        if (targetUc != null) {
            args.put("target_use_case", targetUc);
        }
        args.put("reasoning", reasoning);
        return args;
    }

    private static BotSession newSession(String activeUc) {
        BotSession session = new BotSession();
        session.setSessionId("sess-reroute-1");
        session.setCurrentPhase("RESOLVE");
        session.setActiveUseCase(activeUc);
        session.setTotalBotTurns(3);
        return session;
    }
}
