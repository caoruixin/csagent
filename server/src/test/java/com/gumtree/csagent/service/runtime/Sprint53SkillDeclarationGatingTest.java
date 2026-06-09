package com.gumtree.csagent.service.runtime;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gumtree.csagent.model.BotSession;
import com.gumtree.csagent.service.runtime.skill.Skill;
import com.gumtree.csagent.service.runtime.skill.SkillLoader;
import com.gumtree.csagent.service.runtime.skill.SkillRegistry;
import com.gumtree.csagent.service.runtime.skill.StateInheritance;
import com.gumtree.csagent.service.tools.ToolPolicyEnforcer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * Sprint 53 / M5 S4 — verifies Skill-declared context-key gating (C2 #2,
 * {@code candidate_use_cases}) + soft-signal gating (C2 #5, the three
 * {@code state_inheritance.soft_signal_via_projection} slots —
 * {@code alternate_candidate_use_cases}, {@code discover_disambiguation_signals},
 * {@code prior_use_case_carry}) in
 * {@link ContextProjectionBuilder#buildProjection(BotSession, java.util.List,
 * java.util.List, String)}.
 *
 * <p>Contract (per {@code docs/diagnostics/m5-s4-skill-declaration-audit.md}
 * §4.A + §6.C):
 * <ol>
 *   <li><strong>POSITIVE control</strong> — a Skill that DECLARES a slot
 *       still receives it (no semantic information lost for the cases that
 *       exercise the declaring Skill). discover_triage gets
 *       {@code candidate_use_cases} + {@code alternate_candidate_use_cases} +
 *       {@code discover_disambiguation_signals}; resolve_faq + resolve_intake
 *       get {@code prior_use_case_carry}.</li>
 *   <li><strong>NEGATIVE control</strong> — a Skill that does NOT declare a
 *       slot does NOT receive it (the registered shape change). confirm,
 *       escalate, terminal do NOT receive any of the 4; resolve_faq +
 *       resolve_intake do NOT receive candidate / alternate / disambig.</li>
 *   <li><strong>DEFENSIVE control</strong> — an unmapped (phase, UC) tuple
 *       continues to receive ALL 4 slots (defensive pre-S4 behaviour
 *       preservation for legacy / unmapped sessions; same fallback pattern
 *       as Sprint 52's C2 #4 unmapped tool_schemas).</li>
 *   <li><strong>KEEP-UNCONDITIONAL control</strong> — the four
 *       context slots kept unconditional by the audit
 *       ({@code form_context} / {@code customer_context} /
 *       {@code listing_context} / {@code conversation_history}) are
 *       emitted independent of Skill declaration; their data-gated
 *       emission discipline is preserved.</li>
 *   <li><strong>PRODUCTION YAML guards</strong> — the real Skill YAMLs
 *       loaded from the classpath match the Phase-A audit's NEED column:
 *       only the 4 Skills the audit identifies declare each gated slot.
 *       Re-introduction of a stale declaration (or removal of an
 *       intentional one) trips the guard.</li>
 * </ol>
 *
 * <p>§7 boundary: registry/Skill-driven only; no per-UC if-else added.
 * The gate reads the Skill YAML declaration; the defensive fallback for
 * unmapped tuples is a single uniform {@code true} (emit).
 */
@ExtendWith(MockitoExtension.class)
class Sprint53SkillDeclarationGatingTest {

    private ContextProjectionBuilder builder;
    private ObjectMapper objectMapper;

    @Mock
    private UseCaseRegistryService useCaseRegistry;

    @Mock
    private ControlPolicyService controlPolicy;

    @Mock
    private ToolPolicyEnforcer toolPolicyEnforcer;

    @Mock
    private SkillRegistry skillRegistry;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        builder = new ContextProjectionBuilder(objectMapper, useCaseRegistry,
                controlPolicy, toolPolicyEnforcer, skillRegistry);
        builder.initToolSchemas();
        // Budget stubs are read by the projection builder unconditionally;
        // mark them lenient so individual tests that don't care don't fail
        // on unused-stubbing strictness.
        lenient().when(controlPolicy.getMaxBotTurnsFaq()).thenReturn(6);
        lenient().when(controlPolicy.getMaxClarificationRounds()).thenReturn(3);
        lenient().when(controlPolicy.getMaxFaqMiss()).thenReturn(2);
    }

    // -------------------------------------------------------------------
    // POSITIVE controls — declaring Skill receives the slot
    // -------------------------------------------------------------------

    @Test
    void discoverTriage_receivesCandidateUseCases_alternate_disambig() throws Exception {
        BotSession session = buildSession(null, "DISCOVER");
        when(skillRegistry.select("DISCOVER", null))
                .thenReturn(Optional.of(stubDiscoverSkill()));

        JsonNode root = parse(builder.buildProjection(session, List.of(), null, "hi"));

        assertTrue(root.has("candidate_use_cases"),
                "discover_triage declares candidate_use_cases — POSITIVE control: slot MUST be present.");
        assertTrue(root.has("alternate_candidate_use_cases"),
                "discover_triage declares alternate_candidate_use_cases in soft_signal_via_projection — slot MUST be present.");
        assertTrue(root.has("discover_disambiguation_signals"),
                "discover_triage declares discover_disambiguation_signals in soft_signal_via_projection — slot MUST be present.");
    }

    @Test
    void resolveFaq_receivesPriorUseCaseCarry() throws Exception {
        BotSession session = buildSession("UC-A", "RESOLVE");
        when(skillRegistry.select("RESOLVE", "UC-A"))
                .thenReturn(Optional.of(stubFaqSkill()));

        JsonNode root = parse(builder.buildProjection(session, List.of(), null, "test"));

        assertTrue(root.has("prior_use_case_carry"),
                "resolve_faq_grounded_answer declares prior_use_case_carry in soft_signal_via_projection — slot MUST be present.");
    }

    @Test
    void resolveIntake_receivesPriorUseCaseCarry() throws Exception {
        BotSession session = buildSession("UC-G", "RESOLVE");
        when(skillRegistry.select("RESOLVE", "UC-G"))
                .thenReturn(Optional.of(stubIntakeSkill()));

        JsonNode root = parse(builder.buildProjection(session, List.of(), null, "test"));

        assertTrue(root.has("prior_use_case_carry"),
                "resolve_intake_collect_and_handover declares prior_use_case_carry — slot MUST be present.");
    }

    // -------------------------------------------------------------------
    // NEGATIVE controls — non-declaring Skill does NOT receive the slot
    // -------------------------------------------------------------------

    @Test
    void confirm_doesNotReceive_anyGatedSlot() throws Exception {
        BotSession session = buildSession("UC-A", "CONFIRM");
        when(skillRegistry.select("CONFIRM", "UC-A"))
                .thenReturn(Optional.of(stubConfirmSkill()));

        JsonNode root = parse(builder.buildProjection(session, List.of(), null, "thanks"));

        assertGatedSlotsAbsent(root, "confirm Skill");
    }

    @Test
    void escalate_doesNotReceive_anyGatedSlot() throws Exception {
        BotSession session = buildSession("UC-A", "ESCALATE");
        when(skillRegistry.select("ESCALATE", "UC-A"))
                .thenReturn(Optional.of(stubEscalateSkill()));

        JsonNode root = parse(builder.buildProjection(session, List.of(), null, "human please"));

        assertGatedSlotsAbsent(root, "escalate Skill");
    }

    @Test
    void terminal_doesNotReceive_anyGatedSlot() throws Exception {
        BotSession session = buildSession("UC-A", "CLOSE");
        when(skillRegistry.select("CLOSE", "UC-A"))
                .thenReturn(Optional.of(stubTerminalSkill()));

        JsonNode root = parse(builder.buildProjection(session, List.of(), null, "bye"));

        assertGatedSlotsAbsent(root, "terminal Skill");
    }

    @Test
    void resolveFaq_doesNotReceive_discoverOnlySoftSignals() throws Exception {
        BotSession session = buildSession("UC-A", "RESOLVE");
        when(skillRegistry.select("RESOLVE", "UC-A"))
                .thenReturn(Optional.of(stubFaqSkill()));

        JsonNode root = parse(builder.buildProjection(session, List.of(), null, "test"));

        assertFalse(root.has("candidate_use_cases"),
                "resolve_faq does NOT declare candidate_use_cases — NEGATIVE control: slot MUST be absent (post-classification).");
        assertFalse(root.has("alternate_candidate_use_cases"),
                "resolve_faq does NOT declare alternate_candidate_use_cases — slot MUST be absent.");
        assertFalse(root.has("discover_disambiguation_signals"),
                "resolve_faq does NOT declare discover_disambiguation_signals — slot MUST be absent.");
        // POSITIVE re-check for the soft signal resolve_faq DOES declare.
        assertTrue(root.has("prior_use_case_carry"),
                "resolve_faq DOES declare prior_use_case_carry — slot MUST be present.");
    }

    // -------------------------------------------------------------------
    // DEFENSIVE controls — unmapped tuple preserves pre-S4 behaviour
    // -------------------------------------------------------------------

    @Test
    void unmappedTuple_receivesAllGatedSlots_defensiveDefault() throws Exception {
        BotSession session = buildSession("UC-Z", "RESOLVE");
        when(skillRegistry.select("RESOLVE", "UC-Z")).thenReturn(Optional.empty());
        when(toolPolicyEnforcer.getVisibleToolsForUc("UC-Z"))
                .thenReturn(List.of("search_knowledge"));

        JsonNode root = parse(builder.buildProjection(session, List.of(), null, "test"));

        assertTrue(root.has("candidate_use_cases"),
                "Unmapped (phase, UC) MUST receive candidate_use_cases — defensive pre-S4 behaviour for legacy/unmapped sessions.");
        assertTrue(root.has("alternate_candidate_use_cases"),
                "Unmapped (phase, UC) MUST receive alternate_candidate_use_cases — defensive default.");
        assertTrue(root.has("discover_disambiguation_signals"),
                "Unmapped (phase, UC) MUST receive discover_disambiguation_signals — defensive default.");
        assertTrue(root.has("prior_use_case_carry"),
                "Unmapped (phase, UC) MUST receive prior_use_case_carry — defensive default.");
    }

    @Test
    void nullPhase_receivesAllGatedSlots_defensiveDefault() throws Exception {
        // No phase = the gate cannot evaluate the Skill — defensive default.
        BotSession session = BotSession.builder()
                .sessionId("test-no-phase")
                .activeUseCase("UC-A")
                .currentPhase(null)
                .totalBotTurns(0)
                .clarificationCount(0)
                .faqMissCount(0)
                .handlingState("RUNNING")
                .build();

        JsonNode root = parse(builder.buildProjection(session, List.of(), null, "test"));

        assertTrue(root.has("candidate_use_cases"),
                "Null phase MUST receive candidate_use_cases — defensive default.");
        assertTrue(root.has("alternate_candidate_use_cases"),
                "Null phase MUST receive alternate_candidate_use_cases — defensive default.");
        assertTrue(root.has("discover_disambiguation_signals"),
                "Null phase MUST receive discover_disambiguation_signals — defensive default.");
        assertTrue(root.has("prior_use_case_carry"),
                "Null phase MUST receive prior_use_case_carry — defensive default.");
    }

    // -------------------------------------------------------------------
    // KEEP-UNCONDITIONAL controls — audit §4.B slots remain emit-independent
    // -------------------------------------------------------------------

    @Test
    void confirmSkill_stillReceivesFormContext_whenSessionHasIt() throws Exception {
        // form_context is KEEP-UNCONDITIONAL per audit §3.A; data-gated only.
        BotSession session = BotSession.builder()
                .sessionId("uncond-test")
                .activeUseCase("UC-A")
                .currentPhase("CONFIRM")
                .totalBotTurns(0)
                .clarificationCount(0)
                .faqMissCount(0)
                .handlingState("RUNNING")
                .formContext("{\"topic_subject\":\"ad-visibility\"}")
                .customerContext("{\"name\":\"alice\"}")
                .listingContext("{\"status\":\"REMOVED\"}")
                .build();
        when(skillRegistry.select("CONFIRM", "UC-A"))
                .thenReturn(Optional.of(stubConfirmSkill()));

        JsonNode root = parse(builder.buildProjection(session, List.of(), null, "y"));

        assertTrue(root.has("form_context"),
                "form_context is KEEP-UNCONDITIONAL (audit §3.A) — data-gated only; emitted when session has data even though confirm Skill's required_context_keys is read by S4.");
        assertTrue(root.has("customer_context"),
                "customer_context is KEEP-UNCONDITIONAL (audit §3.B; INHERIT-RISK) — emitted when session has data regardless of confirm Skill not declaring it.");
        assertTrue(root.has("listing_context"),
                "listing_context is KEEP-UNCONDITIONAL (audit §3.C) — emitted when session has data regardless of confirm not declaring it.");
        assertTrue(root.has("conversation_history"),
                "conversation_history is KEEP-UNCONDITIONAL (audit §3.D; emitted unconditionally — possibly empty array — for shape stability).");
    }

    // -------------------------------------------------------------------
    // PRODUCTION YAML guards — declarations match audit's NEED column
    // -------------------------------------------------------------------

    @Test
    void productionSkillYamls_matchAuditDeclarationCoverage() {
        // Loads every production Skill from the classpath and asserts each
        // declaration matches the Phase-A audit's §4.A "GATE" disposition.
        // Re-introducing a stale declaration (or removing an intentional one)
        // would diverge runtime behaviour from the audit's documented
        // expectation — this test is the regression guard.
        UseCaseRegistryService ucRegistry = new UseCaseRegistryService();
        ucRegistry.init();
        SkillLoader loader = new SkillLoader(ucRegistry);
        List<Skill> all = loader.loadAll();

        Skill discover = pick(all, "discover_triage");
        Skill confirm = pick(all, "confirm");
        Skill escalate = pick(all, "escalate");
        Skill terminal = pick(all, "terminal");
        Skill faq = pick(all, "resolve_faq_grounded_answer");
        Skill intake = pick(all, "resolve_intake_collect_and_handover");

        // §3.E — candidate_use_cases: only discover_triage declares.
        assertTrue(discover.requiredContextKeys().contains("candidate_use_cases"),
                "Audit §3.E: discover_triage MUST declare candidate_use_cases.");
        assertFalse(confirm.requiredContextKeys().contains("candidate_use_cases"),
                "Audit §3.E: confirm MUST NOT declare candidate_use_cases (post-classification).");
        assertFalse(escalate.requiredContextKeys().contains("candidate_use_cases"),
                "Audit §3.E: escalate MUST NOT declare candidate_use_cases.");
        assertFalse(terminal.requiredContextKeys().contains("candidate_use_cases"),
                "Audit §3.E: terminal MUST NOT declare candidate_use_cases.");
        assertFalse(faq.requiredContextKeys().contains("candidate_use_cases"),
                "Audit §3.E: resolve_faq MUST NOT declare candidate_use_cases (post-classification).");
        assertFalse(intake.requiredContextKeys().contains("candidate_use_cases"),
                "Audit §3.E: resolve_intake MUST NOT declare candidate_use_cases.");

        // §3.G + §3.H — alternate / disambig: only discover_triage declares.
        assertTrue(discover.stateInheritance().softSignalViaProjection().contains("alternate_candidate_use_cases"),
                "Audit §3.G: discover_triage MUST declare alternate_candidate_use_cases.");
        assertTrue(discover.stateInheritance().softSignalViaProjection().contains("discover_disambiguation_signals"),
                "Audit §3.H: discover_triage MUST declare discover_disambiguation_signals.");
        for (Skill s : List.of(confirm, escalate, terminal, faq, intake)) {
            assertFalse(s.stateInheritance().softSignalViaProjection().contains("alternate_candidate_use_cases"),
                    "Audit §3.G: " + s.name() + " MUST NOT declare alternate_candidate_use_cases.");
            assertFalse(s.stateInheritance().softSignalViaProjection().contains("discover_disambiguation_signals"),
                    "Audit §3.H: " + s.name() + " MUST NOT declare discover_disambiguation_signals.");
        }

        // §3.I — prior_use_case_carry: only resolve_faq + resolve_intake declare.
        assertTrue(faq.stateInheritance().softSignalViaProjection().contains("prior_use_case_carry"),
                "Audit §3.I: resolve_faq MUST declare prior_use_case_carry.");
        assertTrue(intake.stateInheritance().softSignalViaProjection().contains("prior_use_case_carry"),
                "Audit §3.I: resolve_intake MUST declare prior_use_case_carry.");
        for (Skill s : List.of(discover, confirm, escalate, terminal)) {
            assertFalse(s.stateInheritance().softSignalViaProjection().contains("prior_use_case_carry"),
                    "Audit §3.I: " + s.name() + " MUST NOT declare prior_use_case_carry.");
        }
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private JsonNode parse(String json) throws Exception {
        return objectMapper.readTree(json);
    }

    private static void assertGatedSlotsAbsent(JsonNode root, String skillLabel) {
        assertFalse(root.has("candidate_use_cases"),
                skillLabel + " does NOT declare candidate_use_cases — NEGATIVE control: slot MUST be absent.");
        assertFalse(root.has("alternate_candidate_use_cases"),
                skillLabel + " does NOT declare alternate_candidate_use_cases — NEGATIVE control: slot MUST be absent.");
        assertFalse(root.has("discover_disambiguation_signals"),
                skillLabel + " does NOT declare discover_disambiguation_signals — NEGATIVE control: slot MUST be absent.");
        assertFalse(root.has("prior_use_case_carry"),
                skillLabel + " does NOT declare prior_use_case_carry — NEGATIVE control: slot MUST be absent.");
    }

    private static BotSession buildSession(String activeUc, String phase) {
        return BotSession.builder()
                .sessionId("test-session")
                .activeUseCase(activeUc)
                .currentPhase(phase)
                .totalBotTurns(0)
                .clarificationCount(0)
                .faqMissCount(0)
                .handlingState("RUNNING")
                .build();
    }

    private static Skill pick(List<Skill> skills, String name) {
        return skills.stream()
                .filter(s -> name.equals(s.name()))
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "Skill " + name + " MUST load from classpath"));
    }

    // -- Skill stubs mirroring the production YAML state_inheritance/required_context_keys
    //    declarations as of HEAD `49d48b1` (S3 close). The stubs encode only what the
    //    gating helpers read: requiredContextKeys + stateInheritance.softSignalViaProjection.

    private static Skill stubDiscoverSkill() {
        return new Skill(
                "discover_triage",
                "DISCOVER phase Skill stub",
                List.of("DISCOVER"),
                List.of(Skill.UC_WILDCARD),
                List.of("search_knowledge", "classify_use_case"),
                List.of("form_context", "candidate_use_cases"),
                2,
                false,
                List.of("FINAL_ANSWER", "ESCALATE"),
                "objective stub",
                "procedure stub",
                "grounding stub",
                "escalation stub",
                List.of(),
                new StateInheritance(
                        List.of("customer_context", "accumulated_tool_results"),
                        List.of("intake_fields_partial"),
                        List.of("alternate_candidate_use_cases", "discover_disambiguation_signals")),
                List.of()
        );
    }

    private static Skill stubConfirmSkill() {
        return new Skill(
                "confirm",
                "CONFIRM phase Skill stub",
                List.of("CONFIRM"),
                List.of(Skill.UC_WILDCARD),
                List.of("record_outcome", "request_handover"),
                List.of("form_context", "conversation_history"),
                2,
                false,
                List.of("FINAL_ANSWER", "CLARIFICATION_NEEDED", "ESCALATE"),
                "objective stub",
                "procedure stub",
                "grounding stub",
                "escalation stub",
                List.of(),
                new StateInheritance(
                        List.of("customer_context", "accumulated_tool_results", "intake_fields_partial"),
                        List.of(),
                        List.of()),
                List.of()
        );
    }

    private static Skill stubEscalateSkill() {
        return new Skill(
                "escalate",
                "ESCALATE phase Skill stub",
                List.of("ESCALATE"),
                List.of(Skill.UC_WILDCARD),
                List.of("request_handover", "record_outcome"),
                List.of("form_context", "customer_context"),
                2,
                false,
                List.of("ESCALATE", "FINAL_ANSWER"),
                "objective stub",
                "procedure stub",
                "grounding stub",
                "escalation stub",
                List.of(),
                new StateInheritance(
                        List.of("customer_context", "accumulated_tool_results"),
                        List.of("intake_fields_partial"),
                        List.of()),
                List.of()
        );
    }

    private static Skill stubTerminalSkill() {
        return new Skill(
                "terminal",
                "CLOSE phase Skill stub",
                List.of("CLOSE"),
                List.of(Skill.UC_WILDCARD),
                List.of("record_outcome"),
                List.of("form_context"),
                2,
                false,
                List.of("FINAL_ANSWER"),
                "objective stub",
                "procedure stub",
                "grounding stub",
                "escalation stub",
                List.of(),
                new StateInheritance(
                        List.of("customer_context", "accumulated_tool_results"),
                        List.of("intake_fields_partial"),
                        List.of()),
                List.of()
        );
    }

    private static Skill stubFaqSkill() {
        return new Skill(
                "resolve_faq_grounded_answer",
                "RESOLVE-FAQ phase Skill stub",
                List.of("RESOLVE"),
                List.of("UC-A", "UC-B", "UC-C", "UC-D", "UC-E", "UC-F", "UC-FP"),
                List.of("get_customer_context", "search_knowledge",
                        "resolve_article", "record_outcome", "request_handover"),
                List.of("form_context", "customer_context", "listing_context"),
                4,
                false,
                List.of("FINAL_ANSWER", "CLARIFICATION_NEEDED", "ESCALATE"),
                "objective stub",
                "procedure stub",
                "grounding stub",
                "escalation stub",
                List.of(),
                new StateInheritance(
                        List.of("customer_context", "accumulated_tool_results"),
                        List.of("intake_fields_partial"),
                        List.of("prior_use_case_carry")),
                List.of()
        );
    }

    private static Skill stubIntakeSkill() {
        return new Skill(
                "resolve_intake_collect_and_handover",
                "RESOLVE-INTAKE phase Skill stub",
                List.of("RESOLVE"),
                List.of("UC-G", "UC-H", "UC-I", "UC-J", "UC-K"),
                List.of("request_handover"),
                List.of("form_context", "customer_context"),
                3,
                false,
                List.of("CLARIFICATION_NEEDED", "ESCALATE"),
                "objective stub",
                "procedure stub",
                "grounding stub",
                "escalation stub",
                List.of(),
                new StateInheritance(
                        List.of("customer_context", "accumulated_tool_results", "intake_fields_partial"),
                        List.of(),
                        List.of("prior_use_case_carry")),
                List.of()
        );
    }
}
