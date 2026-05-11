Review the Context Pack produced by Claude for a future feature/module change.

Inputs:
- /tmp/context_pack.md
- docs/README.md
- docs/current/doc_governance.md
- docs/current/agent_context_guide.md
- relevant docs/code paths named by the Context Pack

Review goals:
1. Verify the selected docs are appropriate for the feature.
2. Verify Claude did not treat proposal/deferred docs as current runtime truth.
3. Verify current-runtime docs and live code paths are included.
4. Verify important docs are not missing.
5. Verify stale docs are called out.
6. Verify open decisions are real and not avoidable by reading code.
7. Return PASS or BLOCK.

If BLOCK:
- list missing docs
- list wrong docs
- list stale-doc risks
- give a corrected Context Pack outline
