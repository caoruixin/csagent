package com.gumtree.csagent.eval.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A single turn in an eval conversation, parsed from the turns CSV.
 * CSV columns: conversation_id, case_id, sequence, relative_time_sec, role, speaker, message_redacted
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EvalTurn {

    private String conversationId;
    private String caseId;
    private int sequence;
    private int relativeTimeSec;
    /** "visitor" or "agent" */
    private String role;
    private String speaker;
    private String messageRedacted;

    /**
     * Whether this turn is the pre-chat form submission (speaker = [PRE_CHAT_FORM]).
     */
    public boolean isPreChatForm() {
        return "[PRE_CHAT_FORM]".equals(speaker);
    }

    public boolean isVisitor() {
        return "visitor".equalsIgnoreCase(role);
    }

    public boolean isAgent() {
        return "agent".equalsIgnoreCase(role);
    }
}
