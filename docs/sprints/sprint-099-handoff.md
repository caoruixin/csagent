---
title: "Sprint 099 / S-Auto-47 (M-Auto-10 WP2) — design handoff: OQ-S93.1 canonical-closure-path decision = ROUTE (a)"
doc_tier: sprint-archive
status: current
implementation_status: not_started
source_of_truth: this file (design handoff) + docs/proposals/m-auto-10-wp2-closure-path-canonical-decision.md (verdict) + the cited recorded WP2 traces + runtime code paths
last_reviewed: 2026-06-21
review_cadence: ad hoc
supersedes: []
superseded_by: null
notes: >
  Design-only / read-only handoff for Sprint 099 / S-Auto-47 (M-Auto-10 WP2). Routes
  OQ-S93.1 to ROUTE (a): grounding-gated isResolvedSuccessTerminal is the canonical
  RESOLVE closure path; the overly-literal explicit record_outcome→CONFIRM→CLOSE trace
  expectation is corrected/retired. NO runtime fix, NO phase-machine change. Mechanism
  (iv) is RULED OUT — the explicit path lands end-to-end on the PRIMARY
  (current_phase=CLOSE; record_outcome CONFIRM:ok). NO code / CaseSpec / eval / baseline
  / prompt / simulator change; NO real-LLM run. STOPS at the verdict. Codex
  design-review + human sign-off are the pending close gates.
---

# Sprint 099 / S-Auto-47 (M-Auto-10 WP2) — design handoff

**Sub-sprint:** M-Auto-10 WP2 — OQ-S93.1 canonical-closure-path design decision.
**Class:** multi-layer **prospective** (design-only / read-only). §7 stanza **EXEMPT**.
**Decision:** **ROUTE (a)** — adopt grounding-gated `isResolvedSuccessTerminal`; correct
the explicit-`record_outcome` trace expectation; **no runtime fix**.
**Verdict doc:** `docs/proposals/m-auto-10-wp2-closure-path-canonical-decision.md`.

## 0. One-line result

**ROUTE (a).** Grounding-gated `isResolvedSuccessTerminal` (`ControlKernel.java:605-634`)
is the canonical RESOLVE closure on a satisfiable flow. The explicit
`record_outcome→CONFIRM→CLOSE` path is **sound but multi-turn** — **demonstrated to land
end-to-end on the PRIMARY** (`…015324` reaches `current_phase=CLOSE`; `record_outcome`
`CONFIRM:ok` in 2 PRIMARY draws) — and is **structurally unreachable on a one-shot
satisfiable flow** because a satisfied user ends the session (`goal_achieved`) before a
CONFIRM turn. **Mechanism (iv) (genuine runtime closure defect) is RULED OUT.** No
phase-machine change; route (b) rejected. **No §8 STOP fired.**

## 1. Clean-evidence-base confirmation (which runs; WP1-clean)

- **Primary evidence:** Sprint 097 / S-Auto-45 WP2 bounded real-LLM run — companion
  `cs_uc_a_loaded_listing_resolvable` ×11 + PRIMARY `cs_uc_a_loaded_listing` ×11 +
  `cs_uc_a_no_ad_id_ad_specific` ×11. Artifacts on disk
  `eval_interactive/results/20260621-0143…–0156…/results.json` (manifest
  `/tmp/wp2_core_manifest.txt`; gitignored, read-only). Mined all 33 per-draw.
- **DB reconstruction (read-only):** persisted `bot_sessions` / `bot_turns` (local
  `csagent` postgres) for the decision-critical draws — `current_phase`,
  `articles_shown`, per-turn `phase_after` / `source_ids`. Read-only diagnosis only.
- **Corroborating:** Sprint 093 bounded run (42 draws) + the OQ-S93.1 research.
- **WP1-clean:** re-mined all 33 core draws → **0 contract-violation statuses, 0
  zero-turn draws, 0 `contract_warnings`**; every draw reached `RESOLVE`+ with a
  committed UC + multi-turn trace. The WP1 (Sprint 098) collector fix targets only
  pre-routing (`INIT`/`DISCOVER`) 1-turn/empty-UC sessions, which do **not** intersect
  this evidence base. Companion 8/11 + PRIMARY 7/7 counts reproduce Sprint 097 §4.1
  exactly. **No re-collection required.** **NO real-LLM run.**

## 2. Runtime code map (the exact gate on the explicit path for a satisfiable flow)

Five gates (verdict §2). On a *satisfiable* one-shot flow the explicit path is stopped
at **Gate E**, not by any runtime defect:

- **Gate A — premature-resolve guard** (`ResolveDispositionEvaluator.shouldRejectPrematureResolveOutcome:160-177`):
  rejects `record_outcome(resolve)` in RESOLVE; **permits** it in CONFIRM/CLOSE
  (`:172-175`). Correct by design.
- **Gate B — RESOLVE→CONFIRM promotion** (`PhaseEvaluator.mapFinalAnswer:929-935`;
  precondition `priorGroundedResolveAnswerDelivered:807-818`): promotes only with a
  **prior** grounded RESOLVE turn → needs **≥2 bot turns**.
- **Gate C — record_outcome-failed-retry early-return** (`mapFinalAnswer:882-889`): a
  premature-rejected `record_outcome` on the first grounded turn early-returns **before**
  the disposition is set (side-effect on Gate D — §4 note).
- **Gate D — grounding-gated terminal** (`ControlKernel.isResolvedSuccessTerminal:1441-1492`,
  called at `:600`/`:605`): stamps `resolved` on FINAL_ANSWER + disposition ∈
  {READY_TO_CONFIRM, ANSWERED_SUBTASK} + non-empty `articlesShown`. Anti-误杀 by
  construction. **The canonical one-shot closure.**
- **Gate E — simulator `goal_achieved` preempt** (`session_runner.py:259-269` ←
  `user_simulator`): satisfied → session ends **before** any CONFIRM turn.

`confirm.yaml` exposes+instructs `record_outcome(RESOLVED)` once a CONFIRM **turn** runs.
**Precise gate:** on a satisfiable flow, **Gate E races ahead of the Gate B promotion**,
so no CONFIRM turn runs — **not** Gate A, **not** a CONFIRM-skill/Gate-D defect.

## 3. Per-mechanism attribution (i)–(iv): counts + cited traces

**Companion (n=11, the satisfiable closure population)** — all `satisfied`/`goal_achieved`,
0 escalate:

- **8/11 resolved via grounding-gated (Gate D) → mechanism (iii)** (grounding-gated IS
  the canonical closure; the literal record_outcome expectation is the artefact). Only
  **2/11** even attempted `record_outcome`. Cited: `…014438` (UC-A, CONFIRM-promoted,
  articles=6) + 7 others.
- **3/11 empty → mechanism (i)+(ii) + Gate-D-internal miss; NOT (iv):**
  - `…014322` (UC-B): premature `record_outcome` REJ in RESOLVE **(i)** + preempt **(ii)**;
    Gate C early-return suppressed Gate D. Satisfied.
  - `…015420` (UC-A): 2× premature REJ **(i)** + preempt **(ii)**. Satisfied.
  - `…015038` (UC-B): **no** `record_outcome`; only grounded RESOLVE answer was the
    terminal turn (turn-1 = DISCOVER clarifier; `bot_turns`: DISCOVER→DISCOVER,
    DISCOVER→RESOLVE src=3; `current_phase=RESOLVE`, `articles_shown=3`, containment
    empty) → Gate-D-internal miss **(ii)**. Satisfied.
- **(iv) on companion: 0/11** — every empty is satisfied **and** preempted.

**PRIMARY cross-check (rules out (iv)):** `cs_uc_a_loaded_listing` (n=11) **8/11 reach
CONFIRM**; **`record_outcome` lands `CONFIRM:ok` in `…015210` + `…015324`**; **`…015324`
reaches `current_phase=CLOSE`** (DB-confirmed) — the **full explicit
record_outcome→CONFIRM→CLOSE path completing**. The mechanism is sound when a flow
reaches CONFIRM. (Caveat: both landings were on **unsatisfied** PRIMARY users — the
PRIMARY's pre-existing false-resolve over-stamp — so grounding-gated Gate D, scored
against `user_state=satisfied`, is the *safer* canonical path.)

| mechanism | verdict | key evidence |
|---|---|---|
| (i) premature guard | correct; suppresses Gate D via Gate C on first grounded turn | `…014322`, `…015420` |
| (ii) simulator preempt | **operative blocker** of the explicit path on satisfiable flows | all 11 companion `goal_achieved`; companion CONFIRM-reached = 0/11 |
| (iii) overly-literal expectation | the real one-shot closure is Gate D | companion 8/11 grounding-gated; only 2/11 attempt record_outcome |
| (iv) genuine closure defect | **RULED OUT** | PRIMARY `…015324`→CLOSE; explicit path lands end-to-end |

## 4. Decision (a)/(b) + rationale

**ROUTE (a).** Non-landing of the explicit path is **dominated by (ii)** and **reframed
by (iii)**, with **(iv) ruled out** by the PRIMARY end-to-end landing — matching the
scope #3 backbone "(a) if dominated by (ii)+(iii) with no (iv)". Grounding-gated
`isResolvedSuccessTerminal` is **sufficient** (8/11 genuine resolves; the eval scores
`containment_outcome`+`user_state`, not the tool call —
`test_satisfied_resolve_lands_pass`) and **anti-误杀-safe** (and *safer* than the
explicit path, which over-stamped unsatisfied PRIMARY users 2/2).

**Trace-expectation correction (route-(a) deliverable; follow-ups, NOT this sub-sprint):**
1. **eval_spec:** `cs_uc_a_loaded_listing_resolvable.yaml` `expected.expected_tool_sequence`
   + `test_sprint_097_uc_a_resolvable_companion.py:120-128` — retire/annotate the
   required `record_outcome` step. **NOT** a `conditional_outcome_acceptance` bar change
   (the scored gate already PASSes the grounding-gated resolve without record_outcome).
2. **docs:** annotate the closure language in the OQ-S93.1 brief §2/§3.5, M-Auto-9
   charter §3, Sprint 097 handoff §5 — grounding-gated is canonical on the one-shot
   `goal_achieved` flow; explicit path is the multi-turn closure.

**Optional latent-robustness OQ (OQ-S99.1; NOT scheduled, NOT (iv)):** Gate D missed
3/11 satisfied one-shot users (premature-REJ disposition-skip ×2; Gate-D-internal miss
×1). A narrow future hardening (set the disposition on the Gate-C early-return path so
Gate D fires after a premature attempt) is **measurement-completeness**, not a closure
defect — surfaced for the human, **not** manufactured into WP3.

**No WP3 charter sketch** — route (a) opens no runtime sub-sprint (route (b) rejected,
verdict §5: explicit path is not defective; the literal tool path is not the product
requirement; every legitimate WP3 lever is fence-blocked).

## 5. Verdict-doc pointer

`docs/proposals/m-auto-10-wp2-closure-path-canonical-decision.md` (design-tier;
`status: proposal`, `implementation_status: not_started`).

## 6. §8 STOP-check + Codex design-review verdict pointer

- **§8 STOP-check — none fired.** Evidence decides (a) decisively; (iv) positively
  ruled out on recorded evidence (no fresh run needed); no runtime fix implemented; no
  forbidden lever recommended; read-only throughout (trace mining + read-only DB
  reconstruction; zero LLM calls). Detail: verdict §7.
- **Codex design-review:** **PENDING (deliver-dispatched).** Read-only `codex exec`
  design review of the per-mechanism attribution + the (a)/(b) routing for evidentiary
  soundness + scope discipline (NOT the §4.1 anti-hardcode kernel — no semantic surface
  shipped). Verdict to be recorded into `docs/codex-findings.md`; pointer to be inserted
  here at close.
- **Human sign-off:** PENDING.

## 7. Recommended verdict

**COMPLETE (design decision) — ROUTE (a).** OQ-S93.1 resolved: grounding-gated
`isResolvedSuccessTerminal` is the canonical RESOLVE closure; no runtime fix; no
phase-machine change. Two route-(a) corrections (eval_spec + docs) and one optional OQ
(OQ-S99.1) named for deliver-agent scheduling. **M-Auto-10 closes after the §5.6
bad-case rerun** (route (a) — no WP3), once Codex design-review + human sign-off land.

## 8. Commit discipline

Staged: this handoff + the verdict doc only. No code / data / artifact files. Tree green
at each boundary.
