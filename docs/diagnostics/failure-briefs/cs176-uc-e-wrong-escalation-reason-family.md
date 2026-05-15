# Failure Brief — cs176 UC-E wrong escalation_reason family

> **Source case:** `cs_interactive_176` (UC-E primary, UC-B / UC-K secondary, expected `escalate`, trigger=`user_requested`).
> **Source session:** `570Q5000008NMRRIA4`.
> **CaseSpec:** `eval_interactive/case_specs/smoke/cs_interactive_176.yaml`.
> **Approved L3 override:** NONE (greped `case_spec_overrides.yaml`; no entry for this `source_session_id`).
> **Runs:** `20260505-235231` (FAIL, composite=0.0, L1:escalation_compliance + L3:relevance/tone=2.0). 2026-05-10 regressed further (active_use_case=UC-I mis-route + source_citation_present fail) — Cluster C, deferred via `R-smoke-regression-investigation`.
> **Filed:** 2026-05-13 (Sprint 18 / G1, brief #7).

## Ground-truth chain

cs_176 has no approved L3 override. The CaseSpec is rule-extracted L1 + L2 LLM persona review (`expected.*` and `classification.*` fields are rule-extracted defaults, not human-reviewed). Per the G1 method note, this brief should flag the CaseSpec for L3 triage if substantive concerns surface.

Ground truth: UC-E (paid Top Ad / Featured placement), `outcome_class: escalate`, `should_escalate: true`, `escalation_trigger: user_requested` (family=user_intent), `bot_handling_pattern: "Acknowledge the issue, run policy-mandated tools (...), collect required intake fields (none), and hand over with reason user_requested"`. Expected tool sequence: `search_knowledge → resolve_article → request_handover → record_outcome`. Forbidden tools include `get_customer_context` (UC-E is not on the phase 2 line 358 allowed list — no CaseSpec/phase 2 conflict). UC-E does not include `tool_sequence_match` in `outcome_checks`, so tool sequence is not L2-scored for this case.

`escalation_trigger: user_requested` matches phase 2 UC-E policy intent — UC-E is "user paid for a service and wants the service delivered or refunded", which is fundamentally a user-intent demand rather than a knowledge gap. Whether `user_requested` is the optimal label (versus a hypothetical UC-E-specific reason like `refund_or_fulfillment_required`) is a separate L3 review question — see Related observation.

The 2026-05-10 run regression (UC-I mis-route, source_citation_present fail) is Cluster C / deferred.

## What happened?

User entered via the Technical Support form: `form_context.first_name="Gary"`, description "I pad to put my ad for my business and it's not at the top", empty ad_id, persona `frustration_level: high`, `drift_behavior: soft_shift`. The 2-turn transcript:

- **T0 user** (form): "I pad to put my ad for my business and it's not at the top"
- **T0 bot:** "Hi Gary! Hi Gary, I can help with that. To give you the right guidance, could you clarify: are you asking why your ad isn't showing at the top of search results, or did you pay for a featured/promoted placement and it's not appearing?"
- **T1 user:** "Give me my £50 back or put my business to the top like I paid for"
- **T1 bot:** "Let me connect you with a specialist."

The bot's T0 was a genuine clarification question (a real engagement, unlike Cluster B's turn-0 template escalates). The user's T1 answered with a clear UC-E user-intent demand: **either £50 refund or fulfillment of the top-placement service paid for**. The bot escalated on T1, but stamped `escalation_reason=faq_miss_threshold_exceeded`.

Three failures stack:

1. **Cross-family escalation_reason (L1 hard fail).** Expected `user_requested` (family=user_intent — user is asking for action). Actual `faq_miss_threshold_exceeded` (family=bot_limit — bot couldn't find an answer). Cross-family means the downstream routing signal is wrong: a user-intent demand should route to the team that can act on it (refund / billing); a bot-limit signal routes to a generic specialist for FAQ assistance. The L1 `escalation_compliance` check correctly hard-failed this — composite drops to 0.0 despite L2 `correct_outcome=1.0`, `correct_uc=1.0`, `escalation_timing=1.0`, `handover_completeness=1.0`.

2. **Self-stamped reason without runtime evidence.** The bot stamped `faq_miss_threshold_exceeded` but the trace shows no `search_knowledge` / `resolve_article` calls (UC-E doesn't include `tool_sequence_match` in `outcome_checks`, so this is inferred from the transcript and tool-related L2 absence). Even if FAQ search did happen silently, `faq_miss_threshold_exceeded` is the wrong semantic family for a user-demanding-action case. This is the same self-stamping pattern as cs_040's `intake_complete_for_uc_k` claim without intake collection — a runtime contract gap on whether escalation_reason should require corresponding session evidence.

3. **Duplicated greeting in T0** ("Hi Gary! Hi Gary"). Same projection / template rendering bug as cs_095.

## What should a good CS agent have done?

The capability gap is **escalation-reason selection grounded in what actually happened in the session and what the user is asking for**, not in a default-on-ambiguity reason.

Concretely:

- **Distinguish user-intent escalation from bot-limit escalation.** When the user explicitly demands an action (refund, fulfillment, account change, etc.) that the bot cannot perform, the escalation is *user-intent* — pick a user_intent-family reason (`user_requested` per CaseSpec, or a UC-E-specific reason if one exists). When the bot has actually attempted FAQ search and failed, the escalation is *bot_limit* — pick `faq_miss_threshold_exceeded`. The two families are not interchangeable; downstream routing depends on the distinction.

- **Tie the reason to session evidence.** `faq_miss_threshold_exceeded` should require at least one `search_knowledge` / `resolve_article` call returning below the answer-gate threshold. `intake_complete_*` should require the named intake fields populated (per cs_040). The runtime should not allow the bot to free-choose evidence-claiming reasons without the corresponding state.

- **Acknowledge the user's demand in plain language before escalating.** The cs_176 T1 user message names a specific monetary amount (£50) and a specific service expectation (top placement). A real reply would name both back ("£50 refund or top placement fix — let me get someone who can act on that") before the handoff message. The current "Let me connect you with a specialist." discards both pieces of explicit user information.

- **Surface choice.** Render greeting once.

## Why does this matter?

`escalation_reason` is a downstream routing signal, not just an eval label. Different reasons route to different human teams (refund / billing for refund cases, generic CS for FAQ misses, trust & safety for fraud, etc.). A cross-family wrong reason sends the user to the wrong queue — at best wasting time, at worst leaving the case in a queue that can't act on the demand. For cs_176, a `faq_miss_threshold_exceeded` reason likely routes to generic CS; the user's £50 refund demand requires refund / billing handling, which is what `user_requested` (per CaseSpec) is meant to trigger.

This also matters as **systematic evidence of a runtime-contract gap on escalation_reason**: cs_040 self-stamped `intake_complete_for_uc_k` without intake collection; cs_176 self-stamped `faq_miss_threshold_exceeded` without FAQ search evidence. Two confirmed instances of the same pattern across two UCs and two different reasons. The runtime should treat `escalation_reason` as an *evidence-bound claim*, not a free-choice LLM output — at least for the subset of reasons that name a specific session event (`*_complete_*`, `*_threshold_exceeded`, `clarification_budget_exhausted`, etc.). This is `cs_040`'s prior R-item broadened to the full evidence-required-reason set — see Related observation.

cs_176 also reinforces the duplicated-greeting projection bug observed in cs_095 — same surface, different case. Likely a single projection / template fix would address both.

## Is this a one-off or a pattern?

`pattern` on two dimensions:

1. **Self-stamped escalation_reason without runtime evidence.** Confirmed in cs_040 (`intake_complete_for_uc_k` without intake) and cs_176 (`faq_miss_threshold_exceeded` without FAQ search evidence). Two instances across UC-K and UC-E. Likely affects any UC where the bot reaches a handoff path the LLM can label freely.

2. **Cross-family escalation_reason mismatch on UC-E user-intent demands.** cs_176 is the only UC-E case in the smoke set, so single-case evidence on this specific UC dimension. But the underlying root (planner / projection doesn't differentiate user_intent vs bot_limit escalation families) is likely applicable wherever UC-E-class cases appear.

The duplicated-greeting bug is a third pattern (cs_095 + cs_176), separately tracked.

## Which layer is likely responsible?

Three candidate layers:

- **`semantic_planner`** (primary). Given the T1 user message ("Give me my £50 back or put my business to the top"), the LLM chose `faq_miss_threshold_exceeded` over `user_requested`. The LLM had both options in the enum; it picked the wrong family. This is a planner-side reason-selection failure.

- **`prompt_projection`** (secondary). The projection / prompt may not differentiate the two escalation families clearly. If the prompt directs the bot to default to faq_miss when "no resolve path is available" without distinguishing "user demand the bot can't fulfil" (user_intent) from "user question the FAQ can't answer" (bot_limit), this is a projection gap. Also responsible for the duplicated greeting.

- **`runtime contract` (Tier-0 candidate, broadened from cs_040)** — if the runtime were to enforce "escalation_reason must be evidence-bound for the evidence-claiming subset", the LLM could not have self-stamped `faq_miss_threshold_exceeded` without a corresponding FAQ-search event. This is the broadened version of cs_040's prior R-item — see Related observation.

`eval_spec` is partially live for the question of whether `user_requested` is the optimal expected reason for a UC-E refund demand (or whether a UC-E-specific reason should exist) — see Related observation. This does not change the bot-side failure: even if the expected reason were renamed, the bot's `faq_miss_threshold_exceeded` would still be a cross-family mismatch.

## What should NOT be done?

- Do **not** add a regex / keyword on "£50 back" / "I paid for" / "give me my money" → force-route to `user_requested`. Keyword chatbot regression Constitution §1.5.
- Do **not** add a Java guard requiring every UC-E case to stamp `user_requested`. Too rigid; UC-E cases with a genuine faq_miss (e.g. policy question about Featured placement rules) should still be allowed `faq_miss_threshold_exceeded`.
- Do **not** edit cs_176's CaseSpec to accept `faq_miss_threshold_exceeded` as a passing reason. The L1 escalation_compliance cross-family check is correctly hard-failing this; loosening it would mask a real failure family.
- Do **not** broaden the runtime-contract Tier-0 candidate to *all* escalation_reasons without scoping. Some reasons (e.g. `user_distress` — handled by Sprint 4 §E1's deterministic detector) already have evidence gates; some (e.g. `user_requested`) are subjective and don't naturally bind to a runtime event. Scope the contract carefully — see Related observation.

## Related observation — three R-items

1. **`R-escalation-reason-runtime-evidence-contract-review`** (broadened from cs_040's prior `R-intake-complete-runtime-contract-review`). Two instances now confirm a pattern: cs_040 self-stamped `intake_complete_for_uc_k` without intake collection; cs_176 self-stamped `faq_miss_threshold_exceeded` without FAQ-search evidence. Should the runtime enforce that escalation_reasons claiming session events (`*_complete_*`, `*_threshold_exceeded`, `clarification_budget_exhausted`, etc.) require the corresponding event to be present in session state? Potential Tier-0 candidate, scoped to the evidence-claiming subset (NOT subjective reasons like `user_requested` or `user_distress` which have their own contracts). cs_040's brief has been retroactively updated to reference this broadened item.

2. **`R-cs176-escalation-reason-l3-review`** (eval_spec). Is `user_requested` the optimal expected reason for a UC-E refund-or-fulfillment demand, or should UC-E have a more specific reason (e.g. `refund_or_fulfillment_required`)? The current expected reason works (matches user_intent family direction) but is not UC-E-specific. Wave A5/A6 L3 review may produce an approved override or a phase 2 escalation-reason enum extension proposal.

3. **`R-duplicated-greeting-projection-fix`** (prompt_projection / surface bug). Confirmed in cs_095 ("Hi Trish! Hi Trish") and cs_176 ("Hi Gary! Hi Gary"). Likely a single projection / template fix. Not in cs_095's brief R-item list (cs_095 surfaced the bug but didn't open a backlog item); the second confirmed instance graduates it to a tracked backlog item.
