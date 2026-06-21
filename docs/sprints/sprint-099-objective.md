---
title: "Sprint 099 / S-Auto-47 (M-Auto-10 WP2) — OQ-S93.1 canonical-closure-path design decision (design-only)"
doc_tier: current-runtime
status: proposal
implementation_status: not_started
source_of_truth: this file (active sub-sprint contract); parent milestone = docs/milestone_objective.md (M-Auto-10)
last_reviewed: 2026-06-21
review_cadence: per sprint
supersedes: []
superseded_by: null
notes: >
  DRAFT — deliver-agent scoping output, PENDING HUMAN APPROVAL before launch. Second
  sub-sprint of M-Auto-10; unblocked by WP1 (Sprint 098, CLOSED COMPLETE — the §5.8
  trace-contract prerequisite). DESIGN-ONLY / READ-ONLY: produces a design verdict that
  routes OQ-S93.1 (R-oq-s93.1-confirm-record-vs-handover) to (a) adopt grounding-gated
  isResolvedSuccessTerminal as the canonical closure path + correct the overly-literal
  explicit-record_outcome trace expectation (NO runtime fix), OR (b) the explicit
  record_outcome→CONFIRM→CLOSE path is product-required AND currently defective → scope
  the conditional runtime sub-sprint WP3. Distinguishes the four mechanisms on the
  clean (post-WP1) recorded traces. Authorizes NO code / CaseSpec / eval / baseline /
  prompt / simulator change and NO real-LLM run; STOPs at the verdict. "No phase-machine
  change" remains a first-class valid outcome (route (a)). Self-contained dev/design
  prompt: compact/sprint-099-dev-prompt.md.
---

# Sprint 099 / S-Auto-47 (M-Auto-10 WP2) — OQ-S93.1 canonical-closure-path design decision

> **ARCHIVED — CLOSED COMPLETE 2026-06-21 (dev `9882b93e` + Codex design-review APPROVE
> `c602ee3f` + human Route (a) sign-off).** Verdict: **ROUTE (a)** — grounding-gated
> `isResolvedSuccessTerminal` is the canonical one-shot satisfiable RESOLVE closure;
> the explicit `record_outcome→CONFIRM→CLOSE` path is sound multi-turn (demonstrated
> landing on the PRIMARY `…015324`→CLOSE), structurally unreachable on a one-shot flow;
> mechanism (iv) ruled out; route (b) rejected. Verdict doc
> `docs/proposals/m-auto-10-wp2-closure-path-canonical-decision.md`; handoff
> `docs/sprints/sprint-099-handoff.md`. The route-(a) corrections landed in Sprint 100 /
> S-Auto-48. The text below is the historical pre-launch contract.

> **STATUS: DRAFT — pending human approval. No dev session launched.**
> Unblocked by WP1 (Sprint 098, CLOSED COMPLETE 2026-06-21 — §5.8 prerequisite satisfied).

## Class

- **§3.2 layer:** multi-layer **prospective** (design-only / read-only). Candidate
  layers per decision outcome — `eval_spec` (trace-expectation literalism) |
  `semantic_planner`/`skill_state` (CONFIRM record-vs-handover) | `java_guard`/runtime
  (premature-resolve guard) | `infra` (simulator CONFIRM-turn preempt). The verdict
  names which layer(s) any follow-on (WP3) would touch; this sub-sprint touches none.
- **iteration_governance §7 stanza:** **EXEMPT** — design/research; ships no prompt /
  runtime-semantic / eval-spec / judge behaviour. (The verdict enumerates the
  prospective layers per §7's multi-layer-prospective variant; no behaviour changes.)
- **Codex:** **design review** (read-only `codex exec`), verdict recorded into
  `docs/codex-findings.md`. NOT the §4.1 anti-hardcode kernel (no semantic surface
  shipped).

## Goal

Decide, on evidence, the **canonical RESOLVE closure path** and route OQ-S93.1 to
exactly one outcome:

- **(a)** grounding-gated `isResolvedSuccessTerminal` IS the intended canonical closure
  path → **adopt it**, correct/retire the overly-literal explicit-`record_outcome→CONFIRM→CLOSE`
  trace expectation (eval-contract/docs decision), and record the rationale. **No
  runtime fix.** ("No phase-machine change" is a first-class valid outcome, per the
  M-Auto-9 charter.)
- **(b)** the explicit `record_outcome→CONFIRM→CLOSE` tool path is **product-required
  AND currently defective** → **scope WP3** (a minimal runtime repair under full
  protections).

The decision must be backed by per-mechanism trace attribution on the **clean**
(post-WP1) recorded evidence.

## Background (the open question + the evidence we already have)

M-Auto-9 WP2 (Sprint 097 §5) demonstrated resolve-can-land 8/11 on the satisfiable
companion, but observed **two** runtime sub-paths and proved the explicit path does
not land:

1. **Grounding-gated terminal (`isResolvedSuccessTerminal`, `ControlKernel.java:605-634`):**
   the 8/11 genuine resolves land here — substantive grounded answer + simulator ends
   on `goal_achieved` → grounding-gated `containment="resolved"` (anti-误杀: never on an
   unresolved/ungrounded/escalated terminal).
2. **Explicit `record_outcome` premature-rejected (3/11):** the bot calls
   `record_outcome` in RESOLVE; the frozen premature-resolve guard rejects it
   (`progressive_resolve_record_outcome_premature`); the simulator then ends on
   `goal_achieved` **before the bot can retry in CONFIRM** → containment left empty.

The OQ-S93.1 read-only research (`docs/diagnostics/failure-briefs/oq-s93.1-confirm-record-vs-handover.md`)
established `record_outcome` 0/42 on the Sprint-093 bad-case personas was **not** a
mechanism bug (the CONFIRM skill exposes+instructs it; the guard permits it in CONFIRM)
— those personas simply never reach satisfaction. WP2 must now decide the canonical
path for the **satisfiable** case, where the explicit path is blocked by the
guard-in-RESOLVE + simulator-preempt race (sub-path 2).

## Scope (numbered, design-only / read-only)

1. **Assemble the clean evidence base (read-only, NO new run).** Use the existing
   recorded traces — the Sprint 093 bounded run (42 draws), the Sprint 097 WP2 run
   (companion ×11 + 2 PRIMARY ×11), and the M-Auto-9 §5.6 set (now contract-consistent
   post-WP1). Confirm the WP1 fix makes the relevant sessions collect cleanly
   (re-collect read-only if needed). If the recorded evidence is judged insufficient to
   decide (a) vs (b), that is a **STOP → surface to human** (do NOT authorize a fresh
   real-LLM run in this sub-sprint).
2. **Read-only runtime code map.** Document the closure-relevant path:
   `ResolveDispositionEvaluator.shouldRejectPrematureResolveOutcome` (`:160`);
   `isResolvedSuccessTerminal` (`ControlKernel.java:605-634`);
   `PhaseEvaluator.mapFinalAnswer` RESOLVE→CONFIRM promotion + CONFIRM→CLOSE;
   `confirm.yaml` (the disjunctive record-vs-handover procedure); and the simulator's
   CONFIRM-turn / `goal_achieved` preempt (`session_runner.py` ~271-272 ←
   `user_simulator` `goal_status`). State precisely which gate stops the explicit path
   on a *satisfiable* flow.
3. **Per-mechanism attribution.** For each observed non-landing of the explicit
   `record_outcome→CONFIRM→CLOSE` path, classify into the four mechanisms with cited
   trace evidence, and **quantify**:
   - (i) **premature-resolve guard behaviour** — correct-by-design rejection in RESOLVE;
   - (ii) **simulator `goal_achieved` / CONFIRM-turn preemption** — the session ends
     before the bot can retry `record_outcome` in CONFIRM (a framework race, not a bot
     fault);
   - (iii) **stale / overly-literal trace expectation** — the "explicit
     `record_outcome→CONFIRM→CLOSE`" expectation is the artefact; grounding-gated is the
     legitimate closure;
   - (iv) **genuine runtime closure defect** — a satisfied user who is NOT
     simulator-preempted still cannot reach a recorded resolve.
   The split among (i)–(iv) is the decision's backbone: (a) if non-landing is dominated
   by (ii)+(iii) (no (iv)); (b) if (iv) is demonstrated.
4. **Decision + rationale.** Route to (a) or (b) with the quantified attribution. If
   (a): specify exactly how the trace expectation is corrected/retired (which doc /
   eval-contract surface — NOT a CaseSpec bar change) and confirm grounding-gated
   `isResolvedSuccessTerminal` is a sufficient, anti-误杀-safe canonical closure. If (b):
   sketch the **WP3 charter** — the smallest legitimate runtime change + the M-Auto-9
   §4/§5 validation plan (precedence preservation; named guards `anchor_uc_g_gdpr` /
   `anchor_uc_fp_removed` / `cs095_uc_d_email_recovery_misroute` green; anti-误杀;
   bounded real-LLM V3 gate) under the inherited fences (no guard relax / no max_turns /
   no record_outcome-requirement lowering / no CaseSpec exception / no content heuristic).
5. **Write the verdict doc** at `docs/proposals/m-auto-10-wp2-closure-path-canonical-decision.md`
   (design-tier) and obtain **human + Codex design-review sign-off**.

## Hard fences / STOP conditions

- **Design-only / read-only:** NO code / CaseSpec / evaluator / scoring / baseline /
  prompt / simulator change. **NO real-LLM run.** DB/trace reads are read-only diagnosis.
- **STOP at the verdict:** do NOT implement a runtime fix in this sub-sprint. WP3 (if
  route (b)) is a separate sub-sprint requiring a fresh `sprint_objective.md` + §7
  stanza + human approval.
- **STOP — ambiguity / insufficiency:** if the recorded evidence cannot distinguish
  (a) from (b) (e.g. mechanism (iv) can be neither demonstrated nor ruled out without a
  fresh run), STOP and surface to the human with the precise gap; do NOT pick (a)/(b)
  arbitrarily and do NOT authorize a run.
- **Inherited OQ-S93.1 fences (binding even on the recommendation):** do NOT recommend
  relaxing the premature-resolve guard, raising `max_turns`, lowering the
  `record_outcome` requirement, a CaseSpec/PRIMARY exception, or a user-message-content
  heuristic as the "fix."
- **Route (a) is a legitimate outcome, not a failure** — preserve M-Auto-9's "no
  phase-machine change required" conclusion; do NOT manufacture a runtime change to
  "make the explicit path land" if the evidence says grounding-gated is canonical.

## Test / eval requirements

None (design-only; ships no behaviour). The deliverable is the verdict doc + the
per-mechanism attribution table, each claim citing a trace / code path. No suite run is
gated on this sub-sprint beyond confirming (read-only) that the WP1 fix left the
evidence base contract-consistent.

## §7 stanza

**EXEMPT** — design/research sub-sprint; no prompt / runtime-semantic / eval-spec /
judge surface changed. The verdict's prospective-layer enumeration (per §7's
multi-layer-prospective variant) lives in the verdict doc, not as a behaviour change.

## Codex review plan

**Design review** (read-only `codex exec`, deliver-agent dispatch per the
deliver-can-dispatch pattern): Codex independently assesses the per-mechanism
attribution + the (a)/(b) routing for evidentiary soundness and scope discipline.
Verdict recorded into `docs/codex-findings.md`. NOT the §4.1 anti-hardcode kernel (no
semantic surface). If route (b), the WP3 charter sketch is reviewed at WP3 launch under
the §4.1 kernel (per-sub-sprint, frozen-surface trigger).

## Handoff requirements (`docs/sprints/sprint-099-handoff.md`)

1. The clean-evidence-base confirmation (which recorded runs; that WP1 left them
   contract-consistent).
2. The read-only runtime code map (the gates on the explicit path for a satisfiable flow).
3. The per-mechanism attribution table (i)–(iv) with counts + cited traces.
4. The decision (a) or (b) + rationale; if (b), the WP3 charter sketch.
5. Pointer to the verdict doc `docs/proposals/m-auto-10-wp2-closure-path-canonical-decision.md`.
6. §8 STOP-check (none fired / which fired) + the Codex design-review verdict pointer.
7. Recommended verdict (COMPLETE design decision; route (a) closes the milestone after
   the §5.6 rerun, route (b) opens WP3).

## Commit discipline

Stage only the verdict doc + handoff (deliver-agent bundles the close docs). No code /
data / artifact files. Tree green at each boundary.

## Self-check checklist (dev/design ticks before declaring WP2 done)

- [ ] Evidence base assembled read-only; WP1-clean confirmed; NO real-LLM run.
- [ ] Runtime code map written (the exact gate on the explicit path for a satisfiable flow).
- [ ] Per-mechanism attribution (i)–(iv) quantified with cited traces.
- [ ] Decision (a)/(b) made on the attribution — or STOP-surfaced if ambiguous.
- [ ] If (b): WP3 charter sketch with the M-Auto-9 §4/§5 validation plan + inherited fences.
- [ ] No code / CaseSpec / eval / baseline / prompt / simulator change; guard not
      recommended for relaxation.
- [ ] Verdict doc written; human + Codex design-review sign-off recorded.
