---
title: "M-Auto-10 WP2 — Canonical RESOLVE closure-path decision (OQ-S93.1): ROUTE (a) — grounding-gated isResolvedSuccessTerminal is canonical"
doc_tier: proposal
status: proposal
implementation_status: implemented
implemented_by: >
  Route-(a) corrections landed in Sprint 100 / S-Auto-48 (M-Auto-10 WP2
  route-(a) follow-up): companion cs_uc_a_loaded_listing_resolvable
  expected_tool_sequence record_outcome retirement + characterization-test
  update + OQ-S93.1 brief / M-Auto-9 charter / Sprint 097 handoff annotations.
  No runtime / scored-acceptance-bar change. The §4.2 OQ-S99.1 remains an
  untriggered, unscheduled Gate-D robustness / measurement-completeness question.
source_of_truth: this file (design verdict); runtime code paths + recorded WP2 traces cited inline
last_reviewed: 2026-06-21
review_cadence: per milestone
supersedes: []
superseded_by: null
notes: >
  Design verdict for Sprint 099 / S-Auto-47 (M-Auto-10 WP2). DESIGN-ONLY /
  READ-ONLY: routes OQ-S93.1 to ROUTE (a) — adopt grounding-gated
  isResolvedSuccessTerminal (ControlKernel.java:605-634) as the canonical RESOLVE
  closure path and correct the overly-literal explicit-record_outcome→CONFIRM→CLOSE
  trace expectation. NO runtime fix; "no phase-machine change" is the conclusion.
  Decision backed by per-mechanism trace attribution on the clean (post-WP1)
  recorded Sprint 097 WP2 run (companion ×11 + 2 PRIMARY ×11) + read-only DB
  reconstruction. Mechanism (iv) — a genuine runtime closure defect — is RULED OUT:
  the explicit record_outcome→CONFIRM→CLOSE path is demonstrated to land end-to-end
  on the PRIMARY (current_phase=CLOSE; record_outcome CONFIRM:ok), so it is not
  defective; it is merely structurally unreachable on a one-shot satisfiable flow
  because a satisfied user ends the session before a CONFIRM turn. NO code / CaseSpec
  / eval / baseline / prompt / simulator change; NO real-LLM run. STOPS at the
  verdict. Codex design-review + human sign-off are the pending close gates.
---

# M-Auto-10 WP2 — Canonical RESOLVE closure-path decision (OQ-S93.1)

## 0. Verdict (headline)

**ROUTE (a).** Grounding-gated `isResolvedSuccessTerminal`
(`ControlKernel.java:605-634`) **is** the intended canonical RESOLVE closure path on
a satisfiable flow. **Adopt it; correct/retire the overly-literal explicit
`record_outcome→CONFIRM→CLOSE` trace expectation. No runtime fix. No phase-machine
change.**

Mechanism **(iv) — a genuine runtime closure defect — is RULED OUT, not merely
absent.** The explicit `record_outcome→CONFIRM→CLOSE` tool path is **demonstrated to
land end-to-end** on the recorded evidence (a PRIMARY draw reaches
`current_phase=CLOSE` after `record_outcome` succeeds in CONFIRM). It is therefore
**not product-defective**; it is **structurally unreachable on a one-shot satisfiable
flow** because a genuinely satisfied user ends the session (`goal_achieved`) before a
dedicated CONFIRM turn can run. That preemption is intrinsic to the meaning of
"satisfied", not a bot or runtime fault — and grounding-gated `isResolvedSuccessTerminal`
exists precisely to record the resolution on that simulator-preempted one-shot path.

Route (a) is the M-Auto-9-charter-anticipated "no phase-machine change" outcome
(charter §6). Route (b) is **rejected** on the evidence (§5).

## 1. Clean evidence base (read-only; WP1-clean)

Per scope #1, NO new LLM run. The decision rests on the already-recorded:

- **Sprint 097 / S-Auto-45 (M-Auto-9 WP2) bounded real-LLM run** — the core block:
  the satisfiable companion `cs_uc_a_loaded_listing_resolvable` ×11 + the two PRIMARY
  (`cs_uc_a_loaded_listing` ×11, `cs_uc_a_no_ad_id_ad_specific` ×11). Result
  artifacts present on disk: `eval_interactive/results/20260621-0143…–0156…/results.json`
  (manifest `/tmp/wp2_core_manifest.txt`; gitignored data, read-only). All 33 mined
  per-draw: `per_turn_trace` (phase_plan + tool_calls), `containment_outcome`,
  `escalation_reason`, `active_use_case`, `user_state_signals`, `stop_reason`.
- **Read-only DB reconstruction** of the persisted `bot_sessions` / `bot_turns`
  (local `csagent` postgres) for the decision-critical draws — `current_phase`,
  `articles_shown`, per-turn `phase_after` / `source_ids`. (Same read-only-DB
  diagnosis method WP1/Sprint 098 used; no write.)
- **Sprint 093 / S-Auto-39 bounded run (42 draws)** + the **OQ-S93.1 research** —
  corroborating background (the explicit path was 0/42 on the *unsatisfiable*
  bad-case personas; those personas never reach satisfaction).

**WP1-clean confirmed.** The Sprint 098 / S-Auto-46 (WP1) fix narrowed the
`trace_contract_active_use_case` collector contract to exempt **pre-routing
(`INIT`/`DISCOVER`) phases only** (`collector.py`, Sprint 098 handoff §1.4/§2). I
re-mined all 33 core draws: **0 contract-violation statuses, 0 zero-turn draws, 0
`contract_warnings`** — every core draw reached `RESOLVE`+ with a committed UC and a
multi-turn trace, so the WP1 false-positive population (DISCOVER/1-turn/empty-UC) does
**not** intersect this evidence base. The companion's 8/11 genuine-resolve count and
the PRIMARY 7/7 containment counts reproduce the Sprint 097 handoff §4.1 table exactly.
The evidence is contract-consistent post-WP1; **no re-collection was required**.

The recorded evidence **decides** (a) vs (b) (§3–§5); no §8 STOP condition fired
(§7). No fresh real-LLM run was authorized.

## 2. Runtime code map — the exact gate on the explicit path for a *satisfiable* flow

The explicit `record_outcome→CONFIRM→CLOSE` path traverses four runtime gates plus one
framework gate. For a **satisfiable** (one-shot `goal_achieved`) flow the path is
stopped at **Gate E (the simulator preempt)**, not by any runtime defect:

| Gate | Code | Behaviour on a satisfiable flow | Verdict |
|---|---|---|---|
| **A. Premature-resolve guard** | `ResolveDispositionEvaluator.shouldRejectPrematureResolveOutcome` (`:160-177`) | `record_outcome(resolve)` called in **RESOLVE** is rejected (returns `true` for any phase ≠ CONFIRM/CLOSE). **Permits** it in CONFIRM/CLOSE (`:172-175`). | Correct by design (frozen Tier-0-adjacent). |
| **B. RESOLVE→CONFIRM structural promotion** | `PhaseEvaluator.mapFinalAnswer` `ANSWERED_SUBTASK` arm (`:929-935`); precondition `priorGroundedResolveAnswerDelivered` (`:807-818`) | Promotes RESOLVE→CONFIRM **only when a prior turn already delivered a grounded RESOLVE answer** (`phase_after==RESOLVE` + non-empty `source_ids`) — i.e. requires **≥2 bot turns**. | Correct by design (Sprint 093 repair). |
| **C. record_outcome-failed-retry early-return** | `mapFinalAnswer` (`:882-889`) | On the **first** grounded turn, a premature-rejected `record_outcome` early-returns `record_outcome_failed_retry` (stay RESOLVE) **before** the disposition is set (`:903-909` skipped). | Correct for retry honesty; has a side effect on Gate D (§4 note). |
| **D. Grounding-gated terminal** | `ControlKernel.isResolvedSuccessTerminal` (`:1441-1492`), called at `:600` / `:605` | Stamps `containment="resolved"` when: containment null + `terminalOutcome==FINAL_ANSWER` + disposition ∈ {`READY_TO_CONFIRM`,`ANSWERED_SUBTASK`} + `articlesShown` non-empty. **Anti-误杀 by construction** (never on unresolved/ungrounded/escalated/mid-resolution). | **The canonical closure on the one-shot path.** |
| **E. Simulator `goal_achieved` preempt** | `session_runner.py:259-269` (`goal_status=="achieved"` → `break`) ← `user_simulator` | When the user is satisfied, the session **ends immediately** after the grounded answer — **before** any CONFIRM-phase bot turn can run. | Framework race; intrinsic to "satisfied". |

The `confirm.yaml` procedure (`applicable_phases: [CONFIRM]`,
`tools_required: [record_outcome, request_handover]`) **exposes and instructs**
`record_outcome(RESOLVED)` when the user is satisfied — so once a CONFIRM **turn** runs,
the explicit path can complete. The blocker on the satisfiable one-shot flow is that
**no CONFIRM turn ever runs** (Gate E fires first): Gate B needs a second bot turn, but
Gate E ends the session at the first satisfied answer.

**Precise statement:** on a *satisfiable* flow the explicit path is stopped by the
**simulator `goal_achieved` preempt (Gate E) racing ahead of the Gate B promotion** —
**not** by Gate A (which is correct), and **not** by any defect in Gate D/the CONFIRM
skill. The explicit path is sound when the flow does reach CONFIRM (§3, PRIMARY).

## 3. Per-mechanism attribution (counts + cited traces)

### 3.1 Decision population — the satisfiable companion `cs_uc_a_loaded_listing_resolvable` (n=11)

Every one of the 11 companion attempts reaches `user_state=satisfied` /
`stop_reason=goal_achieved` (intrinsically simulator-preempted one-shot), and **0
escalate**. Closure split:

| outcome | n | mechanism | cited draws |
|---|---|---|---|
| **resolved (grounding-gated, Gate D)** | **8/11** | **(iii)** grounding-gated **is** the canonical closure | `…014438` (UC-A, CONFIRM-promoted, articles=6), `…014556`, `…014718`, `…014832`, `…014935`, `…015148`, `…015309`, `…015535` |
| **empty containment** | **3/11** | **(i)+(ii)** + a Gate-D-internal miss (NOT (iv)) | see below |

> **Shorthand clarification (2026-06-21, route-(a) follow-up).** "`…014438`
> (UC-A, **CONFIRM-promoted**)" means the DB record persisted
> `phase_after=CONFIRM` (the Gate-B structural promotion of the prior grounded
> RESOLVE turn). It does **not** mean a CONFIRM-phase `phase_plan` bot turn ran:
> the companion is `reachedCONFIRM=0/11` — no CONFIRM planner turn executed on
> any companion draw (the simulator `goal_achieved` preempt ends the session
> first). The grounding-gated terminal stamps the resolve without a CONFIRM turn.

The 3 empty draws — **all** satisfied + `goal_achieved`-preempted:

- **`…014322`** (UC-B): turn-1 `DISCOVER` clarification, turn-2 `RESOLVE` grounded
  (`search_knowledge` 3 hits + `resolve_article`) **+ `record_outcome` premature-rejected**
  (`error: progressive_resolve_record_outcome_premature`). Mechanism **(i)** premature
  guard (Gate A) **+ (ii)** preempt (Gate E). The premature attempt early-returned
  `mapFinalAnswer` (Gate C) on the first grounded turn, so the disposition was never
  set and **Gate D was suppressed**. User satisfied (`goal_achieved`).
- **`…015420`** (UC-A): two `RESOLVE`-phase `record_outcome` premature-rejections;
  containment empty. Mechanism **(i)×2 + (ii)**; Gate D not stamped. User satisfied.
- **`…015038`** (UC-B): **no `record_outcome` attempt**; clean grounded final answer
  ("…apply a Bump Up feature… For more details, see: https://help.gumtree.com/…").
  DB: `current_phase=RESOLVE`, `articles_shown=3`, `containment` empty; `bot_turns` =
  `DISCOVER→DISCOVER`, then `DISCOVER→RESOLVE` (UC-B, `source_ids=3`). The **only**
  grounded RESOLVE answer **is** the terminal turn (preceded by a DISCOVER clarifier),
  so Gate B has no prior-grounded-RESOLVE turn to promote, and **Gate D did not fire**
  at the terminal RESOLVE turn (the disposition / terminal-outcome gate was not
  satisfied — `resolve_disposition` is not a persisted column, so the exact internal
  gate is not recoverable from the trace). Mechanism **(ii)** preempt + a **Gate-D
  internal miss**. User satisfied. **NOT (iv).**

**(iv) count on the companion: 0/11.** Every empty draw is a satisfied **and**
simulator-preempted one-shot user. **None** is "a satisfied user who is **not**
preempted yet still cannot reach a recorded resolve."

### 3.2 Mechanism-soundness cross-check — the PRIMARY (rules out (iv))

The PRIMARY personas are unsatisfiable-by-construction, so they **drag past** the
first answer and **do** reach CONFIRM — exercising exactly the explicit path the
companion cannot:

- **`cs_uc_a_loaded_listing` (n=11): 8/11 reach `CONFIRM`**;
  **`record_outcome` lands `CONFIRM:ok` in 2 draws** (`…015210`, `…015324`); and
  **`…015324` reaches `current_phase=CLOSE`** (DB-confirmed) — the **full explicit
  `record_outcome→CONFIRM→CLOSE` path completing end-to-end**.
- This **demonstrates Gates A→B→D→(CONFIRM skill)→CLOSE are mechanically sound**: when
  a flow reaches CONFIRM, `record_outcome` is permitted, lands, and promotes to CLOSE.
  The 0/42 explicit-path result in the OQ-S93.1 research was the *persona never
  satisfied*, not a mechanism defect — and is now **positively confirmed** by these
  live CONFIRM/CLOSE landings.

**Caveat (and an argument *for* route (a)):** both PRIMARY `record_outcome`-in-CONFIRM
landings occurred on **unsatisfied** users (`user_state` never `satisfied`) — the
PRIMARY's pre-existing flaky **false-resolve over-stamp**. So the explicit
record_outcome-in-CONFIRM path, as observed, **over-stamped resolved on unsatisfied
users 2/2**, whereas grounding-gated Gate D is gated on grounded substance **and** the
eval scores it against `user_state=satisfied`. **Grounding-gated is therefore the
*safer* canonical path, not merely a sufficient one.**

### 3.3 Attribution summary

| mechanism | role in the closure picture | evidence |
|---|---|---|
| **(i) premature guard (Gate A)** | correct-by-design RESOLVE rejection; suppresses Gate D on the first grounded turn via the Gate-C early-return | companion `…014322`, `…015420`; 20 RESOLVE rejections across Sprint 093 (OQ-S93.1 §3.1) |
| **(ii) simulator `goal_achieved` preempt (Gate E)** | the operative blocker of the explicit path on a satisfiable flow — ends the session before any CONFIRM turn | **all 11** companion draws (`stop=goal_achieved`); companion `reachedCONFIRM=0/11` |
| **(iii) overly-literal trace expectation** | grounding-gated **is** the real one-shot closure; the literal "record_outcome must appear" is the artefact | companion 8/11 grounding-gated resolved with **only 2/11** even attempting `record_outcome` |
| **(iv) genuine runtime closure defect** | **NOT demonstrated; RULED OUT** | explicit path lands end-to-end on PRIMARY (`…015324`→CLOSE); every companion non-landing is satisfied **and** preempted |

## 4. Decision + rationale — ROUTE (a)

**The non-landing of the explicit path is dominated by (ii) (the simulator preempt)
and reframed by (iii) (the trace expectation is the artefact), with (iv) ruled out by
the PRIMARY's end-to-end explicit-path landing.** Per the scope #3 decision backbone
("(a) if dominated by (ii)+(iii) with no (iv)"), this is **route (a)**.

Grounding-gated `isResolvedSuccessTerminal` is a **sufficient, anti-误杀-safe canonical
closure** for the satisfiable flow:

- **Sufficient:** records a genuine SATISFIED+resolve terminal 8/11 (the conditional
  `correct_outcome` PASS the eval already scores against `containment_outcome` +
  `user_state=satisfied`; the eval does **not** require the `record_outcome` tool call —
  verified by `test_sprint_097_uc_a_resolvable_companion.py::test_satisfied_resolve_lands_pass`).
- **Anti-误杀-safe:** Gate D never fires on an unresolved / ungrounded / escalated /
  mid-resolution terminal (`ControlKernel.java:1450-1491`); and it is *safer* than the
  explicit path, which over-stamped on unsatisfied PRIMARY users (§3.2).
- **Canonical, not a workaround:** Gate D is the Sprint 074/075 runtime trace-contract
  completion authored for exactly this simulator-preempted `goal_achieved` one-shot
  path. The explicit `record_outcome→CONFIRM→CLOSE` path remains the **multi-turn**
  closure (demonstrated landing on the PRIMARY); it is not required, and cannot be
  reached, on a one-shot satisfiable flow.

### 4.1 Trace-expectation correction (the route-(a) deliverable) — NOT a CaseSpec bar change

Adopting (a) requires correcting/retiring the **overly-literal** expectation that the
canonical satisfiable trace must contain a landed `record_outcome`. The scored
acceptance bar is **already correct** and is **not** touched. The correction surfaces
(each a **follow-up**, not this design-only sub-sprint):

1. **Eval-contract (`eval_spec` follow-up):**
   `eval_interactive/case_specs/bad_cases/cs_uc_a_loaded_listing_resolvable.yaml`
   `expected.expected_tool_sequence` lists `record_outcome` as the required 5th tool;
   `eval_interactive/tests/test_sprint_097_uc_a_resolvable_companion.py:120-128`
   (`test_companion_tool_sequence_is_consult_then_ground_then_record`) pins it.
   **Retire `record_outcome` from the *required* one-shot canonical sequence, or
   annotate it best-effort / structurally-unreachable-on-`goal_achieved`-one-shot.**
   This is a **trace-expectation** correction, **NOT** a `conditional_outcome_acceptance`
   bar change: the scored gate (`satisfied_outcome: resolve`, reading
   `containment_outcome`) already PASSes the grounding-gated resolve without the tool
   call. No bar is widened or relaxed; the `record_outcome` requirement was never the
   scored gate.
2. **Docs (annotate the closure-path language):** the OQ-S93.1 brief §2/§3.5
   (`docs/diagnostics/failure-briefs/oq-s93.1-confirm-record-vs-handover.md`), the
   M-Auto-9 charter §3 (`docs/proposals/runtime-closure-record-outcome-design-milestone-oq-s93.1.md`),
   and the Sprint 097 handoff §5 (`docs/sprints/sprint-097-handoff.md`) all phrase the
   target as "record `record_outcome(resolve)` → CONFIRM → CLOSE". Annotate that
   **grounding-gated `isResolvedSuccessTerminal` is the canonical closure on a one-shot
   `goal_achieved` satisfiable flow**, and the explicit `record_outcome→CONFIRM→CLOSE`
   path is the **multi-turn** closure (demonstrated landing on the PRIMARY,
   `…015324`→CLOSE), not required on the one-shot case.

These corrections are **out of scope for this design-only sub-sprint** (which STOPS at
the verdict); they are named so the deliver-agent can schedule them (eval_spec
maintenance + docs reconciliation) after sign-off.

### 4.2 Optional latent-robustness follow-up (recorded, NOT required, NOT (iv))

Gate D missed **3/11** satisfied one-shot users via two **Gate-D-internal** gaps:
(1) a premature `record_outcome` attempt on the first grounded turn early-returns
`mapFinalAnswer` (Gate C, `:882-889`) **before** the disposition is set, suppressing the
Gate-D stamp (`…014322`, `…015420`); (2) a single grounded RESOLVE answer that **is**
the terminal turn (preceded by a DISCOVER clarifier) is not stamped (`…015038`). A
future, narrow runtime hardening could set the disposition on the Gate-C early-return
path so Gate D can still fire after a premature `record_outcome` attempt. This is
**measurement-completeness** (3 satisfied users left unrecorded), **NOT a closure
defect** and **NOT (iv)** — those users were already satisfied and simulator-preempted;
a non-preempted satisfied user closes (§3.2). It is surfaced as an **open question for
the human** (candidate `OQ-S99.1`), **not** manufactured into WP3, per the §6 / charter
fence against manufacturing a runtime change.

## 5. Why route (b) is rejected

Route (b) requires the explicit `record_outcome→CONFIRM→CLOSE` path to be **(b1)
product-required AND (b2) currently defective**. Both fail:

- **(b2) fails:** the path is **demonstrated to land end-to-end** (`…015324` →
  `current_phase=CLOSE`; `record_outcome` `CONFIRM:ok` in `…015210`/`…015324`). It is
  not defective — it is structurally unreachable on a one-shot satisfiable flow because
  satisfaction ends the conversation (Gate E) before CONFIRM. There is no (iv).
- **(b1) fails:** the *product* requirement is "a satisfied user is recorded as resolved
  and the case closes" — which grounding-gated Gate D already delivers (8/11, anti-误杀-safe,
  and the eval scores it as a genuine resolve). The *literal tool path* is not the product
  requirement; it is the artefact (iii).
- **The inherited OQ-S93.1 fences would block every legitimate WP3 lever anyway:** making
  the explicit path land on a one-shot satisfiable flow would require relaxing the
  premature guard (Gate A) **or** forcing a CONFIRM turn before the simulator preempt
  (raising `max_turns` / a content heuristic / a CaseSpec exception) — **all explicitly
  forbidden** (sprint_objective Hard fences; charter "Scope discipline"). The only
  fence-clean change (the §4.2 Gate-D robustness hardening) is *not* "make the explicit
  path land" and is not required for canonical closure.

The M-Auto-9 charter §6 pre-authorized this exact conclusion: *"No phase-machine change
is a first-class valid outcome … The design must NOT manufacture a runtime change to
'make resolve land' on personas that are unsatisfiable by construction."* Route (a)
honours it.

## 6. §3.2 prospective-layer enumeration (per §7 multi-layer-prospective variant)

This sub-sprint **ships no behaviour** (touches no layer). The prospective layers it
*names* for the follow-ups it surfaces:

- **Route (a) corrections:** `eval_spec` (the companion `expected_tool_sequence` +
  characterization test) + docs reconciliation (not a §3 runtime layer). **No
  `java_guard` / `semantic_planner` / `skill_state` change.**
- **Optional §4.2 robustness item (OQ-S99.1, NOT scheduled):** would be `java_guard`
  /runtime (the Gate-C early-return / Gate-D disposition interaction in
  `PhaseEvaluator.mapFinalAnswer` + `ControlKernel.isResolvedSuccessTerminal`), under
  the full M-Auto-9 §4/§5 protections — **only if** the human elects to schedule it as
  measurement-completeness hardening; it is **not** part of route (a).

No Tier-0 invariant is added; the premature-resolve guard is **not** recommended for
relaxation.

## 7. §8 STOP-check

| condition | fired? | note |
|---|---|---|
| Evidence insufficient to decide (a)/(b) → STOP | **NO** | the recorded evidence decides (a) decisively (§3–§5); (iv) is positively ruled out by the PRIMARY end-to-end landing |
| (iv) can be neither shown nor ruled out without a fresh run → STOP | **NO** | (iv) is **ruled out** on recorded evidence (`…015324`→CLOSE); no fresh run needed |
| Implement a runtime fix in this sub-sprint | **NO** | design-only; STOPS at the verdict |
| Recommend relaxing the premature guard / raising max_turns / lowering record_outcome / CaseSpec exception / content heuristic | **NO** | none recommended; route (a) needs no runtime change |
| Any code / CaseSpec / eval / baseline / prompt / simulator change; real-LLM run | **NO** | read-only throughout (trace mining + read-only DB reconstruction); zero LLM calls |

## 8. Recommended verdict

**COMPLETE (design decision) — ROUTE (a).** OQ-S93.1 is resolved: grounding-gated
`isResolvedSuccessTerminal` is the canonical RESOLVE closure path on a satisfiable
flow; the explicit `record_outcome→CONFIRM→CLOSE` path is the sound-but-multi-turn
closure (demonstrated landing on the PRIMARY) and is **not** required on the one-shot
satisfiable case. **No runtime fix; no phase-machine change.** Two route-(a)
corrections (eval_spec `expected_tool_sequence` + docs annotation) and one optional
latent-robustness OQ (OQ-S99.1) are named for deliver-agent scheduling. With this
verdict, **M-Auto-10 closes after the §5.6 bad-case rerun** (route (a) — no WP3).

**Pending close gates:** Codex design-review (read-only `codex exec`,
deliver-dispatched; verdict → `docs/codex-findings.md`) + human sign-off.
