Review this docs-only foundational fold-back dry run PR.

Target:
- docs/foundational/phase3_detailed_technical_design.md

Verify:
1. No code files changed.
2. docs/sprints/ was not modified.
3. The PR did not rewrite unrelated sections.
4. Current runtime claims are supported by live code paths.
5. Future-looking design content was preserved and marked FUTURE_DESIGN / DEFERRED when not implemented.
6. The PR correctly reflects AgentRunLoop / PhasePlan / tool-use current behavior.
7. Terminal tool handling for request_handover and record_outcome is described accurately.
8. classify_use_case role in DISCOVER is described only if confirmed by code/config.
9. Legacy executor path is not incorrectly presented as the primary current path if it is rollback-only.
10. No production readiness overclaims were introduced.
11. TODO_DECISION is used where intent is ambiguous.

Return:
- PASS or BLOCK
- inaccurate current behavior claims
- future content incorrectly marked current
- implemented content incorrectly marked future
- missing TODO_DECISION items
- scope creep
