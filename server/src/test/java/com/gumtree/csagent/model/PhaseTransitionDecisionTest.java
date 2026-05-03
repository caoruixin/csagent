package com.gumtree.csagent.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Sanity tests for {@link PhaseTransitionDecision} (D16.A scaffolding).
 */
class PhaseTransitionDecisionTest {

    @Test
    void exposesAllFields() {
        PhaseTransitionDecision d = new PhaseTransitionDecision(
                "CONFIRM", "Anything else?", null, "loop_completed");
        assertEquals("CONFIRM", d.nextPhase());
        assertEquals("Anything else?", d.responseText());
        assertNull(d.escalationReason());
        assertEquals("loop_completed", d.transitionReason());
    }

    @Test
    void valueEquality() {
        PhaseTransitionDecision a = new PhaseTransitionDecision("CLOSE", "bye", null, "ok");
        PhaseTransitionDecision b = new PhaseTransitionDecision("CLOSE", "bye", null, "ok");
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }
}
