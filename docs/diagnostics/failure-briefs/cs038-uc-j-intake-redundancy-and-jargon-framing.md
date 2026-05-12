# Failure Brief — cs038 UC-J intake redundancy and internal-jargon framing

> **Source case:** `cs_interactive_038` (UC-J primary, no secondary, expected `escalate`, trigger=`trust_safety_required`).
> **Source session:** `570Q5000008TPw5IAG`.
> **CaseSpec:** `eval_interactive/case_specs/smoke/cs_interactive_038.yaml`.
> **Approved L3 override:** NONE (greped `case_spec_overrides.yaml`; no entry for this `source_session_id`).
> **Runs:** `20260505-235231` (outcome-PASS, composite=0.7583, L2:tool_sequence_match + L3:relevance/tone=2.0). 2026-05-10 regressed to FAIL with `turn_budget_exhausted` — Cluster C, deferred via `R-smoke-regression-investigation`.
> **Filed:** 2026-05-12 (Sprint 18 / G1, brief #4).

## Ground-truth chain

cs_038 has no approved L3 override. The CaseSpec is rule-extracted L1 + L2 LLM persona review.

CaseSpec ground truth: UC-J (Trust & Safety / Fraud), `allow_bot_resolution: false`, escalation_trigger `trust_safety_required`, intake fields `[report_target, report_type, description]`, expected tool sequence `create_case_controlled → request_handover → record_outcome` (short — UC-J is `grounding_mode: fixed_script_only`, broad `forbidden_tools` including `get_customer_context` / `search_knowledge` / `resolve_article`). The bot's job is intake + controlled case + handover, NOT resolve.

The 2026-05-05 run outcome matches expected (escalate with `trust_safety_required`, `case_id_present`, correct UC), so the bot reached the right destination. The failure is on the journey: framing, intake state-tracking, redundancy. Tool sequence scored 0.333 (`request_handover` + `create_case_controlled` reached, but order / `record_outcome` partial). L3 relevance=2.0 and tone=2.0.

The 2026-05-10 run regression (`turn_budget_exhausted`) is Cluster C / deferred and not this brief's scope.

## What happened?

User entered the chat via the "Report a Safety Issue" form: `form_context.first_name="Paul"`, description "Parkside impact wrench  and more item", empty ad_id, persona `frustration_level: mild`. The 4-turn transcript:

- **T0 user** (form): "Parkside impact wrench  and more item"
- **T0 bot:** "Hi Paul! I'm here to help with your inquiry about trust & safety report. Let me look into this for you."
- **T1 user:** "There a scamer on gumtree"
- **T1 bot:** "Hi Paul, I'm sorry to hear you've encountered a scammer on Gumtree. To help our Trust & Safety team investigate, could you tell me what the scammer is targeting — for example, is it a specific ad, a user profile, or a message you received?"
- **T2 user:** "Yes — the scammer is 'Diane', selling a Parkside impact wrench and other items. I paid £40 but never received anything. My bank confirmed it's fraud, and there's even a Facebook warning about her. I've already emailed details and submitted the 'Report a Safety Issue' form."
- **T2 bot:** "Thank you for those details, Paul. I can see you've already reported this through other channels — that's helpful. To complete your Trust & Safety report, could you briefly describe what happened? For example, how you contacted the seller, how you paid, and when you realised it was a scam."
- **T3 user:** "I paid £40 via bank transfer after messaging Diane through Gumtree — no goods arrived, and my bank confirmed it's fraud. I have screenshots of the chat, the ad, and payment proof. Happy to share if needed."
- **T3 bot:** "Let me connect you with a specialist."

The bot reached escalation with `case_id_present` and `trust_safety_required`. Two L3 quality failures stand out:

1. **Internal-jargon framing.** T0 opens with "your inquiry about trust & safety report" — an awkward phrasing that echoes the internal UC label. The user does not perceive their problem as a "trust & safety report"; they perceive it as "I got scammed." A natural opener would be e.g. "Hi Paul, I can help with what happened with your purchase — can you tell me a bit more about the listing and the seller?"

2. **Intake redundancy.** On T2, after the user provided the scammer name (Diane), the item, the amount (£40), the payment outcome (no item), the bank confirmation (fraud), and cross-channel reporting history, the bot asked again for "how you contacted the seller, how you paid, and when you realised it was a scam" — questions already substantively answered in T2's narrative. The user dutifully restated the same facts in T3. This is intake state-tracking failure: the bot did not recognize that the three required intake fields (`report_target` = seller "Diane" + Parkside ad; `report_type` = fraud / non-delivery; `description` = user's T2 narrative) were already provided.

## What should a good CS agent have done?

- **Open with user-facing language, not internal labels.** "Your inquiry about trust & safety report" should be "what happened with your purchase" or "what you'd like to report". The internal UC name is not user-facing.

- **Recognize intake completion from the narrative.** By the end of T2, all three required intake fields are present in the user's words. The bot should have closed intake at T2 — restated what it captured ("got it: you're reporting Diane for a fraudulent listing of a Parkside impact wrench; £40 paid via bank transfer, never delivered, bank confirmed fraud") and handed off immediately.

- **Trust `form_context.first_name` without re-confirmation.** Using "Paul" from form_context is *correct*; the L3 judge's criticism on this point (see Related observation) is overreach.

## Why does this matter?

UC-J (Trust & Safety / Fraud) is a high-stakes UC: users contacting via "Report a Safety Issue" are typically already harmed (financial loss, fraud, harassment). Two failures compound the user's frustration:

- Internal-jargon framing reads as bureaucratic — the opposite of empathetic.
- Redundant intake asks them to relive the bad experience to provide details they've already volunteered.

Both fail Constitution §1.2 (LLM owns natural customer-facing wording) and §1.3 (LLM owns next action — the bot should detect intake completion semantically, not by counting field-by-field).

cs_038 also reveals a **different Cluster B sub-pattern** from cs_001 / cs_011: the bot DID engage (4 turns, multi-step tool use, correct final outcome) but engaged the WRONG way — internal framing + redundant intake. This is **"engaged-but-mechanical"** vs the **"disengaged-template-escalate"** pattern of cs_001 / cs_011. Both are mechanical surfaces, but they fail differently and likely sit in different layers of the system.

## Is this a one-off or a pattern?

`pattern`. The internal-jargon framing + intake-redundancy combination is shared with cs_040 (UC-K, "technical issue intake" framing for "my ad isn't allowing customers to send requests") and cs_066 (UC-K, similar). cs_038 is the highest-stakes (UC-J / fraud) example in the cluster.

## Which layer is likely responsible?

Two candidate layers, both likely live simultaneously (addressing different L3 dimensions):

- **`prompt_projection`** (primary for jargon framing). The prompt likely surfaces UC labels (e.g. "trust & safety report intake", "technical issue intake") directly to the LLM as the framing the bot should echo. The 100% consistency of this jargon across cs_038 / cs_040 / cs_066 strongly suggests prompt-side, not LLM-decision-side. The fix would be a projection that distinguishes "what UC the bot is operating under" (internal) from "how the bot should phrase the topic to the user" (user-facing).

- **`skill_state`** (primary for intake redundancy). The bot's UC-J intake skill state did not recognize that all three required intake fields were semantically satisfied in T2's narrative. The bot kept asking for fields field-by-field rather than detecting them in the user's full narrative. §3.2 Q4 (multi-tool / multi-turn flow losing state, here "intake completion detection") applies.

`eval_spec` is partially live for the judge_calibration issue (see Related observation), independent of the bot-side failures.

## What should NOT be done?

- Do **not** rename the UC-J label internally (e.g. UC-J → "scam_report") to avoid the jargon framing. That is a layering violation — internal UC labels exist for routing / policy; they should not leak to user-facing wording regardless of how they're named.
- Do **not** add a keyword rule mapping "scam" / "fraud" / "scamer" to a UC-J-specific friendly opener. That is the keyword chatbot regression Constitution §1.5 forbids. The right surface is the prompt-side projection rule "do not echo internal UC labels to the user".
- Do **not** add a Java guard counting how many user messages have happened before allowing handover, to force fewer intake turns. The right surface for "is intake complete" is semantic detection on the projected narrative, not a turn counter.
- Do **not** edit cs_038's CaseSpec to lower the L3 bar. Outcome is already passing; the failure is on relevance / tone, which is where it should be surfaced.
- Do **not** treat the L3 judge's "name without confirmation" criticism as a bot failure to remediate. The bot followed `form_context.first_name` correctly; the judge is mis-calibrated on this point (see Related observation).

## Related observation — two separate eval-side concerns

1. **L3 judge over-reaches on form_context trust.** The judge reasoning includes "incorrectly assumes the user's name is 'Paul' without confirmation". But cs_038's `form_context.first_name="Paul"` — the bot correctly read the form-supplied name. The judge is over-strict: requiring confirmation of a value the user already supplied via the form is bureaucratic, not customer-friendly. The L3 rubric should account for form-supplied context (name, email, ad_id, topic_subject) as a usable signal that does not require re-confirmation. Track in `docs/action_bank.md` as `R-l3-judge-form-context-trust-rubric` (judge_calibration / eval_spec).

2. **cs_038 CaseSpec would benefit from a Wave A5/A6 review.** Specifically: is intake completion at T2 the correct `turn_efficiency` target? Should `expected_tool_sequence` include intake-state-checkpoint tools, or is intake state implicit? Track as `R-cs038-l3-review-intake-efficiency`.

Both are independent of the bot-side L3-quality failures described above (jargon framing + intake redundancy), which remain real even if these eval-side nuances are corrected.
