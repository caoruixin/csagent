# Failure Brief — cs011 UC-D detailed technical description ignored on FAQ-miss

> **Source case:** `cs_interactive_011` (UC-D primary, UC-K secondary, expected `escalate`, trigger=`faq_miss_threshold_exceeded` per Sprint 4 §E1 override).
> **Source session:** `570Q5000008NWIjIAO`.
> **CaseSpec:** `eval_interactive/case_specs/smoke/cs_interactive_011.yaml`.
> **Approved L3 override:** Sprint 4 §E1 supporting fix (2026-05-05, human semantic review, high confidence, supporting_turn_numbers [3, 5, 6, 23, 24, 25, 27]).
> **Runs:** `20260505-235231` (outcome-PASS, composite=0.8, L3 relevance/tone=2.0). 2026-05-10 regressed to FAIL with empty escalation_reason — Cluster C, deferred via `R-smoke-regression-investigation`.
> **Filed:** 2026-05-12 (Sprint 18 / G1, brief #3).

## Ground-truth chain

cs_011 HAS an approved L3 override. The override pins `escalation_trigger: faq_miss_threshold_exceeded` after the Sprint 4 §E1 runtime gate in `ControlKernel.applyEscalationReason` downgrades unconfirmed `user_distress` claims (the bot LLM cannot self-stamp `user_distress` unless the deterministic Sprint §B1 detector already fired). cs_011's seed messages contain no DISTRESS_PATTERN match and no ALL-CAPS shout, so the runtime downgrade applies, and the FAQ corpus has no resolve-grade article for the password-reset-loop shape — hence faq_miss is the truthful reason.

CaseSpec's `expected.escalation_trigger` matches both the runtime behaviour and the bot's actual emitted reason. The bot's outcome (escalate with faq_miss) IS correct in the 2026-05-05 run. **This brief is about the L3-quality failure: ignoring a high-frustration user's detailed technical description and template-escalating.** The 2026-05-10 run's regression to outcome-FAIL with empty escalation_reason is Cluster C / deferred and not this brief's scope.

## What happened?

User opened the chat from the Account Support form with description "Login issue" (empty `ad_id`). Persona: `frustration_level: high`, `verbosity: verbose`, trading name "Fair Deal Vehicles Ltd" (hidden fact). First user message:

> "Your be the third agent today to try n resolve the issue im having. I've clearly explained to the previous to agents that I am unable to log into my account. When I try to login it says pw or email incorrect. I then get a pw reset link sent to my email. I use this link to reset my pw using upper n lower case with number n special character . Minimum of 8 characters. I then press update and nothing happens . I then press again and it says the link is not recognised. I then get a email saying ow updated. I then go back to login and it says pw or email incorrect correct."

That message is dense: it signals high frustration ("third agent today"), gives a clear technical reproduction (reset link → looks updated → login still rejects), and identifies multiple specific symptoms (link reports "not recognised" → email says "pw updated" → login rejects). A human agent reads this as "the password reset flow is broken for this account; we need engineering / auth-state verification".

The bot's turn 0 response (at session_create, before the user message was even visible): "Hi Wasim! I'm having difficulty resolving this. Let me connect you with a specialist." On turn 1, after the full user description landed, the bot replied only "This conversation has been transferred to a human agent. Please wait for them to respond."

Outcome: escalate (correct); composite 0.8 PASS; L3 `relevance=2.0`, `tone_appropriateness=2.0`. Tool sequence: `search_knowledge` + 3× `resolve_article` + `request_handover`; `get_customer_context` was not called.

## What should a good CS agent have done?

cs_011 is structurally identical to cs_001 (turn-0 template-escalate on faq_miss) PLUS two amplifying signals the bot ignored:

- **High frustration evidence.** "Third agent today" + the chained "press update / nothing happens / link not recognised / email says updated / login still rejects" narrative is the canonical "I've already tried" signal. A good agent acknowledges this explicitly ("I can see you've already been through this multiple times today — let me get this to someone who can dig into the account directly") before handing off. Phase 2 §UC-FP-01 escalation policy avoids "I understand how you feel" but mandates *de-escalating* wording — the bot's "I'm having difficulty" template does neither acknowledgment nor de-escalation.

- **Detailed technical reproduction.** The user spelled out a reproducible password-reset failure. Even with no resolve-grade FAQ article (as the override confirms), the bot can restate the symptom back ("just to confirm — you reset using the email link, the email says it was updated, but login still rejects you — is that right?") so the user knows they were heard. The bot ignored every specific symptom.

A separate question is whether the bot should also have called `get_customer_context` first (per the CaseSpec's expected tool sequence). Phase 2 §2.10 line 358 currently restricts that tool to UC-A/UC-FP/UC-K, excluding UC-D — same conflict as cs_001 (see Related observation). The conflict does not change the L3 acknowledgment failure: the bot could and should have written a frustration-aware, specific-reproduction-aware acknowledgment regardless of which tools were available.

## Why does this matter?

cs_011 is the strongest evidence in the smoke set that the mechanical-template failure is **independent of user message richness and persona frustration level**. With cs_001 one might argue "the user only said 'I am unable to receive messages' so there wasn't much to acknowledge". With cs_011 the user gave a multi-symptom technical reproduction in their first message and the bot still produced the same generic template. There is no input-richness threshold under which the template flips to a specific acknowledgment — the template is fixed.

This matters for Constitution §1.2 ("LLM owns response strategy / natural customer-facing wording"): if the LLM produces the same surface regardless of input, the surface is being produced by a non-LLM source (prompt template / runtime template), not by an LLM responding to projection.

Additionally, cs_011's persona is configured with `will_request_human_if: bot cannot resolve after 2 attempts` — the user is *willing* to give the bot two attempts. The bot template-escalating on turn 0 proactively cuts off the user's give-bot-a-chance flow, which is exactly the kind of bot-side over-escalation `feedback_cs_agent_posture` (memory) and the Constitution §1.5 Iteration rule are meant to prevent.

cs_011 also surfaces a UC-D-specific containment concern: password-reset loops are a real account-integrity / fraud-adjacent shape. Repeatedly escalating these without any acknowledgment trains users that the bot is purely a transfer agent for account issues, weakening containment for the entire UC.

## Is this a one-off or a pattern?

`pattern`. Same surface as cs_001 / cs_002 / cs_014 / cs_038 / cs_040 / cs_015 (separate briefs). cs_011 is the "high-frustration + high-verbosity variant" — the most input-rich case in the cluster that still produced the standard template, ruling out "the bot adapts to richer input" as a possible cluster nuance.

## Which layer is likely responsible?

Same hypothesis as cs_001:

- **`prompt_projection`** (primary). The bot ran tool work but the projection at turn 0/1 did not produce a path for the LLM to encode the user's specific symptoms or frustration markers into the response. The 100% reliability of the template across all Cluster B cases — including the high-frustration high-verbosity cs_011 — is the tell. This is prompt-side, not LLM-decision-side variance.

- **`semantic_planner`** (secondary). Only if projection turns out to contain the user's full message and empty-FAQ narrative, and the LLM still defaults to the template.

`eval_spec` is **partially live** for the `get_customer_context` ↔ phase 2 UC-D restriction (see Related observation), same shape as cs_001 — now demonstrably a systematic generator issue, not a per-case quirk.

## What should NOT be done?

- Do **not** add a regex / keyword on "third agent today" / "I already tried" / "press update / not recognised" → frustration_acknowledgment template. That is the keyword chatbot regression Constitution §1.5 forbids.
- Do **not** add a Java guard requiring an empathy phrase before `request_handover` when the user message length exceeds N characters. Length-as-frustration is a brittle proxy and would trigger on irrelevant verbose messages (e.g. cs_036's polite UK consumer law explanation).
- Do **not** trigger `user_distress` for cs_011 specifically. The Sprint 4 §E1 runtime gate exists precisely to prevent this — `user_distress` requires a deterministic §B1 hit. Routing cs_011's frustration through `user_distress` would bypass §E1 and break the Tier-0 semantic-claim invariant.
- Do **not** edit cs_011's CaseSpec or the Sprint 4 §E1 override to make L3 quality scores pass. The override is approved Sprint-4-aligned ground truth; the L3 failure is the bot's failure, not the spec's.
- Do **not** "fix" `expected_tool_sequence` inline. Route the `get_customer_context` conflict through the L3 review process.

## Related observation — systematic `get_customer_context` ↔ phase 2 mismatch (broadened from cs_001)

cs_001 (UC-C) and cs_011 (UC-D) both have `expected_tool_sequence` starting with `get_customer_context`, but phase 2 §2.10 line 358 restricts the tool to UC-A/UC-FP/UC-K. UC-C and UC-D are both excluded. Two CaseSpecs showing the same mismatch across different UCs suggests the **CaseSpec generator includes `get_customer_context` as a default first tool regardless of phase 2 policy** — a systematic generator-vs-policy mismatch, not a per-case quirk.

This subsumes the cs_001-specific item proposed earlier. Track in `docs/action_bank.md` as **`R-generator-get-customer-context-policy-mismatch`** (covering cs_001 / cs_011 / any further smoke cases with the same conflict). Flag as a Wave A5/A6 review priority — multiple CaseSpecs likely need either an L3 override (drop `get_customer_context` from sequence) or a phase 2 policy widening (add UC-C / UC-D to the allowed list). The remediation path depends on the product / privacy intent behind the original phase 2 restriction.

> **Correction (Sprint 22, 2026-05-14):** the "systematic generator-vs-policy mismatch" hypothesis is invalidated. The normative cross-UC allowlist at `docs/foundational/phase2_domain_realization_spec.md` §2.10.1 line 1098 explicitly permits `get_customer_context` for UC-D (corroborated by `docs/customer_service_tool_spec_v0_2.yaml` line 260 and `docs/current/customer_service_tool_spec_v0_3.md` line 60). Line 358 is UC-H-local prose, not the cross-UC rule. The CaseSpec generator was correct. The behavioural question — does the bot use the tool when account-state matters on FAQ-miss? — is moved to `R-uc-cdf-get-customer-context-bot-actual-usage` (action_bank §5.2). `R-generator-get-customer-context-policy-mismatch` and `R-phase2-uc-cdf-customer-context-policy-widen` are both closed.

This is `eval_spec` (generator side) and/or `product_policy` (phase 2 side), independent of the bot-side L3-quality failure described above.
