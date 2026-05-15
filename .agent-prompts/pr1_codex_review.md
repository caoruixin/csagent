You are reviewing a docs-only PR.

Scope:
- docs/README.md
- docs/current/doc_governance.md
- docs/current/agent_context_guide.md

Review goals:
1. Verify the PR establishes a clear docs governance model.
2. Verify it does NOT claim stale docs are current unless proven.
3. Verify it preserves docs-ahead-code as future reference instead of telling maintainers to delete it.
4. Verify the source-of-truth hierarchy is clear:
   code/tests for delivered behavior, docs/current for current contracts, proposals for future intent.
5. Verify sprint archives are explicitly immutable.
6. Verify Claude/Codex responsibilities are clear.
7. Verify no code files were changed.

Do not suggest broad rewrites.
Do not ask to reorganize directories yet.
Return:
- PASS or BLOCK
- blocking issues
- non-blocking suggestions
- any exact text that is risky or misleading
