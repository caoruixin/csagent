package com.gumtree.csagent.model;

import java.util.Objects;

/**
 * One bot-to-user message emitted during an agent run loop. Per Phase 3 §3.3.3,
 * a single {@code AgentRunResult} can contain a sequence of messages:
 * {@code ack? + progress* + final}. For D16.A scaffolding we only define the
 * value type; emission of {@link Type#ACK ACK}/{@link Type#PROGRESS PROGRESS}
 * messages is part of the streaming UX work (Phase E, out of scope for D16).
 *
 * <p>{@code sequenceIndex} is loop-local — it ties the message to its position
 * in {@code AgentRunResult.messages} for ordered playback in trace UIs.
 */
public record AgentMessage(Type type, String text, int sequenceIndex) {

    public enum Type {
        /** Initial acknowledgement, e.g. "Got it, let me check that for you." */
        ACK,
        /** Mid-loop progress update, e.g. "Still working on this — pulling moderation status." */
        PROGRESS,
        /** Terminal customer-facing answer (or clarification question). */
        FINAL
    }

    public AgentMessage {
        Objects.requireNonNull(type, "type is required");
    }

    public static AgentMessage ack(String text) {
        return new AgentMessage(Type.ACK, text, 0);
    }

    public static AgentMessage progress(String text) {
        return new AgentMessage(Type.PROGRESS, text, 0);
    }

    public static AgentMessage finalMessage(String text) {
        return new AgentMessage(Type.FINAL, text, 0);
    }
}
