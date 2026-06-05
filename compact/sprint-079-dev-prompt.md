# Sprint 079 / S-Auto-24 / M-Auto-6 — Sub-sprint B — Dev Prompt (R3 UI/observability: admin session list completeness + dedup ToolEvent folding)

> **Planning-context artifact**: this dev prompt is drafted at M-Auto-6
> open (alongside Sub-sprint A's `sprint-078-dev-prompt.md`) as the
> planned scope for the second sub-sprint. The CANONICAL active
> contract is `docs/sprint_objective.md`, which currently holds
> Sub-sprint A. At Sub-sprint A close + post-A re-bless evidence
> review, deliver-agent will replace `sprint_objective.md` with this
> sub-sprint's contract and dev launches from here.

## Role identity

You are the **dev agent** for **Sprint 079 / S-Auto-24 / M-Auto-6
Sub-sprint B**.

Your one-sentence goal: ship R3.a — admin trace observability UI-only
improvements (session-list `handling_state` completeness + `TraceViewer`
dedup ToolEvent folding with preserved expand-to-audit + diagnostic
logging for `handling_state` transitions) so manual review of bad-case
traces and dedup-aware A1 storm auditability are not blocked by the UI
surface. Zero runtime / Java edit; zero scoring / eval surface touch.

## Read order (minimized)

1. `AGENTS.md` (auto-loaded — already in your context via this prompt).
2. **This prompt** (full sub-sprint contract — do NOT read
   `docs/sprint_objective.md` or `docs/milestone_objective.md` for
   scope; they reference this prompt).
3. **Code anchors only as needed** (cited inline in §Scope below).

## Cumulative context (one-page)

- **M-Auto-5 CLOSED 2026-06-05** (Class A; baseline_dir =
  `m-auto-5-baseline-20260604-simfixed-stalledfix`).
- **M-Auto-6 OPENED 2026-06-05** — runtime substrate hygiene at intake
  + DISCOVER surfaces + observability. Sub-sprint A (R1+R2+R4) shipped
  the runtime side; this Sub-sprint B closes the observability side.
- **Sub-sprint A closure precondition**: A must close with the
  falsifiable prediction-check evidence reviewed (`reducible-flaky`
  6/12 → ≤ 2/12; uc_f_billing + uc_fp_removed → stable ~1.00) before
  B launches.
- **Source-of-truth proposal**:
  `docs/solutions/2026-06-05-runtime-bad-cases-handover-schema-discover-counter.md`
  §4.3 + §6.3 (R3.a UI-only).

## Class (layer classification)

**Layer (per `iteration_governance.md` §3.2):**

- **R3.a** → `infra` (observability). Pure UI; zero runtime/Java logic
  change.

**§7 stanza requirement:** EXEMPT (pure infra / observability per
proposal §7.2). Stanza included below as documentation; no semantic
surface touched.

**Tier-0 invariant:** NONE added. R3.a is read-side rendering only.

**Semantic hardcode:** NONE introduced. UI rendering of existing
`deduplicated` flag (already persisted by `AgentRunLoopImpl.java:605-618`
A1 dedup work) + existing `handling_state` enum values.

## Goal

After this sub-sprint ships:

- `SessionList.tsx` StatusBadge displays the full set of terminal
  `handling_state` values (incl. `ESCALATE` / `QUEUE_TO_HUMAN` /
  whatever else exists in the BotSession enum) — no case is hidden in
  admin because its state isn't recognized.
- `TraceViewer.tsx` renders tool-call entries with a `deduplicated=true`
  flag check: dedup'd repeats are FOLDED by default with a "↳ N
  dedup'd repeats" indicator; click to expand the full list (audit not
  lost).
- Diagnostic logging exists for `handling_state` transitions so future
  c7-style "case missing in admin" investigations have a paper trail.
- c7 (admin missing case) and c8 (dedup event UI confusion) symptoms
  resolved at the UI surface; the underlying runtime is already correct
  (A1 dedup persists `ToolEvent.deduplicated` correctly per S-Auto-12).

## Scope (executable, #1–#5)

### #1 — `SessionList.tsx` StatusBadge: full terminal handling_state coverage

**Anchor:** `ui/.../SessionList.tsx` (admin session-list view).

**Change:** StatusBadge component renders the COMPLETE set of
`handling_state` values from the Java enum (currently
`AVAILABLE` / `RUNNING` / `ESCALATE` / `QUEUE_TO_HUMAN` / `CLOSED` /
etc. — verify the exact set against `BotSession.HandlingState`). For
each value: a distinct visual treatment (color + label). NO filtering
of sessions by handling_state at the list level (the c7 root suspicion
is that ESCALATE-terminated sessions are silently filtered out).

**Why:** c7 surfaced that the admin list does not show all
handling_states; the `DemoInspectionController.java:86` endpoint uses
`sessionRepository.findAll()` (no filter), so the UI side is the
candidate filter point. Verifying + fixing the StatusBadge rendering
ensures all sessions are visible.

### #2 — `TraceViewer.tsx` dedup ToolEvent folding

**Anchor:** `ui/.../TraceViewer.tsx` lines 399-471 (tool_call rendering).

**Change:** When rendering tool_call events, check the
`deduplicated=true` flag (already persisted by `AgentRunLoopImpl.java:605-618`):

- Group consecutive tool_call events with the same `tool_name` +
  `arguments` hash where ANY of them carry `deduplicated=true`.
- Render the FIRST event normally; collapse subsequent
  `deduplicated=true` events into a single "↳ N dedup'd repeats"
  indicator below the primary event.
- The indicator is CLICKABLE: click to expand the full list of dedup'd
  events (each with its original timestamp + `originalAtStep`
  reference).
- Default folded state preserves audit capability (full data still
  persisted; just not displayed by default).

**Why:** c8 showed multiple 0ms tool_call events that looked like a
bug to the human reviewer; in fact they were A1 dedup audit records.
Folding by default makes the trace readable while preserving the
audit trail (anti-误杀 §11 PARAPHRASE_STORM audit unaffected).

### #3 — `TraceViewer.tsx` handling_state terminal indicator

**Anchor:** `ui/.../TraceViewer.tsx` (header / metadata area).

**Change:** Display the session's terminal `handling_state` value
prominently at the trace header (separate from the session-list badge).
For ESCALATE / QUEUE_TO_HUMAN, additionally surface the
`escalation_reason` enum value if present.

**Why:** c7 trace was "found" in admin but the terminal state was not
obvious — adding the explicit header makes the disposition clear.

### #4 — Diagnostic logging for handling_state transitions

**Anchor:** `server/.../SessionManager.java` OR `ui/.../adminApi.ts`
(client side, depending on where the transition events surface).

**Change:** Add structured logging (not user-facing) that records each
`handling_state` transition with: session_id, old_state, new_state,
timestamp, and the triggering source (tool call / runtime budget /
explicit close). This is a backend log entry, NOT runtime behaviour
change. Log level: INFO.

**Why:** future "case missing in admin" investigations need a paper
trail. The c7 trail is currently absent — no way to confirm whether
the session reached `CLOSED` or stayed in an intermediate state.

**Hard fence:** This is the ONE backend touch in Sub-sprint B; it is
purely a logging additions, no behaviour change, no `SessionManager`
state-machine edit.

### #5 — UI tests for #1 + #2 + #3 (visual + behavioural)

**Anchors (UI test files):**

- `ui/.../SessionList.test.tsx` (existing or new)
- `ui/.../TraceViewer.test.tsx` (existing or new)

**Change:** Snapshot + behavioural tests for each of #1, #2, #3:

- **#1**: StatusBadge renders each handling_state value distinctly;
  no value is missed.
- **#2**: dedup folding collapses by default; click expands; expanded
  view shows all dedup'd events with timestamps + originalAtStep.
- **#3**: terminal handling_state header is visible on ESCALATE /
  CLOSED / QUEUE_TO_HUMAN sessions.

## Anti-误杀 invariants (HARD)

1. **Audit data NOT lost.** Dedup folding is a DISPLAY choice;
   underlying `ToolEvent` records remain in the trace with full
   `deduplicated=true` + `originalAtStep` metadata. PARAPHRASE_STORM
   audit MUST still be possible by expanding.
2. **No session filtering** at the UI level — every session in the
   backend response is rendered, regardless of `handling_state`.
3. **No runtime behaviour change.** R3.a does NOT modify
   `SessionManager` state machine, `ControlKernel`, `AgentRunLoopImpl`,
   or any tool implementation. Diagnostic logging is a non-behavioral
   add.
4. **No scoring or eval surface touched.** Eval pytest, autoloop
   pytest, Java baseline UNCHANGED.

## Hard fences / STOP conditions

**Files allowed to edit** (fence):

- `ui/.../SessionList.tsx` + tests
- `ui/.../TraceViewer.tsx` + tests
- `ui/.../adminApi.ts` (only if needed for diagnostic logging hook)
- `server/.../SessionManager.java` (ONLY for #4 diagnostic logging
  additions — NO state-machine edits)
- `docs/sprints/sprint-079-handoff.md` (your dev handoff)

**Files FORBIDDEN to edit**:

- ANY runtime Java behaviour: `ControlKernel.java`, `AgentRunLoopImpl.java`,
  `ContextProjectionBuilder.java`, any tool / skill implementation.
- `BotSession.java` enum changes (handling_state values are already
  defined; UI just renders them).
- Any prompt / yaml / CaseSpec.
- Any `eval_interactive/` file.
- Autoloop 5-file SHA-locked scoring set.

**STOP conditions**:

- If the UI test suite is materially broken by an existing snapshot
  regeneration pre-existing the sub-sprint, surface as OQ; do not mask
  with snapshot-only updates.
- If the `BotSession.HandlingState` enum is incomplete (e.g. missing
  values discovered in trace data), surface as a separate R-item; do
  NOT add enum values in Sub-sprint B.
- If diagnostic logging at #4 requires more than ~10 LOC of behaviour
  change, STOP — that's outside Sub-sprint B's fence.

## Test / eval requirements

- All new UI tests in #5 GREEN.
- Existing UI tests UNCHANGED (no snapshot regressions accepted unless
  the change is explicitly the intended UI delta of #1-#3).
- Java baseline `1244 / 1 / 0 / 2` (post-A baseline + R4 tests should
  be unchanged here) — Sub-sprint B should not affect Java tests
  beyond a possible 1-2 new logging-related test cases.
- Eval pytest UNCHANGED.
- Autoloop pytest UNCHANGED.

## §7 Layer-classification + anti-hardcode stanza (documentation; this sub-sprint is §7-EXEMPT)

```markdown
## Layer-classification + anti-hardcode stanza (documentation only — Sub-sprint B is §7-EXEMPT per proposal §7.2 UI-only carve-out)

**Target failure layer:** infra (observability — admin UI rendering)

**Tier-0 invariant:** This sprint adds no Tier-0 invariant.

**Semantic hardcode:** No semantic hardcode introduced.
(UI-only; zero runtime/Java behaviour change; diagnostic logging at #4
is a non-behavioral additive.)

**Generalization coverage:** target / neighbor / negative / shadow case counts: 2 / 0 / ~5 / 0
- target: c7 (admin missing case), c8 (dedup event UI folding)
- neighbor: none (independent observability fix)
- negative: ESCALATE / handover-completed sessions must remain in
  admin visible + clickable; A1 dedup folding allows expanding to see
  full dedup'd event list (audit not lost)
- shadow: not applicable (UI-only)
```

## Codex review plan

Per-sub-sprint Codex review OPTIONAL (UI-only per proposal §6.5 /
§7.2). Visual verification by deliver+human at sub-sprint close is the
primary acceptance evidence. Milestone-shared Codex at M-Auto-6 close
covers both Sub-sprint A + B cumulatively.

If deliver-agent elects to dispatch per-sub-sprint Codex for B:

- Focus only on Q1 / Q2 / Q5 / Q7 (zero content matching / no Tier-0
  added / no semantic decision moved / safety floor unchanged).
- Q3 / Q4 / Q6 / Q8 / Q9 are largely N/A for a UI-only sub-sprint.

## Handoff requirements (you author `docs/sprints/sprint-079-handoff.md`)

§1 of your handoff must include:

- For each of #1-#4: file:line ranges + rationale + the test name(s)
  that gate it.
- UI test results (full numeric: passed / failed / skipped).
- Java test results (full numeric — confirm no regression vs A
  baseline).
- Eval pytest / autoloop pytest results (UNCHANGED).
- Visual verification evidence — screenshots OR a deliver-reviewable
  step-by-step that exercises each of c7-like (missing case) + c8-like
  (dedup event) scenarios.
- STOP confirmations:
  - File fence respected (no runtime behaviour change beyond #4
    logging).
  - No `BotSession.HandlingState` enum value added.
  - No prompt / yaml / CaseSpec / simulator / scoring touched.
  - `baseline_dir` UNCHANGED.
  - `docs/current_eval_baseline.md` UNCHANGED.

## Commit discipline

Recommended commit split:

1. **Commit 1 — #1 SessionList.tsx StatusBadge** + tests.
2. **Commit 2 — #2 TraceViewer.tsx dedup folding** + tests.
3. **Commit 3 — #3 TraceViewer.tsx terminal indicator** + tests.
4. **Commit 4 — #4 handling_state diagnostic logging**.
5. **Commit 5 — Dev handoff**: `docs/sprints/sprint-079-handoff.md`.

## Self-check checklist (complete BEFORE claiming done)

- [ ] Each of #1-#4 implemented; file:line ranges captured in handoff §1.
- [ ] Each new UI test in #5 GREEN.
- [ ] No existing UI snapshot test regressions (only intentional
      ones from #1-#3 deltas).
- [ ] Java baseline UNCHANGED (besides any 1-2 new logging-related
      tests for #4).
- [ ] Eval pytest / autoloop pytest UNCHANGED.
- [ ] No runtime behaviour change beyond #4 diagnostic logging.
- [ ] Audit data preserved — clicking expand on a folded dedup
      indicator shows ALL dedup'd events with original timestamps +
      `originalAtStep`.
- [ ] Every BotSession `handling_state` value has a visual treatment
      in StatusBadge.
- [ ] §7 stanza copied verbatim into handoff (documentation;
      §7-EXEMPT).
- [ ] Visual verification evidence in handoff §1 (screenshots OR
      step-by-step).
- [ ] `baseline_dir` UNCHANGED.
- [ ] `docs/current_eval_baseline.md` UNCHANGED.

When all checked: hand back to deliver-agent for close review + visual
verification + milestone-shared Codex prep.
