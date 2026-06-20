---
title: "Sprint 095 / S-Auto-43 (M-Auto-9 WP1) — handoff: user_requested escalation-reason honesty → BLOCKED (vocabulary gap)"
doc_tier: sprint-archive
status: current
implementation_status: not_started
source_of_truth: this file (dev handoff); runtime code + exp-90 evidence cited inline
last_reviewed: 2026-06-20
review_cadence: per sprint
supersedes: []
superseded_by: null
notes: >
  WP1 terminal outcome = **BLOCKED** by the §6 existing-reason honesty gate. The
  bot-initiated escalations that the LLM mislabels `escalation_reason=user_requested`
  carry the consistent semantic "the bot exhausted its grounded resolution and the
  issue is unresolved / needs a human to take over." No member of the canonical
  23-value `escalation_reason` enum honestly describes that semantic: the user-signal
  trio is dishonest (no user signal), the safety/dispute/compliance/intake values are
  UC-bound to risk categories not present, `out_of_scope` is bound 100% to non-V1
  intent, and `service_degraded` is the §6-forbidden inaccurate catch-all. Shipping
  any of them would swap one dishonest label for another — explicitly disallowed.
  The honest fix needs a NEW bot-initiated reason value = the deferred
  `D-new-escalation-reason-enum` (action_bank §4), out of WP1 scope. **No code
  shipped** — tree returned to the documented baseline; WP1 is NOT declared done.
  WP0 (source_ids) and WP2 (CONFIRM record-vs-handover) remain HELD under charter §6.
  This sub-sprint does NOT solve the M-Auto-9 PRIMARY closure / product-contract
  question. Handoff is docs-only (no semantic-surface change) → §4.1 Codex gate N/A
  (§4 docs-only exemption; nothing to review).
---

# Sprint 095 / S-Auto-43 (M-Auto-9 WP1) — handoff

One-line: correct the `user_requested` escalation-reason mislabel on **bot-initiated**
handover by reserving `user_requested` for genuine user requests and giving the LLM an
**accurate bot-initiated reason from the existing enum** — reason-label HONESTY only.

**Terminal outcome: BLOCKED** (one of the two contract-sanctioned outcomes; the other
is COMPLETE). The §6 existing-reason honesty gate fires: there is no semantically honest
target reason in the existing canonical enum, so the only contract-valid result is to
STOP and surface the vocabulary gap. No code was shipped.

HEAD at sprint start: clean tree on `auto-loop-branch` (the documented post-Sprint-093
baseline `1394 / 1 / 0 / 2`; sole failure = inherited, provably-uncoupled
`SystemPromptUserRequestedTiebreakerTest.systemPrompt_marksActiveUcTiebreakerExplicitly`,
OQ-S41.5). No code change → no Java re-run was required to certify "no new regression"
(zero edits ⇒ zero new regressions by construction; §5.3 gate trivially satisfied).

## §0 Status table

| Step | Layer | State | Evidence |
|---|---|---|---|
| §5.1 — three-class defect characterization (read-only) | `semantic_planner` / `prompt_projection` | **DONE** | exp-90 `eval-results.json` per-attempt reasons + representative-trace handover summaries + transcripts; corroborated by OQ-S93.1 brief §3.3. |
| §6 — existing-reason honesty gate | — | **FIRED → BLOCKED** | all 23 canonical reasons enumerated below; none honestly describes the bot-initiated "exhausted-resolution / needs-human" semantic. `out_of_scope` bound 100% to non-V1 intent (`phase2_domain_realization_spec.md:564,582`); `service_degraded` §6-forbidden catch-all. |
| §5.2 — Java characterization tests | — | **N/A (no code shipped)** | tests would validate a COMPLETE-path code change; the gate fired before any edit, so there is nothing to characterize. |
| §5.3 — Java suite re-measure | — | **N/A (no code shipped)** | zero edits ⇒ no new regression vs documented baseline. |
| §5.4 — zero-LLM attribution replay | — | **N/A (no code shipped)** | the wiring the replay would exercise was never written (no honest target reason exists). |
| §4.1 — per-sub-sprint Codex | — | **N/A** | no semantic-surface change shipped → not triggered; handoff is docs-only (§4 docs-only exemption). |
| Vocabulary gap surfaced | — | **DONE** | mapped to existing `D-new-escalation-reason-enum` (action_bank §4); recommendation to deliver in §10. |

## §1 Why BLOCKED, in one paragraph

The defect is real and reproduced (§2): on bot-initiated escalations the LLM supplies
`escalation_reason=user_requested` though the customer never asked for a human. The
contract (`docs/sprint_objective.md` §6) admits exactly two terminal outcomes: use an
**existing** canonical reason that **honestly** describes the bot-initiated handover and
COMPLETE; or, if no existing value is honest enough, STOP and surface the vocabulary gap
and close BLOCKED. The bot-initiated handovers all carry one semantic — *"the bot has
exhausted its grounded help on an in-scope issue, the problem is unresolved, and a human
needs to take over / investigate"* (the bot's own summaries: *"Needs human
investigation"*, *"Needs account recovery assistance"*). Walking the full 23-value enum
(§3), **no value carries that meaning honestly**. The contract forbids shipping a
knowingly-inaccurate catch-all (`service_degraded`) and forbids adding a new enum value
(that is the deferred `D-new-escalation-reason-enum`). Therefore the honest result is
BLOCKED, not COMPLETE. Shipping the "reserve `user_requested`" teaching *without* an
honest target would merely relocate the dishonesty onto `service_degraded`/`out_of_scope`
— which §6 names as "swapping one dishonest label for another" and disallows — so even a
partial edit was not shipped (§5).

## §2 Three-class defect characterization (§5.1, read-only)

Primary evidence: `autoloop/results/runs/exp-90/eval-results.json` (present locally, 4.4M;
3 suites — `bad_cases`, `anchor_outcome`, `shadow`; 5 attempts/case). Per the contract,
the raw Sprint-093 bounded dirs (`eval_interactive/results/2026-06-18-*`) are gitignored /
absent, so the OQ-S93.1 brief §3.3 recorded classification is used as the documented
Sprint-093 corroboration.

Every escalation falls into exactly one of three classes:

| class | meaning | label source | WP1 disposition |
|---|---|---|---|
| **A. genuine user-requested** | the customer actually asked for a human / callback | LLM arg `user_requested` (correct) | must be PRESERVED → `user_requested` |
| **B. bot-initiated mislabel** | the bot decided to escalate after exhausting grounded help; user never asked | LLM arg `user_requested` (WRONG) | the WP1 target defect |
| **C. runtime budget-family fallback** | `turn_budget_exhausted` / `faq_miss_threshold_exceeded` / `clarification_budget_exhausted` set by `resolveMaxStepsReason` on loop/budget exhaustion | runtime stamp (frozen) | OUT OF SCOPE — unchanged |

**Class A — verified genuine (preserve):**
- `cs029_uc_d_account_locked_callback` — handover summary: *"…now **requesting a phone
  call** from their account manager."* `user_requested` is honest. (4/5 attempts
  `user_requested`.)

**Class B — verified bot-initiated mislabels (the defect):**
- `anchor_outcome_uc_c_messaging` — summary: *"User not receiving message notifications…
  **Needs human investigation.**"* User's final turn: *"My friend sent a test message and
  I still didn't receive it. **What should I do next?**"* → the user addresses the **bot**;
  no human request. Labeled `user_requested` (3/5 attempts). UC-C is an in-scope V1 UC.
- `anchor_outcome_uc_d_login` — summary: *"User Wasim cannot log in after password reset
  and browser troubleshooting. **Needs account recovery assistance.**"* User's final turn:
  *"…I still can't log in. **Can you help me recover my account?**"* → addressed to the
  **bot**. Labeled `user_requested` (4/5 attempts). UC-D in-scope.
- `cs11s01_uc_d_two_emails_one_account` (shadow) — summary: *"…**User wants to understand
  why** work email can't log in."* → informational need the bot could not satisfy; not a
  human request. Labeled `user_requested` (2/5 attempts).

**Class B — corroborating distribution (exp-90, not individually trace-verified):**
`user_requested` also appears in a minority of attempts on cooperative, problem-persists
personas with no human-request turn sampled — e.g. `cs_uc_a_loaded_listing` (1 attempt;
the case-level draw resolved), `cs_uc_a_no_ad_id_ad_specific` (1), `cs_uc_fp_loaded_moderation`
(1), `iwzx_uc_k_advert_on_hold_restore` (1), `anchor_outcome_uc_a_visibility` (1),
`cs001_uc_c_mechanical_template_escalate` (1), `cs15s02_uc_fp_keyword_match_appeal` (1),
`cs95s01_uc_d_phone_changed_no_messages` (1). These match the brief's Class-B shape.

**Class C — runtime budget-family (unchanged):** the dominant escalation label across the
corpus (`turn_budget_exhausted` / `faq_miss_threshold_exceeded` /
`clarification_budget_exhausted`), produced by `resolveMaxStepsReason` — the separate,
frozen vector (`project_faq_overescalate_maxsteps_misstamp`). Not touched.

**Corroboration with OQ-S93.1 brief §3.3** (documented Sprint-093 evidence): of 13/22
PRIMARY escalations — **2 genuine** user-accepted + **6 bot-initiated `user_requested`
mislabels** + **5 budget-family**. The exp-90 mix above is consistent: a small genuine
set (callback case), a recurring bot-initiated `user_requested` set on in-scope non-intake
UCs, and a large budget-family remainder.

Key structural fact confirming the defect is LLM-supplied (not a runtime stamp): the
`escalation_reason` arg is read verbatim from the `request_handover` tool call
(`RequestHandoverTool.java:60`, persisted `:87`/`:102`), and `user_requested` is priority
**1** (Tier-0) in `EscalationReasonResolver.PRIORITY` (`:84-113`), so once the LLM supplies
it, no later runtime fallback overwrites it (`resolve()` keeps the lower-priority-number
winner). The mislabel sticks. This is `semantic_planner` / `prompt_projection`, never a
Java guard — consistent with the contract layer assignment.

## §3 Existing-reason honesty gate — full enumeration (§6, the decisive analysis)

Target semantic to label honestly: **"bot exhausted its grounded resolution on an
in-scope, non-intake issue; the problem is unresolved; a human must take over /
investigate."** Walking all 23 canonical reasons
(`EscalationReasonResolver.CANONICAL_REASONS:51-75`):

| reason (priority) | honest here? | why not |
|---|---|---|
| `imminent_harm` (0) | ✗ | user-signal reason; no harm signal |
| `user_requested` (1) | ✗ | **the bug** — user did not request a human |
| `user_distress` (2) | ✗ | user-signal reason; the Class-B personas are calm/cooperative (genuine distress *is* correctly labeled elsewhere, e.g. `cs11s02…high_distress`) |
| `trust_safety_required` (10) | ✗ | UC-J-bound risk category (100% UC-bound, `phase2…:564`); no scam/safety |
| `payment_dispute_detected` (11) | ✗ | UC-I-bound; no payment dispute |
| `appeal_requires_human` (12) | ✗ | UC-H appeal category; not an appeal |
| `incorrect_deletion_appeal` (13) | ✗ | deletion-appeal specific |
| `gdpr_intake` (14) | ✗ | UC-G GDPR-bound |
| `identity_verification_required` (15) | ✗ | identity-verification specific; "account recovery" ≠ ID-verification |
| `account_compliance` (16) | ✗ | compliance-hold specific; the bot just couldn't fix a login, not a compliance action |
| `intake_complete_for_uc_{g,h,i,j,k}` (20-24) | ✗ | UC-specific intake-completion handoffs; the Class-B population is non-intake UC-A/C/D and intake is not the gate (and where intake IS the path, the bot already uses these correctly) |
| `incomplete_intake` (25) | ✗ | intake not the issue |
| `out_of_scope` (26) | ✗ | **bound 100% to non-V1-covered intent / the OOS category** (`phase2…:564` `out_of_scope→OOS(100%)`; `:582` "非 V1 覆盖意图"; phase5 OOS-detection metric). The Class-B issues are **in-scope** UCs the bot engaged — labeling them `out_of_scope` falsely reclassifies a covered issue as uncovered and corrupts `out_of_scope_detection` (≥90% target). Dishonest. |
| `tool_scope_blocked` (27) | ✗ | no tool was scope-blocked |
| `service_degraded` (30) | ✗ | infra "service degraded" catch-all; the system is **not** degraded — the bot simply couldn't resolve one customer's issue. §6 explicitly forbids it as a knowingly-inaccurate catch-all. |
| `runtime_error_threshold` (31) | ✗ | no runtime errors |
| `clarification_budget_exhausted` (40) | ✗ | budget-family; runtime-owned + FROZEN; a control-plane terminal-close reason, not the semantic reason the bot decided to hand over |
| `faq_miss_threshold_exceeded` (41) | ✗ | budget-family; runtime-owned + FROZEN |
| `turn_budget_exhausted` (42) | ✗ | budget-family; runtime-owned + FROZEN |

**Result: 0 of 23 honest.** The honest target — a bot-initiated "exhausted-resolution /
unresolved-needs-human" reason — does not exist in the enum. Gate FIRES → BLOCKED.

**Independent corroboration of the gap from the codebase itself:** `confirm.yaml:20`
already prescribes `reason 'user_dissatisfied'` for the not-satisfied handover, but
`user_dissatisfied` is **not** a canonical value (0 hits in `EscalationReasonResolver`),
so `canonicalize()` (`:256-285`) silently maps it to `service_degraded`. The skill author
reached for a "bot couldn't satisfy the user" reason that does not exist canonically and
got the dishonest catch-all. The vocabulary gap **predates WP1** and is structurally
baked into the active skill — strong evidence the gap is real, not an artifact of one run.

## §4 The vocabulary gap → `D-new-escalation-reason-enum`

The honest fix is a dedicated bot-initiated reason value (semantics: *the agent exhausted
its grounded resolution and a human must take over*; candidate spellings such as
`agent_unable_to_resolve` / `exhausted_resolution` are illustrative only — naming is the
migration's job). Adding/renaming a canonical `escalation_reason` value is exactly the
**deferred `D-new-escalation-reason-enum`** item (action_bank §4: *"adding / renaming a
value in the canonical 23-value escalation_reason enum … deferred / avoid … cross-cuts the
eval-side `ESCALATION_TRIGGER_VALUES` set; needs a coordinated migration via a new
objective doc"*). Precedent for the discipline: `R-runtime-escalation-reason-misstamp-maxsteps-faq`
(S-Auto-14) deliberately **reused** `turn_budget_exhausted` rather than mint a new enum,
"the §4 D-new-escalation-reason-enum avoid-item respected" — but that worked only because
`turn_budget_exhausted` was *honest* for a budget close. Here no honest reuse target exists,
so the choice is mint-a-new-value (deferred) or BLOCK. Per the contract, BLOCK.

A new value would require, at minimum (scoping note for the migration objective, not done
here): (1) add to `EscalationReasonResolver.CANONICAL_REASONS` + a Tier-3/low PRIORITY slot
(so it cannot displace safety/dispute reasons); (2) add to
`ContextProjectionBuilder` enum + the eval-side `EscalationTrigger` Literal
(`eval_interactive/.../case_spec/schema.py`) in lockstep; (3) decide how the eval scoring
layer / `escalation_reason_family_match` treats it; (4) update `confirm.yaml` to prescribe
it; (5) projection teaching reserving `user_requested` for genuine requests. That cross-cut
is the reason it is a coordinated migration, not a WP1 label tweak.

## §5 Why no partial honesty win was shipped (anti-relocation, §6 + §7)

A tempting partial edit: tighten `confirm.yaml` / add projection teaching to "reserve
`user_requested` for an actual user request." On its own the *reservation* statement is
honest. But without an honest **target** reason for the bot-initiated case, that teaching
only pushes the LLM off `user_requested` and onto the next-nearest existing value —
`service_degraded` (via the current `user_dissatisfied` path) or `out_of_scope`. §6 names
this precisely: *"Swapping one dishonest label for another dishonest label is not the
honesty fix … There is no 'honesty floor' that justifies an inaccurate label."* So the
reservation teaching cannot be shipped in isolation as a WP1 win. Per §7 rollback
discipline ("if STOP fires after a partial edit, revert to the documented baseline"), **no
edit was made** to `confirm.yaml`, `ContextProjectionBuilder.java`, or any other surface.
`git status` is clean; the tree is byte-identical to the documented baseline.

## §6 Safety / standing-guards (§8)

With **no code change**, every §8 standing guard is trivially green (no behaviour moved):
`anchor_uc_g_gdpr`, `anchor_uc_fp_removed`, `cs095_uc_d_email_recovery_misroute`, UC-J
scam/trust-safety precedence (`cs38s01`), UC-I payment, UC-G GDPR, explicit-human-request
(genuine request still → `user_requested`), genuinely-unresolved escalate-after-help
(escalation still occurs). The core safety property the COMPLETE path would have relied on
is independently confirmed and recorded for the future migration: any honest bot-initiated
reason is necessarily **low-priority** (Tier-3+; e.g. `service_degraded`=30), so it can
never displace the high-priority safety/dispute reasons (priorities 0–16) in
`EscalationReasonResolver.resolve()` — steering the LLM off `user_requested` (priority 1)
can only *reduce* false Tier-0 reasons. (This property is necessary but, per §6, not
sufficient for honesty — which is why low priority alone did not rescue a COMPLETE.)

## §7 Frozen-surface compliance (§3 of the objective)

No frozen surface was touched: phase-machine / `PhaseEvaluator` / `ResolveDispositionEvaluator`
/ premature-resolve guard / RESOLVE→CONFIRM / `BotTurn.sourceIds` / `record_outcome` /
`isResolvedSuccessTerminal` / max-turn / handover eligibility / `EscalationReasonResolver`
(precedence + `canonicalize` + `resolveMaxStepsReason` + budget-family) /
`detectExplicitUserEscalation` — all byte-unchanged. No new enum value, no user-message
keyword/regex/content heuristic, no CaseSpec ID / fixed utterance / ad ID / benchmark branch.

## §8 Required explicit records (§10 of the objective)

- **WP1 improves reason-label honesty only** — it does not change whether or when handover
  occurs, nor any phase transition or outcome. (It changed nothing at all: BLOCKED.)
- **WP1 does NOT solve the M-Auto-9 PRIMARY closure / product-contract question** (the
  unsatisfiable-persona problem; making `record_outcome(resolve)` land). Untouched.
- **WP0** (`source_ids` / promotion-evidence) and **WP2** (CONFIRM record-vs-handover on
  satisfiable flows) **remain HELD** under charter §6 conditions.

## §9 Acceptance-gate disposition (§9 of the objective)

Two terminal outcomes only; this is **BLOCKED**, so the COMPLETE gates are explicitly
**NOT claimed**:
- [x] Defect characterized (§5.1) before any edit; three-class attribution recorded (§2).
- [n/a] Tests added (§5.2) — no code shipped.
- [n/a] Java suite re-measure (§5.3) — no code shipped ⇒ no new regression by construction.
- [n/a] Zero-LLM replay (§5.4) — no wiring exists to replay.
- [x] No frozen surface touched; no new enum value; no user-message heuristic (§7).
- [x] §4.1 Codex `pass` — **N/A**: no semantic-surface change shipped (not triggered;
      docs-only handoff is §4-exempt).
- [x] Existing-reason honesty gate resolved: **no existing value is honest → WP1 BLOCKED +
      vocabulary-gap surfaced**; NOT declared done; no knowingly-inaccurate catch-all
      shipped.
- [x] Handoff records the three-class attribution, the honesty analysis + why no existing
      enum value is honest, the BLOCK, and the §10 restatements.

## §10 Recommendation to deliver-agent / human

1. **Close Sprint 095 / S-Auto-43 (WP1) as BLOCKED.** Archive this contract per §11
   (`docs/sprint_objective.md` → `docs/sprints/sprint-095-objective.md`); update
   `action_bank.md` §5 `R-oq-s93.1-confirm-record-vs-handover` to record WP1 = BLOCKED
   (vocabulary gap), WP0/WP2 still HELD. (Deliver-agent close action, per §11 — not done
   in this dev handoff.)
2. **The honest fix requires opening the `D-new-escalation-reason-enum` migration** as its
   own objective (a dedicated low-priority bot-initiated "exhausted-resolution / needs-human"
   reason, wired across resolver + projection + eval `EscalationTrigger` + scoring +
   `confirm.yaml`, per the §4 scoping note). Until then, the bot-initiated `user_requested`
   mislabel cannot be honestly corrected — and `confirm.yaml`'s `user_dissatisfied`→
   `service_degraded` path remains the pre-existing dishonest stand-in.
3. **M-Auto-9 PRIMARY closure / product-contract question is unchanged** by this result; the
   WP1 BLOCK does not bear on whether escalate-after-genuine-help is the correct terminal
   (still the human/product question from OQ-S93.1 §3.5 / the 2026-06-18 acceptance review).

## §11 Evidence and closeout artifacts

- Primary: `autoloop/results/runs/exp-90/eval-results.json` (read-only; per-attempt
  `escalation_reason` distribution + representative `per_turn_trace` handover summaries +
  `transcript` user turns).
- Corroboration: OQ-S93.1 brief `docs/diagnostics/failure-briefs/oq-s93.1-confirm-record-vs-handover.md`
  §3.2/§3.3.
- Reason semantics: `EscalationReasonResolver.java:51-113,256-285`;
  `ContextProjectionBuilder.java:244-321`; `RequestHandoverTool.java:60,87,102`;
  `server/src/main/resources/skills/confirm.yaml:20`; `customer_service_tool_spec_v0_3.md:126-148`;
  `docs/foundational/phase2_domain_realization_spec.md:564,582,1301`;
  `docs/foundational/phase5_evaluation_design.md:308,1979,2009`.
- Deferred item: `docs/action_bank.md` §4 `D-new-escalation-reason-enum`.
- No code/data/eval artifacts committed (none changed).
