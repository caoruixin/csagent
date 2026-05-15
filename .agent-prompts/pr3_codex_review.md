Review this docs-only PR that creates current runtime and tool contract docs.

Verify against live code/config:
- tool-policy.yaml
- use-case-registry.yaml
- service/tools/*
- ContextProjectionBuilder.java
- PhaseEvaluator.java
- AgentRunLoopImpl.java
- ToolDispatcher.java

Check:
1. docs/current/runtime_contract.md accurately reflects current runtime architecture.
2. docs/current/customer_service_tool_spec_v0_3.md matches current tool schemas and aliases.
3. classify_use_case is included only if it is actually visible to the agent.
4. request_handover summary/primary_use_case requiredness matches implementation.
5. record_outcome enum matches implementation.
6. search_knowledge and resolve_article schemas match projection and implementation.
7. v0.2 docs are marked superseded/historical without deleting future reference content.
8. No code was changed.
9. No future/proposal behavior was misrepresented as implemented.

Return:
- PASS or BLOCK
- exact claims that are inaccurate
- missing current behavior that should be documented
- overclaims that should be downgraded to TODO_DECISION
