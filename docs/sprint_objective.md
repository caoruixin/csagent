---
title: Sprint 084 / S-Auto-29 (M-Auto-7 S-A) — CS1 default-resolved-on-CLOSE anti-误杀 gate
doc_tier: current-runtime
status: current
implementation_status: not_started
source_of_truth: this file
last_reviewed: 2026-06-08
review_cadence: per sprint
supersedes: docs/sprints/sprint-083-objective.md
superseded_by: null
notes: >
  First autoloop launch blocker of milestone M-Auto-7 (Autoloop
  readiness and CS4 entity-context pilot). Pure-infra runtime
  trace-contract fix. Source:
  docs/solutions/2026-06-07-cs1-default-resolved-cs2-user-role-projection-gap.md
  §3.1 + §4.1 (Option C1.A) + §6.1 + §7.1. Lands FIRST so the autoloop
  pilot's baseline (S-Y2 Part C.1) is measured on an honest floor —
  before CS3, the CS4 readiness blocker, the pilot, and CS2-original.
---

# Sprint 084 / S-Auto-29 — CS1 default-resolved-on-CLOSE gate

## Class

- **Layer (§3.2):** infra (Runtime trace-contract / persistence).
- **§7 stanza:** EXEMPT (pure infra + characterization-test; no prompt,
  no semantic decision, no eval-spec, no judge change). The §7.1 stanza
  is recorded below for the audit trail per proposal §7.1 but is not a
  §7 requirement.
- **Milestone role:** Phase 1 autoloop launch blocker #1.

## Goal

The runtime must not credit `containment_outcome="resolved"` on a
phase=CLOSE transition that delivered no grounded answer. Gate the
`ControlKernel.java:575-579` Path B CLOSE-arm stamp through the same
`isResolvedSuccessTerminal` gate Path C already uses; when no grounding
evidence exists, leave containment null so the eval routes
`case_passed` by L2 evidence instead of a false default.

## Scope

1. **Rewrite the CLOSE arm at `ControlKernel.java:575-581`** (inside
   the `if ("CLOSE".equals(phaseAfter))` block) — CANONICAL SHAPE:
   inline the gate into the CLOSE arm.
   - Keep `session.setHandlingState("CLOSED")` UNCONDITIONAL (the
     handlingState side-effect must fire on every CLOSE transition).
   - Stamp `"resolved"` ONLY when `getContainmentOutcome() == null &&
     isResolvedSuccessTerminal(session, runResult)`; otherwise leave
     containment null (no grounding evidence on CLOSE).
   - Keep `eventEmitter.emitSessionClosed(...)` emitting the (possibly
     null) containment.
   - Preserve the existing non-null containment guard (a prior turn's
     stamp is never overwritten).
2. **Update the `:572-574` D16.D comment** to reflect that the
   legacy evaluateClose mirror is now grounding-gated.
3. **Do NOT modify** `isResolvedSuccessTerminal` (`:1418`),
   `shouldVoidResolvedStamp` (`:1508`), `voidResolvedStamp` (`:1532`),
   the Path C body (`:582-611`), or the Path D body (`:612-641`).
4. **Characterization tests** (pin all 5 — mandatory):
   1. DISCOVER→DISCOVER→DISCOVER→CLOSE, 0 tool calls, no UC committed
      → containment is NOT `"resolved"` (null).
   2. RESOLVE → FINAL_ANSWER + ANSWERED_SUBTASK + non-empty
      articlesShown + LLM emits CLOSE → containment IS `"resolved"`
      (goal_achieved one-shot anti-误杀 preserved).
   3. RESOLVE → FINAL_ANSWER WITHOUT articlesShown → CLOSE →
      containment is NOT `"resolved"`.
   4. Escalation arm + CLOSE → containment stays `"escalated"`.
   5. Prior turn stamped `"resolved"` + this turn's CLOSE transition →
      value is NOT overwritten/regressed (non-null guard).
   Also assert `handlingState == "CLOSED"` in scenarios 1-3,5 (the
   side-effect fires regardless of the containment outcome).
5. **(Bonus, may defer if scope tightening needed)** emit an
   observability field `containment_reason="no_grounding_evidence_on_
   close"` on the null-containment CLOSE path. Defer if it widens the
   diff materially.

## Binding contract (Decision 3, human-approved 2026-06-08)

- `handlingState=CLOSED` remains UNCONDITIONAL on CLOSE.
- `emitSessionClosed` still fires.
- `"resolved"` is stamped ONLY when containment is null AND
  `isResolvedSuccessTerminal(session, runResult)` is true.
- No grounding evidence → containment remains null.
- Escalation behaviour preserved.
- Prior non-null containment preserved.
- goal_achieved one-shot resolved behaviour preserved.
- The five characterization tests are mandatory.

## Hard fences / STOP conditions

- No new `containment_outcome` enum value (Option C1.B deferred).
- No `semantic_planner` edit — the LLM's CLOSE-transition decision is
  unchanged; the runtime just stops false-crediting it.
- No keyword / regex / if-else dispatch on `user_message` content.
- No eval-side composite-gate edit (`composite.py:285-303` is the §5.4
  backstop, not this sub-sprint's lever).
- No touch to `isResolvedSuccessTerminal` / `shouldVoidResolvedStamp`
  internals.
- STOP and surface as an OQ if the goal_achieved one-shot path
  (test #2) cannot be preserved by the gated stamp — do NOT weaken the
  gate to force it green.

## Test / eval requirements

- New/updated Java characterization tests pin all 5 scenarios above
  (co-locate with existing ControlKernel containment tests).
- Focused Java suite: no new regression vs the inherited baseline
  (`1358 / 1 / 0 / 2`; sole known failure = OQ-S41.5 prompt tiebreaker,
  provably uncoupled — this sub-sprint touches no prompt file). Report
  exact counts.
- Mini re-bless (deliver-agent + human, post-dev) on the curated
  bad-case suite + CS1 anchor trace `3e4f0aad-af7…`: confirm the
  resolved-stamp count drops on stalled-CLOSE sessions and the
  goal_achieved one-shot resolved stamps are unchanged. Document the
  expected resolved-count delta in handoff §12 so milestone-close
  review reads the drop as honesty, not regression.

## §7.1 stanza (audit-trail; sub-sprint is §7-EXEMPT)

**Target failure layer:** infra
**Tier-0 invariant:** none added. Extends the existing §1.4 Runtime
trace-contract (containment_outcome must reflect actually-delivered
grounding evidence) into the previously-unguarded CLOSE-transition arm.
**Semantic hardcode:** none. Reuses the `isResolvedSuccessTerminal`
gate validated by Sprint 075 / S-Auto-20. No keyword/regex/if-else on
user_message, no enum expansion.
**Generalization coverage:** T/N/G/S = 1 / 1 / 1 / ≥1 — target = CS1
trace `3e4f0aad-af7…` + curated DISCOVER-stalled traces; neighbor =
goal_achieved one-shot (still resolved); negative = escalation stays
escalated + Path D unchanged; shadow = held-out DISCOVER-stalled traces.

## Codex review plan (§4.3)

Per-sub-sprint Codex EXEMPT (pure infra + characterization-test; §4.1
exemption clause + §4.3 final paragraph). Folds into the M-Auto-7
milestone-shared close review over the cumulative commit range. No
§4.3 per-sub-sprint trigger fires (not Tier-0; not §1.7-adjacent — the
change REMOVES a default stamp, adds no hardcode; not hard-fence; not
fix-iteration). Dev does NOT dispatch Codex.

## Handoff requirements

Dev authors `docs/sprints/sprint-084-handoff.md` at close:
- §0 evidence: 5 characterization tests green; focused Java suite delta.
- §11: per-sub-sprint Codex deferral recorded (EXEMPT) + any OQs.
- §12: the resolved-stamp count delta (before/after on the bad-case
  suite + anchor trace), framed as expected measurement honesty.

## Commit discipline

Stage only authorized-scope files (`ControlKernel.java` + the
characterization test file). Do NOT `git add -A`; deliver-agent files
bundle at close per §8.7. New commit per fix; no `--amend` across a
failed hook.
