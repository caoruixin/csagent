package com.gumtree.csagent.service.runtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gumtree.csagent.model.BotSession;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Sprint 080 / S-Auto-25 / M-Auto-6 Sub-sprint C-1 (R7, path β) —
 * characterization of {@link IntakeFieldsMerger}, the helper extracted from
 * {@code AgentRunLoopImpl.persistInlineIntakeFields}.
 *
 * <p>These tests pin the EXACT observable contract of the pre-refactor inline
 * merge so the extraction is byte-equivalent on the {@code request_handover}
 * persist path:
 * <ul>
 *   <li>parse existing {@code session.intakeFields} → merge incoming
 *       (incoming WINS on conflict) → write back only when the merged map
 *       differs from the existing one;</li>
 *   <li>field names normalised to canonical form (aliases collapsed);</li>
 *   <li>blank / null incoming values dropped;</li>
 *   <li>null session / empty incoming are no-ops.</li>
 * </ul>
 */
class IntakeFieldsMergerCharacterizationTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private BotSession session(String intakeFieldsJson) {
        BotSession s = new BotSession();
        s.setSessionId("ifm");
        s.setIntakeFields(intakeFieldsJson);
        return s;
    }

    private Map<String, String> collected(BotSession s) {
        return IntakeFieldsRegistry.parseCollectedFields(objectMapper, s.getIntakeFields());
    }

    @Test
    void emptyExisting_persistsIncoming() {
        BotSession s = session(null);

        IntakeFieldsMerger.Result r = IntakeFieldsMerger.merge(
                s, Map.of("report_type", "scam"), objectMapper);

        assertTrue(r.changed());
        assertEquals(java.util.List.of("report_type"), r.persistedFields());
        assertEquals("scam", collected(s).get("report_type"));
    }

    @Test
    void unionMerge_keepsBothFields() {
        BotSession s = session("{\"report_target\":\"AD-7\"}");

        IntakeFieldsMerger.merge(s, Map.of("report_type", "scam"), objectMapper);

        Map<String, String> collected = collected(s);
        assertEquals("AD-7", collected.get("report_target"));
        assertEquals("scam", collected.get("report_type"));
    }

    @Test
    void incomingWinsOnConflict() {
        // Pre-refactor contract: IntakeFieldsRegistry.mergeFields puts incoming
        // over existing, so a re-supplied field overwrites the stored value.
        BotSession s = session("{\"report_type\":\"old\"}");

        IntakeFieldsMerger.Result r = IntakeFieldsMerger.merge(
                s, Map.of("report_type", "new"), objectMapper);

        assertTrue(r.changed());
        assertEquals("new", collected(s).get("report_type"));
    }

    @Test
    void aliasCanonicalised() {
        BotSession s = session(null);

        IntakeFieldsMerger.merge(s, Map.of("type", "scam"), objectMapper);

        assertEquals("scam", collected(s).get("report_type"));
    }

    @Test
    void noChange_whenAllAlreadyPresent_doesNotRewrite() {
        BotSession s = session("{\"report_type\":\"scam\"}");
        String before = s.getIntakeFields();

        IntakeFieldsMerger.Result r = IntakeFieldsMerger.merge(
                s, Map.of("report_type", "scam"), objectMapper);

        assertFalse(r.changed());
        assertEquals(before, s.getIntakeFields(), "no write when merged equals existing");
    }

    @Test
    void blankValueDropped() {
        BotSession s = session(null);
        Map<String, Object> incoming = new LinkedHashMap<>();
        incoming.put("report_type", "");

        IntakeFieldsMerger.Result r = IntakeFieldsMerger.merge(s, incoming, objectMapper);

        assertFalse(r.changed());
        assertTrue(r.persistedFields().isEmpty());
        assertTrue(collected(s).isEmpty());
    }

    @Test
    void nullSession_isNoOp() {
        IntakeFieldsMerger.Result r = IntakeFieldsMerger.merge(
                null, Map.of("report_type", "scam"), objectMapper);
        assertFalse(r.changed());
        assertTrue(r.persistedFields().isEmpty());
    }

    @Test
    void emptyIncoming_isNoOp() {
        BotSession s = session("{\"report_type\":\"scam\"}");
        String before = s.getIntakeFields();

        IntakeFieldsMerger.Result r = IntakeFieldsMerger.merge(s, Map.of(), objectMapper);

        assertFalse(r.changed());
        assertEquals(before, s.getIntakeFields());
    }

    @Test
    void persistedBlob_roundTripsToMergedMap() {
        BotSession s = session("{\"report_target\":\"AD-7\"}");

        IntakeFieldsMerger.merge(s, Map.of("report_type", "scam", "description", "phishing"),
                objectMapper);

        Map<String, String> collected = collected(s);
        assertEquals("AD-7", collected.get("report_target"));
        assertEquals("scam", collected.get("report_type"));
        assertEquals("phishing", collected.get("description"));
        assertTrue(IntakeFieldsRegistry.intakeComplete("UC-J", collected));
    }
}
