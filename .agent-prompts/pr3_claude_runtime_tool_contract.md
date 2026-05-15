You are doing a docs-only reconciliation PR for current runtime and tool contracts.

Do NOT edit code.
Do NOT change behavior.
Do NOT delete old specs.
Do NOT edit sprint archives.

Read first:
- docs/README.md
- docs/current/doc_governance.md
- docs/current/agent_context_guide.md
- docs/phase3_detailed_technical_design.md
- docs/phase2_domain_realization_spec.md
- any existing customer_service_tool_spec_v0_2.md/yaml if present

Read live code/config for source of truth:
- server/src/main/resources/config/tool-policy.yaml
- server/src/main/resources/config/use-case-registry.yaml
- server/src/main/java/com/gumtree/csagent/service/tools/
- server/src/main/java/com/gumtree/csagent/service/runtime/ContextProjectionBuilder.java
- server/src/main/java/com/gumtree/csagent/service/runtime/PhaseEvaluator.java
- server/src/main/java/com/gumtree/csagent/service/runtime/AgentRunLoopImpl.java
- server/src/main/java/com/gumtree/csagent/service/tools/ToolDispatcher.java

Tasks:

1. Create docs/current/runtime_contract.md.
   It should summarize the CURRENT delivered runtime behavior:
   - component ownership:
     ControlKernel, PhaseEvaluator, AgentRunLoop, ContextProjectionBuilder, ToolDispatcher
   - phase model
   - current PhasePlan concept
   - current tool-use model
   - current allowed-tool enforcement
   - current projection fields at high level
   - current known deviations from older specs
   - what is NOT current runtime behavior

2. Create docs/current/customer_service_tool_spec_v0_3.md.
   It should be a human-readable current tool spec based on code/config.
   Include:
   - agent-visible tools
   - runtime-only tools
   - human-only tools if represented in docs/config
   - classify_use_case if currently agent-visible
   - request_handover current schema
   - record_outcome current schema
   - search_knowledge current schema
   - resolve_article current schema
   - any legacy aliases supported by code
   - known docs-v0.2 differences
   - do not invent fields that code does not support

3. If old v0.2 docs exist, add only a front matter/status banner marking them as superseded or historical.
   Do not rewrite their body.
   Point to docs/current/customer_service_tool_spec_v0_3.md.

4. Update docs/README.md only if needed to point to the new current docs.

Rules:
- Code is source of truth for delivered behavior.
- If docs describe future behavior not yet implemented, preserve it elsewhere and mark future/deferred; do not copy future behavior into current contract.
- Every non-obvious current behavior claim should cite a code/config path in prose.
- Do not claim exact line numbers unless you verified them.
- Keep docs concise. This is a contract/index, not a giant rewritten spec.

Output:
- files created/modified
- important code-ahead-doc decisions
- any unresolved ambiguity that should become a TODO_DECISION
