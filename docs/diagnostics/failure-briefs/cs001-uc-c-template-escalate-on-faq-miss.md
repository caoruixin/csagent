# Failure Brief — cs001 UC-C template escalate on FAQ-miss

> **Source case:** `cs_interactive_001` (UC-C primary, UC-D secondary, expected `escalate`, trigger=`clarification_budget_exhausted`).
> **Source session:** `570Q5000008kr6LIAQ`.
> **CaseSpec:** `eval_interactive/case_specs/smoke/cs_interactive_001.yaml`.
> **Approved L3 override:** NONE (greped `case_spec_overrides.yaml`; no entry for this `source_session_id`).
> **Runs:** `20260505-235231` (outcome-PASS, composite=0.7714, L3 relevance/tone=2.0) and `20260510-134558` (outcome-PASS, composite=0.819, L3 relevance=2.0).
> **Filed:** 2026-05-12 (Sprint 18 / G1, brief #2).

## Ground-truth chain

cs_001 has **no approved L3 override**. The CaseSpec is the L1 rule-extracted version plus L2 LLM persona review (Wave A6 cache, persona-only). The `expected.*` and `classification.*` fields are **rule-extracted defaults, not human-reviewed**. Per the G1 method note (to be added to `iteration_governance.md` §2 during G1 packaging), this brief should flag whether the CaseSpec itself needs L3 triage as a separate `eval_spec` candidate.

The CaseSpec has an internal inconsistency: `escalation_trigger: clarification_budget_exhausted` together with `bot_handling_pattern: ... collect required intake fields (none)` — there is no clarification budget to exhaust if no clarification fields are required. The bot's actual `escalation_reason=faq_miss_threshold_exceeded` is more semantically accurate. The L1 `escalation_reason_consistency` check passes regardless (likely because it accepts any valid enum value), so the spec inconsistency does not block outcome scoring. **This is a CaseSpec generator artifact worth L3 review.** It is independent of the bot's L3-quality failure described below.

The bot's outcome (escalate) IS correct — UC-C with no FAQ resolve-grade hit warrants escalation. **This brief is about the bot's L3-quality failure (mechanical template, zero acknowledgment), not its L2-outcome behaviour.**

## What happened?

User said via form_context "I am unable to receive messages on my account" (topic_subject "Replies & Messaging", empty `ad_id`). Bot turn 0: "Hi Benjamin! I'm having difficulty resolving this. Let me connect you with a specialist." — a fixed template with no acknowledgment. On turn 1, user clarified confusion about two emails ("I am confused i see you guys both of my email but when i log in i use [EMAIL]"); bot turn 1 replied only "This conversation has been transferred to a human agent. Please wait for them to respond."

Behind the scenes the bot ran `search_knowledge` + 3× `resolve_article` + `request_handover` — tool work happened — but none was reflected in the user-facing message. `get_customer_context` (the first tool in the CaseSpec's expected sequence) was not called; whether that is a bot omission or a runtime policy block depends on phase 2 enforcement (see Related observation). Outcome: escalate (correct); composite 0.7714 PASS; L3 `relevance=2.0`, `tone_appropriateness=2.0`.

## What should a good CS agent have done?

Even when escalation is the right outcome, the *way* the agent escalates should respect what the user said:

- Acknowledge the specific complaint ("you're not receiving messages on your account") instead of generic "I'm having difficulty".
- Confirm scope before handing off if the complaint is ambiguous ("by 'messages' do you mean replies from buyers in your Gumtree inbox, or email notifications?") — Replies & Messaging covers multiple sub-shapes.
- When handing off, name *why* the bot is handing off ("I couldn't find an article that covers this — let me get a specialist") rather than disguising the failure as generic "difficulty".
- The user's turn 1 follow-up about two emails was a real diagnostic signal (different login email vs displayed email is a known UC-D / account-config pattern) — a human agent would have picked it up; the bot ignored it entirely.

A separate question is whether the bot should also have called `get_customer_context` first (per the CaseSpec's expected tool sequence). Phase 2 §2.10 line 358 currently restricts this tool to UC-A/UC-FP/UC-K, excluding UC-C — see Related observation. That conflict does not change the L3 acknowledgment failure above: the bot could and should have written a specific acknowledgment regardless of which tools were available.

## Why does this matter?

This is the canonical "agent → chatbot" regression the Constitution exists to prevent (§1.2 LLM owns response strategy / natural customer-facing wording; §1.5 Iteration rule). The CaseSpec scores cs_001 PASS by outcome, but L3 `relevance=2.0` and `tone_appropriateness=2.0` reveal the gap: the user experience is indistinguishable from a no-LLM keyword router. If we optimize on outcome / UC / escalation correctness alone we reinforce this pattern. Cluster B briefs (cs_001, cs_011, cs_038, cs_040, and cs_015's secondary surface) together show this is the dominant bot surface when FAQ search misses.

## Is this a one-off or a pattern?

`pattern`. Same template surface confirmed in: cs_002 (UC-C notifications, both runs), cs_011 (UC-D login detail ignored), cs_014 (UC-C messaging + dual-email), cs_038 (UC-J scam report, with extra mis-framing as "trust & safety report intake"), cs_040 (UC-K ad request failure, with extra wrong-name "Mo"), cs_015 (UC-FP turn-0 same template, separate brief). Across runs `20260505-235231` and `20260510-134558` the phrasing is stable: "Hi {Name}! I'm having difficulty resolving this. Let me connect you with a specialist." is the most common bot turn-0 response in the smoke set when no FAQ resolves the issue.

## Which layer is likely responsible?

Two candidate layers; §3.2 first-match-wins resolves to one but both may be live:

- **`prompt_projection`** (primary). The bot ran search_knowledge + 3× resolve_article (which presumably scored below the answer-gate, hence faq_miss). On turn 0 the LLM saw "search returned no usable hit; recommend escalation" but the projected context likely did not surface either the user's literal complaint text or the empty-result narrative in a way the prompt could turn into a specific acknowledgment. The 100% reliability of the template phrasing across cases strongly suggests the system prompt directs the LLM to a fixed response shape on faq_miss — that is the prompt-side variant of the same projection failure.

- **`semantic_planner`** (secondary). Even with adequate projection, the LLM could be defaulting to the template. If projection turns out to include the user's complaint text and the empty-result narrative, the failure is the planner choosing generic over specific. §3.2 disambiguates by reading the actual turn-0 `projected_context` and the system prompt's faq_miss branch.

`eval_spec` is **partially live** for two separate CaseSpec issues — `escalation_trigger` inconsistency and the `get_customer_context` ↔ phase 2 conflict (see Related observation). Neither changes the bot-side failure classification — the L3 quality gap stands on its own.

## What should NOT be done?

- Do **not** add a regex / keyword on "can't receive messages" / "not getting notifications" / "messages on my account" and route to a UC-C-specific response template. That is the keyword chatbot regression the Constitution's §1.5 Iteration rule forbids.
- Do **not** add a new `escalation_reason` enum value (e.g. `faq_miss_acknowledged` vs `faq_miss_template`) to differentiate "agent acknowledged user" from "agent gave template". The enum is frozen per Sprint 13 / runtime freeze policy; this moves a soft signal into the wrong layer.
- Do **not** add a Java guard requiring the bot to echo a substring of the user's message before `request_handover`. Too rigid; would break legitimate quick handovers like cs_029 (UC-D explicit `user_requested`, 4 turns, no echo, scores 0.9667). The right surface for "did the bot acknowledge" is L3 judge dimensions + prompt-side wording, not Java.
- Do **not** edit cs_001's CaseSpec to start failing on outcome — outcome is correct (UC-C, escalate, faq_miss-equivalent reason). The failure lives on L3 relevance / tone; widening the outcome rubric would mask a genuine bot bug per `iteration_governance.md` §5 eval-override rule.
- Do **not** "fix" the CaseSpec's `escalation_trigger` or `expected_tool_sequence` inline without going through the L3 review process — the Wave A5/A6 pipeline exists precisely so spec changes carry rationale + reviewer + date + supporting_turn_numbers. Patching the YAML directly bypasses governance.

## Related observation — cs_001 CaseSpec needs L3 triage on two fields

Two CaseSpec issues surfaced during this brief, both worth L3 review via the Wave A5/A6 pipeline:

1. **`escalation_trigger` inconsistency.** The trigger `clarification_budget_exhausted` conflicts with `intake fields (none)` in the same `bot_handling_pattern`. The bot's actual `faq_miss_threshold_exceeded` is more semantically accurate. Track in `docs/action_bank.md` as `R-cs001-escalation-trigger-l3-review`.

2. **CaseSpec ↔ phase 2 tool-policy conflict on `get_customer_context`.** The `expected_tool_sequence` starts with `get_customer_context`, but phase 2 §2.10 line 358 restricts that tool to UC-A/UC-FP/UC-K (excluding UC-C). The bot's actual skipping of the tool is consistent with phase 2 but inconsistent with the CaseSpec. Either phase 2 should widen UC-C's allowance (`product_policy` review) or the CaseSpec generator over-included the tool for UC-C (generator bug + L3 override). cs_011 (UC-D, separate brief) shows the same mismatch, confirming this is a systematic generator-vs-policy issue rather than per-case. Track in `docs/action_bank.md` as **`R-generator-get-customer-context-policy-mismatch`** (systematic version covering cs_001 / cs_011 / any further smoke cases).

   > **Correction (Sprint 22, 2026-05-14):** the claim that phase 2 §2.10 line 358 restricts `get_customer_context` to UC-A / UC-FP / UC-K is based on a misread of UC-H-local prose inside the UC-H-01 YAML block. The normative cross-UC allowlist at §2.10.1 line 1098 explicitly permits the tool for UC-C (corroborated by `docs/customer_service_tool_spec_v0_2.yaml` line 260 and `docs/current/customer_service_tool_spec_v0_3.md` line 60). The CaseSpec generator was correct; no policy widening is needed. The residual question — does the bot actually use the tool when account-state matters? — is captured in `R-uc-cdf-get-customer-context-bot-actual-usage` (action_bank §5.2). The originally-proposed `R-generator-get-customer-context-policy-mismatch` and its routed-to `R-phase2-uc-cdf-customer-context-policy-widen` are both closed.

Both items are `eval_spec` and/or `product_policy` candidates; neither blocks the bot-side `prompt_projection` / `semantic_planner` failure described above. Both should be triaged via the Wave A5/A6 review process, not inline patches.
