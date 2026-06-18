# Sprint 093 / S-Auto-39 — Step 0 escalation attribution (read-only, NO LLM)

> **Filed:** 2026-06-18 by dev agent (S-Auto-39).
> **Inputs:** exp-86 deep per-turn traces (5 main attempts `a0..a4` +
> 8 PRIMARY oversample runs `_primary_oversample/bad_cases_a0..a7` +
> top-level representative) and `m-auto-7-prepilot-baseline-20260608`
> (per-attempt summary only — no per-turn trace retained).
> **Scope:** PRIMARY = UC-A bad-cases (`alice_uc_a_uc_h_misclass`,
> `cs_uc_a_generic_policy_question`, `cs_uc_a_loaded_listing`,
> `cs_uc_a_lookup_failed`, `cs_uc_a_no_ad_id_ad_specific`,
> `wmkb_uc_a_trader_flag_secondary_uc_h`).
> **Method:** deterministic extraction (`/tmp/step0_*.py`, read-only) of
> per-draw phase sequence, `record_outcome` premature-guard hits
> (`progressive_resolve_record_outcome_premature`), grounding evidence
> (`search_knowledge` hit + `resolve_article`), `containment_outcome`,
> `escalation_reason`, `stop_reason`, and L2 `correct_outcome`. NO LLM.

## Classification rule (anti-误杀 gated)

A draw is **deadlock-downstream runtime mis-stamp** iff ALL of:
grounded (search hit + resolve_article) · tripped the premature
`record_outcome` guard ≥1× · ended escalated/exhausted · L2
`correct_outcome == 0.0` (the escalation was scored WRONG) · the case
is not a should-escalate case. A draw whose `correct_outcome == 1.0`
(resolved, OR accepted-escalation as in `wmkb_…_trader_flag`) is
**not** a deadlock victim even if it tripped the guard — the outcome
was credited. Infra timeouts (`stop=timeout`, `turns=0`) and
escalations that never tripped the guard are separated out as their
own classes (the latter is the anti-误杀 boundary: a different shape
the fix must NOT convert).

## exp-86 deep PRIMARY UC-A — per-draw separation (46 draws)

| class | count | meaning |
|---|---|---|
| **DEADLOCK_DOWNSTREAM_MISSTAMP** | **12** | grounded + premature-guard hit + escalated + `co=0.0` → escalation reason mis-attributed |
| **DEADLOCK_STALL_NO_RECORD** | **1** | grounded + premature hit + blank containment, no record (stalled in RESOLVE) |
| OUTCOME_CREDITED | 24 | resolved OR accepted-escalation (`co=1.0`); some only "lucky" — reached `max_turns` and `isResolvedSuccessTerminal` stamped resolved *despite* ≥1 premature hit |
| ESC_NO_GUARD_HIT | 5 | escalated, grounded, but never tripped the guard (clarification-budget loop / genuine `user_requested` / `faq_miss` on re-search) — **anti-误杀 boundary, NOT to be force-converted** |
| INFRA_TIMEOUT | 3 | `stop=timeout`, `turns=0` (backend pressure during the a0/agg run) — infra, not deadlock |
| OTHER | 1 | grounded, blank containment, no guard hit |

**Deadlock-attributable total: 13 / 46 deep draws (28%).** Of the 24
OUTCOME_CREDITED, ≥6 carried `premature > 0` and resolved ONLY because
the session happened to exhaust `max_turns` with a grounded answer
(the `isResolvedSuccessTerminal` ANSWERED_SUBTASK stamp) rather than
via a clean `record_outcome(resolve)` — i.e. the current "success" on
those draws is non-deterministic luck downstream of the same deadlock.

### Representative draws (evidence)

- `alice_uc_a_uc_h_misclass` (a0..a4, 6/6 draws DEADLOCK): grounded "your
  ad was removed for a policy violation; check email/spam" delivered,
  `record_outcome(resolve)` rejected `progressive_resolve_record_outcome_premature`
  (1–3× per draw), phase stays RESOLVE, loop ends `request_handover`
  → `containment=escalated`, reason `user_requested` (a0–a3) /
  `faq_miss_threshold_exceeded` (a4). **The transcript shows the user
  NEVER asked for a human** — `user_requested` is a pure runtime
  mis-stamp downstream of the deadlock.
- `cs_uc_a_no_ad_id_ad_specific` (a2 prem=4, ovs-a1/a4/a6/a7): looked up
  AD-1001, confirmed LIVE, grounded troubleshooting, `record_outcome`
  rejected, looped the same grounded content across 6 turns; persona
  eventually said "please escalate this" (`user_requested`) or re-search
  tripped `faq_miss_threshold_exceeded` / `turn_budget_exhausted`.
  Frustration-escalation **induced by** the deadlock (vs alice's pure
  mis-stamp with no user utterance).

## baseline-20260608 shallow PRIMARY UC-A (per-attempt `escalation_reason`)

No per-turn trace retained in the aggregated file (guard/grounding
depth unavailable); classified by `escalation_reason` shape only:

| bucket | count |
|---|---|
| PASS | 21 |
| NONESC_FAIL `max_turns_exceeded` | 12 |
| NONESC_FAIL `goal_achieved` | 9 |
| **DEADLOCK_SHAPE_ESC `user_requested`** | **8** |
| **DEADLOCK_SHAPE_ESC `faq_miss_threshold_exceeded`** | **5** |
| EXPECTED_ESC_CASE (`wmkb_…_trader_flag`) | 3 |
| OTHER_ESC `appeal_requires_human` | 2 |
| NONESC_FAIL `loop_detected` | 2 |
| DEADLOCK_SHAPE_ESC `clarification_budget_exhausted` | 2 |
| OTHER_ESC `incorrect_deletion_appeal` | 1 |
| DEADLOCK_SHAPE_ESC `turn_budget_exhausted` | 1 |

16 deadlock-shape escalations among PRIMARY draws — consistent with the
deep exp-86 rate. (Shallow: cannot confirm guard hits per draw; the
shape is the same reasons the deep traces attribute to the deadlock.)

## Conclusion (drives the fix)

The dominant PRIMARY failure is a **circular RESOLVE→CONFIRM deadlock**:
`ResolveDispositionEvaluator.evaluate()` only returns `READY_TO_CONFIRM`
when a `record_outcome` already succeeded this run, but
`shouldRejectPrematureResolveOutcome` rejects `record_outcome(resolve)`
outside CONFIRM/CLOSE — so a grounded answer can never legitimately
reach CONFIRM, the loop continues, and the bot eventually
`request_handover`s, **overwriting an earlier transient `resolved`
stamp with `escalated`** (mis-attributed `user_requested` /
`faq_miss_threshold_exceeded` / `turn_budget_exhausted`). The fix must
provide a **structural** RESOLVE→CONFIRM transition (grounded
FINAL_ANSWER on a prior turn + a subsequent user turn) so
`record_outcome(resolve)` can land in CONFIRM — without relaxing the
premature guard. The 5 ESC_NO_GUARD_HIT draws are the anti-误杀
boundary: a different shape (genuine clarification/user-request) the
fix must leave failing/escalating.
