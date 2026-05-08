# Runtime Freeze and Risk Policy

Date: 2026-05-09
Sprint: Sprint 13 — Runtime Freeze, Risk Policy, and Eval Guardrails
Branch: `design-v1-without-human-review`

## 0. Purpose

Sprint 13 freezes the runtime main flow that converged across Sprints 10
/ 11 / 11.1 / 12 and shifts the next iteration layer to **risk
policy**, **prompt behaviour**, and **eval guardrails**. This document:

1. Records the runtime freeze decision (what is frozen, what can still
   be tuned, what requires a future runtime sprint).
2. Defines the canonical risk taxonomy (Level 1 / 2 / 3) and the term
   distinctions Sprint 13 needs (`risk_flag`, `escalation_trigger`,
   `handover_reason`, `intake_reason`).
3. Walks the five canonical examples (paid Top Ad / money back / delete
   account / scammed / want a human) end-to-end through the taxonomy.
4. Captures the exact, narrow prompt change proposal Sprint 13 chose to
   defer to a docs-only deliverable, with the rationale for keeping
   the runtime + system prompt unchanged this sprint.

This file is normative for Sprint 13 review and for the Sprint 13 §O2
eval guardrails. Pair it with `docs/sprints/sprint-013-handoff.md` (on
closure) and the focused regression suite at
`server/src/test/java/com/gumtree/csagent/service/runtime/Sprint13RiskPolicyGuardrailsTest.java`.

---

## 1. Runtime freeze decision

The runtime main flow is **frozen** as of Sprint 12 closure. Sprint 13
must not alter the state machine, the pre-plan reroute decision matrix,
the same-UC progressive-resolve disposition evaluator, the
`record_outcome` guard, or the projection / trace fields the kernel
emits. The reasoning:

- Sprints 10 / 11 / 11.1 closed the runtime alignment workstream
  (cross-UC reroute + same-UC progressive resolve + terminal-evidence
  closure). Sprint 12 hardened the trace evidence so a reviewer can
  audit any one turn end-to-end.
- All hard invariants are green
  (`L1:escalation_reason_consistency = 0`,
  `CONTRACT_VIOLATION:active_use_case = 0`).
- No P0/P1 runtime blocker is open. The remaining residuals are
  governance-class (`judge_volatility`, `faq_corpus_gap`,
  `product_policy_gap`) per Sprint 12 §N2.
- Touching the kernel / phase evaluator / classifier / decider here
  would risk regressing one of those green invariants without any
  failing scenario forcing the change.

### 1.1 What is frozen

The following surfaces are **frozen** in Sprint 13 and must not be
edited by O0 / O1 / O2:

1. **Pre-plan reroute flow.** Sprint 10 §L0 / §L1 — the
   `RuntimeIntentClassifier → RerouteDecider → applyRerouteDecision`
   pipeline that runs BEFORE `PhaseEvaluator.plan(...)`. The Sprint 10
   MVP shapes (UC-A / UC-C soft-shift, UC-J risk-shift, UC-A
   same-issue / same-UC follow-up, payment-ambiguity negative guard,
   DriftDetector hard-shift fallback) are normative. Adding a new
   regex shape, changing the high-risk vs low-risk UC partition, or
   re-classifying any of the existing shapes is out of scope.
2. **Same-UC progressive resolve flow.** Sprint 11 §M0 / §M1 / §M2 —
   the `BotSession.taskStatus / lastEntityContextRef` fields, the
   ad_id same-UC capture, `ResolveDisposition`, and the post-loop
   `task_status` projection. This includes the soft next-step pattern
   in `ResolveDispositionEvaluator`.
3. **`ResolveDisposition` terminal-evidence guard.** Sprint 11.1 — the
   `FINAL_ANSWER` branch must continue to require deterministic
   terminal evidence (a successful `record_outcome` dispatch on the
   same run) before returning `READY_TO_CONFIRM`; non-slot,
   non-clarifying answers must continue to default to
   `ANSWERED_SUBTASK`.
4. **`record_outcome` guard.** Sprint 11 §M1 — `AgentRunLoopImpl`
   step `6a''` continues to refuse `record_outcome(outcome_class=resolve)`
   on a RESOLVE / FAQ plan when the session is not in CONFIRM / CLOSE.
5. **Observability and validation layer.** Sprint 12 §N0 — eight
   `BotSession` `@Transient` slots (`predictedUseCase`,
   `intentRelation`, `rerouteAction`, `phaseTransitionReason`,
   `resolveDisposition`, `recordOutcomeAttempted`,
   `recordOutcomeSucceeded`, `recordOutcomeGuardResult`); the
   alias-key enrichment on `REROUTE_DECISION`; the `RESOLVE_DISPOSITION`
   and `RECORD_OUTCOME_GUARD` events; the `drift_history` and
   `task_history` projection aggregates; the §N1
   `Sprint12RuntimeAlignmentValidationTest` 13 / 0 deterministic
   regression suite.

These surfaces ALL remain green at Sprint 13 close. Sprint 13 introduces
no new fields, no new events, and no new branches in any of the above
files.

### 1.2 What can still be tuned

The following remain tunable WITHIN Sprint 13:

1. **Prompt wording.** Narrow, additive instructions in
   `server/src/main/resources/prompts/system_prompt.txt` are
   in-scope IF (a) they do not rewrite the routing taxonomy / escalation
   reason enum, (b) they keep an explicit human request as
   `user_requested`, and (c) they ship with focused golden prompt
   tests. Sprint 13 explicitly chooses **not** to land such an edit
   (see §4 below for the exact proposal and deferral rationale).
2. **Reroute policy thresholds.** Eval-side thresholds, anchor
   thresholds, and per-judge thresholds are docs-tunable. Hard-gate
   promotion is NOT in-scope (see Do-Not-Implement list).
3. **Escalation policy wording.** Wording-only updates inside the
   risk-policy doc and the prompt's escalation-reason decision tree
   are in-scope. The 23-value `escalation_reason` enum is FROZEN; do
   not add or rename a reason.
4. **Eval cases.** New deterministic Java tests on top of the Sprint 13
   risk-policy taxonomy are in-scope. New live CaseSpecs are NOT
   in-scope unless explicitly reviewed and kept out of any hard gate.
5. **Risk taxonomy.** This document IS the risk taxonomy. Future
   sprints may extend it; Sprint 13 owns the first version.

### 1.3 What requires a future runtime sprint

The following are **explicitly deferred** beyond Sprint 13. Each one
requires a new objective doc, scoped acceptance criteria, and a new
runtime sprint:

1. **Full Issue Ledger.** Per-issue ledger, `issues[]` array on the
   session, per-issue lifecycle. Sprint 11 carved this out; not
   reopened.
2. **Per-issue budgets.** Per-issue clarification budget, per-issue
   FAQ-miss budget, per-issue stall counters. Out of Sprint 11/12/13
   scope.
3. **All-UC task taxonomy.** A canonical `task_type` registry across
   every UC. Sprint 11 only pinned the UC-A subset
   (`listing_visibility_diagnostic`, `listing_lifecycle_followup`,
   `listing_visibility_paid_promotion`).
4. **Handover payload redesign.** A new `HandoverContext` /
   `HandoverPayload` schema. Out of Sprint 9 / 10 / 11 / 12 / 13 scope.
5. **New escalation reason enum.** Adding, removing, or renaming any
   value in the canonical 23-value enum (e.g. a dedicated
   `risk_observed_continue` reason). Cross-cuts the eval-side
   `ESCALATION_TRIGGER_VALUES` set; needs a coordinated migration.

If a real-traffic case opens any of the above as a P0/P1 runtime
blocker, that is the trigger to start a new runtime sprint. Sprint 13
does NOT pre-empt that decision.

---

## 2. Frozen runtime contract (recap)

The seven-rule runtime contract that Sprint 13 must preserve:

1. **Explicit human request always wins.** The user typing "I want a
   human" / "talk to an agent" / "call me back" sets
   `escalation_reason=user_requested` via the
   `EscalationReasonResolver` + kernel step 2.5 path BEFORE the
   planner runs. Priority 1 in the resolver; never overwritten.
2. **Critical policy / safety / GDPR execution requests can
   escalate early.** Account deletion / GDPR execution (UC-G), scam
   / fraud / safety reports (UC-J), appeal / restoration decisions
   (UC-H), real payment disputes (UC-I), identity verification
   (`identity_verification_required`), imminent harm
   (`imminent_harm`), and trust/safety required
   (`trust_safety_required`) flows are intake or escalation paths,
   not FAQ resolution.
3. **Other risk signals become `risk_flag`s, not automatic
   handovers.** A money-back keyword on its own, a paid-Top-Ad
   visibility complaint on its own, or a generic frustration phrase
   on its own may be observed as a risk flag without immediately
   firing `request_handover` or stamping a Tier-2
   `escalation_reason`.
4. **Same-UC follow-up stays in or returns to RESOLVE.** Sprint 11
   §M0 / §M1 — a UC-A user following up on the same listing
   (slot, follow-up, status) continues in RESOLVE; CONFIRM rebounds
   to RESOLVE on `SAME_ISSUE` / `SAME_UC_NEW_TASK`.
5. **Cross-UC soft shift reroutes before phase planning.** Sprint 10
   §L1 — `applyRerouteDecision` runs BEFORE `PhaseEvaluator.plan(...)`
   so the bot enters the new UC's plan instead of generic FAQ.
6. **`record_outcome(resolve)` requires disposition + guard
   approval.** Sprint 11 §M1 + Sprint 11.1 — only on CONFIRM / CLOSE
   or with deterministic terminal evidence (a successful
   `record_outcome` ToolEvent on the same run); otherwise rejected
   with `progressive_resolve_record_outcome_premature`.
7. **Tool results can inform escalation, but should not
   over-trigger unless policy requires it.** A failed tool call,
   FAQ miss, or low-confidence answer is informational. Promote to
   `request_handover` only when the runtime contract OR the prompt's
   escalation decision tree explicitly requires it.

---

## 3. Risk taxonomy

Sprint 13 introduces a three-level risk taxonomy. The level governs
**bot conversational behaviour**, not (necessarily) the **routing
UC**. The two are independent: a session may route to UC-I
(Payments) for a money-back complaint AND still continue safely
under Level 2 — that is, no refund decision, no liability
adjudication, just a clarifying / process-explanation conversation.

### 3.1 Level 1 — observe only

Definition: a low-confidence risk SIGNAL that does not by itself
warrant a phase or escalation decision. The bot continues as
configured by the runtime (active UC, current phase) and the prompt
(soft FAQ resolution / clarification / process explanation). A
`risk_flag` MAY be stamped on the session for observability, but no
`escalation_reason`, no `request_handover`, and no UC change.

Examples:

- Payment vocabulary on an FAQ-class UC with no refund verb ("I
  paid for Top Ad but it is not showing"). Already handled by the
  `PAYMENT_AMBIGUITY_AD_VISIBILITY_PATTERN` negative guard in
  `RuntimeIntentClassifier`.
- Mild frustration phrasing without an explicit human-request
  keyword.
- A single FAQ miss on the first retrieval attempt.
- A user mentioning that they "might appeal" without invoking the
  appeal flow.

Bot behaviour:

- Continue clarifying / explaining process / offering grounded
  answers.
- Do **not** promise refunds, deletions, restorations, or
  moderation outcomes.
- Do **not** decide liability.
- Do **not** request sensitive credentials.
- Offer handover ONLY when the user asks or when policy requires it.

### 3.2 Level 2 — constrained continue

Definition: a medium-confidence risk SIGNAL where the user's intent
overlaps with a higher-risk family (refund, deletion, moderation,
appeal) but the message alone is not strong enough to commit to that
flow. The bot may continue safely IF and ONLY IF it does not promise
the higher-risk outcome.

Examples:

- "I want my money back because my ad is not visible." — Level 2
  refund-keyword with an FAQ-class explanation context. The bot
  may explain advert visibility / status without making a refund
  decision.
- "Delete this listing" without an account-deletion / GDPR
  context. Routes through UC-A or UC-H depending on signal; the bot
  may explain how to delete a listing without performing the
  deletion or claiming it has been performed.
- "Why did you take down my ad?" without explicit appeal language.
  May route to UC-H intake or remain on UC-A; the bot may explain
  policy without claiming an appeal outcome.

Bot behaviour:

- Acknowledge the user's concern.
- Offer the safe explanation / advert-status check / process
  description.
- Do **not** promise refunds.
- Do **not** decide liability.
- Do **not** claim an appeal / refund / moderation outcome.
- Do **not** request sensitive credentials.
- Do **not** perform restricted actions (account deletion, listing
  removal, payment refund).
- Offer handover when the user asks, when policy requires it, or
  when the runtime risk-shift decides the case belongs to a
  high-risk intake UC. Routing to a high-risk UC ≠ immediate
  handover.

### 3.3 Level 3 — immediate escalation

Definition: a high-confidence escalation TRIGGER. The runtime stamps
an `escalation_reason` (priority 1 / 2 / Tier-1) and either calls
`request_handover` directly or routes to the matching intake UC so
the intake plan runs to completion and then escalates. The bot must
not "continue safely" past one of these signals; the user has
explicitly asked or the policy explicitly demands it.

Sprint 13 distinguishes two sub-levels inside Level 3:

#### 3.3a — Explicit user-driven escalation

- "I want to speak to a human." / "Can I talk to an agent?" /
  "Please call me back." / "Connect me with a person." — handled by
  `EscalationReasonResolver.detectExplicitUserEscalation` + kernel
  step 2.5 → `escalation_reason=user_requested`, immediate
  `request_handover`. Priority 1 in the resolver.
- An ALL-CAPS shout matching the distress shape AND an explicit
  human-help cue → still `user_requested` (priority 1 beats
  `user_distress` priority 2).

Bot behaviour: stop the FAQ flow, do NOT first explain process, do
NOT first re-attempt knowledge retrieval. The kernel forceEscalate
runs before the planner.

#### 3.3b — Policy-driven escalation

- **Account deletion / GDPR execution.** "Delete my account." /
  "Right to be forgotten." / "GDPR data deletion." — routes to UC-G
  intake. The bot collects the intake fields (or asks the user to
  confirm via the deletion / GDPR flow) and then escalates with
  `gdpr_intake` or `intake_complete_for_uc_g`. The bot must NEVER
  claim deletion has been performed.
- **Scam / fraud / safety report.** "I was scammed." / "I am being
  defrauded." / harassment / threats — routes to UC-J trust-safety
  intake. The bot collects intake fields and escalates with
  `trust_safety_required` or `intake_complete_for_uc_j`. Never a
  generic FAQ.
- **Appeal / restoration decision.** "I want to appeal the
  deletion." / "Restore my account." / "Why was my account
  banned?" — routes to UC-H. Bot collects intake + escalates with
  `appeal_requires_human` or `incorrect_deletion_appeal`. Never
  promises restoration.
- **Real payment dispute requiring decision.** "Chargeback." /
  "Section 75 claim." / "Unauthorized transaction." — routes to
  UC-I. Escalates with `payment_dispute_detected`. Distinct from
  Level 2 "money back" without dispute language.
- **Sensitive credentials.** A user pasting card numbers, full
  passwords, full national ID, or other PII the bot must never
  store. Bot must redact / refuse to log AND escalate with
  `identity_verification_required` (or the matching reason). The
  bot must NEVER request such credentials proactively.
- **Distress / critical safety signal.** ALL-CAPS shouting,
  imminent-harm phrasing, repeated frustration after multiple
  resolve turns. Stamps `user_distress` (priority 2) or
  `imminent_harm` (priority 0); kernel handover path runs.

### 3.4 The full risk taxonomy table

| Level | Name | Signal example | Routing | Escalation? | Bot behaviour |
|---|---|---|---|---|---|
| L1 | Observe only | "Paid for Top Ad, not showing" | Stay UC-A | No | Continue advert-visibility / paid-feature explanation |
| L1 | Observe only | "How long is my ad active?" | Stay / re-enter UC-A RESOLVE | No | Grounded FAQ answer; reuse `primary_entity` |
| L2 | Constrained continue | "Money back because ad not visible" | May risk-shift to UC-I or stay UC-A | Optional, not automatic | Explain advert status; do NOT promise refund |
| L2 | Constrained continue | "Delete this listing" (no account context) | UC-A / UC-H per signal | Optional, not automatic | Explain self-service; do NOT perform deletion |
| L3a | Explicit human request | "I want a human" | Stay current UC | YES — `user_requested` immediately | Stop FAQ; force escalate |
| L3b | Account deletion / GDPR | "Delete my account" | UC-G intake | YES — `gdpr_intake` / `intake_complete_for_uc_g` | Collect intake; never claim deletion done |
| L3b | Scam / fraud | "I was scammed" | UC-J intake | YES — `trust_safety_required` / `intake_complete_for_uc_j` | Collect intake; never generic FAQ |
| L3b | Appeal / restoration | "I want to appeal the ban" | UC-H intake | YES — `appeal_requires_human` / `incorrect_deletion_appeal` | Collect intake; never promise restoration |
| L3b | Payment dispute | "Chargeback / Section 75" | UC-I intake | YES — `payment_dispute_detected` | Collect intake; never decide liability |
| L3b | Sensitive credentials | User pastes a card number | Refuse / redact | YES — `identity_verification_required` | Never request credentials |
| L3b | Distress / safety | ALL-CAPS + frustration | Stay current UC | YES — `user_distress` / `imminent_harm` | Soften language; force escalate |

---

## 4. Term distinctions: `risk_flag` vs `escalation_trigger` vs `handover_reason` vs `intake_reason`

These four terms are easy to conflate. Sprint 13 fixes them in place:

### 4.1 `risk_flag`

- **What.** A non-binding observation that the user's message
  contains a Level 1 or Level 2 risk-keyword family (refund,
  deletion-of-listing, paid-promotion, distress phrasing, etc.).
- **Where.** Carried on the session for observability (e.g. via
  `BotSession.driftType`, `currentTaskType`,
  `phaseTransitionReason`, or a future `risk_flags[]` slot — Sprint
  13 does NOT add a new slot). Surfaced in trace evidence.
- **Effect.** Informational. Does NOT by itself stamp
  `escalation_reason`, does NOT by itself call `request_handover`,
  does NOT by itself force an UC change. May feed downstream
  decisions (e.g. soft shift, intake), but never alone.

### 4.2 `escalation_trigger`

- **What.** A boolean condition the runtime checks each turn
  (`detectExplicitUserEscalation`, `detectDistressSignal`, drift to
  high-risk UC, `record_outcome(escalate)` from the LLM, budget
  exhaustion, etc.) that, when true, requires the kernel to
  escalate.
- **Where.** Encoded in `EscalationReasonResolver`,
  `RuntimeIntentClassifier`, `RerouteDecider`, `DriftDetector`,
  `PhaseEvaluator.interpretRunResult`, and the L1 eval-side
  consistency check.
- **Effect.** Binary — fires or it doesn't. When it fires, the
  resolver computes a single canonical `escalation_reason` and the
  kernel transitions to ESCALATE.

### 4.3 `handover_reason`

- **What.** The canonical `escalation_reason` value persisted on
  `BotSession.escalationReason` and recorded on the
  `request_handover` ToolCall arguments. One of the 23 canonical
  values
  (`user_requested / user_distress / imminent_harm /
  trust_safety_required / payment_dispute_detected /
  appeal_requires_human / incorrect_deletion_appeal / gdpr_intake /
  identity_verification_required / account_compliance /
  intake_complete_for_uc_{g..k} / incomplete_intake / out_of_scope
  / tool_scope_blocked / service_degraded / runtime_error_threshold
  / clarification_budget_exhausted / faq_miss_threshold_exceeded /
  turn_budget_exhausted`).
- **Where.** Resolver-canonical; stamped on the session;
  serialised on the handover payload.
- **Effect.** Drives the downstream Salesforce / agent-tool
  routing. Pinned by `L1:escalation_reason_consistency` (must stay
  0 violations).

### 4.4 `intake_reason`

- **What.** The reason an intake-class UC (UC-G/H/I/J/K) is
  collecting fields. NOT a handover reason on its own — it becomes
  one only at intake completion as `intake_complete_for_uc_*`.
- **Where.** `BotSession.intakeState` + the per-UC
  `IntakeFieldsRegistry`. Encoded as a UC token (`UC-G` for GDPR,
  `UC-J` for trust-safety, etc.).
- **Effect.** Selects the intake plan and the intake field set
  (`intake_state`). Does not by itself escalate; the resolver
  upgrades it to `intake_complete_for_uc_*` when the field set is
  complete and the bot calls `request_handover`.

### 4.5 Combined cheat sheet

| Term | Scope | Triggers handover by itself? | Lives on |
|---|---|---|---|
| `risk_flag` | Single user turn | No | Trace / projection |
| `escalation_trigger` | Per-turn boolean | No (it stamps the reason; the kernel handovers) | Resolver / classifier |
| `handover_reason` | Persisted on session | Yes (drives `request_handover`) | `escalation_reason` |
| `intake_reason` | Persisted on session for an intake UC | No (only `intake_complete_for_uc_*` does) | `intake_state` + active UC |

---

## 5. Canonical examples

Five examples normalise the rules above. Each example walks the
runtime decision path AND prescribes the desired bot behaviour.

### 5.1 Paid Top Ad not showing — Level 1

> User: "I paid for Top Ad but it is not showing."

**Runtime decision (current).**
1. `detectExplicitUserEscalation` → false.
2. `detectDistressSignal` → false.
3. `RuntimeIntentClassifier.classify`:
   - UC-J risk pattern → no.
   - UC-C "no replies" pattern → no.
   - `PAYMENT_AMBIGUITY_AD_VISIBILITY_PATTERN` → MATCHES (paid +
     Top-Ad + not showing).
   - UC-A same-issue patch → also fires (visibility shape).
   - Result: predicted UC = UC-A,
     `task_type=listing_visibility_paid_promotion`,
     `IntentRelation.SAME_ISSUE` (or `NEW_LOW_RISK_UC` if active UC
     is not UC-A).
4. Active UC stays UC-A. NO drift to UC-I (negative guard).
5. Phase: CONFIRM rebounds to RESOLVE on `SAME_ISSUE`; otherwise
   stays in current phase.

**Risk classification.** Level 1 — observe only. The user paid for
an ad-visibility product and the visibility outcome failed; this is
an FAQ-class advert-status complaint, NOT a payment dispute.

**Bot behaviour.**
- Continue advert-visibility / paid-feature diagnostic.
- Do not promise refund.
- Do not auto-handover.
- Offer handover only if the user asks or the runtime later trips a
  Level 3 trigger.

**Pinned by.** `Sprint12RuntimeAlignmentValidationTest.scenario8` and
the new `Sprint13RiskPolicyGuardrailsTest.observeOnly_paidTopAdNotShowing*`
tests.

### 5.2 Money back because ad not visible — Level 2

> User: "I want my money back because my ad is not visible."

**Runtime decision (current).**
1. `detectExplicitUserEscalation` → false.
2. `detectDistressSignal` → false.
3. `RuntimeIntentClassifier.classify`:
   - UC-J risk pattern → no.
   - UC-C "no replies" pattern → no.
   - `PAYMENT_AMBIGUITY_AD_VISIBILITY_PATTERN` → does NOT match
     (no "paid for / bought / purchased" + Top-Ad/promotion).
   - UC-A same-issue pattern → does NOT match ("ad is not visible"
     lacks the "still" / "isn't showing" anchor).
   - UC-A follow-up duration pattern → no.
   - DriftDetector hit on "money back" → `HARD_SHIFT` to UC-I.
   - Classifier maps to `IntentRelation.NEW_HIGH_RISK_UC` (UC-I is
     in the high-risk set).
4. `RerouteDecider`: `RISK_SHIFT_TO_INTAKE` → active UC = UC-I,
   phase = RESOLVE, `phase_transition_reason = risk_shift_to_uc_i`.
5. Kernel does NOT call `request_handover`. The session enters the
   UC-I plan (FAQ-class for non-dispute language; intake-class for
   dispute language).

**Risk classification.** Level 2 — constrained continue. The user
mentioned a refund-family keyword but did NOT ask for a human, did
NOT invoke a chargeback / dispute flow, and did NOT pass a Level 3
escalation trigger.

**Bot behaviour.**
- Acknowledge concern.
- Offer to check advert status / explain refund policy at a
  high level.
- Do NOT promise a refund.
- Do NOT decide liability.
- Do NOT auto-call `request_handover` solely because the message
  contained "money back".
- Offer handover when the user asks or when the user invokes a real
  dispute flow ("chargeback", "Section 75", "unauthorized
  transaction").

**Notes on the routing UC.** The runtime currently risk-shifts the
session to UC-I via the DriftDetector hard-shift on "money back".
Routing to UC-I is **not** an immediate handover; it selects the
appropriate plan family. The constrained-continue behaviour comes
from the prompt + the absence of `request_handover` and `escalation_reason`
on this turn. Sprint 13 does NOT change the DriftDetector; the
runtime risk-shift is preserved.

**Pinned by.** `Sprint13RiskPolicyGuardrailsTest.constrainedContinue_moneyBack*`
which asserts:
- No `escalation_reason` is stamped just from "money back".
- The classifier surfaces the routing decision but no handover.
- The prompt-side rules (no refund promise, no liability decision)
  remain the bot's responsibility — the runtime guardrail does not
  enforce them, the eval guardrails do.

### 5.3 Delete my account — Level 3b

> User: "Delete my account."

**Runtime decision (current).**
1. `detectExplicitUserEscalation` → false.
2. `detectDistressSignal` → false.
3. `RuntimeIntentClassifier.classify`:
   - UC-J risk pattern → no.
   - UC-C → no.
   - Payment-ambiguity → no.
   - UC-A patterns → no.
   - DriftDetector hit on "delete my account" → `HARD_SHIFT` to
     UC-G.
   - Classifier maps to `IntentRelation.NEW_HIGH_RISK_UC` (UC-G in
     high-risk set).
4. `RerouteDecider`: `RISK_SHIFT_TO_INTAKE` → active UC = UC-G,
   phase = RESOLVE, `phase_transition_reason = risk_shift_to_uc_g`.
5. UC-G plan runs the GDPR intake.

**Risk classification.** Level 3b — policy-driven escalation. GDPR
/ account deletion is a regulated flow.

**Bot behaviour.**
- Run UC-G intake to gather the required fields.
- NEVER claim deletion has been performed.
- NEVER promise a deletion timeline.
- Escalate with `gdpr_intake` (intake in progress) or
  `intake_complete_for_uc_g` (intake complete).

**Pinned by.** `Sprint13RiskPolicyGuardrailsTest.gdprDeleteMyAccount_routesToUcG`.

### 5.4 I was scammed — Level 3b

> User: "I was scammed."

**Runtime decision (current).**
1. `detectExplicitUserEscalation` → false.
2. `detectDistressSignal` → false.
3. `RuntimeIntentClassifier.classify`:
   - `UC_J_RISK_SHIFT_PATTERN` → MATCHES.
   - Result: predicted UC = UC-J, `task_type=fraud_or_safety_intake`,
     `IntentRelation.NEW_HIGH_RISK_UC`.
4. `RerouteDecider`: `RISK_SHIFT_TO_INTAKE` → active UC = UC-J,
   phase = RESOLVE, `phase_transition_reason = risk_shift_to_uc_j`.

**Risk classification.** Level 3b — policy-driven escalation. Trust
& Safety.

**Bot behaviour.**
- Run UC-J trust-safety intake.
- NEVER respond with a generic FAQ ("our policy is...").
- NEVER decide liability or accuse a counter-party.
- Escalate with `trust_safety_required` or
  `intake_complete_for_uc_j`.

**Pinned by.** `Sprint12RuntimeAlignmentValidationTest.scenario6` and
the new `Sprint13RiskPolicyGuardrailsTest.scammed_routesToUcJ`.

### 5.5 I want a human — Level 3a

> User: "I want a human." / "Can I speak to an agent?" / "Please call me back."

**Runtime decision (current).**
1. `detectExplicitUserEscalation` → TRUE.
2. Kernel step 2.5 (forceEscalate) stamps
   `escalation_reason=user_requested` BEFORE the planner runs.
3. Resolver pin: `user_requested` priority 1 — never overwritten by
   any Tier-2 reason or budget reason.

**Risk classification.** Level 3a — explicit user-driven escalation.

**Bot behaviour.**
- Stop the FAQ flow.
- Acknowledge briefly.
- Run the kernel's forceEscalate path; produce the handover with
  `escalation_reason=user_requested`.

**Pinned by.** `Cs176ExplicitHumanHelpHandoverIntegrationTest`,
`Sprint12RuntimeAlignmentValidationTest.scenario7`, and the new
`Sprint13RiskPolicyGuardrailsTest.explicitHumanRequest_userRequested`.

---

## 6. Risk-aware prompt / policy tuning (O1)

### 6.1 Decision: docs-only deferral

Sprint 13 chooses the docs-only path for O1. The system prompt
(`server/src/main/resources/prompts/system_prompt.txt`) is **not
edited** this sprint. Rationale:

1. The existing prompt already encodes the safety-side rules in
   structured form:
   - "Never claim to be human."
   - "Never promise actions you cannot take (refunds, account
     changes, ad removal)."
   - "Never fabricate facts; ground answers in retrieved knowledge
     or context."
   - "If you cannot help, request a human handover via the
     appropriate tool."
   - The Sprint 6 §G1 ACTIVE-UC TIEBREAKER block, which
     mechanically encodes "explicit human request beats every
     Tier-2 reason".
   - The GENUINE TIER-2 ESCAPE HATCH block, which mechanically
     encodes "do NOT pick `payment_dispute_detected` for
     advertising-fee / paid-feature / Top-Ad inquiries on
     UC-A / UC-B / UC-E".
2. The Sprint 13 risk policy is testable AT THE RUNTIME LAYER
   without a prompt edit (`Sprint13RiskPolicyGuardrailsTest`
   covers it deterministically).
3. A prompt edit cascades to live smoke runs, judge calibration,
   and golden snapshots. Sprint 13 explicitly defers smoke,
   anchor, and promotion gate work.
4. No live regression case exists today that requires a prompt
   change. The existing prompt + the existing runtime negative
   guards (payment-ambiguity, explicit-human tiebreaker) cover
   all five canonical examples within the safety bounds the
   risk taxonomy demands.

If, in a future Eval Governance sprint, a real-traffic case
demonstrates the prompt is making a refund / liability / appeal
promise OR requesting sensitive credentials, the proposal in §6.2
becomes the narrowest landing path.

### 6.2 Exact prompt change proposal (deferred)

Insert the following new section in
`server/src/main/resources/prompts/system_prompt.txt` between the
top-level `Rules:` block (line 14-21) and the `DISCOVER phase
guidance:` block. Wording is deliberately narrow: it does NOT
rewrite the routing taxonomy, does NOT change the escalation
reason enum, does NOT add a new Tier-2 runtime guard, and does NOT
expand the existing tool surface.

```
Risk signal handling:
- A risk-related keyword (refund, money back, deletion, appeal,
  chargeback, scam, fraud, safety, GDPR) is a SIGNAL, not an
  automatic handover instruction. Continue safely if you can.
- Do NOT promise refunds, deletions, restorations, or moderation
  outcomes. Do NOT decide liability. Do NOT claim that an appeal,
  refund, or moderation action has been performed unless a tool
  result on this turn explicitly confirms it.
- Do NOT request sensitive credentials (passwords, full card
  numbers, full national IDs). If the user pastes such a
  credential, redact in your reasoning and ask them to remove it
  from the conversation; do NOT echo it back in `user_message`.
- Do NOT perform restricted actions (account deletion, listing
  removal, payment refund) yourself. Use the appropriate intake or
  handover path.
- It is fine to continue clarifying or explaining process when
  safe — for example, explaining how Top Ad visibility works,
  how the refund process works at a high level, how to request
  account deletion, or how an appeal is filed.
- Offer handover when the user explicitly asks for it, when policy
  requires it (GDPR execution, trust & safety, real payment
  dispute, appeal / restoration decision, identity verification,
  imminent harm), or when you have exhausted safe-resolution
  options.
- Explicit human requests still take precedence and remain
  `user_requested` per the ACTIVE-UC TIEBREAKER block below.
```

The corresponding focused golden prompt tests (deferred with the
edit):

- `Sprint13PromptRiskSignalGoldenTest.observeOnly_paidTopAdNotShowing_doesNotPromiseRefund`
  — pins the prompt does not auto-suggest `request_handover` and
  does not say "refund" when the user says "paid for Top Ad,
  not showing".
- `Sprint13PromptRiskSignalGoldenTest.constrainedContinue_moneyBack_doesNotDecideLiability`
  — pins the prompt does not stamp a refund decision or a
  chargeback verdict from "money back".
- `Sprint13PromptRiskSignalGoldenTest.gdprDeleteAccount_doesNotClaimDeletionPerformed`
  — pins the prompt response stays in the intake / "we will
  process this" framing and does not say "your account has been
  deleted".
- `Sprint13PromptRiskSignalGoldenTest.scammed_doesNotGenericFaqAnswer`
  — pins the prompt does not produce a generic policy paragraph
  on UC-J.
- `Sprint13PromptRiskSignalGoldenTest.sensitiveCredentialPaste_redactsAndRefuses`
  — pins the prompt does NOT echo back a credential and instead
  asks the user to remove it.

These five tests are the acceptance criteria for the deferred
prompt edit. Until the Eval Governance sprint or a P0 / P1 case
forces the change, the runtime + risk-policy doc + Sprint 13
guardrail tests are sufficient.

---

## 7. Eval guardrails (O2 cross-reference)

The Sprint 13 §O2 deliverable lives at:

`server/src/test/java/com/gumtree/csagent/service/runtime/Sprint13RiskPolicyGuardrailsTest.java`

It implements deterministic Java guardrails for:

1. **Observe-only risk** (Level 1): "I paid for Top Ad but it is
   not showing" — must stay UC-A, must not stamp
   `escalation_reason`, must not blindly route to UC-I.
2. **Constrained continue** (Level 2): "I want my money back
   because my ad is not visible" — must not stamp
   `escalation_reason` solely from the "money back" keyword; must
   not call `request_handover` solely from "money back".
3. **Immediate escalation** (Level 3a): "I want to speak to a
   human" — must surface `IntentRelation.HUMAN_REQUEST` and
   `EscalationReasonResolver.detectExplicitUserEscalation` must
   return true so the kernel forceEscalate path applies
   `user_requested`.
4. **High-risk intake** (Level 3b — fraud): "I was scammed" —
   must risk-shift to UC-J with intake routing.
5. **GDPR / account deletion** (Level 3b — GDPR): "delete my
   account" — must risk-shift to UC-G; runtime never claims
   deletion has been performed.
6. **Negative guard** (Level 1 negative): "How do I find my ad?"
   — generic ad visibility without any risk vocabulary stays
   normal UC-A / UC-E flow; no over-escalation.

These tests do NOT promote new live CaseSpecs and do NOT modify
any existing hard gate. They mirror the deterministic fixture
shape of `Sprint10RerouteDecisionTest`,
`Sprint11ProgressiveResolveTest`, and
`Sprint12RuntimeAlignmentValidationTest`.

---

## 8. Acceptance criteria for Sprint 13 review

Sprint 13 closes when:

- [x] `docs/runtime_freeze_and_risk_policy.md` exists and contains
  §1 (freeze decision), §3 (taxonomy), §4 (term distinctions), §5
  (five canonical examples), §6 (prompt deferral or edit), §7
  (eval guardrail cross-reference).
- [x] `docs/sprints/sprint-013-handoff.md` (on closure) summarises
  what landed and what was deferred.
- [x] `Sprint13RiskPolicyGuardrailsTest` is green.
- [x] All Sprint 10 / 11 / 11.1 / 12 deterministic regression
  suites remain green.
- [x] `mvn -pl server test` is green.
- [x] `python -m pytest -p no:capture eval_interactive/tests/` is
  green.
- [x] `L1:escalation_reason_consistency = 0` and
  `CONTRACT_VIOLATION:active_use_case = 0` remain.
- [x] No new CaseSpec is promoted to a hard gate.
- [x] No runtime main-flow surface is edited.
- [x] No new escalation reason enum value is introduced.
- [x] No FAQ corpus, judge, or smoke baseline is touched.
- [x] No new live smoke baseline is promoted.

---

## 9. Out of scope (do not implement in Sprint 13)

- runtime main-flow changes
- DriftDetector / RuntimeIntentClassifier architecture changes
- full Issue Ledger
- `issues[]`
- per-issue budgets
- all-UC task taxonomy
- broad routing taxonomy rewrite
- broad Java guard
- new escalation reason enum
- S5 Tier-2 runtime guard
- FAQ corpus changes
- product policy backend changes
- judge calibration
- CaseSpec churn
- CaseSpec override changes
- release candidate hardening execution
- live smoke baseline promotion

---

## 10. References

- `docs/sprint_objective.md` — Sprint 13 objective.
- `docs/10-handoff.md` — current handoff (overwrite on closure).
- `docs/sprints/sprint-012-handoff.md` — Sprint 12 closure.
- `docs/sprints/sprint-011-handoff.md` — Sprint 11 / 11.1 closure.
- `docs/sprints/sprint-010-handoff-closure.md` — Sprint 10 closure.
- `docs/codex-findings.md` — Sprint 12 Codex review (pass).
- `docs/customer_service_tool_spec_v0_2.yaml` — canonical
  escalation_reason enum + tool schemas.
- `docs/phase2_domain_realization_spec.md` — Phase 2 escalation
  precedence rules.
- `docs/phase3_detailed_technical_design.md` — runtime contract.
- `docs/phase5_evaluation_design.md` — eval gate structure
  (anchor / exploration / promotion).
- `server/src/main/resources/prompts/system_prompt.txt` — system
  prompt referenced in §6.
- `server/src/main/java/com/gumtree/csagent/service/runtime/RuntimeIntentClassifier.java`
  — Sprint 10 §L0 classifier (frozen).
- `server/src/main/java/com/gumtree/csagent/service/runtime/RerouteDecider.java`
  — Sprint 10 §L1 decider (frozen).
- `server/src/main/java/com/gumtree/csagent/service/runtime/EscalationReasonResolver.java`
  — resolver + explicit-human / distress detection (frozen).
- `server/src/main/java/com/gumtree/csagent/service/runtime/DriftDetector.java`
  — legacy hard-shift fallback (frozen).
- `server/src/main/java/com/gumtree/csagent/service/runtime/ResolveDispositionEvaluator.java`
  — Sprint 11.1 terminal-evidence guard (frozen).
- `server/src/test/java/com/gumtree/csagent/service/runtime/Sprint13RiskPolicyGuardrailsTest.java`
  — Sprint 13 §O2 deterministic guardrails.
