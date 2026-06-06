package com.gumtree.csagent.service.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gumtree.csagent.model.BotSession;
import com.gumtree.csagent.service.runtime.IntakeFieldsMerger;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Sprint 080 / S-Auto-25 / M-Auto-6 Sub-sprint C-1 (R7) — no-side-effect
 * intake-field accumulation tool.
 *
 * <p>Lets the LLM persist partial intake fields it has collected from the user
 * so they survive across turns, WITHOUT triggering the handover validator. The
 * R1.a {@code request_handover(intake_fields={...})} path covers the
 * "send everything at once" strategy; this tool covers the "send
 * incrementally" strategy (the c14 / UC-J shape where the bot collects one
 * field per turn and otherwise loses the earlier ones).
 *
 * <p>The tool is deliberately thin and Runtime-owned: it does structural
 * validation only (a non-empty {@code fields} map) and delegates the merge to
 * the shared {@link IntakeFieldsMerger} so the persisted state is identical to
 * the {@code request_handover} persist path. It makes NO semantic decision —
 * it never inspects the active use case, never derives the call from message
 * content, and never bypasses the {@code intake_complete_required} guardrail
 * (handover completeness remains gated entirely by the validator on the
 * {@code request_handover} path).
 */
@Slf4j
@Component
public class UpdateIntakeFieldsTool implements Tool {

    private final ObjectMapper objectMapper;

    public UpdateIntakeFieldsTool(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public String getName() {
        return "update_intake_fields";
    }

    @Override
    public ToolResult execute(BotSession session, Map<String, Object> parameters) {
        // Structural validation only (NOT semantic): `fields` must be a
        // non-empty object. A null / missing / non-map / empty argument is a
        // malformed call and is rejected without mutating session state.
        Object raw = parameters == null ? null : parameters.get("fields");
        if (!(raw instanceof Map<?, ?> rawMap) || rawMap.isEmpty()) {
            return ToolResult.error(
                    "update_intake_fields requires a non-empty 'fields' object "
                            + "(a map of collected intake field name -> value)");
        }

        @SuppressWarnings("unchecked")
        Map<String, ?> incoming = (Map<String, ?>) rawMap;
        IntakeFieldsMerger.Result result =
                IntakeFieldsMerger.merge(session, incoming, objectMapper);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("status", "ok");
        data.put("fields_merged", result.persistedFields().size());
        data.put("fields_persisted", result.persistedFields());

        log.info("update_intake_fields persisted {} field(s) for session '{}': {}",
                result.persistedFields().size(),
                session == null ? "?" : session.getSessionId(),
                result.persistedFields());
        return ToolResult.ok(data);
    }
}
