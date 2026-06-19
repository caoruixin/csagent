---
title: "M-Auto-9 — Runtime closure design milestone (record_outcome / RESOLVE→CONFIRM, OQ-S93.1)"
doc_tier: proposal
status: proposal
implementation_status: not_started
source_of_truth: this file (design charter); runtime code paths cited inline
last_reviewed: 2026-06-19
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
- **Status: PROPOSED — awaiting design review/approval.** This charter is the
  successor-milestone north star; it is not launched. No implementation begins as
  part of the documentation fold (next action = design review/approval).

## Why this milestone exists

The M-Auto-7 registry-only pilot is CLOSED NO-KEEP. exp-90 (the byte-identical
frozen `$.grounding_instruction` proposal, run through the real pipeline)
established that the **shared runtime closure blocker independently gates both
PRIMARY cases**: neither `cs_uc_a_no_ad_id_ad_specific` nor
`cs_uc_a_loaded_listing` can reach a recorded SATISFIED+resolve terminal even
when grounding is correct, so no registry-only edit can make the objective
keep-eligible. The grounding rejection is of the *reviewed* hypothesis, not a
proof that all registry wordings are impossible — but the closure blocker makes
further registry search moot for the end-to-end objective.

This blocker is already partly characterised: Sprint 093 / S-Auto-39 repaired the
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
~592-604/~860-927). The residual, per the OQ-S93.1 brief, is **in CONFIRM**:
`record_outcome(resolve)` is exposed, instructed (`confirm.yaml:20/32`), projected,
and guard-permitted, yet lands **0/42** — because the bad-case personas keep
engaging and the bot **escalates instead of recording a resolve**. The design must
establish, on the exp-90 + Sprint-093 traces, the dominant cause(s): (a) the
`user_requested` mislabel where the bot self-initiates handover but stamps a
user-requested reason (LLM-supplied in `request_handover` args — NOT a runtime
`resolveMaxStepsReason` mis-stamp); (b) genuine non-satisfaction (the answer
isn't good enough / the persona is hard); (c) budget-family runtime escalation
pre-empting closure. **Do not collapse these** — exp-90 confirms `no_ad_id`
(grounds, then loops to `goal_impossible`) and `loaded_listing` (superficial
grounding + frequent UC-A→UC-B misclass + escalates) fail for distinct reasons.

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

### 3. Required trace-level behavior for each PRIMARY (the design target)

- `no_ad_id`: request reference → load listing (`get_customer_context`) →
  grounded answer on listing state → **record RESOLVE → CONFIRM** (close as
  satisfied), instead of looping to `goal_impossible`. (exp-90: the ask+ground
  half already works; the closure half does not.)
- `loaded_listing`: **retain UC-A** (do not misclassify to UC-B) →
  **substantively use listing fields** (status/category/price/location/posted_date,
  not generic FAQ) → targeted resolution → **record RESOLVE → CONFIRM**. (exp-90:
  classify fails 8/13; grounding superficial; closure fails.)
- Both targets must reach a recorded resolve **only when genuinely satisfied**;
  when the helped user remains positively UNRESOLVED, the correct terminal is
  escalate-after-help (the WP1-A conditional path), NOT a forced false-resolve.

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

### 6. Smallest runtime change + validation plan (the deliverable)

The milestone's output is **the smallest runtime change** that delivers the §3
trace behavior for the two PRIMARY without violating §2/§4/§5, plus its
validation plan:
- **Unit/integration (Java):** characterization tests pinning the new
  RESOLVE→CONFIRM→record-resolve path for a satisfiable grounded flow; no new
  regression vs the documented baseline; precedence cases (§4) green.
- **Real-LLM (the eval gate, §5.7):** a bounded NON-pilot run (2 PRIMARY ×11 +
  the anti-误杀 control ×5 + neighbors ×5; §5.9 pre-flight GO; `caffeinate`)
  showing the PRIMARY can reach a **recorded SATISFIED+resolve** when satisfiable,
  with a zero-LLM attribution cross-check distinguishing genuine resolve from the
  `user_requested` mislabel and budget-family vectors. Per the OQ-S93.1 research,
  WP2 (CONFIRM record-vs-handover on *satisfiable* flows) needs a **satisfiable
  companion persona** to be testable — the design must specify it (eval companion,
  not a CaseSpec widen of the existing two).
- **Recommended decomposition (from the OQ-S93.1 research, to confirm):** WP1 =
  `user_requested` mislabel (`prompt_projection`-led, low-risk, RECOMMENDED FIRST);
  WP2 = CONFIRM record-vs-handover on satisfiable flows; + the product question
  (are these personas resolvable / is escalation-after-help the correct terminal?
  — record only, no CaseSpec/bar change).

## Companion / deferred items (recorded, not in this milestone's critical path)

- **`discover_triage.$.procedure` classification lever — DEFERRED.** The one
  untried registry lever (for `loaded_listing` UC-A→UC-B misclass). Revisit
  **only after** the shared closure path is fixed: improving classification alone
  cannot make the objective keep-eligible while the closure blocker stands.
- **`source_ids` trace projection — non-blocking observability item.** Per-turn
  trace shows `source_ids=[]` even when `resolve_article` ran; it does not gate
  the PRIMARY cases (`correct_outcome` reads `containment_outcome`; the
  `loaded_listing` PASS condition reads the bot `user_message` text). Track
  separately; do not bundle into the closure design.

## Holds (in force)

No WP1-B, WP2 (until designed + reviewed), objective-alignment annotation,
posterior/bounded-resampling, mutable-surface expansion, re-bless, or baseline
movement is started by this charter. exp-82 stays WITHDRAWN. The canonical
baseline pointer + `docs/current_eval_baseline.md` are frozen.
