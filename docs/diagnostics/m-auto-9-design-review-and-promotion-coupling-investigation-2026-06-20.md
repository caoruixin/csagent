---
title: "M-Auto-9 design review (APPROVE_WITH_REQUIRED_CHANGES) + read-only promotion-coupling investigation"
doc_tier: diagnostic
status: diagnostic
implementation_status: not_started
source_of_truth: >
  This file records (a) the design-review verdict for the M-Auto-9 charter and
  (b) a read-only investigation over a single coherent trace set
  (autoloop/results/runs/exp-90/eval-results.json, n=13 per PRIMARY). Runtime /
  eval code paths cited inline (read-only). No code / runtime / eval / baseline /
  canonical / milestone-doc behavior was changed.
last_reviewed: 2026-06-20
review_cadence: ad hoc
supersedes: []
superseded_by: null
notes: >
  Design-only review finding. The verdict is APPROVE_WITH_REQUIRED_CHANGES;
  implementation is NOT approved. The read-only investigation was run to CONFIRM
  (not assume) the review's pre-CONFIRM source_ids hypothesis for `no_ad_id`. It
  DOWNGRADES that hypothesis: the source_ids→promotion coupling is a confirmed
  LATENT code defect, but on the exp-90 evidence it is NOT the operative blocker —
  the dominant mechanism for BOTH PRIMARY is the unsatisfiable-persona / product
  question. The M-Auto-9 charter is returned to bounded design revision on this
  evidence. WP0 (source_ids/promotion) stays HELD; WP1 (escalation-reason honesty)
  is the intended first dev sub-sprint after the revised charter is approved and
  does NOT itself solve the PRIMARY closure blocker.
---

# M-Auto-9 design review + read-only promotion-coupling investigation (2026-06-20)

## 0. Verdict (recorded)

**`APPROVE_WITH_REQUIRED_CHANGES`. Implementation is NOT approved.** The M-Auto-9
runtime-closure charter
(`docs/proposals/runtime-closure-record-outcome-design-milestone-oq-s93.1.md`) is
a sound *design* charter (design-only, correct fences, honors the holds) but has
one material causal gap and several scoping/validation gaps. It is **returned to
bounded design revision**; no dev sub-sprint is scoped and no WP (WP0/WP1/WP2) is
started by this document.

This finding supersedes nothing; it is the review record + the confirmation
investigation the review demanded before any implementation boundary is drawn.

## 1. Why a read-only investigation was required first

The design review raised a high-confidence hypothesis: `no_ad_id` may be blocked
**before** CONFIRM because a UC-A answer grounded in *listing state* (via
`get_customer_context`) does not populate the `BotTurn.sourceIds` evidence that the
Sprint-093 structural RESOLVE→CONFIRM promotion requires. The human direction was
explicit: treat this as a hypothesis **requiring confirmation, not an
already-approved runtime fix.** This section records the confirmation pass over a
single coherent trace set.

**Trace set used (single, coherent):** `autoloop/results/runs/exp-90/eval-results.json`
(`suites.bad_cases.results.case_results`), the real exp-90 iteration, n=13 attempts
for each PRIMARY (`cs_uc_a_no_ad_id_ad_specific`, `cs_uc_a_loaded_listing`). The
exp-90 proposal edited only `resolve_faq.$.grounding_instruction` (soft teaching
text); it did **not** change the runtime promotion machinery or the source-id
contract, so exp-90 traces are valid for studying runtime promotion behavior. The
Sprint-093 bounded run (OQ-S93.1, 42 draws) is cross-referenced but each causal
claim below is sourced to exp-90 to keep one coherent set per attribution.

## 2. Investigation result — the five questions

### 2.1 Item 4 (the runtime grounding-evidence contract) — CONFIRMED (from code, read-only)

How each tool contributes — or does not — to `BotTurn.sourceIds` (the
grounding-evidence field the promotion reads):

- **`search_knowledge`** — the ONLY contributor. Per-turn `sourceIds` is collected
  exclusively from successful `search_knowledge` hits
  (`ControlKernel.java:2289`). The durable `articlesShown` fallback is also
  populated only from `search_knowledge` hits
  (`ControlKernel.java:2509-2519`, `PhaseEvaluator.java:1106-1119`).
- **`get_customer_context`** (listing state: status / category / price / location /
  posted_date) — contributes **nothing** to `BotTurn.sourceIds`.
- **`resolve_article`** — returns a `source_id` in its result, but that id is
  **not** harvested into `BotTurn.sourceIds` (the collection loop keys on
  `search_knowledge` only). Confirmed in the exp-90 trace: the representative
  `no_ad_id` turn ran `resolve_article` returning `source_id=ka4P200000004ZtIAI`,
  yet that path is not what feeds the promotion field.

Consequence (latent): `BotTurn.sourceIds` →
`PhaseEvaluator.priorGroundedResolveAnswerDelivered` (`PhaseEvaluator.java:799-810`)
→ `session.priorGroundedResolveAnswer` (`ControlKernel.java:460-461`) → the
Sprint-093 `ANSWERED_SUBTASK + priorGroundedResolveAnswer → CONFIRM` promotion
(`PhaseEvaluator.java:921-927`). A grounded answer carried **only** by listing
state and/or `resolve_article`, with a `search_knowledge` **miss**, would leave
`sourceIds` empty → `priorGroundedResolveAnswerDelivered=false` → **the structural
promotion cannot fire.** This coupling is real and is a confirmed latent defect.

### 2.2 Items 1–3 and 5 (the operative `no_ad_id` mechanism) — NOT confirmed; partially refuted

On the exp-90 evidence the latent coupling is **not** the operative blocker:

1. **Per-turn `sourceIds` empty?** — NO in the observed draw. The representative
   `no_ad_id` RESOLVE turn shows `search_knowledge` **HIT**: `faq_miss=false`,
   2 hits (`ka4P200000004ZtIAI`, `ka4P2000000060bIAA`). So `sourceIds` was
   populated and the empty-evidence path was not exercised.
2. **`priorGroundedResolveAnswerDelivered` false?** — Not in this draw; with a
   non-empty `sourceIds` the precondition is satisfiable.
3. **Promotion consequently doesn't fire?** — Not demonstrable as the cause here.
   The eval `per_turn_trace` is truncated (2 entries vs `total_turns=4`), so the
   CONFIRM transition itself is not directly observable; but the absence of a
   stall is: `no_ad_id` has **zero** `max_turns_exceeded` across 13 attempts (a
   pre-CONFIRM RESOLVE loop would surface as `loop_detected` / `max_turns`, which
   it does not).
5. **`goal_impossible` follows from the failed promotion?** — NO. `goal_impossible`
   is the **simulator persona** declaring the goal unsolvable
   (`simulator/session_runner.py:271-272`, set when the user-simulator returns
   `goal_status:"impossible"`, `simulator/user_simulator.py:70`). It is a
   simulator-side stop reason the runtime never observes
   (`scoring/hard_checks.py:787-800`), not a runtime/bot escalation and not a
   phase-machine deadlock.

**Dominant observed mechanism (both PRIMARY = the unsatisfiable persona / product
question):**

| | `no_ad_id` (n=13) | `loaded_listing` (n=13) |
|---|---|---|
| terminal mix | `goal_impossible` ×10, `bot_ended` ×3 | `bot_ended` ×7 (mostly `clarification_budget_exhausted`; 1 `user_requested`; 1 `service_degraded`), `goal_impossible` ×3, `max_turns_exceeded` ×2, `goal_achieved` ×1 |
| escalation on dominant | empty (`goal_impossible` is not a bot escalation) | budget-family + 1 `user_requested` mislabel |
| persona signal | `working`→`unresolved_after_help`/`goal_status=impossible` after ONE grounded answer | `unresolved_after_help` across 3 turns → `impossible` |
| tier-2 advisory | `record-outcome-on-grounded-answer` (RESOLVE-stage) | `record-outcome-on-confirmed-resolve` (CONFIRM-stage) |
| grounding in the rep draw | `get_customer_context` listing + `search_knowledge` HIT + `resolve_article` → coherent grounded reply ("AD-1001 is live and active … may take time to index … search exact keywords") | superficial listing use + frequent UC-A→UC-B misclass (per exp-90 §3.2) |

The `no_ad_id` persona flips to `goal_status=impossible` after a **reasonable**
grounded answer; the bot did not stall and did not escalate. This is the
"hard-to-satisfy persona rather than a bot fault" reading the eval authors already
flagged for `goal_impossible` (`hard_checks.py:787-800`), and "whether
`resolved+goal_impossible` should itself be a hard fail is left as an `eval_spec`
open question."

### 2.3 Net

- The **source_ids→promotion coupling is a CONFIRMED latent code defect** (§2.1),
  but it is **NOT shown to be the operative `no_ad_id` blocker** in exp-90 (§2.2).
  It would bite only on a `search_knowledge`-**miss** `no_ad_id` draw (listing-state
  -only grounding). Whether any such draw exists across the suite is **unconfirmed**
  — the eval JSON does not carry per-attempt tool data, so the search-miss path
  could not be enumerated here.
- The **observed dominant blocker for BOTH PRIMARY is the unsatisfiable persona /
  product question** (is escalate-after-help the correct terminal? should
  `resolved+goal_impossible` hard-fail?), distinguished by persona pacing:
  `no_ad_id` terminates fast as `goal_impossible`; `loaded_listing` drags into
  budget-family escalations plus the `user_requested` mislabel.
- The review's pre-CONFIRM source_ids hypothesis is therefore **downgraded**: real
  in code, not demonstrated as the critical-path closure mechanism. Fixing the
  coupling would not make `no_ad_id` pass while the persona remains unsatisfiable.

## 3. Required charter changes (folded into the revised charter)

1. **Separate `no_ad_id` from `loaded_listing` on the correct axis** — persona
   pacing / terminal shape (fast `goal_impossible` vs dragged budget-escalation +
   `user_requested` mislabel), NOT on the source_ids axis.
2. **One coherent evidence set per attribution** — exp-90 for both PRIMARY here;
   any Sprint-093 cross-reference is labeled as such.
3. **Reclassify `source_ids`** — retract the unconditional "non-blocking
   observability" label AND do not promote it to "the closure blocker." It is a
   **confirmed latent code coupling** (`get_customer_context` / `resolve_article`
   do not feed the grounding-evidence contract) that is **not the observed
   operative blocker**; HELD as a robustness item (WP0) pending confirmation that a
   `search_knowledge`-miss `no_ad_id` draw is actually blocked by it.
4. **"No phase-machine change" is a first-class valid design outcome** —
   strengthened: both PRIMARY are dominated by the product question, so the
   milestone may legitimately conclude with WP1 (label honesty) + a recorded
   eval_spec/product decision + a satisfiable companion, and **no** closure-forcing
   runtime edit.
5. **Define the satisfiable companion persona** — required; the two existing
   PRIMARY personas are unsatisfiable-by-construction (`goal_status=impossible`),
   so record→CLOSE cannot be demonstrated on them. Specify authoring, human-blessed
   ground truth, and shadow/visible status.
6. **Specify a V3 noise-aware success rule** — given the S-Y1.7 small-n volatility
   finding, the bounded real-LLM gate must state the V3-gate decision rule, not a
   raw pass-count.
7. **Name standing regression guards** — the exp-90-regressed `anchor_uc_g_gdpr`,
   `anchor_uc_fp_removed`, and `cs095_uc_d_email_recovery_misroute` cases are named
   precedence/blast-radius guards in the validation plan.

## 4. Boundary recorded (not scoped, not started)

- **WP0 (source_ids / promotion-evidence) — HELD.** Confirmed latent, not confirmed
  operative; revisit only after a search-miss `no_ad_id` draw is shown to be blocked
  by it, and only with full §4/§5 phase-machine protections + human sign-off.
- **WP1 (escalation-reason honesty) — intended first dev sub-sprint AFTER the
  revised charter is approved.** `prompt_projection` / `semantic_planner`;
  `confirm.yaml` + projection escalation-reason vocabulary so the LLM reserves
  `user_requested` for an actual user request. Present in the data
  (`loaded_listing` attempt 3, `no_ad_id` attempt 6). **It improves reason-label
  correctness; it does NOT by itself solve the PRIMARY closure blocker** (the
  unsatisfiable-persona / product question).
- **WP2 (record-vs-handover on satisfiable flows) — HELD**, gated on the satisfiable
  companion persona.

## 5. Constraints honored

Read-only investigation: no code / runtime / eval / CaseSpec / baseline / canonical
pointer changed; no new LLM run. No WP started. The holds remain in force (no
exp-91, no mutable-surface change, no re-bless, no baseline move, no
canonical-pointer change; exp-82 stays WITHDRAWN). Implementation remains
unapproved; the next step is the bounded charter revision recorded here, then human
review/approval before any dev sub-sprint.

**Evidence pointers:** `autoloop/results/runs/exp-90/eval-results.json`
(`suites.bad_cases.results.case_results[] | select(.case_id=="cs_uc_a_no_ad_id_ad_specific" / "cs_uc_a_loaded_listing")`:
`attempts[].failure_reason`/`escalation_reason`, `per_turn_trace[].tool_calls`,
`user_state_signals`); runtime `PhaseEvaluator.java:799-810,921-927,1106-1119`,
`ControlKernel.java:460-461,2289,2453-2466,2509-2519`,
`ResolveDispositionEvaluator.java:133-177`; eval
`simulator/session_runner.py:271-272`, `simulator/user_simulator.py:70`,
`scoring/hard_checks.py:787-800,836`.
