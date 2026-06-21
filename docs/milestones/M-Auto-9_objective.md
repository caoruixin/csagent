---
title: "Milestone M-Auto-9 (CLOSED COMPLETE 2026-06-21) — Runtime closure; M-Auto-7 historical body retained below"
doc_tier: current-runtime
status: archived
implementation_status: implemented
source_of_truth: this file (active-milestone pointer); design north-star = docs/proposals/runtime-closure-record-outcome-design-milestone-oq-s93.1.md
last_reviewed: 2026-06-21
review_cadence: per milestone
supersedes: []
superseded_by: null
notes: >
  North star is AUTOLOOP READINESS, validated by running the CS4
  entity-context autoloop pilot end-to-end — NOT completing every
  bad-case semantic fix. Two phases inside one milestone:
    Phase 1 (autoloop launch blockers): CS1 measurement honesty,
      CS3 DISCOVER runtime hygiene, CS4 projection infra, CS4
      executable CaseSpecs.
    Phase 2 (CORE GATE — the pilot): autoloop authors a skill-yaml
      procedure candidate against the CaseSpecs on the honest
      baseline; human reviews under §4.1; accepted candidate merged
      + re-blessed.
  CS2-original / perspective grounding is additive back-half work
  (or a follow-on sub-sprint); it is explicitly NOT an autoloop
  pilot launch blocker and NOT part of the core acceptance bar.
  Source proposals (single coherent input):
    - docs/solutions/2026-06-07-cs1-default-resolved-cs2-user-role-projection-gap.md
    - docs/solutions/2026-06-07-cs3-cs4-discover-stall-uc-fp-boundary-empty-trace-ux.md
    - docs/solutions/2026-06-08-cs4-casespec-drafts-appendix.md
  Launch precondition (M-Auto-6 fully CLOSED 2026-06-07: S-Auto-28
  Definition-of-done + milestone-shared re-bless + Codex §4.3) is MET.
  Autoloop capability spike RESOLVED READY (CS3/CS4 §3.5) — no tooling
  pre-req for the pilot. D-full-issue-ledger-light (CS3/CS4 §10.4) is
  NOT in this milestone (post-close research workstream). Replaces the
  post-M-Auto-6 M-Auto-7-candidate-selection placeholder.
  2026-06-09 UPDATE — INSERTED sub-sprint S-Y1.5 (Sprint 087 / S-Auto-32)
  between S-Y1 and the S-Y2 CORE GATE, after the S-Y2 run-1 validation
  tranche (exp-66/67/68; 0/3 on-gap, 2/3 phase-incorrect) surfaced three
  autoloop-meta-agent feedback-loop defects (P0-A/B/C) + one amplifier
  (P1). S-Y1.5 is a pure-autoloop-infra patch (Option B of the audit
  docs/solutions/2026-06-08-autoloop-mechanism-audit-and-feedback-loop-tightening.md);
  no re-bless. S-Y2 renumbered → Sprint 088 / S-Auto-33; S-B → Sprint 089
  / S-Auto-34. S-Y1.5 CLOSED 2026-06-09 (dev 43cd9cf + Codex
  APPROVE_S_Y1_5 / blocking_count=0); the S-Y2 CORE-GATE contract is
  promoted to docs/sprint_objective.md (Sprint 088) + dev prompt
  compact/sprint-088-dev-prompt.md; Part C HELD pending human go-ahead.
  2026-06-16 UPDATE — INSERTED sub-sprint S-Y1.7 (Sprint 091 / S-Auto-37,
  "Autoloop fitness-measurement reliability") between S-Y1.5 and the S-Y2 CORE
  GATE, after the S-Y2 run-1/2/3 tranches produced 0/14 keeps and the zero-LLM
  P0.7 retrospective calibration root-caused it as a ~92–95% false-discard rate
  under the n=3–5 majority-flip fitness gate. Per §5.8 (framework-defect
  priority) S-Y1.7 PREEMPTS S-Y2 Part C: the gate is replaced with a
  stability-tiered noise-aware rule (V3) before the pilot burns more real-LLM
  tranches. Resumes the PAUSED M-Auto-4 scope as an M-Auto-7 sub-sprint (human
  decision: insert, not formally pause M-Auto-7). S-Y2 / Sprint 088 contract
  HELD verbatim at compact/sprint-088-objective-HELD.md; re-promoted to
  docs/sprint_objective.md on S-Y1.7 close. S-Y1.7 contract is LIVE at
  docs/sprint_objective.md + compact/sprint-091-dev-prompt.md; acceptance
  evidence docs/solutions/p07-calibration/. NO re-bless (autoloop/** +
  config.yaml only). Estimated +2–4 d to the §9 duration.
  2026-06-18 UPDATE — S-Y2 Part C ran (exp-66..85; exp-82 nominated→OVERTURNED
  n=13→WITHDRAWN). Inserted + CLOSED S-Auto-38 / Sprint 092 (escalation_compliance
  tier-0 split, §5.8) — gate-trust validated by the exp-86 -n1 smoke (PASS /
  OFF_TARGET, not mergeable / not seed). A read-only acceptance review + OQ-S86b.3
  forensic confirmed the PRIMARY bars are product-correct (do NOT lower) and a
  RESOLVE→CONFIRM/CLOSE phase-transition deadlock as the non-escalation root cause.
  INSERTED Sprint 093 / S-Auto-39 (runtime/skill_state corrective) as the next
  pre-pilot blocker; S-Y2 stays HELD until it closes + passes Codex. Human-confirmed
  boundaries: objective-alignment annotation stays in M-Auto-7 as the final
  readiness item (NOT implemented until Sprint 093 closes); primary-first staged
  eval + adaptive sampling → new milestone M-Auto-8 (out of scope here). Full pilot
  tranche HELD; exp-82 WITHDRAWN.
  2026-06-19 CLOSURE — the S-Y2 registry-only autoloop pilot (the Phase-2 CORE
  GATE) is CLOSED — NO KEEP: registry-only control-surface limit reached for the
  current PRIMARY objective. Arc after the 2026-06-18 update: S-Auto-40 WP1-A
  CLOSED (measurement/conditional-acceptance infra only) → S-Auto-41 proposer
  steering + read-only mutation-surface scoping → S-Auto-42 bool-only flaky-tier0
  defer VALIDATED on first real evidence → exp-90 (byte-identical frozen
  $.grounding_instruction) real DISCARD. exp-90 established that the shared runtime
  closure / record_outcome blocker (OQ-S93.1) independently gates BOTH PRIMARY, so
  no registry-only edit can make the objective keep-eligible. The CS4
  entity-context PRIMARY objective + the runtime/orchestration closure continue
  under the formal successor milestone **M-Auto-9** (charter
  docs/proposals/runtime-closure-record-outcome-design-milestone-oq-s93.1.md;
  PROPOSED, awaiting design review/approval — implementation NOT started).
  discover_triage.$.procedure classify lever DEFERRED; source_ids =
  non-blocking observability. Holds intact (no exp-91 / WP1-B / WP2 /
  objective-alignment annotation / posterior-resampling / surface expansion /
  re-bless / baseline move / canonical-pointer change). See the §0 status banner.
  2026-06-20 MILESTONE SYNC — the active, approved milestone is now M-Auto-9
  (runtime/orchestration closure; design FINAL verdict APPROVE 2026-06-20; north-star
  charter docs/proposals/runtime-closure-record-outcome-design-milestone-oq-s93.1.md).
  Its first dev sub-sprint, Sprint 095 / S-Auto-43 (WP1 — user_requested
  escalation-reason honesty), was launched + CLOSED BLOCKED 2026-06-20 (no honest
  existing escalation_reason; no code shipped); the honest fix = the
  D-new-escalation-reason-enum migration, now SCOPED as Sprint 096 / S-Auto-44 (not
  launched); WP0/WP2 HELD under charter §6. This is a MINIMAL
  active-milestone pointer sync only: the entire M-Auto-7 body + outcome below
  (registry-only pilot CLOSED — NO KEEP) is PRESERVED unchanged as the delivered
  historical record; no milestone history is rewritten and no WP1 scope is expanded
  here. The M-Auto-9 active banner is the section immediately below the front matter.
  2026-06-21 WP1 CLOSE — the M-Auto-9 WP1 honest-fix (D-new-escalation-reason-enum
  migration) was DELIVERED by Sprint 096 / S-Auto-44 (dev+Codex CLOSED COMPLETE
  2026-06-20): added the 24th canonical escalation_reason `agent_unable_to_resolve`
  (resolver priority 50, strictly lowest; §6 precedence proven; §7.6 bounded real-LLM
  PASS no-over-use; §4.1 Codex approve/pass/0; no re-bless §5.7 N/A). implementation_status
  bumped not_started → partial (WP1 reason-honesty delivered; the PRIMARY closure /
  record_outcome question + WP0 remain not_started/HELD). 2026-06-21 product/eval_spec
  decision RECORDED (docs/current/m-auto-9-escalate-after-help-product-decision.md:
  escalate-after-genuine-help valid; goal_impossible not an auto-hard-fail) + WP2 (satisfiable
  UC-A entity-context companion cs_uc_a_loaded_listing_resolvable) SCOPED as Sprint 097 /
  S-Auto-45 (companion name + 10-point acceptance contract human-BLESSED 2026-06-21; NOT LAUNCHED;
  docs/sprint_objective.md + compact/sprint-097-dev-prompt.md). WP0 HELD. Minimal pointer
  sync only; no milestone history rewritten. Archives docs/sprints/sprint-096-{objective,handoff}.md.
---

# Milestone M-Auto-9 — CLOSED COMPLETE (2026-06-21); runtime/orchestration closure

> **M-Auto-9 is CLOSED — COMPLETE (2026-06-21).** Both close gates passed: the §5.6
> curated bad-case suite manual review = **PASS / no regression** (54 real-LLM sessions,
> hard floors 100%; runs `20260621-0248/0253/0258`, recorded in the bad-case manifest
> "M-Auto-9 milestone close" section), and the milestone-shared Codex = **`pass` /
> blocking_count 0** (`docs/milestones/M-Auto-9_codex-review.md`). **Delivered:** WP1
> (Sprint 096 — the `agent_unable_to_resolve` escalation-reason honesty migration) + the
> recorded product/eval decision (`docs/current/m-auto-9-escalate-after-help-product-decision.md`)
> + WP2 (Sprint 097 — the satisfiable companion `cs_uc_a_loaded_listing_resolvable` proving a
> recorded SATISFIED+resolve terminal **CAN land**, 8/11 V3-clearing, 2 PRIMARY 0/11 no
> regression). Per the charter, **"no phase-machine change" is a first-class valid outcome** —
> and that is where M-Auto-9 landed: the two PRIMARY personas are unsatisfiable-by-construction
> and correctly escalate-after-help.
>
> **Carried forward (NOT in M-Auto-9 delivered scope; → M-Auto-10 candidate selection):**
> (1) **OQ-S93.1 residual** — the explicit `record_outcome→CONFIRM→CLOSE` tool path stays
> premature-guard-blocked; resolves land grounding-gated via `isResolvedSuccessTerminal`. **Open
> design question:** is the explicit `record_outcome` route a required product mechanism to
> repair, or is the grounding-gated terminal the intended canonical closure path (revise the
> trace expectation)? (2) **WP0** (source_ids/promotion-evidence) — HELD. (3) **eval-framework
> brief** `R-trace-contract-active-use-case-snakecase-camelcase` (§3 infra; address before the
> next §5.6 rerun per §5.8). (4) **post-satisfaction over-escalation** bad-case candidate
> (`semantic_planner` posture). See `docs/action_bank.md` §5. Archives:
> `docs/sprints/sprint-09{5,6,7}-*`. Successor = M-Auto-10 (human candidate selection).
>
> **Everything below is the M-Auto-9 historical record** — design milestone. Design FINAL
> verdict **`APPROVE`** (2026-06-20). North-star =
> the charter
> [`docs/proposals/runtime-closure-record-outcome-design-milestone-oq-s93.1.md`](proposals/runtime-closure-record-outcome-design-milestone-oq-s93.1.md)
> (carries the full required design coverage + the WP0/WP1/WP2 decomposition).
>
> **First dev sub-sprint: Sprint 095 / S-Auto-43 — WP1 `user_requested` escalation-reason
> honesty** (`prompt_projection`/`semantic_planner`). Launched + **CLOSED BLOCKED
> 2026-06-20** at the §6 existing-reason honesty gate: no member of the canonical
> 23-value `escalation_reason` enum honestly labels the bot-initiated "exhausted-resolution
> / unresolved / needs-human" handover, and the contract forbade shipping a
> knowingly-inaccurate catch-all → **no code shipped; the mislabel is not fixed.** The
> honest fix — the `D-new-escalation-reason-enum` migration — was **DELIVERED by Sprint 096 /
> S-Auto-44 (dev+Codex CLOSED COMPLETE 2026-06-20)**: it added the 24th canonical reason
> **`agent_unable_to_resolve`** (resolver priority 50, strictly lowest; §6 precedence proven),
> wired atomically across all producer/consumer surfaces, so the bot-initiated mislabel is
> corrected at the vocabulary level. §7.6 bounded real-LLM PASS / no over-use; §4.1 Codex
> `approve`/`pass`/0; no re-bless (§5.7 N/A). Archives `docs/sprints/sprint-09{5,6}-{objective,handoff}.md`.
> **WP1 delivered reason-label honesty only — it does NOT solve the PRIMARY closure /
> product-contract question.** The gating **product/eval_spec decision is now RECORDED**
> ([`docs/current/m-auto-9-escalate-after-help-product-decision.md`](current/m-auto-9-escalate-after-help-product-decision.md):
> escalate-after-genuine-help is a valid terminal under 4 conditions; `goal_impossible` alone is
> not an auto-hard-fail; false-resolve judged on authoritative `user_state` + closure evidence —
> resolving the `hard_checks.py:799` open question), and **WP2 (satisfiable UC-A entity-context
> companion — prove a recorded RESOLVE→CONFIRM→CLOSE can land) is SCOPED as Sprint 097 /
> S-Auto-45** (`docs/sprint_objective.md` + `compact/sprint-097-dev-prompt.md`; SCOPED; companion
> **`cs_uc_a_loaded_listing_resolvable`** + the 10-point acceptance contract human-BLESSED
> 2026-06-21; NOT LAUNCHED — awaits the human's paste). **WP0 (source_ids/promotion) remains
> HELD** under charter §6.
>
> **Everything below this banner is the M-Auto-7 historical record** (theme "Autoloop
> readiness and CS4 entity-context pilot"; the registry-only pilot is CLOSED — NO KEEP,
> 2026-06-19). It is preserved unchanged as the milestone's delivered outcome and is
> **not** the active contract. M-Auto-7's residual CS4 entity-context PRIMARY objective
> is carried forward by M-Auto-9. (This is a minimal active-milestone pointer sync; the
> M-Auto-7 history is not rewritten.)

---

# Milestone M-Auto-7 (historical record — CLOSED, retained below)

## 0. STATUS — registry-only pilot CLOSED, NO KEEP (2026-06-19)

> **The S-Y2 registry-only autoloop pilot (the Phase-2 CORE GATE below) is CLOSED
> — NO KEEP: registry-only control-surface limit reached for the current PRIMARY
> objective.** This is the milestone's autoloop-readiness *finding* — the
> registry-only soft-field control surface was exercised end-to-end and is
> insufficient to deliver the CS4 entity-context PRIMARY objective, because the
> shared runtime closure / `record_outcome` blocker (OQ-S93.1) independently gates
> both PRIMARY cases (`cs_uc_a_no_ad_id_ad_specific`, `cs_uc_a_loaded_listing`).
> Precise grounding conclusion: exp-90 shows the *reviewed* `$.grounding_instruction`
> hypothesis was insufficient + harmful elsewhere — NOT that every registry wording
> is impossible; the stop is justified by the runtime closure blocker, not by
> exhausting the wording space.
>
> **Successor: M-Auto-9** — runtime/orchestration closure DESIGN milestone
> (`docs/proposals/runtime-closure-record-outcome-design-milestone-oq-s93.1.md`),
> PROPOSED and awaiting design review/approval; it carries the CS4 PRIMARY
> objective forward. **Next action = M-Auto-9 design review/approval**
> (implementation NOT started as part of this fold).
>
> **Per-blocker routing (do not collapse):** `loaded_listing` UC-A→UC-B
> misclassification = registry-addressable (`discover_triage.$.procedure`, **DEFERRED**
> — cannot overcome the shared closure blocker, so revisit only after closure is
> fixed); the closure / `record_outcome` blocker + substantive-grounding salience =
> **runtime/orchestration** (M-Auto-9); `source_ids` trace projection =
> **non-blocking eval/observability** debt (separate).
>
> **Holds intact:** no exp-91, WP1-B, WP2, objective-alignment annotation,
> posterior/bounded-resampling, mutable-surface expansion, re-bless, baseline
> movement, or canonical-pointer change. exp-82 stays WITHDRAWN. Full analysis:
> `docs/diagnostics/exp-90-real-result-and-primary-control-surface-2026-06-19.md`;
> closure ledger `docs/action_bank.md` §5. The §1 table + body below are retained
> as the **historical** M-Auto-7 record (do not read as the active contract).

## 1. Milestone class

| Sub-sprint | Phase | Layer (§3.2) | §7 stanza | Per-sub-sprint Codex (§4.3) |
|---|---|---|---|---|
| S-A (CS1) | Blocker | infra (trace-contract) | EXEMPT | EXEMPT → milestone close |
| S-X (CS3) | Blocker | infra + prompt_projection | REQUIRED | defer → milestone close |
| S-Y1 (CS4 readiness: Part A+B) | Blocker | prompt_projection (A) + eval_spec (B) | REQUIRED | defer → milestone close; Part B gated by §5.6 human tiering |
| **S-Y1.5 (autoloop substrate patch)** | Blocker (pre-pilot) | infra (autoloop meta-agent) | REQUIRED | **REQUIRED** — new `pilot` config steering surface near the §1.7 line (§4.3 trigger #3) |
| **S-Y1.7 (autoloop fitness-measurement reliability)** | Blocker (pre-pilot) | infra (autoloop fitness gate / scoring) | REQUIRED | **REQUIRED** — new statistical decision surface on the fitness gate near the §1.7 / §5.4 line (§4.3 trigger #3) |
| **S-Auto-38 (escalation_compliance tier-0 split)** | Blocker (pre-pilot) | eval_spec (scoring-harness; §5.8) | REQUIRED | **REQUIRED** — done; `pass`/0 (`docs/sprints/sprint-092-codex-review.md`) |
| **Sprint 093 / S-Auto-39 (RESOLVE→CONFIRM/CLOSE phase-transition corrective, OQ-S86b.3)** — **CLOSED 2026-06-18** (`pass`/0; structural transition repaired; record_outcome 0/42 → 2nd layer OQ-S93.1) | Blocker (pre-pilot) | skill_state (runtime phase machine; frozen-surface-adjacent) | REQUIRED | **DONE** — Codex `pass`/0 (`docs/sprints/sprint-093-codex-review.md`) |
| **Sprint 094 / S-Auto-40 (WP1-A: measurement contract → conditional outcome acceptance, OQ-S93.1)** | Blocker (pre-pilot) | infra (Phase-1 eval-framework signal) + eval_spec (Phase-3 declarative conditional acceptance) | REQUIRED | **REQUIRED** — anti-hardcode kernel on the simulator instrumentation + the declarative conditional evaluator + 2 PRIMARY CaseSpec change, vs the blast-radius |
| **S-Y2 (CS4 pilot: Part C+D)** | **CORE GATE** | semantic_planner (autoloop-authored) + eval_spec/governance | REQUIRED | **REQUIRED** — autoloop output is the §1.7 binding gate (§4.3 trigger #2) |
| S-B (CS2-original) | Back-half (non-blocking, optional) | prompt_projection + thin soft cue | REQUIRED | via milestone-shared close |

Milestone-level §7: stanzas pre-filled per sub-sprint (proposal §4.1 /
§7.1 / §7.2 + CS3/CS4 §4.1 / §4.2). S-A is the only §7-EXEMPT slice.

## 2. Goal

The north star is **autoloop readiness for skill-yaml procedure
optimization, proven by running the CS4 entity-context pilot
end-to-end** — not finishing every bad-case semantic fix.

1. **Clear the four autoloop launch blockers** so the autoloop can run
   against an honest baseline with the projection slots and CaseSpecs
   it needs: CS1 (stop false-crediting `resolved` on CLOSE), CS3 (stop
   the synthesised DISCOVER placeholder burning the clarification
   budget), CS4 Part A (projection infra: `moderation_reason_available`
   + candidate-UC human-readable names), CS4 Part B (executable
   CaseSpecs from the appendix).
2. **Run the CS4 entity-context autoloop pilot (CORE GATE):** autoloop
   proposes skill-yaml procedure candidate(s) against the Part B
   CaseSpecs on the post-CS1/CS3 honest baseline; human reviews each
   under the §4.1 nine-question kernel; an accepted candidate is merged
   + re-blessed.
3. **CS2-original (perspective grounding) is additive back-half work**,
   NOT a pilot launch blocker and NOT in the core acceptance bar; it may
   close with M-Auto-7 if appetite remains, else carry to a follow-on.

## 3. Sub-sprint sequence

| # | Phase | Sub-sprint | R-item | Scope (3-sentence) | Depends on |
|---|---|---|---|---|---|
| 1 | Blocker | **S-A (CS1)** Sprint 084 / S-Auto-29 | `R-controlkernel-default-resolved-on-close-anti误杀` | Gate `ControlKernel.java:575-579` Path B CLOSE-arm resolved-stamp through `isResolvedSuccessTerminal`; leave containment null when no grounding; preserve handlingState/escalation/Path-D. 5 characterization tests; no enum value. | M-Auto-6 close (MET) |
| 2 | Blocker | **S-X (CS3)** Sprint 085 / S-Auto-30 | `R-discover-null-turn-counter-anti误杀` | Structural provenance flag (`ParsedAction.userMessageSynthesised`) set by `ActionParser.java:70-72`; the R2.a counter at `AgentRunLoopImpl.java:455` excludes synthesised placeholders; add one §1.3-soft DISCOVER cue + a `user_message_synthesised` trace diagnostic. (Proposal Option C3.A is trace-confirmed inert.) | S-A dev close (Java-test verified) |
| 3 | Blocker | **S-Y1 (CS4 readiness)** Sprint 086 / S-Auto-31 | `R-uc-a-entity-context-verify-procedure-via-autoloop` (readiness scope) | Part A: human projection infra (`moderation_reason_available` boolean + candidate-UC names + soft_signal_via_projection wiring). Part B: author appendix §2-§8 CaseSpecs (5 NEW + 2 EXTEND), reconcile placeholders to real fixtures, §5.6 tiering. NO procedure-text change (that is the pilot). | S-X dev close (Java-test verified) |
| 4 | Blocker (pre-pilot) | **S-Y1.5 (autoloop substrate patch)** Sprint 087 / S-Auto-32 | `R-autoloop-feedback-loop-thinness` | Fix P0-A (tier_breakdown passthrough) + P0-B (candidate-results passthrough w/ shadow-firewall extension) + P0-C (`pilot` block target-steering) + P1 (lessons opt-out) + per-exp pilot snapshot + 4-layer hit-rate audit, in the autoloop meta-agent prompt builder + config. Pure autoloop infra; no scoring/gate/sandbox/mutable-surface touch → NO re-bless. | S-Y1 close + S-Y2 run-1 audit |
| 4.5 | Blocker (pre-pilot) | **S-Y1.7 (autoloop fitness-measurement reliability)** Sprint 091 / S-Auto-37 — **dev+Codex CLOSED 2026-06-16** (commit `758503b6`; oracle 22-pass; autoloop pytest 348→390; Codex `pass`/0; archives `docs/sprints/sprint-091-{objective,handoff,codex-review}.md`) | `R-autoloop-fitness-measurement-reliability` (resumed M-Auto-4 scope) | Replace the fitness gate's zero-tolerance majority-flip rule (`anchor_outcome_max_drop_cases:0` + raw tier2/shadow count gates) with a stability-tiered noise-aware statistical rule (V3: tier0 floor unchanged + TIER-S majority-flip anti-误杀 floor + TIER-N Beta-Binomial/δ/BH-count + noise-aware tier2/shadow), preserving the §5.4 anti-误杀 + §1.4 tier0 floors. Inserted per §5.8 (preempts S-Y2 Part C). Acceptance = zero-LLM oracle replay of exp-66..79 (P0.7). `autoloop/**` + `config.yaml` only → NO re-bless. | S-Y2 run-1/2/3 tranches (0/14 keeps) + P0.7 calibration |
| 4.7 | Blocker (pre-pilot) | **S-Auto-38 (escalation_compliance tier-0 split)** Sprint 092 — **dev+Codex CLOSED 2026-06-18** (WP-A `57cd93c5` + WP-B `8ed65cc6`; zero-LLM replay 43/43 `PART2_DEMOTION`/0 regressions; Codex `pass`/0; gate-trust validated by exp-86 `-n1` smoke = PASS/OFF_TARGET; archives `docs/sprints/sprint-092-*`) | (inserted §5.8 framework-defect; no §5 R-item) | Split `escalation_compliance` → Part-1 (escalate-vs-don't behaviour → stays tier-0) + Part-2 (`escalation_reason_family_match` → observation-only); add a narrow unified `escalation:` override schema (zero active bindings). Stops reason-label sampling noise flipping KEEP↔DISCARD. exp-82 stays WITHDRAWN; no PRIMARY success. | S-Y2 Part C run-3 (exp-82 overturned) + OQ-E forensic |
| 4.8 | Blocker (pre-pilot) | **Sprint 093 / S-Auto-39 (RESOLVE→CONFIRM/CLOSE phase-transition corrective, OQ-S86b.3)** — **dev+Codex CLOSED 2026-06-18** (fix `033abaee`; structural transition repaired + validated; 11/22 reach CONFIRM; `record_outcome` 0/42 → SECOND layer OQ-S93.1; Codex `pass`/0; archives `docs/sprints/sprint-093-*`) | `R-resolve-confirm-transition-deadlock` (`closed-structural`) | Repaired the formal RESOLVE→CONFIRM transition (grounded FINAL_ANSWER on a prior RESOLVE turn + this turn re-delivers grounded `ANSWERED_SUBTASK` → CONFIRM) without relaxing the premature guard / max_turns / CaseSpec-exception / content heuristic. Necessary-but-not-sufficient: `record_outcome` still 0/42 → OQ-S93.1 (read-only research DONE → WP1-A). | S-Auto-38 close (MET) + acceptance review + OQ-S86b.3 forensic |
| 4.9 | Blocker (pre-pilot) | **Sprint 094 / S-Auto-40 (WP1-A: measurement contract → conditional outcome acceptance, OQ-S93.1)** — **NEXT (drafted; pending dev)** | `R-oq-s93.1-confirm-record-vs-handover` (WP1-A) | Encode the 2026-06-19 product decision (escalation-after-genuine-help is a VALID terminal when the user remains unresolved; resolve when satisfied) as a DECLARATIVE three-state conditional outcome acceptance for the 2 PRIMARY, on a POSITIVE structured user-state signal. MEASUREMENT-FIRST (current traces are UNKNOWN-only → all escalations UNKNOWN): Phase 1 persist same-call simulator state (no post-hoc LLM / no back-inference) → Phase 2 small bounded real-LLM run for decidable traces (≥1 SATISFIED + ≥3 post-help UNRESOLVED, else INCONCLUSIVE) → Phase 3 declarative conditional acceptance + zero-LLM blast-radius. CONDITIONAL_ELIGIBLE never auto-PASS (versioned adjudication artifact). No bot/prompt/reason change (WP1-B separate). Canonical pointer unchanged. | OQ-S93.1 research (DONE) + product decision |
| 5 | **CORE GATE** | **S-Y2 (CS4 pilot)** Sprint 088 / S-Auto-33 — **HELD; runs under the S-Y1.7 fitness gate; does NOT resume until the OQ-S93.1 / WP1-A scope is resolved + passes Codex** | `R-uc-a-entity-context-verify-procedure-via-autoloop` (pilot scope) | Part C: baseline run (targets fail / negative-control passes) → autoloop authors skill-yaml procedure candidate(s) → human §4.1 review → merge accepted candidate → re-run. Part D: mini re-bless + per-sub-sprint Codex + close. Fallback (§3.4) only if no candidate is acceptable. | **OQ-S93.1 / WP1-A resolution** (+ S-Y1.7 fitness gate, MET) |
| 6 | Back-half (optional, non-blocking) | **S-B (CS2-original)** Sprint 089 / S-Auto-34 | `R-user-role-projection-slot-from-listing-ownership` | Data-derived `user_role` slot (`ad_owner \| unknown`) from form-email↔listing-`posted_by` match; one-sentence soft cue into discover_triage + resolve_faq. Reuses S-Y1 candidate-UC-name infra. | S-Y2 close (additive) |

## 4. Non-goals (explicit)

- **CS2-original is NOT an autoloop pilot launch blocker** and NOT part
  of the core acceptance bar. It is additive back-half work; it may
  carry to a post-M-Auto-7 follow-on at the human's discretion.
- **D-full-issue-ledger-light** (cross-UC stash-and-resume, CS3/CS4
  §10.4) — post-close research-only workstream.
- **CS2-new empty-trace UX** — not a defect; P3 admin backlog only.
- **Not consumed by M-Auto-7** (stay backlog):
  `R-r5-citation-result-binding-grounding-strengthening`,
  `R-standalone-reconcile-entry-gate-test`,
  `R-docs-reconciliation-faq-grounding-contract-vs-must-cite-source`.
- No framework / CaseSpec-schema extension (Part B uses Path γ).
- No `containment_outcome` enum addition (C1.B deferred); no
  answer-repetition bucket (C4.D deferred). M-Auto-4 stays PAUSED.

## 5. Milestone acceptance bar

### 5.1 PRIMARY (core gate) — the CS4 entity-context autoloop pilot

The milestone closes on a **successful pilot run**, measured on the
post-CS1/CS3 honest baseline:

1. **Pilot launched + run to completion** against the Part B CaseSpecs.
2. **Baseline run (C.1)** confirms the gap is real: Tier-1 targets
   (`cs_uc_a_no_ad_id_ad_specific`, `cs_uc_a_loaded_listing`,
   `cs_uc_a_lookup_failed`) fail; the anti-误杀 negative-control
   (`cs_uc_a_generic_policy_question`) passes.
3. **Autoloop produces ≥1 skill-yaml procedure candidate**; human
   reviews each under the §4.1 nine-question kernel + §1.7 (reject any
   semantic hardcode — per-UC if-else, user_message keyword check,
   enum widening, Java guard).
4. **SUCCESS:** an accepted candidate, post-merge + re-bless, flips the
   three Tier-1 targets to PASS while the negative-control STAYS
   passing and the Tier-2 neighbor (`cs_uc_fp_loaded_moderation`)
   improves; the 2 EXTEND cases (`alice_uc_a_uc_h_misclass`,
   `wmkb_uc_a_trader_flag_secondary_uc_h`) stay green.
5. **VALID ALTERNATE OUTCOME:** if no autoloop candidate is acceptable,
   the §3.4 fallback (deliver/dev hand-authors the procedure text; same
   CaseSpec gate; same §4.1 review) is invoked in Part D — the pilot
   still produces a go/no-go + a merged, gated procedure. The pilot
   RUNNING is the gate; the candidate flip is the success metric.

### 5.2 Blocker gates (prerequisites)

- **CS1** — anchor trace `3e4f0aad-af7…`: DISCOVER→CLOSE 0-tool
  no-grounding is NOT stamped `resolved`; goal_achieved one-shot
  anti-误杀 preserved (Java characterization tests + manual trace
  review).
- **CS3** — anchor trace `33edc1eb-15d…`: synthesised placeholder does
  not increment the clarification counter; turn-3 reaches the LLM (no
  premature force-escalate); legitimate Sprint-33 clarification still
  counts.
- **CS4 readiness** — projection slots render in the "Projected
  Context" panel; 5 NEW + 2 EXTEND CaseSpecs authored, compile against
  `eval_interactive/eval_interactive/specs/schema.py`, pass §5.6
  tiering, placeholders reconciled to real fixtures.
- **S-Y1.5 (autoloop substrate)** — 7 new autoloop unit tests + ~24
  existing green; the shadow-firewall test fails when the P0-B filter is
  removed (proven); the `--dry-run -n 2` rationale checklist passes (no
  L-005 cite; ≥1 CS4 case tagged `primary`; no case_id in `after_value`);
  `scoring_code_baseline_sha` unchanged (NO re-bless); per-sub-sprint
  Codex §4.1 pass. This gate makes the §5.1 pilot's PRIMARY-SUCCESS
  probability realistic; it is NOT itself the milestone acceptance bar.

### 5.3 Additive (NOT in the acceptance bar)

- **CS2-original** — anchor trace `89f4ab98…` + new bad case
  `cs_seller_buyer_perspective_001`: ad_owner framing holds;
  ambiguous → one clarifying question; UC-A baseline no regression.
  Only if run as back-half.

Safety floor + grounding floor unchanged across the milestone (HARD
gate). Documented expected shift: CS1 lowers the resolved-stamp count
(measurement honesty, not regression).

## 6. Hard fences

**Milestone-wide (all sub-sprints):**
- No keyword / regex / if-else / enum expansion on a semantic surface
  (§1.5 / §1.7).
- No eval-side CaseSpec widening to mask a genuine bot mistake (§5.4).
- No editing of `docs/sprints/*`, `docs/archive/*`, `docs/milestones/*`.
- No system_prompt edits to encode any CS fix.
- No Java guard on `user_message` content for any of the four.

**S-A (CS1):** no new `containment_outcome` enum value (C1.B deferred);
no eval-side composite-gate edit; no touch to `isResolvedSuccessTerminal`
/ `shouldVoidResolvedStamp` internals.

**S-X (CS3):** do NOT raise `max-clarification-rounds`; do NOT edit the
shared placeholder string in `templates.yaml` / `PhaseEvaluator` /
`ActionParser` (used on legitimate slow-LLM paths); do NOT touch the
pre-LLM budget gate at `ControlKernel.java:288-318`; the DISCOVER cue
is §1.3-soft, no hard guard on empty user_message.

**S-Y1 / S-Y2 (CS4):** no per-UC if-else on user_message; no Java guard
blocking `classify_use_case(UC-A)` on removed ads; do NOT project the
raw `moderation_reason_text` (only the boolean `moderation_reason_
available`); candidate-UC name field is additive (no UC-id
rename/migration); autoloop output is a proposal — human §4.1 review is
the binding gate; the negative-control CaseSpec is the anti-误杀 gate on
autoloop candidates.

**S-B (CS2-original):** `user_role.inferred_role` stays `ad_owner |
unknown` only (no `third_party_buyer` in v1); do NOT expose raw
`posted_by` email; role inference is data-derived only (no "my ad" /
"my buyer" keyword detector); soft cue is LLM-soft (no Java enforcement).

## 7. R-items consumed / surfaced

**Consumed (promoted from §5 candidate ledger):**
- `R-controlkernel-default-resolved-on-close-anti误杀` (S-A)
- `R-discover-null-turn-counter-anti误杀` (S-X)
- `R-uc-a-entity-context-verify-procedure-via-autoloop` (S-Y1 readiness
  + S-Y2 pilot — one R-item across two sub-sprints; primary = OBS-S1
  entity-context verification; secondary = OBS-S2 UC-A/UC-FP boundary
  cue)
- `R-autoloop-feedback-loop-thinness` (S-Y1.5 — surfaced + consumed
  2026-06-09 from the autoloop mechanism audit; pre-pilot meta-agent
  feedback-loop patch P0-A/B/C + P1; NEEDS an `action_bank.md` §5 row
  added at the close bundle)
- `R-autoloop-fitness-measurement-reliability` (S-Y1.7 — surfaced +
  consumed 2026-06-16 after the S-Y2 run-1/2/3 tranches produced 0/14 keeps
  and the zero-LLM P0.7 retrospective calibration root-caused it as a
  ~92–95% false-discard rate under the n=3–5 majority-flip gate; resumes the
  PAUSED M-Auto-4 "Autoloop Fitness Measurement Reliability" scope as an
  inserted M-Auto-7 pre-pilot blocker per §5.8)
- `R-user-role-projection-slot-from-listing-ownership` (S-B,
  non-blocking back-half)

**Surfaced (deferred):**
- `D-full-issue-ledger-light` (§4 deferred; post-close research trigger)
- `R-admin-empty-trace-zero-turn-affordance` (P3; CS2-new)

## 8. Codex review plan (§4.3)

Default = milestone-shared close review over the cumulative S-A..S-B
commit range (one §4.1 nine-question kernel walk + §4.2 header).
**Per-sub-sprint Codex REQUIRED for S-Y1.5** — the new `pilot` config
steering surface sits near the §1.7 "raw eval phrase" line (§4.3 trigger
#3); must complete before S-Y2 Part C begins. **Per-sub-sprint Codex
REQUIRED for S-Y2 (the pilot)** — the autoloop-authored procedure
candidate is the §1.7 anti-hardcode binding gate (§4.3 trigger #2); must
complete before S-B begins (or before milestone close if S-B carries
out). S-A is §7-EXEMPT + §4.1-exempt (pure infra). S-X / S-Y1 / S-B fold
into the milestone-shared close.

**Re-bless cadence (batched, 2026-06-08 decision; 2026-06-09 update):**
exactly two real-LLM gates, both Claude-launched / human-reviewed —
(1) the pre-pilot baseline re-bless = the pilot's Part C.1 (DONE
2026-06-08; `m-auto-7-prepilot-baseline-20260608`, n=9, gap confirmed),
and (2) the milestone-close re-bless. Blockers proceed on Java-test +
diff verification only; there is no inter-blocker mini re-bless.
**S-Y1.5, inserted after Part C.1, needs NO re-bless** — it touches only
the autoloop meta-agent prompt builder + config (no scoring/gate/mutable-
surface; `scoring_code_baseline_sha` unchanged), so the prepilot baseline
stays valid as the S-Y2 pilot fitness baseline.

## 9. Estimated milestone duration

~3–4 calendar weeks (S-A 1-2 d; S-X 2-3 d; S-Y1 4-6 d; S-Y1.5 2-3 d;
S-Y2 pilot 3-5 d; S-B 3-5 d; + re-bless cycles). Informational, not a
gate.
