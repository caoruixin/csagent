Review this docs-only admin / ops / production-readiness cleanup PR.

Verify:
1. No code files were changed.
2. docs/sprints/ was not modified.
3. Admin/runbook docs no longer present dropped bot_turns.action_selected/action_parameters fields as current.
4. LLM provider/env var descriptions match current code/config.
5. Local/demo behavior is clearly separated from production-ready behavior.
6. Production designs not implemented are preserved but marked PRODUCTION_GAP or FUTURE_DESIGN.
7. Missing asset links in customer_service_agent_delivery_workbook.md are handled honestly as MISSING_ASSET/TODO_REVIEW, not invented.
8. Current runtime/tool contracts were not rewritten unnecessarily.
9. No proposal/future docs were deleted.

Return:
- PASS or BLOCK
- exact stale claims that remain
- any overclaims of production readiness
- any missing TODO_REVIEW labels
