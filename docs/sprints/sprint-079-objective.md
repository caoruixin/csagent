---
title: Sub-sprint B — S-Auto-24 / Sprint 079 — R3 admin trace observability (R3.a session-list completeness + R3.b dedup ToolEvent folding + R3.c informational guard rejection badge) — ARCHIVED (dev-side closed; visual-verified; §7-EXEMPT; milestone evidence deferred)
doc_tier: sprint-archive
status: archived
implementation_status: implemented
source_of_truth: this file (archived contract) + docs/sprints/sprint-079-handoff.md (dev handoff) + eval_interactive/results/20260605-105339/ (M-Auto-6 P2 smoke on A; B has no eval surface) + browser-live visual verification on session e82c8da3 (deliver-agent 2026-06-06)
last_reviewed: 2026-06-06
review_cadence: archived
supersedes: docs/sprints/sprint-078-objective.md
superseded_by: null
notes: >
  Archived at S-Auto-24 dev-side close 2026-06-06 after deliver-agent
  visual verification. **Sub-sprint B is dev-side closed /
  visual-verified / §7-EXEMPT (no per-sub-sprint Codex required);
  milestone-level outcome evidence is deferred to the M-Auto-6 final
  re-bless.**

  Status semantics (per human 2026-06-06 cadence + deliver 2026-06-06
  close verdict):
  - dev work complete: 5 commits a26ec88..505aca3 (UI test harness
    bootstrap + R3.a SessionList + R3.b dedup folding + R3.c
    informational guard badge + #4 terminal header + #5 SessionManager
    diagnostic logging + dev handoff).
  - UI vitest: 10 passed / 0 failed / 0 skipped (10 new tests across
    SessionList.test.tsx + TraceViewer.test.tsx covering #1-#4 +
    negative).
  - UI tsc -b + vite build: exit 0 / success.
  - Java baseline `1297 / 1 / 0 / 2` preserved (post-S-Auto-23
    baseline; sole failure = inherited OQ-S41.5; no new Java tests
    added — diagnostic logging at #5 is non-behavioural and required
    no test).
  - eval pytest 553 (548 + 5 inherited conda-python env-drift in
    test_corpus_lint.py per `reference_eval_pytest_corpus_lint_conda_python`
    memory) UNCHANGED. autoloop pytest 324 UNCHANGED.
  - Codex per-sub-sprint review: SKIPPED. §7-EXEMPT per proposal §6.5 /
    §7.2 UI-only carve-out + milestone framework §4.3 OPTIONAL clause.
    Milestone-shared Codex at M-Auto-6 close covers B cumulatively.
  - **Live visual verification 2026-06-06 by deliver-agent on session
    e82c8da3** (UC-D record_outcome trace): R3.a distinct
    handling_state badges (amber "Queued to Human" + green "Bot
    Handling") rendered with no UI-side filtering; R3.b "↳ N dedup'd
    repeats" indicator rendered for repeated `search_knowledge` 0ms
    cache-hits with audit preserved on expand; R3.c
    `data-rejection-kind="informational"` + yellow "Informational
    guard rejection" badge rendered on record_outcome guard rejection,
    distinct from `data-rejection-kind="ok"` and (would-be) `blocking`;
    #4 terminal state header showing `Terminal state: Queued to Human`
    + `escalation_reason: account_compliance`; #5 diagnostic logging
    verified via diff inspection (commit 5ffb618; ~7 behaviour-affecting
    LOC + private helper that only reads + logs; no state-machine
    touch).
  - `baseline_dir` NOT moved; `docs/current_eval_baseline.md`
    UNCHANGED. Both flip at M-Auto-6 milestone close after the
    milestone-shared re-bless.

  Three OQs surfaced + dispositioned at close:
  - OQ-S79.1 (UI test harness): ACCEPTED. The harness (vitest +
    @testing-library/react + jsdom; `ui/package.json` devDeps +
    `ui/vite.config.ts` `test` block + `ui/src/test/setup.ts`) is
    necessary for the contract's #6 "UI tests GREEN" requirement.
    Self-contained to `ui/`; no runtime / eval / scoring touch;
    revertible if ever desired. Future UI work benefits.
  - OQ-S79.2 (single-session demo endpoint): DEFERRED post-M-Auto-6.
    `TraceViewer` reads session disposition via `listSessions()`
    because no `GET /demo/sessions/{id}` exists. Out of B's UI-only
    fence; ~80 LOC backend add that doesn't gate close.
  - OQ-S79.3 (per-call timestamps): DEFERRED. Trace payload has no
    per-tool-call timestamp (only per-turn); R3.b dedup rows show
    `step_index` + `original_at_step` instead. Backend trace shape
    change is out of M-Auto-6 scope.

  Outcome evidence DEFERRED. B has no eval / pass-rate surface (UI
  only). The M-Auto-6 milestone-shared re-bless runs AFTER A + B +
  C-1 + C-2 land; B's contribution is observability quality of life
  for manual triage, not measured pass-rate movement.

  Original scope: R3.a (SessionList full handling_state coverage),
  R3.b (TraceViewer A1-dedup ToolEvent folding with expand-to-audit),
  R3.c (NEW post-ship informational guard rejection badge with SAFER
  unrecognised-code default = blocking), #4 (terminal handling_state
  + escalation_reason header), #5 (≤10 LOC non-behavioural
  handling_state-transition diagnostic logging — the ONE permitted
  backend touch).

  Subsequent sub-sprints (per human 2026-06-06 packaging on the
  post-ship proposal expansion to c1-c17):
  - **Sub-sprint C-1 (S-Auto-25 / Sprint 080; R7 + R2.a#5-ext —
    intake/clarification runtime contract bundle) — CURRENT active
    contract in `docs/sprint_objective.md`** (launched 2026-06-06
    after B dev-side close + visual verification).
  - Sub-sprint C-2 (S-Auto-26 / Sprint 081; R5 + R6 — citation
    display token + corpus `bot_visible` filter) — planning context
    in `compact/sprint-081-dev-prompt.md`; launches after C-1
    dev-side closes (sequential per human 2026-06-06 decision).
  - Milestone-shared §9 re-bless and M-Auto-6 close after all four
    sub-sprints land.

  Forbidden (preserved for archival reference): any runtime Java
  behaviour change beyond the thin diagnostic logging at #5; any
  prompt / yaml / CaseSpec / simulator / scoring / autoloop 5-file
  SHA-locked set touch; `SkillGuardrailDispatcher` reject logic;
  `BotSession.HandlingState` enum changes; audit data loss (folding +
  collapsing must always be expandable).
---

# Sub-sprint B — S-Auto-24 / Sprint 079 — R3 admin trace observability (ARCHIVED 2026-06-06)

> **Dev-side close status (2026-06-06):** Sub-sprint B is dev-side
> closed / visual-verified / §7-EXEMPT (no per-sub-sprint Codex
> required); milestone-level outcome evidence is deferred to the
> M-Auto-6 final re-bless.
>
> | Gate | Status | Evidence |
> |---|---|---|
> | Dev code shipped | ✅ | Commits `a26ec88` (vitest/jsdom test harness — OQ-S79.1 accepted) / `ffbf77d` (R3.a SessionList) / `42c9bc0` (R3.b dedup folding + R3.c guard badge + #4 terminal header) / `5ffb618` (#5 SessionManager logging, ≤10 LOC) / `505aca3` (sprint-079 dev handoff) |
> | UI vitest | ✅ `10 passed / 0 failed / 0 skipped` | New harness `vitest run`; tests cover #1-#4 + negative (unknown rejection code → blocking, SAFER default) |
> | UI tsc -b + vite build | ✅ exit 0 / success | Production build OK |
> | Java baseline | ✅ `1297 / 1 / 0 / 2` preserved | Sole failure = inherited OQ-S41.5; no behaviour-affecting Java change; baseline matches post-S-Auto-23 |
> | Eval pytest | ✅ `553` UNCHANGED | 548 + 5 conda-python env-drift (`test_corpus_lint.py` ModuleNotFoundError); not a regression |
> | Autoloop pytest | ✅ `324` UNCHANGED | Untouched by construction |
> | Per-sub-sprint Codex review | ➖ SKIPPED (OPTIONAL per §7-EXEMPT carve-out) | Milestone-shared Codex at M-Auto-6 close covers B cumulatively |
> | Visual verification by deliver-agent | ✅ live in browser 2026-06-06 | Session `e82c8da3` (UC-D record_outcome): R3.a badge colours + R3.b dedup `↳` indicator + R3.c `data-rejection-kind="informational"` on `record_outcome` + #4 terminal state header all present; SAFER default fence preserved; audit accessible via expand |
> | **Milestone-shared §9 real-LLM re-bless** | ⏳ DEFERRED to M-Auto-6 close | Will run after Sub-sprint C-1 (R7 + R2.a#5-ext) + Sub-sprint C-2 (R5 + R6) land. `baseline_dir` and `docs/current_eval_baseline.md` NOT moved until then. |
>
> The body below is preserved verbatim as the archived contract that
> dev executed against. Do not edit — record-only.

# Sub-sprint B — S-Auto-24 / Sprint 079 — R3 admin trace observability (original contract preserved below)

  Sub-sprint B of M-Auto-6 (Runtime substrate hygiene + observability).
  Activated 2026-06-06 after S-Auto-23 dev-side close (smoke-verified +
  Codex-approved; milestone-level outcome evidence deferred to the
  M-Auto-6 milestone-shared re-bless).

  Scope (UI/observability only; zero runtime semantic surface):
  - R3.a — `SessionList.tsx` StatusBadge renders the COMPLETE set of
    `BotSession.HandlingState` terminal values so c7-style "admin can't
    find the case" investigations have a paper trail.
  - R3.b — `TraceViewer.tsx` folds A1-dedup'd ToolEvent records by
    default with an "↳ N dedup'd repeats" indicator that EXPANDS to
    preserve full audit (the c8 readability problem: 0ms dedup events
    look like a bug because the UI doesn't render the `deduplicated=true`
    flag).
  - R3.c — `TraceViewer.tsx` distinguishes INFORMATIONAL guard rejection
    events (e.g. `progressive_resolve_record_outcome_premature` from
    c2 / c10 / c17 — the guard is CORRECT per §1.4) from BLOCKING runtime
    errors (transport / 5xx / OOM). Distinct badge + collapsed-by-default
    display; expand reveals the guard rationale string. Guard behaviour
    itself is UNTOUCHED — this is purely a UI display distinction.

  Sub-sprint A (R1+R2+R4 bundle) is dev-side closed and smoke-verified
  per `docs/sprints/sprint-078-objective.md`; the outcome-evidence
  re-bless waits for Sub-sprint B + Sub-sprint C-1 + Sub-sprint C-2 to
  all land. Sub-sprint C-1 (S-Auto-25 / Sprint 080; R7 + R2.a#5-ext) and
  Sub-sprint C-2 (S-Auto-26 / Sprint 081; R5 + R6) are drafted as
  planning context in `compact/sprint-080-dev-prompt.md` and
  `compact/sprint-081-dev-prompt.md` respectively; C-2 can run parallel
  to C-1 per the human 2026-06-06 packaging decision (independent
  surfaces).

  Forbidden: any runtime Java behaviour change beyond a thin diagnostic
  logging addition at #4 (NO state-machine, NO ControlKernel,
  AgentRunLoopImpl, ContextProjectionBuilder, or tool implementation
  edits); any prompt / yaml / CaseSpec / simulator / scoring / autoloop
  5-file SHA-locked set touch; `SkillGuardrailDispatcher` reject logic
  (3.c is UI-only — the guard remains the source of truth);
  `BotSession.HandlingState` enum changes (UI renders existing values);
  audit data loss (folding + collapsing must always be expandable).

  Acceptance: visual verification by deliver-agent + human on c7/c8 +
  c2/c10/c17 trace shapes; UI test suite green; Java baseline `1244/1/0/2`
  preserved with possibly +1-2 logging-related test additions; eval pytest
  / autoloop pytest UNCHANGED. NO outcome-evidence re-bless at this
  sub-sprint close — that runs at the M-Auto-6 milestone close.

  `baseline_dir` UNCHANGED (`m-auto-5-baseline-20260604-simfixed-stalledfix`
  per M-Auto-5 close); `docs/current_eval_baseline.md` UNCHANGED.
---

# Sub-sprint B — S-Auto-24 / Sprint 079 — R3 admin trace observability

## Class

**Layer (per `iteration_governance.md` §3.2):**

- **R3.a** → `infra` (observability). UI-only; renders existing
  `BotSession.HandlingState` enum values that the backend already exposes.
- **R3.b** → `infra` (observability). UI-only; renders the existing
  `ToolEvent.deduplicated` flag persisted by A1 dedup (S-Auto-12).
- **R3.c** → `infra` (observability). UI-only display distinction over
  the existing guard-rejection trace event shape (no guard behaviour
  change).

**§7 stanza requirement:** **EXEMPT** per proposal §7.2 + §6.5 (pure
UI/observability sub-sprint; zero semantic surface touched). Stanza
documented in §7 below as record-only.

**Tier-0 invariant:** This sub-sprint adds no Tier-0 invariant. R3.*
is read-side rendering only.

**Semantic hardcode:** None introduced. UI rendering of existing enum
values + existing structured flags; zero content/keyword matching of
user messages or article content.

## Goal

After this sub-sprint ships:

- **Admin observability surface is honest for bad-case triage.** Every
  session reachable in the backend is visible in `SessionList.tsx`
  regardless of `handling_state`; A1-dedup'd ToolEvent records render
  as a folded "↳ N dedup'd repeats" indicator (expand to see full audit);
  informational guard rejections (record_outcome premature, intake
  validator rejections, similar §1.4 guard rejections) render as a
  distinct badge separate from real runtime errors.
- **c7 / c8 / c2 / c10 / c17 admin-side symptoms resolved at the UI
  surface.** The underlying runtime is already correct (A1 dedup
  persists `ToolEvent.deduplicated` correctly per S-Auto-12; record_outcome
  guard at `ResolveDispositionEvaluator.java:160-186` is correct per
  §1.4). Only the UI display needs to catch up.
- **Audit capability preserved end-to-end.** Folding / collapsing /
  badge distinctions are DISPLAY choices; the underlying trace records
  remain identical and fully expandable (anti-误杀 §11 PARAPHRASE_STORM
  audit unaffected; guard rationale strings remain readable).
- **Future "case missing in admin" investigations have a paper trail**
  via the diagnostic logging at #4.

NOT a goal: changing any runtime behaviour; changing guard logic;
changing eval / scoring / simulator; surfacing any semantic decision;
modifying `BotSession.HandlingState` enum.

## Scope (executable, #1–#6)

### #1 — `SessionList.tsx` StatusBadge: full terminal handling_state coverage (R3.a)

**Anchor:** `ui/.../SessionList.tsx` (admin session-list view).

**Change:** StatusBadge component renders the COMPLETE set of
`BotSession.HandlingState` values that the backend exposes (verify the
exact set against the Java enum: `AVAILABLE` / `RUNNING` / `ESCALATE` /
`QUEUE_TO_HUMAN` / `CLOSED` or whatever the current enum lists). For
each value: distinct visual treatment (color + label). NO filtering of
sessions at the UI level (the c7 root suspicion was that
ESCALATE-terminated sessions are silently filtered out).

**Why:** c7 surfaced that the admin list does not show all
handling_states; the `DemoInspectionController.java:86` endpoint uses
`sessionRepository.findAll()` (no filter), so the UI side is the
candidate filter point. Verifying + fixing the StatusBadge rendering
ensures every session is visible.

### #2 — `TraceViewer.tsx` dedup ToolEvent folding (R3.b)

**Anchor:** `ui/.../TraceViewer.tsx` lines 399-471 (tool_call rendering).

**Change:** When rendering tool_call events, check the
`deduplicated=true` flag (already persisted by
`AgentRunLoopImpl.java:580-619` A1 dedup):

- Group consecutive tool_call events with the same `tool_name` +
  `arguments` hash where ANY of them carry `deduplicated=true`.
- Render the FIRST event normally; collapse subsequent
  `deduplicated=true` events into a single "↳ N dedup'd repeats"
  indicator below the primary event.
- Indicator is CLICKABLE: click to expand the full list of dedup'd
  events (each with its original timestamp + `originalAtStep`
  reference).
- Default folded state preserves audit capability (full data still
  persisted; just not displayed by default).

**Why:** c8 showed multiple 0ms tool_call events that looked like a
bug to the human reviewer; in fact they were A1 dedup audit records.
Folding by default makes the trace readable while preserving the audit
trail (anti-误杀 §11 PARAPHRASE_STORM audit unaffected).

### #3 — `TraceViewer.tsx` informational guard rejection badge (R3.c)

**Anchor:** `ui/.../TraceViewer.tsx` (tool_call rendering + guard event
rendering — the exact line range depends on where guard rejections
currently surface; if they're rendered alongside tool_calls, the
distinction is a per-event badge; if they have a separate render path,
3.c lands there).

**Change:** Distinguish INFORMATIONAL guard rejection events from
BLOCKING runtime errors at the trace UI level. The current UI renders
both as red-tinted errors which causes c2/c10/c17 human-review confusion
(a CORRECT §1.4 guard rejection looks identical to a transport 500 or a
crashed tool dispatch).

Categorisation criteria (UI-side, derived from the existing trace event
shape — no new server payload required if the existing payload already
distinguishes; if not, surface as STOP):

- **Informational guard rejection** — trace event whose error class /
  rejection code matches a known §1.4 informational guard:
  - `progressive_resolve_record_outcome_premature` (record_outcome
    submitted in phase ≠ CONFIRM/CLOSE; guard at
    `ResolveDispositionEvaluator.java:160-186` is CORRECT — c2 / c10 /
    c17 surfaced this confusion).
  - `intake_required_fields_missing_for_intake_complete` (handover
    submitted without `intake_fields` populated; guard at
    `SkillGuardrailDispatcher.java:265-298` is CORRECT — c1/c5/c6 pre-fix
    pattern; preserved at all times even after Sub-sprint A's schema
    declaration because dispatching tools may still reject partial
    intake).
  - Add others as discovered (the categoriser list is a UI-side
    allow-list driven by the rejection-code string; NO server-side
    behavioural change). Source the canonical list from the Java guard
    constants if accessible (e.g. as exported constants from
    `SkillGuardrailDispatcher` or `ResolveDispositionEvaluator`); if no
    such export exists, hard-code the UI-side list with a comment
    citing the guard file:line ranges so the allow-list cannot drift
    silently.
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
If a guard somewhere is misclassified by the UI allow-list, the
SAFER default is to render it as a blocking error (preserve human
attention) rather than informational (which would visually downgrade
a real issue).

**Why:** c2 / c10 / c17 — three independent traces — surfaced human
reviewers being confused by record_outcome guard rejections that look
like crashes. The fix is UI-side only because the runtime is already
correct per §1.4 (`OBS-S4` is the autoloop concern about LLM timing;
that is deferred). With R3.c, human reviewers can distinguish
"informational: bot tried to record outcome too early, guard correctly
held it back" from "blocking: tool actually crashed".

### #4 — `TraceViewer.tsx` terminal handling_state indicator

**Anchor:** `ui/.../TraceViewer.tsx` (header / metadata area).

**Change:** Display the session's terminal `handling_state` value
prominently at the trace header (separate from the session-list badge).
For ESCALATE / QUEUE_TO_HUMAN, additionally surface the
`escalation_reason` enum value if present.

**Why:** c7 trace was "found" in admin but the terminal state was not
obvious — adding the explicit header makes the disposition clear.

### #5 — Diagnostic logging for handling_state transitions

**Anchor:** `server/.../SessionManager.java` OR the equivalent client-side
hook in `ui/.../adminApi.ts` (depending on where transitions surface).

**Change:** Add structured logging (not user-facing) that records each
`handling_state` transition with: session_id, old_state, new_state,
timestamp, triggering source (tool call / runtime budget / explicit
close). Backend log entry, NOT runtime behaviour change. Log level: INFO.

**Why:** future "case missing in admin" investigations need a paper
trail. The c7 trail is currently absent — no way to confirm whether
the session reached `CLOSED` or stayed in an intermediate state.

**Hard fence:** this is the ONE backend touch in Sub-sprint B; purely
logging additions, NO behaviour change, NO `SessionManager` state-machine
edit. If the diagnostic logging at #5 requires more than ~10 LOC of
behaviour change, STOP — that is outside Sub-sprint B's fence.

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
  three canonical codes (`progressive_resolve_record_outcome_premature`,
  `intake_required_fields_missing_for_intake_complete`, and at least one
  unknown-code negative that falls through to the blocking-error badge);
  expand reveals the rejection-code string + rationale; click → expand
  preserves audit.
- #4 — terminal handling_state header is visible on ESCALATE / CLOSED /
  QUEUE_TO_HUMAN sessions.

## Anti-误杀 invariants (HARD)

1. **Audit data NOT lost.** Dedup folding (#2) and informational
   collapsing (#3) are DISPLAY choices; the underlying `ToolEvent`
   records and guard rejection events remain in the trace with full
   payload. PARAPHRASE_STORM audit MUST still be possible by expanding.
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
   does not recognise a rejection code, render it as a blocking error
   (preserve human attention). DO NOT render unknown rejections as
   informational by default — that would visually downgrade unknown
   issues, the opposite of what observability needs.
6. **No new `escalation_reason` enum value** (#4 surfaces existing
   values).
7. **No `BotSession.HandlingState` enum changes.** UI renders existing
   values; if a missing value is discovered, surface as a separate
   R-item — do NOT add enum values in Sub-sprint B.

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
- Any `eval_interactive/` file (CaseSpecs, simulator, scoring, harness).
- Autoloop 5-file SHA-locked scoring set (`autoloop/.../scoring/*.py` —
  fence-#13).
- `autoloop/config.yaml` `baseline_dir` pointer.
- `docs/current_eval_baseline.md`.

**STOP conditions**:

- If the UI test suite has a pre-existing snapshot regression that
  predates this sub-sprint, surface as OQ; do NOT mask with snapshot-only
  updates.
- If `BotSession.HandlingState` is incomplete (a missing value
  discovered in trace data during #1 implementation), surface as a
  separate R-item; do NOT add enum values here.
- If the R3.c rejection-code allow-list cannot be sourced from existing
  Java constants AND would require > ~6 hard-coded entries, STOP and
  surface as scope question (the safe path is a small allow-list with
  the SAFER blocking default — large allow-lists drift silently).
- If #5 diagnostic logging requires more than ~10 LOC of behaviour
  change, STOP — outside Sub-sprint B's fence.

## Test / eval requirements

- All new UI tests in #6 GREEN.
- Existing UI tests UNCHANGED (no snapshot regressions accepted unless
  the change is explicitly the intended UI delta of #1-#4).
- Java baseline `1244 / 1 / 0 / 2` preserved (sole failure = inherited
  OQ-S41.5); Sub-sprint B may add +1-2 logging-related test cases for
  #5 — these MUST not flip the failure count.
- Eval pytest UNCHANGED.
- Autoloop pytest UNCHANGED.
- NO real-LLM re-bless at this sub-sprint close. The outcome evidence
  re-bless is the M-Auto-6 milestone-shared re-bless that runs after
  Sub-sprint B + C-1 + C-2 all land.

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
- neighbor: none (independent observability fix; the surface is
  orthogonal to runtime semantic R-items).
- negative: ESCALATE / handover-completed sessions remain in admin
  visible + clickable; A1 dedup folding allows expanding to see full
  dedup'd event list (audit not lost); unrecognised rejection codes
  render as blocking (NOT informational); real transport / 5xx / OOM
  errors render as blocking (UNCHANGED).
- shadow: not applicable (UI-only; eval traces don't render through
  this UI).
```

## Codex review plan (per `process/milestone-framework.md` §4.3)

**Per-sub-sprint Codex review: OPTIONAL** (UI-only per proposal §6.5 /
§7.2; visual verification by deliver + human is the primary acceptance
evidence). Milestone-shared Codex at M-Auto-6 close covers Sub-sprint
A + B + C-1 + C-2 cumulatively.

If deliver-agent elects to dispatch a per-sub-sprint Codex on B:

- Focus on Q1 / Q2 / Q5 / Q7 (zero content matching / no Tier-0
  invariant / no semantic decision moved / safety floor unchanged).
- Q3 / Q4 / Q6 / Q8 / Q9 are largely N/A for a UI-only sub-sprint.
- The R3.c allow-list MUST be reviewed for keyword-matching shape: the
  rejection-code strings are CONSTANTS sourced from the Java guard
  layer, not user-message keywords. If they originate from anywhere
  other than backend trace event shapes, that's a Q1 concern.

## Handoff requirements (dev authors `docs/sprints/sprint-079-handoff.md`)

§1 of the handoff must include:

- For each of #1-#5: file:line ranges + rationale + the test name(s)
  that gate it.
- **For R3.c (#3): the explicit rejection-code allow-list** the UI
  uses to categorise informational vs blocking, plus the source of
  each entry (Java guard constant / hard-coded with file:line citation).
- **Visual verification evidence**: screenshots OR a deliver-reviewable
  step-by-step that exercises each of c7-like (admin missing case),
  c8-like (dedup event folding + expand), c2/c10/c17-like (informational
  guard rejection badge + expand revealing rationale), and a negative
  (unrecognised rejection code → blocking badge).
- UI test results (full numeric: passed / failed / skipped).
- Java test results (full numeric — confirm no regression vs A's
  `1244/1/0/2` post-S-Auto-23 baseline).
- Eval pytest / autoloop pytest results (UNCHANGED).
- STOP confirmations:
  - File fence respected (no runtime behaviour change beyond #5 logging).
  - No `BotSession.HandlingState` enum value added.
  - No prompt / yaml / CaseSpec / simulator / scoring touched.
  - `baseline_dir` UNCHANGED.
  - `docs/current_eval_baseline.md` UNCHANGED.
  - **No outcome-evidence re-bless launched at this sub-sprint** (the
    re-bless is the M-Auto-6 milestone-shared one that runs after C-1 +
    C-2).

## Commit discipline

Recommended commit split (per `prompt-artifact-rules.md` §9 + proposal
§6.3):

1. **Commit 1 — R3.a**: `SessionList.tsx` StatusBadge full handling_state
   coverage + tests.
2. **Commit 2 — R3.b**: `TraceViewer.tsx` dedup ToolEvent folding + tests.
3. **Commit 3 — R3.c**: `TraceViewer.tsx` informational guard rejection
   badge + tests (incl. negative: unknown code → blocking).
4. **Commit 4 — #4**: `TraceViewer.tsx` terminal handling_state header.
5. **Commit 5 — #5**: handling_state diagnostic logging
   (`SessionManager.java` or equivalent).
6. **Commit 6 — Dev handoff**: `docs/sprints/sprint-079-handoff.md`.

Each commit message follows the established convention (see sprint-077
+ sprint-078 commits as reference). All six commits are by the dev agent;
deliver-agent commits its own close-archive artifacts separately at
sub-sprint close.

## Self-check checklist (dev completes before claiming done)

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
- [ ] No runtime behaviour change beyond #5 diagnostic logging
      (≤ ~10 LOC behaviour-affecting).
- [ ] Audit data preserved end-to-end — clicking expand on a folded
      dedup indicator (#2) and on an informational guard badge (#3)
      shows ALL underlying records.
- [ ] No `BotSession.HandlingState` enum value added.
- [ ] No prompt / yaml / CaseSpec / simulator / scoring touched.
- [ ] §7 stanza copied verbatim into the handoff (record-only;
      §7-EXEMPT).
- [ ] Visual verification evidence in handoff §1 (screenshots OR
      step-by-step) covering c7 / c8 / c2 / c10 / c17 + negative.
- [ ] `baseline_dir` UNCHANGED.
- [ ] `docs/current_eval_baseline.md` UNCHANGED.
- [ ] No outcome-evidence re-bless launched at this sub-sprint close
      (the milestone-shared re-bless runs after C-1 + C-2).

When all checked: hand back to deliver-agent for close review + visual
verification + Sub-sprint C-1 / C-2 dev launch.
