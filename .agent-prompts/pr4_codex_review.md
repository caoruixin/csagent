Review this docs-only directory reorganization PR.

Verify:
1. No code files changed.
2. docs/sprints/ remains immutable except necessary link fixes.
3. Proposal/future docs were preserved.
4. Current contract docs remain in docs/current/.
5. Foundational docs moved to docs/foundational/.
6. Proposal docs moved to docs/proposals/.
7. Diagnostic docs moved to docs/diagnostics/.
8. Runbooks moved to docs/runbooks/.
9. docs/README.md accurately explains the new layout.
10. docs/current/agent_context_guide.md uses the new paths.
11. Important relative links were updated.
12. Ambiguous files are marked TODO_REVIEW rather than guessed aggressively.

Return:
- PASS or BLOCK
- misplaced files
- broken or suspicious links
- docs that should not have been moved
- missing README/path updates
