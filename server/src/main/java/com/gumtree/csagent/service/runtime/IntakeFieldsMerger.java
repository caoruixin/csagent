package com.gumtree.csagent.service.runtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gumtree.csagent.model.BotSession;

import java.util.List;
import java.util.Map;

/**
 * Sprint 080 / S-Auto-25 / M-Auto-6 Sub-sprint C-1 (R7) — shared intake-field
 * merge helper.
 *
 * <p>Extracted (path β) from
 * {@code AgentRunLoopImpl.persistInlineIntakeFields} so the
 * {@code request_handover} inline-persist path AND the new
 * {@code update_intake_fields} tool persist collected intake values through a
 * single, byte-equivalent code path. The merge primitives themselves live in
 * {@link IntakeFieldsRegistry}; this helper only sequences
 * parse → merge → persist and reports which canonical field names were
 * supplied. No semantic decision is made here — it never inspects the active
 * use case or the tool name.
 */
public final class IntakeFieldsMerger {

    private IntakeFieldsMerger() {
        // static-only helper
    }

    /**
     * Outcome of a merge.
     *
     * @param persistedFields the canonical names of the (non-blank) incoming
     *                        fields supplied this call, in incoming order.
     * @param changed         true iff the persisted {@code session.intakeFields}
     *                        blob actually changed (false when every supplied
     *                        value was already present, or on serialization
     *                        failure).
     */
    public record Result(List<String> persistedFields, boolean changed) {}

    /**
     * Merge {@code incoming} field values into {@code session.intakeFields},
     * normalising field names to canonical form via
     * {@link IntakeFieldsRegistry#canonicalFieldName(String)}. Mirrors the
     * pre-refactor {@code persistInlineIntakeFields} contract exactly: the
     * existing blob is parsed, merged with the incoming values
     * ({@link IntakeFieldsRegistry#mergeFields(Map, Map)}), and only written
     * back when the merged map differs from the existing one. Tolerant — never
     * throws; a serialization failure leaves the session unchanged and is
     * reported as {@code changed=false}.
     *
     * @return the canonical names of the supplied (non-blank) incoming fields
     *         and whether the persisted blob changed.
     */
    public static Result merge(BotSession session, Map<String, ?> incoming,
                               ObjectMapper objectMapper) {
        if (session == null || incoming == null || incoming.isEmpty()) {
            return new Result(List.of(), false);
        }
        Map<String, String> existing = IntakeFieldsRegistry.parseCollectedFields(
                objectMapper, session.getIntakeFields());
        Map<String, String> merged = IntakeFieldsRegistry.mergeFields(existing, incoming);
        // Canonical names actually supplied this call (non-blank values only),
        // for structural confirmation on the tool result. Reusing mergeFields
        // against an empty base applies the same normalisation + blank-drop.
        List<String> supplied = List.copyOf(
                IntakeFieldsRegistry.mergeFields(null, incoming).keySet());
        if (merged.equals(existing)) {
            return new Result(supplied, false);
        }
        try {
            session.setIntakeFields(objectMapper.writeValueAsString(merged));
            return new Result(supplied, true);
        } catch (Exception ex) {
            return new Result(supplied, false);
        }
    }
}
