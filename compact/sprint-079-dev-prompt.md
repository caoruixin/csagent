# Sprint 079 / S-Auto-24 / M-Auto-6 — Sub-sprint B — Dev Prompt (R3 admin trace observability: R3.a session-list completeness + R3.b dedup ToolEvent folding + R3.c informational guard rejection badge)

## Role identity

You are the **dev agent** for **Sprint 079 / S-Auto-24 / M-Auto-6
Sub-sprint B**.

Your one-sentence goal: ship R3.a + R3.b + R3.c — admin trace
observability UI-only improvements (session-list `handling_state`
completeness + `TraceViewer` A1-dedup ToolEvent folding with preserved
expand-to-audit + informational guard rejection badge distinct from
blocking runtime errors, plus a terminal-state header and a thin
diagnostic-logging trail for `handling_state` transitions) so manual
review of bad-case traces is not blocked by the UI surface. Zero runtime
semantic edit, zero scoring / eval surface touch; ≤ ~10 LOC of
non-behavioural diagnostic logging on the backend is the ONE permitted
backend touch.

## Read order (minimized)

1. `AGENTS.md` (auto-loaded via this prompt).
2. **This prompt** (full sub-sprint contract — do NOT read
   `docs/sprint_objective.md` or `docs/milestone_objective.md` for
   scope; they reference this prompt as their executable view).
3. **Code anchors only as needed** (cited inline in §Scope below).

## Cumulative context (one-page)

- **M-Auto-5 CLOSED 2026-06-05** (Class A; baseline_dir =
  `m-auto-5-baseline-20260604-simfixed-stalledfix`).
- **M-Auto-6 ACTIVE — Sub-sprint A (R1+R2+R4) is dev-side closed
  2026-06-06**: code shipped at commits `a873d18 / 840a5e2 / 247da11 /
  af44903`; Codex APPROVE_S_AUTO_23 at `62b4d7b`; P2 smoke wiring
  evidence at run `eval_interactive/results/20260605-105339/`;
  outcome-evidence re-bless DEFERRED to the M-Auto-6 milestone-shared
  re-bless that runs after Sub-sprint B + C-1 + C-2 all land. Archived
  contract: `docs/sprints/sprint-078-objective.md`.
- **This Sub-sprint B closes the observability side of M-Auto-6**.
  The runtime side (A) is complete and live in production builds; the
  UI surface needs to catch up so bad-case triage (the input that drives
  R-item evolution per Path 2) is not blocked by display artefacts.
- **Source-of-truth proposal**:
  `docs/solutions/2026-06-05-runtime-bad-cases-handover-schema-discover-counter.md`
  §4.3 + §6.3 (R3.a/b/c three sub-items; c1-c17 case mapping); the case
  triggers below are c7 (R3.a admin missing case), c8 (R3.b dedup UI),
  c2 / c10 / c17 (R3.c informational vs blocking guard rejection badge).

## Class (layer classification)

**Layer (per `iteration_governance.md` §3.2):**

- **R3.a** → `infra` (observability). UI-only; renders existing
  `BotSession.HandlingState` enum values the backend already exposes.
- **R3.b** → `infra` (observability). UI-only; renders the existing
  `ToolEvent.deduplicated` flag persisted by A1 dedup (S-Auto-12).
- **R3.c** → `infra` (observability). UI-only display distinction over
  the existing guard-rejection trace event shape; no guard behaviour
  change.

**§7 stanza requirement:** **EXEMPT** per proposal §7.2 + §6.5 (pure
UI/observability; zero semantic surface touched). Stanza documented in
§9 below as record-only.

**Tier-0 invariant:** NONE added. R3.* is read-side rendering only.

**Semantic hardcode:** NONE introduced. UI rendering of existing enum
values + existing structured flags + existing trace event shapes; zero
content / keyword matching of user messages or article content.

## Goal

After this sub-sprint ships:

- `SessionList.tsx` StatusBadge displays the full set of
  `BotSession.HandlingState` terminal values — no session is hidden in
  admin because its state is unrendered.
- `TraceViewer.tsx` renders A1-dedup tool_call entries folded by default
  with a "↳ N dedup'd repeats" indicator; click to expand (audit not
  lost).
- `TraceViewer.tsx` distinguishes INFORMATIONAL guard rejections
  (e.g. `progressive_resolve_record_outcome_premature` from c2/c10/c17,
  `intake_required_fields_missing_for_intake_complete` from c1/c5/c6's
  validator path) from BLOCKING runtime errors (transport / 5xx / OOM /
  uncaught). Distinct badge + collapsed-by-default; expand reveals the
  rejection-code string + rationale.
- Terminal `handling_state` is rendered prominently at the trace header
  with `escalation_reason` when present.
- Diagnostic logging exists for `handling_state` transitions so future
  "case missing in admin" investigations have a paper trail.
- c7 / c8 / c2 / c10 / c17 admin-side symptoms resolved at the UI
  surface; the underlying runtime is already correct (A1 dedup persists
  `ToolEvent.deduplicated` correctly per S-Auto-12; record_outcome
  guard at `ResolveDispositionEvaluator.java:160-186` is correct per
  §1.4).

NOT a goal: changing runtime behaviour; changing guard logic; changing
eval / scoring / simulator; surfacing any semantic decision; modifying
`BotSession.HandlingState` enum.

## Scope (executable, #1–#6)

### #1 — `SessionList.tsx` StatusBadge: full terminal handling_state coverage (R3.a)

**Anchor:** `ui/.../SessionList.tsx` (admin session-list view).

**Change:** StatusBadge renders the COMPLETE set of
`BotSession.HandlingState` values that the backend exposes (verify the
exact set against the Java enum — `AVAILABLE` / `RUNNING` / `ESCALATE`
/ `QUEUE_TO_HUMAN` / `CLOSED` or whatever the current enum lists). For
each value: distinct visual treatment (color + label). NO filtering of
sessions at the UI level (the c7 root suspicion is that
ESCALATE-terminated sessions are silently filtered).

**Why:** c7 surfaced that the admin list does not show all
handling_states; `DemoInspectionController.java:86` uses
`sessionRepository.findAll()` (no filter), so the UI side is the
candidate filter point.

### #2 — `TraceViewer.tsx` dedup ToolEvent folding (R3.b)

**Anchor:** `ui/.../TraceViewer.tsx` lines 399-471 (tool_call rendering).

**Change:** When rendering tool_call events, check the
`deduplicated=true` flag (already persisted by
`AgentRunLoopImpl.java:580-619` A1 dedup):

- Group consecutive tool_call events with the same `tool_name` +
  `arguments` hash where ANY carry `deduplicated=true`.
- Render the FIRST event normally; collapse subsequent
  `deduplicated=true` events into a single "↳ N dedup'd repeats"
  indicator below the primary event.
- Indicator is CLICKABLE: click expands the full list of dedup'd events
  (each with its original timestamp + `originalAtStep` reference).
- Default folded state preserves audit capability (full data still
  persisted; just not displayed by default).

**Why:** c8 showed multiple 0ms tool_call events that looked like a bug;
in fact they were A1 dedup audit records. Folding by default makes the
trace readable while preserving the audit trail (anti-误杀 §11
PARAPHRASE_STORM audit unaffected).

### #3 — `TraceViewer.tsx` informational guard rejection badge (R3.c)

**Anchor:** `ui/.../TraceViewer.tsx` (guard event / tool_call rejection
rendering — exact line range depends on where guard rejections surface;
if alongside tool_calls, this is a per-event badge; if separate, 3.c
lands on that path).

**Change:** Distinguish INFORMATIONAL guard rejection events from
BLOCKING runtime errors at the trace UI level. The current UI renders
both as red-tinted errors which causes c2/c10/c17 human-review confusion
(a CORRECT §1.4 guard rejection looks identical to a transport 500 or a
crashed tool dispatch).

Categorisation criteria (UI-side, derived from the existing trace event
shape — NO new server payload required; if the existing payload does
NOT distinguish, surface as STOP):

- **Informational guard rejection** — trace event whose error class /
  rejection code matches a known §1.4 informational guard. The seed
  allow-list (extend ONLY for codes traceable to the guard files):
  - `progressive_resolve_record_outcome_premature` — record_outcome
    submitted in phase ≠ CONFIRM/CLOSE; guard at
    `ResolveDispositionEvaluator.java:160-186` is CORRECT (c2 / c10 /
    c17 surfaced this confusion).
  - `intake_required_fields_missing_for_intake_complete` — handover
    submitted without `intake_fields` populated; guard at
    `SkillGuardrailDispatcher.java:265-298` is CORRECT (c1/c5/c6 pre-fix
    pattern; preserved as a §1.4 guard even after Sub-sprint A's schema
    declaration because dispatching tools may still reject partial
    intake).
  - Source canonical strings from the Java guard layer where possible
    (e.g. exported constants from `SkillGuardrailDispatcher` or
    `ResolveDispositionEvaluator`). If no such export exists, hard-code
    the UI-side list with a comment citing each guard file:line so the
    allow-list cannot drift silently.
- **Blocking runtime error** — anything else (transport, 5xx, OOM,
  uncaught exception, tool dispatch failure, etc.).

UI rendering distinction:

- Informational: yellow / blue badge ("Informational guard rejection")
  + collapsed-by-default detail; expand reveals the rejection-code
  string + the guard rationale ("Phase ≠ CONFIRM/CLOSE; outcome not
  recordable yet" or "intake_fields missing"). Distinct from red error
  badge.
- Blocking: existing red error badge (UNCHANGED).
- Both ALWAYS expandable to the full original trace payload (no audit
  data loss).

**Hard constraint:** the categoriser MUST NOT weaken the guard itself.
This is a UI display distinction over events the guard ALREADY produces.
If a guard somewhere is misclassified by the UI allow-list, the SAFER
default is to render it as a blocking error (preserve human attention)
rather than informational (which would visually downgrade a real issue).

**Why:** c2 / c10 / c17 — three independent traces — surfaced human
reviewers being confused by record_outcome guard rejections that look
like crashes. The fix is UI-side only because the runtime is already
correct per §1.4. With R3.c, human reviewers can distinguish
"informational: bot tried to record outcome too early, guard correctly
held it back" from "blocking: tool actually crashed".

### #4 — `TraceViewer.tsx` terminal handling_state indicator

**Anchor:** `ui/.../TraceViewer.tsx` (header / metadata area).

**Change:** Display the session's terminal `handling_state` value
prominently at the trace header (separate from the session-list badge).
For ESCALATE / QUEUE_TO_HUMAN, additionally surface the
`escalation_reason` enum value when present.

**Why:** c7 trace was "found" in admin but the terminal state was not
obvious — the explicit header makes the disposition clear.

### #5 — Diagnostic logging for handling_state transitions

**Anchor:** `server/.../SessionManager.java` OR the client-side hook in
`ui/.../adminApi.ts` (depending on where transitions currently surface).

**Change:** Add structured logging that records each `handling_state`
transition with: session_id, old_state, new_state, timestamp, triggering
source (tool call / runtime budget / explicit close). Backend log entry,
NOT runtime behaviour change. Log level: INFO.

**Hard fence:** This is the ONE backend touch in Sub-sprint B; purely
logging, NO behaviour change, NO `SessionManager` state-machine edit.
If the diagnostic logging at #5 requires more than ~10 LOC of
behaviour-affecting change, STOP — outside Sub-sprint B's fence.

**Why:** future "case missing in admin" investigations need a paper
trail. The c7 trail is currently absent — no way to confirm whether the
session reached `CLOSED` or stayed in an intermediate state.

### #6 — UI tests for #1 + #2 + #3 + #4

**Anchors (UI test files):**

- `ui/.../SessionList.test.tsx` (existing or new)
- `ui/.../TraceViewer.test.tsx` (existing or new)

**Change:** Snapshot + behavioural tests for each of #1, #2, #3, #4:

- #1 — StatusBadge renders each handling_state value distinctly; no
  value is missed.
- #2 — dedup folding collapses by default; click expands; expanded view
  shows all dedup'd events with timestamps + originalAtStep.
- #3 — informational guard rejection badge appears for at least the
  two canonical codes (`progressive_resolve_record_outcome_premature`,
  `intake_required_fields_missing_for_intake_complete`) AND a negative
  case: an unrecognised rejection code renders as the blocking-error
  badge (SAFER default). Expand reveals the rejection-code string +
  rationale; click → expand preserves audit.
- #4 — terminal handling_state header is visible on ESCALATE / CLOSED
  / QUEUE_TO_HUMAN sessions.

## Anti-误杀 invariants (HARD)

1. **Audit data NOT lost.** Dedup folding (#2) and informational
   collapsing (#3) are DISPLAY choices; underlying `ToolEvent` records
   and guard rejection events remain in the trace with full payload.
   PARAPHRASE_STORM audit MUST still be possible by expanding.
2. **No session filtering at the UI level.** Every session in the
   backend response is rendered, regardless of `handling_state` (#1).
3. **No runtime behaviour change.** R3.* does NOT modify
   `SessionManager` state machine, `ControlKernel`, `AgentRunLoopImpl`,
   `ContextProjectionBuilder`, any tool implementation, any guard
   implementation. Diagnostic logging at #5 is a non-behavioural add.
4. **No scoring or eval surface touched.** Eval pytest, autoloop pytest,
   Java baseline UNCHANGED (modulo +1-2 logging-related test additions
   for #5).
5. **R3.c categoriser SAFER default = blocking.** If the UI allow-list
   does not recognise a rejection code, render as blocking. DO NOT
   render unknown rejections as informational by default.
6. **No new `escalation_reason` enum value** (#4 surfaces existing
   values).
7. **No `BotSession.HandlingState` enum changes.** UI renders existing
   values; if a missing value is discovered, surface as a separate
   R-item.

## Hard fences / STOP conditions

**Files allowed to edit** (fence):

- `ui/.../SessionList.tsx` + tests
- `ui/.../TraceViewer.tsx` + tests
- `ui/.../adminApi.ts` (only if needed for diagnostic logging hook)
- `server/.../SessionManager.java` (ONLY for #5 diagnostic logging
  additions — NO state-machine edits; ≤ ~10 LOC behaviour-affecting
  change is the STOP cap)
- `docs/sprints/sprint-079-handoff.md` (your dev handoff)

**Files FORBIDDEN to edit**:

- ANY runtime Java behaviour: `ControlKernel.java`,
  `AgentRunLoopImpl.java`, `ContextProjectionBuilder.java`,
  `ResolveDispositionEvaluator.java`, `SkillGuardrailDispatcher.java`,
  `BudgetChecker.java`, any tool / skill implementation.
- `BotSession.java` enum changes.
- Any prompt / yaml / CaseSpec.
- Any `eval_interactive/` file.
- Autoloop 5-file SHA-locked scoring set (fence-#13).
- `autoloop/config.yaml` `baseline_dir` pointer.
- `docs/current_eval_baseline.md`.

**STOP conditions**:

- UI test suite has a pre-existing snapshot regression that predates
  this sub-sprint → surface as OQ; do NOT mask with snapshot-only
  updates.
- `BotSession.HandlingState` incomplete (missing value discovered during
  #1) → surface as separate R-item.
- R3.c rejection-code allow-list cannot be sourced from existing Java
  constants AND would require > ~6 hard-coded entries → STOP and
  surface as scope question.
- #5 diagnostic logging requires > ~10 LOC of behaviour change → STOP.

## Test / eval requirements

- All new UI tests in #6 GREEN.
- Existing UI tests UNCHANGED (no snapshot regressions accepted unless
  the change is the intended UI delta of #1-#4).
- Java baseline `1244 / 1 / 0 / 2` preserved (inherited OQ-S41.5);
  Sub-sprint B may add +1-2 logging-related test cases for #5 — these
  MUST not flip the failure count.
- Eval pytest UNCHANGED.
- Autoloop pytest UNCHANGED.
- NO real-LLM re-bless at this sub-sprint close. The outcome evidence
  re-bless is the M-Auto-6 milestone-shared one that runs after C-1 +
  C-2.

## §7 Layer-classification + anti-hardcode stanza (documentation only — §7-EXEMPT)

```markdown
## Layer-classification + anti-hardcode stanza (documentation only — Sub-sprint B is §7-EXEMPT per proposal §6.5 / §7.2 UI-only carve-out)

**Target failure layer:** infra (observability — admin UI rendering of
existing runtime trace events + structured flags).

**Tier-0 invariant:** This sprint adds no Tier-0 invariant.

**Semantic hardcode:** No semantic hardcode introduced.
(UI-only; zero runtime/Java behaviour change beyond ≤ ~10 LOC of
diagnostic logging additions at #5. R3.c's rejection-code allow-list is
a UI-side categoriser over existing trace event shapes, not a per-UC
matrix and not a content-keyword match. SAFER default for an
unrecognised rejection code is "blocking" — preserves human attention
rather than masking it.)

**Generalization coverage:** target / neighbor / negative / shadow case counts: 5 / 0 / ~5 / 0
- target: c7 (admin missing case), c8 (dedup event UI folding), c2 /
  c10 / c17 (informational guard rejection badge).
- neighbor: none.
- negative: ESCALATE / handover-completed sessions remain visible +
  clickable; A1 dedup folding allows expanding to see full dedup'd event
  list (audit not lost); unrecognised rejection codes render as blocking
  (NOT informational); real transport / 5xx / OOM errors render as
  blocking (UNCHANGED).
- shadow: not applicable (UI-only).
```

## Codex review plan

Per-sub-sprint Codex review **OPTIONAL** (UI-only per proposal §6.5 /
§7.2); visual verification by deliver + human is the primary acceptance
evidence. Milestone-shared Codex at M-Auto-6 close covers Sub-sprint A
+ B + C-1 + C-2 cumulatively.

If deliver-agent elects to dispatch per-sub-sprint Codex for B:

- Focus on Q1 / Q2 / Q5 / Q7 (zero content matching / no Tier-0
  invariant / no semantic decision moved / safety floor unchanged).
- Q3 / Q4 / Q6 / Q8 / Q9 are largely N/A for a UI-only sub-sprint.
- R3.c allow-list MUST be reviewed for keyword-matching shape (Q1):
  the rejection-code strings are CONSTANTS sourced from the Java guard
  layer, not user-message keywords.

## Handoff requirements (you author `docs/sprints/sprint-079-handoff.md`)

§1 of your handoff must include:

- For each of #1-#5: file:line ranges + rationale + the test name(s)
  that gate it.
- **R3.c rejection-code allow-list** — the explicit list the UI uses
  to categorise informational vs blocking, plus the source of each
  entry (Java guard constant import / hard-coded with file:line
  citation).
- **Visual verification evidence** — screenshots OR a deliver-reviewable
  step-by-step that exercises each of c7-like (admin missing case),
  c8-like (dedup event folding + expand), c2/c10/c17-like (informational
  guard rejection badge + expand revealing rationale), and a negative
  (unrecognised rejection code → blocking badge).
- UI test results (full numeric: passed / failed / skipped).
- Java test results (full numeric — confirm no regression vs A's
  `1244/1/0/2` post-S-Auto-23 baseline).
- Eval pytest / autoloop pytest results (UNCHANGED).
- STOP confirmations:
  - File fence respected (no runtime behaviour change beyond #5
    logging).
  - No `BotSession.HandlingState` enum value added.
  - No prompt / yaml / CaseSpec / simulator / scoring touched.
  - `baseline_dir` UNCHANGED.
  - `docs/current_eval_baseline.md` UNCHANGED.
  - **No outcome-evidence re-bless launched at this sub-sprint** (the
    re-bless is the M-Auto-6 milestone-shared one that runs after C-1
    + C-2).

## Commit discipline

Recommended commit split (per `prompt-artifact-rules.md` §9):

1. **Commit 1 — R3.a**: `SessionList.tsx` StatusBadge full
   handling_state coverage + tests.
2. **Commit 2 — R3.b**: `TraceViewer.tsx` dedup ToolEvent folding +
   tests.
3. **Commit 3 — R3.c**: `TraceViewer.tsx` informational guard rejection
   badge + tests (incl. negative: unknown code → blocking).
4. **Commit 4 — #4**: `TraceViewer.tsx` terminal handling_state header.
5. **Commit 5 — #5**: handling_state diagnostic logging
   (`SessionManager.java` or equivalent).
6. **Commit 6 — Dev handoff**: `docs/sprints/sprint-079-handoff.md`.

Each commit message follows the established convention (see sprint-077
+ sprint-078 commits as reference).

## Self-check checklist (complete BEFORE claiming done)

- [ ] Each of #1-#5 implemented; file:line ranges captured in handoff §1.
- [ ] Each new UI test in #6 GREEN.
- [ ] R3.c allow-list documented in handoff §1 with each entry's source.
- [ ] R3.c negative test confirms unrecognised rejection code renders
      as blocking (SAFER default).
- [ ] R3.b expand-from-folded test confirms full dedup audit data is
      visible after expansion (no audit loss).
- [ ] R3.a coverage test confirms every `BotSession.HandlingState`
      value has a visual treatment in StatusBadge.
- [ ] No existing UI snapshot test regressions (only intentional ones
      from #1-#4 deltas).
- [ ] Java baseline `1244/1/0/2` preserved (besides any 1-2 new
      logging-related tests for #5).
- [ ] Eval pytest / autoloop pytest UNCHANGED.
- [ ] No runtime behaviour change beyond #5 diagnostic logging.
- [ ] Audit data preserved — clicking expand on a folded dedup
      indicator (#2) and on an informational guard badge (#3) shows ALL
      underlying records.
- [ ] No `BotSession.HandlingState` enum value added.
- [ ] No prompt / yaml / CaseSpec / simulator / scoring touched.
- [ ] §7 stanza copied verbatim into handoff (record-only; §7-EXEMPT).
- [ ] Visual verification evidence in handoff §1 covering c7 / c8 /
      c2 / c10 / c17 + negative.
- [ ] `baseline_dir` UNCHANGED.
- [ ] `docs/current_eval_baseline.md` UNCHANGED.
- [ ] No outcome-evidence re-bless launched at this sub-sprint close.

When all checked: hand back to deliver-agent for close review + visual
verification + Sub-sprint C-1 / C-2 dev launch.
