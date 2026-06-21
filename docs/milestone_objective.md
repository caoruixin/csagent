---
title: "M-Auto-10 — Closure-path canonical resolution (trace-contract integrity → OQ-S93.1 design decision → conditional runtime repair)"
doc_tier: current-runtime
status: proposal
implementation_status: not_started
source_of_truth: this file (active-milestone pointer)
last_reviewed: 2026-06-21
review_cadence: per milestone
supersedes: []
superseded_by: null
notes: >
  DRAFT — deliver-agent scoping output, PENDING HUMAN APPROVAL before any dev
  session is launched. M-Auto-9 (runtime/orchestration closure) is CLOSED COMPLETE
  2026-06-21; M-Auto-10 picks up its two carried items in dependency order:
  (1) the §3-infra eval-framework brief R-trace-contract-active-use-case-snakecase-camelcase,
  which per §5.8 PREEMPTS — it must be resolved before the next §5.6 milestone-level
  rerun and before any new semantic sub-sprint; and (2) the OQ-S93.1 canonical-closure-path
  DESIGN question (R-oq-s93.1-confirm-record-vs-handover): is grounding-gated
  isResolvedSuccessTerminal the intended canonical closure path, or is the explicit
  record_outcome→CONFIRM→CLOSE tool path a product-required mechanism that must be
  repaired? WP2 is design-first / evidence-driven and authorizes NO runtime change;
  a runtime sub-sprint (WP3) is implementation-eligible ONLY if the design concludes
  the explicit path is product-required AND currently defective. WP0 (source_ids/
  promotion coupling) stays HELD and OUT of the critical path; the post-satisfaction
  mechanical over-escalation bad-case is a separate semantic_planner backlog item;
  doc-governance retention + action-bank cross-file sweep are housekeeping only — none
  are M-Auto-10 critical path. Preserves M-Auto-9's conclusion that no phase-machine
  change was required for its closure.
---

# M-Auto-10 — Closure-path canonical resolution

> **STATUS: DRAFT — pending human approval. No dev session has been launched.**
> The deliver-agent drafted this milestone + the first sub-sprint contract
> (`docs/sprint_objective.md`, Sprint 098 / S-Auto-46) + its dev prompt
> (`compact/sprint-098-dev-prompt.md`). WP1 launches only after the human reviews
> and approves this scope.

## 1. Milestone class (layer breakdown + §7 coverage)

| WP | Sub-sprint id | §3.2 layer | §7 stanza | Codex |
|----|---------------|-----------|-----------|-------|
| **WP1 — trace-contract integrity (PREREQUISITE)** | Sprint 098 / S-Auto-46 | `infra` (eval-framework trace contract) | **EXEMPT** (pure infra) | **EXEMPT** per iteration_governance §4.1 pure-infra clause (record exemption in verdict) |
| **WP2 — OQ-S93.1 canonical-closure-path decision (DESIGN-ONLY)** | Sprint 099 / S-Auto-47 | multi-layer **prospective** (read-only): candidate §3.2 layers per outcome — `eval_spec` (trace-expectation literalism) \| `semantic_planner`/`skill_state` (CONFIRM record-vs-handover) \| `java_guard`/runtime (premature-resolve guard) \| `infra` (simulator CONFIRM-turn preempt) | **EXEMPT** (design/research; ships no behaviour) | Design review (read-only `codex exec`), not the §4.1 anti-hardcode kernel (no semantic surface shipped) |
| **WP3 — runtime closure repair (CONDITIONAL, NOT pre-committed)** | Sprint 100 / S-Auto-48 *(reserved; only if WP2 ⇒ route (b))* | `semantic_planner`/`skill_state`/`java_guard` (touches the FROZEN phase machine) | **REQUIRED** (authored if/when scoped) | **Per-sub-sprint §4.1 REQUIRED** (§4.3 trigger #3 — frozen surface) |

The milestone is **prerequisite → design → conditional implementation**. The
ordering is not a preference — it is forced by iteration_governance §5.8 (the WP1
infra eval-framework brief preempts: no §5.6 rerun and no new semantic sub-sprint
while it is open) and by evidence comparability (WP1's spurious 0-turn
`CONTRACT_VIOLATION` sessions contaminate the very trace set WP2 must analyse).

## 2. Goal

Restore trace-contract evidence integrity, then make an **evidence-driven canonical
decision** on the RESOLVE closure path — explicit `record_outcome→CONFIRM→CLOSE`
vs grounding-gated `isResolvedSuccessTerminal` — and implement a runtime repair
**only if** the design proves the explicit path is product-required and currently
defective. M-Auto-9 demonstrated (Sprint 097 §5) that a recorded SATISFIED+resolve
terminal *can* land (8/11) — but **grounding-gated** via `isResolvedSuccessTerminal`,
not via the explicit `record_outcome→CONFIRM→CLOSE` tool path, which stays
premature-guard-blocked. M-Auto-10 closes that open question: decide which path is
canonical, and either adopt + correct the trace expectation (no runtime change) or
repair the explicit path (runtime change, fully protected).

## 3. Sub-sprint sequence

### WP1 — Sprint 098 / S-Auto-46 — trace-contract `active_use_case` snake/camel reconciliation (PREREQUISITE, implementation-eligible now)

`infra`. The L1 `trace_contract_active_use_case` check raises a 0-turn
`CONTRACT_VIOLATION` on sessions whose session-state telemetry carries camelCase
`activeUseCase` (value present in `available_keys`) but whose authoritative read
path loses it — surfaced at the M-Auto-9 §5.6 rerun as 3/54 *rotating*
violations. `eval_interactive/eval_interactive/trace/collector.py:_build_session_state`
(line ~416) already reads `g(raw, "activeUseCase", "active_use_case")`, yet the
conditional check at `collector.py:367-375` still fires — so the value lives on an
object/path the session-level read misses. WP1 root-causes (read-only) the exact
loss point, applies the **smallest** key-reconciliation fix, and proves via a
zero-LLM characterization test + a zero-LLM replay over the recorded M-Auto-9 §5.6
trace set that the spurious violation is eliminated **and no previously-correctly-scored
session's outcome changes**. No real-LLM run; no scoring/aggregation/baseline/CaseSpec
change. This is the §5.8 prerequisite that unblocks the next §5.6 rerun. **First
executable sub-sprint; dev prompt authored at `compact/sprint-098-dev-prompt.md`.**

### WP2 — Sprint 099 / S-Auto-47 — OQ-S93.1 canonical-closure-path decision (DESIGN-ONLY, evidence-driven; starts AFTER WP1 closes)

Design-only / read-only. Using the **clean** (post-WP1) recorded trace set plus a
read-only runtime code map, distinguish the four mechanisms behind "the explicit
`record_outcome→CONFIRM→CLOSE` path does not land":

1. **premature-resolve guard behaviour** — `record_outcome(resolve)` rejected in
   RESOLVE by `ResolveDispositionEvaluator.shouldRejectPrematureResolveOutcome`
   (`ResolveDispositionEvaluator.java:160`); correct-by-design or over-strict?
2. **simulator `goal_achieved` / CONFIRM-turn preemption** — the simulator ends the
   session on `goal_achieved` before the bot can retry `record_outcome` in CONFIRM
   (Sprint 097 §5 sub-path 2); a framework race, not a bot fault?
3. **stale / overly-literal trace expectation** — is the "explicit
   `record_outcome→CONFIRM→CLOSE`" expectation itself the artefact, when the
   grounding-gated `isResolvedSuccessTerminal` terminal (`ControlKernel.java:605-634`)
   is the actual, legitimate closure mechanism (Sprint 097 §5 sub-path 1, 8/11)?
4. **a genuine runtime closure defect** — a real gap that blocks a satisfied user
   from a recorded resolve even when the simulator does NOT preempt.

**Deliverable = a design verdict** routing OQ-S93.1 to exactly one of:
- **(a) grounding-gated `isResolvedSuccessTerminal` IS the intended canonical closure
  path** → adopt it, correct/retire the overly-literal explicit-`record_outcome`
  trace expectation (eval-contract/docs decision), and record the rationale. **No
  runtime fix.** ("No phase-machine change" remains a first-class valid outcome,
  per the M-Auto-9 charter.)
- **(b) explicit `record_outcome→CONFIRM→CLOSE` is product-required AND currently
  defective** → scope WP3 (a minimal runtime repair under full protections).

The verdict must cite trace evidence per mechanism and receive human + Codex
design-review sign-off. **WP2 ships no behaviour and STOPS at the verdict.**

### WP3 — Sprint 100 / S-Auto-48 — runtime closure repair (CONDITIONAL; reserved id; NOT pre-committed)

`semantic_planner`/`skill_state`/`java_guard`. Launched **only if** WP2 ⇒ route (b),
and only after a fresh `sprint_objective.md` + §7 stanza + human approval. Scope =
the smallest legitimate runtime change that lets a genuinely-satisfied user reach a
recorded resolve via the explicit path, under the **inherited M-Auto-9 fences**: do
NOT relax the premature-resolve guard, do NOT raise `max_turns`, do NOT lower the
`record_outcome` requirement, no CaseSpec/PRIMARY exception, no user-message-content
heuristic. Validated by the M-Auto-9 charter §4/§5 protections (precedence
preservation; the named regression guards `anchor_uc_g_gdpr` / `anchor_uc_fp_removed`
/ `cs095_uc_d_email_recovery_misroute` green; anti-误杀; bounded real-LLM run scored
under the S-Y1.7 V3 noise-aware rule). **If WP2 ⇒ route (a), WP3 is not launched.**

## 4. Non-goals (explicit)

- **WP0 — `source_ids` / promotion-evidence coupling — HELD, OUT of the critical
  path.** Confirmed-latent code defect (`get_customer_context` + `resolve_article`
  do not feed `BotTurn.sourceIds`; only `search_knowledge` does) but not the observed
  operative blocker (M-Auto-9 charter §2). Stays HELD; revisit only if a
  `search_knowledge`-miss draw is shown blocked by it.
- **`R-post-satisfaction-mechanical-over-escalation`** — the cs001-style mechanical
  handover after a satisfied user (surfaced by Sprint 097 §7). Separate
  `semantic_planner` bad-case backlog item; triage with the human per Path 2; NOT in
  M-Auto-10.
- **doc-governance retention cleanup + action-bank cross-file sweep** — housekeeping
  only; NOT milestone scope (do not bundle into WP1/WP2).
- No CaseSpec widen, premature-guard relax, `max_turns` raise, `record_outcome`
  requirement lowering, baseline move, canonical-pointer flip, or re-bless in WP1 or
  WP2 (inherited M-Auto-9 fences; any such change is WP3-only and fully protected).
- No simulator behaviour change (the CONFIRM-turn preempt is analysed in WP2, not
  changed; if WP2 finds it is a framework issue, that surfaces as a new R-item, not
  an in-flight scope expansion).

## 5. Milestone acceptance bar (anchored to the curated bad-case suite)

1. **WP1 (hard):** on the recorded M-Auto-9 §5.6 trace set, the rotating 0-turn
   `trace_contract_active_use_case` `CONTRACT_VIOLATION`s are eliminated; a zero-LLM
   replay shows **no previously-correctly-scored session's outcome changed** (evidence
   comparability restored, not altered); a real-missing-UC session still violates
   (the genuine contract preserved); eval_interactive pytest green.
2. **WP2 (hard):** a human + Codex-blessed design verdict routing OQ-S93.1 to (a) or
   (b), distinguishing the four §3-WP2 mechanisms on **clean** (post-WP1) trace
   evidence.
3. **Conditional (WP3, only if route (b)):** the runtime repair passes the M-Auto-9
   charter §4/§5 protections (precedence + named guards green + anti-误杀 + bounded
   real-LLM V3 gate); the explicit `record_outcome→CONFIRM→CLOSE` path lands a
   recorded resolve for a genuinely-satisfied user **without** relaxing any fence. If
   route (a): the canonical-path decision + corrected trace expectation is recorded
   (eval-contract/docs), no runtime fix.
4. **Milestone close (hard):** the §5.6 curated bad-case suite rerun (which REQUIRES
   WP1 done first, per §5.8) shows **no regression** vs the M-Auto-9 close baseline,
   with the safety and grounding hard floors at 100%; milestone-shared Codex `pass`/0.

## 6. Hard fences (milestone level)

- **§5.8 framework-defect priority:** while the WP1 infra trace-contract brief is
  open, do NOT launch a semantic sub-sprint and do NOT perform a §5.6 bad-case rerun.
  WP2 (read-only design) and WP3 (semantic) both wait on WP1 close.
- **WP2 is design-only:** it produces a verdict + (if route (b)) a WP3 charter; it
  STOPS at the verdict and ships no runtime/eval/prompt/CaseSpec/baseline change.
- **WP3 is not pre-committed:** no runtime work is authorized until WP2 concludes
  route (b) AND the human approves a fresh `sprint_objective.md` with a §7 stanza.
- **Frozen surfaces** (`ResolveDispositionEvaluator`, `isResolvedSuccessTerminal`,
  the premature-resolve guard, `PhaseEvaluator` promotion logic) are touched only in
  WP3-if-(b), with the full §4/§5 anti-hardcode + cross-UC protections + human
  sign-off.
- No edits to existing CaseSpec families per cascade fence; no Tier-0 invention
  without `human_review_required`.

## 7. R-items consumed / surfaced

- **Consumed:** `R-trace-contract-active-use-case-snakecase-camelcase` (WP1);
  `R-oq-s93.1-confirm-record-vs-handover` (WP2 design decision; WP3 if route (b)).
- **Expected to surface:** possibly a framework R-item if WP2 attributes the
  non-landing to the simulator CONFIRM-turn preempt (mechanism 2); possibly an
  eval-contract note if route (a) requires retiring the explicit-`record_outcome`
  trace expectation.
- **Explicitly NOT consumed (stay open in `action_bank.md` §5):** WP0 source_ids
  coupling (HELD); `R-post-satisfaction-mechanical-over-escalation` (separate
  semantic backlog).

## 8. Codex review plan (§4.3)

- **WP1:** Codex-**exempt** per iteration_governance §4.1 pure-infra exemption
  clause; the exemption is recorded in the sub-sprint handoff verdict.
- **WP2:** a **design review** (read-only `codex exec`, per the deliver-can-dispatch
  pattern), verdict recorded into `docs/codex-findings.md`; not the §4.1 anti-hardcode
  kernel (WP2 ships no semantic surface).
- **WP3 (if launched):** **per-sub-sprint §4.1 anti-hardcode kernel REQUIRED**
  (§4.3 trigger #3 — touches the explicitly-hard-fenced frozen phase machine).
- **Milestone close:** milestone-shared §4.1 Codex over the cumulative WP1(+WP2+WP3)
  commit range.

## 9. Estimated milestone duration (informational)

~2–3 sub-sprints. WP1 + WP2 are the committed arc (1 infra + 1 design); WP3 is
conditional on the WP2 verdict. If WP2 ⇒ route (a), the milestone closes after WP2 +
the §5.6 rerun (2 sub-sprints). If WP2 ⇒ route (b), WP3 adds one runtime sub-sprint.

## 10. One milestone vs two — decision

**One milestone with staged sub-sprints** (recommended), NOT two milestones. The two
carried items are causally linked: WP1 is the *prerequisite that restores evidence
comparability for* WP2 (and §5.8 forces the ordering). They share a single theme —
"resolve the closure-path canonical question on clean evidence." Splitting them into
two milestones would obscure the dependency and duplicate planning/close overhead for
what is a 2–3 sub-sprint arc that fits the §8.1 milestone shape cleanly. The WP1→WP2
boundary is a hard gate (§5.8), and the WP2→WP3 boundary is a branch (route (a) vs
(b)); both are expressed as staged sub-sprints within one milestone rather than as
milestone boundaries.
