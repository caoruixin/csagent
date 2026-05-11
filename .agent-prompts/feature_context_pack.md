You are selecting the correct repository context for a future feature/module change.

Feature/module to analyze:
<REPLACE_WITH_FEATURE_NAME_AND_GOAL>

Rules:
1. Do NOT edit files.
2. Do NOT propose code changes yet.
3. Read docs/README.md and docs/current/doc_governance.md first.
4. Use docs/current/agent_context_guide.md to choose relevant docs.
5. Distinguish:
   - current runtime docs
   - foundational business/architecture docs
   - proposal/future docs
   - diagnostic docs
   - sprint archives
6. If docs and code disagree:
   - code is source of truth for delivered behavior
   - docs ahead of code should be preserved as future reference
   - flag conflicts explicitly
7. Return a Context Pack with:
   A. Feature summary
   B. Required docs to read, grouped by doc_tier
   C. Docs to avoid treating as current truth
   D. Relevant code paths to inspect
   E. Known stale/spec-risk areas
   F. Current implementation status
   G. Open decisions before planning
   H. Recommended next agent prompt for planning

Output only the Context Pack. No code edits.
