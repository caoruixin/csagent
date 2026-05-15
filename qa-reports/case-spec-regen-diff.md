# CaseSpec regeneration diff (Wave A3)

Source corpus: `eval_interactive/case_specs/{anchor,promotion,exploration}/`.

OLD = corpus committed in `1b71fa4` (last regen before Wave A3). NEW =
corpus produced by `python eval_interactive/scripts/regenerate_case_specs.py
--clean` on 2026-04-27.

The 25 yamls in `case_specs/smoke/` are intentionally **not** regenerated
(they are hand-curated fixtures and are out of scope for this wave).

## 1. Spec counts old vs new

| Set | OLD count | NEW count | Delta |
| --- | ---: | ---: | ---: |
| anchor | 159 | 159 | 0 |
| promotion | 101 | 101 | 0 |
| exploration | 107 | 107 | 0 |
| **total (regenerated subset)** | **367** | **367** | **0** |
| smoke (untouched) | 25 | 25 | 0 |

`git diff --stat eval_interactive/case_specs/` reports
`367 files changed, 6174 insertions(+), 5031 deletions(-)`. Every
regenerated spec changed (every spec gained the new
`expected.allow_bot_resolution` and `expected.bot_handling_pattern`
fields, the renamed `persona.user_goal_summary`, and the L1 hard-check
prefix), but no `case_id` was added or removed.

`source_session_id` membership: identical between OLD and NEW for
non-smoke specs (367 sessions present in both). No specs disappeared,
no new specs were added.

## 2. The 11 UC-B reclassifications -- per-case verification

All 11 reclassifications applied. Each session is now extracted with
the corrected `primary_uc` (and the policy-derived
`expected_tool_sequence` / `outcome_class` that follows from it).

| case_id | session_id | primary_uc OLD -> NEW | secondary_ucs NEW | outcome_class OLD -> NEW | expected_tool_sequence OLD -> NEW |
| --- | --- | --- | --- | --- | --- |
| cs_interactive_012 | 570Q5000008hx9tIAA | UC-B -> **UC-FP** | [UC-K] | escalate -> resolve | search_knowledge,resolve_article,request_handover,record_outcome -> get_customer_context,search_knowledge,resolve_article,record_outcome |
| cs_interactive_015 | 570Q5000008WmXxIAK | UC-B -> **UC-K** | [UC-FP, UC-B] | escalate -> escalate | search_knowledge,resolve_article,request_handover,record_outcome -> get_customer_context,create_case_controlled,request_handover,record_outcome |
| cs_interactive_055 | 570Q5000008TMmvIAG | UC-B -> **UC-FP** | [UC-K] | escalate -> resolve | search_knowledge,resolve_article,request_handover,record_outcome -> get_customer_context,search_knowledge,resolve_article,record_outcome |
| cs_interactive_095 | 570Q5000008U5C9IAK | UC-B -> **UC-A** | [UC-D, UC-K] | resolve -> resolve | search_knowledge,resolve_article,answer_grounded,record_outcome -> get_customer_context,search_knowledge,resolve_article,record_outcome |
| cs_interactive_130 | 570Q5000008wmKbIAI | UC-B -> **UC-A** | [UC-H, UC-D] | escalate -> resolve | search_knowledge,resolve_article,request_handover,record_outcome -> get_customer_context,search_knowledge,resolve_article,record_outcome |
| cs_interactive_190 | 570Q5000009060DIAQ | UC-B -> **UC-A** | [UC-K] | resolve -> resolve | search_knowledge,resolve_article,answer_grounded,record_outcome -> get_customer_context,search_knowledge,resolve_article,record_outcome |
| cs_interactive_191 | 570Q5000008w24rIAA | UC-B -> **UC-FP** | [UC-F] | resolve -> resolve | search_knowledge,resolve_article,answer_grounded,record_outcome -> get_customer_context,search_knowledge,resolve_article,record_outcome |
| cs_interactive_221 | 570Q5000008fG5qIAE | UC-B -> **UC-FP** | [UC-K, UC-C] | resolve -> resolve | search_knowledge,resolve_article,answer_grounded,record_outcome -> get_customer_context,search_knowledge,resolve_article,record_outcome |
| cs_interactive_244 | 570Q5000008caqfIAA | UC-B -> **UC-D** | [] | resolve -> resolve | search_knowledge,resolve_article,answer_grounded,record_outcome -> get_customer_context,search_knowledge,resolve_article,record_outcome |
| cs_interactive_249 | 570Q5000008IwKHIA0 | UC-B -> **UC-FP** | [UC-K] | resolve -> resolve | search_knowledge,resolve_article,answer_grounded,record_outcome -> get_customer_context,search_knowledge,resolve_article,record_outcome |
| cs_interactive_250 | 570Q5000008iwZxIAI | UC-B -> **UC-K** | [UC-FP, UC-B] | resolve -> resolve | search_knowledge,resolve_article,answer_grounded,record_outcome -> get_customer_context,create_case_controlled,request_handover,record_outcome |

Notes:
- `cs_interactive_250`: outcome stayed `resolve` because the HR row asks
  for resolve and UC-K is policy-class `either`; the case-level should
  remain HR-driven for resolvable UC-K cases.
- All 11 cases gained the new `expected.allow_bot_resolution` /
  `expected.bot_handling_pattern` fields and the
  `persona.user_goal_summary` rename.

## 3. cs_interactive_040 (UC-K resolve -> escalate)

`source_session_id=570Q5000008pMbOIAU`; the bug was that HR rated this
session `outcome_class=resolve` even though the form description ("My
ad isn't allowing customers to send requests. There is a flaw on the
app") describes a defect that requires human engineering. The Wave A3
extractor picks this up via a per-session
`OUTCOME_OVERRIDES["570Q5000008pMbOIAU"] = ("escalate", True,
"intake_complete_for_uc_k")` entry.

| Field | OLD | NEW |
| --- | --- | --- |
| `expected.outcome_class` | `resolve` | `escalate` |
| `expected.should_escalate` | `false` | `true` |
| `expected.escalation_trigger` | `''` | `intake_complete_for_uc_k` |
| `expected.allow_bot_resolution` | (absent) | `partial` |
| `expected.bot_handling_pattern` | (absent) | "Acknowledge the issue, run get_customer_context for safe_summary, collect required intake fields (platform, repro_steps_or_error_message), create_case_controlled, and hand over with reason intake_complete_for_uc_k." |
| `expected.expected_tool_sequence` | get_customer_context, answer_grounded, record_outcome | get_customer_context, create_case_controlled, request_handover, record_outcome |
| `expected.forbidden_tools` | search_knowledge, resolve_article, **moderation_enforcement_action**, **send_followup_email_or_async_update** | get_message_moderation_context, get_moderation_review_context, resolve_article, search_knowledge |
| `persona.user_goal_summary` | (was `goal_summary`: "Get help with: ... Expects bot to resolve the issue directly.") | "User reports Account Support: My ad isn't allowing customers to send requests. Drift: none." |
| `persona.hidden_facts` | [email is customer@example.com] | [] (form_context already has the email) |
| `scoring.outcome_checks` | resolution_achieved, intake_fields_collected | escalation_triggered, intake_fields_collected |
| `scoring.hard_checks` | no_forbidden_tools, budget_enforcement, fixed_script_adherence | phase_transition_validity, no_critical_policy_violation, no_pii_leakage, no_stall, no_human_only_tool_exposure, no_forbidden_tools, budget_enforcement, fixed_script_adherence |

The two human-only tools (`moderation_enforcement_action`,
`send_followup_email_or_async_update`) are now stripped from
per-case `forbidden_tools`; they are blocked globally by the new L1
`no_human_only_tool_exposure` hard-check.

## 4. cs_interactive_015 (UC-B -> UC-K)

`source_session_id=570Q5000008WmXxIAK`. The HR row tagged primary_uc
as UC-B (browse/messaging FAQ) but the transcript ("Hi - can you tell
me what happened to my ad?") is a UC-K runtime-state question about
the customer's own ad. The Wave A2.1
`UC_B_RECLASSIFICATION_OVERRIDES` entry corrects this.

| Field | OLD | NEW |
| --- | --- | --- |
| `expected.primary_uc` | `UC-B` | `UC-K` |
| `expected.secondary_ucs` | [UC-K] | [UC-FP, UC-B] |
| `expected.outcome_class` | `escalate` | `escalate` (unchanged) |
| `expected.escalation_trigger` | `user_distress` | `user_distress` (unchanged -- HR trigger valid for UC-K) |
| `expected.allow_bot_resolution` | (absent) | `partial` |
| `expected.bot_handling_pattern` | (absent) | "Acknowledge the issue, run get_customer_context for safe_summary, collect required intake fields (platform, repro_steps_or_error_message), create_case_controlled, and hand over with reason user_distress." |
| `expected.expected_tool_sequence` | search_knowledge, resolve_article, request_handover, record_outcome | **get_customer_context**, create_case_controlled, request_handover, record_outcome |
| `expected.forbidden_tools` | **get_customer_context**, **create_case_controlled**, moderation_enforcement_action, send_followup_email_or_async_update | get_message_moderation_context, get_moderation_review_context, resolve_article, search_knowledge |
| `expected.grounding_mode` | `faq_source_backed` | `fixed_script_only` |
| `expected.max_turns` | 15 | 10 |
| `persona.user_goal_summary` | (was `goal_summary`: "Get help with: ... Expects bot to escalate to human agent.") | "User reports Ad Support: Hi - can you tell me what happened to my ad?. Drift: soft_shift." |
| `persona.hidden_facts` | [email is customer@example.com] | [] |
| `scoring.hard_checks` | no_forbidden_tools, budget_enforcement, grounding_compliance | phase_transition_validity, no_critical_policy_violation, no_pii_leakage, no_stall, no_human_only_tool_exposure, no_forbidden_tools, budget_enforcement, fixed_script_adherence |

Critical: `get_customer_context` moved from the OLD `forbidden_tools`
list to the NEW `expected_tool_sequence`. Likewise
`create_case_controlled` is now an expected tool (UC-K controlled-case
creation), not a forbidden one. The OLD spec was actively contradicting
UC-K policy.

## 5. Linter violation delta

| Scope | Pre-regen (`case-spec-lint-pre-regen.md`) | Post-regen (`case-spec-lint-post-regen.md`) |
| --- | --- | --- |
| Specs scanned | 184 (anchor + smoke) | 392 (anchor + promotion + exploration + smoke) |
| Errors | 295 | 85 |
| Warnings | 736 | 100 |

The pre-regen lint scope deliberately excluded `promotion` and
`exploration` (not yet regenerated against the Wave A1.1 schema). The
post-regen lint covers the entire corpus, including the 25
hand-curated `smoke/` fixtures that were intentionally **not**
regenerated this wave.

Restricting the post-regen scope to the regenerated subdirs only:

| Scope | Specs | Errors | Warnings |
| --- | ---: | ---: | ---: |
| `anchor/` (NEW) | 159 | 0 | 0 |
| `promotion/` (NEW) | 101 | 0 | 0 |
| `exploration/` (NEW) | 107 | 0 | 0 |
| `smoke/` (legacy, untouched) | 25 | 35 | 100 |

In other words, **the regenerated 367 specs are clean**: every R0 /
R5 / R8 / R9 / R11 violation in the 392-spec post-regen lint comes
from the 25 legacy `smoke/` fixtures. The remaining R13 errors (50)
and R1 errors (10) are cross-spec checks that fire because anchor
and smoke share `case_id` namespace (`cs_interactive_001`,
`_015`, `_040`, etc. exist in both).

Top 3 remaining lint rules in the post-regen full-corpus report:
1. `R13 source_session_no_duplication` x 50 -- anchor/smoke duplicates
2. `R0 legacy_field_name` x 25 -- smoke fixtures still use `goal_summary`
3. `R5 no_human_only_tool_in_forbidden` x 25 -- smoke fixtures still
   list `moderation_enforcement_action` / `send_followup_email_or_async_update` per-case

All three categories are owned by the smoke fixtures and would be
resolved if smoke were regenerated (or moved out of the lint scope) in
a follow-up wave.

## 6. Files changed in this wave

- `eval_interactive/eval_interactive/case_spec/loader.py` -- accept
  `user_goal_summary` (preferred) or legacy `goal_summary`; default
  the new `Expected.allow_bot_resolution` / `bot_handling_pattern`
  fields and normalise empty `escalation_trigger` -> None.
- `eval_interactive/eval_interactive/case_spec/extractor.py`:
  - new `OUTCOME_OVERRIDES` per-session map (currently 1 entry,
    `570Q5000008pMbOIAU`).
  - extended `_filter_redundant_hidden_facts` to drop
    `ad id is REDACTED_AD_ID` style facts when `form_context.ad_id`
    is already set.
  - audit dict now records `outcome_override_applied`.
- `eval_interactive/scripts/regenerate_case_specs.py` (new) +
  `eval_interactive/scripts/__init__.py` (new) -- driver script.
- `eval_interactive/case_specs/{anchor,promotion,exploration}/*.yaml`
  (367 files) -- regenerated.
- `qa-reports/case-spec-generation-audit.md` -- new (extractor audit
  dump).
- `qa-reports/case-spec-lint-post-regen.md` -- new (post-regen lint
  output).
- `qa-reports/case-spec-regen-diff.md` -- this file.
