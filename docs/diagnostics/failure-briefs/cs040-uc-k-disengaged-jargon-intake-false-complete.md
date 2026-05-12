# Failure Brief — cs040 UC-K disengaged escalate with internal jargon framing and false intake-complete claim

> **Source case:** `cs_interactive_040` (UC-K primary, no secondary, expected `escalate`, trigger=`intake_complete_for_uc_k`, intake fields=`[platform, repro_steps_or_error_message]`).
> **Source session:** `570Q5000008pMbOIAU`.
> **CaseSpec:** `eval_interactive/case_specs/smoke/cs_interactive_040.yaml`.
> **Approved L3 override:** NONE.
> **Runs:** `20260505-235231` (outcome-PASS, composite=0.7531, L2:tool_sequence_match=0.25 + L3:relevance/tone=2.0). 2026-05-10 regressed to FAIL with `active_use_case=UC-C` mis-route + multiple L1 fails — Cluster C, deferred via `R-smoke-regression-investigation`.
> **Filed:** 2026-05-12 (Sprint 18 / G1, brief #5).

## Ground-truth chain

cs_040 has no approved L3 override entry in `case_spec_overrides.yaml`. The CaseSpec is rule-extracted L1 + L2 LLM persona review.

**Data source context.** Per `docs/proposals/interactive_case_spec_generation_plan.md`, CaseSpec generation consumes (1) Phase 2 policy, (2) HR annotation row, and (3) selected source transcript. The source transcript captures the **human-agent ↔ user dialogue**; there is no bot in the original data. The CaseSpec encodes what the bot *should* do per UC-K policy (intake + handover), not what the human-agent did in the original session. This is consistent with phase 2 UC-K's `allow_bot_resolution: partial` (intake + handover only, no resolve), so the CaseSpec's `outcome_class: escalate` is the correct bot expectation regardless of how the original human-agent closed the underlying issue.

**Prior reviewer notes (resolved in current spec).** A human review of cs_040 previously surfaced three concerns that the current spec state already addresses:

- `outcome_class` is `escalate` (not `resolve`), matching UC-K's intake+handover policy.
- `send_followup_email_or_async_update` is **not** in cs_040's `expected_tool_sequence` or `forbidden_tools` — it is a Human-Only Tool (`visibility: human_only` per `customer_service_tool_spec_v0_2.yaml`; declared in `phase0_normative_freeze.md` §0.3 and `phase2_domain_realization_spec.md` §2.10.3 as never bot-callable). Exposure is enforced by the global L1 check `no_human_only_tool_exposure` (`phase5_evaluation_design.md` line 951, zero-tolerance), which cs_040's 2026-05-05 run passed.
- Persona `user_goal_summary` describes the user's underlying goal ("User wants the request-quote button fixed"), which is broader than the bot's task scope (intake + handover for UC-K). The current wording is internally consistent (intake is the bot's stage en route to "fixed"), but the scope distinction is a separate persona-construction concern — see Related observation #4.

**Ground truth.** UC-K (Technical defect / bug report), `allow_bot_resolution: partial`, escalation_trigger `intake_complete_for_uc_k`, intake fields `[platform, repro_steps_or_error_message]`, expected tool sequence `get_customer_context → create_case_controlled → request_handover → record_outcome`, `grounding_mode: fixed_script_only`.

The 2026-05-05 run outcome PASSED by `correct_outcome` + `correct_uc` + `case_id_present` (composite 0.7531). But **the bot did not run intake** — only 2 turns, 2 tool calls (`request_handover`, `create_case_controlled`), zero intake questions. The escalation_reason `intake_complete_for_uc_k` is the bot's **claim**, not a state established. tool_sequence_match=0.25. L3 relevance=2.0, tone=2.0.

UC-K is allowed `get_customer_context` per phase 2 line 358 — no CaseSpec/phase 2 conflict here, the `R-generator-get-customer-context-policy-mismatch` issue does not extend to cs_040.

The 2026-05-10 regression (UC-C mis-route + multiple L1 fails) is Cluster C / deferred.

## What happened?

User entered via the Account Support form: `form_context.first_name="Mo"`, description "My ad isn't allowing customers to send requests. There is a flaw on the app", empty ad_id, persona `frustration_level: high`. The 2-turn transcript:

- **T0 user** (form): "My ad isn't allowing customers to send requests. There is a flaw on the app"
- **T0 bot:** "Hi Mo! I'm here to help with your inquiry about technical issue intake. Let me look into this for you."
- **T1 user:** "There seems to be issues with my ad"
- **T1 bot:** "Let me connect you with a specialist."

Three failures compound:

1. **Internal-jargon framing.** T0 opens with "your inquiry about technical issue intake" — same internal UC label pattern as cs_038's "trust & safety report intake". The user's specific form description ("ad isn't allowing customers to send requests... flaw on the app") was reframed into generic internal jargon with zero acknowledgment of the actual problem.

2. **Disengaged-escalate after the framing.** Unlike cs_038 (4 turns of partial intake before escalating), cs_040 escalated on turn 1 with no intake whatsoever. Required intake fields (`platform`, `repro_steps_or_error_message`) were never asked. The user's "There seems to be issues with my ad" T1 reply could have been a re-entry into intake; the bot went straight to escalation.

3. **False `intake_complete_*` claim.** The bot self-stamped `escalation_reason=intake_complete_for_uc_k` despite having collected zero intake fields. The escalation_reason semantically *means* "intake completed for UC-K"; no runtime check appears to verify the fields are populated. Potential Tier-0 candidate (see Related observation).

The L3 judge also flagged "uses an incorrect name ('Mo') not provided by the user" — but `form_context.first_name="Mo"`. Same form-context-trust judge calibration issue as cs_038 — reinforces `R-l3-judge-form-context-trust-rubric`.

## What should a good CS agent have done?

- **User-facing language, not internal labels.** "Your inquiry about technical issue intake" should be e.g. "what's happening with your ad and the request-quote flow". (Same point as cs_038.)
- **Actually collect intake.** UC-K's required fields (`platform`, `repro_steps_or_error_message`) map naturally to "are you on web or the mobile app?" and "what exactly happens when a customer tries to send a request — error message? blank screen? something else?". These were not asked. After answers, restate → `get_customer_context` → `create_case_controlled` → `request_handover` → `record_outcome`.
- **Trust `form_context.first_name="Mo"`** — same as cs_038.

## Why does this matter?

UC-K (Technical defect / bug report) is medium-risk: users are typically paying customers reporting a real product issue. cs_040's persona `frustration_level: high`; seed_messages explicitly say "Can you tell me what am I supposed to do if I'm paying for this service but the customers cannot reach me". A bot that escalates with empty intake + jargon framing wastes both the user's time and the downstream human-agent's time — the human has to start intake from scratch.

The deeper concern is the **bot self-stamping `intake_complete_for_uc_k` without doing intake**: the escalation_reason claims a state that did not occur. If downstream routing or eval scoring relies on `intake_complete_*` meaning "fields collected", that assumption is broken. (Potential `runtime_freeze_and_risk_policy` review item — see Related observation.)

cs_040 also confirms there is a **third Cluster B sub-pattern**: "disengaged + jargon-framing" hybrid, distinct from cs_001-style pure disengagement and cs_038-style engaged-mechanical. All three are mechanical surfaces, but they fail in different layer-combinations.

## Is this a one-off or a pattern?

`pattern`. The "disengaged + jargon" hybrid is also visible in cs_066 (UC-K, similar — bot says "I understand you're having trouble adding your phone number" then escalates after one platform-clarification turn; cs_066 engaged marginally more than cs_040). cs_038 (engaged-mechanical) and cs_040 (disengaged-jargon-hybrid) are both UC-K / UC-J intake-required UCs that the bot fails by either over-asking or skipping intake entirely. The common root: intake state isn't anchored in the bot's planning.

## Which layer is likely responsible?

Three candidate layers:

- **`prompt_projection`** (primary). The prompt likely echoes UC labels as user-facing framing (same root as cs_038). Fix: projection rule that distinguishes internal UC label from user-facing topic phrasing.

- **`semantic_planner`** (secondary). Even with adequate projection, the LLM chose to escalate on turn 1 without doing intake, then self-stamped `intake_complete`. The Sprint 11 ResolveDisposition / intake state mechanics may not enforce intake before allowing `intake_complete_*` reasons in the planner.

- **`runtime contract` / Tier-0 candidate** (tertiary, see Related observation). Whether `request_handover(intake_complete_for_uc_*)` should be gated at the runtime / java layer (Tier-0 invariant: "claim of intake completion requires fields populated") is a design call. The brief flags this without prescribing. cs_176 (UC-E, separate brief) shows the same self-stamping pattern with `faq_miss_threshold_exceeded`, broadening this concern to the full evidence-claiming reason subset — see Related observation.

`eval_spec` is partially live for the form-context judge calibration (shared with cs_038).

## What should NOT be done?

- Do **not** rename the UC-K label internally to avoid "technical issue intake" framing. Same layering-violation argument as cs_038.
- Do **not** add a keyword rule mapping "ad isn't working" / "flaw on the app" → a UC-K-friendly opener. Keyword chatbot regression.
- Do **not** add a Java guard on `request_handover(intake_complete_for_uc_k)` requiring specific field names UNLESS this is approved as a Tier-0 invariant via `runtime_freeze_and_risk_policy` review (it's a candidate — see Related observation).
- Do **not** edit cs_040's CaseSpec to lower the L3 bar or remove `intake_fields_collected` from outcome_checks. The L3 failure and the intake-skip are both real.
- Do **not** treat "Mo" as a bot error. `form_context.first_name="Mo"`. Same as cs_038.

## Related observation — runtime contract candidate + reinforced eval issues + persona scope

1. **`R-escalation-reason-runtime-evidence-contract-review`** (Tier-0 candidate, broadened to cover all evidence-claiming escalation_reasons). cs_040 shows the bot can self-stamp `intake_complete_for_uc_k` with zero intake fields collected. cs_176 (UC-E, separate brief) shows the bot can self-stamp `faq_miss_threshold_exceeded` without `search_knowledge` evidence. Two instances across UC-K and UC-E confirm a pattern: the runtime allows `escalation_reason` as a free-choice LLM output even when the reason names a specific session event (`*_complete_*`, `*_threshold_exceeded`, `clarification_budget_exhausted`, etc.). Track in `docs/action_bank.md` as **`R-escalation-reason-runtime-evidence-contract-review`** — should the runtime enforce evidence-binding for the evidence-claiming reason subset, gated at the runtime layer? Scope: the subset that names a session event, NOT subjective reasons like `user_requested` or `user_distress` which have their own contracts (`user_distress` already gated by Sprint 4 §E1 deterministic detector). Potential Tier-0 candidate if approved.

2. **L3 judge over-reaches on form_context trust.** Reinforces cs_038's existing `R-l3-judge-form-context-trust-rubric`. cs_040 with "Mo" is additional evidence of the same systematic judge_calibration issue. No new R-item needed.

3. **cs_040 CaseSpec would benefit from Wave A5/A6 review.** Specifically: should `escalation_reason=intake_complete_for_uc_k` paired with `intake_fields_collected=0` be a hard outcome fail (not just an L2 partial score)? Track as `R-cs040-l3-review-intake-completion-semantics`.

4. **Persona `user_goal_summary` scope (conditional).** cs_040's persona goal "User wants the request-quote button fixed" describes the user's ultimate desired outcome (button fixed), which is broader than what the bot's UC-K task can deliver (intake + handover, no fix). The two are logically consistent (intake is one stage toward fix), but the L2 LLM persona reviewer (Wave A6) might benefit from a guardrail to scope `goal_summary` to "what the user wants the conversation to produce" rather than "what the user ultimately wants fixed". This is a persona-construction quality concern, not a cs_040-specific fix. Track conditionally as `R-persona-goal-summary-scope-clarity` only if other smoke cases show the same scope-broadening pattern (review during G2 case-family construction).
