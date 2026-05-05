package com.gumtree.csagent.service.runtime;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gumtree.csagent.model.BotSession;
import com.gumtree.csagent.model.PhasePlan;
import com.gumtree.csagent.model.TerminalOutcome;
import com.gumtree.csagent.service.tools.ToolPolicyEnforcer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * Sprint 7 §I0 — focused regression for the C5 candidate_use_cases
 * projection + DISCOVER cue, anchored on cs_interactive_259's empty-form +
 * payment-sale-proceeds shape.
 *
 * <p>Tests:
 * <ul>
 *   <li>candidate_use_cases is surfaced in the projected JSON (populated and empty cases).</li>
 *   <li>DISCOVER systemInstruction names the Sprint 7 §I0 weak-candidate cue
 *       and the cs259-shape examples (payment / sale-proceeds / how-to FAQ).</li>
 *   <li>The cue forbids premature handover with faq_miss_threshold_exceeded
 *       on the cs259-shape and steers toward UC-F.</li>
 *   <li>Sprint 6 G2 S1 FAQ-grounded-resolve cue remains intact (regression guard).</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class Sprint7CandidateUseCasesProjectionTest {

    private ContextProjectionBuilder builder;
    private PhaseEvaluator phaseEvaluator;
    private ObjectMapper objectMapper;

    @Mock private UseCaseRegistryService useCaseRegistry;
    @Mock private ControlPolicyService controlPolicy;
    @Mock private ToolPolicyEnforcer toolPolicyEnforcer;
    @Mock private com.gumtree.csagent.service.knowledge.KnowledgeSearchService knowledgeSearchService;
    @Mock private com.gumtree.csagent.service.guardrails.ScriptLibraryService scriptLibrary;
    @Mock private LlmInvocationService llmInvocation;
    @Mock private ActionParser actionParser;
    @Mock private com.gumtree.csagent.service.tools.CreateCaseControlledTool createCaseTool;
    @Mock private com.gumtree.csagent.service.observability.EventEmitter eventEmitter;
    @Mock private com.gumtree.csagent.service.tools.ToolDispatcher toolDispatcher;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        builder = new ContextProjectionBuilder(objectMapper, useCaseRegistry, controlPolicy, toolPolicyEnforcer);
        builder.initToolSchemas();
        phaseEvaluator = new PhaseEvaluator(
                useCaseRegistry, knowledgeSearchService, scriptLibrary,
                llmInvocation, builder, actionParser, objectMapper,
                createCaseTool, eventEmitter, toolDispatcher);
        lenient().when(controlPolicy.getMaxBotTurnsFaq()).thenReturn(6);
        lenient().when(controlPolicy.getMaxBotTurnsIntake()).thenReturn(8);
        lenient().when(controlPolicy.getMaxClarificationRounds()).thenReturn(3);
        lenient().when(controlPolicy.getMaxFaqMiss()).thenReturn(2);
    }

    @Test
    void buildProjection_withCandidateUseCases_shouldSurfaceArray() throws Exception {
        BotSession session = baseSession(null, "DISCOVER");
        session.setCandidateUseCases(new String[]{"UC-A", "UC-B", "UC-FP", "UC-H"});
        session.setFormTopicSubject("Ad Support");

        String projection = builder.buildProjection(session, List.of(), null, "what happened to my ad?");
        JsonNode root = objectMapper.readTree(projection);

        assertTrue(root.has("candidate_use_cases"),
                "Projection must surface candidate_use_cases (Sprint 7 §I0)");
        JsonNode arr = root.get("candidate_use_cases");
        assertTrue(arr.isArray());
        assertEquals(4, arr.size());
        assertEquals("UC-A", arr.get(0).asText());
        assertEquals("UC-FP", arr.get(2).asText());
    }

    @Test
    void buildProjection_emptyCandidateUseCases_shouldSurfaceEmptyArray() throws Exception {
        // cs_interactive_259 shape — UNKNOWN topic + empty description means
        // FormContextIngestionService never assigned candidate UCs.
        BotSession session = baseSession(null, "DISCOVER");
        session.setCandidateUseCases(null);
        session.setFormTopicSubject("UNKNOWN");

        String projection = builder.buildProjection(
                session, List.of(), null, "How do I receive the payment when I sell an item");
        JsonNode root = objectMapper.readTree(projection);

        assertTrue(root.has("candidate_use_cases"),
                "candidate_use_cases must be present (empty array) so DISCOVER cue can fire");
        JsonNode arr = root.get("candidate_use_cases");
        assertTrue(arr.isArray());
        assertEquals(0, arr.size(),
                "empty candidate_use_cases is the cs259 trigger shape");
    }

    @Test
    void buildProjection_planAware_shouldSurfaceCandidateUseCases() throws Exception {
        BotSession session = baseSession(null, "DISCOVER");
        session.setCandidateUseCases(new String[]{"UC-F"});
        session.setFormTopicSubject("UNKNOWN");

        PhasePlan plan = phaseEvaluator.plan(session, "How do I receive the payment when I sell an item", List.of());
        assertNotNull(plan, "DISCOVER plan should be non-null");

        String projection = builder.build(
                session, List.of(), plan,
                "How do I receive the payment when I sell an item", null);
        JsonNode root = objectMapper.readTree(projection);

        assertTrue(root.has("candidate_use_cases"));
        assertTrue(root.has("phase_plan"));
        assertEquals("DISCOVER", root.get("phase_plan").get("phase").asText());
    }

    @Test
    void discoverPlan_systemInstruction_shouldNameSprint7WeakCandidateCue() {
        BotSession session = baseSession(null, "DISCOVER");
        session.setCandidateUseCases(new String[0]);
        session.setFormTopicSubject("UNKNOWN");

        PhasePlan plan = phaseEvaluator.plan(session, "How do I receive the payment when I sell an item", List.of());
        assertNotNull(plan);
        assertEquals("DISCOVER", plan.phase());

        String si = plan.systemInstruction();
        assertNotNull(si);
        assertTrue(si.contains("Sprint 7"),
                "Sprint 7 anchor should be present in the DISCOVER systemInstruction");
        assertTrue(si.contains("candidate_use_cases"),
                "DISCOVER systemInstruction should reference candidate_use_cases");
        assertTrue(si.contains("UNKNOWN") || si.contains("UNKNOWN topic"),
                "DISCOVER systemInstruction should mention the UNKNOWN-topic trigger");
        assertTrue(si.contains("payment") || si.contains("sale-proceeds"),
                "DISCOVER systemInstruction should mention payment / sale-proceeds shape");
        assertTrue(si.contains("UC-F"),
                "DISCOVER systemInstruction should steer payment / sale-proceeds shape toward UC-F");
    }

    @Test
    void discoverPlan_systemInstruction_forbidsPrematureFaqMissHandover() {
        BotSession session = baseSession(null, "DISCOVER");
        session.setCandidateUseCases(new String[0]);

        PhasePlan plan = phaseEvaluator.plan(session, "How do I receive the payment when I sell an item", List.of());
        assertNotNull(plan);

        String si = plan.systemInstruction();
        assertTrue(si.contains("faq_miss_threshold_exceeded"),
                "Cue must explicitly name faq_miss_threshold_exceeded as the wrong move");
        assertTrue(si.contains("search_knowledge"),
                "Cue must direct the bot to call search_knowledge first");
        assertTrue(si.contains("classify_use_case"),
                "Cue must direct the bot to call classify_use_case after search");
    }

    @Test
    void discoverPlan_allowsClassifyUseCaseAndSearchKnowledge() {
        BotSession session = baseSession(null, "DISCOVER");
        PhasePlan plan = phaseEvaluator.plan(session, "How do I receive the payment when I sell an item", List.of());
        assertNotNull(plan);
        assertTrue(plan.allowedTools().contains("classify_use_case"));
        assertTrue(plan.allowedTools().contains("search_knowledge"));
        assertTrue(plan.validTerminalOutcomes().contains(TerminalOutcome.CLARIFICATION_NEEDED));
    }

    @Test
    void faqResolvePlan_s1FaqGroundedResolveCueRemainsIntact() {
        // Sprint 6 G2 regression guard — cs259 primary fix (search → resolve →
        // grounded answer → record_outcome) must remain in PhaseEvaluator.
        BotSession session = baseSession("UC-F", "RESOLVE");

        UseCaseRegistryService.UseCaseDefinition ucF = new UseCaseRegistryService.UseCaseDefinition(
                "UC-F", "Payment Inquiry", List.of("Payments"), "MEDIUM", true, "FAQ");
        when(useCaseRegistry.getUseCase("UC-F")).thenReturn(ucF);

        PhasePlan plan = phaseEvaluator.plan(session, "How do I get paid", List.of());
        assertNotNull(plan, "RESOLVE FAQ plan should be non-null for UC-F");
        assertEquals("RESOLVE", plan.phase());
        String si = plan.systemInstruction();
        assertTrue(si.contains("S1") || si.contains("search_knowledge -> resolve_article"),
                "Sprint 6 S1 FAQ-grounded-resolve cue must remain intact");
        assertTrue(plan.allowedTools().contains("resolve_article"));
        assertTrue(plan.allowedTools().contains("record_outcome"));
    }

    private BotSession baseSession(String activeUc, String phase) {
        return BotSession.builder()
                .sessionId("test-session-i0")
                .activeUseCase(activeUc)
                .currentPhase(phase)
                .handlingState("BOT_HANDLING")
                .totalBotTurns(0)
                .clarificationCount(0)
                .faqMissCount(0)
                .repeatedActionCount(0)
                .build();
    }
}
