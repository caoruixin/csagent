package com.gumtree.csagent.model.enums;

public enum EventType {
    SESSION_STARTED,
    USE_CASE_INFERRED,
    RETRIEVAL_EXECUTED,
    ARTICLE_SHOWN,
    CLARIFICATION_ASKED,
    ESCALATION_REQUESTED,
    CASE_CREATED,
    OUTCOME_RECORDED,
    SESSION_CLOSED,
    TOOL_SCOPE_BLOCKED,
    GUARDRAIL_VIOLATION,
    OUT_OF_SCOPE_HANDOVER,
    /** Emitted by classify_use_case tool when LLM commits a UC during DISCOVER. */
    CLASSIFICATION_COMMITTED
}
