---
title: Sprint 084 / S-Auto-29 handoff — CLOSE-arm containment grounding gate
doc_tier: sprint-archive
status: current
implementation_status: implemented
source_of_truth: server/src/main/java/com/gumtree/csagent/service/runtime/ControlKernel.java
last_reviewed: 2026-06-08
review_cadence: ad hoc
supersedes: []
superseded_by: null
notes: >
  Pure-infra runtime trace-contract fix (M-Auto-7 autoloop launch blocker #1).
  Gates the agent-loop phase=CLOSE resolved-stamp through
  isResolvedSuccessTerminal so an ungrounded CLOSE no longer auto-credits
  containment_outcome="resolved". §7-EXEMPT, per-sub-sprint-Codex-EXEMPT
  (folds into M-Auto-7 milestone-shared review). One staging deviation flagged
  in §11 (the pre-existing AgentRunLoopConfirmCloseIntegrationTest required a
  reconciliation edit — a 3rd touched file beyond the contract's 2-file stage
  set).
---

# Sprint 084 / S-Auto-29 — CLOSE-arm containment grounding gate

## §0 — Cold-start evidence table

| Item | Result |
|------|--------|
| **Scope** | Route the agent-loop `phase=CLOSE` resolved-stamp through `isResolvedSuccessTerminal` so the runtime stops crediting `containment_outcome="resolved"` on CLOSE transitions that delivered no grounded answer. |
| **Layer (§3.2)** | `infra` — runtime trace-contract completion. Removes a default stamp; adds no hardcode, no enum value, no semantic decision. |
| **Code changed** | `server/.../runtime/ControlKernel.java` CLOSE arm (the D16.D legacy-mirror): `containment` is stamped `"resolved"` **only** when `containment == null && isResolvedSuccessTerminal(session, runResult)`. `handlingState="CLOSED"` and `emitSessionClosed(...)` remain UNCONDITIONAL. D16.D comment updated to state the mirror is now grounding-gated. |
| **Gates untouched** | `isResolvedSuccessTerminal` (:1418), `shouldVoidResolvedStamp` (:1508), `voidResolvedStamp` (:1532), Path C body, Path D body — all unmodified. No prompt / skill / eval / `composite.py` / enum edit. |
| **New characterization tests** | `server/.../integration/AgentRunLoopCloseContainmentGroundingGateIntegrationTest.java` — 5 tests, all green. |
| **Test 1** — DISCOVER→CLOSE, no grounding, no UC → containment **null** (not resolved); `handlingState=CLOSED`. | PASS |
| **Test 2** — FINAL_ANSWER + ANSWERED_SUBTASK + non-empty articlesShown → CLOSE → containment **"resolved"** (goal_achieved one-shot anti-误杀). | PASS |
| **Test 3** — FINAL_ANSWER + EMPTY articlesShown → CLOSE → containment **null**; `handlingState=CLOSED`. | PASS |
| **Test 4** — prior `"escalated"` + CLOSE → containment **"escalated"** (non-null guard, not overwritten). | PASS |
| **Test 5** — prior `"resolved"` + CLOSE → containment **"resolved"** (not regressed); `handlingState=CLOSED`. | PASS |
| **Focused unit gates** | `ControlKernelResolvedSuccessTerminalTest` 19/0/0, `ControlKernelVoidResolvedStampTest` 12/0/0 — both green, untouched. |
| **Full Java module suite** | `mvn -o test` → **1363 run / 1 failure / 0 errors / 2 skipped**. |
| **Delta vs inherited baseline `1358 / 1 / 0 / 2`** | **+5 run** = the 5 new characterization tests. Failures/errors/skipped unchanged. **No new regression.** |
| **Sole failure** | `SystemPromptUserRequestedTiebreakerTest.systemPrompt_marksActiveUcTiebreakerExplicitly` — the documented inherited OQ-S41.5 prompt tiebreaker test. Provably uncoupled: this sub-sprint touches no prompt file. |
| **§7 stanza** | §7-EXEMPT (pure infra + characterization-test). |
| **Codex** | Per-sub-sprint EXEMPT; folds into M-Auto-7 milestone-shared close review (see §11). |

## §11 — Codex deferral + open questions

**Codex (§4.3):** per-sub-sprint Codex review is EXEMPT for this sub-sprint
— not a Tier-0 candidate, not §1.7-adjacent (the change REMOVES a default
stamp and adds no hardcode), not a hard-fence violation, not a fix-iteration.
Deferred into the M-Auto-7 milestone-shared close review. Codex was NOT
dispatched.

**OQ-S84.1 — staging deviation: a 3rd file was necessarily touched.**
The contract (§3.10) authorized staging ONLY `ControlKernel.java` + the new
characterization test file. Implementing the gate broke an existing test,
`AgentRunLoopConfirmCloseIntegrationTest.confirmFinalAnswer_userSatisfied_transitionsToCloseAndRecordsOutcome`,
which asserted `containment == "resolved"` on a CONFIRM→CLOSE flow whose
fixture carried **no grounding and no prior containment**. That assertion was
pinning the *pre-S-Auto-29 leaky default* directly. Keeping the suite green
(a §3.6 hard gate) therefore required a reconciliation edit to that test — an
unavoidable 3rd file. Root cause (verified, not assumed): `PhaseEvaluator.interpretRunResult`
sets `resolve_disposition` ONLY in its RESOLVE branch (PhaseEvaluator.java:818);
its CONFIRM branch (PhaseEvaluator.java:838-843) returns CLOSE with no
disposition. So `isResolvedSuccessTerminal` can never fire on a CONFIRM→CLOSE
turn. In a **real** flow the `"resolved"` stamp is earned earlier, on the
RESOLVE→CONFIRM turn, via Path C (FINAL_ANSWER + READY_TO_CONFIRM + grounding);
by the time CONFIRM→CLOSE runs, the non-null guard preserves it. The test was
an artificial single-turn fixture starting in CONFIRM with no prior Path C
turn, so it relied solely on the removed default. The reconciliation models
reality: the fixture now pre-sets `containment="resolved"` (the prior-turn
Path C stamp) and asserts CONFIRM→CLOSE PRESERVES it. **Decision requested:**
confirm the 3-file stage set for the milestone-shared review, or advise an
alternative.

**OQ-S84.2 — no separate legitimate-resolution regression.** Investigated
whether the gate un-credits legitimate `record_outcome(RESOLVED)` closes. It
does not: legitimate FAQ resolutions earn `"resolved"` via Path C on the
RESOLVE turn (or stay one-shot ANSWERED_SUBTASK, also Path C), independent of
the CLOSE arm. The CLOSE-arm default was only ever the credit-of-last-resort
for sessions that reached CLOSE WITHOUT a Path C stamp — i.e. exactly the
ungrounded / hallucinated / stalled closes the sprint targets. Test #2
(grounded goal_achieved one-shot) stays green, satisfying the §3.5 STOP
guard.

## §12 — Resolved-stamp delta framing (expected honesty drop, not regression)

This change **lowers** the count of `containment_outcome="resolved"` stamps,
by design. The drop is confined to CLOSE transitions that reach the LLM-owned
`next_phase=CLOSE` WITHOUT having earned a grounded resolved terminal
(`isResolvedSuccessTerminal` false at the CLOSE turn AND no prior non-null
containment): DISCOVER-stalled clarifier closes, hallucinated "user confirmed"
closes, simulator drop-out closes. Those previously received a false
`"resolved"` credit from the unconditional default; they now leave containment
`null`, and the eval routes `case_passed` by L2 evidence rather than by a false
default. Grounded resolutions are unaffected (credited via Path C earlier).

Observed resolved-stamp behaviour in the new characterization fixtures:
- 1 of 5 scenarios stamps `"resolved"` at the CLOSE arm (Test 2, grounded
  goal_achieved one-shot).
- 2 of 5 preserve a prior non-null stamp (Test 4 `"escalated"`, Test 5
  `"resolved"`) via the non-null guard.
- 2 of 5 correctly leave containment `null` (Test 1 ungrounded stalled close,
  Test 3 empty-grounding FINAL_ANSWER) — the honesty drop.

Real-LLM mini re-bless (deliver-agent + human, post-dev) is the eval-evidence
gate per §5.7 / §3.6; the mocked-LLM characterization tests above cover the
CLOSE-arm wiring only and are not primary eval evidence.
