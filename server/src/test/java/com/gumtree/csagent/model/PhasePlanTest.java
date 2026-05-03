package com.gumtree.csagent.model;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link PhasePlan} (D16.A scaffolding).
 * Verifies the hand-rolled builder, defensive collection copies, and
 * equality semantics inherited from {@code java.lang.Record}.
 */
class PhasePlanTest {

    @Test
    void builder_setsAllFields() {
        PhasePlan plan = PhasePlan.builder()
                .phase("RESOLVE")
                .useCase("UC-A")
                .objective("Determine ad status")
                .allowedTools(List.of("get_customer_context", "search_knowledge"))
                .requiredContextKeys(Set.of("form_context", "customer_context"))
                .maxToolSteps(4)
                .allowInterimMessage(true)
                .validTerminalOutcomes(Set.of(TerminalOutcome.FINAL_ANSWER, TerminalOutcome.ESCALATE))
                .systemInstruction("sys")
                .groundingInstruction("ground")
                .escalationPolicy("policy")
                .build();

        assertEquals("RESOLVE", plan.phase());
        assertEquals("UC-A", plan.useCase());
        assertEquals("Determine ad status", plan.objective());
        assertEquals(List.of("get_customer_context", "search_knowledge"), plan.allowedTools());
        assertEquals(Set.of("form_context", "customer_context"), plan.requiredContextKeys());
        assertEquals(4, plan.maxToolSteps());
        assertTrue(plan.allowInterimMessage());
        assertEquals(Set.of(TerminalOutcome.FINAL_ANSWER, TerminalOutcome.ESCALATE),
                plan.validTerminalOutcomes());
        assertEquals("sys", plan.systemInstruction());
        assertEquals("ground", plan.groundingInstruction());
        assertEquals("policy", plan.escalationPolicy());
    }

    @Test
    void builder_requiresPhase() {
        PhasePlan.Builder builder = PhasePlan.builder().useCase("UC-A");
        assertThrows(NullPointerException.class, builder::build);
    }

    @Test
    void compactConstructor_rejectsNegativeMaxToolSteps() {
        assertThrows(IllegalArgumentException.class, () -> PhasePlan.builder()
                .phase("RESOLVE")
                .maxToolSteps(-1)
                .build());
    }

    @Test
    void compactConstructor_substitutesEmptyForNullCollections() {
        PhasePlan plan = PhasePlan.builder()
                .phase("RESOLVE")
                .build();

        assertNotNull(plan.allowedTools());
        assertTrue(plan.allowedTools().isEmpty());
        assertNotNull(plan.requiredContextKeys());
        assertTrue(plan.requiredContextKeys().isEmpty());
        assertNotNull(plan.validTerminalOutcomes());
        assertTrue(plan.validTerminalOutcomes().isEmpty());
    }

    @Test
    void compactConstructor_makesAllowedToolsImmutable() {
        PhasePlan plan = PhasePlan.builder()
                .phase("RESOLVE")
                .allowedTools(List.of("a", "b"))
                .build();

        assertThrows(UnsupportedOperationException.class,
                () -> plan.allowedTools().add("c"));
    }

    @Test
    void records_haveValueEquality() {
        PhasePlan a = PhasePlan.builder()
                .phase("RESOLVE")
                .useCase("UC-A")
                .allowedTools(List.of("search_knowledge"))
                .maxToolSteps(2)
                .build();
        PhasePlan b = PhasePlan.builder()
                .phase("RESOLVE")
                .useCase("UC-A")
                .allowedTools(List.of("search_knowledge"))
                .maxToolSteps(2)
                .build();

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void records_unequalWhenFieldDiffers() {
        PhasePlan a = PhasePlan.builder().phase("RESOLVE").maxToolSteps(2).build();
        PhasePlan b = PhasePlan.builder().phase("RESOLVE").maxToolSteps(4).build();

        assertNotEquals(a, b);
    }
}
