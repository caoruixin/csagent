---
title: "M-Auto-9 — Runtime closure design milestone (record_outcome / RESOLVE→CONFIRM, OQ-S93.1)"
doc_tier: proposal
status: proposal
implementation_status: not_started
source_of_truth: this file (design charter); runtime code paths cited inline
last_reviewed: 2026-06-20
review_cadence: per milestone
supersedes: []
superseded_by: null
notes: >
  Charter for M-Auto-9, a separate, narrowly-scoped runtime/orchestration DESIGN
  milestone opened (and formally numbered) when the M-Auto-7 registry-only
  autoloop pilot was closed NO-KEEP (control-surface limit reached for the
  current PRIMARY objective; see
  docs/diagnostics/exp-90-real-result-and-primary-control-surface-2026-06-19.md).
  This is a DESIGN charter only — it scopes the design work + required coverage;
  it authorizes NO runtime/eval/prompt/baseline change, and design review/approval
  is the gate before any implementation sub-sprint. Builds on the existing
  OQ-S93.1 research (action_bank R-oq-s93.1-confirm-record-vs-handover +
  docs/diagnostics/failure-briefs/oq-s93.1-confirm-record-vs-handover.md); it does
  not redo it. ID assignment: M-Auto-8 is already the human-confirmed (2026-06-18)
  "primary-first staged eval + adaptive sampling" milestone, so this milestone
  takes the next unambiguous id, M-Auto-9.
  2026-06-20 DESIGN REVIEW — initial verdict APPROVE_WITH_REQUIRED_CHANGES;
  required changes folded into the bounded revision; the revised charter received
  FINAL verdict APPROVE (design complete; implementation NOT started; WP1 sub-sprint
  scoping unblocked; WP0/WP2 remain HELD). A read-only confirmation
  investigation over a single coherent trace set (exp-90, n=13/PRIMARY;
  docs/diagnostics/m-auto-9-design-review-and-promotion-coupling-investigation-2026-06-20.md)
  DOWNGRADED the review's pre-CONFIRM source_ids hypothesis: the
  source_ids→priorGroundedResolveAnswerDelivered coupling is a CONFIRMED latent code
  defect (only search_knowledge feeds BotTurn.sourceIds; get_customer_context +
  resolve_article do not) but is NOT the observed operative blocker — the dominant
  mechanism for BOTH PRIMARY is the unsatisfiable-persona / product question
  (no_ad_id terminates fast as simulator-driven goal_impossible; loaded_listing
  drags into budget-family escalation + the user_requested mislabel). §1–§3 and the
  source_ids companion item below are revised on that evidence. WP0
  (source_ids/promotion) stays HELD; WP1 (escalation-reason honesty) is the intended
  first dev sub-sprint after this revised charter is approved and does NOT itself
  solve the closure blocker; WP2 stays HELD pending a satisfiable companion persona.
---

# M-Auto-9 — Runtime closure design milestone (`record_outcome` / RESOLVE→CONFIRM, OQ-S93.1)

## Milestone identity & ID-conflict resolution

- **Formal id: M-Auto-9.** Assigned under the existing M-Auto-N numbering
  discipline (latest live milestone is M-Auto-7; M-Auto-8 is reserved).
- **ID-conflict resolved:** `M-Auto-8` is **already associated** with the
  "primary-first staged eval + adaptive sampling" theme — human-confirmed on
  2026-06-18 (`docs/10-handoff.md` §0; `docs/milestone_objective.md` notes; the
  staged-eval proposal `docs/proposals/m_auto_7_objective_alignment_and_staged_eval.md`).
  An earlier (2026-06-08) solution note
  (`docs/solutions/2026-06-08-record-outcome-guardrail-deadlock.md`) had
  *tentatively* floated "M-Auto-8 (Phase-advance contract honesty)" for this
  record_outcome/phase-advance work, but that was superseded by the 2026-06-18
  human boundary. To avoid the collision, this runtime-closure milestone takes
  **M-Auto-9** (no prior M-Auto-9 reference exists anywhere in the live docs).
- **Status: APPROVED (design) 2026-06-20 — `APPROVE`. Design complete;
  implementation NOT started.** The initial design review (2026-06-20) returned
  `APPROVE_WITH_REQUIRED_CHANGES`; all required changes were folded into the bounded
  revision and the revised charter received FINAL verdict `APPROVE`. This charter is
  the approved successor-milestone north star; it is design-complete but not
  launched. **Next action = deliver-agent scopes the first WP1 dev sub-sprint
  (scoping only, not implementation). WP0 and WP2 remain HELD under the §6
  conditions.**

## Design-review status (2026-06-20)

- **FINAL verdict: `APPROVE` (2026-06-20). Charter design complete; implementation
  NOT started.** The initial design review returned `APPROVE_WITH_REQUIRED_CHANGES`;
  all required changes were folded into the bounded charter revision, and the final
  human-approval review of the revised charter (review commit `c71e620b`) confirmed
  all six assessment criteria and issued `APPROVE`. **WP1 sub-sprint scoping is now
  unblocked (scoping only, not implementation); WP0 and WP2 remain HELD under the §6
  conditions.**
- **Initial verdict (superseded by the `APPROVE` above): `APPROVE_WITH_REQUIRED_CHANGES`.**
  Review + the read-only confirmation investigation are recorded in
  [`../diagnostics/m-auto-9-design-review-and-promotion-coupling-investigation-2026-06-20.md`](../diagnostics/m-auto-9-design-review-and-promotion-coupling-investigation-2026-06-20.md).
- **Key revision driver:** the review's high-confidence hypothesis — that `no_ad_id`
  is blocked *before* CONFIRM because listing-state grounding does not populate
  `BotTurn.sourceIds` — was tested against a single coherent trace set (exp-90,
  n=13/PRIMARY) and **downgraded**. The `source_ids → priorGroundedResolveAnswerDelivered`
  coupling is a **confirmed latent code defect** (`get_customer_context` and
  `resolve_article` do not feed the grounding-evidence contract; only
  `search_knowledge` does) but is **not the observed operative blocker**: in the
  representative `no_ad_id` draw `search_knowledge` HIT, `sourceIds` was populated,
  and the case still failed because the **simulator persona declared
  `goal_status=impossible`** after a reasonable grounded answer.
- **Consequence for scope:** both PRIMARY are dominated by the **unsatisfiable-persona
  / product question** (is escalate-after-help the correct terminal? should
  `resolved+goal_impossible` hard-fail?). "No phase-machine change" is now a
  first-class valid design outcome. WP0 (source_ids/promotion) is **HELD** as a
  latent-robustness item; WP1 (escalation-reason honesty) is the intended first dev
  sub-sprint after approval and **does not itself solve the closure blocker**.

## Why this milestone exists

The M-Auto-7 registry-only pilot is CLOSED NO-KEEP. exp-90 (the byte-identical
frozen `$.grounding_instruction` proposal, run through the real pipeline)
established that **neither `cs_uc_a_no_ad_id_ad_specific` nor
`cs_uc_a_loaded_listing` reaches a recorded SATISFIED+resolve terminal even when
grounding is correct**, so no registry-only edit can make the objective
keep-eligible. Per the 2026-06-20 read-only confirmation investigation, the
observed outcomes are **dominated by the unsatisfiable-persona / product-contract
question** — the simulator persona declares `goal_status=impossible` after a
reasonable grounded answer, and the correct terminal (resolve vs
escalate-after-help vs whether `resolved+goal_impossible` should hard-fail) is an
open product / `eval_spec` decision — **not a proven shared phase-machine closure
defect**. The `source_ids → promotion` coupling is a confirmed *latent* code defect
but was not the operative blocker on exp-90 (§2). The grounding rejection is of the
*reviewed* hypothesis, not a proof that all registry wordings are impossible — but
because neither PRIMARY reaches a recorded resolve, further registry search is moot
for the end-to-end objective.

This closure question is already partly characterised: Sprint 093 / S-Auto-39 repaired the
*structural* RESOLVE→CONFIRM phase transition (`R-resolve-confirm-transition-deadlock`,
CLOSED), and the OQ-S93.1 read-only research (`R-oq-s93.1-confirm-record-vs-handover`;
`docs/diagnostics/failure-briefs/oq-s93.1-confirm-record-vs-handover.md`) found
the **residual** layer: `record_outcome(resolve)` lands 0/42 even in CONFIRM, and
the PRIMARY escalations split as **2 genuine user-accepted + 6 `user_requested`
mislabel (LLM-supplied) + 5 budget-family runtime**. This milestone turns that
research into a scoped design.

## Scope discipline (binding)

- **Design-only.** Produces a Failure-Brief-grounded design + the smallest-change
  proposal + a validation plan. Authorizes NO runtime / eval / prompt / CaseSpec /
  baseline change until the design is reviewed and a dev sub-sprint is scoped.
- **Inherits the OQ-S93.1 fix fences:** do NOT relax the premature-resolve guard;
  do NOT raise `max_turns`; do NOT lower the `record_outcome` requirement; do NOT
  add a CaseSpec/PRIMARY exception; no user-message-content keyword heuristic.
- Touches FROZEN runtime surfaces (`ResolveDispositionEvaluator`,
  `isResolvedSuccessTerminal`, the premature-resolve guard) — any change needs the
  §3.2 anti-hardcode + cross-UC protections below and human sign-off.

## Required design coverage

### 1. Why grounded help does not reliably transition through `record_outcome` / RESOLVE→CONFIRM

Sprint 093 fixed the *structural* path (a grounded RESOLVE answer can now be
promoted to CONFIRM via `PhaseEvaluator.mapFinalAnswer`, `PhaseEvaluator.java`
~592-604/~860-927). The residual is **not one diagnosis**, and — per the 2026-06-20
read-only investigation — the dominant mechanism for **both** PRIMARY is the
**unsatisfiable persona / product question**, not a single "record_outcome 0/N in
CONFIRM" defect. Each attribution below is sourced to **one coherent set**, exp-90
(n=13/PRIMARY); the OQ-S93.1 Sprint-093 run (42 draws) is corroborating, labeled
as such.

- **`no_ad_id` — fast simulator-driven `goal_impossible`, not a pre-CONFIRM stall.**
  The bot grounds adequately (`get_customer_context` listing state + a
  `search_knowledge` HIT + `resolve_article` → a coherent grounded reply), and the
  **simulator persona then declares `goal_status=impossible`** after one reasonable
  answer (`session_runner.py:271-272` ← `user_simulator` `goal_status:"impossible"`).
  Terminal mix: `goal_impossible` ×10, `bot_ended` ×3; escalation_reason empty on
  the `goal_impossible` draws; **zero `max_turns_exceeded`** (so it is not a RESOLVE
  loop). `goal_impossible` is a simulator-side stop reason the runtime never
  observes and which the eval already treats as ambiguous ground truth
  (`hard_checks.py:787-800`). This is the "hard-to-satisfy persona rather than a bot
  fault" reading — a **product / `eval_spec` question**, not a confirmed phase-machine
  defect.
- **`loaded_listing` — CONFIRM record-vs-handover + budget escalation + the
  `user_requested` mislabel.** The persona stays `unresolved_after_help` longer (3
  turns) and the bot exhausts budget: terminal mix `bot_ended` ×7 (mostly
  `clarification_budget_exhausted`; 1 `user_requested` mislabel; 1
  `service_degraded`), `goal_impossible` ×3, `max_turns_exceeded` ×2,
  `goal_achieved` ×1. Compounded by frequent UC-A→UC-B misclass (exp-90 §3.2). Its
  tier-2 advisory is the CONFIRM-stage `record-outcome-on-confirmed-resolve` (vs
  `no_ad_id`'s RESOLVE-stage `record-outcome-on-grounded-answer`).

**Do not collapse these**, and **do not separate them on the source_ids axis** (the
review's first hypothesis): the distinction is **persona pacing / terminal shape**,
not a sourceIds-driven pre-CONFIRM block. The one clear actionable defect across both
is the **`user_requested` mislabel** where the bot self-initiates handover but stamps
a user-requested reason (LLM-supplied in `request_handover` args — NOT a runtime
`resolveMaxStepsReason` mis-stamp). The latent `source_ids` coupling (§2) is real in
code but unconfirmed as operative; see the revised companion item.

### 2. The `PhaseEvaluator` / `ResolveDispositionEvaluator` / premature-resolve-guard interaction

Map the post-loop interpreter and disposition logic as the design's foundation:
- `PhaseEvaluator.interpretRunResult` / `mapFinalAnswer` — RESOLVE→CONFIRM
  promotion preconditions (`priorGroundedResolveAnswer`,
  `recordOutcomeRejectedOnlyByPrematureGuard`), CONFIRM→CLOSE, escalate paths.
- `ResolveDispositionEvaluator.shouldRejectPrematureResolveOutcome`
  (`ResolveDispositionEvaluator.java:160`) — rejects `record_outcome(resolve)`
  outside CONFIRM/CLOSE; `READY_TO_CONFIRM` requires a successful `record_outcome`
  this run. The design must state exactly which of these gates the 0/42 and
  whether the smallest fix is in projection/planner guidance (so the LLM records
  in CONFIRM), in the disposition/transition logic, or in the reason-resolver.
- Confirm the `user_requested` label is LLM-supplied (planner/projection), not a
  runtime stamp, so the WP1 fix layer is `prompt_projection`/`semantic_planner`,
  not the guard.
- **The grounding-evidence precondition (verified latent, 2026-06-20).** The
  Sprint-093 promotion fires only when `priorGroundedResolveAnswer` is true
  (`PhaseEvaluator.java:921-927`, stashed at `ControlKernel.java:460-461`), which
  `priorGroundedResolveAnswerDelivered` (`PhaseEvaluator.java:799-810`) derives from
  a prior RESOLVE `BotTurn` with non-empty `getSourceIds()`. `BotTurn.sourceIds` is
  harvested **only from `search_knowledge` hits** (`ControlKernel.java:2289`; the
  `articlesShown` fallback at `ControlKernel.java:2509-2519` /
  `PhaseEvaluator.java:1106-1119` is also search-only). `get_customer_context`
  (listing state) and `resolve_article` (despite returning a `source_id`) do **not**
  contribute. So a UC-A answer grounded only in listing state with a
  `search_knowledge` **miss** would leave `sourceIds` empty → the promotion cannot
  fire. This is a **confirmed latent defect** but, on exp-90, **not** the operative
  `no_ad_id` blocker (the representative draw had a `search_knowledge` HIT and still
  failed via persona `goal_impossible`). The design must **confirm whether any
  `search_knowledge`-miss `no_ad_id` draw is actually blocked by this coupling**
  before treating it as a closure lever (WP0); if none exists, WP0 is
  latent-robustness hardening, not a PRIMARY closure fix.

### 3. Required trace-level behavior for each PRIMARY (the design target)

- `no_ad_id`: request reference → load listing (`get_customer_context`) →
  grounded answer on listing state → **the correct terminal depends on the product
  decision.** exp-90 shows the ask+ground half already works (search HIT + listing
  state + coherent reply), but the **simulator persona declares
  `goal_status=impossible`** after a reasonable answer. The target is therefore NOT
  "force RESOLVE→CONFIRM on this persona": it is (a) record RESOLVE→CONFIRM **only
  when genuinely satisfied** (demonstrated on the satisfiable companion, §6), and
  (b) **escalate-after-help** (the WP1-A conditional path) when the helped user
  remains positively unresolved — never a forced false-resolve, never a relaxed
  guard. Whether `resolved+goal_impossible` should hard-fail is the open `eval_spec`
  question to record (`hard_checks.py:799`).
- `loaded_listing`: **retain UC-A** (do not misclassify to UC-B) →
  **substantively use listing fields** (status/category/price/location/posted_date,
  not generic FAQ) → targeted resolution → **record RESOLVE → CONFIRM when
  satisfied, else escalate-after-help**. (exp-90: classify fails 8/13; grounding
  superficial; budget-family escalation + the `user_requested` mislabel dominate.)
- For both: a recorded resolve is earned **only when genuinely satisfied**; when the
  helped user remains positively UNRESOLVED, escalate-after-help (WP1-A conditional
  path) is the correct terminal, NOT a forced false-resolve. Because the two
  existing personas are unsatisfiable-by-construction, the resolvable path can only
  be demonstrated on the **satisfiable companion persona** (§6).

### 4. Precedence preservation (must not regress)

The design must preserve, with explicit checks: trust-and-safety / scam-seller
escalation (UC-J, e.g. `cs38s01`), payment (UC-I), GDPR (UC-G), explicit
human-request escalation, and **genuinely-unresolved escalation-after-help**. No
change may convert a legitimate escalation into a forced resolve, or suppress an
escalation the user/safety floor requires. The premature-resolve guard and
`escalation_policy` precedence stay intact.

### 5. Anti-hardcode + cross-UC regression protections

- Per §1.5/§1.7 + the §4.1 kernel: no keyword/regex/case-id/free-text/per-UC
  matrix to force `record_outcome`; no user-message-content heuristic for the
  `user_requested` mislabel. The fix must be a generalizable runtime/projection
  change, not a benchmark-shaped patch.
- Cross-UC blast-radius: the change is in the shared phase machine / projection,
  so it reaches all UCs. Require a no-regression check across the
  bad_cases / anchor_outcome / shadow suites (the exact suites the V3 gate reads),
  with the safety/grounding floors held — the same bars exp-90 was scored against.
- **Standing regression guards (named, from exp-90's demonstrated blast radius):**
  `anchor_uc_g_gdpr` (0.636→0.20), `anchor_uc_fp_removed` (0.70→0.20), and
  `cs095_uc_d_email_recovery_misroute` (0.818→0.0) regressed under the exp-90
  soft-field edit — the shared-surface blast radius is **not** hypothetical. These
  three are named guards the validation must hold green. Note any WP0-style
  broadening of the promotion precondition lets *more* flows reach CONFIRM, so it
  must additionally verify no safety/escalation case (UC-J `cs38s01`, UC-I, UC-G) is
  promoted into a forced/false resolve; the premature guard and `escalation_policy`
  precedence stay intact.

### 6. Smallest *legitimate* change + validation plan (the deliverable)

The milestone's output is **the smallest legitimate change** that delivers the §3
trace behavior without violating §2/§4/§5 — **which may be projection/label-only
plus a recorded product/`eval_spec` decision, with NO phase-machine change.**

- **"No phase-machine change" is a first-class valid outcome.** The 2026-06-20
  evidence is that both PRIMARY are dominated by the unsatisfiable-persona /
  product question, not a proven closure defect. A legitimate design conclusion is:
  **WP1 (escalation-reason honesty) + a recorded product decision** (escalate
  -after-genuine-help is a valid terminal for the positively-unresolved, already
  the 2026-06-19 direction encoded in WP1-A's conditional acceptance) + **a
  satisfiable companion** to prove resolve *can* land — and **no** closure-forcing
  edit to the frozen phase machine. The design must NOT manufacture a runtime change
  to "make resolve land" on personas that are unsatisfiable by construction.
- **Unit/integration (Java):** characterization tests pinning a
  RESOLVE→CONFIRM→record-resolve path for a *satisfiable* grounded flow (on the
  companion, below); no new regression vs the documented baseline; precedence cases
  (§4) and the §5 named guards green.
- **Satisfiable companion persona (required, specify before WP2).** The two existing
  PRIMARY personas emit `goal_status=impossible` and so cannot exercise record→CLOSE.
  WP2 needs a companion persona that accepts a grounded answer. The design must
  specify: authoring path (eval companion, **not** a CaseSpec widen of the existing
  two), **human-blessed ground truth** (who blesses that the persona is genuinely
  resolvable and that `resolve` is the correct expected outcome), and **visibility
  status** (shadow vs visible) so the dev agent cannot consume a shadow companion
  during development.
- **Real-LLM (the eval gate, §5.7) + V3 noise-aware success rule.** A bounded
  NON-pilot run (2 PRIMARY ×11 + the satisfiable companion + the anti-误杀 control ×5
  + neighbors ×5; §5.9 pre-flight GO; `caffeinate`) showing the companion can reach a
  **recorded SATISFIED+resolve**, with a zero-LLM attribution cross-check
  distinguishing genuine resolve from the `user_requested` mislabel and budget-family
  vectors. **Success is decided under the S-Y1.7 V3 stability-tiered, noise-aware
  rule** (tier0 floor + TIER-S majority-flip anti-误杀 floor + TIER-N
  Beta-Binomial/δ/BH-count), **not a raw pass-count** — the small-n majority-flip
  gate had a ~92–95% false-discard rate, so a raw ×11/×5 count cannot certify a
  closure improvement.
- **Work-package decomposition + sequencing (gates the dev boundary):**
  - **WP1 — `user_requested` escalation-reason honesty (intended FIRST dev
    sub-sprint, after this revised charter is approved).** `prompt_projection` /
    `semantic_planner`; tighten `confirm.yaml` + the projection escalation-reason
    vocabulary so the LLM reserves `user_requested` for an actual user request.
    Low-risk, anti-hardcode-clean, touches no frozen surface. **It improves
    reason-label correctness; it does NOT by itself solve the PRIMARY closure
    blocker** (the unsatisfiable-persona / product question). No real-LLM run is
    required to certify a label change (zero-LLM attribution replay + Java tests).
  - **WP0 — `source_ids` / promotion-evidence — HELD.** Confirmed latent (§2), not
    confirmed operative. Revisit only after a `search_knowledge`-miss `no_ad_id`
    draw is shown to be blocked by it; any change touches the frozen phase machine
    and needs the full §4/§5 protections + human sign-off. Not the first boundary.
  - **WP2 — CONFIRM record-vs-handover on satisfiable flows — HELD**, gated on the
    satisfiable companion above.
  - **Product / `eval_spec` question (record only, no CaseSpec/bar change here):**
    are the two PRIMARY personas resolvable, or is escalate-after-help the correct
    terminal? Should `resolved+goal_impossible` hard-fail (`hard_checks.py:799`)?
    Revisit the bar only via the review/override/re-bless process if the human
    decides to.

## Companion / deferred items (recorded, not in this milestone's critical path)

- **`discover_triage.$.procedure` classification lever — DEFERRED.** The one
  untried registry lever (for `loaded_listing` UC-A→UC-B misclass). Revisit
  **only after** the shared closure path is fixed: improving classification alone
  cannot make the objective keep-eligible while the closure blocker stands.
- **`source_ids` — reclassified (2026-06-20): confirmed latent runtime coupling,
  NOT unconditional observability.** The earlier "non-blocking observability"
  label was derived from eval *scoring* (the PASS conditions read
  `containment_outcome` / the bot `user_message`, not `source_ids`) — that part
  stands for the *score*. But the **same `BotTurn.getSourceIds()` field gates the
  Sprint-093 RESOLVE→CONFIRM promotion** (§2): `get_customer_context` and
  `resolve_article` do not feed it; only `search_knowledge` does. So `source_ids`
  is **not** unconditionally non-blocking. It is a confirmed latent code coupling
  that, on exp-90, is **not** the observed operative blocker (the `no_ad_id`
  representative had a `search_knowledge` HIT). Disposition: **HELD as WP0** — do
  not treat it as a closure lever until a `search_knowledge`-miss `no_ad_id` draw is
  shown to be blocked by it; if none exists it is latent-robustness hardening, not a
  PRIMARY closure fix. The separate *trace-display* `source_ids=[]` rendering remains
  a minor observability item.

## Holds (in force)

No WP1-B, WP2 (until designed + reviewed), objective-alignment annotation,
posterior/bounded-resampling, mutable-surface expansion, re-bless, or baseline
movement is started by this charter. exp-82 stays WITHDRAWN. The canonical
baseline pointer + `docs/current_eval_baseline.md` are frozen.
