You are doing a docs-only foundational fold-back dry run.

Goal:
Fold stable sprint/runtime deltas back into the foundational Phase 3 design doc, using current code as source of truth for delivered behavior.

Do NOT edit code.
Do NOT edit server/, ui/, eval/, data/, config.
Do NOT edit docs/sprints/.
Do NOT rewrite the whole document.
Do NOT delete future-looking design content.
Do NOT broaden scope beyond Phase 3 runtime/tool-loop related content.

Target doc:
- docs/foundational/phase3_detailed_technical_design.md

Read first:
- docs/README.md
- docs/current/doc_governance.md
- docs/current/agent_context_guide.md
- docs/current/runtime_contract.md
- docs/current/customer_service_tool_spec_v0_3.md
- docs/runbooks/agent_workflows.md if present
- docs/10-handoff.md if present
- docs/action_bank.md if present

Read sprint archive range if present:
- docs/sprints/sprint-012-*
- docs/sprints/sprint-013-*
- docs/sprints/sprint-014-*
- docs/sprints/sprint-015-*

Read relevant live code paths:
- server/src/main/java/com/gumtree/csagent/service/runtime/ControlKernel.java
- server/src/main/java/com/gumtree/csagent/service/runtime/PhaseEvaluator.java
- server/src/main/java/com/gumtree/csagent/service/runtime/AgentRunLoopImpl.java
- server/src/main/java/com/gumtree/csagent/service/runtime/ContextProjectionBuilder.java
- server/src/main/java/com/gumtree/csagent/service/tools/ToolDispatcher.java
- server/src/main/java/com/gumtree/csagent/service/tools/RequestHandoverTool.java
- server/src/main/java/com/gumtree/csagent/service/tools/RecordOutcomeTool.java
- server/src/main/java/com/gumtree/csagent/service/tools/ClassifyUseCaseTool.java

Fold-back focus:
1. AgentRunLoop / PhasePlan current behavior.
2. Tool-use model replacing old action abstraction.
3. Terminal tool handling for request_handover and record_outcome.
4. Current projection and allowed-tool enforcement behavior.
5. Current status of legacy executor path as rollback only, if code/docs confirm.
6. Any known deviations from older Phase 3 statements that are now implemented and stable.
7. Current classify_use_case role in DISCOVER if code confirms it.
8. Do not address unrelated production readiness unless Phase 3 already discusses it and a short status note is needed.

Rules:
- Code is source of truth for delivered behavior.
- Sprint archives are historical input, not files to edit.
- If Phase 3 describes future behavior not implemented, preserve it and mark it as FUTURE_DESIGN / DEFERRED if needed.
- If code is ahead of Phase 3, update Phase 3.
- If uncertain, add TODO_DECISION instead of guessing.
- Keep edits minimal and localized.
- Every non-obvious current behavior claim should cite a code path in prose.
- Do not add exact line numbers unless verified.
- Do not modify docs/current/runtime_contract.md or tool_spec_v0_3.md in this PR unless a tiny path link fix is required.

At the end, output:
1. gap classification table:
   - code_ahead_doc
   - doc_ahead_code
   - true_conflict
   - stale_reference
   - future_proposal
2. sections edited
3. future content preserved
4. TODO_DECISION items
5. files changed
