# Dev/design prompt — Sprint 099 / S-Auto-47 (M-Auto-10 WP2)

> Paste-to-launch executable view of `docs/sprint_objective.md` (Sprint 099).
> Self-contained per `docs/current/process/prompt-artifact-rules.md` §9.1/§9.2 — you
> need only `AGENTS.md` (auto-loaded) + this prompt + the cited evidence/code anchors.
> **DESIGN-ONLY / READ-ONLY.** No code, CaseSpec, eval, baseline, prompt, or simulator
> change; no real-LLM run.

## 1. Role identity

You are the **design/research agent for Sprint 099 / S-Auto-47 (M-Auto-10 WP2)**.

**One-line goal:** decide the **canonical RESOLVE closure path** on the clean
(post-WP1) recorded evidence and route OQ-S93.1 to exactly one outcome —
**(a)** adopt grounding-gated `isResolvedSuccessTerminal` as canonical + correct the
overly-literal explicit-`record_outcome→CONFIRM→CLOSE` trace expectation (**no runtime
fix**), or **(b)** the explicit path is product-required AND defective → **scope WP3**
(a conditional runtime sub-sprint). This sub-sprint **ships no behaviour and STOPS at
the verdict.** "No phase-machine change" (route (a)) is a first-class valid outcome.

## 2. Read order (minimal)

1. `AGENTS.md` (auto-loaded).
2. This prompt (full contract embedded below).
3. Evidence + code anchors as needed:
   - OQ-S93.1 research: `docs/diagnostics/failure-briefs/oq-s93.1-confirm-record-vs-handover.md`.
   - Sprint 097 WP2 handoff §5 (the two sub-paths): `docs/sprints/sprint-097-handoff.md`.
   - M-Auto-9 charter (the §4/§5 protections + the "no phase-machine change" framing):
     `docs/proposals/runtime-closure-record-outcome-design-milestone-oq-s93.1.md`.
   - Recorded runs (read-only; gitignored data): the Sprint 093 bounded run, the
     Sprint 097 WP2 run, the M-Auto-9 §5.6 set (manifest pointers in
     `eval_interactive/case_specs/bad_cases/_manifest.md`).
   - Runtime code (read-only): `ResolveDispositionEvaluator.java:160`
     (`shouldRejectPrematureResolveOutcome`); `ControlKernel.java:605-634`
     (`isResolvedSuccessTerminal`); `PhaseEvaluator.mapFinalAnswer` RESOLVE→CONFIRM /
     CONFIRM→CLOSE; `server/src/main/resources/skills/confirm.yaml`; the simulator
     CONFIRM-turn/`goal_achieved` preempt (`session_runner.py` ~271-272 ←
     `user_simulator`).

## 3. Embedded contract

### Class

- **§3.2 layer:** multi-layer **prospective** (design-only). The verdict names which
  layer(s) any follow-on WP3 would touch; this sub-sprint touches none.
- **§7 stanza:** EXEMPT (design/research; ships no behaviour).
- **Codex:** design review (read-only `codex exec`, deliver-dispatched), NOT the §4.1
  kernel.

### Why this sub-sprint exists (the open question)

M-Auto-9 WP2 (Sprint 097 §5) proved resolve-can-land 8/11 on the satisfiable companion
— but via **two** runtime sub-paths, and proved the explicit path does not land:

1. **Grounding-gated terminal (`isResolvedSuccessTerminal`):** the 8/11 land here —
   substantive grounded answer + simulator ends on `goal_achieved` → grounding-gated
   `containment="resolved"` (anti-误杀-safe).
2. **Explicit `record_outcome` premature-rejected (3/11):** bot calls `record_outcome`
   in RESOLVE → frozen premature guard rejects → simulator ends on `goal_achieved`
   **before a CONFIRM retry** → containment empty.

The OQ-S93.1 research showed `record_outcome` 0/42 on the bad-case personas was NOT a
mechanism bug (skill exposes+instructs it; guard permits it in CONFIRM) — those
personas never reach satisfaction. WP2 decides the canonical path for the **satisfiable**
case, where the explicit path is blocked by the guard-in-RESOLVE + simulator-preempt race.

### Scope (numbered, read-only)

1. **Assemble the clean evidence base (NO new run).** Use the Sprint 093 (42-draw),
   Sprint 097 WP2 (companion ×11 + 2 PRIMARY ×11), and M-Auto-9 §5.6 recorded traces
   (now contract-consistent post-WP1 — re-collect read-only if needed). If the recorded
   evidence cannot decide (a) vs (b), **STOP → surface to human** (do NOT authorize a
   fresh real-LLM run here).
2. **Read-only runtime code map.** State precisely which gate stops the explicit
   `record_outcome→CONFIRM→CLOSE` path on a *satisfiable* flow:
   `ResolveDispositionEvaluator.shouldRejectPrematureResolveOutcome` (:160);
   `isResolvedSuccessTerminal` (`ControlKernel.java:605-634`); `PhaseEvaluator.mapFinalAnswer`
   promotion; `confirm.yaml` disjunction; the simulator `goal_achieved`/CONFIRM-turn
   preempt.
3. **Per-mechanism attribution (quantify, cite traces).** Classify every observed
   non-landing into:
   - (i) premature-resolve guard behaviour (correct-by-design RESOLVE rejection);
   - (ii) simulator `goal_achieved`/CONFIRM-turn preemption (framework race);
   - (iii) stale/overly-literal trace expectation (grounding-gated is the real closure);
   - (iv) genuine runtime closure defect (a satisfied, NON-preempted user still cannot
     record a resolve).
   The split is the decision backbone: (a) if dominated by (ii)+(iii) with no (iv);
   (b) if (iv) is demonstrated.
4. **Decision + rationale.** Route to (a) or (b):
   - **(a):** specify exactly how the explicit-`record_outcome` trace expectation is
     corrected/retired (which doc / eval-contract surface — NOT a CaseSpec bar change);
     confirm grounding-gated `isResolvedSuccessTerminal` is a sufficient, anti-误杀-safe
     canonical closure.
   - **(b):** sketch the WP3 charter — smallest legitimate runtime change + the M-Auto-9
     §4/§5 validation plan (precedence; named guards `anchor_uc_g_gdpr` /
     `anchor_uc_fp_removed` / `cs095_uc_d_email_recovery_misroute` green; anti-误杀;
     bounded real-LLM V3 gate) under the inherited fences.
5. **Write the verdict doc** at
   `docs/proposals/m-auto-10-wp2-closure-path-canonical-decision.md` and obtain human +
   Codex design-review sign-off.

### Hard fences / STOP conditions

- Read-only / design-only: NO code / CaseSpec / evaluator / scoring / baseline / prompt
  / simulator change; **NO real-LLM run**.
- **STOP at the verdict** — do not implement; WP3 is a separate, human-approved sub-sprint.
- **STOP on ambiguity** — if (a) vs (b) cannot be decided from recorded evidence (esp.
  if (iv) can be neither shown nor ruled out without a fresh run), surface to human with
  the precise gap; do NOT pick arbitrarily, do NOT authorize a run.
- **Inherited OQ-S93.1 fences (bind the recommendation):** do NOT recommend relaxing
  the premature guard, raising `max_turns`, lowering the `record_outcome` requirement, a
  CaseSpec/PRIMARY exception, or a user-message-content heuristic.
- **Route (a) is legitimate** — do not manufacture a runtime change if the evidence says
  grounding-gated is canonical.

### Handoff requirements (`docs/sprints/sprint-099-handoff.md`)

1. Clean-evidence-base confirmation (which runs; WP1-clean).
2. Runtime code map (the exact gate on the explicit path for a satisfiable flow).
3. Per-mechanism attribution (i)–(iv): counts + cited traces.
4. Decision (a)/(b) + rationale; if (b), the WP3 charter sketch.
5. Verdict-doc pointer.
6. §8 STOP-check + Codex design-review verdict pointer.
7. Recommended verdict (COMPLETE; (a) → milestone closes after §5.6 rerun, (b) → opens WP3).

### Commit discipline

Stage only the verdict doc + handoff. No code/data/artifact files. Tree green at each
boundary.

## 4. Self-check checklist (tick before declaring done)

- [ ] Evidence base assembled read-only; WP1-clean confirmed; NO real-LLM run.
- [ ] Runtime code map written (exact gate on the explicit path for a satisfiable flow).
- [ ] Per-mechanism attribution (i)–(iv) quantified with cited traces.
- [ ] Decision (a)/(b) made on the attribution — or STOP-surfaced if ambiguous.
- [ ] If (b): WP3 charter sketch with the M-Auto-9 §4/§5 validation plan + inherited fences.
- [ ] No code / CaseSpec / eval / baseline / prompt / simulator change; guard not
      recommended for relaxation.
- [ ] Verdict doc written; human + Codex design-review sign-off recorded.
