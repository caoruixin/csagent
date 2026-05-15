You are doing a docs-only admin / ops / production-readiness cleanup PR after the docs directory reorg.

Do NOT edit code.
Do NOT edit server/, ui/, eval/, data/, config.
Do NOT modify docs/sprints/.
Do NOT delete future/proposal docs.
Do NOT rewrite large foundational sections.
Keep changes scoped to operational docs and path/status corrections.

Read first:
- docs/README.md
- docs/current/doc_governance.md
- docs/current/agent_context_guide.md
- docs/current/runtime_contract.md
- docs/current/customer_service_tool_spec_v0_3.md
- docs/current/faq_grounding_contract.md if present

Read runbooks/current docs if present:
- docs/runbooks/admin-guide.md
- docs/runbooks/salesforce-part-spec.md
- docs/runbooks/abtesing-policy.md
- docs/customer_service_agent_delivery_workbook.md if still top-level or TODO_REVIEW
- docs/current/salesforce_contract.md if present
- docs/current/eval_contract.md if present

Read live code/config only to verify current operational truth:
- server/src/main/java/com/gumtree/csagent/config/LlmClientConfig.java
- server/src/main/resources/application.yml
- server/src/main/resources/application-local.yml
- server/src/main/java/com/gumtree/csagent/model/BotTurn.java
- server/src/main/resources/db/migration/
- server/src/main/java/com/gumtree/csagent/service/SalesforceService.java
- server/src/main/java/com/gumtree/csagent/service/mock/MockSalesforceService.java
- server/src/main/java/com/gumtree/csagent/service/embedding/
- Dockerfile, helm-chart/, Jenkinsfile, contract/ if present

Tasks:
1. Update docs/runbooks/admin-guide.md if present:
   - Remove or mark obsolete references to dropped bot_turns.action_selected/action_parameters fields.
   - Update LLM provider/env var descriptions to match current code/config.
   - Clarify local/demo vs production behavior.
   - Do not invent production setup that does not exist.

2. Update operational docs to distinguish:
   - current local/demo implementation
   - current production-ready implementation
   - future production design / gap
   Use explicit wording: CURRENT, LOCAL_ONLY, PRODUCTION_GAP, FUTURE_DESIGN.

3. If docs mention production assets not present in the repo, mark them as production-readiness gaps, not current implementation.

4. Fix pre-existing missing asset links in docs/customer_service_agent_delivery_workbook.md if present:
   - Do not invent missing files.
   - If case-samples.md or .xlsx files are not present, mark those links as MISSING_ASSET / TODO_REVIEW.
   - If the file belongs in runbooks or archive, propose the move only if docs governance clearly supports it.

5. Update docs/README.md only if needed to reflect top-level TODO_REVIEW changes.

6. Do not change current runtime/tool contracts unless a small link/path correction is needed.

Output:
- files changed
- stale operational claims corrected
- production gaps documented
- missing assets marked
- unresolved TODO_REVIEW items
