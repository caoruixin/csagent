You are doing a docs-only status stamping PR.

Task:
Add front matter/status banners to proposal and diagnostic docs so future humans/agents do not mistake them for current runtime behavior.

Do NOT edit code.
Do NOT rewrite document bodies.
Do NOT delete future design content.
Do NOT move files.
Do NOT reflow entire markdown files.

Read first:
- docs/README.md
- docs/current/doc_governance.md

Candidate docs to stamp if present:
- docs/autoloop_design.md
- docs/problem_retrieval_solution_plan_pgvector.md
- docs/interactive_case_spec_generation_plan.md
- docs/skill_orchestration_candidates.md
- docs/proposal_uc_topic_subject_alignment.md
- docs/java_guard_prompt_flexibility_design.md
- docs/prompt_context_projection_audit.md
- docs/fix_layer_taxonomy.md
- docs/codex-findings.md

For each file:
1. Inspect the file title and current wording.
2. Add YAML front matter at the top if missing.
3. Use appropriate values:
   - doc_tier: proposal, diagnostic, or reference
   - status: proposal, deferred, partial, or diagnostic
   - implementation_status: not_started, partial, or unknown
   - runtime_contract: false unless the doc clearly declares itself current runtime truth
   - last_reviewed: 2026-05-10
   - review_cadence: on_reactivation or every_3_to_5_sprints
   - notes: one sentence explaining how to use the doc
4. If a document contains both implemented and future parts, mark status: partial.
5. Add a short visible note after front matter:
   "This document is not the current runtime contract unless a docs/current/* contract or live code path confirms it."

Important:
- Preserve docs-ahead-code as future coding reference.
- Do not mark any doc as superseded unless you can identify the replacement doc.
- Do not modify docs/sprints/.
- Do not modify foundational docs in this PR.

Output at the end:
- List of files stamped
- Status chosen for each
- Any file skipped and why
