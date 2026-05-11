You are doing a docs-only directory reorganization PR.

Goal:
Move docs into the governance-tier directory structure now that docs governance and current runtime/tool contracts exist.

Do NOT edit code.
Do NOT edit server/, ui/, eval/, data/, config.
Do NOT rewrite document bodies.
Do NOT delete future/proposal docs.
Do NOT change sprint archive contents except for necessary link fixes.
Use git mv for moves.

Read first:
- docs/README.md
- docs/current/doc_governance.md
- docs/current/agent_context_guide.md
- docs/current/runtime_contract.md
- docs/current/customer_service_tool_spec_v0_3.md

Create directories if missing:
- docs/foundational
- docs/proposals
- docs/diagnostics
- docs/runbooks

Move docs based on their doc_tier/status/front matter where available.

Target placement rules:

1. docs/current/
Current runtime/contract entry points only:
- runtime_contract.md
- customer_service_tool_spec_v0_3.md
- salesforce_contract.md if already current
- eval_contract.md if already current
- doc_governance.md
- agent_context_guide.md

2. docs/foundational/
Durable architecture/business/eval foundation:
- phase0_normative_freeze.md
- phase1_solution_input_pack.md
- phase2_domain_realization_spec.md
- phase3_detailed_technical_design.md
- phase4_demo_coding_agent_implementation_packet.md if present and foundational/demo baseline
- phase5_evaluation_design.md
- customer_service_agent_tech_spec.md
- BRD.md if present
- PRD_biz_part.md if present

3. docs/proposals/
Forward-looking or partially implemented designs:
- autoloop_design.md
- skill_orchestration_candidates.md
- problem_retrieval_solution_plan_pgvector.md
- interactive_case_spec_generation_plan.md
- proposal_uc_topic_subject_alignment.md
- java_guard_prompt_flexibility_design.md

4. docs/diagnostics/
Audit/finding/diagnostic docs:
- prompt_context_projection_audit.md
- fix_layer_taxonomy.md
- codex-findings.md
- other diagnostic docs not in docs/sprints/

5. docs/runbooks/
Operational references:
- admin-guide.md
- release-checklist.md if present
- eval-runbook.md if present
- salesforce-part-spec.md if used as operational integration reference
- abtesing-policy.md if operational rollout reference

6. docs/sprints/
Leave as immutable historical archive.

7. docs/archive/
Leave archive docs in place.
If docs/archive/current-docs exists and clearly represents a snapshot, rename to:
docs/archive/pre-sprint-9-snapshot

After moving:
- Update docs/README.md with the new directory layout.
- Update docs/current/agent_context_guide.md with new paths.
- Update docs/current/doc_governance.md only if needed.
- Update relative links in moved docs if they would break.
- Do not rewrite large sections.

If placement is ambiguous:
- Leave the file in place OR move conservatively.
- Add TODO_REVIEW in docs/README.md.

Final response:
- moved files table: old path -> new path
- files not moved and why
- links updated
- TODO_REVIEW items
