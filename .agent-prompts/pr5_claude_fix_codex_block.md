Address the Codex BLOCK comments for the PR5 admin / ops / production-readiness cleanup PR.

Do NOT edit code.
Do NOT edit server/, ui/, eval/, data/, config.
Do NOT modify docs/sprints/.
Do NOT rewrite current runtime/tool contracts.
Do NOT broaden the PR.
Only edit the markdown files already in scope for PR5, especially:
- docs/customer_service_agent_delivery_workbook.md
- docs/runbooks/admin-guide.md
- docs/runbooks/abtesing-policy.md only if a small cross-reference is needed
- docs/README.md only if needed for TODO_REVIEW consistency

Blocking fixes required:

1. In docs/customer_service_agent_delivery_workbook.md:
   - Replace any claim that CURRENT implementation uses Vertex AI text-embedding-004 with a precise distinction:
     CURRENT_LOCAL / CURRENT_DEMO: DashScope embedding, model text-embedding-v3.
     PRODUCTION_TARGET / FUTURE_DESIGN: Vertex AI text-embedding-004, if the design still intends that.
   - Any instruction to enable Vertex AI Embedding + Gemini API must be marked PRODUCTION_GAP or FUTURE_DESIGN, not current implementation.
   - Do not delete future production design. Preserve it with proper status labels.

2. In docs/runbooks/admin-guide.md:
   - Update "system emits 12 event types" to current truth: 13 event types, including CLASSIFICATION_COMMITTED.
   - Mention CLASSIFICATION_COMMITTED is emitted when classify_use_case commits a use case, based on current code.
   - Keep wording concise.

3. In docs/runbooks/admin-guide.md:
   - Fix the overclaim that Tool Calls fully replaces the retired Action Parameters panel.
   - Correct wording:
     The DB action_selected/action_parameters columns are retired/dropped for current traces.
     The UI may still show a conditional legacy Action Parameters panel for old trace data / backward compatibility.
   - Do not claim the UI cleanup is complete if it is not.

4. In docs/customer_service_agent_delivery_workbook.md:
   - Fix the overclaim "remaining gaps are none".
   - Replace with "no remaining Phase 3 design blockers" if that is accurate, or mark listed items as PRODUCTION_GAP / FUTURE_DESIGN.
   - GrowthBook rollout should not be presented as fully implemented locally if abtesing-policy says csagent integration is not implemented.
     Mark it PRODUCTION_GAP / FUTURE_DESIGN or "policy defined, runtime integration pending" as appropriate.

5. Add TODO_REVIEW next to the top audit bullets that currently say MISSING_ASSET without TODO_REVIEW:
   - docs/customer_service_agent_delivery_workbook.md lines around 12, 16, 18, 20
   Use MISSING_ASSET / TODO_REVIEW consistently.

Rules:
- Preserve docs-ahead-code as future reference.
- Code/config is source of truth for delivered behavior.
- Do not invent production functionality.
- Keep changes minimal and targeted.
- After editing, summarize exactly which Codex BLOCK item each change resolves.
