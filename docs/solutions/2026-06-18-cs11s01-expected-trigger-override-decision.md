---
title: "cs11s01_uc_d expected-trigger — override-pipeline decision record (read-only)"
doc_tier: proposal
status: proposal
implementation_status: not_started
source_of_truth: this file
last_reviewed: 2026-06-18
review_cadence: ad hoc
supersedes: []
superseded_by: null
notes: >
  READ-ONLY DECISION RECORD. No CaseSpec, override registry, or gate code is
  changed by this doc. It evaluates three options for the internally-inconsistent
  expected escalation trigger on cs11s01_uc_d_two_emails_one_account and
  recommends one on product-contract grounds, for the override-review pipeline
  (human decision #2: cs11s01 MUST go through override review, not a direct
  CaseSpec edit). Evidence: docs/sprints/sprint-088-handoff.md "Expected-trigger
  verification" (commit 56686db0). Companion to the gate-reclassification plan
  (2026-06-18-escalation-compliance-gate-reclassification-subsprint-plan.md).
---

# cs11s01_uc_d expected-trigger — override decision record

## 1. The inconsistency (evidence)

`cs11s01_uc_d_two_emails_one_account` (shadow; `source_session_id:
synthetic-sprint20-cs011-shadow-01`; `source_dataset: case_family_authored`; **no
existing L3 override** → the authored value is unreviewed) sets:

- `persona.will_request_human_if: "bot cannot resolve after 2 attempts"`
  → drives the **priority-1 `user_requested`** escalation path; and
- `expected.escalation_trigger: faq_miss_threshold_exceeded`.

These two are in tension. Per the contract chain:

- **Foundational Escalation Matrix** (`phase2_domain_realization_spec.md §2.4`):
  `user_requests_human → user_requested` applies to **all UCs, immediately**;
  `faq_miss_ge_2 → faq_miss_threshold_exceeded` applies to `allow_bot_resolution=
  true` UCs **including UC-D**. **Both are contract-valid** for this case.
- **Runtime skill policy** (`resolve_faq_grounded_answer.yaml` escalation_policy):
  user-requests-human → `user_requested` is **priority 1**, ahead of faq_miss.

So when the bot honours the user's request for a human (which the persona
actively triggers), it correctly stamps `user_requested` — yet the CaseSpec scores
that a cross-family FAIL against `faq_miss_threshold_exceeded`. Baseline draws are
heavily `user_requested`, confirming the bot follows the priority-1 path.
**Conclusion: the expected trigger is internally inconsistent with the persona; it
is an `eval_spec` defect, not a bot defect** (§5.4: fix the CaseSpec, not the bot).

> Scope note: contrast `cs40s02_uc_k` — `will_request_human_if: ''`,
> `search_knowledge` forbidden, baseline 11/11 on `intake_complete_for_uc_k`.
> That spec is correct and consistent and needs **no** change (human decision #3).

## 2. Options

### Option A — accept the set `{faq_miss_threshold_exceeded, user_requested}`
Make the expected trigger multi-valued (an approved accepted-set override); the
reason-family check passes if the bot stamps **either**.

- **Contract fit:** strongest. Both reasons are sanctioned by §2.4, and the
  scenario genuinely admits both depending on flow timing (FAQ-miss before the
  user asks vs the user asking first). The priority-1 rule says `user_requested`
  wins *when the user asks*, but `faq_miss_threshold_exceeded` remains correct on
  the resolve-then-miss path.
- **Pros:** preserves the realistic dual-path persona; does not bend the scenario;
  scores every contract-correct bot behaviour as pass.
- **Cons:** requires the override / check to support an accepted **set** (small
  schema affordance). Slightly looser specificity — acceptable here because both
  members are genuinely valid (it is not masking a wrong behaviour).

### Option B — change expected trigger to `user_requested`
Single-value it to the priority-1 reason.

- **Contract fit:** partial. Aligns with priority-1 + observed baseline, but
  **drops the equally-valid faq_miss path**: if the bot exhausts FAQ resolution
  *before* the user explicitly asks, `faq_miss_threshold_exceeded` is the §2.4
  correct reason and Option B would now score it FAIL — the same
  over-specification flaw, mirrored.
- **Pros:** simplest single value; matches the most common observed draw.
- **Cons:** re-introduces a single-family hard expectation on a genuinely
  dual-path scenario; penalises the FAQ-miss-first path.

### Option C — delete / modify `will_request_human_if`
Remove the user-request trigger so the scenario is single-path (faq_miss only),
restoring consistency with the authored `expected_trigger`.

- **Contract fit:** weakest. It bends the **scenario** (persona behaviour) to fit
  an inconvenient-to-score expectation rather than reflecting realistic user
  behaviour. A high-frustration "two-emails / can't log in" user realistically
  *would* ask for a human; removing that reduces coverage of the priority-1
  user-request path and edges toward "shape the eval to the bot" (§5.4 smell,
  applied to scenario design).
- **Pros:** keeps a single expected reason; no set-support needed.
- **Cons:** least faithful to real user behaviour; loses test coverage of the
  priority-1 path; a behavioural edit to dodge a scoring artefact.

## 3. Recommendation — **Option A**, via an approved accepted-set override

Option A is the only choice faithful to the full product contract: §2.4 sanctions
both reasons, the priority-1 rule orders them without invalidating faq_miss, and
the persona's dual-path design is intentional and realistic. A and B both require
touching the expectation; A keeps the scenario honest, B amputates a valid path.
C edits behaviour to suit scoring and is not recommended.

**Interaction with the gate sprint (important).** This override is **not** a
re-elevation to tier-0. Under the companion gate-reclassification sprint
(S-Auto-38 proposed), the Part-2 reason-family match is demoted out of
zero-tolerance tier-0; so cs11s01 stops flipping keep/discard **regardless** of
this override. Option A's value is to make the *demoted* Part-2 signal (observation
or noise-aware tier-1) **correct** — i.e. record cs11s01 as compliant when the bot
stamps either valid reason — so the signal is not a standing false-positive. The
accepted-set override is the multi-valid counterpart of §3.4's unique-reason
opt-in: both keep reason bindings **explicit + human-reviewed**, never implicit.

**Framing (per human decision #4).** This override does **not** affect exp-82.
exp-82 remains withdrawn on its n=13 primary results; cs11s01 is a held-out shadow
case, not a primary target.

## 4. Draft override entry (NOT applied — for review only)

Proposed `case_spec_overrides.yaml` (schema v2) entry for the reviewer to approve
through the pipeline. Recorded here only; **do not write it to the registry until
approved**, and only alongside the check/affordance that consumes an accepted-set.

```yaml
  - source_session_id: synthetic-sprint20-cs011-shadow-01
    case_id_hint: cs11s01_uc_d_two_emails_one_account
    status: pending_review        # -> approved only after human review
    source: OQ-E expected-trigger verification (sprint-088 handoff, 2026-06-18)
    reviewer: <human semantic review — TBD>
    date: <TBD>
    confidence: high
    supporting_turn_numbers: []   # synthetic/case_family_authored; cite persona instead
    rationale: >-
      Persona will_request_human_if drives the priority-1 user_requested path
      (resolve_faq escalation_policy), while the FAQ-resolution-miss path yields
      faq_miss_threshold_exceeded (phase2 §2.4, UC-D allow_bot_resolution=true).
      Both are contract-valid for this dual-path scenario; accept either.
    expected:
      escalation_trigger: [faq_miss_threshold_exceeded, user_requested]   # accepted SET
```

> Affordance note: the registry/`expected.escalation_trigger` field and the
> `escalation_reason_family_match` check currently assume a single value. Accepting
> a set is a small affordance to be designed **inside** the gate sprint (or a
> parallel eval_spec sprint), under §5.4 review. Until then this entry stays
> `pending_review` and unapplied.

## 5. Open questions

- **OQ-1:** confirm Option A over B with the product owner — is the FAQ-miss-first
  path in-scope for this case, or is the case intended purely as a user-request
  test? (If purely user-request, Option B becomes defensible.)
- **OQ-2:** schema for an accepted-set trigger (list vs a new `accepted_triggers`
  field) — decide with the gate sprint's §3.4 override affordance so there is one
  mechanism, not two.
- **OQ-3:** sweep — are there sibling `cs011_uc_d_*` / other `will_request_human_if`
  cases with the same single-value-vs-dual-path inconsistency? A short read-only
  audit before approving avoids a one-off patch.
