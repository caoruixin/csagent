---
title: "OQ-S93.1 — CONFIRM-phase record-vs-handover + user_requested mislabel (read-only research)"
doc_tier: diagnostic
status: diagnostic
implementation_status: not_started
source_of_truth: this file (evidence) + Sprint 093 bounded-run traces (eval_interactive/results/2026061[78]-*) + runtime code cited inline
last_reviewed: 2026-06-21
review_cadence: ad hoc
supersedes: []
superseded_by: null
resolution: >
  RESOLVED 2026-06-21 (M-Auto-10 WP2, ROUTE (a)): runtime-closure-defect
  hypothesis (mechanism (iv)) REJECTED; grounding-gated isResolvedSuccessTerminal
  is the canonical one-shot satisfiable closure; explicit record_outcome→CONFIRM→CLOSE
  is the sound multi-turn path (demonstrated landing on the PRIMARY), not required
  one-shot. Route-(a) corrections applied Sprint 100 / S-Auto-48; no runtime /
  scored-bar change. See the RESOLUTION banner in the body + the verdict doc
  docs/proposals/m-auto-10-wp2-closure-path-canonical-decision.md.
notes: >
  Read-only research for OQ-S93.1, the SECOND deadlock layer surfaced by the
  Sprint 093 / S-Auto-39 bounded run. NO code / CaseSpec / evaluator / baseline
  change; NO new LLM run. Evidence = the 42 recorded Sprint 093 bounded-run draws
  + two read-only runtime code investigations. Output = a §2 Failure Brief + a
  per-question evidence appendix + a minimal-fix recommendation + a product
  question to surface. The human decides whether to open the next sub-sprint;
  the M-Auto-7 pilot + objective-alignment annotation stay HELD meanwhile.
---

# OQ-S93.1 — CONFIRM-phase record-vs-handover + `user_requested` mislabel

> **RESOLUTION (2026-06-21, M-Auto-10 WP2 — ROUTE (a); runtime-defect hypothesis
> REJECTED).** The OQ-S93.1 question "is the explicit
> `record_outcome→CONFIRM→CLOSE` path a product-required mechanism that must be
> repaired?" is resolved **route (a)**: grounding-gated
> `isResolvedSuccessTerminal` (`ControlKernel.java`) **is** the canonical RESOLVE
> closure on a one-shot satisfiable flow; the explicit
> `record_outcome→CONFIRM→CLOSE` path is the **sound multi-turn** closure,
> **not** a runtime defect. The earlier "make `record_outcome` land" /
> runtime-closure-defect hypothesis (WP2 mechanism (iv)) is **RULED OUT, not
> merely absent**: the explicit path is demonstrated to land end-to-end on the
> PRIMARY `cs_uc_a_loaded_listing` (a draw reaches `current_phase=CLOSE` after
> `record_outcome` succeeds in CONFIRM). On the satisfiable companion it is
> structurally unreachable only because a satisfied user ends the session
> (`goal_achieved`) before any CONFIRM turn — a simulator preempt, not a bot or
> runtime fault. **No runtime fix; no phase-machine change.** The route-(a)
> trace-expectation + docs corrections were applied in **Sprint 100 / S-Auto-48**
> (the companion `expected_tool_sequence` `record_outcome` retirement +
> characterization-test update + these annotations); **no scored acceptance bar
> changed**. A residual latent-robustness question (Gate-D missing 3/11 satisfied
> one-shot users via two Gate-D-internal gaps) is preserved as the **untriggered,
> unscheduled** open question **OQ-S99.1** (measurement-completeness, NOT a
> closure defect, NOT mechanism (iv)). Verdict + full attribution:
> [`../../proposals/m-auto-10-wp2-closure-path-canonical-decision.md`](../../proposals/m-auto-10-wp2-closure-path-canonical-decision.md).
>
> Sections below are preserved as the original 2026-06-18 research record. The
> §3.5 "WP2 — CONFIRM record-vs-handover on *satisfiable* flows" item and the §5
> "make resolve land" framing are now answered by route (a) above (the
> satisfiable path closes grounding-gated; the explicit tool path is not
> required on a one-shot flow).

## 0. Headline (reframes OQ-S93.1)

The Sprint 093 structural fix **works**: 16/42 draws now reach CONFIRM (0 in the
deadlocked baseline shape); the stuck-in-RESOLVE loop is gone. But
`record_outcome(resolve)` lands **0/42** — and the traces show this is **not** a
dispatch / projection / skill-wiring bug. The CONFIRM skill exposes and instructs
`record_outcome`, the projection signals it, and the premature guard permits it in
CONFIRM. `record_outcome(resolve)` does not land because **the bot never reaches a
user-satisfied state on these bad-case personas** — the personas keep reporting the
problem persists ("still not showing up, is there something else?"), so in CONFIRM
the bot (defensibly) chooses `request_handover` over `record_outcome`, exactly as
`confirm.yaml`'s "if satisfied → record / else → handover" disjunction prescribes.

So OQ-S93.1 is **not** a single "make record_outcome land" defect. It is three
distinct things, only one of which is a clear bug:

1. **A real, actionable defect:** bot-initiated escalations are mislabeled
   `escalation_reason=user_requested` (the LLM supplies it in the `request_handover`
   args even though the user never asked for a human).
2. **Largely-defensible behaviour:** on these hard personas the bot exhausts
   grounded help and escalates; `record_outcome(resolve)` legitimately does not fire.
3. **A product question (NOT a fix here):** are these PRIMARY personas *resolvable*,
   or is escalation-after-genuine-help the correct terminal? The CaseSpec expects
   `resolve` and scores escalation `co=0.0`, including 2 draws where the bot offered
   and the **user explicitly accepted**.

## 1. Evidence base

- **Sprint 093 bounded run, 42 draws** (`eval_interactive/results/2026-06-18` dirs
  per `/tmp/s39_matrix_manifest.txt`; gitignored data, read-only): 22 PRIMARY (×11)
  + 20 others (×5). Per-draw mining of `per_turn_trace` (phase_plan + tool_calls +
  result_data) + `transcript` + final `containment_outcome`/`escalation_reason`.
- **Two read-only runtime code investigations** (CONFIRM-phase mechanics; the
  escalation-reason resolver) — file:line cited inline.

## 2. Failure Brief (§2)

- **What happened?** After the Sprint 093 fix, UC-A grounded RESOLVE answers now
  reach CONFIRM, but `record_outcome(resolve)` never lands (0/42). In CONFIRM the
  bot calls `request_handover` instead — and on bot-initiated escalations it labels
  the handover `escalation_reason=user_requested` even though the user did not
  request a human (e.g. the bot's own handover summary says *"Needs human
  investigation"*). Resolved is reached only via the pre-existing
  `isResolvedSuccessTerminal` grounding stamp (7/22 PRIMARY), never via a recorded
  outcome. 0/42 draws reach CLOSE.
- **What should a good CS agent have done?** On a genuinely-resolvable turn where
  the customer is satisfied, record the resolution (`record_outcome(resolve)`) and
  reach CLOSE. When the bot itself decides to escalate after exhausting grounded
  help, label the escalation with a *bot-initiated* reason (e.g. exhausted-resolution
  / service-degraded), reserving `user_requested` for when the customer actually
  asks for a human.
- **Why does this matter?** (a) Measurement honesty: `user_requested` on
  bot-initiated escalations misleads triage and the escalation analytics (and the
  now-observation-only `escalation_reason_family_match`). (b) The PRIMARY signal
  stays capped: with no recorded resolve and no CLOSE, the pilot cannot demonstrate
  clean end-to-end resolution. Constitution: §1.3 (LLM owns escalation posture +
  reason) + the §1.4 trace-contract.
- **Is this a one-off or a pattern?** **Pattern.** 13/22 PRIMARY draws escalate;
  the mislabel + never-record shape repeats across both PRIMARY cases and runs.
- **Which layer is likely responsible?** Multi-layer (see §3.5): primarily
  `semantic_planner` / `prompt_projection` (the LLM's record-vs-handover choice +
  the reason label it supplies), with a `eval_spec`/`product_policy` question about
  whether the personas are resolvable. **Not** `infra` for the `user_requested`
  label (it is LLM-supplied, not a runtime resolveMaxStepsReason mis-stamp — that
  is a *separate* vector that produces the budget-family reasons).
- **What should NOT be done?** Do not relax the premature-resolve guard; do not
  force `record_outcome` (faking user satisfaction); do not widen the CaseSpec or
  lower the bar; do not add a user-message keyword/content heuristic to detect
  "user requested a human"; do not presume all escalations are wrong (2/13 are
  genuine user-accepted; ~5/13 more are defensible on a hard problem).

## 3. Per-question evidence

### 3.1 (Q1) Direct cause of `record_outcome(resolve)` 0/42

| where | record_outcome attempts | outcome |
|---|---|---|
| RESOLVE | 20 | **all premature-rejected** (correct: phase ≠ CONFIRM/CLOSE — `shouldRejectPrematureResolveOutcome`) |
| CONFIRM | **0** | the bot never calls it in CONFIRM |

So the cause is **"not selected in CONFIRM"** (the LLM chooses `request_handover`),
compounded by **"rejected in RESOLVE"** (premature) on the draws that try early. It
is **not** selected-but-rejected-in-CONFIRM, **not** a tool-result-persist failure,
and **not** a missing-tool/projection gap — the CONFIRM skill (`confirm.yaml`,
`tools_required: [record_outcome, request_handover]`) exposes + instructs it, the
projection (`ContextProjectionBuilder`) carries phase=CONFIRM + the record_outcome
schema, and `ResolveDispositionEvaluator.shouldRejectPrematureResolveOutcome`
returns `false` (permit) in CONFIRM. The bot reaches CONFIRM and *acts* — it just
acts by handing over, because the persona is visibly unsatisfied.

### 3.2 (Q2) CONFIRM-phase projection / available actions / planner guidance

- Active skill in CONFIRM (UC-A) = `server/src/main/resources/skills/confirm.yaml`
  (`applicable_phases: [CONFIRM]`, `applicable_use_cases: ["*"]`). Its procedure is
  **disjunctive**: *if satisfied ("thanks/that helps/yes") → `record_outcome(RESOLVED)`;
  if not satisfied ("no/still not working/I need more help") → `request_handover`
  (reason `user_dissatisfied`) OR transition back to RESOLVE.*
- Projection (`ContextProjectionBuilder`) injects `phase_plan.phase=CONFIRM`, the
  objective, the system_instruction (the procedure), and the `record_outcome` +
  `request_handover` tool schemas. The LLM **is** told it is in CONFIRM and **can**
  record.
- **Mislabel surface:** `confirm.yaml` prescribes `user_dissatisfied` for the
  not-satisfied handover, but the LLM supplies **`user_requested`** in the
  `request_handover` args instead (verified in the raw tool calls). There is no
  guidance making the LLM use a *bot-initiated* reason when the bot itself decides
  to escalate, and `user_requested` is the highest-priority reason
  (`EscalationReasonResolver` tier-0), so once supplied it sticks.

### 3.3 (Q3) Per-item classification of the 13/22 PRIMARY escalations

| class | count | meaning | layer |
|---|---|---|---|
| **GENUINE user-accepted** | **2** | bot offered → user explicitly accepted ("Yes, please. I'd appreciate speaking to someone…") → escalation is the correct CS outcome, but scored `co=0.0` | not a bot defect (eval_spec / product question) |
| **bot-initiated MISLABEL** | **6** | bot decided to escalate after the persona kept engaging (no user request); labeled `user_requested` (LLM-supplied arg) | `semantic_planner` / `prompt_projection` (the actionable defect) |
| **budget-family runtime** | **5** | `turn_budget_exhausted` / `faq_miss_threshold_exceeded` / `clarification_budget_exhausted` from `resolveMaxStepsReason` on loop/budget exhaustion; several on UC-A→UC-B misclassified draws | `infra`/reason-resolver + the UC-misclass tail (pre-existing) |

`user_requested` is **LLM-supplied** (8 draws total carry it; the request_handover
arg = `user_requested`), confirmed by the raw tool calls. The budget-family reasons
are the **runtime resolveMaxStepsReason** vector (the known
`project_faq_overescalate_maxsteps_misstamp` pattern) — a *different* mislabel
source from the `user_requested` one.

### 3.4 (Q4) The 2 surface-plausible (genuine) escalations — full chain

Both are `cs_uc_a_loaded_listing`, UC-A: the bot delivered grounded
visibility/performance tips, the user reported the tips did not help, the bot
**offered** to escalate, and the user **explicitly accepted** ("Yes, please. I'd
appreciate speaking to someone who can help…"). Did the user change intent + accept
a human? **Yes** (accepted the bot's offer). Conflict with the cooperative-persona /
CaseSpec terminal contract? **Yes** — the CaseSpec expects `resolve` (`co=1.0`) and
scores these `co=0.0`, even though escalation-after-genuine-help is defensible CS
behaviour. **Recorded only; no CaseSpec / bar change in this research** (per the
constraint).

### 3.5 (Q5) Recommended next fix layer / work-package split

- **WP1 — `user_requested` mislabel on bot-initiated escalation (RECOMMENDED FIRST).**
  Layer: `prompt_projection` / `semantic_planner` (reason labeling is LLM-owned per
  §1.3). Tighten `confirm.yaml` (+ the escalation reason vocabulary in projection)
  so the LLM reserves `user_requested` for an actual user request and uses a
  bot-initiated reason when it decides to escalate. Low-risk, anti-hardcode-clean,
  measurement-honesty win. **Do NOT** add a runtime content heuristic that inspects
  the user message to "detect a request" (forbidden vector).
- **WP2 — CONFIRM record-vs-handover on *satisfiable* flows.** Layer:
  `prompt_projection` / `semantic_planner`. Whether the bot records when the user is
  actually satisfied is hard to validate on these never-satisfied personas; WP2
  likely needs a *satisfiable* companion case (a persona that accepts the grounded
  answer) to exercise the record→CLOSE path. Scope/validate separately.
- **Pre-existing UC-A↔UC-B misclassification tail** (≥4 of the 5 budget-family draws
  ran as UC-B): not new to OQ-S93.1; track under the existing UC-stability work, not
  bundled here.
- **PRODUCT QUESTION (human / product owner — NOT a fix):** are the PRIMARY personas
  resolvable, or is escalation-after-genuine-help the correct terminal? If escalation
  is acceptable here, the issue is the *label* (WP1) + possibly the CaseSpec's
  accepted-outcome set — **not** the bot's behaviour. This is the same open question
  the 2026-06-18 acceptance review raised, now sharpened. Record only; revisit the
  bar only via the review/override/re-bless process if the human decides to.

## 4. Constraints honoured

Read-only: no code / CaseSpec / evaluator / baseline edited; no new LLM run. Not all
escalations presumed wrong (2 genuine + ~5 defensible). The premature guard is not
relaxed and `record_outcome=0` is not used to justify weakening it.

## 5. Recommendation to the human

Open a sub-sprint for **WP1** first (the `user_requested` mislabel — clear,
low-risk, `prompt_projection`-led), and **take the §3.5 product question** before
investing in WP2 / any "make resolve land" work, since on these personas escalation
may be the correct terminal. The M-Auto-7 pilot + objective-alignment annotation
stay HELD until the OQ-S93.1 scope is decided. No sub-sprint is drafted by this
research; the human decides next.
