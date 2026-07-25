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
| ok | 8 |
| generator_bug | 0 |
| policy_ambiguity | 0 |
| needs_override | 6 |
| needs_human_decision | 0 |

Smoke set under review (14 cases): cs_interactive_001, cs_interactive_002, cs_interactive_011, cs_interactive_014, cs_interactive_015, cs_interactive_029, cs_interactive_036, cs_interactive_038, cs_interactive_040, cs_interactive_066, cs_interactive_176, cs_interactive_190, cs_interactive_192, cs_interactive_259.

Sprint 071 / S-Auto-15 (B4) doc-sync: cs_interactive_095 left the smoke set and cs_interactive_190 joined it; cs_interactive_001's recommended outcome was brought in line with its approved `case_spec_overrides.yaml` entry (`faq_miss_threshold_exceeded`). Status counts updated accordingly (cs_001 ok -> needs_override).

Notable follow-up:
- `cs_interactive_004` has been removed from the smoke fixture set; its review row is removed here. `cs_interactive_176` joins the smoke set as the new UC-E escalate / `user_requested` handover anchor and is reviewed below.
- `cs_interactive_011` was flipped from `ok` to `needs_override` in the Sprint 4 §E1 supporting fix: the Sprint 4 §E1 runtime gate in `ControlKernel.applyEscalationReason` refuses to upgrade the canonical session reason to `user_distress` on the bot LLM's say-so unless the deterministic §B1 detector fired earlier in the session. cs_011's seed messages contain no DISTRESS_PATTERN match and no ALL-CAPS shout, so the truthful Phase 2 §2.4 reason is `faq_miss_threshold_exceeded` (the FAQ corpus has no useful article for the password-reset-loop the user reports). Override approved and applied in `case_spec_overrides.yaml` for `source_session_id 570Q5000008NWIjIAO`; supersedes the Codex 2026-05-03 round 3 §1.6 smoke YAML hand-edit (`user_distress`).
- `cs_interactive_015` was flipped from `ok` to `needs_override` in the Wave A6 semantic re-review: the transcript matches UC-FP ("why was my ad deleted?" + edit-and-repost), and the historical late escalation is failure-path evidence rather than the desired golden behavior. Override approved and applied in `case_spec_overrides.yaml`.
- `cs_interactive_014` was flipped from `ok` to `needs_override` in the Sprint 2.1 P1 follow-up: the transcript persona is frustrated, but the *replayed* cs_014 form context + seed_messages contain no Sprint-B1 DISTRESS_PATTERNS hit and no ALL-CAPS shout — so the deterministic resolver cannot stamp `user_distress`. The truthful escalation reason on this UC-C Replies/Messaging case is `faq_miss_threshold_exceeded` (no FAQ article covers the admin-mediated email-revert request). Override approved and applied in `case_spec_overrides.yaml` for `source_session_id 570Q5000008u9gjIAA`.
- `cs_interactive_029` was flipped from `ok` to `needs_override` in the Sprint 4 §E2 follow-up: the transcript shows an account-locked business user demanding a phone callback to the account manager — Phase 2 §2.2 places "account locked / can't advertise / business account access" issues under UC-D (Account & Login), not UC-C (Messages & Replies). The runtime deterministic UC fallback in `ControlKernel.inferFallbackUseCase` already picks UC-D for these account-locked seeds, so the spec override flips primary to UC-D / secondary to [UC-C] and closes the L2 `correct_uc` gap (D12) without changing runtime behaviour. Semantic escalation reason stays `user_requested` via the explicit-callback path (priority 1). Override approved and applied in `case_spec_overrides.yaml` for `source_session_id 570Q5000008kDiPIAU`.
- `cs_interactive_066` was flipped from `ok` to `needs_override` in the Sprint 4 §E3 formalization of Codex 2026-05-04 round 6 §P0: the form description ("Why am I not getting the option to add my phone number as a point of contact when listening an item any more") is an in-app technical regression, which Phase 2 §2.2:500 places under UC-K (Technical Support intake), not UC-E. `UseCaseRouter.matchUcKTechnicalRegression` deterministically routes this to UC-K, and the runtime escalates with the UC-K-specific `intake_complete_for_uc_k` reason once intake fields are collected. Override approved and applied in `case_spec_overrides.yaml` for `source_session_id 570Q5000008fBsXIAU`; the round 6 §P0 hand-edit was outside the override registry and is now superseded by this entry.

## Case Reviews

### cs_interactive_001

- Source: `badcase`, session `570Q5000008kr6LIAQ`
- Status: `needs_override` (applied)
- Recommended outcome: UC-C resolve, no escalation trigger
- Supporting turns: 12, 13, 18, 19
- Rationale: HR, transcript, YAML, and audit align that the user remains confused about two emails and the agent says further investigation is needed. The approved override in `case_spec_overrides.yaml` (`source_session_id 570Q5000008kr6LIAQ`) pins the escalation trigger to `faq_miss_threshold_exceeded`: the FAQ corpus has no useful article for the two-email confusion, so the truthful Phase 2 §2.4 reason is FAQ-miss rather than `clarification_budget_exhausted`. Sprint 071 / S-Auto-15 (B4) doc-sync: this recommended-outcome line is brought in line with the override-pipeline smoke YAML. **WS-2 (2026-07-25) doc-sync: the recommended outcome is revised from `UC-C escalate, faq_miss_threshold_exceeded` to `UC-C resolve, no escalation trigger`.** The "further investigation is needed" signal above is the historical *human agent's* wording, and the corpus is by construction sessions that reached a human, so it is not evidence about what the bot should do (replan rev 2 §1.1 A1). The ask — "which of my two account emails do I log in with so I receive messages?" — is a read-only lookup of the user's own account state plus an explanation, which product decision D1 (2026-07-25) places in current scope for the agent. The escalation trigger is cleared because `schema.Expected.__post_init__` and linter R4 both reject a trigger when `should_escalate` is false. The matching L3 override in `case_spec_overrides.yaml` was revised in the same change so a regeneration does not re-assert the escalate shape.
- Confidence: high

### cs_interactive_002

- Source: `badcase`, session `570Q5000008vyxJIAQ`
- Status: `ok`
- Recommended outcome: UC-C escalate, `user_distress`
- Supporting turns: 7, 9, 14, 19, 20
- Rationale: HR and YAML align. The transcript has explicit escalation and the user rejects the spam/marketing-preference workaround with visible frustration.
- Confidence: high

### cs_interactive_011

- Source: `badcase`, session `570Q5000008NWIjIAO`
- Status: `needs_override` (applied)
- Recommended outcome: UC-D resolve, no escalation trigger
- Supporting turns: 3, 5, 6, 23, 24, 25, 27
- Rationale: Sprint 4 §E1 supporting fix. cs_011's replayed seeds (long-form login-issue description in lower case + clarifying questions) contain no Sprint §B1 DISTRESS_PATTERN match and no ALL-CAPS shout, so `EscalationReasonResolver.detectDistressSignal` cannot deterministically stamp `user_distress`. The Sprint 4 §E1 runtime gate in `ControlKernel.applyEscalationReason` refuses to let the bot LLM upgrade the canonical session reason to a Tier-0 semantic claim (`user_distress`) without a prior B1 hit, downgrading the candidate to `faq_miss_threshold_exceeded`. The truthful Phase 2 §2.4 reason is FAQ-miss: the FAQ corpus has no useful article for the password-reset-loop, so the bot exhausts FAQ search and hands over. Override approved and applied in `case_spec_overrides.yaml`; supersedes the prior Codex 2026-05-03 round 3 §1.6 smoke YAML hand-edit (`user_distress`). **WS-2 (2026-07-25) doc-sync: the recommended outcome is revised from `UC-D escalate, faq_miss_threshold_exceeded` to `UC-D resolve, no escalation trigger`.** The Sprint 4 §E1 reasoning above remains correct about *which reason* is truthful **if** the bot hands over — it was never evidence that a handover is the right outcome. The password-reset loop is self-serve resolvable (the paired bad case `cs011_uc_c_faq_miss_not_distress`'s own `hidden_facts` place the reset mail in the user's spam / promotions folder with the platform-side address correct), and nothing in it asks the platform to modify data, so product decision D1 / the 2026-07-25 product principle puts it in the agent's scope. The escalation trigger is cleared because `schema.Expected.__post_init__` and linter R4 both reject a trigger when `should_escalate` is false. The matching L3 override in `case_spec_overrides.yaml` was revised in the same change so a regeneration does not re-assert the escalate shape.
- Confidence: high

### cs_interactive_014

- Source: `badcase`, session `570Q5000008u9gjIAA`
- Status: `needs_override` (applied)
- Recommended outcome: UC-C escalate, `faq_miss_threshold_exceeded`
- Supporting turns: 6, 10, 12, 14, 16
- Rationale: Sprint 2.1 P1 follow-up override. The replayed cs_014 form description + both seed messages are calm/cooperative ("How come ...?", "I'd be happy enough if ..."); they contain no Sprint-B1 DISTRESS_PATTERNS hit and no ALL-CAPS shout, so the deterministic resolver cannot stamp `user_distress` on this case. The selected agent turns (6, 10, 12, 14, 16) confirm Gumtree has disabled the admin-mediated contact-email-revert path — no FAQ article covers it — so the truthful escalation reason is `faq_miss_threshold_exceeded`. UC-C primary still holds via the Replies-or-Messaging strong-prior + the B2 messaging bias. Pinned by `Cs014RouteAndDistressRegressionTest` (helper) and `Cs014RouteAndLoopHandoverIntegrationTest` (case-level integration). Override approved and applied in `case_spec_overrides.yaml`.
- Confidence: high

### cs_interactive_015

- Source: `badcase`, session `570Q5000008WmXxIAK`
- Status: `needs_override` (applied)
- Recommended outcome: UC-FP resolve, no escalation trigger
- Supporting turns: 5, 6, 7, 8, 9, 11, 13
- Rationale: Wave A6 re-review supersedes the Wave A2.1 UC-K legacy pin. Phase 2 §2.2:282 places "why was my ad deleted?" plus reposting/editing under UC-FP; turns 5-7 show the human agent already giving the policy reasons (no "men only" wording, no body parts in images, ad title flagged), and the user's "how do I change it?" is the canonical UC-FP edit-and-repost ask. The late escalation in turn 11 is failure-path evidence (the human punted), not the desired golden behavior. Persona `frustration_level: mild` + empty `will_request_human_if` also fail to ground the previous `user_distress` escalation trigger. Mirrors cs_interactive_012's resolve-first override; applied via `case_spec_overrides.yaml`.
- Confidence: high

### cs_interactive_029

- Source: `badcase`, session `570Q5000008kDiPIAU`
- Status: `needs_override` (applied)
- Recommended outcome: UC-D escalate, `user_requested`
- Supporting turns: 3, 4, 6, 10, 43, 47
- Rationale: Sprint 4 §E2 follow-up override. The user's actual issue is an account-locked business user demanding a phone callback so they can resume advertising — Phase 2 §2.2 places "account locked / can't advertise / business account access" issues under UC-D (Account & Login), not UC-C (Messages & Replies). Turns 3–6 ("HI MY ACCOUNT OS", "IS LOCKED", "I NEED MY ACCOUNT MANNAGER TO CALL ME"), turn 10 (agent referral to the Business for Gumtree contact form), and turns 43 / 47 (human agent escalates with a case number to the business team) all align with UC-D. The runtime deterministic UC fallback in `ControlKernel.inferFallbackUseCase` already picks UC-D for these account-locked seeds, so the spec override closes the L2 `correct_uc` gap (D12) without changing runtime behaviour. Semantic escalation reason stays `user_requested` via the explicit-callback path (priority 1). Override approved and applied in `case_spec_overrides.yaml`.
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
- Status: `needs_override` (applied)
- Recommended outcome: UC-K escalate, `intake_complete_for_uc_k`
- Supporting turns: 2, 3, 5, 7, 9, 10
- Rationale: Codex 2026-05-04 round 6 §P0 reclassification, formalized through the Wave A6.6 v2 override path during Sprint 4 §E3. The form description ("Why am I not getting the option to add my phone number as a point of contact when listening an item any more") is an in-app technical regression — a contact option that used to be available has disappeared. Phase 2 §2.2:500 places this under UC-K (Technical Support intake), not UC-E (product/feature explanation FAQ). `UseCaseRouter.matchUcKTechnicalRegression` deterministically routes this to UC-K, and the runtime escalates with the UC-K-specific `intake_complete_for_uc_k` reason once intake fields are collected. Pinned by `Cs014RouteAndDistressRegressionTest` and `ClassifyUseCaseToolStrongPriorTest`. Override approved and applied in `case_spec_overrides.yaml`; the round 6 §P0 smoke-only hand-edit was outside the override registry and is now superseded by this entry.
- Confidence: high

### cs_interactive_176

- Source: `handover`, session `570Q5000008NMRRIA4`
- Status: `ok`
- Recommended outcome: UC-E escalate, `user_requested`
- Supporting turns: 2, 3, 5, 7, 10
- Rationale: HR, selected transcript, YAML, and audit align on a paid-listing complaint that escalates because the user explicitly asks for phone support. The form description ("I pad to put my ad for my business and it's not at the top") is a paid-promotion complaint that fits UC-E (FAQ/explanation), and the seed messages ("Give me my £50 back or put my business to the top like I paid for", "What about giving a phone number to talk to someone") trigger the explicit-callback path that resolves to `user_requested`. The audit's `evidence signal counts.user_requested_human=True` corroborates the UC-E escalate / `user_requested` outcome the YAML pins. UC-K appears in `secondary_ucs` because the underlying complaint also has a paid-promotion-defect reading, but the runtime explicit-handover path takes precedence.
- Confidence: high

### cs_interactive_190

- Source: `golden`, session `570Q5000009060DIAQ`
- Status: `ok`
- Recommended outcome: UC-A resolve, no escalation trigger
- Supporting turns: -
- Rationale: Sprint 071 / S-Auto-15 (B4) doc-sync: cs_interactive_190 joined the smoke fixture set (replacing cs_interactive_095 as the UC-A resolve anchor). The golden source session resolves an account/advert-visibility question with grounded guidance and no handover; the smoke YAML pins UC-A resolve with no escalation trigger. Reviewed against the smoke YAML's expected block.
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
