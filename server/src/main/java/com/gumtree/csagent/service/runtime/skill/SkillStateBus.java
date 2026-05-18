package com.gumtree.csagent.service.runtime.skill;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gumtree.csagent.model.BotSession;
import com.gumtree.csagent.service.runtime.IntakeFieldsRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Session-level state-bus mediating cross-Skill state preservation on UC
 * switch, per Sprint 37 freeze decision (i) §10 + Sprint 41 D-a.
 *
 * <p>The bus is invoked at the single Skill-switch boundary (when
 * {@code PhaseEvaluator.plan(...)} detects {@code session.getActiveUseCase()}
 * has changed since the prior turn AND the new {@code select(phase, newUc)}
 * returns a different Skill from the prior turn's active Skill). It reads the
 * new Skill's {@link StateInheritance} declaration and applies it to the
 * session per the dimension semantics in §10.3:
 *
 * <ul>
 *   <li><b>{@code inherit}</b> — state passes through unchanged. For the
 *       {@code intake_fields_partial} dimension, the bus applies the
 *       registry-level intersection rule (per §10.3 + OLD Sprint 36 D5
 *       §6.1.C carried forward): keep only the fields whose canonical name
 *       is in {@link IntakeFieldsRegistry#requiredFieldsFor(String)} for the
 *       new active UC. The rule is a SINGLE registry-driven check; the bus
 *       body has NO per-UC-pair branch (§1.7).</li>
 *   <li><b>{@code reset}</b> — clear the state in the session.</li>
 *   <li><b>{@code soft_signal_via_projection}</b> — no-op at the bus level.
 *       {@link com.gumtree.csagent.service.runtime.ContextProjectionBuilder}
 *       emits the named slot unconditionally when its content conditions
 *       are met (per §N0 nullable-field convention); the declaration is
 *       documentation of which Skills consume the slot.</li>
 * </ul>
 *
 * <p>Allowed state keys are enforced at Skill-load time by
 * {@link SkillLoader#VALID_STATE_KEYS} per §10.2 schema (three keys:
 * {@code customer_context}, {@code accumulated_tool_results},
 * {@code intake_fields_partial}). New state keys require explicit
 * deliver-agent + human review at M3+ planning round.
 *
 * <p>§1.3 / §1.4 boundary: this class is Runtime-owned capability-floor
 * enforcement (which session-state dimensions carry vs reset). It does NOT
 * make semantic decisions; the LLM still owns the read of any soft signal
 * via the per-turn projection.
 *
 * <p>C3 Tier-0 candidate (§10.10): when {@code PhaseEvaluator} selects a
 * new Skill on UC switch, the bus applies the new Skill's
 * {@link StateInheritance} unconditionally; the LLM cannot "inherit" or
 * "override" state the new Skill declares as {@code reset}. The bus is
 * the single enforcement point for cross-Skill state preservation. Sprint
 * 41 ships the first observed evidence surface; deliver-agent + human +
 * Codex jointly evaluate elevation timing at Sprint 41 close OR M2 close
 * per {@code feedback_constitution_discipline_vs_planning_anticipation.md}.
 */
@Slf4j
@Component
public class SkillStateBus {

    private final ObjectMapper objectMapper;

    public SkillStateBus(ObjectMapper objectMapper) {
        this.objectMapper = Objects.requireNonNull(objectMapper,
                "ObjectMapper is required for SkillStateBus intake_fields_partial "
                        + "serialization round-trip.");
    }

    /**
     * Applied at the Skill-switch boundary. Reads the new Skill's
     * {@link StateInheritance} declaration and applies it to the session
     * per §10.3 dimension semantics. {@code priorSkill} is informational
     * (used in trace / diagnostics); the application is driven by
     * {@code newSkill}'s declaration alone.
     *
     * <p>This is a no-op when {@code newSkill} is null. It is also a no-op
     * when both skills have the same {@link Skill#name()} (defensive — the
     * caller should not invoke the bus on a same-Skill turn, but the
     * defensive check guards against double-application during fix-iteration).
     */
    public void applyOnSkillSwitch(Skill priorSkill, Skill newSkill, BotSession session) {
        if (newSkill == null || session == null) {
            return;
        }
        if (priorSkill != null && Objects.equals(priorSkill.name(), newSkill.name())) {
            return;
        }

        StateInheritance si = newSkill.stateInheritance();
        if (si == null) {
            si = StateInheritance.EMPTY;
        }

        // Reset dimensions: clear the state outright.
        for (String key : si.reset()) {
            switch (key) {
                case "customer_context" -> session.setCustomerContext(null);
                case "intake_fields_partial" -> session.setIntakeFields(null);
                case "accumulated_tool_results" -> {
                    // No persistent session field for accumulated_tool_results;
                    // the per-loop accumulator (AgentRunLoopImpl) is reset
                    // naturally on the next loop start. Declaration is
                    // documentation; no session-level action required.
                }
                default -> log.warn("SkillStateBus: unexpected reset key '{}' on Skill '{}' "
                        + "(schema validation should have caught this at load time)",
                        key, newSkill.name());
            }
        }

        // Inherit dimensions: pass-through, except intake_fields_partial
        // which applies the registry-driven intersection rule per §10.3.
        for (String key : si.inherit()) {
            switch (key) {
                case "intake_fields_partial" -> applyIntakeFieldsIntersection(session);
                case "customer_context", "accumulated_tool_results" -> {
                    // Pass-through: state survives unchanged.
                }
                default -> log.warn("SkillStateBus: unexpected inherit key '{}' on Skill '{}' "
                        + "(schema validation should have caught this at load time)",
                        key, newSkill.name());
            }
        }

        // soft_signal_via_projection: no-op at bus level. The
        // ContextProjectionBuilder emits the named slot unconditionally
        // when its content conditions are met per §N0 nullable-field
        // convention.

        if (log.isDebugEnabled()) {
            log.debug("SkillStateBus: applied state_inheritance on switch session={} priorSkill={} "
                            + "newSkill={} inherit={} reset={} softSignal={}",
                    session.getSessionId(),
                    priorSkill == null ? null : priorSkill.name(),
                    newSkill.name(),
                    si.inherit(), si.reset(), si.softSignalViaProjection());
        }
    }

    /**
     * Diagnostic accessor — returns the inheritance dimensions for a Skill.
     * Never null; an unset {@code state_inheritance} on the YAML normalises
     * to {@link StateInheritance#EMPTY} at the {@link Skill} record's compact
     * constructor.
     */
    public StateInheritance inheritanceFor(Skill skill) {
        if (skill == null || skill.stateInheritance() == null) {
            return StateInheritance.EMPTY;
        }
        return skill.stateInheritance();
    }

    /**
     * Registry-driven intersection rule per design doc §10.3 + OLD Sprint 36
     * D5 §6.1.C carried forward: keep only the intake fields whose canonical
     * name is in {@link IntakeFieldsRegistry#requiredFieldsFor(String)} for
     * the session's CURRENT active UC. The rule is a SINGLE registry-driven
     * check; the bus body has NO per-UC-pair branch.
     *
     * <p>When the active UC is not an intake UC, {@code requiredFieldsFor}
     * returns an empty list, and all collected fields are dropped — that
     * is the desired behaviour for a Skill that inherits
     * {@code intake_fields_partial} but lands on a non-intake UC.
     */
    private void applyIntakeFieldsIntersection(BotSession session) {
        String rawIntakeFields = session.getIntakeFields();
        if (rawIntakeFields == null || rawIntakeFields.isBlank()) {
            return;
        }
        String activeUc = session.getActiveUseCase();
        List<String> required = IntakeFieldsRegistry.requiredFieldsFor(activeUc);
        Map<String, String> collected = IntakeFieldsRegistry.parseCollectedFields(
                objectMapper, rawIntakeFields);

        Map<String, String> survivors = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : collected.entrySet()) {
            String canonical = IntakeFieldsRegistry.canonicalFieldName(entry.getKey());
            if (required.contains(canonical)) {
                survivors.put(canonical, entry.getValue());
            }
        }

        if (survivors.isEmpty()) {
            session.setIntakeFields(null);
            return;
        }
        try {
            session.setIntakeFields(objectMapper.writeValueAsString(survivors));
        } catch (Exception ex) {
            // Defensive: if serialization fails (should not — survivors is a
            // plain string-keyed string-valued map), drop the field rather
            // than persist a stale value.
            log.warn("SkillStateBus: failed to serialize intake_fields_partial survivors "
                    + "for session={} activeUc={}; dropping the field",
                    session.getSessionId(), activeUc, ex);
            session.setIntakeFields(null);
        }
    }
}
