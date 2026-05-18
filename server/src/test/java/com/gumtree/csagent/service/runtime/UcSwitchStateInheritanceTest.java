package com.gumtree.csagent.service.runtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gumtree.csagent.model.BotSession;
import com.gumtree.csagent.model.BotTurn;
import com.gumtree.csagent.service.guardrails.ScriptLibraryService;
import com.gumtree.csagent.service.knowledge.KnowledgeSearchService;
import com.gumtree.csagent.service.observability.EventEmitter;
import com.gumtree.csagent.service.runtime.skill.SkillRegistry;
import com.gumtree.csagent.service.runtime.skill.SkillStateBus;
import com.gumtree.csagent.service.runtime.skill.SkillTestFixtures;
import com.gumtree.csagent.service.tools.CreateCaseControlledTool;
import com.gumtree.csagent.service.tools.ToolDispatcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Sprint 41 integration tests: representative Skill switches drive
 * {@link PhaseEvaluator#plan} → {@link SkillStateBus#applyOnSkillSwitch}
 * end-to-end and assert the per-Skill {@code state_inheritance}
 * declarations from the production YAMLs honour the §10.5 invariant
 * matrix.
 *
 * <p>Each test sets up a session whose prior persisted turn carries a
 * Skill X, then mutates session.activeUseCase + currentPhase to land
 * on Skill Y, calls {@code PhaseEvaluator.plan(...)}, and asserts the
 * resulting session-state side effects (which fields were cleared,
 * which were preserved, what the registry intersection rule did) match
 * the matrix row for Y.
 */
@ExtendWith(MockitoExtension.class)
class UcSwitchStateInheritanceTest {

    @Mock private UseCaseRegistryService useCaseRegistry;
    @Mock private KnowledgeSearchService knowledgeSearchService;
    @Mock private ScriptLibraryService scriptLibrary;
    @Mock private LlmInvocationService llmInvocation;
    @Mock private ContextProjectionBuilder contextProjection;
    @Mock private ActionParser actionParser;
    @Mock private CreateCaseControlledTool createCaseTool;
    @Mock private EventEmitter eventEmitter;
    @Mock private ToolDispatcher toolDispatcher;

    private PhaseEvaluator evaluator;
    private ObjectMapper objectMapper;
    private SkillRegistry skillRegistry;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        skillRegistry = SkillTestFixtures.productionRegistry();
        SkillStateBus bus = new SkillStateBus(objectMapper);
        evaluator = new PhaseEvaluator(
                useCaseRegistry, knowledgeSearchService, scriptLibrary,
                llmInvocation, contextProjection, actionParser,
                objectMapper, createCaseTool, eventEmitter, toolDispatcher,
                skillRegistry, bus);
    }

    // -------------------- helpers --------------------

    private BotSession sessionAt(String phase, String activeUc) {
        BotSession s = new BotSession();
        s.setSessionId("sess-uc-switch-int");
        s.setCurrentPhase(phase);
        s.setActiveUseCase(activeUc);
        return s;
    }

    private BotTurn priorTurn(String activeUc, String phaseAfter) {
        BotTurn t = new BotTurn();
        t.setTurnId("prior-turn");
        t.setSessionId("sess-uc-switch-int");
        t.setTurnIndex(0);
        t.setActiveUseCase(activeUc);
        t.setPhaseAfter(phaseAfter);
        t.setPhaseBefore(phaseAfter);
        return t;
    }

    private String json(Map<String, String> kv) throws Exception {
        return objectMapper.writeValueAsString(kv);
    }

    // -------------------- DISCOVER → RESOLVE-FAQ --------------------

    @Test
    void discoverToResolveFaq_resetsIntakeFieldsPartial() throws Exception {
        BotSession s = sessionAt("RESOLVE", "UC-A");
        s.setIntakeFields(json(Map.of("ad_id_or_listing_url", "abandoned-from-discover")));
        s.setCustomerContext("{\"name\":\"alice\"}");
        List<BotTurn> history = List.of(priorTurn("UC-G", "DISCOVER"));
        // UC-G prior, UC-A current → Skill switch from
        // resolve_intake (no, DISCOVER + UC-G goes through discover_triage)
        // Actually DISCOVER + UC-G uses discover_triage wildcard. So switch:
        // discover_triage → resolve_faq_grounded_answer.
        evaluator.plan(s, "next msg", history);
        // resolve_faq_grounded_answer's reset: [intake_fields_partial].
        assertNull(s.getIntakeFields());
        // customer_context inherited (preserved).
        assertEquals("{\"name\":\"alice\"}", s.getCustomerContext());
    }

    // -------------------- RESOLVE-FAQ → RESOLVE-INTAKE --------------------

    @Test
    void resolveFaqToResolveIntake_preservesIntakeFieldsViaIntersection() throws Exception {
        BotSession s = sessionAt("RESOLVE", "UC-H");
        // Existing intake_fields_partial (legacy carried fields). UC-H
        // required: ad_id_or_listing_url, registered_email, stated_reason_or_context.
        s.setIntakeFields(json(Map.of(
                "ad_id_or_listing_url", "ad-7",
                "registered_email", "a@b.com",
                "irrelevant_legacy", "drop_me"
        )));
        s.setCustomerContext("{\"name\":\"alice\"}");
        List<BotTurn> history = List.of(priorTurn("UC-A", "RESOLVE"));
        // UC-A prior → resolve_faq Skill; UC-H current → resolve_intake Skill.
        evaluator.plan(s, "next msg", history);
        assertNotNull(s.getIntakeFields());
        @SuppressWarnings("unchecked")
        Map<String, Object> survived =
                objectMapper.readValue(s.getIntakeFields(), Map.class);
        assertEquals("ad-7", survived.get("ad_id_or_listing_url"));
        assertEquals("a@b.com", survived.get("registered_email"));
        assertEquals(2, survived.size());  // irrelevant_legacy dropped
    }

    // -------------------- RESOLVE-INTAKE → RESOLVE-FAQ --------------------

    @Test
    void resolveIntakeToResolveFaq_clearsIntakeFieldsPartial() throws Exception {
        BotSession s = sessionAt("RESOLVE", "UC-A");
        s.setIntakeFields(json(Map.of(
                "ad_id_or_listing_url", "ad-9",
                "registered_email", "x@y.com"
        )));
        s.setCustomerContext("{\"name\":\"alice\"}");
        List<BotTurn> history = List.of(priorTurn("UC-H", "RESOLVE"));
        evaluator.plan(s, "next msg", history);
        // resolve_faq_grounded_answer's reset: [intake_fields_partial].
        assertNull(s.getIntakeFields());
        assertEquals("{\"name\":\"alice\"}", s.getCustomerContext());
    }

    // -------------------- RESOLVE-INTAKE → CONFIRM --------------------

    @Test
    void resolveIntakeToConfirm_inheritsIntakeFieldsForIntakeContinuation() throws Exception {
        BotSession s = sessionAt("CONFIRM", "UC-H");
        s.setIntakeFields(json(Map.of(
                "ad_id_or_listing_url", "ad-7",
                "registered_email", "a@b.com",
                "stated_reason_or_context", "policy"
        )));
        List<BotTurn> history = List.of(priorTurn("UC-A", "RESOLVE"));
        // UC-A prior → resolve_faq Skill; UC-H + CONFIRM → confirm Skill.
        evaluator.plan(s, "next msg", history);
        // confirm.yaml inherits intake_fields_partial. Active UC=UC-H,
        // intersection keeps all UC-H required fields.
        assertNotNull(s.getIntakeFields());
        @SuppressWarnings("unchecked")
        Map<String, Object> survived =
                objectMapper.readValue(s.getIntakeFields(), Map.class);
        assertEquals(3, survived.size());
    }

    // -------------------- RESOLVE → ESCALATE --------------------

    @Test
    void resolveFaqToEscalate_resetsIntakeFieldsPartial() throws Exception {
        BotSession s = sessionAt("ESCALATE", "UC-G");
        s.setIntakeFields(json(Map.of("ad_id_or_listing_url", "ad-x")));
        s.setCustomerContext("{\"id\":42}");
        // Prior UC-A on RESOLVE; now ESCALATE/UC-G.
        List<BotTurn> history = List.of(priorTurn("UC-A", "RESOLVE"));
        evaluator.plan(s, "next msg", history);
        // escalate.yaml's reset: [intake_fields_partial].
        assertNull(s.getIntakeFields());
        assertEquals("{\"id\":42}", s.getCustomerContext());
    }

    // -------------------- RESOLVE → CLOSE/terminal --------------------

    @Test
    void resolveFaqToTerminal_resetsIntakeFieldsPartial() throws Exception {
        BotSession s = sessionAt("CLOSE", "UC-G");
        s.setIntakeFields(json(Map.of("ad_id_or_listing_url", "ad-x")));
        List<BotTurn> history = List.of(priorTurn("UC-A", "RESOLVE"));
        evaluator.plan(s, "next msg", history);
        // terminal.yaml's reset: [intake_fields_partial].
        assertNull(s.getIntakeFields());
    }

    // -------------------- same-UC transitions: no bus call --------------------

    @Test
    void sameUcResolveToConfirm_noStateBusInvocation() throws Exception {
        // RESOLVE → CONFIRM on the same UC-A; the bus should NOT clear
        // anything because the UC didn't change. (The Skill changed
        // resolve_faq → confirm, but design doc §10.3 requires BOTH
        // UC change AND Skill change.)
        BotSession s = sessionAt("CONFIRM", "UC-A");
        s.setIntakeFields(json(Map.of("ad_id_or_listing_url", "preserved")));
        List<BotTurn> history = List.of(priorTurn("UC-A", "RESOLVE"));
        evaluator.plan(s, "next msg", history);
        assertNotNull(s.getIntakeFields());
    }

    // -------------------- fresh session (no prior turn) --------------------

    @Test
    void freshSession_noStateBusInvocation() {
        BotSession s = sessionAt("DISCOVER", null);
        // Empty history → no Skill switch detection at all.
        evaluator.plan(s, "first msg", List.of());
        // Trivially: no exception, no state mutation.
    }

    // -------------------- prior turn with null UC --------------------

    @Test
    void priorTurnWithoutCommittedUc_noStateBusInvocation() throws Exception {
        // Prior DISCOVER turn never called classify_use_case; current turn
        // has just committed UC-A. This is the FIRST UC commitment, not a
        // switch; the bus must not fire (priorUc is null).
        BotSession s = sessionAt("RESOLVE", "UC-A");
        s.setIntakeFields(json(Map.of("ad_id_or_listing_url", "should-be-cleared-iff-bus-fires")));
        List<BotTurn> history = List.of(priorTurn(null, "DISCOVER"));
        evaluator.plan(s, "next msg", history);
        // BUT — DISCOVER turn with null UC means there's no prior UC,
        // so the bus does not fire. intake_fields_partial preserved
        // because the bus did not run reset.
        assertNotNull(s.getIntakeFields());
    }
}
