You are fixing a BLOCKED docs-only directory reorganization PR.

Do NOT edit code.
Do NOT edit server/, ui/, eval/, data/, config.
Do NOT rewrite document bodies.
Do NOT move broad sets of files unless explicitly needed.
Do NOT edit docs/sprints/ or docs/archive/ except if explicitly instructed; sprint/archive docs are immutable historical records.

Codex review BLOCK reasons to fix:

1. Broken markdown links in:
   - docs/customer_service_agent_delivery_workbook.md
   Lines around 26, 27, 28, 29, 31, 39, 46, 47, 53, 54 still link to moved top-level files:
   - ./phase0_normative_freeze.md
   - ./BRD.md
   - ./salesforce-part-spec.md
   - ./problem_retrieval_solution_plan_pgvector.md
   Update these links to the new paths under:
   - docs/foundational/
   - docs/runbooks/
   - docs/proposals/
   Use correct relative links from the delivery workbook's current location.

2. Fix docs/README.md:
   - Around line 142 it still says foundational design docs are "at docs/".
   - Update it to docs/foundational/.
   - Update the directory layout section so it describes the current moved structure.
   - Add docs/bak/ and docs/design-extract/ either to the layout or TODO_REVIEW section.
   - Narrow or remove the statement around line 115 that says moved docs still contain old inline paths, because important active links should be updated in this PR.

3. Replace stale inline path references in active non-archive docs.
   Do NOT touch docs/sprints/ or docs/archive/.
   Fix references like:
   - docs/fix_layer_taxonomy.md -> docs/diagnostics/fix_layer_taxonomy.md
   - docs/codex-findings.md -> docs/diagnostics/codex-findings.md
   - docs/prompt_context_projection_audit.md -> docs/diagnostics/prompt_context_projection_audit.md
   - docs/phase*_*.md -> docs/foundational/phase*_*.md
   - docs/abtesing-policy.md -> docs/runbooks/abtesing-policy.md if moved there
   Specific examples from Codex:
   - docs/proposals/java_guard_prompt_flexibility_design.md around lines 45, 198, 393
   - docs/proposals/skill_orchestration_candidates.md around line 49
   - docs/diagnostics/prompt_context_projection_audit.md around line 112
   - docs/runtime_freeze_and_risk_policy.md around line 910

4. Re-check ambiguous top-level files:
   - docs/handover_orchestrator_design.md
   - docs/faq_grounding_contract.md

   If handover_orchestrator_design.md is a future design/proposal, move it to docs/proposals/ using git mv.
   If faq_grounding_contract.md is a current canonical grounding contract, move it to docs/current/ using git mv.
   If uncertain, leave in place but ensure docs/README.md TODO_REVIEW clearly names it and explains the decision needed.

5. Do not fix pre-existing missing external/data links such as missing .xlsx or case-samples.md unless they are directly caused by this reorg.
   If needed, document them as pre-existing missing references.

6. After edits, provide:
   - changed files
   - moved files, if any
   - links fixed
   - remaining TODO_REVIEW items
   - confirmation that no code files were changed

Keep the changes minimal and scoped to Codex's blockers.
