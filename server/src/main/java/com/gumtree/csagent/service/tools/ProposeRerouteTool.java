package com.gumtree.csagent.service.tools;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gumtree.csagent.model.BotEvent;
import com.gumtree.csagent.model.BotSession;
import com.gumtree.csagent.model.enums.EventType;
import com.gumtree.csagent.repository.BotEventRepository;
import com.gumtree.csagent.repository.BotSessionRepository;
import com.gumtree.csagent.service.runtime.UseCaseRegistryService;
import com.gumtree.csagent.service.runtime.skill.SkillRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Sprint 103 / WS-6-A — lets the LLM move the session to a different use case
 * mid-conversation, from RESOLVE or CONFIRM.
 *
 * <p><strong>Why this exists.</strong> Before this tool the only way the
 * active use case could change after DISCOVER was runtime pattern matching
 * ({@code DriftDetector}'s keyword groups + {@code RuntimeIntentClassifier}'s
 * regexes). {@code classify_use_case} is scoped to the DISCOVER plan, so
 * outside DISCOVER the LLM could neither see nor call it, and a customer who
 * raised a second, different need mid-conversation could only be answered
 * inside the original use case's tool whitelist — or handed over when that
 * failed. Drift / topic shift is LLM-owned per Constitution §1.3; this tool is
 * the capability half of that ownership. See
 * {@code docs/diagnostics/failure-briefs/ws5-2026-07-25-intent-switch-forces-escalation.md}.
 *
 * <p><strong>Why not re-use {@code classify_use_case}.</strong> Two code
 * reasons, both load-bearing:
 * <ol>
 *   <li>{@link ClassifyUseCaseTool#shouldPreserveStrongPrior} refuses any
 *       {@code use_case_id} that differs from an active UC which the
 *       deterministic form-context router would still derive. On a
 *       strong-prior topic (e.g. "Replies or Messaging" → UC-C) that guard
 *       fires on exactly the mid-session shift this sprint targets, and the
 *       refusal is silent ({@code committed: false}).</li>
 *   <li>{@code classify_use_case}'s commit semantics are wired to DISCOVER:
 *       {@code AgentRunLoopImpl} only returns {@code USE_CASE_IDENTIFIED} on a
 *       DISCOVER plan, and {@code ControlKernel} only replans on that pairing.
 *       Called mid-RESOLVE it would mutate {@code session.activeUseCase} while
 *       the loop kept dispatching against the previous UC's plan — the
 *       per-UC {@code ToolPolicyEnforcer} check would see the new UC and the
 *       {@code validateAgainstPlan} whitelist the old one.</li>
 * </ol>
 * A distinct tool keeps DISCOVER's proven commit path untouched, and keeps an
 * intake-time commit distinguishable from a mid-session re-route in the trace.
 *
 * <p><strong>What the runtime decides, and what it does not.</strong> The
 * runtime honours or declines a proposal on <em>capability</em> grounds only —
 * is the target a known use case, is it actually different, and does a Skill
 * exist to serve it. It never judges whether the customer's new ask "really"
 * belongs to the proposed UC: that is the semantic judgement §1.3 assigns to
 * the LLM. There is no keyword, regex, or per-UC branch in this class.
 *
 * <p>A declined proposal is returned as a normal tool result with
 * {@code honoured: false} and a reason, so the LLM can pick a different target
 * or carry on; it is not an error and it does not end the turn.
 */
@Slf4j
@Component
public class ProposeRerouteTool implements Tool {

    /**
     * The phase a honoured re-route lands in. RESOLVE is where every use case
     * has a Skill that can actually act on the customer's new ask, and both
     * {@code RESOLVE → RESOLVE} (no transition) and {@code CONFIRM → RESOLVE}
     * (already in {@code control-policy.yaml}) are legal, so no new phase edge
     * is introduced. See {@code phase0_normative_freeze.md} §0.6.
     */
    public static final String REROUTE_LANDING_PHASE = "RESOLVE";

    private final UseCaseRegistryService useCaseRegistry;
    private final SkillRegistry skillRegistry;
    private final BotSessionRepository botSessionRepository;
    private final BotEventRepository botEventRepository;
    private final ObjectMapper objectMapper;

    public ProposeRerouteTool(UseCaseRegistryService useCaseRegistry,
                              SkillRegistry skillRegistry,
                              BotSessionRepository botSessionRepository,
                              BotEventRepository botEventRepository,
                              ObjectMapper objectMapper) {
        this.useCaseRegistry = useCaseRegistry;
        this.skillRegistry = skillRegistry;
        this.botSessionRepository = botSessionRepository;
        this.botEventRepository = botEventRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public String getName() {
        return "propose_reroute";
    }

    @Override
    public ToolResult execute(BotSession session, Map<String, Object> parameters) {
        if (session == null) {
            return ToolResult.error("Session is required");
        }
        if (parameters == null) {
            return ToolResult.error("Parameters are required: target_use_case, reasoning");
        }

        String targetUc = asString(parameters.get("target_use_case"));
        if (targetUc == null || targetUc.isBlank()) {
            return ToolResult.error("Parameter 'target_use_case' is required");
        }
        targetUc = targetUc.trim();
        if (!useCaseRegistry.isKnownUseCase(targetUc)) {
            return ToolResult.error("unknown_use_case: " + targetUc);
        }

        String reasoning = asString(parameters.get("reasoning"));

        String currentUc = session.getActiveUseCase();
        if (targetUc.equals(currentUc)) {
            return declined(targetUc, currentUc, "already_active");
        }

        // Capability check: a re-route lands in RESOLVE, so a Skill must exist
        // for (RESOLVE, target). Without one PhaseEvaluator.plan(...) returns
        // null and the same-turn replan has nothing to run. Registry-driven —
        // no per-UC list is maintained here.
        if (skillRegistry.select(REROUTE_LANDING_PHASE, targetUc).isEmpty()) {
            log.warn("propose_reroute: no Skill serves ({}, {}) — declining proposal on session='{}'",
                    REROUTE_LANDING_PHASE, targetUc, session.getSessionId());
            return declined(targetUc, currentUc, "no_skill_for_target_use_case");
        }

        session.setPreviousActiveUseCase(currentUc);
        session.setActiveUseCase(targetUc);
        session.setUpdatedAt(OffsetDateTime.now());
        botSessionRepository.save(session);

        emitEvent(session, targetUc, currentUc, reasoning);

        log.info("propose_reroute honoured: session='{}', {} -> {}",
                session.getSessionId(), currentUc, targetUc);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("honoured", true);
        data.put("target_use_case", targetUc);
        data.put("previous_use_case", currentUc);
        data.put("landing_phase", REROUTE_LANDING_PHASE);
        return ToolResult.ok(data);
    }

    private ToolResult declined(String targetUc, String currentUc, String reason) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("honoured", false);
        data.put("target_use_case", targetUc);
        data.put("active_use_case", currentUc);
        data.put("reason", reason);
        return ToolResult.ok(data);
    }

    /**
     * Emit the re-route as a {@link EventType#CLASSIFICATION_COMMITTED} event
     * carrying {@code via: propose_reroute} + the previous UC. Re-using the
     * existing event type keeps every downstream consumer that counts
     * classification events working unchanged; the {@code via} discriminator
     * is what makes a mid-session re-route queryable apart from an
     * intake-time commit.
     */
    private void emitEvent(BotSession session, String targetUc, String previousUc, String reasoning) {
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("use_case_id", targetUc);
            payload.put("previous_use_case_id", previousUc);
            payload.put("via", getName());
            if (reasoning != null && !reasoning.isBlank()) {
                payload.put("reasoning", reasoning);
            }
            BotEvent event = BotEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .sessionId(session.getSessionId())
                    .eventType(EventType.CLASSIFICATION_COMMITTED.name())
                    .turnIndex(session.getTotalBotTurns())
                    .payload(objectMapper.writeValueAsString(payload))
                    .createdAt(OffsetDateTime.now())
                    .build();
            botEventRepository.save(event);
        } catch (JsonProcessingException ex) {
            log.warn("propose_reroute: failed to serialize CLASSIFICATION_COMMITTED payload: {}",
                    ex.getMessage());
        }
    }

    private static String asString(Object value) {
        return value == null ? null : value.toString();
    }
}
