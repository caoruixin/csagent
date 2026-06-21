---
title: "M-Auto-11 candidate investigation — post-satisfaction mechanical over-escalation (R-post-satisfaction-mechanical-over-escalation)"
doc_tier: diagnostic
status: diagnostic
implementation_status: historical
source_of_truth: >
  the persisted bot_sessions/bot_turns/bot_events for session
  55762050-e7e9-4c76-8e22-fa82ff4021a6 (local csagent postgres, read-only) +
  the cited runtime code paths; docs/sprints/sprint-097-handoff.md §7 / §5.9 is
  the surfacing record
last_reviewed: 2026-06-21
review_cadence: ad hoc
supersedes: []
superseded_by: null
notes: >
  Read-only deliver-agent scoping investigation for the M-Auto-11 primary candidate
  R-post-satisfaction-mechanical-over-escalation. Conclusion: the candidate's premise
  (a reproducible semantic_planner posture defect) is DISPROVEN by DB evidence; the
  observed event is an ISOLATED (n=1) occurrence whose mechanism is the runtime
  MAX_STEPS escalation fallback (PhaseEvaluator.java:628-644 + resolveMaxStepsReason),
  a member of the known MAX_STEPS-misstamp / within-turn retry-storm family. Recommends
  NOT creating a milestone around this candidate; reclassify + fold as an observation;
  next-strongest candidate named. No runtime/eval/prompt/CaseSpec/baseline change.
---

# M-Auto-11 candidate investigation — post-satisfaction mechanical over-escalation

**Scope:** read-only trace / DB / code / CaseSpec / docs investigation of the M-Auto-11
primary candidate `R-post-satisfaction-mechanical-over-escalation`. No implementation;
no runtime/eval/prompt/CaseSpec/simulator/baseline/canonical-pointer change; no LLM run.

## 0. One-line result

**The candidate as framed — a reproducible `semantic_planner` posture defect ("the bot
mechanically escalates an already-satisfied user") — is NOT supported.** The single
observed event is an **isolated (n=1)** occurrence whose true mechanism (DB-confirmed) is
the **runtime MAX_STEPS escalation fallback** triggered by a within-turn `search_knowledge`
retry-storm — a known, partially-mitigated family — **not** an LLM posture choice and
**not** a satisfaction-blind planner. **Recommendation: do not manufacture a milestone;
record as an observation, reclassify the R-item, and name the next-strongest candidate.**

## 1. What was claimed vs what the evidence shows

**Claim (action_bank §5, surfaced by Sprint 097 §7 / the §5.9 pre-flight):** after a
correct grounded answer + a satisfied user, the bot emitted the cs001-style mechanical
"I'm having difficulty resolving this. Let me connect you with a specialist." handover on
a trivial follow-up; provisionally classified `semantic_planner (posture)`.

**Evidence base:** the §5.9 pre-flight single draw `results/20260621-014055`. The disk
artifact is gitignored and has been cleaned (absent), but the run is uniquely and durably
identified in the local `csagent` postgres as session
`55762050-e7e9-4c76-8e22-fa82ff4021a6` (`created_at = 2026-06-21 01:40:55 UTC`, UC-A,
companion case `cs_uc_a_loaded_listing_resolvable` — the unique first session of the day;
timestamp + UC + case all match). `bot_turns` / `bot_events` / `session_outcomes` are
intact and were read read-only.

## 2. DB-reconstructed timeline (the decisive evidence)

- **Turn 1** (`DISCOVER → RESOLVE`, 14.1s): user asks whether the Vintage Record Player
  ad is still active. Bot classifies UC-A (conf 0.90), runs `search_knowledge` (2 hits,
  `faq_miss=false`), `resolve_article`, then attempts `record_outcome(resolve)` which the
  runtime **premature-rejects** (`progressive_resolve_record_outcome_premature` — the
  expected Gate-A behaviour in RESOLVE). Bot delivers the **correct grounded answer**
  ("…ad (AD-2007) has expired… repost it or use Bump Up… [help URL]"),
  `source_ids = {ka4P200000004ZtIAI, ka4P2000000060bIAA}`. `llm_raw_response`: answer
  delivered with empty `tool_calls`; bot explicitly notes it will "wait for user
  confirmation" since `record_outcome` was rejected.
- **Turn 2** (`RESOLVE → ESCALATE`, 11.9s): user sends a satisfied trivial follow-up
  ("Thank you for explaining. I understand now that my ad expired. How do I go about
  reposting it?"). **The LLM raw response requested exactly ONE tool —
  `search_knowledge("how to repost an expired ad")` with `user_message:""`. It did NOT
  request a handover and did NOT author the "I'm having difficulty" line.** The within-turn
  agent loop then re-emitted that search across steps; the A1/A3 storm-breakers
  dedup/`paraphrase_suppressed` the repeats (each returning the same 3 valid hits,
  `faq_miss=false`) but each suppressed step still advances the step counter, so the loop
  reached the step ceiling. Event log: `RESOLVE_DISPOSITION transition_reason=max_steps_exceeded`
  → `ESCALATION_REQUESTED {reason: turn_budget_exhausted}` → `OUTCOME_RECORDED {escalated}`.
  The user-facing text is the canned MAX_STEPS template.
- **Terminal:** `current_phase=ESCALATE`, `containment_outcome=escalated`,
  `escalation_reason=turn_budget_exhausted`, `closed_at=NULL`. `record_outcome` never
  succeeded (turn-1 attempt premature-rejected; turn-2 attempted none).
- **Simulator:** **continued past satisfaction on this draw** (it sent the turn-2
  follow-up rather than `goal_achieved`-preempting). In the subsequent 11-draw core block
  the simulator preempted **11/11** (`reachedCONFIRM = 0/11`), so even *exposing* the bot
  to a post-satisfaction follow-up is itself a low-probability simulator-nondeterminism
  event on this companion.

## 3. Root cause (code-corroborated)

The escalation is **runtime-synthesized**, not LLM-authored:

- **MAX_STEPS terminal** (`PhaseEvaluator.java:628-644`): when the agent loop exhausts its
  step budget, the runtime unconditionally emits the hardcoded
  `"I'm having difficulty resolving this. Let me connect you with a specialist."` with
  `phase_transition_reason="max_steps_exceeded"` and
  `escalation_reason = resolveMaxStepsReason(...)`.
- **Honest label** (`PhaseEvaluator.java:189-219`, the B1 / S-Auto-14 / Sprint 070
  evidence-aware version): a viable-hit (`faq_miss=false`) exhaustion correctly falls
  through to the `turn_budget_exhausted` catch-all — which is exactly what this draw
  recorded. So the *label* fired honestly; the *behaviour* (escalate on loop exhaustion
  despite usable hits) is the open part.
- **Loop step-advance** (`AgentRunLoopImpl.java:305` `for (int step = 0; step < maxSteps;
  step++)`; the dedup/paraphrase `continue`s at `:649/:665/:700/:748` advance `step`;
  `:967` logs "hit max_tool_steps without terminal outcome"): the storm-breakers stop
  external tool re-dispatch/latency but do **not** break the loop or feed back "you already
  have viable hits — answer or record"; the loop still reaches `maxSteps`.
- **Runtime never observes satisfaction** (`ContextProjectionBuilder` projects no
  `user_state` / `satisfied` / `goal_achieved` field — grep-confirmed empty;
  `docs/current/m-auto-9-escalate-after-help-product-decision.md` confirms `user_state` is
  a simulator-side eval signal the runtime never sees). The "post-satisfaction" qualifier
  is therefore a description of the *eval label on the transcript*, not a signal the bot
  could have consumed.

## 4. Hypothesis adjudication (per the scoping brief)

| hypothesis | verdict | evidence |
|---|---|---|
| (1) semantic_planner failed to consume current SATISFIED user_state | **DISPROVEN** | runtime never projects satisfaction (`ContextProjectionBuilder`); planner cannot consume it. Separately, the planner did **not** choose to escalate — it requested a search. |
| (2) a previously-generated escalation plan executed after state changed | **DISPROVEN** | plans are fetched fresh every turn (`ControlKernel.java:372`); no cross-turn plan persisted on `BotSession`. |
| (3) phase/tool ordering created a stale-plan race | **DISPROVEN** | same as (2); the only cross-turn state is `currentPhase`. |
| (4) CaseSpec / simulator contract permitted contradictory behavior | **CONTRIBUTING, not causal** | the simulator continued past satisfaction on this 1 draw (vs 11/11 preempt in the core block); that *exposed* the second turn but did not *cause* the escalation. |
| (5) the handover was actually justified by another specific reason | **NO** | `turn_budget_exhausted` is a MAX_STEPS fall-through stamp, not a safety/compliance/payment/fraud/legal/user-requested reason. |
| (6) isolated model variance, not a stable defect | **YES for the occurrence** | n=1; 0/11 reproduction in the core block; the within-turn search-loop trigger is model noise. The *underlying* MAX_STEPS amplification path is, however, a stable runtime path (see §5). |

**Net:** the *occurrence* is isolated noise that happened to exercise a **real, stable,
known, partially-mitigated runtime amplification path** (within-turn search retry-storm →
MAX_STEPS → mechanical escalate). The defect is mis-attributed in the candidate
(`semantic_planner posture`); the operative layer is `infra`/runtime (with a
`prompt_projection`/`semantic_planner` loop-convergence contributing factor).

## 5. Family + prior-art status (this is an old family, not a new defect)

- `R-runtime-identical-tool-call-retry-storm` (A1 / S-Auto-12 / Sprint 067) — **SHIPPED**
  (per-run byte-identical dedup).
- `R-runtime-paraphrase-storm-search-knowledge` (A3 / S-Auto-13b/15) — **PARTIAL**
  (within-turn + cross-turn rank-2+ shipped; cross-turn rank-1 first-refinement is an
  open OBSERVATION).
- `R-runtime-escalation-reason-misstamp-maxsteps-faq` (B1 / S-Auto-14 / Sprint 070) —
  **SHIPPED** (evidence-aware `resolveMaxStepsReason`; only the *label* was fixed).
- Sprint 039 `faq_miss_handover_requires_resolve_attempt` guardrail — closed only the
  **LLM-self-stamp** vector; the **runtime MAX_STEPS** vector stayed open.
- **Still open (behavioural):** there is **no** guard that diverts a viable-hit
  (`faq_miss=false`) MAX_STEPS exit away from the mechanical escalation, and **no** guard
  that breaks a non-converging within-turn search loop before `maxSteps`. Solution doc
  `docs/solutions/escalation_reason_runtime_evidence_contract_maxsteps_misstamp.md`
  shipped SS-A (label) only; SS-B (faq_miss/moderation projection) is deferred.
- **Recurrence of this specific shape (ii) — post-grounded-answer escalation via
  MAX_STEPS** (vs shape (i) cs001 turn-0-before-tools): 3 documented historical instances,
  all UC-A (`docs/diagnostics/failure-briefs/manual-probe-2026-05-13-…`,
  `…2026-05-24-uc-a-faq-refutation-overescalate.md`,
  `oq-s93.1-confirm-record-vs-handover.md` §3.3/§3.4) — **none with the "user already
  satisfied + trivial follow-up" precondition.** So the 2026-06-21 draw is a **new trigger
  context for an old family**, at n=1.
- **CaseSpec coverage:** shape (i) is encoded by `cs001_uc_c_mechanical_template_escalate.yaml`
  (escalate **before any tool call**); **no** bad-case encodes shape (ii) (the
  post-help / viable-hit MAX_STEPS escalation).

## 6. Overlap checks (kept separate — no direct causal evidence of overlap)

- **OQ-S99.1 (Gate-D measurement-completeness):** opposite direction (under-recording a
  satisfied resolve vs over-firing an escalation) and a different mechanism (Gate-C/D vs
  MAX_STEPS). No causal overlap. Keep separate.
- **one-turn DISCOVER pre-routing:** unrelated — those are 1-turn pre-routing sessions;
  this is a 2-turn `RESOLVE→ESCALATE` session with a committed UC. Keep separate.
- **WP0 `source_ids`/promotion:** this draw delivered grounded `source_ids` from
  `search_knowledge`; the latent `get_customer_context`/`resolve_article` coupling is not
  the operative factor here. Keep separate / HELD.
- **M-Auto-9 / M-Auto-10 closure semantics:** this is the *over-escalation* mirror, but
  mechanistically distinct (MAX_STEPS escalate vs grounding-gated resolve). It does **not**
  contradict M-Auto-10's "no general closure-path defect" conclusion — it is an
  escalation-path / loop-convergence issue, which M-Auto-10 explicitly carved out as a
  separate backlog item. Preserve the M-Auto-10 conclusion.

## 7. Reproducibility classification

**ISOLATED (n=1) occurrence of a real-but-known, partially-mitigated family.** Not
"reproducible" in the load-bearing sense (0/11 core-block reproduction; the simulator
continue itself is rare on this companion). Not "plausible-but-unproven" as a *posture*
defect — that framing is positively disproven. The **runtime MAX_STEPS-escalate-despite-
viable-hits behaviour** is stable and reproducible-by-construction, but its rate on
genuinely-satisfiable/satisfied flows is unestablished (n=1).

## 8. Recommendation

1. **Do NOT create M-Auto-11 around `R-post-satisfaction-mechanical-over-escalation` as a
   `semantic_planner` posture milestone.** The premise is disproven and the occurrence is
   n=1.
2. **Reclassify + fold the R-item** (action_bank §5): layer `semantic_planner (posture)` →
   `infra`/runtime MAX_STEPS escalation (with a `prompt_projection`/`semantic_planner`
   loop-convergence contributing factor); record the DB-corrected mechanism, n=1, and the
   family membership; mark **not yet load-bearing** pending recurrence on real traffic.
3. **If the human wants to pursue the over-escalation theme**, the honest next step is a
   **minimum bounded characterization** (NOT launched here) to establish whether the
   MAX_STEPS-escalate-despite-viable-hits path fires on genuinely-satisfiable/satisfied
   flows at a load-bearing rate — e.g. encode a shape-(ii) bad-case (satisfied user +
   trivial follow-up after a grounded answer) and run a bounded N-draw real-LLM sample
   under the S-Y1.7 V3 noise-aware rule. Only if recurrence confirms load-bearing should a
   runtime sub-sprint be scoped, targeting **loop-convergence** (project the existing
   viable hits so the planner answers/records instead of re-searching; and/or a soft
   loop-break before `maxSteps`) under the inherited M-Auto-9 fences — **not** a Java
   "don't escalate when satisfied" gate (the runtime cannot see satisfaction, and a
   keyword/posture gate is forbidden per Constitution §1.5/§1.7 and
   `feedback_cs_agent_posture`).
4. **Otherwise**, the next-strongest backlog candidates are (deliberately ranked, human
   selects): the open **MAX_STEPS-escalate-despite-viable-hits behavioural** item (real,
   recurring family; needs the §3 characterization first); **OQ-S99.1** Gate-D
   measurement-completeness (narrow, low-risk, but optional/measurement-only); or a new
   research-driven direction. WP0 stays HELD; the trace-contract infra brief is already
   closed (Sprint 098).

## 9. Forbidden / held / eligible surfaces (for any future scoping)

- **Forbidden** (without a fresh `sprint_objective.md` + §7 stanza + human approval, and
  never as a posture/keyword gate): any change to `ResolveDispositionEvaluator`, the
  premature-resolve guard, `isResolvedSuccessTerminal`, `PhaseEvaluator` promotion logic,
  `max_turns`/`maxToolSteps`, the `record_outcome` requirement, any CaseSpec widen,
  baseline move, or canonical-pointer flip.
- **Held:** WP0 `source_ids`/promotion coupling.
- **Eligible only after a load-bearing recurrence finding:** a loop-convergence
  prompt_projection change (surface existing viable hits to discourage re-search) and/or a
  soft within-turn loop-break, validated by a bounded real-LLM V3 run with the named
  cross-UC regression guards green and anti-误杀 preserved.

## 10. Provenance

- DB (read-only): `csagent` postgres session `55762050-e7e9-4c76-8e22-fa82ff4021a6`
  (`bot_sessions` / `bot_turns` turn_index 1–2 / `bot_events` / `session_outcomes`).
- Code: `PhaseEvaluator.java:189-219` (resolveMaxStepsReason) + `:628-644` (MAX_STEPS
  template); `AgentRunLoopImpl.java:305/649/665/700/748/967` (loop + storm-breakers);
  `ControlKernel.java:372` (fresh plan/turn); `ContextProjectionBuilder` (no satisfaction
  projection); `EscalationReasonResolver` (priority table).
- Docs: `docs/sprints/sprint-097-handoff.md` §5.9/§4.1/§7 (surfacing record);
  `docs/proposals/m-auto-10-wp2-closure-path-canonical-decision.md` (WP2 verdict context);
  `docs/solutions/escalation_reason_runtime_evidence_contract_maxsteps_misstamp.md`
  (family prior-art); `docs/diagnostics/failure-briefs/` (shape-(ii) prior instances).
