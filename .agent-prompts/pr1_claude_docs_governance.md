You are working in a repository with many overlapping docs produced across foundational design, sprint iterations, diagnostics, and proposals.

Task: create the docs governance foundation. Do NOT edit code.

Allowed file changes:
- docs/README.md
- docs/current/doc_governance.md
- docs/current/agent_context_guide.md

Do NOT move files.
Do NOT rewrite existing docs.
Do NOT edit server/, ui/, eval/, data/, or config files.

Context:
The repo currently has foundational docs such as phase0/phase1/phase2/phase3/phase5 specs, sprint archive docs under docs/sprints/, diagnostics such as prompt/context audits, proposals such as autoloop and skill orchestration plans, and runbooks such as admin-guide.

Goal:
Make it clear to humans and agents which docs are current source of truth, which are historical, which are proposals, and which are diagnostic.

Create docs/README.md with:
1. A short explanation of the docs system.
2. A source-of-truth hierarchy:
   - code + tests for delivered behavior
   - docs/current/* for current runtime contracts
   - foundational docs for durable architecture/business intent
   - proposal docs for future intended behavior
   - sprint archive docs as immutable history
3. A five-tier doc model:
   - foundational
   - current-runtime / durable connective
   - sprint-archive
   - proposal / forward-looking
   - diagnostic / runbook / reference
4. Update rules for each tier.
5. A rule for code-ahead-docs:
   If code implementation has been reviewed and is the delivered behavior, update docs rather than reverting code to stale docs.
6. A rule for docs-ahead-code:
   Preserve the doc, mark it PROPOSAL / DEFERRED / PARTIAL, and do not delete future design content.
7. A short "How to ask agents for context" section pointing to docs/current/agent_context_guide.md.
8. A "Do not edit sprint archives" rule.

Create docs/current/doc_governance.md with:
1. Front matter schema for docs:
   - title
   - doc_tier
   - status
   - implementation_status
   - source_of_truth
   - last_reviewed
   - review_cadence
   - supersedes
   - superseded_by
   - notes
2. Allowed values for:
   - doc_tier: current-runtime, foundational, durable-connective, sprint-archive, proposal, diagnostic, runbook, reference, archived
   - status: current, proposal, partial, deferred, diagnostic, archived, superseded
   - implementation_status: implemented, partial, not_started, historical, unknown
3. Decision rules:
   - code ahead of docs
   - docs ahead of code
   - true conflict
   - stale references
   - future proposals
4. Fold-back cadence:
   every 3–5 sprints or when a foundational doc visibly drifts.
5. Claude Code / Codex responsibilities:
   - Claude drafts docs diffs and cites code paths
   - Codex verifies accuracy, scope, and source-of-truth decisions
6. A short PR checklist for docs-only reconciliation.

Create docs/current/agent_context_guide.md with:
1. A feature/module context-selection guide.
2. For each task type, list which docs to read first:
   - runtime / phase machine / drift
   - tool schema / tool policy
   - FAQ retrieval / grounding
   - intake / handover / Salesforce
   - privacy / PII / trace
   - eval / governance
   - production readiness
   - proposal exploration
3. Include a reusable "Context Pack Prompt" that asks an agent to return:
   - relevant docs
   - relevant code paths
   - doc status warnings
   - source-of-truth decision
   - implementation status
   - risks before coding

Style:
- Keep docs concise.
- Do not claim a specific implementation detail unless you cite a code path or say it is an example.
- Do not mark individual existing docs yet; that will happen in a later PR.
