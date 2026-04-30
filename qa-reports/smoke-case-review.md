# Smoke Case Semantic Review

Worker B review artifact for Wave A5 hybrid semantic QA. Scope was read-only for generated CaseSpec YAML and generator code; this report only records recommendations.

Inputs reviewed for each smoke case:
- generated spec under `eval_interactive/case_specs/smoke/`
- HR row from `data/human_review_annotations_2026-04-22_golden.csv`
- selected source turns from `data/eval_datasets/<source_dataset>_turns.csv`
- generation audit entry from `qa-reports/case-spec-generation-audit.md`

## Summary

| review_status | count |
| --- | ---: |
| ok | 12 |
| generator_bug | 0 |
| policy_ambiguity | 0 |
| needs_override | 2 |
| needs_human_decision | 0 |

Notable follow-up:
- No stale generator bug remains in the smoke review set. `cs_interactive_004` and `cs_interactive_095` now align with final generated YAML as resolve/no-escalation cases.
- `cs_interactive_015` was flipped from `ok` to `needs_override` in the Wave A6 semantic re-review: the transcript matches UC-FP ("why was my ad deleted?" + edit-and-repost), and the historical late escalation is failure-path evidence rather than the desired golden behavior. Override approved and applied in `case_spec_overrides.yaml`.

## Case Reviews

### cs_interactive_001

- Source: `badcase`, session `570Q5000008kr6LIAQ`
- Status: `ok`
- Recommended outcome: UC-C escalate, `clarification_budget_exhausted`
- Supporting turns: 12, 13, 18, 19
- Rationale: HR, transcript, YAML, and audit align. The user remains confused about two emails and the agent says further investigation is needed.
- Confidence: high

### cs_interactive_002

- Source: `badcase`, session `570Q5000008vyxJIAQ`
- Status: `ok`
- Recommended outcome: UC-C escalate, `user_distress`
- Supporting turns: 7, 9, 14, 19, 20
- Rationale: HR and YAML align. The transcript has explicit escalation and the user rejects the spam/marketing-preference workaround with visible frustration.
- Confidence: high

### cs_interactive_004

- Source: `badcase`, session `570Q50000090OefIAE`
- Status: `ok`
- Recommended outcome: UC-D resolve, no escalation trigger
- Supporting turns: 19, 20, 21, 22, 24
- Rationale: HR, selected transcript, audit, and final YAML align on a contained account/login issue: the ad is live under another account/email and the user is given sign-in guidance. The final YAML resolves with no handover.
- Confidence: high

### cs_interactive_011

- Source: `badcase`, session `570Q5000008NWIjIAO`
- Status: `ok`
- Recommended outcome: UC-D escalate, `user_requested`
- Supporting turns: 3, 5, 6, 23, 24, 25, 27
- Rationale: HR, transcript, YAML, and audit align. The user asks for phone support, has repeated unresolved login failures, and the agent routes them to the business team.
- Confidence: high

### cs_interactive_012

- Source: `badcase`, session `570Q5000008hx9tIAA`
- Status: `needs_override`
- Recommended outcome: UC-FP resolve, no escalation trigger
- Supporting turns: 5, 6, 8, 10, 12, 14
- Rationale: The selected transcript is about an ad repeatedly being deleted. The late phone/human request happens after the user has not received a concrete deletion reason, so it is treated as a failure-path signal rather than the desired first outcome. The reviewed target is resolve-first: retrieve safe account/listing/moderation context, explain the deletion reason with policy-grounded next steps, and escalate only if the bot cannot produce or the user rejects that explanation.
- Confidence: high

### cs_interactive_014

- Source: `badcase`, session `570Q5000008u9gjIAA`
- Status: `ok`
- Recommended outcome: UC-C escalate, `user_distress`
- Supporting turns: 6, 7, 9, 11, 15, 17, 19
- Rationale: HR, transcript, YAML, and audit align. The user reports messaging/contact-email issues, rejects the self-serve explanation, and expresses clear dissatisfaction.
- Confidence: high

### cs_interactive_015

- Source: `badcase`, session `570Q5000008WmXxIAK`
- Status: `needs_override` (applied)
- Recommended outcome: UC-FP resolve, no escalation trigger
- Supporting turns: 5, 6, 7, 8, 9, 11, 13
- Rationale: Wave A6 re-review supersedes the Wave A2.1 UC-K legacy pin. Phase 2 §2.2:282 places "why was my ad deleted?" plus reposting/editing under UC-FP; turns 5-7 show the human agent already giving the policy reasons (no "men only" wording, no body parts in images, ad title flagged), and the user's "how do I change it?" is the canonical UC-FP edit-and-repost ask. The late escalation in turn 11 is failure-path evidence (the human punted), not the desired golden behavior. Persona `frustration_level: mild` + empty `will_request_human_if` also fail to ground the previous `user_distress` escalation trigger. Mirrors cs_interactive_012's resolve-first override; applied via `case_spec_overrides.yaml`.
- Confidence: high

### cs_interactive_036

- Source: `intake_tool_contract`, session `570Q5000008hOYjIAM`
- Status: `ok`
- Recommended outcome: UC-I escalate, `payment_dispute_detected`
- Supporting turns: 4, 11, 12, 15, 19, 20, 22
- Rationale: HR, transcript, YAML, and audit align on a delivery refund/payment dispute requiring specialist handover.
- Confidence: high

### cs_interactive_038

- Source: `intake_tool_contract`, session `570Q5000008TPw5IAG`
- Status: `ok`
- Recommended outcome: UC-J escalate, `trust_safety_required`
- Supporting turns: 4, 6, 8, 12, 15, 20, 21
- Rationale: HR, transcript, YAML, and audit align on fraud/scam reporting requiring Trust & Safety intake and handover.
- Confidence: high

### cs_interactive_040

- Source: `intake_tool_contract`, session `570Q5000008pMbOIAU`
- Status: `ok`
- Recommended outcome: UC-K escalate, `intake_complete_for_uc_k`
- Supporting turns: 7, 8, 12, 13, 14, 17, 18
- Rationale: Although the HR row says resolve, the selected transcript shows a reproduced technical defect, case creation, escalation, and a case reference. The generated UC-K escalation matches the Wave A5 pin.
- Confidence: high

### cs_interactive_066

- Source: `badcase`, session `570Q5000008fBsXIAU`
- Status: `ok`
- Recommended outcome: UC-E escalate, `clarification_budget_exhausted`
- Supporting turns: 2, 3, 5, 7, 9, 10
- Rationale: HR and YAML align. The transcript involves a product/contact-option issue, missing intake context, and agent escalation to a specialist team.
- Confidence: high

### cs_interactive_095

- Source: `drift_control`, session `570Q5000008U5C9IAK`
- Status: `ok`
- Recommended outcome: UC-A resolve, no escalation trigger
- Supporting turns: 6, 7, 9, 10, 12, 13, 16, 17
- Rationale: The UC-B to UC-A reclassification is defensible for "wrong email, no adverts showing", and the selected transcript resolves the issue by identifying the correct account/email and advising sign-out/sign-in. The final YAML resolves with no handover.
- Confidence: high

### cs_interactive_192

- Source: `golden`, session `570Q5000008rbcjIAA`
- Status: `ok`
- Recommended outcome: UC-B resolve, no escalation trigger
- Supporting turns: 2, 3, 5, 7, 10, 12, 23, 26, 27
- Rationale: HR and YAML align on grounded posting guidance for free items. The transcript is resolved through answer guidance, account check, and photo requirement confirmation.
- Confidence: high

### cs_interactive_259

- Source: `golden`, session `570Q5000008OqaLIAS`
- Status: `ok`
- Recommended outcome: UC-F resolve, no escalation trigger
- Supporting turns: 4, 6, 8, 9
- Rationale: HR and YAML align on payments guidance. The transcript gives a grounded explanation of Gumtree delivery/payment availability and does not show a required escalation.
- Confidence: high
