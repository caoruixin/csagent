package com.gumtree.csagent.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link AgentMessage} (D16.A scaffolding).
 */
class AgentMessageTest {

    @Test
    void ack_buildsAckMessage() {
        AgentMessage msg = AgentMessage.ack("Got it, checking now.");
        assertEquals(AgentMessage.Type.ACK, msg.type());
        assertEquals("Got it, checking now.", msg.text());
        assertEquals(0, msg.sequenceIndex());
    }

    @Test
    void progress_buildsProgressMessage() {
        AgentMessage msg = AgentMessage.progress("Still working...");
        assertEquals(AgentMessage.Type.PROGRESS, msg.type());
        assertEquals("Still working...", msg.text());
    }

    @Test
    void finalMessage_buildsFinalMessage() {
        AgentMessage msg = AgentMessage.finalMessage("Your ad is approved.");
        assertEquals(AgentMessage.Type.FINAL, msg.type());
        assertEquals("Your ad is approved.", msg.text());
    }

    @Test
    void compactConstructor_rejectsNullType() {
        assertThrows(NullPointerException.class,
                () -> new AgentMessage(null, "text", 0));
    }

    @Test
    void records_haveValueEquality() {
        AgentMessage a = new AgentMessage(AgentMessage.Type.FINAL, "hi", 3);
        AgentMessage b = new AgentMessage(AgentMessage.Type.FINAL, "hi", 3);
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }
}
