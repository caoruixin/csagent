package com.gumtree.csagent.service.runtime;

import com.gumtree.csagent.model.BotSession;
import com.gumtree.csagent.model.DriftResult;
import com.gumtree.csagent.model.DriftResult.DriftType;
import com.gumtree.csagent.service.runtime.RiskKeywordsConfig.HardShiftGroup;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Sprint 15 §M0 — config validation and behaviour parity tests for
 * {@link RiskKeywordsConfig} and {@link DriftDetector}.
 *
 * <p>The parity reference is the previously hardcoded
 * {@code DriftDetector.HARD_SHIFT_KEYWORDS} list and
 * {@code DriftDetector.ESCALATION_PATTERN} regex. Each parity case
 * exercises the YAML-loaded path on a representative input that the
 * old hardcoded path also covered, asserting identical
 * {@link DriftResult} output (type, escalationRequested, newUseCase).
 */
class Sprint15RiskKeywordsConfigTest {

    // ---------------------------------------------------------------
    // Config validation
    // ---------------------------------------------------------------

    @Test
    void defaultConfig_loadsAndValidates() throws IOException {
        RiskKeywordsConfig config = RiskKeywordsConfig.loadDefault();

        assertNotNull(config.getEscalationPattern(),
                "escalation pattern must compile from default config");
        assertEquals(5, config.getHardShiftGroups().size(),
                "default config must declare exactly 5 hard-shift groups (parity with hardcoded list)");
        assertEquals(1, config.getVersion(), "default config version must be 1");
    }

    @Test
    void defaultConfig_keywordSetMatchesHardcodedReference() throws IOException {
        // Reference parity table: the exact keyword groups that used to
        // live in DriftDetector.HARD_SHIFT_KEYWORDS, in declaration order.
        List<HardShiftGroup> expected = List.of(
                new HardShiftGroup("UC-J",
                        List.of("scam", "scammed", "fraud", "fraudulent")),
                new HardShiftGroup("UC-G",
                        List.of("delete my data", "delete my account", "gdpr",
                                "data deletion", "right to be forgotten")),
                new HardShiftGroup("UC-I",
                        List.of("refund", "money back", "charge back", "chargeback",
                                "dispute payment")),
                new HardShiftGroup("UC-J",
                        List.of("unsafe", "harassment", "threatening", "danger", "abusive")),
                new HardShiftGroup("UC-H",
                        List.of("ad removed", "ad deleted", "ad taken down",
                                "why was my ad removed", "appeal"))
        );

        RiskKeywordsConfig config = RiskKeywordsConfig.loadDefault();
        List<HardShiftGroup> actual = config.getHardShiftGroups();

        assertEquals(expected.size(), actual.size(),
                "group count parity vs hardcoded reference");
        for (int i = 0; i < expected.size(); i++) {
            assertEquals(expected.get(i).targetUseCase(), actual.get(i).targetUseCase(),
                    "group #" + i + " target UC parity");
            assertEquals(expected.get(i).keywords(), actual.get(i).keywords(),
                    "group #" + i + " keyword list parity (order included)");
        }
    }

    @Test
    void config_missingEscalationPattern_failsFast() {
        String yaml = """
                version: 1
                hard-shift-groups:
                  - target-use-case: UC-J
                    keywords: [foo]
                """;
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> RiskKeywordsConfig.loadFrom(toStream(yaml)));
        assertTrue(ex.getMessage().contains("escalation-pattern"),
                "error must name the missing field");
    }

    @Test
    void config_emptyHardShiftGroups_failsFast() {
        String yaml = """
                version: 1
                escalation-pattern: "x"
                hard-shift-groups: []
                """;
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> RiskKeywordsConfig.loadFrom(toStream(yaml)));
        assertTrue(ex.getMessage().contains("hard-shift-groups"),
                "error must name the empty list field");
    }

    @Test
    void config_emptyKeywordsInGroup_failsFast() {
        String yaml = """
                version: 1
                escalation-pattern: "x"
                hard-shift-groups:
                  - target-use-case: UC-J
                    keywords: []
                """;
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> RiskKeywordsConfig.loadFrom(toStream(yaml)));
        assertTrue(ex.getMessage().contains("keywords"),
                "error must name the empty keywords list");
    }

    @Test
    void config_blankTargetUseCase_failsFast() {
        String yaml = """
                version: 1
                escalation-pattern: "x"
                hard-shift-groups:
                  - target-use-case: ""
                    keywords: [foo]
                """;
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> RiskKeywordsConfig.loadFrom(toStream(yaml)));
        assertTrue(ex.getMessage().contains("target-use-case"),
                "error must name the blank field");
    }

    @Test
    void config_duplicateKeyword_failsFast() {
        String yaml = """
                version: 1
                escalation-pattern: "x"
                hard-shift-groups:
                  - target-use-case: UC-J
                    keywords: [scam, scam]
                """;
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> RiskKeywordsConfig.loadFrom(toStream(yaml)));
        assertTrue(ex.getMessage().contains("duplicate"),
                "error must mention duplicate keyword");
    }

    @Test
    void config_invalidRegex_failsFast() {
        String yaml = """
                version: 1
                escalation-pattern: "(unbalanced"
                hard-shift-groups:
                  - target-use-case: UC-J
                    keywords: [foo]
                """;
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> RiskKeywordsConfig.loadFrom(toStream(yaml)));
        assertTrue(ex.getMessage().contains("escalation-pattern"),
                "error must name escalation-pattern");
    }

    // ---------------------------------------------------------------
    // Behavioural parity (YAML-loaded vs hardcoded reference outcomes)
    // ---------------------------------------------------------------

    /**
     * Parity matrix: each row is (input, activeUc, expectedType,
     * expectedNewUc, expectedEscalationRequested). The expected values
     * are exactly what the previous hardcoded list produced.
     */
    @Test
    void detect_parityMatrix_matchesHardcodedReference() throws IOException {
        DriftDetector detector = new DriftDetector(RiskKeywordsConfig.loadDefault());

        record Row(String message, String activeUc, DriftType type, String newUc, boolean esc) {}
        List<Row> rows = List.of(
                // Escalation phrases — previously matched ESCALATION_PATTERN
                new Row("I want to talk to a real person", "UC-A",
                        DriftType.USER_ESCALATION_REQUEST, null, true),
                new Row("Talk to an agent", "UC-A",
                        DriftType.USER_ESCALATION_REQUEST, null, true),
                new Row("transfer me", "UC-A",
                        DriftType.USER_ESCALATION_REQUEST, null, true),
                new Row("speak with a human", "UC-A",
                        DriftType.USER_ESCALATION_REQUEST, null, true),
                new Row("connect me with a live agent", "UC-A",
                        DriftType.USER_ESCALATION_REQUEST, null, true),
                new Row("I need to speak to someone", "UC-A",
                        DriftType.USER_ESCALATION_REQUEST, null, true),
                new Row("let me talk to customer service", "UC-A",
                        DriftType.USER_ESCALATION_REQUEST, null, true),

                // Hard-shift keyword groups
                new Row("I think I got scammed by this seller", "UC-A",
                        DriftType.HARD_SHIFT, "UC-J", false),
                new Row("this looks like fraud", "UC-A",
                        DriftType.HARD_SHIFT, "UC-J", false),
                new Row("I want to delete my data", "UC-A",
                        DriftType.HARD_SHIFT, "UC-G", false),
                new Row("please process a gdpr request", "UC-A",
                        DriftType.HARD_SHIFT, "UC-G", false),
                new Row("I want a refund for this", "UC-A",
                        DriftType.HARD_SHIFT, "UC-I", false),
                new Row("how do I get my money back", "UC-A",
                        DriftType.HARD_SHIFT, "UC-I", false),
                new Row("this person is being threatening", "UC-A",
                        DriftType.HARD_SHIFT, "UC-J", false),
                new Row("they are being abusive", "UC-A",
                        DriftType.HARD_SHIFT, "UC-J", false),
                new Row("why was my ad removed", "UC-A",
                        DriftType.HARD_SHIFT, "UC-H", false),
                new Row("when an ad taken down by mods", "UC-A",
                        DriftType.HARD_SHIFT, "UC-H", false),
                new Row("I want to appeal this", "UC-A",
                        DriftType.HARD_SHIFT, "UC-H", false),

                // Same-UC suppression — historical behaviour
                new Row("I was scammed", "UC-J",
                        DriftType.NONE, null, false),
                new Row("I want a refund", "UC-I",
                        DriftType.NONE, null, false),

                // Escalation precedence — historical behaviour
                new Row("talk to an agent about this scam", "UC-A",
                        DriftType.USER_ESCALATION_REQUEST, null, true),

                // No-drift baseline
                new Row("my ad is not showing up", "UC-A",
                        DriftType.NONE, null, false),
                new Row("", "UC-A",
                        DriftType.NONE, null, false),
                new Row("   ", "UC-A",
                        DriftType.NONE, null, false)
        );

        for (Row row : rows) {
            BotSession session = BotSession.builder()
                    .sessionId("parity-test")
                    .activeUseCase(row.activeUc())
                    .build();
            DriftResult got = detector.detect(session, row.message());

            assertSame(row.type(), got.getType(),
                    "type mismatch for input: " + row.message());
            assertEquals(row.esc(), got.isEscalationRequested(),
                    "escalationRequested mismatch for input: " + row.message());
            if (row.newUc() == null) {
                assertNull(got.getNewUseCase(),
                        "newUseCase should be null for input: " + row.message());
            } else {
                assertEquals(row.newUc(), got.getNewUseCase(),
                        "newUseCase mismatch for input: " + row.message());
            }
        }
    }

    @Test
    void detect_firstMatchWinsAcrossGroups_matchesHardcodedPrecedence() throws IOException {
        DriftDetector detector = new DriftDetector(RiskKeywordsConfig.loadDefault());

        // "scam" lives in group #1 (UC-J) and "ad removed" lives in
        // group #5 (UC-H). The hardcoded list iterates groups in
        // declaration order with first-match-wins, so a message
        // containing both must resolve to UC-J.
        BotSession session = BotSession.builder()
                .sessionId("first-match-test")
                .activeUseCase("UC-A")
                .build();
        DriftResult result = detector.detect(session,
                "my ad removed but I think it was a scam");

        assertEquals(DriftType.HARD_SHIFT, result.getType());
        assertEquals("UC-J", result.getNewUseCase(),
                "first-match-wins: UC-J wins over later UC-H group");
    }

    @Test
    void detect_keywordsIndexableByGroup_forDocSurface() throws IOException {
        // Sanity: every keyword in the YAML appears under exactly one
        // group and has exactly one target UC. This catches accidental
        // duplication that would silently change first-match-wins.
        RiskKeywordsConfig config = RiskKeywordsConfig.loadDefault();
        Map<String, Long> counts = config.getHardShiftGroups().stream()
                .flatMap(g -> g.keywords().stream())
                .collect(Collectors.groupingBy(k -> k, Collectors.counting()));
        counts.forEach((kw, count) ->
                assertEquals(1L, count.longValue(),
                        "keyword '" + kw + "' must appear in exactly one group"));
    }

    private static ByteArrayInputStream toStream(String yaml) {
        return new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8));
    }
}
