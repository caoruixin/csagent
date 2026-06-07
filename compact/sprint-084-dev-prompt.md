# Dev prompt — Sprint 084 / S-Auto-29 (M-Auto-7 S-A) — CS1 default-resolved-on-CLOSE gate

## 1. Role identity

你是 dev agent for **Sprint 084 / S-Auto-29**, the first **autoloop
launch blocker** of milestone **M-Auto-7 (Autoloop readiness and CS4
entity-context pilot)**. One-sentence goal: **gate the ControlKernel
phase=CLOSE resolved-stamp through `isResolvedSuccessTerminal` so the
runtime stops crediting `containment_outcome="resolved"` on CLOSE
transitions that delivered no grounded answer.**

This is a **pure-infra runtime trace-contract fix** (§3.2 layer:
`infra`). It is §7-EXEMPT and per-sub-sprint-Codex-EXEMPT.

## 2. Read order (minimal)

- `AGENTS.md` (auto-loaded — governance chain).
- This prompt (self-contained contract below).
- Code anchors you will edit / reference (verified at HEAD
  `auto-loop-branch`, 2026-06-08):
  - `server/src/main/java/com/gumtree/csagent/service/runtime/ControlKernel.java:572-641` — the containment-stamp if/else-if chain (D16.D comment + Path B / Path C / Path D).
  - `ControlKernel.java:1418` — `isResolvedSuccessTerminal(BotSession, AgentRunResult)` (the gate to reuse; DO NOT modify).
  - `ControlKernel.java:1508` — `shouldVoidResolvedStamp(...)` (DO NOT modify).
  - `ControlKernel.java:1532` — `voidResolvedStamp(...)` (DO NOT modify).

Do NOT read `docs/sprints/*` or `docs/archive/*`.

## 3. Embedded contract

### 3.1 Background (why)

Today `ControlKernel.java:575-581` (Path B) unconditionally defaults
`containment_outcome` to `"resolved"` on ANY `phaseAfter == "CLOSE"`
transition where containment is null — with no grounding-evidence
check. The CLOSE transition is LLM-owned (the LLM emits
`next_phase=CLOSE`), so a DISCOVER-stalled clarifier turn, a
hallucinated "user confirmed" close, or a simulator drop-out close all
get falsely credited as a resolved success. This is the one remaining
containment-stamp site without the grounding gate that Sprint 075 /
S-Auto-20 (`isResolvedSuccessTerminal`) and Sprint 077 / S-Auto-22
(`shouldVoidResolvedStamp`) already added everywhere else.

The current structure (verified at HEAD):

```java
// :575  Path B — phase=CLOSE default
if ("CLOSE".equals(phaseAfter)) {
    session.setHandlingState("CLOSED");
    if (session.getContainmentOutcome() == null) {
        session.setContainmentOutcome("resolved");   // <-- the leak
    }
    eventEmitter.emitSessionClosed(session.getSessionId(),
            session.getContainmentOutcome());
} else if (isResolvedSuccessTerminal(session, runResult)) {   // :582 Path C
    session.setContainmentOutcome("resolved");
    eventEmitter.emitSessionClosed(...);
} else if (shouldVoidResolvedStamp(session, runResult)) {     // :612 Path D
    ...
}
```

Note the structural nuance: Path C is reached ONLY when phaseAfter is
NOT CLOSE (it's an `else if`). So today a legitimate goal_achieved
one-shot (FINAL_ANSWER + ANSWERED_SUBTASK + articlesShown + LLM emits
CLOSE) is resolved via Path B, not Path C. Your rewrite MUST keep that
case resolved (test #2).

### 3.2 What to change (Scope) — canonical shape: inline the gate

Rewrite the CLOSE arm (`:575-581`) so the gate is inlined:

```java
if ("CLOSE".equals(phaseAfter)) {
    session.setHandlingState("CLOSED");   // UNCONDITIONAL side-effect — keep
    if (session.getContainmentOutcome() == null
            && isResolvedSuccessTerminal(session, runResult)) {
        session.setContainmentOutcome("resolved");
    }
    // else: no grounding evidence on CLOSE → leave containment null;
    // the eval gate routes case_passed by L2 evidence, not by a false
    // default. (No new enum value — see hard fences.)
    eventEmitter.emitSessionClosed(session.getSessionId(),
            session.getContainmentOutcome());
} else if (isResolvedSuccessTerminal(session, runResult)) {
    ... (unchanged)
} else if (shouldVoidResolvedStamp(session, runResult)) {
    ... (unchanged)
}
```

Also update the `:572-574` D16.D comment to state the legacy
evaluateClose mirror is now grounding-gated.

Do NOT modify `isResolvedSuccessTerminal`, `shouldVoidResolvedStamp`,
`voidResolvedStamp`, the Path C body, the Path D body, the budget gate,
or any prompt/skill/eval file.

### 3.3 Binding contract (human-approved 2026-06-08)

- `handlingState=CLOSED` remains UNCONDITIONAL on CLOSE.
- `emitSessionClosed` still fires.
- `"resolved"` is stamped ONLY when containment is null AND
  `isResolvedSuccessTerminal(session, runResult)` is true.
- No grounding evidence → containment remains null.
- Escalation behaviour preserved.
- Prior non-null containment preserved (never overwritten).
- goal_achieved one-shot resolved behaviour preserved.
- The five characterization tests are mandatory.

### 3.4 Characterization tests (pin all 5)

Co-locate with the existing ControlKernel containment tests. Each is
the binding contract:

1. DISCOVER→DISCOVER→DISCOVER→CLOSE, 0 tool calls, no UC committed →
   `containmentOutcome` is NOT `"resolved"` (expect null).
2. RESOLVE → terminalOutcome FINAL_ANSWER + resolveDisposition
   ANSWERED_SUBTASK + non-empty articlesShown + phaseAfter CLOSE →
   `containmentOutcome == "resolved"` (goal_achieved one-shot anti-误杀).
3. RESOLVE → FINAL_ANSWER + EMPTY articlesShown → CLOSE →
   `containmentOutcome` is NOT `"resolved"`.
4. Escalation path + CLOSE → `containmentOutcome == "escalated"`
   (escalation stamped earlier; non-null guard prevents overwrite).
5. Prior turn stamped `"resolved"` + this turn's CLOSE transition →
   value is NOT overwritten/regressed (non-null guard holds).

Also assert `handlingState == "CLOSED"` is set in scenarios 1-3,5 (the
side-effect must fire regardless of the containment outcome).

### 3.5 Hard fences / STOP conditions

- No new `containment_outcome` enum value (C1.B deferred).
- No `semantic_planner` / prompt / skill / eval edit.
- No keyword / regex / if-else on `user_message` content.
- No edit to `composite.py` or any eval-side gate.
- No change to `isResolvedSuccessTerminal` / `shouldVoidResolvedStamp`.
- If test #2 (goal_achieved one-shot) cannot stay green with the gated
  stamp, STOP — surface an OQ in handoff §11; do NOT weaken the gate.

### 3.6 Test / eval requirements

- 5 characterization tests green.
- Focused Java suite: no new regression vs inherited baseline
  `1358 / 1 / 0 / 2` (sole known failure OQ-S41.5 is a prompt
  tiebreaker test — provably uncoupled; this sub-sprint touches no
  prompt file). Report exact counts.
- Real-LLM mini re-bless is run by deliver-agent + human post-dev (not
  your responsibility); record in §12 the resolved-stamp count you
  observe in the new characterization fixtures.

### 3.7 §7 status

**§7-EXEMPT** (pure infra + characterization-test). No stanza required
in the dev work; the audit-trail stanza is in `docs/sprint_objective.md`.

### 3.8 Codex review trigger (§4.3)

**Per-sub-sprint Codex EXEMPT.** No §4.3 trigger fires: not a Tier-0
candidate; not §1.7-adjacent (the change REMOVES a default stamp, adds
no hardcode); not a hard-fence violation; not a fix-iteration. Folds
into the M-Auto-7 milestone-shared close review. Do NOT dispatch Codex
yourself.

### 3.9 Handoff requirements

Author `docs/sprints/sprint-084-handoff.md`:
- §0 evidence table (5 tests green; Java suite counts).
- §11: Codex deferral recorded (EXEMPT); any OQs.
- §12: resolved-stamp delta framing (expected honesty drop, not
  regression).

### 3.10 Commit discipline

Stage ONLY `ControlKernel.java` + the characterization test file. No
`git add -A`. New commit per fix; never `--amend` across a failed hook.
Co-author trailer per repo convention.

## 4. Self-check checklist (before declaring done)

- [ ] CLOSE arm stamps `"resolved"` ONLY when `containment==null &&
      isResolvedSuccessTerminal(...)`.
- [ ] `handlingState="CLOSED"` still set unconditionally on CLOSE.
- [ ] `emitSessionClosed` still called on the CLOSE arm.
- [ ] All 5 characterization tests pass.
- [ ] `isResolvedSuccessTerminal` / `shouldVoidResolvedStamp` /
      `voidResolvedStamp` untouched.
- [ ] No prompt / skill / eval / enum change.
- [ ] Focused Java suite: no new regression (counts reported).
- [ ] Handoff §0/§11/§12 authored; only authorized files staged.
